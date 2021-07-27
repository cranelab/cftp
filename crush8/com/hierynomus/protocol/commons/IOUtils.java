package com.hierynomus.protocol.commons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOUtils {
  private static final Logger logger = LoggerFactory.getLogger(IOUtils.class);
  
  public static void closeQuietly(AutoCloseable... closeables) {
    for (AutoCloseable c : closeables) {
      try {
        if (c != null)
          c.close(); 
      } catch (Exception logged) {
        logger.warn("Error closing {} - {}", c, logged);
      } 
    } 
  }
  
  public static void closeSilently(AutoCloseable... closeables) {
    for (AutoCloseable c : closeables) {
      try {
        if (c != null)
          c.close(); 
      } catch (Exception exception) {}
    } 
  }
}
