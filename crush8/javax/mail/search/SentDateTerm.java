package javax.mail.search;

import java.util.Date;
import javax.mail.Message;

public final class SentDateTerm extends DateTerm {
  private static final long serialVersionUID = 5647755030530907263L;
  
  public SentDateTerm(int comparison, Date date) {
    super(comparison, date);
  }
  
  public boolean match(Message msg) {
    Date d;
    try {
      d = msg.getSentDate();
    } catch (Exception e) {
      return false;
    } 
    if (d == null)
      return false; 
    return match(d);
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof SentDateTerm))
      return false; 
    return super.equals(obj);
  }
}
