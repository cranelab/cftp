package com.crushftp.client;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class RandomOutputStream extends OutputStream {
  public static final Properties sharedOutput = new Properties();
  
  public static final Properties sharedOutputCount = new Properties();
  
  String filePath = null;
  
  RandomAccessFile raf = null;
  
  Object rafLock = new Object();
  
  long pos = 0L;
  
  boolean shared = false;
  
  public MessageDigest md5 = null;
  
  public String lastMd5 = "";
  
  public RandomOutputStream(File f, boolean shared) throws IOException {
    this.filePath = f.getCanonicalPath();
    this.shared = shared;
    try {
      if (this.md5 == null)
        this.md5 = MessageDigest.getInstance(System.getProperty("crushftp.hash_algorithm", "MD5")); 
    } catch (NoSuchAlgorithmException noSuchAlgorithmException) {}
    this.md5.reset();
    this.raf = (RandomAccessFile)sharedOutput.get(this.filePath);
    if (!shared && this.raf != null)
      throw new IOException("File in use:" + f); 
    if (this.raf == null)
      this.raf = new RandomAccessFile(new File_U(this.filePath), "rw"); 
    sharedOutput.put(this.filePath, this.raf);
    synchronized (this.rafLock) {
      sharedOutputCount.put(this.filePath, (new StringBuffer(String.valueOf(Integer.parseInt(sharedOutputCount.getProperty(this.filePath, "0")) + 1))).toString());
    } 
    this.pos = this.raf.getFilePointer();
  }
  
  public void append() throws IOException {
    if (this.raf == null)
      return; 
    synchronized (this.rafLock) {
      seek(length());
    } 
  }
  
  public void setLength(long l) throws IOException {
    if (this.raf == null)
      return; 
    synchronized (this.rafLock) {
      this.raf.setLength(l);
      this.pos = this.raf.getFilePointer();
    } 
  }
  
  public void flush() {}
  
  public void seek(long l) throws IOException {
    if (this.raf == null)
      return; 
    synchronized (this.rafLock) {
      this.raf.seek(l);
      this.pos = this.raf.getFilePointer();
    } 
  }
  
  public long length() throws IOException {
    return this.raf.length();
  }
  
  public void write(int i) throws IOException {
    write(new byte[] { (byte)i }, 0, 1);
  }
  
  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }
  
  public void write(byte[] b, int start, int len) throws IOException {
    if (this.raf == null)
      return; 
    synchronized (this.rafLock) {
      this.md5.update(b, start, len);
      this.raf.seek(this.pos);
      this.raf.write(b, start, len);
      this.pos = this.raf.getFilePointer();
    } 
  }
  
  public String getLastMd5() {
    return this.lastMd5;
  }
  
  public void close() throws IOException {
    if (this.raf == null)
      return; 
    synchronized (this.rafLock) {
      int i = Integer.parseInt(sharedOutputCount.getProperty(this.filePath, "0")) - 1;
      if (i <= 0 || !this.shared) {
        sharedOutputCount.remove(this.filePath);
        this.raf.close();
        sharedOutput.remove(this.filePath);
      } else {
        sharedOutputCount.put(this.filePath, (new StringBuffer(String.valueOf(i))).toString());
      } 
      this.raf = null;
      this.lastMd5 = (new BigInteger(1, this.md5.digest())).toString(16).toLowerCase();
      if (this.lastMd5.length() < 32)
        this.lastMd5 = "0" + this.lastMd5; 
    } 
  }
}
