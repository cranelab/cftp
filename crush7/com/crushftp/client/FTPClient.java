package com.crushftp.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class FTPClient extends GenericClient {
  InputStream in2 = null;
  
  OutputStream out2 = null;
  
  Socket sock = null;
  
  BufferedReader is = null;
  
  BufferedWriter os = null;
  
  static int curPort = -1;
  
  static Object activePortLock = new Object();
  
  SimpleDateFormat msdf = new SimpleDateFormat("MM-dd-yyyy hh:mmaa", Locale.US);
  
  SimpleDateFormat msdf2 = new SimpleDateFormat("MM-dd-yy hh:mm:ssaa", Locale.US);
  
  Vector recent_mkdirs = new Vector();
  
  SSLContext factory = null;
  
  SSLSocketFactory factory_ssl = null;
  
  static Properties stat_cache = new Properties();
  
  static Class class$0;
  
  public FTPClient(String url, String header, Vector log) {
    super(header, log);
    this.url = url;
    if (url.toUpperCase().startsWith("FTPES:")) {
      this.config.put("secure", "true");
    } else if (url.toUpperCase().startsWith("FTPS:")) {
      this.config.put("implicit", "true");
    } 
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.config.put("username", username);
    this.config.put("password", password);
    if (clientid != null)
      this.config.put("clientid", clientid); 
    String userMessage = "";
    VRL u = new VRL(this.url);
    log("Connecting to:" + u.getHost() + ":" + u.getPort());
    this.sock = Common.getSocket("FTP", u, this.config.getProperty("use_dmz", "false"), "", Integer.parseInt(this.config.getProperty("timeout", "30000")));
    Socket sockOriginal = this.sock;
    if (this.config.getProperty("implicit", "false").equals("true") || (this.config.getProperty("secure", "false").equals("true") && !this.config.getProperty("implicit", "false").equals("true")))
      log(this.in + ":using SSL parameters:client cert keystore=" + this.config.getProperty("keystore_path") + ", truststore=" + this.config.getProperty("trustore_path", "builtin") + ", clientAuth=" + (this.config.getProperty("keystore_path", "").equals("") ? 0 : 1) + ", trustAll=" + this.config.getProperty("acceptAnyCert", "true")); 
    if (this.config.getProperty("implicit", "false").equals("true")) {
      if (this.factory == null)
        this.factory = (new Common()).getSSLContext(!this.config.getProperty("keystore_path", "").equals("") ? this.config.getProperty("keystore_path") : "builtin", this.config.getProperty("trustore_path", "builtin"), Common.encryptDecrypt(this.config.getProperty("keystore_pass"), false), Common.encryptDecrypt(this.config.getProperty("key_pass"), false), "TLS", !this.config.getProperty("keystore_path", "").equals(""), this.config.getProperty("acceptAnyCert", "true").equalsIgnoreCase("true")); 
      if (this.factory_ssl == null)
        this.factory_ssl = this.factory.getSocketFactory(); 
      SSLSocket ss = (SSLSocket)this.factory_ssl.createSocket(this.sock, u.getHost(), u.getPort(), true);
      Common.configureSSLTLSSocket(ss, System.getProperty("crushftp.tls_version_client", "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2"));
      Common.setEnabledCiphers(this.config.getProperty("disabled_ciphers", ""), ss, null);
      ss.setUseClientMode(true);
      ss.startHandshake();
      this.sock = ss;
    } 
    this.os = new BufferedWriter(new OutputStreamWriter(this.sock.getOutputStream(), "UTF8"));
    String server_announce = send_data_raw(220, "", this.sock.getInputStream());
    if ((this.config.getProperty("secure", "false").equals("true") || u.getProtocol().equalsIgnoreCase("FTPES")) && !this.config.getProperty("implicit", "false").equals("true")) {
      send_data_raw(234, "AUTH " + this.config.getProperty("secure_mode", "TLS").toUpperCase(), this.sock.getInputStream());
      if (this.factory == null)
        this.factory = (new Common()).getSSLContext(!this.config.getProperty("keystore_path", "").equals("") ? this.config.getProperty("keystore_path") : "builtin", this.config.getProperty("trustore_path", "builtin"), Common.encryptDecrypt(this.config.getProperty("keystore_pass"), false), Common.encryptDecrypt(this.config.getProperty("key_pass"), false), "TLS", !this.config.getProperty("keystore_path", "").equals(""), this.config.getProperty("acceptAnyCert", "true").equalsIgnoreCase("true")); 
      if (this.factory_ssl == null)
        this.factory_ssl = this.factory.getSocketFactory(); 
      SSLSocket ss = (SSLSocket)this.factory_ssl.createSocket(this.sock, u.getHost(), (u.getPort() == -1) ? 21 : u.getPort(), false);
      Common.configureSSLTLSSocket(ss, System.getProperty("crushftp.tls_version_client", "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2"));
      Common.setEnabledCiphers(this.config.getProperty("disabled_ciphers", ""), ss, null);
      ss.startHandshake();
      this.sock = ss;
    } 
    this.os = new BufferedWriter(new OutputStreamWriter(this.sock.getOutputStream(), "UTF8"));
    executeScript(this.config.getProperty("before_login_script", ""), "");
    try {
      String result = send_data_raw(331, "USER " + username, this.sock.getInputStream());
      if (result == null || result.indexOf("230 ") < 0) {
        if (result != null && result.startsWith("5"))
          throw new IOException(result); 
        try {
          userMessage = send_data_raw(230, "PASS " + password, this.sock.getInputStream());
        } catch (Exception e) {
          if (e.toString().indexOf("332 ") >= 0) {
            userMessage = send_data_raw(230, "ACCT " + this.config.getProperty("account", ""), this.sock.getInputStream());
          } else {
            throw e;
          } 
        } 
      } 
    } catch (Exception e) {
      try {
        send_data_raw(221, "QUIT", this.sock.getInputStream());
      } catch (Exception exception) {}
      if (this.is != null)
        this.is.close(); 
      if (this.os != null)
        this.os.close(); 
      if (this.sock != null)
        this.sock.close(); 
      this.is = null;
      this.os = null;
      throw e;
    } 
    if (this.config.getProperty("secure", "false").equals("true") || this.config.getProperty("implicit", "false").equals("true"))
      send_data_raw(-1, "PBSZ 0", this.sock.getInputStream()); 
    if ((this.config.getProperty("secure", "false").equals("true") || this.config.getProperty("implicit", "false").equals("true")) && this.config.getProperty("secure_data", "true").equals("true")) {
      send_data_raw(-1, "PROT P", this.sock.getInputStream());
    } else if ((this.config.getProperty("secure", "false").equals("true") || this.config.getProperty("implicit", "false").equals("true")) && this.config.getProperty("secure_data", "true").equals("false")) {
      send_data_raw(-1, "PROT C", this.sock.getInputStream());
    } 
    if (this.config.getProperty("ccc", "false").equals("true")) {
      try {
        send_data_raw(200, "CCC", this.sock.getInputStream());
        if (this.config.getProperty("allow_ccc_ssl_close", "false").equals("true")) {
          Thread t = new Thread(new Runnable(this) {
                final FTPClient this$0;
                
                public void run() {
                  try {
                    this.this$0.sock.close();
                  } catch (Exception exception) {}
                }
              });
          t.start();
          t.join(3000L);
          if (t.isAlive())
            t.interrupt(); 
        } 
        this.is = new BufferedReader(new InputStreamReader(sockOriginal.getInputStream(), "UTF8"));
        this.os = new BufferedWriter(new OutputStreamWriter(sockOriginal.getOutputStream(), "UTF8"));
      } catch (Exception exception) {}
    } else {
      this.is = new BufferedReader(new InputStreamReader(this.sock.getInputStream(), "UTF8"));
    } 
    if (this.config.getProperty("implicit", "false").equals("true"))
      setConfig("secure", "true"); 
    setConfig("userMessage", userMessage);
    try {
      send_data(-1, "PWD").toString();
      this.config.put("server_type", send_data(215, "SYST").toString().toUpperCase());
    } catch (Exception e) {
      log(e);
      this.config.put("server_type", "UNKNOWN:" + e);
      String data2 = send_data(-1, "SYST").toString();
      if (data2.startsWith("25"))
        this.config.put("server_type", send_data(215, "").toString().toUpperCase()); 
    } 
    if (this.config.getProperty("server_type").indexOf("OS/400") >= 0)
      send_data(-1, "SITE NAMEFMT 1").toString(); 
    if (this.config.getProperty("server_type").indexOf("NONSTOP") >= 0) {
      this.config.put("cwd_list", "true");
      this.config.put("no_stat", "true");
      this.config.put("no_mkd", "true");
      this.config.put("no_mdtm", "true");
      this.config.put("ascii", "true");
    } 
    if (this.config.getProperty("server_type").indexOf("TYPSOFT") >= 0 || server_announce.toUpperCase().indexOf("TYPSOFT") >= 0) {
      this.config.put("cwd_list", "true");
      this.config.put("no_stat", "true");
      this.config.put("no_mkd", "true");
      this.config.put("no_mdtm", "true");
    } 
    if (this.config.getProperty("server_type").indexOf("WINDOW") >= 0)
      this.config.put("no_stat", "true"); 
    this.config.put("default_dir", "/");
    String pwdStr = send_data(-1, "PWD").toString();
    this.config.put("default_pwd", pwdStr);
    if (pwdStr != null && pwdStr.indexOf("\"") >= 0 && pwdStr.indexOf("\"") < pwdStr.lastIndexOf("\"")) {
      String defaultDir = pwdStr.substring(pwdStr.indexOf("\"") + 1);
      defaultDir = defaultDir.substring(0, defaultDir.indexOf("\""));
      if (!defaultDir.startsWith("/"))
        defaultDir = "/" + defaultDir; 
      if (!defaultDir.endsWith("/"))
        defaultDir = String.valueOf(defaultDir) + "/"; 
      if (this.config.getProperty("server_type").indexOf("OS/400") >= 0)
        defaultDir = "/"; 
      this.config.put("default_dir", defaultDir);
    } 
    executeScript(this.config.getProperty("after_login_script", ""), "");
    return "";
  }
  
  public void logout() throws Exception {
    try {
      close();
    } catch (Exception e) {
      if (this.in2 != null)
        this.in2.close(); 
      if (this.out2 != null)
        this.out2.close(); 
    } 
    executeScript(this.config.getProperty("before_logout_script", ""), "");
    try {
      send_data(221, "QUIT");
    } catch (Exception e) {
      Common.log("FTP_CLIENT", 1, e);
    } 
  }
  
  public boolean delete(String path) throws Exception {
    this.recent_mkdirs.removeAllElements();
    if (this.config.getProperty("simple", "false").equals("true") && path.indexOf("/") >= 0)
      path = Common.last(path); 
    try {
      send_data(250, "DELE " + path);
    } catch (Exception e1) {
      if (e1.indexOf("550") >= 0)
        try {
          send_data(250, "RMD " + path);
        } catch (Exception e2) {
          if (e2.indexOf("not empty") >= 0) {
            Common.log("FTP_CLIENT", 1, e2);
            return false;
          } 
          throw e1;
        }  
    } 
    return true;
  }
  
  public boolean makedir(String path) throws Exception {
    if (this.config.getProperty("no_mkd", "false").equals("true") || this.config.getProperty("simple", "false").equals("true"))
      return true; 
    if (this.config.getProperty("simple", "false").equals("true") && path.indexOf("/") >= 0)
      path = Common.last(path); 
    try {
      if (path.endsWith("/"))
        path = path.substring(0, path.length() - 1); 
      send_data(257, "MKD " + path);
    } catch (IOException e) {
      if ((e.toString().indexOf("550") >= 0 || e.toString().indexOf("521") >= 0) && e.toString().toLowerCase().indexOf("exists") >= 0)
        return true; 
      throw e;
    } 
    return true;
  }
  
  public String quote(String command) throws Exception {
    return send_data(-1, command.trim());
  }
  
  public boolean makedirs(String path) throws Exception {
    boolean ok = false;
    try {
      ok = makedir(path);
    } catch (Exception e) {
      Common.log("FTP_CLIENT", 1, "MKDIR recursive failed:" + path + " so we will try recursive. (" + e + ")");
      String[] parts = path.split("/");
      String path2 = "";
      for (int x = 0; x < parts.length; x++) {
        path2 = String.valueOf(path2) + parts[x] + "/";
        if (x >= 1)
          if (this.recent_mkdirs.indexOf(path2) < 0) {
            this.recent_mkdirs.addElement(path2);
            if (stat(path2) == null) {
              try {
                ok = makedir(path2);
              } catch (Exception ee) {
                Common.log("FTP_CLIENT", 1, "MKDIR individual:" + path2 + " failed, moving to next. (" + ee + ")");
              } 
            } else {
              ok = true;
            } 
          }  
      } 
    } 
    return ok;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    if (this.config.getProperty("no_mdtm", "false").equals("true") || this.config.getProperty("simple", "false").equals("true"))
      return false; 
    boolean ok = this.config.getProperty("mfmt_ok", "true").equalsIgnoreCase("true");
    SimpleDateFormat sdf_yyyyMMddHHmmss2 = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    try {
      if (ok) {
        send_data(213, "MFMT " + sdf_yyyyMMddHHmmss2.format(new Date(modified)) + " " + path);
        ok = true;
      } 
    } catch (Exception e) {
      ok = false;
      setConfig("mfmt_ok", "false");
    } 
    if (!ok)
      send_data(213, "MDTM " + sdf_yyyyMMddHHmmss2.format(new Date(modified)) + " " + path); 
    return true;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    this.recent_mkdirs.removeAllElements();
    if (this.config.getProperty("simple", "false").equals("true") && rnfr.indexOf("/") >= 0)
      rnfr = Common.last(rnfr); 
    if (this.config.getProperty("simple", "false").equals("true") && rnto.indexOf("/") >= 0)
      rnto = Common.last(rnto); 
    send_data(350, "RNFR " + rnfr);
    send_data(250, "RNTO " + rnto);
    return true;
  }
  
  public Properties stat(String path) throws Exception {
    if (this.config.getProperty("server_type").indexOf("OS/400") >= 0 && this.config.getProperty("no_os400", "false").equalsIgnoreCase("false")) {
      String new_path = "/";
      for (int x = 0; x < (path.split("\\/")).length; x++) {
        String s = path.split("\\/")[x];
        if (s.equals(""))
          continue; 
        if (s.indexOf(".") >= 0 && x == 3)
          if (s.indexOf(".FILE") < 0 && s.indexOf(".MBR") < 0 && s.indexOf(".LIB") < 0) {
            s = String.valueOf(Common.replace_str(s, ".", ".FILE/")) + ".MBR";
            new_path = String.valueOf(new_path) + s;
            continue;
          }  
        if (x < 3 && !s.toUpperCase().endsWith(".LIB"))
          s = String.valueOf(s) + ".LIB"; 
        if (x == 3 && !s.toUpperCase().endsWith(".FILE"))
          s = String.valueOf(s) + ".FILE"; 
        if (x == 4 && !s.toUpperCase().endsWith(".MBR"))
          s = String.valueOf(s) + ".MBR"; 
        new_path = String.valueOf(new_path) + s;
        if (x < (path.split("\\/")).length - 1)
          new_path = String.valueOf(new_path) + "/"; 
        continue;
      } 
      path = new_path;
      boolean file = (!Common.last(path).toUpperCase().endsWith(".LIB") && !Common.last(path).toUpperCase().endsWith(".LIB/"));
      Properties properties = new Properties();
      properties.put("name", Common.last(path));
      properties.put("size", file ? "0" : "1");
      properties.put("modified", "0");
      properties.put("type", file ? "FILE" : "DIR");
      properties.put("root_dir", "/");
      properties.put("url", String.valueOf(this.url) + path.substring(1) + (file ? "" : "/"));
      return properties;
    } 
    if (this.config.getProperty("simple", "false").equals("true")) {
      boolean file = (Common.last(path).indexOf(".") > 0);
      Properties properties = new Properties();
      properties.put("name", Common.last(path));
      properties.put("size", file ? "0" : "1");
      properties.put("modified", "0");
      properties.put("type", file ? "FILE" : "DIR");
      properties.put("root_dir", "/");
      properties.put("url", String.valueOf(this.url) + path.substring(1) + (file ? "" : "/"));
      return properties;
    } 
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Properties dir_item = null;
    Vector list = new Vector();
    String searchPath = Common.all_but_last(path);
    if (searchPath.equals(""))
      searchPath = "/"; 
    try {
      String result = "";
      if (this.config.getProperty("stat_cache", "false").equals("true"))
        if (stat_cache.containsKey(String.valueOf(this.url) + path.substring(1)))
          return (Properties)stat_cache.get(String.valueOf(this.url) + path.substring(1));  
      if (this.config.getProperty("star_stat", "false").equals("true")) {
        result = send_data_now("STAT " + path + "*", true);
      } else if (this.config.getProperty("no_stat", "false").equals("false")) {
        result = send_data_now("STAT " + path, true);
      } 
      if (result.startsWith("5") && result.toUpperCase().indexOf("RECOGNIZE") >= 0) {
        result = "";
        this.config.put("no_stat", "true");
      } 
      if (result.toUpperCase().startsWith("211-STATUS FOR USER")) {
        result = "";
        this.config.put("no_stat", "true");
      } 
      BufferedReader br = new BufferedReader(new StringReader(result));
      String data = "";
      Vector list2 = new Vector();
      int line_num = 0;
      SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss");
      while ((data = br.readLine()) != null) {
        line_num++;
        if ((data.startsWith("2") && line_num == 1) || data.trim().equals(""))
          continue; 
        if (data.startsWith("550")) {
          if (data.toUpperCase().indexOf("RECOGNIZE") >= 0) {
            this.config.put("no_stat", "true");
            continue;
          } 
          if (System.getProperty("crushftp.isTestCall", "false").equals("true"))
            throw new Exception("Item not found..." + path); 
          return null;
        } 
        Properties dir_item2 = new Properties();
        dir_item2.put("root_dir", Common.all_but_last(path));
        dir_item2.put("local", "false");
        dir_item2.put("protocol", "ftp");
        if (data.startsWith("211-"))
          data = data.substring("211-".length()); 
        if (data.toUpperCase().indexOf("TYPE=") >= 0) {
          dir_item2.put("size", "0");
          dir_item2.put("num_items", "1");
          dir_item2.put("owner", "owner");
          dir_item2.put("group", "group");
          dir_item2.put("proxy_item", "true");
          String[] parts = data.split(";");
          for (int x = 0; x < parts.length - 1; x++) {
            if (parts[x].split("=")[0].trim().equalsIgnoreCase("Type")) {
              if (parts[x].split("=")[1].trim().equalsIgnoreCase("dir")) {
                dir_item2.put("type", "DIR");
              } else {
                dir_item2.put("type", "FILE");
              } 
            } else if (parts[x].split("=")[0].trim().equalsIgnoreCase("UNIX.mode")) {
              String r = dir_item2.getProperty("type", "").equalsIgnoreCase("DIR") ? "d" : "-";
              r = String.valueOf(r) + numToStrPrivs(parts[x].split("=")[1].trim());
              dir_item2.put("permissions", r);
            } else if (parts[x].split("=")[0].trim().equalsIgnoreCase("Modify")) {
              Date d = yyyyMMddHHmmss.parse(parts[x].split("=")[1].trim());
              dir_item2.put("month", this.mmm.format(d));
              dir_item2.put("day", this.dd.format(d));
              dir_item2.put("time_or_year", this.yyyy.format(d));
              dir_item2.put("modified", (new StringBuffer(String.valueOf(d.getTime()))).toString());
            } else if (parts[x].split("=")[0].trim().equalsIgnoreCase("Size")) {
              dir_item2.put("size", parts[x].split("=")[1].trim());
            } else if (parts[x].split("=")[0].trim().equalsIgnoreCase("UNIX.Owner")) {
              dir_item2.put("owner", parts[x].split("=")[1].trim());
            } else if (parts[x].split("=")[0].trim().equalsIgnoreCase("UNIX.Group")) {
              dir_item2.put("group", parts[x].split("=")[1].trim());
            } 
          } 
          dir_item2.put("name", parts[parts.length - 1].substring(1));
          if (dir_item2.getProperty("name").startsWith("/"))
            dir_item2.put("name", parts[parts.length - 1].substring(2)); 
        } else {
          parse_unix_line(dir_item2, data, 7, Common.all_but_last(path), list2);
        } 
        dir_item2.put("url", String.valueOf(this.url) + path.substring(1));
        if (this.config.getProperty("no_os400", "false").equalsIgnoreCase("true"))
          dir_item2.put("no_os400", "true"); 
        Common.log("FTP_CLIENT", 2, "path=" + path + "  data:" + data + "   dir_item2:" + dir_item2);
        if (dir_item2.getProperty("name").equals(Common.last(path)))
          list.addAll(list2); 
      } 
      if (list2.size() == 1) {
        list.addAll(list2);
      } else if (list.size() == 0 && this.config.getProperty("star_stat", "false").equals("false")) {
        list(Common.all_but_last(path), list);
      } 
    } catch (Exception e) {
      Common.log("FTP_CLIENT", 2, e);
      if (System.getProperty("crushftp.isTestCall", "false").equals("true"))
        throw e; 
    } 
    if (dir_item == null)
      for (int x = 0; x < list.size(); x++) {
        Properties p = list.elementAt(x);
        if (p.getProperty("name", "").toUpperCase().equals(Common.last(path).toUpperCase())) {
          dir_item = p;
          break;
        } 
      }  
    addModifiedItem(dir_item);
    if (dir_item != null && dir_item.getProperty("type").equals("DIR") && !dir_item.getProperty("url").endsWith("/"))
      dir_item.put("url", String.valueOf(dir_item.getProperty("url", "")) + "/"); 
    if (dir_item != null)
      dir_item.put("pasv", this.config.getProperty("pasv", "true")); 
    return dir_item;
  }
  
  private String numToStrPrivs(String s) {
    String r = "";
    if (s.length() == 4)
      s = s.substring(1); 
    for (int loop = 0; loop < 3; loop++) {
      if (s.charAt(loop) == '0') {
        r = String.valueOf(r) + "---";
      } else if (s.charAt(loop) == '1') {
        r = String.valueOf(r) + "--x";
      } else if (s.charAt(loop) == '2') {
        r = String.valueOf(r) + "-w-";
      } else if (s.charAt(loop) == '3') {
        r = String.valueOf(r) + "-wx";
      } else if (s.charAt(loop) == '4') {
        r = String.valueOf(r) + "r--";
      } else if (s.charAt(loop) == '5') {
        r = String.valueOf(r) + "r-x";
      } else if (s.charAt(loop) == '6') {
        r = String.valueOf(r) + "rw-";
      } else if (s.charAt(loop) == '7') {
        r = String.valueOf(r) + "rwx";
      } 
    } 
    return r;
  }
  
  protected InputStream download3(String path1, long startPos, long endPos, boolean binary) throws Exception {
    if (this.config.getProperty("simple", "false").equals("true") && path1.indexOf("/") >= 0)
      path1 = Common.last(path1); 
    if (this.config.getProperty("server_type").indexOf("OS/400") >= 0 && this.config.getProperty("no_os400", "false").equalsIgnoreCase("false")) {
      String new_path = "/";
      for (int x = 0; x < (path1.split("\\/")).length; x++) {
        String s = path1.split("\\/")[x].toUpperCase();
        if (s.equals(""))
          continue; 
        if (s.indexOf(".") >= 0 && x == 3)
          if (s.indexOf(".FILE") < 0 && s.indexOf(".MBR") < 0 && s.indexOf(".LIB") < 0) {
            s = String.valueOf(Common.replace_str(s, ".", ".FILE/")) + ".MBR";
            new_path = String.valueOf(new_path) + s;
            continue;
          }  
        if (x < 3 && !s.toUpperCase().endsWith(".LIB"))
          s = String.valueOf(s) + ".LIB"; 
        if (x == 3 && !s.toUpperCase().endsWith(".FILE"))
          s = String.valueOf(s) + ".FILE"; 
        if (x == 4 && !s.toUpperCase().endsWith(".MBR"))
          s = String.valueOf(s) + ".MBR"; 
        new_path = String.valueOf(new_path) + s;
        if (x < (path1.split("\\/")).length - 1) {
          new_path = String.valueOf(new_path) + "/";
        } else if (s.indexOf(".") >= 0) {
          s = s.replace('.', '/');
        } 
        continue;
      } 
      path1 = new_path;
    } 
    String path2 = path1;
    executeScript(this.config.getProperty("before_download_script", ""), path2.trim());
    send_data(-1, "TYPE " + ((binary && this.config.getProperty("ascii", "false").equals("false")) ? "I" : "A"));
    if (startPos > 0L)
      send_data(-1, "REST " + startPos); 
    Socket transfer_socket = null;
    if (this.config.getProperty("pasv", "true").equals("true")) {
      transfer_socket = (Socket)getTransferSocket(true);
      send_data(150, "RETR " + path2.trim());
      if (this.config.getProperty("secure", "false").equals("true") && this.config.getProperty("secure_data", "true").equals("true"))
        ((SSLSocket)transfer_socket).setUseClientMode(true); 
    } else {
      ServerSocket ss = (ServerSocket)getTransferSocket(false);
      send_data(150, "RETR " + path2.trim());
      ss.setSoTimeout(Integer.parseInt(System.getProperty("crushftp.ftpcommand.timeout3", "120")) * 1000);
      transfer_socket = ss.accept();
      ss.close();
      if (this.config.getProperty("secure", "false").equals("true") && this.config.getProperty("secure_data", "true").equals("true"))
        ((SSLSocket)transfer_socket).setUseClientMode(true); 
      if (this.config.getProperty("secure", "false").equals("true") && this.config.getProperty("secure_data", "true").equals("true"))
        ((SSLSocket)transfer_socket).startHandshake(); 
    } 
    Properties byteCount = new Properties();
    setupTimeout(byteCount, transfer_socket);
    this.in2 = transfer_socket.getInputStream();
    this.in = new null.InputWrapper(this, this.in2, startPos, endPos, byteCount, path2);
    return this.in;
  }
  
  protected OutputStream upload3(String path1, long startPos, boolean truncate, boolean binary) throws Exception {
    if (this.config.getProperty("simple", "false").equals("true") && path1.indexOf("/") >= 0)
      path1 = Common.last(path1); 
    if (this.config.getProperty("server_type").indexOf("OS/400") >= 0 && this.config.getProperty("no_os400", "false").equalsIgnoreCase("false")) {
      String new_path = "/";
      for (int x = 0; x < (path1.split("\\/")).length; x++) {
        String s = path1.split("\\/")[x];
        if (!s.equals("")) {
          if (x < 3 && !s.toUpperCase().endsWith(".LIB"))
            s = String.valueOf(s) + ".LIB"; 
          if (x == 3 && !s.toUpperCase().endsWith(".FILE"))
            s = String.valueOf(s) + ".FILE"; 
          if (x == 4 && !s.toUpperCase().endsWith(".MBR")) {
            s = Common.replace_str(s, ".FILE", "");
            s = String.valueOf(s) + ".MBR";
          } 
          new_path = String.valueOf(new_path) + s;
          if (x < (path1.split("\\/")).length - 1)
            new_path = String.valueOf(new_path) + "/"; 
        } 
      } 
      path1 = new_path.toUpperCase();
    } 
    String path2 = path1;
    executeScript(this.config.getProperty("before_upload_script", ""), path2.trim());
    send_data(-1, "TYPE " + ((binary && this.config.getProperty("ascii", "false").equals("false")) ? "I" : "A"));
    String stor_command = "STOR";
    if (startPos > 0L)
      send_data(-1, "REST " + startPos); 
    if (startPos < -1L)
      stor_command = "APPE"; 
    Socket transfer_socket = null;
    if (this.config.getProperty("pasv", "true").equals("true")) {
      transfer_socket = (Socket)getTransferSocket(true);
      send_data(150, String.valueOf(stor_command) + " " + path2.trim());
      if (this.config.getProperty("secure", "false").equals("true") && this.config.getProperty("secure_data", "true").equals("true"))
        ((SSLSocket)transfer_socket).setUseClientMode(true); 
    } else {
      ServerSocket ss = (ServerSocket)getTransferSocket(false);
      send_data(150, String.valueOf(stor_command) + " " + path2.trim());
      ss.setSoTimeout(Integer.parseInt(System.getProperty("crushftp.ftpcommand.timeout3", "120")) * 1000);
      transfer_socket = ss.accept();
      ss.close();
      if (this.config.getProperty("secure", "false").equals("true") && this.config.getProperty("secure_data", "true").equals("true"))
        ((SSLSocket)transfer_socket).setUseClientMode(true); 
      if (this.config.getProperty("secure", "false").equals("true") && this.config.getProperty("secure_data", "true").equals("true"))
        ((SSLSocket)transfer_socket).startHandshake(); 
    } 
    Properties byteCount = new Properties();
    setupTimeout(byteCount, transfer_socket);
    this.out2 = transfer_socket.getOutputStream();
    this.out = new null.OutputWrapper(this, this.out2, transfer_socket, byteCount, path2);
    return this.out;
  }
  
  public Vector list(String path, Vector list) throws Exception {
    String search = "";
    if (path.indexOf(":") >= 0) {
      search = " " + path.split(":")[1];
      path = path.split(":")[0];
    } 
    if (path.equals("") || path.equals("."))
      path = ""; 
    executeScript(this.config.getProperty("before_dir_script", ""), path.trim());
    send_data(-1, "TYPE A");
    Socket transfer_socket = null;
    String listCommand = "LIST";
    if (this.config.getProperty("simple", "false").equals("true")) {
      listCommand = "NLST";
      path = "";
    } 
    if (!this.config.getProperty("listCommand", "").equals(""))
      listCommand = this.config.getProperty("listCommand", ""); 
    if (this.config.getProperty("pasv", "true").equals("true")) {
      if (this.config.getProperty("cwd_list", "false").equals("true") || this.config.getProperty("config_cwd_list", "false").equals("true")) {
        if (this.config.getProperty("server_type").indexOf("OS/400") >= 0 && this.config.getProperty("no_os400", "false").equalsIgnoreCase("false")) {
          String new_path = "/";
          for (int x = 0; x < (path.split("\\/")).length; x++) {
            String s = path.split("\\/")[x];
            if (!s.equals("")) {
              if (!s.toUpperCase().endsWith(".LIB"))
                s = String.valueOf(s) + ".LIB"; 
              new_path = String.valueOf(new_path) + s + "/";
            } 
          } 
          send_data(250, ("CWD " + new_path).trim());
        } else {
          send_data(250, ("CWD " + path).trim());
        } 
        transfer_socket = (Socket)getTransferSocket(true);
        send_data(150, String.valueOf(listCommand) + search);
      } else {
        transfer_socket = (Socket)getTransferSocket(true);
        if (!search.equals(""))
          search = search.substring(1); 
        send_data(150, (String.valueOf(listCommand) + " " + path + search).trim());
      } 
      if (this.config.getProperty("secure", "false").equals("true") && this.config.getProperty("secure_data", "true").equals("true"))
        ((SSLSocket)transfer_socket).setUseClientMode(true); 
    } else {
      if (this.config.getProperty("cwd_list", "false").equals("true") || this.config.getProperty("config_cwd_list", "false").equals("true")) {
        if (this.config.getProperty("server_type").indexOf("OS/400") >= 0 && this.config.getProperty("no_os400", "false").equalsIgnoreCase("false")) {
          String new_path = "/";
          for (int x = 0; x < (path.split("\\/")).length; x++) {
            String s = path.split("\\/")[x];
            if (!s.equals("")) {
              if (!s.toUpperCase().endsWith(".LIB"))
                s = String.valueOf(s) + ".LIB"; 
              new_path = String.valueOf(new_path) + s + "/";
            } 
          } 
          send_data(250, ("CWD " + new_path).trim());
        } else {
          send_data(250, ("CWD " + path).trim());
        } 
        ServerSocket ss = (ServerSocket)getTransferSocket(false);
        send_data(150, String.valueOf(listCommand) + search);
        ss.setSoTimeout(Integer.parseInt(System.getProperty("crushftp.ftpcommand.timeout2", "60")) * 1000);
        transfer_socket = ss.accept();
        ss.close();
      } else {
        ServerSocket ss = (ServerSocket)getTransferSocket(false);
        send_data(150, (String.valueOf(listCommand) + search + " " + path).trim());
        ss.setSoTimeout(Integer.parseInt(System.getProperty("crushftp.ftpcommand.timeout2", "60")) * 1000);
        transfer_socket = ss.accept();
        ss.close();
      } 
      if (this.config.getProperty("secure", "false").equals("true") && this.config.getProperty("secure_data", "true").equals("true"))
        ((SSLSocket)transfer_socket).setUseClientMode(true); 
      if (this.config.getProperty("secure", "false").equals("true") && this.config.getProperty("secure_data", "true").equals("true"))
        ((SSLSocket)transfer_socket).startHandshake(); 
    } 
    BufferedReader is_list = new BufferedReader(new InputStreamReader(transfer_socket.getInputStream(), "UTF8"));
    String data = "";
    int loops = 0;
    int max_list = Integer.parseInt(System.getProperty("crushftp.proxy.list.max", "0"));
    while (data != null) {
      loops++;
      data = is_list.readLine();
      if ((max_list <= 0 || list.size() < max_list) && 
        data != null && data.trim().length() > 0 && !data.toUpperCase().startsWith("TOTAL")) {
        log(this.is + ":" + data);
        try {
          Properties dir_item = new Properties();
          dir_item.put("root_dir", path);
          dir_item.put("local", "false");
          dir_item.put("protocol", "ftp");
          int tokenCount = 0;
          StringTokenizer get_em2 = new StringTokenizer(data, " ");
          while (get_em2.hasMoreElements()) {
            tokenCount++;
            String data2 = get_em2.nextElement().toString();
            if (tokenCount == 2 && (data2.trim().endsWith("AM") || data2.trim().endsWith("PM"))) {
              this.config.put("permanently_windows", "true");
              this.config.put("no_stat", "true");
            } 
          } 
          if (data.indexOf("<DIR>") >= 0) {
            this.config.put("permanently_windows", "true");
            this.config.put("no_stat", "true");
          } 
          if (this.config.getProperty("permanently_windows", "false").equals("false")) {
            if (this.config.getProperty("server_type", "").indexOf("WINDOWS") >= 0 && data.indexOf("owner    group") >= 0)
              this.config.put("server_type", "UNIX"); 
            if (this.config.getProperty("server_type", "").indexOf("WINDOWS") >= 0 && data.indexOf("          generic  ") >= 0)
              this.config.put("server_type", "UNIX"); 
            if (this.config.getProperty("server_type", "").indexOf("WINDOWS") >= 0 && data.indexOf(" ") == 9)
              this.config.put("server_type", "UNIX"); 
            if (this.config.getProperty("server_type", "").indexOf("WINDOWS") >= 0 && tokenCount >= 9)
              this.config.put("server_type", "UNIX"); 
          } 
          if (listCommand.equals("NLST")) {
            dir_item.put("permissions", "-rwxrwxrwx");
            dir_item.put("type", "FILE");
            dir_item.put("name", data.trim());
            if (data.endsWith("/") || data.endsWith("\\")) {
              dir_item.put("type", "DIR");
              dir_item.put("permissions", "drwxrwxrwx");
              dir_item.put("name", data.trim().substring(0, data.trim().length() - 1));
            } 
            dir_item.put("owner", "user");
            dir_item.put("group", "group");
            dir_item.put("time_or_year", "00:00");
            dir_item.put("modified", "0");
            dir_item.put("link", "false");
            dir_item.put("num_items", "1");
            dir_item.put("is_virtual", "true");
            dir_item.put("protocol", "FTP");
            dir_item.put("day", "1");
            dir_item.put("num_items", "1");
            dir_item.put("month", "Jan");
            dir_item.put("size", "0");
            if (!dir_item.getProperty("name").equals(".") && !dir_item.getProperty("name").equals("..")) {
              addModifiedItem(dir_item);
              list.addElement(dir_item);
            } 
          } else if (this.config.getProperty("server_type", "").indexOf("NETWARE") >= 0) {
            if (!data.toUpperCase().startsWith("TOTAL ")) {
              if (data.toUpperCase().startsWith("D")) {
                dir_item.put("type", "DIR");
              } else {
                dir_item.put("type", "FILE");
              } 
              StringTokenizer get_em = new StringTokenizer(data, " ");
              get_em.nextToken();
              get_em.nextToken();
              if (dir_item.getProperty("type").equals("DIR")) {
                dir_item.put("permissions", "drwxrwxrwx");
              } else {
                dir_item.put("permissions", "-rwxrwxrwx");
              } 
              dir_item.put("num_items", "1");
              String the_owner = get_em.nextToken().trim();
              dir_item.put("owner", the_owner);
              dir_item.put("group", the_owner);
              dir_item.put("size", get_em.nextToken().trim());
              dir_item.put("month", get_em.nextToken().trim());
              dir_item.put("day", get_em.nextToken().trim());
              dir_item.put("time_or_year", get_em.nextToken().trim());
              String name_data = get_em.nextToken();
              name_data = data.substring(data.indexOf(name_data));
              dir_item.put("name", name_data);
              dir_item.put("local", "false");
              dir_item.put("dir", path);
              if (!dir_item.getProperty("name").equals(".") && !dir_item.getProperty("name").equals("..")) {
                addModifiedItem(dir_item);
                list.addElement(dir_item);
              } 
            } 
          } else if (this.config.getProperty("server_type", "").indexOf("MACOS") >= 0) {
            if (!data.toUpperCase().startsWith("TOTAL ")) {
              if (data.toUpperCase().startsWith("D")) {
                dir_item.put("type", "DIR");
              } else {
                dir_item.put("type", "FILE");
              } 
              StringTokenizer get_em = new StringTokenizer(data, " ");
              dir_item.put("permissions", get_em.nextToken().trim());
              dir_item.put("owner", get_em.nextToken().trim());
              if (dir_item.getProperty("owner").toUpperCase().equals("FOLDER")) {
                dir_item.put("num_items", get_em.nextToken().trim());
                dir_item.put("group", dir_item.getProperty("owner"));
                dir_item.put("size", "0");
              } else {
                dir_item.put("num_items", "0");
                dir_item.put("group", get_em.nextToken().trim());
                dir_item.put("size", get_em.nextToken().trim());
              } 
              dir_item.put("month", get_em.nextToken().trim());
              dir_item.put("day", get_em.nextToken().trim());
              dir_item.put("time_or_year", get_em.nextToken().trim());
              String name_data = get_em.nextToken();
              name_data = data.substring(data.indexOf(name_data));
              dir_item.put("name", name_data);
              dir_item.put("local", "false");
              dir_item.put("dir", path);
              if (!dir_item.getProperty("name").equals(".") && !dir_item.getProperty("name").equals("..")) {
                addModifiedItem(dir_item);
                list.addElement(dir_item);
              } 
            } 
          } else if (this.config.getProperty("server_type", "").indexOf("OS/400") >= 0 && data.indexOf("*") >= 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
            String owner = data.substring(0, 7).trim();
            String size = data.substring(8, 21).trim();
            String date = data.substring(22, 30).trim();
            String time = data.substring(31, 39).trim();
            String type = data.substring(40, 45).trim();
            String name = data.substring(46).trim();
            if (name.startsWith("/"))
              name = name.substring(1); 
            if (name.endsWith("/"))
              name = name.substring(0, name.length() - 1); 
            if (size.equals(""))
              size = "0"; 
            if (date.equals(""))
              date = "01/01/1970"; 
            if (time.equals(""))
              time = "12:00:00"; 
            if (type.equals("*LIB") || name.toUpperCase().endsWith(".LIB")) {
              dir_item.put("type", "DIR");
            } else {
              dir_item.put("type", "FILE");
            } 
            dir_item.put("permissions", String.valueOf(name.toUpperCase().endsWith(".LIB") ? "d" : "-") + "rwxrwxrwx");
            dir_item.put("owner", owner);
            dir_item.put("num_items", "0");
            dir_item.put("group", "group");
            dir_item.put("size", size);
            if (date.indexOf(".") >= 0)
              sdf = new SimpleDateFormat("MM.dd.yy"); 
            dir_item.put("month", this.mm.format(sdf.parse(date)));
            dir_item.put("day", this.dd.format(sdf.parse(date)));
            dir_item.put("time_or_year", this.yyyy.format(sdf.parse(date)));
            if (this.config.getProperty("no_os400", "false").equalsIgnoreCase("false")) {
              name = Common.replace_str(name, ".FILE", "");
              name = Common.replace_str(name, ".MBR", "");
              if (name.indexOf("/") >= 0)
                name = name.replace('/', '.'); 
            } else if (name.indexOf("/") >= 0) {
              name = Common.last(name);
            } 
            dir_item.put("name", name);
            dir_item.put("local", "false");
            dir_item.put("dir", path);
            if (!dir_item.getProperty("name").equals(".") && !dir_item.getProperty("name").equals("..")) {
              addModifiedItem(dir_item);
              list.addElement(dir_item);
            } 
          } else if (this.config.getProperty("server_type", "").indexOf("WINDOWS") >= 0) {
            if (!data.toUpperCase().startsWith("TOTAL ")) {
              String d = getLineToken(data, 0, true).trim();
              String t = getLineToken(data, 1, true).trim();
              Date date = new Date();
              try {
                if (Integer.parseInt(d.substring(6)) < 95) {
                  d = String.valueOf(d.substring(0, 6)) + "20" + d.substring(6);
                } else {
                  d = String.valueOf(d.substring(0, 6)) + "19" + d.substring(6);
                } 
                date = this.msdf.parse(String.valueOf(d) + " " + t);
              } catch (ParseException e) {
                Common.log("FTP_CLIENT", 1, e);
              } 
              dir_item.put("month", this.mmm.format(date));
              dir_item.put("day", this.dd.format(date));
              String time_or_year = this.hhmm.format(date);
              if (!this.yyyy.format(date).equals(this.yyyy.format(new Date())))
                time_or_year = this.yyyy.format(date); 
              dir_item.put("time_or_year", time_or_year);
              String typeOrSize = getLineToken(data, 2, true).trim();
              if (typeOrSize.toUpperCase().indexOf("DIR") >= 0) {
                dir_item.put("type", "DIR");
                dir_item.put("permissions", "drwxrwxrwx");
                dir_item.put("size", "0");
                typeOrSize = String.valueOf(typeOrSize) + "         ";
              } else {
                dir_item.put("type", "FILE");
                dir_item.put("permissions", "-rwxrwxrwx");
                dir_item.put("size", typeOrSize);
              } 
              dir_item.put("owner", "user");
              dir_item.put("group", "group");
              dir_item.put("num_items", "0");
              String name_data = getLineToken(data, 3, false);
              dir_item.put("name", name_data);
              dir_item.put("local", "false");
              dir_item.put("dir", path);
              if (!dir_item.getProperty("name").equals(".") && !dir_item.getProperty("name").equals("..")) {
                addModifiedItem(dir_item);
                list.addElement(dir_item);
              } 
            } 
            if (System.getProperty("crushftp.ftpclient.always_windows", "false").equals("true"))
              Common.log("FTP_CLIENT", 2, "2:" + dir_item); 
          } else if (this.config.getProperty("server_type", "").indexOf("NONSTOP") >= 0) {
            if (data.endsWith(".."))
              continue; 
            parse_unix_line(dir_item, data, tokenCount, path, list);
          } else {
            parse_unix_line(dir_item, data, tokenCount, path, list);
          } 
          if (this.config.getProperty("no_os400", "false").equalsIgnoreCase("true"))
            dir_item.put("no_os400", "true"); 
          if (this.url.endsWith("/") && path.startsWith("/")) {
            dir_item.put("url", String.valueOf(this.url) + path.substring(1) + dir_item.getProperty("name"));
          } else {
            dir_item.put("url", String.valueOf(this.url) + path + dir_item.getProperty("name"));
          } 
          if (this.config.getProperty("stat_cache", "false").equals("true"))
            stat_cache.put(dir_item.getProperty("url"), dir_item); 
        } catch (Exception eee) {
          Common.log("FTP_CLIENT", 1, eee);
          if (eee.indexOf("Interrupted") >= 0)
            throw eee; 
        } 
      } 
    } 
    is_list.close();
    send_data(226, "");
    executeScript(this.config.getProperty("after_dir_script", ""), path.trim());
    return list;
  }
  
  public String getLineToken(String data, int i, boolean stop_on_white) {
    int i2 = -1;
    String s = "";
    boolean in_white = true;
    for (int x = 0; x < data.length(); x++) {
      if (in_white && data.charAt(x) != ' ') {
        in_white = false;
        if (i == i2)
          break; 
        i2++;
      } 
      if (!in_white && data.charAt(x) == ' ')
        in_white = true; 
      if (!in_white && i == i2 && !stop_on_white)
        return data.substring(x); 
      if (!in_white && i == i2)
        s = String.valueOf(s) + data.charAt(x); 
    } 
    return s;
  }
  
  public void addModifiedItem(Properties dir_item) {
    if (dir_item != null) {
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MMM dd yyyy HH:mm", Locale.US);
      SimpleDateFormat yyyy = new SimpleDateFormat("yyyy");
      Date modified = new Date();
      String time_or_year = dir_item.getProperty("time_or_year", "");
      String year = yyyy.format(new Date());
      String time = "00:00";
      if (time_or_year.indexOf(":") < 0) {
        year = time_or_year;
      } else {
        time = time_or_year;
      } 
      try {
        modified = mmddyyyy.parse(String.valueOf(dir_item.getProperty("month", "")) + " " + dir_item.getProperty("day", "") + " " + year + " " + time);
        if (modified.getTime() > System.currentTimeMillis() + 172800000L) {
          year = (new StringBuffer(String.valueOf(Integer.parseInt(year) - 1))).toString();
          modified = mmddyyyy.parse(String.valueOf(dir_item.getProperty("month", "")) + " " + dir_item.getProperty("day", "") + " " + year + " " + time);
        } 
      } catch (Exception e) {
        Common.log("FTP_CLIENT", 1, e);
      } 
      dir_item.put("modified", (new StringBuffer(String.valueOf(modified.getTime()))).toString());
    } 
  }
  
  public void parse_unix_line(Properties dir_item, String data, int tokenCount, String path, Vector list) {
    if (!data.toUpperCase().startsWith("TOTAL ") && !data.toUpperCase().trim().startsWith("SIZE")) {
      if (data.toUpperCase().startsWith("D") || data.toUpperCase().startsWith("L")) {
        dir_item.put("type", "DIR");
      } else {
        dir_item.put("type", "FILE");
      } 
      StringTokenizer get_em = new StringTokenizer(data, " ");
      int countTokens = 0;
      while (get_em.hasMoreElements()) {
        get_em.nextToken();
        countTokens++;
      } 
      get_em = new StringTokenizer(data, " ");
      dir_item.put("proxy_item", "true");
      if (countTokens < 6 || System.getProperty("crushftp.ftpclient.always_windows", "false").equals("true")) {
        Common.log("FTP_CLIENT", 2, "tokens on ftp dir list line:" + countTokens);
        if (!data.toUpperCase().startsWith("TOTAL ")) {
          String d = getLineToken(data, 0, true).trim();
          String t = getLineToken(data, 1, true).trim();
          Date date = new Date();
          try {
            if (Integer.parseInt(d.substring(6)) < 95) {
              d = String.valueOf(d.substring(0, 6)) + "20" + d.substring(6);
            } else {
              d = String.valueOf(d.substring(0, 6)) + "19" + d.substring(6);
            } 
            date = this.msdf.parse(String.valueOf(d) + " " + t);
          } catch (ParseException e) {
            Common.log("FTP_CLIENT", 1, e);
          } 
          dir_item.put("month", this.mmm.format(date));
          dir_item.put("day", this.dd.format(date));
          String time_or_year = this.hhmm.format(date);
          if (!this.yyyy.format(date).equals(this.yyyy.format(new Date())))
            time_or_year = this.yyyy.format(date); 
          dir_item.put("time_or_year", time_or_year);
          String typeOrSize = getLineToken(data, 2, true).trim();
          if (typeOrSize.toUpperCase().indexOf("DIR") >= 0) {
            dir_item.put("type", "DIR");
            dir_item.put("permissions", "drwxrwxrwx");
            dir_item.put("size", "0");
            typeOrSize = String.valueOf(typeOrSize) + "         ";
          } else {
            dir_item.put("type", "FILE");
            dir_item.put("permissions", "-rwxrwxrwx");
            dir_item.put("size", typeOrSize);
          } 
          dir_item.put("owner", "user");
          dir_item.put("group", "group");
          dir_item.put("num_items", "0");
          String name_data = getLineToken(data, 3, false);
          dir_item.put("name", name_data);
          dir_item.put("local", "false");
          dir_item.put("dir", path);
        } 
      } else {
        dir_item.put("permissions", get_em.nextToken().trim());
        dir_item.put("num_items", get_em.nextToken().trim());
        dir_item.put("owner", get_em.nextToken().trim().replace('\\', '_'));
        String group = get_em.nextToken().trim();
        Common.log("FTP_CLIENT", 2, "tokens on ftp dir list line:" + tokenCount);
        if (tokenCount == 8 || this.config.getProperty("7_token_proxy", "false").equalsIgnoreCase("true")) {
          dir_item.put("group", "group");
          dir_item.put("size", group);
        } else {
          dir_item.put("group", group.replace('\\', '_'));
          dir_item.put("size", get_em.nextToken().trim());
        } 
        dir_item.put("month", get_em.nextToken().trim());
        dir_item.put("day", get_em.nextToken().trim());
        dir_item.put("time_or_year", get_em.nextToken().trim());
        String name_data = get_em.nextToken();
        String searchName = String.valueOf(dir_item.getProperty("time_or_year")) + " " + name_data;
        name_data = data.substring(data.indexOf(name_data, data.indexOf(searchName) + dir_item.getProperty("time_or_year").length() + 1));
        if (name_data.startsWith("/"))
          name_data = Common.last(name_data); 
        dir_item.put("name", name_data);
        if (data.toUpperCase().startsWith("L")) {
          dir_item.put("name", name_data.substring(0, name_data.indexOf(" ->")));
          dir_item.put("permissions", "drwxrwxrwx");
          dir_item.put("type", "DIR");
        } 
      } 
      if (dir_item.getProperty("type").equalsIgnoreCase("DIR"))
        dir_item.put("size", "1"); 
      dir_item.put("local", "false");
      dir_item.put("dir", path);
      if (!dir_item.getProperty("name").equals(".") && !dir_item.getProperty("name").equals("..")) {
        addModifiedItem(dir_item);
        list.addElement(dir_item);
      } 
    } 
    if (System.getProperty("crushftp.ftpclient.always_windows", "false").equals("true"))
      Common.log("FTP_CLIENT", 2, "1:" + dir_item); 
  }
  
  public Object getTransferSocket(boolean pasv) throws Exception {
    if (!pasv) {
      String str = this.sock.getLocalAddress().getHostAddress();
      if (this.url.indexOf("~") >= 0) {
        VRL u = new VRL(this.url);
        if (u.getHost().indexOf("~") >= 0)
          str = u.getHost().split("~")[0].trim(); 
      } 
      ServerSocket ss = null;
      synchronized (activePortLock) {
        int startPort = Integer.parseInt(this.config.getProperty("proxyActivePorts", "1025-65535").split("-")[0]);
        int endPort = Integer.parseInt(this.config.getProperty("proxyActivePorts", "1025-65535").split("-")[1]);
        int loops = 0;
        while (ss == null && loops++ < 1000) {
          if (curPort < 0 || curPort < startPort)
            curPort = startPort; 
          if (curPort > endPort)
            curPort = startPort; 
          try {
            int listen_port = 0;
            if (startPort != 1025 && endPort != 65535)
              listen_port = curPort; 
            if (this.config.getProperty("secure", "false").equalsIgnoreCase("true") && this.config.getProperty("secure_data", "true").equals("true")) {
              if (common_code == null)
                common_code = new Common(); 
              String tempKeystore = this.config.getProperty("keystore_path", "builtin");
              if (tempKeystore.endsWith("dxserverpub"))
                tempKeystore = "builtin"; 
              if (this.factory == null)
                this.factory = (new Common()).getSSLContext(!this.config.getProperty("keystore_path", "").equals("") ? this.config.getProperty("keystore_path") : "builtin", this.config.getProperty("trustore_path", "builtin"), Common.encryptDecrypt(this.config.getProperty("keystore_pass"), false), Common.encryptDecrypt(this.config.getProperty("key_pass"), false), "TLS", !this.config.getProperty("keystore_path", "").equals(""), this.config.getProperty("acceptAnyCert", "true").equalsIgnoreCase("true")); 
              ss = common_code.getServerSocket(this.factory.getServerSocketFactory(), listen_port, null, this.config.getProperty("disabled_ciphers", ""), false);
            } else {
              ss = new ServerSocket(listen_port, 1, InetAddress.getByName(str));
            } 
          } catch (Exception e) {
            Common.log("FTP_CLIENT", 0, String.valueOf(e.getMessage()) + ":ip=" + str + " port=" + curPort);
            Common.log("FTP_CLIENT", 2, e);
          } 
          curPort++;
        } 
      } 
      if (ss == null)
        throw new Exception("Could not build a server socket."); 
      int i = ss.getLocalPort();
      str = str.replace('.', ',');
      int j = i / 256;
      int k = i - i / 256 * 256;
      send_data(200, "PORT " + str + "," + j + "," + k);
      return ss;
    } 
    String data = "";
    data = send_data(227, "PASV");
    int endPasv = data.indexOf(")");
    if (endPasv < 0)
      endPasv = data.length(); 
    data = data.substring(data.indexOf("(") + 1, endPasv).trim();
    int port1 = Integer.parseInt(data.substring(data.lastIndexOf(",", data.lastIndexOf(",") - 1) + 1, data.lastIndexOf(",")).trim());
    int port2 = Integer.parseInt(data.substring(data.lastIndexOf(",") + 1).trim());
    int port = port1 * 256 + port2;
    String ip = data.substring(0, data.lastIndexOf(",", data.lastIndexOf(",") - 1)).replace(',', '.').trim();
    if (this.config.getProperty("autoPasvIpSubstitution", "true").equalsIgnoreCase("true")) {
      VRL u = new VRL(this.url);
      ip = u.getHost();
    } 
    Socket tempSock = null;
    for (int x = 0; x < 4; x++) {
      try {
        tempSock = Common.getSocket("FTP", new VRL("ftp://" + ip + ":" + port + "/"), this.config.getProperty("use_dmz", "false"), "", Integer.parseInt(this.config.getProperty("timeout", "30000")));
        break;
      } catch (IOException e) {
        if (x == 3)
          throw e; 
        Thread.sleep(500L);
      } 
    } 
    if (this.config.getProperty("secure", "false").equalsIgnoreCase("true") && this.config.getProperty("secure_data", "true").equals("true")) {
      SSLSocket ss = null;
      if (this.factory == null)
        this.factory = (new Common()).getSSLContext(!this.config.getProperty("keystore_path", "").equals("") ? this.config.getProperty("keystore_path") : "builtin", this.config.getProperty("trustore_path", "builtin"), Common.encryptDecrypt(this.config.getProperty("keystore_pass"), false), Common.encryptDecrypt(this.config.getProperty("key_pass"), false), "TLS", !this.config.getProperty("keystore_path", "").equals(""), this.config.getProperty("acceptAnyCert", "true").equalsIgnoreCase("true")); 
      if (this.factory_ssl == null)
        this.factory_ssl = this.factory.getSocketFactory(); 
      ss = (SSLSocket)this.factory_ssl.createSocket(tempSock, ip, port, true);
      try {
        SSLSessionContext sessionContext = ((SSLSocket)this.sock).getSession().getSessionContext();
        sessionContext.setSessionCacheSize(100);
        Field sessionHostPortCache = sessionContext.getClass().getDeclaredField("sessionHostPortCache");
        sessionHostPortCache.setAccessible(true);
        Object cache = sessionHostPortCache.get(sessionContext);
        if (class$0 == null)
          try {
          
          } catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(null.getMessage());
          }  
        false[class$0] = class$0 = Class.forName("java.lang.Object");
        if (class$0 == null)
          try {
          
          } catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(null.getMessage());
          }  
        true[class$0] = class$0 = Class.forName("java.lang.Object");
        Method method = (new Class[2]).getDeclaredMethod((String)new Class[2], new Class[2]);
        method.setAccessible(true);
        String key1 = String.format("%s:%s", new Object[] { ss.getInetAddress().getHostName(), String.valueOf(ss.getPort()) }).toLowerCase(Locale.ROOT);
        method.invoke(cache, new Object[] { key1, ((SSLSocket)this.sock).getSession() });
        String key2 = String.format("%s:%s", new Object[] { ip, String.valueOf(ss.getPort()) }).toLowerCase(Locale.ROOT);
        method.invoke(cache, new Object[] { key2, ((SSLSocket)this.sock).getSession() });
      } catch (Exception e) {
        Common.log("FTP_CLIENT", 1, e);
      } 
      return ss;
    } 
    return tempSock;
  }
  
  private String send_data_raw(int expectedResponse, String data, InputStream in) throws IOException {
    String verb = null;
    String verb_data = null;
    if (data.trim().length() > 0) {
      verb = (String.valueOf(data) + " ").substring(0, (String.valueOf(data) + " ").indexOf(" ")).toLowerCase().trim();
      verb_data = (String.valueOf(data) + " ").substring((String.valueOf(data) + " ").indexOf(" ")).trim();
      executeScript(this.config.getProperty("before_" + verb + "_script", ""), verb_data);
    } 
    int timeout = Integer.parseInt(System.getProperty("crushftp.ftpcommand.timeout1", "30")) * 1000;
    if (data.length() > 0) {
      send_data_raw(String.valueOf(data) + "\r\n");
    } else {
      timeout = Integer.parseInt(System.getProperty("crushftp.ftpcommand.timeout2", "60")) * 1000;
    } 
    if (data.trim().equalsIgnoreCase("QUIT"))
      timeout = 3000; 
    int current_timeout = 0;
    try {
      current_timeout = this.sock.getSoTimeout();
    } catch (Exception exception) {}
    boolean startsWithInt = false;
    try {
      this.sock.setSoTimeout(timeout);
    } catch (Exception exception) {}
    data = "";
    String dataTotal = "";
    while (data.length() < 3 || (data.length() >= 4 && data.charAt(3) != ' ') || !startsWithInt) {
      String result = "";
      int lastByte = 0;
      while (result.indexOf("\r\n") < 0 && lastByte >= 0) {
        lastByte = in.read();
        if (lastByte > 0)
          result = String.valueOf(result) + (char)lastByte; 
      } 
      if (lastByte < 0)
        return null; 
      data = result.trim();
      log(in + ":" + data);
      dataTotal = String.valueOf(dataTotal) + data + "\r\n";
      try {
        startsWithInt = false;
        Integer.parseInt(data.substring(0, 3));
        startsWithInt = true;
      } catch (Exception exception) {}
    } 
    Common.log("FTP_CLIENT", 2, "FTP RESPONSE:" + expectedResponse + ":" + dataTotal);
    try {
      this.sock.setSoTimeout(current_timeout);
    } catch (Exception exception) {}
    int code = Integer.parseInt(data.substring(0, 3));
    if (expectedResponse > 0 && Math.abs(code - expectedResponse) >= 100 && expectedResponse != 331) {
      Common.log("FTP_CLIENT", 1, "FTP Error!  Expected " + expectedResponse + " but got " + data);
      if (this.config.getProperty("rfc_proxy", "false").equals("true"))
        throw new IOException(data); 
      throw new IOException("FTP Error!  Expected " + expectedResponse + " but got " + data);
    } 
    if (verb != null)
      executeScript(this.config.getProperty("after_" + verb + "_script", ""), verb_data); 
    return dataTotal;
  }
  
  private String send_data(int expectedResponse, String data) throws IOException {
    if (!data.toUpperCase().startsWith("PASS"))
      Common.log("FTP_CLIENT", 2, "FTP COMMAND:" + expectedResponse + ":" + data); 
    if (data.startsWith("TYPE ") && data.equals(this.config.getProperty("currentTYPE", "")))
      return ""; 
    if (data.startsWith("TYPE "))
      setConfig("currentTYPE", data); 
    data = send_data_now(data, true);
    String rawData = data;
    String[] lines = data.split("\\r\\n");
    int code = Integer.parseInt(lines[lines.length - 1].substring(0, 3));
    if (code != expectedResponse && Math.abs(code - expectedResponse) < 75)
      code = expectedResponse; 
    if (code != expectedResponse && expectedResponse > 0) {
      Common.log("FTP_CLIENT", 1, "FTP Error!  Expected " + expectedResponse + " but got " + rawData);
      if (this.config.getProperty("rfc_proxy", "false").equals("true"))
        throw new IOException(rawData); 
      throw new IOException("FTP Error!  Expected " + expectedResponse + " but got " + rawData);
    } 
    return data;
  }
  
  private String send_data_now(String data, boolean returnAll) throws IOException {
    String verb = null;
    String verb_data = null;
    if (data.trim().length() > 0) {
      verb = (String.valueOf(data) + " ").substring(0, (String.valueOf(data) + " ").indexOf(" ")).toLowerCase().trim();
      verb_data = (String.valueOf(data) + " ").substring((String.valueOf(data) + " ").indexOf(" ")).trim();
      executeScript(this.config.getProperty("before_" + verb + "_script", ""), verb_data);
    } 
    int timeout = Integer.parseInt(System.getProperty("crushftp.ftpcommand.timeout1", "30")) * 1000;
    if (data.length() > 0) {
      send_data_raw(String.valueOf(data) + "\r\n");
    } else {
      timeout = Integer.parseInt(System.getProperty("crushftp.ftpcommand.timeout2", "60")) * 1000;
    } 
    if (data.trim().equalsIgnoreCase("QUIT"))
      timeout = 3000; 
    int current_timeout = 0;
    try {
      current_timeout = this.sock.getSoTimeout();
    } catch (Exception exception) {}
    data = "-------------";
    boolean startsWithInt = false;
    String totalData = "";
    try {
      this.sock.setSoTimeout(timeout);
    } catch (Exception exception) {}
    while (data.length() < 3 || (data.length() >= 4 && data.charAt(3) != ' ') || !startsWithInt) {
      if (this.is == null)
        return null; 
      data = this.is.readLine();
      log(this.is + ":" + data);
      Common.log("FTP_CLIENT", 2, this.is + ":" + data.trim());
      if ((this.config.getProperty("server_type", "UNKNOWN").indexOf("NONSTOP") >= 0 && data.startsWith("500 '': command not understood.")) || (
        this.config.getProperty("server_type", "UNKNOWN").indexOf("OS/400") >= 0 && data.toUpperCase().indexOf("not valid".toUpperCase()) >= 0) || (
        this.config.getProperty("server_type", "UNKNOWN").indexOf("WFTPD") >= 0 && data.toUpperCase().indexOf("Unidentified command".toUpperCase()) >= 0) || 
        data.toUpperCase().indexOf("Syntax error, command unrecognized".toUpperCase()) >= 0)
        continue; 
      totalData = String.valueOf(totalData) + data + "\r\n";
      try {
        startsWithInt = false;
        Integer.parseInt(data.substring(0, 4).trim());
        startsWithInt = true;
      } catch (Exception e) {
        Common.log("FTP_CLIENT", 3, e);
      } 
      if (data.startsWith("221"))
        break; 
    } 
    if (verb != null)
      executeScript(this.config.getProperty("after_" + verb + "_script", ""), verb_data); 
    try {
      this.sock.setSoTimeout(current_timeout);
    } catch (Exception exception) {}
    if (returnAll)
      return totalData; 
    return data;
  }
  
  private void send_data_raw(String data) throws IOException {
    try {
      if (data.indexOf("PASS ") < 0) {
        log(this.os + ":" + data);
      } else {
        log(this.os + ":" + "PASS **********");
      } 
      this.os.write(data);
      this.os.flush();
    } catch (SocketException e) {
      this.config.put("error", e.toString());
      throw e;
    } 
  }
  
  public void setMod(String path, String val, String param) {}
  
  public void setOwner(String path, String val, String param) {}
  
  public void setGroup(String path, String val, String param) {}
  
  public String doCommand(String command) throws Exception {
    return send_data_now(String.valueOf(command) + "\r\n", false);
  }
  
  public void executeScript(String script, String verb_data) throws IOException {
    if (script == null || script.trim().equals(""))
      return; 
    script = Common.replace_str(script, "%data%", verb_data);
    script = Common.replace_str(script, "{data}", verb_data);
    BufferedReader br = new BufferedReader(new StringReader(script));
    while (true) {
      String command = br.readLine();
      if (command == null)
        break; 
      String response = br.readLine();
      if (response == null)
        throw new IOException("Script format error, missing required response pattern."); 
      command = command.trim();
      response = response.trim();
      boolean isNumber = false;
      try {
        Integer.parseInt(response);
        isNumber = true;
      } catch (Exception exception) {}
      if (isNumber) {
        send_data(Integer.parseInt(response), command);
        continue;
      } 
      String actualResponse = send_data_now(command, true);
      if (!Common.do_search(response, actualResponse, false, 0))
        throw new IOException("Script validation failure:'" + response + "' does not match: '" + actualResponse + "'."); 
    } 
  }
}
