package com.sun.mail.imap.protocol;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Writer;

public class BASE64MailboxEncoder {
  protected byte[] buffer = new byte[4];
  
  protected int bufsize = 0;
  
  protected boolean started = false;
  
  protected Writer out = null;
  
  public static String encode(String original) {
    BASE64MailboxEncoder base64stream = null;
    char[] origchars = original.toCharArray();
    int length = origchars.length;
    boolean changedString = false;
    CharArrayWriter writer = new CharArrayWriter(length);
    for (int index = 0; index < length; index++) {
      char current = origchars[index];
      if (current >= ' ' && current <= '~') {
        if (base64stream != null)
          base64stream.flush(); 
        if (current == '&') {
          changedString = true;
          writer.write(38);
          writer.write(45);
        } else {
          writer.write(current);
        } 
      } else {
        if (base64stream == null) {
          base64stream = new BASE64MailboxEncoder(writer);
          changedString = true;
        } 
        base64stream.write(current);
      } 
    } 
    if (base64stream != null)
      base64stream.flush(); 
    if (changedString)
      return writer.toString(); 
    return original;
  }
  
  public BASE64MailboxEncoder(Writer what) {
    this.out = what;
  }
  
  public void write(int c) {
    try {
      if (!this.started) {
        this.started = true;
        this.out.write(38);
      } 
      this.buffer[this.bufsize++] = (byte)(c >> 8);
      this.buffer[this.bufsize++] = (byte)(c & 0xFF);
      if (this.bufsize >= 3) {
        encode();
        this.bufsize -= 3;
      } 
    } catch (IOException iOException) {}
  }
  
  public void flush() {
    try {
      if (this.bufsize > 0) {
        encode();
        this.bufsize = 0;
      } 
      if (this.started) {
        this.out.write(45);
        this.started = false;
      } 
    } catch (IOException iOException) {}
  }
  
  protected void encode() throws IOException {
    if (this.bufsize == 1) {
      byte a = this.buffer[0];
      byte b = 0;
      byte c = 0;
      this.out.write(pem_array[a >>> 2 & 0x3F]);
      this.out.write(pem_array[(a << 4 & 0x30) + (b >>> 4 & 0xF)]);
    } else if (this.bufsize == 2) {
      byte a = this.buffer[0];
      byte b = this.buffer[1];
      byte c = 0;
      this.out.write(pem_array[a >>> 2 & 0x3F]);
      this.out.write(pem_array[(a << 4 & 0x30) + (b >>> 4 & 0xF)]);
      this.out.write(pem_array[(b << 2 & 0x3C) + (c >>> 6 & 0x3)]);
    } else {
      byte a = this.buffer[0];
      byte b = this.buffer[1];
      byte c = this.buffer[2];
      this.out.write(pem_array[a >>> 2 & 0x3F]);
      this.out.write(pem_array[(a << 4 & 0x30) + (b >>> 4 & 0xF)]);
      this.out.write(pem_array[(b << 2 & 0x3C) + (c >>> 6 & 0x3)]);
      this.out.write(pem_array[c & 0x3F]);
      if (this.bufsize == 4)
        this.buffer[0] = this.buffer[3]; 
    } 
  }
  
  private static final char[] pem_array = new char[] { 
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 
      'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 
      'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 
      'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 
      'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 
      'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', 
      '8', '9', '+', ',' };
}
