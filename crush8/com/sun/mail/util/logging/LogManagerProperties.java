package com.sun.mail.util.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;

final class LogManagerProperties extends Properties {
  private static final long serialVersionUID = -2239983349056806252L;
  
  private static final Method LR_GET_INSTANT;
  
  private static final Method ZI_SYSTEM_DEFAULT;
  
  private static final Method ZDT_OF_INSTANT;
  
  private static volatile String[] REFLECT_NAMES;
  
  static {
    Method lrgi = null;
    Method zisd = null;
    Method zdtoi = null;
    try {
      lrgi = LogRecord.class.getMethod("getInstant", new Class[0]);
      assert Comparable.class
        .isAssignableFrom(lrgi.getReturnType()) : lrgi;
      zisd = findClass("java.time.ZoneId").getMethod("systemDefault", new Class[0]);
      if (!Modifier.isStatic(zisd.getModifiers()))
        throw new NoSuchMethodException(zisd.toString()); 
      zdtoi = findClass("java.time.ZonedDateTime").getMethod("ofInstant", new Class[] { findClass("java.time.Instant"), 
            findClass("java.time.ZoneId") });
      if (!Modifier.isStatic(zdtoi.getModifiers()))
        throw new NoSuchMethodException(zdtoi.toString()); 
      if (!Comparable.class.isAssignableFrom(zdtoi.getReturnType()))
        throw new NoSuchMethodException(zdtoi.toString()); 
    } catch (RuntimeException runtimeException) {
    
    } catch (Exception exception) {
    
    } catch (LinkageError linkageError) {
    
    } finally {
      if (lrgi == null || zisd == null || zdtoi == null) {
        lrgi = null;
        zisd = null;
        zdtoi = null;
      } 
    } 
    LR_GET_INSTANT = lrgi;
    ZI_SYSTEM_DEFAULT = zisd;
    ZDT_OF_INSTANT = zdtoi;
  }
  
  private static final Object LOG_MANAGER = loadLogManager();
  
  private final String prefix;
  
  private static Object loadLogManager() {
    Object m;
    try {
      m = LogManager.getLogManager();
    } catch (LinkageError restricted) {
      m = readConfiguration();
    } catch (RuntimeException unexpected) {
      m = readConfiguration();
    } 
    return m;
  }
  
  private static Properties readConfiguration() {
    Properties props = new Properties();
    try {
      String n = System.getProperty("java.util.logging.config.file");
      if (n != null) {
        File f = (new File(n)).getCanonicalFile();
        InputStream in = new FileInputStream(f);
        try {
          props.load(in);
        } finally {
          in.close();
        } 
      } 
    } catch (RuntimeException runtimeException) {
    
    } catch (Exception exception) {
    
    } catch (LinkageError linkageError) {}
    return props;
  }
  
  static String fromLogManager(String name) {
    if (name == null)
      throw new NullPointerException(); 
    Object m = LOG_MANAGER;
    try {
      if (m instanceof Properties)
        return ((Properties)m).getProperty(name); 
    } catch (RuntimeException runtimeException) {}
    if (m != null)
      try {
        if (m instanceof LogManager)
          return ((LogManager)m).getProperty(name); 
      } catch (LinkageError linkageError) {
      
      } catch (RuntimeException runtimeException) {} 
    return null;
  }
  
  static void checkLogManagerAccess() {
    boolean checked = false;
    Object m = LOG_MANAGER;
    if (m != null)
      try {
        if (m instanceof LogManager) {
          checked = true;
          ((LogManager)m).checkAccess();
        } 
      } catch (SecurityException notAllowed) {
        if (checked)
          throw notAllowed; 
      } catch (LinkageError linkageError) {
      
      } catch (RuntimeException runtimeException) {} 
    if (!checked)
      checkLoggingAccess(); 
  }
  
  private static void checkLoggingAccess() {
    boolean checked = false;
    Logger global = Logger.getLogger("global");
    try {
      if (Logger.class == global.getClass()) {
        global.removeHandler((Handler)null);
        checked = true;
      } 
    } catch (NullPointerException nullPointerException) {}
    if (!checked) {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
        sm.checkPermission(new LoggingPermission("control", null)); 
    } 
  }
  
