package org.apache.http.impl.client;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;

@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class LaxRedirectStrategy extends DefaultRedirectStrategy {
  public static final LaxRedirectStrategy INSTANCE = new LaxRedirectStrategy();
  
  private static final String[] REDIRECT_METHODS = new String[] { "GET", "POST", "HEAD", "DELETE" };
  
  protected boolean isRedirectable(String method) {
    for (String m : REDIRECT_METHODS) {
      if (m.equalsIgnoreCase(method))
        return true; 
    } 
    return false;
  }
}
