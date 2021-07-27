package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum SMB2CreateAction implements EnumWithValue<SMB2CreateAction> {
  FILE_SUPERSEDED(0L),
  FILE_OPENED(1L),
  FILE_CREATED(2L),
  FILE_OVERWRITTEN(3L);
  
  private long value;
  
  SMB2CreateAction(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
