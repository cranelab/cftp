package com.crushftp.tunnel2;

import com.crushftp.client.Common;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class DProperties {
  static Vector freeBytes = new Vector();
  
  Properties p = new Properties();
  
  boolean diskInitialized = false;
  
  RandomAccessFile tmp = null;
  
  String tmpName = null;
  
  Vector used = new Vector();
  
  Vector free = new Vector();
  
  long maxLoc = 0L;
  
  byte[] tmpB = null;
  
  boolean usingDisk = false;
  
  static int chunkSize = 65548;
  
  static byte[] zeros = new byte[chunkSize];
  
  public DProperties() {
    this.tmpB = getArray();
  }
  
  public static byte[] getArray() {
    synchronized (freeBytes) {
      if (freeBytes.size() > 0)
        return freeBytes.remove(0); 
      return new byte[chunkSize];
    } 
  }
  
  public static byte[] releaseArray(byte[] b) {
    if (b == null)
      return null; 
    if (b.length != chunkSize)
      return null; 
    System.arraycopy(zeros, 0, b, 0, b.length);
    freeBytes.addElement(b);
    return null;
  }
  
  public Chunk get(String key) throws IOException {
    Object o = this.p.get(key);
    if (o instanceof String) {
      init();
      Chunk c = null;
      synchronized (this.tmp) {
        long offset = Long.parseLong(o.toString());
        this.tmp.seek(offset);
        this.tmp.readFully(this.tmpB);
        ByteArrayInputStream bis = new ByteArrayInputStream(this.tmpB);
        c = Chunk.parse(bis);
        bis.close();
      } 
      return c;
    } 
    return (Chunk)o;
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
  
  public synchronized void put(String key, Chunk c) throws IOException {
    if (Tunnel2.ramUsage <= Tunnel2.maxRam || !this.usingDisk);
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
        this.p.put(key, (new StringBuffer(String.valueOf(offset))).toString());
      } 
    } else {
      if (this.usingDisk)
        Tunnel2.msg("DP:Changing back to ram cache:" + Tunnel2.ramUsage + " < " + Tunnel2.maxRam); 
      this.usingDisk = false;
      this.p.put(key, c);
      Tunnel2.addRam(c.len);
    } 
  }
  
  public synchronized Chunk remove(String key) throws IOException {
    Object o = this.p.remove(key);
    if (o == null)
      return null; 
    if (o instanceof String) {
      init();
      Chunk chunk = null;
      synchronized (this.tmp) {
        long offset = Long.parseLong(o.toString());
        this.tmp.seek(offset);
        this.tmp.readFully(this.tmpB);
        ByteArrayInputStream bis = new ByteArrayInputStream(this.tmpB);
        chunk = Chunk.parse(bis);
        bis.close();
        this.used.remove((new StringBuffer(String.valueOf(offset))).toString());
        this.free.addElement((new StringBuffer(String.valueOf(offset))).toString());
      } 
      return chunk;
    } 
    Chunk c = (Chunk)o;
    Tunnel2.removeRam(c.len);
    return c;
  }
  
  public int size() {
    return this.p.size();
  }
  
  public boolean containsKey(String key) {
    return this.p.containsKey(key);
  }
  
  public Enumeration keys() {
    return this.p.keys();
  }
  
  public synchronized void close() {
    try {
      if (this.diskInitialized) {
        this.tmp.close();
        (new File(this.tmpName)).delete();
        this.diskInitialized = false;
      } 
    } catch (Exception exception) {}
    Enumeration keys = keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      Object o = this.p.remove(key);
      if (!(o instanceof String)) {
        Chunk c = (Chunk)o;
        Tunnel2.removeRam(c.len);
      } 
    } 
    this.tmpB = releaseArray(this.tmpB);
    this.used.clear();
    this.free.clear();
  }
}
