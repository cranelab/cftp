package com.sun.mail.util.logging;

import java.lang.reflect.UndeclaredThrowableException;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class CollectorFormatter extends Formatter {
  private static final long INIT_TIME = System.currentTimeMillis();
  
  private final String fmt;
  
  private final Formatter formatter;
  
  private final Comparator<? super LogRecord> comparator;
  
  private LogRecord last;
  
  private long count;
  
  private long generation = 1L;
  
  private long thrown;
  
  private long minMillis = INIT_TIME;
  
  private long maxMillis = Long.MIN_VALUE;
  
  public CollectorFormatter() {
    String p = getClass().getName();
    this.fmt = initFormat(p);
    this.formatter = initFormatter(p);
    this.comparator = initComparator(p);
  }
  
  public CollectorFormatter(String format) {
    String p = getClass().getName();
    this.fmt = (format == null) ? initFormat(p) : format;
    this.formatter = initFormatter(p);
    this.comparator = initComparator(p);
  }
  
  public CollectorFormatter(String format, Formatter f, Comparator<? super LogRecord> c) {
    String p = getClass().getName();
    this.fmt = (format == null) ? initFormat(p) : format;
    this.formatter = f;
    this.comparator = c;
  }
  
  public String format(LogRecord record) {
    if (record == null)
      throw new NullPointerException(); 
    while (true) {
      boolean accepted;
      LogRecord peek = peek();
      LogRecord update = apply((peek != null) ? peek : record, record);
      if (peek != update) {
        update.getSourceMethodName();
        accepted = acceptAndUpdate(peek, update);
      } else {
        accepted = accept(peek, record);
      } 
      if (accepted)
        return ""; 
    } 
  }
  
  public String getTail(Handler h) {
    super.getTail(h);
    return formatRecord(h, true);
  }
  
  public String toString() {
    String result;
    try {
      result = formatRecord((Handler)null, false);
    } catch (RuntimeException ignore) {
      result = super.toString();
    } 
    return result;
  }
  
  protected LogRecord apply(LogRecord t, LogRecord u) {
    if (t == null || u == null)
      throw new NullPointerException(); 
    if (this.comparator != null)
      return (this.comparator.compare(t, u) >= 0) ? t : u; 
    return u;
  }
  
  private synchronized boolean accept(LogRecord e, LogRecord u) {
    long millis = u.getMillis();
    Throwable ex = u.getThrown();
    if (this.last == e) {
      if (++this.count != 1L) {
        this.minMillis = Math.min(this.minMillis, millis);
      } else {
        this.minMillis = millis;
      } 
      this.maxMillis = Math.max(this.maxMillis, millis);
      if (ex != null)
        this.thrown++; 
      return true;
    } 
    return false;
  }
  
  private synchronized void reset(long min) {
    if (this.last != null) {
      this.last = null;
      this.generation++;
    } 
    this.count = 0L;
    this.thrown = 0L;
    this.minMillis = min;
    this.maxMillis = Long.MIN_VALUE;
  }
  
  private String formatRecord(Handler h, boolean reset) {
    LogRecord record;
    long c, t, g, msl, msh, now;
    String head, msg, tail;
    MessageFormat mf;
    synchronized (this) {
      record = this.last;
      c = this.count;
      g = this.generation;
      t = this.thrown;
      msl = this.minMillis;
      msh = this.maxMillis;
      now = System.currentTimeMillis();
      if (c == 0L)
        msh = now; 
      if (reset)
        reset(msh); 
    } 
    Formatter f = this.formatter;
    if (f != null) {
      synchronized (f) {
        head = f.getHead(h);
        msg = (record != null) ? f.format(record) : "";
        tail = f.getTail(h);
      } 
    } else {
      head = "";
      msg = (record != null) ? formatMessage(record) : "";
      tail = "";
    } 
    Locale l = null;
    if (record != null) {
      ResourceBundle rb = record.getResourceBundle();
      l = (rb == null) ? null : rb.getLocale();
    } 
    if (l == null) {
      mf = new MessageFormat(this.fmt);
    } else {
      mf = new MessageFormat(this.fmt, l);
    } 
    return mf.format(new Object[] { 
          finish(head), finish(msg), finish(tail), 
          Long.valueOf(c), Long.valueOf(c - 1L), Long.valueOf(t), Long.valueOf(c - t), Long.valueOf(msl), Long.valueOf(msh), Long.valueOf(msh - msl), 
          Long.valueOf(INIT_TIME), Long.valueOf(now), 
          Long.valueOf(now - INIT_TIME), Long.valueOf(g) });
  }
  
  protected String finish(String s) {
    return s.trim();
  }
  
  private synchronized LogRecord peek() {
    return this.last;
  }
  
  private synchronized boolean acceptAndUpdate(LogRecord e, LogRecord u) {
    if (accept(e, u)) {
      this.last = u;
      return true;
    } 
    return false;
  }
  
  private String initFormat(String p) {
    String v = LogManagerProperties.fromLogManager(p.concat(".format"));
    if (v == null || v.length() == 0)
      v = "{0}{1}{2}{4,choice,-1#|0#|0<... {4,number,integer} more}\n"; 
    return v;
  }
  
  private Formatter initFormatter(String p) {
    Formatter f;
    String v = LogManagerProperties.fromLogManager(p.concat(".formatter"));
    if (v != null && v.length() != 0) {
      if (!"null".equalsIgnoreCase(v)) {
        try {
          f = LogManagerProperties.newFormatter(v);
        } catch (RuntimeException re) {
          throw re;
        } catch (Exception e) {
          throw new UndeclaredThrowableException(e);
        } 
      } else {
        f = null;
      } 
    } else {
      f = Formatter.class.cast(new CompactFormatter());
    } 
    return f;
  }
  
  private Comparator<? super LogRecord> initComparator(String p) {
    Comparator<? super LogRecord> c;
    String name = LogManagerProperties.fromLogManager(p.concat(".comparator"));
    String reverse = LogManagerProperties.fromLogManager(p.concat(".comparator.reverse"));
    try {
      if (name != null && name.length() != 0) {
        if (!"null".equalsIgnoreCase(name)) {
          c = LogManagerProperties.newComparator(name);
          if (Boolean.parseBoolean(reverse)) {
            assert c != null;
            c = LogManagerProperties.reverseOrder(c);
          } 
        } else {
          if (reverse != null)
            throw new IllegalArgumentException("No comparator to reverse."); 
          c = null;
        } 
      } else {
        if (reverse != null)
          throw new IllegalArgumentException("No comparator to reverse."); 
        c = Comparator.class.cast(SeverityComparator.getInstance());
      } 
    } catch (RuntimeException re) {
      throw re;
    } catch (Exception e) {
      throw new UndeclaredThrowableException(e);
    } 
    return c;
  }
}
