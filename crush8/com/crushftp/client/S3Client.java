package com.crushftp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class S3Client extends GenericClient {
  Vector in_progress = new Vector();
  
  public static long ram_used_download = 0L;
  
  static Object ram_lock = new Object();
  
  SimpleDateFormat yyyyMMddtHHmmssSSSZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
  
  SimpleDateFormat yyyyMMddtHHmmssZ = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
  
  SimpleDateFormat yyyyMMddtHHmmssSSS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US);
  
  SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  
  SecretKeySpec secretKey = null;
  
  String region_host = "s3.amazonaws.com";
  
  private String region_name = "us-east-1";
  
  public static Properties valid_credentials_cache = new Properties();
  
  String cache_reference = null;
  
  public static Properties s3_global_cache = new Properties();
  
  String http_protocol = "https";
  
  public static void main(String[] args) throws IOException {
    System.setProperty("crushtunnel.debug", "2");
    System.getProperties().put("crushftp.s3_sha256", "false");
  }
  
  public S3Client(String url, String header, Vector log) {
    super(header, log);
    System.setProperty("crushtunnel.debug", System.getProperty("crushftp.debug", "2"));
    if (!this.yyyyMMddtHHmmssZ.getTimeZone().getID().equals("GMT"))
      this.yyyyMMddtHHmmssZ.setTimeZone(TimeZone.getTimeZone("GMT")); 
    if (!this.yyyyMMddtHHmmssSSSZ.getTimeZone().getID().equals("GMT"))
      this.yyyyMMddtHHmmssSSSZ.setTimeZone(TimeZone.getTimeZone("GMT")); 
    if (!this.yyyyMMddtHHmmssSSS.getTimeZone().getID().equals("GMT"))
      this.yyyyMMddtHHmmssSSS.setTimeZone(TimeZone.getTimeZone("GMT")); 
    if (!this.yyyyMMddHHmmss.getTimeZone().getID().equals("GMT"))
      this.yyyyMMddHHmmss.setTimeZone(TimeZone.getTimeZone("GMT")); 
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
    VRL s3_vrl = new VRL(url);
    if (s3_vrl.getPort() == 80 || s3_vrl.getPort() == 8080 || s3_vrl.getPort() == 9090 || (new StringBuffer(String.valueOf(s3_vrl.getPort()))).toString().endsWith("80"))
      this.http_protocol = "http"; 
    this.region_host = s3_vrl.getHost().toLowerCase();
    if (s3_vrl.getPort() != 443)
      this.region_host = String.valueOf(s3_vrl.getHost().toLowerCase()) + ":" + s3_vrl.getPort(); 
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.cache_reference = String.valueOf(this.config.getProperty("vfs_user")) + ":" + this.config.getProperty("baseURL");
    if (!s3_global_cache.containsKey(this.cache_reference)) {
      Properties caches = new Properties();
      caches.put("list_cache", new Properties());
      caches.put("cache_resume", new Properties());
      caches.put("stat_cache", new Properties());
      caches.put("recently_created_folder_cache", new Properties());
      caches.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      synchronized (s3_global_cache) {
        s3_global_cache.put(this.cache_reference, caches);
      } 
    } 
    this.config.put("username", username.trim());
    this.config.put("password", password.trim());
    updateIamAuth();
    String md5hash = Common.getMD5(new ByteArrayInputStream((String.valueOf(username) + password + clientid).getBytes()));
    this.secretKey = new SecretKeySpec(this.config.getProperty("real_password", this.config.getProperty("password")).getBytes("UTF8"), "HmacSHA1");
    if (this.config.getProperty("s3_accelerate", "false").equals("true")) {
      this.region_name = this.region_host.substring(3).substring(0, this.region_host.substring(3).indexOf("."));
      this.region_host = "s3-accelerate.amazonaws.com";
    } else {
      this.region_name = this.region_host.substring(3).substring(0, this.region_host.substring(3).indexOf("."));
    } 
    if (this.region_name.equals("amazonaws"))
      this.region_name = "us-east-1"; 
    if (!valid_credentials_cache.containsKey(md5hash) || System.getProperty("crushftp.s3.always_auth", "false").equals("false")) {
      String result = "";
      if (this.config.getProperty("no_bucket_check", "false").equals("true")) {
        String path0 = lower((new VRL(this.config.getProperty("url", ""))).getPath());
        String tempurl = Common.getBaseUrl(this.url, false);
        if (!this.config.getProperty("s3_bucket_in_path", "false").equals("true"))
          this.url = tempurl; 
        String path = path0;
        String bucketName = "";
        if (!path.equals("/")) {
          bucketName = path.substring(1, path.indexOf("/", 1));
          path = path.substring(bucketName.length() + 1);
          if (!path.endsWith("/"))
            path = String.valueOf(path) + "/"; 
        } 
        String query = "?delimiter=%2F&marker=&max-keys=1&prefix=" + handle_path_special_chars(path.substring(1), true);
        if (this.config.getProperty("s3_bucket_in_path", "false").equals("true"))
          query = String.valueOf(bucketName) + "/" + query; 
        URLConnection urlc = null;
        urlc = URLConnection.openConnection(new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + query), this.config);
        urlc.setRemoveDoubleEncoding(true);
        urlc.setDoOutput(false);
        urlc.setReadTimeout(10000);
        doStandardAmazonAlterations(urlc, (String)null, bucketName);
        int code = urlc.getResponseCode();
        result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code < 200 || code > 299) {
          log(String.valueOf(result) + "\r\n");
          throw new IOException(result);
        } 
      } else {
        URLConnection urlc = doAction("GET", (new VRL(this.url)).getPath(), (StringBuffer)null, false, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
        int code = urlc.getResponseCode();
        result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code == 404) {
          String bucketName = (new VRL(this.url)).getPath().substring(1);
          if (bucketName.indexOf("?") >= 0)
            bucketName = bucketName.substring(0, bucketName.indexOf("?")); 
          if (bucketName.indexOf("/") >= 0)
            bucketName = bucketName.substring(0, bucketName.indexOf("/")); 
          list((new VRL(this.url)).getPath(), new Vector());
          result = "Success";
        } else if (code < 200 || code > 299) {
          log(String.valueOf(result) + "\r\n");
          throw new IOException(result);
        } 
      } 
      valid_credentials_cache.put(md5hash, "Success");
    } 
    return valid_credentials_cache.getProperty(md5hash);
  }
  
  public void logout() throws Exception {
    this.cache_reference = String.valueOf(this.config.getProperty("vfs_user")) + ":" + this.config.getProperty("baseURL");
    s3_global_cache.remove(this.cache_reference);
    synchronized (this.in_progress) {
      while (this.in_progress.size() > 0) {
        Thread t = this.in_progress.remove(0);
        t.interrupt();
      } 
    } 
    close();
  }
  
  public Vector list(String path0, Vector list) throws Exception {
    return list(path0, list, 1000);
  }
  
  public Vector list(String path0, Vector list, int max_keys) throws Exception {
    path0 = lower(path0);
    String tempurl = Common.getBaseUrl(this.url, false);
    if (!this.config.getProperty("s3_bucket_in_path", "false").equals("true"))
      this.url = tempurl; 
    String path = path0;
    String bucketName = "";
    if (!path.equals("/")) {
      bucketName = path.substring(1, path.indexOf("/", 1));
      path = path.substring(bucketName.length() + 1);
      if (!path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
    } 
    Vector list2 = null;
    if (get_cache_item("list_cache").containsKey(path0)) {
      Properties ctmp = (Properties)get_cache_item("list_cache").get(path0);
      if (System.currentTimeMillis() - Long.parseLong(ctmp.getProperty("time")) < 30000L)
        list2 = (Vector)Common.CLONE(ctmp.get("o")); 
    } 
    if (list2 == null) {
      String last_key = null;
      list2 = new Vector();
      while (true) {
        String query = "?delimiter=%2F" + ((last_key != null) ? ("&marker=" + handle_path_special_chars(last_key, true)) : "") + "&max-keys=" + max_keys + "&prefix=" + handle_path_special_chars(path.substring(1), true);
        if (this.config.getProperty("s3_bucket_in_path", "false").equals("true"))
          query = String.valueOf(bucketName) + "/" + query; 
        String result = "";
        URLConnection urlc = null;
        for (int retries = 0; retries < 31; retries++) {
          urlc = URLConnection.openConnection(new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + query), this.config);
          urlc.setRemoveDoubleEncoding(true);
          urlc.setDoOutput(false);
          urlc.setReadTimeout(10000);
          doStandardAmazonAlterations(urlc, (String)null, bucketName);
          int code = 0;
          try {
            code = urlc.getResponseCode();
          } catch (SocketTimeoutException e) {
            log(e);
            log("Number of retry listing : " + retries++);
          } 
          result = URLConnection.consumeResponse(urlc.getInputStream());
          urlc.disconnect();
          if (code < 200 || code > 299) {
            log(String.valueOf(result) + "\r\n");
            if (retries >= 30 || result.indexOf("InternalError") < 0)
              throw new IOException(result); 
            log("Retrying failed dir listing query...");
            Thread.sleep((retries * 1000));
          } else {
            break;
          } 
        } 
        if (result.length() == 0)
          return list; 
        Element root = Common.getSaxBuilder().build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
        if (bucketName.toString().equals("")) {
          Element buckets = getElement(root, "Buckets");
          List buckets2 = buckets.getChildren();
          for (int x = 0; x < buckets2.size(); x++) {
            Element bucket = buckets2.get(x);
            log(String.valueOf(getKeyText(bucket, "Name")) + "\r\n");
            Date d = this.yyyyMMddtHHmmssSSS.parse(getKeyText(bucket, "CreationDate"));
            String line = "drwxrwxrwx   1    owner   group   0   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + getKeyText(bucket, "Name");
            Properties stat = parseStat(line);
            stat.put("url", String.valueOf(Common.url_decode(tempurl)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1) + stat.getProperty("name"));
            list.addElement(stat);
          } 
        } else {
          List prefixes = getElements(root, "CommonPrefixes");
          for (int x = 0; prefixes != null && x < prefixes.size(); x++) {
            Element content = prefixes.get(x);
            String name = getKeyText(content, "Prefix");
            name = name.substring(0, name.length() - 1);
            last_key = name;
            Common.log("S3_CLIENT", 2, String.valueOf(name) + "\r\n");
            boolean folder = true;
            Date d = new Date(0L);
            String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   0   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + name;
            Properties stat = parseStat(line);
            stat.put("check_all_recursive_deletes", "true");
            stat.put("url", String.valueOf(Common.url_decode(tempurl)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1) + stat.getProperty("name"));
            Common.log("S3_CLIENT", 2, stat + "\r\n");
            list2.addElement(stat);
          } 
          List contents = getElements(root, "Contents");
          for (int i = 0; i < contents.size(); i++) {
            Element content = contents.get(i);
            String name = getKeyText(content, "Key");
            if (max_keys == 1 || !name.equals(path.substring(1))) {
              last_key = name;
              Common.log("S3_CLIENT", 2, String.valueOf(name) + "\r\n");
              boolean folder = !(!name.endsWith("/") && urlc.getHeaderField("Content-Type").indexOf("x-directory") < 0);
              if (folder)
                name = name.substring(0, name.length() - 1); 
              String lastModified = getKeyText(content, "LastModified");
              Date d = null;
              SimpleDateFormat sdf_temp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
              sdf_temp.setTimeZone(TimeZone.getTimeZone("GMT"));
              if (lastModified.endsWith("Z") && !lastModified.contains(".")) {
                d = sdf_temp.parse(lastModified);
              } else {
                d = this.yyyyMMddtHHmmssSSS.parse(lastModified);
              } 
              String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + getKeyText(content, "Size") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + name;
              Properties stat = parseStat(line);
              stat.put("url", String.valueOf(Common.url_decode(tempurl)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1) + stat.getProperty("name"));
              Common.log("S3_CLIENT", 2, stat + "\r\n");
              list2.addElement(stat);
            } 
          } 
          if (max_keys != 1) {
            Properties ctmp = new Properties();
            ctmp.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
            ctmp.put("o", Common.CLONE(list2));
            get_cache_item("list_cache").put(path0, ctmp);
          } 
        } 
        try {
          if (!getElement(root, "IsTruncated").getText().equalsIgnoreCase("true") || last_key == null || max_keys == 1)
            break; 
        } catch (Exception exception) {
          break;
        } 
      } 
    } 
    list.addAll(list2);
    if (!bucketName.equals("") && max_keys != 1) {
      list2 = null;
      if (get_cache_item("cache_resume").containsKey(path0)) {
        Properties ctmp = (Properties)get_cache_item("cache_resume").get(path0);
        if (System.currentTimeMillis() - Long.parseLong(ctmp.getProperty("time")) < 30000L)
          list2 = (Vector)Common.CLONE(ctmp.get("o")); 
      } 
      boolean s3_partial = System.getProperty("crushftp.s3_partial", "true").equals("true");
      if (this.config.containsKey("s3_partial"))
        s3_partial = this.config.getProperty("s3_partial", "true").equals("true"); 
      if (list2 == null && s3_partial) {
        list2 = get_uploads_in_progress_or_failed_uploads(list, path, bucketName);
        if (list2 != null) {
          Properties ctmp = new Properties();
          ctmp.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
          ctmp.put("o", Common.CLONE(list2));
          get_cache_item("cache_resume").put(path0, ctmp);
        } else {
          list2 = new Vector();
        } 
      } else if (list2 == null) {
        list2 = new Vector();
      } 
      list.addAll(list2);
    } 
    return list;
  }
  
  private Vector get_uploads_in_progress_or_failed_uploads(Vector list, String path, String bucketName) throws IOException, SocketTimeoutException, InterruptedException, JDOMException, UnsupportedEncodingException, ParseException, Exception {
    path = lower(path);
    String result = "";
    String query = "";
    for (int retries = 0; retries < 31; ) {
      log("Looking for failed transfers, or in progress transfers.");
      query = "?delimiter=%2F&prefix=" + handle_path_special_chars(path.substring(1), true) + "&uploads";
      boolean s3_sha256 = System.getProperty("crushftp.s3_sha256", "false").equals("true");
      if (this.config.containsKey("s3_sha256"))
        s3_sha256 = this.config.getProperty("s3_sha256", "false").equals("true"); 
      if (s3_sha256 || !this.config.getProperty("server_side_encrypt_kms", "").equals(""))
        query = String.valueOf(query) + "="; 
      URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + (this.config.getProperty("s3_bucket_in_path", "false").equals("true") ? (String.valueOf(bucketName) + "/") : "") + query), this.config);
      urlc.setDoOutput(false);
      doStandardAmazonAlterations(urlc, (String)null, bucketName);
      int code = urlc.getResponseCode();
      result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        if (retries >= 30 || result.indexOf("InternalError") < 0)
          throw new IOException(result); 
        log("Retrying failed transfer lookup...");
        Thread.sleep((retries * 1000));
        retries++;
      } 
      break;
    } 
    Vector list2 = new Vector();
    if (result.length() == 0)
      return null; 
    Element root = Common.getSaxBuilder().build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
    List uploads = getElements(root, "Upload");
    Common.log("S3_CLIENT", 2, "In progress uploads:" + uploads.size());
    for (int x = 0; x < uploads.size(); x++) {
      Element content = uploads.get(x);
      String name = getKeyText(content, "Key");
      Common.log("S3_CLIENT", 2, "In progress upload:" + x + ":" + name);
      if ((!path.substring(1).equals("") || name.indexOf("/") <= 0) && 
        name.startsWith(path.substring(1)) && 
        name.indexOf("/", path.length()) <= 0) {
        log(String.valueOf(name) + "\r\n");
        boolean folder = name.endsWith("/");
        if (folder)
          name = name.substring(0, name.length() - 1); 
        Date d = this.yyyyMMddtHHmmssSSS.parse(getKeyText(content, "Initiated"));
        query = String.valueOf(handle_path_special_chars(name, false)) + "?uploadId=" + getKeyText(content, "UploadId");
        URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + (this.config.getProperty("s3_bucket_in_path", "false").equals("true") ? (String.valueOf(bucketName) + "/") : "") + query), this.config);
        urlc.setRemoveDoubleEncoding(true);
        urlc.setDoOutput(false);
        doStandardAmazonAlterations(urlc, (String)null, bucketName);
        int code = urlc.getResponseCode();
        result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code >= 200 && code <= 299) {
          Element partRoot = Common.getSaxBuilder().build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
          List parts = getElements(partRoot, "Part");
          if (parts == null)
            parts = new ArrayList(); 
          long totalSize = 0L;
          Vector resumeParts = new Vector();
          for (int xx = 0; xx < parts.size(); xx++) {
            Element part = parts.get(xx);
            totalSize += Long.parseLong(getKeyText(part, "Size"));
            d = this.yyyyMMddtHHmmssSSS.parse(getKeyText(part, "LastModified"));
            Properties chunk_part = new Properties();
            chunk_part.put("etag", getKeyText(part, "ETag"));
            resumeParts.addElement(chunk_part);
          } 
          String line = String.valueOf(folder ? "d" : "-") + "-w--w--w-   " + parts.size() + "    owner   group   " + totalSize + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + name;
          Properties stat = parseStat(line);
          stat.put("resumeParts", resumeParts);
          stat.put("uploadId", getKeyText(content, "UploadId"));
          String path2 = path;
          if (path2.endsWith(stat.getProperty("name")))
            path2 = Common.all_but_last(path2); 
          stat.put("url", String.valueOf(Common.url_decode(this.url)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path2.substring(1) + stat.getProperty("name"));
          log(stat + "\r\n");
          list2.addElement(stat);
        } 
      } 
    } 
    return list2;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    path = lower(path);
    this.in = download4(path, startPos, endPos, binary, (String)null);
    return this.in;
  }
  
  protected InputStream download4(String path0_tmp, long startPos0, long endPos, boolean binary, String bucketName0) throws Exception {
    String path0 = lower(path0_tmp);
    long startPos = (startPos0 < 0L) ? 0L : startPos0;
    Properties stat = stat(path0);
    Properties socks = Common.getConnectedSocks(false);
    Socket sock1 = (Socket)socks.get("sock1");
    Socket sock2 = (Socket)socks.get("sock2");
    Vector chunks_needed = new Vector();
    if (stat.containsKey("segments"))
      chunks_needed.addAll((Vector)stat.get("segments")); 
    Properties chunks = new Properties();
    Properties status = new Properties();
    long len = Long.parseLong(stat.getProperty("size"));
    if (endPos > 0L && endPos < len)
      len = endPos; 
    long lenF = len;
    boolean segmented = stat.containsKey("segments");
    if (!stat.containsKey("segments")) {
      long pos = startPos;
      long amount = Long.parseLong(this.config.getProperty("s3_buffer", "5")) * 1024L * 1024L;
      if (pos < 0L)
        pos = 0L; 
      int i = 1;
      while (pos < len) {
        chunks_needed.addElement(String.valueOf(i) + ":" + pos + "-" + (pos + amount));
        pos += amount + 1L;
        i++;
      } 
    } 
    if (startPos > 0L && segmented) {
      boolean delete = false;
      for (int x = chunks_needed.size() - 1; x >= 0; x--) {
        if (delete) {
          chunks_needed.remove(x);
        } else {
          String part = chunks_needed.elementAt(x).toString();
          if (startPos >= Long.parseLong(part.split(":")[1].split("-")[0]) && startPos <= Long.parseLong(part.split(":")[1].split("-")[1])) {
            if (startPos == Long.parseLong(part.split(":")[1].split("-")[1]))
              chunks_needed.remove(x); 
            delete = true;
          } 
        } 
      } 
    } 
    status.put("first", "true");
    status.put("ram", "0");
    status.put("current_pos", "0");
    log("Chunks needed:" + chunks_needed);
    Runnable grabChunk = new Runnable(this, chunks_needed, status, bucketName0, stat, path0, segmented, startPos, chunks) {
        final S3Client this$0;
        
        private final Vector val$chunks_needed;
        
        private final Properties val$status;
        
        private final String val$bucketName0;
        
        private final Properties val$stat;
        
        private final String val$path0;
        
        private final boolean val$segmented;
        
        private final long val$startPos;
        
        private final Properties val$chunks;
        
        public void run() {
          // Byte code:
          //   0: invokestatic currentThread : ()Ljava/lang/Thread;
          //   3: invokevirtual getName : ()Ljava/lang/String;
          //   6: astore_1
          //   7: goto -> 1339
          //   10: getstatic com/crushftp/client/S3Client.ram_used_download : J
          //   13: ldc2_w 1024
          //   16: ldiv
          //   17: ldc2_w 1024
          //   20: ldiv
          //   21: aload_0
          //   22: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   25: getfield config : Ljava/util/Properties;
          //   28: ldc 's3_max_buffer_download'
          //   30: ldc '100'
          //   32: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   35: invokestatic parseLong : (Ljava/lang/String;)J
          //   38: lcmp
          //   39: ifle -> 60
          //   42: aload_0
          //   43: getfield val$status : Ljava/util/Properties;
          //   46: ldc 'error'
          //   48: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   51: ifne -> 60
          //   54: ldc2_w 1000
          //   57: invokestatic sleep : (J)V
          //   60: iconst_0
          //   61: istore_2
          //   62: goto -> 74
          //   65: ldc2_w 1000
          //   68: invokestatic sleep : (J)V
          //   71: iinc #2, 1
          //   74: iload_2
          //   75: bipush #60
          //   77: if_icmpge -> 123
          //   80: aload_0
          //   81: getfield val$status : Ljava/util/Properties;
          //   84: ldc 'ram'
          //   86: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   89: invokestatic parseLong : (Ljava/lang/String;)J
          //   92: ldc2_w 1024
          //   95: ldiv
          //   96: ldc2_w 1024
          //   99: ldiv
          //   100: ldc2_w 128
          //   103: lcmp
          //   104: ifle -> 123
          //   107: aload_0
          //   108: getfield val$status : Ljava/util/Properties;
          //   111: ldc 'error'
          //   113: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   116: ifeq -> 65
          //   119: goto -> 123
          //   122: astore_2
          //   123: aload_0
          //   124: getfield val$status : Ljava/util/Properties;
          //   127: ldc 'error'
          //   129: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   132: ifeq -> 138
          //   135: goto -> 1361
          //   138: aconst_null
          //   139: astore_2
          //   140: iconst_0
          //   141: istore_3
          //   142: aload_0
          //   143: getfield val$chunks_needed : Ljava/util/Vector;
          //   146: dup
          //   147: astore #4
          //   149: monitorenter
          //   150: aload_0
          //   151: getfield val$status : Ljava/util/Properties;
          //   154: ldc 'first'
          //   156: ldc 'false'
          //   158: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   161: ldc 'true'
          //   163: invokevirtual equals : (Ljava/lang/Object;)Z
          //   166: istore_3
          //   167: aload_0
          //   168: getfield val$status : Ljava/util/Properties;
          //   171: ldc 'first'
          //   173: ldc 'false'
          //   175: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   178: pop
          //   179: aload_0
          //   180: getfield val$chunks_needed : Ljava/util/Vector;
          //   183: invokevirtual size : ()I
          //   186: ifle -> 201
          //   189: aload_0
          //   190: getfield val$chunks_needed : Ljava/util/Vector;
          //   193: iconst_0
          //   194: invokevirtual remove : (I)Ljava/lang/Object;
          //   197: invokevirtual toString : ()Ljava/lang/String;
          //   200: astore_2
          //   201: aload #4
          //   203: monitorexit
          //   204: goto -> 211
          //   207: aload #4
          //   209: monitorexit
          //   210: athrow
          //   211: aload_2
          //   212: ifnonnull -> 218
          //   215: goto -> 1361
          //   218: aload_0
          //   219: getfield val$bucketName0 : Ljava/lang/String;
          //   222: astore #4
          //   224: aload_0
          //   225: getfield val$stat : Ljava/util/Properties;
          //   228: ldc 'uid'
          //   230: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   233: ifeq -> 241
          //   236: aload #4
          //   238: ifnonnull -> 373
          //   241: aload_0
          //   242: getfield val$path0 : Ljava/lang/String;
          //   245: astore #5
          //   247: aload #5
          //   249: iconst_1
          //   250: invokevirtual substring : (I)Ljava/lang/String;
          //   253: astore #4
          //   255: aload #4
          //   257: ldc '?'
          //   259: invokevirtual indexOf : (Ljava/lang/String;)I
          //   262: iflt -> 280
          //   265: aload #4
          //   267: iconst_0
          //   268: aload #4
          //   270: ldc '?'
          //   272: invokevirtual indexOf : (Ljava/lang/String;)I
          //   275: invokevirtual substring : (II)Ljava/lang/String;
          //   278: astore #4
          //   280: aload #4
          //   282: ldc '/'
          //   284: invokevirtual indexOf : (Ljava/lang/String;)I
          //   287: iflt -> 320
          //   290: aload #4
          //   292: iconst_0
          //   293: aload #4
          //   295: ldc '/'
          //   297: invokevirtual indexOf : (Ljava/lang/String;)I
          //   300: invokevirtual substring : (II)Ljava/lang/String;
          //   303: astore #4
          //   305: aload #5
          //   307: aload #5
          //   309: ldc '/'
          //   311: iconst_1
          //   312: invokevirtual indexOf : (Ljava/lang/String;I)I
          //   315: invokevirtual substring : (I)Ljava/lang/String;
          //   318: astore #5
          //   320: aload #5
          //   322: new java/lang/StringBuffer
          //   325: dup
          //   326: ldc '/'
          //   328: invokespecial <init> : (Ljava/lang/String;)V
          //   331: aload #4
          //   333: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   336: invokevirtual toString : ()Ljava/lang/String;
          //   339: invokevirtual equals : (Ljava/lang/Object;)Z
          //   342: ifeq -> 349
          //   345: ldc '/'
          //   347: astore #5
          //   349: aload_0
          //   350: getfield val$stat : Ljava/util/Properties;
          //   353: ldc 'uid'
          //   355: aload_0
          //   356: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   359: aload #5
          //   361: iconst_1
          //   362: invokevirtual substring : (I)Ljava/lang/String;
          //   365: iconst_1
          //   366: invokevirtual handle_path_special_chars : (Ljava/lang/String;Z)Ljava/lang/String;
          //   369: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   372: pop
          //   373: aload_0
          //   374: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   377: ldc 'GET'
          //   379: new java/lang/StringBuffer
          //   382: dup
          //   383: ldc '/'
          //   385: invokespecial <init> : (Ljava/lang/String;)V
          //   388: aload #4
          //   390: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   393: ldc '/'
          //   395: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   398: aload_0
          //   399: getfield val$stat : Ljava/util/Properties;
          //   402: ldc 'uid'
          //   404: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   407: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   410: aload_0
          //   411: getfield val$segmented : Z
          //   414: ifeq -> 443
          //   417: new java/lang/StringBuffer
          //   420: dup
          //   421: ldc '_'
          //   423: invokespecial <init> : (Ljava/lang/String;)V
          //   426: aload_2
          //   427: ldc ':'
          //   429: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   432: iconst_0
          //   433: aaload
          //   434: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   437: invokevirtual toString : ()Ljava/lang/String;
          //   440: goto -> 445
          //   443: ldc ''
          //   445: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   448: invokevirtual toString : ()Ljava/lang/String;
          //   451: aconst_null
          //   452: iconst_0
          //   453: iconst_0
          //   454: aload_0
          //   455: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   458: getfield config : Ljava/util/Properties;
          //   461: ldc 's3_bucket_in_path'
          //   463: ldc 'false'
          //   465: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   468: ldc 'true'
          //   470: invokevirtual equals : (Ljava/lang/Object;)Z
          //   473: invokevirtual doAction : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/StringBuffer;ZZZ)Lcom/crushftp/client/URLConnection;
          //   476: astore #5
          //   478: aload_0
          //   479: getfield val$startPos : J
          //   482: aload_2
          //   483: ldc ':'
          //   485: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   488: iconst_1
          //   489: aaload
          //   490: ldc '-'
          //   492: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   495: iconst_0
          //   496: aaload
          //   497: invokestatic parseLong : (Ljava/lang/String;)J
          //   500: lsub
          //   501: lstore #6
          //   503: aload_0
          //   504: getfield val$segmented : Z
          //   507: ifne -> 543
          //   510: aload #5
          //   512: ldc 'Range'
          //   514: new java/lang/StringBuffer
          //   517: dup
          //   518: ldc 'bytes='
          //   520: invokespecial <init> : (Ljava/lang/String;)V
          //   523: aload_2
          //   524: ldc ':'
          //   526: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   529: iconst_1
          //   530: aaload
          //   531: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   534: invokevirtual toString : ()Ljava/lang/String;
          //   537: invokevirtual setRequestProperty : (Ljava/lang/String;Ljava/lang/String;)V
          //   540: goto -> 664
          //   543: iload_3
          //   544: ifeq -> 664
          //   547: aload_0
          //   548: getfield val$segmented : Z
          //   551: ifeq -> 664
          //   554: lload #6
          //   556: lconst_0
          //   557: lcmp
          //   558: ifeq -> 664
          //   561: new java/lang/StringBuffer
          //   564: dup
          //   565: aload_2
          //   566: ldc ':'
          //   568: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   571: iconst_0
          //   572: aaload
          //   573: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   576: invokespecial <init> : (Ljava/lang/String;)V
          //   579: ldc ':'
          //   581: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   584: aload_2
          //   585: ldc ':'
          //   587: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   590: iconst_1
          //   591: aaload
          //   592: ldc '-'
          //   594: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   597: iconst_0
          //   598: aaload
          //   599: invokestatic parseLong : (Ljava/lang/String;)J
          //   602: lload #6
          //   604: ladd
          //   605: invokevirtual append : (J)Ljava/lang/StringBuffer;
          //   608: ldc '-'
          //   610: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   613: aload_2
          //   614: ldc ':'
          //   616: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   619: iconst_1
          //   620: aaload
          //   621: ldc '-'
          //   623: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   626: iconst_1
          //   627: aaload
          //   628: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   631: invokevirtual toString : ()Ljava/lang/String;
          //   634: astore_2
          //   635: aload #5
          //   637: ldc 'Range'
          //   639: new java/lang/StringBuffer
          //   642: dup
          //   643: ldc 'bytes='
          //   645: invokespecial <init> : (Ljava/lang/String;)V
          //   648: lload #6
          //   650: invokevirtual append : (J)Ljava/lang/StringBuffer;
          //   653: ldc '-'
          //   655: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   658: invokevirtual toString : ()Ljava/lang/String;
          //   661: invokevirtual setRequestProperty : (Ljava/lang/String;Ljava/lang/String;)V
          //   664: invokestatic currentTimeMillis : ()J
          //   667: lstore #8
          //   669: aload_2
          //   670: ldc ':-1-'
          //   672: invokevirtual indexOf : (Ljava/lang/String;)I
          //   675: iflt -> 696
          //   678: aload_2
          //   679: ldc ':-1-'
          //   681: ldc ':0-'
          //   683: invokestatic replace_str : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   686: astore_2
          //   687: goto -> 696
          //   690: ldc2_w 100
          //   693: invokestatic sleep : (J)V
          //   696: aload_0
          //   697: getfield val$status : Ljava/util/Properties;
          //   700: ldc 'error'
          //   702: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   705: ifne -> 975
          //   708: iload_3
          //   709: ifne -> 975
          //   712: invokestatic currentTimeMillis : ()J
          //   715: lload #8
          //   717: lsub
          //   718: ldc2_w 60000
          //   721: lcmp
          //   722: ifge -> 975
          //   725: aload_2
          //   726: ldc ':'
          //   728: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   731: iconst_1
          //   732: aaload
          //   733: ldc '-'
          //   735: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   738: iconst_0
          //   739: aaload
          //   740: invokestatic parseLong : (Ljava/lang/String;)J
          //   743: aload_0
          //   744: getfield val$status : Ljava/util/Properties;
          //   747: ldc 'current_pos'
          //   749: ldc '0'
          //   751: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   754: invokestatic parseLong : (Ljava/lang/String;)J
          //   757: lsub
          //   758: aload_0
          //   759: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   762: getfield config : Ljava/util/Properties;
          //   765: ldc 's3_buffer'
          //   767: ldc '5'
          //   769: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   772: invokestatic parseLong : (Ljava/lang/String;)J
          //   775: ldc2_w 1024
          //   778: lmul
          //   779: ldc2_w 1024
          //   782: lmul
          //   783: ldc2_w 2
          //   786: lmul
          //   787: aload_0
          //   788: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   791: getfield config : Ljava/util/Properties;
          //   794: ldc 's3_threads_download'
          //   796: ldc '3'
          //   798: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   801: invokestatic parseInt : (Ljava/lang/String;)I
          //   804: i2l
          //   805: lmul
          //   806: lcmp
          //   807: ifgt -> 690
          //   810: goto -> 975
          //   813: astore #10
          //   815: aload_0
          //   816: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   819: new java/lang/StringBuffer
          //   822: dup
          //   823: ldc 'Wait:'
          //   825: invokespecial <init> : (Ljava/lang/String;)V
          //   828: invokestatic currentTimeMillis : ()J
          //   831: lload #8
          //   833: lsub
          //   834: invokevirtual append : (J)Ljava/lang/StringBuffer;
          //   837: invokevirtual toString : ()Ljava/lang/String;
          //   840: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   843: pop
          //   844: aload_0
          //   845: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   848: new java/lang/StringBuffer
          //   851: dup
          //   852: ldc 'Part:'
          //   854: invokespecial <init> : (Ljava/lang/String;)V
          //   857: aload_2
          //   858: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   861: invokevirtual toString : ()Ljava/lang/String;
          //   864: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   867: pop
          //   868: aload_0
          //   869: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   872: new java/lang/StringBuffer
          //   875: dup
          //   876: ldc 'Buffer:'
          //   878: invokespecial <init> : (Ljava/lang/String;)V
          //   881: aload_0
          //   882: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   885: getfield config : Ljava/util/Properties;
          //   888: ldc 's3_buffer'
          //   890: ldc '5'
          //   892: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   895: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   898: invokevirtual toString : ()Ljava/lang/String;
          //   901: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   904: pop
          //   905: aload_0
          //   906: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   909: new java/lang/StringBuffer
          //   912: dup
          //   913: ldc 'Threads:'
          //   915: invokespecial <init> : (Ljava/lang/String;)V
          //   918: aload_0
          //   919: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   922: getfield config : Ljava/util/Properties;
          //   925: ldc 's3_threads_download'
          //   927: ldc '3'
          //   929: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   932: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   935: invokevirtual toString : ()Ljava/lang/String;
          //   938: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   941: pop
          //   942: aload_0
          //   943: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   946: new java/lang/StringBuffer
          //   949: dup
          //   950: ldc_w 'Status:'
          //   953: invokespecial <init> : (Ljava/lang/String;)V
          //   956: aload_0
          //   957: getfield val$status : Ljava/util/Properties;
          //   960: invokevirtual append : (Ljava/lang/Object;)Ljava/lang/StringBuffer;
          //   963: invokevirtual toString : ()Ljava/lang/String;
          //   966: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   969: pop
          //   970: aload #10
          //   972: athrow
          //   973: astore #8
          //   975: aload_0
          //   976: getfield val$status : Ljava/util/Properties;
          //   979: ldc 'error'
          //   981: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   984: ifeq -> 990
          //   987: goto -> 1361
          //   990: aload #5
          //   992: invokevirtual getResponseCode : ()I
          //   995: istore #8
          //   997: iload #8
          //   999: sipush #200
          //   1002: if_icmplt -> 1013
          //   1005: iload #8
          //   1007: sipush #299
          //   1010: if_icmple -> 1067
          //   1013: aload #5
          //   1015: invokevirtual getInputStream : ()Ljava/io/InputStream;
          //   1018: invokestatic consumeResponse : (Ljava/io/InputStream;)Ljava/lang/String;
          //   1021: astore #9
          //   1023: aload #5
          //   1025: invokevirtual disconnect : ()V
          //   1028: aload_0
          //   1029: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   1032: new java/lang/StringBuffer
          //   1035: dup
          //   1036: aload #9
          //   1038: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   1041: invokespecial <init> : (Ljava/lang/String;)V
          //   1044: ldc_w '\\r\\n'
          //   1047: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1050: invokevirtual toString : ()Ljava/lang/String;
          //   1053: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   1056: pop
          //   1057: new java/io/IOException
          //   1060: dup
          //   1061: aload #9
          //   1063: invokespecial <init> : (Ljava/lang/String;)V
          //   1066: athrow
          //   1067: new java/io/ByteArrayOutputStream
          //   1070: dup
          //   1071: invokespecial <init> : ()V
          //   1074: astore #9
          //   1076: aload #5
          //   1078: invokevirtual getInputStream : ()Ljava/io/InputStream;
          //   1081: aload #9
          //   1083: iconst_0
          //   1084: iconst_1
          //   1085: iconst_1
          //   1086: invokestatic streamCopier : (Ljava/io/InputStream;Ljava/io/OutputStream;ZZZ)V
          //   1089: aload #5
          //   1091: invokevirtual disconnect : ()V
          //   1094: aload_0
          //   1095: getfield val$status : Ljava/util/Properties;
          //   1098: ldc 'error'
          //   1100: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   1103: ifne -> 1316
          //   1106: getstatic com/crushftp/client/S3Client.ram_lock : Ljava/lang/Object;
          //   1109: dup
          //   1110: astore #10
          //   1112: monitorenter
          //   1113: aload #9
          //   1115: invokevirtual toByteArray : ()[B
          //   1118: astore #11
          //   1120: getstatic com/crushftp/client/S3Client.ram_used_download : J
          //   1123: aload #11
          //   1125: arraylength
          //   1126: i2l
          //   1127: ladd
          //   1128: putstatic com/crushftp/client/S3Client.ram_used_download : J
          //   1131: aload_0
          //   1132: getfield val$chunks : Ljava/util/Properties;
          //   1135: aload_2
          //   1136: ldc ':'
          //   1138: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   1141: iconst_1
          //   1142: aaload
          //   1143: ldc '-'
          //   1145: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   1148: iconst_0
          //   1149: aaload
          //   1150: aload #11
          //   1152: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1155: pop
          //   1156: aload_0
          //   1157: getfield val$status : Ljava/util/Properties;
          //   1160: ldc 'ram'
          //   1162: new java/lang/StringBuffer
          //   1165: dup
          //   1166: aload_0
          //   1167: getfield val$status : Ljava/util/Properties;
          //   1170: ldc 'ram'
          //   1172: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   1175: invokestatic parseLong : (Ljava/lang/String;)J
          //   1178: aload #11
          //   1180: arraylength
          //   1181: i2l
          //   1182: ladd
          //   1183: invokestatic valueOf : (J)Ljava/lang/String;
          //   1186: invokespecial <init> : (Ljava/lang/String;)V
          //   1189: invokevirtual toString : ()Ljava/lang/String;
          //   1192: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1195: pop
          //   1196: new java/lang/StringBuffer
          //   1199: dup
          //   1200: aload_1
          //   1201: iconst_0
          //   1202: aload_1
          //   1203: ldc ':'
          //   1205: invokevirtual lastIndexOf : (Ljava/lang/String;)I
          //   1208: iconst_1
          //   1209: iadd
          //   1210: invokevirtual substring : (II)Ljava/lang/String;
          //   1213: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   1216: invokespecial <init> : (Ljava/lang/String;)V
          //   1219: aload_0
          //   1220: getfield val$status : Ljava/util/Properties;
          //   1223: ldc 'ram'
          //   1225: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   1228: invokestatic parseLong : (Ljava/lang/String;)J
          //   1231: invokestatic format_bytes_short : (J)Ljava/lang/String;
          //   1234: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1237: ldc_w ' of '
          //   1240: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1243: getstatic com/crushftp/client/S3Client.ram_used_download : J
          //   1246: invokestatic format_bytes_short : (J)Ljava/lang/String;
          //   1249: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1252: invokevirtual toString : ()Ljava/lang/String;
          //   1255: astore #12
          //   1257: invokestatic currentThread : ()Ljava/lang/Thread;
          //   1260: aload #12
          //   1262: invokevirtual setName : (Ljava/lang/String;)V
          //   1265: aload #10
          //   1267: monitorexit
          //   1268: goto -> 1316
          //   1271: aload #10
          //   1273: monitorexit
          //   1274: athrow
          //   1275: astore #8
          //   1277: aload_0
          //   1278: getfield val$status : Ljava/util/Properties;
          //   1281: ldc 'error'
          //   1283: aload #8
          //   1285: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1288: pop
          //   1289: goto -> 1316
          //   1292: astore #8
          //   1294: aload_0
          //   1295: getfield val$status : Ljava/util/Properties;
          //   1298: ldc 'error'
          //   1300: aload #8
          //   1302: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1305: pop
          //   1306: ldc_w 'S3_CLIENT'
          //   1309: iconst_2
          //   1310: aload #8
          //   1312: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
          //   1315: pop
          //   1316: aload_0
          //   1317: getfield val$status : Ljava/util/Properties;
          //   1320: ldc_w 'run_once'
          //   1323: ldc 'false'
          //   1325: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   1328: ldc 'true'
          //   1330: invokevirtual equals : (Ljava/lang/Object;)Z
          //   1333: ifeq -> 1339
          //   1336: goto -> 1361
          //   1339: aload_0
          //   1340: getfield val$chunks_needed : Ljava/util/Vector;
          //   1343: invokevirtual size : ()I
          //   1346: ifle -> 1361
          //   1349: aload_0
          //   1350: getfield val$status : Ljava/util/Properties;
          //   1353: ldc 'error'
          //   1355: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   1358: ifeq -> 10
          //   1361: return
          // Line number table:
          //   Java source line number -> byte code offset
          //   #640	-> 0
          //   #641	-> 7
          //   #645	-> 10
          //   #646	-> 60
          //   #647	-> 65
          //   #646	-> 71
          //   #649	-> 122
          //   #652	-> 123
          //   #653	-> 138
          //   #654	-> 140
          //   #655	-> 142
          //   #657	-> 150
          //   #658	-> 167
          //   #659	-> 179
          //   #655	-> 201
          //   #661	-> 211
          //   #662	-> 218
          //   #663	-> 224
          //   #665	-> 241
          //   #666	-> 247
          //   #667	-> 255
          //   #668	-> 280
          //   #670	-> 290
          //   #671	-> 305
          //   #673	-> 320
          //   #674	-> 349
          //   #676	-> 373
          //   #677	-> 478
          //   #678	-> 503
          //   #679	-> 543
          //   #681	-> 561
          //   #682	-> 635
          //   #688	-> 664
          //   #690	-> 669
          //   #693	-> 687
          //   #694	-> 690
          //   #693	-> 696
          //   #696	-> 813
          //   #698	-> 815
          //   #699	-> 844
          //   #700	-> 868
          //   #701	-> 905
          //   #702	-> 942
          //   #703	-> 970
          //   #706	-> 973
          //   #709	-> 975
          //   #710	-> 990
          //   #711	-> 997
          //   #713	-> 1013
          //   #714	-> 1023
          //   #715	-> 1028
          //   #716	-> 1057
          //   #718	-> 1067
          //   #719	-> 1076
          //   #720	-> 1089
          //   #721	-> 1094
          //   #723	-> 1106
          //   #725	-> 1113
          //   #726	-> 1120
          //   #727	-> 1131
          //   #728	-> 1156
          //   #729	-> 1196
          //   #730	-> 1257
          //   #723	-> 1265
          //   #734	-> 1275
          //   #736	-> 1277
          //   #738	-> 1292
          //   #740	-> 1294
          //   #741	-> 1306
          //   #743	-> 1316
          //   #641	-> 1339
          //   #745	-> 1361
          // Local variable table:
          //   start	length	slot	name	descriptor
          //   0	1362	0	this	Lcom/crushftp/client/S3Client$1;
          //   7	1355	1	s_master	Ljava/lang/String;
          //   62	57	2	x	I
          //   140	1199	2	part	Ljava/lang/String;
          //   142	1197	3	first	Z
          //   224	1115	4	bucketName	Ljava/lang/String;
          //   247	126	5	path	Ljava/lang/String;
          //   478	861	5	urlc	Lcom/crushftp/client/URLConnection;
          //   503	836	6	offset	J
          //   669	304	8	start_wait	J
          //   815	158	10	e	Ljava/lang/NumberFormatException;
          //   997	278	8	code	I
          //   1023	44	9	result	Ljava/lang/String;
          //   1076	199	9	baos	Ljava/io/ByteArrayOutputStream;
          //   1120	145	11	b	[B
          //   1257	8	12	s	Ljava/lang/String;
          //   1277	12	8	e	Ljava/lang/InterruptedException;
          //   1294	22	8	e	Ljava/io/IOException;
          // Exception table:
          //   from	to	target	type
          //   10	119	122	java/lang/InterruptedException
          //   150	204	207	finally
          //   207	210	207	finally
          //   664	973	973	java/lang/InterruptedException
          //   664	987	1275	java/lang/InterruptedException
          //   664	987	1292	java/io/IOException
          //   687	810	813	java/lang/NumberFormatException
          //   990	1275	1275	java/lang/InterruptedException
          //   990	1275	1292	java/io/IOException
          //   1113	1268	1271	finally
          //   1271	1274	1271	finally
        }
      };
    Runnable grabChunkF = grabChunk;
    Runnable downloadChunks = new Runnable(this, startPos, chunks_needed, status, chunks, sock2, lenF, sock1, grabChunkF) {
        final S3Client this$0;
        
        private final long val$startPos;
        
        private final Vector val$chunks_needed;
        
        private final Properties val$status;
        
        private final Properties val$chunks;
        
        private final Socket val$sock2;
        
        private final long val$lenF;
        
        private final Socket val$sock1;
        
        private final Runnable val$grabChunkF;
        
        public void run() {
          String s_master = Thread.currentThread().getName();
          this.this$0.in_progress.addElement(Thread.currentThread());
          long current_pos = this.val$startPos;
          if (current_pos < 0L)
            current_pos = 0L; 
          try {
            OutputStream out_tmp = this.val$sock2.getOutputStream();
            while (current_pos < this.val$lenF && !this.val$status.containsKey("error") && !this.val$sock1.isClosed()) {
              if (this.this$0.config.getProperty("multithreaded_s3", "true").equals("false")) {
                this.val$status.put("run_once", "true");
                this.val$grabChunkF.run();
              } 
              String s = String.valueOf(s_master.substring(0, s_master.lastIndexOf(":") + 1)) + "Waiting for " + current_pos + " and using " + Common.format_bytes_short(Long.parseLong(this.val$status.getProperty("ram"))) + " of " + Common.format_bytes_short(S3Client.ram_used_download);
              Thread.currentThread().setName(s);
              if (this.val$chunks.containsKey((new StringBuffer(String.valueOf(current_pos))).toString())) {
                byte[] b = (byte[])null;
                synchronized (S3Client.ram_lock) {
                  b = (byte[])this.val$chunks.remove((new StringBuffer(String.valueOf(current_pos))).toString());
                  S3Client.ram_used_download -= b.length;
                  this.val$status.put("ram", (new StringBuffer(String.valueOf(Long.parseLong(this.val$status.getProperty("ram")) - b.length))).toString());
                  s = String.valueOf(s_master.substring(0, s_master.lastIndexOf(":") + 1)) + Common.format_bytes_short(Long.parseLong(this.val$status.getProperty("ram"))) + " of " + Common.format_bytes_short(S3Client.ram_used_download);
                  Thread.currentThread().setName(s);
                } 
                current_pos += b.length;
                this.val$status.put("current_pos", (new StringBuffer(String.valueOf(current_pos))).toString());
                out_tmp.write(b);
                continue;
              } 
              Thread.sleep(100L);
            } 
            if (this.val$status.containsKey("error"))
              throw (Exception)this.val$status.get("error"); 
            this.val$sock2.close();
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.this$0.config.put("error", e);
            this.this$0.config.put("error_msg", (new StringBuffer(String.valueOf(e.getMessage()))).toString());
            Common.log("S3_CLIENT", 2, e);
            try {
              this.this$0.close();
              this.val$sock2.close();
              this.val$sock1.close();
            } catch (Exception exception) {}
          } finally {
            synchronized (this.this$0.in_progress) {
              this.this$0.in_progress.remove(Thread.currentThread());
            } 
            this.val$chunks_needed.removeAllElements();
            if (this.val$status.containsKey("error"))
              try {
                Thread.sleep(5000L);
              } catch (InterruptedException interruptedException) {} 
            Enumeration keys = this.val$chunks.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              synchronized (S3Client.ram_lock) {
                byte[] b = (byte[])this.val$chunks.remove(key);
                S3Client.ram_used_download -= b.length;
                this.val$status.put("ram", (new StringBuffer(String.valueOf(Long.parseLong(this.val$status.getProperty("ram")) - b.length))).toString());
                String s = String.valueOf(s_master.substring(0, s_master.lastIndexOf(":") + 1)) + Common.format_bytes_short(Long.parseLong(this.val$status.getProperty("ram"))) + " of " + Common.format_bytes_short(S3Client.ram_used_download);
                Thread.currentThread().setName(s);
              } 
            } 
          } 
        }
      };
    if (this.config.getProperty("multithreaded_s3", "true").equals("true"))
      for (int x = 0; x < Integer.parseInt(this.config.getProperty("s3_threads_download", "3")); x++)
        Worker.startWorker(grabChunk, "S3 chunked file downloader:" + (x + 1) + "/" + this.config.getProperty("s3_threads_download", "3") + ":" + path0 + ":");  
    Worker.startWorker(downloadChunks, "S3 buffer processor:" + path0 + ":");
    this.in = sock1.getInputStream();
    return this.in;
  }
  
  private String getExt(String path) {
    path = lower(path);
    Properties mimes = Common.mimes;
    String ext = "NULL";
    if (path.toString().lastIndexOf(".") >= 0)
      ext = path.toString().substring(path.toString().lastIndexOf(".")).toUpperCase(); 
    if (mimes.getProperty(ext, "").equals(""))
      ext = "NULL"; 
    try {
      Common.updateMimes();
    } catch (Exception e) {
      Common.log("S3_CLIENT", 2, e);
    } 
    return ext;
  }
  
  protected OutputStream upload3(String path_tmp, long startPos, boolean truncate, boolean binary) throws Exception {
    String path = lower(path_tmp);
    Vector tempResumeParts = null;
    String tempUploadId = "";
    boolean resume = false;
    boolean needCopyResume = false;
    ByteArrayOutputStream temp_buf = new ByteArrayOutputStream();
    if (startPos > 0L) {
      Properties stat = stat(path);
      if (stat.containsKey("resumeParts")) {
        tempResumeParts = (Vector)stat.get("resumeParts");
        tempUploadId = stat.getProperty("uploadId");
        resume = true;
      } 
      if (tempResumeParts == null) {
        resume = false;
        if (Long.parseLong(stat.getProperty("size")) < 5242880L) {
          Common.streamCopier(download3(path, -1L, -1L, true), temp_buf, false, true, false);
          tempResumeParts = new Vector();
          tempResumeParts.addElement(new Properties());
        } else {
          needCopyResume = true;
        } 
      } 
    } 
    if (!resume) {
      String bucketName = path.substring(1);
      String tempPath = path;
      if (bucketName.indexOf("/") >= 0) {
        bucketName = bucketName.substring(0, bucketName.indexOf("/"));
        if (!this.config.getProperty("s3_bucket_in_path", "false").equals("true"))
          tempPath = tempPath.substring(tempPath.indexOf("/", 1)); 
      } 
      String result = "";
      for (int retries = 0; retries < 31; ) {
        String query = "?delimiter=%2F&prefix=" + handle_path_special_chars(path.substring(1), true) + "&uploads";
        boolean s3_sha256 = System.getProperty("crushftp.s3_sha256", "false").equals("true");
        if (this.config.containsKey("s3_sha256"))
          s3_sha256 = this.config.getProperty("s3_sha256", "false").equals("true"); 
        if (s3_sha256 || !this.config.getProperty("server_side_encrypt_kms", "").equals(""))
          query = String.valueOf(query) + "="; 
        URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + query), this.config);
        urlc.setRemoveDoubleEncoding(true);
        urlc.setRequestMethod("POST");
        if (!this.config.getProperty("server_side_encrypt_kms", "").equals("")) {
          urlc.setRequestProperty("x-amz-server-side-encryption", "aws:kms");
          urlc.setRequestProperty("x-amz-server-side-encryption-aws-kms-key-id", this.config.getProperty("server_side_encrypt_kms", ""));
        } else if (this.config.getProperty("server_side_encrypt", "false").equals("true")) {
          urlc.setRequestProperty("x-amz-server-side-encryption", "AES256");
        } 
        urlc.setDoOutput(false);
        doStandardAmazonAlterations(urlc, Common.mimes.getProperty(getExt(path)), bucketName);
        int code = urlc.getResponseCode();
        result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code < 200 || code > 299) {
          log(String.valueOf(result) + "\r\n");
          if (retries >= 30 || result.indexOf("InternalError") < 0)
            throw new IOException(result); 
          log("Retrying failed upload start..." + path);
          Thread.sleep((retries * 1000));
          retries++;
        } 
        break;
      } 
      Element root = Common.getSaxBuilder().build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
      tempUploadId = getKeyText(root, "UploadId");
      tempResumeParts = new Vector();
    } 
    if (needCopyResume)
      doCopyResume(tempResumeParts, new Vector(), (new StringBuffer()).append("1"), (new StringBuffer()).append("1"), tempUploadId, path, path, startPos); 
    StringBuffer partNumber = new StringBuffer();
    partNumber.append("1");
    Vector resumeParts = tempResumeParts;
    String uploadId = tempUploadId;
    long maxBufferSize = Long.parseLong(this.config.getProperty("s3_buffer", "5")) * 1024L * 1024L;
    Vector resumePartsDone = new Vector();
    if (temp_buf.size() > 0) {
      int i = Integer.parseInt(partNumber.toString());
      partNumber.setLength(0);
      partNumber.append((new StringBuffer(String.valueOf(i + 1))).toString());
      flushNow(resumePartsDone, temp_buf, resumeParts, uploadId, path, i, i, false);
    } 
    get_cache_item("list_cache").remove(Common.all_but_last(path));
    this.out = new null.OutputWrapper(this, maxBufferSize, partNumber, resumeParts, resumePartsDone, uploadId, path);
    return this.out;
  }
  
  public static Object s3_buffer_lock = new Object();
  
  public void flushNow(Vector resumePartsDone, ByteArrayOutputStream buf2, Vector resumeParts, String uploadId, String path, int partNumber, int partNumberPos, boolean ignoreZero) throws IOException {
    if (buf2.size() == 0 && !ignoreZero)
      return; 
    String bucketName = path.substring(1);
    String tempPath = path;
    if (bucketName.indexOf("/") >= 0) {
      bucketName = bucketName.substring(0, bucketName.indexOf("/"));
      if (!this.config.getProperty("s3_bucket_in_path", "false").equals("true"))
        tempPath = tempPath.substring(tempPath.indexOf("/", 1)); 
    } 
    Properties chunk_part = resumeParts.elementAt(partNumberPos - 1);
    chunk_part.put("start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    for (int loops = 0; loops < 20; loops++) {
      URLConnection urlc = null;
      try {
        if (uploadId == null) {
          urlc = URLConnection.openConnection(new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + "_" + partNumber), this.config);
        } else {
          urlc = URLConnection.openConnection(new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + "?partNumber=" + partNumber + "&uploadId=" + uploadId), this.config);
        } 
        chunk_part.put("urlc", urlc);
        urlc.setRemoveDoubleEncoding(true);
        urlc.setRequestMethod("PUT");
        urlc.setDoOutput(true);
        urlc.setLength(buf2.size());
        doStandardAmazonAlterations(urlc, (String)null, bucketName);
        long start = System.currentTimeMillis();
        chunk_part.put("start", (new StringBuffer(String.valueOf(start))).toString());
        try {
          OutputStream tmp_out = urlc.getOutputStream();
          Common.log("S3_CLIENT", 2, urlc + ":Writing part " + partNumber + " with " + buf2.size() + " bytes to AWS...");
          ByteArrayInputStream inb = new ByteArrayInputStream(buf2.toByteArray());
          int bytesRead = 0;
          byte[] b = new byte[32768];
          while (bytesRead >= 0) {
            bytesRead = inb.read(b);
            if (bytesRead > 0)
              tmp_out.write(b, 0, bytesRead); 
            chunk_part.put("start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
          } 
          inb.close();
          tmp_out.close();
          Common.log("S3_CLIENT", 2, urlc + ":Done writing part " + partNumber + " to AWS.");
        } catch (Exception e) {
          Common.log("S3_CLIENT", 1, e);
        } 
        int code = urlc.getResponseCode();
        Common.log("S3_CLIENT", 2, urlc + ":Got part " + partNumber + " response:" + code + " time:" + (System.currentTimeMillis() - start) + "ms");
        String result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code < 200 || code > 299) {
          log(String.valueOf(result) + "\r\n");
          throw new IOException(result);
        } 
        buf2.reset();
        Common.log("S3_CLIENT", 2, urlc + ":Got part " + partNumber + " chunk id:" + urlc.getHeaderField("ETag") + " time:" + (System.currentTimeMillis() - start) + "ms");
        chunk_part.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis() - start))).toString());
        chunk_part.put("etag", urlc.getHeaderField("ETag"));
        Common.log("S3_CLIENT", 2, "resumeParts:" + resumeParts);
        resumePartsDone.addElement((new StringBuffer(String.valueOf(partNumber))).toString());
        break;
      } catch (IOException e) {
        if (loops > 3)
          try {
            chunk_part.put("start", (new StringBuffer(String.valueOf(System.currentTimeMillis() + (1000 * loops)))).toString());
            Thread.sleep((1000 * loops));
          } catch (InterruptedException interruptedException) {} 
        if (loops > 8)
          throw e; 
        Common.log("S3_CLIENT", 1, e);
      } finally {
        chunk_part.remove("urlc");
        urlc.disconnect();
      } 
    } 
  }
  
  public void doCopyResume(Vector resumeParts, Vector resumePartsDone, StringBuffer partNumber, StringBuffer partNumberPos, String uploadId, String old_path, String new_path, long startPos) throws Exception {
    String bucketName = new_path.substring(1);
    String tempPath = new_path;
    if (bucketName.indexOf("/") >= 0) {
      bucketName = bucketName.substring(0, bucketName.indexOf("/"));
      if (!this.config.getProperty("s3_bucket_in_path", "false").equals("true"))
        tempPath = tempPath.substring(tempPath.indexOf("/", 1)); 
    } 
    long total_bytes = startPos;
    long pos = 0L;
    while (total_bytes > 0L) {
      long chunk_size = total_bytes;
      if (chunk_size > 1073741824L)
        chunk_size = 1073741824L; 
      URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + "?partNumber=" + partNumber + "&uploadId=" + uploadId), this.config);
      urlc.setRemoveDoubleEncoding(true);
      urlc.setRequestMethod("PUT");
      urlc.setRequestProperty("x-amz-copy-source", Common.url_decode(handle_path_special_chars(old_path, true)));
      urlc.setRequestProperty("x-amz-copy-source-range", "bytes=" + pos + "-" + (pos + chunk_size - 1L));
      doStandardAmazonAlterations(urlc, (String)null, bucketName);
      int code = urlc.getResponseCode();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        throw new IOException(result);
      } 
      Element root = Common.getSaxBuilder().build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
      Properties chunk_part = new Properties();
      chunk_part.put("etag", getKeyText(root, "ETag"));
      resumeParts.addElement(chunk_part);
      resumePartsDone.addElement(partNumber.toString());
      int partNum = Integer.parseInt(partNumber.toString()) + 1;
      partNumber.setLength(0);
      partNumber.append((new StringBuffer(String.valueOf(partNum))).toString());
      int partNumPos = Integer.parseInt(partNumberPos.toString()) + 1;
      partNumberPos.setLength(0);
      partNumberPos.append((new StringBuffer(String.valueOf(partNumPos))).toString());
      total_bytes -= chunk_size;
      pos += chunk_size;
    } 
  }
  
  public void finishUpload(Vector resumeParts, String path, String uploadId) throws IOException {
    if (uploadId == null)
      return; 
    StringBuffer sb = new StringBuffer();
    sb.append("<CompleteMultipartUpload>");
    for (int x = 0; x < resumeParts.size(); x++) {
      Properties chunk_part = resumeParts.elementAt(x);
      if (chunk_part.getProperty("etag") != null) {
        sb.append("<Part>");
        sb.append("<PartNumber>" + (x + 1) + "</PartNumber>");
        sb.append("<ETag>" + chunk_part.getProperty("etag") + "</ETag>");
        sb.append("</Part>");
      } 
    } 
    sb.append("</CompleteMultipartUpload>");
    byte[] b = sb.toString().getBytes("UTF8");
    String bucketName = path.substring(1);
    String tempPath = path;
    if (bucketName.indexOf("/") >= 0) {
      bucketName = bucketName.substring(0, bucketName.indexOf("/"));
      if (!this.config.getProperty("s3_bucket_in_path", "false").equals("true"))
        tempPath = tempPath.substring(tempPath.indexOf("/", 1)); 
    } 
    int code = -1;
    String result = "";
    int loops = 0;
    while (code == -1 && loops++ < 60) {
      try {
        URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + "?uploadId=" + uploadId), this.config);
        urlc.setRemoveDoubleEncoding(true);
        urlc.setRequestMethod("POST");
        urlc.setDoOutput(true);
        urlc.setLength(b.length);
        doStandardAmazonAlterations(urlc, (String)null, bucketName);
        this.out3 = urlc.getOutputStream();
        this.out3.write(b);
        this.out3.close();
        Common.log("S3_CLIENT", 2, sb.toString());
        code = urlc.getResponseCode();
        result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code >= 200 && code < 299 && result.indexOf("InternalError") < 0)
          break; 
        log("Trying to finish the upload again, no response from AWS:" + code + "\r\n");
      } catch (Exception exception) {}
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException interruptedException) {}
    } 
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      throw new IOException(result);
    } 
  }
  
  public boolean delete(String path) throws Exception {
    path = lower(path);
    int code = 0;
    String result = "";
    int loops = 1;
    boolean s3_one_delete_attempt = System.getProperty("crushftp.s3_one_delete_attempt", "false").equals("true");
    while (code != 404 && loops++ < 100) {
      Properties stat = stat(path);
      if (stat == null)
        stat = new Properties(); 
      boolean is_folder = stat.getProperty("type", "FILE").equalsIgnoreCase("DIR");
      URLConnection urlc = doAction("DELETE", String.valueOf(path) + (stat.getProperty("type", "FILE").equalsIgnoreCase("DIR") ? "/" : "") + (!stat.getProperty("uploadId", "").equals("") ? ("?uploadId=" + stat.getProperty("uploadId", "")) : ""), (StringBuffer)null, false, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
      code = urlc.getResponseCode();
      result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if ((code < 200 || code > 299) && code != 404) {
        log(String.valueOf(result) + "\r\n");
        return false;
      } 
      get_cache_item("cache_resume").clear();
      updateCache(stat, path, "remove");
      if (get_cache_item("recently_created_folder_cache").containsKey(path))
        get_cache_item("recently_created_folder_cache").remove(path); 
      if (stat.size() == 0 || (
        s3_one_delete_attempt && stat(path) == null))
        break; 
      if (is_folder && stat(path) != null)
        return false; 
    } 
    return (loops < 99);
  }
  
  private void updateCache(Properties stat, String path, String action) throws Exception {
    path = lower(path);
    Properties ctmp = (Properties)get_cache_item("list_cache").get(Common.all_but_last(path));
    if (ctmp != null) {
      Vector v = (Vector)ctmp.get("o");
      for (int x = 0; x < v.size(); x++) {
        Properties p = v.elementAt(x);
        if (p.getProperty("url", "").equals(stat.getProperty("url", ""))) {
          if (action.equals("remove")) {
            v.remove(x);
          } else if (action.equals("modified")) {
            p.put("modified", stat.getProperty("modified"));
          } 
          get_cache_item("list_cache").put(Common.all_but_last(path), ctmp);
          break;
        } 
      } 
    } 
    ctmp = (Properties)get_cache_item("cache_resume").get(Common.all_but_last(path));
    if (ctmp != null) {
      Vector v = (Vector)ctmp.get("o");
      for (int x = 0; x < v.size(); x++) {
        Properties p = v.elementAt(x);
        if (p.getProperty("url", "").equals(stat.getProperty("url", ""))) {
          if (action.equals("remove")) {
            v.remove(x);
          } else if (action.equals("modified")) {
            p.put("modified", stat.getProperty("modified"));
          } 
          get_cache_item("cache_resume").put(Common.all_but_last(path), ctmp);
          break;
        } 
      } 
    } 
    Properties stat_cache = get_cache_item("stat_cache");
    if (action.equals("remove") && stat_cache.containsKey(path))
      stat_cache.remove(path); 
    if (action.equals("modified") && stat_cache.containsKey(path))
      ((Properties)stat_cache.get(path)).put("modified", stat.getProperty("modified")); 
  }
  
  public boolean makedir(String path) throws Exception {
    path = lower(path);
    if (is_folder_recently_created(path))
      return true; 
    URLConnection urlc = doAction("PUT", path, (StringBuffer)null, true, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
    urlc.setLength(0L);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
    get_cache_item("stat_cache").clear();
    get_cache_item("list_cache").clear();
    get_cache_item("cache_resume").clear();
    get_cache_item("recently_created_folder_cache").put(path, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    return true;
  }
  
  public boolean makedirs(String path) throws Exception {
    path = lower(path);
    boolean ok = true;
    String[] parts = path.startsWith("/") ? path.substring(1, path.length()).split("/") : path.split("/");
    String path2 = "/";
    for (int x = 0; x < parts.length && ok; x++) {
      path2 = String.valueOf(path2) + parts[x] + "/";
      if (x >= 1)
        if (this.config.getProperty("s3_stat_head_calls", "true").equals("true") && stat(path2) == null)
          makedir(path2);  
    } 
    return ok;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    rnfr = lower(rnfr);
    rnto = lower(rnto);
    get_cache_item("stat_cache").remove(rnfr);
    get_cache_item("stat_cache").remove(rnto);
    Properties stat = stat(rnfr);
    if (stat.getProperty("type").equalsIgnoreCase("DIR"))
      return false; 
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("PUT", rnto, bucketNameSB, true, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
    urlc.setRequestProperty("x-amz-copy-source", Common.url_decode(handle_path_special_chars(rnfr, true)));
    Properties metadata = getMetadata(rnfr);
    Enumeration keys = metadata.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      urlc.setRequestProperty(key, metadata.getProperty(key));
    } 
    urlc.setRequestProperty("x-amz-metadata-directive", "REPLACE");
    doStandardAmazonAlterations(urlc, Common.mimes.getProperty(getExt(handle_path_special_chars(rnto, false))), bucketNameSB.toString());
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    get_cache_item("list_cache").clear();
    get_cache_item("cache_resume").clear();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
    delete(rnfr);
    return true;
  }
  
  public Properties stat(String path) throws Exception {
    path = lower(path);
    if (path.endsWith(":filetree"))
      path = path.substring(0, path.indexOf(":filetree") - 1); 
    if (this.config.getProperty("s3_stat_head_calls", "true").equals("true"))
      return stat_head_calls(path); 
    Vector v = new Vector();
    list(Common.all_but_last(path), v);
    return stat_list(path, v);
  }
  
  private Properties stat_head_calls(String path) throws Exception {
    path = lower(path);
    Vector list = null;
    Properties ctmp = (Properties)get_cache_item("list_cache").get(Common.all_but_last(path));
    if (ctmp != null && System.currentTimeMillis() - Long.parseLong(ctmp.getProperty("time")) < 30000L)
      list = (Vector)Common.CLONE(ctmp.get("o")); 
    Properties info = null;
    if (list != null)
      info = stat_list(path, list); 
    if (info == null) {
      boolean cache_now = true;
      if (get_cache_item("stat_cache").containsKey(path)) {
        cache_now = false;
        Properties p = (Properties)get_cache_item("stat_cache").get(path);
        if (System.currentTimeMillis() - Long.parseLong(p.getProperty("time", "0")) < 30000L) {
          if (p.getProperty("exists", "").equals("false"))
            return null; 
          return (Properties)Common.CLONE(p.get("info"));
        } 
        get_cache_item("stat_cache").remove(path);
        cache_now = true;
      } 
      if (!path.endsWith("/"))
        info = getS3ObjectInfo(path); 
      if (info == null) {
        Vector v = new Vector();
        v = list(path, v, 1);
        if (v.size() > 0) {
          String bucketName = path.substring(1);
          String path2 = path;
          if (bucketName.indexOf("?") >= 0)
            bucketName = bucketName.substring(0, bucketName.indexOf("?")); 
          if (bucketName.indexOf("/") >= 0) {
            bucketName = bucketName.substring(0, bucketName.indexOf("/") + 1);
            path2 = path2.substring(path2.indexOf("/", 1));
          } 
          if (path2.endsWith("/"))
            path2 = path2.substring(0, path2.length() - 1); 
          if (path2.equals(""))
            path2 = "/"; 
          Date d = new Date(0L);
          String line = "drwxrwxrwx   1    owner   group   0   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + Common.last(path2);
          Properties stat = parseStat(line);
          if (Common.url_decode(this.url).endsWith(bucketName))
            bucketName = ""; 
          stat.put("url", String.valueOf(Common.url_decode(this.url)) + bucketName + path2.substring(1));
          stat.put("check_all_recursive_deletes", "true");
          if (ctmp != null && list != null)
            list.add(stat); 
          info = stat;
          cache_now = true;
        } 
        boolean s3_partial = System.getProperty("crushftp.s3_partial", "true").equals("true");
        if (this.config.containsKey("s3_partial"))
          s3_partial = this.config.getProperty("s3_partial", "true").equals("true"); 
        if (info == null && s3_partial) {
          String path2 = path;
          String bucketName = "";
          if (!path2.equals("/")) {
            bucketName = path2.substring(1, path2.indexOf("/", 1));
            path2 = path2.substring(bucketName.length() + 1);
          } 
          if (path2.endsWith("/"))
            path2 = path2.substring(0, path2.length() - 1); 
          Vector v2 = new Vector();
          if (v2 != null) {
            v2 = get_uploads_in_progress_or_failed_uploads(v2, path2, bucketName);
            info = stat_list(path, v2);
          } 
        } 
      } else if (info != null) {
        String bucketName = path.substring(1);
        String path2 = path;
        if (bucketName.indexOf("?") >= 0)
          bucketName = bucketName.substring(0, bucketName.indexOf("?")); 
        if (bucketName.indexOf("/") >= 0) {
          bucketName = bucketName.substring(0, bucketName.indexOf("/") + 1);
          path2 = path2.substring(path2.indexOf("/", 1));
        } 
        if (path2.endsWith("/"))
          path2 = path2.substring(0, path2.length() - 1); 
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date d = sdf.parse(info.getProperty("LAST-MODIFIED"));
        String line = "-rwxrwxrwx   1    owner   group   " + info.getProperty("CONTENT-LENGTH", "0") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + Common.last(path2);
        Properties stat = parseStat(line);
        if (Common.url_decode(this.url).endsWith(bucketName))
          bucketName = ""; 
        stat.put("url", String.valueOf(Common.url_decode(this.url)) + bucketName + path2.substring(1));
        if (ctmp != null && list != null)
          list.add(stat); 
        info = stat;
      } 
      if (cache_now) {
        Properties p = new Properties();
        if (info == null) {
          p.put("exists", "false");
        } else {
          p.put("info", Common.CLONE(info));
        } 
        p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        get_cache_item("stat_cache").put(path, p);
      } 
    } 
    return info;
  }
  
  private Properties stat_list(String path, Vector v) throws Exception {
    path = lower(path);
    String last_path = Common.last(path);
    for (int x = 0; x < v.size(); x++) {
      Properties p = v.elementAt(x);
      if (p.getProperty("name").equals(last_path))
        return p; 
      if (last_path.endsWith("/") && (String.valueOf(p.getProperty("name")) + "/").equals(last_path))
        return p; 
    } 
    return null;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    path = lower(path);
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("PUT", path, bucketNameSB, true, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
    urlc.setLength(0L);
    urlc.setRequestProperty("x-amz-copy-source", Common.url_decode(handle_path_special_chars(path, true)));
    urlc.setRequestProperty("x-amz-meta-modified", (new StringBuffer(String.valueOf(modified))).toString());
    Properties p = getS3ObjectInfo(path);
    if (p != null) {
      if (p.containsKey("X-AMZ-META-UPLOADED-BY")) {
        urlc.setRequestProperty("x-amz-meta-uploaded-by", p.getProperty("X-AMZ-META-UPLOADED-BY", ""));
      } else if (!this.config.getProperty("uploaded_by", "").equals("")) {
        urlc.setRequestProperty("x-amz-meta-uploaded-by", this.config.getProperty("uploaded_by", ""));
      } 
      if (p.containsKey("X-AMZ-META-MD5")) {
        urlc.setRequestProperty("x-amz-meta-md5", p.getProperty("X-AMZ-META-MD5", ""));
      } else if (!this.config.getProperty("uploaded_md5", "").equals("")) {
        urlc.setRequestProperty("x-amz-meta-md5", this.config.getProperty("uploaded_md5", ""));
      } 
      if (p.containsKey("X-AMZ-SERVER-SIDE-ENCRYPTION"))
        urlc.setRequestProperty("x-amz-server-side-encryption", (String)p.get("X-AMZ-SERVER-SIDE-ENCRYPTION")); 
    } 
    urlc.setRequestProperty("x-amz-metadata-directive", "REPLACE");
    doStandardAmazonAlterations(urlc, Common.mimes.getProperty(getExt(path)), bucketNameSB.toString());
    Worker.startWorker(new Runnable(this, urlc) {
          final S3Client this$0;
          
          private final URLConnection val$urlc;
          
          public void run() {
            try {
              int code = this.val$urlc.getResponseCode();
              String result = URLConnection.consumeResponse(this.val$urlc.getInputStream());
              this.val$urlc.disconnect();
              if (code < 200 || code > 299)
                this.this$0.log(String.valueOf(result) + "\r\n"); 
            } catch (Exception e) {
              this.this$0.log(e);
            } 
          }
        });
    String bucketName = "";
    if (!path.equals("/")) {
      bucketName = path.substring(1, path.indexOf("/", 1));
      path = path.substring(bucketName.length() + 1);
    } 
    Properties stat = new Properties();
    stat.put("url", String.valueOf(this.url) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1));
    stat.put("modified", (new StringBuffer(String.valueOf(modified))).toString());
    updateCache(stat, path, "modified");
    return true;
  }
  
  public Properties getMetadata(String path) throws IOException, SocketTimeoutException {
    path = lower(path);
    Properties header_properties = new Properties();
    Properties s3ObjectInfo = getS3ObjectInfo(path);
    if (s3ObjectInfo == null)
      return null; 
    Enumeration keys = s3ObjectInfo.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (key.startsWith("X-AMZ-META-"))
        header_properties.put(key.toLowerCase(), s3ObjectInfo.get(key)); 
    } 
    return header_properties;
  }
  
  private Properties getS3ObjectInfo(String path) throws IOException, SocketTimeoutException {
    path = lower(path);
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("HEAD", path, bucketNameSB, false, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
    urlc.setLength(0L);
    doStandardAmazonAlterations(urlc, Common.mimes.getProperty(getExt(path)), bucketNameSB.toString());
    int code = urlc.getResponseCode();
    if (code < 200 || code > 299) {
      log("S3 object info : path = " + path + "error message: " + urlc.getResponseMessage() + "\r\n");
      return null;
    } 
    Properties p = (Properties)urlc.headers.clone();
    urlc.disconnect();
    return p;
  }
  
  public String getUploadedByMetadata(String path) {
    path = lower(path);
    Properties p = null;
    try {
      p = getMetadata(path);
    } catch (Exception e) {
      Common.log("S3_CLIENT", 1, e);
    } 
    if (p != null && p.containsKey("x-amz-meta-uploaded-by") && !p.getProperty("x-amz-meta-uploaded-by").equals(""))
      return p.getProperty("x-amz-meta-uploaded-by"); 
    return "";
  }
  
  public void set_MD5_and_upload_id(String path) throws Exception {
    path = lower(path);
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("PUT", path, bucketNameSB, true, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
    urlc.setLength(0L);
    urlc.setRequestProperty("x-amz-copy-source", Common.url_decode(handle_path_special_chars(path, true)));
    Properties p = getS3ObjectInfo(path);
    if (p != null) {
      if (p.containsKey("X-AMZ-META-UPLOADED-BY")) {
        urlc.setRequestProperty("x-amz-meta-uploaded-by", p.getProperty("X-AMZ-META-UPLOADED-BY", ""));
      } else if (!this.config.getProperty("uploaded_by", "").equals("")) {
        urlc.setRequestProperty("x-amz-meta-uploaded-by", this.config.getProperty("uploaded_by", ""));
      } 
      if (p.containsKey("X-AMZ-META-MD5")) {
        urlc.setRequestProperty("x-amz-meta-md5", p.getProperty("X-AMZ-META-MD5", ""));
      } else if (!this.config.getProperty("uploaded_md5", "").equals("")) {
        urlc.setRequestProperty("x-amz-meta-md5", this.config.getProperty("uploaded_md5", ""));
      } 
      if (p.containsKey("X-AMZ-META-MODIFIED"))
        urlc.setRequestProperty("x-amz-meta-modified", p.getProperty("X-AMZ-META-MODIFIED", "")); 
      if (p.containsKey("X-AMZ-SERVER-SIDE-ENCRYPTION"))
        urlc.setRequestProperty("x-amz-server-side-encryption", (String)p.get("X-AMZ-SERVER-SIDE-ENCRYPTION")); 
    } 
    urlc.setRequestProperty("x-amz-metadata-directive", "REPLACE");
    doStandardAmazonAlterations(urlc, Common.mimes.getProperty(getExt(path)), bucketNameSB.toString());
    Worker.startWorker(new Runnable(this, urlc) {
          final S3Client this$0;
          
          private final URLConnection val$urlc;
          
          public void run() {
            try {
              int code = this.val$urlc.getResponseCode();
              String result = URLConnection.consumeResponse(this.val$urlc.getInputStream());
              this.val$urlc.disconnect();
              if (code < 200 || code > 299)
                this.this$0.log(String.valueOf(result) + "\r\n"); 
            } catch (Exception e) {
              this.this$0.log(e);
            } 
          }
        });
  }
  
  public void doStandardAmazonAlterations(URLConnection urlc, String contentType, String bucketName) {
    try {
      updateIamAuth();
    } catch (Exception e) {
      Common.log("S3_CLIENT", 0, e);
    } 
    urlc.setRequestProperty("Content-Type", contentType);
    urlc.setRequestProperty("Accept", null);
    urlc.setRequestProperty("Pragma", null);
    if (this.config.getProperty("s3_bucket_in_path", "false").equals("true")) {
      urlc.setRequestProperty("Host", this.region_host);
    } else if (!bucketName.equals("")) {
      urlc.setRequestProperty("Host", String.valueOf(bucketName) + "." + this.region_host);
    } 
    urlc.setRequestProperty("Cache", null);
    urlc.setRequestProperty("Cache-Control", null);
    if (this.config.containsKey("real_token"))
      urlc.setRequestProperty("x-amz-security-token", this.config.getProperty("real_token")); 
    boolean s3_sha256 = System.getProperty("crushftp.s3_sha256", "false").equals("true");
    if (this.config.containsKey("s3_sha256"))
      s3_sha256 = this.config.getProperty("s3_sha256", "false").equals("true"); 
    if (!s3_sha256 && this.config.getProperty("server_side_encrypt_kms", "").equals("")) {
      urlc.setRequestProperty("Authorization", "AWS " + this.config.getProperty("real_username", this.config.getProperty("username")) + ":" + calculateAmazonSignature(urlc));
    } else {
      try {
        urlc.setRequestProperty("Authorization", calculateAmazonSignaturev4(urlc));
        urlc.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
        urlc.setRequestProperty("x-amz-date", this.yyyyMMddtHHmmssZ.format(urlc.getDate()));
      } catch (Exception e) {
        e.printStackTrace();
      } 
    } 
    urlc.setUseCaches(false);
  }
  
  public URLConnection doAction(String verb, String path, StringBuffer bucketNameSB, boolean do_secure, boolean handle_special_chars, boolean bucket_in_path) {
    path = lower(path);
    String bucketName = path.substring(1);
    if (bucketName.indexOf("?") >= 0)
      bucketName = bucketName.substring(0, bucketName.indexOf("?")); 
    if (bucketName.indexOf("/") >= 0) {
      bucketName = bucketName.substring(0, bucketName.indexOf("/"));
      if (!bucket_in_path)
        path = path.substring(path.indexOf("/", 1)); 
    } 
    if (path.equals("/" + bucketName))
      path = "/"; 
    if (handle_special_chars)
      if (path.contains("?uploadId=")) {
        path = String.valueOf(handle_path_special_chars(path.substring(0, path.indexOf("?uploadId=")), false)) + path.substring(path.indexOf("?uploadId="), path.length());
      } else {
        path = handle_path_special_chars(path, false);
      }  
    VRL vrl = new VRL(String.valueOf(this.http_protocol) + "://" + ((bucketName.equals("") || bucket_in_path) ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + path);
    Common.log("S3_CLIENT", 1, "S3 URL:" + verb + ":" + vrl.safe());
    URLConnection urlc = URLConnection.openConnection(vrl, this.config);
    urlc.setRemoveDoubleEncoding(true);
    urlc.setRequestMethod(verb);
    urlc.setDoOutput(false);
    if (do_secure && !this.config.getProperty("server_side_encrypt_kms", "").equals("")) {
      urlc.setRequestProperty("x-amz-server-side-encryption", "aws:kms");
      urlc.setRequestProperty("x-amz-server-side-encryption-aws-kms-key-id", this.config.getProperty("server_side_encrypt_kms", ""));
    } else if (do_secure && this.config.getProperty("server_side_encrypt", "false").equals("true")) {
      urlc.setRequestProperty("x-amz-server-side-encryption", "AES256");
    } 
    doStandardAmazonAlterations(urlc, (String)null, bucketName);
    if (bucketNameSB != null) {
      bucketNameSB.setLength(0);
      bucketNameSB.append(bucketName);
    } 
    return urlc;
  }
  
  public String calculateAmazonSignature(URLConnection urlc) {
    String data = String.valueOf(urlc.getRequestMethod()) + "\n";
    data = String.valueOf(data) + "\n";
    data = String.valueOf(data) + urlc.getContentType() + "\n";
    data = String.valueOf(data) + urlc.sdf_rfc1123.format(urlc.getDate()) + "\n";
    String bucketName = urlc.getRequestProps().getProperty("HOST", urlc.getURL().getHost());
    if (this.region_host.contains(":"))
      bucketName = String.valueOf(urlc.getRequestProps().getProperty("HOST", urlc.getURL().getHost())) + ":" + urlc.getURL().getPort(); 
    if (this.config.getProperty("s3_bucket_in_path", "false").equals("true")) {
      bucketName = urlc.getURL().getPath().substring(1, urlc.getURL().getPath().indexOf("/", 1));
    } else if (bucketName.equalsIgnoreCase(this.region_host)) {
      bucketName = "";
    } else {
      bucketName = bucketName.substring(0, bucketName.toLowerCase().indexOf("." + this.region_host));
    } 
    Properties props = urlc.getRequestProps();
    Vector recs = new Vector();
    Enumeration keys = props.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (key.startsWith("x-amz-"))
        recs.addElement(String.valueOf(key) + ":" + props.getProperty(key).trim()); 
    } 
    for (int j = 0; j < recs.size(); j++) {
      String a = recs.elementAt(j).toString();
      for (int k = j; k < recs.size(); k++) {
        String b = recs.elementAt(k).toString();
        if (a.toLowerCase().substring(0, a.indexOf(":")).compareTo(b.toLowerCase().substring(0, b.indexOf(":"))) > 0) {
          recs.setElementAt(b, j);
          recs.setElementAt(a, k);
          a = b;
        } 
      } 
    } 
    for (int x = 0; x < recs.size(); x++)
      data = String.valueOf(data) + recs.elementAt(x) + "\n"; 
    String tmp_path = "";
    if (bucketName.equals("")) {
      tmp_path = "/";
    } else if (urlc.getURL().getPath().indexOf("&uploads") >= 0) {
      tmp_path = "/" + bucketName + urlc.getURL().getPath().substring(0, urlc.getURL().getPath().indexOf("?")) + "?uploads";
    } else if (urlc.getURL().getPath().indexOf("?delimiter") >= 0) {
      if (this.config.getProperty("s3_bucket_in_path", "false").equals("true")) {
        tmp_path = urlc.getURL().getPath().substring(0, urlc.getURL().getPath().indexOf("?"));
      } else {
        tmp_path = "/" + bucketName + urlc.getURL().getPath().substring(0, urlc.getURL().getPath().indexOf("?"));
      } 
    } else if (this.config.getProperty("s3_bucket_in_path", "false").equals("true")) {
      tmp_path = urlc.getURL().getPath();
    } else {
      tmp_path = "/" + bucketName + urlc.getURL().getPath();
    } 
    tmp_path = Common.url_encode(tmp_path, "/.#@&?!\\=+~");
    if (urlc.getRemoveDoubleEncoding())
      tmp_path = URLConnection.remove_double_encoding_of_special_chars(tmp_path); 
    data = String.valueOf(data) + tmp_path;
    Common.log("S3_CLIENT", 2, "Signing data:----------------\n" + data + "\n----------------");
    String sign = "";
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(this.secretKey);
      sign = Base64.encodeBytes(mac.doFinal(data.getBytes("UTF8")));
    } catch (Exception e) {
      Common.log("S3_CLIENT", 1, e);
    } 
    return sign;
  }
  
  public String calculateAmazonSignaturev4(URLConnection urlc) {
    try {
      setRegionName();
      SimpleDateFormat yyyymmdd = new SimpleDateFormat("yyyyMMdd", Locale.US);
      yyyymmdd.setTimeZone(TimeZone.getTimeZone("GMT"));
      String amzdate = this.yyyyMMddtHHmmssZ.format(urlc.getDate());
      String datestamp = yyyymmdd.format(urlc.getDate());
      String bucketName = urlc.getRequestProps().getProperty("HOST", urlc.getURL().getHost());
      if (this.region_host.contains(":"))
        bucketName = String.valueOf(urlc.getRequestProps().getProperty("HOST", urlc.getURL().getHost())) + ":" + urlc.getURL().getPort(); 
      if (this.config.getProperty("s3_bucket_in_path", "false").equals("true")) {
        bucketName = urlc.getURL().getPath().substring(1, urlc.getURL().getPath().indexOf("/", 1));
      } else if (bucketName.equalsIgnoreCase(this.region_host)) {
        bucketName = "";
      } else {
        bucketName = bucketName.substring(0, bucketName.toLowerCase().indexOf("." + this.region_host));
      } 
      String tmp_path = "";
      if (bucketName.equals("")) {
        tmp_path = "/";
      } else if (urlc.getURL().getPath().indexOf("?") >= 0) {
        tmp_path = urlc.getURL().getPath().substring(0, urlc.getURL().getPath().indexOf("?"));
      } else {
        tmp_path = urlc.getURL().getPath();
      } 
      String canonical_uri = Common.url_encode(tmp_path, "/.#@&?!\\=+~");
      if (urlc.getRemoveDoubleEncoding())
        canonical_uri = URLConnection.remove_double_encoding_of_special_chars(canonical_uri); 
      String secret_key = this.config.getProperty("real_password", this.config.getProperty("password"));
      String canonical_headers = "";
      if (this.config.getProperty("s3_bucket_in_path", "false").equals("true")) {
        canonical_headers = "host:" + this.region_host + "\n" + "x-amz-content-sha256:UNSIGNED-PAYLOAD" + "\n";
      } else {
        String host = urlc.getURL().getHost();
        if (this.region_host.contains(":"))
          host = String.valueOf(host) + ":" + urlc.getURL().getPort(); 
        canonical_headers = "host:" + host + "\n" + "x-amz-content-sha256:UNSIGNED-PAYLOAD" + "\n";
      } 
      String signed_headers = "host;x-amz-content-sha256";
      if (urlc.getRequestProps().containsKey("x-amz-copy-source")) {
        signed_headers = String.valueOf(signed_headers) + ";x-amz-copy-source";
        canonical_headers = String.valueOf(canonical_headers) + "x-amz-copy-source:" + urlc.getRequestProps().getProperty("x-amz-copy-source") + "\n";
      } 
      signed_headers = String.valueOf(signed_headers) + ";x-amz-date";
      canonical_headers = String.valueOf(canonical_headers) + "x-amz-date:" + amzdate + "\n";
      if (urlc.getRequestProps().containsKey("x-amz-meta-md5")) {
        signed_headers = String.valueOf(signed_headers) + ";x-amz-meta-md5";
        canonical_headers = String.valueOf(canonical_headers) + "x-amz-meta-md5:" + urlc.getRequestProps().getProperty("x-amz-meta-md5") + "\n";
      } 
      if (urlc.getRequestProps().containsKey("x-amz-meta-modified")) {
        signed_headers = String.valueOf(signed_headers) + ";x-amz-meta-modified";
        canonical_headers = String.valueOf(canonical_headers) + "x-amz-meta-modified:" + urlc.getRequestProps().getProperty("x-amz-meta-modified") + "\n";
      } 
      if (urlc.getRequestProps().containsKey("x-amz-meta-uploaded-by")) {
        signed_headers = String.valueOf(signed_headers) + ";x-amz-meta-uploaded-by";
        canonical_headers = String.valueOf(canonical_headers) + "x-amz-meta-uploaded-by:" + urlc.getRequestProps().getProperty("x-amz-meta-uploaded-by") + "\n";
      } 
      if (urlc.getRequestProps().containsKey("x-amz-metadata-directive")) {
        signed_headers = String.valueOf(signed_headers) + ";x-amz-metadata-directive";
        canonical_headers = String.valueOf(canonical_headers) + "x-amz-metadata-directive:" + urlc.getRequestProps().getProperty("x-amz-metadata-directive") + "\n";
      } 
      if (urlc.getRequestProps().containsKey("x-amz-security-token")) {
        signed_headers = String.valueOf(signed_headers) + ";x-amz-security-token";
        canonical_headers = String.valueOf(canonical_headers) + "x-amz-security-token:" + urlc.getRequestProps().getProperty("x-amz-security-token") + "\n";
      } 
      if (urlc.getRequestProps().containsKey("x-amz-server-side-encryption-aws-kms-key-id")) {
        signed_headers = String.valueOf(signed_headers) + ";x-amz-server-side-encryption;x-amz-server-side-encryption-aws-kms-key-id";
        canonical_headers = String.valueOf(canonical_headers) + "x-amz-server-side-encryption:aws:kms\nx-amz-server-side-encryption-aws-kms-key-id:" + urlc.getRequestProps().getProperty("x-amz-server-side-encryption-aws-kms-key-id") + "\n";
      } else if (urlc.getRequestProps().containsKey("x-amz-server-side-encryption")) {
        signed_headers = String.valueOf(signed_headers) + ";x-amz-server-side-encryption";
        canonical_headers = String.valueOf(canonical_headers) + "x-amz-server-side-encryption:" + urlc.getRequestProps().getProperty("x-amz-server-side-encryption") + "\n";
      } 
      String request_parameters = "";
      if (urlc.getURL().getPath().indexOf("?") >= 0)
        request_parameters = urlc.getURL().getPath().substring(urlc.getURL().getPath().indexOf("?") + 1); 
      String canonical_querystring = Common.url_encode(request_parameters, ".#@&?!\\=+~");
      if (urlc.getRemoveDoubleEncoding())
        canonical_querystring = URLConnection.remove_double_encoding_of_special_chars(canonical_querystring); 
      String payload_hash = "UNSIGNED-PAYLOAD";
      String canonical_request = String.valueOf(urlc.getRequestMethod()) + '\n' + canonical_uri + '\n' + canonical_querystring + '\n' + canonical_headers + '\n' + signed_headers + '\n' + payload_hash;
      Common.log("S3_CLIENT", 2, "canonical_request:----------------\n" + canonical_request + "\n----------------");
      String algorithm = "AWS4-HMAC-SHA256";
      String credential_scope = String.valueOf(datestamp) + '/' + this.region_name + '/' + "s3" + '/' + "aws4_request";
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.reset();
      md.update(canonical_request.getBytes("UTF8"));
      String string_to_sign = String.valueOf(algorithm) + '\n' + amzdate + '\n' + credential_scope + '\n' + bytesToHex(md.digest());
      Common.log("S3_CLIENT", 2, "string_to_sign:----------------\n" + string_to_sign + "\n----------------");
      byte[] signing_key = getSignatureKey(secret_key, datestamp, this.region_name, "s3");
      String signature = bytesToHex(HmacSHA256(string_to_sign, signing_key));
      String authorization_header = String.valueOf(algorithm) + " " + "Credential=" + this.config.getProperty("real_username", this.config.getProperty("username")) + "/" + credential_scope + ", " + "SignedHeaders=" + signed_headers + ", " + "Signature=" + signature;
      return authorization_header;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } 
  }
  
  static byte[] HmacSHA256(String data, byte[] key) throws Exception {
    String algorithm = "HmacSHA256";
    Mac mac = Mac.getInstance(algorithm);
    mac.init(new SecretKeySpec(key, algorithm));
    return mac.doFinal(data.getBytes("UTF8"));
  }
  
  static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
    byte[] kSecret = ("AWS4" + key).getBytes("UTF8");
    byte[] kDate = HmacSHA256(dateStamp, kSecret);
    byte[] kRegion = HmacSHA256(regionName, kDate);
    byte[] kService = HmacSHA256(serviceName, kRegion);
    byte[] kSigning = HmacSHA256("aws4_request", kService);
    return kSigning;
  }
  
  protected static final char[] hexArray = "0123456789abcdef".toCharArray();
  
  public static String bytesToHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0xF];
    } 
    return new String(hexChars);
  }
  
  public String getKeyText(Element el, String key) {
    List l = el.getChildren();
    for (int x = 0; x < l.size(); x++) {
      Element el2 = l.get(x);
      if (el2.getName().equals(key))
        return el2.getValue(); 
    } 
    return null;
  }
  
  public Element getElement(Element el, String key) {
    List l = el.getChildren();
    for (int x = 0; x < l.size(); x++) {
      Element el2 = l.get(x);
      if (el2.getName().equals(key))
        return el2; 
    } 
    return null;
  }
  
  public List getElements(Element el, String key) {
    List l2 = new ArrayList();
    List l = el.getChildren();
    for (int x = 0; x < l.size(); x++) {
      Element el2 = l.get(x);
      if (el2.getName().equals(key))
        l2.add(el2); 
    } 
    return l2;
  }
  
  public void updateIamAuth() throws Exception {
    if (this.config.getProperty("username").equalsIgnoreCase("iam_lookup")) {
      long expire = Long.parseLong(this.config.getProperty("iam_expire", "0"));
      if (System.currentTimeMillis() - expire > -3600000L) {
        Properties credentials = getCredentials(this.config.getProperty("password"));
        if (!credentials.getProperty("Code").equalsIgnoreCase("Success"))
          throw new Exception(credentials); 
        this.config.put("real_username", credentials.getProperty("AccessKeyId"));
        this.config.put("real_password", credentials.getProperty("SecretAccessKey"));
        this.config.put("real_token", credentials.getProperty("Token"));
        this.config.put("iam_expire", (new StringBuffer(String.valueOf(this.yyyyMMddtHHmmssSSSZ.parse(credentials.getProperty("Expiration")).getTime()))).toString());
        this.secretKey = new SecretKeySpec(this.config.getProperty("real_password", this.config.getProperty("password")).getBytes("UTF8"), "HmacSHA1");
      } 
    } 
  }
  
  public Properties getCredentials(String instance_profile_name) throws IOException {
    if (instance_profile_name.equals("lookup")) {
      URLConnection uRLConnection = URLConnection.openConnection(new VRL("http://169.254.169.254/latest/meta-data/iam/security-credentials/"), this.config);
      int i = uRLConnection.getResponseCode();
      String str = URLConnection.consumeResponse(uRLConnection.getInputStream());
      uRLConnection.disconnect();
      if (i < 200 || i > 299) {
        log(String.valueOf(str) + "\r\n");
        throw new IOException(str);
      } 
      instance_profile_name = str.trim();
      uRLConnection.disconnect();
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("http://169.254.169.254/latest/meta-data/iam/security-credentials/" + instance_profile_name), this.config);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      throw new IOException(result);
    } 
    Properties p = new Properties();
    Object obj = JSONValue.parse(result);
    if (obj instanceof JSONArray) {
      JSONArray ja = (JSONArray)obj;
      for (int xxx = 0; xxx < ja.size(); xxx++) {
        Object obj2 = ja.get(xxx);
        if (obj2 instanceof JSONObject) {
          JSONObject jo = (JSONObject)obj2;
          Object[] a = jo.entrySet().toArray();
          for (int i = 0; i < a.length; i++) {
            String key2 = a[i].toString().split("=")[0];
            p.put(key2, jo.get(key2));
          } 
        } 
      } 
    } else if (obj instanceof JSONObject) {
      JSONObject jo = (JSONObject)obj;
      Object[] a = jo.entrySet().toArray();
      for (int i = 0; i < a.length; i++) {
        String key2 = a[i].toString().split("=")[0];
        p.put(key2, jo.get(key2));
      } 
    } 
    return p;
  }
  
  private boolean is_folder_recently_created(String path) throws Exception {
    path = lower(path);
    if (get_cache_item("recently_created_folder_cache").containsKey(path)) {
      if (System.currentTimeMillis() - Long.parseLong(get_cache_item("recently_created_folder_cache").getProperty(path, "0")) < 30000L)
        return true; 
      get_cache_item("recently_created_folder_cache").remove(path);
    } 
    return false;
  }
  
  protected Properties get_cache_item(String cache_type) throws Exception {
    synchronized (s3_global_cache) {
      if (s3_global_cache.containsKey(this.cache_reference)) {
        Properties p = (Properties)s3_global_cache.get(this.cache_reference);
        return (Properties)p.get(cache_type);
      } 
    } 
    return new Properties();
  }
  
  protected String handle_path_special_chars(String path, boolean encode) {
    path = lower(path);
    String handled_path = path;
    if (handled_path.contains("%"))
      handled_path = handled_path.replace("%", encode ? "%25" : "%2525"); 
    if (handled_path.contains(" "))
      handled_path = handled_path.replace(" ", encode ? "%20" : "%2520"); 
    if (handled_path.contains("+"))
      handled_path = handled_path.replace("+", encode ? "%2B" : "%252B"); 
    if (handled_path.contains("&"))
      handled_path = handled_path.replace("&", encode ? "%26" : "%2526"); 
    if (handled_path.contains("$"))
      handled_path = handled_path.replace("$", encode ? "%24" : "%2524"); 
    if (handled_path.contains("@"))
      handled_path = handled_path.replace("@", encode ? "%40" : "%2540"); 
    if (handled_path.contains("="))
      handled_path = handled_path.replace("=", encode ? "%3D" : "%253D"); 
    if (handled_path.contains(":"))
      handled_path = handled_path.replace(":", encode ? "%3A" : "%253A"); 
    if (handled_path.contains(","))
      handled_path = handled_path.replace(",", encode ? "%2C" : "%252C"); 
    if (handled_path.contains("?"))
      handled_path = handled_path.replace("?", encode ? "%3F" : "%253F"); 
    if (handled_path.contains("~"))
      handled_path = handled_path.replace("~", encode ? "%7E" : "%257E"); 
    if (handled_path.contains("!"))
      handled_path = handled_path.replace("!", encode ? "%21" : "%2521"); 
    if (handled_path.contains("#"))
      handled_path = handled_path.replace("#", encode ? "%23" : "%2523"); 
    if (encode)
      handled_path = Common.url_encode(handled_path); 
    return handled_path;
  }
  
  protected void setRegionName() {
    String region = this.region_host;
    if (region.contains(":"))
      region = region.substring(0, region.indexOf(":")); 
    if (!region.equals("s3.amazonaws.com"))
      this.region_name = region.substring(3).substring(0, region.substring(3).indexOf(".")); 
  }
  
  public String lower(String s) {
    if (System.getProperty("crushftp.lowercase_all_s3_paths", "false").equals("true"))
      return s.toLowerCase(); 
    return s;
  }
}
