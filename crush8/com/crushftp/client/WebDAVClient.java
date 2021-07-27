package com.crushftp.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class WebDAVClient extends GenericClient {
  String auth = null;
  
  public WebDAVClient(String url, String header, Vector log) {
    super(header, log);
    if (url.endsWith("/"))
      url = url.substring(0, url.lastIndexOf("/")); 
    this.url = url;
    this.config.put("protocol", "DAV");
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.config.put("username", username);
    this.config.put("password", password);
    if (clientid != null)
      this.config.put("clientid", clientid); 
    this.auth = "Basic " + Base64.encodeBytes((String.valueOf(username) + ":" + password).getBytes());
    return "";
  }
  
  public Vector list(String path, Vector v) throws Exception {
    String xml = doAction("list", path, "1");
    Vector v2 = parseXml(xml, 1);
    v.addAll(v2);
    return v;
  }
  
  public boolean delete(String path) throws Exception {
    return doAction("delete", path, "").equals("");
  }
  
  public boolean makedir(String path) throws Exception {
    String result = doAction("makedir", path, "");
    if (result.toUpperCase().indexOf("CREATED") >= 0)
      return true; 
    return result.equals("");
  }
  
  public boolean makedirs(String path) throws Exception {
    return makedir(path);
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    return doAction("rename", rnfr, rnto).equals("");
  }
  
  public Properties stat(String path) throws Exception {
    Vector v = parseXml(doAction("list", path, "0"), 0);
    if (v.size() > 0)
      return v.elementAt(0); 
    if (System.getProperty("crushftp.isTestCall" + Thread.currentThread().getId(), "false").equals("true"))
      throw new Exception("Item not found..." + path); 
    return null;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    return doAction("mdtm", path, (new StringBuffer(String.valueOf(modified))).toString()).equals("");
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.url) + path), this.config);
    urlc.autoClose = true;
    urlc.setRequestMethod("GET");
    urlc.setRequestProperty("Authorization", this.auth);
    if (startPos >= 0L)
      urlc.setRequestProperty("Range", "bytes=" + startPos + "-" + ((endPos >= 0L) ? (new StringBuffer(String.valueOf(endPos))).toString() : "")); 
    urlc.setUseCaches(false);
    this.in = urlc.getInputStream();
    return this.in;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    log("PUT:" + (new VRL(String.valueOf(this.url) + path)).getHost() + ":" + (new VRL(String.valueOf(this.url) + path)).getPort());
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.url) + path), this.config);
    urlc.autoClose = true;
    urlc.setRequestMethod("PUT");
    urlc.setRequestProperty("Authorization", this.auth);
    if (startPos >= 0L)
      urlc.setRequestProperty("Range", "bytes=" + startPos + "-"); 
    urlc.setUseCaches(false);
    urlc.setDoOutput(true);
    urlc.setUseChunkedStreaming(true);
    urlc.setRequestProperty("Content-Type", "application/binary");
    this.out = urlc.getOutputStream();
    this.out = new WebDAVClient$1$OutputWrapperHttp(this, this.out, urlc);
    return this.out;
  }
  
  private String doAction(String command, String path, String param1) throws Exception {
    VRL u = new VRL(this.url);
    log("PUT:" + u.getHost() + ":" + u.getPort() + ":" + command + " | " + path + " | " + param1);
    if (path.startsWith(u.getPath()))
      path = path.substring(u.getPath().length()); 
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.url) + path), this.config);
    urlc.autoClose = true;
    urlc.setRequestProperty("Authorization", this.auth);
    urlc.setUseCaches(false);
    if (command.equalsIgnoreCase("delete")) {
      urlc.setRequestMethod("DELETE");
    } else if (command.equalsIgnoreCase("makedir")) {
      urlc.setRequestMethod("MKCOL");
    } else if (command.equalsIgnoreCase("rename")) {
      urlc.setRequestMethod("MOVE");
      if (this.config.getProperty("webdav_full_url_on_rename", "false").equals("true")) {
        String temp_url = Common.replace_str(this.url, "WEBDAVS://", "https://");
        temp_url = Common.replace_str(temp_url, "webdavs://", "https://");
        temp_url = Common.replace_str(temp_url, "WEBDAV://", "http://");
        temp_url = Common.replace_str(temp_url, "webdav://", "http://");
        urlc.setRequestProperty("Destination", String.valueOf(temp_url) + param1);
      } else {
        urlc.setRequestProperty("Destination", param1);
      } 
    } else if (command.equalsIgnoreCase("list")) {
      urlc.setRequestMethod("PROPFIND");
      urlc.setRequestProperty("Depth", param1);
      urlc.setRequestProperty("Content-Type", "application/xml");
      urlc.setDoOutput(true);
      String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> \r\n<a:propfind xmlns:a=\"DAV:\">\r\n<a:prop>\r\n<a:href/>\r\n<a:getlastmodified/>\r\n<a:getcontentlength/>\r\n<a:resourcetype/>\r\n</a:prop>\r\n</a:propfind>\r\n";
      byte[] b = xml.getBytes("UTF8");
      urlc.setLength(b.length);
      urlc.getOutputStream().write(b);
    } else if (command.equalsIgnoreCase("mdtm")) {
      urlc = URLConnection.openConnection(new VRL(String.valueOf(this.url) + "/WebInterface/function/"), this.config);
      urlc.autoClose = true;
      urlc.setRequestMethod("POST");
      urlc.setRequestProperty("Authorization", this.auth);
      urlc.setRequestProperty("Content-Type", "application/xml");
      urlc.setDoOutput(true);
      String xml = "command=mdtm&path=" + HTTPClient.u(path) + "&date=" + HTTPClient.u((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date(Long.parseLong(param1))));
      byte[] b = xml.getBytes("UTF8");
      urlc.setLength(b.length);
      urlc.getOutputStream().write(b);
    } 
    int code = urlc.getResponseCode();
    if (urlc.getURL().toString().indexOf("/WebInterface/login.html") >= 0)
      code = 302; 
    if (code == 302)
      throw new Exception("Logged out."); 
    if (code == 401)
      throw new Exception("Unauthorized"); 
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (!result.equals("") && (command.equalsIgnoreCase("rename") || command.equalsIgnoreCase("makedir") || command.equalsIgnoreCase("delete")) && code >= 200 && code < 300) {
      Common.log("HTTP_CLIENT", 2, result);
      result = "";
    } 
    return result;
  }
  
  public Vector parseXml(String xml, int skipCount) throws Exception {
    Vector list = new Vector();
    try {
      SAXBuilder sax = Common.getSaxBuilder();
      Document doc = sax.build(new StringReader(xml));
      Iterator i = doc.getRootElement().getChildren().iterator();
      int count = 0;
      while (i.hasNext()) {
        Element response = i.next();
        if (count++ >= skipCount) {
          Iterator i2 = response.getChildren().iterator();
          Properties p = new Properties();
          p.put("url", this.url);
          p.put("owner", "owner");
          p.put("num_items", "1");
          p.put("group", "group");
          p.put("size", "0");
          while (i2.hasNext()) {
            Element element = i2.next();
            if (element.getName().equalsIgnoreCase("href")) {
              p.put("name", Common.last(element.getText()));
              p.put("path", Common.all_but_last(element.getText()));
              String new_path = element.getText();
              if (new_path.toUpperCase().startsWith("HTTP:") || new_path.toUpperCase().startsWith("HTTPS:"))
                new_path = (new VRL(new_path)).getPath(); 
              p.put("url", String.valueOf(this.url) + new_path);
              continue;
            } 
            if (element.getName().equalsIgnoreCase("propstat")) {
              Properties holder = new Properties();
              holder.put("type", "FILE");
              holder.put("permissions", "-rwxrwxrwx");
              Element propstat = element;
              Element status = propstat.getChild("status", propstat.getNamespace());
              Element prop = propstat.getChild("prop", propstat.getNamespace());
              if (prop != null) {
                Element getlastmodified = prop.getChild("getlastmodified", propstat.getNamespace());
                Element getcontentlength = prop.getChild("getcontentlength", propstat.getNamespace());
                Element resourcetype = prop.getChild("resourcetype", propstat.getNamespace());
                if (getlastmodified != null) {
                  holder.put("modified", (new StringBuffer(String.valueOf(this.rfc1123.parse(getlastmodified.getText()).getTime()))).toString());
                  Date d = new Date(Long.parseLong(holder.getProperty("modified")));
                  holder.put("month", this.mmm.format(d));
                  holder.put("day", this.dd.format(d));
                  String time_or_year = this.hhmm.format(d);
                  if (!this.yyyy.format(d).equals(this.yyyy.format(new Date())))
                    time_or_year = this.yyyy.format(d); 
                  holder.put("time_or_year", time_or_year);
                } 
                if (getcontentlength != null)
                  holder.put("size", getcontentlength.getText()); 
                if (resourcetype != null) {
                  Element collection = resourcetype.getChild("collection", propstat.getNamespace());
                  if (collection != null) {
                    holder.put("type", "DIR");
                    holder.put("permissions", "drwxrwxrwx");
                    if (p.getProperty("name", "").endsWith("/"))
                      p.put("name", p.getProperty("name").substring(0, p.getProperty("name").length() - 1)); 
                  } 
                } 
                if (status != null && status.getText().indexOf(" 200 ") >= 0) {
                  p.putAll(holder);
                  p.put("name", Common.url_decode(p.getProperty("name")));
                  if (p.getProperty("type").equalsIgnoreCase("DIR"))
                    p.put("size", "1"); 
                  list.addElement(p);
                } 
              } 
            } 
          } 
        } 
      } 
    } catch (Exception e) {
      Common.log("HTTP_CLIENT", 1, e);
    } 
    return list;
  }
}
