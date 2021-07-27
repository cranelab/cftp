package com.crushftp.client;

import java.io.BufferedReader;
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

public class BoxClient extends GenericClient {
  static Properties resourceIdCache = new Properties();
  
  String bearer = "";
  
  public BoxClient(String url, String header, Vector log) {
    super(header, log);
    System.setProperty("crushtunnel.debug", "2");
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    password = VRL.vrlDecode(password);
    this.config.put("username", username);
    this.config.put("password", password);
    Properties p = setup_bearer_refresh(password);
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
    SimpleDateFormat sdf_rfc1123_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    String resourceId = resourceIdCache.getProperty(String.valueOf(getConfig("password")) + path.substring(0, path.length() - 1));
    if (path.equals("/"))
      resourceId = "0"; 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.box.com/2.0/folders/" + resourceId + "/items?fields=name,id,type,modified_at,item_status,size&limit=1000&offset=0"), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("GET");
    urlc.setRequestProperty("Authorization", "Bearer " + this.bearer);
    String json = Common.consumeResponse(urlc.getInputStream());
    Object obj = ((JSONObject)JSONValue.parse(json)).get("entries");
    if (obj instanceof JSONArray) {
      JSONArray ja = (JSONArray)obj;
      for (int xxx = 0; xxx < ja.size(); xxx++) {
        Object obj2 = ja.get(xxx);
        if (obj2 instanceof JSONObject) {
          Properties item = new Properties();
          JSONObject jo = (JSONObject)obj2;
          boolean folder = false;
          if (jo.get("type").equals("folder"))
            folder = true; 
          Object[] a = jo.entrySet().toArray();
          for (int i = 0; i < a.length; i++) {
            String key2 = a[i].toString().split("=")[0];
            item.put(key2.trim(), jo.get(key2).trim());
          } 
          if (item.getProperty("item_status", "").equals("active")) {
            Date d = sdf_rfc1123_2.parse(item.getProperty("modified_at"));
            String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + item.getProperty("size") + "   " + yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + item.getProperty("name");
            Properties stat = parseStat(line);
            stat.put("resource_id", item.getProperty("id"));
            stat.put("url", "box://" + (String)getConfig("username") + ":" + VRL.vrlEncode((String)getConfig("password")) + "@www.googleapis.com" + path + stat.getProperty("name"));
            resourceIdCache.put(String.valueOf(getConfig("password")) + path + stat.getProperty("name"), item.getProperty("id"));
            if (stat.getProperty("type", "").equalsIgnoreCase("DIR"))
              resourceIdCache.put(String.valueOf(getConfig("password")) + path + stat.getProperty("name") + "/", item.getProperty("id")); 
            list.addElement(stat);
          } 
        } 
      } 
    } 
    return list;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    try {
      delete(path);
    } catch (Exception exception) {}
    String filePath = path;
    if (path.startsWith("/"))
      filePath = path.substring(1, path.length()); 
    System.out.println("filePath: " + filePath);
    Properties config = new Properties();
    URLConnection urlc = URLConnection.openConnection(new VRL("https://dav.box.com/dav/" + filePath), config);
    urlc.setDoOutput(true);
    urlc.setUseCaches(true);
    urlc.setRequestMethod("PUT");
    urlc.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes("zolkriv2@gmail.com:semmivagyok123".getBytes()));
    urlc.setRequestProperty("Accept", "*/*");
    urlc.setRequestProperty("Content-Type", null);
    urlc.setRequestProperty("Date", null);
    urlc.setUseChunkedStreaming(true);
    urlc.setExpect100(true);
    this.out = new BoxClient$1$OutputWrapper(this, urlc);
    return this.out;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(getConfig("password")) + path);
    if (resourceId == null) {
      log("Download path not found:" + path + "\r\n");
      throw new IOException("Download path not found:" + path);
    } 
    Properties config = new Properties();
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.box.com/2.0/files/" + resourceId + "/content/"), config);
    urlc.setDoOutput(false);
    urlc.setRequestMethod("GET");
    urlc.setRequestProperty("Content-Type", null);
    urlc.setRequestProperty("Accept", null);
    urlc.setRequestProperty("Pragma", null);
    urlc.setRequestProperty("Cache", null);
    urlc.setRequestProperty("Cache-Control", null);
    urlc.setRequestProperty("Authorization", "Bearer " + this.bearer);
    urlc.setUseCaches(false);
    if (startPos > 0L || endPos >= 0L)
      urlc.setRequestProperty("Range", "bytes=" + startPos + "-" + ((endPos >= 0L) ? (new StringBuffer(String.valueOf(endPos))).toString() : "")); 
    int code = urlc.getResponseCode();
    if (code < 200 || code > 303) {
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      log(String.valueOf(result) + "\r\n");
      throw new Exception(result);
    } 
    URLConnection urlc2 = URLConnection.openConnection(new VRL(urlc.getHeaderField("LOCATION")), config);
    urlc2.setDoOutput(false);
    urlc2.setRequestMethod("GET");
    urlc2.setRequestProperty("Content-Type", null);
    urlc2.setRequestProperty("Accept", null);
    urlc2.setRequestProperty("Pragma", null);
    urlc2.setRequestProperty("Cache", null);
    urlc2.setRequestProperty("Cache-Control", null);
    urlc2.setUseCaches(false);
    urlc.disconnect();
    int code2 = urlc2.getResponseCode();
    if (code2 < 200 || code > 303) {
      String result = URLConnection.consumeResponse(urlc2.getInputStream());
      urlc2.disconnect();
      log(String.valueOf(result) + "\r\n");
      throw new Exception(result);
    } 
    this.in = urlc2.getInputStream();
    return this.in;
  }
  
  public boolean delete(String path) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(getConfig("password")) + path);
    if (resourceId == null) {
      log("Delete path not found:" + path + "\r\n");
      return true;
    } 
    String url = "https://api.box.com/2.0/files/" + resourceId;
    String folderPropertyId = String.valueOf(getConfig("password")) + path + "/";
    if (path.endsWith("/"))
      folderPropertyId.substring(0, folderPropertyId.length() - 1); 
    if (resourceIdCache.getProperty(folderPropertyId) != null)
      url = "https://api.box.com/2.0/folders/" + resourceId + "?recursive=true"; 
    URLConnection urlc = URLConnection.openConnection(new VRL(url), new Properties());
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
    resourceIdCache.remove(String.valueOf(getConfig("password")) + path);
    return true;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(getConfig("password")) + rnfr);
    if (resourceId == null) {
      log("Delete path not found:" + rnfr + "\r\n");
      return true;
    } 
    boolean is_folder = false;
    String url = "https://api.box.com/2.0/files/" + resourceId;
    if (resourceIdCache.getProperty(String.valueOf(getConfig("password")) + rnfr + "/") != null) {
      url = "https://api.box.com/2.0/folders/" + resourceId;
      is_folder = true;
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL(url), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("PUT");
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
    resourceIdCache.put(String.valueOf(getConfig("password")) + rnto, resourceIdCache.remove(String.valueOf(getConfig("password")) + rnfr));
    if (is_folder)
      resourceIdCache.put(String.valueOf(getConfig("password")) + rnto + "/", resourceIdCache.remove(String.valueOf(getConfig("password")) + rnfr + "/")); 
    return true;
  }
  
  public boolean makedir(String path) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.box.com/2.0/folders/"), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    doStandardDocsAlterations(urlc, "application/json; charset=UTF-8");
    JSONObject fileMetaInfo = new JSONObject();
    String[] folders = path.split("/");
    fileMetaInfo.put((K)"name", (V)folders[folders.length - 1]);
    String parent_id = "0";
    if (Common.all_but_last(path).length() > 1)
      parent_id = resourceIdCache.getProperty(String.valueOf(getConfig("password")) + Common.all_but_last(path)); 
    JSONObject parentID = new JSONObject();
    parentID.put((K)"id", (V)parent_id);
    fileMetaInfo.put((K)"parent", (V)parentID);
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
    JSONObject json_result = (JSONObject)((JSONObject)JSONValue.parse(result)).get("owned_by");
    resourceIdCache.put(String.valueOf(getConfig("password")) + path, json_result.get("id"));
    return true;
  }
  
  public boolean makedirs(String path) throws Exception {
    boolean ok = true;
    String[] parts = path.split("/");
    String path2 = "";
    for (int x = 0; x < parts.length && ok; x++) {
      path2 = String.valueOf(path2) + parts[x] + "/";
      if (x >= 1) {
        String resourceId = resourceIdCache.getProperty(String.valueOf(getConfig("password")) + path2);
        if (resourceId == null)
          ok = makedir(path2); 
      } 
    } 
    return ok;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(getConfig("password")) + path);
    if (resourceId == null) {
      stat(path);
      resourceId = resourceIdCache.getProperty(String.valueOf(getConfig("password")) + path);
      if (resourceId == null) {
        log("Delete path not found:" + path + "\r\n");
        return true;
      } 
    } 
    String url = "https://api.box.com/2.0/files/" + resourceId;
    if (resourceIdCache.getProperty(String.valueOf(getConfig("password")) + path + "/") != null)
      url = "https://api.box.com/2.0/folders/" + resourceId; 
    URLConnection urlc = URLConnection.openConnection(new VRL(url), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("PUT");
    urlc.setRequestProperty("Authorization", "Bearer " + this.bearer);
    urlc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
    SimpleDateFormat sdf_rfc1123_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    JSONObject fileMetaInfo = new JSONObject();
    fileMetaInfo.put((K)"modified_at", (V)sdf_rfc1123_2.format(new Date(modified)));
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
  
  public static Properties setup_bearer_refresh(String refresh_token) {
    try {
      String full_form = "client_id=hy6pb1k1m99td5l47xt38oyu3yxlzr6h";
      full_form = String.valueOf(full_form) + "&client_secret=wreXqsqasqqC26kSTJf1RjTyHZEkuwhO";
      full_form = String.valueOf(full_form) + "&refresh_token=" + refresh_token;
      full_form = String.valueOf(full_form) + "&grant_type=refresh_token";
      URLConnection urlc = URLConnection.openConnection(new VRL("https://api.box.com/oauth2/token"), new Properties());
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
  
  public static Properties setup_bearer(String oauth_access_code, String server_url) {
    try {
      String full_form = "code=" + URLEncoder.encode(oauth_access_code, "UTF-8");
      full_form = String.valueOf(full_form) + "&client_id=hy6pb1k1m99td5l47xt38oyu3yxlzr6h";
      full_form = String.valueOf(full_form) + "&client_secret=wreXqsqasqqC26kSTJf1RjTyHZEkuwhO";
      full_form = String.valueOf(full_form) + "&redirect_uri=" + server_url;
      full_form = String.valueOf(full_form) + "&grant_type=authorization_code";
      byte[] b = full_form.getBytes("UTF8");
      URLConnection urlc = URLConnection.openConnection(new VRL("https://api.box.com/oauth2/token"), new Properties());
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
          key = key.substring(2, key.lastIndexOf("\"")); 
        String val = line.split(":")[1].trim();
        if (val.indexOf("\"") >= 0)
          val = val.substring(1, val.lastIndexOf("\",")); 
        p.put(key, val);
      } 
    } catch (Exception e) {
      System.out.println(result);
      System.out.println(line);
    } 
    return p;
  }
}
