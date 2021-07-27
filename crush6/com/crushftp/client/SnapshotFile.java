package com.crushftp.client;

import java.io.File;
import java.net.URI;
import java.util.Properties;

public class SnapshotFile extends File {
  private static final long serialVersionUID = 1L;
  
  public long snapshotModified = 0L;
  
  public long snapshotSize = 0L;
  
  boolean isDir = false;
  
  public Properties info = null;
  
  public SnapshotFile(File f) {
    super(f.getPath());
    snapshot();
  }
  
  public SnapshotFile(String s) {
    super(s);
    snapshot();
  }
  
  public SnapshotFile(URI u) {
    super(u);
    snapshot();
  }
  
  public void snapshot() {
    this.snapshotModified = super.lastModified() / 1000L * 1000L;
    this.snapshotSize = super.length();
    this.isDir = super.isDirectory();
  }
  
  public void put(String key, String val) {
    if (this.info == null)
      this.info = new Properties(); 
    this.info.put(key, val);
  }
  
  public String get(String key) {
    if (this.info == null)
      this.info = new Properties(); 
    return this.info.getProperty(key);
  }
  
  public String get(String key, String defaultVal) {
    if (this.info == null)
      this.info = new Properties(); 
    return this.info.getProperty(key, defaultVal);
  }
  
  public boolean isDirectory() {
    return this.isDir;
  }
  
  public boolean isFile() {
    return !this.isDir;
  }
  
  public long length() {
    return this.snapshotSize;
  }
  
  public long lastModified() {
    return this.snapshotModified / 1000L * 1000L;
  }
  
  public File[] listFiles() {
    File[] files = super.listFiles();
    SnapshotFile[] files2 = new SnapshotFile[files.length];
    for (int x = 0; x < files.length; x++)
      files2[x] = new SnapshotFile(files[x]); 
    return (File[])files2;
  }
  
  public boolean equals(Object o) {
    return super.equals(o);
  }
  
  public int hashCode() {
    return super.hashCode();
  }
}
