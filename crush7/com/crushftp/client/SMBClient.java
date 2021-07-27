package com.crushftp.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import jcifs.Config;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbRandomAccessFile;

public class SMBClient extends GenericClient {
  int lsBytesRead = 0;
  
  Properties dirCache = new Properties();
  
  NtlmPasswordAuthentication auth = null;
  
  public SMBClient(String url, String header, Vector log) {
    super(header, log);
    System.setProperty("jcifs.resolveOrder", "DNS");
    if (url.indexOf("@\\") >= 0)
      url = url.replace('\\', '/'); 
    if (url.indexOf("@//") >= 0)
      url = Common.replace_str(url, "@//", "@"); 
    if (url.toUpperCase().startsWith("SMB:/") && !url.toUpperCase().startsWith("SMB://"))
      url = "SMB://" + url.substring(5); 
    this.url = url;
    this.config.put("protocol", "SMB");
    Enumeration keys = System.getProperties().keys();
    while (keys.hasMoreElements()) {
      String key = (String)keys.nextElement();
      if (key.startsWith("jcifs.")) {
        String val = System.getProperty(key, "");
        Config.setProperty(key, val);
      } 
    } 
  }
  
  public String simpleUrl(String s) {
    if (s.indexOf("@") >= 0)
      s = "SMB://" + s.substring(s.indexOf("@") + 1); 
    return s;
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    String domain = null;
    username = VRL.vrlDecode(username);
    password = VRL.vrlDecode(password);
    if (username.indexOf("\\") >= 0) {
      domain = username.substring(0, username.indexOf("\\"));
      username = username.substring(username.indexOf("\\") + 1);
    } 
    this.config.put("username", username);
    this.config.put("password", password);
    if (domain != null)
      this.config.put("domain", domain); 
    if (clientid != null)
      this.config.put("clientid", clientid); 
    if (domain == null && this.config.containsKey("domain"))
      domain = this.config.getProperty("domain"); 
    this.auth = new NtlmPasswordAuthentication(domain, username, password);
    try {
      (new SmbFile(this.url, this.auth)).connect();
      return "";
    } catch (Exception e) {
      throw new Exception("login failed:" + e);
    } 
  }
  
  private String getAbsolutePath(SmbFile f) {
    return Common.machine_is_windows() ? ("/" + f.getPath().replace('\\', '/')) : f.getPath();
  }
  
