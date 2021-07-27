package crushftp.server;

import com.crushftp.client.Base64;
import com.crushftp.client.Common;
import com.crushftp.client.File_S;
import com.crushftp.client.GenericClient;
import com.crushftp.client.UnChunkInputStream;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import com.crushftp.tunnel2.Chunk;
import com.crushftp.tunnel2.DVector;
import com.crushftp.tunnel2.Queue;
import com.crushftp.tunnel2.Tunnel2;
import crushftp.db.SearchHandler;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.IdleMonitor;
import crushftp.handlers.Log;
import crushftp.handlers.PreviewWorker;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.SharedSession;
import crushftp.handlers.SharedSessionReplicated;
import crushftp.handlers.UserTools;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.net.ssl.SSLSocket;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jdom.output.XMLOutputter;

public class ServerSessionHTTP {
  static Properties proppatches = null;
  
  static Properties locktokens = null;
  
  String http_dir = null;
  
  public int bufferSize = 262144;
  
  byte[] headerBytes = new byte[this.bufferSize];
  
  public SessionCrush thisSession = null;
  
  public Thread this_thread = null;
  
  public Socket sock = null;
  
  public Socket reverseSock = null;
  
  public long reverseSockUsed = 0L;
  
  public OutputStream original_os = null;
  
  public BufferedInputStream original_is = null;
  
  public boolean keepGoing = true;
  
  SimpleDateFormat sdf_rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  
  int timeoutSeconds = 300;
  
  boolean done = false;
  
  RETR_handler retr = new RETR_handler();
  
  String cacheHeader = "";
  
  boolean writeCookieAuth = false;
  
  boolean deleteCookieAuth = false;
  
  public IdleMonitor thread_killer_item = null;
  
  Properties server_item;
  
  Vector headers = new Vector();
  
  Properties headerLookup = new Properties();
  
  String proxy = "";
  
  String hostString = "";
  
  XMLOutputter xmlOut = null;
  
  public boolean chunked = false;
  
  boolean alreadyChunked = false;
  
  long http_len_max = 0L;
  
  ServerSessionAJAX ssa = null;
  
  ServerSessionDAV ssd = null;
  
  String CRLF = "\r\n";
  
  String secureCookie = "";
  
  boolean reverseProxyHttps = false;
  
  public static Vector webDavAgents = new Vector();
  
  String user_ip = null;
  
  int listen_port = 0;
  
  String listen_ip = null;
  
  String listen_ip_port = null;
  
  int user_number = 0;
  
  String direction;
  
