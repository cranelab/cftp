package com.crushftp.client;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslClientFactory;

public class CrushOAuth2SaslClientFactory implements SaslClientFactory {
  public SaslClient createSaslClient(String[] mechanisms, String authorizationId, String protocol, String serverName, Map props, CallbackHandler callbackHandler) {
    boolean matchedMechanism = false;
    for (int i = 0; i < mechanisms.length; i++) {
      if ("XOAUTH2".equalsIgnoreCase(mechanisms[i])) {
        matchedMechanism = true;
        break;
      } 
    } 
    if (!matchedMechanism)
      return null; 
    return new CrushOAuth2SaslClient((String)props.get("mail.sasl.mechanisms.oauth2.oauthToken"), callbackHandler);
  }
  
  public String[] getMechanismNames(Map props) {
    return new String[] { "XOAUTH2" };
  }
}
