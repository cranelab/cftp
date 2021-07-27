package net.engio.mbassy.common;

import java.util.HashMap;
import java.util.Iterator;

public class StrongConcurrentSet<T> extends AbstractConcurrentSet<T> {
  public StrongConcurrentSet() {
    super(new HashMap<T, ISetEntry<T>>());
  }
  
  public Iterator<T> iterator() {
    return new Iterator<T>() {
        private ISetEntry<T> current = StrongConcurrentSet.this.head;
        
        public boolean hasNext() {
          return (this.current != null);
        }
        
        public T next() {
          if (this.current == null)
            return null; 
          T value = this.current.getValue();
          this.current = this.current.next();
          return value;
        }
        
        public void remove() {
          if (this.current == null)
            return; 
          ISetEntry<T> newCurrent = this.current.next();
          StrongConcurrentSet.this.remove(this.current.getValue());
          this.current = newCurrent;
        }
      };
  }
  
  protected AbstractConcurrentSet.Entry<T> createEntry(T value, AbstractConcurrentSet.Entry<T> next) {
    return (next != null) ? new StrongEntry<T>(value, next) : new StrongEntry<T>(value);
  }
  
  public static class StrongEntry<T> extends AbstractConcurrentSet.Entry<T> {
    private T value;
    
    private StrongEntry(T value, AbstractConcurrentSet.Entry<T> next) {
      super(next);
      this.value = value;
    }
    
    private StrongEntry(T value) {
      this.value = value;
    }
    
    public T getValue() {
      return this.value;
    }
  }
}
