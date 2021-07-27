package com.hierynomus.security.jce;

import com.hierynomus.security.Mac;
import com.hierynomus.security.SecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class JceMac implements Mac {
  private final String algorithm;
  
  private Mac mac;
  
  public JceMac(String algorithm, Provider jceProvider, String providerName) throws SecurityException {
    this.algorithm = algorithm;
    try {
      if (jceProvider != null) {
        this.mac = Mac.getInstance(algorithm, jceProvider);
      } else if (providerName != null) {
        this.mac = Mac.getInstance(algorithm, providerName);
      } else {
        this.mac = Mac.getInstance(algorithm);
      } 
    } catch (NoSuchProviderException e) {
      throw new SecurityException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new SecurityException(e);
    } 
  }
  
  public void init(byte[] key) throws SecurityException {
    try {
      this.mac.init(new SecretKeySpec(key, this.algorithm));
    } catch (InvalidKeyException e) {
      throw new SecurityException(e);
    } 
  }
  
  public void update(byte b) {
    this.mac.update(b);
  }
  
  public void update(byte[] array) {
    this.mac.update(array);
  }
  
  public void update(byte[] array, int offset, int length) {
    this.mac.update(array, offset, length);
  }
  
  public byte[] doFinal() {
    return this.mac.doFinal();
  }
  
  public void reset() {
    this.mac.reset();
  }
}
