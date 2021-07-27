package crushftp.handlers;

import java.util.Properties;

public class CIProperties extends Properties {
  private static final long serialVersionUID = 1L;
  
  Properties ci = new Properties();
  
  public Object put(Object key, Object val) {
    this.ci.put(key.toString().toUpperCase(), key.toString());
    return super.put(key, val);
  }
  
  public Object get(Object key) {
    return super.get(this.ci.getProperty(key.toString().toUpperCase(), key.toString()));
  }
  
  public String getProperty(String key) {
    return super.getProperty(this.ci.getProperty(key.toString().toUpperCase(), key.toLowerCase()));
  }
  
  public String getProperty(String key, String defaultVal) {
    return super.getProperty(this.ci.getProperty(key.toString().toUpperCase(), key.toString()), defaultVal);
  }
  
  public boolean containsKey(Object key) {
    return super.containsKey(this.ci.getProperty(key.toString().toUpperCase(), key.toString()));
  }
}
