package com.hierynomus.protocol.commons;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public interface EnumWithValue<E extends Enum<E>> {
  long getValue();
  
  public static class EnumUtils {
    public static <E extends Enum<E>> long toLong(Collection<E> set) {
      long l = 0L;
      for (Enum enum_ : set) {
        if (enum_ instanceof EnumWithValue) {
          l |= ((EnumWithValue)enum_).getValue();
          continue;
        } 
        throw new IllegalArgumentException("Can only be used with EnumWithValue enums.");
      } 
      return l;
    }
    
    public static <E extends Enum<E>> EnumSet<E> toEnumSet(long l, Class<E> clazz) {
      if (!EnumWithValue.class.isAssignableFrom(clazz))
        throw new IllegalArgumentException("Can only be used with EnumWithValue enums."); 
      EnumSet<E> es = EnumSet.noneOf(clazz);
      byte b;
      int i;
      Enum[] arrayOfEnum;
      for (i = (arrayOfEnum = (Enum[])clazz.getEnumConstants()).length, b = 0; b < i; ) {
        Enum enum_ = arrayOfEnum[b];
        if (isSet(l, (EnumWithValue)enum_))
          es.add((E)enum_); 
        b++;
      } 
      return es;
    }
    
    public static <E extends EnumWithValue<?>> boolean isSet(long bytes, E value) {
      return ((bytes & value.getValue()) > 0L);
    }
    
    public static <E extends EnumWithValue<?>> E valueOf(long l, Class<E> enumClass, E defaultValue) {
      EnumWithValue[] enumConstants = (EnumWithValue[])enumClass.getEnumConstants();
      byte b;
      int i;
      EnumWithValue[] arrayOfEnumWithValue1;
      for (i = (arrayOfEnumWithValue1 = enumConstants).length, b = 0; b < i; ) {
        EnumWithValue enumWithValue = arrayOfEnumWithValue1[b];
        if (enumWithValue.getValue() == l)
          return (E)enumWithValue; 
        b++;
      } 
      return defaultValue;
    }
    
    public static <E extends Enum<E>> Set<E> ensureNotNull(Set<E> set, Class<E> clazz) {
      if (set == null)
        return EnumSet.noneOf(clazz); 
      return set;
    }
    
    public static <E extends Enum<E>> E ensureNotNull(E value, E defaultValue) {
      return (value != null) ? value : defaultValue;
    }
  }
}
