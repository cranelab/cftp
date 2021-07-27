package crushftp.server.daemon;

import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.net.ssl.SSLServerSocket;

public class GenericServer implements Runnable {
  public Thread thread = null;
  
  public ServerSocket server_sock = null;
  
  public int listen_port = 21;
  
  public boolean socket_created = false;
  
  Socket sock = null;
  
  public StringBuffer die_now = new StringBuffer();
  
  public String listen_ip = "lookup";
  
  public String the_ip = "";
  
  public Properties server_item = null;
  
  String busyMessage = "";
  
  boolean port_denied = false;
  
  int connection_number = 0;
  
  public int connected_users = 0;
  
  String startingPropertiesHash = "";
  
  boolean restart = false;
  
  boolean started = false;
  
  static boolean warned = false;
  
  public static Object updateServerStatuses = new Object();
  
  public static Vector serverPorts = new Vector();
  
  public GenericServer(Properties server_item) {
    this.server_item = server_item;
  }
  
  public void run() {}
  
  public static GenericServer buildServer(Properties server_item) {
    if (server_item.getProperty("serverType", "").equalsIgnoreCase("CUSTOM"))
      return new CustomServer(server_item); 
    if (server_item.getProperty("serverType", "").equalsIgnoreCase("SERVERBEAT"))
      return new ServerBeat(server_item); 
    if (server_item.getProperty("serverType", "").toUpperCase().indexOf("DMZ") >= 0)
      return new DMZServer(server_item); 
    return new TCPServer(server_item);
  }
  
  public void init() {
    this.thread = Thread.currentThread();
    this.listen_port = Integer.parseInt(this.server_item.getProperty("port"));
    this.listen_ip = this.server_item.getProperty("ip");
    serverPorts.addElement((new StringBuffer(String.valueOf(this.listen_port))).toString());
    this.the_ip = this.listen_ip;
    this.startingPropertiesHash = getPropertiesHash((Properties)this.server_item.clone());
    this.started = true;
  }
  
  public void getSocket() {
    try {
      while (!this.socket_created && this.die_now.length() == 0) {
        try {
          if (this.server_item.getProperty("serverType", "false").toUpperCase().equals("FTPS") || this.server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTPS")) {
            this.busyMessage = LOC.G("SSL Cert Error");
            String keystore = ServerStatus.SG("cert_path");
            String certPass = ServerStatus.SG("globalKeystoreCertPass");
            String keystorePass = ServerStatus.SG("globalKeystorePass");
            boolean needClientAuth = ServerStatus.BG("needClientAuth");
            if (!this.server_item.getProperty("customKeystore", "").equals(""))
              keystore = this.server_item.getProperty("customKeystore", ""); 
            if (!this.server_item.getProperty("customKeystoreCertPass", "").equals(""))
              certPass = this.server_item.getProperty("customKeystoreCertPass", ""); 
            if (!this.server_item.getProperty("customKeystorePass", "").equals(""))
              keystorePass = this.server_item.getProperty("customKeystorePass", ""); 
            if (!this.server_item.getProperty("needClientAuth", "false").equals("false"))
              needClientAuth = this.server_item.getProperty("needClientAuth", "").equals("true"); 
            if (this.listen_ip.equals("lookup")) {
              this.server_sock = ServerStatus.thisObj.common_code.getServerSocket(this.listen_port, null, keystore, keystorePass, certPass, ServerStatus.SG("disabled_ciphers"), needClientAuth, 1000);
            } else {
              this.server_sock = ServerStatus.thisObj.common_code.getServerSocket(this.listen_port, this.listen_ip, keystore, keystorePass, certPass, ServerStatus.SG("disabled_ciphers"), needClientAuth, 1000);
            } 
            this.busyMessage = "";
            String[] ciphers = ((SSLServerSocket)this.server_sock).getSupportedCipherSuites();
            String cipherStr = "";
            for (int x = 0; x < ciphers.length; x++) {
              if (x > 0)
                cipherStr = String.valueOf(cipherStr) + ","; 
              cipherStr = String.valueOf(cipherStr) + ciphers[x];
            } 
            ServerStatus.siPUT("ciphers", cipherStr);
          } else if (this.server_item.getProperty("serverType", "false").toUpperCase().indexOf("DMZ") >= 0) {
            this.server_sock = new ServerSocket(0);
            this.server_sock.close();
          } else if (this.listen_ip.equals("lookup")) {
            this.server_sock = new ServerSocket(this.listen_port, 1000, null);
          } else {
            this.server_sock = new ServerSocket(this.listen_port, 1000, InetAddress.getByName(this.listen_ip));
          } 
          this.socket_created = true;
          ServerStatus.thisObj.server_started(this.listen_ip, this.listen_port);
          updateStatus();
        } catch (Exception ee) {
          this.busyMessage = String.valueOf(this.busyMessage) + ":" + ee.toString();
          int sleepAmount = 30000;
          Log.log("SERVER", 2, ee);
          if (ee.toString().indexOf("Permission denied") >= 0) {
            this.busyMessage = LOC.G("Port $0 is reserved since its below 1024.  Authenticate as root to fix.", (new StringBuffer(String.valueOf(this.listen_port))).toString());
            if (!warned) {
              warned = true;
              Log.log("SERVER", 0, this.busyMessage);
            } 
          } else if (ee.toString().indexOf("assign requested") >= 0) {
            this.busyMessage = LOC.G("The IP specified ($0) is invalid.  This machine is not using that IP.  Please update in preferences.", this.listen_ip);
            if (!warned) {
              warned = true;
              Log.log("SERVER", 0, this.busyMessage);
            } 
          } else if (ee.toString().indexOf("Address already in use") >= 0) {
            this.busyMessage = LOC.G("Port $0 is already in use by another process.", (new StringBuffer(String.valueOf(this.listen_port))).toString());
            if (!warned) {
              warned = true;
              Log.log("SERVER", 0, this.busyMessage);
            } 
          } 
          String data = "";
          if (this.busyMessage.equals("")) {
            try {
              Socket testSock = new Socket("127.0.0.1", this.listen_port);
              BufferedReader in = new BufferedReader(new InputStreamReader(testSock.getInputStream()));
              data = " ";
              while (data.indexOf("220 ") < 0 && data.indexOf("null") < 0)
                data = String.valueOf(data) + in.readLine(); 
              testSock.getOutputStream().write("QUIT\r\n".getBytes());
              in.close();
              testSock.close();
            } catch (Exception eee) {
              Thread.sleep(1000L);
            } 
            data = data.toUpperCase();
            if (data.indexOf("LUKEMFTPD") >= 0 || data.indexOf("TNFTPD") >= 0) {
              this.busyMessage = LOC.G("Turn off 'FTP Sharing' in System Prefs");
              Log.log("SERVER", 0, LOC.G("Disable built in FTP server first..."));
              try {
                Runtime.getRuntime().exec("open /System/Library/PreferencePanes/SharingPref.prefPane");
              } catch (Exception exception) {}
            } else {
              this.busyMessage = String.valueOf(this.listen_ip) + ":" + this.listen_port + " - " + LOC.G("Port in use by some other server : $0", ee.toString());
            } 
            if (!this.busyMessage.equals(""))
              Log.log("SERVER", 0, this.busyMessage); 
          } 
          if (!this.busyMessage.equals("")) {
            Thread.sleep(sleepAmount);
            this.busyMessage = "";
            continue;
          } 
          int loopNum = 30;
          while (loopNum >= 0) {
            this.busyMessage = LOC.G("Port $0 in use! Retrying $1 secs...", (new StringBuffer(String.valueOf(this.listen_port))).toString(), (new StringBuffer(String.valueOf(loopNum--))).toString());
            Thread.sleep(1000L);
          } 
          this.busyMessage = "";
        } 
      } 
      if (this.socket_created && this.die_now.length() == 0 && this.listen_ip.equals("lookup"))
        this.the_ip = ServerStatus.SG("discovered_ip"); 
    } catch (InterruptedException interruptedException) {}
  }
  
