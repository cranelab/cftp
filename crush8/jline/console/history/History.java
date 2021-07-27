package jline.console.history;

import java.util.Iterator;
import java.util.ListIterator;

public interface History extends Iterable<History.Entry> {
  int size();
  
  boolean isEmpty();
  
  int index();
  
  void clear();
  
  CharSequence get(int paramInt);
  
  void add(CharSequence paramCharSequence);
  
  void set(int paramInt, CharSequence paramCharSequence);
  
  CharSequence remove(int paramInt);
  
  CharSequence removeFirst();
  
  CharSequence removeLast();
  
  void replace(CharSequence paramCharSequence);
  
  ListIterator<Entry> entries(int paramInt);
  
  ListIterator<Entry> entries();
  
  Iterator<Entry> iterator();
  
  CharSequence current();
  
  boolean previous();
  
  boolean next();
  
  boolean moveToFirst();
  
  boolean moveToLast();
  
  boolean moveTo(int paramInt);
  
  void moveToEnd();
  
  public static interface Entry {
    int index();
    
    CharSequence value();
  }
}
