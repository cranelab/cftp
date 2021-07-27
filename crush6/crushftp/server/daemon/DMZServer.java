package crushftp.server.daemon;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SharedSession;
import crushftp.handlers.SharedSessionReplicated;
import crushftp.handlers.SyncTools;
import crushftp.handlers.UserTools;
import crushftp.server.AdminControls;
import crushftp.server.QuickConnect;
import crushftp.server.ServerSession;
import crushftp.server.ServerSessionAJAX5_2;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
import crushftp.server.Worker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.Vector;
import javax.net.ssl.SSLSocketFactory;

public class DMZServer extends GenericServer {
  public static Properties dmzInstances = new Properties();
  
  public static Properties dmzResponses = new Properties();
  
  DMZServer thisObj = this;
  
  Exception error = null;
  
  Socket read_sock = null;
  
  Socket write_sock = null;
  
  Vector responseQueue = new Vector();
  
  int messages_received = 0;
  
  int messages_sent = 0;
  
  public DMZServer(Properties server_item) {
    super(server_item);
  }
  
  public static void sendCommand(String instance_name, Properties data, String type, String id) {
    DMZServer dmz = (DMZServer)dmzInstances.get(instance_name);
    try {
      if (dmz.write_sock != null) {
        Properties p = new Properties();
        p.put("type", type.toUpperCase());
        p.put("data", data);
        p.put("id", id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.reset();
        out.writeObject(p);
        out.close();
        byte[] b = baos.toByteArray();
        synchronized (dmz.write_sock) {
          dmz.write_sock.setSoTimeout(10000);
          dmz.write_sock.getOutputStream().write((String.valueOf(b.length) + ":").getBytes());
          dmz.write_sock.getOutputStream().write(b);
          dmz.write_sock.getOutputStream().flush();
          int i = dmz.write_sock.getInputStream().read();
          if (i != 1)
            throw new Exception("Invalid response received from DMZ send:" + i); 
          dmz.messages_sent++;
        } 
      } 
    } catch (Exception e) {
      dmz.error = e;
    } 
  }
  
  public static Properties getResponse(String id, int timeout) throws Exception {
    long start = System.currentTimeMillis();
    while (!dmzResponses.containsKey(id) && start > System.currentTimeMillis() - (1000 * timeout))
      Thread.sleep(100L); 
    if (!dmzResponses.containsKey(id))
      return null; 
    return (Properties)dmzResponses.remove(id);
  }
  
  public void run() {
    init();
    try {
      if (ServerStatus.siIG("enterprise_level") <= 0) {
        this.busyMessage = "DMZ only valid for Enterprise licenses.";
        throw new Exception(this.busyMessage);
      } 
      if (Common.System2.get("crushftp.dmz.queue") != null) {
        this.busyMessage = "DMZ port cannot operate on a DMZ server, only on an internal server.";
        throw new Exception(this.busyMessage);
      } 
      getSocket();
      this.server_sock.close();
      for (int x = 0; x < UserTools.anyPassTokens.size(); x++)
        SharedSessionReplicated.send("", "anyPassToken", "anyPassToken", UserTools.anyPassTokens.elementAt(x).toString()); 
      this.busyMessage = "Finding DMZ...";
      SSLSocketFactory factory = ServerStatus.thisObj.common_code.getSSLContext("builtin", null, "crushftp", "crushftp", "TLS", false, true).getSocketFactory();
      this.read_sock = factory.createSocket(this.listen_ip, this.listen_port);
      this.write_sock = factory.createSocket(this.listen_ip, this.listen_port);
      this.read_sock.setSoTimeout(10000);
      this.write_sock.setSoTimeout(10000);
      this.read_sock.getOutputStream().write("W".getBytes());
      this.read_sock.getOutputStream().flush();
      this.write_sock.getOutputStream().write("R".getBytes());
      this.write_sock.getOutputStream().flush();
      dmzInstances.put(this.server_item.getProperty("server_item_name"), this);
      this.busyMessage = "Starting DMZ...";
      startSocketConnectors();
      File f = new File(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs_" + this.server_item.getProperty("server_item_name") + ".XML");
      Properties instance_server_settings = (Properties)Common.readXMLObject(f);
      instance_server_settings.put("tunnels_dmz", ServerStatus.VG("tunnels"));
      instance_server_settings.put("miniURLs_dmz", ServerStatus.VG("miniURLs"));
      sendFileToMemory(instance_server_settings.getProperty("cert_path", ""));
      Vector instance_servers = (Vector)instance_server_settings.get("server_list");
      for (int i = 0; i < instance_servers.size(); i++) {
        sendFileToMemory(((Properties)instance_servers.elementAt(i)).getProperty("customKeystore", ""));
        sendFileToMemory(((Properties)instance_servers.elementAt(i)).getProperty("ssh_rsa_key", ""));
        sendFileToMemory(((Properties)instance_servers.elementAt(i)).getProperty("ssh_dsa_key", ""));
      } 
      sendCommand(this.server_item.getProperty("server_item_name"), instance_server_settings, "PUT:SERVER_SETTINGS", "");
      if (this.error != null)
        throw this.error; 
      this.busyMessage = "";
      long lastToken = 0L;
      while (this.socket_created && this.die_now.length() == 0) {
        if (System.currentTimeMillis() - lastToken > 30000L) {
          sendToken();
          lastToken = System.currentTimeMillis();
        } 
        if (this.error != null)
          throw this.error; 
        Thread.sleep(500L);
      } 
    } catch (InterruptedException e) {
      Log.log("SERVER", 0, e);
      this.die_now.append(System.currentTimeMillis());
    } catch (ConnectException e) {
      Log.log("SERVER", 2, e);
      this.die_now.append(System.currentTimeMillis());
      this.restart = true;
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
      this.die_now.append(System.currentTimeMillis());
      this.restart = true;
    } 
    this.socket_created = false;
    try {
      this.server_sock.close();
    } catch (Exception exception) {}
    updateStatus();
    if (this.restart) {
      try {
        Thread.sleep(1000L);
      } catch (Exception exception) {}
      ServerStatus.thisObj.start_this_server(ServerStatus.thisObj.main_servers.indexOf(this));
    } 
  }
  
  public void sendToken() {
    Properties system_prop = new Properties();
    system_prop.put("key", "crushftp.proxy.anyPassToken");
    system_prop.put("val", UserTools.anyPassTokens.elementAt(0).toString());
    sendCommand(this.server_item.getProperty("server_item_name"), system_prop, "PUT:SYSTEM.PROPERTIES", "");
    SharedSessionReplicated.send("", "anyPassToken", "anyPassToken", UserTools.anyPassTokens.elementAt(0).toString());
  }
  
  public void processResponse(Properties p) throws Exception {
    Log.log("DMZ", 2, "READ:" + this.server_item.getProperty("server_item_name") + ":" + p.getProperty("type"));
    if (p.getProperty("type").equalsIgnoreCase("RESPONSE")) {
      p.put("received", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      dmzResponses.put(p.getProperty("id"), p);
    } else if (p.getProperty("type").equalsIgnoreCase("GET:USER")) {
      Vector public_keys = null;
      Properties user = UserTools.ut.getUser(this.server_item.getProperty("linkedServer", ""), p.getProperty("username"), true);
      VFS uVFS = UserTools.ut.getVFS(this.server_item.getProperty("linkedServer", ""), p.getProperty("username"));
      if (user != null)
        public_keys = UserTools.buildPublicKeys(p.getProperty("username"), user); 
      if (user != null) {
        user.remove("filePublicEncryptionKey");
        user.remove("fileEncryptionKey");
        user.remove("fileDecryptionKey");
        p.put("public_keys", public_keys);
        p.put("user", user);
        p.put("vfs", uVFS.homes);
      } 
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:SHARE")) {
      Properties request = (Properties)p.remove("request");
      String crushAuth = p.getProperty("crushAuth");
      VFS uVFS = (VFS)SharedSession.find("crushftp.usernames").get(String.valueOf(Common.getPartialIp(p.getProperty("user_ip"))) + "_" + crushAuth + "_vfs");
      VFS priorVFS = uVFS.thisSession.uVFS;
      uVFS.thisSession.uVFS = uVFS;
      Vector path_items = new Vector();
      String[] paths = Common.url_decode(request.getProperty("paths")).split("\r\n");
      for (int x = 0; x < paths.length; x++) {
        String the_dir = paths[x].trim();
        if (!the_dir.equals("")) {
          if (the_dir.startsWith(uVFS.thisSession.SG("root_dir")))
            the_dir = the_dir.substring(uVFS.thisSession.SG("root_dir").length() - 1); 
          uVFS.thisSession.setupCurrentDir(the_dir);
          Log.log("HTTP_SERVER", 2, "Sharing:" + the_dir + "  vs.  " + uVFS.thisSession.uiSG("current_dir"));
          Properties item = uVFS.get_item(uVFS.thisSession.uiSG("current_dir"));
          Log.log("HTTP_SERVER", 2, "Sharing:" + item);
          VRL vrl = new VRL(item.getProperty("url"));
          Properties stat = null;
          GenericClient c = uVFS.getClient(item);
          try {
            stat = c.stat(vrl.getPath());
          } finally {
            c = uVFS.releaseClient(c);
          } 
          stat.put("privs", item.getProperty("privs"));
          path_items.addElement(stat);
        } 
      } 
      String response = ServerSessionAJAX5_2.createShare(path_items, request, (Vector)uVFS.thisSession.user.get("web_customizations"), uVFS.thisSession.uiSG("user_name"), uVFS.thisSession.server_item.getProperty("linkedServer"), uVFS.thisSession.user, uVFS.thisSession.date_time);
      uVFS.thisSession.uVFS = priorVFS;
      p.put("object_response", response);
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:MANAGESHARES")) {
      p.remove("request");
      String crushAuth = p.getProperty("crushAuth");
      VFS uVFS = (VFS)SharedSession.find("crushftp.usernames").get(String.valueOf(Common.getPartialIp(p.getProperty("user_ip"))) + "_" + crushAuth + "_vfs");
      VFS priorVFS = uVFS.thisSession.uVFS;
      uVFS.thisSession.uVFS = uVFS;
      String response = ServerSessionAJAX5_2.manageShares(uVFS.thisSession);
      uVFS.thisSession.uVFS = priorVFS;
      p.put("object_response", response);
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:DELETESHARE")) {
      Properties request = (Properties)p.remove("request");
      String crushAuth = p.getProperty("crushAuth");
      VFS uVFS = (VFS)SharedSession.find("crushftp.usernames").get(String.valueOf(Common.getPartialIp(p.getProperty("user_ip"))) + "_" + crushAuth + "_vfs");
      VFS priorVFS = uVFS.thisSession.uVFS;
      uVFS.thisSession.uVFS = uVFS;
      String response = ServerSessionAJAX5_2.deleteShare(request, uVFS.thisSession);
      uVFS.thisSession.uVFS = priorVFS;
      p.put("object_response", response);
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:PONG")) {
      Properties pong = (Properties)p.remove("data");
      Log.log("SERVER", 0, "DMZ command queue ping:" + (System.currentTimeMillis() - Long.parseLong(pong.getProperty("time"))) + "ms");
    } else if (p.getProperty("type").equalsIgnoreCase("GET:SYNC")) {
      Log.log("DMZ", 2, "READ:" + this.server_item.getProperty("server_item_name") + ":" + p.getProperty("type") + ":" + p);
      Properties user = UserTools.ut.getUser(this.server_item.getProperty("linkedServer", ""), p.getProperty("username"), true);
      VFS uVFS = UserTools.ut.getVFS(this.server_item.getProperty("linkedServer", ""), p.getProperty("username"));
      if (user != null) {
        Properties request = (Properties)p.remove("request");
        if (request.getProperty("command", "").equalsIgnoreCase("getSyncTableData")) {
          Properties request2 = request;
          Properties p2 = p;
          VFS uVFS2 = uVFS;
          Properties user2 = user;
          (new Thread(new Runnable(this, request2, uVFS2, user2, p2) {
                final DMZServer this$0;
                
                private final Properties val$request2;
                
                private final VFS val$uVFS2;
                
                private final Properties val$user2;
                
                private final Properties val$p2;
                
                public void run() {
                  Thread.currentThread().setName("DMZ:getSyncTableData:" + this.val$request2.toString());
                  String vfs_path = this.val$request2.getProperty("path", "");
                  String root_dir = ServerSession.getRootDir(null, this.val$uVFS2, this.val$user2, false);
                  if (vfs_path.equals(""))
                    vfs_path = "/"; 
                  if (!vfs_path.startsWith(root_dir))
                    vfs_path = String.valueOf(root_dir) + vfs_path.substring(1); 
                  try {
                    Object o = Common.getSyncTableData(this.val$p2.getProperty("syncID").toUpperCase(), Long.parseLong(this.val$request2.getProperty("lastRID")), this.val$request2.getProperty("table"), this.val$p2.getProperty("clientid"), this.val$uVFS2.get_item(vfs_path).getProperty("url"), vfs_path);
                    if (o != null)
                      this.val$p2.put("object_response", o); 
                    Log.log("DMZ", 2, "READ:" + this.this$0.server_item.getProperty("server_item_name") + ":" + this.val$p2.getProperty("type") + ":GOT RESPONSE, sending back.");
                    DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
                  } catch (Exception e) {
                    Log.log("SERVER", 0, e);
                  } 
                }
              })).start();
        } else if (request.getProperty("command", "").equalsIgnoreCase("syncConflict")) {
          SyncTools.addJournalEntry(p.getProperty("syncID"), request.getProperty("item_path"), "CONFLICT", "", "");
          sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
        } else if (request.getProperty("command", "").equalsIgnoreCase("purgeSync")) {
          String root_dir = p.getProperty("root_dir");
          if (root_dir.indexOf("/", 1) > 0)
            root_dir = p.getProperty("root_dir").substring(p.getProperty("root_dir").indexOf("/", 1)); 
          AdminControls.purgeSync(request, uVFS, root_dir);
          sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
        } 
      } else {
        sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
      } 
    } 
  }
  
  public void startSocketConnectors() throws Exception {
    Runnable r1 = new Runnable(this) {
        final DMZServer this$0;
        
        public void run() {
          StringBuffer die_now2 = this.this$0.die_now;
          try {
            SSLSocketFactory factory = ServerStatus.thisObj.common_code.getSSLContext("builtin", null, "crushftp", "crushftp", "TLS", false, true).getSocketFactory();
            while (die_now2.length() == 0) {
              Socket tempSock = null;
              try {
                tempSock = factory.createSocket(this.this$0.server_item.getProperty("ip"), Integer.parseInt(this.this$0.server_item.getProperty("port")));
                tempSock.getOutputStream().write("D".getBytes());
                tempSock.getOutputStream().flush();
                byte[] pb = new byte[5];
                int bytesRead = tempSock.getInputStream().read(pb);
                if (bytesRead == 0) {
                  Thread.sleep(1000L);
                  bytesRead = tempSock.getInputStream().read(pb);
                } 
                if ((new String(pb)).trim().equals(""))
                  throw new IOException("DMZ socket received invalid preferred port."); 
                int preferredPort = Integer.parseInt((new String(pb)).trim());
                if (preferredPort <= 0)
                  throw new IOException("DMZ socket failed to start."); 
                Properties server_item_temp = null;
                int x;
                for (x = 0; x < ServerStatus.VG("server_list").size(); x++) {
                  Properties si = ServerStatus.VG("server_list").elementAt(x);
                  if (si.getProperty("serverType").startsWith("HTTP") && si.getProperty("port").equals((new StringBuffer(String.valueOf(preferredPort))).toString())) {
                    server_item_temp = si;
                    break;
                  } 
                } 
                if (server_item_temp == null)
                  for (x = 0; x < ServerStatus.VG("server_list").size(); x++) {
                    server_item_temp = ServerStatus.VG("server_list").elementAt(x);
                    if (server_item_temp.getProperty("serverType").equals("HTTP") || server_item_temp.getProperty("serverType").equals("HTTPS"))
                      break; 
                  }  
                if (server_item_temp.getProperty("https_redirect", "false").equalsIgnoreCase("true")) {
                  server_item_temp = (Properties)server_item_temp.clone();
                  server_item_temp.put("https_redirect", "false");
                  Log.log("SERVER", 0, "You must turn off HTTPS redirect on your first HTTP port to prevent DMZ issues.");
                } 
                QuickConnect quicky = new QuickConnect(this.this$0.thisObj, this.this$0.listen_port, tempSock, this.this$0.the_ip, String.valueOf(this.this$0.listen_ip) + "_" + this.this$0.listen_port, server_item_temp);
                tempSock = null;
                if (!Worker.startWorker(quicky, String.valueOf(this.this$0.listen_ip) + "_" + this.this$0.listen_port + " --> " + this.this$0.the_ip)) {
                  this.this$0.sock.close();
                  quicky = null;
                  synchronized (this) {
                    this.this$0.connected_users--;
                  } 
                } 
                ServerStatus.siPUT("thread_pool_available", (new StringBuffer(String.valueOf(ServerStatus.thisObj.availableWorkers.size()))).toString());
                ServerStatus.siPUT("thread_pool_busy", (new StringBuffer(String.valueOf(ServerStatus.thisObj.busyWorkers.size()))).toString());
              } catch (IOException e) {
                Log.log("SERVER", 4, e);
                Thread.sleep(200L);
              } 
              if (tempSock != null)
                tempSock.close(); 
              Thread.sleep(10L);
            } 
          } catch (Exception e) {
            Log.log("SERVER", 0, e);
            this.this$0.socket_created = false;
            die_now2.append(System.currentTimeMillis());
          } 
        }
      };
    Runnable r2 = new Runnable(this) {
        final DMZServer this$0;
        
        public void run() {
          StringBuffer die_now2 = this.this$0.die_now;
          try {
            InputStream in = this.this$0.read_sock.getInputStream();
            OutputStream out = this.this$0.read_sock.getOutputStream();
            byte[] b1 = new byte[1];
            long last_ping = System.currentTimeMillis();
            while (die_now2.length() == 0) {
              try {
                b1[0] = 0;
                String len_str = "";
                while (b1[0] != 58) {
                  int i = in.read(b1);
                  if (i > 0) {
                    len_str = String.valueOf(len_str) + new String(b1);
                    continue;
                  } 
                  throw new Exception("DMZ:EOF reached in receiver read of chunk size.");
                } 
                int len = Integer.parseInt(len_str.substring(0, len_str.length() - 1));
                byte[] b = new byte[len];
                int bytesRead = 0;
                int totalBytes = 0;
                while (totalBytes < len) {
                  bytesRead = in.read(b, totalBytes, len - totalBytes);
                  if (bytesRead < 0)
                    throw new Exception("DMZ:EOF reached in receiver read of chunk."); 
                  totalBytes += bytesRead;
                } 
                out.write(1);
                out.flush();
                if (len > 0) {
                  ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(b));
                  Properties p = (Properties)ois.readObject();
                  ois.close();
                  this.this$0.responseQueue.addElement(p);
                  this.this$0.messages_received++;
                } 
              } catch (SocketTimeoutException socketTimeoutException) {}
              if (System.currentTimeMillis() - last_ping > 10000L) {
                Properties ping = new Properties();
                ping.put("id", Common.makeBoundary());
                ping.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), ping, "PUT:PING", ping.getProperty("id"));
                last_ping = System.currentTimeMillis();
              } 
              if (System.currentTimeMillis() - last_ping > 20000L)
                throw new Exception("Socket timeout, firewall killed socket."); 
            } 
          } catch (Exception e) {
            Log.log("SERVER", 0, e);
            this.this$0.socket_created = false;
            this.this$0.restart = true;
            die_now2.append(System.currentTimeMillis());
          } 
        }
      };
    Runnable r3 = new Runnable(this) {
        final DMZServer this$0;
        
        public void run() {
          StringBuffer die_now2 = this.this$0.die_now;
          try {
            while (die_now2.length() == 0) {
              while (this.this$0.responseQueue.size() > 0)
                this.this$0.processResponse(this.this$0.responseQueue.remove(0)); 
              Thread.sleep(100L);
            } 
          } catch (Exception e) {
            Log.log("SERVER", 0, e);
            this.this$0.socket_created = false;
            die_now2.append(System.currentTimeMillis());
          } 
        }
      };
    Worker.startWorker(r1, "DMZ:SocketConnector:" + this.server_item.getProperty("server_item_name"));
    Worker.startWorker(r2, "DMZ:SocketReceiver:" + this.server_item.getProperty("server_item_name"));
    Worker.startWorker(r3, "DMZ:ResponseProcessor:" + this.server_item.getProperty("server_item_name"));
  }
  
