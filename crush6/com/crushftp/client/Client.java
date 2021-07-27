package com.crushftp.client;

import com.crushftp.tunnel2.Tunnel2;
import com.crushftp.tunnel3.StreamController;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URI;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class Client implements Runnable {
  public static final String version = "1.4.11";
  
  String current_dest_dir = "/";
  
  String current_source_dir = null;
  
  Vector logQueue = new Vector();
  
  public BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
  
  String starting_url1 = null;
  
  String starting_url2 = null;
  
  Vector retry_active = new Vector();
  
  Vector source_used = new Vector();
  
  Vector source_free = new Vector();
  
  Vector destination_used = new Vector();
  
  Vector destination_free = new Vector();
  
  public Properties source_credentials = new Properties();
  
  public Properties destination_credentials = new Properties();
  
  boolean source_logged_in = false;
  
  boolean destination_logged_in = false;
  
  Properties prefs = new Properties();
  
  Variables vars = new Variables();
  
  boolean local_echo = false;
  
  Vector recent_transfers_upload = new Vector();
  
  Vector recent_transfers_download = new Vector();
  
  Vector after_next_command = new Vector();
  
  boolean abort_wait = false;
  
  Vector messages = null;
  
  boolean dual_log = true;
  
  boolean interactive = true;
  
  public Vector pending_transfer_queue = new Vector();
  
  Vector tunnel_log = new Vector();
  
  SimpleDateFormat log_sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");
  
  Properties source_config = new Properties();
  
  Properties dest_config = new Properties();
  
  static final String unique_client_id = Common.makeBoundary(10);
  
  public Client(Vector messages) {
    this.messages = messages;
  }
  
  public Client() {}
  
  public static void main(String[] args) {
    System.out.println("CrushClient:1.4.11:http://www.crushftp.com/crush7wiki/Wiki.jsp?page=CrushClient");
    Client client = new Client();
    client.setupSignalHandler();
    if (args != null && args.length > 0 && args[0].equalsIgnoreCase("SCRIPT")) {
      try {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int x = 1; x < args.length; x++) {
          if (!args[x].trim().equals("")) {
            Common.streamCopier(new FileInputStream(args[x]), baos, false, true, false);
            baos.write("\r\n".getBytes());
          } 
        } 
        baos.close();
        client.br.close();
        client.br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
        client.local_echo = true;
      } catch (Exception e) {
        e.printStackTrace();
      } 
    } else if (args != null && args.length > 0 && args[0].equalsIgnoreCase("INLINE_SCRIPT")) {
      try {
        client.br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream((String.valueOf(args[1]) + ";").replaceAll(";", "\r\n").getBytes("UTF8"))));
        client.local_echo = true;
      } catch (Exception e) {
        e.printStackTrace();
      } 
    } else {
      if (args != null && args.length > 0)
        client.starting_url1 = args[0]; 
      if (args != null && args.length > 1)
        client.starting_url2 = args[1]; 
    } 
    (new Thread(new Runnable(client) {
          private final Client val$client;
          
          public void run() {
            Thread.currentThread().setName("Logging handler");
            int aborts = 0;
            while (true) {
              while (this.val$client.logQueue.size() <= 0) {
                try {
                  if (StreamController.old_msg != null) {
                    if (aborts++ < 3)
                      Worker.startWorker((Runnable)new Object(this, this.val$client)); 
                    this.val$client.line(StreamController.old_msg);
                    Thread.sleep(3000L);
                  } 
                  Thread.sleep(100L);
                } catch (Exception exception) {}
              } 
              if (this.val$client.prefs.getProperty("client_debug", "false").equals("true")) {
                this.val$client.line(this.val$client.logQueue.remove(0));
                continue;
              } 
              this.val$client.logQueue.remove(0);
            } 
          }
        })).start();
    (new Thread(client)).start();
  }
  
  public synchronized GenericClient getClient(boolean source) throws Exception {
    if (source) {
      if (this.source_free.size() > 0) {
        GenericClient genericClient1 = this.source_free.remove(0);
        this.source_used.addElement(genericClient1);
        return genericClient1;
      } 
      VRL vRL = (VRL)this.source_credentials.get("vrl");
      line("Creating new client (src):" + vRL.getProtocol() + "://" + vRL.getHost() + ((vRL.getPort() > -1) ? (":" + vRL.getPort()) : "") + "/");
      GenericClient genericClient = Common.getClient(Common.getBaseUrl(vRL.toString()), "CrushClient:", this.logQueue);
      if (this.prefs.getProperty("shared_session", "false").equals("true"))
        genericClient.setConfigObj((Properties)this.source_config.clone()); 
      genericClient.login(this.source_credentials.getProperty("username"), this.source_credentials.getProperty("password"), unique_client_id);
      if (genericClient.getConfig("crushAuth") != null)
        this.source_config.put("crushAuth", genericClient.getConfig("crushAuth")); 
      this.source_used.addElement(genericClient);
      return genericClient;
    } 
    if (this.destination_free.size() > 0) {
      GenericClient genericClient = this.destination_free.remove(0);
      this.destination_used.addElement(genericClient);
      return genericClient;
    } 
    VRL vrl = (VRL)this.destination_credentials.get("vrl");
    line("Creating new client (dst):" + vrl.getProtocol() + "://" + vrl.getHost() + ((vrl.getPort() > -1) ? (":" + vrl.getPort()) : "") + "/");
    GenericClient c = Common.getClient(Common.getBaseUrl(vrl.toString()), "CrushClient:", this.logQueue);
    if (this.prefs.getProperty("shared_session", "false").equals("true"))
      c.setConfigObj((Properties)this.dest_config.clone()); 
    c.login(this.destination_credentials.getProperty("username"), this.destination_credentials.getProperty("password"), unique_client_id);
    if (c.getConfig("crushAuth") != null)
      this.dest_config.put("crushAuth", c.getConfig("crushAuth")); 
    this.destination_used.addElement(c);
    return c;
  }
  
  public GenericClient freeClient(GenericClient c) {
    if (c == null)
      return null; 
    c.setConfig("transfer_direction", null);
    c.setConfig("transfer_path", null);
    c.setConfig("transfer_path_src", null);
    c.setConfig("transfer_path_dst", null);
    c.setConfig("transfer_stats", null);
    c.setConfig("transfer_start", null);
    c.setConfig("transfer_bytes_total", null);
    c.setConfig("transfer_bytes", null);
    if (this.source_used.indexOf(c) >= 0) {
      this.source_used.remove(c);
      this.source_free.addElement(c);
    } else if (this.destination_used.indexOf(c) >= 0) {
      this.destination_used.remove(c);
      this.destination_free.addElement(c);
    } 
    return null;
  }
  
  public void killClient(GenericClient c) {
    if (c == null)
      return; 
    this.source_used.remove(c);
    this.source_free.remove(c);
    this.destination_used.remove(c);
    this.destination_free.remove(c);
    try {
      c.logout();
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
  
  public void run() {
    try {
      this.current_source_dir = String.valueOf((new File("./")).getCanonicalPath().replace('\\', '/')) + "/";
      if (!this.current_source_dir.startsWith("/"))
        this.current_source_dir = "/" + this.current_source_dir; 
      if (this.starting_url1 != null)
        process_command("connect " + this.starting_url1, false); 
      if (this.starting_url2 != null) {
        process_command("lconnect " + this.starting_url2, false);
      } else {
        process_command("lconnect file://" + (new File("./")).getCanonicalPath().replace('\\', '/') + "/", false);
      } 
      Thread.currentThread().setName("Command processor");
      while (true) {
        print_prompt();
        String command = null;
        boolean echo = this.local_echo;
        if (this.after_next_command.size() > 0) {
          command = this.after_next_command.remove(0).toString();
          if (command.equalsIgnoreCase("SKIP")) {
            command = null;
          } else {
            echo = true;
          } 
        } 
        if (command == null)
          command = this.br.readLine(); 
        if (command == null) {
          this.br = new BufferedReader(new InputStreamReader(System.in));
          command = this.br.readLine();
        } 
        try {
          Thread.currentThread().setName("Command processor:" + command);
          process_command(command, echo);
        } catch (Exception e) {
          e.printStackTrace();
        } 
      } 
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
      return;
    } 
  }
  
  public void run_transfers() {
    try {
      while (true) {
        Thread.currentThread().setName("Transfer thread.");
        if (this.pending_transfer_queue.size() > 0) {
          String command = null;
          synchronized (this.pending_transfer_queue) {
            if (this.pending_transfer_queue.size() > 0)
              command = this.pending_transfer_queue.remove(0).toString(); 
          } 
          try {
            if (command != null) {
              Thread.currentThread().setName("Transfer thread:" + command);
              process_command(command, true);
            } 
          } catch (Exception e) {
            e.printStackTrace();
          } 
          continue;
        } 
        Thread.sleep(100L);
      } 
    } catch (Exception e) {
      e.printStackTrace();
      return;
    } 
  }
  
  public String[] parseCommand(String command_str) {
    Vector v = new Vector();
    int last_pos = 0;
    boolean waiting_quote = false;
    boolean skip_next = false;
    boolean drop_space = false;
    boolean delete_backslashes = false;
    for (int x = 0; x < command_str.length(); x++) {
      char c = command_str.charAt(x);
      if (!skip_next && ((c == ' ' && !waiting_quote) || x == command_str.length() - 1)) {
        int pos = x;
        if (x == command_str.length() - 1 && !waiting_quote)
          pos++; 
        if (!drop_space)
          if (delete_backslashes) {
            v.addElement(Common.replace_str(command_str.substring(last_pos, pos), "\\", ""));
          } else {
            v.addElement(command_str.substring(last_pos, pos));
          }  
        last_pos = x + 1;
        drop_space = false;
      } else if (c == '"' && !waiting_quote) {
        last_pos = x + 1;
        waiting_quote = true;
      } else if (c == '"' && waiting_quote) {
        v.addElement(command_str.substring(last_pos, x));
        last_pos = x + 1;
        waiting_quote = false;
        drop_space = true;
      } else if (c == '\\') {
        if (x < command_str.length() - 1 && command_str.charAt(x + 1) == ' ') {
          skip_next = true;
          delete_backslashes = true;
        } 
        continue;
      } 
      skip_next = false;
      continue;
    } 
    String[] command = new String[v.size()];
    for (int i = 0; i < v.size(); i++)
      command[i] = v.elementAt(i).toString(); 
    return command;
  }
  
  public String getArgs(String[] command, int i, boolean slash, boolean source) {
    String the_dir = source ? this.current_source_dir : this.current_dest_dir;
    if (command.length > i) {
      the_dir = command[i].trim().replace('\\', '/');
      if (the_dir.length() > 2 && the_dir.charAt(1) == ':' && !the_dir.startsWith("/"))
        the_dir = "/" + the_dir; 
      if (!the_dir.startsWith("/"))
        the_dir = String.valueOf(source ? this.current_source_dir : this.current_dest_dir) + the_dir; 
      if (slash && !the_dir.endsWith("/"))
        the_dir = String.valueOf(the_dir) + "/"; 
      the_dir = Common.dots(the_dir);
    } 
    if (slash && !the_dir.endsWith("/"))
      the_dir = String.valueOf(the_dir) + "/"; 
    return the_dir;
  }
  
  public void hideInput(StringBuffer controller) {
    (new Thread(new Runnable(this, controller) {
          final Client this$0;
          
          private final StringBuffer val$controller;
          
          public void run() {
            try {
              while (this.val$controller.length() == 0) {
                System.out.print("\b*");
                Thread.sleep(1L);
              } 
            } catch (Exception exception) {}
          }
        })).start();
  }
  
  public void process_command(String command_str, boolean echo, boolean multithreaded) throws Exception {
    if (echo) {
      print_prompt();
      line(command_str);
    } 
    process_command(command_str, multithreaded);
  }
  
  public Object process_command(String command_str, boolean multithreaded) throws Exception {
    boolean source = false;
    if (command_str.length() > 0)
      source = (command_str.toUpperCase().charAt(0) == 'L'); 
    if (source && (command_str.toUpperCase().startsWith("LS") || command_str.toUpperCase().startsWith("LIST")))
      source = false; 
    Enumeration keys = this.prefs.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      command_str = Common.replace_str(command_str, "{" + key + "}", this.prefs.getProperty(key, ""));
    } 
    try {
      command_str = this.vars.replace_vars_line_date(command_str, this.prefs, "{", "}");
      command_str = Common.textFunctions(command_str, "{", "}");
    } catch (Exception e) {
      e.printStackTrace();
    } 
    if (command_str.startsWith("#") || command_str.toUpperCase().startsWith("REM"))
      return null; 
    if (command_str.toUpperCase().startsWith("ECHO ")) {
      line(command_str.substring(command_str.indexOf(" ") + 1));
      return null;
    } 
    String[] command = parseCommand(command_str);
    if (command_str.endsWith("&")) {
      command = parseCommand(command_str.substring(0, command_str.length() - 1));
      if (this.prefs.getProperty("multithreaded", "true").equals("true")) {
        String the_dir1 = getArgs(command, 1, false, true);
        String the_dir2 = getArgs(command, 2, false, false);
        String command_str_f = String.valueOf(command[0]) + " \"" + the_dir1 + "\" \"" + the_dir2 + "\"";
        Worker.startWorker(new Runnable(this, command_str_f) {
              final Client this$0;
              
              private final String val$command_str_f;
              
              public void run() {
                try {
                  this.this$0.process_command(this.val$command_str_f, true);
                } catch (Exception e) {
                  e.printStackTrace();
                } 
              }
            });
        return null;
      } 
    } 
    if (command.length == 0)
      return null; 
    Properties credentials = source ? this.source_credentials : this.destination_credentials;
    if (command[0].toUpperCase().startsWith("SET") || command[0].toUpperCase().startsWith("LSET")) {
      this.prefs.put(command[1], command[2]);
      line(String.valueOf(command[1]) + " set to " + command[2]);
    } else if (command[0].toUpperCase().startsWith("QUEUE")) {
      String real_q_id = "Q:" + command[1];
      String q_command = command[2];
      if (q_command.equalsIgnoreCase("ADD") && !this.prefs.containsKey(real_q_id))
        this.prefs.put(real_q_id, ""); 
      keys = this.prefs.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.startsWith("Q:") && (real_q_id.equalsIgnoreCase("Q:all") || key.equalsIgnoreCase(real_q_id))) {
          String q_id = key;
          String queue = this.prefs.getProperty(q_id, "");
          if (q_command.equalsIgnoreCase("ADD")) {
            queue = String.valueOf(queue) + command_str.substring(command_str.indexOf(" ", command_str.indexOf(" ", 7) + 1) + 1) + "\r\n";
          } else if (q_command.equalsIgnoreCase("CLEAR") || q_command.equalsIgnoreCase("RESET")) {
            queue = "";
          } else if (q_command.equalsIgnoreCase("EXECUTE") || q_command.equalsIgnoreCase("RUN")) {
            String queue_f = queue;
            Thread t = new Thread(new Runnable(this, queue_f) {
                  final Client this$0;
                  
                  private final String val$queue_f;
                  
                  public void run() {
                    try {
                      BufferedReader br2 = new BufferedReader(new StringReader(this.val$queue_f));
                      String data = "";
                      while ((data = br2.readLine()) != null)
                        this.this$0.process_command(data, false); 
                    } catch (Exception e) {
                      e.printStackTrace();
                    } 
                  }
                });
            t.setName("Running queue:" + q_id);
            t.start();
          } 
          this.prefs.put(q_id, queue);
        } 
      } 
    } else {
      if (command[0].toUpperCase().startsWith("CONNECT") || command[0].toUpperCase().startsWith("LCONNECT")) {
        if (source ? !this.source_logged_in : !this.destination_logged_in) {
          if (command[1].equalsIgnoreCase("tunnel"))
            command[1] = "http://" + VRL.vrlEncode(this.prefs.getProperty("last_tunnel_username")) + ":" + VRL.vrlEncode(this.prefs.getProperty("last_tunnel_password")) + "@127.0.0.1:" + this.prefs.getProperty("last_tunnel_port") + "/"; 
          if (!command[1].endsWith("/"))
            command[1] = String.valueOf(command[1]) + "/"; 
          VRL vrl = new VRL(command[1]);
          if (source) {
            this.current_source_dir = vrl.getPath();
          } else {
            this.current_dest_dir = vrl.getPath();
          } 
          String username = vrl.getUsername();
          if ((username == null || username.equals("")) && !vrl.getProtocol().equalsIgnoreCase("file")) {
            last_line_prompt = "message";
            System.out.print("(" + vrl + ") Username: ");
            username = this.br.readLine();
          } 
          String password = vrl.getPassword();
          if ((password == null || password.equals("")) && !vrl.getProtocol().equalsIgnoreCase("file")) {
            last_line_prompt = "message";
            System.out.print("(" + vrl + ") Password: ");
            StringBuffer controller = new StringBuffer();
            hideInput(controller);
            password = this.br.readLine();
            controller.append("done");
          } 
          credentials.put("vrl", vrl);
          credentials.put("username", username);
          credentials.put("password", password);
          GenericClient c = null;
          try {
            if (source) {
              this.source_config = new Properties();
            } else {
              this.dest_config = new Properties();
            } 
            c = getClient(source);
            line("Connected to " + vrl.safe());
            if (vrl.getProtocol().toUpperCase().startsWith("FTP:"))
              c.setConfig("pasv", "true"); 
            freeClient(c);
            if (source) {
              this.source_logged_in = true;
            } else {
              this.destination_logged_in = true;
            } 
            line("Connected to:" + vrl.safe());
            return "true";
          } catch (Exception e) {
            e.printStackTrace();
            line("Login failed: " + e);
            GenericClient c_tmp = c;
            (new Thread(new Runnable(this, c_tmp) {
                  final Client this$0;
                  
                  private final GenericClient val$c_tmp;
                  
                  public void run() {
                    try {
                      this.this$0.killClient(this.val$c_tmp);
                    } catch (Exception exception) {}
                  }
                })).start();
            c = null;
          } 
        } else {
          line("Already connected, disconnect first.");
        } 
        return "false";
      } 
      if (command[0].toUpperCase().startsWith("DELAY")) {
        Thread.sleep(Integer.parseInt(command[1]));
      } else if (command[0].toUpperCase().startsWith("QUI") || command[0].toUpperCase().startsWith("BYE") || command[0].toUpperCase().startsWith("LQUI") || command[0].toUpperCase().startsWith("LBYE") || command[0].toUpperCase().startsWith("TERMINATE")) {
        StringBuffer status = new StringBuffer();
        Worker.startWorker(new Runnable(this, status) {
              final Client this$0;
              
              private final StringBuffer val$status;
              
              public void run() {
                try {
                  while (this.this$0.source_used.size() > 0) {
                    GenericClient c_tmp = this.this$0.source_used.remove(0);
                    c_tmp.close();
                    this.this$0.freeClient(c_tmp);
                  } 
                  while (this.this$0.destination_used.size() > 0) {
                    GenericClient c_tmp = this.this$0.destination_used.remove(0);
                    c_tmp.close();
                    this.this$0.freeClient(c_tmp);
                  } 
                  while (this.this$0.source_free.size() > 0) {
                    GenericClient c_tmp = this.this$0.source_free.remove(0);
                    this.this$0.killClient(c_tmp);
                  } 
                  while (this.this$0.destination_free.size() > 0) {
                    GenericClient c_tmp = this.this$0.destination_free.remove(0);
                    this.this$0.killClient(c_tmp);
                  } 
                  this.this$0.source_logged_in = false;
                  this.this$0.destination_logged_in = false;
                  this.this$0.line("Logged out.");
                  this.this$0.line("Goodbye.");
                } catch (Exception e) {
                  e.printStackTrace();
                } 
                this.val$status.append("done");
              }
            });
        int loops = 0;
        while (loops++ < 50 && status.length() == 0)
          Thread.sleep(100L); 
        if (this.interactive || command[0].toUpperCase().startsWith("TERMINATE"))
          System.exit(0); 
      } else {
        if (command[0].toUpperCase().startsWith("DIS") || command[0].toUpperCase().startsWith("LDIS")) {
          if (source ? !this.source_logged_in : !this.destination_logged_in)
            return line("Not connected."); 
          while (source && this.source_used.size() > 0) {
            GenericClient c_tmp = this.source_used.remove(0);
            c_tmp.close();
            freeClient(c_tmp);
          } 
          while (!source && this.destination_used.size() > 0) {
            GenericClient c_tmp = this.destination_used.remove(0);
            c_tmp.close();
            freeClient(c_tmp);
          } 
          while (source && this.source_free.size() > 0) {
            GenericClient c_tmp = this.source_free.remove(0);
            killClient(c_tmp);
          } 
          while (!source && this.destination_free.size() > 0) {
            GenericClient c_tmp = this.destination_free.remove(0);
            killClient(c_tmp);
          } 
          if (source) {
            this.source_logged_in = false;
          } else {
            this.destination_logged_in = false;
          } 
          return line("Logged out.");
        } 
        if (command[0].toUpperCase().startsWith("ABOR")) {
          this.prefs.put("aborting", "true");
          while (this.source_used.size() > 0) {
            try {
              GenericClient c_tmp = this.source_used.remove(0);
              c_tmp.close();
              freeClient(c_tmp);
            } catch (Exception e) {
              e.printStackTrace();
            } 
          } 
          while (this.destination_used.size() > 0) {
            try {
              GenericClient c_tmp = this.destination_used.remove(0);
              c_tmp.close();
              freeClient(c_tmp);
            } catch (Exception e) {
              e.printStackTrace();
            } 
          } 
          this.pending_transfer_queue.removeAllElements();
          this.retry_active.removeAllElements();
          line("Transfers aborted if any were running.");
          Thread.sleep(900L);
          print_prompt();
          this.prefs.remove("aborting");
        } else if (command[0].toUpperCase().startsWith("AGENT")) {
          if (AgentUI.thisObj == null) {
            this.messages = new Vector();
            this.dual_log = true;
            Worker.startWorker(new Runnable(this) {
                  final Client this$0;
                  
                  public void run() {
                    AgentUI.main(new String[0]);
                  }
                });
            while (AgentUI.thisObj == null && !this.prefs.containsKey("aborting"))
              Thread.sleep(100L); 
            AgentUI.thisObj.clients.put("command_line", this);
            line("WebUI started on port 33333.");
            Desktop.getDesktop().browse(new URI("http://127.0.0.1:33333/"));
          } else {
            line("WebUI already started on port 33333.");
          } 
          print_prompt();
        } else if (command[0].toUpperCase().startsWith("SERVICE")) {
          File jar = new File("win_service.jar");
          if (!jar.exists()) {
            line("Could not locate 'win_service.jar' in the current folder with CrushTunnel.jar.");
            line("Please copy this file from a CrushFTP installation's CrushFTP7_PC/plugins/lib/ folder and try again or");
            line("download a copy from https://www.crushftp.com/early7/CrushFTP7_PC/plugins/lib/win_service.jar");
            print_prompt();
          } else {
            AgentUI.writeFileFromJar("service/lib/core/commons/commons-cli-2-SNAPSHOT.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/commons/commons-collections-3.2.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/commons/commons-configuration-1.8.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/commons/commons-io-1.3.1.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/commons/commons-lang-2.4.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/commons/commons-logging-1.1.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/commons/commons-vfs2-2.0.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/groovy/groovy-all-1.8.6.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/jna/jna-3.4.1.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/jna/platform-3.4.1.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/netty/netty-3.5.1.Final.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/regex/jrexx-1.1.1.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/lib/core/yajsw/ahessian.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/wrapper.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            AgentUI.writeFileFromJar("service/wrapperApp.jar", String.valueOf(System.getProperty("user.home")) + "/CrushClient/", jar);
            String wrapperStr = "wrapper.java.command=%JAVA%\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.working.dir=%WORKING.DIR%\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.java.app.mainclass=com.crushftp.client.AgentUI\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.console.visible=false\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.console.title=CrushClient\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.ntservice.name=CrushClient\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.ntservice.displayname=CrushClient\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.ntservice.description=CrushClient\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.tray=false\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.logfile.format=LPNTM\\r\\n\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.logfile=wrapper.log\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.logfile.maxsize=10m\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.logfile.maxfiles=10\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.on_exit.0=SHUTDOWN\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.on_exit.default=RESTART\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.java.classpath.1=service/CrushTunnel.jar\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.java.additional.1=-Xmx512m\r\n";
            wrapperStr = String.valueOf(wrapperStr) + "wrapper.java.additional.2=-Duser.home=%USER.HOME%\r\n";
            wrapperStr = Common.replace_str(wrapperStr, "%WORKING.DIR%", String.valueOf(Common.replace_str((new File(String.valueOf(System.getProperty("user.home")) + "/CrushClient/")).getCanonicalPath(), "\\", "\\\\")) + "\\" + "\\");
            wrapperStr = Common.replace_str(wrapperStr, "%USER.HOME%", String.valueOf(Common.replace_str((new File(System.getProperty("user.home"))).getCanonicalPath(), "\\", "\\\\")) + "\\" + "\\");
            String javaPath = String.valueOf(System.getProperty("java.home")) + "\\bin\\java";
            wrapperStr = Common.replace_str(wrapperStr, "%JAVA%", Common.replace_str(javaPath, "\\", "\\\\"));
            RandomAccessFile wrapper = new RandomAccessFile(String.valueOf(System.getProperty("user.home")) + "/CrushClient/service/wrapper.conf", "rw");
            wrapper.setLength(0L);
            wrapper.seek(0L);
            wrapper.write(wrapperStr.getBytes("UTF8"));
            wrapper.close();
            (new File(String.valueOf(System.getProperty("user.home")) + "/CrushClient/service/tmp")).mkdirs();
            String bat = "setlocal\r\n";
            bat = String.valueOf(bat) + "set TEMP=" + (new File(String.valueOf(System.getProperty("user.home")) + "/CrushClient/service/tmp/")).getCanonicalPath() + "\r\n";
            bat = String.valueOf(bat) + "set TMP=" + (new File(String.valueOf(System.getProperty("user.home")) + "/CrushClient/service/tmp/")).getCanonicalPath() + "\r\n";
            bat = String.valueOf(bat) + "\"" + javaPath + "\" -Xmx30m -jar wrapper.jar -i wrapper.conf\r\n";
            bat = String.valueOf(bat) + "net start \"CrushClient\"\r\n";
            RandomAccessFile out = new RandomAccessFile(String.valueOf(System.getProperty("user.home")) + "/CrushClient/service/installService.bat", "rw");
            out.setLength(0L);
            out.write(bat.getBytes());
            out.close();
            bat = Common.replace_str(bat, " -i ", " -r ");
            out = new RandomAccessFile(String.valueOf(System.getProperty("user.home")) + "/CrushClient/service/removeService.bat", "rw");
            out.setLength(0L);
            out.write(bat.getBytes());
            out.close();
            Common.streamCopier(new FileInputStream("./CrushTunnel.jar"), new FileOutputStream(String.valueOf(System.getProperty("user.home")) + "/CrushClient/service/CrushTunnel.jar"));
            Desktop.getDesktop().open(new File(String.valueOf(System.getProperty("user.home")) + "/CrushClient/service/"));
            line("Service ready to be installed.  Run installService.bat.");
            print_prompt();
          } 
        } else {
          GenericClient c = null;
          try {
            c = getClient(source);
          } catch (NullPointerException nullPointerException) {}
          try {
            if (command[0].toUpperCase().startsWith("STAT") || command[0].toUpperCase().startsWith("LSTAT")) {
              if (source ? !this.source_logged_in : !this.destination_logged_in) {
                line("Not connected.");
                return null;
              } 
              String the_dir = getArgs(command, 1, true, source);
              Properties stat = null;
              try {
                stat = c.stat(the_dir);
              } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + e;
              } 
              last_line_prompt = "message";
              if (stat != null)
                return line(format_ls_la(stat)); 
              return line("Error: Not found.");
            } 
            if (command[0].toUpperCase().startsWith("DIR") || command[0].toUpperCase().startsWith("LIST") || command[0].toUpperCase().startsWith("LS") || command[0].toUpperCase().startsWith("LDIR") || command[0].toUpperCase().startsWith("LLIST") || command[0].toUpperCase().startsWith("LLS")) {
              if (source ? !this.source_logged_in : !this.destination_logged_in) {
                line("Not connected.");
                return null;
              } 
              String the_dir = getArgs(command, 1, true, source);
              Vector list = new Vector();
              try {
                c.list(the_dir, list);
              } catch (Exception e) {
                e.printStackTrace();
                return "ERROR:" + e;
              } 
              last_line_prompt = "message";
              for (int x = 0; x < list.size(); x++)
                line(format_ls_la(list.elementAt(x))); 
              return list;
            } 
            if (command[0].toUpperCase().startsWith("PASV") || command[0].toUpperCase().startsWith("LPASV")) {
              if (source ? !this.source_logged_in : !this.destination_logged_in) {
                line("Not connected.");
                return null;
              } 
              if (c.getConfig("pasv") != null) {
                c.setConfig("pasv", (new StringBuffer(String.valueOf(!c.getConfig("pasv").toString().equals("true")))).toString());
                return line("Passive enabled: " + c.getConfig("pasv"));
              } 
              return line("The protocol does not have or need passive mode.");
            } 
            if (command[0].toUpperCase().startsWith("CD") || command[0].toUpperCase().startsWith("CWD") || command[0].toUpperCase().startsWith("LCD") || command[0].toUpperCase().startsWith("LCWD")) {
              if (source ? !this.source_logged_in : !this.destination_logged_in) {
                line("Not connected.");
                return null;
              } 
              String the_dir = getArgs(command, 1, true, source);
              Properties stat = null;
              if (!the_dir.equals("/"))
                stat = c.stat(the_dir); 
              if (stat == null && !the_dir.equals("/")) {
                if (this.local_echo)
                  this.br.close(); 
                return line(String.valueOf(command[0]) + " \"" + the_dir + "\": No such file or directory.");
              } 
              if (source) {
                this.current_source_dir = the_dir;
              } else {
                this.current_dest_dir = the_dir;
              } 
              return line("\"" + the_dir + "\" CWD command successful.");
            } 
            if (command[0].toUpperCase().startsWith("PWD") || command[0].toUpperCase().startsWith("LPWD")) {
              if (source ? !this.source_logged_in : !this.destination_logged_in) {
                line("Not connected.");
                return null;
              } 
              return line("\"" + (source ? this.current_source_dir : this.current_dest_dir) + "\" PWD command successful.");
            } 
            if (command[0].toUpperCase().startsWith("DEL") || command[0].toUpperCase().startsWith("RM") || command[0].toUpperCase().startsWith("LDEL") || command[0].toUpperCase().startsWith("LRM")) {
              if (source ? !this.source_logged_in : !this.destination_logged_in) {
                line("Not connected.");
                return null;
              } 
              String the_dir = getArgs(command, 1, false, source);
              if (the_dir.indexOf("*") >= 0) {
                Vector list = new Vector();
                try {
                  String parent_dir = source ? this.current_source_dir : this.current_dest_dir;
                  c.list(parent_dir, list);
                  int count = 0;
                  int x;
                  for (x = 0; x < list.size(); x++) {
                    String name = ((Properties)list.elementAt(x)).getProperty("name");
                    if (Common.do_search(command[1], name, false, 0))
                      count++; 
                  } 
                  line("\"" + count + "\" items to delete...");
                  for (x = 0; x < list.size(); x++) {
                    String name = ((Properties)list.elementAt(x)).getProperty("name");
                    if (Common.do_search(command[1], name, false, 0))
                      try {
                        c.delete(String.valueOf(parent_dir) + name);
                        line("\"" + name + "\" delete command successful.");
                      } catch (Exception e) {
                        e.printStackTrace();
                      }  
                  } 
                } catch (Exception e) {
                  e.printStackTrace();
                } 
              } else {
                boolean ok = c.delete(the_dir);
                if (!ok)
                  return line("\"" + the_dir + "\": Delete failed."); 
                return line("\"" + the_dir + "\" delete command successful.");
              } 
            } else {
              if (command[0].toUpperCase().startsWith("MKD") || command[0].toUpperCase().startsWith("LMKD")) {
                if (source ? !this.source_logged_in : !this.destination_logged_in) {
                  line("Not connected.");
                  return null;
                } 
                String the_dir = getArgs(command, 1, true, source);
                boolean ok = c.makedir(the_dir);
                if (!ok) {
                  if (this.local_echo)
                    this.br.close(); 
                  return line("\"" + the_dir + "\": MKD failed.");
                } 
                return line("\"" + the_dir + "\" MKD command successful.");
              } 
              if (command[0].toUpperCase().startsWith("QUOTE") || command[0].toUpperCase().startsWith("LQUOTE")) {
                if (source ? !this.source_logged_in : !this.destination_logged_in) {
                  line("Not connected.");
                  return null;
                } 
                if (c instanceof FTPClient)
                  return line(((FTPClient)c).quote(command_str.substring(command_str.indexOf(" ") + 1))); 
                return line("The quote command can only be used with the FTP protocol. (ftp:// , ftps://, ftpes://)");
              } 
              if (command[0].toUpperCase().startsWith("REN") || command[0].toUpperCase().startsWith("MV") || command[0].toUpperCase().startsWith("LREN") || command[0].toUpperCase().startsWith("LMV")) {
                if (source ? !this.source_logged_in : !this.destination_logged_in) {
                  line("Not connected.");
                  return null;
                } 
                String the_dir1 = getArgs(command, 1, false, source);
                String the_dir2 = getArgs(command, 2, false, source);
                boolean ok = c.rename(the_dir1, the_dir2);
                if (!ok)
                  return line("\"" + the_dir1 + "\" -> \"" + the_dir2 + "\": rename failed."); 
                return line("\"" + the_dir1 + "\" -> \"" + the_dir2 + "\": rename command successful.");
              } 
              if (command[0].toUpperCase().startsWith("MDTM") || command[0].toUpperCase().startsWith("LMDTM")) {
                if (source ? !this.source_logged_in : !this.destination_logged_in) {
                  line("Not connected.");
                  return null;
                } 
                String the_dir = getArgs(command, 1, false, source);
                String date = command[2];
                SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                boolean ok = c.mdtm(the_dir, sdf_yyyyMMddHHmmss.parse(date).getTime());
                if (!ok)
                  return line("\"" + the_dir + "\" : mdtm failed. " + date); 
                return line("\"" + the_dir + "\": mdtm command successful. " + date);
              } 
              if (command[0].toUpperCase().startsWith("AFTER") || command[0].toUpperCase().startsWith("LAFTER")) {
                if (source ? !this.source_logged_in : !this.destination_logged_in) {
                  line("Not connected.");
                  return null;
                } 
                String s = command_str.substring(command[0].length() + 1).trim();
                this.after_next_command.addElement(s);
                this.after_next_command.insertElementAt("SKIP", 0);
                return line("\"" + s + "\": Added to after queue.");
              } 
              if (command[0].toUpperCase().startsWith("CONFIG") || command[0].toUpperCase().startsWith("LCONFIG")) {
                if (source ? !this.source_logged_in : !this.destination_logged_in) {
                  line("Not connected.");
                  return null;
                } 
                c.setConfig(command[1], command[2]);
                return line("\"" + command[1] + "\" -> \"" + command[2] + "\": config command successful.");
              } 
              if (command[0].toUpperCase().startsWith("TUNNEL") || command[0].toUpperCase().startsWith("LTUNNEL")) {
                if (command[1].equalsIgnoreCase("stop")) {
                  Vector tunnel_list = (Vector)this.prefs.get("tunnel_list");
                  int i = Integer.parseInt(command[2]);
                  if (tunnel_list.size() >= i) {
                    Properties tunnel = null;
                    if (tunnel_list.elementAt(i - 1) instanceof StreamController) {
                      tunnel = ((StreamController)tunnel_list.elementAt(i - 1)).tunnel;
                      ((StreamController)tunnel_list.elementAt(i - 1)).startStopTunnel(false);
                    } else if (tunnel_list.elementAt(i - 1) instanceof Tunnel2) {
                      tunnel = ((Tunnel2)tunnel_list.elementAt(i - 1)).tunnel;
                      ((Tunnel2)tunnel_list.elementAt(i - 1)).startStopTunnel(false);
                    } 
                    tunnel.put("tunnel_status", "stopped");
                    return line("Tunnel id " + i + " stopped, local port " + tunnel.getProperty("localPort", "0") + " closed.");
                  } 
                  return line("No such tunnel id:" + i);
                } 
                if (command[1].equalsIgnoreCase("list")) {
                  Vector tunnel_list = (Vector)this.prefs.get("tunnel_list");
                  if (tunnel_list == null)
                    tunnel_list = new Vector(); 
                  for (int x = 0; x < tunnel_list.size(); x++) {
                    Properties tunnel = null;
                    if (tunnel_list.elementAt(x) instanceof StreamController) {
                      tunnel = ((StreamController)tunnel_list.elementAt(x)).tunnel;
                    } else if (tunnel_list.elementAt(x) instanceof Tunnel2) {
                      tunnel = ((Tunnel2)tunnel_list.elementAt(x)).tunnel;
                    } 
                    if (tunnel == null) {
                      line("id=" + (x + 1));
                    } else {
                      line("id=" + (x + 1) + ", " + tunnel.getProperty("tunnel_version", "tunnel2") + ", local port:" + tunnel.getProperty("localPort", "0") + ", status:" + tunnel.getProperty("tunnel_status"));
                    } 
                  } 
                  return line("Total tunnels:" + tunnel_list.size());
                } 
                if (command[1].equalsIgnoreCase("log")) {
                  while (this.tunnel_log.size() > 0)
                    line(this.tunnel_log.remove(0).toString()); 
                  print_prompt();
                } else {
                  if (command[1].equalsIgnoreCase("trust")) {
                    Common.trustEverything();
                    return line("Tunnels no longer validate SSL certificates.");
                  } 
                  if (command[1].equalsIgnoreCase("start"))
                    return line("Tunnels cannot be restarted once stopped, please issue 'tunnel url' again to start a tunnel."); 
                  VRL vrl = new VRL(command[1]);
                  String tunnel_username = vrl.getUsername();
                  if (tunnel_username == null || tunnel_username.equals("")) {
                    last_line_prompt = "message";
                    System.out.print("Tunnel Username: ");
                    tunnel_username = this.br.readLine();
                  } 
                  this.prefs.put("last_tunnel_username", tunnel_username);
                  String tunnel_username_f = tunnel_username;
                  String tunnel_password = vrl.getPassword();
                  if (tunnel_password == null || tunnel_password.equals("")) {
                    last_line_prompt = "message";
                    System.out.print("Tunnel Password: ");
                    StringBuffer controller = new StringBuffer();
                    hideInput(controller);
                    tunnel_password = this.br.readLine();
                    controller.append("done");
                  } 
                  this.prefs.put("last_tunnel_password", tunnel_password);
                  if (!this.prefs.containsKey("tunnel_list"))
                    this.prefs.put("tunnel_list", new Vector()); 
                  Vector tunnel_list = (Vector)this.prefs.get("tunnel_list");
                  String tunnel_password_f = tunnel_password;
                  try {
                    Tunnel2.setLog(this.tunnel_log);
                    Tunnel2 t = new Tunnel2(vrl.toString(), tunnel_username_f, tunnel_password_f, false);
                    t.startThreads();
                    t.tunnel.put("tunnel_status", "running");
                    if (t.tunnel.getProperty("tunnel_version", "tunnel2").equalsIgnoreCase("tunnel3")) {
                      StreamController sc = new StreamController(vrl.toString(), tunnel_username_f, tunnel_password_f, unique_client_id);
                      tunnel_list.addElement(sc);
                      sc.setLog(this.tunnel_log);
                      sc.startThreads();
                      sc.tunnel.put("tunnel_status", "running");
                    } else {
                      tunnel_list.addElement(t);
                    } 
                    line("Tunnel started (id=" + tunnel_list.size() + ") " + t.tunnel.getProperty("tunnel_version", "tunnel2") + " on local port " + t.tunnel.getProperty("localPort", "0") + ", property count:" + t.tunnel.size());
                    this.prefs.put("last_tunnel_port", t.tunnel.getProperty("localPort", "0"));
                    Thread.sleep(500L);
                  } catch (Exception e) {
                    line("Tunnel failed:" + e);
                    e.printStackTrace();
                  } 
                  print_prompt();
                  Worker.startWorker(new Runnable(this) {
                        final Client this$0;
                        
                        public void run() {
                          while (true) {
                            while (this.this$0.tunnel_log.size() <= 1000) {
                              try {
                                Thread.sleep(1000L);
                              } catch (InterruptedException interruptedException) {}
                            } 
                            this.this$0.tunnel_log.remove(0);
                          } 
                        }
                      });
                } 
              } else if (command[0].toUpperCase().startsWith("CIPHER") || command[0].toUpperCase().startsWith("LCIPHER")) {
                if (command[1].equalsIgnoreCase("list")) {
                  Vector cipher_suites = URLConnection.cipher_suites;
                  if (cipher_suites == null) {
                    line("Cannot get list until a connection ahs been made.");
                  } else {
                    for (int x = 0; x < cipher_suites.size(); x++)
                      line(cipher_suites.elementAt(x)); 
                  } 
                  print_prompt();
                  return cipher_suites;
                } 
                if (command[1].equalsIgnoreCase("set")) {
                  line("Preferred cipher set to: " + command[2]);
                  URLConnection.preferred_cipher = command[2];
                  print_prompt();
                } else if (command[1].equalsIgnoreCase("get")) {
                  line("Last used cipher: " + URLConnection.last_cipher);
                  print_prompt();
                } else if (command[1].equalsIgnoreCase("trust")) {
                  Common.trustEverything();
                  print_prompt();
                  return line("SSL/TLS no longer validate SSL certificates.");
                } 
              } else if (command[0].toUpperCase().startsWith("GET") || command[0].toUpperCase().startsWith("REGET")) {
                if (!this.destination_logged_in) {
                  line("Not connected to dest.");
                  return null;
                } 
                if (!this.source_logged_in) {
                  line("Not connected to source.");
                  return null;
                } 
                freeClient(c);
                Exception transfer_error = null;
                boolean resume = !command[0].toUpperCase().startsWith("GET");
                while (!this.prefs.containsKey("aborting")) {
                  transfer_error = null;
                  c = null;
                  String the_dir = getArgs(command, 1, false, false);
                  String the_dir_source = getArgs(command, 2, false, true);
                  if (the_dir.indexOf("*") >= 0) {
                    Vector list = new Vector();
                    try {
                      startupThreads();
                      String parent_dir = Common.all_but_last(the_dir);
                      GenericClient genericClient = getClient(false);
                      try {
                        genericClient.list(parent_dir, list);
                      } finally {
                        freeClient(genericClient);
                      } 
                      for (int x = 0; x < list.size() && !this.prefs.containsKey("aborting"); x++) {
                        Properties p = list.elementAt(x);
                        if (Common.do_search(Common.last(the_dir), p.getProperty("name"), false, 0))
                          if (p.getProperty("name").indexOf("*") >= 0) {
                            line("Skipping invalid filename for wildcard download:" + p.getProperty("name"));
                          } else {
                            this.pending_transfer_queue.addElement(String.valueOf(command[0]) + " \"" + parent_dir + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\" \"" + the_dir_source + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\"");
                            if (this.prefs.containsKey("aborting")) {
                              list.removeAllElements();
                              this.pending_transfer_queue.removeAllElements();
                              break;
                            } 
                          }  
                      } 
                    } catch (Exception e) {
                      e.printStackTrace();
                      line("Error:" + e);
                    } 
                    print_prompt();
                    break;
                  } 
                  if (the_dir_source.endsWith("/") && !the_dir.endsWith("/"))
                    the_dir_source = String.valueOf(the_dir_source) + Common.last(the_dir); 
                  InputStream in = null;
                  OutputStream out = null;
                  GenericClient c_source = null;
                  GenericClient c_dest = null;
                  long source_size = 0L;
                  try {
                    c_source = getClient(true);
                    c_dest = getClient(false);
                    long start_pos = 0L;
                    Properties source_stat = null;
                    if (!the_dir_source.startsWith("/WebInterface/"))
                      source_stat = c_source.stat(the_dir_source); 
                    if (source_stat != null)
                      source_size = Long.parseLong(source_stat.getProperty("size", "0")); 
                    boolean folder = false;
                    Properties dest_stat = null;
                    if (!the_dir.startsWith("/WebInterface/"))
                      dest_stat = c_dest.stat(the_dir); 
                    if (dest_stat != null && dest_stat.getProperty("type").equalsIgnoreCase("DIR"))
                      folder = true; 
                    if (folder) {
                      startupThreads();
                      if (!the_dir_source.endsWith("/"))
                        the_dir_source = String.valueOf(the_dir_source) + "/"; 
                      if (!the_dir.endsWith("/"))
                        the_dir = String.valueOf(the_dir) + "/"; 
                      process_command("LMKD \"" + the_dir_source + "\"", true);
                      Vector list = new Vector();
                      c_dest.list(the_dir, list);
                      freeClient(c_source);
                      freeClient(c_dest);
                      while (list.size() > 0) {
                        Properties p = list.remove(0);
                        if (this.prefs.containsKey("aborting")) {
                          list.removeAllElements();
                          this.pending_transfer_queue.removeAllElements();
                          break;
                        } 
                        try {
                          if (p.getProperty("type").equalsIgnoreCase("DIR"))
                            process_command("LMKD \"" + the_dir_source + p.getProperty("name") + "\"", true); 
                          this.pending_transfer_queue.addElement(String.valueOf(command[0]) + " \"" + the_dir + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\" \"" + the_dir_source + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\"");
                          while (this.pending_transfer_queue.size() > 10000)
                            Thread.sleep(100L); 
                        } catch (Exception e) {
                          e.printStackTrace();
                          line("Error:" + e);
                        } 
                      } 
                    } else {
                      if (resume)
                        start_pos = source_size; 
                      the_dir = this.vars.replace_vars_line_url(the_dir, null, "{", "}");
                      the_dir_source = this.vars.replace_vars_line_url(the_dir_source, source_stat, "{", "}");
                      the_dir = this.vars.replace_vars_line_date(the_dir, null, "{", "}");
                      the_dir_source = this.vars.replace_vars_line_date(the_dir_source, source_stat, "{", "}");
                      boolean skip = false;
                      if (this.prefs.getProperty("skip_modified_and_size", "true").equals("true") && source_stat != null) {
                        if (dest_stat != null && Math.abs(Long.parseLong(source_stat.getProperty("modified", "0")) - Long.parseLong(dest_stat.getProperty("modified", "50000"))) < 1000L && source_stat.getProperty("size").equals(dest_stat.getProperty("size"))) {
                          skip = true;
                          line(String.valueOf(the_dir) + ":Skipping item because of matching modified date and size (skip_modified_and_size)");
                          print_prompt();
                        } 
                      } else if (this.prefs.getProperty("skip_modified", "false").equals("true") && source_stat != null) {
                        if (dest_stat != null && Math.abs(Long.parseLong(source_stat.getProperty("modified", "0")) - Long.parseLong(dest_stat.getProperty("modified", "50000"))) < 1000L) {
                          skip = true;
                          line(String.valueOf(the_dir) + ":Skipping item because of matching modifed date (skip_modified)");
                          print_prompt();
                        } 
                      } else if (this.prefs.getProperty("skip_size", "false").equals("true") && source_stat != null) {
                        if (dest_stat != null && source_stat.getProperty("size").equals(dest_stat.getProperty("size"))) {
                          skip = true;
                          line(String.valueOf(the_dir) + ":Skipping item because of matching size (skip_size)");
                          print_prompt();
                        } 
                      } 
                      if (skip) {
                        freeClient(c_source);
                        freeClient(c_dest);
                        print_prompt();
                      } else {
                        InputStream in_f = c_dest.download(the_dir, start_pos, -1L, true);
                        in = in_f;
                        c_source.setConfig("transfer_direction", "GET");
                        c_dest.setConfig("transfer_direction", "GET");
                        c_source.setConfig("transfer_path_dst", the_dir_source);
                        c_dest.setConfig("transfer_path_src", the_dir);
                        c_dest.setConfig("transfer_path_dst", the_dir_source);
                        c_source.setConfig("transfer_path_src", the_dir);
                        c_source.setConfig("transfer_stats", "true");
                        c_dest.setConfig("transfer_stats", "true");
                        OutputStream out_f = c_source.upload(the_dir_source, start_pos, true, true);
                        out = out_f;
                        line("Download started:" + the_dir + " -> " + the_dir_source + (resume ? (" : Resuming from position:" + start_pos) : ""));
                        if (multithreaded)
                          print_prompt(); 
                        String the_dir_dest_f = the_dir;
                        String the_dir_source_f = the_dir_source;
                        GenericClient c_source_f = c_source;
                        GenericClient c_dest_f = c_dest;
                        Properties dest_stat_f = dest_stat;
                        long start = System.currentTimeMillis();
                        c_dest_f.setConfig("transfer_start", (new StringBuffer(String.valueOf(start))).toString());
                        if (dest_stat != null)
                          c_dest_f.setConfig("transfer_bytes_total", (new StringBuffer(String.valueOf(dest_stat.getProperty("size")))).toString()); 
                        c_source_f.setConfig("transfer_start", (new StringBuffer(String.valueOf(start))).toString());
                        if (dest_stat != null)
                          c_source_f.setConfig("transfer_bytes_total", (new StringBuffer(String.valueOf(dest_stat.getProperty("size")))).toString()); 
                        c_dest_f.setConfig("transfer_bytes", "0");
                        c_source_f.setConfig("transfer_bytes", "0");
                        if (!multithreaded)
                          Worker.startWorker(new Runnable(this, c_source_f) {
                                final Client this$0;
                                
                                private final GenericClient val$c_source_f;
                                
                                public void run() {
                                  while (this.val$c_source_f.getConfig("transfer_stats") != null) {
                                    this.this$0.printStats(false);
                                    try {
                                      Thread.sleep(1000L);
                                    } catch (InterruptedException e) {
                                      e.printStackTrace();
                                    } 
                                  } 
                                }
                              }); 
                        try {
                          MessageDigest m = MessageDigest.getInstance("MD5");
                          byte[] b = new byte[32768];
                          int bytes_read = 0;
                          long total_bytes = 0L;
                          boolean slow_speed = !this.prefs.getProperty("slow_transfer", "0").equals("0");
                          if (slow_speed)
                            b = new byte[1]; 
                          while (bytes_read >= 0) {
                            bytes_read = in_f.read(b);
                            if (bytes_read >= 0) {
                              out_f.write(b, 0, bytes_read);
                              m.update(b, 0, bytes_read);
                              if (slow_speed)
                                Thread.sleep(Integer.parseInt(this.prefs.getProperty("slow_transfer", "0"))); 
                              total_bytes += bytes_read;
                            } 
                            c_dest_f.setConfig("transfer_bytes", (new StringBuffer(String.valueOf(total_bytes))).toString());
                            c_source_f.setConfig("transfer_bytes", (new StringBuffer(String.valueOf(total_bytes))).toString());
                          } 
                          in_f.close();
                          out_f.close();
                          c_source_f.close();
                          c_dest_f.close();
                          c_source.setConfig("transfer_stats", null);
                          c_dest.setConfig("transfer_stats", null);
                          if (!multithreaded)
                            printStats(true); 
                          float speed = 10.0F * (float)total_bytes / 1024.0F / (float)(System.currentTimeMillis() - start) / 1000.0F;
                          String speed_str = (speed > 10240.0F) ? (String.valueOf((int)(speed / 1024.0F) / 10.0F) + "MB/sec") : (String.valueOf((float)((int)speed / 10.0D)) + "KB/sec");
                          String md5 = (new BigInteger(1, m.digest())).toString(16).toLowerCase();
                          if (md5.length() < 32)
                            md5 = "0" + md5; 
                          line("Download completed:" + the_dir_dest_f + " -> " + the_dir_source_f + " : " + (System.currentTimeMillis() - start) + "ms, " + speed_str + ", size=" + total_bytes + ", md5=" + md5);
                          if (this.prefs.getProperty("keep_date", "true").equals("true")) {
                            SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                            if (dest_stat_f != null)
                              process_command("LMDTM \"" + the_dir_source_f + "\" " + sdf_yyyyMMddHHmmss.format(new Date(Long.parseLong(dest_stat_f.getProperty("modified")))), true); 
                          } 
                          print_prompt();
                        } catch (Exception e) {
                          transfer_error = e;
                          e.printStackTrace();
                          line("Error:" + e);
                        } 
                        freeClient(c_source_f);
                        freeClient(c_dest_f);
                        this.recent_transfers_download.addElement(dest_stat_f);
                        while (this.recent_transfers_upload.size() > 1000)
                          this.recent_transfers_upload.remove(0); 
                        while (this.recent_transfers_download.size() > 1000)
                          this.recent_transfers_download.remove(0); 
                        int idle_time = 0;
                        while (this.source_used.size() == 0 && this.pending_transfer_queue.size() == 0 && !this.prefs.containsKey("aborting") && this.retry_active.size() == 0) {
                          idle_time++;
                          if (idle_time > 10)
                            break; 
                          Thread.sleep(100L);
                        } 
                        if (idle_time >= 10) {
                          line("Transfer complete.");
                          print_prompt();
                        } 
                      } 
                    } 
                  } catch (Exception e) {
                    transfer_error = e;
                    if (in != null)
                      in.close(); 
                    if (c_source != null)
                      c_source.close(); 
                    if (out != null)
                      out.close(); 
                    e.printStackTrace();
                    line("Error:" + e);
                  } 
                  if (transfer_error == null || this.prefs.containsKey("aborting") || transfer_error.indexOf("403") >= 0 || transfer_error.indexOf("404") >= 0 || transfer_error.indexOf("denied") >= 0 || transfer_error.indexOf("not allowed") >= 0) {
                    if (transfer_error != null)
                      line("Ended with error:" + transfer_error + ":" + command_str); 
                    this.prefs.put("auto_retry_delay", "1000");
                    break;
                  } 
                  if (this.prefs.getProperty("auto_retry", "true").equals("true")) {
                    long i = Integer.parseInt(this.prefs.getProperty("auto_retry_delay", "1000"));
                    long slept = 100L;
                    synchronized (this.retry_active) {
                      this.retry_active.addElement("active");
                    } 
                    while (slept < i && !this.prefs.containsKey("aborting")) {
                      Thread.sleep(100L);
                      slept += 100L;
                    } 
                    if (i > 15000L)
                      i = 15000L; 
                    this.prefs.put("auto_retry_delay", (new StringBuffer(String.valueOf(i * 2L))).toString());
                    resume = true;
                    synchronized (this.retry_active) {
                      this.retry_active.removeElementAt(0);
                    } 
                  } 
                } 
              } else if (command[0].toUpperCase().startsWith("PUT") || command[0].toUpperCase().startsWith("REPUT") || command[0].toUpperCase().startsWith("APPE")) {
                if (!this.destination_logged_in) {
                  line("Not connected to dest.");
                  return null;
                } 
                if (!this.source_logged_in) {
                  line("Not connected to source.");
                  return null;
                } 
                freeClient(c);
                Exception transfer_error = null;
                boolean resume = !command[0].toUpperCase().startsWith("PUT");
                while (!this.prefs.containsKey("aborting")) {
                  transfer_error = null;
                  c = null;
                  String the_dir = getArgs(command, 1, false, true);
                  String the_dir_dest = getArgs(command, 2, false, false);
                  if (the_dir.indexOf("*") >= 0) {
                    Vector list = new Vector();
                    try {
                      startupThreads();
                      String parent_dir = Common.all_but_last(the_dir);
                      GenericClient genericClient = getClient(true);
                      try {
                        genericClient.list(parent_dir, list);
                      } finally {
                        freeClient(genericClient);
                      } 
                      for (int x = 0; x < list.size() && !this.prefs.containsKey("aborting"); x++) {
                        Properties p = list.elementAt(x);
                        if (Common.do_search(Common.last(the_dir), p.getProperty("name"), false, 0))
                          if (p.getProperty("name").indexOf("*") >= 0) {
                            line("Skipping invalid filename for wildcard download:" + p.getProperty("name"));
                          } else {
                            this.pending_transfer_queue.addElement(String.valueOf(command[0]) + " \"" + parent_dir + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\" \"" + the_dir_dest + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\"");
                            if (this.prefs.containsKey("aborting")) {
                              list.removeAllElements();
                              this.pending_transfer_queue.removeAllElements();
                              break;
                            } 
                          }  
                      } 
                    } catch (Exception e) {
                      e.printStackTrace();
                      line("Error:" + e);
                    } 
                    print_prompt();
                    break;
                  } 
                  if (the_dir_dest.endsWith("/") && !the_dir.endsWith("/"))
                    the_dir_dest = String.valueOf(the_dir_dest) + Common.last(the_dir); 
                  InputStream in = null;
                  OutputStream out = null;
                  GenericClient c1 = null;
                  GenericClient c2 = null;
                  long source_size = 0L;
                  long dest_size = 0L;
                  try {
                    c1 = getClient(true);
                    c2 = getClient(false);
                    long start_pos = 0L;
                    Properties dest_stat = c2.stat(the_dir_dest);
                    if (dest_stat != null)
                      dest_size = Long.parseLong(dest_stat.getProperty("size", "0")); 
                    if (resume)
                      start_pos = dest_size; 
                    Properties source_stat = c1.stat(the_dir);
                    if (source_stat != null)
                      source_size = Long.parseLong(source_stat.getProperty("size", "0")); 
                    boolean folder = false;
                    if (source_stat == null) {
                      line(String.valueOf(the_dir) + " not found.");
                      freeClient(c1);
                      freeClient(c2);
                      break;
                    } 
                    if (source_stat.getProperty("type").equalsIgnoreCase("DIR"))
                      folder = true; 
                    if (folder) {
                      startupThreads();
                      if (!the_dir_dest.endsWith("/"))
                        the_dir_dest = String.valueOf(the_dir_dest) + "/"; 
                      if (!the_dir.endsWith("/"))
                        the_dir = String.valueOf(the_dir) + "/"; 
                      process_command("MKD \"" + the_dir_dest + "\"", true);
                      Vector list = new Vector();
                      c1.list(the_dir, list);
                      freeClient(c1);
                      freeClient(c2);
                      while (list.size() > 0) {
                        Properties p = list.remove(0);
                        if (this.prefs.containsKey("aborting")) {
                          list.removeAllElements();
                          this.pending_transfer_queue.removeAllElements();
                          break;
                        } 
                        try {
                          if (p.getProperty("type").equalsIgnoreCase("DIR"))
                            process_command("MKD \"" + the_dir_dest + p.getProperty("name") + "\"", true); 
                          this.pending_transfer_queue.addElement(String.valueOf(command[0]) + " \"" + the_dir + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\" \"" + the_dir_dest + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\"");
                          while (this.pending_transfer_queue.size() > 100000)
                            Thread.sleep(100L); 
                        } catch (Exception e) {
                          e.printStackTrace();
                          line("Error:" + e);
                        } 
                      } 
                    } else {
                      long start_pos1 = start_pos;
                      long start_pos2 = start_pos;
                      the_dir = this.vars.replace_vars_line_url(the_dir, source_stat, "{", "}");
                      the_dir_dest = this.vars.replace_vars_line_url(the_dir_dest, dest_stat, "{", "}");
                      the_dir = this.vars.replace_vars_line_date(the_dir, source_stat, "{", "}");
                      the_dir_dest = this.vars.replace_vars_line_date(the_dir_dest, dest_stat, "{", "}");
                      boolean skip = false;
                      if (this.prefs.getProperty("skip_modified_and_size", "true").equals("true") && dest_stat != null) {
                        if (dest_stat != null && Math.abs(Long.parseLong(source_stat.getProperty("modified", "0")) - Long.parseLong(dest_stat.getProperty("modified", "50000"))) < 1000L && source_stat.getProperty("size").equals(dest_stat.getProperty("size"))) {
                          skip = true;
                          line(String.valueOf(the_dir) + ":Skipping item because of matching modified date and size (skip_modified_and_size)");
                          print_prompt();
                        } 
                      } else if (this.prefs.getProperty("skip_modified", "false").equals("true") && dest_stat != null) {
                        if (dest_stat != null && Math.abs(Long.parseLong(source_stat.getProperty("modified", "0")) - Long.parseLong(dest_stat.getProperty("modified", "50000"))) < 1000L) {
                          skip = true;
                          line(String.valueOf(the_dir) + ":Skipping item because of matching modifed date (skip_modified)");
                          print_prompt();
                        } 
                      } else if (this.prefs.getProperty("skip_size", "false").equals("true") && dest_stat != null) {
                        if (source_stat.getProperty("size").equals(dest_stat.getProperty("size"))) {
                          skip = true;
                          line(String.valueOf(the_dir) + ":Skipping item because of matching size (skip_size)");
                          print_prompt();
                        } 
                      } 
                      if (skip) {
                        freeClient(c1);
                        freeClient(c2);
                        print_prompt();
                      } else {
                        if (command[0].toUpperCase().startsWith("APPE")) {
                          start_pos1 = 0L;
                          resume = false;
                        } 
                        InputStream in_f = c1.download(the_dir, start_pos1, -1L, true);
                        in = in_f;
                        c1.setConfig("transfer_direction", "PUT");
                        c2.setConfig("transfer_direction", "PUT");
                        c1.setConfig("transfer_path_src", the_dir);
                        c2.setConfig("transfer_path_dst", the_dir_dest);
                        c2.setConfig("transfer_path_src", the_dir);
                        c1.setConfig("transfer_path_dst", the_dir_dest);
                        c1.setConfig("transfer_stats", "true");
                        c2.setConfig("transfer_stats", "true");
                        OutputStream out_f = c2.upload(String.valueOf(the_dir_dest) + this.prefs.getProperty("upload_temp_ext", ""), start_pos2, true, true);
                        out = out_f;
                        line("Upload started:" + the_dir + " -> " + the_dir_dest + (resume ? (" : Resuming from position:" + start_pos) : ""));
                        if (multithreaded)
                          print_prompt(); 
                        String the_dir_dest_f = the_dir_dest;
                        String the_dir_source_f = the_dir;
                        Properties source_stat_f = source_stat;
                        long start = System.currentTimeMillis();
                        c2.setConfig("transfer_start", (new StringBuffer(String.valueOf(start))).toString());
                        c2.setConfig("transfer_bytes_total", (new StringBuffer(String.valueOf(source_size))).toString());
                        c1.setConfig("transfer_start", (new StringBuffer(String.valueOf(start))).toString());
                        c1.setConfig("transfer_bytes_total", (new StringBuffer(String.valueOf(source_size))).toString());
                        c2.setConfig("transfer_bytes", "0");
                        c1.setConfig("transfer_bytes", "0");
                        if (!multithreaded) {
                          GenericClient c2_f = c2;
                          Worker.startWorker(new Runnable(this, c2_f) {
                                final Client this$0;
                                
                                private final GenericClient val$c2_f;
                                
                                public void run() {
                                  while (this.val$c2_f.getConfig("transfer_stats") != null) {
                                    this.this$0.printStats(false);
                                    try {
                                      Thread.sleep(1000L);
                                    } catch (InterruptedException e) {
                                      e.printStackTrace();
                                    } 
                                  } 
                                }
                              });
                        } 
                        try {
                          MessageDigest m = MessageDigest.getInstance("MD5");
                          byte[] b = new byte[32768];
                          int bytes_read = 0;
                          long total_bytes = 0L;
                          boolean slow_speed = !this.prefs.getProperty("slow_transfer", "0").equals("0");
                          if (slow_speed)
                            b = new byte[1]; 
                          while (bytes_read >= 0) {
                            bytes_read = in_f.read(b);
                            if (bytes_read >= 0) {
                              out_f.write(b, 0, bytes_read);
                              m.update(b, 0, bytes_read);
                              if (slow_speed)
                                Thread.sleep(Integer.parseInt(this.prefs.getProperty("slow_transfer", "0"))); 
                              total_bytes += bytes_read;
                            } 
                            c2.setConfig("transfer_bytes", (new StringBuffer(String.valueOf(total_bytes))).toString());
                            c1.setConfig("transfer_bytes", (new StringBuffer(String.valueOf(total_bytes))).toString());
                          } 
                          in_f.close();
                          out_f.close();
                          c1.close();
                          c2.close();
                          if (!this.prefs.getProperty("upload_temp_ext", "").equals(""))
                            c2.rename(String.valueOf(the_dir_dest_f) + this.prefs.getProperty("upload_temp_ext", ""), the_dir_dest_f); 
                          c1.setConfig("transfer_stats", null);
                          c2.setConfig("transfer_stats", null);
                          if (!multithreaded)
                            printStats(true); 
                          float speed = 10.0F * (float)total_bytes / 1024.0F / (float)(System.currentTimeMillis() - start) / 1000.0F;
                          String speed_str = (speed > 10240.0F) ? (String.valueOf((int)(speed / 1024.0F) / 10.0F) + "MB/sec") : (String.valueOf((float)((int)speed / 10.0D)) + "KB/sec");
                          String md5 = (new BigInteger(1, m.digest())).toString(16).toLowerCase();
                          if (md5.length() < 32)
                            md5 = "0" + md5; 
                          line("Upload completed:" + the_dir_source_f + " -> " + the_dir_dest_f + " : " + (System.currentTimeMillis() - start) + "ms, " + speed_str + ", size=" + total_bytes + ", md5=" + md5);
                          if (c1 != null)
                            c1 = freeClient(c1); 
                          if (c2 != null)
                            c2 = freeClient(c2); 
                          if (this.prefs.getProperty("keep_date", "true").equals("true")) {
                            SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                            process_command("MDTM \"" + the_dir_dest_f + "\" " + sdf_yyyyMMddHHmmss.format(new Date(Long.parseLong(source_stat_f.getProperty("modified")))), true);
                          } 
                          print_prompt();
                        } catch (Exception e) {
                          transfer_error = e;
                          line("Error:" + e);
                        } 
                        if (c1 != null)
                          c1 = freeClient(c1); 
                        if (c2 != null)
                          c2 = freeClient(c2); 
                        this.recent_transfers_upload.addElement(source_stat_f);
                        while (this.recent_transfers_upload.size() > 1000)
                          this.recent_transfers_upload.remove(0); 
                        while (this.recent_transfers_download.size() > 1000)
                          this.recent_transfers_download.remove(0); 
                        int idle_time = 0;
                        while (this.source_used.size() == 0 && this.pending_transfer_queue.size() == 0 && !this.prefs.containsKey("aborting") && this.retry_active.size() == 0) {
                          idle_time++;
                          if (idle_time > 10)
                            break; 
                          Thread.sleep(100L);
                        } 
                        if (idle_time >= 10) {
                          line("Transfer complete.");
                          print_prompt();
                        } 
                      } 
                    } 
                  } catch (Exception e) {
                    transfer_error = e;
                    if (in != null)
                      in.close(); 
                    if (c1 != null)
                      c1.close(); 
                    if (out != null)
                      out.close(); 
                    e.printStackTrace();
                    line("Error:" + e);
                  } 
                  if (transfer_error == null || this.prefs.containsKey("aborting") || transfer_error.indexOf("403") >= 0 || transfer_error.indexOf("404") >= 0 || transfer_error.indexOf("denied") >= 0 || transfer_error.indexOf("not allowed") >= 0) {
                    if (transfer_error != null)
                      line("Ended with error:" + transfer_error + ":" + command_str); 
                    this.prefs.put("auto_retry_delay", "1000");
                    break;
                  } 
                  if (this.prefs.getProperty("auto_retry", "true").equals("true")) {
                    long i = Integer.parseInt(this.prefs.getProperty("auto_retry_delay", "1000"));
                    long slept = 100L;
                    synchronized (this.retry_active) {
                      this.retry_active.addElement("active");
                    } 
                    while (slept < i && !this.prefs.containsKey("aborting")) {
                      Thread.sleep(100L);
                      slept += 100L;
                    } 
                    if (i > 15000L)
                      i = 15000L; 
                    this.prefs.put("auto_retry_delay", (new StringBuffer(String.valueOf(i * 2L))).toString());
                    resume = true;
                    synchronized (this.retry_active) {
                      this.retry_active.removeElementAt(0);
                    } 
                  } 
                } 
              } else if (command[0].toUpperCase().startsWith("DIFFPUT") || command[0].toUpperCase().startsWith("DIFFGET")) {
                if (!this.destination_logged_in) {
                  line("Not connected to dest.");
                  return null;
                } 
                if (!this.source_logged_in) {
                  line("Not connected to source.");
                  return null;
                } 
                freeClient(c);
                c = null;
                boolean upload = command[0].toUpperCase().startsWith("DIFFPUT");
                GenericClient c_source = getClient(!upload);
                GenericClient c_dest = getClient(upload);
                String the_dir = getArgs(command, 1, false, upload);
                String the_dir_opposite = getArgs(command, 2, false, !upload);
                if (the_dir_opposite.endsWith("/") && !the_dir.endsWith("/"))
                  the_dir_opposite = String.valueOf(the_dir_opposite) + Common.last(the_dir); 
                InputStream in = null;
                OutputStream out = null;
                Properties source_stat = c_source.stat(the_dir_opposite);
                Properties dest_stat = c_dest.stat(the_dir);
                boolean folder = false;
                if (dest_stat != null && dest_stat.getProperty("type").equalsIgnoreCase("DIR"))
                  folder = true; 
                if (folder) {
                  startupThreads();
                  if (!the_dir_opposite.endsWith("/"))
                    the_dir_opposite = String.valueOf(the_dir_opposite) + "/"; 
                  if (!the_dir.endsWith("/"))
                    the_dir = String.valueOf(the_dir) + "/"; 
                  process_command(String.valueOf(upload ? "" : "L") + "MKD \"" + the_dir_opposite + "\"", true);
                  Vector list = new Vector();
                  c_dest.list(the_dir, list);
                  freeClient(c_source);
                  freeClient(c_dest);
                  while (list.size() > 0) {
                    Properties p = list.remove(0);
                    if (this.prefs.containsKey("aborting")) {
                      list.removeAllElements();
                      this.pending_transfer_queue.removeAllElements();
                      break;
                    } 
                    try {
                      if (p.getProperty("type").equalsIgnoreCase("DIR"))
                        process_command(String.valueOf(upload ? "" : "L") + "MKD \"" + the_dir_opposite + p.getProperty("name") + "\"", true); 
                      this.pending_transfer_queue.addElement(String.valueOf(command[0]) + " \"" + the_dir + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\" \"" + the_dir_opposite + p.getProperty("name") + (p.getProperty("type").equalsIgnoreCase("DIR") ? "/" : "") + "\"");
                      while (this.pending_transfer_queue.size() > 10000)
                        Thread.sleep(100L); 
                    } catch (Exception e) {
                      e.printStackTrace();
                      line("Error:" + e);
                    } 
                  } 
                } else {
                  boolean skip = false;
                  if (this.prefs.getProperty("skip_modified_and_size", "true").equals("true") && source_stat != null && dest_stat != null) {
                    if (dest_stat != null && Math.abs(Long.parseLong(source_stat.getProperty("modified", "0")) - Long.parseLong(dest_stat.getProperty("modified", "50000"))) < 1000L && source_stat.getProperty("size").equals(dest_stat.getProperty("size"))) {
                      skip = true;
                      line(String.valueOf(the_dir_opposite) + ":Skipping item because of matching modified date and size (skip_modified_and_size)");
                      print_prompt();
                    } 
                  } else if (this.prefs.getProperty("skip_modified", "false").equals("true") && source_stat != null && dest_stat != null) {
                    if (dest_stat != null && Math.abs(Long.parseLong(source_stat.getProperty("modified", "0")) - Long.parseLong(dest_stat.getProperty("modified", "50000"))) < 1000L) {
                      skip = true;
                      line(String.valueOf(the_dir_opposite) + ":Skipping item because of matching modifed date (skip_modified)");
                      print_prompt();
                    } 
                  } else if (this.prefs.getProperty("skip_size", "false").equals("true") && source_stat != null && dest_stat != null) {
                    if (source_stat.getProperty("size").equals(dest_stat.getProperty("size"))) {
                      skip = true;
                      line(String.valueOf(the_dir_opposite) + ":Skipping item because of matching size: (skip_size)");
                      print_prompt();
                    } 
                  } 
                  if (skip) {
                    freeClient(c_source);
                    freeClient(c_dest);
                  } else {
                    if (source_stat == null) {
                      source_stat = new Properties();
                      source_stat.put("size", "0");
                    } 
                    if (dest_stat == null) {
                      dest_stat = new Properties();
                      dest_stat.put("size", "0");
                    } 
                    line(String.valueOf(the_dir_opposite) + ":Diff transfer starting...modified=" + source_stat.getProperty("modified") + " vs. " + dest_stat.getProperty("modified") + "  size=" + source_stat.getProperty("size") + " vs. " + dest_stat.getProperty("size"));
                    Vector byteRanges = diff(the_dir_opposite, the_dir, source_stat, dest_stat, c_source, c_dest);
                    for (int x = 0; x < byteRanges.size(); x++) {
                      try {
                        c_source.setConfig("transfer_direction", upload ? "PUT" : "GET");
                        c_dest.setConfig("transfer_direction", upload ? "PUT" : "GET");
                        c_source.setConfig("transfer_path_src", the_dir);
                        c_dest.setConfig("transfer_path_dst", the_dir_opposite);
                        c_dest.setConfig("transfer_path_src", the_dir);
                        c_source.setConfig("transfer_path_dst", the_dir_opposite);
                        c_source.setConfig("transfer_stats", "true");
                        c_dest.setConfig("transfer_stats", "true");
                        String range = byteRanges.elementAt(x).toString();
                        long start_pos = Long.parseLong(range.split("-")[0]);
                        long end_pos = -1L;
                        if ((range.split("-")).length > 1)
                          end_pos = Long.parseLong(range.substring(range.indexOf("-") + 1)); 
                        long start = System.currentTimeMillis();
                        c_dest.setConfig("transfer_start", (new StringBuffer(String.valueOf(start))).toString());
                        long item_size = end_pos - start_pos;
                        if (item_size < 0L && upload)
                          item_size = Long.parseLong(dest_stat.getProperty("size")); 
                        if (item_size < 0L && !upload)
                          item_size = Long.parseLong(dest_stat.getProperty("size")); 
                        c_dest.setConfig("transfer_bytes_total", (new StringBuffer(String.valueOf(item_size))).toString());
                        c_source.setConfig("transfer_start", (new StringBuffer(String.valueOf(start))).toString());
                        c_source.setConfig("transfer_bytes_total", (new StringBuffer(String.valueOf(item_size))).toString());
                        c_dest.setConfig("transfer_bytes", "0");
                        c_source.setConfig("transfer_bytes", "0");
                        GenericClient c_f = c_dest;
                        if (!multithreaded)
                          Worker.startWorker(new Runnable(this, c_f) {
                                final Client this$0;
                                
                                private final GenericClient val$c_f;
                                
                                public void run() {
                                  while (this.val$c_f.getConfig("transfer_stats") != null) {
                                    this.this$0.printStats(false);
                                    try {
                                      Thread.sleep(1000L);
                                    } catch (InterruptedException e) {
                                      e.printStackTrace();
                                    } 
                                  } 
                                }
                              }); 
                        InputStream in_f = c_dest.download(the_dir, start_pos, end_pos, true);
                        in = in_f;
                        boolean truncate = false;
                        if (Long.parseLong(dest_stat.getProperty("size")) < Long.parseLong(source_stat.getProperty("size")) && x == byteRanges.size() - 1)
                          truncate = true; 
                        OutputStream out_f = c_source.upload(the_dir_opposite, start_pos, truncate, true);
                        out = out_f;
                        line("\r\n" + the_dir_opposite + ":Started transferring part " + (x + 1) + " of " + byteRanges.size() + " at position " + start_pos + " to " + ((end_pos == -1L) ? "end" : (new StringBuffer(String.valueOf(end_pos))).toString()) + ".");
                        if (multithreaded)
                          print_prompt(); 
                        byte[] b = new byte[32768];
                        int bytes_read = 0;
                        long total_bytes = start_pos;
                        try {
                          boolean slow_speed = !this.prefs.getProperty("slow_transfer", "0").equals("0");
                          if (slow_speed)
                            b = new byte[1]; 
                          while (bytes_read >= 0) {
                            bytes_read = in_f.read(b);
                            if (bytes_read >= 0) {
                              out_f.write(b, 0, bytes_read);
                              if (slow_speed)
                                Thread.sleep(Integer.parseInt(this.prefs.getProperty("slow_transfer", "0"))); 
                              total_bytes += bytes_read;
                            } 
                            c_source.setConfig("transfer_bytes", (new StringBuffer(String.valueOf(total_bytes))).toString());
                            c_dest.setConfig("transfer_bytes", (new StringBuffer(String.valueOf(total_bytes))).toString());
                          } 
                        } finally {
                          in_f.close();
                          out_f.close();
                        } 
                        c_source.setConfig("transfer_stats", null);
                        c_dest.setConfig("transfer_stats", null);
                        printStats(false);
                        line("\r\n" + the_dir_opposite + ":Finished transferring part " + (x + 1) + " of " + byteRanges.size() + " at position " + start_pos + " to " + ((end_pos == -1L) ? "end" : (new StringBuffer(String.valueOf(end_pos))).toString()) + ": " + (System.currentTimeMillis() - start) + "ms");
                        if (multithreaded)
                          print_prompt(); 
                      } catch (Exception e) {
                        if (in != null)
                          in.close(); 
                        if (c_dest != null)
                          c_dest.close(); 
                        if (out != null)
                          out.close(); 
                        e.printStackTrace();
                        line("Error:" + e);
                      } 
                    } 
                    if (byteRanges.size() > 0) {
                      if (upload) {
                        this.recent_transfers_upload.addElement(dest_stat);
                      } else {
                        this.recent_transfers_download.addElement(source_stat);
                      } 
                      if (this.prefs.getProperty("keep_date", "true").equals("true")) {
                        SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                        process_command(String.valueOf(upload ? "" : "L") + "MDTM \"" + the_dir_opposite + "\" " + sdf_yyyyMMddHHmmss.format(new Date(Long.parseLong(dest_stat.getProperty("modified")))), true);
                      } 
                    } 
                    while (this.recent_transfers_upload.size() > 1000)
                      this.recent_transfers_upload.remove(0); 
                    while (this.recent_transfers_download.size() > 1000)
                      this.recent_transfers_download.remove(0); 
                    c = null;
                    freeClient(c_source);
                    freeClient(c_dest);
                    if (byteRanges.size() > 0) {
                      line(String.valueOf(the_dir_opposite) + ":Diff transfer completed, " + byteRanges.size() + " parts transferred.");
                    } else {
                      line(String.valueOf(the_dir_opposite) + ":Diff transfer completed, no changes.");
                    } 
                    if (multithreaded)
                      print_prompt(); 
                  } 
                } 
              } else {
                if (command[0].toUpperCase().startsWith("DIFF")) {
                  if (!this.destination_logged_in) {
                    line("Not connected to dest.");
                    return null;
                  } 
                  if (!this.source_logged_in) {
                    line("Not connected to source.");
                    return null;
                  } 
                  freeClient(c);
                  c = null;
                  String the_dir = getArgs(command, 1, false, true);
                  String the_dir_dest = getArgs(command, 2, false, false);
                  if (the_dir_dest.endsWith("/"))
                    the_dir_dest = String.valueOf(the_dir_dest) + Common.last(the_dir); 
                  line(String.valueOf(the_dir_dest) + ":Diff starting...");
                  GenericClient c_source = getClient(true);
                  GenericClient c_dest = getClient(false);
                  Properties stat1 = c_dest.stat(the_dir_dest);
                  Properties stat2 = c_source.stat(the_dir);
                  if (stat1 == null) {
                    stat1 = new Properties();
                    stat1.put("size", "0");
                  } 
                  if (stat2 == null) {
                    stat2 = new Properties();
                    stat2.put("size", "0");
                  } 
                  Vector byteRanges = diff(the_dir_dest, the_dir, stat1, stat2, c_dest, c_source);
                  for (int x = 0; x < byteRanges.size(); x++) {
                    try {
                      String range = byteRanges.elementAt(x).toString();
                      long start_pos = Long.parseLong(range.split("-")[0]);
                      long end_pos = -1L;
                      if ((range.split("-")).length > 1)
                        end_pos = Long.parseLong(range.substring(range.indexOf("-") + 1)); 
                      boolean truncate = false;
                      if (Long.parseLong(stat2.getProperty("size")) < Long.parseLong(stat1.getProperty("size")) && x == byteRanges.size() - 1)
                        truncate = true; 
                      line(String.valueOf(the_dir_dest) + ":Part " + (x + 1) + " of " + byteRanges.size() + ": " + the_dir + " -> " + the_dir_dest + " (" + start_pos + "-" + end_pos + ") truncate=" + truncate);
                    } catch (Exception e) {
                      e.printStackTrace();
                      line("Error:" + e);
                    } 
                  } 
                  c = null;
                  freeClient(c_source);
                  freeClient(c_dest);
                  return line(String.valueOf(the_dir_dest) + ":Diff completed, " + byteRanges.size() + " parts are different.");
                } 
                if (command[0].toUpperCase().startsWith("WAIT") || command[0].toUpperCase().startsWith("LWAIT")) {
                  this.abort_wait = false;
                  freeClient(c);
                  c = null;
                  long secs = 0L;
                  long loops = 0L;
                  boolean found = false;
                  if (command.length > 1)
                    secs = Long.parseLong(command[1]); 
                  long start_wait = System.currentTimeMillis();
                  long last_activity = System.currentTimeMillis();
                  while ((this.destination_used.size() > 0 || this.source_used.size() > 0 || this.pending_transfer_queue.size() > 0 || this.retry_active.size() > 0 || System.currentTimeMillis() - last_activity < 2000L) && !this.abort_wait) {
                    if (secs > 0L && System.currentTimeMillis() - start_wait > secs * 1000L)
                      throw new Exception("Timeout while waiting for transfers."); 
                    Thread.sleep(100L);
                    if (loops++ > 10L) {
                      found = true;
                      loops = 0L;
                      printStats(true);
                    } 
                    if (this.destination_used.size() > 0 || this.source_used.size() > 0 || this.pending_transfer_queue.size() > 0 || this.retry_active.size() > 0)
                      last_activity = System.currentTimeMillis(); 
                  } 
                  if (!found)
                    line("No transfers in progress."); 
                  print_prompt();
                  this.abort_wait = false;
                } else if (command[0].toUpperCase().startsWith("INFO") || command[0].toUpperCase().startsWith("LINFO")) {
                  if (command.length > 1 && command[1].equalsIgnoreCase("CLEAR")) {
                    this.recent_transfers_download.removeAllElements();
                    this.recent_transfers_upload.removeAllElements();
                  } 
                  printStats(true);
                  if (command.length > 1 && command[1].equalsIgnoreCase("ALL"))
                    printDownloadsUploads(); 
                  last_line_prompt = "message";
                  print_prompt();
                } else if (!command[0].trim().equals("")) {
                  return line("Command not recognized or allowed.");
                } 
              } 
            } 
          } finally {
            freeClient(c);
          } 
        } 
      } 
    } 
    return null;
  }
  
  public Vector diff(String path1, String path2, Properties stat1, Properties stat2, GenericClient c1, GenericClient c2) throws Exception {
    StringBuffer status1 = new StringBuffer();
    StringBuffer status2 = new StringBuffer();
    Vector chunksF1 = new Vector();
    Vector chunksF2 = new Vector();
    if (stat1.getProperty("size", "0").equals("0") || stat2.getProperty("size", "0").equals("0")) {
      Vector byteRanges = new Vector();
      byteRanges.addElement("0--1");
      return byteRanges;
    } 
    (new Thread(new Runnable(this, c1, path1, stat2, chunksF1, status1) {
          final Client this$0;
          
          private final GenericClient val$c1;
          
          private final String val$path1;
          
          private final Properties val$stat2;
          
          private final Vector val$chunksF1;
          
          private final StringBuffer val$status1;
          
          public void run() {
            try {
              if (this.val$c1 instanceof HTTPClient) {
                if (this.val$c1.stat(this.val$path1) != null && this.val$stat2 != null)
                  Tunnel2.getRemoteMd5s(this.val$c1.url, this.val$path1, this.val$chunksF1, true, (new StringBuffer()).append(this.val$c1.getConfig("crushAuth")), this.val$status1, Long.parseLong(this.val$stat2.getProperty("size"))); 
              } else if (this.val$c1.stat(this.val$path1) != null && this.val$stat2 != null) {
                Tunnel2.getInputStreamMd5s(this.val$c1.download(this.val$path1, 0L, -1L, true), Long.parseLong(this.val$stat2.getProperty("size")), true, this.val$status1, this.val$chunksF1);
              } 
            } catch (Exception e) {
              e.printStackTrace();
            } 
            this.val$status1.append("done");
          }
        })).start();
    (new Thread(new Runnable(this, c2, path2, stat1, chunksF2, status2) {
          final Client this$0;
          
          private final GenericClient val$c2;
          
          private final String val$path2;
          
          private final Properties val$stat1;
          
          private final Vector val$chunksF2;
          
          private final StringBuffer val$status2;
          
          public void run() {
            try {
              if (this.val$c2 instanceof HTTPClient) {
                if (this.val$c2.stat(this.val$path2) != null && this.val$stat1 != null)
                  Tunnel2.getRemoteMd5s(this.val$c2.url, this.val$path2, this.val$chunksF2, true, (new StringBuffer()).append(this.val$c2.getConfig("crushAuth")), this.val$status2, Long.parseLong(this.val$stat1.getProperty("size"))); 
              } else if (this.val$c2.stat(this.val$path2) != null && this.val$stat1 != null) {
                Tunnel2.getInputStreamMd5s(this.val$c2.download(this.val$path2, 0L, -1L, true), Long.parseLong(this.val$stat1.getProperty("size")), true, this.val$status2, this.val$chunksF2);
              } 
            } catch (Exception e) {
              e.printStackTrace();
            } 
            this.val$status2.append("done");
          }
        })).start();
    int pos = 0;
    while (true) {
      if (chunksF1.size() > pos && chunksF2.size() > pos) {
        Properties chunk1 = chunksF1.elementAt(pos);
        Properties chunk2 = chunksF2.elementAt(pos);
        if (chunk1.getProperty("md5").equals(chunk2.getProperty("md5")) && chunk1.getProperty("start").equals(chunk2.getProperty("start")) && chunk1.getProperty("size").equals(chunk2.getProperty("size"))) {
          chunksF1.remove(pos);
          chunksF2.remove(pos);
          continue;
        } 
        pos++;
        continue;
      } 
      if (status1.length() > 0 && status2.length() > 0)
        break; 
      Thread.sleep(100L);
    } 
    return Tunnel2.compareMd5s(chunksF1, chunksF2, false);
  }
  
  public void startupThreads() throws Exception {
    if (this.prefs.getProperty("transfer_threads_started", "false").equals("false")) {
      this.prefs.put("transfer_threads_started", "true");
      for (int x = 0; x < Integer.parseInt(this.prefs.getProperty("max_threads", "5")); x++) {
        Worker.startWorker(new Runnable(this) {
              final Client this$0;
              
              public void run() {
                this.this$0.run_transfers();
              }
            });
      } 
    } 
  }
  
  public static String format_ls_la(Properties item) {
    StringBuffer item_str = new StringBuffer();
    item_str.append(item.getProperty("permissions"));
    item_str.append(String.valueOf(Common.lpad(item.getProperty("num_items", ""), 4)) + " ");
    item_str.append(String.valueOf(Common.rpad(item.getProperty("owner", ""), 8)) + " ");
    item_str.append(String.valueOf(Common.rpad(item.getProperty("group", ""), 8)) + " ");
    item_str.append(String.valueOf(Common.lpad(item.getProperty("size", ""), 13)) + " ");
    item_str.append(String.valueOf(Common.lpad(item.getProperty("month", "").trim(), 3)) + " ");
    item_str.append(String.valueOf(Common.lpad(item.getProperty("day", "").trim(), 2)) + " ");
    item_str.append(String.valueOf(Common.lpad(item.getProperty("time_or_year", ""), 5)) + " ");
    item_str.append(item.getProperty("name", ""));
    return item_str.toString();
  }
  
  public void setupSignalHandler() {
    SignalHandler sh = new SignalHandler(this) {
        final Client this$0;
        
        public void handle(Signal sig) {
          try {
            this.this$0.abort_wait = true;
            this.this$0.process_command("ABOR", true);
          } catch (Exception e) {
            e.printStackTrace();
          } 
        }
      };
    try {
      Signal.handle(new Signal("INT"), sh);
    } catch (IllegalArgumentException illegalArgumentException) {}
  }
  
  static String last_line_prompt = "";
  
  static Object last_line_lock = new Object();
  
  public String line(String s) {
    synchronized (last_line_lock) {
      last_line_prompt = "message";
      if (this.messages != null)
        this.messages.addElement(s); 
      if (this.local_echo && (this.messages == null || this.dual_log)) {
        System.out.println(String.valueOf(this.log_sdf.format(new Date())) + "|" + s);
      } else if (this.messages == null || this.dual_log) {
        System.out.println(s);
      } 
    } 
    return s;
  }
  
  public void print_prompt() {
    synchronized (last_line_lock) {
      if (last_line_prompt.equals("stat"))
        System.out.println("\r\n"); 
      if (last_line_prompt.equals("message") || last_line_prompt.equals("stat")) {
        last_line_prompt = "prompt";
        System.out.print("CrushClient> ");
      } 
    } 
  }
  
  public Vector getStats() {
    Vector v = new Vector();
    try {
      for (int x = 0; x < this.source_used.size(); x++) {
        GenericClient c_tmp = this.source_used.elementAt(x);
        String transfer_path_src = c_tmp.getConfig("transfer_path_src", "");
        String transfer_path_dst = c_tmp.getConfig("transfer_path_dst", "");
        String direction = c_tmp.getConfig("transfer_direction", "");
        if (!transfer_path_src.equals("")) {
          long start = Long.parseLong(c_tmp.getConfig("transfer_start", "0").toString());
          long transfer_bytes_total = Long.parseLong(c_tmp.getConfig("transfer_bytes_total", "0").toString());
          long transfer_bytes = Long.parseLong(c_tmp.getConfig("transfer_bytes", "0").toString());
          v.addElement(c_tmp + ";" + transfer_path_src + ";" + transfer_path_dst + ";" + ((System.currentTimeMillis() - start) / 1000L) + ";" + transfer_bytes + ";" + transfer_bytes_total + ";" + direction);
        } 
      } 
    } catch (Exception exception) {}
    return v;
  }
  
  public void printStats(boolean new_line) {
    try {
      for (int x = 0; x < this.source_used.size(); x++) {
        GenericClient c_tmp = this.source_used.elementAt(x);
        String transfer_path_src = c_tmp.getConfig("transfer_path_src", "");
        String transfer_path_dst = c_tmp.getConfig("transfer_path_dst", "");
        if (!transfer_path_src.equals("")) {
          long start = Long.parseLong(c_tmp.getConfig("transfer_start").toString());
          long transfer_bytes_total = Long.parseLong(c_tmp.getConfig("transfer_bytes_total").toString());
          long transfer_bytes = Long.parseLong(c_tmp.getConfig("transfer_bytes").toString());
          float speed = 10.0F * (float)transfer_bytes / 1024.0F / (float)(System.currentTimeMillis() - start) / 1000.0F;
          String speed_str = (speed > 10240.0F) ? (String.valueOf((int)(speed / 1024.0F) / 10.0F) + "MB/sec") : (String.valueOf((float)((int)speed / 10.0D)) + "KB/sec");
          synchronized (last_line_lock) {
            System.out.print("\r" + transfer_path_src + "->" + transfer_path_dst + ":" + ((System.currentTimeMillis() - start) / 1000L) + " sec elapsed, " + Common.format_bytes_short(transfer_bytes) + " of " + Common.format_bytes_short(transfer_bytes_total) + " (" + (int)((float)transfer_bytes / (float)transfer_bytes_total * 100.0F) + "%) " + speed_str + "            " + (new_line ? "\n" : ""));
            if (new_line) {
              last_line_prompt = "message";
            } else {
              last_line_prompt = "stats";
            } 
          } 
        } 
      } 
    } catch (Exception exception) {}
  }
  
  public void printDownloadsUploads() {
    try {
      int x;
      for (x = 0; x < this.recent_transfers_download.size(); x++) {
        Properties p = this.recent_transfers_download.elementAt(x);
        line("Download:" + (new VRL(p.getProperty("url"))).getPath());
      } 
      for (x = 0; x < this.recent_transfers_upload.size(); x++) {
        Properties p = this.recent_transfers_upload.elementAt(x);
        line("Upload:" + (new VRL(p.getProperty("url"))).getPath());
      } 
    } catch (NullPointerException nullPointerException) {}
  }
}
