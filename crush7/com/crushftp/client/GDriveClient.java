package com.crushftp.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class GDriveClient extends GenericClient {
  String bearer = "";
  
  static Properties resourceIdCache = new Properties();
  
  public GDriveClient(String url, String header, Vector log) {
    super(header, log);
    System.setProperty("crushtunnel.debug", "2");
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
  }
  
  public static void main(String[] args) {}
  
  public String login2(String username, String password, String clientid) throws Exception {
    password = VRL.vrlDecode(password);
    this.config.put("username", username);
    this.config.put("password", password);
    Properties p = setup_bearer_refresh(password, username.split("~")[0], username.split("~")[1]);
    if (p.containsKey("access_token")) {
      this.bearer = p.getProperty("access_token");
      return "Success";
    } 
    return "Failure!";
  }
  
  public void logout() throws Exception {
    this.bearer = "";
    close();
  }
  
  public Vector list(String path, Vector list) throws Exception {
    SimpleDateFormat sdf_rfc1123_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path.substring(0, path.length() - 1));
    if (path.equals("/"))
      resourceId = "root"; 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files?q='" + resourceId + "'+in+parents&fields=files/id,files/name,files/size,files/mimeType,files/trashed,files/modifiedTime,files/parents"), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("GET");
    urlc.setRequestProperty("Authorization", "Bearer " + this.bearer);
    String json = Common.consumeResponse(urlc.getInputStream());
    Object obj = ((JSONObject)JSONValue.parse(json)).get("files");
    if (obj instanceof JSONArray) {
      JSONArray ja = (JSONArray)obj;
      for (int xxx = 0; xxx < ja.size(); xxx++) {
        Object obj2 = ja.get(xxx);
        if (obj2 instanceof JSONObject) {
          Properties item = new Properties();
          JSONObject jo = (JSONObject)obj2;
          boolean folder = false;
          if (jo.get("mimeType").equals("application/vnd.google-apps.folder"))
            folder = true; 
          Object[] a = jo.entrySet().toArray();
          for (int i = 0; i < a.length; i++) {
            String key2 = a[i].toString().split("=")[0];
            item.put(key2.trim(), jo.get(key2).trim());
          } 
          if (!item.getProperty("trashed", "").equals("true")) {
            Date d = sdf_rfc1123_2.parse(item.getProperty("modifiedTime"));
            String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + item.getProperty("size") + "   " + yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + item.getProperty("name");
            Properties stat = parseStat(line);
            stat.put("resource_id", item.getProperty("id"));
            stat.put("url", "gdrive://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@www.googleapis.com" + path + stat.getProperty("name"));
            resourceIdCache.put(String.valueOf(this.config.getProperty("password")) + path + stat.getProperty("name"), item.getProperty("id"));
            if (stat.getProperty("type", "").equalsIgnoreCase("DIR"))
              resourceIdCache.put(String.valueOf(this.config.getProperty("password")) + path + stat.getProperty("name") + "/", item.getProperty("id")); 
            list.addElement(stat);
          } 
        } 
      } 
    } 
    return list;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path);
    if (resourceId == null) {
      log("Download path not found:" + path + "\r\n");
      throw new IOException("Download path not found:" + path);
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files/" + resourceId + "?alt=media"), this.config);
    urlc.setDoOutput(false);
    doStandardDocsAlterations(urlc, (String)null);
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
    String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + Common.all_but_last(path));
    if (Common.all_but_last(path).equals("/"))
      resourceId = "root"; 
    if (resourceId == null) {
      log("Upload path not found:" + Common.all_but_last(path) + "\r\n");
      throw new IOException("Upload path not found:" + Common.all_but_last(path));
    } 
    try {
      delete(path);
    } catch (Exception exception) {}
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable"), this.config);
    urlc.setRequestMethod("POST");
    doStandardDocsAlterations(urlc, "application/json; charset=UTF-8");
    urlc.setDoOutput(true);
    JSONObject fileMetaInfo = new JSONObject();
    fileMetaInfo.put((K)"name", (V)Common.last(path));
    String[] folders = path.split("/");
    if (folders.length > 2) {
      JSONArray parents = new JSONArray();
      parents.add((E)resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + Common.all_but_last(path)));
      fileMetaInfo.put((K)"parents", (V)parents);
    } 
    urlc.setRequestProperty("X-Upload-Content-Type", "application/octet-stream");
    OutputStream out = urlc.getOutputStream();
    out.write(fileMetaInfo.toString().getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      throw new IOException(result);
    } 
    String postLocation = urlc.getHeaderField("Location");
    out = new GDriveClient$1$OutputWrapper(this, path, postLocation);
    return out;
  }
  
  public boolean delete(String path) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path);
    if (resourceId == null) {
      log("Delete path not found:" + path + "\r\n");
      return true;
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files/" + resourceId), new Properties());
    urlc.setDoOutput(false);
    urlc.setRequestMethod("DELETE");
    doStandardDocsAlterations(urlc, (String)null);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
    resourceIdCache.remove(String.valueOf(this.config.getProperty("password")) + path);
    return true;
  }
  
  public boolean makedir(String path) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files"), this.config);
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    doStandardDocsAlterations(urlc, "application/json; charset=UTF-8");
    JSONObject fileMetaInfo = new JSONObject();
    String[] folders = path.split("/");
    fileMetaInfo.put((K)"name", (V)folders[folders.length - 1]);
    fileMetaInfo.put((K)"mimeType", (V)"application/vnd.google-apps.folder");
    if (folders.length > 2) {
      JSONArray parents = new JSONArray();
      parents.add((E)resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + Common.all_but_last(path)));
      fileMetaInfo.put((K)"parents", (V)parents);
    } 
    OutputStream out = urlc.getOutputStream();
    out.write(fileMetaInfo.toString().getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
    Properties p = parse_json_reply(result);
    resourceIdCache.put(String.valueOf(this.config.getProperty("password")) + path, p.getProperty("id"));
    return true;
  }
  
  public boolean makedirs(String path) throws Exception {
    boolean ok = true;
    String[] parts = path.split("/");
    String path2 = "";
    for (int x = 0; x < parts.length && ok; x++) {
      path2 = String.valueOf(path2) + parts[x] + "/";
      if (x >= 1) {
        String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path2);
        if (resourceId == null)
          ok = makedir(path2); 
      } 
    } 
    return ok;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + rnfr);
    if (resourceId == null) {
      log("Delete path not found:" + rnfr + "\r\n");
      return true;
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files/" + resourceId), this.config);
    urlc.setDoOutput(true);
    urlc.setRequestMethod("PATCH");
    urlc.setRequestProperty("Authorization", "Bearer " + this.bearer);
    urlc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    JSONObject fileMetaInfo = new JSONObject();
    fileMetaInfo.put((K)"name", (V)Common.last(rnto));
    OutputStream out = urlc.getOutputStream();
    out.write(fileMetaInfo.toString().getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
    resourceIdCache.put(String.valueOf(this.config.getProperty("password")) + rnto, resourceIdCache.remove(String.valueOf(this.config.getProperty("password")) + rnfr));
    return true;
  }
  
  public Properties stat(String path) throws Exception {
    if (path.endsWith(":filetree"))
      path = path.substring(0, path.indexOf(":filetree") - 1); 
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
  
  public boolean mdtm(String path, long modified) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path);
    if (resourceId == null) {
      log("MDTM path not found:" + path + "\r\n");
      throw new IOException("MDTM path not found:" + path);
    } 
    SimpleDateFormat sdf_rfc1123_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files/" + resourceId + "?setModifiedDate=true&modifiedDate=" + sdf_rfc1123_2.format(new Date(modified))), new Properties());
    urlc.setDoOutput(false);
    urlc.setRequestMethod("PUT");
    doStandardDocsAlterations(urlc, (String)null);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
    resourceIdCache.remove(String.valueOf(this.config.getProperty("password")) + path);
    return true;
  }
  
  public void doStandardDocsAlterations(URLConnection urlc, String contentType) {
    urlc.setRequestProperty("Content-Type", contentType);
    urlc.setRequestProperty("Accept", null);
    urlc.setRequestProperty("Pragma", null);
    urlc.setRequestProperty("Cache", null);
    urlc.setRequestProperty("Cache-Control", null);
    urlc.setRequestProperty("Authorization", "Bearer " + this.bearer);
    urlc.setUseCaches(false);
  }
  
  public static Properties setup_bearer(String oauth_access_code, String server_url) {
    return setup_bearer(oauth_access_code, server_url, "994408646094-qhc4ism4cihfpjqve8hgv8jsm6jl5v7j.apps.googleusercontent.com", "bs0nKD4ZMBen-1mcUMcvzXns");
  }
  
  public static Properties setup_bearer(String oauth_access_code, String server_url, String google_client_id, String google_client_secret) {
    try {
      String full_form = "code=" + URLEncoder.encode(oauth_access_code, "UTF-8");
      full_form = String.valueOf(full_form) + "&client_id=" + google_client_id;
      full_form = String.valueOf(full_form) + "&client_secret=" + google_client_secret;
      full_form = String.valueOf(full_form) + "&redirect_uri=" + server_url;
      full_form = String.valueOf(full_form) + "&grant_type=authorization_code";
      byte[] b = full_form.getBytes("UTF8");
      URLConnection urlc = URLConnection.openConnection(new VRL("https://accounts.google.com/o/oauth2/token"), new Properties());
      urlc.setDoOutput(true);
      urlc.setRequestMethod("POST");
      OutputStream out = urlc.getOutputStream();
      out.write(full_form.getBytes("UTF8"));
      out.close();
      urlc.getResponseCode();
      return parse_json_reply(Common.consumeResponse(urlc.getInputStream()));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } 
  }
  
  public static Properties setup_bearer_refresh(String refresh_token, String google_client_id, String google_client_secret) {
    try {
      String full_form = "client_id=" + google_client_id;
      full_form = String.valueOf(full_form) + "&client_secret=" + google_client_secret;
      full_form = String.valueOf(full_form) + "&refresh_token=" + refresh_token;
      full_form = String.valueOf(full_form) + "&grant_type=refresh_token";
      URLConnection urlc = URLConnection.openConnection(new VRL("https://accounts.google.com/o/oauth2/token"), new Properties());
      urlc.setDoOutput(true);
      urlc.setRequestMethod("POST");
      OutputStream out = urlc.getOutputStream();
      out.write(full_form.getBytes("UTF8"));
      out.close();
      urlc.getResponseCode();
      Properties p = parse_json_reply(Common.consumeResponse(urlc.getInputStream()));
      p.put("refresh_token", refresh_token);
      return p;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } 
  }
  
  public static Properties parse_json_reply(String result) throws IOException {
    BufferedReader br = new BufferedReader(new StringReader(result));
    String line = "";
    Properties p = new Properties();
    try {
      while ((line = br.readLine()) != null) {
        if (line.indexOf(":") < 0)
          continue; 
        String key = line.split(":")[0].trim();
        if (key.indexOf("\"") >= 0)
          key = key.substring(1, key.lastIndexOf("\"")); 
        String val = line.split(":")[1].trim();
        if (val.indexOf("\"") >= 0)
          val = val.substring(1, val.lastIndexOf("\"")); 
        p.put(key, val);
      } 
    } catch (Exception e) {
      System.out.println(result);
      System.out.println(line);
    } 
    return p;
  }
}
