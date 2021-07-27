package crushftp.handlers;

import com.crushftp.client.File_S;
import com.crushftp.client.Worker;
import crushftp.server.QuickConnect;
import crushftp.server.ServerStatus;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class SharedSession {
  public static Object sessionLock = new Object();
  
  public static Object sessionFindLock = new Object();
  
  private static Properties thisObj = null;
  
  String id = "";
  
  private static void init() {
    if (thisObj == null)
      synchronized (sessionLock) {
        if (thisObj == null) {
          ObjectInputStream ois = null;
          try {
            if ((new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/lib/sessionCache.xml")).exists()) {
              Common.recurseDelete(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/cache/", false);
              (new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/lib/sessionCache.xml")).delete();
              (new File_S(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/lib/ehcache-core-2.5.0.jar")).delete();
            } 
            if ((new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj")).exists()) {
              ois = new ObjectInputStream(new FileInputStream(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj")));
              thisObj = (Properties)ois.readObject();
              thisObj.remove("crushftp.usernames.activity");
              SharedSession user_sessions = find("crushftp.sessions");
              Enumeration keys = user_sessions.keys();
              if (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                Object o = user_sessions.get(key);
                if (o instanceof Properties) {
                  user_sessions.remove(key);
                } else if (o instanceof SessionCrush) {
                  if (((SessionCrush)o).getProperty("last_activity") == null)
                    ((SessionCrush)o).active(); 
                } 
              } 
              SharedSession recent_users = find("recent_user_list");
              if (recent_users.get("recent_user_list") != null) {
                ServerStatus.siVG("recent_user_list").addAll((Vector)recent_users.get("recent_user_list"));
                synchronized (QuickConnect.syncUserNumbers) {
                  int maxNum = 0;
                  for (int x = 0; x < ServerStatus.siVG("recent_user_list").size(); x++) {
                    Properties p = ServerStatus.siVG("recent_user_list").elementAt(x);
                    int userNum = Integer.parseInt(p.getProperty("user_number", "0"));
                    if (userNum > maxNum)
                      maxNum = userNum; 
                  } 
                  int saved_maxNum = Integer.parseInt(recent_users.getProperty("user_login_num", "0"));
                  if (saved_maxNum > maxNum)
                    maxNum = saved_maxNum; 
                  maxNum++;
                  ServerStatus.put_in("user_login_num", (new StringBuffer(String.valueOf(maxNum))).toString());
                } 
              } 
              ois.close();
              ois = null;
            } 
            if ((new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes.obj")).exists()) {
              ois = new ObjectInputStream(new FileInputStream(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes.obj")));
              Properties md4_hashes = (Properties)ois.readObject();
              ServerStatus.thisObj.server_info.put("md4_hashes", md4_hashes);
              ois.close();
              ois = null;
            } 
          } catch (Throwable e) {
            Log.log("SERVER", 0, e);
            try {
              if (ois != null)
                ois.close(); 
            } catch (Exception exception) {}
            (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj")).delete();
          } 
          if (thisObj == null)
            thisObj = new Properties(); 
          if (!ServerStatus.SG("replicate_session_host_port").equals(""))
            try {
              SharedSessionReplicated.init();
            } catch (Exception e) {
              Log.log("SERVER", 0, e);
            }  
        } 
      }  
  }
  
  static boolean shutting_down = false;
  
  public static void shutdown() {
    if (ServerStatus.BG("allow_session_caching_on_exit")) {
      while (true) {
        if (ServerStatus.siVG("user_list").size() <= 0)
          break; 
        Properties user_info = ServerStatus.siVG("user_list").elementAt(0);
        SessionCrush thisSession = (SessionCrush)user_info.get("session");
        if (thisSession != null) {
          thisSession.do_kill(null);
        } else {
          ServerStatus.thisObj.remove_user(user_info);
        } 
        ServerStatus.siVG("user_list").remove(user_info);
      } 
      SharedSession recent_users = find("recent_user_list");
      recent_users.put("recent_user_list", ServerStatus.siVG("recent_user_list"));
      recent_users.put("user_login_num", ServerStatus.siSG("user_login_num"));
    } 
    shutting_down = true;
    flush();
  }
  
  public static void flush() {
    Thread currThread = Thread.currentThread();
    StringBuffer status = new StringBuffer();
    try {
      (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj")).delete();
      ObjectOutputStream oos1 = null;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      if (ServerStatus.BG("allow_session_caching_memory")) {
        oos1 = new ObjectOutputStream(baos);
      } else {
        oos1 = new ObjectOutputStream(new FileOutputStream(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj")));
      } 
      ObjectOutputStream oos2 = oos1;
      Runnable r = new Runnable(status, currThread, oos2) {
          private final StringBuffer val$status;
          
          private final Thread val$currThread;
          
          private final ObjectOutputStream val$oos2;
          
          public void run() {
            for (int x = 0; x < 120 && this.val$status.length() == 0; x++) {
              try {
                Thread.sleep(1000L);
              } catch (Exception exception) {}
            } 
            if (this.val$status.length() == 0) {
              Log.log("SERVER", 0, "TIMEOUT waiting for sessions flush...");
              this.val$currThread.interrupt();
            } 
            try {
              this.val$oos2.close();
            } catch (Exception exception) {}
          }
        };
      Worker.startWorker(r);
      find("running_tasks").remove("running_tasks");
      synchronized (sessionLock) {
        synchronized (sessionFindLock) {
          try {
            oos2.writeObject(thisObj);
            oos2.flush();
            oos2.close();
            if (!ServerStatus.BG("allow_session_caching_memory")) {
              (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj")).delete();
              (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj"));
            } 
          } finally {
            try {
              oos2.close();
            } catch (Exception exception) {}
          } 
        } 
      } 
      if (ServerStatus.BG("allow_session_caching_memory")) {
        r = new Runnable(baos) {
            private final ByteArrayOutputStream val$baos;
            
            public void run() {
              try {
                FileOutputStream out = new FileOutputStream(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj"));
                out.write(this.val$baos.toByteArray());
                out.close();
                (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj")).delete();
                (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj"));
              } catch (Exception exception) {}
            }
          };
        if (shutting_down) {
          r.run();
        } else {
          Worker.startWorker(r);
        } 
      } 
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    status.append("done");
  }
  
  private static boolean isShared(String key) {
    return (SharedSessionReplicated.replicatedItems.indexOf(key.toString()) >= 0);
  }
  
  public static SharedSession find(String id) {
    init();
    synchronized (sessionFindLock) {
      if (isShared(id)) {
        if (!thisObj.containsKey(id))
          thisObj.put(id, new Properties()); 
      } else if (!thisObj.containsKey(id)) {
        thisObj.put(id, new Properties());
      } 
      return new SharedSession(id);
    } 
  }
  
  private SharedSession(String id) {
    this.id = id;
  }
  
  public void put(Object key, Object val) {
    put(key, val, true);
  }
  
  public void put(Object key, Object val, boolean replicate) {
    if (replicate && isShared(this.id) && ServerStatus.BG("replicate_sessions"))
      SharedSessionReplicated.send(this.id, "put", key, val); 
    Properties cache = (Properties)thisObj.get(this.id);
    if (val != null)
      cache.put(key, val); 
  }
  
  public static Properties getCache(String id) {
    return (Properties)thisObj.get(id);
  }
  
  public Object get(Object key) {
    Properties cache = (Properties)thisObj.get(this.id);
    return cache.get(key);
  }
  
  public String getProperty(String key, String val) {
    Properties cache = (Properties)thisObj.get(this.id);
    return cache.getProperty(key, val);
  }
  
  public String getProperty(String key) {
    Properties cache = (Properties)thisObj.get(this.id);
    return cache.getProperty(key);
  }
  
  public Object remove(Object key) {
    return remove(key, true);
  }
  
  public Object remove(Object key, boolean replicate) {
    if (replicate && isShared(this.id) && ServerStatus.BG("replicate_sessions"))
      SharedSessionReplicated.send(this.id, "remove", key, null); 
    Properties cache = (Properties)thisObj.get(this.id);
    return cache.remove(key);
  }
  
  public boolean containsKey(String key) {
    Properties cache = (Properties)thisObj.get(this.id);
    return cache.containsKey(key);
  }
  
  public Enumeration keys() {
    Properties cache = (Properties)thisObj.get(this.id);
    return cache.keys();
  }
}
