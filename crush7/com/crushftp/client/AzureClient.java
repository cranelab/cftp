package com.crushftp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class AzureClient extends GenericClient {
  public AzureClient(String url, String header, Vector log) {
    super(header, log);
    this.url = url;
    String[] parts = url.split("/");
    int share_index = this.url.indexOf("@file.core.windows.net/") + "@file.core.windows.net/".length();
    if (this.url.indexOf("/", share_index) < 0)
      this.url = String.valueOf(this.url) + "/"; 
    String share = this.url.substring(share_index, this.url.indexOf("/", share_index));
    this.config.put("share", share);
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.config.put("username", username.trim());
    this.config.put("key", VRL.vrlDecode(password.trim()));
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net/" + this.config.getProperty("share") + "?restype=share"), new Properties());
    urlc.setRequestMethod("GET");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    if (urlc.getResponseCode() == 200)
      return "Success"; 
    if (urlc.getResponseCode() < 200 || urlc.getResponseCode() > 299) {
      log(String.valueOf(urlc.getResponseCode()) + result + "\r\n");
      throw new IOException(result);
    } 
    return "Failure!";
  }
  
  public Vector list(String path, Vector list) throws Exception {
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + "?comp=list&restype=directory"), new Properties());
    urlc.setRequestMethod("GET");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    if (result.contains("<Entries>") && result.contains("</Entries>") && result.indexOf("<Entries>") < result.indexOf("</Entries>")) {
      result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + result.substring(result.indexOf("<Entries>"), result.indexOf("</Entries>") + "</Entries>".length());
      try {
        SAXBuilder sax = Common.getSaxBuilder();
        Document doc = sax.build(new StringReader(result));
        Iterator i = doc.getRootElement().getChildren().iterator();
        while (i.hasNext()) {
          Element element = i.next();
          Properties p = new Properties();
          if (element.getName().equals("File")) {
            p.put("permissions", "-rwxrwxrwx");
            p.put("type", "FILE");
          } else {
            p.put("type", "DIR");
            p.put("permissions", "drwxrwxrwx");
            p.put("check_all_recursive_deletes", "true");
          } 
          Iterator i2 = element.getChildren().iterator();
          p.put("size", "0");
          while (i2.hasNext()) {
            Element element2 = i2.next();
            if (element2.getName().equals("Name")) {
              p.put("name", element2.getText());
              p.put("path", String.valueOf(path) + element2.getText());
            } 
            if (element2.getName().equals("Properties") && element2.getChildren().size() > 0) {
              Element size = element2.getChild("Content-Length");
              p.put("size", size.getText());
            } 
          } 
          p.put("url", "azure://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("key")) + "@file.core.windows.net" + path + p.getProperty("name"));
          p.put("owner", "owner");
          p.put("group", "group");
          String restype = "directory";
          if (p.getProperty("type", "").equals("FILE"))
            restype = "file"; 
          String path2 = "";
          URLConnection urlc2 = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + p.getProperty("name") + "?comp=metadata&restype=" + restype), new Properties());
          urlc2.setRequestMethod("GET");
          urlc2.setDoInput(true);
          urlc2.setUseCaches(false);
          signRequestSK(urlc2);
          urlc2.connect();
          if (urlc.getResponseCode() != 200) {
            String result2 = URLConnection.consumeResponse(urlc2.getInputStream());
            log(String.valueOf(urlc.getResponseCode()) + result2);
          } 
          String last_modified = urlc2.getHeaderField("LAST-MODIFIED");
          SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
          Date date = fmt.parse(last_modified);
          p.put("modified", (new StringBuffer(String.valueOf(date.getTime()))).toString());
          list.add(p);
        } 
      } catch (Exception e) {
        log(e);
      } 
    } 
    return list;
  }
  
  public Properties stat(String path) throws Exception {
    if (path.endsWith(":filetree"))
      path = path.substring(0, path.indexOf(":filetree") - 1); 
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    if (path.equals("/" + this.config.getProperty("share"))) {
      Properties p_root = new Properties();
      p_root.put("name", "");
      p_root.put("path", path);
      p_root.put("type", "DIR");
      p_root.put("permissions", "drwxrwxrwx");
      p_root.put("size", "0");
      p_root.put("url", "azure://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("key")) + "@file.core.windows.net" + path);
      p_root.put("owner", "owner");
      p_root.put("group", "group");
      p_root.put("modified", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      return p_root;
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path), new Properties());
    urlc.setRequestMethod("GET");
    urlc.setDoInput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    Properties p = new Properties();
    if (urlc.getResponseCode() != 200) {
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      if (urlc.getResponseCode() == 404) {
        URLConnection urlc2 = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + "?comp=metadata&restype=directory"), new Properties());
        urlc2.setRequestMethod("GET");
        urlc2.setDoInput(true);
        urlc2.setUseCaches(false);
        signRequestSK(urlc2);
        urlc2.connect();
        if (urlc2.getResponseCode() == 200) {
          get_file_from_request_header(urlc2, path, p, false);
          return p;
        } 
        String result2 = URLConnection.consumeResponse(urlc2.getInputStream());
        log(String.valueOf(urlc2.getResponseCode()) + result2);
        return null;
      } 
      log(String.valueOf(urlc.getResponseCode()) + result);
      return null;
    } 
    get_file_from_request_header(urlc, path, p, true);
    return p;
  }
  
  private void get_file_from_request_header(URLConnection urlc, String path, Properties p, boolean is_file) throws ParseException {
    String name = path;
    if (name.endsWith("/"))
      name = name.substring(0, name.length() - 1); 
    name = name.substring(name.lastIndexOf("/") + 1, name.length());
    p.put("name", name);
    p.put("path", path);
    if (is_file) {
      p.put("permissions", "-rwxrwxrwx");
      p.put("type", "FILE");
      p.put("size", urlc.getHeaderField("CONTENT-LENGTH"));
    } else {
      p.put("type", "DIR");
      p.put("permissions", "drwxrwxrwx");
      p.put("size", "0");
      p.put("check_all_recursive_deletes", "true");
    } 
    p.put("url", "azure://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("key")) + "@file.core.windows.net" + path);
    p.put("owner", "owner");
    p.put("group", "group");
    String last_modified = urlc.getHeaderField("LAST-MODIFIED");
    SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    Date date = fmt.parse(last_modified);
    p.put("modified", (new StringBuffer(String.valueOf(date.getTime()))).toString());
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path), new Properties());
    urlc.setRequestMethod("GET");
    urlc.setDoInput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    if (urlc.getResponseCode() != 200) {
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      log(String.valueOf(urlc.getResponseCode()) + result);
      return null;
    } 
    this.in = urlc.getInputStream();
    return this.in;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    String path2 = path;
    if (!path.startsWith("/"))
      path2 = "/" + path; 
    String upload_path = path2;
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + upload_path), new Properties());
    urlc.setRequestMethod("PUT");
    urlc.setRequestProperty("x-ms-content-length", "0");
    urlc.setRequestProperty("x-ms-type", "file");
    urlc.setRequestProperty("Content-Length", "0");
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    if (urlc.getResponseCode() != 201) {
      log(String.valueOf(urlc.getResponseCode()) + result);
      return null;
    } 
    urlc.disconnect();
    this.out = new AzureClient$1$OutputWrapper(this, upload_path);
    return this.out;
  }
  
  public boolean delete(String path) throws Exception {
    Properties p = stat(path);
    String restype = "";
    if (p.getProperty("type").equals("FILE")) {
      restype = "file";
    } else {
      restype = "directory";
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + "?restype=" + restype), new Properties());
    urlc.setRequestMethod("DELETE");
    urlc.setDoInput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    if (urlc.getResponseCode() == 202)
      return true; 
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    log(String.valueOf(urlc.getResponseCode()) + result);
    return false;
  }
  
  public boolean makedirs(String path) throws Exception {
    boolean ok = true;
    String[] parts = path.split("/");
    String path2 = "";
    if (parts.length < 2 && !this.config.getProperty("share").equals(parts[1]))
      return false; 
    String share_part = "/" + parts[1] + "/";
    for (int x = 2; x < parts.length && ok; x++) {
      path2 = String.valueOf(path2) + parts[x] + "/";
      if (x >= 1)
        if (stat(String.valueOf(share_part) + path2) == null)
          ok = makedir(String.valueOf(share_part) + path2);  
    } 
    return ok;
  }
  
  public boolean makedir(String path) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + "?restype=directory"), new Properties());
    urlc.setRequestMethod("PUT");
    urlc.setRequestProperty("Content-Length", "0");
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    try {
      signRequestSK(urlc);
    } catch (Exception e1) {
      log(e1);
      return false;
    } 
    String result = "";
    try {
      result = Common.consumeResponse(urlc.getInputStream());
    } catch (Exception e2) {
      log(e2);
      return false;
    } 
    if (urlc.getResponseCode() == 201)
      return true; 
    log(String.valueOf(urlc.getResponseCode()) + result);
    return false;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    Properties p = stat(path);
    String restype = "";
    if (p.getProperty("type").equals("FILE")) {
      restype = "file";
    } else {
      restype = "directory";
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + "?comp=metadata&restype=" + restype), new Properties());
    urlc.setRequestMethod("PUT");
    SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    String date = String.valueOf(fmt.format(new Date())) + " GMT";
    urlc.setRequestProperty("Last-Modified", date);
    urlc.setRequestProperty("Content-Length", "0");
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    try {
      signRequestSK(urlc);
    } catch (Exception e1) {
      log(e1);
      return false;
    } 
    String result = "";
    try {
      result = Common.consumeResponse(urlc.getInputStream());
    } catch (Exception e2) {
      log(e2);
      return false;
    } 
    if (urlc.getResponseCode() == 200 || urlc.getResponseCode() == 202) {
      urlc.disconnect();
      return true;
    } 
    log(String.valueOf(urlc.getResponseCode()) + result);
    return false;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    log("Azure do not support rename functionality!");
    throw new Exception("Azure do not support rename functionality!");
  }
  
  public void signRequestSK(URLConnection urlc) throws Exception {
    SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    String date = String.valueOf(fmt.format(Calendar.getInstance().getTime())) + " GMT";
    String sb = "";
    sb = String.valueOf(sb) + urlc.getRequestMethod() + "\n";
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    if (urlc.getRequestProps().getProperty("Content-Length") != null && !urlc.getRequestProps().getProperty("Content-Length").equals("0")) {
      sb = String.valueOf(sb) + urlc.getRequestProps().getProperty("Content-Length") + "\n";
    } else {
      sb = String.valueOf(sb) + "\n";
    } 
    sb = String.valueOf(sb) + "\n";
    if (urlc.getRequestProps().getProperty("Content-Type") != null && !urlc.getRequestProps().getProperty("Content-Type").equals("0")) {
      sb = String.valueOf(sb) + urlc.getRequestProps().getProperty("Content-Type") + "\n";
    } else {
      sb = String.valueOf(sb) + "application/x-www-form-urlencoded; charset=UTF-8\n";
    } 
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    if (urlc.getRequestProps().getProperty("x-ms-content-length") != null)
      sb = String.valueOf(sb) + "x-ms-content-length:" + urlc.getRequestProps().getProperty("x-ms-content-length") + "\n"; 
    sb = String.valueOf(sb) + "x-ms-date:" + date + "\n";
    if (urlc.getRequestProps().getProperty("x-ms-range") != null)
      sb = String.valueOf(sb) + "x-ms-range:" + urlc.getRequestProps().getProperty("x-ms-range") + "\n"; 
    if (urlc.getRequestProps().getProperty("x-ms-type") != null)
      sb = String.valueOf(sb) + "x-ms-type:" + urlc.getRequestProps().getProperty("x-ms-type") + "\n"; 
    sb = String.valueOf(sb) + "x-ms-version:2015-12-11\n";
    if (urlc.getRequestProps().getProperty("x-ms-write") != null)
      sb = String.valueOf(sb) + "x-ms-write:" + urlc.getRequestProps().getProperty("x-ms-write") + "\n"; 
    String path = urlc.getURL().getPath();
    if (path.contains("?")) {
      String path2 = Common.url_encode(path.substring(0, path.indexOf("?")), "/%.#@&!\\=+");
      sb = String.valueOf(sb) + "/" + this.config.getProperty("username") + path2 + "\n";
      String commands = path.substring(path.indexOf("?") + 1, path.length());
      sb = String.valueOf(sb) + commands.replaceAll("&", "\n").replaceAll("=", ":");
    } else {
      sb = String.valueOf(sb) + "/" + this.config.getProperty("username") + Common.url_encode(path, "/%.#@&!\\=+");
    } 
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(Base64.decode(this.config.getProperty("key")), "HmacSHA256"));
    String authKey = new String(Base64.encodeBytes(mac.doFinal(sb.toString().getBytes("UTF-8"))));
    String auth = "SharedKey " + this.config.getProperty("username") + ":" + authKey;
    urlc.setRequestProperty("x-ms-date", date);
    urlc.setRequestProperty("x-ms-version", "2015-12-11");
    urlc.setRequestProperty("Authorization", auth);
  }
}
