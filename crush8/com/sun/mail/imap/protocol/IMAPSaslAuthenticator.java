package com.sun.mail.imap.protocol;

import com.sun.mail.auth.OAuth2SaslClientFactory;
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.util.ASCIIUtility;
import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.BASE64EncoderStream;
import com.sun.mail.util.MailLogger;
import com.sun.mail.util.PropUtil;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.RealmChoiceCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

public class IMAPSaslAuthenticator implements SaslAuthenticator {
  private IMAPProtocol pr;
  
  private String name;
  
  private Properties props;
  
  private MailLogger logger;
  
  private String host;
  
  static {
    try {
      OAuth2SaslClientFactory.init();
    } catch (Throwable throwable) {}
  }
  
  public IMAPSaslAuthenticator(IMAPProtocol pr, String name, Properties props, MailLogger logger, String host) {
    this.pr = pr;
    this.name = name;
    this.props = props;
    this.logger = logger;
    this.host = host;
  }
  
  public boolean authenticate(String[] mechs, final String realm, String authzid, final String u, final String p) throws ProtocolException {
    synchronized (this.pr) {
      SaslClient sc;
      List<Response> v = new ArrayList<Response>();
      String tag = null;
      Response r = null;
      boolean done = false;
      if (this.logger.isLoggable(Level.FINE)) {
        this.logger.fine("SASL Mechanisms:");
        for (int i = 0; i < mechs.length; i++)
          this.logger.fine(" " + mechs[i]); 
        this.logger.fine("");
      } 
      CallbackHandler cbh = new CallbackHandler() {
          public void handle(Callback[] callbacks) {
            if (IMAPSaslAuthenticator.this.logger.isLoggable(Level.FINE))
              IMAPSaslAuthenticator.this.logger.fine("SASL callback length: " + callbacks.length); 
            for (int i = 0; i < callbacks.length; i++) {
              if (IMAPSaslAuthenticator.this.logger.isLoggable(Level.FINE))
                IMAPSaslAuthenticator.this.logger.fine("SASL callback " + i + ": " + callbacks[i]); 
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
        Argument args = new Argument();
        args.writeAtom(sc.getMechanismName());
        if (this.pr.hasCapability("SASL-IR") && sc.hasInitialResponse()) {
          String irs;
          byte[] ba = sc.evaluateChallenge(new byte[0]);
          if (ba.length > 0) {
            ba = BASE64EncoderStream.encode(ba);
            irs = ASCIIUtility.toString(ba, 0, ba.length);
          } else {
            irs = "=";
          } 
          args.writeAtom(irs);
        } 
        tag = this.pr.writeCommand("AUTHENTICATE", args);
      } catch (Exception ex) {
        this.logger.log(Level.FINE, "SASL AUTHENTICATE Exception", ex);
        return false;
      } 
      OutputStream os = this.pr.getIMAPOutputStream();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      byte[] CRLF = { 13, 10 };
      boolean isXGWTRUSTEDAPP = (sc.getMechanismName().equals("XGWTRUSTEDAPP") && PropUtil.getBooleanProperty(this.props, "mail." + this.name + ".sasl.xgwtrustedapphack.enable", true));
      while (!done) {
        try {
          r = this.pr.readResponse();
          if (r.isContinuation()) {
            byte[] ba = null;
            if (!sc.isComplete()) {
              ba = r.readByteArray().getNewBytes();
              if (ba.length > 0)
                ba = BASE64DecoderStream.decode(ba); 
              if (this.logger.isLoggable(Level.FINE))
                this.logger.fine("SASL challenge: " + 
                    ASCIIUtility.toString(ba, 0, ba.length) + " :"); 
              ba = sc.evaluateChallenge(ba);
            } 
            if (ba == null) {
              this.logger.fine("SASL no response");
              os.write(CRLF);
              os.flush();
              bos.reset();
              continue;
            } 
            if (this.logger.isLoggable(Level.FINE))
              this.logger.fine("SASL response: " + 
                  ASCIIUtility.toString(ba, 0, ba.length) + " :"); 
            ba = BASE64EncoderStream.encode(ba);
            if (isXGWTRUSTEDAPP)
              bos.write(ASCIIUtility.getBytes("XGWTRUSTEDAPP ")); 
            bos.write(ba);
            bos.write(CRLF);
            os.write(bos.toByteArray());
            os.flush();
            bos.reset();
            continue;
          } 
          if (r.isTagged() && r.getTag().equals(tag)) {
            done = true;
            continue;
          } 
          if (r.isBYE()) {
            done = true;
            continue;
          } 
          v.add(r);
        } catch (Exception ioex) {
          this.logger.log(Level.FINE, "SASL Exception", ioex);
          r = Response.byeResponse(ioex);
          done = true;
        } 
      } 
      if (sc.isComplete()) {
        String qop = (String)sc.getNegotiatedProperty("javax.security.sasl.qop");
        if (qop != null && (qop.equalsIgnoreCase("auth-int") || qop
          .equalsIgnoreCase("auth-conf"))) {
          this.logger.fine("SASL Mechanism requires integrity or confidentiality");
          return false;
        } 
      } 
      Response[] responses = v.<Response>toArray(new Response[v.size()]);
      this.pr.notifyResponseHandlers(responses);
      this.pr.handleLoginResult(r);
      this.pr.setCapabilities(r);
      if (isXGWTRUSTEDAPP && authzid != null) {
        Argument args = new Argument();
        args.writeString(authzid);
        responses = this.pr.command("LOGIN", args);
        this.pr.notifyResponseHandlers(responses);
        this.pr.handleResult(responses[responses.length - 1]);
        this.pr.setCapabilities(responses[responses.length - 1]);
      } 
      return true;
    } 
  }
}
