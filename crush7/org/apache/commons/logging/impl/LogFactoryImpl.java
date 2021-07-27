package org.apache.commons.logging.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

public class LogFactoryImpl extends LogFactory {
  public static final String LOG_PROPERTY = "org.apache.commons.logging.Log";
  
  protected static final String LOG_PROPERTY_OLD = "org.apache.commons.logging.log";
  
  protected Hashtable attributes = new Hashtable();
  
  protected Hashtable instances = new Hashtable();
  
  private String logClassName;
  
  protected Constructor logConstructor = null;
  
  protected Class[] logConstructorSignature = new Class[] { String.class };
  
  protected Method logMethod = null;
  
  protected Class[] logMethodSignature = new Class[] { LogFactory.class };
  
  public Object getAttribute(String name) {
    return this.attributes.get(name);
  }
  
  public String[] getAttributeNames() {
    Vector names = new Vector();
    Enumeration keys = this.attributes.keys();
    while (keys.hasMoreElements())
      names.addElement(keys.nextElement()); 
    String[] results = new String[names.size()];
    for (int i = 0; i < results.length; i++)
      results[i] = names.elementAt(i); 
    return results;
  }
  
  public Log getInstance(Class clazz) throws LogConfigurationException {
    return getInstance(clazz.getName());
  }
  
  public Log getInstance(String name) throws LogConfigurationException {
    Log instance = (Log)this.instances.get(name);
    if (instance == null) {
      instance = newInstance(name);
      this.instances.put(name, instance);
    } 
    return instance;
  }
  
  public void release() {
    this.instances.clear();
  }
  
  public void removeAttribute(String name) {
    this.attributes.remove(name);
  }
  
  public void setAttribute(String name, Object value) {
    if (value == null) {
      this.attributes.remove(name);
    } else {
      this.attributes.put(name, value);
    } 
  }
  
  protected String getLogClassName() {
    if (this.logClassName != null)
      return this.logClassName; 
    this.logClassName = (String)getAttribute("org.apache.commons.logging.Log");
    if (this.logClassName == null)
      this.logClassName = (String)getAttribute("org.apache.commons.logging.log"); 
    if (this.logClassName == null)
      try {
        this.logClassName = System.getProperty("org.apache.commons.logging.Log");
      } catch (SecurityException e) {} 
    if (this.logClassName == null)
      try {
        this.logClassName = System.getProperty("org.apache.commons.logging.log");
      } catch (SecurityException e) {} 
    if (this.logClassName == null && isLog4JAvailable())
      this.logClassName = "org.apache.commons.logging.impl.Log4JLogger"; 
    if (this.logClassName == null && isJdk14Available())
      this.logClassName = "org.apache.commons.logging.impl.Jdk14Logger"; 
    if (this.logClassName == null)
      this.logClassName = "org.apache.commons.logging.impl.SimpleLog"; 
    return this.logClassName;
  }
  
  protected Constructor getLogConstructor() throws LogConfigurationException {
    if (this.logConstructor != null)
      return this.logConstructor; 
    String logClassName = getLogClassName();
    Class logClass = null;
    try {
      logClass = loadClass(logClassName);
      if (logClass == null)
        throw new LogConfigurationException("No suitable Log implementation for " + logClassName); 
      if (!Log.class.isAssignableFrom(logClass))
        throw new LogConfigurationException("Class " + logClassName + " does not implement Log"); 
    } catch (Throwable t) {
      throw new LogConfigurationException(t);
    } 
    try {
      this.logMethod = logClass.getMethod("setLogFactory", this.logMethodSignature);
    } catch (Throwable t) {
      this.logMethod = null;
    } 
    try {
      this.logConstructor = logClass.getConstructor(this.logConstructorSignature);
      return this.logConstructor;
    } catch (Throwable t) {
      throw new LogConfigurationException("No suitable Log constructor " + this.logConstructorSignature + " for " + logClassName, t);
    } 
  }
  
  private static Class loadClass(String name) throws ClassNotFoundException {
    Object result = AccessController.doPrivileged(new PrivilegedAction(name) {
          private final String val$name;
          
          public Object run() {
            ClassLoader threadCL = LogFactory.getContextClassLoader();
            if (threadCL != null)
              try {
                return threadCL.loadClass(this.val$name);
              } catch (ClassNotFoundException ex) {} 
            try {
              return Class.forName(this.val$name);
            } catch (ClassNotFoundException e) {
              return null;
            } 
          }
        });
    if (result instanceof Class)
      return (Class)result; 
    throw (ClassNotFoundException)result;
  }
  
  protected boolean isJdk14Available() {
    try {
      loadClass("java.util.logging.Logger");
      loadClass("org.apache.commons.logging.impl.Jdk14Logger");
      return true;
    } catch (Throwable t) {
      return false;
    } 
  }
  
  protected boolean isLog4JAvailable() {
    try {
      loadClass("org.apache.log4j.Logger");
      loadClass("org.apache.commons.logging.impl.Log4JLogger");
      return true;
    } catch (Throwable t) {
      return false;
    } 
  }
  
  protected Log newInstance(String name) throws LogConfigurationException {
    Log instance = null;
    try {
      Object[] params = new Object[1];
      params[0] = name;
      instance = getLogConstructor().newInstance(params);
      if (this.logMethod != null) {
        params[0] = this;
        this.logMethod.invoke(instance, params);
      } 
      return instance;
    } catch (Throwable t) {
      throw new LogConfigurationException(t);
    } 
  }
}
