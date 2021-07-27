package com.crushftp.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipTransfer {
  String boundary = "--" + Common.makeBoundary(11);
  
  ZipOutputStream zout = null;
  
  ZipInputStream zin = null;
  
  BufferedOutputStream out = null;
  
  BufferedInputStream in = null;
  
  public HttpURLConnection urlc = null;
  
  String url = null;
  
  String CrushAuth = null;
  
  String path = null;
  
  Properties params = null;
  
  boolean compress = true;
  
  ZipEntry zipEntry = null;
  
  public ZipTransfer(String url, String CrushAuth, String path, Properties params, boolean compress) {
    this.url = url;
    this.CrushAuth = CrushAuth;
    this.path = path;
    this.params = params;
    this.compress = compress;
  }
  
  public void openUpload() throws Exception {
    this.urlc = (HttpURLConnection)(new URL(this.url)).openConnection();
    this.urlc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + this.boundary.substring(2, this.boundary.length()));
    this.urlc.setRequestMethod("POST");
    this.urlc.setRequestProperty("Cookie", "CrushAuth=" + this.CrushAuth + ";");
    this.urlc.setUseCaches(false);
    this.urlc.setChunkedStreamingMode(9999);
    this.urlc.setDoOutput(true);
    this.out = new BufferedOutputStream(this.urlc.getOutputStream());
    if (this.params != null) {
      Enumeration en = this.params.keys();
      while (en.hasMoreElements()) {
        String key = en.nextElement().toString();
        if (key.toUpperCase().startsWith("META_")) {
          String val = this.params.getProperty(key, "");
          while (key.toUpperCase().startsWith("META_"))
            key = key.substring("META_".length()); 
          HTTPClient.writeEntry("META_" + key, val, this.out, this.boundary);
        } 
      } 
    } 
    HTTPClient.writeEntry("the_action", "STOR", this.out, this.boundary);
    HTTPClient.writeEntry("c2f", this.CrushAuth.toString().substring(this.CrushAuth.toString().length() - 4), this.out, this.boundary);
    HTTPClient.writeEntry("uploadPath", this.path, this.out, this.boundary);
    String cheat = "";
    this.out.write((String.valueOf(this.boundary) + "\r\n").getBytes("UTF8"));
    this.out.write(("Content-Disposition: form-data; name=\"fileupload" + cheat + "\"; filename=\"uploader" + (new Date()).getTime() + ".zipstream\"\r\n").getBytes("UTF8"));
    this.out.write("Content-Type: application/octet-stream\r\n".getBytes("UTF8"));
    this.out.write("\r\n".getBytes("UTF8"));
    this.out.flush();
    this.zout = new ZipOutputStream(this.out);
    if (!this.compress) {
      this.zout.setLevel(0);
    } else {
      this.zout.setLevel(9);
    } 
  }
  
  public void addUploadFile(String itemName, long modified) throws Exception {
    if (this.zipEntry != null)
      this.zout.closeEntry(); 
    this.zipEntry = new ZipEntry(itemName);
    if (modified > 0L)
      this.zipEntry.setTime(modified); 
    this.zout.putNextEntry(this.zipEntry);
  }
  
  public void write(byte[] b, int off, int len) throws Exception {
    this.zout.write(b, off, len);
  }
  
  public boolean closeUpload() throws Exception {
    if (this.zipEntry != null)
      this.zout.closeEntry(); 
    this.zout.finish();
    this.out.write("\r\n".getBytes("UTF8"));
    HTTPClient.writeEnd(this.out, this.boundary);
    int code = this.urlc.getResponseCode();
    if (this.urlc.getURL().toExternalForm().indexOf("/WebInterface/login.html") >= 0)
      code = 302; 
    if (code == 302)
      throw new Exception("Logged out."); 
    String result = Common.consumeResponse(this.urlc.getInputStream());
    this.urlc.disconnect();
    if (result.toUpperCase().indexOf("SUCCESS") < 0)
      throw new Exception(result); 
    return true;
  }
  
  public void openDownload(Vector files, String filters) throws Exception {
    try {
      this.urlc = (HttpURLConnection)(new URL(this.url)).openConnection();
      this.urlc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + this.boundary.substring(2, this.boundary.length()));
      this.urlc.setRequestMethod("POST");
      this.urlc.setRequestProperty("Cookie", "CrushAuth=" + this.CrushAuth + ";");
      this.urlc.setUseCaches(false);
      this.urlc.setChunkedStreamingMode(9999);
      this.urlc.setDoInput(true);
      this.urlc.setDoOutput(true);
      BufferedOutputStream out = new BufferedOutputStream(this.urlc.getOutputStream());
      if (this.params != null) {
        Enumeration en = this.params.keys();
        while (en.hasMoreElements()) {
          String key = en.nextElement().toString();
          if (key.toUpperCase().startsWith("META_")) {
            String val = this.params.getProperty(key, "");
            while (key.toUpperCase().startsWith("META_"))
              key = key.substring("META_".length()); 
            Common.writeEntry("META_" + key, val, out, this.boundary);
          } 
        } 
      } 
      HTTPClient.writeEntry("command", "downloadAsZip", out, this.boundary);
      HTTPClient.writeEntry("no_zip_compression", (new StringBuffer(String.valueOf(!this.compress))).toString(), out, this.boundary);
      HTTPClient.writeEntry("filters", filters, out, this.boundary);
      HTTPClient.writeEntry("c2f", this.CrushAuth.toString().substring(this.CrushAuth.toString().length() - 4), out, this.boundary);
      out.write((String.valueOf(this.boundary) + "\r\n").getBytes("UTF8"));
      out.write("Content-Disposition: form-data; name=\"paths\"\r\n".getBytes("UTF8"));
      out.write("\r\n".getBytes("UTF8"));
      for (int x = 0; x < files.size(); x++) {
        out.write(files.elementAt(x).toString().getBytes("UTF8"));
        if (x < files.size())
          out.write(":".getBytes("UTF8")); 
      } 
      out.write("\r\n".getBytes("UTF8"));
      Common.writeEnd(out, this.boundary);
      int code = this.urlc.getResponseCode();
      if (this.urlc.getURL().toExternalForm().indexOf("/WebInterface/login.html") >= 0)
        code = 302; 
      if (code == 302)
        throw new Exception("Logged out."); 
    } catch (SocketException e) {
      if (e.toString().indexOf("end of file") < 0)
        throw e; 
      this.urlc.disconnect();
    } 
    Thread.sleep(1000L);
    this.in = new BufferedInputStream(this.urlc.getInputStream());
    this.zin = new ZipInputStream(this.in);
  }
  
  public ZipEntry getDownloadFile() throws Exception {
    return this.zin.getNextEntry();
  }
  
  public int read(byte[] b) throws Exception {
    return this.zin.read(b);
  }
  
  public void closeDownload() throws Exception {
    this.zin.close();
    this.in.close();
  }
}
