package org.apache.http.client.methods;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.http.HttpRequest;
import org.apache.http.client.utils.CloneUtils;
import org.apache.http.concurrent.Cancellable;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.ConnectionReleaseTrigger;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.message.HeaderGroup;
import org.apache.http.params.HttpParams;

public abstract class AbstractExecutionAwareRequest extends AbstractHttpMessage implements HttpExecutionAware, AbortableHttpRequest, Cloneable, HttpRequest {
  private final AtomicBoolean aborted = new AtomicBoolean(false);
  
  private final AtomicReference<Cancellable> cancellableRef = new AtomicReference<Cancellable>(null);
  
  @Deprecated
  public void setConnectionRequest(final ClientConnectionRequest connRequest) {
    setCancellable(new Cancellable() {
          public boolean cancel() {
            connRequest.abortRequest();
            return true;
          }
        });
  }
  
  @Deprecated
  public void setReleaseTrigger(final ConnectionReleaseTrigger releaseTrigger) {
    setCancellable(new Cancellable() {
          public boolean cancel() {
            try {
              releaseTrigger.abortConnection();
              return true;
            } catch (IOException ex) {
              return false;
            } 
          }
        });
  }
  
  public void abort() {
    if (this.aborted.compareAndSet(false, true)) {
      Cancellable cancellable = this.cancellableRef.getAndSet(null);
      if (cancellable != null)
        cancellable.cancel(); 
    } 
  }
  
  public boolean isAborted() {
    return this.aborted.get();
  }
  
  public void setCancellable(Cancellable cancellable) {
    if (!this.aborted.get())
      this.cancellableRef.set(cancellable); 
  }
  
  public Object clone() throws CloneNotSupportedException {
    AbstractExecutionAwareRequest clone = (AbstractExecutionAwareRequest)super.clone();
    clone.headergroup = CloneUtils.<HeaderGroup>cloneObject(this.headergroup);
    clone.params = CloneUtils.<HttpParams>cloneObject(this.params);
    return clone;
  }
  
  public void completed() {
    this.cancellableRef.set(null);
  }
  
  public void reset() {
    Cancellable cancellable = this.cancellableRef.getAndSet(null);
    if (cancellable != null)
      cancellable.cancel(); 
    this.aborted.set(false);
  }
}
