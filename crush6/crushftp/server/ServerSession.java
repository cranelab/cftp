package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import crushftp.db.SearchHandler;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.IdlerKiller;
import crushftp.handlers.Log;
import crushftp.handlers.SharedSession;
import crushftp.handlers.SharedSessionReplicated;
import crushftp.handlers.UserTools;
import crushftp.user.XMLUsers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ServerSession implements Runnable, Serializable {
  static final long serialVersionUID = 0L;
  
  public Properties user = null;
  
  public transient Socket sock = null;
  
  public transient Socket sockOriginal = null;
  
  public Properties user_info = new Properties();
  
  public boolean not_done = true;
  
  public transient Vector data_socks = new Vector();
  
  public transient Vector pasv_socks = new Vector();
  
  public transient Vector old_data_socks = new Vector();
  
  public transient ServerSocket pasv_sock3 = null;
  
  public transient BufferedReader is = null;
  
  public transient OutputStream os = null;
  
  public VFS uVFS = null;
  
  public VFS expired_uVFS = null;
  
  public Properties rnfr_file = null;
  
  public String rnfr_file_path = null;
  
  String CRLF = "\r\n";
  
  public transient RETR_handler retr_files = null;
  
  public transient STOR_handler stor_files = null;
  
  public transient LIST_handler list_files = null;
  
  public Vector stor_files_pool_free = new Vector();
  
  public Vector retr_files_pool_free = new Vector();
  
  public Vector stor_files_pool_used = new Vector();
  
  public Vector retr_files_pool_used = new Vector();
  
  public SimpleDateFormat date_time = new SimpleDateFormat("MM/dd/yy");
  
  public transient IdlerKiller thread_killer_item = null;
  
  public transient Properties thread_killer_items = new Properties();
  
  public transient Thread this_thread = null;
  
  public Properties server_item = null;
  
  public Properties accessExceptions = new Properties();
  
  Properties quotaDelta = new Properties();
  
  public int delayInterval = 0;
  
  int current_pasv_port = 0;
  
  SimpleDateFormat hh = new SimpleDateFormat("HH");
  
  SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss");
  
  SimpleDateFormat sdf_yyyyMMddHHmmssGMT = new SimpleDateFormat("yyyyMMddHHmmss");
  
  public void give_thread_pointer(Thread this_thread) {
    this.this_thread = this_thread;
  }
  
  public int uiIG(String data) {
    try {
      return Integer.parseInt(uiSG(data));
    } catch (Exception exception) {
      return 0;
    } 
  }
  
  public long uiLG(String data) {
    try {
      return Long.parseLong(uiSG(data));
    } catch (Exception exception) {
      return 0L;
    } 
  }
  
  public boolean uiBG(String data) {
    return uiSG(data).toLowerCase().equals("true");
  }
  
  public String uiSG(String data) {
    if (this.user_info.containsKey(data))
      return this.user_info.getProperty(data); 
    return "";
  }
  
  public void uiPUT(String key, Object val) {
    this.user_info.put(key, val);
  }
  
  public void uiPUT(String key, boolean val) {
    uiPUT(key, (new StringBuffer(String.valueOf(val))).toString());
  }
  
  public void uiPPUT(String key, long val) {
    uiPUT(key, (new StringBuffer(String.valueOf(uiLG(key) + val))).toString());
  }
  
  public Vector uiVG(String key) {
    return (Vector)this.user_info.get(key);
  }
  
  public Properties uiPG(String key) {
    return (Properties)this.user_info.get(key);
  }
  
  public void run() {
    this.this_thread = Thread.currentThread();
    try {
      this.thread_killer_item = (IdlerKiller)this.thread_killer_items.get(this.sock.toString());
      if (this.thread_killer_item == null)
        this.thread_killer_item = new IdlerKiller(this, (new Date()).getTime(), Integer.parseInt(System.getProperty("crushftp.max_auth_time", "2")), Thread.currentThread()); 
      this.thread_killer_items.put(this.sock.toString(), this.thread_killer_item);
      Worker.startWorker(this.thread_killer_item, String.valueOf(uiSG("user_name")) + ":(" + uiSG("user_number") + ")-" + uiSG("user_ip") + " (idle_time)");
      ServerStatus.thisObj.hold_user_pointer(this.user_info);
      this.is = new BufferedReader(new InputStreamReader(this.sock.getInputStream()));
      this.os = this.sock.getOutputStream();
      add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + this.server_item.getProperty("ip", "0.0.0.0") + ":" + this.server_item.getProperty("port", "21") + "][" + uiSG("user_number") + "] " + LOC.G("Accepting connection from") + ": " + uiSG("user_ip") + ":" + this.sock.getPort() + this.CRLF, "ACCEPT");
      String wMsg = this.server_item.getProperty("ftp_welcome_message", ServerStatus.SG("ftp_welcome_message")).trim();
      wMsg = Common.replace_str(wMsg, "\\r", "\r");
      wMsg = Common.replace_str(wMsg, "\\n", "\n");
      String sMsg = ServerStatus.SG("server_start_message").trim();
      sMsg = Common.replace_str(sMsg, "\\r", this.CRLF);
      sMsg = Common.replace_str(sMsg, "\\n", "");
      write_command("220", String.valueOf(wMsg) + this.CRLF + sMsg);
      while (this.not_done && !uiBG("friendly_quit")) {
        Thread.sleep(this.delayInterval);
        if (get_command() < 0)
          this.not_done = false; 
        if (uiBG("refresh_user")) {
          uiPUT("refresh_user", "false");
          if (!verify_user(uiSG("user_name"), uiSG("current_password"))) {
            this.not_done = false;
            break;
          } 
          setupRootDir(null, true);
        } 
        while (uiBG("pause_now"))
          Thread.sleep(1000L); 
        runPlugin("command", null);
        if (uiSG("the_command").equals("USER") && uiSG("the_command_data").length() > 0) {
          if (ServerStatus.BG("username_uppercase"))
            uiPUT("the_command_data", uiSG("the_command_data").toUpperCase()); 
          if (uiSG("the_command_data").indexOf("/") >= 0)
            uiPUT("the_command_data", uiSG("the_command_data").substring(uiSG("the_command_data").indexOf("/") + 1)); 
          boolean require_encryptionUser = false;
          try {
            long time = Long.parseLong(ServerStatus.siPG("invalid_usernames").getProperty(uiSG("the_command_data").toUpperCase(), "0"));
            if (time <= 0L || time <= (new Date()).getTime() - (ServerStatus.IG("invalid_usernames_seconds") * 1000))
              require_encryptionUser = UserTools.ut.getUser(uiSG("listen_ip_port"), uiSG("the_command_data"), false).getProperty("require_encryption", "false").toUpperCase().equals("TRUE"); 
          } catch (Exception exception) {}
          if (uiBG("user_logged_in")) {
            this.not_done = write_command("503", LOC.G("%already_logged_in%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
          } else if ((uiBG("require_encryption") || require_encryptionUser) && !uiBG("secure")) {
            this.not_done = write_command("550", LOC.G("This server requires encryption.") + this.CRLF + LOC.G("You must issue the AUTH command to change to an encrypted session before you can attempt to login."));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
          } else {
            uiPUT("user_name", uiSG("the_command_data"));
            if (uiSG("the_command_data").toUpperCase().equals("ANONYMOUS")) {
              this.not_done = write_command("331", LOC.G("Enter e-mail for password.") + this.CRLF + LOC.G("%USER%"));
            } else {
              this.not_done = write_command("331", LOC.G("%USER%"));
            } 
            runPlugin("beforeLogin", null);
            uiPUT("id", String.valueOf(uiSG("user_number")) + "-" + uiSG("user_name"));
          } 
        } else if (uiSG("the_command").equals("PASS")) {
          String originalUsername = uiSG("user_name");
          uiPUT("current_password", uiSG("the_command_data"));
          boolean needClientAuth = !(!ServerStatus.BG("needClientAuth") && !this.server_item.getProperty("needClientAuth", "").equals("true") && (this.user == null || !this.user.getProperty("needClientAuth", "false").equals("true")));
          if (needClientAuth && uiBG("secure")) {
            String subject = "";
            try {
              subject = ((SSLSocket)this.sock).getSession().getPeerCertificateChain()[0].getSubjectDN().toString();
              add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("WROTE") + ": *" + LOC.G("Found subject name in client certificate: $0", subject.trim()) + "*", "USER");
              if (subject.toUpperCase().indexOf("CN=") >= 0)
                subject = subject.substring(subject.toUpperCase().indexOf("CN=")); 
              add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("WROTE") + ": *" + LOC.G("Found subject name in client certificate: $0", subject.trim()) + "*", "USER");
            } catch (Exception exception) {}
            String certUsername = subject.substring(subject.indexOf("=") + 1, subject.indexOf(", "));
            uiPUT("user_name", certUsername.trim());
            uiPUT("current_password", "");
            add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("WROTE") + ": *" + LOC.G("Attempting login based on client certificate username: $0", certUsername.trim()) + "*", "USER");
          } 
          boolean good = false;
          if (uiSG("user_name").startsWith("NOLOGIN_") && needClientAuth && uiBG("secure")) {
            good = false;
          } else {
            good = login_user_pass((needClientAuth && uiBG("secure")), true);
          } 
          if (!good && needClientAuth && uiBG("secure")) {
            uiPUT("user_name", originalUsername);
            uiPUT("current_password", uiSG("the_command_data"));
            good = login_user_pass(false, true);
          } 
          this.this_thread.setName(String.valueOf(uiSG("user_name")) + ":(" + uiSG("user_number") + ")-" + uiSG("user_ip") + " (control)");
          if (good) {
            stop_idle_timer();
            start_idle_timer();
            do_event5("LOGIN", null);
          } 
        } else if (uiSG("the_command").equals("AUTH") && uiSG("the_command_data").length() > 0) {
          try {
            uiPUT("secureType", uiSG("the_command_data").toUpperCase().trim());
            if (uiBG("explicit_ssl") && uiSG("secureType").indexOf("SSL") >= 0) {
              uiPUT("secureType", "SSL");
            } else if (uiBG("explicit_tls") && uiSG("secureType").indexOf("TLS") >= 0) {
              uiPUT("secureType", "TLS");
            } else {
              throw new Exception(LOC.G("Invalid Encryption type") + " : " + uiSG("secureType"));
            } 
            SSLSocket ss = null;
            String keystore = ServerStatus.SG("cert_path");
            String certPass = ServerStatus.SG("globalKeystoreCertPass");
            String keystorePass = ServerStatus.SG("globalKeystorePass");
            if (!this.server_item.getProperty("customKeystore", "").equals(""))
              keystore = this.server_item.getProperty("customKeystore", ""); 
            if (!this.server_item.getProperty("customKeystoreCertPass", "").equals(""))
              certPass = this.server_item.getProperty("customKeystoreCertPass", ""); 
            if (!this.server_item.getProperty("customKeystorePass", "").equals(""))
              keystorePass = this.server_item.getProperty("customKeystorePass", ""); 
            boolean needClientAuth = !(!ServerStatus.BG("needClientAuth") && !this.server_item.getProperty("needClientAuth", "").equals("true") && (this.user == null || !this.user.getProperty("needClientAuth", "false").equals("true")));
            SSLSocketFactory factory = ServerStatus.thisObj.common_code.getSSLContext(keystore, String.valueOf(keystore) + "_trust", keystorePass, certPass, uiSG("secureType"), needClientAuth, true).getSocketFactory();
            this.not_done = write_command("234", LOC.G("Changing to secure mode..."));
            this.sockOriginal = this.sock;
            ss = (SSLSocket)factory.createSocket(this.sock, uiSG("port_remote_ip"), uiIG("port_remote_port"), false);
            this.sock = ss;
            Common.setEnabledCiphers(ServerStatus.SG("disabled_ciphers"), (SSLSocket)this.sock, null);
            ss.setNeedClientAuth(needClientAuth);
            ss.addHandshakeCompletedListener(new HandshakeCompletedListener(this) {
                  final ServerSession this$0;
                  
                  public void handshakeCompleted(HandshakeCompletedEvent event) {
                    ((SSLSocket)this.this$0.sock).setEnabledCipherSuites(new String[0]);
                  }
                });
            ss.setUseClientMode(uiBG("sscn_mode"));
            this.is = new BufferedReader(new InputStreamReader(this.sock.getInputStream(), SG("char_encoding")));
            this.os = this.sock.getOutputStream();
            uiPUT("secure", "true");
            if (this.server_item.getProperty("require_secure", "false").equals("true"))
              uiPUT("dataSecure", "true"); 
            uiPUT("user_protocol", "FTPS");
          } catch (Exception e) {
            add_log("SSL/TLS : " + LOC.G("Negotiation Failed."), "ACCEPT");
            add_log("SSL/TLS : " + LOC.G("Have you setup a certificate in the CrushFTP advanced prefs under the SSL tab?"), "ACCEPT");
            add_log("SSL/TLS : " + e.toString(), "ACCEPT");
            Log.log("FTP_SERVER", 1, e);
            this.not_done = write_command("550", LOC.G("Server not configured for encryption."));
            doErrorEvent(new Exception(uiSG("lastLog")));
          } 
        } else if (uiSG("the_command").equals("CCC")) {
          if (!uiBG("secure")) {
            this.not_done = write_command("550", LOC.G("Channel not encrypted."));
            doErrorEvent(new Exception(uiSG("lastLog")));
          } else {
            this.not_done = write_command("200", LOC.G("CCC command OK. Disabling encryption on control channel."));
            uiPUT("secure", "false");
            if (this.sockOriginal != null) {
              int originalTimeout = this.sock.getSoTimeout();
              Socket sock2 = this.sock;
              sock2.setSoTimeout(1000);
              if (System.getProperty("crushftp.allow_ccc_ssl_close", "true").equals("true")) {
                Thread t = new Thread(new Runnable(this, sock2) {
                      final ServerSession this$0;
                      
                      private final Socket val$sock2;
                      
                      public void run() {
                        try {
                          this.val$sock2.setSoTimeout(5000);
                          this.val$sock2.close();
                        } catch (Exception exception) {}
                      }
                    });
                t.start();
                Thread.sleep(100L);
                t.join(5000L);
                try {
                  t.interrupt();
                } catch (Exception exception) {}
              } 
              this.sockOriginal.setSoTimeout(originalTimeout);
              this.sock = this.sockOriginal;
              this.is = new BufferedReader(new InputStreamReader(this.sockOriginal.getInputStream(), SG("char_encoding")));
              this.os = this.sockOriginal.getOutputStream();
            } 
          } 
        } else if (uiSG("the_command").equals("PROT") && uiSG("the_command_data").length() > 0) {
          if (uiSG("the_command_data").toUpperCase().startsWith("P")) {
            uiPUT("dataSecure", "true");
            this.not_done = write_command("200", LOC.G("PROT command OK. Using $0 data connection", uiBG("dataSecure") ? "secure" : "clear") + ".");
          } else if (uiSG("the_command_data").toUpperCase().startsWith("C")) {
            if (uiBG("secure") && this.server_item.getProperty("require_secure", "false").equals("true")) {
              this.not_done = write_command("550", LOC.G("Encryption is required."));
              doErrorEvent(new Exception(uiSG("lastLog")));
            } else {
              uiPUT("dataSecure", "false");
              this.not_done = write_command("200", LOC.G("PROT command OK. Using $0 data connection", uiBG("dataSecure") ? "secure" : "clear") + ".");
            } 
          } 
        } else if (uiSG("the_command").equals("PBSZ")) {
          String bLevel = "0";
          if (!uiSG("the_command_data").trim().equals(""))
            bLevel = uiSG("the_command_data").trim(); 
          this.not_done = write_command("200", LOC.G("PBSZ command OK.  Using buffer size set to $0", bLevel) + ".");
        } else if (uiSG("the_command").equals("SYST")) {
          stop_idle_timer();
          this.not_done = write_command("215", LOC.G("%SYST%"));
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("SSCN")) {
          stop_idle_timer();
          if (uiSG("the_command_data").toUpperCase().equals("ON")) {
            uiPUT("sscn_mode", "true");
          } else if (uiSG("the_command_data").toUpperCase().equals("OFF")) {
            uiPUT("sscn_mode", "false");
          } 
          this.not_done = write_command("200", "SSCN:" + uiSG("the_command_data"));
          start_idle_timer();
        } else if (uiSG("the_command").equals("FEAT")) {
          stop_idle_timer();
          this.not_done = write_command("211-" + LOC.G("Extensions supported:"));
          if (uiBG("explicit_tls"))
            this.not_done = write_command_raw(" AUTH TLS"); 
          if (uiBG("explicit_ssl"))
            this.not_done = write_command_raw(" AUTH SSL"); 
          this.not_done = write_command_raw(" SSCN");
          this.not_done = write_command_raw(" PBSZ");
          this.not_done = write_command_raw(" PROT");
          this.not_done = write_command_raw(" CCC");
          this.not_done = write_command_raw(" CLNT");
          if (ServerStatus.BG("epsveprt")) {
            this.not_done = write_command_raw(" EPSV");
            this.not_done = write_command_raw(" EPRT");
          } 
          this.not_done = write_command_raw(" MDTM");
          this.not_done = write_command_raw(" MDTM YYYYMMDDHHMMSS[+-TZ];filename");
          this.not_done = write_command_raw(" MFMT");
          this.not_done = write_command_raw(" SIZE");
          this.not_done = write_command_raw(" REST STREAM");
          this.not_done = write_command_raw(" MODE Z");
          this.not_done = write_command_raw(" LIST -Q");
          this.not_done = write_command_raw(" SITE UTIME");
          this.not_done = write_command_raw(" SITE MD5");
          this.not_done = write_command_raw(" SITE MD5s");
          this.not_done = write_command_raw(" SITE RANDOMACCESS");
          if (ServerStatus.BG("allow_mlst"))
            this.not_done = write_command_raw(" MLST Type;Size;Modify;Perm;Unique;UNIX.owner;UNIX.group;"); 
          this.not_done = write_command_raw(" " + Common.replace_str(SG("char_encoding"), "UTF-8", "UTF8"));
          this.not_done = write_command("211 END");
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("OPTS")) {
          stop_idle_timer();
          if (uiSG("the_command_data").toUpperCase().indexOf("LEVEL ") >= 0) {
            uiPUT("zlibLevel", uiSG("the_command_data").substring(uiSG("the_command_data").lastIndexOf(" ")).trim());
            this.not_done = write_command("200 " + LOC.G("Level set to $0", uiSG("zlibLevel")) + ".");
          } else if (uiSG("the_command_data").toUpperCase().indexOf("UTF8 ON") >= 0 || uiSG("the_command_data").toUpperCase().indexOf("UTF-8 ON") >= 0) {
            this.user.put("char_encoding", "UTF8");
            this.not_done = write_command("200 UTF8 OPTS ON.");
          } else {
            this.not_done = write_command("502", LOC.G("Unknown OPS format."));
          } 
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("MODE") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          if (uiSG("the_command_data").toUpperCase().startsWith("Z")) {
            this.not_done = write_command("200", "MODE Z ok");
            uiPUT("modez", "true");
          } else if (uiSG("the_command_data").toUpperCase().startsWith("S")) {
            this.not_done = write_command("200", "MODE S ok");
            uiPUT("modez", "false");
          } else {
            this.not_done = write_command("502", LOC.G("%MODE%"));
          } 
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("CLNT") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          this.not_done = write_command("200", LOC.G("Noted."));
          start_idle_timer();
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("NOOP") || uiSG("the_command").equals("ALLO"))) {
          String the_dir = uiSG("current_dir");
          String parentPath = this.uVFS.getRootVFS(the_dir, -1);
          Properties dir_item = this.uVFS.get_item(parentPath, -1);
          if (dir_item.getProperty("protocol", "file").equalsIgnoreCase("FTP")) {
            GenericClient c = this.uVFS.getClient(dir_item);
            try {
              String response = c.doCommand("NOOP");
              this.not_done = write_command(response);
            } finally {
              c = this.uVFS.releaseClient(c);
            } 
          } else {
            this.not_done = write_command("200", LOC.G("%NOOP%"));
          } 
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("SIZE") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          do_SIZE();
          start_idle_timer();
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("MDTM") || uiSG("the_command").equals("MFMT"))) {
          stop_idle_timer();
          do_MDTM();
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("RNFR") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          do_RNFR();
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("RNTO") && uiSG("the_command_data").length() > 0 && this.rnfr_file != null) {
          stop_idle_timer();
          do_RNTO(ServerStatus.BG("rnto_overwrite"));
          start_idle_timer();
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("PWD") || uiSG("the_command").equals("XPWD"))) {
          stop_idle_timer();
          this.not_done = write_command("257", LOC.G("\"$0\" PWD command successful.", get_PWD()));
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("TYPE") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          if (uiSG("the_command_data").toUpperCase().startsWith("A")) {
            this.not_done = write_command("200", LOC.G("%TYPE-ascii%"));
            uiPUT("file_transfer_mode", "ASCII");
          } else if (uiSG("the_command_data").toUpperCase().startsWith("I")) {
            this.not_done = write_command("200", LOC.G("%TYPE-binary%"));
            uiPUT("file_transfer_mode", "BINARY");
          } else {
            this.not_done = write_command("504", LOC.G("Mode not supported."));
          } 
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("STRU") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          if (uiSG("the_command_data").startsWith("F")) {
            this.not_done = write_command("200", LOC.G("Accepted"));
          } else {
            this.not_done = write_command("550", LOC.G("$0 Not Supported.", uiSG("the_command_data")));
          } 
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("REST") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          uiPUT("start_resume_loc", (new StringBuffer(String.valueOf(Long.parseLong(uiSG("the_command_data"))))).toString());
          this.not_done = write_command("350", LOC.G("%REST%"));
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("DELE") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          do_DELE(false, null);
          start_idle_timer();
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("MKD") || uiSG("the_command").equals("XMKD")) && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          do_MKD(true, null);
          start_idle_timer();
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("RMD") || uiSG("the_command").equals("XRMD")) && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          do_RMD(uiSG("current_dir"));
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").startsWith("ABOR")) {
          stop_idle_timer();
          if (uiBG("sending_file")) {
            kill_stor_files(this.stor_files_pool_free);
            kill_stor_files(this.stor_files_pool_used);
          } else if (uiBG("receiving_file")) {
            kill_retr_files(this.retr_files_pool_free);
            kill_retr_files(this.retr_files_pool_used);
          } else if (uiBG("listing_files")) {
            this.list_files.thisThread.interrupt();
          } 
          Thread.sleep(1000L);
          this.not_done = write_command("225", LOC.G("%ABOR%"));
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("RETR") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          if (this.retr_files == null)
            this.retr_files = new RETR_handler(); 
          while (this.retr_files.active)
            Thread.sleep(100L); 
          String the_dir = uiSG("current_dir");
          if (!uiSG("the_command_data").equals("")) {
            if (uiSG("the_command_data").startsWith("/")) {
              the_dir = uiSG("the_command_data");
            } else {
              the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
            } 
            the_dir = Common.dots(the_dir);
            if (the_dir.equals("/"))
              the_dir = SG("root_dir"); 
            if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
              the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
          } 
          String other_the_dir = the_dir;
          String encode_on_fly = "";
          if (the_dir.toUpperCase().endsWith(".BIN") || the_dir.toUpperCase().endsWith(".ZIP"))
            other_the_dir = the_dir.substring(0, the_dir.length() - 4); 
          if (the_dir.endsWith("/")) {
            if (this.data_socks.size() > 0)
              ((Socket)this.data_socks.remove(0)).close(); 
            this.not_done = write_command("550", LOC.G("%RETR-wrong%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
          } else {
            String parentPath = this.uVFS.getRootVFS(the_dir, -1);
            Properties dir_item = this.uVFS.get_item(parentPath, -1);
            Properties item = this.uVFS.get_fake_item(the_dir, "FILE");
            boolean allowed = check_access_privs(the_dir, uiSG("the_command"), item);
            if ((allowed || check_access_privs(other_the_dir, uiSG("the_command"))) && Common.filter_check("D", Common.last(the_dir), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter"))) {
              if (ServerStatus.BG("binary_mode"))
                uiPUT("file_transfer_mode", "BINARY"); 
              if (!allowed) {
                encode_on_fly = "BIN";
                item = this.uVFS.get_item(other_the_dir);
                if (item != null) {
                  if (the_dir.toUpperCase().endsWith(".ZIP")) {
                    encode_on_fly = "ZIP";
                    item.put("url", String.valueOf(item.getProperty("url")) + ".zip");
                    Common.startMultiThreadZipper(this.uVFS, this.retr_files, other_the_dir, 2000, !item.getProperty("url").toUpperCase().startsWith("FILE:/"), new Vector());
                  } 
                  the_dir = other_the_dir;
                } 
              } 
              boolean connected = false;
              boolean pasv_conn = uiBG("pasv_connect");
              try {
                do_port_connect();
                connected = !(!pasv_conn && this.data_socks.size() <= 0);
              } catch (Exception e) {
                Log.log("FTP_SERVER", 1, e);
              } 
              if (the_dir.indexOf(":filetree") >= 0)
                item = this.uVFS.get_item(the_dir.substring(0, the_dir.indexOf(":filetree"))); 
              if (!connected) {
                if (this.data_socks.size() > 0)
                  ((Socket)this.data_socks.remove(0)).close(); 
                this.not_done = write_command("550", LOC.G("File not found, access denied, or port issue encountered."));
                doErrorEvent(new Exception(uiSG("lastLog")));
                uiVG("failed_commands").addElement((new Date()).getTime());
                start_idle_timer();
              } else if ((item != null && item.getProperty("type", "").equals("DIR") && !encode_on_fly.equals("ZIP") && the_dir.indexOf(":filetree") < 0) || item == null) {
                if (this.data_socks.size() > 0)
                  ((Socket)this.data_socks.remove(0)).close(); 
                this.not_done = write_command("550", LOC.G("%RETR-bad%"));
                doErrorEvent(new Exception(uiSG("lastLog")));
                uiVG("failed_commands").addElement((new Date()).getTime());
                start_idle_timer();
              } else if (ServerStatus.thisObj.server_download_queue.size() >= ServerStatus.IG("server_download_queue_size_max") && ServerStatus.BG("server_download_queueing") && item.getProperty("privs").indexOf("(bypassqueue)") < 0) {
                if (this.data_socks.size() > 0)
                  ((Socket)this.data_socks.remove(0)).close(); 
                this.not_done = write_command("550", LOC.G("%RETR-queue_full%"));
                doErrorEvent(new Exception(uiSG("lastLog")));
                uiVG("failed_commands").addElement((new Date()).getTime());
                start_idle_timer();
              } else {
                if (ServerStatus.BG("server_download_queueing")) {
                  uiPUT("pause_now", "true");
                  if (item.getProperty("privs").indexOf("(bypassqueue)") >= 0) {
                    ServerStatus.thisObj.server_download_queue.insertElementAt(this, 0);
                    uiPUT("pause_now", "false");
                  } else {
                    ServerStatus.thisObj.server_download_queue.addElement(this);
                  } 
                } 
                boolean allowTransfer = true;
                Properties p = new Properties();
                p.put("the_dir", the_dir);
                p.put("allowTransfer", "true");
                p.put("transferType", uiSG("the_command"));
                runPlugin("transfer_path", p);
                the_dir = p.getProperty("the_dir", the_dir);
                allowTransfer = p.getProperty("allowTransfer", "true").equals("true");
                if (allowTransfer) {
                  this.retr_files.init_vars(the_dir, uiLG("start_resume_loc"), -1L, this, item, uiBG("pasv_connect"), encode_on_fly, null);
                  Worker.startWorker(this.retr_files, String.valueOf(uiSG("user_name")) + ":(" + uiSG("user_number") + ")-" + uiSG("user_ip") + " (retr_handler)");
                  uiVG("failed_commands").removeAllElements();
                  uiPUT("start_resume_loc", "0");
                  int loops = 0;
                  while (loops++ < 10000 && (this.retr_files.streamOpenStatus.equals("STOPPED") || this.retr_files.streamOpenStatus.equals("PENDING")))
                    Thread.sleep(1L); 
                } else {
                  if (this.data_socks.size() > 0)
                    ((Socket)this.data_socks.remove(0)).close(); 
                  this.not_done = write_command("550", LOC.G("%RETR-bad% (plugin blocked)"));
                  doErrorEvent(new Exception(uiSG("lastLog")));
                  uiVG("failed_commands").addElement((new Date()).getTime());
                  start_idle_timer();
                } 
              } 
            } else {
              if (this.data_socks.size() > 0)
                ((Socket)this.data_socks.remove(0)).close(); 
              this.not_done = write_command("550", LOC.G("File not found, access denied."));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
              start_idle_timer();
            } 
          } 
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("STOR") || uiSG("the_command").equals("APPE") || uiSG("the_command").equals("STOU")) && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          uiPUT("current_dir", uiSG("current_dir").replace(':', '_'));
          String the_dir = uiSG("current_dir");
          Log.log("FTP_SERVER", 3, "STOR the_dir1:" + the_dir);
          if (!uiSG("the_command_data").equals("")) {
            if (uiSG("the_command_data").startsWith("/")) {
              the_dir = uiSG("the_command_data");
            } else {
              the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
            } 
            the_dir = Common.dots(the_dir);
            if (the_dir.equals("/"))
              the_dir = SG("root_dir"); 
            if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
              the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
          } 
          Log.log("FTP_SERVER", 3, "STOR the_dir1:" + the_dir);
          Properties item = this.uVFS.get_item(the_dir);
          Log.log("FTP_SERVER", 3, "item returned for STOR lookup of item:" + item);
          if (item == null) {
            item = this.uVFS.get_item_parent(the_dir);
            if (item != null)
              item.put("type", "FILE"); 
          } 
          if (check_access_privs(the_dir, uiSG("the_command"), item) && Common.filter_check("U", Common.last(the_dir), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter")) && item.getProperty("type").equalsIgnoreCase("FILE")) {
            if (ServerStatus.BG("make_dir_uploads")) {
              String tmp = uiSG("dont_write");
              uiPUT("dont_write", "true");
              do_MKD(true, Common.all_but_last(the_dir));
              uiPUT("dont_write", tmp);
            } 
            String realAction = uiSG("the_command");
            if (uiSG("the_command").equals("APPE")) {
              Properties item2 = this.uVFS.get_item(the_dir);
              try {
                if (item2 != null) {
                  uiPUT("start_resume_loc", (new StringBuffer(String.valueOf(Long.parseLong(item2.getProperty("size"))))).toString());
                } else {
                  uiPUT("start_resume_loc", "0");
                } 
              } catch (Exception exception) {}
            } 
            if (ServerStatus.BG("binary_mode_stor"))
              uiPUT("file_transfer_mode", "BINARY"); 
            long quota = get_quota(the_dir);
            do_port_connect();
            if (ServerStatus.thisObj.server_upload_queue.size() >= ServerStatus.IG("server_upload_queue_size_max") && ServerStatus.BG("server_upload_queueing") && item.getProperty("privs").indexOf("(bypassqueue)") < 0) {
              if (this.data_socks.size() > 0)
                ((Socket)this.data_socks.remove(0)).close(); 
              this.not_done = write_command("550", LOC.G("%STOR-queue_full%"));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
              start_idle_timer();
            } else if (quota < 0L && quota != -12345L) {
              if (this.data_socks.size() > 0)
                ((Socket)this.data_socks.remove(0)).close(); 
              this.not_done = write_command("550-" + LOC.G("Your quota has been exceeded") + ".  " + LOC.G("Available") + ": " + quota + "k.");
              this.not_done = write_command("550", LOC.G("%STOR-quota exceeded%"));
              doErrorEvent(new Exception(uiSG("lastLog")));
              start_idle_timer();
            } else {
              if (ServerStatus.BG("server_upload_queueing")) {
                uiPUT("pause_now", "true");
                if (item.getProperty("privs").indexOf("(bypassqueue)") >= 0) {
                  uiPUT("pause_now", "false");
                  ServerStatus.thisObj.server_upload_queue.insertElementAt(this, 0);
                } else {
                  ServerStatus.thisObj.server_upload_queue.addElement(this);
                } 
              } 
              if (this.stor_files == null)
                this.stor_files = new STOR_handler(); 
              while (this.stor_files.active)
                Thread.sleep(100L); 
              boolean allowTransfer = true;
              Properties p = new Properties();
              p.put("the_dir", the_dir);
              p.put("allowTransfer", "true");
              p.put("transferType", uiSG("the_command"));
              p.put("pluginError", LOC.G("%STOR-bad% (plugin blocked)"));
              runPlugin("transfer_path", p);
              String pluginMsg = p.getProperty("pluginError");
              the_dir = p.getProperty("the_dir", the_dir);
              allowTransfer = p.getProperty("allowTransfer", "true").equals("true");
              if (allowTransfer) {
                this.stor_files.init_vars(the_dir, uiLG("start_resume_loc"), this, item, realAction, uiSG("the_command").equals("STOU"), uiBG("randomaccess"));
                Worker.startWorker(this.stor_files, String.valueOf(uiSG("user_name")) + ":(" + uiSG("user_number") + ")-" + uiSG("user_ip") + " (stor_handler)");
                uiPUT("start_resume_loc", "0");
                uiVG("failed_commands").removeAllElements();
                int loops = 0;
                while (loops++ < 10000 && (this.stor_files.streamOpenStatus.equals("STOPPED") || this.stor_files.streamOpenStatus.equals("PENDING")))
                  Thread.sleep(1L); 
              } else {
                if (this.data_socks.size() > 0)
                  ((Socket)this.data_socks.remove(0)).close(); 
                this.not_done = write_command("550", pluginMsg);
                doErrorEvent(new Exception(uiSG("lastLog")));
                uiVG("failed_commands").addElement((new Date()).getTime());
                start_idle_timer();
              } 
            } 
          } else {
            if (this.data_socks.size() > 0)
              ((Socket)this.data_socks.remove(0)).close(); 
            if (item != null && item.getProperty("type").equalsIgnoreCase("DIR")) {
              this.not_done = write_command("553", LOC.G("$0 is a directory.", uiSG("the_command_data")));
              Log.log("FTP_SERVER", 2, LOC.G("STOR Failure item:$0", item.toString()));
            } else {
              this.not_done = write_command("550", LOC.G("%STOR-bad%"));
            } 
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
            start_idle_timer();
          } 
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("LIST") || uiSG("the_command").equals("NLST") || (uiSG("the_command").equals("MLSD") && ServerStatus.BG("allow_mlst")))) {
          stop_idle_timer();
          boolean names_only = false;
          do_port_connect();
          if (uiSG("the_command_data").startsWith("-f"))
            names_only = true; 
          if (uiSG("the_command_data").startsWith("-") && !uiSG("the_command_data").startsWith("-Q"))
            if (uiSG("the_command_data").indexOf(" ", uiSG("the_command_data").indexOf("-")) > 0)
              uiPUT("the_command_data", uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ", uiSG("the_command_data").indexOf("-")) + 1));  
          String the_dir = uiSG("current_dir");
          if (the_dir.equals("/"))
            the_dir = SG("root_dir"); 
          if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
            the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
          if (!uiSG("the_command_data").equals(""))
            if (uiSG("the_command_data").startsWith("/")) {
              the_dir = uiSG("the_command_data");
              if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
                the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
              Properties tester = this.uVFS.get_fake_item(the_dir, "DIR");
              if (tester == null) {
                if (!the_dir.endsWith("/") && !ServerStatus.BG("disable_dir_filter")) {
                  the_dir = Common.all_but_last(the_dir);
                  uiPUT("the_command_data", Common.last(uiSG("the_command_data")));
                } else {
                  uiPUT("the_command_data", "");
                  if (ServerStatus.BG("disable_dir_filter"))
                    the_dir = String.valueOf(the_dir) + "/"; 
                } 
              } else if (tester.getProperty("type", "").equals("DIR")) {
                if (!the_dir.endsWith("/")) {
                  the_dir = String.valueOf(the_dir) + "/";
                  uiPUT("the_command_data", "");
                } 
              } else if (!the_dir.endsWith("/")) {
                the_dir = Common.all_but_last(the_dir);
                uiPUT("the_command_data", Common.last(uiSG("the_command_data")));
              } else {
                uiPUT("the_command_data", "");
              } 
            } else if (uiSG("the_command_data").indexOf("/") >= 0 || ServerStatus.BG("disable_dir_filter")) {
              the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
              if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
                the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
              Properties tester = this.uVFS.get_fake_item(the_dir, "DIR");
              if (tester == null) {
                if (!the_dir.endsWith("/")) {
                  the_dir = Common.all_but_last(the_dir);
                  uiPUT("the_command_data", Common.last(uiSG("the_command_data")));
                } else {
                  uiPUT("the_command_data", "");
                } 
              } else if (tester.getProperty("type", "").equals("DIR")) {
                if (!the_dir.endsWith("/")) {
                  the_dir = String.valueOf(the_dir) + "/";
                  uiPUT("the_command_data", "");
                } 
              } else if (!the_dir.endsWith("/")) {
                the_dir = Common.all_but_last(the_dir);
                uiPUT("the_command_data", Common.last(uiSG("the_command_data")));
              } else {
                uiPUT("the_command_data", "");
              } 
            }  
          the_dir = Common.dots(the_dir);
          if (the_dir.equals("/") || the_dir.equals(""))
            the_dir = SG("root_dir"); 
          if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
            the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
          Properties dir_item = this.uVFS.get_item(the_dir, -1);
          boolean showListing = check_access_privs(the_dir, uiSG("the_command"), dir_item);
          if (!the_dir.endsWith("/"))
            the_dir = Common.all_but_last(the_dir); 
          if (this.list_files == null)
            this.list_files = new LIST_handler(); 
          while (this.list_files.active)
            Thread.sleep(100L); 
          if (uiSG("the_command").equals("NLST") && (!Common.last(uiSG("the_command_data")).startsWith("-") || Common.last(uiSG("the_command_data")).indexOf("l") <= 0))
            names_only = true; 
          this.list_files.init_vars(the_dir, names_only, this, Common.last(uiSG("the_command_data")), showListing, (uiSG("the_command").equals("MLSD") && ServerStatus.BG("allow_mlst")));
          uiPUT("listing_files", "true");
          Worker.startWorker(this.list_files, String.valueOf(uiSG("user_name")) + ":(" + uiSG("user_number") + ")-" + uiSG("user_ip") + " (list_handler)");
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("HELP")) {
          this.not_done = write_command("214", String.valueOf(ServerStatus.thisObj.change_vars_to_values(LOC.G("%HELP-start%"), this).trim()) + this.CRLF + LOC.G("%HELP-end%"));
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("STAT") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          String the_dir = uiSG("current_dir");
          if (!uiSG("the_command_data").equals("")) {
            if (uiSG("the_command_data").startsWith("/")) {
              the_dir = uiSG("the_command_data");
            } else {
              the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
            } 
            the_dir = Common.dots(the_dir);
            if (the_dir.equals("/"))
              the_dir = SG("root_dir"); 
            if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
              the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
          } 
          boolean STAT_ok = false;
          if (check_access_privs(the_dir, uiSG("the_command"))) {
            Properties item = this.uVFS.get_item(the_dir);
            if (item != null) {
              STAT_ok = true;
              Vector items = new Vector();
              if (item.getProperty("type", "DIR").equals("DIR")) {
                this.uVFS.getListing(items, the_dir);
              } else {
                items.addElement(item);
              } 
              StringBuffer item_str = new StringBuffer();
              Vector nameList = new Vector();
              for (int x = 0; x < items.size(); x++) {
                Properties p = items.elementAt(x);
                if (LIST_handler.checkName(p, this, false, false))
                  if (nameList.indexOf(p.getProperty("name")) < 0) {
                    LIST_handler.generateLineEntry(p, item_str, false, the_dir, false, this, false);
                    nameList.addElement(p.getProperty("name"));
                  }  
              } 
              this.not_done = write_command("211-" + LOC.G("%STAT-start%"));
              if (item_str.toString().trim().length() > 0)
                this.not_done = write_command(item_str.toString().trim()); 
              this.not_done = write_command("211 " + LOC.G("%STAT-end%"));
            } 
          } 
          if (!STAT_ok) {
            this.not_done = write_command("550", "%STAT-bad%");
            uiVG("failed_commands").addElement((new Date()).getTime());
          } 
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("MLST") && uiSG("the_command_data").length() > 0 && ServerStatus.BG("allow_mlst")) {
          if (ServerStatus.BG("mdtm_gmt"))
            this.sdf_yyyyMMddHHmmss.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT"))); 
          stop_idle_timer();
          String the_dir = uiSG("current_dir");
          if (!uiSG("the_command_data").equals("")) {
            if (uiSG("the_command_data").startsWith("/")) {
              the_dir = uiSG("the_command_data");
            } else {
              the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
            } 
            the_dir = Common.dots(the_dir);
            if (the_dir.equals("/"))
              the_dir = SG("root_dir"); 
            if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
              the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
          } 
          boolean STAT_ok = false;
          if (check_access_privs(the_dir, uiSG("the_command"))) {
            Properties item = this.uVFS.get_item(the_dir);
            if (item != null) {
              STAT_ok = true;
              Vector items = new Vector();
              items.addElement(item);
              StringBuffer item_str = new StringBuffer();
              for (int x = 0; x < items.size(); x++) {
                Properties p = items.elementAt(x);
                if (LIST_handler.checkName(p, this, false, false))
                  LIST_handler.generateLineEntry(p, item_str, false, the_dir, false, this, true); 
              } 
              this.not_done = write_command("250-" + LOC.G("Listing") + " " + uiSG("the_command_data"));
              if (item_str.toString().trim().length() > 0)
                this.not_done = write_command(item_str.toString().trim()); 
              this.not_done = write_command("250 " + LOC.G("End"));
            } 
          } 
          if (!STAT_ok) {
            this.not_done = write_command("550", LOC.G("%STAT-bad%"));
            uiVG("failed_commands").addElement((new Date()).getTime());
          } 
          start_idle_timer();
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("CWD") || uiSG("the_command").equals("XCWD"))) {
          stop_idle_timer();
          do_CWD();
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("CDUP")) {
          stop_idle_timer();
          uiPUT("the_command_data", "..");
          do_CWD();
          start_idle_timer();
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("PASV") || (uiSG("the_command").equals("EPSV") && ServerStatus.BG("epsveprt")))) {
          stop_idle_timer();
          try {
            Properties pasv_stuff = do_pasv_command();
            while (this.pasv_socks.size() > 0)
              ((ServerSocket)this.pasv_socks.remove(0)).close(); 
            ServerSocket pasv_sock2 = (ServerSocket)pasv_stuff.remove("server_socket");
            uiPUT("PASV_port", (new StringBuffer(String.valueOf(Integer.parseInt(pasv_stuff.getProperty("port"))))).toString());
            Worker.startWorker(new Runnable(this, pasv_sock2) {
                  final ServerSession this$0;
                  
                  private final ServerSocket val$pasv_sock2;
                  
                  public void run() {
                    try {
                      Thread.currentThread().setName(String.valueOf(this.this$0.this_thread.getName()) + " (PASV " + this.val$pasv_sock2.getLocalPort() + ")");
                      this.val$pasv_sock2.setSoTimeout(20000);
                      this.this$0.pasv_socks.addElement(this.val$pasv_sock2);
                      Socket tempSock = this.val$pasv_sock2.accept();
                      this.val$pasv_sock2.close();
                      if (!this.this$0.uiBG("adminAllowed") && this.this$0.uiSG("user_ip").equals("127.0.0.1"))
                        this.this$0.uiPUT("user_ip", tempSock.getInetAddress().getHostAddress()); 
                      this.this$0.kill_active_socks();
                      this.this$0.data_socks.addElement(tempSock);
                      this.this$0.old_data_socks.addElement(tempSock);
                    } catch (SocketTimeoutException e) {
                      if (this.this$0.pasv_socks.contains(this.val$pasv_sock2))
                        try {
                          this.this$0.write_command("550", e + this.this$0.CRLF + LOC.G("20 second timeout while waiting for PASV connection on port $0.", (new StringBuffer(String.valueOf(this.val$pasv_sock2.getLocalPort()))).toString()));
                          this.this$0.doErrorEvent(new Exception(this.this$0.uiSG("lastLog")));
                        } catch (Exception exception) {} 
                      try {
                        this.val$pasv_sock2.close();
                      } catch (Exception exception) {}
                    } catch (IOException e) {
                      try {
                        this.val$pasv_sock2.close();
                        this.this$0.pasv_socks.remove(this.val$pasv_sock2);
                      } catch (Exception exception) {}
                    } finally {
                      this.this$0.pasv_socks.remove(this.val$pasv_sock2);
                    } 
                  }
                });
            if (uiSG("the_command").equals("EPSV")) {
              this.not_done = write_command("229", LOC.G("Entering Extended Passive Mode") + " (|||" + pasv_stuff.getProperty("port", "0") + "|)");
            } else {
              this.not_done = write_command("227", LOC.G("Entering Passive Mode") + " (" + pasv_stuff.getProperty("data_string") + ")");
            } 
            uiPUT("port_remote_ip", pasv_stuff.getProperty("ip", "0.0.0.0"));
            uiPUT("port_remote_port", (new StringBuffer(String.valueOf(Integer.parseInt(pasv_stuff.getProperty("port", "0"))))).toString());
            uiPUT("pasv_connect", "true");
          } catch (Exception e) {
            Log.log("FTP_SERVER", 1, e);
            if (e.indexOf("Interrupted") >= 0)
              throw e; 
          } 
          start_idle_timer();
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("PORT") || (uiSG("the_command").equals("EPRT") && ServerStatus.BG("epsveprt"))) && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          try {
            if (uiSG("the_command").equals("EPRT")) {
              if (uiSG("the_command_data").startsWith("|"))
                uiPUT("the_command_data", "1" + uiSG("the_command_data")); 
              uiPUT("the_command_data", uiSG("the_command_data").replace('|', ','));
              StringTokenizer st = new StringTokenizer(uiSG("the_command_data"), ",");
              String EIPV = st.nextToken().toString();
              String EIPProtocol = st.nextToken().toString();
              String EIP = st.nextToken().toString();
              String EPortStr = st.nextToken().toString();
              int EPort = Integer.parseInt(EPortStr.trim());
              if (EIP.equals(""))
                EIP = uiSG("user_ip"); 
              uiPUT("the_command_data", String.valueOf(EIP.replace('.', ',')) + "," + (EPort / 256) + "," + (EPort - EPort / 256 * 256));
              uiPUT("port_remote_ip", EIP);
              uiPUT("port_remote_port", (new StringBuffer(String.valueOf(EPort))).toString());
            } else {
              uiPUT("last_port_string", uiSG("the_command_data"));
              uiPUT("pasv_connect", "false");
              try {
                while (this.pasv_socks.size() > 0)
                  ((ServerSocket)this.pasv_socks.remove(0)).close(); 
              } catch (Exception exception) {}
              boolean parsing_ip = true;
              int str_pos = 0;
              int comma_count = 0;
              while (parsing_ip) {
                if (uiSG("the_command_data").charAt(str_pos++) == ',')
                  comma_count++; 
                if (comma_count == 4)
                  parsing_ip = false; 
              } 
              uiPUT("port_remote_ip", uiSG("the_command_data").substring(0, str_pos - 1));
              uiPUT("port_remote_ip", uiSG("port_remote_ip").replace(',', '.'));
              if (!uiBG("adminAllowed") && uiSG("user_ip").equals("127.0.0.1"))
                uiPUT("user_ip", uiSG("port_remote_ip")); 
              String remote_port_raw = uiSG("the_command_data").substring(str_pos);
              Integer port_part1 = new Integer(remote_port_raw.substring(0, remote_port_raw.indexOf(',')));
              Integer port_part2 = new Integer(remote_port_raw.substring(remote_port_raw.indexOf(',') + 1, remote_port_raw.length()));
              int port1 = port_part1.intValue();
              int port2 = port_part2.intValue();
              uiPUT("port_remote_port", (new StringBuffer(String.valueOf(port1 * 256 + port2))).toString());
            } 
            if (ServerStatus.BG("deny_reserved_ports") && uiIG("port_remote_port") < 1024) {
              uiPUT("port_remote_ip", "0.0.0.0");
              this.not_done = write_command("550", LOC.G("%PORT_reserved%"));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
              uiPUT("port_remote_port", "0");
            } else if (ServerStatus.BG("deny_fxp") && !uiSG("port_remote_ip").equals(uiSG("user_ip"))) {
              this.not_done = write_command("550", LOC.G("%PORT-fxp%"));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
              uiPUT("port_remote_ip", "0.0.0.0");
              uiPUT("port_remote_port", "0");
            } else if (uiBG("secure") && ServerStatus.BG("deny_secure_active_mode")) {
              this.not_done = write_command("509", LOC.G("Active mode FTP won't work when the control channel is encrypted, use passive (PASV) mode."));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
              uiPUT("port_remote_ip", "0.0.0.0");
              uiPUT("port_remote_port", "0");
            } else {
              this.not_done = write_command("200", LOC.G("%PORT%"));
            } 
          } catch (Exception e) {
            if (e.indexOf("Interrupted") >= 0)
              throw e; 
          } 
          start_idle_timer();
        } else if (uiBG("user_logged_in") && (uiSG("the_command").equals("SITE") || uiSG("the_command").equals("RCMD")) && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          do_SITE();
          start_idle_timer();
        } else if (!uiBG("user_logged_in") && uiSG("the_command").equals("SITE") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          if (uiSG("the_command_data").toUpperCase().startsWith("PROXY_")) {
            String key = uiSG("the_command_data");
            if (key.indexOf(" ") >= 0)
              key = key.substring(0, key.indexOf(" ")); 
            String val = "";
            if (uiSG("the_command_data").indexOf(" ") >= 0)
              val = uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1); 
            uiPUT(key.toLowerCase(), val);
            this.not_done = write_command("214", "OK");
          } else {
            this.not_done = write_command("502", LOC.G("%unknown_command%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
          } 
          start_idle_timer();
        } else if (uiBG("user_logged_in") && uiSG("the_command").equals("ACCT") && uiSG("the_command_data").length() > 0) {
          stop_idle_timer();
          GenericClient c = this.uVFS.getClient(this.uVFS.get_item(uiSG("current_dir")));
          try {
            String response = c.doCommand(String.valueOf(uiSG("the_command")) + " " + uiSG("the_command_data"));
            if (response == null) {
              this.not_done = write_command("502", LOC.G("%unknown_command%"));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
            } else {
              this.not_done = write_command(response);
            } 
          } finally {
            c = this.uVFS.releaseClient(c);
          } 
          start_idle_timer();
        } else if (uiSG("the_command").equals("QUIT")) {
          stop_idle_timer();
          int loops = 0;
          while (this.retr_files != null && this.retr_files.active) {
            Thread.sleep(100L);
            if (loops++ > 50)
              break; 
          } 
          loops = 0;
          while (this.stor_files != null && this.stor_files.active) {
            Thread.sleep(100L);
            if (loops++ > 50)
              break; 
          } 
          loops = 0;
          while (this.list_files != null && this.list_files.active) {
            Thread.sleep(100L);
            if (loops++ > 50)
              break; 
          } 
          this.not_done = write_command("221", LOC.G("%QUIT%"));
          try {
            this.uVFS.free();
          } catch (Exception exception) {}
          try {
            this.uVFS.disconnect();
          } catch (Exception exception) {}
          this.not_done = false;
        } else if (uiBG("user_logged_in")) {
          if (uiSG("the_command").length() > 0) {
            this.not_done = write_command("502", LOC.G("%unknown_command%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
          } 
        } else if (uiSG("the_command").length() > 0) {
          this.not_done = write_command("550", LOC.G("%unknown_command%"));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
        } 
        uiPUT("the_command", "");
        uiPUT("the_command_data", "");
      } 
    } catch (Exception e) {
      boolean needClientAuth = !(!ServerStatus.BG("needClientAuth") && !this.server_item.getProperty("needClientAuth", "").equals("true") && (this.user == null || !this.user.getProperty("needClientAuth", "false").equals("true")));
      if (e.toString().indexOf("null cert chain") >= 0 && needClientAuth)
        add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] *" + LOC.G("Client certificate was rejected.") + "*", "QUIT"); 
      Log.log("FTP_SERVER", 1, e);
      uiPUT("dieing", "true");
    } 
    add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] *" + LOC.G("Disconnected") + ".*", "QUIT");
    uiPUT("dieing", "true");
    do_kill();
  }
  
  public void start_idle_timer() throws Exception {
    if (uiBG("user_logged_in")) {
      stop_idle_timer();
      start_idle_timer(IG("max_idle_time"));
    } 
  }
  
  public void start_idle_timer(int timeout) throws Exception {
    try {
      if (uiBG("user_logged_in")) {
        this.thread_killer_item.timeout = timeout;
        this.thread_killer_item.last_activity = (new Date()).getTime();
        this.thread_killer_item.enabled = (timeout > 0);
      } 
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
  
  public void do_kill() {
    // Byte code:
    //   0: aload_0
    //   1: iconst_0
    //   2: putfield not_done : Z
    //   5: aload_0
    //   6: getfield thread_killer_item : Lcrushftp/handlers/IdlerKiller;
    //   9: iconst_1
    //   10: putfield die_now : Z
    //   13: goto -> 17
    //   16: astore_1
    //   17: aload_0
    //   18: aconst_null
    //   19: putfield thread_killer_item : Lcrushftp/handlers/IdlerKiller;
    //   22: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   25: getfield statTools : Lcrushftp/db/StatTools;
    //   28: ldc_w 'stats_update_sessions'
    //   31: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   34: iconst_2
    //   35: anewarray java/lang/Object
    //   38: dup
    //   39: iconst_0
    //   40: new java/util/Date
    //   43: dup
    //   44: invokespecial <init> : ()V
    //   47: aastore
    //   48: dup
    //   49: iconst_1
    //   50: aload_0
    //   51: getfield user_info : Ljava/util/Properties;
    //   54: ldc_w 'SESSION_RID'
    //   57: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   60: aastore
    //   61: invokevirtual executeSql : (Ljava/lang/String;[Ljava/lang/Object;)V
    //   64: goto -> 77
    //   67: astore_1
    //   68: ldc_w 'SERVER'
    //   71: iconst_2
    //   72: aload_1
    //   73: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   76: pop
    //   77: aload_0
    //   78: ldc_w 'didDisconnect'
    //   81: invokevirtual uiBG : (Ljava/lang/String;)Z
    //   84: ifne -> 118
    //   87: aload_0
    //   88: ldc_w 'didDisconnect'
    //   91: ldc_w 'true'
    //   94: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   97: aload_0
    //   98: ldc_w 'LOGOUT'
    //   101: aconst_null
    //   102: invokevirtual do_event5 : (Ljava/lang/String;Ljava/util/Properties;)V
    //   105: goto -> 118
    //   108: astore_1
    //   109: ldc_w 'SERVER'
    //   112: iconst_2
    //   113: aload_1
    //   114: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   117: pop
    //   118: aload_0
    //   119: ldc_w 'ratio_field_permanent'
    //   122: invokevirtual BG : (Ljava/lang/String;)Z
    //   125: ifeq -> 239
    //   128: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   131: aload_0
    //   132: ldc 'listen_ip_port'
    //   134: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   137: aload_0
    //   138: ldc 'user_name'
    //   140: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   143: ldc_w 'user_bytes_sent'
    //   146: new java/lang/StringBuffer
    //   149: dup
    //   150: aload_0
    //   151: ldc_w 'bytes_sent'
    //   154: invokevirtual uiLG : (Ljava/lang/String;)J
    //   157: aload_0
    //   158: ldc_w 'ratio_bytes_sent'
    //   161: invokevirtual uiLG : (Ljava/lang/String;)J
    //   164: ladd
    //   165: invokestatic valueOf : (J)Ljava/lang/String;
    //   168: invokespecial <init> : (Ljava/lang/String;)V
    //   171: invokevirtual toString : ()Ljava/lang/String;
    //   174: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    //   177: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   180: aload_0
    //   181: ldc 'listen_ip_port'
    //   183: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   186: aload_0
    //   187: ldc 'user_name'
    //   189: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   192: ldc_w 'user_bytes_received'
    //   195: new java/lang/StringBuffer
    //   198: dup
    //   199: aload_0
    //   200: ldc_w 'bytes_received'
    //   203: invokevirtual uiLG : (Ljava/lang/String;)J
    //   206: aload_0
    //   207: ldc_w 'ratio_bytes_received'
    //   210: invokevirtual uiLG : (Ljava/lang/String;)J
    //   213: ladd
    //   214: invokestatic valueOf : (J)Ljava/lang/String;
    //   217: invokespecial <init> : (Ljava/lang/String;)V
    //   220: invokevirtual toString : ()Ljava/lang/String;
    //   223: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    //   226: goto -> 239
    //   229: astore_1
    //   230: ldc_w 'SERVER'
    //   233: iconst_2
    //   234: aload_1
    //   235: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   238: pop
    //   239: aload_0
    //   240: getfield uVFS : Lcrushftp/server/VFS;
    //   243: ifnull -> 253
    //   246: aload_0
    //   247: getfield uVFS : Lcrushftp/server/VFS;
    //   250: invokevirtual free : ()V
    //   253: aload_0
    //   254: getfield uVFS : Lcrushftp/server/VFS;
    //   257: ifnull -> 290
    //   260: aload_0
    //   261: getfield server_item : Ljava/util/Properties;
    //   264: ldc 'serverType'
    //   266: ldc 'ftp'
    //   268: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   271: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   274: ldc_w 'HTTP'
    //   277: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   280: ifne -> 290
    //   283: aload_0
    //   284: getfield uVFS : Lcrushftp/server/VFS;
    //   287: invokevirtual disconnect : ()V
    //   290: aload_0
    //   291: getfield uVFS : Lcrushftp/server/VFS;
    //   294: ifnull -> 305
    //   297: aload_0
    //   298: getfield uVFS : Lcrushftp/server/VFS;
    //   301: aconst_null
    //   302: putfield thisSession : Lcrushftp/server/ServerSession;
    //   305: aload_0
    //   306: aconst_null
    //   307: putfield uVFS : Lcrushftp/server/VFS;
    //   310: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   313: aload_0
    //   314: getfield user_info : Ljava/util/Properties;
    //   317: invokevirtual remove_user : (Ljava/util/Properties;)V
    //   320: goto -> 337
    //   323: aload_0
    //   324: getfield pasv_socks : Ljava/util/Vector;
    //   327: iconst_0
    //   328: invokevirtual remove : (I)Ljava/lang/Object;
    //   331: checkcast java/net/ServerSocket
    //   334: invokevirtual close : ()V
    //   337: aload_0
    //   338: getfield pasv_socks : Ljava/util/Vector;
    //   341: invokevirtual size : ()I
    //   344: ifgt -> 323
    //   347: goto -> 351
    //   350: astore_1
    //   351: aload_0
    //   352: getfield retr_files : Lcrushftp/server/RETR_handler;
    //   355: ifnull -> 369
    //   358: aload_0
    //   359: getfield retr_files_pool_used : Ljava/util/Vector;
    //   362: aload_0
    //   363: getfield retr_files : Lcrushftp/server/RETR_handler;
    //   366: invokevirtual addElement : (Ljava/lang/Object;)V
    //   369: aload_0
    //   370: aload_0
    //   371: getfield retr_files_pool_free : Ljava/util/Vector;
    //   374: invokevirtual kill_retr_files : (Ljava/util/Vector;)V
    //   377: aload_0
    //   378: aload_0
    //   379: getfield retr_files_pool_used : Ljava/util/Vector;
    //   382: invokevirtual kill_retr_files : (Ljava/util/Vector;)V
    //   385: goto -> 389
    //   388: astore_1
    //   389: aload_0
    //   390: getfield stor_files : Lcrushftp/server/STOR_handler;
    //   393: ifnull -> 407
    //   396: aload_0
    //   397: getfield stor_files_pool_used : Ljava/util/Vector;
    //   400: aload_0
    //   401: getfield stor_files : Lcrushftp/server/STOR_handler;
    //   404: invokevirtual addElement : (Ljava/lang/Object;)V
    //   407: aload_0
    //   408: aload_0
    //   409: getfield stor_files_pool_free : Ljava/util/Vector;
    //   412: invokevirtual kill_stor_files : (Ljava/util/Vector;)V
    //   415: aload_0
    //   416: aload_0
    //   417: getfield stor_files_pool_used : Ljava/util/Vector;
    //   420: invokevirtual kill_stor_files : (Ljava/util/Vector;)V
    //   423: goto -> 427
    //   426: astore_1
    //   427: aload_0
    //   428: getfield list_files : Lcrushftp/server/LIST_handler;
    //   431: iconst_1
    //   432: putfield die_now : Z
    //   435: goto -> 439
    //   438: astore_1
    //   439: aload_0
    //   440: aconst_null
    //   441: putfield retr_files : Lcrushftp/server/RETR_handler;
    //   444: aload_0
    //   445: aconst_null
    //   446: putfield stor_files : Lcrushftp/server/STOR_handler;
    //   449: aload_0
    //   450: aconst_null
    //   451: putfield list_files : Lcrushftp/server/LIST_handler;
    //   454: aload_0
    //   455: getfield sock : Ljava/net/Socket;
    //   458: sipush #2000
    //   461: invokevirtual setSoTimeout : (I)V
    //   464: aload_0
    //   465: getfield sock : Ljava/net/Socket;
    //   468: iconst_1
    //   469: iconst_2
    //   470: invokevirtual setSoLinger : (ZI)V
    //   473: aload_0
    //   474: getfield sock : Ljava/net/Socket;
    //   477: invokevirtual close : ()V
    //   480: goto -> 484
    //   483: astore_1
    //   484: aload_0
    //   485: getfield thread_killer_items : Ljava/util/Properties;
    //   488: aload_0
    //   489: getfield sock : Ljava/net/Socket;
    //   492: invokevirtual toString : ()Ljava/lang/String;
    //   495: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
    //   498: pop
    //   499: aload_0
    //   500: getfield sockOriginal : Ljava/net/Socket;
    //   503: sipush #2000
    //   506: invokevirtual setSoTimeout : (I)V
    //   509: aload_0
    //   510: getfield sockOriginal : Ljava/net/Socket;
    //   513: iconst_1
    //   514: iconst_2
    //   515: invokevirtual setSoLinger : (ZI)V
    //   518: aload_0
    //   519: getfield sockOriginal : Ljava/net/Socket;
    //   522: invokevirtual close : ()V
    //   525: goto -> 579
    //   528: astore_1
    //   529: goto -> 579
    //   532: aload_0
    //   533: getfield old_data_socks : Ljava/util/Vector;
    //   536: iconst_0
    //   537: invokevirtual remove : (I)Ljava/lang/Object;
    //   540: astore_1
    //   541: aload_1
    //   542: instanceof java/net/Socket
    //   545: ifeq -> 565
    //   548: aload_1
    //   549: checkcast java/net/Socket
    //   552: sipush #2000
    //   555: invokevirtual setSoTimeout : (I)V
    //   558: aload_1
    //   559: checkcast java/net/Socket
    //   562: invokevirtual close : ()V
    //   565: aload_1
    //   566: instanceof java/net/ServerSocket
    //   569: ifeq -> 579
    //   572: aload_1
    //   573: checkcast java/net/ServerSocket
    //   576: invokevirtual close : ()V
    //   579: aload_0
    //   580: getfield old_data_socks : Ljava/util/Vector;
    //   583: invokevirtual size : ()I
    //   586: ifgt -> 532
    //   589: goto -> 593
    //   592: astore_1
    //   593: aload_0
    //   594: getfield os : Ljava/io/OutputStream;
    //   597: invokevirtual close : ()V
    //   600: goto -> 604
    //   603: astore_1
    //   604: aload_0
    //   605: getfield is : Ljava/io/BufferedReader;
    //   608: invokevirtual close : ()V
    //   611: goto -> 615
    //   614: astore_1
    //   615: aload_0
    //   616: aconst_null
    //   617: putfield this_thread : Ljava/lang/Thread;
    //   620: return
    // Line number table:
    //   Java source line number -> byte code offset
    //   #1738	-> 0
    //   #1741	-> 5
    //   #1743	-> 16
    //   #1746	-> 17
    //   #1749	-> 22
    //   #1751	-> 67
    //   #1753	-> 68
    //   #1757	-> 77
    //   #1759	-> 87
    //   #1760	-> 97
    //   #1763	-> 108
    //   #1765	-> 109
    //   #1769	-> 118
    //   #1771	-> 128
    //   #1772	-> 177
    //   #1775	-> 229
    //   #1777	-> 230
    //   #1779	-> 239
    //   #1780	-> 253
    //   #1781	-> 290
    //   #1782	-> 305
    //   #1783	-> 310
    //   #1786	-> 320
    //   #1787	-> 323
    //   #1786	-> 337
    //   #1789	-> 350
    //   #1794	-> 351
    //   #1795	-> 369
    //   #1796	-> 377
    //   #1798	-> 388
    //   #1803	-> 389
    //   #1804	-> 407
    //   #1805	-> 415
    //   #1807	-> 426
    //   #1812	-> 427
    //   #1814	-> 438
    //   #1817	-> 439
    //   #1818	-> 444
    //   #1819	-> 449
    //   #1822	-> 454
    //   #1823	-> 464
    //   #1824	-> 473
    //   #1826	-> 483
    //   #1829	-> 484
    //   #1832	-> 499
    //   #1833	-> 509
    //   #1834	-> 518
    //   #1836	-> 528
    //   #1841	-> 529
    //   #1843	-> 532
    //   #1844	-> 541
    //   #1846	-> 548
    //   #1847	-> 558
    //   #1849	-> 565
    //   #1841	-> 579
    //   #1852	-> 592
    //   #1857	-> 593
    //   #1859	-> 603
    //   #1864	-> 604
    //   #1866	-> 614
    //   #1870	-> 615
    //   #1871	-> 620
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	621	0	this	Lcrushftp/server/ServerSession;
    //   68	9	1	e	Ljava/lang/Exception;
    //   109	9	1	e	Ljava/lang/Exception;
    //   230	9	1	e	Ljava/lang/Exception;
    //   541	38	1	obj	Ljava/lang/Object;
    // Exception table:
    //   from	to	target	type
    //   5	13	16	java/lang/Exception
    //   22	64	67	java/lang/Exception
    //   77	105	108	java/lang/Exception
    //   118	226	229	java/lang/Exception
    //   320	347	350	java/lang/Exception
    //   351	385	388	java/lang/Exception
    //   389	423	426	java/lang/Exception
    //   427	435	438	java/lang/Exception
    //   454	480	483	java/lang/Exception
    //   499	525	528	java/lang/Exception
    //   529	589	592	java/lang/Exception
    //   593	600	603	java/lang/Exception
    //   604	611	614	java/lang/Exception
  }
  
  public void kill_stor_files(Vector v) {
    while (v.size() > 0) {
      STOR_handler sf = v.remove(0);
      sf.die_now = true;
      if (sf.thisThread != null)
        sf.thisThread.interrupt(); 
      try {
        if (sf.data_is != null)
          sf.data_is.close(); 
      } catch (IOException e) {
        Log.log("SERVER", 1, e);
      } 
    } 
  }
  
  public void kill_retr_files(Vector v) {
    while (v.size() > 0) {
      RETR_handler rf = v.remove(0);
      rf.die_now = true;
      if (rf.thisThread != null)
        rf.thisThread.interrupt(); 
    } 
  }
  
  public void log_pauses() {
    add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] *" + (uiBG("pause_now") ? (LOC.G("Paused") + ".*") : (LOC.G("Unpaused") + ".*")), "PAUSE_RESUME");
  }
  
  public int get_command() throws Exception {
    if (uiBG("dont_read"))
      return 0; 
    if (this.is == null)
      return -1; 
    uiPUT("the_command", "");
    String data = null;
    while (true) {
      try {
        data = this.is.readLine();
        break;
      } catch (SocketTimeoutException e) {
        boolean killOk = true;
        if (this.stor_files != null && this.stor_files.active)
          killOk = false; 
        if (this.retr_files != null && this.retr_files.active)
          killOk = false; 
        if (this.list_files != null && this.list_files.active)
          killOk = false; 
        if (killOk) {
          uiPUT("termination_message", "TIMEOUT");
          do_kill();
          throw e;
        } 
      } 
    } 
    if (data == null)
      return -1; 
    if (!data.toUpperCase().startsWith("PASS "))
      data = Common.url_decode(data); 
    uiPUT("the_command", data.trim());
    uiPUT("the_command", ServerStatus.thisObj.strip_variables(uiSG("the_command"), this));
    if (uiSG("the_command").indexOf(" ") >= 0) {
      uiPUT("the_command_data", uiSG("the_command").substring(uiSG("the_command").indexOf(" ") + 1));
      uiPUT("the_command", uiSG("the_command").substring(0, uiSG("the_command").indexOf(" ")));
    } else {
      uiPUT("the_command_data", "");
    } 
    uiPUT("the_command", uiSG("the_command").toUpperCase());
    if (!uiSG("the_command").equals("USER") && !uiSG("the_command").equals("PASS") && ServerStatus.BG("fix_slashes"))
      uiPUT("the_command_data", uiSG("the_command_data").replace('\\', '/')); 
    if (uiSG("the_command").indexOf("ABOR") >= 0)
      uiPUT("the_command", "ABOR"); 
    if (uiSG("the_command").startsWith("PASS") && !uiSG("user_name").toUpperCase().equals("ANONYMOUS") && !uiSG("user_name").toUpperCase().equals("PUBLIC")) {
      add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("READ") + " : *" + uiSG("the_command") + "*", uiSG("the_command"));
      uiPUT("last_action", uiSG("the_command"));
    } else if (uiSG("the_command").startsWith("PASS") && uiSG("user_name").toUpperCase().equals("ANONYMOUS")) {
      add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("READ") + " : *" + uiSG("the_command") + " " + uiSG("the_command_data") + "*", "USER");
      uiPUT("last_action", uiSG("the_command"));
    } else {
      add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("READ") + " : *" + uiSG("the_command") + " " + uiSG("the_command_data") + "*", uiSG("the_command"));
      uiPUT("last_action", String.valueOf(uiSG("the_command")) + " " + uiSG("the_command_data"));
    } 
    uiPUT("last_logged_command", uiSG("the_command"));
    String full_the_command_data = uiSG("the_command_data");
    if (!full_the_command_data.startsWith("/"))
      full_the_command_data = String.valueOf(uiSG("current_dir")) + full_the_command_data; 
    Properties p = new Properties();
    p.put("the_command", uiSG("the_command"));
    p.put("the_command_data", full_the_command_data);
    p.put("user_time", ServerStatus.thisObj.logDateFormat.format(new Date()));
    String tempCommandData = uiSG("the_command_data");
    if (uiSG("the_command").toUpperCase().equals("PASS"))
      tempCommandData = "**************"; 
    p.put("display", String.valueOf(p.getProperty("user_time")) + " | " + uiSG("the_command") + " " + tempCommandData);
    p.put("stamp", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    uiVG("session_commands").addElement(p);
    while (uiVG("session_commands").size() > ServerStatus.IG("user_log_buffer"))
      uiVG("session_commands").removeElementAt(0); 
    return 1;
  }
  
  public boolean write_command(String data) throws Exception {
    try {
      data = ServerStatus.thisObj.change_vars_to_values(data, this).trim();
      return write_command_raw(data);
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
      do_kill();
      return false;
    } 
  }
  
  public boolean write_command_raw(String data) throws Exception {
    try {
      add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] WROTE: *" + data + "*", uiSG("last_logged_command"));
      data = String.valueOf(data) + this.CRLF;
      if (uiBG("dont_write"))
        return true; 
      this.os.write(data.getBytes(SG("char_encoding")));
      this.os.flush();
      return true;
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
      do_kill();
      return false;
    } 
  }
  
  public boolean write_command(String code, String data) throws Exception {
    try {
      data = ServerStatus.thisObj.change_vars_to_values(data, this);
      Properties p = new Properties();
      p.put("command_code", code);
      p.put("command_data", data);
      runPlugin("afterCommand", p);
      data = p.getProperty("command_data", data);
      code = p.getProperty("command_code", code);
      data = ServerStatus.thisObj.common_code.format_message(code, data).trim();
      add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("WROTE") + ": *" + data + "*", uiSG("last_logged_command"));
      uiPUT("lastLog", data);
      if (uiBG("dont_write"))
        return true; 
      data = String.valueOf(data) + this.CRLF;
      this.os.write(data.getBytes(SG("char_encoding")));
      this.os.flush();
      return true;
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
      Log.log("FTP_SERVER", 2, e);
      do_kill();
      return false;
    } 
  }
  
  public String decode(String data) {
    return Common.decode64(data);
  }
  
  public void do_port_connect() throws Exception {
    if (uiBG("pasv_connect")) {
      uiPUT("pasv_connect", "false");
    } else {
      start_idle_timer(60000);
      int loop = 0;
      while (loop++ < 3) {
        if (uiBG("dataSecure")) {
          SSLSocket ss = null;
          try {
            String keystore = ServerStatus.SG("cert_path");
            String certPass = ServerStatus.SG("globalKeystoreCertPass");
            String keystorePass = ServerStatus.SG("globalKeystorePass");
            if (!this.server_item.getProperty("customKeystore", "").equals(""))
              keystore = this.server_item.getProperty("customKeystore", ""); 
            if (!this.server_item.getProperty("customKeystoreCertPass", "").equals(""))
              certPass = this.server_item.getProperty("customKeystoreCertPass", ""); 
            if (!this.server_item.getProperty("customKeystorePass", "").equals(""))
              keystorePass = this.server_item.getProperty("customKeystorePass", ""); 
            boolean needClientAuth = !(!ServerStatus.BG("needClientAuth") && !this.server_item.getProperty("needClientAuth", "").equals("true") && (this.user == null || !this.user.getProperty("needClientAuth", "false").equals("true")));
            SSLSocketFactory factory = ServerStatus.thisObj.common_code.getSSLContext(keystore, String.valueOf(keystore) + "_trust", keystorePass, certPass, uiSG("secureType"), needClientAuth, true).getSocketFactory();
            try {
              Socket tempSock = new Socket();
              tempSock.setReuseAddress(true);
              try {
                tempSock.bind(new InetSocketAddress(this.sock.getLocalAddress(), Integer.parseInt(this.server_item.getProperty("source_port", (new StringBuffer(String.valueOf(Integer.parseInt(this.server_item.getProperty("bind_port", "21")) - 1))).toString()))));
              } catch (IOException e) {
                tempSock.bind(new InetSocketAddress(this.sock.getLocalAddress(), 0));
              } 
              tempSock.connect(new InetSocketAddress(uiSG("port_remote_ip"), uiIG("port_remote_port")));
              ss = (SSLSocket)factory.createSocket(tempSock, uiSG("port_remote_ip"), uiIG("port_remote_port"), true);
            } catch (Exception e) {
              Socket tempSock = new Socket(uiSG("port_remote_ip"), uiIG("port_remote_port"));
              ss = (SSLSocket)factory.createSocket(tempSock, uiSG("port_remote_ip"), uiIG("port_remote_port"), true);
            } 
            ss.setNeedClientAuth(needClientAuth);
            ss.setUseClientMode(uiBG("sscn_mode"));
            if (uiBG("sscn_mode"))
              ss.startHandshake(); 
            kill_active_socks();
            this.data_socks.addElement(ss);
            this.old_data_socks.addElement(ss);
          } catch (Exception e) {
            e.printStackTrace();
          } 
          break;
        } 
        try {
          if (!Common.machine_is_mac()) {
            Socket socket = new Socket();
            socket.setReuseAddress(true);
            try {
              socket.bind(new InetSocketAddress(this.sock.getLocalAddress(), Integer.parseInt(this.server_item.getProperty("source_port", "20"))));
            } catch (IOException e) {
              socket.bind(new InetSocketAddress(this.sock.getLocalAddress(), 0));
            } 
            socket.connect(new InetSocketAddress(uiSG("port_remote_ip"), uiIG("port_remote_port")));
            kill_active_socks();
            this.data_socks.addElement(socket);
            this.old_data_socks.addElement(socket);
            break;
          } 
          ServerSocket portlookup = new ServerSocket(0);
          int locPort = portlookup.getLocalPort();
          portlookup.close();
          Socket tempSock = new Socket(uiSG("port_remote_ip"), uiIG("port_remote_port"), this.sock.getLocalAddress(), locPort);
          kill_active_socks();
          this.data_socks.addElement(tempSock);
          this.old_data_socks.addElement(tempSock);
          break;
        } catch (IOException e) {
          try {
            Socket tempSock = new Socket(uiSG("port_remote_ip"), uiIG("port_remote_port"));
            kill_active_socks();
            this.data_socks.addElement(tempSock);
            this.old_data_socks.addElement(tempSock);
            break;
          } catch (IOException ee) {
            Log.log("FTP_SERVER", 1, ee);
            Thread.sleep(1000L);
          } 
        } 
      } 
      stop_idle_timer();
    } 
  }
  
  public static Object portLocker = new Object();
  
  String last_priv_dir;
  
  boolean shareVFS;
  
  public Properties do_pasv_command() throws Exception {
    Properties return_item = new Properties();
    ServerSocket create_it = null;
    try {
      int loop_count = 0;
      if (this.server_item.getProperty("pasv_ports") == null)
        this.server_item.put("pasv_ports", "1025-65535"); 
      if (this.server_item.getProperty("ftp_aware_router") == null)
        this.server_item.put("ftp_aware_router", "true"); 
      while (create_it == null) {
        if (this.server_item.getProperty("pasv_ports").indexOf("1025") >= 0 && (this.server_item.getProperty("pasv_ports").indexOf("65536") >= 0 || this.server_item.getProperty("pasv_ports").indexOf("65535") >= 0)) {
          if (uiBG("dataSecure")) {
            String keystore = ServerStatus.SG("cert_path");
            String certPass = ServerStatus.SG("globalKeystoreCertPass");
            String keystorePass = ServerStatus.SG("globalKeystorePass");
            if (!this.server_item.getProperty("customKeystore", "").equals(""))
              keystore = this.server_item.getProperty("customKeystore", ""); 
            if (!this.server_item.getProperty("customKeystoreCertPass", "").equals(""))
              certPass = this.server_item.getProperty("customKeystoreCertPass", ""); 
            if (!this.server_item.getProperty("customKeystorePass", "").equals(""))
              keystorePass = this.server_item.getProperty("customKeystorePass", ""); 
            boolean needClientAuth = !(!ServerStatus.BG("needClientAuth") && !this.server_item.getProperty("needClientAuth", "").equals("true") && (this.user == null || !this.user.getProperty("needClientAuth", "false").equals("true")));
            try {
              create_it = ServerStatus.thisObj.common_code.getServerSocket(0, uiSG("listen_ip"), keystore, keystorePass, certPass, ServerStatus.SG("disabled_ciphers"), needClientAuth, 1000);
            } catch (Exception e) {
              if (e.indexOf("Interrupted") >= 0)
                throw e; 
              Log.log("FTP_SERVER", 1, e);
            } 
            if (create_it == null && !this.server_item.getProperty("ip", "0.0.0.0").equals("lookup"))
              try {
                create_it = ServerStatus.thisObj.common_code.getServerSocket(0, this.server_item.getProperty("ip", "0.0.0.0"), keystore, keystorePass, certPass, ServerStatus.SG("disabled_ciphers"), needClientAuth, 1000);
              } catch (Exception e) {
                if (e.indexOf("Interrupted") >= 0)
                  throw e; 
                Log.log("FTP_SERVER", 1, e);
              }  
            if (create_it == null)
              try {
                create_it = ServerStatus.thisObj.common_code.getServerSocket(0, "0.0.0.0", keystore, keystorePass, certPass, ServerStatus.SG("disabled_ciphers"), needClientAuth, 1000);
              } catch (Exception e) {
                if (e.indexOf("Interrupted") >= 0)
                  throw e; 
                Log.log("FTP_SERVER", 1, e);
              }  
          } else {
            try {
              create_it = new ServerSocket(0, 999, InetAddress.getByName(uiSG("listen_ip")));
            } catch (Exception e) {
              if (e.indexOf("Interrupted") >= 0)
                throw e; 
              Log.log("FTP_SERVER", 3, e);
            } 
            if (create_it == null && !this.server_item.getProperty("ip", "0.0.0.0").equals("lookup"))
              try {
                create_it = new ServerSocket(0, 999, InetAddress.getByName(this.server_item.getProperty("ip", "0.0.0.0")));
              } catch (Exception e) {
                if (e.indexOf("Interrupted") >= 0)
                  throw e; 
                Log.log("FTP_SERVER", 3, e);
              }  
            if (create_it == null)
              try {
                create_it = new ServerSocket(0);
              } catch (Exception e) {
                if (e.indexOf("Interrupted") >= 0)
                  throw e; 
                Log.log("FTP_SERVER", 1, e);
              }  
          } 
        } else {
          synchronized (portLocker) {
            String pasv_ports = this.server_item.getProperty("pasv_ports").trim();
            int min_pasv_port = 1025;
            int max_pasv_port = 65535;
            try {
              if (pasv_ports.indexOf(",") < 0 && pasv_ports.indexOf("-") > 0) {
                min_pasv_port = Integer.parseInt(pasv_ports.substring(0, pasv_ports.indexOf("-")).trim());
                max_pasv_port = Integer.parseInt(pasv_ports.substring(pasv_ports.indexOf("-") + 1).trim());
              } else {
                min_pasv_port = Integer.parseInt(pasv_ports.substring(0, pasv_ports.indexOf(",")).trim());
                max_pasv_port = Integer.parseInt(pasv_ports.substring(pasv_ports.indexOf(",") + 1).trim());
              } 
              this.current_pasv_port = Integer.parseInt(this.server_item.getProperty("current_pasv_port", (new StringBuffer(String.valueOf(min_pasv_port))).toString()));
              if (this.current_pasv_port < min_pasv_port || this.current_pasv_port > max_pasv_port)
                this.current_pasv_port = min_pasv_port; 
              if (uiBG("dataSecure")) {
                String keystore = ServerStatus.SG("cert_path");
                String certPass = ServerStatus.SG("globalKeystoreCertPass");
                String keystorePass = ServerStatus.SG("globalKeystorePass");
                if (!this.server_item.getProperty("customKeystore", "").equals(""))
                  keystore = this.server_item.getProperty("customKeystore", ""); 
                if (!this.server_item.getProperty("customKeystoreCertPass", "").equals(""))
                  certPass = this.server_item.getProperty("customKeystoreCertPass", ""); 
                if (!this.server_item.getProperty("customKeystorePass", "").equals(""))
                  keystorePass = this.server_item.getProperty("customKeystorePass", ""); 
                boolean needClientAuth = !(!ServerStatus.BG("needClientAuth") && !this.server_item.getProperty("needClientAuth", "").equals("true") && (this.user == null || !this.user.getProperty("needClientAuth", "false").equals("true")));
                try {
                  create_it = ServerStatus.thisObj.common_code.getServerSocket(this.current_pasv_port, uiSG("listen_ip"), keystore, keystorePass, certPass, ServerStatus.SG("disabled_ciphers"), needClientAuth, 1000);
                } catch (Exception e) {
                  if (e.indexOf("Interrupted") >= 0)
                    throw e; 
                } 
                if (create_it == null && !this.server_item.getProperty("ip", "0.0.0.0").equals("lookup"))
                  try {
                    create_it = ServerStatus.thisObj.common_code.getServerSocket(this.current_pasv_port, this.server_item.getProperty("ip", "0.0.0.0"), keystore, keystorePass, certPass, ServerStatus.SG("disabled_ciphers"), needClientAuth, 1000);
                  } catch (Exception e) {
                    if (e.indexOf("Interrupted") >= 0)
                      throw e; 
                  }  
              } else {
                try {
                  create_it = new ServerSocket(this.current_pasv_port, 999, InetAddress.getByName(uiSG("listen_ip")));
                } catch (Exception e) {
                  if (e.indexOf("Interrupted") >= 0)
                    throw e; 
                } 
                if (create_it == null && !this.server_item.getProperty("ip", "0.0.0.0").equals("lookup"))
                  try {
                    create_it = new ServerSocket(this.current_pasv_port, 999, InetAddress.getByName(this.server_item.getProperty("ip", "0.0.0.0")));
                  } catch (Exception e) {
                    if (e.indexOf("Interrupted") >= 0)
                      throw e; 
                  }  
                if (create_it == null)
                  try {
                    create_it = new ServerSocket(this.current_pasv_port);
                  } catch (Exception e) {
                    if (e.indexOf("Interrupted") >= 0)
                      throw e; 
                  }  
              } 
            } finally {
              this.current_pasv_port++;
              if (this.current_pasv_port > max_pasv_port)
                this.current_pasv_port = min_pasv_port; 
              this.server_item.put("current_pasv_port", (new StringBuffer(String.valueOf(this.current_pasv_port))).toString());
              if (this.current_pasv_port == min_pasv_port && loop_count > 1)
                break; 
            } 
          } 
        } 
        if (create_it != null)
          break; 
        Thread.sleep(100L);
        if (loop_count++ > 1000)
          Thread.currentThread().interrupt(); 
      } 
    } catch (Exception e) {
      Log.log("FTP_SERVER", 1, e);
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    return_item.put("server_socket", create_it);
    int the_port = create_it.getLocalPort();
    return_item.put("port", (new StringBuffer(String.valueOf(the_port))).toString());
    String return_address = null;
    if (uiSG("user_ip").startsWith("127.0")) {
      return_address = uiSG("user_ip");
    } else if (uiSG("listen_ip").indexOf(",") >= 0) {
      return_address = uiSG("listen_ip");
    } else if (uiSG("user_ip").split("\\.")[0].equals(create_it.getInetAddress().getHostAddress().split("\\.")[0]) && uiSG("user_ip").split("\\.")[1].equals(create_it.getInetAddress().getHostAddress().split("\\.")[1])) {
      return_address = create_it.getInetAddress().getHostAddress();
    } else if (ServerStatus.BG("allow_local_ip_pasv") && uiSG("user_ip").split("\\.")[0].equals(Common.getLocalIP().split("\\.")[0]) && uiSG("user_ip").split("\\.")[1].equals(Common.getLocalIP().split("\\.")[1])) {
      return_address = Common.getLocalIP();
    } else if (ServerStatus.BG("allow_local_ip_pasv_any") && uiSG("user_ip").split("\\.")[0].equals(Common.getLocalIP().split("\\.")[0])) {
      return_address = Common.getLocalIP();
    } else if (uiBG("secure") || this.server_item.getProperty("ftp_aware_router").equals("false")) {
      return_address = uiSG("listen_ip");
    } else if (create_it.getInetAddress().getHostAddress().equals("0.0.0.0")) {
      return_address = Common.getLocalIP();
    } else {
      return_address = create_it.getInetAddress().getHostAddress();
    } 
    Log.log("FTP_SERVER", 3, "return_address:" + return_address);
    Log.log("FTP_SERVER", 3, "listen_ip:" + uiSG("listen_ip"));
    Log.log("FTP_SERVER", 3, "create_it:" + create_it.getInetAddress().getHostAddress());
    if (return_address.indexOf(",") >= 0) {
      return_address = return_address.substring(0, return_address.indexOf(",")).trim();
      String ips = uiSG("listen_ip");
      ips = ips.substring(ips.indexOf(",") + 1).trim();
      ips = String.valueOf(ips) + "," + return_address;
      uiPUT("listen_ip", ips);
      this.server_item.put("server_ip", ips);
    } 
    return_item.put("ip", return_address);
    return_item.put("data_string", String.valueOf(return_address.replace('.', ',')) + "," + (the_port / 256) + "," + (the_port - the_port / 256 * 256));
    return return_item;
  }
  
  public ServerSession(Socket sock, int user_number, String user_ip, int listen_port, String listen_ip, String listen_ip_port, Properties server_item) {
    this.last_priv_dir = "";
    this.shareVFS = false;
    this.sock = sock;
    this.server_item = server_item;
    try {
      this.delayInterval = Integer.parseInt(server_item.getProperty("commandDelayInterval", "0"));
    } catch (Exception exception) {}
    uiPUT("session", this);
    uiPUT("id", (new StringBuffer(String.valueOf(user_number))).toString());
    uiPUT("user_number", (new StringBuffer(String.valueOf(user_number))).toString());
    uiPUT("listen_ip_port", server_item.getProperty("linkedServer", ""));
    uiPUT("listen_ip", listen_ip);
    uiPUT("bind_port", (new StringBuffer(String.valueOf(listen_port))).toString());
    uiPUT("bind_ip", "0.0.0.0");
    uiPUT("bind_ip_config", server_item.getProperty("ip", listen_ip));
    if (!server_item.getProperty("ip", listen_ip).equals("lookup") && !server_item.getProperty("ip", listen_ip).equals("manual"))
      uiPUT("bind_ip", server_item.getProperty("ip", listen_ip)); 
    uiPUT("user_ip", user_ip);
    uiPUT("user_protocol", server_item.getProperty("serverType", "ftp"));
    uiPUT("user_protocol_proxy", server_item.getProperty("serverType", "ftp"));
    uiPUT("user_port", (sock == null) ? "0" : (new StringBuffer(String.valueOf(sock.getPort()))).toString());
    uiPUT("user_name", "");
    uiPUT("current_password", "");
    uiPUT("the_command", "");
    uiPUT("the_command_data", "");
    uiPUT("current_dir", "/");
    uiPUT("user_logged_in", "false");
    uiPUT("user_log", new Vector());
    uiPUT("failed_commands", new Vector());
    uiPUT("session_commands", new Vector());
    uiPUT("refresh_user", "false");
    uiPUT("stat", new Properties());
    uiPUT("password_expired", "false");
    uiPUT("password_attempts", new Vector());
    uiPUT("lastUploadStats", new Vector());
    uiPUT("proxy_mode", "none");
    uiPUT("dieing", "false");
    uiPUT("pasv_connect", "false");
    uiPUT("last_logged_command", "");
    uiPUT("session_uploads", "");
    uiPUT("session_downloads", "");
    uiPUT("list_filetree_status", "");
    uiPUT("session_download_count", "0");
    uiPUT("session_upload_count", "0");
    uiPUT("list_zip_dir", "false");
    uiPUT("list_zip_file", "false");
    uiPUT("list_zip_only", "false");
    uiPUT("list_zip_app", ServerStatus.SG("list_zip_app"));
    uiPUT("list_dot", "true");
    uiPUT("zlibLevel", "8");
    uiPUT("last_file_real_path", "");
    uiPUT("last_file_name", "");
    uiPUT("login_date_stamp", "");
    uiPUT("login_date", "");
    uiPUT("login_date_formatted", "");
    uiPUT("termination_message", "");
    uiPUT("file_transfer_mode", ServerStatus.SG("file_transfer_mode"));
    uiPUT("modez", "false");
    uiPUT("dataSecure", "false");
    uiPUT("secureType", "TLS");
    uiPUT("friendly_quit", "false");
    uiPUT("randomaccess", "false");
    uiPUT("last_port_string", "");
    uiPUT("last_time_remaining", "");
    uiPUT("last_action", "");
    uiPUT("crc", "");
    uiPUT("pause_now", "false");
    uiPUT("new_pass1", "");
    uiPUT("new_pass2", "");
    uiPUT("PASV_port", "2000");
    uiPUT("sending_file", "false");
    uiPUT("receiving_file", "false");
    uiPUT("listing_files", "false");
    uiPUT("dont_write", "false");
    uiPUT("dont_read", "false");
    uiPUT("dont_log", "false");
    uiPUT("didDisconnect", "false");
    uiPUT("adminAllowed", "true");
    uiPUT("sscn_mode", "false");
    uiPUT("file_length", "0");
    uiPUT("start_transfer_time", "0");
    uiPUT("end_part_transfer_time", "0");
    uiPUT("overall_transfer_speed", "0");
    uiPUT("current_transfer_speed", "0");
    uiPUT("seconds_remaining", "0");
    uiPUT("start_transfer_byte_amount", "0");
    uiPUT("bytes_sent", "0");
    uiPUT("bytes_sent_formatted", "0b");
    uiPUT("bytes_received", "0");
    uiPUT("bytes_received_formatted", "0b");
    uiPUT("ratio_bytes_sent", "0");
    uiPUT("ratio_bytes_received", "0");
    uiPUT("start_resume_loc", "0");
    uiPUT("no_zip_compression", "false");
    uiPUT("secure", "false");
    uiPUT("explicit_ssl", "false");
    uiPUT("explicit_tls", "false");
    uiPUT("require_encryption", "false");
    uiPUT("login_date_stamp", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    uiPUT("login_date_stamp_unique", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    uiPUT("login_date", (new Date()).toString());
    uiPUT("login_date_formatted", ServerStatus.thisObj.logDateFormat.format(new Date()));
    uiPUT("time", ServerStatus.thisObj.logDateFormat.format(new Date()));
    if (server_item.getProperty("serverType", "FTP").toUpperCase().equals("FTPS")) {
      uiPUT("secure", "true");
      uiPUT("dataSecure", "true");
      uiPUT("sscn_mode", "false");
    } 
    if (server_item.getProperty("explicit_ssl", "false").toUpperCase().equals("TRUE"))
      uiPUT("explicit_ssl", "true"); 
    if (server_item.getProperty("explicit_tls", "false").toUpperCase().equals("TRUE"))
      uiPUT("explicit_tls", "true"); 
    if (server_item.getProperty("require_encryption", "false").toUpperCase().equals("TRUE"))
      uiPUT("require_encryption", "true"); 
    this.sdf_yyyyMMddHHmmssGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }
  
  public boolean check_access_privs(String the_dir, String command) throws Exception {
    try {
      Properties item = this.uVFS.get_fake_item(the_dir, "FILE");
      return check_access_privs(the_dir, command, item);
    } catch (Exception e) {
      Log.log("ACCESS", 2, e);
      return false;
    } 
  }
  
  public boolean check_access_privs(String the_dir, String command, Properties item) throws Exception {
    if (the_dir.indexOf(":filetree") >= 0) {
      the_dir = the_dir.substring(0, the_dir.indexOf(":filetree"));
      if (item == null)
        item = this.uVFS.get_item(the_dir); 
    } 
    try {
      Properties item2 = null;
      String additionalAccess = check_access_exception(the_dir, command, item);
      Properties p = new Properties();
      p.put("command", command);
      p.put("the_command_data", uiSG("the_command_data"));
      if (item != null)
        p.put("item", item); 
      if ((item == null && command.equals("MKD")) || command.equals("XMKD")) {
        if (the_dir.equals(SG("root_dir"))) {
          item2 = this.uVFS.get_item(the_dir);
        } else {
          item2 = this.uVFS.get_item_parent(the_dir);
        } 
        if (item2 != null)
          p.put("item", item2); 
      } 
      p.put("the_dir", the_dir);
      p.put("additionalAccess", additionalAccess);
      runPlugin("access", p);
      additionalAccess = p.getProperty("additionalAccess", additionalAccess);
      command = p.getProperty("command", command);
      the_dir = p.getProperty("the_dir", the_dir);
      if (p.get("item") != null && item2 == null)
        item = (Properties)p.get("item"); 
      String last_dir = this.last_priv_dir;
      this.last_priv_dir = the_dir;
      String privs = (item == null) ? "" : (String.valueOf(item.getProperty("privs", "")) + additionalAccess);
      Properties combinedPermissions = this.uVFS.getCombinedPermissions();
      boolean aclPermissions = combinedPermissions.getProperty("acl_permissions", "false").equals("true");
      if (aclPermissions) {
        if (item == null)
          item = this.uVFS.get_item(Common.all_but_last(the_dir)); 
        privs = this.uVFS.getPriv(the_dir, item);
      } 
      Pattern pattern = null;
      String block_access = SG("block_access").trim();
      block_access = String.valueOf(block_access) + "\r\n";
      block_access = String.valueOf(block_access) + ServerStatus.SG("block_access").trim();
      block_access = block_access.trim();
      BufferedReader br = new BufferedReader(new StringReader(block_access));
      String searchPattern = "";
      while ((searchPattern = br.readLine()) != null) {
        searchPattern = searchPattern.trim();
        try {
          pattern = Pattern.compile(searchPattern);
        } catch (Exception e) {
          e.printStackTrace();
        } 
        if (!searchPattern.startsWith("~") && pattern != null && pattern.matcher(the_dir).matches())
          return false; 
        if (searchPattern.startsWith("~") && Common.do_search(searchPattern.substring(1), the_dir, false, 0))
          return false; 
      } 
      if (command.equals("WWW")) {
        if (privs.indexOf("(www)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("CWD"))
        if (the_dir.equals("/"))
          return true;  
      if (command.equals("CWD")) {
        if (item != null)
          return true; 
        return false;
      } 
      if (command.equals("RETR") && uiLG("start_resume_loc") > 0L) {
        if (privs.indexOf("(resume)") >= 0 && privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("RETR"))
        if ((privs == null || privs.indexOf("(read)") < 0) && item != null && item.getProperty("size", "0").equals("0"))
          return true;  
      if (command.equals("RETR")) {
        if (privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("DELE")) {
        if (privs.indexOf("(delete)") >= 0 && (privs.indexOf("(inherited)") >= 0 || this.uVFS.getCombinedPermissions().getProperty("acl_permissions", "false").equals("true")))
          return true; 
        return false;
      } 
      if (command.equals("RNFR"))
        if (privs.indexOf("(rename)") < 0 && item != null && item.getProperty("name").toUpperCase().startsWith("NEW FOLDER"))
          return true;  
      if (command.equals("RNFR")) {
        if (privs.indexOf("(rename)") >= 0 && (privs.indexOf("(inherited)") >= 0 || this.uVFS.getCombinedPermissions().getProperty("acl_permissions", "false").equals("true")))
          return true; 
        return false;
      } 
      if (command.equals("STOR") && uiLG("start_resume_loc") > 0L && item != null) {
        if (privs.indexOf("(resume)") >= 0 && privs.indexOf("(write)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("APPE") && item != null) {
        if (privs.indexOf("(resume)") >= 0 && privs.indexOf("(write)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("SIZE")) {
        if (privs.indexOf("(view)") >= 0 || privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("MDTM")) {
        if (privs.indexOf("(view)") >= 0 || privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("STAT")) {
        if (privs.indexOf("(view)") >= 0 || privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("MLSD") || command.equals("MLST")) {
        if (privs.indexOf("(view)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("LIST") || command.equals("NLST")) {
        if (privs.indexOf("(view)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("SHARE")) {
        if (privs.indexOf("(share)") >= 0)
          return true; 
        return false;
      } 
      if (the_dir.equals(SG("root_dir"))) {
        item = this.uVFS.get_item(the_dir);
      } else {
        item = this.uVFS.get_item_parent(the_dir);
      } 
      privs = (item == null) ? "" : (String.valueOf(item.getProperty("privs", "")) + additionalAccess);
      if (command.equals("STOR") || command.equals("APPE") || command.equals("STOU")) {
        if (privs.indexOf("(write)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("MKD") || command.equals("XMKD")) {
        if (privs.indexOf("(makedir)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("RMD") || command.equals("XRMD")) {
        if (privs.indexOf("(deletedir)") >= 0 && (privs.indexOf("(inherited)") >= 0 || this.uVFS.getCombinedPermissions().getProperty("acl_permissions", "false").equals("true")))
          return true; 
        return false;
      } 
      if (command.equals("RNTO"))
        if (privs.indexOf("(rename)") >= 0 && Common.all_but_last(last_dir).equals(Common.all_but_last(the_dir)))
          return true;  
      if (command.equals("RNTO")) {
        if (privs.indexOf("(write)") >= 0)
          return true; 
        return false;
      } 
      return false;
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
      Log.log("ACCESS", 1, e);
      return false;
    } 
  }
  
  public String check_access_exception(String the_dir, String command, Properties item) throws Exception {
    String original_the_dir = the_dir;
    if (command.equals("RNTO"))
      the_dir = this.rnfr_file_path; 
    if (this.accessExceptions.get(the_dir) == null)
      return ""; 
    Properties master_item = (Properties)this.accessExceptions.get(the_dir);
    if (item != null && master_item.getProperty("modified", "-1").equals(item.getProperty("modified", "-2"))) {
      if (command.equals("RNTO")) {
        this.accessExceptions.remove(the_dir);
        this.accessExceptions.put(original_the_dir, master_item);
      } 
      return "(read)(rename)(delete)";
    } 
    return "";
  }
  
  public long get_quota(String the_dir) throws Exception {
    return get_quota(the_dir, this.uVFS, SG("parent_quota_dir"), this.quotaDelta, this);
  }
  
  public static long get_quota(String the_dir, VFS uVFS, String parentQuotaDir, Properties quotaDelta, ServerSession thisSession) throws Exception {
    try {
      Log.log("QUOTA", 3, "get_quota the_dir:" + the_dir + ", parentQuotaDir:" + parentQuotaDir + ", quotaDelta:" + quotaDelta);
      Properties item = uVFS.get_item(uVFS.getPrivPath(the_dir));
      Log.log("QUOTA", 3, "get_quota item:" + item);
      if (item.getProperty("privs", "").indexOf("(quota") >= 0) {
        long totalQuota = get_total_quota(the_dir, uVFS, quotaDelta);
        Log.log("QUOTA", 3, "get_quota totalQuota:" + totalQuota);
        if (item.getProperty("privs", "").indexOf("(real_quota)") >= 0) {
          Log.log("QUOTA", 3, "get_quota_used:" + get_quota_used(the_dir, uVFS, parentQuotaDir, thisSession));
          totalQuota -= get_quota_used(the_dir, uVFS, parentQuotaDir, thisSession);
          Log.log("QUOTA", 3, "get_quota_used totalQuota:" + totalQuota);
        } 
        return totalQuota;
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    return -12345L;
  }
  
  public long get_quota_used(String the_dir) throws Exception {
    return get_quota_used(the_dir, this.uVFS, SG("parent_quota_dir"), this);
  }
  
  public static long get_quota_used(String the_dir, VFS uVFS, String parentQuotaDir, ServerSession thisSession) throws Exception {
    try {
      Properties item = uVFS.get_item(uVFS.getPrivPath(the_dir));
      if (item.getProperty("privs", "").indexOf("(quota") >= 0) {
        if (item.getProperty("privs", "").indexOf("(real_quota)") >= 0) {
          String parentAddon = parentQuotaDir;
          if (parentAddon.equals("parent_quota_dir"))
            parentAddon = ""; 
          String real_path = String.valueOf((new VRL(item.getProperty("url"))).getCanonicalPath()) + "/" + parentAddon;
          long size = -12345L;
          if (VFS.quotaCache.containsKey(real_path.toUpperCase())) {
            Properties p = (Properties)VFS.quotaCache.get(real_path.toUpperCase());
            if (Long.parseLong(p.getProperty("time")) < (new Date()).getTime() - 300000L) {
              VFS.quotaCache.remove(real_path.toUpperCase());
            } else {
              size = Long.parseLong(p.getProperty("size"));
            } 
          } 
          if (size == -12345L) {
            while (VFS.activeQuotaChecks.size() > Integer.parseInt(System.getProperty("crushftp.quotathreads", "5")))
              Thread.sleep(100L); 
            while (VFS.activeQuotaChecks.indexOf(real_path) >= 0)
              Thread.sleep(100L); 
            if (VFS.quotaCache.containsKey(real_path.toUpperCase())) {
              Properties p = (Properties)VFS.quotaCache.get(real_path.toUpperCase());
              if (Long.parseLong(p.getProperty("time")) < (new Date()).getTime() - 300000L) {
                VFS.quotaCache.remove(real_path.toUpperCase());
              } else {
                size = Long.parseLong(p.getProperty("size"));
              } 
            } 
            if (size == -12345L)
              try {
                VFS.activeQuotaChecks.addElement(real_path);
                Properties qp = new Properties();
                qp.put("realPath", real_path);
                thisSession.runPlugin("getUsedQuota", qp);
                if (!qp.getProperty("usedQuota", "").equals(""))
                  size = Long.parseLong(qp.getProperty("usedQuota", "0")); 
                if (size == -12345L)
                  size = Common.recurseSize(real_path, 0L, thisSession); 
                Properties p = new Properties();
                p.put("time", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                p.put("size", (new StringBuffer(String.valueOf(size))).toString());
                VFS.quotaCache.put(real_path.toUpperCase(), p);
              } finally {
                VFS.activeQuotaChecks.remove(real_path);
              }  
          } 
          return size;
        } 
        return -12345L;
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    return -12345L;
  }
  
  public long get_total_quota(String the_dir) throws Exception {
    return get_total_quota(the_dir, this.uVFS, this.quotaDelta);
  }
  
  public static long get_total_quota(String the_dir, VFS uVFS, Properties quotaDelta) throws Exception {
    try {
      Properties item = uVFS.get_item(uVFS.getPrivPath(the_dir));
      if (item.getProperty("privs", "").indexOf("(quota") >= 0) {
        String data = item.getProperty("privs", "");
        data = data.substring(data.indexOf("(quota") + 6, data.indexOf(")", data.indexOf("(quota")));
        quotaDelta.put(the_dir, data);
        return Long.parseLong(data);
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    return -12345L;
  }
  
  public void set_quota(String the_dir, long quota_val) throws Exception {
    try {
      UserTools.loadPermissions(this.uVFS);
      Properties item = this.uVFS.get_item(this.uVFS.getPrivPath(the_dir));
      if (item.getProperty("privs", "").indexOf("(quota") >= 0 && item.getProperty("privs", "").indexOf("(real_quota)") < 0) {
        long originalQuota = Long.parseLong(this.quotaDelta.getProperty(the_dir));
        long quotaDiff = originalQuota - quota_val;
        String data = item.getProperty("privs", "");
        data = data.substring(data.indexOf("(quota") + 6, data.indexOf(")", data.indexOf("(quota")));
        data = Common.replace_str(item.getProperty("privs", ""), data, (new StringBuffer(String.valueOf(originalQuota - quotaDiff))).toString());
        item.put("privs", data);
        String privPath = this.uVFS.getPrivPath(the_dir);
        UserTools.addPriv(uiSG("listen_ip_port"), uiSG("user_name"), privPath, data, Integer.parseInt(this.uVFS.getPrivPath(the_dir, true, true)), this.uVFS);
        this.uVFS.reset();
        Properties p = new Properties();
        p.put("permissions", this.uVFS.getCombinedPermissions());
        runPlugin("quotaUpdate", p);
      } else if (item.getProperty("privs", "").indexOf("(quota") >= 0 && item.getProperty("privs", "").indexOf("(real_quota)") >= 0) {
        String data = item.getProperty("privs", "");
        data = data.substring(data.indexOf("(quota") + 6, data.indexOf(")", data.indexOf("(quota")));
        String parentAddon = SG("parent_quota_dir");
        if (parentAddon.equals("parent_quota_dir"))
          parentAddon = ""; 
        String real_path = String.valueOf((new VRL(item.getProperty("url"))).getCanonicalPath()) + "/" + parentAddon;
        if (VFS.quotaCache.containsKey(real_path.toUpperCase())) {
          Properties p = (Properties)VFS.quotaCache.get(real_path.toUpperCase());
          p.put("size", (new StringBuffer(String.valueOf(Long.parseLong(data) - quota_val))).toString());
        } 
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
  }
  
  public void add_log_formatted(String log_data, String check_data) {
    if (uiBG("dont_log"))
      return; 
    check_data = String.valueOf(check_data) + " ";
    if (!check_data.trim().equals("DIR_LIST") && !log_data.trim().startsWith("RETR END") && !log_data.trim().startsWith("STOR END")) {
      Properties p = new Properties();
      p.put("the_command", check_data.substring(0, check_data.indexOf(" ")));
      p.put("user_time", ServerStatus.thisObj.logDateFormat.format(new Date()));
      String command_data = uiSG("the_command_data");
      if (uiSG("the_command").toUpperCase().equals("PASS"))
        command_data = "**************"; 
      p.put("display", String.valueOf(p.getProperty("user_time")) + " | " + p.getProperty("the_command") + " " + command_data);
      p.put("stamp", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      uiVG("session_commands").addElement(p);
      uiPUT("last_logged_command", check_data.trim());
      while (uiVG("session_commands").size() > ServerStatus.IG("user_log_buffer"))
        uiVG("session_commands").removeElementAt(0); 
    } 
    if (check_data.trim().equals("DIR_LIST")) {
      add_log(log_data, check_data.trim());
    } else {
      add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("READ") + ": *" + log_data + "*", check_data.trim());
    } 
  }
  
  public void add_log(String log_data, String check_data) {
    add_log(log_data, check_data, check_data);
  }
  
  public void add_log(String log_data, String short_data, String check_data) {
    if (uiBG("dont_log"))
      return; 
    if (log_data.indexOf("WROTE: *220-") < 0 && log_data.indexOf("WROTE: *230-") < 0) {
      log_data = String.valueOf(log_data.trim()) + this.CRLF;
      BufferedReader lines = new BufferedReader(new StringReader(log_data));
      String data = "";
      try {
        while ((data = lines.readLine()) != null) {
          if (check_data.equals("DIR_LIST"))
            data = "[" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] WROTE: " + data; 
          if (check_data.equals("PROXY"))
            data = "[" + uiSG("user_number") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] : " + data; 
          ServerStatus.thisObj.append_log(String.valueOf(ServerStatus.thisObj.logDateFormat.format(new Date())) + "|" + data + this.CRLF, check_data);
          uiVG("user_log").addElement(String.valueOf(ServerStatus.thisObj.logDateFormat.format(new Date())) + "|" + data);
        } 
        while (uiVG("user_log").size() > ServerStatus.IG("user_log_buffer"))
          uiVG("user_log").removeElementAt(0); 
      } catch (IOException iOException) {}
    } 
  }
  
  public void do_event5(String type, Properties fileItem1) {
    do_event5(type, fileItem1, null);
  }
  
  public void do_event5(String type, Properties fileItem1, Properties fileItem2) {
    Properties originalUser = this.user;
    try {
      if (this.user == null && type.equalsIgnoreCase("ERROR"))
        this.user = UserTools.ut.getUser(uiSG("listen_ip_port"), uiSG("user_name"), true); 
      if (this.user == null)
        return; 
      Properties fileItem1_2 = null;
      if (fileItem1 != null)
        fileItem1_2 = (Properties)fileItem1.clone(); 
      Properties fileItem2_2 = null;
      if (fileItem2 != null)
        fileItem2_2 = (Properties)fileItem2.clone(); 
      ServerStatus.thisObj.events6.process(type, fileItem1_2, fileItem2_2, (Vector)this.user.get("events"), this);
      if (fileItem1_2 != null && fileItem1_2.containsKey("execute_log"))
        fileItem1.put("execute_log", fileItem1_2.get("execute_log")); 
      if (fileItem2_2 != null && fileItem2_2.containsKey("execute_log"))
        fileItem2.put("execute_log", fileItem2_2.get("execute_log")); 
    } finally {
      this.user = originalUser;
    } 
  }
  
  public Properties runPlugin(String action, Properties p) {
    Log.log("PLUGIN", 3, "PLUGIN:Calling " + action);
    if (p == null)
      p = new Properties(); 
    p.put("action", action);
    p.put("server_item", this.server_item);
    if (p.get("user") == null && this.user != null)
      p.put("user", this.user); 
    p.put("user_info", this.user_info);
    p.put("ServerSession", this);
    p.put("ServerSessionObject", this);
    p.put("server_settings", ServerStatus.server_settings);
    ServerStatus.thisObj.runPlugins(p, action.equals("login"));
    Log.log("PLUGIN", 3, "PLUGIN:Completed " + action);
    return p;
  }
  
  public void checkTempAccounts(Properties p) {
    try {
      if (!ServerStatus.thisObj.server_info.containsKey("knownBadTempAccounts"))
        ServerStatus.thisObj.server_info.put("knownBadTempAccounts", new Properties()); 
      Properties knownBadTempAccounts = (Properties)ServerStatus.thisObj.server_info.get("knownBadTempAccounts");
      synchronized (knownBadTempAccounts) {
        Enumeration keys = knownBadTempAccounts.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          if (System.currentTimeMillis() - Long.parseLong(knownBadTempAccounts.getProperty(key)) > 30000L)
            knownBadTempAccounts.remove(key); 
        } 
        if (knownBadTempAccounts.containsKey(p.getProperty("username")))
          return; 
      } 
      String tempAccountsPath = ServerStatus.SG("temp_accounts_path");
      File[] accounts = (new File(String.valueOf(tempAccountsPath) + "accounts/")).listFiles();
      boolean found = false;
      if (accounts != null) {
        for (int x = 0; !found && x < accounts.length; x++) {
          try {
            File f = accounts[x];
            if (f.getName().indexOf(",,") >= 0 && f.isDirectory()) {
              String[] tokens = f.getName().split(",,");
              Properties pp = new Properties();
              for (int xx = 0; xx < tokens.length; xx++) {
                boolean skip = false;
                String key = tokens[xx].substring(0, tokens[xx].indexOf("="));
                String val = tokens[xx].substring(tokens[xx].indexOf("=") + 1);
                if (key.equals("C")) {
                  key = val.split("=")[0];
                  if ((val.split("=")).length > 1) {
                    val = val.split("=")[1];
                  } else {
                    val = "";
                  } 
                  Vector v = (Vector)pp.get("web_customizations");
                  if (v == null)
                    v = new Vector(); 
                  Properties ppp = new Properties();
                  ppp.put("key", key);
                  ppp.put("value", val);
                  v.addElement(ppp);
                  pp.put("web_customizations", v);
                  skip = true;
                } 
                if (!skip)
                  pp.put(key.toUpperCase(), val); 
              } 
              if (ServerStatus.thisObj.common_code.check_date_expired_roll(pp.getProperty("EX"))) {
                Common.recurseDelete(String.valueOf(f.getCanonicalPath()) + "/../../storage/" + pp.getProperty("U") + pp.getProperty("P"), false);
                Common.recurseDelete(f.getCanonicalPath(), false);
              } else if (p.getProperty("username").equalsIgnoreCase(pp.getProperty("U")) && (p.getProperty("password").equalsIgnoreCase(pp.getProperty("P")) || p.getProperty("anyPass").equals("true"))) {
                Properties tempUser = UserTools.ut.getUser(uiSG("listen_ip_port"), pp.getProperty("T"), true);
                tempUser.put("username", p.getProperty("username"));
                tempUser.put("password", p.getProperty("password"));
                Properties u = (Properties)p.get("user");
                Properties info = (Properties)Common.readXMLObject(String.valueOf(f.getPath()) + "/INFO.XML");
                info.remove("command");
                info.remove("type");
                u.putAll(tempUser);
                u.putAll(pp);
                u.putAll(info);
                UserTools.mergeWebCustomizations(u, pp);
                UserTools.mergeWebCustomizations(u, info);
                p.remove("permissions");
                p.put("virtual", XMLUsers.buildVFSXML(String.valueOf(f.getPath()) + "/"));
                p.put("action", "success");
                p.put("overwrite_vfs", "false");
                p.put("overwrite_permissions", "false");
                found = true;
              } 
            } 
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
          } 
        } 
        if (!found)
          knownBadTempAccounts.put(p.getProperty("username"), (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()); 
      } 
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
    } 
  }
  
  public static SimpleDateFormat updateDateCustomizations(SimpleDateFormat date_time, Properties user) {
    Vector customizations = (Vector)user.get("web_customizations");
    if (customizations == null)
      customizations = new Vector(); 
    String date = "";
    String time = "";
    for (int x = 0; x < customizations.size(); x++) {
      Properties pp = customizations.elementAt(x);
      String key = pp.getProperty("key");
      if (key.equalsIgnoreCase("DATE_FORMAT_TEXT"))
        date = pp.getProperty("value"); 
      if (key.equalsIgnoreCase("TIME_FORMAT_TEXT"))
        time = pp.getProperty("value"); 
    } 
    if (date.length() != 0 || time.length() != 0)
      date_time = new SimpleDateFormat(String.valueOf(date) + " " + time); 
    return date_time;
  }
  
  public boolean verify_user(String theUser, String thePass) {
    return verify_user(theUser, thePass, false, true);
  }
  
  public boolean verify_user(String theUser, String thePass, boolean anyPass) {
    return verify_user(theUser, thePass, anyPass, true);
  }
  
  public boolean verify_user(String theUser, String thePass, boolean anyPass, boolean doAfterLogin) {
    if (theUser.startsWith("~")) {
      this.shareVFS = true;
      theUser = theUser.substring(1);
      uiPUT("user_name", theUser);
    } 
    if (theUser.toUpperCase().startsWith("$ASCII$")) {
      theUser = theUser.substring(7);
      uiPUT("user_name", theUser);
      uiPUT("proxy_ascii_binary", "ascii");
    } 
    if (UserTools.checkPassword(thePass))
      anyPass = true; 
    if (theUser.equalsIgnoreCase("default"))
      return false; 
    this.uVFS = null;
    Properties loginReason = new Properties();
    long time = Long.parseLong(ServerStatus.siPG("invalid_usernames").getProperty(theUser.toUpperCase(), "0"));
    if (time > 0L && time > (new Date()).getTime() - (ServerStatus.IG("invalid_usernames_seconds") * 1000))
      return false; 
    this.user = null;
    Properties u = new Properties();
    Properties temp_p = new Properties();
    temp_p.put("user", u);
    temp_p.put("username", theUser);
    temp_p.put("password", thePass);
    temp_p.put("anyPass", (new StringBuffer(String.valueOf(anyPass))).toString());
    if (theUser.equalsIgnoreCase("default"))
      return false; 
    checkTempAccounts(temp_p);
    if (!temp_p.getProperty("action", "").equalsIgnoreCase("success"))
      this.user = UserTools.ut.verify_user(ServerStatus.thisObj, theUser, thePass, uiSG("listen_ip_port"), uiIG("user_number"), uiSG("user_ip"), uiIG("user_port"), this.server_item, loginReason, anyPass); 
    if (this.user == null) {
      Properties p = temp_p;
      if (!p.getProperty("action", "").equalsIgnoreCase("success")) {
        Properties tempUser = UserTools.ut.verify_user(ServerStatus.thisObj, theUser, thePass, uiSG("listen_ip_port"), uiIG("user_number"), uiSG("user_ip"), uiIG("user_port"), this.server_item, loginReason, true);
        p.put("authenticationOnlyExists", (new StringBuffer(String.valueOf((tempUser != null)))).toString());
        p = runPlugin("login", p);
      } 
      synchronized (ServerStatus.thisObj) {
        if (p.getProperty("action", "").equalsIgnoreCase("success") || p.getProperty("dump_xml_user", "false").equals("true")) {
          theUser = p.getProperty("username", theUser);
          if (p.getProperty("authenticationOnly", "false").equalsIgnoreCase("true")) {
            Log.log("AUTH", 2, String.valueOf(LOC.G("Plugin authenticated user (not user manager):")) + theUser);
            this.user = UserTools.ut.verify_user(ServerStatus.thisObj, theUser, thePass, uiSG("listen_ip_port"), uiIG("user_number"), uiSG("user_ip"), uiIG("user_port"), this.server_item, loginReason, true);
            Vector extraLinkedVfs = (Vector)p.get("linked_vfs");
            if (extraLinkedVfs != null) {
              Vector linked_vfs = (Vector)this.user.get("linked_vfs");
              if (linked_vfs == null)
                linked_vfs = new Vector(); 
              this.user.put("linked_vfs", linked_vfs);
              linked_vfs.addAll(extraLinkedVfs);
            } 
          } else {
            try {
              loginReason.put("reason", "valid user");
              this.user = u;
              Properties virtual = UserTools.generateEmptyVirtual();
              if (!p.getProperty("templateUser", "").equals("")) {
                Vector extraLinkedVfs = (Vector)p.get("linked_vfs");
                Vector ichain = new Vector();
                ichain.addElement("default");
                int x;
                for (x = 0; x < (p.getProperty("templateUser", "").split(";")).length; x++)
                  ichain.addElement(p.getProperty("templateUser", "").split(";")[x].trim()); 
                if (extraLinkedVfs != null)
                  ichain.addAll(extraLinkedVfs); 
                for (x = 0; x < ichain.size(); x++) {
                  Properties tempUser = UserTools.ut.getUser(uiSG("listen_ip_port"), ichain.elementAt(x).toString(), ServerStatus.BG("resolve_inheritance"));
                  if (tempUser != null) {
                    UserTools.mergeWebCustomizations(this.user, tempUser);
                    Enumeration keys = tempUser.keys();
                    Log.log("AUTH", 1, String.valueOf(LOC.G("Setting templateUser's settings:")) + p.size());
                    while (keys.hasMoreElements()) {
                      String key = keys.nextElement().toString();
                      if (!key.equalsIgnoreCase("username") && !key.equalsIgnoreCase("user_name") && !key.equalsIgnoreCase("password")) {
                        if ((key.equals("max_logins") && tempUser.get(key).equals("-1")) || (
                          key.equals("email") && tempUser.getProperty(key, "").equals("")) || (
                          key.equals("first_name") && tempUser.getProperty(key, "").equals("")) || (
                          key.equals("last_name") && tempUser.getProperty(key, "").equals("")) || (
                          key.equals("account_expire") && tempUser.getProperty(key, "").equals("")))
                          continue; 
                        try {
                          this.user.put(key, tempUser.get(key));
                        } catch (Exception exception) {}
                      } 
                    } 
                  } 
                } 
                if (extraLinkedVfs != null) {
                  Vector linked_vfs = (Vector)this.user.get("linked_vfs");
                  if (linked_vfs == null)
                    linked_vfs = new Vector(); 
                  this.user.put("linked_vfs", linked_vfs);
                  linked_vfs.addAll(extraLinkedVfs);
                } 
                Properties virtual2 = null;
                for (int i = 0; i < (p.getProperty("templateUser", "").split(";")).length; i++) {
                  VFS tempVFS = UserTools.ut.getVFS(uiSG("listen_ip_port"), p.getProperty("templateUser", "").split(";")[i].trim());
                  if (virtual2 == null) {
                    virtual2 = tempVFS.homes.elementAt(0);
                  } else {
                    virtual2.putAll(tempVFS.homes.elementAt(0));
                  } 
                  try {
                    Properties permissions = (Properties)p.get("permissions");
                    Vector v = (Vector)virtual2.get("vfs_permissions_object");
                    Properties permissions2 = v.elementAt(0);
                    if (permissions2.containsKey("/") && permissions.containsKey("/") && permissions != permissions2)
                      permissions2.remove("/"); 
                    permissions.putAll(permissions2);
                    permissions2.putAll(permissions);
                  } catch (Exception e) {
                    Log.log("AUTH", 1, e);
                  } 
                } 
                virtual = virtual2;
                this.user.put("root_dir", "/");
                Log.log("SERVER", 3, "Dump of user properties form plugin:" + this.user);
              } 
              if (p.containsKey("virtual")) {
                virtual = (Properties)p.get("virtual");
              } else {
                Vector VFSItems = (Vector)p.get("VFSItems");
                for (int x = 0; x < VFSItems.size(); x++) {
                  Properties pp = VFSItems.elementAt(x);
                  String path2 = String.valueOf(pp.getProperty("dir")) + pp.getProperty("name");
                  if (path2.endsWith("/"))
                    path2 = path2.substring(0, path2.length() - 1); 
                  Properties vItem = new Properties();
                  vItem.put("name", pp.getProperty("name"));
                  vItem.put("type", pp.getProperty("type", "FILE"));
                  vItem.put("virtualPath", path2);
                  vItem.put("vItems", pp.get("data"));
                  virtual.put(path2, vItem);
                } 
                if (p.getProperty("overwrite_permissions", "true").equals("true")) {
                  Properties permissions = (Properties)p.get("permissions");
                  Vector v = (Vector)virtual.get("vfs_permissions_object");
                  Properties permissions2 = v.elementAt(0);
                  if (permissions2.containsKey("/") && permissions.containsKey("/") && permissions != permissions2)
                    permissions2.remove("/"); 
                  permissions.putAll(permissions2);
                  permissions2.putAll(permissions);
                } 
              } 
              this.uVFS = VFS.getVFS(virtual);
            } catch (Exception e) {
              Log.log("AUTH", 1, e);
            } 
          } 
          if (p.getProperty("dump_xml_user", "false").equals("true")) {
            this.user.remove("username");
            this.user.remove("userName");
            this.user.remove("userpass");
            this.user.remove("userPass");
            this.user.remove("virtualUser");
            this.user.remove("id");
            this.user.remove("SQL_ID");
            this.user.put("root_dir", "/");
            this.user.remove("real_path_to_user");
            this.user.remove("vfs_modified");
            this.user.remove("x_lastName");
            this.user.remove("admin_group_name");
            this.user.remove("user_name");
            this.user.remove("defaultsVersion");
            UserTools.stripUser(this.user, UserTools.ut.getUser(uiSG("listen_ip_port"), "default", false));
            UserTools.writeUser(uiSG("listen_ip_port"), theUser, this.user);
            UserTools.writeVFS(uiSG("listen_ip_port"), theUser, this.uVFS);
            return false;
          } 
        } else if (!p.getProperty("redirect_url", "").equals("")) {
          this.user_info.put("redirect_url", p.getProperty("redirect_url"));
        } 
      } 
    } 
    Log.log("AUTH", 3, "Loggining in...");
    if (this.user != null && this.uVFS == null && this.user.getProperty("virtualUser", "false").equalsIgnoreCase("false")) {
      this.uVFS = UserTools.ut.getVFS(uiSG("listen_ip_port"), this.user.getProperty("username"));
      Log.log("AUTH", 2, String.valueOf(LOC.G("Got VFS from real user:")) + this.uVFS);
    } 
    if (this.user != null) {
      Properties p = new Properties();
      p.put("user", this.user);
      if (!this.user.getProperty("username", "").equals("template"))
        theUser = this.user.getProperty("username", theUser); 
      p.put("username", theUser);
      p.put("password", thePass);
      p.put("allowLogin", "true");
      if (this.uVFS != null)
        p.put("uVFSObject", this.uVFS); 
      if (doAfterLogin)
        runPlugin("afterLogin", p); 
      if (!p.getProperty("allowLogin", "true").equals("true")) {
        this.user = null;
        this.uVFS = null;
        add_log(LOC.G("A plugin rejected the login. Login failed."), "USER");
        return false;
      } 
      Log.log("AUTH", 3, "After login...");
      UserTools.setupVFSLinking(uiSG("listen_ip_port"), theUser, this.uVFS, this.user);
      this.uVFS.setUserPassIpPortProtocol(uiSG("user_name"), uiSG("current_password"), uiSG("user_ip"), uiIG("user_port"), uiSG("user_protocol"), this.user_info, this.user, this);
      if (ServerStatus.BG("track_user_md4_hashes") && !thePass.startsWith("NTLM:")) {
        String md4_user = ServerStatus.thisObj.common_code.encode_pass(theUser, "MD4").substring("MD4:".length());
        String md4_pass = ServerStatus.thisObj.common_code.encode_pass(thePass, "MD4").substring("MD4:".length());
        Properties md4_hashes = (Properties)ServerStatus.thisObj.server_info.get("md4_hashes");
        if (md4_hashes == null)
          md4_hashes = new Properties(); 
        ServerStatus.thisObj.server_info.put("md4_hashes", md4_hashes);
        if (!md4_hashes.getProperty(md4_user, "").equals(md4_pass)) {
          md4_hashes.put(md4_user, md4_pass);
          synchronized (md4_hashes) {
            ObjectOutputStream oos = null;
            try {
              (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes2.obj")).delete();
              oos = new ObjectOutputStream(new FileOutputStream(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes2.obj"));
              oos.writeObject(md4_hashes);
              oos.flush();
              oos.close();
              oos = null;
              (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes.obj")).delete();
              (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes2.obj")).renameTo(new File(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes.obj"));
            } catch (Exception e) {
              Log.log("SERVER", 0, e);
            } finally {
              try {
                if (oos != null)
                  oos.close(); 
              } catch (Exception exception) {}
            } 
          } 
        } 
      } 
    } 
    uiPUT("current_dir", SG("root_dir"));
    if (this.user != null && this.uVFS != null && this.user.getProperty("username", "").equalsIgnoreCase("TEMPLATE")) {
      Vector listing = new Vector();
      uiPUT("user_name", uiSG("user_name").replace(':', ';'));
      this.uVFS.setUserPassIpPortProtocol(uiSG("user_name"), uiSG("current_password"), uiSG("user_ip"), uiIG("user_port"), uiSG("user_protocol"), this.user_info, this.user, this);
      if ((uiSG("user_name").equalsIgnoreCase("anonymous") || uiSG("user_name").trim().equals("")) && ServerStatus.BG("ignore_web_anonymous_proxy"))
        return false; 
      boolean newVFS = true;
      if (this.shareVFS)
        newVFS = doShareVFS(false); 
      try {
        Properties session = (Properties)SharedSession.find("crushftp.sessions").get(getId());
        if (session == null) {
          session = new Properties();
          SharedSession.find("crushftp.sessions").put(getId(), session);
          SharedSessionReplicated.flushWait();
        } 
        this.uVFS.getListing(listing, "/");
        if (listing.size() > 0) {
          Properties p = listing.elementAt(0);
          if (p.getProperty("type").equalsIgnoreCase("DIR")) {
            p = this.uVFS.get_item(String.valueOf(p.getProperty("root_dir")) + p.getProperty("name") + "/");
            GenericClient c = this.uVFS.getClient(p);
            try {
              if (!uiBG("skip_proxy_check")) {
                uiPUT("skip_proxy_check", "false");
                String userMessage = c.getConfig("userMessage", null);
                this.user.remove("welcome_message2");
                if (userMessage != null) {
                  String[] lines = userMessage.split("\\r\\n");
                  userMessage = "";
                  for (int x = 0; x < lines.length - 1; x++) {
                    if (lines[x].startsWith("230-user.")) {
                      String param = lines[x].substring("230-user.".length()).trim();
                      this.user.put(param.split("=")[0], param.split("=")[1]);
                    } else if (lines[x].startsWith("230-user_info.")) {
                      String param = lines[x].substring("230-user_info.".length()).trim();
                      this.user_info.put(param.split("=")[0], param.split("=")[1]);
                    } else if (lines[x].startsWith("230-")) {
                      userMessage = String.valueOf(userMessage) + lines[x].substring(4) + "\r\n";
                      if (lines[x].substring(4).startsWith("PASSWORD EXPIRATION:")) {
                        String expireDate = lines[x].substring(lines[x].indexOf(":") + 1).trim();
                        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                        SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
                        session.put("expire_password_when", sdf2.format(sdf1.parse(expireDate)));
                      } 
                    } else {
                      userMessage = String.valueOf(userMessage) + lines[x] + "\r\n";
                    } 
                  } 
                  if (!userMessage.equals(""))
                    this.user.put("welcome_message2", userMessage.trim()); 
                } 
                if (c.getConfig("default_dir", "").indexOf("/") >= 0) {
                  String defaultDir = c.getConfig("default_dir", "/");
                  if (!this.server_item.getProperty("root_directory", "/").equals("/"))
                    uiPUT("default_current_dir", this.server_item.getProperty("root_directory", "/")); 
                  if (!defaultDir.equals("/"))
                    session.put("default_current_dir", defaultDir); 
                  if (c.getConfig("default_pwd", "").indexOf("(unlocked)") >= 0)
                    session.put("default_current_dir_unlocked", "true"); 
                } 
              } 
              if (session.containsKey("default_current_dir"))
                uiPUT("default_current_dir", session.getProperty("default_current_dir")); 
            } finally {
              c = this.uVFS.releaseClient(c);
            } 
            if (Common.System2.get("crushftp.dmz.queue") != null) {
              Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
              Properties action = new Properties();
              action.put("type", "GET:USER");
              action.put("id", Common.makeBoundary());
              action.put("username", theUser);
              action.put("need_response", "true");
              queue.addElement(action);
              action = UserTools.waitResponse(action, 30);
              if (action != null && action.containsKey("user")) {
                this.user = (Properties)action.get("user");
                Vector homes = (Vector)action.get("vfs");
                Properties permission = this.uVFS.getPermission0();
                for (int x = homes.size() - 1; x >= 0; x--) {
                  Properties tempVFS = homes.elementAt(x);
                  Vector tempPermissionHomes = (Vector)tempVFS.get("vfs_permissions_object");
                  Properties tempPermission = tempPermissionHomes.elementAt(0);
                  Enumeration keys = tempPermission.keys();
                  while (keys.hasMoreElements()) {
                    String key = keys.nextElement().toString();
                    String newKey = key;
                    if (newKey.indexOf("/", 1) > 0) {
                      if (tempVFS.size() == 3)
                        newKey = newKey.substring(newKey.indexOf("/", 1)); 
                      permission.put("/" + p.getProperty("name").toUpperCase() + newKey, tempPermission.getProperty(key));
                    } 
                  } 
                } 
              } 
            } 
            if (ServerStatus.BG("learning_proxy")) {
              Properties temp_user = new Properties();
              temp_user.put("username", theUser);
              temp_user.put("password", ServerStatus.thisObj.common_code.encode_pass(thePass, ServerStatus.SG("password_encryption")));
              temp_user.put("root_dir", "/");
              temp_user.put("userVersion", "6");
              temp_user.put("version", "1.0");
              temp_user.put("max_logins", "0");
              UserTools.writeUser(String.valueOf(uiSG("listen_ip_port")) + "_learning", theUser, temp_user);
            } 
          } 
        } 
      } catch (Exception e) {
        Log.log("AUTH", 2, e);
        this.user_info.put("lastProxyError", (new StringBuffer(String.valueOf(e.getMessage()))).toString());
        String[] hack_users = SG("hack_usernames").split(",");
        for (int x = 0; x < hack_users.length; x++) {
          if (!theUser.trim().equals("") && theUser.trim().equalsIgnoreCase(hack_users[x].trim())) {
            ServerStatus.thisObj.ban(this.user_info, ServerStatus.IG("hban_timeout"));
            ServerStatus.thisObj.kick(this.user_info);
            break;
          } 
        } 
        doErrorEvent(e);
        return false;
      } 
    } 
    if (BG("expire_password") || SG("expire_password_when").equals("01/01/1978 12:00:00 AM"))
      try {
        String s = SG("expire_password_when");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
        Date d = null;
        try {
          d = sdf.parse(s);
        } catch (ParseException e) {
          sdf = new SimpleDateFormat("MM/dd/yyyy");
          d = sdf.parse(s);
        } 
        uiPUT("password_expired", "false");
        if ((new Date()).getTime() > d.getTime()) {
          uiPUT("password_expired", "true");
          if (!uiSG("user_protocol").equalsIgnoreCase("SFTP")) {
            String fname = "expired.html";
            String buildPrivs = "(read)(view)";
            Properties permissions = new Properties();
            permissions.put("/", buildPrivs);
            Properties dir_item = new Properties();
            dir_item.put("url", (new File(String.valueOf(System.getProperty("crushftp.web", "")) + "WebInterface/" + fname)).toURI().toURL().toExternalForm());
            dir_item.put("type", "file");
            Vector v = new Vector();
            v.addElement(dir_item);
            Properties virtual = UserTools.generateEmptyVirtual();
            String path = "/" + fname;
            if (path.endsWith("/"))
              path = path.substring(0, path.length() - 1); 
            Properties vItem = new Properties();
            vItem.put("virtualPath", path);
            vItem.put("name", fname);
            vItem.put("type", "FILE");
            vItem.put("vItems", v);
            virtual.put(path, vItem);
            vItem = new Properties();
            vItem.put("name", "VFS");
            vItem.put("type", "DIR");
            vItem.put("virtualPath", "/");
            virtual.put("/", vItem);
            this.expired_uVFS = this.uVFS;
            this.uVFS = VFS.getVFS(virtual);
            Properties tempUser = UserTools.ut.getUser(uiSG("listen_ip_port"), theUser, false);
            tempUser.put("auto_set_pass", "true");
            if (Common.System2.get("crushftp.dmz.queue") == null)
              UserTools.writeUser(uiSG("listen_ip_port"), theUser, tempUser); 
          } 
        } 
      } catch (Exception e) {
        Log.log("AUTH", 2, e);
      }  
    if (loginReason.getProperty("changedPassword", "").equals("true")) {
      loginReason.remove("changedPassword");
      ServerStatus.thisObj.runAlerts("password_change", this);
    } 
    if (this.user == null) {
      if (loginReason.getProperty("reason", "").equals(""))
        ServerStatus.siPG("invalid_usernames").put(theUser.toUpperCase(), (new StringBuffer(String.valueOf((new Date()).getTime()))).toString()); 
      String[] hack_users = SG("hack_usernames").split(",");
      for (int x = 0; x < hack_users.length; x++) {
        if (theUser.trim().equalsIgnoreCase(hack_users[x].trim())) {
          ServerStatus.thisObj.ban(this.user_info, ServerStatus.IG("hban_timeout"));
          ServerStatus.thisObj.kick(this.user_info);
          break;
        } 
      } 
      return false;
    } 
    try {
      this.is = new BufferedReader(new InputStreamReader(this.sock.getInputStream(), SG("char_encoding")));
    } catch (Exception exception) {}
    try {
      this.os = this.sock.getOutputStream();
    } catch (Exception exception) {}
    Log.log("AUTH", 3, LOC.G("Login complete."));
    return true;
  }
  
  public void doErrorEvent(Exception e) {
    Properties error_info = new Properties();
    error_info.put("the_command", uiSG("the_command"));
    error_info.put("the_command_data", uiSG("the_command_data"));
    error_info.put("url", e.toString());
    error_info.put("the_file_status", "FAILED");
    error_info.put("the_file_error", e.toString());
    error_info.put("the_file_name", e.toString());
    error_info.put("the_file_path", e.toString());
    error_info.put("the_file_start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    error_info.put("the_file_end", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    error_info.put("the_file_speed", "0");
    error_info.put("the_file_size", "0");
    error_info.put("the_file_resume_loc", "0");
    error_info.put("the_file_md5", "");
    error_info.put("modified", "0");
    do_event5("ERROR", error_info);
  }
  
  public boolean doShareVFS(boolean reset) {
    String id = getId();
    synchronized (SharedSession.sessionLock) {
      if (reset)
        SharedSession.find("crushftp.sessions").remove(id); 
      Properties session = (Properties)SharedSession.find("crushftp.sessions").get(id);
      if (session == null) {
        session = new Properties();
        SharedSession.find("crushftp.sessions").put(id, session);
        SharedSessionReplicated.flushWait();
      } 
      if (session.get("sharedVFS") == null) {
        session.put("sharedVFS", this.uVFS);
        return true;
      } 
      VFS sharedVFS = (VFS)session.get("sharedVFS");
      VFS.doCopyVFS(sharedVFS, this.uVFS);
      return false;
    } 
  }
  
  public String getId() {
    if ((uiSG("user_protocol").startsWith("HTTP") || uiSG("user_protocol_proxy").startsWith("HTTP")) && uiSG("CrushAuth").length() > 30)
      return uiSG("CrushAuth"); 
    if (ServerStatus.BG("relaxed_event_grouping"))
      return String.valueOf(uiSG("user_name")) + uiSG("user_ip") + uiSG("user_protocol"); 
    return String.valueOf(uiSG("user_name")) + uiSG("user_ip") + uiIG("user_port") + uiSG("user_protocol");
  }
  
  public boolean login_user_pass() throws Exception {
    return login_user_pass(false, true);
  }
  
  public boolean login_user_pass(boolean anyPass) throws Exception {
    return login_user_pass(anyPass, true);
  }
  
  public boolean login_user_pass(boolean anyPass, boolean doAfterLogin) throws Exception {
    if (uiSG("user_name").length() > 2000 || uiSG("current_password").length() > 500) {
      this.not_done = write_command("550", "Invalid");
      doErrorEvent(new Exception(uiSG("lastLog")));
      return false;
    } 
    Log.log("AUTH", 3, new Exception(String.valueOf(LOC.G("INFO:Logging in with user:")) + uiSG("user_name")));
    uiPUT("last_logged_command", "USER");
    boolean stripped_char = false;
    if (ServerStatus.BG("lowercase_usernames"))
      uiPUT("user_name", uiSG("user_name").toLowerCase()); 
    if (uiSG("user_name").startsWith("!")) {
      uiPUT("user_name", uiSG("user_name").substring(1));
      stripped_char = true;
    } 
    if (this.user_info.getProperty("user_name_original", "").equals("") || this.user_info.getProperty("user_name_original", "").equalsIgnoreCase("anonymous"))
      uiPUT("user_name_original", uiSG("user_name")); 
    if (this.server_item.getProperty("linkedServer", "").equals("@AutoDomain"))
      if (uiSG("user_name_original").indexOf("@") > 0) {
        String newLinkedServer = uiSG("user_name_original").split("@")[(uiSG("user_name_original").split("@")).length - 1];
        String newLinkedServer2 = Common.dots(newLinkedServer);
        newLinkedServer2 = newLinkedServer2.replace('/', '-').replace('\\', '-').replace('%', '-').replace(':', '-').replace(';', '-');
        if (newLinkedServer.equals(newLinkedServer2)) {
          uiPUT("user_name", uiSG("user_name_original").substring(0, uiSG("user_name_original").lastIndexOf("@")));
          uiPUT("listen_ip_port", newLinkedServer);
        } 
      }  
    if (verify_user(uiSG("user_name"), uiSG("current_password"), anyPass, doAfterLogin)) {
      this.uVFS.setUserPassIpPortProtocol(uiSG("user_name"), uiSG("current_password"), uiSG("user_ip"), uiIG("user_port"), uiSG("user_protocol"), this.user_info, this.user, this);
      Log.log("AUTH", 2, LOC.G("User $0 authenticated, VFS set to:$1", uiSG("user_name"), this.uVFS.toString()));
      if (ServerStatus.BG("create_home_folder"))
        try {
          Vector v = new Vector();
          this.uVFS.getListing(v, "/");
          for (int xx = 0; xx < v.size(); xx++) {
            Properties p = v.elementAt(xx);
            v.setElementAt(this.uVFS.get_item(String.valueOf(p.getProperty("root_dir")) + p.getProperty("name") + "/"), xx);
          } 
          for (int i = 0; i < v.size(); i++) {
            Properties p = v.elementAt(i);
            if (p.getProperty("url").endsWith("/") && p.getProperty("url").toUpperCase().startsWith("FILE:/")) {
              Common.verifyOSXVolumeMounted(p.getProperty("url"));
              if (!(new File((new VRL(p.getProperty("url"))).getPath())).exists())
                (new File((new VRL(p.getProperty("url"))).getPath())).mkdirs(); 
            } 
          } 
        } catch (Exception e) {
          Log.log("USER", 1, e);
        }  
      setupRootDir(null, false);
      Properties session = (Properties)SharedSession.find("crushftp.sessions").get(getId());
      if (session == null) {
        session = new Properties();
        SharedSession.find("crushftp.sessions").put(getId(), session);
        SharedSessionReplicated.flushWait();
      } 
      if (ServerStatus.BG("jailproxy") && session.getProperty("default_current_dir_unlocked", "false").equals("false"))
        uiPUT("current_dir", SG("root_dir")); 
      if (this.user.get("ip_list") != null) {
        String ips = String.valueOf(this.user.getProperty("ip_list").trim()) + this.CRLF;
        ips = Common.replace_str(ips, "\r", "~");
        StringTokenizer get_em = new StringTokenizer(ips, "~");
        int num_to_do = get_em.countTokens();
        Vector ip_list = new Vector();
        try {
          for (int i = 0; i < num_to_do; i++) {
            String ip_str = get_em.nextToken().trim();
            Properties ip_data = new Properties();
            ip_data.put("type", (new StringBuffer(String.valueOf(ip_str.charAt(0)))).toString());
            ip_data.put("start_ip", ip_str.substring(1, ip_str.indexOf(",")));
            ip_data.put("stop_ip", ip_str.substring(ip_str.indexOf(",") + 1));
            ip_list.addElement(ip_data);
          } 
        } catch (Exception e) {
          if (e.indexOf("Interrupted") >= 0)
            throw e; 
        } 
        this.user.put("ip_restrictions", ip_list);
        this.user.remove("ip_list");
      } 
      boolean auto_kicked = false;
      Vector allowedHours = new Vector();
      if (SG("hours_of_day").equals("") || SG("hours_of_day").equals("hours_of_day"))
        this.user.put("hours_of_day", "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23"); 
      if (this.user.get("allowed_protocols") == null || SG("allowed_protocols").equals("allowed_protocols"))
        this.user.put("allowed_protocols", ",ftp:0,ftps:0,sftp:0,http:0,https:0,webdav:0,"); 
      String[] hours = SG("hours_of_day").split(",");
      for (int x = 0; x < hours.length; x++) {
        try {
          allowedHours.addElement((new StringBuffer(String.valueOf(Integer.parseInt(hours[x])))).toString());
        } catch (Exception e) {
          Log.log("AUTH", 1, e);
        } 
      } 
      if (IG("max_logins_ip") != 0 && BG("logins_ip_auto_kick") && ServerStatus.count_users_ip(this, null) > IG("max_logins_ip")) {
        auto_kicked = ServerStatus.thisObj.kill_first_same_name_same_ip(this.user_info);
        Thread.sleep(5000L);
        verify_user(uiSG("user_name"), uiSG("current_password"), false, doAfterLogin);
      } 
      if (stripped_char)
        stripped_char = ServerStatus.thisObj.kill_same_name_same_ip(this.user_info, true); 
      if (IG("max_logins") < 0) {
        this.not_done = write_command("421", String.valueOf(LOC.G("%account_disabled%")) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (ServerStatus.siIG("concurrent_users") >= ServerStatus.IG("max_users") + 1 && !BG("ignore_max_logins")) {
        this.not_done = write_command("421", String.valueOf(LOC.G("%max_users_server%")) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (ServerStatus.siIG("concurrent_users") >= ServerStatus.IG("max_max_users") + 1) {
        this.not_done = write_command("421", String.valueOf(LOC.G("%max_max_users_server%")) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (Integer.parseInt(this.server_item.getProperty("connected_users")) > Integer.parseInt(this.server_item.getProperty("max_connected_users", "32768"))) {
        this.not_done = write_command("421", String.valueOf(LOC.G("%max_users_server%")) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (!ServerStatus.thisObj.common_code.check_ip((Vector)this.user.get("ip_restrictions"), uiSG("user_ip"))) {
        this.not_done = write_command("550", LOC.G("%bad_ip%"));
        uiPUT("user_logged_in", "false");
      } else if (!Common.check_day_of_week(ServerStatus.SG("day_of_week_allow"), new Date())) {
        this.not_done = write_command("530", String.valueOf(LOC.G("%day_restricted%")) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (Common.check_protocol(uiSG("user_protocol"), SG("allowed_protocols")) < 0) {
        this.not_done = write_command("530", String.valueOf(LOC.G("This user is not allowed to use this protocol.")) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (ServerStatus.count_users_ip(this, uiSG("user_protocol")) > Common.check_protocol(uiSG("user_protocol"), SG("allowed_protocols"))) {
        this.not_done = write_command("421", String.valueOf(LOC.G("%max_simultaneous_connections_ip%")) + " " + LOC.G("(For this protocol.)") + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (!Common.check_day_of_week(SG("day_of_week_allow"), new Date())) {
        this.not_done = write_command("530", String.valueOf(LOC.G("%user_day_restricted%")) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (allowedHours.indexOf((new StringBuffer(String.valueOf(Integer.parseInt(this.hh.format(new Date()))))).toString()) < 0) {
        this.not_done = write_command("530", String.valueOf(LOC.G("Not allowed to login at the present hour ($0), try later.", (new StringBuffer(String.valueOf(Integer.parseInt(this.hh.format(new Date()))))).toString())) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (IG("max_logins_ip") != 0 && ServerStatus.count_users_ip(this, null) > IG("max_logins_ip") && !auto_kicked && !stripped_char) {
        this.not_done = write_command("421", String.valueOf(LOC.G("%max_simultaneous_connections_ip%")) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (IG("max_logins") != 0 && ServerStatus.thisObj.count_users(this) > IG("max_logins") && !stripped_char) {
        this.not_done = write_command("421", String.valueOf(LOC.G("%max_simultaneous_connections%")) + this.CRLF + LOC.G("Control connection closed") + ".");
        uiPUT("user_logged_in", "false");
      } else if (ServerStatus.thisObj.common_code.check_date_expired_roll(SG("account_expire"))) {
        if (BG("account_expire_delete")) {
          try {
            UserTools.deleteUser(uiSG("listen_ip_port"), uiSG("user_name"));
          } catch (NullPointerException nullPointerException) {}
          this.not_done = write_command("530", LOC.G("%account_expired_deleted%"));
          uiVG("failed_commands").addElement((new Date()).getTime());
        } else {
          this.not_done = write_command("530", LOC.G("%account_expired%"));
          uiVG("failed_commands").addElement((new Date()).getTime());
        } 
        uiPUT("user_logged_in", "false");
      } else {
        if (SG("account_expire") != null && !SG("account_expire").equals("") && !SG("account_expire").equals("0") && !SG("account_expire_rolling_days").equals("") && IG("account_expire_rolling_days") > 0) {
          GregorianCalendar gc = new GregorianCalendar();
          gc.setTime(new Date());
          gc.add(5, IG("account_expire_rolling_days"));
          SimpleDateFormat sdf = null;
          if (SG("account_expire").indexOf("/") >= 0) {
            sdf = new SimpleDateFormat("MM/dd/yy hh:mm aa");
          } else {
            sdf = new SimpleDateFormat("MMddyyyyHHmm");
          } 
          try {
            if (sdf.parse(SG("account_expire")).getTime() < gc.getTime().getTime()) {
              this.user.put("account_expire", sdf.format(gc.getTime()));
              UserTools.ut.put_in_user(uiSG("listen_ip_port"), uiSG("user_name"), "account_expire", sdf.format(gc.getTime()));
              UserTools.ut.put_in_user(uiSG("listen_ip_port"), uiSG("user_name"), "account_expire_rolling_days", (new StringBuffer(String.valueOf(IG("account_expire_rolling_days")))).toString());
            } 
          } catch (Exception e) {
            return true;
          } 
        } 
        try {
          if (this.sock != null) {
            int priorTimeout = this.sock.getSoTimeout() / 60000;
            this.sock.setSoTimeout(((IG("max_idle_time") == 0) ? priorTimeout : IG("max_idle_time")) * 1000 * 60);
          } 
        } catch (SocketException socketException) {}
        String login_message = "";
        if (auto_kicked)
          login_message = String.valueOf(login_message) + LOC.G("First user with same name, same IP, was autokicked.") + this.CRLF; 
        if (stripped_char)
          login_message = String.valueOf(login_message) + LOC.G("Previous sessions were kicked.") + this.CRLF; 
        ServerStatus.thisObj.set_user_pointer(this.user_info);
        try {
          if (!uiBG("dont_write")) {
            String msg2 = this.server_item.getProperty("user_welcome_message", "");
            if (!this.user.getProperty("welcome_message2", "").equals(""))
              msg2 = this.user.getProperty("welcome_message2", ""); 
            login_message = String.valueOf(login_message) + msg2 + this.CRLF;
            String welcome_msg = ServerStatus.thisObj.change_vars_to_values(SG("welcome_message"), this).trim();
            if (welcome_msg.equals("welcome_msg"))
              welcome_msg = ""; 
            if (welcome_msg.length() > 0)
              welcome_msg = String.valueOf(welcome_msg) + this.CRLF; 
            this.user.put("user_name", uiSG("user_name"));
            login_message = String.valueOf(login_message.trim()) + this.CRLF + welcome_msg + "%PASS% logged in";
            this.not_done = write_command("230", login_message);
          } 
        } catch (Exception e) {
          if (e.indexOf("Interrupted") >= 0)
            throw e; 
        } 
        SimpleDateFormat date_time = updateDateCustomizations(ServerStatus.thisObj.logDateFormat, this.user);
        uiPUT("login_date_formatted", date_time.format(new Date()));
        uiPUT("user_logged_in", "true");
        uiPUT("sharedId", getId());
        if (BG("ratio_field_permanent")) {
          uiPUT("ratio_bytes_sent", (new StringBuffer(String.valueOf(IG("user_bytes_sent")))).toString());
          uiPUT("ratio_bytes_received", (new StringBuffer(String.valueOf(IG("user_bytes_received")))).toString());
        } 
        if (IG("max_login_time") != 0)
          Worker.startWorker(new null.Killer(this), String.valueOf(Thread.currentThread().getName()) + " (max_time)"); 
      } 
      if (uiBG("user_logged_in") && doAfterLogin) {
        ServerStatus.put_in("successful_logins", ServerStatus.IG("successful_logins") + 1);
      } else if (doAfterLogin) {
        ServerStatus.put_in("failed_logins", ServerStatus.IG("failed_logins") + 1);
        if (uiVG("failed_commands").size() - 10 > 0)
          Thread.sleep((1000 * (uiVG("failed_commands").size() - 10))); 
      } 
    } else {
      if (!this.user_info.getProperty("lastProxyError", "").equals("")) {
        if (ServerStatus.BG("rfc_proxy")) {
          this.not_done = write_command_raw(this.user_info.getProperty("lastProxyError", ""));
        } else {
          this.not_done = write_command("530", this.user_info.getProperty("lastProxyError", ""));
        } 
      } else if (this.server_item.getProperty("serverType", "ftp").toUpperCase().startsWith("FTP") || (!uiSG("user_name").equals("") && !uiSG("user_name").equalsIgnoreCase("anonymous"))) {
        this.not_done = write_command("530", "%PASS-bad%");
      } 
      uiVG("failed_commands").addElement((new Date()).getTime());
      uiPUT("user_logged_in", "false");
      ServerStatus.put_in("failed_logins", ServerStatus.IG("failed_logins") + 1);
      uiPUT("user_logged_in", "false");
      if (uiVG("failed_commands").size() + uiVG("password_attempts").size() - 10 > 0)
        Thread.sleep((1000 * (uiVG("failed_commands").size() + uiVG("password_attempts").size() - 10))); 
    } 
    uiPUT("stat", new Properties());
    if (!uiBG("skip_proxy_check"))
      uiPUT("stat", ServerStatus.thisObj.statTools.add_login_stat(this.server_item, uiSG("user_name"), uiSG("user_ip"), uiBG("user_logged_in"), this)); 
    if (uiBG("user_logged_in"))
      return true; 
    if (doAfterLogin) {
      uiVG("password_attempts").addElement((new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      ServerStatus.put_in("failed_logins", ServerStatus.IG("failed_logins") + 1);
      if (uiVG("failed_commands").size() + uiVG("password_attempts").size() - 10 > 0)
        Thread.sleep((1000 * (uiVG("failed_commands").size() + uiVG("password_attempts").size() - 10))); 
    } 
    doErrorEvent(new Exception(uiSG("lastLog")));
    return false;
  }
  
  public void do_Recycle(VRL vrl) throws Exception {
    File v = new File(vrl.getCanonicalPath());
    String recycle = ServerStatus.SG("recycle_path");
    if (!recycle.startsWith("/"))
      recycle = "/" + recycle; 
    if (!recycle.endsWith("/"))
      recycle = String.valueOf(recycle) + "/"; 
    (new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_'))).mkdirs();
    String addOn = "";
    int pos = 1;
    while ((new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName() + addOn)).exists())
      addOn = (new StringBuffer(String.valueOf(pos++))).toString(); 
    if (!addOn.equals("")) {
      boolean bool = (new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName())).renameTo(new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName() + addOn));
      if (!bool) {
        Common.copy(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName(), String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName() + addOn, true);
        (new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName())).delete();
      } 
    } 
    boolean ok = v.renameTo(new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName()));
    if (!ok) {
      Common.copy(v.getCanonicalPath(), String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName(), true);
      v.delete();
    } 
  }
  
  public void removeCacheItem(Properties item) {
    boolean ok = true;
    for (int x = -1; x < 10 && ok; x++) {
      String tmpKey = String.valueOf(x) + item.getProperty("root_dir").substring(1) + item.getProperty("name") + uiSG("user_name");
      ok = (this.uVFS.cacheItem.remove(tmpKey) == null);
      this.uVFS.cacheItemStamp.remove(tmpKey);
    } 
  }
  
  public void do_SITE() throws Exception {
    boolean doProxySite = false;
    String original_the_command_data = uiSG("the_command_data");
    if (uiSG("the_command_data").toUpperCase().startsWith("PASS") && SG("site").toUpperCase().indexOf("(SITE_PASS)") >= 0) {
      if (uiSG("new_pass1").equals("")) {
        uiPUT("new_pass1", uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1, uiSG("the_command_data").length()));
        this.not_done = write_command("214", LOC.G("%SITE_PASS-first%"));
      } else {
        uiPUT("new_pass2", uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1, uiSG("the_command_data").length()));
        if (uiSG("new_pass1").equals(uiSG("new_pass2"))) {
          this.not_done = write_command("214", do_ChangePass(uiSG("user_name"), uiSG("new_pass2")));
        } else {
          this.not_done = write_command("214", LOC.G("%SITE_PASS-bad%"));
        } 
        uiPUT("new_pass1", "");
        uiPUT("new_pass2", "");
      } 
    } else if (uiSG("the_command_data").toUpperCase().startsWith("ZONE")) {
      SimpleDateFormat timeZone = new SimpleDateFormat("Z");
      this.not_done = write_command("210", "UTC" + timeZone.format(new Date()));
    } else if (uiSG("the_command_data").toUpperCase().startsWith("UTIME")) {
      stop_idle_timer();
      String the_dir = uiSG("current_dir");
      uiPUT("the_command_data", uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1));
      int loc = 0;
      int dEnd = 0;
      int dStart = 0;
      String fileName = "";
      try {
        try {
          loc = uiSG("the_command_data").lastIndexOf(" ");
          dEnd = uiSG("the_command_data").lastIndexOf(" ", loc - 1);
          dStart = uiSG("the_command_data").lastIndexOf(" ", dEnd - 1);
          loc = uiSG("the_command_data").lastIndexOf(" ", dStart - 1);
          fileName = uiSG("the_command_data").substring(0, loc).trim();
          SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
          df.parse(uiSG("the_command_data").substring(dStart, dEnd).trim());
        } catch (Exception e) {
          dEnd = uiSG("the_command_data").indexOf(" ");
          dStart = 0;
          fileName = uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1).trim();
          SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
          df.parse(uiSG("the_command_data").substring(dStart, dEnd).trim());
        } 
      } catch (Exception exception) {}
      if (!fileName.equals("")) {
        if (fileName.startsWith("/")) {
          the_dir = fileName;
        } else {
          the_dir = String.valueOf(the_dir) + fileName;
        } 
        if (the_dir.equals("/"))
          the_dir = SG("root_dir"); 
        if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
          the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
      } 
      if (check_access_privs(the_dir, "STOR") && !fileName.equals("")) {
        Properties item = this.uVFS.get_item(the_dir);
        if (item != null) {
          try {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
            Date d = df.parse(uiSG("the_command_data").substring(dStart, dEnd).trim());
            Calendar cal = new GregorianCalendar();
            cal.setTime(d);
            cal.add(11, IG("timezone_offset"));
            d = new Date(cal.getTime().getTime());
            if (!ServerStatus.BG("disable_mdtm_modifications")) {
              GenericClient c = this.uVFS.getClient(item);
              try {
                c.mdtm((new VRL(item.getProperty("url"))).getPath(), d.getTime());
              } finally {
                c = this.uVFS.releaseClient(c);
              } 
            } 
            this.not_done = write_command("200", LOC.G("%UTIME-good%"));
          } catch (Exception e) {
            this.not_done = write_command("550", String.valueOf(LOC.G("%UTIME-error%-exception")) + e);
            uiVG("failed_commands").addElement((new Date()).getTime());
          } 
        } else {
          this.not_done = write_command("550", LOC.G("%UTIME-error%"));
          uiVG("failed_commands").addElement((new Date()).getTime());
        } 
      } else if (fileName.equals("")) {
        this.not_done = write_command("200", SG("Command ignored (bad format!)"));
        uiVG("failed_commands").addElement((new Date()).getTime());
      } else {
        this.not_done = write_command("550", LOC.G("%UTIME-bad%"));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
      } 
      start_idle_timer();
    } else if (uiSG("the_command_data").toUpperCase().startsWith("CHMOD")) {
      stop_idle_timer();
      String the_dir = uiSG("current_dir");
      String the_permissions = uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1);
      uiPUT("the_command_data", the_permissions.substring(the_permissions.indexOf(" ") + 1));
      the_permissions = the_permissions.substring(0, the_permissions.indexOf(" "));
      if (the_permissions.indexOf(";") >= 0) {
        the_permissions = "";
        uiPUT("the_command_data", "");
      } 
      if (!uiSG("the_command_data").equals("")) {
        if (uiSG("the_command_data").startsWith("/")) {
          the_dir = uiSG("the_command_data");
        } else {
          the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
        } 
        the_dir = Common.dots(the_dir);
        if (the_dir.equals("/"))
          the_dir = SG("root_dir"); 
        if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
          the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
      } 
      if (check_access_privs(the_dir, "STOR") && SG("site").toUpperCase().indexOf("(SITE_CHMOD)") >= 0) {
        Properties item = this.uVFS.get_item(the_dir);
        if (item.getProperty("url").toUpperCase().startsWith("FILE:")) {
          GenericClient c = this.uVFS.getClient(item);
          try {
            c.setMod((new VRL(item.getProperty("url"))).getPath(), the_permissions, "");
          } finally {
            c = this.uVFS.releaseClient(c);
          } 
          this.not_done = write_command("214", LOC.G("%CHMOD-good%"));
        } else {
          doProxySite = true;
        } 
      } else {
        this.not_done = write_command("214", LOC.G("%CHMOD-bad%"));
        uiVG("failed_commands").addElement((new Date()).getTime());
      } 
      start_idle_timer();
    } else if (uiSG("the_command_data").toUpperCase().startsWith("CHOWN")) {
      stop_idle_timer();
      String the_dir = uiSG("current_dir");
      String the_permissions = uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1);
      uiPUT("the_command_data", the_permissions.substring(the_permissions.lastIndexOf(" ") + 1));
      the_permissions = the_permissions.substring(0, the_permissions.lastIndexOf(" "));
      if (the_permissions.indexOf(";") >= 0) {
        the_permissions = "";
        uiPUT("the_command_data", "");
      } 
      if (!uiSG("the_command_data").equals("")) {
        if (uiSG("the_command_data").startsWith("/")) {
          the_dir = uiSG("the_command_data");
        } else {
          the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
        } 
        the_dir = Common.dots(the_dir);
        if (the_dir.equals("/"))
          the_dir = SG("root_dir"); 
        if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
          the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
      } 
      if (check_access_privs(the_dir, "STOR") && SG("site").toUpperCase().indexOf("(SITE_CHOWN)") >= 0) {
        Properties item = this.uVFS.get_item(the_dir);
        if (item.getProperty("url").toUpperCase().startsWith("FILE:")) {
          GenericClient c = this.uVFS.getClient(item);
          try {
            c.setOwner((new VRL(item.getProperty("url"))).getPath(), the_permissions, "");
          } finally {
            c = this.uVFS.releaseClient(c);
          } 
          this.not_done = write_command("214", LOC.G("%CHOWN-good%"));
        } else {
          doProxySite = true;
        } 
      } else {
        this.not_done = write_command("214", LOC.G("%CHOWN-bad%"));
        uiVG("failed_commands").addElement((new Date()).getTime());
      } 
      start_idle_timer();
    } else if (uiSG("the_command_data").toUpperCase().startsWith("CHGRP")) {
      stop_idle_timer();
      String the_dir = uiSG("current_dir");
      String the_permissions = uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1);
      uiPUT("the_command_data", the_permissions.substring(the_permissions.lastIndexOf(" ") + 1));
      the_permissions = the_permissions.substring(0, the_permissions.lastIndexOf(" "));
      if (the_permissions.indexOf(";") >= 0) {
        the_permissions = "";
        uiPUT("the_command_data", "");
      } 
      if (!uiSG("the_command_data").equals("")) {
        if (uiSG("the_command_data").startsWith("/")) {
          the_dir = uiSG("the_command_data");
        } else {
          the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
        } 
        the_dir = Common.dots(the_dir);
        if (the_dir.equals("/"))
          the_dir = SG("root_dir"); 
        if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
          the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
      } 
      if (check_access_privs(the_dir, "STOR") && SG("site").toUpperCase().indexOf("(SITE_CHGRP)") >= 0) {
        Properties item = this.uVFS.get_item(the_dir);
        if (item.getProperty("url").toUpperCase().startsWith("FILE:")) {
          GenericClient c = this.uVFS.getClient(item);
          try {
            c.setGroup((new VRL(item.getProperty("url"))).getPath(), the_permissions, "");
          } finally {
            c = this.uVFS.releaseClient(c);
          } 
          this.not_done = write_command("214", LOC.G("%CHGRP-good%"));
        } else {
          doProxySite = true;
        } 
      } else {
        this.not_done = write_command("214", LOC.G("%CHGRP-bad%"));
        uiVG("failed_commands").addElement((new Date()).getTime());
      } 
      start_idle_timer();
    } else if (uiSG("the_command_data").toUpperCase().startsWith("RANDOMACCESS")) {
      uiPUT("randomaccess", uiSG("the_command_data").substring("RANDOMACCESS".length()).trim().toLowerCase());
    } else if (uiSG("the_command_data").toUpperCase().startsWith("PGP_HEADER_SIZE")) {
      stop_idle_timer();
      String the_dir = uiSG("current_dir");
      uiPUT("the_command_data", uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1));
      String the_size = uiSG("the_command_data").substring(0, uiSG("the_command_data").indexOf(" "));
      uiPUT("the_command_data", uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1));
      if (!uiSG("the_command_data").equals("")) {
        if (uiSG("the_command_data").startsWith("/")) {
          the_dir = uiSG("the_command_data");
        } else {
          the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
        } 
        the_dir = Common.dots(the_dir);
        if (the_dir.equals("/"))
          the_dir = SG("root_dir"); 
        if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
          the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
      } 
      if (check_access_privs(the_dir, "STOR")) {
        Properties item = this.uVFS.get_item(the_dir);
        if (item.getProperty("url").toUpperCase().startsWith("FILE:")) {
          GenericClient c = this.uVFS.getClient(item);
          try {
            c.doCommand("SITE PGP_HEADER_SIZE " + the_size + " " + (new VRL(item.getProperty("url"))).getPath());
          } finally {
            c = this.uVFS.releaseClient(c);
          } 
          this.not_done = write_command("214", "OK");
        } else {
          doProxySite = true;
        } 
      } else {
        this.not_done = write_command("214", "Write not allowed.");
        uiVG("failed_commands").addElement((new Date()).getTime());
      } 
      start_idle_timer();
    } else if (uiSG("the_command_data").toUpperCase().startsWith("MD5") || uiSG("the_command_data").toUpperCase().startsWith("XMD5")) {
      stop_idle_timer();
      boolean chunked = uiSG("the_command_data").toUpperCase().startsWith("MD5S");
      String the_dir = uiSG("current_dir");
      String the_permissions = uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1);
      uiPUT("the_command_data", the_permissions.substring(the_permissions.lastIndexOf(" ") + 1));
      if (!uiSG("the_command_data").equals("")) {
        if (uiSG("the_command_data").startsWith("/")) {
          the_dir = uiSG("the_command_data");
        } else {
          the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
        } 
        the_dir = Common.dots(the_dir);
        if (the_dir.equals("/"))
          the_dir = SG("root_dir"); 
        if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
          the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
      } 
      if (check_access_privs(the_dir, "RETR")) {
        Properties item = this.uVFS.get_item(the_dir);
        GenericClient c = this.uVFS.getClient(item);
        try {
          Vector md5s = new Vector();
          Common.getMD5(c.download((new VRL(item.getProperty("url"))).getPath(), 0L, -1L, true), md5s, chunked, true, Long.parseLong(item.getProperty("size")), Long.parseLong(item.getProperty("size")));
          this.not_done = write_command("214", md5s.elementAt(0).toString());
        } finally {
          c = this.uVFS.releaseClient(c);
        } 
      } else {
        this.not_done = write_command("550", LOC.G("MD5 not allowed."));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
      } 
      start_idle_timer();
    } else if (uiSG("the_command_data").toUpperCase().trim().startsWith("ZIP") && (SG("site").toUpperCase().indexOf("(SITE_ZIP)") >= 0 || SG("site").toUpperCase().indexOf("(SITE_INVISIBLE)") >= 0)) {
      if (uiSG("the_command_data").toUpperCase().indexOf("DIR") >= 0)
        uiPUT("list_zip_dir", (new StringBuffer(String.valueOf(!uiBG("list_zip_dir")))).toString()); 
      if (uiSG("the_command_data").toUpperCase().indexOf("FILE") >= 0)
        uiPUT("list_zip_file", (new StringBuffer(String.valueOf(!uiBG("list_zip_file")))).toString()); 
      if (uiSG("the_command_data").toUpperCase().indexOf("ONLY") >= 0)
        uiPUT("list_zip_only", (new StringBuffer(String.valueOf(!uiBG("list_zip_only")))).toString()); 
      if (uiSG("the_command_data").toUpperCase().indexOf("APP") >= 0)
        uiPUT("list_zip_app", (new StringBuffer(String.valueOf(!uiBG("list_zip_app")))).toString()); 
      this.not_done = write_command("214", "(" + LOC.G("Zip files") + ":" + uiBG("list_zip_file") + ")  (" + LOC.G("Zip applications") + ":" + uiBG("list_zip_app") + ")  (" + LOC.G("Zip directories") + ":" + uiBG("list_zip_dir") + ")  (" + LOC.G("Only show zipped directories") + ":" + uiBG("list_zip_only") + ")");
    } else if (uiSG("the_command_data").toUpperCase().trim().startsWith("DOT") && SG("site").toUpperCase().indexOf("(SITE_DOT)") >= 0) {
      uiPUT("list_dot", (new StringBuffer(String.valueOf(!uiBG("list_dot")))).toString());
      this.not_done = write_command("214", "(. " + LOC.G("files") + " : " + uiBG("list_dot") + ")");
    } else if (uiSG("the_command_data").toUpperCase().startsWith("VERSION") && SG("site").toUpperCase().indexOf("(SITE_VERSION)") >= 0) {
      this.not_done = write_command("214", "%version_info%");
    } else if (uiSG("the_command_data").toUpperCase().startsWith("USERS") && SG("site").toUpperCase().indexOf("(SITE_USERS)") >= 0) {
      String user_lising = "";
      for (int x = 0; x < ServerStatus.siVG("user_list").size(); x++) {
        try {
          user_lising = String.valueOf(user_lising) + "'" + ((Properties)ServerStatus.siVG("user_list").elementAt(x)).getProperty("id") + "' , ";
        } catch (Exception exception) {}
      } 
      this.not_done = write_command("214", "USER LIST=" + user_lising.substring(0, user_lising.length() - 3));
    } else if (uiSG("the_command_data").toUpperCase().startsWith("KICK") && !uiSG("the_command_data").toUpperCase().startsWith("KICKBAN") && uiSG("the_command_data").length() > 4 && SG("site").toUpperCase().indexOf("(SITE_KICK)") >= 0) {
      String the_user = uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1, uiSG("the_command_data").length());
      if (ServerStatus.thisObj.kick(the_user)) {
        this.not_done = write_command("214", "USER " + the_user + " " + LOC.G("KICKED") + ".");
      } else {
        this.not_done = write_command("214", "USER " + the_user + " " + LOC.G("KICK failed") + ".  " + LOC.G("User") + " '" + the_user + "' " + LOC.G("not found") + ".");
      } 
    } else if (uiSG("the_command_data").toUpperCase().startsWith("KICKBAN") && uiSG("the_command_data").length() > 7 && SG("site").toUpperCase().indexOf("(SITE_KICKBAN)") >= 0) {
      String the_user = uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1, uiSG("the_command_data").length());
      if (ServerStatus.thisObj.ban(the_user) && ServerStatus.thisObj.kick(the_user)) {
        this.not_done = write_command("214", "USER " + the_user + " " + LOC.G("KICKED and BANNED") + ".");
      } else {
        this.not_done = write_command("214", "USER " + the_user + " " + LOC.G("KICK and BAN failed") + ".  " + LOC.G("User") + " '" + the_user + "' " + LOC.G("not found") + ".");
      } 
    } else if (uiSG("the_command_data").toUpperCase().startsWith("QUIT") && SG("site").toUpperCase().indexOf("(SITE_QUIT)") >= 0) {
      this.not_done = write_command("214", LOC.G("%SITE_QUIT-good%"));
      ServerStatus.siPUT("waiting_quit_user_name", this.user_info);
    } else if (uiSG("the_command_data").toUpperCase().startsWith("RESTART") && (SG("site").toUpperCase().indexOf("(SITE_QUIT)") >= 0 || SG("site").toUpperCase().indexOf("(CONNECT)") >= 0)) {
      this.not_done = write_command("214", LOC.G("Server will restart when you logout."));
      ServerStatus.siPUT("waiting_restart_user_name", this.user_info);
    } else if (uiSG("the_command_data").toUpperCase().startsWith("PLUGIN") && SG("site").toUpperCase().indexOf("(SITE_PLUGIN)") >= 0) {
      Properties fileItem = new Properties();
      fileItem.put("url", "ftp://127.0.0.1:56789/");
      fileItem.put("the_file_path", uiSG("the_command_data").substring("plugin".length()).trim());
      fileItem.put("the_file_size", "1");
      fileItem.put("event_name", fileItem.getProperty("the_file_path").substring(0, fileItem.getProperty("the_file_path").indexOf(" ")));
      fileItem.put("the_file_name", fileItem.getProperty("the_file_path").substring(fileItem.getProperty("event_name").length() + 1));
      do_event5("SITE", fileItem);
      this.not_done = write_command("214", fileItem.getProperty("execute_log", "No Result"));
    } else if (uiSG("the_command_data").toUpperCase().startsWith("LIST")) {
      try {
        String add_str = "";
        if (!add_str.equals("")) {
          int item_count = Common.count_str(add_str, this.CRLF);
          int folder_count = Common.count_str(add_str, "/" + this.CRLF);
          add_str = String.valueOf(folder_count) + " " + LOC.G("folders") + ", " + (item_count - folder_count) + " " + LOC.G("files") + "." + this.CRLF + add_str;
        } 
        this.not_done = write_command("214", String.valueOf(add_str.trim()) + this.CRLF + LOC.G("End of list") + ".");
      } catch (Exception e) {
        this.not_done = write_command("214", LOC.G("Format is SITE LIST /dir/path/"));
      } 
    } else if (uiSG("the_command_data").toUpperCase().startsWith("SEARCH") || uiSG("the_command_data").toUpperCase().startsWith("DUPE")) {
      Vector listing = new Vector();
      try {
        this.uVFS.getListing(listing, uiSG("current_dir"), 20, 30000, false);
      } catch (Exception exception) {}
      String search = uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1);
      StringBuffer item_str = new StringBuffer();
      try {
        for (int x = 0; x < listing.size(); x++) {
          Properties item = listing.elementAt(x);
          if (LIST_handler.checkName(item, this, false, false) && Common.do_search(search.toUpperCase(), item.getProperty("name", "").toUpperCase(), false, 0))
            LIST_handler.generateLineEntry(item, item_str, false, "/", false, this, false); 
        } 
        this.not_done = write_command("214", String.valueOf(item_str.toString()) + this.CRLF + LOC.G("End of list") + ".");
      } catch (Exception e) {
        this.not_done = write_command("214", LOC.G("Format is SITE SEARCH filename"));
      } 
    } else if (uiSG("the_command_data").toUpperCase().startsWith("HELP")) {
      if (uiSG("the_command_data").toUpperCase().length() == 4) {
        this.not_done = write_command("214", LOC.G("%SITE_HELP%"));
      } else if (uiSG("the_command_data").toUpperCase().endsWith("PASS")) {
        this.not_done = write_command("214", LOC.G("%SITE_PASS%"));
      } else if (uiSG("the_command_data").toUpperCase().endsWith("VERSION")) {
        this.not_done = write_command("214", LOC.G("%SITE_VERSION%"));
      } else if (uiSG("the_command_data").toUpperCase().endsWith("USERS")) {
        this.not_done = write_command("214", LOC.G("%SITE_USERS%"));
      } else if (uiSG("the_command_data").toUpperCase().endsWith("KICKBAN")) {
        this.not_done = write_command("214", LOC.G("%SITE_KICKBAN%"));
      } else if (uiSG("the_command_data").toUpperCase().endsWith("KICK")) {
        this.not_done = write_command("214", LOC.G("%SITE_KICK%"));
      } else if (uiSG("the_command_data").toUpperCase().endsWith("QUIT")) {
        this.not_done = write_command("214", LOC.G("%SITE_QUIT%"));
      } else if (uiSG("the_command_data").toUpperCase().endsWith("QUIT")) {
        this.not_done = write_command("550", LOC.G("Unknown HELP Command"));
      } 
    } else if (uiSG("the_command").equals("SITE") && uiSG("the_command_data").toUpperCase().startsWith("PROXY_") && !uiSG("the_command_data").toUpperCase().startsWith("PROXY_CONFIRM")) {
      stop_idle_timer();
      uiPUT(uiSG("the_command_data").substring(0, uiSG("the_command_data").indexOf(" ")).toLowerCase(), uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1));
      this.not_done = write_command("214", LOC.G("OK"));
      start_idle_timer();
    } else {
      doProxySite = true;
    } 
    if (doProxySite)
      try {
        GenericClient c = this.uVFS.getClient(this.uVFS.get_item_parent(uiSG("current_dir")));
        try {
          String response = c.doCommand(String.valueOf(uiSG("the_command")) + " " + original_the_command_data);
          if (response == null || response.equals("")) {
            this.not_done = write_command("502", LOC.G("%unknown_command%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
          } else {
            this.not_done = write_command(response);
          } 
        } finally {
          c = this.uVFS.releaseClient(c);
        } 
      } catch (NullPointerException e) {
        this.not_done = write_command("502", LOC.G("%unknown_command%"));
      }  
  }
  
  public String do_DELE(boolean recurse, String user_dir) throws Exception {
    uiPUT("the_command", "DELE");
    uiPUT("last_logged_command", "DELE");
    String the_dir = fixupDir(user_dir);
    String parentPath = this.uVFS.getPrivPath(the_dir, false, false);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_item(the_dir, -1);
    if (item == null) {
      dir_item.put("privs", String.valueOf(dir_item.getProperty("privs")) + "(inherited)");
      if (check_access_privs(the_dir, uiSG("the_command"), dir_item))
        Common.trackSync("DELETE", the_dir, null, false, 0L, 0L, SG("root_dir"), dir_item.getProperty("privs"), uiSG("clientid"), ""); 
      this.not_done = write_command("550", LOC.G("%DELE-not found%"));
      doErrorEvent(new Exception(uiSG("lastLog")));
      uiVG("failed_commands").addElement((new Date()).getTime());
      return "%DELE-not found%";
    } 
    if (check_access_privs(the_dir, uiSG("the_command"), item)) {
      boolean check_all = false;
      if (uiSG("the_command").equalsIgnoreCase("DELE") && !check_access_privs(the_dir, "RMD", item))
        check_all = true; 
      changeProxyToCurrentDir(item);
      Common.trackSync("DELETE", the_dir, null, false, 0L, 0L, SG("root_dir"), dir_item.getProperty("privs"), uiSG("clientid"), "");
      Properties stat = null;
      long quota = -12345L;
      if (item == null) {
        this.not_done = write_command("550", LOC.G("%DELE-not found%"));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        return "%DELE-not found%";
      } 
      GenericClient c = this.uVFS.getClient(item);
      try {
        stat = c.stat((new VRL(item.getProperty("url"))).getPath());
        if (stat.getProperty("type").equalsIgnoreCase("DIR")) {
          uiPUT("the_command", "RMD");
          if (!check_access_privs(the_dir, uiSG("the_command"))) {
            uiPUT("the_command", "DELE");
            this.not_done = write_command("550", LOC.G("%DELE-bad%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
            return "%DELE-bad%";
          } 
          uiPUT("the_command", "DELE");
        } 
        quota = get_quota(the_dir);
        if (item != null && stat != null) {
          Properties fileItem = uiVG("session_commands").elementAt(uiVG("session_commands").size() - 1);
          fileItem = (Properties)fileItem.clone();
          Log.log("FTP_SERVER", 2, "Tracking delete:" + the_dir);
          uiVG("session_commands").addElement(fileItem);
          fileItem.put("the_command", "DELE");
          fileItem.put("the_command_data", the_dir);
          fileItem.put("url", item.getProperty("url", ""));
          fileItem.put("the_file_path", Common.all_but_last(the_dir));
          fileItem.put("the_file_name", stat.getProperty("name"));
          fileItem.put("the_file_size", stat.getProperty("size"));
          fileItem.put("the_file_speed", "0");
          fileItem.put("the_file_start", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
          fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
          fileItem.put("the_file_error", "");
          fileItem.put("the_file_status", "SUCCESS");
          do_event5("DELETE", fileItem);
          long totalSize = Long.parseLong(item.getProperty("size", "0"));
          boolean deleted = false;
          Common.trackSyncRevision(new File((new VRL(item.getProperty("url"))).getCanonicalPath()), the_dir, SG("root_dir"), item.getProperty("privs"), true);
          SearchHandler.buildEntry(item, this.uVFS, true, true);
          if (ServerStatus.BG("recycle")) {
            Log.log("FTP_SERVER", 3, String.valueOf(LOC.G("Attempting to recycle file:")) + the_dir);
            do_Recycle(new VRL(item.getProperty("url")));
          } else {
            Log.log("FTP_SERVER", 3, String.valueOf(LOC.G("Attempting to delete file:")) + the_dir);
            deleted = c.delete((new VRL(item.getProperty("url"))).getPath());
          } 
          stat = null;
          if (!deleted)
            stat = c.stat((new VRL(item.getProperty("url"))).getPath()); 
          if (!deleted && stat != null && recurse) {
            try {
              totalSize = Common.recurseSize((new VRL(item.getProperty("url"))).getCanonicalPath(), 0L, this);
            } catch (Exception e) {
              Log.log("FTP_SERVER", 1, e);
            } 
            if (ServerStatus.BG("recycle")) {
              do_Recycle(new VRL(item.getProperty("url")));
            } else {
              try {
                if (this.user.get("events") != null && ((Vector)this.user.get("events")).size() > 0) {
                  Vector list = new Vector();
                  Common.getAllFileListing(list, (new VRL(item.getProperty("url"))).getCanonicalPath(), 999, true);
                  for (int x = 0; x < list.size(); x++) {
                    File file = list.elementAt(x);
                    String path = file.getAbsolutePath().substring((new VRL(item.getProperty("url"))).getCanonicalPath().length());
                    fileItem = (Properties)fileItem.clone();
                    uiVG("session_commands").addElement(fileItem);
                    Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Tracking delete:")) + path);
                    fileItem.put("the_command", "DELE");
                    fileItem.put("the_command_data", path);
                    fileItem.put("url", file.toURI().toURL().toExternalForm());
                    fileItem.put("the_file_path", Common.all_but_last(path));
                    fileItem.put("the_file_name", file.getName());
                    fileItem.put("the_file_size", (new StringBuffer(String.valueOf(file.length()))).toString());
                    fileItem.put("the_file_speed", "0");
                    fileItem.put("the_file_start", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                    fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                    fileItem.put("the_file_error", "");
                    fileItem.put("the_file_status", "SUCCESS");
                    do_event5("DELETE", fileItem);
                  } 
                } 
                if (check_all || ServerStatus.BG("check_all_recursive_deletes")) {
                  Vector list = new Vector();
                  this.uVFS.getListing(list, the_dir, 99, 9999, true);
                  for (int x = list.size() - 1; x >= 0; x--) {
                    Properties p = list.elementAt(x);
                    if (p.getProperty("type", "").equals("FILE")) {
                      String temp_dir = String.valueOf(p.getProperty("root_dir")) + p.getProperty("name");
                      if (check_access_privs(temp_dir, "DELE", p))
                        c.delete((new VRL(p.getProperty("url"))).getPath()); 
                      list.removeElementAt(x);
                    } 
                  } 
                  for (int loop = 0; list.size() > 0 && loop < 5; loop++) {
                    for (int i = list.size() - 1; i >= 0; i--) {
                      Properties p = list.elementAt(i);
                      if (p.getProperty("type", "").equals("DIR")) {
                        String temp_dir = String.valueOf(p.getProperty("root_dir")) + p.getProperty("name");
                        if (check_access_privs(temp_dir, "RMD", p))
                          if (c.delete((new VRL(p.getProperty("url"))).getPath()))
                            list.removeElementAt(i);  
                      } 
                    } 
                  } 
                } else {
                  Common.recurseDelete((new VRL(item.getProperty("url"))).getCanonicalPath(), false);
                  c.delete(the_dir);
                } 
              } catch (NullPointerException nullPointerException) {}
            } 
            if (item != null)
              trackAndUpdateUploads(uiVG("lastUploadStats"), new VRL(item.getProperty("url")), new VRL(item.getProperty("url")), "DELETE"); 
          } 
          stat = null;
          if (!deleted)
            stat = c.stat((new VRL(item.getProperty("url"))).getPath()); 
          if (!deleted && stat != null) {
            if (!(new File(ServerStatus.SG("recycle_path"))).exists() && ServerStatus.BG("recycle")) {
              this.not_done = write_command("550", LOC.G("%DELE-error%:Recycle bin not found."));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
              return "%DELE-error%:Recycle bin not found.";
            } 
            Log.log("FTP_SERVER", 3, LOC.G("Delete failure.  Deleted:$0 Exists:$1", (new StringBuffer(String.valueOf(deleted))).toString(), (stat != null) ? 1 : 0));
            this.not_done = write_command("550", LOC.G("%DELE-error%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
            return "%DELE-error%";
          } 
          this.not_done = write_command("250", ServerStatus.SG("custom_delete_msg"));
          removeCacheItem(item);
          if (quota != -12345L) {
            if (item.getProperty("privs", "").indexOf("(real_quota)") >= 0) {
              quota += totalSize;
            } else {
              quota += totalSize;
            } 
            set_quota(the_dir, quota);
          } 
          return "";
        } 
        this.not_done = write_command("550", LOC.G("%DELE-not found%"));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        return "%DELE-not found%";
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
    } 
    this.not_done = write_command("550", LOC.G("%DELE-bad%"));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%DELE-bad%";
  }
  
  public String do_RNFR() throws Exception {
    uiPUT("the_command", "RNFR");
    uiPUT("last_logged_command", "RNFR");
    this.rnfr_file = null;
    String the_dir = fixupDir(null);
    String parentPath = this.uVFS.getRootVFS(the_dir, -1);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_fake_item(the_dir, "FILE");
    this.rnfr_file_path = the_dir;
    if (check_access_privs(the_dir, uiSG("the_command"), item)) {
      changeProxyToCurrentDir(item);
      GenericClient c = this.uVFS.getClient(item);
      try {
        if (c.stat((new VRL(item.getProperty("url"))).getPath()) != null) {
          this.not_done = write_command("350", LOC.G("%RNFR%"));
          this.rnfr_file = item;
          this.rnfr_file_path = the_dir;
          return "";
        } 
        this.not_done = write_command("550", LOC.G("%RNFR-not found%"));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        return "%RNFR-not found%";
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
    } 
    this.not_done = write_command("550", LOC.G("%RNFR-bad%"));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%RNFR-bad%";
  }
  
  public void trackAndUpdateUploads(Vector lastUploadStats, VRL src, VRL dest, String type) {
    if (lastUploadStats == null)
      return; 
    for (int x = lastUploadStats.size() - 1; x >= 0; x--) {
      Properties p2 = lastUploadStats.elementAt(x);
      if (p2.getProperty("url", "").toUpperCase().equals(src.toUpperCase()) && type.equals("RENAME")) {
        p2.put("url", dest);
        p2.put("path", dest.getPath());
        p2.put("name", dest.getName());
        break;
      } 
      if (p2.getProperty("url", "").toUpperCase().equals(src.toUpperCase()) && type.equals("DELETE")) {
        do_event5("DELETE", p2, null);
        lastUploadStats.removeElementAt(x);
        break;
      } 
    } 
  }
  
  public String do_RNTO(boolean overwrite) throws Exception {
    uiPUT("the_command", "RNTO");
    uiPUT("last_logged_command", "RNTO");
    String the_dir = fixupDir(null);
    Properties combinedPermissions = this.uVFS.getCombinedPermissions();
    boolean aclPermissions = combinedPermissions.getProperty("acl_permissions", "false").equals("true");
    Properties actual_item = this.uVFS.get_item(the_dir);
    Properties item = this.uVFS.get_item_parent(the_dir);
    if (!aclPermissions)
      actual_item = item; 
    if (check_access_privs(the_dir, uiSG("the_command"), item) && (!overwrite || (overwrite && check_access_privs(Common.all_but_last(the_dir), "DELE", actual_item))) && Common.filter_check("R", Common.last(the_dir), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter"))) {
      changeProxyToCurrentDir(this.uVFS.get_item_parent(Common.all_but_last(the_dir)));
      GenericClient c = this.uVFS.getClient(item);
      try {
        VRL vrl = new VRL(item.getProperty("url"));
        boolean exists = (c.stat(vrl.getPath()) != null);
        if (this.rnfr_file == null) {
          Common.trackSync("RENAME", this.rnfr_file_path, String.valueOf(item.getProperty("root_dir", "")) + item.getProperty("name", "") + (this.rnfr_file_path.endsWith("/") ? "/" : ""), false, 0L, 0L, SG("root_dir"), item.getProperty("privs"), uiSG("clientid"), "");
          return "";
        } 
        if (!exists || (exists && overwrite) || this.rnfr_file.getProperty("url").equalsIgnoreCase(item.getProperty("url")) || this.rnfr_file.getProperty("url").toUpperCase().equals(String.valueOf(item.getProperty("url").toUpperCase()) + "/") || (new VRL(this.rnfr_file.getProperty("url"))).getPath().equalsIgnoreCase(vrl.getPath())) {
          SearchHandler.buildEntry(this.rnfr_file, this.uVFS, true, true);
          UserTools.updatePrivpath(uiSG("listen_ip_port"), uiSG("user_name"), String.valueOf(this.rnfr_file.getProperty("root_dir", "")) + this.rnfr_file.getProperty("name", ""), the_dir, item, null, this.uVFS);
          if (overwrite && !vrl.getPath().equalsIgnoreCase((new VRL(this.rnfr_file.getProperty("url"))).getPath()))
            if (c.stat((new VRL(this.rnfr_file.getProperty("url"))).getPath()) != null)
              c.delete(vrl.getPath());  
          if (c.rename((new VRL(this.rnfr_file.getProperty("url"))).getPath(), vrl.getPath())) {
            trackAndUpdateUploads(uiVG("lastUploadStats"), new VRL(this.rnfr_file.getProperty("url")), new VRL(item.getProperty("url")), "RENAME");
          } else {
            String srcPath = (new VRL(this.rnfr_file.getProperty("url"))).getCanonicalPath();
            String dstPath = (new VRL(item.getProperty("url"))).getCanonicalPath();
            if (dstPath.startsWith(srcPath) || !(new VRL(this.rnfr_file.getProperty("url"))).getProtocol().equalsIgnoreCase("file") || !(new VRL(item.getProperty("url"))).getProtocol().equalsIgnoreCase("file")) {
              this.not_done = write_command("550", LOC.G("%RNTO-bad%"));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
              this.rnfr_file = null;
              this.rnfr_file_path = null;
              return "%RNTO-bad%";
            } 
            if (this.rnfr_file.getProperty("type").equalsIgnoreCase("DIR")) {
              srcPath = String.valueOf(srcPath) + "/";
              dstPath = String.valueOf(dstPath) + "/";
              if (srcPath.equals(dstPath))
                dstPath = String.valueOf(dstPath) + " Copy/"; 
              Common.recurseCopy(srcPath, dstPath, true);
            } else {
              if (srcPath.equals(dstPath))
                dstPath = String.valueOf(dstPath) + " Copy"; 
              Common.recurseCopy(srcPath, dstPath, true);
            } 
            Common.recurseDelete(srcPath, false);
            trackAndUpdateUploads(uiVG("lastUploadStats"), new VRL(this.rnfr_file.getProperty("url")), new VRL(item.getProperty("url")), "RENAME");
          } 
          Properties fileItem1 = item;
          fileItem1 = (Properties)fileItem1.clone();
          Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Tracking rename:")) + the_dir);
          uiVG("session_commands").addElement(fileItem1);
          fileItem1.put("the_command", "RNTO");
          fileItem1.put("the_command_data", the_dir);
          fileItem1.put("the_file_path2", this.rnfr_file.getProperty("root_dir", ""));
          fileItem1.put("url_2", this.rnfr_file.getProperty("url", ""));
          fileItem1.put("the_file_name_2", this.rnfr_file.getProperty("name"));
          fileItem1.put("the_file_path", Common.all_but_last(the_dir));
          fileItem1.put("the_file_name", item.getProperty("name"));
          fileItem1.put("the_file_size", this.rnfr_file.getProperty("size", "0"));
          fileItem1.put("the_file_speed", "0");
          fileItem1.put("the_file_start", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
          fileItem1.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
          fileItem1.put("the_file_error", "");
          fileItem1.put("the_file_status", "SUCCESS");
          Properties fileItem2 = (Properties)fileItem1.clone();
          fileItem2.put("url", fileItem2.getProperty("url_2"));
          fileItem2.put("the_file_name", fileItem2.getProperty("the_file_name_2"));
          do_event5("RENAME", fileItem1, fileItem2);
          this.not_done = write_command("250", LOC.G("%RNTO%"));
          boolean isDir = this.rnfr_file.getProperty("type").equalsIgnoreCase("DIR");
          item.put("type", isDir ? "DIR" : "FILE");
          Common.trackSync("RENAME", String.valueOf(this.rnfr_file.getProperty("root_dir", "")) + this.rnfr_file.getProperty("name", "") + (isDir ? "/" : ""), String.valueOf(item.getProperty("root_dir", "")) + item.getProperty("name", "") + (isDir ? "/" : ""), false, 0L, 0L, SG("root_dir"), item.getProperty("privs"), uiSG("clientid"), "");
          SearchHandler.buildEntry(item, this.uVFS, false, false);
          this.rnfr_file = null;
          this.rnfr_file_path = null;
          this.uVFS.reset();
          return "";
        } 
        this.not_done = write_command("550", LOC.G("%RNTO-error%"));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        this.rnfr_file = null;
        this.rnfr_file_path = null;
        return "%RNTO-error%";
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
    } 
    this.not_done = write_command("550", LOC.G("%RNTO-bad%"));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    this.rnfr_file = null;
    this.rnfr_file_path = null;
    return "%RNTO-bad%";
  }
  
  public String do_MKD(boolean mkdirs, String user_dir) throws Exception {
    uiPUT("the_command", "MKD");
    uiPUT("last_logged_command", "MKD");
    String the_dir = fixupDir(user_dir);
    if (!the_dir.endsWith("/"))
      the_dir = String.valueOf(the_dir) + "/"; 
    String parentPath = this.uVFS.getRootVFS(the_dir, -1);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_item_parent(the_dir);
    if (check_access_privs(the_dir, uiSG("the_command"), item)) {
      changeProxyToCurrentDir(item);
      Log.log("FTP_SERVER", 3, String.valueOf(LOC.G("Using item:")) + dir_item);
      GenericClient c = this.uVFS.getClient(dir_item);
      try {
        boolean result = false;
        if (mkdirs) {
          Common.verifyOSXVolumeMounted(item.getProperty("url"));
          result = c.makedirs((new VRL(item.getProperty("url"))).getPath());
        } else {
          result = c.makedir((new VRL(item.getProperty("url"))).getPath());
        } 
        if (!result && c.stat((new VRL(item.getProperty("url"))).getPath()) != null) {
          this.not_done = write_command(System.getProperty("crushftp.mkd.451", "521"), LOC.G("%MKD-exists%"));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
          return "%MKD-exists%";
        } 
        Common.trackSync("CHANGE", the_dir, null, true, 0L, 0L, SG("root_dir"), item.getProperty("privs"), uiSG("clientid"), "");
        if (!result) {
          this.not_done = write_command("550", LOC.G("%MKD-bad%"));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
          return "%MKD-bad%";
        } 
        setFolderPrivs(c, item);
        if (the_dir.startsWith(SG("root_dir")))
          the_dir = the_dir.substring(SG("root_dir").length() - 1); 
        this.not_done = write_command("257", LOC.G("\"$0\" directory created.", the_dir));
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
      this.uVFS.reset();
      return "";
    } 
    this.not_done = write_command("550", LOC.G("%MKD-bad%"));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%MKD-bad%";
  }
  
  public void setFolderPrivs(GenericClient c, Properties item) throws Exception {
    if (!SG("default_owner_command").equals("")) {
      c.setOwner((new VRL(item.getProperty("url"))).getPath(), ServerStatus.change_vars_to_values_static(SG("default_owner_command"), this.user, this.user_info, this), "");
      Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set owner of new folder to:")) + SG("default_owner_command"));
    } else if (!item.getProperty("owner", "").equals("user") && !item.getProperty("owner", "").equals("owner")) {
      try {
        Properties parentItem = item;
        c.setOwner((new VRL(item.getProperty("url"))).getPath(), parentItem.getProperty("owner", "").trim(), "");
        Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set owner of new folder to:")) + parentItem.getProperty("owner", "").trim());
      } catch (Exception e) {
        Log.log("FTP_SERVER", 2, e);
      } 
    } 
    if (!SG("default_group_command").equals("")) {
      c.setGroup((new VRL(item.getProperty("url"))).getPath(), SG("default_group_command"), "");
      Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set group of new folder to:")) + SG("default_group_command"));
    } else if (!item.getProperty("group", "").equals("group")) {
      try {
        Properties parentItem = item;
        c.setGroup((new VRL(item.getProperty("url"))).getPath(), parentItem.getProperty("group", "").trim(), "");
        Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set group of new folder to:")) + parentItem.getProperty("group", "").trim());
      } catch (Exception exception) {}
    } 
    String folderPrivs = SG("default_folder_privs_command");
    if (folderPrivs == null || (folderPrivs.equals("") && !SG("default_privs_command").equals("")))
      folderPrivs = SG("default_privs_command"); 
    if (!folderPrivs.equals("")) {
      c.setMod((new VRL(item.getProperty("url"))).getPath(), folderPrivs, "");
      Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set privs of new folder to:")) + folderPrivs);
    } 
  }
  
  public String do_RMD(String user_dir) throws Exception {
    uiPUT("the_command", "RMD");
    uiPUT("last_logged_command", "RMD");
    String the_dir = user_dir;
    if (!uiSG("the_command_data").equals("")) {
      if (uiSG("the_command_data").startsWith("/")) {
        the_dir = uiSG("the_command_data");
      } else {
        the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
      } 
      the_dir = Common.dots(the_dir);
      if (the_dir.equals("/"))
        the_dir = SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
    } 
    String parentPath = this.uVFS.getRootVFS(the_dir, -1);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_fake_item(the_dir, "DIR");
    if (check_access_privs(the_dir, uiSG("the_command"), item) && item != null) {
      changeProxyToCurrentDir(item);
      GenericClient c = this.uVFS.getClient(item);
      try {
        Properties stat1 = c.stat((new VRL(item.getProperty("url"))).getPath());
        if (stat1 != null && stat1.getProperty("type").equalsIgnoreCase("dir")) {
          if (c.delete((new VRL(this.uVFS.get_item(the_dir).getProperty("url"))).getPath())) {
            this.not_done = write_command("250", LOC.G("%RMD%"));
          } else {
            this.not_done = write_command("550", LOC.G("%RMD-not_empty%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
            return "%RMD-not_empty%";
          } 
        } else {
          this.not_done = write_command("550", LOC.G("%RMD-not_found%"));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
          return "%RMD-not_found%";
        } 
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
    } else {
      this.not_done = write_command("550", LOC.G("%RMD-bad%"));
      doErrorEvent(new Exception(uiSG("lastLog")));
      uiVG("failed_commands").addElement((new Date()).getTime());
      return "%RMD-bad%";
    } 
    return "";
  }
  
  public void changeProxyToCurrentDir(Properties item) throws Exception {}
  
  public String do_SIZE() throws Exception {
    String the_dir = uiSG("current_dir");
    if (!uiSG("the_command_data").equals("")) {
      if (uiSG("the_command_data").startsWith("/")) {
        the_dir = uiSG("the_command_data");
      } else {
        the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
      } 
      the_dir = Common.dots(the_dir);
      if (the_dir.equals("/"))
        the_dir = SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
    } 
    String parentPath = this.uVFS.getRootVFS(the_dir, -1);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_item(the_dir);
    if (!check_access_privs(the_dir, uiSG("the_command"), dir_item) && (uiSG("the_command_data").toUpperCase().endsWith(".BIN") || uiSG("the_command_data").toUpperCase().endsWith(".ZIP"))) {
      uiPUT("the_command_data", uiSG("the_command_data").substring(0, uiSG("the_command_data").lastIndexOf(".")));
      the_dir = the_dir.substring(0, the_dir.lastIndexOf("."));
    } 
    if (check_access_privs(the_dir, uiSG("the_command"), item) && the_dir.indexOf(":filetree") < 0) {
      changeProxyToCurrentDir(item);
      if (item != null && item.getProperty("type", "").equals("FILE")) {
        this.not_done = write_command("213", item.getProperty("size"));
        return "";
      } 
      this.not_done = write_command("550", LOC.G("%SIZE-wrong%"));
      return "%SIZE-wrong%";
    } 
    this.not_done = write_command("550", LOC.G("File not found, or access denied."));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%SIZE-bad%";
  }
  
  public String do_MDTM() throws Exception {
    boolean mfmt = uiSG("the_command").equals("MFMT");
    uiPUT("the_command", "MDTM");
    uiPUT("last_logged_command", "MDTM");
    String the_dir = uiSG("current_dir");
    String dateNumber = "";
    if (!uiSG("the_command_data").equals("")) {
      if (mfmt) {
        dateNumber = uiSG("the_command_data").split(" ")[0];
        uiPUT("the_command_data", uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1));
      } else {
        if (uiSG("the_command_data").lastIndexOf(" ") >= 0)
          dateNumber = uiSG("the_command_data").substring(uiSG("the_command_data").lastIndexOf(" ")).trim(); 
        try {
          Long.parseLong(dateNumber);
          if (dateNumber.length() > 5) {
            dateNumber = (new StringBuffer(String.valueOf(Long.parseLong(dateNumber.trim())))).toString();
            uiPUT("the_command_data", uiSG("the_command_data").substring(0, uiSG("the_command_data").length() - dateNumber.length()).trim());
          } else {
            dateNumber = "";
          } 
        } catch (Exception e) {
          if (uiSG("the_command_data").indexOf(" ") >= 0)
            dateNumber = uiSG("the_command_data").substring(0, uiSG("the_command_data").indexOf(" ")).trim(); 
          try {
            Long.parseLong(dateNumber);
            if (dateNumber.length() > 5) {
              dateNumber = (new StringBuffer(String.valueOf(Long.parseLong(dateNumber.trim())))).toString();
              uiPUT("the_command_data", uiSG("the_command_data").substring(dateNumber.length() + 1));
            } else {
              dateNumber = "";
            } 
            Log.log("FTP_SERVER", 1, "4:dateNumber=" + dateNumber);
          } catch (Exception ee) {
            dateNumber = "";
          } 
        } 
      } 
      if (uiSG("the_command_data").startsWith("/")) {
        the_dir = uiSG("the_command_data");
      } else {
        the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
      } 
      the_dir = Common.dots(the_dir);
      if (the_dir.equals("/"))
        the_dir = SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
    } 
    Calendar cal = new GregorianCalendar();
    Properties item = this.uVFS.get_item(the_dir);
    if (check_access_privs(the_dir, uiSG("the_command"), item) && the_dir.indexOf(":filetree") < 0) {
      changeProxyToCurrentDir(item);
      GenericClient c = this.uVFS.getClient(item);
      try {
        Properties stat = c.stat((new VRL(item.getProperty("url"))).getPath());
        long time = Long.parseLong(stat.getProperty("modified"));
        if (dateNumber.trim().length() > 0)
          try {
            if (check_access_privs(the_dir, "STOR")) {
              Date d = this.sdf_yyyyMMddHHmmss.parse(dateNumber.trim());
              cal.setTime(d);
              cal.add(11, IG("timezone_offset"));
              d = new Date(cal.getTime().getTime());
              if (!ServerStatus.BG("disable_mdtm_modifications"))
                c.mdtm((new VRL(item.getProperty("url"))).getPath(), d.getTime()); 
              stat = c.stat((new VRL(item.getProperty("url"))).getPath());
              time = Long.parseLong(stat.getProperty("modified"));
              cal.setTime(new Date(time));
              cal.add(11, IG("timezone_offset"));
              d = new Date(cal.getTime().getTime());
              if (ServerStatus.BG("mdtm_gmt"))
                this.sdf_yyyyMMddHHmmss.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT"))); 
              if (mfmt) {
                this.not_done = write_command("213", "Modify=" + this.sdf_yyyyMMddHHmmss.format(d) + "; " + uiSG("the_command_data"));
              } else {
                this.not_done = write_command("213", this.sdf_yyyyMMddHHmmss.format(d));
              } 
              Common.trackSync("CHANGE", the_dir, null, false, Long.parseLong(item.getProperty("size", "0")), time, SG("root_dir"), item.getProperty("privs"), uiSG("clientid"), "");
              return "";
            } 
          } catch (Exception e) {
            Log.log("FTP_SERVER", 1, e);
            time = -1L;
          }  
        if (time >= 0L || ServerStatus.BG("disable_mdtm_modifications")) {
          cal.setTime(new Date(time));
          cal.add(11, IG("timezone_offset"));
          Date d = new Date(cal.getTime().getTime());
          if (ServerStatus.BG("mdtm_gmt"))
            this.sdf_yyyyMMddHHmmss.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT"))); 
          if (mfmt) {
            this.not_done = write_command("213", "Modify=" + this.sdf_yyyyMMddHHmmss.format(d) + "; " + uiSG("the_command_data"));
            return "";
          } 
          this.not_done = write_command("213", this.sdf_yyyyMMddHHmmss.format(d));
          return "";
        } 
        this.not_done = write_command("550", LOC.G("%MDTM-wrong%"));
        uiVG("failed_commands").addElement((new Date()).getTime());
        return "%MDTM-wrong%";
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
    } 
    this.not_done = write_command("550", LOC.G("File not found, or access denied."));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%MDTM-bad%";
  }
  
  public String get_PWD() {
    try {
      return uiSG("current_dir").substring(SG("root_dir").length() - 1);
    } catch (Exception e) {
      return uiSG("current_dir");
    } 
  }
  
  public String do_CWD() throws Exception {
    uiPUT("the_command", "CWD");
    uiPUT("last_logged_command", "CWD");
    if (uiSG("the_command_data").trim().equals(""))
      uiPUT("the_command_data", "."); 
    String originalCommandData = uiSG("the_command_data");
    uiPUT("the_command_data", Common.url_decode(uiSG("the_command_data")));
    if (uiSG("the_command_data").startsWith("//"))
      uiPUT("the_command_data", uiSG("the_command_data").substring(1)); 
    if (uiSG("the_command_data").startsWith("//") && !uiSG("the_command_data").endsWith("/"))
      uiPUT("the_command_data", String.valueOf(uiSG("the_command_data").substring(1)) + "/"); 
    if (uiSG("the_command_data").equals("~")) {
      uiPUT("current_dir", Common.replace_str(String.valueOf(SG("root_dir")) + this.user_info.getProperty("default_current_dir", ""), "//", "/"));
      this.not_done = write_command("250", LOC.G("\"$0\" CWD command successful.", get_PWD()));
    } else {
      String the_dir = uiSG("current_dir");
      if (uiSG("the_command_data").startsWith("/")) {
        the_dir = uiSG("the_command_data");
      } else {
        the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
      } 
      if (!the_dir.endsWith("/"))
        the_dir = String.valueOf(the_dir) + "/"; 
      if (the_dir.equals("/"))
        the_dir = SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
      the_dir = Common.dots(the_dir);
      if (!the_dir.startsWith(SG("root_dir")))
        the_dir = SG("root_dir"); 
      Properties item = this.uVFS.get_item(the_dir);
      if (check_access_privs(the_dir, uiSG("the_command"), item)) {
        if (item == null && !the_dir.equals("/")) {
          this.not_done = write_command("550", LOC.G("$0 : No such file or directory.", the_dir));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
          if (ServerStatus.BG("slow_directory_scanners") && uiVG("failed_commands").size() - 10 > 0)
            Thread.sleep((100 * (uiVG("failed_commands").size() - 10))); 
          return String.valueOf(the_dir) + ": " + LOC.G("No such file or directory.");
        } 
        if (the_dir.equals("/")) {
          uiPUT("current_dir", SG("root_dir"));
          this.not_done = write_command("250", LOC.G("\"$0\" CWD command successful.", get_PWD()));
          return "";
        } 
        if (item.getProperty("type").equals("DIR")) {
          if (the_dir.equals(""))
            the_dir = SG("root_dir"); 
          uiPUT("current_dir", the_dir);
          this.not_done = write_command("250", LOC.G("\"$0\" CWD command successful.", get_PWD()));
          if (!(new VRL(item.getProperty("url"))).getProtocol().equalsIgnoreCase("virtual")) {
            GenericClient c = this.uVFS.getClient(item);
            try {
              if (c.getConfig("server_type", "").toUpperCase().indexOf("UNIX") < 0 && c.getConfig("server_type", "").toUpperCase().indexOf("WIND") < 0)
                c.doCommand("CWD " + originalCommandData); 
            } finally {
              c = this.uVFS.releaseClient(c);
            } 
          } 
          return "";
        } 
        this.not_done = write_command("550", "\"" + uiSG("the_command_data") + "\": " + LOC.G("No such file or directory."));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        if (ServerStatus.BG("slow_directory_scanners") && uiVG("failed_commands").size() - 10 > 0)
          Thread.sleep((100 * (uiVG("failed_commands").size() - 10))); 
        return "%CWD-not found%";
      } 
      this.not_done = write_command("550", "\"" + uiSG("the_command_data") + "\": " + LOC.G("No such file or directory."));
      doErrorEvent(new Exception(uiSG("lastLog")));
      uiVG("failed_commands").addElement((new Date()).getTime());
      if (ServerStatus.BG("slow_directory_scanners") && uiVG("failed_commands").size() - 10 > 0)
        Thread.sleep((100 * (uiVG("failed_commands").size() - 10))); 
      return "%CWD-not found%";
    } 
    return "";
  }
  
  public String do_ChangePass(String theUser, String new_password) {
    String result = LOC.G("Password not changed.");
    if (!Common.checkPasswordRequirements(new_password, this.user.getProperty("password_history", "")).equals(""))
      return String.valueOf(LOC.G("ERROR:")) + " " + Common.checkPasswordRequirements(new_password, this.user.getProperty("password_history", "")); 
    boolean ok = false;
    if (!new_password.equals(uiSG("current_password"))) {
      String old_password = uiSG("current_password");
      String response = "";
      try {
        VFS realVfs = this.uVFS;
        if (this.expired_uVFS != null)
          realVfs = this.expired_uVFS; 
        Properties dir_item = realVfs.get_item(getRootDir(null, realVfs, this.user, false), -1);
        VRL vrl = new VRL(dir_item.getProperty("url"));
        if (!vrl.getProtocol().equalsIgnoreCase("file") && !vrl.getProtocol().equalsIgnoreCase("virtual") && ServerStatus.BG("change_remote_password")) {
          GenericClient c = realVfs.getClient(dir_item);
          try {
            if (c instanceof com.crushftp.client.HTTPClient) {
              String split = Common.makeBoundary();
              response = c.doCommand("SITE PASS " + split + " " + old_password + split + new_password);
            } else {
              response = c.doCommand("SITE PASS " + new_password);
              if (response.startsWith("2"))
                response = c.doCommand("SITE PASS " + new_password); 
            } 
            if (response.startsWith("2"))
              ok = true; 
          } finally {
            c = this.uVFS.releaseClient(c);
          } 
        } else {
          UserTools.changeUsername(uiSG("listen_ip_port"), theUser, theUser, ServerStatus.thisObj.common_code.encode_pass(new_password, ServerStatus.SG("password_encryption")));
          Properties tempUser = UserTools.ut.getUser(uiSG("listen_ip_port"), theUser, false);
          if (tempUser.containsKey("expire_password_when")) {
            Calendar gc = new GregorianCalendar();
            gc.setTime(new Date());
            gc.add(5, IG("expire_password_days"));
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
            String s = sdf.format(gc.getTime());
            tempUser.put("expire_password_when", s);
            tempUser.put("expire_password_days", (new StringBuffer(String.valueOf(IG("expire_password_days")))).toString());
          } 
          tempUser.put("auto_set_pass", "false");
          tempUser.put("password", ServerStatus.thisObj.common_code.encode_pass(new_password, ServerStatus.SG("password_encryption")));
          tempUser.put("password_history", Common.getPasswordHistory(new_password, tempUser.getProperty("password_history", "")));
          UserTools.writeUser(uiSG("listen_ip_port"), theUser, tempUser);
          ok = true;
          response = "214 " + LOC.G("Password changed.");
        } 
      } catch (Exception e) {
        Log.log("AUTH", 0, e);
        return e.getMessage();
      } 
      result = response.substring(4);
      if (ok) {
        uiPUT("current_password", new_password);
        Properties p = new Properties();
        p.put("user_name", theUser);
        p.put("old_password", old_password);
        p.put("new_password", new_password);
        runPlugin("changePass", p);
        ServerStatus.thisObj.runAlerts("password_change", this);
      } 
    } 
    return result;
  }
  
  public void kill_active_socks() {
    while (this.data_socks.size() > 0) {
      try {
        ((Socket)this.data_socks.remove(0)).close();
      } catch (Exception e) {
        Log.log("FTP_SERVER", 1, e);
      } 
    } 
  }
  
  public String fixupDir(String user_dir) {
    if (user_dir == null) {
      String str = Common.url_decode(uiSG("current_dir"));
      if (!uiSG("the_command_data").equals("")) {
        if (uiSG("the_command_data").startsWith("/")) {
          str = uiSG("the_command_data");
        } else {
          str = String.valueOf(str) + uiSG("the_command_data");
        } 
        str = Common.dots(str);
        if (str.equals("/"))
          str = SG("root_dir"); 
        if (str.toUpperCase().startsWith("/") && !str.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
          str = String.valueOf(SG("root_dir")) + str.substring(1); 
      } 
      uiPUT("the_command_data", Common.dots(uiSG("the_command_data")));
      return str;
    } 
    String the_dir = Common.dots(Common.url_decode(user_dir));
    if (the_dir.equals("/"))
      the_dir = SG("root_dir"); 
    if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
      the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
    return the_dir;
  }
  
  public void setupRootDir(String domain, boolean reset) throws Exception {
    if (this.user != null) {
      Properties session = null;
      try {
        session = (Properties)SharedSession.find("crushftp.sessions").get(getId());
      } catch (Exception e) {
        Common.debug(1, e);
      } 
      if (session == null) {
        session = new Properties();
        SharedSession.find("crushftp.sessions").put(getId(), session);
        SharedSessionReplicated.flushWait();
      } 
      if (ServerStatus.BG("jailproxy") && session.getProperty("default_current_dir_unlocked", "false").equals("false")) {
        this.user.put("root_dir", getRootDir(domain, this.uVFS, this.user, reset));
      } else {
        this.user.put("root_dir", getRootDir(domain, this.uVFS, this.user, reset, false));
        uiPUT("current_dir", String.valueOf(SG("root_dir")) + this.uVFS.user_info.getProperty("default_current_dir", "/").substring(1));
      } 
      this.user_info.put("root_dir", this.user.getProperty("root_dir"));
    } 
  }
  
  public static String getRootDir(String domain, VFS uVFS, Properties user, boolean reset) {
    return getRootDir(domain, uVFS, user, reset, true);
  }
  
  public static String getRootDir(String domain, VFS uVFS, Properties user, boolean reset, boolean include_default_current_dir) {
    String root_dir = "/";
    Vector v = new Vector();
    try {
      uVFS.getListing(v, "/");
    } catch (Exception exception) {}
    if (reset)
      uVFS.reset(); 
    Properties dir_item = null;
    Properties names = new Properties();
    int x;
    for (x = 0; x < v.size(); x++) {
      Properties p = v.elementAt(x);
      if (p.getProperty("type").equalsIgnoreCase("DIR") && dir_item == null)
        dir_item = p; 
      if (!names.containsKey(p.getProperty("name")))
        names.put(p.getProperty("name"), ""); 
    } 
    if (dir_item != null && names.size() == 1)
      root_dir = "/" + dir_item.getProperty("name") + "/"; 
    if (include_default_current_dir && !uVFS.user_info.getProperty("default_current_dir", "").equals("/") && !uVFS.user_info.getProperty("default_current_dir", "").equals("")) {
      root_dir = String.valueOf(root_dir) + uVFS.user_info.getProperty("default_current_dir").substring(1);
      if (!root_dir.endsWith("/"))
        root_dir = String.valueOf(root_dir) + "/"; 
    } 
    if (domain != null && !domain.equals("") && user != null && user.get("domain_root_list") != null) {
      v = (Vector)user.get("domain_root_list");
      for (x = 0; x < v.size(); x++) {
        Properties p = v.elementAt(x);
        if (Common.do_search(p.getProperty("domain"), domain, false, 0)) {
          String path = p.getProperty("path");
          if (!path.startsWith("/"))
            path = "/" + path; 
          if (!path.endsWith("/"))
            path = String.valueOf(path) + "/"; 
          root_dir = path;
          break;
        } 
      } 
    } 
    return root_dir;
  }
  
  public int IG(String data) {
    int x = 0;
    try {
      x = Integer.parseInt(this.user.getProperty(data));
    } catch (Exception exception) {}
    return x;
  }
  
  public String SG(String data) {
    String return_data = null;
    if (this.user != null)
      return_data = this.user.getProperty(data); 
    if (return_data == null)
      if (data.equals("root_dir")) {
        return_data = "/";
      } else {
        try {
          return_data = ServerStatus.SG(data);
        } catch (Exception e) {
          return_data = "";
        } 
      }  
    return return_data;
  }
  
  public boolean BG(String data) {
    boolean test = false;
    try {
      test = this.user.getProperty(data).equals("true");
    } catch (Exception exception) {}
    return test;
  }
  
  public String stripRoot(String s) {
    if (s.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
      s = s.substring(SG("root_dir").length() - 1); 
    return s;
  }
  
  public void setupCurrentDir(String path) {
    if (path.startsWith("/WebInterface/function/"))
      return; 
    uiPUT("current_dir", Common.dots(path));
    if (!uiSG("current_dir").toUpperCase().startsWith(SG("root_dir").toUpperCase()))
      uiPUT("current_dir", String.valueOf(SG("root_dir")) + (uiSG("current_dir").startsWith("/") ? uiSG("current_dir").substring(1) : uiSG("current_dir"))); 
    if (uiSG("current_dir").indexOf("\\") >= 0)
      uiPUT("current_dir", uiSG("current_dir").replace('\\', '/')); 
    if (!uiSG("current_dir").startsWith("/"))
      uiPUT("current_dir", "/" + uiSG("current_dir")); 
  }
  
  public void killSession() {
    SharedSession.find("crushftp.usernames").remove(String.valueOf(Common.getPartialIp(uiSG("user_ip"))) + "_" + getId() + "_user");
    SharedSession.find("crushftp.usernames").remove(String.valueOf(Common.getPartialIp(uiSG("user_ip"))) + "_" + getId() + "_userProp");
    SharedSession.find("crushftp.usernames").remove(String.valueOf(Common.getPartialIp(uiSG("user_ip"))) + "_" + getId() + "_vfs");
    uiPUT("CrushAuth", "");
    SharedSession.find("crushftp.usernames").remove(String.valueOf(Common.getPartialIp(uiSG("user_ip"))) + "_" + getId() + "_user");
    SharedSession.find("crushftp.usernames").remove(String.valueOf(Common.getPartialIp(uiSG("user_ip"))) + "_" + getId() + "_userProp");
    SharedSession.find("crushftp.usernames").remove(String.valueOf(Common.getPartialIp(uiSG("user_ip"))) + "_" + getId() + "_vfs");
    SharedSession.find("crushftp.usernames").remove("127.0.0.1_" + getId() + "_user");
    SharedSession.find("crushftp.usernames").remove("127.0.0.1_" + getId() + "_userProp");
    SharedSession.find("crushftp.usernames").remove("127.0.0.1_" + getId() + "_vfs");
  }
}
