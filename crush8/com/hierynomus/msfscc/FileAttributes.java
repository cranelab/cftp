package com.hierynomus.msfscc;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum FileAttributes implements EnumWithValue<FileAttributes> {
  FILE_ATTRIBUTE_ARCHIVE(32L),
  FILE_ATTRIBUTE_COMPRESSED(2048L),
  FILE_ATTRIBUTE_DIRECTORY(16L),
  FILE_ATTRIBUTE_ENCRYPTED(16384L),
  FILE_ATTRIBUTE_HIDDEN(2L),
  FILE_ATTRIBUTE_NORMAL(128L),
  FILE_ATTRIBUTE_NOT_CONTENT_INDEXED(8192L),
  FILE_ATTRIBUTE_OFFLINE(4096L),
  FILE_ATTRIBUTE_READONLY(1L),
  FILE_ATTRIBUTE_REPARSE_POINT(1024L),
  FILE_ATTRIBUTE_SPARSE_FILE(512L),
  FILE_ATTRIBUTE_SYSTEM(4L),
  FILE_ATTRIBUTE_TEMPORARY(256L),
  FILE_ATTRIBUTE_INTEGRITY_STREAM(32768L),
  FILE_ATTRIBUTE_NO_SCRUB_DATA(131072L);
  
  private long value;
  
  FileAttributes(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
