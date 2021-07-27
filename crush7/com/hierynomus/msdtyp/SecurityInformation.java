package com.hierynomus.msdtyp;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum SecurityInformation implements EnumWithValue<SecurityInformation> {
  OWNER_SECURITY_INFORMATION(1L),
  GROUP_SECURITY_INFORMATION(2L),
  DACL_SECURITY_INFORMATION(4L),
  SACL_SECURITY_INFORMATION(8L),
  LABEL_SECURITY_INFORMATION(16L),
  UNPROTECTED_SACL_SECURITY_INFORMATION(268435456L),
  UNPROTECTED_DACL_SECURITY_INFORMATION(536870912L),
  PROTECTED_SACL_SECURITY_INFORMATION(1073741824L),
  PROTECTED_DACL_SECURITY_INFORMATION(2147483648L),
  ATTRIBUTE_SECURITY_INFORMATION(32L),
  SCOPE_SECURITY_INFORMATION(64L),
  BACKUP_SECURITY_INFORMATION(65536L);
  
  private long value;
  
  SecurityInformation(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
