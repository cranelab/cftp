package org.apache.http.conn;

import org.apache.http.HttpHost;

public interface SchemePortResolver {
  int resolve(HttpHost paramHttpHost) throws UnsupportedSchemeException;
}
