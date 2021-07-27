package com.hierynomus.msfscc.fileinformation;

public class FileInternalInformation implements FileQueryableInformation {
  private long indexNumber;
  
  FileInternalInformation(long eaSize) {
    this.indexNumber = eaSize;
  }
  
  public long getIndexNumber() {
    return this.indexNumber;
  }
}
