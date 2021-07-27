package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class SMB2Echo extends SMB2Packet {
  public SMB2Echo() {}
  
  public SMB2Echo(SMB2Dialect dialect) {
    super(4, dialect, SMB2MessageCommandCode.SMB2_ECHO);
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    buffer.putUInt16(0);
  }
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.skip(4);
  }
}
