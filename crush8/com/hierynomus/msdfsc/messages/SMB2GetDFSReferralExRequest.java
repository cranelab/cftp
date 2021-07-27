package com.hierynomus.msdfsc.messages;

import com.hierynomus.smb.SMBBuffer;

public class SMB2GetDFSReferralExRequest {
  private int maxReferralLevel;
  
  private int requestFlags;
  
  private String requestFileName;
  
  private String siteName;
  
  enum RequestFlags {
    FLAGS_SITENAMEPRESENT(1);
    
    private int value;
    
    RequestFlags(int value) {
      this.value = value;
    }
    
    public int getValue() {
      return this.value;
    }
  }
  
  public SMB2GetDFSReferralExRequest(String path) {
    this.maxReferralLevel = 0;
    this.requestFlags = 0;
    this.requestFileName = path;
    this.siteName = null;
  }
  
  public SMB2GetDFSReferralExRequest(String path, String site) {
    this.maxReferralLevel = 0;
    this.requestFlags = RequestFlags.FLAGS_SITENAMEPRESENT.getValue();
    this.requestFileName = path;
    this.siteName = site;
  }
  
  public void writeTo(SMBBuffer buffer) {
    buffer.putUInt16(this.maxReferralLevel);
    buffer.putUInt16(this.requestFlags);
    if ((this.requestFlags & RequestFlags.FLAGS_SITENAMEPRESENT.getValue()) != 0) {
      buffer.putUInt32((this.requestFileName.length() + 2 + this.siteName.length()) + 2L);
    } else {
      buffer.putUInt32(this.requestFileName.length() + 2L);
    } 
    buffer.putStringLengthUInt16(this.requestFileName);
    buffer.putString(this.requestFileName);
    if ((this.requestFlags & RequestFlags.FLAGS_SITENAMEPRESENT.getValue()) != 0) {
      buffer.putStringLengthUInt16(this.requestFileName);
      buffer.putString(this.requestFileName);
    } 
  }
}
