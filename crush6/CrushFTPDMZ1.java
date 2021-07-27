import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.AdminControls;
import crushftp.server.ServerStatus;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.Vector;

public class CrushFTPDMZ1 {
  public static Vector queue = new Vector();
  
  public static Properties dmzResponses = new Properties();
  
  boolean started = false;
  
  Common common_code = new Common();
  
  public CrushFTPDMZ1(String[] args) {
    Common.System2.put("crushftp.dmz.queue", queue);
    try {
      ServerSocket ss = this.common_code.getServerSocket(Integer.parseInt(args[1]), null, "builtin", "crushftp", "crushftp", "", false, 10);
      while (true) {
        Socket sock = ss.accept();
        Runnable r = new Runnable(this, sock) {
            final CrushFTPDMZ1 this$0;
            
            private final Socket val$sock;
            
            public void run() {
              try {
                ObjectInputStream ois = new ObjectInputStream(this.val$sock.getInputStream());
                String ip = this.val$sock.getInetAddress().getHostAddress();
                boolean write = false;
                while (!write) {
                  Properties p = (Properties)ois.readObject();
                  if (CrushFTPDMZ1.dmzResponses.containsKey(p.getProperty("id", ""))) {
                    Properties response = (Properties)CrushFTPDMZ1.dmzResponses.remove(p.getProperty("id"));
                    response.putAll(p);
                    if (p.containsKey("data"))
                      response.putAll((Properties)p.get("data")); 
                    p = response;
                    p.put("status", "done");
                  } 
                  Log.log("DMZ", 2, "READ:" + ip + ":" + p.getProperty("type"));
                  if (!p.getProperty("type").equalsIgnoreCase("PUT:OUTPUT")) {
                    if (p.getProperty("type").equalsIgnoreCase("PUT:INPUT")) {
                      write = true;
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("PUT:SERVER_SETTINGS")) {
                      synchronized (CrushFTPDMZ1.queue) {
                        if (!this.this$0.started) {
                          try {
                          
                          } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(0);
                          } 
                          this.this$0.started = true;
                        } 
                      } 
                      p.put("data", new Properties());
                      p.put("type", "PUT:DMZ_STARTED");
                      CrushFTPDMZ1.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("PUT:ANY_PASS_TOKEN")) {
                      Properties tokenProp = (Properties)p.get("data");
                      Common.System2.put("crushftp.proxy.anyPassToken", tokenProp.getProperty("crushftp.proxy.anyPassToken"));
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("GET:SERVER_SETTINGS")) {
                      p.put("data", ServerStatus.server_settings);
                      p.put("type", "RESPONSE");
                      CrushFTPDMZ1.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("GET:SERVER_INFO")) {
                      Properties si = new Properties();
                      if (this.this$0.started)
                        si = (Properties)ServerStatus.thisObj.server_info.clone(); 
                      si.remove("plugins");
                      Vector user_list = (Vector)((Vector)si.get("user_list")).clone();
                      si.put("user_list", user_list);
                      for (int x = 0; x < user_list.size(); x++) {
                        Properties user_info = (Properties)((Properties)user_list.elementAt(x)).clone();
                        user_list.setElementAt(user_info, x);
                        user_info.remove("session_commands");
                        user_info.remove("session");
                      } 
                      p.put("data", si);
                      p.put("type", "RESPONSE");
                      CrushFTPDMZ1.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("RUN:INSTANCE_ACTION")) {
                      p.put("data", AdminControls.runInstanceAction((Properties)p.get("data")));
                      p.put("type", "RESPONSE");
                      CrushFTPDMZ1.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("GET:PING")) {
                      p.put("data", new Properties());
                      p.put("type", "PUT:PONG");
                      CrushFTPDMZ1.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("PUT:SOCKET")) {
                      p.put("socket", this.val$sock);
                      return;
                    } 
                  } 
                } 
                if (write) {
                  ObjectOutputStream oos = new ObjectOutputStream(this.val$sock.getOutputStream());
                  while (write) {
                    synchronized (CrushFTPDMZ1.queue) {
                      while (CrushFTPDMZ1.queue.size() > 0) {
                        Properties p = CrushFTPDMZ1.queue.elementAt(0);
                        if (p.getProperty("need_response", "false").equalsIgnoreCase("true")) {
                          p.put("status", "waiting");
                          CrushFTPDMZ1.dmzResponses.put(p.getProperty("id"), p);
                        } 
                        oos.reset();
                        try {
                          oos.writeObject(p);
                        } catch (Exception e) {
                          System.out.println(p.getProperty("type"));
                          System.out.println(p);
                          throw e;
                        } 
                        oos.flush();
                        Log.log("DMZ", 2, "WROTE:" + ip + ":" + p.getProperty("type"));
                        CrushFTPDMZ1.queue.removeElementAt(0);
                      } 
                    } 
                    Thread.sleep(100L);
                  } 
                } 
              } catch (Exception e) {
                e.printStackTrace();
              } 
              try {
                this.val$sock.close();
              } catch (Exception exception) {}
            }
          };
        if (args.length < 3 || args[2].indexOf(sock.getInetAddress().getHostAddress()) >= 0) {
          Thread t = new Thread(r);
          t.setName("DMZ Daemon:" + sock.getInetAddress().getHostAddress());
          t.start();
          continue;
        } 
        String okIps = "all";
        if (args.length >= 3)
          okIps = args[2]; 
        System.out.println("IP " + sock.getInetAddress().getHostAddress() + " was from an untrusted host and was denied DMZ server contorl. Allowed IPs: " + okIps);
        sock.close();
      } 
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
      return;
    } 
  }
}
