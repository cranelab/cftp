package crushftp.server;

import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.server.daemon.GenericServer;
import java.awt.Toolkit;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class QuickConnect implements Runnable {
  public int listen_port = 21;
  
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
      String ip = this.sock.getInetAddress().getHostAddress();
      if (this.server_item.getProperty("proxy_header", "false").equals("true"))
        if (ServerStatus.BG("v8_beta")) {
          InputStream in = this.sock.getInputStream();
          String line = "";
          byte[] b = new byte[1];
          int read = 1;
          while (!line.endsWith("\r\n") && read > 0) {
            read = in.read(b);
            if (read > 0)
              line = String.valueOf(line) + new String(b); 
          } 
          ip = line.split(" ")[2].trim();
        }  
      if (ip.indexOf(".") < 0)
        ip = "0.0.0.0"; 
      if (validate_ip(ip, this.server_item)) {
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
          ServerSessionHTTP serverSessionHTTP5_2 = new ServerSessionHTTP(this.sock, j, ip, this.listen_port, this.listen_ip, this.listen_ip_port, this.server_item);
          serverSessionHTTP5_2.give_thread_pointer(Thread.currentThread());
          Thread.currentThread().setName(String.valueOf(j) + "-" + ip);
          Thread.currentThread().setPriority(1);
          serverSessionHTTP5_2.run();
          return;
        } 
        if (this.server_item.getProperty("serverType", "").toUpperCase().startsWith("PORTFORWARD")) {
          int j = getUserLoginNum();
          Socket sock2 = null;
          try {
            if (Common.System2.get("crushftp.dmz.queue.sock") != null) {
              Vector socket_queue = (Vector)Common.System2.get("crushftp.dmz.queue.sock");
              if (socket_queue == null)
                socket_queue = (Vector)Common.System2.get("crushftp.dmz.queue"); 
              Properties mySock = new Properties();
              mySock.put("type", "GET:SOCKET");
              mySock.put("port", String.valueOf(this.server_item.getProperty("dest_ip")) + ":" + this.server_item.getProperty("dest_port"));
              mySock.put("data", new Properties());
              mySock.put("id", String.valueOf(Common.makeBoundary(10)) + (new Date()).getTime());
              mySock.put("sticky_token", "");
              mySock.put("created", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
              mySock.put("need_response", "true");
              socket_queue.addElement(mySock);
              int loops = 0;
              while (loops++ < 3000) {
                if (mySock.containsKey("socket"))
                  break; 
                try {
                  Thread.sleep(10L);
                } catch (InterruptedException interruptedException) {}
              } 
              socket_queue.remove(mySock);
              if (mySock.get("socket") == null)
                throw new Exception("failure: Waited 30 seconds for DMZ socket, giving up."); 
              sock2 = (Socket)mySock.remove("socket");
            } else {
              sock2 = new Socket(this.server_item.getProperty("dest_ip"), Integer.parseInt(this.server_item.getProperty("dest_port")));
            } 
            Common.streamCopier(this.sock, sock2, this.sock.getInputStream(), sock2.getOutputStream(), true, false, false);
            Common.streamCopier(this.sock, sock2, sock2.getInputStream(), this.sock.getOutputStream(), false, false, false);
          } catch (Exception e) {
            Log.log("SERVER", 0, (String)e);
          } 
          this.sock.close();
          if (sock2 != null)
            sock2.close(); 
          return;
        } 
        if (this.server_item.getProperty("serverType", "").toUpperCase().startsWith("SOCKS")) {
          if (ServerStatus.siIG("enterprise_level") <= 0)
            throw new Exception("SOCKS5 only valid for Enterprise licenses."); 
          int j = getUserLoginNum();
          SessionCrush serverSession = new SessionCrush(this.sock, j, ip, this.listen_port, this.listen_ip, this.listen_ip_port, this.server_item);
          Thread.currentThread().setName(String.valueOf(j) + "-" + ip);
          Thread.currentThread().setPriority(1);
          byte[] head = new byte[3];
          InputStream in = this.sock.getInputStream();
          OutputStream out = this.sock.getOutputStream();
          StringBuffer proxyHeader = new StringBuffer();
          int loops = 0;
          while (in.available() < 3 && loops++ < 100)
            Thread.sleep(100L); 
          int read = in.read(head);
          proxyHeader.append("(read:" + read + ":" + head[0] + "," + head[1] + "," + head[2] + ")");
          String host = "";
          int port = 0;
          int type = -1;
          int socksmode = head[0];
          if (socksmode == 4) {
            port = head[2] * 256 + in.read();
            host = String.valueOf(Math.abs(in.read())) + "." + Math.abs(in.read()) + "." + Math.abs(in.read()) + "." + Math.abs(in.read());
            do {
            
            } while (in.read() > 0);
            out.write(new byte[] { 0, 90, 1, 1, 2, 2, 2, 2 });
            out.flush();
          } else if (socksmode == 5) {
            out.write(new byte[] { head[0] });
            out.flush();
            loops = 0;
            while (in.available() < 3 && loops++ < 100)
              Thread.sleep(100L); 
            read = in.read(head);
            proxyHeader.append("(read:" + read + ":" + head[0] + "," + head[1] + "," + head[2] + ")");
            type = in.read();
            proxyHeader.append("(type" + type + ")");
            if (type == 0) {
              type = in.read();
              proxyHeader.append("(type" + type + ")");
            } 
            if (type == 1) {
              byte[] b = new byte[4];
              loops = 0;
              while (in.available() < b.length && loops++ < 100)
                Thread.sleep(100L); 
              host = String.valueOf(in.read()) + "." + in.read() + "." + in.read() + "." + in.read();
              port = in.read() * 256 + in.read();
            } else if (type == 3) {
              byte[] b = new byte[in.read()];
              loops = 0;
              while (in.available() < b.length && loops++ < 100)
                Thread.sleep(100L); 
              in.read(b);
              host = new String(b);
              port = in.read() * 256 + in.read();
            } else {
              proxyHeader.append(":unknown:");
              read = in.read(head);
              proxyHeader.append("(read:" + read + ":" + head[0] + "," + head[1] + "," + head[2] + ")");
              read = in.read(head);
              proxyHeader.append("(read:" + read + ":" + head[0] + "," + head[1] + "," + head[2] + ")");
              read = in.read(head);
              proxyHeader.append("(read:" + read + ":" + head[0] + "," + head[1] + "," + head[2] + ")");
            } 
            out.write(new byte[] { 5, 1 });
            out.flush();
          } 
          Log.log("SERVER", 1, "SOCKS:header:" + proxyHeader.toString());
          Log.log("SERVER", 1, "SOCKS:" + host + ":" + port);
          Socket sock2 = new Socket(host, port);
          Common.streamCopier(this.sock, sock2, sock2.getInputStream(), out, true, true, true);
          Common.streamCopier(this.sock, sock2, in, sock2.getOutputStream(), false, true, true);
          serverSession.do_kill(null);
          return;
        } 
        int i = getUserLoginNum();
        ServerSessionFTP serverSessionFTP = new ServerSessionFTP(this.sock, i, ip, this.listen_port, this.listen_ip, this.listen_ip_port, this.server_item);
        serverSessionFTP.give_thread_pointer(Thread.currentThread());
        Thread.currentThread().setName(String.valueOf(i) + "-" + ip);
        Thread.currentThread().setPriority(1);
        serverSessionFTP.run();
        return;
      } 
      this.server.connected_users--;
      this.server.updateStatus();
      if (this.server_item.getProperty("serverType", "").toUpperCase().equals("HTTP") || this.server_item.getProperty("serverType", "").toUpperCase().equals("HTTPS")) {
        OutputStream out = this.sock.getOutputStream();
        out.write("HTTP/1.1 200 BANNED\r\n".getBytes());
        String msg = "Your IP is banned, no further requests will be processed from this IP (" + ip + ").\r\n";
        out.write(("Content-Length: " + msg.length() + "\r\n").getBytes());
        out.write("\r\n".getBytes());
        out.write(msg.getBytes());
        out.close();
      } else if (this.server_item.getProperty("serverType", "").toUpperCase().equals("FTP")) {
        OutputStream out = this.sock.getOutputStream();
        String msg = "421 Your IP is banned, no further requests will be processed from this IP (" + ip + ").\r\n";
        out.write(msg.getBytes());
        out.close();
      } 
      this.sock.close();
      try {
        ServerStatus.thisObj.append_log(String.valueOf(ServerStatus.thisObj.logDateFormat.format(new Date())) + "|---" + ServerStatus.SG("BANNED IP CONNECTION TERMINATED") + "---:" + ip, "DENIAL");
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
  
  static boolean validate_ip(String ip, Properties server_item) throws Exception {
    boolean ipAllowed = Common.check_ip((Vector)ServerStatus.server_settings.get("ip_restrictions"), ip);
    boolean notHammer = false;
    if (server_item.getProperty("serverType", "").toUpperCase().indexOf("HTTP") >= 0) {
      notHammer = ServerStatus.thisObj.check_hammer_ip_http(ip);
    } else {
      notHammer = ServerStatus.thisObj.check_hammer_ip(ip);
    } 
    Vector server_ips = (Vector)server_item.get("ip_restrictions");
    if (ipAllowed && notHammer && (server_ips == null || Common.check_ip(server_ips, ip))) {
      String addon = (server_item.getProperty("serverType", "").toUpperCase().indexOf("HTTP") >= 0) ? "_http" : "";
      if (ServerStatus.IG("hammer_banning" + addon) > 0)
        ServerStatus.siPUT("hammer_history" + addon, String.valueOf(ServerStatus.siSG("hammer_history" + addon)) + ip + "\r\n"); 
      return true;
    } 
    return false;
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
