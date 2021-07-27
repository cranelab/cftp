package net.engio.mbassy.common;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractConcurrentSet<T> implements Set<T> {
  private static final AtomicLong id = new AtomicLong();
  
  private final long ID = id.getAndIncrement();
  
  protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  
  private final Map<T, ISetEntry<T>> entries;
  
  protected Entry<T> head;
  
  protected AbstractConcurrentSet(Map<T, ISetEntry<T>> entries) {
    this.entries = entries;
  }
  
  protected abstract Entry<T> createEntry(T paramT, Entry<T> paramEntry);
  
  public boolean add(T element) {
    boolean changed;
    if (element == null)
      return false; 
    Lock writeLock = this.lock.writeLock();
    try {
      writeLock.lock();
      changed = insert(element);
    } finally {
      writeLock.unlock();
    } 
    return changed;
  }
  
  public boolean contains(Object element) {
    ISetEntry<T> entry;
    Lock readLock = this.lock.readLock();
    try {
      readLock.lock();
      entry = this.entries.get(element);
    } finally {
      readLock.unlock();
    } 
    return (entry != null && entry.getValue() != null);
  }
  
  private boolean insert(T element) {
    if (!this.entries.containsKey(element)) {
      this.head = createEntry(element, this.head);
      this.entries.put(element, this.head);
      return true;
    } 
    return false;
  }
  
  public int size() {
    return this.entries.size();
  }
  
  public boolean isEmpty() {
    return (this.head == null);
  }
  
  public boolean addAll(Collection<? extends T> elements) {
    boolean changed = false;
    Lock writeLock = this.lock.writeLock();
    try {
      writeLock.lock();
      for (T element : elements) {
        if (element != null)
          changed |= insert(element); 
      } 
    } finally {
      writeLock.unlock();
    } 
    return changed;
  }
  
  public boolean remove(Object element) {
    if (!contains(element))
      return false; 
    Lock writeLock = this.lock.writeLock();
    try {
      writeLock.lock();
      ISetEntry<T> listelement = this.entries.get(element);
      if (listelement == null)
        return false; 
      if (listelement != this.head) {
        listelement.remove();
      } else {
        this.head = this.head.next();
      } 
      this.entries.remove(element);
    } finally {
      writeLock.unlock();
    } 
    return true;
  }
  
  public Object[] toArray() {
    return this.entries.entrySet().toArray();
  }
  
  public <T> T[] toArray(T[] a) {
    return (T[])this.entries.entrySet().toArray((Object[])a);
  }
  
  public boolean containsAll(Collection<?> c) {
    throw new UnsupportedOperationException("Not implemented");
  }
  
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("Not implemented");
  }
  
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException("Not implemented");
  }
  
  public void clear() {
    Lock writeLock = this.lock.writeLock();
    try {
      writeLock.lock();
      this.head = null;
      this.entries.clear();
    } finally {
      writeLock.unlock();
    } 
  }
  
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = 31 * result + (int)(this.ID ^ this.ID >>> 32L);
    return result;
  }
  
  public boolean equals(Object obj) {
    if (this == obj)
      return true; 
    if (obj == null)
      return false; 
    if (getClass() != obj.getClass())
      return false; 
    AbstractConcurrentSet other = (AbstractConcurrentSet)obj;
    if (this.ID != other.ID)
      return false; 
    return true;
  }
  
  public static abstract class Entry<T> implements ISetEntry<T> {
    private Entry<T> next;
    
    private Entry<T> predecessor;
    
    protected Entry(Entry<T> next) {
      this.next = next;
      next.predecessor = this;
    }
    
    protected Entry() {}
    
    public void remove() {
      if (this.predecessor != null) {
        this.predecessor.next = this.next;
        if (this.next != null)
          this.next.predecessor = this.predecessor; 
      } else if (this.next != null) {
        this.next.predecessor = null;
      } 
    }
    
    public Entry<T> next() {
      return this.next;
    }
    
    public void clear() {
      this.next = null;
    }
  }
}
