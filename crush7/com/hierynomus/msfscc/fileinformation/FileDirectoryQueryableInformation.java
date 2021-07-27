package com.hierynomus.msfscc.fileinformation;

public abstract class FileDirectoryQueryableInformation implements FileInformation {
  private final String fileName;
  
  private long nextOffset;
  
  private long fileIndex;
  
  FileDirectoryQueryableInformation(long nextOffset, long fileIndex, String fileName) {
    this.nextOffset = nextOffset;
    this.fileIndex = fileIndex;
    this.fileName = fileName;
  }
  
  public long getNextOffset() {
    return this.nextOffset;
  }
  
  public long getFileIndex() {
    return this.fileIndex;
  }
  
  public String getFileName() {
    return this.fileName;
  }
}
