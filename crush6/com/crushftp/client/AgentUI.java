package com.crushftp.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AgentUI extends HttpCommandHandler {
  Properties prefs = new Properties();
  
  public Properties clients = new Properties();
  
  public static AgentUI thisObj = null;
  
  static String last_m = "";
  
  static SimpleDateFormat mm = new SimpleDateFormat("mm");
  
  public Properties valid_tokens = new Properties();
  
  public static void main(String[] args) {
    System.setProperty("com.crushftp.server.httphandler", "com.crushftp.client.AgentUI");
    HTTPD.main(args);
  }
  
  public AgentUI() {
    thisObj = this;
    String home_folder = String.valueOf(System.getProperty("user.home")) + "/" + System.getProperty("crushclient.appname", "CrushClient") + "/";
    System.getProperties().put("crushclient.prefs", home_folder);
    this.prefs.put("version", "1.0");
    try {
      if ((new File(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML")).exists())
        this.prefs = (Properties)Common.readXMLObject(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML"); 
    } catch (Exception e) {
      e.printStackTrace();
    } 
    try {
      Worker.startWorker(new Runnable(this) {
            final AgentUI this$0;
            
            public void run() {
              while (true) {
                try {
                  String current_m = AgentUI.mm.format(new Date());
                  if (!AgentUI.last_m.equals(current_m)) {
                    AgentUI.last_m = current_m;
                    Thread.sleep(3000L);
                    AgentScheduler.runSchedules(AgentUI.thisObj);
                  } 
                } catch (Exception e) {
                  System.out.println(new Date() + ":" + e);
                  e.printStackTrace();
                } 
                try {
                  Thread.sleep(1000L);
                } catch (InterruptedException interruptedException) {}
              } 
            }
          });
    } catch (IOException e) {
      e.printStackTrace();
    } 
  }
  
  public void handleCommand(String path, Properties headers, Properties request, OutputStream out, InputStream in, Properties session, String ip) throws IOException {
    if (path.equals("agent")) {
      String contentType = "text/plain";
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
        ByteArrayOutputStream tmp = getRequest(headers, request, in);
        contentType = processRequest(request, baos, session, ip, tmp);
      } catch (Exception e) {
        e.printStackTrace();
        baos.write(e.getBytes());
      } 
      write_command_http("HTTP/1.1 200 OK", out);
      write_command_http("Date: " + this.sdf_rfc1123.format(new Date()), out);
      write_command_http("Server: CrushHTTPD", out);
      write_command_http("P3P: policyref=\"p3p.xml\", CP=\"IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT\"", out);
      write_command_http("Keep-Alive: timeout=15, max=20", out);
      write_command_http("Content-Type: " + contentType, out);
      write_command_http("Connection: Keep-Alive", out);
      write_command_http("Last-Modified: " + this.sdf_rfc1123.format(new Date()), out);
      write_command_http("ETag: " + System.currentTimeMillis(), out);
      write_command_http("Content-Length: " + baos.size(), out);
      write_command_http("", out);
      out.write(baos.toByteArray());
    } else {
      ByteArrayOutputStream f_b = new ByteArrayOutputStream();
      File file = new File(String.valueOf(System.getProperty("com.crushftp.server.httphandler.path", "./clientui/")) + path);
      URL f = null;
      if (file.exists()) {
        f = file.toURI().toURL();
        try {
          Common.streamCopier(new FileInputStream(file), f_b, false, true, true);
        } catch (InterruptedException e) {
          e.printStackTrace();
        } 
      } else {
        path = Common.replace_str(path, "crushClient", "CrushClient");
        f = getClass().getResource("/clientui/" + path);
        if (f != null)
          try {
            Common.streamCopier(getClass().getResourceAsStream("/clientui/" + path), f_b, false, true, true);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }  
      } 
      if (f != null) {
        write_command_http("HTTP/1.1 200 OK", out);
      } else {
        write_command_http("HTTP/1.1 404 Not found", out);
      } 
      write_command_http("Date: " + this.sdf_rfc1123.format(new Date()), out);
      write_command_http("Server: CrushHTTPD", out);
      write_command_http("P3P: policyref=\"p3p.xml\", CP=\"IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT\"", out);
      write_command_http("Keep-Alive: timeout=15, max=20", out);
      write_command_http("Content-Type: " + Common.getContentType(Common.last(f.getPath())), out);
      write_command_http("Connection: Keep-Alive", out);
      write_command_http("Last-Modified: " + this.sdf_rfc1123.format(new Date()), out);
      write_command_http("ETag: " + System.currentTimeMillis(), out);
      write_command_http("Content-Length: " + f_b.size(), out);
      write_command_http("", out);
      if (f != null)
        out.write(f_b.toByteArray()); 
    } 
    out.flush();
  }
  
  public String processRequest(Properties request, ByteArrayOutputStream baos, Properties session, String ip, ByteArrayOutputStream tmp) throws Exception {
    String contentType = "text/html";
    if (request.getProperty("command", "").equals("authenticate")) {
      if ((new File(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML")).exists())
        this.prefs = (Properties)Common.readXMLObject(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML"); 
      if (this.prefs.getProperty("agent_password", "").equals("") || Common.getMD5(new ByteArrayInputStream(VRL.vrlDecode(request.getProperty("password")).getBytes())).equals(this.prefs.getProperty("agent_password", ""))) {
        String token = Common.makeBoundary(15);
        this.valid_tokens.put(token, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        baos.write(("SUCCESS:" + token).getBytes());
      } else {
        baos.write("ERROR:Invalid password".getBytes());
      } 
    } else if (!this.valid_tokens.containsKey(request.getProperty("auth_token", ""))) {
      baos.write("ERROR:NOT AUTHENTICATED\r\n".getBytes());
    } else if (request.getProperty("command", "").equals("create")) {
      Client client = getNewClient();
      this.clients.put(request.getProperty("client"), client);
    } else if (request.getProperty("command", "").equals("list")) {
      Enumeration keys = this.clients.keys();
      while (keys.hasMoreElements())
        baos.write((String.valueOf(keys.nextElement().toString()) + "\r\n").getBytes()); 
    } else if (request.getProperty("command", "").equals("encrypt_decrypt")) {
      String s = request.getProperty("pass");
      if (request.getProperty("encrypt").equals("true"))
        s = VRL.vrlDecode(s); 
      s = Common.encryptDecrypt(s, request.getProperty("encrypt").equals("true"));
      if (request.getProperty("encrypt").equals("false"))
        s = VRL.vrlEncode(s); 
      baos.write(s.getBytes());
    } else if (request.getProperty("command", "").equals("destroy")) {
      Client client = (Client)this.clients.remove(request.getProperty("client"));
      client.process_command("QUIT", false);
    } else if (request.getProperty("command", "").equals("info")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Properties credentials = new Properties();
      credentials.put("source", client.source_credentials);
      credentials.put("destination", client.destination_credentials);
      baos.write(Common.getXMLString(credentials, "credentials").getBytes("UTF8"));
      contentType = "text/xml";
    } else if (request.getProperty("command", "").equals("process_command")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Object o = client.process_command(String.valueOf(VRL.vrlDecode(request.getProperty("command_str"))) + (request.getProperty("multithreaded", "false").equals("true") ? "&" : ""), false);
      if (o != null && (request.getProperty("command_str").toUpperCase().startsWith("LIST") || request.getProperty("command_str").toUpperCase().startsWith("LLIST") || request.getProperty("command_str").toUpperCase().startsWith("DIR") || request.getProperty("command_str").toUpperCase().startsWith("LDIR"))) {
        if (o instanceof Vector) {
          Properties p = new Properties();
          p.put("listing", o);
          o = p;
        } 
        if (o instanceof Properties) {
          baos.write(getJsonListObj((Properties)o, false).getBytes("UTF8"));
          contentType = "application/jsonrequest;charset=utf-8";
        } else {
          baos.write(o.getBytes());
        } 
      } else if (o != null) {
        baos.write(o.getBytes());
      } 
    } else if (request.getProperty("command", "").equals("run_schedule")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Vector schedules = (Vector)this.prefs.get("schedules");
      for (int x = 0; x < schedules.size(); x++) {
        Properties p = schedules.elementAt(x);
        if (p.getProperty("scheduleName").equals(request.getProperty("scheduleName")))
          return runSchedule(client, p); 
      } 
    } else if (request.getProperty("command", "").equals("log")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      while (client != null && client.messages.size() > 0) {
        baos.write(client.messages.remove(0).toString().getBytes("UTF8"));
        baos.write("\r\n".getBytes());
      } 
    } else if (request.getProperty("command", "").equals("stats")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Vector v = client.getStats();
      while (v.size() > 0) {
        baos.write(v.remove(0).toString().getBytes("UTF8"));
        baos.write("\r\n".getBytes());
      } 
    } else if (request.getProperty("command", "").equals("queue")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Vector v = (Vector)client.pending_transfer_queue.clone();
      while (v.size() > 0) {
        baos.write(v.remove(0).toString().getBytes("UTF8"));
        baos.write("\r\n".getBytes());
      } 
    } else if (request.getProperty("command", "").equals("set_password")) {
      String pass_hash = Common.getMD5(new ByteArrayInputStream(VRL.vrlDecode(request.getProperty("password")).getBytes()));
      if (this.prefs.getProperty("agent_password", "").equals("") || pass_hash.equals(this.prefs.getProperty("agent_password", ""))) {
        if (request.getProperty("new_password", "").equals("")) {
          this.prefs.put("agent_password", "");
        } else {
          this.prefs.put("agent_password", Common.getMD5(new ByteArrayInputStream(VRL.vrlDecode(request.getProperty("new_password")).getBytes())));
        } 
        savePrefs();
        baos.write("SUCCESS:Password updated\r\n".getBytes());
      } else {
        baos.write("ERROR:Invalid password\r\n".getBytes());
      } 
      baos.write("\r\n".getBytes());
    } else if (request.getProperty("command", "").equals("load_prefs")) {
      if ((new File(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML")).exists())
        this.prefs = (Properties)Common.readXMLObject(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML"); 
      Properties prefs2 = (Properties)this.prefs.clone();
      prefs2.remove("agent_password");
      baos.write(Common.getXMLString(prefs2, "prefs").getBytes("UTF8"));
      contentType = "text/xml";
    } else if (request.getProperty("command", "").equals("save_prefs")) {
      Properties prefs2 = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("prefs").replace('+', ' ')).getBytes("UTF8")));
      if (this.prefs == null)
        this.prefs = new Properties(); 
      prefs2.remove("agent_password");
      this.prefs.putAll(prefs2);
      savePrefs();
    } 
    return contentType;
  }
  
  public Client getNewClient() {
    Client client = new Client(new Vector());
    client.dual_log = false;
    client.interactive = false;
    return client;
  }
  
  public void savePrefs() throws Exception {
    (new File(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML.new")).delete();
    Common.writeXMLObject(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML.new", this.prefs, "prefs");
    (new File(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML")).delete();
    (new File(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML.new")).renameTo(new File(String.valueOf(System.getProperty("crushsync.prefs", "./")) + "prefs.XML"));
  }
  
  public String runSchedule(Client client, Properties schedule) {
    try {
      String script = schedule.getProperty("script", "").trim();
      if ((new File(script)).exists()) {
        ByteArrayOutputStream baos_script = new ByteArrayOutputStream();
        Common.streamCopier(new FileInputStream(script), baos_script, false, true, true);
        script = (new String(baos_script.toByteArray(), "UTF8")).trim();
      } 
      script = String.valueOf(script) + "\r\nquit\r\n";
      int pos = script.indexOf("{bookmark:");
      while (pos >= 0) {
        String inner = script.substring(pos, script.indexOf("}", pos) + 1);
        Vector bookmarks = (Vector)this.prefs.get("bookmarks");
        String result = null;
        for (int x = 0; bookmarks != null && x < bookmarks.size(); x++) {
          Properties book = bookmarks.elementAt(x);
          if (book.getProperty("name", "").equalsIgnoreCase(inner.split(":")[1])) {
            String val = "";
            String key = inner.split(":")[2];
            key = key.substring(0, key.length() - 1);
            if (key.equalsIgnoreCase("pass")) {
              val = VRL.vrlEncode(Common.encryptDecrypt(book.getProperty("pass"), false));
            } else if (key.equalsIgnoreCase("url")) {
              if (book.getProperty("protocol").startsWith("file:")) {
                val = String.valueOf(book.getProperty("protocol")) + book.getProperty("defaultPath");
              } else {
                val = String.valueOf(book.getProperty("protocol")) + book.getProperty("user") + ":" + VRL.vrlEncode(Common.encryptDecrypt(book.getProperty("pass"), false)) + "@" + book.getProperty("host") + ":" + book.getProperty("port") + book.getProperty("defaultPath");
              } 
              if (!val.endsWith("/"))
                val = String.valueOf(val) + "/"; 
            } else {
              val = book.getProperty(key);
            } 
            result = val;
            break;
          } 
        } 
        if (result == null)
          return "Bookmark not found:" + inner; 
        script = Common.replace_str(script, inner, result);
        pos = script.indexOf("{bookmark:");
      } 
      client.br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(script.getBytes("UTF8"))));
      client.local_echo = true;
      (new Thread(client)).start();
      return String.valueOf(schedule.getProperty("scheduleName")) + " started.";
    } catch (Exception e) {
      e.printStackTrace();
      return (String)e;
    } 
  }
  
  public static String getJsonList(Properties listingProp, boolean exif_listings) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    sb.append("l = new Array();\r\n");
    String s = "";
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      String eol = "\r\n";
      sb.append("lp = {};\r\n");
      s = "name";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "dir";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "type";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "root_dir";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "source";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "href_path";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "privs";
      sb.append("lp." + s + "=\"" + lp.getProperty(s).replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "size";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "modified";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "owner";
      sb.append("lp." + s + "=\"" + lp.getProperty(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "group";
      sb.append("lp." + s + "=\"" + lp.getProperty(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "permissionsNum";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "keywords";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").trim().replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\";" + eol);
      s = "permissions";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "num_items";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "boot";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "false") + "\";" + eol);
      s = "preview";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "dateFormatted";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "sizeFormatted";
      if (exif_listings) {
        s = "width";
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
        s = "height";
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
        Enumeration keys = lp.keys();
        while (keys.hasMoreElements()) {
          s = (String)keys.nextElement();
          if (s.startsWith("crushftp_"))
            sb.append("lp." + s + "=\"" + lp.getProperty(s, "").trim().replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\";" + eol); 
        } 
      } 
      sb.append("l[l.length] = lp;" + eol);
    } 
    return "\r\n<listing>" + sb.toString() + "</listing>";
  }
  
  public static String getJsonListObj(Properties listingProp, boolean exif_listings) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    String s = "";
    String eol = "\r\n";
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      if (x > 0) {
        sb.append(",{").append(eol);
      } else {
        sb.append("{").append(eol);
      } 
      s = "name";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "dir";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "type";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "root_dir";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "source";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "href_path";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "privs";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "size";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "modified";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "owner";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\"," + eol);
      s = "group";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\"," + eol);
      s = "permissionsNum";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "keywords";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\"," + eol);
      s = "permissions";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "num_items";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "preview";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      if (exif_listings) {
        s = "width";
        sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
        s = "height";
        sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
        Enumeration keys = lp.keys();
        while (keys.hasMoreElements()) {
          s = (String)keys.nextElement();
          if (s.startsWith("crushftp_"))
            sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\"," + eol); 
        } 
      } 
      s = "dateFormatted";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "sizeFormatted";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"" + eol);
      sb.append("}").append(eol);
    } 
    sb.append("]");
    return sb.toString();
  }
  
  public static String getStatList(Properties listingProp) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      sb.append(String.valueOf(lp.getProperty("permissions")) + " " + lp.getProperty("num_items") + " " + lp.getProperty("owner") + " " + lp.getProperty("group") + " " + lp.getProperty("size") + " " + yyyyMMddHHmmss.format(new Date(Long.parseLong(lp.getProperty("modified")))) + " " + lp.getProperty("day") + " " + lp.getProperty("time_or_year") + " " + (String.valueOf(lp.getProperty("root_dir")) + lp.getProperty("name")).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C") + "\r\n");
    } 
    return "\r\n<listing>" + sb.toString() + "</listing>";
  }
  
  public static String getDmzList(Properties listingProp) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      sb.append(formatDmzStat(lp)).append("\r\n");
    } 
    return "\r\n<listing>" + sb.toString() + "</listing>";
  }
  
  public static String formatDmzStat(Properties lp) {
    Enumeration keys = lp.keys();
    String s = "";
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      String val = (new StringBuffer(String.valueOf(lp.getProperty(key)))).toString();
      s = String.valueOf(s) + key + "=" + Common.url_encode(val) + ";";
    } 
    return s;
  }
  
  public static void writeFileFromJar(String src, String dst, File jar) {
    try {
      ZipFile zf = new ZipFile(jar);
      ZipEntry ze = zf.getEntry("assets/crushftp/" + src);
      InputStream in = zf.getInputStream(ze);
      (new File(Common.all_but_last(String.valueOf(dst) + src))).mkdirs();
      RandomAccessFile out = new RandomAccessFile(String.valueOf(dst) + src, "rw");
      int bytes = 0;
      byte[] b = new byte[32768];
      while (bytes >= 0) {
        bytes = in.read(b);
        if (bytes > 0)
          out.write(b, 0, bytes); 
      } 
      in.close();
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
}
