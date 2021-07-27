package com.hierynomus.security.jce;

import com.hierynomus.security.Cipher;
import com.hierynomus.security.Mac;
import com.hierynomus.security.MessageDigest;
import com.hierynomus.security.SecurityException;
import com.hierynomus.security.SecurityProvider;
import java.security.Provider;

public class JceSecurityProvider implements SecurityProvider {
  private final Provider jceProvider;
  
  private final String providerName;
  
  public JceSecurityProvider() {
    this.jceProvider = null;
    this.providerName = null;
  }
  
  public JceSecurityProvider(String providerName) {
    this.providerName = providerName;
    this.jceProvider = null;
  }
  
  public JceSecurityProvider(Provider provider) {
    this.providerName = null;
    this.jceProvider = provider;
  }
  
  public MessageDigest getDigest(String name) throws SecurityException {
    return new JceMessageDigest(name, this.jceProvider, this.providerName);
  }
  
  public Mac getMac(String name) throws SecurityException {
    return new JceMac(name, this.jceProvider, this.providerName);
  }
  
  public Cipher getCipher(String name) throws SecurityException {
    return new JceCipher(name, this.jceProvider, this.providerName);
  }
}
