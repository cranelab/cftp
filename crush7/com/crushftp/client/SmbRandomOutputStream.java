package com.crushftp.client;

import java.io.IOException;
import java.io.OutputStream;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbRandomAccessFile;

public class SmbRandomOutputStream extends OutputStream {
  SmbRandomAccessFile raf = null;
  
  Object rafLock = new Object();
  
  long pos = 0L;
  
  boolean shared = false;
  
  public SmbRandomOutputStream(SmbFile f, boolean shared) throws IOException {
    this.shared = shared;
    this.raf = new SmbRandomAccessFile(f, "rw");
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
      this.raf.seek(this.pos);
      this.raf.write(b, start, len);
      this.pos = this.raf.getFilePointer();
    } 
  }
  
  public void close() throws IOException {
    if (this.raf == null)
      return; 
    this.raf.close();
    this.raf = null;
  }
}
