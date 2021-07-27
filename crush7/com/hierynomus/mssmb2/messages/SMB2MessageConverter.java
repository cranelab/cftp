package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.protocol.Packet;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.transport.PacketFactory;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.common.Check;
import com.hierynomus.smbj.common.SMBRuntimeException;
import java.io.IOException;

public class SMB2MessageConverter implements PacketFactory<SMB2Packet> {
  private SMB2Packet read(SMBBuffer buffer) throws Buffer.BufferException {
    Check.ensureEquals(buffer.readRawBytes(4), new byte[] { -2, 83, 77, 66 }, "Could not find SMB2 Packet header");
    buffer.skip(8);
    SMB2MessageCommandCode command = SMB2MessageCommandCode.lookup(buffer.readUInt16());
    buffer.rpos(0);
    switch (command) {
      case SMB2_NEGOTIATE:
        return read(new SMB2NegotiateResponse(), buffer);
      case SMB2_SESSION_SETUP:
        return read(new SMB2SessionSetup(), buffer);
      case SMB2_TREE_CONNECT:
        return read(new SMB2TreeConnectResponse(), buffer);
      case SMB2_TREE_DISCONNECT:
        return read(new SMB2TreeDisconnect(), buffer);
      case SMB2_LOGOFF:
        return read(new SMB2Logoff(), buffer);
      case SMB2_CREATE:
        return read(new SMB2CreateResponse(), buffer);
      case SMB2_CHANGE_NOTIFY:
        return read(new SMB2ChangeNotifyResponse(), buffer);
      case SMB2_QUERY_DIRECTORY:
        return read(new SMB2QueryDirectoryResponse(), buffer);
      case SMB2_ECHO:
        return read(new SMB2Echo(), buffer);
      case SMB2_READ:
        return read(new SMB2ReadResponse(), buffer);
      case SMB2_CLOSE:
        return read(new SMB2Close(), buffer);
      case SMB2_FLUSH:
        return read(new SMB2Flush(), buffer);
      case SMB2_WRITE:
        return read(new SMB2WriteResponse(), buffer);
      case SMB2_IOCTL:
        return read(new SMB2IoctlResponse(), buffer);
      case SMB2_QUERY_INFO:
        return read(new SMB2QueryInfoResponse(), buffer);
      case SMB2_SET_INFO:
        return read(new SMB2SetInfoResponse(), buffer);
    } 
    throw new SMBRuntimeException("Unknown SMB2 Message Command type: " + command);
  }
  
  private SMB2Packet read(SMB2Packet packet, SMBBuffer buffer) throws Buffer.BufferException {
    packet.read(buffer);
    return packet;
  }
  
  public SMB2Packet read(byte[] data) throws Buffer.BufferException {
    return read(new SMBBuffer(data));
  }
  
  public boolean canHandle(byte[] data) {
    return (data[0] == -2 && data[1] == 83 && data[2] == 77 && data[3] == 66);
  }
}
