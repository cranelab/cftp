package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum SMB2CompletionFilter implements EnumWithValue<SMB2CompletionFilter> {
  FILE_NOTIFY_CHANGE_FILE_NAME(1L),
  FILE_NOTIFY_CHANGE_DIR_NAME(2L),
  FILE_NOTIFY_CHANGE_ATTRIBUTES(4L),
  FILE_NOTIFY_CHANGE_SIZE(8L),
  FILE_NOTIFY_CHANGE_LAST_WRITE(16L),
  FILE_NOTIFY_CHANGE_LAST_ACCESS(32L),
  FILE_NOTIFY_CHANGE_CREATION(64L),
  FILE_NOTIFY_CHANGE_EA(128L),
  FILE_NOTIFY_CHANGE_SECURITY(256L),
  FILE_NOTIFY_CHANGE_STREAM_NAME(512L),
  FILE_NOTIFY_CHANGE_STREAM_SIZE(1024L),
  FILE_NOTIFY_CHANGE_STREAM_WRITE(2048L);
  
  private long value;
  
  SMB2CompletionFilter(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
