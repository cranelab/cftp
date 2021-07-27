package com.hierynomus.msfscc.fileinformation;

public class FileStreamInformationItem {
  private long size;
  
  private long allocSize;
  
  private String name;
  
  public FileStreamInformationItem(long size, long allocSize, String name) {
    this.size = size;
    this.allocSize = allocSize;
    this.name = name;
  }
  
  public long getSize() {
    return this.size;
  }
  
  public long getAllocSize() {
    return this.allocSize;
  }
  
  public String getName() {
    return this.name;
  }
}
