package com.crushftp.tunnel;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.HTTPClient;
import com.crushftp.tunnel2.Tunnel2;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class Uploader2 {
  public Properties controller = new Properties();
  
  long totalBytes = 1L;
  
  long transferedBytes = 0L;
  
  int totalItems = 0;
  
  int transferedItems = 0;
  
  String status2 = "";
  
  StringBuffer action = new StringBuffer();
  
  public Properties statusInfo = null;
  
  StringBuffer crushAuth = null;
  
  SimpleDateFormat sdf_rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  
  public Object statLock = new Object();
  
  Properties crossRef = new Properties();
  
  Vector ask = new Vector();
  
  Properties ask_response = new Properties();
  
  Vector clientPool = new Vector();
  
  public Uploader2(Properties statusInfo, StringBuffer crushAuth) {
    this.statusInfo = statusInfo;
    this.crushAuth = crushAuth;
  }
  
  public long getTotalBytes() {
    return this.totalBytes;
  }
  
  public long getTransferedBytes() {
    return this.transferedBytes;
  }
  
  public int getTotalItems() {
    return this.totalItems;
  }
  
  public int getTransferedItems() {
    return this.transferedItems;
  }
  
  public void refreshStatusInfo() {
    this.statusInfo.put("totalBytes", (new StringBuffer(String.valueOf(this.totalBytes))).toString());
    this.statusInfo.put("transferedBytes", (new StringBuffer(String.valueOf(this.transferedBytes))).toString());
    this.statusInfo.put("totalItems", (new StringBuffer(String.valueOf(this.totalItems))).toString());
    this.statusInfo.put("transferedItems", (new StringBuffer(String.valueOf(this.transferedItems))).toString());
  }
  
  public String getStatus() {
    return String.valueOf(this.statusInfo.getProperty("uploadStatus", "")) + this.statusInfo.getProperty("tunnelInfo", "");
  }
  
  public void pause() {
    Tunnel2.msg("PAUSE");
    this.action.setLength(0);
    this.action.append("pause");
    this.status2 = getStatus();
    try {
      Thread.sleep(200L);
    } catch (Exception exception) {}
    this.statusInfo.put("uploadStatus", "Paused:" + this.status2);
  }
  
  public void resume() {
    Tunnel2.msg("RESUME");
    this.statusInfo.put("uploadStatus", this.status2);
    this.action.setLength(0);
  }
  
  public void cancel() {
    Tunnel2.msg("CANCEL");
    this.status2 = getStatus();
    this.action.setLength(0);
    this.action.append("cancel");
    this.statusInfo.put("uploadStatus", "Cancelled:" + this.status2);
    (new Thread(new Runnable(this) {
          final Uploader2 this$0;
          
          public void run() {
            int loops = 0;
            while (this.this$0.action.toString().equals("cancel") && loops++ < 100) {
              try {
                Thread.sleep(100L);
              } catch (Exception exception) {}
            } 
          }
        })).start();
  }
  
  public String getAsk() {
    if (this.ask.size() > 0) {
      Properties p = this.ask.elementAt(0);
      this.ask_response.put(p.getProperty("uid"), p);
      this.ask.remove(0);
      return ":::ask=true:::path=" + p.getProperty("path") + ":::modified=" + p.getProperty("modified") + ":::size=" + p.getProperty("size") + ":::uid=" + p.getProperty("uid");
    } 
    return "";
  }
  
  public void go() {
    synchronized (this.statLock) {
      this.totalBytes = 1L;
      this.transferedBytes = 0L;
      this.totalItems = 0;
      this.transferedItems = 0;
    } 
    this.status2 = "";
    this.action.setLength(0);
    this.controller.put("statusInfo", this.statusInfo);
    refreshStatusInfo();
    Tunnel2.msg("Using Server URL:" + this.controller.getProperty("URL"));
    this.controller.put("serverFiles", new Properties());
    this.statusInfo.put("uploadStatus", "Finding files...");
    Vector parentfiles1 = new Vector();
    Vector parentfiles = new Vector();
    Vector files2 = new Vector();
    int loop = 1;
    while (this.controller.containsKey("P" + loop)) {
      parentfiles1.addElement(new File(this.controller.getProperty("P" + loop)));
      loop++;
    } 
    try {
      Tunnel2.msg("Files:" + parentfiles1.size());
      int x;
      for (x = 0; x < parentfiles1.size(); x++) {
        if (!Tunnel2.checkAction(this.action))
          return; 
        File f = parentfiles1.elementAt(x);
        this.statusInfo.put("uploadStatus", "Finding files " + f.getPath() + "... " + Common.percent(x, parentfiles1.size()));
        getAllFileListing(files2, f.getCanonicalPath(), 999);
        if (parentfiles.indexOf(f.getParentFile()) < 0)
          parentfiles.addElement(f.getParentFile()); 
      } 
      this.statusInfo.put("uploadStatus", "Getting file sizes...");
      Tunnel2.msg("Files2:" + files2.size());
      for (x = 0; x < files2.size(); x++) {
        if (!Tunnel2.checkAction(this.action))
          return; 
        this.statusInfo.put("uploadStatus", "Getting file sizes... " + Common.percent(x, files2.size()));
        File f = files2.elementAt(x);
        synchronized (this.statLock) {
          this.totalBytes += f.length();
          this.totalItems++;
        } 
      } 
    } catch (Exception e) {
      Tunnel2.msg(e);
    } 
    Object t = null;
    try {
      try {
        if (this.totalBytes > 1048576L && this.controller.getProperty("ALLOWTUNNEL", "true").equals("true"))
          t = AutoChannelProxy.enableAppletTunnel(this.controller, false, this.crushAuth); 
      } catch (Exception e) {
        Tunnel2.msg("Error checking for tunnel.");
        Tunnel2.msg(e);
      } 
      if (!Tunnel2.checkAction(this.action))
        return; 
      doUpload(parentfiles, files2, this.controller, "");
    } catch (Exception e) {
      Tunnel2.msg(e);
    } finally {
      this.controller.put("stopTunnel", "true");
      if (t != null)
        while (this.controller.containsKey("stopTunnel")) {
          try {
            Thread.sleep(100L);
          } catch (InterruptedException e) {
            e.printStackTrace();
          } 
        }  
      this.action.setLength(0);
    } 
    while (this.clientPool.size() > 0) {
      try {
        ((GenericClient)this.clientPool.remove(0)).close();
      } catch (Exception e) {
        e.printStackTrace();
      } 
    } 
  }
  
  public void doUpload(Vector parentfiles, Vector uploadFiles, Properties params, String keywords) {
    // Byte code:
    //   0: aload_0
    //   1: getfield controller : Ljava/util/Properties;
    //   4: ldc_w 'PARENTFILES'
    //   7: invokevirtual containsKey : (Ljava/lang/Object;)Z
    //   10: ifeq -> 27
    //   13: aload_0
    //   14: getfield controller : Ljava/util/Properties;
    //   17: ldc_w 'PARENTFILES'
    //   20: invokevirtual remove : (Ljava/lang/Object;)Ljava/lang/Object;
    //   23: checkcast java/util/Vector
    //   26: astore_1
    //   27: iconst_0
    //   28: istore #5
    //   30: new java/util/Vector
    //   33: dup
    //   34: invokespecial <init> : ()V
    //   37: astore #6
    //   39: new java/util/Vector
    //   42: dup
    //   43: invokespecial <init> : ()V
    //   46: astore #7
    //   48: aload_0
    //   49: getfield statusInfo : Ljava/util/Properties;
    //   52: ldc 'uploadStatus'
    //   54: ldc_w 'Upload:Checking for duplicate files, and sorting small and large files...'
    //   57: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   60: pop
    //   61: aload_2
    //   62: invokevirtual size : ()I
    //   65: istore #8
    //   67: iconst_0
    //   68: istore #9
    //   70: goto -> 130
    //   73: aload_0
    //   74: getfield statusInfo : Ljava/util/Properties;
    //   77: ldc 'uploadStatus'
    //   79: new java/lang/StringBuffer
    //   82: dup
    //   83: ldc_w 'Upload:Checking for duplicate files, and sorting small and large files... '
    //   86: invokespecial <init> : (Ljava/lang/String;)V
    //   89: iload #9
    //   91: iload #8
    //   93: invokestatic percent : (II)Ljava/lang/String;
    //   96: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   99: invokevirtual toString : ()Ljava/lang/String;
    //   102: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   105: pop
    //   106: aload_2
    //   107: iconst_0
    //   108: invokevirtual remove : (I)Ljava/lang/Object;
    //   111: checkcast java/io/File
    //   114: astore #10
    //   116: aload #6
    //   118: aload #10
    //   120: invokevirtual addElement : (Ljava/lang/Object;)V
    //   123: iinc #9, 1
    //   126: aload_0
    //   127: invokevirtual refreshStatusInfo : ()V
    //   130: aload_2
    //   131: invokevirtual size : ()I
    //   134: ifgt -> 73
    //   137: aload #7
    //   139: invokevirtual size : ()I
    //   142: aload #6
    //   144: invokevirtual size : ()I
    //   147: iadd
    //   148: istore #10
    //   150: goto -> 753
    //   153: aload_0
    //   154: invokevirtual refreshStatusInfo : ()V
    //   157: aload_0
    //   158: getfield action : Ljava/lang/StringBuffer;
    //   161: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
    //   164: ifne -> 168
    //   167: return
    //   168: iload #5
    //   170: iconst_1
    //   171: if_icmple -> 194
    //   174: aload_0
    //   175: getfield statusInfo : Ljava/util/Properties;
    //   178: ldc 'uploadStatus'
    //   180: ldc_w 'Upload:Recovering from an error, re-checking what the server received so we can resume.'
    //   183: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   186: pop
    //   187: aload_0
    //   188: invokevirtual getStatus : ()Ljava/lang/String;
    //   191: invokestatic msg : (Ljava/lang/String;)V
    //   194: aload_0
    //   195: getfield statusInfo : Ljava/util/Properties;
    //   198: ldc 'uploadStatus'
    //   200: ldc_w 'Uploading files...'
    //   203: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   206: pop
    //   207: aload_0
    //   208: invokevirtual getStatus : ()Ljava/lang/String;
    //   211: invokestatic msg : (Ljava/lang/String;)V
    //   214: aload_3
    //   215: ldc_w 'OVERWRITE'
    //   218: ldc ''
    //   220: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   223: astore #11
    //   225: new java/lang/StringBuffer
    //   228: dup
    //   229: ldc_w 'Connecting to URL:'
    //   232: invokespecial <init> : (Ljava/lang/String;)V
    //   235: aload_0
    //   236: getfield controller : Ljava/util/Properties;
    //   239: ldc 'URL'
    //   241: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   244: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   247: invokevirtual toString : ()Ljava/lang/String;
    //   250: invokestatic msg : (Ljava/lang/String;)V
    //   253: aload_0
    //   254: getfield statusInfo : Ljava/util/Properties;
    //   257: ldc 'uploadStatus'
    //   259: new java/lang/StringBuffer
    //   262: dup
    //   263: ldc_w 'Upload:Connecting to URL:'
    //   266: invokespecial <init> : (Ljava/lang/String;)V
    //   269: aload_0
    //   270: getfield controller : Ljava/util/Properties;
    //   273: ldc 'URL'
    //   275: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   278: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   281: invokevirtual toString : ()Ljava/lang/String;
    //   284: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   287: pop
    //   288: aload #7
    //   290: invokevirtual removeAllElements : ()V
    //   293: iload #5
    //   295: iconst_1
    //   296: if_icmple -> 327
    //   299: aload_3
    //   300: ldc_w 'OVERWRITE'
    //   303: ldc ''
    //   305: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   308: ldc_w 'OVERWRITE'
    //   311: invokevirtual equals : (Ljava/lang/Object;)Z
    //   314: ifeq -> 327
    //   317: aload_3
    //   318: ldc_w 'OVERWRITE'
    //   321: ldc 'RESUME'
    //   323: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   326: pop
    //   327: aload_0
    //   328: aload_0
    //   329: getfield controller : Ljava/util/Properties;
    //   332: ldc 'URL'
    //   334: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   337: aload_3
    //   338: aload #4
    //   340: aload #6
    //   342: aload_1
    //   343: iload #10
    //   345: invokevirtual uploadNormalFiles : (Ljava/lang/String;Ljava/util/Properties;Ljava/lang/String;Ljava/util/Vector;Ljava/util/Vector;I)V
    //   348: aload_0
    //   349: getfield statusInfo : Ljava/util/Properties;
    //   352: ldc 'uploadStatus'
    //   354: ldc ''
    //   356: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   359: pop
    //   360: aload_0
    //   361: invokevirtual getStatus : ()Ljava/lang/String;
    //   364: invokestatic msg : (Ljava/lang/String;)V
    //   367: aload_0
    //   368: getfield statLock : Ljava/lang/Object;
    //   371: dup
    //   372: astore #12
    //   374: monitorenter
    //   375: aload_0
    //   376: aload_0
    //   377: getfield totalBytes : J
    //   380: putfield transferedBytes : J
    //   383: aload #12
    //   385: monitorexit
    //   386: goto -> 393
    //   389: aload #12
    //   391: monitorexit
    //   392: athrow
    //   393: jsr -> 728
    //   396: goto -> 763
    //   399: astore #12
    //   401: aload #12
    //   403: invokestatic msg : (Ljava/lang/Exception;)V
    //   406: aload_0
    //   407: getfield action : Ljava/lang/StringBuffer;
    //   410: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
    //   413: ifne -> 420
    //   416: jsr -> 728
    //   419: return
    //   420: aload #12
    //   422: invokevirtual getMessage : ()Ljava/lang/String;
    //   425: ldc_w 'ERROR:'
    //   428: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   431: ifeq -> 475
    //   434: aload_0
    //   435: getfield statusInfo : Ljava/util/Properties;
    //   438: ldc 'uploadStatus'
    //   440: new java/lang/StringBuffer
    //   443: dup
    //   444: ldc_w 'Upload:'
    //   447: invokespecial <init> : (Ljava/lang/String;)V
    //   450: aload #12
    //   452: invokevirtual getMessage : ()Ljava/lang/String;
    //   455: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   458: invokevirtual toString : ()Ljava/lang/String;
    //   461: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   464: pop
    //   465: aload_0
    //   466: invokevirtual getStatus : ()Ljava/lang/String;
    //   469: invokestatic msg : (Ljava/lang/String;)V
    //   472: goto -> 393
    //   475: aload #12
    //   477: invokevirtual getMessage : ()Ljava/lang/String;
    //   480: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   483: ldc_w 'ACCESS IS DENIED'
    //   486: invokevirtual indexOf : (Ljava/lang/String;)I
    //   489: iflt -> 533
    //   492: aload_0
    //   493: getfield statusInfo : Ljava/util/Properties;
    //   496: ldc 'uploadStatus'
    //   498: new java/lang/StringBuffer
    //   501: dup
    //   502: ldc_w 'Upload:ERROR:'
    //   505: invokespecial <init> : (Ljava/lang/String;)V
    //   508: aload #12
    //   510: invokevirtual getMessage : ()Ljava/lang/String;
    //   513: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   516: invokevirtual toString : ()Ljava/lang/String;
    //   519: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   522: pop
    //   523: aload_0
    //   524: invokevirtual getStatus : ()Ljava/lang/String;
    //   527: invokestatic msg : (Ljava/lang/String;)V
    //   530: goto -> 393
    //   533: aload #12
    //   535: invokevirtual getMessage : ()Ljava/lang/String;
    //   538: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   541: ldc_w 'BROKEN PIPE'
    //   544: invokevirtual indexOf : (Ljava/lang/String;)I
    //   547: ifge -> 567
    //   550: aload #12
    //   552: invokevirtual getMessage : ()Ljava/lang/String;
    //   555: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   558: ldc_w 'HAS BEEN SHUTDOWN'
    //   561: invokevirtual indexOf : (Ljava/lang/String;)I
    //   564: iflt -> 614
    //   567: iload #5
    //   569: iconst_1
    //   570: if_icmple -> 614
    //   573: aload_0
    //   574: getfield statusInfo : Ljava/util/Properties;
    //   577: ldc 'uploadStatus'
    //   579: new java/lang/StringBuffer
    //   582: dup
    //   583: ldc_w 'Upload:ERROR:Server denied the file due to a restriction in filename or permissions:'
    //   586: invokespecial <init> : (Ljava/lang/String;)V
    //   589: aload #12
    //   591: invokevirtual getMessage : ()Ljava/lang/String;
    //   594: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   597: invokevirtual toString : ()Ljava/lang/String;
    //   600: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   603: pop
    //   604: aload_0
    //   605: invokevirtual getStatus : ()Ljava/lang/String;
    //   608: invokestatic msg : (Ljava/lang/String;)V
    //   611: goto -> 393
    //   614: aload_0
    //   615: getfield statusInfo : Ljava/util/Properties;
    //   618: ldc 'uploadStatus'
    //   620: new java/lang/StringBuffer
    //   623: dup
    //   624: ldc_w 'Upload:WARN:'
    //   627: invokespecial <init> : (Ljava/lang/String;)V
    //   630: aload #12
    //   632: invokevirtual getMessage : ()Ljava/lang/String;
    //   635: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   638: invokevirtual toString : ()Ljava/lang/String;
    //   641: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   644: pop
    //   645: aload_0
    //   646: invokevirtual getStatus : ()Ljava/lang/String;
    //   649: invokestatic msg : (Ljava/lang/String;)V
    //   652: aload #12
    //   654: invokevirtual getMessage : ()Ljava/lang/String;
    //   657: ldc_w '403'
    //   660: invokevirtual indexOf : (Ljava/lang/String;)I
    //   663: ifle -> 679
    //   666: aload_0
    //   667: getfield statusInfo : Ljava/util/Properties;
    //   670: ldc 'uploadStatus'
    //   672: ldc_w 'ERROR:Uploads are not allowed.'
    //   675: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   678: pop
    //   679: aload #12
    //   681: instanceof java/io/FileNotFoundException
    //   684: ifne -> 694
    //   687: iload #5
    //   689: bipush #10
    //   691: if_icmple -> 702
    //   694: aload_0
    //   695: getfield crushAuth : Ljava/lang/StringBuffer;
    //   698: iconst_0
    //   699: invokevirtual setLength : (I)V
    //   702: iload #5
    //   704: sipush #1000
    //   707: imul
    //   708: i2l
    //   709: invokestatic sleep : (J)V
    //   712: goto -> 750
    //   715: astore #13
    //   717: goto -> 750
    //   720: astore #15
    //   722: jsr -> 728
    //   725: aload #15
    //   727: athrow
    //   728: astore #14
    //   730: aload_0
    //   731: getfield action : Ljava/lang/StringBuffer;
    //   734: iconst_0
    //   735: invokevirtual setLength : (I)V
    //   738: aload_3
    //   739: ldc_w 'OVERWRITE'
    //   742: aload #11
    //   744: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   747: pop
    //   748: ret #14
    //   750: jsr -> 728
    //   753: iload #5
    //   755: iinc #5, 1
    //   758: bipush #60
    //   760: if_icmplt -> 153
    //   763: aload_0
    //   764: invokevirtual refreshStatusInfo : ()V
    //   767: return
    // Line number table:
    //   Java source line number -> byte code offset
    //   #320	-> 0
    //   #321	-> 27
    //   #322	-> 30
    //   #323	-> 39
    //   #324	-> 48
    //   #325	-> 61
    //   #326	-> 67
    //   #327	-> 70
    //   #329	-> 73
    //   #332	-> 106
    //   #336	-> 116
    //   #337	-> 123
    //   #338	-> 126
    //   #327	-> 130
    //   #341	-> 137
    //   #342	-> 150
    //   #344	-> 153
    //   #345	-> 157
    //   #346	-> 168
    //   #348	-> 174
    //   #349	-> 187
    //   #352	-> 194
    //   #353	-> 207
    //   #354	-> 214
    //   #357	-> 225
    //   #358	-> 253
    //   #360	-> 288
    //   #361	-> 293
    //   #363	-> 299
    //   #365	-> 327
    //   #367	-> 348
    //   #368	-> 360
    //   #369	-> 367
    //   #371	-> 375
    //   #369	-> 383
    //   #373	-> 393
    //   #375	-> 399
    //   #377	-> 401
    //   #378	-> 406
    //   #379	-> 420
    //   #381	-> 434
    //   #382	-> 465
    //   #383	-> 472
    //   #385	-> 475
    //   #387	-> 492
    //   #388	-> 523
    //   #389	-> 530
    //   #391	-> 533
    //   #393	-> 573
    //   #394	-> 604
    //   #395	-> 611
    //   #397	-> 614
    //   #398	-> 645
    //   #399	-> 652
    //   #401	-> 666
    //   #421	-> 679
    //   #423	-> 694
    //   #428	-> 702
    //   #430	-> 715
    //   #435	-> 720
    //   #438	-> 725
    //   #435	-> 728
    //   #436	-> 730
    //   #437	-> 738
    //   #438	-> 748
    //   #342	-> 753
    //   #440	-> 763
    //   #441	-> 767
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	768	0	this	Lcom/crushftp/tunnel/Uploader2;
    //   0	768	1	parentfiles	Ljava/util/Vector;
    //   0	768	2	uploadFiles	Ljava/util/Vector;
    //   0	768	3	params	Ljava/util/Properties;
    //   0	768	4	keywords	Ljava/lang/String;
    //   30	738	5	loops	I
    //   39	729	6	bigFiles	Ljava/util/Vector;
    //   48	720	7	files2	Ljava/util/Vector;
    //   67	701	8	totalCount	I
    //   70	698	9	pos	I
    //   116	14	10	f	Ljava/io/File;
    //   150	618	10	initialSize	I
    //   225	528	11	original_overwrite	Ljava/lang/String;
    //   401	316	12	e	Ljava/lang/Exception;
    // Exception table:
    //   from	to	target	type
    //   225	396	399	java/lang/Exception
    //   225	396	720	finally
    //   375	386	389	finally
    //   389	392	389	finally
    //   399	419	720	finally
    //   420	717	720	finally
    //   702	712	715	java/lang/Exception
    //   750	753	720	finally
  }
  
  public void sendMeta(GenericClient c, Properties params, String keywords) throws Exception {
    Properties params2 = new Properties();
    if (params != null) {
      Enumeration en = params.keys();
      while (en.hasMoreElements()) {
        String key = en.nextElement().toString();
        if (key.toUpperCase().startsWith("META_")) {
          String val = params.getProperty(key, "");
          while (key.toUpperCase().startsWith("META_"))
            key = key.substring("META_".length()); 
          params2.put(key, val);
        } 
      } 
    } 
    if (keywords != null && !keywords.trim().equals(""))
      params2.put("keywords", keywords); 
    if (params2.size() > 0)
      ((HTTPClient)c).sendMetaInfo(params2); 
  }
  
  private GenericClient getClient(String url) {
    synchronized (this.clientPool) {
      if (this.clientPool.size() > 0)
        return this.clientPool.remove(0); 
    } 
    GenericClient c = Common.getClient(url, "UPLOADER", null);
    c.setConfig("crushAuth", this.crushAuth.toString());
    return c;
  }
  
  private void releaseClient(GenericClient c) {
    this.clientPool.addElement(c);
  }
  
  public void uploadNormalFiles(String url, Properties params, String keywords, Vector files2, Vector parentfiles, int initialSize) throws Exception {
    Properties threads = new Properties();
    StringBuffer itemIndex = new StringBuffer();
    itemIndex.append("1");
    Runnable r = new Runnable(this, threads, files2, url, parentfiles, params, keywords, itemIndex, initialSize) {
        final Uploader2 this$0;
        
        private final Properties val$threads;
        
        private final Vector val$files2;
        
        private final String val$url;
        
        private final Vector val$parentfiles;
        
        private final Properties val$params;
        
        private final String val$keywords;
        
        private final StringBuffer val$itemIndex;
        
        private final int val$initialSize;
        
        public void run() {
          // Byte code:
          //   0: aload_0
          //   1: getfield val$threads : Ljava/util/Properties;
          //   4: invokestatic currentThread : ()Ljava/lang/Thread;
          //   7: invokevirtual get : (Ljava/lang/Object;)Ljava/lang/Object;
          //   10: checkcast java/util/Properties
          //   13: astore_1
          //   14: aconst_null
          //   15: astore_2
          //   16: goto -> 1395
          //   19: aload_0
          //   20: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   23: invokevirtual refreshStatusInfo : ()V
          //   26: aload_0
          //   27: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   30: aload_0
          //   31: getfield val$url : Ljava/lang/String;
          //   34: invokestatic access$0 : (Lcom/crushftp/tunnel/Uploader2;Ljava/lang/String;)Lcom/crushftp/client/GenericClient;
          //   37: astore_3
          //   38: aload_0
          //   39: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   42: getfield action : Ljava/lang/StringBuffer;
          //   45: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
          //   48: ifne -> 61
          //   51: new java/lang/Exception
          //   54: dup
          //   55: ldc 'Cancelled'
          //   57: invokespecial <init> : (Ljava/lang/String;)V
          //   60: athrow
          //   61: aload_0
          //   62: getfield val$files2 : Ljava/util/Vector;
          //   65: dup
          //   66: astore #4
          //   68: monitorenter
          //   69: aload_0
          //   70: getfield val$files2 : Ljava/util/Vector;
          //   73: invokevirtual size : ()I
          //   76: ifle -> 91
          //   79: aload_0
          //   80: getfield val$files2 : Ljava/util/Vector;
          //   83: iconst_0
          //   84: invokevirtual remove : (I)Ljava/lang/Object;
          //   87: checkcast java/io/File
          //   90: astore_2
          //   91: aload #4
          //   93: monitorexit
          //   94: goto -> 101
          //   97: aload #4
          //   99: monitorexit
          //   100: athrow
          //   101: aload_2
          //   102: ifnonnull -> 111
          //   105: jsr -> 1380
          //   108: goto -> 1405
          //   111: aload_2
          //   112: invokevirtual exists : ()Z
          //   115: ifeq -> 1210
          //   118: new java/lang/StringBuffer
          //   121: dup
          //   122: aload_1
          //   123: ldc 'index'
          //   125: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   128: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   131: invokespecial <init> : (Ljava/lang/String;)V
          //   134: ldc ':Uploading normally:'
          //   136: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   139: aload_2
          //   140: invokevirtual getName : ()Ljava/lang/String;
          //   143: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   146: invokevirtual toString : ()Ljava/lang/String;
          //   149: invokestatic msg : (Ljava/lang/String;)V
          //   152: iconst_0
          //   153: istore #4
          //   155: aload_0
          //   156: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   159: aload_2
          //   160: aload_0
          //   161: getfield val$parentfiles : Ljava/util/Vector;
          //   164: invokevirtual indexOfParent : (Ljava/io/File;Ljava/util/Vector;)I
          //   167: istore #5
          //   169: iload #5
          //   171: iflt -> 202
          //   174: aload_0
          //   175: getfield val$parentfiles : Ljava/util/Vector;
          //   178: iload #5
          //   180: invokevirtual elementAt : (I)Ljava/lang/Object;
          //   183: checkcast java/io/File
          //   186: astore #6
          //   188: aload_0
          //   189: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   192: aload #6
          //   194: invokevirtual getCanonicalPath : (Ljava/io/File;)Ljava/lang/String;
          //   197: invokevirtual length : ()I
          //   200: istore #4
          //   202: aload_0
          //   203: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   206: getfield controller : Ljava/util/Properties;
          //   209: ldc 'UPLOADPATH'
          //   211: ldc ''
          //   213: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   216: astore #6
          //   218: aload #6
          //   220: ldc '//'
          //   222: invokevirtual endsWith : (Ljava/lang/String;)Z
          //   225: ifeq -> 243
          //   228: aload #6
          //   230: iconst_0
          //   231: aload #6
          //   233: invokevirtual length : ()I
          //   236: iconst_1
          //   237: isub
          //   238: invokevirtual substring : (II)Ljava/lang/String;
          //   241: astore #6
          //   243: aload_2
          //   244: invokevirtual getCanonicalPath : ()Ljava/lang/String;
          //   247: bipush #92
          //   249: bipush #47
          //   251: invokevirtual replace : (CC)Ljava/lang/String;
          //   254: iload #4
          //   256: invokevirtual substring : (I)Ljava/lang/String;
          //   259: astore #7
          //   261: aload #6
          //   263: ldc '/'
          //   265: invokevirtual endsWith : (Ljava/lang/String;)Z
          //   268: ifeq -> 289
          //   271: aload #7
          //   273: ldc '/'
          //   275: invokevirtual startsWith : (Ljava/lang/String;)Z
          //   278: ifeq -> 289
          //   281: aload #7
          //   283: iconst_1
          //   284: invokevirtual substring : (I)Ljava/lang/String;
          //   287: astore #7
          //   289: new java/lang/StringBuffer
          //   292: dup
          //   293: aload #6
          //   295: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   298: invokespecial <init> : (Ljava/lang/String;)V
          //   301: aload #7
          //   303: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   306: invokevirtual toString : ()Ljava/lang/String;
          //   309: astore #8
          //   311: aload #8
          //   313: ldc '//'
          //   315: invokevirtual startsWith : (Ljava/lang/String;)Z
          //   318: ifeq -> 329
          //   321: aload #8
          //   323: iconst_1
          //   324: invokevirtual substring : (I)Ljava/lang/String;
          //   327: astore #8
          //   329: lconst_0
          //   330: lstore #9
          //   332: iconst_0
          //   333: istore #11
          //   335: aload_0
          //   336: getfield val$params : Ljava/util/Properties;
          //   339: ldc 'OVERWRITE'
          //   341: ldc ''
          //   343: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   346: ldc 'OVERWRITE'
          //   348: invokevirtual equals : (Ljava/lang/Object;)Z
          //   351: ifne -> 946
          //   354: aload_2
          //   355: invokevirtual isFile : ()Z
          //   358: ifeq -> 946
          //   361: aload_3
          //   362: aload #8
          //   364: invokevirtual stat : (Ljava/lang/String;)Ljava/util/Properties;
          //   367: astore #12
          //   369: aload #12
          //   371: ifnull -> 946
          //   374: aload_0
          //   375: getfield val$params : Ljava/util/Properties;
          //   378: ldc 'OVERWRITE'
          //   380: ldc ''
          //   382: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   385: ldc 'RESUME'
          //   387: invokevirtual equals : (Ljava/lang/Object;)Z
          //   390: ifeq -> 408
          //   393: aload #12
          //   395: ldc 'size'
          //   397: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   400: invokestatic parseLong : (Ljava/lang/String;)J
          //   403: lstore #9
          //   405: goto -> 946
          //   408: aload_0
          //   409: getfield val$params : Ljava/util/Properties;
          //   412: ldc 'OVERWRITE'
          //   414: ldc ''
          //   416: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   419: ldc 'SKIP'
          //   421: invokevirtual equals : (Ljava/lang/Object;)Z
          //   424: ifeq -> 433
          //   427: iconst_1
          //   428: istore #11
          //   430: goto -> 946
          //   433: aload_0
          //   434: getfield val$params : Ljava/util/Properties;
          //   437: ldc 'OVERWRITE'
          //   439: ldc ''
          //   441: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   444: ldc 'ASK'
          //   446: invokevirtual equals : (Ljava/lang/Object;)Z
          //   449: ifeq -> 946
          //   452: aload #12
          //   454: ldc 'uid'
          //   456: bipush #8
          //   458: invokestatic makeBoundary : (I)Ljava/lang/String;
          //   461: invokevirtual toUpperCase : ()Ljava/lang/String;
          //   464: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   467: pop
          //   468: aload #12
          //   470: ldc 'path'
          //   472: aload #8
          //   474: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   477: pop
          //   478: aload_0
          //   479: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   482: getfield ask : Ljava/util/Vector;
          //   485: aload #12
          //   487: invokevirtual addElement : (Ljava/lang/Object;)V
          //   490: goto -> 522
          //   493: aload_0
          //   494: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   497: getfield action : Ljava/lang/StringBuffer;
          //   500: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
          //   503: ifne -> 516
          //   506: new java/lang/Exception
          //   509: dup
          //   510: ldc 'Cancelled'
          //   512: invokespecial <init> : (Ljava/lang/String;)V
          //   515: athrow
          //   516: ldc2_w 300
          //   519: invokestatic sleep : (J)V
          //   522: aload_0
          //   523: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   526: getfield ask : Ljava/util/Vector;
          //   529: aload #12
          //   531: invokevirtual indexOf : (Ljava/lang/Object;)I
          //   534: ifge -> 493
          //   537: goto -> 569
          //   540: aload_0
          //   541: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   544: getfield action : Ljava/lang/StringBuffer;
          //   547: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
          //   550: ifne -> 563
          //   553: new java/lang/Exception
          //   556: dup
          //   557: ldc 'Cancelled'
          //   559: invokespecial <init> : (Ljava/lang/String;)V
          //   562: athrow
          //   563: ldc2_w 300
          //   566: invokestatic sleep : (J)V
          //   569: aload_0
          //   570: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   573: getfield ask_response : Ljava/util/Properties;
          //   576: aload #12
          //   578: ldc 'uid'
          //   580: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   583: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   586: ifne -> 540
          //   589: aload_0
          //   590: getfield val$params : Ljava/util/Properties;
          //   593: ldc 'OVERWRITE'
          //   595: ldc ''
          //   597: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   600: ldc 'RESUME'
          //   602: invokevirtual equals : (Ljava/lang/Object;)Z
          //   605: ifeq -> 623
          //   608: aload #12
          //   610: ldc 'size'
          //   612: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   615: invokestatic parseLong : (Ljava/lang/String;)J
          //   618: lstore #9
          //   620: goto -> 946
          //   623: aload_0
          //   624: getfield val$params : Ljava/util/Properties;
          //   627: ldc 'OVERWRITE'
          //   629: ldc ''
          //   631: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   634: ldc 'OVERWRITE'
          //   636: invokevirtual equals : (Ljava/lang/Object;)Z
          //   639: ifeq -> 648
          //   642: lconst_0
          //   643: lstore #9
          //   645: goto -> 946
          //   648: aload_0
          //   649: getfield val$params : Ljava/util/Properties;
          //   652: ldc 'OVERWRITE'
          //   654: ldc ''
          //   656: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   659: ldc 'SKIP'
          //   661: invokevirtual equals : (Ljava/lang/Object;)Z
          //   664: ifeq -> 673
          //   667: iconst_1
          //   668: istore #11
          //   670: goto -> 946
          //   673: aload_0
          //   674: getfield val$params : Ljava/util/Properties;
          //   677: ldc 'OVERWRITE'
          //   679: ldc ''
          //   681: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   684: ldc 'ASK'
          //   686: invokevirtual equals : (Ljava/lang/Object;)Z
          //   689: ifeq -> 946
          //   692: aload #12
          //   694: ldc 'response'
          //   696: ldc 'overwrite'
          //   698: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   701: ldc 'overwrite'
          //   703: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
          //   706: ifeq -> 715
          //   709: lconst_0
          //   710: lstore #9
          //   712: goto -> 946
          //   715: aload #12
          //   717: ldc 'response'
          //   719: ldc ''
          //   721: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   724: ldc_w 'overwrite_all'
          //   727: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
          //   730: ifeq -> 771
          //   733: aload_0
          //   734: getfield val$params : Ljava/util/Properties;
          //   737: ldc 'OVERWRITE'
          //   739: ldc 'OVERWRITE'
          //   741: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   744: pop
          //   745: lconst_0
          //   746: lstore #9
          //   748: aload_0
          //   749: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   752: getfield ask : Ljava/util/Vector;
          //   755: invokevirtual removeAllElements : ()V
          //   758: aload_0
          //   759: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   762: getfield ask_response : Ljava/util/Properties;
          //   765: invokevirtual clear : ()V
          //   768: goto -> 946
          //   771: aload #12
          //   773: ldc 'response'
          //   775: ldc ''
          //   777: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   780: ldc_w 'resume'
          //   783: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
          //   786: ifeq -> 804
          //   789: aload #12
          //   791: ldc 'size'
          //   793: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   796: invokestatic parseLong : (Ljava/lang/String;)J
          //   799: lstore #9
          //   801: goto -> 946
          //   804: aload #12
          //   806: ldc 'response'
          //   808: ldc ''
          //   810: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   813: ldc_w 'resume_all'
          //   816: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
          //   819: ifeq -> 869
          //   822: aload_0
          //   823: getfield val$params : Ljava/util/Properties;
          //   826: ldc 'OVERWRITE'
          //   828: ldc 'RESUME'
          //   830: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   833: pop
          //   834: aload #12
          //   836: ldc 'size'
          //   838: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   841: invokestatic parseLong : (Ljava/lang/String;)J
          //   844: lstore #9
          //   846: aload_0
          //   847: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   850: getfield ask : Ljava/util/Vector;
          //   853: invokevirtual removeAllElements : ()V
          //   856: aload_0
          //   857: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   860: getfield ask_response : Ljava/util/Properties;
          //   863: invokevirtual clear : ()V
          //   866: goto -> 946
          //   869: aload #12
          //   871: ldc 'response'
          //   873: ldc ''
          //   875: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   878: ldc_w 'skip'
          //   881: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
          //   884: ifeq -> 893
          //   887: iconst_1
          //   888: istore #11
          //   890: goto -> 946
          //   893: aload #12
          //   895: ldc 'response'
          //   897: ldc ''
          //   899: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   902: ldc_w 'skip_all'
          //   905: invokevirtual equalsIgnoreCase : (Ljava/lang/String;)Z
          //   908: ifeq -> 946
          //   911: aload_0
          //   912: getfield val$params : Ljava/util/Properties;
          //   915: ldc 'OVERWRITE'
          //   917: ldc 'SKIP'
          //   919: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   922: pop
          //   923: iconst_1
          //   924: istore #11
          //   926: aload_0
          //   927: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   930: getfield ask : Ljava/util/Vector;
          //   933: invokevirtual removeAllElements : ()V
          //   936: aload_0
          //   937: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   940: getfield ask_response : Ljava/util/Properties;
          //   943: invokevirtual clear : ()V
          //   946: iload #11
          //   948: ifne -> 1144
          //   951: aload_2
          //   952: invokevirtual isDirectory : ()Z
          //   955: ifeq -> 999
          //   958: aload_3
          //   959: aload #8
          //   961: invokevirtual makedirs : (Ljava/lang/String;)Z
          //   964: pop
          //   965: aload_0
          //   966: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   969: getfield statLock : Ljava/lang/Object;
          //   972: dup
          //   973: astore #12
          //   975: monitorenter
          //   976: aload_0
          //   977: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   980: dup
          //   981: getfield transferedItems : I
          //   984: iconst_1
          //   985: iadd
          //   986: putfield transferedItems : I
          //   989: aload #12
          //   991: monitorexit
          //   992: goto -> 1194
          //   995: aload #12
          //   997: monitorexit
          //   998: athrow
          //   999: lload #9
          //   1001: lconst_0
          //   1002: lcmp
          //   1003: ifle -> 1041
          //   1006: aload_0
          //   1007: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1010: getfield statLock : Ljava/lang/Object;
          //   1013: dup
          //   1014: astore #12
          //   1016: monitorenter
          //   1017: aload_0
          //   1018: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1021: dup
          //   1022: getfield transferedBytes : J
          //   1025: lload #9
          //   1027: ladd
          //   1028: putfield transferedBytes : J
          //   1031: aload #12
          //   1033: monitorexit
          //   1034: goto -> 1041
          //   1037: aload #12
          //   1039: monitorexit
          //   1040: athrow
          //   1041: aload_0
          //   1042: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1045: aload_3
          //   1046: aload_0
          //   1047: getfield val$params : Ljava/util/Properties;
          //   1050: aload_0
          //   1051: getfield val$keywords : Ljava/lang/String;
          //   1054: invokevirtual sendMeta : (Lcom/crushftp/client/GenericClient;Ljava/util/Properties;Ljava/lang/String;)V
          //   1057: aload_3
          //   1058: ldc_w 'send_compressed'
          //   1061: new java/lang/StringBuffer
          //   1064: dup
          //   1065: aload_0
          //   1066: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1069: getfield controller : Ljava/util/Properties;
          //   1072: ldc_w 'NOCOMPRESSION'
          //   1075: ldc_w 'false'
          //   1078: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   1081: ldc_w 'false'
          //   1084: invokevirtual equals : (Ljava/lang/Object;)Z
          //   1087: invokestatic valueOf : (Z)Ljava/lang/String;
          //   1090: invokespecial <init> : (Ljava/lang/String;)V
          //   1093: invokevirtual toString : ()Ljava/lang/String;
          //   1096: invokevirtual setConfig : (Ljava/lang/String;Ljava/lang/Object;)V
          //   1099: aload_3
          //   1100: aload #8
          //   1102: lload #9
          //   1104: iconst_1
          //   1105: iconst_1
          //   1106: invokevirtual upload : (Ljava/lang/String;JZZ)Ljava/io/OutputStream;
          //   1109: astore #12
          //   1111: aload_0
          //   1112: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1115: aload #8
          //   1117: lload #9
          //   1119: aload_2
          //   1120: iconst_0
          //   1121: aload #12
          //   1123: aload_3
          //   1124: aload_0
          //   1125: getfield val$itemIndex : Ljava/lang/StringBuffer;
          //   1128: aload_0
          //   1129: getfield val$initialSize : I
          //   1132: aload_1
          //   1133: invokevirtual uploadItem : (Ljava/lang/String;JLjava/io/File;ZLjava/io/OutputStream;Lcom/crushftp/client/GenericClient;Ljava/lang/StringBuffer;ILjava/util/Properties;)V
          //   1136: aload #12
          //   1138: invokevirtual close : ()V
          //   1141: goto -> 1194
          //   1144: aload_0
          //   1145: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1148: getfield statLock : Ljava/lang/Object;
          //   1151: dup
          //   1152: astore #12
          //   1154: monitorenter
          //   1155: aload_0
          //   1156: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1159: dup
          //   1160: getfield transferedItems : I
          //   1163: iconst_1
          //   1164: iadd
          //   1165: putfield transferedItems : I
          //   1168: aload_0
          //   1169: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1172: dup
          //   1173: getfield transferedBytes : J
          //   1176: aload_2
          //   1177: invokevirtual length : ()J
          //   1180: ladd
          //   1181: putfield transferedBytes : J
          //   1184: aload #12
          //   1186: monitorexit
          //   1187: goto -> 1194
          //   1190: aload #12
          //   1192: monitorexit
          //   1193: athrow
          //   1194: aload_0
          //   1195: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1198: getfield action : Ljava/lang/StringBuffer;
          //   1201: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
          //   1204: ifne -> 1210
          //   1207: goto -> 105
          //   1210: aload_0
          //   1211: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1214: getfield action : Ljava/lang/StringBuffer;
          //   1217: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
          //   1220: ifne -> 1233
          //   1223: new java/lang/Exception
          //   1226: dup
          //   1227: ldc 'Cancelled'
          //   1229: invokespecial <init> : (Ljava/lang/String;)V
          //   1232: athrow
          //   1233: aload_0
          //   1234: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1237: getfield statLock : Ljava/lang/Object;
          //   1240: dup
          //   1241: astore #4
          //   1243: monitorenter
          //   1244: aload_0
          //   1245: getfield val$itemIndex : Ljava/lang/StringBuffer;
          //   1248: invokevirtual toString : ()Ljava/lang/String;
          //   1251: invokestatic parseInt : (Ljava/lang/String;)I
          //   1254: istore #5
          //   1256: iinc #5, 1
          //   1259: aload_0
          //   1260: getfield val$itemIndex : Ljava/lang/StringBuffer;
          //   1263: iconst_0
          //   1264: invokevirtual setLength : (I)V
          //   1267: aload_0
          //   1268: getfield val$itemIndex : Ljava/lang/StringBuffer;
          //   1271: new java/lang/StringBuffer
          //   1274: dup
          //   1275: iload #5
          //   1277: invokestatic valueOf : (I)Ljava/lang/String;
          //   1280: invokespecial <init> : (Ljava/lang/String;)V
          //   1283: invokevirtual toString : ()Ljava/lang/String;
          //   1286: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1289: pop
          //   1290: aload #4
          //   1292: monitorexit
          //   1293: goto -> 1300
          //   1296: aload #4
          //   1298: monitorexit
          //   1299: athrow
          //   1300: aload_1
          //   1301: ldc_w 'itemIndex'
          //   1304: new java/lang/StringBuffer
          //   1307: dup
          //   1308: invokespecial <init> : ()V
          //   1311: aload_0
          //   1312: getfield val$itemIndex : Ljava/lang/StringBuffer;
          //   1315: invokevirtual append : (Ljava/lang/Object;)Ljava/lang/StringBuffer;
          //   1318: invokevirtual toString : ()Ljava/lang/String;
          //   1321: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1324: pop
          //   1325: goto -> 1392
          //   1328: astore #4
          //   1330: aload_2
          //   1331: ifnull -> 1343
          //   1334: aload_0
          //   1335: getfield val$files2 : Ljava/util/Vector;
          //   1338: aload_2
          //   1339: iconst_0
          //   1340: invokevirtual insertElementAt : (Ljava/lang/Object;I)V
          //   1343: aload_0
          //   1344: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1347: getfield action : Ljava/lang/StringBuffer;
          //   1350: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
          //   1353: ifne -> 1359
          //   1356: goto -> 105
          //   1359: aload_1
          //   1360: ldc_w 'error'
          //   1363: aload #4
          //   1365: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1368: pop
          //   1369: goto -> 105
          //   1372: astore #14
          //   1374: jsr -> 1380
          //   1377: aload #14
          //   1379: athrow
          //   1380: astore #13
          //   1382: aload_0
          //   1383: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1386: aload_3
          //   1387: invokestatic access$1 : (Lcom/crushftp/tunnel/Uploader2;Lcom/crushftp/client/GenericClient;)V
          //   1390: ret #13
          //   1392: jsr -> 1380
          //   1395: aload_0
          //   1396: getfield val$files2 : Ljava/util/Vector;
          //   1399: invokevirtual size : ()I
          //   1402: ifgt -> 19
          //   1405: aload_0
          //   1406: getfield this$0 : Lcom/crushftp/tunnel/Uploader2;
          //   1409: invokevirtual refreshStatusInfo : ()V
          //   1412: aload_1
          //   1413: ldc_w 'status'
          //   1416: ldc_w 'DONE'
          //   1419: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1422: pop
          //   1423: return
          // Line number table:
          //   Java source line number -> byte code offset
          //   #489	-> 0
          //   #490	-> 14
          //   #491	-> 16
          //   #493	-> 19
          //   #494	-> 26
          //   #497	-> 38
          //   #498	-> 61
          //   #500	-> 69
          //   #498	-> 91
          //   #502	-> 101
          //   #503	-> 111
          //   #505	-> 118
          //   #507	-> 152
          //   #508	-> 155
          //   #509	-> 169
          //   #511	-> 174
          //   #512	-> 188
          //   #514	-> 202
          //   #515	-> 218
          //   #516	-> 243
          //   #517	-> 261
          //   #518	-> 289
          //   #519	-> 311
          //   #520	-> 329
          //   #521	-> 332
          //   #522	-> 335
          //   #524	-> 361
          //   #525	-> 369
          //   #527	-> 374
          //   #528	-> 408
          //   #529	-> 433
          //   #531	-> 452
          //   #532	-> 468
          //   #533	-> 478
          //   #534	-> 490
          //   #536	-> 493
          //   #537	-> 516
          //   #534	-> 522
          //   #539	-> 537
          //   #541	-> 540
          //   #542	-> 563
          //   #539	-> 569
          //   #545	-> 589
          //   #546	-> 623
          //   #547	-> 648
          //   #548	-> 673
          //   #550	-> 692
          //   #551	-> 715
          //   #553	-> 733
          //   #554	-> 745
          //   #555	-> 748
          //   #556	-> 758
          //   #558	-> 771
          //   #559	-> 804
          //   #561	-> 822
          //   #562	-> 834
          //   #563	-> 846
          //   #564	-> 856
          //   #566	-> 869
          //   #567	-> 893
          //   #569	-> 911
          //   #570	-> 923
          //   #571	-> 926
          //   #572	-> 936
          //   #578	-> 946
          //   #580	-> 951
          //   #582	-> 958
          //   #583	-> 965
          //   #585	-> 976
          //   #583	-> 989
          //   #590	-> 999
          //   #592	-> 1006
          //   #594	-> 1017
          //   #592	-> 1031
          //   #597	-> 1041
          //   #598	-> 1057
          //   #599	-> 1099
          //   #600	-> 1111
          //   #601	-> 1136
          //   #606	-> 1144
          //   #608	-> 1155
          //   #609	-> 1168
          //   #606	-> 1184
          //   #612	-> 1194
          //   #614	-> 1210
          //   #615	-> 1233
          //   #617	-> 1244
          //   #618	-> 1256
          //   #619	-> 1259
          //   #620	-> 1267
          //   #615	-> 1290
          //   #622	-> 1300
          //   #624	-> 1328
          //   #626	-> 1330
          //   #627	-> 1343
          //   #628	-> 1359
          //   #629	-> 1369
          //   #632	-> 1372
          //   #634	-> 1377
          //   #632	-> 1380
          //   #633	-> 1382
          //   #634	-> 1390
          //   #491	-> 1395
          //   #636	-> 1405
          //   #637	-> 1412
          //   #638	-> 1423
          // Local variable table:
          //   start	length	slot	name	descriptor
          //   0	1424	0	this	Lcom/crushftp/tunnel/Uploader2$2;
          //   14	1410	1	status	Ljava/util/Properties;
          //   16	1408	2	item	Ljava/io/File;
          //   38	1357	3	c	Lcom/crushftp/client/GenericClient;
          //   155	1055	4	offset	I
          //   169	1041	5	index	I
          //   188	14	6	parent	Ljava/io/File;
          //   218	992	6	uploadPath	Ljava/lang/String;
          //   261	949	7	new_part	Ljava/lang/String;
          //   311	899	8	itemName	Ljava/lang/String;
          //   332	878	9	startPos	J
          //   335	875	11	skip	Z
          //   369	577	12	stat	Ljava/util/Properties;
          //   1111	30	12	out	Ljava/io/OutputStream;
          //   1256	34	5	i	I
          //   1330	42	4	e	Ljava/lang/Exception;
          // Exception table:
          //   from	to	target	type
          //   38	108	1328	java/lang/Exception
          //   38	108	1372	finally
          //   69	94	97	finally
          //   97	100	97	finally
          //   111	1325	1328	java/lang/Exception
          //   111	1372	1372	finally
          //   976	992	995	finally
          //   995	998	995	finally
          //   1017	1034	1037	finally
          //   1037	1040	1037	finally
          //   1155	1187	1190	finally
          //   1190	1193	1190	finally
          //   1244	1293	1296	finally
          //   1296	1299	1296	finally
          //   1392	1395	1372	finally
        }
      };
    int maxThreads = Integer.parseInt(this.controller.getProperty("UPLOAD_THREADS", "1"));
    maxThreads = 1;
    for (int x = 0; x < maxThreads; x++) {
      Thread t = new Thread(r);
      Properties status = new Properties();
      status.put("status", "RUNNING");
      status.put("index", (new StringBuffer(String.valueOf(x))).toString());
      threads.put(t, status);
      t.start();
    } 
    Exception lastError = null;
    while (threads.size() > 0) {
      Enumeration keys = threads.keys();
      while (keys.hasMoreElements()) {
        Object key = keys.nextElement();
        Properties p = (Properties)threads.get(key);
        if (p.getProperty("status", "").equals("DONE")) {
          threads.remove(key);
          if (p.containsKey("error"))
            lastError = (Exception)p.get("error"); 
        } 
      } 
      Thread.sleep(100L);
    } 
    if (lastError != null)
      throw lastError; 
  }
  
  public void uploadItem(String itemName, long startPos, File item, boolean zip, OutputStream out, GenericClient c, StringBuffer itemIndex, int initialSize, Properties status) throws Exception {
    FileArchiveEntry zipEntry = new FileArchiveEntry(itemName);
    if (startPos > 0L) {
      this.statusInfo.put("uploadStatus", String.valueOf(status.getProperty("index")) + ":Upload:Resuming item:" + itemName + " at position:" + startPos + " (" + itemIndex + " of " + initialSize + ")");
      Tunnel2.msg(getStatus());
      zipEntry = new FileArchiveEntry(String.valueOf(itemName) + ":REST=" + startPos);
    } 
    zipEntry.setTime(item.lastModified());
    if (zip) {
      this.statusInfo.put("uploadStatus", String.valueOf(status.getProperty("index")) + ":Upload:Adding zip=" + zip + " entry:" + itemName);
    } else {
      this.statusInfo.put("uploadStatus", String.valueOf(status.getProperty("index")) + ":Upload:" + itemName + " (" + itemIndex + " of " + initialSize + ")");
    } 
    if (zip)
      ((FileArchiveOutputStream)out).putArchiveEntry(zipEntry); 
    Tunnel2.msg(getStatus());
    RandomAccessFile in = new RandomAccessFile(item.getCanonicalPath(), "r");
    long bytesThisFile = startPos;
    try {
      if (startPos > 0L)
        in.seek(startPos); 
      byte[] b = new byte[65536];
      int bytesRead = 0;
      long pos = 0L;
      long start = System.currentTimeMillis();
      while (bytesRead >= 0) {
        bytesRead = in.read(b);
        if (bytesRead > 0) {
          out.write(b, 0, bytesRead);
          synchronized (this.statLock) {
            this.transferedBytes += bytesRead;
          } 
          bytesThisFile += bytesRead;
          pos += bytesRead;
          if (System.currentTimeMillis() - start > 1000L) {
            start = System.currentTimeMillis();
            this.statusInfo.put("uploadStatus", String.valueOf(status.getProperty("index")) + ":Upload:" + itemName + "... " + Common.format_bytes_short(bytesThisFile) + "/" + Common.format_bytes_short(item.length()) + " (" + itemIndex + " of " + initialSize + ")");
          } 
        } 
        refreshStatusInfo();
        if (!Tunnel2.checkAction(this.action)) {
          System.out.println(c.doCommand("SITE BLOCK_UPLOADS"));
          Thread.sleep(1000L);
          break;
        } 
      } 
    } catch (Exception e) {
      synchronized (this.statLock) {
        this.transferedBytes -= bytesThisFile;
      } 
    } finally {
      in.close();
    } 
    if (zip)
      ((FileArchiveOutputStream)out).closeArchiveEntry(); 
    synchronized (this.statLock) {
      this.transferedItems++;
    } 
    Tunnel2.msg("Bytes written:" + itemName);
  }
  
  public int indexOfParent(File item, Vector files) throws IOException {
    int i = -1;
    int offset = 0;
    for (int loop = 0; loop < files.size(); loop++) {
      File f = files.elementAt(loop);
      if (getCanonicalPath(item).startsWith(getCanonicalPath(f))) {
        int newOffset = getCanonicalPath(f).length();
        if (newOffset > offset) {
          offset = newOffset;
          i = loop;
        } 
      } 
    } 
    return i;
  }
  
  public void getAllFileListing(Vector list, String path, int depth) throws Exception {
    if (!Tunnel2.checkAction(this.action))
      return; 
    File item = new File(path);
    buildCrossRef(item);
    if (item.isFile()) {
      list.addElement(item);
    } else {
      appendListing(path, list, "", depth);
    } 
  }
  
  public void buildCrossRef(File f) throws IOException {
    if (Common.machine_is_x()) {
      if (this.crossRef.containsKey(f.getCanonicalPath()))
        return; 
      boolean hasBadPathChar = false;
      StringBuffer sb = new StringBuffer();
      File f2 = f;
      while (f != null) {
        String name = f.getName();
        sb.insert(0, "/" + name.replace('/', '.').replace(':', '.'));
        if (name.indexOf("/") >= 0 || name.indexOf(":") >= 0)
          hasBadPathChar = true; 
        f = f.getParentFile();
        if (f.getPath().equals("/"))
          break; 
      } 
      if (hasBadPathChar)
        this.crossRef.put(f2.getCanonicalPath(), sb.toString()); 
    } 
  }
  
  public String getCanonicalPath(File f) throws IOException {
    return this.crossRef.getProperty(f.getCanonicalPath(), f.getCanonicalPath());
  }
  
  public void appendListing(String path, Vector list, String dir, int depth) throws Exception {
    if (!Tunnel2.checkAction(this.action))
      return; 
    if (depth == 0)
      return; 
    depth--;
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    String[] items = (new File(String.valueOf(path) + dir)).list();
    list.addElement(new File(String.valueOf(path) + dir));
    if (items == null)
      return; 
    for (int x = 0; x < items.length; x++) {
      if (!Tunnel2.checkAction(this.action))
        return; 
      File item = new File(String.valueOf(path) + dir + items[x]);
      buildCrossRef(item);
      if (item.isFile()) {
        list.addElement(item);
      } else {
        appendListing(path, list, String.valueOf(dir) + items[x] + "/", depth);
      } 
    } 
  }
}