  public void updateStatus() {}
  
  public void updateStatusInit() {
    if (!this.started)
      return; 
    String hash2 = getPropertiesHash((Properties)this.server_item.clone());
    if (!this.startingPropertiesHash.equals(hash2) && !this.startingPropertiesHash.equals("")) {
      this.startingPropertiesHash = Common.makeBoundary();
      this.restart = true;
      this.die_now.append(System.currentTimeMillis());
      try {
        this.server_sock.close();
      } catch (Exception exception) {}
    } 
    boolean found = false;
    for (int x = 0; x < ((Vector)ServerStatus.server_settings.get("server_list")).size(); x++) {
      Properties pp = ((Vector)ServerStatus.server_settings.get("server_list")).elementAt(x);
      if (pp.getProperty("ip").equals(this.server_item.getProperty("ip")) && pp.getProperty("port").equals(this.server_item.getProperty("port")))
        found = true; 
    } 
    if (!found) {
      this.die_now.append(System.currentTimeMillis());
      try {
        this.server_sock.close();
      } catch (Exception exception) {}
    } 
    ServerStatus.thisObj.server_info.put("server_list", ServerStatus.server_settings.get("server_list"));
    this.server_item.put("running", (new StringBuffer(String.valueOf(this.socket_created))).toString());
    this.server_item.put("connected_users", (new StringBuffer(String.valueOf(this.connected_users))).toString());
    this.server_item.put("connection_number", (new StringBuffer(String.valueOf(this.connection_number))).toString());
    this.server_item.put("busyMessage", this.busyMessage);
  }
  
  public static String getPropertiesHash(Properties p) {
    p.remove("display");
    p.remove("connected_users");
    p.remove("connection_number");
    p.remove("running");
    p.remove("busyMessage");
    p.remove("ssh_local_port");
    p.remove("current_pasv_port");
    p.remove("require_secure");
    p.remove("server_ip");
    p.remove("allow_webdav");
    p.remove("linkedServer");
    p.remove("require_encryption");
    p.remove("pasv_ports");
    p.remove("explicit_ssl");
    p.remove("explicit_tls");
    p.remove("ftp_aware_router");
    p.remove("https_redirect");
    p.remove("commandDelayInterval");
    Enumeration keys = p.keys();
    Vector v = new Vector();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      v.addElement(key);
    } 
    Object[] a = v.toArray();
    Arrays.sort(a);
    String s = "";
    for (int x = 0; x < a.length; x++) {
      Object o = p.get(a[x]);
      if (o instanceof String)
        s = String.valueOf(s) + "," + a[x] + o.toString(); 
    } 
    if (p.getProperty("serverType", "false").toUpperCase().equals("FTPS") || p.getProperty("serverType", "FTP").toUpperCase().equals("HTTPS"))
      s = String.valueOf(s) + "," + ServerStatus.SG("cert_path") + ServerStatus.SG("globalKeystoreCertPass") + ServerStatus.SG("globalKeystorePass"); 
    s = String.valueOf(s) + ";";
    return s;
  }
}
