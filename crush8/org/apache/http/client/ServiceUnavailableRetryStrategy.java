package org.apache.http.client;

import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

public interface ServiceUnavailableRetryStrategy {
  boolean retryRequest(HttpResponse paramHttpResponse, int paramInt, HttpContext paramHttpContext);
  
  long getRetryInterval();
}
