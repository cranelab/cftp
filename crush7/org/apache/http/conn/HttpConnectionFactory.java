package org.apache.http.conn;

import org.apache.http.config.ConnectionConfig;

public interface HttpConnectionFactory<T, C extends org.apache.http.HttpConnection> {
  C create(T paramT, ConnectionConfig paramConnectionConfig);
}
