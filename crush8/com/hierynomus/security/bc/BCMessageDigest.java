package com.hierynomus.security.bc;

import com.hierynomus.protocol.commons.Factory;
import com.hierynomus.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD4Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

public class BCMessageDigest implements MessageDigest {
  private static Map<String, Factory<Digest>> lookup = new HashMap<>();
  
  private final Digest digest;
  
  static {
    lookup.put("SHA256", new Factory<Digest>() {
          public Digest create() {
            return (Digest)new SHA256Digest();
          }
        });
    lookup.put("MD4", new Factory<Digest>() {
          public Digest create() {
            return (Digest)new MD4Digest();
          }
        });
  }
  
  BCMessageDigest(String name) {
    this.digest = getDigest(name);
  }
  
  private Digest getDigest(String name) {
    Factory<Digest> digestFactory = lookup.get(name);
    if (digestFactory == null)
      throw new IllegalArgumentException("No MessageDigest " + name + " defined in BouncyCastle"); 
    return digestFactory.create();
  }
  
  public void update(byte[] bytes) {
    this.digest.update(bytes, 0, bytes.length);
  }
  
  public byte[] digest() {
    byte[] output = new byte[this.digest.getDigestSize()];
    this.digest.doFinal(output, 0);
    return output;
  }
  
  public void reset() {
    this.digest.reset();
  }
}
