package com.sun.mail.util;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class UUEncoderStream extends FilterOutputStream {
  private byte[] buffer;
  
  private int bufsize = 0;
  
  private boolean wrotePrefix = false;
  
  private boolean wroteSuffix = false;
  
  private String name;
  
  private int mode;
  
  public UUEncoderStream(OutputStream out) {
    this(out, "encoder.buf", 420);
  }
  
  public UUEncoderStream(OutputStream out, String name) {
    this(out, name, 420);
  }
  
  public UUEncoderStream(OutputStream out, String name, int mode) {
    super(out);
    this.name = name;
    this.mode = mode;
    this.buffer = new byte[45];
  }
  
  public void setNameMode(String name, int mode) {
    this.name = name;
    this.mode = mode;
  }
  
  public void write(byte[] b, int off, int len) throws IOException {
    for (int i = 0; i < len; i++)
      write(b[off + i]); 
  }
  
  public void write(byte[] data) throws IOException {
    write(data, 0, data.length);
  }
  
  public void write(int c) throws IOException {
    this.buffer[this.bufsize++] = (byte)c;
    if (this.bufsize == 45) {
      writePrefix();
      encode();
      this.bufsize = 0;
    } 
  }
  
  public void flush() throws IOException {
    if (this.bufsize > 0) {
      writePrefix();
      encode();
      this.bufsize = 0;
    } 
    writeSuffix();
    this.out.flush();
  }
  
  public void close() throws IOException {
    flush();
    this.out.close();
  }
  
  private void writePrefix() throws IOException {
    if (!this.wrotePrefix) {
      PrintStream ps = new PrintStream(this.out, false, "utf-8");
      ps.format("begin %o %s%n", new Object[] { Integer.valueOf(this.mode), this.name });
      ps.flush();
      this.wrotePrefix = true;
    } 
  }
  
  private void writeSuffix() throws IOException {
    if (!this.wroteSuffix) {
      PrintStream ps = new PrintStream(this.out, false, "us-ascii");
      ps.println(" \nend");
      ps.flush();
      this.wroteSuffix = true;
    } 
  }
  
  private void encode() throws IOException {
    int i = 0;
    this.out.write((this.bufsize & 0x3F) + 32);
    while (i < this.bufsize) {
      byte b, c, a = this.buffer[i++];
      if (i < this.bufsize) {
        b = this.buffer[i++];
        if (i < this.bufsize) {
          c = this.buffer[i++];
        } else {
          c = 1;
        } 
      } else {
        b = 1;
        c = 1;
      } 
      int c1 = a >>> 2 & 0x3F;
      int c2 = a << 4 & 0x30 | b >>> 4 & 0xF;
      int c3 = b << 2 & 0x3C | c >>> 6 & 0x3;
      int c4 = c & 0x3F;
      this.out.write(c1 + 32);
      this.out.write(c2 + 32);
      this.out.write(c3 + 32);
      this.out.write(c4 + 32);
    } 
    this.out.write(10);
  }
}
