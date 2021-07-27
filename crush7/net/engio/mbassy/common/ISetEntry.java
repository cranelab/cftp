package net.engio.mbassy.common;

public interface ISetEntry<T> {
  T getValue();
  
  void remove();
  
  ISetEntry<T> next();
  
  void clear();
}
