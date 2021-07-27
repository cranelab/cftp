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
  
  static long ram_used_download = 0L;
  
  static Object ram_lock = new Object();
  
  SimpleDateFormat yyyyMMddtHHmmssSSS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US);
  
  SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  
  SecretKeySpec secretKey = null;
  
  String region_host = "s3.amazonaws.com";
  
  private String region_name = "us-east-1";
  
  public static Properties valid_credentials_cache = new Properties();
  
  String cache_reference = null;
  
  public static Properties s3_global_cache = new Properties();
  
  public static void main(String[] args) throws IOException {
    System.setProperty("crushtunnel.debug", "2");
    GenericClient client = Common.getClient("s3://s3.amazonaws.com/", "", null);
    try {
      client.list("/crushftp/", new Vector());
    } catch (Exception e) {
      Common.log("S3_CLIENT", 1, e);
    } 
  }
  
  public S3Client(String url, String header, Vector log) {
    super(header, log);
    System.setProperty("crushtunnel.debug", System.getProperty("crushftp.debug", "2"));
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
    this.region_host = (new VRL(url)).getHost().toLowerCase();
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
    if (!valid_credentials_cache.containsKey(md5hash) || System.getProperty("crushftp.s3.always_auth", "false").equals("true")) {
      URLConnection urlc = doAction("GET", (new VRL(this.url)).getPath(), (StringBuffer)null, false, true);
      int code = urlc.getResponseCode();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        throw new IOException(result);
      } 
      valid_credentials_cache.put(md5hash, result);
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
    this.url = Common.getBaseUrl(this.url, false);
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
        String result = "";
        URLConnection urlc = null;
        for (int retries = 0; retries < 31; retries++) {
          urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + query), this.config);
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
            stat.put("url", String.valueOf(Common.url_decode(this.url)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1) + stat.getProperty("name"));
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
            Date d = new Date();
            String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   0   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + name;
            Properties stat = parseStat(line);
            stat.put("check_all_recursive_deletes", "true");
            stat.put("url", String.valueOf(Common.url_decode(this.url)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1) + stat.getProperty("name"));
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
              Date d = this.yyyyMMddtHHmmssSSS.parse(getKeyText(content, "LastModified"));
              String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + getKeyText(content, "Size") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + name;
              Properties stat = parseStat(line);
              stat.put("url", String.valueOf(Common.url_decode(this.url)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1) + stat.getProperty("name"));
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
      if (list2 == null && System.getProperty("crushftp.s3_partial", "true").equals("true") && this.config.getProperty("s3_partial", "true").equals("true")) {
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
    String result = "";
    String query = "";
    for (int retries = 0; retries < 31; ) {
      log("Looking for failed transfers, or in progress transfers.");
      query = "?uploads";
      if (System.getProperty("crushftp.s3_sha256", "false").equals("true") || !this.config.getProperty("server_side_encrypt_kms", "").equals(""))
        query = String.valueOf(query) + "="; 
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + query), this.config);
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
        URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + query), this.config);
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
    this.in = download4(path, startPos, endPos, binary, (String)null);
    return this.in;
  }
  
  protected InputStream download4(String path0, long startPos0, long endPos, boolean binary, String bucketName0) throws Exception {
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
          //   0: goto -> 1299
          //   3: getstatic com/crushftp/client/S3Client.ram_used_download : J
          //   6: ldc2_w 1024
          //   9: ldiv
          //   10: ldc2_w 1024
          //   13: ldiv
          //   14: aload_0
          //   15: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   18: getfield config : Ljava/util/Properties;
          //   21: ldc 's3_max_buffer_download'
          //   23: ldc '100'
          //   25: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   28: invokestatic parseLong : (Ljava/lang/String;)J
          //   31: lcmp
          //   32: ifle -> 53
          //   35: aload_0
          //   36: getfield val$status : Ljava/util/Properties;
          //   39: ldc 'error'
          //   41: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   44: ifne -> 53
          //   47: ldc2_w 1000
          //   50: invokestatic sleep : (J)V
          //   53: aload_0
          //   54: getfield val$status : Ljava/util/Properties;
          //   57: ldc 'ram'
          //   59: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   62: invokestatic parseLong : (Ljava/lang/String;)J
          //   65: ldc2_w 1024
          //   68: ldiv
          //   69: ldc2_w 1024
          //   72: ldiv
          //   73: aload_0
          //   74: getfield val$status : Ljava/util/Properties;
          //   77: ldc 's3_max_buffer_download'
          //   79: ldc '100'
          //   81: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   84: invokestatic parseLong : (Ljava/lang/String;)J
          //   87: ldc2_w 2
          //   90: ldiv
          //   91: lcmp
          //   92: ifle -> 117
          //   95: aload_0
          //   96: getfield val$status : Ljava/util/Properties;
          //   99: ldc 'error'
          //   101: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   104: ifne -> 117
          //   107: ldc2_w 1000
          //   110: invokestatic sleep : (J)V
          //   113: goto -> 117
          //   116: astore_1
          //   117: aload_0
          //   118: getfield val$status : Ljava/util/Properties;
          //   121: ldc 'error'
          //   123: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   126: ifeq -> 132
          //   129: goto -> 1321
          //   132: aconst_null
          //   133: astore_1
          //   134: iconst_0
          //   135: istore_2
          //   136: aload_0
          //   137: getfield val$chunks_needed : Ljava/util/Vector;
          //   140: dup
          //   141: astore_3
          //   142: monitorenter
          //   143: aload_0
          //   144: getfield val$status : Ljava/util/Properties;
          //   147: ldc 'first'
          //   149: ldc 'false'
          //   151: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   154: ldc 'true'
          //   156: invokevirtual equals : (Ljava/lang/Object;)Z
          //   159: istore_2
          //   160: aload_0
          //   161: getfield val$status : Ljava/util/Properties;
          //   164: ldc 'first'
          //   166: ldc 'false'
          //   168: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   171: pop
          //   172: aload_0
          //   173: getfield val$chunks_needed : Ljava/util/Vector;
          //   176: invokevirtual size : ()I
          //   179: ifle -> 194
          //   182: aload_0
          //   183: getfield val$chunks_needed : Ljava/util/Vector;
          //   186: iconst_0
          //   187: invokevirtual remove : (I)Ljava/lang/Object;
          //   190: invokevirtual toString : ()Ljava/lang/String;
          //   193: astore_1
          //   194: aload_3
          //   195: monitorexit
          //   196: goto -> 202
          //   199: aload_3
          //   200: monitorexit
          //   201: athrow
          //   202: aload_1
          //   203: ifnonnull -> 209
          //   206: goto -> 1321
          //   209: aload_0
          //   210: getfield val$bucketName0 : Ljava/lang/String;
          //   213: astore_3
          //   214: aload_0
          //   215: getfield val$stat : Ljava/util/Properties;
          //   218: ldc 'uid'
          //   220: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   223: ifeq -> 230
          //   226: aload_3
          //   227: ifnonnull -> 344
          //   230: aload_0
          //   231: getfield val$path0 : Ljava/lang/String;
          //   234: astore #4
          //   236: aload #4
          //   238: iconst_1
          //   239: invokevirtual substring : (I)Ljava/lang/String;
          //   242: astore_3
          //   243: aload_3
          //   244: ldc '?'
          //   246: invokevirtual indexOf : (Ljava/lang/String;)I
          //   249: iflt -> 264
          //   252: aload_3
          //   253: iconst_0
          //   254: aload_3
          //   255: ldc '?'
          //   257: invokevirtual indexOf : (Ljava/lang/String;)I
          //   260: invokevirtual substring : (II)Ljava/lang/String;
          //   263: astore_3
          //   264: aload_3
          //   265: ldc '/'
          //   267: invokevirtual indexOf : (Ljava/lang/String;)I
          //   270: iflt -> 300
          //   273: aload_3
          //   274: iconst_0
          //   275: aload_3
          //   276: ldc '/'
          //   278: invokevirtual indexOf : (Ljava/lang/String;)I
          //   281: invokevirtual substring : (II)Ljava/lang/String;
          //   284: astore_3
          //   285: aload #4
          //   287: aload #4
          //   289: ldc '/'
          //   291: iconst_1
          //   292: invokevirtual indexOf : (Ljava/lang/String;I)I
          //   295: invokevirtual substring : (I)Ljava/lang/String;
          //   298: astore #4
          //   300: aload #4
          //   302: new java/lang/StringBuffer
          //   305: dup
          //   306: ldc '/'
          //   308: invokespecial <init> : (Ljava/lang/String;)V
          //   311: aload_3
          //   312: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   315: invokevirtual toString : ()Ljava/lang/String;
          //   318: invokevirtual equals : (Ljava/lang/Object;)Z
          //   321: ifeq -> 328
          //   324: ldc '/'
          //   326: astore #4
          //   328: aload_0
          //   329: getfield val$stat : Ljava/util/Properties;
          //   332: ldc 'uid'
          //   334: aload #4
          //   336: iconst_1
          //   337: invokevirtual substring : (I)Ljava/lang/String;
          //   340: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   343: pop
          //   344: aload_0
          //   345: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   348: ldc 'GET'
          //   350: new java/lang/StringBuffer
          //   353: dup
          //   354: ldc '/'
          //   356: invokespecial <init> : (Ljava/lang/String;)V
          //   359: aload_3
          //   360: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   363: ldc '/'
          //   365: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   368: aload_0
          //   369: getfield val$stat : Ljava/util/Properties;
          //   372: ldc 'uid'
          //   374: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   377: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   380: aload_0
          //   381: getfield val$segmented : Z
          //   384: ifeq -> 413
          //   387: new java/lang/StringBuffer
          //   390: dup
          //   391: ldc '_'
          //   393: invokespecial <init> : (Ljava/lang/String;)V
          //   396: aload_1
          //   397: ldc ':'
          //   399: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   402: iconst_0
          //   403: aaload
          //   404: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   407: invokevirtual toString : ()Ljava/lang/String;
          //   410: goto -> 415
          //   413: ldc ''
          //   415: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   418: invokevirtual toString : ()Ljava/lang/String;
          //   421: aconst_null
          //   422: iconst_0
          //   423: iconst_0
          //   424: invokevirtual doAction : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/StringBuffer;ZZ)Lcom/crushftp/client/URLConnection;
          //   427: astore #4
          //   429: aload_0
          //   430: getfield val$startPos : J
          //   433: aload_1
          //   434: ldc ':'
          //   436: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   439: iconst_1
          //   440: aaload
          //   441: ldc '-'
          //   443: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   446: iconst_0
          //   447: aaload
          //   448: invokestatic parseLong : (Ljava/lang/String;)J
          //   451: lsub
          //   452: lstore #5
          //   454: aload_0
          //   455: getfield val$segmented : Z
          //   458: ifne -> 494
          //   461: aload #4
          //   463: ldc 'Range'
          //   465: new java/lang/StringBuffer
          //   468: dup
          //   469: ldc 'bytes='
          //   471: invokespecial <init> : (Ljava/lang/String;)V
          //   474: aload_1
          //   475: ldc ':'
          //   477: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   480: iconst_1
          //   481: aaload
          //   482: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   485: invokevirtual toString : ()Ljava/lang/String;
          //   488: invokevirtual setRequestProperty : (Ljava/lang/String;Ljava/lang/String;)V
          //   491: goto -> 615
          //   494: iload_2
          //   495: ifeq -> 615
          //   498: aload_0
          //   499: getfield val$segmented : Z
          //   502: ifeq -> 615
          //   505: lload #5
          //   507: lconst_0
          //   508: lcmp
          //   509: ifeq -> 615
          //   512: new java/lang/StringBuffer
          //   515: dup
          //   516: aload_1
          //   517: ldc ':'
          //   519: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   522: iconst_0
          //   523: aaload
          //   524: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   527: invokespecial <init> : (Ljava/lang/String;)V
          //   530: ldc ':'
          //   532: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   535: aload_1
          //   536: ldc ':'
          //   538: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   541: iconst_1
          //   542: aaload
          //   543: ldc '-'
          //   545: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   548: iconst_0
          //   549: aaload
          //   550: invokestatic parseLong : (Ljava/lang/String;)J
          //   553: lload #5
          //   555: ladd
          //   556: invokevirtual append : (J)Ljava/lang/StringBuffer;
          //   559: ldc '-'
          //   561: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   564: aload_1
          //   565: ldc ':'
          //   567: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   570: iconst_1
          //   571: aaload
          //   572: ldc '-'
          //   574: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   577: iconst_1
          //   578: aaload
          //   579: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   582: invokevirtual toString : ()Ljava/lang/String;
          //   585: astore_1
          //   586: aload #4
          //   588: ldc 'Range'
          //   590: new java/lang/StringBuffer
          //   593: dup
          //   594: ldc 'bytes='
          //   596: invokespecial <init> : (Ljava/lang/String;)V
          //   599: lload #5
          //   601: invokevirtual append : (J)Ljava/lang/StringBuffer;
          //   604: ldc '-'
          //   606: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   609: invokevirtual toString : ()Ljava/lang/String;
          //   612: invokevirtual setRequestProperty : (Ljava/lang/String;Ljava/lang/String;)V
          //   615: invokestatic currentTimeMillis : ()J
          //   618: lstore #7
          //   620: aload_1
          //   621: ldc ':-1-'
          //   623: invokevirtual indexOf : (Ljava/lang/String;)I
          //   626: iflt -> 647
          //   629: aload_1
          //   630: ldc ':-1-'
          //   632: ldc ':0-'
          //   634: invokestatic replace_str : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   637: astore_1
          //   638: goto -> 647
          //   641: ldc2_w 100
          //   644: invokestatic sleep : (J)V
          //   647: aload_0
          //   648: getfield val$status : Ljava/util/Properties;
          //   651: ldc 'error'
          //   653: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   656: ifne -> 925
          //   659: iload_2
          //   660: ifne -> 925
          //   663: invokestatic currentTimeMillis : ()J
          //   666: lload #7
          //   668: lsub
          //   669: ldc2_w 60000
          //   672: lcmp
          //   673: ifge -> 925
          //   676: aload_1
          //   677: ldc ':'
          //   679: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   682: iconst_1
          //   683: aaload
          //   684: ldc '-'
          //   686: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   689: iconst_0
          //   690: aaload
          //   691: invokestatic parseLong : (Ljava/lang/String;)J
          //   694: aload_0
          //   695: getfield val$status : Ljava/util/Properties;
          //   698: ldc 'current_pos'
          //   700: ldc '0'
          //   702: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   705: invokestatic parseLong : (Ljava/lang/String;)J
          //   708: lsub
          //   709: aload_0
          //   710: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   713: getfield config : Ljava/util/Properties;
          //   716: ldc 's3_buffer'
          //   718: ldc '5'
          //   720: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   723: invokestatic parseLong : (Ljava/lang/String;)J
          //   726: ldc2_w 1024
          //   729: lmul
          //   730: ldc2_w 1024
          //   733: lmul
          //   734: ldc2_w 2
          //   737: lmul
          //   738: aload_0
          //   739: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   742: getfield config : Ljava/util/Properties;
          //   745: ldc 's3_threads_download'
          //   747: ldc '3'
          //   749: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   752: invokestatic parseInt : (Ljava/lang/String;)I
          //   755: i2l
          //   756: lmul
          //   757: lcmp
          //   758: ifgt -> 641
          //   761: goto -> 925
          //   764: astore #9
          //   766: aload_0
          //   767: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   770: new java/lang/StringBuffer
          //   773: dup
          //   774: ldc 'Wait:'
          //   776: invokespecial <init> : (Ljava/lang/String;)V
          //   779: invokestatic currentTimeMillis : ()J
          //   782: lload #7
          //   784: lsub
          //   785: invokevirtual append : (J)Ljava/lang/StringBuffer;
          //   788: invokevirtual toString : ()Ljava/lang/String;
          //   791: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   794: pop
          //   795: aload_0
          //   796: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   799: new java/lang/StringBuffer
          //   802: dup
          //   803: ldc 'Part:'
          //   805: invokespecial <init> : (Ljava/lang/String;)V
          //   808: aload_1
          //   809: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   812: invokevirtual toString : ()Ljava/lang/String;
          //   815: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   818: pop
          //   819: aload_0
          //   820: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   823: new java/lang/StringBuffer
          //   826: dup
          //   827: ldc 'Buffer:'
          //   829: invokespecial <init> : (Ljava/lang/String;)V
          //   832: aload_0
          //   833: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   836: getfield config : Ljava/util/Properties;
          //   839: ldc 's3_buffer'
          //   841: ldc '5'
          //   843: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   846: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   849: invokevirtual toString : ()Ljava/lang/String;
          //   852: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   855: pop
          //   856: aload_0
          //   857: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   860: new java/lang/StringBuffer
          //   863: dup
          //   864: ldc 'Threads:'
          //   866: invokespecial <init> : (Ljava/lang/String;)V
          //   869: aload_0
          //   870: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   873: getfield config : Ljava/util/Properties;
          //   876: ldc 's3_threads_download'
          //   878: ldc '3'
          //   880: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   883: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   886: invokevirtual toString : ()Ljava/lang/String;
          //   889: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   892: pop
          //   893: aload_0
          //   894: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   897: new java/lang/StringBuffer
          //   900: dup
          //   901: ldc 'Status:'
          //   903: invokespecial <init> : (Ljava/lang/String;)V
          //   906: aload_0
          //   907: getfield val$status : Ljava/util/Properties;
          //   910: invokevirtual append : (Ljava/lang/Object;)Ljava/lang/StringBuffer;
          //   913: invokevirtual toString : ()Ljava/lang/String;
          //   916: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   919: pop
          //   920: aload #9
          //   922: athrow
          //   923: astore #7
          //   925: aload_0
          //   926: getfield val$status : Ljava/util/Properties;
          //   929: ldc 'error'
          //   931: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   934: ifeq -> 940
          //   937: goto -> 1321
          //   940: aload #4
          //   942: invokevirtual getResponseCode : ()I
          //   945: istore #7
          //   947: iload #7
          //   949: sipush #200
          //   952: if_icmplt -> 963
          //   955: iload #7
          //   957: sipush #299
          //   960: if_icmple -> 1017
          //   963: aload #4
          //   965: invokevirtual getInputStream : ()Ljava/io/InputStream;
          //   968: invokestatic consumeResponse : (Ljava/io/InputStream;)Ljava/lang/String;
          //   971: astore #8
          //   973: aload #4
          //   975: invokevirtual disconnect : ()V
          //   978: aload_0
          //   979: getfield this$0 : Lcom/crushftp/client/S3Client;
          //   982: new java/lang/StringBuffer
          //   985: dup
          //   986: aload #8
          //   988: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   991: invokespecial <init> : (Ljava/lang/String;)V
          //   994: ldc_w '\\r\\n'
          //   997: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1000: invokevirtual toString : ()Ljava/lang/String;
          //   1003: invokevirtual log : (Ljava/lang/String;)Ljava/lang/String;
          //   1006: pop
          //   1007: new java/io/IOException
          //   1010: dup
          //   1011: aload #8
          //   1013: invokespecial <init> : (Ljava/lang/String;)V
          //   1016: athrow
          //   1017: new java/io/ByteArrayOutputStream
          //   1020: dup
          //   1021: invokespecial <init> : ()V
          //   1024: astore #8
          //   1026: aload #4
          //   1028: invokevirtual getInputStream : ()Ljava/io/InputStream;
          //   1031: aload #8
          //   1033: iconst_0
          //   1034: iconst_1
          //   1035: iconst_1
          //   1036: invokestatic streamCopier : (Ljava/io/InputStream;Ljava/io/OutputStream;ZZZ)V
          //   1039: aload #4
          //   1041: invokevirtual disconnect : ()V
          //   1044: aload_0
          //   1045: getfield val$status : Ljava/util/Properties;
          //   1048: ldc 'error'
          //   1050: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   1053: ifne -> 1276
          //   1056: getstatic com/crushftp/client/S3Client.ram_lock : Ljava/lang/Object;
          //   1059: dup
          //   1060: astore #9
          //   1062: monitorenter
          //   1063: aload #8
          //   1065: invokevirtual toByteArray : ()[B
          //   1068: astore #10
          //   1070: getstatic com/crushftp/client/S3Client.ram_used_download : J
          //   1073: aload #10
          //   1075: arraylength
          //   1076: i2l
          //   1077: ladd
          //   1078: putstatic com/crushftp/client/S3Client.ram_used_download : J
          //   1081: aload_0
          //   1082: getfield val$chunks : Ljava/util/Properties;
          //   1085: aload_1
          //   1086: ldc ':'
          //   1088: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   1091: iconst_1
          //   1092: aaload
          //   1093: ldc '-'
          //   1095: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   1098: iconst_0
          //   1099: aaload
          //   1100: aload #10
          //   1102: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1105: pop
          //   1106: aload_0
          //   1107: getfield val$status : Ljava/util/Properties;
          //   1110: ldc 'ram'
          //   1112: new java/lang/StringBuffer
          //   1115: dup
          //   1116: aload_0
          //   1117: getfield val$status : Ljava/util/Properties;
          //   1120: ldc 'ram'
          //   1122: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   1125: invokestatic parseLong : (Ljava/lang/String;)J
          //   1128: aload #10
          //   1130: arraylength
          //   1131: i2l
          //   1132: ladd
          //   1133: invokestatic valueOf : (J)Ljava/lang/String;
          //   1136: invokespecial <init> : (Ljava/lang/String;)V
          //   1139: invokevirtual toString : ()Ljava/lang/String;
          //   1142: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1145: pop
          //   1146: invokestatic currentThread : ()Ljava/lang/Thread;
          //   1149: invokevirtual getName : ()Ljava/lang/String;
          //   1152: astore #11
          //   1154: new java/lang/StringBuffer
          //   1157: dup
          //   1158: aload #11
          //   1160: iconst_0
          //   1161: aload #11
          //   1163: ldc ':'
          //   1165: invokevirtual lastIndexOf : (Ljava/lang/String;)I
          //   1168: iconst_1
          //   1169: iadd
          //   1170: invokevirtual substring : (II)Ljava/lang/String;
          //   1173: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   1176: invokespecial <init> : (Ljava/lang/String;)V
          //   1179: aload_0
          //   1180: getfield val$status : Ljava/util/Properties;
          //   1183: ldc 'ram'
          //   1185: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   1188: invokestatic parseLong : (Ljava/lang/String;)J
          //   1191: invokestatic format_bytes_short : (J)Ljava/lang/String;
          //   1194: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1197: ldc_w ' of '
          //   1200: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1203: getstatic com/crushftp/client/S3Client.ram_used_download : J
          //   1206: invokestatic format_bytes_short : (J)Ljava/lang/String;
          //   1209: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1212: invokevirtual toString : ()Ljava/lang/String;
          //   1215: astore #11
          //   1217: invokestatic currentThread : ()Ljava/lang/Thread;
          //   1220: aload #11
          //   1222: invokevirtual setName : (Ljava/lang/String;)V
          //   1225: aload #9
          //   1227: monitorexit
          //   1228: goto -> 1276
          //   1231: aload #9
          //   1233: monitorexit
          //   1234: athrow
          //   1235: astore #7
          //   1237: aload_0
          //   1238: getfield val$status : Ljava/util/Properties;
          //   1241: ldc 'error'
          //   1243: aload #7
          //   1245: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1248: pop
          //   1249: goto -> 1276
          //   1252: astore #7
          //   1254: aload_0
          //   1255: getfield val$status : Ljava/util/Properties;
          //   1258: ldc 'error'
          //   1260: aload #7
          //   1262: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1265: pop
          //   1266: ldc_w 'S3_CLIENT'
          //   1269: iconst_2
          //   1270: aload #7
          //   1272: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
          //   1275: pop
          //   1276: aload_0
          //   1277: getfield val$status : Ljava/util/Properties;
          //   1280: ldc_w 'run_once'
          //   1283: ldc 'false'
          //   1285: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   1288: ldc 'true'
          //   1290: invokevirtual equals : (Ljava/lang/Object;)Z
          //   1293: ifeq -> 1299
          //   1296: goto -> 1321
          //   1299: aload_0
          //   1300: getfield val$chunks_needed : Ljava/util/Vector;
          //   1303: invokevirtual size : ()I
          //   1306: ifle -> 1321
          //   1309: aload_0
          //   1310: getfield val$status : Ljava/util/Properties;
          //   1313: ldc 'error'
          //   1315: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   1318: ifeq -> 3
          //   1321: return
          // Line number table:
          //   Java source line number -> byte code offset
          //   #569	-> 0
          //   #573	-> 3
          //   #574	-> 53
          //   #576	-> 116
          //   #579	-> 117
          //   #580	-> 132
          //   #581	-> 134
          //   #582	-> 136
          //   #584	-> 143
          //   #585	-> 160
          //   #586	-> 172
          //   #582	-> 194
          //   #588	-> 202
          //   #589	-> 209
          //   #590	-> 214
          //   #592	-> 230
          //   #593	-> 236
          //   #594	-> 243
          //   #595	-> 264
          //   #597	-> 273
          //   #598	-> 285
          //   #600	-> 300
          //   #601	-> 328
          //   #603	-> 344
          //   #604	-> 429
          //   #605	-> 454
          //   #606	-> 494
          //   #608	-> 512
          //   #609	-> 586
          //   #615	-> 615
          //   #617	-> 620
          //   #620	-> 638
          //   #621	-> 641
          //   #620	-> 647
          //   #623	-> 764
          //   #625	-> 766
          //   #626	-> 795
          //   #627	-> 819
          //   #628	-> 856
          //   #629	-> 893
          //   #630	-> 920
          //   #633	-> 923
          //   #636	-> 925
          //   #637	-> 940
          //   #638	-> 947
          //   #640	-> 963
          //   #641	-> 973
          //   #642	-> 978
          //   #643	-> 1007
          //   #645	-> 1017
          //   #646	-> 1026
          //   #647	-> 1039
          //   #648	-> 1044
          //   #650	-> 1056
          //   #652	-> 1063
          //   #653	-> 1070
          //   #654	-> 1081
          //   #655	-> 1106
          //   #656	-> 1146
          //   #657	-> 1154
          //   #658	-> 1217
          //   #650	-> 1225
          //   #662	-> 1235
          //   #664	-> 1237
          //   #666	-> 1252
          //   #668	-> 1254
          //   #669	-> 1266
          //   #671	-> 1276
          //   #569	-> 1299
          //   #673	-> 1321
          // Local variable table:
          //   start	length	slot	name	descriptor
          //   0	1322	0	this	Lcom/crushftp/client/S3Client$1;
          //   134	1165	1	part	Ljava/lang/String;
          //   136	1163	2	first	Z
          //   214	1085	3	bucketName	Ljava/lang/String;
          //   236	108	4	path	Ljava/lang/String;
          //   429	870	4	urlc	Lcom/crushftp/client/URLConnection;
          //   454	845	5	offset	J
          //   620	303	7	start_wait	J
          //   766	157	9	e	Ljava/lang/NumberFormatException;
          //   947	288	7	code	I
          //   973	44	8	result	Ljava/lang/String;
          //   1026	209	8	baos	Ljava/io/ByteArrayOutputStream;
          //   1070	155	10	b	[B
          //   1154	71	11	s	Ljava/lang/String;
          //   1237	12	7	e	Ljava/lang/InterruptedException;
          //   1254	22	7	e	Ljava/io/IOException;
          // Exception table:
          //   from	to	target	type
          //   3	113	116	java/lang/InterruptedException
          //   143	196	199	finally
          //   199	201	199	finally
          //   615	923	923	java/lang/InterruptedException
          //   615	937	1235	java/lang/InterruptedException
          //   615	937	1252	java/io/IOException
          //   638	761	764	java/lang/NumberFormatException
          //   940	1235	1235	java/lang/InterruptedException
          //   940	1235	1252	java/io/IOException
          //   1063	1228	1231	finally
          //   1231	1234	1231	finally
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
              String s = Thread.currentThread().getName();
              s = String.valueOf(s.substring(0, s.lastIndexOf(":") + 1)) + "Waiting for " + current_pos + " and using " + Common.format_bytes_short(Long.parseLong(this.val$status.getProperty("ram"))) + " of " + Common.format_bytes_short(S3Client.ram_used_download);
              Thread.currentThread().setName(s);
              if (this.val$chunks.containsKey((new StringBuffer(String.valueOf(current_pos))).toString())) {
                byte[] b = (byte[])null;
                synchronized (S3Client.ram_lock) {
                  b = (byte[])this.val$chunks.remove((new StringBuffer(String.valueOf(current_pos))).toString());
                  S3Client.ram_used_download -= b.length;
                  this.val$status.put("ram", (new StringBuffer(String.valueOf(Long.parseLong(this.val$status.getProperty("ram")) - b.length))).toString());
                  s = Thread.currentThread().getName();
                  s = String.valueOf(s.substring(0, s.lastIndexOf(":") + 1)) + Common.format_bytes_short(Long.parseLong(this.val$status.getProperty("ram"))) + " of " + Common.format_bytes_short(S3Client.ram_used_download);
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
                String s = Thread.currentThread().getName();
                s = String.valueOf(s.substring(0, s.lastIndexOf(":") + 1)) + Common.format_bytes_short(Long.parseLong(this.val$status.getProperty("ram"))) + " of " + Common.format_bytes_short(S3Client.ram_used_download);
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
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
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
        tempPath = tempPath.substring(tempPath.indexOf("/", 1));
      } 
      String result = "";
      for (int retries = 0; retries < 31; ) {
        String query = "?uploads";
        if (System.getProperty("crushftp.s3_sha256", "false").equals("true") || !this.config.getProperty("server_side_encrypt_kms", "").equals(""))
          query = String.valueOf(query) + "="; 
        URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + query), this.config);
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
    this.out = new null.OutputWrapper(this, maxBufferSize, partNumber, resumeParts, resumePartsDone, uploadId, path);
    return this.out;
  }
  
  public void flushNow(Vector resumePartsDone, ByteArrayOutputStream buf2, Vector resumeParts, String uploadId, String path, int partNumber, int partNumberPos, boolean ignoreZero) throws IOException {
    if (buf2.size() == 0 && !ignoreZero)
      return; 
    String bucketName = path.substring(1);
    String tempPath = path;
    if (bucketName.indexOf("/") >= 0) {
      bucketName = bucketName.substring(0, bucketName.indexOf("/"));
      tempPath = tempPath.substring(tempPath.indexOf("/", 1));
    } 
    Properties chunk_part = resumeParts.elementAt(partNumberPos - 1);
    chunk_part.put("start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    for (int loops = 0; loops < 20; loops++) {
      URLConnection urlc = null;
      try {
        if (uploadId == null) {
          urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + "_" + partNumber), this.config);
        } else {
          urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + "?partNumber=" + partNumber + "&uploadId=" + uploadId), this.config);
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
      tempPath = tempPath.substring(tempPath.indexOf("/", 1));
    } 
    long total_bytes = startPos;
    long pos = 0L;
    while (total_bytes > 0L) {
      long chunk_size = total_bytes;
      if (chunk_size > 1073741824L)
        chunk_size = 1073741824L; 
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + "?partNumber=" + partNumber + "&uploadId=" + uploadId), this.config);
      urlc.setRemoveDoubleEncoding(true);
      urlc.setRequestMethod("PUT");
      urlc.setRequestProperty("x-amz-copy-source", old_path);
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
      tempPath = tempPath.substring(tempPath.indexOf("/", 1));
    } 
    int code = -1;
    String result = "";
    int loops = 0;
    while (code == -1 && loops++ < 60) {
      try {
        URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(tempPath, false) + "?uploadId=" + uploadId), this.config);
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
    int code = 0;
    String result = "";
    int loops = 1;
    while (code != 404 && loops++ < 100) {
      Properties stat = stat(path);
      if (stat == null)
        stat = new Properties(); 
      boolean is_folder = stat.getProperty("type", "FILE").equalsIgnoreCase("DIR");
      URLConnection urlc = doAction("DELETE", String.valueOf(path) + (stat.getProperty("type", "FILE").equalsIgnoreCase("DIR") ? "/" : "") + (!stat.getProperty("uploadId", "").equals("") ? ("?uploadId=" + stat.getProperty("uploadId", "")) : ""), (StringBuffer)null, false, true);
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
      if (stat.size() == 0)
        break; 
      if (is_folder && stat(path) != null)
        return false; 
    } 
    return (loops < 99);
  }
  
  private void updateCache(Properties stat, String path, String action) throws Exception {
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
    if (is_folder_recently_created(path))
      return true; 
    URLConnection urlc = doAction("PUT", path, (StringBuffer)null, true, true);
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
    get_cache_item("stat_cache").remove(rnfr);
    get_cache_item("stat_cache").remove(rnto);
    Properties stat = stat(rnfr);
    if (stat.getProperty("type").equalsIgnoreCase("DIR"))
      return false; 
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("PUT", rnto, bucketNameSB, true, true);
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
    if (path.endsWith(":filetree"))
      path = path.substring(0, path.indexOf(":filetree") - 1); 
    if (this.config.getProperty("s3_stat_head_calls", "true").equals("true"))
      return stat_head_calls(path); 
    Vector v = new Vector();
    list(Common.all_but_last(path), v);
    return stat_list(path, v);
  }
  
  private Properties stat_head_calls(String path) throws Exception {
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
          Date d = new Date();
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
        if (info == null && System.getProperty("crushftp.s3_partial", "true").equals("true") && this.config.getProperty("s3_partial", "true").equals("true")) {
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
            System.out.println("0");
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
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("PUT", path, bucketNameSB, false, true);
    urlc.setLength(0L);
    urlc.setRequestProperty("x-amz-copy-source", path);
    urlc.setRequestProperty("x-amz-meta-modified", (new StringBuffer(String.valueOf(modified))).toString());
    if (!this.config.getProperty("uploaded_by", "").equals(""))
      urlc.setRequestProperty("x-amz-meta-uploaded-by", this.config.getProperty("uploaded_by", "")); 
    if (!this.config.getProperty("uploaded_md5", "").equals(""))
      urlc.setRequestProperty("x-amz-meta-md5", this.config.getProperty("uploaded_md5", "")); 
    Properties p = getS3ObjectInfo(path);
    if (p != null) {
      if (this.config.getProperty("uploaded_by", "").equals("") && p.containsKey("X-AMZ-META-UPLOADED-BY"))
        urlc.setRequestProperty("x-amz-meta-uploaded-by", p.getProperty("X-AMZ-META-UPLOADED-BY", "")); 
      if (this.config.getProperty("uploaded_md5", "").equals("") && p.containsKey("X-AMZ-META-MD5"))
        urlc.setRequestProperty("x-amz-meta-md5", p.getProperty("X-AMZ-META-MD5", "")); 
      if (p.containsKey("X-AMZ-SERVER-SIDE-ENCRYPTION"))
        urlc.setRequestProperty("x-amz-server-side-encryption", (String)p.get("X-AMZ-SERVER-SIDE-ENCRYPTION")); 
    } 
    urlc.setRequestProperty("x-amz-metadata-directive", "REPLACE");
    doStandardAmazonAlterations(urlc, Common.mimes.getProperty(getExt(path)), bucketNameSB.toString());
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
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
    Properties header_properties = new Properties();
    Properties s3ObjectInfo = getS3ObjectInfo(path);
    Enumeration keys = s3ObjectInfo.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (key.startsWith("X-AMZ-META-"))
        header_properties.put(key.toLowerCase(), s3ObjectInfo.get(key)); 
    } 
    return header_properties;
  }
  
  private Properties getS3ObjectInfo(String path) throws IOException, SocketTimeoutException {
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("HEAD", path, bucketNameSB, false, true);
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
  
  public String getUploadedByMetadata(String path) throws IOException, SocketTimeoutException {
    Properties p = getMetadata(path);
    if (p != null && p.containsKey("x-amz-meta-uploaded-by") && !p.getProperty("x-amz-meta-uploaded-by").equals(""))
      return p.getProperty("x-amz-meta-uploaded-by"); 
    return "";
  }
  
  public void set_MD5_and_upload_id(String path) throws Exception {
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("PUT", path, bucketNameSB, false, true);
    urlc.setLength(0L);
    urlc.setRequestProperty("x-amz-copy-source", path);
    if (!this.config.getProperty("uploaded_by", "").equals(""))
      urlc.setRequestProperty("x-amz-meta-uploaded-by", this.config.getProperty("uploaded_by", "")); 
    if (!this.config.getProperty("uploaded_md5", "").equals(""))
      urlc.setRequestProperty("x-amz-meta-md5", this.config.getProperty("uploaded_md5", "")); 
    Properties p = getS3ObjectInfo(path);
    if (p != null) {
      if (this.config.getProperty("uploaded_by", "").equals("") && p.containsKey("X-AMZ-META-UPLOADED-BY"))
        urlc.setRequestProperty("x-amz-meta-uploaded-by", p.getProperty("X-AMZ-META-UPLOADED-BY", "")); 
      if (this.config.getProperty("uploaded_md5", "").equals("") && p.containsKey("X-AMZ-META-MD5"))
        urlc.setRequestProperty("x-amz-meta-md5", p.getProperty("X-AMZ-META-MD5", "")); 
      if (p.containsKey("X-AMZ-META-MODIFIED"))
        urlc.setRequestProperty("x-amz-meta-modified", p.getProperty("X-AMZ-META-MODIFIED", "")); 
      if (p.containsKey("X-AMZ-SERVER-SIDE-ENCRYPTION"))
        urlc.setRequestProperty("x-amz-server-side-encryption", (String)p.get("X-AMZ-SERVER-SIDE-ENCRYPTION")); 
    } 
    urlc.setRequestProperty("x-amz-metadata-directive", "REPLACE");
    doStandardAmazonAlterations(urlc, Common.mimes.getProperty(getExt(path)), bucketNameSB.toString());
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299)
      log(String.valueOf(result) + "\r\n"); 
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
    if (!bucketName.equals(""))
      urlc.setRequestProperty("Host", String.valueOf(bucketName) + "." + this.region_host); 
    urlc.setRequestProperty("Cache", null);
    urlc.setRequestProperty("Cache-Control", null);
    if (this.config.containsKey("real_token"))
      urlc.setRequestProperty("x-amz-security-token", this.config.getProperty("real_token")); 
    if (System.getProperty("crushftp.s3_sha256", "false").equals("false") && this.config.getProperty("server_side_encrypt_kms", "").equals("")) {
      urlc.setRequestProperty("Authorization", "AWS " + this.config.getProperty("real_username", this.config.getProperty("username")) + ":" + calculateAmazonSignature(urlc));
    } else {
      try {
        urlc.setRequestProperty("Authorization", calculateAmazonSignaturev4(urlc));
        urlc.setRequestProperty("x-amz-content-sha256", "UNSIGNED-PAYLOAD");
        urlc.setRequestProperty("x-amz-date", urlc.sdf_rfc1123.format(urlc.getDate()));
      } catch (Exception e) {
        e.printStackTrace();
      } 
    } 
    urlc.setUseCaches(false);
  }
  
  public URLConnection doAction(String verb, String path, StringBuffer bucketNameSB, boolean do_secure, boolean handle_special_chars) {
    String bucketName = path.substring(1);
    if (bucketName.indexOf("?") >= 0)
      bucketName = bucketName.substring(0, bucketName.indexOf("?")); 
    if (bucketName.indexOf("/") >= 0) {
      bucketName = bucketName.substring(0, bucketName.indexOf("/"));
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
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + handle_path_special_chars(path, false)), this.config);
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
    if (bucketName.equalsIgnoreCase(this.region_host)) {
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
    } else if (urlc.getURL().getPath().indexOf("?delimiter") >= 0) {
      tmp_path = "/" + bucketName + urlc.getURL().getPath().substring(0, urlc.getURL().getPath().indexOf("?"));
    } else {
      tmp_path = "/" + bucketName + urlc.getURL().getPath();
    } 
    tmp_path = Common.url_encode(tmp_path, "/.#@&?!\\=+");
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
      SimpleDateFormat yyymmdd = new SimpleDateFormat("yyyyMMdd", Locale.US);
      String amzdate = urlc.sdf_rfc1123.format(urlc.getDate());
      String datestamp = yyymmdd.format(urlc.getDate());
      String bucketName = urlc.getRequestProps().getProperty("HOST", urlc.getURL().getHost());
      if (bucketName.equalsIgnoreCase(this.region_host)) {
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
      String canonical_uri = Common.url_encode(tmp_path, "/.#@&?!\\=+");
      if (urlc.getRemoveDoubleEncoding())
        canonical_uri = URLConnection.remove_double_encoding_of_special_chars(canonical_uri); 
      String secret_key = this.config.getProperty("real_password", this.config.getProperty("password"));
      String canonical_headers = "host:" + urlc.getURL().getHost() + "\n" + "x-amz-content-sha256:UNSIGNED-PAYLOAD" + "\n";
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
      String canonical_querystring = Common.url_encode(request_parameters, ".#@&?!\\=+");
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
        SimpleDateFormat yyyyMMddtHHmmssSSSZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        this.config.put("iam_expire", (new StringBuffer(String.valueOf(yyyyMMddtHHmmssSSSZ.parse(credentials.getProperty("Expiration")).getTime()))).toString());
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
    String handled_path = path;
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
    if (encode)
      handled_path = Common.url_encode(handled_path); 
    return handled_path;
  }
}
