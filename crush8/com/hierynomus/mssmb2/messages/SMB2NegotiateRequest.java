package com.hierynomus.mssmb2.messages;

import com.hierynomus.msdtyp.MsDataTypes;
import com.hierynomus.mssmb2.SMB2Dialect;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.smb.SMBBuffer;
import java.util.Set;
import java.util.UUID;

public class SMB2NegotiateRequest extends SMB2Packet {
  private Set<SMB2Dialect> dialects;
  
  private UUID clientGuid;
  
  private boolean clientSigningRequired;
  
  public SMB2NegotiateRequest(Set<SMB2Dialect> dialects, UUID clientGuid, boolean clientSigningRequired) {
    super(36, SMB2Dialect.UNKNOWN, SMB2MessageCommandCode.SMB2_NEGOTIATE, 0L, 0L);
    this.dialects = dialects;
    this.clientGuid = clientGuid;
    this.clientSigningRequired = clientSigningRequired;
  }
  
  protected void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.structureSize);
    buffer.putUInt16(this.dialects.size());
    buffer.putUInt16(securityMode());
    buffer.putReserved(2);
    putCapabilities(buffer);
    MsDataTypes.putGuid(this.clientGuid, buffer);
    putNegotiateStartTime(buffer);
    putDialects(buffer);
    int eightByteAlignment = (34 + this.dialects.size() * 2) % 8;
    if (eightByteAlignment > 0)
      buffer.putReserved(8 - eightByteAlignment); 
    putNegotiateContextList();
  }
  
  private int securityMode() {
    if (this.clientSigningRequired)
      return 3; 
    return 1;
  }
  
  private void putNegotiateContextList() {
    if (this.dialects.contains(SMB2Dialect.SMB_3_1_1))
      throw new UnsupportedOperationException("SMB 3.x support is not yet implemented"); 
  }
  
  private void putDialects(SMBBuffer buffer) {
    for (SMB2Dialect dialect : this.dialects)
      buffer.putUInt16(dialect.getValue()); 
  }
  
  private void putNegotiateStartTime(SMBBuffer buffer) {
    if (this.dialects.contains(SMB2Dialect.SMB_3_1_1))
      throw new UnsupportedOperationException("SMB 3.x support is not yet implemented"); 
    buffer.putReserved4();
    buffer.putReserved4();
  }
  
  private void putCapabilities(SMBBuffer buffer) {
    if (SMB2Dialect.supportsSmb3x(this.dialects))
      throw new UnsupportedOperationException("SMB 3.x support is not yet implemented"); 
    buffer.putReserved4();
  }
}
