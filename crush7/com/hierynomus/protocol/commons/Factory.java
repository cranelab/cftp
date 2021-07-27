package com.hierynomus.protocol.commons;

import java.util.LinkedList;
import java.util.List;

public interface Factory<T> {
  T create();
  
  public static interface Named<T> extends Factory<T> {
    String getName();
    
    public static class Util {
      public static <T> T create(List<Factory.Named<T>> factories, String name) {
        Factory.Named<T> factory = get(factories, name);
        if (factory != null)
          return factory.create(); 
        return null;
      }
      
      public static <T> Factory.Named<T> get(List<Factory.Named<T>> factories, String name) {
        if (factories != null)
          for (Factory.Named<T> f : factories) {
            if (f.getName().equals(name))
              return f; 
          }  
        return null;
      }
      
      public static <T> List<String> getNames(List<Factory.Named<T>> factories) {
        List<String> list = new LinkedList<String>();
        for (Factory.Named<T> f : factories)
          list.add(f.getName()); 
        return list;
      }
    }
  }
}
