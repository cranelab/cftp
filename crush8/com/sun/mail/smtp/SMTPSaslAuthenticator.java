package com.sun.mail.smtp;

import com.sun.mail.auth.OAuth2SaslClientFactory;
import com.sun.mail.util.ASCIIUtility;
import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.BASE64EncoderStream;
import com.sun.mail.util.MailLogger;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.mail.MessagingException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

public class SMTPSaslAuthenticator implements SaslAuthenticator {
  private SMTPTransport pr;
  
  private String name;
  
  private Properties props;
  
  private MailLogger logger;
  
  private String host;
  
  static {
    try {
      OAuth2SaslClientFactory.init();
    } catch (Throwable throwable) {}
  }
  
  public SMTPSaslAuthenticator(SMTPTransport pr, String name, Properties props, MailLogger logger, String host) {
    this.pr = pr;
    this.name = name;
    this.props = props;
    this.logger = logger;
    this.host = host;
  }
  
  public boolean authenticate(String[] mechs, final String realm, String authzid, final String u, final String p) throws MessagingException {
    SaslClient sc;
    int resp;
    boolean done = false;
    if (this.logger.isLoggable(Level.FINE)) {
      this.logger.fine("SASL Mechanisms:");
      for (int i = 0; i < mechs.length; i++)
        this.logger.fine(" " + mechs[i]); 
      this.logger.fine("");
    } 
    CallbackHandler cbh = new CallbackHandler() {
        public void handle(Callback[] callbacks) {
          if (SMTPSaslAuthenticator.this.logger.isLoggable(Level.FINE))
            SMTPSaslAuthenticator.this.logger.fine("SASL callback length: " + callbacks.length); 
          for (int i = 0; i < callbacks.length; i++) {
            if (SMTPSaslAuthenticator.this.logger.isLoggable(Level.FINE))
              SMTPSaslAuthenticator.this.logger.fine("SASL callback " + i + ": " + callbacks[i]); 
            if (callbacks[i] instanceof NameCallback) {
              NameCallback ncb = (NameCallback)callbacks[i];
              ncb.setName(u);
            } else if (callbacks[i] instanceof PasswordCallback) {
              PasswordCallback pcb = (PasswordCallback)callbacks[i];
              pcb.setPassword(p.toCharArray());
            } else if (callbacks[i] instanceof RealmCallback) {
              RealmCallback rcb = (RealmCallback)callbacks[i];
              rcb.setText((realm != null) ? realm : rcb
                  .getDefaultText());
            } else if (callbacks[i] instanceof RealmChoiceCallback) {
              RealmChoiceCallback rcb = (RealmChoiceCallback)callbacks[i];
              if (realm == null) {
                rcb.setSelectedIndex(rcb.getDefaultChoice());
              } else {
                String[] choices = rcb.getChoices();
                for (int k = 0; k < choices.length; k++) {
                  if (choices[k].equals(realm)) {
                    rcb.setSelectedIndex(k);
                    break;
                  } 
                } 
              } 
            } 
          } 
        }
      };
    try {
      Map<String, ?> propsMap = this.props;
      sc = Sasl.createSaslClient(mechs, authzid, this.name, this.host, propsMap, cbh);
    } catch (SaslException sex) {
      this.logger.log(Level.FINE, "Failed to create SASL client", sex);
      throw new UnsupportedOperationException(sex.getMessage(), sex);
    } 
    if (sc == null) {
      this.logger.fine("No SASL support");
      throw new UnsupportedOperationException("No SASL support");
    } 
    if (this.logger.isLoggable(Level.FINE))
      this.logger.fine("SASL client " + sc.getMechanismName()); 
    try {
      String mech = sc.getMechanismName();
      String ir = null;
      if (sc.hasInitialResponse()) {
        byte[] ba = sc.evaluateChallenge(new byte[0]);
        if (ba.length > 0) {
          ba = BASE64EncoderStream.encode(ba);
          ir = ASCIIUtility.toString(ba, 0, ba.length);
        } else {
          ir = "=";
        } 
      } 
      if (ir != null) {
        resp = this.pr.simpleCommand("AUTH " + mech + " " + ir);
      } else {
        resp = this.pr.simpleCommand("AUTH " + mech);
      } 
      if (resp == 530) {
        this.pr.startTLS();
        if (ir != null) {
          resp = this.pr.simpleCommand("AUTH " + mech + " " + ir);
        } else {
          resp = this.pr.simpleCommand("AUTH " + mech);
        } 
      } 
      if (resp == 235)
        return true; 
      if (resp != 334)
        return false; 
    } catch (Exception ex) {
      this.logger.log(Level.FINE, "SASL AUTHENTICATE Exception", ex);
      return false;
    } 
    while (!done) {
      try {
        if (resp == 334) {
          byte[] ba = null;
          if (!sc.isComplete()) {
            ba = ASCIIUtility.getBytes(responseText(this.pr));
            if (ba.length > 0)
              ba = BASE64DecoderStream.decode(ba); 
            if (this.logger.isLoggable(Level.FINE))
              this.logger.fine("SASL challenge: " + 
                  ASCIIUtility.toString(ba, 0, ba.length) + " :"); 
            ba = sc.evaluateChallenge(ba);
          } 
          if (ba == null) {
            this.logger.fine("SASL: no response");
            resp = this.pr.simpleCommand("");
            continue;
          } 
          if (this.logger.isLoggable(Level.FINE))
            this.logger.fine("SASL response: " + 
                ASCIIUtility.toString(ba, 0, ba.length) + " :"); 
          ba = BASE64EncoderStream.encode(ba);
          resp = this.pr.simpleCommand(ba);
          continue;
        } 
        done = true;
      } catch (Exception ioex) {
        this.logger.log(Level.FINE, "SASL Exception", ioex);
        done = true;
      } 
    } 
    if (resp != 235)
      return false; 
    if (sc.isComplete()) {
      String qop = (String)sc.getNegotiatedProperty("javax.security.sasl.qop");
      if (qop != null && (qop.equalsIgnoreCase("auth-int") || qop
        .equalsIgnoreCase("auth-conf"))) {
        this.logger.fine("SASL Mechanism requires integrity or confidentiality");
        return false;
      } 
    } 
    return true;
  }
  
  private static final String responseText(SMTPTransport pr) {
    String resp = pr.getLastServerResponse().trim();
    if (resp.length() > 4)
      return resp.substring(4); 
    return "";
  }
}
