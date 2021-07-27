package javax.mail.internet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.mail.Address;

public class NewsAddress extends Address {
  protected String newsgroup;
  
  protected String host;
  
  private static final long serialVersionUID = -4203797299824684143L;
  
  public NewsAddress() {}
  
  public NewsAddress(String newsgroup) {
    this(newsgroup, null);
  }
  
  public NewsAddress(String newsgroup, String host) {
    this.newsgroup = newsgroup.replaceAll("\\s+", "");
    this.host = host;
  }
  
  public String getType() {
    return "news";
  }
  
  public void setNewsgroup(String newsgroup) {
    this.newsgroup = newsgroup;
  }
  
  public String getNewsgroup() {
    return this.newsgroup;
  }
  
  public void setHost(String host) {
    this.host = host;
  }
  
  public String getHost() {
    return this.host;
  }
  
  public String toString() {
    return this.newsgroup;
  }
  
  public boolean equals(Object a) {
    if (!(a instanceof NewsAddress))
      return false; 
    NewsAddress s = (NewsAddress)a;
    return (((this.newsgroup == null && s.newsgroup == null) || (this.newsgroup != null && this.newsgroup
      .equals(s.newsgroup))) && ((this.host == null && s.host == null) || (this.host != null && s.host != null && this.host
      
      .equalsIgnoreCase(s.host))));
  }
  
  public int hashCode() {
    int hash = 0;
    if (this.newsgroup != null)
      hash += this.newsgroup.hashCode(); 
    if (this.host != null)
      hash += this.host.toLowerCase(Locale.ENGLISH).hashCode(); 
    return hash;
  }
  
  public static String toString(Address[] addresses) {
    if (addresses == null || addresses.length == 0)
      return null; 
    StringBuffer s = new StringBuffer(((NewsAddress)addresses[0]).toString());
    int used = s.length();
    for (int i = 1; i < addresses.length; i++) {
      s.append(",");
      used++;
      String ng = ((NewsAddress)addresses[i]).toString();
      if (used + ng.length() > 76) {
        s.append("\r\n\t");
        used = 8;
      } 
      s.append(ng);
      used += ng.length();
    } 
    return s.toString();
  }
  
  public static NewsAddress[] parse(String newsgroups) throws AddressException {
    StringTokenizer st = new StringTokenizer(newsgroups, ",");
    List<NewsAddress> nglist = new ArrayList<NewsAddress>();
    while (st.hasMoreTokens()) {
      String ng = st.nextToken();
      nglist.add(new NewsAddress(ng));
    } 
    return nglist.<NewsAddress>toArray(new NewsAddress[nglist.size()]);
  }
}
