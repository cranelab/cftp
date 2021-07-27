package com.crushftp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.jdom.input.SAXBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class S3Client extends GenericClient {
  SimpleDateFormat yyyyMMddtHHmmssSSS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US);
  
  SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  
  SecretKeySpec secretKey = null;
  
  String region_host = "s3.amazonaws.com";
  
  Properties cache = new Properties();
  
  Properties cache_resume = new Properties();
  
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
    System.setProperty("crushtunnel.debug", "2");
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
    this.region_host = (new VRL(url)).getHost().toLowerCase();
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.config.put("username", username.trim());
    this.config.put("password", password.trim());
    updateIamAuth();
    this.secretKey = new SecretKeySpec(this.config.getProperty("real_password", this.config.getProperty("password")).getBytes("UTF8"), "HmacSHA1");
    URLConnection urlc = doAction("GET", (new VRL(this.url)).getPath(), (StringBuffer)null, false);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      throw new IOException(result);
    } 
    return result;
  }
  
  public void logout() throws Exception {
    close();
  }
  
  public Vector list(String path0, Vector list) throws Exception {
    this.url = Common.getBaseUrl(this.url, false);
    String path = path0;
    String bucketName = "";
    if (!path.equals("/")) {
      bucketName = path.substring(1, path.indexOf("/", 1));
      path = path.substring(bucketName.length() + 1);
    } 
    Vector list2 = null;
    if (this.cache.containsKey(path0)) {
      Properties ctmp = (Properties)this.cache.get(path0);
      if (System.currentTimeMillis() - Long.parseLong(ctmp.getProperty("time")) < 30000L)
        list2 = (Vector)Common.CLONE(ctmp.get("o")); 
    } 
    if (list2 == null) {
      String last_key = null;
      list2 = new Vector();
      while (true) {
        String query = "?delimiter=%2F" + ((last_key != null) ? ("&marker=" + Common.replace_str(last_key, " ", "%20")) : "") + "&prefix=" + Common.url_encode(path.substring(1));
        URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + query), this.config);
        urlc.setDoOutput(false);
        doStandardAmazonAlterations(urlc, (String)null, bucketName);
        int code = urlc.getResponseCode();
        String result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code < 200 || code > 299) {
          log(String.valueOf(result) + "\r\n");
          throw new IOException(result);
        } 
        if (result.length() == 0)
          return list; 
        Element root = (new SAXBuilder()).build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
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
          if (last_key == null) {
            List prefixes = getElements(root, "CommonPrefixes");
            for (int i = 0; prefixes != null && i < prefixes.size(); i++) {
              Element content = prefixes.get(i);
              String name = getKeyText(content, "Prefix");
              name = name.substring(0, name.length() - 1);
              log(String.valueOf(name) + "\r\n");
              boolean folder = true;
              Date d = new Date();
              String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   0   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + name;
              Properties stat = parseStat(line);
              stat.put("url", String.valueOf(Common.url_decode(this.url)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1) + stat.getProperty("name"));
              log(stat + "\r\n");
              list2.addElement(stat);
            } 
          } 
          List contents = getElements(root, "Contents");
          for (int x = 0; x < contents.size(); x++) {
            Element content = contents.get(x);
            String name = getKeyText(content, "Key");
            if (!name.equals(path.substring(1))) {
              last_key = name;
              log(String.valueOf(name) + "\r\n");
              boolean folder = !(!name.endsWith("/") && urlc.getHeaderField("Content-Type").indexOf("x-directory") < 0);
              if (folder)
                name = name.substring(0, name.length() - 1); 
              Date d = this.yyyyMMddtHHmmssSSS.parse(getKeyText(content, "LastModified"));
              String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + getKeyText(content, "Size") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + name;
              Properties stat = parseStat(line);
              stat.put("url", String.valueOf(Common.url_decode(this.url)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1) + stat.getProperty("name"));
              log(stat + "\r\n");
              list2.addElement(stat);
            } 
          } 
          Properties ctmp = new Properties();
          ctmp.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
          ctmp.put("o", Common.CLONE(list2));
          this.cache.put(path0, ctmp);
        } 
        try {
          if (!getElement(root, "IsTruncated").getText().equalsIgnoreCase("true"))
            break; 
        } catch (Exception exception) {
          break;
        } 
      } 
    } 
    list.addAll(list2);
    if (!bucketName.equals("")) {
      list2 = null;
      if (this.cache_resume.containsKey(path0)) {
        Properties ctmp = (Properties)this.cache_resume.get(path0);
        if (System.currentTimeMillis() - Long.parseLong(ctmp.getProperty("time")) < 30000L)
          list2 = (Vector)Common.CLONE(ctmp.get("o")); 
      } 
      if (list2 == null) {
        log("Looking for failed transfers, or in progress transfers.");
        String query = "?uploads";
        URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + query), this.config);
        urlc.setDoOutput(false);
        doStandardAmazonAlterations(urlc, (String)null, bucketName);
        int code = urlc.getResponseCode();
        String result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code < 200 || code > 299) {
          log(String.valueOf(result) + "\r\n");
          throw new IOException(result);
        } 
        list2 = new Vector();
        if (result.length() == 0)
          return list; 
        Element root = (new SAXBuilder()).build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
        List uploads = getElements(root, "Upload");
        for (int x = 0; x < uploads.size(); x++) {
          Element content = uploads.get(x);
          String name = getKeyText(content, "Key");
          if (!name.equals(path.substring(1))) {
            log(String.valueOf(name) + "\r\n");
            boolean folder = name.endsWith("/");
            if (folder)
              name = name.substring(0, name.length() - 1); 
            Date d = this.yyyyMMddtHHmmssSSS.parse(getKeyText(content, "Initiated"));
            query = String.valueOf(name.replace(' ', '+')) + "?uploadId=" + getKeyText(content, "UploadId");
            urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + "/" + query), this.config);
            urlc.setDoOutput(false);
            doStandardAmazonAlterations(urlc, (String)null, bucketName);
            code = urlc.getResponseCode();
            result = URLConnection.consumeResponse(urlc.getInputStream());
            urlc.disconnect();
            if (code >= 200 && code <= 299) {
              Element partRoot = (new SAXBuilder()).build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
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
              stat.put("url", String.valueOf(Common.url_decode(this.url)) + (bucketName.toString().equals("") ? "" : (String.valueOf(bucketName.toString()) + "/")) + path.substring(1) + stat.getProperty("name"));
              log(stat + "\r\n");
              list2.addElement(stat);
            } 
          } 
        } 
        Properties ctmp = new Properties();
        ctmp.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        ctmp.put("o", Common.CLONE(list2));
        this.cache_resume.put(path0, ctmp);
      } 
      list.addAll(list2);
    } 
    return list;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    URLConnection urlc = doAction("GET", path, (StringBuffer)null, false);
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
      tempPath = tempPath.replace(' ', '+');
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + tempPath + "?uploads"), this.config);
      urlc.setRequestMethod("POST");
      if (this.config.getProperty("server_side_encrypt", "false").equals("true"))
        urlc.setRequestProperty("x-amz-server-side-encryption", "AES256"); 
      urlc.setDoOutput(false);
      doStandardAmazonAlterations(urlc, (String)null, bucketName);
      int code = urlc.getResponseCode();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        throw new IOException(result);
      } 
      Element root = (new SAXBuilder()).build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
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
    tempPath = tempPath.replace(' ', '+');
    Properties chunk_part = resumeParts.elementAt(partNumberPos - 1);
    chunk_part.put("start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    for (int loops = 0; loops < 20; loops++) {
      URLConnection urlc = null;
      try {
        if (uploadId == null) {
          urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + tempPath + "_" + partNumber), this.config);
        } else {
          urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + tempPath + "?partNumber=" + partNumber + "&uploadId=" + uploadId), this.config);
        } 
        chunk_part.put("urlc", urlc);
        urlc.setRequestMethod("PUT");
        urlc.setDoOutput(true);
        urlc.setLength(buf2.size());
        doStandardAmazonAlterations(urlc, "application/binary", bucketName);
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
        Common.log("S3_CLIENT", 0, e);
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
    tempPath = tempPath.replace(' ', '+');
    long total_bytes = startPos;
    long pos = 0L;
    while (total_bytes > 0L) {
      long chunk_size = total_bytes;
      if (chunk_size > 1073741824L)
        chunk_size = 1073741824L; 
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + tempPath + "?partNumber=" + partNumber + "&uploadId=" + uploadId), this.config);
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
      Element root = (new SAXBuilder()).build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
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
    tempPath = tempPath.replace(' ', '+');
    int code = -1;
    String result = "";
    int loops = 0;
    while (code == -1 && loops++ < 60) {
      try {
        URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + tempPath + "?uploadId=" + uploadId), this.config);
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
        if (code >= 200 && code < 299)
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
    Properties stat = stat(path);
    if (stat == null)
      stat = new Properties(); 
    URLConnection urlc = doAction("DELETE", String.valueOf(path) + (!stat.getProperty("uploadId", "").equals("") ? ("?uploadId=" + stat.getProperty("uploadId", "")) : ""), (StringBuffer)null, false);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if ((code < 200 || code > 299) && code != 404) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
    updateCache(stat, path, "remove");
    return true;
  }
  
  private void updateCache(Properties stat, String path, String action) {
    Properties ctmp = (Properties)this.cache.get(Common.all_but_last(path));
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
          this.cache.put(Common.all_but_last(path), ctmp);
          break;
        } 
      } 
    } 
    ctmp = (Properties)this.cache_resume.get(Common.all_but_last(path));
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
          this.cache_resume.put(Common.all_but_last(path), ctmp);
          break;
        } 
      } 
    } 
  }
  
  public boolean makedir(String path) throws Exception {
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    URLConnection urlc = doAction("PUT", path, (StringBuffer)null, true);
    urlc.setLength(0L);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    this.cache.clear();
    this.cache_resume.clear();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
    return true;
  }
  
  public boolean makedirs(String path) throws Exception {
    return makedir(path);
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    Properties stat = stat(rnfr);
    if (stat.getProperty("type").equalsIgnoreCase("DIR"))
      return false; 
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("PUT", rnto, bucketNameSB, true);
    urlc.setRequestProperty("x-amz-copy-source", rnfr);
    urlc.setRequestProperty("x-amz-metadata-directive", "COPY");
    doStandardAmazonAlterations(urlc, (String)null, bucketNameSB.toString());
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    this.cache.clear();
    this.cache_resume.clear();
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
    StringBuffer bucketNameSB = new StringBuffer();
    URLConnection urlc = doAction("PUT", path, bucketNameSB, false);
    urlc.setLength(0L);
    urlc.setRequestProperty("x-amz-copy-source", path);
    urlc.setRequestProperty("x-amz-meta-modified", (new StringBuffer(String.valueOf(modified))).toString());
    urlc.setRequestProperty("x-amz-metadata-directive", "REPLACE");
    doStandardAmazonAlterations(urlc, (String)null, bucketNameSB.toString());
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
    urlc.setRequestProperty("Authorization", "AWS " + this.config.getProperty("real_username", this.config.getProperty("username")) + ":" + calculateAmazonSignature(urlc));
    urlc.setUseCaches(false);
  }
  
  public URLConnection doAction(String verb, String path, StringBuffer bucketNameSB, boolean do_secure) {
    String bucketName = path.substring(1);
    if (bucketName.indexOf("?") >= 0)
      bucketName = bucketName.substring(0, bucketName.indexOf("?")); 
    if (bucketName.indexOf("/") >= 0) {
      bucketName = bucketName.substring(0, bucketName.indexOf("/"));
      path = path.substring(path.indexOf("/", 1));
    } 
    if (path.equals("/" + bucketName))
      path = "/"; 
    path = path.replace(' ', '+');
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + (bucketName.equals("") ? "" : (String.valueOf(bucketName) + ".")) + this.region_host + path), this.config);
    urlc.setRequestMethod(verb);
    urlc.setDoOutput(false);
    if (do_secure && this.config.getProperty("server_side_encrypt", "false").equals("true"))
      urlc.setRequestProperty("x-amz-server-side-encryption", "AES256"); 
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
        if (a.toLowerCase().compareTo(b.toLowerCase()) > 0) {
          recs.setElementAt(b, j);
          recs.setElementAt(a, k);
          a = b;
        } 
      } 
    } 
    for (int x = recs.size() - 1; x >= 0; x--)
      data = String.valueOf(data) + recs.elementAt(x) + "\n"; 
    String tmp_path = "";
    if (bucketName.equals("")) {
      tmp_path = "/";
    } else if (urlc.getURL().getPath().indexOf("?delimiter") >= 0) {
      tmp_path = "/" + bucketName + urlc.getURL().getPath().substring(0, urlc.getURL().getPath().indexOf("?"));
    } else {
      tmp_path = "/" + bucketName + urlc.getURL().getPath();
    } 
    data = String.valueOf(data) + Common.url_encode(tmp_path, "/%.#@&?!\\=+");
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
}
