package com.crushftp.client;

import java.security.Provider;

public class CrushOAuth2Provider extends Provider {
  private static final long serialVersionUID = 9038124458540162644L;
  
  public CrushOAuth2Provider() {
    super("CrushFTP OAuth2 Provider", 1.0D, "XOAUTH2 SASL Mechanism");
    put("SaslClientFactory.XOAUTH2", "com.crushftp.client.CrushOAuth2SaslClientFactory");
  }
}
