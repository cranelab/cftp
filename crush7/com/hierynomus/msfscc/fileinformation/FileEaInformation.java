package com.hierynomus.msfscc.fileinformation;

public class FileEaInformation implements FileQueryableInformation {
  private long eaSize;
  
  FileEaInformation(long eaSize) {
    this.eaSize = eaSize;
  }
  
  public long getEaSize() {
    return this.eaSize;
  }
}
