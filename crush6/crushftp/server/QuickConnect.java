package crushftp.server;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.daemon.GenericServer;
import java.awt.Toolkit;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSocket;

public class QuickConnect implements Runnable {
  public int listen_port = 21;
  
  String CRLF = "\r\n";
  
  Socket sock;
  
  GenericServer server = null;
  
  String listen_ip = "127.0.0.1";
  
  String listen_ip_port = "lookup_21";
  
  Properties server_item = null;
  
  public static Object syncUserNumbers = new Object();
  
  public QuickConnect(GenericServer server, int listen_port, Socket sock, String listen_ip, String listen_ip_port, Properties server_item) {
    this.listen_port = listen_port;
    this.sock = sock;
    this.server = server;
    this.listen_ip = listen_ip;
    this.listen_ip_port = listen_ip_port;
    this.server_item = server_item;
  }
  
  public void run() {
    try {
      if (this.sock instanceof SSLSocket)
        ((SSLSocket)this.sock).addHandshakeCompletedListener(new HandshakeCompletedListener(this) {
              final QuickConnect this$0;
              
              public void handshakeCompleted(HandshakeCompletedEvent event) {
                ((SSLSocket)this.this$0.sock).setEnabledCipherSuites(new String[0]);
                Log.log("SERVER", 2, "CVE-2009-3555 fixed.  Ciphers removed from SSL socket.");
              }
            }); 
      ServerSession serverSession = null;
      String ip = this.sock.getInetAddress().getHostAddress();
      if (ip.indexOf(".") < 0)
        ip = "0.0.0.0"; 
      Vector server_ips = (Vector)this.server_item.get("ip_restrictions");
      boolean ipAllowed = ServerStatus.thisObj.common_code.check_ip((Vector)ServerStatus.server_settings.get("ip_restrictions"), ip);
      boolean notHammer = ServerStatus.thisObj.check_hammer_ip(ip);
      if (this.server_item.getProperty("serverType", "").toUpperCase().equals("HTTP") || this.server_item.getProperty("serverType", "").toUpperCase().equals("HTTPS"))
        notHammer = true; 
      if (ipAllowed && notHammer && (server_ips == null || ServerStatus.thisObj.common_code.check_ip(server_ips, ip))) {
        if (ServerStatus.IG("hammer_banning") > 0 && this.server_item.getProperty("serverType", "").toUpperCase().indexOf("HTTP") < 0)
          ServerStatus.siPUT("hammer_history", String.valueOf(ServerStatus.siSG("hammer_history")) + ip + this.CRLF); 
        if (ServerStatus.BG("beep_connect"))
          Toolkit.getDefaultToolkit().beep(); 
        if (this.server_item.getProperty("serverType", "").toUpperCase().equals("SFTP")) {
          Properties p = new Properties();
          p.put("user_number", (new StringBuffer(String.valueOf(getUserLoginNum()))).toString());
          p.put("server_item", this.server_item);
          p.put("socket", this.sock);
          p.put("user_ip", ip);
          p.put("user_port", (new StringBuffer(String.valueOf(this.listen_port))).toString());
          p.put("listen_ip", this.listen_ip);
          p.put("listen_port", (new StringBuffer(String.valueOf(this.listen_port))).toString());
          p.put("listen_ip_port", this.listen_ip_port);
          p.put("connectionTime", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
          return;
        } 
        if (this.server_item.getProperty("serverType", "").toUpperCase().equals("HTTP") || this.server_item.getProperty("serverType", "").toUpperCase().equals("HTTPS")) {
          int j = getUserLoginNum();
          ServerSessionHTTP5_2 serverSessionHTTP5_2 = new ServerSessionHTTP5_2(this.sock, j, ip, this.listen_port, this.listen_ip, this.listen_ip_port, this.server_item);
          if (this.server_item.getProperty("serverType", "FTP").toUpperCase().equals("HTTPS"))
            serverSessionHTTP5_2.thisSession.uiPUT("secure", "true"); 
          serverSessionHTTP5_2.give_thread_pointer(Thread.currentThread());
          Thread.currentThread().setName(String.valueOf(j) + "-" + ip);
          Thread.currentThread().setPriority(1);
          serverSessionHTTP5_2.run();
          return;
        } 
        if (this.server_item.getProperty("serverType", "").toUpperCase().startsWith("SOCKS")) {
          if (ServerStatus.siIG("enterprise_level") <= 0)
            throw new Exception("SOCKS5 only valid for Enterprise licenses."); 
          int j = getUserLoginNum();
          serverSession = new ServerSession(this.sock, j, ip, this.listen_port, this.listen_ip, this.listen_ip_port, this.server_item);
          serverSession.give_thread_pointer(Thread.currentThread());
          Thread.currentThread().setName(String.valueOf(j) + "-" + ip);
          Thread.currentThread().setPriority(1);
          byte[] head = new byte[3];
          InputStream in = this.sock.getInputStream();
          OutputStream out = this.sock.getOutputStream();
          in.read(head);
          String host = "";
          int port = 0;
          if (head[0] == 4) {
            port = head[2] * 256 + in.read();
            host = String.valueOf(Math.abs(in.read())) + "." + Math.abs(in.read()) + "." + Math.abs(in.read()) + "." + Math.abs(in.read());
            do {
            
            } while (in.read() > 0);
            out.write(new byte[] { 0, 90, 1, 1, 2, 2, 2, 2 });
            out.flush();
          } else if (head[0] == 5) {
            out.write(new byte[] { head[0] });
            out.flush();
            in.read(head);
            int type = in.read();
            if (type == 1) {
              byte[] b = new byte[4];
              in.read(b);
              host = String.valueOf(b[0]) + "." + b[1] + "." + b[2] + "." + b[3];
              port = in.read() * 256 + in.read();
            } else if (type == 3) {
              byte[] b = new byte[in.read()];
              in.read(b);
              host = new String(b);
              port = in.read() * 256 + in.read();
            } 
            out.write(new byte[] { 5, 1 });
            out.flush();
          } 
          Log.log("SERVER", 1, "SOCKS:" + host + ":" + port);
          Socket sock2 = new Socket(host, port);
          Common.streamCopier(sock2.getInputStream(), out, true, true, true);
          Common.streamCopier(in, sock2.getOutputStream(), false, true, true);
          serverSession.do_kill();
          return;
        } 
        int i = getUserLoginNum();
        serverSession = new ServerSession(this.sock, i, ip, this.listen_port, this.listen_ip, this.listen_ip_port, this.server_item);
        serverSession.give_thread_pointer(Thread.currentThread());
        Thread.currentThread().setName(String.valueOf(i) + "-" + ip);
        Thread.currentThread().setPriority(1);
        serverSession.run();
        return;
      } 
      this.server.connected_users--;
      this.server.updateStatus();
      if (this.server_item.getProperty("serverType", "").toUpperCase().equals("HTTP") || this.server_item.getProperty("serverType", "").toUpperCase().equals("HTTPS")) {
        OutputStream out = this.sock.getOutputStream();
        out.write("HTTP/1.1 200 BANNED\r\n".getBytes());
        String msg = "Your IP is banned, no further requests will be processed from this IP.\r\n";
        out.write(("Content-Length: " + msg.length() + "\r\n").getBytes());
        out.write("\r\n".getBytes());
        out.write(msg.getBytes());
        out.close();
      } else if (this.server_item.getProperty("serverType", "").toUpperCase().equals("FTP")) {
        OutputStream out = this.sock.getOutputStream();
        String msg = "421 Your IP is banned, no further requests will be processed from this IP.\r\n";
        out.write(msg.getBytes());
        out.close();
      } 
      this.sock.close();
      try {
        ServerStatus.thisObj.append_log("!" + (new Date()).toString() + "!  ---" + ServerStatus.SG("BANNED IP CONNECTION TERMINATED") + "---:" + ip, "DENIAL");
      } catch (Exception exception) {}
      ServerStatus.put_in("failed_logins", ServerStatus.IG("failed_logins") + 1);
      Thread.sleep(100L);
    } catch (Exception e) {
      try {
        this.sock.close();
      } catch (Exception exception) {}
      Log.log("SERVER", 1, e);
    } 
  }
  
  public static int getUserLoginNum() {
    synchronized (syncUserNumbers) {
      int i = ServerStatus.siIG("user_login_num");
      if (i >= 2147483640)
        i = 0; 
      ServerStatus.siPUT("user_login_num", (new StringBuffer(String.valueOf(i + 1))).toString());
      return i + 1;
    } 
  }
}
