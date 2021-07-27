package crushftp.server.ssh;

import com.crushftp.client.Common;
import crushftp.gui.LOC;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.server.ServerSessionSSH;
import crushftp.server.ServerStatus;
import crushftp.server.daemon.GenericServer;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.Properties;

public class SSHSocket extends Socket {
  public Socket sockIn = null;
  
  Properties p = null;
  
  Socket sockOut = new Socket();
  
  Socket sockOut2 = null;
  
  public static Object socket_lock = new Object();
  
  public SSHSocket(GenericServer server, Properties p, int localSSHPort) {
    this.p = p;
    this.sockIn = (Socket)p.get("socket");
    p.put("socket", this);
    Properties server_item = (Properties)p.get("server_item");
    ServerStatus.thisObj.append_log(String.valueOf(ServerStatus.thisObj.logDateFormat.format(new Date())) + "|" + "[SFTP:" + server_item.getProperty("ip", "0.0.0.0") + ":" + server_item.getProperty("port", "21") + "][" + p.getProperty("user_number") + "] " + LOC.G("Accepting connection from") + ": " + p.getProperty("user_ip") + ":" + this.sockIn.getPort() + "\r\n", "ACCEPT");
    int localPort = 0;
    try {
      BufferedInputStream bis = new BufferedInputStream(this.sockIn.getInputStream());
      synchronized (socket_lock) {
        this.sockOut2 = new Socket("127.0.0.1", localSSHPort);
      } 
      this.sockOut.connect(this.sockOut2.getRemoteSocketAddress());
      localPort = this.sockOut.getLocalPort();
      ServerSessionSSH.connectionLookup.put((new StringBuffer(String.valueOf(localPort))).toString(), p);
      Log.log("SSH_SERVER", 2, "SSH PORT CONNECTOR:" + localPort);
      this.sockIn.setSoTimeout(Integer.parseInt(server_item.getProperty("ssh_session_timeout", "300")) * 1000);
      this.sockOut.setSoTimeout(Integer.parseInt(server_item.getProperty("ssh_session_timeout", "300")) * 1000);
      Common.streamCopier(this.sockIn, this.sockOut, bis, this.sockOut.getOutputStream(), true, true, true);
      Common.streamCopier(this.sockIn, this.sockOut, this.sockOut.getInputStream(), this.sockIn.getOutputStream(), false, true, true);
      Thread.sleep(100L);
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } finally {
      ServerStatus.thisObj.append_log("[SFTP:" + server_item.getProperty("ip", "0.0.0.0") + ":" + server_item.getProperty("port", "21") + "][" + p.getProperty("user_number") + "] " + LOC.G("Disconnected") + ": " + p.getProperty("user_ip") + ":" + this.sockIn.getPort() + "\r\n", "QUIT");
      Log.log("SSH_SERVER", 2, "SSH PORT CONNECTOR close RELEASE:" + localPort);
      ServerSessionSSH.connectionLookup.remove((new StringBuffer(String.valueOf(localPort))).toString());
      SSHCrushAuthentication8.endSession((SessionCrush)p.get("session"));
      if (p.get("session_id") != null)
        ServerSessionSSH.sessionLookup.remove(p.get("session_id")); 
      p.clear();
      synchronized (server) {
        server.connected_users--;
        if (server.connected_users < 0)
          server.connected_users = 0; 
      } 
    } 
  }
  
  public void close() throws IOException {
    this.sockIn.close();
    this.sockOut.close();
    if (this.sockOut2 != null)
      this.sockOut2.close(); 
    super.close();
  }
}
