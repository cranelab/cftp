package com.hierynomus.msfscc.fileinformation;

public class FileRenameInformation implements FileSettableInformation {
  private final boolean replaceIfExists;
  
  private final long rootDirectory;
  
  private final int fileNameLength;
  
  private final String fileName;
  
  public FileRenameInformation(boolean replaceIfExists, long rootDirectory, String fileName) {
    this.replaceIfExists = replaceIfExists;
    this.rootDirectory = rootDirectory;
    this.fileNameLength = fileName.length();
    this.fileName = fileName;
  }
  
  public boolean isReplaceIfExists() {
    return this.replaceIfExists;
  }
  
  public long getRootDirectory() {
    return this.rootDirectory;
  }
  
  public int getFileNameLength() {
    return this.fileNameLength;
  }
  
  public String getFileName() {
    return this.fileName;
  }
}
