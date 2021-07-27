package com.sun.mail.util;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BASE64DecoderStream extends FilterInputStream {
  private byte[] buffer = new byte[3];
  
  private int bufsize = 0;
  
  private int index = 0;
  
  private byte[] input_buffer = new byte[8190];
  
  private int input_pos = 0;
  
  private int input_len = 0;
  
  private boolean ignoreErrors = false;
  
  public BASE64DecoderStream(InputStream in) {
    super(in);
    this.ignoreErrors = PropUtil.getBooleanSystemProperty("mail.mime.base64.ignoreerrors", false);
  }
  
  public BASE64DecoderStream(InputStream in, boolean ignoreErrors) {
    super(in);
    this.ignoreErrors = ignoreErrors;
  }
  
  public int read() throws IOException {
    if (this.index >= this.bufsize) {
      this.bufsize = decode(this.buffer, 0, this.buffer.length);
      if (this.bufsize <= 0)
        return -1; 
      this.index = 0;
    } 
    return this.buffer[this.index++] & 0xFF;
  }
  
  public int read(byte[] buf, int off, int len) throws IOException {
    int off0 = off;
    while (this.index < this.bufsize && len > 0) {
      buf[off++] = this.buffer[this.index++];
      len--;
    } 
    if (this.index >= this.bufsize)
      this.bufsize = this.index = 0; 
    int bsize = len / 3 * 3;
    if (bsize > 0) {
      int size = decode(buf, off, bsize);
      off += size;
      len -= size;
      if (size != bsize) {
        if (off == off0)
          return -1; 
        return off - off0;
      } 
    } 
    for (; len > 0; len--) {
      int c = read();
      if (c == -1)
        break; 
      buf[off++] = (byte)c;
    } 
    if (off == off0)
      return -1; 
    return off - off0;
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
    return this.in.available() * 3 / 4 + this.bufsize - this.index;
  }
  
  private static final char[] pem_array = new char[] { 
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 
      'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 
      'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 
      'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 
      'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 
      'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', 
      '8', '9', '+', '/' };
  
  private static final byte[] pem_convert_array = new byte[256];
  
  static {
    int i;
    for (i = 0; i < 255; i++)
      pem_convert_array[i] = -1; 
    for (i = 0; i < pem_array.length; i++)
      pem_convert_array[pem_array[i]] = (byte)i; 
  }
  
  private int decode(byte[] outbuf, int pos, int len) throws IOException {
    int pos0 = pos;
    while (len >= 3) {
      int got = 0;
      int val = 0;
      while (got < 4) {
        int i = getByte();
        if (i == -1 || i == -2) {
          boolean atEOF;
          if (i == -1) {
            if (got == 0)
              return pos - pos0; 
            if (!this.ignoreErrors)
              throw new DecodingException("BASE64Decoder: Error in encoded stream: needed 4 valid base64 characters but only got " + got + " before EOF" + 


                  
                  recentChars()); 
            atEOF = true;
          } else {
            if (got < 2 && !this.ignoreErrors)
              throw new DecodingException("BASE64Decoder: Error in encoded stream: needed at least 2 valid base64 characters, but only got " + got + " before padding character (=)" + 



                  
                  recentChars()); 
            if (got == 0)
              return pos - pos0; 
            atEOF = false;
          } 
          int size = got - 1;
          if (size == 0)
            size = 1; 
          got++;
          val <<= 6;
          while (got < 4) {
            if (!atEOF) {
              i = getByte();
              if (i == -1) {
                if (!this.ignoreErrors)
                  throw new DecodingException("BASE64Decoder: Error in encoded stream: hit EOF while looking for padding characters (=)" + 


                      
                      recentChars()); 
              } else if (i != -2 && 
                !this.ignoreErrors) {
                throw new DecodingException("BASE64Decoder: Error in encoded stream: found valid base64 character after a padding character (=)" + 


                    
                    recentChars());
              } 
            } 
            val <<= 6;
            got++;
          } 
          val >>= 8;
          if (size == 2)
            outbuf[pos + 1] = (byte)(val & 0xFF); 
          val >>= 8;
          outbuf[pos] = (byte)(val & 0xFF);
          pos += size;
          return pos - pos0;
        } 
        val <<= 6;
        got++;
        val |= i;
      } 
      outbuf[pos + 2] = (byte)(val & 0xFF);
      val >>= 8;
      outbuf[pos + 1] = (byte)(val & 0xFF);
      val >>= 8;
      outbuf[pos] = (byte)(val & 0xFF);
      len -= 3;
      pos += 3;
    } 
    return pos - pos0;
  }
  
  private int getByte() throws IOException {
    while (true) {
      if (this.input_pos >= this.input_len) {
        try {
          this.input_len = this.in.read(this.input_buffer);
        } catch (EOFException ex) {
          return -1;
        } 
        if (this.input_len <= 0)
          return -1; 
        this.input_pos = 0;
      } 
      int c = this.input_buffer[this.input_pos++] & 0xFF;
      if (c == 61)
        return -2; 
      c = pem_convert_array[c];
      if (c != -1)
        return c; 
    } 
  }
  
  private String recentChars() {
    String errstr = "";
    int nc = (this.input_pos > 10) ? 10 : this.input_pos;
    if (nc > 0) {
      errstr = errstr + ", the " + nc + " most recent characters were: \"";
      for (int k = this.input_pos - nc; k < this.input_pos; k++) {
        char c = (char)(this.input_buffer[k] & 0xFF);
        switch (c) {
          case '\r':
            errstr = errstr + "\\r";
            break;
          case '\n':
            errstr = errstr + "\\n";
            break;
          case '\t':
            errstr = errstr + "\\t";
            break;
          default:
            if (c >= ' ' && c < '') {
              errstr = errstr + c;
              break;
            } 
            errstr = errstr + "\\" + c;
            break;
        } 
      } 
      errstr = errstr + "\"";
    } 
    return errstr;
  }
  
  public static byte[] decode(byte[] inbuf) {
    int size = inbuf.length / 4 * 3;
    if (size == 0)
      return inbuf; 
    if (inbuf[inbuf.length - 1] == 61) {
      size--;
      if (inbuf[inbuf.length - 2] == 61)
        size--; 
    } 
    byte[] outbuf = new byte[size];
    int inpos = 0, outpos = 0;
    size = inbuf.length;
    while (size > 0) {
      int osize = 3;
      int val = pem_convert_array[inbuf[inpos++] & 0xFF];
      val <<= 6;
      val |= pem_convert_array[inbuf[inpos++] & 0xFF];
      val <<= 6;
      if (inbuf[inpos] != 61) {
        val |= pem_convert_array[inbuf[inpos++] & 0xFF];
      } else {
        osize--;
      } 
      val <<= 6;
      if (inbuf[inpos] != 61) {
        val |= pem_convert_array[inbuf[inpos++] & 0xFF];
      } else {
        osize--;
      } 
      if (osize > 2)
        outbuf[outpos + 2] = (byte)(val & 0xFF); 
      val >>= 8;
      if (osize > 1)
        outbuf[outpos + 1] = (byte)(val & 0xFF); 
      val >>= 8;
      outbuf[outpos] = (byte)(val & 0xFF);
      outpos += osize;
      size -= 4;
    } 
    return outbuf;
  }
}
