package com.hierynomus.msfscc.fileinformation;

public class FileAllocationInformation implements FileSettableInformation {
  private long allocationSize;
  
  public FileAllocationInformation(long allocationSize) {
    this.allocationSize = allocationSize;
  }
  
  public long getAllocationSize() {
    return this.allocationSize;
  }
}
