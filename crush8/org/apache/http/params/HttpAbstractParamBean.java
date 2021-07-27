package org.apache.http.params;

import org.apache.http.util.Args;

@Deprecated
public abstract class HttpAbstractParamBean {
  protected final HttpParams params;
  
  public HttpAbstractParamBean(HttpParams params) {
    this.params = Args.<HttpParams>notNull(params, "HTTP parameters");
  }
}
