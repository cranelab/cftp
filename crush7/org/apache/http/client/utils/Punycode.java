package org.apache.http.client.utils;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;

@Deprecated
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class Punycode {
  private static final Idn impl;
  
  static {
    Idn idn;
    try {
      idn = new JdkIdn();
    } catch (Exception e) {
      idn = new Rfc3492Idn();
    } 
    impl = idn;
  }
  
  public static String toUnicode(String punycode) {
    return impl.toUnicode(punycode);
  }
}
