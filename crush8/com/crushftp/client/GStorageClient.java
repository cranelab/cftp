package com.crushftp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class GStorageClient extends GenericClient {
  String bearer = "";
  
  String bucketName = "";
  
  SimpleDateFormat sdf_rfc1123_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
  
  SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  
  public GStorageClient(String url, String header, Vector log) {
    super(header, log);
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    password = VRL.vrlDecode(password);
    this.config.put("username", username);
    this.config.put("password", password);
    Properties p = Common.google_renew_tokens(password, username.split("~")[0], username.split("~")[1]);
    if (p.containsKey("expires_in")) {
      this.config.put("token_start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      String expire_in = p.getProperty("expires_in");
      if (expire_in.endsWith(","))
        expire_in = expire_in.substring(0, expire_in.length() - 1); 
      this.config.put("token_expire", expire_in);
    } 
    if (p.containsKey("access_token")) {
      this.bearer = p.getProperty("access_token");
    } else if (p.containsKey("refresh_token")) {
      this.bearer = p.getProperty("refresh_token");
    } 
    String path0 = (new VRL(this.url)).getPath();
    try {
      this.bucketName = path0.substring(1, path0.indexOf("/", 1));
    } catch (Exception e) {
      log(e);
      throw new Exception("Error : Wrong bucket!");
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://storage.googleapis.com/storage/v1/b/" + this.bucketName), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("GET");
    urlc.setRequestProperty("Authorization", "Bearer " + getBearer());
    String result = Common.consumeResponse(urlc.getInputStream());
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      log(result);
      throw new IOException(result);
    } 
    return "Success";
  }
  
  public Vector list(String path, Vector list) throws Exception {
    return list(path, list, "%2F", true);
  }
  
  public Vector list(String path, Vector list, String delimiter, boolean includeDelimiter) throws Exception {
    String path0 = path.substring(path.indexOf(String.valueOf(this.bucketName) + "/") + (String.valueOf(this.bucketName) + "/").length());
    if (path0.startsWith("/"))
      path0 = path0.substring(1); 
    if (!path0.equals("") && !path0.endsWith("/"))
      path0 = String.valueOf(path0) + "/"; 
    String prefix = Common.url_encode(path0);
    String next = "";
    String pageToken = "";
    int count = 0;
    do {
      count++;
      if (!next.equals(""))
        pageToken = "&pageToken=" + next; 
      next = "";
      URLConnection urlc = URLConnection.openConnection(new VRL("https://storage.googleapis.com/storage/v1/b/" + this.bucketName + "/o" + "?delimiter=" + delimiter + "&maxResults=1000&includeTrailingDelimiter=" + includeDelimiter + "&versions=false&prefix=" + prefix + pageToken), new Properties());
      urlc.setDoOutput(false);
      urlc.setRequestMethod("GET");
      urlc.setRequestProperty("Authorization", "Bearer " + getBearer());
      int code = urlc.getResponseCode();
      if (code < 200 || code > 299) {
        String str = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        log(String.valueOf(str) + "\r\n");
        throw new Exception(str);
      } 
      String result = Common.consumeResponse(urlc.getInputStream());
      JSONObject obj = (JSONObject)JSONValue.parse(result);
      if (obj.containsKey("nextPageToken")) {
        next = obj.get("nextPageToken").toString();
      } else {
        next = "";
      } 
      Object obj2 = obj.get("items");
      if (!(obj2 instanceof JSONArray))
        continue; 
      JSONArray ja = (JSONArray)obj2;
      for (int xxx = 0; xxx < ja.size(); xxx++) {
        Object team = ja.get(xxx);
        if (team instanceof JSONObject) {
          Properties item = new Properties();
          JSONObject jo = (JSONObject)team;
          Object[] a = jo.entrySet().toArray();
          for (int i = 0; i < a.length; i++) {
            String key2 = a[i].toString().split("=")[0];
            item.put(key2.trim(), jo.get(key2).trim());
          } 
          if (!item.getProperty("name").equals(path0)) {
            String objectName = item.getProperty("name");
            boolean folder = item.getProperty("name").endsWith("/");
            if (folder)
              item.put("name", item.getProperty("name").substring(0, item.getProperty("name").length() - 1)); 
            item.put("name", Common.last(item.getProperty("name")));
            Date d = new Date();
            try {
              d = this.sdf_rfc1123_2.parse(item.getProperty("updated"));
            } catch (Exception e) {
              log(e);
            } 
            String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + item.getProperty("size") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + item.getProperty("name");
            Properties stat = parseStat(line);
            stat.put("obejct_name", objectName);
            stat.put("url", "gstorage://" + (String)getConfig("username") + ":" + VRL.vrlEncode((String)getConfig("password")) + "@storage.googleapis.com" + path + stat.getProperty("name"));
            list.addElement(stat);
          } 
        } 
      } 
    } while (count != 100 && !next.equals(""));
    return list;
  }
  
  public Properties stat(String path) throws Exception {
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Vector v = new Vector();
    list(Common.all_but_last(path), v);
    for (int x = 0; x < v.size(); x++) {
      Properties p = v.elementAt(x);
      if (p.getProperty("name").equals(Common.last(path)))
        return p; 
    } 
    return null;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    String path0 = path.substring(path.indexOf(String.valueOf(this.bucketName) + "/") + (String.valueOf(this.bucketName) + "/").length());
    if (path0.startsWith("/"))
      path0 = path0.substring(1); 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://storage.googleapis.com/storage/v1/b/" + this.bucketName + "/o/" + Common.url_decode(path0) + "?alt=media"), new Properties());
    urlc.setDoOutput(false);
    urlc.setRequestMethod("GET");
    urlc.setRequestProperty("Authorization", "Bearer " + getBearer());
    if (startPos > 0L || endPos >= 0L)
      urlc.setRequestProperty("Range", "bytes=" + startPos + "-" + ((endPos >= 0L) ? (new StringBuffer(String.valueOf(endPos))).toString() : "")); 
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      log(String.valueOf(result) + "\r\n");
      throw new Exception(result);
    } 
    this.in = urlc.getInputStream();
    return this.in;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    String path0 = path.substring(path.indexOf(String.valueOf(this.bucketName) + "/") + (String.valueOf(this.bucketName) + "/").length());
    if (path0.startsWith("/"))
      path0 = path0.substring(1); 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://storage.googleapis.com/upload/storage/v1/b/" + this.bucketName + "/o?uploadType=resumable"), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + getBearer());
    urlc.setRequestProperty("X-Upload-Content-Type", "application/octet-stream");
    urlc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    JSONObject fileMetaInfo = new JSONObject();
    fileMetaInfo.put((K)"name", (V)Common.url_decode(path0));
    OutputStream out = urlc.getOutputStream();
    out.write(fileMetaInfo.toString().getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      String result = Common.consumeResponse(urlc.getInputStream());
      throw new IOException(result);
    } 
    String uploadLocation = urlc.getHeaderField("Location");
    out = new null.OutputWrapper(this, uploadLocation);
    return out;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    log("Google stortage does not support mdtm modifications on objects!");
    return true;
  }
  
  public boolean makedir(String path) throws Exception {
    String path0 = path.substring(path.indexOf(String.valueOf(this.bucketName) + "/") + (String.valueOf(this.bucketName) + "/").length());
    if (path0.startsWith("/"))
      path0 = path0.substring(1); 
    if (!path0.endsWith("/"))
      path0 = String.valueOf(path0) + "/"; 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://storage.googleapis.com/upload/storage/v1/b/" + this.bucketName + "/o?uploadType=media&name=" + Common.url_encode(path0)), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + getBearer());
    urlc.setRequestProperty("Content-Type", "Folder");
    urlc.setRequestProperty("Content-Length", "0");
    urlc.getOutputStream().close();
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      String result = Common.consumeResponse(urlc.getInputStream());
      log("Delete Error :" + result);
      return false;
    } 
    return true;
  }
  
  public boolean makedirs(String path) throws Exception {
    boolean ok = true;
    String[] parts = path.split("/");
    String path2 = "";
    for (int x = 0; x < parts.length && ok; x++) {
      path2 = String.valueOf(path2) + parts[x] + "/";
      if (x >= 2)
        if (stat(path2) == null)
          ok = makedir(path2);  
    } 
    return ok;
  }
  
  public boolean delete(String path) throws Exception {
    String path0 = path.substring(path.indexOf(String.valueOf(this.bucketName) + "/") + (String.valueOf(this.bucketName) + "/").length());
    if (path0.startsWith("/"))
      path0 = path0.substring(1); 
    Properties p = stat(path);
    Vector list = new Vector();
    if (p.getProperty("type", "FILE").equalsIgnoreCase("DIR") && !path0.endsWith("/")) {
      if (!path0.endsWith("/"))
        path0 = String.valueOf(path0) + "/"; 
      list(path, list, "", false);
      for (int x = list.size() - 1; x >= 0; x--) {
        Properties item = list.get(x);
        if (!deleteObject(item.getProperty("obejct_name")))
          return false; 
      } 
      deleteObject(path0);
      return true;
    } 
    return deleteObject(path0);
  }
  
  private boolean deleteObject(String path) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL("https://storage.googleapis.com/storage/v1/b/" + this.bucketName + "/o/" + double_encode(path)), new Properties());
    urlc.setDoOutput(false);
    urlc.setDoInput(true);
    urlc.setDoOutput(false);
    urlc.setUseCaches(false);
    urlc.setRequestMethod("DELETE");
    urlc.setRequestProperty("Authorization", "Bearer " + getBearer());
    urlc.setRemoveDoubleEncoding(true);
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      String result = Common.consumeResponse(urlc.getInputStream());
      log("Delete Error :" + result);
      return false;
    } 
    return true;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    boolean result = true;
    String path_rnfr = rnfr.substring(rnfr.indexOf(String.valueOf(this.bucketName) + "/") + (String.valueOf(this.bucketName) + "/").length());
    if (path_rnfr.startsWith("/"))
      path_rnfr = path_rnfr.substring(1); 
    String path_rnto = rnto.substring(rnto.indexOf(String.valueOf(this.bucketName) + "/") + (String.valueOf(this.bucketName) + "/").length());
    if (path_rnto.startsWith("/"))
      path_rnto = path_rnto.substring(1); 
    Properties p = stat(rnfr);
    if (p.getProperty("type", "FILE").equalsIgnoreCase("DIR")) {
      Vector list = new Vector();
      if (!path_rnfr.endsWith("/"))
        path_rnfr = String.valueOf(path_rnfr) + "/"; 
      if (!path_rnto.endsWith("/"))
        path_rnto = String.valueOf(path_rnto) + "/"; 
      list(rnfr, list, "", false);
      for (int x = list.size() - 1; x >= 0; x--) {
        Properties item = list.get(x);
        String subrnfr = item.getProperty("obejct_name");
        String subrnto = subrnfr.replaceAll(path_rnfr, path_rnto);
        if (!renameObject(subrnfr, subrnto))
          return false; 
      } 
      result = renameObject(path_rnfr, path_rnto);
      if (!result)
        return false; 
    } else {
      result = renameObject(path_rnfr, path_rnto);
      if (!result)
        return false; 
    } 
    delete(rnfr);
    return result;
  }
  
  private boolean renameObject(String path_rnfr, String path_rnto) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL("https://storage.googleapis.com/storage/v1/b/" + this.bucketName + "/o/" + double_encode(path_rnfr) + "/rewriteTo/b/" + this.bucketName + "/o/" + double_encode(path_rnto)), new Properties());
    urlc.setDoOutput(false);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + getBearer());
    urlc.setRequestProperty("Content-Length", "0");
    urlc.setRemoveDoubleEncoding(true);
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      String result = Common.consumeResponse(urlc.getInputStream());
      log(result);
      return false;
    } 
    return true;
  }
  
  public void doStandardDocsAlterations(URLConnection urlc, String contentType) throws Exception {
    urlc.setRequestProperty("Content-Type", contentType);
    urlc.setRequestProperty("Accept", null);
    urlc.setRequestProperty("Pragma", null);
    urlc.setRequestProperty("Cache", null);
    urlc.setRequestProperty("Cache-Control", null);
    urlc.setRequestProperty("Authorization", "Bearer " + getBearer());
    urlc.setUseCaches(false);
  }
  
  private String getBearer() throws Exception {
    if (this.config.containsKey("token_start") && this.config.containsKey("token_expire") && System.currentTimeMillis() - Long.parseLong(this.config.getProperty("token_start")) > (Long.parseLong(this.config.getProperty("token_expire")) - 600L) * 1000L) {
      Properties p = Common.google_renew_tokens(this.config.getProperty("password"), this.config.getProperty("username").split("~")[0], this.config.getProperty("username").split("~")[1]);
      if (p.containsKey("access_token")) {
        this.bearer = p.getProperty("access_token");
        this.config.put("token_start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      } else if (p.containsKey("refresh_token")) {
        this.bearer = p.getProperty("refresh_token");
        this.config.put("token_start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      } 
    } 
    return this.bearer;
  }
  
  private String double_encode(String text) {
    if (text.contains("/"))
      text = text.replace("/", "%252F"); 
    if (text.contains("+"))
      text = text.replace("+", "%252B"); 
    if (text.contains("="))
      text = text.replace("=", "%253D"); 
    if (text.contains("#"))
      text = text.replace("#", "%2523"); 
    if (text.contains("~"))
      text = text.replace("~", "%257E"); 
    if (text.contains("!"))
      text = text.replace("!", "%2521"); 
    if (text.contains("\\"))
      text = text.replace("\\", "%255C"); 
    if (text.contains("%2F"))
      text = text.replace("%2F", "%252F"); 
    if (text.contains("%2B"))
      text = text.replace("%2B", "%252B"); 
    if (text.contains("%3D"))
      text = text.replace("%3D", "%253D"); 
    if (text.contains("%23"))
      text = text.replace("%23", "%2523"); 
    if (text.contains("%7E"))
      text = text.replace("%7E", "%257E"); 
    if (text.contains("%21"))
      text = text.replace("%21", "%2521"); 
    if (text.contains("%5C"))
      text = text.replace("%5C", "%255C"); 
    return text;
  }
}
