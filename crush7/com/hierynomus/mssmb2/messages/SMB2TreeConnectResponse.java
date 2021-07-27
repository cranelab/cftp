package com.hierynomus.mssmb2.messages;

import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.mssmb2.SMB2ShareCapabilities;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.Set;

public class SMB2TreeConnectResponse extends SMB2Packet {
  private byte shareType;
  
  private long shareFlags;
  
  private Set<SMB2ShareCapabilities> capabilities;
  
  private long maximalAccess;
  
  protected void readMessage(SMBBuffer buffer) throws Buffer.BufferException {
    buffer.skip(2);
    this.shareType = buffer.readByte();
    buffer.readByte();
    this.shareFlags = buffer.readUInt32();
    this.capabilities = EnumWithValue.EnumUtils.toEnumSet(buffer.readUInt32(), SMB2ShareCapabilities.class);
    this.maximalAccess = buffer.readUInt32();
  }
  
  public boolean isDiskShare() {
    return (this.shareType == 1);
  }
  
  public boolean isNamedPipe() {
    return (this.shareType == 2);
  }
  
  public boolean isPrinterShare() {
    return (this.shareType == 3);
  }
  
  public long getShareFlags() {
    return this.shareFlags;
  }
  
  public Set<SMB2ShareCapabilities> getCapabilities() {
    return this.capabilities;
  }
  
  public long getMaximalAccess() {
    return this.maximalAccess;
  }
}
