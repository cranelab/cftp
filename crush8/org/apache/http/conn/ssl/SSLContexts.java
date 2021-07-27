package org.apache.http.conn.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;

@Deprecated
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class SSLContexts {
  public static SSLContext createDefault() throws SSLInitializationException {
    try {
      SSLContext sslcontext = SSLContext.getInstance("TLS");
      sslcontext.init(null, null, null);
      return sslcontext;
    } catch (NoSuchAlgorithmException ex) {
      throw new SSLInitializationException(ex.getMessage(), ex);
    } catch (KeyManagementException ex) {
      throw new SSLInitializationException(ex.getMessage(), ex);
    } 
  }
  
  public static SSLContext createSystemDefault() throws SSLInitializationException {
    try {
      return SSLContext.getDefault();
    } catch (NoSuchAlgorithmException ex) {
      return createDefault();
    } 
  }
  
  public static SSLContextBuilder custom() {
    return new SSLContextBuilder();
  }
}
