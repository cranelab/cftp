package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum SMB2ImpersonationLevel implements EnumWithValue<SMB2ImpersonationLevel> {
  Anonymous(0L),
  Identification(1L),
  Impersonation(2L),
  Delegate(3L);
  
  private long value;
  
  SMB2ImpersonationLevel(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
