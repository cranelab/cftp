package org.apache.http.impl.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;

@Contract(threading = ThreadingBehavior.SAFE)
public abstract class CloseableHttpClient implements HttpClient, Closeable {
  private final Log log = LogFactory.getLog(getClass());
  
  public CloseableHttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
    return doExecute(target, request, context);
  }
  
  public CloseableHttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
    Args.notNull(request, "HTTP request");
    return doExecute(determineTarget(request), request, context);
  }
  
  private static HttpHost determineTarget(HttpUriRequest request) throws ClientProtocolException {
    HttpHost target = null;
    URI requestURI = request.getURI();
    if (requestURI.isAbsolute()) {
      target = URIUtils.extractHost(requestURI);
      if (target == null)
        throw new ClientProtocolException("URI does not specify a valid host name: " + requestURI); 
    } 
    return target;
  }
  
  public CloseableHttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
    return execute(request, (HttpContext)null);
  }
  
  public CloseableHttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
    return doExecute(target, request, null);
  }
  
  public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
    return execute(request, responseHandler, (HttpContext)null);
  }
  
  public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
    HttpHost target = determineTarget(request);
    return execute(target, request, responseHandler, context);
  }
  
  public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
    return execute(target, request, responseHandler, null);
  }
  
  public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
    Args.notNull(responseHandler, "Response handler");
    CloseableHttpResponse response = execute(target, request, context);
    try {
      T result = responseHandler.handleResponse(response);
      HttpEntity entity = response.getEntity();
      EntityUtils.consume(entity);
      return result;
    } catch (ClientProtocolException t) {
      HttpEntity entity = response.getEntity();
      try {
        EntityUtils.consume(entity);
      } catch (Exception t2) {
        this.log.warn("Error consuming content after an exception.", t2);
      } 
      throw t;
    } finally {
      response.close();
    } 
  }
  
  protected abstract CloseableHttpResponse doExecute(HttpHost paramHttpHost, HttpRequest paramHttpRequest, HttpContext paramHttpContext) throws IOException, ClientProtocolException;
}
