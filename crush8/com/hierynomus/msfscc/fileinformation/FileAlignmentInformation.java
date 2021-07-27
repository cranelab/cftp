package com.hierynomus.msfscc.fileinformation;

public class FileAlignmentInformation implements FileQueryableInformation {
  private long alignmentRequirement;
  
  FileAlignmentInformation(long alignmentRequirement) {
    this.alignmentRequirement = alignmentRequirement;
  }
  
  public long getAlignmentRequirement() {
    return this.alignmentRequirement;
  }
}
