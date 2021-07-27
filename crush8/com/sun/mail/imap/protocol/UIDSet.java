package com.sun.mail.imap.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class UIDSet {
  public long start;
  
  public long end;
  
  public UIDSet() {}
  
  public UIDSet(long start, long end) {
    this.start = start;
    this.end = end;
  }
  
  public long size() {
    return this.end - this.start + 1L;
  }
  
  public static UIDSet[] createUIDSets(long[] uids) {
    if (uids == null)
      return null; 
    List<UIDSet> v = new ArrayList<UIDSet>();
    for (int i = 0; i < uids.length; i++) {
      UIDSet ms = new UIDSet();
      ms.start = uids[i];
      int j;
      for (j = i + 1; j < uids.length && 
        uids[j] == uids[j - 1] + 1L; j++);
      ms.end = uids[j - 1];
      v.add(ms);
      i = j - 1;
    } 
    UIDSet[] uidset = new UIDSet[v.size()];
    return v.<UIDSet>toArray(uidset);
  }
  
  public static UIDSet[] parseUIDSets(String uids) {
    if (uids == null)
      return null; 
    List<UIDSet> v = new ArrayList<UIDSet>();
    StringTokenizer st = new StringTokenizer(uids, ",:", true);
    long start = -1L;
    UIDSet cur = null;
    try {
      while (st.hasMoreTokens()) {
        String s = st.nextToken();
        if (s.equals(",")) {
          if (cur != null)
            v.add(cur); 
          cur = null;
          continue;
        } 
        if (s.equals(":"))
          continue; 
        long n = Long.parseLong(s);
        if (cur != null) {
          cur.end = n;
          continue;
        } 
        cur = new UIDSet(n, n);
      } 
    } catch (NumberFormatException numberFormatException) {}
    if (cur != null)
      v.add(cur); 
    UIDSet[] uidset = new UIDSet[v.size()];
    return v.<UIDSet>toArray(uidset);
  }
  
  public static String toString(UIDSet[] uidset) {
    if (uidset == null)
      return null; 
    if (uidset.length == 0)
      return ""; 
    int i = 0;
    StringBuilder s = new StringBuilder();
    int size = uidset.length;
    while (true) {
      long start = (uidset[i]).start;
      long end = (uidset[i]).end;
      if (end > start) {
        s.append(start).append(':').append(end);
      } else {
        s.append(start);
      } 
      i++;
      if (i >= size)
        break; 
      s.append(',');
    } 
    return s.toString();
  }
  
  public static long[] toArray(UIDSet[] uidset) {
    if (uidset == null)
      return null; 
    long[] uids = new long[(int)size(uidset)];
    int i = 0;
    for (UIDSet u : uidset) {
      long n;
      for (n = u.start; n <= u.end; n++)
        uids[i++] = n; 
    } 
    return uids;
  }
  
  public static long[] toArray(UIDSet[] uidset, long uidmax) {
    if (uidset == null)
      return null; 
    long[] uids = new long[(int)size(uidset, uidmax)];
    int i = 0;
    for (UIDSet u : uidset) {
      long n;
      for (n = u.start; n <= u.end && (
        uidmax < 0L || n <= uidmax); n++)
        uids[i++] = n; 
    } 
    return uids;
  }
  
  public static long size(UIDSet[] uidset) {
    long count = 0L;
    if (uidset != null)
      for (UIDSet u : uidset)
        count += u.size();  
    return count;
  }
  
  private static long size(UIDSet[] uidset, long uidmax) {
    long count = 0L;
    if (uidset != null)
      for (UIDSet u : uidset) {
        if (uidmax < 0L) {
          count += u.size();
        } else if (u.start <= uidmax) {
          if (u.end < uidmax) {
            count += u.end - u.start + 1L;
          } else {
            count += uidmax - u.start + 1L;
          } 
        } 
      }  
    return count;
  }
}
