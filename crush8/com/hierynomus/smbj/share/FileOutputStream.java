package com.hierynomus.smbj.share;

import com.hierynomus.smbj.ProgressListener;
import com.hierynomus.smbj.io.ByteChunkProvider;
import java.io.IOException;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileOutputStream extends OutputStream {
  private SMB2Writer writer;
  
  private ProgressListener progressListener;
  
  private boolean isClosed = false;
  
  private ByteArrayProvider provider;
  
  private static final Logger logger = LoggerFactory.getLogger(FileOutputStream.class);
  
  FileOutputStream(SMB2Writer writer, int bufferSize, long offset, ProgressListener progressListener) {
    this.writer = writer;
    this.progressListener = progressListener;
    this.provider = new ByteArrayProvider(bufferSize, offset);
  }
  
  public void write(int b) throws IOException {
    verifyConnectionNotClosed();
    if (this.provider.isBufferFull())
      flush(); 
    if (!this.provider.isBufferFull())
      this.provider.writeByte(b); 
  }
  
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }
  
  public void write(byte[] b, int off, int len) throws IOException {
    verifyConnectionNotClosed();
    int offset = off;
    int length = len;
    do {
      int writeLen = Math.min(length, this.provider.maxSize());
      while (this.provider.isBufferFull(writeLen))
        flush(); 
      if (!this.provider.isBufferFull())
        this.provider.writeBytes(b, offset, writeLen); 
      offset += writeLen;
      length -= writeLen;
    } while (length > 0);
  }
  
  public void flush() throws IOException {
    verifyConnectionNotClosed();
    if (this.provider.isAvailable())
      sendWriteRequest(); 
  }
  
  private void sendWriteRequest() {
    this.writer.write(this.provider, this.progressListener);
  }
  
  public void close() throws IOException {
    while (this.provider.isAvailable())
      sendWriteRequest(); 
    this.provider.reset();
    this.isClosed = true;
    this.writer = null;
    logger.debug("EOF, {} bytes written", Long.valueOf(this.provider.getOffset()));
  }
  
  private void verifyConnectionNotClosed() throws IOException {
    if (this.isClosed)
      throw new IOException("Stream is closed"); 
  }
  
  private static class ByteArrayProvider extends ByteChunkProvider {
    private RingBuffer buf;
    
    private ByteArrayProvider(int maxWriteSize, long offset) {
      this.buf = new RingBuffer(maxWriteSize);
      this.offset = offset;
    }
    
    public boolean isAvailable() {
      return (this.buf != null && !this.buf.isEmpty());
    }
    
    protected int getChunk(byte[] chunk) {
      return this.buf.read(chunk);
    }
    
    public int bytesLeft() {
      return this.buf.size();
    }
    
    public void writeBytes(byte[] b, int off, int len) {
      this.buf.write(b, off, len);
    }
    
    public void writeByte(int b) {
      this.buf.write(b);
    }
    
    public boolean isBufferFull() {
      return this.buf.isFull();
    }
    
    public boolean isBufferFull(int len) {
      return this.buf.isFull(len);
    }
    
    public int maxSize() {
      return this.buf.maxSize();
    }
    
    private void reset() {
      this.buf = null;
    }
  }
}
