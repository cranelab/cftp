package com.hierynomus.mssmb;

import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smb.SMBPacket;
import com.hierynomus.smb.SMBPacketData;

public class SMB1Packet extends SMBPacket<SMB1PacketData, SMB1Header> {
  protected SMB1Packet() {
    super(new SMB1Header());
  }
  
  public final void write(SMBBuffer buffer) {
    this.header.writeTo(buffer);
    writeTo(buffer);
  }
  
  protected void writeTo(SMBBuffer buffer) {
    throw new UnsupportedOperationException("Should be implemented by specific message type");
  }
  
  protected void read(SMB1PacketData packetData) throws Buffer.BufferException {
    throw new UnsupportedOperationException("Receiving SMBv1 Messages not supported in SMBJ");
  }
}
