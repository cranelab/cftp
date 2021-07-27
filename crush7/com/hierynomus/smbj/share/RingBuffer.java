package com.hierynomus.smbj.share;

class RingBuffer {
  private byte[] buf;
  
  private int writeIndex;
  
  private int readIndex;
  
  private int size;
  
  public RingBuffer(int maxSize) {
    this.buf = new byte[maxSize];
  }
  
  public void write(byte[] b, int off, int len) {
    if (b.length - off < len)
      throw new IllegalArgumentException("Bytes to write do not exist in source"); 
    if (len > this.buf.length - this.size)
      throw new IndexOutOfBoundsException("Size of bytes to be written is greater than available buffer space"); 
    writeBytes(b, off, len);
    this.writeIndex = (this.writeIndex + len) % this.buf.length;
    this.size += len;
  }
  
  public void write(int b) {
    write(new byte[] { (byte)b }, 0, 1);
  }
  
  public int read(byte[] chunk) {
    int len = (this.size < chunk.length) ? this.size : chunk.length;
    readBytes(chunk, len);
    this.readIndex = (this.readIndex + len) % this.buf.length;
    this.size -= len;
    return len;
  }
  
  private void readBytes(byte[] chunk, int len) {
    if (this.readIndex + len <= this.buf.length) {
      System.arraycopy(this.buf, this.readIndex, chunk, 0, len);
    } else {
      int bytesToEnd = this.buf.length - this.readIndex;
      System.arraycopy(this.buf, this.readIndex, chunk, 0, bytesToEnd);
      System.arraycopy(this.buf, 0, chunk, bytesToEnd, len - bytesToEnd);
    } 
  }
  
  private void writeBytes(byte[] b, int off, int len) {
    if (this.writeIndex + len <= this.buf.length) {
      System.arraycopy(b, off, this.buf, this.writeIndex, len);
    } else {
      int bytesToEnd = this.buf.length - this.writeIndex;
      System.arraycopy(b, off, this.buf, this.writeIndex, bytesToEnd);
      System.arraycopy(b, off + bytesToEnd, this.buf, 0, len - bytesToEnd);
    } 
  }
  
  public int maxSize() {
    return this.buf.length;
  }
  
  public int size() {
    return this.size;
  }
  
  public boolean isFull() {
    return (size() == this.buf.length);
  }
  
  public boolean isFull(int len) {
    if (len > this.buf.length)
      throw new IllegalArgumentException("RingBuffer of length " + this.buf.length + " cannot accomodate " + len + " bytes."); 
    return (this.size + len > this.buf.length);
  }
  
  public boolean isEmpty() {
    return (this.size <= 0);
  }
}
