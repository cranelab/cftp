package com.hierynomus.security.bc;

import com.hierynomus.security.Cipher;
import com.hierynomus.security.Mac;
import com.hierynomus.security.MessageDigest;
import com.hierynomus.security.SecurityException;
import com.hierynomus.security.SecurityProvider;

public class BCSecurityProvider implements SecurityProvider {
  public MessageDigest getDigest(String name) throws SecurityException {
    return new BCMessageDigest(name);
  }
  
  public Mac getMac(String name) throws SecurityException {
    return new BCMac(name);
  }
  
  public Cipher getCipher(String name) throws SecurityException {
    return BCCipherFactory.create(name);
  }
}
