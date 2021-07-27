package com.crushftp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import javax.crypto.spec.SecretKeySpec;
import org.jdom.Element;

public class S3CrushClient extends S3Client {
  String bucketName0 = null;
  
  String s3_root = "./s3/";
  
  boolean uploading = false;
  
  Vector replicating = null;
  
  public S3CrushClient(String url, String header, Vector log) {
    super(url, header, log);
    System.setProperty("crushtunnel.debug", System.getProperty("crushftp.debug", "2"));
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
    this.s3_root = System.getProperty("crushftp.s3_root", "./s3/");
    this.replicating = (Vector)System.getProperties().get("crushftp.s3_replicated");
  }
  
  public String getRawXmlPath(String path0) throws Exception {
    path0 = getPath(path0);
    return String.valueOf(this.s3_root) + this.bucketName0 + path0;
  }
  
  private String getPath(String path0) throws Exception {
    if (this.bucketName0 == null) {
      this.bucketName0 = path0.substring(1, path0.indexOf("/", 1)).toLowerCase();
      (new File_S(String.valueOf(this.s3_root) + this.bucketName0)).mkdirs();
    } 
    if (this.secretKey == null)
      login((new VRL(this.url)).getUsername(), (new VRL(this.url)).getPassword(), ""); 
    return Common.dots(path0.substring(path0.indexOf("/", 1)));
  }
  
  public static void writeFs(String s3_root, String bucketName0, Vector replicating, String path0, Properties p) throws Exception {
    String path = Common.dots(path0);
    (new File_S(String.valueOf(s3_root) + bucketName0 + Common.all_but_last(path))).mkdirs();
    Common.writeXMLObject(String.valueOf(s3_root) + bucketName0 + path, p, "s3");
    if (replicating != null) {
      Properties p2 = new Properties();
      p2.put("bucketName0", bucketName0);
      p2.put("path", path0.replace(" ", "+"));
      p2.put("data", Common.CLONE(p));
      replicating.addElement(p2);
    } 
  }
  
  public void resetBucket() {
    this.bucketName0 = null;
  }
  
  public String login(String username, String password, String clientid) throws Exception {
    if (clientid == null)
      clientid = ""; 
    this.config.put("username", username.trim());
    this.config.put("password", password.trim());
    this.secretKey = new SecretKeySpec(password.trim().getBytes("UTF8"), "HmacSHA1");
    if (!clientid.equals("") && !clientid.endsWith("/"))
      clientid = String.valueOf(clientid) + "/"; 
    if (!clientid.equals("") && clientid.startsWith("/"))
      clientid = clientid.substring(1); 
    if (!clientid.equals(""))
      clientid = clientid.substring(0, clientid.indexOf("/") + 1); 
    String md5hash = Common.getMD5(new ByteArrayInputStream((String.valueOf(username) + password + clientid).getBytes()));
    if (!valid_credentials_cache.containsKey(md5hash) || System.getProperty("crushftp.s3.always_auth", "false").equals("true")) {
      URLConnection urlc = doAction("GET", (new VRL(String.valueOf(this.url) + clientid)).getPath(), (StringBuffer)null, false, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
      String result = "";
      if (this.config.getProperty("ignore_login_errors", "false").equals("false")) {
        int code = urlc.getResponseCode();
        result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code < 200 || code > 299) {
          log(String.valueOf(result) + "\r\n");
          throw new IOException(result);
        } 
      } 
      valid_credentials_cache.put(md5hash, result);
    } 
    this.config.remove("ignore_login_errors");
    return valid_credentials_cache.getProperty(md5hash);
  }
  
  public void close() throws Exception {
    if (this.in != null) {
      this.in.close();
      this.in = null;
    } 
    if (this.out != null) {
      for (int x = 0; x < 60 && this.uploading; x++)
        Thread.sleep(1000L); 
      this.out.close();
      this.out = null;
    } 
  }
  
