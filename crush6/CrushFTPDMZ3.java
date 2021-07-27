import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.AdminControls;
import crushftp.server.ServerStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import java.util.Vector;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.stack.GossipRouter;

public class CrushFTPDMZ3 {
  public static Vector queue = new Vector();
  
  public static Properties dmzResponses = new Properties();
  
  boolean started = false;
  
  boolean starting = false;
  
  Common common_code = new Common();
  
  public static Vector socket_queue = new Vector();
  
  Vector grouters = new Vector();
  
  Vector activeAdminSockets = new Vector();
  
  Vector activeAdminSocketsWrite = new Vector();
  
  Vector channels = new Vector();
  
  Vector xmls = new Vector();
  
  Exception error = null;
  
  static String ipv4 = "false";
  
  public CrushFTPDMZ3(String[] args) {
    Common.System2.put("crushftp.dmz.queue", queue);
    Common.System2.put("crushftp.dmz.queue.sock", socket_queue);
    String[] port_and_ips = args[1].split(",");
    int[] listen_ports = new int[port_and_ips.length];
    String[] listen_ips = new String[port_and_ips.length];
    int x;
    for (x = 0; x < port_and_ips.length; x++) {
      if (port_and_ips[x].indexOf(":") >= 0) {
        listen_ports[x] = Integer.parseInt(port_and_ips[x].split(":")[1]);
        listen_ips[x] = port_and_ips[x].split(":")[0];
      } else {
        listen_ports[x] = Integer.parseInt(port_and_ips[x]);
        listen_ips[x] = "0.0.0.0";
      } 
    } 
    ipv4 = System.getProperty("java.net.preferIPv4Stack", "false");
    System.setProperty("java.net.preferIPv4Stack", "true");
    try {
      for (x = 0; x < listen_ports.length; x++) {
        int data_port = listen_ports[x] + 1;
        String data_ip = listen_ips[x];
        int loop_num = x;
        (new Thread(new Runnable(this, loop_num, data_port, data_ip) {
              final CrushFTPDMZ3 this$0;
              
              private final int val$loop_num;
              
              private final int val$data_port;
              
              private final String val$data_ip;
              
              public void run() {
                // Byte code:
                //   0: invokestatic currentThread : ()Ljava/lang/Thread;
                //   3: new java/lang/StringBuffer
                //   6: dup
                //   7: ldc 'DMZDataSocketReceiver:'
                //   9: invokespecial <init> : (Ljava/lang/String;)V
                //   12: aload_0
                //   13: getfield val$loop_num : I
                //   16: invokevirtual append : (I)Ljava/lang/StringBuffer;
                //   19: invokevirtual toString : ()Ljava/lang/String;
                //   22: invokevirtual setName : (Ljava/lang/String;)V
                //   25: aload_0
                //   26: getfield this$0 : LCrushFTPDMZ3;
                //   29: getfield common_code : Lcrushftp/handlers/Common;
                //   32: aload_0
                //   33: getfield val$data_port : I
                //   36: aload_0
                //   37: getfield val$data_ip : Ljava/lang/String;
                //   40: ldc 'builtin'
                //   42: ldc 'crushftp'
                //   44: ldc 'crushftp'
                //   46: ldc ''
                //   48: iconst_0
                //   49: iconst_1
                //   50: invokevirtual getServerSocket : (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZI)Ljava/net/ServerSocket;
                //   53: astore_1
                //   54: aconst_null
                //   55: astore_2
                //   56: aload_1
                //   57: invokevirtual accept : ()Ljava/net/Socket;
                //   60: astore_2
                //   61: iconst_0
                //   62: istore_3
                //   63: goto -> 72
                //   66: ldc2_w 1000
                //   69: invokestatic sleep : (J)V
                //   72: getstatic CrushFTPDMZ3.socket_queue : Ljava/util/Vector;
                //   75: invokevirtual size : ()I
                //   78: ifne -> 90
                //   81: iload_3
                //   82: iinc #3, 1
                //   85: bipush #40
                //   87: if_icmplt -> 66
                //   90: getstatic CrushFTPDMZ3.socket_queue : Ljava/util/Vector;
                //   93: invokevirtual size : ()I
                //   96: ifle -> 200
                //   99: aload_2
                //   100: astore #4
                //   102: new java/lang/StringBuffer
                //   105: dup
                //   106: invokespecial <init> : ()V
                //   109: astore #5
                //   111: new CrushFTPDMZ3$2
                //   114: dup
                //   115: aload_0
                //   116: aload #4
                //   118: aload #5
                //   120: invokespecial <init> : (LCrushFTPDMZ3$1;Ljava/net/Socket;Ljava/lang/StringBuffer;)V
                //   123: invokestatic startWorker : (Ljava/lang/Runnable;)Z
                //   126: pop
                //   127: iconst_0
                //   128: istore #6
                //   130: goto -> 155
                //   133: aload #5
                //   135: invokevirtual length : ()I
                //   138: ifne -> 150
                //   141: ldc2_w 1000
                //   144: invokestatic sleep : (J)V
                //   147: goto -> 152
                //   150: aconst_null
                //   151: astore_2
                //   152: iinc #6, 1
                //   155: iload #6
                //   157: iconst_5
                //   158: if_icmpge -> 200
                //   161: aload_2
                //   162: ifnonnull -> 133
                //   165: goto -> 200
                //   168: astore_3
                //   169: aload_3
                //   170: invokevirtual printStackTrace : ()V
                //   173: goto -> 200
                //   176: astore_3
                //   177: goto -> 200
                //   180: astore #8
                //   182: jsr -> 188
                //   185: aload #8
                //   187: athrow
                //   188: astore #7
                //   190: aload_2
                //   191: ifnull -> 198
                //   194: aload_2
                //   195: invokevirtual close : ()V
                //   198: ret #7
                //   200: jsr -> 188
                //   203: goto -> 56
                //   206: astore_1
                //   207: aload_1
                //   208: invokevirtual printStackTrace : ()V
                //   211: iconst_0
                //   212: invokestatic exit : (I)V
                //   215: return
                // Line number table:
                //   Java source line number -> byte code offset
                //   #77	-> 0
                //   #80	-> 25
                //   #81	-> 54
                //   #86	-> 56
                //   #87	-> 61
                //   #88	-> 63
                //   #89	-> 66
                //   #88	-> 72
                //   #90	-> 90
                //   #92	-> 99
                //   #93	-> 102
                //   #94	-> 111
                //   #113	-> 127
                //   #115	-> 133
                //   #116	-> 150
                //   #113	-> 152
                //   #120	-> 168
                //   #122	-> 169
                //   #124	-> 176
                //   #129	-> 180
                //   #131	-> 185
                //   #129	-> 188
                //   #130	-> 190
                //   #131	-> 198
                //   #82	-> 203
                //   #134	-> 206
                //   #136	-> 207
                //   #137	-> 211
                //   #139	-> 215
                // Local variable table:
                //   start	length	slot	name	descriptor
                //   0	216	0	this	LCrushFTPDMZ3$1;
                //   54	152	1	ss_data	Ljava/net/ServerSocket;
                //   56	150	2	sock	Ljava/net/Socket;
                //   63	105	3	loops	I
                //   102	63	4	sock2	Ljava/net/Socket;
                //   111	54	5	status	Ljava/lang/StringBuffer;
                //   130	35	6	x	I
                //   169	4	3	e	Ljava/lang/ArrayIndexOutOfBoundsException;
                //   207	8	1	e	Ljava/lang/Exception;
                // Exception table:
                //   from	to	target	type
                //   25	206	206	java/lang/Exception
                //   56	165	168	java/lang/ArrayIndexOutOfBoundsException
                //   56	165	176	java/io/IOException
                //   56	173	180	finally
                //   176	177	180	finally
                //   200	203	180	finally
              }
            })).start();
        String xml_config = "";
        if (!(new File("dmz_cluster.xml")).exists()) {
          xml_config = "<config xmlns=\"urn:org:jgroups\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:org:jgroups http://www.jgroups.org/schema/JGroups-3.1.xsd\">\r\n";
          xml_config = String.valueOf(xml_config) + "<TUNNEL gossip_router_hosts=\"127.0.0.1[%bind_port%]\"/>\r\n";
          xml_config = String.valueOf(xml_config) + "<PING/>\r\n";
          xml_config = String.valueOf(xml_config) + "<MERGE2/><FD_SOCK/><FD/><VERIFY_SUSPECT/><pbcast.NAKACK2 use_mcast_xmit=\"false\"/><UNICAST/><pbcast.STABLE/><pbcast.GMS/><UFC/><MFC/><FRAG2/><pbcast.STATE_TRANSFER/><pbcast.FLUSH timeout=\"5\"/>\r\n";
          xml_config = String.valueOf(xml_config) + "</config>\r\n";
          Common.streamCopier(new ByteArrayInputStream(xml_config.getBytes("UTF8")), new FileOutputStream("dmz_cluster.xml"), false, true, true);
        } 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Common.streamCopier(new FileInputStream("dmz_cluster.xml"), baos, false, true, true);
        xml_config = new String(baos.toByteArray(), "UTF8");
        xml_config = Common.replace_str(xml_config, "%bind_port%", (new StringBuffer(String.valueOf(listen_ports[x]))).toString());
        xml_config = Common.replace_str(xml_config, "%bind_addr%", (new StringBuffer(String.valueOf(listen_ips[x]))).toString());
        xml_config = Common.replace_str(xml_config, "%initial_hosts%", "");
        if (xml_config.indexOf("TUNNEL") >= 0) {
          GossipRouter grouter = new GossipRouter(listen_ports[x]);
          this.grouters.addElement(grouter);
        } 
        JChannel channel = new JChannel(new ByteArrayInputStream(xml_config.getBytes()));
        this.channels.addElement(channel);
        this.xmls.addElement(xml_config);
      } 
      int waitingIntervals = 1000;
      while (true) {
        while (queue.size() <= 0) {
          if (!this.starting)
            if (waitingIntervals++ >= 5) {
              int i;
              for (i = 0; i < this.channels.size(); i++) {
                if (this.grouters.size() > 0)
                  ((GossipRouter)this.grouters.elementAt(i)).stop(); 
                if (this.grouters.size() > 0)
                  ((GossipRouter)this.grouters.elementAt(i)).destroy(); 
                ((JChannel)this.channels.elementAt(i)).clearChannelListeners();
                ((JChannel)this.channels.elementAt(i)).disconnect();
                ((JChannel)this.channels.elementAt(i)).close();
              } 
              waitingIntervals = 0;
              for (i = 0; i < this.channels.size(); i++) {
                if (this.grouters.size() > 0) {
                  GossipRouter grouter = new GossipRouter(listen_ports[i]);
                  this.grouters.setElementAt(grouter, i);
                  grouter.start();
                } 
                JChannel channel = new JChannel(new ByteArrayInputStream(this.xmls.elementAt(i).toString().getBytes()));
                ReceiverAdapter ra = new ReceiverAdapter(this) {
                    final CrushFTPDMZ3 this$0;
                    
                    public void receive(Message msg) {
                      try {
                        this.this$0.processResponse((Properties)msg.getObject());
                      } catch (Exception e) {
                        this.this$0.error = e;
                      } 
                    }
                    
                    public void viewAccepted(View new_view) {
                      Log.log("DMZ", 0, "VIEW:" + new_view.getViewId() + ":" + new_view.getMembers());
                    }
                  };
                channel.setReceiver((Receiver)ra);
                channel.setDiscardOwnMessages(true);
                channel.connect("DMZ:" + listen_ports[i]);
                this.channels.setElementAt(channel, i);
              } 
            }  
          if (this.error != null)
            throw this.error; 
          Thread.sleep(1000L);
        } 
        sendCommand(queue.remove(0));
      } 
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
      return;
    } 
  }
  
