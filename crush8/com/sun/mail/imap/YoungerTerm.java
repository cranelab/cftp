package com.sun.mail.imap;

import java.util.Date;
import javax.mail.Message;
import javax.mail.search.SearchTerm;

public final class YoungerTerm extends SearchTerm {
  private int interval;
  
  private static final long serialVersionUID = 1592714210688163496L;
  
  public YoungerTerm(int interval) {
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
      (d.getTime() >= System.currentTimeMillis() - this.interval * 1000L);
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof YoungerTerm))
      return false; 
    return (this.interval == ((YoungerTerm)obj).interval);
  }
  
  public int hashCode() {
    return this.interval;
  }
}
