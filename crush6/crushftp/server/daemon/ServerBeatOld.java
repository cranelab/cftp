package crushftp.server.daemon;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class ServerBeatOld extends GenericServer {
  ServerBeatOld sb = null;
  
  Thread beatVerifier;
  
  Thread beatListener;
  
  Thread beatListener2;
  
  Thread beatMasterVerifier;
  
  public String vip;
  
  public String vip2;
  
  public String index1;
  
  public String index2;
  
  public int port;
  
  public ServerSocket beatSS;
  
  public ServerSocket beatSS2;
  
  public Socket sock;
  
  public Vector ips;
  
  public Vector ipTimes;
  
  public boolean master;
  
  public String ifconfig;
  
  public String netmask;
  
  public String adapter;
  
  public String localIp;
  
  public boolean pause;
  
  public boolean stop;
  
  public int clusteredServers;
  
  public Vector uniqueIps;
  
  public ServerBeatOld(Properties server_item) {
    super(server_item);
    this.beatVerifier = null;
    this.beatListener = null;
    this.beatListener2 = null;
    this.beatMasterVerifier = null;
    this.vip = "";
    this.vip2 = "";
    this.index1 = "1";
    this.index2 = "2";
    this.port = 0;
    this.beatSS = null;
    this.beatSS2 = null;
    this.sock = null;
    this.ips = new Vector();
    this.ipTimes = new Vector();
    this.master = false;
    this.ifconfig = null;
    this.netmask = null;
    this.adapter = "";
    this.localIp = "";
    this.pause = false;
    this.stop = false;
    this.clusteredServers = 0;
    this.uniqueIps = new Vector();
    this.sb = this;
  }
  
  public void updateStatus() {
    synchronized (updateServerStatuses) {
      if (!this.started) {
        this.server_item.put("display", "ServerBeat: disabled");
        return;
      } 
      updateStatusInit();
      if (this.socket_created)
        this.server_item.put("display", "ServerBeat://" + this.sb.vip + ":" + this.sb.port + "/ " + (this.sb.master ? ("(Master, " + ((this.sb.clusteredServers - 1 < 0) ? 0 : (this.sb.clusteredServers - 1)) + " Child.)") : "(Child)") + " VIP Status IP:" + this.sb.vip2); 
    } 
  }
  
  public void run() {
    init();
    try {
      getSocket();
      if (this.socket_created && this.die_now.length() == 0) {
        if (this.sb != null)
          this.sb.stop = true; 
        if (this.server_item.getProperty("serverbeatEnabled", "false").equals("true")) {
          String ifconfig = "/sbin/ifconfig";
          String netmask = "255.255.255.255";
          if (Common.machine_is_windows()) {
            ifconfig = "netsh";
            netmask = "255.255.255.0";
          } 
          this.server_sock.close();
          this.sb.init(this.server_item.getProperty("vip"), this.server_item.getProperty("vip2"), this.server_item.getProperty("index1", "1"), this.server_item.getProperty("index2", "2"), Integer.parseInt(this.server_item.getProperty("port")), ifconfig, netmask, this.server_item.getProperty("adapter"));
          this.sb.kill();
          this.sb.startBeat();
          while (this.socket_created && this.die_now.length() == 0 && this.server_item.getProperty("serverbeatEnabled", "false").equals("true"))
            Thread.sleep(1000L); 
        } 
        this.sb.stop = true;
      } 
    } catch (Exception e) {
      if (e.getMessage().indexOf("socket closed") < 0) {
        Log.log("SERVER", 1, e);
      } else {
        Log.log("SERVER", 3, e);
      } 
    } 
    if (this.sb != null)
      this.sb.stop = true; 
    this.socket_created = false;
    updateStatus();
    if (this.restart) {
      this.restart = false;
      this.die_now = new StringBuffer();
      (new Thread(this)).start();
    } 
  }
  
  public void init(String vip, String vip2, String index1, String index2, int port, String ifconfig, String netmask, String adapter) {
    this.vip = vip;
    this.vip2 = vip2;
    this.index1 = index1;
    this.index2 = index2;
    this.port = port;
    this.ifconfig = ifconfig;
    this.netmask = netmask;
    this.adapter = adapter;
    try {
      disableMaster(vip, index1);
    } catch (Exception exception) {}
    try {
      disableMaster(vip2, index2);
    } catch (Exception exception) {}
    try {
      this.localIp = Common.getLocalIP();
    } catch (Exception exception) {}
  }
  
  public void purgeOldIps() {
    for (int x = this.ipTimes.size() - 1; x >= 0; x--) {
      long l = Long.parseLong(this.ipTimes.elementAt(x).toString());
      if ((new Date()).getTime() - l > 2000L) {
        this.ipTimes.removeElementAt(x);
        this.ips.removeElementAt(x);
      } 
    } 
  }
  
  public void startBeat() {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(this) {
            final ServerBeatOld this$0;
            
            public void run() {
              this.this$0.stop = true;
              try {
                Thread.sleep(1000L);
              } catch (Exception exception) {}
              Log.log("SERVERBEAT", 1, "Serverbeat:releasing " + this.this$0.vip + "...");
              try {
                this.this$0.disableMaster(this.this$0.vip, this.this$0.index1);
              } catch (Exception exception) {}
              Log.log("SERVERBEAT", 1, "Serverbeat:released " + this.this$0.vip + ".");
            }
          }));
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(this) {
            final ServerBeatOld this$0;
            
            public void run() {
              this.this$0.stop = true;
              try {
                Thread.sleep(1000L);
              } catch (Exception exception) {}
              Log.log("SERVERBEAT", 1, "Serverbeat:releasing " + this.this$0.vip2 + "...");
              try {
                this.this$0.disableMaster(this.this$0.vip2, this.this$0.index2);
              } catch (Exception exception) {}
              Log.log("SERVERBEAT", 1, "Serverbeat:released " + this.this$0.vip2 + ".");
            }
          }));
    this.beatListener = new Thread(new Runnable(this) {
          final ServerBeatOld this$0;
          
          public void run() {
            Thread.currentThread().setName("beatListener");
            while (!this.this$0.stop) {
              try {
                this.this$0.purgeOldIps();
                if (this.this$0.beatSS == null || this.this$0.beatSS.isClosed()) {
                  Thread.sleep(100L);
                  continue;
                } 
                try {
                  this.this$0.beatSS.setSoTimeout(100);
                  Socket s = this.this$0.beatSS.accept();
                  this.this$0.ips.addElement(s.getInetAddress().getHostAddress());
                  this.this$0.ipTimes.addElement((new StringBuffer(String.valueOf((new Date()).getTime()))).toString());
                  s.close();
                } catch (SocketException socketException) {
                
                } catch (IOException iOException) {}
              } catch (Exception exception) {}
              Log.log("SERVERBEAT", 3, "ServerBeat: " + this.this$0.ips.toString());
            } 
            this.this$0.kill();
          }
        });
    this.beatListener.start();
    this.beatListener2 = new Thread(new Runnable(this) {
          final ServerBeatOld this$0;
          
          public void run() {
            Thread.currentThread().setName("beatListener2");
            while (!this.this$0.stop) {
              try {
                if (this.this$0.beatSS2 == null || this.this$0.beatSS2.isClosed()) {
                  Thread.sleep(100L);
                  continue;
                } 
                try {
                  this.this$0.beatSS2.setSoTimeout(100);
                  Socket s = this.this$0.beatSS2.accept();
                  s.close();
                } catch (SocketException socketException) {
                
                } catch (IOException iOException) {}
              } catch (Exception exception) {}
            } 
            this.this$0.kill();
          }
        });
    this.beatListener2.start();
    this.beatMasterVerifier = new Thread(new Runnable(this) {
          final ServerBeatOld this$0;
          
          public void run() {
            Thread.currentThread().setName("beatMasterVerifier");
            while (!this.this$0.stop) {
              try {
                if (this.this$0.master) {
                  this.this$0.pause = true;
                  Thread.sleep(1000L);
                  Log.log("SERVERBEAT", 2, "ServerBeat: verifying we are really are the master still...");
                  if (this.this$0.beatSS2 != null)
                    this.this$0.beatSS2.close(); 
                  this.this$0.disableMaster(this.this$0.vip2, this.this$0.index2);
                  Thread.sleep(2000L);
                  try {
                    Socket sock2 = new Socket();
                    sock2.connect(new InetSocketAddress(this.this$0.vip2, this.this$0.port), 900);
                    sock2.close();
                    if (this.this$0.beatSS != null)
                      this.this$0.beatSS.close(); 
                    this.this$0.disableMaster(this.this$0.vip, this.this$0.index1);
                    this.this$0.pause = false;
                  } catch (Exception e) {
                    this.this$0.becomeMaster(this.this$0.vip2, this.this$0.index2);
                    this.this$0.pause = false;
                  } 
                } 
              } catch (Exception e) {
                Log.log("SERVERBEAT", 1, e);
              } 
              try {
                Thread.sleep((10000 + (int)(Math.random() * 10.0D)));
              } catch (Exception exception) {}
            } 
            this.this$0.kill();
          }
        });
    this.beatMasterVerifier.start();
    this.beatVerifier = new Thread(new Runnable(this) {
          final ServerBeatOld this$0;
          
          public void run() {
            Thread.currentThread().setName("beatVerifier");
            try {
              while (!this.this$0.stop) {
                while (this.this$0.pause)
                  Thread.sleep(100L); 
                if (this.this$0.beatSS == null || this.this$0.beatSS.isClosed()) {
                  this.this$0.beatSS = null;
                  try {
                    this.this$0.beatSS = new ServerSocket(this.this$0.port, 100, InetAddress.getByName(this.this$0.vip));
                  } catch (Exception exception) {}
                } 
                if (this.this$0.beatSS2 == null || this.this$0.beatSS2.isClosed()) {
                  this.this$0.beatSS2 = null;
                  try {
                    this.this$0.beatSS2 = new ServerSocket(this.this$0.port, 100, InetAddress.getByName(this.this$0.vip2));
                  } catch (Exception exception) {}
                } 
                try {
                  if (this.this$0.sock != null && !this.this$0.sock.isClosed())
                    this.this$0.sock.close(); 
                  this.this$0.sock = new Socket();
                  this.this$0.sock.connect(new InetSocketAddress(this.this$0.vip, this.this$0.port), 900);
                  this.this$0.sock.close();
                  Thread.sleep(900L);
                  this.this$0.uniqueIps.removeAllElements();
                  for (int x = this.this$0.ipTimes.size() - 1; x >= 0; x--) {
                    if (this.this$0.uniqueIps.indexOf(this.this$0.ips.elementAt(x).toString()) < 0)
                      this.this$0.uniqueIps.addElement(this.this$0.ips.elementAt(x).toString()); 
                  } 
                  this.this$0.clusteredServers = this.this$0.uniqueIps.size();
                  if (this.this$0.ips.indexOf(this.this$0.vip) >= 0 || this.this$0.ips.indexOf(this.this$0.localIp) >= 0) {
                    Log.log("SERVERBEAT", 2, "ServerBeat: we are master.");
                  } else {
                    Log.log("SERVERBEAT", 2, "ServerBeat: we are a child.");
                    if (this.this$0.beatSS != null) {
                      this.this$0.beatSS.close();
                      this.this$0.beatSS = null;
                      if (this.this$0.beatSS2 != null)
                        this.this$0.beatSS2.close(); 
                      this.this$0.beatSS2 = null;
                      this.this$0.disableMaster(this.this$0.vip, this.this$0.index1);
                      this.this$0.disableMaster(this.this$0.vip2, this.this$0.index2);
                    } 
                    if (this.this$0.master) {
                      this.this$0.disableMaster(this.this$0.vip, this.this$0.index1);
                      this.this$0.disableMaster(this.this$0.vip2, this.this$0.index2);
                    } 
                  } 
                } catch (Exception e) {
                  if (this.this$0.beatSS == null) {
                    this.this$0.disableMaster(this.this$0.vip, this.this$0.index1);
                    this.this$0.disableMaster(this.this$0.vip2, this.this$0.index2);
                    Thread.sleep(1000L);
                    this.this$0.becomeMaster(this.this$0.vip, this.this$0.index1);
                    this.this$0.becomeMaster(this.this$0.vip2, this.this$0.index2);
                    Thread.sleep(3000L);
                  } 
                } 
                Thread.sleep(1000L);
              } 
            } catch (Exception e) {
              Log.log("SERVERBEAT", 1, e);
            } 
            this.this$0.kill();
          }
        });
    this.beatVerifier.start();
  }
  
  public synchronized void kill() {
    try {
      if (this.beatSS != null)
        this.beatSS.close(); 
      if (this.beatSS2 != null)
        this.beatSS2.close(); 
      if (this.sock != null)
        this.sock.close(); 
      if (this.master) {
        disableMaster(this.vip, this.index1);
        disableMaster(this.vip2, this.index2);
      } 
    } catch (Exception e) {
      Log.log("SERVERBEAT", 1, e);
    } 
  }
  
  public void becomeMaster(String ip, String index) throws Exception {
    int logLevel = 2;
    if (ip.equals(this.vip))
      logLevel = 0; 
    this.master = true;
    Log.log("SERVERBEAT", logLevel, "ServerBeat: becoming Master..." + ip);
    if (Common.machine_is_windows()) {
      exec(new String[] { "cmd", "/C", String.valueOf(this.ifconfig) + " interface ip add address name=\"" + this.adapter + "\" addr=" + ip + " mask=" + this.netmask });
    } else if (Common.machine_is_solaris() || Common.machine_is_linux()) {
      exec((String.valueOf(this.ifconfig) + " " + this.adapter + ":" + index + " plumb").split(" "));
      exec((String.valueOf(this.ifconfig) + " " + this.adapter + ":" + index + " " + ip + " netmask " + this.netmask + " up").split(" "));
    } else {
      String command = String.valueOf(this.ifconfig) + " " + this.adapter + " alias " + ip + " netmask " + this.netmask;
      Log.log("SERVERBEAT", 2, "ServerBeat: " + command);
      exec(command.split(" "));
    } 
  }
  
  public void disableMaster(String ip, String index) throws Exception {
    int logLevel = 2;
    if (ip.equals(this.vip))
      logLevel = 0; 
    if (ip.equals(this.vip))
      this.master = false; 
    Log.log("SERVERBEAT", logLevel, "ServerBeat: disabling Master..." + ip);
    if (Common.machine_is_windows()) {
      exec(new String[] { "cmd", "/C", String.valueOf(this.ifconfig) + " interface ip delete address name=\"" + this.adapter + "\" addr=" + ip });
    } else if (Common.machine_is_solaris() || Common.machine_is_linux()) {
      exec((String.valueOf(this.ifconfig) + " " + this.adapter + ":" + index + " " + ip + " netmask " + this.netmask + " down").split(" "));
      exec((String.valueOf(this.ifconfig) + " " + this.adapter + ":" + index + " unplumb").split(" "));
    } else {
      String command = String.valueOf(this.ifconfig) + " " + this.adapter + " -alias " + ip + " netmask " + this.netmask;
      Log.log("SERVERBEAT", 2, "ServerBeat: " + command);
      exec(command.split(" "));
    } 
  }
  
  public String exec(String[] c) throws Exception {
    Process proc = Runtime.getRuntime().exec(c);
    BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
    String result = "";
    String lastLine = "";
    while ((result = br1.readLine()) != null) {
      Log.log("SERVERBEAT", 2, "ServerBeat: " + result);
      lastLine = result;
    } 
    proc.waitFor();
    try {
      proc.destroy();
    } catch (Exception exception) {}
    return lastLine;
  }
}
