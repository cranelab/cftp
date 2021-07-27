package org.apache.http.config;

public interface Lookup<I> {
  I lookup(String paramString);
}
