package org.slf4j.impl;

import org.apache.log4j.Level;
import org.slf4j.ILoggerFactory;
import org.slf4j.helpers.Util;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {
  private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
  
  public static final StaticLoggerBinder getSingleton() {
    return SINGLETON;
  }
  
  public static String REQUESTED_API_VERSION = "1.6.99";
  
  private static final String loggerFactoryClassStr = Log4jLoggerFactory.class.getName();
  
  private final ILoggerFactory loggerFactory;
  
  private StaticLoggerBinder() {
    this.loggerFactory = new Log4jLoggerFactory();
    try {
      Level level = Level.TRACE;
    } catch (NoSuchFieldError nsfe) {
      Util.report("This version of SLF4J requires log4j version 1.2.12 or later. See also http://www.slf4j.org/codes.html#log4j_version");
    } 
  }
  
  public ILoggerFactory getLoggerFactory() {
    return this.loggerFactory;
  }
  
  public String getLoggerFactoryClassStr() {
    return loggerFactoryClassStr;
  }
}
