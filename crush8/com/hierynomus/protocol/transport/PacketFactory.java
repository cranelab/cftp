package com.hierynomus.protocol.transport;

import com.hierynomus.protocol.commons.buffer.Buffer;
import java.io.IOException;

public interface PacketFactory<D extends com.hierynomus.protocol.PacketData<?>> {
  D read(byte[] paramArrayOfbyte) throws Buffer.BufferException, IOException;
  
  boolean canHandle(byte[] paramArrayOfbyte);
}
