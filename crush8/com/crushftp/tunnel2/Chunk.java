package com.crushftp.tunnel2;

import com.crushftp.tunnel3.StreamWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class Chunk {
  public int id = -1;
  
  public byte[] b = null;
  
  public int len = 0;
  
  public int num = -1;
  
  String command = "";
  
  public long time = System.currentTimeMillis();
  
  public StreamWriter sw = null;
  
  public Chunk(int id, byte[] b, int len, int num) {
    this.id = id;
    this.b = b;
    this.len = len;
    this.num = num;
    if (num < 0)
      try {
        this.command = new String(b, 0, len, "UTF8");
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }  
  }
  
  public Chunk() {}
  
  public Chunk(byte[] b) {
    this.b = b;
  }
  
  public String getCommand() {
    return this.command;
  }
  
  public boolean isCommand() {
    return (this.num < 0);
  }
  
  public static Chunk parse(InputStream in) throws IOException {
    byte[] header = new byte[12];
    int totalBytes = 0;
    int bytesRead = 0;
    while (totalBytes < 12) {
      bytesRead = in.read(header, totalBytes, 12 - totalBytes);
      if (bytesRead < 0)
        return null; 
      totalBytes += bytesRead;
    } 
    Chunk c = new Chunk();
    int loc = 0;
    c.id = (header[loc++] & 0xFF) << 24 | (header[loc++] & 0xFF) << 16 | (header[loc++] & 0xFF) << 8 | header[loc++] & 0xFF;
    c.num = (header[loc++] & 0xFF) << 24 | (header[loc++] & 0xFF) << 16 | (header[loc++] & 0xFF) << 8 | header[loc++] & 0xFF;
    c.len = (header[loc++] & 0xFF) << 24 | (header[loc++] & 0xFF) << 16 | (header[loc++] & 0xFF) << 8 | header[loc++] & 0xFF;
    header = (byte[])null;
    if (c.len > 65535)
      throw new IOException("Invalid chunk received, size is too big:" + c.len); 
    c.b = new byte[c.len];
    totalBytes = 0;
    bytesRead = 0;
    while (bytesRead >= 0 && totalBytes < c.len) {
      bytesRead = in.read(c.b, totalBytes, c.len - totalBytes);
      if (bytesRead >= 0) {
        totalBytes += bytesRead;
        continue;
      } 
      return null;
    } 
    if (c.num < 0)
      c.command = new String(c.b, "UTF8"); 
    return c;
  }
  
  public byte[] toBytes() {
    byte[] bb = new byte[12 + this.len];
    System.arraycopy(new byte[] { (byte)(this.id >>> 24), (byte)(this.id >>> 16), (byte)(this.id >>> 8), (byte)this.id }, 0, bb, 0, 4);
    System.arraycopy(new byte[] { (byte)(this.num >>> 24), (byte)(this.num >>> 16), (byte)(this.num >>> 8), (byte)this.num }, 0, bb, 4, 4);
    System.arraycopy(new byte[] { (byte)(this.len >>> 24), (byte)(this.len >>> 16), (byte)(this.len >>> 8), (byte)this.len }, 0, bb, 8, 4);
    System.arraycopy(this.b, 0, bb, 12, this.len);
    return bb;
  }
  
  public String toString() {
    if (this.num > 0)
      return String.valueOf(this.id) + ":" + this.num + ":" + this.len; 
    return String.valueOf(this.id) + ":" + this.num + ":" + this.command;
  }
}
