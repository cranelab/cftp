package org.apache.commons.logging.impl;

import org.apache.commons.logging.Log;
import org.apache.log4j.Category;
import org.apache.log4j.Priority;

public final class Log4JCategoryLog implements Log {
  private static final String FQCN = Log4JCategoryLog.class.getName();
  
  private Category category = null;
  
  public Log4JCategoryLog(String name) {
    this.category = Category.getInstance(name);
  }
  
  public Log4JCategoryLog(Category category) {
    this.category = category;
  }
  
  public void trace(Object message) {
    this.category.log(FQCN, Priority.DEBUG, message, null);
  }
  
  public void trace(Object message, Throwable t) {
    this.category.log(FQCN, Priority.DEBUG, message, t);
  }
  
  public void debug(Object message) {
    this.category.log(FQCN, Priority.DEBUG, message, null);
  }
  
  public void debug(Object message, Throwable t) {
    this.category.log(FQCN, Priority.DEBUG, message, t);
  }
  
  public void info(Object message) {
    this.category.log(FQCN, Priority.INFO, message, null);
  }
  
  public void info(Object message, Throwable t) {
    this.category.log(FQCN, Priority.INFO, message, t);
  }
  
  public void warn(Object message) {
    this.category.log(FQCN, Priority.WARN, message, null);
  }
  
  public void warn(Object message, Throwable t) {
    this.category.log(FQCN, Priority.WARN, message, t);
  }
  
  public void error(Object message) {
    this.category.log(FQCN, Priority.ERROR, message, null);
  }
  
  public void error(Object message, Throwable t) {
    this.category.log(FQCN, Priority.ERROR, message, t);
  }
  
  public void fatal(Object message) {
    this.category.log(FQCN, Priority.FATAL, message, null);
  }
  
  public void fatal(Object message, Throwable t) {
    this.category.log(FQCN, Priority.FATAL, message, t);
  }
  
  public Category getCategory() {
    return this.category;
  }
  
  public boolean isDebugEnabled() {
    return this.category.isDebugEnabled();
  }
  
  public boolean isErrorEnabled() {
    return this.category.isEnabledFor(Priority.ERROR);
  }
  
  public boolean isFatalEnabled() {
    return this.category.isEnabledFor(Priority.FATAL);
  }
  
  public boolean isInfoEnabled() {
    return this.category.isInfoEnabled();
  }
  
  public boolean isTraceEnabled() {
    return this.category.isDebugEnabled();
  }
  
  public boolean isWarnEnabled() {
    return this.category.isEnabledFor(Priority.WARN);
  }
  
  public Log4JCategoryLog() {}
}
