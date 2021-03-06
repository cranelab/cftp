package com.hierynomus.msdtyp.ace;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum AceType implements EnumWithValue<AceType> {
  ACCESS_ALLOWED_ACE_TYPE(0L),
  ACCESS_DENIED_ACE_TYPE(1L),
  SYSTEM_AUDIT_ACE_TYPE(2L),
  SYSTEM_ALARM_ACE_TYPE(3L),
  ACCESS_ALLOWED_COMPOUND_ACE_TYPE(4L),
  ACCESS_ALLOWED_OBJECT_ACE_TYPE(5L),
  ACCESS_DENIED_OBJECT_ACE_TYPE(6L),
  SYSTEM_AUDIT_OBJECT_ACE_TYPE(7L),
  SYSTEM_ALARM_OBJECT_ACE_TYPE(8L),
  ACCESS_ALLOWED_CALLBACK_ACE_TYPE(9L),
  ACCESS_DENIED_CALLBACK_ACE_TYPE(10L),
  ACCESS_ALLOWED_CALLBACK_OBJECT_ACE_TYPE(11L),
  ACCESS_DENIED_CALLBACK_OBJECT_ACE_TYPE(12L),
  SYSTEM_AUDIT_CALLBACK_ACE_TYPE(13L),
  SYSTEM_ALARM_CALLBACK_ACE_TYPE(14L),
  SYSTEM_AUDIT_CALLBACK_OBJECT_ACE_TYPE(15L),
  SYSTEM_ALARM_CALLBACK_OBJECT_ACE_TYPE(16L),
  SYSTEM_MANDATORY_LABEL_ACE_TYPE(17L),
  SYSTEM_RESOURCE_ATTRIBUTE_ACE_TYPE(18L),
  SYSTEM_SCOPED_POLICY_ID_ACE_TYPE(19L);
  
  private long value;
  
  AceType(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
