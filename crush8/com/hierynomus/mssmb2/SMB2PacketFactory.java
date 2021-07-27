package com.hierynomus.mssmb2;

import com.hierynomus.protocol.PacketData;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.transport.PacketFactory;
import java.io.IOException;

public class SMB2PacketFactory implements PacketFactory<SMB2PacketData> {
  public SMB2PacketData read(byte[] data) throws Buffer.BufferException {
    return new SMB2PacketData(data);
  }
  
  public boolean canHandle(byte[] data) {
    return (data[0] == -2 && data[1] == 83 && data[2] == 77 && data[3] == 66);
  }
}
