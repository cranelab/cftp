package com.hierynomus.mssmb;

import com.hierynomus.protocol.PacketData;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.transport.PacketFactory;
import java.io.IOException;

public class SMB1PacketFactory implements PacketFactory<SMB1PacketData> {
  public SMB1PacketData read(byte[] data) throws Buffer.BufferException, IOException {
    throw new SMB1NotSupportedException();
  }
  
  public boolean canHandle(byte[] data) {
    return (data[0] == -1 && data[1] == 83 && data[2] == 77 && data[3] == 66);
  }
}
