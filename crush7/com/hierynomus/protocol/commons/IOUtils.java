package com.hierynomus.protocol.commons;

import com.hierynomus.smbj.connection.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOUtils {
  private static final Logger LOG = LoggerFactory.getLogger(IOUtils.class);
  
  public static void closeQuietly(Connection c) {
    try {
      if (c != null)
        c.close(); 
    } catch (Exception logged) {
      LOG.warn("Error closing {} - {}", c, logged);
    } 
  }
  
  public static void closeSilently(Connection c) {
    try {
      if (c != null)
        c.close(); 
    } catch (Exception exception) {}
  }
}
