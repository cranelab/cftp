package org.apache.http.protocol;

import java.util.List;
import org.apache.http.HttpResponseInterceptor;

@Deprecated
public interface HttpResponseInterceptorList {
  void addResponseInterceptor(HttpResponseInterceptor paramHttpResponseInterceptor);
  
  void addResponseInterceptor(HttpResponseInterceptor paramHttpResponseInterceptor, int paramInt);
  
  int getResponseInterceptorCount();
  
  HttpResponseInterceptor getResponseInterceptor(int paramInt);
  
  void clearResponseInterceptors();
  
  void removeResponseInterceptorByClass(Class<? extends HttpResponseInterceptor> paramClass);
  
  void setInterceptors(List<?> paramList);
}
