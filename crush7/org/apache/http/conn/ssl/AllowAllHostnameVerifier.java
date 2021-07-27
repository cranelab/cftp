package org.apache.http.conn.ssl;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;

@Deprecated
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class AllowAllHostnameVerifier extends AbstractVerifier {
  public static final AllowAllHostnameVerifier INSTANCE = new AllowAllHostnameVerifier();
  
  public final void verify(String host, String[] cns, String[] subjectAlts) {}
  
  public final String toString() {
    return "ALLOW_ALL";
  }
}
