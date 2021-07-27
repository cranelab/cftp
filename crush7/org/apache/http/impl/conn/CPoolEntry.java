package org.apache.http.impl.conn;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.http.HttpClientConnection;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.conn.ManagedHttpClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.pool.PoolEntry;

@Contract(threading = ThreadingBehavior.SAFE)
class CPoolEntry extends PoolEntry<HttpRoute, ManagedHttpClientConnection> {
  private final Log log;
  
  private volatile boolean routeComplete;
  
  public CPoolEntry(Log log, String id, HttpRoute route, ManagedHttpClientConnection conn, long timeToLive, TimeUnit tunit) {
    super(id, route, conn, timeToLive, tunit);
    this.log = log;
  }
  
  public void markRouteComplete() {
    this.routeComplete = true;
  }
  
  public boolean isRouteComplete() {
    return this.routeComplete;
  }
  
  public void closeConnection() throws IOException {
    HttpClientConnection conn = getConnection();
    conn.close();
  }
  
  public void shutdownConnection() throws IOException {
    HttpClientConnection conn = getConnection();
    conn.shutdown();
  }
  
  public boolean isExpired(long now) {
    boolean expired = super.isExpired(now);
    if (expired && this.log.isDebugEnabled())
      this.log.debug("Connection " + this + " expired @ " + new Date(getExpiry())); 
    return expired;
  }
  
  public boolean isClosed() {
    HttpClientConnection conn = getConnection();
    return !conn.isOpen();
  }
  
  public void close() {
    try {
      closeConnection();
    } catch (IOException ex) {
      this.log.debug("I/O error closing connection", ex);
    } 
  }
}
