package com.hierynomus.smbj.connection;

import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.transport.PacketSerializer;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smb.SMBPacket;

public class SMBPacketSerializer implements PacketSerializer<SMBPacket<?, ?>, SMBBuffer> {
  public SMBBuffer write(SMBPacket<?, ?> packet) {
    SMBBuffer b = new SMBBuffer();
    packet.write(b);
    return b;
  }
}
