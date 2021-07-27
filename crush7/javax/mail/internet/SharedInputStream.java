package javax.mail.internet;

import java.io.InputStream;

public interface SharedInputStream {
  long getPosition();
  
  InputStream newStream(long paramLong1, long paramLong2);
}
