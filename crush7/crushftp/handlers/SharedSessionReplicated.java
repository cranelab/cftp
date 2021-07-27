package crushftp.handlers;

import com.crushftp.client.Common;
import com.crushftp.client.S3CrushClient;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import crushftp.server.AdminControls;
import crushftp.server.ServerStatus;
import crushftp.server.daemon.DMZServer;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.net.ssl.SSLSocketFactory;

public class SharedSessionReplicated {
  public static Vector pending_user_sync = new Vector();
  
  public static Vector pending_job_sync = new Vector();
  
  public static Properties send_queues = new Properties();
  
  public static Vector remote_host_ports = new Vector();
  
  static SSLSocketFactory factory = null;
  
  public static Object send_lock = new Object();
  
  static boolean offline = false;
  
  static Vector replicatedItems = new Vector();
  
  static long sync_delay = 0L;
  
  static long lastActive = 0L;
  
  static Properties pendingResponses = new Properties();
  
  public static void init() {
    try {
      if (ServerStatus.siIG("enterprise_level") <= 0)
        throw new Exception("SharedSession only valid for Enterprise licenses."); 
      replicatedItems.addElement("crushftp.usernames");
      replicatedItems.addElement("crushftp.sessions");
      if ((new File(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster.xml")).exists()) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Common.streamCopier(new FileInputStream(new File(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster.xml")), baos, false);
        String xml = new String(baos.toByteArray(), "UTF8");
        xml = xml.substring(xml.indexOf("initial_hosts") + "initial_hosts".length());
        xml = xml.substring(0, xml.indexOf("]"));
        xml = xml.substring(xml.indexOf("\"") + 1);
        remote_host_ports.addElement(xml.replace('[', ':').trim());
      } else {
        String our_hostname = "UNKNOWN";
        try {
          our_hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
        } 
        String s = ServerStatus.SG("replicate_session_host_port");
        for (int x = 0; x < (s.split(",")).length; x++) {
          if (!s.split(",")[x].trim().equals(""))
            if (s.indexOf(",") >= 0 && s.split(",")[x].trim().toUpperCase().indexOf(our_hostname.toUpperCase()) < 0) {
              remote_host_ports.addElement(s.split(",")[x].trim());
            } else if (s.indexOf(",") < 0) {
              remote_host_ports.addElement(s.split(",")[x].trim());
            }  
        } 
      } 
      ServerStatus.thisObj.server_info.put("replicated_servers", remote_host_ports);
      ServerStatus.thisObj.server_info.put("replicated_servers_count", (new StringBuffer(String.valueOf(remote_host_ports.size()))).toString());
      for (int xx = 0; xx < remote_host_ports.size(); xx++)
        send_queues.put(remote_host_ports.elementAt(xx), new Vector()); 
      Vector bind_ports = new Vector();
      for (int i = 0; i < remote_host_ports.size(); i++) {
        String remote_host_port = remote_host_ports.elementAt(i).toString();
        Worker.startWorker(new Runnable(remote_host_port, bind_ports) {
              private final String val$remote_host_port;
              
              private final Vector val$bind_ports;
              
              public void run() {
                try {
                  ServerSocket ss = null;
                  int bind_port = 0;
                  if ((this.val$remote_host_port.split(":")).length > 2) {
                    bind_port = Integer.parseInt(this.val$remote_host_port.split(":")[0]);
                  } else {
                    bind_port = Integer.parseInt(SharedSessionReplicated.remote_host_ports.elementAt(0).toString().split(":")[1]);
                  } 
                  if (this.val$bind_ports.indexOf((new StringBuffer(String.valueOf(bind_port))).toString()) < 0) {
                    this.val$bind_ports.addElement((new StringBuffer(String.valueOf(bind_port))).toString());
                  } else {
                    return;
                  } 
                  if (System.getProperty("crushftp.sharedsession.ssl", "true").equals("true")) {
                    ss = ServerStatus.thisObj.common_code.getServerSocket(bind_port, System.getProperty("crushftp.sharedsession.bindip", "0.0.0.0"), "builtin", "crushftp", "crushftp", "", false, 1, true, false);
                    Common.configureSSLTLSSocket(ss);
                  } else {
                    ss = new ServerSocket(bind_port, 1000, InetAddress.getByName(System.getProperty("crushftp.sharedsession.bindip", "0.0.0.0")));
                  } 
                  int reconnects = 0;
                  String current_name = Thread.currentThread().getName();
                  String allowed_ips2 = ServerStatus.SG("replicated_server_ips");
                  while (true) {
                    Socket sock = ss.accept();
                    String incoming_ip = sock.getInetAddress().getHostAddress();
                    if (!allowed_ips2.equals("") && !Common.do_search(allowed_ips2, incoming_ip, false, 0) && allowed_ips2.indexOf(incoming_ip) < 0) {
                      System.out.println("IP " + sock.getInetAddress().getHostAddress() + " was from an untrusted host and was denied replication control. Allowed IPs: " + allowed_ips2);
                      sock.close();
                      continue;
                    } 
                    sock.setSoTimeout(10000);
                    try {
                      ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
                      long received = 0L;
                      while (true) {
                        Properties p = (Properties)ois.readObject();
                        if (p.getProperty("action").equalsIgnoreCase("CLOSE"))
                          break; 
                        p.put("source_socket", sock);
                        SharedSessionReplicated.receive(p);
                        received++;
                        Thread.currentThread().setName(String.valueOf(current_name) + " reconnects=" + reconnects + " received=" + received + " pending_user_sync=" + SharedSessionReplicated.pending_user_sync.size() + " pending_job_sync=" + SharedSessionReplicated.pending_job_sync.size() + " pendingResponses=" + SharedSessionReplicated.pendingResponses);
                      } 
                      ois.close();
                    } catch (IOException e) {
                      Log.log("SERVER", 0, e);
                    } 
                    sock.close();
                    reconnects++;
                  } 
                } catch (Exception e) {
                  Log.log("SERVER", 0, e);
                  return;
                } 
              }
            }"SharedSessionReplicatedReceiver:" + remote_host_ports.elementAt(i) + ":");
      } 
      Worker.startWorker(new Runnable() {
            public void run() {
              long lastSync = System.currentTimeMillis();
              while (true) {
                SharedSessionReplicated.flushNow();
                boolean empty = true;
                for (int x = 0; x < 100; x++) {
                  try {
                    Thread.sleep(10L);
                  } catch (Exception exception) {}
                  for (int xx = 0; xx < SharedSessionReplicated.remote_host_ports.size(); xx++) {
                    Vector v = (Vector)SharedSessionReplicated.send_queues.get(SharedSessionReplicated.remote_host_ports.elementAt(xx));
                    if (v.size() > 0)
                      empty = false; 
                  } 
                  if (!empty)
                    break; 
                } 
                try {
                  if (System.currentTimeMillis() - lastSync > 30000L && empty) {
                    synchronized (SharedSessionReplicated.pending_user_sync) {
                      Vector pending_user_sync2 = (Vector)SharedSessionReplicated.pending_user_sync.clone();
                      for (int i = 0; i < pending_user_sync2.size(); i++) {
                        Properties p = pending_user_sync2.elementAt(i);
                        if (System.currentTimeMillis() - Long.parseLong(p.getProperty("time")) > 40000L) {
                          SharedSessionReplicated.pending_user_sync.remove(p);
                          SharedSessionReplicated.send(p.getProperty("id"), p.getProperty("action"), p.get("key"), p.get("val"));
                        } 
                      } 
                      if (pending_user_sync2.size() != SharedSessionReplicated.pending_user_sync.size())
                        SharedSessionReplicated.flushPendingUserSync(); 
                      ServerStatus.thisObj.server_info.put("replicated_servers_pending_user_sync", (new StringBuffer(String.valueOf(SharedSessionReplicated.pending_user_sync.size()))).toString());
                    } 
                    synchronized (SharedSessionReplicated.pending_job_sync) {
                      Vector pending_job_sync2 = (Vector)SharedSessionReplicated.pending_job_sync.clone();
                      for (int i = 0; i < pending_job_sync2.size(); i++) {
                        Properties p = pending_job_sync2.elementAt(i);
                        if (System.currentTimeMillis() - Long.parseLong(p.getProperty("time")) > 40000L) {
                          SharedSessionReplicated.pending_job_sync.remove(p);
                          SharedSessionReplicated.send(p.getProperty("id"), p.getProperty("action"), p.get("key"), p.get("val"));
                        } 
                      } 
                      if (pending_job_sync2.size() != SharedSessionReplicated.pending_job_sync.size())
                        SharedSessionReplicated.flushPendingJobSync(); 
                      ServerStatus.thisObj.server_info.put("replicated_servers_pending_job_sync", (new StringBuffer(String.valueOf(SharedSessionReplicated.pending_job_sync.size()))).toString());
                    } 
                    Enumeration keys = SharedSessionReplicated.pendingResponses.keys();
                    while (keys.hasMoreElements()) {
                      String key = keys.nextElement().toString();
                      Properties val = (Properties)SharedSessionReplicated.pendingResponses.get(key);
                      if (System.currentTimeMillis() - Long.parseLong(val.getProperty("time", "0")) > 10000L) {
                        val.put("response_num", "-1");
                        SharedSessionReplicated.pendingResponses.remove(key);
                      } 
                    } 
                    ServerStatus.thisObj.server_info.put("replicated_servers_pendingResponses", (new StringBuffer(String.valueOf(SharedSessionReplicated.pendingResponses.size()))).toString());
                    lastSync = System.currentTimeMillis();
                  } 
                } catch (Exception exception) {}
              } 
            }
          }"SharedSessionReplicatedSender:");
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    if ((new File(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster_user_sync.xml")).exists()) {
      Vector pending_user_sync2 = (Vector)Common.readXMLObject(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster_user_sync.xml");
      if (pending_user_sync2 != null)
        for (int x = 0; x < pending_user_sync2.size(); x++) {
          Properties p = pending_user_sync2.elementAt(x);
          send(p.getProperty("id"), p.getProperty("action"), p.get("key"), p.get("val"));
        }  
      ServerStatus.thisObj.server_info.put("replicated_servers_pending_user_sync", (new StringBuffer(String.valueOf(pending_user_sync.size()))).toString());
    } 
    if ((new File(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster_job_sync.xml")).exists()) {
      Vector pending_job_sync2 = (Vector)Common.readXMLObject(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster_job_sync.xml");
      if (pending_job_sync2 != null)
        for (int x = 0; x < pending_job_sync2.size(); x++) {
          Properties p = pending_job_sync2.elementAt(x);
          send(p.getProperty("id"), p.getProperty("action"), p.get("key"), p.get("val"));
        }  
      ServerStatus.thisObj.server_info.put("replicated_servers_pending_job_sync", (new StringBuffer(String.valueOf(pending_job_sync.size()))).toString());
    } 
  }
  
  public static void flushWait() {}
  
  public static void flushNow() {
    boolean dofull = false;
    synchronized (send_lock) {
      for (int xx = 0; xx < remote_host_ports.size(); xx++) {
        lastActive = System.currentTimeMillis();
        Vector send_queue = (Vector)send_queues.get(remote_host_ports.elementAt(xx));
        try {
          if (factory == null && System.getProperty("crushftp.sharedsession.ssl", "true").equals("true"))
            factory = ServerStatus.thisObj.common_code.getSSLContext("builtin", null, "crushftp", "crushftp", "TLS", false, true).getSocketFactory(); 
          if (send_queue.size() > 0) {
            Socket sock = null;
            String remote_host_port = remote_host_ports.elementAt(xx).toString();
            if (System.getProperty("crushftp.sharedsession.ssl", "true").equals("true")) {
              if ((remote_host_port.split(":")).length > 2) {
                sock = factory.createSocket(remote_host_port.split(":")[1], Integer.parseInt(remote_host_port.split(":")[2]));
              } else {
                sock = factory.createSocket(remote_host_port.split(":")[0], Integer.parseInt(remote_host_port.split(":")[1]));
              } 
            } else {
              sock = new Socket(remote_host_port.split(":")[0], Integer.parseInt(remote_host_port.split(":")[1]));
            } 
            sock.setSoTimeout(10000);
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            while (send_queue.size() > 0) {
              Thread.currentThread().setName(String.valueOf(Thread.currentThread().getName().substring(0, Thread.currentThread().getName().lastIndexOf(":") + 1)) + send_queue.size());
              Properties properties = send_queue.elementAt(0);
              if (!properties.getProperty("action", "").equals("crushftp.session.update"))
                Log.log("SERVER", 2, "SharedSession:Send:" + properties.getProperty("id") + ":" + properties.getProperty("action") + ":" + properties.getProperty("key") + ":" + properties.getProperty("size", "0") + " bytes"); 
              oos.writeObject(properties);
              oos.flush();
              send_queue.remove(0);
              sync_delay = System.currentTimeMillis() - Long.parseLong(properties.getProperty("queued"));
              lastActive = System.currentTimeMillis();
            } 
            Properties p = new Properties();
            p.put("action", "CLOSE");
            oos.writeObject(p);
            oos.flush();
            oos.close();
            sock.close();
            offline = false;
            lastActive = System.currentTimeMillis();
            ServerStatus.thisObj.server_info.put("replicated_servers_lastActive", (new StringBuffer(String.valueOf(lastActive))).toString());
            ServerStatus.thisObj.server_info.put("replicated_servers_sent_" + xx, (new StringBuffer(String.valueOf(Integer.parseInt(ServerStatus.thisObj.server_info.getProperty("replicated_servers_" + xx, "0")) + 1))).toString());
          } 
        } catch (SocketException e) {
          lastActive = 0L;
          send_queue.removeAllElements();
          dofull = true;
        } catch (Exception e) {
          lastActive = 0L;
          Log.log("SERVER", 0, e);
          dofull = true;
        } 
        ServerStatus.thisObj.server_info.put("replicated_servers_queue_" + xx, (new StringBuffer(String.valueOf(send_queue.size()))).toString());
      } 
    } 
    if (dofull)
      doFullSync(); 
  }
  
  public static void doFullSync() {
    if (!offline) {
      offline = true;
      for (int xx = 0; xx < remote_host_ports.size(); xx++) {
        Vector send_queue = (Vector)send_queues.get(remote_host_ports.elementAt(xx));
        send_queue.removeAllElements();
      } 
      for (int x = 0; x < replicatedItems.size(); x++) {
        String id = replicatedItems.elementAt(x).toString();
        Properties cache = SharedSession.getCache(id);
        Enumeration keys = cache.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          send(id, "put", key, cache.get(key));
        } 
      } 
    } 
  }
  
  public static void receive(Properties p) {
    long ms = System.currentTimeMillis() - Long.parseLong(p.getProperty("queued"));
    if (!p.getProperty("action", "").equals("crushftp.session.update"))
      Log.log("SERVER", 2, "SharedSession:Receive:" + p.getProperty("id") + ":" + p.getProperty("action") + ":" + p.getProperty("key") + ":" + p.getProperty("size", "0") + " bytes:" + ms + "ms"); 
    if (p.getProperty("action").equalsIgnoreCase("RESPONSE")) {
      if (pendingResponses.containsKey(p.getProperty("id"))) {
        Properties val = (Properties)pendingResponses.remove(p.getProperty("id"));
        int response_num = Integer.parseInt(val.getProperty("response_num", "0"));
        response_num++;
        val.putAll(p);
        val.put("response_num", (new StringBuffer(String.valueOf(response_num))).toString());
        ServerStatus.thisObj.server_info.put("replicated_servers_pendingResponses", (new StringBuffer(String.valueOf(pendingResponses.size()))).toString());
      } else {
        int x;
        for (x = pending_user_sync.size() - 1; x >= 0; x--) {
          Properties pp = pending_user_sync.elementAt(x);
          if (p.getProperty("id").equals(pp.getProperty("id"))) {
            pending_user_sync.remove(x);
            flushPendingUserSync();
          } 
        } 
        ServerStatus.thisObj.server_info.put("replicated_servers_pending_user_sync", (new StringBuffer(String.valueOf(pending_user_sync.size()))).toString());
        for (x = pending_job_sync.size() - 1; x >= 0; x--) {
          Properties pp = pending_job_sync.elementAt(x);
          if (p.getProperty("id").equals(pp.getProperty("id"))) {
            pending_job_sync.remove(x);
            flushPendingJobSync();
          } 
        } 
        ServerStatus.thisObj.server_info.put("replicated_servers_pending_job_sync", (new StringBuffer(String.valueOf(pending_job_sync.size()))).toString());
      } 
    } else if (p.getProperty("action", "").equals("WRITE_PREFS")) {
      Properties prefs = (Properties)p.remove("val");
      try {
        Common.write_server_settings(prefs, p.getProperty("key"));
        DMZServer.last_prefs_time.put(p.getProperty("key"), (new StringBuffer(String.valueOf((new File(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs_" + p.getProperty("key") + ".XML")).lastModified()))).toString());
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.AdminControls.setServerItem")) {
      try {
        Properties pp = (Properties)p.get("val");
        AdminControls.setServerItem((Properties)pp.remove("request"), (String)pp.remove("site"), false);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.AdminControls.renameJob")) {
      try {
        Properties pp = (Properties)p.get("val");
        AdminControls.renameJob((Properties)pp.remove("request"), (String)pp.remove("site"), false);
        send(p.getProperty("id"), "RESPONSE", "", null);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.AdminControls.removeJob")) {
      try {
        Properties pp = (Properties)p.get("val");
        AdminControls.removeJob((Properties)pp.remove("request"), (String)pp.remove("site"), false);
        send(p.getProperty("id"), "RESPONSE", "", null);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.AdminControls.addJob")) {
      try {
        Properties pp = (Properties)p.get("val");
        AdminControls.addJob((Properties)pp.remove("request"), (String)pp.remove("site"), false);
        send(p.getProperty("id"), "RESPONSE", "", null);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.AdminControls.newFolder")) {
      try {
        Properties pp = (Properties)p.get("val");
        AdminControls.newFolder((Properties)pp.remove("request"), (String)pp.remove("site"), false);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.AdminControls.renameItem")) {
      try {
        Properties pp = (Properties)p.get("val");
        AdminControls.renameItem((Properties)pp.remove("request"), (String)pp.remove("site"), false);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.AdminControls.duplicateItem")) {
      try {
        Properties pp = (Properties)p.get("val");
        AdminControls.duplicateItem((Properties)pp.remove("request"), (String)pp.remove("site"), false);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.AdminControls.deleteItem")) {
      try {
        Properties pp = (Properties)p.get("val");
        AdminControls.deleteItem((Properties)pp.remove("request"), (String)pp.remove("site"), false);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.s3CrushClient.writeFs")) {
      try {
        Properties pp = (Properties)p.get("val");
        S3CrushClient.writeFs(System.getProperty("crushftp.s3_root", "./s3/"), pp.getProperty("bucketName0"), null, pp.getProperty("path"), (Properties)pp.get("data"));
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.JobScheduler.jobRunning")) {
      try {
        String id = p.getProperty("id");
        Properties val = (Properties)p.get("val");
        boolean ok = JobScheduler.jobRunning(val.getProperty("scheduleName"));
        Properties response = new Properties();
        response.put("scheduleName", val.getProperty("scheduleName"));
        response.put("running_" + Common.makeBoundary(), (new StringBuffer(String.valueOf(ok))).toString());
        send(id, "RESPONSE", "", response);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.JobScheduler.runJob")) {
      try {
        Properties p_f2 = p;
        Properties info = (Properties)p.remove("val");
        Worker.startWorker(new Runnable(info, p_f2) {
              private final Properties val$info;
              
              private final Properties val$p_f2;
              
              public void run() {
                byte[] b = (byte[])null;
                Properties event = (Properties)this.val$info.remove("data");
                Log.log("SERVER", 0, "crushftp.JobScheduler.runJob:" + this.val$info.getProperty("use_dmz", "") + ":" + this.val$p_f2.getProperty("source_socket", "") + ":" + event.getProperty("scheduleName"));
                if (this.val$info.getProperty("use_dmz", "").equalsIgnoreCase("All DMZ Recursive")) {
                  Vector items = (Vector)this.val$info.remove("items");
                  Enumeration keys = DMZServer.dmzInstances.keys();
                  while (keys.hasMoreElements()) {
                    Vector log = new Vector();
                    try {
                      String the_dmz = keys.nextElement().toString();
                      Log.log("SERVER", 0, "crushftp.JobScheduler.runJob:" + this.val$info.getProperty("use_dmz", "") + ":" + this.val$p_f2.getProperty("source_socket", "") + ":" + event.getProperty("scheduleName") + ":" + the_dmz);
                      items = SharedSessionReplicated.runOnDmz(event, the_dmz, items, log);
                      this.val$info.put("items", items);
                      Vector log2 = (Vector)log.clone();
                      String log_str = "";
                      while (log2.size() > 0)
                        log_str = String.valueOf(log_str) + log2.remove(0) + "\r\n"; 
                      Log.log("SERVER", 1, log_str);
                      b = log_str.getBytes("UTF8");
                    } catch (Exception e) {
                      Log.log("SERVER", 0, e);
                    } 
                  } 
                } else {
                  Vector items = (Vector)this.val$info.remove("items");
                  event.put("event_plugin_list", "CrushTask (User Defined)");
                  event.put("name", event.getProperty("scheduleName"));
                  Properties info = ServerStatus.thisObj.events6.doEventPlugin(null, event, null, items);
                  this.val$p_f2.put("data", info);
                  try {
                    RandomAccessFile raf = new RandomAccessFile(info.getProperty("log_file"), "r");
                    b = new byte[(int)raf.length()];
                    raf.readFully(b);
                    raf.close();
                  } catch (Throwable e) {
                    Log.log("SERVER", 0, e);
                    b = new byte[0];
                  } 
                } 
                String uid = Common.makeBoundary();
                this.val$p_f2.put("log", new String(b));
                this.val$p_f2.put("type", "RESPONSE");
                this.val$p_f2.put("log_" + uid, this.val$p_f2.remove("log"));
                this.val$p_f2.put("data_" + uid, this.val$p_f2.remove("data"));
                SharedSessionReplicated.send(this.val$p_f2.getProperty("id"), "RESPONSE", "", this.val$p_f2);
              }
            });
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").startsWith("crushftp.handlers.")) {
      String id = p.getProperty("id");
      try {
        String action = p.getProperty("action");
        p = (Properties)p.remove("val");
        Log.log("SERVER", 2, (String)p);
        if (action.endsWith(".writeGroups")) {
          Log.log("SERVER", 0, String.valueOf(action) + ":" + p.getProperty("serverGroup"));
          UserTools.writeGroups(p.getProperty("serverGroup"), (Properties)p.get("groups"), false);
        } else if (action.endsWith(".writeUser")) {
          Log.log("SERVER", 0, String.valueOf(action) + ":" + p.getProperty("serverGroup") + ":" + p.getProperty("username") + ":" + p.getProperty("backup", ""));
          UserTools.writeUser(p.getProperty("serverGroup"), p.getProperty("username"), (Properties)p.get("user"), false, p.getProperty("backup", "").equals("true"));
        } else if (action.endsWith(".writeInheritance")) {
          Log.log("SERVER", 0, String.valueOf(action) + ":" + p.getProperty("serverGroup"));
          UserTools.writeInheritance(p.getProperty("serverGroup"), (Properties)p.get("inheritance"), false);
        } else if (action.endsWith(".deleteUser")) {
          Log.log("SERVER", 0, String.valueOf(action) + ":" + p.getProperty("serverGroup") + ":" + p.getProperty("username"));
          UserTools.deleteUser(p.getProperty("serverGroup"), p.getProperty("username"), false);
        } else if (action.endsWith(".addFolder")) {
          Log.log("SERVER", 0, String.valueOf(action) + ":" + p.getProperty("serverGroup") + ":" + p.getProperty("username") + ":" + p.getProperty("path") + ":" + p.getProperty("name"));
          UserTools.addFolder(p.getProperty("serverGroup"), p.getProperty("username"), p.getProperty("path"), p.getProperty("name"), false);
        } else if (action.endsWith(".addItem")) {
          try {
            Log.log("SERVER", 0, String.valueOf(action) + ":" + p.getProperty("serverGroup") + ":" + p.getProperty("username") + ":" + p.getProperty("path") + ":" + p.getProperty("name") + ":" + (new VRL(p.getProperty("url"))).safe());
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
          } 
          UserTools.addItem(p.getProperty("serverGroup"), p.getProperty("username"), p.getProperty("path"), p.getProperty("name"), p.getProperty("url"), p.getProperty("type"), (Properties)p.get("moreItems"), p.getProperty("encrypted").equals("true"), p.getProperty("encrypted_class"), false);
        } else if (action.endsWith(".writeVFS")) {
          Log.log("SERVER", 0, String.valueOf(action) + ":" + p.getProperty("serverGroup") + ":" + p.getProperty("username"));
          UserTools.writeVFS(p.getProperty("serverGroup"), p.getProperty("username"), (Properties)p.get("virtual"), false);
        } 
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
      send(id, "RESPONSE", "", null);
    } else if (p.getProperty("action", "").equals("crushftp.session.update")) {
      try {
        SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(p.getProperty("id"));
        if (thisSession != null)
          thisSession.put(p.getProperty("key"), p.get("val"), false); 
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.server.ServerSessionAjax.doFileAbortBlock")) {
      try {
        Properties p_f2 = p;
        Worker.startWorker(new Runnable(p_f2) {
              private final Properties val$p_f2;
              
              public void run() {
                Properties pp = (Properties)this.val$p_f2.get("val");
                SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").get(pp.remove("CrushAuth"));
                try {
                  if (thisSession != null)
                    thisSession.doFileAbortBlock((String)pp.remove("the_command_data"), false); 
                } catch (Exception e) {
                  Log.log("SERVER", 0, e);
                } 
              }
            });
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else if (p.getProperty("action", "").equals("crushftp.session.remove_user")) {
      try {
        SessionCrush thisSession = (SessionCrush)SharedSession.find("crushftp.sessions").remove(p.getProperty("id"), false);
        if (thisSession != null) {
          ServerStatus.thisObj.remove_user(thisSession.user_info, true);
        } else {
          synchronized (ServerStatus.thisObj) {
            for (int loops = 0; loops < 30; loops++) {
              try {
                for (int x = ServerStatus.siVG("user_list").size() - 1; x >= 0; x--) {
                  Properties user_info = ServerStatus.siVG("user_list").elementAt(x);
                  if (user_info.getProperty("CrushAuth", "").equals(p.getProperty("id")))
                    ServerStatus.thisObj.remove_user(user_info); 
                } 
                break;
              } catch (Exception exception) {
                Thread.sleep(100L);
              } 
            } 
          } 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else {
      SharedSession ss = SharedSession.find(p.getProperty("id"));
      if (p.getProperty("action").equals("put")) {
        ss.put(p.get("key"), p.get("val"), false);
      } else if (p.getProperty("action").equals("remove")) {
        ss.remove(p.get("key"), false);
      } else if (p.getProperty("action").equals("anyPassToken")) {
        UserTools.addAnyPassToken(p.getProperty("val"));
      } 
    } 
  }
  
  public static void send(String id, String action, Object key, Object val) {
    try {
      Properties p = new Properties();
      p.put("id", id);
      p.put("action", action);
      p.put("key", key);
      if (val != null) {
        p.put("val", val);
        if (val instanceof Properties) {
          Properties val2 = (Properties)val;
          if (val2.getProperty("need_response", "").equals("true")) {
            val2.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
            pendingResponses.put(id, val2);
            ServerStatus.thisObj.server_info.put("replicated_servers_pendingResponses", (new StringBuffer(String.valueOf(pendingResponses.size()))).toString());
          } 
        } 
      } 
      p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      byte[] b = (byte[])null;
      synchronized (SharedSession.sessionLock) {
        b = Common.CLONE1(p);
      } 
      Properties p2 = (Properties)Common.CLONE2(b);
      p2.put("size", (new StringBuffer(String.valueOf(b.length))).toString());
      if (action.startsWith("crushftp.handlers.")) {
        if (!ServerStatus.BG("replicated_users_sync"))
          return; 
        synchronized (pending_user_sync) {
          pending_user_sync.addElement(p2);
          flushPendingUserSync();
          ServerStatus.thisObj.server_info.put("replicated_servers_pending_user_sync", (new StringBuffer(String.valueOf(pending_user_sync.size()))).toString());
        } 
      } 
      if (action.equals("crushftp.AdminControls.renameJob") || action.equals("crushftp.AdminControls.removeJob") || action.equals("crushftp.AdminControls.addJob")) {
        if (!ServerStatus.BG("replicate_jobs_sync"))
          return; 
        synchronized (pending_job_sync) {
          pending_job_sync.addElement(p2);
          flushPendingJobSync();
          ServerStatus.thisObj.server_info.put("replicated_servers_pending_job_sync", (new StringBuffer(String.valueOf(pending_job_sync.size()))).toString());
        } 
      } 
      for (int xx = 0; xx < remote_host_ports.size(); xx++) {
        Vector send_queue = (Vector)send_queues.get(remote_host_ports.elementAt(xx));
        p2.put("queued", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        send_queue.addElement(p2);
        ServerStatus.thisObj.server_info.put("replicated_servers_queue_" + xx, (new StringBuffer(String.valueOf(send_queue.size()))).toString());
      } 
      if (sync_delay > 3000L && System.currentTimeMillis() - lastActive < 10000L)
        flushNow(); 
    } catch (Exception e) {
      e.printStackTrace();
      Log.log("SERVER", 0, e);
    } 
  }
  
  public static Vector runOnDmz(Properties job_tmp, String use_dmz, Vector tempItems, Vector log) throws Exception {
    String id = Common.makeBoundary(11);
    Log.log("SERVER", 0, "crushftp.JobScheduler.runJob:dmz_name=" + use_dmz + ":RUN:JOB:" + id);
    Properties request = new Properties();
    request.put("data", job_tmp);
    request.put("items", tempItems);
    DMZServer.sendCommand(use_dmz, request, "RUN:JOB", id);
    Properties p = DMZServer.getResponse(id, 600);
    Object object = p.remove("log");
    BufferedReader br = new BufferedReader(new StringReader((String)object));
    String data = "";
    while ((data = br.readLine()) != null)
      log.addElement(String.valueOf(use_dmz) + ":" + data); 
    Properties result_info = (Properties)p.get("data");
    tempItems = (Vector)result_info.remove("newItems");
    Log.log("SERVER", 0, "crushftp.JobScheduler.runJob:dmz_name=" + use_dmz + ":RUN:JOB:" + id + ":complete");
    if (result_info.containsKey("errors"))
      throw new Exception(result_info.get("errors")); 
    return tempItems;
  }
  
  public static void flushPendingUserSync() {
    try {
      if (pending_user_sync.size() == 0)
        (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster_user_sync.xml")).delete(); 
      Common.writeXMLObject(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster_user_sync.xml", pending_user_sync, "pending_user_sync");
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
  }
  
  public static void flushPendingJobSync() {
    try {
      if (pending_job_sync.size() == 0) {
        (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster_job_sync.xml")).delete();
      } else {
        Common.writeXMLObject(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster_job_sync.xml", pending_job_sync, "pending_job_sync");
      } 
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
  }
}
