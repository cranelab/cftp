package crushftp.server.daemon;

import com.crushftp.client.Base64;
import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.SharedSession;
import crushftp.handlers.SharedSessionReplicated;
import crushftp.handlers.SyncTools;
import crushftp.handlers.UserTools;
import crushftp.server.AdminControls;
import crushftp.server.As2Msg;
import crushftp.server.QuickConnect;
import crushftp.server.ServerSessionAJAX;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
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
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
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
  
  String singleton_id = Common.makeBoundary();
  
  File prefs_file = null;
  
  public static Properties last_prefs_time = new Properties();
  
  public static Object stop_send_prefs = new Object();
  
  public DMZServer(Properties server_item) {
    super(server_item);
  }
  
  public static void sendCommand(String instance_name, Properties data, String type, String id) {
    sendCommand(instance_name, data, (String)null, type, id);
  }
  
  public static void sendCommand(String instance_name, Properties data, String site, String type, String id) {
    DMZServer dmz = (DMZServer)dmzInstances.get(instance_name);
    try {
      if (dmz.write_sock != null) {
        Properties p = new Properties();
        p.put("type", type.toUpperCase());
        p.put("data", data);
        if (site != null)
          p.put("site", site); 
        p.put("id", id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.reset();
        synchronized (SharedSession.sessionLock) {
          out.writeObject(p);
          out.close();
        } 
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
        Log.log("DMZ", 2, "WROTE:" + instance_name + ":" + p.getProperty("type") + ":" + p.getProperty("id"));
      } 
    } catch (Exception e) {
      dmz.error = e;
    } 
  }
  
  public static Properties getResponse(String id, int timeout) throws Exception {
    long start = System.currentTimeMillis();
    while (!dmzResponses.containsKey(id) && start > System.currentTimeMillis() - (1000 * timeout))
      Thread.sleep(1L); 
    if (!dmzResponses.containsKey(id))
      return null; 
    return (Properties)dmzResponses.remove(id);
  }
  
  public void run() {
    init();
    String dmz_name_host_port = "";
    try {
      if (ServerStatus.SG("never_ban").indexOf(this.listen_ip) < 0)
        if (!ServerStatus.SG("never_ban").trim().equals("*"))
          if (!ServerStatus.SG("never_ban").equals("disabled"))
            ServerStatus.server_settings.put("never_ban", String.valueOf(ServerStatus.SG("never_ban")) + "," + this.listen_ip);   
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
      Common.System2.put("crushftp.dmz.factory", factory);
      if (this.read_sock != null) {
        Common.sockLog(this.read_sock, "read_sock close at port restart");
        this.read_sock.close();
      } 
      if (this.write_sock != null) {
        Common.sockLog(this.write_sock, "write_sock close at port restart");
        this.write_sock.close();
      } 
      if (System.getProperty("crushftp.dmz.ssl", "true").equals("true")) {
        this.read_sock = factory.createSocket(this.listen_ip, this.listen_port);
        this.write_sock = factory.createSocket(this.listen_ip, this.listen_port);
        Common.configureSSLTLSSocket(this.read_sock);
        Common.configureSSLTLSSocket(this.write_sock);
      } else {
        this.read_sock = new Socket(this.listen_ip, this.listen_port);
        this.write_sock = new Socket(this.listen_ip, this.listen_port);
      } 
      Common.sockLog(this.read_sock, "read_sock create");
      Common.sockLog(this.write_sock, "write_sock create");
      this.read_sock.setSoTimeout(10000);
      this.write_sock.setSoTimeout(10000);
      this.read_sock.getOutputStream().write("W".getBytes());
      this.read_sock.getOutputStream().flush();
      this.write_sock.getOutputStream().write("R".getBytes());
      this.write_sock.getOutputStream().flush();
      dmzInstances.put(this.server_item.getProperty("server_item_name"), this);
      this.busyMessage = "Starting DMZ...";
      startSocketConnectors();
      load_and_send_prefs(false);
      Worker.startWorker(new Runnable(this) {
            final DMZServer this$0;
            
            public void run() {
              DMZServer.last_prefs_time.put(this.this$0.server_item.getProperty("server_item_name"), (new StringBuffer(String.valueOf(this.this$0.prefs_file.lastModified()))).toString());
              while (this.this$0.die_now.length() == 0) {
                synchronized (DMZServer.stop_send_prefs) {
                  try {
                    if (this.this$0.prefs_file.lastModified() != Long.parseLong(DMZServer.last_prefs_time.getProperty(this.this$0.server_item.getProperty("server_item_name", ""))))
                      this.this$0.load_and_send_prefs(false); 
                  } catch (Exception exception) {}
                } 
                try {
                  Thread.sleep(1000L);
                } catch (Exception exception) {}
              } 
            }
          });
      if (this.error != null)
        throw this.error; 
      this.busyMessage = "";
      synchronized (dmzInstances) {
        Vector vector = (Vector)Common.System2.get("crushftp.dmz.hosts");
        if (vector == null)
          vector = new Vector(); 
        Common.System2.put("crushftp.dmz.hosts", vector);
        dmz_name_host_port = String.valueOf(this.server_item.getProperty("server_item_name")) + ":" + this.server_item.getProperty("ip").trim() + ":" + Integer.parseInt(this.server_item.getProperty("port"));
        vector.addElement(dmz_name_host_port);
      } 
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
      Log.log("DMZ", 0, e);
      this.die_now.append(System.currentTimeMillis());
    } catch (ConnectException e) {
      Log.log("DMZ", 3, e);
      this.restart = true;
      this.die_now.append(System.currentTimeMillis());
    } catch (SocketException e) {
      Log.log("DMZ", 2, e);
      this.restart = true;
      this.die_now.append(System.currentTimeMillis());
    } catch (Exception e) {
      Log.log("DMZ", 0, e);
      this.restart = true;
      this.die_now.append(System.currentTimeMillis());
    } 
    Vector v = (Vector)Common.System2.get("crushftp.dmz.hosts");
    if (v != null)
      v.remove(dmz_name_host_port); 
    this.socket_created = false;
    try {
      this.server_sock.close();
    } catch (Exception exception) {}
    updateStatus();
    if (this.restart) {
      try {
        Thread.sleep(1000L);
      } catch (Exception exception) {}
      if (ServerStatus.thisObj.main_servers.indexOf(this) >= 0)
        ServerStatus.thisObj.start_this_server(ServerStatus.thisObj.main_servers.indexOf(this)); 
    } 
  }
  
  public void load_and_send_prefs(boolean needSave) throws Exception {
    this.prefs_file = new File(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs_" + this.server_item.getProperty("server_item_name") + ".XML");
    Properties instance_server_settings = (Properties)Common.readXMLObject(this.prefs_file);
    instance_server_settings.put("registration_name", ServerStatus.SG("registration_name"));
    instance_server_settings.put("registration_email", ServerStatus.SG("registration_email"));
    instance_server_settings.put("registration_code", ServerStatus.SG("registration_code"));
    instance_server_settings.put("tunnels_dmz", ServerStatus.VG("tunnels"));
    instance_server_settings.put("miniURLs_dmz", ServerStatus.VG("miniURLs"));
    ServerStatus.thisObj.common_code.set_defaults(ServerStatus.thisObj.default_settings);
    Enumeration the_list = ServerStatus.thisObj.default_settings.propertyNames();
    while (the_list.hasMoreElements()) {
      Object cur = the_list.nextElement();
      if (instance_server_settings.get(cur.toString()) == null) {
        instance_server_settings.put(cur.toString(), ServerStatus.thisObj.default_settings.get(cur.toString()));
        if (!(ServerStatus.thisObj.default_settings.get(cur.toString()) instanceof Vector) || ((Vector)ServerStatus.thisObj.default_settings.get(cur.toString())).size() != 0)
          if (!(ServerStatus.thisObj.default_settings.get(cur.toString()) instanceof Properties) || ((Properties)ServerStatus.thisObj.default_settings.get(cur.toString())).size() != 0)
            needSave = true;  
      } 
    } 
    if (needSave)
      Common.writeXMLObject(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs_" + this.server_item.getProperty("server_item_name") + ".XML", instance_server_settings, "server_prefs"); 
    last_prefs_time.put(this.server_item.getProperty("server_item_name"), (new StringBuffer(String.valueOf(this.prefs_file.lastModified()))).toString());
    sendFileToMemory(instance_server_settings.getProperty("cert_path", ""), this.server_item.getProperty("server_item_name"));
    Vector instance_servers = (Vector)instance_server_settings.get("server_list");
    for (int x = 0; x < instance_servers.size(); x++) {
      sendFileToMemory(((Properties)instance_servers.elementAt(x)).getProperty("customKeystore", ""), this.server_item.getProperty("server_item_name"));
      sendFileToMemory(String.valueOf(((Properties)instance_servers.elementAt(x)).getProperty("customKeystore", "")) + "_trust", this.server_item.getProperty("server_item_name"));
      sendFileToMemory(((Properties)instance_servers.elementAt(x)).getProperty("ssh_rsa_key", ""), this.server_item.getProperty("server_item_name"));
      sendFileToMemory(((Properties)instance_servers.elementAt(x)).getProperty("ssh_dsa_key", ""), this.server_item.getProperty("server_item_name"));
    } 
    sendCommand(this.server_item.getProperty("server_item_name"), instance_server_settings, "PUT:SERVER_SETTINGS", "");
  }
  
  public void sendToken() {
    Properties system_prop = new Properties();
    system_prop.put("key", "crushftp.proxy.anyPassToken");
    system_prop.put("val", UserTools.anyPassTokens.elementAt(0).toString());
    sendCommand(this.server_item.getProperty("server_item_name"), system_prop, "PUT:SYSTEM.PROPERTIES", "");
    SharedSessionReplicated.send("", "anyPassToken", "anyPassToken", UserTools.anyPassTokens.elementAt(0).toString());
  }
  
  public void processResponse(Properties p) throws Exception {
    Log.log("DMZ", 2, "READ:" + this.server_item.getProperty("server_item_name") + ":" + p.getProperty("type") + ":" + p.getProperty("id"));
    if (p.getProperty("type").equalsIgnoreCase("RESPONSE")) {
      p.put("received", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      dmzResponses.put(p.getProperty("id"), p);
    } else if (p.getProperty("type").equalsIgnoreCase("GET:USER_SSH_KEYS")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              Vector public_keys = null;
              Properties user = UserTools.ut.getUser(this.val$p2.getProperty("linkedServer", "MainUsers"), this.val$p2.getProperty("username"), true);
              if (user == null) {
                SessionCrush tempSession = new SessionCrush(null, 1, "127.0.0.1", 0, "0.0.0.0", this.this$0.server_item.getProperty("linkedServer", ""), this.this$0.server_item);
                try {
                  tempSession.verify_user(this.val$p2.getProperty("username"), this.val$p2.getProperty("password", ""), true, false);
                  user = tempSession.user;
                  if (user.getProperty("ssh_public_keys").equals("")) {
                    Properties pp = new Properties();
                    pp.put("user", user);
                    pp.put("username", this.val$p2.getProperty("username"));
                    pp.put("password", "");
                    pp.put("anyPass", "true");
                    pp.put("publickey_lookup", "true");
                    pp = tempSession.runPlugin("login", pp);
                    user.getProperty("ssh_public_keys");
                  } 
                } catch (Exception e) {
                  Log.log("DMZ", 1, e);
                } 
              } 
              try {
                if (user != null)
                  public_keys = UserTools.buildPublicKeys(this.val$p2.getProperty("username"), user); 
              } catch (IOException e) {
                Log.log("DMZ", 1, e);
              } 
              if (user != null) {
                user.remove("filePublicEncryptionKey");
                user.remove("fileEncryptionKey");
                user.remove("fileDecryptionKey");
                this.val$p2.put("public_keys", public_keys);
                this.val$p2.put("user", user);
              } 
              DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:USER")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              try {
                Vector public_keys = null;
                Properties user = UserTools.ut.getUser(this.this$0.server_item.getProperty("linkedServer", ""), this.val$p2.getProperty("username"), true);
                VFS uVFS = null;
                try {
                  uVFS = UserTools.ut.getVFS(this.this$0.server_item.getProperty("linkedServer", ""), this.val$p2.getProperty("username"));
                } catch (Exception e) {
                  Log.log("SERVER", 0, e);
                  user = null;
                } 
                if (user == null) {
                  SessionCrush tempSession = new SessionCrush(null, 1, "127.0.0.1", 0, "0.0.0.0", this.this$0.server_item.getProperty("linkedServer", ""), this.this$0.server_item);
                  try {
                    tempSession.verify_user(this.val$p2.getProperty("username"), this.val$p2.getProperty("password"), false, false);
                    user = tempSession.user;
                    uVFS = tempSession.uVFS;
                  } catch (Exception e) {
                    Log.log("DMZ", 1, e);
                  } 
                } 
                if (user != null)
                  public_keys = UserTools.buildPublicKeys(this.val$p2.getProperty("username"), user); 
                if (user != null) {
                  DMZServer.sendFileToMemory(user.getProperty("as2EncryptKeystorePath", ""), this.this$0.server_item.getProperty("server_item_name"));
                  DMZServer.sendFileToMemory(user.getProperty("as2SignKeystorePath", ""), this.this$0.server_item.getProperty("server_item_name"));
                  UserTools.setupVFSLinking(this.this$0.server_item.getProperty("linkedServer", ""), this.val$p2.getProperty("username"), uVFS, user);
                  user.remove("filePublicEncryptionKey");
                  user.remove("fileEncryptionKey");
                  user.remove("fileDecryptionKey");
                  this.val$p2.put("public_keys", public_keys);
                  this.val$p2.put("user", user);
                  this.val$p2.put("vfs", uVFS.homes);
                } 
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
              } catch (Exception e) {
                Log.log("DMZ", 0, e);
              } 
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:RESET_TOKEN")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              try {
                String responseText = ServerSessionAJAX.doResetToken(this.val$p2.getProperty("reset_username_email"), this.val$p2.getProperty("currentURL"), this.this$0.server_item.getProperty("linkedServer", ""), this.val$p2.getProperty("reset_token"), this.this$0.singleton_id.equals(this.val$p2.getProperty("singleton_id", this.this$0.singleton_id)));
                this.val$p2.put("responseText", responseText);
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
              } catch (Exception e) {
                Log.log("SERVER", 0, e);
              } 
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:SINGLETON")) {
      p.put("singleton_id", this.singleton_id);
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("GET:RESET_TOKEN_PASS")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              String responseText = ServerSessionAJAX.doResetTokenPass(this.val$p2.getProperty("resetToken"), this.this$0.server_item.getProperty("linkedServer", ""), this.val$p2.getProperty("password1"), new Properties());
              this.val$p2.put("responseText", responseText);
              DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:DOWNLOAD_COUNT")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              this.val$p2.put("responseText", (new StringBuffer(String.valueOf(ServerStatus.thisObj.statTools.getUserDownloadCount(this.val$p2.getProperty("username"))))).toString());
              DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:ACL")) {
      String item_privs = "";
      if (ServerStatus.SG("acl_mode").equals("2")) {
        item_privs = VFS.getAcl2Proc((Properties)p.get("dir_item"), p.getProperty("acl_domain"), p.getProperty("localPath"), p.getProperty("username"));
      } else if (ServerStatus.SG("acl_mode").equals("3")) {
        item_privs = VFS.getAcl3Proc((Properties)p.get("dir_item"), p.getProperty("acl_domain"), p.getProperty("localPath"), p.getProperty("username"), new Properties());
      } 
      p.put("item_privs", item_privs);
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:SHARE")) {
      Properties request = (Properties)p.remove("request");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null) {
        String response = "";
        try {
          Vector path_items = new Vector();
          String[] paths = Common.url_decode(request.getProperty("paths")).split("\r\n");
          for (int x = 0; x < paths.length; x++) {
            String the_dir = paths[x].trim();
            if (!the_dir.equals("")) {
              if (the_dir.startsWith(thisSession.SG("root_dir")))
                the_dir = the_dir.substring(thisSession.SG("root_dir").length() - 1); 
              String path = thisSession.getStandardizedDir(the_dir);
              Log.log("HTTP_SERVER", 2, "Sharing:" + the_dir + "  vs.  " + path);
              Properties item = thisSession.uVFS.get_item(path);
              Log.log("HTTP_SERVER", 2, "Sharing:" + item);
              VRL vrl = new VRL(item.getProperty("url"));
              Properties stat = null;
              GenericClient c = thisSession.uVFS.getClient(item);
              try {
                stat = c.stat(vrl.getPath());
                stat.put("root_dir", item.getProperty("root_dir"));
              } finally {
                c = thisSession.uVFS.releaseClient(c);
              } 
              stat.put("privs", item.getProperty("privs"));
              path_items.addElement(stat);
            } 
          } 
          response = ServerSessionAJAX.createShare(path_items, request, (Vector)thisSession.user.get("web_customizations"), thisSession.uiSG("user_name"), thisSession.server_item.getProperty("linkedServer"), thisSession.user, thisSession.date_time, thisSession);
        } catch (Exception e) {
          Log.log("DMZ", 0, e);
        } 
        p.put("object_response", response);
        sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
      } 
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:GETMD5S")) {
      Properties p2 = p;
      Properties request2 = (Properties)p.remove("request");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null)
        try {
          Worker.startWorker(new Runnable(this, request2, thisSession, p2) {
                final DMZServer this$0;
                
                private final Properties val$request2;
                
                private final SessionCrush val$thisSession;
                
                private final Properties val$p2;
                
                public void run() {
                  String path_str = null;
                  try {
                    path_str = new String(Base64.decode(this.val$request2.getProperty("path")));
                  } catch (Exception e) {
                    path_str = Common.dots(Common.url_decode(this.val$request2.getProperty("path")));
                  } 
                  if (!path_str.equals("")) {
                    if (path_str.startsWith(this.val$thisSession.SG("root_dir")))
                      path_str = path_str.substring(this.val$thisSession.SG("root_dir").length() - 1); 
                    Vector md5s = new Vector();
                    StringBuffer responseBuf = new StringBuffer();
                    try {
                      Properties item = this.val$thisSession.uVFS.get_item(String.valueOf(this.val$thisSession.SG("root_dir")) + path_str);
                      if (item != null) {
                        GenericClient c = this.val$thisSession.uVFS.getClient(item);
                        try {
                          VRL vrl = new VRL(item.getProperty("url"));
                          Properties stat = c.stat(vrl.getPath());
                          Common.getMD5(c.download(vrl.getPath(), 0L, -1L, true), md5s, this.val$request2.getProperty("chunked", "true").equals("true"), this.val$request2.getProperty("forward", "true").equals("true"), Long.parseLong(stat.getProperty("size")), Long.parseLong(this.val$request2.getProperty("local_size", "0")));
                        } finally {
                          c = this.val$thisSession.uVFS.releaseClient(c);
                        } 
                      } 
                    } catch (Exception e) {
                      Log.log("HTTP_SERVER", 1, e);
                    } 
                    while (md5s.size() > 0)
                      responseBuf.append(md5s.remove(0).toString()).append("\r\n"); 
                    this.val$p2.put("object_response", responseBuf.toString());
                    DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
                  } 
                }
              });
        } catch (Exception e) {
          Log.log("DMZ", 0, e);
        }  
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:CUSTOM")) {
      Properties request = (Properties)p.remove("request");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null) {
        String response = "";
        try {
          Vector path_items = new Vector();
          String[] paths = (String[])null;
          if (request.getProperty("paths").indexOf("|") >= 0) {
            paths = Common.url_decode(request.getProperty("paths")).split("\\|");
          } else if (request.getProperty("paths").indexOf(";") >= 0) {
            paths = Common.url_decode(request.getProperty("paths")).split("\r\n");
          } else {
            paths = Common.url_decode(request.getProperty("paths")).split("\r\n");
          } 
          for (int x = 0; x < paths.length; x++) {
            String the_dir = paths[x].trim();
            if (!the_dir.equals("")) {
              if (the_dir.startsWith(thisSession.SG("root_dir")))
                the_dir = the_dir.substring(thisSession.SG("root_dir").length() - 1); 
              String path = thisSession.getStandardizedDir(the_dir);
              Log.log("HTTP_SERVER", 2, "Custom:" + the_dir + "  vs.  " + path);
              Properties item = thisSession.uVFS.get_item(path);
              Log.log("HTTP_SERVER", 2, "Custom:" + item);
              VRL vrl = new VRL(item.getProperty("url"));
              Properties stat = null;
              GenericClient c = thisSession.uVFS.getClient(item);
              try {
                stat = c.stat(vrl.getPath());
              } finally {
                c = thisSession.uVFS.releaseClient(c);
              } 
              stat.put("privs", item.getProperty("privs"));
              String root_dir = Common.all_but_last(the_dir);
              stat.put("root_dir", root_dir);
              path_items.addElement(stat);
            } 
          } 
          String common_root = "";
          int depth = 0;
          while (true) {
            boolean all_ok = true;
            String root_dir = ((Properties)path_items.elementAt(0)).getProperty("root_dir");
            String new_common_root = "/";
            if (depth >= (root_dir.split("/")).length)
              break; 
            int j;
            for (j = 0; j < depth; j++)
              new_common_root = String.valueOf(new_common_root) + root_dir.split("/")[j + 1] + "/"; 
            depth++;
            for (j = 0; j < path_items.size(); j++) {
              Properties pp = path_items.elementAt(j);
              if (!pp.getProperty("root_dir").startsWith(new_common_root))
                all_ok = false; 
            } 
            if (!all_ok || common_root.equals(new_common_root))
              break; 
            common_root = new_common_root;
          } 
          if (common_root.equals(""))
            common_root = "/"; 
          common_root = common_root.substring(0, common_root.length() - 1);
          for (int i = 0; i < path_items.size(); i++) {
            Properties pp = path_items.elementAt(i);
            String root_dir = pp.getProperty("root_dir");
            if (root_dir.startsWith(common_root))
              root_dir = root_dir.substring(common_root.length()); 
            pp.put("root_dir", root_dir);
          } 
          response = ServerSessionAJAX.createCustom(path_items, request, thisSession);
        } catch (Exception e) {
          Log.log("DMZ", 0, e);
        } 
        p.put("object_response", response);
        sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
      } 
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:BATCH_COMPLETE")) {
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null)
        thisSession.do_event5("BATCH_COMPLETE", null); 
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:MANAGESHARES")) {
      p.remove("request");
      p.put("object_response", "");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null) {
        String response = ServerSessionAJAX.manageShares(thisSession);
        p.put("object_response", response);
      } 
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:GETHISTORY")) {
      Properties request = (Properties)p.remove("request");
      p.put("object_response", "");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null) {
        String response = ServerSessionAJAX.getHistory(request, thisSession);
        p.put("object_response", response);
      } 
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:DELETESHARE")) {
      Properties request = (Properties)p.remove("request");
      p.put("object_response", "");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null) {
        String response = ServerSessionAJAX.deleteShare(request, thisSession);
        p.put("object_response", response);
      } 
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:EDITSHARE")) {
      Properties request = (Properties)p.remove("request");
      p.put("object_response", "");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null) {
        String response = ServerSessionAJAX.editShare(request, thisSession);
        p.put("object_response", response);
      } 
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:SELFREGISTRATION")) {
      Properties request = (Properties)p.remove("request");
      String req_id = p.remove("req_id").toString();
      p.put("object_response", "");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null) {
        String response = ServerSessionAJAX.selfRegistration(request, thisSession, req_id);
        p.put("object_response", response);
      } 
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:ERROR_EVENT")) {
      Properties error_info = (Properties)p.get("error_info");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null) {
        Properties ui2 = (Properties)p.get("error_user_info");
        Enumeration keys = ui2.keys();
        while (keys.hasMoreElements()) {
          String key = (String)keys.nextElement();
          boolean allowed = false;
          if (key.startsWith("as2") || key.equals("host") || key.equals("message-id") || key.equals("content_length") || key.startsWith("disp") || key.equals("accept") || key.equals("signMdn"))
            allowed = true; 
          if (!thisSession.user_info.containsKey(key) || allowed)
            thisSession.user_info.put(key, ui2.get(key)); 
        } 
        thisSession.do_event5("ERROR", error_info);
      } 
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:AS2MDN")) {
      Properties mdnInfo = (Properties)p.get("mdnInfo");
      Worker.startWorker(new Runnable(this, mdnInfo) {
            final DMZServer this$0;
            
            private final Properties val$mdnInfo;
            
            public void run() {
              As2Msg.mdnResponses.put(this.val$mdnInfo.getProperty("Original-Message-ID".toLowerCase()), this.val$mdnInfo);
              try {
                Thread.sleep(5000L);
              } catch (InterruptedException interruptedException) {}
              As2Msg.mdnResponses.remove(this.val$mdnInfo.getProperty("Original-Message-ID".toLowerCase()));
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:PONG")) {
      Properties pong = (Properties)p.remove("data");
      Log.log("DMZ", 1, "DMZ command queue ping:" + (System.currentTimeMillis() - Long.parseLong(pong.getProperty("time"))) + "ms");
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:ALERT")) {
      Properties alert_info = p;
      Worker.startWorker(new Runnable(this, alert_info) {
            final DMZServer this$0;
            
            private final Properties val$alert_info;
            
            public void run() {
              try {
                ServerStatus.thisObj.runAlerts(this.val$alert_info.getProperty("alert_action"), (Properties)this.val$alert_info.get("info"), (Properties)this.val$alert_info.get("user_info"), null, (Properties)this.val$alert_info.get("alert"), true);
              } catch (Exception e) {
                Log.log("SERVER", 1, e);
              } 
            }
          });
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
          Worker.startWorker(new Runnable(this, request2, uVFS2, user2, p2) {
                final DMZServer this$0;
                
                private final Properties val$request2;
                
                private final VFS val$uVFS2;
                
                private final Properties val$user2;
                
                private final Properties val$p2;
                
                public void run() {
                  Thread.currentThread().setName("DMZ:getSyncTableData:" + this.val$request2.toString());
                  String vfs_path = this.val$request2.getProperty("path", "");
                  String root_dir = SessionCrush.getRootDir(null, this.val$uVFS2, this.val$user2, false);
                  if (vfs_path.equals(""))
                    vfs_path = "/"; 
                  if (!vfs_path.startsWith(root_dir))
                    vfs_path = String.valueOf(root_dir) + vfs_path.substring(1); 
                  try {
                    Object o = Common.getSyncTableData(this.val$p2.getProperty("syncID").toUpperCase(), Long.parseLong(this.val$request2.getProperty("lastRID")), this.val$request2.getProperty("table"), this.val$p2.getProperty("clientid"), vfs_path, this.val$uVFS2);
                    if (o != null)
                      this.val$p2.put("object_response", o); 
                    Log.log("DMZ", 2, "READ:" + this.this$0.server_item.getProperty("server_item_name") + ":" + this.val$p2.getProperty("type") + ":GOT RESPONSE, sending back.");
                    DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
                  } catch (Exception e) {
                    Log.log("DMZ", 0, e);
                  } 
                }
              });
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
    } else if (p.getProperty("type").equalsIgnoreCase("GET:QUOTA")) {
      Log.log("DMZ", 2, "READ:" + this.server_item.getProperty("server_item_name") + ":" + p.getProperty("type") + ":" + p);
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              String q = "-12345";
              try {
                VFS uVFS = null;
                Properties user = UserTools.ut.getUser(this.this$0.server_item.getProperty("linkedServer", ""), this.val$p2.getProperty("username"), true);
                if (user == null) {
                  SessionCrush tempSession = new SessionCrush(null, 1, "127.0.0.1", 0, "0.0.0.0", this.this$0.server_item.getProperty("linkedServer", ""), this.this$0.server_item);
                  tempSession.verify_user(this.val$p2.getProperty("username"), this.val$p2.getProperty("password"), false, false);
                  uVFS = tempSession.uVFS;
                } else {
                  uVFS = UserTools.ut.getVFS(this.this$0.server_item.getProperty("linkedServer", ""), this.val$p2.getProperty("username"));
                } 
                String root_dir = SessionCrush.getRootDir(null, uVFS, null, true);
                String the_dir = String.valueOf(root_dir) + this.val$p2.getProperty("the_dir").substring(1);
                q = (new StringBuffer(String.valueOf(SessionCrush.get_quota(the_dir, uVFS, "", new Properties(), null, true)))).toString();
                q = String.valueOf(q) + ":" + SessionCrush.get_quota(the_dir, uVFS, "", new Properties(), null, false);
              } catch (Exception e) {
                Log.log("DMZ", 1, e);
              } 
              this.val$p2.put("object_response", (new StringBuffer(String.valueOf(q))).toString());
              DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:QUOTA_USED")) {
      Log.log("DMZ", 2, "READ:" + this.server_item.getProperty("server_item_name") + ":" + p.getProperty("type") + ":" + p);
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              long q = -12345L;
              try {
                VFS uVFS = null;
                Properties user = UserTools.ut.getUser(this.this$0.server_item.getProperty("linkedServer", ""), this.val$p2.getProperty("username"), true);
                if (user == null) {
                  SessionCrush tempSession = new SessionCrush(null, 1, "127.0.0.1", 0, "0.0.0.0", this.this$0.server_item.getProperty("linkedServer", ""), this.this$0.server_item);
                  tempSession.verify_user(this.val$p2.getProperty("username"), this.val$p2.getProperty("password"), false, false);
                  uVFS = tempSession.uVFS;
                } else {
                  uVFS = UserTools.ut.getVFS(this.this$0.server_item.getProperty("linkedServer", ""), this.val$p2.getProperty("username"));
                } 
                String root_dir = SessionCrush.getRootDir(null, uVFS, null, true);
                String the_dir = String.valueOf(root_dir) + this.val$p2.getProperty("the_dir").substring(1);
                q = SessionCrush.get_quota_used(the_dir, uVFS, "", null);
              } catch (Exception e) {
                Log.log("DMZ", 1, e);
              } 
              this.val$p2.put("object_response", (new StringBuffer(String.valueOf(q))).toString());
              DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
            }
          });
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
                if (System.getProperty("crushftp.dmz.ssl", "true").equals("true")) {
                  tempSock = factory.createSocket(this.this$0.server_item.getProperty("ip"), Integer.parseInt(this.this$0.server_item.getProperty("port")));
                  Common.configureSSLTLSSocket(tempSock);
                } else {
                  tempSock = new Socket(this.this$0.server_item.getProperty("ip"), Integer.parseInt(this.this$0.server_item.getProperty("port")));
                } 
                Common.sockLog(tempSock, "tempSock create D");
                tempSock.setSoTimeout(12000);
                tempSock.getOutputStream().write("D".getBytes());
                tempSock.getOutputStream().flush();
                byte[] pb = new byte[100];
                int bytesRead = tempSock.getInputStream().read(pb);
                if (bytesRead == 0) {
                  Thread.sleep(1000L);
                  bytesRead = tempSock.getInputStream().read(pb);
                } 
                if ((new String(pb)).trim().equals(""))
                  throw new IOException("DMZ socket received invalid prefered port."); 
                Common.sockLog(tempSock, "tempSock create D -> " + new String(pb));
                boolean reverse = false;
                if ((new String(pb)).trim().indexOf(":") > 0) {
                  reverse = true;
                  if (!(new String(pb)).trim().split(":")[0].trim().equals("")) {
                    tempSock.setSoTimeout(0);
                    Socket sock = new Socket((new String(pb)).trim().split(":")[0], Integer.parseInt((new String(pb)).trim().split(":")[1]));
                    Common.sockLog(sock, "sock create reverse");
                    Common.streamCopier(sock, tempSock, tempSock.getInputStream(), sock.getOutputStream(), true, true, true);
                    Common.streamCopier(sock, tempSock, sock.getInputStream(), tempSock.getOutputStream(), true, true, true);
                  } else {
                    tempSock.close();
                  } 
                } else if (Integer.parseInt((new String(pb)).trim()) <= 0) {
                  throw new IOException("DMZ socket failed to start.");
                } 
                if (!reverse) {
                  Log.log("SERVER", 2, "GET:SOCKET:Request for socket with prefered port:" + (new String(pb)).trim());
                  Properties server_item_temp = null;
                  int x;
                  for (x = 0; x < ServerStatus.VG("server_list").size(); x++) {
                    Properties si = ServerStatus.VG("server_list").elementAt(x);
                    if (si.getProperty("serverType").startsWith("HTTP") && si.getProperty("port").equals((new StringBuffer(String.valueOf(Integer.parseInt((new String(pb)).trim())))).toString())) {
                      server_item_temp = si;
                      break;
                    } 
                  } 
                  if (server_item_temp == null) {
                    Log.log("SERVER", 2, "GET:SOCKET:Prefered port not found...finding first HTTP(s) item to use...");
                    for (x = 0; x < ServerStatus.VG("server_list").size(); x++) {
                      server_item_temp = ServerStatus.VG("server_list").elementAt(x);
                      if (server_item_temp.getProperty("serverType").equals("HTTP") || server_item_temp.getProperty("serverType").equals("HTTPS"))
                        break; 
                    } 
                  } 
                  if (server_item_temp.getProperty("https_redirect", "false").equalsIgnoreCase("true")) {
                    server_item_temp = (Properties)server_item_temp.clone();
                    server_item_temp.put("https_redirect", "false");
                    Log.log("DMZ", 0, "You must turn off HTTPS redirect on your first HTTP port to prevent DMZ issues.");
                  } 
                  tempSock.setSoTimeout(0);
                  Common.sockLog(tempSock, "tempSock starting protocol handling");
                  QuickConnect quicky = new QuickConnect(this.this$0.thisObj, this.this$0.listen_port, tempSock, this.this$0.the_ip, String.valueOf(this.this$0.listen_ip) + "_" + this.this$0.listen_port, server_item_temp);
                  if (!Worker.startWorker(quicky, String.valueOf(this.this$0.listen_ip) + "_" + this.this$0.listen_port + " --> " + this.this$0.the_ip)) {
                    Common.sockLog(tempSock, "tempSock no workers");
                    tempSock.close();
                    quicky = null;
                    synchronized (this) {
                      this.this$0.connected_users--;
                    } 
                  } 
                } 
                tempSock = null;
                ServerStatus.siPUT("thread_pool_available", (new StringBuffer(String.valueOf(Worker.availableWorkers.size()))).toString());
                ServerStatus.siPUT("thread_pool_busy", (new StringBuffer(String.valueOf(Worker.busyWorkers.size()))).toString());
              } catch (IOException e) {
                Common.sockLog(tempSock, "tempSock IOException:" + e);
                Log.log("DMZ", 2, e);
                Thread.sleep(200L);
              } 
              Common.sockLog(tempSock, "tempSock closing");
              if (tempSock != null)
                tempSock.close(); 
              tempSock = null;
              Thread.sleep(1L);
            } 
          } catch (Exception e) {
            Log.log("DMZ", 0, e);
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
                long start = System.currentTimeMillis();
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
                long end = System.currentTimeMillis();
                if (len > 0) {
                  ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(b));
                  Properties p = (Properties)ois.readObject();
                  ois.close();
                  Thread.currentThread().setName("DMZSender:responseQueue=" + this.this$0.responseQueue.size() + " last write len=" + len + "(" + p.getProperty("type") + ") milliseconds=" + (end - start) + ", total millis=" + (System.currentTimeMillis() - start));
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
            Log.log("DMZ", 0, e);
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
            long last_response_clean = System.currentTimeMillis();
            while (die_now2.length() == 0) {
              while (this.this$0.responseQueue.size() > 0)
                this.this$0.processResponse(this.this$0.responseQueue.remove(0)); 
              if (System.currentTimeMillis() - last_response_clean > 60000L) {
                Enumeration keys = DMZServer.dmzResponses.keys();
                while (keys.hasMoreElements()) {
                  String id = keys.nextElement().toString();
                  Properties p = (Properties)DMZServer.dmzResponses.get(id);
                  if (p != null) {
                    long received = Long.parseLong(p.getProperty("received", "0"));
                    if (System.currentTimeMillis() - received > 60000L) {
                      DMZServer.dmzResponses.remove(id);
                      p.clear();
                    } 
                  } 
                } 
                last_response_clean = System.currentTimeMillis();
              } 
              Thread.sleep(10L);
            } 
          } catch (Exception e) {
            Log.log("DMZ", 0, e);
            this.this$0.socket_created = false;
            die_now2.append(System.currentTimeMillis());
          } 
        }
      };
    Worker.startWorker(r1, "DMZ:SocketConnector:" + this.server_item.getProperty("server_item_name"));
    Worker.startWorker(r2, "DMZ:SocketReceiver:" + this.server_item.getProperty("server_item_name"));
    Worker.startWorker(r3, "DMZ:ResponseProcessor:" + this.server_item.getProperty("server_item_name"));
  }
  
  public static void sendFileToMemory(String path, String dmz_instance) throws Exception {
    if (path != null && !path.equals("") && (new File(path)).exists()) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Common.streamCopier(null, null, new FileInputStream(path), baos, false, true, true);
      Properties pp = new Properties();
      pp.put("bytes", baos.toByteArray());
      Properties system_prop = new Properties();
      system_prop.put("key", "crushftp.keystores." + path.toUpperCase().replace('\\', '/'));
      system_prop.put("val", pp);
      sendCommand(dmz_instance, system_prop, "PUT:SYSTEM.PROPERTIES", "");
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
