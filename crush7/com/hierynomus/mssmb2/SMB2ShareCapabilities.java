package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum SMB2ShareCapabilities implements EnumWithValue<SMB2ShareCapabilities> {
  SMB2_SHARE_CAP_DFS(8L),
  SMB2_SHARE_CAP_CONTINUOUS_AVAILABILITY(16L),
  SMB2_SHARE_CAP_SCALEOUT(32L),
  SMB2_SHARE_CAP_CLUSTER(64L),
  SMB2_SHARE_CAP_ASYMMETRIC(128L);
  
  private long value;
  
  SMB2ShareCapabilities(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
