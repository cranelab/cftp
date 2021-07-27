package crushftp.handlers;

import crushftp.server.ServerSession;
import crushftp.server.ServerStatus;
import java.io.File;
import java.io.FileOutputStream;
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
    // Byte code:
    //   0: getstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   3: ifnonnull -> 698
    //   6: getstatic crushftp/handlers/SharedSession.sessionLock : Ljava/lang/Object;
    //   9: dup
    //   10: astore_0
    //   11: monitorenter
    //   12: getstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   15: ifnonnull -> 690
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
    //   184: ifeq -> 453
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
    //   235: ldc 'recent_user_list'
    //   237: invokestatic find : (Ljava/lang/String;)Lcrushftp/handlers/SharedSession;
    //   240: astore_2
    //   241: aload_2
    //   242: ldc 'recent_user_list'
    //   244: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   247: ifnull -> 410
    //   250: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   253: pop
    //   254: ldc 'recent_user_list'
    //   256: invokestatic siVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   259: aload_2
    //   260: ldc 'recent_user_list'
    //   262: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   265: checkcast java/util/Vector
    //   268: invokevirtual addAll : (Ljava/util/Collection;)Z
    //   271: pop
    //   272: getstatic crushftp/server/QuickConnect.syncUserNumbers : Ljava/lang/Object;
    //   275: dup
    //   276: astore_3
    //   277: monitorenter
    //   278: iconst_0
    //   279: istore #4
    //   281: iconst_0
    //   282: istore #5
    //   284: goto -> 334
    //   287: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   290: pop
    //   291: ldc 'recent_user_list'
    //   293: invokestatic siVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   296: iload #5
    //   298: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   301: checkcast java/util/Properties
    //   304: astore #6
    //   306: aload #6
    //   308: ldc 'user_number'
    //   310: ldc '0'
    //   312: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   315: invokestatic parseInt : (Ljava/lang/String;)I
    //   318: istore #7
    //   320: iload #7
    //   322: iload #4
    //   324: if_icmple -> 331
    //   327: iload #7
    //   329: istore #4
    //   331: iinc #5, 1
    //   334: iload #5
    //   336: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   339: pop
    //   340: ldc 'recent_user_list'
    //   342: invokestatic siVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   345: invokevirtual size : ()I
    //   348: if_icmplt -> 287
    //   351: aload_2
    //   352: ldc 'user_login_num'
    //   354: ldc '0'
    //   356: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   359: invokestatic parseInt : (Ljava/lang/String;)I
    //   362: istore #5
    //   364: iload #5
    //   366: iload #4
    //   368: if_icmple -> 375
    //   371: iload #5
    //   373: istore #4
    //   375: iinc #4, 1
    //   378: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   381: pop
    //   382: ldc 'user_login_num'
    //   384: new java/lang/StringBuffer
    //   387: dup
    //   388: iload #4
    //   390: invokestatic valueOf : (I)Ljava/lang/String;
    //   393: invokespecial <init> : (Ljava/lang/String;)V
    //   396: invokevirtual toString : ()Ljava/lang/String;
    //   399: invokestatic put_in : (Ljava/lang/String;Ljava/lang/Object;)V
    //   402: aload_3
    //   403: monitorexit
    //   404: goto -> 410
    //   407: aload_3
    //   408: monitorexit
    //   409: athrow
    //   410: ldc 'running_tasks'
    //   412: invokestatic find : (Ljava/lang/String;)Lcrushftp/handlers/SharedSession;
    //   415: astore_3
    //   416: aload_3
    //   417: ldc 'running_tasks'
    //   419: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   422: ifnull -> 447
    //   425: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   428: pop
    //   429: ldc 'running_tasks'
    //   431: invokestatic siVG : (Ljava/lang/String;)Ljava/util/Vector;
    //   434: aload_3
    //   435: ldc 'running_tasks'
    //   437: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
    //   440: checkcast java/util/Vector
    //   443: invokevirtual addAll : (Ljava/util/Collection;)Z
    //   446: pop
    //   447: aload_1
    //   448: invokevirtual close : ()V
    //   451: aconst_null
    //   452: astore_1
    //   453: new java/io/File
    //   456: dup
    //   457: new java/lang/StringBuffer
    //   460: dup
    //   461: ldc 'crushftp.prefs'
    //   463: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   466: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   469: invokespecial <init> : (Ljava/lang/String;)V
    //   472: ldc 'md4_hashes.obj'
    //   474: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   477: invokevirtual toString : ()Ljava/lang/String;
    //   480: invokespecial <init> : (Ljava/lang/String;)V
    //   483: invokevirtual exists : ()Z
    //   486: ifeq -> 612
    //   489: new java/io/ObjectInputStream
    //   492: dup
    //   493: new java/io/FileInputStream
    //   496: dup
    //   497: new java/lang/StringBuffer
    //   500: dup
    //   501: ldc 'crushftp.prefs'
    //   503: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   506: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   509: invokespecial <init> : (Ljava/lang/String;)V
    //   512: ldc 'md4_hashes.obj'
    //   514: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   517: invokevirtual toString : ()Ljava/lang/String;
    //   520: invokespecial <init> : (Ljava/lang/String;)V
    //   523: invokespecial <init> : (Ljava/io/InputStream;)V
    //   526: astore_1
    //   527: aload_1
    //   528: invokevirtual readObject : ()Ljava/lang/Object;
    //   531: checkcast java/util/Properties
    //   534: astore_2
    //   535: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   538: getfield server_info : Ljava/util/Properties;
    //   541: ldc 'md4_hashes'
    //   543: aload_2
    //   544: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   547: pop
    //   548: aload_1
    //   549: invokevirtual close : ()V
    //   552: aconst_null
    //   553: astore_1
    //   554: goto -> 612
    //   557: astore_2
    //   558: ldc 'SERVER'
    //   560: iconst_0
    //   561: aload_2
    //   562: invokestatic log : (Ljava/lang/String;ILjava/lang/Throwable;)Z
    //   565: pop
    //   566: aload_1
    //   567: ifnull -> 578
    //   570: aload_1
    //   571: invokevirtual close : ()V
    //   574: goto -> 578
    //   577: astore_3
    //   578: new java/io/File
    //   581: dup
    //   582: new java/lang/StringBuffer
    //   585: dup
    //   586: ldc 'crushftp.prefs'
    //   588: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   591: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   594: invokespecial <init> : (Ljava/lang/String;)V
    //   597: ldc 'sessions.obj'
    //   599: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   602: invokevirtual toString : ()Ljava/lang/String;
    //   605: invokespecial <init> : (Ljava/lang/String;)V
    //   608: invokevirtual delete : ()Z
    //   611: pop
    //   612: getstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   615: ifnonnull -> 628
    //   618: new java/util/Properties
    //   621: dup
    //   622: invokespecial <init> : ()V
    //   625: putstatic crushftp/handlers/SharedSession.thisObj : Ljava/util/Properties;
    //   628: invokestatic flush : ()V
    //   631: new java/io/File
    //   634: dup
    //   635: new java/lang/StringBuffer
    //   638: dup
    //   639: ldc 'crushftp.prefs'
    //   641: invokestatic getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   644: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   647: invokespecial <init> : (Ljava/lang/String;)V
    //   650: ldc 'cluster.xml'
    //   652: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   655: invokevirtual toString : ()Ljava/lang/String;
    //   658: invokespecial <init> : (Ljava/lang/String;)V
    //   661: invokevirtual exists : ()Z
    //   664: ifne -> 684
    //   667: getstatic crushftp/server/ServerStatus.thisObj : Lcrushftp/server/ServerStatus;
    //   670: pop
    //   671: ldc 'replicate_session_host_port'
    //   673: invokestatic SG : (Ljava/lang/String;)Ljava/lang/String;
    //   676: ldc ''
    //   678: invokevirtual equals : (Ljava/lang/Object;)Z
    //   681: ifne -> 690
    //   684: invokestatic init : ()V
    //   687: invokestatic flush : ()V
    //   690: aload_0
    //   691: monitorexit
    //   692: goto -> 698
    //   695: aload_0
    //   696: monitorexit
    //   697: athrow
    //   698: return
    // Line number table:
    //   Java source line number -> byte code offset
    //   #26	-> 0
    //   #28	-> 6
    //   #30	-> 12
    //   #32	-> 18
    //   #35	-> 20
    //   #37	-> 56
    //   #38	-> 83
    //   #39	-> 117
    //   #41	-> 151
    //   #43	-> 187
    //   #44	-> 225
    //   #45	-> 235
    //   #46	-> 241
    //   #48	-> 250
    //   #49	-> 272
    //   #51	-> 278
    //   #52	-> 281
    //   #54	-> 287
    //   #55	-> 306
    //   #56	-> 320
    //   #52	-> 331
    //   #58	-> 351
    //   #59	-> 364
    //   #60	-> 375
    //   #61	-> 378
    //   #49	-> 402
    //   #64	-> 410
    //   #65	-> 416
    //   #67	-> 425
    //   #69	-> 447
    //   #70	-> 451
    //   #72	-> 453
    //   #74	-> 489
    //   #75	-> 527
    //   #76	-> 535
    //   #77	-> 548
    //   #78	-> 552
    //   #81	-> 557
    //   #83	-> 558
    //   #86	-> 566
    //   #88	-> 577
    //   #91	-> 578
    //   #93	-> 612
    //   #94	-> 628
    //   #95	-> 631
    //   #97	-> 684
    //   #98	-> 687
    //   #28	-> 690
    //   #103	-> 698
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   20	670	1	ois	Ljava/io/ObjectInputStream;
    //   241	212	2	recent_users	Lcrushftp/handlers/SharedSession;
    //   281	121	4	maxNum	I
    //   284	67	5	x	I
    //   306	25	6	p	Ljava/util/Properties;
    //   320	11	7	userNum	I
    //   364	38	5	saved_maxNum	I
    //   416	37	3	running_tasks	Lcrushftp/handlers/SharedSession;
    //   535	19	2	md4_hashes	Ljava/util/Properties;
    //   558	54	2	e	Ljava/lang/Throwable;
    // Exception table:
    //   from	to	target	type
    //   12	692	695	finally
    //   20	554	557	java/lang/Throwable
    //   278	404	407	finally
    //   407	409	407	finally
    //   566	574	577	java/lang/Exception
    //   695	697	695	finally
  }
  
  public static void shutdown() {
    if (ServerStatus.BG("allow_session_caching")) {
      while (true) {
        if (ServerStatus.siVG("user_list").size() <= 0)
          break; 
        Properties user_info = ServerStatus.siVG("user_list").elementAt(0);
        ServerSession thisSession = (ServerSession)user_info.get("session");
        if (thisSession != null) {
          thisSession.do_kill();
          continue;
        } 
        ServerStatus.siVG("user_list").remove(user_info);
      } 
      SharedSession recent_users = find("recent_user_list");
      recent_users.put("recent_user_list", ServerStatus.siVG("recent_user_list"));
      recent_users.put("user_login_num", ServerStatus.siSG("user_login_num"));
    } 
    flush();
  }
  
  public static void flush() {
    if (ServerStatus.BG("allow_session_caching")) {
      SharedSession running_tasks = find("running_tasks");
      Vector running_tasks_v = (Vector)Common.CLONE(ServerStatus.siVG("running_tasks"));
      for (int x = 0; x < running_tasks_v.size(); x++) {
        Properties tracker = running_tasks_v.elementAt(x);
        if (tracker.getProperty("status", "").equals("running"))
          tracker.put("status", "Cancelled by shutdown."); 
      } 
      running_tasks.put("running_tasks", running_tasks_v);
      synchronized (sessionLock) {
        synchronized (sessionFindLock) {
          ObjectOutputStream oos = null;
          try {
            (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj")).delete();
            oos = new ObjectOutputStream(new FileOutputStream(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj"));
            oos.writeObject(thisObj);
            oos.flush();
            oos.close();
            oos = null;
            (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj")).delete();
            (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions2.obj")).renameTo(new File(String.valueOf(System.getProperty("crushftp.prefs")) + "sessions.obj"));
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
    if (replicate && isShared(this.id))
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
    if (cache == null)
      return null; 
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
    if (replicate && isShared(this.id))
      SharedSessionReplicated.send(this.id, "remove", key, null); 
    Properties cache = (Properties)thisObj.get(this.id);
    if (cache == null)
      return null; 
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
