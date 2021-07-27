package com.hierynomus.smbj.auth;

import javax.security.auth.Subject;
import org.ietf.jgss.GSSCredential;

public class GSSAuthenticationContext extends AuthenticationContext {
  Subject subject;
  
  GSSCredential creds;
  
  public GSSAuthenticationContext(String username, String domain, Subject subject, GSSCredential creds) {
    super(username, new char[0], domain);
    this.subject = subject;
    this.creds = creds;
  }
  
  public Subject getSubject() {
    return this.subject;
  }
  
  public GSSCredential getCreds() {
    return this.creds;
  }
  
  public String toString() {
    return "GSSAuthenticationContext[" + this.subject + ']';
  }
}
