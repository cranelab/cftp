package crushftp.handlers;

import com.crushftp.client.Worker;
import crushftp.server.ServerStatus;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Properties;

public class SharedSession {
  public static Object sessionLock = new Object();
  
  public static Object sessionFindLock = new Object();
  
  private static Properties thisObj = null;
  
  String id = "";
  
  private static void init() {
    // Byte code:
    //   0: getstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   3: ifnonnull -> 769
    //   6: getstatic crushftp/handlers/SharedSession.sessionLock : Ljava/lang/Object;
    //   9: dup
    //   10: astore_0
    //   11: monitorenter
    //   12: getstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   15: ifnonnull -> 761
    //   18: aconst_null
    //   19: astore_1
    //   20: new java/io/File
    //   23: dup
    //   24: new java/lang/StringBuffer
    //   27: dup
    //   28: ldc 'crushftp.plugins'
    //   30: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   33: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   36: invokespecial <init> : (Ljava/lang/String;)V
    //   39: ldc 'plugins/lib/sessionCache.xml'
    //   41: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   44: invokevirtual toString : ()Ljava/lang/String;
    //   47: invokespecial <init> : (Ljava/lang/String;)V
    //   50: invokevirtual exists : ()Z
    //   53: ifeq -> 151
    //   56: new java/lang/StringBuffer
    //   59: dup
    //   60: ldc 'crushftp.plugins'
    //   62: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   65: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   68: invokespecial <init> : (Ljava/lang/String;)V
    //   71: ldc 'plugins/cache/'
    //   73: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   76: invokevirtual toString : ()Ljava/lang/String;
    //   79: iconst_0
    //   80: invokestatic recurseDelete : (Ljava/lang/String;Z)V
    //   83: new java/io/File
    //   86: dup
    //   87: new java/lang/StringBuffer
    //   90: dup
    //   91: ldc 'crushftp.plugins'
    //   93: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   96: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   99: invokespecial <init> : (Ljava/lang/String;)V
    //   102: ldc 'plugins/lib/sessionCache.xml'
    //   104: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   107: invokevirtual toString : ()Ljava/lang/String;
    //   110: invokespecial <init> : (Ljava/lang/String;)V
    //   113: invokevirtual delete : ()Z
    //   116: pop
    //   117: new java/io/File
    //   120: dup
    //   121: new java/lang/StringBuffer
    //   124: dup
    //   125: ldc 'crushftp.plugins'
    //   127: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   130: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   133: invokespecial <init> : (Ljava/lang/String;)V
    //   136: ldc 'plugins/lib/ehcache-core-2.5.0.jar'
    //   138: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   141: invokevirtual toString : ()Ljava/lang/String;
    //   144: invokespecial <init> : (Ljava/lang/String;)V
    //   147: invokevirtual delete : ()Z
    //   150: pop
    //   151: new java/io/File
    //   154: dup
    //   155: new java/lang/StringBuffer
    //   158: dup
    //   159: ldc 'crushftp.prefs'
    //   161: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   164: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   167: invokespecial <init> : (Ljava/lang/String;)V
    //   170: ldc 'sessions.obj'
    //   172: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   175: invokevirtual toString : ()Ljava/lang/String;
    //   178: invokespecial <init> : (Ljava/lang/String;)V
    //   181: invokevirtual exists : ()Z
    //   184: ifeq -> 518
    //   187: new java/io/ObjectInputStream
    //   190: dup
    //   191: new java/io/FileInputStream
    //   194: dup
    //   195: new java/lang/StringBuffer
    //   198: dup
    //   199: ldc 'crushftp.prefs'
    //   201: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   204: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   207: invokespecial <init> : (Ljava/lang/String;)V
    //   210: ldc 'sessions.obj'
    //   212: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   215: invokevirtual toString : ()Ljava/lang/String;
    //   218: invokespecial <init> : (Ljava/lang/String;)V
    //   221: invokespecial <init> : (Ljava/io/InputStream;)V
    //   224: astore_1
    //   225: aload_1
    //   226: invokevirtual readObject : ()Ljava/lang/Object;
    //   229: checkcast java/util/Properties
    //   232: putstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   235: getstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   238: ldc 'crushftp.usernames.activity'
    //   240: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
    //   243: pop
    //   244: ldc 'crushftp.sessions'
    //   246: invokestatic find : (Ljava/lang/String;)Lcrushftp/handlers/SharedSession;
    //   249: astore_2
    //   250: aload_2
    //   251: invokevirtual keys : ()Ljava/util/Enumeration;
    //   254: astore_3
    //   255: aload_3
    //   256: invokeinterface hasMoreElements : ()Z
    //   261: ifeq -> 330
    //   264: aload_3
    //   265: invokeinterface nextElement : ()Ljava/lang/Object;
    //   270: invokevirtual toString : ()Ljava/lang/String;
    //   273: astore #4
    //   275: aload_2
    //   276: aload #4
    //   278: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   281: astore #5
    //   283: aload #5
    //   285: instanceof java/util/Properties
    //   288: ifeq -> 301
    //   291: aload_2
    //   292: aload #4
    //   294: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
    //   297: pop
    //   298: goto -> 330
    //   301: aload #5
    //   303: instanceof crushftp/handlers/SessionCrush
    //   306: ifeq -> 330
    //   309: aload #5
    //   311: checkcast crushftp/handlers/SessionCrush
    //   314: ldc 'last_activity'
    //   316: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   319: ifnonnull -> 330
    //   322: aload #5
    //   324: checkcast crushftp/handlers/SessionCrush
    //   327: invokevirtual active : ()V
    //   330: ldc 'recent_user_list'
    //   332: invokestatic find : (Ljava/lang/String;)Lcrushftp/handlers/SharedSession;
    //   335: astore #4
    //   337: aload #4
    //   339: ldc 'recent_user_list'
    //   341: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   344: ifnull -> 512
    //   347: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   350: pop
    //   351: ldc 'recent_user_list'
    //   353: invokestatic siVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   356: aload #4
    //   358: ldc 'recent_user_list'
    //   360: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   363: checkcast java/util/Vector
    //   366: invokevirtual addAll : (Ljava/util/Collection;)Z
    //   369: pop
    //   370: getstatic crushftp/server/QuickConnect.syncUserNumbers : Ljava/lang/Object;
    //   373: dup
    //   374: astore #5
    //   376: monitorenter
    //   377: iconst_0
    //   378: istore #6
    //   380: iconst_0
    //   381: istore #7
    //   383: goto -> 433
    //   386: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   389: pop
    //   390: ldc 'recent_user_list'
    //   392: invokestatic siVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   395: iload #7
    //   397: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   400: checkcast java/util/Properties
    //   403: astore #8
    //   405: aload #8
    //   407: ldc 'user_number'
    //   409: ldc '0'
    //   411: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   414: invokestatic parseInt : (Ljava/lang/String;)I
    //   417: istore #9
    //   419: iload #9
    //   421: iload #6
    //   423: if_icmple -> 430
    //   426: iload #9
    //   428: istore #6
    //   430: iinc #7, 1
    //   433: iload #7
    //   435: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   438: pop
    //   439: ldc 'recent_user_list'
    //   441: invokestatic siVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   444: invokevirtual size : ()I
    //   447: if_icmplt -> 386
    //   450: aload #4
    //   452: ldc 'user_login_num'
    //   454: ldc '0'
    //   456: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   459: invokestatic parseInt : (Ljava/lang/String;)I
    //   462: istore #7
    //   464: iload #7
    //   466: iload #6
    //   468: if_icmple -> 475
    //   471: iload #7
    //   473: istore #6
    //   475: iinc #6, 1
    //   478: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   481: pop
    //   482: ldc 'user_login_num'
    //   484: new java/lang/StringBuffer
    //   487: dup
    //   488: iload #6
    //   490: invokestatic valueOf : (I)Ljava/lang/String;
    //   493: invokespecial <init> : (Ljava/lang/String;)V
    //   496: invokevirtual toString : ()Ljava/lang/String;
    //   499: invokestatic put_in : (Ljava/lang/String;Ljava/lang/Object;)V
    //   502: aload #5
    //   504: monitorexit
    //   505: goto -> 512
    //   508: aload #5
    //   510: monitorexit
    //   511: athrow
    //   512: aload_1
    //   513: invokevirtual close : ()V
    //   516: aconst_null
    //   517: astore_1
    //   518: new java/io/File
    //   521: dup
    //   522: new java/lang/StringBuffer
    //   525: dup
    //   526: ldc 'crushftp.prefs'
    //   528: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   531: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   534: invokespecial <init> : (Ljava/lang/String;)V
    //   537: ldc 'md4_hashes.obj'
    //   539: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   542: invokevirtual toString : ()Ljava/lang/String;
    //   545: invokespecial <init> : (Ljava/lang/String;)V
    //   548: invokevirtual exists : ()Z
    //   551: ifeq -> 677
    //   554: new java/io/ObjectInputStream
    //   557: dup
    //   558: new java/io/FileInputStream
    //   561: dup
    //   562: new java/lang/StringBuffer
    //   565: dup
    //   566: ldc 'crushftp.prefs'
    //   568: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   571: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   574: invokespecial <init> : (Ljava/lang/String;)V
    //   577: ldc 'md4_hashes.obj'
    //   579: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   582: invokevirtual toString : ()Ljava/lang/String;
    //   585: invokespecial <init> : (Ljava/lang/String;)V
    //   588: invokespecial <init> : (Ljava/io/InputStream;)V
    //   591: astore_1
    //   592: aload_1
    //   593: invokevirtual readObject : ()Ljava/lang/Object;
    //   596: checkcast java/util/Properties
    //   599: astore_2
    //   600: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   603: getfield server_info : Ljava/util/Properties;
    //   606: ldc 'md4_hashes'
    //   608: aload_2
    //   609: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   612: pop
    //   613: aload_1
    //   614: invokevirtual close : ()V
    //   617: aconst_null
    //   618: astore_1
    //   619: goto -> 677
    //   622: astore_2
    //   623: ldc 'SERVER'
    //   625: iconst_0
    //   626: aload_2
    //   627: invokestatic log : (Ljava/lang/String;ILjava/lang/Throwable;)Z
    //   630: pop
    //   631: aload_1
    //   632: ifnull -> 643
    //   635: aload_1
    //   636: invokevirtual close : ()V
    //   639: goto -> 643
    //   642: astore_3
    //   643: new java/io/File
    //   646: dup
    //   647: new java/lang/StringBuffer
    //   650: dup
    //   651: ldc 'crushftp.prefs'
    //   653: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   656: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   659: invokespecial <init> : (Ljava/lang/String;)V
    //   662: ldc 'sessions.obj'
    //   664: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   667: invokevirtual toString : ()Ljava/lang/String;
    //   670: invokespecial <init> : (Ljava/lang/String;)V
    //   673: invokevirtual delete : ()Z
    //   676: pop
    //   677: getstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   680: ifnonnull -> 693
    //   683: new java/util/Properties
    //   686: dup
    //   687: invokespecial <init> : ()V
    //   690: putstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   693: new java/io/File
    //   696: dup
    //   697: new java/lang/StringBuffer
    //   700: dup
    //   701: ldc 'crushftp.prefs'
    //   703: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   706: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   709: invokespecial <init> : (Ljava/lang/String;)V
    //   712: ldc 'cluster.xml'
    //   714: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   717: invokevirtual toString : ()Ljava/lang/String;
    //   720: invokespecial <init> : (Ljava/lang/String;)V
    //   723: invokevirtual exists : ()Z
    //   726: ifne -> 746
    //   729: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   732: pop
    //   733: ldc 'replicate_session_host_port'
    //   735: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   738: ldc ''
    //   740: invokevirtual equals : (Ljava/lang/Object;)Z
    //   743: ifne -> 761
    //   746: invokestatic init : ()V
    //   749: goto -> 761
    //   752: astore_2
    //   753: ldc 'SERVER'
    //   755: iconst_0
    //   756: aload_2
    //   757: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
    //   760: pop
    //   761: aload_0
    //   762: monitorexit
    //   763: goto -> 769
    //   766: aload_0
    //   767: monitorexit
    //   768: athrow
    //   769: return
    // Line number table:
    //   Java source line number -> byte code offset
    //   #28	-> 0
    //   #30	-> 6
    //   #32	-> 12
    //   #34	-> 18
    //   #37	-> 20
    //   #39	-> 56
    //   #40	-> 83
    //   #41	-> 117
    //   #43	-> 151
    //   #45	-> 187
    //   #46	-> 225
    //   #47	-> 235
    //   #48	-> 244
    //   #49	-> 250
    //   #50	-> 255
    //   #52	-> 264
    //   #53	-> 275
    //   #54	-> 283
    //   #55	-> 301
    //   #57	-> 309
    //   #60	-> 330
    //   #61	-> 337
    //   #63	-> 347
    //   #64	-> 370
    //   #66	-> 377
    //   #67	-> 380
    //   #69	-> 386
    //   #70	-> 405
    //   #71	-> 419
    //   #67	-> 430
    //   #73	-> 450
    //   #74	-> 464
    //   #75	-> 475
    //   #76	-> 478
    //   #64	-> 502
    //   #79	-> 512
    //   #80	-> 516
    //   #82	-> 518
    //   #84	-> 554
    //   #85	-> 592
    //   #86	-> 600
    //   #87	-> 613
    //   #88	-> 617
    //   #91	-> 622
    //   #93	-> 623
    //   #96	-> 631
    //   #98	-> 642
    //   #101	-> 643
    //   #103	-> 677
    //   #104	-> 693
    //   #108	-> 746
    //   #110	-> 752
    //   #112	-> 753
    //   #30	-> 761
    //   #118	-> 769
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   20	741	1	ois	Ljava/io/ObjectInputStream;
    //   250	268	2	user_sessions	Lcrushftp/handlers/SharedSession;
    //   255	263	3	keys	Ljava/util/Enumeration;
    //   275	55	4	key	Ljava/lang/String;
    //   283	47	5	o	Ljava/lang/Object;
    //   337	181	4	recent_users	Lcrushftp/handlers/SharedSession;
    //   380	122	6	maxNum	I
    //   383	67	7	x	I
    //   405	25	8	p	Ljava/util/Properties;
    //   419	11	9	userNum	I
    //   464	38	7	saved_maxNum	I
    //   600	19	2	md4_hashes	Ljava/util/Properties;
    //   623	54	2	e	Ljava/lang/Throwable;
    //   753	8	2	e	Ljava/lang/Exception;
    // Exception table:
    //   from	to	target	type
    //   12	763	766	finally
    //   20	619	622	java/lang/Throwable
    //   377	505	508	finally
    //   508	511	508	finally
    //   631	639	642	java/lang/Exception
    //   746	749	752	java/lang/Exception
    //   766	768	766	finally
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
      (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj")).delete();
      ObjectOutputStream oos1 = null;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      if (ServerStatus.BG("allow_session_caching_memory")) {
        oos1 = new ObjectOutputStream(baos);
      } else {
        oos1 = new ObjectOutputStream(new FileOutputStream(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj"));
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
              (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj")).delete();
              (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj")).renameTo(new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj"));
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
                FileOutputStream out = new FileOutputStream(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj");
                out.write(this.val$baos.toByteArray());
                out.close();
                (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj")).delete();
                (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj")).renameTo(new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj"));
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
