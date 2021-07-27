package crushftp.server.daemon;

import com.crushftp.client.Common;
import com.crushftp.client.File_S;
import com.crushftp.client.Worker;
import com.crushftp.ssl.sni.SNITool;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.PortMapper;
import crushftp.server.QuickConnect;
import crushftp.server.ServerStatus;
import crushftp.server.ssh.SSHDaemon;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.Vector;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class TCPServer extends GenericServer {
  SSHDaemon sshd;
  
  PortMapper portmapper = new PortMapper();
  
  Vector sockets = new Vector();
  
  TCPServer thisObj = this;
  
  SSLSocketFactory factory = null;
  
  public TCPServer(Properties server_item) {
    super(server_item);
  }
  
  public void run() {
    init();
    try {
      getSocket();
      if (this.socket_created && this.die_now.length() == 0)
        if (this.server_item.getProperty("serverType", "").toUpperCase().equals("SFTP")) {
          this.sshd = new SSHDaemon(this.server_item);
          this.sshd.startup();
        }  
      this.server_sock.setSoTimeout(30000);
      long last_map = 0L;
      while (this.socket_created && this.die_now.length() == 0) {
        this.busyMessage = "";
        if (this.server_item.getProperty("configure_external", "false").equals("true") && System.currentTimeMillis() - last_map > 3300000L) {
          boolean mapped = false;
          try {
            mapped = this.portmapper.mapPort(this.listen_ip, Integer.parseInt(this.server_item.getProperty("port")), 3600000);
          } catch (Exception e) {
            Log.log("SERVER", 0, e);
          } 
          this.server_item.put("external_mapped", (new StringBuffer(String.valueOf(mapped))).toString());
          last_map = System.currentTimeMillis();
        } 
        try {
          this.sock = this.server_sock.accept();
        } catch (SocketTimeoutException e) {
          continue;
        } 
        this.sockets.addElement(this.sock);
        Runnable sr = new Runnable(this) {
            final TCPServer this$0;
            
            public void run() {
              String proxied_ip = "";
              Socket sock2 = this.this$0.sockets.remove(0);
              if (this.this$0.server_item.getProperty("proxy_header", "false").equals("true"))
                proxied_ip = this.this$0.readProxyIP(sock2, this.this$0.thisObj); 
              if (this.this$0.server_item.getProperty("proxy_header", "false").equals("true") && (this.this$0.server_item.getProperty("serverType", "").toUpperCase().equals("FTPS") || this.this$0.server_item.getProperty("serverType", "").toUpperCase().equals("HTTPS")) && !this.this$0.sni_enabled)
                try {
                  if (this.this$0.factory == null)
                    this.this$0.factory = ServerStatus.thisObj.common_code.getSSLContext(this.this$0.keystore, String.valueOf(this.this$0.keystore) + "_trust", this.this$0.keystorePass, this.this$0.certPass, "TLS", this.this$0.needClientAuth, true).getSocketFactory(); 
                  sock2 = this.this$0.factory.createSocket(sock2, sock2.getInetAddress().getHostAddress(), sock2.getPort(), true);
                  Common.configureSSLTLSSocket(sock2);
                  Common.setEnabledCiphers(ServerStatus.SG("disabled_ciphers"), (SSLSocket)sock2, null);
                  ((SSLSocket)sock2).setNeedClientAuth(this.this$0.needClientAuth);
                  ((SSLSocket)sock2).setUseClientMode(false);
                } catch (Exception e) {
                  Log.log("SERVER", 0, e);
                }  
              if (this.this$0.sni_enabled)
                sock2 = TCPServer.doSni(sock2, this.this$0.keystore, this.this$0.keystorePass, this.this$0.certPass, this.this$0.needClientAuth, (StringBuffer)null); 
              if (this.this$0.sshd != null)
                this.this$0.server_item.put("ssh_local_port", (new StringBuffer(String.valueOf(this.this$0.sshd.localSSHPort))).toString()); 
              synchronized (this.this$0.thisObj) {
                this.this$0.connected_users++;
                if (this.this$0.connected_users < 0)
                  this.this$0.connected_users = 1; 
              } 
              this.this$0.connection_number++;
              if (this.this$0.listen_ip.equals("lookup") || this.this$0.listen_ip.equals("manual"))
                this.this$0.the_ip = ServerStatus.SG("discovered_ip"); 
              if (this.this$0.server_item.getProperty("serverType", "").toUpperCase().equals("FTP") || this.this$0.server_item.getProperty("serverType", "").toUpperCase().equals("FTPS"))
                if (!this.this$0.server_item.getProperty("server_ip", "").trim().equals("") && !this.this$0.server_item.getProperty("server_ip", "").trim().equals("auto") && (this.this$0.server_item.getProperty("server_ip").trim().charAt(0) > '9' || this.this$0.server_item.getProperty("server_ip", "").indexOf(",") >= 0) && !this.this$0.server_item.getProperty("server_ip", "").trim().equals("lookup"))
                  this.this$0.the_ip = this.this$0.server_item.getProperty("server_ip", "");  
              this.this$0.updateStatus();
              (new QuickConnect(this.this$0.thisObj, this.this$0.listen_port, sock2, this.this$0.the_ip, String.valueOf(this.this$0.listen_ip) + "_" + this.this$0.listen_port, this.this$0.server_item, proxied_ip)).run();
            }
          };
        try {
          if (!Worker.startWorker(sr, String.valueOf(this.listen_ip) + "_" + this.listen_port + " --> " + this.the_ip)) {
            this.sockets.remove(this.sock);
            this.sock.close();
            synchronized (this.thisObj) {
              this.connected_users--;
              if (this.connected_users < 0)
                this.connected_users = 0; 
            } 
          } 
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
        } 
        ServerStatus.siPUT("thread_pool_available", (new StringBuffer(String.valueOf(Worker.availableWorkers.size()))).toString());
        ServerStatus.siPUT("thread_pool_busy", (new StringBuffer(String.valueOf(Worker.busyWorkers.size()))).toString());
      } 
    } catch (Throwable e) {
      if (e.getMessage() == null || e.getMessage().indexOf("socket closed") < 0) {
        Log.log("SERVER", 1, e);
      } else {
        Log.log("SERVER", 3, e);
      } 
      if (e.toUpperCase().indexOf("INTERRUPTED") < 0)
        try {
          Properties info = new Properties();
          info.put("alert_type", "server_port_error");
          info.put("alert_error", e);
          info.put("alert_msg", this.server_item.getProperty("display"));
          ServerStatus.thisObj.runAlerts("server_port_error", info, info, null);
        } catch (Exception ee) {
          Log.log("BAN", 1, ee);
        }  
    } 
    if (this.sshd != null)
      this.sshd.stop(); 
    this.socket_created = false;
    updateStatus();
    this.portmapper.clearAll();
    if (this.restart) {
      this.restart = false;
      this.die_now = new StringBuffer();
      (new Thread(this)).start();
    } 
  }
  
  public void updateStatus() {
    synchronized (updateServerStatuses) {
      if (!this.started)
        return; 
      updateStatusInit();
      if (this.socket_created) {
        if (this.server_item.getProperty("configure_external", "false").equals("true")) {
          this.server_item.put("display", String.valueOf(this.busyMessage.equals("") ? "" : ("(" + this.busyMessage + ")  ")) + LOC.G("$0 is running, $1 users connected. Port Forwarded : $2, Connections Processed : $3", ServerStatus.thisObj.common_code.setServerStatus(this.server_item, this.the_ip).trim(), (new StringBuffer(String.valueOf(this.connected_users))).toString(), this.server_item.getProperty("external_mapped", "false"), (new StringBuffer(String.valueOf(this.connection_number))).toString()));
        } else {
          this.server_item.put("display", String.valueOf(this.busyMessage.equals("") ? "" : ("(" + this.busyMessage + ")  ")) + LOC.G("$0 is running, $1 users connected. Connections Processed : $2", ServerStatus.thisObj.common_code.setServerStatus(this.server_item, this.the_ip).trim(), (new StringBuffer(String.valueOf(this.connected_users))).toString(), (new StringBuffer(String.valueOf(this.connection_number))).toString()));
        } 
      } else {
        this.server_item.put("display", String.valueOf(this.busyMessage.equals("") ? "" : ("(" + this.busyMessage + ")  ")) + LOC.G("$0 is stopped, $1 users still connected.  Connections Processed : $2", ServerStatus.thisObj.common_code.setServerStatus(this.server_item, this.the_ip).trim(), (new StringBuffer(String.valueOf(this.connected_users))).toString(), (new StringBuffer(String.valueOf(this.connection_number))).toString()));
      } 
    } 
  }
  
  public static Socket doSni(Socket sock2, String keystore, String keystorePass, String certPass, boolean needClientAuth, StringBuffer keystore_used) {
    try {
      sock2.setSoTimeout(5000);
      Properties result = SNITool.check(sock2);
      sock2.setSoTimeout(0);
      if (result.containsKey("error"))
        throw (Exception)result.remove("error"); 
      Vector names = (Vector)result.get("names");
      Log.log("SERVER", 2, (String)names);
      String keystore2 = keystore;
      ByteArrayInputStream bais = (ByteArrayInputStream)result.remove("buffer");
      for (int x = 0; x < names.size(); x++) {
        String s = names.elementAt(x).toString();
        String type = s.split(":")[0];
        String host = s.split(":")[1].trim().toLowerCase();
        if (type.equals("0")) {
          host = Common.dots(host);
          File_S f = new File_S(keystore);
          f = new File_S(String.valueOf(f.getParentFile().getAbsolutePath()) + "/" + host + "_" + f.getName());
          if (f.exists()) {
            keystore2 = f.getPath();
            if (keystore_used != null)
              keystore_used.append(keystore2); 
            Log.log("SERVER", 1, "Using keystore " + keystore2 + " for connection:" + sock2);
            break;
          } 
        } 
      } 
      SSLSocketFactory factory = ServerStatus.thisObj.common_code.getSSLContext(keystore2, String.valueOf(keystore2) + "_trust", keystorePass, certPass, "TLS", needClientAuth, true).getSocketFactory();
      sock2 = SNITool.makeSocket(sock2, factory, bais, ServerStatus.SG("disabled_ciphers"));
      Common.configureSSLTLSSocket(sock2);
      Common.setEnabledCiphers(ServerStatus.SG("disabled_ciphers"), (SSLSocket)sock2, null);
      ((SSLSocket)sock2).setNeedClientAuth(needClientAuth);
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
      try {
        sock2.close();
      } catch (Exception exception) {}
    } 
    return sock2;
  }
  
  public String readProxyIP(Socket sock, GenericServer server) {
    try {
      InputStream in = sock.getInputStream();
      String line = "";
      byte[] b = new byte[1];
      int read = 1;
      while (!line.endsWith("\r\n") && read > 0) {
        read = in.read(b);
        if (read > 0)
          line = String.valueOf(line) + new String(b); 
      } 
      return line.split(" ")[2].trim();
    } catch (Exception e) {
      Log.log("SERVER", 2, e);
      synchronized (server) {
        server.connected_users--;
        if (server.connected_users < 0)
          server.connected_users = 0; 
      } 
      server.updateStatus();
      return null;
    } 
  }
}
