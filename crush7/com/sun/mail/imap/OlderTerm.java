package com.sun.mail.imap;

import java.util.Date;
import javax.mail.Message;
import javax.mail.search.SearchTerm;

public final class OlderTerm extends SearchTerm {
  private int interval;
  
  private static final long serialVersionUID = 3951078948727995682L;
  
  public OlderTerm(int interval) {
    this.interval = interval;
  }
  
  public int getInterval() {
    return this.interval;
  }
  
  public boolean match(Message msg) {
    Date d;
    try {
      d = msg.getReceivedDate();
    } catch (Exception e) {
      return false;
    } 
    if (d == null)
      return false; 
    return 
      (d.getTime() <= System.currentTimeMillis() - this.interval * 1000L);
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof OlderTerm))
      return false; 
    return (this.interval == ((OlderTerm)obj).interval);
  }
  
  public int hashCode() {
    return this.interval;
  }
}
