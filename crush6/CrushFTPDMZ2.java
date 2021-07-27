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

public class CrushFTPDMZ2 {
  public static Vector queue = new Vector();
  
  public static Properties dmzResponses = new Properties();
  
  boolean started = false;
  
  Common common_code = new Common();
  
  public static Vector socket_queue = new Vector();
  
  Vector activeAdminSockets = new Vector();
  
  Vector activeAdminSocketsWrite = new Vector();
  
  public CrushFTPDMZ2(String[] args) {
    Common.System2.put("crushftp.dmz.queue", queue);
    Common.System2.put("crushftp.dmz.queue.sock", socket_queue);
    try {
      int data_port = Integer.parseInt(args[1]) + 1;
      (new Thread(new Runnable(this, data_port) {
            final CrushFTPDMZ2 this$0;
            
            private final int val$data_port;
            
            public void run() {
              // Byte code:
              //   0: invokestatic currentThread : ()Ljava/lang/Thread;
              //   3: ldc 'DMZDataSocketReceiver'
              //   5: invokevirtual setName : (Ljava/lang/String;)V
              //   8: aload_0
              //   9: getfield this$0 : LCrushFTPDMZ2;
              //   12: getfield common_code : Lcrushftp/handlers/Common;
              //   15: aload_0
              //   16: getfield val$data_port : I
              //   19: aconst_null
              //   20: ldc 'builtin'
              //   22: ldc 'crushftp'
              //   24: ldc 'crushftp'
              //   26: ldc ''
              //   28: iconst_0
              //   29: iconst_1
              //   30: invokevirtual getServerSocket : (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZI)Ljava/net/ServerSocket;
              //   33: astore_1
              //   34: aconst_null
              //   35: astore_2
              //   36: aload_1
              //   37: invokevirtual accept : ()Ljava/net/Socket;
              //   40: astore_2
              //   41: iconst_0
              //   42: istore_3
              //   43: goto -> 102
              //   46: getstatic CrushFTPDMZ2.socket_queue : Ljava/util/Vector;
              //   49: invokevirtual size : ()I
              //   52: ifle -> 96
              //   55: aload_2
              //   56: invokevirtual getOutputStream : ()Ljava/io/OutputStream;
              //   59: iconst_1
              //   60: invokevirtual write : (I)V
              //   63: aload_2
              //   64: invokevirtual getOutputStream : ()Ljava/io/OutputStream;
              //   67: invokevirtual flush : ()V
              //   70: getstatic CrushFTPDMZ2.socket_queue : Ljava/util/Vector;
              //   73: iconst_0
              //   74: invokevirtual remove : (I)Ljava/lang/Object;
              //   77: checkcast java/util/Properties
              //   80: astore #4
              //   82: aload #4
              //   84: ldc 'socket'
              //   86: aload_2
              //   87: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
              //   90: pop
              //   91: aconst_null
              //   92: astore_2
              //   93: goto -> 111
              //   96: ldc2_w 1000
              //   99: invokestatic sleep : (J)V
              //   102: iload_3
              //   103: iinc #3, 1
              //   106: bipush #40
              //   108: if_icmplt -> 46
              //   111: aload_2
              //   112: ifnull -> 154
              //   115: aload_2
              //   116: invokevirtual close : ()V
              //   119: goto -> 154
              //   122: astore_3
              //   123: aload_3
              //   124: invokevirtual printStackTrace : ()V
              //   127: goto -> 154
              //   130: astore_3
              //   131: goto -> 154
              //   134: astore #6
              //   136: jsr -> 142
              //   139: aload #6
              //   141: athrow
              //   142: astore #5
              //   144: aload_2
              //   145: ifnull -> 152
              //   148: aload_2
              //   149: invokevirtual close : ()V
              //   152: ret #5
              //   154: jsr -> 142
              //   157: goto -> 36
              //   160: astore_1
              //   161: aload_1
              //   162: invokevirtual printStackTrace : ()V
              //   165: iconst_0
              //   166: invokestatic exit : (I)V
              //   169: return
              // Line number table:
              //   Java source line number -> byte code offset
              //   #36	-> 0
              //   #39	-> 8
              //   #40	-> 34
              //   #45	-> 36
              //   #46	-> 41
              //   #47	-> 43
              //   #49	-> 46
              //   #51	-> 55
              //   #52	-> 63
              //   #53	-> 70
              //   #54	-> 82
              //   #55	-> 91
              //   #56	-> 93
              //   #58	-> 96
              //   #47	-> 102
              //   #60	-> 111
              //   #62	-> 122
              //   #64	-> 123
              //   #66	-> 130
              //   #71	-> 134
              //   #73	-> 139
              //   #71	-> 142
              //   #72	-> 144
              //   #73	-> 152
              //   #41	-> 157
              //   #76	-> 160
              //   #78	-> 161
              //   #79	-> 165
              //   #81	-> 169
              // Local variable table:
              //   start	length	slot	name	descriptor
              //   0	170	0	this	LCrushFTPDMZ2$1;
              //   34	126	1	ss_data	Ljava/net/ServerSocket;
              //   36	124	2	sock	Ljava/net/Socket;
              //   43	79	3	loops	I
              //   82	14	4	p	Ljava/util/Properties;
              //   123	4	3	e	Ljava/lang/ArrayIndexOutOfBoundsException;
              //   161	8	1	e	Ljava/lang/Exception;
              // Exception table:
              //   from	to	target	type
              //   8	160	160	java/lang/Exception
              //   36	119	122	java/lang/ArrayIndexOutOfBoundsException
              //   36	119	130	java/io/IOException
              //   36	127	134	finally
              //   130	131	134	finally
              //   154	157	134	finally
            }
          })).start();
      ServerSocket ss = this.common_code.getServerSocket(Integer.parseInt(args[1]), null, "builtin", "crushftp", "crushftp", "", false, 10);
      while (true) {
        Socket sock = ss.accept();
        synchronized (this.activeAdminSockets) {
          this.activeAdminSockets.addElement(sock);
        } 
        Runnable r = new Runnable(this, sock) {
            final CrushFTPDMZ2 this$0;
            
            private final Socket val$sock;
            
            public void run() {
              try {
                ObjectInputStream ois = new ObjectInputStream(this.val$sock.getInputStream());
                String ip = this.val$sock.getInetAddress().getHostAddress();
                boolean write = false;
                while (!write) {
                  Properties p = (Properties)ois.readObject();
                  if (CrushFTPDMZ2.dmzResponses.containsKey(p.getProperty("id", ""))) {
                    Properties response = (Properties)CrushFTPDMZ2.dmzResponses.remove(p.getProperty("id"));
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
                      synchronized (CrushFTPDMZ2.queue) {
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
                      CrushFTPDMZ2.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("PUT:SYSTEM.PROPERTIES")) {
                      Properties system_prop = (Properties)p.get("data");
                      Common.System2.put(system_prop.getProperty("key"), system_prop.get("val"));
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("GET:SERVER_SETTINGS")) {
                      p.put("data", ServerStatus.server_settings);
                      p.put("type", "RESPONSE");
                      CrushFTPDMZ2.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("GET:SERVER_INFO")) {
                      Properties request = (Properties)p.get("data");
                      Properties si = new Properties();
                      if (this.this$0.started)
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
                          user_info.remove("session_commands");
                          user_info.remove("session");
                        } 
                      } 
                      p.put("data", si);
                      p.put("type", "RESPONSE");
                      CrushFTPDMZ2.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("RUN:INSTANCE_ACTION")) {
                      p.put("data", AdminControls.runInstanceAction((Properties)p.get("data")));
                      p.put("type", "RESPONSE");
                      CrushFTPDMZ2.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("GET:PING")) {
                      p.put("data", new Properties());
                      p.put("type", "PUT:PONG");
                      CrushFTPDMZ2.queue.addElement(p);
                      continue;
                    } 
                    if (p.getProperty("type").equalsIgnoreCase("PUT:SOCKET")) {
                      this.this$0.activeAdminSockets.remove(this.val$sock);
                      p.put("socket", this.val$sock);
                      return;
                    } 
                  } 
                } 
                this.this$0.activeAdminSockets.remove(this.val$sock);
                if (write) {
                  this.this$0.activeAdminSocketsWrite.addElement(this.val$sock);
                  while (this.this$0.activeAdminSocketsWrite.size() > 5) {
                    Socket sock2 = this.this$0.activeAdminSocketsWrite.remove(0);
                    sock2.close();
                  } 
                  ObjectOutputStream oos = new ObjectOutputStream(this.val$sock.getOutputStream());
                  long lastwrite = System.currentTimeMillis();
                  while (write && !this.val$sock.isClosed()) {
                    Properties p = null;
                    while (CrushFTPDMZ2.queue.size() > 0) {
                      p = null;
                      if (System.currentTimeMillis() - lastwrite > 10000L) {
                        p = new Properties();
                        p.put("id", Common.makeBoundary());
                        p.put("data", new Properties());
                        p.put("type", "PUT:PONG");
                        p.put("need_response", "false");
                      } else {
                        synchronized (CrushFTPDMZ2.queue) {
                          if (CrushFTPDMZ2.queue.size() > 0)
                            p = CrushFTPDMZ2.queue.remove(0); 
                        } 
                      } 
                      if (p == null)
                        continue; 
                      if (p.getProperty("need_response", "false").equalsIgnoreCase("true")) {
                        p.put("status", "waiting");
                        CrushFTPDMZ2.dmzResponses.put(p.getProperty("id"), p);
                      } 
                      try {
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
                        p = null;
                        lastwrite = System.currentTimeMillis();
                      } finally {
                        if (p != null && !p.getProperty("type").equals("PUT:PONG"))
                          CrushFTPDMZ2.queue.insertElementAt(p, 0); 
                        Log.log("DMZ", 2, "WROTE:" + ip + ":" + ":Idled=" + (System.currentTimeMillis() - lastwrite) + ",queue=" + CrushFTPDMZ2.queue.size());
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
              synchronized (this.this$0.activeAdminSockets) {
                this.this$0.activeAdminSockets.remove(this.val$sock);
                this.this$0.activeAdminSocketsWrite.remove(this.val$sock);
              } 
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
