package com.sun.mail.imap.protocol;

import com.sun.mail.iap.ParsingException;
import com.sun.mail.iap.Response;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Status {
  public String mbox = null;
  
  public int total = -1;
  
  public int recent = -1;
  
  public long uidnext = -1L;
  
  public long uidvalidity = -1L;
  
  public int unseen = -1;
  
  public long highestmodseq = -1L;
  
  public Map<String, Long> items;
  
  static final String[] standardItems = new String[] { "MESSAGES", "RECENT", "UNSEEN", "UIDNEXT", "UIDVALIDITY" };
  
  public Status(Response r) throws ParsingException {
    this.mbox = BASE64MailboxDecoder.decode(r.readAtomString());
    StringBuffer buffer = new StringBuffer();
    boolean onlySpaces = true;
    while (r.peekByte() != 40 && r.peekByte() != 0) {
      char next = (char)r.readByte();
      buffer.append(next);
      if (next != ' ')
        onlySpaces = false; 
    } 
    if (!onlySpaces)
      this.mbox = (this.mbox + buffer).trim(); 
    if (r.readByte() != 40)
      throw new ParsingException("parse error in STATUS"); 
    do {
      String attr = r.readAtom();
      if (attr.equalsIgnoreCase("MESSAGES")) {
        this.total = r.readNumber();
      } else if (attr.equalsIgnoreCase("RECENT")) {
        this.recent = r.readNumber();
      } else if (attr.equalsIgnoreCase("UIDNEXT")) {
        this.uidnext = r.readLong();
      } else if (attr.equalsIgnoreCase("UIDVALIDITY")) {
        this.uidvalidity = r.readLong();
      } else if (attr.equalsIgnoreCase("UNSEEN")) {
        this.unseen = r.readNumber();
      } else if (attr.equalsIgnoreCase("HIGHESTMODSEQ")) {
        this.highestmodseq = r.readLong();
      } else {
        if (this.items == null)
          this.items = new HashMap<String, Long>(); 
        this.items.put(attr.toUpperCase(Locale.ENGLISH), 
            Long.valueOf(r.readLong()));
      } 
    } while (r.readByte() != 41);
  }
  
  public long getItem(String item) {
    item = item.toUpperCase(Locale.ENGLISH);
    long ret = -1L;
    Long v;
    if (this.items != null && (v = this.items.get(item)) != null) {
      ret = v.longValue();
    } else if (item.equals("MESSAGES")) {
      ret = this.total;
    } else if (item.equals("RECENT")) {
      ret = this.recent;
    } else if (item.equals("UIDNEXT")) {
      ret = this.uidnext;
    } else if (item.equals("UIDVALIDITY")) {
      ret = this.uidvalidity;
    } else if (item.equals("UNSEEN")) {
      ret = this.unseen;
    } else if (item.equals("HIGHESTMODSEQ")) {
      ret = this.highestmodseq;
    } 
    return ret;
  }
  
  public static void add(Status s1, Status s2) {
    if (s2.total != -1)
      s1.total = s2.total; 
    if (s2.recent != -1)
      s1.recent = s2.recent; 
    if (s2.uidnext != -1L)
      s1.uidnext = s2.uidnext; 
    if (s2.uidvalidity != -1L)
      s1.uidvalidity = s2.uidvalidity; 
    if (s2.unseen != -1)
      s1.unseen = s2.unseen; 
    if (s2.highestmodseq != -1L)
      s1.highestmodseq = s2.highestmodseq; 
    if (s1.items == null) {
      s1.items = s2.items;
    } else if (s2.items != null) {
      s1.items.putAll(s2.items);
    } 
  }
}
