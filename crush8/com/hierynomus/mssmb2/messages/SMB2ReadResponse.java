package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class SMB2ReadResponse extends SMB2Packet {
  private int dataLength;
  
  private byte[] data;
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.skip(2);
    byte dataOffset = buffer.readByte();
    buffer.skip(1);
    this.dataLength = buffer.readUInt32AsInt();
    buffer.readUInt32AsInt();
    buffer.skip(4);
    buffer.rpos(dataOffset);
    this.data = buffer.readRawBytes(this.dataLength);
  }
  
  public int getDataLength() {
    return this.dataLength;
  }
  
  public byte[] getData() {
    return this.data;
  }
}
