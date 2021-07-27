package org.apache.commons.logging.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;

public final class Jdk14Logger implements Log {
  protected Logger logger;
  
  public Jdk14Logger(String name) {
    this.logger = null;
    this.logger = Logger.getLogger(name);
  }
  
  private void log(Level level, String msg, Throwable ex) {
    if (this.logger.isLoggable(level)) {
      Throwable dummyException = new Throwable();
      StackTraceElement[] locations = dummyException.getStackTrace();
      String cname = "unknown";
      String method = "unknown";
      if (locations != null && locations.length > 2) {
        StackTraceElement caller = locations[2];
        cname = caller.getClassName();
        method = caller.getMethodName();
      } 
      if (ex == null) {
        this.logger.logp(level, cname, method, msg);
      } else {
        this.logger.logp(level, cname, method, msg, ex);
      } 
    } 
  }
  
  public void debug(Object message) {
    log(Level.FINE, String.valueOf(message), null);
  }
  
  public void debug(Object message, Throwable exception) {
    log(Level.FINE, String.valueOf(message), exception);
  }
  
  public void error(Object message) {
    log(Level.SEVERE, String.valueOf(message), null);
  }
  
  public void error(Object message, Throwable exception) {
    log(Level.SEVERE, String.valueOf(message), exception);
  }
  
  public void fatal(Object message) {
    log(Level.SEVERE, String.valueOf(message), null);
  }
  
  public void fatal(Object message, Throwable exception) {
    log(Level.SEVERE, String.valueOf(message), exception);
  }
  
  public Logger getLogger() {
    return this.logger;
  }
  
  public void info(Object message) {
    log(Level.INFO, String.valueOf(message), null);
  }
  
  public void info(Object message, Throwable exception) {
    log(Level.INFO, String.valueOf(message), exception);
  }
  
  public boolean isDebugEnabled() {
    return this.logger.isLoggable(Level.FINE);
  }
  
  public boolean isErrorEnabled() {
    return this.logger.isLoggable(Level.SEVERE);
  }
  
  public boolean isFatalEnabled() {
    return this.logger.isLoggable(Level.SEVERE);
  }
  
  public boolean isInfoEnabled() {
    return this.logger.isLoggable(Level.INFO);
  }
  
  public boolean isTraceEnabled() {
    return this.logger.isLoggable(Level.FINEST);
  }
  
  public boolean isWarnEnabled() {
    return this.logger.isLoggable(Level.WARNING);
  }
  
  public void trace(Object message) {
    log(Level.FINEST, String.valueOf(message), null);
  }
  
  public void trace(Object message, Throwable exception) {
    log(Level.FINEST, String.valueOf(message), exception);
  }
  
  public void warn(Object message) {
    log(Level.WARNING, String.valueOf(message), null);
  }
  
  public void warn(Object message, Throwable exception) {
    log(Level.WARNING, String.valueOf(message), exception);
  }
}
