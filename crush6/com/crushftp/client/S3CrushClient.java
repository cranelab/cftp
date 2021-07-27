package com.crushftp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.crypto.spec.SecretKeySpec;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class S3CrushClient extends S3Client {
  static long ram_used_download = 0L;
  
  static Object ram_lock = new Object();
  
  String bucketName0 = null;
  
  String s3_root = "./s3/";
  
  Vector in_progress = new Vector();
  
  boolean uploading = false;
  
  Vector replicating = null;
  
  public S3CrushClient(String url, String header, Vector log) {
    super(url, header, log);
    System.setProperty("crushtunnel.debug", "2");
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
    this.s3_root = System.getProperty("crushftp.s3_root", "./s3/");
    this.replicating = (Vector)System.getProperties().get("crushftp.s3_replicated");
  }
  
  public String getRawXmlPath(String path0) throws Exception {
    path0 = getPath(path0);
    return String.valueOf(this.s3_root) + this.bucketName0 + path0;
  }
  
  private String getPath(String path0) throws Exception {
    if (this.bucketName0 == null) {
      this.bucketName0 = path0.substring(1, path0.indexOf("/", 1)).toLowerCase();
      (new File(String.valueOf(this.s3_root) + this.bucketName0)).mkdirs();
    } 
    if (this.secretKey == null)
      login((new VRL(this.url)).getUsername(), (new VRL(this.url)).getPassword(), ""); 
    return Common.dots(path0.substring(path0.indexOf("/", 1)));
  }
  
  public static void writeFs(String s3_root, String bucketName0, Vector replicating, String path0, Properties p) throws Exception {
    String path = Common.dots(path0);
    (new File(String.valueOf(s3_root) + bucketName0 + Common.all_but_last(path))).mkdirs();
    Common.writeXMLObject(String.valueOf(s3_root) + bucketName0 + path, p, "s3");
    if (replicating != null) {
      Properties p2 = new Properties();
      p2.put("bucketName0", bucketName0);
      p2.put("path", path0);
      p2.put("data", Common.CLONE(p));
      replicating.addElement(p2);
    } 
  }
  
  public void resetBucket() {
    this.bucketName0 = null;
  }
  
  public String login(String username, String password, String clientid) throws Exception {
    this.config.put("username", username.trim());
    this.config.put("password", password.trim());
    this.secretKey = new SecretKeySpec(password.trim().getBytes("UTF8"), "HmacSHA1");
    URLConnection urlc = doAction("GET", "/", (StringBuffer)null, false);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code < 200 || code > 299) {
      log(String.valueOf(result) + "\r\n");
      throw new IOException(result);
    } 
    return result;
  }
  
  public void logout() throws Exception {
    synchronized (this.in_progress) {
      while (this.in_progress.size() > 0) {
        Thread t = this.in_progress.remove(0);
        t.interrupt();
      } 
    } 
    close();
  }
  
  public void close() throws Exception {
    if (this.in != null) {
      this.in.close();
      this.in = null;
    } 
    if (this.out != null) {
      for (int x = 0; x < 60 && this.uploading; x++)
        Thread.sleep(1000L); 
      this.out.close();
      this.out = null;
    } 
  }
  
  public Vector list(String path0, Vector list) throws Exception {
    path0 = getPath(path0);
    File[] f = (new File(String.valueOf(this.s3_root) + this.bucketName0 + path0)).listFiles();
    if (f == null)
      throw new Exception("No such folder."); 
    for (int x = 0; x < f.length; x++) {
      if (!f[x].getName().equals(".DS_Store")) {
        Date d = new Date(f[x].lastModified());
        Properties p = null;
        if (f[x].isFile()) {
          p = (Properties)Common.readXMLObject(f[x]);
        } else {
          p = new Properties();
        } 
        String line = String.valueOf(f[x].isDirectory() ? "d" : "-") + "rwxrwxrwx   1    owner   group   " + p.getProperty("size", "0") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + f[x].getName();
        Properties stat = parseStat(line);
        stat.put("url", String.valueOf(this.url) + this.bucketName0 + path0 + stat.getProperty("name") + (f[x].isDirectory() ? "/" : ""));
        list.addElement(stat);
      } 
    } 
    return list;
  }
  
  protected InputStream download3(String path0, long startPos0, long endPos, boolean binary) throws Exception {
    long startPos = (startPos0 < 0L) ? 0L : startPos0;
    Properties stat = stat(path0);
    Properties socks = Common.getConnectedSocks(false);
    Socket sock1 = (Socket)socks.get("sock1");
    Socket sock2 = (Socket)socks.get("sock2");
    Vector chunks_needed = new Vector();
    if (stat.containsKey("segments"))
      chunks_needed.addAll((Vector)stat.get("segments")); 
    Properties chunks = new Properties();
    Properties status = new Properties();
    long len = Long.parseLong(stat.getProperty("size"));
    if (endPos > 0L && endPos < len)
      len = endPos; 
    long lenF = len;
    boolean segmented = stat.containsKey("segments");
    if (!stat.containsKey("segments")) {
      long pos = startPos;
      long amount = Long.parseLong(this.config.getProperty("s3_buffer", "5")) * 1024L * 1024L;
      if (pos < 0L)
        pos = 0L; 
      int i = 1;
      while (pos < len) {
        chunks_needed.addElement(String.valueOf(i) + ":" + pos + "-" + (pos + amount));
        pos += amount + 1L;
        i++;
      } 
    } 
    if (startPos > 0L && segmented) {
      boolean delete = false;
      for (int x = chunks_needed.size() - 1; x >= 0; x--) {
        if (delete) {
          chunks_needed.remove(x);
        } else {
          String part = chunks_needed.elementAt(x).toString();
          if (startPos >= Long.parseLong(part.split(":")[1].split("-")[0]) && startPos <= Long.parseLong(part.split(":")[1].split("-")[1]))
            delete = true; 
        } 
      } 
    } 
    status.put("first", "true");
    status.put("ram", "0");
    status.put("current_pos", "0");
    log("Chunks needed:" + chunks_needed);
    Runnable grabChunk = new Runnable(this, chunks_needed, status, stat, segmented, startPos, chunks) {
        final S3CrushClient this$0;
        
        private final Vector val$chunks_needed;
        
        private final Properties val$status;
        
        private final Properties val$stat;
        
        private final boolean val$segmented;
        
        private final long val$startPos;
        
        private final Properties val$chunks;
        
        public void run() {
          // Byte code:
          //   0: goto -> 1150
          //   3: getstatic com/crushftp/client/S3CrushClient.ram_used_download : J
          //   6: ldc2_w 1024
          //   9: ldiv
          //   10: ldc2_w 1024
          //   13: ldiv
          //   14: aload_0
          //   15: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   18: getfield config : Ljava/util/Properties;
          //   21: ldc 's3_max_buffer_download'
          //   23: ldc '100'
          //   25: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   28: invokestatic parseLong : (Ljava/lang/String;)J
          //   31: lcmp
          //   32: ifle -> 53
          //   35: aload_0
          //   36: getfield val$status : Ljava/util/Properties;
          //   39: ldc 'error'
          //   41: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   44: ifne -> 53
          //   47: ldc2_w 1000
          //   50: invokestatic sleep : (J)V
          //   53: aload_0
          //   54: getfield val$status : Ljava/util/Properties;
          //   57: ldc 'ram'
          //   59: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   62: invokestatic parseLong : (Ljava/lang/String;)J
          //   65: ldc2_w 1024
          //   68: ldiv
          //   69: ldc2_w 1024
          //   72: ldiv
          //   73: aload_0
          //   74: getfield val$status : Ljava/util/Properties;
          //   77: ldc 's3_max_buffer_download'
          //   79: ldc '100'
          //   81: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   84: invokestatic parseLong : (Ljava/lang/String;)J
          //   87: ldc2_w 2
          //   90: ldiv
          //   91: lcmp
          //   92: ifle -> 117
          //   95: aload_0
          //   96: getfield val$status : Ljava/util/Properties;
          //   99: ldc 'error'
          //   101: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   104: ifne -> 117
          //   107: ldc2_w 1000
          //   110: invokestatic sleep : (J)V
          //   113: goto -> 117
          //   116: astore_1
          //   117: aload_0
          //   118: getfield val$status : Ljava/util/Properties;
          //   121: ldc 'error'
          //   123: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   126: ifeq -> 132
          //   129: goto -> 1172
          //   132: aconst_null
          //   133: astore_1
          //   134: iconst_0
          //   135: istore_2
          //   136: aload_0
          //   137: getfield val$chunks_needed : Ljava/util/Vector;
          //   140: dup
          //   141: astore_3
          //   142: monitorenter
          //   143: aload_0
          //   144: getfield val$status : Ljava/util/Properties;
          //   147: ldc 'first'
          //   149: ldc 'false'
          //   151: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   154: ldc 'true'
          //   156: invokevirtual equals : (Ljava/lang/Object;)Z
          //   159: istore_2
          //   160: aload_0
          //   161: getfield val$status : Ljava/util/Properties;
          //   164: ldc 'first'
          //   166: ldc 'false'
          //   168: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   171: pop
          //   172: aload_0
          //   173: getfield val$chunks_needed : Ljava/util/Vector;
          //   176: invokevirtual size : ()I
          //   179: ifle -> 194
          //   182: aload_0
          //   183: getfield val$chunks_needed : Ljava/util/Vector;
          //   186: iconst_0
          //   187: invokevirtual remove : (I)Ljava/lang/Object;
          //   190: invokevirtual toString : ()Ljava/lang/String;
          //   193: astore_1
          //   194: aload_3
          //   195: monitorexit
          //   196: goto -> 202
          //   199: aload_3
          //   200: monitorexit
          //   201: athrow
          //   202: aload_1
          //   203: ifnonnull -> 209
          //   206: goto -> 1172
          //   209: aload_0
          //   210: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   213: ldc 'GET'
          //   215: new java/lang/StringBuffer
          //   218: dup
          //   219: ldc '/'
          //   221: invokespecial <init> : (Ljava/lang/String;)V
          //   224: aload_0
          //   225: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   228: getfield bucketName0 : Ljava/lang/String;
          //   231: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   234: ldc '/'
          //   236: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   239: aload_0
          //   240: getfield val$stat : Ljava/util/Properties;
          //   243: ldc 'uid'
          //   245: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   248: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   251: aload_0
          //   252: getfield val$segmented : Z
          //   255: ifeq -> 284
          //   258: new java/lang/StringBuffer
          //   261: dup
          //   262: ldc '_'
          //   264: invokespecial <init> : (Ljava/lang/String;)V
          //   267: aload_1
          //   268: ldc ':'
          //   270: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   273: iconst_0
          //   274: aaload
          //   275: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   278: invokevirtual toString : ()Ljava/lang/String;
          //   281: goto -> 286
          //   284: ldc ''
          //   286: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   289: invokevirtual toString : ()Ljava/lang/String;
          //   292: aconst_null
          //   293: iconst_0
          //   294: invokevirtual doAction : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/StringBuffer;Z)Lcom/crushftp/client/URLConnection;
          //   297: astore_3
          //   298: aload_0
          //   299: getfield val$startPos : J
          //   302: aload_1
          //   303: ldc ':'
          //   305: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   308: iconst_1
          //   309: aaload
          //   310: ldc '-'
          //   312: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   315: iconst_0
          //   316: aaload
          //   317: invokestatic parseLong : (Ljava/lang/String;)J
          //   320: lsub
          //   321: lstore #4
          //   323: aload_0
          //   324: getfield val$segmented : Z
          //   327: ifne -> 362
          //   330: aload_3
          //   331: ldc 'Range'
          //   333: new java/lang/StringBuffer
          //   336: dup
          //   337: ldc 'bytes='
          //   339: invokespecial <init> : (Ljava/lang/String;)V
          //   342: aload_1
          //   343: ldc ':'
          //   345: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   348: iconst_1
          //   349: aaload
          //   350: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   353: invokevirtual toString : ()Ljava/lang/String;
          //   356: invokevirtual setRequestProperty : (Ljava/lang/String;Ljava/lang/String;)V
          //   359: goto -> 482
          //   362: iload_2
          //   363: ifeq -> 482
          //   366: aload_0
          //   367: getfield val$segmented : Z
          //   370: ifeq -> 482
          //   373: lload #4
          //   375: lconst_0
          //   376: lcmp
          //   377: ifeq -> 482
          //   380: new java/lang/StringBuffer
          //   383: dup
          //   384: aload_1
          //   385: ldc ':'
          //   387: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   390: iconst_0
          //   391: aaload
          //   392: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   395: invokespecial <init> : (Ljava/lang/String;)V
          //   398: ldc ':'
          //   400: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   403: aload_1
          //   404: ldc ':'
          //   406: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   409: iconst_1
          //   410: aaload
          //   411: ldc '-'
          //   413: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   416: iconst_0
          //   417: aaload
          //   418: invokestatic parseLong : (Ljava/lang/String;)J
          //   421: lload #4
          //   423: ladd
          //   424: invokevirtual append : (J)Ljava/lang/StringBuffer;
          //   427: ldc '-'
          //   429: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   432: aload_1
          //   433: ldc ':'
          //   435: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   438: iconst_1
          //   439: aaload
          //   440: ldc '-'
          //   442: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   445: iconst_1
          //   446: aaload
          //   447: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   450: invokevirtual toString : ()Ljava/lang/String;
          //   453: astore_1
          //   454: aload_3
          //   455: ldc 'Range'
          //   457: new java/lang/StringBuffer
          //   460: dup
          //   461: ldc 'bytes='
          //   463: invokespecial <init> : (Ljava/lang/String;)V
          //   466: lload #4
          //   468: invokevirtual append : (J)Ljava/lang/StringBuffer;
          //   471: ldc '-'
          //   473: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   476: invokevirtual toString : ()Ljava/lang/String;
          //   479: invokevirtual setRequestProperty : (Ljava/lang/String;Ljava/lang/String;)V
          //   482: invokestatic currentTimeMillis : ()J
          //   485: lstore #6
          //   487: aload_1
          //   488: ldc ':-1-'
          //   490: invokevirtual indexOf : (Ljava/lang/String;)I
          //   493: iflt -> 514
          //   496: aload_1
          //   497: ldc ':-1-'
          //   499: ldc ':0-'
          //   501: invokestatic replace_str : (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   504: astore_1
          //   505: goto -> 514
          //   508: ldc2_w 100
          //   511: invokestatic sleep : (J)V
          //   514: aload_0
          //   515: getfield val$status : Ljava/util/Properties;
          //   518: ldc 'error'
          //   520: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   523: ifne -> 783
          //   526: invokestatic currentTimeMillis : ()J
          //   529: lload #6
          //   531: lsub
          //   532: ldc2_w 60000
          //   535: lcmp
          //   536: ifge -> 783
          //   539: aload_1
          //   540: ldc ':'
          //   542: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   545: iconst_1
          //   546: aaload
          //   547: ldc '-'
          //   549: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   552: iconst_0
          //   553: aaload
          //   554: invokestatic parseLong : (Ljava/lang/String;)J
          //   557: aload_0
          //   558: getfield val$status : Ljava/util/Properties;
          //   561: ldc 'current_pos'
          //   563: ldc '0'
          //   565: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   568: invokestatic parseLong : (Ljava/lang/String;)J
          //   571: lsub
          //   572: aload_0
          //   573: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   576: getfield config : Ljava/util/Properties;
          //   579: ldc 's3_buffer'
          //   581: ldc '5'
          //   583: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   586: invokestatic parseLong : (Ljava/lang/String;)J
          //   589: ldc2_w 1024
          //   592: lmul
          //   593: ldc2_w 1024
          //   596: lmul
          //   597: ldc2_w 2
          //   600: lmul
          //   601: aload_0
          //   602: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   605: getfield config : Ljava/util/Properties;
          //   608: ldc 's3_threads_download'
          //   610: ldc '3'
          //   612: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   615: invokestatic parseInt : (Ljava/lang/String;)I
          //   618: i2l
          //   619: lmul
          //   620: lcmp
          //   621: ifgt -> 508
          //   624: goto -> 783
          //   627: astore #8
          //   629: aload_0
          //   630: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   633: new java/lang/StringBuffer
          //   636: dup
          //   637: ldc 'Wait:'
          //   639: invokespecial <init> : (Ljava/lang/String;)V
          //   642: invokestatic currentTimeMillis : ()J
          //   645: lload #6
          //   647: lsub
          //   648: invokevirtual append : (J)Ljava/lang/StringBuffer;
          //   651: invokevirtual toString : ()Ljava/lang/String;
          //   654: invokevirtual log : (Ljava/lang/String;)V
          //   657: aload_0
          //   658: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   661: new java/lang/StringBuffer
          //   664: dup
          //   665: ldc 'Part:'
          //   667: invokespecial <init> : (Ljava/lang/String;)V
          //   670: aload_1
          //   671: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   674: invokevirtual toString : ()Ljava/lang/String;
          //   677: invokevirtual log : (Ljava/lang/String;)V
          //   680: aload_0
          //   681: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   684: new java/lang/StringBuffer
          //   687: dup
          //   688: ldc 'Buffer:'
          //   690: invokespecial <init> : (Ljava/lang/String;)V
          //   693: aload_0
          //   694: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   697: getfield config : Ljava/util/Properties;
          //   700: ldc 's3_buffer'
          //   702: ldc '5'
          //   704: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   707: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   710: invokevirtual toString : ()Ljava/lang/String;
          //   713: invokevirtual log : (Ljava/lang/String;)V
          //   716: aload_0
          //   717: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   720: new java/lang/StringBuffer
          //   723: dup
          //   724: ldc 'Threads:'
          //   726: invokespecial <init> : (Ljava/lang/String;)V
          //   729: aload_0
          //   730: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   733: getfield config : Ljava/util/Properties;
          //   736: ldc 's3_threads_download'
          //   738: ldc '3'
          //   740: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   743: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   746: invokevirtual toString : ()Ljava/lang/String;
          //   749: invokevirtual log : (Ljava/lang/String;)V
          //   752: aload_0
          //   753: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   756: new java/lang/StringBuffer
          //   759: dup
          //   760: ldc 'Status:'
          //   762: invokespecial <init> : (Ljava/lang/String;)V
          //   765: aload_0
          //   766: getfield val$status : Ljava/util/Properties;
          //   769: invokevirtual append : (Ljava/lang/Object;)Ljava/lang/StringBuffer;
          //   772: invokevirtual toString : ()Ljava/lang/String;
          //   775: invokevirtual log : (Ljava/lang/String;)V
          //   778: aload #8
          //   780: athrow
          //   781: astore #6
          //   783: aload_0
          //   784: getfield val$status : Ljava/util/Properties;
          //   787: ldc 'error'
          //   789: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   792: ifeq -> 798
          //   795: goto -> 1172
          //   798: aload_3
          //   799: invokevirtual getResponseCode : ()I
          //   802: istore #6
          //   804: iload #6
          //   806: sipush #200
          //   809: if_icmplt -> 820
          //   812: iload #6
          //   814: sipush #299
          //   817: if_icmple -> 870
          //   820: aload_3
          //   821: invokevirtual getInputStream : ()Ljava/io/InputStream;
          //   824: invokestatic consumeResponse : (Ljava/io/InputStream;)Ljava/lang/String;
          //   827: astore #7
          //   829: aload_3
          //   830: invokevirtual disconnect : ()V
          //   833: aload_0
          //   834: getfield this$0 : Lcom/crushftp/client/S3CrushClient;
          //   837: new java/lang/StringBuffer
          //   840: dup
          //   841: aload #7
          //   843: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   846: invokespecial <init> : (Ljava/lang/String;)V
          //   849: ldc '\\r\\n'
          //   851: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   854: invokevirtual toString : ()Ljava/lang/String;
          //   857: invokevirtual log : (Ljava/lang/String;)V
          //   860: new java/io/IOException
          //   863: dup
          //   864: aload #7
          //   866: invokespecial <init> : (Ljava/lang/String;)V
          //   869: athrow
          //   870: new java/io/ByteArrayOutputStream
          //   873: dup
          //   874: invokespecial <init> : ()V
          //   877: astore #7
          //   879: aload_3
          //   880: invokevirtual getInputStream : ()Ljava/io/InputStream;
          //   883: aload #7
          //   885: iconst_0
          //   886: iconst_1
          //   887: iconst_1
          //   888: invokestatic streamCopier : (Ljava/io/InputStream;Ljava/io/OutputStream;ZZZ)V
          //   891: aload_3
          //   892: invokevirtual disconnect : ()V
          //   895: aload_0
          //   896: getfield val$status : Ljava/util/Properties;
          //   899: ldc 'error'
          //   901: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   904: ifne -> 1127
          //   907: getstatic com/crushftp/client/S3CrushClient.ram_lock : Ljava/lang/Object;
          //   910: dup
          //   911: astore #8
          //   913: monitorenter
          //   914: aload #7
          //   916: invokevirtual toByteArray : ()[B
          //   919: astore #9
          //   921: getstatic com/crushftp/client/S3CrushClient.ram_used_download : J
          //   924: aload #9
          //   926: arraylength
          //   927: i2l
          //   928: ladd
          //   929: putstatic com/crushftp/client/S3CrushClient.ram_used_download : J
          //   932: aload_0
          //   933: getfield val$chunks : Ljava/util/Properties;
          //   936: aload_1
          //   937: ldc ':'
          //   939: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   942: iconst_1
          //   943: aaload
          //   944: ldc '-'
          //   946: invokevirtual split : (Ljava/lang/String;)[Ljava/lang/String;
          //   949: iconst_0
          //   950: aaload
          //   951: aload #9
          //   953: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   956: pop
          //   957: aload_0
          //   958: getfield val$status : Ljava/util/Properties;
          //   961: ldc 'ram'
          //   963: new java/lang/StringBuffer
          //   966: dup
          //   967: aload_0
          //   968: getfield val$status : Ljava/util/Properties;
          //   971: ldc 'ram'
          //   973: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   976: invokestatic parseLong : (Ljava/lang/String;)J
          //   979: aload #9
          //   981: arraylength
          //   982: i2l
          //   983: ladd
          //   984: invokestatic valueOf : (J)Ljava/lang/String;
          //   987: invokespecial <init> : (Ljava/lang/String;)V
          //   990: invokevirtual toString : ()Ljava/lang/String;
          //   993: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   996: pop
          //   997: invokestatic currentThread : ()Ljava/lang/Thread;
          //   1000: invokevirtual getName : ()Ljava/lang/String;
          //   1003: astore #10
          //   1005: new java/lang/StringBuffer
          //   1008: dup
          //   1009: aload #10
          //   1011: iconst_0
          //   1012: aload #10
          //   1014: ldc ':'
          //   1016: invokevirtual lastIndexOf : (Ljava/lang/String;)I
          //   1019: iconst_1
          //   1020: iadd
          //   1021: invokevirtual substring : (II)Ljava/lang/String;
          //   1024: invokestatic valueOf : (Ljava/lang/Object;)Ljava/lang/String;
          //   1027: invokespecial <init> : (Ljava/lang/String;)V
          //   1030: aload_0
          //   1031: getfield val$status : Ljava/util/Properties;
          //   1034: ldc 'ram'
          //   1036: invokevirtual getProperty : (Ljava/lang/String;)Ljava/lang/String;
          //   1039: invokestatic parseLong : (Ljava/lang/String;)J
          //   1042: invokestatic format_bytes_short : (J)Ljava/lang/String;
          //   1045: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1048: ldc_w ' of '
          //   1051: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1054: getstatic com/crushftp/client/S3CrushClient.ram_used_download : J
          //   1057: invokestatic format_bytes_short : (J)Ljava/lang/String;
          //   1060: invokevirtual append : (Ljava/lang/String;)Ljava/lang/StringBuffer;
          //   1063: invokevirtual toString : ()Ljava/lang/String;
          //   1066: astore #10
          //   1068: invokestatic currentThread : ()Ljava/lang/Thread;
          //   1071: aload #10
          //   1073: invokevirtual setName : (Ljava/lang/String;)V
          //   1076: aload #8
          //   1078: monitorexit
          //   1079: goto -> 1127
          //   1082: aload #8
          //   1084: monitorexit
          //   1085: athrow
          //   1086: astore #6
          //   1088: aload_0
          //   1089: getfield val$status : Ljava/util/Properties;
          //   1092: ldc 'error'
          //   1094: aload #6
          //   1096: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1099: pop
          //   1100: goto -> 1127
          //   1103: astore #6
          //   1105: aload_0
          //   1106: getfield val$status : Ljava/util/Properties;
          //   1109: ldc 'error'
          //   1111: aload #6
          //   1113: invokevirtual put : (Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
          //   1116: pop
          //   1117: ldc_w 'S3_CLIENT'
          //   1120: iconst_1
          //   1121: aload #6
          //   1123: invokestatic log : (Ljava/lang/String;ILjava/lang/Exception;)Z
          //   1126: pop
          //   1127: aload_0
          //   1128: getfield val$status : Ljava/util/Properties;
          //   1131: ldc_w 'run_once'
          //   1134: ldc 'false'
          //   1136: invokevirtual getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
          //   1139: ldc 'true'
          //   1141: invokevirtual equals : (Ljava/lang/Object;)Z
          //   1144: ifeq -> 1150
          //   1147: goto -> 1172
          //   1150: aload_0
          //   1151: getfield val$chunks_needed : Ljava/util/Vector;
          //   1154: invokevirtual size : ()I
          //   1157: ifle -> 1172
          //   1160: aload_0
          //   1161: getfield val$status : Ljava/util/Properties;
          //   1164: ldc 'error'
          //   1166: invokevirtual containsKey : (Ljava/lang/Object;)Z
          //   1169: ifeq -> 3
          //   1172: return
          // Line number table:
          //   Java source line number -> byte code offset
          //   #237	-> 0
          //   #241	-> 3
          //   #242	-> 53
          //   #244	-> 116
          //   #247	-> 117
          //   #248	-> 132
          //   #249	-> 134
          //   #250	-> 136
          //   #252	-> 143
          //   #253	-> 160
          //   #254	-> 172
          //   #250	-> 194
          //   #256	-> 202
          //   #258	-> 209
          //   #259	-> 298
          //   #260	-> 323
          //   #261	-> 362
          //   #263	-> 380
          //   #264	-> 454
          //   #270	-> 482
          //   #272	-> 487
          //   #275	-> 505
          //   #276	-> 508
          //   #275	-> 514
          //   #278	-> 627
          //   #280	-> 629
          //   #281	-> 657
          //   #282	-> 680
          //   #283	-> 716
          //   #284	-> 752
          //   #285	-> 778
          //   #288	-> 781
          //   #291	-> 783
          //   #292	-> 798
          //   #293	-> 804
          //   #295	-> 820
          //   #296	-> 829
          //   #297	-> 833
          //   #298	-> 860
          //   #300	-> 870
          //   #301	-> 879
          //   #302	-> 891
          //   #303	-> 895
          //   #305	-> 907
          //   #307	-> 914
          //   #308	-> 921
          //   #309	-> 932
          //   #310	-> 957
          //   #311	-> 997
          //   #312	-> 1005
          //   #313	-> 1068
          //   #305	-> 1076
          //   #317	-> 1086
          //   #319	-> 1088
          //   #321	-> 1103
          //   #323	-> 1105
          //   #324	-> 1117
          //   #326	-> 1127
          //   #237	-> 1150
          //   #328	-> 1172
          // Local variable table:
          //   start	length	slot	name	descriptor
          //   0	1173	0	this	Lcom/crushftp/client/S3CrushClient$1;
          //   134	1016	1	part	Ljava/lang/String;
          //   136	1014	2	first	Z
          //   298	852	3	urlc	Lcom/crushftp/client/URLConnection;
          //   323	827	4	offset	J
          //   487	294	6	start_wait	J
          //   629	152	8	e	Ljava/lang/NumberFormatException;
          //   804	282	6	code	I
          //   829	41	7	result	Ljava/lang/String;
          //   879	207	7	baos	Ljava/io/ByteArrayOutputStream;
          //   921	155	9	b	[B
          //   1005	71	10	s	Ljava/lang/String;
          //   1088	12	6	e	Ljava/lang/InterruptedException;
          //   1105	22	6	e	Ljava/io/IOException;
          // Exception table:
          //   from	to	target	type
          //   3	113	116	java/lang/InterruptedException
          //   143	196	199	finally
          //   199	201	199	finally
          //   482	781	781	java/lang/InterruptedException
          //   482	795	1086	java/lang/InterruptedException
          //   482	795	1103	java/io/IOException
          //   505	624	627	java/lang/NumberFormatException
          //   798	1086	1086	java/lang/InterruptedException
          //   798	1086	1103	java/io/IOException
          //   914	1079	1082	finally
          //   1082	1085	1082	finally
        }
      };
    Runnable grabChunkF = grabChunk;
    Runnable downloadChunks = new Runnable(this, startPos, chunks_needed, status, chunks, sock2, lenF, grabChunkF, sock1) {
        final S3CrushClient this$0;
        
        private final long val$startPos;
        
        private final Vector val$chunks_needed;
        
        private final Properties val$status;
        
        private final Properties val$chunks;
        
        private final Socket val$sock2;
        
        private final long val$lenF;
        
        private final Runnable val$grabChunkF;
        
        private final Socket val$sock1;
        
        public void run() {
          this.this$0.in_progress.addElement(Thread.currentThread());
          long current_pos = this.val$startPos;
          if (current_pos < 0L)
            current_pos = 0L; 
          try {
            OutputStream out_tmp = this.val$sock2.getOutputStream();
            while (current_pos < this.val$lenF && !this.val$status.containsKey("error")) {
              if (this.this$0.config.getProperty("multithreaded_s3", "true").equals("false")) {
                this.val$status.put("run_once", "true");
                this.val$grabChunkF.run();
              } 
              if (this.val$chunks.containsKey((new StringBuffer(String.valueOf(current_pos))).toString())) {
                byte[] b = (byte[])null;
                synchronized (S3CrushClient.ram_lock) {
                  b = (byte[])this.val$chunks.remove((new StringBuffer(String.valueOf(current_pos))).toString());
                  S3CrushClient.ram_used_download -= b.length;
                  this.val$status.put("ram", (new StringBuffer(String.valueOf(Long.parseLong(this.val$status.getProperty("ram")) - b.length))).toString());
                  String s = Thread.currentThread().getName();
                  s = String.valueOf(s.substring(0, s.lastIndexOf(":") + 1)) + Common.format_bytes_short(Long.parseLong(this.val$status.getProperty("ram"))) + " of " + Common.format_bytes_short(S3CrushClient.ram_used_download);
                  Thread.currentThread().setName(s);
                } 
                current_pos += b.length;
                this.val$status.put("current_pos", (new StringBuffer(String.valueOf(current_pos))).toString());
                out_tmp.write(b);
                continue;
              } 
              Thread.sleep(100L);
            } 
            if (this.val$status.containsKey("error"))
              throw (Exception)this.val$status.get("error"); 
            this.val$sock2.close();
          } catch (Exception e) {
            this.val$status.put("error", e);
            this.this$0.config.put("error", e);
            this.this$0.config.put("error_msg", (new StringBuffer(String.valueOf(e.getMessage()))).toString());
            Common.log("S3_CLIENT", 1, e);
            try {
              this.this$0.close();
              this.val$sock2.close();
              this.val$sock1.close();
            } catch (Exception exception) {}
          } finally {
            synchronized (this.this$0.in_progress) {
              this.this$0.in_progress.remove(Thread.currentThread());
            } 
            this.val$chunks_needed.removeAllElements();
            if (this.val$status.containsKey("error"))
              try {
                Thread.sleep(5000L);
              } catch (InterruptedException interruptedException) {} 
            Enumeration keys = this.val$chunks.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              synchronized (S3CrushClient.ram_lock) {
                byte[] b = (byte[])this.val$chunks.remove(key);
                S3CrushClient.ram_used_download -= b.length;
                this.val$status.put("ram", (new StringBuffer(String.valueOf(Long.parseLong(this.val$status.getProperty("ram")) - b.length))).toString());
                String s = Thread.currentThread().getName();
                s = String.valueOf(s.substring(0, s.lastIndexOf(":") + 1)) + Common.format_bytes_short(Long.parseLong(this.val$status.getProperty("ram"))) + " of " + Common.format_bytes_short(S3CrushClient.ram_used_download);
                Thread.currentThread().setName(s);
              } 
            } 
          } 
        }
      };
    if (this.config.getProperty("multithreaded_s3", "true").equals("true"))
      for (int x = 0; x < Integer.parseInt(this.config.getProperty("s3_threads_download", "3")); x++)
        Worker.startWorker(grabChunk, "S3 chunked file downloader:" + (x + 1) + "/" + this.config.getProperty("s3_threads_download", "3") + ":" + path0 + ":");  
    Worker.startWorker(downloadChunks, "S3 buffer processor:" + path0 + ":");
    this.in = sock1.getInputStream();
    return this.in;
  }
  
  protected OutputStream upload3(String path0, long startPos0, boolean truncate, boolean binary) throws Exception {
    String original_path = path0;
    path0 = getPath(path0);
    String path_f = path0;
    StringBuffer uid = new StringBuffer(String.valueOf(path0.substring(1).replace(' ', '+').replace('%', '_')) + (this.config.getProperty("random_id", "true").equals("true") ? ("/" + Common.makeBoundary(10)) : ""));
    Properties stat_existing = new Properties();
    Vector segments = new Vector();
    Vector tempResumeParts = null;
    StringBuffer partNumber = new StringBuffer("1");
    StringBuffer partNumberPos = new StringBuffer("1");
    String tempUploadId = null;
    boolean needCopyResume = false;
    ByteArrayOutputStream temp_buf = new ByteArrayOutputStream();
    Properties p_upload = new Properties();
    if (startPos0 > 0L) {
      stat_existing = stat(original_path);
      if (stat_existing.containsKey("segments")) {
        uid.setLength(0);
        uid.append(stat_existing.getProperty("uid"));
        segments.addAll((Vector)stat_existing.get("segments"));
        partNumber.setLength(0);
        partNumber.append(Integer.parseInt(segments.elementAt(segments.size() - 1).toString().split(":")[0]) + 1);
        p_upload.put("segments", segments);
      } else if (Long.parseLong(stat_existing.getProperty("size")) < 5242880L) {
        Common.streamCopier(download3(original_path, -1L, -1L, true), temp_buf, false, true, false);
        deleteAws(stat_existing.getProperty("uid"));
        startPos0 = 0L;
      } else {
        needCopyResume = true;
      } 
    } else {
      delete(original_path);
    } 
    long startPos = startPos0;
    p_upload.put("size", (new StringBuffer(String.valueOf(startPos))).toString());
    p_upload.put("uid", uid.toString());
    writeFs(this.s3_root, this.bucketName0, this.replicating, path0, p_upload);
    if (this.config.getProperty("segmented", "false").equals("false")) {
      URLConnection urlc = URLConnection.openConnection(new VRL("https://" + this.bucketName0 + "." + this.region_host + "/" + uid + "?uploads"), this.config);
      urlc.setRequestMethod("POST");
      if (this.config.getProperty("server_side_encrypt", "false").equals("true"))
        urlc.setRequestProperty("x-amz-server-side-encryption", "AES256"); 
      urlc.setDoOutput(false);
      doStandardAmazonAlterations(urlc, (String)null, this.bucketName0);
      int code = urlc.getResponseCode();
      String result = URLConnection.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      if (code < 200 || code > 299) {
        log(String.valueOf(result) + "\r\n");
        throw new IOException(result);
      } 
      Element root = (new SAXBuilder()).build(new ByteArrayInputStream(result.getBytes("UTF8"))).getRootElement();
      tempUploadId = getKeyText(root, "UploadId");
    } else {
      p_upload.put("segments", segments);
    } 
    tempResumeParts = new Vector();
    Vector resumePartsDone = new Vector();
    Vector resumeParts = tempResumeParts;
    String uploadId = tempUploadId;
    long maxBuf = Long.parseLong(this.config.getProperty("s3_buffer", "5"));
    if (maxBuf < 5L)
      maxBuf = 5L; 
    long maxBufferSize = maxBuf * 1024L * 1024L;
    if (needCopyResume) {
      doCopyResume(tempResumeParts, resumePartsDone, partNumber, partNumberPos, tempUploadId, "/" + this.bucketName0 + "/" + stat_existing.getProperty("uid"), "/" + this.bucketName0 + "/" + uid, startPos);
      deleteAws(stat_existing.getProperty("uid"));
    } 
    this.uploading = true;
    this.out = new null.OutputWrapper(this, startPos, p_upload, maxBufferSize, partNumber, partNumberPos, resumeParts, resumePartsDone, segments, uploadId, uid, path_f);
    if (temp_buf.size() > 0)
      this.out.write(temp_buf.toByteArray()); 
    return this.out;
  }
  
  public boolean setSize(String path0, long size) throws Exception {
    Properties stat0 = stat(path0);
    if (stat0 == null)
      return false; 
    if (stat0.getProperty("type", "").equalsIgnoreCase("DIR"))
      return false; 
    path0 = getPath(path0);
    Properties p_upload = new Properties();
    p_upload.put("uid", stat0.getProperty("uid"));
    p_upload.put("size", (new StringBuffer(String.valueOf(size))).toString());
    try {
      writeFs(this.s3_root, this.bucketName0, this.replicating, path0, p_upload);
      return true;
    } catch (Exception e) {
      Common.log("S3_CLIENT", 0, e);
      return false;
    } 
  }
  
  public boolean delete(String path0) throws Exception {
    Properties stat0 = stat(path0);
    if (stat0 == null)
      return false; 
    path0 = getPath(path0);
    Vector items = new Vector();
    if ((new File(String.valueOf(this.s3_root) + this.bucketName0 + path0)).isDirectory()) {
      Vector items2 = new Vector();
      Common.appendListing(String.valueOf(this.s3_root) + this.bucketName0 + path0, items2, "", 3, false);
      if (items2.size() > 1)
        return false; 
      items = items2;
    } else {
      items.addElement(new File(String.valueOf(this.s3_root) + this.bucketName0 + path0));
    } 
    for (int x = items.size() - 1; x >= 0; x--) {
      File f = items.elementAt(x);
      if (!f.getName().equals(".DS_Store") && 
        !f.isDirectory()) {
        Properties p = (Properties)Common.readXMLObject(f);
        if (p.containsKey("segments")) {
          Vector threads = new Vector();
          Vector errors = new Vector();
          Vector segments = (Vector)p.get("segments");
          for (int xx = 0; xx < segments.size(); xx++) {
            String delete_path = String.valueOf(p.getProperty("uid")) + "_" + segments.elementAt(xx).toString().split(":")[0];
            threads.addElement(delete_path);
            while (threads.size() > 20)
              Thread.sleep(100L); 
            Worker.startWorker(new Runnable(this, delete_path, errors, threads) {
                  final S3CrushClient this$0;
                  
                  private final String val$delete_path;
                  
                  private final Vector val$errors;
                  
                  private final Vector val$threads;
                  
                  public void run() {
                    boolean ok2 = true;
                    try {
                      for (int loop = 0; loop < 10; loop++) {
                        ok2 = true;
                        if (this.this$0.deleteAws(this.val$delete_path))
                          break; 
                        ok2 = false;
                      } 
                    } catch (Exception e) {
                      Common.log("S3_CLIENT", 1, e);
                      ok2 = false;
                    } 
                    if (!ok2)
                      this.val$errors.addElement(this.val$delete_path); 
                    this.val$threads.remove(this.val$delete_path);
                  }
                });
          } 
          for (int loops = 0; loops < 6000 && threads.size() > 0; loops++)
            Thread.sleep(100L); 
          if (errors.size() > 0) {
            log("Failed to delete:" + errors);
            return false;
          } 
          f.delete();
        } else {
          boolean ok2 = true;
          try {
            for (int loop = 0; loop < 10; loop++) {
              ok2 = true;
              if (deleteAws(p.getProperty("uid"))) {
                f.delete();
                break;
              } 
              ok2 = false;
            } 
          } catch (Exception e) {
            Common.log("S3_CLIENT", 1, e);
            ok2 = false;
          } 
          if (!ok2)
            throw new Exception("Can't delete:" + p.getProperty("uid")); 
        } 
      } 
    } 
    Common.recurseDelete(String.valueOf(this.s3_root) + this.bucketName0 + path0, false);
    return true;
  }
  
  private boolean deleteAws(String uid) throws Exception {
    URLConnection urlc = doAction("DELETE", "/" + this.bucketName0 + "/" + uid, (StringBuffer)null, false);
    int code = urlc.getResponseCode();
    String result = URLConnection.consumeResponse(urlc.getInputStream());
    urlc.disconnect();
    if (code == 403 && this.config.getProperty("delete_403", "false").equalsIgnoreCase("true"))
      return true; 
    if ((code < 200 || code > 299) && code != 404) {
      log(String.valueOf(code) + ":" + result + "\r\n");
      return false;
    } 
    return true;
  }
  
  public boolean makedir(String path0) throws Exception {
    path0 = getPath(path0);
    if (!path0.endsWith("/"))
      path0 = String.valueOf(path0) + "/"; 
    if (path0.equals("/"))
      return true; 
    return (new File(String.valueOf(this.s3_root) + this.bucketName0 + path0)).mkdirs();
  }
  
  public boolean makedirs(String path0) throws Exception {
    return makedir(path0);
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    rnfr = getPath(rnfr);
    rnto = getPath(rnto);
    return (new File(String.valueOf(this.s3_root) + this.bucketName0 + rnfr)).renameTo(new File(String.valueOf(this.s3_root) + this.bucketName0 + rnto));
  }
  
  public Properties stat(String path0) throws Exception {
    if (path0.endsWith(":filetree"))
      path0 = path0.substring(0, path0.indexOf(":filetree") - 1); 
    path0 = getPath(path0);
    File f = new File(String.valueOf(this.s3_root) + this.bucketName0 + Common.dots(path0));
    if (!f.exists())
      return null; 
    if (f.isDirectory()) {
      Date date = new Date(f.lastModified());
      String str = "drwxrwxrwx   1    owner   group   0   " + this.yyyyMMddHHmmss.format(date) + "   " + this.dd.format(date) + " " + this.yyyy.format(date) + " /" + f.getName();
      Properties properties = parseStat(str);
      properties.put("url", String.valueOf(this.url) + this.bucketName0 + path0 + ((properties.getProperty("type").equalsIgnoreCase("DIR") && !path0.endsWith("/")) ? "/" : ""));
      properties.put("modified", (new StringBuffer(String.valueOf(date.getTime()))).toString());
      return properties;
    } 
    Properties p = (Properties)Common.readXMLObject(f);
    Date d = new Date(f.lastModified());
    String line = "-rwxrwxrwx   1    owner   group   " + p.getProperty("size", "0") + "   " + this.yyyyMMddHHmmss.format(d) + "   " + this.dd.format(d) + " " + this.yyyy.format(d) + " /" + f.getName();
    Properties stat = parseStat(line);
    stat.put("url", String.valueOf(this.url) + this.bucketName0 + path0);
    if (p.containsKey("segments"))
      stat.put("segments", p.get("segments")); 
    stat.put("uid", p.getProperty("uid"));
    stat.put("modified", (new StringBuffer(String.valueOf(d.getTime()))).toString());
    return stat;
  }
  
  public boolean mdtm(String path0, long modified) throws Exception {
    path0 = getPath(path0);
    return (new File(String.valueOf(this.s3_root) + this.bucketName0 + path0)).setLastModified(modified);
  }
}
