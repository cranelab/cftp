package com.hierynomus.protocol;

import com.hierynomus.protocol.commons.buffer.Buffer;

public interface Packet<B extends Buffer<B>> {
  void write(B paramB);
  
  void read(B paramB) throws Buffer.BufferException;
}
