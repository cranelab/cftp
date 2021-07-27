package com.sun.mail.util.logging;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class DurationFilter implements Filter {
  private final long records;
  
  private final long duration;
  
  private long count;
  
  private long peak;
  
  private long start;
  
  public DurationFilter() {
    this.records = checkRecords(initLong(".records"));
    this.duration = checkDuration(initLong(".duration"));
  }
  
  public DurationFilter(long records, long duration) {
    this.records = checkRecords(records);
    this.duration = checkDuration(duration);
  }
  
  public boolean equals(Object obj) {
    long c, p, s;
    if (this == obj)
      return true; 
    if (obj == null || getClass() != obj.getClass())
      return false; 
    DurationFilter other = (DurationFilter)obj;
    if (this.records != other.records)
      return false; 
    if (this.duration != other.duration)
      return false; 
    synchronized (this) {
      c = this.count;
      p = this.peak;
      s = this.start;
    } 
    synchronized (other) {
      if (c != other.count || p != other.peak || s != other.start)
        return false; 
    } 
    return true;
  }
  
  public boolean isIdle() {
    return test(0L, System.currentTimeMillis());
  }
  
  public int hashCode() {
    int hash = 3;
    hash = 89 * hash + (int)(this.records ^ this.records >>> 32L);
    hash = 89 * hash + (int)(this.duration ^ this.duration >>> 32L);
    return hash;
  }
  
  public boolean isLoggable(LogRecord record) {
    return accept(record.getMillis());
  }
  
  public boolean isLoggable() {
    return test(this.records, System.currentTimeMillis());
  }
  
  public String toString() {
    boolean idle;
    boolean loggable;
    synchronized (this) {
      long millis = System.currentTimeMillis();
      idle = test(0L, millis);
      loggable = test(this.records, millis);
    } 
    return getClass().getName() + "{records=" + this.records + ", duration=" + this.duration + ", idle=" + idle + ", loggable=" + loggable + '}';
  }
  
  protected DurationFilter clone() throws CloneNotSupportedException {
    DurationFilter clone = (DurationFilter)super.clone();
    clone.count = 0L;
    clone.peak = 0L;
    clone.start = 0L;
    return clone;
  }
  
  private boolean test(long limit, long millis) {
    long c;
    long s;
    assert limit >= 0L : limit;
    synchronized (this) {
      c = this.count;
      s = this.start;
    } 
    if (c > 0L) {
      if (millis - s >= this.duration || c < limit)
        return true; 
    } else if (millis - s >= 0L || c == 0L) {
      return true;
    } 
    return false;
  }
  
  private synchronized boolean accept(long millis) {
    boolean allow;
    if (this.count > 0L) {
      if (millis - this.peak > 0L)
        this.peak = millis; 
      if (this.count != this.records) {
        this.count++;
        allow = true;
      } else if (this.peak - this.start >= this.duration) {
        this.count = 1L;
        this.start = this.peak;
        allow = true;
      } else {
        this.count = -1L;
        this.start = this.peak + this.duration;
        allow = false;
      } 
    } else if (millis - this.start >= 0L || this.count == 0L) {
      this.count = 1L;
      this.start = millis;
      this.peak = millis;
      allow = true;
    } else {
      allow = false;
    } 
    return allow;
  }
  
  private long initLong(String suffix) {
    long result = 0L;
    String p = getClass().getName();
    String value = LogManagerProperties.fromLogManager(p.concat(suffix));
    if (value != null && value.length() != 0) {
      value = value.trim();
      if (isTimeEntry(suffix, value))
        try {
          result = LogManagerProperties.parseDurationToMillis(value);
        } catch (RuntimeException runtimeException) {
        
        } catch (Exception exception) {
        
        } catch (LinkageError linkageError) {} 
      if (result == 0L)
        try {
          result = 1L;
          for (String s : tokenizeLongs(value)) {
            if (s.endsWith("L") || s.endsWith("l"))
              s = s.substring(0, s.length() - 1); 
            result = multiplyExact(result, Long.parseLong(s));
          } 
        } catch (RuntimeException ignore) {
          result = Long.MIN_VALUE;
        }  
    } else {
      result = Long.MIN_VALUE;
    } 
    return result;
  }
  
  private boolean isTimeEntry(String suffix, String value) {
    return ((value.charAt(0) == 'P' || value.charAt(0) == 'p') && suffix
      .equals(".duration"));
  }
  
  private static String[] tokenizeLongs(String value) {
    int i = value.indexOf('*');
    String[] e;
    if (i > -1 && (e = value.split("\\s*\\*\\s*")).length != 0) {
      if (i == 0 || value.charAt(value.length() - 1) == '*')
        throw new NumberFormatException(value); 
      if (e.length == 1)
        throw new NumberFormatException(e[0]); 
    } else {
      e = new String[] { value };
    } 
    return e;
  }
  
  private static long multiplyExact(long x, long y) {
    long r = x * y;
    if ((Math.abs(x) | Math.abs(y)) >>> 31L != 0L && ((
      y != 0L && r / y != x) || (x == Long.MIN_VALUE && y == -1L)))
      throw new ArithmeticException(); 
    return r;
  }
  
  private static long checkRecords(long records) {
    return (records > 0L) ? records : 1000L;
  }
  
  private static long checkDuration(long duration) {
    return (duration > 0L) ? duration : 900000L;
  }
}
