package crushftp.server.daemon;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

public class ServerBeat extends GenericServer {
  JChannel channel = null;
  
  Exception error = null;
  
  public String vip = "";
  
  public String vip2 = "";
  
  public String index1 = "1";
  
  public int port = 0;
  
  public String ifconfig = null;
  
  public String netmask = null;
  
  public String adapter = "";
  
  public String localIp = "";
  
  boolean master = false;
  
  public ServerBeat(Properties server_item) {
    super(server_item);
  }
  
  public void updateStatus() {
    synchronized (updateServerStatuses) {
      updateStatusInit();
      if (this.socket_created) {
        this.server_item.put("display", "ServerBeat://" + this.vip + ":" + this.port + "/ is running. (" + this.vip2 + ") Master=" + this.master);
      } else {
        this.server_item.put("display", "ServerBeat://" + this.vip + ":" + this.port + "/ is stopped.  (" + this.vip2 + ") Master=" + this.master);
      } 
    } 
  }
  
  public void run() {
    init();
    try {
      if (ServerStatus.siIG("enterprise_level") <= 0) {
        this.busyMessage = "ServerBeat only valid for Enterprise licenses.";
        throw new Exception(this.busyMessage);
      } 
      getSocket();
      this.server_sock.close();
      startBeat();
      String initial_hosts = "";
      this.vip2 = this.server_item.getProperty("vip2");
      for (int x = 0; x < (this.vip2.split(",")).length; x++) {
        String ip = this.vip2.split(",")[x].trim();
        if (x > 0)
          initial_hosts = String.valueOf(initial_hosts) + ","; 
        initial_hosts = String.valueOf(initial_hosts) + ip + "[" + this.listen_port + "]";
      } 
      String xml_config = "<config xmlns=\"urn:org:jgroups\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:org:jgroups http://www.jgroups.org/schema/JGroups-3.1.xsd\">";
      xml_config = String.valueOf(xml_config) + "<TCP bind_port=\"" + this.listen_port + "\" bind_addr=\"" + this.listen_ip + "\"/><TCPPING initial_hosts=\"" + initial_hosts + "\" port_range=\"1\"/>";
      xml_config = String.valueOf(xml_config) + "<MERGE2/><FD_SOCK/><FD/><VERIFY_SUSPECT/><pbcast.NAKACK2 use_mcast_xmit=\"false\"/><UNICAST/><pbcast.STABLE/><pbcast.GMS/><UFC/><MFC/><FRAG2/><pbcast.STATE_TRANSFER/><pbcast.FLUSH timeout=\"5\"/>";
      xml_config = String.valueOf(xml_config) + "</config>";
      ServerBeat sb = this;
      this.channel = new JChannel(new ByteArrayInputStream(xml_config.getBytes()));
      ReceiverAdapter ra = new ReceiverAdapter(this, sb) {
          final ServerBeat this$0;
          
          private final ServerBeat val$sb;
          
          public void receive(Message msg) {
            Properties p = (Properties)msg.getObject();
            if (p.getProperty("type").equals("sync")) {
              View view = this.this$0.channel.getView();
              (new Thread((Runnable)new Object(this, view, this.val$sb)))












                
                .start();
            } 
          }
          
          public void viewAccepted(View new_view) {
            try {
              Log.log("SERVERBEAT", 0, "VIEW:" + this.this$0.channel.getClusterName() + ":" + this.this$0.channel.getName() + ":" + new_view.getMembers());
              (new Thread((Runnable)new Object(this)))













                
                .start();
            } catch (Exception e) {
              this.this$0.error = e;
            } 
          }
        };
      this.channel.setReceiver((Receiver)ra);
      this.channel.connect("VIP:" + this.vip);
      Properties p = new Properties();
      p.put("type", "sync");
      Message msg = new Message(null, null, p);
      this.channel.send(msg);
      String ifconfig = ServerStatus.SG("serverbeat_command");
      String netmask = "255.255.255.0";
      init(this.server_item.getProperty("vip"), this.server_item.getProperty("index1", "1"), Integer.parseInt(this.server_item.getProperty("port")), ifconfig, netmask, this.server_item.getProperty("adapter"));
      while (this.socket_created && this.die_now.length() == 0 && this.server_item.getProperty("serverbeatEnabled", "false").equals("true")) {
        Thread.sleep(1000L);
        if (this.error != null)
          throw this.error; 
        updateStatus();
      } 
    } catch (Exception e) {
      if (e.getMessage() == null || e.getMessage().indexOf("socket closed") < 0) {
        Log.log("SERVER", 1, e);
      } else {
        Log.log("SERVER", 3, e);
      } 
    } 
    this.channel.close();
    this.socket_created = false;
    kill();
    updateStatus();
    if (this.restart) {
      this.restart = false;
      this.die_now = new StringBuffer();
      (new Thread(this)).start();
    } 
  }
  
  public void init(String vip, String index1, int port, String ifconfig, String netmask, String adapter) {
    this.vip = vip;
    this.index1 = index1;
    this.port = port;
    this.ifconfig = ifconfig;
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
    this.master = true;
    Thread.sleep(2000L);
    Log.log("SERVERBEAT", 0, "ServerBeat: becoming Master..." + ip);
    if (Common.machine_is_windows()) {
      exec(new String[] { "cmd", "/C", String.valueOf(this.ifconfig) + " interface ip add address name=\"" + this.adapter + "\" addr=" + ip + " mask=" + this.netmask });
    } else if (Common.machine_is_solaris() || Common.machine_is_linux()) {
      exec((String.valueOf(ServerStatus.SG("serverbeat_ifup_command")) + " " + this.adapter + ":" + index).split(" "));
      exec((String.valueOf(this.ifconfig) + " " + this.adapter + ":" + index + " plumb").split(" "));
      exec((String.valueOf(this.ifconfig) + " " + this.adapter + ":" + index + " " + ip + " netmask " + this.netmask + " up").split(" "));
    } else {
      String str = String.valueOf(this.ifconfig) + " " + this.adapter + " alias " + ip + " netmask " + this.netmask;
      Log.log("SERVERBEAT", 2, "ServerBeat: " + str);
      exec(str.split(" "));
    } 
    this.master = true;
    String command = ServerStatus.SG("serverbeat_post_command");
    if (!command.equals("")) {
      command = Common.replace_str(command, "{vip}", ip);
      Log.log("SERVERBEAT", 2, "ServerBeat: " + command);
      exec(command.split(" "));
    } 
  }
  
  public void disableMaster(String ip, String index) throws Exception {
    this.master = false;
    Log.log("SERVERBEAT", 0, "ServerBeat: disabling Master..." + ip);
    if (Common.machine_is_windows()) {
      exec(new String[] { "cmd", "/C", String.valueOf(this.ifconfig) + " interface ip delete address name=\"" + this.adapter + "\" addr=" + ip });
    } else if (Common.machine_is_solaris() || Common.machine_is_linux()) {
      exec((String.valueOf(this.ifconfig) + " " + this.adapter + ":" + index + " " + ip + " netmask " + this.netmask + " down").split(" "));
      exec((String.valueOf(this.ifconfig) + " " + this.adapter + ":" + index + " unplumb").split(" "));
      exec((String.valueOf(ServerStatus.SG("serverbeat_ifdown_command")) + " " + this.adapter + ":" + index).split(" "));
    } else {
      String command = String.valueOf(this.ifconfig) + " " + this.adapter + " -alias " + ip + " netmask " + this.netmask;
      Log.log("SERVERBEAT", 2, "ServerBeat: " + command);
      exec(command.split(" "));
    } 
    this.master = false;
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
