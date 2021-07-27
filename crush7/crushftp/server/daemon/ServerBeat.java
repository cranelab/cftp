package crushftp.server.daemon;

import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;

public class ServerBeat extends GenericServer {
  public static boolean current_master = true;
  
  Exception error = null;
  
  public String vip = "";
  
  public String member_ips = "";
  
  public String index1 = "1";
  
  public int port = 0;
  
  public String adapter = "";
  
  public String localIp = "";
  
  public String netmask = "255.255.255.0";
  
  boolean master = false;
  
  long born_on = 0L;
  
  long last_age_check = 0L;
  
  long born_on_min = 0L;
  
  String master_lan_ip = "";
  
  boolean need_remaster = false;
  
  static long start_millis = System.currentTimeMillis();
  
  boolean starting = true;
  
  public ServerBeat(Properties server_item) {
    super(server_item);
  }
  
  public void updateStatus() {
    synchronized (updateServerStatuses) {
      updateStatusInit();
      if (this.socket_created) {
        this.server_item.put("display", "ServerBeat://" + this.vip + ":" + this.port + "/ is running. (" + this.member_ips + ") Master=" + this.master + " MasterIP=" + this.master_lan_ip);
      } else {
        this.server_item.put("display", "ServerBeat://" + this.vip + ":" + this.port + "/ is stopped.  (" + this.member_ips + ") Master=" + this.master + " MasterIP=" + this.master_lan_ip);
      } 
    } 
  }
  
  public void run() {
    this.starting = true;
    init();
    try {
      if (ServerStatus.siIG("enterprise_level") <= 0) {
        this.busyMessage = "ServerBeat only valid for Enterprise licenses.";
        throw new Exception(this.busyMessage);
      } 
      getSocket();
      startBeat();
      this.member_ips = this.server_item.getProperty("vip2");
      this.last_age_check = System.currentTimeMillis();
      this.born_on = System.currentTimeMillis();
      this.born_on_min = 0L;
      ServerBeat sb = this;
      (new Thread(new Runnable(this, sb) {
            final ServerBeat this$0;
            
            private final ServerBeat val$sb;
            
            public void run() {
              while (this.this$0.die_now.length() == 0) {
                try {
                  int priority = Integer.parseInt(this.this$0.server_item.getProperty("priority", "1"));
                  this.this$0.server_sock.setSoTimeout(1000);
                  Socket pong = this.this$0.server_sock.accept();
                  pong.setSoTimeout(2000);
                  BufferedReader br = new BufferedReader(new InputStreamReader(pong.getInputStream()));
                  String msg = br.readLine();
                  Log.log("SERVERBEAT", 3, "Serverbeat pong:" + msg + ":" + pong);
                  pong.getOutputStream().write((String.valueOf(this.this$0.master) + ":" + this.this$0.born_on + ":" + this.this$0.listen_ip + ":" + this.this$0.need_remaster + ":" + priority + "\r\n").getBytes());
                  br.close();
                  pong.close();
                  this.this$0.master_receive_logic(msg, priority);
                } catch (SocketTimeoutException socketTimeoutException) {
                
                } catch (Exception e) {
                  Log.log("SERVERBEAT", 2, e);
                } 
                if (System.currentTimeMillis() - this.this$0.last_age_check > 5000L) {
                  this.this$0.starting = false;
                  this.this$0.need_remaster = false;
                  try {
                    if (this.this$0.born_on == this.this$0.born_on_min && !this.this$0.master) {
                      Log.log("SERVERBEAT", 0, "We are the oldest, but not master, starting master sequence now.");
                      this.val$sb.disableMaster(this.this$0.vip, this.this$0.index1);
                      this.val$sb.becomeMaster(this.this$0.vip, this.this$0.index1);
                      this.this$0.master_lan_ip = this.this$0.listen_ip;
                      this.this$0.updateStatus();
                    } else if (this.this$0.born_on != this.this$0.born_on_min && this.this$0.master) {
                      Log.log("SERVERBEAT", 0, "We are not the oldest, but we are master, releasing master status now.");
                      this.this$0.born_on = System.currentTimeMillis() - ServerBeat.start_millis;
                      this.val$sb.disableMaster(this.this$0.vip, this.this$0.index1);
                      this.this$0.need_remaster = true;
                      this.this$0.updateStatus();
                    } 
                  } catch (Exception e) {
                    Log.log("SERVERBEAT", 1, e);
                  } 
                  this.this$0.born_on_min = this.this$0.born_on;
                  this.this$0.last_age_check = System.currentTimeMillis();
                } 
              } 
            }
          })).start();
      (new Thread(new Runnable(this) {
            final ServerBeat this$0;
            
            public void run() {
              while (this.this$0.die_now.length() == 0) {
                for (int x = 0; x < (this.this$0.member_ips.split(",")).length; x++) {
                  String member_ip = this.this$0.member_ips.split(",")[x].trim();
                  try {
                    int priority = Integer.parseInt(this.this$0.server_item.getProperty("priority", "1"));
                    Socket pinger = new Socket();
                    pinger.setSoTimeout(1000);
                    pinger.connect(new InetSocketAddress(member_ip, this.this$0.port));
                    pinger.getOutputStream().write((String.valueOf(this.this$0.master) + ":" + this.this$0.born_on + ":" + this.this$0.listen_ip + ":" + this.this$0.need_remaster + ":" + priority + "\r\n").getBytes());
                    BufferedReader br = new BufferedReader(new InputStreamReader(pinger.getInputStream()));
                    String msg = br.readLine();
                    Log.log("SERVERBEAT", 3, "Serverbeat ping:" + msg + ":" + pinger);
                    br.close();
                    pinger.close();
                    this.this$0.master_sent_logic(msg, priority);
                  } catch (Exception exception) {}
                } 
                try {
                  Thread.sleep(1000L);
                } catch (Exception exception) {}
              } 
            }
          })).start();
      init(this.server_item.getProperty("vip"), this.server_item.getProperty("index1", "1"), Integer.parseInt(this.server_item.getProperty("port")), this.server_item.getProperty("netmask", "255.255.255.0"), this.server_item.getProperty("adapter"));
      while (this.socket_created && this.die_now.length() == 0) {
        Thread.sleep(1000L);
        if (this.error != null)
          throw this.error; 
        updateStatus();
      } 
    } catch (Exception e) {
      if (e.getMessage() == null || e.getMessage().indexOf("socket closed") < 0) {
        Log.log("SERVERBEAT", 1, e);
      } else {
        Log.log("SERVERBEAT", 3, e);
      } 
    } 
    this.socket_created = false;
    kill();
    updateStatus();
    if (this.restart) {
      this.restart = false;
      this.die_now = new StringBuffer();
      (new Thread(this)).start();
    } 
  }
  
