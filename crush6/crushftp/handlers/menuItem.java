package crushftp.handlers;

import java.io.Serializable;
import java.util.Properties;

public class menuItem implements Serializable {
  public String display = null;
  
  public Properties val = null;
  
  public menuItem(String display, Properties val2) {
    this.display = display;
    this.val = val2;
  }
  
  public menuItem(String display, String val2) {
    this(display, display, val2);
  }
  
  public menuItem(String display, String key, String val2) {
    this.display = display;
    this.val = new Properties();
    this.val.put("key", key);
    this.val.put("value", val2);
  }
  
  public Properties getVal() {
    return this.val;
  }
  
  public String toString() {
    return this.display;
  }
}
