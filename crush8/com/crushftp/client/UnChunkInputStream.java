package com.crushftp.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UnChunkInputStream extends BufferedInputStream {
  boolean closed;
  
  InputStream in = null;
  
  boolean needSize = true;
  
  long chunkSize = 0L;
  
  long markChunkSize = 0L;
  
  boolean markNeedSize = true;
  
  boolean chunked = true;
  
  public UnChunkInputStream(InputStream in) {
    super(in);
    this.in = in;
  }
  
  public int read() throws IOException {
    byte[] b1 = new byte[1];
    int bytesRead = read(b1, 0, 1);
    if (bytesRead < 0)
      return -1; 
    return b1[0] & 0xFF;
  }
  
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }
  
  public void setChunked(boolean chunked) {
    this.chunked = chunked;
  }
  
  public void reInitialize() {
    this.needSize = true;
    this.chunkSize = 0L;
    this.markChunkSize = 0L;
    this.markNeedSize = true;
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    if (!this.chunked)
      return this.in.read(b); 
    if (this.needSize) {
      this.chunkSize = Common.getChunkSize(this.in);
      this.needSize = false;
      if (this.chunkSize == 0L)
        return -1; 
    } 
    if (this.chunkSize < 0L)
      return -1; 
    int minLen = b.length;
    if (this.chunkSize < minLen)
      minLen = (int)this.chunkSize; 
    if (len < minLen)
      minLen = len; 
    int bytes_read = this.in.read(b, off, minLen);
    if (bytes_read > 0)
      this.chunkSize -= bytes_read; 
    if (this.chunkSize == 0L) {
      Common.getChunkSize(this.in);
      this.needSize = true;
    } 
    return bytes_read;
  }
  
  public long skip(long n) throws IOException {
    return read(new byte[(int)n]);
  }
  
  public int available() throws IOException {
    return this.in.available();
  }
  
  public synchronized void mark(int readlimit) {
    this.in.mark(readlimit);
    this.markChunkSize = this.chunkSize;
    this.markNeedSize = this.needSize;
  }
  
  public synchronized void reset() throws IOException {
    this.in.reset();
    this.chunkSize = this.markChunkSize;
    this.needSize = this.markNeedSize;
  }
  
  public boolean markSupported() {
    return this.in.markSupported();
  }
  
  public void close() throws IOException {
    if (this.closed)
      return; 
    this.closed = true;
    this.in.close();
  }
}
