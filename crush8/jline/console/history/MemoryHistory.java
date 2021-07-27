package jline.console.history;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import jline.internal.Preconditions;

public class MemoryHistory implements History {
  public static final int DEFAULT_MAX_SIZE = 500;
  
  private final LinkedList<CharSequence> items = new LinkedList<CharSequence>();
  
  private int maxSize = 500;
  
  private boolean ignoreDuplicates = true;
  
  private boolean autoTrim = false;
  
  private int offset = 0;
  
  private int index = 0;
  
  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
    maybeResize();
  }
  
  public int getMaxSize() {
    return this.maxSize;
  }
  
  public boolean isIgnoreDuplicates() {
    return this.ignoreDuplicates;
  }
  
  public void setIgnoreDuplicates(boolean flag) {
    this.ignoreDuplicates = flag;
  }
  
  public boolean isAutoTrim() {
    return this.autoTrim;
  }
  
  public void setAutoTrim(boolean flag) {
    this.autoTrim = flag;
  }
  
  public int size() {
    return this.items.size();
  }
  
  public boolean isEmpty() {
    return this.items.isEmpty();
  }
  
  public int index() {
    return this.offset + this.index;
  }
  
  public void clear() {
    this.items.clear();
    this.offset = 0;
    this.index = 0;
  }
  
  public CharSequence get(int index) {
    return this.items.get(index - this.offset);
  }
  
  public void set(int index, CharSequence item) {
    this.items.set(index - this.offset, item);
  }
  
  public void add(CharSequence item) {
    Preconditions.checkNotNull(item);
    if (isAutoTrim())
      item = String.valueOf(item).trim(); 
    if (isIgnoreDuplicates() && 
      !this.items.isEmpty() && item.equals(this.items.getLast()))
      return; 
    internalAdd(item);
  }
  
  public CharSequence remove(int i) {
    return this.items.remove(i);
  }
  
  public CharSequence removeFirst() {
    return this.items.removeFirst();
  }
  
  public CharSequence removeLast() {
    return this.items.removeLast();
  }
  
  protected void internalAdd(CharSequence item) {
    this.items.add(item);
    maybeResize();
  }
  
  public void replace(CharSequence item) {
    this.items.removeLast();
    add(item);
  }
  
  private void maybeResize() {
    while (size() > getMaxSize()) {
      this.items.removeFirst();
      this.offset++;
    } 
    this.index = size();
  }
  
  public ListIterator<History.Entry> entries(int index) {
    return new EntriesIterator(index - this.offset);
  }
  
  public ListIterator<History.Entry> entries() {
    return entries(this.offset);
  }
  
  public Iterator<History.Entry> iterator() {
    return entries();
  }
  
  private static class EntryImpl implements History.Entry {
    private final int index;
    
    private final CharSequence value;
    
    public EntryImpl(int index, CharSequence value) {
      this.index = index;
      this.value = value;
    }
    
    public int index() {
      return this.index;
    }
    
    public CharSequence value() {
      return this.value;
    }
    
    public String toString() {
      return String.format("%d: %s", new Object[] { Integer.valueOf(this.index), this.value });
    }
  }
  
  private class EntriesIterator implements ListIterator<History.Entry> {
    private final ListIterator<CharSequence> source;
    
    private EntriesIterator(int index) {
      this.source = MemoryHistory.this.items.listIterator(index);
    }
    
    public History.Entry next() {
      if (!this.source.hasNext())
        throw new NoSuchElementException(); 
      return new MemoryHistory.EntryImpl(MemoryHistory.this.offset + this.source.nextIndex(), this.source.next());
    }
    
    public History.Entry previous() {
      if (!this.source.hasPrevious())
        throw new NoSuchElementException(); 
      return new MemoryHistory.EntryImpl(MemoryHistory.this.offset + this.source.previousIndex(), this.source.previous());
    }
    
    public int nextIndex() {
      return MemoryHistory.this.offset + this.source.nextIndex();
    }
    
    public int previousIndex() {
      return MemoryHistory.this.offset + this.source.previousIndex();
    }
    
    public boolean hasNext() {
      return this.source.hasNext();
    }
    
    public boolean hasPrevious() {
      return this.source.hasPrevious();
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }
    
    public void set(History.Entry entry) {
      throw new UnsupportedOperationException();
    }
    
    public void add(History.Entry entry) {
      throw new UnsupportedOperationException();
    }
  }
  
  public boolean moveToLast() {
    int lastEntry = size() - 1;
    if (lastEntry >= 0 && lastEntry != this.index) {
      this.index = size() - 1;
      return true;
    } 
    return false;
  }
  
  public boolean moveTo(int index) {
    index -= this.offset;
    if (index >= 0 && index < size()) {
      this.index = index;
      return true;
    } 
    return false;
  }
  
  public boolean moveToFirst() {
    if (size() > 0 && this.index != 0) {
      this.index = 0;
      return true;
    } 
    return false;
  }
  
  public void moveToEnd() {
    this.index = size();
  }
  
  public CharSequence current() {
    if (this.index >= size())
      return ""; 
    return this.items.get(this.index);
  }
  
  public boolean previous() {
    if (this.index <= 0)
      return false; 
    this.index--;
    return true;
  }
  
  public boolean next() {
    if (this.index >= size())
      return false; 
    this.index++;
    return true;
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (History.Entry e : this)
      sb.append(e.toString() + "\n"); 
    return sb.toString();
  }
}