  public Vector list(String path0, Vector list) throws Exception {
    path0 = getPath(path0);
    if (!(new File_S(String.valueOf(this.s3_root) + this.bucketName0 + path0)).exists())
      throw new Exception("No such folder: \"" + this.s3_root + this.bucketName0 + path0 + "\""); 
    File_S[] f = (File_S[])(new File_S(String.valueOf(this.s3_root) + this.bucketName0 + path0)).listFiles();
    for (int x = 0; f != null && x < f.length; x++) {
      if (!f[x].getName().equals(".DS_Store")) {
        Date d = new Date(f[x].lastModified());
        Properties p = null;
        if (f[x].isFile()) {
          p = (Properties)Common.readXMLObject(f[x]);
        } else {
          p = new Properties();
        } 
        String line = String.valueOf(f[x].isDirectory() ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + p.getProperty("size", "0") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + f[x].getName();
        Properties stat = parseStat(line);
        stat.put("url", String.valueOf(this.url) + this.bucketName0 + path0 + stat.getProperty("name") + (f[x].isDirectory() ? "/" : ""));
        list.addElement(stat);
      } 
    } 
    return list;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    this.in = download4(path, startPos, endPos, binary, this.bucketName0);
    return this.in;
  }
  
  protected OutputStream upload3(String path0, long startPos0, boolean truncate, boolean binary) throws Exception {
    String original_path = path0;
    path0 = getPath(path0);
    String path_f = path0;
    StringBuffer uid = new StringBuffer(String.valueOf(path0.substring(1)) + (this.config.getProperty("random_id", "true").equals("true") ? ("/" + Common.makeBoundary(10)) : ""));
    Properties stat_existing = new Properties();
    Vector segments = new Vector();
    Vector tempResumeParts = null;
    StringBuffer partNumber = new StringBuffer("1");
    StringBuffer partNumberPos = new StringBuffer("1");
    String tempUploadId = null;
    boolean needCopyResume = false;
    ByteArrayOutputStream temp_buf = new ByteArrayOutputStream();
    Properties p_upload = new Properties();
    if (startPos0 > 0L) {
      stat_existing = stat(original_path);
      if (stat_existing.containsKey("segments")) {
        uid.setLength(0);
        uid.append(stat_existing.getProperty("uid"));
        segments.addAll((Vector)stat_existing.get("segments"));
        partNumber.setLength(0);
        partNumber.append(Integer.parseInt(segments.elementAt(segments.size() - 1).toString().split(":")[0]) + 1);
        p_upload.put("segments", segments);
      } else if (Long.parseLong(stat_existing.getProperty("size")) < 5242880L) {
        Common.streamCopier(download3(original_path, -1L, -1L, true), temp_buf, false, true, false);
        deleteAws(stat_existing.getProperty("uid"));
        startPos0 = 0L;
      } else {
        if (this.config.getProperty("random_id", "true").equals("false")) {
          String rnfr = stat_existing.getProperty("uid");
          String rnto = String.valueOf(stat_existing.getProperty("uid")) + "_" + Common.makeBoundary(4) + ".original";
          StringBuffer bucketNameSB = new StringBuffer();
          URLConnection urlc = doAction("PUT", "/" + this.bucketName0 + "/" + rnto, bucketNameSB, true, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
          urlc.setRequestProperty("x-amz-copy-source", "/" + this.bucketName0 + "/" + rnfr);
          urlc.setRequestProperty("x-amz-metadata-directive", "COPY");
          Properties mimes = Common.mimes;
          String ext = "NULL";
          if (rnto.toString().lastIndexOf(".") >= 0)
            ext = rnto.toString().substring(rnto.toString().lastIndexOf(".")).toUpperCase(); 
          if (mimes.getProperty(ext, "").equals(""))
            ext = "NULL"; 
          Common.updateMimes();
          doStandardAmazonAlterations(urlc, Common.mimes.getProperty(ext), bucketNameSB.toString());
          int code = urlc.getResponseCode();
          String result = URLConnection.consumeResponse(urlc.getInputStream());
          urlc.disconnect();
          get_cache_item("list_cache").clear();
          get_cache_item("cache_resume").clear();
          if (code < 200 || code > 299) {
            log(String.valueOf(result) + "\r\n");
            throw new Exception(result);
          } 
          deleteAws(rnfr);
          stat_existing.put("uid", rnto);
        } 
        needCopyResume = true;
      } 
    } else {
      delete(original_path);
    } 
    long startPos = startPos0;
    p_upload.put("size", (new StringBuffer(String.valueOf(startPos))).toString());
    p_upload.put("uid", uid.toString().replace(" ", "+"));
    writeFs(this.s3_root, this.bucketName0, this.replicating, path0, p_upload);
    if (this.config.getProperty("segmented", "false").equals("false")) {
      String request_parameter = "?uploads";
      boolean s3_sha256 = System.getProperty("crushftp.s3_sha256", "false").equals("true");
      if (this.config.containsKey("s3_sha256"))
        s3_sha256 = this.config.getProperty("s3_sha256", "false").equals("true"); 
      if (s3_sha256 || !this.config.getProperty("server_side_encrypt_kms", "").equals(""))
        request_parameter = String.valueOf(request_parameter) + "="; 
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + ((this.bucketName0.equals("") || this.config.getProperty("s3_bucket_in_path", "false").equals("true")) ? "" : (String.valueOf(this.bucketName0) + ".")) + this.region_host + "/" + (this.config.getProperty("s3_bucket_in_path", "false").equals("true") ? (String.valueOf(this.bucketName0) + "/") : "") + handle_path_special_chars(uid.toString(), false) + request_parameter), this.config);
      urlc.setRemoveDoubleEncoding(true);
      urlc.setRequestMethod("POST");
      if (!this.config.getProperty("server_side_encrypt_kms", "").equals("")) {
        urlc.setRequestProperty("x-amz-server-side-encryption", "aws:kms");
        urlc.setRequestProperty("x-amz-server-side-encryption-aws-kms-key-id", this.config.getProperty("server_side_encrypt_kms", ""));
      } else if (this.config.getProperty("server_side_encrypt", "false").equals("true")) {
        urlc.setRequestProperty("x-amz-server-side-encryption", "AES256");
      } 
      urlc.setDoOutput(false);
      Properties mimes = Common.mimes;
      String ext = "NULL";
      if (path0.toString().lastIndexOf(".") >= 0)
        ext = path0.toString().substring(path0.toString().lastIndexOf(".")).toUpperCase(); 
      if (mimes.getProperty(ext, "").equals(""))
        ext = "NULL"; 
      Common.updateMimes();
      doStandardAmazonAlterations(urlc, Common.mimes.getProperty(ext), this.bucketName0);
      int code = urlc.getResponseCode();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        throw new IOException(result);
      } 
      Element root = Common.getSaxBuilder().build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
      tempUploadId = getKeyText(root, "UploadId");
    } else {
      p_upload.put("segments", segments);
    } 
    tempResumeParts = new Vector();
    Vector resumePartsDone = new Vector();
    Vector resumeParts = tempResumeParts;
    String uploadId = tempUploadId;
    long maxBuf = Long.parseLong(this.config.getProperty("s3_buffer", "5"));
    if (maxBuf < 5L)
      maxBuf = 5L; 
    long maxBufferSize = maxBuf * 1024L * 1024L;
    if (needCopyResume) {
      doCopyResume(tempResumeParts, resumePartsDone, partNumber, partNumberPos, tempUploadId, "/" + this.bucketName0 + "/" + stat_existing.getProperty("uid"), "/" + this.bucketName0 + "/" + uid, startPos);
      deleteAws(stat_existing.getProperty("uid"));
    } 
    this.uploading = true;
    this.out = new null.OutputWrapper(this, startPos, p_upload, maxBufferSize, partNumber, partNumberPos, resumeParts, resumePartsDone, segments, uid, uploadId, path_f);
    if (temp_buf.size() > 0)
      this.out.write(temp_buf.toByteArray()); 
    return this.out;
  }
  
  public boolean setSize(String path0, long size) throws Exception {
    Properties stat0 = stat(path0);
    if (stat0 == null)
      return false; 
    if (stat0.getProperty("type", "").equalsIgnoreCase("DIR"))
      return false; 
    path0 = getPath(path0);
    Properties p_upload = new Properties();
    p_upload.put("uid", stat0.getProperty("uid"));
    p_upload.put("size", (new StringBuffer(String.valueOf(size))).toString());
    try {
      writeFs(this.s3_root, this.bucketName0, this.replicating, path0, p_upload);
      return true;
    } catch (Exception e) {
      Common.log("S3_CLIENT", 0, e);
      return false;
    } 
  }
  
  public boolean delete2(String path0) throws Exception {
    Properties stat0 = stat(path0);
    if (stat0 == null)
      return false; 
    path0 = getPath(path0);
    Vector items = new Vector();
    if ((new File_S(String.valueOf(this.s3_root) + this.bucketName0 + path0)).isDirectory()) {
      Vector items2 = new Vector();
      Common.appendListing_S(String.valueOf(this.s3_root) + this.bucketName0 + path0, items2, "", 3, false);
      if (items2.size() > 1)
        return false; 
      items = items2;
    } else {
      items.addElement(new File_S(String.valueOf(this.s3_root) + this.bucketName0 + path0));
    } 
    boolean deleted = true;
    for (int x = items.size() - 1; x >= 0; x--) {
      File_S f = items.elementAt(x);
      if (!f.getName().equals(".DS_Store") && 
        !f.isDirectory() && 
        !f.delete() && f.exists())
        deleted = false; 
    } 
    if (deleted)
      Common.recurseDelete(String.valueOf(this.s3_root) + this.bucketName0 + path0, false); 
    return deleted;
  }
  
  public boolean delete(String path0) throws Exception {
    Properties stat0 = stat(path0);
    if (stat0 == null)
      return false; 
    path0 = getPath(path0);
    Vector items = new Vector();
    if ((new File_S(String.valueOf(this.s3_root) + this.bucketName0 + path0)).isDirectory()) {
      Vector items2 = new Vector();
      Common.appendListing_S(String.valueOf(this.s3_root) + this.bucketName0 + path0, items2, "", 3, false);
      if (items2.size() > 1)
        return false; 
      items = items2;
    } else {
      items.addElement(new File_S(String.valueOf(this.s3_root) + this.bucketName0 + path0));
    } 
    for (int x = items.size() - 1; x >= 0; x--) {
      File_S f = items.elementAt(x);
      if (!f.getName().equals(".DS_Store") && 
        !f.isDirectory()) {
        Properties p = (Properties)Common.readXMLObject(f);
        if (p.containsKey("segments")) {
          Vector threads = new Vector();
          Vector errors = new Vector();
          Vector segments = (Vector)p.get("segments");
          for (int xx = 0; xx < segments.size(); xx++) {
            String delete_path = String.valueOf(p.getProperty("uid")) + "_" + segments.elementAt(xx).toString().split(":")[0];
            threads.addElement(delete_path);
            while (threads.size() > 20)
              Thread.sleep(100L); 
            Worker.startWorker(new Runnable(this, delete_path, errors, threads) {
                  final S3CrushClient this$0;
                  
                  private final String val$delete_path;
                  
                  private final Vector val$errors;
                  
                  private final Vector val$threads;
                  
                  public void run() {
                    boolean ok2 = true;
                    try {
                      for (int loop = 0; loop < 10; loop++) {
                        ok2 = true;
                        if (this.this$0.deleteAws(this.val$delete_path))
                          break; 
                        ok2 = false;
                      } 
                    } catch (Exception e) {
                      Common.log("S3_CLIENT", 1, e);
                      ok2 = false;
                    } 
                    if (!ok2)
                      this.val$errors.addElement(this.val$delete_path); 
                    this.val$threads.remove(this.val$delete_path);
                  }
                });
          } 
          for (int loops = 0; loops < 6000 && threads.size() > 0; loops++)
            Thread.sleep(100L); 
          if (errors.size() > 0) {
            log("Failed to delete:" + errors);
            return false;
          } 
          f.delete();
        } else {
          boolean ok2 = true;
          try {
            for (int loop = 0; loop < 10; loop++) {
              ok2 = true;
              if (deleteAws(p.getProperty("uid"))) {
                f.delete();
                break;
              } 
              ok2 = false;
            } 
          } catch (Exception e) {
            Common.log("S3_CLIENT", 1, e);
            ok2 = false;
          } 
          if (!ok2)
            throw new Exception("Can't delete:" + p.getProperty("uid")); 
        } 
      } 
    } 
    Common.recurseDelete(String.valueOf(this.s3_root) + this.bucketName0 + path0, false);
    return true;
  }
  
  private boolean deleteAws(String uid) throws Exception {
    URLConnection urlc = doAction("DELETE", "/" + this.bucketName0 + "/" + uid.replace("+", " "), (StringBuffer)null, false, true, this.config.getProperty("s3_bucket_in_path", "false").equals("true"));
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code == 403 && this.config.getProperty("delete_403", "false").equalsIgnoreCase("true"))
      return true; 
    if ((code < 200 || code > 299) && code != 404) {
      log(String.valueOf(code) + ":" + result + "\r\n");
      return false;
    } 
    return true;
  }
  
  public boolean makedir(String path0) throws Exception {
    path0 = getPath(path0);
    if (!path0.endsWith("/"))
      path0 = String.valueOf(path0) + "/"; 
    if (path0.equals("/"))
      return true; 
    return (new File_S(String.valueOf(this.s3_root) + this.bucketName0 + path0)).mkdirs();
  }
  
  public boolean makedirs(String path0) throws Exception {
    return makedir(path0);
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    rnfr = getPath(rnfr);
    rnto = getPath(rnto);
    return (new File_S(String.valueOf(this.s3_root) + this.bucketName0 + rnfr)).renameTo(new File_S(String.valueOf(this.s3_root) + this.bucketName0 + rnto));
  }
  
  public Properties stat(String path0) throws Exception {
    if (path0.endsWith(":filetree"))
      path0 = path0.substring(0, path0.indexOf(":filetree") - 1); 
    path0 = getPath(path0);
    File_S f = new File_S(String.valueOf(this.s3_root) + this.bucketName0 + Common.dots(path0));
    if (!f.exists())
      return null; 
    if (f.isDirectory()) {
      Date date = new Date(f.lastModified());
      String str = "drwxrwxrwx   1    owner   group   0   " + this.yyyyMMddHHmmss.format(date) + "   " + this.dd.format(date) + " " + this.yyyy.format(date) + " /" + f.getName();
      Properties properties = parseStat(str);
      properties.put("url", String.valueOf(this.url) + this.bucketName0 + path0 + ((properties.getProperty("type").equalsIgnoreCase("DIR") && !path0.endsWith("/")) ? "/" : ""));
      properties.put("modified", (new StringBuffer(String.valueOf(date.getTime()))).toString());
      return properties;
    } 
    Properties p = (Properties)Common.readXMLObject(f);
    Date d = new Date(f.lastModified());
    String line = "-rwxrwxrwx   1    owner   group   " + p.getProperty("size", "0") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + f.getName();
    Properties stat = parseStat(line);
    stat.put("url", String.valueOf(this.url) + this.bucketName0 + path0);
    if (p.containsKey("segments"))
      stat.put("segments", p.get("segments")); 
    stat.put("uid", p.getProperty("uid").replace("+", " "));
    stat.put("modified", (new StringBuffer(String.valueOf(d.getTime()))).toString());
    return stat;
  }
  
  public boolean mdtm(String path0, long modified) throws Exception {
    path0 = getPath(path0);
    return (new File_S(String.valueOf(this.s3_root) + this.bucketName0 + path0)).setLastModified(modified);
  }
}
