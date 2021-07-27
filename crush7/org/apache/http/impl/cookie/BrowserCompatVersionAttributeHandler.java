package org.apache.http.impl.cookie;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.cookie.SetCookie;
import org.apache.http.util.Args;

@Deprecated
@Contract(threading = ThreadingBehavior.IMMUTABLE)
public class BrowserCompatVersionAttributeHandler extends AbstractCookieAttributeHandler implements CommonCookieAttributeHandler {
  public void parse(SetCookie cookie, String value) throws MalformedCookieException {
    Args.notNull(cookie, "Cookie");
    if (value == null)
      throw new MalformedCookieException("Missing value for version attribute"); 
    int version = 0;
    try {
      version = Integer.parseInt(value);
    } catch (NumberFormatException e) {}
    cookie.setVersion(version);
  }
  
  public String getAttributeName() {
    return "version";
  }
}
