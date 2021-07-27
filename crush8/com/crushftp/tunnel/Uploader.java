package com.crushftp.tunnel;

import com.crushftp.client.Common;
import com.crushftp.tunnel2.Tunnel2;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipException;
import org.apache.commons.compress.archivers.zip.Zip64Mode;

public class Uploader {
  public Properties controller = new Properties();
  
  long totalBytes = 1L;
  
  long transferedBytes = 0L;
  
  int totalItems = 0;
  
  int transferedItems = 0;
  
  String status2 = "";
  
  StringBuffer action = new StringBuffer();
  
  boolean standAlone = false;
  
  public Properties statusInfo = null;
  
  StringBuffer CrushAuth = null;
  
  SimpleDateFormat sdf_rfc1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
  
  public Object statLock = new Object();
  
  Properties crossRef = new Properties();
  
  public Uploader(Properties statusInfo, StringBuffer CrushAuth) {
    this.statusInfo = statusInfo;
    this.CrushAuth = CrushAuth;
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
          final Uploader this$0;
          
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
    if (!login(this.controller, this.standAlone, this.CrushAuth))
      return; 
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
    Object t = null;
    try {
      Tunnel2.msg("Files:" + parentfiles1.size());
      int x;
      for (x = 0; x < parentfiles1.size(); ) {
        if (Tunnel2.checkAction(this.action)) {
          File f = parentfiles1.elementAt(x);
          this.statusInfo.put("uploadStatus", "Finding files " + f.getPath() + "... " + Common.percent(x, parentfiles1.size()));
          getAllFileListing(files2, f.getCanonicalPath(), 999);
          if (this.controller.getProperty("RESUME", "false").equalsIgnoreCase("true") || this.controller.getProperty("resume", "false").equalsIgnoreCase("true"))
            getServerFileSizes(f.getName()); 
          if (parentfiles.indexOf(f.getParentFile()) < 0)
            parentfiles.addElement(f.getParentFile()); 
          x++;
          continue;
        } 
        return;
      } 
      this.controller.put("resume", "true");
      this.statusInfo.put("uploadStatus", "Getting file sizes...");
      Tunnel2.msg("Files2:" + files2.size());
      for (x = 0; x < files2.size(); ) {
        if (Tunnel2.checkAction(this.action)) {
          this.statusInfo.put("uploadStatus", "Getting file sizes... " + Common.percent(x, files2.size()));
          File f = files2.elementAt(x);
          synchronized (this.statLock) {
            this.totalBytes += f.length();
            this.totalItems++;
          } 
          x++;
          continue;
        } 
        return;
      } 
      try {
        if (this.totalBytes > 1048576L && this.controller.getProperty("ALLOWTUNNEL", "true").equals("true"))
          t = AutoChannelProxy.enableAppletTunnel(this.controller, false, this.CrushAuth); 
      } catch (Exception e) {
        Tunnel2.msg("Error checking for tunnel.");
        Tunnel2.msg(e);
      } 
      doUpload(parentfiles, files2, this.controller, "");
    } catch (Exception e) {
      Tunnel2.msg(e);
      if (this.standAlone)
        System.exit(1); 
    } finally {
      this.controller.put("resume", "false");
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
  }
  
  public void getServerFileSizes(String partialPath) {
    if (!Tunnel2.checkAction(this.action))
      return; 
    try {
      Properties serverFiles = (Properties)this.controller.get("serverFiles");
      URL u = new URL(String.valueOf(this.controller.getProperty("URL_REAL", this.controller.getProperty("URL"))) + this.controller.getProperty("UPLOADPATH", "") + partialPath + "/:filetree");
      Tunnel2.msg("Getting folder contents information " + u.toExternalForm());
      HttpURLConnection urlc = (HttpURLConnection)u.openConnection();
      urlc.setReadTimeout(70000);
      urlc.setRequestMethod("GET");
      urlc.setRequestProperty("Cookie", "CrushAuth=" + this.CrushAuth.toString() + ";");
      urlc.setUseCaches(false);
      urlc.setDoInput(true);
      BufferedReader br = null;
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
      try {
        br = new BufferedReader(new InputStreamReader(urlc.getInputStream(), "UTF8"));
        String data = "";
        long tempSize = 0L;
        long tempfileSize = 0L;
        long modified = 0L;
        while ((data = br.readLine()) != null) {
          StringTokenizer st = new StringTokenizer(data);
          st.nextToken();
          st.nextToken();
          st.nextToken();
          st.nextToken();
          tempfileSize = Long.parseLong(st.nextToken());
          tempSize += tempfileSize;
          String mdate = st.nextToken();
          modified = sdf.parse(mdate).getTime();
          st.nextToken();
          st.nextToken();
          String rootdir = st.nextToken();
          while (st.hasMoreTokens())
            rootdir = String.valueOf(rootdir) + " " + st.nextToken(); 
          Properties p = new Properties();
          p.put("size", (new StringBuffer(String.valueOf(tempfileSize))).toString());
          p.put("modified", (new StringBuffer(String.valueOf(modified))).toString());
          serverFiles.put(rootdir, p);
          if (!Tunnel2.checkAction(this.action))
            break; 
        } 
      } finally {
        br.close();
      } 
      urlc.getResponseCode();
      urlc.disconnect();
    } catch (Exception e) {
      Tunnel2.msg(String.valueOf(partialPath) + " not found:" + e.toString());
    } 
  }
  
  public static boolean login(Properties controller, boolean standAlone, StringBuffer CrushAuth2) {
    try {
      System.getProperties().put("sun.net.http.retryPost", "false");
      String clientid = "";
      if (controller.containsKey("CLIENTID"))
        clientid = "&clientid=" + controller.getProperty("CLIENTID"); 
      if (CrushAuth2.toString().equals("")) {
        URL u = new URL(controller.getProperty("URL_REAL", controller.getProperty("URL")));
        HttpURLConnection urlc = (HttpURLConnection)u.openConnection();
        urlc.setRequestMethod("POST");
        urlc.setUseCaches(false);
        urlc.setDoOutput(true);
        urlc.getOutputStream().write(("command=login&username=" + controller.getProperty("USERNAME") + "&password=" + controller.getProperty("PASSWORD") + clientid).getBytes("UTF8"));
        int code = urlc.getResponseCode();
        String result = Tunnel2.consumeResponse(urlc);
        String cookie = urlc.getHeaderField("Set-Cookie");
        Tunnel2.msg("Got login result:" + code + " and result:" + result + " and cookie:" + cookie);
        CrushAuth2.setLength(0);
        CrushAuth2.append(cookie.substring(cookie.indexOf("CrushAuth=") + "CrushAuth=".length(), cookie.indexOf(";", cookie.indexOf("CrushAuth="))));
        urlc.disconnect();
        if (result.indexOf("<response>failure</response>") >= 0)
          return false; 
      } 
      return true;
    } catch (SocketException e) {
      Tunnel2.msg(e);
      try {
        if (e.toUpperCase().indexOf("SOCKET CLOSED") >= 0) {
          Tunnel2.msg("Forced wait to overcome java bug on HTTP Client.");
          Thread.sleep(31000L);
        } 
      } catch (Exception exception) {}
    } catch (Exception e) {
      Tunnel2.msg(e);
      if (standAlone)
        System.exit(1); 
    } 
    return false;
  }
  
  public void findUploadInfo(Vector v, Vector parentfiles) {
    Properties serverFiles = (Properties)this.controller.get("serverFiles");
    Vector toDelete = new Vector();
    int x;
    for (x = 0; x < v.size(); x++) {
      if (!Tunnel2.checkAction(this.action))
        return; 
      File f = v.elementAt(x);
      this.statusInfo.put("uploadStatus", "Upload:Finding file " + f.getPath() + "...");
      Tunnel2.msg(getStatus());
      String partialPath = "";
      int index = -1;
      try {
        index = indexOfParent(f, parentfiles);
      } catch (IOException iOException) {}
      if (index >= 0) {
        File parent = parentfiles.elementAt(index);
        try {
          partialPath = getCanonicalPath(f).substring(getCanonicalPath(parent).length()).replace('\\', '/').substring(1);
        } catch (IOException iOException) {}
      } else {
        partialPath = f.getName();
      } 
      try {
        Properties serverItem = (Properties)serverFiles.get(String.valueOf((new URL(this.controller.getProperty("URL_REAL", this.controller.getProperty("URL")))).getPath()) + partialPath);
        if (serverItem == null)
          getServerFileSizes(partialPath); 
        serverItem = (Properties)serverFiles.get(String.valueOf((new URL(this.controller.getProperty("URL_REAL", this.controller.getProperty("URL")))).getPath()) + partialPath);
        if (serverItem != null)
          if (Long.parseLong(serverItem.getProperty("size", "-1")) == f.length() && Math.abs(Long.parseLong(serverItem.getProperty("modified", "-1")) - f.lastModified()) < 3000L)
            toDelete.addElement(f);  
      } catch (IOException iOException) {}
    } 
    for (x = 0; x < toDelete.size(); x++)
      v.remove(toDelete.elementAt(x)); 
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
    //   70: goto -> 177
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
    //   106: iconst_0
    //   107: istore #10
    //   109: iconst_0
    //   110: istore #11
    //   112: aload_2
    //   113: iconst_0
    //   114: invokevirtual remove : (I)Ljava/lang/Object;
    //   117: checkcast java/io/File
    //   120: astore #12
    //   122: iload #8
    //   124: ldc_w 50000
    //   127: if_icmplt -> 140
    //   130: aload #7
    //   132: aload #12
    //   134: invokevirtual indexOf : (Ljava/lang/Object;)I
    //   137: ifge -> 143
    //   140: iconst_1
    //   141: istore #11
    //   143: iload #10
    //   145: ifeq -> 158
    //   148: aload #6
    //   150: aload #12
    //   152: invokevirtual addElement : (Ljava/lang/Object;)V
    //   155: goto -> 170
    //   158: iload #11
    //   160: ifeq -> 170
    //   163: aload #7
    //   165: aload #12
    //   167: invokevirtual addElement : (Ljava/lang/Object;)V
    //   170: iinc #9, 1
    //   173: aload_0
    //   174: invokevirtual refreshStatusInfo : ()V
    //   177: aload_2
    //   178: invokevirtual size : ()I
    //   181: ifgt -> 73
    //   184: aload #7
    //   186: invokevirtual size : ()I
    //   189: aload #6
    //   191: invokevirtual size : ()I
    //   194: iadd
    //   195: istore #10
    //   197: goto -> 1048
    //   200: aload_0
    //   201: invokevirtual refreshStatusInfo : ()V
    //   204: aload_0
    //   205: getfield action : Ljava/lang/StringBuffer;
    //   208: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
    //   211: ifne -> 215
    //   214: return
    //   215: iload #5
    //   217: iconst_1
    //   218: if_icmple -> 376
    //   221: aload_0
    //   222: getfield statusInfo : Ljava/util/Properties;
    //   225: ldc 'uploadStatus'
    //   227: ldc_w 'Upload:Recovering from an error, re-checking what the server received so we can resume.'
    //   230: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   233: pop
    //   234: aload_0
    //   235: invokevirtual getStatus : ()Ljava/lang/String;
    //   238: invokestatic msg : (Ljava/lang/String;)V
    //   241: iconst_0
    //   242: istore #11
    //   244: goto -> 325
    //   247: aload_0
    //   248: getfield action : Ljava/lang/StringBuffer;
    //   251: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
    //   254: ifne -> 258
    //   257: return
    //   258: aload_1
    //   259: iload #11
    //   261: invokevirtual elementAt : (I)Ljava/lang/Object;
    //   264: checkcast java/io/File
    //   267: astore #12
    //   269: aload_0
    //   270: getfield statusInfo : Ljava/util/Properties;
    //   273: ldc 'uploadStatus'
    //   275: new java/lang/StringBuffer
    //   278: dup
    //   279: ldc_w 'Upload:Finding file '
    //   282: invokespecial <init> : (Ljava/lang/String;)V
    //   285: aload #12
    //   287: invokevirtual getPath : ()Ljava/lang/String;
    //   290: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   293: ldc_w '...'
    //   296: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   299: invokevirtual toString : ()Ljava/lang/String;
    //   302: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   305: pop
    //   306: aload_0
    //   307: invokevirtual getStatus : ()Ljava/lang/String;
    //   310: invokestatic msg : (Ljava/lang/String;)V
    //   313: aload_0
    //   314: aload #12
    //   316: invokevirtual getName : ()Ljava/lang/String;
    //   319: invokevirtual getServerFileSizes : (Ljava/lang/String;)V
    //   322: iinc #11, 1
    //   325: iload #11
    //   327: aload_1
    //   328: invokevirtual size : ()I
    //   331: if_icmplt -> 247
    //   334: aload_0
    //   335: aload #7
    //   337: aload_1
    //   338: invokevirtual findUploadInfo : (Ljava/util/Vector;Ljava/util/Vector;)V
    //   341: aload_0
    //   342: aload #6
    //   344: aload_1
    //   345: invokevirtual findUploadInfo : (Ljava/util/Vector;Ljava/util/Vector;)V
    //   348: aload_0
    //   349: getfield statLock : Ljava/lang/Object;
    //   352: dup
    //   353: astore #11
    //   355: monitorenter
    //   356: aload_0
    //   357: lconst_0
    //   358: putfield transferedBytes : J
    //   361: aload_0
    //   362: iconst_0
    //   363: putfield transferedItems : I
    //   366: aload #11
    //   368: monitorexit
    //   369: goto -> 376
    //   372: aload #11
    //   374: monitorexit
    //   375: athrow
    //   376: aload_0
    //   377: getfield statusInfo : Ljava/util/Properties;
    //   380: ldc 'uploadStatus'
    //   382: ldc_w 'Uploading files...'
    //   385: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   388: pop
    //   389: aload_0
    //   390: invokevirtual getStatus : ()Ljava/lang/String;
    //   393: invokestatic msg : (Ljava/lang/String;)V
    //   396: aconst_null
    //   397: astore #11
    //   399: new java/lang/StringBuffer
    //   402: dup
    //   403: ldc_w 'Connecting to URL:'
    //   406: invokespecial <init> : (Ljava/lang/String;)V
    //   409: aload_0
    //   410: getfield controller : Ljava/util/Properties;
    //   413: ldc 'URL'
    //   415: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   418: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   421: invokevirtual toString : ()Ljava/lang/String;
    //   424: invokestatic msg : (Ljava/lang/String;)V
    //   427: aload_0
    //   428: getfield statusInfo : Ljava/util/Properties;
    //   431: ldc 'uploadStatus'
    //   433: new java/lang/StringBuffer
    //   436: dup
    //   437: ldc_w 'Upload:Connecting to URL:'
    //   440: invokespecial <init> : (Ljava/lang/String;)V
    //   443: aload_0
    //   444: getfield controller : Ljava/util/Properties;
    //   447: ldc 'URL'
    //   449: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   452: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   455: invokevirtual toString : ()Ljava/lang/String;
    //   458: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   461: pop
    //   462: new java/net/URL
    //   465: dup
    //   466: aload_0
    //   467: getfield controller : Ljava/util/Properties;
    //   470: ldc 'URL'
    //   472: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   475: invokespecial <init> : (Ljava/lang/String;)V
    //   478: astore #12
    //   480: aload_0
    //   481: aload #12
    //   483: aload_3
    //   484: aload #4
    //   486: aload #7
    //   488: aload_1
    //   489: iload #10
    //   491: invokevirtual uploadZippedFiles : (Ljava/net/URL;Ljava/util/Properties;Ljava/lang/String;Ljava/util/Vector;Ljava/util/Vector;I)V
    //   494: aload #7
    //   496: invokevirtual removeAllElements : ()V
    //   499: aload_0
    //   500: aload #12
    //   502: aload_3
    //   503: aload #4
    //   505: aload #6
    //   507: aload_1
    //   508: iload #10
    //   510: invokevirtual uploadNormalFiles : (Ljava/net/URL;Ljava/util/Properties;Ljava/lang/String;Ljava/util/Vector;Ljava/util/Vector;I)V
    //   513: aload_0
    //   514: getfield statusInfo : Ljava/util/Properties;
    //   517: ldc 'uploadStatus'
    //   519: ldc ''
    //   521: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   524: pop
    //   525: aload_0
    //   526: invokevirtual getStatus : ()Ljava/lang/String;
    //   529: invokestatic msg : (Ljava/lang/String;)V
    //   532: aload_0
    //   533: getfield statLock : Ljava/lang/Object;
    //   536: dup
    //   537: astore #13
    //   539: monitorenter
    //   540: aload_0
    //   541: aload_0
    //   542: getfield totalBytes : J
    //   545: putfield transferedBytes : J
    //   548: aload #13
    //   550: monitorexit
    //   551: goto -> 558
    //   554: aload #13
    //   556: monitorexit
    //   557: athrow
    //   558: jsr -> 1033
    //   561: goto -> 1058
    //   564: astore #12
    //   566: aload #12
    //   568: invokestatic msg : (Ljava/lang/Exception;)V
    //   571: aload_0
    //   572: getfield action : Ljava/lang/StringBuffer;
    //   575: invokestatic checkAction : (Ljava/lang/StringBuffer;)Z
    //   578: ifne -> 585
    //   581: jsr -> 1033
    //   584: return
    //   585: aload #12
    //   587: invokevirtual getMessage : ()Ljava/lang/String;
    //   590: ldc_w 'ERROR:'
    //   593: invokevirtual startsWith : (Ljava/lang/String;)Z
    //   596: ifeq -> 640
    //   599: aload_0
    //   600: getfield statusInfo : Ljava/util/Properties;
    //   603: ldc 'uploadStatus'
    //   605: new java/lang/StringBuffer
    //   608: dup
    //   609: ldc_w 'Upload:'
    //   612: invokespecial <init> : (Ljava/lang/String;)V
    //   615: aload #12
    //   617: invokevirtual getMessage : ()Ljava/lang/String;
    //   620: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   623: invokevirtual toString : ()Ljava/lang/String;
    //   626: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   629: pop
    //   630: aload_0
    //   631: invokevirtual getStatus : ()Ljava/lang/String;
    //   634: invokestatic msg : (Ljava/lang/String;)V
    //   637: goto -> 558
    //   640: aload #12
    //   642: invokevirtual getMessage : ()Ljava/lang/String;
    //   645: invokevirtual toUpperCase : ()Ljava/lang/String;
    //   648: ldc_w 'ACCESS IS DENIED'
    //   651: invokevirtual indexOf : (Ljava/lang/String;)I
    //   654: iflt -> 698
    //   657: aload_0
    //   658: getfield statusInfo : Ljava/util/Properties;
    //   661: ldc 'uploadStatus'
    //   663: new java/lang/StringBuffer
    //   666: dup
    //   667: ldc_w 'Upload:ERROR:'
    //   670: invokespecial <init> : (Ljava/lang/String;)V
    //   673: aload #12
    //   675: invokevirtual getMessage : ()Ljava/lang/String;
    //   678: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   681: invokevirtual toString : ()Ljava/lang/String;
    //   684: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   687: pop
    //   688: aload_0
    //   689: invokevirtual getStatus : ()Ljava/lang/String;
    //   692: invokestatic msg : (Ljava/lang/String;)V
    //   695: goto -> 558
    //   698: aload_0
    //   699: getfield statusInfo : Ljava/util/Properties;
    //   702: ldc 'uploadStatus'
    //   704: new java/lang/StringBuffer
    //   707: dup
    //   708: ldc_w 'Upload:WARN:'
    //   711: invokespecial <init> : (Ljava/lang/String;)V
    //   714: aload #12
    //   716: invokevirtual getMessage : ()Ljava/lang/String;
    //   719: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   722: invokevirtual toString : ()Ljava/lang/String;
    //   725: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   728: pop
    //   729: aload_0
    //   730: invokevirtual getStatus : ()Ljava/lang/String;
    //   733: invokestatic msg : (Ljava/lang/String;)V
    //   736: aload #12
    //   738: invokevirtual getMessage : ()Ljava/lang/String;
    //   741: ldc_w ' 403 '
    //   744: invokevirtual indexOf : (Ljava/lang/String;)I
    //   747: ifle -> 763
    //   750: aload_0
    //   751: getfield statusInfo : Ljava/util/Properties;
    //   754: ldc 'uploadStatus'
    //   756: ldc_w 'ERROR:Uploads are not allowed.'
    //   759: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   762: pop
    //   763: new java/net/URL
    //   766: dup
    //   767: new java/lang/StringBuffer
    //   770: dup
    //   771: aload_0
    //   772: getfield controller : Ljava/util/Properties;
    //   775: ldc_w 'URL_REAL'
    //   778: aload_0
    //   779: getfield controller : Ljava/util/Properties;
    //   782: ldc 'URL'
    //   784: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
    //   787: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   790: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
    //   793: invokespecial <init> : (Ljava/lang/String;)V
    //   796: ldc_w '?c2f='
    //   799: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   802: aload_0
    //   803: getfield CrushAuth : Ljava/lang/StringBuffer;
    //   806: invokevirtual toString : ()Ljava/lang/String;
    //   809: aload_0
    //   810: getfield CrushAuth : Ljava/lang/StringBuffer;
    //   813: invokevirtual toString : ()Ljava/lang/String;
    //   816: invokevirtual length : ()I
    //   819: iconst_4
    //   820: isub
    //   821: invokevirtual substring : (I)Ljava/lang/String;
    //   824: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   827: ldc_w 'command=getLastUploadError'
    //   830: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   833: invokevirtual toString : ()Ljava/lang/String;
    //   836: invokespecial <init> : (Ljava/lang/String;)V
    //   839: astore #13
    //   841: aload #13
    //   843: invokevirtual openConnection : ()Ljava/net/URLConnection;
    //   846: checkcast java/net/HttpURLConnection
    //   849: astore #11
    //   851: aload #11
    //   853: ldc_w 'GET'
    //   856: invokevirtual setRequestMethod : (Ljava/lang/String;)V
    //   859: aload #11
    //   861: ldc_w 'Cookie'
    //   864: new java/lang/StringBuffer
    //   867: dup
    //   868: ldc_w 'CrushAuth='
    //   871: invokespecial <init> : (Ljava/lang/String;)V
    //   874: aload_0
    //   875: getfield CrushAuth : Ljava/lang/StringBuffer;
    //   878: invokevirtual toString : ()Ljava/lang/String;
    //   881: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   884: ldc_w ';'
    //   887: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   890: invokevirtual toString : ()Ljava/lang/String;
    //   893: invokevirtual setRequestProperty : (Ljava/lang/String;Ljava/lang/String;)V
    //   896: aload #11
    //   898: iconst_0
    //   899: invokevirtual setUseCaches : (Z)V
    //   902: aload #11
    //   904: invokestatic consumeResponse : (Ljava/net/HttpURLConnection;)Ljava/lang/String;
    //   907: astore #14
    //   909: aload #11
    //   911: invokevirtual disconnect : ()V
    //   914: aload #14
    //   916: ldc ''
    //   918: invokevirtual equals : (Ljava/lang/Object;)Z
    //   921: ifne -> 952
    //   924: aload_0
    //   925: getfield statusInfo : Ljava/util/Properties;
    //   928: ldc 'uploadStatus'
    //   930: new java/lang/StringBuffer
    //   933: dup
    //   934: ldc_w 'Upload:'
    //   937: invokespecial <init> : (Ljava/lang/String;)V
    //   940: aload #14
    //   942: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
    //   945: invokevirtual toString : ()Ljava/lang/String;
    //   948: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    //   951: pop
    //   952: aload #14
    //   954: ldc_w 'ERROR:'
    //   957: invokevirtual indexOf : (Ljava/lang/String;)I
    //   960: iflt -> 968
    //   963: goto -> 558
    //   966: astore #13
    //   968: aload #12
    //   970: instanceof java/io/FileNotFoundException
    //   973: ifne -> 983
    //   976: iload #5
    //   978: bipush #10
    //   980: if_icmple -> 1007
    //   983: aload_0
    //   984: getfield CrushAuth : Ljava/lang/StringBuffer;
    //   987: iconst_0
    //   988: invokevirtual setLength : (I)V
    //   991: aload_0
    //   992: getfield controller : Ljava/util/Properties;
    //   995: aload_0
    //   996: getfield standAlone : Z
    //   999: aload_0
    //   1000: getfield CrushAuth : Ljava/lang/StringBuffer;
    //   1003: invokestatic login : (Ljava/util/Properties;ZLjava/lang/StringBuffer;)Z
    //   1006: pop
    //   1007: iload #5
    //   1009: sipush #1000
    //   1012: imul
    //   1013: i2l
    //   1014: invokestatic sleep : (J)V
    //   1017: goto -> 1045
    //   1020: astore #13
    //   1022: goto -> 1045
    //   1025: astore #16
    //   1027: jsr -> 1033
    //   1030: aload #16
    //   1032: athrow
    //   1033: astore #15
    //   1035: aload_0
    //   1036: getfield action : Ljava/lang/StringBuffer;
    //   1039: iconst_0
    //   1040: invokevirtual setLength : (I)V
    //   1043: ret #15
    //   1045: jsr -> 1033
    //   1048: iload #5
    //   1050: iinc #5, 1
    //   1053: bipush #60
    //   1055: if_icmplt -> 200
    //   1058: aload_0
    //   1059: invokevirtual refreshStatusInfo : ()V
    //   1062: iload #5
    //   1064: bipush #59
    //   1066: if_icmplt -> 1080
    //   1069: aload_0
    //   1070: getfield standAlone : Z
    //   1073: ifeq -> 1080
    //   1076: iconst_1
    //   1077: invokestatic exit : (I)V
    //   1080: return
    // Line number table:
    //   Java source line number -> byte code offset
    //   #514	-> 0
    //   #515	-> 27
    //   #516	-> 30
    //   #517	-> 39
    //   #518	-> 48
    //   #519	-> 61
    //   #520	-> 67
    //   #521	-> 70
    //   #523	-> 73
    //   #524	-> 106
    //   #525	-> 109
    //   #526	-> 112
    //   #527	-> 122
    //   #528	-> 143
    //   #529	-> 158
    //   #530	-> 170
    //   #531	-> 173
    //   #521	-> 177
    //   #534	-> 184
    //   #535	-> 197
    //   #537	-> 200
    //   #538	-> 204
    //   #539	-> 215
    //   #541	-> 221
    //   #542	-> 234
    //   #543	-> 241
    //   #545	-> 247
    //   #546	-> 258
    //   #547	-> 269
    //   #548	-> 306
    //   #549	-> 313
    //   #543	-> 322
    //   #551	-> 334
    //   #552	-> 341
    //   #553	-> 348
    //   #555	-> 356
    //   #556	-> 361
    //   #553	-> 366
    //   #559	-> 376
    //   #560	-> 389
    //   #561	-> 396
    //   #564	-> 399
    //   #565	-> 427
    //   #566	-> 462
    //   #567	-> 480
    //   #568	-> 494
    //   #569	-> 499
    //   #571	-> 513
    //   #572	-> 525
    //   #573	-> 532
    //   #575	-> 540
    //   #573	-> 548
    //   #577	-> 558
    //   #579	-> 564
    //   #581	-> 566
    //   #582	-> 571
    //   #583	-> 585
    //   #585	-> 599
    //   #586	-> 630
    //   #587	-> 637
    //   #589	-> 640
    //   #591	-> 657
    //   #592	-> 688
    //   #593	-> 695
    //   #595	-> 698
    //   #596	-> 729
    //   #597	-> 736
    //   #599	-> 750
    //   #605	-> 763
    //   #606	-> 841
    //   #607	-> 851
    //   #608	-> 859
    //   #609	-> 896
    //   #610	-> 902
    //   #611	-> 909
    //   #612	-> 914
    //   #613	-> 952
    //   #615	-> 966
    //   #618	-> 968
    //   #620	-> 983
    //   #621	-> 991
    //   #625	-> 1007
    //   #627	-> 1020
    //   #632	-> 1025
    //   #634	-> 1030
    //   #632	-> 1033
    //   #633	-> 1035
    //   #634	-> 1043
    //   #535	-> 1048
    //   #636	-> 1058
    //   #637	-> 1062
    //   #638	-> 1080
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	1081	0	this	Lcom/crushftp/tunnel/Uploader;
    //   0	1081	1	parentfiles	Ljava/util/Vector;
    //   0	1081	2	uploadFiles	Ljava/util/Vector;
    //   0	1081	3	params	Ljava/util/Properties;
    //   0	1081	4	keywords	Ljava/lang/String;
    //   30	1051	5	loops	I
    //   39	1042	6	bigFiles	Ljava/util/Vector;
    //   48	1033	7	files2	Ljava/util/Vector;
    //   67	1014	8	totalCount	I
    //   70	1011	9	pos	I
    //   109	68	10	addBig	Z
    //   112	65	11	addSmall	Z
    //   122	55	12	f	Ljava/io/File;
    //   197	884	10	initialSize	I
    //   244	90	11	x	I
    //   269	53	12	f	Ljava/io/File;
    //   399	649	11	urlc	Ljava/net/HttpURLConnection;
    //   480	84	12	u	Ljava/net/URL;
    //   566	456	12	e	Ljava/lang/Exception;
    //   841	125	13	u	Ljava/net/URL;
    //   909	57	14	result	Ljava/lang/String;
    // Exception table:
    //   from	to	target	type
    //   356	369	372	finally
    //   372	375	372	finally
    //   399	561	564	java/lang/Exception
    //   399	561	1025	finally
    //   540	551	554	finally
    //   554	557	554	finally
    //   564	584	1025	finally
    //   585	1022	1025	finally
    //   763	963	966	java/lang/Exception
    //   1007	1017	1020	java/lang/Exception
    //   1045	1048	1025	finally
  }
  
  public OutputStream prepForUpload(URL u, HttpURLConnection urlc, String boundary, Properties params, String keywords, String path, String filename, String byteRange, long fileSize, File item) throws Exception {
    urlc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary.substring(2, boundary.length()));
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Cookie", "CrushAuth=" + this.CrushAuth.toString() + ";");
    urlc.setUseCaches(false);
    urlc.setChunkedStreamingMode(9999);
    urlc.setDoOutput(true);
    BufferedOutputStream out = new BufferedOutputStream(urlc.getOutputStream());
    if (params != null) {
      Enumeration en = params.keys();
      while (en.hasMoreElements()) {
        String key = en.nextElement().toString();
        if (key.toUpperCase().startsWith("META_")) {
          String val = params.getProperty(key, "");
          while (key.toUpperCase().startsWith("META_"))
            key = key.substring("META_".length()); 
          writeEntry("META_" + key, val, out, boundary);
        } 
      } 
    } 
    writeEntry("the_action", "STOR", out, boundary);
    writeEntry("c2f", this.CrushAuth.toString().substring(this.CrushAuth.toString().length() - 4), out, boundary);
    out.flush();
    if (keywords != null && !keywords.trim().equals(""))
      writeEntry("keywords", keywords, out, boundary); 
    writeEntry("uploadPath", path, out, boundary);
    if (byteRange != null) {
      long startPos = Long.parseLong(byteRange.substring(0, byteRange.indexOf("-")));
      String endPart = byteRange.substring(byteRange.indexOf("-") + 1);
      long endPos = -1L;
      if (!endPart.equals(""))
        endPos = Long.parseLong(endPart); 
      writeEntry("start_resume_loc", (new StringBuffer(String.valueOf(startPos))).toString(), out, boundary);
      writeEntry("randomaccess", (new StringBuffer(String.valueOf((endPos > -1L)))).toString(), out, boundary);
    } 
    if (fileSize >= 0L && byteRange == null) {
      writeEntry("speedCheat", "true", out, boundary);
      writeEntry("speedCheatSize", (new StringBuffer(String.valueOf(fileSize))).toString(), out, boundary);
    } 
    if (item != null)
      writeEntry("Last-Modified", this.sdf_rfc1123.format(new Date(item.lastModified())), out, boundary); 
    out.flush();
    String cheat = "";
    out.write((String.valueOf(boundary) + "\r\n").getBytes("UTF8"));
    out.write(("Content-Disposition: form-data; name=\"fileupload" + cheat + "\"; filename=\"" + filename + "\"\r\n").getBytes("UTF8"));
    out.write("Content-Type: application/octet-stream\r\n".getBytes("UTF8"));
    out.write("\r\n".getBytes("UTF8"));
    out.flush();
    return out;
  }
  
