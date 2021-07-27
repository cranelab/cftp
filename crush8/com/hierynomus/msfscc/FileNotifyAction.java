package com.hierynomus.msfscc;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum FileNotifyAction implements EnumWithValue<FileNotifyAction> {
  FILE_ACTION_ADDED(1L),
  FILE_ACTION_REMOVED(2L),
  FILE_ACTION_MODIFIED(3L),
  FILE_ACTION_RENAMED_OLD_NAME(4L),
  FILE_ACTION_RENAMED_NEW_NAME(5L),
  FILE_ACTION_ADDED_STREAM(6L),
  FILE_ACTION_REMOVED_STREAM(7L),
  FILE_ACTION_MODIFIED_STREAM(8L),
  FILE_ACTION_REMOVED_BY_DELETE(9L),
  FILE_ACTION_ID_NOT_TUNNELLED(10L),
  FILE_ACTION_TUNNELLED_ID_COLLISION(11L);
  
  private long value;
  
  FileNotifyAction(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
