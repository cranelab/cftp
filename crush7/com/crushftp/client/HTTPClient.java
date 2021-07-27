package com.crushftp.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class HTTPClient extends GenericClient {
  Vector openConnections = new Vector();
  
  public HTTPClient(String url, String header, Vector log) {
    super(header, log);
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
    this.config.put("protocol", "HTTP");
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    username = VRL.vrlEncode(username);
    password = VRL.vrlEncode(password);
    this.config.put("username", username);
    this.config.put("password", password);
    if (clientid != null)
      this.config.put("clientid", clientid); 
    if (!this.config.getProperty("crushAuth", "").equals(""))
      return "Success"; 
    if (username.equals("anonymous") && password.equals("anonymous"))
      return "Success"; 
    System.getProperties().put("sun.net.http.retryPost", "false");
    VRL u = new VRL(String.valueOf(this.url) + "WebInterface/function/");
    String result = "";
    if (u.getProtocol().equalsIgnoreCase("HTTPS") && !this.config.getProperty("keystore_path", "").equals("")) {
      u = new VRL(this.url);
      log("Connecting to:" + u.getHost() + ":" + u.getPort());
      URLConnection uRLConnection = URLConnection.openConnection(u, this.config);
      try {
        uRLConnection.setRequestMethod("GET");
        uRLConnection.setUseCaches(false);
        uRLConnection.setDoOutput(true);
        uRLConnection.getOutputStream().write(("command=login&username=" + username + "&password=" + password + "&clientid=" + this.config.getProperty("clientid")).getBytes("UTF8"));
        uRLConnection.getResponseCode();
        if (uRLConnection.getCookie("CrushAuth") == null)
          throw new SocketException("Login failed, no CrushAuth cookie"); 
        this.config.put("crushAuth", uRLConnection.getCookie("CrushAuth"));
        result = Common.consumeResponse(uRLConnection.getInputStream());
      } finally {
        uRLConnection.disconnect();
      } 
      return "Success";
    } 
    log("Connecting to:" + u.getHost() + ":" + u.getPort());
    URLConnection urlc = URLConnection.openConnection(u, this.config);
    try {
      urlc.setRequestMethod("POST");
      urlc.setUseCaches(false);
      urlc.setDoOutput(true);
      urlc.getOutputStream().write(("command=login&username=" + username + "&password=" + password + "&clientid=" + this.config.getProperty("clientid")).getBytes("UTF8"));
      urlc.getResponseCode();
      if (urlc.getCookie("CrushAuth") == null)
        throw new SocketException("Login failed, no CrushAuth cookie"); 
      this.config.put("crushAuth", urlc.getCookie("CrushAuth"));
      result = Common.consumeResponse(urlc.getInputStream());
    } finally {
      urlc.disconnect();
    } 
    String message = "";
    if (result.indexOf("<message>") >= 0)
      message = result.substring(result.indexOf("<message>"), result.indexOf("</message>") + "</message>".length()); 
    result = result.substring(result.indexOf("<response>"), result.indexOf("</response>") + "</response>".length());
    if (result.indexOf("failure") >= 0)
      throw new Exception(String.valueOf(result) + ":" + message); 
    return result;
  }
  
  public void logout() throws Exception {
    close();
    try {
      doAction("logout", (String)null, (String)null);
    } catch (Exception exception) {}
    this.config.put("crushAuth", "");
  }
  
  public void close() throws Exception {
    if (this.in != null)
      this.in.close(); 
    if (this.out != null)
      this.out.close(); 
    while (this.openConnections.size() > 0) {
      URLConnection urlc = this.openConnections.remove(0);
      urlc.disconnect();
    } 
    this.in = null;
    this.out = null;
  }
  
  public Vector list(String path, Vector list) throws Exception {
    Properties listingProp = list2(path, list);
    return (Vector)listingProp.remove("listing");
  }
  
  public Properties list2(String path, Vector list) throws Exception {
    String result = doAction("list", path, "");
    String result2 = null;
    for (int x = 0; x < 5; x++) {
      if (result.indexOf("<listing>") >= 0 && result.indexOf("</listing>") >= 0) {
        result2 = String.valueOf(result.substring(0, result.indexOf("<listing>"))) + result.substring(result.lastIndexOf("</listing>") + "</listing>".length());
        break;
      } 
      Thread.sleep(1000L);
      result = doAction("list", path, "");
    } 
    if (result2 == null)
      throw new IOException("Listing failed:" + path + ":" + result); 
    Properties listingProp = (Properties)Common.readXMLObject(new ByteArrayInputStream(result2.getBytes("UTF8")));
    listingProp.put("listing", list);
    result = result.substring(result.indexOf("<listing>") + "<listing>".length(), result.lastIndexOf("</listing>")).trim();
    if (this.statCache != null) {
      Enumeration keys = this.statCache.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (this.statCache.get(key) instanceof Properties) {
          Properties h = (Properties)this.statCache.get(key);
          if (System.currentTimeMillis() - Long.parseLong(h.getProperty("time")) > 60000L)
            this.statCache.remove(key); 
        } 
      } 
    } 
    BufferedReader br = new BufferedReader(new StringReader(result));
    String data = "";
    while ((data = br.readLine()) != null) {
      Properties stat = parseDmzStat(data);
      stat.put("url", String.valueOf(this.url) + path.substring(1) + stat.getProperty("name"));
      list.addElement(stat);
      if (this.config.getProperty("dmz_stat_caching", "true").equals("true") && this.statCache != null) {
        Properties h = new Properties();
        h.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        h.put("stat", stat.clone());
        this.statCache.put(noSlash(String.valueOf(path) + stat.getProperty("name")), h);
        if (!this.statCache.containsKey(String.valueOf(path) + "._" + stat.getProperty("name"))) {
          h = new Properties();
          h.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
          this.statCache.put(noSlash(String.valueOf(path) + "._" + stat.getProperty("name")), h);
        } 
      } 
    } 
    if (this.statCache != null && list.size() > 10000)
      this.statCache.clear(); 
    addFinderBadItems();
    return listingProp;
  }
  
  public void addFinderBadItems() throws Exception {
    if (this.statCache != null) {
      long aday = System.currentTimeMillis() + 86400000L;
      Properties h = new Properties();
      h.put("time", (new StringBuffer(String.valueOf(aday))).toString());
      this.statCache.put(noSlash("/Backups.backupdb"), h);
      h = new Properties();
      h.put("time", (new StringBuffer(String.valueOf(aday))).toString());
      this.statCache.put(noSlash("/.hidden"), h);
      h = new Properties();
      h.put("time", (new StringBuffer(String.valueOf(aday))).toString());
      this.statCache.put(noSlash("/mach_kernel"), h);
      h = new Properties();
      h.put("time", (new StringBuffer(String.valueOf(aday))).toString());
      this.statCache.put(noSlash("/DCIM"), h);
      h = new Properties();
      h.put("time", (new StringBuffer(String.valueOf(aday))).toString());
      this.statCache.put(noSlash("/.Spotlight-V100"), h);
      h = new Properties();
      Properties stat = parseStat("-rwx------  1 user  group           0 20000101000000 01 2000 /.metadata_never_index");
      if (stat != null)
        stat.put("url", String.valueOf(this.url) + ".metadata_never_index"); 
      h.put("stat", stat.clone());
      h.put("time", (new StringBuffer(String.valueOf(aday))).toString());
      this.statCache.put(noSlash("/.metadata_never_index"), h);
    } 
  }
  
  public ZipTransfer getZipTransfer(String path, Properties params, boolean compress) {
    return new ZipTransfer(this.url, this.config.getProperty("crushAuth", ""), path, params, compress);
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    return download3(path, startPos, endPos, binary, (String)null, -1);
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary, String paths, int rev) throws Exception {
    log("download | " + path + " | " + startPos + " | " + endPos + " | " + binary + ((paths != null) ? (" | downloadAsZip:" + paths) : "") + ((rev >= 0) ? (" | revision:" + rev) : ""));
    URLConnection urlc = null;
    if (rev >= 0) {
      urlc = URLConnection.openConnection(new VRL(String.valueOf(this.url) + "WebInterface/function/"), this.config);
      urlc.setRequestMethod("POST");
      urlc.setRequestProperty("Cookie", "CrushAuth=" + this.config.getProperty("crushAuth", "") + ";");
      urlc.setUseCaches(false);
      urlc.setDoOutput(true);
      urlc.getOutputStream().write(("command=download&c2f=" + this.config.getProperty("crushAuth", "").substring(this.config.getProperty("crushAuth", "").length() - 4) + "&path=" + Common.url_encode(path) + "&META_downloadRevision=" + rev).getBytes("UTF8"));
      urlc.getResponseCode();
    } else if (paths != null) {
      urlc = URLConnection.openConnection(new VRL(String.valueOf(this.url) + "WebInterface/function/"), this.config);
      urlc.setRequestMethod("POST");
      urlc.setRequestProperty("Cookie", "CrushAuth=" + this.config.getProperty("crushAuth", "") + ";");
      urlc.setUseCaches(false);
      urlc.setDoOutput(true);
      urlc.getOutputStream().write(("command=downloadAsZip&c2f=" + this.config.getProperty("crushAuth", "").substring(this.config.getProperty("crushAuth", "").length() - 4) + "&path_shortening=" + this.config.getProperty("path_shortening", "true").equals("true") + "&path=" + Common.url_encode(path) + "&paths=" + Common.url_encode(paths)).getBytes("UTF8"));
      urlc.getResponseCode();
    } else {
      urlc = URLConnection.openConnection(new VRL(String.valueOf(this.url) + path.substring(1)), this.config);
      urlc.setRequestMethod("GET");
      urlc.setRequestProperty("Cookie", "CrushAuth=" + this.config.getProperty("crushAuth", "") + ";");
      if (startPos > 0L || endPos >= 0L)
        urlc.setRequestProperty("Range", "bytes=" + startPos + "-" + ((endPos >= 0L) ? (new StringBuffer(String.valueOf(endPos))).toString() : "")); 
      urlc.setUseCaches(false);
      urlc.setReceiveCompression((this.config.getProperty("receive_compressed", "false").equals("true") && startPos <= 0L));
    } 
    urlc.setReadTimeout(Integer.parseInt(this.config.getProperty("timeout", "0")));
    this.openConnections.addElement(urlc);
    InputStream tmp = urlc.getInputStream();
    if (urlc.responseCode > 299) {
      log("download-end | " + path + " | " + startPos + " | " + endPos + " | " + binary + " | ERROR:" + urlc.responseCode + ":" + urlc.message);
      throw new IOException(String.valueOf(urlc.responseCode) + ":" + urlc.message);
    } 
    this.in = new HTTPClient$1$InputWrapper(this, tmp, path, startPos, endPos, binary);
    return this.in;
  }
  
  public InputStream downloadAsZip(String path, String paths) throws Exception {
    return downloadAsZip(path, paths, true);
  }
  
  public InputStream downloadAsZip(String path, String paths, boolean path_shortening) throws Exception {
    this.config.put("path_shortening", (new StringBuffer(String.valueOf(path_shortening))).toString());
    return download3(path, 0L, -1L, true, paths, -1);
  }
  
  public InputStream downloadRev(String path, int rev) throws Exception {
    return download3(path, 0L, -1L, true, (String)null, rev);
  }
  
  public void sendMetaInfo(Properties metaInfo) throws Exception {
    if (metaInfo == null)
      return; 
    String content = "";
    Enumeration keys = metaInfo.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      String val = metaInfo.getProperty(key);
      content = String.valueOf(content) + "&META_" + key + "=" + u(val);
    } 
    if (!content.trim().equals(""))
      doAction("setMetaInfo", content, ""); 
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    log("upload | " + path + " | " + startPos + " | " + truncate + " | " + binary);
    if (this.statCache != null)
      this.statCache.remove(noSlash(path)); 
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.url) + path.substring(1)), this.config);
    this.openConnections.addElement(urlc);
    urlc.setRequestMethod("PUT");
    urlc.setRequestProperty("Cookie", "CrushAuth=" + this.config.getProperty("crushAuth", "") + ";RandomAccess=" + (truncate ? 0 : 1) + ";");
    if (startPos > 0L)
      urlc.setRequestProperty("Range", "bytes=" + startPos + "-"); 
    urlc.setUseCaches(false);
    urlc.setDoOutput(true);
    urlc.setChunkedStreamingMode(9999L);
    urlc.setSendCompression(this.config.getProperty("send_compressed", "false").equals("true"));
    if (!this.transfer_info.getProperty("transfer_content_length", "").equals(""))
      urlc.setRequestProperty("Content-Length", this.transfer_info.getProperty("transfer_content_length", "")); 
    this.out = new HTTPClient$1$OutputWrapper(this, urlc.getOutputStream(), path, urlc, startPos, truncate, binary);
    return this.out;
  }
  
  public boolean upload_0_byte(String path) throws Exception {
    if (this.statCache != null)
      this.statCache.remove(noSlash(path)); 
    return doAction("upload_0_byte", path, "").trim().equalsIgnoreCase("OK");
  }
  
  public boolean delete(String path) throws Exception {
    if (this.statCache != null)
      this.statCache.remove(noSlash(path)); 
    return doAction("delete", path, "").equals("");
  }
  
  public boolean makedir(String path) throws Exception {
    if (this.statCache != null)
      this.statCache.remove(noSlash(path)); 
    return doAction("makedir", path, "").equals("");
  }
  
  public boolean makedirs(String path) throws Exception {
    if (this.statCache != null)
      this.statCache.remove(noSlash(path)); 
    return makedir(path);
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    if (this.statCache != null)
      this.statCache.remove(noSlash(rnfr)); 
    if (this.statCache != null)
      this.statCache.remove(noSlash(rnto)); 
    return doAction("rename", rnfr, rnto).equals("");
  }
  
  public Properties stat(String path) throws Exception {
    addFinderBadItems();
    if (this.config.getProperty("dmz_stat_caching", "true").equals("true") && this.statCache != null) {
      Properties h = (Properties)this.statCache.get(noSlash(path));
      if (h != null) {
        if (System.currentTimeMillis() - Long.parseLong(h.getProperty("time")) < 60000L) {
          Properties properties = (Properties)h.get("stat");
          if (properties == null)
            return null; 
          if (this.config.containsKey(path))
            properties.put("size", this.config.getProperty(path)); 
          properties = (Properties)properties.clone();
          return properties;
        } 
        this.statCache.remove(noSlash(path));
      } 
    } 
    if (this.config.getProperty("dmz_stat_caching", "true").equals("true") && this.statCache != null) {
      this.statCache.put(String.valueOf(Common.all_but_last(path)) + "...count", (new StringBuffer(String.valueOf(Integer.parseInt(this.statCache.getProperty(String.valueOf(Common.all_but_last(path)) + "...count", "0")) + 1))).toString());
      if (!path.equals("/") && Integer.parseInt(this.statCache.getProperty(String.valueOf(Common.all_but_last(path)) + "...count", "0")) > 5) {
        list(Common.all_but_last(path), new Vector());
        this.statCache.put(String.valueOf(Common.all_but_last(path)) + "...count", "0");
        Properties h = (Properties)this.statCache.get(noSlash(path));
        if (h != null) {
          Properties properties = (Properties)h.get("stat");
          if (properties == null)
            return null; 
          if (this.config.containsKey(path))
            properties.put("size", this.config.getProperty(path)); 
          properties = (Properties)properties.clone();
          return properties;
        } 
      } 
    } 
    Properties stat = parseDmzStat(doAction("stat", path, ""));
    if (this.config.containsKey(path))
      stat.put("size", this.config.getProperty(path)); 
    if (stat != null)
      stat.put("url", String.valueOf(this.url) + path.substring(1)); 
    if (this.config.getProperty("dmz_stat_caching", "true").equals("true") && this.statCache != null) {
      Properties h = new Properties();
      h.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      if (stat != null) {
        h.put("stat", stat.clone());
        this.statCache.put(noSlash(path), h);
      } 
    } 
    if (stat != null)
      log("stat-server | " + path + " | " + stat.getProperty("size") + " | " + stat.getProperty("modified")); 
    if (stat == null && System.getProperty("crushftp.isTestCall", "false").equals("true"))
      throw new Exception("Item not found..." + path); 
    return stat;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    if (this.statCache != null)
      this.statCache.remove(noSlash(path)); 
    return doAction("mdtm", path, (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date(modified))).equals("");
  }
  
  public String doAction(String command, String param1, String param2) throws Exception {
    int loops = 0;
    while (loops++ < 4) {
      log(String.valueOf(command) + " | " + param1 + " | " + param2);
      URLConnection urlc = URLConnection.openConnection(new VRL(this.url), this.config);
      urlc.setRequestMethod("POST");
      urlc.setRequestProperty("Cookie", "CrushAuth=" + this.config.getProperty("crushAuth", "") + ";");
      urlc.setUseCaches(false);
      urlc.setDoOutput(true);
      if (command.equalsIgnoreCase("logout"))
        urlc.autoClose = true; 
      String c2f = "";
      if (!this.config.getProperty("crushAuth", "").equals(""))
        c2f = this.config.getProperty("crushAuth", "").substring(this.config.getProperty("crushAuth", "").length() - 4); 
      if (command.equalsIgnoreCase("delete")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=delete&names=" + u(param1)).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("logout")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=logout").getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("getUserName")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=getUserName").getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("getCrushAuth")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=getCrushAuth").getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("makedir")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=makedir&path=" + u(param1)).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("rename")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=rename&name1=" + u(param1) + "&name2=" + u(param2) + "&path=/").getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("stat")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=stat&path=" + u(param1) + "&format=stat_dmz").getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("mdtm")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=mdtm&path=" + u(param1) + "&date=" + u(param2)).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("changePassword")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=changePassword&current_password=" + u(param1) + "&new_password1=" + u(param2) + "&new_password2=" + u(param2)).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("getTime")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=getTime").getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("siteCommand")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=siteCommand&siteCommand=" + u(param1)).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("getQuota")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=getQuota&path=" + u(param1)).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("upload_0_byte")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=upload_0_byte&path=" + u(param1)).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("list")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=getXMLListing&path=" + u(param1) + "&format=stat_dmz").getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("setMetaInfo")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=setMetaInfo" + param1 + param2).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("agentRegister")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=agentRegister" + param1 + param2).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("agentQueue")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=agentQueue" + param1 + param2).getBytes("UTF8"));
      } else if (command.equalsIgnoreCase("agentResponse")) {
        urlc.getOutputStream().write(("c2f=" + c2f + "&command=agentResponse" + param1 + param2).getBytes("UTF8"));
      } 
      int code = 302;
      String result = "";
      try {
        code = urlc.getResponseCode();
        result = Common.consumeResponse(urlc.getInputStream());
      } catch (Exception e) {
        Common.log("HTTP_CLIENT", 1, e);
      } 
      if (code != 302 && urlc.getURL().toString().indexOf("/WebInterface/login.html") >= 0)
        code = 302; 
      setConfig("error", null);
      urlc.disconnect();
      if (code == 302 && command.equalsIgnoreCase("logout"))
        code = 200; 
      if (code == 302 && loops <= 2) {
        Thread.sleep(4500L);
        continue;
      } 
      if (code == 302 || result.indexOf("FAILURE:Access Denied. (c2f)") >= 0) {
        this.config.put("crushAuth", "");
        login(this.config.getProperty("username"), this.config.getProperty("password"), this.config.getProperty("clientid"));
        Thread.sleep(4500L);
        continue;
      } 
      if (result.indexOf("<response>") >= 0)
        result = result.substring(result.indexOf("<response>") + "<response>".length(), result.lastIndexOf("</response>")); 
      if (!command.equalsIgnoreCase("list") && !command.equalsIgnoreCase("agentQueue"))
        log(result); 
      return result;
    } 
    setConfig("error", "Logged out.");
    this.config.put("crushAuth", "");
    throw new Exception("Logged out.");
  }
  
  public String noSlash(String path) {
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    return path;
  }
  
  public String doCommand(String command) {
    if (command.startsWith("SITE PASS")) {
      command = command.substring("site pass".length()).trim();
      String split = command.split(" ")[0];
      command = command.substring(split.length() + 1).trim();
      try {
        String result = doAction("changePassword", command.split(split)[0], command.split(split)[1]);
        return "200 " + result;
      } catch (Exception e) {
        return "500 " + e;
      } 
    } 
    if (command.startsWith("SITE PGP_HEADER_SIZE")) {
      command = command.substring(command.indexOf(" ") + 1);
      command = command.substring(command.indexOf(" ") + 1);
      long size = Long.parseLong(command.substring(0, command.indexOf(" ")).trim());
      command = command.substring(command.indexOf(" ") + 1);
      String path = command.trim();
      try {
        String result = doAction("siteCommand", "PGP_HEADER_SIZE " + size + " " + path, "");
        return "214 " + result;
      } catch (Exception e) {
        return "500 " + e;
      } 
    } 
    if (command.startsWith("SITE BLOCK_UPLOADS"))
      try {
        String result = doAction("siteCommand", "BLOCK_UPLOADS", "");
        return "214 " + result;
      } catch (Exception e) {
        return "500 " + e;
      }  
    if (command.startsWith("SITE TIME"))
      try {
        String result = doAction("getTime", "", "");
        return "214 " + result;
      } catch (Exception e) {
        return "500 " + e;
      }  
    if (command.startsWith("SITE QUOTA"))
      try {
        String result = doAction("getQuota", command.substring("SITE QUOTA".length()).trim(), "");
        return "214 " + result;
      } catch (Exception e) {
        return "500 " + e;
      }  
    if (command.startsWith("ABOR"))
      try {
        String result = doAction("siteCommand", command, "");
        return "214 " + result;
      } catch (Exception e) {
        return "500 " + e;
      }  
    return "500 unknown command:" + command;
  }
}
