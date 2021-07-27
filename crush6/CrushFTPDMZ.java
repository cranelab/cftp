import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.AdminControls;
import crushftp.server.ServerStatus;
import crushftp.server.Worker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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
    try {
      for (int i = 0; i < listen_ports.length; i++) {
        int command_port = listen_ports[i];
        String command_ip = listen_ips[i];
        int loop_num = i;
        (new Thread(new Runnable(this, loop_num, command_port, command_ip, args2) {
              final CrushFTPDMZ this$0;
              
              private final int val$loop_num;
              
              private final int val$command_port;
              
              private final String val$command_ip;
              
              private final String[] val$args2;
              
              public void run() {
                // Byte code:
                //   0: invokestatic currentThread : ()Ljava/lang/Thread;
                //   3: new java/lang/StringBuffer
                //   6: dup
                //   7: ldc 'DMZCommandSocketReceiver:'
                //   9: invokespecial <init> : (Ljava/lang/String;)V
                //   12: aload_0
                //   13: getfield val$loop_num : I
                //   16: invokevirtual append : (I)Ljava/lang/StringBuffer;
                //   19: invokevirtual toString : ()Ljava/lang/String;
                //   22: invokevirtual setName : (Ljava/lang/String;)V
                //   25: aload_0
                //   26: getfield this$0 : LCrushFTPDMZ;
                //   29: getfield common_code : Lcrushftp/handlers/Common;
                //   32: aload_0
                //   33: getfield val$command_port : I
                //   36: aload_0
                //   37: getfield val$command_ip : Ljava/lang/String;
                //   40: ldc 'builtin'
                //   42: ldc 'crushftp'
                //   44: ldc 'crushftp'
                //   46: ldc ''
                //   48: iconst_0
                //   49: iconst_1
                //   50: invokevirtual getServerSocket : (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZI)Ljava/net/ServerSocket;
                //   53: astore_1
                //   54: aconst_null
                //   55: astore_2
                //   56: aload_1
                //   57: invokevirtual accept : ()Ljava/net/Socket;
                //   60: astore_2
                //   61: aload_0
                //   62: getfield val$args2 : [Ljava/lang/String;
                //   65: arraylength
                //   66: iconst_3
                //   67: if_icmplt -> 141
                //   70: aload_0
                //   71: getfield val$args2 : [Ljava/lang/String;
                //   74: iconst_2
                //   75: aaload
                //   76: aload_2
                //   77: invokevirtual getInetAddress : ()Ljava/net/InetAddress;
                //   80: invokevirtual getHostAddress : ()Ljava/lang/String;
                //   83: invokevirtual indexOf : (Ljava/lang/String;)I
                //   86: ifge -> 141
                //   89: getstatic java/lang/System.out : Ljava/io/PrintStream;
                //   92: new java/lang/StringBuffer
                //   95: dup
                //   96: ldc 'IP '
                //   98: invokespecial <init> : (Ljava/lang/String;)V
                //   101: aload_2
                //   102: invokevirtual getInetAddress : ()Ljava/net/InetAddress;
                //   105: invokevirtual getHostAddress : ()Ljava/lang/String;
                //   108: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
                //   111: ldc ' was from an untrusted host and was denied DMZ server contorl. Allowed IPs: '
                //   113: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
                //   116: aload_0
                //   117: getfield val$args2 : [Ljava/lang/String;
                //   120: iconst_2
                //   121: aaload
                //   122: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
                //   125: invokevirtual toString : ()Ljava/lang/String;
                //   128: invokevirtual println : (Ljava/lang/String;)V
                //   131: aload_2
                //   132: invokevirtual close : ()V
                //   135: jsr -> 239
                //   138: goto -> 56
                //   141: aload_2
                //   142: sipush #10000
                //   145: invokevirtual setSoTimeout : (I)V
                //   148: aload_2
                //   149: invokevirtual getInputStream : ()Ljava/io/InputStream;
                //   152: invokevirtual read : ()I
                //   155: istore_3
                //   156: iload_3
                //   157: bipush #82
                //   159: if_icmpne -> 186
                //   162: aload_0
                //   163: getfield this$0 : LCrushFTPDMZ;
                //   166: getfield read_socks : Ljava/util/Vector;
                //   169: aload_2
                //   170: invokevirtual addElement : (Ljava/lang/Object;)V
                //   173: aload_0
                //   174: getfield this$0 : LCrushFTPDMZ;
                //   177: aload_2
                //   178: invokestatic access$0 : (LCrushFTPDMZ;Ljava/net/Socket;)V
                //   181: aconst_null
                //   182: astore_2
                //   183: goto -> 251
                //   186: iload_3
                //   187: bipush #87
                //   189: if_icmpne -> 208
                //   192: aload_0
                //   193: getfield this$0 : LCrushFTPDMZ;
                //   196: getfield write_socks : Ljava/util/Vector;
                //   199: aload_2
                //   200: invokevirtual addElement : (Ljava/lang/Object;)V
                //   203: aconst_null
                //   204: astore_2
                //   205: goto -> 251
                //   208: iload_3
                //   209: bipush #68
                //   211: if_icmpne -> 251
                //   214: aload_0
                //   215: getfield this$0 : LCrushFTPDMZ;
                //   218: aload_2
                //   219: invokevirtual startDataSock : (Ljava/net/Socket;)V
                //   222: aconst_null
                //   223: astore_2
                //   224: goto -> 251
                //   227: astore_3
                //   228: goto -> 251
                //   231: astore #5
                //   233: jsr -> 239
                //   236: aload #5
                //   238: athrow
                //   239: astore #4
                //   241: aload_2
                //   242: ifnull -> 249
                //   245: aload_2
                //   246: invokevirtual close : ()V
                //   249: ret #4
                //   251: jsr -> 239
                //   254: goto -> 56
                //   257: astore_1
                //   258: aload_1
                //   259: invokevirtual printStackTrace : ()V
                //   262: iconst_0
                //   263: invokestatic exit : (I)V
                //   266: return
                // Line number table:
                //   Java source line number -> byte code offset
                //   #68	-> 0
                //   #71	-> 25
                //   #72	-> 54
                //   #77	-> 56
                //   #78	-> 61
                //   #80	-> 89
                //   #81	-> 131
                //   #82	-> 135
                //   #84	-> 141
                //   #85	-> 148
                //   #86	-> 156
                //   #88	-> 162
                //   #89	-> 173
                //   #90	-> 181
                //   #92	-> 186
                //   #94	-> 192
                //   #95	-> 203
                //   #97	-> 208
                //   #99	-> 214
                //   #100	-> 222
                //   #103	-> 227
                //   #108	-> 231
                //   #110	-> 236
                //   #108	-> 239
                //   #109	-> 241
                //   #110	-> 249
                //   #73	-> 254
                //   #113	-> 257
                //   #115	-> 258
                //   #116	-> 262
                //   #118	-> 266
                // Local variable table:
                //   start	length	slot	name	descriptor
                //   0	267	0	this	LCrushFTPDMZ$1;
                //   54	203	1	ss_data	Ljava/net/ServerSocket;
                //   56	201	2	sock	Ljava/net/Socket;
                //   156	71	3	i	I
                //   258	8	1	e	Ljava/lang/Exception;
                // Exception table:
                //   from	to	target	type
                //   25	257	257	java/lang/Exception
                //   56	138	227	java/io/IOException
                //   56	138	231	finally
                //   141	224	227	java/io/IOException
                //   141	228	231	finally
                //   251	254	231	finally
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
  
  public void startDataSock(Socket sock) {
    (new Thread(new Runnable(this, sock) {
          final CrushFTPDMZ this$0;
          
          private final Socket val$sock;
          
          public void run() {
            Thread.currentThread().setName("DMZDataSocketReceiver:" + this.val$sock);
            try {
              int loops = 0;
              while (CrushFTPDMZ.socket_queue.size() == 0 && loops++ < 40)
                Thread.sleep(200L); 
              while (true) {
                boolean skip = false;
                synchronized (CrushFTPDMZ.socket_queue) {
                  if (System.currentTimeMillis() - CrushFTPDMZ.last_token_scan > 60000L)
                    this.this$0.cleanup_sticky_tokens(); 
                  if (CrushFTPDMZ.socket_queue.size() > 0) {
                    Properties mySock = CrushFTPDMZ.socket_queue.elementAt(0);
                    if (!mySock.getProperty("sticky_token", "").equals("")) {
                      Properties token_info = (Properties)CrushFTPDMZ.sticky_tokens.get(mySock.getProperty("sticky_token", ""));
                      if (token_info == null || !token_info.getProperty("ip").equals(this.val$sock.getInetAddress().getHostAddress()))
                        if (System.currentTimeMillis() - Long.parseLong(mySock.getProperty("created", "0")) > 2000L || token_info == null) {
                          if (token_info == null)
                            token_info = new Properties(); 
                          token_info.put("ip", this.val$sock.getInetAddress().getHostAddress());
                          CrushFTPDMZ.sticky_tokens.put(mySock.getProperty("sticky_token", ""), token_info);
                        } else {
                          skip = true;
                        }  
                      token_info.put("used", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
                    } 
                    if (!skip) {
                      this.val$sock.getOutputStream().write((String.valueOf(mySock.getProperty("port")) + "                                                                                                           ").substring(0, 100).getBytes());
                      this.val$sock.getOutputStream().flush();
                      mySock = CrushFTPDMZ.socket_queue.remove(0);
                      mySock.put("socket", this.val$sock);
                      break;
                    } 
                  } else {
                    this.val$sock.close();
                    break;
                  } 
                } 
                if (skip)
                  Thread.sleep(100L); 
              } 
            } catch (Exception e) {
              e.printStackTrace();
              try {
                this.val$sock.close();
              } catch (Exception exception) {}
            } 
          }
        })).start();
  }
  
  public void sendCommand(Properties p) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream out = new ObjectOutputStream(baos);
      out.reset();
      out.writeObject(p);
      out.close();
      byte[] b = baos.toByteArray();
      boolean wrote = false;
      for (int x = this.write_socks.size() - 1; x >= 0; x--) {
        Socket sock = this.write_socks.elementAt(x);
        try {
          sock.getOutputStream().write("0:".getBytes());
          int i = sock.getInputStream().read();
          if (i != 1)
            throw new IOException(sock); 
          sock.getOutputStream().write((String.valueOf(b.length) + ":").getBytes());
          sock.getOutputStream().write(b);
          sock.getOutputStream().flush();
          i = sock.getInputStream().read();
          if (i != 1)
            throw new IOException(sock); 
          wrote = true;
        } catch (IOException e) {
          sock.close();
          this.write_socks.remove(sock);
          Log.log("DMZ", 0, "Removed dead socket:" + sock + ":" + e);
        } 
      } 
      if (wrote) {
        Log.log("DMZ", 2, "WROTE:" + p.getProperty("type"));
        if (p.getProperty("need_response", "false").equalsIgnoreCase("true")) {
          p.put("status", "waiting");
          dmzResponses.put(p.getProperty("id"), p);
        } 
      } else {
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
      Log.log("DMZ", 0, "READ:IGNORING, ALREADY PROCESSED:" + p.getProperty("type"));
      if (response == null)
        return; 
      response.putAll(p);
      if (p.containsKey("data"))
        response.putAll((Properties)p.get("data")); 
      p = response;
      p.put("status", "done");
    } 
    Log.log("DMZ", 2, "READ:" + p.getProperty("type"));
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
        } 
      } 
      p.put("data", new Properties());
      p.put("type", "PUT:DMZ_STARTED");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:SYSTEM.PROPERTIES")) {
      Properties system_prop = (Properties)p.get("data");
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
          user_info.remove("session_commands");
          user_info.remove("session");
        } 
      } 
      p.put("data", si);
      p.put("type", "RESPONSE");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("RUN:INSTANCE_ACTION")) {
      p.put("data", AdminControls.runInstanceAction((Properties)p.get("data")));
      p.put("type", "RESPONSE");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:PING")) {
      Properties pong = (Properties)p.remove("data");
      pong.put("time2", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      p.put("data", pong);
      p.put("type", "PUT:PONG");
      queue.addElement(p);
    } 
  }
}
