package com.hierynomus.msfscc.fileinformation;

public class FileModeInformation implements FileQueryableInformation, FileSettableInformation {
  private int mode;
  
  public FileModeInformation(int mode) {
    this.mode = mode;
  }
  
  public int getMode() {
    return this.mode;
  }
}
