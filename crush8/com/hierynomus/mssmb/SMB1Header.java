package com.hierynomus.mssmb;

import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smb.SMBHeader;

public class SMB1Header implements SMBHeader {
  public void writeTo(SMBBuffer buffer) {
    buffer.putRawBytes(new byte[] { -1, 83, 77, 66 });
    buffer.putByte((byte)114);
    buffer.putUInt32(0L);
    buffer.putByte((byte)24);
    buffer.putUInt16(51283);
    buffer.putUInt16(0);
    buffer.putUInt64(0L);
    buffer.putReserved2();
    buffer.putUInt16(0);
    buffer.putUInt16(0);
    buffer.putUInt16(0);
    buffer.putUInt16(0);
  }
  
  public void readFrom(Buffer<?> buffer) {
    throw new UnsupportedOperationException("Receiving SMBv1 Messages not supported in SMBJ");
  }
}
