package crushftp.handlers;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import crushftp.db.SearchHandler;
import crushftp.gui.LOC;
import crushftp.server.RETR_handler;
import crushftp.server.STOR_handler;
import crushftp.server.ServerSessionFTP;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
import crushftp.server.ssh.SSHSocket;
import crushftp.user.XMLUsers;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Pattern;

public class SessionCrush implements Serializable {
  static final long serialVersionUID = 0L;
  
  public static final String CRLF = "\r\n";
  
  public static Object set_quota_lock = new Object();
  
  public transient Vector session_socks = new Vector();
  
  public transient Vector data_socks = new Vector();
  
  public transient Vector old_data_socks = new Vector();
  
  public transient Vector pasv_socks = new Vector();
  
  public transient Vector stor_files_pool_free = new Vector();
  
  public transient Vector retr_files_pool_free = new Vector();
  
  public transient Vector stor_files_pool_used = new Vector();
  
  public transient Vector retr_files_pool_used = new Vector();
  
  public transient SimpleDateFormat hh = new SimpleDateFormat("HH", Locale.US);
  
  public transient SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  
  public transient SimpleDateFormat sdf_yyyyMMddHHmmssGMT = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
  
  public Properties user = null;
  
  public Properties user_info = new Properties();
  
  public boolean not_done = true;
  
  public VFS uVFS = null;
  
  public VFS expired_uVFS = null;
  
  public Properties rnfr_file = null;
  
  public String rnfr_file_path = null;
  
  public transient ServerSessionFTP ftp = null;
  
  public Properties server_item = null;
  
  public final Properties accessExceptions = new Properties();
  
  public final Properties quotaDelta = new Properties();
  
  public SimpleDateFormat date_time = new SimpleDateFormat("MM/dd/yy", Locale.US);
  
  transient boolean allow_replication = true;
  
  long last_active_replicate = System.currentTimeMillis();
  
  boolean shareVFS;
  
  public void setFtp(ServerSessionFTP ftp) {
    this.ftp = ftp;
  }
  
  public int uiIG(String data) {
    try {
      return Integer.parseInt(uiSG(data));
    } catch (Exception exception) {
      return 0;
    } 
  }
  
  public long uiLG(String data) {
    try {
      return Long.parseLong(uiSG(data));
    } catch (Exception exception) {
      return 0L;
    } 
  }
  
  public boolean uiBG(String data) {
    return uiSG(data).toLowerCase().equals("true");
  }
  
  public String uiSG(String data) {
    if (this.user_info.containsKey(data))
      return this.user_info.getProperty(data); 
    return "";
  }
  
  public void uiPUT(String key, Object val) {
    put(key, val);
  }
  
  public void uiPUT(String key, boolean val) {
    uiPUT(key, (new StringBuffer(String.valueOf(val))).toString());
  }
  
  public void uiPPUT(String key, long val) {
    uiPUT(key, (new StringBuffer(String.valueOf(uiLG(key) + val))).toString());
  }
  
  public Vector uiVG(String key) {
    return (Vector)this.user_info.get(key);
  }
  
  public Properties uiPG(String key) {
    return (Properties)this.user_info.get(key);
  }
  
  public void put(String key, Object val) {
    put(key, val, true);
  }
  
  public void put(String key, Object val2, boolean replicate) {
    Properties session = this.user_info;
    if (val2 == null) {
      session.remove(key);
    } else {
      Object object = session.put(key, val2);
      if (object != null && object.equals(val2))
        return; 
      if (replicate && this.allow_replication) {
        if (key.equals("dont_read"))
          return; 
        if (key.equals("dont_write"))
          return; 
        if (key.equals("last_logged_command"))
          return; 
        if (key.equals("request"))
          return; 
        if (key.equals("last_priv_dir"))
          return; 
        if (key.equals("skip_proxy_check"))
          return; 
        if (key.equals("bytes_received"))
          return; 
        if (key.equals("bytes_received_formatted"))
          return; 
        if (key.equals("bytes_sent"))
          return; 
        if (key.equals("bytes_sent_formatted"))
          return; 
        if (SharedSessionReplicated.send_queues.size() > 0 && ServerStatus.BG("replicate_sessions"))
          SharedSessionReplicated.send(getId(), "crushftp.session.update", key, val2); 
      } 
    } 
  }
  
  public String getProperty(String key) {
    Properties session = this.user_info;
    return session.getProperty(key);
  }
  
  public String getProperty(String key, String defaultVal) {
    Properties session = this.user_info;
    return session.getProperty(key, defaultVal);
  }
  
  public Object get(String key) {
    Properties session = this.user_info;
    return session.get(key);
  }
  
  public boolean containsKey(String key) {
    Properties session = this.user_info;
    return session.containsKey(key);
  }
  
  public void putAll(Properties p) {
    Properties session = this.user_info;
    session.putAll(p);
  }
  
  public Object remove(String key) {
    Properties session = this.user_info;
    return session.remove(key);
  }
  
  public void active() {
    put("last_activity", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString(), true);
    this.last_active_replicate = System.currentTimeMillis();
  }
  
  public void active_transfer() {
    if (System.currentTimeMillis() - this.last_active_replicate > 60000L) {
      this.last_active_replicate = System.currentTimeMillis();
      put("last_activity", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString(), true);
      this.last_active_replicate = System.currentTimeMillis();
    } else {
      put("last_activity", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString(), false);
    } 
  }
  
  public void do_kill() {
    do_kill(null);
  }
  
  public void do_kill(IdlerKiller thread_killer_item) {
    // Byte code:
    //   0: aload_0
    //   1: iconst_0
    //   2: putfield not_done : Z
    //   5: aload_1
    //   6: ifnull -> 18
    //   9: aload_1
    //   10: iconst_1
    //   11: putfield die_now : Z
    //   14: goto -> 18
    //   17: astore_2
    //   18: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   21: getfield statTools : Lcrushftp/db/StatTools;
    //   24: ldc_w 'stats_update_sessions'
    //   27: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   30: iconst_2
    //   31: anewarray java/lang/Object
    //   34: dup
    //   35: iconst_0
    //   36: new java/util/Date
    //   39: dup
    //   40: invokespecial <init> : ()V
    //   43: aastore
    //   44: dup
    //   45: iconst_1
    //   46: aload_0
    //   47: getfield user_info : Ljava/util/Properties;
    //   50: ldc_w 'SESSION_RID'
    //   53: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   56: aastore
    //   57: invokevirtual executeSql : (Ljava/lang/String;[Ljava/lang/Object;)V
    //   60: goto -> 72
    //   63: astore_2
    //   64: ldc 'SERVER'
    //   66: iconst_2
    //   67: aload_2
    //   68: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   71: pop
    //   72: aload_0
    //   73: ldc_w 'didDisconnect'
    //   76: invokevirtual uiBG : (Ljava/lang/String;)Z
    //   79: ifne -> 113
    //   82: aload_0
    //   83: ldc_w 'didDisconnect'
    //   86: ldc_w 'true'
    //   89: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   92: aload_0
    //   93: ldc_w 'LOGOUT'
    //   96: aconst_null
    //   97: invokevirtual do_event5 : (Ljava/lang/String;Ljava/util/Properties;)Ljava/util/Properties;
    //   100: pop
    //   101: goto -> 113
    //   104: astore_2
    //   105: ldc 'SERVER'
    //   107: iconst_2
    //   108: aload_2
    //   109: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   112: pop
    //   113: aload_0
    //   114: ldc_w 'ratio_field_permanent'
    //   117: invokevirtual BG : (Ljava/lang/String;)Z
    //   120: ifeq -> 257
    //   123: ldc_w 'dmz_mode'
    //   126: invokestatic siBG : (Ljava/lang/String;)Z
    //   129: ifne -> 184
    //   132: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   135: aload_0
    //   136: ldc 'listen_ip_port'
    //   138: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   141: aload_0
    //   142: ldc_w 'user_name'
    //   145: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   148: ldc_w 'user_bytes_sent'
    //   151: new java/lang/StringBuffer
    //   154: dup
    //   155: aload_0
    //   156: ldc_w 'bytes_sent'
    //   159: invokevirtual uiLG : (Ljava/lang/String;)J
    //   162: aload_0
    //   163: ldc_w 'ratio_bytes_sent'
    //   166: invokevirtual uiLG : (Ljava/lang/String;)J
    //   169: ladd
    //   170: invokestatic valueOf : (J)Ljava/lang/String;
    //   173: invokespecial <init> : (Ljava/lang/String;)V
    //   176: invokevirtual toString : ()Ljava/lang/String;
    //   179: iconst_0
    //   180: iconst_1
    //   181: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   184: ldc_w 'dmz_mode'
    //   187: invokestatic siBG : (Ljava/lang/String;)Z
    //   190: ifne -> 257
    //   193: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   196: aload_0
    //   197: ldc 'listen_ip_port'
    //   199: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   202: aload_0
    //   203: ldc_w 'user_name'
    //   206: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   209: ldc_w 'user_bytes_received'
    //   212: new java/lang/StringBuffer
    //   215: dup
    //   216: aload_0
    //   217: ldc_w 'bytes_received'
    //   220: invokevirtual uiLG : (Ljava/lang/String;)J
    //   223: aload_0
    //   224: ldc_w 'ratio_bytes_received'
    //   227: invokevirtual uiLG : (Ljava/lang/String;)J
    //   230: ladd
    //   231: invokestatic valueOf : (J)Ljava/lang/String;
    //   234: invokespecial <init> : (Ljava/lang/String;)V
    //   237: invokevirtual toString : ()Ljava/lang/String;
    //   240: iconst_0
    //   241: iconst_1
    //   242: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   245: goto -> 257
    //   248: astore_2
    //   249: ldc 'SERVER'
    //   251: iconst_2
    //   252: aload_2
    //   253: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   256: pop
    //   257: aload_0
    //   258: getfield uVFS : Lcrushftp/server/VFS;
    //   261: ifnull -> 271
    //   264: aload_0
    //   265: getfield uVFS : Lcrushftp/server/VFS;
    //   268: invokevirtual free : ()V
    //   271: aload_0
    //   272: getfield uVFS : Lcrushftp/server/VFS;
    //   275: ifnull -> 308
    //   278: aload_0
    //   279: getfield server_item : Ljava/util/Properties;
    //   282: ldc 'serverType'
    //   284: ldc 'ftp'
    //   286: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   289: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   292: ldc_w 'HTTP'
    //   295: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   298: ifne -> 308
    //   301: aload_0
    //   302: getfield uVFS : Lcrushftp/server/VFS;
    //   305: invokevirtual disconnect : ()V
    //   308: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   311: aload_0
    //   312: getfield user_info : Ljava/util/Properties;
    //   315: invokevirtual remove_user : (Ljava/util/Properties;)V
    //   318: goto -> 335
    //   321: aload_0
    //   322: getfield pasv_socks : Ljava/util/Vector;
    //   325: iconst_0
    //   326: invokevirtual remove : (I)Ljava/lang/Object;
    //   329: checkcast java/net/ServerSocket
    //   332: invokevirtual close : ()V
    //   335: aload_0
    //   336: getfield pasv_socks : Ljava/util/Vector;
    //   339: invokevirtual size : ()I
    //   342: ifgt -> 321
    //   345: goto -> 349
    //   348: astore_2
    //   349: invokestatic getProperties : ()Ljava/util/Properties;
    //   352: ldc_w 'crushftp.sftp.wait_transfers'
    //   355: ldc_w 'true'
    //   358: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   361: ldc_w 'true'
    //   364: invokevirtual equals : (Ljava/lang/Object;)Z
    //   367: ifeq -> 458
    //   370: invokestatic currentTimeMillis : ()J
    //   373: lstore_2
    //   374: goto -> 424
    //   377: ldc2_w 1000
    //   380: invokestatic sleep : (J)V
    //   383: invokestatic currentTimeMillis : ()J
    //   386: lload_2
    //   387: lsub
    //   388: ldc2_w 3000
    //   391: lcmp
    //   392: ifle -> 424
    //   395: ldc 'SERVER'
    //   397: iconst_2
    //   398: new java/lang/StringBuffer
    //   401: dup
    //   402: ldc_w 'Waiting for STOR/RETR threads to finish...'
    //   405: invokespecial <init> : (Ljava/lang/String;)V
    //   408: invokestatic currentThread : ()Ljava/lang/Thread;
    //   411: invokevirtual getName : ()Ljava/lang/String;
    //   414: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   417: invokevirtual toString : ()Ljava/lang/String;
    //   420: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   423: pop
    //   424: aload_0
    //   425: getfield retr_files_pool_used : Ljava/util/Vector;
    //   428: invokevirtual size : ()I
    //   431: aload_0
    //   432: getfield stor_files_pool_used : Ljava/util/Vector;
    //   435: invokevirtual size : ()I
    //   438: iadd
    //   439: ifle -> 458
    //   442: invokestatic currentTimeMillis : ()J
    //   445: lload_2
    //   446: lsub
    //   447: ldc2_w 10000
    //   450: lcmp
    //   451: iflt -> 377
    //   454: goto -> 458
    //   457: astore_2
    //   458: aload_0
    //   459: aload_0
    //   460: getfield retr_files_pool_free : Ljava/util/Vector;
    //   463: invokevirtual kill_retr_files : (Ljava/util/Vector;)V
    //   466: aload_0
    //   467: aload_0
    //   468: getfield retr_files_pool_used : Ljava/util/Vector;
    //   471: invokevirtual kill_retr_files : (Ljava/util/Vector;)V
    //   474: goto -> 478
    //   477: astore_2
    //   478: aload_0
    //   479: aload_0
    //   480: getfield stor_files_pool_free : Ljava/util/Vector;
    //   483: invokevirtual kill_stor_files : (Ljava/util/Vector;)V
    //   486: aload_0
    //   487: aload_0
    //   488: getfield stor_files_pool_used : Ljava/util/Vector;
    //   491: invokevirtual kill_stor_files : (Ljava/util/Vector;)V
    //   494: goto -> 498
    //   497: astore_2
    //   498: aload_0
    //   499: getfield session_socks : Ljava/util/Vector;
    //   502: invokevirtual size : ()I
    //   505: iconst_1
    //   506: isub
    //   507: istore_2
    //   508: goto -> 542
    //   511: aload_0
    //   512: getfield session_socks : Ljava/util/Vector;
    //   515: iload_2
    //   516: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   519: checkcast java/net/Socket
    //   522: astore_3
    //   523: aload_3
    //   524: invokevirtual isClosed : ()Z
    //   527: ifeq -> 539
    //   530: aload_0
    //   531: getfield session_socks : Ljava/util/Vector;
    //   534: aload_3
    //   535: invokevirtual remove : (Ljava/lang/Object;)Z
    //   538: pop
    //   539: iinc #2, -1
    //   542: iload_2
    //   543: ifge -> 511
    //   546: aload_0
    //   547: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   550: ifnull -> 642
    //   553: aload_0
    //   554: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   557: getfield sockOriginal : Ljava/net/Socket;
    //   560: sipush #2000
    //   563: invokevirtual setSoTimeout : (I)V
    //   566: aload_0
    //   567: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   570: getfield sockOriginal : Ljava/net/Socket;
    //   573: iconst_1
    //   574: iconst_2
    //   575: invokevirtual setSoLinger : (ZI)V
    //   578: aload_0
    //   579: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   582: getfield sockOriginal : Ljava/net/Socket;
    //   585: invokevirtual close : ()V
    //   588: goto -> 642
    //   591: astore_2
    //   592: goto -> 642
    //   595: aload_0
    //   596: getfield old_data_socks : Ljava/util/Vector;
    //   599: iconst_0
    //   600: invokevirtual remove : (I)Ljava/lang/Object;
    //   603: astore_2
    //   604: aload_2
    //   605: instanceof java/net/Socket
    //   608: ifeq -> 628
    //   611: aload_2
    //   612: checkcast java/net/Socket
    //   615: sipush #2000
    //   618: invokevirtual setSoTimeout : (I)V
    //   621: aload_2
    //   622: checkcast java/net/Socket
    //   625: invokevirtual close : ()V
    //   628: aload_2
    //   629: instanceof java/net/ServerSocket
    //   632: ifeq -> 642
    //   635: aload_2
    //   636: checkcast java/net/ServerSocket
    //   639: invokevirtual close : ()V
    //   642: aload_0
    //   643: getfield old_data_socks : Ljava/util/Vector;
    //   646: invokevirtual size : ()I
    //   649: ifgt -> 595
    //   652: goto -> 656
    //   655: astore_2
    //   656: aload_0
    //   657: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   660: ifnull -> 677
    //   663: aload_0
    //   664: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   667: getfield os : Ljava/io/OutputStream;
    //   670: invokevirtual close : ()V
    //   673: goto -> 677
    //   676: astore_2
    //   677: aload_0
    //   678: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   681: ifnull -> 698
    //   684: aload_0
    //   685: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   688: getfield is : Ljava/io/BufferedReader;
    //   691: invokevirtual close : ()V
    //   694: goto -> 698
    //   697: astore_2
    //   698: return
    // Line number table:
    //   Java source line number -> byte code offset
    //   #479	-> 0
    //   #480	-> 5
    //   #484	-> 9
    //   #486	-> 17
    //   #492	-> 18
    //   #494	-> 63
    //   #496	-> 64
    //   #500	-> 72
    //   #502	-> 82
    //   #503	-> 92
    //   #506	-> 104
    //   #508	-> 105
    //   #512	-> 113
    //   #514	-> 123
    //   #515	-> 184
    //   #518	-> 248
    //   #520	-> 249
    //   #522	-> 257
    //   #523	-> 271
    //   #525	-> 308
    //   #528	-> 318
    //   #529	-> 321
    //   #528	-> 335
    //   #531	-> 348
    //   #534	-> 349
    //   #538	-> 370
    //   #539	-> 374
    //   #541	-> 377
    //   #542	-> 383
    //   #544	-> 395
    //   #539	-> 424
    //   #548	-> 457
    //   #554	-> 458
    //   #555	-> 466
    //   #557	-> 477
    //   #562	-> 478
    //   #563	-> 486
    //   #565	-> 497
    //   #568	-> 498
    //   #570	-> 511
    //   #571	-> 523
    //   #568	-> 539
    //   #573	-> 546
    //   #577	-> 553
    //   #578	-> 566
    //   #579	-> 578
    //   #581	-> 591
    //   #587	-> 592
    //   #589	-> 595
    //   #590	-> 604
    //   #592	-> 611
    //   #593	-> 621
    //   #595	-> 628
    //   #587	-> 642
    //   #598	-> 655
    //   #603	-> 656
    //   #605	-> 676
    //   #610	-> 677
    //   #612	-> 697
    //   #616	-> 698
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	699	0	this	Lcrushftp/handlers/SessionCrush;
    //   0	699	1	thread_killer_item	Lcrushftp/handlers/IdlerKiller;
    //   64	8	2	e	Ljava/lang/Exception;
    //   105	8	2	e	Ljava/lang/Exception;
    //   249	8	2	e	Ljava/lang/Exception;
    //   374	83	2	start	J
    //   508	38	2	x	I
    //   523	16	3	sock	Ljava/net/Socket;
    //   604	38	2	obj	Ljava/lang/Object;
    // Exception table:
    //   from	to	target	type
    //   9	14	17	java/lang/Exception
    //   18	60	63	java/lang/Exception
    //   72	101	104	java/lang/Exception
    //   113	245	248	java/lang/Exception
    //   318	345	348	java/lang/Exception
    //   370	454	457	java/lang/Exception
    //   458	474	477	java/lang/Exception
    //   478	494	497	java/lang/Exception
    //   553	588	591	java/lang/Exception
    //   592	652	655	java/lang/Exception
    //   656	673	676	java/lang/Exception
    //   677	694	697	java/lang/Exception
  }
  
  public void kill_stor_files(Vector v) {
    while (v.size() > 0) {
      STOR_handler sf = v.remove(0);
      sf.die_now = true;
      if (sf.thisThread != null)
        sf.thisThread.interrupt(); 
      try {
        if (sf.data_is != null)
          sf.data_is.close(); 
      } catch (IOException e) {
        Log.log("SERVER", 1, e);
      } 
    } 
  }
  
  public void kill_retr_files(Vector v) {
    while (v.size() > 0) {
      RETR_handler rf = v.remove(0);
      rf.die_now = true;
      if (rf.thisThread != null)
        rf.thisThread.interrupt(); 
    } 
  }
  
