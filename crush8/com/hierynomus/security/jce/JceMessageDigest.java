package com.hierynomus.security.jce;

import com.hierynomus.security.MessageDigest;
import com.hierynomus.security.SecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

public class JceMessageDigest implements MessageDigest {
  private MessageDigest md;
  
  JceMessageDigest(String algorithm, Provider jceProvider, String providerName) throws SecurityException {
    try {
      if (jceProvider != null) {
        this.md = MessageDigest.getInstance(algorithm, jceProvider);
      } else if (providerName != null) {
        this.md = MessageDigest.getInstance(algorithm, providerName);
      } else {
        this.md = MessageDigest.getInstance(algorithm);
      } 
    } catch (NoSuchAlgorithmException e) {
      if ("MD4".equals(algorithm)) {
        tryMd4(e);
      } else {
        throw new SecurityException(e);
      } 
    } catch (NoSuchProviderException e) {
      throw new SecurityException(e);
    } 
  }
  
  private void tryMd4(NoSuchAlgorithmException originalException) throws SecurityException {
    try {
      Class<?> md4Class = Class.forName("sun.security.provider.MD4");
      this.md = (MessageDigest)md4Class.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
    } catch (ClassNotFoundException|IllegalAccessException|java.lang.reflect.InvocationTargetException|NoSuchMethodException e1) {
      throw new SecurityException(originalException);
    } 
  }
  
  public void update(byte[] bytes) {
    this.md.update(bytes);
  }
  
  public byte[] digest() {
    return this.md.digest();
  }
  
  public void reset() {
    this.md.reset();
  }
}
