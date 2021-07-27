package com.hierynomus.msfscc.fileinformation;

public class FileLinkInformation extends FileRenameInformation {
  public FileLinkInformation(boolean replaceIfExists, String fileName) {
    super(replaceIfExists, 0L, fileName);
  }
}