  private void master_receive_logic(String msg, int priority) throws Exception {
    boolean master_opposite = msg.split(":")[0].equals("true");
    long born_on_opposite = Long.parseLong(msg.split(":")[1]);
    if (born_on_opposite < this.born_on_min || this.born_on_min == 0L)
      this.born_on_min = born_on_opposite; 
    if (master_opposite)
      this.master_lan_ip = msg.split(":")[2]; 
    boolean need_remaster_opposite = msg.split(":")[3].equals("true");
    int priority_opposite = Integer.parseInt(msg.split(":")[4].trim());
    if (priority_opposite > priority && this.master && !this.starting) {
      Log.log("SERVERBEAT", 0, "We are the master, but we are not the highest priority, making us young, and releasing master.");
      this.born_on = System.currentTimeMillis() - start_millis;
      disableMaster(this.vip, this.index1);
      updateStatus();
      this.need_remaster = true;
    } else if (need_remaster_opposite && this.master && !this.starting) {
      Log.log("SERVERBEAT", 0, "Elected master.");
      becomeMaster(this.vip, this.index1);
      this.master_lan_ip = this.listen_ip;
      updateStatus();
    } else if (priority_opposite > priority && this.born_on == this.born_on_min && !this.starting) {
      Log.log("SERVERBEAT", 0, "We are the oldest, but we are not the highest priority, making us young now.");
      this.born_on = System.currentTimeMillis() - start_millis;
      disableMaster(this.vip, this.index1);
      updateStatus();
      this.need_remaster = true;
    } else if (!this.master && priority > priority_opposite && !this.starting) {
      Log.log("SERVERBEAT", 0, "We are not the oldest, and we are not master, but we have priority.  Becoming master.");
      becomeMaster(this.vip, this.index1);
      this.master_lan_ip = this.listen_ip;
      this.born_on = this.born_on_min;
      updateStatus();
    } 
  }
  
  private void master_sent_logic(String msg, int priority) throws Exception {
    boolean master_opposite = msg.split(":")[0].equals("true");
    long born_on_opposite = Long.parseLong(msg.split(":")[1]);
    boolean need_remaster_opposite = msg.split(":")[3].equals("true");
    int priority_opposite = Integer.parseInt(msg.split(":")[4].trim());
    if (master_opposite && this.master && priority_opposite == priority && this.born_on != this.born_on_min && !this.starting) {
      Log.log("SERVERBEAT", 0, "Opposite says its master, and so do we, and we have the same priority...new election being held now.");
      this.born_on = System.currentTimeMillis() - start_millis;
      disableMaster(this.vip, this.index1);
      updateStatus();
      this.need_remaster = true;
    } else if (need_remaster_opposite && this.master && !this.starting) {
      Log.log("SERVERBEAT", 0, "Elected master, and already master.");
      becomeMaster(this.vip, this.index1);
      this.master_lan_ip = this.listen_ip;
      updateStatus();
    } else if (priority_opposite > priority && this.master && !this.starting) {
      Log.log("SERVERBEAT", 0, "Opposite has higher priority but we are master...giving up master status now.");
      this.born_on = System.currentTimeMillis() - start_millis;
      disableMaster(this.vip, this.index1);
      updateStatus();
      this.need_remaster = true;
    } 
  }
  
