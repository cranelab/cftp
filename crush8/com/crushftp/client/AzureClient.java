package com.crushftp.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Vector;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class AzureClient extends GenericClient {
  public AzureClient(String url, String header, Vector log) {
    super(header, log);
    this.url = url;
    String[] parts = url.split("/");
    String container_type = "";
    if (this.url.contains("@file.core.windows.net")) {
      container_type = "@file.core.windows.net/";
      this.config.put("container_type", "share");
    } else if (this.url.contains("@blob.core.windows.net")) {
      container_type = "@blob.core.windows.net/";
      this.config.put("container_type", "blob");
      if (this.config.getProperty("upload_blob_type", "").equals(""))
        this.config.put("upload_blob_type", "appendblob"); 
    } else {
    
    } 
    int share_index = this.url.indexOf(container_type) + container_type.length();
    if (this.url.indexOf("/", share_index) < 0)
      this.url = String.valueOf(this.url) + "/"; 
    String share = this.url.substring(share_index, this.url.indexOf("/", share_index));
    this.config.put("share", share);
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.config.put("username", username.trim());
    this.config.put("password", VRL.vrlDecode(password.trim()));
    String restype = "?restype=";
    if (!getSASToken().equals(""))
      restype = "?comp=list&delimiter=%2F&restype="; 
    if (this.config.getProperty("container_type").equals("blob")) {
      restype = String.valueOf(restype) + "container";
    } else {
      restype = String.valueOf(restype) + "share";
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + "/" + this.config.getProperty("share", "") + restype + getSASToken()), new Properties());
    urlc.setRequestMethod("GET");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    if (urlc.getResponseCode() == 200)
      return "Success"; 
    if (urlc.getResponseCode() < 200 || urlc.getResponseCode() > 299) {
      Common.log("AZURE_CLIENT", 1, String.valueOf(urlc.getResponseCode()) + " " + result);
      throw new IOException(result);
    } 
    return "Failure!";
  }
  
  public Vector list(String path, Vector list) throws Exception {
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    String prefix = "";
    if (this.config.getProperty("container_type").equals("blob") && !path.equals("/" + this.config.getProperty("share") + "/"))
      prefix = path.substring(path.indexOf("/" + this.config.getProperty("share") + "/") + this.config.getProperty("share").length() + 2, path.length()); 
    String result = list2(path, prefix, "");
    Common.log("AZURE_CLIENT", 2, "List : all result : " + result);
    String marker = "";
    if (result.contains("<NextMarker>") && result.contains("</NextMarker>") && result.indexOf("<NextMarker>") < result.indexOf("</NextMarker>"))
      marker = result.substring(result.indexOf("<NextMarker>") + "<NextMarker>".length(), result.indexOf("</NextMarker>")); 
    if (this.config.getProperty("container_type").equals("share")) {
      parseShareList(path, list, result);
    } else if (this.config.getProperty("container_type").equals("blob")) {
      parseBlobList(prefix, path, list, result);
    } 
    if (!marker.equals(""))
      for (int x = 0; x < 100; x++) {
        result = list2(path, prefix, marker);
        marker = "";
        if (result.contains("<NextMarker>") && result.contains("</NextMarker>") && result.indexOf("<NextMarker>") < result.indexOf("</NextMarker>"))
          marker = result.substring(result.indexOf("<NextMarker>") + "<NextMarker>".length(), result.indexOf("</NextMarker>")); 
        if (this.config.getProperty("container_type").equals("share")) {
          parseShareList(path, list, result);
        } else if (this.config.getProperty("container_type").equals("blob")) {
          parseBlobList(prefix, path, list, result);
        } 
        if (marker.equals(""))
          break; 
      }  
    return list;
  }
  
  private String list2(String path, String prefix, String marker) throws Exception {
    String restype = "";
    String url_path = path;
    if (this.config.getProperty("container_type").equals("blob")) {
      if (!marker.equals("")) {
        restype = "delimiter=%2F&marker=" + marker + "&maxresults=5000&prefix=" + prefix.replace("/", "%2F") + "&restype=container";
      } else {
        restype = "delimiter=%2F&maxresults=5000&prefix=" + prefix.replace("/", "%2F") + "&restype=container";
      } 
      url_path = "/" + this.config.getProperty("share") + "/";
    } else if (this.config.getProperty("container_type").equals("share")) {
      restype = "maxresults=5000&restype=directory";
      if (!marker.equals(""))
        restype = "marker=" + marker + "&maxresults=5000&restype=directory"; 
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + url_path + "?comp=list&" + restype + getSASToken()), new Properties());
    urlc.setRequestMethod("GET");
    urlc.setDoInput(true);
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    urlc.getResponseCode();
    return URLConnection.consumeResponse(urlc.getInputStream());
  }
  
  private void parseBlobList(String prefix, String path, Vector list, String result) {
    if (result.contains("<Blobs>") && result.contains("</Blobs>") && result.indexOf("<Blobs>") < result.indexOf("</Blobs>")) {
      result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + result.substring(result.indexOf("<Blobs>"), result.indexOf("</Blobs>") + "</Blobs>".length());
      try {
        SAXBuilder sax = Common.getSaxBuilder();
        Document doc = sax.build(new StringReader(result));
        Iterator i = doc.getRootElement().getChildren().iterator();
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        while (i.hasNext()) {
          Element element = i.next();
          Properties p = new Properties();
          Iterator i2 = element.getChildren().iterator();
          if (((Element)element.getChildren().get(0)).getText().equals(prefix))
            continue; 
          while (i2.hasNext()) {
            Element element2 = i2.next();
            if (element2.getName().equals("Name") && !element2.getText().equals("")) {
              p.put("name", Common.last(element2.getText()));
              if (!p.getProperty("name").endsWith("/")) {
                p.put("permissions", "-rwxrwxrwx");
                p.put("type", "FILE");
              } else {
                p.put("name", p.getProperty("name").substring(0, p.getProperty("name").length() - 1));
                p.put("type", "DIR");
                p.put("permissions", "drwxrwxrwx");
                p.put("size", "0");
              } 
              p.put("path", String.valueOf(path) + p.getProperty("name"));
            } 
            if (element2.getName().equals("Properties")) {
              Iterator i3 = element2.getChildren().iterator();
              while (i3.hasNext()) {
                Element element3 = i3.next();
                if (element3.getName().equals("Content-Length"))
                  p.put("size", element3.getText()); 
                if (element3.getName().equals("Last-Modified")) {
                  Date date = new Date();
                  try {
                    date = fmt.parse(element3.getText());
                  } catch (Exception e) {
                    Common.log("AZURE_CLIENT", 2, e);
                  } 
                  p.put("modified", (new StringBuffer(String.valueOf(date.getTime()))).toString());
                } 
              } 
            } 
          } 
          p.put("url", "azure://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@blob.core.windows.net" + path + p.getProperty("name"));
          p.put("owner", "owner");
          p.put("group", "group");
          Common.log("AZURE_CLIENT", 2, "List : " + path + p.getProperty("name"));
          list.add(p);
        } 
      } catch (Exception e) {
        log(e);
        Common.log("AZURE_CLIENT", 1, e);
      } 
    } 
  }
  
  private void parseShareList(String path, Vector list, String result) {
    if (result.contains("<Entries>") && result.contains("</Entries>") && result.indexOf("<Entries>") < result.indexOf("</Entries>")) {
      result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + result.substring(result.indexOf("<Entries>"), result.indexOf("</Entries>") + "</Entries>".length());
      try {
        SAXBuilder sax = Common.getSaxBuilder();
        Document doc = sax.build(new StringReader(result));
        Iterator i = doc.getRootElement().getChildren().iterator();
        while (i.hasNext()) {
          Element element = i.next();
          Properties p = new Properties();
          if (element.getName().equals("File")) {
            p.put("permissions", "-rwxrwxrwx");
            p.put("type", "FILE");
          } else {
            p.put("type", "DIR");
            p.put("permissions", "drwxrwxrwx");
            p.put("check_all_recursive_deletes", "true");
          } 
          Iterator i2 = element.getChildren().iterator();
          p.put("size", "0");
          while (i2.hasNext()) {
            Element element2 = i2.next();
            if (element2.getName().equals("Name")) {
              p.put("name", element2.getText());
              p.put("path", String.valueOf(path) + element2.getText());
            } 
            if (element2.getName().equals("Properties") && element2.getChildren().size() > 0) {
              Element size = element2.getChild("Content-Length");
              p.put("size", size.getText());
            } 
          } 
          p.put("url", "azure://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@file.core.windows.net" + path + p.getProperty("name"));
          p.put("owner", "owner");
          p.put("group", "group");
          String item_restype = "directory";
          if (p.getProperty("type", "").equals("FILE"))
            item_restype = "file"; 
          String path2 = "";
          String last_modified = "";
          if (!this.config.getProperty("ignore_mdtm_on_list", "false").equals("true"))
            try {
              URLConnection urlc2 = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + p.getProperty("name") + "?restype=" + item_restype + getSASToken()), new Properties());
              urlc2.setRequestMethod("HEAD");
              urlc2.setDoInput(true);
              urlc2.setUseCaches(false);
              signRequestSK(urlc2);
              urlc2.connect();
              if (urlc2.getResponseCode() != 200) {
                String result2 = URLConnection.consumeResponse(urlc2.getInputStream());
                Common.log("AZURE_CLIENT", 2, String.valueOf(urlc2.getResponseCode()) + result2 + "\r\n");
              } 
              last_modified = urlc2.getHeaderField("LAST-MODIFIED");
              if (urlc2.getHeaderField("CONTENT-LENGTH") != null)
                p.put("size", urlc2.getHeaderField("CONTENT-LENGTH")); 
              Common.log("AZURE_CLIENT", 2, "List : Get modified date. path : " + path + p.getProperty("name") + " modified : " + last_modified + "\r\n");
            } catch (Exception e) {
              Common.log("AZURE_CLIENT", 1, e);
            }  
          long modified = System.currentTimeMillis();
          Common.log("AZURE_CLIENT", 2, "List : modified :" + modified);
          if (!last_modified.equals(""))
            try {
              String[] dtz = last_modified.split(" ");
              String time_zone = dtz[dtz.length - 1];
              SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
              fmt.setTimeZone(TimeZone.getTimeZone(time_zone));
              Date d = fmt.parse(last_modified);
              SimpleDateFormat mmmfmt = new SimpleDateFormat("MM");
              mmmfmt.setTimeZone(TimeZone.getTimeZone(time_zone));
              p.put("month", mmmfmt.format(d));
              SimpleDateFormat ddfmt = new SimpleDateFormat("dd");
              ddfmt.setTimeZone(TimeZone.getTimeZone(time_zone));
              p.put("day", ddfmt.format(d));
              SimpleDateFormat yyyyfmt = new SimpleDateFormat("yyyy");
              ddfmt.setTimeZone(TimeZone.getTimeZone(time_zone));
              p.put("time_or_year", yyyyfmt.format(d));
              modified = d.getTime();
              Common.log("AZURE_CLIENT", 2, "List : parsed modified :" + modified);
            } catch (Exception e) {
              Common.log("AZURE_CLIENT", 1, e);
            }  
          p.put("modified", (new StringBuffer(String.valueOf(modified))).toString());
          Common.log("AZURE_CLIENT", 2, "List : " + path + p.getProperty("name"));
          list.add(p);
        } 
      } catch (Exception e) {
        log(e);
        Common.log("AZURE_CLIENT", 1, e);
      } 
    } 
  }
  
  public Properties stat(String path) throws Exception {
    if (path.endsWith(":filetree"))
      path = path.substring(0, path.indexOf(":filetree") - 1); 
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    Common.log("AZURE_CLIENT", 2, "Stat : " + path);
    if (path.equals("/" + this.config.getProperty("share"))) {
      Properties p_root = new Properties();
      p_root.put("name", "");
      p_root.put("path", path);
      p_root.put("type", "DIR");
      p_root.put("permissions", "drwxrwxrwx");
      p_root.put("size", "0");
      String host = "";
      if (this.config.getProperty("container_type").equals("share")) {
        host = "file.core.windows.net";
      } else if (this.config.getProperty("container_type").equals("blob")) {
        host = "blob.core.windows.net";
      } 
      p_root.put("url", "azure://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@" + host + path);
      p_root.put("owner", "owner");
      p_root.put("group", "group");
      p_root.put("modified", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      return p_root;
    } 
    if (this.config.getProperty("container_type").equals("share")) {
      String params = "";
      if (!getSASToken().equals(""))
        params = "?" + getSASToken().substring(1); 
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + params), new Properties());
      urlc.setRequestMethod("GET");
      urlc.setDoInput(true);
      urlc.setUseCaches(false);
      signRequestSK(urlc);
      urlc.connect();
      Properties p = new Properties();
      if (urlc.getResponseCode() != 200) {
        String result = URLConnection.consumeResponse(urlc.getInputStream());
        if (urlc.getResponseCode() == 404) {
          URLConnection urlc2 = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + "?comp=metadata&restype=directory" + getSASToken()), new Properties());
          urlc2.setRequestMethod("GET");
          urlc2.setDoInput(true);
          urlc2.setUseCaches(false);
          signRequestSK(urlc2);
          urlc2.connect();
          if (urlc2.getResponseCode() == 200) {
            get_file_from_request_header(urlc2, path, p, false);
            return p;
          } 
          String result2 = URLConnection.consumeResponse(urlc2.getInputStream());
          Common.log("AZURE_CLIENT", 2, String.valueOf(urlc2.getResponseCode()) + result2);
          return null;
        } 
        Common.log("AZURE_CLIENT", 2, String.valueOf(urlc.getResponseCode()) + " " + result);
        return null;
      } 
      get_file_from_request_header(urlc, path, p, true);
      return p;
    } 
    if (this.config.getProperty("container_type").equals("blob")) {
      Vector v = new Vector();
      list(Common.all_but_last(path), v);
      for (int x = 0; x < v.size(); x++) {
        Properties p = v.elementAt(x);
        if (p.getProperty("name").equals(Common.last(path)))
          return p; 
      } 
      return null;
    } 
    return null;
  }
  
  private void get_file_from_request_header(URLConnection urlc, String path, Properties p, boolean is_file) throws Exception {
    String name = path;
    if (name.endsWith("/"))
      name = name.substring(0, name.length() - 1); 
    name = name.substring(name.lastIndexOf("/") + 1, name.length());
    p.put("name", name);
    p.put("path", path);
    if (is_file) {
      p.put("permissions", "-rwxrwxrwx");
      p.put("type", "FILE");
      p.put("size", urlc.getHeaderField("CONTENT-LENGTH"));
    } else {
      p.put("type", "DIR");
      p.put("permissions", "drwxrwxrwx");
      p.put("size", "0");
      p.put("check_all_recursive_deletes", "true");
    } 
    p.put("url", "azure://" + this.config.getProperty("username") + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@file.core.windows.net" + path);
    p.put("owner", "owner");
    p.put("group", "group");
    String last_modified = urlc.getHeaderField("LAST-MODIFIED");
    Date date = new Date();
    try {
      String[] dtz = last_modified.split(" ");
      String time_zone = dtz[dtz.length - 1];
      SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
      fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
      date = fmt.parse(last_modified);
      Common.log("AZURE_CLIENT", 2, "STAT : parsed modified :" + date.getTime());
      fmt.setTimeZone(TimeZone.getTimeZone(time_zone));
      Date d = fmt.parse(last_modified);
      SimpleDateFormat mmmfmt = new SimpleDateFormat("MM");
      mmmfmt.setTimeZone(TimeZone.getTimeZone(time_zone));
      p.put("month", mmmfmt.format(d));
      SimpleDateFormat ddfmt = new SimpleDateFormat("dd");
      ddfmt.setTimeZone(TimeZone.getTimeZone(time_zone));
      p.put("day", ddfmt.format(d));
      SimpleDateFormat yyyyfmt = new SimpleDateFormat("yyyy");
      ddfmt.setTimeZone(TimeZone.getTimeZone(time_zone));
      p.put("time_or_year", yyyyfmt.format(d));
    } catch (Exception e) {
      log(e);
      Common.log("AZURE_CLIENT", 1, e);
    } 
    p.put("modified", (new StringBuffer(String.valueOf(date.getTime()))).toString());
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    String params = "";
    if (!getSASToken().equals(""))
      params = "?" + getSASToken().substring(1); 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + path + params), new Properties());
    urlc.setRequestMethod("GET");
    urlc.setDoInput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    if (urlc.getResponseCode() != 200) {
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      Common.log("AZURE_CLIENT", 2, String.valueOf(urlc.getResponseCode()) + " " + result);
      throw new Exception("Download Error: " + urlc.getResponseCode() + " " + result);
    } 
    this.in = urlc.getInputStream();
    return this.in;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    String path2 = path;
    if (!path.startsWith("/"))
      path2 = "/" + path; 
    String upload_path = path2;
    long resume_pos = startPos;
    String params = "";
    if (!this.config.getProperty("container_type").equals("blob") || !this.config.getProperty("upload_blob_type", "appendblob").equals("blockblob")) {
      if (!getSASToken().equals(""))
        params = "?" + getSASToken().substring(1); 
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + upload_path + params), new Properties());
      urlc.setRequestMethod("PUT");
      urlc.setRequestProperty("x-ms-content-length", "0");
      urlc.setRequestProperty("x-ms-type", "file");
      if (Common.last(path).contains(".")) {
        String ext = Common.last(path).substring(Common.last(path).lastIndexOf(".")).toUpperCase();
        Common.mimes.getProperty(ext, "");
        if (!Common.mimes.getProperty(ext, "").equals(""))
          urlc.setRequestProperty("Content-Type", Common.mimes.getProperty(ext, "")); 
      } 
      urlc.setRequestProperty("Content-Length", "0");
      if (this.config.getProperty("container_type").equals("blob"))
        urlc.setRequestProperty("x-ms-blob-type", "AppendBlob"); 
      urlc.setDoOutput(true);
      urlc.setUseCaches(false);
      signRequestSK(urlc);
      urlc.connect();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      if (urlc.getResponseCode() != 201) {
        Common.log("AZURE_CLIENT", 1, String.valueOf(urlc.getResponseCode()) + " " + result);
        throw new Exception("Upload Error :" + urlc.getResponseCode() + " " + result);
      } 
      urlc.disconnect();
    } 
    this.out = new null.OutputWrapper(this, resume_pos, upload_path, path);
    return this.out;
  }
  
  public Vector getBlockList(String upload_path, String type) {
    Vector blockIds = new Vector();
    try {
      String commands = "blocklisttype=" + type + "&comp=blocklist";
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + upload_path + "?" + commands + getSASToken()), new Properties());
      urlc.setRequestMethod("GET");
      urlc.setDoInput(true);
      urlc.setUseCaches(false);
      signRequestSK(urlc);
      urlc.connect();
      String result = "";
      if (urlc.getResponseCode() == 200) {
        result = Common.consumeResponse(urlc.getInputStream());
        if (result.contains("<BlockList>") && result.contains("</BlockList>") && result.indexOf("<BlockList>") < result.indexOf("</BlockList>")) {
          result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + result.substring(result.indexOf("<BlockList>"), result.indexOf("</BlockList>") + "</BlockList>".length());
          SAXBuilder sax = Common.getSaxBuilder();
          Document doc = sax.build(new StringReader(result));
          Iterator i = doc.getRootElement().getChildren().iterator();
          while (i.hasNext()) {
            Element element = i.next();
            Iterator i2 = element.getChildren().iterator();
            while (i2.hasNext()) {
              Element element2 = i2.next();
              Iterator i3 = element2.getChildren().iterator();
              while (i3.hasNext()) {
                Element element3 = i3.next();
                if (element3.getName().equals("Name")) {
                  String text = element3.getText();
                  if (text != null) {
                    Common.log("AZURE_CLIENT", 2, "Block upload - List " + type + "bocks" + new String(Base64.decode(text), "UTF8"));
                    blockIds.add(text);
                  } 
                } 
              } 
            } 
          } 
        } 
      } else {
        result = Common.consumeResponse(urlc.getInputStream());
        Common.log("AZURE_CLIENT", 1, String.valueOf(urlc.getResponseCode()) + " " + urlc.getResponseMessage() + " " + result);
      } 
    } catch (Exception e) {
      log(e);
      Common.log("AZURE_CLIENT", 1, e);
    } 
    return blockIds;
  }
  
  public boolean delete(String path) throws Exception {
    Properties p = stat(path);
    String restype = "";
    if (p.getProperty("type").equals("FILE")) {
      restype = "file";
    } else {
      restype = "directory";
    } 
    String commands = "?restype=" + restype;
    if (this.config.getProperty("container_type").equals("blob")) {
      commands = "";
      if (restype.equals("directory") && !path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
      String prefix = "";
      Vector list = new Vector();
      if (!path.equals("/" + this.config.getProperty("share") + "/"))
        prefix = path.substring(path.indexOf("/" + this.config.getProperty("share") + "/") + this.config.getProperty("share").length() + 2, path.length()); 
      restype = "maxresults=5000&prefix=" + prefix.replace("/", "%2F") + "&restype=container";
      String url_path = "/" + this.config.getProperty("share") + "/";
      URLConnection uRLConnection = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + url_path + "?comp=list&" + restype + getSASToken()), new Properties());
      uRLConnection.setRequestMethod("GET");
      uRLConnection.setDoInput(true);
      uRLConnection.setDoOutput(true);
      uRLConnection.setUseCaches(false);
      signRequestSK(uRLConnection);
      uRLConnection.connect();
      uRLConnection.getResponseCode();
      String str1 = URLConnection.consumeResponse(uRLConnection.getInputStream());
      parseBlobList(prefix, path, list, str1);
      Common.log("AZURE_CLIENT", 1, "Blob folder delete : Searching for sub items. List size : " + list.size());
      for (int i = 0; i < list.size(); i++) {
        Properties pp = list.get(i);
        Common.log("AZURE_CLIENT", 2, "Blob folder delete : Delete blob : " + pp.getProperty("path"));
        String d_path = pp.getProperty("path");
        if (pp.getProperty("type").equals("DIR") && !d_path.endsWith("/"))
          d_path = String.valueOf(d_path) + "/"; 
        if (!getSASToken().equals("") && commands.equals(""))
          commands = "?" + getSASToken().substring(1); 
        URLConnection urlc2 = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + d_path + commands), new Properties());
        urlc2.setRequestMethod("DELETE");
        urlc2.setDoInput(true);
        urlc2.setUseCaches(false);
        signRequestSK(urlc2);
        uRLConnection.connect();
        if (urlc2.getResponseCode() != 202) {
          Common.log("AZURE_CLIENT", 1, "Blob folder delete : Could not delete blob : " + pp.getProperty("path"));
          Common.log("AZURE_CLIENT", 1, "Blob folder delete : Respond message : " + urlc2.getResponseMessage());
          Common.log("AZURE_CLIENT", 1, "Blob folder delete : Result : " + URLConnection.consumeResponse(urlc2.getInputStream()));
          log("Blob folder delete : Could not delete blob : " + pp.getProperty("path"));
          return false;
        } 
      } 
    } 
    if (!getSASToken().equals(""))
      if (commands.equals("")) {
        commands = "?" + getSASToken().substring(1);
      } else if (!commands.contains(getSASToken().substring(1))) {
        commands = String.valueOf(commands) + getSASToken();
      }  
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + path + commands), new Properties());
    urlc.setRequestMethod("DELETE");
    urlc.setDoInput(true);
    urlc.setUseCaches(false);
    signRequestSK(urlc);
    urlc.connect();
    if (urlc.getResponseCode() == 202)
      return true; 
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    Common.log("AZURE_CLIENT", 2, String.valueOf(urlc.getResponseCode()) + " " + result);
    return false;
  }
  
  public boolean makedirs(String path) throws Exception {
    boolean ok = true;
    String[] parts = path.split("/");
    String path2 = "";
    if (parts.length < 2 && !this.config.getProperty("share").equals(parts[1]))
      return false; 
    String share_part = "/" + parts[1] + "/";
    for (int x = 2; x < parts.length && ok; x++) {
      path2 = String.valueOf(path2) + parts[x] + "/";
      if (x >= 1)
        if (stat(String.valueOf(share_part) + path2) == null)
          ok = makedir(String.valueOf(share_part) + path2);  
    } 
    return ok;
  }
  
  public boolean makedir(String path) throws Exception {
    if (this.config.getProperty("container_type").equals("share")) {
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + ".file.core.windows.net" + path + "?restype=directory" + getSASToken()), new Properties());
      urlc.setRequestMethod("PUT");
      urlc.setRequestProperty("Content-Length", "0");
      urlc.setDoOutput(true);
      urlc.setUseCaches(false);
      try {
        signRequestSK(urlc);
      } catch (Exception e1) {
        log(e1);
        Common.log("AZURE_CLIENT", 1, e1);
        return false;
      } 
      String result = "";
      try {
        result = Common.consumeResponse(urlc.getInputStream());
      } catch (Exception e2) {
        Common.log("AZURE_CLIENT", 1, e2);
        return false;
      } 
      if (urlc.getResponseCode() == 201)
        return true; 
      Common.log("AZURE_CLIENT", 2, String.valueOf(urlc.getResponseCode()) + " " + result);
      return false;
    } 
    if (this.config.getProperty("container_type").equals("blob"))
      if (this.config.getProperty("upload_blob_type", "appendblob").equals("blockblob")) {
        String path2 = path;
        if (!path.startsWith("/"))
          path2 = "/" + path; 
        String upload_path = path2;
        String params = "";
        if (!getSASToken().equals(""))
          params = "?" + getSASToken().substring(1); 
        String addition_name = "";
        if (this.config.getProperty("data_lake_storagegen2", "false").equals("true"))
          addition_name = "_empty_"; 
        URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + upload_path + addition_name + params), new Properties());
        urlc.setRequestMethod("PUT");
        urlc.setRequestProperty("x-ms-content-length", "0");
        urlc.setRequestProperty("Content-Length", "0");
        urlc.setRequestProperty("x-ms-blob-type", "BlockBlob");
        urlc.setDoOutput(true);
        urlc.setUseCaches(false);
        signRequestSK(urlc);
        urlc.connect();
        String result = URLConnection.consumeResponse(urlc.getInputStream());
        if (urlc.getResponseCode() != 201) {
          Common.log("AZURE_CLIENT", 2, String.valueOf(urlc.getResponseCode()) + " " + result);
          return false;
        } 
        urlc.disconnect();
        if (this.config.getProperty("data_lake_storagegen2", "false").equals("true")) {
          String commands = "";
          if (!getSASToken().equals("") && commands.equals(""))
            commands = "?" + getSASToken().substring(1); 
          URLConnection urlc2 = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + upload_path + addition_name + commands), new Properties());
          urlc2.setRequestMethod("DELETE");
          urlc2.setDoInput(true);
          urlc2.setUseCaches(false);
          signRequestSK(urlc2);
          urlc2.connect();
          if (urlc2.getResponseCode() != 202) {
            Common.log("AZURE_CLIENT", 1, "Blob folder delete : Respond message : " + urlc2.getResponseMessage());
            Common.log("AZURE_CLIENT", 1, "Blob folder delete : Result : " + URLConnection.consumeResponse(urlc2.getInputStream()));
            log("Blob folder delete : Respond message : " + urlc2.getResponseMessage());
            return false;
          } 
        } 
      } else {
        String path2 = path;
        if (!path.startsWith("/"))
          path2 = "/" + path; 
        String upload_path = path2;
        String params = "";
        if (!getSASToken().equals(""))
          params = "?" + getSASToken().substring(1); 
        URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + upload_path + params), new Properties());
        urlc.setRequestMethod("PUT");
        urlc.setRequestProperty("x-ms-content-length", "0");
        urlc.setRequestProperty("Content-Length", "0");
        urlc.setRequestProperty("x-ms-blob-type", "AppendBlob");
        urlc.setDoOutput(true);
        urlc.setUseCaches(false);
        signRequestSK(urlc);
        urlc.connect();
        String result = URLConnection.consumeResponse(urlc.getInputStream());
        if (urlc.getResponseCode() != 201) {
          Common.log("AZURE_CLIENT", 2, String.valueOf(urlc.getResponseCode()) + " " + result);
          log("Makedir Error: " + urlc.getResponseCode() + " " + result);
          return false;
        } 
        urlc.disconnect();
        return true;
      }  
    return false;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    String restype = "";
    if (this.config.getProperty("container_type").equals("blob"))
      restype = ""; 
    if (this.config.getProperty("container_type").equals("share")) {
      Properties properties = stat(path);
      if (properties.getProperty("type").equals("FILE")) {
        restype = "&restype=file";
      } else {
        restype = "&restype=directory";
      } 
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + path + "?comp=metadata" + restype + getSASToken()), new Properties());
    urlc.setRequestMethod("PUT");
    SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
    fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
    String date = String.valueOf(fmt.format(new Date(modified))) + " GMT";
    urlc.setRequestProperty("Last-Modified", date);
    urlc.setRequestProperty("Content-Length", "0");
    Properties p = getMetadata(path);
    if (p.containsKey("X-MS-META-UPLOADED_BY")) {
      urlc.setRequestProperty("x-ms-meta-uploaded_by", p.getProperty("X-MS-META-UPLOADED_BY", ""));
    } else if (!this.config.getProperty("uploaded_by", "").equals("")) {
      urlc.setRequestProperty("x-ms-meta-uploaded_by", this.config.getProperty("uploaded_by", ""));
    } 
    if (p.containsKey("X-MS-META-MD5")) {
      urlc.setRequestProperty("x-ms-meta-md5", p.getProperty("X-MS-META-MD5", ""));
    } else if (!this.config.getProperty("uploaded_md5", "").equals("")) {
      urlc.setRequestProperty("x-ms-meta-md5", this.config.getProperty("uploaded_md5", ""));
    } 
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    try {
      signRequestSK(urlc);
    } catch (Exception e1) {
      log(e1);
      Common.log("AZURE_CLIENT", 1, e1);
      return false;
    } 
    String result = "";
    try {
      result = Common.consumeResponse(urlc.getInputStream());
    } catch (Exception e2) {
      log(e2);
      Common.log("AZURE_CLIENT", 1, e2);
      return false;
    } 
    if (urlc.getResponseCode() == 200 || urlc.getResponseCode() == 202) {
      urlc.disconnect();
      return true;
    } 
    Common.log("AZURE_CLIENT", 2, String.valueOf(urlc.getResponseCode()) + " " + result);
    log("MDTM Error: " + urlc.getResponseCode() + " " + result);
    return false;
  }
  
  public void set_MD5_and_upload_id(String path) throws Exception {
    String restype = "";
    if (this.config.getProperty("container_type").equals("blob"))
      restype = ""; 
    if (this.config.getProperty("container_type").equals("share")) {
      Properties properties = stat(path);
      if (properties.getProperty("type").equals("FILE")) {
        restype = "&restype=file";
      } else {
        restype = "&restype=directory";
      } 
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + path + "?comp=metadata" + restype + getSASToken()), new Properties());
    urlc.setRequestMethod("PUT");
    urlc.setRequestProperty("Content-Length", "0");
    Properties p = getMetadata(path);
    if (p.containsKey("X-MS-META-UPLOADED_BY")) {
      urlc.setRequestProperty("x-ms-meta-uploaded_by", p.getProperty("X-MS-META-UPLOADED_BY", ""));
    } else if (!this.config.getProperty("uploaded_by", "").equals("")) {
      urlc.setRequestProperty("x-ms-meta-uploaded_by", this.config.getProperty("uploaded_by", ""));
    } 
    if (p.containsKey("X-MS-META-MD5")) {
      urlc.setRequestProperty("x-ms-meta-md5", p.getProperty("X-MS-META-MD5", ""));
    } else if (!this.config.getProperty("uploaded_md5", "").equals("")) {
      urlc.setRequestProperty("x-ms-meta-md5", this.config.getProperty("uploaded_md5", ""));
    } 
    urlc.setDoOutput(true);
    urlc.setUseCaches(false);
    try {
      signRequestSK(urlc);
    } catch (Exception e1) {
      log(e1);
      Common.log("AZURE_CLIENT", 1, e1);
    } 
    String result = "";
    try {
      result = Common.consumeResponse(urlc.getInputStream());
    } catch (Exception e2) {
      log(e2);
      Common.log("AZURE_CLIENT", 1, e2);
    } 
    if (urlc.getResponseCode() == 200 || urlc.getResponseCode() == 202)
      urlc.disconnect(); 
    Common.log("AZURE_CLIENT", 2, String.valueOf(urlc.getResponseCode()) + result);
  }
  
  public Properties getMetadata(String path) {
    Properties p = new Properties();
    try {
      String restype = "";
      if (this.config.getProperty("container_type").equals("blob"))
        restype = ""; 
      if (this.config.getProperty("container_type").equals("share")) {
        p = stat(path);
        if (p.getProperty("type").equals("FILE")) {
          restype = "&restype=file";
        } else {
          restype = "&restype=directory";
        } 
      } 
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.config.getProperty("username") + getUrl() + path + "?comp=metadata" + restype + getSASToken()), new Properties());
      urlc.setRequestMethod("GET");
      SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss");
      fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
      String date = String.valueOf(fmt.format(new Date())) + " GMT";
      urlc.setRequestProperty("Last-Modified", date);
      urlc.setDoOutput(true);
      urlc.setUseCaches(false);
      try {
        signRequestSK(urlc);
      } catch (Exception e1) {
        log(e1);
        Common.log("AZURE_CLIENT", 1, e1);
      } 
      String result = Common.consumeResponse(urlc.getInputStream());
      if (urlc.getResponseCode() == 200 || urlc.getResponseCode() == 202) {
        p = (Properties)urlc.headers.clone();
        urlc.disconnect();
      } 
      Common.log("AZURE_CLIENT", 2, String.valueOf(urlc.getResponseCode()) + " " + result);
    } catch (Exception e) {
      Common.log("AZURE_CLIENT", 1, e);
    } 
    return p;
  }
  
  public String getUploadedByMetadata(String path) {
    Properties p = null;
    try {
      p = getMetadata(path);
    } catch (Exception e) {
      Common.log("AZURE_CLIENT", 1, e);
    } 
    if (p != null && p.containsKey("X-MS-META-UPLOADED_BY") && !p.getProperty("X-MS-META-UPLOADED_BY").equals(""))
      return p.getProperty("X-MS-META-UPLOADED_BY"); 
    return "";
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    Properties p = stat(rnfr);
    if (p.getProperty("type").equals("FILE")) {
      InputStream rnin = download3(rnfr, 0L, Long.parseLong(p.getProperty("size")), true);
      OutputStream rnout = upload3(rnto, 0L, true, true);
      try {
        byte[] b = new byte[32768];
        int bytesRead = 0;
        while (bytesRead >= 0) {
          bytesRead = rnin.read(b);
          if (bytesRead > 0)
            rnout.write(b, 0, bytesRead); 
        } 
      } catch (Exception e) {
        Common.log("AZURE_CLIENT", 1, e);
        throw e;
      } finally {
        rnin.close();
        rnout.close();
      } 
      return delete(rnfr);
    } 
    Common.log("AZURE_CLIENT", 0, "Azure does not support rename functionality for Folders!");
    throw new Exception("Azure does not support rename functionality for Folders!");
  }
  
  public void signRequestSK(URLConnection urlc) throws Exception {
    String ms_version = "2015-12-11";
    if (this.config.getProperty("container_type").equals("blob"))
      ms_version = "2019-02-02"; 
    urlc.setReadTimeout(Integer.parseInt(this.config.getProperty("timeout", "60")) * 1000);
    urlc.setRequestProperty("x-ms-version", ms_version);
    if (!getSASToken().equals("")) {
      urlc.setRemoveDoubleEncoding(true);
      return;
    } 
    String date = urlc.sdf_rfc1123.format(urlc.getDate());
    String sb = "";
    sb = String.valueOf(sb) + urlc.getRequestMethod() + "\n";
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    if (urlc.getRequestProps().getProperty("Content-Length") != null && !urlc.getRequestProps().getProperty("Content-Length").equals("0")) {
      sb = String.valueOf(sb) + urlc.getRequestProps().getProperty("Content-Length") + "\n";
    } else {
      sb = String.valueOf(sb) + "\n";
    } 
    sb = String.valueOf(sb) + "\n";
    if (urlc.getRequestProps().getProperty("Content-Type") != null && !urlc.getRequestProps().getProperty("Content-Type").equals("0")) {
      sb = String.valueOf(sb) + urlc.getRequestProps().getProperty("Content-Type") + "\n";
    } else {
      sb = String.valueOf(sb) + "application/x-www-form-urlencoded; charset=UTF-8\n";
    } 
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    sb = String.valueOf(sb) + "\n";
    String range = "\n";
    if (urlc.getRequestProps().getProperty("Range") != null)
      range = String.valueOf(urlc.getRequestProps().getProperty("Range")) + "\n"; 
    sb = String.valueOf(sb) + range;
    if (urlc.getRequestProps().getProperty("x-ms-blob-content-length") != null)
      sb = String.valueOf(sb) + "x-ms-blob-content-length:" + urlc.getRequestProps().getProperty("x-ms-blob-content-length") + "\n"; 
    if (urlc.getRequestProps().getProperty("x-ms-blob-content-type") != null)
      sb = String.valueOf(sb) + "x-ms-blob-content-type:" + urlc.getRequestProps().getProperty("x-ms-blob-content-type") + "\n"; 
    if (urlc.getRequestProps().getProperty("x-ms-blob-type") != null)
      sb = String.valueOf(sb) + "x-ms-blob-type:" + urlc.getRequestProps().getProperty("x-ms-blob-type") + "\n"; 
    if (urlc.getRequestProps().getProperty("x-ms-content-length") != null)
      sb = String.valueOf(sb) + "x-ms-content-length:" + urlc.getRequestProps().getProperty("x-ms-content-length") + "\n"; 
    sb = String.valueOf(sb) + "x-ms-date:" + date + "\n";
    if (urlc.getRequestProps().getProperty("x-ms-meta-md5") != null)
      sb = String.valueOf(sb) + "x-ms-meta-md5:" + urlc.getRequestProps().getProperty("x-ms-meta-md5") + "\n"; 
    if (urlc.getRequestProps().getProperty("x-ms-meta-uploaded_by") != null)
      sb = String.valueOf(sb) + "x-ms-meta-uploaded_by:" + urlc.getRequestProps().getProperty("x-ms-meta-uploaded_by") + "\n"; 
    if (urlc.getRequestProps().getProperty("x-ms-page-write") != null)
      sb = String.valueOf(sb) + "x-ms-page-write:" + urlc.getRequestProps().getProperty("x-ms-page-write") + "\n"; 
    if (urlc.getRequestProps().getProperty("x-ms-range") != null)
      sb = String.valueOf(sb) + "x-ms-range:" + urlc.getRequestProps().getProperty("x-ms-range") + "\n"; 
    if (urlc.getRequestProps().getProperty("x-ms-type") != null)
      sb = String.valueOf(sb) + "x-ms-type:" + urlc.getRequestProps().getProperty("x-ms-type") + "\n"; 
    sb = String.valueOf(sb) + "x-ms-version:" + ms_version + "\n";
    if (urlc.getRequestProps().getProperty("x-ms-write") != null)
      sb = String.valueOf(sb) + "x-ms-write:" + urlc.getRequestProps().getProperty("x-ms-write") + "\n"; 
    String path = urlc.getURL().getPath();
    if (path.contains("?")) {
      String path2 = Common.url_encode(path.substring(0, path.indexOf("?")), "/%.#@&!\\=+");
      if (urlc.getRemoveDoubleEncoding())
        path2 = URLConnection.remove_double_encoding_of_special_chars(path2); 
      sb = String.valueOf(sb) + "/" + this.config.getProperty("username") + path2 + "\n";
      String commands = path.substring(path.indexOf("?") + 1, path.length());
      commands = commands.replaceAll("&", "\n").replaceAll("=", ":");
      if (urlc.getRemoveDoubleEncoding())
        commands = Common.url_decode(commands); 
      sb = String.valueOf(sb) + commands;
    } else {
      sb = String.valueOf(sb) + "/" + this.config.getProperty("username") + Common.url_encode(path, "/%.#@&!\\=+");
    } 
    Common.log("AZURE_CLIENT", 2, "Signing header : " + sb.replace("\n", " "));
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(Base64.decode(this.config.getProperty("password")), "HmacSHA256"));
    String authKey = new String(Base64.encodeBytes(mac.doFinal(sb.toString().getBytes("UTF-8"))));
    String auth = "SharedKey " + this.config.getProperty("username") + ":" + authKey;
    urlc.setRequestProperty("x-ms-date", date);
    urlc.setRequestProperty("Authorization", auth);
  }
  
  private String getUrl() {
    String url = ".file.core.windows.net";
    if (this.config.getProperty("container_type").equals("blob"))
      url = ".blob.core.windows.net"; 
    return url;
  }
  
  private String getSASToken() {
    String token = this.config.getProperty("sas_token", "");
    if (token.startsWith("?"))
      token = "&" + token.substring(1); 
    if (!token.equals("") && !token.startsWith("&"))
      token = "&" + token; 
    if (token.contains("sig=")) {
      String sig = token.substring(token.indexOf("sig=") + 4, (token.indexOf("&", token.indexOf("sig=") + 4) > 0) ? token.indexOf("&", token.indexOf("sig=") + 4) : token.length());
      String sig_double_encoded = sig;
      sig_double_encoded = double_encode(sig_double_encoded);
      token = Common.replace_str(token, sig, sig_double_encoded);
    } 
    return token;
  }
  
  private String double_encode(String text) {
    if (text.contains("/"))
      text = text.replace("/", "%252F"); 
    if (text.contains("+"))
      text = text.replace("+", "%252B"); 
    if (text.contains("="))
      text = text.replace("=", "%253D"); 
    if (text.contains("#"))
      text = text.replace("#", "%2523"); 
    if (text.contains("~"))
      text = text.replace("~", "%257E"); 
    if (text.contains("!"))
      text = text.replace("!", "%2521"); 
    if (text.contains("\\"))
      text = text.replace("\\", "%255C"); 
    if (text.contains("%2F"))
      text = text.replace("%2F", "%252F"); 
    if (text.contains("%2B"))
      text = text.replace("%2B", "%252B"); 
    if (text.contains("%3D"))
      text = text.replace("%3D", "%253D"); 
    if (text.contains("%23"))
      text = text.replace("%23", "%2523"); 
    if (text.contains("%7E"))
      text = text.replace("%7E", "%257E"); 
    if (text.contains("%21"))
      text = text.replace("%21", "%2521"); 
    if (text.contains("%5C"))
      text = text.replace("%5C", "%255C"); 
    return text;
  }
}
