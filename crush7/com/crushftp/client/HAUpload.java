package com.crushftp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Vector;

public class HAUpload extends OutputStream {
  GenericClient c = null;
  
  String path = null;
  
  boolean binary = true;
  
  OutputStream out = null;
  
  boolean closed = false;
  
  long pos = 0L;
  
  byte[] b1 = new byte[1];
  
  int priorWriteCount = 0;
  
  int reconnectDelay = 10;
  
  Vector priorWrites = new Vector();
  
  public HAUpload(GenericClient c, String path, long startPos, boolean truncate, boolean binary, int priorWriteCount, int reconnectDelay) throws IOException {
    this.c = c;
    this.path = path;
    this.binary = binary;
    this.priorWriteCount = priorWriteCount;
    this.reconnectDelay = reconnectDelay;
    this.pos = startPos;
    try {
      this.out = c.upload2(path, startPos, truncate, binary);
    } catch (Exception e) {
      throw new IOException(e);
    } 
  }
  
  public void write(int i) throws IOException {
    this.b1[0] = (byte)i;
    write(this.b1, 0, 1);
  }
  
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }
  
  public void write(byte[] b, int off, int len) throws IOException {
    write(b, off, len, System.currentTimeMillis());
  }
  
  public void write(byte[] b, int off, int len, long start) throws IOException {
    Properties chunk = new Properties();
    chunk.put("b", b);
    chunk.put("off", (new StringBuffer(String.valueOf(off))).toString());
    chunk.put("len", (new StringBuffer(String.valueOf(len))).toString());
    chunk.put("pos", (new StringBuffer(String.valueOf(this.pos))).toString());
    this.priorWrites.addElement(chunk);
    while (this.priorWrites.size() > this.priorWriteCount)
      this.priorWrites.remove(0); 
    while (true) {
      Exception ee = null;
      try {
        this.out.write(b, off, len);
        this.pos += len;
        return;
      } catch (Exception e) {
        ee = e;
        Common.log("SERVER", 1, e);
        if (System.currentTimeMillis() - start > 1000L * Long.parseLong(this.c.getConfig("ha_timeout", "120"))) {
          Common.log("SERVER", 1, "HA Timeout:" + this.c.getConfig("ha_timeout", "120") + " seconds reached.");
          throw new IOException(ee);
        } 
        try {
          Thread.sleep((this.reconnectDelay * 1000));
          reconnect(start);
          return;
        } catch (Exception exception) {
          if (exception.getMessage().indexOf("history buffer") >= 0)
            throw new IOException(exception); 
          Common.log("SERVER", 1, exception);
        } 
      } 
    } 
  }
  
  private void reconnect(long start) throws Exception {
    try {
      this.closed = true;
      this.c.logout();
    } catch (Exception e) {
      Common.log("SERVER", 1, e);
    } 
    this.c.login(this.c.getConfig("username", ""), this.c.getConfig("password", ""), this.c.getConfig("clientid", ""));
    this.closed = false;
    Properties stat = this.c.stat(this.path);
    long serverSize = 0L;
    if (stat != null)
      serverSize = Long.parseLong(stat.getProperty("size", "0")); 
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (int x = this.priorWrites.size() - 1; x >= 0; x--) {
      Properties chunk = this.priorWrites.elementAt(x);
      long pos2 = Long.parseLong(chunk.getProperty("pos")) + 1L;
      if (this.c instanceof HTTPClient)
        pos2--; 
      if (pos2 <= serverSize) {
        int diff = (int)(serverSize - pos2);
        for (int xx = x; xx < this.priorWrites.size(); xx++) {
          chunk = this.priorWrites.elementAt(xx);
          byte[] arrayOfByte = (byte[])chunk.get("b");
          int off = Integer.parseInt(chunk.getProperty("off"));
          int len = Integer.parseInt(chunk.getProperty("len"));
          baos.write(arrayOfByte, off + diff, len - diff);
          diff = 0;
        } 
        this.pos = serverSize;
        this.out = this.c.upload(this.path, this.pos, false, this.binary);
        this.priorWrites.removeAllElements();
        byte[] b = baos.toByteArray();
        write(b, 0, b.length, start);
        return;
      } 
    } 
    throw new IOException("Unable to resume upload, not enough history buffer!");
  }
  
  public void close() throws IOException {
    if (this.closed)
      return; 
    long start = System.currentTimeMillis();
    while (true) {
      try {
        this.out.close();
        break;
      } catch (Exception e) {
        Common.log("SERVER", 1, e);
        try {
          Thread.sleep((this.reconnectDelay * 1000));
          reconnect(start);
        } catch (Exception exception) {
          Common.log("SERVER", 1, exception);
        } 
      } 
    } 
    this.closed = true;
  }
}
