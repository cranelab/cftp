package com.hierynomus.protocol.transport;

public interface PacketSerializer<P extends com.hierynomus.protocol.Packet<B>, B extends com.hierynomus.protocol.commons.buffer.Buffer<B>> {
  B write(P paramP);
}
