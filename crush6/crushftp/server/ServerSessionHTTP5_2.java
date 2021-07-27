package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.UnChunkInputStream;
import com.crushftp.client.VRL;
import com.crushftp.tunnel2.Chunk;
import com.crushftp.tunnel2.DVector;
import com.crushftp.tunnel2.Queue;
import com.crushftp.tunnel2.Tunnel2;
import crushftp.db.SearchHandler;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.IdlerKiller;
import crushftp.handlers.Log;
import crushftp.handlers.PreviewWorker;
import crushftp.handlers.SharedSession;
import crushftp.handlers.SharedSessionReplicated;
import crushftp.handlers.UserTools;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import java.util.Locale;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.net.ssl.SSLSocket;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jdom.output.XMLOutputter;

public class ServerSessionHTTP5_2 {
  static Properties proppatches = null;
  
  static Properties locktokens = null;
  
  public int bufferSize = 262144;
  
  byte[] headerBytes = new byte[this.bufferSize];
  
  public ServerSession thisSession = null;
  
  public Thread this_thread = null;
  
  public Socket sock = null;
  
  public Socket reverseSock = null;
  
  public OutputStream original_os = null;
  
  public BufferedInputStream original_is = null;
  
  public boolean keepGoing = true;
  
  SimpleDateFormat sdf_rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  
  int timeoutSeconds = 300;
  
  boolean done = false;
  
  RETR_handler retr = new RETR_handler();
  
  STOR_handler stor = null;
  
  String cacheHeader = "";
  
  boolean writeCookieAuth = false;
  
  Properties server_item;
  
  Vector headers = new Vector();
  
  Properties headerLookup = new Properties();
  
  String proxy = "";
  
  long mimesModified = 0L;
  
  Properties mimes = new Properties();
  
  String hostString = "";
  
  XMLOutputter xmlOut = null;
  
  String userAgent = "";
  
  public boolean chunked = false;
  
  boolean alreadyChunked = false;
  
  long http_len_max = 0L;
  
  ServerSessionAJAX5_2 ssa = null;
  
  ServerSessionDAV5_2 ssd = null;
  
  String CRLF = "\r\n";
  
  String secureCookie = "";
  
  boolean reverseProxyHttps = false;
  
  public static Vector webDavAgents = new Vector();
  
  String direction;
  
