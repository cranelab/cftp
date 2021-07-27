package com.hierynomus.mssmb2;

import java.util.Set;

public enum SMB2Dialect {
  UNKNOWN(0),
  SMB_2_0_2(514),
  SMB_2_1(528),
  SMB_2XX(767),
  SMB_3_0(768),
  SMB_3_0_2(770),
  SMB_3_1_1(785);
  
  private int value;
  
  SMB2Dialect(int value) {
    this.value = value;
  }
  
  public int getValue() {
    return this.value;
  }
  
  public boolean isSmb3x() {
    return !(this != SMB_3_0 && this != SMB_3_0_2 && this != SMB_3_1_1);
  }
  
  public static boolean supportsSmb3x(Set<SMB2Dialect> dialects) {
    for (SMB2Dialect dialect : dialects) {
      if (dialect.isSmb3x())
        return true; 
    } 
    return false;
  }
  
  public static SMB2Dialect lookup(int v) {
    byte b;
    int i;
    SMB2Dialect[] arrayOfSMB2Dialect;
    for (i = (arrayOfSMB2Dialect = values()).length, b = 0; b < i; ) {
      SMB2Dialect smb2Dialect = arrayOfSMB2Dialect[b];
      if (smb2Dialect.getValue() == v)
        return smb2Dialect; 
      b++;
    } 
    throw new IllegalStateException("Unknown SMB2 Dialect: " + v);
  }
}
