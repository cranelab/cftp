package com.hierynomus.msfscc.fileinformation;

import com.hierynomus.msdtyp.FileTime;

public class FileBasicInformation implements FileQueryableInformation, FileSettableInformation {
  public static final FileTime DONT_SET = new FileTime(0L);
  
  public static final FileTime DONT_UPDATE = new FileTime(-1L);
  
  private final FileTime creationTime;
  
  private final FileTime lastAccessTime;
  
  private final FileTime lastWriteTime;
  
  private final FileTime changeTime;
  
  private long fileAttributes;
  
  public FileBasicInformation(FileTime creationTime, FileTime lastAccessTime, FileTime lastWriteTime, FileTime changeTime, long fileAttributes) {
    this.creationTime = creationTime;
    this.lastAccessTime = lastAccessTime;
    this.lastWriteTime = lastWriteTime;
    this.changeTime = changeTime;
    this.fileAttributes = fileAttributes;
  }
  
  public FileTime getCreationTime() {
    return this.creationTime;
  }
  
  public FileTime getLastAccessTime() {
    return this.lastAccessTime;
  }
  
  public FileTime getLastWriteTime() {
    return this.lastWriteTime;
  }
  
  public FileTime getChangeTime() {
    return this.changeTime;
  }
  
  public long getFileAttributes() {
    return this.fileAttributes;
  }
}
