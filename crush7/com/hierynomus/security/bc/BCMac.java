package com.hierynomus.security.bc;

import com.hierynomus.protocol.commons.Factory;
import com.hierynomus.security.Mac;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

public class BCMac implements Mac {
  private static Map<String, Factory<Mac>> lookup = new HashMap<String, Factory<Mac>>();
  
  private final Mac mac;
  
  static {
    lookup.put("HMACSHA256", new Factory<Mac>() {
          public Mac create() {
            return (Mac)new HMac((Digest)new SHA256Digest());
          }
        });
    lookup.put("HMACMD5", new Factory<Mac>() {
          public Mac create() {
            return (Mac)new HMac((Digest)new MD5Digest());
          }
        });
  }
  
  BCMac(String name) {
    this.mac = getMacFactory(name).create();
  }
  
  private Factory<Mac> getMacFactory(String name) {
    Factory<Mac> macFactory = lookup.get(name.toUpperCase());
    if (macFactory == null)
      throw new IllegalArgumentException("No Mac defined for " + name); 
    return macFactory;
  }
  
  public void init(byte[] key) {
    this.mac.init((CipherParameters)new KeyParameter(key));
  }
  
  public void update(byte b) {
    this.mac.update(b);
  }
  
  public void update(byte[] array) {
    this.mac.update(array, 0, array.length);
  }
  
  public void update(byte[] array, int offset, int length) {
    this.mac.update(array, offset, length);
  }
  
  public byte[] doFinal() {
    byte[] output = new byte[this.mac.getMacSize()];
    this.mac.doFinal(output, 0);
    return output;
  }
  
  public void reset() {
    this.mac.reset();
  }
}