  public void run() {
    String disconnectReason = "";
    try {
      this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.server_item.getProperty("ip", "0.0.0.0") + ":" + this.server_item.getProperty("port", "21") + "] " + SG("Accepting connection from") + ": " + this.thisSession.uiSG("user_ip") + ":" + this.sock.getPort() + this.CRLF, "ACCEPT");
      this.thisSession.uiPUT("login_date", (new Date()).toString());
      ServerStatus.thisObj.hold_user_pointer(this.thisSession.user_info);
      this.thisSession.thread_killer_item = new IdlerKiller(this.thisSession, (new Date()).getTime(), 2L, Thread.currentThread());
      this.original_os = this.sock.getOutputStream();
      this.original_is = new BufferedInputStream(this.sock.getInputStream());
      while (this.sock != null && this.sock.isConnected() && !this.done) {
        this.thisSession.uiPUT("user_logged_in", "false");
        this.thisSession.uiPUT("dont_log", "false");
        this.thisSession.uiPUT("CrushAuth", "");
        if (this.thisSession.uVFS != null)
          this.thisSession.uVFS.free(); 
        this.thisSession.uVFS = null;
        Thread.sleep(this.thisSession.delayInterval);
        this.keepGoing = true;
        this.thisSession.shareVFS = true;
        if (this.thisSession.server_item.getProperty("serverType", "FTP").equalsIgnoreCase("HTTPS")) {
          this.secureCookie = "; secure";
        } else {
          this.secureCookie = "";
        } 
        this.secureCookie = String.valueOf(ServerStatus.SG("domain_cookie")) + this.secureCookie;
        handle_http_requests();
      } 
      if (this.sock != null)
        this.sock.close(); 
      if (this.reverseSock != null)
        this.reverseSock.close(); 
    } catch (SocketTimeoutException e) {
      disconnectReason = e.getMessage();
      Log.log("HTTP_SERVER", 3, e);
      this.thisSession.uiPUT("dieing", "true");
    } catch (IOException e) {
      disconnectReason = e.getMessage();
      Log.log("HTTP_SERVER", 2, e);
      this.thisSession.uiPUT("dieing", "true");
    } catch (Exception e) {
      disconnectReason = e.getMessage();
      Log.log("HTTP_SERVER", 0, e);
      this.thisSession.uiPUT("dieing", "true");
    } 
    if (!this.thisSession.uiBG("didDisconnect")) {
      this.thisSession.uiPUT("didDisconnect", "true");
      this.thisSession.do_event5("LOGOUT", null);
    } 
    this.thisSession.add_log("[" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] *" + SG("Disconnected") + ":" + disconnectReason + "*", "DISCONNECTED");
    this.thisSession.uiPUT("dieing", "true");
    do_kill();
  }
  
  public void loginCheckAuthToken(boolean requirePassword) {
    try {
      String user = SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user");
      Properties userProp = (Properties)SharedSession.find("crushftp.usernames").get(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_userProp");
      if (ServerStatus.BG("ignore_web_anonymous") && (user == null || user.equalsIgnoreCase("anonymous")))
        return; 
      this.thisSession.uVFS = (VFS)SharedSession.find("crushftp.usernames").get(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_vfs");
      if (requirePassword && user != null && user.trim().length() > 0 && !this.headerLookup.containsKey("Authorization".toUpperCase()) && !this.headerLookup.containsKey("as2-to".toUpperCase())) {
        this.thisSession.uiPUT("user_name", user);
        this.thisSession.uiPUT("login_date_stamp", this.thisSession.getId());
        if (this.thisSession.uVFS != null)
          this.thisSession.uiPUT("skip_proxy_check", "true"); 
        this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
        Properties session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
        if (session != null && this.thisSession.uVFS != null) {
          this.thisSession.uiPUT("user_logged_in", "true");
          this.thisSession.user = userProp;
          if (session.containsKey("clientid"))
            this.thisSession.uiPUT("clientid", session.getProperty("clientid")); 
          if (session.containsKey("SESSION_RID"))
            this.thisSession.uiPUT("SESSION_RID", session.getProperty("SESSION_RID")); 
          if (this.thisSession.uVFS.connectedVFSItems == null) {
            this.thisSession.uVFS.connectedVFSItems = new Vector();
            this.thisSession.uVFS.clientCacheFree = new Properties();
            this.thisSession.uVFS.clientCacheUsed = new Properties();
            VFS.doCopyVFS(this.thisSession.uVFS, this.thisSession.uVFS);
          } 
          if (!session.getProperty("expire_time", "0").equals("0"))
            if (System.currentTimeMillis() > Long.parseLong(session.getProperty("expire_time"))) {
              session.put("expire_time", "0");
              this.ssa.logout_all();
              this.done = true;
              sendRedirect("/WebInterface/login.html");
              write_command_http("Content-Length: 0");
              write_command_http("");
            }  
        } else {
          this.thisSession.login_user_pass(false, false);
          this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
        } 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
  }
  
  public void give_thread_pointer(Thread this_thread) {
    this.this_thread = this_thread;
    this.thisSession.this_thread = this_thread;
  }
  
  public void do_kill() {
    if (this.stor != null)
      this.stor.die_now = true; 
    addSessionCommand("QUIT", "");
    this.thisSession.do_kill();
  }
  
  public BufferedInputStream getHeaders(BufferedInputStream is) throws Exception {
    long start = System.currentTimeMillis();
    this.sock.setSoTimeout(30000);
    String headerStr = "";
    if (this.alreadyChunked) {
      ((UnChunkInputStream)is).reInitialize();
      ((UnChunkInputStream)is).setChunked(false);
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
        if (headerStr.indexOf("command=getServerItem") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("command=getUser") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("command=getUserXMLListing") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("command=getUserList") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("command=getAdminXMLListing") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("command=getSessionTimeout") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("command=getUserXMLListing") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("command=getAdminXMLListing") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("command=updateNowProgress") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("command=encryptPassword") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } else if (headerStr.indexOf("GET /WebInterface/") >= 0) {
          this.thisSession.uiPUT("dont_log", "true");
        } 
        BufferedReader bsr = new BufferedReader(new StringReader(headerStr));
        String data = "";
        while ((data = bsr.readLine()) != null) {
          data = data.trim();
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
              if (ServerStatus.BG("omnipresence_hack")) {
                if (data.indexOf("%3A") >= 0)
                  data = Common.replace_str(data, "%3A", "/"); 
                if (data.indexOf(":") >= 0)
                  data = Common.replace_str(data, ":", "/"); 
                if (data.indexOf("/%20/") >= 0)
                  data = Common.replace_str(data, "/%20/", "/"); 
                if (data.indexOf("/ /") >= 0)
                  data = Common.replace_str(data, "/ /", "/"); 
              } 
              String[] hack_users = SG("hack_usernames").split(",");
              path = path.substring(0, path.lastIndexOf(" "));
              for (int x = 0; x < hack_users.length; x++) {
                if (hack_users[x].trim().toUpperCase().startsWith("URL:")) {
                  String pattern = hack_users[x].trim().toUpperCase().substring(4).trim();
                  if (Common.do_search(pattern, path.toUpperCase(), false, 0)) {
                    Log.log("HTTP_SERVER", 0, "Banning IP for hack attempt:" + pattern + " vs. " + path);
                    ServerStatus.thisObj.ban(this.thisSession.user_info, ServerStatus.IG("hban_timeout"));
                    ServerStatus.thisObj.kick(this.thisSession.user_info);
                    this.done = true;
                    return null;
                  } 
                } 
              } 
            } catch (Exception exception) {}
          } else {
            data = Common.url_decode(data);
            if (ServerStatus.BG("omnipresence_hack") && data.toUpperCase().startsWith("DESTINATION:")) {
              if (data.indexOf(":", "destination:".length()) >= 0)
                data = String.valueOf(data.substring(0, data.lastIndexOf(":"))) + "/" + data.substring(data.lastIndexOf(":") + 1); 
              if (data.indexOf("/ /") >= 0)
                data = Common.replace_str(data, "/ /", "/"); 
            } 
            if (data.indexOf(":") > 0)
              this.headerLookup.put(data.substring(0, data.indexOf(":")).toUpperCase().trim(), data.substring(data.indexOf(":") + 1).trim()); 
          } 
          this.headers.addElement(data);
        } 
        if (ServerStatus.IG("log_debug_level") >= 4)
          logVector(this.headers); 
        if (this.headers.elementAt(0).toString().indexOf("CrushAuth=") >= 0) {
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
        this.thisSession.uiPUT("user_name", "");
      } 
    } catch (SocketTimeoutException e) {
      Log.log("HTTP_SERVER", 2, e);
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
  
  public void passReverseData(InputStream in, OutputStream out, boolean doChunked) {
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
            this.thisSession.add_log("WROTE " + bytes + " to server.", "PROXY"); 
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
    String headersStr = "";
    for (int x = 0; x < this.headers.size(); x++) {
      String data = this.headers.elementAt(x).toString();
      if (x == 0)
        data = String.valueOf(data.substring(0, data.indexOf(" ") + 1)) + data.substring(data.indexOf(" ") + 1, data.lastIndexOf(" ")).replaceAll(" ", "%20") + data.substring(data.lastIndexOf(" ")); 
      headersStr = String.valueOf(headersStr) + data + "\r\n";
    } 
    try {
      if (this.reverseSock == null)
        this.reverseSock = ServerStatus.thisObj.common_code.getSock(new VRL(url)); 
      BufferedInputStream is = new BufferedInputStream(this.reverseSock.getInputStream());
      OutputStream os = this.reverseSock.getOutputStream();
      os.write(headersStr.getBytes("UTF8"));
      os.write("\r\n".getBytes());
      os.flush();
      this.http_len_max = getContentLength();
      boolean chunked = (this.headerLookup.getProperty("TRANSFER-ENCODING", "").toUpperCase().indexOf("CHUNKED") >= 0);
      if (this.http_len_max > 0L || this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0 || chunked)
        passReverseData(this.original_is, os, chunked); 
      this.headers.removeAllElements();
      this.headerLookup.clear();
      headersStr = "";
      is = getHeaders(is);
      for (int i = 0; i < this.headers.size(); i++) {
        String data = this.headers.elementAt(i).toString();
        if (data.toUpperCase().startsWith("LOCATION:")) {
          String loc = this.headerLookup.getProperty("LOCATION");
          if (loc.startsWith("HTTPS:") && this.server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTP")) {
            this.headerLookup.put("LOCATION", "HTTP" + loc.substring(loc.indexOf(":")));
            data = String.valueOf(data.substring(0, data.indexOf(":") + 2)) + this.headerLookup.getProperty("LOCATION");
          } 
        } 
        if ((data.toUpperCase().startsWith("COOKIE:") || data.toUpperCase().startsWith("SET-COOKIE:")) && this.server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTP"))
          if (data.indexOf("; secure") >= 0)
            data = Common.replace_str(data, "; secure", "");  
        this.headers.setElementAt(data, i);
        logVector(this.headers);
        headersStr = String.valueOf(headersStr) + data + "\r\n";
      } 
      this.http_len_max = getContentLength();
      this.original_os.write(headersStr.getBytes("UTF8"));
      this.original_os.write("\r\n".getBytes());
      this.original_os.flush();
      this.thisSession.add_log(headersStr, "PROXY");
      chunked = (this.headerLookup.getProperty("TRANSFER-ENCODING", "").toUpperCase().indexOf("CHUNKED") >= 0);
      if (this.http_len_max > 0L || this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0 || chunked)
        passReverseData(is, this.original_os, chunked); 
      if (this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0) {
        this.reverseSock.close();
        this.reverseSock = null;
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    if (this.headerLookup.getProperty("CONNECTION", "").toUpperCase().indexOf("CLOSE") >= 0 || this.http_len_max < 0L)
      this.done = true; 
  }
  
  public void logVector(Vector v) {
    synchronized (ServerStatus.thisObj.logDateFormat) {
      for (int x = 0; x < v.size(); x++) {
        String data = v.elementAt(x).toString();
        String data_l = data.toLowerCase();
        if (data_l.startsWith("cache-control".toLowerCase()) || data_l.startsWith("pragma") || data_l.startsWith("accept") || data_l.startsWith("connection") || data_l.startsWith("content-type") || data_l.startsWith("date")) {
          if (ServerStatus.IG("log_debug_level") >= 3)
            this.thisSession.add_log_formatted(data, "POST"); 
        } else if (data_l.startsWith("user-agent") || data.startsWith("host")) {
          if (ServerStatus.IG("log_debug_level") >= 2)
            this.thisSession.add_log_formatted(data, "POST"); 
        } else if (data_l.startsWith("content-length") || data_l.startsWith("cookie")) {
          if (ServerStatus.IG("log_debug_level") >= 1)
            this.thisSession.add_log_formatted(data, "POST"); 
        } else {
          this.thisSession.add_log_formatted(data, "POST");
        } 
      } 
    } 
  }
  
  public void handle_http_requests() throws Exception {
    if (proppatches == null && (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop")).exists())
      try {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop"));
        proppatches = (Properties)ois.readObject();
        ois.close();
      } catch (Exception e) {
        (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches_BAD.prop"));
      }  
    if (proppatches == null)
      proppatches = new Properties(); 
    if (locktokens == null && (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop")).exists())
      try {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop"));
        locktokens = (Properties)ois.readObject();
        ois.close();
      } catch (Exception e) {
        (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens_BAD.prop"));
      }  
    if (locktokens == null)
      locktokens = new Properties(); 
    if (this.ssa == null)
      this.ssa = new ServerSessionAJAX5_2(this.thisSession, this); 
    String header0 = "";
    this.headers.removeAllElements();
    this.headerLookup.clear();
    this.original_is = getHeaders(this.original_is);
    this.thisSession.uiPUT("http_headers", this.headers);
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
      this.thisSession.uiPUT("bind_port", this.headerLookup.getProperty("X-PROXY_BIND_PORT")); 
    if (this.done) {
      logVector(this.headers);
      return;
    } 
    try {
      this.userAgent = this.headerLookup.getProperty("User-Agent".toUpperCase(), "").trim();
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
        this.thisSession.uiPUT("user_ip", tempIp);
      } 
      boolean ipAllowed = ServerStatus.thisObj.common_code.check_ip((Vector)ServerStatus.server_settings.get("ip_restrictions"), this.thisSession.uiSG("user_ip"));
      if (!ipAllowed) {
        this.done = true;
        ServerStatus.thisObj.append_log("!" + (new Date()).toString() + "!  ---" + ServerStatus.SG("BANNED IP CONNECTION TERMINATED") + "---:" + this.thisSession.uiSG("user_ip"), "DENIAL");
        ServerStatus.put_in("failed_logins", ServerStatus.IG("failed_logins") + 1);
        write_command_http("HTTP/1.1 429 Banned");
        write_command_http("Connection: close");
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
          this.thisSession.uVFS = null;
          this.thisSession.uiPUT("CrushAuth", cookies.getProperty("CrushAuth"));
        } 
        String user = SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user");
        if (user != null)
          this.thisSession.uiPUT("user_name", user); 
      } else if (!this.headerLookup.containsKey("Authorization".toUpperCase())) {
        this.writeCookieAuth = true;
        createCookieSession(true);
        setupSession();
      } else {
        setupSession();
      } 
      if (this.server_item.getProperty("port", "0").startsWith("55580") && this.thisSession.uiSG("user_ip").equals("127.0.0.1"))
        this.thisSession.uiPUT("user_ip", SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_ip", this.thisSession.uiSG("user_ip"))); 
      if (this.headers.size() > 0) {
        if (this.thisSession.server_item.getProperty("https_redirect", "false").equalsIgnoreCase("true") && this.thisSession.server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTP")) {
          logVector(this.headers);
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
        header0 = Common.dots(header0);
        if (header0.indexOf("\r") >= 0)
          header0 = "GET / HTTP/1.1"; 
        String request_path = header0.substring(header0.indexOf(" ") + 1, header0.lastIndexOf(" "));
        String reverseProxyUrl = null;
        String reverseProxyPath = null;
        if (!this.server_item.getProperty("reverseProxyUrl", "").equals("")) {
          String[] domains = this.server_item.getProperty("reverseProxyDomain", "*").split(",");
          for (int i = 0; i < domains.length; i++) {
            if (domains[i].equals(""))
              domains[i] = "*"; 
            if (Common.do_search(domains[i].trim(), domain, false, 0)) {
              reverseProxyUrl = this.server_item.getProperty("reverseProxyUrl", "").split(",")[i].trim();
              reverseProxyPath = this.server_item.getProperty("reverseProxyPath", "/").split(",")[i].trim();
              if (reverseProxyPath.equals(""))
                reverseProxyPath = "/"; 
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
          logVector(this.headers);
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
        logVector(this.headers);
        if (processWebInterface) {
          ServerSessionHTTPWI5_2.serveFile(this, this.headers, this.original_os, false, null);
          return;
        } 
        Vector items = new Vector();
        if (header0.toUpperCase().startsWith("POST ") && header0.toUpperCase().indexOf("/CRUSH_STREAMING_HTTP_PROXY") < 0 && header0.indexOf("StartCrushFTPAdminSession.bin") < 0)
          if (!http_boundary.equals("") && !isAS2()) {
            items = parsePostArguments(http_boundary, this.http_len_max, this.thisSession.uiBG("user_logged_in"));
          } else {
            loginCheckHeaderAuth();
            Properties p = new Properties();
            this.ssa.buildPostItem(p, this.http_len_max, this.headers);
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
      String[] tokenStr = this.headers.elementAt(0).toString().substring(header0.indexOf("?") + 1, this.headers.elementAt(0).toString().lastIndexOf(" ")).split("&");
      for (int xx = 0; xx < tokenStr.length; xx++) {
        if (tokenStr[xx].indexOf("=") >= 0) {
          String key = tokenStr[xx].substring(0, tokenStr[xx].indexOf("=")).trim();
          String val = tokenStr[xx].substring(tokenStr[xx].indexOf("=") + 1).trim();
          request.put(Common.url_decode(key), Common.url_decode(val));
          if (!key.equals("path") && !key.equals("w") && !key.equals("u") && !key.equals("p"))
            urlRequestItems.put(key, val); 
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
    if (this.ssa.processItemAnonymous(request, urlRequestItems))
      return; 
    header0 = this.headers.elementAt(0).toString();
    loginCheckAuthToken(requirePassword);
    loginCheckHeaderAuth();
    loginCheckHttpTrustHeaders();
    if (this.thisSession.uVFS != null)
      this.thisSession.uVFS.thisSession = this.thisSession; 
    if (this.thisSession.uiBG("user_logged_in"))
      fixRootDir(null, false); 
    if (header0.toUpperCase().startsWith("OPTIONS ")) {
      if (webDavAgents.indexOf(this.userAgent) < 0)
        webDavAgents.addElement(this.userAgent); 
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
    trackHttpCommand(header0, request);
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
      } else if (webDavAgents.indexOf(this.userAgent) < 0 && (header0.toUpperCase().startsWith("GET ") || header0.toUpperCase().startsWith("POST "))) {
        if (!this.writeCookieAuth && !request.containsKey("username") && !request.containsKey("password"))
          this.thisSession.killSession(); 
        if (header0.indexOf("/WebInterface/") >= 0) {
          write_command_http("HTTP/1.1 404 Not Found");
          String html404 = ServerStatus.SG("web404Text");
          html404 = ServerStatus.thisObj.change_vars_to_values(html404, this.thisSession);
          write_command_http("Content-Length: " + ((html404.getBytes("UTF8")).length + 2));
          write_command_http("");
          write_command_http(html404);
          return;
        } 
        if (!header0.startsWith("GET / ")) {
          sendRedirect("/WebInterface/login.html?path=" + header0.substring(4, header0.lastIndexOf(" ")).trim());
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
        Common.streamCopier(this.sock.getInputStream(), sock2.getOutputStream(), true, true, true);
        Common.streamCopier(sock2.getInputStream(), this.sock.getOutputStream(), false, true, true);
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
      Properties session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
      session.putAll(urlRequestItems);
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
      if (!user_dir.toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
        user_dir = String.valueOf(this.thisSession.SG("root_dir")) + (user_dir.startsWith("/") ? user_dir.substring(1) : user_dir); 
      if (!user_dir.startsWith("/"))
        user_dir = "/" + user_dir; 
      user_dir = Common.dots(user_dir);
      if (user_dir.startsWith(String.valueOf(this.thisSession.SG("root_dir")) + "WebInterface/"))
        user_dir = user_dir.substring(this.thisSession.SG("root_dir").length() - 1); 
      if (!user_dir.startsWith("/WebInterface/function/"))
        this.thisSession.uiPUT("current_dir", user_dir); 
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
        if (this.thisSession.uiSG("current_dir").endsWith("/") && this.thisSession.uiSG("current_dir").toLowerCase().indexOf("crushftp.jnlp") < 0 && !headersOnly) {
          action = "serve dir";
        } else {
          action = "serve file";
          boolean dirOK = false;
          try {
            Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
            if (item != null && item.getProperty("type", "").equals("DIR") && !this.thisSession.uiSG("current_dir").endsWith("/"))
              if (this.headers.toString().toUpperCase().indexOf("DREAMWEAVER") >= 0) {
                this.thisSession.uiPUT("current_dir", String.valueOf(this.thisSession.uiSG("current_dir")) + "/");
                action = "serve dir";
                dirOK = true;
              } else {
                sendRedirect(String.valueOf(this.thisSession.uiSG("current_dir")) + "/");
                write_command_http("Content-Length: 0");
                write_command_http("");
                return;
              }  
            if (!dirOK) {
              boolean ok = (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") && Common.filter_check("U", Common.last(this.thisSession.uiSG("current_dir")), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")));
              if (ok && item != null)
                otherFile = new VRL(item.getProperty("url")); 
              if (otherFile == null && this.thisSession.uiSG("current_dir").toUpperCase().endsWith(".ZIP")) {
                this.thisSession.uiPUT("current_dir", this.thisSession.uiSG("current_dir").substring(0, this.thisSession.uiSG("current_dir").length() - 4));
                ok = (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") && Common.filter_check("U", Common.last(this.thisSession.uiSG("current_dir")), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")));
                item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
                if (ok) {
                  otherFile = new VRL(item.getProperty("url"));
                  otherFile = new VRL(String.valueOf(Common.all_but_last(otherFile.toString())) + otherFile.getName() + ".zip");
                } else {
                  this.thisSession.uiPUT("current_dir", String.valueOf(this.thisSession.uiSG("current_dir")) + ".zip");
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
            otherFile = new VRL((new File(String.valueOf(System.getProperty("crushftp.web")) + theFile)).toURI().toURL().toExternalForm());
          } 
        } 
      } else if (header0.toUpperCase().startsWith("POST ")) {
        action = "process post";
      } else if (header0.toUpperCase().startsWith("LOCK ") || header0.toUpperCase().startsWith("UNLOCK ") || header0.toUpperCase().startsWith("DELETE ") || header0.toUpperCase().startsWith("MKCOL ") || header0.toUpperCase().startsWith("PROPFIND ") || header0.toUpperCase().startsWith("MOVE ") || header0.toUpperCase().startsWith("PROPPATCH ") || header0.toUpperCase().startsWith("DMMKPATH ") || header0.toUpperCase().startsWith("DMMKPATHS ") || header0.toUpperCase().startsWith("ACL ") || header0.toUpperCase().startsWith("COPY ")) {
        if (webDavAgents.indexOf(this.userAgent) < 0)
          webDavAgents.addElement(this.userAgent); 
        action = header0.toLowerCase().substring(0, header0.indexOf(" "));
        boolean ok = (Common.check_protocol("WEBDAV", SG("allowed_protocols")) >= 0);
        if (this.thisSession.server_item.getProperty("allow_webdav", "true").equalsIgnoreCase("false") || !ok)
          action = "serve file"; 
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
          Thread.currentThread().setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " CRUSH_STREAMING_HTTP_PROXY:HTTPReader");
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
                this.thisSession.add_log_formatted("Chunk Command:" + c.getCommand(), "POST"); 
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
              SharedSession.find("crushftp.usernames.activity").put(this.thisSession.getId(), (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
              if (System.currentTimeMillis() - chunkTimer > 10000L) {
                this.thisSession.add_log_formatted("read " + chunkCount + " chunks", "POST");
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
          this.thisSession.add_log_formatted("read total chunks:" + chunkCountTotal, "POST");
        } else {
          Thread.currentThread().setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " CRUSH_STREAMING_HTTP_PROXY:HTTPWriter");
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
              if (c.isCommand() && !c.getCommand().startsWith("A:"))
                this.thisSession.add_log_formatted("Chunk Command:" + c.getCommand(), "POST"); 
              byte[] b = c.toBytes();
              if (doChunked)
                this.original_os.write((String.valueOf(Long.toHexString(b.length)) + "\r\n").getBytes()); 
              this.original_os.write(b);
              if (doChunked)
                this.original_os.write("\r\n".getBytes()); 
              chunkCountTotal++;
              chunkCount++;
              SharedSession.find("crushftp.usernames.activity").put(this.thisSession.getId(), (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
              if (System.currentTimeMillis() - chunkTimer > 10000L) {
                chunkTimer = System.currentTimeMillis();
                this.thisSession.add_log_formatted("wrote " + chunkCount + " chunks", "POST");
                chunkCount = 0;
                if (Tunnel2.getTunnel(this.thisSession.getId()) == null) {
                  this.done = true;
                  break;
                } 
              } 
            } else {
              Thread.sleep(1L);
            } 
            if (t != null && t.getWantClose()) {
              this.done = true;
              break;
            } 
          } 
          if (doChunked)
            this.original_os.write("0\r\n\r\n".getBytes()); 
          this.thisSession.add_log_formatted("wrote total chunks:" + chunkCountTotal, "POST");
          return;
        } 
      } 
      String initial_current_dir = this.thisSession.uiSG("current_dir");
      String error_message = "";
      boolean webDavOK = (Common.check_protocol("WEBDAV", SG("allowed_protocols")) >= 0);
      if (request.getProperty("processFileUpload", "false").equals("true")) {
        if (session.containsKey("blockUploads")) {
          this.thisSession.add_log_formatted("Blocking file upload by user request.", "STOR");
          this.ssa.writeResponse("", true, 500, true, false);
          return;
        } 
        parsePostArguments(http_boundary, this.http_len_max, true);
      } 
      if (!this.ssa.processItems(request, byteRanges))
        if (action.equals("propfind") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV5_2(this.thisSession, this); 
          this.ssd.propfind(this.http_len_max, initial_current_dir, depth);
        } else if (action.equals("proppatch") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV5_2(this.thisSession, this); 
          this.ssd.proppatch(this.http_len_max, initial_current_dir, depth);
        } else if (action.equals("delete") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV5_2(this.thisSession, this); 
          error_message = this.ssd.delete(initial_current_dir, error_message);
        } else if (action.equals("acl") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV5_2(this.thisSession, this); 
          this.ssd.acl(this.http_len_max, initial_current_dir);
        } else if (action.equals("copy") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV5_2(this.thisSession, this); 
          this.ssd.copy(initial_current_dir, move_destination, overwrite);
        } else if (action.equals("mkcol") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV5_2(this.thisSession, this); 
          error_message = this.ssd.mkcol(this.http_len_max, initial_current_dir, error_message);
        } else if (action.equals("lock") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV5_2(this.thisSession, this); 
          this.ssd.lock(this.http_len_max, initial_current_dir, depth);
        } else if (action.equals("unlock") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV5_2(this.thisSession, this); 
          this.ssd.unlock(initial_current_dir);
        } else if (action.equals("move") && webDavOK) {
          if (this.ssd == null)
            this.ssd = new ServerSessionDAV5_2(this.thisSession, this); 
          error_message = this.ssd.move(move_destination, error_message, overwrite);
        } else if (action.equals("put")) {
          Properties cookies = getCookies();
          boolean ok = doPutFile(this.http_len_max, this.done, this.headers, null, user_dir, cookies.getProperty("RandomAccess", "false").equals("true"), start_resume_loc);
          if (ok) {
            write_command_http("HTTP/1.1 201  Created");
            write_command_http("Last-Modified: " + this.sdf_rfc1123.format(new Date()));
            if (this.headerLookup.containsKey("LAST-MODIFIED")) {
              String modified = this.headerLookup.getProperty("LAST-MODIFIED").trim();
              this.thisSession.uiPUT("the_command", "MDTM");
              SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
              this.thisSession.uiPUT("the_command_data", String.valueOf(this.thisSession.uiSG("current_dir")) + " " + sdf.format(this.sdf_rfc1123.parse(modified)));
              error_message = String.valueOf(error_message) + this.thisSession.do_MDTM();
            } 
            this.thisSession.uVFS.reset();
          } else {
            write_command_http("HTTP/1.1 403  Access Denied.");
          } 
          write_standard_headers();
          write_command_http("Content-Length: 0");
          write_command_http("");
        } else if (!action.equals("process post")) {
          if (action.equals("serve dir")) {
            if (this.thisSession.BG("WebServerMode") && (this.thisSession.uiSG("current_dir").equals("/") || this.thisSession.uiSG("current_dir").equals(this.thisSession.SG("root_dir")))) {
              Vector v = new Vector();
              try {
                this.thisSession.uVFS.getListing(v, this.thisSession.uiSG("current_dir"));
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
            String basePath = this.thisSession.uiSG("current_dir").substring(this.thisSession.SG("root_dir").length() - 1);
            if (!basePath.equals("/")) {
              if (webDavAgents.indexOf(this.userAgent) >= 0) {
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
            RandomAccessFile web = new RandomAccessFile(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/jQuery/index.html", "r");
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
      if (this.thisSession.uiSG("current_dir").indexOf("/:filetree") >= 0 && ServerStatus.BG("allow_filetree")) {
        this.retr.data_os = this.original_os;
        this.retr.httpDownload = true;
        String the_dir = Common.all_but_last(this.thisSession.uiSG("current_dir"));
        Properties item = null;
        try {
          item = this.thisSession.uVFS.get_item(the_dir);
        } catch (Exception exception) {}
        if (item != null) {
          otherFile = new VRL(item.getProperty("url"));
          this.retr.init_vars(this.thisSession.uiSG("current_dir"), 0L, 0L, this.thisSession, item, false, "", otherFile);
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
        boolean ok1 = (this.thisSession.check_access_privs(Common.all_but_last(this.thisSession.uiSG("current_dir")), "RETR") && Common.filter_check("U", Common.last(this.thisSession.uiSG("current_dir")), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")));
        boolean ok2 = (this.thisSession.check_access_privs(this.thisSession.uiSG("current_dir"), "RETR") && Common.filter_check("U", Common.last(this.thisSession.uiSG("current_dir")), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")));
        if (ok1 || ok2 || headersOnly) {
          write_command_http("HTTP/1.1 404 Not Found");
        } else {
          write_command_http("HTTP/1.1 403 Access Denied.");
        } 
        String html404 = ServerStatus.SG("web404Text");
        html404 = ServerStatus.thisObj.change_vars_to_values(html404, this.thisSession);
        write_command_http("Content-Length: " + ((html404.getBytes("UTF8")).length + 2));
        write_command_http("");
        write_command_http(html404);
      } 
    } else {
      this.thisSession.uiPUT("the_command", "RETR");
      if (ServerStatus.SG("default_logo").equals("logo.gif"))
        ServerStatus.server_settings.put("default_logo", "logo.png"); 
      updateMimes();
      String ext = "";
      if (otherFile.toString().lastIndexOf(".") >= 0)
        ext = otherFile.toString().substring(otherFile.toString().lastIndexOf(".")).toUpperCase(); 
      if (this.mimes.getProperty(ext, "").equals(""))
        ext = "*"; 
      Properties item = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
      String htmlData = "";
      if ((this.mimes.getProperty(ext, "").toUpperCase().endsWith("/HTML") || this.mimes.getProperty(ext, "").toUpperCase().endsWith("/X-JAVA-JNLP-FILE") || this.mimes.getProperty(ext, "").toUpperCase().endsWith("/JAVASCRIPT") || this.mimes.getProperty(ext, "").toUpperCase().endsWith("/CSS")) && (otherFile.toString().indexOf("/WebInterface/") >= 0 || this.thisSession.uiSG("current_dir").toUpperCase().startsWith("/WEBINTERFACE/") || this.thisSession.BG("WebServerSSI"))) {
        String current_dir = this.thisSession.uiSG("current_dir");
        if (current_dir.indexOf("?") >= 0)
          current_dir = current_dir.substring(0, current_dir.indexOf("?")); 
        if (current_dir.startsWith(this.thisSession.SG("root_dir")))
          current_dir = current_dir.substring(this.thisSession.SG("root_dir").length()); 
        if (!current_dir.startsWith("/"))
          current_dir = "/" + current_dir; 
        if (item == null && current_dir.startsWith("/WebInterface/"))
          if ((new File(String.valueOf(System.getProperty("crushftp.web")) + current_dir)).exists()) {
            RandomAccessFile in = new RandomAccessFile(String.valueOf(System.getProperty("crushftp.web")) + current_dir, "r");
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
          if (!cjs.equals("javascript"))
            htmlData = "$(document).ready(function () {\r\n\r\n" + htmlData + cjs + "\r\n\r\n});"; 
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
        if (this.thisSession.BG("WebServerSSI") && this.mimes.getProperty(ext, "").toUpperCase().endsWith("/HTML")) {
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
        write_command_http("");
        write_command_http(html404);
        return;
      } 
      boolean checkOK = false;
      boolean zipDownload = false;
      Properties stat = new Properties();
      if (item != null) {
        GenericClient c = this.thisSession.uVFS.getClient(item);
        try {
          stat = c.stat(otherFile.getPath());
          if (stat == null && otherFile.getPath().toUpperCase().endsWith(".ZIP")) {
            zipDownload = true;
            stat = c.stat(otherFile.getPath().substring(0, otherFile.getPath().length() - 4));
          } 
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
        String contentType = this.mimes.getProperty(ext, "");
        if (byteRanges.size() <= 1) {
          write_command_http("Content-Type: " + contentType);
          if (contentType.toLowerCase().indexOf("/binary") >= 0)
            write_command_http("Content-Disposition: attachment; filename=\"" + ((this.userAgent.toUpperCase().indexOf("MSIE") >= 0 || this.userAgent.toUpperCase().indexOf("TRIDENT") >= 0) ? Common.url_encode(otherFile.getName()) : otherFile.getName()) + "\""); 
        } else if (byteRanges.size() > 1) {
          write_command_http("Content-Type: multipart/byteranges; boundary=" + byteRangeBoundary);
        } 
        write_command_http("X-UA-Compatible: chrome=1");
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
          Common.startMultiThreadZipper(this.thisSession.uVFS, this.retr, this.thisSession.uiSG("current_dir"), 2000, !item.getProperty("url").toUpperCase().startsWith("FILE:/"), new Vector());
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
              String the_dir = this.thisSession.uiSG("current_dir");
              Properties pp = new Properties();
              pp.put("the_dir", the_dir);
              this.thisSession.runPlugin("transfer_path", pp);
              the_dir = pp.getProperty("the_dir", the_dir);
              this.retr.init_vars(the_dir, Long.parseLong(p.getProperty("start")), Long.parseLong(p.getProperty("end")) + 1L, this.thisSession, item, false, "", otherFile);
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
    if (this.thisSession.uiSG("user_name").equals("") && !ServerStatus.BG("ignore_web_anonymous")) {
      this.thisSession.uiPUT("user_name", "anonymous");
      this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
      createCookieSession(false);
      this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
      if (this.thisSession.uVFS != null)
        this.thisSession.uVFS.free(); 
      this.thisSession.uVFS = null;
      this.thisSession.uiPUT("dont_log", "true");
      if (this.thisSession.uiSG("user_name").equals("anonymous") && !ServerStatus.BG("ignore_web_anonymous_proxy")) {
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
          if (this.userAgent.toUpperCase().indexOf("IPHOTO") >= 0 || this.userAgent.toUpperCase().indexOf("ISNETSERVICES") >= 0 || this.userAgent.toUpperCase().indexOf("ICAL") >= 0 || this.userAgent.toUpperCase().indexOf("CALENDAR") >= 0 || this.userAgent.toUpperCase().indexOf("CFNETWORK") >= 0)
            this.thisSession.uiPUT("user_logged_in", "false"); 
          if (!header0.toUpperCase().startsWith("GET /WEBINTERFACE/") && header0.toUpperCase().startsWith("GET / ")) {
            sendRedirect("/WebInterface/login.html");
            write_command_http("Content-Length: 0");
            write_command_http("");
            return false;
          } 
        } 
        if (!this.thisSession.uiBG("user_logged_in") && (header0.startsWith("PROPFIND") || header0.startsWith("DELETE") || header0.startsWith("PUT") || header0.startsWith("MKCOL") || this.userAgent.toUpperCase().indexOf("ICAL") >= 0 || this.userAgent.toUpperCase().indexOf("CALENDAR") >= 0)) {
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
    if ((!this.thisSession.uiBG("user_logged_in") || this.thisSession.uiSG("user_name").equalsIgnoreCase("anonymous") || this.thisSession.uiSG("user_name").equalsIgnoreCase("")) && (this.headerLookup.containsKey("Authorization".toUpperCase()) || this.headerLookup.containsKey("Proxy-Authorization".toUpperCase()) || this.headerLookup.containsKey("as2-to".toUpperCase()))) {
      String authorization = "";
      if (this.headerLookup.containsKey("Authorization".toUpperCase())) {
        authorization = this.headerLookup.getProperty("Authorization".toUpperCase()).trim();
        authorization = Common.decode64(authorization.substring("Basic".length()).trim());
        if (webDavAgents.indexOf(this.userAgent) < 0)
          webDavAgents.addElement(this.userAgent); 
      } else if (this.headerLookup.containsKey("Proxy-Authorization".toUpperCase())) {
        authorization = this.headerLookup.getProperty("Proxy-Authorization".toUpperCase()).trim();
        authorization = Common.decode64(authorization.substring("Basic".length()).trim());
        if (webDavAgents.indexOf(this.userAgent) < 0)
          webDavAgents.addElement(this.userAgent); 
      } else if (this.headerLookup.containsKey("as2-to".toUpperCase())) {
        authorization = this.headerLookup.getProperty("as2-to".toUpperCase()).trim();
        authorization = Common.replace_str(authorization, "-_-", ":");
        if (authorization.indexOf(":") < 0)
          authorization = String.valueOf(authorization) + ":"; 
        this.headerLookup.put("as2-to".toUpperCase(), authorization);
        this.thisSession.uiPUT("current_password", authorization);
        Log.log("HTTP_SERVER", 0, "Authentication as AS2 user:" + authorization.substring(0, authorization.indexOf(":")));
      } 
      this.thisSession.uiPUT("current_password", authorization.substring(authorization.indexOf(":") + 1));
      this.thisSession.uiPUT("user_name", authorization.substring(0, authorization.indexOf(":")));
      if (ServerStatus.BG("username_uppercase"))
        this.thisSession.uiPUT("user_name", this.thisSession.uiSG("the_command_data").toUpperCase()); 
      if (this.thisSession.uiSG("user_name").indexOf("\\") >= 0)
        this.thisSession.uiPUT("user_name", this.thisSession.uiSG("user_name").substring(this.thisSession.uiSG("user_name").indexOf("\\") + 1)); 
      this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
      this.thisSession.runPlugin("beforeLogin", null);
      this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
      if (this.thisSession.uVFS != null)
        this.thisSession.uVFS.free(); 
      this.thisSession.uVFS = null;
      boolean good = this.thisSession.login_user_pass();
      setupSession();
      if (good) {
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
      } catch (Exception exception) {}
      String certUsername = subject.substring(subject.indexOf("CN=") + 3).trim();
      certUsername = certUsername.substring(0, certUsername.indexOf(",")).trim();
      if (!certUsername.startsWith("NOLOGIN_"))
        if (ServerStatus.BG("client_cert_auth")) {
          requirePassword = false;
          if (!this.thisSession.uiBG("user_logged_in")) {
            this.thisSession.uiPUT("user_name", certUsername.trim());
            this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
            this.thisSession.runPlugin("beforeLogin", null);
            this.thisSession.uiPUT("current_password", "");
            this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
            if (this.thisSession.uVFS != null)
              this.thisSession.uVFS.free(); 
            this.thisSession.uVFS = null;
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
    try {
      Vector httpTrustHeaderVariables = new Vector();
      Vector httpTrustHeaderValues = new Vector();
      if (this.server_item.getProperty("httpTrustHeaderVariables", "").equals("false"))
        this.server_item.put("httpTrustHeaderVariables", ""); 
      String[] trustHeadersStr = this.server_item.getProperty("httpTrustHeaderVariables", "").split(",");
      for (int x = 0; x < trustHeadersStr.length; x++) {
        if (!trustHeadersStr[x].trim().equals("")) {
          httpTrustHeaderVariables.addElement(trustHeadersStr[x].split("=")[0].toUpperCase().trim());
          httpTrustHeaderValues.addElement(trustHeadersStr[x].split("=")[1].toUpperCase().trim());
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
        this.this_thread.setName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (control)");
        if (this.thisSession.uVFS != null)
          this.thisSession.uVFS.free(); 
        this.thisSession.uVFS = null;
        boolean good = this.thisSession.login_user_pass(true);
        if (good) {
          keys = trustHeaderVals.keys();
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            String val = trustHeaderVals.getProperty(key, "");
            this.thisSession.user.put(key, val);
          } 
          setupSession();
          createCookieSession(false);
          this.thisSession.do_event5("LOGIN", null);
        } 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
  }
  
  public void trackHttpCommand(String header0, Properties request) {
    this.thisSession.runPlugin("command", null);
    try {
      Properties p = new Properties();
      String cmd = header0.substring(0, header0.indexOf(" "));
      String cmdVal = header0.substring(header0.indexOf(" ") + 1, header0.lastIndexOf(" "));
      if (header0.indexOf("/function/") > 0) {
        cmd = request.getProperty("command", request.getProperty("the_action", "unknown"));
        Properties request2 = (Properties)request.clone();
        request2.remove("random");
        cmdVal = Common.url_decode(request2.toString());
      } 
      p.put("the_command", cmd);
      p.put("the_command_data", cmdVal);
      p.put("user_time", ServerStatus.thisObj.logDateFormat.format(new Date()));
      p.put("display", String.valueOf(p.getProperty("user_time")) + " | " + this.thisSession.uiSG("the_command") + " " + p.getProperty("the_command_data", ""));
      p.put("stamp", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      this.thisSession.uiVG("session_commands").addElement(p);
      if (cmd.equals("password") && cmdVal.length() > 0)
        cmdVal = "-----------"; 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
  }
  
  public OutputStream getStorOutputStream(String user_dir, long start_resume_loc, boolean random_access) throws Exception {
    if (this.stor == null)
      this.stor = new STOR_handler(); 
    Socket local_s = Common.getSTORSocket(this.thisSession, this.stor, "", true, user_dir, random_access, start_resume_loc);
    return local_s.getOutputStream();
  }
  
  public boolean doPutFile(long content_length, boolean connectionClose, Vector headers, OutputStream of_stream, String user_dir, boolean random_access, long start_resume_loc) throws Exception {
    boolean ok = false;
    if (this.thisSession.check_access_privs(user_dir, "STOR") && Common.filter_check("U", Common.last(user_dir), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")))
      ok = true; 
    if (ok) {
      user_dir = user_dir.replace(':', '_');
      this.thisSession.uiPUT("current_dir", user_dir);
      Properties item = this.thisSession.uVFS.get_item_parent(Common.all_but_last(user_dir));
      if (item == null)
        this.thisSession.do_MKD(true, user_dir); 
      if (item == null)
        item = this.thisSession.uVFS.get_item_parent(user_dir); 
      if (of_stream == null)
        of_stream = getStorOutputStream(user_dir, start_resume_loc, random_access); 
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
            if (bytes_read > 0) {
              content_length -= bytes_read;
              of_stream.write(b, 0, bytes_read);
            } 
          } 
          of_stream.flush();
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
      } 
      try {
        of_stream.close();
      } catch (Exception exception) {}
      if (this.stor != null && this.stor.active)
        try {
          while (this.stor.active)
            Thread.sleep(100L); 
        } catch (Exception exception) {} 
    } else {
      get_raw_http_command((int)content_length);
    } 
    if (this.stor != null && this.stor.inError)
      ok = false; 
    this.thisSession.uVFS.reset();
    if (ok) {
      Properties item = this.thisSession.uVFS.get_item(user_dir);
      if (item != null)
        this.thisSession.accessExceptions.put(user_dir, item); 
    } 
    return ok;
  }
  
  public Vector parsePostArguments(String boundary, long max_len, boolean allow_file) throws Exception {
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
    Properties session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
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
            logVector(logData);
            logData.clear();
            this.thisSession.uiPUT("the_command", "STOR");
            lastUploadName = name;
            fileUploaded = true;
            if (Common.System2.get("crushftp.activeUpload.info" + this.thisSession.getId()) == null)
              Common.System2.put("crushftp.activeUpload.info" + this.thisSession.getId(), new Properties()); 
            activeUpload = (Properties)Common.System2.get("crushftp.activeUpload.info" + this.thisSession.getId());
            item.put("type", "file");
            String upload_item = data.substring(data.indexOf("filename=\"") + 10, data.indexOf("\"", data.indexOf("filename=\"") + 10));
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
            for (int x = 0; x < ServerStatus.SG("unsafe_filename_chars").length(); x++)
              upload_item = upload_item.replace(ServerStatus.SG("unsafe_filename_chars").charAt(x), '_'); 
            p.put("filename", upload_item);
            data = get_http_command();
            len += (data.length() + 2);
            data = data.trim();
            data = Common.url_decode(data);
            p.put("encoding", data);
            get_http_command();
            len += 2L;
            boolean ok = false;
            if (!globalItems.getProperty("uploadPath", "").equals("")) {
              this.thisSession.setupCurrentDir(globalItems.getProperty("uploadPath", ""));
              this.thisSession.uiPUT("the_command_data", "");
              this.thisSession.do_MKD(true, this.thisSession.uiSG("current_dir"));
            } 
            this.thisSession.uiPUT("start_resume_loc", globalItems.getProperty("start_resume_loc", "0"));
            Properties dir_item = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.uiSG("current_dir")) + upload_item);
            if (this.thisSession.check_access_privs(String.valueOf(this.thisSession.uiSG("current_dir")) + upload_item, "STOR") && Common.filter_check("U", Common.last(String.valueOf(this.thisSession.uiSG("current_dir")) + upload_item), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")) && (dir_item == null || dir_item.getProperty("type").equalsIgnoreCase("file"))) {
              ok = true;
            } else if (this.stor != null) {
              String msg = LOC.G("Access denied. (You do not have permission or the file extension is not allowed.)");
              this.stor.stop_message = msg;
              activeUpload.put(lastUploadName, String.valueOf(LOC.G("ERROR")) + ": " + msg);
            } else {
              activeUpload.put(lastUploadName, String.valueOf(LOC.G("ERROR")) + ": " + LOC.G("Access denied. (You do not have permission or the file extension is not allowed.)"));
            } 
            if (upload_item.equals(""))
              ok = false; 
            if (ok) {
              activeUpload.put(lastUploadName, "PROGRESS:" + len + "/" + speed_cheat_len + ";" + upload_item);
              if (max_len > 0L)
                this.thisSession.uiPPUT("file_length", max_len - len); 
              if (this.stor == null)
                this.stor = new STOR_handler(); 
              Socket local_s = Common.getSTORSocket(this.thisSession, this.stor, upload_item, true, this.thisSession.uiSG("current_dir"), globalItems.getProperty("randomaccess", "false").equals("true"), Long.parseLong(globalItems.getProperty("start_resume_loc", "0")));
              OutputStream of_stream = local_s.getOutputStream();
              while (!this.stor.active)
                Thread.sleep(1L); 
              this.stor.metaInfo = metaInfo;
              if (globalItems.containsKey("Last-Modified") || this.headerLookup.containsKey("LAST-MODIFIED")) {
                this.headerLookup.put("LAST_MODIFIED", globalItems.getProperty("Last-Modified", this.headerLookup.getProperty("LAST-MODIFIED")));
                this.stor.fileModifiedDate = this.sdf_rfc1123.parse(this.headerLookup.getProperty("LAST-MODIFIED").trim()).getTime();
              } 
              try {
                byte[] buffer = new byte[this.bufferSize * 2];
                byte[] boundaryBytes = ("\r\n--" + boundary).getBytes();
                int len1 = 0;
                int len2 = 0;
                byte[] b = new byte[this.bufferSize];
                int bytes_read = 0;
                this.original_is.mark(0);
                while ((speedCheat || findSeparator(boundaryBytes, buffer, len1, len2) < 0) && bytes_read >= 0) {
                  if (session.containsKey("blockUploads"))
                    throw new Exception("Upload failed: User Cancelled"); 
                  activeUpload.put(lastUploadName, "PROGRESS:" + len + "/" + speed_cheat_len + ";");
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
                  if (!this.stor.active && this.stor.inError)
                    throw new Exception("Upload failed:" + this.stor.stop_message); 
                  if (session.containsKey("blockUploads"))
                    throw new Exception("Upload failed: User Cancelled"); 
                } 
                if (bytes_read < 0) {
                  this.stor.inError = true;
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
                this.stor.inError = true;
                this.done = true;
                len = max_len;
                this.stor.die_now = true;
                Properties errorItem = new Properties();
                items.addElement(errorItem);
                errorItem.put("responseHeader", "HTTP/1.1 200 " + this.stor.stop_message);
              } 
              try {
                of_stream.close();
              } catch (Exception exception) {}
              while (this.stor.active)
                Thread.sleep(100L); 
              try {
                this.stor.c.close();
              } catch (Exception exception) {}
              local_s.close();
              try {
                Properties newItem1 = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.uiSG("current_dir")) + upload_item);
                if (newItem1 != null) {
                  String previewPath = Common.all_but_last(SearchHandler.getPreviewPath(newItem1, "1", 1)).trim();
                  if (globalItems.getProperty("the_action2", "").equals("changeIcon")) {
                    Properties newItem2 = this.thisSession.uVFS.get_item(String.valueOf(this.thisSession.uiSG("current_dir")) + Common.last(globalItems.getProperty("changeIconItem")));
                    VRL v1 = new VRL(newItem1.getProperty("url"));
                    VRL v2 = new VRL(newItem2.getProperty("url"));
                    for (int i = 0; i < ServerStatus.thisObj.previewWorkers.size(); i++) {
                      PreviewWorker preview = ServerStatus.thisObj.previewWorkers.elementAt(i);
                      if (preview.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true") && preview.checkExtension(Common.last(v2.toString()), new File(v2.getPath())))
                        preview.doConvert(new File(v1.getPath()), new File(v2.getPath()), false, new Properties()); 
                    } 
                    this.thisSession.uiPUT("current_dir", String.valueOf(this.thisSession.uiSG("current_dir")) + upload_item);
                    this.thisSession.uiPUT("the_command", "DELE");
                    this.thisSession.uiPUT("the_command_data", this.thisSession.uiSG("current_dir"));
                    this.thisSession.do_DELE(false, this.thisSession.uiSG("current_dir"));
                    (new File(v1.getPath())).delete();
                  } else if (!previewPath.equals("") && !previewPath.equals("/") && !previewPath.equals(".") && !previewPath.equals("./")) {
                    previewPath = String.valueOf(ServerStatus.SG("previews_path")) + previewPath;
                    if ((new File(previewPath)).exists())
                      Common.recurseDelete(previewPath, false); 
                  } 
                } 
              } catch (Exception e) {
                Log.log("HTTP_SERVER", 2, e);
              } 
              activeUpload.put(lastUploadName, "PROGRESS:" + len + "/" + speed_cheat_len + ";");
              if (this.thisSession.uiPG("lastUploadStat") != null)
                ServerStatus.thisObj.statTools.insertMetaInfo(this.thisSession.uiSG("SESSION_RID"), metaInfo, this.thisSession.uiPG("lastUploadStat").getProperty("TRANSFER_RID")); 
            } else {
              this.done = true;
            } 
            if (this.stor != null && this.stor.stop_message.length() > 0)
              activeUpload.put(lastUploadName, String.valueOf(LOC.G("ERROR")) + ":" + this.stor.stop_message); 
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
            } 
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
      logVector(logData);
      logData.clear();
    } 
    if (activeUpload != null && !activeUpload.getProperty(lastUploadName, "").startsWith("ERROR:"))
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
    if (fileUploaded && this.thisSession.uiSG("current_dir").toUpperCase().indexOf("/IMPULSEUPLOAD/") >= 0) {
      GenericClient c = this.thisSession.uVFS.getClient(this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir")));
      try {
        c.doCommand("SITE IMPULSE " + metaString);
      } finally {
        c = this.thisSession.uVFS.releaseClient(c);
      } 
    } 
    return items;
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
          this.original_is.close();
        } catch (Exception exception) {}
        return "";
      } 
      totalBytesRead++;
      bout.write(aByte, 0, bytesRead);
      eol[0] = eol[1];
      eol[1] = aByte[0];
    } 
    this.thisSession.thread_killer_item.last_activity = (new Date()).getTime();
    return new String(bout.toByteArray(), "UTF8");
  }
  
  public ServerSessionHTTP5_2(Socket sock, int user_number, String user_ip, int listen_port, String listen_ip, String listen_ip_port, Properties server_item) {
    this.direction = "";
    this.sdf_rfc1123.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT")));
    this.sock = sock;
    try {
      sock.setSoTimeout(this.timeoutSeconds * 1000);
    } catch (SocketException socketException) {}
    this.server_item = server_item;
    this.thisSession = new ServerSession(sock, user_number, user_ip, listen_port, listen_ip, listen_ip_port, server_item);
    this.thisSession.give_thread_pointer(Thread.currentThread());
    this.thisSession.uiPUT("dont_read", "true");
    this.thisSession.uiPUT("dont_write", "true");
    this.proxy = this.thisSession.server_item.getProperty("httpReverseProxy", "");
    if (this.proxy.startsWith("!")) {
      this.proxy = this.proxy.substring(1);
      this.reverseProxyHttps = true;
    } 
    if (!this.proxy.endsWith("/"))
      this.proxy = String.valueOf(this.proxy) + "/"; 
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
    this.thisSession.thread_killer_item.last_activity = (new Date()).getTime();
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
      String data_l = data.toLowerCase();
      if (data_l.startsWith("cache-control") || data_l.startsWith("content-type") || data_l.startsWith("date") || data_l.startsWith("server") || data_l.startsWith("p3p") || data_l.startsWith("keep-alive") || data_l.startsWith("connection") || data_l.startsWith("content-type")) {
        if (ServerStatus.IG("log_debug_level") >= 3)
          this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data.trim() + "*", "POST"); 
      } else if (data_l.startsWith("X-PROXY_USER_PORT".toLowerCase()) || data_l.startsWith("X-PROXY_BIND_IP".toLowerCase()) || data_l.startsWith("Access-Control-Allow-Origin".toLowerCase()) || data_l.startsWith("Access-Control-Allow-Headers".toLowerCase()) || data_l.startsWith("Access-Control-Allow-Methods".toLowerCase()) || data_l.startsWith("X-UA-Compatible".toLowerCase()) || data_l.startsWith("Last-Modified".toLowerCase()) || data_l.startsWith("etag") || data_l.startsWith("accept-ranges")) {
        if (ServerStatus.IG("log_debug_level") >= 2)
          this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data.trim() + "*", "POST"); 
      } else if (data_l.startsWith("content-length") || data_l.trim().equals("") || data_l.startsWith("<?xml ")) {
        if (ServerStatus.IG("log_debug_level") >= 1)
          this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data.trim() + "*", "POST"); 
      } else if (data_l.indexOf("<listing>") >= 0) {
        this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data.trim() + "*", "DIR_LIST");
      } else {
        this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data.trim() + "*", "POST");
      } 
    } 
    data = String.valueOf(data) + this.CRLF;
    write_command_raw(data);
    if (this.writeCookieAuth) {
      Log.log("HTTP_SERVER", 3, "Setting up cookie for this session:" + this.thisSession.uiSG("user_name") + ":" + Thread.currentThread().getName());
      Log.log("HTTP_SERVER", 4, new Exception("Who called us?"));
      setupSession();
      String data2 = "Set-Cookie: mainServerInstance=; path=/" + this.CRLF;
      data2 = String.valueOf(data2) + "Set-Cookie: CrushAuth=" + this.thisSession.getId() + "; path=/" + this.secureCookie + this.CRLF;
      write_command_raw(data2);
      this.thisSession.add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.thisSession.uiSG("user_number") + ":" + this.thisSession.uiSG("user_name") + ":" + this.thisSession.uiSG("user_ip") + "] WROTE: *" + data2.trim() + "*", "POST");
      this.thisSession.uiPUT("login_date_stamp", this.thisSession.getId());
      this.writeCookieAuth = false;
    } 
    return data.length();
  }
  
  public void createCookieSession(boolean forceNew) {
    if (forceNew) {
      this.thisSession.killSession();
      this.thisSession.uiPUT("user_name", "anonymous");
      this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name"));
      this.thisSession.uiPUT("CrushAuth", String.valueOf((new Date()).getTime()) + "_" + Common.makeBoundary(30));
    } 
    if (this.thisSession.user_info.getProperty("user_name_original", "").equals("") || this.thisSession.user_info.getProperty("user_name_original", "").equalsIgnoreCase("anonymous"))
      this.thisSession.uiPUT("user_name_original", this.thisSession.uiSG("user_name")); 
    SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user", this.thisSession.uiSG("user_name_original"));
    SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_userProp", this.thisSession.user);
    if (this.thisSession.uVFS != null)
      SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_vfs", this.thisSession.uVFS); 
    SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_user", SharedSession.find("crushftp.usernames").getProperty(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_user"));
    SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_userProp", SharedSession.find("crushftp.usernames").get(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_userProp"));
    SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_vfs", SharedSession.find("crushftp.usernames").get(String.valueOf(Common.getPartialIp(this.thisSession.uiSG("user_ip"))) + "_" + this.thisSession.getId() + "_vfs"));
    SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + this.thisSession.getId() + "_ip", this.thisSession.uiSG("user_ip"));
  }
  
  public void setupSession() {
    Properties session = null;
    synchronized (SharedSession.sessionLock) {
      session = (Properties)SharedSession.find("crushftp.sessions").get(this.thisSession.getId());
      if (session == null) {
        session = new Properties();
        SharedSession.find("crushftp.sessions").put(this.thisSession.getId(), session);
      } 
      if (this.thisSession.uVFS != null)
        if (session.get("sharedVFS") == null) {
          session.put("sharedVFS", this.thisSession.uVFS);
          Log.log("HTTP_SERVER", 2, "setupSession:VFS for " + this.thisSession.getId() + " = " + this.thisSession.uVFS.toString());
        } else {
          VFS sharedVFS = (VFS)session.get("sharedVFS");
          VFS.doCopyVFS(sharedVFS, this.thisSession.uVFS);
        }  
      session.put("created", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      if (session.get("lastUploadStats") == null)
        session.put("lastUploadStats", new Vector()); 
      Vector v = (Vector)session.get("lastUploadStats");
      this.thisSession.uiPUT("lastUploadStats", v);
    } 
    SharedSessionReplicated.flushWait();
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
    write_command_http("P3P: CP=\"IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT\"", log, true);
    if (!ServerStatus.SG("Access-Control-Allow-Origin").equals(""))
      for (int x = 0; x < (ServerStatus.SG("Access-Control-Allow-Origin").split(",")).length; x++) {
        write_command_http("Access-Control-Allow-Origin: " + ServerStatus.SG("Access-Control-Allow-Origin").split(",")[x].trim());
        write_command_http("Access-Control-Allow-Headers: authorization,content-type");
        write_command_http("Access-Control-Allow-Methods: GET,POST,OPTIONS,PUT,PROPFIND,DELETE,MKCOL,MOVE,COPY,HEAD,PROPPATCH,LOCK,UNLOCK,ACL,TR");
      }  
    if (this.done) {
      write_command_http("Connection: close", log, true);
    } else {
      write_command_http("Keep-Alive: timeout=15, max=20", log, true);
      write_command_http("Connection: Keep-Alive", log, true);
    } 
  }
  
  public void addSessionCommand(String the_command, String the_command_data) {
    Properties pp = new Properties();
    pp.put("the_command", the_command);
    pp.put("the_command_data", the_command_data);
    pp.put("user_time", ServerStatus.thisObj.logDateFormat.format(new Date()));
    pp.put("display", String.valueOf(pp.getProperty("user_time")) + " | " + the_command + " " + the_command_data);
    pp.put("stamp", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    this.thisSession.uiVG("session_commands").addElement(pp);
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
    String baseURL = getBaseUrl(this.hostString);
    if (path.toUpperCase().startsWith("HTTP")) {
      write_command_http("location: " + path);
    } else {
      if (path.startsWith("/") && baseURL.endsWith("/"))
        path = path.substring(1); 
      write_command_http("location: " + baseURL + path);
    } 
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
    for (int x = 0; x < server_list.size(); x++) {
      Properties p = server_list.elementAt(x);
      if (p.getProperty("serverType", "FTP").equalsIgnoreCase("HTTPS")) {
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
  }
  
  public void updateMimes() throws Exception {
    long mimesModifiedNew = (new File(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/mime_types.txt")).lastModified();
    if (this.mimesModified != mimesModifiedNew) {
      this.mimesModified = mimesModifiedNew;
      this.mimes = new Properties();
      BufferedReader mimeIn = new BufferedReader(new FileReader(new File(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/mime_types.txt")));
      String s = mimeIn.readLine();
      while (s != null) {
        if (!s.startsWith("#"))
          try {
            this.mimes.put(s.substring(0, s.indexOf(" ")).trim().toUpperCase(), s.substring(s.indexOf(" ") + 1).trim());
          } catch (Exception exception) {} 
        s = mimeIn.readLine();
      } 
      mimeIn.close();
    } 
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
              SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm aa");
              try {
                if (!p.getProperty("expire", "").equals(""))
                  if (sdf.parse(p.getProperty("expire", "")).getTime() < (new Date()).getTime())
                    expired = true;  
              } catch (Exception e) {
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
              this.thisSession.uVFS = null;
              break;
            } 
          } 
        }  
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
  }
  
  public void savePropPatches() throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop.save"));
    oos.writeObject(proppatches);
    oos.close();
    (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop")).delete();
    (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop.save")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/proppatches.prop"));
  }
  
  public void saveLockTokens() throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop.save"));
    oos.writeObject(locktokens);
    oos.close();
    (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop")).delete();
    (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop.save")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/locktokens.prop"));
  }
}
