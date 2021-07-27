package com.hierynomus.smbj.auth;

import com.hierynomus.security.SecurityProvider;
import com.hierynomus.smbj.session.Session;
import java.io.IOException;
import java.util.Random;

public interface Authenticator {
  void init(SecurityProvider paramSecurityProvider, Random paramRandom);
  
  boolean supports(AuthenticationContext paramAuthenticationContext);
  
  AuthenticateResponse authenticate(AuthenticationContext paramAuthenticationContext, byte[] paramArrayOfbyte, Session paramSession) throws IOException;
}