  public void init(String vip, String index1, int port, String netmask, String adapter) {
    this.vip = vip;
    this.index1 = index1;
    this.port = port;
    this.netmask = netmask;
    this.adapter = adapter;
    try {
      disableMaster(vip, index1);
    } catch (Exception exception) {}
    try {
      this.localIp = Common.getLocalIP();
    } catch (Exception exception) {}
  }
  
  public void startBeat() {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(this) {
            final ServerBeat this$0;
            
            public void run() {
              try {
                Thread.sleep(1000L);
                Log.log("SERVERBEAT", 1, "Serverbeat:releasing " + this.this$0.vip + "...");
                this.this$0.disableMaster(this.this$0.vip, this.this$0.index1);
              } catch (Exception exception) {}
              Log.log("SERVERBEAT", 1, "Serverbeat:released " + this.this$0.vip + ".");
            }
          }));
  }
  
  public void kill() {
    try {
      disableMaster(this.vip, this.index1);
    } catch (Exception e) {
      Log.log("SERVERBEAT", 1, e);
    } 
  }
  
  public void becomeMaster(String ip, String index) throws Exception {
    String ifconfig = ServerStatus.SG("serverbeat_command");
    if (ifconfig.equals("netsh") && !Common.machine_is_windows())
      ifconfig = "ifconfig"; 
    if (ifconfig.equals("ifconfig") && Common.machine_is_windows())
      ifconfig = "netsh"; 
    this.master = true;
    current_master = this.master;
    Thread.sleep(2000L);
    Log.log("SERVERBEAT", 0, "ServerBeat: becoming Master..." + ip);
    if (Common.machine_is_windows()) {
      exec(new String[] { "cmd", "/C", String.valueOf(ifconfig) + " interface ip add address name=\"" + this.adapter + "\" addr=" + ip + " mask=" + this.netmask });
    } else if (Common.machine_is_solaris() || Common.machine_is_linux()) {
      exec((String.valueOf(ServerStatus.SG("serverbeat_ifup_command")) + " " + this.adapter + ":" + index).split(" "));
      if (ServerStatus.BG("serverbeat_plumb"))
        exec((String.valueOf(ifconfig) + " " + this.adapter + ":" + index + " plumb").split(" ")); 
      exec((String.valueOf(ifconfig) + " " + this.adapter + ":" + index + " " + ip + " netmask " + this.netmask + " up").split(" "));
    } else {
      exec((String.valueOf(ifconfig) + " " + this.adapter + " alias " + ip + " netmask " + this.netmask).split(" "));
    } 
    this.master = true;
    current_master = this.master;
    String command = ServerStatus.SG("serverbeat_post_command");
    if (!command.equals("")) {
      command = Common.replace_str(command, "{vip}", ip);
      command = Common.replace_str(command, "{adapter}", this.adapter);
      exec(command.split(" "));
    } 
  }
  
  public void disableMaster(String ip, String index) throws Exception {
    String ifconfig = ServerStatus.SG("serverbeat_command");
    if (ifconfig.equals("netsh") && !Common.machine_is_windows())
      ifconfig = "ifconfig"; 
    if (ifconfig.equals("ifconfig") && Common.machine_is_windows())
      ifconfig = "netsh"; 
    this.master = false;
    current_master = this.master;
    Log.log("SERVERBEAT", 0, "ServerBeat: disabling Master..." + ip);
    if (Common.machine_is_windows()) {
      exec(new String[] { "cmd", "/C", String.valueOf(ifconfig) + " interface ip delete address name=\"" + this.adapter + "\" addr=" + ip });
    } else if (Common.machine_is_solaris() || Common.machine_is_linux()) {
      exec((String.valueOf(ifconfig) + " " + this.adapter + ":" + index + " " + ip + " netmask " + this.netmask + " down").split(" "));
      if (ServerStatus.BG("serverbeat_unplumb"))
        exec((String.valueOf(ifconfig) + " " + this.adapter + ":" + index + " unplumb").split(" ")); 
      exec((String.valueOf(ServerStatus.SG("serverbeat_ifdown_command")) + " " + this.adapter + ":" + index).split(" "));
    } else {
      exec((String.valueOf(ifconfig) + " " + this.adapter + " -alias " + ip + " netmask " + this.netmask).split(" "));
    } 
    this.master = false;
    current_master = this.master;
  }
  
  public String exec(String[] c) throws Exception {
    if (this.vip.toUpperCase().indexOf("JOB") >= 0)
      return ""; 
    String s = "";
    for (int x = 0; x < c.length; x++)
      s = String.valueOf(s) + c[x] + " "; 
    Log.log("SERVERBEAT", 0, "ServerBeat exec: " + s.trim());
    Process proc = Runtime.getRuntime().exec(c);
    BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
    String result = "";
    String lastLine = "";
    while ((result = br1.readLine()) != null) {
      Log.log("SERVERBEAT", 0, "ServerBeat: " + result);
      lastLine = result;
    } 
    proc.waitFor();
    try {
      proc.destroy();
    } catch (Exception exception) {}
    return lastLine;
  }
}
