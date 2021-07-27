package com.crushftp.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class GDriveClient extends GenericClient {
  SimpleDateFormat yyyyMMddtHHmmssSSS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US);
  
  SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  
  String authToken = "";
  
  static Properties resourceIdCache = new Properties();
  
  static Properties authTokenHash = new Properties();
  
  public GDriveClient(String url, String header, Vector log) {
    super(header, log);
    System.setProperty("crushtunnel.debug", "2");
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    this.config.put("username", username);
    this.config.put("password", password);
    String hash = Common.getMD5(new ByteArrayInputStream((String.valueOf(username) + password).getBytes("UTF8")));
    this.authToken = authTokenHash.getProperty(hash, "");
    if (!this.authToken.equals(""))
      return "skipped"; 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.google.com/accounts/ClientLogin?service=writely&accountType=GOOGLE&Email=" + Common.url_encode(username) + "&Passwd=" + Common.url_encode(password)), this.config);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299 || (result != null && result.toUpperCase().startsWith("ERROR="))) {
      log(String.valueOf(result) + "\r\n");
      throw new IOException(result);
    } 
    BufferedReader br = new BufferedReader(new StringReader(result));
    String data = "";
    while ((data = br.readLine()) != null) {
      if (data.toUpperCase().startsWith("AUTH="))
        this.authToken = data.substring(5).trim(); 
    } 
    authTokenHash.put(hash, this.authToken);
    authTokenHash.put(this.authToken, hash);
    return result;
  }
  
  public void logout() throws Exception {
    String hash = (String)authTokenHash.remove(this.authToken);
    if (hash != null)
      authTokenHash.remove(hash); 
    close();
  }
  
  public Vector list(String path, Vector list) throws Exception {
    String resourceId = getResourceId(findItemInfo(path));
    if (resourceId == null) {
      log("List path not found:" + path + "\r\n");
    } else {
      String query = "/" + Common.replace_str(resourceId, ":", "%3A") + "/contents?v=3&showfolders=true&showdeleted=false";
      URLConnection urlc = URLConnection.openConnection(new VRL("https://docs.google.com/feeds/default/private/full" + query), this.config);
      urlc.setDoOutput(false);
      doStandardDocsAlterations(urlc, (String)null);
      int code = urlc.getResponseCode();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        throw new IOException(result);
      } 
      if (result.length() == 0)
        return list; 
      processList(result, path, list, urlc);
    } 
    return list;
  }
  
  public Properties findItemInfo(String path) throws Exception {
    if (path.equals(""))
      path = "/"; 
    Properties last_p = (Properties)resourceIdCache.get(String.valueOf(this.authToken) + path);
    if (last_p != null) {
      if (System.currentTimeMillis() - Long.parseLong(last_p.getProperty("cache_time", "0")) < 30000L)
        return last_p; 
      last_p = null;
    } 
    if (last_p == null)
      last_p = new Properties(); 
    String[] parts = path.split("/");
    last_p.put("url", "gdrive://user:pass@folder%3Aroot/");
    boolean found = false;
    for (int x = 0; x < parts.length; x++) {
      if (!parts[x].equals("")) {
        String query = "/" + Common.replace_str(getResourceId(last_p), ":", "%3A") + "/contents?v=3&showfolders=true&showdeleted=false";
        URLConnection urlc = URLConnection.openConnection(new VRL("https://docs.google.com/feeds/default/private/full" + query), this.config);
        urlc.setDoOutput(false);
        doStandardDocsAlterations(urlc, (String)null);
        int code = urlc.getResponseCode();
        String result = URLConnection.consumeResponse(urlc.getInputStream());
        urlc.disconnect();
        if (code < 200 || code > 299) {
          log(String.valueOf(result) + "\r\n");
          throw new IOException(result);
        } 
        Vector v = new Vector();
        processList(result, path, v, urlc);
        for (int xx = 0; xx < v.size(); xx++) {
          Properties p = v.elementAt(xx);
          if (p.getProperty("name").equalsIgnoreCase(parts[x])) {
            if (x == parts.length - 1)
              found = true; 
            last_p = p;
          } 
        } 
      } 
    } 
    if (!found && !path.equals("/"))
      return null; 
    resourceIdCache.put(String.valueOf(this.authToken) + path, last_p);
    last_p.put("cache_time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    return last_p;
  }
  
  public String getResourceId(Properties p) {
    if (p == null)
      return null; 
    return VRL.vrlDecode((new VRL(p.getProperty("url"))).getHost());
  }
  
  public Vector processList(String result, String path, Vector list, URLConnection urlc) throws Exception {
    Element root = (new SAXBuilder()).build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
    List entries = getElements(root, "entry");
    for (int x = 0; x < entries.size(); x++) {
      Element entry = entries.get(x);
      String name = getKeyText(entry, "title");
      log(String.valueOf(name) + "\r\n");
      String resourceId = getKeyText(entry, "resourceId");
      boolean folder = resourceId.toLowerCase().startsWith("folder");
      Date d = this.yyyyMMddtHHmmssSSS.parse(getKeyText(entry, "updated"));
      String size = getKeyText(entry, "size");
      if (size == null)
        size = "0"; 
      if (!folder && name.indexOf(".") < 0)
        name = String.valueOf(name) + ".pdf"; 
      String line = String.valueOf(folder ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + size + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + name;
      Properties stat = parseStat(line);
      stat.put("url", "gdrive://" + VRL.vrlEncode(this.config.getProperty("username")) + ":" + VRL.vrlEncode(this.config.getProperty("password")) + "@" + VRL.vrlEncode(resourceId) + path + name);
      stat.put("content.src", getElement(entry, "content").getAttributeValue("src"));
      log(stat + "\r\n");
      list.addElement(stat);
    } 
    return list;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    Properties p = findItemInfo(path);
    if (p == null) {
      log("Download path not found:" + path + "\r\n");
      throw new IOException("Download path not found:" + path);
    } 
    String content_src = p.getProperty("content.src");
    URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(content_src) + "&format=pdf"), this.config);
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
    String resourceId = getResourceId(findItemInfo(Common.all_but_last(path)));
    if (resourceId == null) {
      log("Upload path not found:" + Common.all_but_last(path) + "\r\n");
      throw new IOException("Upload path not found:" + Common.all_but_last(path));
    } 
    try {
      delete(path);
    } catch (Exception exception) {}
    String query = "/" + Common.replace_str(resourceId, ":", "%3A") + "/contents?v=3&convert=false";
    URLConnection urlc = URLConnection.openConnection(new VRL("https://docs.google.com/feeds/upload/create-session/default/private/full" + query), this.config);
    urlc.setDoOutput(false);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("GData-Version", "3.0");
    urlc.setRequestProperty("Slug", Common.last(path));
    urlc.setLength(0L);
    doStandardDocsAlterations(urlc, "application/binary");
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      throw new IOException(result);
    } 
    String postLocation = urlc.getHeaderField("Location");
    URLConnection urlc2 = URLConnection.openConnection(new VRL(postLocation), this.config);
    urlc2.setRequestMethod("PUT");
    urlc2.setRequestProperty("GData-Version", "3.0");
    urlc2.setRequestProperty("Slug", Common.last(path));
    urlc2.setDoOutput(true);
    doStandardDocsAlterations(urlc2, "application/binary");
    this.out = new GDriveClient$1$OutputWrapper(this, urlc2.getOutputStream(), urlc2);
    return this.out;
  }
  
  public boolean delete(String path) throws Exception {
    String resourceId = getResourceId(findItemInfo(path));
    if (resourceId == null) {
      log("Delete path not found:" + path + "\r\n");
      throw new IOException("Delete path not found:" + path);
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://docs.google.com/feeds/default/private/full/" + resourceId), this.config);
    urlc.setDoOutput(false);
    urlc.setRequestMethod("DELETE");
    urlc.setRequestProperty("GData-Version", "3.0");
    urlc.setRequestProperty("If-Match", "*");
    urlc.setLength(0L);
    doStandardDocsAlterations(urlc, (String)null);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      return false;
    } 
    return true;
  }
  
  public boolean makedir(String path) throws Exception {
    Properties p = findItemInfo(Common.all_but_last(path));
    URLConnection urlc = URLConnection.openConnection(new VRL("https://docs.google.com/feeds/default/private/full/" + Common.url_decode(getResourceId(p)) + "/contents"), this.config);
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("GData-Version", "3.0");
    doStandardDocsAlterations(urlc, "application/atom+xml");
    String full_xml = "<?xml version='1.0' encoding='UTF-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\">";
    full_xml = String.valueOf(full_xml) + "<category scheme=\"http://schemas.google.com/g/2005#kind\" term=\"http://schemas.google.com/docs/2007#folder\"/>";
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1); 
    full_xml = String.valueOf(full_xml) + "  <title>" + Common.last(path) + "</title>";
    full_xml = String.valueOf(full_xml) + "</entry>";
    byte[] b = full_xml.getBytes("UTF8");
    this.out = urlc.getOutputStream();
    this.out.write(b);
    this.out.close();
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
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
      if (x >= 1) {
        Properties p = findItemInfo(path2);
        if (p == null)
          ok = makedir(path2); 
      } 
    } 
    return ok;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    boolean rename_all_ok = true;
    boolean same_dir = Common.all_but_last(rnfr).equalsIgnoreCase(Common.all_but_last(rnto));
    boolean same_name = Common.last(rnfr).equalsIgnoreCase(Common.last(rnto));
    Properties rnfr_p = findItemInfo(rnfr);
    String resourceId_rnfr = getResourceId(rnfr_p);
    Properties rnto_p = findItemInfo(Common.all_but_last(rnto));
    if (rnto_p == null) {
      log("Rename destination path not found:" + Common.all_but_last(rnto));
      rename_all_ok = false;
    } 
    String resourceId_rnto = getResourceId(rnto_p);
    if (!same_name && rename_all_ok) {
      URLConnection urlc = URLConnection.openConnection(new VRL("https://docs.google.com/feeds/default/private/full/" + Common.url_decode(resourceId_rnfr)), this.config);
      urlc.setDoOutput(true);
      urlc.setRequestMethod("PUT");
      urlc.setRequestProperty("GData-Version", "3.0");
      urlc.setRequestProperty("If-Match", "*");
      doStandardDocsAlterations(urlc, "application/atom+xml");
      String full_xml = "<?xml version='1.0' encoding='UTF-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\">";
      full_xml = String.valueOf(full_xml) + "<title>" + Common.last(rnto) + "</title>";
      full_xml = String.valueOf(full_xml) + "</entry>";
      byte[] b = full_xml.getBytes("UTF8");
      OutputStream out = urlc.getOutputStream();
      out.write(b);
      out.close();
      int code = urlc.getResponseCode();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        rename_all_ok = false;
      } 
    } 
    if (rename_all_ok) {
      URLConnection urlc = URLConnection.openConnection(new VRL("https://docs.google.com/feeds/default/private/full/" + Common.url_decode(resourceId_rnto) + "/contents"), this.config);
      urlc.setDoOutput(true);
      urlc.setRequestMethod("POST");
      urlc.setRequestProperty("GData-Version", "3.0");
      doStandardDocsAlterations(urlc, "application/atom+xml");
      String full_xml = "<?xml version='1.0' encoding='UTF-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\">";
      full_xml = String.valueOf(full_xml) + "  <id>https://docs.google.com/feeds/default/private/full/" + resourceId_rnfr + "</id>";
      full_xml = String.valueOf(full_xml) + "</entry>";
      byte[] b = full_xml.getBytes("UTF8");
      this.out = urlc.getOutputStream();
      this.out.write(b);
      this.out.close();
      int code = urlc.getResponseCode();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        rename_all_ok = false;
      } 
    } 
    if (!same_dir && rename_all_ok) {
      Properties rnfr_parent_p = findItemInfo(Common.all_but_last(rnfr));
      String resourceId_rnfr_parent = getResourceId(rnfr_parent_p);
      URLConnection urlc = URLConnection.openConnection(new VRL("https://docs.google.com/feeds/default/private/full/" + resourceId_rnfr_parent + "/contents/" + resourceId_rnfr), this.config);
      urlc.setDoOutput(false);
      urlc.setRequestMethod("DELETE");
      urlc.setRequestProperty("GData-Version", "3.0");
      urlc.setRequestProperty("Content-length", "0");
      urlc.setRequestProperty("If-Match", "*");
      doStandardDocsAlterations(urlc, "application/atom+xml");
      int code = urlc.getResponseCode();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        rename_all_ok = false;
      } 
    } 
    return rename_all_ok;
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
    String resourceId = getResourceId(findItemInfo(path));
    if (resourceId == null) {
      log("MDTM path not found:" + path + "\r\n");
      throw new IOException("MDTM path not found:" + path);
    } 
    URLConnection urlc = URLConnection.openConnection(new VRL("https://docs.google.com/feeds/default/private/full/" + Common.url_decode(resourceId)), this.config);
    urlc.setDoOutput(true);
    urlc.setRequestMethod("PUT");
    urlc.setRequestProperty("GData-Version", "3.0");
    urlc.setRequestProperty("If-Match", "*");
    doStandardDocsAlterations(urlc, "application/atom+xml");
    String full_xml = "<?xml version='1.0' encoding='UTF-8'?><entry xmlns=\"http://www.w3.org/2005/Atom\">";
    full_xml = String.valueOf(full_xml) + "<updated>" + this.yyyyMMddtHHmmssSSS.format(new Date(modified)) + "</updated>";
    full_xml = String.valueOf(full_xml) + "</entry>";
    byte[] b = full_xml.getBytes("UTF8");
    OutputStream out = urlc.getOutputStream();
    out.write(b);
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
    urlc.setRequestProperty("Authorization", "GoogleLogin auth=" + this.authToken);
    urlc.setUseCaches(false);
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
}
