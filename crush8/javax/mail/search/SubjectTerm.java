package javax.mail.search;

import javax.mail.Message;

public final class SubjectTerm extends StringTerm {
  private static final long serialVersionUID = 7481568618055573432L;
  
  public SubjectTerm(String pattern) {
    super(pattern);
  }
  
  public boolean match(Message msg) {
    String subj;
    try {
      subj = msg.getSubject();
    } catch (Exception e) {
      return false;
    } 
    if (subj == null)
      return false; 
    return match(subj);
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof SubjectTerm))
      return false; 
    return super.equals(obj);
  }
}
