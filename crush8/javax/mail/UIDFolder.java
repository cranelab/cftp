package javax.mail;

public interface UIDFolder {
  public static final long LASTUID = -1L;
  
  long getUIDValidity() throws MessagingException;
  
  Message getMessageByUID(long paramLong) throws MessagingException;
  
  Message[] getMessagesByUID(long paramLong1, long paramLong2) throws MessagingException;
  
  Message[] getMessagesByUID(long[] paramArrayOflong) throws MessagingException;
  
  long getUID(Message paramMessage) throws MessagingException;
  
  public static class FetchProfileItem extends FetchProfile.Item {
    protected FetchProfileItem(String name) {
      super(name);
    }
    
    public static final FetchProfileItem UID = new FetchProfileItem("UID");
  }
}
