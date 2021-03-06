package javax.mail.search;

import java.io.IOException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

public final class BodyTerm extends StringTerm {
  private static final long serialVersionUID = -4888862527916911385L;
  
  public BodyTerm(String pattern) {
    super(pattern);
  }
  
  public boolean match(Message msg) {
    return matchPart(msg);
  }
  
  private boolean matchPart(Part p) {
    try {
      if (p.isMimeType("text/*")) {
        String s = (String)p.getContent();
        if (s == null)
          return false; 
        return match(s);
      } 
      if (p.isMimeType("multipart/*")) {
        Multipart mp = (Multipart)p.getContent();
        int count = mp.getCount();
        for (int i = 0; i < count; i++) {
          if (matchPart(mp.getBodyPart(i)))
            return true; 
        } 
      } else if (p.isMimeType("message/rfc822")) {
        return matchPart((Part)p.getContent());
      } 
    } catch (MessagingException messagingException) {
    
    } catch (IOException iOException) {
    
    } catch (RuntimeException runtimeException) {}
    return false;
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof BodyTerm))
      return false; 
    return super.equals(obj);
  }
}