  public Properties stat(String path) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    SmbFile test = new SmbFile(path, this.auth);
    if (!test.exists() && System.getProperty("crushftp.isTestCall", "false").equals("true"))
      throw new Exception("Item not found..."); 
    if (!test.exists())
      return null; 
    return stat(test, path);
  }
  
  public Properties stat(SmbFile test, String path) throws Exception {
    Properties dir_item = new Properties();
    String name = test.getName();
    if (test.isDirectory() && name.endsWith("/"))
      name = name.substring(0, name.length() - 1); 
    dir_item.put("name", name.replaceAll("\r", "%0A").replaceAll("\n", "%0D"));
    dir_item.put("size", "0");
    if (test.isDirectory()) {
      if (!path.endsWith("/")) {
        path = String.valueOf(path) + "/";
        test = new SmbFile(path, this.auth);
      } 
      test = new SmbFile(path, this.auth);
      dir_item.put("type", "DIR");
      dir_item.put("permissions", "drwxrwxrwx");
      dir_item.put("size", "1");
      if (this.config.getProperty("count_dir_items", "false").equals("true")) {
        int i = 0;
        SmbFile[] list = test.listFiles();
        for (int x = 0; list != null && x < list.length; x++) {
          if (!list[x].getName().startsWith("."))
            i++; 
        } 
        dir_item.put("size", (new StringBuffer(String.valueOf(i))).toString());
      } 
      if (!path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
    } else if (test.isFile()) {
      dir_item.put("type", "FILE");
      dir_item.put("permissions", "-rwxrwxrwx");
      dir_item.put("size", (new StringBuffer(String.valueOf(getSize(test)))).toString());
    } 
    dir_item.put("url", test.getURL().toExternalForm());
    dir_item.put("link", "false");
    if (Common.isSymbolicLink(getAbsolutePath(test)))
      dir_item.put("link", "true"); 
    dir_item.put("num_items", "1");
    dir_item.put("owner", "user");
    dir_item.put("group", "group");
    dir_item.put("protocol", "file");
    dir_item.put("root_dir", Common.all_but_last((new VRL(dir_item.getProperty("url"))).getPath()));
    setFileDateInfo(test, dir_item);
    return dir_item;
  }
  
  private long getSize(SmbFile test) throws SmbException {
    if (this.config.getProperty("checkEncryptedHeader", "false").equals("true"))
      return Common.getFileSize(test.getPath()); 
    return test.length();
  }
  
  public Vector list(String path, Vector list) throws Exception {
    if (!path.replace('\\', '/').endsWith("/"))
      path = String.valueOf(path) + "/"; 
    String originalPath = path;
    path = String.valueOf(this.url) + path.substring(1);
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    Common.log("FILE_CLIENT", 2, "Getting list for:" + path);
    SmbFile item = new SmbFile(path, this.auth);
    SmbFile[] items = item.listFiles();
    if ((getAbsolutePath(item).toLowerCase().endsWith(".zip") || getAbsolutePath(item).toLowerCase().indexOf(".zip/") >= 0) && path.endsWith("/")) {
      int pos = getAbsolutePath(item).toLowerCase().indexOf(".zip");
      ZipClient zc = new ZipClient(getAbsolutePath(item).substring(0, pos + 4), this.logHeader, this.logQueue);
      String addOn = "";
      if (getAbsolutePath(item).toLowerCase().indexOf(".zip/") >= 0) {
        pos++;
        addOn = "/";
      } 
      zc.list("!" + getAbsolutePath(item).substring(pos + 4) + addOn, list);
      items = (SmbFile[])null;
    } else if (item.isFile()) {
      items = new SmbFile[1];
      items[0] = item;
    } 
    if (items == null)
      items = new SmbFile[0]; 
    this.dirCache.clear();
    for (int x = 0; x < items.length; x++) {
      SmbFile test = items[x];
      try {
        String tempName = test.getName();
        if (test.isDirectory() && tempName.endsWith("/"))
          tempName = tempName.substring(0, tempName.length() - 1); 
        if (Common.machine_is_windows() && path.equals("/"))
          tempName = items[x].getPath().substring(0, 2); 
        String tempPath = String.valueOf(originalPath) + tempName;
        tempPath = String.valueOf(this.url) + tempPath.substring(1);
        Properties dir_item = stat(test, tempPath);
        if (dir_item.getProperty("type").equalsIgnoreCase("FILE"))
          dir_item.put("size", (new StringBuffer(String.valueOf(getSize(test)))).toString()); 
        list.add(dir_item);
      } catch (Exception e) {
        Common.log("FILE_CLIENT", 1, String.valueOf(x) + " of " + items.length + ":Invalid file, or dead alias:" + test);
        Common.log("FILE_CLIENT", 1, e);
      } 
    } 
    this.dirCache.clear();
    return list;
  }
  
  private void setFileDateInfo(SmbFile test, Properties dir_item) throws SmbException {
    Date itemDate = new Date(test.lastModified());
    dir_item.put("modified", (new StringBuffer(String.valueOf(itemDate.getTime()))).toString());
    dir_item.put("month", months[Integer.parseInt(this.mm.format(itemDate))]);
    dir_item.put("day", this.dd.format(itemDate));
    String time_or_year = this.hhmm.format(itemDate);
    if (!this.yyyy.format(itemDate).equals(this.yyyy.format(new Date())) || System.getProperty("crushftp.ls.year", "false").equals("true"))
      time_or_year = this.yyyy.format(itemDate); 
    dir_item.put("time_or_year", time_or_year);
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    InputStream inputStream;
    path = String.valueOf(this.url) + path.substring(1);
    SmbFileInputStream smbFileInputStream = new SmbFileInputStream(new SmbFile(path, this.auth));
    try {
      if (startPos > 0L)
        smbFileInputStream.skip(startPos); 
    } catch (Exception e) {
      smbFileInputStream.close();
      throw e;
    } 
    if (endPos > 0L)
      inputStream = getLimitedInputStream((InputStream)smbFileInputStream, startPos, endPos); 
    this.in = inputStream;
    return this.in;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    (new SmbFile(path, this.auth)).setLastModified(modified);
    return true;
  }
  
  public boolean rename(String rnfr, String rnto) {
    rnfr = String.valueOf(this.url) + rnfr.substring(1);
    rnto = String.valueOf(this.url) + rnto.substring(1);
    try {
      SmbFile f2 = new SmbFile(rnto, this.auth);
      if (f2.exists())
        return false; 
      SmbFile f1 = new SmbFile(rnfr, this.auth);
      boolean f1_exists1 = f1.exists();
      boolean f2_exists1 = f2.exists();
      f1.renameTo(f2);
      if (!f2_exists1 && f2.exists())
        return true; 
      if (f1_exists1 && !f1.exists() && f2.exists())
        return true; 
    } catch (Exception e) {
      log(e);
    } 
    return false;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    Common.log("FILE_CLIENT", 2, "Uploading:" + path);
    SmbFile smb = new SmbFile(path, this.auth);
    try {
      if (smb.exists() && truncate)
        smb.delete(); 
    } catch (Exception exception) {}
    SmbRandomOutputStream fout = new SmbRandomOutputStream(smb, false);
    if (truncate && startPos > 0L && startPos != smb.length())
      fout.setLength(startPos); 
    if (startPos > 0L)
      fout.seek(startPos); 
    this.out = fout;
    return fout;
  }
  
  public boolean delete(String path) {
    path = String.valueOf(this.url) + path.substring(1);
    try {
      (new SmbFile(path, this.auth)).delete();
    } catch (Exception e) {
      if (e.indexOf("find the file") < 0)
        log(e); 
      return false;
    } 
    return true;
  }
  
  public boolean makedir(String path) {
    path = String.valueOf(this.url) + path.substring(1);
    try {
      SmbFile f = new SmbFile(path, this.auth);
      if (!f.exists())
        f.mkdir(); 
    } catch (Exception e) {
      log(e);
      return false;
    } 
    return true;
  }
  
  public boolean makedirs(String path) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    try {
      SmbFile f = new SmbFile(path, this.auth);
      if (!f.exists())
        f.mkdirs(); 
    } catch (Exception e) {
      log(e);
      return false;
    } 
    return true;
  }
  
  public String doCommand(String command) throws Exception {
    if (command.startsWith("SITE PGP_HEADER_SIZE")) {
      command = command.substring(command.indexOf(" ") + 1);
      command = command.substring(command.indexOf(" ") + 1);
      long size = Long.parseLong(command.substring(0, command.indexOf(" ")).trim());
      command = command.substring(command.indexOf(" ") + 1);
      if (command.startsWith("/"))
        command = command.substring(1); 
      String path = String.valueOf(this.url) + command.trim();
      SmbRandomAccessFile fout = new SmbRandomAccessFile(new SmbFile(path, this.auth), "rw");
      int offset = "-----BEGIN PGP MESSAGE-----\r\nCRUSHFTP#".length() + 10;
      fout.seek(offset);
      fout.write((new StringBuffer(String.valueOf(size))).toString().getBytes("UTF8"));
      fout.close();
      return "214 OK";
    } 
    return "";
  }
}
