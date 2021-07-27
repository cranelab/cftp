package com.crushftp.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.Vector;

public class CustomClient extends GenericClient {
  public String client_class_str = "";
  
  Class c = null;
  
  Object c_o = null;
  
  static Class class$0;
  
  static Class class$1;
  
  static Class class$2;
  
  public CustomClient(String url, String header, Vector log) {
    super(header, log);
    this.url = url;
    this.client_class_str = (new VRL(url)).getProtocol().substring("custom.".length());
    try {
      try {
        this.c = Thread.currentThread().getContextClassLoader().loadClass(this.client_class_str);
      } catch (Throwable e) {
        this.c = (new URLClassLoader(new URL[] { (new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/lib/" + this.client_class_str + ".jar")).toURI().toURL() }, Thread.currentThread().getContextClassLoader())).loadClass(this.client_class_str);
      } 
      if (class$0 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      false[class$0] = class$0 = Class.forName("java.lang.String");
      if (class$0 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      true[class$0] = class$0 = Class.forName("java.lang.String");
      if (class$1 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      2[class$1] = class$1 = Class.forName("java.util.Vector");
      Constructor cons = (new Class[3]).getConstructor(new Class[3]);
      this.c_o = cons.newInstance(new Object[] { url, header, log });
      setConfig(this.config);
    } catch (Exception e) {
      Common.log("SERVER", 0, e);
    } 
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.config.put("username", username.trim());
    this.config.put("password", VRL.vrlDecode(password.trim()));
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    true[class$0] = class$0 = Class.forName("java.lang.String");
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    2[class$0] = class$0 = Class.forName("java.lang.String");
    Method wrapper = (new Class[3]).getMethod((String)new Class[3], new Class[3]);
    return (String)wrapper.invoke(this.c_o, new Object[] { username, password, clientid });
  }
  
  public void setConfig(Properties config) throws Exception {
    if (class$2 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$2] = class$2 = Class.forName("java.util.Properties");
    Method wrapper = "setConfig".getMethod((String)new Class[1], new Class[1]);
    wrapper.invoke(this.c_o, new Object[] { config });
  }
  
  public void logout() throws Exception {
    Method wrapper = this.c_o.getClass().getMethod("logout", null);
    wrapper.invoke(this.c_o, null);
  }
  
  public Vector list(String path, Vector list) throws Exception {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    if (class$1 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    true[class$1] = class$1 = Class.forName("java.util.Vector");
    Method wrapper = (new Class[2]).getMethod((String)new Class[2], new Class[2]);
    return (Vector)wrapper.invoke(this.c_o, new Object[] { path, list });
  }
  
  public Properties stat(String path) throws Exception {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    Method wrapper = "stat".getMethod((String)new Class[1], new Class[1]);
    return (Properties)wrapper.invoke(this.c_o, new Object[] { path });
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    Method wrapper = "download3".getMethod((String)new Class[4], new Class[] { null, long.class, long.class, boolean.class });
    return (InputStream)wrapper.invoke(this.c_o, new Object[] { path, new Long(startPos), new Long(endPos), new Boolean(binary) });
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    Method wrapper = "upload3".getMethod((String)new Class[4], new Class[] { null, long.class, boolean.class, boolean.class });
    return (OutputStream)wrapper.invoke(this.c_o, new Object[] { path, new Long(startPos), new Boolean(truncate), new Boolean(binary) });
  }
  
  public boolean delete(String path) throws Exception {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    Method wrapper = "delete".getMethod((String)new Class[1], new Class[1]);
    return ((Boolean)wrapper.invoke(this.c_o, new Object[] { path })).booleanValue();
  }
  
  public boolean makedirs(String path) throws Exception {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    Method wrapper = "makedirs".getMethod((String)new Class[1], new Class[1]);
    return ((Boolean)wrapper.invoke(this.c_o, new Object[] { path })).booleanValue();
  }
  
  public boolean makedir(String path) throws Exception {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    Method wrapper = "makedir".getMethod((String)new Class[1], new Class[1]);
    return ((Boolean)wrapper.invoke(this.c_o, new Object[] { path })).booleanValue();
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    Method wrapper = "mdtm".getMethod((String)new Class[2], new Class[] { null, long.class });
    return ((Boolean)wrapper.invoke(this.c_o, new Object[] { path, new Long(modified) })).booleanValue();
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    false[class$0] = class$0 = Class.forName("java.lang.String");
    if (class$0 == null)
      try {
      
      } catch (ClassNotFoundException classNotFoundException) {
        throw new NoClassDefFoundError(null.getMessage());
      }  
    true[class$0] = class$0 = Class.forName("java.lang.String");
    Method wrapper = (new Class[2]).getMethod((String)new Class[2], new Class[2]);
    return ((Boolean)wrapper.invoke(this.c_o, new Object[] { rnfr, rnto })).booleanValue();
  }
}
