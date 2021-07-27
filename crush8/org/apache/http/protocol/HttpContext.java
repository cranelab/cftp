package org.apache.http.protocol;

public interface HttpContext {
  public static final String RESERVED_PREFIX = "http.";
  
  Object getAttribute(String paramString);
  
  void setAttribute(String paramString, Object paramObject);
  
  Object removeAttribute(String paramString);
}
