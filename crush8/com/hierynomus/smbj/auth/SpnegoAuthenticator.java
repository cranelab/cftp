package com.hierynomus.smbj.auth;

import com.hierynomus.protocol.commons.ByteArrayUtils;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.GSSContextConfig;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.session.Session;
import com.sun.security.jgss.ExtendedGSSContext;
import com.sun.security.jgss.InquireType;
import java.io.IOException;
import java.security.Key;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import javax.security.auth.Subject;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpnegoAuthenticator implements Authenticator {
  private static final Logger logger = LoggerFactory.getLogger(SpnegoAuthenticator.class);
  
  private GSSContextConfig gssContextConfig;
  
  private GSSContext gssContext;
  
  public static class Factory implements com.hierynomus.protocol.commons.Factory.Named<Authenticator> {
    public String getName() {
      return "1.3.6.1.4.1.311.2.2.30";
    }
    
    public SpnegoAuthenticator create() {
      return new SpnegoAuthenticator();
    }
  }
  
  public AuthenticateResponse authenticate(AuthenticationContext context, final byte[] gssToken, final Session session) throws IOException {
    final GSSAuthenticationContext gssAuthenticationContext = (GSSAuthenticationContext)context;
    try {
      return Subject.<AuthenticateResponse>doAs(gssAuthenticationContext.getSubject(), new PrivilegedExceptionAction<AuthenticateResponse>() {
            public AuthenticateResponse run() throws Exception {
              return SpnegoAuthenticator.this.authenticateSession(gssAuthenticationContext, gssToken, session);
            }
          });
    } catch (PrivilegedActionException e) {
      throw new TransportException(e);
    } 
  }
  
  private AuthenticateResponse authenticateSession(GSSAuthenticationContext context, byte[] gssToken, Session session) throws TransportException {
    try {
      logger.debug("Authenticating {} on {} using SPNEGO", context.getUsername(), session.getConnection().getRemoteHostname());
      if (this.gssContext == null) {
        GSSManager gssManager = GSSManager.getInstance();
        Oid spnegoOid = new Oid("1.3.6.1.5.5.2");
        String service = "cifs";
        String hostName = session.getConnection().getRemoteHostname();
        GSSName serverName = gssManager.createName(service + "@" + hostName, GSSName.NT_HOSTBASED_SERVICE);
        this.gssContext = gssManager.createContext(serverName, spnegoOid, context.getCreds(), 0);
        this.gssContext.requestMutualAuth(this.gssContextConfig.isRequestMutualAuth());
        this.gssContext.requestCredDeleg(this.gssContextConfig.isRequestCredDeleg());
      } 
      byte[] newToken = this.gssContext.initSecContext(gssToken, 0, gssToken.length);
      if (newToken != null)
        logger.trace("Received token: {}", ByteArrayUtils.printHex(newToken)); 
      AuthenticateResponse response = new AuthenticateResponse(newToken);
      if (this.gssContext.isEstablished()) {
        ExtendedGSSContext e = (ExtendedGSSContext)this.gssContext;
        Key key = (Key)e.inquireSecContext(InquireType.KRB5_GET_SESSION_KEY);
        if (key != null)
          response.setSigningKey(adjustSessionKeyLength(key.getEncoded())); 
      } 
      return response;
    } catch (GSSException e) {
      throw new TransportException(e);
    } 
  }
  
  private byte[] adjustSessionKeyLength(byte[] key) {
    byte[] newKey;
    if (key.length > 16) {
      newKey = Arrays.copyOfRange(key, 0, 16);
    } else if (key.length < 16) {
      newKey = new byte[16];
      System.arraycopy(key, 0, newKey, 0, key.length);
      Arrays.fill(newKey, key.length, 15, (byte)0);
    } else {
      newKey = key;
    } 
    return newKey;
  }
  
  public void init(SmbConfig config) {
    this.gssContextConfig = config.getClientGSSContextConfig();
  }
  
  public boolean supports(AuthenticationContext context) {
    return context.getClass().equals(GSSAuthenticationContext.class);
  }
}
