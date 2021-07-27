package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class SMB2Flush extends SMB2Packet {
  private SMB2FileId fileId;
  
  public SMB2Flush() {}
  
  public SMB2Flush(SMB2Dialect smbDialect, SMB2FileId fileId, long sessionId, long treeId) {
    super(24, smbDialect, SMB2MessageCommandCode.SMB2_FLUSH, sessionId, treeId);
    this.fileId = fileId;
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    buffer.putReserved2();
    buffer.putReserved4();
    this.fileId.write(buffer);
  }
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.readUInt16();
    buffer.skip(2);
  }
}
