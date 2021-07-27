package com.crushftp.tunnel2;

import com.crushftp.client.Common;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

public class DVector {
  Vector v = new Vector();
  
  boolean diskInitialized = false;
  
  RandomAccessFile tmp = null;
  
  String tmpName = null;
  
  Vector used = new Vector();
  
  Vector free = new Vector();
  
  long maxLoc = 0L;
  
  byte[] tmpB = null;
  
  long bytes = 0L;
  
  boolean usingDisk = false;
  
  public DVector() {
    this.tmpB = DProperties.getArray();
  }
  
  public long getBytes() {
    return this.bytes;
  }
  
  public synchronized void init() throws IOException {
    this.usingDisk = true;
    if (this.diskInitialized)
      return; 
    this.tmpName = String.valueOf(System.getProperty("crushftp.tunnel.temp.path", System.getProperty("java.io.tmpdir"))) + "Tunnel2_" + System.currentTimeMillis() + "_" + Common.makeBoundary(5) + ".tmp";
    Tunnel2.msg("Ram cache exceeded, using scratch file on disk:" + this.tmpName + "   " + Tunnel2.ramUsage + " > " + Tunnel2.maxRam + " Free JVM Memory:" + Common.format_bytes_short(Common.getFreeRam()));
    this.tmp = new RandomAccessFile(this.tmpName, "rw");
    this.tmp.setLength(0L);
    this.diskInitialized = true;
  }
  
  public synchronized void insertElementAt(Chunk c, int i) throws IOException {
    this.bytes += c.len;
    if (Tunnel2.ramUsage > Tunnel2.maxRam);
    if (Tunnel2.ramUsage > Tunnel2.maxRam || Common.getFreeRam() < 33554432L) {
      init();
      synchronized (this.tmp) {
        long offset = 0L;
        if (this.free.size() == 0) {
          this.free.addElement((new StringBuffer(String.valueOf(this.maxLoc))).toString());
          this.maxLoc += this.tmpB.length;
        } 
        offset = Long.parseLong(this.free.remove(0).toString());
        this.used.addElement((new StringBuffer(String.valueOf(offset))).toString());
        this.tmp.seek(offset);
        byte[] b = c.toBytes();
        System.arraycopy(b, 0, this.tmpB, 0, b.length);
        this.tmp.write(this.tmpB);
        this.v.insertElementAt((new StringBuffer(String.valueOf(offset))).toString(), i);
      } 
    } else {
      if (this.usingDisk)
        Tunnel2.msg("DV:Changing back to ram cache:" + Tunnel2.ramUsage + " < " + Tunnel2.maxRam); 
      this.usingDisk = false;
      this.v.insertElementAt(c, i);
      Tunnel2.addRam(c.len);
    } 
  }
  
  public synchronized void addElement(Chunk c) throws IOException {
    insertElementAt(c, this.v.size());
  }
  
  public synchronized Chunk remove(int i) throws IOException {
    Object o = this.v.remove(i);
    Chunk c = null;
    if (o == null)
      return null; 
    if (o instanceof String) {
      init();
      synchronized (this.tmp) {
        long offset = Long.parseLong(o.toString());
        this.tmp.seek(offset);
        this.tmp.readFully(this.tmpB);
        ByteArrayInputStream bis = new ByteArrayInputStream(this.tmpB);
        c = Chunk.parse(bis);
        bis.close();
        this.used.remove((new StringBuffer(String.valueOf(offset))).toString());
        this.free.addElement((new StringBuffer(String.valueOf(offset))).toString());
      } 
    } else {
      c = (Chunk)o;
      Tunnel2.removeRam(c.len);
    } 
    this.bytes -= c.len;
    return c;
  }
  
  public int size() {
    return this.v.size();
  }
  
  public void close() {
    try {
      if (this.diskInitialized) {
        this.tmp.close();
        (new File(this.tmpName)).delete();
        this.diskInitialized = false;
        this.usingDisk = false;
      } 
    } catch (Exception exception) {}
    this.tmpB = DProperties.releaseArray(this.tmpB);
  }
}