  static boolean hasLogManager() {
    Object m = LOG_MANAGER;
    return (m != null && !(m instanceof Properties));
  }
  
  static Comparable<?> getZonedDateTime(LogRecord record) {
    if (record == null)
      throw new NullPointerException(); 
    Method m = ZDT_OF_INSTANT;
    if (m != null)
      try {
        return (Comparable)m.invoke(null, new Object[] { LR_GET_INSTANT
              .invoke(record, new Object[0]), ZI_SYSTEM_DEFAULT
              .invoke(null, new Object[0]) });
      } catch (RuntimeException ignore) {
        assert LR_GET_INSTANT != null && ZI_SYSTEM_DEFAULT != null : ignore;
      } catch (InvocationTargetException ite) {
        Throwable cause = ite.getCause();
        if (cause instanceof Error)
          throw (Error)cause; 
        if (cause instanceof RuntimeException)
          throw (RuntimeException)cause; 
        throw new UndeclaredThrowableException(ite);
      } catch (Exception exception) {} 
    return null;
  }
  
  static String getLocalHost(Object s) throws Exception {
    try {
      Method m = s.getClass().getMethod("getLocalHost", new Class[0]);
      if (!Modifier.isStatic(m.getModifiers()) && m
        .getReturnType() == String.class)
        return (String)m.invoke(s, new Object[0]); 
      throw new NoSuchMethodException(m.toString());
    } catch (ExceptionInInitializerError EIIE) {
      throw wrapOrThrow(EIIE);
    } catch (InvocationTargetException ite) {
      throw paramOrError(ite);
    } 
  }
  
  static long parseDurationToMillis(CharSequence value) throws Exception {
    try {
      Class<?> k = findClass("java.time.Duration");
      Method parse = k.getMethod("parse", new Class[] { CharSequence.class });
      if (!k.isAssignableFrom(parse.getReturnType()) || 
        !Modifier.isStatic(parse.getModifiers()))
        throw new NoSuchMethodException(parse.toString()); 
      Method toMillis = k.getMethod("toMillis", new Class[0]);
      if (!long.class.isAssignableFrom(toMillis.getReturnType()) || 
        Modifier.isStatic(toMillis.getModifiers()))
        throw new NoSuchMethodException(toMillis.toString()); 
      return ((Long)toMillis.invoke(parse.invoke(null, new Object[] { value }), new Object[0])).longValue();
    } catch (ExceptionInInitializerError EIIE) {
      throw wrapOrThrow(EIIE);
    } catch (InvocationTargetException ite) {
      throw paramOrError(ite);
    } 
  }
  
  static String toLanguageTag(Locale locale) {
    String l = locale.getLanguage();
    String c = locale.getCountry();
    String v = locale.getVariant();
    char[] b = new char[l.length() + c.length() + v.length() + 2];
    int count = l.length();
    l.getChars(0, count, b, 0);
    if (c.length() != 0 || (l.length() != 0 && v.length() != 0)) {
      b[count] = '-';
      count++;
      c.getChars(0, c.length(), b, count);
      count += c.length();
    } 
    if (v.length() != 0 && (l.length() != 0 || c.length() != 0)) {
      b[count] = '-';
      count++;
      v.getChars(0, v.length(), b, count);
      count += v.length();
    } 
    return String.valueOf(b, 0, count);
  }
  
  static Filter newFilter(String name) throws Exception {
    return newObjectFrom(name, Filter.class);
  }
  
  static Formatter newFormatter(String name) throws Exception {
    return newObjectFrom(name, Formatter.class);
  }
  
  static Comparator<? super LogRecord> newComparator(String name) throws Exception {
    return newObjectFrom(name, (Class)Comparator.class);
  }
  
