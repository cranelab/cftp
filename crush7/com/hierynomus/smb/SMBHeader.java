package com.hierynomus.smb;

import com.hierynomus.protocol.commons.buffer.Buffer;

public interface SMBHeader {
  void writeTo(SMBBuffer paramSMBBuffer);
  
  void readFrom(Buffer<?> paramBuffer) throws Buffer.BufferException;
}
