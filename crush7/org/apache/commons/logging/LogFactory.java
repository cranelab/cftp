package org.apache.commons.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

public abstract class LogFactory {
  public static final String FACTORY_PROPERTY = "org.apache.commons.logging.LogFactory";
  
  public static final String FACTORY_DEFAULT = "org.apache.commons.logging.impl.LogFactoryImpl";
  
  public static final String FACTORY_PROPERTIES = "commons-logging.properties";
  
  protected static final String SERVICE_ID = "META-INF/services/org.apache.commons.logging.LogFactory";
  
  protected static Hashtable factories = new Hashtable();
  
  public static LogFactory getFactory() throws LogConfigurationException {
    ClassLoader contextClassLoader = AccessController.<ClassLoader>doPrivileged(new PrivilegedAction() {
          public Object run() {
            return LogFactory.getContextClassLoader();
          }
        });
    LogFactory factory = getCachedFactory(contextClassLoader);
    if (factory != null)
      return factory; 
    Properties props = null;
    try {
      InputStream stream = getResourceAsStream(contextClassLoader, "commons-logging.properties");
      if (stream != null) {
        props = new Properties();
        props.load(stream);
        stream.close();
      } 
    } catch (IOException e) {
    
    } catch (SecurityException e) {}
    try {
      String factoryClass = System.getProperty("org.apache.commons.logging.LogFactory");
      if (factoryClass != null)
        factory = newFactory(factoryClass, contextClassLoader); 
    } catch (SecurityException e) {}
    if (factory == null)
      try {
        InputStream is = getResourceAsStream(contextClassLoader, "META-INF/services/org.apache.commons.logging.LogFactory");
        if (is != null) {
          BufferedReader bufferedReader;
          try {
            bufferedReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
          } catch (UnsupportedEncodingException e) {
            bufferedReader = new BufferedReader(new InputStreamReader(is));
          } 
          String factoryClassName = bufferedReader.readLine();
          bufferedReader.close();
          if (factoryClassName != null && !"".equals(factoryClassName))
            factory = newFactory(factoryClassName, contextClassLoader); 
        } 
      } catch (Exception ex) {} 
    if (factory == null && props != null) {
      String factoryClass = props.getProperty("org.apache.commons.logging.LogFactory");
      if (factoryClass != null)
        factory = newFactory(factoryClass, contextClassLoader); 
    } 
    if (factory == null)
      factory = newFactory("org.apache.commons.logging.impl.LogFactoryImpl", LogFactory.class.getClassLoader()); 
    if (factory != null) {
      cacheFactory(contextClassLoader, factory);
      if (props != null) {
        Enumeration names = props.propertyNames();
        while (names.hasMoreElements()) {
          String name = (String)names.nextElement();
          String value = props.getProperty(name);
          factory.setAttribute(name, value);
        } 
      } 
    } 
    return factory;
  }
  
  public static Log getLog(Class clazz) throws LogConfigurationException {
    return getFactory().getInstance(clazz);
  }
  
  public static Log getLog(String name) throws LogConfigurationException {
    return getFactory().getInstance(name);
  }
  
  public static void release(ClassLoader classLoader) {
    synchronized (factories) {
      LogFactory factory = (LogFactory)factories.get(classLoader);
      if (factory != null) {
        factory.release();
        factories.remove(classLoader);
      } 
    } 
  }
  
  public static void releaseAll() {
    synchronized (factories) {
      Enumeration elements = factories.elements();
      while (elements.hasMoreElements()) {
        LogFactory element = elements.nextElement();
        element.release();
      } 
      factories.clear();
    } 
  }
  
  protected static ClassLoader getContextClassLoader() throws LogConfigurationException {
    ClassLoader classLoader = null;
    try {
      Method method = Thread.class.getMethod("getContextClassLoader", null);
      try {
        classLoader = (ClassLoader)method.invoke(Thread.currentThread(), null);
      } catch (IllegalAccessException e) {
        throw new LogConfigurationException("Unexpected IllegalAccessException", e);
      } catch (InvocationTargetException e) {
        if (!(e.getTargetException() instanceof SecurityException))
          throw new LogConfigurationException("Unexpected InvocationTargetException", e.getTargetException()); 
      } 
    } catch (NoSuchMethodException e) {
      classLoader = LogFactory.class.getClassLoader();
    } 
    return classLoader;
  }
  
  private static LogFactory getCachedFactory(ClassLoader contextClassLoader) {
    LogFactory factory = null;
    if (contextClassLoader != null)
      factory = (LogFactory)factories.get(contextClassLoader); 
    return factory;
  }
  
  private static void cacheFactory(ClassLoader classLoader, LogFactory factory) {
    if (classLoader != null && factory != null)
      factories.put(classLoader, factory); 
  }
  
  protected static LogFactory newFactory(String factoryClass, ClassLoader classLoader) throws LogConfigurationException {
    Object result = AccessController.doPrivileged(new PrivilegedAction(classLoader, factoryClass) {
          private final ClassLoader val$classLoader;
          
          private final String val$factoryClass;
          
          public Object run() {
            try {
              if (this.val$classLoader != null)
                try {
                  return this.val$classLoader.loadClass(this.val$factoryClass).newInstance();
                } catch (ClassNotFoundException ex) {
                  if (this.val$classLoader == ((LogFactory.class$org$apache$commons$logging$LogFactory == null) ? (LogFactory.class$org$apache$commons$logging$LogFactory = LogFactory.class$("org.apache.commons.logging.LogFactory")) : LogFactory.class$org$apache$commons$logging$LogFactory).getClassLoader())
                    throw ex; 
                } catch (NoClassDefFoundError e) {
                  if (this.val$classLoader == ((LogFactory.class$org$apache$commons$logging$LogFactory == null) ? (LogFactory.class$org$apache$commons$logging$LogFactory = LogFactory.class$("org.apache.commons.logging.LogFactory")) : LogFactory.class$org$apache$commons$logging$LogFactory).getClassLoader())
                    throw e; 
                } catch (ClassCastException e) {
                  if (this.val$classLoader == ((LogFactory.class$org$apache$commons$logging$LogFactory == null) ? (LogFactory.class$org$apache$commons$logging$LogFactory = LogFactory.class$("org.apache.commons.logging.LogFactory")) : LogFactory.class$org$apache$commons$logging$LogFactory).getClassLoader())
                    throw e; 
                }  
              return Class.forName(this.val$factoryClass).newInstance();
            } catch (Exception e) {
              return new LogConfigurationException(e);
            } 
          }
        });
    if (result instanceof LogConfigurationException)
      throw (LogConfigurationException)result; 
    return (LogFactory)result;
  }
  
  private static InputStream getResourceAsStream(ClassLoader loader, String name) {
    return AccessController.<InputStream>doPrivileged(new PrivilegedAction(loader, name) {
          private final ClassLoader val$loader;
          
          private final String val$name;
          
          public Object run() {
            if (this.val$loader != null)
              return this.val$loader.getResourceAsStream(this.val$name); 
            return ClassLoader.getSystemResourceAsStream(this.val$name);
          }
        });
  }
  
  public abstract Object getAttribute(String paramString);
  
  public abstract String[] getAttributeNames();
  
  public abstract Log getInstance(Class paramClass) throws LogConfigurationException;
  
  public abstract Log getInstance(String paramString) throws LogConfigurationException;
  
  public abstract void release();
  
  public abstract void removeAttribute(String paramString);
  
  public abstract void setAttribute(String paramString, Object paramObject);
}