  public void sendFileToMemory(String path) throws Exception {
    if (path != null && !path.equals("") && (new File(path)).exists()) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Common.streamCopier(new FileInputStream(path), baos, false, true, true);
      Properties pp = new Properties();
      pp.put("bytes", baos.toByteArray());
      Properties system_prop = new Properties();
      system_prop.put("key", "crushftp.keystores." + path.toUpperCase().replace('\\', '/'));
      system_prop.put("val", pp);
      sendCommand(this.server_item.getProperty("server_item_name"), system_prop, "PUT:SYSTEM.PROPERTIES", "");
      if (this.error != null)
        throw this.error; 
    } 
  }
  
  public void updateStatus() {
    synchronized (updateServerStatuses) {
      if (!this.started)
        return; 
      updateStatusInit();
      String msg = this.socket_created ? "running" : "stopped";
      this.server_item.put("display", String.valueOf(this.busyMessage.equals("") ? "" : ("(" + this.busyMessage + ")  ")) + LOC.G("dmz://$0:($1)/  ($2) is $3, $4 messages received, $5 messages sent.", this.server_item.getProperty("ip"), this.server_item.getProperty("port"), this.server_item.getProperty("server_item_name", ""), msg, (new StringBuffer(String.valueOf(this.messages_received))).toString(), (new StringBuffer(String.valueOf(this.messages_sent))).toString()));
    } 
  }
}
