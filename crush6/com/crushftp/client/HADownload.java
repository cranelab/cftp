package com.crushftp.client;

import java.io.IOException;
import java.io.InputStream;

public class HADownload extends InputStream {
  GenericClient c = null;
  
  InputStream in = null;
  
  String path = null;
  
  long endPos = 0L;
  
  boolean binary = true;
  
  int reconnectDelay = 10;
  
  byte[] b1 = new byte[1];
  
  long pos = 0L;
  
  boolean closed = false;
  
  public HADownload(GenericClient c, String path, long startPos, long endPos, boolean binary, int reconnectDelay) throws IOException {
    this.c = c;
    this.path = path;
    this.endPos = endPos;
    this.binary = binary;
    this.reconnectDelay = reconnectDelay;
    this.pos = startPos;
    try {
      this.in = c.download2(path, startPos, endPos, binary);
    } catch (Exception e) {
      throw new IOException(e);
    } 
  }
  
  public int read() throws IOException {
    int bytesRead = this.in.read(this.b1, 0, 1);
    if (bytesRead >= 0) {
      this.pos++;
      return this.b1[0] & 0xFF;
    } 
    return -1;
  }
  
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    if (this.pos >= this.endPos && this.endPos >= 0L)
      return -1; 
    while (true) {
      try {
        int i = this.in.read(b, off, len);
        if (i > 0)
          this.pos += i; 
        if (this.endPos > 0L && this.pos > this.endPos)
          i = (int)(i - this.pos - this.endPos); 
        if (i < 0)
          close(); 
        return i;
      } catch (Exception e) {
        e.printStackTrace();
        try {
          Thread.sleep((this.reconnectDelay * 1000));
          reconnect();
        } catch (Exception exception) {
          exception.printStackTrace();
        } 
      } 
    } 
  }
  
  private void reconnect() throws Exception {
    try {
      this.c.logout();
    } catch (Exception exception) {}
    this.c.login(this.c.getConfig("username", ""), this.c.getConfig("password", ""), this.c.getConfig("clientid", ""));
    this.in = this.c.download(this.path, this.pos, this.endPos, this.binary);
  }
  
  public void close() throws IOException {
    if (this.closed)
      return; 
    this.in.close();
    this.closed = true;
  }
}
