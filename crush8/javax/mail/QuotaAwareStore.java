package javax.mail;

public interface QuotaAwareStore {
  Quota[] getQuota(String paramString) throws MessagingException;
  
  void setQuota(Quota paramQuota) throws MessagingException;
}
