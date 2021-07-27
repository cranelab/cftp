package com.crushftp.client;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.SocketTimeoutException;
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
  
  static Properties team_drives = new Properties();
  
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
    log("Gdrive:  Login : Get barear response: " + p.keySet().toString());
    if (p.containsKey("access_token")) {
      this.bearer = p.getProperty("access_token");
      return "Success";
    } 
    if (p.containsKey("refresh_token")) {
      this.bearer = p.getProperty("refresh_token");
      return "Success";
    } 
    return "Failure!";
  }
  
  public void logout() throws Exception {
    this.bearer = "";
    close();
  }
  
  public Vector list(String path, Vector list) throws Exception {
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    if (path.equals("/")) {
      list2(path, list, "root");
      return list;
    } 
    String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path);
    if (resourceId == null) {
      log("List : Could not found resource id in chache : " + path);
      loadResourceIds(path);
      resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path);
    } 
    list2(path, list, resourceId);
    return list;
  }
  
  private void list2(String path, Vector list, String resourceId) throws Exception {
    if (resourceId == null)
      log("List : Could not found resource id for path : " + path); 
    SimpleDateFormat sdf_rfc1123_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    String corpora = "";
    if (!path.equals("/") && this.config.getProperty("team_drive", "false").equals("true") && !this.config.getProperty("gdrive_corpora", "").equals(""))
      if (this.config.getProperty("gdrive_corpora", "").equals("drive")) {
        if (team_drives.size() != 0 && team_drives.containsKey(Common.first(path.substring(1)))) {
          corpora = "&corpora=drive&driveId=" + team_drives.getProperty(Common.first(path.substring(1)));
        } else {
          getAllDrives("/", new Vector());
          if (team_drives.containsKey(Common.first(path.substring(1))))
            corpora = "&corpora=drive&driveId=" + team_drives.getProperty(Common.first(path.substring(1))); 
        } 
      } else {
        corpora = "&corpora=" + this.config.getProperty("gdrive_corpora", "");
      }  
    String next_page_token = "";
    for (int x = 0; x < 100; x++) {
      if (next_page_token.equals("") && x > 0)
        break; 
      String page_token = "";
      if (!next_page_token.equals(""))
        page_token = "&pageToken=" + next_page_token; 
      URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files?q='" + resourceId + "'+in+parents and trashed=false&fields=kind,nextPageToken,incompleteSearch,files/id,files/name,files/size,files/mimeType,files/trashed,files/modifiedTime,files/parents&pageSize=1000&includeItemsFromAllDrives=true&supportsAllDrives=true" + page_token + corpora), new Properties());
      urlc.setDoOutput(true);
      urlc.setRequestMethod("GET");
      urlc.setRequestProperty("Authorization", "Bearer " + this.bearer);
      int code = urlc.getResponseCode();
      String json = Common.consumeResponse(urlc.getInputStream());
      Common.log("HTTP_CLIENT", 2, "Path = " + path + " List result = " + json);
      if (((JSONObject)JSONValue.parse(json)).containsKey("nextPageToken")) {
        next_page_token = ((JSONObject)JSONValue.parse(json)).get("nextPageToken").toString();
      } else {
        next_page_token = "";
      } 
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
              if (folder || item.getProperty("size") != null) {
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
      } 
    } 
    if (path.equals("/") && this.config.getProperty("team_drive", "false").equals("true"))
      getAllDrives(path, list); 
  }
  
  private void getAllDrives(String path, Vector list) throws IOException, Exception, SocketTimeoutException {
    String json2 = getTeamDrives(path, list, "");
    String nextPageToken = (String)((JSONObject)JSONValue.parse(json2)).get("nextPageToken");
    if (nextPageToken != null)
      for (int x = 0; x < 7; x++) {
        log("List : Team Drives - Next page token : " + nextPageToken + " index = " + x);
        String json_next = getTeamDrives(path, list, nextPageToken);
        nextPageToken = (String)((JSONObject)JSONValue.parse(json_next)).get("nextPageToken");
        if (nextPageToken == null)
          break; 
      }  
  }
  
  private String getTeamDrives(String path, Vector list, String pageToken) throws IOException, Exception, SocketTimeoutException {
    String pageTokenParameter = "&pageToken=" + pageToken;
    if (pageToken.equals(""))
      pageTokenParameter = ""; 
    URLConnection urlc2 = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/drives?pageSize=100" + pageTokenParameter), new Properties());
    urlc2.setDoOutput(true);
    urlc2.setRequestMethod("GET");
    urlc2.setRequestProperty("Authorization", "Bearer " + this.bearer);
    urlc2.getResponseCode();
    String json2 = Common.consumeResponse(urlc2.getInputStream());
    Object obj2 = ((JSONObject)JSONValue.parse(json2)).get("drives");
    if (obj2 instanceof JSONArray) {
      JSONArray ja = (JSONArray)obj2;
      SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
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
          Date d = new Date();
          String line = "drwxrwxrwx   1    owner   group   0   " + yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + item.getProperty("name");
          Properties stat = parseStat(line);
          stat.put("resource_id", item.getProperty("id"));
          stat.put("url", "gdrive://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@www.googleapis.com" + path + stat.getProperty("name"));
          team_drives.put(stat.getProperty("name"), item.getProperty("id"));
          resourceIdCache.put(String.valueOf(this.config.getProperty("password")) + path + stat.getProperty("name"), item.getProperty("id"));
          resourceIdCache.put(String.valueOf(this.config.getProperty("password")) + path + stat.getProperty("name") + "/", item.getProperty("id"));
          list.addElement(stat);
        } 
      } 
    } 
    return json2;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path);
    if (resourceId == null) {
      stat(path);
      resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path);
    } 
    if (resourceId == null) {
      log("Download path not found:" + path + "\r\n");
      throw new IOException("Download path not found:" + path);
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files/" + resourceId + "?alt=media&supportsAllDrives=true"), this.config);
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
      stat(Common.all_but_last(path));
      resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + Common.all_but_last(path));
    } 
    if (resourceId == null) {
      log("Upload path not found:" + Common.all_but_last(path) + "\r\n");
      throw new IOException("Upload path not found:" + Common.all_but_last(path));
    } 
    try {
      delete(path);
    } catch (Exception exception) {}
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable&supportsAllDrives=true"), this.config);
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
      log("resource id not found:" + path + "\r\n");
      if (stat(path) != null)
        resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path); 
      if (resourceId == null) {
        log("Delete path not found:" + path + "\r\n");
        return true;
      } 
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files/" + resourceId + "?supportsAllDrives=true"), new Properties());
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
    String path2 = path;
    if (path.endsWith("/")) {
      path2 = path.substring(0, path.length() - 1);
    } else {
      path2 = String.valueOf(path2) + "/";
    } 
    if (resourceIdCache.containsKey(String.valueOf(this.config.getProperty("password")) + path))
      resourceIdCache.remove(String.valueOf(this.config.getProperty("password")) + path); 
    if (resourceIdCache.containsKey(String.valueOf(this.config.getProperty("password")) + path2))
      resourceIdCache.remove(String.valueOf(this.config.getProperty("password")) + path2); 
    return true;
  }
  
  public boolean makedir(String path) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files?supportsAllDrives=true"), this.config);
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
    boolean was_created = true;
    for (int x = 0; x <= 5; x++) {
      if (stat(path) == null) {
        was_created = false;
      } else {
        was_created = true;
        break;
      } 
      Thread.sleep(300L);
    } 
    if (!was_created)
      throw new Exception("The path was not created : " + path); 
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
        if (resourceId == null) {
          stat(path2);
          resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path2);
        } 
        if (resourceId == null)
          ok = makedir(path2); 
      } 
    } 
    return ok;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + rnfr);
    if (resourceId == null) {
      stat(rnfr);
      resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + rnfr);
    } 
    if (resourceId == null) {
      log("Delete path not found:" + rnfr + "\r\n");
      return true;
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.googleapis.com/drive/v3/files/" + resourceId + "?supportsAllDrives=true"), this.config);
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
    String rnfr2 = rnfr;
    if (rnfr.endsWith("/")) {
      rnfr2 = rnfr.substring(0, rnfr.length() - 1);
    } else {
      rnfr2 = String.valueOf(rnfr2) + "/";
    } 
    if (resourceIdCache.containsKey(String.valueOf(this.config.getProperty("password")) + rnfr))
      resourceIdCache.remove(String.valueOf(this.config.getProperty("password")) + rnfr); 
    if (resourceIdCache.containsKey(String.valueOf(this.config.getProperty("password")) + rnfr2))
      resourceIdCache.remove(String.valueOf(this.config.getProperty("password")) + rnfr2); 
    boolean was_created = true;
    for (int x = 1; x < 6; x++) {
      if (stat(rnto) == null) {
        was_created = false;
      } else {
        was_created = true;
        break;
      } 
      Thread.sleep((x * 300));
    } 
    if (!was_created)
      throw new Exception("The path was not created : " + rnto); 
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
  
  public static Properties setup_bearer(String oauth_access_code, String server_url, String google_client_id, String google_client_secret) throws Exception {
    return Common.google_get_refresh_token(oauth_access_code, server_url, google_client_id, google_client_secret);
  }
  
  public static Properties setup_bearer_refresh(String refresh_token, String google_client_id, String google_client_secret) throws Exception {
    Properties p = Common.google_renew_tokens(refresh_token, google_client_id, google_client_secret);
    p.put("refresh_token", refresh_token);
    return p;
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
    } catch (Exception exception) {}
    return p;
  }
  
  private void loadResourceIds(String path) {
    log("List : Searching for resource id. The full path : " + path);
    String[] parts = (String[])null;
    if (path.equals("/")) {
      parts = new String[] { "" };
    } else {
      parts = path.split("/");
    } 
    String path2 = "";
    for (int x = 0; x < parts.length; x++) {
      path2 = String.valueOf(path2) + parts[x] + "/";
      String resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path2);
      if (path2.equals("/"))
        resourceId = "root"; 
      try {
        list2(path2, new Vector(), resourceId);
        resourceId = resourceIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + path2);
        log("List : Searching for resource id. The path : " + path2 + " Resource id :" + resourceId);
      } catch (Exception e) {
        log("Load Resource Ids : " + e);
      } 
    } 
  }
}
