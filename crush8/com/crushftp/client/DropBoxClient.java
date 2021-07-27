package com.crushftp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class DropBoxClient extends GenericClient {
  Properties config = new Properties();
  
  Properties resourceIdCache = new Properties();
  
  SimpleDateFormat yyyyMMddtHHmmssSSSZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
  
  SimpleDateFormat yyyyMMddtHHmmssZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
  
  SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  
  public DropBoxClient(String url, String header, Vector log) {
    super(header, log);
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
  }
  
  public static Properties setup_bearer(String oauth_access_code, String server_url, String google_client_id, String google_client_secret) throws Exception {
    String full_form = "code=" + URLEncoder.encode(oauth_access_code, "UTF-8");
    full_form = String.valueOf(full_form) + "&client_id=" + google_client_id;
    full_form = String.valueOf(full_form) + "&client_secret=" + google_client_secret;
    full_form = String.valueOf(full_form) + "&redirect_uri=" + server_url;
    full_form = String.valueOf(full_form) + "&grant_type=authorization_code";
    byte[] b = full_form.getBytes("UTF8");
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.dropboxapi.com/oauth2/token"), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    OutputStream out = urlc.getOutputStream();
    out.write(full_form.getBytes("UTF8"));
    out.close();
    urlc.getResponseCode();
    String response = Common.consumeResponse(urlc.getInputStream());
    String acess_token = ((JSONObject)JSONValue.parse(response)).get("access_token").toString();
    Properties p = new Properties();
    p.put("access_token", acess_token);
    return p;
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    password = VRL.vrlDecode(password);
    this.config.put("username", username);
    this.config.put("password", password);
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.dropboxapi.com/2/check/user"), new Properties());
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + this.config.getProperty("password", ""));
    urlc.setRequestProperty("Content-Type", "application/json");
    String query = "{\"query\": \"foo\"}";
    OutputStream out = urlc.getOutputStream();
    out.write(query.getBytes("UTF8"));
    out.close();
    String response = Common.consumeResponse(urlc.getInputStream());
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      log(String.valueOf(response) + "\r\n");
      return "Failure!";
    } 
    if (!((JSONObject)JSONValue.parse(response)).get("result").toString().equals("foo"))
      return "Failure!"; 
    return "Success";
  }
  
  public Vector list(String path, Vector list) throws Exception {
    String temp_path = path;
    if (path.equals("/"))
      temp_path = ""; 
    String cursor = "";
    for (int x = 0; x <= 10; x++) {
      cursor = list2(temp_path, cursor, list);
      if (cursor.equals(""))
        break; 
    } 
    return list;
  }
  
  private String list2(String path, String cursor, Vector list) throws Exception {
    String command_continue = "";
    if (!cursor.equals(""))
      command_continue = "/continue"; 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.dropboxapi.com/2/files/list_folder" + command_continue), new Properties());
    urlc.setDoOutput(true);
    urlc.setDoInput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + this.config.getProperty("password", ""));
    urlc.setRequestProperty("Content-Type", "application/json");
    JSONObject postData = new JSONObject();
    if (!cursor.equals("")) {
      postData.put((K)"cursor", (V)cursor);
    } else {
      postData.put((K)"path", (V)path);
      postData.put((K)"recursive", (V)new Boolean(false));
      postData.put((K)"include_deleted", (V)new Boolean(false));
      postData.put((K)"include_has_explicit_shared_members", (V)new Boolean(false));
      postData.put((K)"include_mounted_folders", (V)new Boolean(false));
      postData.put((K)"limit", (V)new Integer(1000));
      postData.put((K)"include_non_downloadable_files", (V)new Boolean(false));
    } 
    OutputStream out = urlc.getOutputStream();
    out.write(postData.toJSONString().getBytes("UTF8"));
    out.close();
    String response = Common.consumeResponse(urlc.getInputStream());
    parseListResponse(path, response, list);
    try {
      boolean has_more = ((Boolean)((JSONObject)JSONValue.parse(response)).get("has_more")).booleanValue();
      if (has_more) {
        cursor = (String)((JSONObject)JSONValue.parse(response)).get("cursor");
      } else {
        cursor = "";
      } 
    } catch (Exception e) {
      cursor = "";
    } 
    return cursor;
  }
  
  private Vector parseListResponse(String path, String response, Vector list) throws Exception {
    Object obj = ((JSONObject)JSONValue.parse(response)).get("entries");
    if (obj instanceof JSONArray) {
      JSONArray ja = (JSONArray)obj;
      for (int xxx = 0; xxx < ja.size(); xxx++) {
        Object json_item = ja.get(xxx);
        if (json_item instanceof JSONObject)
          try {
            Properties stat = parseItem(path, json_item);
            list.addElement(stat);
          } catch (Exception exception) {} 
      } 
    } 
    return null;
  }
  
  private Properties parseItem(String path, Object json_item) throws Exception {
    JSONObject jo = (JSONObject)json_item;
    Object[] a = jo.entrySet().toArray();
    Properties item = new Properties();
    for (int i = 0; i < a.length; i++) {
      String key2 = a[i].toString().split("=")[0];
      item.put(key2.trim(), jo.get(key2).trim());
    } 
    Date d = new Date();
    if (!item.getProperty("client_modified", "").equals("")) {
      String s = item.getProperty("client_modified", this.yyyyMMddtHHmmssSSSZ.format(d).toString());
      SimpleDateFormat format = null;
      try {
        if (s.length() == 20) {
          format = this.yyyyMMddtHHmmssZ;
        } else {
          format = this.yyyyMMddtHHmmssSSSZ;
        } 
        d = format.parse(s);
      } catch (Exception exception) {}
    } 
    boolean folder = true;
    if (item.getProperty(".tag", "folder").toLowerCase().equals("file"))
      folder = false; 
    String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + (folder ? "0" : item.getProperty("size", "0")) + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + item.getProperty("name");
    Properties stat = null;
    stat = parseStat(line);
    stat.put("resource_id", item.getProperty("id", ""));
    if (!item.getProperty("content_hash", "").equals(""))
      stat.put("dropbox_content_hash", item.getProperty("content_hash", "")); 
    if (!item.getProperty("has_explicit_shared_members", "").equals(""))
      stat.put("dropbox_has_explicit_shared_members", item.getProperty("has_explicit_shared_members", "false")); 
    if (!item.getProperty("is_downloadable", "").equals(""))
      stat.put("dropbox_is_downloadable", item.getProperty("is_downloadable", "false")); 
    if (!item.getProperty("server_modified", "").equals(""))
      stat.put("dropbox_server_modified", item.getProperty("server_modified", "")); 
    stat.put("url", "dropbox://" + this.config.getProperty("username", "") + ":" + VRL.vrlEncode(this.config.getProperty("password", "")) + "@api.dropboxapi.com" + (path.equals("") ? "/" : path) + stat.getProperty("name"));
    this.resourceIdCache.put(String.valueOf(this.config.getProperty("password")) + path + stat.getProperty("name"), item.getProperty("id"));
    this.resourceIdCache.put(String.valueOf(this.config.getProperty("password")) + path + stat.getProperty("name") + "/", item.getProperty("id"));
    return stat;
  }
  
  public Properties stat(String path) throws Exception {
    String temp_path = path;
    if (path.endsWith(":filetree"))
      temp_path = path.substring(0, path.indexOf(":filetree") - 1); 
    if (path.endsWith("/"))
      temp_path = path.substring(0, path.length() - 1); 
    if (path.equals("/"))
      temp_path = ""; 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.dropboxapi.com/2/files/get_metadata"), new Properties());
    urlc.setDoOutput(true);
    urlc.setDoInput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + this.config.getProperty("password", ""));
    urlc.setRequestProperty("Content-Type", "application/json");
    JSONObject postData = new JSONObject();
    postData.put((K)"path", (V)temp_path);
    postData.put((K)"include_media_info", (V)new Boolean(false));
    postData.put((K)"include_deleted", (V)new Boolean(false));
    postData.put((K)"include_has_explicit_shared_members", (V)new Boolean(false));
    OutputStream out = urlc.getOutputStream();
    out.write(postData.toJSONString().getBytes("UTF8"));
    out.close();
    String response = Common.consumeResponse(urlc.getInputStream());
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      log(String.valueOf(response) + "\r\n");
      return null;
    } 
    Properties p = parseItem(Common.all_but_last(path), JSONValue.parse(response));
    return p;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    String params = "";
    URLConnection urlc = URLConnection.openConnection(new VRL("https://content.dropboxapi.com/2/files/download"), new Properties());
    urlc.setDoOutput(true);
    urlc.setDoInput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + this.config.getProperty("password", ""));
    urlc.setRequestProperty("Content-Type", "application/octet-stream");
    JSONObject postData = new JSONObject();
    postData.put((K)"path", (V)path.toLowerCase());
    urlc.setRequestProperty("Dropbox-API-Arg", postData.toJSONString());
    this.in = urlc.getInputStream();
    return this.in;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL("https://content.dropboxapi.com/2/files/upload_session/start"), new Properties());
    urlc.setDoOutput(true);
    urlc.setDoInput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + this.config.getProperty("password", ""));
    urlc.setRequestProperty("Content-Type", "application/octet-stream");
    JSONObject postData = new JSONObject();
    postData.put((K)"close", (V)new Boolean(false));
    urlc.setRequestProperty("Dropbox-API-Arg", postData.toJSONString());
    int code = urlc.getResponseCode();
    String response = Common.consumeResponse(urlc.getInputStream());
    if (code > 0 && (code < 200 || code > 299)) {
      log(String.valueOf(response) + "\r\n");
      throw new IOException("Path :" + path + " Error : " + response);
    } 
    String upload_session_id = (String)((JSONObject)JSONValue.parse(response)).get("session_id");
    this.out = new null.OutputWrapper(this, path, upload_session_id);
    return this.out;
  }
  
  public boolean delete(String path) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.dropboxapi.com/2/files/delete_v2"), new Properties());
    urlc.setDoOutput(true);
    urlc.setDoInput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + this.config.getProperty("password", ""));
    urlc.setRequestProperty("Content-Type", "application/json");
    JSONObject postData = new JSONObject();
    postData.put((K)"path", (V)path);
    OutputStream out = urlc.getOutputStream();
    out.write(postData.toJSONString().getBytes("UTF8"));
    out.close();
    String response = Common.consumeResponse(urlc.getInputStream());
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      log(String.valueOf(response) + "\r\n");
      return false;
    } 
    return true;
  }
  
  public boolean makedir(String path) throws Exception {
    String temp_path = path;
    if (path.endsWith("/"))
      temp_path = path.substring(0, path.length() - 1); 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.dropboxapi.com/2/files/create_folder_v2"), new Properties());
    urlc.setDoOutput(true);
    urlc.setDoInput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + this.config.getProperty("password", ""));
    urlc.setRequestProperty("Content-Type", "application/json");
    JSONObject postData = new JSONObject();
    postData.put((K)"path", (V)temp_path);
    postData.put((K)"autorename", (V)new Boolean(false));
    OutputStream out = urlc.getOutputStream();
    out.write(postData.toJSONString().getBytes("UTF8"));
    out.close();
    String response = Common.consumeResponse(urlc.getInputStream());
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      log(String.valueOf(response) + "\r\n");
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
      if (x >= 1)
        if (stat(path2) == null)
          ok = makedir(path2);  
    } 
    return ok;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    String temp_rnfr = rnfr;
    if (rnfr.endsWith("/"))
      temp_rnfr = rnfr.substring(0, rnfr.length() - 1); 
    String temp_to = rnto;
    if (rnto.endsWith("/"))
      temp_to = rnto.substring(0, rnto.length() - 1); 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.dropboxapi.com/2/files/move_v2"), new Properties());
    urlc.setDoOutput(true);
    urlc.setDoInput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Authorization", "Bearer " + this.config.getProperty("password", ""));
    urlc.setRequestProperty("Content-Type", "application/json");
    JSONObject postData = new JSONObject();
    postData.put((K)"from_path", (V)temp_rnfr);
    postData.put((K)"to_path", (V)temp_to);
    postData.put((K)"autorename", (V)new Boolean(false));
    postData.put((K)"allow_shared_folder", (V)new Boolean(false));
    postData.put((K)"allow_ownership_transfer", (V)new Boolean(false));
    OutputStream out = urlc.getOutputStream();
    out.write(postData.toJSONString().getBytes("UTF8"));
    out.close();
    String response = Common.consumeResponse(urlc.getInputStream());
    int code = urlc.getResponseCode();
    code++;
    if (code < 200 || code > 299) {
      log(String.valueOf(response) + "\r\n");
      return false;
    } 
    return true;
  }
}
