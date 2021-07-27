package org.apache.http.client.methods;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.Args;

public class RequestBuilder {
  private String method;
  
  private Charset charset;
  
  private ProtocolVersion version;
  
  private URI uri;
  
  private HeaderGroup headergroup;
  
  private HttpEntity entity;
  
  private List<NameValuePair> parameters;
  
  private RequestConfig config;
  
  RequestBuilder(String method) {
    this.charset = Consts.UTF_8;
    this.method = method;
  }
  
  RequestBuilder(String method, URI uri) {
    this.method = method;
    this.uri = uri;
  }
  
  RequestBuilder(String method, String uri) {
    this.method = method;
    this.uri = (uri != null) ? URI.create(uri) : null;
  }
  
  RequestBuilder() {
    this(null);
  }
  
  public static RequestBuilder create(String method) {
    Args.notBlank(method, "HTTP method");
    return new RequestBuilder(method);
  }
  
  public static RequestBuilder get() {
    return new RequestBuilder("GET");
  }
  
  public static RequestBuilder get(URI uri) {
    return new RequestBuilder("GET", uri);
  }
  
  public static RequestBuilder get(String uri) {
    return new RequestBuilder("GET", uri);
  }
  
  public static RequestBuilder head() {
    return new RequestBuilder("HEAD");
  }
  
  public static RequestBuilder head(URI uri) {
    return new RequestBuilder("HEAD", uri);
  }
  
  public static RequestBuilder head(String uri) {
    return new RequestBuilder("HEAD", uri);
  }
  
  public static RequestBuilder patch() {
    return new RequestBuilder("PATCH");
  }
  
  public static RequestBuilder patch(URI uri) {
    return new RequestBuilder("PATCH", uri);
  }
  
  public static RequestBuilder patch(String uri) {
    return new RequestBuilder("PATCH", uri);
  }
  
  public static RequestBuilder post() {
    return new RequestBuilder("POST");
  }
  
  public static RequestBuilder post(URI uri) {
    return new RequestBuilder("POST", uri);
  }
  
  public static RequestBuilder post(String uri) {
    return new RequestBuilder("POST", uri);
  }
  
  public static RequestBuilder put() {
    return new RequestBuilder("PUT");
  }
  
  public static RequestBuilder put(URI uri) {
    return new RequestBuilder("PUT", uri);
  }
  
  public static RequestBuilder put(String uri) {
    return new RequestBuilder("PUT", uri);
  }
  
  public static RequestBuilder delete() {
    return new RequestBuilder("DELETE");
  }
  
  public static RequestBuilder delete(URI uri) {
    return new RequestBuilder("DELETE", uri);
  }
  
  public static RequestBuilder delete(String uri) {
    return new RequestBuilder("DELETE", uri);
  }
  
  public static RequestBuilder trace() {
    return new RequestBuilder("TRACE");
  }
  
  public static RequestBuilder trace(URI uri) {
    return new RequestBuilder("TRACE", uri);
  }
  
  public static RequestBuilder trace(String uri) {
    return new RequestBuilder("TRACE", uri);
  }
  
  public static RequestBuilder options() {
    return new RequestBuilder("OPTIONS");
  }
  
  public static RequestBuilder options(URI uri) {
    return new RequestBuilder("OPTIONS", uri);
  }
  
  public static RequestBuilder options(String uri) {
    return new RequestBuilder("OPTIONS", uri);
  }
  
  public static RequestBuilder copy(HttpRequest request) {
    Args.notNull(request, "HTTP request");
    return (new RequestBuilder()).doCopy(request);
  }
  
  private RequestBuilder doCopy(HttpRequest request) {
    if (request == null)
      return this; 
    this.method = request.getRequestLine().getMethod();
    this.version = request.getRequestLine().getProtocolVersion();
    if (this.headergroup == null)
      this.headergroup = new HeaderGroup(); 
    this.headergroup.clear();
    this.headergroup.setHeaders(request.getAllHeaders());
    this.parameters = null;
    this.entity = null;
    if (request instanceof HttpEntityEnclosingRequest) {
      HttpEntity originalEntity = ((HttpEntityEnclosingRequest)request).getEntity();
      ContentType contentType = ContentType.get(originalEntity);
      if (contentType != null && contentType.getMimeType().equals(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())) {
        try {
          List<NameValuePair> formParams = URLEncodedUtils.parse(originalEntity);
          if (!formParams.isEmpty())
            this.parameters = formParams; 
        } catch (IOException ignore) {}
      } else {
        this.entity = originalEntity;
      } 
    } 
    if (request instanceof HttpUriRequest) {
      this.uri = ((HttpUriRequest)request).getURI();
    } else {
      this.uri = URI.create(request.getRequestLine().getUri());
    } 
    if (request instanceof Configurable) {
      this.config = ((Configurable)request).getConfig();
    } else {
      this.config = null;
    } 
    return this;
  }
  
  public RequestBuilder setCharset(Charset charset) {
    this.charset = charset;
    return this;
  }
  
  public Charset getCharset() {
    return this.charset;
  }
  
  public String getMethod() {
    return this.method;
  }
  
