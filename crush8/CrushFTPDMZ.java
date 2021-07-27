import com.crushftp.client.Common;
import com.crushftp.client.File_S;
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
  
  boolean servers_stopped = false;
  
  int servers_stopped_count = 0;
  
  public CrushFTPDMZ(String[] args) {
    Common.dmz_mode = System.getProperty("crushftp.dmz", "true").equals("true");
    Common.System2.put("crushftp.dmz.queue", queue);
    Common.System2.put("crushftp.dmz.queue.sock", socket_queue);
    System.getProperties().put("crushftp.worker.v9", System.getProperty("crushftp.worker.v9", "false"));
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
                //   25: aconst_null
                //   26: astore_1
                //   27: getstatic java/lang/System.out : Ljava/io/PrintStream;
                //   30: new java/lang/StringBuffer
                //   33: dup
                //   34: ldc 'Waiting for DMZ connection from internal server on port '
                //   36: invokespecial <init> : (Ljava/lang/String;)V
                //   39: aload_0
                //   40: getfield val$command_port : I
                //   43: invokevirtual append : (I)Ljava/lang/StringBuffer;
                //   46: ldc '.'
                //   48: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
                //   51: invokevirtual toString : ()Ljava/lang/String;
                //   54: invokevirtual println : (Ljava/lang/String;)V
                //   57: ldc 'crushftp.dmz.ssl'
                //   59: ldc 'true'
                //   61: invokestatic getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                //   64: ldc 'true'
                //   66: invokevirtual equals : (Ljava/lang/Object;)Z
                //   69: ifeq -> 104
                //   72: aload_0
                //   73: getfield this$0 : LCrushFTPDMZ;
                //   76: getfield common_code : Lcrushftp/handlers/Common;
                //   79: aload_0
                //   80: getfield val$command_port : I
                //   83: aload_0
                //   84: getfield val$command_ip : Ljava/lang/String;
                //   87: ldc 'builtin'
                //   89: ldc 'crushftp'
                //   91: ldc 'crushftp'
                //   93: ldc ''
                //   95: iconst_0
                //   96: iconst_1
                //   97: invokevirtual getServerSocket : (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZI)Ljava/net/ServerSocket;
                //   100: astore_1
                //   101: goto -> 116
                //   104: new java/net/ServerSocket
                //   107: dup
                //   108: aload_0
                //   109: getfield val$command_port : I
                //   112: invokespecial <init> : (I)V
                //   115: astore_1
                //   116: aload_1
                //   117: sipush #11000
                //   120: invokevirtual setSoTimeout : (I)V
                //   123: aconst_null
                //   124: astore_2
                //   125: aload_0
                //   126: getfield val$allowed_ips : Ljava/lang/String;
                //   129: astore_3
                //   130: aload_1
                //   131: invokevirtual accept : ()Ljava/net/Socket;
                //   134: astore_2
                //   135: aload_0
                //   136: getfield this$0 : LCrushFTPDMZ;
                //   139: iconst_0
                //   140: putfield servers_stopped_count : I
                //   143: aload_0
                //   144: getfield this$0 : LCrushFTPDMZ;
                //   147: getfield servers_stopped : Z
                //   150: ifeq -> 179
                //   153: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
                //   156: pop
                //   157: ldc 'stop_dmz_ports_internal_down'
                //   159: invokestatic BG : (Ljava/lang/String;)Z
                //   162: ifeq -> 179
                //   165: aload_0
                //   166: getfield this$0 : LCrushFTPDMZ;
                //   169: iconst_0
                //   170: putfield servers_stopped : Z
                //   173: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
                //   176: invokevirtual start_all_servers : ()V
                //   179: aload_2
                //   180: ldc 'DMZ Incoming'
                //   182: invokestatic sockLog : (Ljava/net/Socket;Ljava/lang/String;)V
                //   185: aload_2
                //   186: invokevirtual getInetAddress : ()Ljava/net/InetAddress;
                //   189: invokevirtual getHostAddress : ()Ljava/lang/String;
                //   192: astore #4
                //   194: aload_3
                //   195: ldc ''
                //   197: invokevirtual equals : (Ljava/lang/Object;)Z
                //   200: ifne -> 270
                //   203: aload_3
                //   204: aload #4
                //   206: iconst_0
                //   207: iconst_0
                //   208: invokestatic do_search : (Ljava/lang/String;Ljava/lang/String;ZI)Z
                //   211: ifne -> 270
                //   214: aload_3
                //   215: aload #4
                //   217: invokevirtual indexOf : (Ljava/lang/String;)I
                //   220: ifge -> 270
                //   223: getstatic java/lang/System.out : Ljava/io/PrintStream;
                //   226: new java/lang/StringBuffer
                //   229: dup
                //   230: ldc 'IP '
                //   232: invokespecial <init> : (Ljava/lang/String;)V
                //   235: aload_2
                //   236: invokevirtual getInetAddress : ()Ljava/net/InetAddress;
                //   239: invokevirtual getHostAddress : ()Ljava/lang/String;
                //   242: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
                //   245: ldc ' was from an untrusted host and was denied DMZ server control. Allowed IPs: '
                //   247: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
                //   250: aload_3
                //   251: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
                //   254: invokevirtual toString : ()Ljava/lang/String;
                //   257: invokevirtual println : (Ljava/lang/String;)V
                //   260: aload_2
                //   261: invokevirtual close : ()V
                //   264: jsr -> 631
                //   267: goto -> 130
                //   270: aload_3
                //   271: ldc ''
                //   273: invokevirtual equals : (Ljava/lang/Object;)Z
                //   276: ifeq -> 313
                //   279: new java/lang/StringBuffer
                //   282: dup
                //   283: aload #4
                //   285: iconst_0
                //   286: aload #4
                //   288: ldc '.'
                //   290: invokevirtual lastIndexOf : (Ljava/lang/String;)I
                //   293: iconst_1
                //   294: iadd
                //   295: invokevirtual substring : (II)Ljava/lang/String;
                //   298: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
                //   301: invokespecial <init> : (Ljava/lang/String;)V
                //   304: ldc '*'
                //   306: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
                //   309: invokevirtual toString : ()Ljava/lang/String;
                //   312: astore_3
                //   313: aload_2
                //   314: sipush #10000
                //   317: invokevirtual setSoTimeout : (I)V
                //   320: aload_2
                //   321: invokevirtual getInputStream : ()Ljava/io/InputStream;
                //   324: invokevirtual read : ()I
                //   327: istore #5
                //   329: iload #5
                //   331: bipush #82
                //   333: if_icmpne -> 366
                //   336: aload_2
                //   337: ldc 'DMZ read_sock'
                //   339: invokestatic sockLog : (Ljava/net/Socket;Ljava/lang/String;)V
                //   342: aload_0
                //   343: getfield this$0 : LCrushFTPDMZ;
                //   346: getfield read_socks : Ljava/util/Vector;
                //   349: aload_2
                //   350: invokevirtual addElement : (Ljava/lang/Object;)V
                //   353: aload_0
                //   354: getfield this$0 : LCrushFTPDMZ;
                //   357: aload_2
                //   358: invokestatic access$0 : (LCrushFTPDMZ;Ljava/net/Socket;)V
                //   361: aconst_null
                //   362: astore_2
                //   363: goto -> 650
                //   366: iload #5
                //   368: bipush #87
                //   370: if_icmpne -> 395
                //   373: aload_2
                //   374: ldc 'DMZ write_sock'
                //   376: invokestatic sockLog : (Ljava/net/Socket;Ljava/lang/String;)V
                //   379: aload_0
                //   380: getfield this$0 : LCrushFTPDMZ;
                //   383: getfield write_socks : Ljava/util/Vector;
                //   386: aload_2
                //   387: invokevirtual addElement : (Ljava/lang/Object;)V
                //   390: aconst_null
                //   391: astore_2
                //   392: goto -> 650
                //   395: iload #5
                //   397: bipush #100
                //   399: if_icmpne -> 442
                //   402: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
                //   405: pop
                //   406: ldc 'serverbeat_dmz_master'
                //   408: invokestatic BG : (Ljava/lang/String;)Z
                //   411: ifeq -> 423
                //   414: aload_2
                //   415: ldc 'DMZ data_sock discarding since not master'
                //   417: invokestatic sockLog : (Ljava/net/Socket;Ljava/lang/String;)V
                //   420: goto -> 650
                //   423: aload_2
                //   424: ldc 'DMZ data_sock'
                //   426: invokestatic sockLog : (Ljava/net/Socket;Ljava/lang/String;)V
                //   429: aload_0
                //   430: getfield this$0 : LCrushFTPDMZ;
                //   433: aload_2
                //   434: invokevirtual startDataSock : (Ljava/net/Socket;)V
                //   437: aconst_null
                //   438: astore_2
                //   439: goto -> 650
                //   442: iload #5
                //   444: bipush #68
                //   446: if_icmpne -> 468
                //   449: aload_2
                //   450: ldc 'DMZ data_sock master'
                //   452: invokestatic sockLog : (Ljava/net/Socket;Ljava/lang/String;)V
                //   455: aload_0
                //   456: getfield this$0 : LCrushFTPDMZ;
                //   459: aload_2
                //   460: invokevirtual startDataSock : (Ljava/net/Socket;)V
                //   463: aconst_null
                //   464: astore_2
                //   465: goto -> 650
                //   468: iload #5
                //   470: bipush #69
                //   472: if_icmpne -> 488
                //   475: aload_0
                //   476: getfield this$0 : LCrushFTPDMZ;
                //   479: aload_2
                //   480: invokevirtual proxyConnection : (Ljava/net/Socket;)V
                //   483: aconst_null
                //   484: astore_2
                //   485: goto -> 650
                //   488: iload #5
                //   490: bipush #88
                //   492: if_icmpne -> 650
                //   495: aload_2
                //   496: astore #6
                //   498: new CrushFTPDMZ$2
                //   501: dup
                //   502: aload_0
                //   503: aload #6
                //   505: invokespecial <init> : (LCrushFTPDMZ$1;Ljava/net/Socket;)V
                //   508: new java/lang/StringBuffer
                //   511: dup
                //   512: ldc 'DMZ_process_executor:'
                //   514: invokespecial <init> : (Ljava/lang/String;)V
                //   517: aload_2
                //   518: invokevirtual append : (Ljava/lang/Object;)Ljava/lang/StringBuffer;
                //   521: ldc ':'
                //   523: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
                //   526: invokevirtual toString : ()Ljava/lang/String;
                //   529: invokestatic startWorker : (Ljava/lang/Runnable;Ljava/lang/String;)Z
                //   532: pop
                //   533: aconst_null
                //   534: astore_2
                //   535: goto -> 650
                //   538: astore #4
                //   540: aload_0
                //   541: getfield this$0 : LCrushFTPDMZ;
                //   544: getfield started : Z
                //   547: ifeq -> 650
                //   550: aload_0
                //   551: getfield this$0 : LCrushFTPDMZ;
                //   554: dup
                //   555: getfield servers_stopped_count : I
                //   558: iconst_1
                //   559: iadd
                //   560: putfield servers_stopped_count : I
                //   563: aload_0
                //   564: getfield this$0 : LCrushFTPDMZ;
                //   567: getfield servers_stopped_count : I
                //   570: iconst_2
                //   571: if_icmple -> 650
                //   574: aload_0
                //   575: getfield this$0 : LCrushFTPDMZ;
                //   578: getfield servers_stopped : Z
                //   581: ifne -> 650
                //   584: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
                //   587: pop
                //   588: ldc 'stop_dmz_ports_internal_down'
                //   590: invokestatic BG : (Ljava/lang/String;)Z
                //   593: ifeq -> 650
                //   596: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
                //   599: invokevirtual stop_all_servers : ()V
                //   602: aload_0
                //   603: getfield this$0 : LCrushFTPDMZ;
                //   606: iconst_1
                //   607: putfield servers_stopped : Z
                //   610: goto -> 650
                //   613: astore #4
                //   615: aload #4
                //   617: invokevirtual printStackTrace : ()V
                //   620: goto -> 650
                //   623: astore #8
                //   625: jsr -> 631
                //   628: aload #8
                //   630: athrow
                //   631: astore #7
                //   633: aload_2
                //   634: ifnull -> 648
                //   637: aload_2
                //   638: ldc_w 'DMZ closed'
                //   641: invokestatic sockLog : (Ljava/net/Socket;Ljava/lang/String;)V
                //   644: aload_2
                //   645: invokevirtual close : ()V
                //   648: ret #7
                //   650: jsr -> 631
                //   653: goto -> 130
                //   656: astore_1
                //   657: aload_1
                //   658: invokevirtual printStackTrace : ()V
                //   661: iconst_0
                //   662: invokestatic exit : (I)V
                //   665: return
                // Line number table:
                //   Java source line number -> byte code offset
                //   #79	-> 0
                //   #82	-> 25
                //   #83	-> 27
                //   #84	-> 57
                //   #85	-> 104
                //   #86	-> 116
                //   #87	-> 123
                //   #88	-> 125
                //   #93	-> 130
                //   #94	-> 135
                //   #95	-> 143
                //   #97	-> 165
                //   #98	-> 173
                //   #100	-> 179
                //   #101	-> 185
                //   #102	-> 194
                //   #104	-> 223
                //   #105	-> 260
                //   #106	-> 264
                //   #108	-> 270
                //   #109	-> 313
                //   #110	-> 320
                //   #111	-> 329
                //   #113	-> 336
                //   #114	-> 342
                //   #115	-> 353
                //   #116	-> 361
                //   #118	-> 366
                //   #120	-> 373
                //   #121	-> 379
                //   #122	-> 390
                //   #124	-> 395
                //   #126	-> 402
                //   #129	-> 423
                //   #130	-> 429
                //   #131	-> 437
                //   #134	-> 442
                //   #136	-> 449
                //   #137	-> 455
                //   #138	-> 463
                //   #140	-> 468
                //   #142	-> 475
                //   #143	-> 483
                //   #145	-> 488
                //   #147	-> 495
                //   #148	-> 498
                //   #187	-> 508
                //   #148	-> 529
                //   #188	-> 533
                //   #191	-> 538
                //   #193	-> 540
                //   #195	-> 550
                //   #197	-> 563
                //   #199	-> 596
                //   #200	-> 602
                //   #204	-> 613
                //   #206	-> 615
                //   #210	-> 623
                //   #216	-> 628
                //   #210	-> 631
                //   #211	-> 633
                //   #213	-> 637
                //   #214	-> 644
                //   #216	-> 648
                //   #89	-> 653
                //   #219	-> 656
                //   #221	-> 657
                //   #222	-> 661
                //   #224	-> 665
                // Local variable table:
                //   start	length	slot	name	descriptor
                //   0	666	0	this	LCrushFTPDMZ$1;
                //   27	629	1	ss_data	Ljava/net/ServerSocket;
                //   125	531	2	sock	Ljava/net/Socket;
                //   130	526	3	allowed_ips2	Ljava/lang/String;
                //   194	344	4	incoming_ip	Ljava/lang/String;
                //   329	209	5	i	I
                //   498	37	6	sock2	Ljava/net/Socket;
                //   540	70	4	e	Ljava/net/SocketTimeoutException;
                //   615	5	4	e	Ljava/io/IOException;
                //   657	8	1	e	Ljava/lang/Exception;
                // Exception table:
                //   from	to	target	type
                //   25	656	656	java/lang/Exception
                //   130	267	538	java/net/SocketTimeoutException
                //   130	267	613	java/io/IOException
                //   130	267	623	finally
                //   270	535	538	java/net/SocketTimeoutException
                //   270	535	613	java/io/IOException
                //   270	610	623	finally
                //   613	620	623	finally
                //   650	653	623	finally
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
  
  public void proxyConnection(Socket sock) throws Exception {
    Worker.startWorker(new Runnable(this, sock) {
          final CrushFTPDMZ this$0;
          
          private final Socket val$sock;
          
          public void run() {
            Socket sock2 = null;
            String host_port = "";
            try {
              Common.sockLog(this.val$sock, "DMZ external_sock");
              int len = this.val$sock.getInputStream().read();
              byte[] b = new byte[len];
              while (len > 0) {
                int len2 = this.val$sock.getInputStream().read(b, b.length - len, len);
                if (len2 < 0) {
                  len = len2;
                  continue;
                } 
                len -= len2;
              } 
              host_port = new String(b, "UTF8");
              Log.log("DMZ", 0, "CONNECTING:Proxying outgoing connection from internal server (" + this.val$sock + ") to:" + host_port);
              sock2 = new Socket(host_port.split(":")[0], Integer.parseInt(host_port.split(":")[1]));
              Log.log("DMZ", 0, "SUCCESS:Proxyied outgoing connection from internal server (" + this.val$sock + ") to:" + host_port);
              Common.sockLog(sock2, "DMZ external_sock2");
              sock2.setTcpNoDelay(true);
              this.val$sock.setSoTimeout(600000);
              sock2.setSoTimeout(600000);
              Common.streamCopier(this.val$sock, sock2, this.val$sock.getInputStream(), sock2.getOutputStream(), true, true, true);
              Common.streamCopier(this.val$sock, sock2, sock2.getInputStream(), this.val$sock.getOutputStream(), true, true, true);
            } catch (Exception e) {
              Log.log("DMZ", 0, "Proxy sock error:" + this.val$sock + "->" + sock2 + ":" + host_port + ":" + e);
              try {
                this.val$sock.close();
              } catch (IOException iOException) {}
            } 
          }
        });
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
          Common.sockLog(sock, "data sock loop used..alerting waiter");
          mySock = socket_queue.remove(0);
          mySock.put("socket", sock);
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
        if (!p.getProperty("type").equals("PUT:LOGGING"))
          Log.log("DMZ", 2, "WROTE:" + p.getProperty("type") + ":" + p.getProperty("id")); 
      } else {
        Log.log("DMZ", 2, "FAILED WRITE:" + p.getProperty("type") + ":" + p.getProperty("id"));
        throw new Exception("Unable to write DMZ message, no server to write to:" + this.write_socks + " servers_stopped_count=" + this.servers_stopped_count + " servers_stopped=" + this.servers_stopped);
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
            Properties server_settings2 = (Properties)p.get("data");
            System.out.println(new Date() + ":DMZ Started");
          } catch (Exception e) {
            System.out.println(new Date() + ":DMZ Error");
            e.printStackTrace();
            System.exit(0);
          } 
          this.started = true;
        } else {
          Common.updateObjectLog(p.get("data"), ServerStatus.server_settings, null);
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
      p.put("data", AdminControls.runInstanceAction((Properties)p.get("data"), p.getProperty("site"), "127.0.0.1"));
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
                RandomAccessFile raf = new RandomAccessFile(new File_S(info.getProperty("log_file")), "r");
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
      queue.insertElementAt(p, 0);
    } 
  }
}
