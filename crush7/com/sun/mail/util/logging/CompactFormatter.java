package com.sun.mail.util.logging;

import java.util.Collections;
import java.util.Date;
import java.util.Formattable;
import java.util.Formatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class CompactFormatter extends Formatter {
  private final String fmt;
  
  static {
    loadDeclaredClasses();
  }
  
  private static Class<?>[] loadDeclaredClasses() {
    return new Class[] { Alternate.class };
  }
  
  public CompactFormatter() {
    String p = getClass().getName();
    this.fmt = initFormat(p);
  }
  
  public CompactFormatter(String format) {
    String p = getClass().getName();
    this.fmt = (format == null) ? initFormat(p) : format;
  }
  
  public String format(LogRecord record) {
    ResourceBundle rb = record.getResourceBundle();
    Locale l = (rb == null) ? null : rb.getLocale();
    String msg = formatMessage(record);
    String thrown = formatThrown(record);
    String err = formatError(record);
    Object[] params = { 
        formatZonedDateTime(record), formatSource(record), formatLoggerName(record), formatLevel(record), msg, thrown, new Alternate(msg, thrown), new Alternate(thrown, msg), Long.valueOf(record.getSequenceNumber()), formatThreadID(record), 
        err, new Alternate(msg, err), new Alternate(err, msg), formatBackTrace(record), record.getResourceBundleName(), record.getMessage() };
    if (l == null)
      return String.format(this.fmt, params); 
    return String.format(l, this.fmt, params);
  }
  
  public String formatMessage(LogRecord record) {
    String msg = super.formatMessage(record);
    msg = replaceClassName(msg, record.getThrown());
    msg = replaceClassName(msg, record.getParameters());
    return msg;
  }
  
  public String formatMessage(Throwable t) {
    return (t != null) ? replaceClassName(apply(t).getMessage(), t) : "";
  }
  
  public String formatLevel(LogRecord record) {
    return record.getLevel().getLocalizedName();
  }
  
  public String formatSource(LogRecord record) {
    String source = record.getSourceClassName();
    if (source != null) {
      if (record.getSourceMethodName() != null) {
        source = simpleClassName(source) + " " + record.getSourceMethodName();
      } else {
        source = simpleClassName(source);
      } 
    } else {
      source = simpleClassName(record.getLoggerName());
    } 
    return source;
  }
  
  public String formatLoggerName(LogRecord record) {
    return simpleClassName(record.getLoggerName());
  }
  
  public Number formatThreadID(LogRecord record) {
    return Long.valueOf(record.getThreadID() & 0xFFFFFFFFL);
  }
  
  public String formatThrown(LogRecord record) {
    String msg;
    Throwable t = record.getThrown();
    if (t != null) {
      String site = formatBackTrace(record);
      msg = formatToString(t) + (isNullOrSpaces(site) ? "" : (' ' + site));
    } else {
      msg = "";
    } 
    return msg;
  }
  
  public String formatError(LogRecord record) {
    Throwable t = record.getThrown();
    if (t != null)
      return formatToString(t); 
    return "";
  }
  
  private String formatToString(Throwable t) {
    return simpleClassName(apply(t).getClass()) + ": " + formatMessage(t);
  }
  
  public String formatBackTrace(LogRecord record) {
    String site = "";
    Throwable t = record.getThrown();
    if (t != null) {
      Throwable root = apply(t);
      site = findAndFormat(root.getStackTrace());
      if (isNullOrSpaces(site)) {
        int limit = 0;
        for (Throwable c = t; c != null; c = c.getCause()) {
          site = findAndFormat(c.getStackTrace());
          if (!isNullOrSpaces(site))
            break; 
          if (++limit == 65536)
            break; 
        } 
      } 
    } 
    return site;
  }
  
  private String findAndFormat(StackTraceElement[] trace) {
    String site = "";
    for (StackTraceElement s : trace) {
      if (!ignore(s)) {
        site = formatStackTraceElement(s);
        break;
      } 
    } 
    if (isNullOrSpaces(site))
      for (StackTraceElement s : trace) {
        if (!defaultIgnore(s)) {
          site = formatStackTraceElement(s);
          break;
        } 
      }  
    return site;
  }
  
  private String formatStackTraceElement(StackTraceElement s) {
    String result, v = simpleClassName(s.getClassName());
    if (v != null) {
      result = s.toString().replace(s.getClassName(), v);
    } else {
      result = s.toString();
    } 
    v = simpleFileName(s.getFileName());
    if (v != null && result.startsWith(v))
      result = result.replace(s.getFileName(), ""); 
    return result;
  }
  
  protected Throwable apply(Throwable t) {
    return SeverityComparator.getInstance().apply(t);
  }
  
  protected boolean ignore(StackTraceElement s) {
    return (isUnknown(s) || defaultIgnore(s));
  }
  
  protected String toAlternate(String s) {
    return (s != null) ? s.replaceAll("[\\x00-\\x1F\\x7F]+", "") : null;
  }
  
  private Comparable<?> formatZonedDateTime(LogRecord record) {
    Comparable<?> zdt = LogManagerProperties.getZonedDateTime(record);
    if (zdt == null)
      zdt = new Date(record.getMillis()); 
    return zdt;
  }
  
  private boolean defaultIgnore(StackTraceElement s) {
    return (isSynthetic(s) || isStaticUtility(s) || isReflection(s));
  }
  
  private boolean isStaticUtility(StackTraceElement s) {
    try {
      return LogManagerProperties.isStaticUtilityClass(s.getClassName());
    } catch (RuntimeException runtimeException) {
    
    } catch (Exception exception) {
    
    } catch (LinkageError linkageError) {}
    String cn = s.getClassName();
    return ((cn.endsWith("s") && !cn.endsWith("es")) || cn
      .contains("Util") || cn.endsWith("Throwables"));
  }
  
  private boolean isSynthetic(StackTraceElement s) {
    return (s.getMethodName().indexOf('$') > -1);
  }
  
  private boolean isUnknown(StackTraceElement s) {
    return (s.getLineNumber() < 0);
  }
  
  private boolean isReflection(StackTraceElement s) {
    try {
      return LogManagerProperties.isReflectionClass(s.getClassName());
    } catch (RuntimeException runtimeException) {
    
    } catch (Exception exception) {
    
    } catch (LinkageError linkageError) {}
    return (s.getClassName().startsWith("java.lang.reflect.") || s
      .getClassName().startsWith("sun.reflect."));
  }
  
  private String initFormat(String p) {
    String v = LogManagerProperties.fromLogManager(p.concat(".format"));
    if (isNullOrSpaces(v))
      v = "%7$#.160s%n"; 
    return v;
  }
  
  private static String replaceClassName(String msg, Throwable t) {
    if (!isNullOrSpaces(msg)) {
      int limit = 0;
      for (Throwable c = t; c != null; c = c.getCause()) {
        Class<?> k = c.getClass();
        msg = msg.replace(k.getName(), simpleClassName(k));
        if (++limit == 65536)
          break; 
      } 
    } 
    return msg;
  }
  
  private static String replaceClassName(String msg, Object[] p) {
    if (!isNullOrSpaces(msg) && p != null)
      for (Object o : p) {
        if (o != null) {
          Class<?> k = o.getClass();
          msg = msg.replace(k.getName(), simpleClassName(k));
        } 
      }  
    return msg;
  }
  
  private static String simpleClassName(Class<?> k) {
    try {
      return k.getSimpleName();
    } catch (InternalError internalError) {
      return simpleClassName(k.getName());
    } 
  }
  
  private static String simpleClassName(String name) {
    if (name != null) {
      int index = name.lastIndexOf('.');
      name = (index > -1) ? name.substring(index + 1) : name;
    } 
    return name;
  }
  
  private static String simpleFileName(String name) {
    if (name != null) {
      int index = name.lastIndexOf('.');
      name = (index > -1) ? name.substring(0, index) : name;
    } 
    return name;
  }
  
  private static boolean isNullOrSpaces(String s) {
    return (s == null || s.trim().length() == 0);
  }
  
  private class Alternate implements Formattable {
    private final String left;
    
    private final String right;
    
    Alternate(String left, String right) {
      this.left = String.valueOf(left);
      this.right = String.valueOf(right);
    }
    
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
      String l = this.left;
      String r = this.right;
      if ((flags & 0x2) == 2) {
        l = l.toUpperCase(formatter.locale());
        r = r.toUpperCase(formatter.locale());
      } 
      if ((flags & 0x4) == 4) {
        l = CompactFormatter.this.toAlternate(l);
        r = CompactFormatter.this.toAlternate(r);
      } 
      if (precision <= 0)
        precision = Integer.MAX_VALUE; 
      int fence = Math.min(l.length(), precision);
      if (fence > precision >> 1)
        fence = Math.max(fence - r.length(), fence >> 1); 
      if (fence > 0) {
        if (fence > l.length() && 
          Character.isHighSurrogate(l.charAt(fence - 1)))
          fence--; 
        l = l.substring(0, fence);
      } 
      r = r.substring(0, Math.min(precision - fence, r.length()));
      if (width > 0) {
        int half = width >> 1;
        if (l.length() < half)
          l = pad(flags, l, half); 
        if (r.length() < half)
          r = pad(flags, r, half); 
      } 
      Object[] empty = Collections.emptySet().toArray();
      formatter.format(l, empty);
      if (l.length() != 0 && r.length() != 0)
        formatter.format("|", empty); 
      formatter.format(r, empty);
    }
    
    private String pad(int flags, String s, int length) {
      int padding = length - s.length();
      StringBuilder b = new StringBuilder(length);
      if ((flags & 0x1) == 1) {
        for (int i = 0; i < padding; i++)
          b.append(' '); 
        b.append(s);
      } else {
        b.append(s);
        for (int i = 0; i < padding; i++)
          b.append(' '); 
      } 
      return b.toString();
    }
  }
}
