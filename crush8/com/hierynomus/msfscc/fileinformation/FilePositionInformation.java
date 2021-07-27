package com.hierynomus.msfscc.fileinformation;

public class FilePositionInformation implements FileQueryableInformation {
  private long currentByteOffset;
  
  FilePositionInformation(long currentByteOffset) {
    this.currentByteOffset = currentByteOffset;
  }
  
  public long getCurrentByteOffset() {
    return this.currentByteOffset;
  }
}
