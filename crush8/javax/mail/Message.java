package javax.mail;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Date;
import javax.mail.search.SearchTerm;

public abstract class Message implements Part {
  protected int msgnum = 0;
  
  protected boolean expunged = false;
  
  protected Folder folder = null;
  
  protected Session session = null;
  
  protected Message() {}
  
  protected Message(Folder folder, int msgnum) {
    this.folder = folder;
    this.msgnum = msgnum;
    this.session = folder.store.session;
  }
  
  protected Message(Session session) {
    this.session = session;
  }
  
  public Session getSession() {
    return this.session;
  }
  
  public abstract Address[] getFrom() throws MessagingException;
  
  public abstract void setFrom() throws MessagingException;
  
  public abstract void setFrom(Address paramAddress) throws MessagingException;
  
  public abstract void addFrom(Address[] paramArrayOfAddress) throws MessagingException;
  
  public abstract Address[] getRecipients(RecipientType paramRecipientType) throws MessagingException;
  
  public static class RecipientType implements Serializable {
    public static final RecipientType TO = new RecipientType("To");
    
    public static final RecipientType CC = new RecipientType("Cc");
    
    public static final RecipientType BCC = new RecipientType("Bcc");
    
    protected String type;
    
    private static final long serialVersionUID = -7479791750606340008L;
    
    protected RecipientType(String type) {
      this.type = type;
    }
    
    protected Object readResolve() throws ObjectStreamException {
      if (this.type.equals("To"))
        return TO; 
      if (this.type.equals("Cc"))
        return CC; 
      if (this.type.equals("Bcc"))
        return BCC; 
      throw new InvalidObjectException("Attempt to resolve unknown RecipientType: " + this.type);
    }
    
    public String toString() {
      return this.type;
    }
  }
  
  public Address[] getAllRecipients() throws MessagingException {
    Address[] to = getRecipients(RecipientType.TO);
    Address[] cc = getRecipients(RecipientType.CC);
    Address[] bcc = getRecipients(RecipientType.BCC);
    if (cc == null && bcc == null)
      return to; 
    int numRecip = ((to != null) ? to.length : 0) + ((cc != null) ? cc.length : 0) + ((bcc != null) ? bcc.length : 0);
    Address[] addresses = new Address[numRecip];
    int pos = 0;
    if (to != null) {
      System.arraycopy(to, 0, addresses, pos, to.length);
      pos += to.length;
    } 
    if (cc != null) {
      System.arraycopy(cc, 0, addresses, pos, cc.length);
      pos += cc.length;
    } 
    if (bcc != null)
      System.arraycopy(bcc, 0, addresses, pos, bcc.length); 
    return addresses;
  }
  
  public abstract void setRecipients(RecipientType paramRecipientType, Address[] paramArrayOfAddress) throws MessagingException;
  
  public void setRecipient(RecipientType type, Address address) throws MessagingException {
    if (address == null) {
      setRecipients(type, null);
    } else {
      Address[] a = new Address[1];
      a[0] = address;
      setRecipients(type, a);
    } 
  }
  
  public abstract void addRecipients(RecipientType paramRecipientType, Address[] paramArrayOfAddress) throws MessagingException;
  
  public void addRecipient(RecipientType type, Address address) throws MessagingException {
    Address[] a = new Address[1];
    a[0] = address;
    addRecipients(type, a);
  }
  
  public Address[] getReplyTo() throws MessagingException {
    return getFrom();
  }
  
  public void setReplyTo(Address[] addresses) throws MessagingException {
    throw new MethodNotSupportedException("setReplyTo not supported");
  }
  
  public abstract String getSubject() throws MessagingException;
  
  public abstract void setSubject(String paramString) throws MessagingException;
  
  public abstract Date getSentDate() throws MessagingException;
  
  public abstract void setSentDate(Date paramDate) throws MessagingException;
  
  public abstract Date getReceivedDate() throws MessagingException;
  
  public abstract Flags getFlags() throws MessagingException;
  
  public boolean isSet(Flags.Flag flag) throws MessagingException {
    return getFlags().contains(flag);
  }
  
  public abstract void setFlags(Flags paramFlags, boolean paramBoolean) throws MessagingException;
  
  public void setFlag(Flags.Flag flag, boolean set) throws MessagingException {
    Flags f = new Flags(flag);
    setFlags(f, set);
  }
  
  public int getMessageNumber() {
    return this.msgnum;
  }
  
  protected void setMessageNumber(int msgnum) {
    this.msgnum = msgnum;
  }
  
  public Folder getFolder() {
    return this.folder;
  }
  
  public boolean isExpunged() {
    return this.expunged;
  }
  
  protected void setExpunged(boolean expunged) {
    this.expunged = expunged;
  }
  
  public abstract Message reply(boolean paramBoolean) throws MessagingException;
  
  public abstract void saveChanges() throws MessagingException;
  
  public boolean match(SearchTerm term) throws MessagingException {
    return term.match(this);
  }
}
