package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class SMB2IoctlResponse extends SMB2Packet {
  private int controlCode;
  
  private SMB2FileId fileId;
  
  byte[] inputBuffer;
  
  byte[] outputBuffer;
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.skip(2);
    buffer.skip(2);
    this.controlCode = buffer.readUInt32AsInt();
    this.fileId = SMB2FileId.read(buffer);
    int inputOffset = buffer.readUInt32AsInt();
    int inputCount = buffer.readUInt32AsInt();
    int outputOffset = buffer.readUInt32AsInt();
    int outputCount = buffer.readUInt32AsInt();
    buffer.skip(4);
    buffer.skip(4);
    if (inputCount > 0) {
      buffer.rpos(inputOffset);
      this.inputBuffer = buffer.readRawBytes(inputCount);
    } 
    if (outputCount > 0) {
      buffer.rpos(outputOffset);
      this.outputBuffer = buffer.readRawBytes(outputCount);
    } 
  }
  
  public byte[] getOutputBuffer() {
    return this.outputBuffer;
  }
  
  public byte[] getInputBuffer() {
    return this.inputBuffer;
  }
  
  public int getControlCode() {
    return this.controlCode;
  }
  
  public SMB2FileId getFileId() {
    return this.fileId;
  }
}