  static <T> Comparator<T> reverseOrder(Comparator<T> c) {
    if (c == null)
      throw new NullPointerException(); 
    Comparator<T> reverse = null;
    try {
      Method m = c.getClass().getMethod("reversed", new Class[0]);
      if (!Modifier.isStatic(m.getModifiers()) && Comparator.class
        .isAssignableFrom(m.getReturnType()))
        try {
          reverse = (Comparator<T>)m.invoke(c, new Object[0]);
        } catch (ExceptionInInitializerError eiie) {
          throw wrapOrThrow(eiie);
        }  
    } catch (NoSuchMethodException noSuchMethodException) {
    
    } catch (IllegalAccessException illegalAccessException) {
    
    } catch (RuntimeException runtimeException) {
    
    } catch (InvocationTargetException ite) {
      paramOrError(ite);
    } 
    if (reverse == null)
      reverse = Collections.reverseOrder(c); 
    return reverse;
  }
  
  static ErrorManager newErrorManager(String name) throws Exception {
    return newObjectFrom(name, ErrorManager.class);
  }
  
  static boolean isStaticUtilityClass(String name) throws Exception {
    boolean util;
    Class<?> c = findClass(name);
    Class<?> obj = Object.class;
    Method[] methods;
    if (c != obj && (methods = c.getMethods()).length != 0) {
      util = true;
      for (Method m : methods) {
        if (m.getDeclaringClass() != obj && 
          !Modifier.isStatic(m.getModifiers())) {
          util = false;
          break;
        } 
      } 
    } else {
      util = false;
    } 
    return util;
  }
  
  static boolean isReflectionClass(String name) throws Exception {
    String[] names = REFLECT_NAMES;
    if (names == null)
      REFLECT_NAMES = names = reflectionClassNames(); 
    for (String rf : names) {
      if (name.equals(rf))
        return true; 
    } 
    findClass(name);
    return false;
  }
  
  private static String[] reflectionClassNames() throws Exception {
    Class<?> thisClass = LogManagerProperties.class;
    assert Modifier.isFinal(thisClass.getModifiers()) : thisClass;
    try {
      HashSet<String> traces = new HashSet<String>();
      Throwable t = Throwable.class.getConstructor(new Class[0]).newInstance(new Object[0]);
      StackTraceElement[] arrayOfStackTraceElement;
      int i;
      byte b;
      for (arrayOfStackTraceElement = t.getStackTrace(), i = arrayOfStackTraceElement.length, b = 0; b < i; ) {
        StackTraceElement ste = arrayOfStackTraceElement[b];
        if (!thisClass.getName().equals(ste.getClassName())) {
          traces.add(ste.getClassName());
          b++;
        } 
      } 
      Throwable.class.getMethod("fillInStackTrace", new Class[0]).invoke(t, new Object[0]);
      for (arrayOfStackTraceElement = t.getStackTrace(), i = arrayOfStackTraceElement.length, b = 0; b < i; ) {
        StackTraceElement ste = arrayOfStackTraceElement[b];
        if (!thisClass.getName().equals(ste.getClassName())) {
          traces.add(ste.getClassName());
          b++;
        } 
      } 
      return (String[])traces.toArray((Object[])new String[traces.size()]);
    } catch (InvocationTargetException ITE) {
      throw paramOrError(ITE);
    } 
  }
  
  static <T> T newObjectFrom(String name, Class<T> type) throws Exception {
    try {
      Class<?> clazz = findClass(name);
      if (type.isAssignableFrom(clazz))
        try {
          return type.cast(clazz.getConstructor(new Class[0]).newInstance(new Object[0]));
        } catch (InvocationTargetException ITE) {
          throw paramOrError(ITE);
        }  
      throw new ClassCastException(clazz.getName() + " cannot be cast to " + type
          .getName());
    } catch (NoClassDefFoundError NCDFE) {
      throw new ClassNotFoundException(NCDFE.toString(), NCDFE);
    } catch (ExceptionInInitializerError EIIE) {
      throw wrapOrThrow(EIIE);
    } 
  }
  
  private static Exception paramOrError(InvocationTargetException ite) {
    Throwable cause = ite.getCause();
    if (cause != null)
      if ((cause instanceof VirtualMachineError | cause instanceof ThreadDeath) != 0)
        throw (Error)cause;  
    return ite;
  }
  
  private static InvocationTargetException wrapOrThrow(ExceptionInInitializerError eiie) {
    if (eiie.getCause() instanceof Error)
      throw eiie; 
    return new InvocationTargetException(eiie);
  }
  
