package com.hierynomus.security.jce;

import com.hierynomus.security.MessageDigest;
import com.hierynomus.security.SecurityException;
import java.lang.reflect.InvocationTargetException;
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
        tryMd4();
      } else {
        throw new SecurityException(e);
      } 
    } catch (NoSuchProviderException e) {
      throw new SecurityException(e);
    } 
  }
  
  private void tryMd4() throws SecurityException {
    try {
      Class<?> md4Class = Class.forName("sun.security.provider.MD4");
      this.md = (MessageDigest)md4Class.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
    } catch (ClassNotFoundException e1) {
      throw new SecurityException(e1);
    } catch (IllegalAccessException e1) {
      throw new SecurityException(e1);
    } catch (InvocationTargetException e1) {
      throw new SecurityException(e1);
    } catch (NoSuchMethodException e1) {
      throw new SecurityException(e1);
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
