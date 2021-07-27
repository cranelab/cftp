package com.hierynomus.msdtyp.ace;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum AceObjectFlags implements EnumWithValue<AceObjectFlags> {
  NONE(0L),
  ACE_OBJECT_TYPE_PRESENT(1L),
  ACE_INHERITED_OBJECT_TYPE_PRESENT(2L);
  
  private long value;
  
  AceObjectFlags(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
