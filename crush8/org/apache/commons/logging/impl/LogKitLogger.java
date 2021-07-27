package org.apache.commons.logging.impl;

import org.apache.commons.logging.Log;
import org.apache.log.Hierarchy;
import org.apache.log.Logger;

public final class LogKitLogger implements Log {
  protected Logger logger = null;
  
  public LogKitLogger(String name) {
    this.logger = Hierarchy.getDefaultHierarchy().getLoggerFor(name);
  }
  
  public void trace(Object message) {
    debug(message);
  }
  
  public void trace(Object message, Throwable t) {
    debug(message, t);
  }
  
  public void debug(Object message) {
    if (message != null)
      this.logger.debug(String.valueOf(message)); 
  }
  
  public void debug(Object message, Throwable t) {
    if (message != null)
      this.logger.debug(String.valueOf(message), t); 
  }
  
  public void info(Object message) {
    if (message != null)
      this.logger.info(String.valueOf(message)); 
  }
  
  public void info(Object message, Throwable t) {
    if (message != null)
      this.logger.info(String.valueOf(message), t); 
  }
  
  public void warn(Object message) {
    if (message != null)
      this.logger.warn(String.valueOf(message)); 
  }
  
  public void warn(Object message, Throwable t) {
    if (message != null)
      this.logger.warn(String.valueOf(message), t); 
  }
  
  public void error(Object message) {
    if (message != null)
      this.logger.error(String.valueOf(message)); 
  }
  
  public void error(Object message, Throwable t) {
    if (message != null)
      this.logger.error(String.valueOf(message), t); 
  }
  
  public void fatal(Object message) {
    if (message != null)
      this.logger.fatalError(String.valueOf(message)); 
  }
  
  public void fatal(Object message, Throwable t) {
    if (message != null)
      this.logger.fatalError(String.valueOf(message), t); 
  }
  
  public boolean isDebugEnabled() {
    return this.logger.isDebugEnabled();
  }
  
  public boolean isErrorEnabled() {
    return this.logger.isErrorEnabled();
  }
  
  public boolean isFatalEnabled() {
    return this.logger.isFatalErrorEnabled();
  }
  
  public boolean isInfoEnabled() {
    return this.logger.isInfoEnabled();
  }
  
  public boolean isTraceEnabled() {
    return this.logger.isDebugEnabled();
  }
  
  public boolean isWarnEnabled() {
    return this.logger.isWarnEnabled();
  }
}
