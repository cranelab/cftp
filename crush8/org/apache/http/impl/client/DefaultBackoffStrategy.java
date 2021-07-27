package org.apache.http.impl.client;

import org.apache.http.HttpResponse;
import org.apache.http.client.ConnectionBackoffStrategy;

public class DefaultBackoffStrategy implements ConnectionBackoffStrategy {
  public boolean shouldBackoff(Throwable t) {
    return (t instanceof java.net.SocketTimeoutException || t instanceof java.net.ConnectException);
  }
  
  public boolean shouldBackoff(HttpResponse resp) {
    return (resp.getStatusLine().getStatusCode() == 503);
  }
}
