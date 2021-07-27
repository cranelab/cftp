package org.apache.http.pool;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.util.Args;
import org.apache.http.util.Asserts;

@Contract(threading = ThreadingBehavior.SAFE_CONDITIONAL)
public abstract class AbstractConnPool<T, C, E extends PoolEntry<T, C>> implements ConnPool<T, E>, ConnPoolControl<T> {
  private final Lock lock;
  
  private final Condition condition;
  
  private final ConnFactory<T, C> connFactory;
  
  private final Map<T, RouteSpecificPool<T, C, E>> routeToPool;
  
  private final Set<E> leased;
  
  private final LinkedList<E> available;
  
  private final LinkedList<Future<E>> pending;
  
  private final Map<T, Integer> maxPerRoute;
  
  private volatile boolean isShutDown;
  
  private volatile int defaultMaxPerRoute;
  
  private volatile int maxTotal;
  
  private volatile int validateAfterInactivity;
  
  public AbstractConnPool(ConnFactory<T, C> connFactory, int defaultMaxPerRoute, int maxTotal) {
    this.connFactory = Args.<ConnFactory<T, C>>notNull(connFactory, "Connection factory");
    this.defaultMaxPerRoute = Args.positive(defaultMaxPerRoute, "Max per route value");
    this.maxTotal = Args.positive(maxTotal, "Max total value");
    this.lock = new ReentrantLock();
    this.condition = this.lock.newCondition();
    this.routeToPool = new HashMap<T, RouteSpecificPool<T, C, E>>();
    this.leased = new HashSet<E>();
    this.available = new LinkedList<E>();
    this.pending = new LinkedList<Future<E>>();
    this.maxPerRoute = new HashMap<T, Integer>();
  }
  
  protected void onLease(E entry) {}
  
  protected void onRelease(E entry) {}
  
  protected void onReuse(E entry) {}
  
  protected boolean validate(E entry) {
    return true;
  }
  
  public boolean isShutdown() {
    return this.isShutDown;
  }
  
  public void shutdown() throws IOException {
    if (this.isShutDown)
      return; 
    this.isShutDown = true;
    this.lock.lock();
    try {
      for (PoolEntry poolEntry : this.available)
        poolEntry.close(); 
      for (PoolEntry poolEntry : this.leased)
        poolEntry.close(); 
      for (RouteSpecificPool<T, C, E> pool : this.routeToPool.values())
        pool.shutdown(); 
      this.routeToPool.clear();
      this.leased.clear();
      this.available.clear();
    } finally {
      this.lock.unlock();
    } 
  }
  
  private RouteSpecificPool<T, C, E> getPool(final T route) {
    RouteSpecificPool<T, C, E> pool = this.routeToPool.get(route);
    if (pool == null) {
      pool = new RouteSpecificPool<T, C, E>(route) {
          protected E createEntry(C conn) {
            return AbstractConnPool.this.createEntry(route, conn);
          }
        };
      this.routeToPool.put(route, pool);
    } 
    return pool;
  }
  
  public Future<E> lease(final T route, final Object state, final FutureCallback<E> callback) {
    Args.notNull(route, "Route");
    Asserts.check(!this.isShutDown, "Connection pool shut down");
    return new Future<E>() {
        private volatile boolean cancelled;
        
        private volatile boolean done;
        
        private volatile E entry;
        
        public boolean cancel(boolean mayInterruptIfRunning) {
          this.cancelled = true;
          AbstractConnPool.this.lock.lock();
          try {
            AbstractConnPool.this.condition.signalAll();
          } finally {
            AbstractConnPool.this.lock.unlock();
          } 
          synchronized (this) {
            boolean result = !this.done;
            this.done = true;
            if (callback != null)
              callback.cancelled(); 
            return result;
          } 
        }
        
        public boolean isCancelled() {
          return this.cancelled;
        }
        
        public boolean isDone() {
          return this.done;
        }
        
        public E get() throws InterruptedException, ExecutionException {
          try {
            return get(0L, TimeUnit.MILLISECONDS);
          } catch (TimeoutException ex) {
            throw new ExecutionException(ex);
          } 
        }
        
        public E get(long timeout, TimeUnit tunit) throws InterruptedException, ExecutionException, TimeoutException {
          if (this.entry != null)
            return this.entry; 
          synchronized (this) {
            while (true) {
              try {
                PoolEntry poolEntry = (PoolEntry)AbstractConnPool.this.getPoolEntryBlocking((T)route, state, timeout, tunit, this);
                if (AbstractConnPool.this.validateAfterInactivity > 0 && 
                  poolEntry.getUpdated() + AbstractConnPool.this.validateAfterInactivity <= System.currentTimeMillis() && 
                  !AbstractConnPool.this.validate(poolEntry)) {
                  poolEntry.close();
                  AbstractConnPool.this.release(poolEntry, false);
                  continue;
                } 
                this.entry = (E)poolEntry;
                this.done = true;
                AbstractConnPool.this.onLease(this.entry);
                if (callback != null)
                  callback.completed(this.entry); 
                return this.entry;
              } catch (IOException ex) {
                this.done = true;
                if (callback != null)
                  callback.failed(ex); 
                throw new ExecutionException(ex);
              } 
            } 
          } 
        }
      };
  }
  
