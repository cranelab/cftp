package org.slf4j.impl;

import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class Log4jLoggerFactory implements ILoggerFactory {
  Map loggerMap = new HashMap<Object, Object>();
  
  public Logger getLogger(String name) {
    Logger slf4jLogger = null;
    synchronized (this) {
      slf4jLogger = (Logger)this.loggerMap.get(name);
      if (slf4jLogger == null) {
        Logger log4jLogger;
        if (name.equalsIgnoreCase("ROOT")) {
          log4jLogger = LogManager.getRootLogger();
        } else {
          log4jLogger = LogManager.getLogger(name);
        } 
        slf4jLogger = new Log4jLoggerAdapter(log4jLogger);
        this.loggerMap.put(name, slf4jLogger);
      } 
    } 
    return slf4jLogger;
  }
}