  public void finishHttpConnection(HttpURLConnection urlc, OutputStream out, String boundary, Properties status) throws Exception {
    out.write("\r\n".getBytes("UTF8"));
    writeEnd(out, boundary);
    urlc.getResponseCode();
    String result = Tunnel2.consumeResponse(urlc);
    urlc.disconnect();
    if (!Tunnel2.checkAction(this.action))
      return; 
    Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":" + result);
    if (result.toUpperCase().indexOf("SUCCESS") < 0) {
      Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":result:" + result);
      throw new Exception(result);
    } 
  }
  
  public void uploadNormalFiles(URL u, Properties params, String keywords, Vector files2, Vector parentfiles, int initialSize) throws Exception {
    String boundary = "--" + Common.makeBoundary(11);
    Properties threads = new Properties();
    StringBuffer itemIndex = new StringBuffer();
    itemIndex.append("1");
    Runnable r = new Runnable(this, threads, files2, u, parentfiles, boundary, params, keywords, itemIndex, initialSize) {
        final Uploader this$0;
        
        private final Properties val$threads;
        
        private final Vector val$files2;
        
        private final URL val$u;
        
        private final Vector val$parentfiles;
        
        private final String val$boundary;
        
        private final Properties val$params;
        
        private final String val$keywords;
        
        private final StringBuffer val$itemIndex;
        
        private final int val$initialSize;
        
        public void run() {
          Properties status = (Properties)this.val$threads.get(Thread.currentThread());
          HttpURLConnection urlc = null;
          File item = null;
          while (this.val$files2.size() > 0) {
            this.this$0.refreshStatusInfo();
            try {
              if (!Tunnel2.checkAction(this.this$0.action))
                throw new Exception("Cancelled"); 
              synchronized (this.val$files2) {
                if (this.val$files2.size() > 0)
                  item = this.val$files2.remove(0); 
              } 
              if (item == null)
                break; 
              if (item.exists()) {
                urlc = (HttpURLConnection)this.val$u.openConnection();
                urlc.setRequestProperty("Last-Modified", this.this$0.sdf_rfc1123.format(new Date(item.lastModified())));
                Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":Uploading normally:" + item.getName());
                Vector byteRanges = new Vector();
                StringBuffer delta = new StringBuffer();
                int offset = 0;
                int index = this.this$0.indexOfParent(item, this.val$parentfiles);
                if (index >= 0) {
                  File parent = this.val$parentfiles.elementAt(index);
                  offset = this.this$0.getCanonicalPath(parent).length();
                } 
                String uploadPath = this.val$u.getPath();
                if (!this.this$0.controller.getProperty("UPLOADPATH", "").equals(""))
                  uploadPath = this.this$0.controller.getProperty("UPLOADPATH", ""); 
                String itemName = this.this$0.getFileDownloadInfo(item, offset, byteRanges, delta, uploadPath, status);
                if (itemName == null)
                  continue; 
                uploadPath = String.valueOf(uploadPath) + Common.all_but_last(itemName);
                if (uploadPath.endsWith("//"))
                  uploadPath = uploadPath.substring(0, uploadPath.length() - 1); 
                for (int x = 0; x < byteRanges.size(); x++) {
                  String byteRange = byteRanges.elementAt(x).toString();
                  long startPos = Long.parseLong(byteRange.substring(0, byteRange.indexOf("-")));
                  String endPart = byteRange.substring(byteRange.indexOf("-") + 1);
                  long endPos = -1L;
                  if (!endPart.equals(""))
                    endPos = Long.parseLong(endPart); 
                  long byteAmount = -1L;
                  if (startPos >= 0L) {
                    byteAmount = item.length();
                    byteAmount -= startPos;
                    if (endPos > 0L)
                      byteAmount = item.length() - byteAmount; 
                  } 
                  if (x == byteRanges.size() - 1 && endPos >= 0L && item.length() < endPos)
                    byteRange = String.valueOf(startPos) + "--1"; 
                  Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":Uploading bytes:" + item.getName() + ":" + startPos + "-" + endPos);
                  OutputStream out = this.this$0.prepForUpload(this.val$u, urlc, this.val$boundary, this.val$params, this.val$keywords, uploadPath, item.getName(), byteRange, byteAmount, item);
                  OutputStream out2 = null;
                  this.this$0.uploadItem(byteRange, itemName, item, false, (out2 != null) ? out2 : out, this.val$itemIndex, this.val$initialSize, status);
                  if (!Tunnel2.checkAction(this.this$0.action))
                    break; 
                  Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":Checking for upload completion message..." + itemName);
                  if (out2 != null)
                    out2.close(); 
                  this.this$0.finishHttpConnection(urlc, out, this.val$boundary, status);
                  Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":Got upload completion message:" + itemName);
                } 
              } 
              if (!Tunnel2.checkAction(this.this$0.action))
                throw new Exception("Cancelled"); 
              synchronized (this.this$0.statLock) {
                int i = Integer.parseInt(this.val$itemIndex.toString());
                i++;
                this.val$itemIndex.setLength(0);
                this.val$itemIndex.append((new StringBuffer(String.valueOf(i))).toString());
              } 
              status.put("itemIndex", this.val$itemIndex);
            } catch (Exception e) {
              if (item != null)
                this.val$files2.insertElementAt(item, 0); 
              if (urlc != null)
                urlc.disconnect(); 
              if (!Tunnel2.checkAction(this.this$0.action))
                break; 
              status.put("error", e);
              break;
            } 
          } 
          this.this$0.refreshStatusInfo();
          status.put("status", "DONE");
        }
      };
    int maxThreads = Integer.parseInt(this.controller.getProperty("UPLOAD_THREADS", "1"));
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
  
  public void uploadZippedFiles(URL u, Properties params, String keywords, Vector files2, Vector parentfiles, int initialSize) throws Exception {
    Properties status = new Properties();
    status.put("index", "0");
    if (files2.size() == 0)
      return; 
    HttpURLConnection urlc = (HttpURLConnection)u.openConnection();
    StringBuffer bonusStatus = new StringBuffer();
    try {
      urlc.setReadTimeout(300000);
      String boundary = "--" + Common.makeBoundary(11);
      String uploadPath = u.getPath();
      if (!this.controller.getProperty("UPLOADPATH", "").equals(""))
        uploadPath = this.controller.getProperty("UPLOADPATH", ""); 
      OutputStream out = prepForUpload(u, urlc, boundary, params, keywords, uploadPath, "uploader" + (new Date()).getTime() + ".zipstream", null, -1L, null);
      FileArchiveOutputStream zout = new FileArchiveOutputStream(out, this.controller.getProperty("NOCOMPRESSION", "false").equals("false"));
      zout.setUseZip64(Zip64Mode.Always);
      if (this.controller.getProperty("NOCOMPRESSION", "false").equals("true")) {
        Tunnel2.msg("No compression being used.");
      } else {
        zout.setLevel(8);
      } 
      for (int xx = 0; xx < files2.size(); xx++) {
        refreshStatusInfo();
        try {
          File item = files2.elementAt(xx);
          if (item.exists()) {
            Tunnel2.msg("Working on item:" + item);
            int offset = 0;
            int index = indexOfParent(item, parentfiles);
            if (index >= 0) {
              File parent = parentfiles.elementAt(index);
              offset = getCanonicalPath(parent).length();
            } 
            if (item.isDirectory()) {
              String itemName = (String.valueOf(getCanonicalPath(item).substring(offset)) + "/").replace('\\', '/');
              if (itemName.startsWith("/"))
                itemName = itemName.substring(1); 
              this.statusInfo.put("uploadStatus", "Upload:Creating folder:" + itemName);
              Tunnel2.msg(getStatus());
              FileArchiveEntry zipEntry = new FileArchiveEntry(itemName);
              zipEntry.setTime(item.lastModified());
              zout.putArchiveEntry(zipEntry);
              zout.closeArchiveEntry();
              synchronized (this.statLock) {
                this.transferedItems++;
              } 
            } else if (item.isFile()) {
              if (!Tunnel2.checkAction(this.action))
                break; 
              Vector byteRanges = new Vector();
              String itemName = getFileDownloadInfo(item, offset, byteRanges, new StringBuffer(), uploadPath, status);
              if (itemName != null) {
                Tunnel2.msg("Adding zip entry:" + itemName);
                for (int x = 0; x < byteRanges.size(); x++) {
                  uploadItem(byteRanges.elementAt(x).toString(), itemName, item, true, (OutputStream)zout, (new StringBuffer()).append((new StringBuffer(String.valueOf(xx + 1))).toString()), initialSize, status);
                  if (!Tunnel2.checkAction(this.action))
                    break; 
                } 
                Tunnel2.msg("Zip entry complete:" + itemName);
              } 
            } 
          } 
        } catch (ZipException e) {
          if (e.toString().toUpperCase().indexOf("DUPLICATE") >= 0) {
            Tunnel2.msg("Ignoring duplicate item:" + e);
          } else {
            throw e;
          } 
        } 
      } 
      if (this.transferedItems > 0) {
        this.statusInfo.put("uploadStatus", "Upload:...");
        Tunnel2.msg(getStatus());
        zout.finish();
        zout.flush();
      } 
      Tunnel2.msg("Getting completion message for zip entries...");
      urlc.setReadTimeout(300000);
      (new Thread(new Runnable(this, bonusStatus) {
            final Uploader this$0;
            
            private final StringBuffer val$bonusStatus;
            
            public void run() {
              Thread.currentThread().setName("Uploader:UploadZippedFiles - message waiter");
              int loops = 0;
              while (this.val$bonusStatus.length() == 0) {
                try {
                  Thread.sleep(1000L);
                } catch (Exception exception) {}
                if (!Tunnel2.checkAction(this.this$0.action))
                  return; 
                if (this.val$bonusStatus.length() == 0 && loops++ >= 10) {
                  this.this$0.statusInfo.put("uploadStatus", "Upload:Server is decompressing files...please wait. (" + loops + ")");
                  Tunnel2.msg("Upload:Server is decompressing files...please wait. (" + loops + ")");
                } 
              } 
            }
          })).start();
      if (!Tunnel2.checkAction(this.action))
        throw new Exception("Cancelled"); 
      finishHttpConnection(urlc, out, boundary, status);
      Tunnel2.msg("Got completion message for zip entries.");
    } catch (Exception e) {
      bonusStatus.append("done");
      refreshStatusInfo();
      if (urlc != null)
        urlc.disconnect(); 
      throw e;
    } finally {
      bonusStatus.append("done");
      refreshStatusInfo();
    } 
  }
  
  public void uploadItem(String byteRange, String itemName, File item, boolean zip, OutputStream out, StringBuffer itemIndex, int initialSize, Properties status) throws Exception {
    long startPos = 0L;
    startPos = Long.parseLong(byteRange.substring(0, byteRange.indexOf("-")));
    long endPos = -1L;
    String endPart = byteRange.substring(byteRange.indexOf("-") + 1);
    if (!endPart.equals(""))
      endPos = Long.parseLong(endPart); 
    FileArchiveEntry zipEntry = new FileArchiveEntry(itemName);
    if (startPos > 0L) {
      this.statusInfo.put("uploadStatus", String.valueOf(status.getProperty("index")) + ":Upload:Resuming item:" + itemName + " at position:" + startPos + " to " + endPos + " (" + itemIndex + " of " + initialSize + ")");
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
    try {
      if (startPos > 0L)
        in.seek(startPos); 
      byte[] b = new byte[65536];
      int bytesRead = 0;
      long bytesThisFile = startPos;
      long pos = 0L;
      long start = System.currentTimeMillis();
      while (bytesRead >= 0) {
        if (endPos >= 0L && b.length > endPos - pos)
          b = new byte[(int)(endPos - pos)]; 
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
        if (!Tunnel2.checkAction(this.action) || (
          endPos >= 0L && pos == endPos))
          break; 
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
  
  public String getFileDownloadInfo(File f, int offset, Vector byteRanges, StringBuffer delta, String path, Properties status) throws Exception {
    this.statusInfo.put("uploadStatus", String.valueOf(status.getProperty("index")) + ":Uploading file " + f.getPath() + "...");
    String itemName = getCanonicalPath(f).substring(offset).replace('\\', '/');
    if (itemName.startsWith("/"))
      itemName = itemName.substring(1); 
    itemName = String.valueOf(path) + itemName;
    long serverSize = -1L;
    URL u = new URL(String.valueOf(this.controller.getProperty("URL_REAL", this.controller.getProperty("URL"))) + itemName + "/:filetree");
    Tunnel2.msg("Getting folder contents information " + u.toExternalForm());
    HttpURLConnection urlc = (HttpURLConnection)u.openConnection();
    urlc.setReadTimeout(70000);
    urlc.setRequestMethod("GET");
    urlc.setRequestProperty("Cookie", "CrushAuth=" + this.CrushAuth.toString() + ";");
    urlc.setUseCaches(false);
    urlc.setDoInput(true);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(urlc.getInputStream(), "UTF8"));
      String data = "";
      while ((data = br.readLine()) != null) {
        StringTokenizer st = new StringTokenizer(data);
        st.nextToken();
        st.nextToken();
        st.nextToken();
        st.nextToken();
        serverSize = Long.parseLong(st.nextToken());
        if (!Tunnel2.checkAction(this.action))
          break; 
      } 
    } catch (Exception e) {
      e.printStackTrace();
    } 
    if (br != null)
      br.close(); 
    urlc.disconnect();
    delta.append((new StringBuffer(String.valueOf(!(!this.controller.getProperty("delta", "false").equals("true") && !this.controller.getProperty("DELTA", "false").equals("true"))))).toString());
    if (!delta.toString().equals("true") && serverSize == f.length()) {
      Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":Skipping item since it already exists:" + itemName);
      this.statusInfo.put("uploadStatus", String.valueOf(status.getProperty("index")) + ":Upload:Skipping item since it already exists:" + itemName);
      synchronized (this.statLock) {
        this.transferedBytes += serverSize;
      } 
      return null;
    } 
    if (serverSize > 0L && serverSize < f.length()) {
      delta.setLength(0);
      delta.append("true");
    } 
    byteRanges.addElement("0--1");
    if (delta.toString().equals("true") && f.length() > 10485760L) {
      Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":Asking server for list of remote MD5 hashes for " + itemName);
      StringBuffer status1 = new StringBuffer();
      StringBuffer status2 = new StringBuffer();
      Vector chunksF1 = new Vector();
      Vector chunksF2 = new Vector();
      Tunnel2.doMD5Comparisons("upload", this.statusInfo, itemName, this.controller, chunksF1, chunksF2, this.CrushAuth, status1, status2, f, byteRanges, this.action);
      Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":Got " + chunksF1.size() + " remote and " + chunksF2.size() + " local MD5 hashes for " + itemName);
      Tunnel2.msg(String.valueOf(status.getProperty("index")) + ":Hash comparison: " + itemName + ":" + byteRanges.size());
      if (byteRanges.size() == 1 && byteRanges.elementAt(0).equals("0--1"))
        byteRanges.setElementAt("0-" + f.length(), 0); 
      long amount = 0L;
      for (int x = 0; x < byteRanges.size(); x++) {
        if (!byteRanges.elementAt(x).toString().trim().equals("")) {
          long start = Long.parseLong(byteRanges.elementAt(x).toString().substring(0, byteRanges.elementAt(x).toString().indexOf("-")));
          String endPart = byteRanges.elementAt(x).toString().substring(byteRanges.elementAt(x).toString().indexOf("-") + 1);
          if (endPart.equals(""))
            endPart = (new StringBuffer(String.valueOf(f.length()))).toString(); 
          long end = Long.parseLong(endPart);
          if (end >= 0L)
            amount += end - start; 
        } 
      } 
      synchronized (this.statLock) {
        this.transferedBytes += f.length() - amount;
      } 
    } 
    return itemName.substring(path.length());
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
  
  public void writeEnd(OutputStream out, String boundary) throws Exception {
    if (!Tunnel2.checkAction(this.action))
      throw new Exception("Cancelled"); 
    out.write((String.valueOf(boundary) + "--\r\n").getBytes("UTF8"));
    out.flush();
    out.close();
  }
  
  public void writeEntry(String key, String val, OutputStream dos, String boundary) throws Exception {
    Tunnel2.msg(String.valueOf(key) + ":" + val);
    if (!Tunnel2.checkAction(this.action))
      throw new Exception("Cancelled"); 
    dos.write((String.valueOf(boundary) + "\r\n").getBytes("UTF8"));
    dos.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n").getBytes("UTF8"));
    dos.write("\r\n".getBytes("UTF8"));
    dos.write((String.valueOf(val) + "\r\n").getBytes("UTF8"));
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
