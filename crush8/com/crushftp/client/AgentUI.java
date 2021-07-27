package com.crushftp.client;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class AgentUI extends HttpCommandHandler {
  Properties prefs = new Properties();
  
  public Properties clients = new Properties();
  
  public static AgentUI thisObj = null;
  
  static String last_m = "";
  
  static SimpleDateFormat mm = new SimpleDateFormat("mm");
  
  public Properties valid_tokens = new Properties();
  
  long pong = System.currentTimeMillis();
  
  public static Vector messages2 = new Vector();
  
  public TrayIcon trayIcon = null;
  
  static Properties ui_cache = null;
  
  public static void main(String[] args) {
    System.setProperty("crushftp.worker.v8", "true");
    System.setProperty("com.crushftp.server.httphandler", "com.crushftp.client.AgentUI");
    String[] args2 = args;
    try {
      Worker.startWorker(new Runnable(args2) {
            private final String[] val$args2;
            
            public void run() {
              HTTPD.main(this.val$args2);
            }
          });
      if (args.length > 0) {
        Client.main(args);
      } else {
        Thread.sleep(1000L);
        if (System.getProperty("java.awt.headless", "false").equals("false"))
          Desktop.getDesktop().browse(new URI("http://127.0.0.1:33333/")); 
      } 
    } catch (Exception e) {
      e.printStackTrace();
      try {
        if (System.getProperty("java.awt.headless", "false").equals("false"))
          Desktop.getDesktop().browse(new URI("http://127.0.0.1:33333/")); 
      } catch (Exception exception) {}
      System.exit(1);
    } 
  }
  
  public AgentUI() {
    thisObj = this;
    (new Thread(new Runnable(this) {
          final AgentUI this$0;
          
          public void run() {
            if (System.getProperty("java.awt.headless", "false").equals("false"))
              this.this$0.setupSysTray(); 
          }
        })).start();
    String home_folder = String.valueOf(System.getProperty("user.home")) + "/" + System.getProperty("crushclient.appname", "CrushClient") + "/";
    if ((new File("./Java")).exists())
      home_folder = "./"; 
    if (System.getProperty("crushclient.prefs", "").equals(""))
      System.getProperties().put("crushclient.prefs", home_folder); 
    this.prefs.put("version", "1.0");
    try {
      if ((new File(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML")).exists())
        this.prefs = (Properties)Common.readXMLObject(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML"); 
    } catch (Exception e) {
      e.printStackTrace();
    } 
    try {
      Worker.startWorker(new Runnable(this) {
            final AgentUI this$0;
            
            public void run() {
              Thread.currentThread().setName("Agent Scheduler");
              while (true) {
                try {
                  String current_m = AgentUI.mm.format(new Date());
                  if (!AgentUI.last_m.equals(current_m)) {
                    AgentUI.last_m = current_m;
                    Thread.sleep(3000L);
                    AgentScheduler.runSchedules(AgentUI.thisObj);
                  } 
                } catch (Exception e) {
                  Client.printStackTrace(e, 1, AgentUI.messages2);
                } 
                try {
                  Thread.sleep(1000L);
                } catch (InterruptedException interruptedException) {}
                if (System.currentTimeMillis() - this.this$0.pong > 30000L && System.getProperty("crushclient.temp_agent", "false").equals("true"))
                  if (System.getProperty("java.awt.headless", "false").equals("false"))
                    System.exit(0);  
              } 
            }
          });
      Worker.startWorker(new Runnable(this) {
            final AgentUI this$0;
            
            public void run() {
              Thread.currentThread().setName("Managed Agent Worker");
              Properties config = new Properties();
              try {
                this.this$0.registerAgent(config);
              } catch (Exception e1) {
                e1.printStackTrace();
              } 
              long lastRegister = 0L;
              while (true) {
                try {
                  if (System.currentTimeMillis() - lastRegister > 60000L) {
                    this.this$0.registerAgent(config);
                    lastRegister = System.currentTimeMillis();
                  } 
                  Properties job_tmp = this.this$0.getActionItem(config);
                  if (job_tmp != null && job_tmp.size() > 0)
                    Worker.startWorker((Runnable)new Object(this, job_tmp, config)); 
                } catch (Exception e) {
                  Client.printStackTrace(e, 1, AgentUI.messages2);
                } 
                try {
                  Thread.sleep(10000L);
                } catch (InterruptedException interruptedException) {}
              } 
            }
          });
      Worker.startWorker(new Runnable(this) {
            final AgentUI this$0;
            
            public void run() {
              Thread.currentThread().setName("Logging");
              SimpleDateFormat day = new SimpleDateFormat("yyyyMMdd");
              String last_day = day.format(new Date());
              while (true) {
                RandomAccessFile raf = null;
                System.getProperties().put("crushclient.debug", this.this$0.prefs.getProperty("log_level", "1"));
                while (AgentUI.messages2.size() > 0) {
                  try {
                    if (raf == null && (this.this$0.prefs.getProperty("enable_logging", "false").equals("true") || this.this$0.prefs.getProperty("enable_logging", "false").equals("on"))) {
                      String log_file = this.this$0.prefs.getProperty("log_file", "CrushClient.log");
                      if (log_file.equals(""))
                        log_file = String.valueOf(System.getProperty("crushclient.logging", "./")) + "CrushClient.log"; 
                      if (log_file.indexOf("/") < 0 && log_file.indexOf("\\") < 0)
                        log_file = String.valueOf(System.getProperty("crushclient.logging", "./")) + log_file; 
                      raf = new RandomAccessFile(log_file, "rw");
                      raf.seek(raf.length());
                    } 
                    String s = AgentUI.messages2.remove(0).toString();
                    if (raf != null)
                      raf.write((String.valueOf(s) + "\r\n").getBytes("UTF8")); 
                  } catch (Exception e) {
                    e.printStackTrace();
                  } 
                } 
                try {
                  if (raf != null)
                    raf.close(); 
                  Thread.sleep(1000L);
                } catch (Exception exception) {}
                if (!day.format(new Date()).equals(last_day)) {
                  last_day = day.format(new Date());
                  if (this.this$0.prefs.getProperty("enable_logging", "false").equals("true")) {
                    String log_file = this.this$0.prefs.getProperty("log_file", "CrushClient.log");
                    if (log_file.equals(""))
                      log_file = String.valueOf(System.getProperty("crushclient.logging", "./")) + "CrushClient.log"; 
                    if (log_file.indexOf("/") < 0 && log_file.indexOf("\\") < 0)
                      log_file = String.valueOf(System.getProperty("crushclient.logging", "./")) + log_file; 
                    File log_f = new File(log_file);
                    try {
                      log_f.renameTo(new File(String.valueOf(log_f.getParentFile().getPath()) + "/" + last_day + "_" + log_f.getName()));
                      (new RandomAccessFile(log_file, "rw")).close();
                    } catch (Exception e) {
                      e.printStackTrace();
                    } 
                    try {
                      int days = Integer.parseInt(this.this$0.prefs.getProperty("log_history", "3"));
                      GregorianCalendar cal = new GregorianCalendar();
                      cal.setTime(new Date());
                      for (int x = days; x < 100; x++) {
                        if (x > days)
                          (new File(String.valueOf(log_f.getParentFile().getPath()) + "/" + day.format(cal.getTime()) + "_" + log_f.getName())).delete(); 
                        cal.add(5, -1 * days);
                      } 
                    } catch (NumberFormatException e) {
                      e.printStackTrace();
                    } 
                  } 
                } 
              } 
            }
          });
    } catch (IOException e) {
      Client.printStackTrace(e, 1, messages2);
    } 
  }
  
  public void registerAgent(Properties config) throws Exception {
    try {
      Vector servers = (Vector)this.prefs.get("servers");
      if (servers != null && servers.size() > 0)
        for (int x = 0; x < servers.size(); x++) {
          Properties p = servers.elementAt(x);
          if (!p.getProperty("protocol", "").equals("") && !p.getProperty("host", "").equals("") && !p.getProperty("port", "").equals("") && !p.getProperty("user", "").equals("") && !p.getProperty("pass", "").equals("")) {
            String url = String.valueOf(p.getProperty("protocol")) + "://" + p.getProperty("host") + ":" + p.getProperty("port") + "/";
            HTTPClient c = new HTTPClient(url, "AGENT:", messages2);
            c.setConfigObj(config);
            c.login(p.getProperty("user"), Common.encryptDecrypt(p.getProperty("pass"), false), "CrushClient");
            c.doAction("agentRegister", "&name=" + p.getProperty("name"), "");
          } 
        }  
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
  
  public Properties getActionItem(Properties config) throws Exception {
    Vector servers = (Vector)this.prefs.get("servers");
    if (servers != null && servers.size() > 0)
      for (int x = 0; x < servers.size(); ) {
        Properties p = servers.elementAt(x);
        if (p.getProperty("protocol", "").equals("") || p.getProperty("host", "").equals("") || p.getProperty("port", "").equals("") || p.getProperty("user", "").equals("") || p.getProperty("pass", "").equals("")) {
          x++;
          continue;
        } 
        String url = String.valueOf(p.getProperty("protocol")) + "://" + p.getProperty("host") + ":" + p.getProperty("port") + "/";
        HTTPClient c = new HTTPClient(url, "AGENT:", messages2);
        c.setConfigObj(config);
        String result = c.doAction("agentQueue", "&name=" + p.getProperty("name"), "");
        byte[] b = Base64.decode(result);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(b));
        Properties action = (Properties)ois.readObject();
        ois.close();
        return action;
      }  
    return null;
  }
  
  public void sendActionResponse(Properties config, Properties action, String response_id) throws Exception {
    Vector servers = (Vector)this.prefs.get("servers");
    if (servers != null && servers.size() > 0)
      for (int x = 0; x < servers.size(); x++) {
        Properties p = servers.elementAt(x);
        if (!p.getProperty("protocol", "").equals("") && !p.getProperty("host", "").equals("") && !p.getProperty("port", "").equals("") && !p.getProperty("user", "").equals("") && !p.getProperty("pass", "").equals("")) {
          String url = String.valueOf(p.getProperty("protocol")) + "://" + p.getProperty("host") + ":" + p.getProperty("port") + "/";
          HTTPClient c = new HTTPClient(url, "AGENT:", messages2);
          c.setConfigObj(config);
          String b64 = Base64.encodeBytes(Common.getXMLString(action, "response").getBytes("UTF8"));
          String str1 = c.doAction("agentResponse", "&name=" + p.getProperty("name") + "&response_id=" + response_id, "&response=" + Common.url_encode(b64));
        } 
      }  
  }
  
  public void menuItemSelected(String action) {
    if (action.equals("exit")) {
      System.exit(0);
    } else if (action.equals("view")) {
      try {
        Desktop.getDesktop().browse(new URI("http://127.0.0.1:33333/"));
      } catch (Exception e1) {
        e1.printStackTrace();
      } 
    } 
  }
  
  public MenuItem addMenuItem(PopupMenu popup, String label, String action) {
    MenuItem mi = new MenuItem(label);
    popup.insert(mi, 0);
    mi.addActionListener(new ActionListener(this, action) {
          final AgentUI this$0;
          
          private final String val$action;
          
          public void actionPerformed(ActionEvent e) {
            this.this$0.menuItemSelected(this.val$action);
          }
        });
    return mi;
  }
  
  public void setupSysTray() {
    String ext = "png";
    File iconFile = new File(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "icon." + ext);
    (new File(System.getProperty("crushclient.prefs", "./"))).mkdirs();
    PopupMenu popup = new PopupMenu();
    try {
      iconFile = new File(iconFile.getCanonicalPath());
      boolean gotOne = false;
      if (!iconFile.exists() && !gotOne)
        Common.streamCopier(getClass().getResourceAsStream("/com/crushftp/client/icon.png"), new FileOutputStream(iconFile), false, true, true); 
      Common.writeFileFromJar_plain("icon.png", System.getProperty("crushclient.prefs", "./"), false);
      BufferedImage image = ImageIO.read(iconFile);
      if (image != null) {
        BufferedImage resizedImage = new BufferedImage(20, 20, (image.getType() == 0) ? 2 : image.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, 20, 20, null);
        g.dispose();
        this.trayIcon = new TrayIcon(resizedImage, System.getProperty("crushclient.appname", "CrushClient"));
      } else {
        this.trayIcon = new TrayIcon(Toolkit.getDefaultToolkit().getImage(iconFile.toURI().toURL()), System.getProperty("crushclient.appname", "CrushClient"));
      } 
      this.trayIcon.setImageAutoSize(true);
      SystemTray tray = SystemTray.getSystemTray();
      this.trayIcon.setPopupMenu(popup);
      this.trayIcon.addMouseListener(new MouseListener(this) {
            final AgentUI this$0;
            
            public void mouseClicked(MouseEvent e) {
              if (e.getClickCount() == 2 && (e.getModifiers() == 18 || e.getModifiers() == 24)) {
                try {
                  Desktop.getDesktop().open(new File(System.getProperty("crushclient.prefs", "./")));
                } catch (Exception ee) {
                  ee.printStackTrace();
                  JOptionPane.showMessageDialog(null, ee);
                } 
              } else if (e.getClickCount() == 2) {
                try {
                  Desktop.getDesktop().browse(new URI("http://127.0.0.1:33333/"));
                } catch (Exception e1) {
                  e1.printStackTrace();
                } 
              } 
            }
            
            public void mouseEntered(MouseEvent e) {}
            
            public void mouseExited(MouseEvent e) {}
            
            public void mousePressed(MouseEvent e) {}
            
            public void mouseReleased(MouseEvent e) {}
          });
      tray.add(this.trayIcon);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, e);
    } 
    try {
      if (Common.machine_is_windows()) {
        addMenuItem(popup, l("Exit"), "exit");
      } else {
        addMenuItem(popup, l("Quit"), "exit");
      } 
      addMenuItem(popup, l("View..."), "view");
      MenuItem itemStatus = addMenuItem(popup, String.valueOf(System.getProperty("crushclient.appname", "CrushClient")) + " " + "1.5.1", "status");
      itemStatus.setEnabled(false);
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, e);
    } 
  }
  
  public static String l(String key) {
    String s = System.getProperties().getProperty("crushclient.localization." + key, key);
    s = Common.replace_str(s, "%appname%", System.getProperty("crushclient.appname", "CrushClient"));
    return s;
  }
  
  public void handleCommand(String path, Properties headers, Properties request, OutputStream out, InputStream in, Properties session, String ip) throws IOException {
    if (path.equals("agent")) {
      String contentType = "text/plain";
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
        ByteArrayOutputStream tmp = getRequest(headers, request, in);
        contentType = processRequest(request, baos, session, ip, tmp);
      } catch (Exception e) {
        Client.printStackTrace(e, 1, messages2);
        baos.write(e.getBytes());
      } 
      write_command_http("HTTP/1.1 200 OK", out);
      write_command_http("Date: " + this.sdf_rfc1123.format(new Date()), out);
      write_command_http("Server: CrushHTTPD", out);
      write_command_http("P3P: policyref=\"p3p.xml\", CP=\"IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT\"", out);
      write_command_http("Keep-Alive: timeout=15, max=20", out);
      write_command_http("Content-Type: " + contentType, out);
      write_command_http("Connection: Keep-Alive", out);
      write_command_http("Last-Modified: " + this.sdf_rfc1123.format(new Date()), out);
      write_command_http("ETag: " + System.currentTimeMillis(), out);
      write_command_http("Content-Length: " + baos.size(), out);
      write_command_http("", out);
      out.write(baos.toByteArray());
    } else {
      if (System.getProperty("com.crushftp.server.httphandler.path", "./clientui/").toUpperCase().startsWith("HTTP"))
        if (ui_cache == null) {
          URLConnection urlc = URLConnection.openConnection(new VRL(System.getProperties().remove("com.crushftp.server.httphandler.path") + "WebInterface/CrushClient.zip"), new Properties());
          ZipInputStream zis = new ZipInputStream(urlc.getInputStream());
          ZipEntry entry = zis.getNextEntry();
          while (entry != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
              Common.streamCopier(zis, baos, false, false, true);
            } catch (InterruptedException e) {
              Client.printStackTrace(e, 1, messages2);
            } 
            if (ui_cache == null)
              ui_cache = new Properties(); 
            ui_cache.put(entry.getName(), baos);
            entry = zis.getNextEntry();
          } 
          zis.close();
          urlc.disconnect();
        }  
      ByteArrayOutputStream f_b = new ByteArrayOutputStream();
      File file = null;
      URL f = null;
      if (ui_cache != null) {
        f = new URL("file://clientui/" + path);
        f_b = (ByteArrayOutputStream)ui_cache.get("clientui/" + path);
      } else {
        file = new File(String.valueOf(System.getProperty("com.crushftp.server.httphandler.path", "./clientui/")) + path);
        if (file.exists()) {
          f = file.toURI().toURL();
          try {
            Common.streamCopier(new FileInputStream(file), f_b, false, true, true);
          } catch (InterruptedException e) {
            Client.printStackTrace(e, 1, messages2);
          } 
        } else if (ui_cache == null) {
          path = Common.replace_str(path, "crushClient", "CrushClient");
          f = getClass().getResource("/clientui/" + path);
          if (f != null)
            try {
              Common.streamCopier(getClass().getResourceAsStream("/clientui/" + path), f_b, false, true, true);
            } catch (InterruptedException e) {
              Client.printStackTrace(e, 1, messages2);
            }  
        } 
      } 
      if (f != null) {
        write_command_http("HTTP/1.1 200 OK", out);
      } else {
        write_command_http("HTTP/1.1 404 Not found", out);
      } 
      write_command_http("Date: " + this.sdf_rfc1123.format(new Date()), out);
      write_command_http("Server: CrushHTTPD", out);
      write_command_http("P3P: policyref=\"p3p.xml\", CP=\"IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT\"", out);
      write_command_http("Keep-Alive: timeout=15, max=20", out);
      write_command_http("Content-Type: " + Common.getContentType(Common.last(f.getPath())), out);
      write_command_http("Connection: Keep-Alive", out);
      write_command_http("Last-Modified: " + this.sdf_rfc1123.format(new Date()), out);
      write_command_http("ETag: " + System.currentTimeMillis(), out);
      write_command_http("Content-Length: " + f_b.size(), out);
      write_command_http("", out);
      if (f != null)
        out.write(f_b.toByteArray()); 
    } 
    out.flush();
  }
  
  public String processRequest(Properties request, ByteArrayOutputStream baos, Properties session, String ip, ByteArrayOutputStream tmp) throws Exception {
    String contentType = "text/html";
    if (request.getProperty("command", "").equals("authenticate")) {
      if ((new File(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML")).exists())
        this.prefs = (Properties)Common.readXMLObject(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML"); 
      if (this.prefs.getProperty("agent_password", "").equals("") || Common.getMD5(new ByteArrayInputStream(VRL.vrlDecode(request.getProperty("password")).getBytes())).equals(this.prefs.getProperty("agent_password", ""))) {
        String token = Common.makeBoundary(15);
        this.valid_tokens.put(token, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        baos.write(("SUCCESS:" + token).getBytes());
      } else {
        baos.write("ERROR:Invalid password".getBytes());
      } 
    } else if (request.getProperty("command", "").equals("ping")) {
      this.pong = System.currentTimeMillis();
      baos.write(System.getProperty("crushclient.temp_agent", "false").getBytes("UTF8"));
      baos.write("\r\n".getBytes());
    } else if (!this.valid_tokens.containsKey(request.getProperty("auth_token", ""))) {
      baos.write("ERROR:NOT AUTHENTICATED\r\n".getBytes());
    } else if (request.getProperty("command", "").equals("log")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Vector log_snippet = new Vector();
      while (client != null && client.messages.size() > 0)
        log_snippet.insertElementAt(client.messages.remove(0).toString(), 0); 
      for (int x = 0; x < log_snippet.size(); x++) {
        baos.write(log_snippet.elementAt(x).toString().getBytes("UTF8"));
        baos.write("\r\n".getBytes());
      } 
    } else if (request.getProperty("command", "").equals("stats")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Vector v = client.getStats();
      while (v.size() > 0) {
        baos.write(v.remove(0).toString().getBytes("UTF8"));
        baos.write("\r\n".getBytes());
      } 
    } else if (request.getProperty("command", "").equals("queue")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Vector v = null;
      if (request.getProperty("queue_type", "").equals("success")) {
        v = (Vector)client.success_transfer_queue.clone();
      } else if (request.getProperty("queue_type", "").equals("failed")) {
        v = (Vector)client.failed_transfer_queue.clone();
      } else if (request.getProperty("queue_type", "").equals("pending")) {
        v = (Vector)client.pending_transfer_queue.clone();
      } 
      while (v.size() > 0) {
        baos.write(v.remove(0).toString().getBytes("UTF8"));
        baos.write("\r\n".getBytes());
      } 
    } else if (request.getProperty("command", "").equals("stats_summary")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Enumeration keys = client.stats.keys();
      while (keys.hasMoreElements()) {
        String key = (String)keys.nextElement();
        baos.write((String.valueOf(key) + "=" + client.stats.getProperty(key) + ";").getBytes("UTF8"));
      } 
      baos.write("\r\n".getBytes());
    } else if (request.getProperty("command", "").equals("create")) {
      Client client = getNewClient();
      client.uid = request.getProperty("client");
      this.clients.put(client.uid, client);
      if (this.clients.size() == 1)
        if (!this.prefs.getProperty("default_script", "").equals("")) {
          request.put("command_str", Common.replace_str(this.prefs.getProperty("default_script", ""), ";", "\r\n"));
          request.put("command", "process_command");
          processRequest(request, baos, session, ip, tmp);
        }  
    } else if (request.getProperty("command", "").equals("list")) {
      Enumeration keys = this.clients.keys();
      while (keys.hasMoreElements())
        baos.write((String.valueOf(keys.nextElement().toString()) + "\r\n").getBytes()); 
    } else if (request.getProperty("command", "").equals("encrypt_decrypt")) {
      String s = request.getProperty("pass");
      s = VRL.vrlDecode(s);
      s = Common.encryptDecrypt(s, request.getProperty("encrypt").equals("true"));
      if (request.getProperty("encrypt").equals("false"))
        s = VRL.vrlEncode(s); 
      baos.write(s.getBytes());
    } else if (request.getProperty("command", "").equals("destroy")) {
      Client client = (Client)this.clients.remove(request.getProperty("client"));
      client.process_command("QUIT", false);
    } else if (request.getProperty("command", "").equals("info")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Properties credentials = new Properties();
      Properties p1 = (Properties)client.source_credentials.clone();
      p1.remove("password");
      credentials.put("source", p1);
      Properties p2 = (Properties)client.destination_credentials.clone();
      p2.remove("password");
      credentials.put("destination", p2);
      baos.write(Common.getXMLString(credentials, "credentials").getBytes("UTF8"));
      contentType = "text/xml";
    } else if (request.getProperty("command", "").equals("process_command")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      client.interactive = false;
      BufferedReader br = new BufferedReader(new StringReader(request.getProperty("command_str")));
      String s = "";
      while ((s = br.readLine()) != null) {
        Object o = client.process_command(String.valueOf(VRL.vrlDecode(s)) + (request.getProperty("multithreaded", "false").equals("true") ? "&" : ""), false);
        if (o != null && (request.getProperty("command_str").toUpperCase().startsWith("LIST") || request.getProperty("command_str").toUpperCase().startsWith("LLIST") || request.getProperty("command_str").toUpperCase().startsWith("DIR") || request.getProperty("command_str").toUpperCase().startsWith("LDIR"))) {
          if (o instanceof Properties) {
            Properties p = (Properties)o;
            baos.write("{\"listing\":".getBytes("UTF8"));
            baos.write(getJsonListObj(p, false).getBytes("UTF8"));
            contentType = "application/jsonrequest;charset=utf-8";
            Enumeration keys = p.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              if (key.equals("listing"))
                continue; 
              String val = p.getProperty(key);
              if (val == null)
                continue; 
              baos.write((",\"" + key + "\":\"").getBytes("UTF8"));
              baos.write((String.valueOf(val.trim().replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99")) + "\"").getBytes("UTF8"));
            } 
            baos.write("}".getBytes("UTF8"));
            continue;
          } 
          baos.write(o.getBytes());
          continue;
        } 
        if (o != null)
          baos.write(o.getBytes()); 
      } 
    } else if (request.getProperty("command", "").equals("clear_failed") || request.getProperty("command", "").equals("clear_success") || request.getProperty("command", "").equals("clear_queue")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      if (request.getProperty("command", "").equals("clear_failed")) {
        if (request.getProperty("specific_item", "").equals("")) {
          client.failed_transfer_queue.clear();
        } else {
          client.failed_transfer_queue.removeElement(request.getProperty("specific_item", ""));
        } 
      } else if (request.getProperty("command", "").equals("clear_success")) {
        client.stats.put("download_count", "0");
        client.stats.put("upload_count", "0");
        client.stats.put("download_bytes", "0");
        client.stats.put("upload_bytes", "0");
        client.stats.put("upload_skipped_count", "0");
        client.stats.put("upload_skipped_bytes", "0");
        client.stats.put("download_skipped_count", "0");
        client.stats.put("download_skipped_bytes", "0");
        client.success_transfer_queue.clear();
      } else if (request.getProperty("command", "").equals("clear_queue")) {
        if (request.getProperty("specific_item", "").equals("")) {
          client.pending_transfer_queue.clear();
        } else {
          client.pending_transfer_queue.removeElement(request.getProperty("specific_item", "-1"));
        } 
      } 
    } else if (request.getProperty("command", "").equals("run_schedule")) {
      Client client = (Client)this.clients.get(request.getProperty("client"));
      Vector schedules = (Vector)this.prefs.get("schedules");
      for (int x = 0; x < schedules.size(); x++) {
        Properties p = schedules.elementAt(x);
        if (p.getProperty("scheduleName").equals(request.getProperty("scheduleName")))
          return runSchedule(client, p); 
      } 
    } else if (request.getProperty("command", "").equals("version")) {
      baos.write("1.5.1".getBytes("UTF8"));
      baos.write("\r\n".getBytes());
    } else if (request.getProperty("command", "").equals("set_password")) {
      String pass_hash = Common.getMD5(new ByteArrayInputStream(VRL.vrlDecode(request.getProperty("password")).getBytes()));
      if (this.prefs.getProperty("agent_password", "").equals("") || pass_hash.equals(this.prefs.getProperty("agent_password", ""))) {
        if (request.getProperty("new_password", "").equals("")) {
          this.prefs.put("agent_password", "");
        } else {
          this.prefs.put("agent_password", Common.getMD5(new ByteArrayInputStream(VRL.vrlDecode(request.getProperty("new_password")).getBytes())));
        } 
        savePrefs();
        baos.write("SUCCESS:Password updated\r\n".getBytes());
      } else {
        baos.write("ERROR:Invalid password\r\n".getBytes());
      } 
      baos.write("\r\n".getBytes());
    } else if (request.getProperty("command", "").equals("load_prefs")) {
      if ((new File(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML")).exists())
        this.prefs = (Properties)Common.readXMLObject(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML"); 
      Properties prefs2 = (Properties)this.prefs.clone();
      prefs2.remove("agent_password");
      baos.write(Common.getXMLString(prefs2, "prefs").getBytes("UTF8"));
      contentType = "text/xml";
      if (this.prefs.getProperty("temp_agent", "false").equals("true"))
        System.getProperties().put("crushclient.temp_agent", "true"); 
    } else if (request.getProperty("command", "").equals("save_prefs")) {
      String s = request.getProperty("prefs").replace('+', ' ');
      s = Common.replace_str(s, "%26", "&amp;");
      s = Common.replace_str(s, "%3C", "&lt;");
      s = Common.replace_str(s, "%3E", "&gt;");
      Properties prefs2 = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(s).getBytes("UTF8")));
      if (this.prefs == null)
        this.prefs = new Properties(); 
      prefs2.remove("agent_password");
      this.prefs.putAll(prefs2);
      savePrefs();
    } 
    return contentType;
  }
  
  public Client getNewClient() {
    Client client = new Client(new Vector(), messages2);
    client.dual_log = false;
    client.interactive = false;
    return client;
  }
  
  public synchronized void savePrefs() throws Exception {
    (new File(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML.new")).delete();
    Common.writeXMLObject(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML.new", this.prefs, "prefs");
    (new File(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML")).delete();
    (new File(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML.new")).renameTo(new File(String.valueOf(System.getProperty("crushclient.prefs", "./")) + "prefs.XML"));
  }
  
  public String runSchedule(Client client, Properties schedule) {
    try {
      String script = schedule.getProperty("script", "").trim();
      if ((new File(script)).exists()) {
        ByteArrayOutputStream baos_script = new ByteArrayOutputStream();
        Common.streamCopier(new FileInputStream(script), baos_script, false, true, true);
        script = (new String(baos_script.toByteArray(), "UTF8")).trim();
      } 
      script = String.valueOf(script) + "\r\nquit\r\n";
      String connect_prefix = "";
      int pos = script.indexOf("{bookmark:");
      int pos2 = script.indexOf("connect {bookmark:");
      while (pos >= 0) {
        String inner = script.substring(pos, script.indexOf("}", pos) + 1);
        Vector bookmarks = (Vector)this.prefs.get("bookmarks");
        String result = null;
        for (int x = 0; bookmarks != null && x < bookmarks.size(); x++) {
          Properties book = bookmarks.elementAt(x);
          if (book.getProperty("name", "").equalsIgnoreCase(inner.split(":")[1])) {
            if (!book.getProperty("pbe_pass", "").equals(""))
              script = "pbe " + book.getProperty("pbe_pass") + "\r\n" + script; 
            String val = "";
            String key = inner.split(":")[2];
            key = key.substring(0, key.length() - 1);
            if (key.equalsIgnoreCase("pass")) {
              val = VRL.vrlEncode(Common.encryptDecrypt(book.getProperty("pass"), false));
            } else if (key.equalsIgnoreCase("url")) {
              if (book.getProperty("protocol").startsWith("file:")) {
                val = String.valueOf(book.getProperty("protocol")) + book.getProperty("defaultPath");
              } else {
                if (!book.getProperty("maxThreads", "").equals(""))
                  connect_prefix = String.valueOf(connect_prefix) + "set max_threads " + book.getProperty("maxThreads") + "\r\n"; 
                if (!book.getProperty("secure_data", "").equals(""))
                  connect_prefix = String.valueOf(connect_prefix) + "config secure_data " + book.getProperty("secure_data") + "\r\n"; 
                if (!book.getProperty("connect", "Default").equalsIgnoreCase("Default"))
                  connect_prefix = String.valueOf(connect_prefix) + "config pasv " + book.getProperty("connect").equalsIgnoreCase("PASV") + "\r\n"; 
                if (!book.getProperty("ssh_private_key", "").equalsIgnoreCase(""))
                  connect_prefix = String.valueOf(connect_prefix) + "config ssh_private_key " + book.getProperty("ssh_private_key") + "\r\n"; 
                if (!book.getProperty("ssh_private_key_pass", "").equalsIgnoreCase(""))
                  connect_prefix = String.valueOf(connect_prefix) + "config ssh_private_key_pass " + book.getProperty("ssh_private_key_pass") + "\r\n"; 
                if (!book.getProperty("ssh_two_factor", "").equalsIgnoreCase(""))
                  connect_prefix = String.valueOf(connect_prefix) + "config ssh_two_factor " + book.getProperty("ssh_two_factor") + "\r\n"; 
                if (!book.getProperty("verifyHost", "").equalsIgnoreCase(""))
                  connect_prefix = String.valueOf(connect_prefix) + "config verifyHost " + book.getProperty("verifyHost") + "\r\n"; 
                if (!book.getProperty("addNewHost", "").equalsIgnoreCase(""))
                  connect_prefix = String.valueOf(connect_prefix) + "config addNewHost " + book.getProperty("addNewHost") + "\r\n"; 
                if (!book.getProperty("knownHostFile", "").equalsIgnoreCase(""))
                  connect_prefix = String.valueOf(connect_prefix) + "config knownHostFile " + book.getProperty("knownHostFile") + "\r\n"; 
                val = String.valueOf(book.getProperty("protocol")) + book.getProperty("user") + ":" + VRL.vrlEncode(Common.encryptDecrypt(book.getProperty("pass"), false)) + "@" + book.getProperty("host") + ":" + book.getProperty("port") + book.getProperty("defaultPath");
              } 
              if (!val.endsWith("/"))
                val = String.valueOf(val) + "/"; 
            } else {
              val = book.getProperty(key);
            } 
            result = val;
            break;
          } 
        } 
        if (result == null)
          return "Bookmark not found:" + inner; 
        if (script.indexOf("connect {bookmark:", pos2) >= 0)
          script = String.valueOf(script.substring(0, script.indexOf("connect {bookmark:", pos2))) + connect_prefix + script.substring(script.indexOf("connect {bookmark:", pos2)); 
        script = Common.replace_str(script, inner, result);
        pos = script.indexOf("{bookmark:");
        if (script.indexOf("lconnect {bookmark:") >= 0)
          pos2 = script.length(); 
      } 
      client.console_mode = false;
      client.getClass();
      client.br = new Client.DualReader(client, new BufferedReader(new InputStreamReader(new ByteArrayInputStream(script.getBytes("UTF8")))));
      client.local_echo = true;
      (new Thread(new Runnable(this, client, schedule) {
            final AgentUI this$0;
            
            private final Client val$client;
            
            private final Properties val$schedule;
            
            public void run() {
              long pending_sync_last_run = System.currentTimeMillis();
              this.val$client.prefs.put("pending_sync_last_run", (new StringBuffer(String.valueOf(pending_sync_last_run))).toString());
              this.val$client.prefs.put("sync_last_run", this.val$schedule.getProperty("sync_last_run", "0"));
              this.val$client.run();
              this.val$schedule.put("sync_last_run", (new StringBuffer(String.valueOf(pending_sync_last_run))).toString());
              try {
                this.this$0.savePrefs();
              } catch (Exception e) {
                Client.printStackTrace(e, 1, AgentUI.messages2);
              } 
              try {
                this.val$client.process_command("QUIT", false);
                Thread.sleep(60000L);
              } catch (Exception exception) {}
              this.this$0.clients.remove(this.val$client.uid);
            }
          })).start();
      return String.valueOf(schedule.getProperty("scheduleName")) + " started.";
    } catch (Exception e) {
      Client.printStackTrace(e, 1, messages2);
      return (String)e;
    } 
  }
  
  public static String getJsonList(Properties listingProp, boolean exif_listings, boolean simple) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    sb.append("l = new Array();\r\n");
    String s = "";
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      String eol = "\r\n";
      sb.append("lp = {};\r\n");
      s = "name";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "dir";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol); 
      s = "type";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "root_dir";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "href_path";
      sb.append("lp." + s + "=\"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "privs";
      sb.append("lp." + s + "=\"" + lp.getProperty(s).replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol);
      s = "size";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "modified";
      sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
      s = "owner";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol); 
      s = "group";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D") + "\";" + eol); 
      s = "permissionsNum";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol); 
      s = "keywords";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s, "").trim().replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\";" + eol); 
      s = "permissions";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol); 
      s = "num_items";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol); 
      s = "boot";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s, "false") + "\";" + eol); 
      s = "preview";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol); 
      s = "dateFormatted";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol); 
      s = "sizeFormatted";
      if (!simple)
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol); 
      if (exif_listings && !simple) {
        s = "width";
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
        s = "height";
        sb.append("lp." + s + "=\"" + lp.getProperty(s) + "\";" + eol);
        Enumeration keys = lp.keys();
        while (keys.hasMoreElements()) {
          s = (String)keys.nextElement();
          if (s.startsWith("crushftp_"))
            sb.append("lp." + s + "=\"" + lp.getProperty(s, "").trim().replaceAll("%", "%25").replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\";" + eol); 
        } 
      } 
      sb.append("l[l.length] = lp;" + eol);
    } 
    return "\r\n<listing>" + sb.toString() + "</listing>";
  }
  
  public static String getJsonListObj(Properties listingProp, boolean exif_listings) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    sb.append("[");
    String s = "";
    String eol = "\r\n";
    String parent_privs = listingProp.getProperty("privs", "NONE");
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      if (x > 0) {
        sb.append(",{").append(eol);
      } else {
        sb.append("{").append(eol);
      } 
      s = "name";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "dir";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "type";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "root_dir";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "href_path";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("%", "%25").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "privs";
      if (!lp.getProperty(s, "").equals(parent_privs))
        sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol); 
      s = "size";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "modified";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "owner";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\"," + eol);
      s = "group";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\"," + eol);
      s = "permissionsNum";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "keywords";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").trim().replaceAll("%", "%25").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\"," + eol);
      s = "permissions";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "num_items";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      s = "preview";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
      if (exif_listings) {
        s = "width";
        sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
        s = "height";
        sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "") + "\"," + eol);
        Enumeration keys = lp.keys();
        while (keys.hasMoreElements()) {
          s = (String)keys.nextElement();
          if (s.startsWith("crushftp_"))
            sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").trim().replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A").replaceAll("\\\\", "%5C").replaceAll(":", "%3A").replaceAll("’", "%E2%80%99") + "\"," + eol); 
        } 
      } 
      s = "dateFormatted";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"," + eol);
      s = "sizeFormatted";
      sb.append("\"" + s + "\" : \"" + lp.getProperty(s, "").replaceAll("\"", "%22").replaceAll("\t", "%09").replaceAll("\\r", "%0D").replaceAll("\\n", "%0A") + "\"" + eol);
      sb.append("}").append(eol);
    } 
    sb.append("]");
    return sb.toString();
  }
  
  public static String getStatList(Properties listingProp) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      sb.append(String.valueOf(lp.getProperty("permissions")) + " " + lp.getProperty("num_items") + " " + lp.getProperty("owner") + " " + lp.getProperty("group") + " " + lp.getProperty("size") + " " + yyyyMMddHHmmss.format(new Date(Long.parseLong(lp.getProperty("modified")))) + " " + lp.getProperty("day") + " " + lp.getProperty("time_or_year") + " " + (String.valueOf(lp.getProperty("root_dir")) + lp.getProperty("name")).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\\\\", "%5C") + "\r\n");
    } 
    return "\r\n<listing>" + sb.toString() + "</listing>";
  }
  
  public static String getDmzList(Properties listingProp) {
    Vector listing = (Vector)listingProp.remove("listing");
    StringBuffer sb = new StringBuffer();
    for (int x = 0; x < listing.size(); x++) {
      Properties lp = listing.elementAt(x);
      sb.append(formatDmzStat(lp)).append("\r\n");
    } 
    return "\r\n<listing>" + sb.toString() + "</listing>";
  }
  
  public static String formatDmzStat(Properties lp) {
    Enumeration keys = lp.keys();
    String s = "";
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      String val = (new StringBuffer(String.valueOf(lp.getProperty(key)))).toString();
      s = String.valueOf(s) + key + "=" + Common.url_encode(val) + ";";
    } 
    return s;
  }
  
  public static void writeFileFromJar(String src, String dst, File jar) {
    try {
      ZipFile zf = new ZipFile(jar);
      ZipEntry ze = zf.getEntry("assets/crushftp/" + src);
      InputStream in = zf.getInputStream(ze);
      (new File(Common.all_but_last(String.valueOf(dst) + src))).mkdirs();
      RandomAccessFile out = new RandomAccessFile(String.valueOf(dst) + src, "rw");
      int bytes = 0;
      byte[] b = new byte[32768];
      while (bytes >= 0) {
        bytes = in.read(b);
        if (bytes > 0)
          out.write(b, 0, bytes); 
      } 
      in.close();
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
}
