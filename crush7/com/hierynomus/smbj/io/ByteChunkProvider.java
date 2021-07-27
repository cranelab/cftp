package com.hierynomus.smbj.io;

import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smbj.common.SMBRuntimeException;
import java.io.IOException;
import java.io.OutputStream;

public abstract class ByteChunkProvider {
  protected static final int CHUNK_SIZE = 65536;
  
  protected long offset;
  
  protected int chunkSize = 65536;
  
  public abstract boolean isAvailable();
  
  public void writeChunk(OutputStream os) {
    byte[] chunk = new byte[this.chunkSize];
    try {
      int size = getChunk(chunk);
      os.write(chunk, 0, size);
      this.offset += size;
    } catch (IOException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  public void writeChunks(Buffer<?> buffer, int nrChunks) {
    byte[] chunk = new byte[this.chunkSize];
    for (int i = 0; i < nrChunks; i++) {
      try {
        int size = getChunk(chunk);
        buffer.putRawBytes(chunk, 0, size);
        this.offset += size;
      } catch (IOException e) {
        throw new SMBRuntimeException(e);
      } 
    } 
  }
  
  public void writeChunk(Buffer<?> buffer) {
    byte[] chunk = new byte[this.chunkSize];
    try {
      int size = getChunk(chunk);
      buffer.putRawBytes(chunk, 0, size);
      this.offset += size;
    } catch (IOException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  public long getOffset() {
    return this.offset;
  }
  
  protected abstract int getChunk(byte[] paramArrayOfbyte) throws IOException;
  
  public abstract int bytesLeft();
}
