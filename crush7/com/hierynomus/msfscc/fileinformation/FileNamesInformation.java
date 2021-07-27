package com.hierynomus.msfscc.fileinformation;

public class FileNamesInformation extends FileDirectoryQueryableInformation {
  FileNamesInformation(long nextOffset, long fileIndex, String fileName) {
    super(nextOffset, fileIndex, fileName);
  }
}
