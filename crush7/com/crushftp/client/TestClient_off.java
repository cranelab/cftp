package com.crushftp.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Vector;

public class TestClient_off extends GenericClient {
  public String client_class_str = "com.crushftp.client.TestClient";
  
  String base_path = null;
  
  public TestClient_off(String url, String header, Vector log) {
    super(header, log);
    this.url = url;
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    throw new Exception("DISABLED");
  }
  
  public void setConfig(Properties config) {
    this.config = config;
  }
  
  public void logout() throws Exception {}
  
  public Vector list(String path, Vector list) throws Exception {
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    File folder = new File(String.valueOf(this.base_path) + path);
    if (!folder.exists())
      throw new IOException("No such folder:" + path); 
    File[] files = folder.listFiles();
    for (int x = 0; x < files.length; x++)
      list.add(stat(String.valueOf(path) + files[x].getName())); 
    return list;
  }
  
  public Properties stat(String path) throws Exception {
    if (path.endsWith(":filetree"))
      path = path.substring(0, path.indexOf(":filetree") - 1); 
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties p = new Properties();
    File f = new File(String.valueOf(this.base_path) + path);
    if (!f.exists())
      return null; 
    if (f.isFile()) {
      p.put("permissions", "-rwxrwxrwx");
      p.put("type", "FILE");
    } else {
      p.put("type", "DIR");
      p.put("permissions", "drwxrwxrwx");
      p.put("check_all_recursive_deletes", "true");
    } 
    p.put("name", f.getName());
    p.put("path", path);
    p.put("size", (new StringBuffer(String.valueOf(f.length()))).toString());
    p.put("url", "custom." + this.client_class_str + "://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@no.host.com" + path);
    p.put("owner", "owner");
    p.put("group", "group");
    p.put("modified", (new StringBuffer(String.valueOf(f.lastModified()))).toString());
    return p;
  }
  
  public InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    this.in = new FileInputStream(String.valueOf(this.base_path) + path);
    this.in.skip(startPos);
    return this.in;
  }
  
  public OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    this.out = new FileOutputStream(String.valueOf(this.base_path) + path, truncate);
    return this.out;
  }
  
  public boolean delete(String path) throws Exception {
    Properties p = stat(path);
    if (p == null)
      return false; 
    return (new File(String.valueOf(this.base_path) + path)).delete();
  }
  
  public boolean makedirs(String path) throws Exception {
    return (new File(String.valueOf(this.base_path) + path)).mkdirs();
  }
  
  public boolean makedir(String path) throws Exception {
    return (new File(String.valueOf(this.base_path) + path)).mkdir();
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    Properties p = stat(path);
    if (p == null)
      return false; 
    return (new File(String.valueOf(this.base_path) + path)).setLastModified(modified);
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    return (new File(String.valueOf(this.base_path) + rnfr)).renameTo(new File(String.valueOf(this.base_path) + rnto));
  }
}
