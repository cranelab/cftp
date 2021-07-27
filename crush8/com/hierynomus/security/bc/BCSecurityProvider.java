package com.hierynomus.security.bc;

import com.hierynomus.security.Cipher;
import com.hierynomus.security.Mac;
import com.hierynomus.security.MessageDigest;
import com.hierynomus.security.SecurityProvider;

public class BCSecurityProvider implements SecurityProvider {
  public MessageDigest getDigest(String name) {
    return new BCMessageDigest(name);
  }
  
  public Mac getMac(String name) {
    return new BCMac(name);
  }
  
  public Cipher getCipher(String name) {
    return BCCipherFactory.create(name);
  }
}
