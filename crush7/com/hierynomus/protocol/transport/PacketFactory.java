package com.hierynomus.protocol.transport;

import com.hierynomus.protocol.commons.buffer.Buffer;
import java.io.IOException;

public interface PacketFactory<P extends com.hierynomus.protocol.Packet<?>> {
  P read(byte[] paramArrayOfbyte) throws Buffer.BufferException, IOException;
  
  boolean canHandle(byte[] paramArrayOfbyte);
}
