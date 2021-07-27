package crushftp.handlers;

import com.crushftp.client.Common;
import com.crushftp.client.File_B;
import com.crushftp.client.File_S;
import com.crushftp.client.File_U;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.SimpleTimeZone;
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
  
  public SimpleDateFormat logDateFormat = (SimpleDateFormat)ServerStatus.thisObj.logDateFormat.clone();
  
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
  
  public void do_kill(IdleMonitor thread_killer_item) {
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
    //   120: ifeq -> 251
    //   123: getstatic com/crushftp/client/Common.dmz_mode : Z
    //   126: ifne -> 181
    //   129: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   132: aload_0
    //   133: ldc 'listen_ip_port'
    //   135: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   138: aload_0
    //   139: ldc_w 'user_name'
    //   142: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   145: ldc_w 'user_bytes_sent'
    //   148: new java/lang/StringBuffer
    //   151: dup
    //   152: aload_0
    //   153: ldc_w 'bytes_sent'
    //   156: invokevirtual uiLG : (Ljava/lang/String;)J
    //   159: aload_0
    //   160: ldc_w 'ratio_bytes_sent'
    //   163: invokevirtual uiLG : (Ljava/lang/String;)J
    //   166: ladd
    //   167: invokestatic valueOf : (J)Ljava/lang/String;
    //   170: invokespecial <init> : (Ljava/lang/String;)V
    //   173: invokevirtual toString : ()Ljava/lang/String;
    //   176: iconst_0
    //   177: iconst_1
    //   178: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   181: getstatic com/crushftp/client/Common.dmz_mode : Z
    //   184: ifne -> 251
    //   187: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   190: aload_0
    //   191: ldc 'listen_ip_port'
    //   193: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   196: aload_0
    //   197: ldc_w 'user_name'
    //   200: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   203: ldc_w 'user_bytes_received'
    //   206: new java/lang/StringBuffer
    //   209: dup
    //   210: aload_0
    //   211: ldc_w 'bytes_received'
    //   214: invokevirtual uiLG : (Ljava/lang/String;)J
    //   217: aload_0
    //   218: ldc_w 'ratio_bytes_received'
    //   221: invokevirtual uiLG : (Ljava/lang/String;)J
    //   224: ladd
    //   225: invokestatic valueOf : (J)Ljava/lang/String;
    //   228: invokespecial <init> : (Ljava/lang/String;)V
    //   231: invokevirtual toString : ()Ljava/lang/String;
    //   234: iconst_0
    //   235: iconst_1
    //   236: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   239: goto -> 251
    //   242: astore_2
    //   243: ldc 'SERVER'
    //   245: iconst_2
    //   246: aload_2
    //   247: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   250: pop
    //   251: aload_0
    //   252: getfield uVFS : Lcrushftp/server/VFS;
    //   255: ifnull -> 265
    //   258: aload_0
    //   259: getfield uVFS : Lcrushftp/server/VFS;
    //   262: invokevirtual free : ()V
    //   265: aload_0
    //   266: getfield uVFS : Lcrushftp/server/VFS;
    //   269: ifnull -> 304
    //   272: aload_0
    //   273: getfield server_item : Ljava/util/Properties;
    //   276: ldc_w 'serverType'
    //   279: ldc_w 'ftp'
    //   282: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   285: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   288: ldc_w 'HTTP'
    //   291: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   294: ifne -> 304
    //   297: aload_0
    //   298: getfield uVFS : Lcrushftp/server/VFS;
    //   301: invokevirtual disconnect : ()V
    //   304: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   307: aload_0
    //   308: getfield user_info : Ljava/util/Properties;
    //   311: invokevirtual remove_user : (Ljava/util/Properties;)V
    //   314: goto -> 331
    //   317: aload_0
    //   318: getfield pasv_socks : Ljava/util/Vector;
    //   321: iconst_0
    //   322: invokevirtual remove : (I)Ljava/lang/Object;
    //   325: checkcast java/net/ServerSocket
    //   328: invokevirtual close : ()V
    //   331: aload_0
    //   332: getfield pasv_socks : Ljava/util/Vector;
    //   335: invokevirtual size : ()I
    //   338: ifgt -> 317
    //   341: goto -> 345
    //   344: astore_2
    //   345: aload_0
    //   346: getfield server_item : Ljava/util/Properties;
    //   349: ldc_w 'serverType'
    //   352: ldc_w 'ftp'
    //   355: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   358: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   361: ldc_w 'HTTP'
    //   364: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   367: ifne -> 519
    //   370: invokestatic getProperties : ()Ljava/util/Properties;
    //   373: ldc_w 'crushftp.sftp.wait_transfers'
    //   376: ldc_w 'true'
    //   379: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   382: ldc_w 'true'
    //   385: invokevirtual equals : (Ljava/lang/Object;)Z
    //   388: ifeq -> 479
    //   391: invokestatic currentTimeMillis : ()J
    //   394: lstore_2
    //   395: goto -> 445
    //   398: ldc2_w 100
    //   401: invokestatic sleep : (J)V
    //   404: invokestatic currentTimeMillis : ()J
    //   407: lload_2
    //   408: lsub
    //   409: ldc2_w 3000
    //   412: lcmp
    //   413: ifle -> 445
    //   416: ldc 'SERVER'
    //   418: iconst_2
    //   419: new java/lang/StringBuffer
    //   422: dup
    //   423: ldc_w 'Waiting for STOR/RETR threads to finish...'
    //   426: invokespecial <init> : (Ljava/lang/String;)V
    //   429: invokestatic currentThread : ()Ljava/lang/Thread;
    //   432: invokevirtual getName : ()Ljava/lang/String;
    //   435: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   438: invokevirtual toString : ()Ljava/lang/String;
    //   441: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   444: pop
    //   445: aload_0
    //   446: getfield retr_files_pool_used : Ljava/util/Vector;
    //   449: invokevirtual size : ()I
    //   452: aload_0
    //   453: getfield stor_files_pool_used : Ljava/util/Vector;
    //   456: invokevirtual size : ()I
    //   459: iadd
    //   460: ifle -> 479
    //   463: invokestatic currentTimeMillis : ()J
    //   466: lload_2
    //   467: lsub
    //   468: ldc2_w 10000
    //   471: lcmp
    //   472: iflt -> 398
    //   475: goto -> 479
    //   478: astore_2
    //   479: aload_0
    //   480: aload_0
    //   481: getfield retr_files_pool_free : Ljava/util/Vector;
    //   484: invokevirtual kill_retr_files : (Ljava/util/Vector;)V
    //   487: aload_0
    //   488: aload_0
    //   489: getfield retr_files_pool_used : Ljava/util/Vector;
    //   492: invokevirtual kill_retr_files : (Ljava/util/Vector;)V
    //   495: goto -> 499
    //   498: astore_2
    //   499: aload_0
    //   500: aload_0
    //   501: getfield stor_files_pool_free : Ljava/util/Vector;
    //   504: invokevirtual kill_stor_files : (Ljava/util/Vector;)V
    //   507: aload_0
    //   508: aload_0
    //   509: getfield stor_files_pool_used : Ljava/util/Vector;
    //   512: invokevirtual kill_stor_files : (Ljava/util/Vector;)V
    //   515: goto -> 519
    //   518: astore_2
    //   519: aload_0
    //   520: getfield session_socks : Ljava/util/Vector;
    //   523: invokevirtual size : ()I
    //   526: iconst_1
    //   527: isub
    //   528: istore_2
    //   529: goto -> 618
    //   532: aload_0
    //   533: getfield session_socks : Ljava/util/Vector;
    //   536: iload_2
    //   537: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   540: checkcast java/net/Socket
    //   543: astore_3
    //   544: aload_0
    //   545: ldc_w 'user_protocol'
    //   548: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   551: ldc_w 'SFTP'
    //   554: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   557: ifeq -> 571
    //   560: aload_3
    //   561: instanceof crushftp/server/ssh/SSHSocket
    //   564: ifeq -> 571
    //   567: aload_3
    //   568: invokevirtual close : ()V
    //   571: aload_0
    //   572: ldc_w 'user_protocol'
    //   575: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   578: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   581: ldc_w 'HTTP'
    //   584: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   587: ifeq -> 599
    //   590: aload_3
    //   591: invokevirtual close : ()V
    //   594: goto -> 599
    //   597: astore #4
    //   599: aload_3
    //   600: invokevirtual isClosed : ()Z
    //   603: ifeq -> 615
    //   606: aload_0
    //   607: getfield session_socks : Ljava/util/Vector;
    //   610: aload_3
    //   611: invokevirtual remove : (Ljava/lang/Object;)Z
    //   614: pop
    //   615: iinc #2, -1
    //   618: iload_2
    //   619: ifge -> 532
    //   622: aload_0
    //   623: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   626: ifnull -> 718
    //   629: aload_0
    //   630: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   633: getfield sockOriginal : Ljava/net/Socket;
    //   636: sipush #2000
    //   639: invokevirtual setSoTimeout : (I)V
    //   642: aload_0
    //   643: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   646: getfield sockOriginal : Ljava/net/Socket;
    //   649: iconst_1
    //   650: iconst_2
    //   651: invokevirtual setSoLinger : (ZI)V
    //   654: aload_0
    //   655: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   658: getfield sockOriginal : Ljava/net/Socket;
    //   661: invokevirtual close : ()V
    //   664: goto -> 718
    //   667: astore_2
    //   668: goto -> 718
    //   671: aload_0
    //   672: getfield old_data_socks : Ljava/util/Vector;
    //   675: iconst_0
    //   676: invokevirtual remove : (I)Ljava/lang/Object;
    //   679: astore_2
    //   680: aload_2
    //   681: instanceof java/net/Socket
    //   684: ifeq -> 704
    //   687: aload_2
    //   688: checkcast java/net/Socket
    //   691: sipush #2000
    //   694: invokevirtual setSoTimeout : (I)V
    //   697: aload_2
    //   698: checkcast java/net/Socket
    //   701: invokevirtual close : ()V
    //   704: aload_2
    //   705: instanceof java/net/ServerSocket
    //   708: ifeq -> 718
    //   711: aload_2
    //   712: checkcast java/net/ServerSocket
    //   715: invokevirtual close : ()V
    //   718: aload_0
    //   719: getfield old_data_socks : Ljava/util/Vector;
    //   722: invokevirtual size : ()I
    //   725: ifgt -> 671
    //   728: goto -> 732
    //   731: astore_2
    //   732: aload_0
    //   733: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   736: ifnull -> 753
    //   739: aload_0
    //   740: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   743: getfield os : Ljava/io/OutputStream;
    //   746: invokevirtual close : ()V
    //   749: goto -> 753
    //   752: astore_2
    //   753: aload_0
    //   754: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   757: ifnull -> 774
    //   760: aload_0
    //   761: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   764: getfield is : Ljava/io/BufferedReader;
    //   767: invokevirtual close : ()V
    //   770: goto -> 774
    //   773: astore_2
    //   774: return
    // Line number table:
    //   Java source line number -> byte code offset
    //   #487	-> 0
    //   #488	-> 5
    //   #492	-> 9
    //   #494	-> 17
    //   #500	-> 18
    //   #502	-> 63
    //   #504	-> 64
    //   #508	-> 72
    //   #510	-> 82
    //   #511	-> 92
    //   #514	-> 104
    //   #516	-> 105
    //   #520	-> 113
    //   #522	-> 123
    //   #523	-> 181
    //   #526	-> 242
    //   #528	-> 243
    //   #530	-> 251
    //   #531	-> 265
    //   #533	-> 304
    //   #536	-> 314
    //   #537	-> 317
    //   #536	-> 331
    //   #539	-> 344
    //   #542	-> 345
    //   #544	-> 370
    //   #548	-> 391
    //   #549	-> 395
    //   #551	-> 398
    //   #552	-> 404
    //   #554	-> 416
    //   #549	-> 445
    //   #558	-> 478
    //   #564	-> 479
    //   #565	-> 487
    //   #567	-> 498
    //   #572	-> 499
    //   #573	-> 507
    //   #575	-> 518
    //   #579	-> 519
    //   #581	-> 532
    //   #584	-> 544
    //   #585	-> 571
    //   #587	-> 597
    //   #590	-> 599
    //   #579	-> 615
    //   #592	-> 622
    //   #596	-> 629
    //   #597	-> 642
    //   #598	-> 654
    //   #600	-> 667
    //   #606	-> 668
    //   #608	-> 671
    //   #609	-> 680
    //   #611	-> 687
    //   #612	-> 697
    //   #614	-> 704
    //   #606	-> 718
    //   #617	-> 731
    //   #622	-> 732
    //   #624	-> 752
    //   #629	-> 753
    //   #631	-> 773
    //   #635	-> 774
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	775	0	this	Lcrushftp/handlers/SessionCrush;
    //   0	775	1	thread_killer_item	Lcrushftp/handlers/IdleMonitor;
    //   64	8	2	e	Ljava/lang/Exception;
    //   105	8	2	e	Ljava/lang/Exception;
    //   243	8	2	e	Ljava/lang/Exception;
    //   395	83	2	start	J
    //   529	93	2	x	I
    //   544	71	3	sock	Ljava/net/Socket;
    //   680	38	2	obj	Ljava/lang/Object;
    // Exception table:
    //   from	to	target	type
    //   9	14	17	java/lang/Exception
    //   18	60	63	java/lang/Exception
    //   72	101	104	java/lang/Exception
    //   113	239	242	java/lang/Exception
    //   314	341	344	java/lang/Exception
    //   391	475	478	java/lang/Exception
    //   479	495	498	java/lang/Exception
    //   499	515	518	java/lang/Exception
    //   544	594	597	java/io/IOException
    //   629	664	667	java/lang/Exception
    //   668	728	731	java/lang/Exception
    //   732	749	752	java/lang/Exception
    //   753	770	773	java/lang/Exception
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
          pattern = Common.getPattern(searchPattern, true);
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
      if (item != null) {
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
        if (!locked && privs.indexOf("(delete)") >= 0 && ((privs2.indexOf("(inherited)") >= 0 && privs2.indexOf("(locked)") < 0) || this.uVFS.getCombinedPermissions().getProperty("acl_permissions", "false").equals("true")))
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
          String real_path = "";
          if (parentQuotaDir.startsWith("FILE:") || parentQuotaDir.startsWith("file:")) {
            real_path = (new VRL(parentQuotaDir)).getPath();
          } else {
            String parentAddon = parentQuotaDir;
            if (parentAddon.equals("parent_quota_dir"))
              parentAddon = ""; 
            real_path = String.valueOf((new VRL(item.getProperty("url"))).getCanonicalPath()) + "/" + parentAddon;
          } 
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
                  size = Common.recurseSize_U(real_path, 0L, thisSession); 
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
          String parentQuotaDir = SG("parent_quota_dir");
          String real_path = "";
          if (parentQuotaDir.startsWith("FILE:") || parentQuotaDir.startsWith("file:")) {
            real_path = (new VRL(parentQuotaDir)).getPath();
          } else {
            String parentAddon = SG("parent_quota_dir");
            if (parentAddon.equals("parent_quota_dir"))
              parentAddon = ""; 
            real_path = String.valueOf((new VRL(item.getProperty("url"))).getCanonicalPath()) + "/" + parentAddon;
          } 
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
    if (this.logDateFormat == null)
      this.logDateFormat = (SimpleDateFormat)ServerStatus.thisObj.logDateFormat.clone(); 
    check_data = String.valueOf(check_data) + " ";
    if (!check_data.trim().equals("DIR_LIST") && !log_data.trim().startsWith("RETR END") && !log_data.trim().startsWith("STOR END")) {
      Properties p = new Properties();
      p.put("the_command", check_data.substring(0, check_data.indexOf(" ")));
      p.put("user_time", this.logDateFormat.format(new Date()));
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
    if (this.logDateFormat == null)
      this.logDateFormat = (SimpleDateFormat)ServerStatus.thisObj.logDateFormat.clone(); 
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
          ServerStatus.thisObj.append_log(String.valueOf(data) + "\r\n", check_data);
          if (ServerStatus.BG("write_session_logs"))
            uiVG("user_log").addElement("SESSION|" + this.logDateFormat.format(new Date()) + "|" + data); 
        } 
      } catch (IOException iOException) {}
    } 
    drain_log();
  }
  
  public void drain_log() {
    if (!ServerStatus.BG("write_session_logs"))
      return; 
    StringBuffer sb = new StringBuffer();
    synchronized (SharedSession.sessionLock) {
      synchronized (uiVG("user_log")) {
        while (uiVG("user_log").size() > 0) {
          int loops = 0;
          while (uiVG("user_log").size() > 0 && loops++ < 1000) {
            try {
              sb.append(uiVG("user_log").remove(0).toString()).append("\r\n");
            } catch (Exception exception) {}
          } 
        } 
      } 
    } 
    synchronized (uiVG("user_log")) {
      try {
        Common.copyStreams(new ByteArrayInputStream(sb.toString().getBytes("UTF8")), new FileOutputStream(new File_S(String.valueOf(uiSG("user_log_path")) + uiSG("user_log_file")), true), true, true);
      } catch (IOException e) {
        Log.log("SERVER", 1, e);
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
      if (fileItem2_2 != null && fileItem2_2.containsKey("execute_log"))
        fileItem2.put("execute_log", fileItem2_2.get("execute_log")); 
    } finally {
      this.user = originalUser;
    } 
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
  
  public static void checkTempAccounts(Properties p, String serverGroup) {
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
      File_U[] accounts = (File_U[])(new File_U(String.valueOf(tempAccountsPath) + "accounts/")).listFiles();
      boolean found = false;
      boolean exausted_usage = false;
      if (accounts != null) {
        for (int x = 0; !found && x < accounts.length; x++) {
          try {
            File_U f = accounts[x];
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
                File_U f2 = f;
                int i = Integer.parseInt(pp.getProperty("I")) - 1;
                if (i < 0) {
                  exausted_usage = true;
                } else {
                  f2 = new File_U(f.getPath().replaceAll(",,i=" + (i + 1), ",,i=" + i));
                  f.renameTo(f2);
                  f = f2;
                } 
              } 
              if (!ServerStatus.thisObj.common_code.check_date_expired_roll(pp.getProperty("EX")))
                if (p.getProperty("username").equalsIgnoreCase(pp.getProperty("U")) && (p.getProperty("password").equalsIgnoreCase(pp.getProperty("P")) || p.getProperty("anyPass").equals("true"))) {
                  Properties tempUser = UserTools.ut.getUser(serverGroup, pp.getProperty("T"), true);
                  tempUser.put("username", p.getProperty("username"));
                  tempUser.put("password", p.getProperty("password"));
                  tempUser.put("account_expire", pp.getProperty("EX"));
                  Properties u = (Properties)p.get("user");
                  Properties info = (Properties)Common.readXMLObject_U(String.valueOf(f.getPath()) + "/INFO.XML");
                  info.remove("command");
                  info.remove("type");
                  u.putAll(tempUser);
                  u.putAll(pp);
                  u.putAll(info);
                  u.put("email", info.getProperty("emailTo"));
                  Vector events = (Vector)u.get("events");
                  if (events != null)
                    for (int i = 0; i < events.size(); i++) {
                      Properties event = events.elementAt(i);
                      if (event.getProperty("resolveShareEvent", "false").equals("true"))
                        if (event.getProperty("linkUser") != null) {
                          Properties linkUser = UserTools.ut.getUser(serverGroup, event.getProperty("linkUser"), true);
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
                  UserTools.mergeWebCustomizations(u, tempUser);
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
                    dir_item.put("url", (new File_S(String.valueOf(System.getProperty("crushftp.web", "")) + "WebInterface/" + fname)).toURI().toURL().toExternalForm());
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
      SimpleDateFormat logHour = new SimpleDateFormat("yyMMddHH");
      uiPUT("user_log_path", ServerStatus.change_vars_to_values_static(String.valueOf(ServerStatus.SG("user_log_location")) + logHour.format(new Date()) + "/", null, null, null));
      if (!(new File_S(uiSG("user_log_path"))).exists())
        (new File_S(uiSG("user_log_path"))).mkdirs(); 
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
    uiPUT("current_file", "");
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
    uiPUT("login_date_formatted", this.logDateFormat.format(new Date()));
    uiPUT("time", this.logDateFormat.format(new Date()));
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
    checkTempAccounts(temp_p, uiSG("listen_ip_port"));
    String templateUser = "";
    String SAMLResponse = uiSG("SAMLResponse");
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
            if (p.getProperty("CrushSSO_trusted", "").equals("true")) {
              this.user.put("site", Common.replace_str(this.user.getProperty("site").toUpperCase(), "(CONNECT)", ""));
              this.user.put("site", Common.replace_str(this.user.getProperty("site").toUpperCase(), "(PREF", "(NOTHING"));
              this.user.put("site", Common.replace_str(this.user.getProperty("site").toUpperCase(), "(USER", "(NOTHING"));
              this.user.put("site", Common.replace_str(this.user.getProperty("site").toUpperCase(), "(JOB", "(NOTHING"));
              this.user.put("site", Common.replace_str(this.user.getProperty("site").toUpperCase(), "(SERVER", "(NOTHING"));
              this.user.put("site", Common.replace_str(this.user.getProperty("site").toUpperCase(), "(UPDATE", "(NOTHING"));
              this.user.put("site", Common.replace_str(this.user.getProperty("site").toUpperCase(), "(REPORT", "(NOTHING"));
              this.user.put("site", Common.replace_str(this.user.getProperty("site").toUpperCase(), "(SHARE", "(NOTHING"));
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
                for (x = 0; x < (p.getProperty("templateUser", "").split(";")).length; x++) {
                  if (ichain.indexOf(p.getProperty("templateUser", "").split(";")[x].trim()) < 0)
                    ichain.addElement(p.getProperty("templateUser", "").split(";")[x].trim()); 
                } 
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
      if (this.uVFS != null) {
        this.uVFS.setUserPassIpPortProtocol(uiSG("user_name"), uiSG("current_password"), uiSG("user_ip"), uiIG("user_port"), uiSG("user_protocol"), this.user_info, this.user, this);
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
              (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes2.obj")).delete();
              oos = new ObjectOutputStream(new FileOutputStream(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes2.obj")));
              oos.writeObject(md4_hashes);
              oos.flush();
              oos.close();
              oos = null;
              (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes.obj")).delete();
              (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes2.obj")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "md4_hashes.obj"));
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
            if (Common.dmz_mode) {
              Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
              Properties action = new Properties();
              action.put("type", "GET:USER");
              action.put("id", Common.makeBoundary());
              action.put("SAMLResponse", SAMLResponse);
              action.put("username", theUser);
              action.put("password", thePass);
              action.put("need_response", "true");
              try {
                action.put("preferred_port", (new StringBuffer(String.valueOf((new VRL(p.getProperty("url"))).getPort()))).toString());
              } catch (Exception e) {
                Log.log("SERVER", 0, e);
              } 
              queue.addElement(action);
              action = UserTools.waitResponse(action, 60);
              if (!templateUser.equals("") && (action == null || !action.containsKey("user"))) {
                action = new Properties();
                action.put("type", "GET:USER");
                action.put("id", Common.makeBoundary());
                action.put("SAMLResponse", SAMLResponse);
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
                Properties tempPermission0 = null;
                for (int x = homes.size() - 1; x >= 0; x--) {
                  Properties tempVFS = homes.elementAt(x);
                  tempVFS.remove("");
                  Vector tempPermissionHomes = (Vector)tempVFS.get("vfs_permissions_object");
                  Properties tempPermission = tempPermissionHomes.elementAt(0);
                  tempPermission0 = tempPermission;
                  Enumeration keys = tempPermission.keys();
                  while (keys.hasMoreElements()) {
                    String key = keys.nextElement().toString();
                    if (key.indexOf("/", 1) > 0) {
                      permission.put("/" + p.getProperty("name").toUpperCase() + key, tempPermission.getProperty(key));
                      continue;
                    } 
                    if (tempPermission.size() == 1 && key.equals("/"))
                      permission.put("/INTERNAL/", tempPermission.getProperty(key)); 
                  } 
                } 
                Vector unique_items = new Vector();
                Enumeration perm_keys = permission.keys();
                while (perm_keys.hasMoreElements()) {
                  String key = perm_keys.nextElement().toString();
                  if (key.startsWith("/INTERNAL/") && !key.equals("/INTERNAL/")) {
                    String path = key.substring("/INTERNAL/".length(), key.length());
                    String unique_item = path.substring(0, path.indexOf("/"));
                    if (!unique_items.contains(unique_item))
                      unique_items.add(unique_item); 
                  } 
                } 
                if (unique_items.size() == 1) {
                  Enumeration keys = permission.keys();
                  Properties perm2 = new Properties();
                  while (keys.hasMoreElements()) {
                    String key = (String)keys.nextElement();
                    if (key.equals("/INTERNAL/" + unique_items.get(0) + "/"))
                      continue; 
                    if (key.equals("/INTERNAL/")) {
                      perm2.put("/INTERNAL/", permission.getProperty("/INTERNAL/" + unique_items.get(0) + "/"));
                      continue;
                    } 
                    if (key.equals("/")) {
                      perm2.put("/", permission.get("/"));
                      continue;
                    } 
                    String newKey = "/INTERNAL/" + key.substring(key.indexOf(String.valueOf(unique_items.get(0)) + "/") + (String.valueOf(unique_items.get(0)) + "/").length(), key.length());
                    perm2.put(newKey, permission.get(key));
                  } 
                  permission.clear();
                  permission.putAll(new HashMap(perm2));
                } 
                if (unique_items.size() > 1)
                  permission.put("/INTERNAL/", permission.getProperty("/")); 
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
        Log.log("LOGIN", 2, e);
        this.user_info.put("lastProxyError", (new StringBuffer(String.valueOf(e.getMessage()))).toString());
        boolean hack = checkHackUsernames(theUser);
        doErrorEvent(e);
        if (!hack && !theUser.equals("") && !theUser.equals("anonymous") && !Common.dmz_mode)
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
            dir_item.put("url", (new File_S(String.valueOf(System.getProperty("crushftp.web", "")) + "WebInterface/" + fname)).toURI().toURL().toExternalForm());
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
            if (!Common.dmz_mode)
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
      if (!hack && !theUser.equals("") && !theUser.equals("anonymous") && !Common.dmz_mode)
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
            the_url = Common.replace_str(the_url, String.valueOf(r1) + "working_dir" + addon + r2, String.valueOf((new File_S("./")).getCanonicalPath().replace('\\', '/')) + "/"); 
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
      if (Common.compare_with_hack_username(theUser, v.elementAt(i).toString())) {
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
    if (Common.dmz_mode) {
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
        if (ServerStatus.BG("send_dmz_error_events_to_internal"))
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
    // Byte code:
    //   0: aload_3
    //   1: invokevirtual length : ()I
    //   4: sipush #2000
    //   7: if_icmpgt -> 21
    //   10: aload #4
    //   12: invokevirtual length : ()I
    //   15: sipush #500
    //   18: if_icmple -> 55
    //   21: aload_0
    //   22: aload_0
    //   23: ldc_w '550'
    //   26: ldc_w 'Invalid'
    //   29: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   32: putfield not_done : Z
    //   35: aload_0
    //   36: new java/lang/Exception
    //   39: dup
    //   40: aload_0
    //   41: ldc_w 'lastLog'
    //   44: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   47: invokespecial <init> : (Ljava/lang/String;)V
    //   50: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   53: iconst_0
    //   54: ireturn
    //   55: ldc_w 'LOGIN'
    //   58: iconst_3
    //   59: new java/lang/Exception
    //   62: dup
    //   63: new java/lang/StringBuffer
    //   66: dup
    //   67: ldc_w 'INFO:Logging in with user:'
    //   70: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   73: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   76: invokespecial <init> : (Ljava/lang/String;)V
    //   79: aload_3
    //   80: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   83: invokevirtual toString : ()Ljava/lang/String;
    //   86: invokespecial <init> : (Ljava/lang/String;)V
    //   89: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   92: pop
    //   93: aload_0
    //   94: ldc_w 'last_logged_command'
    //   97: ldc_w 'USER'
    //   100: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   103: iconst_0
    //   104: istore #5
    //   106: ldc_w 'lowercase_usernames'
    //   109: invokestatic BG : (Ljava/lang/String;)Z
    //   112: ifeq -> 126
    //   115: aload_0
    //   116: ldc_w 'user_name'
    //   119: aload_3
    //   120: invokevirtual toLowerCase : ()Ljava/lang/String;
    //   123: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   126: aload_3
    //   127: ldc_w '!'
    //   130: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   133: ifeq -> 153
    //   136: aload_3
    //   137: iconst_1
    //   138: invokevirtual substring : (I)Ljava/lang/String;
    //   141: astore_3
    //   142: aload_0
    //   143: ldc_w 'user_name'
    //   146: aload_3
    //   147: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   150: iconst_1
    //   151: istore #5
    //   153: aload_0
    //   154: getfield user_info : Ljava/util/Properties;
    //   157: ldc_w 'user_name_original'
    //   160: ldc ''
    //   162: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   165: ldc ''
    //   167: invokevirtual equals : (Ljava/lang/Object;)Z
    //   170: ifne -> 194
    //   173: aload_0
    //   174: getfield user_info : Ljava/util/Properties;
    //   177: ldc_w 'user_name_original'
    //   180: ldc ''
    //   182: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   185: ldc_w 'anonymous'
    //   188: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   191: ifeq -> 202
    //   194: aload_0
    //   195: ldc_w 'user_name_original'
    //   198: aload_3
    //   199: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   202: aload_0
    //   203: getfield server_item : Ljava/util/Properties;
    //   206: ldc 'linkedServer'
    //   208: ldc ''
    //   210: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   213: ldc_w '@AutoDomain'
    //   216: invokevirtual equals : (Ljava/lang/Object;)Z
    //   219: ifeq -> 365
    //   222: aload_0
    //   223: ldc_w 'user_name_original'
    //   226: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   229: ldc_w '@'
    //   232: invokevirtual indexOf : (Ljava/lang/String;)I
    //   235: ifle -> 365
    //   238: aload_0
    //   239: ldc_w 'user_name_original'
    //   242: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   245: ldc_w '@'
    //   248: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
    //   251: aload_0
    //   252: ldc_w 'user_name_original'
    //   255: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   258: ldc_w '@'
    //   261: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
    //   264: arraylength
    //   265: iconst_1
    //   266: isub
    //   267: aaload
    //   268: astore #6
    //   270: aload #6
    //   272: invokestatic dots : (Ljava/lang/String;)Ljava/lang/String;
    //   275: astore #7
    //   277: aload #7
    //   279: bipush #47
    //   281: bipush #45
    //   283: invokevirtual replace : (CC)Ljava/lang/String;
    //   286: bipush #92
    //   288: bipush #45
    //   290: invokevirtual replace : (CC)Ljava/lang/String;
    //   293: bipush #37
    //   295: bipush #45
    //   297: invokevirtual replace : (CC)Ljava/lang/String;
    //   300: bipush #58
    //   302: bipush #45
    //   304: invokevirtual replace : (CC)Ljava/lang/String;
    //   307: bipush #59
    //   309: bipush #45
    //   311: invokevirtual replace : (CC)Ljava/lang/String;
    //   314: astore #7
    //   316: aload #6
    //   318: aload #7
    //   320: invokevirtual equals : (Ljava/lang/Object;)Z
    //   323: ifeq -> 365
    //   326: aload_0
    //   327: ldc_w 'user_name'
    //   330: aload_0
    //   331: ldc_w 'user_name_original'
    //   334: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   337: iconst_0
    //   338: aload_0
    //   339: ldc_w 'user_name_original'
    //   342: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   345: ldc_w '@'
    //   348: invokevirtual lastIndexOf : (Ljava/lang/String;)I
    //   351: invokevirtual substring : (II)Ljava/lang/String;
    //   354: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   357: aload_0
    //   358: ldc 'listen_ip_port'
    //   360: aload #6
    //   362: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   365: aload_0
    //   366: aconst_null
    //   367: invokevirtual setVFS : (Lcrushftp/server/VFS;)V
    //   370: iconst_0
    //   371: istore #6
    //   373: iconst_0
    //   374: istore #7
    //   376: aload #4
    //   378: astore #8
    //   380: getstatic com/crushftp/client/Common.dmz_mode : Z
    //   383: ifne -> 431
    //   386: aload #4
    //   388: ldc_w ':'
    //   391: invokevirtual contains : (Ljava/lang/CharSequence;)Z
    //   394: ifeq -> 427
    //   397: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   400: pop
    //   401: ldc_w 'otp_validated_logins'
    //   404: invokestatic BG : (Ljava/lang/String;)Z
    //   407: ifeq -> 427
    //   410: aload #4
    //   412: iconst_0
    //   413: aload #4
    //   415: ldc_w ':'
    //   418: invokevirtual indexOf : (Ljava/lang/String;)I
    //   421: invokevirtual substring : (II)Ljava/lang/String;
    //   424: goto -> 429
    //   427: aload #4
    //   429: astore #8
    //   431: aload_0
    //   432: aload_3
    //   433: aload #8
    //   435: iload_1
    //   436: iload_2
    //   437: invokevirtual verify_user : (Ljava/lang/String;Ljava/lang/String;ZZ)Z
    //   440: istore #7
    //   442: iload #7
    //   444: ifeq -> 1441
    //   447: aload_0
    //   448: getfield user : Ljava/util/Properties;
    //   451: ifnull -> 1441
    //   454: aload_0
    //   455: getfield user : Ljava/util/Properties;
    //   458: ldc_w 'otp_auth'
    //   461: ldc ''
    //   463: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   466: ldc_w 'true'
    //   469: invokevirtual equals : (Ljava/lang/Object;)Z
    //   472: ifeq -> 1441
    //   475: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   478: pop
    //   479: ldc_w 'otp_validated_logins'
    //   482: invokestatic BG : (Ljava/lang/String;)Z
    //   485: ifeq -> 1441
    //   488: ldc_w 'enterprise_level'
    //   491: invokestatic siIG : (Ljava/lang/String;)I
    //   494: ifgt -> 508
    //   497: new java/lang/Exception
    //   500: dup
    //   501: ldc_w 'OTP only valid for Enterprise licenses.'
    //   504: invokespecial <init> : (Ljava/lang/String;)V
    //   507: athrow
    //   508: iconst_1
    //   509: istore #9
    //   511: ldc_w 'v9_beta'
    //   514: invokestatic BG : (Ljava/lang/String;)Z
    //   517: ifeq -> 564
    //   520: aload_0
    //   521: getfield user : Ljava/util/Properties;
    //   524: new java/lang/StringBuffer
    //   527: dup
    //   528: ldc_w 'otp_auth_'
    //   531: invokespecial <init> : (Ljava/lang/String;)V
    //   534: aload_0
    //   535: ldc_w 'user_protocol'
    //   538: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   541: invokevirtual toLowerCase : ()Ljava/lang/String;
    //   544: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   547: invokevirtual toString : ()Ljava/lang/String;
    //   550: ldc_w 'true'
    //   553: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   556: ldc_w 'true'
    //   559: invokevirtual equals : (Ljava/lang/Object;)Z
    //   562: istore #9
    //   564: iload #9
    //   566: ifeq -> 1438
    //   569: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   572: getfield server_info : Ljava/util/Properties;
    //   575: ldc_w 'otp_tokens'
    //   578: invokevirtual containsKey : (Ljava/lang/Object;)Z
    //   581: ifne -> 604
    //   584: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   587: getfield server_info : Ljava/util/Properties;
    //   590: ldc_w 'otp_tokens'
    //   593: new java/util/Properties
    //   596: dup
    //   597: invokespecial <init> : ()V
    //   600: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   603: pop
    //   604: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   607: getfield server_info : Ljava/util/Properties;
    //   610: ldc_w 'otp_tokens'
    //   613: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   616: checkcast java/util/Properties
    //   619: astore #10
    //   621: aload #10
    //   623: new java/lang/StringBuffer
    //   626: dup
    //   627: aload_3
    //   628: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   631: invokespecial <init> : (Ljava/lang/String;)V
    //   634: aload_0
    //   635: ldc_w 'user_ip'
    //   638: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   641: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   644: invokevirtual toString : ()Ljava/lang/String;
    //   647: invokevirtual containsKey : (Ljava/lang/Object;)Z
    //   650: ifeq -> 883
    //   653: aload #4
    //   655: ldc_w ':'
    //   658: invokevirtual indexOf : (Ljava/lang/String;)I
    //   661: iflt -> 883
    //   664: aload #10
    //   666: new java/lang/StringBuffer
    //   669: dup
    //   670: aload_3
    //   671: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   674: invokespecial <init> : (Ljava/lang/String;)V
    //   677: aload_0
    //   678: ldc_w 'user_ip'
    //   681: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   684: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   687: invokevirtual toString : ()Ljava/lang/String;
    //   690: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   693: checkcast java/util/Properties
    //   696: astore #11
    //   698: invokestatic currentTimeMillis : ()J
    //   701: aload #11
    //   703: ldc_w 'time'
    //   706: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   709: invokestatic parseLong : (Ljava/lang/String;)J
    //   712: lsub
    //   713: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   716: pop
    //   717: ldc_w 'otp_token_timeout'
    //   720: invokestatic LG : (Ljava/lang/String;)J
    //   723: lcmp
    //   724: ifle -> 760
    //   727: aload #10
    //   729: new java/lang/StringBuffer
    //   732: dup
    //   733: aload_3
    //   734: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   737: invokespecial <init> : (Ljava/lang/String;)V
    //   740: aload_0
    //   741: ldc_w 'user_ip'
    //   744: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   747: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   750: invokevirtual toString : ()Ljava/lang/String;
    //   753: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
    //   756: pop
    //   757: goto -> 1441
    //   760: ldc_w 'LOGIN'
    //   763: iconst_1
    //   764: ldc_w 'CHALLENGE_OTP : Checking OTP token.'
    //   767: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   770: pop
    //   771: aload #4
    //   773: ldc_w ':'
    //   776: invokevirtual indexOf : (Ljava/lang/String;)I
    //   779: iflt -> 846
    //   782: aload #11
    //   784: ldc_w 'token'
    //   787: ldc ''
    //   789: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   792: aload #4
    //   794: aload #4
    //   796: ldc_w ':'
    //   799: invokevirtual lastIndexOf : (Ljava/lang/String;)I
    //   802: iconst_1
    //   803: iadd
    //   804: invokevirtual substring : (I)Ljava/lang/String;
    //   807: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   810: ifeq -> 846
    //   813: ldc_w 'LOGIN'
    //   816: iconst_1
    //   817: ldc_w 'CHALLENGE_OTP : OTP token is valid.'
    //   820: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   823: pop
    //   824: aload #4
    //   826: iconst_0
    //   827: aload #4
    //   829: ldc_w ':'
    //   832: invokevirtual lastIndexOf : (Ljava/lang/String;)I
    //   835: invokevirtual substring : (II)Ljava/lang/String;
    //   838: astore #4
    //   840: iconst_1
    //   841: istore #6
    //   843: goto -> 1441
    //   846: getstatic com/crushftp/client/Common.dmz_mode : Z
    //   849: ifne -> 1441
    //   852: aload_0
    //   853: getfield user_info : Ljava/util/Properties;
    //   856: ldc_w 'lastProxyError'
    //   859: ldc_w 'CHALLENGE_OTP:OTP invalid.'
    //   862: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   865: pop
    //   866: aload_0
    //   867: ldc_w 'user_logged_in'
    //   870: ldc_w 'false'
    //   873: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   876: aload_0
    //   877: iconst_0
    //   878: putfield not_done : Z
    //   881: iconst_0
    //   882: ireturn
    //   883: ldc_w 'temp_accounts_length'
    //   886: invokestatic IG : (Ljava/lang/String;)I
    //   889: invokestatic makeBoundary : (I)Ljava/lang/String;
    //   892: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   895: astore #11
    //   897: new java/util/Properties
    //   900: dup
    //   901: invokespecial <init> : ()V
    //   904: astore #12
    //   906: aload #10
    //   908: new java/lang/StringBuffer
    //   911: dup
    //   912: aload_3
    //   913: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   916: invokespecial <init> : (Ljava/lang/String;)V
    //   919: aload_0
    //   920: ldc_w 'user_ip'
    //   923: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   926: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   929: invokevirtual toString : ()Ljava/lang/String;
    //   932: aload #12
    //   934: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   937: pop
    //   938: aload #12
    //   940: ldc_w 'time'
    //   943: new java/lang/StringBuffer
    //   946: dup
    //   947: invokestatic currentTimeMillis : ()J
    //   950: invokestatic valueOf : (J)Ljava/lang/String;
    //   953: invokespecial <init> : (Ljava/lang/String;)V
    //   956: invokevirtual toString : ()Ljava/lang/String;
    //   959: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   962: pop
    //   963: aload #12
    //   965: ldc_w 'token'
    //   968: aload #11
    //   970: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   973: pop
    //   974: getstatic com/crushftp/client/Common.dmz_mode : Z
    //   977: ifne -> 1421
    //   980: ldc_w 'LOGIN'
    //   983: iconst_1
    //   984: ldc_w 'CHALLENGE_OTP : Sending the sms'
    //   987: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   990: pop
    //   991: ldc_w 'otp_url'
    //   994: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   997: ldc_w 'SMTP'
    //   1000: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   1003: ifne -> 1026
    //   1006: aload_0
    //   1007: getfield user : Ljava/util/Properties;
    //   1010: ldc_w 'phone'
    //   1013: ldc ''
    //   1015: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1018: ldc ''
    //   1020: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1023: ifeq -> 1404
    //   1026: new java/util/Properties
    //   1029: dup
    //   1030: invokespecial <init> : ()V
    //   1033: astore #13
    //   1035: aload #13
    //   1037: ldc_w 'server'
    //   1040: ldc_w 'smtp_server'
    //   1043: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1046: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1049: pop
    //   1050: aload #13
    //   1052: ldc_w 'user'
    //   1055: ldc_w 'smtp_user'
    //   1058: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1061: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1064: pop
    //   1065: aload #13
    //   1067: ldc_w 'pass'
    //   1070: ldc_w 'smtp_pass'
    //   1073: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1076: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1079: pop
    //   1080: aload #13
    //   1082: ldc_w 'ssl'
    //   1085: ldc_w 'smtp_ssl'
    //   1088: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1091: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1094: pop
    //   1095: aload #13
    //   1097: ldc_w 'html'
    //   1100: ldc_w 'smtp_html'
    //   1103: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1106: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1109: pop
    //   1110: aload #13
    //   1112: ldc_w 'to'
    //   1115: aload_0
    //   1116: getfield user : Ljava/util/Properties;
    //   1119: ldc_w 'email'
    //   1122: aload_0
    //   1123: getfield user : Ljava/util/Properties;
    //   1126: ldc_w 'emailTo'
    //   1129: ldc ''
    //   1131: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1134: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1137: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1140: pop
    //   1141: ldc_w 'smtp_from'
    //   1144: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1147: astore #14
    //   1149: ldc ''
    //   1151: astore #15
    //   1153: ldc ''
    //   1155: astore #16
    //   1157: ldc ''
    //   1159: astore #17
    //   1161: ldc_w 'CrushFTP Two Factor Authentication'
    //   1164: astore #18
    //   1166: new java/lang/StringBuffer
    //   1169: dup
    //   1170: ldc_w 'OTP password : '
    //   1173: invokespecial <init> : (Ljava/lang/String;)V
    //   1176: aload #11
    //   1178: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1181: invokevirtual toString : ()Ljava/lang/String;
    //   1184: astore #19
    //   1186: ldc_w 'Two Factor Auth'
    //   1189: invokestatic get_email_template : (Ljava/lang/String;)Ljava/util/Properties;
    //   1192: astore #20
    //   1194: aload #20
    //   1196: ifnull -> 1296
    //   1199: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   1202: aload #20
    //   1204: ldc_w 'emailReplyTo'
    //   1207: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1210: aload_0
    //   1211: invokevirtual change_vars_to_values : (Ljava/lang/String;Lcrushftp/handlers/SessionCrush;)Ljava/lang/String;
    //   1214: astore #15
    //   1216: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   1219: aload #20
    //   1221: ldc_w 'emailCC'
    //   1224: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1227: aload_0
    //   1228: invokevirtual change_vars_to_values : (Ljava/lang/String;Lcrushftp/handlers/SessionCrush;)Ljava/lang/String;
    //   1231: astore #16
    //   1233: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   1236: aload #20
    //   1238: ldc_w 'emailBCC'
    //   1241: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1244: aload_0
    //   1245: invokevirtual change_vars_to_values : (Ljava/lang/String;Lcrushftp/handlers/SessionCrush;)Ljava/lang/String;
    //   1248: astore #17
    //   1250: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   1253: aload #20
    //   1255: ldc_w 'emailSubject'
    //   1258: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1261: aload_0
    //   1262: invokevirtual change_vars_to_values : (Ljava/lang/String;Lcrushftp/handlers/SessionCrush;)Ljava/lang/String;
    //   1265: astore #18
    //   1267: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   1270: aload #20
    //   1272: ldc_w 'emailBody'
    //   1275: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1278: aload_0
    //   1279: invokevirtual change_vars_to_values : (Ljava/lang/String;Lcrushftp/handlers/SessionCrush;)Ljava/lang/String;
    //   1282: astore #19
    //   1284: aload #19
    //   1286: ldc_w '\{auth_token\}'
    //   1289: aload #11
    //   1291: invokevirtual replaceAll : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1294: astore #19
    //   1296: aload #13
    //   1298: ldc_w 'from'
    //   1301: aload #14
    //   1303: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1306: pop
    //   1307: aload #13
    //   1309: ldc_w 'to'
    //   1312: aload_0
    //   1313: getfield user : Ljava/util/Properties;
    //   1316: ldc_w 'email'
    //   1319: aload_0
    //   1320: getfield user : Ljava/util/Properties;
    //   1323: ldc_w 'emailTo'
    //   1326: ldc ''
    //   1328: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1331: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1334: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1337: pop
    //   1338: aload #13
    //   1340: ldc_w 'reply_to'
    //   1343: aload #15
    //   1345: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1348: pop
    //   1349: aload #13
    //   1351: ldc_w 'cc'
    //   1354: aload #16
    //   1356: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1359: pop
    //   1360: aload #13
    //   1362: ldc_w 'bcc'
    //   1365: aload #17
    //   1367: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1370: pop
    //   1371: aload #13
    //   1373: ldc_w 'body'
    //   1376: aload #19
    //   1378: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1381: pop
    //   1382: aload #13
    //   1384: ldc_w 'subject'
    //   1387: aload #18
    //   1389: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1392: pop
    //   1393: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   1396: aload #13
    //   1398: invokevirtual sendEmail : (Ljava/util/Properties;)V
    //   1401: goto -> 1421
    //   1404: aload_0
    //   1405: getfield user : Ljava/util/Properties;
    //   1408: ldc_w 'phone'
    //   1411: ldc ''
    //   1413: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1416: aload #11
    //   1418: invokestatic send_otp_for_auth_sms : (Ljava/lang/String;Ljava/lang/String;)V
    //   1421: aload_0
    //   1422: getfield user_info : Ljava/util/Properties;
    //   1425: ldc_w 'lastProxyError'
    //   1428: ldc_w 'CHALLENGE_OTP:OTP invalid.'
    //   1431: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1434: pop
    //   1435: goto -> 1441
    //   1438: iconst_1
    //   1439: istore #6
    //   1441: getstatic com/crushftp/client/Common.dmz_mode : Z
    //   1444: ifeq -> 1475
    //   1447: aload_0
    //   1448: getfield user : Ljava/util/Properties;
    //   1451: ifnull -> 1475
    //   1454: aload_0
    //   1455: getfield user : Ljava/util/Properties;
    //   1458: ldc_w 'otp_valid'
    //   1461: ldc_w 'false'
    //   1464: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1467: ldc_w 'true'
    //   1470: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1473: istore #6
    //   1475: iload #7
    //   1477: ifeq -> 1508
    //   1480: aload_0
    //   1481: getfield user : Ljava/util/Properties;
    //   1484: ifnull -> 1518
    //   1487: aload_0
    //   1488: getfield user : Ljava/util/Properties;
    //   1491: ldc_w 'otp_auth'
    //   1494: ldc ''
    //   1496: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1499: ldc_w 'true'
    //   1502: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1505: ifeq -> 1518
    //   1508: iload #7
    //   1510: ifeq -> 5373
    //   1513: iload #6
    //   1515: ifeq -> 5373
    //   1518: aload_0
    //   1519: ldc_w 'user_name'
    //   1522: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1525: ldc ''
    //   1527: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1530: ifne -> 1623
    //   1533: aload_3
    //   1534: ldc_w 'SSO_SAML'
    //   1537: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1540: ifeq -> 1563
    //   1543: aload #4
    //   1545: ldc_w 'none'
    //   1548: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1551: ifeq -> 1563
    //   1554: aload_0
    //   1555: ldc_w 'current_password'
    //   1558: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1561: astore #4
    //   1563: aload_0
    //   1564: ldc_w 'user_name'
    //   1567: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1570: astore_3
    //   1571: ldc_w 'crushftp.usernames'
    //   1574: invokestatic find : (Ljava/lang/String;)Lcrushftp/handlers/SharedSession;
    //   1577: new java/lang/StringBuffer
    //   1580: dup
    //   1581: aload_0
    //   1582: ldc_w 'user_ip'
    //   1585: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1588: invokestatic getPartialIp : (Ljava/lang/String;)Ljava/lang/String;
    //   1591: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1594: invokespecial <init> : (Ljava/lang/String;)V
    //   1597: ldc_w '_'
    //   1600: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1603: aload_0
    //   1604: invokevirtual getId : ()Ljava/lang/String;
    //   1607: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1610: ldc_w '_user'
    //   1613: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1616: invokevirtual toString : ()Ljava/lang/String;
    //   1619: aload_3
    //   1620: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)V
    //   1623: aload_0
    //   1624: ldc_w 'user_name'
    //   1627: aload_3
    //   1628: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   1631: aload_0
    //   1632: ldc_w 'current_password'
    //   1635: aload #4
    //   1637: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   1640: aload_0
    //   1641: getfield uVFS : Lcrushftp/server/VFS;
    //   1644: aload_3
    //   1645: aload #4
    //   1647: aload_0
    //   1648: ldc_w 'user_ip'
    //   1651: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1654: aload_0
    //   1655: ldc_w 'user_port'
    //   1658: invokevirtual uiIG : (Ljava/lang/String;)I
    //   1661: aload_0
    //   1662: ldc_w 'user_protocol'
    //   1665: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1668: aload_0
    //   1669: getfield user_info : Ljava/util/Properties;
    //   1672: aload_0
    //   1673: getfield user : Ljava/util/Properties;
    //   1676: aload_0
    //   1677: invokevirtual setUserPassIpPortProtocol : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/util/Properties;Ljava/util/Properties;Lcrushftp/handlers/SessionCrush;)V
    //   1680: ldc_w 'LOGIN'
    //   1683: iconst_2
    //   1684: ldc_w 'User $0 authenticated, VFS set to:$1'
    //   1687: aload_3
    //   1688: aload_0
    //   1689: getfield uVFS : Lcrushftp/server/VFS;
    //   1692: invokevirtual toString : ()Ljava/lang/String;
    //   1695: invokestatic G : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1698: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   1701: pop
    //   1702: ldc_w 'create_home_folder'
    //   1705: invokestatic BG : (Ljava/lang/String;)Z
    //   1708: ifeq -> 2093
    //   1711: new java/util/Vector
    //   1714: dup
    //   1715: invokespecial <init> : ()V
    //   1718: astore #9
    //   1720: aload_0
    //   1721: getfield uVFS : Lcrushftp/server/VFS;
    //   1724: aload #9
    //   1726: ldc '/'
    //   1728: invokevirtual getListing : (Ljava/util/Vector;Ljava/lang/String;)V
    //   1731: iconst_0
    //   1732: istore #10
    //   1734: goto -> 1803
    //   1737: aload #9
    //   1739: iload #10
    //   1741: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   1744: checkcast java/util/Properties
    //   1747: astore #11
    //   1749: aload #9
    //   1751: aload_0
    //   1752: getfield uVFS : Lcrushftp/server/VFS;
    //   1755: new java/lang/StringBuffer
    //   1758: dup
    //   1759: aload #11
    //   1761: ldc_w 'root_dir'
    //   1764: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1767: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1770: invokespecial <init> : (Ljava/lang/String;)V
    //   1773: aload #11
    //   1775: ldc_w 'name'
    //   1778: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1781: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1784: ldc '/'
    //   1786: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1789: invokevirtual toString : ()Ljava/lang/String;
    //   1792: invokevirtual get_item : (Ljava/lang/String;)Ljava/util/Properties;
    //   1795: iload #10
    //   1797: invokevirtual setElementAt : (Ljava/lang/Object;I)V
    //   1800: iinc #10, 1
    //   1803: iload #10
    //   1805: aload #9
    //   1807: invokevirtual size : ()I
    //   1810: if_icmplt -> 1737
    //   1813: aload_0
    //   1814: getfield uVFS : Lcrushftp/server/VFS;
    //   1817: getfield homes : Ljava/util/Vector;
    //   1820: astore #9
    //   1822: iconst_0
    //   1823: istore #10
    //   1825: goto -> 2068
    //   1828: aload #9
    //   1830: iload #10
    //   1832: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   1835: checkcast java/util/Properties
    //   1838: astore #11
    //   1840: aload #11
    //   1842: invokevirtual keys : ()Ljava/util/Enumeration;
    //   1845: astore #12
    //   1847: goto -> 2055
    //   1850: aload #11
    //   1852: aload #12
    //   1854: invokeinterface nextElement : ()Ljava/lang/Object;
    //   1859: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   1862: astore #13
    //   1864: aload #13
    //   1866: instanceof java/util/Properties
    //   1869: ifeq -> 2055
    //   1872: aload #13
    //   1874: checkcast java/util/Properties
    //   1877: astore #14
    //   1879: aload #14
    //   1881: ldc_w 'vItems'
    //   1884: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   1887: checkcast java/util/Vector
    //   1890: astore #15
    //   1892: aload #15
    //   1894: ifnull -> 2055
    //   1897: aload #15
    //   1899: iconst_0
    //   1900: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   1903: checkcast java/util/Properties
    //   1906: astore #16
    //   1908: aload #16
    //   1910: ldc_w 'url'
    //   1913: aload_0
    //   1914: getfield uVFS : Lcrushftp/server/VFS;
    //   1917: aload #16
    //   1919: ldc_w 'url'
    //   1922: ldc ''
    //   1924: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1927: invokevirtual updateUrlVariables : (Ljava/lang/String;)Ljava/lang/String;
    //   1930: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1933: pop
    //   1934: aload #16
    //   1936: ldc_w 'url'
    //   1939: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1942: ldc '/'
    //   1944: invokevirtual endsWith : (Ljava/lang/String;)Z
    //   1947: ifeq -> 2055
    //   1950: aload #16
    //   1952: ldc_w 'url'
    //   1955: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1958: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   1961: ldc_w 'FILE:/'
    //   1964: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   1967: ifeq -> 2055
    //   1970: aload #16
    //   1972: ldc_w 'url'
    //   1975: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1978: astore #17
    //   1980: aload #17
    //   1982: ldc_w '{username}'
    //   1985: aload_3
    //   1986: invokestatic replace_str : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1989: astore #17
    //   1991: aload #17
    //   1993: ldc_w '{user_name}'
    //   1996: aload_3
    //   1997: invokestatic replace_str : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   2000: astore #17
    //   2002: aload #17
    //   2004: invokestatic verifyOSXVolumeMounted : (Ljava/lang/String;)V
    //   2007: new com/crushftp/client/File_S
    //   2010: dup
    //   2011: new com/crushftp/client/VRL
    //   2014: dup
    //   2015: aload #17
    //   2017: invokespecial <init> : (Ljava/lang/String;)V
    //   2020: invokevirtual getPath : ()Ljava/lang/String;
    //   2023: invokespecial <init> : (Ljava/lang/String;)V
    //   2026: invokevirtual exists : ()Z
    //   2029: ifne -> 2055
    //   2032: new com/crushftp/client/File_S
    //   2035: dup
    //   2036: new com/crushftp/client/VRL
    //   2039: dup
    //   2040: aload #17
    //   2042: invokespecial <init> : (Ljava/lang/String;)V
    //   2045: invokevirtual getPath : ()Ljava/lang/String;
    //   2048: invokespecial <init> : (Ljava/lang/String;)V
    //   2051: invokevirtual mkdirs : ()Z
    //   2054: pop
    //   2055: aload #12
    //   2057: invokeinterface hasMoreElements : ()Z
    //   2062: ifne -> 1850
    //   2065: iinc #10, 1
    //   2068: iload #10
    //   2070: aload #9
    //   2072: invokevirtual size : ()I
    //   2075: if_icmplt -> 1828
    //   2078: goto -> 2093
    //   2081: astore #9
    //   2083: ldc_w 'LOGIN'
    //   2086: iconst_1
    //   2087: aload #9
    //   2089: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   2092: pop
    //   2093: aload_0
    //   2094: aconst_null
    //   2095: iconst_0
    //   2096: invokevirtual setupRootDir : (Ljava/lang/String;Z)V
    //   2099: ldc_w 'jailproxy'
    //   2102: invokestatic BG : (Ljava/lang/String;)Z
    //   2105: ifeq -> 2141
    //   2108: aload_0
    //   2109: ldc_w 'default_current_dir_unlocked'
    //   2112: ldc_w 'false'
    //   2115: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   2118: ldc_w 'false'
    //   2121: invokevirtual equals : (Ljava/lang/Object;)Z
    //   2124: ifeq -> 2141
    //   2127: aload_0
    //   2128: ldc_w 'current_dir'
    //   2131: aload_0
    //   2132: ldc_w 'root_dir'
    //   2135: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   2138: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   2141: aload_0
    //   2142: getfield user : Ljava/util/Properties;
    //   2145: ldc_w 'ip_list'
    //   2148: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   2151: ifnull -> 2403
    //   2154: new java/lang/StringBuffer
    //   2157: dup
    //   2158: aload_0
    //   2159: getfield user : Ljava/util/Properties;
    //   2162: ldc_w 'ip_list'
    //   2165: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2168: invokevirtual trim : ()Ljava/lang/String;
    //   2171: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2174: invokespecial <init> : (Ljava/lang/String;)V
    //   2177: ldc '\\r\\n'
    //   2179: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2182: invokevirtual toString : ()Ljava/lang/String;
    //   2185: astore #9
    //   2187: aload #9
    //   2189: ldc_w '\\r'
    //   2192: ldc_w '~'
    //   2195: invokestatic replace_str : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   2198: astore #9
    //   2200: new java/util/StringTokenizer
    //   2203: dup
    //   2204: aload #9
    //   2206: ldc_w '~'
    //   2209: invokespecial <init> : (Ljava/lang/String;Ljava/lang/String;)V
    //   2212: astore #10
    //   2214: aload #10
    //   2216: invokevirtual countTokens : ()I
    //   2219: istore #11
    //   2221: new java/util/Vector
    //   2224: dup
    //   2225: invokespecial <init> : ()V
    //   2228: astore #12
    //   2230: iconst_0
    //   2231: istore #13
    //   2233: goto -> 2340
    //   2236: aload #10
    //   2238: invokevirtual nextToken : ()Ljava/lang/String;
    //   2241: invokevirtual trim : ()Ljava/lang/String;
    //   2244: astore #14
    //   2246: new java/util/Properties
    //   2249: dup
    //   2250: invokespecial <init> : ()V
    //   2253: astore #15
    //   2255: aload #15
    //   2257: ldc_w 'type'
    //   2260: new java/lang/StringBuffer
    //   2263: dup
    //   2264: aload #14
    //   2266: iconst_0
    //   2267: invokevirtual charAt : (I)C
    //   2270: invokestatic valueOf : (C)Ljava/lang/String;
    //   2273: invokespecial <init> : (Ljava/lang/String;)V
    //   2276: invokevirtual toString : ()Ljava/lang/String;
    //   2279: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2282: pop
    //   2283: aload #15
    //   2285: ldc_w 'start_ip'
    //   2288: aload #14
    //   2290: iconst_1
    //   2291: aload #14
    //   2293: ldc_w ','
    //   2296: invokevirtual indexOf : (Ljava/lang/String;)I
    //   2299: invokevirtual substring : (II)Ljava/lang/String;
    //   2302: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2305: pop
    //   2306: aload #15
    //   2308: ldc_w 'stop_ip'
    //   2311: aload #14
    //   2313: aload #14
    //   2315: ldc_w ','
    //   2318: invokevirtual indexOf : (Ljava/lang/String;)I
    //   2321: iconst_1
    //   2322: iadd
    //   2323: invokevirtual substring : (I)Ljava/lang/String;
    //   2326: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2329: pop
    //   2330: aload #12
    //   2332: aload #15
    //   2334: invokevirtual addElement : (Ljava/lang/Object;)V
    //   2337: iinc #13, 1
    //   2340: iload #13
    //   2342: iload #11
    //   2344: if_icmplt -> 2236
    //   2347: goto -> 2379
    //   2350: astore #13
    //   2352: new java/lang/StringBuffer
    //   2355: dup
    //   2356: invokespecial <init> : ()V
    //   2359: aload #13
    //   2361: invokevirtual append : (Ljava/lang/Object;)Ljava/lang/StringBuffer;
    //   2364: invokevirtual toString : ()Ljava/lang/String;
    //   2367: ldc_w 'Interrupted'
    //   2370: invokevirtual indexOf : (Ljava/lang/String;)I
    //   2373: iflt -> 2379
    //   2376: aload #13
    //   2378: athrow
    //   2379: aload_0
    //   2380: getfield user : Ljava/util/Properties;
    //   2383: ldc_w 'ip_restrictions'
    //   2386: aload #12
    //   2388: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2391: pop
    //   2392: aload_0
    //   2393: getfield user : Ljava/util/Properties;
    //   2396: ldc_w 'ip_list'
    //   2399: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
    //   2402: pop
    //   2403: iconst_0
    //   2404: istore #9
    //   2406: new java/util/Vector
    //   2409: dup
    //   2410: invokespecial <init> : ()V
    //   2413: astore #10
    //   2415: aload_0
    //   2416: ldc_w 'hours_of_day'
    //   2419: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   2422: ldc ''
    //   2424: invokevirtual equals : (Ljava/lang/Object;)Z
    //   2427: ifne -> 2446
    //   2430: aload_0
    //   2431: ldc_w 'hours_of_day'
    //   2434: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   2437: ldc_w 'hours_of_day'
    //   2440: invokevirtual equals : (Ljava/lang/Object;)Z
    //   2443: ifeq -> 2460
    //   2446: aload_0
    //   2447: getfield user : Ljava/util/Properties;
    //   2450: ldc_w 'hours_of_day'
    //   2453: ldc_w '0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23'
    //   2456: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2459: pop
    //   2460: aload_0
    //   2461: getfield user : Ljava/util/Properties;
    //   2464: ldc_w 'allowed_protocols'
    //   2467: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   2470: ifnull -> 2489
    //   2473: aload_0
    //   2474: ldc_w 'allowed_protocols'
    //   2477: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   2480: ldc_w 'allowed_protocols'
    //   2483: invokevirtual equals : (Ljava/lang/Object;)Z
    //   2486: ifeq -> 2503
    //   2489: aload_0
    //   2490: getfield user : Ljava/util/Properties;
    //   2493: ldc_w 'allowed_protocols'
    //   2496: ldc_w ',ftp:0,ftps:0,sftp:0,http:0,https:0,webdav:0,'
    //   2499: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2502: pop
    //   2503: aload_0
    //   2504: ldc_w 'hours_of_day'
    //   2507: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   2510: ldc_w ','
    //   2513: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
    //   2516: astore #11
    //   2518: iconst_0
    //   2519: istore #12
    //   2521: goto -> 2568
    //   2524: aload #10
    //   2526: new java/lang/StringBuffer
    //   2529: dup
    //   2530: aload #11
    //   2532: iload #12
    //   2534: aaload
    //   2535: invokestatic parseInt : (Ljava/lang/String;)I
    //   2538: invokestatic valueOf : (I)Ljava/lang/String;
    //   2541: invokespecial <init> : (Ljava/lang/String;)V
    //   2544: invokevirtual toString : ()Ljava/lang/String;
    //   2547: invokevirtual addElement : (Ljava/lang/Object;)V
    //   2550: goto -> 2565
    //   2553: astore #13
    //   2555: ldc_w 'LOGIN'
    //   2558: iconst_1
    //   2559: aload #13
    //   2561: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   2564: pop
    //   2565: iinc #12, 1
    //   2568: iload #12
    //   2570: aload #11
    //   2572: arraylength
    //   2573: if_icmplt -> 2524
    //   2576: aload_0
    //   2577: ldc_w 'max_logins_ip'
    //   2580: invokevirtual IG : (Ljava/lang/String;)I
    //   2583: ifeq -> 2639
    //   2586: aload_0
    //   2587: ldc_w 'logins_ip_auto_kick'
    //   2590: invokevirtual BG : (Ljava/lang/String;)Z
    //   2593: ifeq -> 2639
    //   2596: aload_0
    //   2597: aconst_null
    //   2598: invokestatic count_users_ip : (Lcrushftp/handlers/SessionCrush;Ljava/lang/String;)I
    //   2601: aload_0
    //   2602: ldc_w 'max_logins_ip'
    //   2605: invokevirtual IG : (Ljava/lang/String;)I
    //   2608: if_icmple -> 2639
    //   2611: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   2614: aload_0
    //   2615: getfield user_info : Ljava/util/Properties;
    //   2618: invokevirtual kill_first_same_name_same_ip : (Ljava/util/Properties;)Z
    //   2621: istore #9
    //   2623: ldc2_w 5000
    //   2626: invokestatic sleep : (J)V
    //   2629: aload_0
    //   2630: aload_3
    //   2631: aload #4
    //   2633: iconst_0
    //   2634: iload_2
    //   2635: invokevirtual verify_user : (Ljava/lang/String;Ljava/lang/String;ZZ)Z
    //   2638: pop
    //   2639: iload #5
    //   2641: ifeq -> 2657
    //   2644: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   2647: aload_0
    //   2648: getfield user_info : Ljava/util/Properties;
    //   2651: iconst_1
    //   2652: invokevirtual kill_same_name_same_ip : (Ljava/util/Properties;Z)Z
    //   2655: istore #5
    //   2657: aload_0
    //   2658: ldc_w 'max_logins'
    //   2661: invokevirtual IG : (Ljava/lang/String;)I
    //   2664: ifge -> 2730
    //   2667: aload_0
    //   2668: aload_0
    //   2669: ldc_w '421'
    //   2672: new java/lang/StringBuffer
    //   2675: dup
    //   2676: ldc_w '%account_disabled%'
    //   2679: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2682: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2685: invokespecial <init> : (Ljava/lang/String;)V
    //   2688: ldc '\\r\\n'
    //   2690: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2693: ldc_w 'Control connection closed'
    //   2696: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2699: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2702: ldc_w '.'
    //   2705: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2708: invokevirtual toString : ()Ljava/lang/String;
    //   2711: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   2714: putfield not_done : Z
    //   2717: aload_0
    //   2718: ldc_w 'user_logged_in'
    //   2721: ldc_w 'false'
    //   2724: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   2727: goto -> 5239
    //   2730: ldc_w 'concurrent_users'
    //   2733: invokestatic siIG : (Ljava/lang/String;)I
    //   2736: ldc_w 'max_users'
    //   2739: invokestatic IG : (Ljava/lang/String;)I
    //   2742: iconst_1
    //   2743: iadd
    //   2744: if_icmplt -> 2820
    //   2747: aload_0
    //   2748: ldc_w 'ignore_max_logins'
    //   2751: invokevirtual BG : (Ljava/lang/String;)Z
    //   2754: ifne -> 2820
    //   2757: aload_0
    //   2758: aload_0
    //   2759: ldc_w '421'
    //   2762: new java/lang/StringBuffer
    //   2765: dup
    //   2766: ldc_w '%max_users_server%'
    //   2769: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2772: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2775: invokespecial <init> : (Ljava/lang/String;)V
    //   2778: ldc '\\r\\n'
    //   2780: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2783: ldc_w 'Control connection closed'
    //   2786: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2789: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2792: ldc_w '.'
    //   2795: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2798: invokevirtual toString : ()Ljava/lang/String;
    //   2801: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   2804: putfield not_done : Z
    //   2807: aload_0
    //   2808: ldc_w 'user_logged_in'
    //   2811: ldc_w 'false'
    //   2814: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   2817: goto -> 5239
    //   2820: ldc_w 'concurrent_users'
    //   2823: invokestatic siIG : (Ljava/lang/String;)I
    //   2826: ldc_w 'max_max_users'
    //   2829: invokestatic IG : (Ljava/lang/String;)I
    //   2832: iconst_1
    //   2833: iadd
    //   2834: if_icmplt -> 2900
    //   2837: aload_0
    //   2838: aload_0
    //   2839: ldc_w '421'
    //   2842: new java/lang/StringBuffer
    //   2845: dup
    //   2846: ldc_w '%max_max_users_server%'
    //   2849: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2852: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2855: invokespecial <init> : (Ljava/lang/String;)V
    //   2858: ldc '\\r\\n'
    //   2860: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2863: ldc_w 'Control connection closed'
    //   2866: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2869: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2872: ldc_w '.'
    //   2875: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2878: invokevirtual toString : ()Ljava/lang/String;
    //   2881: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   2884: putfield not_done : Z
    //   2887: aload_0
    //   2888: ldc_w 'user_logged_in'
    //   2891: ldc_w 'false'
    //   2894: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   2897: goto -> 5239
    //   2900: aload_0
    //   2901: getfield server_item : Ljava/util/Properties;
    //   2904: ldc_w 'connected_users'
    //   2907: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2910: invokestatic parseInt : (Ljava/lang/String;)I
    //   2913: aload_0
    //   2914: getfield server_item : Ljava/util/Properties;
    //   2917: ldc_w 'max_connected_users'
    //   2920: ldc_w '32768'
    //   2923: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   2926: invokestatic parseInt : (Ljava/lang/String;)I
    //   2929: if_icmple -> 2995
    //   2932: aload_0
    //   2933: aload_0
    //   2934: ldc_w '421'
    //   2937: new java/lang/StringBuffer
    //   2940: dup
    //   2941: ldc_w '%max_users_server%'
    //   2944: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2947: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2950: invokespecial <init> : (Ljava/lang/String;)V
    //   2953: ldc '\\r\\n'
    //   2955: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2958: ldc_w 'Control connection closed'
    //   2961: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   2964: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2967: ldc_w '.'
    //   2970: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2973: invokevirtual toString : ()Ljava/lang/String;
    //   2976: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   2979: putfield not_done : Z
    //   2982: aload_0
    //   2983: ldc_w 'user_logged_in'
    //   2986: ldc_w 'false'
    //   2989: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   2992: goto -> 5239
    //   2995: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   2998: getfield common_code : Lcrushftp/handlers/Common;
    //   3001: pop
    //   3002: aload_0
    //   3003: getfield user : Ljava/util/Properties;
    //   3006: ldc_w 'ip_restrictions'
    //   3009: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   3012: checkcast java/util/Vector
    //   3015: aload_0
    //   3016: ldc_w 'user_ip'
    //   3019: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   3022: invokestatic check_ip : (Ljava/util/Vector;Ljava/lang/String;)Z
    //   3025: ifne -> 3058
    //   3028: aload_0
    //   3029: aload_0
    //   3030: ldc_w '550'
    //   3033: ldc_w '%bad_ip%'
    //   3036: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3039: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3042: putfield not_done : Z
    //   3045: aload_0
    //   3046: ldc_w 'user_logged_in'
    //   3049: ldc_w 'false'
    //   3052: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   3055: goto -> 5239
    //   3058: ldc_w 'day_of_week_allow'
    //   3061: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   3064: new java/util/Date
    //   3067: dup
    //   3068: invokespecial <init> : ()V
    //   3071: invokestatic check_day_of_week : (Ljava/lang/String;Ljava/util/Date;)Z
    //   3074: ifne -> 3140
    //   3077: aload_0
    //   3078: aload_0
    //   3079: ldc_w '530'
    //   3082: new java/lang/StringBuffer
    //   3085: dup
    //   3086: ldc_w '%day_restricted%'
    //   3089: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3092: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   3095: invokespecial <init> : (Ljava/lang/String;)V
    //   3098: ldc '\\r\\n'
    //   3100: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3103: ldc_w 'Control connection closed'
    //   3106: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3109: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3112: ldc_w '.'
    //   3115: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3118: invokevirtual toString : ()Ljava/lang/String;
    //   3121: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3124: putfield not_done : Z
    //   3127: aload_0
    //   3128: ldc_w 'user_logged_in'
    //   3131: ldc_w 'false'
    //   3134: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   3137: goto -> 5239
    //   3140: aload_0
    //   3141: ldc_w 'user_protocol'
    //   3144: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   3147: aload_0
    //   3148: ldc_w 'allowed_protocols'
    //   3151: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   3154: invokestatic check_protocol : (Ljava/lang/String;Ljava/lang/String;)I
    //   3157: ifge -> 3223
    //   3160: aload_0
    //   3161: aload_0
    //   3162: ldc_w '530'
    //   3165: new java/lang/StringBuffer
    //   3168: dup
    //   3169: ldc_w 'This user is not allowed to use this protocol.'
    //   3172: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3175: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   3178: invokespecial <init> : (Ljava/lang/String;)V
    //   3181: ldc '\\r\\n'
    //   3183: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3186: ldc_w 'Control connection closed'
    //   3189: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3192: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3195: ldc_w '.'
    //   3198: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3201: invokevirtual toString : ()Ljava/lang/String;
    //   3204: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3207: putfield not_done : Z
    //   3210: aload_0
    //   3211: ldc_w 'user_logged_in'
    //   3214: ldc_w 'false'
    //   3217: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   3220: goto -> 5239
    //   3223: aload_0
    //   3224: aload_0
    //   3225: ldc_w 'user_protocol'
    //   3228: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   3231: invokestatic count_users_ip : (Lcrushftp/handlers/SessionCrush;Ljava/lang/String;)I
    //   3234: aload_0
    //   3235: ldc_w 'user_protocol'
    //   3238: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   3241: aload_0
    //   3242: ldc_w 'allowed_protocols'
    //   3245: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   3248: invokestatic check_protocol : (Ljava/lang/String;Ljava/lang/String;)I
    //   3251: if_icmple -> 3377
    //   3254: aload_0
    //   3255: aload_0
    //   3256: ldc_w '421'
    //   3259: new java/lang/StringBuffer
    //   3262: dup
    //   3263: ldc_w '%max_simultaneous_connections_ip%'
    //   3266: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3269: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   3272: invokespecial <init> : (Ljava/lang/String;)V
    //   3275: ldc_w ' '
    //   3278: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3281: ldc_w '(For this protocol.)'
    //   3284: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3287: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3290: ldc '\\r\\n'
    //   3292: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3295: ldc_w 'Control connection closed'
    //   3298: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3301: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3304: ldc_w '. ('
    //   3307: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3310: aload_0
    //   3311: aload_0
    //   3312: ldc_w 'user_protocol'
    //   3315: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   3318: invokestatic count_users_ip : (Lcrushftp/handlers/SessionCrush;Ljava/lang/String;)I
    //   3321: invokevirtual append : (I)Ljava/lang/StringBuffer;
    //   3324: ldc '/'
    //   3326: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3329: aload_0
    //   3330: ldc_w 'user_protocol'
    //   3333: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   3336: aload_0
    //   3337: ldc_w 'allowed_protocols'
    //   3340: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   3343: invokestatic check_protocol : (Ljava/lang/String;Ljava/lang/String;)I
    //   3346: invokevirtual append : (I)Ljava/lang/StringBuffer;
    //   3349: ldc_w ')'
    //   3352: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3355: invokevirtual toString : ()Ljava/lang/String;
    //   3358: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3361: putfield not_done : Z
    //   3364: aload_0
    //   3365: ldc_w 'user_logged_in'
    //   3368: ldc_w 'false'
    //   3371: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   3374: goto -> 5239
    //   3377: aload_0
    //   3378: ldc_w 'day_of_week_allow'
    //   3381: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   3384: new java/util/Date
    //   3387: dup
    //   3388: invokespecial <init> : ()V
    //   3391: invokestatic check_day_of_week : (Ljava/lang/String;Ljava/util/Date;)Z
    //   3394: ifne -> 3460
    //   3397: aload_0
    //   3398: aload_0
    //   3399: ldc_w '530'
    //   3402: new java/lang/StringBuffer
    //   3405: dup
    //   3406: ldc_w '%user_day_restricted%'
    //   3409: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3412: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   3415: invokespecial <init> : (Ljava/lang/String;)V
    //   3418: ldc '\\r\\n'
    //   3420: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3423: ldc_w 'Control connection closed'
    //   3426: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3429: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3432: ldc_w '.'
    //   3435: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3438: invokevirtual toString : ()Ljava/lang/String;
    //   3441: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3444: putfield not_done : Z
    //   3447: aload_0
    //   3448: ldc_w 'user_logged_in'
    //   3451: ldc_w 'false'
    //   3454: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   3457: goto -> 5239
    //   3460: aload #10
    //   3462: new java/lang/StringBuffer
    //   3465: dup
    //   3466: aload_0
    //   3467: getfield hh : Ljava/text/SimpleDateFormat;
    //   3470: new java/util/Date
    //   3473: dup
    //   3474: invokespecial <init> : ()V
    //   3477: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   3480: invokestatic parseInt : (Ljava/lang/String;)I
    //   3483: invokestatic valueOf : (I)Ljava/lang/String;
    //   3486: invokespecial <init> : (Ljava/lang/String;)V
    //   3489: invokevirtual toString : ()Ljava/lang/String;
    //   3492: invokevirtual indexOf : (Ljava/lang/Object;)I
    //   3495: ifge -> 3591
    //   3498: aload_0
    //   3499: aload_0
    //   3500: ldc_w '530'
    //   3503: new java/lang/StringBuffer
    //   3506: dup
    //   3507: ldc_w 'Not allowed to login at the present hour ($0), try later.'
    //   3510: new java/lang/StringBuffer
    //   3513: dup
    //   3514: aload_0
    //   3515: getfield hh : Ljava/text/SimpleDateFormat;
    //   3518: new java/util/Date
    //   3521: dup
    //   3522: invokespecial <init> : ()V
    //   3525: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   3528: invokestatic parseInt : (Ljava/lang/String;)I
    //   3531: invokestatic valueOf : (I)Ljava/lang/String;
    //   3534: invokespecial <init> : (Ljava/lang/String;)V
    //   3537: invokevirtual toString : ()Ljava/lang/String;
    //   3540: invokestatic G : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   3543: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   3546: invokespecial <init> : (Ljava/lang/String;)V
    //   3549: ldc '\\r\\n'
    //   3551: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3554: ldc_w 'Control connection closed'
    //   3557: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3560: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3563: ldc_w '.'
    //   3566: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3569: invokevirtual toString : ()Ljava/lang/String;
    //   3572: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3575: putfield not_done : Z
    //   3578: aload_0
    //   3579: ldc_w 'user_logged_in'
    //   3582: ldc_w 'false'
    //   3585: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   3588: goto -> 5239
    //   3591: aload_0
    //   3592: ldc_w 'max_logins_ip'
    //   3595: invokevirtual IG : (Ljava/lang/String;)I
    //   3598: ifeq -> 3689
    //   3601: aload_0
    //   3602: aconst_null
    //   3603: invokestatic count_users_ip : (Lcrushftp/handlers/SessionCrush;Ljava/lang/String;)I
    //   3606: aload_0
    //   3607: ldc_w 'max_logins_ip'
    //   3610: invokevirtual IG : (Ljava/lang/String;)I
    //   3613: if_icmple -> 3689
    //   3616: iload #9
    //   3618: ifne -> 3689
    //   3621: iload #5
    //   3623: ifne -> 3689
    //   3626: aload_0
    //   3627: aload_0
    //   3628: ldc_w '421'
    //   3631: new java/lang/StringBuffer
    //   3634: dup
    //   3635: ldc_w '%max_simultaneous_connections_ip%'
    //   3638: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3641: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   3644: invokespecial <init> : (Ljava/lang/String;)V
    //   3647: ldc '\\r\\n'
    //   3649: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3652: ldc_w 'Control connection closed'
    //   3655: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3658: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3661: ldc_w '.'
    //   3664: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3667: invokevirtual toString : ()Ljava/lang/String;
    //   3670: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3673: putfield not_done : Z
    //   3676: aload_0
    //   3677: ldc_w 'user_logged_in'
    //   3680: ldc_w 'false'
    //   3683: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   3686: goto -> 5239
    //   3689: aload_0
    //   3690: ldc_w 'max_logins'
    //   3693: invokevirtual IG : (Ljava/lang/String;)I
    //   3696: ifeq -> 3784
    //   3699: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   3702: aload_0
    //   3703: invokevirtual count_users : (Lcrushftp/handlers/SessionCrush;)I
    //   3706: aload_0
    //   3707: ldc_w 'max_logins'
    //   3710: invokevirtual IG : (Ljava/lang/String;)I
    //   3713: if_icmple -> 3784
    //   3716: iload #5
    //   3718: ifne -> 3784
    //   3721: aload_0
    //   3722: aload_0
    //   3723: ldc_w '421'
    //   3726: new java/lang/StringBuffer
    //   3729: dup
    //   3730: ldc_w '%max_simultaneous_connections%'
    //   3733: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3736: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   3739: invokespecial <init> : (Ljava/lang/String;)V
    //   3742: ldc '\\r\\n'
    //   3744: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3747: ldc_w 'Control connection closed'
    //   3750: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3753: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3756: ldc_w '.'
    //   3759: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   3762: invokevirtual toString : ()Ljava/lang/String;
    //   3765: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3768: putfield not_done : Z
    //   3771: aload_0
    //   3772: ldc_w 'user_logged_in'
    //   3775: ldc_w 'false'
    //   3778: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   3781: goto -> 5239
    //   3784: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   3787: getfield common_code : Lcrushftp/handlers/Common;
    //   3790: aload_0
    //   3791: ldc_w 'account_expire'
    //   3794: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   3797: invokevirtual check_date_expired_roll : (Ljava/lang/String;)Z
    //   3800: ifeq -> 3948
    //   3803: aload_0
    //   3804: ldc_w 'account_expire_delete'
    //   3807: invokevirtual BG : (Ljava/lang/String;)Z
    //   3810: ifeq -> 3885
    //   3813: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   3816: pop
    //   3817: aload_0
    //   3818: ldc 'listen_ip_port'
    //   3820: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   3823: aload_3
    //   3824: invokestatic deleteUser : (Ljava/lang/String;Ljava/lang/String;)V
    //   3827: goto -> 3832
    //   3830: astore #12
    //   3832: aload_0
    //   3833: aload_0
    //   3834: ldc_w '530'
    //   3837: ldc_w '%account_expired_deleted%'
    //   3840: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3843: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3846: putfield not_done : Z
    //   3849: aload_0
    //   3850: ldc_w 'failed_commands'
    //   3853: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   3856: new java/lang/StringBuffer
    //   3859: dup
    //   3860: invokespecial <init> : ()V
    //   3863: new java/util/Date
    //   3866: dup
    //   3867: invokespecial <init> : ()V
    //   3870: invokevirtual getTime : ()J
    //   3873: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   3876: invokevirtual toString : ()Ljava/lang/String;
    //   3879: invokevirtual addElement : (Ljava/lang/Object;)V
    //   3882: goto -> 3935
    //   3885: aload_0
    //   3886: aload_0
    //   3887: ldc_w '530'
    //   3890: ldc_w '%account_expired%'
    //   3893: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   3896: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   3899: putfield not_done : Z
    //   3902: aload_0
    //   3903: ldc_w 'failed_commands'
    //   3906: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   3909: new java/lang/StringBuffer
    //   3912: dup
    //   3913: invokespecial <init> : ()V
    //   3916: new java/util/Date
    //   3919: dup
    //   3920: invokespecial <init> : ()V
    //   3923: invokevirtual getTime : ()J
    //   3926: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   3929: invokevirtual toString : ()Ljava/lang/String;
    //   3932: invokevirtual addElement : (Ljava/lang/Object;)V
    //   3935: aload_0
    //   3936: ldc_w 'user_logged_in'
    //   3939: ldc_w 'false'
    //   3942: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   3945: goto -> 5239
    //   3948: ldc_w 'crushftp.singleuser'
    //   3951: ldc_w 'false'
    //   3954: invokestatic getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   3957: ldc_w 'true'
    //   3960: invokevirtual equals : (Ljava/lang/Object;)Z
    //   3963: ifeq -> 4058
    //   3966: aload_0
    //   3967: ldc_w 'site'
    //   3970: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   3973: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   3976: ldc_w '(CONNECT)'
    //   3979: invokevirtual indexOf : (Ljava/lang/String;)I
    //   3982: iflt -> 3995
    //   3985: aload_0
    //   3986: ldc_w 'ignore_max_logins'
    //   3989: invokevirtual BG : (Ljava/lang/String;)Z
    //   3992: ifeq -> 4058
    //   3995: aload_0
    //   3996: aload_0
    //   3997: ldc_w '530'
    //   4000: new java/lang/StringBuffer
    //   4003: dup
    //   4004: ldc_w 'Not allowed to login during maintenance, try again later.'
    //   4007: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   4010: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   4013: invokespecial <init> : (Ljava/lang/String;)V
    //   4016: ldc '\\r\\n'
    //   4018: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4021: ldc_w 'Control connection closed'
    //   4024: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   4027: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4030: ldc_w '.'
    //   4033: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4036: invokevirtual toString : ()Ljava/lang/String;
    //   4039: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   4042: putfield not_done : Z
    //   4045: aload_0
    //   4046: ldc_w 'user_logged_in'
    //   4049: ldc_w 'false'
    //   4052: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   4055: goto -> 5239
    //   4058: aload_0
    //   4059: ldc_w 'user_name'
    //   4062: aload_3
    //   4063: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   4066: aload_0
    //   4067: ldc_w 'current_password'
    //   4070: aload #4
    //   4072: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   4075: aload_0
    //   4076: ldc_w 'account_expire'
    //   4079: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   4082: ifnull -> 4433
    //   4085: aload_0
    //   4086: ldc_w 'account_expire'
    //   4089: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   4092: ldc ''
    //   4094: invokevirtual equals : (Ljava/lang/Object;)Z
    //   4097: ifne -> 4433
    //   4100: aload_0
    //   4101: ldc_w 'account_expire'
    //   4104: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   4107: ldc_w '0'
    //   4110: invokevirtual equals : (Ljava/lang/Object;)Z
    //   4113: ifne -> 4433
    //   4116: aload_0
    //   4117: ldc_w 'account_expire_rolling_days'
    //   4120: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   4123: ldc ''
    //   4125: invokevirtual equals : (Ljava/lang/Object;)Z
    //   4128: ifne -> 4433
    //   4131: aload_0
    //   4132: ldc_w 'account_expire_rolling_days'
    //   4135: invokevirtual IG : (Ljava/lang/String;)I
    //   4138: ifle -> 4433
    //   4141: new java/util/GregorianCalendar
    //   4144: dup
    //   4145: invokespecial <init> : ()V
    //   4148: astore #12
    //   4150: aload #12
    //   4152: new java/util/Date
    //   4155: dup
    //   4156: invokespecial <init> : ()V
    //   4159: invokevirtual setTime : (Ljava/util/Date;)V
    //   4162: aload #12
    //   4164: iconst_5
    //   4165: aload_0
    //   4166: ldc_w 'account_expire_rolling_days'
    //   4169: invokevirtual IG : (Ljava/lang/String;)I
    //   4172: invokevirtual add : (II)V
    //   4175: aconst_null
    //   4176: astore #13
    //   4178: aload_0
    //   4179: ldc_w 'account_expire'
    //   4182: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   4185: ldc '/'
    //   4187: invokevirtual indexOf : (Ljava/lang/String;)I
    //   4190: iflt -> 4211
    //   4193: new java/text/SimpleDateFormat
    //   4196: dup
    //   4197: ldc_w 'MM/dd/yy hh:mm aa'
    //   4200: getstatic java/util/Locale.US : Ljava/util/Locale;
    //   4203: invokespecial <init> : (Ljava/lang/String;Ljava/util/Locale;)V
    //   4206: astore #13
    //   4208: goto -> 4226
    //   4211: new java/text/SimpleDateFormat
    //   4214: dup
    //   4215: ldc_w 'MMddyyyyHHmm'
    //   4218: getstatic java/util/Locale.US : Ljava/util/Locale;
    //   4221: invokespecial <init> : (Ljava/lang/String;Ljava/util/Locale;)V
    //   4224: astore #13
    //   4226: aload #13
    //   4228: aload_0
    //   4229: ldc_w 'account_expire'
    //   4232: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   4235: invokevirtual parse : (Ljava/lang/String;)Ljava/util/Date;
    //   4238: invokevirtual getTime : ()J
    //   4241: aload #12
    //   4243: invokevirtual getTime : ()Ljava/util/Date;
    //   4246: invokevirtual getTime : ()J
    //   4249: lcmp
    //   4250: ifge -> 4433
    //   4253: aload_0
    //   4254: getfield user : Ljava/util/Properties;
    //   4257: ldc_w 'account_expire'
    //   4260: aload #13
    //   4262: aload #12
    //   4264: invokevirtual getTime : ()Ljava/util/Date;
    //   4267: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   4270: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4273: pop
    //   4274: getstatic com/crushftp/client/Common.dmz_mode : Z
    //   4277: ifne -> 4308
    //   4280: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   4283: aload_0
    //   4284: ldc 'listen_ip_port'
    //   4286: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   4289: aload_3
    //   4290: ldc_w 'account_expire'
    //   4293: aload #13
    //   4295: aload #12
    //   4297: invokevirtual getTime : ()Ljava/util/Date;
    //   4300: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   4303: iconst_1
    //   4304: iconst_1
    //   4305: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   4308: getstatic com/crushftp/client/Common.dmz_mode : Z
    //   4311: ifne -> 4433
    //   4314: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   4317: aload_0
    //   4318: ldc 'listen_ip_port'
    //   4320: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   4323: aload_3
    //   4324: ldc_w 'account_expire_rolling_days'
    //   4327: new java/lang/StringBuffer
    //   4330: dup
    //   4331: aload_0
    //   4332: ldc_w 'account_expire_rolling_days'
    //   4335: invokevirtual IG : (Ljava/lang/String;)I
    //   4338: invokestatic valueOf : (I)Ljava/lang/String;
    //   4341: invokespecial <init> : (Ljava/lang/String;)V
    //   4344: invokevirtual toString : ()Ljava/lang/String;
    //   4347: iconst_1
    //   4348: iconst_1
    //   4349: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   4352: goto -> 4433
    //   4355: astore #14
    //   4357: aload_0
    //   4358: getfield user : Ljava/util/Properties;
    //   4361: ldc_w 'failure_count_max'
    //   4364: ldc_w '0'
    //   4367: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   4370: ldc_w '0'
    //   4373: invokevirtual equals : (Ljava/lang/Object;)Z
    //   4376: ifne -> 4431
    //   4379: aload_0
    //   4380: getfield user : Ljava/util/Properties;
    //   4383: ldc_w 'failure_count_max'
    //   4386: ldc_w '0'
    //   4389: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   4392: ldc ''
    //   4394: invokevirtual equals : (Ljava/lang/Object;)Z
    //   4397: ifne -> 4431
    //   4400: aload_0
    //   4401: ldc_w 'failure_count'
    //   4404: invokevirtual IG : (Ljava/lang/String;)I
    //   4407: ifle -> 4431
    //   4410: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   4413: aload_0
    //   4414: ldc 'listen_ip_port'
    //   4416: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   4419: aload_3
    //   4420: ldc_w 'failure_count'
    //   4423: ldc_w '0'
    //   4426: iconst_1
    //   4427: iconst_1
    //   4428: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   4431: iconst_1
    //   4432: ireturn
    //   4433: aload_0
    //   4434: ldc_w 'last_logins'
    //   4437: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   4440: astore #12
    //   4442: aload #12
    //   4444: ldc_w 'last_logins'
    //   4447: invokevirtual equals : (Ljava/lang/Object;)Z
    //   4450: ifeq -> 4457
    //   4453: ldc ''
    //   4455: astore #12
    //   4457: new java/text/SimpleDateFormat
    //   4460: dup
    //   4461: ldc_w 'MM/dd/yyyy hh:mm:ss aa'
    //   4464: getstatic java/util/Locale.US : Ljava/util/Locale;
    //   4467: invokespecial <init> : (Ljava/lang/String;Ljava/util/Locale;)V
    //   4470: astore #13
    //   4472: new java/lang/StringBuffer
    //   4475: dup
    //   4476: aload #13
    //   4478: new java/util/Date
    //   4481: dup
    //   4482: invokespecial <init> : ()V
    //   4485: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   4488: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   4491: invokespecial <init> : (Ljava/lang/String;)V
    //   4494: ldc_w ','
    //   4497: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4500: aload #12
    //   4502: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4505: invokevirtual toString : ()Ljava/lang/String;
    //   4508: astore #12
    //   4510: ldc ''
    //   4512: astore #14
    //   4514: iconst_0
    //   4515: istore #15
    //   4517: goto -> 4582
    //   4520: iload #15
    //   4522: ifle -> 4548
    //   4525: new java/lang/StringBuffer
    //   4528: dup
    //   4529: aload #14
    //   4531: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   4534: invokespecial <init> : (Ljava/lang/String;)V
    //   4537: ldc_w ','
    //   4540: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4543: invokevirtual toString : ()Ljava/lang/String;
    //   4546: astore #14
    //   4548: new java/lang/StringBuffer
    //   4551: dup
    //   4552: aload #14
    //   4554: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   4557: invokespecial <init> : (Ljava/lang/String;)V
    //   4560: aload #12
    //   4562: ldc_w ','
    //   4565: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
    //   4568: iload #15
    //   4570: aaload
    //   4571: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4574: invokevirtual toString : ()Ljava/lang/String;
    //   4577: astore #14
    //   4579: iinc #15, 1
    //   4582: iload #15
    //   4584: aload #12
    //   4586: ldc_w ','
    //   4589: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
    //   4592: arraylength
    //   4593: if_icmpge -> 4603
    //   4596: iload #15
    //   4598: bipush #10
    //   4600: if_icmplt -> 4520
    //   4603: getstatic com/crushftp/client/Common.dmz_mode : Z
    //   4606: ifne -> 4638
    //   4609: ldc_w 'track_last_logins'
    //   4612: invokestatic BG : (Ljava/lang/String;)Z
    //   4615: ifeq -> 4638
    //   4618: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   4621: aload_0
    //   4622: ldc 'listen_ip_port'
    //   4624: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   4627: aload_3
    //   4628: ldc_w 'last_logins'
    //   4631: aload #14
    //   4633: iconst_0
    //   4634: iconst_0
    //   4635: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   4638: aload_0
    //   4639: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   4642: ifnull -> 4727
    //   4645: aload_0
    //   4646: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   4649: getfield sock : Ljava/net/Socket;
    //   4652: invokevirtual getSoTimeout : ()I
    //   4655: sipush #1000
    //   4658: idiv
    //   4659: istore #15
    //   4661: aload_0
    //   4662: ldc_w 'max_idle_time'
    //   4665: invokevirtual IG : (Ljava/lang/String;)I
    //   4668: istore #16
    //   4670: iload #16
    //   4672: ifge -> 4684
    //   4675: iload #16
    //   4677: iconst_m1
    //   4678: imul
    //   4679: istore #16
    //   4681: goto -> 4691
    //   4684: iload #16
    //   4686: bipush #60
    //   4688: imul
    //   4689: istore #16
    //   4691: aload_0
    //   4692: getfield ftp : Lcrushftp/server/ServerSessionFTP;
    //   4695: getfield sock : Ljava/net/Socket;
    //   4698: aload_0
    //   4699: ldc_w 'max_idle_time'
    //   4702: invokevirtual IG : (Ljava/lang/String;)I
    //   4705: ifne -> 4713
    //   4708: iload #15
    //   4710: goto -> 4715
    //   4713: iload #16
    //   4715: sipush #1000
    //   4718: imul
    //   4719: invokevirtual setSoTimeout : (I)V
    //   4722: goto -> 4727
    //   4725: astore #15
    //   4727: ldc ''
    //   4729: astore #15
    //   4731: iload #9
    //   4733: ifeq -> 4767
    //   4736: new java/lang/StringBuffer
    //   4739: dup
    //   4740: aload #15
    //   4742: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   4745: invokespecial <init> : (Ljava/lang/String;)V
    //   4748: ldc_w 'First user with same name, same IP, was autokicked.'
    //   4751: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   4754: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4757: ldc '\\r\\n'
    //   4759: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4762: invokevirtual toString : ()Ljava/lang/String;
    //   4765: astore #15
    //   4767: iload #5
    //   4769: ifeq -> 4803
    //   4772: new java/lang/StringBuffer
    //   4775: dup
    //   4776: aload #15
    //   4778: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   4781: invokespecial <init> : (Ljava/lang/String;)V
    //   4784: ldc_w 'Previous sessions were kicked.'
    //   4787: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   4790: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4793: ldc '\\r\\n'
    //   4795: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4798: invokevirtual toString : ()Ljava/lang/String;
    //   4801: astore #15
    //   4803: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   4806: aload_0
    //   4807: getfield user_info : Ljava/util/Properties;
    //   4810: invokevirtual set_user_pointer : (Ljava/util/Properties;)V
    //   4813: aload_0
    //   4814: getfield server_item : Ljava/util/Properties;
    //   4817: ldc_w 'user_welcome_message'
    //   4820: ldc ''
    //   4822: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   4825: astore #16
    //   4827: aload_0
    //   4828: getfield user : Ljava/util/Properties;
    //   4831: ldc_w 'welcome_message2'
    //   4834: ldc ''
    //   4836: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   4839: ldc ''
    //   4841: invokevirtual equals : (Ljava/lang/Object;)Z
    //   4844: ifne -> 4861
    //   4847: aload_0
    //   4848: getfield user : Ljava/util/Properties;
    //   4851: ldc_w 'welcome_message2'
    //   4854: ldc ''
    //   4856: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   4859: astore #16
    //   4861: new java/lang/StringBuffer
    //   4864: dup
    //   4865: aload #15
    //   4867: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   4870: invokespecial <init> : (Ljava/lang/String;)V
    //   4873: aload #16
    //   4875: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4878: ldc '\\r\\n'
    //   4880: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4883: invokevirtual toString : ()Ljava/lang/String;
    //   4886: astore #15
    //   4888: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   4891: aload_0
    //   4892: ldc_w 'welcome_message'
    //   4895: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   4898: aload_0
    //   4899: invokevirtual change_vars_to_values : (Ljava/lang/String;Lcrushftp/handlers/SessionCrush;)Ljava/lang/String;
    //   4902: invokevirtual trim : ()Ljava/lang/String;
    //   4905: astore #17
    //   4907: aload #17
    //   4909: ldc_w 'welcome_msg'
    //   4912: invokevirtual equals : (Ljava/lang/Object;)Z
    //   4915: ifeq -> 4922
    //   4918: ldc ''
    //   4920: astore #17
    //   4922: aload #17
    //   4924: invokevirtual length : ()I
    //   4927: ifle -> 4952
    //   4930: new java/lang/StringBuffer
    //   4933: dup
    //   4934: aload #17
    //   4936: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   4939: invokespecial <init> : (Ljava/lang/String;)V
    //   4942: ldc '\\r\\n'
    //   4944: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4947: invokevirtual toString : ()Ljava/lang/String;
    //   4950: astore #17
    //   4952: aload_0
    //   4953: getfield user : Ljava/util/Properties;
    //   4956: ldc_w 'user_name'
    //   4959: aload_3
    //   4960: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   4963: pop
    //   4964: new java/lang/StringBuffer
    //   4967: dup
    //   4968: aload #15
    //   4970: invokevirtual trim : ()Ljava/lang/String;
    //   4973: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   4976: invokespecial <init> : (Ljava/lang/String;)V
    //   4979: ldc '\\r\\n'
    //   4981: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4984: aload #17
    //   4986: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4989: ldc_w '%PASS% logged in'
    //   4992: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   4995: invokevirtual toString : ()Ljava/lang/String;
    //   4998: astore #15
    //   5000: aload_0
    //   5001: ldc_w 'last_login_message'
    //   5004: aload #15
    //   5006: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5009: aload_0
    //   5010: ldc_w 'dont_write'
    //   5013: invokevirtual uiBG : (Ljava/lang/String;)Z
    //   5016: ifne -> 5064
    //   5019: aload_0
    //   5020: aload_0
    //   5021: ldc_w '230'
    //   5024: aload #15
    //   5026: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   5029: putfield not_done : Z
    //   5032: goto -> 5064
    //   5035: astore #16
    //   5037: new java/lang/StringBuffer
    //   5040: dup
    //   5041: invokespecial <init> : ()V
    //   5044: aload #16
    //   5046: invokevirtual append : (Ljava/lang/Object;)Ljava/lang/StringBuffer;
    //   5049: invokevirtual toString : ()Ljava/lang/String;
    //   5052: ldc_w 'Interrupted'
    //   5055: invokevirtual indexOf : (Ljava/lang/String;)I
    //   5058: iflt -> 5064
    //   5061: aload #16
    //   5063: athrow
    //   5064: aload_0
    //   5065: getfield logDateFormat : Ljava/text/SimpleDateFormat;
    //   5068: aload_0
    //   5069: getfield user : Ljava/util/Properties;
    //   5072: invokestatic updateDateCustomizations : (Ljava/text/SimpleDateFormat;Ljava/util/Properties;)Ljava/text/SimpleDateFormat;
    //   5075: astore #16
    //   5077: aload_0
    //   5078: ldc_w 'login_date_formatted'
    //   5081: aload #16
    //   5083: new java/util/Date
    //   5086: dup
    //   5087: invokespecial <init> : ()V
    //   5090: invokevirtual format : (Ljava/util/Date;)Ljava/lang/String;
    //   5093: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5096: aload_0
    //   5097: ldc_w 'user_logged_in'
    //   5100: ldc_w 'true'
    //   5103: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5106: aload_0
    //   5107: ldc_w 'sharedId'
    //   5110: aload_0
    //   5111: invokevirtual getId : ()Ljava/lang/String;
    //   5114: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5117: aload_0
    //   5118: ldc_w 'ratio_field_permanent'
    //   5121: invokevirtual BG : (Ljava/lang/String;)Z
    //   5124: ifeq -> 5181
    //   5127: aload_0
    //   5128: ldc_w 'ratio_bytes_sent'
    //   5131: new java/lang/StringBuffer
    //   5134: dup
    //   5135: aload_0
    //   5136: ldc_w 'user_bytes_sent'
    //   5139: invokevirtual IG : (Ljava/lang/String;)I
    //   5142: invokestatic valueOf : (I)Ljava/lang/String;
    //   5145: invokespecial <init> : (Ljava/lang/String;)V
    //   5148: invokevirtual toString : ()Ljava/lang/String;
    //   5151: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5154: aload_0
    //   5155: ldc_w 'ratio_bytes_received'
    //   5158: new java/lang/StringBuffer
    //   5161: dup
    //   5162: aload_0
    //   5163: ldc_w 'user_bytes_received'
    //   5166: invokevirtual IG : (Ljava/lang/String;)I
    //   5169: invokestatic valueOf : (I)Ljava/lang/String;
    //   5172: invokespecial <init> : (Ljava/lang/String;)V
    //   5175: invokevirtual toString : ()Ljava/lang/String;
    //   5178: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5181: aload_0
    //   5182: ldc_w 'max_login_time'
    //   5185: invokevirtual IG : (Ljava/lang/String;)I
    //   5188: ifeq -> 5239
    //   5191: aload_0
    //   5192: ldc_w 'max_login_time'
    //   5195: invokevirtual IG : (Ljava/lang/String;)I
    //   5198: istore #17
    //   5200: new crushftp/handlers/SessionCrush$1$Killer
    //   5203: dup
    //   5204: aload_0
    //   5205: iload #17
    //   5207: invokespecial <init> : (Lcrushftp/handlers/SessionCrush;I)V
    //   5210: new java/lang/StringBuffer
    //   5213: dup
    //   5214: invokestatic currentThread : ()Ljava/lang/Thread;
    //   5217: invokevirtual getName : ()Ljava/lang/String;
    //   5220: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   5223: invokespecial <init> : (Ljava/lang/String;)V
    //   5226: ldc_w ' (max_time)'
    //   5229: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   5232: invokevirtual toString : ()Ljava/lang/String;
    //   5235: invokestatic startWorker : (Ljava/lang/Runnable;Ljava/lang/String;)Z
    //   5238: pop
    //   5239: aload_0
    //   5240: ldc_w 'user_logged_in'
    //   5243: invokevirtual uiBG : (Ljava/lang/String;)Z
    //   5246: ifeq -> 5283
    //   5249: iload_2
    //   5250: ifeq -> 5283
    //   5253: ldc_w 'successful_logins'
    //   5256: new java/lang/StringBuffer
    //   5259: dup
    //   5260: invokespecial <init> : ()V
    //   5263: ldc_w 'successful_logins'
    //   5266: invokestatic IG : (Ljava/lang/String;)I
    //   5269: iconst_1
    //   5270: iadd
    //   5271: invokevirtual append : (I)Ljava/lang/StringBuffer;
    //   5274: invokevirtual toString : ()Ljava/lang/String;
    //   5277: invokestatic siPUT2 : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5280: goto -> 5667
    //   5283: iload_2
    //   5284: ifeq -> 5667
    //   5287: aload_0
    //   5288: ldc_w 'user_protocol'
    //   5291: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   5294: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   5297: ldc_w 'HTTP'
    //   5300: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   5303: ifne -> 5333
    //   5306: ldc_w 'failed_logins'
    //   5309: new java/lang/StringBuffer
    //   5312: dup
    //   5313: invokespecial <init> : ()V
    //   5316: ldc_w 'failed_logins'
    //   5319: invokestatic IG : (Ljava/lang/String;)I
    //   5322: iconst_1
    //   5323: iadd
    //   5324: invokevirtual append : (I)Ljava/lang/StringBuffer;
    //   5327: invokevirtual toString : ()Ljava/lang/String;
    //   5330: invokestatic siPUT2 : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5333: aload_0
    //   5334: ldc_w 'failed_commands'
    //   5337: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5340: invokevirtual size : ()I
    //   5343: bipush #10
    //   5345: isub
    //   5346: ifle -> 5667
    //   5349: sipush #1000
    //   5352: aload_0
    //   5353: ldc_w 'failed_commands'
    //   5356: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5359: invokevirtual size : ()I
    //   5362: bipush #10
    //   5364: isub
    //   5365: imul
    //   5366: i2l
    //   5367: invokestatic sleep : (J)V
    //   5370: goto -> 5667
    //   5373: aload_0
    //   5374: getfield user_info : Ljava/util/Properties;
    //   5377: ldc_w 'lastProxyError'
    //   5380: ldc ''
    //   5382: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   5385: ldc ''
    //   5387: invokevirtual equals : (Ljava/lang/Object;)Z
    //   5390: ifne -> 5451
    //   5393: ldc_w 'rfc_proxy'
    //   5396: invokestatic BG : (Ljava/lang/String;)Z
    //   5399: ifeq -> 5425
    //   5402: aload_0
    //   5403: aload_0
    //   5404: aload_0
    //   5405: getfield user_info : Ljava/util/Properties;
    //   5408: ldc_w 'lastProxyError'
    //   5411: ldc ''
    //   5413: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   5416: invokevirtual ftp_write_command_raw : (Ljava/lang/String;)Z
    //   5419: putfield not_done : Z
    //   5422: goto -> 5509
    //   5425: aload_0
    //   5426: aload_0
    //   5427: ldc_w '530'
    //   5430: aload_0
    //   5431: getfield user_info : Ljava/util/Properties;
    //   5434: ldc_w 'lastProxyError'
    //   5437: ldc ''
    //   5439: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   5442: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   5445: putfield not_done : Z
    //   5448: goto -> 5509
    //   5451: aload_0
    //   5452: getfield server_item : Ljava/util/Properties;
    //   5455: ldc_w 'serverType'
    //   5458: ldc_w 'ftp'
    //   5461: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   5464: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   5467: ldc_w 'FTP'
    //   5470: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   5473: ifne -> 5495
    //   5476: aload_3
    //   5477: ldc ''
    //   5479: invokevirtual equals : (Ljava/lang/Object;)Z
    //   5482: ifne -> 5509
    //   5485: aload_3
    //   5486: ldc_w 'anonymous'
    //   5489: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   5492: ifne -> 5509
    //   5495: aload_0
    //   5496: aload_0
    //   5497: ldc_w '530'
    //   5500: ldc_w '%PASS-bad%'
    //   5503: invokevirtual ftp_write_command : (Ljava/lang/String;Ljava/lang/String;)Z
    //   5506: putfield not_done : Z
    //   5509: aload_0
    //   5510: ldc_w 'failed_commands'
    //   5513: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5516: new java/lang/StringBuffer
    //   5519: dup
    //   5520: invokespecial <init> : ()V
    //   5523: new java/util/Date
    //   5526: dup
    //   5527: invokespecial <init> : ()V
    //   5530: invokevirtual getTime : ()J
    //   5533: invokevirtual append : (J)Ljava/lang/StringBuffer;
    //   5536: invokevirtual toString : ()Ljava/lang/String;
    //   5539: invokevirtual addElement : (Ljava/lang/Object;)V
    //   5542: aload_0
    //   5543: ldc_w 'user_logged_in'
    //   5546: ldc_w 'false'
    //   5549: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5552: aload_0
    //   5553: ldc_w 'user_protocol'
    //   5556: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   5559: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   5562: ldc_w 'HTTP'
    //   5565: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   5568: ifne -> 5598
    //   5571: ldc_w 'failed_logins'
    //   5574: new java/lang/StringBuffer
    //   5577: dup
    //   5578: invokespecial <init> : ()V
    //   5581: ldc_w 'failed_logins'
    //   5584: invokestatic IG : (Ljava/lang/String;)I
    //   5587: iconst_1
    //   5588: iadd
    //   5589: invokevirtual append : (I)Ljava/lang/StringBuffer;
    //   5592: invokevirtual toString : ()Ljava/lang/String;
    //   5595: invokestatic siPUT2 : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5598: aload_0
    //   5599: ldc_w 'user_logged_in'
    //   5602: ldc_w 'false'
    //   5605: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5608: aload_0
    //   5609: ldc_w 'failed_commands'
    //   5612: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5615: invokevirtual size : ()I
    //   5618: aload_0
    //   5619: ldc_w 'password_attempts'
    //   5622: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5625: invokevirtual size : ()I
    //   5628: iadd
    //   5629: bipush #10
    //   5631: isub
    //   5632: ifle -> 5667
    //   5635: sipush #1000
    //   5638: aload_0
    //   5639: ldc_w 'failed_commands'
    //   5642: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5645: invokevirtual size : ()I
    //   5648: aload_0
    //   5649: ldc_w 'password_attempts'
    //   5652: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5655: invokevirtual size : ()I
    //   5658: iadd
    //   5659: bipush #10
    //   5661: isub
    //   5662: imul
    //   5663: i2l
    //   5664: invokestatic sleep : (J)V
    //   5667: aload_0
    //   5668: ldc_w 'stat'
    //   5671: new java/util/Properties
    //   5674: dup
    //   5675: invokespecial <init> : ()V
    //   5678: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5681: aload_0
    //   5682: ldc_w 'skip_proxy_check'
    //   5685: invokevirtual uiBG : (Ljava/lang/String;)Z
    //   5688: ifne -> 5727
    //   5691: aload_0
    //   5692: ldc_w 'stat'
    //   5695: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   5698: getfield statTools : Lcrushftp/db/StatTools;
    //   5701: aload_0
    //   5702: getfield server_item : Ljava/util/Properties;
    //   5705: aload_3
    //   5706: aload_0
    //   5707: ldc_w 'user_ip'
    //   5710: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   5713: aload_0
    //   5714: ldc_w 'user_logged_in'
    //   5717: invokevirtual uiBG : (Ljava/lang/String;)Z
    //   5720: aload_0
    //   5721: invokevirtual add_login_stat : (Ljava/util/Properties;Ljava/lang/String;Ljava/lang/String;ZLcrushftp/handlers/SessionCrush;)Ljava/util/Properties;
    //   5724: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5727: aload_0
    //   5728: ldc_w 'user_name'
    //   5731: aload_3
    //   5732: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5735: aload_0
    //   5736: ldc_w 'current_password'
    //   5739: aload #4
    //   5741: invokevirtual uiPUT : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5744: aload_0
    //   5745: ldc_w 'user_logged_in'
    //   5748: invokevirtual uiBG : (Ljava/lang/String;)Z
    //   5751: ifeq -> 5834
    //   5754: aload_0
    //   5755: invokevirtual active : ()V
    //   5758: aload_0
    //   5759: getfield user : Ljava/util/Properties;
    //   5762: ldc_w 'failure_count_max'
    //   5765: ldc_w '0'
    //   5768: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   5771: ldc_w '0'
    //   5774: invokevirtual equals : (Ljava/lang/Object;)Z
    //   5777: ifne -> 5832
    //   5780: aload_0
    //   5781: getfield user : Ljava/util/Properties;
    //   5784: ldc_w 'failure_count_max'
    //   5787: ldc_w '0'
    //   5790: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   5793: ldc ''
    //   5795: invokevirtual equals : (Ljava/lang/Object;)Z
    //   5798: ifne -> 5832
    //   5801: aload_0
    //   5802: ldc_w 'failure_count'
    //   5805: invokevirtual IG : (Ljava/lang/String;)I
    //   5808: ifle -> 5832
    //   5811: getstatic crushftp/handlers/UserTools.ut : Lcrushftp/handlers/UserTools;
    //   5814: aload_0
    //   5815: ldc 'listen_ip_port'
    //   5817: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   5820: aload_3
    //   5821: ldc_w 'failure_count'
    //   5824: ldc_w '0'
    //   5827: iconst_1
    //   5828: iconst_1
    //   5829: invokevirtual put_in_user : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V
    //   5832: iconst_1
    //   5833: ireturn
    //   5834: iload_2
    //   5835: ifeq -> 5976
    //   5838: aload_0
    //   5839: ldc_w 'password_attempts'
    //   5842: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5845: new java/lang/StringBuffer
    //   5848: dup
    //   5849: new java/util/Date
    //   5852: dup
    //   5853: invokespecial <init> : ()V
    //   5856: invokevirtual getTime : ()J
    //   5859: invokestatic valueOf : (J)Ljava/lang/String;
    //   5862: invokespecial <init> : (Ljava/lang/String;)V
    //   5865: invokevirtual toString : ()Ljava/lang/String;
    //   5868: invokevirtual addElement : (Ljava/lang/Object;)V
    //   5871: aload_0
    //   5872: ldc_w 'user_protocol'
    //   5875: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   5878: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   5881: ldc_w 'HTTP'
    //   5884: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   5887: ifne -> 5917
    //   5890: ldc_w 'failed_logins'
    //   5893: new java/lang/StringBuffer
    //   5896: dup
    //   5897: invokespecial <init> : ()V
    //   5900: ldc_w 'failed_logins'
    //   5903: invokestatic IG : (Ljava/lang/String;)I
    //   5906: iconst_1
    //   5907: iadd
    //   5908: invokevirtual append : (I)Ljava/lang/StringBuffer;
    //   5911: invokevirtual toString : ()Ljava/lang/String;
    //   5914: invokestatic siPUT2 : (Ljava/lang/String;Ljava/lang/Object;)V
    //   5917: aload_0
    //   5918: ldc_w 'failed_commands'
    //   5921: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5924: invokevirtual size : ()I
    //   5927: aload_0
    //   5928: ldc_w 'password_attempts'
    //   5931: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5934: invokevirtual size : ()I
    //   5937: iadd
    //   5938: bipush #10
    //   5940: isub
    //   5941: ifle -> 5976
    //   5944: sipush #1000
    //   5947: aload_0
    //   5948: ldc_w 'failed_commands'
    //   5951: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5954: invokevirtual size : ()I
    //   5957: aload_0
    //   5958: ldc_w 'password_attempts'
    //   5961: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   5964: invokevirtual size : ()I
    //   5967: iadd
    //   5968: bipush #10
    //   5970: isub
    //   5971: imul
    //   5972: i2l
    //   5973: invokestatic sleep : (J)V
    //   5976: aload_0
    //   5977: new java/lang/Exception
    //   5980: dup
    //   5981: aload_0
    //   5982: ldc_w 'lastLog'
    //   5985: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   5988: invokespecial <init> : (Ljava/lang/String;)V
    //   5991: invokevirtual doErrorEvent : (Ljava/lang/Exception;)V
    //   5994: iconst_0
    //   5995: ireturn
    // Line number table:
    //   Java source line number -> byte code offset
    //   #2557	-> 0
    //   #2559	-> 21
    //   #2560	-> 35
    //   #2561	-> 53
    //   #2563	-> 55
    //   #2564	-> 93
    //   #2565	-> 103
    //   #2566	-> 106
    //   #2567	-> 126
    //   #2569	-> 136
    //   #2570	-> 142
    //   #2571	-> 150
    //   #2573	-> 153
    //   #2574	-> 202
    //   #2576	-> 222
    //   #2578	-> 238
    //   #2579	-> 270
    //   #2580	-> 277
    //   #2581	-> 316
    //   #2583	-> 326
    //   #2584	-> 357
    //   #2588	-> 365
    //   #2589	-> 370
    //   #2590	-> 373
    //   #2591	-> 376
    //   #2592	-> 380
    //   #2594	-> 431
    //   #2595	-> 442
    //   #2597	-> 488
    //   #2598	-> 508
    //   #2599	-> 511
    //   #2600	-> 564
    //   #2602	-> 569
    //   #2603	-> 604
    //   #2604	-> 621
    //   #2606	-> 664
    //   #2607	-> 698
    //   #2609	-> 727
    //   #2613	-> 760
    //   #2614	-> 771
    //   #2616	-> 813
    //   #2617	-> 824
    //   #2618	-> 840
    //   #2622	-> 846
    //   #2625	-> 852
    //   #2626	-> 866
    //   #2627	-> 876
    //   #2628	-> 881
    //   #2635	-> 883
    //   #2636	-> 897
    //   #2637	-> 906
    //   #2638	-> 938
    //   #2639	-> 963
    //   #2640	-> 974
    //   #2642	-> 980
    //   #2643	-> 991
    //   #2645	-> 1026
    //   #2646	-> 1035
    //   #2647	-> 1050
    //   #2648	-> 1065
    //   #2649	-> 1080
    //   #2650	-> 1095
    //   #2651	-> 1110
    //   #2652	-> 1141
    //   #2653	-> 1149
    //   #2654	-> 1153
    //   #2655	-> 1157
    //   #2656	-> 1161
    //   #2657	-> 1166
    //   #2658	-> 1186
    //   #2659	-> 1194
    //   #2661	-> 1199
    //   #2662	-> 1216
    //   #2663	-> 1233
    //   #2664	-> 1250
    //   #2665	-> 1267
    //   #2666	-> 1284
    //   #2669	-> 1296
    //   #2670	-> 1307
    //   #2671	-> 1338
    //   #2672	-> 1349
    //   #2673	-> 1360
    //   #2674	-> 1371
    //   #2675	-> 1382
    //   #2676	-> 1393
    //   #2678	-> 1404
    //   #2680	-> 1421
    //   #2683	-> 1438
    //   #2685	-> 1441
    //   #2686	-> 1475
    //   #2688	-> 1518
    //   #2690	-> 1533
    //   #2691	-> 1563
    //   #2692	-> 1571
    //   #2694	-> 1623
    //   #2695	-> 1631
    //   #2696	-> 1640
    //   #2697	-> 1680
    //   #2699	-> 1702
    //   #2703	-> 1711
    //   #2704	-> 1720
    //   #2705	-> 1731
    //   #2707	-> 1737
    //   #2708	-> 1749
    //   #2705	-> 1800
    //   #2710	-> 1813
    //   #2711	-> 1822
    //   #2713	-> 1828
    //   #2714	-> 1840
    //   #2715	-> 1847
    //   #2717	-> 1850
    //   #2718	-> 1864
    //   #2720	-> 1872
    //   #2721	-> 1879
    //   #2722	-> 1892
    //   #2724	-> 1897
    //   #2725	-> 1908
    //   #2726	-> 1934
    //   #2728	-> 1970
    //   #2729	-> 1980
    //   #2730	-> 1991
    //   #2731	-> 2002
    //   #2732	-> 2007
    //   #2715	-> 2055
    //   #2711	-> 2065
    //   #2739	-> 2081
    //   #2741	-> 2083
    //   #2746	-> 2093
    //   #2748	-> 2099
    //   #2752	-> 2141
    //   #2754	-> 2154
    //   #2755	-> 2187
    //   #2756	-> 2200
    //   #2757	-> 2214
    //   #2758	-> 2221
    //   #2761	-> 2230
    //   #2763	-> 2236
    //   #2764	-> 2246
    //   #2765	-> 2255
    //   #2766	-> 2283
    //   #2767	-> 2306
    //   #2768	-> 2330
    //   #2761	-> 2337
    //   #2771	-> 2350
    //   #2773	-> 2352
    //   #2775	-> 2379
    //   #2776	-> 2392
    //   #2780	-> 2403
    //   #2781	-> 2406
    //   #2782	-> 2415
    //   #2783	-> 2460
    //   #2784	-> 2503
    //   #2785	-> 2518
    //   #2788	-> 2524
    //   #2790	-> 2553
    //   #2792	-> 2555
    //   #2785	-> 2565
    //   #2794	-> 2576
    //   #2796	-> 2611
    //   #2797	-> 2623
    //   #2798	-> 2629
    //   #2800	-> 2639
    //   #2802	-> 2644
    //   #2804	-> 2657
    //   #2806	-> 2667
    //   #2807	-> 2717
    //   #2809	-> 2730
    //   #2811	-> 2757
    //   #2812	-> 2807
    //   #2814	-> 2820
    //   #2816	-> 2837
    //   #2817	-> 2887
    //   #2819	-> 2900
    //   #2821	-> 2932
    //   #2822	-> 2982
    //   #2824	-> 2995
    //   #2826	-> 3028
    //   #2827	-> 3045
    //   #2829	-> 3058
    //   #2831	-> 3077
    //   #2832	-> 3127
    //   #2834	-> 3140
    //   #2836	-> 3160
    //   #2837	-> 3210
    //   #2839	-> 3223
    //   #2841	-> 3254
    //   #2842	-> 3364
    //   #2844	-> 3377
    //   #2846	-> 3397
    //   #2847	-> 3447
    //   #2849	-> 3460
    //   #2851	-> 3498
    //   #2852	-> 3578
    //   #2854	-> 3591
    //   #2856	-> 3626
    //   #2857	-> 3676
    //   #2859	-> 3689
    //   #2861	-> 3721
    //   #2862	-> 3771
    //   #2864	-> 3784
    //   #2866	-> 3803
    //   #2870	-> 3813
    //   #2872	-> 3830
    //   #2875	-> 3832
    //   #2876	-> 3849
    //   #2880	-> 3885
    //   #2881	-> 3902
    //   #2883	-> 3935
    //   #2885	-> 3948
    //   #2887	-> 3995
    //   #2888	-> 4045
    //   #2892	-> 4058
    //   #2893	-> 4066
    //   #2895	-> 4075
    //   #2898	-> 4141
    //   #2899	-> 4150
    //   #2900	-> 4162
    //   #2902	-> 4175
    //   #2903	-> 4178
    //   #2904	-> 4211
    //   #2908	-> 4226
    //   #2910	-> 4253
    //   #2911	-> 4274
    //   #2912	-> 4308
    //   #2915	-> 4355
    //   #2917	-> 4357
    //   #2918	-> 4431
    //   #2922	-> 4433
    //   #2923	-> 4442
    //   #2924	-> 4457
    //   #2925	-> 4472
    //   #2926	-> 4510
    //   #2927	-> 4514
    //   #2929	-> 4520
    //   #2930	-> 4548
    //   #2927	-> 4579
    //   #2932	-> 4603
    //   #2936	-> 4638
    //   #2938	-> 4645
    //   #2939	-> 4661
    //   #2940	-> 4670
    //   #2941	-> 4684
    //   #2942	-> 4691
    //   #2945	-> 4725
    //   #2948	-> 4727
    //   #2949	-> 4731
    //   #2950	-> 4767
    //   #2952	-> 4803
    //   #2955	-> 4813
    //   #2956	-> 4827
    //   #2957	-> 4861
    //   #2958	-> 4888
    //   #2959	-> 4907
    //   #2960	-> 4922
    //   #2961	-> 4952
    //   #2962	-> 4964
    //   #2963	-> 5000
    //   #2964	-> 5009
    //   #2966	-> 5035
    //   #2969	-> 5037
    //   #2971	-> 5064
    //   #2972	-> 5077
    //   #2973	-> 5096
    //   #2974	-> 5106
    //   #2977	-> 5117
    //   #2979	-> 5127
    //   #2980	-> 5154
    //   #2984	-> 5181
    //   #2986	-> 5191
    //   #3034	-> 5200
    //   #3038	-> 5239
    //   #3040	-> 5253
    //   #3042	-> 5283
    //   #3044	-> 5287
    //   #3045	-> 5333
    //   #3051	-> 5373
    //   #3053	-> 5393
    //   #3054	-> 5425
    //   #3056	-> 5451
    //   #3058	-> 5509
    //   #3059	-> 5542
    //   #3060	-> 5552
    //   #3061	-> 5598
    //   #3062	-> 5608
    //   #3064	-> 5667
    //   #3065	-> 5681
    //   #3066	-> 5727
    //   #3067	-> 5735
    //   #3068	-> 5744
    //   #3070	-> 5754
    //   #3071	-> 5758
    //   #3072	-> 5832
    //   #3076	-> 5834
    //   #3078	-> 5838
    //   #3079	-> 5871
    //   #3080	-> 5917
    //   #3082	-> 5976
    //   #3083	-> 5994
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	5996	0	this	Lcrushftp/handlers/SessionCrush;
    //   0	5996	1	anyPass	Z
    //   0	5996	2	doAfterLogin	Z
    //   0	5996	3	user_name	Ljava/lang/String;
    //   0	5996	4	user_pass	Ljava/lang/String;
    //   106	5890	5	stripped_char	Z
    //   270	95	6	newLinkedServer	Ljava/lang/String;
    //   277	88	7	newLinkedServer2	Ljava/lang/String;
    //   373	5623	6	otp_valid	Z
    //   376	5620	7	verified	Z
    //   380	5616	8	verify_password	Ljava/lang/String;
    //   511	930	9	otp_protocol_check	Z
    //   621	817	10	otp_tokens	Ljava/util/Properties;
    //   698	185	11	token	Ljava/util/Properties;
    //   897	538	11	auth_token	Ljava/lang/String;
    //   906	529	12	token	Ljava/util/Properties;
    //   1035	369	13	email_info	Ljava/util/Properties;
    //   1149	255	14	from	Ljava/lang/String;
    //   1153	251	15	reply_to	Ljava/lang/String;
    //   1157	247	16	cc	Ljava/lang/String;
    //   1161	243	17	bcc	Ljava/lang/String;
    //   1166	238	18	subject	Ljava/lang/String;
    //   1186	218	19	body	Ljava/lang/String;
    //   1194	210	20	template	Ljava/util/Properties;
    //   1720	361	9	v	Ljava/util/Vector;
    //   1734	79	10	xx	I
    //   1749	51	11	p	Ljava/util/Properties;
    //   1825	253	10	x	I
    //   1840	225	11	virtual	Ljava/util/Properties;
    //   1847	218	12	keys	Ljava/util/Enumeration;
    //   1864	191	13	value	Ljava/lang/Object;
    //   1879	176	14	val	Ljava/util/Properties;
    //   1892	163	15	vItems	Ljava/util/Vector;
    //   1908	147	16	p	Ljava/util/Properties;
    //   1980	75	17	url	Ljava/lang/String;
    //   2083	10	9	e	Ljava/lang/Exception;
    //   2187	216	9	ips	Ljava/lang/String;
    //   2214	189	10	get_em	Ljava/util/StringTokenizer;
    //   2221	182	11	num_to_do	I
    //   2230	173	12	ip_list	Ljava/util/Vector;
    //   2233	114	13	x	I
    //   2246	91	14	ip_str	Ljava/lang/String;
    //   2255	82	15	ip_data	Ljava/util/Properties;
    //   2352	27	13	e	Ljava/lang/Exception;
    //   2406	2967	9	auto_kicked	Z
    //   2415	2958	10	allowedHours	Ljava/util/Vector;
    //   2518	2855	11	hours	[Ljava/lang/String;
    //   2521	55	12	x	I
    //   2555	10	13	e	Ljava/lang/Exception;
    //   4150	283	12	gc	Ljava/util/GregorianCalendar;
    //   4178	255	13	sdf	Ljava/text/SimpleDateFormat;
    //   4357	76	14	e	Ljava/lang/Exception;
    //   4442	797	12	last_logins	Ljava/lang/String;
    //   4472	767	13	sdf	Ljava/text/SimpleDateFormat;
    //   4514	725	14	last_logins2	Ljava/lang/String;
    //   4517	86	15	x	I
    //   4661	61	15	priorTimeout	I
    //   4670	52	16	timeout	I
    //   4731	508	15	login_message	Ljava/lang/String;
    //   4827	208	16	msg2	Ljava/lang/String;
    //   4907	128	17	welcome_msg	Ljava/lang/String;
    //   5037	27	16	e	Ljava/lang/Exception;
    //   5077	162	16	date_time	Ljava/text/SimpleDateFormat;
    //   5200	39	17	max_minutes	I
    // Exception table:
    //   from	to	target	type
    //   1711	2078	2081	java/lang/Exception
    //   2230	2347	2350	java/lang/Exception
    //   2524	2550	2553	java/lang/Exception
    //   3813	3827	3830	java/lang/NullPointerException
    //   4226	4352	4355	java/lang/Exception
    //   4638	4722	4725	java/net/SocketException
    //   4813	5032	5035	java/lang/Exception
  }
  
  public void do_Recycle(VRL vrl) throws Exception {
    if (vrl.getProtocol().equalsIgnoreCase("file")) {
      File_B v = new File_B(vrl.getCanonicalPath());
      String recycle = ServerStatus.SG("recycle_path");
      if (!recycle.startsWith("/"))
        recycle = "/" + recycle; 
      if (!recycle.endsWith("/"))
        recycle = String.valueOf(recycle) + "/"; 
      (new File_B(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_'))).mkdirs();
      String addOn = "";
      int pos = 1;
      while ((new File_B(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName() + addOn)).exists())
        addOn = (new StringBuffer(String.valueOf(pos++))).toString(); 
      File_B trash_item = new File_B(String.valueOf(recycle) + v.getCanonicalFile().getParentFile().getCanonicalPath().replace(':', '_') + "/" + v.getName());
      if (!addOn.equals("")) {
        boolean bool = trash_item.renameTo(new File_B(String.valueOf(trash_item.getCanonicalPath()) + addOn));
        if (!bool) {
          Common.copy_U(trash_item.getCanonicalPath(), String.valueOf(trash_item.getCanonicalPath()) + addOn, true);
          trash_item.delete();
        } 
      } 
      boolean ok = v.renameTo(trash_item);
      if (!ok) {
        Common.copy_U(v.getCanonicalPath(), trash_item.getCanonicalPath(), true);
        v.delete();
      } 
      trash_item.setLastModified(System.currentTimeMillis());
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
    uiPUT("the_command", "DELE");
    uiPUT("last_logged_command", "DELE");
    String the_dir = fixupDir(user_dir);
    String parentPath = this.uVFS.getPrivPath(the_dir, false, false);
    Properties dir_item = this.uVFS.get_item(parentPath, -1);
    Properties item = this.uVFS.get_item(the_dir, -1);
    if (item == null) {
      dir_item.put("privs", String.valueOf(dir_item.getProperty("privs")) + "(inherited)");
      if (check_access_privs(the_dir, uiSG("the_command"), dir_item))
        Common.trackSync("DELETE", the_dir, null, false, 0L, 0L, SG("root_dir"), dir_item.getProperty("privs"), uiSG("clientid"), ""); 
      this.not_done = ftp_write_command("550", LOC.G("%DELE-not found%"));
      doErrorEvent(new Exception(uiSG("lastLog")));
      uiVG("failed_commands").addElement((new Date()).getTime());
      return "%DELE-not found%";
    } 
    if (check_access_privs(the_dir, uiSG("the_command"), item) && Common.filter_check("X", Common.last(the_dir), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + SG("file_filter"))) {
      boolean check_all = false;
      if (uiSG("the_command").equalsIgnoreCase("DELE") && !check_access_privs(the_dir, "RMD", item))
        check_all = true; 
      changeProxyToCurrentDir(item);
      Common.trackSync("DELETE", the_dir, null, false, 0L, 0L, SG("root_dir"), dir_item.getProperty("privs"), uiSG("clientid"), "");
      Properties stat = null;
      long quota = -12345L;
      if (item == null) {
        this.not_done = ftp_write_command("550", LOC.G("%DELE-not found%"));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        return "%DELE-not found%";
      } 
      GenericClient c = this.uVFS.getClient(item);
      try {
        String fix_url = item.getProperty("url");
        if (fix_url.endsWith(" "))
          fix_url = Common.replace_str(fix_url, " ", "%20"); 
        stat = c.stat((new VRL(fix_url)).getPath());
        if (stat.getProperty("type").equalsIgnoreCase("DIR")) {
          uiPUT("the_command", "RMD");
          if (!check_access_privs(the_dir, uiSG("the_command"))) {
            uiPUT("the_command", "DELE");
            this.not_done = ftp_write_command("550", LOC.G("%DELE-bad%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
            return "%DELE-bad%";
          } 
          uiPUT("the_command", "DELE");
        } 
        quota = get_quota(the_dir);
        if (item != null && stat != null) {
          Properties fileItem = (Properties)item.clone();
          fileItem = (Properties)fileItem.clone();
          Log.log("FTP_SERVER", 2, "Tracking delete:" + the_dir);
          fileItem.put("the_command", "DELE");
          fileItem.put("the_command_data", the_dir);
          fileItem.put("url", item.getProperty("url", ""));
          fileItem.put("the_file_path", the_dir);
          fileItem.put("the_file_name", stat.getProperty("name"));
          fileItem.put("the_file_size", stat.getProperty("size"));
          fileItem.put("the_file_speed", "0");
          fileItem.put("the_file_start", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
          fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
          fileItem.put("the_file_error", "");
          fileItem.put("the_file_type", stat.getProperty("type", ""));
          fileItem.put("the_file_status", "SUCCESS");
          ServerStatus.thisObj.statTools.add_item_stat(this, fileItem, "DELETE");
          do_event5("DELETE", fileItem);
          long totalSize = Long.parseLong(item.getProperty("size", "0"));
          boolean deleted = false;
          Properties info = new Properties();
          info.put("crushftp_user_name", uiSG("user_name"));
          Common.trackSyncRevision(c, new VRL(fix_url), the_dir, SG("root_dir"), item.getProperty("privs"), true, info);
          SearchHandler.buildEntry(item, this.uVFS, true, true);
          if (ServerStatus.BG("recycle") && (new VRL(fix_url)).getProtocol().equalsIgnoreCase("FILE")) {
            Log.log("FTP_SERVER", 3, String.valueOf(LOC.G("Attempting to recycle file:")) + the_dir);
            do_Recycle(new VRL(fix_url));
            if (c instanceof com.crushftp.client.GenericClientMulti) {
              c.setConfig("skip_first_client", "true");
              deleted = c.delete((new VRL(fix_url)).getPath());
              c.setConfig("skip_first_client", "false");
            } 
          } else {
            Log.log("FTP_SERVER", 3, String.valueOf(LOC.G("Attempting to delete file:")) + the_dir);
            deleted = c.delete((new VRL(fix_url)).getPath());
          } 
          stat = null;
          if (!deleted)
            stat = c.stat((new VRL(fix_url)).getPath()); 
          if (!deleted && stat != null && recurse) {
            try {
              totalSize = Common.recurseSize_U((new VRL(fix_url)).getCanonicalPath(), 0L, this);
            } catch (Exception e) {
              Log.log("FTP_SERVER", 1, e);
            } 
            if (ServerStatus.BG("recycle")) {
              do_Recycle(new VRL(fix_url));
            } else {
              try {
                boolean has_events = (this.user.get("events") != null && ((Vector)this.user.get("events")).size() > 0);
                if (has_events) {
                  has_events = false;
                  Vector events = (Vector)this.user.get("events");
                  for (int x = 0; x < events.size(); x++) {
                    Properties event = events.elementAt(x);
                    if (event.getProperty("event_user_action_list", "").indexOf("(delete)") >= 0)
                      has_events = true; 
                  } 
                } 
                boolean has_delete_event = has_events;
                if (check_all || ServerStatus.BG("check_all_recursive_deletes") || stat.getProperty("check_all_recursive_deletes", "").equals("true")) {
                  Vector list1 = new Vector();
                  String the_dir_f = the_dir;
                  Vector errors = new Vector();
                  Properties status = new Properties();
                  Worker.startWorker(new Runnable(this, status, list1, the_dir_f, errors) {
                        final SessionCrush this$0;
                        
                        private final Properties val$status;
                        
                        private final Vector val$list1;
                        
                        private final String val$the_dir_f;
                        
                        private final Vector val$errors;
                        
                        public void run() {
                          try {
                            this.this$0.uVFS.getListing(this.val$list1, this.val$the_dir_f, 99, 999, true);
                            this.val$status.put("done", "true");
                          } catch (Exception e) {
                            Log.log("SERVER", 1, e);
                            this.val$errors.addElement(e);
                          } finally {
                            this.val$status.put("done", "true");
                          } 
                        }
                      });
                  Vector threads = new Vector();
                  Vector list2 = new Vector();
                  while (list1.size() > 0 || !status.containsKey("done")) {
                    if (list1.size() == 0) {
                      Thread.sleep(100L);
                      continue;
                    } 
                    Properties p = list1.remove(0);
                    SessionCrush thisObj = this;
                    if (p.getProperty("type", "").equals("FILE")) {
                      while (threads.size() > ServerStatus.IG("delete_threads"))
                        Thread.sleep(100L); 
                      threads.addElement(p.getProperty("url"));
                      Worker.startWorker(new Runnable(this, p, threads, has_delete_event, c, thisObj, errors) {
                            final SessionCrush this$0;
                            
                            private final Properties val$p;
                            
                            private final Vector val$threads;
                            
                            private final boolean val$has_delete_event;
                            
                            private final GenericClient val$c;
                            
                            private final SessionCrush val$thisObj;
                            
                            private final Vector val$errors;
                            
                            public void run() {
                              boolean ok2 = true;
                              String temp_dir = String.valueOf(this.val$p.getProperty("root_dir")) + this.val$p.getProperty("name");
                              VRL vrl = new VRL(this.val$p.getProperty("url"));
                              try {
                                if (this.this$0.check_access_privs(temp_dir, "DELE", this.val$p)) {
                                  if (this.val$has_delete_event) {
                                    Properties fileItem = this.val$c.stat(vrl.getPath());
                                    String path = vrl.getPath();
                                    Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Tracking delete:")) + path);
                                    fileItem.put("the_command", "DELE");
                                    fileItem.put("the_command_data", path);
                                    fileItem.put("url", vrl);
                                    fileItem.put("the_file_path", path);
                                    fileItem.put("the_file_name", vrl.getName());
                                    fileItem.put("the_file_size", (new StringBuffer(String.valueOf(fileItem.getProperty("size")))).toString());
                                    fileItem.put("the_file_speed", "0");
                                    fileItem.put("the_file_start", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                                    fileItem.put("the_file_end", (new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                                    fileItem.put("the_file_error", "");
                                    fileItem.put("the_file_type", fileItem.getProperty("type"));
                                    fileItem.put("the_file_status", "SUCCESS");
                                    ServerStatus.thisObj.statTools.add_item_stat(this.val$thisObj, fileItem, "DELETE");
                                    this.this$0.do_event5("DELETE", fileItem);
                                  } 
                                  ok2 = this.val$c.delete(vrl.getPath());
                                } 
                              } catch (Exception e) {
                                Log.log("SERVER", 1, e);
                                this.val$errors.addElement(e);
                              } finally {
                                this.val$threads.remove(this.val$p.getProperty("url"));
                              } 
                              if (!ok2)
                                this.val$errors.addElement(vrl.safe()); 
                            }
                          });
                      continue;
                    } 
                    list2.addElement(p);
                  } 
                  for (int loops = 0; loops < 6000 && threads.size() > 0; loops++)
                    Thread.sleep(100L); 
                  if (errors.size() > 0) {
                    Log.log("SERVER", 1, "Failed to delete:" + errors);
                    return "%DELE-error%";
                  } 
                  for (int loop = 0; list2.size() > 0 && loop < 5; loop++) {
                    for (int x = list2.size() - 1; x >= 0; x--) {
                      Properties p = list2.elementAt(x);
                      if (p.getProperty("type", "").equals("DIR")) {
                        String temp_dir = String.valueOf(p.getProperty("root_dir")) + p.getProperty("name");
                        if (check_access_privs(temp_dir, "RMD", p))
                          if (c.delete((new VRL(p.getProperty("url"))).getPath()))
                            list2.removeElementAt(x);  
                      } 
                    } 
                  } 
                } else {
                  Common.recurseDelete_U((new VRL(fix_url)).getCanonicalPath(), false);
                  c.delete(the_dir);
                } 
              } catch (NullPointerException nullPointerException) {}
            } 
            if (item != null)
              trackAndUpdateUploads(uiVG("lastUploadStats"), new VRL(fix_url), new VRL(fix_url), "DELETE"); 
          } 
          stat = null;
          if (!deleted)
            stat = c.stat((new VRL(fix_url)).getPath()); 
          if (!deleted && stat != null) {
            if (!(new File_B(ServerStatus.SG("recycle_path"))).exists() && ServerStatus.BG("recycle")) {
              this.not_done = ftp_write_command("550", LOC.G("%DELE-error%:Recycle bin not found."));
              doErrorEvent(new Exception(uiSG("lastLog")));
              uiVG("failed_commands").addElement((new Date()).getTime());
              return "%DELE-error%:Recycle bin not found.";
            } 
            Log.log("FTP_SERVER", 3, LOC.G("Delete failure.  Deleted:$0 Exists:$1", (new StringBuffer(String.valueOf(deleted))).toString(), (stat != null) ? 1 : 0));
            this.not_done = ftp_write_command("550", LOC.G("%DELE-error%"));
            doErrorEvent(new Exception(uiSG("lastLog")));
            uiVG("failed_commands").addElement((new Date()).getTime());
            return "%DELE-error%";
          } 
          if (ServerStatus.BG("generic_ftp_responses")) {
            this.not_done = ftp_write_command("250", "Delete operation successful.");
          } else {
            this.not_done = ftp_write_command("250", ServerStatus.SG("custom_delete_msg"));
          } 
          removeCacheItem(item);
          if (quota != -12345L) {
            if (item.getProperty("privs", "").indexOf("(real_quota)") >= 0) {
              quota += totalSize;
            } else {
              quota += totalSize;
            } 
            set_quota(the_dir, quota);
          } 
          return "";
        } 
        this.not_done = ftp_write_command("550", LOC.G("%DELE-not found%"));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        return "%DELE-not found%";
      } finally {
        this.uVFS.releaseClient(c);
      } 
    } 
    this.not_done = ftp_write_command("550", LOC.G("%DELE-bad%"));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%DELE-bad%";
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
    String the_dir = fixupUnsafeChars(fixupDir(null));
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
        boolean view = check_access_privs(Common.all_but_last(the_dir), "LIST", actual_item);
        if (this.rnfr_file == null) {
          Common.trackSync("RENAME", this.rnfr_file_path, String.valueOf(item.getProperty("root_dir", "")) + item.getProperty("name", "") + (this.rnfr_file_path.endsWith("/") ? "/" : ""), false, 0L, 0L, SG("root_dir"), item.getProperty("privs"), uiSG("clientid"), "");
          return "";
        } 
        if (!exists || (exists && overwrite) || (exists && !view) || this.rnfr_file.getProperty("url").equalsIgnoreCase(item.getProperty("url")) || this.rnfr_file.getProperty("url").toUpperCase().equals(String.valueOf(item.getProperty("url").toUpperCase()) + "/") || (new VRL(this.rnfr_file.getProperty("url"))).getPath().equalsIgnoreCase(vrl.getPath())) {
          if (vrl.toString().length() > ServerStatus.IG("max_url_length"))
            throw new IOException("File url length too long:" + vrl.toString().length() + " vs. " + ServerStatus.IG("max_url_length")); 
          SearchHandler.buildEntry(this.rnfr_file, this.uVFS, true, true);
          UserTools.updatePrivpath(uiSG("listen_ip_port"), uiSG("user_name"), String.valueOf(this.rnfr_file.getProperty("root_dir", "")) + this.rnfr_file.getProperty("name", ""), the_dir, item, null, this.uVFS);
          if (overwrite && !vrl.getPath().equalsIgnoreCase((new VRL(this.rnfr_file.getProperty("url"))).getPath()))
            if (c.stat((new VRL(this.rnfr_file.getProperty("url"))).getPath()) != null)
              c.delete(vrl.getPath());  
          if (exists && !view && !overwrite)
            STOR_handler.do_unique_rename(actual_item, vrl, c, false, Common.last(vrl.toString()), c.stat(vrl.getPath())); 
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
              Common.recurseCopy_U(srcPath, dstPath, true);
            } else {
              if (srcPath.equals(dstPath))
                dstPath = String.valueOf(dstPath) + " Copy"; 
              Common.recurseCopy_U(srcPath, dstPath, true);
            } 
            Common.recurseDelete_U(srcPath, false);
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
          return "";
        } 
        this.not_done = ftp_write_command("550", LOC.G("%RNTO-error%"));
        doErrorEvent(new Exception(uiSG("lastLog")));
        uiVG("failed_commands").addElement((new Date()).getTime());
        this.rnfr_file = null;
        this.rnfr_file_path = null;
        return "%RNTO-error%";
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
    String the_dir = fixupUnsafeChars(fixupDir(user_dir));
    if (!the_dir.endsWith("/"))
      the_dir = String.valueOf(the_dir) + "/"; 
    if (the_dir != null && the_dir.endsWith(" /"))
      the_dir = String.valueOf(the_dir.substring(0, the_dir.length() - 2)) + "/"; 
    Properties item = this.uVFS.get_item_parent(the_dir);
    if (item.getProperty("url").length() > ServerStatus.IG("max_url_length"))
      throw new IOException("File url length too long:" + item.getProperty("url").length() + " vs. " + ServerStatus.IG("max_url_length")); 
    if (check_access_privs(the_dir, uiSG("the_command"), item)) {
      changeProxyToCurrentDir(item);
      Log.log("FTP_SERVER", 3, String.valueOf(LOC.G("Using item:")) + item);
      GenericClient c = this.uVFS.getClient(item);
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
        this.not_done = ftp_write_command("257", LOC.G("\"$0\" directory created.", the_dir));
        VRL vrl = new VRL(item.getProperty("url"));
        String path = vrl.getPath();
        Properties fileItem = c.stat(vrl.getPath());
        Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Tracking make directory:")) + path);
        fileItem.put("the_command", "MAKEDIR");
        fileItem.put("the_command_data", path);
        fileItem.put("url", vrl);
        fileItem.put("the_file_path", path);
        fileItem.put("the_file_name", vrl.getName());
        fileItem.put("the_file_size", "0");
        fileItem.put("the_file_speed", "0");
        fileItem.put("the_file_start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        fileItem.put("the_file_end", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        fileItem.put("the_file_error", "");
        fileItem.put("the_file_type", "DIR");
        fileItem.put("the_file_status", "SUCCESS");
        ServerStatus.thisObj.statTools.add_item_stat(this, fileItem, "MAKEDIR");
        do_event5("MAKEDIR", fileItem);
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
      this.uVFS.reset();
      return "";
    } 
    this.not_done = ftp_write_command("550", LOC.G("%MKD-bad%"));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%MKD-bad%";
  }
  
  public void setFolderPrivs(GenericClient c, Properties item) throws Exception {
    Properties vfs_posix_settings = Common.get_vfs_posix_settings(item.getProperty("privs", ""), false);
    if (!vfs_posix_settings.getProperty("vfs_owner", "").equals("")) {
      c.setOwner((new VRL(item.getProperty("url"))).getPath(), ServerStatus.change_vars_to_values_static(vfs_posix_settings.getProperty("vfs_owner", ""), this.user, this.user_info, this), "");
      Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("VFS permission setting: Set owner of new folder to:")) + vfs_posix_settings.getProperty("vfs_owner", ""));
    } else if (!SG("default_owner_command").equals("")) {
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
    if (!vfs_posix_settings.getProperty("vfs_group", "").equals("")) {
      c.setGroup((new VRL(item.getProperty("url"))).getPath(), ServerStatus.change_vars_to_values_static(vfs_posix_settings.getProperty("vfs_group", ""), this.user, this.user_info, this), "");
      Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("VFS permission setting: Set group of new folder to:")) + vfs_posix_settings.getProperty("vfs_group", ""));
    } else if (!SG("default_group_command").equals("")) {
      c.setGroup((new VRL(item.getProperty("url"))).getPath(), ServerStatus.change_vars_to_values_static(SG("default_group_command"), this.user, this.user_info, this), "");
      Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set group of new folder to:")) + SG("default_group_command"));
    } else if (!item.getProperty("group", "").equals("group")) {
      try {
        Properties parentItem = item;
        c.setGroup((new VRL(item.getProperty("url"))).getPath(), parentItem.getProperty("group", "").trim(), "");
        Log.log("FTP_SERVER", 2, String.valueOf(LOC.G("Set group of new folder to:")) + parentItem.getProperty("group", "").trim());
      } catch (Exception e) {
        Log.log("FTP_SERVER", 2, e);
      } 
    } 
    String folderPrivs = vfs_posix_settings.getProperty("vfs_privs", "");
    if (folderPrivs.equals(""))
      folderPrivs = SG("default_folder_privs_command"); 
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
    boolean mfmt = uiSG("the_command").equals("MFMT");
    uiPUT("the_command", "MDTM");
    uiPUT("last_logged_command", "MDTM");
    String the_dir = uiSG("current_dir");
    String dateNumber = "";
    if (!uiSG("the_command_data").equals("")) {
      if (mfmt) {
        dateNumber = uiSG("the_command_data").split(" ")[0];
        uiPUT("the_command_data", uiSG("the_command_data").substring(uiSG("the_command_data").indexOf(" ") + 1));
      } else {
        if (uiSG("the_command_data").lastIndexOf(" ") >= 0)
          dateNumber = uiSG("the_command_data").substring(uiSG("the_command_data").lastIndexOf(" ")).trim(); 
        try {
          Long.parseLong(dateNumber);
          if (dateNumber.length() > 5) {
            dateNumber = (new StringBuffer(String.valueOf(Long.parseLong(dateNumber.trim())))).toString();
            uiPUT("the_command_data", uiSG("the_command_data").substring(0, uiSG("the_command_data").length() - dateNumber.length()).trim());
          } else {
            dateNumber = "";
          } 
        } catch (Exception e) {
          if (uiSG("the_command_data").indexOf(" ") >= 0)
            dateNumber = uiSG("the_command_data").substring(0, uiSG("the_command_data").indexOf(" ")).trim(); 
          try {
            Long.parseLong(dateNumber);
            if (dateNumber.length() > 5) {
              dateNumber = (new StringBuffer(String.valueOf(Long.parseLong(dateNumber.trim())))).toString();
              uiPUT("the_command_data", uiSG("the_command_data").substring(dateNumber.length() + 1));
            } else {
              dateNumber = "";
            } 
            Log.log("FTP_SERVER", 1, "4:dateNumber=" + dateNumber);
          } catch (Exception ee) {
            dateNumber = "";
          } 
        } 
      } 
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
    Calendar cal = new GregorianCalendar();
    Properties item = this.uVFS.get_item(the_dir);
    if (check_access_privs(the_dir, uiSG("the_command"), item) && the_dir.indexOf(":filetree") < 0) {
      SimpleDateFormat sdf_yyyyMMddHHmmss2 = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
      changeProxyToCurrentDir(item);
      GenericClient c = this.uVFS.getClient(item);
      try {
        Properties stat = c.stat((new VRL(item.getProperty("url"))).getPath());
        long time = Long.parseLong(stat.getProperty("modified"));
        if (dateNumber.trim().length() > 0)
          try {
            if (check_access_privs(the_dir, "STOR")) {
              Date d = sdf_yyyyMMddHHmmss2.parse(dateNumber.trim());
              cal.setTime(d);
              cal.setTimeInMillis((long)(cal.getTimeInMillis() + DG("timezone_offset") * 1000.0D * 60.0D * 60.0D));
              d = new Date(cal.getTime().getTime());
              boolean bool = ServerStatus.BG("disable_mdtm_modifications");
              if (!SG("disable_mdtm_modifications").equals("") && ServerStatus.BG("v9_beta"))
                bool = BG("disable_mdtm_modifications"); 
              if (!bool)
                c.mdtm((new VRL(item.getProperty("url"))).getPath(), d.getTime()); 
              stat = c.stat((new VRL(item.getProperty("url"))).getPath());
              time = Long.parseLong(stat.getProperty("modified"));
              cal.setTime(new Date(time));
              cal.setTimeInMillis((long)(cal.getTimeInMillis() + DG("timezone_offset") * 1000.0D * 60.0D * 60.0D));
              d = new Date(cal.getTime().getTime());
              if (ServerStatus.BG("mdtm_gmt"))
                sdf_yyyyMMddHHmmss2.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT"))); 
              if (mfmt) {
                this.not_done = ftp_write_command("213", "Modify=" + sdf_yyyyMMddHHmmss2.format(d) + "; " + uiSG("the_command_data"));
              } else {
                this.not_done = ftp_write_command("213", sdf_yyyyMMddHHmmss2.format(d));
              } 
              Common.trackSync("CHANGE", the_dir, null, false, Long.parseLong(item.getProperty("size", "0")), time, SG("root_dir"), item.getProperty("privs"), uiSG("clientid"), "");
              return "";
            } 
          } catch (Exception e) {
            Log.log("FTP_SERVER", 1, e);
            time = -1L;
          }  
        boolean disable_mdtm_modifications = ServerStatus.BG("disable_mdtm_modifications");
        if (!SG("disable_mdtm_modifications").equals("") && ServerStatus.BG("v9_beta"))
          disable_mdtm_modifications = BG("disable_mdtm_modifications"); 
        if (time >= 0L || disable_mdtm_modifications) {
          cal.setTime(new Date(time));
          cal.setTimeInMillis((long)(cal.getTimeInMillis() + DG("timezone_offset") * 1000.0D * 60.0D * 60.0D));
          Date d = new Date(cal.getTime().getTime());
          if (ServerStatus.BG("mdtm_gmt"))
            sdf_yyyyMMddHHmmss2.setCalendar(Calendar.getInstance(new SimpleTimeZone(0, "GMT"))); 
          if (mfmt) {
            this.not_done = ftp_write_command("213", "Modify=" + sdf_yyyyMMddHHmmss2.format(d) + "; " + uiSG("the_command_data"));
            return "";
          } 
          this.not_done = ftp_write_command("213", sdf_yyyyMMddHHmmss2.format(d));
          return "";
        } 
        this.not_done = ftp_write_command("550", LOC.G("%MDTM-wrong%"));
        uiVG("failed_commands").addElement((new Date()).getTime());
        return "%MDTM-wrong%";
      } finally {
        c = this.uVFS.releaseClient(c);
      } 
    } 
    this.not_done = ftp_write_command("550", LOC.G("File not found, or access denied."));
    doErrorEvent(new Exception(uiSG("lastLog")));
    uiVG("failed_commands").addElement((new Date()).getTime());
    return "%MDTM-bad%";
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
  
  public static Properties build_password_rules(Properties user) {
    if (user == null)
      user = new Properties(); 
    Properties password_rules = new Properties();
    password_rules.put("min_password_length", user.getProperty("min_password_length", ServerStatus.SG("min_password_length")));
    password_rules.put("min_password_numbers", user.getProperty("min_password_numbers", ServerStatus.SG("min_password_numbers")));
    password_rules.put("min_password_lowers", user.getProperty("min_password_lowers", ServerStatus.SG("min_password_lowers")));
    password_rules.put("min_password_uppers", user.getProperty("min_password_uppers", ServerStatus.SG("min_password_uppers")));
    password_rules.put("min_password_specials", user.getProperty("min_password_specials", ServerStatus.SG("min_password_specials")));
    password_rules.put("unsafe_password_chars", user.getProperty("unsafe_password_chars", ServerStatus.SG("unsafe_password_chars")));
    password_rules.put("password_history_count", user.getProperty("password_history_count", ServerStatus.SG("password_history_count")));
    password_rules.put("random_password_length", user.getProperty("random_password_length", ServerStatus.SG("random_password_length")));
    return password_rules;
  }
  
  public String do_ChangePass(String theUser, String new_password) {
    String result = LOC.G("Password not changed.");
    Properties password_rules = build_password_rules(this.user);
    if (!Common.checkPasswordRequirements(new_password, this.user.getProperty("password_history", ""), password_rules).equals(""))
      return String.valueOf(LOC.G("ERROR:")) + " " + Common.checkPasswordRequirements(new_password, this.user.getProperty("password_history", ""), password_rules); 
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
          String history = Common.getPasswordHistory(old_password, tempUser.getProperty("password_history", ""), password_rules);
          tempUser.put("password_history", Common.getPasswordHistory(new_password, history, password_rules));
          if (!Common.dmz_mode)
            UserTools.writeUser(uiSG("listen_ip_port"), theUser, tempUser); 
          ok = true;
          response = "214 " + LOC.G("Password changed.");
          Log.log("SERVER", 0, String.valueOf(theUser) + " password changed by user.");
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
        Common.send_change_pass_email(this);
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
    String the_dir = "";
    if (user_dir == null) {
      the_dir = Common.url_decode(uiSG("current_dir"));
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
      uiPUT("the_command_data", Common.dots(uiSG("the_command_data")));
    } else {
      the_dir = Common.dots(Common.url_decode(user_dir));
      if (the_dir.equals("/"))
        the_dir = SG("root_dir"); 
      if (the_dir.toUpperCase().startsWith("/") && !the_dir.toUpperCase().startsWith(SG("root_dir").toUpperCase()))
        the_dir = String.valueOf(SG("root_dir")) + the_dir.substring(1); 
    } 
    return the_dir;
  }
  
  public String fixupUnsafeChars(String the_dir) {
    String the_dir_root = Common.all_but_last(the_dir);
    String last_item = Common.last(the_dir);
    boolean need_slash = last_item.endsWith("/");
    if (need_slash)
      last_item = last_item.substring(0, last_item.length() - 1); 
    for (int x = 0; x < ServerStatus.SG("unsafe_filename_chars_rename").length(); x++)
      last_item = last_item.replace(ServerStatus.SG("unsafe_filename_chars_rename").charAt(x), '_'); 
    return String.valueOf(the_dir_root) + last_item + (need_slash ? "/" : "");
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
  
  public double DG(String data) {
    double x = 0.0D;
    try {
      x = Double.parseDouble(this.user.getProperty(data));
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
    Worker.startWorker(new Runnable(this, the_command_data, event) {
          final SessionCrush this$0;
          
          private final String val$the_command_data;
          
          private final boolean val$event;
          
          public void run() {
            try {
              Thread.sleep(2900L);
              this.this$0.put("blockUploads", "false");
              this.this$0.doFileAbortEvent(this.val$the_command_data, this.val$event);
            } catch (Exception exception) {}
          }
        });
    doFileAbortEvent(the_command_data, event);
  }
  
  public void doFileAbortEvent(String the_command_data, boolean event) throws Exception {
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
    add_log("[" + this.server_item.getProperty("serverType", "ftp") + ":" + uiSG("user_number") + "_" + uiSG("sock_port") + ":" + uiSG("user_name") + ":" + uiSG("user_ip") + "] " + SG("WROTE") + ": *" + ServerStatus.thisObj.change_vars_to_values(data, this) + "*", logged_command);
    return true;
  }
  
  public static void doPaste(SessionCrush thisSession, StringBuffer status, String[] names, String destPath, String command) {
    // Byte code:
    //   0: ldc_w 'OK'
    //   3: astore #5
    //   5: iconst_0
    //   6: istore #6
    //   8: goto -> 2387
    //   11: aload_0
    //   12: invokevirtual active : ()V
    //   15: aload_2
    //   16: iload #6
    //   18: aaload
    //   19: invokevirtual trim : ()Ljava/lang/String;
    //   22: astore #7
    //   24: aload #7
    //   26: aload_0
    //   27: ldc_w 'root_dir'
    //   30: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   33: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   36: ifeq -> 58
    //   39: aload #7
    //   41: aload_0
    //   42: ldc_w 'root_dir'
    //   45: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   48: invokevirtual length : ()I
    //   51: iconst_1
    //   52: isub
    //   53: invokevirtual substring : (I)Ljava/lang/String;
    //   56: astore #7
    //   58: aload_0
    //   59: aload #7
    //   61: invokevirtual getStandardizedDir : (Ljava/lang/String;)Ljava/lang/String;
    //   64: astore #8
    //   66: aload_0
    //   67: getfield uVFS : Lcrushftp/server/VFS;
    //   70: aload #8
    //   72: invokevirtual get_item : (Ljava/lang/String;)Ljava/util/Properties;
    //   75: astore #9
    //   77: aload #9
    //   79: ifnonnull -> 115
    //   82: new java/lang/StringBuffer
    //   85: dup
    //   86: aload #5
    //   88: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   91: invokespecial <init> : (Ljava/lang/String;)V
    //   94: ldc_w '\\r\\nItem not found:'
    //   97: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   100: aload_2
    //   101: iload #6
    //   103: aaload
    //   104: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   107: invokevirtual toString : ()Ljava/lang/String;
    //   110: astore #5
    //   112: goto -> 2384
    //   115: aload #9
    //   117: ldc_w 'type'
    //   120: ldc_w 'FILE'
    //   123: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   126: ldc_w 'DIR'
    //   129: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   132: ifeq -> 189
    //   135: iconst_0
    //   136: istore #10
    //   138: iconst_0
    //   139: istore #11
    //   141: goto -> 174
    //   144: aload_2
    //   145: iload #11
    //   147: aaload
    //   148: invokevirtual trim : ()Ljava/lang/String;
    //   151: astore #12
    //   153: aload_2
    //   154: iload #6
    //   156: aaload
    //   157: invokevirtual trim : ()Ljava/lang/String;
    //   160: aload #12
    //   162: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   165: ifeq -> 171
    //   168: iconst_1
    //   169: istore #10
    //   171: iinc #11, 1
    //   174: iload #11
    //   176: iload #6
    //   178: if_icmplt -> 144
    //   181: iload #10
    //   183: ifeq -> 189
    //   186: goto -> 2384
    //   189: aload #9
    //   191: ldc_w 'url'
    //   194: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   197: astore #10
    //   199: aload #10
    //   201: ldc '/'
    //   203: invokevirtual endsWith : (Ljava/lang/String;)Z
    //   206: ifne -> 250
    //   209: aload #9
    //   211: ldc_w 'type'
    //   214: ldc ''
    //   216: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   219: ldc_w 'DIR'
    //   222: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   225: ifeq -> 250
    //   228: new java/lang/StringBuffer
    //   231: dup
    //   232: aload #10
    //   234: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   237: invokespecial <init> : (Ljava/lang/String;)V
    //   240: ldc '/'
    //   242: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   245: invokevirtual toString : ()Ljava/lang/String;
    //   248: astore #10
    //   250: new com/crushftp/client/VRL
    //   253: dup
    //   254: aload #10
    //   256: invokespecial <init> : (Ljava/lang/String;)V
    //   259: astore #11
    //   261: aconst_null
    //   262: astore #12
    //   264: aload_0
    //   265: getfield uVFS : Lcrushftp/server/VFS;
    //   268: aload #9
    //   270: invokevirtual getClient : (Ljava/util/Properties;)Lcom/crushftp/client/GenericClient;
    //   273: astore #13
    //   275: aload #13
    //   277: aload #11
    //   279: invokevirtual getUsername : ()Ljava/lang/String;
    //   282: aload #11
    //   284: invokevirtual getPassword : ()Ljava/lang/String;
    //   287: aconst_null
    //   288: invokevirtual login : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   291: pop
    //   292: aload #13
    //   294: aload #11
    //   296: invokevirtual getPath : ()Ljava/lang/String;
    //   299: invokevirtual stat : (Ljava/lang/String;)Ljava/util/Properties;
    //   302: astore #12
    //   304: goto -> 330
    //   307: astore #15
    //   309: jsr -> 315
    //   312: aload #15
    //   314: athrow
    //   315: astore #14
    //   317: aload_0
    //   318: getfield uVFS : Lcrushftp/server/VFS;
    //   321: aload #13
    //   323: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   326: astore #13
    //   328: ret #14
    //   330: jsr -> 315
    //   333: aload_0
    //   334: aload #8
    //   336: ldc_w 'DELE'
    //   339: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;)Z
    //   342: istore #14
    //   344: aload_0
    //   345: aload #8
    //   347: ldc_w 'RETR'
    //   350: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;)Z
    //   353: ifeq -> 2348
    //   356: aload_3
    //   357: invokestatic url_decode : (Ljava/lang/String;)Ljava/lang/String;
    //   360: astore #15
    //   362: aload #15
    //   364: aload_0
    //   365: ldc_w 'root_dir'
    //   368: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   371: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   374: ifeq -> 396
    //   377: aload #15
    //   379: aload_0
    //   380: ldc_w 'root_dir'
    //   383: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   386: invokevirtual length : ()I
    //   389: iconst_1
    //   390: isub
    //   391: invokevirtual substring : (I)Ljava/lang/String;
    //   394: astore #15
    //   396: aload_0
    //   397: aload #15
    //   399: invokevirtual getStandardizedDir : (Ljava/lang/String;)Ljava/lang/String;
    //   402: astore #16
    //   404: aload_0
    //   405: getfield uVFS : Lcrushftp/server/VFS;
    //   408: aload #16
    //   410: invokevirtual get_item : (Ljava/lang/String;)Ljava/util/Properties;
    //   413: astore #17
    //   415: new com/crushftp/client/VRL
    //   418: dup
    //   419: new java/lang/StringBuffer
    //   422: dup
    //   423: aload #17
    //   425: ldc_w 'url'
    //   428: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   431: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   434: invokespecial <init> : (Ljava/lang/String;)V
    //   437: aload #17
    //   439: ldc_w 'url'
    //   442: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   445: ldc '/'
    //   447: invokevirtual endsWith : (Ljava/lang/String;)Z
    //   450: ifeq -> 458
    //   453: ldc ''
    //   455: goto -> 460
    //   458: ldc '/'
    //   460: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   463: invokevirtual toString : ()Ljava/lang/String;
    //   466: invokespecial <init> : (Ljava/lang/String;)V
    //   469: astore #18
    //   471: aload_0
    //   472: aload #16
    //   474: ldc_w 'STOR'
    //   477: invokevirtual check_access_privs : (Ljava/lang/String;Ljava/lang/String;)Z
    //   480: ifeq -> 2309
    //   483: ldc ''
    //   485: astore #19
    //   487: iconst_1
    //   488: istore #20
    //   490: new com/crushftp/client/VRL
    //   493: dup
    //   494: new java/lang/StringBuffer
    //   497: dup
    //   498: aload #18
    //   500: invokevirtual toString : ()Ljava/lang/String;
    //   503: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   506: invokespecial <init> : (Ljava/lang/String;)V
    //   509: aload #11
    //   511: invokevirtual getName : ()Ljava/lang/String;
    //   514: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   517: aload #12
    //   519: ldc_w 'type'
    //   522: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   525: ldc_w 'DIR'
    //   528: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   531: ifeq -> 539
    //   534: ldc '/'
    //   536: goto -> 541
    //   539: ldc ''
    //   541: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   544: invokevirtual toString : ()Ljava/lang/String;
    //   547: invokespecial <init> : (Ljava/lang/String;)V
    //   550: invokevirtual toString : ()Ljava/lang/String;
    //   553: aload #11
    //   555: invokevirtual toString : ()Ljava/lang/String;
    //   558: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   561: ifeq -> 772
    //   564: iconst_0
    //   565: istore #20
    //   567: new com/crushftp/client/VRL
    //   570: dup
    //   571: new java/lang/StringBuffer
    //   574: dup
    //   575: aload #18
    //   577: invokevirtual toString : ()Ljava/lang/String;
    //   580: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   583: invokespecial <init> : (Ljava/lang/String;)V
    //   586: aload #11
    //   588: invokevirtual getName : ()Ljava/lang/String;
    //   591: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   594: aload #12
    //   596: ldc_w 'type'
    //   599: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   602: ldc_w 'DIR'
    //   605: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   608: ifeq -> 616
    //   611: ldc '/'
    //   613: goto -> 618
    //   616: ldc ''
    //   618: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   621: invokevirtual toString : ()Ljava/lang/String;
    //   624: invokespecial <init> : (Ljava/lang/String;)V
    //   627: invokevirtual toString : ()Ljava/lang/String;
    //   630: astore #21
    //   632: aload #11
    //   634: invokevirtual toString : ()Ljava/lang/String;
    //   637: astore #22
    //   639: goto -> 657
    //   642: aload #21
    //   644: iconst_0
    //   645: aload #21
    //   647: invokevirtual length : ()I
    //   650: iconst_1
    //   651: isub
    //   652: invokevirtual substring : (II)Ljava/lang/String;
    //   655: astore #21
    //   657: aload #21
    //   659: ldc '/'
    //   661: invokevirtual endsWith : (Ljava/lang/String;)Z
    //   664: ifne -> 642
    //   667: goto -> 685
    //   670: aload #22
    //   672: iconst_0
    //   673: aload #22
    //   675: invokevirtual length : ()I
    //   678: iconst_1
    //   679: isub
    //   680: invokevirtual substring : (II)Ljava/lang/String;
    //   683: astore #22
    //   685: aload #22
    //   687: ldc '/'
    //   689: invokevirtual endsWith : (Ljava/lang/String;)Z
    //   692: ifne -> 670
    //   695: aload #21
    //   697: aload #22
    //   699: invokevirtual equals : (Ljava/lang/Object;)Z
    //   702: ifeq -> 741
    //   705: iconst_1
    //   706: istore #20
    //   708: new java/lang/StringBuffer
    //   711: dup
    //   712: aload #19
    //   714: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   717: invokespecial <init> : (Ljava/lang/String;)V
    //   720: ldc_w '_copy_'
    //   723: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   726: iconst_3
    //   727: invokestatic makeBoundary : (I)Ljava/lang/String;
    //   730: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   733: invokevirtual toString : ()Ljava/lang/String;
    //   736: astore #19
    //   738: goto -> 772
    //   741: new java/lang/StringBuffer
    //   744: dup
    //   745: aload #5
    //   747: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   750: invokespecial <init> : (Ljava/lang/String;)V
    //   753: ldc '\\r\\n'
    //   755: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   758: ldc_w 'Cannot copy item into itself.'
    //   761: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   764: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   767: invokevirtual toString : ()Ljava/lang/String;
    //   770: astore #5
    //   772: iload #20
    //   774: ifeq -> 2384
    //   777: aload_0
    //   778: aload_0
    //   779: ldc_w 'lastUploadStats'
    //   782: invokevirtual uiVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   785: aload #11
    //   787: aload #18
    //   789: ldc_w 'RENAME'
    //   792: invokevirtual trackAndUpdateUploads : (Ljava/util/Vector;Lcom/crushftp/client/VRL;Lcom/crushftp/client/VRL;Ljava/lang/String;)V
    //   795: aload #9
    //   797: aload_0
    //   798: getfield uVFS : Lcrushftp/server/VFS;
    //   801: iconst_1
    //   802: iconst_1
    //   803: invokestatic buildEntry : (Ljava/util/Properties;Lcrushftp/server/VFS;ZZ)V
    //   806: new java/lang/StringBuffer
    //   809: dup
    //   810: aload #15
    //   812: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   815: invokespecial <init> : (Ljava/lang/String;)V
    //   818: aload #11
    //   820: invokevirtual getName : ()Ljava/lang/String;
    //   823: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   826: aload #12
    //   828: ldc_w 'type'
    //   831: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   834: ldc_w 'DIR'
    //   837: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   840: ifeq -> 848
    //   843: ldc '/'
    //   845: goto -> 850
    //   848: ldc ''
    //   850: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   853: invokevirtual toString : ()Ljava/lang/String;
    //   856: astore #15
    //   858: new java/lang/StringBuffer
    //   861: dup
    //   862: aload #17
    //   864: ldc_w 'url'
    //   867: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   870: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   873: invokespecial <init> : (Ljava/lang/String;)V
    //   876: aload #17
    //   878: ldc_w 'url'
    //   881: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   884: ldc '/'
    //   886: invokevirtual endsWith : (Ljava/lang/String;)Z
    //   889: ifeq -> 897
    //   892: ldc ''
    //   894: goto -> 899
    //   897: ldc '/'
    //   899: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   902: aload #11
    //   904: invokevirtual getName : ()Ljava/lang/String;
    //   907: invokestatic url_encode : (Ljava/lang/String;)Ljava/lang/String;
    //   910: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   913: aload #19
    //   915: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   918: aload #12
    //   920: ldc_w 'type'
    //   923: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   926: ldc_w 'DIR'
    //   929: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   932: ifeq -> 940
    //   935: ldc '/'
    //   937: goto -> 942
    //   940: ldc ''
    //   942: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   945: invokevirtual toString : ()Ljava/lang/String;
    //   948: astore #21
    //   950: new com/crushftp/client/VRL
    //   953: dup
    //   954: aload #21
    //   956: invokespecial <init> : (Ljava/lang/String;)V
    //   959: astore #22
    //   961: aload_0
    //   962: getfield uVFS : Lcrushftp/server/VFS;
    //   965: aload #9
    //   967: invokevirtual getClient : (Ljava/util/Properties;)Lcom/crushftp/client/GenericClient;
    //   970: astore #23
    //   972: aload #23
    //   974: aload #11
    //   976: invokevirtual getUsername : ()Ljava/lang/String;
    //   979: aload #11
    //   981: invokevirtual getPassword : ()Ljava/lang/String;
    //   984: aconst_null
    //   985: invokevirtual login : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   988: pop
    //   989: aload #9
    //   991: invokevirtual clone : ()Ljava/lang/Object;
    //   994: checkcast java/util/Properties
    //   997: astore #24
    //   999: aload #24
    //   1001: ldc_w 'url'
    //   1004: aload #21
    //   1006: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1009: pop
    //   1010: aload_0
    //   1011: getfield uVFS : Lcrushftp/server/VFS;
    //   1014: aload #24
    //   1016: invokevirtual getClient : (Ljava/util/Properties;)Lcom/crushftp/client/GenericClient;
    //   1019: astore #25
    //   1021: aload #25
    //   1023: aload #22
    //   1025: invokevirtual getUsername : ()Ljava/lang/String;
    //   1028: aload #22
    //   1030: invokevirtual getPassword : ()Ljava/lang/String;
    //   1033: aconst_null
    //   1034: invokevirtual login : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1037: pop
    //   1038: aload_1
    //   1039: dup
    //   1040: astore #26
    //   1042: monitorenter
    //   1043: aload_1
    //   1044: invokevirtual toString : ()Ljava/lang/String;
    //   1047: ldc_w 'CANCELLED'
    //   1050: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1053: ifeq -> 1067
    //   1056: new java/lang/Exception
    //   1059: dup
    //   1060: ldc_w 'CANCELLED'
    //   1063: invokespecial <init> : (Ljava/lang/String;)V
    //   1066: athrow
    //   1067: aload #26
    //   1069: monitorexit
    //   1070: goto -> 1077
    //   1073: aload #26
    //   1075: monitorexit
    //   1076: athrow
    //   1077: aload #11
    //   1079: aload #22
    //   1081: aload #23
    //   1083: aload #25
    //   1085: iconst_0
    //   1086: iconst_1
    //   1087: aload_1
    //   1088: invokestatic recurseCopy : (Lcom/crushftp/client/VRL;Lcom/crushftp/client/VRL;Lcom/crushftp/client/GenericClient;Lcom/crushftp/client/GenericClient;IZLjava/lang/StringBuffer;)V
    //   1091: aload_0
    //   1092: getfield uVFS : Lcrushftp/server/VFS;
    //   1095: aload #23
    //   1097: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   1100: astore #23
    //   1102: aload_0
    //   1103: getfield uVFS : Lcrushftp/server/VFS;
    //   1106: aload #25
    //   1108: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   1111: astore #25
    //   1113: aload_1
    //   1114: dup
    //   1115: astore #26
    //   1117: monitorenter
    //   1118: aload_1
    //   1119: invokevirtual toString : ()Ljava/lang/String;
    //   1122: ldc_w 'CANCELLED'
    //   1125: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1128: ifeq -> 1142
    //   1131: new java/lang/Exception
    //   1134: dup
    //   1135: ldc_w 'CANCELLED'
    //   1138: invokespecial <init> : (Ljava/lang/String;)V
    //   1141: athrow
    //   1142: aload_1
    //   1143: iconst_0
    //   1144: invokevirtual setLength : (I)V
    //   1147: aload_1
    //   1148: ldc_w 'Updating search references...'
    //   1151: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1154: pop
    //   1155: aload #26
    //   1157: monitorexit
    //   1158: goto -> 1165
    //   1161: aload #26
    //   1163: monitorexit
    //   1164: athrow
    //   1165: aload #17
    //   1167: aload_0
    //   1168: getfield uVFS : Lcrushftp/server/VFS;
    //   1171: iconst_0
    //   1172: iconst_0
    //   1173: invokestatic buildEntry : (Ljava/util/Properties;Lcrushftp/server/VFS;ZZ)V
    //   1176: aload #7
    //   1178: aload_0
    //   1179: ldc_w 'root_dir'
    //   1182: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1185: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   1188: ifne -> 1222
    //   1191: new java/lang/StringBuffer
    //   1194: dup
    //   1195: aload_0
    //   1196: ldc_w 'root_dir'
    //   1199: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1202: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1205: invokespecial <init> : (Ljava/lang/String;)V
    //   1208: aload #7
    //   1210: iconst_1
    //   1211: invokevirtual substring : (I)Ljava/lang/String;
    //   1214: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1217: invokevirtual toString : ()Ljava/lang/String;
    //   1220: astore #7
    //   1222: aload #15
    //   1224: aload_0
    //   1225: ldc_w 'root_dir'
    //   1228: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1231: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   1234: ifne -> 1268
    //   1237: new java/lang/StringBuffer
    //   1240: dup
    //   1241: aload_0
    //   1242: ldc_w 'root_dir'
    //   1245: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1248: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1251: invokespecial <init> : (Ljava/lang/String;)V
    //   1254: aload #15
    //   1256: iconst_1
    //   1257: invokevirtual substring : (I)Ljava/lang/String;
    //   1260: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1263: invokevirtual toString : ()Ljava/lang/String;
    //   1266: astore #15
    //   1268: aload #9
    //   1270: ldc_w 'url'
    //   1273: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1276: ldc_w '1'
    //   1279: iconst_1
    //   1280: invokestatic getPreviewPath : (Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;
    //   1283: astore #26
    //   1285: aload #24
    //   1287: ldc_w 'url'
    //   1290: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1293: ldc_w '1'
    //   1296: iconst_1
    //   1297: invokestatic getPreviewPath : (Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;
    //   1300: astore #27
    //   1302: new java/lang/StringBuffer
    //   1305: dup
    //   1306: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   1309: pop
    //   1310: ldc_w 'previews_path'
    //   1313: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1316: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1319: invokespecial <init> : (Ljava/lang/String;)V
    //   1322: aload #26
    //   1324: iconst_1
    //   1325: invokevirtual substring : (I)Ljava/lang/String;
    //   1328: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1331: invokevirtual toString : ()Ljava/lang/String;
    //   1334: astore #28
    //   1336: new java/lang/StringBuffer
    //   1339: dup
    //   1340: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   1343: pop
    //   1344: ldc_w 'previews_path'
    //   1347: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1350: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1353: invokespecial <init> : (Ljava/lang/String;)V
    //   1356: aload #27
    //   1358: iconst_1
    //   1359: invokevirtual substring : (I)Ljava/lang/String;
    //   1362: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1365: invokevirtual toString : ()Ljava/lang/String;
    //   1368: astore #29
    //   1370: new com/crushftp/client/File_U
    //   1373: dup
    //   1374: new java/lang/StringBuffer
    //   1377: dup
    //   1378: aload #28
    //   1380: invokestatic all_but_last : (Ljava/lang/String;)Ljava/lang/String;
    //   1383: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1386: invokespecial <init> : (Ljava/lang/String;)V
    //   1389: ldc_w '../index.txt'
    //   1392: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1395: invokevirtual toString : ()Ljava/lang/String;
    //   1398: invokespecial <init> : (Ljava/lang/String;)V
    //   1401: invokevirtual exists : ()Z
    //   1404: ifeq -> 1475
    //   1407: new com/crushftp/client/File_U
    //   1410: dup
    //   1411: aload #29
    //   1413: invokestatic all_but_last : (Ljava/lang/String;)Ljava/lang/String;
    //   1416: invokespecial <init> : (Ljava/lang/String;)V
    //   1419: invokevirtual mkdirs : ()Z
    //   1422: pop
    //   1423: new java/lang/StringBuffer
    //   1426: dup
    //   1427: aload #28
    //   1429: invokestatic all_but_last : (Ljava/lang/String;)Ljava/lang/String;
    //   1432: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1435: invokespecial <init> : (Ljava/lang/String;)V
    //   1438: ldc_w '../index.txt'
    //   1441: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1444: invokevirtual toString : ()Ljava/lang/String;
    //   1447: new java/lang/StringBuffer
    //   1450: dup
    //   1451: aload #29
    //   1453: invokestatic all_but_last : (Ljava/lang/String;)Ljava/lang/String;
    //   1456: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1459: invokespecial <init> : (Ljava/lang/String;)V
    //   1462: ldc_w '../index.txt'
    //   1465: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1468: invokevirtual toString : ()Ljava/lang/String;
    //   1471: iconst_1
    //   1472: invokestatic copy_U : (Ljava/lang/String;Ljava/lang/String;Z)V
    //   1475: aload #4
    //   1477: ldc_w 'cut_paste'
    //   1480: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
    //   1483: ifeq -> 1706
    //   1486: iload #14
    //   1488: ifeq -> 1631
    //   1491: aload_1
    //   1492: dup
    //   1493: astore #30
    //   1495: monitorenter
    //   1496: aload_1
    //   1497: invokevirtual toString : ()Ljava/lang/String;
    //   1500: ldc_w 'CANCELLED'
    //   1503: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1506: ifeq -> 1520
    //   1509: new java/lang/Exception
    //   1512: dup
    //   1513: ldc_w 'CANCELLED'
    //   1516: invokespecial <init> : (Ljava/lang/String;)V
    //   1519: athrow
    //   1520: aload_1
    //   1521: iconst_0
    //   1522: invokevirtual setLength : (I)V
    //   1525: aload_1
    //   1526: ldc_w 'Removing original...'
    //   1529: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1532: pop
    //   1533: aload #30
    //   1535: monitorexit
    //   1536: goto -> 1543
    //   1539: aload #30
    //   1541: monitorexit
    //   1542: athrow
    //   1543: aload_0
    //   1544: getfield uVFS : Lcrushftp/server/VFS;
    //   1547: aload #9
    //   1549: invokevirtual getClient : (Ljava/util/Properties;)Lcom/crushftp/client/GenericClient;
    //   1552: astore #23
    //   1554: aload #23
    //   1556: aload #11
    //   1558: invokevirtual getUsername : ()Ljava/lang/String;
    //   1561: aload #11
    //   1563: invokevirtual getPassword : ()Ljava/lang/String;
    //   1566: aconst_null
    //   1567: invokevirtual login : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1570: pop
    //   1571: aload #11
    //   1573: iconst_0
    //   1574: aload #23
    //   1576: iconst_0
    //   1577: invokestatic recurseDelete : (Lcom/crushftp/client/VRL;ZLcom/crushftp/client/GenericClient;I)V
    //   1580: aload_0
    //   1581: getfield uVFS : Lcrushftp/server/VFS;
    //   1584: aload #25
    //   1586: invokevirtual releaseClient : (Lcom/crushftp/client/GenericClient;)Lcom/crushftp/client/GenericClient;
    //   1589: astore #23
    //   1591: ldc_w 'RENAME'
    //   1594: aload #7
    //   1596: aload #15
    //   1598: iconst_0
    //   1599: lconst_0
    //   1600: lconst_0
    //   1601: aload_0
    //   1602: ldc_w 'root_dir'
    //   1605: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1608: aload #9
    //   1610: ldc_w 'privs'
    //   1613: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1616: aload_0
    //   1617: ldc_w 'clientid'
    //   1620: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1623: ldc ''
    //   1625: invokestatic trackSync : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    //   1628: goto -> 1742
    //   1631: new java/lang/StringBuffer
    //   1634: dup
    //   1635: aload #5
    //   1637: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1640: invokespecial <init> : (Ljava/lang/String;)V
    //   1643: ldc '\\r\\n'
    //   1645: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1648: ldc_w 'Item $0 copied, but not 'cut' as you did not have delete permissions.'
    //   1651: aload #11
    //   1653: invokevirtual getName : ()Ljava/lang/String;
    //   1656: invokestatic G : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1659: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1662: invokevirtual toString : ()Ljava/lang/String;
    //   1665: astore #5
    //   1667: ldc_w 'CHANGE'
    //   1670: aload #15
    //   1672: aconst_null
    //   1673: iconst_1
    //   1674: lconst_0
    //   1675: lconst_0
    //   1676: aload_0
    //   1677: ldc_w 'root_dir'
    //   1680: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1683: aload #9
    //   1685: ldc_w 'privs'
    //   1688: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1691: aload_0
    //   1692: ldc_w 'clientid'
    //   1695: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1698: ldc ''
    //   1700: invokestatic trackSync : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    //   1703: goto -> 1742
    //   1706: ldc_w 'CHANGE'
    //   1709: aload #15
    //   1711: aconst_null
    //   1712: iconst_1
    //   1713: lconst_0
    //   1714: lconst_0
    //   1715: aload_0
    //   1716: ldc_w 'root_dir'
    //   1719: invokevirtual SG : (Ljava/lang/String;)Ljava/lang/String;
    //   1722: aload #9
    //   1724: ldc_w 'privs'
    //   1727: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1730: aload_0
    //   1731: ldc_w 'clientid'
    //   1734: invokevirtual uiSG : (Ljava/lang/String;)Ljava/lang/String;
    //   1737: ldc ''
    //   1739: invokestatic trackSync : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZJJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    //   1742: aload_0
    //   1743: invokevirtual active : ()V
    //   1746: aload_1
    //   1747: dup
    //   1748: astore #30
    //   1750: monitorenter
    //   1751: aload_1
    //   1752: invokevirtual toString : ()Ljava/lang/String;
    //   1755: ldc_w 'CANCELLED'
    //   1758: invokevirtual equals : (Ljava/lang/Object;)Z
    //   1761: ifeq -> 1775
    //   1764: new java/lang/Exception
    //   1767: dup
    //   1768: ldc_w 'CANCELLED'
    //   1771: invokespecial <init> : (Ljava/lang/String;)V
    //   1774: athrow
    //   1775: aload_1
    //   1776: iconst_0
    //   1777: invokevirtual setLength : (I)V
    //   1780: aload_1
    //   1781: ldc_w 'Generating event for copy/paste...'
    //   1784: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1787: pop
    //   1788: aload #30
    //   1790: monitorexit
    //   1791: goto -> 1798
    //   1794: aload #30
    //   1796: monitorexit
    //   1797: athrow
    //   1798: aload #9
    //   1800: astore #30
    //   1802: aload #30
    //   1804: invokevirtual clone : ()Ljava/lang/Object;
    //   1807: checkcast java/util/Properties
    //   1810: astore #30
    //   1812: ldc_w 'FTP_SERVER'
    //   1815: iconst_2
    //   1816: new java/lang/StringBuffer
    //   1819: dup
    //   1820: ldc_w 'Tracking rename:'
    //   1823: invokestatic G : (Ljava/lang/String;)Ljava/lang/String;
    //   1826: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   1829: invokespecial <init> : (Ljava/lang/String;)V
    //   1832: aload #15
    //   1834: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   1837: invokevirtual toString : ()Ljava/lang/String;
    //   1840: invokestatic log : (Ljava/lang/String;ILjava/lang/String;)Z
    //   1843: pop
    //   1844: aload #30
    //   1846: ldc_w 'the_command'
    //   1849: ldc_w 'RNTO'
    //   1852: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1855: pop
    //   1856: aload #30
    //   1858: ldc_w 'the_command_data'
    //   1861: aload #15
    //   1863: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1866: pop
    //   1867: aload #30
    //   1869: ldc_w 'the_file_path2'
    //   1872: aload #12
    //   1874: ldc_w 'root_dir'
    //   1877: ldc ''
    //   1879: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1882: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1885: pop
    //   1886: aload #30
    //   1888: ldc_w 'url_2'
    //   1891: aload #12
    //   1893: ldc_w 'url'
    //   1896: ldc ''
    //   1898: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1901: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1904: pop
    //   1905: aload #30
    //   1907: ldc_w 'the_file_name_2'
    //   1910: aload #12
    //   1912: ldc_w 'name'
    //   1915: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1918: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1921: pop
    //   1922: aload #30
    //   1924: ldc_w 'the_file_path'
    //   1927: aload #15
    //   1929: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1932: pop
    //   1933: aload #30
    //   1935: ldc_w 'the_file_name'
    //   1938: aload #9
    //   1940: ldc_w 'name'
    //   1943: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   1946: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1949: pop
    //   1950: aload #30
    //   1952: ldc_w 'the_file_size'
    //   1955: aload #12
    //   1957: ldc_w 'size'
    //   1960: ldc_w '0'
    //   1963: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   1966: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1969: pop
    //   1970: aload #30
    //   1972: ldc_w 'the_file_speed'
    //   1975: ldc_w '0'
    //   1978: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   1981: pop
    //   1982: aload #30
    //   1984: ldc_w 'the_file_start'
    //   1987: new java/lang/StringBuffer
    //   1990: dup
    //   1991: new java/util/Date
    //   1994: dup
    //   1995: invokespecial <init> : ()V
    //   1998: invokevirtual getTime : ()J
    //   2001: invokestatic valueOf : (J)Ljava/lang/String;
    //   2004: invokespecial <init> : (Ljava/lang/String;)V
    //   2007: invokevirtual toString : ()Ljava/lang/String;
    //   2010: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2013: pop
    //   2014: aload #30
    //   2016: ldc_w 'the_file_end'
    //   2019: new java/lang/StringBuffer
    //   2022: dup
    //   2023: new java/util/Date
    //   2026: dup
    //   2027: invokespecial <init> : ()V
    //   2030: invokevirtual getTime : ()J
    //   2033: invokestatic valueOf : (J)Ljava/lang/String;
    //   2036: invokespecial <init> : (Ljava/lang/String;)V
    //   2039: invokevirtual toString : ()Ljava/lang/String;
    //   2042: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2045: pop
    //   2046: aload #30
    //   2048: ldc_w 'the_file_error'
    //   2051: ldc ''
    //   2053: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2056: pop
    //   2057: aload #30
    //   2059: ldc_w 'the_file_status'
    //   2062: ldc_w 'SUCCESS'
    //   2065: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2068: pop
    //   2069: aload #30
    //   2071: invokevirtual clone : ()Ljava/lang/Object;
    //   2074: checkcast java/util/Properties
    //   2077: astore #31
    //   2079: aload #31
    //   2081: ldc_w 'url'
    //   2084: aload #31
    //   2086: ldc_w 'url_2'
    //   2089: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2092: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2095: pop
    //   2096: aload #31
    //   2098: ldc_w 'the_file_name'
    //   2101: aload #31
    //   2103: ldc_w 'the_file_name_2'
    //   2106: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2109: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2112: pop
    //   2113: aload #30
    //   2115: invokevirtual clone : ()Ljava/lang/Object;
    //   2118: checkcast java/util/Properties
    //   2121: astore #32
    //   2123: aload #32
    //   2125: ldc_w 'the_file_name'
    //   2128: new java/lang/StringBuffer
    //   2131: dup
    //   2132: aload #32
    //   2134: ldc_w 'the_file_name_2'
    //   2137: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2140: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2143: invokespecial <init> : (Ljava/lang/String;)V
    //   2146: ldc_w ':'
    //   2149: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2152: aload #32
    //   2154: ldc_w 'the_file_name'
    //   2157: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2160: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2163: invokevirtual toString : ()Ljava/lang/String;
    //   2166: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2169: pop
    //   2170: aload #32
    //   2172: ldc_w 'the_file_path'
    //   2175: new java/lang/StringBuffer
    //   2178: dup
    //   2179: aload #32
    //   2181: ldc_w 'the_file_path2'
    //   2184: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2187: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2190: invokespecial <init> : (Ljava/lang/String;)V
    //   2193: ldc_w ':'
    //   2196: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2199: aload #32
    //   2201: ldc_w 'the_file_path'
    //   2204: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2207: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2210: invokevirtual toString : ()Ljava/lang/String;
    //   2213: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2216: pop
    //   2217: aload #32
    //   2219: ldc_w 'url'
    //   2222: new java/lang/StringBuffer
    //   2225: dup
    //   2226: aload #32
    //   2228: ldc_w 'url_2'
    //   2231: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2234: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2237: invokespecial <init> : (Ljava/lang/String;)V
    //   2240: ldc_w ':'
    //   2243: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2246: aload #32
    //   2248: ldc_w 'url'
    //   2251: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   2254: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2257: invokevirtual toString : ()Ljava/lang/String;
    //   2260: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   2263: pop
    //   2264: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   2267: getfield statTools : Lcrushftp/db/StatTools;
    //   2270: aload_0
    //   2271: aload #32
    //   2273: ldc_w 'RENAME'
    //   2276: invokevirtual add_item_stat : (Lcrushftp/handlers/SessionCrush;Ljava/util/Properties;Ljava/lang/String;)Ljava/util/Properties;
    //   2279: pop
    //   2280: aload_0
    //   2281: ldc_w 'RENAME'
    //   2284: aload #30
    //   2286: aload #31
    //   2288: invokevirtual do_event5 : (Ljava/lang/String;Ljava/util/Properties;Ljava/util/Properties;)Ljava/util/Properties;
    //   2291: pop
    //   2292: goto -> 2384
    //   2295: astore #30
    //   2297: ldc 'SERVER'
    //   2299: iconst_1
    //   2300: aload #30
    //   2302: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   2305: pop
    //   2306: goto -> 2384
    //   2309: new java/lang/StringBuffer
    //   2312: dup
    //   2313: aload #5
    //   2315: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2318: invokespecial <init> : (Ljava/lang/String;)V
    //   2321: ldc '\\r\\n'
    //   2323: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2326: ldc_w 'Cannot copy $0 because you don't have write permission here.'
    //   2329: aload #11
    //   2331: invokevirtual getName : ()Ljava/lang/String;
    //   2334: invokestatic G : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   2337: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2340: invokevirtual toString : ()Ljava/lang/String;
    //   2343: astore #5
    //   2345: goto -> 2384
    //   2348: new java/lang/StringBuffer
    //   2351: dup
    //   2352: aload #5
    //   2354: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   2357: invokespecial <init> : (Ljava/lang/String;)V
    //   2360: ldc '\\r\\n'
    //   2362: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2365: ldc_w 'Cannot copy $0 because you don't have read permission here.'
    //   2368: aload #11
    //   2370: invokevirtual getName : ()Ljava/lang/String;
    //   2373: invokestatic G : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   2376: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2379: invokevirtual toString : ()Ljava/lang/String;
    //   2382: astore #5
    //   2384: iinc #6, 1
    //   2387: iload #6
    //   2389: aload_2
    //   2390: arraylength
    //   2391: if_icmplt -> 11
    //   2394: aload #5
    //   2396: ldc_w 'OK'
    //   2399: invokevirtual equals : (Ljava/lang/Object;)Z
    //   2402: ifeq -> 2413
    //   2405: ldc_w 'COMPLETED:OK'
    //   2408: astore #5
    //   2410: goto -> 2433
    //   2413: new java/lang/StringBuffer
    //   2416: dup
    //   2417: ldc_w 'ERROR:'
    //   2420: invokespecial <init> : (Ljava/lang/String;)V
    //   2423: aload #5
    //   2425: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2428: invokevirtual toString : ()Ljava/lang/String;
    //   2431: astore #5
    //   2433: aload_1
    //   2434: dup
    //   2435: astore #6
    //   2437: monitorenter
    //   2438: aload_1
    //   2439: iconst_0
    //   2440: invokevirtual setLength : (I)V
    //   2443: aload_1
    //   2444: aload #5
    //   2446: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2449: pop
    //   2450: aload #6
    //   2452: monitorexit
    //   2453: goto -> 2514
    //   2456: aload #6
    //   2458: monitorexit
    //   2459: athrow
    //   2460: astore #5
    //   2462: ldc 'SERVER'
    //   2464: iconst_0
    //   2465: aload #5
    //   2467: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   2470: pop
    //   2471: aload_1
    //   2472: dup
    //   2473: astore #6
    //   2475: monitorenter
    //   2476: aload_1
    //   2477: iconst_0
    //   2478: invokevirtual setLength : (I)V
    //   2481: aload_1
    //   2482: new java/lang/StringBuffer
    //   2485: dup
    //   2486: ldc_w 'ERROR:'
    //   2489: invokespecial <init> : (Ljava/lang/String;)V
    //   2492: aload #5
    //   2494: invokevirtual append : (Ljava/lang/Object;)Ljava/lang/StringBuffer;
    //   2497: invokevirtual toString : ()Ljava/lang/String;
    //   2500: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   2503: pop
    //   2504: aload #6
    //   2506: monitorexit
    //   2507: goto -> 2514
    //   2510: aload #6
    //   2512: monitorexit
    //   2513: athrow
    //   2514: return
    // Line number table:
    //   Java source line number -> byte code offset
    //   #4775	-> 0
    //   #4776	-> 5
    //   #4778	-> 11
    //   #4779	-> 15
    //   #4780	-> 24
    //   #4781	-> 58
    //   #4783	-> 66
    //   #4784	-> 77
    //   #4786	-> 82
    //   #4787	-> 112
    //   #4789	-> 115
    //   #4791	-> 135
    //   #4792	-> 138
    //   #4794	-> 144
    //   #4795	-> 153
    //   #4792	-> 171
    //   #4797	-> 181
    //   #4799	-> 189
    //   #4800	-> 199
    //   #4801	-> 250
    //   #4802	-> 261
    //   #4803	-> 264
    //   #4806	-> 275
    //   #4807	-> 292
    //   #4810	-> 307
    //   #4812	-> 312
    //   #4810	-> 315
    //   #4811	-> 317
    //   #4812	-> 328
    //   #4814	-> 333
    //   #4815	-> 344
    //   #4817	-> 356
    //   #4818	-> 362
    //   #4819	-> 396
    //   #4821	-> 404
    //   #4822	-> 415
    //   #4823	-> 471
    //   #4825	-> 483
    //   #4826	-> 487
    //   #4827	-> 490
    //   #4829	-> 564
    //   #4830	-> 567
    //   #4831	-> 632
    //   #4832	-> 639
    //   #4833	-> 642
    //   #4832	-> 657
    //   #4834	-> 667
    //   #4835	-> 670
    //   #4834	-> 685
    //   #4836	-> 695
    //   #4838	-> 705
    //   #4839	-> 708
    //   #4841	-> 741
    //   #4843	-> 772
    //   #4845	-> 777
    //   #4846	-> 795
    //   #4847	-> 806
    //   #4848	-> 858
    //   #4849	-> 950
    //   #4850	-> 961
    //   #4851	-> 972
    //   #4852	-> 989
    //   #4853	-> 999
    //   #4854	-> 1010
    //   #4855	-> 1021
    //   #4856	-> 1038
    //   #4858	-> 1043
    //   #4856	-> 1067
    //   #4860	-> 1077
    //   #4861	-> 1091
    //   #4862	-> 1102
    //   #4863	-> 1113
    //   #4865	-> 1118
    //   #4866	-> 1142
    //   #4867	-> 1147
    //   #4863	-> 1155
    //   #4869	-> 1165
    //   #4870	-> 1176
    //   #4871	-> 1222
    //   #4873	-> 1268
    //   #4874	-> 1285
    //   #4875	-> 1302
    //   #4876	-> 1336
    //   #4877	-> 1370
    //   #4879	-> 1407
    //   #4880	-> 1423
    //   #4883	-> 1475
    //   #4885	-> 1486
    //   #4887	-> 1491
    //   #4889	-> 1496
    //   #4890	-> 1520
    //   #4891	-> 1525
    //   #4887	-> 1533
    //   #4893	-> 1543
    //   #4894	-> 1554
    //   #4895	-> 1571
    //   #4896	-> 1580
    //   #4897	-> 1591
    //   #4901	-> 1631
    //   #4902	-> 1667
    //   #4907	-> 1706
    //   #4909	-> 1742
    //   #4912	-> 1746
    //   #4914	-> 1751
    //   #4915	-> 1775
    //   #4916	-> 1780
    //   #4912	-> 1788
    //   #4918	-> 1798
    //   #4919	-> 1802
    //   #4920	-> 1812
    //   #4921	-> 1844
    //   #4922	-> 1856
    //   #4923	-> 1867
    //   #4924	-> 1886
    //   #4925	-> 1905
    //   #4926	-> 1922
    //   #4927	-> 1933
    //   #4928	-> 1950
    //   #4929	-> 1970
    //   #4930	-> 1982
    //   #4931	-> 2014
    //   #4932	-> 2046
    //   #4933	-> 2057
    //   #4934	-> 2069
    //   #4935	-> 2079
    //   #4936	-> 2096
    //   #4937	-> 2113
    //   #4938	-> 2123
    //   #4939	-> 2170
    //   #4940	-> 2217
    //   #4941	-> 2264
    //   #4942	-> 2280
    //   #4944	-> 2295
    //   #4946	-> 2297
    //   #4950	-> 2309
    //   #4952	-> 2348
    //   #4776	-> 2384
    //   #4954	-> 2394
    //   #4955	-> 2413
    //   #4956	-> 2433
    //   #4958	-> 2438
    //   #4959	-> 2443
    //   #4956	-> 2450
    //   #4962	-> 2460
    //   #4964	-> 2462
    //   #4965	-> 2471
    //   #4967	-> 2476
    //   #4968	-> 2481
    //   #4965	-> 2504
    //   #4971	-> 2514
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	2515	0	thisSession	Lcrushftp/handlers/SessionCrush;
    //   0	2515	1	status	Ljava/lang/StringBuffer;
    //   0	2515	2	names	[Ljava/lang/String;
    //   0	2515	3	destPath	Ljava/lang/String;
    //   0	2515	4	command	Ljava/lang/String;
    //   5	2455	5	msg	Ljava/lang/String;
    //   8	2386	6	x	I
    //   24	2360	7	the_dir1	Ljava/lang/String;
    //   66	2318	8	src_item_path	Ljava/lang/String;
    //   77	2307	9	item	Ljava/util/Properties;
    //   138	51	10	skip	Z
    //   141	40	11	xx	I
    //   153	18	12	the_dir_tmp	Ljava/lang/String;
    //   199	2185	10	url1	Ljava/lang/String;
    //   261	2123	11	vrl	Lcom/crushftp/client/VRL;
    //   264	2120	12	stat	Ljava/util/Properties;
    //   275	2109	13	c	Lcom/crushftp/client/GenericClient;
    //   344	2040	14	deleteAllowed	Z
    //   362	1986	15	the_dir2	Ljava/lang/String;
    //   404	1944	16	dest_item_path	Ljava/lang/String;
    //   415	1933	17	item2	Ljava/util/Properties;
    //   471	1877	18	vrl2	Lcom/crushftp/client/VRL;
    //   487	1822	19	addon	Ljava/lang/String;
    //   490	1819	20	ok	Z
    //   632	140	21	s1	Ljava/lang/String;
    //   639	133	22	s2	Ljava/lang/String;
    //   950	1356	21	dest_url	Ljava/lang/String;
    //   961	1345	22	vrl_dest	Lcom/crushftp/client/VRL;
    //   972	1334	23	c1	Lcom/crushftp/client/GenericClient;
    //   999	1307	24	item_dest	Ljava/util/Properties;
    //   1021	1285	25	c2	Lcom/crushftp/client/GenericClient;
    //   1285	1021	26	the_dir_index1	Ljava/lang/String;
    //   1302	1004	27	the_dir_index2	Ljava/lang/String;
    //   1336	970	28	index1	Ljava/lang/String;
    //   1370	936	29	index2	Ljava/lang/String;
    //   1802	493	30	fileItem1	Ljava/util/Properties;
    //   2079	216	31	fileItem2	Ljava/util/Properties;
    //   2123	172	32	temp_rename	Ljava/util/Properties;
    //   2297	9	30	e	Ljava/lang/Exception;
    //   2462	52	5	e	Ljava/lang/Exception;
    // Exception table:
    //   from	to	target	type
    //   0	2460	2460	java/lang/Exception
    //   275	307	307	finally
    //   330	333	307	finally
    //   1043	1070	1073	finally
    //   1073	1076	1073	finally
    //   1118	1158	1161	finally
    //   1161	1164	1161	finally
    //   1496	1536	1539	finally
    //   1539	1542	1539	finally
    //   1746	2292	2295	java/lang/Exception
    //   1751	1791	1794	finally
    //   1794	1797	1794	finally
    //   2438	2453	2456	finally
    //   2456	2459	2456	finally
    //   2476	2507	2510	finally
    //   2510	2513	2510	finally
  }
}
