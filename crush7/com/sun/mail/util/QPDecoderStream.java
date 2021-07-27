package com.sun.mail.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class QPDecoderStream extends FilterInputStream {
  protected byte[] ba = new byte[2];
  
  protected int spaces = 0;
  
  public QPDecoderStream(InputStream in) {
    super(new PushbackInputStream(in, 2));
  }
  
  public int read() throws IOException {
    if (this.spaces > 0) {
      this.spaces--;
      return 32;
    } 
    int c = this.in.read();
    if (c == 32) {
      while ((c = this.in.read()) == 32)
        this.spaces++; 
      if (c == 13 || c == 10 || c == -1) {
        this.spaces = 0;
      } else {
        ((PushbackInputStream)this.in).unread(c);
        c = 32;
      } 
      return c;
    } 
    if (c == 61) {
      int a = this.in.read();
      if (a == 10)
        return read(); 
      if (a == 13) {
        int b = this.in.read();
        if (b != 10)
          ((PushbackInputStream)this.in).unread(b); 
        return read();
      } 
      if (a == -1)
        return -1; 
      this.ba[0] = (byte)a;
      this.ba[1] = (byte)this.in.read();
      try {
        return ASCIIUtility.parseInt(this.ba, 0, 2, 16);
      } catch (NumberFormatException nex) {
        ((PushbackInputStream)this.in).unread(this.ba);
        return c;
      } 
    } 
    return c;
  }
  
  public int read(byte[] buf, int off, int len) throws IOException {
    int i;
    for (i = 0; i < len; i++) {
      int c;
      if ((c = read()) == -1) {
        if (i == 0)
          i = -1; 
        break;
      } 
      buf[off + i] = (byte)c;
    } 
    return i;
  }
  
  public long skip(long n) throws IOException {
    long skipped = 0L;
    while (n-- > 0L && read() >= 0)
      skipped++; 
    return skipped;
  }
  
  public boolean markSupported() {
    return false;
  }
  
  public int available() throws IOException {
    return this.in.available();
  }
}
