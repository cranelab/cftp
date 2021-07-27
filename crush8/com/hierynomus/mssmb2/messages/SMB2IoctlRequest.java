package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2MultiCreditPacket;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.io.ByteChunkProvider;

public class SMB2IoctlRequest extends SMB2MultiCreditPacket {
  private final long controlCode;
  
  private final SMB2FileId fileId;
  
  private final ByteChunkProvider inputData;
  
  private final boolean fsctl;
  
  private long maxOutputResponse;
  
  public SMB2IoctlRequest(SMB2Dialect negotiatedDialect, long sessionId, long treeId, long controlCode, SMB2FileId fileId, ByteChunkProvider inputData, boolean fsctl, int maxOutputResponse) {
    super(57, negotiatedDialect, SMB2MessageCommandCode.SMB2_IOCTL, sessionId, treeId, Math.max(inputData.bytesLeft(), maxOutputResponse));
    this.controlCode = controlCode;
    this.fileId = fileId;
    this.inputData = inputData;
    this.fsctl = fsctl;
    this.maxOutputResponse = maxOutputResponse;
  }
  
  protected void writeTo(SMBBuffer smbBuffer) {
    smbBuffer.putUInt16(this.structureSize);
    smbBuffer.putReserved2();
    smbBuffer.putUInt32(this.controlCode);
    this.fileId.write(smbBuffer);
    int offset = 120;
    int inputDataSize = this.inputData.bytesLeft();
    if (inputDataSize > 0) {
      smbBuffer.putUInt32(offset);
      smbBuffer.putUInt32(inputDataSize);
    } else {
      smbBuffer.putUInt32(0L);
      smbBuffer.putUInt32(0L);
    } 
    smbBuffer.putUInt32(0L);
    smbBuffer.putUInt32(0L);
    smbBuffer.putUInt32(0L);
    smbBuffer.putUInt32(this.maxOutputResponse);
    smbBuffer.putUInt32(this.fsctl ? 1L : 0L);
    smbBuffer.putReserved4();
    while (this.inputData.bytesLeft() > 0)
      this.inputData.writeChunk(smbBuffer); 
  }
  
  public long getControlCode() {
    return this.controlCode;
  }
}
