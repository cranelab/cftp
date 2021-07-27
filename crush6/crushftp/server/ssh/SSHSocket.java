package crushftp.server.ssh;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.daemon.GenericServer;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;

public class SSHSocket extends Socket {
  Socket sockIn = null;
  
  Properties p = null;
  
  Socket sockOut = new Socket();
  
  Socket sockOut2 = null;
  
  public SSHSocket(GenericServer server, Properties p, int localSSHPort) {
    this.p = p;
    this.sockIn = (Socket)p.get("socket");
    p.put("socket", this);
    int localPort = 0;
    try {
      BufferedInputStream bis = new BufferedInputStream(this.sockIn.getInputStream());
      this.sockOut2 = new Socket("127.0.0.1", localSSHPort);
      this.sockOut.connect(this.sockOut2.getRemoteSocketAddress());
      localPort = this.sockOut.getLocalPort();
      ServerSessionSSH.connectionLookup.put((new StringBuffer(String.valueOf(localPort))).toString(), p);
      Log.log("SSH_SERVER", 2, "SSH PORT CONNECTOR:" + localPort);
      Properties server_item = (Properties)p.get("server_item");
      this.sockIn.setSoTimeout(Integer.parseInt(server_item.getProperty("ssh_session_timeout", "300")) * 1000);
      this.sockOut.setSoTimeout(Integer.parseInt(server_item.getProperty("ssh_session_timeout", "300")) * 1000);
      Common.streamCopier(bis, this.sockOut.getOutputStream());
      Common.streamCopier(this.sockOut.getInputStream(), this.sockIn.getOutputStream(), false);
      Thread.sleep(5000L);
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } finally {
      Log.log("SSH_SERVER", 2, "SSH PORT CONNECTOR close RELEASE:" + localPort);
      ServerSessionSSH.connectionLookup.remove((new StringBuffer(String.valueOf(localPort))).toString());
      p.clear();
      synchronized (server) {
        if (server.connected_users > 0)
          server.connected_users--; 
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
