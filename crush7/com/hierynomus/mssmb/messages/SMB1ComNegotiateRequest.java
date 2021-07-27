package com.hierynomus.mssmb.messages;

import com.hierynomus.msdfsc.messages.StandardCharsets;
import com.hierynomus.mssmb.SMB1Packet;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.smb.SMBBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SMB1ComNegotiateRequest extends SMB1Packet {
  private Set<SMB2Dialect> dialects;
  
  public SMB1ComNegotiateRequest(Set<SMB2Dialect> dialects) {
    this.dialects = dialects;
  }
  
  public void writeTo(SMBBuffer buffer) {
    buffer.putByte((byte)0);
    List<String> dialectsToWrite = new ArrayList<String>();
    dialectsToWrite.add("SMB 2.002");
    if (this.dialects.size() > 1 || !this.dialects.contains(SMB2Dialect.SMB_2_0_2))
      dialectsToWrite.add("SMB 2.???"); 
    int byteCount = 0;
    for (String s : dialectsToWrite)
      byteCount += 1 + s.length() + 1; 
    buffer.putUInt16(byteCount);
    for (String s : dialectsToWrite) {
      buffer.putByte((byte)2);
      buffer.putNullTerminatedString(s, StandardCharsets.UTF_8);
    } 
  }
  
  public void read(SMBBuffer buffer) throws Buffer.BufferException {
    throw new IllegalStateException("SMBv1 not implemented in SMBJ");
  }
  
  public String toString() {
    return "SMB_COM_NEGOTIATE";
  }
}
