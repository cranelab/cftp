package com.hierynomus.smbj.common;

public class SmbPath {
  private final String hostname;
  
  private final String shareName;
  
  private final String path;
  
  public SmbPath(String hostname) {
    this(hostname, null, null);
  }
  
  public SmbPath(String hostname, String shareName) {
    this(hostname, shareName, null);
  }
  
  public SmbPath(String hostname, String shareName, String path) {
    this.shareName = shareName;
    this.hostname = hostname;
    this.path = path;
  }
  
  public SmbPath(SmbPath parent, String path) {
    this.hostname = parent.hostname;
    if (parent.shareName != null) {
      this.shareName = parent.shareName;
    } else {
      throw new IllegalArgumentException("Can only make child SmbPath of fully specified SmbPath");
    } 
    if (parent.path != null) {
      this.path = String.valueOf(parent.path) + "\\" + path;
    } else {
      this.path = path;
    } 
  }
  
  public String toUncPath() {
    StringBuilder b = new StringBuilder("\\\\");
    b.append(this.hostname);
    if (this.shareName != null) {
      if (this.shareName.charAt(0) != '\\')
        b.append("\\"); 
      b.append(this.shareName);
      if (this.path != null)
        b.append("\\").append(this.path); 
    } 
    return b.toString();
  }
  
  public String toString() {
    return toUncPath();
  }
  
  public static SmbPath parse(String path) {
    String splitPath = path;
    if (path.charAt(0) == '\\')
      if (path.charAt(1) == '\\') {
        splitPath = path.substring(2);
      } else {
        splitPath = path.substring(1);
      }  
    String[] split = splitPath.split("\\\\", 3);
    if (split.length == 1)
      return new SmbPath(split[0]); 
    if (split.length == 2)
      return new SmbPath(split[0], split[1]); 
    return new SmbPath(split[0], split[1], split[2]);
  }
  
  public String getHostname() {
    return this.hostname;
  }
  
  public String getShareName() {
    return this.shareName;
  }
  
  public String getPath() {
    return this.path;
  }
}
