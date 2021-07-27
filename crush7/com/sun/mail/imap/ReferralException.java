package com.sun.mail.imap;

import javax.mail.AuthenticationFailedException;

public class ReferralException extends AuthenticationFailedException {
  private String url;
  
  private String text;
  
  private static final long serialVersionUID = -3414063558596287683L;
  
  public ReferralException(String url, String text) {
    super("[REFERRAL " + url + "] " + text);
    this.url = url;
    this.text = text;
  }
  
  public String getUrl() {
    return this.url;
  }
  
  public String getText() {
    return this.text;
  }
}
