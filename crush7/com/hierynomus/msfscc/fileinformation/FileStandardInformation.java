package com.hierynomus.msfscc.fileinformation;

public class FileStandardInformation implements FileQueryableInformation {
  private long allocationSize;
  
  private long endOfFile;
  
  private long numberOfLinks;
  
  private boolean deletePending;
  
  private boolean directory;
  
  FileStandardInformation(long allocationSize, long endOfFile, long numberOfLinks, boolean deletePending, boolean directory) {
    this.allocationSize = allocationSize;
    this.endOfFile = endOfFile;
    this.numberOfLinks = numberOfLinks;
    this.deletePending = deletePending;
    this.directory = directory;
  }
  
  public long getAllocationSize() {
    return this.allocationSize;
  }
  
  public long getEndOfFile() {
    return this.endOfFile;
  }
  
  public long getNumberOfLinks() {
    return this.numberOfLinks;
  }
  
  public boolean isDeletePending() {
    return this.deletePending;
  }
  
  public boolean isDirectory() {
    return this.directory;
  }
}
