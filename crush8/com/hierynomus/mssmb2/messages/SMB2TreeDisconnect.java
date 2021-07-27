package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class SMB2TreeDisconnect extends SMB2Packet {
  public SMB2TreeDisconnect() {}
  
  public SMB2TreeDisconnect(SMB2Dialect smbDialect, long sessionId, long treeId) {
    super(4, smbDialect, SMB2MessageCommandCode.SMB2_TREE_DISCONNECT, sessionId, treeId);
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    buffer.putReserved(2);
  }
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.readUInt16();
    buffer.skip(2);
  }
}
