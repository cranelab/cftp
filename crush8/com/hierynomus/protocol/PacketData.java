package com.hierynomus.protocol;

public interface PacketData<B extends com.hierynomus.protocol.commons.buffer.Buffer<B>> {
  B getDataBuffer();
}
