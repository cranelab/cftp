package com.sun.mail.auth;

import com.sun.mail.util.ASCIIUtility;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

public class OAuth2SaslClient implements SaslClient {
  private CallbackHandler cbh;
  
  private boolean complete = false;
  
  public OAuth2SaslClient(Map<String, ?> props, CallbackHandler cbh) {
    this.cbh = cbh;
  }
  
  public String getMechanismName() {
    return "XOAUTH2";
  }
  
  public boolean hasInitialResponse() {
    return true;
  }
  
  public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
    byte[] response;
    if (this.complete)
      return new byte[0]; 
    NameCallback ncb = new NameCallback("User name:");
    PasswordCallback pcb = new PasswordCallback("OAuth token:", false);
    try {
      this.cbh.handle(new Callback[] { ncb, pcb });
    } catch (UnsupportedCallbackException ex) {
      throw new SaslException("Unsupported callback", ex);
    } catch (IOException ex) {
      throw new SaslException("Callback handler failed", ex);
    } 
    String user = ncb.getName();
    String token = new String(pcb.getPassword());
    pcb.clearPassword();
    String resp = "user=" + user + "\001auth=Bearer " + token + "\001\001";
    try {
      response = resp.getBytes("utf-8");
    } catch (UnsupportedEncodingException ex) {
      response = ASCIIUtility.getBytes(resp);
    } 
    this.complete = true;
    return response;
  }
  
  public boolean isComplete() {
    return this.complete;
  }
  
  public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
    throw new IllegalStateException("OAUTH2 unwrap not supported");
  }
  
  public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
    throw new IllegalStateException("OAUTH2 wrap not supported");
  }
  
  public Object getNegotiatedProperty(String propName) {
    if (!this.complete)
      throw new IllegalStateException("OAUTH2 getNegotiatedProperty"); 
    return null;
  }
  
  public void dispose() throws SaslException {}
}
