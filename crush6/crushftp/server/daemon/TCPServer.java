package crushftp.server.daemon;

import crushftp.gui.LOC;
import crushftp.handlers.Log;
import crushftp.server.QuickConnect;
import crushftp.server.ServerStatus;
import crushftp.server.Worker;
import crushftp.server.ssh.SSHDaemon;
import java.util.Properties;

public class TCPServer extends GenericServer {
  SSHDaemon sshd;
  
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
      while (this.socket_created && this.die_now.length() == 0) {
        this.busyMessage = "";
        this.sock = this.server_sock.accept();
        if (this.sshd != null)
          this.server_item.put("ssh_local_port", (new StringBuffer(String.valueOf(this.sshd.localSSHPort))).toString()); 
        synchronized (this) {
          this.connected_users++;
        } 
        this.connection_number++;
        if (this.listen_ip.equals("lookup") || this.listen_ip.equals("manual"))
          this.the_ip = ServerStatus.SG("discovered_ip"); 
        if (this.server_item.getProperty("serverType", "").toUpperCase().equals("FTP") || this.server_item.getProperty("serverType", "").toUpperCase().equals("FTPS"))
          if (!this.server_item.getProperty("server_ip", "").trim().equals("") && !this.server_item.getProperty("server_ip", "").trim().equals("auto") && !this.server_item.getProperty("server_ip", "").trim().equals("lookup"))
            this.the_ip = this.server_item.getProperty("server_ip", "");  
        updateStatus();
        QuickConnect quicky = new QuickConnect(this, this.listen_port, this.sock, this.the_ip, String.valueOf(this.listen_ip) + "_" + this.listen_port, this.server_item);
        try {
          if (!Worker.startWorker(quicky, String.valueOf(this.listen_ip) + "_" + this.listen_port + " --> " + this.the_ip)) {
            this.sock.close();
            quicky = null;
            synchronized (this) {
              this.connected_users--;
            } 
          } 
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
        } 
        ServerStatus.siPUT("thread_pool_available", (new StringBuffer(String.valueOf(ServerStatus.thisObj.availableWorkers.size()))).toString());
        ServerStatus.siPUT("thread_pool_busy", (new StringBuffer(String.valueOf(ServerStatus.thisObj.busyWorkers.size()))).toString());
      } 
    } catch (Exception e) {
      if (e.getMessage().indexOf("socket closed") < 0) {
        Log.log("SERVER", 1, e);
      } else {
        Log.log("SERVER", 3, e);
      } 
    } 
    if (this.sshd != null)
      this.sshd.stop(); 
    this.socket_created = false;
    updateStatus();
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
        this.server_item.put("display", String.valueOf(this.busyMessage.equals("") ? "" : ("(" + this.busyMessage + ")  ")) + LOC.G("$0 is running, $1 users connected. Connections Processed : $2", ServerStatus.thisObj.common_code.setServerStatus(this.server_item, this.the_ip).trim(), (new StringBuffer(String.valueOf(this.connected_users))).toString(), (new StringBuffer(String.valueOf(this.connection_number))).toString()));
      } else {
        this.server_item.put("display", String.valueOf(this.busyMessage.equals("") ? "" : ("(" + this.busyMessage + ")  ")) + LOC.G("$0 is stopped, $1 users still connected.  Connections Processed : $2", ServerStatus.thisObj.common_code.setServerStatus(this.server_item, this.the_ip).trim(), (new StringBuffer(String.valueOf(this.connected_users))).toString(), (new StringBuffer(String.valueOf(this.connection_number))).toString()));
      } 
    } 
  }
}
