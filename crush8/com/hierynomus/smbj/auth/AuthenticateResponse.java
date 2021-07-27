package com.hierynomus.smbj.auth;

import com.hierynomus.ntlm.messages.WindowsVersion;

public class AuthenticateResponse {
  private byte[] negToken;
  
  private byte[] signingKey;
  
  private WindowsVersion windowsVersion;
  
  private String netBiosName;
  
  public AuthenticateResponse() {}
  
  public AuthenticateResponse(byte[] negToken) {
    this.negToken = negToken;
  }
  
  public WindowsVersion getWindowsVersion() {
    return this.windowsVersion;
  }
  
  public void setWindowsVersion(WindowsVersion windowsVersion) {
    this.windowsVersion = windowsVersion;
  }
  
  public byte[] getNegToken() {
    return this.negToken;
  }
  
  public void setNegToken(byte[] negToken) {
    this.negToken = negToken;
  }
  
  public byte[] getSigningKey() {
    return this.signingKey;
  }
  
  public void setSigningKey(byte[] signingKey) {
    this.signingKey = signingKey;
  }
  
  public String getNetBiosName() {
    return this.netBiosName;
  }
  
  public void setNetBiosName(String netBiosName) {
    this.netBiosName = netBiosName;
  }
}
