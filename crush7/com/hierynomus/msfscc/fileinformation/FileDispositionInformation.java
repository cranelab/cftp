package com.hierynomus.msfscc.fileinformation;

public class FileDispositionInformation implements FileSettableInformation {
  private boolean deleteOnClose;
  
  public FileDispositionInformation() {
    this.deleteOnClose = true;
  }
  
  public FileDispositionInformation(boolean deleteOnClose) {
    this.deleteOnClose = deleteOnClose;
  }
  
  public boolean isDeleteOnClose() {
    return this.deleteOnClose;
  }
}
