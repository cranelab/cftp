package javax.mail.search;

import javax.mail.Message;

public final class MessageNumberTerm extends IntegerComparisonTerm {
  private static final long serialVersionUID = -5379625829658623812L;
  
  public MessageNumberTerm(int number) {
    super(3, number);
  }
  
  public boolean match(Message msg) {
    int msgno;
    try {
      msgno = msg.getMessageNumber();
    } catch (Exception e) {
      return false;
    } 
    return match(msgno);
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof MessageNumberTerm))
      return false; 
    return super.equals(obj);
  }
}
