package javax.mail;

public class MessagingException extends Exception {
  private Exception next;
  
  private static final long serialVersionUID = -7569192289819959253L;
  
  public MessagingException() {
    initCause(null);
  }
  
  public MessagingException(String s) {
    super(s);
    initCause(null);
  }
  
  public MessagingException(String s, Exception e) {
    super(s);
    this.next = e;
    initCause(null);
  }
  
  public synchronized Exception getNextException() {
    return this.next;
  }
  
  public synchronized Throwable getCause() {
    return this.next;
  }
  
  public synchronized boolean setNextException(Exception ex) {
    Exception theEnd = this;
    while (theEnd instanceof MessagingException && ((MessagingException)theEnd).next != null)
      theEnd = ((MessagingException)theEnd).next; 
    if (theEnd instanceof MessagingException) {
      ((MessagingException)theEnd).next = ex;
      return true;
    } 
    return false;
  }
  
  public synchronized String toString() {
    String s = super.toString();
    Exception n = this.next;
    if (n == null)
      return s; 
    StringBuffer sb = new StringBuffer((s == null) ? "" : s);
    while (n != null) {
      sb.append(";\n  nested exception is:\n\t");
      if (n instanceof MessagingException) {
        MessagingException mex = (MessagingException)n;
        sb.append(mex.superToString());
        n = mex.next;
        continue;
      } 
      sb.append(n.toString());
      n = null;
    } 
    return sb.toString();
  }
  
  private final String superToString() {
    return super.toString();
  }
}
