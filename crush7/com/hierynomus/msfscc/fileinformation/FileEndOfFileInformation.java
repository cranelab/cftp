package com.hierynomus.msfscc.fileinformation;

public class FileEndOfFileInformation implements FileSettableInformation {
  private long endOfFile;
  
  public FileEndOfFileInformation(long endOfFile) throws IllegalArgumentException {
    if (endOfFile < 0L)
      throw new IllegalArgumentException("endOfFile MUST be greater than or equal to 0"); 
    this.endOfFile = endOfFile;
  }
  
  public long getEndOfFile() {
    return this.endOfFile;
  }
}
