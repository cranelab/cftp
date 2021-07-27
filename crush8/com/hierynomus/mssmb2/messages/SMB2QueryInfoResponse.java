package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class SMB2QueryInfoResponse extends SMB2Packet {
  byte[] outputBuffer;
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.skip(2);
    int outputBufferOffset = buffer.readUInt16();
    int outBufferLength = buffer.readUInt32AsInt();
    buffer.rpos(outputBufferOffset);
    this.outputBuffer = buffer.readRawBytes(outBufferLength);
  }
  
  public byte[] getOutputBuffer() {
    return this.outputBuffer;
  }
}