  public Future<E> lease(T route, Object state) {
    return lease(route, state, null);
  }
  
  private E getPoolEntryBlocking(T route, Object state, long timeout, TimeUnit tunit, Future<E> future) throws IOException, InterruptedException, TimeoutException {
    Date deadline = null;
    if (timeout > 0L)
      deadline = new Date(System.currentTimeMillis() + tunit.toMillis(timeout)); 
    this.lock.lock();
    try {
      boolean success;
      RouteSpecificPool<T, C, E> pool = getPool(route);
      do {
        E entry;
        Asserts.check(!this.isShutDown, "Connection pool shut down");
        while (true) {
          entry = pool.getFree(state);
          if (entry == null)
            break; 
          if (entry.isExpired(System.currentTimeMillis()))
            entry.close(); 
          if (entry.isClosed()) {
            this.available.remove(entry);
            pool.free(entry, false);
            continue;
          } 
          break;
        } 
        if (entry != null) {
          this.available.remove(entry);
          this.leased.add(entry);
          onReuse(entry);
          return entry;
        } 
        int maxPerRoute = getMax(route);
        int excess = Math.max(0, pool.getAllocatedCount() + 1 - maxPerRoute);
        if (excess > 0)
          for (int i = 0; i < excess; i++) {
            E lastUsed = pool.getLastUsed();
            if (lastUsed == null)
              break; 
            lastUsed.close();
            this.available.remove(lastUsed);
            pool.remove(lastUsed);
          }  
        if (pool.getAllocatedCount() < maxPerRoute) {
          int totalUsed = this.leased.size();
          int freeCapacity = Math.max(this.maxTotal - totalUsed, 0);
          if (freeCapacity > 0) {
            int totalAvailable = this.available.size();
            if (totalAvailable > freeCapacity - 1 && 
              !this.available.isEmpty()) {
              PoolEntry poolEntry = (PoolEntry)this.available.removeLast();
              poolEntry.close();
              RouteSpecificPool<T, C, E> otherpool = getPool((T)poolEntry.getRoute());
              otherpool.remove((E)poolEntry);
            } 
            C conn = this.connFactory.create(route);
            entry = pool.add(conn);
            this.leased.add(entry);
            return entry;
          } 
        } 
        success = false;
        try {
          if (future.isCancelled())
            throw new InterruptedException("Operation interrupted"); 
          pool.queue(future);
          this.pending.add(future);
          if (deadline != null) {
            success = this.condition.awaitUntil(deadline);
          } else {
            this.condition.await();
            success = true;
          } 
          if (future.isCancelled())
            throw new InterruptedException("Operation interrupted"); 
        } finally {
          pool.unqueue(future);
          this.pending.remove(future);
        } 
      } while (success || deadline == null || deadline.getTime() > System.currentTimeMillis());
      throw new TimeoutException("Timeout waiting for connection");
    } finally {
      this.lock.unlock();
    } 
  }
  
  public void release(E entry, boolean reusable) {
    this.lock.lock();
    try {
      if (this.leased.remove(entry)) {
        RouteSpecificPool<T, C, E> pool = getPool((T)entry.getRoute());
        pool.free(entry, reusable);
        if (reusable && !this.isShutDown) {
          this.available.addFirst(entry);
        } else {
          entry.close();
        } 
        onRelease(entry);
        Future<E> future = pool.nextPending();
        if (future != null) {
          this.pending.remove(future);
        } else {
          future = this.pending.poll();
        } 
        if (future != null)
          this.condition.signalAll(); 
      } 
    } finally {
      this.lock.unlock();
    } 
  }
  
  private int getMax(T route) {
    Integer v = this.maxPerRoute.get(route);
    if (v != null)
      return v.intValue(); 
    return this.defaultMaxPerRoute;
  }
  
  public void setMaxTotal(int max) {
    Args.positive(max, "Max value");
    this.lock.lock();
    try {
      this.maxTotal = max;
    } finally {
      this.lock.unlock();
    } 
  }
  
