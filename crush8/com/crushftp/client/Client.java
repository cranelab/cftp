package com.crushftp.client;

import CrushTask.Start;
import com.crushftp.tunnel2.Tunnel2;
import com.crushftp.tunnel3.StreamController;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
import jline.console.ConsoleReader;
import org.boris.winrun4j.AbstractService;
import org.boris.winrun4j.ServiceException;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class Client extends AbstractService implements Runnable {
  boolean console_mode = true;
  
  public static final String version = "1.5.1";
  
  String current_dest_dir = "/";
  
  String current_source_dir = null;
  
  Vector logQueue = new Vector();
  
  Vector agent_log = new Vector();
  
  public DualReader br = new DualReader(this, null);
  
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
  
  Vector messages2 = null;
  
  boolean dual_log = true;
  
  boolean interactive = true;
  
  public Vector pending_transfer_queue = new Vector();
  
  public Vector failed_transfer_queue = new Vector();
  
  public Vector success_transfer_queue = new Vector();
  
  Vector tunnel_log = new Vector();
  
  SimpleDateFormat log_sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");
  
  Properties source_config = new Properties();
  
  Properties dest_config = new Properties();
  
  static final String unique_client_id = Common.makeBoundary(10);
  
  public Properties stats = new Properties();
  
  public static SimpleDateFormat log_format = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss:S");
  
  long client_start_time = 0L;
  
  String transfer_log = null;
  
  int additional_errors = 0;
  
  String uid = Common.makeBoundary(4);
  
  boolean transfer_threads_started = false;
  
  boolean validate_mode = false;
  
  public Client(Vector messages, Vector messages2) {
    this.messages = messages;
    this.messages2 = messages2;
    System.getProperties().put("crushftp.worker.v9", "true");
  }
  
  public Client() {
    System.getProperties().put("crushftp.worker.v9", "true");
  }
  
  public static void main(String[] args) {
    System.out.println("CrushClient:1.5.1:http://www.crushftp.com/crush7wiki/Wiki.jsp?page=CrushClient");
    System.setProperty("crushftp.worker.v9", "true");
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
        client.console_mode = false;
        client.getClass();
        client.br = new DualReader(client, new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()))));
        client.local_echo = true;
      } catch (Exception e) {
        client.printStackTrace(e, 1);
      } 
    } else if (args != null && args.length > 0 && args[0].equalsIgnoreCase("INLINE_SCRIPT")) {
      try {
        client.validate_mode = true;
        client.console_mode = false;
        client.getClass();
        client.br = new DualReader(client, new BufferedReader(new InputStreamReader(new ByteArrayInputStream((String.valueOf(args[1]) + ";").replaceAll(";", "\r\n").getBytes("UTF8")))));
        client.local_echo = true;
      } catch (Exception e) {
        client.printStackTrace(e, 1);
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
      genericClient.setConfigObj(this.source_config);
      if (this.prefs.getProperty("shared_session", "false").equals("true"))
        genericClient.setConfigObj((Properties)this.source_config.clone()); 
      genericClient.login(this.source_credentials.getProperty("username"), this.source_credentials.getProperty("password"), unique_client_id);
      if (genericClient.getConfig("crushAuth") != null)
        this.source_config.put("crushAuth", genericClient.getConfig("crushAuth")); 
      genericClient.setConfig("no_stat", "true");
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
    c.setConfigObj(this.dest_config);
    if (this.prefs.getProperty("shared_session", "false").equals("true")) {
      c.setConfigObj((Properties)this.dest_config.clone());
    } else if (this.dest_config.containsKey("pgpPublicKeyUploadPath")) {
      c.setConfig("pgpEncryptUpload", "true");
      c.setConfig("pgpDecryptDownload", "true");
      c.setConfig("pgpPublicKeyUploadPath", this.dest_config.getProperty("pgpPublicKeyUploadPath"));
      c.setConfig("pgpPrivateKeyDownloadPath", this.dest_config.getProperty("pgpPrivateKeyDownloadPath"));
    } 
    c.login(this.destination_credentials.getProperty("username"), this.destination_credentials.getProperty("password"), unique_client_id);
    if (c.getConfig("crushAuth") != null)
      this.dest_config.put("crushAuth", c.getConfig("crushAuth")); 
    c.setConfig("no_stat", "true");
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
    c.setConfig("transfer_history", null);
    c.setConfig("transfer_bytes_last", null);
    c.setConfig("transfer_bytes_last_interval", null);
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
      printStackTrace(e, 1);
    } 
  }
  
  public void run() {
    try {
      boolean echo;
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
        echo = this.local_echo;
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
          this.br = new DualReader(this, null);
          command = this.br.readLine();
        } 
        try {
          Thread.currentThread().setName("Command processor:" + command);
          process_command(command, echo);
          if (command.equalsIgnoreCase("quit") && this.local_echo)
            return; 
        } catch (Exception e) {
          printStackTrace(e, 1);
          if (command.equalsIgnoreCase("quit"))
            break; 
        } 
      } 
      process_command("quit now", echo);
      this.transfer_threads_started = false;
      return;
    } catch (Exception e) {
      printStackTrace(e, 1);
      if (System.getProperty("java.awt.headless", "false").equals("false"))
        System.exit(this.failed_transfer_queue.size() + this.additional_errors); 
      return;
    } 
  }
  
  public void run_transfers() {
    try {
      do {
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
            printStackTrace(e, 1);
          } 
        } else {
          Thread.sleep(100L);
        } 
      } while (this.transfer_threads_started);
    } catch (Exception e) {
      printStackTrace(e, 1);
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
      command[i] = replace_vars(v.elementAt(i).toString()); 
    return command;
  }
  
  public String replace_vars(String s) {
    s = Common.replace_str(s, "{space}", " ");
    return s;
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
      printStackTrace(e, 1);
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
                  this.this$0.printStackTrace(e, 1);
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
                      this.this$0.printStackTrace(e, 1);
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
      if (command[0].toUpperCase().startsWith("CONNECT") || command[0].toUpperCase().startsWith("LCONNECT") || command[0].toUpperCase().startsWith("VALIDATE")) {
        if (command[0].toUpperCase().startsWith("VALIDATE"))
          this.validate_mode = true; 
        if (source ? !this.source_logged_in : !this.destination_logged_in) {
          boolean was_tunnel = false;
          if (this.prefs.getProperty("use_tunnel", "").equals("true")) {
            this.prefs.remove("use_tunnel");
            process_command("TUNNEL " + command[1], false);
            command[1] = "tunnel";
          } 
          if (command[1].equalsIgnoreCase("tunnel")) {
            was_tunnel = true;
            command[1] = "http://" + VRL.vrlEncode(this.prefs.getProperty("last_tunnel_username")) + ":" + VRL.vrlEncode(this.prefs.getProperty("last_tunnel_password")) + "@127.0.0.1:" + this.prefs.getProperty("last_tunnel_port") + "/";
          } 
          if (!command[1].endsWith("/"))
            command[1] = String.valueOf(command[1]) + "/"; 
          VRL vrl = new VRL(command[1]);
          if (source) {
            this.current_source_dir = vrl.getPath();
          } else {
            this.current_dest_dir = vrl.getPath();
          } 
          String username = (vrl.getUserInfo() != null && vrl.getUserInfo().equals("")) ? "" : vrl.getUsername();
          if ((username == null || username.equals("")) && !vrl.getProtocol().equalsIgnoreCase("file") && this.interactive) {
            last_line_prompt = "message";
            System.out.print("(" + vrl + ") Username: ");
            username = this.br.readLine();
          } 
          String password = (vrl.getUserInfo() != null && vrl.getUserInfo().equals("")) ? "" : vrl.getPassword();
          if ((password == null || password.equals("") || password.equalsIgnoreCase("-ASK-")) && !vrl.getProtocol().equalsIgnoreCase("file") && this.interactive) {
            last_line_prompt = "message";
            System.out.print("(" + vrl + ") Password: ");
            if (password != null && password.equalsIgnoreCase("-ASK-")) {
              password = (new DualReader(this, null)).readPassword();
            } else {
              password = this.br.readPassword();
            } 
          } 
          if (was_tunnel) {
            credentials.put("display_vrl", (new VRL(this.prefs.getProperty("last_tunnel_url"))).safe());
          } else {
            credentials.put("display_vrl", vrl.safe());
          } 
          credentials.put("vrl", vrl);
          credentials.put("username", (username == null && vrl.getProtocol().equalsIgnoreCase("HTTPS") && !this.dest_config.getProperty("keystore_path", "").equals("")) ? "" : username);
          credentials.put("password", (password == null && vrl.getProtocol().equalsIgnoreCase("HTTPS") && !this.dest_config.getProperty("keystore_path", "").equals("")) ? "" : password);
          GenericClient c = null;
          try {
            if (!source)
              if (credentials.containsKey("pgpPublicKeyUploadPath")) {
                this.dest_config.put("pgpEncryptUpload", "true");
                this.dest_config.put("pgpDecryptDownload", "true");
                this.dest_config.put("pgpPublicKeyUploadPath", credentials.getProperty("pgpPublicKeyUploadPath"));
                this.dest_config.put("pgpPrivateKeyDownloadPath", credentials.getProperty("pgpPrivateKeyDownloadPath"));
              }  
            c = getClient(source);
            if (vrl.getProtocol().toUpperCase().startsWith("FTP:"))
              c.setConfig("pasv", "true"); 
            freeClient(c);
            if (source) {
              this.source_logged_in = true;
            } else {
              this.destination_logged_in = true;
            } 
            if (was_tunnel) {
              line("Connect to:" + (new VRL(this.prefs.getProperty("last_tunnel_url"))).safe());
            } else {
              line("Connected to:" + vrl.safe());
            } 
            return "true";
          } catch (Exception e) {
            printStackTrace(e, 1);
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
      if (command[0].toUpperCase().startsWith("PBE")) {
        String pbe_pass = Common.encryptDecrypt(command[1], true);
        this.prefs.put("md5_check", "false");
        this.prefs.put("skip_modified_and_size", "false");
        this.prefs.put("skip_modified", "true");
        credentials.put("pgpPublicKeyUploadPath", "password:" + pbe_pass);
        credentials.put("pgpPrivateKeyDownloadPath", "password:" + pbe_pass);
      } else if (command[0].toUpperCase().startsWith("DELAY")) {
        Thread.sleep(Integer.parseInt(command[1]));
      } else if (command[0].toUpperCase().startsWith("QUI") || command[0].toUpperCase().startsWith("BYE") || command[0].toUpperCase().startsWith("LQUI") || command[0].toUpperCase().startsWith("LBYE") || command[0].toUpperCase().startsWith("TERMINATE")) {
        long end_time = System.currentTimeMillis();
        if (this.local_echo && command_str.toUpperCase().indexOf("NOW") < 0) {
          Thread.sleep(1000L);
          process_command("WAIT", true);
        } 
        long transfer_history_bytes = Long.parseLong(this.stats.getProperty("upload_bytes", "0")) + Long.parseLong(this.stats.getProperty("download_bytes", "0"));
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
                  this.this$0.transfer_threads_started = false;
                } catch (Exception e) {
                  this.this$0.printStackTrace(e, 1);
                } 
                this.val$status.append("done");
              }
            });
        int loops = 0;
        while (loops++ < 50 && status.length() == 0)
          Thread.sleep(100L); 
        printStats(true);
        if (this.client_start_time == 0L)
          this.client_start_time = end_time - 2000L; 
        float speed = (float)transfer_history_bytes / (float)(end_time - this.client_start_time) / 1000.0F;
        line(stats_summary());
        this.stats.put("total_time", Common.format_time_pretty((end_time - this.client_start_time) / 1000L));
        this.stats.put("total_speed", Common.format_bytes_short((long)speed));
        line("Total time:" + this.stats.getProperty("total_time") + ", Avg Speed:" + this.stats.getProperty("total_speed"));
        print_prompt();
        if (this.interactive || command[0].toUpperCase().startsWith("TERMINATE"))
          if (System.getProperty("java.awt.headless", "false").equals("false"))
            if (this.validate_mode) {
              System.exit(this.additional_errors);
            } else {
              System.exit(0);
            }   
      } else {
        if (command[0].toUpperCase().startsWith("DIS") || command[0].toUpperCase().startsWith("LDIS")) {
          if (source) {
            this.source_config.clear();
          } else if (this.interactive) {
            this.dest_config.clear();
          } 
          credentials.remove("pgpPublicKeyUploadPath");
          credentials.remove("pgpPrivateKeyDownloadPath");
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
          Vector tunnel_list = (Vector)this.prefs.get("tunnel_list");
          if (tunnel_list != null && tunnel_list.size() > 0)
            process_command("TUNNEL stop 1", false); 
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
              printStackTrace(e, 1);
            } 
          } 
          while (this.destination_used.size() > 0) {
            try {
              GenericClient c_tmp = this.destination_used.remove(0);
              c_tmp.close();
              freeClient(c_tmp);
            } catch (Exception e) {
              printStackTrace(e, 1);
            } 
          } 
          this.pending_transfer_queue.removeAllElements();
          this.retry_active.removeAllElements();
          line("Transfers aborted if any were running.");
          Thread.sleep(900L);
          print_prompt();
          this.prefs.remove("aborting");
        } else if (command[0].toUpperCase().startsWith("AGENTUI")) {
          if (AgentUI.thisObj == null) {
            if (command.length > 1)
              if (command[1].toUpperCase().startsWith("HTTP")) {
                if (!command[1].trim().endsWith("/"))
                  command[1] = String.valueOf(command[1].trim()) + "/"; 
                System.getProperties().put("com.crushftp.server.httphandler.path", command[1].trim());
              }  
            this.messages = new Vector();
            this.messages2 = AgentUI.messages2;
            this.dual_log = true;
            System.setProperty("com.crushftp.server.httphandler", "com.crushftp.client.AgentUI");
            Worker.startWorker(new Runnable(this) {
                  final Client this$0;
                  
                  public void run() {
                    HTTPD.main(new String[0]);
                  }
                });
            while (AgentUI.thisObj == null && !this.prefs.containsKey("aborting"))
              Thread.sleep(100L); 
            AgentUI.thisObj.clients.put("command_line", this);
            line("WebUI started on port 33333.");
          } else {
            line("WebUI already started on port 33333.");
            Desktop.getDesktop().browse(new URI("http://127.0.0.1:33333/"));
          } 
          print_prompt();
        } else if (command[0].toUpperCase().startsWith("AGENT")) {
          if (AgentUI.thisObj == null) {
            if (command.length > 1)
              if (command[1].toUpperCase().startsWith("HTTP")) {
                if (!command[1].trim().endsWith("/"))
                  command[1] = String.valueOf(command[1].trim()) + "/"; 
                System.getProperties().put("com.crushftp.server.httphandler.path", command[1].trim());
              }  
            this.messages = new Vector();
            this.messages2 = AgentUI.messages2;
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
          } else {
            line("WebUI already started on port 33333.");
            Desktop.getDesktop().browse(new URI("http://127.0.0.1:33333/"));
          } 
          print_prompt();
        } else if (command[0].toUpperCase().startsWith("LOG")) {
          this.transfer_log = getArgs(command, 1, true, source);
          if (this.transfer_log.equalsIgnoreCase("NULL") || this.transfer_log.trim().equals("")) {
            line("Transfer logging disabled.");
            this.transfer_log = null;
          } else {
            line("Transfer logging enabled:" + this.transfer_log);
          } 
          print_prompt();
        } else if (command[0].toUpperCase().startsWith("SERVICE")) {
          if (command.length > 1 && command[1].toUpperCase().startsWith("REMOVE")) {
            Common.remove_windows_service("CrushTunnel", "CrushTunnel.jar");
            Common.remove_windows_service("CrushClient", "CrushTunnel.jar");
            (new File_S("./service/elevate.exe")).delete();
            line("Client and Tunnel service removed.");
          } else if (command.length > 1 && command[1].toUpperCase().startsWith("CLIENT")) {
            Common.install_windows_service(512, "CrushClient", "CrushTunnel.jar", false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Common.copyStreams(new FileInputStream("service/CrushClientService.ini"), baos, true, true);
            String config = new String(baos.toByteArray());
            String new_args = "";
            config = Common.replace_str(config, "arg.1=-d", new_args);
            Common.copyStreams(new ByteArrayInputStream(config.getBytes()), new FileOutputStream("service/CrushClientService.ini", false), true, true);
            Common.startDaemon(true, "CrushClient");
            Thread.sleep(500L);
            (new File("./service/elevate.exe")).delete();
            line("Client service installed.");
          } else if (command.length > 1 && command[1].toUpperCase().startsWith("TUNNEL")) {
            if (command.length < 7) {
              line("Invalid parameters.");
              line("service tunnel {protocol} {host} {port} {username} {password}");
              line("Example: ");
              line("service tunnel https www.crushftp.com demo demo");
            } else {
              Common.install_windows_service(512, "CrushTunnel", "CrushTunnel.jar", false);
              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              Common.copyStreams(new FileInputStream("service/CrushTunnelService.ini"), baos, true, true);
              String config = new String(baos.toByteArray());
              String new_args = "arg.1=protocol=" + command[2] + "\r\n";
              new_args = String.valueOf(new_args) + "arg.2=host=" + command[3] + "\r\n";
              new_args = String.valueOf(new_args) + "arg.3=port=" + command[4] + "\r\n";
              new_args = String.valueOf(new_args) + "arg.4=username=" + command[5] + "\r\n";
              new_args = String.valueOf(new_args) + "arg.5=password=" + command[6] + "\r\n";
              config = Common.replace_str(config, "arg.1=-d", new_args);
              Common.copyStreams(new ByteArrayInputStream(config.getBytes()), new FileOutputStream("service/CrushTunnelService.ini", false), true, true);
              Common.startDaemon(true, "CrushTunnel");
              Thread.sleep(500L);
              (new File("./service/elevate.exe")).delete();
              line("Tunnel service installed.");
            } 
          } 
          print_prompt();
        } else {
          if (command[0].toUpperCase().startsWith("CONFIG") || command[0].toUpperCase().startsWith("LCONFIG")) {
            String key = command[1];
            String value = "";
            if (key.equalsIgnoreCase("reset")) {
              if (source) {
                this.source_config.clear();
              } else {
                this.dest_config.clear();
              } 
            } else {
              value = command[2];
              if (key.equals("keystore_pass") || key.equals("truststore_pass"))
                value = Common.encryptDecrypt(value, true); 
              if (source ? !this.source_logged_in : !this.destination_logged_in) {
                if (source) {
                  this.source_config.put(key, value);
                } else {
                  this.dest_config.put(key, value);
                } 
              } else {
                GenericClient genericClient = null;
                try {
                  genericClient = getClient(source);
                } catch (NullPointerException nullPointerException) {}
                genericClient.setConfig(key, value);
                freeClient(genericClient);
              } 
            } 
            return line("\"" + key + "\" -> \"" + value + "\": config command successful.");
          } 
          if (command[0].toUpperCase().startsWith("VMPROP")) {
            String key = command[1];
            String value = "";
            System.getProperties().put(key, command[2]);
            return line("\"" + key + "\" -> \"" + value + "\": System Property configured.");
          } 
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
                printStackTrace(e, 1);
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
              Properties listingProp = new Properties();
              try {
                if (c instanceof HTTPClient) {
                  listingProp = ((HTTPClient)c).list2(the_dir, list);
                } else {
                  c.list(the_dir, list);
                } 
                listingProp.put("listing", list);
              } catch (Exception e) {
                printStackTrace(e, 1);
                return "ERROR:" + e;
              } 
              last_line_prompt = "message";
              for (int x = 0; x < list.size(); x++)
                line(format_ls_la(list.elementAt(x))); 
              return listingProp;
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
                    if (this.prefs.containsKey("aborting"))
                      break; 
                  } 
                  line("\"" + count + "\" items to delete...");
                  for (x = 0; x < list.size(); x++) {
                    if (this.prefs.containsKey("aborting"))
                      break; 
                    String name = ((Properties)list.elementAt(x)).getProperty("name");
                    if (Common.do_search(command[1], name, false, 0))
                      try {
                        c.delete(String.valueOf(parent_dir) + name);
                        line("\"" + name + "\" delete command successful.");
                      } catch (Exception e) {
                        printStackTrace(e, 1);
                      }  
                  } 
                } catch (Exception e) {
                  printStackTrace(e, 1);
                } 
              } else {
                c.setConfig("file_recurse_delete", "true");
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
                  this.additional_errors++;
                  if (this.local_echo)
                    this.br.close(); 
                  return line("\"" + the_dir + "\": MKD failed.");
                } 
                synchronized (this.stats) {
                  if (command[0].toUpperCase().startsWith("L")) {
                    this.stats.put("download_folders", (new StringBuffer(String.valueOf(Integer.parseInt(this.stats.getProperty("download_folders", "0")) + 1))).toString());
                  } else {
                    this.stats.put("upload_folders", (new StringBuffer(String.valueOf(Float.parseFloat(this.stats.getProperty("upload_folders", "0.0")) + 0.5D))).toString());
                  } 
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
                if (!ok) {
                  this.additional_errors++;
                  return line("\"" + the_dir1 + "\" -> \"" + the_dir2 + "\": rename failed.");
                } 
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
              if (command[0].toUpperCase().startsWith("TIMEOUT") || command[0].toUpperCase().startsWith("LTIMEOUT")) {
                if (source) {
                  this.source_config.put("timeout", command[1]);
                } else {
                  this.dest_config.put("timeout", command[1]);
                } 
                return line("timeout set : " + command[1]);
              } 
              if (command[0].toUpperCase().startsWith("KILL")) {
                String[] command_f = command;
                return line("timeout set : " + command[1] + command[2]);
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
              if (command[0].toUpperCase().startsWith("TUNNEL") || command[0].toUpperCase().startsWith("LTUNNEL")) {
                if (command[1].equalsIgnoreCase("gui"))
                  while (true)
                    Thread.sleep(1000L);  
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
                    tunnel_list.remove(i - 1);
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
                  VRL vrl = new VRL(command[1].endsWith("/") ? command[1] : (String.valueOf(command[1]) + "/"));
                  String tunnel_username = vrl.getUsername();
                  if (tunnel_username == null || tunnel_username.equals("")) {
                    last_line_prompt = "message";
                    System.out.print("Tunnel Username: ");
                    tunnel_username = this.br.readLine();
                  } 
                  this.prefs.put("last_tunnel_username", tunnel_username);
                  this.prefs.put("last_tunnel_url", vrl);
                  String tunnel_username_f = tunnel_username;
                  String tunnel_password = vrl.getPassword();
                  if (tunnel_password == null || tunnel_password.equals("")) {
                    last_line_prompt = "message";
                    System.out.print("Tunnel Password: ");
                    tunnel_password = this.br.readPassword();
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
                    if (t.tunnel.size() == 0) {
                      t.stopThisTunnel();
                      GenericClient c_test = null;
                      try {
                        c_test = Common.getClient(Common.getBaseUrl(vrl.toString()), "CrushClient:", this.logQueue);
                        c_test.login(tunnel_username_f, tunnel_password_f, unique_client_id);
                      } finally {
                        c_test.logout();
                        Worker.startWorker(new Runnable(this) {
                              final Client this$0;
                              
                              public void run() {
                                int loops = 0;
                                while (loops++ < 5) {
                                  if (this.this$0.tunnel_log.size() > 0) {
                                    while (this.this$0.tunnel_log.size() > 0)
                                      this.this$0.line(this.this$0.tunnel_log.remove(0).toString()); 
                                    continue;
                                  } 
                                  try {
                                    Thread.sleep(1000L);
                                  } catch (InterruptedException interruptedException) {}
                                } 
                              }
                            });
                      } 
                      line("No tunnels configured for this account.");
                    } else {
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
                  } catch (Exception e) {
                    line("Tunnel failed:" + e);
                    printStackTrace(e, 1);
                  } 
                  print_prompt();
                } 
              } else if (command[0].toUpperCase().startsWith("CIPHER") || command[0].toUpperCase().startsWith("LCIPHER")) {
                if (command[1].equalsIgnoreCase("list")) {
                  Vector cipher_suites = URLConnection.cipher_suites;
                  if (cipher_suites == null) {
                    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.crushftp.com/WebInterface/login.html"), new Properties());
                    urlc.getResponseCode();
                    urlc.disconnect();
                    cipher_suites = URLConnection.cipher_suites;
                  } 
                  if (cipher_suites == null) {
                    line("Cannot get list until a connection has been made.");
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
              } else if (command[0].toUpperCase().startsWith("GET") || command[0].toUpperCase().startsWith("REGET") || command[0].toUpperCase().startsWith("LAPPE")) {
                if (this.client_start_time == 0L)
                  this.client_start_time = System.currentTimeMillis(); 
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
                  if (this.prefs.getProperty("simple", "false").equals("true"))
                    the_dir = command[1]; 
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
                      printStackTrace(e, 1);
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
                  boolean folder = false;
                  long source_size = 0L;
                  String success_end_str = "";
                  try {
                    c_source = getClient(true);
                    c_dest = getClient(false);
                    long start_pos = 0L;
                    Properties source_stat = null;
                    if (!the_dir_source.startsWith("/WebInterface/"))
                      source_stat = c_source.stat(the_dir_source); 
                    if (source_stat != null)
                      source_size = Long.parseLong(source_stat.getProperty("size", "0")); 
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
                          while (this.pending_transfer_queue.size() > 1000000)
                            Thread.sleep(100L); 
                        } catch (Exception e) {
                          printStackTrace(e, 1);
                          line("Error:" + e);
                        } 
                      } 
                    } else {
                      if (resume)
                        start_pos = source_size; 
                      long start_pos1 = start_pos;
                      long start_pos2 = start_pos;
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
                        synchronized (this.stats) {
                          this.stats.put("download_skipped_count", (new StringBuffer(String.valueOf(Integer.parseInt(this.stats.getProperty("download_skipped_count", "0")) + 1))).toString());
                          this.stats.put("download_skipped_bytes", (new StringBuffer(String.valueOf(Long.parseLong(this.stats.getProperty("download_skipped_bytes", "0")) + Long.parseLong(dest_stat.getProperty("size"))))).toString());
                        } 
                        freeClient(c_source);
                        freeClient(c_dest);
                        print_prompt();
                      } else {
                        if (command[0].toUpperCase().startsWith("LAPPE")) {
                          start_pos1 = 0L;
                          start_pos2 = 0L;
                          if (source_stat != null)
                            start_pos2 = Long.parseLong(source_stat.getProperty("size", "0")); 
                          resume = false;
                        } 
                        InputStream in_f = c_dest.download(the_dir, start_pos1, -1L, true);
                        in = in_f;
                        c_source.setConfig("transfer_direction", "GET");
                        c_dest.setConfig("transfer_direction", "PUT");
                        c_source.setConfig("transfer_path_dst", the_dir_source);
                        c_dest.setConfig("transfer_path_src", the_dir);
                        c_dest.setConfig("transfer_path_dst", the_dir_source);
                        c_source.setConfig("transfer_path_src", the_dir);
                        c_source.setConfig("transfer_stats", "true");
                        c_dest.setConfig("transfer_stats", "true");
                        OutputStream out_f = c_source.upload(the_dir_source, start_pos2, true, true);
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
                        c_dest_f.setConfig("transfer_bytes_last", "0");
                        c_source_f.setConfig("transfer_history", new Vector());
                        c_dest_f.setConfig("transfer_history", new Vector());
                        c_source_f.setConfig("transfer_bytes_last", "0");
                        c_dest_f.setConfig("transfer_bytes_last_interval", "0");
                        c_source_f.setConfig("transfer_bytes_last_interval", "0");
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
                                      this.this$0.printStackTrace(e, 1);
                                    } 
                                  } 
                                }
                              }); 
                        long total_bytes = 0L;
                        try {
                          MessageDigest m = MessageDigest.getInstance("MD5");
                          byte[] b = new byte[32768];
                          int bytes_read = 0;
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
                          while (md5.length() < 32)
                            md5 = "0" + md5; 
                          success_end_str = " : " + (System.currentTimeMillis() - start) + "ms, " + speed_str + ", size=" + total_bytes + ", md5=" + md5;
                          line("Download completed:" + the_dir_dest_f + " -> " + the_dir_source_f + success_end_str);
                          if (this.prefs.getProperty("keep_date", "true").equals("true")) {
                            SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                            if (dest_stat_f != null)
                              process_command("LMDTM \"" + the_dir_source_f + "\" " + sdf_yyyyMMddHHmmss.format(new Date(Long.parseLong(dest_stat_f.getProperty("modified")))), true); 
                          } 
                          print_prompt();
                        } catch (Exception e) {
                          transfer_error = e;
                          printStackTrace(e, 1);
                          line("Error:" + e);
                        } 
                        freeClient(c_source_f);
                        freeClient(c_dest_f);
                        this.recent_transfers_download.addElement(dest_stat_f);
                        synchronized (this.stats) {
                          this.stats.put("download_count", (new StringBuffer(String.valueOf(Integer.parseInt(this.stats.getProperty("download_count", "0")) + 1))).toString());
                          this.stats.put("download_bytes", (new StringBuffer(String.valueOf(Long.parseLong(this.stats.getProperty("download_bytes", "0")) + total_bytes))).toString());
                        } 
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
                        if (idle_time >= 10 && transfer_error == null) {
                          line("Transfer complete.  " + stats_summary());
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
                    printStackTrace(e, 1);
                    line("Error:" + e);
                  } 
                  if (transfer_error == null || this.prefs.containsKey("aborting") || transfer_error.indexOf("403") >= 0 || transfer_error.indexOf("404") >= 0 || transfer_error.indexOf("denied") >= 0 || transfer_error.indexOf("not allowed") >= 0 || transfer_error.toLowerCase().indexOf("no such file") >= 0) {
                    if (transfer_error != null) {
                      line("Ended with error:" + transfer_error + ":" + command_str);
                      this.failed_transfer_queue.addElement(command_str);
                      add_transfer_log("ERROR:" + command_str + ":" + transfer_error);
                    } else if (!folder) {
                      add_transfer_log("SUCCESS:" + command_str + success_end_str);
                      this.success_transfer_queue.addElement(String.valueOf(command_str) + success_end_str);
                    } 
                    while (this.success_transfer_queue.size() > 1000)
                      this.success_transfer_queue.remove(0); 
                    while (this.failed_transfer_queue.size() > 1000)
                      this.failed_transfer_queue.remove(0); 
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
              } else if (command[0].toUpperCase().startsWith("PUT") || command[0].toUpperCase().startsWith("REPUT") || command[0].toUpperCase().startsWith("APPE") || command[0].toUpperCase().startsWith("PUTDEL") || command[0].toUpperCase().startsWith("PUTSYNC") || command[0].toUpperCase().startsWith("MOVE")) {
                if (this.client_start_time == 0L)
                  this.client_start_time = System.currentTimeMillis(); 
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
                boolean resume = !(!command[0].toUpperCase().startsWith("REPUT") && !command[0].toUpperCase().startsWith("APPE"));
                while (!this.prefs.containsKey("aborting")) {
                  transfer_error = null;
                  c = null;
                  String the_dir = getArgs(command, 1, false, true);
                  String the_dir_dest = getArgs(command, 2, false, false);
                  if (this.prefs.getProperty("simple", "false").equals("true"))
                    the_dir_dest = command[2]; 
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
                            line("Skipping invalid filename for wildcard upload:" + p.getProperty("name"));
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
                      printStackTrace(e, 1);
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
                  boolean folder = false;
                  String success_end_str = "";
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
                      if (command[0].toUpperCase().startsWith("PUTDEL")) {
                        Properties local_lookup = new Properties();
                        for (int x = 0; x < list.size(); x++) {
                          Properties p = list.elementAt(x);
                          local_lookup.put(p.getProperty("name"), p.getProperty("size"));
                        } 
                        Vector list_remote = new Vector();
                        c2.list(the_dir_dest, list_remote);
                        freeClient(c1);
                        freeClient(c2);
                        for (int i = 0; i < list_remote.size(); i++) {
                          Properties p = list_remote.elementAt(i);
                          if (!local_lookup.containsKey(p.getProperty("name"))) {
                            line(String.valueOf(the_dir_dest) + p.getProperty("name") + ":Deleting item because no matching local item was found.");
                            if (p.getProperty("type").equalsIgnoreCase("DIR")) {
                              process_command("RMD \"" + the_dir_dest + p.getProperty("name") + "\"", true);
                            } else {
                              process_command("DEL \"" + the_dir_dest + p.getProperty("name") + "\"", true);
                            } 
                          } 
                        } 
                      } else if (command[0].toUpperCase().startsWith("PUTSYNC")) {
                        Properties local_lookup = new Properties();
                        for (int x = 0; x < list.size(); x++) {
                          Properties p = list.elementAt(x);
                          local_lookup.put(p.getProperty("name"), p.getProperty("size"));
                        } 
                        Vector list_remote = new Vector();
                        c2.list(the_dir_dest, list_remote);
                        if (c2 instanceof HTTPClient && !this.dest_config.containsKey("sync_hour_offset")) {
                          String result = c2.doCommand("SITE TIME");
                          if (result.startsWith("214 ")) {
                            long diff = System.currentTimeMillis() - Long.parseLong(result.substring(4).trim());
                            this.prefs.put("sync_hour_offset", (new StringBuffer(String.valueOf(diff / 3600000L))).toString());
                            this.dest_config.put("sync_hour_offset", this.prefs.getProperty("sync_hour_offset"));
                          } 
                        } 
                        freeClient(c1);
                        freeClient(c2);
                        Properties remote_lookup = new Properties();
                        int i;
                        for (i = 0; i < list_remote.size(); i++) {
                          Properties p = list_remote.elementAt(i);
                          remote_lookup.put(p.getProperty("name"), p.getProperty("size"));
                          if (!local_lookup.containsKey(p.getProperty("name")))
                            if (Long.parseLong(p.getProperty("modified")) < Long.parseLong(this.prefs.getProperty("sync_last_run", "0")) - 3600000L * Long.parseLong(this.prefs.getProperty("sync_hour_offset", "0"))) {
                              line(String.valueOf(the_dir_dest) + p.getProperty("name") + ":Deleting item because no matching local item was found.");
                              if (p.getProperty("type").equalsIgnoreCase("DIR")) {
                                process_command("RMD \"" + the_dir_dest + p.getProperty("name") + "\"", true);
                              } else {
                                process_command("DEL \"" + the_dir_dest + p.getProperty("name") + "\"", true);
                              } 
                            } else if (Long.parseLong(p.getProperty("modified")) > Long.parseLong(this.prefs.getProperty("pending_sync_last_run", "0")) - 3600000L * Long.parseLong(this.prefs.getProperty("sync_hour_offset", "0"))) {
                              line(String.valueOf(the_dir_dest) + p.getProperty("name") + ":Downloading item because no matching local item was found.");
                              if (p.getProperty("type").equalsIgnoreCase("DIR")) {
                                process_command("GET \"" + the_dir_dest + p.getProperty("name") + "\"", true);
                              } else {
                                process_command("GET \"" + the_dir_dest + p.getProperty("name") + "\" \"" + the_dir + p.getProperty("name") + "\"", true);
                              } 
                            }  
                        } 
                        for (i = list.size() - 1; i >= 0; i--) {
                          Properties p = list.elementAt(i);
                          if (!remote_lookup.containsKey(p.getProperty("name")))
                            if (Long.parseLong(p.getProperty("modified")) < Long.parseLong(this.prefs.getProperty("sync_last_run", "0")) - 3600000L * Long.parseLong(this.prefs.getProperty("sync_hour_offset", "0"))) {
                              line(String.valueOf(the_dir_dest) + p.getProperty("name") + ":Deleting item because no matching local item was found.");
                              if (p.getProperty("type").equalsIgnoreCase("DIR")) {
                                process_command("LRMD \"" + the_dir + p.getProperty("name") + "\"", true);
                              } else {
                                process_command("LDEL \"" + the_dir + p.getProperty("name") + "\"", true);
                              } 
                            } else if (Long.parseLong(p.getProperty("modified")) > Long.parseLong(this.prefs.getProperty("pending_sync_last_run", "0")) - 3600000L * Long.parseLong(this.prefs.getProperty("sync_hour_offset", "0")) && Long.parseLong(this.prefs.getProperty("pending_sync_last_run", "0")) > 0L) {
                              list.remove(i);
                            }  
                        } 
                      } else {
                        freeClient(c1);
                        freeClient(c2);
                      } 
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
                          while (this.pending_transfer_queue.size() > 1000000)
                            Thread.sleep(100L); 
                        } catch (Exception e) {
                          printStackTrace(e, 1);
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
                        synchronized (this.stats) {
                          this.stats.put("upload_skipped_count", (new StringBuffer(String.valueOf(Integer.parseInt(this.stats.getProperty("upload_skipped_count", "0")) + 1))).toString());
                          this.stats.put("upload_skipped_bytes", (new StringBuffer(String.valueOf(Long.parseLong(this.stats.getProperty("upload_skipped_bytes", "0")) + Long.parseLong(source_stat.getProperty("size"))))).toString());
                        } 
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
                        c1.setConfig("transfer_direction", "GET");
                        c2.setConfig("transfer_direction", "PUT");
                        c1.setConfig("transfer_path_src", the_dir);
                        c2.setConfig("transfer_path_dst", the_dir_dest);
                        c2.setConfig("transfer_path_src", the_dir);
                        c1.setConfig("transfer_path_dst", the_dir_dest);
                        c1.setConfig("transfer_stats", "true");
                        c2.setConfig("transfer_stats", "true");
                        c2.setConfig("transfer_content_length", source_stat.getProperty("size"));
                        if (this.pending_transfer_queue.size() == 0 && this.prefs.getProperty("makedir_before", "true").equals("true"))
                          c2.makedirs(Common.all_but_last(the_dir_dest)); 
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
                        c2.setConfig("transfer_history", new Vector());
                        c1.setConfig("transfer_history", new Vector());
                        c2.setConfig("transfer_bytes_last", "0");
                        c1.setConfig("transfer_bytes_last", "0");
                        c2.setConfig("transfer_bytes_last_interval", "0");
                        c2.setConfig("transfer_content_length", source_stat.getProperty("size"));
                        c1.setConfig("transfer_bytes_last_interval", "0");
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
                                      this.this$0.printStackTrace(e, 1);
                                    } 
                                  } 
                                }
                              });
                        } 
                        long total_bytes = 0L;
                        try {
                          MessageDigest m = MessageDigest.getInstance("MD5");
                          byte[] b = new byte[32768];
                          int bytes_read = 0;
                          total_bytes = resume ? start_pos : 0L;
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
                          String last_md5 = c2.getConfig("last_md5", "");
                          if (!this.prefs.getProperty("upload_temp_ext", "").equals(""))
                            c2.rename(String.valueOf(the_dir_dest_f) + this.prefs.getProperty("upload_temp_ext", ""), the_dir_dest_f); 
                          c1.setConfig("transfer_stats", null);
                          c2.setConfig("transfer_stats", null);
                          if (!multithreaded)
                            printStats(true); 
                          float speed = 10.0F * (float)total_bytes / 1024.0F / (float)(System.currentTimeMillis() - start) / 1000.0F;
                          String speed_str = (speed > 10240.0F) ? (String.valueOf((int)(speed / 1024.0F) / 10.0F) + "MB/sec") : (String.valueOf((float)((int)speed / 10.0D)) + "KB/sec");
                          String md5 = (new BigInteger(1, m.digest())).toString(16).toLowerCase();
                          while (md5.length() < 32)
                            md5 = "0" + md5; 
                          VRL vrl = (VRL)credentials.get("vrl");
                          if (vrl.getProtocol().toLowerCase().startsWith("http") && !md5.equals(last_md5) && this.prefs.getProperty("md5_check", "true").equals("true")) {
                            c2.delete(the_dir_dest_f);
                            throw new Exception("md5 mismatch:local " + md5 + " does not equal remote " + last_md5);
                          } 
                          success_end_str = " : " + (System.currentTimeMillis() - start) + "ms, " + speed_str + ", size=" + total_bytes + ", md5=" + md5 + ((md5.equals(last_md5) && this.prefs.getProperty("md5_check", "true").equals("true")) ? " (validated)" : "");
                          line("Upload completed:" + the_dir_source_f + " -> " + the_dir_dest_f + success_end_str);
                          if (this.prefs.getProperty("keep_date", "true").equals("true")) {
                            SimpleDateFormat sdf_yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
                            process_command("MDTM \"" + the_dir_dest_f + "\" " + sdf_yyyyMMddHHmmss.format(new Date(Long.parseLong(source_stat_f.getProperty("modified")))), true);
                          } 
                          if (command[0].toUpperCase().startsWith("MOVE"))
                            if (c1.delete(the_dir)) {
                              line("Local file deleted:" + the_dir_source_f);
                            } else {
                              line("Local file delete failed:" + the_dir_source_f);
                            }  
                          if (c1 != null)
                            c1 = freeClient(c1); 
                          if (c2 != null)
                            c2 = freeClient(c2); 
                          print_prompt();
                        } catch (Exception e) {
                          transfer_error = e;
                          line("Error:" + e);
                        } 
                        if (c1 != null)
                          c1 = freeClient(c1); 
                        if (c2 != null)
                          c2 = freeClient(c2); 
                        if (!folder)
                          this.recent_transfers_upload.addElement(source_stat_f); 
                        synchronized (this.stats) {
                          this.stats.put("upload_count", (new StringBuffer(String.valueOf(Integer.parseInt(this.stats.getProperty("upload_count", "0")) + 1))).toString());
                          this.stats.put("upload_bytes", (new StringBuffer(String.valueOf(Long.parseLong(this.stats.getProperty("upload_bytes", "0")) + total_bytes - start_pos))).toString());
                        } 
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
                        if (idle_time >= 10 && transfer_error == null) {
                          line("Transfer complete.  " + stats_summary());
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
                    freeClient(c1);
                    freeClient(c2);
                    printStackTrace(e, 1);
                    line("Error:" + e);
                  } 
                  if ((!resume || transfer_error == null) && (transfer_error == null || this.prefs.containsKey("aborting") || transfer_error.indexOf("403") >= 0 || transfer_error.indexOf("404") >= 0 || transfer_error.indexOf("denied") >= 0 || transfer_error.indexOf("not allowed") >= 0)) {
                    if (transfer_error != null) {
                      line("Ended with error:" + transfer_error + ":" + command_str);
                      this.failed_transfer_queue.addElement(command_str);
                      add_transfer_log("ERROR:" + command_str + ":" + transfer_error);
                    } else if (!folder) {
                      add_transfer_log("SUCCESS:" + command_str + success_end_str);
                      this.success_transfer_queue.addElement(String.valueOf(command_str) + success_end_str);
                    } 
                    while (this.success_transfer_queue.size() > 1000)
                      this.success_transfer_queue.remove(0); 
                    while (this.failed_transfer_queue.size() > 1000)
                      this.failed_transfer_queue.remove(0); 
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
                    if (resume && transfer_error != null && (transfer_error.indexOf("403") >= 0 || transfer_error.indexOf("404") >= 0 || transfer_error.indexOf("denied") >= 0 || transfer_error.indexOf("not allowed") >= 0)) {
                      resume = false;
                    } else {
                      resume = true;
                    } 
                    synchronized (this.retry_active) {
                      this.retry_active.removeElementAt(0);
                    } 
                  } 
                } 
              } else if (command[0].toUpperCase().startsWith("DIFFPUT") || command[0].toUpperCase().startsWith("DIFFGET")) {
                if (this.client_start_time == 0L)
                  this.client_start_time = System.currentTimeMillis(); 
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
                      while (this.pending_transfer_queue.size() > 1000000)
                        Thread.sleep(100L); 
                    } catch (Exception e) {
                      printStackTrace(e, 1);
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
                    long total_bytes_summary = 0L;
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
                        c_dest.setConfig("transfer_history", new Vector());
                        c_source.setConfig("transfer_history", new Vector());
                        c_dest.setConfig("transfer_bytes_last", "0");
                        c_source.setConfig("transfer_bytes_last", "0");
                        c_dest.setConfig("transfer_bytes_last_interval", "0");
                        c_source.setConfig("transfer_bytes_last_interval", "0");
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
                                      this.this$0.printStackTrace(e, 1);
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
                              total_bytes_summary += bytes_read;
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
                        printStackTrace(e, 1);
                        line("Error:" + e);
                      } 
                    } 
                    if (byteRanges.size() > 0) {
                      if (upload) {
                        this.recent_transfers_upload.addElement(dest_stat);
                        synchronized (this.stats) {
                          this.stats.put("upload_count", (new StringBuffer(String.valueOf(Integer.parseInt(this.stats.getProperty("upload_count", "0")) + 1))).toString());
                          this.stats.put("upload_bytes", (new StringBuffer(String.valueOf(Long.parseLong(this.stats.getProperty("upload_bytes", "0")) + total_bytes_summary))).toString());
                        } 
                      } else {
                        this.recent_transfers_download.addElement(source_stat);
                        synchronized (this.stats) {
                          this.stats.put("download_count", (new StringBuffer(String.valueOf(Integer.parseInt(this.stats.getProperty("download_count", "0")) + 1))).toString());
                          this.stats.put("download_bytes", (new StringBuffer(String.valueOf(Long.parseLong(this.stats.getProperty("download_bytes", "0")) + total_bytes_summary))).toString());
                        } 
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
                      line(String.valueOf(the_dir_opposite) + ":Diff transfer completed, " + byteRanges.size() + " parts transferred.  " + stats_summary());
                    } else {
                      line(String.valueOf(the_dir_opposite) + ":Diff transfer completed, no changes.  " + stats_summary());
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
                      printStackTrace(e, 1);
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
                } else if (command[0].toUpperCase().startsWith("DUMPSTACK")) {
                  line(Common.dumpStack("1.5.1"));
                  last_line_prompt = "message";
                  print_prompt();
                } else if (command[0].toUpperCase().startsWith("DUMPMEMORY")) {
                  line((new HeapDumper()).dump());
                  last_line_prompt = "message";
                  print_prompt();
                } else if (command[0].toUpperCase().startsWith("JOB")) {
                  if (command[1].equalsIgnoreCase("remote")) {
                    GenericClient c_dest = getClient(false);
                    Properties config = c_dest.config;
                    URLConnection urlc = URLConnection.openConnection((VRL)credentials.get("vrl"), config);
                    urlc.setRequestMethod("POST");
                    urlc.setRequestProperty("Cookie", "CrushAuth=" + config.getProperty("crushAuth", "") + ";");
                    urlc.setUseCaches(false);
                    urlc.setDoOutput(true);
                    String c2f = "";
                    if (!config.getProperty("crushAuth", "").equals(""))
                      c2f = config.getProperty("crushAuth", "").substring(config.getProperty("crushAuth", "").length() - 4); 
                    String extra_params = "";
                    if (command.length > 3)
                      for (int x = 3; x < command.length; x++)
                        extra_params = String.valueOf(extra_params) + "&" + command[x];  
                    urlc.getOutputStream().write(("c2f=" + c2f + "&command=testJobSchedule&scheduleName=" + GenericClient.u(command[2]) + extra_params).getBytes("UTF8"));
                    line(String.valueOf(command[1]) + " " + command[2]);
                    int code = 302;
                    String result = "";
                    try {
                      code = urlc.getResponseCode();
                      result = Common.consumeResponse(urlc.getInputStream());
                    } catch (Exception e) {
                      Common.log("HTTP_CLIENT", 1, e);
                    } 
                    if (code != 302 && urlc.getURL().toString().indexOf("/WebInterface/login.html") >= 0)
                      code = 302; 
                    urlc.disconnect();
                    if (result.indexOf("<response>") >= 0)
                      result = result.substring(result.indexOf("<response>") + "<response>".length(), result.lastIndexOf("</response>")); 
                    line(result);
                    freeClient(c_dest);
                  } else if (command[1].equalsIgnoreCase("status")) {
                    GenericClient c_dest = getClient(false);
                    Properties config = c_dest.config;
                    URLConnection urlc = URLConnection.openConnection((VRL)credentials.get("vrl"), config);
                    urlc.setRequestMethod("POST");
                    urlc.setRequestProperty("Cookie", "CrushAuth=" + config.getProperty("crushAuth", "") + ";");
                    urlc.setUseCaches(false);
                    urlc.setDoOutput(true);
                    String c2f = "";
                    if (!config.getProperty("crushAuth", "").equals(""))
                      c2f = config.getProperty("crushAuth", "").substring(config.getProperty("crushAuth", "").length() - 4); 
                    urlc.getOutputStream().write(("c2f=" + c2f + "&command=getJobsSummary&type=text&end_time=" + System.currentTimeMillis() + "&scheduleName=" + command[2]).getBytes("UTF8"));
                    line(String.valueOf(command[1]) + " " + command[2]);
                    int code = 302;
                    String result = "";
                    try {
                      code = urlc.getResponseCode();
                      result = Common.consumeResponse(urlc.getInputStream());
                    } catch (Exception e) {
                      Common.log("HTTP_CLIENT", 1, e);
                    } 
                    if (code != 302 && urlc.getURL().toString().indexOf("/WebInterface/login.html") >= 0)
                      code = 302; 
                    urlc.disconnect();
                    if (result.indexOf("<response>") >= 0)
                      result = result.substring(result.indexOf("<response>") + "<response>".length(), result.lastIndexOf("</response>")); 
                    line(result);
                    freeClient(c_dest);
                  } else {
                    File job = new File("./jobs/" + command[1]);
                    Properties params = (Properties)Common.readXMLObject(String.valueOf(job.getPath()) + "/job.XML");
                    params.put("debug", "true");
                    Properties event = new Properties();
                    event.put("event_plugin_list", "CrushTask");
                    event.put("name", "ScheduledPluginEvent:" + params.getProperty("scheduleName"));
                    params.put("new_job_id", Common.makeBoundary(20));
                    event.putAll(params);
                    Properties info = new Properties();
                    info.put("action", "event");
                    info.put("server_settings", new Properties());
                    info.put("event", event);
                    info.put("items", new Vector());
                    Start crush_task = new Start();
                    crush_task.setSettings(params);
                    crush_task.run(info);
                    line(String.valueOf(command_str) + " completed.");
                  } 
                  last_line_prompt = "message";
                  print_prompt();
                } else if (command[0].toUpperCase().startsWith("USER")) {
                  GenericClient c_dest = getClient(false);
                  Properties config = c_dest.config;
                  URLConnection urlc = URLConnection.openConnection((VRL)credentials.get("vrl"), config);
                  urlc.setRequestMethod("POST");
                  urlc.setRequestProperty("Cookie", "CrushAuth=" + config.getProperty("crushAuth", "") + ";");
                  urlc.setUseCaches(false);
                  urlc.setDoOutput(true);
                  String c2f = "";
                  if (!config.getProperty("crushAuth", "").equals(""))
                    c2f = config.getProperty("crushAuth", "").substring(config.getProperty("crushAuth", "").length() - 4); 
                  if (command[1].equalsIgnoreCase("user_add") || command[1].equalsIgnoreCase("user_update") || command[1].equalsIgnoreCase("user_delete")) {
                    String data_action = "replace";
                    if (command[1].equalsIgnoreCase("user_update")) {
                      data_action = "update";
                    } else if (command[1].equalsIgnoreCase("user_delete")) {
                      data_action = "delete";
                    } 
                    urlc.getOutputStream().write(("c2f=" + c2f + "&command=setUserItem&xmlItem=user&data_action=" + data_action + "&serverGroup=" + GenericClient.u(command[2]) + "&username=" + GenericClient.u(command[3])).getBytes("UTF8"));
                    Properties p = new Properties();
                    for (int x = 4; x < command.length; x++) {
                      String key = command[x].split("=")[0].trim();
                      String val = command[x].substring(command[x].indexOf("=") + 1).trim();
                      p.put(Common.url_decode(key), Common.url_decode(val));
                    } 
                    p.put("root_dir", "/");
                    p.put("username", command[3]);
                    p.put("user_name", command[3]);
                    String user_xml = Common.getXMLString(p, "user");
                    urlc.getOutputStream().write(("&user=" + GenericClient.u(user_xml)).getBytes("UTF8"));
                    Properties permissions = new Properties();
                    permissions.put("/", "(read)(view)(resume)");
                    String permissions_xml = Common.getXMLString(permissions, "privs");
                    if (data_action.equals("replace")) {
                      urlc.getOutputStream().write(("&permissions=" + GenericClient.u(permissions_xml)).getBytes("UTF8"));
                      String vfs_items_xml = Common.getXMLString(new Vector(), "VFS");
                      urlc.getOutputStream().write(("&vfs_items=" + GenericClient.u(vfs_items_xml)).getBytes("UTF8"));
                    } 
                    line(String.valueOf(command[1]) + " " + command[3] + " " + p.toString());
                  } else if (command[1].equalsIgnoreCase("vfs_add") || command[1].equalsIgnoreCase("vfs_delete")) {
                    String data_action = "update_vfs";
                    if (command[1].equalsIgnoreCase("vfs_delete"))
                      data_action = "update_vfs_remove"; 
                    urlc.getOutputStream().write(("c2f=" + c2f + "&command=setUserItem&xmlItem=user&data_action=" + data_action + "&serverGroup=" + GenericClient.u(command[2]) + "&username=" + GenericClient.u(command[3])).getBytes("UTF8"));
                    Vector vfs_items = new Vector();
                    Properties permissions = new Properties();
                    Properties p = new Properties();
                    Properties p_parent = new Properties();
                    vfs_items.addElement(p_parent);
                    Vector vfs_item = new Vector();
                    vfs_item.addElement(p);
                    p_parent.put("vfs_item", vfs_item);
                    for (int x = 4; x < command.length; x++) {
                      String key = command[x].split("=")[0].trim();
                      String val = command[x].substring(command[x].indexOf("=") + 1).trim();
                      p.put(Common.url_decode(key), Common.url_decode(val));
                    } 
                    String path = p.remove("path").toString();
                    p_parent.put("path", Common.all_but_last(path));
                    p_parent.put("name", p.remove("name"));
                    if (!path.endsWith("/"))
                      path = String.valueOf(path) + "/"; 
                    if (!path.startsWith("/"))
                      path = "/" + path; 
                    p.put("type", p.getProperty("type", "DIR"));
                    if (p.containsKey("privs")) {
                      permissions.put(path.toUpperCase(), p.remove("privs"));
                    } else {
                      permissions.remove(path.toUpperCase());
                    } 
                    String permissions_xml = Common.getXMLString(permissions, "privs");
                    urlc.getOutputStream().write(("&permissions=" + GenericClient.u(permissions_xml)).getBytes("UTF8"));
                    String vfs_items_xml = Common.getXMLString(vfs_items, "VFS");
                    urlc.getOutputStream().write(("&vfs_items=" + GenericClient.u(vfs_items_xml)).getBytes("UTF8"));
                    line(String.valueOf(command[1]) + " " + command[3] + " " + path + ":" + permissions.getProperty(path));
                  } else if (command[1].equalsIgnoreCase("group_add") || command[1].equalsIgnoreCase("group_delete") || command[1].equalsIgnoreCase("inheritance_add") || command[1].equalsIgnoreCase("inheritance_delete") || command[1].equalsIgnoreCase("group_delete_all") || command[1].equalsIgnoreCase("inheritance_delete_all")) {
                    String data_action = "add";
                    String xmlItem = "groups";
                    boolean all = false;
                    if (command[1].equalsIgnoreCase("group_delete")) {
                      data_action = "delete";
                    } else if (command[1].equalsIgnoreCase("inheritance_add")) {
                      xmlItem = "inheritance";
                      data_action = "add";
                    } else if (command[1].equalsIgnoreCase("inheritance_delete")) {
                      xmlItem = "inheritance";
                      data_action = "delete";
                    } else if (command[1].equalsIgnoreCase("inheritance_delete_all")) {
                      xmlItem = "inheritance";
                      data_action = "delete";
                      all = true;
                    } else if (command[1].equalsIgnoreCase("group_delete_all")) {
                      xmlItem = "groups";
                      data_action = "delete";
                      all = true;
                    } 
                    urlc.getOutputStream().write(("c2f=" + c2f + "&command=setUserItem&xmlItem=" + xmlItem + "&data_action=" + data_action + "&serverGroup=" + GenericClient.u(command[2])).getBytes("UTF8"));
                    String object_name = command[4];
                    if (xmlItem.equals("groups")) {
                      urlc.getOutputStream().write(("&group_name=" + GenericClient.u(object_name)).getBytes("UTF8"));
                    } else {
                      urlc.getOutputStream().write(("&inheritance_name=" + GenericClient.u(object_name)).getBytes("UTF8"));
                    } 
                    if (!all) {
                      urlc.getOutputStream().write(("&usernames=" + GenericClient.u(command[3])).getBytes("UTF8"));
                      line(String.valueOf(xmlItem) + " " + data_action + " " + command[3]);
                    } else {
                      urlc.getOutputStream().write("&usernames=".getBytes("UTF8"));
                      line(String.valueOf(xmlItem) + " " + data_action + " all");
                    } 
                  } 
                  int code = 302;
                  String result = "";
                  try {
                    code = urlc.getResponseCode();
                    result = Common.consumeResponse(urlc.getInputStream());
                  } catch (Exception e) {
                    Common.log("HTTP_CLIENT", 1, e);
                  } 
                  if (code != 302 && urlc.getURL().toString().indexOf("/WebInterface/login.html") >= 0)
                    code = 302; 
                  urlc.disconnect();
                  if (result.indexOf("<response>") >= 0)
                    result = result.substring(result.indexOf("<response>") + "<response>".length(), result.lastIndexOf("</response>")); 
                  line(result);
                  freeClient(c_dest);
                  last_line_prompt = "message";
                  print_prompt();
                } else if (command[0].equalsIgnoreCase("batch")) {
                  GenericClient c_dest = getClient(false);
                  Properties config = c_dest.config;
                  URLConnection urlc = URLConnection.openConnection((VRL)credentials.get("vrl"), config);
                  urlc.setRequestMethod("POST");
                  urlc.setRequestProperty("Cookie", "CrushAuth=" + config.getProperty("crushAuth", "") + ";");
                  urlc.setUseCaches(false);
                  urlc.setDoOutput(true);
                  String c2f = "";
                  if (!config.getProperty("crushAuth", "").equals(""))
                    c2f = config.getProperty("crushAuth", "").substring(config.getProperty("crushAuth", "").length() - 4); 
                  urlc.getOutputStream().write(("c2f=" + c2f + "&command=batchComplete").getBytes("UTF8"));
                  int code = 302;
                  String result = "";
                  try {
                    code = urlc.getResponseCode();
                    result = Common.consumeResponse(urlc.getInputStream());
                  } catch (Exception e) {
                    Common.log("HTTP_CLIENT", 1, e);
                  } 
                  if (code != 302 && urlc.getURL().toString().indexOf("/WebInterface/login.html") >= 0)
                    code = 302; 
                  urlc.disconnect();
                  if (result.indexOf("<response>") >= 0)
                    result = result.substring(result.indexOf("<response>") + "<response>".length(), result.lastIndexOf("</response>")); 
                  line(result);
                  freeClient(c_dest);
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
              this.this$0.printStackTrace(e, 1);
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
              this.this$0.printStackTrace(e, 1);
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
    if (!this.transfer_threads_started) {
      this.transfer_threads_started = true;
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
            this.this$0.printStackTrace(e, 1);
          } 
        }
      };
    try {
      Signal.handle(new Signal("INT"), sh);
    } catch (IllegalArgumentException illegalArgumentException) {}
  }
  
  public String stats_summary() {
    String skipped = "";
    if (!this.stats.getProperty("upload_skipped_bytes", "0").equals("0") || !this.stats.getProperty("upload_skipped_count", "0").equals("0"))
      skipped = String.valueOf(skipped) + "Skipped Uploads:" + this.stats.getProperty("upload_skipped_count", "0") + " file(s), size=" + Common.format_bytes_short(Long.parseLong(this.stats.getProperty("upload_skipped_bytes", "0"))) + "."; 
    if (!this.stats.getProperty("download_skipped_bytes", "0").equals("0") || !this.stats.getProperty("download_skipped_count", "0").equals("0"))
      skipped = String.valueOf(skipped) + "Skipped Downloads:" + this.stats.getProperty("download_skipped_count", "0") + " file(s), size=" + Common.format_bytes_short(Long.parseLong(this.stats.getProperty("download_skipped_bytes", "0"))) + "."; 
    return "Uploads:" + this.stats.getProperty("upload_count", "0") + " file(s), " + (int)Float.parseFloat(this.stats.getProperty("upload_folders", "0")) + " folder(s), size=" + Common.format_bytes_short(Long.parseLong(this.stats.getProperty("upload_bytes", "0"))) + ". Downloads:" + this.stats.getProperty("download_count", "0") + " file(s), " + this.stats.getProperty("download_folders", "0") + " folder(s), size=" + Common.format_bytes_short(Long.parseLong(this.stats.getProperty("download_bytes", "0"))) + ". " + skipped + " Queue:" + this.pending_transfer_queue.size();
  }
  
  static String last_line_prompt = "";
  
  static Object last_line_lock = new Object();
  
  public String line(String s) {
    synchronized (last_line_lock) {
      last_line_prompt = "message";
      if (this.messages != null) {
        this.messages.addElement(s);
        this.messages2.addElement(String.valueOf(log_format.format(new Date())) + "|" + s);
      } 
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
          Vector transfer_history = (Vector)c_tmp.getConfig("transfer_history");
          if (transfer_history == null) {
            transfer_history = new Vector();
            c_tmp.setConfig("transfer_history", transfer_history);
          } 
          long start = Long.parseLong(c_tmp.getConfig("transfer_start", "0").toString());
          long transfer_bytes_total = Long.parseLong(c_tmp.getConfig("transfer_bytes_total", "0").toString());
          long transfer_bytes = Long.parseLong(c_tmp.getConfig("transfer_bytes", "0").toString());
          transfer_history.addElement(String.valueOf(System.currentTimeMillis()) + ";" + transfer_bytes + ";");
          long transfer_history_start = Long.parseLong(transfer_history.elementAt(0).toString().split(";")[0]);
          while (System.currentTimeMillis() - transfer_history_start > 10000L && transfer_history.size() > 1) {
            transfer_history.removeElementAt(0);
            transfer_history_start = Long.parseLong(transfer_history.elementAt(0).toString().split(";")[0]);
          } 
          long transfer_history_bytes = Long.parseLong(transfer_history.elementAt(0).toString().split(";")[1]);
          float speed = (float)(transfer_bytes - transfer_history_bytes) / (float)(System.currentTimeMillis() - transfer_history_start) / 1000.0F;
          v.addElement(c_tmp + ";" + transfer_path_src + ";" + transfer_path_dst + ";" + ((System.currentTimeMillis() - start) / 1000L) + ";" + transfer_bytes + ";" + transfer_bytes_total + ";" + direction + ";" + (long)speed);
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
          long transfer_bytes_last = Long.parseLong(c_tmp.getConfig("transfer_bytes_last").toString());
          long transfer_bytes_last_interval = Long.parseLong(c_tmp.getConfig("transfer_bytes_last_interval").toString()) + 1L;
          if (transfer_bytes_last != transfer_bytes)
            transfer_bytes_last_interval = 0L; 
          c_tmp.setConfig("transfer_bytes_last_interval", (new StringBuffer(String.valueOf(transfer_bytes_last_interval + 1L))).toString());
          c_tmp.setConfig("transfer_bytes_last", (new StringBuffer(String.valueOf(transfer_bytes))).toString());
          float speed = 10.0F * (float)transfer_bytes / 1024.0F / (float)(System.currentTimeMillis() - start) / 1000.0F;
          speed = (int)(speed * 100.0F) / 100.0F;
          String error_msg = "";
          if (transfer_bytes_last_interval > 20L)
            speed = 0.0F; 
          if (transfer_bytes_last_interval > 40L)
            error_msg = " (Stalled) "; 
          if (transfer_bytes_last_interval > 60L)
            error_msg = " (Timing out...) "; 
          String speed_str = (speed > 10240.0F) ? (String.valueOf((int)(speed / 1024.0F) / 10.0F) + "MB/sec") : (String.valueOf((float)((int)speed / 10.0D)) + "KB/sec");
          synchronized (last_line_lock) {
            System.out.print("\r" + transfer_path_src + "->" + transfer_path_dst + error_msg + ":" + ((System.currentTimeMillis() - start) / 1000L) + " sec elapsed, " + Common.format_bytes_short(transfer_bytes) + " of " + Common.format_bytes_short(transfer_bytes_total) + " (" + (int)((float)transfer_bytes / (float)transfer_bytes_total * 100.0F) + "%) " + speed_str + "            " + (new_line ? "\n" : ""));
            if (new_line) {
              last_line_prompt = "message";
            } else {
              last_line_prompt = "stats";
            } 
          } 
        } 
      } 
      if (this.source_used.size() == 0)
        if (new_line)
          System.out.println("Used:destination_used:" + this.destination_used.size() + ",source_used:" + this.source_used.size() + ",pending_transfer_queue:" + this.pending_transfer_queue.size() + ",retry_active:" + this.retry_active.size());  
      if (new_line)
        System.out.println(stats_summary()); 
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
  
  public void add_transfer_log(String s) throws IOException {
    if (this.transfer_log != null) {
      String date_str = "";
      synchronized (log_format) {
        date_str = log_format.format(new Date());
      } 
      synchronized (this.transfer_log) {
        FileOutputStream l_out = new FileOutputStream(this.transfer_log, true);
        l_out.write((String.valueOf(date_str) + "|" + s + "\r\n").getBytes());
        l_out.close();
      } 
    } 
  }
  
  public static String printStackTrace(Exception e, int level, Vector messages2) {
    String s = "";
    if (System.getProperty("crushclient.debug", "1").equals(""))
      System.getProperties().put("crushclient.debug", "1"); 
    if (Integer.parseInt(System.getProperty("crushclient.debug", "1")) >= level) {
      s = String.valueOf(s) + Thread.currentThread().getName() + "\r\n";
      s = String.valueOf(s) + e.toString() + "\r\n";
      StackTraceElement[] ste = e.getStackTrace();
      for (int x = 0; x < ste.length; x++)
        s = String.valueOf(s) + ste[x].getClassName() + "." + ste[x].getMethodName() + ":" + ste[x].getLineNumber() + "\r\n"; 
      synchronized (log_format) {
        if (messages2 != null) {
          messages2.addElement(String.valueOf(log_format.format(new Date())) + "|" + s);
        } else {
          System.out.println(log_format.format(new Date()));
          e.printStackTrace();
        } 
      } 
    } 
    return s;
  }
  
  public String printStackTrace(Exception e, int level) {
    if (this.validate_mode)
      this.additional_errors++; 
    return printStackTrace(e, level, this.messages2);
  }
  
  public int serviceMain(String[] args) throws ServiceException {
    System.setProperty("java.awt.headless", "true");
    String[] args2 = args;
    try {
      Worker.startWorker(new Runnable(this, args2) {
            final Client this$0;
            
            private final String[] val$args2;
            
            public void run() {
              if (this.val$args2.length > 0) {
                Tunnel2.main(this.val$args2);
              } else {
                AgentUI.main(this.val$args2);
              } 
            }
          });
    } catch (Exception e) {
      e.printStackTrace();
    } 
    while (!this.shutdown) {
      try {
        Thread.sleep(1000L);
      } catch (InterruptedException interruptedException) {}
    } 
    System.exit(0);
    return 0;
  }
  
  public class DualReader {
    public ConsoleReader console;
    
    BufferedReader br2;
    
    final Client this$0;
    
    public DualReader(Client this$0, BufferedReader br2) {
      this.this$0 = this$0;
      this.console = null;
      this.br2 = null;
      this.br2 = br2;
    }
    
    private void init() {
      if (this.this$0.console_mode && this.console == null) {
        try {
          this.console = new ConsoleReader();
          this.console.setPrompt("");
        } catch (IOException e) {
          e.printStackTrace();
        } 
      } else if (this.br2 == null) {
        this.br2 = new BufferedReader(new InputStreamReader(System.in));
      } 
    }
    
    public String readLine() throws IOException {
      init();
      if (this.this$0.console_mode)
        return this.console.readLine(); 
      return this.br2.readLine();
    }
    
    public String readPassword() throws IOException {
      init();
      if (this.this$0.console_mode)
        return this.console.readLine(new Character('*')); 
      return this.br2.readLine();
    }
    
    public void close() throws IOException {
      if (this.br2 != null)
        this.br2.close(); 
      if (this.console != null)
        this.console.close(); 
    }
  }
}
