package com.hierynomus.msdtyp.ace;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum AceFlags implements EnumWithValue<AceFlags> {
  CONTAINER_INHERIT_ACE(2L),
  FAILED_ACCESS_ACE_FLAG(128L),
  INHERIT_ONLY_ACE(8L),
  INHERITED_ACE(16L),
  NO_PROPAGATE_INHERIT_ACE(4L),
  OBJECT_INHERIT_ACE(1L),
  SUCCESSFUL_ACCESS_ACE_FLAG(64L);
  
  private long value;
  
  AceFlags(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
