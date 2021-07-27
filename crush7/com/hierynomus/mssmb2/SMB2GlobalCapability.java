package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum SMB2GlobalCapability implements EnumWithValue<SMB2GlobalCapability> {
  SMB2_GLOBAL_CAP_DFS(1L),
  SMB2_GLOBAL_CAP_LEASING(2L),
  SMB2_GLOBAL_CAP_LARGE_MTU(4L),
  SMB2_GLOBAL_CAP_MULTI_CHANNEL(8L),
  SMB2_GLOBAL_CAP_PERSISTENT_HANDLES(16L),
  SMB2_GLOBAL_CAP_DIRECTORY_LEASING(32L),
  SMB2_GLOBAL_CAP_ENCRYPTION(64L);
  
  private long i;
  
  SMB2GlobalCapability(long i) {
    this.i = i;
  }
  
  public long getValue() {
    return this.i;
  }
}
