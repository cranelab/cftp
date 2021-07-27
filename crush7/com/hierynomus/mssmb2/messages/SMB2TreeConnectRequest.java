package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.common.SmbPath;

public class SMB2TreeConnectRequest extends SMB2Packet {
  private final SMB2Dialect dialect;
  
  private boolean isClusterReconnect;
  
  private SmbPath smbPath;
  
  public SMB2TreeConnectRequest(SMB2Dialect dialect, SmbPath smbPath, long sessionId) {
    super(9, dialect, SMB2MessageCommandCode.SMB2_TREE_CONNECT, sessionId, 0L);
    this.dialect = dialect;
    this.smbPath = smbPath;
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    putFlags(buffer);
    buffer.putUInt16(72);
    String pathString = this.smbPath.toString();
    buffer.putStringLengthUInt16(pathString);
    buffer.putString(pathString);
  }
  
  private void putFlags(SMBBuffer buffer) {
    if (this.dialect == SMB2Dialect.SMB_3_1_1 && this.isClusterReconnect) {
      buffer.putUInt16(1);
    } else {
      buffer.putReserved2();
    } 
  }
  
  public SmbPath getSmbPath() {
    return this.smbPath;
  }
  
  public void setSmbPath(SmbPath smbPath) {
    this.smbPath = smbPath;
  }
}
