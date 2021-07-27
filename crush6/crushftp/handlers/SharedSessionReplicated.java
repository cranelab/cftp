package crushftp.handlers;

import crushftp.server.ServerStatus;
import crushftp.server.Worker;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.net.ssl.SSLSocketFactory;

public class SharedSessionReplicated {
  public static Vector send_queue = new Vector();
  
  public static String remote_host_port = "";
  
  static SSLSocketFactory factory = null;
  
  public static Object send_lock = new Object();
  
  static boolean offline = false;
  
  public static Vector replicatedItems = new Vector();
  
  public static void init() {
    try {
      if (ServerStatus.siIG("enterprise_level") <= 0)
        throw new Exception("SharedSession only valid for Enterprise licenses."); 
      replicatedItems.addElement("crushftp.usernames");
      replicatedItems.addElement("crushftp.usernames.activity");
      replicatedItems.addElement("crushftp.sessions");
      if ((new File(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster.xml")).exists()) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Common.streamCopier(new FileInputStream(new File(String.valueOf(System.getProperty("crushftp.prefs")) + "cluster.xml")), baos, false);
        String xml = new String(baos.toByteArray(), "UTF8");
        xml = xml.substring(xml.indexOf("initial_hosts") + "initial_hosts".length());
        xml = xml.substring(0, xml.indexOf("]"));
        xml = xml.substring(xml.indexOf("\"") + 1);
        remote_host_port = xml.replace('[', ':').trim();
      } else {
        remote_host_port = ServerStatus.SG("replicate_session_host_port");
      } 
      if (remote_host_port.indexOf(",") >= 0) {
        String s = remote_host_port;
        for (int x = 0; x < (s.split(",")).length; x++) {
          if (s.split(",")[x].toUpperCase().indexOf(InetAddress.getLocalHost().getHostName().toUpperCase()) < 0)
            remote_host_port = s.split(",")[x].trim(); 
        } 
      } 
      Worker.startWorker(new Runnable() {
            public void run() {
              try {
                ServerSocket ss = ServerStatus.thisObj.common_code.getServerSocket(Integer.parseInt(SharedSessionReplicated.remote_host_port.split(":")[1]), "0.0.0.0", "builtin", "crushftp", "crushftp", "", false, 1);
                while (true) {
                  Socket sock = ss.accept();
                  sock.setSoTimeout(10000);
                  try {
                    ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
                    while (true) {
                      Properties p = (Properties)ois.readObject();
                      if (p.getProperty("action").equalsIgnoreCase("CLOSE"))
                        break; 
                      SharedSessionReplicated.receive(p);
                    } 
                    ois.close();
                  } catch (IOException e) {
                    Log.log("SERVER", 0, e);
                  } 
                  sock.close();
                } 
              } catch (Exception e) {
                Log.log("SERVER", 0, e);
                return;
              } 
            }
          }"SharedSessionReplicatedReceiver");
      Worker.startWorker(new Runnable() {
            public void run() {
              while (true) {
                SharedSessionReplicated.flushNow();
                for (int x = 0; SharedSessionReplicated.send_queue.size() == 0 && x < 100; x++) {
                  try {
                    Thread.sleep(10L);
                  } catch (Exception exception) {}
                } 
              } 
            }
          }"SharedSessionReplicatedSender");
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
  }
  
  public static void flushWait() {}
  
  public static void flushNow() {
    synchronized (send_lock) {
      try {
        if (factory == null)
          factory = ServerStatus.thisObj.common_code.getSSLContext("builtin", null, "crushftp", "crushftp", "TLS", false, true).getSocketFactory(); 
        if (send_queue.size() > 0) {
          Socket sock = factory.createSocket(remote_host_port.split(":")[0], Integer.parseInt(remote_host_port.split(":")[1]));
          ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
          while (send_queue.size() > 0) {
            Properties properties = send_queue.elementAt(0);
            Log.log("SERVER", 2, "SharedSession:Send:" + properties.getProperty("id") + ":" + properties.getProperty("action") + ":" + properties.getProperty("key") + ":" + properties.getProperty("size", "0"));
            oos.writeObject(properties);
            oos.flush();
            send_queue.remove(0);
          } 
          Properties p = new Properties();
          p.put("action", "CLOSE");
          oos.writeObject(p);
          oos.flush();
          oos.close();
          sock.close();
          offline = false;
        } 
      } catch (SocketException e) {
        send_queue.removeAllElements();
        doFullSync();
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        doFullSync();
      } 
    } 
  }
  
  public static void doFullSync() {
    if (!offline) {
      offline = true;
      send_queue.removeAllElements();
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
    Log.log("SERVER", 2, "SharedSession:Receive:" + p.getProperty("id") + ":" + p.getProperty("action") + ":" + p.getProperty("key") + ":" + p.getProperty("size", "0"));
    if (p.getProperty("action", "").equals("WRITE_PREFS")) {
      Properties prefs = (Properties)p.remove("val");
      try {
        Common.write_server_settings(prefs, p.getProperty("key"));
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
      if (val != null)
        p.put("val", val); 
      p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      byte[] b = Common.CLONE1(p);
      Properties p2 = (Properties)Common.CLONE2(b);
      p2.put("size", (new StringBuffer(String.valueOf(b.length))).toString());
      send_queue.addElement(p2);
    } catch (Exception e) {
      e.printStackTrace();
      Log.log("SERVER", 0, e);
    } 
  }
}
