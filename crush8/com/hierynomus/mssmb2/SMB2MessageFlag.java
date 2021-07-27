package com.hierynomus.mssmb2;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum SMB2MessageFlag implements EnumWithValue<SMB2MessageFlag> {
  SMB2_FLAGS_SERVER_TO_REDIR(1L),
  SMB2_FLAGS_ASYNC_COMMAND(2L),
  SMB2_FLAGS_RELATED_OPERATIONS(4L),
  SMB2_FLAGS_SIGNED(8L),
  SMB2_FLAGS_PRIORITY_MASK(112L),
  SMB2_FLAGS_DFS_OPERATIONS(268435456L),
  SMB2_FLAGS_REPLAY_OPERATION(536870912L);
  
  private long value;
  
  SMB2MessageFlag(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
