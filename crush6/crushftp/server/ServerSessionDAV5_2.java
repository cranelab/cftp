package crushftp.server;

import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import java.io.StringReader;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.Vector;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class ServerSessionDAV5_2 {
  ServerSession thisSession = null;
  
  ServerSessionHTTP5_2 thisSessionHTTP = null;
  
  SimpleDateFormat sdf_rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  
  SimpleDateFormat sdf_rfc1123_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
  
  SAXBuilder sax = new SAXBuilder();
  
  XMLOutputter xmlOut = null;
  
  public ServerSessionDAV5_2(ServerSession thisSession, ServerSessionHTTP5_2 thisSessionHTTP) {
    this.thisSession = thisSession;
    this.thisSessionHTTP = thisSessionHTTP;
    this.sdf_rfc1123.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT")));
  }
  
  public String doPropFind(Properties commandActions, String depth, boolean listProps, Vector fieldOrder) throws Exception {
    if (!this.thisSession.uiSG("current_dir").toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
      this.thisSession.uiPUT("current_dir", String.valueOf(this.thisSession.SG("root_dir")) + (this.thisSession.uiSG("current_dir").startsWith("/") ? this.thisSession.uiSG("current_dir").substring(1) : this.thisSession.uiSG("current_dir"))); 
    Namespace D = Namespace.getNamespace("D", "DAV:");
    Namespace ns0 = Namespace.getNamespace("ns0", "DAV:");
    Namespace lp1 = Namespace.getNamespace("lp1", "DAV:");
    Namespace lp2 = Namespace.getNamespace("lp2", "http://apache.org/dav/props/");
    Namespace g0 = Namespace.getNamespace("g0", "DAV:");
    Element root = new Element("multistatus", D);
    root.addNamespaceDeclaration(ns0);
    Document doc = new Document(root);
    Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
    if (item == null)
      return null; 
    if (item.getProperty("type").toUpperCase().equals("DIR") && !this.thisSession.uiSG("current_dir").endsWith("/"))
      this.thisSession.uiPUT("current_dir", String.valueOf(this.thisSession.uiSG("current_dir")) + "/"); 
    Vector items = new Vector();
    if (!depth.equals("0")) {
      if (depth.indexOf("inf") >= 0) {
        this.thisSession.uVFS.getListing(items, this.thisSession.uiSG("current_dir"), 999, 10000, true);
      } else if (depth.equals("1")) {
        this.thisSession.uVFS.getListing(items, this.thisSession.uiSG("current_dir"));
      } 
      Properties p = new Properties();
      p.put("listing", items);
      this.thisSession.runPlugin("list", p);
    } 
    items.insertElementAt(item, 0);
    Vector hrefs = new Vector();
    boolean listAdditionalProps = false;
    for (int x = 0; x < items.size(); x++) {
      item = items.elementAt(x);
      String parentPrivs = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir")).getProperty("privs", "");
      if (item.getProperty("privs").toLowerCase().indexOf("(invisible)") < 0 && parentPrivs.toLowerCase().indexOf("(view)") >= 0)
        if (Common.filter_check("L", item.getProperty("name"), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSession.SG("file_filter"))) {
          Element href = new Element("href", D);
          String dir = this.thisSession.uiSG("current_dir");
          if (!this.thisSession.SG("root_dir").equals("/"))
            dir = dir.substring(this.thisSession.SG("root_dir").length() - 1); 
          if (!depth.equals("0") && x > 0) {
            href.setText(String.valueOf(Common.url_encode(dir, "/")) + Common.url_encode(item.getProperty("name", "")));
          } else {
            href.setText(Common.url_encode(dir, "/"));
          } 
          if (depth.indexOf("inf") >= 0) {
            dir = item.getProperty("root_dir");
            if (dir.startsWith(this.thisSessionHTTP.SG("root_dir")))
              dir = dir.substring(this.thisSessionHTTP.SG("root_dir").length() - 1); 
            href.setText(String.valueOf(Common.url_encode(dir, "/")) + Common.url_encode(item.getProperty("name", "")));
          } 
          if (!href.getText().endsWith("/") && item.getProperty("type").toUpperCase().equals("DIR"))
            href.setText(String.valueOf(href.getText()) + "/"); 
          if (hrefs.indexOf(href.getText()) < 0) {
            hrefs.addElement(href.getText());
            Element response = new Element("response", D);
            response.addNamespaceDeclaration(lp1);
            response.addNamespaceDeclaration(lp2);
            response.addNamespaceDeclaration(g0);
            root.addContent((Content)response);
            response.addContent((Content)href);
            Element propstatGood = new Element("propstat", D);
            Element propstatBad = new Element("propstat", D);
            Element propGood = new Element("prop", D);
            Element propBad = new Element("prop", D);
            propstatGood.addContent((Content)propGood);
            propstatBad.addContent((Content)propBad);
            Element error404 = (new Element("status", D)).setText("HTTP/1.1 404 Not Found");
            long quota = -12345L;
            long total_quota = -12345L;
            try {
              quota = this.thisSession.get_quota_used(this.thisSession.uiSG("current_dir"));
            } catch (Exception exception) {}
            try {
              total_quota = this.thisSession.get_total_quota(this.thisSession.uiSG("current_dir"));
            } catch (Exception exception) {}
            if (commandActions.size() == 0) {
              commandActions.put("getcontentlength", "");
              commandActions.put("getlastmodified", "");
              commandActions.put("modificationdate", "");
              fieldOrder.addElement("getcontentlength");
              fieldOrder.addElement("getlastmodified");
              fieldOrder.addElement("modificationdate");
              commandActions.put("getcontenttype", "");
              fieldOrder.addElement("getcontenttype");
              if (item.getProperty("type").toUpperCase().equals("DIR")) {
                commandActions.put("resourceType", "");
                fieldOrder.addElement("resourceType");
                if (quota != -12345L) {
                  commandActions.put("quota", "");
                  commandActions.put("quotaused", "");
                  if (fieldOrder.indexOf("quota") < 0)
                    fieldOrder.addElement("quota"); 
                  if (fieldOrder.indexOf("quotaused") < 0)
                    fieldOrder.addElement("quotaused"); 
                } 
              } 
              listAdditionalProps = true;
            } 
            if (quota == -12345L)
              quota = 0L; 
            Enumeration keylist = commandActions.keys();
            boolean useGood = false;
            boolean useBad = false;
            VRL vrl = new VRL(item.getProperty("url"));
            if (listProps) {
              String full_path = vrl.getPath();
              if (vrl.getProtocol().equalsIgnoreCase("file"))
                full_path = vrl.getCanonicalPath(); 
              Properties commandActions2 = (Properties)ServerSessionHTTP5_2.proppatches.get(full_path);
              GenericClient c = null;
              try {
                c = this.thisSession.uVFS.getClient(item);
                if (c.stat(vrl.getPath()) != null)
                  for (int xx = 0; xx < fieldOrder.size(); xx++) {
                    String key = fieldOrder.elementAt(xx).toString();
                    if (commandActions2.containsKey(key)) {
                      useGood = true;
                      Element elementKey = new Element(key, "http://www.apple.com/SyncServices");
                      propGood.addContent((Content)elementKey);
                    } 
                  }  
              } finally {
                c = this.thisSession.uVFS.releaseClient(c);
              } 
            } else {
              for (int xx = 0; xx < fieldOrder.size(); xx++) {
                String key = fieldOrder.elementAt(xx).toString();
                if (key.equalsIgnoreCase("getlastmodified")) {
                  useGood = true;
                  Element getlastmodified = new Element(key, lp1);
                  propGood.addContent((Content)getlastmodified);
                  getlastmodified.setText(this.sdf_rfc1123.format(new Date(Long.parseLong(item.getProperty("modified", "0")))));
                } else if (key.equalsIgnoreCase("modificationdate")) {
                  useGood = true;
                  Element getlastmodified = new Element(key, D);
                  propGood.addContent((Content)getlastmodified);
                  getlastmodified.setText(this.sdf_rfc1123_2.format(new Date(Long.parseLong(item.getProperty("modified", "0")))));
                } else if (key.equalsIgnoreCase("resourcetype")) {
                  useGood = true;
                  Element resourcetype = new Element("resourcetype", lp1);
                  if (item.getProperty("type").toUpperCase().equals("DIR"))
                    resourcetype.addContent((Content)new Element("collection", D)); 
                  propGood.addContent((Content)resourcetype);
                } else if (key.equalsIgnoreCase("getcontenttype")) {
                  useGood = true;
                  String ext = item.getProperty("name");
                  if (ext.indexOf(".") >= 0)
                    ext = ext.substring(ext.indexOf(".")).toUpperCase(); 
                  if (this.thisSessionHTTP.mimes.getProperty(ext, "").equals(""))
                    ext = "*"; 
                  String contentType = this.thisSessionHTTP.mimes.getProperty(ext, "");
                  if (item.getProperty("type").toUpperCase().equals("DIR"))
                    contentType = "httpd/unix-directory"; 
                  Element getcontenttype = new Element("getcontenttype", D);
                  getcontenttype.setText(contentType);
                  propGood.addContent((Content)getcontenttype);
                } else if (key.equalsIgnoreCase("iscollection")) {
                  useGood = true;
                  Element iscollection = new Element("iscollection", D);
                  iscollection.setText(item.getProperty("type").toUpperCase().equals("DIR"));
                  propGood.addContent((Content)iscollection);
                } else if (key.equalsIgnoreCase("getcontentlength")) {
                  if (item.getProperty("type").toUpperCase().equals("DIR")) {
                    useBad = true;
                    propBad.addContent((Content)new Element(key, g0));
                    if (!propstatBad.isAncestor(error404))
                      propstatBad.addContent((Content)error404); 
                  } else {
                    propGood.addContent((Content)(new Element(key, lp1)).setText(item.getProperty("size", "0")));
                    useGood = true;
                  } 
                } else if (key.equalsIgnoreCase("quota")) {
                  if (!item.getProperty("type").toUpperCase().equals("DIR") || total_quota == -12345L) {
                    useBad = true;
                    propBad.addContent((Content)new Element(key, D));
                    if (!propstatBad.isAncestor(error404))
                      propstatBad.addContent((Content)error404); 
                  } else {
                    propGood.addContent((Content)(new Element(key, D)).setText((new StringBuffer(String.valueOf(total_quota * 2L / 1024L))).toString()));
                    useGood = true;
                  } 
                } else if (key.equalsIgnoreCase("quotaused")) {
                  if (!item.getProperty("type").toUpperCase().equals("DIR")) {
                    useBad = true;
                    propBad.addContent((Content)new Element(key, D));
                    if (!propstatBad.isAncestor(error404))
                      propstatBad.addContent((Content)error404); 
                  } else {
                    propGood.addContent((Content)(new Element(key, D)).setText((new StringBuffer(String.valueOf(quota * 2L / 1024L))).toString()));
                    useGood = true;
                  } 
                } else if (key.equalsIgnoreCase("dotunderscore")) {
                  propGood.addContent((Content)(new Element(key, "A", "http://www.apple.com/webdav_fs/props/")).setText(""));
                  useGood = true;
                } else if (key.equalsIgnoreCase("dotunderscore-size")) {
                  propGood.addContent((Content)(new Element(key, "A", "http://www.apple.com/webdav_fs/props/")).setText("0"));
                  useGood = true;
                } else {
                  useBad = true;
                  Element empty = new Element(key, D);
                  propBad.addContent((Content)empty);
                  if (!propstatBad.isAncestor(error404))
                    propstatBad.addContent((Content)error404); 
                } 
              } 
              if (listAdditionalProps)
                if (vrl.getProtocol().equalsIgnoreCase("file")) {
                  Properties commandActions2 = (Properties)ServerSessionHTTP5_2.proppatches.get(vrl.getCanonicalPath());
                  if (commandActions2 != null) {
                    Enumeration additionalKeyList = commandActions2.keys();
                    while (additionalKeyList.hasMoreElements()) {
                      String key = additionalKeyList.nextElement().toString();
                      String val = commandActions2.getProperty(key);
                      useGood = true;
                      Element elementKey = new Element(key, "X", "http://www.apple.com/SyncServices");
                      propGood.addContent((Content)elementKey);
                      elementKey.setText(val);
                    } 
                  } 
                }  
            } 
            if (useGood)
              propstatGood.addContent((Content)(new Element("status", D)).setText("HTTP/1.1 200 OK")); 
            if (useGood)
              response.addContent((Content)propstatGood); 
            if (useBad && !listAdditionalProps)
              response.addContent((Content)propstatBad); 
          } 
        }  
    } 
    if (this.thisSessionHTTP.xmlOut == null) {
      this.thisSessionHTTP.xmlOut = new XMLOutputter();
      Format f = Format.getPrettyFormat();
      f.setExpandEmptyElements(false);
      this.thisSessionHTTP.xmlOut.setFormat(f);
    } 
    return this.thisSessionHTTP.xmlOut.outputString(doc);
  }
  
  public void propfind(long http_len_max, String initial_current_dir, String depth) throws Exception {
    String xml = "";
    if (http_len_max > 0L) {
      xml = get_raw_http_command((int)http_len_max);
    } else {
      int timeout = this.thisSession.sock.getSoTimeout();
      this.thisSession.sock.setSoTimeout(3000);
      try {
        while (http_len_max == -1L)
          xml = String.valueOf(xml) + get_raw_http_command((int)http_len_max); 
      } catch (SocketTimeoutException socketTimeoutException) {}
      this.thisSession.sock.setSoTimeout(timeout);
    } 
    this.thisSession.uiVG("user_log").addElement(xml);
    Properties commandActions = new Properties();
    Vector commandActionsOrder = new Vector();
    if (!xml.trim().equals(""))
      try {
        Document doc = this.sax.build(new StringReader(xml));
        List items = doc.getRootElement().getChildren();
        Iterator i = items.iterator();
        if (i.hasNext()) {
          Element element = i.next();
          List items2 = element.getChildren();
          Iterator i2 = items2.iterator();
          while (i2.hasNext()) {
            Element element2 = i2.next();
            String key = element2.getName();
            String val = element2.getText();
            commandActions.put(key, val);
            commandActionsOrder.addElement(key);
          } 
        } 
      } catch (Throwable e) {
        Log.log("DAV_SERVER", 1, e);
      }  
    xml = doPropFind(commandActions, depth, false, commandActionsOrder);
    if (this.thisSessionHTTP.SG("username").equals("anonymous")) {
      Properties item = this.thisSession.uVFS.get_item(initial_current_dir);
      if (item == null || item.getProperty("privs").indexOf("(view)") < 0) {
        this.thisSessionHTTP.DEAUTH();
        return;
      } 
    } 
    if (xml != null) {
      write_command_http("HTTP/1.1 207 Multi-Status");
      write_standard_headers();
      write_command_http("Content-Length: " + ((xml.getBytes("UTF8")).length + 2));
      write_command_http("Content-Type: text/xml; charset=\"utf-8\"");
      String dir = initial_current_dir;
      if (dir.startsWith(this.thisSession.SG("root_dir")))
        dir = initial_current_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
      Properties item = this.thisSession.uVFS.get_item(initial_current_dir);
      if (item != null && item.getProperty("type", "").equalsIgnoreCase("DIR") && !dir.endsWith("/"))
        write_command_http("Content-Location: " + Common.url_encode(dir, "/") + "/"); 
      write_command_http("");
      write_command_http(xml);
    } else {
      String msg = "Not Found: Resource does not exist";
      write_command_http("HTTP/1.1 404 Not Found: Resource does not exist");
      write_standard_headers();
      write_command_http("Content-Type: text/xml; charset=utf-8");
      write_command_http("Content-Length: " + msg.length());
      write_command_http("");
      write_command_raw(msg);
    } 
  }
  
  public void proppatch(long http_len_max, String initial_current_dir, String depth) throws Exception {
    String xml = get_raw_http_command((int)http_len_max);
    this.thisSession.uiVG("user_log").addElement(xml);
    Properties commandActions = new Properties();
    Vector commandActionsOrder = new Vector();
    try {
      Document doc = this.sax.build(new StringReader(xml));
      List items = doc.getRootElement().getChildren();
      Iterator i = items.iterator();
      while (i.hasNext()) {
        Element element = i.next();
        List items2 = element.getChildren();
        Iterator i2 = items2.iterator();
        while (i2.hasNext()) {
          Element element2 = i2.next();
          List items3 = element2.getChildren();
          Iterator i3 = items3.iterator();
          while (i3.hasNext()) {
            Element element4 = i3.next();
            String key = element4.getName();
            String val = element4.getText();
            commandActions.put(key, val);
            commandActionsOrder.addElement(key);
          } 
        } 
      } 
    } catch (Exception e) {
      Log.log("DAV_SERVER", 1, e);
    } 
    Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
    GenericClient c = this.thisSession.uVFS.getClient(item);
    try {
      VRL vrl = new VRL(item.getProperty("url"));
      String full_path = vrl.getPath();
      if (vrl.getProtocol().equalsIgnoreCase("file"))
        full_path = vrl.getCanonicalPath(); 
      Properties commandActions2 = (Properties)ServerSessionHTTP5_2.proppatches.get(full_path);
      if (c.stat(vrl.getPath()) == null)
        commandActions2 = null; 
      if (commandActions2 == null)
        commandActions2 = new Properties(); 
      commandActions2.putAll(commandActions);
      ServerSessionHTTP5_2.proppatches.put(full_path, commandActions2);
    } finally {
      c = this.thisSession.uVFS.releaseClient(c);
    } 
    this.thisSessionHTTP.savePropPatches();
    xml = doPropFind(commandActions, depth, true, commandActionsOrder);
    if (xml != null) {
      write_command_http("HTTP/1.1 207 Multi-Status");
      String dir = initial_current_dir;
      if (dir.startsWith(this.thisSession.SG("root_dir")))
        dir = initial_current_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
      item = this.thisSession.uVFS.get_item(initial_current_dir);
      if (item != null && item.getProperty("type", "").equalsIgnoreCase("DIR") && !dir.endsWith("/"))
        write_command_http("Content-Location: " + Common.url_encode(dir, "/") + "/"); 
      write_standard_headers();
      write_command_http("Content-Length: " + ((xml.getBytes("UTF8")).length + 2));
      write_command_http("Content-Type: text/xml;charset=utf-8");
      write_command_http("");
      write_command_http(xml);
      this.thisSession.uiVG("user_log").addElement(xml);
    } else {
      String msg = "Not Found: Resource does not exist";
      write_command_http("HTTP/1.1 404 Not Found: Resource does not exist");
      write_standard_headers();
      write_command_http("Content-Type: text/xml;charset=utf-8");
      write_command_http("Content-Length: " + msg.length());
      write_command_http("");
      write_command_raw(msg);
    } 
  }
  
  public String delete(String initial_current_dir, String error_message) throws Exception {
    String dir = initial_current_dir;
    if (dir.startsWith(this.thisSession.SG("root_dir")))
      dir = initial_current_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
    Properties item = this.thisSession.uVFS.get_item(initial_current_dir);
    Enumeration keys = ServerSessionHTTP5_2.proppatches.keys();
    try {
      VRL vrl = new VRL(item.getProperty("url"));
      String full_path = vrl.getPath();
      if (vrl.getProtocol().equalsIgnoreCase("file"))
        full_path = vrl.getCanonicalPath(); 
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.startsWith(full_path))
          ServerSessionHTTP5_2.proppatches.remove(key); 
      } 
      this.thisSessionHTTP.savePropPatches();
    } catch (Exception exception) {}
    this.thisSession.uiPUT("the_command", "DELE");
    this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
    error_message = String.valueOf(error_message) + this.thisSession.do_DELE(true, this.thisSession.uiSG("current_dir"));
    this.thisSession.uVFS.reset();
    if (error_message.length() == 0) {
      write_command_http("HTTP/1.1 204  No Content");
    } else if (item == null) {
      write_command_http("HTTP/1.1 404 Not found.");
    } else {
      write_command_http("HTTP/1.1 401 Access Denied.");
    } 
    if (item != null && item.getProperty("type", "").equalsIgnoreCase("DIR") && !dir.endsWith("/"))
      write_command_http("Content-Location: " + Common.url_encode(dir, "/") + "/"); 
    write_standard_headers();
    write_command_http("Content-Length: 0");
    write_command_http("");
    return error_message;
  }
  
  public void copy(String initial_current_dir, String move_destination, String overwrite) throws Exception {
    this.thisSession.uVFS.reset();
    this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
    String from = this.thisSession.uiSG("current_dir");
    boolean ok = true;
    if (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR")) {
      this.thisSession.uiPUT("current_dir", (new URL(move_destination)).getPath());
      if (!this.thisSession.uiSG("current_dir").toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
        this.thisSession.uiPUT("current_dir", String.valueOf(this.thisSession.SG("root_dir")) + (this.thisSession.uiSG("current_dir").startsWith("/") ? this.thisSession.uiSG("current_dir").substring(1) : this.thisSession.uiSG("current_dir"))); 
      if (this.thisSession.uiSG("current_dir").indexOf("\\") >= 0)
        this.thisSession.uiPUT("current_dir", this.thisSession.uiSG("current_dir").replace('\\', '/')); 
      if (!this.thisSession.uiSG("current_dir").startsWith("/"))
        this.thisSession.uiPUT("current_dir", "/" + this.thisSession.uiSG("current_dir")); 
      this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
    } else {
      ok = false;
    } 
    Properties destItem = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
    if (overwrite.equalsIgnoreCase("F") && destItem != null)
      ok = false; 
    int responseCode = 201;
    boolean storOK = false;
    if (ok)
      storOK = this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "STOR"); 
    if (ok && storOK) {
      String to = this.thisSession.uiSG("current_dir");
      Properties source = this.thisSession.uVFS.get_item(from);
      Properties dest = this.thisSession.uVFS.get_item_parent(to);
      try {
        VRL vrl1 = new VRL(source.getProperty("url"));
        Properties stat2 = null;
        VRL vrl2 = new VRL(dest.getProperty("url"));
        GenericClient c2 = this.thisSession.uVFS.getClient(dest);
        try {
          stat2 = c2.stat(vrl2.getPath());
        } finally {
          c2 = this.thisSession.uVFS.releaseClient(c2);
        } 
        if (stat2 != null)
          responseCode = 204; 
        if (stat2 != null && stat2.getProperty("type").equalsIgnoreCase("DIR") && overwrite.equalsIgnoreCase("T"))
          if (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RMD") && this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "DELE")) {
            Common.recurseDelete(vrl2.getCanonicalPath(), false);
          } else {
            ok = false;
          }  
        if (ok)
          Common.recurseCopy(vrl1.getCanonicalPath(), vrl2.getCanonicalPath(), overwrite.equalsIgnoreCase("T")); 
      } catch (Exception e) {
        ok = false;
        Log.log("DAV_SERVER", 0, e);
      } 
      this.thisSession.uVFS.reset();
    } 
    if (ok && storOK) {
      write_command_http("HTTP/1.1 " + responseCode + " No Content");
    } else if (overwrite.equalsIgnoreCase("F") && destItem != null) {
      write_command_http("HTTP/1.1 412 Access Denied.");
    } else if (ok && !storOK) {
      write_command_http("HTTP/1.1 409 does not exist");
    } else {
      write_command_http("HTTP/1.1 403 Access Denied.");
    } 
    write_standard_headers();
    write_command_http("Content-Length: 0");
    write_command_http("");
  }
  
  public String mkcol(long http_len_max, String initial_current_dir, String error_message) throws Exception {
    this.thisSession.uiPUT("the_command", "MKDIR");
    String the_dir = this.thisSession.uiSG("current_dir");
    if (!the_dir.endsWith("/"))
      the_dir = String.valueOf(the_dir) + "/"; 
    this.thisSession.uiPUT("the_command_data", the_dir);
    if (http_len_max <= 0L) {
      error_message = String.valueOf(error_message) + this.thisSession.do_MKD(false, the_dir);
    } else {
      error_message = "Body not allowed.";
    } 
    this.thisSession.uVFS.reset();
    boolean good = false;
    Properties item = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.uiSG("current_dir")) + "/");
    if (error_message.length() == 0) {
      good = true;
      this.thisSession.accessExceptions.put(String.valueOf(this.thisSession.uiSG("current_dir")) + "/", item);
      write_command_http("HTTP/1.1 201 Created");
      for (int x = 0; x < this.thisSessionHTTP.headers.size(); x++) {
        String data = this.thisSessionHTTP.headers.elementAt(x).toString();
        if (data.startsWith("Last-Modified: ")) {
          String modified = data.substring(data.indexOf(":") + 1).trim();
          this.thisSession.uiPUT("the_command", "MDTM");
          SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
          this.thisSession.uiPUT("the_command_data", String.valueOf(this.thisSession.uiSG("current_dir")) + " " + sdf.format(this.sdf_rfc1123.parse(modified)));
          error_message = String.valueOf(error_message) + this.thisSession.do_MDTM();
        } 
      } 
      this.thisSession.uVFS.reset();
    } else if (error_message.indexOf(LOC.G("exists")) >= 0 && item.getProperty("type").equalsIgnoreCase("DIR")) {
      write_command_http("HTTP/1.1 405 Already exists.");
    } else if (http_len_max != 0L) {
      write_command_http("HTTP/1.1 415 Body not allowed.");
    } else {
      write_command_http("HTTP/1.1 409 Access Denied.");
    } 
    write_standard_headers();
    if (good)
      write_command_http("Content-Location: " + Common.url_encode(the_dir, "/")); 
    write_command_http("Content-Length: 18");
    write_command_http("");
    write_command_http("Resource created");
    return error_message;
  }
  
  public void lock(long http_len_max, String initial_current_dir, String depth) throws Exception {
    String xml = "";
    if (http_len_max >= 0L)
      xml = get_raw_http_command((int)http_len_max); 
    this.thisSession.uiVG("user_log").addElement(xml);
    Properties item = this.thisSession.uVFS.get_item(initial_current_dir);
    Element root = new Element("prop", "D", "DAV:");
    Document doc = new Document(root);
    Element lockdiscovery = new Element("lockdiscovery", "D", "DAV:");
    root.addContent((Content)lockdiscovery);
    Element activelock = new Element("activelock", "D", "DAV:");
    lockdiscovery.addContent((Content)activelock);
    Element locktype = new Element("locktype", "D", "DAV:");
    activelock.addContent((Content)locktype);
    Element write = new Element("write", "D", "DAV:");
    locktype.addContent((Content)write);
    Element lockscope = new Element("lockscope", "D", "DAV:");
    activelock.addContent((Content)lockscope);
    Element exclusive = new Element("exclusive", "D", "DAV:");
    lockscope.addContent((Content)exclusive);
    Element depthElement = new Element("depth", "D", "DAV:");
    activelock.addContent((Content)depthElement);
    depthElement.setText(depth);
    Element owner = new Element("owner", "D", "DAV:");
    activelock.addContent((Content)owner);
    Element href = new Element("href", "D", "DAV:");
    owner.addContent((Content)href);
    Element synclockinfo = new Element("synclockinfo", "SY", "http://www.apple.com/SyncServices");
    href.addContent((Content)synclockinfo);
    Element lock_user = new Element("lock-user", "SY", "SY:");
    synclockinfo.addContent((Content)lock_user);
    lock_user.setText(this.thisSessionHTTP.SG("username"));
    if (xml.indexOf("clientid>") >= 0) {
      Element clientid = new Element("clientid", "SY", "SY:");
      synclockinfo.addContent((Content)clientid);
      clientid.setText(xml.substring(xml.indexOf("clientid>") + 9, xml.indexOf("<", xml.indexOf("clientid>"))));
    } 
    if (xml.indexOf("clientname>") >= 0) {
      Element clientname = new Element("clientname", "SY", "SY:");
      synclockinfo.addContent((Content)clientname);
      clientname.setText(xml.substring(xml.indexOf("clientname>") + 11, xml.indexOf("<", xml.indexOf("clientname>"))));
    } 
    Element acquiredate = new Element("acquiredate", "SY", "SY:");
    synclockinfo.addContent((Content)acquiredate);
    SimpleDateFormat lockSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    acquiredate.setText(lockSDF.format(new Date()));
    Element timeout = new Element("timeout", "D", "DAV:");
    activelock.addContent((Content)timeout);
    timeout.setText("Second-239");
    Properties lock = new Properties();
    lock.put("token", Common.makeBoundary().toLowerCase());
    lock.put("stamp", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    lock.put("duration", "600");
    String urlLock = initial_current_dir;
    if (item != null)
      urlLock = item.getProperty("url"); 
    try {
      lock.put("resource", urlLock.toUpperCase());
    } catch (Exception exception) {}
    ServerSessionHTTP5_2.locktokens.put(lock.getProperty("token"), lock);
    Element locktoken = new Element("locktoken", "D", "DAV:");
    activelock.addContent((Content)locktoken);
    Element href2 = new Element("href", "D", "DAV:");
    locktoken.addContent((Content)href2);
    href2.setText("dotmaclocktoken:" + lock.getProperty("token"));
    if (this.xmlOut == null) {
      this.xmlOut = new XMLOutputter();
      Format f = Format.getPrettyFormat();
      f.setExpandEmptyElements(false);
      this.xmlOut.setFormat(f);
    } 
    xml = this.xmlOut.outputString(doc);
    write_command_http("HTTP/1.1 200 OK");
    write_standard_headers();
    write_command_http("Content-Length: " + ((xml.getBytes("UTF8")).length + 2));
    write_command_http("Content-Type: text/xml;charset=utf-8");
    write_command_http("Lock-Token: <dotmaclocktoken:" + lock.getProperty("token") + ">");
    String dir = initial_current_dir;
    if (dir.startsWith(this.thisSession.SG("root_dir")))
      dir = initial_current_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
    item = this.thisSession.uVFS.get_item(initial_current_dir);
    if (item != null && item.getProperty("type", "").equalsIgnoreCase("DIR") && !dir.endsWith("/"))
      write_command_http("Content-Location: " + Common.url_encode(dir, "/") + "/"); 
    write_command_http("");
    write_command_raw(String.valueOf(xml) + "\r\n");
    this.thisSession.uiVG("user_log").addElement(xml);
  }
  
  public void unlock(String initial_current_dir) throws Exception {
    Properties item = this.thisSession.uVFS.get_item(initial_current_dir);
    write_command_http("HTTP/1.1 204 No Content");
    write_standard_headers();
    write_command_http("Content-Length: 0");
    write_command_http("");
    Properties lock = new Properties();
    lock.put("token", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    lock.put("stamp", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    lock.put("duration", "600");
    String urlLock = initial_current_dir;
    if (item != null)
      urlLock = item.getProperty("url"); 
    lock.put("resource", urlLock.toUpperCase());
    ServerSessionHTTP5_2.locktokens.put(lock.getProperty("token"), lock);
  }
  
  public String move(String move_destination, String error_message, String overwrite) throws Exception {
    this.thisSession.uVFS.reset();
    String rename_from = this.thisSession.uiSG("current_dir");
    this.thisSession.uiPUT("the_command_data", rename_from);
    error_message = String.valueOf(error_message) + this.thisSession.do_RNFR();
    this.thisSession.uiPUT("current_dir", move_destination);
    try {
      this.thisSession.uiPUT("current_dir", (new URL(move_destination)).getPath());
    } catch (Exception e) {
      Log.log("DAV_SERVER", 2, e);
    } 
    if (!this.thisSession.uiSG("current_dir").toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
      this.thisSession.uiPUT("current_dir", String.valueOf(this.thisSession.SG("root_dir")) + (this.thisSession.uiSG("current_dir").startsWith("/") ? this.thisSession.uiSG("current_dir").substring(1) : this.thisSession.uiSG("current_dir"))); 
    if (this.thisSession.uiSG("current_dir").indexOf("\\") >= 0)
      this.thisSession.uiPUT("current_dir", this.thisSession.uiSG("current_dir").replace('\\', '/')); 
    if (!this.thisSession.uiSG("current_dir").startsWith("/"))
      this.thisSession.uiPUT("current_dir", "/" + this.thisSession.uiSG("current_dir")); 
    this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
    Properties destItem = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
    Properties destItemParent = this.thisSession.uVFS.get_item(Common.all_but_last(this.thisSession.uiSG("current_dir")));
    if (overwrite.equalsIgnoreCase("F") && destItem != null)
      error_message = "item exists."; 
    Log.log("DAV_SERVER", 0, "WebDAV Move:" + this.thisSession.uiSG("current_dir"));
    if (error_message.length() == 0 && destItemParent != null)
      error_message = String.valueOf(error_message) + this.thisSession.do_RNTO(true); 
    this.thisSession.uVFS.reset();
    int responseCode = 201;
    if (destItem != null)
      responseCode = 204; 
    if (destItemParent == null) {
      write_command_http("HTTP/1.1 404 Parent Not Found");
    } else if (ServerStatus.BG("omnipresence_hack") && error_message.length() == 0) {
      write_command_http("HTTP/1.1 207 Multi-Status");
    } else if (error_message.length() == 0) {
      write_command_http("HTTP/1.1 201 Moved");
    } else if (error_message.indexOf("RNFR") >= 0) {
      write_command_http("HTTP/1.1 404 Not Found");
    } else if (error_message.indexOf("item exists") >= 0) {
      write_command_http("HTTP/1.1 412 item exists");
    } else {
      write_command_http("HTTP/1.1 403 Access Denied.");
    } 
    write_standard_headers();
    String xml = "";
    if (ServerStatus.BG("omnipresence_hack")) {
      xml = String.valueOf(xml) + "<?xml version=\"1.0\" encoding=\"utf-8\" ?>";
      xml = String.valueOf(xml) + "   <d:multistatus xmlns:d='DAV:'>";
      xml = String.valueOf(xml) + "     <d:response>";
      xml = String.valueOf(xml) + "          <d:href>move_destination</d:href>";
      xml = String.valueOf(xml) + "          <d:status>HTTP/1.1 " + responseCode + " No Content</d:status>";
      xml = String.valueOf(xml) + "     </d:response>";
      xml = String.valueOf(xml) + "   </d:multistatus>";
      write_command_http("Content-Length: " + ((xml.getBytes("UTF8")).length + 2));
      write_command_http("");
      write_command_raw(String.valueOf(xml) + "\r\n");
    } else {
      write_command_http("Content-Length: 0");
      write_command_http("");
    } 
    this.thisSession.uiVG("user_log").addElement(xml);
    return error_message;
  }
  
  public void acl(long http_len_max, String initial_current_dir) throws Exception {
    String xml = get_raw_http_command((int)http_len_max);
    String dir = initial_current_dir;
    if (dir.startsWith(this.thisSession.SG("root_dir")))
      dir = initial_current_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
    Properties item = this.thisSession.uVFS.get_item(initial_current_dir);
    write_command_http("HTTP/1.1 200 OK");
    write_standard_headers();
    write_command_http("Content-Length: 0");
    write_command_http("");
  }
  
  public void write_command_http(String s) throws Exception {
    this.thisSessionHTTP.write_command_http(s);
  }
  
  public void write_command_raw(String s) throws Exception {
    this.thisSessionHTTP.write_command_raw(s);
  }
  
  public void write_standard_headers() throws Exception {
    this.thisSessionHTTP.write_standard_headers();
  }
  
  public String get_raw_http_command(int len) throws Exception {
    return this.thisSessionHTTP.get_raw_http_command(len);
  }
}
