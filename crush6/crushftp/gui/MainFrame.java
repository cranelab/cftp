package crushftp.gui;

import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.awt.LayoutManager;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class MainFrame extends JFrame {
  private static final long serialVersionUID = 1L;
  
  static ServerStatus st = null;
  
  public JFrame thisObj = null;
  
  private JPanel contentPane;
  
  JButton btnAuthenticateCrushftpFor = new JButton("Authenticate for OS X");
  
  JButton btnStartTemporaryServer = new JButton("Start Temporary Server");
  
  JButton btnInstallDaemon = new JButton("Install Daemon / Service");
  
  JButton btnRemoveDaemon = new JButton("Remove Daemon / Service");
  
  JLabel auth_label = new JLabel("<html><body>Before we can accept connections on ports below 1024 (such as 21,80,443) and before you can install the daemon, you must authenticate this on OS X.  This will give it the permissions needed to listen on priviledged ports and install/remove the daemon.</body></html>");
  
  JLabel lblthisWillInstall = new JLabel("<html><body>This will install the daemon on OS X, or Windows service.  This allows CrushFTP to be running without the need for a user to be logged in on the OS.</body></html>");
  
  JLabel lblthisWillRemove = new JLabel("<html><body>This will remove the daemon on OS X, or Windows service.  Only use this when you want to stop and uninstall the server.</body></html>");
  
  private final JButton btnCreateNewAdmin = new JButton("Create New Admin User");
  
  private final JLabel lblthisWillBuild = new JLabel("<html><body>This will build a new administration user so that you can configure the server from your web browser.  Do not use the username 'admin', or 'administrator', or 'root'.  Suggested usernames would be your OS username, 'remoteadmin' or 'crushadmin', etc.</body></html>");
  
  JButton btnStartDaemon = new JButton("Start Daemon / Service");
  
  JButton btnStopDaemon = new JButton("Stop Daemon / Service");
  
  public MainFrame() {
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    System.setProperty("com.apple.mrj.application.apple.menu.about.name", "CrushFTP");
    Common.initSystemProperties(true);
    this.thisObj = this;
    String msg = "";
    if (!Common.haveWriteAccess())
      msg = String.valueOf(msg) + LOC.G("It appears you are running this from a locked disk.") + "\r\n" + LOC.G("Please copy the application folder to a location where it has full access to its own folder.  (Or run as an administrator.)") + "\r\n\r\n"; 
    if (msg.length() > 0)
      JOptionPane.showMessageDialog(this, msg, LOC.G("Alert"), 0); 
    MenuBar mbar = new MenuBar();
    if (!Common.machine_is_x()) {
      Menu menu = new Menu("File");
      MenuItem item = new MenuItem("Quit");
      item.setShortcut(new MenuShortcut(81, false));
      item.addActionListener(new ActionListener(this) {
            final MainFrame this$0;
            
            public void actionPerformed(ActionEvent e) {
              this.this$0.quit();
            }
          });
      menu.add(item);
      mbar.add(menu);
    } 
    setMenuBar(mbar);
    setResizable(false);
    setTitle("CrushFTP " + ServerStatus.version_info_str + ServerStatus.sub_version_info_str);
    addWindowListener(new WindowAdapter(this) {
          final MainFrame this$0;
          
          public void windowClosing(WindowEvent e) {
            this.this$0.quit();
          }
        });
    setBounds(100, 100, 645, 584);
    this.contentPane = new JPanel();
    this.contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    setContentPane(this.contentPane);
    this.contentPane.setLayout((LayoutManager)null);
    this.btnAuthenticateCrushftpFor.setToolTipText("Grants CrushFTP permissions so that it can install a daemon, and use reserved ports.");
    this.btnAuthenticateCrushftpFor.addActionListener(new ActionListener(this) {
          final MainFrame this$0;
          
          public void actionPerformed(ActionEvent arg0) {
            try {
              Common.OSXPermissionsGrant();
              Thread.sleep(1000L);
              Runtime.getRuntime().exec(String.valueOf(System.getProperty("crushftp.home")) + "../../MacOS/CrushFTP");
            } catch (Exception ee) {
              Log.log("SERVER", 0, ee);
              JOptionPane.showMessageDialog(null, ee.getMessage());
            } 
            this.this$0.quit();
          }
        });
    this.btnAuthenticateCrushftpFor.setBounds(182, 146, 282, 29);
    this.contentPane.add(this.btnAuthenticateCrushftpFor);
    this.auth_label.setBounds(5, 175, 635, 63);
    this.contentPane.add(this.auth_label);
    this.btnInstallDaemon.setToolTipText("Install the daemon/service allowing the server to start when the machine boots without user interaction.");
    this.btnInstallDaemon.addActionListener(new ActionListener(this) {
          final MainFrame this$0;
          
          public void actionPerformed(ActionEvent arg0) {
            if (ServerStatus.SG("max_max_users").equals("5")) {
              JOptionPane.showMessageDialog(this.this$0.thisObj, "Sorry, this feature is only available to registered users.", "Alert", 0);
            } else {
              if (System.getProperty("java.version").startsWith("1.4")) {
                JOptionPane.showMessageDialog(this.this$0.thisObj, "Sorry, Java 1.4 is not support for installing a service.\r\n\r\nPlease update to java version 1.6 or higher.", "Alert", 0);
                return;
              } 
              Log.log("SERVER", 0, "Preparing to install service...");
              ServerStatus.thisObj.stop_all_servers();
              ServerStatus.thisObj.statTools.stopDB();
              if (Common.machine_is_windows()) {
                Log.log("SERVER", 0, "Unzipping required service files for Windows.");
                if (!ServerStatus.thisObj.common_code.install_windows_service()) {
                  JOptionPane.showMessageDialog(this.this$0.thisObj, "Access Denied: You must right click and run CrushFTP as an Administrator.", "Service Install Failed", 0);
                  this.this$0.quit();
                  return;
                } 
              } else if (Common.machine_is_x()) {
                ServerStatus.thisObj.common_code.install_osx_service();
              } 
              Log.log("SERVER", 0, "Finished.  Log file has messages if there were any errors. (Administrator user is required.");
              JOptionPane.showMessageDialog(null, "Service Installed");
              this.this$0.quit();
            } 
          }
        });
    this.lblthisWillInstall.setBounds(5, 305, 635, 63);
    this.contentPane.add(this.lblthisWillInstall);
    this.btnInstallDaemon.setBounds(27, 278, 282, 29);
    this.contentPane.add(this.btnInstallDaemon);
    this.btnStartTemporaryServer.addActionListener(new ActionListener(this) {
          final MainFrame this$0;
          
          public void actionPerformed(ActionEvent arg0) {
            this.this$0.startTempServer();
          }
        });
    this.btnStartTemporaryServer.setBounds(337, 278, 282, 29);
    this.contentPane.add(this.btnStartTemporaryServer);
    this.btnRemoveDaemon.setToolTipText("Remove the daemon/service which allowed the server to start when the machine booted without user interaction.");
    this.btnRemoveDaemon.addActionListener(new ActionListener(this) {
          final MainFrame this$0;
          
          public void actionPerformed(ActionEvent arg0) {
            if (Common.machine_is_windows()) {
              if (!Common.remove_windows_service()) {
                JOptionPane.showMessageDialog(this.this$0.thisObj, "Access Denied: You must right click and run CrushFTP as an Administrator.", "Service Remove Failed", 0);
              } else {
                JOptionPane.showMessageDialog(this.this$0.thisObj, "Service removed.", "Alert", 1);
              } 
            } else if (Common.machine_is_x()) {
              Common.remove_osx_service();
              JOptionPane.showMessageDialog(null, "Daemon removed.");
            } 
          }
        });
    this.lblthisWillRemove.setBounds(5, 441, 635, 63);
    this.contentPane.add(this.lblthisWillRemove);
    this.btnRemoveDaemon.setBounds(182, 414, 282, 29);
    this.contentPane.add(this.btnRemoveDaemon);
    this.btnCreateNewAdmin.setToolTipText("Create an admin user that can do administration from the web browser.");
    this.btnCreateNewAdmin.addActionListener(new ActionListener(this) {
          final MainFrame this$0;
          
          public void actionPerformed(ActionEvent arg0) {
            Vector pref_server_items = (Vector)ServerStatus.server_settings.get("server_list");
            for (int x = 0; x < pref_server_items.size(); x++) {
              Properties p = pref_server_items.elementAt(x);
              if (!p.getProperty("linkedServer", "").equals("") && ServerStatus.VG("server_groups").indexOf(p.getProperty("linkedServer", "")) < 0)
                ServerStatus.VG("server_groups").addElement(p.getProperty("linkedServer", "")); 
            } 
            Object[] possibleValues = ServerStatus.VG("server_groups").toArray();
            String serverGroup = null;
            if (possibleValues.length == 1) {
              serverGroup = (String)possibleValues[0];
            } else {
              serverGroup = (String)JOptionPane.showInputDialog(this.this$0.thisObj, "Pick a user connection group:", "Pick A User Connection Group", 1, null, possibleValues, null);
            } 
            if (serverGroup != null) {
              String username = (String)JOptionPane.showInputDialog(this.this$0.thisObj, "Username:", "Username", 0, null, null, "crushadmin");
              if (username != null) {
                String password = Common.getPasswordPrompt("Password:");
                ServerStatus.thisObj.common_code.writeAdminUser(username, password, serverGroup, false);
                JOptionPane.showMessageDialog(null, "Admin User Created.");
              } 
            } 
          }
        });
    this.lblthisWillBuild.setBounds(5, 33, 635, 63);
    this.contentPane.add(this.lblthisWillBuild);
    this.btnCreateNewAdmin.setBounds(182, 6, 282, 29);
    this.contentPane.add(this.btnCreateNewAdmin);
    this.btnStopDaemon.addActionListener(new ActionListener(this) {
          final MainFrame this$0;
          
          public void actionPerformed(ActionEvent arg0) {
            String results = "";
            File f = new File("crushftp_exec_root.sh");
            try {
              Process proc = null;
              if (Common.machine_is_windows()) {
                proc = Runtime.getRuntime().exec("net stop \"CrushFTP Server\"");
              } else if (Common.machine_is_x()) {
                RandomAccessFile out = new RandomAccessFile("crushftp_exec_root.sh", "rw");
                out.setLength(0L);
                out.write("launchctl stop com.crushftp.CrushFTP\n".getBytes("UTF8"));
                out.close();
                Common.exec(new String[] { "chmod", "+x", f.getCanonicalPath() });
                proc = Runtime.getRuntime().exec(new String[] { "osascript", "-e", "do shell script \"" + f.getCanonicalPath() + "\" with administrator privileges" });
              } 
              BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
              String data = "";
              while ((data = proc_in.readLine()) != null) {
                results = String.valueOf(results) + data + "\r\n";
                Log.log("SERVER", 0, data);
              } 
              proc_in.close();
              proc_in = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
              data = "";
              while ((data = proc_in.readLine()) != null) {
                results = String.valueOf(results) + data + "\r\n";
                Log.log("SERVER", 0, data);
              } 
              JOptionPane.showMessageDialog(null, "Stopped\r\n\r\n" + results);
            } catch (Exception e) {
              e.printStackTrace();
              JOptionPane.showMessageDialog(null, String.valueOf(e.toString()) + "\r\n\r\n" + results);
            } 
            f.delete();
          }
        });
    this.btnStopDaemon.setToolTipText("If installed, stop the daemon/service.");
    this.btnStopDaemon.setBounds(25, 502, 282, 29);
    this.contentPane.add(this.btnStopDaemon);
    this.btnStartDaemon.addActionListener(new ActionListener(this) {
          final MainFrame this$0;
          
          public void actionPerformed(ActionEvent arg0) {
            String results = "";
            File f = new File("crushftp_exec_root.sh");
            try {
              Process proc = null;
              if (Common.machine_is_windows()) {
                proc = Runtime.getRuntime().exec("net start \"CrushFTP Server\"");
              } else if (Common.machine_is_x()) {
                RandomAccessFile out = new RandomAccessFile("crushftp_exec_root.sh", "rw");
                out.setLength(0L);
                out.write("launchctl start com.crushftp.CrushFTP\n".getBytes("UTF8"));
                out.close();
                Common.exec(new String[] { "chmod", "+x", f.getCanonicalPath() });
                proc = Runtime.getRuntime().exec(new String[] { "osascript", "-e", "do shell script \"" + f.getCanonicalPath() + "\" with administrator privileges" });
              } 
              BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
              String data = "";
              while ((data = proc_in.readLine()) != null) {
                results = String.valueOf(results) + data + "\r\n";
                Log.log("SERVER", 0, data);
              } 
              proc_in.close();
              proc_in = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
              data = "";
              while ((data = proc_in.readLine()) != null) {
                results = String.valueOf(results) + data + "\r\n";
                Log.log("SERVER", 0, data);
              } 
              JOptionPane.showMessageDialog(null, "Started\r\n\r\n" + results);
            } catch (Exception e) {
              e.printStackTrace();
              JOptionPane.showMessageDialog(null, String.valueOf(e.toString()) + "\r\n\r\n" + results);
            } 
            f.delete();
          }
        });
    this.btnStartDaemon.setToolTipText("If installed, start the daemon/service.");
    this.btnStartDaemon.setBounds(337, 502, 282, 29);
    this.contentPane.add(this.btnStartDaemon);
    checkAuthed();
    setVisible(true);
    (new Thread(new Runnable(this) {
          final MainFrame this$0;
          
          public void run() {
            if (Common.log == null)
              Common.log = new Vector(); 
            MainFrame.st = new ServerStatus(false, null);
          }
        })).start();
    if (!ServerStatus.killUpdateFiles())
      JOptionPane.showMessageDialog(this, LOC.G("Update not complete!  Please close " + LOC.G("CrushFTP") + " and run 'update.bat' first!")); 
    if (System.getProperties().getProperty("crushftp.autostart", "false").equals("true"))
      startTempServer(); 
  }
  
  public void checkAuthed() {
    if (Common.machine_is_x_10_6_plus() && !System.getProperty("java.version").startsWith("1.5") && !System.getProperty("java.version").startsWith("1.6")) {
      this.btnAuthenticateCrushftpFor.setVisible(false);
      this.auth_label.setVisible(false);
      return;
    } 
    if (Common.OSXApp())
      this.btnAuthenticateCrushftpFor.setEnabled(true); 
    int openedPorts = 0;
    for (int x = 900; x < 1000; x++) {
      try {
        ServerSocket s = new ServerSocket(x);
        s.close();
        openedPorts++;
        break;
      } catch (BindException e) {
        if (e.toString().indexOf("denied") >= 0)
          break; 
      } catch (Exception exception) {}
    } 
    if (openedPorts > 0) {
      this.auth_label.setVisible(false);
      this.btnAuthenticateCrushftpFor.setVisible(false);
      this.btnAuthenticateCrushftpFor.setEnabled(false);
    } 
    if (!Common.OSXApp())
      this.btnAuthenticateCrushftpFor.setEnabled(false); 
    if (Common.machine_is_x()) {
      this.btnInstallDaemon.setEnabled((!this.btnAuthenticateCrushftpFor.isEnabled() && Common.OSXApp()));
      this.btnRemoveDaemon.setEnabled((!this.btnAuthenticateCrushftpFor.isEnabled() && Common.OSXApp()));
      this.btnStartDaemon.setEnabled((!this.btnAuthenticateCrushftpFor.isEnabled() && Common.OSXApp()));
      this.btnStopDaemon.setEnabled((!this.btnAuthenticateCrushftpFor.isEnabled() && Common.OSXApp()));
      if (!Common.OSXApp()) {
        this.btnAuthenticateCrushftpFor.setToolTipText("You must be running as an application for these buttons to be enabled.");
        this.btnInstallDaemon.setToolTipText(this.btnAuthenticateCrushftpFor.getToolTipText());
        this.btnRemoveDaemon.setToolTipText(this.btnAuthenticateCrushftpFor.getToolTipText());
      } 
    } 
  }
  
  public void quit() {
    (new Thread(new Runnable(this) {
          final MainFrame this$0;
          
          public void run() {
            ServerStatus.thisObj.quit_server();
            System.exit(0);
          }
        })).start();
    try {
      Thread.sleep(30000L);
      System.exit(1);
    } catch (Exception exception) {}
  }
  
  public void startTempServer() {
    try {
      while (st == null)
        Thread.sleep(100L); 
      st.init_setup(true);
      this.btnStartTemporaryServer.setEnabled(false);
      this.btnStartTemporaryServer.setToolTipText("Server is running, closing this window will quit the server.");
      String validURLs = "";
      Vector servers = ServerStatus.VG("server_list");
      for (int x = 0; x < servers.size(); x++) {
        Properties server = servers.elementAt(x);
        if (!server.getProperty("port").startsWith("555")) {
          String ip = Common.getLocalIP();
          if (server.getProperty("serverType", "").equals("HTTP")) {
            if (server.getProperty("ip").equals("lookup")) {
              validURLs = String.valueOf(validURLs) + "http://127.0.0.1" + (server.getProperty("port").equals("80") ? "" : (":" + server.getProperty("port"))) + "/\r\n";
              validURLs = String.valueOf(validURLs) + "http://" + ip + (server.getProperty("port").equals("80") ? "" : (":" + server.getProperty("port"))) + "/\r\n";
            } else {
              validURLs = String.valueOf(validURLs) + "http://" + server.getProperty("ip") + (server.getProperty("port").equals("80") ? "" : (":" + server.getProperty("port"))) + "/\r\n";
            } 
          } else if (server.getProperty("serverType", "").equals("HTTPS")) {
            if (server.getProperty("ip").equals("lookup")) {
              validURLs = String.valueOf(validURLs) + "https://127.0.0.1" + (server.getProperty("port").equals("443") ? "" : (":" + server.getProperty("port"))) + "/\r\n";
              validURLs = String.valueOf(validURLs) + "https://" + ip + (server.getProperty("port").equals("443") ? "" : (":" + server.getProperty("port"))) + "/\r\n";
            } else {
              validURLs = String.valueOf(validURLs) + "https://" + server.getProperty("ip") + (server.getProperty("port").equals("443") ? "" : (":" + server.getProperty("port"))) + "/\r\n";
            } 
          } 
        } 
      } 
      if (validURLs.equals("")) {
        Properties server_item = new Properties();
        server_item.put("linkedServer", ServerStatus.VG("server_groups").elementAt(0));
        server_item.put("serverType", "HTTP");
        server_item.put("ip", "lookup");
        server_item.put("port", "9090");
        server_item.put("require_encryption", "false");
        server_item.put("https_redirect", "false");
        server_item.put("explicit_ssl", "false");
        server_item.put("explicit_tls", "false");
        server_item.put("explicit_tls", "false");
        server_item.put("require_secure", "false");
        server_item.put("http", "true");
        server_item.put("server_ip", "auto");
        server_item.put("pasv_ports", "1025-65535");
        server_item.put("ftp_aware_router", "true");
        servers.addElement(server_item);
        st.save_server_settings(true);
        startTempServer();
        return;
      } 
      this.btnStartTemporaryServer.setToolTipText("<html><body>Server is running, closing this window will quit the server.<br/><pre>" + validURLs + "</pre></body></html>");
      if (System.getProperties().getProperty("crushftp.autostart", "false").equals("false"))
        JOptionPane.showMessageDialog(null, "Servers Started, use a web browser now and go to one of these URLs:\r\n" + validURLs); 
      if (ServerStatus.thisObj != null)
        ServerStatus.thisObj.checkCrushExpiration(); 
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Error" + e.getMessage());
    } 
  }
}
