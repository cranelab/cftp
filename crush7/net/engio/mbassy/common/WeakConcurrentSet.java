package net.engio.mbassy.common;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;

public class WeakConcurrentSet<T> extends AbstractConcurrentSet<T> {
  public WeakConcurrentSet() {
    super(new WeakHashMap<T, ISetEntry<T>>());
  }
  
  public Iterator<T> iterator() {
    return new Iterator<T>() {
        private ISetEntry<T> current = WeakConcurrentSet.this.head;
        
        private void removeOrphans() {
          Lock writelock = WeakConcurrentSet.this.lock.writeLock();
          try {
            writelock.lock();
            do {
              ISetEntry<T> orphaned = this.current;
              this.current = this.current.next();
              if (orphaned == WeakConcurrentSet.this.head)
                WeakConcurrentSet.this.head = WeakConcurrentSet.this.head.next(); 
              orphaned.remove();
            } while (this.current != null && this.current.getValue() == null);
          } finally {
            writelock.unlock();
          } 
        }
        
        public boolean hasNext() {
          if (this.current == null)
            return false; 
          if (this.current.getValue() == null) {
            removeOrphans();
            return (this.current != null);
          } 
          return true;
        }
        
        public T next() {
          if (this.current == null)
            return null; 
          T value = this.current.getValue();
          if (value == null) {
            removeOrphans();
            return next();
          } 
          this.current = this.current.next();
          return value;
        }
        
        public void remove() {
          if (this.current == null)
            return; 
          ISetEntry<T> newCurrent = this.current.next();
          WeakConcurrentSet.this.remove(this.current.getValue());
          this.current = newCurrent;
        }
      };
  }
  
  protected AbstractConcurrentSet.Entry<T> createEntry(T value, AbstractConcurrentSet.Entry<T> next) {
    return (next != null) ? new WeakEntry<T>(value, next) : new WeakEntry<T>(value);
  }
  
  public static class WeakEntry<T> extends AbstractConcurrentSet.Entry<T> {
    private WeakReference<T> value;
    
    private WeakEntry(T value, AbstractConcurrentSet.Entry<T> next) {
      super(next);
      this.value = new WeakReference<T>(value);
    }
    
    private WeakEntry(T value) {
      this.value = new WeakReference<T>(value);
    }
    
    public T getValue() {
      return this.value.get();
    }
  }
}
