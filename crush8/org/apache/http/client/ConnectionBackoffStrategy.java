package org.apache.http.client;

import org.apache.http.HttpResponse;

public interface ConnectionBackoffStrategy {
  boolean shouldBackoff(Throwable paramThrowable);
  
  boolean shouldBackoff(HttpResponse paramHttpResponse);
}
