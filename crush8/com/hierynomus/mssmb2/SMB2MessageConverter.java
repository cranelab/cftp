package com.hierynomus.mssmb2;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb2.messages.SMB2ChangeNotifyResponse;
import com.hierynomus.mssmb2.messages.SMB2Close;
import com.hierynomus.mssmb2.messages.SMB2CreateResponse;
import com.hierynomus.mssmb2.messages.SMB2Echo;
import com.hierynomus.mssmb2.messages.SMB2Flush;
import com.hierynomus.mssmb2.messages.SMB2IoctlRequest;
import com.hierynomus.mssmb2.messages.SMB2IoctlResponse;
import com.hierynomus.mssmb2.messages.SMB2Logoff;
import com.hierynomus.mssmb2.messages.SMB2NegotiateResponse;
import com.hierynomus.mssmb2.messages.SMB2QueryDirectoryResponse;
import com.hierynomus.mssmb2.messages.SMB2QueryInfoResponse;
import com.hierynomus.mssmb2.messages.SMB2ReadResponse;
import com.hierynomus.mssmb2.messages.SMB2SessionSetup;
import com.hierynomus.mssmb2.messages.SMB2SetInfoResponse;
import com.hierynomus.mssmb2.messages.SMB2TreeConnectResponse;
import com.hierynomus.mssmb2.messages.SMB2TreeDisconnect;
import com.hierynomus.mssmb2.messages.SMB2WriteResponse;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBPacket;
import com.hierynomus.smbj.common.SMBRuntimeException;

public class SMB2MessageConverter {
  private static final long FSCTL_PIPE_PEEK = 1130508L;
  
  private static final long FSCTL_PIPE_TRANSCEIVE = 1163287L;
  
  private static final long FSCTL_DFS_GET_REFERRALS = 393620L;
  
  private static final long FSCTL_SRV_COPYCHUNK = 1327346L;
  
  private static final long FSCTL_SRV_COPYCHUNK_WRITE = 1343730L;
  
  private SMB2Packet getPacketInstance(SMB2PacketData packetData) {
    SMB2MessageCommandCode command = packetData.getHeader().getMessage();
    switch (command) {
      case SMB2_NEGOTIATE:
        return new SMB2NegotiateResponse();
      case SMB2_SESSION_SETUP:
        return new SMB2SessionSetup();
      case SMB2_TREE_CONNECT:
        return new SMB2TreeConnectResponse();
      case SMB2_TREE_DISCONNECT:
        return new SMB2TreeDisconnect();
      case SMB2_LOGOFF:
        return new SMB2Logoff();
      case SMB2_CREATE:
        return new SMB2CreateResponse();
      case SMB2_CHANGE_NOTIFY:
        return new SMB2ChangeNotifyResponse();
      case SMB2_QUERY_DIRECTORY:
        return new SMB2QueryDirectoryResponse();
      case SMB2_ECHO:
        return new SMB2Echo();
      case SMB2_READ:
        return new SMB2ReadResponse();
      case SMB2_CLOSE:
        return new SMB2Close();
      case SMB2_FLUSH:
        return new SMB2Flush();
      case SMB2_WRITE:
        return new SMB2WriteResponse();
      case SMB2_IOCTL:
        return new SMB2IoctlResponse();
      case SMB2_QUERY_INFO:
        return new SMB2QueryInfoResponse();
      case SMB2_SET_INFO:
        return new SMB2SetInfoResponse();
    } 
    throw new SMBRuntimeException("Unknown SMB2 Message Command type: " + command);
  }
  
  public SMB2Packet readPacket(SMBPacket requestPacket, SMB2PacketData packetData) throws Buffer.BufferException {
    SMB2Packet responsePacket = getPacketInstance(packetData);
    if (isSuccess(requestPacket, packetData)) {
      responsePacket.read(packetData);
    } else {
      responsePacket.readError(packetData);
    } 
    return responsePacket;
  }
  
  private boolean isSuccess(SMBPacket requestPacket, SMB2PacketData packetData) {
    SMB2IoctlRequest r;
    long controlCode;
    if (packetData.isSuccess())
      return true; 
    SMB2MessageCommandCode message = packetData.getHeader().getMessage();
    long statusCode = packetData.getHeader().getStatusCode();
    switch (message) {
      case SMB2_SESSION_SETUP:
        return (statusCode == NtStatus.STATUS_MORE_PROCESSING_REQUIRED.getValue());
      case SMB2_CHANGE_NOTIFY:
        return (statusCode == NtStatus.STATUS_NOTIFY_ENUM_DIR.getValue());
      case SMB2_READ:
      case SMB2_QUERY_INFO:
        return (statusCode == NtStatus.STATUS_BUFFER_OVERFLOW.getValue());
      case SMB2_IOCTL:
        r = (SMB2IoctlRequest)requestPacket;
        controlCode = r.getControlCode();
        if (controlCode == 1130508L || controlCode == 1163287L || controlCode == 393620L)
          return (statusCode == NtStatus.STATUS_BUFFER_OVERFLOW.getValue()); 
        if (controlCode == 1327346L || controlCode == 1343730L)
          return (statusCode == NtStatus.STATUS_INVALID_PARAMETER.getValue()); 
        return false;
    } 
    return false;
  }
}
