package com.crushftp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class MemoryClient extends GenericClient {
  public static Properties ram = new Properties();
  
  public MemoryClient(String url, String header, Vector log) {
    super(header, log);
    this.url = (new VRL(url)).getPath();
    synchronized (ram) {
      if (ram.get("/") == null) {
        Vector v = new Vector();
        Properties item = new Properties();
        item.put("list", v);
        ram.put("", item);
        ram.put("/", item);
      } 
    } 
  }
  
  public void logout() throws Exception {
    close();
    this.logQueue = new Vector();
  }
  
  public void freeCache() {
    this.logQueue = new Vector();
  }
  
  public Properties stat(String path) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    return strip((Properties)ram.get(path));
  }
  
  private Properties strip(Properties item) {
    if (item != null) {
      item = (Properties)item.clone();
      item.remove("object");
      item.remove("list");
    } 
    return item;
  }
  
  public Vector list(String path, Vector list) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties item = (Properties)((Properties)ram.get(path)).clone();
    Vector v = (Vector)item.get("list");
    if (v == null)
      v = new Vector(); 
    synchronized (ram) {
      for (int x = 0; x < v.size(); x++)
        list.addElement(strip(v.elementAt(x))); 
    } 
    return list;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties item = (Properties)ram.get(path);
    this.in = new ByteArrayInputStream(((ByteArrayOutputStream)item.get("object")).toByteArray());
    if (startPos > 0L)
      this.in.skip(startPos); 
    if (endPos > 0L)
      this.in = getLimitedInputStream(this.in, startPos, endPos); 
    return this.in;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties item = (Properties)ram.get(path);
    if (item == null)
      return false; 
    Date itemDate = new Date(modified);
    item.put("modified", (new StringBuffer(String.valueOf(itemDate.getTime()))).toString());
    item.put("month", months[Integer.parseInt(this.mm.format(itemDate))]);
    item.put("day", this.dd.format(itemDate));
    String time_or_year = this.hhmm.format(itemDate);
    if (!this.yyyy.format(itemDate).equals(this.yyyy.format(new Date())) || System.getProperty("crushftp.ls.year", "false").equals("true"))
      time_or_year = this.yyyy.format(itemDate); 
    item.put("time_or_year", time_or_year);
    return true;
  }
  
  public boolean rename(String rnfr0, String rnto0) throws Exception {
    String rnfr = String.valueOf(this.url) + rnfr0.substring(1);
    String rnto = String.valueOf(this.url) + rnto0.substring(1);
    if (rnfr.endsWith("/"))
      rnfr = rnfr.substring(0, rnfr.length() - 1); 
    if (rnto.endsWith("/"))
      rnto = rnto.substring(0, rnto.length() - 1); 
    Properties rnfr_p = (Properties)ram.get(rnfr);
    Properties rnto_p = (Properties)ram.get(rnto);
    if (rnfr_p == null)
      return false; 
    if (rnto_p != null)
      return false; 
    rnfr_p.put("name", Common.last(rnto));
    rnfr_p.put("url", "MEMORY://" + rnto);
    ram.put(rnto, ram.remove(rnfr));
    if (!Common.all_but_last(rnfr).equals(Common.all_but_last(rnto))) {
      String parent_path = Common.all_but_last(rnfr);
      parent_path = parent_path.substring(0, parent_path.length() - 1);
      Properties parent_item = (Properties)ram.get(parent_path);
      Vector vector = (Vector)parent_item.get("list");
      vector.remove(rnfr_p);
      parent_path = Common.all_but_last(rnto);
      parent_path = parent_path.substring(0, parent_path.length() - 1);
      parent_item = (Properties)ram.get(parent_path);
      vector = (Vector)parent_item.get("list");
      vector.addElement(rnfr_p);
    } 
    Vector v = (Vector)rnfr_p.get("list");
    if (v != null)
      synchronized (ram) {
        for (int x = 0; x < v.size(); x++) {
          Properties item2 = v.elementAt(x);
          rename(String.valueOf(rnfr) + "/" + item2.getProperty("name"), String.valueOf(rnto) + "/" + item2.getProperty("name"));
        } 
      }  
    return true;
  }
  
  protected OutputStream upload3(String path0, long startPos, boolean truncate, boolean binary) throws Exception {
    String path = String.valueOf(this.url) + path0.substring(1);
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties item = (Properties)ram.get(path);
    if (item != null && item.getProperty("type").equalsIgnoreCase("DIR"))
      throw new Exception("Can't overwrite memory dir with file."); 
    delete(path0);
    synchronized (ram) {
      String name = Common.last(path);
      Properties dir_item = new Properties();
      String parent_path = Common.all_but_last(path);
      parent_path = parent_path.substring(0, parent_path.length() - 1);
      Properties parent_item = (Properties)ram.get(parent_path);
      Vector v = (Vector)parent_item.get("list");
      v.addElement(dir_item);
      dir_item.put("name", name);
      dir_item.put("size", "0");
      dir_item.put("type", "FILE");
      dir_item.put("permissions", "-rwxrwxrwx");
      dir_item.put("size", "0");
      dir_item.put("url", "MEMORY://" + path);
      dir_item.put("link", "false");
      dir_item.put("num_items", "1");
      dir_item.put("owner", "user");
      dir_item.put("group", "group");
      dir_item.put("protocol", "memory");
      dir_item.put("root_dir", Common.all_but_last(path));
      Date itemDate = new Date();
      dir_item.put("modified", (new StringBuffer(String.valueOf(itemDate.getTime()))).toString());
      dir_item.put("month", months[Integer.parseInt(this.mm.format(itemDate))]);
      dir_item.put("day", this.dd.format(itemDate));
      String time_or_year = this.hhmm.format(itemDate);
      if (!this.yyyy.format(itemDate).equals(this.yyyy.format(new Date())) || System.getProperty("crushftp.ls.year", "false").equals("true"))
        time_or_year = this.yyyy.format(itemDate); 
      dir_item.put("time_or_year", time_or_year);
      this.out = new MemoryClient$1$ByteArrayOutputStreamWrapper(this, dir_item);
      dir_item.put("object", this.out);
      ram.put(path, dir_item);
    } 
    return this.out;
  }
  
  public boolean delete(String path) {
    path = String.valueOf(this.url) + path.substring(1);
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties item = (Properties)ram.get(path);
    if (item == null)
      return false; 
    String parent_path = Common.all_but_last(path);
    parent_path = parent_path.substring(0, parent_path.length() - 1);
    Properties parent_item = (Properties)ram.get(parent_path);
    Vector v = (Vector)parent_item.get("list");
    v.remove(item);
    v = (Vector)item.get("list");
    if (v != null)
      synchronized (ram) {
        for (int x = 0; x < v.size(); x++) {
          Properties item2 = v.elementAt(x);
          delete(String.valueOf(path) + "/" + item2.getProperty("name"));
        } 
      }  
    ram.remove(path);
    return (item != null);
  }
  
  public boolean makedir(String path0) {
    String path = String.valueOf(this.url) + path0.substring(1);
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties item = (Properties)ram.get(path);
    if (item != null)
      return false; 
    String name = Common.last(path);
    Properties dir_item = new Properties();
    dir_item.put("name", name);
    dir_item.put("size", "0");
    dir_item.put("type", "DIR");
    dir_item.put("permissions", "drwxrwxrwx");
    dir_item.put("size", "0");
    dir_item.put("url", "MEMORY://" + path + "/");
    dir_item.put("link", "false");
    dir_item.put("num_items", "1");
    dir_item.put("owner", "user");
    dir_item.put("group", "group");
    dir_item.put("protocol", "memory");
    dir_item.put("root_dir", Common.all_but_last(path));
    Date itemDate = new Date();
    dir_item.put("modified", (new StringBuffer(String.valueOf(itemDate.getTime()))).toString());
    dir_item.put("month", months[Integer.parseInt(this.mm.format(itemDate))]);
    dir_item.put("day", this.dd.format(itemDate));
    dir_item.put("list", new Vector());
    String time_or_year = this.hhmm.format(itemDate);
    if (!this.yyyy.format(itemDate).equals(this.yyyy.format(new Date())) || System.getProperty("crushftp.ls.year", "false").equals("true"))
      time_or_year = this.yyyy.format(itemDate); 
    dir_item.put("time_or_year", time_or_year);
    String parent_path = Common.all_but_last(path);
    parent_path = parent_path.substring(0, parent_path.length() - 1);
    Properties parent_item = (Properties)ram.get(parent_path);
    Vector v = (Vector)parent_item.get("list");
    v.addElement(dir_item);
    ram.put(path, dir_item);
    return true;
  }
  
  public boolean makedirs(String path0) throws Exception {
    String path = this.url;
    String[] parts = path0.split("/");
    boolean ok = false;
    for (int x = 0; x < parts.length; x++) {
      if (!parts[x].trim().equals("")) {
        path = String.valueOf(path) + parts[x] + "/";
        ok |= makedir(path);
      } 
    } 
    return ok;
  }
  
  public void setMod(String path, String val, String param) {
    path = String.valueOf(this.url) + path.substring(1);
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties item = (Properties)ram.get(path);
    if (val != null)
      item.put("mod", val); 
  }
  
  public void setOwner(String path, String val, String param) {
    path = String.valueOf(this.url) + path.substring(1);
    Properties item = (Properties)ram.get(path);
    if (val != null)
      item.put("owner", val); 
  }
  
  public void setGroup(String path, String val, String param) {
    path = String.valueOf(this.url) + path.substring(1);
    Properties item = (Properties)ram.get(path);
    if (val != null)
      item.put("group", val); 
  }
  
  public void doOSCommand(String app, String param, String val, String path) {}
  
  public String doCommand(String command) throws Exception {
    return "";
  }
}
