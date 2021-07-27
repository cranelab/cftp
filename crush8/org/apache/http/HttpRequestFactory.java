package org.apache.http;

public interface HttpRequestFactory {
  HttpRequest newHttpRequest(RequestLine paramRequestLine) throws MethodNotSupportedException;
  
  HttpRequest newHttpRequest(String paramString1, String paramString2) throws MethodNotSupportedException;
}
