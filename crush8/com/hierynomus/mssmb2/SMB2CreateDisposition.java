package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum SMB2CreateDisposition implements EnumWithValue<SMB2CreateDisposition> {
  FILE_SUPERSEDE(0L),
  FILE_OPEN(1L),
  FILE_CREATE(2L),
  FILE_OPEN_IF(3L),
  FILE_OVERWRITE(4L),
  FILE_OVERWRITE_IF(5L);
  
  private long value;
  
  SMB2CreateDisposition(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
