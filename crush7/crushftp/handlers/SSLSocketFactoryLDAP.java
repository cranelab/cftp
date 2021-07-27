package crushftp.handlers;

import com.crushftp.client.TrustManagerCustom;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class SSLSocketFactoryLDAP {
  public static Object single = new Object();
  
  static SSLSocketFactory sf = null;
  
  public SSLSocketFactoryLDAP() {
    synchronized (single) {
      if (sf == null)
        try {
          TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
          KeyStore keyStore = KeyStore.getInstance("JKS");
          keyStore.load(getClass().getResource("/assets/builtin").openStream(), "crushftp".toCharArray());
          tmf.init(keyStore);
          SSLContext ctx = SSLContext.getInstance("TLS");
          ctx.init(null, new TrustManager[] { new TrustManagerCustom(null, true, true) }null);
          sf = ctx.getSocketFactory();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
        }  
    } 
  }
  
  public static SSLSocketFactory getDefault() {
    return sf;
  }
}
