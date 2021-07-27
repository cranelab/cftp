package javax.mail;

import javax.activation.DataSource;

public interface MultipartDataSource extends DataSource {
  int getCount();
  
  BodyPart getBodyPart(int paramInt) throws MessagingException;
}
