package com.hierynomus.smbj.io;

import com.hierynomus.smbj.common.SMBRuntimeException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileByteChunkProvider extends ByteChunkProvider {
  private File file;
  
  private BufferedInputStream fis;
  
  public FileByteChunkProvider(File file) throws FileNotFoundException {
    this.file = file;
    this.fis = new BufferedInputStream(new FileInputStream(file), 65536);
  }
  
  public FileByteChunkProvider(File file, long offset) throws IOException {
    this.file = file;
    this.fis = new BufferedInputStream(new FileInputStream(file), 65536);
    ensureSkipped(this.fis, offset);
    this.offset = offset;
  }
  
  private void ensureSkipped(BufferedInputStream fis, long offset) throws IOException {
    long skipped = 0L;
    while (skipped < offset && fis.available() > 0)
      skipped += fis.skip(offset); 
    if (skipped < offset)
      throw new IOException("Was unable to go to the requested offset of " + offset + " of file " + this.file); 
  }
  
  protected int getChunk(byte[] chunk) throws IOException {
    int count = 0;
    int read = 0;
    while (count < 65536 && (read = this.fis.read(chunk, count, 65536 - count)) != -1)
      count += read; 
    return count;
  }
  
  public int bytesLeft() {
    try {
      return this.fis.available();
    } catch (IOException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  public boolean isAvailable() {
    return (bytesLeft() > 0);
  }
}
