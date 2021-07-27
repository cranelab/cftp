package com.crushftp.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LineReader extends InputStream {
  public int lineCount = 0;
  
  byte[] crlf = "\r\n".getBytes();
  
  byte[] crlfOS = System.getProperties().getProperty("line.separator").getBytes();
  
  ByteArrayOutputStream baos = new ByteArrayOutputStream();
  
  BufferedInputStream in = null;
  
  boolean doneReading = false;
  
  byte[] b1 = new byte[1];
  
  byte[] b2 = new byte[2];
  
  boolean strict = false;
  
  boolean slow = false;
  
  BufferedReader br = null;
  
  public LineReader(InputStream in) {
    if (System.getProperty("crushftp.line_separator_crlf", "false").equals("true"))
      this.crlfOS = this.crlf; 
    this.strict = System.getProperty("ftp_ascii_strict", "false").equals("true");
    this.slow = System.getProperty("ftp_ascii_slow", "false").equals("true");
    this.in = new BufferedInputStream(in, 65535);
    if (this.slow)
      this.br = new BufferedReader(new InputStreamReader(this.in)); 
  }
  
  public LineReader(InputStream in, String separator) {
    this.crlfOS = separator.getBytes();
    this.strict = System.getProperty("ftp_ascii_strict", "false").equals("true");
    this.slow = false;
    this.in = new BufferedInputStream(in, 65535);
  }
  
  public int read() throws IOException {
    return this.in.read();
  }
  
  public int read(byte[] b) throws IOException {
    return this.in.read(b);
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    return this.in.read(b, off, len);
  }
  
  public long skip(long n) throws IOException {
    return this.in.skip(n);
  }
  
  public int available() throws IOException {
    return this.in.available();
  }
  
  public synchronized void mark(int readlimit) {
    this.in.mark(readlimit);
  }
  
  public synchronized void reset() throws IOException {
    this.in.reset();
  }
  
  public boolean markSupported() {
    return this.in.markSupported();
  }
  
  public void close() throws IOException {
    this.in.close();
  }
  
  public byte[] readLineCRLF() throws IOException {
    if (this.slow) {
      String s = this.br.readLine();
      if (s == null)
        return null; 
      return (String.valueOf(s) + "\r\n").getBytes("UTF8");
    } 
    this.baos.reset();
    while (readLine()) {
      this.baos.write(this.crlf);
      this.lineCount++;
      if (this.baos.size() > 32768)
        break; 
    } 
    if (this.baos.size() > 0)
      return this.baos.toByteArray(); 
    return null;
  }
  
  public byte[] readLineOS() throws IOException {
    if (this.slow) {
      String s = this.br.readLine();
      if (s == null)
        return null; 
      if (System.getProperty("ftp_ascii_encoding", "UTF8").equals(""))
        return (String.valueOf(s) + System.getProperties().getProperty("line.separator")).getBytes(); 
      return (String.valueOf(s) + System.getProperties().getProperty("line.separator")).getBytes(System.getProperty("ftp_ascii_encoding", "UTF8"));
    } 
    this.baos.reset();
    while (readLine()) {
      this.baos.write(this.crlfOS);
      this.lineCount++;
      if (this.baos.size() > 32768)
        break; 
    } 
    if (this.baos.size() > 0)
      return this.baos.toByteArray(); 
    return null;
  }
  
  private boolean readLine() throws IOException {
    if (this.doneReading)
      return false; 
    this.b1[0] = 0;
    this.b2[0] = 0;
    this.b2[1] = 0;
    int bytesRead = 0;
    boolean foundEOL = false;
    while (bytesRead >= 0 && this.baos.size() < 32768) {
      bytesRead = this.in.read(this.b1);
      if (bytesRead > 0) {
        if (this.b1[0] == 10) {
          foundEOL = true;
          break;
        } 
        if (this.b1[0] == 13) {
          if (this.strict) {
            this.in.mark(1);
            bytesRead = this.in.read(this.b2, 0, 1);
            if (bytesRead > 0) {
              if (this.b2[0] == 10) {
                foundEOL = true;
                break;
              } 
              this.baos.write(this.b1);
              this.baos.write(this.b2, 0, 1);
              continue;
            } 
            this.baos.write(this.b1);
            bytesRead = 0;
            foundEOL = true;
            break;
          } 
          this.in.mark(2);
          bytesRead = this.in.read(this.b2);
          if (bytesRead > 0) {
            if (this.b2[0] == 13 && bytesRead == 2 && this.b2[1] == 10) {
              foundEOL = true;
              break;
            } 
            if (this.b2[0] == 10) {
              this.in.reset();
              this.in.skip(1L);
              foundEOL = true;
              break;
            } 
            this.in.reset();
            foundEOL = true;
            break;
          } 
          bytesRead = 0;
          foundEOL = true;
          break;
        } 
        this.baos.write(this.b1);
      } 
    } 
    if (bytesRead < 0) {
      this.doneReading = true;
      return false;
    } 
    return foundEOL;
  }
}
