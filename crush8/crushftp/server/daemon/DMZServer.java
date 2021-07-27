package crushftp.server.daemon;

import com.crushftp.client.Base64;
import com.crushftp.client.Common;
import com.crushftp.client.File_S;
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
import java.util.Date;
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
  
  File_S prefs_file = null;
  
  public static Properties last_prefs_time = new Properties();
  
  public static Object stop_send_prefs = new Object();
  
  public String last_write_info = "";
  
  long last_ping = System.currentTimeMillis();
  
  long last_pong = System.currentTimeMillis();
  
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
        long start = System.currentTimeMillis();
        long waited = 0L;
        synchronized (dmz.write_sock) {
          waited = System.currentTimeMillis() - start;
          start = System.currentTimeMillis();
          dmz.write_sock.setSoTimeout(10000);
          dmz.write_sock.getOutputStream().write((String.valueOf(b.length) + ":").getBytes());
          dmz.write_sock.getOutputStream().write(b);
          dmz.write_sock.getOutputStream().flush();
          int i = dmz.write_sock.getInputStream().read();
          if (i != 1)
            throw new Exception("Invalid response received from DMZ send:" + i); 
          dmz.messages_sent++;
          dmz.last_write_info = "[Waited " + waited + "ms for send socket, send took " + (System.currentTimeMillis() - start) + "ms for " + b.length + "bytes for " + p.getProperty("type") + " at " + new Date() + "]";
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
      if (Common.dmz_mode) {
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
      while (this.die_now.length() == 0) {
        try {
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
            break;
          } 
          this.read_sock = new Socket(this.listen_ip, this.listen_port);
          this.write_sock = new Socket(this.listen_ip, this.listen_port);
          break;
        } catch (Exception e) {
          this.busyMessage = "ERROR connecting to " + this.listen_ip + ":" + this.listen_port + " " + e;
          Thread.sleep(1000L);
        } 
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
        if (System.currentTimeMillis() - this.last_pong > Integer.parseInt(System.getProperty("crushftp.dmz_pong_timeout", "20000")))
          throw new Exception("No pong reply to ping command after 20 seconds. Restarting DMZ port:" + this.listen_ip + ":" + this.listen_port + "  " + this.last_write_info + " responseQueue_size=" + this.responseQueue.size()); 
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
    this.prefs_file = new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs_" + this.server_item.getProperty("server_item_name") + ".XML");
    Properties instance_server_settings = (Properties)Common.readXMLObject(this.prefs_file);
    instance_server_settings.put("registration_name", ServerStatus.SG("registration_name"));
    instance_server_settings.put("registration_email", ServerStatus.SG("registration_email"));
    instance_server_settings.put("registration_code", ServerStatus.SG("registration_code"));
    instance_server_settings.put("tunnels_dmz", ServerStatus.VG("tunnels"));
    instance_server_settings.put("miniURLs_dmz", ServerStatus.VG("miniURLs"));
    instance_server_settings.put("v8_beta", "true");
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
    if (!p.getProperty("type").equals("PUT:LOGGING")) {
      Log.log("DMZ", 2, "READ:" + this.server_item.getProperty("server_item_name") + ":" + p.getProperty("type") + ":" + p.getProperty("id"));
    } else {
      Log.log("DMZ", Integer.parseInt(p.getProperty("level")), String.valueOf(this.server_item.getProperty("server_item_name")) + ": " + p.getProperty("tag") + ": " + p.getProperty("message"));
      return;
    } 
    if (p.getProperty("type").equalsIgnoreCase("RESPONSE")) {
      p.put("received", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      dmzResponses.put(p.getProperty("id"), p);
    } else if (p.getProperty("type").equalsIgnoreCase("GET:USER_SSH_KEYS")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              // Byte code:
              //   0: aconst_null
              //   1: astore_1
              //   2: iconst_0
              //   3: istore_2
              //   4: goto -> 81
              //   7: ldc 'server_list'
              //   9: invokestatic VG : (Ljava/lang/String;)Ljava/util/Vector;
              //   12: iload_2
              //   13: invokevirtual elementAt : (I)Ljava/lang/Object;
              //   16: checkcast java/util/Properties
              //   19: astore_3
              //   20: aload_3
              //   21: ldc 'serverType'
              //   23: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   26: ldc 'HTTP'
              //   28: invokevirtual startsWith : (Ljava/lang/String;)Z
              //   31: ifeq -> 78
              //   34: aload_3
              //   35: ldc 'port'
              //   37: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   40: new java/lang/StringBuffer
              //   43: dup
              //   44: aload_0
              //   45: getfield val$p2 : Ljava/util/Properties;
              //   48: ldc 'preferred_port'
              //   50: ldc '0'
              //   52: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   55: invokestatic parseInt : (Ljava/lang/String;)I
              //   58: invokestatic valueOf : (I)Ljava/lang/String;
              //   61: invokespecial <init> : (Ljava/lang/String;)V
              //   64: invokevirtual toString : ()Ljava/lang/String;
              //   67: invokevirtual equals : (Ljava/lang/Object;)Z
              //   70: ifeq -> 78
              //   73: aload_3
              //   74: astore_1
              //   75: goto -> 93
              //   78: iinc #2, 1
              //   81: iload_2
              //   82: ldc 'server_list'
              //   84: invokestatic VG : (Ljava/lang/String;)Ljava/util/Vector;
              //   87: invokevirtual size : ()I
              //   90: if_icmplt -> 7
              //   93: aload_1
              //   94: ifnonnull -> 170
              //   97: ldc 'SERVER'
              //   99: iconst_2
              //   100: ldc 'GET:SOCKET:Prefered port not found...finding first HTTP(s) item to use...'
              //   102: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
              //   105: pop
              //   106: iconst_0
              //   107: istore_2
              //   108: goto -> 158
              //   111: ldc 'server_list'
              //   113: invokestatic VG : (Ljava/lang/String;)Ljava/util/Vector;
              //   116: iload_2
              //   117: invokevirtual elementAt : (I)Ljava/lang/Object;
              //   120: checkcast java/util/Properties
              //   123: astore_1
              //   124: aload_1
              //   125: ldc 'serverType'
              //   127: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   130: ldc 'HTTP'
              //   132: invokevirtual equals : (Ljava/lang/Object;)Z
              //   135: ifne -> 170
              //   138: aload_1
              //   139: ldc 'serverType'
              //   141: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   144: ldc 'HTTPS'
              //   146: invokevirtual equals : (Ljava/lang/Object;)Z
              //   149: ifeq -> 155
              //   152: goto -> 170
              //   155: iinc #2, 1
              //   158: iload_2
              //   159: ldc 'server_list'
              //   161: invokestatic VG : (Ljava/lang/String;)Ljava/util/Vector;
              //   164: invokevirtual size : ()I
              //   167: if_icmplt -> 111
              //   170: aload_1
              //   171: ifnonnull -> 182
              //   174: aload_0
              //   175: getfield this$0 : Lcrushftp/server/daemon/DMZServer;
              //   178: getfield server_item : Ljava/util/Properties;
              //   181: astore_1
              //   182: aconst_null
              //   183: astore_2
              //   184: aload_0
              //   185: getfield val$p2 : Ljava/util/Properties;
              //   188: ldc 'linkedServer'
              //   190: aload_1
              //   191: ldc 'linkedServer'
              //   193: ldc 'MainUsers'
              //   195: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   198: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   201: astore_3
              //   202: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
              //   205: aload_3
              //   206: aload_0
              //   207: getfield val$p2 : Ljava/util/Properties;
              //   210: ldc 'username'
              //   212: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   215: iconst_1
              //   216: invokevirtual getUser : (Ljava/lang/String;Ljava/lang/String;Z)Ljava/util/Properties;
              //   219: astore #4
              //   221: aload #4
              //   223: ifnull -> 238
              //   226: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
              //   229: pop
              //   230: ldc 'always_validate_plugins_for_dmz_lookup'
              //   232: invokestatic BG : (Ljava/lang/String;)Z
              //   235: ifeq -> 411
              //   238: new crushftp/handlers/SessionCrush
              //   241: dup
              //   242: aconst_null
              //   243: iconst_1
              //   244: ldc '127.0.0.1'
              //   246: iconst_0
              //   247: ldc '0.0.0.0'
              //   249: aload_3
              //   250: aload_0
              //   251: getfield this$0 : Lcrushftp/server/daemon/DMZServer;
              //   254: getfield server_item : Ljava/util/Properties;
              //   257: invokespecial <init> : (Ljava/net/Socket;ILjava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/util/Properties;)V
              //   260: astore #5
              //   262: aload #5
              //   264: aload_0
              //   265: getfield val$p2 : Ljava/util/Properties;
              //   268: ldc 'username'
              //   270: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   273: aload_0
              //   274: getfield val$p2 : Ljava/util/Properties;
              //   277: ldc 'password'
              //   279: ldc ''
              //   281: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   284: iconst_1
              //   285: iconst_0
              //   286: invokevirtual verify_user : (Ljava/lang/String;Ljava/lang/String;ZZ)Z
              //   289: pop
              //   290: aload #5
              //   292: getfield user : Ljava/util/Properties;
              //   295: astore #4
              //   297: aload #4
              //   299: ldc 'ssh_public_keys'
              //   301: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   304: ldc ''
              //   306: invokevirtual equals : (Ljava/lang/Object;)Z
              //   309: ifeq -> 411
              //   312: new java/util/Properties
              //   315: dup
              //   316: invokespecial <init> : ()V
              //   319: astore #6
              //   321: aload #6
              //   323: ldc 'user'
              //   325: aload #4
              //   327: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   330: pop
              //   331: aload #6
              //   333: ldc 'username'
              //   335: aload_0
              //   336: getfield val$p2 : Ljava/util/Properties;
              //   339: ldc 'username'
              //   341: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   344: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   347: pop
              //   348: aload #6
              //   350: ldc 'password'
              //   352: ldc ''
              //   354: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   357: pop
              //   358: aload #6
              //   360: ldc 'anyPass'
              //   362: ldc 'true'
              //   364: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   367: pop
              //   368: aload #6
              //   370: ldc 'publickey_lookup'
              //   372: ldc 'true'
              //   374: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   377: pop
              //   378: aload #5
              //   380: ldc 'login'
              //   382: aload #6
              //   384: invokevirtual runPlugin : (Ljava/lang/String;Ljava/util/Properties;)Ljava/util/Properties;
              //   387: astore #6
              //   389: aload #4
              //   391: ldc 'ssh_public_keys'
              //   393: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   396: pop
              //   397: goto -> 411
              //   400: astore #6
              //   402: ldc 'DMZ'
              //   404: iconst_1
              //   405: aload #6
              //   407: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
              //   410: pop
              //   411: aload #4
              //   413: ifnull -> 446
              //   416: aload_0
              //   417: getfield val$p2 : Ljava/util/Properties;
              //   420: ldc 'username'
              //   422: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   425: aload #4
              //   427: aload_3
              //   428: invokestatic buildPublicKeys : (Ljava/lang/String;Ljava/util/Properties;Ljava/lang/String;)Ljava/util/Vector;
              //   431: astore_2
              //   432: goto -> 446
              //   435: astore #5
              //   437: ldc 'DMZ'
              //   439: iconst_1
              //   440: aload #5
              //   442: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
              //   445: pop
              //   446: aload #4
              //   448: ifnull -> 498
              //   451: aload #4
              //   453: ldc 'filePublicEncryptionKey'
              //   455: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
              //   458: pop
              //   459: aload #4
              //   461: ldc 'fileEncryptionKey'
              //   463: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
              //   466: pop
              //   467: aload #4
              //   469: ldc 'fileDecryptionKey'
              //   471: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
              //   474: pop
              //   475: aload_0
              //   476: getfield val$p2 : Ljava/util/Properties;
              //   479: ldc 'public_keys'
              //   481: aload_2
              //   482: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   485: pop
              //   486: aload_0
              //   487: getfield val$p2 : Ljava/util/Properties;
              //   490: ldc 'user'
              //   492: aload #4
              //   494: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   497: pop
              //   498: aload_0
              //   499: getfield this$0 : Lcrushftp/server/daemon/DMZServer;
              //   502: getfield server_item : Ljava/util/Properties;
              //   505: ldc 'server_item_name'
              //   507: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   510: aload_0
              //   511: getfield val$p2 : Ljava/util/Properties;
              //   514: ldc 'RESPONSE'
              //   516: aload_0
              //   517: getfield val$p2 : Ljava/util/Properties;
              //   520: ldc 'id'
              //   522: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   525: invokestatic sendCommand : (Ljava/lang/String;Ljava/util/Properties;Ljava/lang/String;Ljava/lang/String;)V
              //   528: return
              // Line number table:
              //   Java source line number -> byte code offset
              //   #377	-> 0
              //   #378	-> 2
              //   #380	-> 7
              //   #381	-> 20
              //   #383	-> 73
              //   #384	-> 75
              //   #378	-> 78
              //   #387	-> 93
              //   #389	-> 97
              //   #390	-> 106
              //   #392	-> 111
              //   #393	-> 124
              //   #390	-> 155
              //   #396	-> 170
              //   #398	-> 182
              //   #399	-> 184
              //   #401	-> 202
              //   #402	-> 221
              //   #404	-> 238
              //   #407	-> 262
              //   #408	-> 290
              //   #409	-> 297
              //   #411	-> 312
              //   #412	-> 321
              //   #413	-> 331
              //   #414	-> 348
              //   #415	-> 358
              //   #416	-> 368
              //   #417	-> 378
              //   #418	-> 389
              //   #421	-> 400
              //   #423	-> 402
              //   #428	-> 411
              //   #430	-> 435
              //   #432	-> 437
              //   #434	-> 446
              //   #436	-> 451
              //   #437	-> 459
              //   #438	-> 467
              //   #439	-> 475
              //   #440	-> 486
              //   #442	-> 498
              //   #443	-> 528
              // Local variable table:
              //   start	length	slot	name	descriptor
              //   0	529	0	this	Lcrushftp/server/daemon/DMZServer$2;
              //   2	527	1	server_item_temp	Ljava/util/Properties;
              //   4	89	2	x	I
              //   20	58	3	si	Ljava/util/Properties;
              //   108	62	2	x	I
              //   184	345	2	public_keys	Ljava/util/Vector;
              //   202	327	3	linkedServer	Ljava/lang/String;
              //   221	308	4	user	Ljava/util/Properties;
              //   262	149	5	tempSession	Lcrushftp/handlers/SessionCrush;
              //   321	76	6	pp	Ljava/util/Properties;
              //   402	9	6	e	Ljava/lang/Exception;
              //   437	9	5	e	Ljava/io/IOException;
              // Exception table:
              //   from	to	target	type
              //   262	397	400	java/lang/Exception
              //   411	432	435	java/io/IOException
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:USER")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              // Byte code:
              //   0: aconst_null
              //   1: astore_1
              //   2: iconst_0
              //   3: istore_2
              //   4: goto -> 81
              //   7: ldc 'server_list'
              //   9: invokestatic VG : (Ljava/lang/String;)Ljava/util/Vector;
              //   12: iload_2
              //   13: invokevirtual elementAt : (I)Ljava/lang/Object;
              //   16: checkcast java/util/Properties
              //   19: astore_3
              //   20: aload_3
              //   21: ldc 'serverType'
              //   23: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   26: ldc 'HTTP'
              //   28: invokevirtual startsWith : (Ljava/lang/String;)Z
              //   31: ifeq -> 78
              //   34: aload_3
              //   35: ldc 'port'
              //   37: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   40: new java/lang/StringBuffer
              //   43: dup
              //   44: aload_0
              //   45: getfield val$p2 : Ljava/util/Properties;
              //   48: ldc 'preferred_port'
              //   50: ldc '0'
              //   52: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   55: invokestatic parseInt : (Ljava/lang/String;)I
              //   58: invokestatic valueOf : (I)Ljava/lang/String;
              //   61: invokespecial <init> : (Ljava/lang/String;)V
              //   64: invokevirtual toString : ()Ljava/lang/String;
              //   67: invokevirtual equals : (Ljava/lang/Object;)Z
              //   70: ifeq -> 78
              //   73: aload_3
              //   74: astore_1
              //   75: goto -> 93
              //   78: iinc #2, 1
              //   81: iload_2
              //   82: ldc 'server_list'
              //   84: invokestatic VG : (Ljava/lang/String;)Ljava/util/Vector;
              //   87: invokevirtual size : ()I
              //   90: if_icmplt -> 7
              //   93: aload_1
              //   94: ifnonnull -> 170
              //   97: ldc 'SERVER'
              //   99: iconst_2
              //   100: ldc 'GET:SOCKET:Prefered port not found...finding first HTTP(s) item to use...'
              //   102: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
              //   105: pop
              //   106: iconst_0
              //   107: istore_2
              //   108: goto -> 158
              //   111: ldc 'server_list'
              //   113: invokestatic VG : (Ljava/lang/String;)Ljava/util/Vector;
              //   116: iload_2
              //   117: invokevirtual elementAt : (I)Ljava/lang/Object;
              //   120: checkcast java/util/Properties
              //   123: astore_1
              //   124: aload_1
              //   125: ldc 'serverType'
              //   127: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   130: ldc 'HTTP'
              //   132: invokevirtual equals : (Ljava/lang/Object;)Z
              //   135: ifne -> 170
              //   138: aload_1
              //   139: ldc 'serverType'
              //   141: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   144: ldc 'HTTPS'
              //   146: invokevirtual equals : (Ljava/lang/Object;)Z
              //   149: ifeq -> 155
              //   152: goto -> 170
              //   155: iinc #2, 1
              //   158: iload_2
              //   159: ldc 'server_list'
              //   161: invokestatic VG : (Ljava/lang/String;)Ljava/util/Vector;
              //   164: invokevirtual size : ()I
              //   167: if_icmplt -> 111
              //   170: aload_1
              //   171: ifnonnull -> 182
              //   174: aload_0
              //   175: getfield this$0 : Lcrushftp/server/daemon/DMZServer;
              //   178: getfield server_item : Ljava/util/Properties;
              //   181: astore_1
              //   182: aconst_null
              //   183: astore_2
              //   184: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
              //   187: aload_1
              //   188: ldc 'linkedServer'
              //   190: ldc ''
              //   192: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   195: aload_0
              //   196: getfield val$p2 : Ljava/util/Properties;
              //   199: ldc 'username'
              //   201: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   204: iconst_1
              //   205: invokevirtual getUser : (Ljava/lang/String;Ljava/lang/String;Z)Ljava/util/Properties;
              //   208: astore_3
              //   209: aconst_null
              //   210: astore #4
              //   212: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
              //   215: aload_1
              //   216: ldc 'linkedServer'
              //   218: ldc ''
              //   220: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   223: aload_0
              //   224: getfield val$p2 : Ljava/util/Properties;
              //   227: ldc 'username'
              //   229: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   232: invokevirtual getVFS : (Ljava/lang/String;Ljava/lang/String;)Lcrushftp/server/VFS;
              //   235: astore #4
              //   237: goto -> 253
              //   240: astore #5
              //   242: ldc 'SERVER'
              //   244: iconst_0
              //   245: aload #5
              //   247: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
              //   250: pop
              //   251: aconst_null
              //   252: astore_3
              //   253: aload_3
              //   254: ifnull -> 269
              //   257: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
              //   260: pop
              //   261: ldc 'always_validate_plugins_for_dmz_lookup'
              //   263: invokestatic BG : (Ljava/lang/String;)Z
              //   266: ifeq -> 362
              //   269: new crushftp/handlers/SessionCrush
              //   272: dup
              //   273: aconst_null
              //   274: iconst_1
              //   275: ldc '127.0.0.1'
              //   277: iconst_0
              //   278: ldc '0.0.0.0'
              //   280: aload_1
              //   281: ldc 'linkedServer'
              //   283: ldc ''
              //   285: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   288: aload_1
              //   289: invokespecial <init> : (Ljava/net/Socket;ILjava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/util/Properties;)V
              //   292: astore #5
              //   294: aload #5
              //   296: getfield user_info : Ljava/util/Properties;
              //   299: ldc 'request'
              //   301: aload_0
              //   302: getfield val$p2 : Ljava/util/Properties;
              //   305: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   308: pop
              //   309: aload #5
              //   311: aload_0
              //   312: getfield val$p2 : Ljava/util/Properties;
              //   315: ldc 'username'
              //   317: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   320: aload_0
              //   321: getfield val$p2 : Ljava/util/Properties;
              //   324: ldc 'password'
              //   326: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   329: iconst_0
              //   330: iconst_0
              //   331: invokevirtual verify_user : (Ljava/lang/String;Ljava/lang/String;ZZ)Z
              //   334: pop
              //   335: aload #5
              //   337: getfield user : Ljava/util/Properties;
              //   340: astore_3
              //   341: aload #5
              //   343: getfield uVFS : Lcrushftp/server/VFS;
              //   346: astore #4
              //   348: goto -> 362
              //   351: astore #6
              //   353: ldc 'DMZ'
              //   355: iconst_1
              //   356: aload #6
              //   358: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
              //   361: pop
              //   362: aload_3
              //   363: ifnull -> 388
              //   366: aload_0
              //   367: getfield val$p2 : Ljava/util/Properties;
              //   370: ldc 'username'
              //   372: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   375: aload_3
              //   376: aload_1
              //   377: ldc 'linkedServer'
              //   379: ldc ''
              //   381: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   384: invokestatic buildPublicKeys : (Ljava/lang/String;Ljava/util/Properties;Ljava/lang/String;)Ljava/util/Vector;
              //   387: astore_2
              //   388: aload_3
              //   389: ifnull -> 743
              //   392: aload_3
              //   393: ldc 'as2EncryptKeystorePath'
              //   395: ldc ''
              //   397: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   400: aload_0
              //   401: getfield this$0 : Lcrushftp/server/daemon/DMZServer;
              //   404: getfield server_item : Ljava/util/Properties;
              //   407: ldc 'server_item_name'
              //   409: ldc ''
              //   411: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   414: invokestatic sendFileToMemory : (Ljava/lang/String;Ljava/lang/String;)V
              //   417: aload_3
              //   418: ldc 'as2SignKeystorePath'
              //   420: ldc ''
              //   422: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   425: aload_0
              //   426: getfield this$0 : Lcrushftp/server/daemon/DMZServer;
              //   429: getfield server_item : Ljava/util/Properties;
              //   432: ldc 'server_item_name'
              //   434: ldc ''
              //   436: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   439: invokestatic sendFileToMemory : (Ljava/lang/String;Ljava/lang/String;)V
              //   442: aload_1
              //   443: ldc 'linkedServer'
              //   445: ldc ''
              //   447: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   450: aload_0
              //   451: getfield val$p2 : Ljava/util/Properties;
              //   454: ldc 'username'
              //   456: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   459: aload #4
              //   461: aload_3
              //   462: invokestatic setupVFSLinking : (Ljava/lang/String;Ljava/lang/String;Lcrushftp/server/VFS;Ljava/util/Properties;)V
              //   465: aload_3
              //   466: ldc 'filePublicEncryptionKey'
              //   468: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
              //   471: pop
              //   472: aload_3
              //   473: ldc 'fileEncryptionKey'
              //   475: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
              //   478: pop
              //   479: aload_3
              //   480: ldc 'fileDecryptionKey'
              //   482: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
              //   485: pop
              //   486: aload_3
              //   487: ldc 'otp_auth'
              //   489: ldc ''
              //   491: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   494: ldc 'true'
              //   496: invokevirtual equals : (Ljava/lang/Object;)Z
              //   499: ifeq -> 706
              //   502: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
              //   505: getfield server_info : Ljava/util/Properties;
              //   508: ldc 'otp_tokens'
              //   510: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
              //   513: checkcast java/util/Properties
              //   516: astore #5
              //   518: aload #5
              //   520: ifnull -> 706
              //   523: aload #5
              //   525: new java/lang/StringBuffer
              //   528: dup
              //   529: aload_0
              //   530: getfield val$p2 : Ljava/util/Properties;
              //   533: ldc 'username'
              //   535: ldc ''
              //   537: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   540: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
              //   543: invokespecial <init> : (Ljava/lang/String;)V
              //   546: ldc '127.0.0.1'
              //   548: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
              //   551: invokevirtual toString : ()Ljava/lang/String;
              //   554: invokevirtual containsKey : (Ljava/lang/Object;)Z
              //   557: ifeq -> 706
              //   560: aload_0
              //   561: getfield val$p2 : Ljava/util/Properties;
              //   564: ldc 'password'
              //   566: ldc ''
              //   568: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   571: ldc ':'
              //   573: invokevirtual indexOf : (Ljava/lang/String;)I
              //   576: iflt -> 706
              //   579: aload #5
              //   581: new java/lang/StringBuffer
              //   584: dup
              //   585: aload_0
              //   586: getfield val$p2 : Ljava/util/Properties;
              //   589: ldc 'username'
              //   591: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   594: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
              //   597: invokespecial <init> : (Ljava/lang/String;)V
              //   600: ldc '127.0.0.1'
              //   602: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
              //   605: invokevirtual toString : ()Ljava/lang/String;
              //   608: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
              //   611: checkcast java/util/Properties
              //   614: astore #6
              //   616: aload_0
              //   617: getfield val$p2 : Ljava/util/Properties;
              //   620: ldc 'password'
              //   622: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   625: ldc ':'
              //   627: invokevirtual indexOf : (Ljava/lang/String;)I
              //   630: iflt -> 697
              //   633: aload #6
              //   635: ldc 'token'
              //   637: ldc ''
              //   639: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   642: aload_0
              //   643: getfield val$p2 : Ljava/util/Properties;
              //   646: ldc 'password'
              //   648: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   651: aload_0
              //   652: getfield val$p2 : Ljava/util/Properties;
              //   655: ldc 'password'
              //   657: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   660: ldc ':'
              //   662: invokevirtual lastIndexOf : (Ljava/lang/String;)I
              //   665: iconst_1
              //   666: iadd
              //   667: invokevirtual substring : (I)Ljava/lang/String;
              //   670: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
              //   673: ifeq -> 697
              //   676: ldc 'LOGIN'
              //   678: iconst_1
              //   679: ldc 'DMZ CHALLENGE_OTP : OTP token is valid.'
              //   681: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
              //   684: pop
              //   685: aload_3
              //   686: ldc 'otp_valid'
              //   688: ldc 'true'
              //   690: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   693: pop
              //   694: goto -> 706
              //   697: aload_3
              //   698: ldc 'otp_valid'
              //   700: ldc 'false'
              //   702: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   705: pop
              //   706: aload_0
              //   707: getfield val$p2 : Ljava/util/Properties;
              //   710: ldc 'public_keys'
              //   712: aload_2
              //   713: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   716: pop
              //   717: aload_0
              //   718: getfield val$p2 : Ljava/util/Properties;
              //   721: ldc 'user'
              //   723: aload_3
              //   724: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   727: pop
              //   728: aload_0
              //   729: getfield val$p2 : Ljava/util/Properties;
              //   732: ldc 'vfs'
              //   734: aload #4
              //   736: getfield homes : Ljava/util/Vector;
              //   739: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   742: pop
              //   743: aload_0
              //   744: getfield this$0 : Lcrushftp/server/daemon/DMZServer;
              //   747: getfield server_item : Ljava/util/Properties;
              //   750: ldc 'server_item_name'
              //   752: ldc ''
              //   754: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
              //   757: aload_0
              //   758: getfield val$p2 : Ljava/util/Properties;
              //   761: ldc_w 'RESPONSE'
              //   764: aload_0
              //   765: getfield val$p2 : Ljava/util/Properties;
              //   768: ldc_w 'id'
              //   771: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
              //   774: invokestatic sendCommand : (Ljava/lang/String;Ljava/util/Properties;Ljava/lang/String;Ljava/lang/String;)V
              //   777: goto -> 789
              //   780: astore_1
              //   781: ldc 'DMZ'
              //   783: iconst_0
              //   784: aload_1
              //   785: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
              //   788: pop
              //   789: return
              // Line number table:
              //   Java source line number -> byte code offset
              //   #454	-> 0
              //   #455	-> 2
              //   #457	-> 7
              //   #458	-> 20
              //   #460	-> 73
              //   #461	-> 75
              //   #455	-> 78
              //   #464	-> 93
              //   #466	-> 97
              //   #467	-> 106
              //   #469	-> 111
              //   #470	-> 124
              //   #467	-> 155
              //   #473	-> 170
              //   #475	-> 182
              //   #476	-> 184
              //   #477	-> 209
              //   #480	-> 212
              //   #482	-> 240
              //   #484	-> 242
              //   #485	-> 251
              //   #487	-> 253
              //   #489	-> 269
              //   #492	-> 294
              //   #493	-> 309
              //   #494	-> 335
              //   #495	-> 341
              //   #497	-> 351
              //   #499	-> 353
              //   #502	-> 362
              //   #503	-> 388
              //   #505	-> 392
              //   #506	-> 417
              //   #507	-> 442
              //   #508	-> 465
              //   #509	-> 472
              //   #510	-> 479
              //   #511	-> 486
              //   #513	-> 502
              //   #514	-> 518
              //   #516	-> 579
              //   #517	-> 616
              //   #519	-> 676
              //   #520	-> 685
              //   #522	-> 697
              //   #525	-> 706
              //   #526	-> 717
              //   #527	-> 728
              //   #529	-> 743
              //   #531	-> 780
              //   #533	-> 781
              //   #535	-> 789
              // Local variable table:
              //   start	length	slot	name	descriptor
              //   0	790	0	this	Lcrushftp/server/daemon/DMZServer$3;
              //   2	778	1	server_item_temp	Ljava/util/Properties;
              //   4	89	2	x	I
              //   20	58	3	si	Ljava/util/Properties;
              //   108	62	2	x	I
              //   184	596	2	public_keys	Ljava/util/Vector;
              //   209	571	3	user	Ljava/util/Properties;
              //   212	568	4	uVFS	Lcrushftp/server/VFS;
              //   242	11	5	e	Ljava/lang/Exception;
              //   294	68	5	tempSession	Lcrushftp/handlers/SessionCrush;
              //   353	9	6	e	Ljava/lang/Exception;
              //   518	188	5	otp_tokens	Ljava/util/Properties;
              //   616	90	6	token	Ljava/util/Properties;
              //   781	8	1	e	Ljava/lang/Exception;
              // Exception table:
              //   from	to	target	type
              //   0	777	780	java/lang/Exception
              //   212	237	240	java/lang/Exception
              //   294	348	351	java/lang/Exception
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:RESET_TOKEN")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              try {
                Properties server_item2 = (Properties)this.this$0.server_item.clone();
                if (!this.val$p2.getProperty("internal_port", "").equals("")) {
                  Vector v = ServerStatus.VG("server_list");
                  for (int x = 0; x < v.size(); x++) {
                    Properties server_item3 = v.elementAt(x);
                    if ((server_item3.getProperty("serverType").equalsIgnoreCase("HTTP") || server_item3.getProperty("serverType").equalsIgnoreCase("HTTPS")) && server_item3.getProperty("port").equals(this.val$p2.getProperty("internal_port"))) {
                      server_item2 = server_item3;
                      break;
                    } 
                  } 
                } else if (this.this$0.server_item.getProperty("linkedServer", "").equals("@AutoHostHttp")) {
                  try {
                    VRL vrl = new VRL(this.val$p2.getProperty("currentURL"));
                    Vector v = ServerStatus.VG("login_page_list");
                    for (int x = 0; x < v.size(); x++) {
                      Properties p = v.elementAt(x);
                      if (Common.do_search(p.getProperty("domain"), vrl.getHost(), false, 0)) {
                        String stem = p.getProperty("page").substring(0, p.getProperty("page").lastIndexOf("."));
                        server_item2.put("linkedServer", stem);
                        break;
                      } 
                    } 
                  } catch (Exception e) {
                    Log.log("HTTP_SERVER", 1, e);
                  } 
                } 
                String responseText = ServerSessionAJAX.doResetToken(this.val$p2.getProperty("reset_username_email"), this.val$p2.getProperty("currentURL"), server_item2.getProperty("linkedServer", ""), this.val$p2.getProperty("reset_token"), this.this$0.singleton_id.equals(this.val$p2.getProperty("singleton_id", this.this$0.singleton_id)));
                this.val$p2.put("responseText", responseText);
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
              } catch (Exception e) {
                Log.log("SERVER", 0, e);
              } 
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:RECAPTCHA_RESPONSE")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              try {
                this.val$p2.put("responseText", ServerSessionAJAX.getRecaptchaResponse(this.val$p2.getProperty("recapcha_info")));
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
              } catch (Exception e) {
                Log.log("SERVER", 0, e);
              } 
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:GENERATE_TOKEN")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              try {
                Properties request = (Properties)this.val$p2.remove("request");
                request.put("method", "generateToken");
                request.put("pluginName", "CrushSSO");
                request.put("pluginSubItem", request.getProperty("pluginSubItem", ""));
                this.val$p2.put("responseText", AdminControls.pluginMethodCall(Common.urlDecodePost(request), this.val$p2.getProperty("site", "")));
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
              } catch (Exception e) {
                Log.log("SERVER", 0, e);
              } 
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:SINGLETON")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              this.val$p2.put("singleton_id", this.this$0.singleton_id);
              DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:RESET_TOKEN_PASS")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              Properties server_item2 = (Properties)this.this$0.server_item.clone();
              if (!this.val$p2.getProperty("internal_port", "").equals("")) {
                Vector v = ServerStatus.VG("server_list");
                for (int x = 0; x < v.size(); x++) {
                  Properties server_item3 = v.elementAt(x);
                  if ((server_item3.getProperty("serverType").equalsIgnoreCase("HTTP") || server_item3.getProperty("serverType").equalsIgnoreCase("HTTPS")) && server_item3.getProperty("port").equals(this.val$p2.getProperty("internal_port"))) {
                    server_item2 = server_item3;
                    break;
                  } 
                } 
              } else if (this.this$0.server_item.getProperty("linkedServer", "").equals("@AutoHostHttp")) {
                try {
                  VRL vrl = new VRL(this.val$p2.getProperty("currentURL"));
                  Vector v = ServerStatus.VG("login_page_list");
                  for (int x = 0; x < v.size(); x++) {
                    Properties p = v.elementAt(x);
                    if (Common.do_search(p.getProperty("domain"), vrl.getHost(), false, 0)) {
                      String stem = p.getProperty("page").substring(0, p.getProperty("page").lastIndexOf("."));
                      server_item2.put("linkedServer", stem);
                      break;
                    } 
                  } 
                } catch (Exception e) {
                  Log.log("HTTP_SERVER", 1, e);
                } 
              } 
              String responseText = ServerSessionAJAX.doResetTokenPass(this.val$p2.getProperty("resetToken"), server_item2.getProperty("linkedServer", ""), this.val$p2.getProperty("password1"), new Properties());
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
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:MESSAGEFORM")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              Properties request = (Properties)this.val$p2.remove("request");
              SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.val$p2.getProperty("crushAuth"));
              if (thisSession != null && thisSession.uVFS != null) {
                String response = ServerSessionAJAX.handle_message_form(request, thisSession);
                this.val$p2.put("object_response", response);
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
              } 
            }
          });
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
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              Properties error_info = (Properties)this.val$p2.get("error_info");
              SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.val$p2.getProperty("crushAuth"));
              if (thisSession != null && thisSession.uVFS != null) {
                Properties ui2 = (Properties)this.val$p2.get("error_user_info");
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
            }
          });
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
      this.last_pong = System.currentTimeMillis();
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:ALERT")) {
      Properties alert_info = p;
      Worker.startWorker(new Runnable(this, alert_info) {
            final DMZServer this$0;
            
            private final Properties val$alert_info;
            
            public void run() {
              ServerStatus.thisObj.runAlerts(this.val$alert_info.getProperty("alert_action"), (Properties)this.val$alert_info.get("info"), (Properties)this.val$alert_info.get("user_info"), null, (Properties)this.val$alert_info.get("alert"), true);
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
                SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.val$p2.getProperty("crushAuth"));
                if (thisSession != null && thisSession.uVFS != null) {
                  String root_dir = SessionCrush.getRootDir(null, thisSession.uVFS, null, true);
                  String the_dir = String.valueOf(root_dir) + this.val$p2.getProperty("the_dir").substring(1);
                  q = (new StringBuffer(String.valueOf(SessionCrush.get_quota(the_dir, thisSession.uVFS, "", new Properties(), null, true)))).toString();
                  q = String.valueOf(q) + ":" + SessionCrush.get_quota(the_dir, thisSession.uVFS, "", new Properties(), null, false);
                } 
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
                SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.val$p2.getProperty("crushAuth"));
                if (thisSession != null && thisSession.uVFS != null) {
                  String root_dir = SessionCrush.getRootDir(null, thisSession.uVFS, null, true);
                  String the_dir = String.valueOf(root_dir) + this.val$p2.getProperty("the_dir").substring(1);
                  q = SessionCrush.get_quota_used(the_dir, thisSession.uVFS, "", null);
                } 
              } catch (Exception e) {
                Log.log("DMZ", 1, e);
              } 
              this.val$p2.put("object_response", (new StringBuffer(String.valueOf(q))).toString());
              DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:CHANGE_PASSWORD")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              this.val$p2.put("object_response", "");
              SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.val$p2.getProperty("crushAuth", ""));
              if (thisSession == null)
                try {
                  Properties server_item_temp = null;
                  int x;
                  for (x = 0; x < ServerStatus.VG("server_list").size(); x++) {
                    Properties si = ServerStatus.VG("server_list").elementAt(x);
                    if (si.getProperty("serverType").startsWith("HTTP") && si.getProperty("port").equals((new StringBuffer(String.valueOf(Integer.parseInt(this.val$p2.getProperty("preferred_port", "0"))))).toString())) {
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
                  if (server_item_temp == null)
                    server_item_temp = this.this$0.server_item; 
                  SessionCrush tempSession = new SessionCrush(null, 1, "127.0.0.1", 0, "0.0.0.0", server_item_temp.getProperty("linkedServer", ""), server_item_temp);
                  try {
                    tempSession.verify_user(this.val$p2.getProperty("username"), this.val$p2.getProperty("current_password"), false, false);
                    tempSession.put("user_name", this.val$p2.getProperty("username"));
                    thisSession = tempSession;
                  } catch (Exception e) {
                    Log.log("DMZ", 1, e);
                  } 
                } catch (Exception e) {
                  Log.log("DMZ", 0, e);
                }  
              if (thisSession != null && thisSession.uVFS != null) {
                Properties request = (Properties)this.val$p2.remove("request");
                String response = ServerSessionAJAX.changePassword(request, (String)this.val$p2.remove("site"), thisSession);
                this.val$p2.put("object_response", response);
              } 
              DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:EDIT_KEYWORDS")) {
      Properties request = (Properties)p.remove("request");
      p.put("object_response", "");
      SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("crushAuth"));
      if (thisSession != null && thisSession.uVFS != null) {
        String response = ServerSessionAJAX.processKeywordsEdit(request, thisSession);
        p.put("object_response", response);
      } 
      sendCommand(this.server_item.getProperty("server_item_name"), p, "RESPONSE", p.getProperty("id"));
    } else if (p.getProperty("type").equalsIgnoreCase("GET:SEARCH")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              Properties request = (Properties)this.val$p2.remove("request");
              this.val$p2.put("object_response", "");
              SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.val$p2.getProperty("crushAuth"));
              if (thisSession != null && thisSession.uVFS != null) {
                Exception exception;
                String response = "";
                try {
                  response = ServerSessionAJAX.search(request, thisSession);
                } catch (Exception e) {
                  exception = e;
                  Log.log("DMZ", 1, e);
                } 
                this.val$p2.put("object_response", exception);
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
              } 
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:SEARCH_STATUS")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              this.val$p2.put("object_response", "");
              SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.val$p2.getProperty("crushAuth"));
              if (thisSession != null && thisSession.uVFS != null) {
                this.val$p2.put("object_response", thisSession.uiSG("search_status").trim());
                thisSession.uVFS.reset();
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
              } 
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("GET:HANDLE_CUSTOMIZATIONS")) {
      Properties p2 = p;
      Worker.startWorker(new Runnable(this, p2) {
            final DMZServer this$0;
            
            private final Properties val$p2;
            
            public void run() {
              Vector customizations = (Vector)this.val$p2.remove("customizations");
              this.val$p2.put("object_response", "");
              SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(this.val$p2.getProperty("crushAuth"));
              if (thisSession != null && thisSession.uVFS != null) {
                String response = "";
                try {
                  ServerSessionAJAX.handleCustomizations(customizations, thisSession);
                } catch (Exception e) {
                  Exception exception1 = e;
                  Log.log("DMZ", 1, e);
                } 
                this.val$p2.put("object_response", customizations);
              } 
              DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), this.val$p2, "RESPONSE", this.val$p2.getProperty("id"));
            }
          });
    } 
  }
  
  public void startSocketConnectors() throws Exception {
    Runnable r1 = new Runnable(this) {
        final DMZServer this$0;
        
        public void run() {
          Thread.currentThread().setPriority(1);
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
                Common.sockLog(tempSock, "tempSock create " + (ServerBeat.current_master ? "D" : "d"));
                tempSock.setSoTimeout(12000);
                if (ServerBeat.current_master) {
                  tempSock.getOutputStream().write("D".getBytes());
                } else {
                  tempSock.getOutputStream().write("d".getBytes());
                } 
                tempSock.getOutputStream().flush();
                byte[] pb = new byte[100];
                int bytesRead = tempSock.getInputStream().read(pb);
                if (bytesRead == 0) {
                  Thread.sleep(1000L);
                  bytesRead = tempSock.getInputStream().read(pb);
                } 
                if ((new String(pb)).trim().equals(""))
                  throw new IOException("DMZ socket received invalid prefered port."); 
                Common.sockLog(tempSock, "tempSock create " + (ServerBeat.current_master ? "D" : "d") + " -> " + new String(pb));
                boolean reverse = false;
                if ((new String(pb)).trim().indexOf(":") > 0) {
                  reverse = true;
                  if (!(new String(pb)).trim().split(":")[0].trim().equals("")) {
                    tempSock.setSoTimeout(0);
                    Socket sock = new Socket((new String(pb)).trim().split(":")[0], Integer.parseInt((new String(pb)).trim().split(":")[1]));
                    if (((new String(pb)).trim().split(":")).length > 2) {
                      String inet_protocol = (((new String(pb)).trim().split(":")[2].split("\\.")).length == 4) ? "TCP4" : "TCP6";
                      String proxy_protocol_v1 = "PROXY " + inet_protocol + " " + (new String(pb)).trim().split(":")[2] + " " + (new String(pb)).trim().split(":")[0] + " " + (new String(pb)).trim().split(":")[3] + " " + (new String(pb)).trim().split(":")[1] + "\r\n";
                      sock.getOutputStream().write(proxy_protocol_v1.getBytes("UTF8"));
                    } 
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
                  QuickConnect quicky = new QuickConnect(this.this$0.thisObj, this.this$0.listen_port, tempSock, this.this$0.the_ip, String.valueOf(this.this$0.listen_ip) + "_" + this.this$0.listen_port, server_item_temp, "");
                  if (!Worker.startWorker(quicky, String.valueOf(this.this$0.listen_ip) + "_" + this.this$0.listen_port + " --> " + this.this$0.the_ip)) {
                    Common.sockLog(tempSock, "tempSock no workers");
                    tempSock.close();
                    quicky = null;
                    synchronized (this) {
                      this.this$0.connected_users--;
                      if (this.this$0.connected_users < 0)
                        this.this$0.connected_users = 0; 
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
          } finally {
            Thread.currentThread().setPriority(5);
          } 
        }
      };
    Runnable r2 = new Runnable(this) {
        final DMZServer this$0;
        
        public void run() {
          Thread.currentThread().setPriority(10);
          StringBuffer die_now2 = this.this$0.die_now;
          try {
            InputStream in = this.this$0.read_sock.getInputStream();
            OutputStream out = this.this$0.read_sock.getOutputStream();
            byte[] b1 = new byte[1];
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
                  Thread.currentThread().setName("DMZSender:responseQueue=" + this.this$0.responseQueue.size() + " last write len=" + len + "(" + p.getProperty("type") + ") milliseconds=" + (end - start) + ", total millis=" + (System.currentTimeMillis() - start) + " last_write_info:" + this.this$0.last_write_info);
                  this.this$0.responseQueue.addElement(p);
                  this.this$0.messages_received++;
                } 
              } catch (SocketTimeoutException socketTimeoutException) {}
              if (System.currentTimeMillis() - this.this$0.last_ping > 10000L) {
                Properties ping = new Properties();
                ping.put("id", Common.makeBoundary());
                ping.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
                DMZServer.sendCommand(this.this$0.server_item.getProperty("server_item_name"), ping, "PUT:PING", ping.getProperty("id"));
                this.this$0.last_ping = System.currentTimeMillis();
              } 
              if (System.currentTimeMillis() - this.this$0.last_ping > Integer.parseInt(System.getProperty("crushftp.dmz_pong_timeout", "20000")))
                throw new Exception("Socket timeout, firewall killed socket."); 
            } 
          } catch (Exception e) {
            Log.log("DMZ", 0, e);
            this.this$0.socket_created = false;
            this.this$0.restart = true;
            die_now2.append(System.currentTimeMillis());
          } finally {
            Thread.currentThread().setPriority(5);
          } 
        }
      };
    Runnable r3 = new Runnable(this) {
        final DMZServer this$0;
        
        public void run() {
          Thread.currentThread().setPriority(10);
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
          } finally {
            Thread.currentThread().setPriority(5);
          } 
        }
      };
    Worker.startWorker(r1, "DMZ:SocketConnector:" + this.server_item.getProperty("server_item_name"));
    Worker.startWorker(r2, "DMZ:SocketReceiver:" + this.server_item.getProperty("server_item_name"));
    Worker.startWorker(r3, "DMZ:ResponseProcessor:" + this.server_item.getProperty("server_item_name"));
  }
  
  public static void sendFileToMemory(String path, String dmz_instance) throws Exception {
    if (path != null && !path.equals("") && (new File_S(path)).exists()) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Common.streamCopier(null, null, new FileInputStream(new File_S(path)), baos, false, true, true);
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
