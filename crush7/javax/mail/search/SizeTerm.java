package javax.mail.search;

import javax.mail.Message;

public final class SizeTerm extends IntegerComparisonTerm {
  private static final long serialVersionUID = -2556219451005103709L;
  
  public SizeTerm(int comparison, int size) {
    super(comparison, size);
  }
  
  public boolean match(Message msg) {
    int size;
    try {
      size = msg.getSize();
    } catch (Exception e) {
      return false;
    } 
    if (size == -1)
      return false; 
    return match(size);
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof SizeTerm))
      return false; 
    return super.equals(obj);
  }
}
