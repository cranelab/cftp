package javax.mail.search;

import javax.mail.Address;
import javax.mail.Message;

public final class RecipientTerm extends AddressTerm {
  private Message.RecipientType type;
  
  private static final long serialVersionUID = 6548700653122680468L;
  
  public RecipientTerm(Message.RecipientType type, Address address) {
    super(address);
    this.type = type;
  }
  
  public Message.RecipientType getRecipientType() {
    return this.type;
  }
  
  public boolean match(Message msg) {
    Address[] recipients;
    try {
      recipients = msg.getRecipients(this.type);
    } catch (Exception e) {
      return false;
    } 
    if (recipients == null)
      return false; 
    for (int i = 0; i < recipients.length; i++) {
      if (match(recipients[i]))
        return true; 
    } 
    return false;
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof RecipientTerm))
      return false; 
    RecipientTerm rt = (RecipientTerm)obj;
    return (rt.type.equals(this.type) && super.equals(obj));
  }
  
  public int hashCode() {
    return this.type.hashCode() + super.hashCode();
  }
}
