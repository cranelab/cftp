package org.apache.http.protocol;

import java.util.List;
import org.apache.http.HttpRequestInterceptor;

@Deprecated
public interface HttpRequestInterceptorList {
  void addRequestInterceptor(HttpRequestInterceptor paramHttpRequestInterceptor);
  
  void addRequestInterceptor(HttpRequestInterceptor paramHttpRequestInterceptor, int paramInt);
  
  int getRequestInterceptorCount();
  
  HttpRequestInterceptor getRequestInterceptor(int paramInt);
  
  void clearRequestInterceptors();
  
  void removeRequestInterceptorByClass(Class<? extends HttpRequestInterceptor> paramClass);
  
  void setInterceptors(List<?> paramList);
}
