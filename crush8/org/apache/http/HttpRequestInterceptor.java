package org.apache.http;

import java.io.IOException;
import org.apache.http.protocol.HttpContext;

public interface HttpRequestInterceptor {
  void process(HttpRequest paramHttpRequest, HttpContext paramHttpContext) throws HttpException, IOException;
}