  public void log_pauses() {
    add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + "_" + uiSG("sock_port") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] *" + (uiBG("pause_now") ? (LOC.G("Paused") + ".*") : (LOC.G("Unpaused") + ".*")), "PAUSE_RESUME");
  }
  
  public boolean check_access_privs(String the_dir, String command) throws Exception {
    try {
      Properties item = this.uVFS.get_fake_item(the_dir, "FILE");
      return check_access_privs(the_dir, command, item);
    } catch (Exception e) {
      Log.log("ACCESS", 2, e);
      return false;
    } 
  }
  
  public boolean check_access_privs(String the_dir, String command, Properties item) throws Exception {
    if (the_dir.indexOf(":filetree") >= 0) {
      the_dir = the_dir.substring(0, the_dir.indexOf(":filetree"));
      if (item == null)
        item = this.uVFS.get_item(the_dir); 
    } 
    try {
      Properties item2 = null;
      String additionalAccess = check_access_exception(the_dir, command, item);
      Properties p = new Properties();
      p.put("command", command);
      p.put("the_command_data", uiSG("the_command_data"));
      if (item != null)
        p.put("item", item); 
      if ((item == null && command.equals("MKD")) || command.equals("XMKD")) {
        if (the_dir.equals(SG("root_dir"))) {
          item2 = this.uVFS.get_item(the_dir);
        } else {
          item2 = this.uVFS.get_item_parent(the_dir);
        } 
        if (item2 != null)
          p.put("item", item2); 
      } 
      p.put("the_dir", the_dir);
      p.put("additionalAccess", additionalAccess);
      runPlugin("access", p);
      additionalAccess = p.getProperty("additionalAccess", additionalAccess);
      command = p.getProperty("command", command);
      the_dir = p.getProperty("the_dir", the_dir);
      if (p.get("item") != null && item2 == null)
        item = (Properties)p.get("item"); 
      String last_dir = uiSG("last_priv_dir");
      uiPUT("last_priv_dir", the_dir);
      String privs = (item == null) ? "" : (String.valueOf(item.getProperty("privs", "")) + additionalAccess);
      Properties combinedPermissions = this.uVFS.getCombinedPermissions();
      boolean aclPermissions = combinedPermissions.getProperty("acl_permissions", "false").equals("true");
      if (aclPermissions) {
        if (item == null)
          item = this.uVFS.get_item(Common.all_but_last(the_dir)); 
        privs = this.uVFS.getPriv(the_dir, item);
      } 
      Pattern pattern = null;
      String block_access = SG("block_access").trim();
      block_access = String.valueOf(block_access) + "\r\n";
      block_access = String.valueOf(block_access) + ServerStatus.SG("block_access").trim();
      block_access = block_access.trim();
      BufferedReader br = new BufferedReader(new StringReader(block_access));
      String searchPattern = "";
      while ((searchPattern = br.readLine()) != null) {
        searchPattern = searchPattern.trim();
        try {
          pattern = null;
          pattern = Pattern.compile(searchPattern);
        } catch (Exception e) {
          Log.log("SERVER", 1, e);
        } 
        if (!searchPattern.startsWith("~") && pattern != null && pattern.matcher(the_dir).matches())
          return false; 
        if (searchPattern.startsWith("~") && Common.do_search(searchPattern.substring(1), the_dir, false, 0))
          return false; 
      } 
      Properties metaInfo = new Properties();
      boolean locked = false;
      if (ServerStatus.BG("v8_beta") && item != null) {
        metaInfo = PreviewWorker.getMetaInfo(PreviewWorker.getDestPath2(String.valueOf(item.getProperty("url")) + "/p1/"));
        locked = (!metaInfo.getProperty("crushftp_locked_user", "").equals("") && !metaInfo.getProperty("crushftp_locked_user", "").equalsIgnoreCase(uiSG("user_name")));
      } 
      if (command.equals("WWW")) {
        if (privs.indexOf("(www)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("CWD"))
        if (the_dir.equals("/"))
          return true;  
      if (command.equals("CWD")) {
        if (item != null)
          return true; 
        return false;
      } 
      if (command.equals("RETR") && uiLG("start_resume_loc") > 0L) {
        if (SG("allow_locked_download").equalsIgnoreCase("true") && locked)
          locked = false; 
        if (!locked && privs.indexOf("(resume)") >= 0 && privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("RETR")) {
        if (SG("allow_locked_download").equalsIgnoreCase("true") && locked)
          locked = false; 
        if (!locked && (privs == null || privs.indexOf("(read)") < 0) && item != null && item.getProperty("size", "0").equals("0"))
          return true; 
      } 
      if (command.equals("RETR")) {
        if (SG("allow_locked_download").equalsIgnoreCase("true") && locked)
          locked = false; 
        if (!locked && privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("DELE")) {
        String privs2 = getLockedPrivs(privs, the_dir);
        if (!locked && privs.indexOf("(delete)") >= 0 && (privs2.indexOf("(inherited)") >= 0 || privs2.indexOf("(locked)") < 0 || this.uVFS.getCombinedPermissions().getProperty("acl_permissions", "false").equals("true")))
          return true; 
        return false;
      } 
      if (command.equals("RNFR"))
        if (!locked && privs.indexOf("(rename)") < 0 && item != null && item.getProperty("name").toUpperCase().startsWith("NEW FOLDER"))
          return true;  
      if (command.equals("RNFR")) {
        String privs2 = getLockedPrivs(privs, the_dir);
        if (!locked && privs.indexOf("(rename)") >= 0 && (privs2.indexOf("(inherited)") >= 0 || privs2.indexOf("(locked)") < 0 || this.uVFS.getCombinedPermissions().getProperty("acl_permissions", "false").equals("true")))
          return true; 
        return false;
      } 
      if (command.equals("STOR") && uiLG("start_resume_loc") > 0L && item != null) {
        String privs2 = getLockedPrivs(privs, the_dir);
        if (!locked && privs.indexOf("(resume)") >= 0 && privs.indexOf("(write)") >= 0 && (privs2.indexOf("(inherited)") >= 0 || privs2.indexOf("(locked)") < 0))
          return true; 
        return false;
      } 
      if (command.equals("APPE") && item != null) {
        String privs2 = getLockedPrivs(privs, the_dir);
        if (!locked && privs.indexOf("(resume)") >= 0 && privs.indexOf("(write)") >= 0 && (privs2.indexOf("(inherited)") >= 0 || privs2.indexOf("(locked)") < 0))
          return true; 
        return false;
      } 
      if (command.equals("SIZE")) {
        if (privs.indexOf("(view)") >= 0 || privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("MDTM")) {
        if (privs.indexOf("(view)") >= 0 || privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("STAT")) {
        if (privs.indexOf("(view)") >= 0 || privs.indexOf("(read)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("MLSD") || command.equals("MLST")) {
        if (privs.indexOf("(view)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("LIST") || command.equals("NLST")) {
        if (privs.indexOf("(view)") >= 0)
          return true; 
        return false;
      } 
      if (command.equals("SHARE")) {
        if (privs.indexOf("(share)") >= 0)
          return true; 
        return false;
      } 
      if (the_dir.equals(SG("root_dir"))) {
        item = this.uVFS.get_item(the_dir);
      } else {
        item = this.uVFS.get_item_parent(the_dir);
      } 
      privs = (item == null) ? "" : (String.valueOf(item.getProperty("privs", "")) + additionalAccess);
      if (command.equals("STOR") || command.equals("APPE") || command.equals("STOU")) {
        String privs2 = getLockedPrivs(privs, String.valueOf(the_dir) + "check_locked.txt");
        if (!locked && privs.indexOf("(write)") >= 0 && (privs2.indexOf("(inherited)") >= 0 || privs2.indexOf("(locked)") < 0))
          return true; 
        return false;
      } 
      if (command.equals("MKD") || command.equals("XMKD")) {
        String privs2 = getLockedPrivs(privs, the_dir);
        if (privs.indexOf("(makedir)") >= 0 && (privs2.indexOf("(inherited)") >= 0 || privs2.indexOf("(locked)") < 0))
          return true; 
        return false;
      } 
      if (command.equals("RMD") || command.equals("XRMD")) {
        String privs2 = getLockedPrivs(privs, the_dir);
        if (privs.indexOf("(deletedir)") >= 0 && (privs2.indexOf("(inherited)") >= 0 || privs2.indexOf("(locked)") < 0 || this.uVFS.getCombinedPermissions().getProperty("acl_permissions", "false").equals("true")))
          return true; 
        return false;
      } 
      if (command.equals("RNTO"))
        if (privs.indexOf("(rename)") >= 0 && Common.all_but_last(last_dir).equals(Common.all_but_last(the_dir)))
          return true;  
      if (command.equals("RNTO")) {
        if (privs.indexOf("(write)") >= 0 && (privs.indexOf("(inherited)") >= 0 || privs.indexOf("(locked)") < 0))
          return true; 
        return false;
      } 
      return false;
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
      Log.log("ACCESS", 1, e);
      return false;
    } 
  }
  
  public String getLockedPrivs(String privs, String the_dir) throws Exception {
    if (privs.indexOf("(locked)") >= 0) {
      Properties item_tmp = this.uVFS.get_item(Common.all_but_last(the_dir));
      privs = (item_tmp == null) ? "" : item_tmp.getProperty("privs", "");
    } 
    return privs;
  }
  
  public String check_access_exception(String the_dir, String command, Properties item) throws Exception {
    String original_the_dir = the_dir;
    if (command.equals("RNTO"))
      the_dir = this.rnfr_file_path; 
    if (this.accessExceptions.get(the_dir) == null)
      return ""; 
    Properties master_item = (Properties)this.accessExceptions.get(the_dir);
    if (item != null && master_item.getProperty("modified", "-1").equals(item.getProperty("modified", "-2"))) {
      if (command.equals("RNTO")) {
        this.accessExceptions.remove(the_dir);
        this.accessExceptions.put(original_the_dir, master_item);
      } 
      return "(read)(rename)(delete)";
    } 
    return "";
  }
  
  public long get_quota(String the_dir) throws Exception {
    return get_quota(the_dir, this.uVFS, SG("parent_quota_dir"), this.quotaDelta, this, true);
  }
  
  public static long get_quota(String the_dir, VFS uVFS, String parentQuotaDir, Properties quotaDelta, SessionCrush thisSession, boolean available) throws Exception {
    try {
      Log.log("QUOTA", 3, "get_quota the_dir:" + the_dir + ", parentQuotaDir:" + parentQuotaDir + ", quotaDelta:" + quotaDelta);
      Properties item = uVFS.get_item(uVFS.getPrivPath(the_dir));
      Log.log("QUOTA", 3, "get_quota item:" + item);
      if (item.getProperty("privs", "").indexOf("(quota") >= 0) {
        long totalQuota = get_total_quota(the_dir, uVFS, quotaDelta);
        Log.log("QUOTA", 3, "get_quota totalQuota:" + totalQuota);
        if (item.getProperty("privs", "").indexOf("(real_quota)") >= 0) {
          long used = get_quota_used(the_dir, uVFS, parentQuotaDir, thisSession);
          if (available)
            totalQuota -= used; 
          Log.log("QUOTA", 3, "get_quota_used:" + used);
          Log.log("QUOTA", 3, "get_quota_used totalQuota:" + totalQuota);
        } 
        return totalQuota;
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    return -12345L;
  }
  
  public long get_quota_used(String the_dir) throws Exception {
    return get_quota_used(the_dir, this.uVFS, SG("parent_quota_dir"), this);
  }
  
  public static long get_quota_used(String the_dir, VFS uVFS, String parentQuotaDir, SessionCrush thisSession) throws Exception {
    try {
      Properties item = uVFS.get_item(uVFS.getPrivPath(the_dir));
      if (item.getProperty("privs", "").indexOf("(quota") >= 0) {
        if (item.getProperty("privs", "").indexOf("(real_quota)") >= 0) {
          String parentAddon = parentQuotaDir;
          if (parentAddon.equals("parent_quota_dir"))
            parentAddon = ""; 
          String real_path = String.valueOf((new VRL(item.getProperty("url"))).getCanonicalPath()) + "/" + parentAddon;
          long size = -12345L;
          if (VFS.quotaCache.containsKey(real_path.toUpperCase())) {
            Properties p = (Properties)VFS.quotaCache.get(real_path.toUpperCase());
            if (Long.parseLong(p.getProperty("time")) < (new Date()).getTime() - 300000L) {
              VFS.quotaCache.remove(real_path.toUpperCase());
            } else {
              size = Long.parseLong(p.getProperty("size"));
            } 
          } 
          if (size == -12345L) {
            while (VFS.activeQuotaChecks.size() > Integer.parseInt(System.getProperty("crushftp.quotathreads", "5")))
              Thread.sleep(100L); 
            while (VFS.activeQuotaChecks.indexOf(real_path) >= 0)
              Thread.sleep(100L); 
            if (VFS.quotaCache.containsKey(real_path.toUpperCase())) {
              Properties p = (Properties)VFS.quotaCache.get(real_path.toUpperCase());
              if (Long.parseLong(p.getProperty("time")) < (new Date()).getTime() - 300000L) {
                VFS.quotaCache.remove(real_path.toUpperCase());
              } else {
                size = Long.parseLong(p.getProperty("size"));
              } 
            } 
            if (size == -12345L)
              try {
                VFS.activeQuotaChecks.addElement(real_path);
                Properties qp = new Properties();
                qp.put("realPath", real_path);
                if (thisSession != null)
                  thisSession.runPlugin("getUsedQuota", qp); 
                if (!qp.getProperty("usedQuota", "").equals(""))
                  size = Long.parseLong(qp.getProperty("usedQuota", "0")); 
                if (size == -12345L)
                  size = Common.recurseSize(real_path, 0L, thisSession); 
                Properties p = new Properties();
                p.put("time", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                p.put("size", (new StringBuffer(String.valueOf(size))).toString());
                VFS.quotaCache.put(real_path.toUpperCase(), p);
              } finally {
                VFS.activeQuotaChecks.remove(real_path);
              }  
          } 
          return size;
        } 
        return -12345L;
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    return -12345L;
  }
  
  public long get_total_quota(String the_dir) throws Exception {
    return get_total_quota(the_dir, this.uVFS, this.quotaDelta);
  }
  
  public static long get_total_quota(String the_dir, VFS uVFS, Properties quotaDelta) throws Exception {
    try {
      Properties item = uVFS.get_item(uVFS.getPrivPath(the_dir));
      if (item.getProperty("privs", "").indexOf("(quota") >= 0) {
        String data = item.getProperty("privs", "");
        data = data.substring(data.indexOf("(quota") + 6, data.indexOf(")", data.indexOf("(quota")));
        quotaDelta.put(the_dir, data);
        return Long.parseLong(data);
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
    return -12345L;
  }
  
  public void set_quota(String the_dir, long quota_val) throws Exception {
    synchronized (set_quota_lock) {
      try {
        UserTools.loadPermissions(this.uVFS);
        Properties item = this.uVFS.get_item(this.uVFS.getPrivPath(the_dir));
        if (item.getProperty("privs", "").indexOf("(quota") >= 0 && item.getProperty("privs", "").indexOf("(real_quota)") < 0) {
          long originalQuota = Long.parseLong(this.quotaDelta.getProperty(the_dir));
          long quotaDiff = originalQuota - quota_val;
          String data = item.getProperty("privs", "");
          data = data.substring(data.indexOf("(quota") + 6, data.indexOf(")", data.indexOf("(quota")));
          data = Common.replace_str(item.getProperty("privs", ""), data, (new StringBuffer(String.valueOf(originalQuota - quotaDiff))).toString());
          item.put("privs", data);
          String privPath = this.uVFS.getPrivPath(the_dir);
          UserTools.addPriv(uiSG("listen_ip_port"), uiSG("user_name"), privPath, data, Integer.parseInt(this.uVFS.getPrivPath(the_dir, true, true)), this.uVFS);
          this.uVFS.reset();
          Properties p = new Properties();
          p.put("permissions", this.uVFS.getCombinedPermissions());
          runPlugin("quotaUpdate", p);
        } else if (item.getProperty("privs", "").indexOf("(quota") >= 0 && item.getProperty("privs", "").indexOf("(real_quota)") >= 0) {
          String data = item.getProperty("privs", "");
          data = data.substring(data.indexOf("(quota") + 6, data.indexOf(")", data.indexOf("(quota")));
          String parentAddon = SG("parent_quota_dir");
          if (parentAddon.equals("parent_quota_dir"))
            parentAddon = ""; 
          String real_path = String.valueOf((new VRL(item.getProperty("url"))).getCanonicalPath()) + "/" + parentAddon;
          if (VFS.quotaCache.containsKey(real_path.toUpperCase())) {
            Properties p = (Properties)VFS.quotaCache.get(real_path.toUpperCase());
            p.put("size", (new StringBuffer(String.valueOf(Long.parseLong(data) - quota_val))).toString());
          } 
        } 
      } catch (Exception e) {
        if (e.indexOf("Interrupted") >= 0)
          throw e; 
      } 
    } 
  }
  
  public void add_log_formatted(String log_data, String check_data) {
    add_log_formatted(log_data, check_data, "");
  }
  
  public void add_log_formatted(String log_data, String check_data, String uid) {
    if (uiBG("dont_log"))
      return; 
    check_data = String.valueOf(check_data) + " ";
    if (!check_data.trim().equals("DIR_LIST") && !log_data.trim().startsWith("RETR END") && !log_data.trim().startsWith("STOR END")) {
      Properties p = new Properties();
      p.put("the_command", check_data.substring(0, check_data.indexOf(" ")));
      p.put("user_time", ServerStatus.thisObj.logDateFormat.format(new Date()));
      String command_data = uiSG("the_command_data");
      if (uiSG("the_command").toUpperCase().equals("PASS"))
        command_data = "**************"; 
      p.put("stamp", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    } 
    if (check_data.trim().equals("DIR_LIST")) {
      add_log(log_data, check_data.trim());
    } else {
      add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + "_" + uiSG("sock_port") + uid + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("READ") + ": *" + log_data + "*", check_data.trim());
    } 
  }
  
  public void add_log(String log_data, String check_data) {
    add_log(log_data, check_data, check_data);
  }
  
  public void add_log(String log_data, String short_data, String check_data) {
    if (uiBG("dont_log"))
      return; 
    if (log_data.indexOf("WROTE: *220-") < 0 && log_data.indexOf("WROTE: *230-") < 0) {
      log_data = String.valueOf(log_data.trim()) + "\r\n";
      BufferedReader lines = new BufferedReader(new StringReader(log_data));
      String data = "";
      try {
        while ((data = lines.readLine()) != null) {
          if (check_data.equals("DIR_LIST"))
            data = "[" + uiSG("user_number") + "_" + uiSG("sock_port") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] WROTE: " + data; 
          if (check_data.equals("PROXY"))
            data = "[" + uiSG("user_number") + "_" + uiSG("sock_port") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] : " + data; 
          ServerStatus.thisObj.append_log(String.valueOf(ServerStatus.thisObj.logDateFormat.format(new Date())) + "|" + data + "\r\n", check_data);
          if (ServerStatus.BG("write_session_logs"))
            uiVG("user_log").addElement("SESSION|" + ServerStatus.thisObj.logDateFormat.format(new Date()) + "|" + data); 
        } 
      } catch (IOException iOException) {}
    } 
    drain_log();
  }
  
  public void drain_log() {
    if (!ServerStatus.BG("write_session_logs"))
      return; 
    synchronized (SharedSession.sessionLock) {
      synchronized (uiVG("user_log")) {
        while (uiVG("user_log").size() > 0) {
          StringBuffer sb = new StringBuffer();
          int loops = 0;
          while (uiVG("user_log").size() > 0 && loops++ < 1000) {
            try {
              sb.append(uiVG("user_log").remove(0).toString()).append("\r\n");
            } catch (Exception exception) {}
          } 
          try {
            Common.copyStreams(new ByteArrayInputStream(sb.toString().getBytes("UTF8")), new FileOutputStream(new File(String.valueOf(uiSG("user_log_path")) + uiSG("user_log_file")), true), true, true);
          } catch (IOException e) {
            Log.log("SERVER", 1, e);
          } 
        } 
      } 
    } 
  }
  
  public Properties do_event5(String type, Properties fileItem1) {
    return do_event5(type, fileItem1, null);
  }
  
  public Properties do_event5(String type, Properties fileItem1, Properties fileItem2) {
    if (fileItem1 != null && fileItem1.containsKey("the_file_type"))
      fileItem1.put("type", fileItem1.getProperty("the_file_type")); 
    if (fileItem2 != null && fileItem2.containsKey("the_file_type"))
      fileItem2.put("type", fileItem2.getProperty("the_file_type")); 
    Properties info = null;
    Properties originalUser = this.user;
    try {
      if (this.user == null && type.equalsIgnoreCase("ERROR"))
        this.user = UserTools.ut.getUser(uiSG("listen_ip_port"), uiSG("user_name"), true); 
      if (this.user == null)
        return info; 
      Properties fileItem1_2 = null;
      if (fileItem1 != null)
        fileItem1_2 = (Properties)fileItem1.clone(); 
      Properties fileItem2_2 = null;
      if (fileItem2 != null)
        fileItem2_2 = (Properties)fileItem2.clone(); 
      info = ServerStatus.thisObj.events6.process(type, fileItem1_2, fileItem2_2, (Vector)this.user.get("events"), this);
      if (fileItem1_2 != null && fileItem1_2.containsKey("execute_log"))
        fileItem1.put("execute_log", fileItem1_2.get("execute_log")); 
    } finally {
      this.user = originalUser;
    } 
    this.user = originalUser;
    return info;
  }
  
  public Properties runPlugin(String action, Properties p) {
    Log.log("PLUGIN", 3, "PLUGIN:Calling " + action);
    if (p == null)
      p = new Properties(); 
    p.put("action", action);
    p.put("server_item", this.server_item);
    if (p.get("user") == null && this.user != null)
      p.put("user", this.user); 
    p.put("user_info", this.user_info);
    p.put("ServerSession", this);
    p.put("ServerSessionObject", this);
    p.put("server_settings", ServerStatus.server_settings);
    ServerStatus.thisObj.runPlugins(p, action.equals("login"));
    Log.log("PLUGIN", 3, "PLUGIN:Completed " + action);
    return p;
  }
  
  public void checkTempAccounts(Properties p) {
    try {
      if (!ServerStatus.thisObj.server_info.containsKey("knownBadTempAccounts"))
        ServerStatus.thisObj.server_info.put("knownBadTempAccounts", new Properties()); 
      Properties knownBadTempAccounts = (Properties)ServerStatus.thisObj.server_info.get("knownBadTempAccounts");
      synchronized (knownBadTempAccounts) {
        Enumeration keys = knownBadTempAccounts.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          if (System.currentTimeMillis() - Long.parseLong(knownBadTempAccounts.getProperty(key)) > (1000 * ServerStatus.IG("temp_account_bad_timeout")))
            knownBadTempAccounts.remove(key); 
        } 
        if (knownBadTempAccounts.containsKey(p.getProperty("username"))) {
          Log.log("SERVER", 2, "Ignoring temp account request for username:" + p.getProperty("username"));
          return;
        } 
      } 
      String tempAccountsPath = ServerStatus.SG("temp_accounts_path");
      File[] accounts = (new File(String.valueOf(tempAccountsPath) + "accounts/")).listFiles();
      boolean found = false;
      boolean exausted_usage = false;
      if (accounts != null) {
        for (int x = 0; !found && x < accounts.length; x++) {
          try {
            File f = accounts[x];
            Log.log("SERVER", 2, "Temp:" + f.getName());
            if (f.getName().indexOf(",,") >= 0 && f.isDirectory()) {
              String[] tokens = f.getName().split(",,");
              Properties pp = new Properties();
              for (int xx = 0; xx < tokens.length; xx++) {
                boolean skip = false;
                String key = tokens[xx].substring(0, tokens[xx].indexOf("="));
                String val = tokens[xx].substring(tokens[xx].indexOf("=") + 1);
                if (key.equals("C")) {
                  key = val.split("=")[0];
                  if ((val.split("=")).length > 1) {
                    val = val.split("=")[1];
                  } else {
                    val = "";
                  } 
                  Vector v = (Vector)pp.get("web_customizations");
                  if (v == null)
                    v = new Vector(); 
                  Properties ppp = new Properties();
                  ppp.put("key", key);
                  ppp.put("value", val);
                  v.addElement(ppp);
                  pp.put("web_customizations", v);
                  skip = true;
                } 
                if (!skip)
                  pp.put(key.toUpperCase(), val); 
              } 
              if (!pp.getProperty("I", "").equals("") && pp.getProperty("U").equalsIgnoreCase(p.getProperty("username")) && pp.getProperty("P").equalsIgnoreCase(p.getProperty("password"))) {
                File f2 = f;
                int i = Integer.parseInt(pp.getProperty("I")) - 1;
                if (i < 0) {
                  exausted_usage = true;
                } else {
                  f2 = new File(f.getPath().replaceAll(",,i=" + (i + 1), ",,i=" + i));
                  f.renameTo(f2);
                  f = f2;
                } 
              } 
              if (ServerStatus.thisObj.common_code.check_date_expired_roll(pp.getProperty("EX"))) {
                if (!ServerStatus.SG("temp_accounts_account_expire_task").equals("")) {
                  Vector items = new Vector();
                  Properties item = new Properties();
                  Properties info = (Properties)Common.readXMLObject(String.valueOf(f.getPath()) + "/INFO.XML");
                  item.putAll(info);
                  item.putAll(pp);
                  item.put("url", f.toURI().toURL().toExternalForm());
                  item.put("the_file_name", f.getName());
                  item.put("the_file_path", "/");
                  item.put("account_path", String.valueOf(f.getCanonicalPath().replace('\\', '/')) + "/");
                  item.put("storage_path", String.valueOf((new File(String.valueOf(f.getCanonicalPath()) + "/../../storage/" + pp.getProperty("U") + pp.getProperty("P"))).getCanonicalPath().replace('\\', '/')) + "/");
                  item.put("the_file_size", (new StringBuffer(String.valueOf(f.length()))).toString());
                  item.put("type", f.isDirectory() ? "DIR" : "FILE");
                  items.addElement(item);
                  Properties event = new Properties();
                  event.put("event_plugin_list", ServerStatus.SG("temp_accounts_account_expire_task"));
                  event.put("name", "TempAccountEvent:" + pp.getProperty("U"));
                  ServerStatus.thisObj.events6.doEventPlugin(null, event, null, items);
                } 
                Common.recurseDelete(String.valueOf(f.getCanonicalPath()) + "/../../storage/" + pp.getProperty("U") + pp.getProperty("P"), false);
                Common.recurseDelete(f.getCanonicalPath(), false);
              } else if (p.getProperty("username").equalsIgnoreCase(pp.getProperty("U")) && (p.getProperty("password").equalsIgnoreCase(pp.getProperty("P")) || p.getProperty("anyPass").equals("true"))) {
                Properties tempUser = UserTools.ut.getUser(uiSG("listen_ip_port"), pp.getProperty("T"), true);
                tempUser.put("username", p.getProperty("username"));
                tempUser.put("password", p.getProperty("password"));
                tempUser.put("account_expire", pp.getProperty("EX"));
                Properties u = (Properties)p.get("user");
                Properties info = (Properties)Common.readXMLObject(String.valueOf(f.getPath()) + "/INFO.XML");
                info.remove("command");
                info.remove("type");
                u.putAll(tempUser);
                u.putAll(pp);
                u.putAll(info);
                Vector events = (Vector)u.get("events");
                if (events != null)
                  for (int i = 0; i < events.size(); i++) {
                    Properties event = events.elementAt(i);
                    if (event.getProperty("resolveShareEvent", "false").equals("true"))
                      if (event.getProperty("linkUser") != null) {
                        Properties linkUser = UserTools.ut.getUser(this.server_item.getProperty("linkedServer", ""), event.getProperty("linkUser"), true);
                        Vector events2 = null;
                        if (linkUser == null) {
                          events2 = (Vector)Common.CLONE(events);
                        } else {
                          events2 = (Vector)linkUser.get("events");
                        } 
                        for (int xxx = 0; events2 != null && xxx < events2.size(); xxx++) {
                          Properties event2 = events2.elementAt(xxx);
                          if (event2.getProperty("name", "").equals(event.getProperty("linkEvent", ""))) {
                            event.putAll((Properties)event2.clone());
                            String event_user_action_list = ")" + event.getProperty("event_user_action_list", "") + "(";
                            String[] parts = event_user_action_list.split("\\)\\(");
                            String new_event_user_action_list = "";
                            for (int xxxx = 0; xxxx < parts.length; xxxx++) {
                              if (parts[xxxx].startsWith("share_"))
                                new_event_user_action_list = String.valueOf(new_event_user_action_list) + "(" + parts[xxxx].substring("share_".length()) + ")"; 
                            } 
                            event.put("event_user_action_list", new_event_user_action_list);
                            break;
                          } 
                        } 
                      }  
                  }  
                UserTools.mergeWebCustomizations(u, pp);
                UserTools.mergeWebCustomizations(u, info);
                p.remove("permissions");
                p.put("virtual", XMLUsers.buildVFSXML(String.valueOf(f.getPath()) + "/"));
                p.put("action", "success");
                p.put("overwrite_permissions", "false");
                if (exausted_usage) {
                  String fname = "invalid_link.html";
                  String buildPrivs = "(read)(view)";
                  Properties permissions = new Properties();
                  permissions.put("/", buildPrivs);
                  Properties dir_item = new Properties();
                  dir_item.put("url", (new File(String.valueOf(System.getProperty("crushftp.web", "")) + "WebInterface/" + fname)).toURI().toURL().toExternalForm());
                  dir_item.put("type", "file");
                  Vector v = new Vector();
                  v.addElement(dir_item);
                  Properties virtual = UserTools.generateEmptyVirtual();
                  String path = "/" + fname;
                  if (path.endsWith("/"))
                    path = path.substring(0, path.length() - 1); 
                  Properties vItem = new Properties();
                  vItem.put("virtualPath", path);
                  vItem.put("name", fname);
                  vItem.put("type", "FILE");
                  vItem.put("vItems", v);
                  virtual.put(path, vItem);
                  vItem = new Properties();
                  vItem.put("name", "VFS");
                  vItem.put("type", "DIR");
                  vItem.put("virtualPath", "/");
                  virtual.put("/", vItem);
                  p.put("virtual", virtual);
                  Vector web_customizations = (Vector)u.get("web_customizations");
                  if (web_customizations == null)
                    web_customizations = new Vector(); 
                  Properties replaceListingWithPage = new Properties();
                  replaceListingWithPage.put("key", "replaceListingWithPage");
                  replaceListingWithPage.put("value", "invalid_link.html");
                  web_customizations.addElement(replaceListingWithPage);
                  u.put("web_customizations", web_customizations);
                } 
                found = true;
              } 
            } 
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
          } 
        } 
        if (!found && !p.getProperty("password", "").equals(""))
          knownBadTempAccounts.put(p.getProperty("username"), (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()); 
      } 
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
    } 
  }
  
  public static SimpleDateFormat updateDateCustomizations(SimpleDateFormat date_time, Properties user) {
    Vector customizations = (Vector)user.get("web_customizations");
    if (customizations == null)
      customizations = new Vector(); 
    String date = "";
    String time = "";
    for (int x = 0; x < customizations.size(); x++) {
      Properties pp = customizations.elementAt(x);
      String key = pp.getProperty("key");
      if (key.equalsIgnoreCase("DATE_FORMAT_TEXT"))
        date = pp.getProperty("value"); 
      if (key.equalsIgnoreCase("TIME_FORMAT_TEXT"))
        time = pp.getProperty("value"); 
    } 
    if (date.length() != 0 || time.length() != 0)
      date_time = new SimpleDateFormat(String.valueOf(date) + " " + time, Locale.US); 
    return date_time;
  }
  
  public boolean verify_user(String theUser, String thePass) {
    return verify_user(theUser, thePass, false, true);
  }
  
  public boolean verify_user(String theUser, String thePass, boolean anyPass) {
    return verify_user(theUser, thePass, anyPass, true);
  }
  
  public SessionCrush(Socket sock, int user_number, String user_ip, int listen_port, String listen_ip, String listen_ip_port, Properties server_item) {
    this.shareVFS = false;
    this.allow_replication = false;
    if (sock != null) {
      this.session_socks.addElement(sock);
      uiPUT("sock_port", (new StringBuffer(String.valueOf(sock.getPort()))).toString());
    } 
    this.server_item = server_item;
    try {
      uiPUT("user_log_path", ServerStatus.change_vars_to_values_static(ServerStatus.SG("user_log_location"), null, null, null));
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    uiPUT("session", this);
    uiPUT("id", (new StringBuffer(String.valueOf(user_number))).toString());
    uiPUT("user_number", (new StringBuffer(String.valueOf(user_number))).toString());
    uiPUT("listen_ip_port", server_item.getProperty("linkedServer", ""));
    uiPUT("listen_ip", listen_ip);
    uiPUT("bind_port", server_item.getProperty("port"));
    String real_bind_ip = "0.0.0.0";
    if (sock != null && sock instanceof SSHSocket) {
      real_bind_ip = ((SSHSocket)sock).sockIn.getLocalAddress().getHostAddress();
    } else if (sock != null) {
      real_bind_ip = sock.getLocalAddress().getHostAddress();
    } 
    uiPUT("bind_ip", real_bind_ip);
    uiPUT("bind_ip_config", server_item.getProperty("ip", listen_ip));
    uiPUT("user_ip", user_ip);
    uiPUT("user_protocol", server_item.getProperty("serverType", "ftp"));
    uiPUT("user_protocol_proxy", server_item.getProperty("serverType", "ftp"));
    uiPUT("user_port", (sock == null) ? "0" : (new StringBuffer(String.valueOf(sock.getPort()))).toString());
    uiPUT("user_name", "");
    uiPUT("current_password", "");
    uiPUT("the_command", "");
    uiPUT("the_command_data", "");
    uiPUT("current_dir", "/");
    uiPUT("user_logged_in", "false");
    uiPUT("user_log", new Vector());
    uiPUT("user_log_file", "session_" + this.user_info.getProperty("user_protocol") + "_" + user_number + ".log");
    uiPUT("failed_commands", new Vector());
    uiPUT("refresh_user", "false");
    uiPUT("stat", new Properties());
    uiPUT("password_expired", "false");
    uiPUT("password_attempts", new Vector());
    uiPUT("lastUploadStats", new Vector());
    uiPUT("proxy_mode", "none");
    uiPUT("dieing", "false");
    uiPUT("pasv_connect", "false");
    uiPUT("last_logged_command", "");
    uiPUT("session_uploads", "");
    uiPUT("session_downloads", "");
    uiPUT("list_filetree_status", "");
    uiPUT("session_download_count", "0");
    uiPUT("session_upload_count", "0");
    uiPUT("list_zip_dir", "false");
    uiPUT("list_zip_file", "false");
    uiPUT("list_zip_only", "false");
    uiPUT("list_zip_app", ServerStatus.SG("list_zip_app"));
    uiPUT("list_dot", "true");
    uiPUT("zlibLevel", "8");
    uiPUT("last_file_real_path", "");
    uiPUT("last_file_name", "");
    uiPUT("login_date_stamp", "");
    uiPUT("login_date", "");
    uiPUT("login_date_formatted", "");
    uiPUT("termination_message", "");
    uiPUT("file_transfer_mode", ServerStatus.SG("file_transfer_mode"));
    uiPUT("modez", "false");
    uiPUT("dataSecure", "false");
    uiPUT("secureType", "TLS");
    uiPUT("friendly_quit", "false");
    uiPUT("randomaccess", "false");
    uiPUT("mlst_format", "Type*;Size*;Modify*;Perm*;UNIX.owner*;UNIX.group*;");
    uiPUT("last_port_string", "");
    uiPUT("last_time_remaining", "");
    uiPUT("last_action", "");
    uiPUT("crc", "");
    uiPUT("pause_now", "false");
    uiPUT("new_pass1", "");
    uiPUT("new_pass2", "");
    uiPUT("PASV_port", "2000");
    uiPUT("sending_file", "false");
    uiPUT("receiving_file", "false");
    uiPUT("listing_files", "false");
    uiPUT("dont_write", "false");
    uiPUT("dont_read", "false");
    uiPUT("dont_log", "false");
    uiPUT("didDisconnect", "false");
    uiPUT("adminAllowed", "true");
    uiPUT("sscn_mode", "false");
    uiPUT("file_length", "0");
    uiPUT("start_transfer_time", "0");
    uiPUT("end_part_transfer_time", "0");
    uiPUT("overall_transfer_speed", "0");
    uiPUT("current_transfer_speed", "0");
    uiPUT("seconds_remaining", "0");
    uiPUT("start_transfer_byte_amount", "0");
    uiPUT("bytes_sent", "0");
    uiPUT("bytes_sent_formatted", "0b");
    uiPUT("bytes_received", "0");
    uiPUT("bytes_received_formatted", "0b");
    uiPUT("ratio_bytes_sent", "0");
    uiPUT("ratio_bytes_received", "0");
    uiPUT("start_resume_loc", "0");
    uiPUT("no_zip_compression", "false");
    uiPUT("zip64", "false");
    uiPUT("secure", "false");
    uiPUT("explicit_ssl", "false");
    uiPUT("explicit_tls", "false");
    uiPUT("require_encryption", "false");
    uiPUT("login_date_stamp", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    uiPUT("login_date_stamp_unique", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
    uiPUT("login_date", (new Date()).toString());
    uiPUT("login_date_formatted", ServerStatus.thisObj.logDateFormat.format(new Date()));
    uiPUT("time", ServerStatus.thisObj.logDateFormat.format(new Date()));
    if (server_item.getProperty("serverType", "FTP").toUpperCase().equals("FTPS")) {
      uiPUT("secure", "true");
      uiPUT("dataSecure", "true");
      uiPUT("sscn_mode", "false");
    } 
    if (server_item.getProperty("explicit_ssl", "false").toUpperCase().equals("TRUE"))
      uiPUT("explicit_ssl", "true"); 
    if (server_item.getProperty("explicit_tls", "false").toUpperCase().equals("TRUE"))
      uiPUT("explicit_tls", "true"); 
    if (server_item.getProperty("require_encryption", "false").toUpperCase().equals("TRUE"))
      uiPUT("require_encryption", "true"); 
    this.sdf_yyyyMMddHHmmssGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
    this.allow_replication = true;
  }
  
  public boolean verify_user(String theUser, String thePass, boolean anyPass, boolean doAfterLogin) {
    if (theUser.startsWith("~")) {
      this.shareVFS = true;
      theUser = theUser.substring(1);
      uiPUT("user_name", theUser);
    } 
    if (theUser.toUpperCase().startsWith("$ASCII$")) {
      theUser = theUser.substring(7);
      uiPUT("user_name", theUser);
      uiPUT("proxy_ascii_binary", "ascii");
    } 
    if (ServerStatus.BG("block_hack_username_immediately") && !theUser.trim().equals("") && !theUser.trim().equalsIgnoreCase("anonymous")) {
      boolean hack = checkHackUsernames(theUser);
      if (hack)
        return false; 
    } 
    if (UserTools.checkPassword(thePass))
      anyPass = true; 
    if (theUser.equalsIgnoreCase("default"))
      return false; 
    if (theUser.equalsIgnoreCase("TempAccount"))
      return false; 
    Properties loginReason = new Properties();
    long time = Long.parseLong(ServerStatus.siPG("invalid_usernames").getProperty(theUser.toUpperCase(), "0"));
    if (time > 0L && time > (new Date()).getTime() - (ServerStatus.IG("invalid_usernames_seconds") * 1000))
      return false; 
    this.user = null;
    Properties u = new Properties();
    Properties temp_p = new Properties();
    temp_p.put("user", u);
    temp_p.put("username", theUser);
    temp_p.put("password", thePass);
    temp_p.put("anyPass", (new StringBuffer(String.valueOf(anyPass))).toString());
    checkTempAccounts(temp_p);
    String templateUser = "";
    if (!temp_p.getProperty("action", "").equalsIgnoreCase("success"))
      this.user = UserTools.ut.verify_user(ServerStatus.thisObj, theUser, thePass, uiSG("listen_ip_port"), uiIG("user_number"), uiSG("user_ip"), uiIG("user_port"), this.server_item, loginReason, anyPass); 
    if (this.user == null || theUser.equals("SSO_UNI") || theUser.equals("SSO_SAML")) {
      Properties p = temp_p;
      if (!p.getProperty("action", "").equalsIgnoreCase("success")) {
        Properties tempUser = UserTools.ut.verify_user(ServerStatus.thisObj, theUser, thePass, uiSG("listen_ip_port"), uiIG("user_number"), uiSG("user_ip"), uiIG("user_port"), this.server_item, loginReason, true);
        p.put("authenticationOnlyExists", (new StringBuffer(String.valueOf((tempUser != null)))).toString());
        p = runPlugin("login", p);
        templateUser = p.getProperty("templateUser", "");
      } 
      synchronized (ServerStatus.thisObj) {
        if (p.getProperty("action", "").equalsIgnoreCase("success") || p.getProperty("dump_xml_user", "false").equals("true")) {
          theUser = p.getProperty("username", theUser);
          if (p.getProperty("authenticationOnly", "false").equalsIgnoreCase("true")) {
            Log.log("LOGIN", 2, String.valueOf(LOC.G("Plugin authenticated user (not user manager):")) + theUser);
            this.user = UserTools.ut.verify_user(ServerStatus.thisObj, theUser, thePass, uiSG("listen_ip_port"), uiIG("user_number"), uiSG("user_ip"), uiIG("user_port"), this.server_item, loginReason, true);
            if (p.getProperty("create_local_user", "").equals("true") && this.user == null) {
              Properties temp_user = UserTools.ut.getUser(uiSG("listen_ip_port"), p.getProperty("create_local_user_template", ""), false);
              temp_user.put("password", ServerStatus.thisObj.common_code.encode_pass(Common.makeBoundary(), "SHA512"));
              temp_user.put("email", p.getProperty("email", ""));
              UserTools.writeUser(uiSG("listen_ip_port"), theUser, temp_user);
              this.user = UserTools.ut.verify_user(ServerStatus.thisObj, theUser, thePass, uiSG("listen_ip_port"), uiIG("user_number"), uiSG("user_ip"), uiIG("user_port"), this.server_item, loginReason, true);
            } 
            Vector extraLinkedVfs = (Vector)p.get("linked_vfs");
            if (extraLinkedVfs != null) {
              Vector linked_vfs = (Vector)this.user.get("linked_vfs");
              if (linked_vfs == null)
                linked_vfs = new Vector(); 
              this.user.put("linked_vfs", linked_vfs);
              linked_vfs.addAll(extraLinkedVfs);
            } 
            Properties user2 = (Properties)p.get("user");
            if (user2 != null) {
              Enumeration keys = user2.keys();
              while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                if (key != null && key.startsWith("ldap_") && this.user != null) {
                  Object val = user2.get(key);
                  if (val != null)
                    this.user.put(key, val); 
                } 
              } 
            } 
          } else {
            try {
              loginReason.put("reason", "valid user");
              this.user = u;
              Properties virtual = UserTools.generateEmptyVirtual();
              if (!p.getProperty("templateUser", "").equals("")) {
                Vector extraLinkedVfs = (Vector)p.get("linked_vfs");
                Vector ichain = new Vector();
                ichain.addElement("default");
                int x;
                for (x = 0; x < (p.getProperty("templateUser", "").split(";")).length; x++)
                  ichain.addElement(p.getProperty("templateUser", "").split(";")[x].trim()); 
                if (extraLinkedVfs != null)
                  ichain.addAll(extraLinkedVfs); 
                for (x = 0; x < ichain.size(); x++) {
                  Properties tempUser = UserTools.ut.getUser(uiSG("listen_ip_port"), ichain.elementAt(x).toString(), ServerStatus.BG("resolve_inheritance"));
                  if (tempUser != null) {
                    UserTools.mergeWebCustomizations(this.user, tempUser);
                    Enumeration keys = tempUser.keys();
                    Log.log("LOGIN", 1, String.valueOf(LOC.G("Setting templateUser's settings:")) + p.size());
                    while (keys.hasMoreElements()) {
                      String key = keys.nextElement().toString();
                      if (!key.equalsIgnoreCase("username") && !key.equalsIgnoreCase("user_name") && !key.equalsIgnoreCase("password")) {
                        if ((key.equals("max_logins") && tempUser.get(key).equals("-1")) || (
                          key.equals("email") && tempUser.getProperty(key, "").equals("")) || (
                          key.equals("first_name") && tempUser.getProperty(key, "").equals("")) || (
                          key.equals("last_name") && tempUser.getProperty(key, "").equals("")) || (
                          key.equals("account_expire") && tempUser.getProperty(key, "").equals("")))
                          continue; 
                        if (key.indexOf("expire") >= 0)
                          Log.log("SERVER", 2, "key=" + key + " val=" + tempUser.getProperty(key, "")); 
                        try {
                          this.user.put(key, tempUser.get(key));
                        } catch (Exception exception) {}
                      } 
                    } 
                  } 
                } 
                if (extraLinkedVfs != null) {
                  Vector linked_vfs = (Vector)this.user.get("linked_vfs");
                  if (linked_vfs == null)
                    linked_vfs = new Vector(); 
                  this.user.put("linked_vfs", linked_vfs);
                  linked_vfs.addAll(extraLinkedVfs);
                } 
                Properties virtual2 = null;
                for (int i = 0; i < (p.getProperty("templateUser", "").split(";")).length; i++) {
                  VFS tempVFS = UserTools.ut.getVFS(uiSG("listen_ip_port"), p.getProperty("templateUser", "").split(";")[i].trim());
                  if (virtual2 == null) {
                    virtual2 = tempVFS.homes.elementAt(0);
                  } else {
                    virtual2.putAll(tempVFS.homes.elementAt(0));
                  } 
                  try {
                    Properties permissions = (Properties)p.get("permissions");
                    Vector v = (Vector)virtual2.get("vfs_permissions_object");
                    Properties permissions2 = v.elementAt(0);
                    if (permissions2.containsKey("/") && permissions.containsKey("/") && permissions != permissions2)
                      permissions2.remove("/"); 
                    permissions.putAll(permissions2);
                    permissions2.putAll(permissions);
                  } catch (Exception e) {
                    Log.log("LOGIN", 1, e);
                  } 
                } 
                virtual = virtual2;
                this.user.put("root_dir", "/");
                Log.log("SERVER", 3, "Dump of user properties from plugin:" + this.user);
              } 
              if (p.containsKey("virtual")) {
                virtual = (Properties)p.get("virtual");
              } else {
                Vector VFSItems = (Vector)p.get("VFSItems");
                for (int x = 0; x < VFSItems.size(); x++) {
                  Properties pp = VFSItems.elementAt(x);
                  String path2 = String.valueOf(pp.getProperty("dir")) + pp.getProperty("name");
                  if (path2.endsWith("/"))
                    path2 = path2.substring(0, path2.length() - 1); 
                  Properties vItem = new Properties();
                  vItem.put("name", pp.getProperty("name"));
                  vItem.put("type", pp.getProperty("type", "FILE"));
                  vItem.put("virtualPath", path2);
                  if (pp.containsKey("data")) {
                    vItem.put("vItems", pp.get("data"));
                  } else {
                    vItem.put("vItems", new Vector());
                  } 
                  virtual.put(path2, vItem);
                } 
                if (p.getProperty("overwrite_permissions", "true").equals("true")) {
                  Properties permissions = (Properties)p.get("permissions");
                  Vector v = (Vector)virtual.get("vfs_permissions_object");
                  Properties permissions2 = v.elementAt(0);
                  if (permissions2.containsKey("/") && permissions.containsKey("/") && permissions != permissions2)
                    permissions2.remove("/"); 
                  permissions.putAll(permissions2);
                  permissions2.putAll(permissions);
                } 
              } 
              setVFS(VFS.getVFS(virtual));
            } catch (Exception e) {
              Log.log("LOGIN", 1, e);
            } 
          } 
          if (p.getProperty("dump_xml_user", "false").equals("true")) {
            this.user.remove("username");
            this.user.remove("userName");
            this.user.remove("userpass");
            this.user.remove("userPass");
            this.user.remove("virtualUser");
            this.user.remove("id");
            this.user.remove("SQL_ID");
            this.user.put("root_dir", "/");
            this.user.remove("real_path_to_user");
            this.user.remove("vfs_modified");
            this.user.remove("x_lastName");
            this.user.remove("admin_group_name");
            this.user.remove("user_name");
            this.user.remove("defaultsVersion");
            UserTools.stripUser(this.user, UserTools.ut.getUser(uiSG("listen_ip_port"), "default", false));
            UserTools.writeUser(uiSG("listen_ip_port"), theUser, this.user);
            UserTools.writeVFS(uiSG("listen_ip_port"), theUser, this.uVFS);
            return false;
          } 
        } else if (!p.getProperty("redirect_url", "").equals("") && p.getProperty("redirect_url", "").startsWith(ServerStatus.SG("http_redirect_base"))) {
          this.user_info.put("redirect_url", p.getProperty("redirect_url"));
        } 
      } 
    } 
    Log.log("LOGIN", 3, "Loggining in...");
    if (this.user != null && this.uVFS == null && this.user.getProperty("virtualUser", "false").equalsIgnoreCase("false")) {
      setVFS(UserTools.ut.getVFS(uiSG("listen_ip_port"), this.user.getProperty("username")));
      Log.log("LOGIN", 2, String.valueOf(LOC.G("Got VFS from real user:")) + this.uVFS);
    } 
    if (this.user != null) {
      Properties p = new Properties();
      p.put("user", this.user);
      if (!this.user.getProperty("username", "").equals("template"))
        theUser = this.user.getProperty("username", theUser); 
      p.put("username", theUser);
      p.put("password", thePass);
      p.put("allowLogin", "true");
      if (this.uVFS != null)
        p.put("uVFSObject", this.uVFS); 
      if (doAfterLogin)
        runPlugin("afterLogin", p); 
      if (!p.getProperty("allowLogin", "true").equals("true")) {
        this.user = null;
        setVFS(null);
        add_log(LOC.G("A plugin rejected the login. Login failed."), "USER");
        return false;
      } 
      Log.log("LOGIN", 3, "After login...");
      UserTools.setupVFSLinking(uiSG("listen_ip_port"), theUser, this.uVFS, this.user);
      this.uVFS.setUserPassIpPortProtocol(uiSG("user_name"), uiSG("current_password"), uiSG("user_ip"), uiIG("user_port"), uiSG("user_protocol"), this.user_info, this.user, this);
      if (this.uVFS != null) {
        Vector homes = this.uVFS.homes;
        for (int x = 0; x < homes.size(); x++) {
          Properties virtual = homes.elementAt(x);
          Vector permissions = (Vector)virtual.get("vfs_permissions_object");
          Properties vfs_permissions_object = permissions.elementAt(0);
          Enumeration keys = virtual.keys();
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if (!key.equals("vfs_permissions_object")) {
              String key2 = ServerStatus.change_vars_to_values_static(key, this.user, this.user_info, this);
              if (!key2.equals(key)) {
                if (vfs_permissions_object.containsKey(key.toUpperCase())) {
                  String str = vfs_permissions_object.remove(key.toUpperCase()).toString();
                  vfs_permissions_object.put(key2.toUpperCase(), str);
                } 
                if (vfs_permissions_object.containsKey(String.valueOf(key.toUpperCase()) + "/")) {
                  String str = vfs_permissions_object.remove(String.valueOf(key.toUpperCase()) + "/").toString();
                  vfs_permissions_object.put(String.valueOf(key2.toUpperCase()) + "/", str);
                } 
                Properties vItem = (Properties)virtual.remove(key);
                String val2 = vItem.getProperty("virtualPath");
                if (val2 != null)
                  vItem.put("virtualPath", ServerStatus.change_vars_to_values_static(val2, this.user, this.user_info, this)); 
                val2 = vItem.getProperty("name");
                if (val2 != null)
                  vItem.put("name", ServerStatus.change_vars_to_values_static(val2, this.user, this.user_info, this)); 
                virtual.put(key2, vItem);
              } 
            } 
          } 
        } 
      } 
      if (ServerStatus.BG("track_user_md4_hashes") && !thePass.startsWith("NTLM:")) {
        String md4_user = ServerStatus.thisObj.common_code.encode_pass(theUser, "MD4", "").substring("MD4:".length());
        String md4_pass = ServerStatus.thisObj.common_code.encode_pass(thePass, "MD4", "").substring("MD4:".length());
        Properties md4_hashes = (Properties)ServerStatus.thisObj.server_info.get("md4_hashes");
        if (md4_hashes == null)
          md4_hashes = new Properties(); 
        ServerStatus.thisObj.server_info.put("md4_hashes", md4_hashes);
        if (!md4_hashes.getProperty(md4_user, "").equals(md4_pass)) {
          md4_hashes.put(md4_user, md4_pass);
          synchronized (md4_hashes) {
            ObjectOutputStream oos = null;
            try {
              (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes2.obj")).delete();
              oos = new ObjectOutputStream(new FileOutputStream(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes2.obj"));
              oos.writeObject(md4_hashes);
              oos.flush();
              oos.close();
              oos = null;
              (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes.obj")).delete();
              (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes2.obj")).renameTo(new File(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes.obj"));
            } catch (Exception e) {
              Log.log("SERVER", 0, e);
            } finally {
              try {
                if (oos != null)
                  oos.close(); 
              } catch (Exception exception) {}
            } 
          } 
        } 
      } 
    } 
    uiPUT("current_dir", SG("root_dir"));
    if (this.user != null && this.uVFS != null && this.user.getProperty("username", "").equalsIgnoreCase("TEMPLATE")) {
      Vector listing = new Vector();
      uiPUT("user_name", uiSG("user_name").replace(':', ';'));
      this.uVFS.setUserPassIpPortProtocol(uiSG("user_name"), uiSG("current_password"), uiSG("user_ip"), uiIG("user_port"), uiSG("user_protocol"), this.user_info, this.user, this);
      if ((uiSG("user_name").equalsIgnoreCase("anonymous") || uiSG("user_name").trim().equals("")) && ServerStatus.BG("ignore_web_anonymous_proxy"))
        return false; 
      try {
        this.uVFS.getListing(listing, "/");
        if (listing.size() > 0) {
          Properties p = listing.elementAt(0);
          if (p.getProperty("type").equalsIgnoreCase("DIR")) {
            p = this.uVFS.get_item(String.valueOf(p.getProperty("root_dir")) + p.getProperty("name") + "/");
            GenericClient c = this.uVFS.getClient(p);
            try {
              if (!uiBG("skip_proxy_check")) {
                uiPUT("skip_proxy_check", "false");
                String userMessage = c.getConfig("userMessage", null);
                this.user.remove("welcome_message2");
                if (userMessage != null) {
                  String[] lines = userMessage.split("\\r\\n");
                  userMessage = "";
                  for (int x = 0; x < lines.length - 1; x++) {
                    if (lines[x].startsWith("230-user.")) {
                      String param = lines[x].substring("230-user.".length()).trim();
                      this.user.put(param.split("=")[0], param.split("=")[1]);
                    } else if (lines[x].startsWith("230-user_info.")) {
                      String param = lines[x].substring("230-user_info.".length()).trim();
                      this.user_info.put(param.split("=")[0], param.split("=")[1]);
                    } else if (lines[x].startsWith("230-")) {
                      userMessage = String.valueOf(userMessage) + lines[x].substring(4) + "\r\n";
                      if (lines[x].substring(4).startsWith("PASSWORD EXPIRATION:")) {
                        String expireDate = lines[x].substring(lines[x].indexOf(":") + 1).trim();
                        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                        SimpleDateFormat sdf2 = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
                        put("expire_password_when", sdf2.format(sdf1.parse(expireDate)));
                      } 
                    } else {
                      userMessage = String.valueOf(userMessage) + lines[x] + "\r\n";
                    } 
                  } 
                  if (!userMessage.equals(""))
                    this.user.put("welcome_message2", userMessage.trim()); 
                } 
                if (c.getConfig("default_dir", "").indexOf("/") >= 0) {
                  String defaultDir = c.getConfig("default_dir", "/");
                  if (!this.server_item.getProperty("root_directory", "/").equals("/"))
                    uiPUT("default_current_dir", this.server_item.getProperty("root_directory", "/")); 
                  if (!defaultDir.equals("/"))
                    put("default_current_dir", defaultDir); 
                  if (c.getConfig("default_pwd", "").indexOf("(unlocked)") >= 0)
                    put("default_current_dir_unlocked", "true"); 
                } 
              } 
              if (containsKey("default_current_dir"))
                uiPUT("default_current_dir", getProperty("default_current_dir")); 
            } finally {
              c = this.uVFS.releaseClient(c);
            } 
            if (Common.System2.get("crushftp.dmz.queue") != null) {
              Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
              Properties action = new Properties();
              action.put("type", "GET:USER");
              action.put("id", Common.makeBoundary());
              action.put("username", theUser);
              action.put("password", thePass);
              action.put("need_response", "true");
              queue.addElement(action);
              action = UserTools.waitResponse(action, 60);
              if (!templateUser.equals("") && (action == null || !action.containsKey("user"))) {
                action = new Properties();
                action.put("type", "GET:USER");
                action.put("id", Common.makeBoundary());
                action.put("username", templateUser);
                action.put("password", thePass);
                action.put("need_response", "true");
                queue.addElement(action);
                action = UserTools.waitResponse(action, 60);
              } 
              if (action != null && action.containsKey("user")) {
                this.user = (Properties)action.get("user");
                Vector homes = (Vector)action.get("vfs");
                Properties permission = this.uVFS.getPermission0();
                for (int x = homes.size() - 1; x >= 0; x--) {
                  Properties tempVFS = homes.elementAt(x);
                  tempVFS.remove("");
                  Vector tempPermissionHomes = (Vector)tempVFS.get("vfs_permissions_object");
                  Properties tempPermission = tempPermissionHomes.elementAt(0);
                  Enumeration keys = tempPermission.keys();
                  while (keys.hasMoreElements()) {
                    String key = keys.nextElement().toString();
                    String newKey = key;
                    if (newKey.indexOf("/", 1) > 0) {
                      if (tempVFS.size() == 3 && (homes.size() == 1 || homes.size() == 2))
                        newKey = newKey.substring(newKey.indexOf("/", 1)); 
                      permission.put("/" + p.getProperty("name").toUpperCase() + newKey, tempPermission.getProperty(key));
                      continue;
                    } 
                    if (key.equals("/") && tempPermission.size() == 1 && !newKey.equals("/"))
                      permission.put("/" + p.getProperty("name").toUpperCase() + newKey, tempPermission.getProperty(key)); 
                  } 
                } 
              } 
            } 
            if (ServerStatus.BG("learning_proxy")) {
              Properties temp_user = new Properties();
              temp_user.put("username", theUser);
              temp_user.put("password", ServerStatus.thisObj.common_code.encode_pass(thePass, ServerStatus.SG("password_encryption"), ""));
              temp_user.put("root_dir", "/");
              temp_user.put("userVersion", "6");
              temp_user.put("version", "1.0");
              temp_user.put("max_logins", "0");
              UserTools.writeUser(String.valueOf(uiSG("listen_ip_port")) + "_learning", theUser, temp_user);
            } 
          } 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 2, e);
        this.user_info.put("lastProxyError", (new StringBuffer(String.valueOf(e.getMessage()))).toString());
        boolean hack = checkHackUsernames(theUser);
        doErrorEvent(e);
        if (!hack && !theUser.equals("") && !theUser.equals("anonymous") && !ServerStatus.siBG("dmz_mode"))
          try {
            Properties info = new Properties();
            info.put("alert_type", "bad_login");
            info.put("alert_sub_type", "username");
            info.put("alert_timeout", "0");
            info.put("alert_max", "0");
            info.put("alert_msg", theUser);
            ServerStatus.thisObj.runAlerts("security_alert", info, this.user_info, null);
          } catch (Exception ee) {
            Log.log("BAN", 1, ee);
          }  
        return false;
      } 
    } 
    if (BG("expire_password") || SG("expire_password_when").equals("01/01/1978 12:00:00 AM"))
      try {
        String s = SG("expire_password_when");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
        Date d = null;
        try {
          d = sdf.parse(s);
        } catch (ParseException e) {
          sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
          d = sdf.parse(s);
        } 
        uiPUT("password_expired", "false");
        if ((new Date()).getTime() > d.getTime()) {
          uiPUT("password_expired", "true");
          if (!uiSG("user_protocol").equalsIgnoreCase("SFTP")) {
            String fname = "expired.html";
            String buildPrivs = "(read)(view)";
            Properties permissions = new Properties();
            permissions.put("/", buildPrivs);
            Properties dir_item = new Properties();
            dir_item.put("url", (new File(String.valueOf(System.getProperty("crushftp.web", "")) + "WebInterface/" + fname)).toURI().toURL().toExternalForm());
            dir_item.put("type", "file");
            Vector v = new Vector();
            v.addElement(dir_item);
            Properties virtual = UserTools.generateEmptyVirtual();
            String path = "/" + fname;
            if (path.endsWith("/"))
              path = path.substring(0, path.length() - 1); 
            Properties vItem = new Properties();
            vItem.put("virtualPath", path);
            vItem.put("name", fname);
            vItem.put("type", "FILE");
            vItem.put("vItems", v);
            virtual.put(path, vItem);
            vItem = new Properties();
            vItem.put("name", "VFS");
            vItem.put("type", "DIR");
            vItem.put("virtualPath", "/");
            virtual.put("/", vItem);
            this.expired_uVFS = this.uVFS;
            setVFS(VFS.getVFS(virtual));
            Properties tempUser = UserTools.ut.getUser(uiSG("listen_ip_port"), theUser, false);
            if (uiSG("user_protocol").toUpperCase().startsWith("FTP"))
              tempUser.put("auto_set_pass", "true"); 
            if (Common.System2.get("crushftp.dmz.queue") == null)
              UserTools.writeUser(uiSG("listen_ip_port"), theUser, tempUser); 
          } 
        } 
      } catch (Exception e) {
        Log.log("LOGIN", 2, e);
      }  
    if (loginReason.getProperty("changedPassword", "").equals("true")) {
      loginReason.remove("changedPassword");
      ServerStatus.thisObj.runAlerts("password_change", this);
    } 
    if (this.user == null) {
      if (loginReason.getProperty("reason", "").equals(""))
        ServerStatus.siPG("invalid_usernames").put(theUser.toUpperCase(), (new StringBuffer(String.valueOf((new Date()).getTime()))).toString()); 
      boolean hack = checkHackUsernames(theUser);
      if (!hack && !theUser.equals("") && !theUser.equals("anonymous") && !ServerStatus.siBG("dmz_mode"))
        try {
          Properties info = new Properties();
          info.put("alert_type", "bad_login");
          info.put("alert_sub_type", "username");
          info.put("alert_timeout", "0");
          info.put("alert_max", "0");
          info.put("alert_msg", theUser);
          ServerStatus.thisObj.runAlerts("security_alert", info, this.user_info, null);
        } catch (Exception ee) {
          Log.log("BAN", 1, ee);
        }  
      return false;
    } 
    try {
      if (this.ftp != null)
        this.ftp.is = new BufferedReader(new InputStreamReader(this.ftp.sock.getInputStream(), SG("char_encoding"))); 
    } catch (Exception exception) {}
    try {
      if (this.ftp != null)
        this.ftp.os = this.ftp.sock.getOutputStream(); 
    } catch (Exception exception) {}
    Log.log("LOGIN", 3, LOC.G("Login complete."));
    if (!this.user.getProperty("failure_count_max", "0").equals("0") && !this.user.getProperty("failure_count_max", "0").equals("") && IG("failure_count") > 0)
      UserTools.ut.put_in_user(uiSG("listen_ip_port"), theUser, "failure_count", "0", true, true); 
    return true;
  }
  
  static Properties hack_cache = new Properties();
  
  public boolean checkHackUsernames(String theUser) {
    String[] hack_users = SG("hack_usernames").split(",");
    if (!hack_cache.containsKey(SG("hack_usernames")))
      hack_cache.clear(); 
    hack_cache.put(SG("hack_usernames"), "");
    Vector v = new Vector();
    for (int x = 0; x < hack_users.length; x++)
      v.addElement(hack_users[x]); 
    boolean hack = false;
    Vector v2 = new Vector();
    int i;
    for (i = 0; i < v.size(); i++) {
      if (v.elementAt(i).toString().trim().indexOf(":/") >= 0) {
        String the_url = v.elementAt(i).toString().trim();
        String r1 = "{";
        String r2 = "}";
        String addon = "";
        try {
          if (the_url.indexOf("working_dir") >= 0)
            the_url = Common.replace_str(the_url, String.valueOf(r1) + "working_dir" + addon + r2, String.valueOf((new File("./")).getCanonicalPath().replace('\\', '/')) + "/"); 
        } catch (IOException iOException) {}
        VRL vrl = new VRL(the_url);
        GenericClient c = Common.getClient(Common.getBaseUrl(vrl.toString()), "hack_username:", new Vector());
        try {
          c.login(vrl.getUsername(), vrl.getPassword(), "");
          if (hack_cache.containsKey(vrl.toString())) {
            v2.addAll((Vector)hack_cache.get(vrl.toString()));
          } else {
            Vector v3 = new Vector();
            hack_cache.put(vrl.toString(), v3);
            Properties stat = c.stat(vrl.getPath());
            if (stat != null) {
              BufferedReader br = new BufferedReader(new InputStreamReader(c.download(vrl.getPath(), 0L, -1L, true)));
              try {
                String data = "";
                while ((data = br.readLine()) != null) {
                  if (data.indexOf(",") >= 0)
                    data = data.substring(0, data.indexOf(",")); 
                  if (data.indexOf(";") >= 0)
                    data = data.substring(0, data.indexOf(";")); 
                  data = Common.replace_str(data, "\"", "");
                  v3.addElement(data.trim());
                } 
                v2.addAll(v3);
              } finally {
                br.close();
              } 
            } 
          } 
        } catch (Exception e) {
          Log.log("SERVER", 1, e);
        } 
        try {
          c.close();
        } catch (Exception exception) {}
      } 
    } 
    v.addAll(v2);
    for (i = 0; i < v.size(); i++) {
      if (theUser.trim().equalsIgnoreCase(v.elementAt(i).toString().trim())) {
        hack = true;
        ServerStatus.thisObj.ban(this.user_info, ServerStatus.IG("hban_timeout"), "hack username:" + theUser);
        ServerStatus.thisObj.kick(this.user_info);
        try {
          Properties info = new Properties();
          info.put("alert_type", "hack");
          info.put("alert_sub_type", "username");
          info.put("alert_timeout", (new StringBuffer(String.valueOf(ServerStatus.IG("hban_timeout")))).toString());
          info.put("alert_max", "0");
          info.put("alert_msg", theUser);
          ServerStatus.thisObj.runAlerts("security_alert", info, this.user_info, null);
        } catch (Exception e) {
          Log.log("BAN", 1, e);
        } 
        break;
      } 
    } 
    return hack;
  }
  
  public void doErrorEvent(Exception e) {
    Properties error_info = new Properties();
    error_info.put("the_command", uiSG("the_command"));
    error_info.put("the_command_data", uiSG("the_command_data"));
    error_info.put("url", (new StringBuffer(String.valueOf(e.toString()))).toString());
    error_info.put("the_file_status", "FAILED");
    error_info.put("the_file_error", (new StringBuffer(String.valueOf(e.toString()))).toString());
    error_info.put("the_file_name", (new StringBuffer(String.valueOf(e.toString()))).toString());
    error_info.put("the_file_path", (new StringBuffer(String.valueOf(e.toString()))).toString());
    error_info.put("the_file_start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    error_info.put("the_file_end", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    error_info.put("the_file_speed", "0");
    error_info.put("the_file_size", "0");
    error_info.put("the_file_resume_loc", "0");
    error_info.put("the_file_md5", "");
    error_info.put("modified", "0");
    if (Common.System2.get("crushftp.dmz.queue") != null) {
      try {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "PUT:ERROR_EVENT");
        action.put("id", Common.makeBoundary());
        action.put("error_info", error_info);
        action.put("error_user_info", this.user_info);
        action.put("need_response", "false");
        Properties root_item = this.uVFS.get_item(SG("root_dir"));
        GenericClient c = this.uVFS.getClient(root_item);
        action.put("crushAuth", c.getConfig("crushAuth"));
        queue.addElement(action);
        Worker.startWorker(new Runnable(this, c) {
              final SessionCrush this$0;
              
              private final GenericClient val$c;
              
              public void run() {
                try {
                  Thread.sleep(5000L);
                  this.this$0.uVFS.releaseClient(this.val$c);
                } catch (Exception exception) {}
              }
            });
        Thread.sleep(5000L);
      } catch (Exception ee) {
        Log.log("SERVER", 0, ee);
      } 
    } else {
      do_event5("ERROR", error_info);
    } 
  }
  
  public void setVFS(VFS newVFS) {
    if (this.uVFS != null)
      this.uVFS.disconnect(); 
    this.uVFS = newVFS;
  }
  
  public String getId() {
    if ((uiSG("user_protocol").startsWith("HTTP") || uiSG("user_protocol_proxy").startsWith("HTTP")) && uiSG("CrushAuth").length() > 30)
      return uiSG("CrushAuth"); 
    if (ServerStatus.BG("relaxed_event_grouping"))
      return String.valueOf(uiSG("user_protocol")) + uiSG("user_name") + uiSG("user_ip"); 
    return String.valueOf(uiSG("user_protocol")) + uiSG("user_name") + uiSG("user_ip") + uiIG("user_port") + uiIG("user_number");
  }
  
  public boolean login_user_pass() throws Exception {
    return login_user_pass(false, true);
  }
  
  public boolean login_user_pass(boolean anyPass) throws Exception {
    return login_user_pass(anyPass, true);
  }
  
  public boolean login_user_pass(boolean anyPass, boolean doAfterLogin) throws Exception {
    return login_user_pass(anyPass, doAfterLogin, uiSG("user_name"), uiSG("current_password"));
  }
  
  public boolean login_user_pass(boolean anyPass, boolean doAfterLogin, String user_name, String user_pass) throws Exception {
    if (user_name.length() > 2000 || user_pass.length() > 500) {
      this.not_done = ftp_write_command("550", "Invalid");
      doErrorEvent(new Exception(uiSG("lastLog")));
      return false;
    } 
    Log.log("LOGIN", 3, new Exception(String.valueOf(LOC.G("INFO:Logging in with user:")) + user_name));
    uiPUT("last_logged_command", "USER");
    boolean stripped_char = false;
    if (ServerStatus.BG("lowercase_usernames"))
      uiPUT("user_name", user_name.toLowerCase()); 
    if (user_name.startsWith("!")) {
      user_name = user_name.substring(1);
      uiPUT("user_name", user_name);
      stripped_char = true;
    } 
    if (this.user_info.getProperty("user_name_original", "").equals("") || this.user_info.getProperty("user_name_original", "").equalsIgnoreCase("anonymous"))
      uiPUT("user_name_original", user_name); 
    if (this.server_item.getProperty("linkedServer", "").equals("@AutoDomain"))
      if (uiSG("user_name_original").indexOf("@") > 0) {
        String newLinkedServer = uiSG("user_name_original").split("@")[(uiSG("user_name_original").split("@")).length - 1];
        String newLinkedServer2 = Common.dots(newLinkedServer);
        newLinkedServer2 = newLinkedServer2.replace('/', '-').replace('\\', '-').replace('%', '-').replace(':', '-').replace(';', '-');
        if (newLinkedServer.equals(newLinkedServer2)) {
          uiPUT("user_name", uiSG("user_name_original").substring(0, uiSG("user_name_original").lastIndexOf("@")));
          uiPUT("listen_ip_port", newLinkedServer);
        } 
      }  
    setVFS(null);
    if (verify_user(user_name, user_pass, anyPass, doAfterLogin)) {
      if (!uiSG("user_name").equals("")) {
        user_name = uiSG("user_name");
        SharedSession.find("crushftp.usernames").put(String.valueOf(Common.getPartialIp(uiSG("user_ip"))) + "_" + getId() + "_user", user_name);
      } 
      uiPUT("user_name", user_name);
      uiPUT("current_password", user_pass);
      this.uVFS.setUserPassIpPortProtocol(user_name, user_pass, uiSG("user_ip"), uiIG("user_port"), uiSG("user_protocol"), this.user_info, this.user, this);
      Log.log("LOGIN", 2, LOC.G("User $0 authenticated, VFS set to:$1", user_name, this.uVFS.toString()));
      if (ServerStatus.BG("create_home_folder"))
        try {
          Vector v = new Vector();
          this.uVFS.getListing(v, "/");
          for (int xx = 0; xx < v.size(); xx++) {
            Properties p = v.elementAt(xx);
            v.setElementAt(this.uVFS.get_item(String.valueOf(p.getProperty("root_dir")) + p.getProperty("name") + "/"), xx);
          } 
          for (int i = 0; i < v.size(); i++) {
            Properties p = v.elementAt(i);
            if (p.getProperty("url").endsWith("/") && p.getProperty("url").toUpperCase().startsWith("FILE:/")) {
              Common.verifyOSXVolumeMounted(p.getProperty("url"));
              if (!(new File((new VRL(p.getProperty("url"))).getPath())).exists())
                (new File((new VRL(p.getProperty("url"))).getPath())).mkdirs(); 
            } 
          } 
        } catch (Exception e) {
          Log.log("LOGIN", 1, e);
        }  
      setupRootDir(null, false);
      if (ServerStatus.BG("jailproxy") && getProperty("default_current_dir_unlocked", "false").equals("false"))
        uiPUT("current_dir", SG("root_dir")); 
      if (this.user.get("ip_list") != null) {
        String ips = String.valueOf(this.user.getProperty("ip_list").trim()) + "\r\n";
        ips = Common.replace_str(ips, "\r", "~");
        StringTokenizer get_em = new StringTokenizer(ips, "~");
        int num_to_do = get_em.countTokens();
        Vector ip_list = new Vector();
        try {
          for (int i = 0; i < num_to_do; i++) {
            String ip_str = get_em.nextToken().trim();
            Properties ip_data = new Properties();
            ip_data.put("type", (new StringBuffer(String.valueOf(ip_str.charAt(0)))).toString());
            ip_data.put("start_ip", ip_str.substring(1, ip_str.indexOf(",")));
            ip_data.put("stop_ip", ip_str.substring(ip_str.indexOf(",") + 1));
            ip_list.addElement(ip_data);
          } 
        } catch (Exception e) {
          if (e.indexOf("Interrupted") >= 0)
            throw e; 
        } 
        this.user.put("ip_restrictions", ip_list);
        this.user.remove("ip_list");
      } 
      boolean auto_kicked = false;
      Vector allowedHours = new Vector();
      if (SG("hours_of_day").equals("") || SG("hours_of_day").equals("hours_of_day"))
        this.user.put("hours_of_day", "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23"); 
      if (this.user.get("allowed_protocols") == null || SG("allowed_protocols").equals("allowed_protocols"))
        this.user.put("allowed_protocols", ",ftp:0,ftps:0,sftp:0,http:0,https:0,webdav:0,"); 
      String[] hours = SG("hours_of_day").split(",");
      for (int x = 0; x < hours.length; x++) {
        try {
          allowedHours.addElement((new StringBuffer(String.valueOf(Integer.parseInt(hours[x])))).toString());
        } catch (Exception e) {
          Log.log("LOGIN", 1, e);
        } 
      } 
      if (IG("max_logins_ip") != 0 && BG("logins_ip_auto_kick") && ServerStatus.count_users_ip(this, null) > IG("max_logins_ip")) {
        auto_kicked = ServerStatus.thisObj.kill_first_same_name_same_ip(this.user_info);
        Thread.sleep(5000L);
        verify_user(user_name, user_pass, false, doAfterLogin);
      } 
      if (stripped_char)
        stripped_char = ServerStatus.thisObj.kill_same_name_same_ip(this.user_info, true); 
      String fail_msg = "";
      if (IG("max_logins") < 0) {
        fail_msg = String.valueOf(LOC.G("%account_disabled%")) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("421", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (ServerStatus.siIG("concurrent_users") >= ServerStatus.IG("max_users") + 1 && !BG("ignore_max_logins")) {
        fail_msg = String.valueOf(LOC.G("%max_users_server%")) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("421", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (ServerStatus.siIG("concurrent_users") >= ServerStatus.IG("max_max_users") + 1) {
        fail_msg = String.valueOf(LOC.G("%max_max_users_server%")) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("421", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (Integer.parseInt(this.server_item.getProperty("connected_users")) > Integer.parseInt(this.server_item.getProperty("max_connected_users", "32768"))) {
        fail_msg = String.valueOf(LOC.G("%max_users_server%")) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("421", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (!Common.check_ip((Vector)this.user.get("ip_restrictions"), uiSG("user_ip"))) {
        fail_msg = LOC.G("%bad_ip%");
        this.not_done = ftp_write_command("550", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (!Common.check_day_of_week(ServerStatus.SG("day_of_week_allow"), new Date())) {
        fail_msg = String.valueOf(LOC.G("%day_restricted%")) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("530", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (Common.check_protocol(uiSG("user_protocol"), SG("allowed_protocols")) < 0) {
        fail_msg = String.valueOf(LOC.G("This user is not allowed to use this protocol.")) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("530", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (ServerStatus.count_users_ip(this, uiSG("user_protocol")) > Common.check_protocol(uiSG("user_protocol"), SG("allowed_protocols"))) {
        fail_msg = String.valueOf(LOC.G("%max_simultaneous_connections_ip%")) + " " + LOC.G("(For this protocol.)") + "\r\n" + LOC.G("Control connection closed") + ". (" + ServerStatus.count_users_ip(this, uiSG("user_protocol")) + "/" + Common.check_protocol(uiSG("user_protocol"), SG("allowed_protocols")) + ")";
        this.not_done = ftp_write_command("421", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (!Common.check_day_of_week(SG("day_of_week_allow"), new Date())) {
        fail_msg = String.valueOf(LOC.G("%user_day_restricted%")) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("530", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (allowedHours.indexOf((new StringBuffer(String.valueOf(Integer.parseInt(this.hh.format(new Date()))))).toString()) < 0) {
        fail_msg = String.valueOf(LOC.G("Not allowed to login at the present hour ($0), try later.", (new StringBuffer(String.valueOf(Integer.parseInt(this.hh.format(new Date()))))).toString())) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("530", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (IG("max_logins_ip") != 0 && ServerStatus.count_users_ip(this, null) > IG("max_logins_ip") && !auto_kicked && !stripped_char) {
        fail_msg = String.valueOf(LOC.G("%max_simultaneous_connections_ip%")) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("421", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (IG("max_logins") != 0 && ServerStatus.thisObj.count_users(this) > IG("max_logins") && !stripped_char) {
        fail_msg = String.valueOf(LOC.G("%max_simultaneous_connections%")) + "\r\n" + LOC.G("Control connection closed") + ".";
        this.not_done = ftp_write_command("421", fail_msg);
        uiPUT("user_logged_in", "false");
      } else if (ServerStatus.thisObj.common_code.check_date_expired_roll(SG("account_expire"))) {
        if (BG("account_expire_delete")) {
          try {
            UserTools.deleteUser(uiSG("listen_ip_port"), user_name);
          } catch (NullPointerException nullPointerException) {}
          fail_msg = LOC.G("%account_expired_deleted%");
          this.not_done = ftp_write_command("530", fail_msg);
          uiVG("failed_commands").addElement((new Date()).getTime());
        } else {
          fail_msg = LOC.G("%account_expired%");
          this.not_done = ftp_write_command("530", fail_msg);
          uiVG("failed_commands").addElement((new Date()).getTime());
        } 
        uiPUT("user_logged_in", "false");
      } else {
        uiPUT("user_name", user_name);
        uiPUT("current_password", user_pass);
        if (SG("account_expire") != null && !SG("account_expire").equals("") && !SG("account_expire").equals("0") && !SG("account_expire_rolling_days").equals("") && IG("account_expire_rolling_days") > 0) {
          GregorianCalendar gc = new GregorianCalendar();
          gc.setTime(new Date());
          gc.add(5, IG("account_expire_rolling_days"));
          SimpleDateFormat simpleDateFormat = null;
          if (SG("account_expire").indexOf("/") >= 0) {
            simpleDateFormat = new SimpleDateFormat("MM/dd/yy hh:mm aa", Locale.US);
          } else {
            simpleDateFormat = new SimpleDateFormat("MMddyyyyHHmm", Locale.US);
          } 
          try {
            if (simpleDateFormat.parse(SG("account_expire")).getTime() < gc.getTime().getTime()) {
              this.user.put("account_expire", simpleDateFormat.format(gc.getTime()));
              if (!ServerStatus.siBG("dmz_mode"))
                UserTools.ut.put_in_user(uiSG("listen_ip_port"), user_name, "account_expire", simpleDateFormat.format(gc.getTime()), true, true); 
              if (!ServerStatus.siBG("dmz_mode"))
                UserTools.ut.put_in_user(uiSG("listen_ip_port"), user_name, "account_expire_rolling_days", (new StringBuffer(String.valueOf(IG("account_expire_rolling_days")))).toString(), true, true); 
            } 
          } catch (Exception e) {
            return true;
          } 
        } 
        String last_logins = SG("last_logins");
        if (last_logins.equals("last_logins"))
          last_logins = ""; 
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
        last_logins = String.valueOf(sdf.format(new Date())) + "," + last_logins;
        String last_logins2 = "";
        for (int i = 0; i < (last_logins.split(",")).length && i < 10; i++) {
          if (i > 0)
            last_logins2 = String.valueOf(last_logins2) + ","; 
          last_logins2 = String.valueOf(last_logins2) + last_logins.split(",")[i];
        } 
        if (!ServerStatus.siBG("dmz_mode") && ServerStatus.BG("track_last_logins"))
          UserTools.ut.put_in_user(uiSG("listen_ip_port"), user_name, "last_logins", last_logins2, false, false); 
        try {
          if (this.ftp != null) {
            int priorTimeout = this.ftp.sock.getSoTimeout() / 1000;
            int timeout = IG("max_idle_time");
            if (timeout < 0) {
              timeout *= -1;
            } else {
              timeout *= 60;
            } 
            this.ftp.sock.setSoTimeout(((IG("max_idle_time") == 0) ? priorTimeout : timeout) * 1000);
          } 
        } catch (SocketException socketException) {}
        String login_message = "";
        if (auto_kicked)
          login_message = String.valueOf(login_message) + LOC.G("First user with same name, same IP, was autokicked.") + "\r\n"; 
        if (stripped_char)
          login_message = String.valueOf(login_message) + LOC.G("Previous sessions were kicked.") + "\r\n"; 
        ServerStatus.thisObj.set_user_pointer(this.user_info);
        try {
          String msg2 = this.server_item.getProperty("user_welcome_message", "");
          if (!this.user.getProperty("welcome_message2", "").equals(""))
            msg2 = this.user.getProperty("welcome_message2", ""); 
          login_message = String.valueOf(login_message) + msg2 + "\r\n";
          String welcome_msg = ServerStatus.thisObj.change_vars_to_values(SG("welcome_message"), this).trim();
          if (welcome_msg.equals("welcome_msg"))
            welcome_msg = ""; 
          if (welcome_msg.length() > 0)
            welcome_msg = String.valueOf(welcome_msg) + "\r\n"; 
          this.user.put("user_name", user_name);
          login_message = String.valueOf(login_message.trim()) + "\r\n" + welcome_msg + "%PASS% logged in";
          uiPUT("last_login_message", login_message);
          if (!uiBG("dont_write"))
            this.not_done = ftp_write_command("230", login_message); 
        } catch (Exception e) {
          if (e.indexOf("Interrupted") >= 0)
            throw e; 
        } 
        SimpleDateFormat date_time = updateDateCustomizations(ServerStatus.thisObj.logDateFormat, this.user);
        uiPUT("login_date_formatted", date_time.format(new Date()));
        uiPUT("user_logged_in", "true");
        uiPUT("sharedId", getId());
        if (BG("ratio_field_permanent")) {
          uiPUT("ratio_bytes_sent", (new StringBuffer(String.valueOf(IG("user_bytes_sent")))).toString());
          uiPUT("ratio_bytes_received", (new StringBuffer(String.valueOf(IG("user_bytes_received")))).toString());
        } 
        if (IG("max_login_time") != 0) {
          int max_minutes = IG("max_login_time");
          Worker.startWorker(new null.Killer(this, max_minutes), String.valueOf(Thread.currentThread().getName()) + " (max_time)");
        } 
      } 
      if (!fail_msg.equals(""))
        try {
          Properties info = new Properties();
          info.put("alert_type", "blocked");
          info.put("alert_sub_type", "username");
          info.put("alert_timeout", "0");
          info.put("alert_max", "0");
          info.put("alert_msg", String.valueOf(user_name) + ":" + fail_msg);
          ServerStatus.thisObj.runAlerts("security_alert", info, this.user_info, null);
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
        }  
      if (uiBG("user_logged_in") && doAfterLogin) {
        ServerStatus.siPUT2("successful_logins", ServerStatus.IG("successful_logins") + 1);
      } else if (doAfterLogin) {
        if (!uiSG("user_protocol").toUpperCase().startsWith("HTTP"))
          ServerStatus.siPUT2("failed_logins", ServerStatus.IG("failed_logins") + 1); 
        if (uiVG("failed_commands").size() - 10 > 0)
          Thread.sleep((1000 * (uiVG("failed_commands").size() - 10))); 
      } 
    } else {
      if (!this.user_info.getProperty("lastProxyError", "").equals("")) {
        if (ServerStatus.BG("rfc_proxy")) {
          this.not_done = ftp_write_command_raw(this.user_info.getProperty("lastProxyError", ""));
        } else {
          this.not_done = ftp_write_command("530", this.user_info.getProperty("lastProxyError", ""));
        } 
      } else if (this.server_item.getProperty("serverType", "ftp").toUpperCase().startsWith("FTP") || (!user_name.equals("") && !user_name.equalsIgnoreCase("anonymous"))) {
        this.not_done = ftp_write_command("530", "%PASS-bad%");
      } 
      uiVG("failed_commands").addElement((new Date()).getTime());
      uiPUT("user_logged_in", "false");
      if (!uiSG("user_protocol").toUpperCase().startsWith("HTTP"))
        ServerStatus.siPUT2("failed_logins", ServerStatus.IG("failed_logins") + 1); 
      uiPUT("user_logged_in", "false");
      if (uiVG("failed_commands").size() + uiVG("password_attempts").size() - 10 > 0)
        Thread.sleep((1000 * (uiVG("failed_commands").size() + uiVG("password_attempts").size() - 10))); 
    } 
    uiPUT("stat", new Properties());
    if (!uiBG("skip_proxy_check"))
      uiPUT("stat", ServerStatus.thisObj.statTools.add_login_stat(this.server_item, user_name, uiSG("user_ip"), uiBG("user_logged_in"), this)); 
    uiPUT("user_name", user_name);
    uiPUT("current_password", user_pass);
    if (uiBG("user_logged_in")) {
      active();
      return true;
    } 
    if (doAfterLogin) {
      uiVG("password_attempts").addElement((new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
      if (!uiSG("user_protocol").toUpperCase().startsWith("HTTP"))
        ServerStatus.siPUT2("failed_logins", ServerStatus.IG("failed_logins") + 1); 
      if (uiVG("failed_commands").size() + uiVG("password_attempts").size() - 10 > 0)
        Thread.sleep((1000 * (uiVG("failed_commands").size() + uiVG("password_attempts").size() - 10))); 
    } 
    doErrorEvent(new Exception(uiSG("lastLog")));
    return false;
  }
  
  public void do_Recycle(VRL vrl) throws Exception {
    if (vrl.getProtocol().equalsIgnoreCase("file")) {
      File v = new File(vrl.getCanonicalPath());
      String recycle = ServerStatus.SG("recycle_path");
      if (!recycle.startsWith("/"))
        recycle = "/" + recycle; 
      if (!recycle.endsWith("/"))
        recycle = String.valueOf(recycle) + "/"; 
      (new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_'))).mkdirs();
      String addOn = "";
      int pos = 1;
      while ((new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName() + addOn)).exists())
        addOn = (new StringBuffer(String.valueOf(pos++))).toString(); 
      if (!addOn.equals("")) {
        boolean bool = (new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName())).renameTo(new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName() + addOn));
        if (!bool) {
          Common.copy(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName(), String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName() + addOn, true);
          (new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName())).delete();
        } 
      } 
      boolean ok = v.renameTo(new File(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName()));
      if (!ok) {
        Common.copy(v.getCanonicalPath(), String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName(), true);
        v.delete();
      } 
    } 
  }
  
  public void removeCacheItem(Properties item) {
    boolean ok = true;
    for (int x = -1; x < 10 && ok; x++) {
      String tmpKey = String.valueOf(x) + item.getProperty("root_dir").substring(1) + item.getProperty("name") + uiSG("user_name");
      ok = (this.uVFS.cacheItem.remove(tmpKey) == null);
      this.uVFS.cacheItemStamp.remove(tmpKey);
    } 
  }
  
  public String do_DELE(boolean recurse, String user_dir) throws Exception {
    // Byte code:
    //   0: aload_0
    //   1: ldc_w 'the_command'
    //   4: ldc_w 'DELE'
    //   7: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   10: aload_0
    //   11: ldc_w 'last_logged_command'
    //   14: ldc_w 'DELE'
    //   17: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   20: aload_0
    //   21: aload_2
    //   22: invokevirtual fixupDir : (Ljava/lang/String;)Ljava/lang/String;
    //   25: astore_3
    //   26: aload_0
    //   27: getfield uVFS : Lcrushftp/server/VFS;
    //   30: aload_3
    //   31: iconst_0
    //   32: iconst_0
    //   33: invokevirtual getPrivPath : (Ljava/lang/String;ZZ)Ljava/lang/String;
    //   36: astore #4
    //   38: aload_0
    //   39: getfield uVFS : Lcrushftp/server/VFS;
    //   42: aload #4
    //   44: iconst_m1
    //   45: invokevirtual get_item : (Ljava/lang/String;I)Ljava/util/Properties;
    //   48: astore #5
    //   50: aload_0
    //   51: getfield uVFS : Lcrushftp/server/VFS;
    //   54: aload_3
    //   55: iconst_m1
    //   56: invokevirtual get_item : (Ljava/lang/String;I)Ljava/util/Properties;
    //   59: astore #6
    //   61: aload #6
    //   63: ifnonnull -> 226
    //   66: aload #5
    //   68: ldc_w 'privs'
    //   71: new java/lang/StringBuffer
    //   74: dup
    //   75: aload #5
    //   77: ldc_w 'privs'
    //   80: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   83: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   86: invokespecial <init> : (Ljava/lang/String;)V
    //   89: ldc_w '(inherited)'
    //   92: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   95: invokevirtual toString : ()Ljava/lang/String;
    //   98: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   101: pop
    //   102: aload_0
    //   103: aload_3
    //   104: aload_0
    //   105: ldc_w 'the_command'
    //   108: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   111: aload #5
    //   113: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;Ljava/util/Properties;)Z
    //   116: ifeq -> 154
    //   119: ldc_w 'DELETE'
    //   122: aload_3
    //   123: aconst_null
    //   124: iconst_0
    //   125: lconst_0
    //   126: lconst_0
    //   127: aload_0
    //   128: ldc_w 'root_dir'
    //   131: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   134: aload #5
    //   136: ldc_w 'privs'
    //   139: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   142: aload_0
    //   143: ldc_w 'clientid'
    //   146: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   149: ldc ''
    //   151: invokestatic trackSync : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    //   154: aload_0
    //   155: aload_0
    //   156: ldc_w '550'
    //   159: ldc_w '%DELE-not found%'
    //   162: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   165: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   168: putfield not_done : Z
    //   171: aload_0
    //   172: new java/lang/Exception
    //   175: dup
    //   176: aload_0
    //   177: ldc_w 'lastLog'
    //   180: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   183: invokespecial <init> : (Ljava/lang/String;)V
    //   186: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   189: aload_0
    //   190: ldc_w 'failed_commands'
    //   193: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   196: new java/lang/StringBuffer
    //   199: dup
    //   200: invokespecial <init> : ()V
    //   203: new java/util/Date
    //   206: dup
    //   207: invokespecial <init> : ()V
    //   210: invokevirtual getTime : ()J
    //   213: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   216: invokevirtual toString : ()Ljava/lang/String;
    //   219: invokevirtual addElement : (Ljava/lang/Object;)V
    //   222: ldc_w '%DELE-not found%'
    //   225: areturn
    //   226: aload_0
    //   227: aload_3
    //   228: aload_0
    //   229: ldc_w 'the_command'
    //   232: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   235: aload #6
    //   237: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;Ljava/util/Properties;)Z
    //   240: ifeq -> 2394
    //   243: ldc_w 'X'
    //   246: aload_3
    //   247: invokestatic last : (Ljava/lang/String;)Ljava/lang/String;
    //   250: new java/lang/StringBuffer
    //   253: dup
    //   254: ldc_w 'filename_filters_str'
    //   257: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   260: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   263: invokespecial <init> : (Ljava/lang/String;)V
    //   266: ldc '\\r\\n'
    //   268: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   271: aload_0
    //   272: ldc_w 'file_filter'
    //   275: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   278: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   281: invokevirtual toString : ()Ljava/lang/String;
    //   284: invokestatic filter_check : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z
    //   287: ifeq -> 2394
    //   290: iconst_0
    //   291: istore #7
    //   293: aload_0
    //   294: ldc_w 'the_command'
    //   297: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   300: ldc_w 'DELE'
    //   303: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   306: ifeq -> 325
    //   309: aload_0
    //   310: aload_3
    //   311: ldc_w 'RMD'
    //   314: aload #6
    //   316: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;Ljava/util/Properties;)Z
    //   319: ifne -> 325
    //   322: iconst_1
    //   323: istore #7
    //   325: aload_0
    //   326: aload #6
    //   328: invokevirtual changeProxyToCurrentDir : (Ljava/util/Properties;)V
    //   331: ldc_w 'DELETE'
    //   334: aload_3
    //   335: aconst_null
    //   336: iconst_0
    //   337: lconst_0
    //   338: lconst_0
    //   339: aload_0
    //   340: ldc_w 'root_dir'
    //   343: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   346: aload #5
    //   348: ldc_w 'privs'
    //   351: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   354: aload_0
    //   355: ldc_w 'clientid'
    //   358: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   361: ldc ''
    //   363: invokestatic trackSync : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    //   366: aconst_null
    //   367: astore #8
    //   369: ldc2_w -12345
    //   372: lstore #9
    //   374: aload #6
    //   376: ifnonnull -> 451
    //   379: aload_0
    //   380: aload_0
    //   381: ldc_w '550'
    //   384: ldc_w '%DELE-not found%'
    //   387: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   390: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   393: putfield not_done : Z
    //   396: aload_0
    //   397: new java/lang/Exception
    //   400: dup
    //   401: aload_0
    //   402: ldc_w 'lastLog'
    //   405: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   408: invokespecial <init> : (Ljava/lang/String;)V
    //   411: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   414: aload_0
    //   415: ldc_w 'failed_commands'
    //   418: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   421: new java/lang/StringBuffer
    //   424: dup
    //   425: invokespecial <init> : ()V
    //   428: new java/util/Date
    //   431: dup
    //   432: invokespecial <init> : ()V
    //   435: invokevirtual getTime : ()J
    //   438: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   441: invokevirtual toString : ()Ljava/lang/String;
    //   444: invokevirtual addElement : (Ljava/lang/Object;)V
    //   447: ldc_w '%DELE-not found%'
    //   450: areturn
    //   451: aload_0
    //   452: getfield uVFS : Lcrushftp/server/VFS;
    //   455: aload #6
    //   457: invokevirtual getClient : (Ljava/util/Properties;)Lcom/crushftp/client/GenericClient;
    //   460: astore #11
    //   462: aload #6
    //   464: ldc_w 'url'
    //   467: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   470: astore #12
    //   472: aload #12
    //   474: ldc_w ' '
    //   477: invokevirtual endsWith : (Ljava/lang/String;)Z
    //   480: ifeq -> 496
    //   483: aload #12
    //   485: ldc_w ' '
    //   488: ldc_w '%20'
    //   491: invokestatic replace_str : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   494: astore #12
    //   496: aload #11
    //   498: new com/crushftp/client/VRL
    //   501: dup
    //   502: aload #12
    //   504: invokespecial <init> : (Ljava/lang/String;)V
    //   507: invokevirtual getPath : ()Ljava/lang/String;
    //   510: invokevirtual stat : (Ljava/lang/String;)Ljava/util/Properties;
    //   513: astore #8
    //   515: aload #8
    //   517: ldc_w 'type'
    //   520: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   523: ldc_w 'DIR'
    //   526: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   529: ifeq -> 659
    //   532: aload_0
    //   533: ldc_w 'the_command'
    //   536: ldc_w 'RMD'
    //   539: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   542: aload_0
    //   543: aload_3
    //   544: aload_0
    //   545: ldc_w 'the_command'
    //   548: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   551: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;)Z
    //   554: ifne -> 649
    //   557: aload_0
    //   558: ldc_w 'the_command'
    //   561: ldc_w 'DELE'
    //   564: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   567: aload_0
    //   568: aload_0
    //   569: ldc_w '550'
    //   572: ldc_w '%DELE-bad%'
    //   575: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   578: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   581: putfield not_done : Z
    //   584: aload_0
    //   585: new java/lang/Exception
    //   588: dup
    //   589: aload_0
    //   590: ldc_w 'lastLog'
    //   593: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   596: invokespecial <init> : (Ljava/lang/String;)V
    //   599: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   602: aload_0
    //   603: ldc_w 'failed_commands'
    //   606: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   609: new java/lang/StringBuffer
    //   612: dup
    //   613: invokespecial <init> : ()V
    //   616: new java/util/Date
    //   619: dup
    //   620: invokespecial <init> : ()V
    //   623: invokevirtual getTime : ()J
    //   626: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   629: invokevirtual toString : ()Ljava/lang/String;
    //   632: invokevirtual addElement : (Ljava/lang/Object;)V
    //   635: aload_0
    //   636: getfield uVFS : Lcrushftp/server/VFS;
    //   639: aload #11
    //   641: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   644: pop
    //   645: ldc_w '%DELE-bad%'
    //   648: areturn
    //   649: aload_0
    //   650: ldc_w 'the_command'
    //   653: ldc_w 'DELE'
    //   656: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   659: aload_0
    //   660: aload_3
    //   661: invokevirtual get_quota : (Ljava/lang/String;)J
    //   664: lstore #9
    //   666: aload #6
    //   668: ifnull -> 2297
    //   671: aload #8
    //   673: ifnull -> 2297
    //   676: aload #6
    //   678: invokevirtual clone : ()Ljava/lang/Object;
    //   681: checkcast java/util/Properties
    //   684: astore #13
    //   686: aload #13
    //   688: invokevirtual clone : ()Ljava/lang/Object;
    //   691: checkcast java/util/Properties
    //   694: astore #13
    //   696: ldc_w 'FTP_SERVER'
    //   699: iconst_2
    //   700: new java/lang/StringBuffer
    //   703: dup
    //   704: ldc_w 'Tracking delete:'
    //   707: invokespecial <init> : (Ljava/lang/String;)V
    //   710: aload_3
    //   711: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   714: invokevirtual toString : ()Ljava/lang/String;
    //   717: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   720: pop
    //   721: aload #13
    //   723: ldc_w 'the_command'
    //   726: ldc_w 'DELE'
    //   729: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   732: pop
    //   733: aload #13
    //   735: ldc_w 'the_command_data'
    //   738: aload_3
    //   739: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   742: pop
    //   743: aload #13
    //   745: ldc_w 'url'
    //   748: aload #6
    //   750: ldc_w 'url'
    //   753: ldc ''
    //   755: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   758: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   761: pop
    //   762: aload #13
    //   764: ldc_w 'the_file_path'
    //   767: aload_3
    //   768: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   771: pop
    //   772: aload #13
    //   774: ldc_w 'the_file_name'
    //   777: aload #8
    //   779: ldc_w 'name'
    //   782: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   785: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   788: pop
    //   789: aload #13
    //   791: ldc_w 'the_file_size'
    //   794: aload #8
    //   796: ldc_w 'size'
    //   799: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   802: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   805: pop
    //   806: aload #13
    //   808: ldc_w 'the_file_speed'
    //   811: ldc_w '0'
    //   814: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   817: pop
    //   818: aload #13
    //   820: ldc_w 'the_file_start'
    //   823: new java/lang/StringBuffer
    //   826: dup
    //   827: new java/util/Date
    //   830: dup
    //   831: invokespecial <init> : ()V
    //   834: invokevirtual getTime : ()J
    //   837: invokestatic valueOf : (J)Ljava/lang/String;
    //   840: invokespecial <init> : (Ljava/lang/String;)V
    //   843: invokevirtual toString : ()Ljava/lang/String;
    //   846: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   849: pop
    //   850: aload #13
    //   852: ldc_w 'the_file_end'
    //   855: new java/lang/StringBuffer
    //   858: dup
    //   859: new java/util/Date
    //   862: dup
    //   863: invokespecial <init> : ()V
    //   866: invokevirtual getTime : ()J
    //   869: invokestatic valueOf : (J)Ljava/lang/String;
    //   872: invokespecial <init> : (Ljava/lang/String;)V
    //   875: invokevirtual toString : ()Ljava/lang/String;
    //   878: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   881: pop
    //   882: aload #13
    //   884: ldc_w 'the_file_error'
    //   887: ldc ''
    //   889: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   892: pop
    //   893: aload #13
    //   895: ldc_w 'the_file_type'
    //   898: aload #8
    //   900: ldc_w 'type'
    //   903: ldc ''
    //   905: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   908: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   911: pop
    //   912: aload #13
    //   914: ldc_w 'the_file_status'
    //   917: ldc_w 'SUCCESS'
    //   920: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   923: pop
    //   924: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   927: getfield statTools : Lcrushftp/db/StatTools;
    //   930: aload_0
    //   931: aload #13
    //   933: ldc_w 'DELETE'
    //   936: invokevirtual add_item_stat : (Lcrushftp/handlers/SessionCrush;Ljava/util/Properties;Ljava/lang/String;)Ljava/util/Properties;
    //   939: pop
    //   940: aload_0
    //   941: ldc_w 'DELETE'
    //   944: aload #13
    //   946: invokevirtual do_event5 : (Ljava/lang/String;Ljava/util/Properties;)Ljava/util/Properties;
    //   949: pop
    //   950: aload #6
    //   952: ldc_w 'size'
    //   955: ldc_w '0'
    //   958: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   961: invokestatic parseLong : (Ljava/lang/String;)J
    //   964: lstore #14
    //   966: iconst_0
    //   967: istore #16
    //   969: new java/util/Properties
    //   972: dup
    //   973: invokespecial <init> : ()V
    //   976: astore #17
    //   978: aload #17
    //   980: ldc_w 'crushftp_user_name'
    //   983: aload_0
    //   984: ldc_w 'user_name'
    //   987: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   990: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   993: pop
    //   994: aload #11
    //   996: new com/crushftp/client/VRL
    //   999: dup
    //   1000: aload #12
    //   1002: invokespecial <init> : (Ljava/lang/String;)V
    //   1005: aload_3
    //   1006: aload_0
    //   1007: ldc_w 'root_dir'
    //   1010: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1013: aload #6
    //   1015: ldc_w 'privs'
    //   1018: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1021: iconst_1
    //   1022: aload #17
    //   1024: invokestatic trackSyncRevision : (Lcom/crushftp/client/GenericClient;Lcom/crushftp/client/VRL;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZLjava/util/Properties;)V
    //   1027: aload #6
    //   1029: aload_0
    //   1030: getfield uVFS : Lcrushftp/server/VFS;
    //   1033: iconst_1
    //   1034: iconst_1
    //   1035: invokestatic buildEntry : (Ljava/util/Properties;Lcrushftp/server/VFS;ZZ)V
    //   1038: ldc_w 'recycle'
    //   1041: invokestatic BG : (Ljava/lang/String;)Z
    //   1044: ifeq -> 1115
    //   1047: new com/crushftp/client/VRL
    //   1050: dup
    //   1051: aload #12
    //   1053: invokespecial <init> : (Ljava/lang/String;)V
    //   1056: invokevirtual getProtocol : ()Ljava/lang/String;
    //   1059: ldc_w 'FILE'
    //   1062: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   1065: ifeq -> 1115
    //   1068: ldc_w 'FTP_SERVER'
    //   1071: iconst_3
    //   1072: new java/lang/StringBuffer
    //   1075: dup
    //   1076: ldc_w 'Attempting to recycle file:'
    //   1079: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   1082: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1085: invokespecial <init> : (Ljava/lang/String;)V
    //   1088: aload_3
    //   1089: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1092: invokevirtual toString : ()Ljava/lang/String;
    //   1095: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   1098: pop
    //   1099: aload_0
    //   1100: new com/crushftp/client/VRL
    //   1103: dup
    //   1104: aload #12
    //   1106: invokespecial <init> : (Ljava/lang/String;)V
    //   1109: invokevirtual do_Recycle : (Lcom/crushftp/client/VRL;)V
    //   1112: goto -> 1165
    //   1115: ldc_w 'FTP_SERVER'
    //   1118: iconst_3
    //   1119: new java/lang/StringBuffer
    //   1122: dup
    //   1123: ldc_w 'Attempting to delete file:'
    //   1126: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   1129: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1132: invokespecial <init> : (Ljava/lang/String;)V
    //   1135: aload_3
    //   1136: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1139: invokevirtual toString : ()Ljava/lang/String;
    //   1142: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   1145: pop
    //   1146: aload #11
    //   1148: new com/crushftp/client/VRL
    //   1151: dup
    //   1152: aload #12
    //   1154: invokespecial <init> : (Ljava/lang/String;)V
    //   1157: invokevirtual getPath : ()Ljava/lang/String;
    //   1160: invokevirtual delete : (Ljava/lang/String;)Z
    //   1163: istore #16
    //   1165: aconst_null
    //   1166: astore #8
    //   1168: iload #16
    //   1170: ifne -> 1192
    //   1173: aload #11
    //   1175: new com/crushftp/client/VRL
    //   1178: dup
    //   1179: aload #12
    //   1181: invokespecial <init> : (Ljava/lang/String;)V
    //   1184: invokevirtual getPath : ()Ljava/lang/String;
    //   1187: invokevirtual stat : (Ljava/lang/String;)Ljava/util/Properties;
    //   1190: astore #8
    //   1192: iload #16
    //   1194: ifne -> 1909
    //   1197: aload #8
    //   1199: ifnull -> 1909
    //   1202: iload_1
    //   1203: ifeq -> 1909
    //   1206: new com/crushftp/client/VRL
    //   1209: dup
    //   1210: aload #12
    //   1212: invokespecial <init> : (Ljava/lang/String;)V
    //   1215: invokevirtual getCanonicalPath : ()Ljava/lang/String;
    //   1218: lconst_0
    //   1219: aload_0
    //   1220: invokestatic recurseSize : (Ljava/lang/String;JLcrushftp/handlers/SessionCrush;)J
    //   1223: lstore #14
    //   1225: goto -> 1240
    //   1228: astore #18
    //   1230: ldc_w 'FTP_SERVER'
    //   1233: iconst_1
    //   1234: aload #18
    //   1236: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   1239: pop
    //   1240: ldc_w 'recycle'
    //   1243: invokestatic BG : (Ljava/lang/String;)Z
    //   1246: ifeq -> 1265
    //   1249: aload_0
    //   1250: new com/crushftp/client/VRL
    //   1253: dup
    //   1254: aload #12
    //   1256: invokespecial <init> : (Ljava/lang/String;)V
    //   1259: invokevirtual do_Recycle : (Lcom/crushftp/client/VRL;)V
    //   1262: goto -> 1872
    //   1265: aload_0
    //   1266: getfield user : Ljava/util/Properties;
    //   1269: ldc_w 'events'
    //   1272: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   1275: ifnull -> 1301
    //   1278: aload_0
    //   1279: getfield user : Ljava/util/Properties;
    //   1282: ldc_w 'events'
    //   1285: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   1288: checkcast java/util/Vector
    //   1291: invokevirtual size : ()I
    //   1294: ifle -> 1301
    //   1297: iconst_1
    //   1298: goto -> 1302
    //   1301: iconst_0
    //   1302: istore #18
    //   1304: iload #18
    //   1306: ifeq -> 1380
    //   1309: iconst_0
    //   1310: istore #18
    //   1312: aload_0
    //   1313: getfield user : Ljava/util/Properties;
    //   1316: ldc_w 'events'
    //   1319: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   1322: checkcast java/util/Vector
    //   1325: astore #19
    //   1327: iconst_0
    //   1328: istore #20
    //   1330: goto -> 1370
    //   1333: aload #19
    //   1335: iload #20
    //   1337: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   1340: checkcast java/util/Properties
    //   1343: astore #21
    //   1345: aload #21
    //   1347: ldc_w 'event_user_action_list'
    //   1350: ldc ''
    //   1352: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1355: ldc_w '(delete)'
    //   1358: invokevirtual indexOf : (Ljava/lang/String;)I
    //   1361: iflt -> 1367
    //   1364: iconst_1
    //   1365: istore #18
    //   1367: iinc #20, 1
    //   1370: iload #20
    //   1372: aload #19
    //   1374: invokevirtual size : ()I
    //   1377: if_icmplt -> 1333
    //   1380: iload #18
    //   1382: istore #19
    //   1384: iload #7
    //   1386: ifne -> 1398
    //   1389: ldc_w 'check_all_recursive_deletes'
    //   1392: invokestatic BG : (Ljava/lang/String;)Z
    //   1395: ifeq -> 1844
    //   1398: new java/util/Vector
    //   1401: dup
    //   1402: invokespecial <init> : ()V
    //   1405: astore #20
    //   1407: aload_3
    //   1408: astore #21
    //   1410: new java/util/Vector
    //   1413: dup
    //   1414: invokespecial <init> : ()V
    //   1417: astore #22
    //   1419: new java/util/Properties
    //   1422: dup
    //   1423: invokespecial <init> : ()V
    //   1426: astore #23
    //   1428: new crushftp/handlers/SessionCrush$2
    //   1431: dup
    //   1432: aload_0
    //   1433: aload #23
    //   1435: aload #20
    //   1437: aload #21
    //   1439: aload #22
    //   1441: invokespecial <init> : (Lcrushftp/handlers/SessionCrush;Ljava/util/Properties;Ljava/util/Vector;Ljava/lang/String;Ljava/util/Vector;)V
    //   1444: invokestatic startWorker : (Ljava/lang/Runnable;)Z
    //   1447: pop
    //   1448: new java/util/Vector
    //   1451: dup
    //   1452: invokespecial <init> : ()V
    //   1455: astore #24
    //   1457: new java/util/Vector
    //   1460: dup
    //   1461: invokespecial <init> : ()V
    //   1464: astore #25
    //   1466: goto -> 1589
    //   1469: aload #20
    //   1471: invokevirtual size : ()I
    //   1474: ifne -> 1486
    //   1477: ldc2_w 100
    //   1480: invokestatic sleep : (J)V
    //   1483: goto -> 1589
    //   1486: aload #20
    //   1488: iconst_0
    //   1489: invokevirtual remove : (I)Ljava/lang/Object;
    //   1492: checkcast java/util/Properties
    //   1495: astore #26
    //   1497: aload_0
    //   1498: astore #27
    //   1500: aload #26
    //   1502: ldc_w 'type'
    //   1505: ldc ''
    //   1507: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1510: ldc_w 'FILE'
    //   1513: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1516: ifeq -> 1582
    //   1519: goto -> 1528
    //   1522: ldc2_w 100
    //   1525: invokestatic sleep : (J)V
    //   1528: aload #24
    //   1530: invokevirtual size : ()I
    //   1533: ldc_w 'delete_threads'
    //   1536: invokestatic IG : (Ljava/lang/String;)I
    //   1539: if_icmpgt -> 1522
    //   1542: aload #24
    //   1544: aload #26
    //   1546: ldc_w 'url'
    //   1549: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1552: invokevirtual addElement : (Ljava/lang/Object;)V
    //   1555: new crushftp/handlers/SessionCrush$3
    //   1558: dup
    //   1559: aload_0
    //   1560: aload #26
    //   1562: aload #24
    //   1564: iload #19
    //   1566: aload #11
    //   1568: aload #27
    //   1570: aload #22
    //   1572: invokespecial <init> : (Lcrushftp/handlers/SessionCrush;Ljava/util/Properties;Ljava/util/Vector;ZLcom/crushftp/client/GenericClient;Lcrushftp/handlers/SessionCrush;Ljava/util/Vector;)V
    //   1575: invokestatic startWorker : (Ljava/lang/Runnable;)Z
    //   1578: pop
    //   1579: goto -> 1589
    //   1582: aload #25
    //   1584: aload #26
    //   1586: invokevirtual addElement : (Ljava/lang/Object;)V
    //   1589: aload #20
    //   1591: invokevirtual size : ()I
    //   1594: ifgt -> 1469
    //   1597: aload #23
    //   1599: ldc_w 'done'
    //   1602: invokevirtual containsKey : (Ljava/lang/Object;)Z
    //   1605: ifeq -> 1469
    //   1608: iconst_0
    //   1609: istore #26
    //   1611: goto -> 1623
    //   1614: ldc2_w 100
    //   1617: invokestatic sleep : (J)V
    //   1620: iinc #26, 1
    //   1623: iload #26
    //   1625: sipush #6000
    //   1628: if_icmpge -> 1639
    //   1631: aload #24
    //   1633: invokevirtual size : ()I
    //   1636: ifgt -> 1614
    //   1639: aload #22
    //   1641: invokevirtual size : ()I
    //   1644: ifle -> 1686
    //   1647: ldc 'SERVER'
    //   1649: iconst_1
    //   1650: new java/lang/StringBuffer
    //   1653: dup
    //   1654: ldc_w 'Failed to delete:'
    //   1657: invokespecial <init> : (Ljava/lang/String;)V
    //   1660: aload #22
    //   1662: invokevirtual append : (Ljava/lang/Object;)Ljava/lang/StringBuffer;
    //   1665: invokevirtual toString : ()Ljava/lang/String;
    //   1668: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   1671: pop
    //   1672: aload_0
    //   1673: getfield uVFS : Lcrushftp/server/VFS;
    //   1676: aload #11
    //   1678: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   1681: pop
    //   1682: ldc_w '%DELE-error%'
    //   1685: areturn
    //   1686: iconst_0
    //   1687: istore #26
    //   1689: goto -> 1827
    //   1692: aload #25
    //   1694: invokevirtual size : ()I
    //   1697: iconst_1
    //   1698: isub
    //   1699: istore #27
    //   1701: goto -> 1819
    //   1704: aload #25
    //   1706: iload #27
    //   1708: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   1711: checkcast java/util/Properties
    //   1714: astore #28
    //   1716: aload #28
    //   1718: ldc_w 'type'
    //   1721: ldc ''
    //   1723: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1726: ldc_w 'DIR'
    //   1729: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1732: ifeq -> 1816
    //   1735: new java/lang/StringBuffer
    //   1738: dup
    //   1739: aload #28
    //   1741: ldc_w 'root_dir'
    //   1744: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1747: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1750: invokespecial <init> : (Ljava/lang/String;)V
    //   1753: aload #28
    //   1755: ldc_w 'name'
    //   1758: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1761: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1764: invokevirtual toString : ()Ljava/lang/String;
    //   1767: astore #29
    //   1769: aload_0
    //   1770: aload #29
    //   1772: ldc_w 'RMD'
    //   1775: aload #28
    //   1777: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;Ljava/util/Properties;)Z
    //   1780: ifeq -> 1816
    //   1783: aload #11
    //   1785: new com/crushftp/client/VRL
    //   1788: dup
    //   1789: aload #28
    //   1791: ldc_w 'url'
    //   1794: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1797: invokespecial <init> : (Ljava/lang/String;)V
    //   1800: invokevirtual getPath : ()Ljava/lang/String;
    //   1803: invokevirtual delete : (Ljava/lang/String;)Z
    //   1806: ifeq -> 1816
    //   1809: aload #25
    //   1811: iload #27
    //   1813: invokevirtual removeElementAt : (I)V
    //   1816: iinc #27, -1
    //   1819: iload #27
    //   1821: ifge -> 1704
    //   1824: iinc #26, 1
    //   1827: aload #25
    //   1829: invokevirtual size : ()I
    //   1832: ifle -> 1872
    //   1835: iload #26
    //   1837: iconst_5
    //   1838: if_icmplt -> 1692
    //   1841: goto -> 1872
    //   1844: new com/crushftp/client/VRL
    //   1847: dup
    //   1848: aload #12
    //   1850: invokespecial <init> : (Ljava/lang/String;)V
    //   1853: invokevirtual getCanonicalPath : ()Ljava/lang/String;
    //   1856: iconst_0
    //   1857: invokestatic recurseDelete : (Ljava/lang/String;Z)V
    //   1860: aload #11
    //   1862: aload_3
    //   1863: invokevirtual delete : (Ljava/lang/String;)Z
    //   1866: pop
    //   1867: goto -> 1872
    //   1870: astore #18
    //   1872: aload #6
    //   1874: ifnull -> 1909
    //   1877: aload_0
    //   1878: aload_0
    //   1879: ldc_w 'lastUploadStats'
    //   1882: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   1885: new com/crushftp/client/VRL
    //   1888: dup
    //   1889: aload #12
    //   1891: invokespecial <init> : (Ljava/lang/String;)V
    //   1894: new com/crushftp/client/VRL
    //   1897: dup
    //   1898: aload #12
    //   1900: invokespecial <init> : (Ljava/lang/String;)V
    //   1903: ldc_w 'DELETE'
    //   1906: invokevirtual trackAndUpdateUploads : (Ljava/util/Vector;Lcom/crushftp/client/VRL;Lcom/crushftp/client/VRL;Ljava/lang/String;)V
    //   1909: aconst_null
    //   1910: astore #8
    //   1912: iload #16
    //   1914: ifne -> 1936
    //   1917: aload #11
    //   1919: new com/crushftp/client/VRL
    //   1922: dup
    //   1923: aload #12
    //   1925: invokespecial <init> : (Ljava/lang/String;)V
    //   1928: invokevirtual getPath : ()Ljava/lang/String;
    //   1931: invokevirtual stat : (Ljava/lang/String;)Ljava/util/Properties;
    //   1934: astore #8
    //   1936: iload #16
    //   1938: ifne -> 2179
    //   1941: aload #8
    //   1943: ifnull -> 2179
    //   1946: new java/io/File
    //   1949: dup
    //   1950: ldc_w 'recycle_path'
    //   1953: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1956: invokespecial <init> : (Ljava/lang/String;)V
    //   1959: invokevirtual exists : ()Z
    //   1962: ifne -> 2056
    //   1965: ldc_w 'recycle'
    //   1968: invokestatic BG : (Ljava/lang/String;)Z
    //   1971: ifeq -> 2056
    //   1974: aload_0
    //   1975: aload_0
    //   1976: ldc_w '550'
    //   1979: ldc_w '%DELE-error%:Recycle bin not found.'
    //   1982: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   1985: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   1988: putfield not_done : Z
    //   1991: aload_0
    //   1992: new java/lang/Exception
    //   1995: dup
    //   1996: aload_0
    //   1997: ldc_w 'lastLog'
    //   2000: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   2003: invokespecial <init> : (Ljava/lang/String;)V
    //   2006: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   2009: aload_0
    //   2010: ldc_w 'failed_commands'
    //   2013: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   2016: new java/lang/StringBuffer
    //   2019: dup
    //   2020: invokespecial <init> : ()V
    //   2023: new java/util/Date
    //   2026: dup
    //   2027: invokespecial <init> : ()V
    //   2030: invokevirtual getTime : ()J
    //   2033: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   2036: invokevirtual toString : ()Ljava/lang/String;
    //   2039: invokevirtual addElement : (Ljava/lang/Object;)V
    //   2042: aload_0
    //   2043: getfield uVFS : Lcrushftp/server/VFS;
    //   2046: aload #11
    //   2048: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   2051: pop
    //   2052: ldc_w '%DELE-error%:Recycle bin not found.'
    //   2055: areturn
    //   2056: ldc_w 'FTP_SERVER'
    //   2059: iconst_3
    //   2060: ldc_w 'Delete failure.  Deleted:$0 Exists:$1'
    //   2063: new java/lang/StringBuffer
    //   2066: dup
    //   2067: iload #16
    //   2069: invokestatic valueOf : (Z)Ljava/lang/String;
    //   2072: invokespecial <init> : (Ljava/lang/String;)V
    //   2075: invokevirtual toString : ()Ljava/lang/String;
    //   2078: new java/lang/StringBuffer
    //   2081: dup
    //   2082: invokespecial <init> : ()V
    //   2085: aload #8
    //   2087: ifnull -> 2094
    //   2090: iconst_1
    //   2091: goto -> 2095
    //   2094: iconst_0
    //   2095: invokevirtual append : (Z)Ljava/lang/StringBuffer;
    //   2098: invokevirtual toString : ()Ljava/lang/String;
    //   2101: invokestatic G : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   2104: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   2107: pop
    //   2108: aload_0
    //   2109: aload_0
    //   2110: ldc_w '550'
    //   2113: ldc_w '%DELE-error%'
    //   2116: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2119: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   2122: putfield not_done : Z
    //   2125: aload_0
    //   2126: new java/lang/Exception
    //   2129: dup
    //   2130: aload_0
    //   2131: ldc_w 'lastLog'
    //   2134: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   2137: invokespecial <init> : (Ljava/lang/String;)V
    //   2140: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   2143: aload_0
    //   2144: ldc_w 'failed_commands'
    //   2147: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   2150: new java/lang/StringBuffer
    //   2153: dup
    //   2154: invokespecial <init> : ()V
    //   2157: new java/util/Date
    //   2160: dup
    //   2161: invokespecial <init> : ()V
    //   2164: invokevirtual getTime : ()J
    //   2167: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   2170: invokevirtual toString : ()Ljava/lang/String;
    //   2173: invokevirtual addElement : (Ljava/lang/Object;)V
    //   2176: goto -> 1672
    //   2179: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   2182: pop
    //   2183: ldc_w 'generic_ftp_responses'
    //   2186: invokestatic BG : (Ljava/lang/String;)Z
    //   2189: ifeq -> 2209
    //   2192: aload_0
    //   2193: aload_0
    //   2194: ldc_w '250'
    //   2197: ldc_w 'Delete operation successful.'
    //   2200: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   2203: putfield not_done : Z
    //   2206: goto -> 2226
    //   2209: aload_0
    //   2210: aload_0
    //   2211: ldc_w '250'
    //   2214: ldc_w 'custom_delete_msg'
    //   2217: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   2220: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   2223: putfield not_done : Z
    //   2226: aload_0
    //   2227: aload #6
    //   2229: invokevirtual removeCacheItem : (Ljava/util/Properties;)V
    //   2232: lload #9
    //   2234: ldc2_w -12345
    //   2237: lcmp
    //   2238: ifeq -> 2284
    //   2241: aload #6
    //   2243: ldc_w 'privs'
    //   2246: ldc ''
    //   2248: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   2251: ldc_w '(real_quota)'
    //   2254: invokevirtual indexOf : (Ljava/lang/String;)I
    //   2257: iflt -> 2270
    //   2260: lload #9
    //   2262: lload #14
    //   2264: ladd
    //   2265: lstore #9
    //   2267: goto -> 2277
    //   2270: lload #9
    //   2272: lload #14
    //   2274: ladd
    //   2275: lstore #9
    //   2277: aload_0
    //   2278: aload_3
    //   2279: lload #9
    //   2281: invokevirtual set_quota : (Ljava/lang/String;J)V
    //   2284: aload_0
    //   2285: getfield uVFS : Lcrushftp/server/VFS;
    //   2288: aload #11
    //   2290: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   2293: pop
    //   2294: ldc ''
    //   2296: areturn
    //   2297: aload_0
    //   2298: aload_0
    //   2299: ldc_w '550'
    //   2302: ldc_w '%DELE-not found%'
    //   2305: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2308: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   2311: putfield not_done : Z
    //   2314: aload_0
    //   2315: new java/lang/Exception
    //   2318: dup
    //   2319: aload_0
    //   2320: ldc_w 'lastLog'
    //   2323: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   2326: invokespecial <init> : (Ljava/lang/String;)V
    //   2329: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   2332: aload_0
    //   2333: ldc_w 'failed_commands'
    //   2336: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   2339: new java/lang/StringBuffer
    //   2342: dup
    //   2343: invokespecial <init> : ()V
    //   2346: new java/util/Date
    //   2349: dup
    //   2350: invokespecial <init> : ()V
    //   2353: invokevirtual getTime : ()J
    //   2356: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   2359: invokevirtual toString : ()Ljava/lang/String;
    //   2362: invokevirtual addElement : (Ljava/lang/Object;)V
    //   2365: aload_0
    //   2366: getfield uVFS : Lcrushftp/server/VFS;
    //   2369: aload #11
    //   2371: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   2374: pop
    //   2375: ldc_w '%DELE-not found%'
    //   2378: areturn
    //   2379: astore #30
    //   2381: aload_0
    //   2382: getfield uVFS : Lcrushftp/server/VFS;
    //   2385: aload #11
    //   2387: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   2390: pop
    //   2391: aload #30
    //   2393: athrow
    //   2394: aload_0
    //   2395: aload_0
    //   2396: ldc_w '550'
    //   2399: ldc_w '%DELE-bad%'
    //   2402: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2405: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   2408: putfield not_done : Z
    //   2411: aload_0
    //   2412: new java/lang/Exception
    //   2415: dup
    //   2416: aload_0
    //   2417: ldc_w 'lastLog'
    //   2420: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   2423: invokespecial <init> : (Ljava/lang/String;)V
    //   2426: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   2429: aload_0
    //   2430: ldc_w 'failed_commands'
    //   2433: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   2436: new java/lang/StringBuffer
    //   2439: dup
    //   2440: invokespecial <init> : ()V
    //   2443: new java/util/Date
    //   2446: dup
    //   2447: invokespecial <init> : ()V
    //   2450: invokevirtual getTime : ()J
    //   2453: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   2456: invokevirtual toString : ()Ljava/lang/String;
    //   2459: invokevirtual addElement : (Ljava/lang/Object;)V
    //   2462: ldc_w '%DELE-bad%'
    //   2465: areturn
    // Line number table:
    //   Java source line number -> byte code offset
    //   #2993	-> 0
    //   #2994	-> 10
    //   #2995	-> 20
    //   #2996	-> 26
    //   #2997	-> 38
    //   #2998	-> 50
    //   #2999	-> 61
    //   #3001	-> 66
    //   #3002	-> 102
    //   #3004	-> 119
    //   #3006	-> 154
    //   #3007	-> 171
    //   #3008	-> 189
    //   #3009	-> 222
    //   #3011	-> 226
    //   #3013	-> 290
    //   #3014	-> 293
    //   #3015	-> 325
    //   #3016	-> 331
    //   #3017	-> 366
    //   #3018	-> 369
    //   #3019	-> 374
    //   #3021	-> 379
    //   #3022	-> 396
    //   #3023	-> 414
    //   #3024	-> 447
    //   #3026	-> 451
    //   #3029	-> 462
    //   #3030	-> 472
    //   #3031	-> 496
    //   #3032	-> 515
    //   #3034	-> 532
    //   #3035	-> 542
    //   #3037	-> 557
    //   #3038	-> 567
    //   #3039	-> 584
    //   #3040	-> 602
    //   #3288	-> 635
    //   #3041	-> 645
    //   #3043	-> 649
    //   #3045	-> 659
    //   #3046	-> 666
    //   #3048	-> 676
    //   #3049	-> 686
    //   #3050	-> 696
    //   #3051	-> 721
    //   #3052	-> 733
    //   #3053	-> 743
    //   #3054	-> 762
    //   #3055	-> 772
    //   #3056	-> 789
    //   #3057	-> 806
    //   #3058	-> 818
    //   #3059	-> 850
    //   #3060	-> 882
    //   #3061	-> 893
    //   #3062	-> 912
    //   #3063	-> 924
    //   #3064	-> 940
    //   #3066	-> 950
    //   #3067	-> 966
    //   #3068	-> 969
    //   #3069	-> 978
    //   #3070	-> 994
    //   #3071	-> 1027
    //   #3072	-> 1038
    //   #3074	-> 1068
    //   #3075	-> 1099
    //   #3079	-> 1115
    //   #3080	-> 1146
    //   #3082	-> 1165
    //   #3083	-> 1168
    //   #3084	-> 1192
    //   #3088	-> 1206
    //   #3090	-> 1228
    //   #3092	-> 1230
    //   #3094	-> 1240
    //   #3096	-> 1249
    //   #3102	-> 1265
    //   #3103	-> 1304
    //   #3105	-> 1309
    //   #3106	-> 1312
    //   #3107	-> 1327
    //   #3109	-> 1333
    //   #3110	-> 1345
    //   #3107	-> 1367
    //   #3113	-> 1380
    //   #3114	-> 1384
    //   #3116	-> 1398
    //   #3117	-> 1407
    //   #3118	-> 1410
    //   #3119	-> 1419
    //   #3120	-> 1428
    //   #3139	-> 1448
    //   #3140	-> 1457
    //   #3141	-> 1466
    //   #3143	-> 1469
    //   #3145	-> 1477
    //   #3146	-> 1483
    //   #3148	-> 1486
    //   #3149	-> 1497
    //   #3150	-> 1500
    //   #3152	-> 1519
    //   #3153	-> 1522
    //   #3152	-> 1528
    //   #3154	-> 1542
    //   #3155	-> 1555
    //   #3203	-> 1582
    //   #3141	-> 1589
    //   #3205	-> 1608
    //   #3206	-> 1614
    //   #3205	-> 1620
    //   #3207	-> 1639
    //   #3209	-> 1647
    //   #3288	-> 1672
    //   #3210	-> 1682
    //   #3212	-> 1686
    //   #3214	-> 1692
    //   #3216	-> 1704
    //   #3217	-> 1716
    //   #3219	-> 1735
    //   #3220	-> 1769
    //   #3222	-> 1783
    //   #3224	-> 1809
    //   #3214	-> 1816
    //   #3212	-> 1824
    //   #3233	-> 1844
    //   #3234	-> 1860
    //   #3237	-> 1870
    //   #3241	-> 1872
    //   #3243	-> 1909
    //   #3244	-> 1912
    //   #3245	-> 1936
    //   #3247	-> 1946
    //   #3249	-> 1974
    //   #3250	-> 1991
    //   #3251	-> 2009
    //   #3288	-> 2042
    //   #3252	-> 2052
    //   #3256	-> 2056
    //   #3257	-> 2108
    //   #3258	-> 2125
    //   #3259	-> 2143
    //   #3260	-> 2176
    //   #3265	-> 2179
    //   #3266	-> 2209
    //   #3267	-> 2226
    //   #3268	-> 2232
    //   #3270	-> 2241
    //   #3271	-> 2270
    //   #3272	-> 2277
    //   #3288	-> 2284
    //   #3275	-> 2294
    //   #3280	-> 2297
    //   #3281	-> 2314
    //   #3282	-> 2332
    //   #3288	-> 2365
    //   #3283	-> 2375
    //   #3287	-> 2379
    //   #3288	-> 2381
    //   #3289	-> 2391
    //   #3293	-> 2394
    //   #3294	-> 2411
    //   #3295	-> 2429
    //   #3296	-> 2462
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	2466	0	this	Lcrushftp/handlers/SessionCrush;
    //   0	2466	1	recurse	Z
    //   0	2466	2	user_dir	Ljava/lang/String;
    //   26	2440	3	the_dir	Ljava/lang/String;
    //   38	2428	4	parentPath	Ljava/lang/String;
    //   50	2416	5	dir_item	Ljava/util/Properties;
    //   61	2405	6	item	Ljava/util/Properties;
    //   293	2101	7	check_all	Z
    //   369	2025	8	stat	Ljava/util/Properties;
    //   374	2020	9	quota	J
    //   462	1932	11	c	Lcom/crushftp/client/GenericClient;
    //   472	1907	12	fix_url	Ljava/lang/String;
    //   686	1611	13	fileItem	Ljava/util/Properties;
    //   966	1331	14	totalSize	J
    //   969	1328	16	deleted	Z
    //   978	1319	17	info	Ljava/util/Properties;
    //   1230	10	18	e	Ljava/lang/Exception;
    //   1304	566	18	has_events	Z
    //   1327	53	19	events	Ljava/util/Vector;
    //   1330	50	20	x	I
    //   1345	22	21	event	Ljava/util/Properties;
    //   1384	486	19	has_delete_event	Z
    //   1407	437	20	list1	Ljava/util/Vector;
    //   1410	434	21	the_dir_f	Ljava/lang/String;
    //   1419	425	22	errors	Ljava/util/Vector;
    //   1428	416	23	status	Ljava/util/Properties;
    //   1457	387	24	threads	Ljava/util/Vector;
    //   1466	378	25	list2	Ljava/util/Vector;
    //   1497	92	26	p	Ljava/util/Properties;
    //   1500	89	27	thisObj	Lcrushftp/handlers/SessionCrush;
    //   1611	28	26	loops	I
    //   1689	152	26	loop	I
    //   1701	123	27	x	I
    //   1716	100	28	p	Ljava/util/Properties;
    //   1769	47	29	temp_dir	Ljava/lang/String;
    // Exception table:
    //   from	to	target	type
    //   462	635	2379	finally
    //   649	1672	2379	finally
    //   1206	1225	1228	java/lang/Exception
    //   1265	1672	1870	java/lang/NullPointerException
    //   1686	1867	1870	java/lang/NullPointerException
    //   1686	2042	2379	finally
    //   2056	2284	2379	finally
    //   2297	2365	2379	finally
  }
  
  public String do_RNFR() throws Exception {
    uiPUT("the_command", "RNFR");
    uiPUT("last_logged_command", "RNFR");
    this.rnfr_file = null;
    String the_dir = fixupDir(null);
    String parentPath = this.uVFS.getRootVFS(the_dir, -1);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_fake_item(the_dir, "FILE");
    if (item == null) {
      Thread.sleep(500L);
      item = this.uVFS.get_fake_item(the_dir, "FILE");
    } 
    this.rnfr_file_path = the_dir;
    if (check_access_privs(the_dir, uiSG("the_command"), item)) {
      changeProxyToCurrentDir(item);
      GenericClient c = this.uVFS.getClient(item);
      try {
        if (c.stat((new VRL(item.getProperty("url"))).getPath()) != null) {
          this.not_done = ftp_write_command("350", LOC.G("%RNFR%"));
          this.rnfr_file = item;
          this.rnfr_file_path = the_dir;
          return "";
        } 
        this.not_done = ftp_write_command("550", LOC.G("%RNFR-not found%"));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        return "%RNFR-not found%";
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
    } 
    this.not_done = ftp_write_command("550", LOC.G("%RNFR-bad%"));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%RNFR-bad%";
  }
  
  public void trackAndUpdateUploads(Vector lastUploadStats, VRL src, VRL dest, String type) {
    if (lastUploadStats == null)
      return; 
    for (int x = lastUploadStats.size() - 1; x >= 0; x--) {
      Properties p2 = lastUploadStats.elementAt(x);
      if (p2.getProperty("url", "").toUpperCase().equals(src.toUpperCase()) && type.equals("RENAME")) {
        p2.put("url", dest);
        p2.put("path", dest.getPath());
        p2.put("name", dest.getName());
        break;
      } 
      if (p2.getProperty("url", "").toUpperCase().equals(src.toUpperCase()) && type.equals("DELETE")) {
        do_event5("DELETE", p2, null);
        lastUploadStats.removeElementAt(x);
        break;
      } 
    } 
  }
  
  public String do_RNTO(boolean overwrite) throws Exception {
    uiPUT("the_command", "RNTO");
    uiPUT("last_logged_command", "RNTO");
    String the_dir = fixupDir(null);
    Properties combinedPermissions = this.uVFS.getCombinedPermissions();
    boolean aclPermissions = combinedPermissions.getProperty("acl_permissions", "false").equals("true");
    Properties actual_item = this.uVFS.get_item(the_dir);
    Properties item = this.uVFS.get_item_parent(the_dir);
    if (!aclPermissions)
      actual_item = item; 
    if (check_access_privs(the_dir, uiSG("the_command"), item) && (!overwrite || (overwrite && check_access_privs(Common.all_but_last(the_dir), "DELE", actual_item))) && Common.filter_check("R", Common.last(the_dir), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter"))) {
      changeProxyToCurrentDir(this.uVFS.get_item_parent(Common.all_but_last(the_dir)));
      GenericClient c = this.uVFS.getClient(item);
      try {
        VRL vrl = new VRL(item.getProperty("url"));
        boolean exists = (c.stat(vrl.getPath()) != null);
        if (this.rnfr_file == null) {
          Common.trackSync("RENAME", this.rnfr_file_path, String.valueOf(item.getProperty("root_dir", "")) + item.getProperty("name", "") + (this.rnfr_file_path.endsWith("/") ? "/" : ""), false, 0L, 0L, SG("root_dir"), item.getProperty("privs"), uiSG("clientid"), "");
        } else if (!exists || (exists && overwrite) || this.rnfr_file.getProperty("url").equalsIgnoreCase(item.getProperty("url")) || this.rnfr_file.getProperty("url").toUpperCase().equals(String.valueOf(item.getProperty("url").toUpperCase()) + "/") || (new VRL(this.rnfr_file.getProperty("url"))).getPath().equalsIgnoreCase(vrl.getPath())) {
          if (vrl.toString().length() > ServerStatus.IG("max_url_length"))
            throw new IOException("File url length too long:" + vrl.toString().length() + " vs. " + ServerStatus.IG("max_url_length")); 
          SearchHandler.buildEntry(this.rnfr_file, this.uVFS, true, true);
          UserTools.updatePrivpath(uiSG("listen_ip_port"), uiSG("user_name"), String.valueOf(this.rnfr_file.getProperty("root_dir", "")) + this.rnfr_file.getProperty("name", ""), the_dir, item, null, this.uVFS);
          if (overwrite && !vrl.getPath().equalsIgnoreCase((new VRL(this.rnfr_file.getProperty("url"))).getPath()))
            if (c.stat((new VRL(this.rnfr_file.getProperty("url"))).getPath()) != null)
              c.delete(vrl.getPath());  
          if (c.rename((new VRL(this.rnfr_file.getProperty("url"))).getPath(), vrl.getPath())) {
            trackAndUpdateUploads(uiVG("lastUploadStats"), new VRL(this.rnfr_file.getProperty("url")), new VRL(item.getProperty("url")), "RENAME");
          } else {
            String srcPath = (new VRL(this.rnfr_file.getProperty("url"))).getCanonicalPath();
            String dstPath = (new VRL(item.getProperty("url"))).getCanonicalPath();
            if (dstPath.startsWith(srcPath) || !(new VRL(this.rnfr_file.getProperty("url"))).getProtocol().equalsIgnoreCase("file") || !(new VRL(item.getProperty("url"))).getProtocol().equalsIgnoreCase("file")) {
              this.not_done = ftp_write_command("550", LOC.G("%RNTO-bad%"));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
              this.rnfr_file = null;
              this.rnfr_file_path = null;
              return "%RNTO-bad%";
            } 
            if (this.rnfr_file.getProperty("type").equalsIgnoreCase("DIR")) {
              srcPath = String.valueOf(srcPath) + "/";
              dstPath = String.valueOf(dstPath) + "/";
              if (srcPath.equals(dstPath))
                dstPath = String.valueOf(dstPath) + " Copy/"; 
              Common.recurseCopy(srcPath, dstPath, true);
            } else {
              if (srcPath.equals(dstPath))
                dstPath = String.valueOf(dstPath) + " Copy"; 
              Common.recurseCopy(srcPath, dstPath, true);
            } 
            Common.recurseDelete(srcPath, false);
            trackAndUpdateUploads(uiVG("lastUploadStats"), new VRL(this.rnfr_file.getProperty("url")), new VRL(item.getProperty("url")), "RENAME");
          } 
          Properties fileItem1 = item;
          fileItem1 = (Properties)fileItem1.clone();
          Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Tracking rename:")) + the_dir);
          fileItem1.put("the_command", "RNTO");
          fileItem1.put("the_command_data", the_dir);
          fileItem1.put("the_file_path2", this.rnfr_file.getProperty("root_dir", ""));
          fileItem1.put("url_2", this.rnfr_file.getProperty("url", ""));
          fileItem1.put("the_file_name_2", this.rnfr_file.getProperty("name"));
          fileItem1.put("the_file_path", the_dir);
          fileItem1.put("the_file_name", item.getProperty("name"));
          fileItem1.put("the_file_size", this.rnfr_file.getProperty("size", "0"));
          fileItem1.put("the_file_speed", "0");
          fileItem1.put("the_file_start", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
          fileItem1.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
          fileItem1.put("the_file_error", "");
          fileItem1.put("the_file_status", "SUCCESS");
          fileItem1.put("the_file_type", this.rnfr_file.getProperty("type", ""));
          fileItem1.put("type", this.rnfr_file.getProperty("type", ""));
          Properties fileItem2 = (Properties)fileItem1.clone();
          fileItem2.put("url", fileItem2.getProperty("url_2"));
          fileItem2.put("the_file_name", fileItem2.getProperty("the_file_name_2"));
          Properties temp_rename = (Properties)fileItem1.clone();
          temp_rename.put("the_file_name", String.valueOf(temp_rename.getProperty("the_file_name_2")) + ":" + temp_rename.getProperty("the_file_name"));
          temp_rename.put("the_file_path", String.valueOf(temp_rename.getProperty("the_file_path2")) + ":" + temp_rename.getProperty("the_file_path"));
          temp_rename.put("the_file_type", this.rnfr_file.getProperty("type", ""));
          temp_rename.put("url", String.valueOf(temp_rename.getProperty("url_2")) + ":" + temp_rename.getProperty("url"));
          ServerStatus.thisObj.statTools.add_item_stat(this, temp_rename, "RENAME");
          do_event5("RENAME", fileItem1, fileItem2);
          if (ServerStatus.BG("generic_ftp_responses")) {
            this.not_done = ftp_write_command("250", LOC.G("Rename successful."));
          } else {
            this.not_done = ftp_write_command("250", LOC.G("%RNTO%"));
          } 
          boolean isDir = this.rnfr_file.getProperty("type").equalsIgnoreCase("DIR");
          item.put("type", isDir ? "DIR" : "FILE");
          Common.trackSync("RENAME", String.valueOf(this.rnfr_file.getProperty("root_dir", "")) + this.rnfr_file.getProperty("name", "") + (isDir ? "/" : ""), String.valueOf(item.getProperty("root_dir", "")) + item.getProperty("name", "") + (isDir ? "/" : ""), false, 0L, 0L, SG("root_dir"), item.getProperty("privs"), uiSG("clientid"), "");
          SearchHandler.buildEntry(item, this.uVFS, false, false);
          this.rnfr_file = null;
          this.rnfr_file_path = null;
          this.uVFS.reset();
        } else {
          this.not_done = ftp_write_command("550", LOC.G("%RNTO-error%"));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
          this.rnfr_file = null;
          this.rnfr_file_path = null;
          return "%RNTO-error%";
        } 
        return "";
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
    } 
    this.not_done = ftp_write_command("550", LOC.G("%RNTO-bad%"));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    this.rnfr_file = null;
    this.rnfr_file_path = null;
    return "%RNTO-bad%";
  }
  
  public String do_MKD(boolean mkdirs, String user_dir) throws Exception {
    uiPUT("the_command", "MKD");
    uiPUT("last_logged_command", "MKD");
    String the_dir = fixupDir(user_dir);
    if (!the_dir.endsWith("/"))
      the_dir = String.valueOf(the_dir) + "/"; 
    if (the_dir != null && the_dir.endsWith(" /"))
      the_dir = String.valueOf(the_dir.substring(0, the_dir.length() - 2)) + "/"; 
    String parentPath = this.uVFS.getRootVFS(the_dir, -1);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_item_parent(the_dir);
    if (item.getProperty("url").length() > ServerStatus.IG("max_url_length"))
      throw new IOException("File url length too long:" + item.getProperty("url").length() + " vs. " + ServerStatus.IG("max_url_length")); 
    if (check_access_privs(the_dir, uiSG("the_command"), item) && Common.filter_check("U", Common.last(the_dir), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter"))) {
      changeProxyToCurrentDir(item);
      Log.log("FTP_SERVER", 3, String.valueOf(LOC.G("Using item:")) + dir_item);
      GenericClient c = this.uVFS.getClient(dir_item);
      try {
        boolean result = false;
        if (mkdirs) {
          Common.verifyOSXVolumeMounted(item.getProperty("url"));
          result = c.makedirs((new VRL(item.getProperty("url"))).getPath());
        } else {
          result = c.makedir((new VRL(item.getProperty("url"))).getPath());
        } 
        if (!result && c.stat((new VRL(item.getProperty("url"))).getPath()) != null) {
          this.not_done = ftp_write_command(System.getProperty("crushftp.mkd.451", "521"), LOC.G("%MKD-exists%"));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
          return "%MKD-exists%";
        } 
        Common.trackSync("CHANGE", the_dir, null, true, 0L, 0L, SG("root_dir"), item.getProperty("privs"), uiSG("clientid"), "");
        if (!result) {
          this.not_done = ftp_write_command("550", LOC.G("%MKD-bad%"));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
          return "%MKD-bad%";
        } 
        setFolderPrivs(c, item);
        if (the_dir.startsWith(SG("root_dir")))
          the_dir = the_dir.substring(SG("root_dir").length() - 1); 
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
      c = this.uVFS.releaseClient(c);
      this.uVFS.reset();
      return "";
    } 
    this.not_done = ftp_write_command("550", LOC.G("%MKD-bad%"));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%MKD-bad%";
  }
  
  public void setFolderPrivs(GenericClient c, Properties item) throws Exception {
    if (!SG("default_owner_command").equals("")) {
      c.setOwner((new VRL(item.getProperty("url"))).getPath(), ServerStatus.change_vars_to_values_static(SG("default_owner_command"), this.user, this.user_info, this), "");
      Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set owner of new folder to:")) + SG("default_owner_command"));
    } else if (!item.getProperty("owner", "").equals("user") && !item.getProperty("owner", "").equals("owner")) {
      try {
        Properties parentItem = item;
        c.setOwner((new VRL(item.getProperty("url"))).getPath(), parentItem.getProperty("owner", "").trim(), "");
        Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set owner of new folder to:")) + parentItem.getProperty("owner", "").trim());
      } catch (Exception e) {
        Log.log("FTP_SERVER", 2, e);
      } 
    } 
    if (!SG("default_group_command").equals("")) {
      c.setGroup((new VRL(item.getProperty("url"))).getPath(), ServerStatus.change_vars_to_values_static(SG("default_group_command"), this.user, this.user_info, this), "");
      Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set group of new folder to:")) + SG("default_group_command"));
    } else if (!item.getProperty("group", "").equals("group")) {
      try {
        Properties parentItem = item;
        c.setGroup((new VRL(item.getProperty("url"))).getPath(), parentItem.getProperty("group", "").trim(), "");
        Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set group of new folder to:")) + parentItem.getProperty("group", "").trim());
      } catch (Exception exception) {}
    } 
    String folderPrivs = SG("default_folder_privs_command");
    if (folderPrivs == null || (folderPrivs.equals("") && !SG("default_privs_command").equals("")))
      folderPrivs = SG("default_privs_command"); 
    if (!folderPrivs.equals("")) {
      c.setMod((new VRL(item.getProperty("url"))).getPath(), folderPrivs, "");
      Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set privs of new folder to:")) + folderPrivs);
    } 
  }
  
  public String do_RMD(String user_dir) throws Exception {
    uiPUT("the_command", "RMD");
    uiPUT("last_logged_command", "RMD");
    String the_dir = user_dir;
    if (!uiSG("the_command_data").equals("")) {
      if (uiSG("the_command_data").startsWith("/")) {
        the_dir = uiSG("the_command_data");
      } else {
        the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
      } 
      the_dir = Common.dots(the_dir);
      if (the_dir.equals("/"))
        the_dir = SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
    } 
    String parentPath = this.uVFS.getRootVFS(the_dir, -1);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_fake_item(the_dir, "DIR");
    if (check_access_privs(the_dir, uiSG("the_command"), item) && item != null) {
      changeProxyToCurrentDir(item);
      GenericClient c = this.uVFS.getClient(item);
      try {
        Properties stat1 = c.stat((new VRL(item.getProperty("url"))).getPath());
        if (stat1 != null && stat1.getProperty("type").equalsIgnoreCase("dir")) {
          if (c.delete((new VRL(this.uVFS.get_item(the_dir).getProperty("url"))).getPath())) {
            this.not_done = ftp_write_command("250", LOC.G("%RMD%"));
          } else {
            this.not_done = ftp_write_command("550", LOC.G("%RMD-not_empty%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
            return "%RMD-not_empty%";
          } 
        } else {
          this.not_done = ftp_write_command("550", LOC.G("%RMD-not_found%"));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
          return "%RMD-not_found%";
        } 
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
    } else {
      this.not_done = ftp_write_command("550", LOC.G("%RMD-bad%"));
      doErrorEvent(new Exception(uiSG("lastLog")));
      uiVG("failed_commands").addElement((new Date()).getTime());
      return "%RMD-bad%";
    } 
    return "";
  }
  
  public void changeProxyToCurrentDir(Properties item) throws Exception {}
  
  public String do_SIZE() throws Exception {
    String the_dir = uiSG("current_dir");
    if (!uiSG("the_command_data").equals("")) {
      if (uiSG("the_command_data").startsWith("/")) {
        the_dir = uiSG("the_command_data");
      } else {
        the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
      } 
      the_dir = Common.dots(the_dir);
      if (the_dir.equals("/"))
        the_dir = SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
    } 
    String parentPath = this.uVFS.getRootVFS(the_dir, -1);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_item(the_dir);
    if (!check_access_privs(the_dir, uiSG("the_command"), dir_item) && (uiSG("the_command_data").toUpperCase().endsWith(".BIN") || uiSG("the_command_data").toUpperCase().endsWith(".ZIP"))) {
      uiPUT("the_command_data", uiSG("the_command_data").substring(0, uiSG("the_command_data").lastIndexOf(".")));
      the_dir = the_dir.substring(0, the_dir.lastIndexOf("."));
    } 
    if (check_access_privs(the_dir, uiSG("the_command"), item) && the_dir.indexOf(":filetree") < 0) {
      changeProxyToCurrentDir(item);
      if (item != null && item.getProperty("type", "").equals("FILE")) {
        this.not_done = ftp_write_command("213", item.getProperty("size"));
        return "";
      } 
      this.not_done = ftp_write_command("550", LOC.G("%SIZE-wrong%"));
      return "%SIZE-wrong%";
    } 
    this.not_done = ftp_write_command("550", LOC.G("File not found, or access denied."));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%SIZE-bad%";
  }
  
  public String do_MDTM() throws Exception {
    // Byte code:
    //   0: aload_0
    //   1: ldc_w 'the_command'
    //   4: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   7: ldc_w 'MFMT'
    //   10: invokevirtual equals : (Ljava/lang/Object;)Z
    //   13: istore_1
    //   14: aload_0
    //   15: ldc_w 'the_command'
    //   18: ldc_w 'MDTM'
    //   21: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   24: aload_0
    //   25: ldc_w 'last_logged_command'
    //   28: ldc_w 'MDTM'
    //   31: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   34: aload_0
    //   35: ldc_w 'current_dir'
    //   38: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   41: astore_2
    //   42: ldc ''
    //   44: astore_3
    //   45: aload_0
    //   46: ldc_w 'the_command_data'
    //   49: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   52: ldc ''
    //   54: invokevirtual equals : (Ljava/lang/Object;)Z
    //   57: ifne -> 516
    //   60: iload_1
    //   61: ifeq -> 115
    //   64: aload_0
    //   65: ldc_w 'the_command_data'
    //   68: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   71: ldc_w ' '
    //   74: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
    //   77: iconst_0
    //   78: aaload
    //   79: astore_3
    //   80: aload_0
    //   81: ldc_w 'the_command_data'
    //   84: aload_0
    //   85: ldc_w 'the_command_data'
    //   88: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   91: aload_0
    //   92: ldc_w 'the_command_data'
    //   95: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   98: ldc_w ' '
    //   101: invokevirtual indexOf : (Ljava/lang/String;)I
    //   104: iconst_1
    //   105: iadd
    //   106: invokevirtual substring : (I)Ljava/lang/String;
    //   109: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   112: goto -> 379
    //   115: aload_0
    //   116: ldc_w 'the_command_data'
    //   119: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   122: ldc_w ' '
    //   125: invokevirtual lastIndexOf : (Ljava/lang/String;)I
    //   128: iflt -> 158
    //   131: aload_0
    //   132: ldc_w 'the_command_data'
    //   135: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   138: aload_0
    //   139: ldc_w 'the_command_data'
    //   142: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   145: ldc_w ' '
    //   148: invokevirtual lastIndexOf : (Ljava/lang/String;)I
    //   151: invokevirtual substring : (I)Ljava/lang/String;
    //   154: invokevirtual trim : ()Ljava/lang/String;
    //   157: astore_3
    //   158: aload_3
    //   159: invokestatic parseLong : (Ljava/lang/String;)J
    //   162: pop2
    //   163: aload_3
    //   164: invokevirtual length : ()I
    //   167: iconst_5
    //   168: if_icmple -> 231
    //   171: new java/lang/StringBuffer
    //   174: dup
    //   175: aload_3
    //   176: invokevirtual trim : ()Ljava/lang/String;
    //   179: invokestatic parseLong : (Ljava/lang/String;)J
    //   182: invokestatic valueOf : (J)Ljava/lang/String;
    //   185: invokespecial <init> : (Ljava/lang/String;)V
    //   188: invokevirtual toString : ()Ljava/lang/String;
    //   191: astore_3
    //   192: aload_0
    //   193: ldc_w 'the_command_data'
    //   196: aload_0
    //   197: ldc_w 'the_command_data'
    //   200: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   203: iconst_0
    //   204: aload_0
    //   205: ldc_w 'the_command_data'
    //   208: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   211: invokevirtual length : ()I
    //   214: aload_3
    //   215: invokevirtual length : ()I
    //   218: isub
    //   219: invokevirtual substring : (II)Ljava/lang/String;
    //   222: invokevirtual trim : ()Ljava/lang/String;
    //   225: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   228: goto -> 379
    //   231: ldc ''
    //   233: astore_3
    //   234: goto -> 379
    //   237: astore #4
    //   239: aload_0
    //   240: ldc_w 'the_command_data'
    //   243: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   246: ldc_w ' '
    //   249: invokevirtual indexOf : (Ljava/lang/String;)I
    //   252: iflt -> 283
    //   255: aload_0
    //   256: ldc_w 'the_command_data'
    //   259: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   262: iconst_0
    //   263: aload_0
    //   264: ldc_w 'the_command_data'
    //   267: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   270: ldc_w ' '
    //   273: invokevirtual indexOf : (Ljava/lang/String;)I
    //   276: invokevirtual substring : (II)Ljava/lang/String;
    //   279: invokevirtual trim : ()Ljava/lang/String;
    //   282: astore_3
    //   283: aload_3
    //   284: invokestatic parseLong : (Ljava/lang/String;)J
    //   287: pop2
    //   288: aload_3
    //   289: invokevirtual length : ()I
    //   292: iconst_5
    //   293: if_icmple -> 343
    //   296: new java/lang/StringBuffer
    //   299: dup
    //   300: aload_3
    //   301: invokevirtual trim : ()Ljava/lang/String;
    //   304: invokestatic parseLong : (Ljava/lang/String;)J
    //   307: invokestatic valueOf : (J)Ljava/lang/String;
    //   310: invokespecial <init> : (Ljava/lang/String;)V
    //   313: invokevirtual toString : ()Ljava/lang/String;
    //   316: astore_3
    //   317: aload_0
    //   318: ldc_w 'the_command_data'
    //   321: aload_0
    //   322: ldc_w 'the_command_data'
    //   325: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   328: aload_3
    //   329: invokevirtual length : ()I
    //   332: iconst_1
    //   333: iadd
    //   334: invokevirtual substring : (I)Ljava/lang/String;
    //   337: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   340: goto -> 346
    //   343: ldc ''
    //   345: astore_3
    //   346: ldc_w 'FTP_SERVER'
    //   349: iconst_1
    //   350: new java/lang/StringBuffer
    //   353: dup
    //   354: ldc_w '4:dateNumber='
    //   357: invokespecial <init> : (Ljava/lang/String;)V
    //   360: aload_3
    //   361: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   364: invokevirtual toString : ()Ljava/lang/String;
    //   367: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   370: pop
    //   371: goto -> 379
    //   374: astore #5
    //   376: ldc ''
    //   378: astore_3
    //   379: aload_0
    //   380: ldc_w 'the_command_data'
    //   383: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   386: ldc_w '/'
    //   389: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   392: ifeq -> 406
    //   395: aload_0
    //   396: ldc_w 'the_command_data'
    //   399: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   402: astore_2
    //   403: goto -> 431
    //   406: new java/lang/StringBuffer
    //   409: dup
    //   410: aload_2
    //   411: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   414: invokespecial <init> : (Ljava/lang/String;)V
    //   417: aload_0
    //   418: ldc_w 'the_command_data'
    //   421: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   424: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   427: invokevirtual toString : ()Ljava/lang/String;
    //   430: astore_2
    //   431: aload_2
    //   432: invokestatic dots : (Ljava/lang/String;)Ljava/lang/String;
    //   435: astore_2
    //   436: aload_2
    //   437: ldc_w '/'
    //   440: invokevirtual equals : (Ljava/lang/Object;)Z
    //   443: ifeq -> 454
    //   446: aload_0
    //   447: ldc_w 'root_dir'
    //   450: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   453: astore_2
    //   454: aload_2
    //   455: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   458: ldc_w '/'
    //   461: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   464: ifeq -> 516
    //   467: aload_2
    //   468: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   471: aload_0
    //   472: ldc_w 'root_dir'
    //   475: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   478: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   481: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   484: ifne -> 516
    //   487: new java/lang/StringBuffer
    //   490: dup
    //   491: aload_0
    //   492: ldc_w 'root_dir'
    //   495: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   498: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   501: invokespecial <init> : (Ljava/lang/String;)V
    //   504: aload_2
    //   505: iconst_1
    //   506: invokevirtual substring : (I)Ljava/lang/String;
    //   509: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   512: invokevirtual toString : ()Ljava/lang/String;
    //   515: astore_2
    //   516: new java/util/GregorianCalendar
    //   519: dup
    //   520: invokespecial <init> : ()V
    //   523: astore #4
    //   525: aload_0
    //   526: getfield uVFS : Lcrushftp/server/VFS;
    //   529: aload_2
    //   530: invokevirtual get_item : (Ljava/lang/String;)Ljava/util/Properties;
    //   533: astore #5
    //   535: aload_0
    //   536: aload_2
    //   537: aload_0
    //   538: ldc_w 'the_command'
    //   541: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   544: aload #5
    //   546: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;Ljava/util/Properties;)Z
    //   549: ifeq -> 1253
    //   552: aload_2
    //   553: ldc_w ':filetree'
    //   556: invokevirtual indexOf : (Ljava/lang/String;)I
    //   559: ifge -> 1253
    //   562: new java/text/SimpleDateFormat
    //   565: dup
    //   566: ldc 'yyyyMMddHHmmss'
    //   568: getstatic java/util/Locale.US : Ljava/util/Locale;
    //   571: invokespecial <init> : (Ljava/lang/String;Ljava/util/Locale;)V
    //   574: astore #6
    //   576: aload_0
    //   577: aload #5
    //   579: invokevirtual changeProxyToCurrentDir : (Ljava/util/Properties;)V
    //   582: aload_0
    //   583: getfield uVFS : Lcrushftp/server/VFS;
    //   586: aload #5
    //   588: invokevirtual getClient : (Ljava/util/Properties;)Lcom/crushftp/client/GenericClient;
    //   591: astore #7
    //   593: aload #7
    //   595: new com/crushftp/client/VRL
    //   598: dup
    //   599: aload #5
    //   601: ldc_w 'url'
    //   604: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   607: invokespecial <init> : (Ljava/lang/String;)V
    //   610: invokevirtual getPath : ()Ljava/lang/String;
    //   613: invokevirtual stat : (Ljava/lang/String;)Ljava/util/Properties;
    //   616: astore #8
    //   618: aload #8
    //   620: ldc_w 'modified'
    //   623: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   626: invokestatic parseLong : (Ljava/lang/String;)J
    //   629: lstore #9
    //   631: aload_3
    //   632: invokevirtual trim : ()Ljava/lang/String;
    //   635: invokevirtual length : ()I
    //   638: ifle -> 1005
    //   641: aload_0
    //   642: aload_2
    //   643: ldc_w 'STOR'
    //   646: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;)Z
    //   649: ifeq -> 1005
    //   652: aload #6
    //   654: aload_3
    //   655: invokevirtual trim : ()Ljava/lang/String;
    //   658: invokevirtual parse : (Ljava/lang/String;)Ljava/util/Date;
    //   661: astore #11
    //   663: aload #4
    //   665: aload #11
    //   667: invokevirtual setTime : (Ljava/util/Date;)V
    //   670: aload #4
    //   672: bipush #11
    //   674: aload_0
    //   675: ldc_w 'timezone_offset'
    //   678: invokevirtual IG : (Ljava/lang/String;)I
    //   681: invokevirtual add : (II)V
    //   684: new java/util/Date
    //   687: dup
    //   688: aload #4
    //   690: invokevirtual getTime : ()Ljava/util/Date;
    //   693: invokevirtual getTime : ()J
    //   696: invokespecial <init> : (J)V
    //   699: astore #11
    //   701: ldc_w 'disable_mdtm_modifications'
    //   704: invokestatic BG : (Ljava/lang/String;)Z
    //   707: ifne -> 739
    //   710: aload #7
    //   712: new com/crushftp/client/VRL
    //   715: dup
    //   716: aload #5
    //   718: ldc_w 'url'
    //   721: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   724: invokespecial <init> : (Ljava/lang/String;)V
    //   727: invokevirtual getPath : ()Ljava/lang/String;
    //   730: aload #11
    //   732: invokevirtual getTime : ()J
    //   735: invokevirtual mdtm : (Ljava/lang/String;J)Z
    //   738: pop
    //   739: aload #7
    //   741: new com/crushftp/client/VRL
    //   744: dup
    //   745: aload #5
    //   747: ldc_w 'url'
    //   750: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   753: invokespecial <init> : (Ljava/lang/String;)V
    //   756: invokevirtual getPath : ()Ljava/lang/String;
    //   759: invokevirtual stat : (Ljava/lang/String;)Ljava/util/Properties;
    //   762: astore #8
    //   764: aload #8
    //   766: ldc_w 'modified'
    //   769: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   772: invokestatic parseLong : (Ljava/lang/String;)J
    //   775: lstore #9
    //   777: aload #4
    //   779: new java/util/Date
    //   782: dup
    //   783: lload #9
    //   785: invokespecial <init> : (J)V
    //   788: invokevirtual setTime : (Ljava/util/Date;)V
    //   791: aload #4
    //   793: bipush #11
    //   795: aload_0
    //   796: ldc_w 'timezone_offset'
    //   799: invokevirtual IG : (Ljava/lang/String;)I
    //   802: invokevirtual add : (II)V
    //   805: new java/util/Date
    //   808: dup
    //   809: aload #4
    //   811: invokevirtual getTime : ()Ljava/util/Date;
    //   814: invokevirtual getTime : ()J
    //   817: invokespecial <init> : (J)V
    //   820: astore #11
    //   822: ldc_w 'mdtm_gmt'
    //   825: invokestatic BG : (Ljava/lang/String;)Z
    //   828: ifeq -> 850
    //   831: aload #6
    //   833: new java/util/SimpleTimeZone
    //   836: dup
    //   837: iconst_0
    //   838: ldc_w 'GMT'
    //   841: invokespecial <init> : (ILjava/lang/String;)V
    //   844: invokestatic getInstance : (Ljava/util/TimeZone;)Ljava/util/Calendar;
    //   847: invokevirtual setCalendar : (Ljava/util/Calendar;)V
    //   850: iload_1
    //   851: ifeq -> 907
    //   854: aload_0
    //   855: aload_0
    //   856: ldc_w '213'
    //   859: new java/lang/StringBuffer
    //   862: dup
    //   863: ldc_w 'Modify='
    //   866: invokespecial <init> : (Ljava/lang/String;)V
    //   869: aload #6
    //   871: aload #11
    //   873: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   876: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   879: ldc_w '; '
    //   882: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   885: aload_0
    //   886: ldc_w 'the_command_data'
    //   889: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   892: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   895: invokevirtual toString : ()Ljava/lang/String;
    //   898: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   901: putfield not_done : Z
    //   904: goto -> 925
    //   907: aload_0
    //   908: aload_0
    //   909: ldc_w '213'
    //   912: aload #6
    //   914: aload #11
    //   916: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   919: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   922: putfield not_done : Z
    //   925: ldc_w 'CHANGE'
    //   928: aload_2
    //   929: aconst_null
    //   930: iconst_0
    //   931: aload #5
    //   933: ldc_w 'size'
    //   936: ldc_w '0'
    //   939: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   942: invokestatic parseLong : (Ljava/lang/String;)J
    //   945: lload #9
    //   947: aload_0
    //   948: ldc_w 'root_dir'
    //   951: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   954: aload #5
    //   956: ldc_w 'privs'
    //   959: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   962: aload_0
    //   963: ldc_w 'clientid'
    //   966: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   969: ldc ''
    //   971: invokestatic trackSync : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    //   974: aload_0
    //   975: getfield uVFS : Lcrushftp/server/VFS;
    //   978: aload #7
    //   980: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   983: astore #7
    //   985: ldc ''
    //   987: areturn
    //   988: astore #11
    //   990: ldc_w 'FTP_SERVER'
    //   993: iconst_1
    //   994: aload #11
    //   996: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   999: pop
    //   1000: ldc2_w -1
    //   1003: lstore #9
    //   1005: lload #9
    //   1007: lconst_0
    //   1008: lcmp
    //   1009: ifge -> 1021
    //   1012: ldc_w 'disable_mdtm_modifications'
    //   1015: invokestatic BG : (Ljava/lang/String;)Z
    //   1018: ifeq -> 1172
    //   1021: aload #4
    //   1023: new java/util/Date
    //   1026: dup
    //   1027: lload #9
    //   1029: invokespecial <init> : (J)V
    //   1032: invokevirtual setTime : (Ljava/util/Date;)V
    //   1035: aload #4
    //   1037: bipush #11
    //   1039: aload_0
    //   1040: ldc_w 'timezone_offset'
    //   1043: invokevirtual IG : (Ljava/lang/String;)I
    //   1046: invokevirtual add : (II)V
    //   1049: new java/util/Date
    //   1052: dup
    //   1053: aload #4
    //   1055: invokevirtual getTime : ()Ljava/util/Date;
    //   1058: invokevirtual getTime : ()J
    //   1061: invokespecial <init> : (J)V
    //   1064: astore #11
    //   1066: ldc_w 'mdtm_gmt'
    //   1069: invokestatic BG : (Ljava/lang/String;)Z
    //   1072: ifeq -> 1094
    //   1075: aload #6
    //   1077: new java/util/SimpleTimeZone
    //   1080: dup
    //   1081: iconst_0
    //   1082: ldc_w 'GMT'
    //   1085: invokespecial <init> : (ILjava/lang/String;)V
    //   1088: invokestatic getInstance : (Ljava/util/TimeZone;)Ljava/util/Calendar;
    //   1091: invokevirtual setCalendar : (Ljava/util/Calendar;)V
    //   1094: iload_1
    //   1095: ifeq -> 1151
    //   1098: aload_0
    //   1099: aload_0
    //   1100: ldc_w '213'
    //   1103: new java/lang/StringBuffer
    //   1106: dup
    //   1107: ldc_w 'Modify='
    //   1110: invokespecial <init> : (Ljava/lang/String;)V
    //   1113: aload #6
    //   1115: aload #11
    //   1117: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   1120: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1123: ldc_w '; '
    //   1126: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1129: aload_0
    //   1130: ldc_w 'the_command_data'
    //   1133: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1136: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1139: invokevirtual toString : ()Ljava/lang/String;
    //   1142: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   1145: putfield not_done : Z
    //   1148: goto -> 974
    //   1151: aload_0
    //   1152: aload_0
    //   1153: ldc_w '213'
    //   1156: aload #6
    //   1158: aload #11
    //   1160: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   1163: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   1166: putfield not_done : Z
    //   1169: goto -> 974
    //   1172: aload_0
    //   1173: aload_0
    //   1174: ldc_w '550'
    //   1177: ldc_w '%MDTM-wrong%'
    //   1180: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   1183: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   1186: putfield not_done : Z
    //   1189: aload_0
    //   1190: ldc_w 'failed_commands'
    //   1193: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   1196: new java/lang/StringBuffer
    //   1199: dup
    //   1200: invokespecial <init> : ()V
    //   1203: new java/util/Date
    //   1206: dup
    //   1207: invokespecial <init> : ()V
    //   1210: invokevirtual getTime : ()J
    //   1213: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   1216: invokevirtual toString : ()Ljava/lang/String;
    //   1219: invokevirtual addElement : (Ljava/lang/Object;)V
    //   1222: aload_0
    //   1223: getfield uVFS : Lcrushftp/server/VFS;
    //   1226: aload #7
    //   1228: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   1231: astore #7
    //   1233: ldc_w '%MDTM-wrong%'
    //   1236: areturn
    //   1237: astore #12
    //   1239: aload_0
    //   1240: getfield uVFS : Lcrushftp/server/VFS;
    //   1243: aload #7
    //   1245: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   1248: astore #7
    //   1250: aload #12
    //   1252: athrow
    //   1253: aload_0
    //   1254: aload_0
    //   1255: ldc_w '550'
    //   1258: ldc_w 'File not found, or access denied.'
    //   1261: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   1264: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   1267: putfield not_done : Z
    //   1270: aload_0
    //   1271: new java/lang/Exception
    //   1274: dup
    //   1275: aload_0
    //   1276: ldc_w 'lastLog'
    //   1279: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1282: invokespecial <init> : (Ljava/lang/String;)V
    //   1285: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   1288: aload_0
    //   1289: ldc_w 'failed_commands'
    //   1292: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   1295: new java/lang/StringBuffer
    //   1298: dup
    //   1299: invokespecial <init> : ()V
    //   1302: new java/util/Date
    //   1305: dup
    //   1306: invokespecial <init> : ()V
    //   1309: invokevirtual getTime : ()J
    //   1312: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   1315: invokevirtual toString : ()Ljava/lang/String;
    //   1318: invokevirtual addElement : (Ljava/lang/Object;)V
    //   1321: ldc_w '%MDTM-bad%'
    //   1324: areturn
    // Line number table:
    //   Java source line number -> byte code offset
    //   #3797	-> 0
    //   #3799	-> 14
    //   #3800	-> 24
    //   #3801	-> 34
    //   #3802	-> 42
    //   #3803	-> 45
    //   #3805	-> 60
    //   #3809	-> 64
    //   #3810	-> 80
    //   #3815	-> 115
    //   #3818	-> 158
    //   #3819	-> 163
    //   #3821	-> 171
    //   #3822	-> 192
    //   #3824	-> 231
    //   #3826	-> 237
    //   #3828	-> 239
    //   #3831	-> 283
    //   #3832	-> 288
    //   #3834	-> 296
    //   #3835	-> 317
    //   #3837	-> 343
    //   #3838	-> 346
    //   #3840	-> 374
    //   #3842	-> 376
    //   #3847	-> 379
    //   #3848	-> 406
    //   #3849	-> 431
    //   #3850	-> 436
    //   #3851	-> 454
    //   #3855	-> 516
    //   #3856	-> 525
    //   #3857	-> 535
    //   #3859	-> 562
    //   #3860	-> 576
    //   #3861	-> 582
    //   #3864	-> 593
    //   #3865	-> 618
    //   #3866	-> 631
    //   #3870	-> 641
    //   #3872	-> 652
    //   #3873	-> 663
    //   #3874	-> 670
    //   #3875	-> 684
    //   #3876	-> 701
    //   #3877	-> 739
    //   #3878	-> 764
    //   #3879	-> 777
    //   #3880	-> 791
    //   #3881	-> 805
    //   #3882	-> 822
    //   #3883	-> 850
    //   #3884	-> 907
    //   #3886	-> 925
    //   #3915	-> 974
    //   #3887	-> 985
    //   #3890	-> 988
    //   #3892	-> 990
    //   #3893	-> 1000
    //   #3896	-> 1005
    //   #3898	-> 1021
    //   #3899	-> 1035
    //   #3900	-> 1049
    //   #3901	-> 1066
    //   #3902	-> 1094
    //   #3903	-> 1151
    //   #3904	-> 1169
    //   #3908	-> 1172
    //   #3909	-> 1189
    //   #3915	-> 1222
    //   #3910	-> 1233
    //   #3914	-> 1237
    //   #3915	-> 1239
    //   #3916	-> 1250
    //   #3920	-> 1253
    //   #3921	-> 1270
    //   #3922	-> 1288
    //   #3923	-> 1321
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	1325	0	this	Lcrushftp/handlers/SessionCrush;
    //   14	1311	1	mfmt	Z
    //   42	1283	2	the_dir	Ljava/lang/String;
    //   45	1280	3	dateNumber	Ljava/lang/String;
    //   239	140	4	e	Ljava/lang/Exception;
    //   376	3	5	ee	Ljava/lang/Exception;
    //   525	800	4	cal	Ljava/util/Calendar;
    //   535	790	5	item	Ljava/util/Properties;
    //   576	677	6	sdf_yyyyMMddHHmmss2	Ljava/text/SimpleDateFormat;
    //   593	660	7	c	Lcom/crushftp/client/GenericClient;
    //   618	619	8	stat	Ljava/util/Properties;
    //   631	606	9	time	J
    //   663	325	11	d	Ljava/util/Date;
    //   990	15	11	e	Ljava/lang/Exception;
    //   1066	106	11	d	Ljava/util/Date;
    // Exception table:
    //   from	to	target	type
    //   158	234	237	java/lang/Exception
    //   283	371	374	java/lang/Exception
    //   593	974	1237	finally
    //   641	974	988	java/lang/Exception
    //   988	1222	1237	finally
  }
  
  public String get_PWD() {
    try {
      return uiSG("current_dir").substring(SG("root_dir").length() - 1);
    } catch (Exception e) {
      return uiSG("current_dir");
    } 
  }
  
  public String do_CWD() throws Exception {
    uiPUT("the_command", "CWD");
    uiPUT("last_logged_command", "CWD");
    if (uiSG("the_command_data").trim().equals(""))
      uiPUT("the_command_data", "."); 
    String originalCommandData = uiSG("the_command_data");
    uiPUT("the_command_data", Common.url_decode(uiSG("the_command_data")));
    if (uiSG("the_command_data").startsWith("//"))
      uiPUT("the_command_data", uiSG("the_command_data").substring(1)); 
    if (uiSG("the_command_data").startsWith("//") && !uiSG("the_command_data").endsWith("/"))
      uiPUT("the_command_data", String.valueOf(uiSG("the_command_data").substring(1)) + "/"); 
    if (uiSG("the_command_data").equals("~")) {
      uiPUT("current_dir", Common.replace_str(String.valueOf(SG("root_dir")) + this.user_info.getProperty("default_current_dir", ""), "//", "/"));
      this.not_done = ftp_write_command("250", LOC.G("\"$0\" CWD command successful.", get_PWD()));
    } else {
      String the_dir = uiSG("current_dir");
      if (uiSG("the_command_data").startsWith("/")) {
        the_dir = uiSG("the_command_data");
      } else {
        the_dir = String.valueOf(the_dir) + uiSG("the_command_data");
      } 
      if (!the_dir.endsWith("/"))
        the_dir = String.valueOf(the_dir) + "/"; 
      if (the_dir.equals("/"))
        the_dir = SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
      the_dir = Common.dots(the_dir);
      if (!the_dir.startsWith(SG("root_dir")))
        the_dir = String.valueOf(SG("root_dir")) + (the_dir.startsWith("/") ? the_dir.substring(1) : the_dir); 
      Properties item = null;
      if (System.getProperty("crushftp.ftp_cwd_validate", "true").equalsIgnoreCase("true")) {
        item = this.uVFS.get_item(the_dir);
      } else {
        item = this.uVFS.get_item_parent(the_dir);
      } 
      if (check_access_privs(the_dir, uiSG("the_command"), item)) {
        if (item == null && !the_dir.equals("/")) {
          this.not_done = ftp_write_command("550", LOC.G("$0 : No such file or directory.", the_dir));
          doErrorEvent(new Exception(uiSG("lastLog")));
          uiVG("failed_commands").addElement((new Date()).getTime());
          if (ServerStatus.BG("slow_directory_scanners") && uiVG("failed_commands").size() - 10 > 0)
            Thread.sleep((100 * (uiVG("failed_commands").size() - 10))); 
          return String.valueOf(the_dir) + ": " + LOC.G("No such file or directory.");
        } 
        if (the_dir.equals("/")) {
          uiPUT("current_dir", SG("root_dir"));
          this.not_done = ftp_write_command("250", LOC.G("\"$0\" CWD command successful.", get_PWD()));
          return "";
        } 
        if (item.getProperty("type").equals("DIR") || (item.getProperty("name").toLowerCase().endsWith(".zip") && originalCommandData.endsWith("/"))) {
          if (the_dir.equals(""))
            the_dir = SG("root_dir"); 
          uiPUT("current_dir", the_dir);
          if (ServerStatus.BG("generic_ftp_responses")) {
            this.not_done = ftp_write_command("250", LOC.G("Directory successfully changed."));
          } else {
            this.not_done = ftp_write_command("250", LOC.G("\"$0\" CWD command successful.", get_PWD()));
          } 
          if (!(new VRL(item.getProperty("url"))).getProtocol().equalsIgnoreCase("virtual")) {
            GenericClient c = this.uVFS.getClient(item);
            try {
              if (c.getConfig("server_type", "").toUpperCase().indexOf("UNIX") < 0 && c.getConfig("server_type", "").toUpperCase().indexOf("WIND") < 0)
                c.doCommand("CWD " + originalCommandData); 
            } finally {
              c = this.uVFS.releaseClient(c);
            } 
          } 
          return "";
        } 
        this.not_done = ftp_write_command("550", "\"" + uiSG("the_command_data") + "\": " + LOC.G("No such file or directory."));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        if (ServerStatus.BG("slow_directory_scanners") && uiVG("failed_commands").size() - 10 > 0)
          Thread.sleep((100 * (uiVG("failed_commands").size() - 10))); 
        return "%CWD-not found%";
      } 
      this.not_done = ftp_write_command("550", "\"" + uiSG("the_command_data") + "\": " + LOC.G("No such file or directory."));
      doErrorEvent(new Exception(uiSG("lastLog")));
      uiVG("failed_commands").addElement((new Date()).getTime());
      if (ServerStatus.BG("slow_directory_scanners") && uiVG("failed_commands").size() - 10 > 0)
        Thread.sleep((100 * (uiVG("failed_commands").size() - 10))); 
      return "%CWD-not found%";
    } 
    return "";
  }
  
  public String do_ChangePass(String theUser, String new_password) {
    String result = LOC.G("Password not changed.");
    if (!Common.checkPasswordRequirements(new_password, this.user.getProperty("password_history", "")).equals(""))
      return String.valueOf(LOC.G("ERROR:")) + " " + Common.checkPasswordRequirements(new_password, this.user.getProperty("password_history", "")); 
    boolean ok = false;
    if (!new_password.equals(uiSG("current_password"))) {
      String old_password = uiSG("current_password");
      String response = "";
      try {
        VFS realVfs = this.uVFS;
        if (this.expired_uVFS != null)
          realVfs = this.expired_uVFS; 
        Properties dir_item = realVfs.get_item(getRootDir(null, realVfs, this.user, false), -1);
        VRL vrl = new VRL(dir_item.getProperty("url"));
        if (!vrl.getProtocol().equalsIgnoreCase("file") && !vrl.getProtocol().equalsIgnoreCase("virtual") && !vrl.getProtocol().equalsIgnoreCase("s3") && !vrl.getProtocol().equalsIgnoreCase("s3crush") && !vrl.getProtocol().equalsIgnoreCase("smb") && ServerStatus.BG("change_remote_password")) {
          GenericClient c = realVfs.getClient(dir_item);
          try {
            if (c instanceof com.crushftp.client.HTTPClient) {
              String split = Common.makeBoundary();
              response = c.doCommand("SITE PASS " + split + " " + old_password + split + new_password);
            } else {
              response = c.doCommand("SITE PASS " + new_password);
              if (response.startsWith("2"))
                response = c.doCommand("SITE PASS " + new_password); 
            } 
            if (response.startsWith("2"))
              ok = true; 
          } finally {
            c = this.uVFS.releaseClient(c);
          } 
        } else {
          UserTools.changeUsername(uiSG("listen_ip_port"), theUser, theUser, ServerStatus.thisObj.common_code.encode_pass(new_password, ServerStatus.SG("password_encryption"), this.user.getProperty("salt", "")));
          Properties tempUser = UserTools.ut.getUser(uiSG("listen_ip_port"), theUser, false);
          if (tempUser.containsKey("expire_password_when")) {
            Calendar gc = new GregorianCalendar();
            gc.setTime(new Date());
            gc.add(5, IG("expire_password_days"));
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
            String s = sdf.format(gc.getTime());
            tempUser.put("expire_password_when", s);
            tempUser.put("expire_password_days", (new StringBuffer(String.valueOf(IG("expire_password_days")))).toString());
          } 
          tempUser.put("auto_set_pass", "false");
          tempUser.put("password", ServerStatus.thisObj.common_code.encode_pass(new_password, ServerStatus.SG("password_encryption"), this.user.getProperty("salt", "")));
          String history = Common.getPasswordHistory(old_password, tempUser.getProperty("password_history", ""));
          tempUser.put("password_history", Common.getPasswordHistory(new_password, history));
          if (!ServerStatus.siBG("dmz_mode"))
            UserTools.writeUser(uiSG("listen_ip_port"), theUser, tempUser); 
          ok = true;
          response = "214 " + LOC.G("Password changed.");
        } 
      } catch (Exception e) {
        Log.log("LOGIN", 0, e);
        return e.getMessage();
      } 
      result = response.substring(4);
      if (ok) {
        uiPUT("current_password", new_password);
        Properties p = new Properties();
        p.put("user_name", theUser);
        p.put("old_password", old_password);
        p.put("new_password", new_password);
        runPlugin("changePass", p);
        ServerStatus.thisObj.runAlerts("password_change", this);
      } 
    } 
    return result;
  }
  
  public void kill_active_socks() {
    while (this.data_socks.size() > 0) {
      try {
        ((Socket)this.data_socks.remove(0)).close();
      } catch (Exception e) {
        Log.log("FTP_SERVER", 1, e);
      } 
    } 
  }
  
  public String fixupDir(String user_dir) {
    if (user_dir == null) {
      String str = Common.url_decode(uiSG("current_dir"));
      if (!uiSG("the_command_data").equals("")) {
        if (uiSG("the_command_data").startsWith("/")) {
          str = uiSG("the_command_data");
        } else {
          str = String.valueOf(str) + uiSG("the_command_data");
        } 
        str = Common.dots(str);
        if (str.equals("/"))
          str = SG("root_dir"); 
        if (str.toUpperCase().startsWith("/") && !str.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
          str = String.valueOf(SG("root_dir")) + str.substring(1); 
      } 
      uiPUT("the_command_data", Common.dots(uiSG("the_command_data")));
      return str;
    } 
    String the_dir = Common.dots(Common.url_decode(user_dir));
    if (the_dir.equals("/"))
      the_dir = SG("root_dir"); 
    if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
      the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
    return the_dir;
  }
  
  public void setupRootDir(String domain, boolean reset) throws Exception {
    if (this.user != null) {
      if (ServerStatus.BG("jailproxy") && getProperty("default_current_dir_unlocked", "false").equals("false")) {
        this.user.put("root_dir", getRootDir(domain, this.uVFS, this.user, reset));
      } else {
        if (!this.user_info.containsKey("configured_root_dir"))
          this.user_info.put("configured_root_dir", getRootDir(domain, this.uVFS, this.user, reset, false)); 
        this.user.put("root_dir", uiSG("configured_root_dir"));
        uiPUT("current_dir", String.valueOf(SG("root_dir")) + this.uVFS.user_info.getProperty("default_current_dir", "/").substring(1));
      } 
      this.user_info.put("root_dir", this.user.getProperty("root_dir"));
    } 
  }
  
  public static String getRootDir(String domain, VFS uVFS, Properties user, boolean reset) {
    return getRootDir(domain, uVFS, user, reset, true);
  }
  
  public static String getRootDir(String domain, VFS uVFS, Properties user, boolean reset, boolean include_default_current_dir) {
    if (uVFS == null)
      return "/"; 
    String root_dir = "/";
    Vector v = new Vector();
    try {
      uVFS.getListing(v, "/");
    } catch (Exception exception) {}
    if (reset)
      uVFS.reset(); 
    Properties dir_item = null;
    Properties names = new Properties();
    int x;
    for (x = 0; x < v.size(); x++) {
      Properties p = v.elementAt(x);
      if (p.getProperty("type").equalsIgnoreCase("DIR") && dir_item == null)
        dir_item = p; 
      if (!names.containsKey(p.getProperty("name")))
        names.put(p.getProperty("name"), ""); 
    } 
    if (dir_item != null && names.size() == 1)
      root_dir = "/" + dir_item.getProperty("name") + "/"; 
    if (include_default_current_dir && !uVFS.user_info.getProperty("default_current_dir", "").equals("/") && !uVFS.user_info.getProperty("default_current_dir", "").equals("")) {
      root_dir = String.valueOf(root_dir) + uVFS.user_info.getProperty("default_current_dir").substring(1);
      if (!root_dir.endsWith("/"))
        root_dir = String.valueOf(root_dir) + "/"; 
    } 
    if (domain != null && !domain.equals("") && user != null && user.get("domain_root_list") != null) {
      v = (Vector)user.get("domain_root_list");
      for (x = 0; x < v.size(); x++) {
        Properties p = v.elementAt(x);
        if (Common.do_search(p.getProperty("domain"), domain, false, 0)) {
          String path = p.getProperty("path");
          if (!path.startsWith("/"))
            path = "/" + path; 
          if (!path.endsWith("/"))
            path = String.valueOf(path) + "/"; 
          root_dir = path;
          break;
        } 
      } 
    } 
    return root_dir;
  }
  
  public int IG(String data) {
    int x = 0;
    try {
      x = Integer.parseInt(this.user.getProperty(data));
    } catch (Exception exception) {}
    return x;
  }
  
  public long LG(String data) {
    long x = 0L;
    try {
      x = Long.parseLong(this.user.getProperty(data));
    } catch (Exception exception) {}
    return x;
  }
  
  public String SG(String data) {
    String return_data = null;
    if (this.user != null)
      return_data = this.user.getProperty(data); 
    if (return_data == null)
      if (data.equals("root_dir")) {
        return_data = "/";
      } else {
        try {
          return_data = ServerStatus.SG(data);
        } catch (Exception e) {
          return_data = "";
        } 
      }  
    return return_data;
  }
  
  public boolean BG(String data) {
    boolean test = false;
    try {
      test = this.user.getProperty(data).equals("true");
    } catch (Exception exception) {}
    return test;
  }
  
  public void doFileAbortBlock(String the_command_data, boolean event) throws Exception {
    put("blockUploads", "true");
    Worker.startWorker(new Runnable(this) {
          final SessionCrush this$0;
          
          public void run() {
            try {
              Thread.sleep(2900L);
            } catch (Exception exception) {}
            this.this$0.put("blockUploads", "false");
          }
        });
    if (the_command_data.startsWith("ABOR") && event) {
      String filePath = the_command_data.substring(5).trim();
      if (!filePath.startsWith(SG("root_dir")))
        filePath = String.valueOf(SG("root_dir")) + filePath; 
      if (this.uVFS != null) {
        Properties fileItem = this.uVFS.get_item(filePath);
        if (fileItem != null) {
          fileItem.put("mark_error", "true");
          fileItem.put("the_file_error", "HTTP aborted");
          fileItem.put("the_file_status", "FAILURE");
          fileItem.put("the_file_path", the_command_data.substring(4).trim());
          fileItem.put("the_file_size", fileItem.getProperty("size"));
          fileItem.put("the_file_name", fileItem.getProperty("name"));
          do_event5("UPLOAD", fileItem);
        } 
      } 
    } 
  }
  
  public String stripRoot(String s) {
    if (s.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
      s = s.substring(SG("root_dir").length() - 1); 
    return s;
  }
  
  public String getStandardizedDir(String path) {
    if (path.startsWith("/WebInterface/function/"))
      return "/"; 
    path = Common.dots(path);
    if (!path.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
      path = String.valueOf(SG("root_dir")) + (path.startsWith("/") ? path.substring(1) : path); 
    if (path.indexOf("\\") >= 0)
      path = path.replace('\\', '/'); 
    if (!path.startsWith("/"))
      path = "/" + path; 
    return path;
  }
  
  public void killSession() {
    SharedSession.find("crushftp.usernames").remove(String.valueOf(Common.getPartialIp(uiSG("user_ip"))) + "_" + getId() + "_user");
    SharedSession.find("crushftp.sessions").remove(getId());
    uiPUT("CrushAuth", "");
    SharedSession.find("crushftp.usernames").remove(String.valueOf(Common.getPartialIp(uiSG("user_ip"))) + "_" + getId() + "_user");
    SharedSession.find("crushftp.usernames").remove("127.0.0.1_" + getId() + "_user");
    SharedSession.find("crushftp.usernames").remove(String.valueOf(Common.getPartialIp("127.0.0.1")) + "_" + getId() + "_user");
  }
  
  public boolean ftp_write_command(String code, String data) throws Exception {
    if (this.ftp != null)
      return this.ftp.write_command(code, data); 
    data = ServerStatus.thisObj.change_vars_to_values(data, this);
    Properties p = new Properties();
    p.put("command_code", code);
    p.put("command_data", data);
    runPlugin("afterCommand", p);
    data = p.getProperty("command_data", data);
    code = p.getProperty("command_code", code);
    data = ServerStatus.thisObj.common_code.format_message(code, data).trim();
    uiPUT("lastLog", data);
    add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + "_" + uiSG("sock_port") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("WROTE") + ": *" + data + "*", "STOR");
    return true;
  }
  
  public boolean ftp_write_command_logged(String code, String data, String logged_command) throws Exception {
    if (this.ftp != null)
      return this.ftp.write_command(code, data); 
    data = ServerStatus.thisObj.change_vars_to_values(data, this);
    Properties p = new Properties();
    p.put("command_code", code);
    p.put("command_data", data);
    runPlugin("afterCommand", p);
    data = p.getProperty("command_data", data);
    code = p.getProperty("command_code", code);
    data = ServerStatus.thisObj.common_code.format_message(code, data).trim();
    uiPUT("lastLog", data);
    add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + "_" + uiSG("sock_port") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("WROTE") + ": *" + data + "*", logged_command);
    return true;
  }
  
  public boolean ftp_write_command_raw(String data) throws Exception {
    if (this.ftp != null)
      return this.ftp.write_command_raw(data); 
    add_log(data, "STOR");
    return true;
  }
  
  public boolean ftp_write_command(String data) throws Exception {
    if (this.ftp != null)
      return this.ftp.write_command(data); 
    return true;
  }
  
  public boolean ftp_write_command_logged(String data, String logged_command) throws Exception {
    if (this.ftp != null)
      return this.ftp.write_command(data); 
    add_log(ServerStatus.thisObj.change_vars_to_values(data, this), logged_command);
    return true;
  }
  
  public static void doPaste(SessionCrush thisSession, StringBuffer status, String[] names, String destPath, String command) {
    try {
      String msg = "OK";
      for (int x = 0; x < names.length; x++) {
        String the_dir1 = names[x].trim();
        boolean skip = false;
        for (int xx = 0; xx < x; xx++) {
          String the_dir_tmp = names[xx].trim();
          if (the_dir1.startsWith(the_dir_tmp))
            skip = true; 
        } 
        if (!skip) {
          if (the_dir1.startsWith(thisSession.SG("root_dir")))
            the_dir1 = the_dir1.substring(thisSession.SG("root_dir").length() - 1); 
          String src_item_path = thisSession.getStandardizedDir(the_dir1);
          Properties item = thisSession.uVFS.get_item(src_item_path);
          if (item == null) {
            msg = String.valueOf(msg) + "\r\nItem not found:" + names[x];
          } else {
            String url1 = item.getProperty("url");
            if (!url1.endsWith("/") && item.getProperty("type", "").equalsIgnoreCase("DIR"))
              url1 = String.valueOf(url1) + "/"; 
            VRL vrl = new VRL(url1);
            Properties stat = null;
            GenericClient c = thisSession.uVFS.getClient(item);
            try {
              c.login(vrl.getUsername(), vrl.getPassword(), null);
              stat = c.stat(vrl.getPath());
            } finally {
              c = thisSession.uVFS.releaseClient(c);
            } 
            boolean deleteAllowed = thisSession.check_access_privs(src_item_path, "DELE");
            if (thisSession.check_access_privs(src_item_path, "RETR")) {
              String the_dir2 = Common.url_decode(destPath);
              if (the_dir2.startsWith(thisSession.SG("root_dir")))
                the_dir2 = the_dir2.substring(thisSession.SG("root_dir").length() - 1); 
              String dest_item_path = thisSession.getStandardizedDir(the_dir2);
              Properties item2 = thisSession.uVFS.get_item(dest_item_path);
              VRL vrl2 = new VRL(String.valueOf(item2.getProperty("url")) + (item2.getProperty("url").endsWith("/") ? "" : "/"));
              if (thisSession.check_access_privs(dest_item_path, "STOR")) {
                String addon = "";
                boolean ok = true;
                if ((new VRL(String.valueOf(vrl2.toString()) + vrl.getName() + (stat.getProperty("type").equalsIgnoreCase("DIR") ? "/" : ""))).toString().startsWith(vrl.toString())) {
                  ok = false;
                  String s1 = (new VRL(String.valueOf(vrl2.toString()) + vrl.getName() + (stat.getProperty("type").equalsIgnoreCase("DIR") ? "/" : ""))).toString();
                  String s2 = vrl.toString();
                  while (s1.endsWith("/"))
                    s1 = s1.substring(0, s1.length() - 1); 
                  while (s2.endsWith("/"))
                    s2 = s2.substring(0, s2.length() - 1); 
                  if (s1.equals(s2)) {
                    ok = true;
                    addon = String.valueOf(addon) + "_copy_" + Common.makeBoundary(3);
                  } else {
                    msg = String.valueOf(msg) + "\r\n" + LOC.G("Cannot copy item into itself.");
                  } 
                } 
                if (ok) {
                  thisSession.trackAndUpdateUploads(thisSession.uiVG("lastUploadStats"), vrl, vrl2, "RENAME");
                  SearchHandler.buildEntry(item, thisSession.uVFS, true, true);
                  the_dir2 = String.valueOf(the_dir2) + vrl.getName() + (stat.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "");
                  VRL vrl_dest = new VRL(vrl2 + Common.url_encode(vrl.getName()) + addon + (stat.getProperty("type").equalsIgnoreCase("DIR") ? "/" : ""));
                  GenericClient c1 = thisSession.uVFS.getClient(item);
                  c1.login(vrl.getUsername(), vrl.getPassword(), null);
                  Properties item_dest = (Properties)item.clone();
                  item_dest.put("url", vrl_dest.toString());
                  GenericClient c2 = thisSession.uVFS.getClient(item_dest);
                  c2.login(vrl_dest.getUsername(), vrl_dest.getPassword(), null);
                  synchronized (status) {
                    if (status.toString().equals("CANCELLED"))
                      throw new Exception("CANCELLED"); 
                  } 
                  Common.recurseCopy(vrl, vrl_dest, c1, c2, 0, true, status);
                  c1 = thisSession.uVFS.releaseClient(c1);
                  c2 = thisSession.uVFS.releaseClient(c2);
                  synchronized (status) {
                    if (status.toString().equals("CANCELLED"))
                      throw new Exception("CANCELLED"); 
                    status.setLength(0);
                    status.append("Updating search references...");
                  } 
                  SearchHandler.buildEntry(item2, thisSession.uVFS, false, false);
                  if (!the_dir1.startsWith(thisSession.SG("root_dir")))
                    the_dir1 = String.valueOf(thisSession.SG("root_dir")) + the_dir1.substring(1); 
                  if (!the_dir2.startsWith(thisSession.SG("root_dir")))
                    the_dir2 = String.valueOf(thisSession.SG("root_dir")) + the_dir2.substring(1); 
                  String the_dir_index1 = SearchHandler.getPreviewPath(item, "1", 1);
                  String the_dir_index2 = SearchHandler.getPreviewPath(item_dest, "1", 1);
                  String index1 = String.valueOf(ServerStatus.SG("previews_path")) + the_dir_index1.substring(1);
                  String index2 = String.valueOf(ServerStatus.SG("previews_path")) + the_dir_index2.substring(1);
                  if ((new File(String.valueOf(Common.all_but_last(index1)) + "../index.txt")).exists()) {
                    (new File(Common.all_but_last(index2))).mkdirs();
                    Common.copy(String.valueOf(Common.all_but_last(index1)) + "../index.txt", String.valueOf(Common.all_but_last(index2)) + "../index.txt", true);
                  } 
                  if (command.equalsIgnoreCase("cut_paste")) {
                    if (deleteAllowed) {
                      synchronized (status) {
                        if (status.toString().equals("CANCELLED"))
                          throw new Exception("CANCELLED"); 
                        status.setLength(0);
                        status.append("Removing original...");
                      } 
                      c1 = thisSession.uVFS.getClient(item);
                      c1.login(vrl.getUsername(), vrl.getPassword(), null);
                      Common.recurseDelete(vrl, false, c1, 0);
                      c1 = thisSession.uVFS.releaseClient(c2);
                      Common.trackSync("RENAME", the_dir1, the_dir2, false, 0L, 0L, thisSession.SG("root_dir"), item.getProperty("privs"), thisSession.uiSG("clientid"), "");
                    } else {
                      msg = String.valueOf(msg) + "\r\n" + LOC.G("Item $0 copied, but not 'cut' as you did not have delete permissions.", vrl.getName());
                      Common.trackSync("CHANGE", the_dir2, null, true, 0L, 0L, thisSession.SG("root_dir"), item.getProperty("privs"), thisSession.uiSG("clientid"), "");
                    } 
                  } else {
                    Common.trackSync("CHANGE", the_dir2, null, true, 0L, 0L, thisSession.SG("root_dir"), item.getProperty("privs"), thisSession.uiSG("clientid"), "");
                  } 
                  try {
                    synchronized (status) {
                      if (status.toString().equals("CANCELLED"))
                        throw new Exception("CANCELLED"); 
                      status.setLength(0);
                      status.append("Generating event for copy/paste...");
                    } 
                    Properties fileItem1 = item;
                    fileItem1 = (Properties)fileItem1.clone();
                    Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Tracking rename:")) + the_dir2);
                    fileItem1.put("the_command", "RNTO");
                    fileItem1.put("the_command_data", the_dir2);
                    fileItem1.put("the_file_path2", stat.getProperty("root_dir", ""));
                    fileItem1.put("url_2", stat.getProperty("url", ""));
                    fileItem1.put("the_file_name_2", stat.getProperty("name"));
                    fileItem1.put("the_file_path", the_dir2);
                    fileItem1.put("the_file_name", item.getProperty("name"));
                    fileItem1.put("the_file_size", stat.getProperty("size", "0"));
                    fileItem1.put("the_file_speed", "0");
                    fileItem1.put("the_file_start", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                    fileItem1.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                    fileItem1.put("the_file_error", "");
                    fileItem1.put("the_file_status", "SUCCESS");
                    Properties fileItem2 = (Properties)fileItem1.clone();
                    fileItem2.put("url", fileItem2.getProperty("url_2"));
                    fileItem2.put("the_file_name", fileItem2.getProperty("the_file_name_2"));
                    Properties temp_rename = (Properties)fileItem1.clone();
                    temp_rename.put("the_file_name", String.valueOf(temp_rename.getProperty("the_file_name_2")) + ":" + temp_rename.getProperty("the_file_name"));
                    temp_rename.put("the_file_path", String.valueOf(temp_rename.getProperty("the_file_path2")) + ":" + temp_rename.getProperty("the_file_path"));
                    temp_rename.put("url", String.valueOf(temp_rename.getProperty("url_2")) + ":" + temp_rename.getProperty("url"));
                    ServerStatus.thisObj.statTools.add_item_stat(thisSession, temp_rename, "RENAME");
                    thisSession.do_event5("RENAME", fileItem1, fileItem2);
                  } catch (Exception e) {
                    Log.log("SERVER", 1, e);
                  } 
                } 
              } else {
                msg = String.valueOf(msg) + "\r\n" + LOC.G("Cannot copy $0 because you don't have write permission here.", vrl.getName());
              } 
            } else {
              msg = String.valueOf(msg) + "\r\n" + LOC.G("Cannot copy $0 because you don't have read permission here.", vrl.getName());
            } 
          } 
        } 
      } 
      if (msg.equals("OK")) {
        msg = "COMPLETED:OK";
      } else {
        msg = "ERROR:" + msg;
      } 
      synchronized (status) {
        status.setLength(0);
        status.append(msg);
      } 
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
      synchronized (status) {
        status.setLength(0);
        status.append("ERROR:" + e);
      } 
    } 
  }
}
