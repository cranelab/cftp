package com.crushftp.tunnel3;

import com.crushftp.client.Common;
import com.crushftp.client.File_S;
import com.crushftp.client.HttpURLConnection;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import com.crushftp.tunnel2.Tunnel2;
import com.crushtunnel.gui.GUIFrame;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class StreamController implements Runnable {
  static String version = "3.4.1";
  
  public Properties tunnel = null;
  
  String auth = null;
  
  VRL vrl = null;
  
  Vector log = null;
  
  public Vector incoming = new Vector();
  
  public Vector outgoing = new Vector();
  
  boolean ready = false;
  
  public Properties in_queues = new Properties();
  
  public Properties bad_queues = new Properties();
  
  public Properties out_queues = new Properties();
  
  Enumeration out_queue_enum = this.out_queues.keys();
  
  public Vector out_queue_commands = new Vector();
  
  public Object out_queue_remove = new Object();
  
  StreamController sc = this;
  
  public static Object ram_lock2 = new Object();
  
  public static Object bytes_lock2 = new Object();
  
  public Properties streams = new Properties();
  
  public Properties localCache = new Properties();
  
  public Object command_num_lock = new Object();
  
  public int command_num = -1;
  
  public Properties closeRequests = new Properties();
  
  public StreamTuner st = new StreamTuner(this);
  
  public long lastActivity = System.currentTimeMillis();
  
  boolean allowReverseMode = false;
  
  boolean active = true;
  
  long last_cache_ram = 0L;
  
  String username = "";
  
  String password = "";
  
  String clientid = "CrushTunnel";
  
  public Properties stats = new Properties();
  
  public static Properties memory = new Properties();
  
  static long total_ram_used = 0L;
  
  public static long ram_max_total = (1048576 * Integer.parseInt(System.getProperty("crushftp.tunnel_ram_cache", "128")));
  
  Properties last_bytes_sent = new Properties();
  
  Properties last_bytes_sent_time = new Properties();
  
  public static long last_version_check = 0L;
  
  public static Object version_check_lock = new Object();
  
  public static String old_msg = null;
  
  boolean reset_wanted = false;
  
  Properties out_binds;
  
  Properties in_binds;
  
  public Object bind_lock;
  
  RandomAccessFile tunnel_log;
  
  public Object log_lock;
  
  public static long addRam(int id, long amount) {
    synchronized (ram_lock2) {
      amount += Long.parseLong(memory.getProperty((new StringBuffer(String.valueOf(id))).toString(), "0"));
      memory.put((new StringBuffer(String.valueOf(id))).toString(), (new StringBuffer(String.valueOf(amount))).toString());
      return amount;
    } 
  }
  
  public static long getRam(int id) {
    return Long.parseLong(memory.getProperty((new StringBuffer(String.valueOf(id))).toString(), "0"));
  }
  
  public static void addRamAllocated(long amount) {
    synchronized (ram_lock2) {
      total_ram_used += amount;
    } 
  }
  
  public static void main(String[] args) {
    try {
      System.out.println("C " + version + " Initialized. " + Common.format_bytes_short(ram_max_total));
      Properties p = new Properties();
      for (int x = 0; x < args.length; x++) {
        String[] s = args[x].split(";");
        for (int xx = 0; xx < s.length; xx++) {
          String key = s[xx].split("=")[0].trim();
          String val = "";
          try {
            val = s[xx].split("=")[1].trim();
          } catch (Exception exception) {}
          while (key.startsWith("-"))
            key = key.substring(1); 
          p.put(key.toUpperCase(), val);
        } 
      } 
      if (p.getProperty("DUMPMD5", "").equals("true")) {
        Tunnel2.dumpMD5s(p);
        return;
      } 
      if ((new File(p.getProperty("PASSWORD"))).exists() && (new File(p.getProperty("PASSWORD"))).length() < 100L) {
        RandomAccessFile in = new RandomAccessFile(new File_S(p.getProperty("PASSWORD")), "r");
        byte[] b = new byte[(int)in.length()];
        in.readFully(b);
        in.close();
        p.put("PASSWORD", (new String(b, "UTF8")).trim());
      } 
      Common.trustEverything();
      StreamController sc = new StreamController(String.valueOf(p.getProperty("PROTOCOL")) + "://" + p.getProperty("HOST") + ":" + p.getProperty("PORT") + "/", p.getProperty("USERNAME"), p.getProperty("PASSWORD"), null);
      sc.startThreads();
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
  
  public void setLog(Vector log2) {
    this.log = log2;
  }
  
  public void startThreads() throws Exception {
    if (this.auth == null)
      this.auth = Common.login(this.vrl.toString(), this.username, this.password, this.clientid); 
    if (this.tunnel == null)
      this.sc.setTunnel(getTunnelItem(null)); 
    if (this.tunnel == null)
      throw new Exception("Can't start tunnel, server returned blank tunnel configuration:" + this.tunnel); 
    Worker.startWorker(this);
    Worker.startWorker(new Runnable(this) {
          final StreamController this$0;
          
          public void run() {
            this.this$0.sc.startSocket();
          }
        });
    for (int x = 0; x < 100; x++) {
      if (this.ready)
        break; 
      Thread.sleep(100L);
    } 
    Thread.sleep(100L);
  }
  
  public void startReverseThreads() throws Exception {
    this.allowReverseMode = true;
    Worker.startWorker(new Runnable(this) {
          final StreamController this$0;
          
          public void run() {
            this.this$0.sc.startSocket();
          }
        });
    for (int x = 0; x < 100; x++) {
      Thread.sleep(110L);
      if (this.ready)
        break; 
    } 
  }
  
  public void setTunnel(Properties tunnel) {
    this.tunnel = tunnel;
  }
  
  public void setAuth(String auth) {
    this.auth = auth;
  }
  
  public void run() {
    try {
      Worker.startWorker(this.st);
      startStopTunnel(true);
      this.ready = true;
      while (isActive()) {
        try {
          checkAckLoop();
        } catch (NullPointerException nullPointerException) {}
      } 
    } catch (Exception e) {
      msg(e);
    } 
  }
  
  public void reset() {
    // Byte code:
    //   0: aload_0
    //   1: iconst_0
    //   2: invokevirtual startStopTunnel : (Z)Ljava/lang/String;
    //   5: pop
    //   6: goto -> 24
    //   9: astore_1
    //   10: aload_0
    //   11: aload_1
    //   12: invokevirtual msg : (Ljava/lang/Exception;)V
    //   15: goto -> 24
    //   18: ldc2_w 100
    //   21: invokestatic sleep : (J)V
    //   24: aload_0
    //   25: getfield st : Lcom/crushftp/tunnel3/StreamTuner;
    //   28: invokevirtual isActive : ()Z
    //   31: ifne -> 18
    //   34: aload_0
    //   35: aconst_null
    //   36: putfield auth : Ljava/lang/String;
    //   39: aload_0
    //   40: aconst_null
    //   41: putfield tunnel : Ljava/util/Properties;
    //   44: aload_0
    //   45: iconst_1
    //   46: putfield active : Z
    //   49: iconst_0
    //   50: istore_1
    //   51: goto -> 76
    //   54: aload_0
    //   55: invokevirtual startThreads : ()V
    //   58: goto -> 101
    //   61: astore_2
    //   62: aload_0
    //   63: aload_2
    //   64: invokevirtual msg : (Ljava/lang/Exception;)V
    //   67: ldc2_w 1000
    //   70: invokestatic sleep : (J)V
    //   73: iinc #1, 1
    //   76: iload_1
    //   77: ldc_w 'crushtunnel.reset_retries'
    //   80: ldc_w '345600'
    //   83: invokestatic getProperty : (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    //   86: invokestatic parseInt : (Ljava/lang/String;)I
    //   89: if_icmplt -> 54
    //   92: goto -> 101
    //   95: astore_1
    //   96: aload_0
    //   97: aload_1
    //   98: invokevirtual msg : (Ljava/lang/Exception;)V
    //   101: return
    // Line number table:
    //   Java source line number -> byte code offset
    //   #240	-> 0
    //   #242	-> 9
    //   #244	-> 10
    //   #248	-> 15
    //   #249	-> 18
    //   #248	-> 24
    //   #250	-> 34
    //   #251	-> 39
    //   #252	-> 44
    //   #253	-> 49
    //   #257	-> 54
    //   #258	-> 58
    //   #260	-> 61
    //   #262	-> 62
    //   #263	-> 67
    //   #253	-> 73
    //   #271	-> 95
    //   #273	-> 96
    //   #275	-> 101
    // Local variable table:
    //   start	length	slot	name	descriptor
    //   0	102	0	this	Lcom/crushftp/tunnel3/StreamController;
    //   10	5	1	e	Ljava/lang/Exception;
    //   51	41	1	x	I
    //   62	11	2	e	Ljava/lang/Exception;
    //   96	5	1	e	Ljava/lang/Exception;
    // Exception table:
    //   from	to	target	type
    //   0	6	9	java/lang/Exception
    //   15	92	95	java/lang/Exception
    //   54	58	61	java/lang/Exception
  }
  
  public void addBytes(int id, int bytes) {
    synchronized (bytes_lock2) {
      long l = Long.parseLong(this.last_bytes_sent.getProperty((new StringBuffer(String.valueOf(id))).toString(), "0"));
      l += bytes;
      this.last_bytes_sent.put((new StringBuffer(String.valueOf(id))).toString(), (new StringBuffer(String.valueOf(l))).toString());
      if (!this.last_bytes_sent_time.containsKey((new StringBuffer(String.valueOf(id))).toString()))
        this.last_bytes_sent_time.put((new StringBuffer(String.valueOf(id))).toString(), (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()); 
    } 
  }
  
  public int getSpeedAndReset(int id) {
    synchronized (bytes_lock2) {
      long secs = System.currentTimeMillis() - Long.parseLong(this.last_bytes_sent_time.getProperty((new StringBuffer(String.valueOf(id))).toString(), (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()));
      secs /= 1000L;
      this.last_bytes_sent_time.put((new StringBuffer(String.valueOf(id))).toString(), (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      if (secs == 0L)
        secs = 1L; 
      long speed = Long.parseLong(this.last_bytes_sent.getProperty((new StringBuffer(String.valueOf(id))).toString(), "0")) / secs;
      this.last_bytes_sent.put((new StringBuffer(String.valueOf(id))).toString(), "0");
      return (int)speed;
    } 
  }
  
  public void checkAckLoop() throws Exception {
    Thread.currentThread().setName("Tunnel3 ACK Thread:" + this.localCache.size());
    if (this.reset_wanted) {
      this.reset_wanted = false;
      reset();
      this.reset_wanted = false;
    } 
    int loop_delay = 1000;
    int timeout = Integer.parseInt(this.tunnel.getProperty("ackTimeout", "30")) * 1000;
    Thread.sleep(loop_delay);
    Enumeration keys = this.localCache.keys();
    int min_num = 0;
    int max_num = 0;
    Properties priority_list_data = new Properties();
    Vector pending_commands = new Vector();
    long ram_used = 0L;
    int normal_chunks = 0;
    int command_chunks = 0;
    Properties writers = new Properties();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      Chunk c = (Chunk)this.localCache.get(key);
      if (c.len > 0)
        ram_used += c.len; 
      if (c.isCommand()) {
        command_chunks++;
      } else {
        normal_chunks++;
      } 
      if (c.sw != null && c.sw.urlc != null) {
        String s = (String)writers.get(c.sw.urlc.getBindIp());
        if (s == null)
          s = "0"; 
        int count = Integer.parseInt(s);
        count++;
        writers.put(c.sw.urlc.getBindIp(), (new StringBuffer(String.valueOf(count))).toString());
      } 
      if (c != null && System.currentTimeMillis() - c.time > timeout) {
        msg("#####################################################NO ACK after " + timeout + "ms, RESENDING:" + c + "#####################################################");
        if (c.num < 0) {
          if (c.getCommand().startsWith("PING")) {
            this.localCache.remove(key);
            continue;
          } 
          if (c.getCommand().startsWith("VERSION")) {
            this.localCache.remove(key);
            continue;
          } 
          pending_commands.addElement(c);
          continue;
        } 
        if (c.num < min_num)
          min_num = c.num; 
        if (c.num > max_num)
          max_num = c.num; 
        priority_list_data.put((new StringBuffer(String.valueOf(c.num))).toString(), c);
      } 
    } 
    Vector v = getQueue("unknown");
    while (v.size() > 1000)
      v.remove(0); 
    this.last_cache_ram = ram_used;
    msg("Ram currently used:" + Common.format_bytes_short(ram_used) + " for " + command_chunks + " commands and " + normal_chunks + " data chunks. Writers:" + writers + " in:" + this.sc.incoming.size() + " out:" + this.sc.outgoing.size());
    for (int x = max_num; x >= min_num; x--) {
      Chunk c = (Chunk)priority_list_data.get((new StringBuffer(String.valueOf(x))).toString());
      if (c != null) {
        c.time = System.currentTimeMillis();
        getQueue((new StringBuffer(String.valueOf(c.id))).toString()).insertElementAt(c, 0);
      } 
    } 
    while (pending_commands.size() > 0) {
      Chunk c = pending_commands.remove(0);
      c.time = System.currentTimeMillis();
      this.sc.out_queue_commands.insertElementAt(c, 0);
    } 
    synchronized (version_check_lock) {
      if (System.currentTimeMillis() - last_version_check > 60000L) {
        last_version_check = System.currentTimeMillis();
        this.sc.out_queue_commands.addElement(this.sc.makeCommand(0, "VERSION_CHECK:" + version));
      } 
    } 
  }
  
  public Vector getQueue(String id) {
    Vector q = (Vector)this.out_queues.get((new StringBuffer(String.valueOf(id))).toString());
    if (q == null)
      q = (Vector)this.out_queues.get("unknown"); 
    return q;
  }
  
  public int getQueueCount() {
    int total = 0;
    Enumeration keys = this.out_queues.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      Vector q = (Vector)this.out_queues.get(key);
      total += q.size();
    } 
    return total;
  }
  
  public Chunk popOut() {
    int enum_loops = 0;
    Chunk c = null;
    synchronized (this.out_queue_remove) {
      while (c == null && enum_loops++ < 2) {
        if (!this.out_queue_enum.hasMoreElements())
          this.out_queue_enum = this.out_queues.keys(); 
        while (c == null) {
          if (this.out_queue_enum.hasMoreElements()) {
            String key = this.out_queue_enum.nextElement().toString();
            Vector q = (Vector)this.out_queues.get(key);
            if (q != null && q.size() > 0)
              c = q.remove(0); 
            continue;
          } 
          break;
        } 
      } 
    } 
    return c;
  }
  
  public void startServerTunnel() throws IOException {
    Worker.startWorker(new Runnable(this) {
          final StreamController this$0;
          
          public void run() {
            try {
              while (this.this$0.isActive())
                this.this$0.checkAckLoop(); 
            } catch (Exception exception) {}
          }
        });
  }
  
  public StreamController(Properties tunnel) {
    this.out_binds = new Properties();
    this.in_binds = new Properties();
    this.bind_lock = new Object();
    this.tunnel_log = null;
    this.log_lock = new Object();
    this.tunnel = tunnel;
    synchronized (this.out_queues) {
      if (!this.out_queues.containsKey("unknown"))
        this.out_queues.put("unknown", new Vector()); 
    } 
  }
  
  public StreamController(String url, String username, String password, String clientid) {
    this.out_binds = new Properties();
    this.in_binds = new Properties();
    this.bind_lock = new Object();
    this.tunnel_log = null;
    this.log_lock = new Object();
    synchronized (this.out_queues) {
      if (!this.out_queues.containsKey("unknown"))
        this.out_queues.put("unknown", new Vector()); 
    } 
    this.vrl = new VRL(url);
    this.username = username;
    this.password = password;
    this.clientid = clientid;
  }
  
  public HttpURLConnection addTransport(boolean write, String channel_id) {
    try {
      String bind_ip = null;
      VRL vrl_tmp = this.vrl;
      if (!System.getProperty("crushftp.bind_ips", "").equals("")) {
        String ips = System.getProperty("crushftp.bind_ips", "");
        String ip_load_write = System.getProperty("crushftp.bind_ips_out", "");
        String ip_load_read = System.getProperty("crushftp.bind_ips_in", "");
        synchronized (this.bind_lock) {
          String lowest_ip = "0.0.0.0:100";
          Properties load = new Properties();
          for (int x = 0; x < (ips.split(",")).length; x++) {
            String ip = ips.split(",")[x].trim();
            float l = 0.0F;
            if (x < ((write ? ip_load_write : ip_load_read).split(",")).length)
              l = Integer.parseInt((write ? ip_load_write : ip_load_read).split(",")[x].trim()); 
            float count = Integer.parseInt((write ? this.out_binds : this.in_binds).getProperty(ip, "0"));
            float ip_load = count / l;
            load.put(ip, (new StringBuffer(String.valueOf(ip_load))).toString());
            if (ip_load < Float.parseFloat(lowest_ip.split(":")[1]))
              lowest_ip = String.valueOf(ip) + ":" + ip_load; 
          } 
          bind_ip = lowest_ip.split(":")[0];
          (write ? this.out_binds : this.in_binds).put(lowest_ip.split(":")[0], (new StringBuffer(String.valueOf(Integer.parseInt((write ? this.out_binds : this.in_binds).getProperty(bind_ip, "0")) + 1))).toString());
          vrl_tmp = new VRL(Common.replace_str((String)this.vrl, this.vrl.getHost(), String.valueOf(bind_ip) + "~" + this.vrl.getHost()));
        } 
        msg("in:" + this.in_binds + "out:" + this.out_binds);
      } 
      VRL u = new VRL(vrl_tmp + "CRUSH_STREAMING_HTTP_PROXY3/?writing=" + write + "&channel_id=" + channel_id + "&tunnelId=" + this.tunnel.getProperty("tunnelId"));
      HttpURLConnection urlc = (HttpURLConnection)u.openConnection();
      urlc.setBindIp(bind_ip);
      urlc.setRequestMethod("POST");
      urlc.setRequestProperty("Cookie", "CrushAuth=" + this.auth + ";");
      urlc.setUseCaches(false);
      urlc.setDoOutput(write);
      if (write)
        urlc.setUseChunkedStreaming(true); 
      msg("Tunnel3:getSendGet:urlc:" + urlc);
      return urlc;
    } catch (Exception e) {
      msg(e);
      return null;
    } 
  }
  
  public void startSocket() {
    String bindip = this.tunnel.getProperty("bindIp", "0.0.0.0");
    int bindport = Integer.parseInt(this.tunnel.getProperty("localPort", "0"));
    ServerSocket ss1 = null;
    ServerSocket ss2 = null;
    Thread.currentThread().setName("Tunnel:ConnectionHandler:bindip:" + bindip + ":" + bindport);
    try {
      if (this.allowReverseMode)
        this.ready = true; 
      while (!this.ready)
        Thread.sleep(100L); 
      if (this.tunnel.getProperty("reverse", "false").equals("true") && !this.allowReverseMode)
        startStopTunnel(true); 
      ServerSocket ss0 = null;
      int sock_num = 0;
      while (isActive()) {
        if (this.tunnel.getProperty("reverse", "false").equals("false") || this.allowReverseMode) {
          try {
            if (ss1 == null) {
              ss1 = new ServerSocket(bindport, 1000, InetAddress.getByName(bindip));
              msg("Tunnel3:ConnectionHandler:bound port:" + bindport);
              ss1.setSoTimeout(100);
              if (!this.allowReverseMode)
                startStopTunnel(true); 
              msg("Tunnel3:ConnectionHandler:tunnel started.");
            } 
            if ((new StringBuffer(String.valueOf(bindport))).toString().startsWith("444") && ss2 == null) {
              ss2 = new ServerSocket(bindport + 10, 1000, InetAddress.getByName(bindip));
              msg("Tunnel3:ConnectionHandler:bound port:" + (bindport + 10));
              ss2.setSoTimeout(100);
            } 
            sock_num++;
            if (sock_num == 1) {
              ss0 = ss1;
            } else {
              ss0 = ss2;
              sock_num = 0;
            } 
            if (ss0 == null)
              ss0 = ss1; 
            Socket proxy = ss0.accept();
            msg("Tunnel3:ConnectionHandler:received connection:" + proxy);
            Socket control = proxy;
            boolean ftp = (new StringBuffer(String.valueOf(bindport))).toString().endsWith("21");
            try {
              if ((new StringBuffer(String.valueOf(bindport))).toString().startsWith("444")) {
                if (ss0.getLocalPort() == ss2.getLocalPort())
                  ftp = true; 
                if (ftp) {
                  this.tunnel.put("destPort", "55521");
                } else {
                  this.tunnel.put("destPort", "55580");
                } 
              } else if (bindport == 55555 || this.tunnel.getProperty("destPort").equals("55555") || this.tunnel.getProperty("destPort").equals("55580") || this.tunnel.getProperty("destPort").equals("55521") || this.tunnel.getProperty("destPort").equals("0")) {
                ftp = true;
                for (int x = 0; x < 50 && ftp; x++) {
                  if (proxy.getInputStream().available() > 0)
                    ftp = false; 
                  if (ftp)
                    Thread.sleep(10L); 
                } 
                if (ftp) {
                  this.tunnel.put("destPort", "55521");
                } else {
                  this.tunnel.put("destPort", "55580");
                } 
              } 
            } catch (Exception e) {
              msg(e);
              try {
                proxy.close();
              } catch (Exception ee) {
                Common.log("TUNNEL", 1, ee);
              } 
            } 
            msg("Tunnel3:ConnectionHandler:ftp=" + ftp);
            if (ftp) {
              ServerSocket ssProxyControl = new ServerSocket(0);
              int localPort = ssProxyControl.getLocalPort();
              control = new Socket("127.0.0.1", localPort);
              int stream_id = process(ssProxyControl.accept(), this.tunnel.getProperty("destIp"), Integer.parseInt(this.tunnel.getProperty("destPort")), 0, 0);
              ssProxyControl.close();
              StreamFTP ftpp = new StreamFTP(this.sc, stream_id);
              ftpp.proxyNATs(control, proxy);
              msg("Tunnel3:Started FTP NAT:control=" + control + " proxy=" + proxy);
            } else {
              process(proxy, this.tunnel.getProperty("destIp"), Integer.parseInt(this.tunnel.getProperty("destPort")), 0, 0);
            } 
          } catch (SocketTimeoutException socketTimeoutException) {
          
          } catch (Exception e) {
            msg(e);
            try {
              Thread.sleep(1000L);
            } catch (Exception ee) {
              Common.log("TUNNEL", 1, ee);
            } 
          } 
          if (this.allowReverseMode)
            if (System.currentTimeMillis() - this.lastActivity > 30000L) {
              msg("Tunnel is apparently inactive..." + (System.currentTimeMillis() - this.lastActivity));
              startStopTunnel(false);
            }  
          continue;
        } 
        Thread.sleep(1000L);
      } 
      if (ss1 != null)
        ss1.close(); 
      if (ss2 != null)
        ss2.close(); 
    } catch (Exception e) {
      Common.log("TUNNEL", 1, e);
    } finally {
      try {
        if (ss1 != null)
          ss1.close(); 
      } catch (Exception exception) {}
      try {
        if (ss2 != null)
          ss2.close(); 
      } catch (Exception exception) {}
    } 
    try {
      startStopTunnel(false);
    } catch (Exception e) {
      Common.log("TUNNEL", 1, e);
    } 
  }
  
  public boolean processCommand(Chunk c, Stream st) {
    String command = c.getCommand();
    if (!command.startsWith("PING"))
      this.lastActivity = System.currentTimeMillis(); 
    if (command.startsWith("A:")) {
      if (command.startsWith("A:M:")) {
        Enumeration keys = this.localCache.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          if (key.startsWith(String.valueOf(c.id) + ":") && Integer.parseInt(key.split(":")[1]) < Integer.parseInt(command.split(":")[2])) {
            Chunk c2 = (Chunk)this.localCache.remove(key);
            if (c2 != null)
              addRam(c2.id, (c2.len * -1)); 
          } 
        } 
      } else {
        Chunk c2 = (Chunk)this.localCache.remove(String.valueOf(c.id) + ":" + command.split(":")[1]);
        if (c2 != null) {
          addRam(c2.id, (c2.len * -1));
          if (c2.getCommand().startsWith("PINGSEND:"))
            msg("Latency one way:" + (System.currentTimeMillis() - Long.parseLong(c2.getCommand().split(":")[1])) + "ms"); 
        } 
      } 
      return false;
    } 
    if (!c.getCommand().startsWith("PING"))
      msg("RECEIVED COMMAND:" + c); 
    boolean close_stream = false;
    if (command.startsWith("END:")) {
      if (st != null)
        st.last_num = Integer.parseInt(command.split(":")[1]); 
    } else if (command.startsWith("KILL:")) {
      try {
        if (st != null)
          st.kill(); 
      } catch (IOException e) {
        msg(e);
      } 
    } else if (command.startsWith("RESET:")) {
      this.reset_wanted = true;
    } else if (command.startsWith("CLOSE:")) {
      close_stream = true;
    } else if (command.startsWith("VERSION_OLD:")) {
      old_msg = "****************Tunnel client is too old.  You must update your client.  " + version + " less than " + command.split(":")[1] + "****************";
      if (GUIFrame.thisObj != null) {
        old_msg = "Tunnel client is too old.  You must restart your client.  " + version + " less than " + command.split(":")[1];
        GUIFrame.thisObj.showMessage(old_msg);
        try {
          Worker.startWorker(new Runnable(this) {
                final StreamController this$0;
                
                public void run() {
                  while (true) {
                    try {
                      Thread.sleep(20000L);
                    } catch (InterruptedException interruptedException) {}
                    GUIFrame.thisObj.showMessage(StreamController.old_msg);
                  } 
                }
              });
        } catch (Exception e) {
          msg(e);
        } 
      } else {
        try {
          Worker.startWorker(new Runnable(this) {
                final StreamController this$0;
                
                public void run() {
                  for (int x = 0; x < 100; x++) {
                    this.this$0.msg(StreamController.old_msg);
                    try {
                      Thread.sleep(1000L);
                    } catch (InterruptedException interruptedException) {}
                  } 
                }
              });
        } catch (Exception e) {
          msg(e);
        } 
      } 
      try {
        this.sc.startStopTunnel(false);
      } catch (Exception e) {
        msg(e);
      } 
    } else if (command.startsWith("CONNECT:")) {
      try {
        Worker.startWorker(new Runnable(this, command, c) {
              final StreamController this$0;
              
              private final String val$command;
              
              private final Chunk val$c;
              
              public void run() {
                try {
                  Socket sock = new Socket(this.val$command.split(":")[1], Integer.parseInt(this.val$command.split(":")[2]));
                  Properties queue = new Properties();
                  this.this$0.in_queues.put((new StringBuffer(String.valueOf(this.val$c.id))).toString(), queue);
                  int parent_stream_id = 0;
                  if ((this.val$command.split(":")).length > 3)
                    parent_stream_id = Integer.parseInt(this.val$command.split(":")[3]); 
                  Stream st = new Stream(sock, this.val$c.id, this.this$0.sc, this.this$0.tunnel, parent_stream_id);
                  this.this$0.sc.streams.put((new StringBuffer(String.valueOf(this.val$c.id))).toString(), st);
                  Worker.startWorker(st);
                } catch (Exception e) {
                  this.this$0.msg(e);
                } 
              }
            });
      } catch (IOException e) {
        msg(e);
      } 
    } 
    this.out_queue_commands.addElement(this.sc.makeCommand(c.id, "A:" + c.num));
    return close_stream;
  }
  
  public int process(Socket sock, String host, int port, int id2, int parent_stream_id) throws Exception {
    if (id2 == 0)
      id2 = (int)(Math.random() * 100000.0D); 
    msg("Stream id is:" + id2);
    Properties queue = new Properties();
    this.sc.in_queues.put((new StringBuffer(String.valueOf(id2))).toString(), queue);
    this.out_queue_commands.addElement(this.sc.makeCommand(id2, "CONNECT:" + host + ":" + port + ":" + parent_stream_id));
    Stream st = new Stream(sock, id2, this.sc, this.tunnel, parent_stream_id);
    this.streams.put((new StringBuffer(String.valueOf(id2))).toString(), st);
    Worker.startWorker(st);
    return id2;
  }
  
  public String startStopTunnel(boolean start) throws Exception {
    if (start) {
      HttpURLConnection urlc = (HttpURLConnection)this.vrl.openConnection();
      urlc.setRequestMethod("POST");
      urlc.setRequestProperty("Cookie", "CrushAuth=" + this.auth + ";");
      urlc.setUseCaches(false);
      urlc.setDoOutput(true);
      String extra = "";
      if (this.tunnel.getProperty("configurable", "false").equals("true")) {
        extra = String.valueOf(extra) + "&bindIp=" + this.tunnel.getProperty("bindIp");
        extra = String.valueOf(extra) + "&localPort=" + this.tunnel.getProperty("localPort");
        extra = String.valueOf(extra) + "&destIp=" + this.tunnel.getProperty("destIp");
        extra = String.valueOf(extra) + "&destPort=" + this.tunnel.getProperty("destPort");
        extra = String.valueOf(extra) + "&channelsOutMax=" + this.tunnel.getProperty("channelsOutMax");
        extra = String.valueOf(extra) + "&channelsInMax=" + this.tunnel.getProperty("channelsInMax");
        extra = String.valueOf(extra) + "&reverse=" + this.tunnel.getProperty("reverse");
      } 
      urlc.getOutputStream().write(("c2f=" + this.auth.toString().substring(this.auth.toString().length() - 4) + "&command=" + (start ? "startTunnel3" : "stopTunnel3") + "&tunnelId=" + this.tunnel.getProperty("id") + ((this.clientid != null) ? ("&clientid=" + this.clientid) : "") + extra).getBytes("UTF8"));
      urlc.getResponseCode();
      Common.consumeResponse(urlc.getInputStream());
      urlc.disconnect();
      msg("Tunnel3:Started tunnel." + start);
      return "success";
    } 
    msg("Tunnel3:Closing tunnel:" + this.auth);
    this.active = false;
    for (int x = 0; x < 100; x++)
      Thread.sleep(100L); 
    return "success";
  }
  
  public boolean isActive() {
    return this.active;
  }
  
  public void msg(String s) {
    if (!System.getProperty("crushftp.home", "").equals("")) {
      Common.log("TUNNEL", 0, s);
    } else if (this.log != null) {
      this.log.addElement(new Date() + ": " + s);
    } else {
      System.out.println(new Date() + ": " + s);
    } 
  }
  
  public void msg(Exception e) {
    StackTraceElement[] ste = e.getStackTrace();
    msg(String.valueOf(Thread.currentThread().getName()) + ":" + e.toString());
    for (int x = 0; x < ste.length; x++)
      msg(String.valueOf(ste[x].getClassName()) + "." + ste[x].getMethodName() + ":" + ste[x].getLineNumber()); 
  }
  
  public Properties getTunnelItem(String tunnel_name) throws IOException {
    msg("Tunnel3:Getting tunnel from server.");
    HttpURLConnection urlc = (HttpURLConnection)this.vrl.openConnection();
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("Cookie", "CrushAuth=" + this.auth + ";");
    urlc.setUseCaches(false);
    urlc.setDoOutput(true);
    urlc.getOutputStream().write(("c2f=" + this.auth.toString().substring(this.auth.toString().length() - 4) + "&command=getTunnels").getBytes("UTF8"));
    urlc.getResponseCode();
    InputStream in = urlc.getInputStream();
    String data = "";
    int bytesRead = 0;
    byte[] b = new byte[16384];
    while (bytesRead >= 0) {
      bytesRead = in.read(b);
      if (bytesRead > 0)
        data = String.valueOf(data) + new String(b, 0, bytesRead, "UTF8"); 
    } 
    in.close();
    urlc.disconnect();
    Properties use_tunnel = null;
    if (data.indexOf("<response>") > 0) {
      data = data.substring(data.indexOf("<response>") + "<response>".length(), data.indexOf("</response"));
      String[] tunnelsStr = Common.url_decode(data.replace('~', '%')).split(";;;");
      for (int x = 0; x < tunnelsStr.length; x++) {
        Properties tunnel2 = new Properties();
        try {
          tunnel2.load(new ByteArrayInputStream(tunnelsStr[x].getBytes("UTF8")));
          if (tunnel_name != null) {
            if (tunnel_name.equalsIgnoreCase(tunnel2.getProperty("name"))) {
              use_tunnel = tunnel2;
              break;
            } 
          } else {
            use_tunnel = tunnel2;
            if (tunnel2.getProperty("localPort", "0").equals(System.getProperty("crushtunnel.magicport", "55555")))
              break; 
          } 
        } catch (Exception e) {
          msg(e);
        } 
      } 
    } 
    if (use_tunnel != null && use_tunnel.size() > 0) {
      msg("Tunnel3:Got tunnel from server:" + use_tunnel.size());
    } else {
      msg("**********NO TUNNEL FOUND FOR USER**********");
    } 
    return use_tunnel;
  }
  
  public void updateStats(Chunk c, String channel_id, Vector history, String label, int index) {}
  
  public Chunk makeCommand(int id, String command) {
    try {
      int num = 0;
      synchronized (this.command_num_lock) {
        num = this.command_num--;
        if (this.command_num < -1073741824)
          this.command_num = -1; 
      } 
      return new Chunk(id, command.getBytes("UTF8"), command.length(), num);
    } catch (UnsupportedEncodingException unsupportedEncodingException) {
      return null;
    } 
  }
}
