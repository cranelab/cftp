package com.crushftp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class B2Client extends GenericClient {
  String token = "";
  
  String downloadUrl = "";
  
  String account_id = "";
  
  String api_url = "";
  
  static Properties resourceIdCache = new Properties();
  
  static Properties bucketIdCache = new Properties();
  
  public B2Client(String url, String header, Vector log) {
    super(header, log);
    this.url = url;
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.config.put("username", username.trim());
    this.config.put("password", VRL.vrlDecode(password.trim()));
    URLConnection urlc = URLConnection.openConnection(new VRL("https://api.backblazeb2.com/b2api/v2/b2_authorize_account"), new Properties());
    urlc.setRequestMethod("GET");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    urlc.setRequestProperty("Authorization", "Basic  " + Base64.encodeBytes((String.valueOf(this.config.getProperty("username")) + ":" + this.config.getProperty("password")).getBytes()));
    urlc.connect();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    if (urlc.getResponseCode() < 200 || urlc.getResponseCode() > 299) {
      log(String.valueOf(urlc.getResponseCode()) + result + "\r\n");
      throw new IOException(result);
    } 
    JSONObject obj = (JSONObject)JSONValue.parse(result);
    this.token = (String)obj.get("authorizationToken");
    this.account_id = (String)obj.get("accountId");
    this.api_url = (String)obj.get("apiUrl");
    this.downloadUrl = (String)obj.get("downloadUrl");
    return "Success!";
  }
  
  public Vector list(String path, Vector list) throws Exception {
    if (path.equals("/"))
      return listBuckets(list); 
    return listFiles(path, list, true);
  }
  
  public Vector listBuckets(Vector list) throws Exception {
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.api_url) + "/b2api/v2/b2_list_buckets"), new Properties());
    urlc.setRequestMethod("POST");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    urlc.setRequestProperty("Authorization", this.token);
    JSONObject postData = new JSONObject();
    postData.put((K)"accountId", (V)this.config.getProperty("username"));
    JSONArray bucketTypes = new JSONArray();
    bucketTypes.add((E)"allPrivate");
    bucketTypes.add((E)"allPublic");
    postData.put((K)"bucketTypes", (V)bucketTypes);
    urlc.connect();
    OutputStream out = urlc.getOutputStream();
    out.write(postData.toString().getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(urlc.getResponseCode()) + result + "\r\n");
      throw new IOException(result);
    } 
    Vector list2 = new Vector();
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    Object obj = ((JSONObject)JSONValue.parse(result)).get("buckets");
    if (obj instanceof JSONArray) {
      JSONArray ja = (JSONArray)obj;
      for (int xxx = 0; xxx < ja.size(); xxx++) {
        Object obj2 = ja.get(xxx);
        if (obj2 instanceof JSONObject) {
          JSONObject jo = (JSONObject)obj2;
          Date d = new Date();
          String line = "drwxrwxrwx   1    owner   group   0   " + yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + jo.get("bucketName");
          Properties stat = parseStat(line);
          stat.put("url", "b2://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@api.backblaze.com/" + stat.getProperty("name") + "/");
          stat.put("bucketId", jo.get("bucketId"));
          bucketIdCache.put(String.valueOf(this.config.getProperty("password")) + stat.getProperty("name"), jo.get("bucketId"));
          list2.addElement(stat);
        } 
      } 
    } 
    list.addAll(list2);
    return list;
  }
  
  public Vector listFiles(String path, Vector list, boolean ignore_bzEmpty) throws Exception {
    String bucketName = path.substring(1, path.indexOf("/", 1));
    path = path.substring(bucketName.length() + 1);
    if (path.startsWith("/"))
      path = path.substring(1); 
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.api_url) + "/b2api/v2/b2_list_file_names"), new Properties());
    urlc.setRequestMethod("POST");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    urlc.setRequestProperty("Authorization", this.token);
    JSONObject postData = new JSONObject();
    if (!bucketIdCache.containsKey(String.valueOf(this.config.getProperty("password")) + bucketName))
      listBuckets(list); 
    postData.put((K)"bucketId", (V)bucketIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + bucketName));
    postData.put((K)"maxFileCount", (V)new Integer(1000));
    postData.put((K)"prefix", (V)path);
    if (ignore_bzEmpty)
      postData.put((K)"delimiter", (V)"/"); 
    urlc.connect();
    OutputStream out = urlc.getOutputStream();
    out.write(postData.toString().getBytes("UTF8"));
    out.close();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      log(String.valueOf(urlc.getResponseCode()) + result + "\r\n");
      throw new IOException(result);
    } 
    urlc.disconnect();
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    Object obj = ((JSONObject)JSONValue.parse(result)).get("files");
    Vector list2 = new Vector();
    if (obj instanceof JSONArray) {
      JSONArray ja = (JSONArray)obj;
      for (int xxx = 0; xxx < ja.size(); xxx++) {
        Object obj2 = ja.get(xxx);
        if (obj2 instanceof JSONObject) {
          JSONObject jo = (JSONObject)obj2;
          boolean folder = false;
          String name = (String)jo.get("fileName");
          if (!ignore_bzEmpty || !name.endsWith("/.bzEmpty")) {
            long miliseconds = -1L;
            if (((String)jo.get("action")).endsWith("folder")) {
              folder = true;
              name = name.substring(path.length(), name.indexOf("/", path.length()));
            } else {
              name = name.substring(path.length());
              JSONObject mdtm = (JSONObject)jo.get("fileInfo");
              if (mdtm.get("src_last_modified_millis") != null)
                miliseconds = Long.parseLong((String)mdtm.get("src_last_modified_millis")); 
            } 
            Date d = new Date();
            if (miliseconds > 0L)
              d.setTime(miliseconds); 
            String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + jo.get("contentLength") + "   " + yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + name;
            Properties stat = parseStat(line);
            stat.put("b2_file_name", (String)jo.get("fileName"));
            if (jo.get("fileId") != null)
              stat.put("fileId", (String)jo.get("fileId")); 
            stat.put("url", "b2://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@api.backblaze.com/" + bucketName + "/" + path + stat.getProperty("name") + (folder ? "/" : ""));
            list2.addElement(stat);
          } 
        } 
      } 
    } 
    list.addAll(list2);
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
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.downloadUrl) + "/file" + path), new Properties());
    urlc.setRequestMethod("GET");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    urlc.setRequestProperty("Content-Type", "application/json");
    urlc.setRequestProperty("Authorization", this.token);
    this.in = urlc.getInputStream();
    return this.in;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    if (!path.substring(1).contains("/"))
      throw new Exception("Cannot upload on Bucket level!!!"); 
    String file_name = path.substring(("/" + Common.first(path.substring(1)) + "/").length());
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.api_url) + "/b2api/v2/b2_start_large_file"), new Properties());
    urlc.setRequestMethod("POST");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    urlc.setRequestProperty("Content-Type", "application/json");
    urlc.setRequestProperty("Authorization", this.token);
    JSONObject postData = new JSONObject();
    postData.put((K)"fileName", (V)Common.url_encode(file_name, "/.#@&?!\\=+~"));
    postData.put((K)"contentType", (V)"application/octet-stream");
    String bucket_id = bucketIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + Common.first(path.substring(1)), "");
    if (bucket_id.equals("")) {
      list("/", new Vector());
      bucket_id = bucketIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + Common.first(path.substring(1)), "");
    } 
    postData.put((K)"bucketId", (V)bucket_id);
    urlc.connect();
    OutputStream out = urlc.getOutputStream();
    out.write(postData.toString().getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    if (code < 200 || code > 299) {
      log(String.valueOf(urlc.getResponseCode()) + result + "\r\n");
      throw new IOException(result);
    } 
    urlc.disconnect();
    JSONObject obj = (JSONObject)JSONValue.parse(result);
    String file_id = (String)obj.get("fileId");
    URLConnection urlc2 = URLConnection.openConnection(new VRL(String.valueOf(this.api_url) + "/b2api/v2/b2_get_upload_part_url"), new Properties());
    urlc2.setRequestMethod("POST");
    urlc2.setDoInput(true);
    urlc2.setDoOutput(true);
    urlc2.setUseCaches(false);
    urlc2.setRequestProperty("Authorization", this.token);
    urlc2.setRequestProperty("Content-Type", "application/json");
    JSONObject postData2 = new JSONObject();
    postData2.put((K)"fileId", (V)file_id);
    urlc2.connect();
    OutputStream out2 = urlc2.getOutputStream();
    out2.write(postData2.toString().getBytes("UTF8"));
    out2.close();
    int code2 = urlc2.getResponseCode();
    String result2 = URLConnection.consumeResponse(urlc2.getInputStream());
    if (code2 < 200 || code2 > 299) {
      log(String.valueOf(urlc2.getResponseCode()) + result2 + "\r\n");
      throw new IOException(result2);
    } 
    urlc2.disconnect();
    JSONObject obj2 = (JSONObject)JSONValue.parse(result2);
    String upload_auth_token = (String)obj2.get("authorizationToken");
    String upload_url = (String)obj2.get("uploadUrl");
    String upload_bucket_id = bucket_id;
    out = new null.OutputWrapper(this, file_id, upload_bucket_id, file_name, path, upload_url, upload_auth_token);
    return out;
  }
  
  public boolean delete(String path) throws Exception {
    if (!path.substring(1).contains("/"))
      throw new Exception("Cannot delete on Bucket level!"); 
    Properties p = stat(path);
    Vector files = new Vector();
    if (!p.containsKey("fileId")) {
      String folder_path = path;
      listFiles(folder_path, files, false);
      if (files.size() == 0)
        throw new Exception("Could not found file id of the given path :" + path); 
    } else {
      files.add(p);
    } 
    IOException e = null;
    for (int x = 0; x < files.size(); x++) {
      try {
        Properties pp = files.get(x);
        URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.api_url) + "/b2api/v1/b2_delete_file_version"), new Properties());
        urlc.setRequestMethod("POST");
        urlc.setDoInput(true);
        urlc.setDoOutput(true);
        urlc.setUseCaches(false);
        urlc.setRequestProperty("Authorization", this.token);
        JSONObject postData = new JSONObject();
        postData.put((K)"fileId", (V)pp.getProperty("fileId"));
        postData.put((K)"fileName", (V)pp.getProperty("b2_file_name", ""));
        urlc.connect();
        OutputStream out = urlc.getOutputStream();
        out.write(postData.toString().getBytes("UTF8"));
        out.close();
        int code = urlc.getResponseCode();
        String result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code < 200 || code > 299) {
          log(String.valueOf(urlc.getResponseCode()) + result + "\r\n");
          e = new IOException(result);
        } 
      } catch (Exception de) {
        log(de);
        if (e == null)
          e = new IOException(de.getMessage()); 
      } 
    } 
    if (e != null)
      throw e; 
    return true;
  }
  
  public boolean makedir(String path) throws Exception {
    if (!path.substring(1).contains("/"))
      throw new Exception("Cannot create folder on Bucket level!"); 
    String bucket_id = bucketIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + Common.first(path.substring(1)), "");
    if (bucket_id.equals("")) {
      list("/", new Vector());
      bucket_id = bucketIdCache.getProperty(String.valueOf(this.config.getProperty("password")) + Common.first(path.substring(1)), "");
    } 
    String b2_path = path.substring(("/" + Common.first(path.substring(1)) + "/").length());
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.api_url) + "/b2api/v2/b2_get_upload_url"), new Properties());
    urlc.setRequestMethod("POST");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    urlc.setRequestProperty("Authorization", this.token);
    urlc.setRequestProperty("Content-Type", "application/json");
    JSONObject postData3 = new JSONObject();
    postData3.put((K)"bucketId", (V)bucket_id);
    urlc.connect();
    OutputStream out = urlc.getOutputStream();
    out.write(postData3.toString().getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    if (code < 200 || code > 299) {
      log(String.valueOf(urlc.getResponseCode()) + result + "\r\n");
      urlc.disconnect();
      return false;
    } 
    urlc.disconnect();
    JSONObject obj = (JSONObject)JSONValue.parse(result);
    String folder_upload_auth_token = (String)obj.get("authorizationToken");
    String folder_upload_url = (String)obj.get("uploadUrl");
    URLConnection urlc2 = URLConnection.openConnection(new VRL(folder_upload_url), new Properties());
    urlc2.setRequestMethod("POST");
    urlc2.setDoInput(true);
    urlc2.setDoOutput(true);
    urlc2.setUseCaches(false);
    urlc2.setRequestProperty("Authorization", folder_upload_auth_token);
    urlc2.setRequestProperty("X-Bz-File-Name", Common.url_encode(String.valueOf(b2_path) + ".bzEmpty", "/.#@&?!\\=+~"));
    urlc2.setRequestProperty("X-Bz-Content-Sha1", "da39a3ee5e6b4b0d3255bfef95601890afd80709");
    urlc2.setRequestProperty("Content-Length", "0");
    urlc2.setRequestProperty("Content-Type", "text/plain");
    urlc2.connect();
    urlc2.getOutputStream().close();
    int code2 = urlc2.getResponseCode();
    if (code2 < 200 || code2 > 299) {
      log("Create directory path :" + path + "Error : code :" + code2 + " " + urlc2.getResponseMessage());
      log("Response" + URLConnection.consumeResponse(urlc2.getInputStream()));
      urlc2.disconnect();
      return false;
    } 
    urlc2.disconnect();
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
    Properties p = stat(rnfr);
    if (!rnfr.substring(1).contains("/"))
      throw new Exception("Bucket rename not allowed!"); 
    if (!p.containsKey("fileId"))
      throw new Exception("Cannot rename folder! B2 REST API does not support it."); 
    String b2_rnto_path = rnto.substring(("/" + Common.first(rnto.substring(1)) + "/").length());
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.api_url) + "/b2api/v2/b2_copy_file"), new Properties());
    urlc.setRequestMethod("POST");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    urlc.setRequestProperty("Authorization", this.token);
    JSONObject postData = new JSONObject();
    postData.put((K)"sourceFileId", (V)p.getProperty("fileId"));
    postData.put((K)"fileName", (V)b2_rnto_path);
    urlc.connect();
    OutputStream out = urlc.getOutputStream();
    out.write(postData.toString().getBytes("UTF8"));
    out.close();
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      log("Rename from path :" + rnfr + " to " + rnto + " Error : code :" + code + " " + urlc.getResponseMessage());
      log("Result : " + URLConnection.consumeResponse(urlc.getInputStream()));
      urlc.disconnect();
      return false;
    } 
    urlc.disconnect();
    return delete(rnfr);
  }
}