  public void sendCommand(Properties p) {
    try {
      for (int x = 0; x < this.channels.size(); x++) {
        JChannel channel = this.channels.elementAt(x);
        Message msg = new Message(null, null, p);
        channel.send(msg);
      } 
      Log.log("DMZ", 2, "WROTE:" + p.getProperty("type"));
      if (p.getProperty("need_response", "false").equalsIgnoreCase("true")) {
        p.put("status", "waiting");
        dmzResponses.put(p.getProperty("id"), p);
      } 
    } catch (Exception e) {
      this.error = e;
    } 
  }
  
  private void processResponse(Properties p) throws Exception {
    if (dmzResponses.containsKey(p.getProperty("id", ""))) {
      Properties response = (Properties)dmzResponses.remove(p.getProperty("id"));
      Log.log("DMZ", 0, "READ:IGNORING, ALREADY PROCESSED:" + p.getProperty("type"));
      if (response == null)
        return; 
      response.putAll(p);
      if (p.containsKey("data"))
        response.putAll((Properties)p.get("data")); 
      p = response;
      p.put("status", "done");
    } 
    Log.log("DMZ", 2, "READ:" + p.getProperty("type"));
    if (p.getProperty("type").equalsIgnoreCase("PUT:SERVER_SETTINGS")) {
      synchronized (queue) {
        if (!this.started) {
          this.starting = true;
          System.out.println("DMZ Starting...");
          System.setProperty("java.net.preferIPv4Stack", ipv4);
          try {
          
          } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
          } 
          this.started = true;
        } 
      } 
      p.put("data", new Properties());
      p.put("type", "PUT:DMZ_STARTED");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("PUT:SYSTEM.PROPERTIES")) {
      Properties system_prop = (Properties)p.get("data");
      Common.System2.put(system_prop.getProperty("key"), system_prop.get("val"));
    } else if (p.getProperty("type").equalsIgnoreCase("GET:SERVER_SETTINGS")) {
      p.put("data", ServerStatus.server_settings);
      p.put("type", "RESPONSE");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("GET:SERVER_INFO")) {
      Properties request = (Properties)p.get("data");
      Properties si = new Properties();
      if (this.started)
        si = (Properties)ServerStatus.thisObj.server_info.clone(); 
      si.remove("plugins");
      if (request != null && (request.getProperty("key", "").equals("server_info") || request.getProperty("command", "").equals("getStatHistory"))) {
        si.remove("user_list");
        si.remove("recent_user_list");
      } else {
        Vector user_list = (Vector)((Vector)si.get("user_list")).clone();
        si.put("user_list", user_list);
        for (int x = 0; x < user_list.size(); x++) {
          Properties user_info = (Properties)((Properties)user_list.elementAt(x)).clone();
          user_list.setElementAt(user_info, x);
          user_info.remove("session_commands");
          user_info.remove("session");
        } 
      } 
      p.put("data", si);
      p.put("type", "RESPONSE");
      queue.addElement(p);
    } else if (p.getProperty("type").equalsIgnoreCase("RUN:INSTANCE_ACTION")) {
      p.put("data", AdminControls.runInstanceAction((Properties)p.get("data")));
      p.put("type", "RESPONSE");
      queue.addElement(p);
    } 
  }
}
