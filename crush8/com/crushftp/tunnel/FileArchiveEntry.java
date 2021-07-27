package com.crushftp.tunnel;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

public class FileArchiveEntry extends ZipArchiveEntry {
  boolean isDir = false;
  
  public FileArchiveEntry(String name) {
    super(name);
  }
  
  public void setDirectory(boolean isDir) {
    this.isDir = isDir;
  }
  
  public boolean isDirectory() {
    return !(!getName().endsWith("/") && !this.isDir);
  }
  
  public String toString() {
    String s = "TYPE:" + (isDirectory() ? "D" : "F") + "\r\n";
    s = String.valueOf(s) + "NAME:" + getName() + "\r\n";
    s = String.valueOf(s) + "TIME:" + getTime() + "\r\n";
    s = String.valueOf(s) + "DATA:\r\n";
    return s;
  }
  
  public static FileArchiveEntry parseObj(BufferedInputStream in) throws IOException {
    in.mark(32786);
    byte[] b = new byte[8192];
    String headerStr = "";
    while (headerStr.indexOf("DATA:\r\n") < 0) {
      int bytesRead = in.read(b);
      if (bytesRead > 0)
        headerStr = String.valueOf(headerStr) + new String(b, 0, bytesRead); 
      if (bytesRead < 0)
        return null; 
    } 
    in.reset();
    String partial = headerStr.substring(0, headerStr.indexOf("DATA:\r\n"));
    in.skip(((partial.getBytes("UTF8")).length + "DATA:\r\n".length()));
    headerStr = headerStr.substring(0, headerStr.indexOf("DATA:\r\n"));
    BufferedReader br = new BufferedReader(new StringReader(headerStr));
    String data = "";
    FileArchiveEntry ze = new FileArchiveEntry("");
    while ((data = br.readLine()) != null) {
      if (data.split(":")[0].equals("TYPE")) {
        ze.setDirectory(data.split(":")[1].equals("D"));
        continue;
      } 
      if (data.split(":")[0].equals("NAME")) {
        ze.setName(data.substring(data.indexOf(":") + 1));
        continue;
      } 
      if (data.split(":")[0].equals("TIME"))
        ze.setTime(Long.parseLong(data.split(":")[1])); 
    } 
    return ze;
  }
}
