package com.crushftp.tunnel;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class FileArchiveOutputStream extends ZipArchiveOutputStream {
  OutputStream out = null;
  
  String enc = "UTF8";
  
  boolean zip64 = false;
  
  int level = 0;
  
  Enum zip64Mode = null;
  
  boolean zip = false;
  
  public FileArchiveOutputStream(OutputStream out, boolean zip) throws IOException {
    super(out);
    this.out = out;
    this.zip = zip;
  }
  
  public void flush() throws IOException {
    if (this.zip) {
      super.flush();
    } else {
      this.out.flush();
    } 
  }
  
  public void write(int i) throws IOException {
    if (this.zip) {
      super.write(new byte[] { (byte)i }, 0, 1);
    } else {
      write(new byte[] { (byte)i }, 0, 1);
    } 
  }
  
  public void write(byte[] b) throws IOException {
    if (this.zip)
      super.write(b, 0, b.length); 
    write(b, 0, b.length);
  }
  
  public void write(byte[] b, int start, int len) throws IOException {
    if (this.zip) {
      super.write(b, start, len);
    } else {
      String l = String.valueOf(len - start) + ":";
      this.out.write(l.getBytes(this.enc));
      this.out.write(b, start, len);
    } 
  }
  
  public void close() throws IOException {
    if (this.zip) {
      super.close();
    } else {
      this.out.close();
    } 
  }
  
  public void setEncoding(String enc) {
    if (this.zip) {
      super.setEncoding(enc);
    } else {
      this.enc = enc;
    } 
  }
  
  public void putArchiveEntry(FileArchiveEntry ze) throws IOException {
    if (this.zip) {
      putArchiveEntry((ArchiveEntry)ze);
    } else {
      this.out.write(ze.toString().getBytes(this.enc));
    } 
  }
  
  public void closeArchiveEntry() throws IOException {
    if (this.zip) {
      super.closeArchiveEntry();
    } else {
      this.out.write("-1:".getBytes(this.enc));
    } 
  }
  
  public void finish() throws IOException {
    if (this.zip)
      super.finish(); 
  }
}
