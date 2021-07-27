package com.sun.mail.imap.protocol;

import com.sun.mail.iap.ProtocolException;

public class IMAPReferralException extends ProtocolException {
  private String url;
  
  private static final long serialVersionUID = 2578770669364251968L;
  
  public IMAPReferralException(String s, String url) {
    super(s);
    this.url = url;
  }
  
  public String getUrl() {
    return this.url;
  }
}
