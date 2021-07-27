package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2MultiCreditPacket;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.io.ByteChunkProvider;

public class SMB2WriteRequest extends SMB2MultiCreditPacket {
  private final SMB2FileId fileId;
  
  private final ByteChunkProvider byteProvider;
  
  public SMB2WriteRequest(SMB2Dialect negotiatedDialect, SMB2FileId fileId, long sessionId, long treeId, ByteChunkProvider byteProvider, int maxPayloadSize) {
    super(49, negotiatedDialect, SMB2MessageCommandCode.SMB2_WRITE, sessionId, treeId, Math.min(maxPayloadSize, byteProvider.bytesLeft()));
    this.fileId = fileId;
    this.byteProvider = byteProvider;
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    short dataOffset = 112;
    buffer.putUInt16(dataOffset);
    buffer.putUInt32(getPayloadSize());
    buffer.putUInt64(this.byteProvider.getOffset());
    this.fileId.write(buffer);
    buffer.putUInt32(0L);
    buffer.putUInt32(Math.max(0, this.byteProvider.bytesLeft() - getPayloadSize()));
    buffer.putUInt16(0);
    buffer.putUInt16(0);
    buffer.putUInt32(0L);
    this.byteProvider.writeChunks(buffer, getCreditsAssigned());
  }
}
