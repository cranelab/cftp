package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2MessageFlag;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.smb.SMBBuffer;

public class SMB2CancelRequest extends SMB2Packet {
  public SMB2CancelRequest(SMB2Dialect dialect, long messageId, long asyncId) {
    super(4, dialect, SMB2MessageCommandCode.SMB2_CANCEL);
    this.header.setMessageId(messageId);
    if (asyncId != 0L) {
      this.header.setFlag(SMB2MessageFlag.SMB2_FLAGS_ASYNC_COMMAND);
      this.header.setAsyncId(asyncId);
    } 
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    buffer.putReserved2();
  }
}
