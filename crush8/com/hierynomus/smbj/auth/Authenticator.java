package com.hierynomus.smbj.auth;

import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.session.Session;
import java.io.IOException;

public interface Authenticator {
  void init(SmbConfig paramSmbConfig);
  
  boolean supports(AuthenticationContext paramAuthenticationContext);
  
  AuthenticateResponse authenticate(AuthenticationContext paramAuthenticationContext, byte[] paramArrayOfbyte, Session paramSession) throws IOException;
}
