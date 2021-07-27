package com.hierynomus.smb;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;

public abstract class SMBPacket<D extends SMBPacketData<H>, H extends SMBHeader> implements Packet<SMBBuffer> {
  protected H header;
  
  public SMBPacket(H header) {
    this.header = header;
  }
  
  public H getHeader() {
    return this.header;
  }
  
  public final void read(SMBBuffer buffer) throws Buffer.BufferException {
    throw new UnsupportedOperationException("Call read(D extends PacketData<H>) instead of this method");
  }
  
  protected abstract void read(D paramD) throws Buffer.BufferException;
}
