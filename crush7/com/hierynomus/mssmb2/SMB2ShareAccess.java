package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.EnumWithValue;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum SMB2ShareAccess implements EnumWithValue<SMB2ShareAccess> {
  FILE_SHARE_READ(1L),
  FILE_SHARE_WRITE(2L),
  FILE_SHARE_DELETE(4L);
  
  public static final Set<SMB2ShareAccess> ALL;
  
  private long value;
  
  static {
    ALL = Collections.unmodifiableSet(EnumSet.allOf(SMB2ShareAccess.class));
  }
  
  SMB2ShareAccess(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
