package com.hierynomus.smb;

import com.hierynomus.protocol.Packet;

public abstract class SMBPacket<H extends SMBHeader> implements Packet<SMBBuffer> {
  protected final H header;
  
  public SMBPacket(H header) {
    this.header = header;
  }
  
  public H getHeader() {
    return this.header;
  }
}