  public ProtocolVersion getVersion() {
    return this.version;
  }
  
  public RequestBuilder setVersion(ProtocolVersion version) {
    this.version = version;
    return this;
  }
  
  public URI getUri() {
    return this.uri;
  }
  
  public RequestBuilder setUri(URI uri) {
    this.uri = uri;
    return this;
  }
  
  public RequestBuilder setUri(String uri) {
    this.uri = (uri != null) ? URI.create(uri) : null;
    return this;
  }
  
  public Header getFirstHeader(String name) {
    return (this.headergroup != null) ? this.headergroup.getFirstHeader(name) : null;
  }
  
  public Header getLastHeader(String name) {
    return (this.headergroup != null) ? this.headergroup.getLastHeader(name) : null;
  }
  
  public Header[] getHeaders(String name) {
    return (this.headergroup != null) ? this.headergroup.getHeaders(name) : null;
  }
  
  public RequestBuilder addHeader(Header header) {
    if (this.headergroup == null)
      this.headergroup = new HeaderGroup(); 
    this.headergroup.addHeader(header);
    return this;
  }
  
  public RequestBuilder addHeader(String name, String value) {
    if (this.headergroup == null)
      this.headergroup = new HeaderGroup(); 
    this.headergroup.addHeader(new BasicHeader(name, value));
    return this;
  }
  
  public RequestBuilder removeHeader(Header header) {
    if (this.headergroup == null)
      this.headergroup = new HeaderGroup(); 
    this.headergroup.removeHeader(header);
    return this;
  }
  
  public RequestBuilder removeHeaders(String name) {
    if (name == null || this.headergroup == null)
      return this; 
    for (HeaderIterator i = this.headergroup.iterator(); i.hasNext(); ) {
      Header header = i.nextHeader();
      if (name.equalsIgnoreCase(header.getName()))
        i.remove(); 
    } 
    return this;
  }
  
  public RequestBuilder setHeader(Header header) {
    if (this.headergroup == null)
      this.headergroup = new HeaderGroup(); 
    this.headergroup.updateHeader(header);
    return this;
  }
  
  public RequestBuilder setHeader(String name, String value) {
    if (this.headergroup == null)
      this.headergroup = new HeaderGroup(); 
    this.headergroup.updateHeader(new BasicHeader(name, value));
    return this;
  }
  
  public HttpEntity getEntity() {
    return this.entity;
  }
  
  public RequestBuilder setEntity(HttpEntity entity) {
    this.entity = entity;
    return this;
  }
  
  public List<NameValuePair> getParameters() {
    return (this.parameters != null) ? new ArrayList<NameValuePair>(this.parameters) : new ArrayList<NameValuePair>();
  }
  
  public RequestBuilder addParameter(NameValuePair nvp) {
    Args.notNull(nvp, "Name value pair");
    if (this.parameters == null)
      this.parameters = new LinkedList<NameValuePair>(); 
    this.parameters.add(nvp);
    return this;
  }
  
  public RequestBuilder addParameter(String name, String value) {
    return addParameter(new BasicNameValuePair(name, value));
  }
  
  public RequestBuilder addParameters(NameValuePair... nvps) {
    for (NameValuePair nvp : nvps)
      addParameter(nvp); 
    return this;
  }
  
  public RequestConfig getConfig() {
    return this.config;
  }
  
  public RequestBuilder setConfig(RequestConfig config) {
    this.config = config;
    return this;
  }
  
  public HttpUriRequest build() {
    HttpRequestBase result;
    URI uriNotNull = (this.uri != null) ? this.uri : URI.create("/");
    HttpEntity entityCopy = this.entity;
    if (this.parameters != null && !this.parameters.isEmpty())
      if (entityCopy == null && ("POST".equalsIgnoreCase(this.method) || "PUT".equalsIgnoreCase(this.method))) {
        entityCopy = new UrlEncodedFormEntity(this.parameters, (this.charset != null) ? this.charset : HTTP.DEF_CONTENT_CHARSET);
      } else {
        try {
          uriNotNull = (new URIBuilder(uriNotNull)).setCharset(this.charset).addParameters(this.parameters).build();
        } catch (URISyntaxException ex) {}
      }  
    if (entityCopy == null) {
      result = new InternalRequest(this.method);
    } else {
      InternalEntityEclosingRequest request = new InternalEntityEclosingRequest(this.method);
      request.setEntity(entityCopy);
      result = request;
    } 
    result.setProtocolVersion(this.version);
    result.setURI(uriNotNull);
    if (this.headergroup != null)
      result.setHeaders(this.headergroup.getAllHeaders()); 
    result.setConfig(this.config);
    return result;
  }
  
  static class InternalRequest extends HttpRequestBase {
    private final String method;
    
    InternalRequest(String method) {
      this.method = method;
    }
    
    public String getMethod() {
      return this.method;
    }
  }
  
  static class InternalEntityEclosingRequest extends HttpEntityEnclosingRequestBase {
    private final String method;
    
    InternalEntityEclosingRequest(String method) {
      this.method = method;
    }
    
    public String getMethod() {
      return this.method;
    }
  }
}
