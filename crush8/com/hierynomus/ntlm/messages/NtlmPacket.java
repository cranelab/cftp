package com.hierynomus.ntlm.messages;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;

public class NtlmPacket implements Packet<Buffer.PlainBuffer> {
  public void write(Buffer.PlainBuffer buffer) {
    throw new UnsupportedOperationException("Not implemented by base class");
  }
  
  public void read(Buffer.PlainBuffer buffer) throws Buffer.BufferException {
    throw new UnsupportedOperationException("Not implemented by base class");
  }
}