  private static Class<?> findClass(String name) throws ClassNotFoundException {
    Class<?> clazz;
    ClassLoader[] loaders = getClassLoaders();
    assert loaders.length == 2 : loaders.length;
    if (loaders[0] != null) {
      try {
        clazz = Class.forName(name, false, loaders[0]);
      } catch (ClassNotFoundException tryContext) {
        clazz = tryLoad(name, loaders[1]);
      } 
    } else {
      clazz = tryLoad(name, loaders[1]);
    } 
    return clazz;
  }
  
  private static Class<?> tryLoad(String name, ClassLoader l) throws ClassNotFoundException {
    if (l != null)
      return Class.forName(name, false, l); 
    return Class.forName(name);
  }
  
  private static ClassLoader[] getClassLoaders() {
    return AccessController.<ClassLoader[]>doPrivileged((PrivilegedAction)new PrivilegedAction<ClassLoader[]>() {
          public ClassLoader[] run() {
            ClassLoader[] loaders = new ClassLoader[2];
            try {
              loaders[0] = ClassLoader.getSystemClassLoader();
            } catch (SecurityException ignore) {
              loaders[0] = null;
            } 
            try {
              loaders[1] = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException ignore) {
              loaders[1] = null;
            } 
            return loaders;
          }
        });
  }
  
  LogManagerProperties(Properties parent, String prefix) {
    super(parent);
    if (parent == null || prefix == null)
      throw new NullPointerException(); 
    this.prefix = prefix;
  }
  
  public synchronized Object clone() {
    return exportCopy(this.defaults);
  }
  
  public synchronized String getProperty(String key) {
    String value = this.defaults.getProperty(key);
    if (value == null) {
      if (key.length() > 0)
        value = fromLogManager(this.prefix + '.' + key); 
      if (value == null)
        value = fromLogManager(key); 
      if (value != null) {
        super.put(key, value);
      } else {
        Object v = super.get(key);
        value = (v instanceof String) ? (String)v : null;
      } 
    } 
    return value;
  }
  
  public String getProperty(String key, String def) {
    String value = getProperty(key);
    return (value == null) ? def : value;
  }
  
  public synchronized Object get(Object key) {
    Object value;
    if (key instanceof String) {
      value = getProperty((String)key);
    } else {
      value = null;
    } 
    if (value == null) {
      value = this.defaults.get(key);
      if (value == null && !this.defaults.containsKey(key))
        value = super.get(key); 
    } 
    return value;
  }
  
  public synchronized Object put(Object key, Object value) {
    if (key instanceof String && value instanceof String) {
      Object def = preWrite(key);
      Object man = super.put(key, value);
      return (man == null) ? def : man;
    } 
    return super.put(key, value);
  }
  
  public Object setProperty(String key, String value) {
    return put(key, value);
  }
  
  public synchronized boolean containsKey(Object key) {
    boolean found = (key instanceof String && getProperty((String)key) != null);
    if (!found)
      found = (this.defaults.containsKey(key) || super.containsKey(key)); 
    return found;
  }
  
  public synchronized Object remove(Object key) {
    Object def = preWrite(key);
    Object man = super.remove(key);
    return (man == null) ? def : man;
  }
  
  public Enumeration<?> propertyNames() {
    assert false;
    return super.propertyNames();
  }
  
  public boolean equals(Object o) {
    if (o == null)
      return false; 
    if (o == this)
      return true; 
    if (!(o instanceof Properties))
      return false; 
    assert false : this.prefix;
    return super.equals(o);
  }
  
  public int hashCode() {
    assert false : this.prefix.hashCode();
    return super.hashCode();
  }
  
  private Object preWrite(Object key) {
    assert Thread.holdsLock(this);
    return get(key);
  }
  
  private Properties exportCopy(Properties parent) {
    Thread.holdsLock(this);
    Properties child = new Properties(parent);
    child.putAll(this);
    return child;
  }
  
  private synchronized Object writeReplace() throws ObjectStreamException {
    assert false;
    return exportCopy((Properties)this.defaults.clone());
  }
}
