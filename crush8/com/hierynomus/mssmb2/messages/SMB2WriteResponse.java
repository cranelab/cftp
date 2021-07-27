package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;

public class SMB2WriteResponse extends SMB2Packet {
  private long bytesWritten;
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.skip(2);
    buffer.skip(2);
    this.bytesWritten = buffer.readUInt32();
    buffer.skip(4);
    buffer.skip(2);
    buffer.skip(2);
  }
  
  public long getBytesWritten() {
    return this.bytesWritten;
  }
}
