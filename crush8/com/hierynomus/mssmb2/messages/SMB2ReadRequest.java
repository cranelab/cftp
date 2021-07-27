package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2MultiCreditPacket;
import com.hierynomus.smb.SMBBuffer;

public class SMB2ReadRequest extends SMB2MultiCreditPacket {
  private final long offset;
  
  private final SMB2FileId fileId;
  
  public SMB2ReadRequest(SMB2Dialect dialect, SMB2FileId fileId, long sessionId, long treeId, long offset, int maxPayloadSize) {
    super(49, dialect, SMB2MessageCommandCode.SMB2_READ, sessionId, treeId, maxPayloadSize);
    this.fileId = fileId;
    this.offset = offset;
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    buffer.putByte((byte)0);
    buffer.putByte((byte)0);
    buffer.putUInt32(65536L * getCreditsAssigned());
    buffer.putUInt64(this.offset);
    this.fileId.write(buffer);
    buffer.putUInt32(1L);
    buffer.putUInt32(0L);
    buffer.putUInt32(0L);
    buffer.putUInt16(0);
    buffer.putUInt16(0);
    buffer.putByte((byte)0);
  }
}
