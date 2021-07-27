package com.crushftp.tunnel;

import java.io.BufferedInputStream;
import java.io.IOException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

public class FileArchiveInputStream extends ZipArchiveInputStream {
  BufferedInputStream in = null;
  
  byte[] b1 = new byte[1];
  
  long chunkSize = 0L;
  
  boolean zip = false;
  
  public FileArchiveInputStream(BufferedInputStream in) throws IOException {
    super(in);
    in.mark(3);
    byte[] header = new byte[2];
    in.read(header, 0, 1);
    in.read(header, 1, 1);
    if ((new String(header)).startsWith("PK"))
      this.zip = true; 
    in.reset();
    this.in = in;
  }
  
  public int read() throws IOException {
    int bytesRead = read(this.b1);
    if (bytesRead >= 0)
      return this.b1[0]; 
    return -1;
  }
  
  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }
  
  public int read(byte[] b, int off, int len) throws IOException {
    if (this.zip)
      return super.read(b, off, len); 
    if (this.chunkSize == 0L) {
      String size = "";
      while (!size.endsWith(":")) {
        int i = this.in.read(this.b1);
        if (i == 1) {
          size = String.valueOf(size) + new String(this.b1);
          continue;
        } 
        return -1;
      } 
      this.chunkSize = Long.parseLong(size.substring(0, (size.getBytes("UTF8")).length - 1));
    } 
    if (this.chunkSize <= 0L)
      return -1; 
    if (len > this.chunkSize)
      len = (int)this.chunkSize; 
    int bytesRead = this.in.read(b, off, len);
    if (bytesRead >= 0)
      this.chunkSize -= bytesRead; 
    return bytesRead;
  }
  
  public ZipArchiveEntry getNextZipEntry() throws IOException {
    if (this.zip)
      return super.getNextZipEntry(); 
    this.chunkSize = 0L;
    return FileArchiveEntry.parseObj(this.in);
  }
  
  public void close() throws IOException {
    if (this.zip) {
      super.close();
    } else {
      this.in.close();
    } 
  }
}
