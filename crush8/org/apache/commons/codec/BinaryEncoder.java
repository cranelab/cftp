package org.apache.commons.codec;

public interface BinaryEncoder extends Encoder {
  byte[] encode(byte[] paramArrayOfbyte) throws EncoderException;
}
