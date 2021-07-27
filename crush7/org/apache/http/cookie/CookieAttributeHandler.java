package org.apache.http.cookie;

public interface CookieAttributeHandler {
  void parse(SetCookie paramSetCookie, String paramString) throws MalformedCookieException;
  
  void validate(Cookie paramCookie, CookieOrigin paramCookieOrigin) throws MalformedCookieException;
  
  boolean match(Cookie paramCookie, CookieOrigin paramCookieOrigin);
}