  public BufferedInputStream getHeaders(BufferedInputStream is, StringBuffer blockLog) throws Exception {
    long start = System.currentTimeMillis();
    this.sock.setSoTimeout(30000);
    String headerStr = "";
    if (this.alreadyChunked && is instanceof UnChunkInputStream) {
      ((UnChunkInputStream)is).reInitialize();
      ((UnChunkInputStream)is).setChunked(false);
    } else if (this.alreadyChunked) {
      this.alreadyChunked = false;
    } 
    try {
      int bytesRead = 0;
      is.mark(this.bufferSize * 3);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      while (bytesRead >= 0) {
        bytesRead = is.read(this.headerBytes);
        if (bytesRead > 0) {
          baos.write(this.headerBytes, 0, bytesRead);
          headerStr = String.valueOf(headerStr) + new String(this.headerBytes, 0, bytesRead, "UTF8");
        } 
        if (headerStr.indexOf("\r\n\r\n") >= 0 || headerStr.length() > 128000 || System.currentTimeMillis() - start > 30000L)
          break; 
      } 
      if (headerStr.indexOf("\r\n\r\n") < 0) {
        this.done = true;
      } else {
        headerStr = new String(baos.toByteArray(), "UTF8");
        bytesRead = headerStr.indexOf("\r\n\r\n") + 4;
        is.reset();
        is.skip(((new String(baos.toByteArray(), "ISO8859_1")).indexOf("\r\n\r\n") + 4));
        headerStr = headerStr.substring(0, bytesRead).trim();
        if (headerStr.indexOf("%0D") >= 0 || headerStr.indexOf("%0d") >= 0 || headerStr.indexOf("%0a") >= 0 || headerStr.indexOf("%0a") >= 0)
          headerStr = Common.replace_str(headerStr, "%", ""); 
        if (headerStr.indexOf("command=getServerItem") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getLog") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getUser") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getUserXMLListing") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getUserList") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getAdminXMLListing") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getSessionTimeout") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getUserXMLListing") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getAdminXMLListing") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=updateNowProgress") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=encryptPassword") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getJobsSummary") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("command=getStatHistory") >= 0) {
          blockLog.append("true:").append(headerStr);
        } else if (headerStr.indexOf("GET /WebInterface/") >= 0) {
          blockLog.append("true:").append(headerStr);
        } 
        BufferedReader bsr = new BufferedReader(new StringReader(headerStr));
        String data = "";
        boolean hasUrlCheck = false;
        String[] hack_users = ServerStatus.SG("hack_usernames").split(",");
        while ((data = bsr.readLine()) != null) {
          data = data.trim();
          if (data.length() > ServerStatus.IG("max_http_header_length")) {
            Log.log("SERVER", 0, "HTTP Header too long, aborting:" + data);
            throw new Exception("HTTP Header too long:" + data.length());
          } 
          if (this.headers.size() == 0) {
            try {
              String path = data.substring(data.indexOf(" ") + 1);
              String tmpProxy = this.proxy.substring(0, this.proxy.length() - 1);
              if (path.startsWith(this.proxy) && !this.proxy.equals("/") && !this.proxy.equals("")) {
                path = path.substring(this.proxy.length());
                if (!path.startsWith("/"))
                  path = "/" + path; 
                data = String.valueOf(data.substring(0, data.indexOf(" ") + 1)) + path;
                if (data.startsWith("GET  HTTP"))
                  data = Common.replace_str(data, "GET  HTTP", "GET / HTTP"); 
              } else if (path.startsWith(tmpProxy) && !tmpProxy.equals("/") && !tmpProxy.equals("")) {
                path = path.substring(tmpProxy.length());
                if (!path.startsWith("/"))
                  path = "/" + path; 
                data = String.valueOf(data.substring(0, data.indexOf(" ") + 1)) + path;
                if (data.startsWith("GET  HTTP"))
                  data = Common.replace_str(data, "GET  HTTP", "GET / HTTP"); 
              } 
              path = path.substring(0, path.lastIndexOf(" "));
              if (path.startsWith("//"))
                path = path.substring(1); 
              for (int x = 0; x < hack_users.length; x++) {
                if (hack_users[x].trim().toUpperCase().startsWith("URL:")) {
                  hasUrlCheck = true;
                  String pattern = hack_users[x].trim().toUpperCase().substring(4).trim();
                  if (Common.do_search(pattern, path.toUpperCase(), false, 0)) {
                    Log.log("HTTP_SERVER", 0, "Banning IP for hack attempt:" + pattern + " vs. " + path);
                    Properties ban_item = new Properties();
                    ban_item.put("user_ip", this.sock.getInetAddress().getHostAddress());
                    ServerStatus.thisObj.ban(ban_item, ServerStatus.IG("hban_timeout"), "hack URL:" + path);
                    ServerStatus.thisObj.kick(ban_item);
                    try {
                      Properties info = new Properties();
                      info.put("alert_type", "hack");
                      info.put("alert_sub_type", "url");
                      info.put("alert_timeout", (new StringBuffer(String.valueOf(ServerStatus.IG("hban_timeout")))).toString());
                      info.put("alert_max", "0");
                      info.put("alert_msg", path);
                      ServerStatus.thisObj.runAlerts("security_alert", info, this.thisSession.user_info, null);
                    } catch (Exception e) {
                      Log.log("BAN", 1, e);
                    } 
                    this.done = true;
                    return null;
                  } 
                } 
              } 
            } catch (Exception exception) {}
          } else {
            data = Common.url_decode(data);
            if (data.indexOf(":") > 0)
              this.headerLookup.put(data.substring(0, data.indexOf(":")).toUpperCase().trim(), data.substring(data.indexOf(":") + 1).trim()); 
          } 
          this.headers.addElement(data);
        } 
        if (hasUrlCheck)
          for (int x = 0; x < hack_users.length; x++) {
            if (hack_users[x].trim().toUpperCase().startsWith("URL:")) {
              String pattern = hack_users[x].trim().toUpperCase().substring(4).trim();
              if (Common.do_search(pattern, this.headerLookup.getProperty("HOST", ""), false, 0)) {
                Log.log("HTTP_SERVER", 0, "Banning IP for banned host attempt:" + pattern + " vs. " + this.headerLookup.getProperty("HOST", ""));
                Properties ban_item = new Properties();
                ban_item.put("user_ip", this.sock.getInetAddress().getHostAddress());
                ServerStatus.thisObj.ban(ban_item, ServerStatus.IG("hban_timeout"), "hack URL:" + this.headerLookup.getProperty("HOST", ""));
                ServerStatus.thisObj.kick(ban_item);
                this.done = true;
                return null;
              } 
            } 
          }  
        if (this.headers.elementAt(0).toString().indexOf("CrushAuth=") >= 0 && this.headers.elementAt(0).toString().toUpperCase().indexOf("SET-COOKIE") < 0) {
          String auth2 = this.headers.elementAt(0).toString().substring(this.headers.elementAt(0).toString().indexOf("CrushAuth=") + "CrushAuth=".length());
          if (auth2.indexOf("&") >= 0)
            auth2 = auth2.substring(0, auth2.indexOf("&")); 
          if (auth2.indexOf(" ") >= 0)
            auth2 = auth2.substring(0, auth2.indexOf(" ")); 
          auth2 = auth2.trim();
          Properties cookies = getCookies();
          cookies.put("CrushAuth", auth2);
          Enumeration keys = cookies.keys();
          String newCookies = "";
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            String val = cookies.getProperty(key);
            newCookies = String.valueOf(newCookies) + key + "=" + val + ";";
          } 
          this.headerLookup.put("COOKIE", newCookies);
        } 
      } 
    } catch (SocketTimeoutException e) {
      Log.log("HTTP_SERVER", 3, e);
      if (this.headers.size() == 0) {
        this.done = true;
      } else {
        throw e;
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      if (this.headers.size() == 0) {
        this.done = true;
      } else {
        throw e;
      } 
    } finally {
      try {
        this.sock.setSoTimeout(this.timeoutSeconds * 1000);
      } catch (SocketException socketException) {}
    } 
    this.chunked = (this.headerLookup.getProperty("Transfer-Encoding".toUpperCase(), "").toUpperCase().indexOf("CHUNKED") >= 0);
    if (this.chunked && !this.alreadyChunked) {
      this.alreadyChunked = true;
      is = new UnChunkInputStream(is);
    } 
    if (this.alreadyChunked && !this.chunked) {
      ((UnChunkInputStream)is).reInitialize();
      ((UnChunkInputStream)is).setChunked(false);
    } 
    if (this.alreadyChunked && this.chunked) {
      ((UnChunkInputStream)is).reInitialize();
      ((UnChunkInputStream)is).setChunked(true);
      if (this.headerLookup.getProperty("CONTENT-ENCODING", "").toUpperCase().indexOf("GZIP") >= 0)
        is = new BufferedInputStream((InputStream)new GzipCompressorInputStream(is)); 
    } 
    return is;
  }
  
  public void give_thread_pointer(Thread this_thread) {
    this.this_thread = this_thread;
  }
  
  public void run() {
    this.this_thread = Thread.currentThread();
    String disconnectReason = "";
    try {
      this.original_os = this.sock.getOutputStream();
      int loops = 0;
      while (this.sock != null && this.sock.isConnected() && !this.sock.isClosed() && !this.done) {
        String req_id = "_" + Common.makeBoundary(3);
        StringBuffer blockLog = new StringBuffer();
        this.headers.removeAllElements();
        this.headerLookup.clear();
        this.proxy = this.server_item.getProperty("httpReverseProxy", "");
        if (this.proxy.startsWith("!")) {
          this.proxy = this.proxy.substring(1);
          this.reverseProxyHttps = true;
        } 
        if (!this.proxy.endsWith("/"))
          this.proxy = String.valueOf(this.proxy) + "/"; 
        if (loops == 0)
          this.thread_killer_item = new IdleMonitor(this.thisSession, (new Date()).getTime(), 2L, Thread.currentThread(), this.sock); 
        start_idle_timer(-10);
        this.original_is = getHeaders(this.original_is, blockLog);
        stop_idle_timer();
        Properties cookies = getCookies();
        if (cookies != null && cookies.getProperty("CrushAuth") != null) {
          this.thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(cookies.getProperty("CrushAuth"));
        } else {
          this.thisSession = null;
        } 
        if (this.thisSession == null) {
          this.thisSession = new SessionCrush(this.sock, this.user_number, this.user_ip, this.listen_port, this.listen_ip, this.listen_ip_port, this.server_item);
          this.thisSession.uiPUT("CrushAuth", cookies.getProperty("CrushAuth", ""));
          Common.sockLog(this.sock, "HTTP Session created:" + this.user_number);
        } 
        this.thread_killer_item.calling_session = this.thisSession;
        if (ServerStatus.IG("log_debug_level") >= 2)
          logVector(this.headers, req_id); 
        this.thisSession.put("http_headers", this.headers, false);
        if (this.server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTPS"))
          this.thisSession.uiPUT("secure", "true"); 
        if (loops++ == 0) {
          if (this.headerLookup.containsKey("X-FORWARDED-HOST") && ServerStatus.BG("allow_x_forwarded_host")) {
            String ip = this.thisSession.uiSG("user_ip");
            if (!QuickConnect.validate_ip(ip, this.server_item))
              throw new Exception("BANNED:" + ip); 
          } 
          this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ":" + this.server_item.getProperty("ip", "0.0.0.0") + ":" + this.server_item.getProperty("port", "21") + "] " + SG("Accepting connection from") + ": " + this.thisSession.uiSG("user_ip") + ":" + this.sock.getPort() + this.CRLF, "ACCEPT");
        } 
        this.thisSession.uiPUT("dont_read", "true");
        this.thisSession.uiPUT("dont_write", (new StringBuffer(String.valueOf((blockLog.toString().indexOf("true") >= 0)))).toString());
        this.thisSession.uiPUT("login_date", (new Date()).toString());
        try {
          Thread.sleep(Integer.parseInt(this.thisSession.server_item.getProperty("commandDelayInterval", "0")));
        } catch (Exception exception) {}
        this.keepGoing = true;
        if (this.thisSession.server_item.getProperty("serverType", "FTP").equalsIgnoreCase("HTTPS")) {
          this.secureCookie = "; secure";
        } else {
          this.secureCookie = "";
        } 
        this.secureCookie = String.valueOf(ServerStatus.SG("domain_cookie")) + this.secureCookie;
        handle_http_requests(blockLog, req_id);
        this.thisSession.drain_log();
        Common.sockLog(this.sock, "HTTP Session command processed:" + this.thisSession.uiSG("user_number") + "_" + this.thisSession.uiSG("sock_port") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip"));
        if (!this.thisSession.uiBG("user_logged_in")) {
          ServerStatus.thisObj.remove_user(this.thisSession.user_info);
          SharedSession.find("crushftp.sessions").remove(this.thisSession.getId());
        } 
      } 
      Common.sockLog(this.sock, "HTTP Session close");
      if (this.sock != null)
        this.sock.close(); 
      if (this.reverseSock != null)
        this.reverseSock.close(); 
    } catch (SocketTimeoutException e) {
      Common.sockLog(this.sock, "HTTP Session close:" + e);
      disconnectReason = e.getMessage();
      Log.log("HTTP_SERVER", 3, e);
      this.thisSession.uiPUT("dieing", "true");
    } catch (IOException e) {
      Common.sockLog(this.sock, "HTTP Session close:" + e);
      disconnectReason = e.getMessage();
      Log.log("HTTP_SERVER", 2, e);
      this.thisSession.uiPUT("dieing", "true");
    } catch (Exception e) {
      Common.sockLog(this.sock, "HTTP Session close:" + e);
      disconnectReason = e.getMessage();
      Log.log("HTTP_SERVER", 0, e);
      this.thisSession.uiPUT("dieing", "true");
    } 
    if (!this.thisSession.uiBG("didDisconnect")) {
      this.thisSession.uiPUT("didDisconnect", "true");
      this.thisSession.do_event5("LOGOUT", null);
    } 
    this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] *" + SG("Disconnected") + ":" + disconnectReason + "*", "DISCONNECTED");
    this.thisSession.uiPUT("dieing", "true");
    do_kill();
  }
  
  public void loginCheckAuthToken(boolean requirePassword) {
    try {
      String user = SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user");
      if (ServerStatus.BG("ignore_web_anonymous") && (user == null || user.equalsIgnoreCase("anonymous")))
        return; 
      if (requirePassword && user != null && user.trim().length() > 0 && !this.headerLookup.containsKey("Authorization".toUpperCase()) && !this.headerLookup.containsKey("as2-to".toUpperCase())) {
        this.thisSession.uiPUT("user_name", user);
        this.thisSession.uiPUT("login_date_stamp", this.thisSession.getId());
        if (this.thisSession.uVFS != null)
          this.thisSession.uiPUT("skip_proxy_check", "true"); 
        this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
        SessionCrush session = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
        if (session != null && this.thisSession.uVFS != null) {
          this.thisSession.uiPUT("user_logged_in", "true");
          if (session.containsKey("clientid"))
            this.thisSession.uiPUT("clientid", session.getProperty("clientid")); 
          if (session.containsKey("SESSION_RID"))
            this.thisSession.uiPUT("SESSION_RID", session.getProperty("SESSION_RID")); 
          if (this.thisSession.uVFS.clientCacheFree == null)
            this.thisSession.uVFS.clientCacheFree = new Properties(); 
          if (this.thisSession.uVFS.clientCacheUsed == null)
            this.thisSession.uVFS.clientCacheUsed = new Properties(); 
          if (!session.getProperty("expire_time", "0").equals("0"))
            if (System.currentTimeMillis() > Long.parseLong(session.getProperty("expire_time"))) {
              session.put("expire_time", "0");
              logout_all();
              this.done = true;
              sendRedirect("/WebInterface/login.html");
              write_command_http("Content-Length: 0");
              write_command_http("");
            }  
        } else {
          this.thisSession.login_user_pass(false, false);
          this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
        } 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
  }
  
  public void do_kill() {
    try {
      Worker.startWorker(new Runnable(this) {
            final ServerSessionHTTP this$0;
            
            public void run() {
              try {
                this.this$0.sock.setSoTimeout(2000);
                this.this$0.sock.setSoLinger(true, 2);
                this.this$0.sock.close();
              } catch (Exception exception) {}
            }
          });
    } catch (Exception exception) {}
    this.thisSession.do_kill(this.thread_killer_item);
  }
  
  public String getBoundary() {
    String http_boundary = "";
    String contentType = this.headerLookup.getProperty("Content-Type".toUpperCase(), "");
    if (contentType.toUpperCase().indexOf("BOUNDARY=") >= 0) {
      http_boundary = String.valueOf(contentType.substring(contentType.toUpperCase().indexOf("BOUNDARY=") + "BOUNDARY=".length()).trim()) + ";";
      http_boundary = http_boundary.substring(0, http_boundary.indexOf(";"));
    } 
    return http_boundary;
  }
  
  public long getContentLength() throws Exception {
    String contentLength = this.headerLookup.getProperty("Content-Length".toUpperCase(), "-1").trim();
    return Long.parseLong(contentLength);
  }
  
  public Properties getCookies() {
    Properties cookies = new Properties();
    String s = this.headerLookup.getProperty("COOKIE", "");
    String[] cs = s.split(";");
    for (int x = 0; x < cs.length; x++) {
      if (cs[x].indexOf("=") > 0) {
        String key = cs[x].split("=")[0].trim();
        String val = "";
        if ((cs[x].split("=")).length > 1)
          val = cs[x].split("=")[1].trim(); 
        cookies.put(key, val);
      } 
    } 
    return cookies;
  }
  
  public void passReverseData(InputStream in, OutputStream out, boolean doChunked, String url) {
    byte[] b = new byte[32768];
    int bytes = 1;
    long totalBytes = 0L;
    try {
      while (bytes > 0) {
        if (this.http_len_max > 0L && this.http_len_max - totalBytes < b.length)
          b = new byte[(int)(this.http_len_max - totalBytes)]; 
        bytes = in.read(b);
        if (bytes > 0) {
          totalBytes += bytes;
          if (doChunked)
            out.write((String.valueOf(Long.toHexString(bytes)) + "\r\n").getBytes()); 
          out.write(b, 0, bytes);
          if (doChunked)
            out.write("\r\n".getBytes()); 
          if (bytes > 0)
            this.thisSession.add_log("PROXIED_DATA    :" + (new VRL(url)).safe() + " | Wrote " + bytes + " bytes to server.", "PROXY"); 
          out.flush();
        } 
      } 
      if (doChunked)
        out.write((String.valueOf(Long.toHexString(0L)) + "\r\n").getBytes()); 
      if (doChunked)
        out.write("\r\n".getBytes()); 
      out.flush();
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
  }
  
  public void doReverseProxy(String url) {
    VRL vrl = new VRL(url);
    String headersStr = "";
    for (int x = 0; x < this.headers.size(); x++) {
      String str = this.headers.elementAt(x).toString();
      if (x == 0) {
        String new_path = str.substring(str.indexOf(" ") + 1, str.lastIndexOf(" ")).replaceAll(" ", "%20");
        if (new_path.startsWith("/"))
          new_path = new_path.substring(1); 
        str = String.valueOf(str.substring(0, str.indexOf(" ") + 1)) + vrl.getPath() + new_path + str.substring(str.lastIndexOf(" "));
      } 
      if (!str.toUpperCase().startsWith("Accept-Encoding".toUpperCase())) {
        headersStr = String.valueOf(headersStr) + str + "\r\n";
        this.thisSession.add_log("PROXIED_REQUEST :" + vrl.safe() + " | " + str, "PROXY");
      } 
    } 
    String data = "X-Forwarded-For: " + this.thisSession.uiSG("user_ip");
    this.thisSession.add_log("PROXIED_REQUEST :" + vrl.safe() + " | " + data, "PROXY");
    headersStr = String.valueOf(headersStr) + data + "\r\n";
    try {
      if (this.reverseSock != null && System.currentTimeMillis() - this.reverseSockUsed > 20000L) {
        this.reverseSock.close();
        this.reverseSock = null;
      } 
      if (this.reverseSock == null)
        this.reverseSock = Common.getSockVRL(vrl); 
      BufferedInputStream is = new BufferedInputStream(this.reverseSock.getInputStream());
      OutputStream os = this.reverseSock.getOutputStream();
      os.write(headersStr.getBytes("UTF8"));
      os.write("\r\n".getBytes());
      os.flush();
      this.http_len_max = getContentLength();
      boolean chunked = (this.headerLookup.getProperty("TRANSFER-ENCODING", "").toUpperCase().indexOf("CHUNKED") >= 0);
      if (this.http_len_max > 0L || this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0 || chunked)
        passReverseData(this.original_is, os, chunked, url); 
      this.headers.removeAllElements();
      this.headerLookup.clear();
      headersStr = "";
      is = getHeaders(is, new StringBuffer());
      for (int i = 0; i < this.headers.size(); i++) {
        data = this.headers.elementAt(i).toString();
        this.thisSession.add_log("SERVER_RESPONSE :" + vrl.safe() + " | " + data, "PROXY");
        if (data.toUpperCase().startsWith("LOCATION:")) {
          String loc = this.headerLookup.getProperty("LOCATION");
          if (loc.startsWith("HTTPS:") && this.server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTP")) {
            this.headerLookup.put("LOCATION", "HTTP" + loc.substring(loc.indexOf(":")));
            data = String.valueOf(data.substring(0, data.indexOf(":") + 2)) + this.headerLookup.getProperty("LOCATION");
          } 
          if (data.toUpperCase().startsWith("LOCATION: " + vrl.getProtocol().toUpperCase() + "://" + vrl.getHost().toUpperCase())) {
            VRL vrl2 = new VRL(loc);
            data = "Location: " + vrl2.getPath();
            this.headerLookup.put("LOCATION", data.substring(data.indexOf(":") + 2));
          } 
        } 
        if ((data.toUpperCase().startsWith("COOKIE:") || data.toUpperCase().startsWith("SET-COOKIE:")) && this.server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTP"))
          if (data.indexOf("; secure") >= 0)
            data = Common.replace_str(data, "; secure", "");  
        this.headers.setElementAt(data, i);
        headersStr = String.valueOf(headersStr) + data + "\r\n";
        this.thisSession.add_log("PROXIED_RESPONSE:" + vrl.safe() + " | " + data, "PROXY");
      } 
      this.http_len_max = getContentLength();
      this.original_os.write(headersStr.getBytes("UTF8"));
      this.original_os.write("\r\n".getBytes());
      this.original_os.flush();
      chunked = (this.headerLookup.getProperty("TRANSFER-ENCODING", "").toUpperCase().indexOf("CHUNKED") >= 0);
      if (this.http_len_max > 0L || this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0 || chunked)
        passReverseData(is, this.original_os, chunked, url); 
      if (this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0) {
        this.reverseSock.close();
        this.reverseSock = null;
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    this.reverseSockUsed = System.currentTimeMillis();
    if (this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0 || this.http_len_max < 0L)
      this.done = true; 
  }
  
  public void logVector(Vector v, String req_id) {
    if (this.thisSession.logDateFormat == null)
      this.thisSession.logDateFormat = (SimpleDateFormat)ServerStatus.thisObj.logDateFormat.clone(); 
    synchronized (this.thisSession.logDateFormat) {
      for (int x = 0; x < v.size(); x++) {
        String data = v.elementAt(x).toString();
        String data_l = data.toLowerCase();
        if (data_l.startsWith("cache-control".toLowerCase()) || data_l.startsWith("pragma") || data_l.startsWith("dnt") || data_l.startsWith("accept") || data_l.startsWith("connection") || data_l.startsWith("content-type") || data_l.startsWith("date") || data_l.startsWith("access-control-") || data_l.startsWith("etag") || data_l.startsWith("referer") || data_l.startsWith("if-modified-") || data_l.startsWith("if-none-") || data_l.startsWith("origin") || data_l.startsWith("x-requested-") || data_l.startsWith("upgrade-insecure-")) {
          if (ServerStatus.IG("log_debug_level") >= 3)
            this.thisSession.add_log_formatted(data, "POST", req_id); 
        } else if (data_l.startsWith("user-agent".toLowerCase())) {
          if (ServerStatus.IG("log_debug_level") >= 1)
            this.thisSession.add_log_formatted(data, "POST", req_id); 
        } else if (data_l.startsWith("host")) {
          if (ServerStatus.IG("log_debug_level") >= 2)
            this.thisSession.add_log_formatted(data, "POST", req_id); 
        } else if (data_l.startsWith("content-length") || data_l.startsWith("cookie")) {
          if (ServerStatus.IG("log_debug_level") >= 1)
            this.thisSession.add_log_formatted(data, "POST", req_id); 
        } else {
          this.thisSession.add_log_formatted(data, "POST", req_id);
        } 
      } 
    } 
  }
  
  public void handle_http_requests(StringBuffer blockLog, String req_id) throws Exception {
    if (proppatches == null && (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop")).exists())
      try {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop")));
        proppatches = (Properties)ois.readObject();
        ois.close();
      } catch (Exception e) {
        (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches_BAD.prop"));
      }  
    if (proppatches == null)
      proppatches = new Properties(); 
    if (locktokens == null && (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop")).exists())
      try {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop")));
        locktokens = (Properties)ois.readObject();
        ois.close();
      } catch (Exception e) {
        (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens_BAD.prop"));
      }  
    if (locktokens == null)
      locktokens = new Properties(); 
    if (this.ssa == null)
      this.ssa = new ServerSessionAJAX(this); 
    String header0 = "";
    String http_boundary = getBoundary();
    this.http_len_max = getContentLength();
    Properties request = new Properties();
    String domain = "";
    if (this.headerLookup.containsKey("X-PROXY_USER_IP"))
      this.thisSession.uiPUT("user_ip", this.headerLookup.getProperty("X-PROXY_USER_IP")); 
    if (this.headerLookup.containsKey("X-PROXY_USER_PORT"))
      this.thisSession.uiPUT("user_port", this.headerLookup.getProperty("X-PROXY_USER_PORT")); 
    if (this.headerLookup.containsKey("X-PROXY_USER_PROTOCOL"))
      this.thisSession.uiPUT("user_protocol", this.headerLookup.getProperty("X-PROXY_USER_PROTOCOL")); 
    if (this.headerLookup.containsKey("X-PROXY_BIND_IP"))
      this.thisSession.uiPUT("bind_ip", this.headerLookup.getProperty("X-PROXY_BIND_IP")); 
    if (this.headerLookup.containsKey("X-PROXY_BIND_PORT"))
      this.thisSession.uiPUT("bind_port2", this.headerLookup.getProperty("X-PROXY_BIND_PORT")); 
    if (this.headerLookup.containsKey("X-PROXY_HEADER_USER-AGENT"))
      this.headerLookup.put("USER-AGENT", this.headerLookup.getProperty("X-PROXY_HEADER_USER-AGENT")); 
    if (this.done) {
      logVector(this.headers, req_id);
      return;
    } 
    try {
      this.thisSession.user_info.put("header_user-agent", this.headerLookup.getProperty("User-Agent".toUpperCase(), "").trim());
      if (this.headerLookup.containsKey("HOST")) {
        this.hostString = this.headerLookup.getProperty("HOST").trim();
        this.thisSession.uiPUT("listen_ip", this.hostString);
        if (this.hostString.indexOf(":") >= 0)
          this.thisSession.uiPUT("listen_ip", this.hostString.substring(0, this.hostString.indexOf(":")).trim()); 
        domain = this.hostString;
        if (domain.indexOf(":") >= 0)
          domain = domain.substring(0, domain.indexOf(":")); 
      } 
      if (this.headerLookup.containsKey("X-FORWARDED-HOST") && ServerStatus.BG("allow_x_forwarded_host")) {
        this.hostString = this.headerLookup.getProperty("X-FORWARDED-HOST").trim();
        this.thisSession.uiPUT("listen_ip", this.hostString);
        if (this.hostString.indexOf(":") >= 0)
          this.thisSession.uiPUT("listen_ip", this.hostString.substring(0, this.hostString.indexOf(":")).trim()); 
        domain = this.hostString;
        if (domain.indexOf(":") >= 0)
          domain = domain.substring(0, domain.indexOf(":")); 
        if (this.thisSession.uiSG("user_ip").equals("127.0.0.1"))
          this.thisSession.uiPUT("user_ip", domain); 
      } 
      if (this.headerLookup.containsKey("X-Forwarded-For".toUpperCase())) {
        String tempIp = this.headerLookup.getProperty("X-Forwarded-For".toUpperCase()).trim();
        if (tempIp.indexOf(":") >= 0)
          tempIp = tempIp.split(":")[0]; 
        if (tempIp.indexOf(",") >= 0)
          tempIp = tempIp.split(",")[0]; 
        this.thisSession.uiPUT("user_ip", tempIp.trim());
      } 
      boolean ipAllowed = Common.check_ip((Vector)ServerStatus.server_settings.get("ip_restrictions"), this.thisSession.uiSG("user_ip"));
      if (ipAllowed && this.server_item.get("ip_restrictions") != null && !Common.check_ip((Vector)this.server_item.get("ip_restrictions"), this.thisSession.uiSG("user_ip")))
        ipAllowed = false; 
      if (!ipAllowed) {
        this.done = true;
        ServerStatus.thisObj.append_log("!" + (new Date()).toString() + "!  ---" + ServerStatus.SG("BANNED IP CONNECTION TERMINATED") + "---:" + this.thisSession.uiSG("user_ip"), "DENIAL");
        ServerStatus.put_in("failed_logins", ServerStatus.IG("failed_logins") + 1);
        write_command_http("HTTP/1.1 429 Banned");
        write_command_http("Connection: close");
        if (!ServerStatus.SG("X-Frame-Options").equals(""))
          write_command_http("X-Frame-Options: " + ServerStatus.SG("X-Frame-Options")); 
        write_command_http("");
        this.done = true;
        return;
      } 
      Properties cookies = getCookies();
      if (cookies.containsKey("CrushAuth") && !cookies.getProperty("CrushAuth").trim().equals("")) {
        if (!this.thisSession.getId().equals(cookies.getProperty("CrushAuth"))) {
          this.thisSession.uiPUT("user_logged_in", "false");
          if (this.thisSession.uVFS != null)
            this.thisSession.uVFS.free(); 
          this.thisSession.uiPUT("CrushAuth", cookies.getProperty("CrushAuth"));
        } 
        String user = SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user");
        if (user != null)
          this.thisSession.uiPUT("user_name", user); 
      } else if (!this.headerLookup.containsKey("Authorization".toUpperCase())) {
        this.writeCookieAuth = true;
        createCookieSession(true);
      } 
      setupSession();
      if (this.server_item.getProperty("port", "0").startsWith("55580") && this.thisSession.uiSG("user_ip").equals("127.0.0.1"))
        this.thisSession.uiPUT("user_ip", SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_ip", this.thisSession.uiSG("user_ip"))); 
      if (this.server_item.getProperty("port", "0").startsWith("55580") && this.thisSession.uiSG("user_ip").equals("127.0.0.1"))
        this.thisSession.uiPUT("user_ip", SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getProperty("clientid") + "_ip", this.thisSession.uiSG("user_ip"))); 
      ipAllowed = Common.check_ip((Vector)ServerStatus.server_settings.get("ip_restrictions"), this.thisSession.uiSG("user_ip"));
      if (!ipAllowed) {
        this.done = true;
        ServerStatus.thisObj.append_log("!" + (new Date()).toString() + "!  ---" + ServerStatus.SG("BANNED IP CONNECTION TERMINATED") + "---:" + this.thisSession.uiSG("user_ip"), "DENIAL");
        ServerStatus.put_in("failed_logins", ServerStatus.IG("failed_logins") + 1);
        write_command_http("HTTP/1.1 429 Banned");
        write_command_http("Connection: close");
        if (!ServerStatus.SG("X-Frame-Options").equals(""))
          write_command_http("X-Frame-Options: " + ServerStatus.SG("X-Frame-Options")); 
        write_command_http("");
        this.done = true;
        return;
      } 
      if (this.headers.size() > 0) {
        if (this.thisSession.server_item.getProperty("https_redirect", "false").equalsIgnoreCase("true") && this.thisSession.server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTP")) {
          logVector(this.headers, req_id);
          String path = this.headers.elementAt(0).toString();
          int endPos = path.lastIndexOf(" HTTP");
          if (endPos < 0)
            endPos = path.length() - 1; 
          path = path.substring(path.indexOf(" ") + 1, endPos);
          sendHttpsRedirect(path);
          write_command_http("Connection: close");
          write_command_http("");
          this.done = true;
          return;
        } 
        header0 = this.headers.elementAt(0).toString();
        if (header0.indexOf("\r") >= 0)
          header0 = "GET / HTTP/1.1"; 
        String request_path = header0.substring(header0.indexOf(" ") + 1, header0.lastIndexOf(" "));
        request_path = Common.dots(request_path);
        String reverseProxyUrl = null;
        String reverseProxyPath = null;
        if (!this.server_item.getProperty("reverseProxyUrl", "").equals("")) {
          String[] domains = this.server_item.getProperty("reverseProxyDomain", "*").split("\\n");
          for (int i = 0; i < domains.length; i++) {
            if (domains[i].equals(""))
              domains[i] = "*"; 
            if (Common.do_search(domains[i].trim(), domain, false, 0)) {
              reverseProxyUrl = this.server_item.getProperty("reverseProxyUrl", "").split("\\n")[i].trim();
              reverseProxyPath = this.server_item.getProperty("reverseProxyPath", "/").split("\\n")[i].trim();
              if (reverseProxyPath.equals(""))
                reverseProxyPath = "/"; 
              if (request_path.startsWith(reverseProxyPath))
                break; 
            } 
          } 
        } 
        if (reverseProxyUrl != null && request_path.startsWith(reverseProxyPath)) {
          request_path = request_path.substring(reverseProxyPath.length() - 1);
          header0 = String.valueOf(header0.substring(0, header0.indexOf(" "))) + " " + request_path + header0.substring(header0.lastIndexOf(" "));
          this.headers.setElementAt(header0, 0);
          for (int i = 0; i < this.headers.size(); i++) {
            if (this.headers.elementAt(i).toString().toUpperCase().startsWith("HOST:")) {
              VRL vrl = new VRL(reverseProxyUrl);
              this.headers.setElementAt("Host: " + vrl.getHost() + ":" + vrl.getPort(), i);
              break;
            } 
          } 
          logVector(this.headers, req_id);
          doReverseProxy(reverseProxyUrl);
          return;
        } 
        boolean processWebInterface = false;
        if (header0.toUpperCase().startsWith("GET /WEBINTERFACE/") || header0.toUpperCase().startsWith("GET /FAVICON.ICO") || header0.toUpperCase().startsWith("HEAD /WEBINTERFACE/CRUSHTUNNEL.JAR") || header0.toUpperCase().startsWith("GET /WEBINTERFACE/CRUSHTUNNEL.JAR") || (header0.toUpperCase().startsWith("GET /PLUGINS/LIB/") && header0.toUpperCase().indexOf(".JAR HTTP/") >= 0) || (header0.toUpperCase().startsWith("HEAD /PLUGINS/LIB/") && header0.toUpperCase().indexOf(".JAR HTTP/") >= 0))
          processWebInterface = true; 
        if (header0.toUpperCase().startsWith("GET /WEBINTERFACE/FUNCTION/"))
          processWebInterface = false; 
        if (header0.toUpperCase().startsWith("GET /WEBINTERFACE/CUSTOM.JS"))
          processWebInterface = false; 
        if (header0.toUpperCase().startsWith("GET /WEBINTERFACE/CUSTOM.CSS"))
          processWebInterface = false; 
        if (header0.startsWith("GET /.well-known/acme-challenge/"))
          processWebInterface = true; 
        if (blockLog.toString().indexOf("true") >= 0) {
          int h = 8;
        } else {
          logVector(this.headers, req_id);
        } 
        if (processWebInterface) {
          this.thisSession.add_log_formatted(header0, "POST", req_id);
          ServerSessionHTTPWI.serveFile(this, this.headers, this.original_os, false, null);
          if (this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0)
            this.done = true; 
          return;
        } 
        Vector items = new Vector();
        if (header0.toUpperCase().startsWith("POST " + this.proxy + "U/") || header0.toUpperCase().startsWith("POST /U/")) {
          try {
            parseUploadSegment(http_boundary, this.http_len_max, req_id, header0.substring(header0.indexOf("/U/") + 3, header0.lastIndexOf(" ")));
            write_command_http("HTTP/1.1 200 OK");
            write_standard_headers();
            write_command_http("Content-Length: 0");
            write_command_http("");
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 1, e);
            this.keepGoing = false;
            this.done = true;
            write_command_http("HTTP/1.1 400 CHUNK SIZE FAILURE");
            write_standard_headers();
            write_command_http("Content-Length: " + e.getMessage().length() + '\002');
            write_command_http("");
            write_command_http(e.getMessage());
          } 
          return;
        } 
        if (header0.toUpperCase().startsWith("POST " + this.proxy + "D/") || header0.toUpperCase().startsWith("POST /D/") || header0.toUpperCase().startsWith("GET /D/")) {
          try {
            parseDownloadSegment(http_boundary, req_id, header0.substring(header0.indexOf("/D/") + 3, header0.lastIndexOf(" ")));
          } catch (Exception e) {
            this.done = true;
            Log.log("HTTP_SERVER", 0, e);
            write_command_http("HTTP/1.1 200 OK");
            write_standard_headers();
            write_command_http("Content-Length: 0");
            write_command_http("");
          } 
          return;
        } 
        if (header0.toUpperCase().startsWith("POST ") && header0.toUpperCase().indexOf("/CRUSH_STREAMING_HTTP_PROXY") < 0 && header0.indexOf("/put?filename") < 0)
          if (!http_boundary.equals("") && !isAS2()) {
            items = parsePostArguments(http_boundary, this.http_len_max, this.thisSession.uiBG("user_logged_in"), req_id);
          } else {
            loginCheckHeaderAuth();
            loginDNSAuth(domain);
            loginCheckClientAuth();
            Properties p = new Properties();
            this.ssa.buildPostItem(p, this.http_len_max, this.headers, req_id);
            items.addElement(p);
          }  
        for (int x = 0; x < items.size(); x++) {
          Properties pp = items.elementAt(x);
          request.putAll(pp);
        } 
        processMiniURLs(header0, ServerStatus.VG("miniURLs"));
        if (this.thisSession.user_info.getProperty("miniUrlLogin", "false").equals("false"))
          processMiniURLs(header0, ServerStatus.VG("miniURLs_dmz")); 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    Properties urlRequestItems = new Properties();
    if (this.headers.elementAt(0).toString().indexOf("?") >= 0) {
      if (Common.url_decode(header0).indexOf("&p=none") >= 0) {
        header0 = Common.url_decode(header0);
        this.headers.setElementAt(Common.url_decode(this.headers.elementAt(0).toString()), 0);
      } 
      String[] tokenStr = this.headers.elementAt(0).toString().substring(header0.indexOf("?") + 1, this.headers.elementAt(0).toString().lastIndexOf(" ")).split("&");
      for (int xx = 0; xx < tokenStr.length; xx++) {
        if (tokenStr[xx].indexOf("=") >= 0) {
          String key = tokenStr[xx].substring(0, tokenStr[xx].indexOf("=")).trim();
          String val = tokenStr[xx].substring(tokenStr[xx].indexOf("=") + 1).trim();
          request.put(Common.url_decode(key), Common.url_decode(val));
          if (!key.equals("path") && !key.equals("w") && !key.equals("u") && !key.equals("p"))
            urlRequestItems.put(key.trim(), val.trim()); 
        } 
      } 
      Properties tmp2 = new Properties();
      Enumeration keys = urlRequestItems.keys();
      while (keys.hasMoreElements()) {
        String key = (String)keys.nextElement();
        tmp2.put(key, urlRequestItems.getProperty(key));
        tmp2.put("post_" + key, urlRequestItems.getProperty(key));
      } 
      if (!tmp2.getProperty("redirect_url", "").equals("") && !tmp2.getProperty("redirect_url", "").startsWith(ServerStatus.SG("http_redirect_base"))) {
        tmp2.remove("redirect_url");
        tmp2.remove("post_redirect_url");
        urlRequestItems.remove("redirect_url");
        urlRequestItems.remove("post_redirect_url");
      } 
      for (int x = 0; x < 10; x++) {
        try {
          tmp2.putAll(this.thisSession.user_info);
          this.thisSession.putAll(tmp2);
          break;
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 2, e);
          Thread.sleep(100L);
        } 
      } 
      if (header0.indexOf(".js?_=") >= 0)
        header0 = String.valueOf(header0.substring(0, header0.indexOf(".js?_="))) + ".js" + header0.substring(header0.lastIndexOf(" ")); 
      this.headers.setElementAt(header0, 0);
    } 
    String instancePath = this.headers.elementAt(0).toString();
    request.put("instance", "");
    if (instancePath.indexOf("/WebInterface/function/") >= 0 && instancePath.indexOf("/WebInterface/function/?") < 0 && instancePath.indexOf("/WebInterface/function/ ") < 0) {
      if (instancePath.indexOf("?") >= 0) {
        instancePath = instancePath.substring(instancePath.indexOf("/WebInterface/function/") + "/WebInterface/function/".length(), instancePath.indexOf("?") - 1);
      } else {
        instancePath = instancePath.substring(instancePath.indexOf("/WebInterface/function/") + "/WebInterface/function/".length(), instancePath.lastIndexOf(" ") - 1);
      } 
      request.put("instance", instancePath);
    } 
    if (this.thisSession.uiSG("user_ip").equals("127.0.0.1") && request.getProperty("CrushAuth") != null) {
      this.thisSession.uiPUT("CrushAuth", request.getProperty("CrushAuth"));
      this.writeCookieAuth = true;
    } 
    if (this.done)
      return; 
    if (this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0)
      this.done = true; 
    this.cacheHeader = "";
    this.thisSession.uiPUT("start_resume_loc", "0");
    this.thisSession.uiPUT("request", request);
    if (this.thisSession.server_item.getProperty("linkedServer", "").equals("@AutoHostHttp") && this.thisSession.uiSG("listen_ip_port").equals("@AutoHostHttp")) {
      try {
        Vector v = ServerStatus.VG("login_page_list");
        for (int x = 0; x < v.size(); x++) {
          Properties p = v.elementAt(x);
          if (Common.do_search(p.getProperty("domain"), domain, false, 0)) {
            String stem = p.getProperty("page").substring(0, p.getProperty("page").lastIndexOf("."));
            this.thisSession.server_item = (Properties)this.thisSession.server_item.clone();
            this.thisSession.server_item.put("linkedServer", stem);
            this.thisSession.uiPUT("listen_ip_port", stem);
            break;
          } 
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
    } else if (this.thisSession.server_item.getProperty("linkedServer", "").equals("@AutoHostHttp") && !this.thisSession.uiSG("listen_ip_port").equals("@AutoHostHttp")) {
      this.thisSession.server_item = (Properties)this.thisSession.server_item.clone();
      this.thisSession.server_item.put("linkedServer", this.thisSession.uiSG("listen_ip_port"));
    } 
    boolean requirePassword = loginCheckClientAuth();
    if (this.ssa.processItemAnonymous(request, urlRequestItems, req_id))
      return; 
    header0 = this.headers.elementAt(0).toString();
    loginCheckAuthToken(requirePassword);
    loginCheckHeaderAuth();
    loginDNSAuth(domain);
    loginCheckHttpTrustHeaders();
    if (this.thisSession.uiBG("user_logged_in"))
      fixRootDir(null, false); 
    if (header0.toUpperCase().startsWith("OPTIONS ")) {
      this.writeCookieAuth = false;
      write_command_http("HTTP/1.1 200 OK");
      write_command_http("Pragma: no-cache");
      boolean ok = true;
      if (this.thisSession.uiBG("user_logged_in"))
        ok = (Common.check_protocol("WEBDAV", SG("allowed_protocols")) >= 0); 
      if (this.thisSession.server_item.getProperty("allow_webdav", "true").equalsIgnoreCase("true") && ok) {
        write_command_http("x-responding-server: sslngn018");
        write_command_http("X-dmUser: " + SG("username"));
        write_command_http("MS-Author-Via: DAV");
        write_command_http("Allow: GET, HEAD, OPTIONS, PUT, POST, COPY, PROPFIND, DELETE, LOCK, MKCOL, MOVE, PROPPATCH, UNLOCK, ACL, TRACE");
        write_command_http("DAV: 1,2, access-control, <http://apache.org/dav/propset/fs/1>");
        write_command_http("Content-Type: text/plain");
      } else {
        write_command_http("Allow: GET, HEAD, OPTIONS, PUT, POST");
      } 
      this.done = true;
      write_standard_headers();
      write_command_http("Content-Length: 0");
      write_command_http("");
      this.keepGoing = false;
    } 
    if (!this.keepGoing) {
      consumeBadData();
      this.done = true;
      return;
    } 
    if (this.ssa.getUserName(request))
      return; 
    if (!loginCheckAnonymousAuth(header0))
      return; 
    if (request.getProperty("skip_login", "").equals("true")) {
      sendRedirect(header0.substring(header0.indexOf(" ") + 1, header0.lastIndexOf(" ")).trim());
      write_command_http("Content-Length: 0");
      write_command_http("");
    } else if (!this.thisSession.uiBG("user_logged_in") || this.thisSession.uiSG("user_name").equals("")) {
      if (header0.startsWith("CONNECT ") && this.server_item.getProperty("allow_proxy", "false").equals("true")) {
        write_command_http("HTTP/1.1 407 Proxy Authentication Required");
        write_standard_headers();
        write_command_http("Proxy-Authenticate: Basic realm=\"0.0.0.0\"");
        write_command_http("Connection: close");
        write_command_http("");
      } else if ((header0.toUpperCase().startsWith("GET HTTP:/") || header0.toUpperCase().startsWith("POST HTTP:/")) && this.server_item.getProperty("allow_proxy", "false").equals("true")) {
        write_command_http("HTTP/1.1 407 Proxy Authentication Required");
        write_standard_headers();
        write_command_http("Proxy-Authenticate: Basic realm=\"0.0.0.0\"");
        write_command_http("Connection: close");
        write_command_http("");
      } else if (webDavAgents.indexOf(this.thisSession.uiSG("header_user-agent")) < 0 && (header0.toUpperCase().startsWith("GET ") || header0.toUpperCase().startsWith("POST "))) {
        if (!this.writeCookieAuth && !request.containsKey("username") && !request.containsKey("password"))
          this.thisSession.killSession(); 
        if (header0.indexOf("/WebInterface/") >= 0) {
          write_command_http("HTTP/1.1 404 Not Found");
          String html404 = ServerStatus.SG("web404Text");
          html404 = ServerStatus.thisObj.change_vars_to_values(html404, this.thisSession);
          write_command_http("Content-Length: " + ((html404.getBytes("UTF8")).length + 2));
          write_standard_headers();
          write_command_http("");
          write_command_http(html404);
          return;
        } 
        if (!header0.startsWith("GET / ")) {
          if (request.getProperty("command", "").equals("logout")) {
            this.writeCookieAuth = false;
            this.deleteCookieAuth = true;
            write_command_http("HTTP/1.1 200 OK");
            write_command_http("Content-Length: 0");
            write_standard_headers();
            write_command_http("");
            this.done = true;
            return;
          } 
          sendRedirect("/WebInterface/login.html?path=" + Common.replace_str(header0.substring(4, header0.lastIndexOf(" ")).trim(), "+", "%2B"));
        } else {
          this.done = true;
          sendRedirect("/WebInterface/login.html");
        } 
        write_command_http("Content-Length: 0");
        write_command_http("");
      } else {
        DEAUTH();
      } 
    } else {
      if (header0.startsWith("CONNECT ") && this.server_item.getProperty("allow_proxy", "false").equals("true") && SG("site").indexOf("(SITE_PROXY)") >= 0) {
        String host = header0.substring(header0.indexOf(" "), header0.lastIndexOf(" ")).trim();
        int port = 443;
        if (host.indexOf(":") >= 0) {
          port = Integer.parseInt(host.substring(host.lastIndexOf(":") + 1).trim());
          host = host.substring(0, host.lastIndexOf(":"));
        } 
        this.done = true;
        Socket sock2 = new Socket(host, port);
        write_command_http("HTTP/1.1 200 OK");
        write_standard_headers();
        write_command_http("");
        Common.streamCopier(this.sock, sock2, this.sock.getInputStream(), sock2.getOutputStream(), true, true, true);
        Common.streamCopier(this.sock, sock2, sock2.getInputStream(), this.sock.getOutputStream(), false, true, true);
        return;
      } 
      if ((header0.toUpperCase().startsWith("GET HTTP:/") || header0.toUpperCase().startsWith("POST HTTP:/")) && this.server_item.getProperty("allow_proxy", "false").equals("true") && SG("site").indexOf("(SITE_PROXY)") >= 0) {
        VRL u = new VRL(header0.substring(header0.indexOf(" "), header0.lastIndexOf(" ")).trim());
        if (header0.startsWith("POST http:/")) {
          header0 = "POST " + u.getPath() + " HTTP/1.1";
        } else {
          header0 = "GET " + u.getPath() + " HTTP/1.1";
        } 
        this.headers.setElementAt(header0, 0);
        for (int x = this.headers.size() - 1; x >= 0; x--) {
          if (this.headers.elementAt(x).toString().toUpperCase().startsWith("PROXY-") || this.headers.elementAt(x).toString().toUpperCase().startsWith("X-"))
            this.headers.removeElementAt(x); 
        } 
        doReverseProxy(u.toString());
        return;
      } 
      if (header0.toUpperCase().startsWith("GET ") && request.getProperty("command", "").equals("") && !request.getProperty("path", "").equals("")) {
        if (this.thisSession.BG("DisallowListingDirectories")) {
          sendRedirect(request.getProperty("path", ""));
        } else {
          sendRedirect("/#" + request.getProperty("path", ""));
        } 
        write_command_http("Content-Length: 0");
        write_command_http("");
        return;
      } 
      fixRootDir(domain, false);
      String action = "";
      String ifnonematch = "0";
      String move_destination = "";
      String overwrite = "";
      String depth = "0";
      boolean headersOnly = header0.toUpperCase().startsWith("HEAD ");
      VRL otherFile = null;
      String user_dir = header0.substring(header0.indexOf(" ") + 1, header0.lastIndexOf(" ")).replace('\\', '/');
      if (user_dir.startsWith("/put?filename=")) {
        user_dir = user_dir.substring("/put?filename=".length());
        if (header0.startsWith("POST "))
          header0 = "PUT " + header0.substring("POST ".length()); 
        if (header0.endsWith(" HTTP/1.0"))
          this.done = true; 
      } 
      if (!user_dir.toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
        user_dir = String.valueOf(this.thisSession.SG("root_dir")) + (user_dir.startsWith("/") ? user_dir.substring(1) : user_dir); 
      if (!user_dir.startsWith("/"))
        user_dir = "/" + user_dir; 
      user_dir = Common.dots(user_dir);
      if (user_dir.startsWith(String.valueOf(this.thisSession.SG("root_dir")) + "WebInterface/"))
        user_dir = user_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
      if (!user_dir.startsWith("/WebInterface/function/"))
        cd(user_dir); 
      this.thisSession.uiPUT("last_logged_command", header0);
      long start_resume_loc = 0L;
      Vector byteRanges = new Vector();
      if (this.headerLookup.getProperty("RANGE", "").toUpperCase().indexOf("BYTES=") >= 0 || request.containsKey("range")) {
        String amount = String.valueOf(this.headerLookup.getProperty("RANGE", "").toUpperCase()) + ",";
        if (request.containsKey("range"))
          amount = request.getProperty("range"); 
        StringTokenizer st = new StringTokenizer(amount, ",");
        while (st.hasMoreElements()) {
          String amountStart = st.nextElement().toString().trim();
          amountStart = amountStart.substring(amountStart.toUpperCase().indexOf("=") + 1).trim();
          String amountEnd = amountStart.substring(amountStart.indexOf("-") + 1).trim();
          amountStart = amountStart.substring(0, amountStart.indexOf("-")).trim();
          if (amountStart.equals(""))
            amountStart = "0"; 
          Properties p = new Properties();
          p.put("start", amountStart);
          p.put("end", amountEnd);
          byteRanges.addElement(p);
          if (byteRanges.size() == 1) {
            this.thisSession.uiPUT("start_resume_loc", amountStart);
            start_resume_loc = Long.parseLong(amountStart);
          } 
        } 
      } 
      if (header0.toUpperCase().startsWith("GET ") || header0.toUpperCase().startsWith("HEAD ")) {
        this.thisSession.runPlugin("check_path", null);
        if (pwd().endsWith("/") && pwd().toLowerCase().indexOf("crushftp.jnlp") < 0 && !headersOnly) {
          action = "serve dir";
        } else {
          action = "serve file";
          boolean dirOK = false;
          try {
            Properties item = this.thisSession.uVFS.get_item(pwd());
            if (item != null && item.getProperty("type", "").equals("DIR") && !pwd().endsWith("/"))
              if (this.headers.toString().toUpperCase().indexOf("DREAMWEAVER") >= 0) {
                cd(String.valueOf(pwd()) + "/");
                action = "serve dir";
                dirOK = true;
              } else {
                sendRedirect(String.valueOf(pwd()) + "/");
                write_command_http("Content-Length: 0");
                write_command_http("");
                return;
              }  
            if (!dirOK) {
              boolean ok = (this.thisSession.check_access_privs(pwd(), "RETR") && Common.filter_check("D", Common.last(pwd()), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")));
              if (ok && item != null)
                otherFile = new VRL(item.getProperty("url")); 
              if (otherFile == null && pwd().toUpperCase().endsWith(".ZIP")) {
                cd(pwd().substring(0, pwd().length() - 4));
                ok = (this.thisSession.check_access_privs(pwd(), "RETR") && Common.filter_check("D", Common.last(pwd()), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")));
                item = this.thisSession.uVFS.get_item(pwd());
                if (ok) {
                  otherFile = new VRL(item.getProperty("url"));
                  otherFile = new VRL(String.valueOf(Common.all_but_last(otherFile.toString())) + otherFile.getName() + ".zip");
                } else {
                  cd(String.valueOf(pwd()) + ".zip");
                } 
              } 
            } 
          } catch (Exception e) {
            Log.log("HTTP_SERVER", 1, e);
          } 
          if (header0.toUpperCase().startsWith("GET /WEBINTERFACE/") || header0.toUpperCase().startsWith("HEAD /WEBINTERFACE/")) {
            String theFile = user_dir;
            if (theFile.indexOf("?") >= 0)
              theFile = theFile.substring(0, theFile.indexOf("?")); 
            otherFile = new VRL((new File_S(String.valueOf(System.getProperty("crushftp.web")) + theFile)).toURI().toURL().toExternalForm());
          } 
        } 
      } else if (header0.toUpperCase().startsWith("POST ")) {
        action = "process post";
      } else if (header0.toUpperCase().startsWith("LOCK ") || header0.toUpperCase().startsWith("UNLOCK ") || header0.toUpperCase().startsWith("DELETE ") || header0.toUpperCase().startsWith("MKCOL ") || header0.toUpperCase().startsWith("PROPFIND ") || header0.toUpperCase().startsWith("MOVE ") || header0.toUpperCase().startsWith("PROPPATCH ") || header0.toUpperCase().startsWith("DMMKPATH ") || header0.toUpperCase().startsWith("DMMKPATHS ") || header0.toUpperCase().startsWith("ACL ") || header0.toUpperCase().startsWith("COPY ")) {
        if (webDavAgents.indexOf(this.thisSession.uiSG("header_user-agent")) < 0)
          if (ServerStatus.BG("webdav_agent_learning"))
            webDavAgents.addElement(this.thisSession.uiSG("header_user-agent"));  
        action = header0.toLowerCase().substring(0, header0.indexOf(" "));
        boolean ok = (Common.check_protocol("WEBDAV", this.thisSession.SG("allowed_protocols")) >= 0);
        if (this.thisSession.server_item.getProperty("allow_webdav", "true").equalsIgnoreCase("false") || !ok)
          action = "serve file"; 
        cd(user_dir);
      } else if (header0.toUpperCase().startsWith("PUT ")) {
        action = header0.toLowerCase().substring(0, header0.indexOf(" "));
      } 
      if (this.headerLookup.getProperty("X-WEBDAV-METHOD", "").toUpperCase().indexOf("ACL") >= 0)
        action = this.headerLookup.getProperty("X-WEBDAV-METHOD", "").toLowerCase(); 
      if (this.headerLookup.containsKey("IF-MODIFIED-SINCE")) {
        ifnonematch = this.headerLookup.getProperty("IF-MODIFIED-SINCE").trim();
        try {
          ifnonematch = (new StringBuffer(String.valueOf(this.sdf_rfc1123.parse(ifnonematch).getTime()))).toString();
        } catch (Exception exception) {}
      } 
      if (this.headerLookup.containsKey("IF-NONE-MATCH"))
        ifnonematch = this.headerLookup.getProperty("IF-NONE-MATCH").trim(); 
      if (this.headerLookup.containsKey("DEPTH"))
        try {
          depth = this.headerLookup.getProperty("DEPTH").trim();
        } catch (Exception exception) {} 
      if (this.headerLookup.containsKey("DESTINATION"))
        try {
          move_destination = this.headerLookup.getProperty("DESTINATION").trim();
        } catch (Exception exception) {} 
      if (this.headerLookup.containsKey("X-TARGET-HREF"))
        try {
          move_destination = this.headerLookup.getProperty("X-TARGET-HREF").trim();
        } catch (Exception exception) {} 
      if (this.headerLookup.containsKey("OVERWRITE"))
        try {
          overwrite = this.headerLookup.getProperty("OVERWRITE").trim();
        } catch (Exception exception) {} 
      if (header0.toUpperCase().startsWith("POST ") && header0.toUpperCase().indexOf("/CRUSH_STREAMING_HTTP_PROXY2/") >= 0) {
        Tunnel2.setMaxRam(ServerStatus.IG("tunnel_ram_cache"));
        Tunnel2 t = Tunnel2.getTunnel(this.thisSession.getId());
        if (request.getProperty("writing").equals("true")) {
          Thread.currentThread().setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " CRUSH_STREAMING_HTTP_PROXY:HTTPReader");
          this.sock.setSoTimeout(60000);
          long chunkTimer = System.currentTimeMillis();
          int chunkCount = 0;
          int chunkCountTotal = 0;
          while (SharedSession.find("crushftp.usernames").containsKey(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user")) {
            try {
              Chunk c = Chunk.parse(this.original_is);
              if (c == null) {
                this.done = true;
                break;
              } 
              if (c.isCommand() && c.getCommand().startsWith("A:"))
                t.localAck.remove(c.getCommand().split(":")[1]); 
              if (c.isCommand() && !c.getCommand().startsWith("A:"))
                this.thisSession.add_log_formatted("Chunk Command:" + c.getCommand(), "POST", req_id); 
              if (t != null) {
                int loops = 0;
                Queue q = t.getQueue(c.id);
                if (q == null)
                  q = t.getOldQueue(c.id); 
                while (q == null && !c.isCommand() && loops++ < 500) {
                  q = t.getQueue(c.id);
                  if (q != null)
                    break; 
                  Thread.sleep(10L);
                } 
                if (q != null) {
                  q.writeRemote(c);
                  Tunnel2.writeAck(c, q, t);
                } 
              } 
              chunkCountTotal++;
              chunkCount++;
              this.thisSession.active();
              if (System.currentTimeMillis() - chunkTimer > 10000L) {
                this.thisSession.add_log_formatted("read " + chunkCount + " chunks", "POST", req_id);
                chunkTimer = System.currentTimeMillis();
                chunkCount = 0;
                if (Tunnel2.getTunnel(this.thisSession.getId()) == null) {
                  this.done = true;
                  break;
                } 
              } 
            } catch (SocketTimeoutException e) {
              this.done = true;
              break;
            } 
          } 
          this.thisSession.add_log_formatted("read total chunks:" + chunkCountTotal, "POST", req_id);
        } else {
          Thread.currentThread().setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " CRUSH_STREAMING_HTTP_PROXY:HTTPWriter");
          this.done = true;
          write_command_http("HTTP/1.1 200 OK");
          boolean doChunked = false;
          if (doChunked)
            write_command_http("Transfer-Encoding: chunked"); 
          write_standard_headers();
          write_command_http("Pragma: no-cache");
          write_command_http("Content-type: application/binary");
          write_command_http("");
          int chunkCount = 0;
          int chunkCountTotal = 0;
          long chunkTimer = System.currentTimeMillis();
          DVector queue = null;
          if (t != null)
            queue = t.getLocal(); 
          long lastSend = System.currentTimeMillis();
          int delay = 1;
          while (SharedSession.find("crushftp.usernames").containsKey(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user")) {
            Chunk c = null;
            if (t != null) {
              if (System.currentTimeMillis() - lastSend > 10000L) {
                String command = "PINGSEND:" + System.currentTimeMillis();
                c = new Chunk(0, command.getBytes(), command.length(), -1);
                lastSend = System.currentTimeMillis();
              } 
              synchronized (queue) {
                if (c == null && queue.size() > 0)
                  c = queue.remove(0); 
              } 
            } else {
              t = Tunnel2.getTunnel(this.thisSession.getId());
              if (t != null)
                queue = t.getLocal(); 
            } 
            if (c != null) {
              delay = 1;
              if (c.isCommand() && !c.getCommand().startsWith("A:"))
                this.thisSession.add_log_formatted("Chunk Command:" + c.getCommand(), "POST", req_id); 
              byte[] b = c.toBytes();
              if (doChunked)
                this.original_os.write((String.valueOf(Long.toHexString(b.length)) + "\r\n").getBytes()); 
              this.original_os.write(b);
              if (doChunked)
                this.original_os.write("\r\n".getBytes()); 
              chunkCountTotal++;
              chunkCount++;
              this.thisSession.active();
              if (System.currentTimeMillis() - chunkTimer > 10000L) {
                chunkTimer = System.currentTimeMillis();
                this.thisSession.add_log_formatted("wrote " + chunkCount + " chunks", "POST", req_id);
                chunkCount = 0;
                if (Tunnel2.getTunnel(this.thisSession.getId()) == null) {
                  this.done = true;
                  break;
                } 
              } 
            } else {
              Thread.sleep(delay);
              delay *= 2;
              if (delay > 500)
                delay = 500; 
            } 
            if (t != null && t.getWantClose()) {
              this.done = true;
              break;
            } 
          } 
          if (doChunked)
            this.original_os.write("0\r\n\r\n".getBytes()); 
          this.thisSession.add_log_formatted("wrote total chunks:" + chunkCountTotal, "POST", req_id);
          return;
        } 
      } 
      if (header0.toUpperCase().startsWith("POST ") && header0.toUpperCase().indexOf("/CRUSH_STREAMING_HTTP_PROXY3/") >= 0)
        if (ServerSessionTunnel3.process(urlRequestItems, this.thisSession, this.sock, this))
          return;  
      String initial_current_dir = pwd();
      String error_message = "";
      boolean webDavOK = (Common.check_protocol("WEBDAV", SG("allowed_protocols")) >= 0);
      if (request.getProperty("processFileUpload", "false").equals("true")) {
        if (this.thisSession.getProperty("blockUploads", "false").equals("true")) {
          this.thisSession.add_log_formatted("Blocking file upload by user request.", "STOR", req_id);
          this.ssa.writeResponse("", true, 500, true, false, true);
          return;
        } 
        parsePostArguments(http_boundary, this.http_len_max, true, req_id);
      } 
      if (!this.ssa.processItems(request, byteRanges, req_id))
        if (action.equals("propfind") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV(this.sock, this.thisSession, this); 
          this.ssd.propfind(this.http_len_max, initial_current_dir, depth);
        } else if (action.equals("proppatch") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV(this.sock, this.thisSession, this); 
          this.ssd.proppatch(this.http_len_max, initial_current_dir, depth);
        } else if (action.equals("delete") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV(this.sock, this.thisSession, this); 
          error_message = this.ssd.delete(initial_current_dir, error_message);
        } else if (action.equals("acl") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV(this.sock, this.thisSession, this); 
          this.ssd.acl(this.http_len_max, initial_current_dir);
        } else if (action.equals("copy") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV(this.sock, this.thisSession, this); 
          this.ssd.copy(initial_current_dir, move_destination, overwrite);
        } else if (action.equals("mkcol") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV(this.sock, this.thisSession, this); 
          error_message = this.ssd.mkcol(this.http_len_max, initial_current_dir, error_message);
        } else if (action.equals("lock") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV(this.sock, this.thisSession, this); 
          this.ssd.lock(this.http_len_max, initial_current_dir, depth);
        } else if (action.equals("unlock") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV(this.sock, this.thisSession, this); 
          this.ssd.unlock(initial_current_dir);
        } else if (action.equals("move") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV(this.sock, this.thisSession, this); 
          error_message = this.ssd.move(move_destination, error_message, overwrite);
        } else if (action.equals("put")) {
          Properties cookies = getCookies();
          String md5_str = doPutFile(this.http_len_max, this.done, this.headers, null, user_dir, cookies.getProperty("RandomAccess", "false").equals("true"), start_resume_loc, (Properties)this.thisSession.get("last_metaInfo"));
          if (!md5_str.equals("")) {
            write_command_http("HTTP/1.1 201  Created");
            write_command_http("Last-Modified: " + this.sdf_rfc1123.format(new Date()));
            if (this.headerLookup.containsKey("LAST-MODIFIED")) {
              String modified = this.headerLookup.getProperty("LAST-MODIFIED").trim();
              this.thisSession.uiPUT("the_command", "MDTM");
              SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
              this.thisSession.uiPUT("the_command_data", String.valueOf(pwd()) + " " + sdf.format(this.sdf_rfc1123.parse(modified)));
              error_message = String.valueOf(error_message) + this.thisSession.do_MDTM();
            } 
            this.thisSession.uVFS.reset();
          } else {
            write_command_http("HTTP/1.1 403  Access Denied.");
          } 
          write_standard_headers();
          write_command_http("Content-Length: " + (md5_str.trim().length() + 2));
          write_command_http("");
          write_command_http(md5_str.trim());
        } else if (!action.equals("process post")) {
          if (action.equals("serve dir")) {
            if (this.thisSession.BG("WebServerMode") && (pwd().equals("/") || pwd().equals(this.thisSession.SG("root_dir")))) {
              Vector v = new Vector();
              try {
                this.thisSession.uVFS.getListing(v, pwd());
              } catch (Exception e) {
                Log.log("HTTP_SERVER", 2, e);
              } 
              for (int x = 0; x < v.size(); x++) {
                Properties pp = v.elementAt(x);
                if (pp.getProperty("name").toUpperCase().equals("INDEX.HTML") || pp.getProperty("name").toUpperCase().equals("INDEX.HTM")) {
                  sendRedirect("/" + pp.getProperty("name"));
                  write_command_http("Content-Length: 0");
                  write_command_http("");
                  return;
                } 
              } 
            } 
            String basePath = pwd().substring(this.thisSession.SG("root_dir").length() - 1);
            if (!basePath.equals("/")) {
              if (webDavAgents.indexOf(this.thisSession.uiSG("header_user-agent")) >= 0) {
                write_command_http("HTTP/1.1 200 OK");
                write_command_http("Pragma: no-cache");
                write_standard_headers();
                write_command_http("Content-Length: 0");
                write_command_http("Content-Type: text/html;charset=utf-8");
                write_command_http("");
                return;
              } 
              if (this.thisSession.BG("DisallowListingDirectories")) {
                if (this.thisSession.BG("WebServerMode"))
                  basePath = String.valueOf(basePath) + "index.html"; 
                sendRedirect(basePath);
              } else {
                sendRedirect("/#" + basePath);
              } 
              write_command_http("Content-Length: 0");
              write_command_http("");
              return;
            } 
            RandomAccessFile web = new RandomAccessFile(new File_S(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/jQuery/index.html"), "r");
            byte[] b = new byte[(int)web.length()];
            web.readFully(b);
            web.close();
            String web_dot_html = new String(b, "UTF8");
            web_dot_html = Common.replace_str(web_dot_html, "/WebInterface/", String.valueOf(this.proxy) + "WebInterface/");
            web_dot_html = ServerStatus.thisObj.change_vars_to_values(web_dot_html, this.thisSession);
            write_command_http("HTTP/1.1 200 OK");
            write_command_http("Pragma: no-cache");
            write_standard_headers();
            write_command_http("Content-Length: " + (web_dot_html.getBytes("UTF8")).length);
            write_command_http("Content-Type: text/html;charset=utf-8");
            write_command_http("");
            write_command_raw(web_dot_html);
            return;
          } 
          if (action.equals("serve file"))
            doServeFile(otherFile, this.headers, ifnonematch, headersOnly, request, byteRanges); 
        }  
    } 
  }
  
  public void doServeFile(VRL otherFile, Vector headers, String ifnonematch, boolean headersOnly, Properties request, Vector byteRanges) throws Exception {
    if (otherFile == null) {
      if (pwd().indexOf("/:filetree") >= 0 && ServerStatus.BG("allow_filetree")) {
        this.retr.data_os = this.original_os;
        this.retr.httpDownload = true;
        String the_dir = Common.all_but_last(pwd());
        Properties item = null;
        try {
          item = this.thisSession.uVFS.get_item(the_dir);
        } catch (Exception exception) {}
        if (item != null) {
          otherFile = new VRL(item.getProperty("url"));
          this.retr.init_vars(pwd(), 0L, 0L, this.thisSession, item, false, "", otherFile, null);
          this.retr.runOnce = true;
          this.done = true;
          write_command_http("HTTP/1.1 200 OK");
          write_command_http("Pragma: no-cache");
          write_command_http("Content-Type: text/plain");
          write_standard_headers();
          write_command_http("");
          this.retr.run();
        } else {
          write_command_http("HTTP/1.1 200 OK");
          write_command_http("Pragma: no-cache");
          write_command_http("Content-Type: text/plain");
          write_command_http("Content-Length: 0");
          write_standard_headers();
          write_command_http("");
        } 
      } else {
        consumeBadData();
        boolean ok1 = (this.thisSession.check_access_privs(Common.all_but_last(pwd()), "RETR") && Common.filter_check("D", Common.last(pwd()), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")));
        boolean ok2 = (this.thisSession.check_access_privs(pwd(), "RETR") && Common.filter_check("D", Common.last(pwd()), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")));
        if (webDavAgents.indexOf(this.thisSession.uiSG("header_user-agent")) >= 0)
          ok1 = true; 
        if (ok1 || ok2 || headersOnly) {
          write_command_http("HTTP/1.1 404 Not Found");
        } else {
          write_command_http("HTTP/1.1 403 Access Denied.");
        } 
        String html404 = ServerStatus.SG("web404Text");
        html404 = ServerStatus.thisObj.change_vars_to_values(html404, this.thisSession);
        write_command_http("Content-Length: " + ((html404.getBytes("UTF8")).length + 2));
        write_standard_headers();
        write_command_http("");
        write_command_http(html404);
      } 
    } else {
      this.thisSession.uiPUT("the_command", "RETR");
      if (ServerStatus.SG("default_logo").equals("logo.gif"))
        ServerStatus.server_settings.put("default_logo", "logo.png"); 
      Common.updateMimes();
      String ext = "";
      if (otherFile.toString().lastIndexOf(".") >= 0)
        ext = otherFile.toString().substring(otherFile.toString().lastIndexOf(".")).toUpperCase(); 
      if (Common.mimes.getProperty(ext, "").equals(""))
        ext = "*"; 
      Properties item = this.thisSession.uVFS.get_item(pwd());
      String htmlData = "";
      if ((Common.mimes.getProperty(ext, "").toUpperCase().endsWith("/HTML") || Common.mimes.getProperty(ext, "").toUpperCase().endsWith("/X-JAVA-JNLP-FILE") || Common.mimes.getProperty(ext, "").toUpperCase().endsWith("/JAVASCRIPT") || Common.mimes.getProperty(ext, "").toUpperCase().endsWith("/CSS")) && (otherFile.toString().indexOf("/WebInterface/") >= 0 || pwd().toUpperCase().startsWith("/WEBINTERFACE/") || this.thisSession.BG("WebServerSSI"))) {
        String current_dir = pwd();
        if (current_dir.indexOf("?") >= 0)
          current_dir = current_dir.substring(0, current_dir.indexOf("?")); 
        if (current_dir.startsWith(this.thisSession.SG("root_dir")))
          current_dir = current_dir.substring(this.thisSession.SG("root_dir").length()); 
        if (!current_dir.startsWith("/"))
          current_dir = "/" + current_dir; 
        if (item == null && current_dir.startsWith("/WebInterface/"))
          if ((new File_S(String.valueOf(System.getProperty("crushftp.web")) + current_dir)).exists()) {
            RandomAccessFile in = new RandomAccessFile(new File_S(String.valueOf(System.getProperty("crushftp.web")) + current_dir), "r");
            byte[] b = new byte[this.bufferSize];
            htmlData = "";
            int bytesRead = 0;
            while (bytesRead >= 0) {
              bytesRead = in.read(b);
              if (bytesRead > 0)
                htmlData = String.valueOf(htmlData) + new String(b, 0, bytesRead, "UTF8"); 
            } 
            in.close();
          }  
        if (item != null) {
          GenericClient c = this.thisSession.uVFS.getClient(item);
          try {
            if (c.stat(otherFile.getPath()) != null) {
              InputStream in = c.download(otherFile.getPath(), 0L, -1L, true);
              byte[] b = new byte[this.bufferSize];
              htmlData = "";
              int bytesRead = 0;
              while (bytesRead >= 0) {
                bytesRead = in.read(b);
                if (bytesRead > 0)
                  htmlData = String.valueOf(htmlData) + new String(b, 0, bytesRead, "UTF8"); 
              } 
              in.close();
            } 
          } finally {
            c = this.thisSession.uVFS.releaseClient(c);
          } 
        } 
        if (otherFile.getName().equalsIgnoreCase("CRUSHTUNNEL.JNLP")) {
          htmlData = Common.replace_str(htmlData, "%base_url%", getBaseUrl(this.hostString));
          htmlData = Common.replace_str(htmlData, "%CrushAuth%", this.thisSession.getId());
          htmlData = ServerStatus.thisObj.change_vars_to_values(htmlData, this.thisSession);
        } else if (otherFile.getName().equalsIgnoreCase("CUSTOM.JS")) {
          String cjs = this.thisSession.SG("javascript");
          if (!cjs.equals("javascript")) {
            htmlData = "$(document).ready(function () {\r\n\r\n" + htmlData + cjs + "\r\n\r\n});";
            if (htmlData.indexOf("//replace_variables") >= 0)
              htmlData = ServerStatus.thisObj.change_vars_to_values(htmlData, this.thisSession); 
          } 
        } else if (otherFile.getName().equalsIgnoreCase("CUSTOM.CSS")) {
          htmlData = String.valueOf(htmlData) + this.thisSession.SG("css");
        } 
        if (SG("site").indexOf("(SITE_WEBFTPPROXY)") >= 0) {
          String proxyInfo = "<script defer=\"true\">\r\nvar whitelist = new Array();\r\n";
          Vector whitelist = new Vector();
          Vector whitelistval = new Vector();
          Vector proxyRules = ServerStatus.VG("proxyRules");
          int x;
          for (x = 0; x < proxyRules.size(); x++) {
            Properties pp = proxyRules.elementAt(x);
            String part1 = ServerStatus.thisObj.change_vars_to_values(pp.getProperty("criteria1"), this.thisSession);
            String part2 = ServerStatus.thisObj.change_vars_to_values(pp.getProperty("criteria2"), this.thisSession);
            if ((Common.do_search(part1, part2, false, 0) || Common.do_search(part2, part1, false, 0)) && pp.getProperty("condition").equals("=")) {
              String val = String.valueOf(pp.getProperty("protocol")) + "://" + pp.getProperty("host") + ":" + pp.getProperty("port");
              if (pp.getProperty("host").indexOf("*") < 0) {
                whitelistval.addElement(val);
                whitelist.addElement(pp);
              } 
            } else if (!Common.do_search(part1, part2, false, 0) && !Common.do_search(part2, part1, false, 0) && pp.getProperty("condition").equals("!=")) {
              String val = String.valueOf(pp.getProperty("protocol")) + "://" + pp.getProperty("host") + ":" + pp.getProperty("port");
              if (pp.getProperty("host").indexOf("*") < 0) {
                whitelistval.addElement(val);
                whitelist.addElement(pp);
              } 
            } else if (part1.equals("1") && part2.equals("2")) {
              pp.getProperty("condition").equals("=");
            } 
          } 
          for (x = 0; x < whitelist.size(); x++) {
            Properties pp = whitelist.elementAt(x);
            proxyInfo = String.valueOf(proxyInfo) + "whitelist[" + x + "] = new Object();\r\n";
            proxyInfo = String.valueOf(proxyInfo) + "whitelist[" + x + "].proxyName = \"" + pp.getProperty("proxyName") + "\";\r\n";
            proxyInfo = String.valueOf(proxyInfo) + "whitelist[" + x + "].protocol = \"" + pp.getProperty("protocol") + "\";\r\n";
            proxyInfo = String.valueOf(proxyInfo) + "whitelist[" + x + "].host = \"" + pp.getProperty("host") + "\";\r\n";
            proxyInfo = String.valueOf(proxyInfo) + "whitelist[" + x + "].port = \"" + pp.getProperty("port") + "\";\r\n";
          } 
          proxyInfo = String.valueOf(proxyInfo) + "var siteprivs = \"" + SG("site") + "\";\r\n";
          proxyInfo = String.valueOf(proxyInfo) + "</script>\r\n";
          htmlData = Common.replace_str(htmlData, "<!-- PROXY_INFO --!>", proxyInfo);
        } 
        if (this.thisSession.BG("WebServerSSI") && Common.mimes.getProperty(ext, "").toUpperCase().endsWith("/HTML")) {
          int depth = 0;
          while (htmlData.toUpperCase().indexOf("<!--#INCLUDE") >= 0) {
            int loc = htmlData.toUpperCase().indexOf("<!--#INCLUDE");
            int loc2 = htmlData.indexOf("\"", loc) + 1;
            String importFilename = htmlData.substring(loc2, htmlData.indexOf("\"", loc2 + 2));
            Properties importItem = null;
            if (importFilename.startsWith("/")) {
              if (!importFilename.startsWith(this.thisSession.SG("root_dir")))
                importFilename = String.valueOf(this.thisSession.SG("root_dir")) + importFilename.substring(1); 
              importItem = this.thisSession.uVFS.get_item(importFilename);
            } else {
              String the_dir = current_dir;
              if (!the_dir.endsWith("/"))
                the_dir = Common.all_but_last(the_dir); 
              importFilename = String.valueOf(the_dir) + importFilename;
              if (!importFilename.startsWith(this.thisSession.SG("root_dir")))
                importFilename = String.valueOf(this.thisSession.SG("root_dir")) + importFilename.substring(1); 
              importItem = this.thisSession.uVFS.get_item(importFilename);
            } 
            String importHtml = "";
            if (importItem != null) {
              GenericClient c = this.thisSession.uVFS.getClient(importItem);
              try {
                InputStream in = c.download((new VRL(importItem.getProperty("url"))).getPath(), 0L, -1L, true);
                int bytesRead = 0;
                byte[] b = new byte[this.bufferSize];
                while (bytesRead >= 0) {
                  bytesRead = in.read(b);
                  if (bytesRead > 0)
                    importHtml = String.valueOf(importHtml) + new String(b, 0, bytesRead, "UTF8"); 
                } 
                in.close();
              } finally {
                c = this.thisSession.uVFS.releaseClient(c);
              } 
            } 
            String replacer = htmlData.substring(loc, htmlData.indexOf("-->", loc) + 3);
            htmlData = Common.replace_str(htmlData, replacer, importHtml);
            if (depth++ > 100)
              break; 
          } 
        } 
      } 
      long checkDate = (new Date()).getTime();
      try {
        checkDate = Long.parseLong(ifnonematch);
      } catch (Exception exception) {}
      if (item == null && htmlData.length() == 0) {
        write_command_http("HTTP/1.1 404 Not Found");
        String html404 = ServerStatus.SG("web404Text");
        html404 = ServerStatus.thisObj.change_vars_to_values(html404, this.thisSession);
        write_command_http("Content-Length: " + ((html404.getBytes("UTF8")).length + 2));
        write_standard_headers();
        write_command_http("");
        write_command_http(html404);
        return;
      } 
      boolean checkOK = false;
      boolean zipDownload = false;
      Properties stat = new Properties();
      if (item != null) {
        GenericClient c = null;
        try {
          c = this.thisSession.uVFS.getClient(item);
          stat = c.stat(otherFile.getPath());
          if (stat == null && otherFile.getPath().toUpperCase().endsWith(".ZIP")) {
            zipDownload = true;
            stat = c.stat(otherFile.getPath().substring(0, otherFile.getPath().length() - 4));
          } 
        } catch (Exception e) {
          if (c != null)
            throw e; 
          Log.log("SERVER", 1, e);
        } finally {
          c = this.thisSession.uVFS.releaseClient(c);
        } 
      } 
      if (stat != null && checkDate > 0L && checkDate >= Long.parseLong(stat.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())) && !headersOnly) {
        checkOK = true;
      } else if (checkDate > 0L && !headersOnly) {
        checkDate -= 86400000L;
        for (int x = 0; x <= 48; x++) {
          if (stat != null && checkDate == Long.parseLong(stat.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()))) {
            checkOK = true;
            break;
          } 
          checkDate += 3600000L;
        } 
      } 
      if (otherFile.getName().equalsIgnoreCase("crushftp.jnlp") || htmlData.length() > 0)
        checkOK = false; 
      if (checkOK) {
        int validSecs = 30;
        if (otherFile.getPath().toUpperCase().indexOf("/WEBINTERFACE/") >= 0 || this.thisSession.BG("WebServerMode"))
          if (otherFile.getName().toUpperCase().endsWith(".GIF") || otherFile.getName().toUpperCase().endsWith(".PNG") || otherFile.getName().toUpperCase().endsWith(".JPG") || otherFile.getName().toUpperCase().endsWith(".CSS") || otherFile.getName().toUpperCase().endsWith(".XSL") || otherFile.getName().toUpperCase().endsWith(".JS") || otherFile.getName().toUpperCase().endsWith(".ICO") || otherFile.getName().toUpperCase().endsWith(".HTML"))
            validSecs = 3000;  
        write_command_http("HTTP/1.1 304 Not Modified");
        write_standard_headers();
        write_command_http("Cache-Control: post-check=" + validSecs + ",pre-check=" + (validSecs * 10));
        if (this.cacheHeader.length() > 0) {
          write_command_http(this.cacheHeader);
          this.cacheHeader = "";
        } 
        write_command_http("Last-Modified: " + this.sdf_rfc1123.format(new Date(Long.parseLong(stat.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())))));
        write_command_http("ETag: " + Long.parseLong(stat.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())));
        write_command_http("Content-Length: 0");
        write_command_http("");
      } else {
        if (byteRanges.size() == 1 && htmlData.length() == 0 && Long.parseLong(((Properties)byteRanges.elementAt(0)).getProperty("start", "0")) > Long.parseLong(stat.getProperty("size"))) {
          write_command_http("HTTP/1.1 416 Invalid start location");
          write_standard_headers();
          write_command_http("Content-Length: 0");
          write_command_http("");
          return;
        } 
        if (byteRanges.size() > 0 && htmlData.length() == 0) {
          write_command_http("HTTP/1.1 206 Partial Content");
        } else {
          write_command_http("HTTP/1.1 200 OK");
        } 
        write_standard_headers();
        String byteRangeBoundary = Common.makeBoundary();
        String contentType = Common.mimes.getProperty(ext, "");
        if (byteRanges.size() <= 1) {
          write_command_http("Content-Type: " + contentType);
          if (contentType.toLowerCase().indexOf("/binary") >= 0)
            write_command_http("Content-Disposition: attachment; filename=\"" + ((this.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("MSIE") >= 0 || this.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("TRIDENT") >= 0) ? Common.url_encode(otherFile.getName()) : otherFile.getName()) + "\""); 
        } else if (byteRanges.size() > 1) {
          write_command_http("Content-Type: multipart/byteranges; boundary=" + byteRangeBoundary);
        } 
        write_command_http("X-UA-Compatible: chrome=1; IE=Edge");
        if (this.cacheHeader.length() > 0) {
          write_command_http(this.cacheHeader);
          this.cacheHeader = "";
        } 
        if (htmlData.length() == 0 && stat != null) {
          write_command_http("Last-Modified: " + this.sdf_rfc1123.format(new Date(Long.parseLong(stat.getProperty("modified")))));
          write_command_http("ETag: " + Long.parseLong(stat.getProperty("modified")));
        } else {
          write_command_http("Last-Modified: " + this.sdf_rfc1123.format(new Date()));
          write_command_http("ETag: " + (new Date()).getTime());
        } 
        if (headersOnly)
          write_command_http("Pragma: no-cache"); 
        if (otherFile.getName().equalsIgnoreCase("crushftp.jnlp") || htmlData.length() > 0)
          write_command_http("Cache-Control: post-check=1,pre-check=1"); 
        boolean quickWrite = false;
        if (htmlData.length() > 0)
          quickWrite = true; 
        String amountEnd = "0";
        if (stat != null) {
          amountEnd = (new StringBuffer(String.valueOf(Long.parseLong(stat.getProperty("size", "0")) - 1L))).toString();
        } else {
          amountEnd = (new StringBuffer(String.valueOf((htmlData.getBytes("UTF8")).length))).toString();
        } 
        for (int x = 0; x < byteRanges.size(); x++) {
          Properties p = byteRanges.elementAt(x);
          if (p.getProperty("end", "").equals(""))
            p.put("end", amountEnd); 
        } 
        if (zipDownload && otherFile.getName().toUpperCase().endsWith(".ZIP")) {
          Common.startMultiThreadZipper(this.thisSession.uVFS, this.retr, pwd(), 5000, false, new Vector());
          this.done = true;
        } else {
          long l = 0L;
          try {
            l = Long.parseLong(stat.getProperty("size"));
          } catch (Exception exception) {}
          if (ServerStatus.BG("fileEncryption")) {
            write_command_http("Connection: close");
            this.done = true;
          } else if (byteRanges.size() == 1) {
            Properties p = byteRanges.elementAt(0);
            if (Long.parseLong(p.getProperty("start")) > Long.parseLong(p.getProperty("end")))
              p.put("start", p.getProperty("end")); 
            write_command_http("Content-Range: bytes " + p.getProperty("start") + "-" + p.getProperty("end") + "/" + l);
            int adobeReaderOffsetFix = 1;
            long calculatedContentLength = (htmlData.length() > 0) ? ((htmlData.getBytes("UTF8")).length + 2) : (adobeReaderOffsetFix + Long.parseLong(p.getProperty("end")) - Long.parseLong(p.getProperty("start")));
            if (calculatedContentLength == 0L)
              calculatedContentLength = 1L; 
            write_command_http("Content-Length: " + calculatedContentLength);
          } else if (byteRanges.size() == 0) {
            if (htmlData.length() > 0)
              l = ((htmlData.getBytes("UTF8")).length + 2); 
            if (l >= 0L) {
              write_command_http("Content-Length: " + l);
            } else {
              this.done = true;
            } 
          } else if (byteRanges.size() > 1) {
            long calculatedContentLength = 2L;
            for (int j = 0; j < byteRanges.size(); j++) {
              Properties p = byteRanges.elementAt(j);
              if (Long.parseLong(p.getProperty("start")) > Long.parseLong(p.getProperty("end")))
                p.put("start", p.getProperty("end")); 
              calculatedContentLength += (("--" + byteRangeBoundary).length() + 2);
              calculatedContentLength += (("Content-Type: " + contentType).length() + 2);
              calculatedContentLength += (("Content-range: bytes " + p.getProperty("start") + "-" + p.getProperty("end") + "/" + l).length() + 2);
              calculatedContentLength += 2L;
              calculatedContentLength += Long.parseLong(p.getProperty("end")) - Long.parseLong(p.getProperty("start"));
              calculatedContentLength += 2L;
              calculatedContentLength++;
            } 
            calculatedContentLength += (("--" + byteRangeBoundary + "--").length() + 2);
            if (calculatedContentLength == 0L)
              calculatedContentLength = 1L; 
            write_command_http("Content-Length: " + calculatedContentLength);
          } 
          write_command_http("Accept-Ranges: bytes");
        } 
        write_command_http("");
        if (byteRanges.size() == 0) {
          Properties p = new Properties();
          p.put("start", "0");
          p.put("end", "-1");
          byteRanges.addElement(p);
        } 
        long content_length = 0L;
        try {
          content_length = Long.parseLong(stat.getProperty("size"));
        } catch (Exception exception) {}
        for (int i = 0; i < byteRanges.size(); i++) {
          Properties p = byteRanges.elementAt(i);
          if (!headersOnly)
            if (quickWrite) {
              write_command_http(htmlData);
            } else {
              if (byteRanges.size() > 1) {
                if (i == 0)
                  write_command_http(""); 
                write_command_http("--" + byteRangeBoundary);
                write_command_http("Content-Type: " + contentType);
                write_command_http("Content-range: bytes " + p.getProperty("start") + "-" + p.getProperty("end") + "/" + content_length);
                write_command_http("");
              } 
              this.thisSession.uiPUT("file_transfer_mode", "BINARY");
              this.retr.data_os = this.original_os;
              this.retr.httpDownload = true;
              String the_dir = pwd();
              Properties pp = new Properties();
              pp.put("the_dir", the_dir);
              this.thisSession.runPlugin("transfer_path", pp);
              the_dir = pp.getProperty("the_dir", the_dir);
              this.retr.init_vars(the_dir, Long.parseLong(p.getProperty("start")), Long.parseLong(p.getProperty("end")), this.thisSession, item, false, "", otherFile, null);
              this.retr.runOnce = true;
              this.retr.run();
              if (byteRanges.size() > 1)
                write_command_http(""); 
            }  
        } 
        if (byteRanges.size() > 1)
          write_command_http("--" + byteRangeBoundary + "--"); 
      } 
    } 
  }
  
  public boolean isAS2() {
    for (int x = 0; x < this.headers.size(); x++) {
      String s = this.headers.elementAt(x).toString();
      if (s.toLowerCase().trim().startsWith("as2-to"))
        return true; 
    } 
    return false;
  }
  
  public boolean loginCheckAnonymousAuth(String header0) throws Exception {
    if (!this.thisSession.uiBG("user_logged_in") && !ServerStatus.BG("ignore_web_anonymous")) {
      this.thisSession.uiPUT("user_name", "anonymous");
      this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
      createCookieSession(false);
      this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
      if (this.thisSession.uVFS != null)
        this.thisSession.uVFS.free(); 
      this.thisSession.uiPUT("dont_log", "true");
      if (!ServerStatus.BG("ignore_web_anonymous_proxy")) {
        this.thisSession.login_user_pass();
        setupSession();
      } 
      this.thisSession.uiPUT("dont_log", "false");
      String attemptedPath = this.headers.elementAt(0).toString();
      attemptedPath = attemptedPath.substring(attemptedPath.indexOf(" ") + 1, attemptedPath.lastIndexOf(" "));
      if (attemptedPath.toUpperCase().startsWith("/") && !attemptedPath.toUpperCase().startsWith(SG("root_dir").toUpperCase()) && this.thisSession.IG("max_logins") >= 0)
        attemptedPath = String.valueOf(SG("root_dir")) + attemptedPath.substring(1); 
      try {
        if ((this.thisSession.uVFS == null || this.thisSession.IG("max_logins") < 0) && !header0.startsWith("PROPFIND")) {
          if (this.thisSession.uVFS != null)
            this.thisSession.uVFS.free(); 
          this.thisSession.uVFS = VFS.getVFS(UserTools.generateEmptyVirtual());
          if (this.thisSession.user != null)
            this.thisSession.user.put("root_dir", "/"); 
          this.thisSession.uiPUT("user_logged_in", "true");
          this.thisSession.uiPUT("user_name", "");
          this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
          if (this.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("IPHOTO") >= 0 || this.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("ISNETSERVICES") >= 0 || this.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("ICAL") >= 0 || this.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("CALENDAR") >= 0 || this.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("CFNETWORK") >= 0)
            this.thisSession.uiPUT("user_logged_in", "false"); 
          if (!header0.toUpperCase().startsWith("GET /WEBINTERFACE/") && header0.toUpperCase().startsWith("GET / ")) {
            sendRedirect("/WebInterface/login.html");
            write_command_http("Content-Length: 0");
            write_command_http("");
            return false;
          } 
        } 
        if (!this.thisSession.uiBG("user_logged_in") && (header0.startsWith("PROPFIND") || header0.startsWith("DELETE") || header0.startsWith("PUT") || header0.startsWith("MKCOL") || this.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("ICAL") >= 0 || this.thisSession.uiSG("header_user-agent").toUpperCase().indexOf("CALENDAR") >= 0)) {
          DEAUTH();
          return false;
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 2, e);
      } 
      if (this.thisSession.uVFS != null)
        this.thisSession.uVFS.reset(); 
    } 
    return true;
  }
  
  public void loginCheckHeaderAuth() throws Exception {
    if ((!this.thisSession.uiBG("user_logged_in") || this.thisSession.uiSG("user_name").equalsIgnoreCase("anonymous") || this.thisSession.uiSG("user_name").equalsIgnoreCase("") || this.thisSession.user == null) && (this.headerLookup.containsKey("Authorization".toUpperCase()) || this.headerLookup.containsKey("Proxy-Authorization".toUpperCase()) || this.headerLookup.containsKey("as2-to".toUpperCase()))) {
      String authorization = "";
      if (this.headerLookup.containsKey("Authorization".toUpperCase())) {
        authorization = this.headerLookup.getProperty("Authorization".toUpperCase()).trim();
        authorization = Common.decode64(authorization.substring("Basic".length()).trim());
        if (webDavAgents.indexOf(this.thisSession.uiSG("header_user-agent")) < 0)
          if (ServerStatus.BG("webdav_agent_learning"))
            webDavAgents.addElement(this.thisSession.uiSG("header_user-agent"));  
      } else if (this.headerLookup.containsKey("Proxy-Authorization".toUpperCase())) {
        authorization = this.headerLookup.getProperty("Proxy-Authorization".toUpperCase()).trim();
        if (authorization.toUpperCase().startsWith("BASIC")) {
          authorization = Common.decode64(authorization.substring("Basic".length()).trim());
          if (webDavAgents.indexOf(this.thisSession.uiSG("header_user-agent")) < 0)
            if (ServerStatus.BG("webdav_agent_learning"))
              webDavAgents.addElement(this.thisSession.uiSG("header_user-agent"));  
        } else {
          return;
        } 
      } else if (this.headerLookup.containsKey("as2-to".toUpperCase())) {
        authorization = this.headerLookup.getProperty("as2-to".toUpperCase()).trim();
        authorization = Common.replace_str(authorization, "-_-", ":");
        if (authorization.indexOf(":") < 0)
          authorization = String.valueOf(authorization) + ":"; 
        this.headerLookup.put("as2-to".toUpperCase(), authorization);
        this.thisSession.uiPUT("current_password", authorization);
        Log.log("HTTP_SERVER", 0, "Authentication as AS2 user:" + authorization.substring(0, authorization.indexOf(":")));
      } 
      String user_name = authorization.substring(0, authorization.indexOf(":"));
      String user_pass = authorization.substring(authorization.indexOf(":") + 1);
      this.thisSession.uiPUT("current_password", user_pass);
      this.thisSession.uiPUT("user_name", user_name);
      if (ServerStatus.BG("username_uppercase"))
        user_name = user_name.toUpperCase(); 
      if (ServerStatus.BG("lowercase_usernames"))
        user_name = user_name.toLowerCase(); 
      if (this.thisSession.uiSG("user_name").indexOf("\\") >= 0)
        if (ServerStatus.BG("strip_windows_domain_webdav"))
          user_name = this.thisSession.uiSG("user_name").substring(this.thisSession.uiSG("user_name").indexOf("\\") + 1);  
      this.thisSession.uiPUT("user_name_original", user_name);
      this.thisSession.uiPUT("user_name", user_name);
      this.thisSession.runPlugin("beforeLogin", null);
      this.this_thread.setName(String.valueOf(user_name) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
      if (this.thisSession.uVFS != null)
        this.thisSession.uVFS.free(); 
      boolean good = this.thisSession.login_user_pass(false, true, user_name, user_pass);
      setupSession();
      if (good) {
        this.thisSession.uiPUT("user_name", user_name);
        this.thisSession.uiPUT("current_password", user_pass);
        this.thisSession.uiPUT("authorization_header", "true");
        createCookieSession(false);
        this.thisSession.do_event5("LOGIN", null);
      } 
    } 
  }
  
  public void loginDNSAuth(String domain) throws Exception {
    if ((!this.thisSession.uiBG("user_logged_in") || this.thisSession.uiSG("user_name").equalsIgnoreCase("anonymous") || this.thisSession.uiSG("user_name").equalsIgnoreCase("") || this.thisSession.user == null) && domain.toUpperCase().trim().endsWith(this.server_item.getProperty("dns_login_domain", "").toUpperCase().trim()) && !this.server_item.getProperty("dns_login_domain", "").equals("")) {
      domain = domain.substring(0, domain.length() - this.server_item.getProperty("dns_login_domain", "").length());
      if ((domain.split("-")).length < 2)
        return; 
      String user_name = domain.split("-")[0];
      String user_pass = domain.split("-")[1];
      this.thisSession.uiPUT("current_password", user_pass);
      this.thisSession.uiPUT("user_name", user_name);
      if (ServerStatus.BG("username_uppercase"))
        user_name = user_name.toUpperCase(); 
      if (ServerStatus.BG("lowercase_usernames"))
        user_name = user_name.toLowerCase(); 
      this.thisSession.uiPUT("user_name_original", user_name);
      this.thisSession.uiPUT("user_name", user_name);
      this.thisSession.runPlugin("beforeLogin", null);
      this.this_thread.setName(String.valueOf(user_name) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
      if (this.thisSession.uVFS != null)
        this.thisSession.uVFS.free(); 
      boolean good = this.thisSession.login_user_pass(false, true, user_name, user_pass);
      setupSession();
      if (good) {
        this.thisSession.uiPUT("user_name", user_name);
        this.thisSession.uiPUT("current_password", user_pass);
        this.thisSession.uiPUT("authorization_header", "true");
        createCookieSession(false);
        this.thisSession.do_event5("LOGIN", null);
      } 
    } 
  }
  
  public boolean loginCheckClientAuth() throws Exception {
    boolean requirePassword = true;
    boolean needClientAuth = ServerStatus.BG("needClientAuth");
    if (!this.server_item.getProperty("needClientAuth", "false").equals("false"))
      needClientAuth = this.server_item.getProperty("needClientAuth", "").equals("true"); 
    if (needClientAuth && this.thisSession.uiBG("secure")) {
      String subject = "";
      try {
        subject = ((SSLSocket)this.sock).getSession().getPeerCertificateChain()[0].getSubjectDN().toString();
        Log.log("SERVER", 0, "ClientAuth:Subject=" + subject);
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
        Log.log("SERVER", 1, (String)((SSLSocket)this.sock).getSession().getPeerCertificateChain()[0]);
      } 
      String certUsername = subject.substring(subject.indexOf("CN=") + 3).trim();
      if (certUsername.indexOf(",") >= 0)
        certUsername = certUsername.substring(0, certUsername.indexOf(",")).trim(); 
      if (!certUsername.startsWith("NOLOGIN_"))
        if (ServerStatus.BG("client_cert_auth")) {
          requirePassword = false;
          if (!this.thisSession.uiBG("user_logged_in")) {
            this.thisSession.uiPUT("user_name", certUsername.trim());
            this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
            this.thisSession.runPlugin("beforeLogin", null);
            if (!Common.dmz_mode) {
              this.thisSession.uiPUT("current_password", "");
            } else {
              this.thisSession.uiPUT("current_password", Common.System2.getProperty("crushftp.proxy.anyPassToken", ""));
            } 
            this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
            if (this.thisSession.uVFS != null)
              this.thisSession.uVFS.free(); 
            boolean good = this.thisSession.login_user_pass(true, true);
            setupSession();
            if (good) {
              createCookieSession(false);
              this.thisSession.do_event5("LOGIN", null);
              this.writeCookieAuth = true;
            } 
          } 
        }  
    } 
    return requirePassword;
  }
  
  public void loginCheckHttpTrustHeaders() {
    if (this.thisSession.uiBG("user_logged_in"))
      return; 
    try {
      Vector httpTrustHeaderVariables = new Vector();
      Vector httpTrustHeaderValues = new Vector();
      if (this.server_item.getProperty("httpTrustHeaderVariables", "").equals("false"))
        this.server_item.put("httpTrustHeaderVariables", ""); 
      String[] trustHeadersStr = this.server_item.getProperty("httpTrustHeaderVariables", "").split(",");
      for (int x = 0; x < trustHeadersStr.length; x++) {
        if (!trustHeadersStr[x].trim().equals("")) {
          httpTrustHeaderVariables.addElement(trustHeadersStr[x].split("=")[0].toUpperCase().trim());
          httpTrustHeaderValues.addElement(trustHeadersStr[x].split("=")[1].trim());
        } 
      } 
      Properties trustHeaderVals = new Properties();
      boolean foundTrustHeader = false;
      for (int i = 1; i < this.headers.size(); i++) {
        String data = this.headers.elementAt(i).toString();
        if (data.indexOf(":") >= 0) {
          int loc = httpTrustHeaderVariables.indexOf(data.substring(0, data.indexOf(":")).toUpperCase().trim());
          if (!this.server_item.getProperty("httpTrustHeaderVariables", "").equals("") && loc >= 0) {
            String data2 = data.substring(data.indexOf(":") + 1).trim();
            trustHeaderVals.put(httpTrustHeaderValues.elementAt(loc), data2);
            foundTrustHeader = true;
          } 
        } 
      } 
      if (foundTrustHeader) {
        Enumeration keys = trustHeaderVals.keys();
        this.thisSession.uiPUT("current_password", "");
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          String val = trustHeaderVals.getProperty(key, "");
          this.thisSession.uiPUT(key, val);
        } 
        this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
        this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
        if (this.thisSession.uVFS != null)
          this.thisSession.uVFS.free(); 
        boolean good = this.thisSession.login_user_pass(true);
        if (good) {
          keys = trustHeaderVals.keys();
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            String val = trustHeaderVals.getProperty(key, "");
            this.thisSession.user.put(key, val);
          } 
          this.writeCookieAuth = true;
          createCookieSession(true);
          setupSession();
          this.thisSession.do_event5("LOGIN", null);
        } 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
  }
  
  public Properties getStorOutputStream(String user_dir, long start_resume_loc, boolean random_access, Properties metaInfo) throws Exception {
    STOR_handler stor = null;
    synchronized (this.thisSession.stor_files_pool_free) {
      if (this.thisSession.stor_files_pool_free.size() > 0) {
        stor = this.thisSession.stor_files_pool_free.remove(0);
      } else {
        stor = new STOR_handler();
      } 
    } 
    stor.wait_for_parent_free = true;
    this.thisSession.stor_files_pool_used.addElement(stor);
    stor.setThreadName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " (stor)");
    if (this.thisSession.uiSG("user_protocol_proxy").equalsIgnoreCase("FTP"))
      stor.block_ftp_fix = true; 
    Properties active = new Properties();
    stor.active2 = active;
    Socket local_s = Common.getSTORSocket(this.thisSession, stor, "", true, user_dir, random_access, start_resume_loc, metaInfo);
    local_s.setSoTimeout(((this.thisSession.IG("max_idle_time") <= 0) ? 60 : this.thisSession.IG("max_idle_time")) * 1000 * 60);
    int loops = 0;
    while (loops++ < 10000 && (active.getProperty("streamOpenStatus", "").equals("STOPPED") || active.getProperty("streamOpenStatus", "").equals("PENDING") || active.getProperty("streamOpenStatus", "").equals("CLOSED")))
      Thread.sleep(((loops < 100) ? loops : 100L)); 
    Properties result = new Properties();
    result.put("stor", stor);
    result.put("active", active);
    result.put("out", local_s.getOutputStream());
    return result;
  }
  
  public Properties getRetrInputStream(String user_dir, long start_resume_loc, Properties metaInfo) throws Exception {
    RETR_handler retr = null;
    synchronized (this.thisSession.retr_files_pool_free) {
      if (this.thisSession.retr_files_pool_free.size() > 0) {
        retr = this.thisSession.retr_files_pool_free.remove(0);
      } else {
        retr = new RETR_handler();
      } 
    } 
    this.thisSession.retr_files_pool_used.addElement(retr);
    retr.setThreadName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ")-" + this.thisSession.uiSG("user_ip") + " (retr)");
    Properties active = new Properties();
    retr.active2 = active;
    this.thisSession.uiPUT("current_dir", user_dir);
    Socket local_s = Common.getRETRSocket(this.thisSession, retr, start_resume_loc, "", true);
    local_s.setSoTimeout(((this.thisSession.IG("max_idle_time") <= 0) ? 60 : this.thisSession.IG("max_idle_time")) * 1000 * 60);
    int loops = 0;
    while (loops++ < 10000 && (active.getProperty("streamOpenStatus", "").equals("STOPPED") || active.getProperty("streamOpenStatus", "").equals("PENDING") || active.getProperty("streamOpenStatus", "").equals("CLOSED")))
      Thread.sleep(((loops < 100) ? loops : 100L)); 
    Properties result = new Properties();
    result.put("retr", retr);
    result.put("active", active);
    result.put("in", local_s.getInputStream());
    return result;
  }
  
  public String doPutFile(long content_length, boolean connectionClose, Vector headers, OutputStream of_stream, String user_dir, boolean random_access, long start_resume_loc, Properties metaInfo) throws Exception {
    boolean ok = false;
    if (this.thisSession.check_access_privs(user_dir, "STOR") && Common.filter_check("U", Common.last(user_dir), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")))
      ok = true; 
    STOR_handler stor = null;
    if (ok) {
      Log.log("HTTP_SERVER", 2, "Access is granted for : " + user_dir);
      user_dir = user_dir.replace(':', '_');
      String the_file = Common.last(user_dir);
      for (int x = 0; x < ServerStatus.SG("unsafe_filename_chars").length(); x++)
        the_file = the_file.replace(ServerStatus.SG("unsafe_filename_chars").charAt(x), '_'); 
      user_dir = String.valueOf(Common.all_but_last(user_dir)) + the_file;
      cd(user_dir);
      Properties item = this.thisSession.uVFS.get_item_parent(Common.all_but_last(user_dir));
      if (item == null)
        this.thisSession.do_MKD(true, user_dir); 
      if (item == null)
        item = this.thisSession.uVFS.get_item_parent(user_dir); 
      if (content_length > 0L)
        this.thisSession.user_info.put("file_length", (new StringBuffer(String.valueOf(content_length))).toString()); 
      Properties active = null;
      if (item != null) {
        Properties upload_item = this.thisSession.uVFS.get_item(user_dir);
        if (upload_item != null && upload_item.getProperty("type", "FILE").equalsIgnoreCase("DIR")) {
          Log.log("STOR", 1, "Attempted to overwrite folder with file content. Denied");
          this.done = true;
          return "";
        } 
      } 
      if (of_stream == null) {
        Properties result = getStorOutputStream(user_dir, start_resume_loc, random_access, metaInfo);
        of_stream = (OutputStream)result.remove("out");
        stor = (STOR_handler)result.remove("stor");
        active = (Properties)result.get("active");
        Log.log("HTTP_SERVER", 2, "Stor handler created: active: " + active.getProperty("active", "") + "stream status: " + active.getProperty("streamOpenStatus", ""));
        Log.log("HTTP_SERVER", 2, "Stor error created: " + stor.inError + " stop message: " + stor.stop_message);
        if (stor.inError)
          throw new Exception(stor.stop_message); 
      } 
      this.thisSession.uVFS.reset();
      if (content_length > 0L || connectionClose || this.chunked) {
        if (content_length > 0L)
          connectionClose = false; 
        try {
          byte[] b = new byte[this.bufferSize];
          int bytes_read = 0;
          while ((connectionClose || content_length > 0L || this.chunked) && bytes_read >= 0) {
            if (!connectionClose && !this.chunked && content_length < b.length)
              b = new byte[(int)content_length]; 
            bytes_read = this.original_is.read(b);
            if (Log.log("SERVER", 2, ""))
              Log.log("HTTP_SERVER", 2, "Write data:  bufferSize=" + this.bufferSize + " content_length=" + content_length + " connectionClose=" + connectionClose + " chunked=" + this.chunked + " bytes_read=" + bytes_read); 
            if (bytes_read > 0) {
              content_length -= bytes_read;
              of_stream.write(b, 0, bytes_read);
            } 
          } 
          of_stream.flush();
        } catch (IOException e) {
          if (Log.log("SERVER", 2, ""))
            Log.log("HTTP_SERVER", 2, "Write data error:  bufferSize=" + this.bufferSize + " content_length=" + content_length + " connectionClose=" + connectionClose + " chunked=" + this.chunked + " error=" + e); 
          Log.log("HTTP_SERVER", 1, e);
          stor.inError = true;
          this.done = true;
        } 
      } 
      if (Log.log("SERVER", 2, ""))
        Log.log("HTTP_SERVER", 2, "Write data closed:  bufferSize=" + this.bufferSize + " content_length=" + content_length + " connectionClose=" + connectionClose + " chunked=" + this.chunked); 
      try {
        of_stream.close();
      } catch (Exception exception) {}
      if (active != null && !active.getProperty("active", "").equals("false")) {
        Log.log("HTTP_SERVER", 2, "Stor handler check: active: " + active.getProperty("active", "") + " stream status: " + active.getProperty("streamOpenStatus", ""));
        try {
          int i = 1;
          long start = System.currentTimeMillis();
          while (!active.getProperty("active", "").equals("false")) {
            Thread.sleep(i++);
            if (i > 100)
              i = 100; 
            if (System.currentTimeMillis() - start > 5000L) {
              Log.log("HTTP_SERVER", 2, "Stor handler check: active: " + active.getProperty("active", "") + " stream status: " + active.getProperty("streamOpenStatus", ""));
              start = System.currentTimeMillis();
            } 
          } 
        } catch (Exception exception) {}
        Log.log("HTTP_SERVER", 2, "Stor handler check complete: active: " + active.getProperty("active", "") + " stream status: " + active.getProperty("streamOpenStatus", ""));
      } 
    } else {
      get_raw_http_command((int)content_length);
    } 
    if (stor != null)
      Log.log("HTTP_SERVER", 2, "Stor error check: " + stor.inError + " stop message: " + stor.stop_message); 
    if (stor != null && stor.inError)
      ok = false; 
    this.thisSession.uVFS.reset();
    String md5_str = "";
    if (stor != null) {
      if (ok) {
        md5_str = stor.getLastMd5().toLowerCase();
        if (md5_str.length() < 32)
          md5_str = "0" + md5_str; 
        Log.log("HTTP_SERVER", 2, "Stor md5: " + md5_str);
      } 
      stor.freeStor();
    } 
    if (ok) {
      Properties item = this.thisSession.uVFS.get_item(user_dir);
      if (item != null)
        this.thisSession.accessExceptions.put(user_dir, item); 
    } 
    Log.log("HTTP_SERVER", 2, "md5: " + md5_str);
    return md5_str;
  }
  
  protected int findByte(byte value, byte[] buffer, int start_pos, int len1, int len2) {
    int loc = start_pos;
    while (loc < len2) {
      if (loc == len1)
        loc = this.bufferSize; 
      if (buffer[loc] == value)
        return loc; 
      loc++;
    } 
    return -1;
  }
  
  protected int findSeparator(byte[] boundary, byte[] buffer, int len1, int len2) {
    len2 += this.bufferSize;
    int start_pos = 0;
    while (start_pos >= 0) {
      int loc = findByte(boundary[0], buffer, start_pos, len1, len2);
      start_pos = loc;
      if (start_pos >= 0)
        start_pos++; 
      if (loc < 0)
        return -1; 
      if (loc > len2 - boundary.length)
        return -1; 
      int boundary_loc = 0;
      int firstLoc = -1;
      while (loc < buffer.length) {
        loc++;
        if (loc == buffer.length || loc == len2)
          break; 
        boundary_loc++;
        if (boundary_loc == boundary.length)
          return firstLoc; 
        if (loc == len1)
          loc = this.bufferSize; 
        if (buffer[loc] != boundary[boundary_loc]) {
          firstLoc = -1;
          break;
        } 
        if (firstLoc == -1)
          firstLoc = loc - 1; 
      } 
    } 
    return -1;
  }
  
  public String get_raw_http_command(int amount) throws Exception {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    byte[] eol = new byte[2];
    int totalBytesRead = 0;
    while (((eol[0] != 13 || eol[1] != 10) && amount < 0 && totalBytesRead < this.bufferSize) || (amount > 0 && totalBytesRead < amount)) {
      byte[] aByte = new byte[1];
      int bytesRead = this.original_is.read(aByte);
      if (bytesRead < 0) {
        try {
          if (amount >= 0)
            this.original_is.close(); 
        } catch (Exception exception) {}
        return "";
      } 
      totalBytesRead++;
      bout.write(aByte, 0, bytesRead);
      eol[0] = eol[1];
      eol[1] = aByte[0];
    } 
    this.thread_killer_item.last_activity = (new Date()).getTime();
    return new String(bout.toByteArray(), "UTF8");
  }
  
  public ServerSessionHTTP(Socket sock, int user_number, String user_ip, int listen_port, String listen_ip, String listen_ip_port, Properties server_item) {
    this.direction = "";
    if (webDavAgents.indexOf("Java/1.7.0") < 0) {
      webDavAgents.addElement("Java/1.6.0");
      webDavAgents.addElement("Java/1.7.0");
      webDavAgents.addElement("Java/1.8.0");
      webDavAgents.addElement("Java/1.9.0");
    } 
    this.sdf_rfc1123.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT")));
    this.sock = sock;
    try {
      sock.setSoTimeout(this.timeoutSeconds * 1000);
    } catch (SocketException socketException) {}
    this.server_item = server_item;
    this.user_ip = user_ip;
    this.listen_port = listen_port;
    this.listen_ip = listen_ip;
    this.listen_ip_port = listen_ip_port;
    this.user_number = user_number;
    try {
      this.original_is = new BufferedInputStream(sock.getInputStream());
    } catch (IOException e) {
      Log.log("SERVER", 1, e);
    } 
  }
  
  public String get_http_command() throws Exception {
    String data = get_raw_http_command(-1);
    data = data.trim();
    data = Common.url_decode(data);
    data = ServerStatus.thisObj.strip_variables(data, this.thisSession);
    return data;
  }
  
  public void write_command_raw(String data) throws Exception {
    Log.log("HTTP_SERVER", 3, data);
    this.original_os.write(data.getBytes("UTF8"));
    this.original_os.flush();
    this.thread_killer_item.last_activity = System.currentTimeMillis();
  }
  
  public int write_command_http_size(String data) throws Exception {
    data = ServerStatus.thisObj.change_vars_to_values(data, this.thisSession);
    data = String.valueOf(data) + this.CRLF;
    return (data.getBytes("UTF8")).length;
  }
  
  public int write_command_http(String data) throws Exception {
    return write_command_http(data, true, false);
  }
  
  public int write_command_http(String data, boolean log, boolean convertVars) throws Exception {
    if (convertVars)
      data = ServerStatus.thisObj.change_vars_to_values(data, this.thisSession); 
    if (log) {
      String data_lower = data.toLowerCase();
      String data_log = data;
      if (data_log.indexOf("<password>") >= 0 && data_log.indexOf("</password>") >= 0)
        data_log = String.valueOf(data_log.substring(0, data_log.indexOf("<password>") + "<password>".length())) + "*******" + data_log.substring(data_log.indexOf("</password>")); 
      if (data_log.indexOf("current_password") >= 0)
        data_log = String.valueOf(data_log.substring(0, data_log.indexOf(":") + 1)) + "*******"; 
      if (data_lower.indexOf("password") >= 0)
        data_log = String.valueOf(data_log.substring(0, data_log.indexOf(":") + 1)) + "*******"; 
      if (data_lower.indexOf("authorization: basic ") >= 0)
        data_log = String.valueOf(data_log.substring(0, data_log.indexOf(":") + 1)) + "*******"; 
      if (data_lower.startsWith("cache-control") || data_lower.startsWith("content-type") || data_lower.startsWith("date") || data_lower.startsWith("server") || data_lower.startsWith("p3p") || data_lower.startsWith("keep-alive") || data_lower.startsWith("connection") || data_lower.startsWith("content-type") || data_lower.startsWith("access-control") || data_lower.startsWith("x-ua-com") || data_lower.startsWith("accept-ranges") || data_lower.startsWith("etag") || data_lower.startsWith("pragma")) {
        if (ServerStatus.IG("log_debug_level") >= 3)
          this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data.trim() + "*", "POST"); 
      } else if (data_lower.startsWith("X-PROXY_USER_PORT".toLowerCase()) || data_lower.startsWith("X-PROXY_BIND_IP".toLowerCase()) || data_lower.startsWith("X-Frame".toLowerCase()) || data_lower.startsWith("Last-Modified".toLowerCase())) {
        if (ServerStatus.IG("log_debug_level") >= 2)
          this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data.trim() + "*", "POST"); 
      } else if (data_lower.startsWith("content-length") || data_lower.trim().equals("") || data_lower.startsWith("<?xml ")) {
        if (data_log.indexOf("url=") >= 0)
          try {
            int start_pos = data_log.indexOf("url=") + 4;
            int end_pos = data_log.indexOf(";", start_pos);
            if (end_pos < start_pos)
              end_pos = data_log.length() - 1; 
            String data_url = data_log.substring(start_pos, end_pos);
            String log_url = Common.url_decode(data_url);
            VRL vrl = new VRL(log_url);
            String pass = vrl.getPassword();
            if (!data_url.contains(pass) && log_url.contains("@")) {
              pass = Common.url_encode(pass);
              if (!data_url.contains(pass))
                try {
                  pass = data_url.substring(data_url.indexOf(":", data_url.indexOf("/")) + 1, data_url.indexOf("@"));
                } catch (Exception exception) {} 
            } 
            data_log = Common.replace_str(data_log, pass, "**********");
          } catch (Exception e) {
            e.printStackTrace();
          }  
        if (ServerStatus.IG("log_debug_level") >= 1)
          this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data_log.trim() + "*", "POST"); 
      } else if (data_lower.indexOf("<listing>") >= 0) {
        this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data_log.trim() + "*", "DIR_LIST");
      } else if (data_log.indexOf("<userInfo type=\"properties\">") >= 0 || data_log.indexOf("\"listing\" : [{") >= 0) {
        this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data_log.trim() + "*", "DIR_LIST");
      } else {
        if (data_log.indexOf("url=") >= 0)
          try {
            int start_pos = data_log.indexOf("url=") + 4;
            int end_pos = data_log.indexOf(";", start_pos);
            if (end_pos < start_pos)
              end_pos = data_log.length() - 1; 
            String data_url = data_log.substring(start_pos, end_pos);
            String log_url = Common.url_decode(data_log.substring(start_pos, end_pos));
            VRL vrl = new VRL(log_url);
            String pass = vrl.getPassword();
            if (!data_url.contains(pass) && log_url.contains("@")) {
              pass = Common.url_encode(pass);
              if (!data_url.contains(pass))
                try {
                  pass = data_url.substring(data_url.indexOf(":", data_url.indexOf("/")) + 1, data_url.indexOf("@"));
                } catch (Exception exception) {} 
            } 
            data_log = Common.replace_str(data_log, pass, "**********");
          } catch (Exception e) {
            e.printStackTrace();
          }  
        this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data_log.trim() + "*", "POST");
      } 
    } 
    data = String.valueOf(data) + this.CRLF;
    write_command_raw(data);
    if (this.deleteCookieAuth) {
      this.writeCookieAuth = false;
      this.deleteCookieAuth = false;
      String expire = "";
      Calendar gc = new GregorianCalendar();
      gc.setTime(new Date());
      gc.add(5, -1);
      expire = "; Expires=" + this.sdf_rfc1123.format(gc.getTime());
      String data2 = "";
      data2 = String.valueOf(data2) + "Set-Cookie: currentAuth=; path=/" + expire + this.secureCookie + this.CRLF;
      String httpOnly = "; HttpOnly";
      data2 = String.valueOf(data2) + "Set-Cookie: CrushAuth=; path=/" + expire + this.secureCookie + httpOnly + this.CRLF;
      write_command_raw(data2);
    } else if (this.writeCookieAuth) {
      if (!this.thisSession.getId().startsWith("HTTP")) {
        Log.log("HTTP_SERVER", 3, "Setting up cookie for this session:" + this.thisSession.uiSG("user_name") + ":" + Thread.currentThread().getName());
        Log.log("HTTP_SERVER", 4, new Exception("Who called us?"));
        setupSession();
        String data2 = "Set-Cookie: mainServerInstance=; path=/" + this.secureCookie + this.CRLF;
        String expire = "";
        if (ServerStatus.IG("cookie_expire_hours") > 0) {
          Calendar gc = new GregorianCalendar();
          gc.setTime(new Date());
          gc.add(10, ServerStatus.IG("cookie_expire_hours"));
          expire = "; Expires=" + this.sdf_rfc1123.format(gc.getTime());
        } 
        String session_id = this.thisSession.getId();
        data2 = String.valueOf(data2) + "Set-Cookie: currentAuth=" + session_id.substring(session_id.length() - 4) + "; path=/" + expire + this.secureCookie + this.CRLF;
        String httpOnly = "; HttpOnly";
        if (this.thisSession.getProperty("clientid") != null && !this.thisSession.getProperty("clientid").equals("null") && !this.thisSession.getProperty("clientid").equals(""))
          httpOnly = ""; 
        if (!ServerStatus.BG("crushauth_httponly"))
          httpOnly = ""; 
        data2 = String.valueOf(data2) + "Set-Cookie: CrushAuth=" + session_id + "; path=/" + expire + this.secureCookie + httpOnly + this.CRLF;
        write_command_raw(data2);
        this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + "_" + this.sock.getPort() + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data2.trim() + "*", "POST");
        this.thisSession.uiPUT("login_date_stamp", this.thisSession.getId());
        this.writeCookieAuth = false;
      } 
    } 
    return data.length();
  }
  
  public void createCookieSession(boolean forceNew) {
    if (forceNew) {
      this.thisSession.killSession();
      this.thisSession.uiPUT("CrushAuth", String.valueOf((new Date()).getTime()) + "_" + Common.makeBoundary(30));
    } 
    if (this.thisSession.user_info.getProperty("user_name_original", "").equals("") || this.thisSession.user_info.getProperty("user_name_original", "").equalsIgnoreCase("anonymous"))
      this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name")); 
    SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user", this.thisSession.uiSG("user_name_original"));
    SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_user", SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user"));
    SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_ip", this.thisSession.uiSG("user_ip"));
  }
  
  public void setupSession() {
    synchronized (SharedSession.sessionLock) {
      SessionCrush existing = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
      if (existing == null) {
        SharedSession.find("crushftp.sessions").put(this.thisSession.getId(), this.thisSession);
      } else {
        if (existing.session_socks == null) {
          existing.session_socks = new Vector();
          existing.data_socks = new Vector();
          existing.old_data_socks = new Vector();
          existing.pasv_socks = new Vector();
          existing.stor_files_pool_free = new Vector();
          existing.retr_files_pool_free = new Vector();
          existing.stor_files_pool_used = new Vector();
          existing.retr_files_pool_used = new Vector();
          existing.hh = new SimpleDateFormat("HH", Locale.US);
          existing.sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
          existing.sdf_yyyyMMddHHmmssGMT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
          existing.active();
        } 
        this.thisSession.user_info = existing.user_info;
      } 
      ServerStatus.thisObj.hold_user_pointer(this.thisSession.user_info);
    } 
  }
  
  public String SG(String data) {
    return this.thisSession.SG(data);
  }
  
  public void write_standard_headers() throws Exception {
    write_standard_headers(true);
  }
  
  public void write_standard_headers(boolean log) throws Exception {
    write_command_http("Date: " + this.sdf_rfc1123.format(new Date()), log, true);
    write_command_http("Server: " + ServerStatus.SG("http_server_header"), log, true);
    write_command_http("P3P: policyref=\"" + this.proxy + "WebInterface/w3c/p3p.xml\", CP=\"IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT\"", log, true);
    for (int x = 1; x <= 20; x++) {
      if (!ServerStatus.SG("http_header" + x).equals(""))
        write_command_http(ServerStatus.SG("http_header" + x).trim()); 
    } 
    if (!ServerStatus.SG("Access-Control-Allow-Origin").equals("")) {
      String origin = this.headerLookup.getProperty("ORIGIN", "");
      for (int i = 0; i < (ServerStatus.SG("Access-Control-Allow-Origin").split(",")).length; i++) {
        boolean ok = false;
        String pattern = ServerStatus.SG("Access-Control-Allow-Origin").split(",")[i];
        if (origin.equals("")) {
          ok = true;
        } else if (pattern.toUpperCase().trim().equalsIgnoreCase(origin.toUpperCase().trim())) {
          ok = true;
        } else if (Common.do_search(pattern.toUpperCase().trim(), origin.toUpperCase().trim(), false, 0)) {
          pattern = origin;
          ok = true;
        } 
        if (ok)
          write_command_http("Access-Control-Allow-Origin: " + pattern.trim()); 
      } 
      write_command_http("Access-Control-Allow-Headers: authorization,content-type,x-transfersegment");
      write_command_http("Access-Control-Allow-Credentials: true");
      write_command_http("Access-Control-Max-Age: 600");
      write_command_http("Access-Control-Allow-Methods: GET,POST,OPTIONS,PUT,PROPFIND,DELETE,MKCOL,MOVE,COPY,HEAD,PROPPATCH,LOCK,UNLOCK,ACL,TR");
    } 
    if (!ServerStatus.SG("Strict-Transport-Security").equals("") && this.thisSession.uiBG("secure"))
      write_command_http("Strict-Transport-Security: " + ServerStatus.SG("Strict-Transport-Security")); 
    if (!ServerStatus.SG("X-Frame-Options").equals(""))
      write_command_http("X-Frame-Options: " + ServerStatus.SG("X-Frame-Options")); 
    if (this.done) {
      write_command_http("Connection: close", log, true);
    } else {
      write_command_http("Keep-Alive: timeout=15, max=20", log, true);
      write_command_http("Connection: Keep-Alive", log, true);
    } 
  }
  
  public void start_idle_timer(int timeout) throws Exception {
    try {
      this.thread_killer_item.timeout = timeout;
      this.thread_killer_item.last_activity = (new Date()).getTime();
      this.thread_killer_item.enabled = (timeout != 0);
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
  }
  
  public void stop_idle_timer() throws Exception {
    try {
      this.thread_killer_item.enabled = false;
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
  }
  
  public void consumeBadData() throws Exception {
    long bytesRead = 0L;
    while (this.http_len_max > 0L) {
      bytesRead = this.original_is.skip(this.http_len_max);
      if (bytesRead > 0L) {
        this.http_len_max -= bytesRead;
        continue;
      } 
      this.done = true;
      break;
    } 
  }
  
  public void DEAUTH() throws Exception {
    write_command_http("HTTP/1.1 401 Unauthorized");
    write_command_http("Pragma: no-cache");
    write_command_http("Connection: close");
    write_command_http("WWW-Authenticate: Basic realm=\"" + this.hostString + "\"");
    write_command_http("Content-Type: text/html;charset=utf-8");
    this.done = true;
    this.thisSession.killSession();
    write_command_http("Content-Length: " + "Unauthorized".length());
    write_command_http("");
    write_command_raw("Unauthorized");
    if (this.thisSession.uVFS != null) {
      this.thisSession.uVFS.free();
      this.thisSession.uVFS.disconnect();
    } 
    this.thisSession.uVFS = null;
    consumeBadData();
  }
  
  public void fixRootDir(String domain, boolean reset) {
    try {
      if (this.thisSession == null || this.thisSession.uVFS == null)
        return; 
      this.thisSession.setupRootDir(domain, reset);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 2, e);
    } 
  }
  
  public void sendRedirect(String path) throws Exception {
    write_command_http("HTTP/1.0 302 Redirect");
    write_command_http("Pragma: no-cache");
    if (this.done) {
      write_command_http("Connection: close");
    } else {
      write_command_http("Keep-Alive: timeout=15, max=20");
      write_command_http("Connection: Keep-Alive");
    } 
    String baseURL = this.proxy;
    path = Common.url_decode(path);
    if (path.toUpperCase().indexOf("SCRIPT") >= 0)
      path = path.replace('<', '_').replace('>', '_'); 
    if (path.toUpperCase().startsWith("HTTP")) {
      write_command_http("location: " + path);
    } else {
      if (path.startsWith("/") && baseURL.endsWith("/"))
        path = path.substring(1); 
      if (path.indexOf("/#") >= 0 || (path.startsWith("#/") && baseURL.endsWith("/"))) {
        String start = path.substring(0, path.indexOf("/#") + 2);
        String end = path.substring(path.indexOf("/#") + 2);
        end = Base64.encodeBytes(end.getBytes("UTF8"));
        write_command_http("location: " + baseURL + start + "BASE64CRUSH_" + end);
      } else if (path.indexOf("#") >= 0) {
        write_command_http("location: " + baseURL + Common.url_encode(path));
      } else {
        write_command_http("location: " + baseURL + path);
      } 
    } 
    if (!ServerStatus.SG("Strict-Transport-Security").equals("") && this.thisSession.uiBG("secure"))
      write_command_http("Strict-Transport-Security: " + ServerStatus.SG("Strict-Transport-Security")); 
  }
  
  public String getBaseUrl(String hostString) {
    String serverType = this.server_item.getProperty("serverType", "http");
    if (this.reverseProxyHttps)
      serverType = "HTTPS"; 
    return String.valueOf(serverType) + "://" + hostString + this.proxy;
  }
  
  public void sendHttpsRedirect(String path) throws Exception {
    write_command_http("HTTP/1.0 302 Redirect");
    write_command_http("Pragma: no-cache");
    Vector server_list = (Vector)ServerStatus.server_settings.get("server_list");
    String port = "443";
    int start = server_list.indexOf(this.thisSession.server_item);
    if (start < 0)
      start = 0; 
    for (int x = start; x < server_list.size(); x++) {
      Properties p = server_list.elementAt(x);
      if (p.getProperty("serverType", "FTP").equalsIgnoreCase("HTTPS") || p.getProperty("port", "443").equalsIgnoreCase("443")) {
        port = p.getProperty("port", "443");
        break;
      } 
    } 
    if (port.equals("443")) {
      port = "";
    } else {
      port = ":" + port;
    } 
    String tempHost = this.hostString;
    if (tempHost.indexOf(":") >= 0)
      tempHost = tempHost.substring(0, tempHost.indexOf(":")); 
    String newPath = String.valueOf(this.proxy) + path;
    if (newPath.startsWith("//"))
      newPath = newPath.substring(1); 
    write_command_http("location: https://" + tempHost + port + newPath);
    if (!ServerStatus.SG("Strict-Transport-Security").equals("") && this.thisSession.uiBG("secure"))
      write_command_http("Strict-Transport-Security: " + ServerStatus.SG("Strict-Transport-Security")); 
  }
  
  public void processMiniURLs(String header0, Vector miniURLs) {
    try {
      if (miniURLs != null)
        if (header0.toUpperCase().startsWith("GET ")) {
          String data = header0.substring(header0.indexOf(" ") + 1, header0.lastIndexOf(" "));
          String miniURL = data.substring(data.lastIndexOf("/") + 1);
          for (int x = 0; x < miniURLs.size(); x++) {
            Properties p = miniURLs.elementAt(x);
            if (Common.do_search(p.getProperty("key").toUpperCase(), data.toUpperCase().substring(1), false, 0) || p.getProperty("key", "").equalsIgnoreCase(miniURL)) {
              boolean expired = false;
              SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm aa", Locale.US);
              try {
                if (!p.getProperty("expire", "").equals(""))
                  if (sdf.parse(p.getProperty("expire", "")).getTime() < System.currentTimeMillis()) {
                    Log.log("SERVER", 0, "MiniURL Expired:" + p.getProperty("key") + " : " + sdf.parse(p.getProperty("expire", "")).getTime() + " < " + System.currentTimeMillis());
                    expired = true;
                  }  
              } catch (Exception e) {
                Log.log("SERVER", 0, e);
                expired = true;
              } 
              if (expired) {
                miniURLs.removeElementAt(x);
                Properties pp = new Properties();
                pp.put("id", String.valueOf((new Date()).getTime()) + ":" + Common.makeBoundary());
                pp.put("complete", "false");
                pp.put("data", ServerStatus.server_settings);
                ServerStatus.thisObj.setSettings(pp);
                break;
              } 
              p = (Properties)Common.CLONE(p);
              p.put("username", p.getProperty("user", ""));
              p.put("password", ServerStatus.thisObj.common_code.decode_pass(p.getProperty("pass", "")));
              String redirect = p.getProperty("redirect", "/");
              if (redirect.indexOf("://") < 0 && !redirect.startsWith("/"))
                redirect = "/" + redirect; 
              if (!p.getProperty("key", "").equalsIgnoreCase(miniURL) && data.length() > p.getProperty("key", "").length()) {
                redirect = String.valueOf(redirect) + data.substring(p.getProperty("key", "").length() + 1);
                redirect = Common.replace_str(redirect, "//", "/");
              } 
              boolean good = this.ssa.checkLogin1(p);
              if (good) {
                this.thisSession.user_info.put("miniUrlLogin", "true");
                this.thisSession.user_info.put("miniUrl", p);
                this.ssa.checkLogin2("", p);
              } 
              sendRedirect(redirect);
              write_command_http("Connection: close");
              write_command_http("");
              this.done = true;
              if (this.thisSession.uVFS != null) {
                this.thisSession.uVFS.free();
                this.thisSession.uVFS.disconnect();
              } 
              break;
            } 
          } 
        }  
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
  }
  
  public void savePropPatches() throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop.save")));
    oos.writeObject(proppatches);
    oos.close();
    (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop")).delete();
    (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop.save")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop"));
  }
  
  public void saveLockTokens() throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop.save")));
    oos.writeObject(locktokens);
    oos.close();
    (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop")).delete();
    (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop.save")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop"));
  }
  
  public void cd(String user_dir) {
    user_dir = Common.dots(user_dir);
    this.http_dir = user_dir;
    this.thisSession.uiPUT("current_dir", user_dir);
  }
  
  public String pwd() {
    if (this.http_dir == null)
      this.http_dir = this.thisSession.uiSG("current_dir"); 
    return this.http_dir;
  }
  
  public void setupCurrentDir(String path) {
    path = this.thisSession.getStandardizedDir(path);
    cd(path);
  }
  
  public Vector parsePostArguments(String boundary, long max_len, boolean allow_file, String req_id) throws Exception {
    this.original_is.mark(70000);
    Properties metaInfo = new Properties();
    boolean speedCheat = false;
    Vector items = new Vector();
    Properties item = null;
    Properties globalItems = new Properties();
    String data = "";
    boolean start_new_item = false;
    long len = 4L;
    boolean dataAlreadyRead = false;
    boolean fileUploaded = false;
    int emptyDataLoops = 0;
    Properties activeUpload = null;
    String lastUploadName = "";
    Vector logData = new Vector();
    STOR_handler stor_shared = null;
    start_idle_timer(-30);
    try {
      while (!this.done) {
        if (boundary.equals(""))
          break; 
        if (!dataAlreadyRead) {
          try {
            this.sock.setSoTimeout(5000);
          } catch (SocketException socketException) {}
          try {
            data = get_http_command();
            len += (data.length() + 2);
            data = data.trim();
            data = Common.url_decode(data);
          } catch (SocketTimeoutException e) {
            this.done = true;
            throw e;
          } 
          try {
            this.sock.setSoTimeout(this.timeoutSeconds * 1000);
          } catch (SocketException socketException) {}
          if (data.equals("") && emptyDataLoops++ > 500)
            break; 
        } 
        dataAlreadyRead = false;
        if (data.endsWith(boundary))
          start_new_item = true; 
        if (data.endsWith(String.valueOf(boundary) + "--"))
          break; 
        if (start_new_item) {
          item = new Properties();
          items.addElement(item);
          data = get_http_command();
          len += (data.length() + 2);
          data = data.trim();
          data = Common.url_decode(data);
          String name = data.substring(data.indexOf("name=\"") + 6, data.indexOf("\"", data.indexOf("name=\"") + 6));
          if (name.endsWith("_SINGLE_FILE_POST"))
            speedCheat = true; 
          if (globalItems.getProperty("speedCheat", "").equals("true"))
            speedCheat = true; 
          long speed_cheat_len = Long.parseLong(globalItems.getProperty("speedCheatSize", (new StringBuffer(String.valueOf(max_len))).toString()));
          if (speed_cheat_len < (this.bufferSize * 10))
            speedCheat = false; 
          if (data.indexOf("filename") >= 0 && !allow_file) {
            String upload_item = data.substring(data.indexOf("filename=\"") + 10, data.indexOf("\"", data.indexOf("filename=\"") + 10));
            if (globalItems.containsKey("alt_name"))
              upload_item = globalItems.getProperty("alt_name"); 
            upload_item = Common.dots(upload_item.replace(':', '_'));
            Properties p = new Properties();
            p.put("filename", upload_item);
            p.put("type", "file");
            p.put("processFileUpload", "true");
            items.addElement(p);
            this.original_is.reset();
            break;
          } 
          if (data.indexOf("filename") >= 0 && allow_file) {
            stop_idle_timer();
            String session_id = this.thisSession.getId();
            if (ServerStatus.BG("csrf"))
              if (("," + ServerStatus.SG("whitelist_web_commands") + ",").indexOf("," + globalItems.getProperty("command") + ",") < 0 && this.thisSession.user_info.getProperty("authorization_header", "false").equals("false"))
                if (!globalItems.getProperty("c2f", "").equalsIgnoreCase(session_id.substring(session_id.length() - 4)) && !globalItems.getProperty("c2f", "").equalsIgnoreCase(ServerStatus.thisObj.common_code.decode_pass(this.thisSession.user.getProperty("password")))) {
                  this.thisSession.uiVG("failed_commands").addElement((new Date()).getTime());
                  if (lastUploadName != null && activeUpload != null)
                    activeUpload.put((new StringBuffer(String.valueOf(lastUploadName))).toString(), String.valueOf(LOC.G("ERROR")) + ": FAILURE:Access Denied. (c2f)"); 
                  throw new Exception("FAILURE:Access Denied. (c2f)");
                }   
            logVector(logData, req_id);
            logData.clear();
            this.thisSession.uiPUT("the_command", "STOR");
            lastUploadName = name;
            fileUploaded = true;
            if (Common.System2.get("crushftp.activeUpload.info" + this.thisSession.getId()) == null)
              Common.System2.put("crushftp.activeUpload.info" + this.thisSession.getId(), new Properties()); 
            activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSession.getId());
            item.put("type", "file");
            String upload_item = data.substring(data.indexOf("filename=\"") + 10, data.indexOf("\"", data.indexOf("filename=\"") + 10));
            upload_item = Common.normalize2(upload_item);
            if (globalItems.containsKey("alt_name"))
              upload_item = globalItems.getProperty("alt_name"); 
            upload_item = Common.dots(upload_item.replace(':', '_'));
            if (upload_item.indexOf("\\") >= 0)
              upload_item = upload_item.replace('\\', '/'); 
            if (upload_item.indexOf("/") >= 0)
              upload_item = Common.last(upload_item); 
            if (globalItems.getProperty("the_action2", "").equals("changeIcon"))
              upload_item = "changeIcon_" + Common.makeBoundary(3) + "_" + upload_item; 
            Properties p = new Properties();
            item.put("file", p);
            String the_file = Common.last(upload_item);
            for (int x = 0; x < ServerStatus.SG("unsafe_filename_chars").length(); x++)
              the_file = the_file.replace(ServerStatus.SG("unsafe_filename_chars").charAt(x), '_'); 
            upload_item = String.valueOf(Common.all_but_last(upload_item)) + the_file;
            p.put("filename", upload_item);
            data = get_http_command();
            len += (data.length() + 2);
            data = data.trim();
            data = Common.url_decode(data);
            p.put("encoding", data);
            get_http_command();
            len += 2L;
            if (activeUpload != null && !activeUpload.getProperty(lastUploadName, "").startsWith("ERROR:"))
              activeUpload.put(lastUploadName, "PROGRESS:" + len + "/" + speed_cheat_len + ";" + Common.url_encode(upload_item)); 
            boolean ok = false;
            if (!globalItems.getProperty("uploadPath", "").equals("")) {
              setupCurrentDir(globalItems.getProperty("uploadPath", ""));
              this.thisSession.uiPUT("the_command_data", "");
              this.thisSession.do_MKD(true, pwd());
            } 
            this.thisSession.uiPUT("start_resume_loc", globalItems.getProperty("start_resume_loc", "0"));
            Properties dir_item = this.thisSession.uVFS.get_item(String.valueOf(pwd()) + upload_item);
            if (this.thisSession.check_access_privs(String.valueOf(pwd()) + upload_item, "STOR") && Common.filter_check("U", Common.last(String.valueOf(pwd()) + upload_item), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")) && (dir_item == null || dir_item.getProperty("type").equalsIgnoreCase("file") || dir_item.getProperty("simple", "").equalsIgnoreCase("true"))) {
              ok = true;
            } else {
              activeUpload.put(lastUploadName, String.valueOf(LOC.G("ERROR")) + ": " + LOC.G("Access denied. (You do not have permission or the file extension is not allowed.)"));
            } 
            if (upload_item.equals(""))
              ok = false; 
            if (ok) {
              if (this.thisSession.getProperty("blockUploads", "false").equals("true"))
                throw new Exception("Upload failed: User Cancelled"); 
              activeUpload.put(lastUploadName, "PROGRESS:" + len + "/" + speed_cheat_len + ";" + Common.url_encode(upload_item));
              if (max_len > 0L)
                this.thisSession.uiPUT("file_length", (new StringBuffer(String.valueOf(max_len - len))).toString()); 
              Properties result = getStorOutputStream(String.valueOf(pwd()) + upload_item, Long.parseLong(globalItems.getProperty("start_resume_loc", "0")), globalItems.getProperty("randomaccess", "false").equals("true"), metaInfo);
              OutputStream of_stream = (OutputStream)result.remove("out");
              stor_shared = (STOR_handler)result.remove("stor");
              Properties active = (Properties)result.get("active");
              stor_shared.user_agent = this.headerLookup.getProperty("User-Agent".toUpperCase(), "").trim();
              if (globalItems.containsKey("Last-Modified") || this.headerLookup.containsKey("LAST-MODIFIED")) {
                this.headerLookup.put("LAST_MODIFIED", globalItems.getProperty("Last-Modified", this.headerLookup.getProperty("LAST-MODIFIED")));
                stor_shared.fileModifiedDate = this.sdf_rfc1123.parse(this.headerLookup.getProperty("LAST-MODIFIED").trim()).getTime();
              } 
              try {
                byte[] buffer = new byte[this.bufferSize * 2];
                byte[] boundaryBytes = ("\r\n--" + boundary).getBytes();
                int len1 = 0;
                int len2 = 0;
                byte[] b = new byte[this.bufferSize];
                int bytes_read = 0;
                this.original_is.mark(0);
                String upload_item_url_encode = Common.url_encode(upload_item);
                while ((speedCheat || findSeparator(boundaryBytes, buffer, len1, len2) < 0) && bytes_read >= 0) {
                  activeUpload.put(lastUploadName, "PROGRESS:" + len + "/" + speed_cheat_len + ";" + upload_item_url_encode);
                  this.original_is.reset();
                  if (len1 > 0 && len2 > 0) {
                    of_stream.write(buffer, 0, len1);
                    len += len1;
                    this.original_is.skip(len1);
                    this.original_is.mark(b.length * 3);
                    this.original_is.skip(len2);
                    System.arraycopy(buffer, this.bufferSize, buffer, 0, len2);
                    len1 = len2;
                  } else {
                    System.arraycopy(buffer, this.bufferSize, buffer, 0, len2);
                    len1 = len2;
                    this.original_is.mark(b.length * 3);
                    this.original_is.skip(bytes_read);
                  } 
                  bytes_read = this.original_is.read(b);
                  if (bytes_read > 0) {
                    System.arraycopy(b, 0, buffer, this.bufferSize, bytes_read);
                    len2 = bytes_read;
                  } 
                  if (speed_cheat_len - len < (this.bufferSize * 4))
                    speedCheat = false; 
                  if (!active.getProperty("active", "").equals("true") && stor_shared.inError)
                    throw new Exception("Upload failed:" + stor_shared.stop_message); 
                } 
                if (bytes_read < 0) {
                  stor_shared.inError = true;
                  Log.log("HTTP_SERVER", 1, "An error occurred during the POST upload:" + upload_item);
                } 
                this.original_is.reset();
                int loc = findSeparator(boundaryBytes, buffer, len1, len2);
                if (loc == this.bufferSize - 1) {
                  this.original_is.skip(0L);
                } else if (loc < this.bufferSize) {
                  of_stream.write(buffer, 0, loc);
                  len += loc;
                  this.original_is.skip(loc);
                } else {
                  of_stream.write(buffer, 0, len1);
                  len += len1;
                  of_stream.write(buffer, this.bufferSize, loc - this.bufferSize);
                  len += (loc - this.bufferSize);
                  this.original_is.skip(len1);
                  this.original_is.skip(loc - this.bufferSize);
                } 
              } catch (Exception e) {
                Log.log("HTTP_SERVER", 1, e);
                this.keepGoing = false;
                stor_shared.inError = true;
                this.done = true;
                len = max_len;
                stor_shared.die_now = true;
                Properties errorItem = new Properties();
                items.addElement(errorItem);
                errorItem.put("responseHeader", "HTTP/1.1 200 " + stor_shared.stop_message);
              } 
              try {
                of_stream.close();
              } catch (Exception exception) {}
              while (active.getProperty("active", "").equals("true"))
                Thread.sleep(100L); 
              try {
                stor_shared.c.close();
              } catch (Exception exception) {}
              try {
                Properties newItem1 = this.thisSession.uVFS.get_item(String.valueOf(pwd()) + upload_item);
                if (newItem1 != null) {
                  String previewPath = Common.all_but_last(SearchHandler.getPreviewPath(newItem1.getProperty("url"), "1", 1)).trim();
                  if (globalItems.getProperty("the_action2", "").equals("changeIcon")) {
                    Properties newItem2 = this.thisSession.uVFS.get_item(String.valueOf(pwd()) + Common.last(globalItems.getProperty("changeIconItem")));
                    VRL v1 = new VRL(newItem1.getProperty("url"));
                    VRL v2 = new VRL(newItem2.getProperty("url"));
                    for (int i = 0; i < ServerStatus.thisObj.previewWorkers.size(); i++) {
                      PreviewWorker preview = ServerStatus.thisObj.previewWorkers.elementAt(i);
                      if (preview.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true") && (preview.prefs.getProperty("preview_command_line").indexOf("convert") >= 0 || preview.prefs.getProperty("preview_command_line").indexOf("manage") >= 0)) {
                        GenericClient c = this.thisSession.uVFS.getClient(newItem2);
                        preview.doConvert(c, newItem1, newItem2, false, new Properties(), true);
                        this.thisSession.uVFS.releaseClient(c);
                      } 
                    } 
                    cd(String.valueOf(pwd()) + upload_item);
                    this.thisSession.uiPUT("the_command", "DELE");
                    this.thisSession.uiPUT("the_command_data", pwd());
                    this.thisSession.do_DELE(false, pwd());
                    (new File_S(v1.getPath())).delete();
                  } else if (!previewPath.equals("") && !previewPath.equals("/") && !previewPath.equals(".") && !previewPath.equals("./")) {
                    previewPath = String.valueOf(ServerStatus.SG("previews_path")) + previewPath;
                    if ((new File_S(previewPath)).exists())
                      Common.recurseDelete(previewPath, false); 
                  } 
                } 
              } catch (Exception e) {
                Log.log("HTTP_SERVER", 2, e);
              } 
              activeUpload.put(lastUploadName, "PROGRESS:" + len + "/" + speed_cheat_len + ";" + Common.url_encode(upload_item));
            } else {
              this.done = true;
            } 
            if (stor_shared != null && stor_shared.stop_message.length() > 0)
              activeUpload.put(lastUploadName, String.valueOf(LOC.G("ERROR")) + ":" + stor_shared.stop_message); 
          } else {
            item.put("type", "text");
            data = get_http_command();
            len += (data.length() + 2);
            data = data.trim();
            data = Common.url_decode(data);
            String data_item = "";
            dataAlreadyRead = true;
            while (true) {
              data = get_http_command();
              len += (data.length() + 2);
              data = data.trim();
              data = Common.url_decode(data);
              if (data.equals("") && emptyDataLoops++ > 500)
                break; 
              if (data.endsWith(boundary) || 
                data.endsWith(String.valueOf(boundary) + "--") || (
                len >= max_len && max_len > 0L))
                break; 
              data_item = String.valueOf(data_item) + data + this.CRLF;
            } 
            data_item = data_item.substring(0, data_item.length() - 2);
            item.put(name, data_item);
            if (globalItems.containsKey(name)) {
              globalItems.put(name, String.valueOf(globalItems.getProperty(name)) + "," + data_item);
            } else {
              globalItems.put(name, data_item);
            } 
            if (name.toUpperCase().startsWith("META_")) {
              name = name.substring(5);
              if (metaInfo.containsKey(name)) {
                metaInfo.put(name, String.valueOf(metaInfo.getProperty(name)) + "," + data_item);
              } else {
                metaInfo.put(name, data_item);
              } 
              if (name.toUpperCase().startsWith("GLOBAL_")) {
                if (Common.System2.get("global_variables") == null)
                  Common.System2.put("global_variables", new Properties()); 
                Properties global_variables = (Properties)Common.System2.get("global_variables");
                global_variables.put(name, data_item);
              } else if (name.toUpperCase().startsWith("USER_INFO_")) {
                this.thisSession.user_info.put(name, data_item);
              } 
            } 
            this.thisSession.put("last_metaInfo", metaInfo);
            if (name.toUpperCase().indexOf("PASS") >= 0)
              data_item = "***"; 
            if (data_item.indexOf("<password>") >= 0 && data_item.indexOf("</password>") >= 0) {
              data_item = String.valueOf(data_item.substring(0, data_item.indexOf("<password>") + "<password>".length())) + "*******" + data_item.substring(data_item.indexOf("</password>"));
            } else if (data_item.indexOf("current_password") >= 0) {
              data_item = String.valueOf(data_item.substring(0, data_item.indexOf(":") + 1)) + "*******";
            } else if (data_item.toUpperCase().indexOf("PASSWORD") >= 0) {
              data_item = String.valueOf(data_item.substring(0, data_item.indexOf(":") + 1)) + "*******";
            } 
            logData.addElement(String.valueOf(name) + ":" + data_item);
          } 
          start_new_item = false;
        } 
        if (len >= max_len && max_len > 0L)
          break; 
      } 
    } finally {
      logVector(logData, req_id);
      logData.clear();
      stop_idle_timer();
    } 
    if (stor_shared != null) {
      String md5_str = stor_shared.getLastMd5().toLowerCase();
      if (md5_str.length() < 32)
        md5_str = "0" + md5_str; 
      activeUpload.put("last_md5", md5_str);
      stor_shared.freeStor();
    } 
    if (activeUpload != null && !activeUpload.getProperty(lastUploadName, "").startsWith("ERROR:") && !globalItems.containsKey("chunk_upload"))
      activeUpload.put(lastUploadName, String.valueOf(LOC.G("DONE")) + ":" + System.currentTimeMillis()); 
    String metaString = "";
    Enumeration keys = metaInfo.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      String val = metaInfo.getProperty(key);
      metaString = String.valueOf(metaString) + key + "=" + Common.url_encode(val) + "|";
    } 
    if (metaString.length() > 0)
      metaString = metaString.substring(0, metaString.length() - 1); 
    return items;
  }
  
  public void parseDownloadSegment(String boundary, String req_id, String transfer_key) throws Exception {
    if (this.headerLookup.containsKey("X-TRANSFERSEGMENT")) {
      String ref_id = "0.0.0.0:" + transfer_key.split("~")[1].trim();
      String CrushAuth = ServerStatus.siPG("domain_cross_reference").getProperty(ref_id).split(":")[1];
      if (ServerStatus.siPG("domain_cross_reference").containsKey(ref_id))
        ServerStatus.siPG("domain_cross_reference").put(ref_id, String.valueOf(System.currentTimeMillis()) + ":" + CrushAuth); 
      this.thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(CrushAuth);
      this.writeCookieAuth = false;
    } 
    Properties html5_transfers = (Properties)this.thisSession.get("html5_downloads");
    Properties transfer_lock = null;
    for (int x = 0; x < 200 && transfer_lock == null; x++) {
      transfer_lock = (Properties)html5_transfers.get(transfer_key.split("~")[0]);
      if (transfer_lock != null)
        break; 
      Thread.sleep(((x < 100) ? x : 100L));
    } 
    if (transfer_lock == null)
      throw new Exception("No open file found for transfer_id:" + transfer_key); 
    if (transfer_lock.getProperty("status", "").startsWith("ERROR:"))
      throw new Exception(transfer_lock.getProperty("status", "")); 
    int chunk_num = Integer.parseInt(this.headerLookup.getProperty("X-TRANSFERSEGMENT", transfer_key.split("~")[1]));
    for (int i = 0; i < 200 && !transfer_lock.containsKey("total_chunks"); i++) {
      if (transfer_lock.containsKey((new StringBuffer(String.valueOf(chunk_num))).toString()))
        break; 
      Thread.sleep(((i < 100) ? i : 100L));
    } 
    if (transfer_lock.containsKey((new StringBuffer(String.valueOf(chunk_num))).toString())) {
      if (Log.log("HTTP_SERVER", 2, ""))
        Log.log("HTTP_SERVER", 2, "Download segment SUCCESS request:id=" + transfer_lock.getProperty("transfer_id") + ":num=" + chunk_num + ":pending=" + transfer_lock.getProperty("pending_bytes", "0") + ":transfer_lock_size=" + transfer_lock.size() + ":current_num=" + transfer_lock.getProperty("current_num", "0")); 
      int bytes_read = 0;
      byte[] b = (byte[])null;
      synchronized (transfer_lock) {
        Properties chunk = (Properties)transfer_lock.remove((new StringBuffer(String.valueOf(chunk_num))).toString());
        bytes_read = Integer.parseInt(chunk.getProperty("bytes_read"));
        b = (byte[])chunk.remove("b");
        transfer_lock.put("pending_bytes", (new StringBuffer(String.valueOf(Long.parseLong(transfer_lock.getProperty("pending_bytes", "0")) - bytes_read))).toString());
      } 
      write_command_http("HTTP/1.1 200 OK");
      write_standard_headers();
      write_command_http("Content-Length: " + bytes_read);
      write_command_http("");
      this.original_os.write(b, 0, bytes_read);
      this.original_os.flush();
      this.thread_killer_item.last_activity = System.currentTimeMillis();
    } else if (transfer_lock.containsKey("total_chunks") && chunk_num > Integer.parseInt(transfer_lock.getProperty("total_chunks"))) {
      if (Log.log("HTTP_SERVER", 2, ""))
        Log.log("HTTP_SERVER", 2, "Download completed with SUCCESS request:id=" + transfer_lock.getProperty("transfer_id") + ":num=" + chunk_num + ":pending=" + transfer_lock.getProperty("pending_bytes", "0") + ":transfer_lock_size=" + transfer_lock.size() + ":current_num=" + transfer_lock.getProperty("current_num", "0")); 
      write_command_http("HTTP/1.1 200 OK");
      write_standard_headers();
      write_command_http("Content-Length: 0");
      write_command_http("");
      this.original_os.flush();
      this.thread_killer_item.last_activity = System.currentTimeMillis();
    } else {
      Log.log("SERVER", 0, "Download segment FAILURE request:id=" + transfer_lock.getProperty("transfer_id") + ":num=" + chunk_num + ":pending=" + transfer_lock.getProperty("pending_bytes", "0") + ":transfer_lock_size=" + transfer_lock.size() + ":current_num=" + transfer_lock.getProperty("current_num", "0"));
      String msg = "Max chunk:" + transfer_lock.getProperty("current_num", "0");
      write_command_http("HTTP/1.1 404 Failure");
      write_standard_headers();
      write_command_http("Content-Length: " + (msg.length() + 2));
      write_command_http("");
      write_command_http(msg);
    } 
  }
  
  public void parseUploadSegment(String boundary, long max_len, String req_id, String transfer_key) throws Exception {
    this.original_is.mark(70000);
    String data = "";
    boolean start_new_item = false;
    long len = 4L;
    boolean dataAlreadyRead = false;
    int emptyDataLoops = 0;
    while (!this.done) {
      if (boundary.equals(""))
        break; 
      if (!dataAlreadyRead) {
        try {
          this.sock.setSoTimeout(5000);
        } catch (SocketException socketException) {}
        try {
          data = get_http_command();
          len += (data.length() + 2);
          data = data.trim();
          data = Common.url_decode(data);
        } catch (SocketTimeoutException e) {
          this.done = true;
          throw e;
        } 
        try {
          this.sock.setSoTimeout(this.timeoutSeconds * 1000);
        } catch (SocketException socketException) {}
        if (data.equals("") && emptyDataLoops++ > 500)
          break; 
      } 
      dataAlreadyRead = false;
      if (data.endsWith(boundary))
        start_new_item = true; 
      if (data.endsWith(String.valueOf(boundary) + "--"))
        break; 
      if (start_new_item) {
        data = get_http_command();
        len += (data.length() + 2);
        data = data.trim();
        data = Common.url_decode(data);
        if (data.indexOf("CFCD") >= 0) {
          if (this.headerLookup.containsKey("X-TRANSFERSEGMENT")) {
            String ref_id = "0.0.0.0:" + transfer_key.split("~")[1].trim();
            String CrushAuth = ServerStatus.siPG("domain_cross_reference").getProperty(ref_id).split(":")[1];
            if (ServerStatus.siPG("domain_cross_reference").containsKey(ref_id))
              ServerStatus.siPG("domain_cross_reference").put(ref_id, String.valueOf(System.currentTimeMillis()) + ":" + CrushAuth); 
            this.thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(CrushAuth);
            this.writeCookieAuth = false;
          } 
          Properties html5_transfers = (Properties)this.thisSession.get("html5_uploads");
          Properties transfer_lock = null;
          for (int x = 0; x < 200 && transfer_lock == null; x++) {
            transfer_lock = (Properties)html5_transfers.get(transfer_key.split("~")[0]);
            if (transfer_lock != null)
              break; 
            Thread.sleep(((x < 100) ? x : 100L));
          } 
          if (transfer_lock == null)
            throw new Exception("No open file found for transfer_id:" + transfer_key); 
          if (transfer_lock.getProperty("status", "").startsWith("ERROR:"))
            throw new Exception(transfer_lock.getProperty("status", "")); 
          int chunk_num = 0;
          int chunk_size = 0;
          if (this.headerLookup.getProperty("X-TRANSFERSEGMENT", "").equals("")) {
            chunk_num = Integer.parseInt(transfer_key.split("~")[1]);
            chunk_size = Integer.parseInt(transfer_key.split("~")[2]);
          } else {
            chunk_num = Integer.parseInt(this.headerLookup.getProperty("X-TRANSFERSEGMENT").split("~")[0]);
            chunk_size = Integer.parseInt(this.headerLookup.getProperty("X-TRANSFERSEGMENT").split("~")[1]);
          } 
          ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
          data = get_http_command();
          len += (data.length() + 2);
          data = data.trim();
          data = Common.url_decode(data);
          get_http_command();
          len += 2L;
          byte[] buffer = new byte[this.bufferSize * 2];
          byte[] boundaryBytes = ("\r\n--" + boundary).getBytes();
          int len1 = 0;
          int len2 = 0;
          byte[] b = new byte[this.bufferSize];
          int bytes_read = 0;
          this.original_is.mark(0);
          while (findSeparator(boundaryBytes, buffer, len1, len2) < 0 && bytes_read >= 0) {
            this.original_is.reset();
            if (len1 > 0 && len2 > 0) {
              baos.write(buffer, 0, len1);
              len += len1;
              this.original_is.skip(len1);
              this.original_is.mark(b.length * 3);
              this.original_is.skip(len2);
              System.arraycopy(buffer, this.bufferSize, buffer, 0, len2);
              len1 = len2;
            } else {
              System.arraycopy(buffer, this.bufferSize, buffer, 0, len2);
              len1 = len2;
              this.original_is.mark(b.length * 3);
              this.original_is.skip(bytes_read);
            } 
            bytes_read = this.original_is.read(b);
            if (bytes_read > 0) {
              System.arraycopy(b, 0, buffer, this.bufferSize, bytes_read);
              len2 = bytes_read;
            } 
            if (baos.size() > 26214400)
              throw new Exception("Chunk buffer size too big:" + baos.size()); 
          } 
          if (bytes_read < 0)
            Log.log("HTTP_SERVER", 1, "An error occurred during the transfer:" + transfer_key); 
          this.original_is.reset();
          int loc = findSeparator(boundaryBytes, buffer, len1, len2);
          if (loc == this.bufferSize - 1) {
            this.original_is.skip(0L);
          } else if (loc < this.bufferSize) {
            baos.write(buffer, 0, loc);
            len += loc;
            this.original_is.skip(loc);
          } else {
            baos.write(buffer, 0, len1);
            len += len1;
            baos.write(buffer, this.bufferSize, loc - this.bufferSize);
            len += (loc - this.bufferSize);
            this.original_is.skip(len1);
            this.original_is.skip(loc - this.bufferSize);
          } 
          if (chunk_size != baos.size())
            throw new IOException("FAILURE:Chunk size mismatch.  Expected " + chunk_size + " but got " + baos.size() + " for chunk " + chunk_num + ". " + transfer_key); 
          if (chunk_num >= Integer.parseInt(transfer_lock.getProperty("current_num", "0"))) {
            synchronized (transfer_lock) {
              transfer_lock.put("pending_bytes", (new StringBuffer(String.valueOf(Long.parseLong(transfer_lock.getProperty("pending_bytes", "0")) + baos.size()))).toString());
            } 
            if (chunk_num > Integer.parseInt(transfer_lock.getProperty("current_num", "0")))
              if (Long.parseLong(transfer_lock.getProperty("pending_bytes", "0")) > 104857600L) {
                int half_secs = 0;
                while (Long.parseLong(transfer_lock.getProperty("pending_bytes", "0")) > 104857600L && half_secs++ < 120)
                  Thread.sleep(500L); 
              } else if (transfer_lock.size() > 11) {
                Thread.sleep(1000L);
              }  
            transfer_lock.put((new StringBuffer(String.valueOf(chunk_num))).toString(), baos.toByteArray());
          } 
        } else {
          data = get_http_command();
          len += (data.length() + 2);
          data = data.trim();
          data = Common.url_decode(data);
          String data_item = "";
          dataAlreadyRead = true;
          while (true) {
            data = get_http_command();
            len += (data.length() + 2);
            data = data.trim();
            data = Common.url_decode(data);
            if (data.equals("") && emptyDataLoops++ > 500)
              break; 
            if (data.endsWith(boundary) || 
              data.endsWith(String.valueOf(boundary) + "--") || (
              len >= max_len && max_len > 0L))
              break; 
            data_item = String.valueOf(data_item) + data + this.CRLF;
          } 
          data_item = data_item.substring(0, data_item.length() - 2);
        } 
        start_new_item = false;
      } 
      if (len >= max_len && max_len > 0L)
        break; 
    } 
  }
  
  public void logout_all() {
    String auth_temp = this.thisSession.getId();
    this.thisSession.do_event5("LOGOUT_ALL", null);
    this.thisSession.uiPUT("user_name_original", "");
    createCookieSession(true);
    this.done = true;
    if (this.thisSession.uVFS != null) {
      this.thisSession.uVFS.free();
      this.thisSession.uVFS.disconnect();
    } 
    ServerStatus.thisObj.remove_user(this.thisSession.user_info);
    if (SharedSessionReplicated.send_queues.size() > 0 && ServerStatus.BG("replicate_sessions"))
      SharedSessionReplicated.send(auth_temp, "crushftp.session.remove_user", "user_info", null); 
  }
}
