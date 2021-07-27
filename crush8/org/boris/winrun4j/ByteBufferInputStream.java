package org.boris.winrun4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class ByteBufferInputStream extends InputStream {
  private ByteBuffer bb;
  
  public ByteBufferInputStream(ByteBuffer bb) {
    this.bb = bb;
  }
  
  public int read() throws IOException {
    try {
      return this.bb.get() & Integer.MAX_VALUE;
    } catch (BufferUnderflowException e) {
      return -1;
    } 
  }
  
  public int read(byte[] b) throws IOException {
    int len = b.length;
    if (len > this.bb.remaining())
      len = this.bb.remaining(); 
    this.bb.get(b, 0, len);
    return len;
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    if (len > this.bb.remaining())
      len = this.bb.remaining(); 
    this.bb.get(b, off, len);
    return len;
  }
  
  public long skip(long n) throws IOException {
    if (n > this.bb.remaining())
      n = this.bb.remaining(); 
    this.bb.position((int)(this.bb.position() + n));
    return n;
  }
  
  public int available() throws IOException {
    return this.bb.remaining();
  }
  
  public boolean markSupported() {
    return false;
  }
}
