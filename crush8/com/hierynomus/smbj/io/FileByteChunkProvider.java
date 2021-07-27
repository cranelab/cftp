package com.hierynomus.smbj.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileByteChunkProvider extends ByteChunkProvider {
  private File file;
  
  private InputStreamByteChunkProvider underlyingProvider;
  
  public FileByteChunkProvider(File file) throws IOException {
    this(file, 0L);
  }
  
  public FileByteChunkProvider(File file, long offset) throws IOException {
    this.file = file;
    FileInputStream fis = new FileInputStream(file);
    this.underlyingProvider = new InputStreamByteChunkProvider(fis);
    ensureSkipped(fis, offset);
    this.offset = offset;
  }
  
  private void ensureSkipped(FileInputStream fis, long offset) throws IOException {
    long skipped = 0L;
    while (skipped < offset && fis.available() > 0)
      skipped += fis.skip(offset); 
    if (skipped < offset)
      throw new IOException("Was unable to go to the requested offset of " + offset + " of file " + this.file); 
  }
  
  protected int getChunk(byte[] chunk) throws IOException {
    return this.underlyingProvider.getChunk(chunk);
  }
  
  public int bytesLeft() {
    return this.underlyingProvider.bytesLeft();
  }
  
  public boolean isAvailable() {
    return this.underlyingProvider.isAvailable();
  }
  
  public void close() throws IOException {
    this.underlyingProvider.close();
  }
}
