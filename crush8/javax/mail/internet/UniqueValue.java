package javax.mail.internet;

import java.util.concurrent.atomic.AtomicInteger;
import javax.mail.Session;

class UniqueValue {
  private static AtomicInteger id = new AtomicInteger();
  
  public static String getUniqueBoundaryValue() {
    StringBuffer s = new StringBuffer();
    long hash = s.hashCode();
    s.append("----=_Part_").append(id.getAndIncrement()).append("_")
      .append(hash).append('.')
      .append(System.currentTimeMillis());
    return s.toString();
  }
  
  public static String getUniqueMessageIDValue(Session ssn) {
    String suffix = null;
    InternetAddress addr = InternetAddress.getLocalAddress(ssn);
    if (addr != null) {
      suffix = addr.getAddress();
    } else {
      suffix = "javamailuser@localhost";
    } 
    int at = suffix.lastIndexOf('@');
    if (at >= 0)
      suffix = suffix.substring(at); 
    StringBuffer s = new StringBuffer();
    s.append(s.hashCode()).append('.')
      .append(id.getAndIncrement()).append('.')
      .append(System.currentTimeMillis())
      .append(suffix);
    return s.toString();
  }
}
