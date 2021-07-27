package com.hierynomus.smbj.common;

import com.hierynomus.protocol.commons.Objects;
import com.hierynomus.utils.Strings;

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
    this.path = rewritePath(path);
  }
  
  private static String rewritePath(String path) {
    return Strings.isNotBlank(path) ? path.replace('/', '\\') : path;
  }
  
  public SmbPath(SmbPath parent, String path) {
    this.hostname = parent.hostname;
    if (Strings.isNotBlank(parent.shareName)) {
      this.shareName = parent.shareName;
    } else {
      throw new IllegalArgumentException("Can only make child SmbPath of fully specified SmbPath");
    } 
    if (Strings.isNotBlank(parent.path)) {
      parent.path += "\\" + rewritePath(path);
    } else {
      this.path = rewritePath(path);
    } 
  }
  
  public String toUncPath() {
    StringBuilder b = new StringBuilder("\\\\");
    b.append(this.hostname);
    if (this.shareName != null && !this.shareName.isEmpty()) {
      if (this.shareName.charAt(0) != '\\')
        b.append("\\"); 
      b.append(this.shareName);
      if (Strings.isNotBlank(this.path))
        b.append("\\").append(this.path); 
    } 
    return b.toString();
  }
  
  public String toString() {
    return toUncPath();
  }
  
  public static SmbPath parse(String path) {
    String rewritten = rewritePath(path);
    String splitPath = rewritten;
    if (rewritten.charAt(0) == '\\')
      if (rewritten.charAt(1) == '\\') {
        splitPath = rewritten.substring(2);
      } else {
        splitPath = rewritten.substring(1);
      }  
    String[] split = splitPath.split("\\\\", 3);
    if (split.length == 1)
      return new SmbPath(split[0]); 
    if (split.length == 2)
      return new SmbPath(split[0], split[1]); 
    return new SmbPath(split[0], split[1], split[2]);
  }
  
  public boolean equals(Object o) {
    if (this == o)
      return true; 
    if (o == null || getClass() != o.getClass())
      return false; 
    SmbPath smbPath = (SmbPath)o;
    return (Objects.equals(this.hostname, smbPath.hostname) && 
      Objects.equals(this.shareName, smbPath.shareName) && 
      Objects.equals(this.path, smbPath.path));
  }
  
  public int hashCode() {
    return Objects.hash(new Object[] { this.hostname, this.shareName, this.path });
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
  
  public boolean isOnSameHost(SmbPath other) {
    return (other != null && Objects.equals(this.hostname, other.hostname));
  }
  
  public boolean isOnSameShare(SmbPath other) {
    return (isOnSameHost(other) && Objects.equals(this.shareName, other.shareName));
  }
}
