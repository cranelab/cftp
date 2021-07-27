package com.hierynomus.msfscc.fileinformation;

public class FileAccessInformation implements FileQueryableInformation {
  private int accessFlags;
  
  FileAccessInformation(int accessFlags) {
    this.accessFlags = accessFlags;
  }
  
  public int getAccessFlags() {
    return this.accessFlags;
  }
}
