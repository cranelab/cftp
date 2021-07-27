package org.apache.http.config;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.http.util.Args;

public final class RegistryBuilder<I> {
  private final Map<String, I> items;
  
  public static <I> RegistryBuilder<I> create() {
    return new RegistryBuilder<I>();
  }
  
  RegistryBuilder() {
    this.items = new HashMap<String, I>();
  }
  
  public RegistryBuilder<I> register(String id, I item) {
    Args.notEmpty(id, "ID");
    Args.notNull(item, "Item");
    this.items.put(id.toLowerCase(Locale.ROOT), item);
    return this;
  }
  
  public Registry<I> build() {
    return new Registry<I>(this.items);
  }
  
  public String toString() {
    return this.items.toString();
  }
}
