package com.sun.mail.auth;

import java.security.Provider;
import java.security.Security;
import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;
import javax.security.sasl.SaslException;

public class OAuth2SaslClientFactory implements SaslClientFactory {
  private static final String PROVIDER_NAME = "JavaMail-OAuth2";
  
  private static final String MECHANISM_NAME = "SaslClientFactory.XOAUTH2";
  
  static class OAuth2Provider extends Provider {
    private static final long serialVersionUID = -5371795551562287059L;
    
    public OAuth2Provider() {
      super("JavaMail-OAuth2", 1.0D, "XOAUTH2 SASL Mechanism");
      put("SaslClientFactory.XOAUTH2", OAuth2SaslClientFactory.class.getName());
    }
  }
  
  public SaslClient createSaslClient(String[] mechanisms, String authorizationId, String protocol, String serverName, Map<String, ?> props, CallbackHandler cbh) throws SaslException {
    for (String m : mechanisms) {
      if (m.equals("XOAUTH2"))
        return new OAuth2SaslClient(props, cbh); 
    } 
    return null;
  }
  
  public String[] getMechanismNames(Map<String, ?> props) {
    return new String[] { "XOAUTH2" };
  }
  
  public static void init() {
    try {
      if (Security.getProvider("JavaMail-OAuth2") == null)
        Security.addProvider(new OAuth2Provider()); 
    } catch (SecurityException securityException) {}
  }
}
