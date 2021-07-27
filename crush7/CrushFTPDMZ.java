import com.crushftp.client.Common;
import com.crushftp.client.Worker;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.SharedSession;
import crushftp.server.AdminControls;
import crushftp.server.ServerStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class CrushFTPDMZ {
  public static Vector queue = new Vector();
  
  public static Properties dmzResponses = new Properties();
  
  boolean started = false;
  
  boolean starting = false;
  
  Vector read_socks = new Vector();
  
  Vector write_socks = new Vector();
  
  Common common_code = new Common();
  
  public static Vector socket_queue = new Vector();
  
  public static Properties sticky_tokens = new Properties();
  
  public static long last_token_scan = System.currentTimeMillis();
  
  public CrushFTPDMZ(String[] args) {
    Common.System2.put("crushftp.dmz.queue", queue);
    Common.System2.put("crushftp.dmz.queue.sock", socket_queue);
    String[] port_and_ips = args[1].split(",");
    int[] listen_ports = new int[port_and_ips.length];
    String[] listen_ips = new String[port_and_ips.length];
    for (int x = 0; x < port_and_ips.length; x++) {
      if (port_and_ips[x].indexOf(":") >= 0) {
        listen_ports[x] = Integer.parseInt(port_and_ips[x].split(":")[1]);
        listen_ips[x] = port_and_ips[x].split(":")[0];
      } else {
        listen_ports[x] = Integer.parseInt(port_and_ips[x]);
        listen_ips[x] = "0.0.0.0";
      } 
    } 
    String[] args2 = args;
    String allowed_ips_tmp = "";
    if (args2.length >= 3)
      allowed_ips_tmp = args2[2].trim(); 
    String allowed_ips = allowed_ips_tmp;
    try {
      for (int i = 0; i < listen_ports.length; i++) {
        int command_port = listen_ports[i];
        String command_ip = listen_ips[i];
        int loop_num = i;
        (new Thread(new Runnable(this, loop_num, command_port, command_ip, allowed_ips) {
              final CrushFTPDMZ this$0;
              
              private final int val$loop_num;
              
              private final int val$command_port;
              
              private final String val$command_ip;
              
              private final String val$allowed_ips;
              
              public void run() {
                Thread.currentThread().setName("DMZCommandSocketReceiver:" + this.val$loop_num);
                try {
                  ServerSocket ss_data = null;
                  if (System.getProperty("crushftp.dmz.ssl", "true").equals("true")) {
                    System.out.println("Waiting for DMZ connection from internal server on port " + this.val$command_port + ".");
                    ss_data = this.this$0.common_code.getServerSocket(this.val$command_port, this.val$command_ip, "builtin", "crushftp", "crushftp", "", false, 1);
                  } else {
                    ss_data = new ServerSocket(this.val$command_port);
                  } 
                  Socket sock = null;
                  String allowed_ips2 = this.val$allowed_ips;
                  while (true) {
                    try {
                      sock = ss_data.accept();
                      Common.sockLog(sock, "DMZ Incoming");
                      String incoming_ip = sock.getInetAddress().getHostAddress();
                      if (!allowed_ips2.equals("") && !Common.do_search(allowed_ips2, incoming_ip, false, 0) && allowed_ips2.indexOf(incoming_ip) < 0) {
                        System.out.println("IP " + sock.getInetAddress().getHostAddress() + " was from an untrusted host and was denied DMZ server contorl. Allowed IPs: " + allowed_ips2);
                        sock.close();
                        if (sock != null) {
                          Common.sockLog(sock, "DMZ closed");
                          sock.close();
                        } 
                        continue;
                      } 
                      if (allowed_ips2.equals(""))
                        allowed_ips2 = String.valueOf(incoming_ip.substring(0, incoming_ip.lastIndexOf(".") + 1)) + "*"; 
                      sock.setSoTimeout(10000);
                      int i = sock.getInputStream().read();
                    } catch (IOException e) {
                      String incoming_ip;
                      incoming_ip.printStackTrace();
                      continue;
                    } finally {
                      if (sock != null) {
                        Common.sockLog(sock, "DMZ closed");
                        sock.close();
                      } 
                    } 
                    if (sock != null) {
                      Common.sockLog(sock, "DMZ closed");
                      sock.close();
                    } 
                  } 
                } catch (Exception e) {
                  e.printStackTrace();
                  System.exit(0);
                  return;
                } 
              }
            })).start();
      } 
      while (true) {
        while (queue.size() <= 0)
          Thread.sleep(500L); 
        sendCommand(queue.remove(0));
      } 
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
      return;
    } 
  }
  
  public void cleanup_sticky_tokens() throws Exception {
    Worker.startWorker(new Runnable(this) {
          final CrushFTPDMZ this$0;
          
          public void run() {
            Enumeration keys = CrushFTPDMZ.sticky_tokens.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              Properties token_info = (Properties)CrushFTPDMZ.sticky_tokens.get(key);
              if (System.currentTimeMillis() - Long.parseLong(token_info.getProperty("used", "0")) > 300000L)
                CrushFTPDMZ.sticky_tokens.remove(key); 
              CrushFTPDMZ.last_token_scan = System.currentTimeMillis();
            } 
          }
        }"sticky_token_cleanup_scanner");
    last_token_scan = System.currentTimeMillis();
  }
  
  public void startDataSock(Socket sock) throws Exception {
    Worker.startWorker(new Runnable(this, sock) {
          final CrushFTPDMZ this$0;
          
          private final Socket val$sock;
          
          public void run() {
            Thread.currentThread().setName("DMZDataSocketReceiver:" + this.val$sock);
            try {
              int loops = 0;
              long start_loop = System.currentTimeMillis();
              while (CrushFTPDMZ.socket_queue.size() == 0 && loops++ < 8000)
                Thread.sleep(1L); 
              if (System.currentTimeMillis() - start_loop > 8000L) {
                Common.sockLog(this.val$sock, "data sock expired, closing");
                this.val$sock.close();
                return;
              } 
              Common.sockLog(this.val$sock, "data sock being used:" + (System.currentTimeMillis() - start_loop) + "ms old");
              do {
              
              } while (this.this$0.dataSockLoop(this.val$sock));
            } catch (Exception e) {
              e.printStackTrace();
              try {
                Common.sockLog(this.val$sock, "data sock loop exception:" + e);
                this.val$sock.close();
              } catch (Exception exception) {}
            } 
          }
        });
  }
  
  public boolean dataSockLoop(Socket sock) throws Exception {
    boolean skip = false;
    synchronized (socket_queue) {
      if (System.currentTimeMillis() - last_token_scan > 60000L)
        cleanup_sticky_tokens(); 
      if (socket_queue.size() > 0) {
        Properties mySock = socket_queue.elementAt(0);
        if (!mySock.getProperty("sticky_token", "").equals("")) {
          Properties token_info = (Properties)sticky_tokens.get(mySock.getProperty("sticky_token", ""));
          if (token_info == null || !token_info.getProperty("ip").equals(sock.getInetAddress().getHostAddress()))
            if (System.currentTimeMillis() - Long.parseLong(mySock.getProperty("created", "0")) > Integer.parseInt(System.getProperty("crushftp.dmz.stick_token_timeout", "20000")) || token_info == null) {
              if (token_info == null)
                token_info = new Properties(); 
              token_info.put("ip", sock.getInetAddress().getHostAddress());
              sticky_tokens.put(mySock.getProperty("sticky_token", ""), token_info);
            } else {
              skip = true;
            }  
          token_info.put("used", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        } 
        if (!skip) {
          sock.setSoTimeout(0);
          sock.getOutputStream().write((String.valueOf(mySock.getProperty("port")) + "                                                                                                           ").substring(0, 100).getBytes());
          sock.getOutputStream().flush();
          if (System.getProperty("crushftp.worker.v8.dmz", "true").equals("true")) {
            Common.sockLog(sock, "data sock loop used..notifying waiter");
            synchronized (mySock) {
              mySock = socket_queue.remove(0);
              mySock.put("socket", sock);
              mySock.notify();
            } 
          } else {
            Common.sockLog(sock, "data sock loop used..alerting waiter");
            mySock = socket_queue.remove(0);
            mySock.put("socket", sock);
          } 
          return false;
        } 
      } else {
        Common.sockLog(sock, "data sock loop close");
        sock.close();
        return false;
      } 
    } 
    if (skip)
      Thread.sleep(100L); 
    return true;
  }
  
  public void sendCommand(Properties p) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(baos);
      out.reset();
      synchronized (SharedSession.sessionLock) {
        out.writeObject(p);
        out.close();
      } 
      byte[] b = baos.toByteArray();
      boolean wrote = false;
      if (p.getProperty("need_response", "false").equalsIgnoreCase("true")) {
        p.put("status", "waiting");
        dmzResponses.put(p.getProperty("id"), p);
      } 
      for (int x = this.write_socks.size() - 1; x >= 0; x--) {
        Socket sock = this.write_socks.elementAt(x);
        try {
          sock.getOutputStream().write("0:".getBytes());
          Common.sockLog(sock, "writing command");
          int i = sock.getInputStream().read();
          if (i != 1)
            throw new IOException(sock); 
          Common.sockLog(sock, "writing command:" + p.getProperty("type"));
          long start = System.currentTimeMillis();
          sock.getOutputStream().write((String.valueOf(b.length) + ":").getBytes());
          sock.getOutputStream().write(b);
          sock.getOutputStream().flush();
          i = sock.getInputStream().read();
          if (i != 1)
            throw new IOException(sock); 
          wrote = true;
          Thread.currentThread().setName("DMZSender:queue=" + queue.size() + " last write len=" + b.length + "(" + p.getProperty("type") + ") milliseconds=" + (System.currentTimeMillis() - start));
        } catch (IOException e) {
          Common.sockLog(sock, "writing command error:" + e);
          sock.close();
          this.write_socks.remove(sock);
          Log.log("DMZ", 0, "Removed dead socket:" + sock + ":" + e);
        } 
      } 
      if (wrote) {
        Log.log("DMZ", 2, "WROTE:" + p.getProperty("type") + ":" + p.getProperty("id"));
      } else {
        Log.log("DMZ", 2, "FAILED WRITE:" + p.getProperty("type") + ":" + p.getProperty("id"));
        throw new Exception("Unable to write DMZ message, no server to write to:" + this.write_socks);
      } 
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
  
  private void startReceiver(Socket read_sock) {
    (new Thread(new Runnable(this, read_sock) {
          final CrushFTPDMZ this$0;
          
          private final Socket val$read_sock;
          
          public void run() {
            Thread.currentThread().setName("DMZResponseSocketReader:" + this.val$read_sock);
            try {
              InputStream in = this.val$read_sock.getInputStream();
              OutputStream out = this.val$read_sock.getOutputStream();
              byte[] b1 = new byte[1];
              while (true) {
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
                  Common.sockLog(this.val$read_sock, "read_sock got command:" + len);
                  while (totalBytes < len) {
                    bytesRead = in.read(b, totalBytes, len - totalBytes);
                    if (bytesRead < 0)
                      throw new Exception("DMZ:EOF reached in receiver read of chunk."); 
                    totalBytes += bytesRead;
                  } 
                  out.write(1);
                  out.flush();
                  if (len > 0) {
                    Common.sockLog(this.val$read_sock, "read_sock got command complete:" + len);
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(b));
                    Properties p = (Properties)ois.readObject();
                    ois.close();
                    this.this$0.processResponse(p);
                  } 
                } catch (SocketTimeoutException socketTimeoutException) {}
              } 
            } catch (Exception e) {
              System.out.println(new Date());
              e.printStackTrace();
              return;
            } 
          }
        })).start();
  }
  
  private void processResponse(Properties p) throws Exception {
    if (dmzResponses.containsKey(p.getProperty("id", ""))) {
      Properties response = (Properties)dmzResponses.remove(p.getProperty("id"));
      Log.log("DMZ", 0, "READ:IGNORING, ALREADY PROCESSED:" + p.getProperty("type") + ":" + p.getProperty("id"));
      if (response == null)
        return; 
      response.putAll(p);
      if (p.containsKey("data"))
        response.putAll((Properties)p.get("data")); 
      p = response;
      p.put("status", "done");
    } 
    Log.log("DMZ", 2, "READ:" + p.getProperty("type") + ":" + p.getProperty("id"));
    if (p.getProperty("type").equalsIgnoreCase("PUT:SERVER_SETTINGS")) {
      synchronized (queue) {
        if (!this.started) {
          this.starting = true;
          System.out.println(new Date() + ":DMZ Starting...");
          try {
            System.out.println(new Date() + ":DMZ Started");
          } catch (Exception e) {
            System.out.println(new Date() + ":DMZ Error");
            e.printStackTrace();
            System.exit(0);
          } 
          this.started = true;
        } else {
          Common.updateObject(p.get("data"), ServerStatus.server_settings);
        } 
      } 
      p.put("data", new Properties());
      p.put("type", "PUT:DMZ_STARTED");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:SYSTEM.PROPERTIES")) {
      Properties system_prop = (Properties)p.get("data");
      Log.log("DMZ", 2, "READ:" + system_prop);
      Common.System2.put(system_prop.getProperty("key"), system_prop.get("val"));
    } else if (p.getProperty("type").equalsIgnoreCase("GET:SERVER_SETTINGS")) {
      p.put("data", ServerStatus.server_settings);
      p.put("type", "RESPONSE");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("GET:SERVER_INFO")) {
      Properties request = (Properties)p.get("data");
      Properties si = new Properties();
      if (this.started)
        si = (Properties)ServerStatus.thisObj.server_info.clone(); 
      si.remove("plugins");
      if (request != null && (request.getProperty("key", "").equals("server_info") || request.getProperty("command", "").equals("getStatHistory"))) {
        si.remove("user_list");
        si.remove("recent_user_list");
      } else {
        Vector user_list = (Vector)((Vector)si.get("user_list")).clone();
        si.put("user_list", user_list);
        for (int x = 0; x < user_list.size(); x++) {
          Properties user_info = (Properties)((Properties)user_list.elementAt(x)).clone();
          user_list.setElementAt(user_info, x);
          user_info.remove("session");
        } 
      } 
      p.put("data", si);
      p.put("type", "RESPONSE");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("RUN:INSTANCE_ACTION")) {
      p.put("data", AdminControls.runInstanceAction((Properties)p.get("data"), p.getProperty("site")));
      p.put("type", "RESPONSE");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("RUN:JOB")) {
      Log.log("SERVER", 0, "READ:" + p.getProperty("type") + ":" + p.getProperty("id"));
      Properties p_f2 = p;
      Properties info = (Properties)p.remove("data");
      Worker.startWorker(new Runnable(this, info, p_f2) {
            final CrushFTPDMZ this$0;
            
            private final Properties val$info;
            
            private final Properties val$p_f2;
            
            public void run() {
              Vector items = (Vector)this.val$info.remove("items");
              Properties event = (Properties)this.val$info.remove("data");
              event.put("event_plugin_list", "CrushTask (User Defined)");
              event.put("name", event.getProperty("scheduleName"));
              Log.log("SERVER", 0, "READ:" + this.val$p_f2.getProperty("type") + ":" + this.val$p_f2.getProperty("id") + ":" + event.getProperty("name"));
              Properties info = ServerStatus.thisObj.events6.doEventPlugin(null, event, null, items);
              this.val$p_f2.put("data", info);
              byte[] b = (byte[])null;
              try {
                RandomAccessFile raf = new RandomAccessFile(info.getProperty("log_file"), "r");
                b = new byte[(int)raf.length()];
                raf.readFully(b);
                raf.close();
              } catch (Throwable e) {
                Log.log("DMZ", 0, e);
                b = new byte[0];
              } 
              this.val$p_f2.put("log", new String(b));
              this.val$p_f2.put("type", "RESPONSE");
              CrushFTPDMZ.queue.addElement(this.val$p_f2);
              Log.log("SERVER", 0, "READ:" + this.val$p_f2.getProperty("type") + ":" + this.val$p_f2.getProperty("id") + ":" + event.getProperty("name") + ":complete");
            }
          });
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:PING")) {
      Properties pong = (Properties)p.remove("data");
      pong.put("time2", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      p.put("data", pong);
      p.put("type", "PUT:PONG");
      queue.addElement(p);
    } 
  }
}