  public int getMaxTotal() {
    this.lock.lock();
    try {
      return this.maxTotal;
    } finally {
      this.lock.unlock();
    } 
  }
  
  public void setDefaultMaxPerRoute(int max) {
    Args.positive(max, "Max per route value");
    this.lock.lock();
    try {
      this.defaultMaxPerRoute = max;
    } finally {
      this.lock.unlock();
    } 
  }
  
  public int getDefaultMaxPerRoute() {
    this.lock.lock();
    try {
      return this.defaultMaxPerRoute;
    } finally {
      this.lock.unlock();
    } 
  }
  
  public void setMaxPerRoute(T route, int max) {
    Args.notNull(route, "Route");
    Args.positive(max, "Max per route value");
    this.lock.lock();
    try {
      this.maxPerRoute.put(route, Integer.valueOf(max));
    } finally {
      this.lock.unlock();
    } 
  }
  
  public int getMaxPerRoute(T route) {
    Args.notNull(route, "Route");
    this.lock.lock();
    try {
      return getMax(route);
    } finally {
      this.lock.unlock();
    } 
  }
  
  public PoolStats getTotalStats() {
    this.lock.lock();
    try {
      return new PoolStats(this.leased.size(), this.pending.size(), this.available.size(), this.maxTotal);
    } finally {
      this.lock.unlock();
    } 
  }
  
  public PoolStats getStats(T route) {
    Args.notNull(route, "Route");
    this.lock.lock();
    try {
      RouteSpecificPool<T, C, E> pool = getPool(route);
      return new PoolStats(pool.getLeasedCount(), pool.getPendingCount(), pool.getAvailableCount(), getMax(route));
    } finally {
      this.lock.unlock();
    } 
  }
  
  public Set<T> getRoutes() {
    this.lock.lock();
    try {
      return new HashSet(this.routeToPool.keySet());
    } finally {
      this.lock.unlock();
    } 
  }
  
  protected void enumAvailable(PoolEntryCallback<T, C> callback) {
    this.lock.lock();
    try {
      Iterator<E> it = this.available.iterator();
      while (it.hasNext()) {
        PoolEntry<T, C> poolEntry = (PoolEntry)it.next();
        callback.process(poolEntry);
        if (poolEntry.isClosed()) {
          RouteSpecificPool<T, C, E> pool = getPool(poolEntry.getRoute());
          pool.remove((E)poolEntry);
          it.remove();
        } 
      } 
      purgePoolMap();
    } finally {
      this.lock.unlock();
    } 
  }
  
  protected void enumLeased(PoolEntryCallback<T, C> callback) {
    this.lock.lock();
    try {
      Iterator<E> it = this.leased.iterator();
      while (it.hasNext()) {
        PoolEntry<T, C> poolEntry = (PoolEntry)it.next();
        callback.process(poolEntry);
      } 
    } finally {
      this.lock.unlock();
    } 
  }
  
  private void purgePoolMap() {
    Iterator<Map.Entry<T, RouteSpecificPool<T, C, E>>> it = this.routeToPool.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<T, RouteSpecificPool<T, C, E>> entry = it.next();
      RouteSpecificPool<T, C, E> pool = entry.getValue();
      if (pool.getPendingCount() + pool.getAllocatedCount() == 0)
        it.remove(); 
    } 
  }
  
  public void closeIdle(long idletime, TimeUnit tunit) {
    Args.notNull(tunit, "Time unit");
    long time = tunit.toMillis(idletime);
    if (time < 0L)
      time = 0L; 
    final long deadline = System.currentTimeMillis() - time;
    enumAvailable(new PoolEntryCallback<T, C>() {
          public void process(PoolEntry<T, C> entry) {
            if (entry.getUpdated() <= deadline)
              entry.close(); 
          }
        });
  }
  
  public void closeExpired() {
    final long now = System.currentTimeMillis();
    enumAvailable(new PoolEntryCallback<T, C>() {
          public void process(PoolEntry<T, C> entry) {
            if (entry.isExpired(now))
              entry.close(); 
          }
        });
  }
  
  public int getValidateAfterInactivity() {
    return this.validateAfterInactivity;
  }
  
  public void setValidateAfterInactivity(int ms) {
    this.validateAfterInactivity = ms;
  }
  
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append("[leased: ");
    buffer.append(this.leased);
    buffer.append("][available: ");
    buffer.append(this.available);
    buffer.append("][pending: ");
    buffer.append(this.pending);
    buffer.append("]");
    return buffer.toString();
  }
  
  protected abstract E createEntry(T paramT, C paramC);
}
