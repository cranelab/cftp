package com.hierynomus.msdfsc.messages;

import com.hierynomus.smb.SMBBuffer;

public class SMB2GetDFSReferralRequest {
  private String requestFileName;
  
  public SMB2GetDFSReferralRequest(String path) {
    this.requestFileName = path;
  }
  
  public void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(4);
    buffer.putNullTerminatedString(this.requestFileName, StandardCharsets.UTF_16);
  }
}
