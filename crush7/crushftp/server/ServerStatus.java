package crushftp.server;

import com.crushftp.client.CommandBufferFlusher;
import com.crushftp.client.Common;
import com.crushftp.client.FileClient;
import com.crushftp.client.File_B;
import com.crushftp.client.GenericClientMulti;
import com.crushftp.client.MemoryClient;
import com.crushftp.client.Worker;
import com.crushftp.tunnel2.Tunnel2;
import com.crushftp.tunnel3.StreamController;
import crushftp.db.SearchHandler;
import crushftp.db.SearchTools;
import crushftp.db.StatTools;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.JobScheduler;
import crushftp.handlers.Log;
import crushftp.handlers.LoggingProvider;
import crushftp.handlers.PreferencesProvider;
import crushftp.handlers.PreviewWorker;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.SharedSession;
import crushftp.handlers.SharedSessionReplicated;
import crushftp.handlers.ShutdownHandler;
import crushftp.handlers.SyncTools;
import crushftp.handlers.UpdateHandler;
import crushftp.handlers.UpdateTimer;
import crushftp.handlers.UserTools;
import crushftp.handlers.log.LoggingProviderDisk;
import crushftp.license.Maverick;
import crushftp.reports5.ReportTools;
import crushftp.server.daemon.GenericServer;
import crushftp.server.daemon.ServerBeat;
import crushftp.server.ssh.SSHDaemon;
import crushftp.user.SQLUsers;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import javax.crypto.Cipher;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;

public class ServerStatus {
  public static String sub_version_info_str = "_3";
  
  public static String version_info_str = "Version 7.8.0";
  
  public static ClassLoader clasLoader = null;
  
  public static ServerStatus thisObj = null;
  
  public static Properties server_settings = new Properties();
  
  public Properties server_info = new Properties();
  
  public Date server_start_time = new Date();
  
  public Vector commandBuffer = new Vector();
  
  public StatTools statTools = new StatTools();
  
  public SearchTools searchTools = new SearchTools();
  
  public Events events6 = new Events();
  
  CommandBufferFlusher commandBufferFlusher = null;
  
  UpdateHandler updateHandler = new UpdateHandler();
  
  ReportTools rt = new ReportTools();
  
  public long total_server_bytes_sent = 0L;
  
  public long total_server_bytes_received = 0L;
  
  public long server_last_bytes_sent = 0L;
  
  public long server_last_bytes_received = 0L;
  
  public Thread update_timer_thread = null;
  
  public Thread report_scheduler_thread = null;
  
  public Thread scheduler_thread = null;
  
  public Thread alerts_thread = null;
  
  public Thread new_version_thread = null;
  
  public Thread stats_saver_thread = null;
  
  public Object stats_saver_lock = new Object();
  
  public Thread hammer_timer_thread = null;
  
  public Thread hammer_timer_http_thread = null;
  
  public Thread ban_timer_thread = null;
  
  public Thread phammer_timer_thread = null;
  
  public Thread cban_timer_thread = null;
  
  public Thread discover_ip_timer_thread = null;
  
  public Thread log_rolling_thread = null;
  
  public Thread events_thread = null;
  
  public Thread monitor_folders_thread = null;
  
  public Thread monitor_folders_thread_instant = null;
  
  public Thread http_cleaner_thread = null;
  
  public Thread vfs_replication_pinger_thread = null;
  
  public Thread update_2_timer_thread = null;
  
  public Thread expireThread = null;
  
  public Thread jobs_resumer_thread = null;
  
  public Vector main_servers = new Vector();
  
  public String CRLF = "\r\n";
  
  public Properties dayofweek = new Properties();
  
  public Common common_code = new Common();
  
  public Properties default_settings = new Properties();
  
  public Vector previewWorkers = new Vector();
  
  public SimpleDateFormat logDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
  
  public SimpleDateFormat mmddyyhhmmss = new SimpleDateFormat("MMddyyyyHHmmss", Locale.US);
  
  SimpleDateFormat weekday = new SimpleDateFormat("EEE", Locale.US);
  
  SimpleDateFormat MM = new SimpleDateFormat("MM", Locale.US);
  
  SimpleDateFormat mm = new SimpleDateFormat("mm");
  
  public Object eventLock = new Object();
  
  public Object userListLock = new Object();
  
  public Object recentUserListLock = new Object();
  
  public Object loginsLock = new Object();
  
  public boolean starting = true;
  
  ShutdownHandler shutdown = new ShutdownHandler();
  
  LoggingProvider loggingProvider1 = null;
  
  LoggingProvider loggingProvider2 = null;
  
  PreferencesProvider prefsProvider = new PreferencesProvider();
  
  static String hostname = "unknown";
  
  public ServerStatus(boolean start_threads, Properties server_settings2) {
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      e.printStackTrace();
    } 
    if (clasLoader == null)
      clasLoader = Thread.currentThread().getContextClassLoader(); 
    Maverick.initLicense();
    thisObj = this;
    System.getProperties().put("crushftp.version", "6");
    System.setProperty("mail.mime.ignoreunknownencoding", "true");
    this.server_info.put("user_list", new Vector());
    this.server_info.put("last_logins", new Vector());
    this.server_info.put("recent_user_list", new Vector());
    this.server_info.put("invalid_usernames", new Properties());
    this.server_info.put("running_tasks", new Vector());
    Common.System2.put("running_tasks", this.server_info.get("running_tasks"));
    Common.System2.put("alerts_queue", new Vector());
    this.server_info.put("machine_is_x_10_5_plus", (new StringBuffer(String.valueOf(Common.machine_is_x_10_5_plus()))).toString());
    this.server_info.put("machine_is_x", (new StringBuffer(String.valueOf(Common.machine_is_x()))).toString());
    this.server_info.put("machine_is_windows", (new StringBuffer(String.valueOf(Common.machine_is_windows()))).toString());
    this.server_info.put("machine_is_linux", (new StringBuffer(String.valueOf(Common.machine_is_linux()))).toString());
    this.server_info.put("machine_is_unix", (new StringBuffer(String.valueOf(Common.machine_is_unix()))).toString());
    this.server_info.put("machine_is_solaris", (new StringBuffer(String.valueOf(Common.machine_is_solaris()))).toString());
    this.server_info.put("os.name", System.getProperties().getProperty("os.name", "").toUpperCase());
    this.server_info.put("os.version", System.getProperties().getProperty("os.version", "").toUpperCase());
    this.server_info.put("update_available", "false");
    this.server_info.put("update_available_version", version_info_str);
    this.server_info.put("update_available_html", "");
    if (server_settings2 != null) {
      server_settings = server_settings2;
      this.server_info.put("dmz_mode", "true");
    } else {
      this.server_info.put("dmz_mode", "false");
    } 
    init_setup(start_threads);
    this.server_info.put("successful_logins", SG("successful_logins"));
    this.server_info.put("failed_logins", SG("failed_logins"));
    if (IG("phammer_attempts") == 5)
      server_settings.put("phammer_attempts", "15"); 
    killUpdateFiles();
    Vector pref_server_items = (Vector)server_settings.get("server_list");
    for (int x = 0; x < pref_server_items.size(); x++) {
      Properties the_server = pref_server_items.elementAt(x);
      if (server_settings.containsKey("pasv_ports") && the_server.getProperty("serverType", "FTP").equalsIgnoreCase("FTP"))
        the_server.put("pasv_ports", server_settings.getProperty("pasv_ports", "1025-65535")); 
      if (the_server.getProperty("server_ip", "").equals(""))
        the_server.put("server_ip", "auto"); 
      if (server_settings.containsKey("ftp_aware_router") && the_server.getProperty("serverType", "FTP").equalsIgnoreCase("FTP"))
        the_server.put("ftp_aware_router", server_settings.getProperty("ftp_aware_router", "false")); 
    } 
    server_settings.remove("server_ip");
    server_settings.remove("pasv_ports");
    server_settings.remove("ftp_aware_router");
    SharedSession.find("recent_user_list");
    try {
      Worker.startWorker(new Runnable(this) {
            final ServerStatus this$0;
            
            public void run() {
              try {
                Thread.sleep(10000L);
              } catch (InterruptedException interruptedException) {}
              this.this$0.runAlerts("started", null);
            }
          });
    } catch (IOException iOException) {}
  }
  
  public static boolean killUpdateFiles() {
    (new File("./WebInterface/Reports/nohup.out")).delete();
    (new File(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/nohup.out")).delete();
    killJar("log4j-1.2.6.jar", "log4j.jar");
    killJar("jgroups.jar", "jgroups.jar");
    killJar("commons-compress-1.3.jar", "commons-compress.jar");
    killJar("pgplib-2.5.jar", "pgplib.jar");
    killJar("bcmail-jdk15on-147.jar", "bcmail-jdk15on.jar");
    killJar("bcpg-jdk15on-147.jar", "bcpg-jdk15on.jar");
    killJar("bcpkix-jdk15on-147.jar", "bcpkix-jdk15on.jar");
    killJar("bcprov-jdk15on-147.jar", "bcprov-jdk15on.jar");
    String updateHome = "./";
    if (Common.OSXApp())
      updateHome = "../../../../"; 
    if (!(new File(String.valueOf(updateHome) + "update.bat")).exists()) {
      (new File(String.valueOf(updateHome) + "update.sh")).delete();
      (new File(String.valueOf(updateHome) + "update_list.txt")).delete();
      Common.recurseDelete(String.valueOf(updateHome) + "UpdateTemp/", false);
      (new File(String.valueOf(updateHome) + "CrushFTP4_PC_new.zip")).delete();
      (new File(String.valueOf(updateHome) + "CrushFTP4_OSX_new.zip")).delete();
      (new File(String.valueOf(updateHome) + "CrushFTP5_OSX_new.zip")).delete();
      (new File(String.valueOf(updateHome) + "CrushFTP5_PC_new.zip")).delete();
      (new File(String.valueOf(updateHome) + "CrushFTP6_OSX_new.zip")).delete();
      (new File(String.valueOf(updateHome) + "CrushFTP6_PC_new.zip")).delete();
      (new File(String.valueOf(updateHome) + "CrushFTP7_OSX_new.zip")).delete();
      (new File(String.valueOf(updateHome) + "CrushFTP7_PC_new.zip")).delete();
      return true;
    } 
    return false;
  }
  
  public static void killJar(String oldjar, String newjar) {
    if ((new File("./plugins/lib/" + oldjar)).exists() && (new File("./plugins/lib/" + newjar)).exists())
      (new File("./plugins/lib/" + oldjar)).delete(); 
  }
  
  public void checkCrushExpiration() {
    if (!SG("registration_name").equals("crush") && !SG("registration_email").equals("ftp"))
      try {
        boolean ok = this.common_code.register(SG("registration_name"), SG("registration_email"), SG("registration_code"));
        String v = null;
        if (ok)
          v = this.common_code.getRegistrationAccess("V", SG("registration_code")); 
        if (v != null && (v.equals("4") || v.equals("5") || v.equals("6"))) {
          String msg = "CrushFTP " + version_info_str + " will not work with a CrushFTP " + v + " license.";
          Log.log("SERVER", 0, msg);
          put_in("max_max_users", "5");
          put_in("max_users", "5");
        } else if (v == null && this.expireThread == null) {
          String msg = "Your license is expired.\r\nCrushFTP will automatically quit in 5 minutes.";
          Log.log("SERVER", 0, msg);
          this.expireThread = new Thread(new Runnable(this) {
                final ServerStatus this$0;
                
                public void run() {
                  try {
                    for (int x = 0; x < 5; x++) {
                      Thread.sleep(60000L);
                      this.this$0.checkCrushExpiration();
                    } 
                    String msg = "Your license is expired.\r\nYour 5 minutes is up. CrushFTP is quitting now.";
                    Log.log("SERVER", 0, msg);
                    this.this$0.quit_server();
                  } catch (Exception exception) {}
                }
              });
          this.expireThread.start();
        } else if (this.expireThread != null) {
          this.expireThread.interrupt();
        } 
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        quit_server();
      }  
  }
  
  public void init_setup(boolean start_threads) {
    this.dayofweek.put("Sun", "1");
    this.dayofweek.put("Mon", "2");
    this.dayofweek.put("Tue", "3");
    this.dayofweek.put("Wed", "4");
    this.dayofweek.put("Thu", "5");
    this.dayofweek.put("Fri", "6");
    this.dayofweek.put("Sat", "7");
    siPUT("logged_in_users", "0");
    siPUT("concurrent_users", "0");
    siPUT("version_info_str", version_info_str);
    siPUT("sub_version_info_str", sub_version_info_str);
    siPUT("about_info_str", String.valueOf(LOC.G("CrushFTP")) + " " + siSG("version_info_str") + siSG("sub_version_info_str") + " from CrushFTP, LLC");
    siPUT("java_info", String.valueOf(System.getProperty("java.home")) + "/bin/java\r\n" + System.getProperty("java.version") + ", " + System.getProperty("sun.arch.data.model") + " bit\r\n" + System.getProperties().getProperty("os.name"));
    siPUT("server_start_time", this.server_start_time);
    siPUT("current_download_speed", "0");
    siPUT("current_upload_speed", "0");
    siPUT("last_day_of_week", this.weekday.format(new Date()));
    siPUT("last_month", this.MM.format(new Date()));
    siPUT("hammer_history", "");
    siPUT("hammer_history_http", "");
    siPUT("incoming_transfers", new Vector());
    siPUT("outgoing_transfers", new Vector());
    siPUT("user_login_num", this.server_info.getProperty("user_login_num", "0"));
    try {
      siPUT("currentFileDate", (new StringBuffer(String.valueOf(this.prefsProvider.getPrefsTime(null)))).toString());
    } catch (Exception exception) {}
    this.common_code.set_defaults(this.default_settings);
    if (!siBG("dmz_mode"))
      server_settings = (Properties)this.default_settings.clone(); 
    (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).mkdirs();
    Common.updateOSXInfo(String.valueOf(System.getProperty("crushftp.backup")) + "backup/");
    try {
      if (!siBG("dmz_mode"))
        server_settings = this.prefsProvider.loadPrefs(null); 
      this.server_info.put("currentFileDate", (new StringBuffer(String.valueOf(this.prefsProvider.getPrefsTime(null)))).toString());
      this.prefsProvider.check_code();
    } catch (Exception ee) {
      Log.log("SERVER", 0, "Prefs.XML was corrupt.  Looking for automatic backup...");
      Log.log("SERVER", 0, ee);
      server_settings = this.prefsProvider.getBackupPrefs(null);
      thisObj.starting = false;
      thisObj.save_server_settings(true);
      thisObj.starting = true;
      this.prefsProvider.check_code();
    } 
    setupGlobalPrefs();
    try {
      if (start_threads)
        Thread.sleep((IG("startup_delay") * 1000)); 
    } catch (InterruptedException interruptedException) {}
    boolean needSave = false;
    try {
      if (this.loggingProvider2 != null)
        this.loggingProvider2.shutdown(); 
      this.loggingProvider2 = null;
      if (!SG("logging_provider").equals("") && !SG("logging_provider").equals("crushftp.handlers.log.LoggingProviderDisk"))
        this.loggingProvider2 = (LoggingProvider)Class.forName(SG("logging_provider")).newInstance(); 
    } catch (Exception e) {
      this.loggingProvider2 = null;
      e.printStackTrace();
      Log.log("SERVER", 0, e);
    } 
    try {
      if (this.loggingProvider1 == null)
        this.loggingProvider1 = new LoggingProviderDisk(); 
    } catch (Exception e) {
      e.printStackTrace();
      Log.log("SERVER", 0, e);
    } 
    if (VG("plugins") != null)
      for (int x = VG("plugins").size() - 1; x >= 0; x--) {
        try {
          Vector subitems = VG("plugins").elementAt(x);
          Properties p = subitems.elementAt(0);
          if (p.getProperty("pluginName").equalsIgnoreCase("mm.mysql-2.0.14-bin") || p.getProperty("pluginName").equalsIgnoreCase("mysql-connector-java-5.0.4-bin")) {
            VG("plugins").removeElementAt(x);
            needSave = true;
          } else if (!(new File(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/" + p.getProperty("pluginName") + ".jar")).exists()) {
            VG("plugins").removeElementAt(x);
            needSave = true;
          } 
        } catch (Exception exception) {}
      }  
    if (!SG("prefs_version").startsWith("6") && !SG("prefs_version").startsWith("7")) {
      needSave = true;
      server_settings.put("prefs_version", "6");
      Vector previews = (Vector)server_settings.get("preview_configs");
      if (previews == null || (previews.size() == 0 && server_settings.containsKey("preview_enabled"))) {
        previews = new Vector();
        try {
          Properties pre = new Properties();
          pre.put("preview_enabled", server_settings.getProperty("preview_enabled"));
          pre.put("preview_debug", server_settings.getProperty("preview_debug"));
          pre.put("preview_scan_interval", server_settings.getProperty("preview_scan_interval"));
          pre.put("preview_command_line", server_settings.getProperty("preview_command_line"));
          pre.put("preview_conversion_threads", server_settings.getProperty("preview_conversion_threads"));
          pre.put("preview_file_extensions", server_settings.getProperty("preview_file_extensions"));
          pre.put("preview_sizes", server_settings.get("preview_sizes"));
          pre.put("preview_working_dir", server_settings.getProperty("preview_working_dir"));
          pre.put("preview_environment", server_settings.getProperty("preview_environment"));
          pre.put("preview_exif", server_settings.getProperty("preview_exif"));
          pre.put("preview_subdirectories", server_settings.getProperty("preview_subdirectories"));
          pre.put("preview_reverseSubdirectories", server_settings.getProperty("preview_reverseSubdirectories"));
          pre.put("preview_folder_list", server_settings.get("preview_folder_list"));
          previews.addElement(pre);
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
        } 
        server_settings.put("preview_configs", previews);
      } 
      if (!SG("smtp_pass").equals(""))
        server_settings.put("smtp_pass", this.common_code.encode_pass(SG("smtp_pass"), "DES", "")); 
      if (!SG("search_db_pass").equals(""))
        server_settings.put("search_db_pass", this.common_code.encode_pass(SG("search_db_pass"), "DES", "")); 
      if (!SG("db_pass").equals(""))
        server_settings.put("db_pass", this.common_code.encode_pass(SG("db_pass"), "DES", "")); 
      if (!SG("syncs_db_pass").equals(""))
        server_settings.put("syncs_db_pass", this.common_code.encode_pass(SG("syncs_db_pass"), "DES", "")); 
      if (!SG("stats_db_pass").equals(""))
        server_settings.put("stats_db_pass", this.common_code.encode_pass(SG("stats_db_pass"), "DES", "")); 
      if (!SG("filter1").equals(""))
        server_settings.put("globalKeystoreCertPass", server_settings.remove("filter1")); 
      if (!SG("filter2").equals(""))
        server_settings.put("globalKeystorePass", server_settings.remove("filter2")); 
      if (SG("log_allow_str").equals("(ERROR)(START)(STOP)(QUIT_SERVER)(RUN_SERVER)(KICK)(BAN)(DENIAL)(ACCEPT)(DISCONNECT)(USER)(PASS)(SYST)(NOOP)(SIZE)(MDTM)(RNFR)(RNTO)(PWD)(CWD)(TYPE)(REST)(DELE)(MKD)(RMD)(MACB)(ABOR)(RETR)(STOR)(APPE)(LIST)(NLST)(CDUP)(PASV)(PORT)(AUTH)(PBSZ)(PROT)(SITE)(QUIT)(GET)(PUT)(DELETE)(MOVE)(STAT)(HELP)(PAUSE_RESUME)(PROXY)"))
        server_settings.put("log_allow_str", this.default_settings.getProperty("log_allow_str")); 
      for (int x = VG("plugins").size() - 1; x >= 0; x--) {
        try {
          Vector subitems = VG("plugins").elementAt(x);
          for (int xx = 0; xx < subitems.size(); xx++) {
            Properties p = subitems.elementAt(xx);
            if (p.getProperty("pluginName").equalsIgnoreCase("CrushNOIP")) {
              if (!p.getProperty("pass", "").equals(""))
                p.put("pass", this.common_code.encode_pass(p.getProperty("pass"), "DES", "")); 
            } else if (p.getProperty("pluginName").equalsIgnoreCase("CrushSQL")) {
              if (!p.getProperty("db_pass", "").equals(""))
                p.put("db_pass", this.common_code.encode_pass(p.getProperty("db_pass"), "DES", "")); 
            } else if (p.getProperty("pluginName").equalsIgnoreCase("CrushTask")) {
              Vector tasks = (Vector)p.get("tasks");
              for (int xxx = 0; tasks != null && xxx < tasks.size(); xxx++) {
                Properties t = tasks.elementAt(xxx);
                if (t.getProperty("type", "").equalsIgnoreCase("HTTP")) {
                  if (!t.getProperty("password", "").equals(""))
                    t.put("password", this.common_code.encode_pass(t.getProperty("password"), "DES", "")); 
                } else if (t.getProperty("type", "").equalsIgnoreCase("FIND") || t.getProperty("type", "").equalsIgnoreCase("COPY")) {
                  if (!t.getProperty("ssh_private_key_pass", "").equals(""))
                    t.put("ssh_private_key_pass", this.common_code.encode_pass(t.getProperty("ssh_private_key_pass"), "DES", "")); 
                } else if (t.getProperty("type", "").equalsIgnoreCase("PGP")) {
                  if (!t.getProperty("key_password", "").equals(""))
                    t.put("key_password", this.common_code.encode_pass(t.getProperty("key_password"), "DES", "")); 
                } else if (t.getProperty("type", "").equalsIgnoreCase("PopImap")) {
                  if (!t.getProperty("mail_pass", "").equals(""))
                    t.put("mail_pass", this.common_code.encode_pass(t.getProperty("mail_pass"), "DES", "")); 
                } 
              } 
            } else if (p.getProperty("pluginName").equalsIgnoreCase("PostBack")) {
              if (!p.getProperty("password", "").equals(""))
                p.put("password", this.common_code.encode_pass(p.getProperty("password"), "DES", "")); 
            } 
          } 
        } catch (Exception e) {
          Log.log("PLUGIN", 0, e);
        } 
      } 
      Vector pref_server_items = (Vector)server_settings.get("server_list");
      for (int i = 0; i < pref_server_items.size(); i++) {
        Properties p = pref_server_items.elementAt(i);
        if (p.getProperty("serverType", "FTP").equalsIgnoreCase("SFTP"))
          SSHDaemon.setupDaemon(p); 
      } 
    } 
    if (!SG("prefs_version").equals("7.0") && !siBG("dmz_mode")) {
      Vector schedules = (Vector)server_settings.get("schedules");
      (new File(String.valueOf(SG("jobs_location")) + "jobs/")).mkdirs();
      for (int x = 0; schedules != null && x < schedules.size(); x++) {
        Properties p = schedules.elementAt(x);
        String scheduleName = p.getProperty("scheduleName");
        scheduleName = JobScheduler.safeName(scheduleName);
        (new File(String.valueOf(SG("jobs_location")) + "jobs/" + scheduleName)).mkdirs();
        try {
          Common.writeXMLObject(String.valueOf(SG("jobs_location")) + "jobs/" + scheduleName + "/job.XML", p, "job");
        } catch (Exception e) {
          throw new RuntimeException(e);
        } 
      } 
      needSave = true;
      server_settings.put("prefs_version", "7.0");
    } 
    if (SG("Access-Control-Allow-Origin").equals("true"))
      server_settings.put("Access-Control-Allow-Origin", "*"); 
    if (SG("tls_version").equals("") || SG("tls_version").equals("1") || SG("tls_version").equals("TLSv1"))
      server_settings.put("tls_version", "SSLv2Hello,TLSv1,TLSv1.1,TLSv1.2"); 
    if (SG("log_roll_date_format").equals("yyyyMMdd_hhmmss"))
      server_settings.put("log_roll_date_format", "yyyyMMdd_HHmmss"); 
    if (SG("disabled_ciphers").equals(""))
      server_settings.put("disabled_ciphers", this.default_settings.getProperty("disabled_ciphers")); 
    if (SG("disabled_ciphers").toUpperCase().indexOf("_EXPORT_") < 0) {
      String disabled_ciphers = SG("disabled_ciphers").toUpperCase();
      try {
        ServerSocketFactory ssf = this.common_code.getSSLContext("builtin", "builtin", "", "", "TLS", false, true).getServerSocketFactory();
        SSLServerSocket serverSock = (SSLServerSocket)ssf.createServerSocket(0, 1);
        String[] ciphers = serverSock.getSupportedCipherSuites();
        serverSock.close();
        for (int x = 0; x < ciphers.length; x++) {
          if (ciphers[x].toUpperCase().indexOf("EXPORT") >= 0 && disabled_ciphers.indexOf(ciphers[x].toUpperCase()) < 0) {
            disabled_ciphers = String.valueOf(disabled_ciphers) + "(" + ciphers[x].toUpperCase() + ")";
            needSave = true;
          } else if (ciphers[x].toUpperCase().indexOf("ANON") >= 0 && disabled_ciphers.indexOf(ciphers[x].toUpperCase()) < 0) {
            disabled_ciphers = String.valueOf(disabled_ciphers) + "(" + ciphers[x].toUpperCase() + ")";
            needSave = true;
          } else if (ciphers[x].toUpperCase().indexOf("NULL") >= 0 && disabled_ciphers.indexOf(ciphers[x].toUpperCase()) < 0) {
            disabled_ciphers = String.valueOf(disabled_ciphers) + "(" + ciphers[x].toUpperCase() + ")";
            needSave = true;
          } else if (ciphers[x].toUpperCase().indexOf("INFO") >= 0 && disabled_ciphers.indexOf(ciphers[x].toUpperCase()) < 0) {
            disabled_ciphers = String.valueOf(disabled_ciphers) + "(" + ciphers[x].toUpperCase() + ")";
            needSave = true;
          } 
        } 
        server_settings.put("disabled_ciphers", disabled_ciphers);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } 
    checkCrushExpiration();
    Properties localization = new Properties();
    String localized = server_settings.getProperty("localization", "English");
    if (localized.equals("ENGLISH"))
      localized = "English"; 
    server_settings.put("localization", localized);
    localization.put("localization", localized);
    this.logDateFormat = new SimpleDateFormat(SG("log_date_format"), Locale.US);
    try {
      (new File(String.valueOf((new File(change_vars_to_values_static(SG("log_location"), null, null, null))).getCanonicalFile().getParentFile().getPath()) + "/logs/session_logs/")).mkdirs();
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    if (server_settings.containsKey("s3_threads")) {
      server_settings.put("s3_threads_download", server_settings.getProperty("s3_threads"));
      server_settings.put("s3_threads_upload", server_settings.getProperty("s3_threads"));
    } 
    try {
      startStatsLoader(this.server_info, String.valueOf(System.getProperty("crushftp.stats")) + "stats.XML");
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    Enumeration the_list = this.default_settings.propertyNames();
    while (the_list.hasMoreElements()) {
      Object cur = the_list.nextElement();
      if (server_settings.get(cur.toString()) == null) {
        server_settings.put(cur.toString(), this.default_settings.get(cur.toString()));
        needSave = true;
      } 
    } 
    Properties sqlItems = (Properties)server_settings.get("sqlItems");
    Properties sqlItems2 = new Properties();
    SQLUsers.setDefaults(sqlItems2);
    this.default_settings.put("sqlItems", sqlItems2.clone());
    Enumeration keys = sqlItems2.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement().toString();
      if (!sqlItems.containsKey(key))
        sqlItems.put(key, sqlItems2.get(key)); 
    } 
    the_list = server_settings.propertyNames();
    while (the_list.hasMoreElements()) {
      Object cur = the_list.nextElement();
      if (!this.default_settings.containsKey(cur.toString()))
        if (!cur.toString().equals("ftp_aware_router") && !cur.toString().equals("pasv_ports") && !cur.toString().equals("server_ip")) {
          server_settings.remove(cur.toString());
          needSave = true;
        }  
    } 
    this.prefsProvider.check_code();
    if (BG("block_client_renegotiation"))
      System.setProperty("jdk.tls.rejectClientInitiatedRenegotiation", "true"); 
    append_log(String.valueOf(thisObj.logDateFormat.format(new Date())) + "|********" + LOC.G("CrushFTP Run") + "******** " + siSG("version_info_str") + siSG("sub_version_info_str") + "  Java:" + System.getProperty("java.version") + " from:" + System.getProperty("java.home") + " " + System.getProperty("sun.arch.data.model") + " bit  OS:" + System.getProperties().getProperty("os.name") + " Max RAM:" + Common.format_bytes_short2(Runtime.getRuntime().maxMemory()), "RUN_SERVER");
    try {
      this.server_info.put("jce_installed", (new StringBuffer(String.valueOf((Cipher.getMaxAllowedKeyLength("AES") == Integer.MAX_VALUE)))).toString());
      String ipList = "";
      Vector allow_list = (Vector)server_settings.get("ip_restrictions");
      for (int x = allow_list.size() - 1; x >= 0; x--) {
        Properties ip_data = allow_list.elementAt(x);
        String s = String.valueOf(ip_data.getProperty("start_ip")) + ":" + ip_data.getProperty("stop_ip") + ":" + ip_data.getProperty("type");
        if (ipList.indexOf(s) < 0) {
          ipList = String.valueOf(ipList) + s;
        } else {
          allow_list.remove(ip_data);
        } 
      } 
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
    } 
    if (!BG("csrf_flipped")) {
      server_settings.put("csrf", "true");
      server_settings.put("csrf_flipped", "true");
      needSave = true;
    } 
    checkServerGroups();
    Vector v = new Vector();
    try {
      UserTools.refreshUserList("extra_vfs", v);
      int tildas = 0;
      int x;
      for (x = 0; tildas == 0 && x < v.size(); x++) {
        String username = v.elementAt(x).toString();
        if (username.indexOf("~") >= 0) {
          if (tildas == 0)
            Log.log("SERVER", 0, "Found a tilda extra_vfs, no update needed:" + username); 
          tildas++;
        } 
      } 
      for (x = 0; tildas == 0 && x < v.size(); x++) {
        String username = v.elementAt(x).toString();
        Properties user = UserTools.ut.getUser("extra_vfs", username, false);
        if (username.indexOf("~") < 0 && username.indexOf("_") >= 0) {
          String username2 = String.valueOf(username.substring(0, username.lastIndexOf("_"))) + "~" + username.substring(username.lastIndexOf("_") + 1);
          UserTools.changeUsername("extra_vfs", username, username2, user.getProperty("password", ""));
          Log.log("SERVER", 0, "Updating extra_vfs username:" + username + "->" + username2);
        } 
      } 
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    this.total_server_bytes_sent = LG("total_server_bytes_sent");
    this.total_server_bytes_received = LG("total_server_bytes_received");
    if (!System.getProperty("crushftp.previews", "").equals(""))
      server_settings.put("previews_path", System.getProperty("crushftp.previews", "")); 
    reset_threads(start_threads);
    reset_preview_workers();
    if (start_threads) {
      try {
        this.update_timer_thread.interrupt();
      } catch (Exception exception) {}
      this.update_timer_thread = null;
      UpdateTimer the_thread = new UpdateTimer(this, 1000, "ServerStatus", "gui_timer");
      this.update_timer_thread = new Thread(the_thread);
      this.update_timer_thread.setName("ServerStatus:update_timer");
      this.update_timer_thread.setPriority(1);
      this.update_timer_thread.start();
    } 
    if (this.commandBufferFlusher == null) {
      this.commandBufferFlusher = new CommandBufferFlusher(IG("command_flush_interval"));
      Thread t = new Thread(this.commandBufferFlusher);
      t.setName("ServerStatis:CommandBufferFlusher");
      t.start();
    } 
    this.commandBufferFlusher.setInterval(IG("command_flush_interval"));
    if (start_threads && System.getProperty("crushftp.start_servers", "true").equals("true"))
      start_all_servers(); 
    this.starting = false;
    if (needSave)
      save_server_settings(true); 
  }
  
  public void reset_preview_workers() {
    boolean ok = false;
    if (this.previewWorkers.size() == VG("preview_configs").size()) {
      ok = true;
      for (int i = 0; i < this.previewWorkers.size(); i++) {
        PreviewWorker preview = this.previewWorkers.elementAt(i);
        Properties prefs = VG("preview_configs").elementAt(i);
        if (preview.prefs != prefs)
          ok = false; 
      } 
    } 
    if (ok)
      return; 
    while (this.previewWorkers.size() > 0) {
      PreviewWorker preview = this.previewWorkers.elementAt(0);
      preview.abort = true;
      this.previewWorkers.removeElementAt(0);
    } 
    for (int x = 0; x < VG("preview_configs").size(); x++) {
      Properties prefs = VG("preview_configs").elementAt(x);
      PreviewWorker preview = new PreviewWorker(prefs);
      this.previewWorkers.addElement(preview);
    } 
  }
  
  public static void startStatsLoader(Properties server_info, String statsPath) {
    thisObj.statTools.init();
  }
  
  public void setSettings(Properties p) {
    Properties source = (Properties)p.get("data");
    Vector log = new Vector();
    try {
      Common.diffObjects(source, server_settings, log, "", false);
    } catch (RuntimeException e) {
      Log.log("SERVER", 0, e);
    } 
    thisObj.append_log(String.valueOf(thisObj.logDateFormat.format(new Date())) + "|" + "Server Settings Changed", "RUN_SERVER");
    for (int x = 0; x < log.size(); x++) {
      if (log.elementAt(x).toString().toUpperCase().indexOf("PASSWORD") < 0)
        thisObj.append_log(String.valueOf(thisObj.logDateFormat.format(new Date())) + "|" + log.elementAt(x).toString(), "RUN_SERVER"); 
    } 
    Properties dest = server_settings;
    Enumeration the_list = this.default_settings.propertyNames();
    while (the_list.hasMoreElements()) {
      String cur = the_list.nextElement().toString();
      if (source.containsKey(cur)) {
        Object sourceO = source.get(cur);
        Object destO = server_settings.get(cur);
        if (destO == null || destO instanceof String) {
          dest.put(cur, sourceO);
          continue;
        } 
        Common.updateObject(sourceO, destO);
      } 
    } 
    thisObj.common_code.loadPluginsSync(server_settings, this.server_info);
    thisObj.save_server_settings(true);
  }
  
  public void reset_threads(boolean start_threads) {
    if (start_threads) {
      setup_hammer_banning();
      setup_phammer_banning();
      setup_ban_timer();
      setup_discover_ip_refresh();
      setup_log_rolling();
      setup_events();
      setup_monitor_folders();
      setup_http_cleaner();
      setup_vfs_replication_pinger();
      setup_update_2_timer();
      setup_stats_saver();
      setup_jobs_resumer();
      setup_report_scheduler();
      setup_scheduler();
      setup_alerts();
      setup_new_version();
      this.common_code.loadPlugins(server_settings, this.server_info);
    } 
  }
  
  public void reset_server_login_counts() {
    siPUT2("successful_logins", "0");
    siPUT2("failed_logins", "0");
  }
  
  public void reset_server_bytes_in_out() {
    this.total_server_bytes_sent = 0L;
    this.total_server_bytes_received = 0L;
    this.server_last_bytes_sent = 0L;
    this.server_last_bytes_received = 0L;
  }
  
  public void reset_upload_download_counter() {
    siPUT2("downloaded_files", "0");
    siPUT2("uploaded_files", "0");
  }
  
  public void setup_log_rolling() {
    try {
      if (this.log_rolling_thread != null) {
        int loops = 0;
        while (this.log_rolling_thread.isAlive() && loops++ < 100) {
          this.log_rolling_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.log_rolling_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 25000, "ServerStatus", "log_rolling");
    this.log_rolling_thread = new Thread(the_thread);
    this.log_rolling_thread.setName("ServerStatus:log_rolling");
    this.log_rolling_thread.setPriority(1);
    this.log_rolling_thread.start();
  }
  
  public void setup_events() {
    try {
      this.events_thread.interrupt();
    } catch (Exception exception) {}
    this.events_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 1000, "ServerStatus", "events_thread");
    this.events_thread = new Thread(the_thread);
    this.events_thread.setName("ServerStatus:events_thread");
    this.events_thread.setPriority(1);
    this.events_thread.start();
  }
  
  public void setup_monitor_folders() {
    try {
      this.monitor_folders_thread.interrupt();
    } catch (Exception exception) {}
    this.monitor_folders_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 60000, "ServerStatus", "monitor_folders");
    this.monitor_folders_thread = new Thread(the_thread);
    this.monitor_folders_thread.setName("ServerStatus:monitor_folders");
    this.monitor_folders_thread.setPriority(1);
    this.monitor_folders_thread.start();
    try {
      this.monitor_folders_thread_instant.interrupt();
    } catch (Exception exception) {}
    this.monitor_folders_thread_instant = new Thread(new UpdateTimer(this, 1000, "ServerStatus", "monitor_folders_instant"));
    this.monitor_folders_thread_instant.setName("ServerStatus:monitor_folders_instant");
    this.monitor_folders_thread_instant.setPriority(1);
    this.monitor_folders_thread_instant.start();
  }
  
  public void setup_http_cleaner() {
    try {
      if (this.http_cleaner_thread != null) {
        int loops = 0;
        while (this.http_cleaner_thread.isAlive() && loops++ < 100) {
          this.http_cleaner_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.http_cleaner_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 60000, "ServerStatus", "http_cleaner");
    this.http_cleaner_thread = new Thread(the_thread);
    this.http_cleaner_thread.setName("ServerStatus:http_cleaner");
    this.http_cleaner_thread.setPriority(1);
    this.http_cleaner_thread.start();
  }
  
  public void setup_vfs_replication_pinger() {
    try {
      if (this.vfs_replication_pinger_thread != null) {
        int loops = 0;
        while (this.vfs_replication_pinger_thread.isAlive() && loops++ < 100) {
          this.vfs_replication_pinger_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.vfs_replication_pinger_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 1000, "ServerStatus", "vfs_replication_pinger");
    this.vfs_replication_pinger_thread = new Thread(the_thread);
    this.vfs_replication_pinger_thread.setName("ServerStatus_replication_pinger:");
    this.vfs_replication_pinger_thread.setPriority(1);
    this.vfs_replication_pinger_thread.start();
  }
  
  public void setup_discover_ip_refresh() {
    try {
      if (this.discover_ip_timer_thread != null) {
        int loops = 0;
        while (this.discover_ip_timer_thread.isAlive() && loops++ < 100) {
          this.discover_ip_timer_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.discover_ip_timer_thread = null;
    int mins = IG("discover_ip_refresh");
    if (mins < 1)
      mins = 1; 
    UpdateTimer the_thread = new UpdateTimer(this, mins * 60000, "ServerStatus", "discover_ip_timer");
    this.discover_ip_timer_thread = new Thread(the_thread);
    this.discover_ip_timer_thread.setName("ServerStatus:discover_ip_timer");
    this.discover_ip_timer_thread.setPriority(1);
    this.discover_ip_timer_thread.start();
  }
  
  public void setup_ban_timer() {
    try {
      if (this.ban_timer_thread != null) {
        int loops = 0;
        while (this.ban_timer_thread.isAlive() && loops++ < 100) {
          this.ban_timer_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.ban_timer_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 10000, "ServerStatus", "ban_timer");
    this.ban_timer_thread = new Thread(the_thread);
    this.ban_timer_thread.setName("ServerStatus:ban_timer");
    this.ban_timer_thread.setPriority(1);
    this.ban_timer_thread.start();
    try {
      this.cban_timer_thread.interrupt();
    } catch (Exception exception) {}
    this.cban_timer_thread = null;
    the_thread = new UpdateTimer(this, 1000, "ServerStatus", "cban_timer");
    this.cban_timer_thread = new Thread(the_thread);
    this.cban_timer_thread.setName("ServerStatus:cban_timer");
    this.cban_timer_thread.setPriority(1);
    this.cban_timer_thread.start();
  }
  
  public void setup_hammer_banning() {
    try {
      if (this.hammer_timer_thread != null) {
        int loops = 0;
        while (this.hammer_timer_thread.isAlive() && loops++ < 100) {
          this.hammer_timer_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.hammer_timer_thread = new Thread(new UpdateTimer(this, IG("hammer_banning") * 1000, "ServerStatus", "hammer_timer"));
    this.hammer_timer_thread.setName("ServerStatus:hammer_timer");
    this.hammer_timer_thread.setPriority(1);
    this.hammer_timer_thread.start();
    try {
      if (this.hammer_timer_http_thread != null) {
        int loops = 0;
        while (this.hammer_timer_http_thread.isAlive() && loops++ < 100) {
          this.hammer_timer_http_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.hammer_timer_http_thread = new Thread(new UpdateTimer(this, IG("hammer_banning_http") * 1000, "ServerStatus", "hammer_timer_http"));
    this.hammer_timer_http_thread.setName("ServerStatus:hammer_timer");
    this.hammer_timer_http_thread.setPriority(1);
    this.hammer_timer_http_thread.start();
  }
  
  public void setup_phammer_banning() {
    try {
      this.phammer_timer_thread.interrupt();
    } catch (Exception exception) {}
    this.phammer_timer_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 10000, "ServerStatus", "phammer_timer");
    this.phammer_timer_thread = new Thread(the_thread);
    this.phammer_timer_thread.setName("ServerStatus:phammer_timer");
    this.phammer_timer_thread.setPriority(1);
    if (System.getProperty("crushftp.disablephammer", "false").equals("false"))
      this.phammer_timer_thread.start(); 
  }
  
  public void setup_update_2_timer() {
    try {
      this.update_2_timer_thread.interrupt();
    } catch (Exception exception) {}
    this.update_2_timer_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 5000, "ServerStatus", "update_2_timer");
    this.update_2_timer_thread = new Thread(the_thread);
    this.update_2_timer_thread.setName("ServerStatus:update_2_timer");
    this.update_2_timer_thread.setPriority(1);
    this.update_2_timer_thread.start();
  }
  
  public void setup_stats_saver() {
    try {
      if (this.stats_saver_thread != null) {
        int loops = 0;
        while (this.stats_saver_thread.isAlive() && loops++ < 100) {
          this.stats_saver_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.stats_saver_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, IG("stats_min") * 60000, "ServerStatus", "stats_saver");
    this.stats_saver_thread = new Thread(the_thread);
    this.stats_saver_thread.setName("ServerStatus:stats_saver");
    this.stats_saver_thread.setPriority(1);
    this.stats_saver_thread.start();
  }
  
  public void setup_jobs_resumer() {
    try {
      if (this.jobs_resumer_thread != null) {
        int loops = 0;
        while (this.jobs_resumer_thread.isAlive() && loops++ < 100) {
          this.jobs_resumer_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.jobs_resumer_thread = new Thread(new Runnable(this) {
          final ServerStatus this$0;
          
          public void run() {
            while (true) {
              try {
                if (ServerStatus.IG("resume_idle_job_delay") <= 0) {
                  Thread.sleep((ServerStatus.IG("resume_idle_job_delay") * -1000));
                  Thread.sleep(100L);
                  continue;
                } 
                Thread.sleep((ServerStatus.IG("resume_idle_job_delay") * 1000));
                Vector jobs = JobScheduler.getJobList();
                for (int x = jobs.size() - 1; x >= 0; x--) {
                  File f = jobs.elementAt(x);
                  if ((new File(String.valueOf(f.getPath()) + "/inprogress.XML")).exists()) {
                    (new File(String.valueOf(f.getPath()) + "/inprogress/")).mkdirs();
                    Properties tracker = (Properties)Common.readXMLObject(new File(String.valueOf(f.getPath()) + "/inprogress.XML"));
                    (new File(String.valueOf(f.getPath()) + "/inprogress.XML")).renameTo(new File(String.valueOf(f.getPath()) + "/inprogress/" + tracker.getProperty("id") + ".XML"));
                  } 
                  if ((new File(String.valueOf(f.getPath()) + "/inprogress/")).exists()) {
                    File[] ids = (new File(String.valueOf(f.getPath()) + "/inprogress/")).listFiles();
                    for (int xx = 0; xx < ids.length; xx++) {
                      if (ids[xx].getName().toUpperCase().endsWith(".XML"))
                        if (System.currentTimeMillis() - ids[xx].lastModified() > (ServerStatus.IG("resume_idle_job_delay") * 1000))
                          if (!(new File(String.valueOf(f.getPath()) + "/job.XML")).exists()) {
                            ids[xx].renameTo(new File(String.valueOf(f.getPath()) + "/" + ids[xx].getName()));
                          } else {
                            long delay = System.currentTimeMillis() - ids[xx].lastModified();
                            ids[xx].setLastModified(System.currentTimeMillis() + (ServerStatus.IG("resume_idle_job_delay") * 1000));
                            Log.log("SERVER", 0, "Resuming idle job...:" + delay + ":" + ids[xx]);
                            AdminControls.startJob(f, true, new StringBuffer(ids[xx].getName().substring(0, ids[xx].getName().lastIndexOf("."))), null);
                          }   
                    } 
                  } 
                } 
              } catch (Exception e) {
                if (e.indexOf("Interrupted") >= 0)
                  break; 
                Log.log("SERVER", 0, e);
              } 
            } 
          }
        });
    this.jobs_resumer_thread.setName("ServerStatus:jobs_resumer");
    this.jobs_resumer_thread.setPriority(1);
    this.jobs_resumer_thread.start();
  }
  
  public void setup_report_scheduler() {
    try {
      if (this.report_scheduler_thread != null) {
        int loops = 0;
        while (this.report_scheduler_thread.isAlive() && loops++ < 100) {
          this.report_scheduler_thread.interrupt();
          Thread.sleep(100L);
        } 
      } 
    } catch (Exception exception) {}
    this.report_scheduler_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 40000, "ServerStatus", "report_scheduler");
    this.report_scheduler_thread = new Thread(the_thread);
    this.report_scheduler_thread.setName("ServerStatus:report_scheduler");
    this.report_scheduler_thread.setPriority(1);
    this.report_scheduler_thread.start();
  }
  
  public void setup_scheduler() {
    try {
      this.scheduler_thread.interrupt();
    } catch (Exception exception) {}
    this.scheduler_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 1000, "ServerStatus", "schedules");
    this.scheduler_thread = new Thread(the_thread);
    this.scheduler_thread.setName("ServerStatus:schedules");
    this.scheduler_thread.setPriority(1);
    this.scheduler_thread.start();
  }
  
  public void setup_alerts() {
    try {
      this.alerts_thread.interrupt();
    } catch (Exception exception) {}
    this.alerts_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 60000, "ServerStatus", "alerts");
    this.alerts_thread = new Thread(the_thread);
    this.alerts_thread.setName("ServerStatus:alerts");
    this.alerts_thread.setPriority(1);
    this.alerts_thread.start();
  }
  
  public void setup_new_version() {
    try {
      this.new_version_thread.interrupt();
    } catch (Exception exception) {}
    this.new_version_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, 60000, "ServerStatus", "new_version");
    this.new_version_thread = new Thread(the_thread);
    this.new_version_thread.setName("ServerStatus:new_version");
    this.new_version_thread.setPriority(1);
    this.new_version_thread.start();
  }
  
  public void hold_user_pointer(Properties user_info) {
    synchronized (SharedSession.sessionLock) {
      synchronized (this.userListLock) {
        if (siVG("user_list").indexOf(user_info) < 0) {
          siVG("user_list").addElement(user_info);
          siPUT("logged_in_users", siVG("user_list").size());
          updateConcurrentUsers();
        } 
        while (siVG("recent_user_list").indexOf(user_info) >= 0)
          siVG("recent_user_list").remove(user_info); 
      } 
    } 
  }
  
  public void updateConcurrentUsers() {
    Properties uniqueSessions = new Properties();
    Properties concurrentUsers = new Properties();
    synchronized (SharedSession.sessionLock) {
      synchronized (this.userListLock) {
        for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
          Properties p = siVG("user_list").elementAt(x);
          SessionCrush theSession = (SessionCrush)p.get("session");
          if (theSession != null) {
            String protocol = theSession.uiSG("user_protocol");
            float cur = Float.parseFloat(concurrentUsers.getProperty(String.valueOf(protocol) + ":" + theSession.uiSG("user_ip"), "0"));
            String sessionID = theSession.uiSG("sessionID");
            if (sessionID == null)
              sessionID = ""; 
            if ((protocol.toUpperCase().startsWith("HTTP") || protocol.toUpperCase().startsWith("WEBDAV")) && uniqueSessions.containsKey(sessionID)) {
              cur += 0.25F;
            } else {
              cur++;
              uniqueSessions.put(sessionID, "");
            } 
            concurrentUsers.put(String.valueOf(protocol) + ":" + theSession.uiSG("user_ip"), (new StringBuffer(String.valueOf(cur))).toString());
          } 
        } 
      } 
    } 
    Enumeration e = concurrentUsers.keys();
    float total = 0.0F;
    while (e.hasMoreElements()) {
      String key = e.nextElement().toString();
      String val = concurrentUsers.getProperty(key);
      total += Float.parseFloat(val);
    } 
    if (total > 0.0F && total < 1.0F)
      total = 1.0F; 
    siPUT("concurrent_users", (new StringBuffer(String.valueOf((int)total))).toString());
  }
  
  public void set_user_pointer(Properties user_info) {
    String ip_text = user_info.getProperty("user_ip");
    ip_text = ip_text.substring(ip_text.indexOf("/") + 1, ip_text.length());
    this.server_info.put("last_login_user", user_info.getProperty("user_name"));
    this.server_info.put("last_login_date_time", user_info.getProperty("login_date_formatted"));
    this.server_info.put("last_login_ip", ip_text);
    Properties p = new Properties();
    p.put("user_name", user_info.getProperty("user_name"));
    p.put("login_date_formatted", user_info.getProperty("login_date_formatted"));
    p.put("ip", ip_text);
    p.put("dns", "");
    synchronized (this.loginsLock) {
      for (int x = siVG("last_logins").size() - 1; x >= 0; x--) {
        try {
          Properties pp = siVG("last_logins").elementAt(x);
          if (pp.getProperty("user_name").equals(p.getProperty("user_name")) && pp.getProperty("ip").equals(p.getProperty("ip"))) {
            Properties ppp = siVG("last_logins").remove(x);
            p.put("dns", ppp.getProperty("dns", ""));
          } 
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {}
      } 
    } 
    if (p.getProperty("dns", "").equals(""))
      try {
        Worker.startWorker(new Runnable(this, p) {
              final ServerStatus this$0;
              
              private final Properties val$p;
              
              public void run() {
                try {
                  this.val$p.put("dns", "");
                  this.val$p.put("dns", InetAddress.getByName(this.val$p.getProperty("ip")).getCanonicalHostName());
                } catch (Exception exception) {}
              }
            });
      } catch (Exception exception) {} 
    siVG("last_logins").addElement(p);
    while (siVG("last_logins").size() > 20)
      siVG("last_logins").remove(0); 
  }
  
  public int count_users(SessionCrush this_user) {
    int num_users = 0;
    for (int x = 0; x < siVG("user_list").size(); x++) {
      try {
        Properties p = siVG("user_list").elementAt(x);
        if (p.getProperty("user_name").equalsIgnoreCase(this_user.uiSG("user_name")))
          num_users++; 
      } catch (Exception exception) {}
    } 
    return num_users;
  }
  
  public static int count_users_ip(SessionCrush this_user, String protocol) {
    int num_users = 0;
    Vector v = (Vector)siVG("user_list").clone();
    for (int x = v.size() - 1; x >= 0; x--) {
      try {
        Properties p = v.elementAt(x);
        if (p.getProperty("user_name").equalsIgnoreCase(this_user.uiSG("user_name")) && p.getProperty("user_ip").equalsIgnoreCase(this_user.uiSG("user_ip")))
          if (protocol == null || (protocol != null && p.getProperty("user_protocol").equals(protocol)))
            num_users++;  
      } catch (Exception exception) {}
    } 
    return num_users;
  }
  
  public boolean kill_first_same_name_same_ip(Properties user_info) {
    Vector v = (Vector)siVG("user_list").clone();
    for (int x = 0; x < v.size(); x++) {
      try {
        Properties p = v.elementAt(x);
        if (p.getProperty("user_name").equalsIgnoreCase(user_info.getProperty("user_name")) && p.getProperty("user_ip").equalsIgnoreCase(user_info.getProperty("user_ip"))) {
          try {
            append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Kicking first same usernames with this IP") + "--- " + user_info.getProperty("user_name") + ":" + user_info.getProperty("user_ip"), "KICK");
          } catch (Exception exception) {}
          kick(p);
          return true;
        } 
      } catch (Exception exception) {}
    } 
    return false;
  }
  
  public boolean kill_same_name_same_ip(Properties user_info, boolean logit) {
    boolean user_kicked = false;
    Vector v = (Vector)siVG("user_list").clone();
    for (int x = v.size() - 1; x >= 0; x--) {
      try {
        Properties p = v.elementAt(x);
        if ((p.getProperty("user_name").equalsIgnoreCase(user_info.getProperty("user_name")) && p.getProperty("user_ip").equalsIgnoreCase(user_info.getProperty("user_ip"))) || p.getProperty("CrushAuth", "1").equalsIgnoreCase(user_info.getProperty("CrushAuth", "2"))) {
          try {
            append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Kicking usernames with this IP") + "--- " + user_info.getProperty("user_name") + ":" + user_info.getProperty("user_ip"), "KICK");
          } catch (Exception exception) {}
          kick(p, logit);
          user_kicked = true;
        } 
      } catch (Exception exception) {}
    } 
    return user_kicked;
  }
  
  public void remove_user(Properties user_info) {
    remove_user(user_info, true);
  }
  
  public void remove_user(Properties user_info, boolean decrementServerCount) {
    SessionCrush session = null;
    boolean removed = false;
    try {
      for (int x = 0; x < this.main_servers.size(); x++) {
        GenericServer s = this.main_servers.elementAt(x);
        if (s.server_item.getProperty("ip").equals(((SessionCrush)user_info.get("session")).server_item.getProperty("ip")) && s.server_item.getProperty("port").equals(((SessionCrush)user_info.get("session")).server_item.getProperty("port")))
          synchronized (s) {
            if (s.connected_users > 0 && decrementServerCount)
              s.connected_users--; 
            s.updateStatus();
          }  
      } 
    } catch (Exception exception) {}
    try {
      session = (SessionCrush)user_info.get("session");
      synchronized (SharedSession.sessionLock) {
        synchronized (this.userListLock) {
          while (siVG("user_list").indexOf(user_info) >= 0)
            removed = siVG("user_list").remove(user_info); 
        } 
      } 
      user_info.put("root_dir", session.user.getProperty("root_dir"));
    } catch (Exception exception) {}
    siPUT("logged_in_users", siVG("user_list").size());
    updateConcurrentUsers();
    try {
      if (removed) {
        try {
          session.drain_log();
        } catch (NullPointerException nullPointerException) {}
        if (LoggingProvider.checkFilters(SG("filter_log_text"), String.valueOf(user_info.getProperty("user_ip")) + ":" + user_info.getProperty("user_name")))
          synchronized (SharedSession.sessionLock) {
            synchronized (this.recentUserListLock) {
              if (siVG("recent_user_list").indexOf(user_info) < 0)
                siVG("recent_user_list").addElement(user_info); 
              while (siVG("recent_user_list").indexOf(user_info) != siVG("recent_user_list").lastIndexOf(user_info))
                siVG("recent_user_list").remove(user_info); 
              user_info.put("root_dir", "/");
            } 
          }  
        try {
          user_info.put("root_dir", session.user.getProperty("root_dir"));
        } catch (NullPointerException nullPointerException) {}
      } 
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
    user_info.put("user_log", new Vector());
  }
  
  public int getTotalConnectedUsers() {
    int total = 0;
    try {
      total = siIG("concurrent_users");
    } catch (Exception exception) {}
    return total;
  }
  
  public void stop_all_servers() {
    for (int x = this.main_servers.size() - 1; x >= 0; x--) {
      GenericServer gs = this.main_servers.elementAt(x);
      if (!(gs instanceof ServerBeat))
        stop_this_server(x); 
    } 
  }
  
  public void kick_all_users() {
    try {
      append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Kicking all users.") + "---", "KICK");
    } catch (Exception exception) {}
    for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
      Properties p = siVG("user_list").elementAt(x);
      kick(p);
    } 
    siPUT("logged_in_users", "0");
    updateConcurrentUsers();
  }
  
  public void start_all_servers() {
    try {
      Vector the_server_list = null;
      try {
        the_server_list = (Vector)server_settings.get("server_list");
      } catch (Exception e) {
        the_server_list = (Vector)this.default_settings.get("server_list");
        server_settings.put("server_list", the_server_list);
      } 
      for (int x = 0; x < the_server_list.size(); x++)
        start_this_server(x); 
      setup_discover_ip_refresh();
    } catch (Exception ee) {
      Log.log("SERVER", 0, ee);
    } 
  }
  
  public void doServerAction(Properties p) {
    Vector the_server_list = (Vector)server_settings.get("server_list");
    if (p.getProperty("action").equals("create")) {
      Properties server_item = new Properties();
      server_item.put("serverType", p.getProperty("protocol").toUpperCase());
      server_item.put("ip", p.getProperty("ip", "lookup"));
      server_item.put("port", p.getProperty("port"));
      server_item.put("require_encryption", "false");
      server_item.put("https_redirect", "false");
      server_item.put("explicit_ssl", "false");
      server_item.put("explicit_tls", "false");
      server_item.put("http", "false");
      server_item.put("server_ip", "auto");
      server_item.put("pasv_ports", "1025-65535");
      server_item.put("ftp_aware_router", "false");
      if (p.containsKey("port_id"))
        server_item.put("port_id", p.getProperty("port_id")); 
      this.common_code.setServerStatus(server_item, server_item.getProperty("ip", "lookup"));
      the_server_list.addElement(server_item);
      thisObj.save_server_settings(true);
      p.put("server_item", server_item);
      return;
    } 
    Log.log("SERVER", 3, "the_server_list:" + the_server_list);
    for (int x = 0; x < the_server_list.size(); x++) {
      Log.log("SERVER", 3, "ServerAction:" + p.toString());
      Properties pp = the_server_list.elementAt(x);
      Log.log("SERVER", 3, "ServerAction2:" + pp.toString());
      if (pp.getProperty("port_id", "a").equals(p.getProperty("port_id", "b")) || (pp.getProperty("serverType", "a").equalsIgnoreCase(p.getProperty("protocol", "b")) && pp.getProperty("ip", "lookup").equalsIgnoreCase(p.getProperty("ip", "lookup")) && pp.getProperty("port", "-3").equalsIgnoreCase(p.getProperty("port", "-2")))) {
        if (p.getProperty("action").equals("start")) {
          Log.log("SERVER", 2, "starting server:" + pp);
          Thread t = start_this_server(x);
          p.put("thread", t);
          thisObj.save_server_settings(true);
          break;
        } 
        if (p.getProperty("action").equals("stop")) {
          Log.log("SERVER", 2, "stopping server:" + pp);
          Thread t = stop_this_server(x);
          p.put("thread", t);
          thisObj.save_server_settings(true);
          break;
        } 
        if (p.getProperty("action").equals("query")) {
          p.put("server_item", pp);
          break;
        } 
        if (p.getProperty("action").equals("delete")) {
          try {
            Thread t = stop_this_server(x);
            p.put("thread", t);
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
          } 
          the_server_list.removeElement(pp);
          this.main_servers.removeElementAt(x);
          thisObj.save_server_settings(true);
          break;
        } 
      } 
    } 
  }
  
  public Thread start_this_server(int x) {
    Vector the_server_list = (Vector)server_settings.get("server_list");
    Properties server_item = the_server_list.elementAt(x);
    if (this.main_servers.size() - 1 < x) {
      this.main_servers.addElement(GenericServer.buildServer(server_item));
    } else {
      GenericServer gs = this.main_servers.elementAt(x);
      if (gs instanceof ServerBeat && gs.thread != null && gs.thread.isAlive())
        return gs.thread; 
      try {
        stop_this_server(x);
      } catch (Exception exception) {}
      this.main_servers.setElementAt(GenericServer.buildServer(server_item), x);
    } 
    Thread t = new Thread(this.main_servers.elementAt(x));
    t.setName("main_server_thread:" + server_item.getProperty("ip") + ":" + Integer.parseInt(server_item.getProperty("port")));
    t.setPriority(10);
    t.start();
    return t;
  }
  
  public Thread stop_this_server(int x) {
    String ip = ((GenericServer)this.main_servers.elementAt(x)).listen_ip;
    try {
      if (ip.equals("lookup"))
        ip = Common.getLocalIP(); 
    } catch (Exception exception) {}
    try {
      append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Server Stopped") + "--- LAN IP=" + ip + " WAN IP=" + SG("discovered_ip") + " PORT=" + ((GenericServer)this.main_servers.elementAt(x)).listen_port, "STOP");
    } catch (Exception exception) {}
    try {
      ((GenericServer)this.main_servers.elementAt(x)).die_now.append(System.currentTimeMillis());
    } catch (Exception exception) {}
    try {
      ((GenericServer)this.main_servers.elementAt(x)).thread.interrupt();
    } catch (Exception exception) {}
    try {
      ((GenericServer)this.main_servers.elementAt(x)).server_sock.close();
    } catch (Exception exception) {}
    try {
      return ((GenericServer)this.main_servers.elementAt(x)).thread;
    } catch (Exception exception) {
      return null;
    } 
  }
  
  public void server_started(String ip, int the_port) {
    try {
      if (ip.equals("lookup"))
        ip = InetAddress.getLocalHost().getHostAddress(); 
    } catch (Exception exception) {}
    this.logDateFormat = new SimpleDateFormat(SG("log_date_format"), Locale.US);
    if (SG("discovered_ip").equals("0.0.0.0"))
      try {
        update_now("discover_ip_timer");
      } catch (Exception exception) {} 
    if (SG("discovered_ip").equals("0.0.0.0"))
      try {
        server_settings.put("discovered_ip", InetAddress.getLocalHost().getHostAddress());
      } catch (Exception exception) {} 
    try {
      append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Server Started") + "--- LAN IP=" + ip + " WAN IP=" + SG("discovered_ip") + " PORT=" + the_port, "START");
    } catch (Exception exception) {}
  }
  
  public void update_now(String arg) throws Exception {
    if (arg.equals("hammer_timer")) {
      siPUT("hammer_history", "");
    } else if (arg.equals("hammer_timer_http")) {
      siPUT("hammer_history_http", "");
    } else if (arg.equals("phammer_timer")) {
      Properties ips = new Properties();
      int x;
      for (x = 0; x < siVG("user_list").size(); x++) {
        try {
          Properties user_info = siVG("user_list").elementAt(x);
          Vector password_attempts = (Vector)user_info.get("password_attempts");
          if (ips.get(user_info.getProperty("user_ip")) == null) {
            ips.put(user_info.getProperty("user_ip"), new Vector());
            ((Vector)ips.get(user_info.getProperty("user_ip"))).add(user_info);
          } 
          ((Vector)ips.get(user_info.getProperty("user_ip"))).addAll(password_attempts);
        } catch (Exception exception) {}
      } 
      for (x = 0; x < siVG("recent_user_list").size(); x++) {
        try {
          Properties user_info = siVG("recent_user_list").elementAt(x);
          Vector password_attempts = (Vector)user_info.get("password_attempts");
          if (ips.get(user_info.getProperty("user_ip")) == null) {
            ips.put(user_info.getProperty("user_ip"), new Vector());
            ((Vector)ips.get(user_info.getProperty("user_ip"))).add(user_info);
          } 
          ((Vector)ips.get(user_info.getProperty("user_ip"))).addAll(password_attempts);
        } catch (Exception exception) {}
      } 
      Enumeration keys = ips.keys();
      while (keys.hasMoreElements()) {
        String ip = keys.nextElement().toString();
        Vector password_attempts2 = (Vector)ips.get(ip);
        int count = 0;
        for (int i = 1; i < password_attempts2.size(); i++) {
          long time = Long.parseLong(password_attempts2.elementAt(i).toString());
          if (time > (new Date()).getTime() - (1000 * IG("phammer_banning")))
            count++; 
        } 
        if (count > IG("phammer_attempts"))
          if (ban((Properties)password_attempts2.elementAt(0), IG("pban_timeout"), true, "password attempts")) {
            try {
              append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Kicking session because of password hammer trigger.") + "---", "KICK");
            } catch (Exception exception) {}
            Properties user_info = (Properties)password_attempts2.elementAt(0);
            SessionCrush thisSession = null;
            try {
              thisSession = (SessionCrush)user_info.get("session");
            } catch (Exception e) {
              Log.log("BAN", 1, e);
            } 
            kick(user_info);
            try {
              Properties user = null;
              if (thisSession != null)
                user = thisSession.user; 
              runAlerts("ip_banned_logins", user, user_info, thisSession);
            } catch (Exception e) {
              Log.log("BAN", 1, e);
            } 
            try {
              Properties info = new Properties();
              info.put("alert_type", "hammering");
              info.put("alert_sub_type", "password");
              info.put("alert_timeout", (new StringBuffer(String.valueOf(IG("pban_timeout")))).toString());
              info.put("alert_max", (new StringBuffer(String.valueOf(IG("phammer_attempts")))).toString());
              info.put("alert_msg", user_info.getProperty("usern_ame"));
              runAlerts("security_alert", info, user_info, thisSession);
            } catch (Exception e) {
              Log.log("BAN", 1, e);
            } 
            int j;
            for (j = 0; j < siVG("user_list").size(); j++) {
              try {
                Properties p = siVG("user_list").elementAt(j);
                if (p.getProperty("user_ip").equals(ip))
                  ((Vector)p.get("password_attempts")).removeAllElements(); 
              } catch (Exception exception) {}
            } 
            for (j = 0; j < siVG("recent_user_list").size(); j++) {
              try {
                Properties p = siVG("recent_user_list").elementAt(j);
                if (p.getProperty("user_ip").equals(ip))
                  ((Vector)p.get("password_attempts")).removeAllElements(); 
              } catch (Exception exception) {}
            } 
          }  
      } 
      runAlerts("user_hammering", null);
    } else if (arg.equals("report_scheduler")) {
      this.rt.runScheduledReports(server_settings, this.server_info);
    } else if (arg.equals("schedules")) {
      try {
        String last_m = this.server_info.getProperty("job_scheduler_last_run_mm", "");
        String current_m = this.mm.format(new Date());
        if (!last_m.equals(current_m)) {
          this.server_info.put("job_scheduler_last_run_mm", current_m);
          Thread.sleep(3000L);
          JobScheduler.runSchedules(new Properties());
        } 
      } catch (Exception e) {
        System.out.println((String)new Date());
        e.printStackTrace();
        Log.log("SERVER", 0, e);
      } 
    } else if (arg.equals("alerts")) {
      runAlerts("disk", null);
      runAlerts("variables", null);
    } else if (arg.equals("new_version")) {
      if (SG("newversion") == null || BG("newversion")) {
        try {
          Thread.sleep(1000L);
        } catch (Exception exception) {}
        doCheckForUpdate(false);
        try {
          Thread.sleep(259200000L);
        } catch (Exception exception) {}
      } 
    } else if (arg.equals("stats_saver")) {
      Thread.sleep(10000L);
      synchronized (this.stats_saver_lock) {
        long last_stats_time = Long.parseLong(this.server_info.getProperty("last_stats_time", "0"));
        if (System.currentTimeMillis() - last_stats_time > 30000L) {
          if (ServerBeat.current_master) {
            Calendar c = new GregorianCalendar();
            c.setTime(new Date());
            c.add(5, IG("stats_transfer_days") * -1);
            Vector transfer_rids = this.statTools.executeSqlQuery(SG("stats_get_transfers_time"), new Object[] { c.getTime() }, false);
            Log.log("STATISTICS", 2, "Stats Transfer Cleanup: Deleting " + transfer_rids.size() + " sessions.");
            StringBuffer transferRidsStr = new StringBuffer();
            for (int x = 0; x < transfer_rids.size(); x++) {
              Properties p = transfer_rids.elementAt(x);
              if (x > 0)
                transferRidsStr.append(","); 
              transferRidsStr.append(p.getProperty("RID"));
            } 
            if (transferRidsStr.length() > 0) {
              String deleteMetaInfoSql = "DELETE FROM META_INFO WHERE TRANSFER_RID IN (" + transferRidsStr.toString() + ")";
              this.statTools.executeSql(deleteMetaInfoSql, new Object[0]);
            } 
            this.statTools.executeSql(SG("stats_delete_transfers_time"), new Object[] { c.getTime() });
            c = new GregorianCalendar();
            c.setTime(new Date());
            c.add(5, IG("stats_session_days") * -1);
            Vector session_rids = this.statTools.executeSqlQuery(SG("stats_get_sessions_time"), new Object[] { c.getTime() }, false);
            Log.log("STATISTICS", 2, "Stats Session Cleanup: Deleting " + session_rids.size() + " sessions.");
            StringBuffer sessionRidsStr = new StringBuffer();
            int i;
            for (i = 0; i < session_rids.size(); i++) {
              Properties p = session_rids.elementAt(i);
              if (i > 0)
                sessionRidsStr.append(","); 
              sessionRidsStr.append(p.getProperty("RID"));
            } 
            transfer_rids = new Vector();
            if (sessionRidsStr.length() > 0)
              transfer_rids = this.statTools.executeSqlQuery(Common.replace_str(SG("stats_get_transfers_sessions"), "%sessions%", sessionRidsStr.toString()), new Object[0], false); 
            transferRidsStr.setLength(0);
            for (i = 0; i < transfer_rids.size(); i++) {
              Properties p = transfer_rids.elementAt(i);
              if (i > 0)
                transferRidsStr.append(","); 
              transferRidsStr.append(p.getProperty("RID"));
            } 
            if (transferRidsStr.length() > 0)
              this.statTools.executeSql(Common.replace_str(SG("stats_delete_meta_transfers"), "%transfers%", transferRidsStr.toString()), new Object[0]); 
            this.statTools.executeSql(SG("stats_delete_sessions_time"), new Object[] { c.getTime() });
          } 
          checkCrushExpiration();
          if (BG("allow_session_caching"))
            SharedSession.flush(); 
          this.server_info.put("last_stats_time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        } 
      } 
    } else if (arg.equals("ban_timer")) {
      Vector ip_vec = (Vector)server_settings.get("ip_restrictions");
      this.common_code.remove_expired_bans(ip_vec);
      server_settings.put("ip_restrictions", ip_vec);
    } else if (arg.equals("cban_timer")) {
      Vector kick_list = new Vector();
      int x = 0;
      while (x < siVG("user_list").size()) {
        try {
          Properties user_info = siVG("user_list").elementAt(x);
          if (user_info != null) {
            int search_loc = -1;
            Vector ip_list = (Vector)server_settings.get("ip_restrictions");
            for (int loop = 0; loop < ip_list.size(); loop++) {
              Properties ip_data = ip_list.elementAt(loop);
              if ((String.valueOf(ip_data.getProperty("start_ip")) + "," + ip_data.getProperty("stop_ip")).equals(user_info.get("user_ip") + "," + user_info.get("user_ip"))) {
                search_loc = loop;
                break;
              } 
            } 
            if (search_loc < 0) {
              long time_now = (new Date()).getTime();
              int xx = 0;
              while (xx < ((Vector)user_info.get("failed_commands")).size()) {
                long the_time = Long.parseLong(((Vector)user_info.get("failed_commands")).elementAt(xx));
                if (time_now - the_time > (IG("chammer_banning") * 1000)) {
                  ((Vector)user_info.get("failed_commands")).removeElementAt(xx);
                  continue;
                } 
                xx++;
              } 
              if (((Vector)user_info.get("failed_commands")).size() >= IG("chammer_attempts")) {
                String ip = user_info.getProperty("user_ip");
                if (ban_ip(ip, IG("cban_timeout"), false, "failed commands")) {
                  ((Vector)user_info.get("failed_commands")).removeAllElements();
                  try {
                    append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---IP " + LOC.G("Banned") + "---:" + ip, "BAN");
                  } catch (Exception exception) {}
                  kick_list.addElement(user_info);
                } 
                continue;
              } 
              x++;
              continue;
            } 
            x++;
            continue;
          } 
          x++;
        } catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {}
      } 
      for (int xxx = 0; xxx < kick_list.size(); xxx++) {
        try {
          append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Kicking sessions because of too many failed commands.") + "---", "KICK");
        } catch (Exception exception) {}
        Properties user_info = kick_list.elementAt(xxx);
        kick(user_info);
        try {
          Properties info = new Properties();
          info.put("alert_type", "hammering");
          info.put("alert_sub_type", "command");
          info.put("alert_timeout", (new StringBuffer(String.valueOf(IG("cban_timeout")))).toString());
          info.put("alert_max", (new StringBuffer(String.valueOf(IG("chammer_attempts")))).toString());
          info.put("alert_msg", user_info.getProperty("user_name"));
          runAlerts("security_alert", info, user_info, null);
        } catch (Exception e) {
          Log.log("BAN", 1, e);
        } 
      } 
      Thread.sleep(5000L);
    } else if (arg.equals("discover_ip_timer")) {
      if (BG("auto_ip_discovery"))
        update_ip(); 
      Thread.sleep(20000L);
    } else if (arg.equals("update_2_timer")) {
      try {
        if (this.prefsProvider.getPrefsTime(null) != siLG("currentFileDate")) {
          Thread.sleep(2000L);
          synchronized (GenericServer.updateServerStatuses) {
            Properties previousObject = server_settings;
            Vector pref_server_items = (Vector)server_settings.get("server_list");
            String prevServeritemsStr = "";
            for (int x = 0; x < pref_server_items.size(); x++) {
              Properties the_server = (Properties)((Properties)pref_server_items.elementAt(x)).clone();
              prevServeritemsStr = GenericServer.getPropertiesHash(the_server);
            } 
            prevServeritemsStr = Common.replace_str(prevServeritemsStr, "null", "");
            init_setup(false);
            Common.updateObject(server_settings, previousObject);
            pref_server_items = (Vector)server_settings.get("server_list");
            String newServerItemsStr = "";
            for (int i = 0; i < pref_server_items.size(); i++) {
              Properties the_server = (Properties)((Properties)pref_server_items.elementAt(i)).clone();
              newServerItemsStr = GenericServer.getPropertiesHash(the_server);
            } 
            newServerItemsStr = Common.replace_str(newServerItemsStr, "null", "");
            boolean doServerBounce = false;
            if (!newServerItemsStr.equals(prevServeritemsStr))
              doServerBounce = true; 
            if (doServerBounce)
              stop_all_servers(); 
            server_settings = previousObject;
            pref_server_items = (Vector)server_settings.get("server_list");
            for (int j = 0; j < pref_server_items.size(); j++) {
              Properties the_server = pref_server_items.elementAt(j);
              if (the_server.containsKey("encryptKeystorePasswords")) {
                the_server.remove("encryptKeystorePasswords");
                the_server.put("customKeystorePass", this.common_code.encode_pass(the_server.getProperty("customKeystorePass"), "DES", ""));
                the_server.put("customKeystoreCertPass", this.common_code.encode_pass(the_server.getProperty("customKeystoreCertPass"), "DES", ""));
              } 
            } 
            setup_hammer_banning();
            setup_ban_timer();
            setup_discover_ip_refresh();
            setup_log_rolling();
            setup_http_cleaner();
            setup_stats_saver();
            setup_jobs_resumer();
            setup_report_scheduler();
            if (doServerBounce)
              start_all_servers(); 
            this.server_info.put("currentFileDate", (new StringBuffer(String.valueOf(this.prefsProvider.getPrefsTime(null)))).toString());
            setupGlobalPrefs();
          } 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 0, "Prefs.XML was corrupt again.  Could not read changes made...");
        Log.log("SERVER", 0, e);
      } 
      if (siBG("refresh_users")) {
        for (int x = 0; x < siVG("user_list").size(); x++) {
          try {
            Properties p = siVG("user_list").elementAt(x);
            p.put("refresh_user", "true");
          } catch (Exception exception) {}
        } 
        siPUT("refresh_users", "false");
      } 
      if (siOG("waiting_quit_user_name") != null)
        try {
          if (siVG("user_list").indexOf(siOG("waiting_quit_user_name")) < 0)
            quit_server(); 
        } catch (Exception exception) {} 
      if (siOG("waiting_restart_user_name") != null)
        try {
          if (siVG("user_list").indexOf(siOG("waiting_restart_user_name")) < 0)
            restart_crushftp(); 
        } catch (Exception exception) {} 
      setupGlobalPrefs();
      if (this.loggingProvider1 != null)
        this.loggingProvider1.checkLogPath(); 
      if (this.loggingProvider2 != null)
        this.loggingProvider2.checkLogPath(); 
      if (!this.server_info.containsKey("last_expired_accounts_check"))
        this.server_info.put("last_expired_accounts_check", (new StringBuffer(String.valueOf(System.currentTimeMillis() - 60000L))).toString()); 
      if (Long.parseLong(this.server_info.getProperty("last_expired_accounts_check")) < System.currentTimeMillis() - 3600000L) {
        this.server_info.put("last_expired_accounts_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        Worker.startWorker(new Runnable(this) {
              final ServerStatus this$0;
              
              public void run() {
                Log.log("USER_OBJ", 2, "Checking for expired accounts...");
                String username = "";
                try {
                  Vector sgs = (Vector)ServerStatus.server_settings.get("server_groups");
                  for (int x = 0; x < sgs.size(); x++) {
                    String serverGroup = sgs.elementAt(x).toString();
                    Log.log("USER_OBJ", 2, "Checking for expired accounts:" + serverGroup);
                    Vector v = new Vector();
                    UserTools.refreshUserList(serverGroup, v);
                    for (int xx = 0; xx < v.size(); xx++) {
                      this.this$0.server_info.put("last_expired_accounts_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
                      username = v.elementAt(xx).toString();
                      try {
                        Properties user = UserTools.ut.getUser(serverGroup, username, false);
                        if (user != null && user.getProperty("account_expire_delete", "").equals("true") && this.this$0.common_code.check_date_expired_roll(user.getProperty("account_expire")))
                          if (user.getProperty("account_expire_rolling_days", "").equals("") || user.getProperty("account_expire_rolling_days", "").equals("0") || user.getProperty("account_expire_rolling_days", "").equals("account_expire_rolling_days") || Integer.parseInt(user.getProperty("account_expire_rolling_days")) < 0) {
                            Log.log("USER_OBJ", 0, "Skipping delete of expired account:" + serverGroup + "/" + username + " because its a template with a negative expire days.");
                          } else {
                            Log.log("USER_OBJ", 0, "Deleting expired account:" + serverGroup + "/" + username);
                            UserTools.expireUserVFSTask(user, serverGroup, username);
                            Log.log("USER_OBJ", 0, "Removing account:" + serverGroup + "/" + username);
                            UserTools.deleteUser(serverGroup, username);
                          }  
                        Thread.sleep(10L);
                      } catch (Exception e) {
                        Log.log("USER_OBJ", 1, "Checking " + username + " for expiration...error:" + e.toString());
                        Log.log("USER_OBJ", 1, e);
                      } 
                    } 
                  } 
                } catch (Exception e) {
                  Log.log("USER_OBJ", 1, "Checking " + username + " for expiration...error:" + e.toString());
                  Log.log("USER_OBJ", 1, e);
                } 
                Log.log("USER_OBJ", 2, "Checking for expired accounts...done.");
                this.this$0.server_info.put("last_expired_accounts_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
              }
            });
      } 
      this.server_info.put("memcache_objects", (new StringBuffer(String.valueOf(FileClient.dirCachePerm.size()))).toString());
      if (!this.server_info.containsKey("last_search_index_interval")) {
        this.server_info.put("last_search_index_interval", "0");
        Thread.sleep(1000L);
      } 
      if (IG("search_index_interval") > 0 && Long.parseLong(this.server_info.getProperty("last_search_index_interval")) < System.currentTimeMillis() - 60000L * LG("search_index_interval")) {
        this.server_info.put("last_search_index_interval", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        Worker.startWorker(new Runnable(this) {
              final ServerStatus this$0;
              
              public void run() {
                try {
                  String[] usernames = ServerStatus.SG("search_index_usernames").split(",");
                  for (int x = 0; x < usernames.length; x++) {
                    if (!usernames[x].trim().equals("")) {
                      Vector server_groups = (Vector)ServerStatus.server_settings.get("server_groups");
                      for (int xx = 0; xx < server_groups.size(); xx++) {
                        VFS uVFS = UserTools.ut.getVFS(server_groups.elementAt(xx).toString(), usernames[x].trim());
                        Properties pp = uVFS.get_item("/");
                        SearchHandler.buildEntry(pp, uVFS, false, false);
                        uVFS.disconnect();
                        uVFS.free();
                        this.this$0.server_info.put("last_search_index_interval", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
                      } 
                    } 
                  } 
                } catch (Exception e) {
                  Log.log("SEARCH", 0, e);
                } 
                this.this$0.server_info.put("last_search_index_interval", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
              }
            });
      } 
      if (!this.server_info.containsKey("last_expired_sync_check"))
        this.server_info.put("last_expired_sync_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()); 
      if (Long.parseLong(this.server_info.getProperty("last_expired_sync_check")) < System.currentTimeMillis() - 3600000L) {
        this.server_info.put("last_expired_sync_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        if (IG("sync_history_days") > 0)
          Worker.startWorker(new Runnable(this) {
                final ServerStatus this$0;
                
                public void run() {
                  try {
                    Calendar c = new GregorianCalendar();
                    c.setTime(new Date());
                    c.add(5, ServerStatus.IG("sync_history_days") * -1);
                    SyncTools.purgeExpired(c.getTime().getTime());
                  } catch (Exception e) {
                    Log.log("SYNC", 0, e);
                  } 
                  this.this$0.server_info.put("last_expired_sync_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
                }
              }); 
      } 
      if (!this.server_info.containsKey("last_memory_check"))
        this.server_info.put("last_memory_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()); 
      if (Long.parseLong(this.server_info.getProperty("last_memory_check")) < System.currentTimeMillis() - 1000L * LG("memory_log_interval")) {
        this.server_info.put("last_memory_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        Log.log("SERVER", 0, "Server Memory Stats: Max=" + Common.format_bytes_short(siLG("ram_max")) + ", Free=" + Common.format_bytes_short(siLG("ram_free")) + ", Threads:" + Worker.busyWorkers.size() + "/" + Worker.max_busy_workers);
      } 
      if (!this.server_info.containsKey("last_dump_threads"))
        this.server_info.put("last_dump_threads", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()); 
      if (LG("dump_threads_log_interval") > 0L && Long.parseLong(this.server_info.getProperty("last_dump_threads")) < System.currentTimeMillis() - 1000L * LG("dump_threads_log_interval")) {
        this.server_info.put("last_dump_threads", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        System.out.println(new Date() + "THREAD DUMP");
        System.out.println(Common.dumpStack(String.valueOf(version_info_str) + sub_version_info_str));
      } 
      Worker.startWorker(new Runnable(this) {
            final ServerStatus this$0;
            
            public void run() {
              Vector alert_tmp_f = new Vector();
              synchronized (Common.System2.get("alerts_queue")) {
                alert_tmp_f.addAll((Vector)Common.System2.get("alerts_queue"));
                ((Vector)Common.System2.get("alerts_queue")).clear();
              } 
              while (alert_tmp_f.size() > 0) {
                Properties alert = alert_tmp_f.remove(0);
                ServerStatus.thisObj.runAlerts(alert.getProperty("msg", ""), (SessionCrush)alert.get("session"));
              } 
            }
          });
    } else if (arg.equals("vfs_replication_pinger")) {
      if (!this.server_info.containsKey("replicated_vfs_ping_interval"))
        this.server_info.put("replicated_vfs_ping_interval", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()); 
      if (LG("replicated_vfs_ping_interval") > 0L && Long.parseLong(this.server_info.getProperty("replicated_vfs_ping_interval")) < System.currentTimeMillis() - 1000L * LG("replicated_vfs_ping_interval")) {
        this.server_info.put("replicated_vfs_ping_interval", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        if (!SG("replicated_vfs_root_url").equals("") && BG("v8_beta"))
          try {
            Properties vItem = new Properties();
            Properties virtual = new Properties();
            VFS uVFS = VFS.getVFS(virtual);
            vItem.put("url", SG("replicated_vfs_root_url"));
            vItem.put("type", "DIR");
            Properties item = new Properties();
            item.put("vItem", vItem);
            Vector vItems = new Vector();
            Vector clients = new Vector();
            clients.addElement(new MemoryClient("MEMORY:///", "", null));
            vItems.addElement(vItem);
            uVFS.addReplicatedVFSAndClient(item, vItems, clients, true);
            (new GenericClientMulti("PROXY", Common.log, vItem, vItems, clients, true)).close();
            uVFS.disconnect();
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
          }  
        this.server_info.put("replicated_vfs_ping_interval", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      } 
    } else if (arg.equals("http_cleaner")) {
      long sessions = 0L;
      long http_keys = 0L;
      long http_keys_expired1 = 0L;
      long http_keys_expired2 = 0L;
      long http_activity_keys = 0L;
      long http_activity_keys_expired = 0L;
      try {
        Enumeration keys = SharedSession.find("crushftp.sessions").keys();
        while (keys.hasMoreElements()) {
          String id = keys.nextElement().toString();
          http_activity_keys++;
          Object o = SharedSession.find("crushftp.sessions").get(id);
          long time = 0L;
          long timeout = 60L * LG("http_session_timeout");
          if (o instanceof SessionCrush) {
            time = Long.parseLong(((SessionCrush)o).getProperty("last_activity", "0"));
            if (((SessionCrush)o).user != null) {
              long timeout2 = Long.parseLong(((SessionCrush)o).user.getProperty("max_idle_time", "10"));
              if (timeout2 < 0L) {
                timeout = timeout2 * -1L;
              } else if (timeout2 != 0L && timeout2 < timeout) {
                timeout = 60L * timeout2;
              } 
            } 
          } 
          if ((new Date()).getTime() - time > 1000L * timeout) {
            boolean allow_removal = true;
            Enumeration e = SharedSession.find("crushftp.usernames").keys();
            while (e.hasMoreElements() && allow_removal) {
              String key2 = e.nextElement().toString();
              if (key2.indexOf("_" + id + "_") >= 0) {
                Enumeration enumeration = ServerSessionTunnel3.running_tunnels.keys();
                while (enumeration.hasMoreElements() && allow_removal) {
                  String tunnel_id = enumeration.nextElement().toString();
                  if (tunnel_id.startsWith(String.valueOf(key2) + "_")) {
                    StreamController sc = (StreamController)ServerSessionTunnel3.running_tunnels.get(tunnel_id);
                    if (System.currentTimeMillis() - sc.lastActivity > 60000L) {
                      Log.log("TUNNEL", 0, "Current tunnel ID list:" + ServerSessionTunnel3.running_tunnels);
                      Log.log("TUNNEL", 0, "Tunnel is dead and the session has timed out, closing it:" + tunnel_id + " inactive time:" + (System.currentTimeMillis() - sc.lastActivity));
                      sc.startStopTunnel(false);
                      continue;
                    } 
                    allow_removal = false;
                  } 
                } 
                http_keys_expired1++;
                SharedSession.find("crushftp.usernames").remove(key2);
                Tunnel2.stopTunnel(key2);
                if (o instanceof SessionCrush && ((SessionCrush)o).uVFS != null)
                  ((SessionCrush)o).uVFS.disconnect(); 
              } 
            } 
            if (allow_removal) {
              http_activity_keys_expired++;
              if (o instanceof SessionCrush)
                remove_user(((SessionCrush)o).user_info); 
              SharedSession.find("crushftp.sessions").remove(id);
            } 
          } 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      try {
        long timeout = 60L * LG("http_session_timeout");
        Vector current_users = siVG("user_list");
        for (int x = current_users.size() - 1; x >= 0; x--) {
          Properties user_info = current_users.elementAt(x);
          SessionCrush thisSession = (SessionCrush)user_info.get("session");
          if (thisSession == null || SharedSession.find("crushftp.sessions").get(thisSession.getId()) == null)
            if ((System.currentTimeMillis() - Long.parseLong(user_info.getProperty("last_activity", "0"))) / 1000L > timeout)
              remove_user(user_info);  
        } 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      try {
        Enumeration e = SharedSession.find("crushftp.usernames").keys();
        while (e.hasMoreElements()) {
          String key2 = e.nextElement().toString();
          http_keys++;
          String id = key2.substring(key2.indexOf("_") + 1, key2.lastIndexOf("_"));
          if (!SharedSession.find("crushftp.sessions").containsKey(id)) {
            http_keys_expired2++;
            SharedSession.find("crushftp.usernames").remove(key2);
          } 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      Log.log("SERVER", 1, "Cleaning up sessions:" + sessions + " sessions tracked, " + http_activity_keys + " activity items tracked, " + http_keys + " sessions tracked, " + http_activity_keys_expired + " activities expired, " + http_keys_expired1 + " sessions expired in first pass, and " + http_keys_expired2 + " expired in second pass.");
      try {
        Properties resetTokens = siPG("resetTokens");
        if (resetTokens == null)
          resetTokens = new Properties(); 
        thisObj.server_info.put("resetTokens", resetTokens);
        Enumeration e = resetTokens.keys();
        while (e.hasMoreElements()) {
          String key2 = e.nextElement().toString();
          Properties reset = (Properties)resetTokens.get(key2);
          long generated = Long.parseLong(reset.getProperty("generated"));
          if (System.currentTimeMillis() > generated + (60000 * IG("reset_token_timeout")))
            resetTokens.remove(key2); 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      Enumeration tunnel_keys = ServerSessionTunnel3.running_tunnels.keys();
      while (tunnel_keys.hasMoreElements()) {
        String tunnel_id = tunnel_keys.nextElement().toString();
        StreamController sc = (StreamController)ServerSessionTunnel3.running_tunnels.get(tunnel_id);
        if (System.currentTimeMillis() - sc.lastActivity > 60000L) {
          Log.log("TUNNEL", 0, "Current tunnel ID list:" + ServerSessionTunnel3.running_tunnels);
          Log.log("TUNNEL", 0, "Tunnel is dead, closing it:" + tunnel_id + " inactive time:" + (System.currentTimeMillis() - sc.lastActivity));
          ServerSessionTunnel3.running_tunnels.remove(tunnel_id);
          sc.startStopTunnel(false);
        } 
      } 
      try {
        Vector current_users = siVG("user_list");
        for (int x = current_users.size() - 1; x >= 0; x--) {
          Properties user_info = current_users.elementAt(x);
          SessionCrush thisSession = (SessionCrush)user_info.get("session");
          if (thisSession != null && thisSession.uVFS != null && thisSession.uVFS.cacheList != null) {
            Properties cache = thisSession.uVFS.cacheList;
            Enumeration keys = cache.keys();
            while (keys.hasMoreElements()) {
              String key = (String)keys.nextElement();
              Object o2 = cache.get(key);
              if (o2 instanceof Properties) {
                Properties h = (Properties)o2;
                if (h != null && System.currentTimeMillis() - Long.parseLong(h.getProperty("time")) > 60000L) {
                  cache.remove(key);
                  cache.remove(String.valueOf(key) + "...count");
                } 
              } 
            } 
          } 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 2, e);
      } 
    } else if (arg.equals("events_thread")) {
      synchronized (this.eventLock) {
        try {
          this.events6.checkEventsNow();
        } catch (Exception e) {
          Log.log("EVENT", 0, e);
        } 
      } 
    } else if (arg.equals("log_rolling")) {
      Log.log("SERVER", 3, "Log Rolling:Checking is log rolling enabled? roll_log=" + BG("roll_log"));
      if (BG("roll_log") && this.loggingProvider1 != null)
        this.loggingProvider1.checkForLogRoll(); 
      if (BG("roll_log") && this.loggingProvider2 != null)
        this.loggingProvider2.checkForLogRoll(); 
      Thread.sleep(5000L);
      String job_log_path = String.valueOf((new File(change_vars_to_values_static(SG("log_location"), null, null, null))).getCanonicalFile().getParentFile().getPath()) + "/logs/jobs/";
      File logFiles = new File(job_log_path);
      logFiles.mkdirs();
      if (!this.server_info.containsKey("last_expired_log_check_session"))
        this.server_info.put("last_expired_log_check_session", (new StringBuffer(String.valueOf(System.currentTimeMillis() - 60000L))).toString()); 
      if (Long.parseLong(this.server_info.getProperty("last_expired_log_check_session")) < System.currentTimeMillis() - 600000L) {
        this.server_info.put("last_expired_log_check_session", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        Worker.startWorker(new Runnable(this) {
              final ServerStatus this$0;
              
              public void run() {
                try {
                  Log.log("SERVER", 0, "Checking for session logs to clean...");
                  File[] logs = (new File(String.valueOf((new File(ServerStatus.change_vars_to_values_static(ServerStatus.SG("user_log_location"), null, null, null))).getCanonicalFile().getParentFile().getPath()) + "/session_logs/")).listFiles();
                  Log.log("SERVER", 0, "Checking for session logs to clean...found " + logs.length + " log files, checking for expired logs...");
                  if (logs.length > 100000 && ServerStatus.BG("write_session_logs")) {
                    Log.log("SERVER", 0, "WARNING!!!  Session logging has been turned off due to too many individual session logs accumulating in the server logs/session_logs/ folder.  Session logging was disabled to protect your server from runnign out of memory trying to cleanup a folder with too many items..");
                    ServerStatus.server_settings.put("write_session_logs", "false");
                  } 
                  int cleaned = 0;
                  for (int x = 0; logs != null && x < logs.length; x++) {
                    if (logs[x].isFile() && logs[x].getName().toUpperCase().endsWith(".LOG") && System.currentTimeMillis() - logs[x].lastModified() > 86400000L * ServerStatus.LG("recent_user_log_days")) {
                      logs[x].delete();
                      cleaned++;
                    } 
                    this.this$0.server_info.put("last_expired_log_check_session", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
                  } 
                  Log.log("SERVER", 0, "Checking for session logs to clean...purged " + cleaned + " log files.");
                } catch (IOException e) {
                  Log.log("SERVER", 0, e);
                } 
                this.this$0.server_info.put("last_expired_log_check_session", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
              }
            });
      } 
      if (!this.server_info.containsKey("last_expired_log_check_job"))
        this.server_info.put("last_expired_log_check_job", (new StringBuffer(String.valueOf(System.currentTimeMillis() - 60000L))).toString()); 
      if (Long.parseLong(this.server_info.getProperty("last_expired_log_check_job")) < System.currentTimeMillis() - 600000L) {
        this.server_info.put("last_expired_log_check_job", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        Worker.startWorker(new Runnable(this) {
              final ServerStatus this$0;
              
              public void run() {
                try {
                  Log.log("SERVER", 0, "Checking for job logs to clean...");
                  File[] logs = (new File(String.valueOf((new File(ServerStatus.change_vars_to_values_static(ServerStatus.SG("log_location"), null, null, null))).getCanonicalFile().getParentFile().getPath()) + "/logs/jobs/")).listFiles();
                  Log.log("SERVER", 0, "Checking for job logs to clean...found " + logs.length + " log files, checking for expired logs...");
                  int cleaned = 0;
                  for (int x = 0; logs != null && x < logs.length; x++) {
                    this.this$0.server_info.put("last_expired_log_check_job", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
                    if (logs[x].isFile() && logs[x].getName().toUpperCase().endsWith(".LOG"))
                      if (logs[x].getName().toUpperCase().startsWith("_") && System.currentTimeMillis() - logs[x].lastModified() > 86400000L * ServerStatus.LG("recent_temp_job_log_days")) {
                        logs[x].delete();
                        cleaned++;
                      } else if (!logs[x].getName().toUpperCase().startsWith("_") && System.currentTimeMillis() - logs[x].lastModified() > 86400000L * ServerStatus.LG("recent_job_log_days")) {
                        logs[x].delete();
                        cleaned++;
                      }  
                  } 
                  Log.log("SERVER", 0, "Checking for job logs to clean...purged " + cleaned + " log files.");
                } catch (IOException e) {
                  Log.log("SERVER", 0, e);
                } 
                this.this$0.server_info.put("last_expired_log_check_job", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
              }
            });
      } 
    } else if (arg.equals("monitor_folders") || arg.equals("monitor_folders_instant")) {
      Thread.sleep(1000L);
      Vector monitored_folders = VG("monitored_folders");
      Vector filelist = new Vector();
      for (int x = 0; x < monitored_folders.size(); x++) {
        Properties p = monitored_folders.elementAt(x);
        if (!p.getProperty("enabled", "true").equals("true"))
          continue; 
        if (p.getProperty("folder") == null)
          continue; 
        File rFolder = new File(p.getProperty("folder"));
        if (!rFolder.exists())
          continue; 
        filelist = new Vector();
        int scan_depth = 1;
        if (p.getProperty("monitor_sub_folders", "true").equals("true"))
          scan_depth = 99; 
        long timeAmount = Long.parseLong(p.getProperty("time_units_no"));
        long multiplier = 1000L;
        if (p.getProperty("time_units").equals("0")) {
          multiplier = 60000L;
        } else if (p.getProperty("time_units").equals("1")) {
          multiplier = 3600000L;
        } else {
          multiplier = 86400000L;
        } 
        if (arg.equals("monitor_folders_instant") && timeAmount < 0L) {
          timeAmount *= -1L;
          multiplier = 1000L;
        } else if (arg.equals("monitor_folders_instant") && timeAmount >= 0L) {
          continue;
        } 
        Common.getAllFileListing(filelist, p.getProperty("folder"), scan_depth, true);
        Vector foundItems = new Vector();
        for (int i = 0; i < filelist.size(); i++) {
          File currFilePointer = filelist.elementAt(i);
          if (!currFilePointer.getCanonicalPath().equals(rFolder.getCanonicalPath())) {
            long lastMod = currFilePointer.lastModified() + multiplier * timeAmount;
            if (System.currentTimeMillis() - lastMod > 0L) {
              if (Common.machine_is_windows()) {
                if (p.getProperty("folder_match", "*").indexOf("\\") > 0 && p.getProperty("folder_match", "*").indexOf("\\\\") < 0)
                  p.put("folder_match", p.getProperty("folder_match", "*").replace("\\", "\\\\")); 
                if (p.getProperty("folder_not_match", "*").indexOf("\\") > 0 && p.getProperty("folder_not_match", "*").indexOf("\\\\") < 0)
                  p.put("folder_not_match", p.getProperty("folder_not_match", "*").replace("\\", "\\\\")); 
                if (p.getProperty("folder_not_match_name", "*").indexOf("\\") > 0 && p.getProperty("folder_not_match_name", "*").indexOf("\\\\") < 0)
                  p.put("folder_not_match_name", p.getProperty("folder_not_match_name", "*").replace("\\", "\\\\")); 
                if (p.getProperty("folder_match", "*").indexOf("\\") < 0 && p.getProperty("folder_match", "*").indexOf("/") >= 0)
                  p.put("folder_match", p.getProperty("folder_match", "*").replace("/", "\\\\")); 
                if (p.getProperty("folder_not_match", "*").indexOf("\\") < 0 && p.getProperty("folder_not_match", "*").indexOf("/") >= 0)
                  p.put("folder_not_match", p.getProperty("folder_not_match", "*").replace("/", "\\\\")); 
                if (p.getProperty("folder_not_match_name", "*").indexOf("\\") < 0 && p.getProperty("folder_not_match_name", "*").indexOf("/") >= 0)
                  p.put("folder_not_match_name", p.getProperty("folder_not_match_name", "*").replace("/", "\\\\")); 
              } 
              if (Common.do_search(p.getProperty("folder_match", "*"), currFilePointer.getAbsolutePath(), false, 0) && (p.getProperty("folder_not_match", "").equals("") || !Common.do_search(p.getProperty("folder_not_match", ""), currFilePointer.getAbsolutePath(), false, 0)) && (p.getProperty("folder_not_match_name", "").equals("") || !Common.do_search(p.getProperty("folder_not_match_name", ""), currFilePointer.getName(), false, 0))) {
                Log.log("SERVER", 2, "Folder Monitor Match:" + p.getProperty("folder_match", "") + "  vs.  " + currFilePointer.getAbsolutePath());
                Log.log("SERVER", 2, "Folder Monitor Not Match:" + p.getProperty("folder_not_match", "") + "  vs.  " + currFilePointer.getAbsolutePath());
                if (p.getProperty("delete").equals("true")) {
                  if (currFilePointer.isFile() && p.getProperty("monitor_files", "true").equals("true")) {
                    if (p.getProperty("folderMonitorAction", "Archive or Delete").equals("Archive or Delete")) {
                      Log.log("SERVER", 0, "FolderMonitor:Deleting file " + currFilePointer.getAbsolutePath());
                      currFilePointer.delete();
                    } else {
                      foundItems.addElement(currFilePointer);
                    } 
                  } else if (currFilePointer.isDirectory() && (p.getProperty("monitor_empty_folders", "false").equals("true") || p.getProperty("monitor_non_empty_folders", "false").equals("true"))) {
                    Log.log("SERVER", 2, "FolderMonitor:Checking to see if folder is OK to delete: " + currFilePointer.getAbsolutePath());
                    Vector emptyFolder = new Vector();
                    Common.getAllFileListing(emptyFolder, String.valueOf(currFilePointer.getCanonicalPath()) + "/", 99, true);
                    boolean empty = true;
                    for (int xx = 0; xx < emptyFolder.size(); xx++) {
                      File ef = emptyFolder.elementAt(xx);
                      if (!ef.getName().startsWith(".") && ((ef.isFile() && p.getProperty("empty_count_files", "true").equals("true")) || (ef.isDirectory() && p.getProperty("empty_count_folders", "false").equals("true")))) {
                        empty = false;
                        break;
                      } 
                    } 
                    if (empty || p.getProperty("monitor_non_empty_folders", "false").equals("true")) {
                      String action = p.getProperty("folderMonitorAction", "Archive or Delete").equals("Archive or Delete") ? "delete" : "archive";
                      if (!currFilePointer.getCanonicalPath().equals(rFolder.getCanonicalPath())) {
                        Log.log("SERVER", 0, "FolderMonitor:" + action + " folder " + currFilePointer.getAbsolutePath());
                        Vector filelist2 = new Vector();
                        Common.getAllFileListing(filelist2, String.valueOf(currFilePointer.getCanonicalPath()) + "/", 99, true);
                        while (filelist2.size() > 0) {
                          File f2 = filelist2.remove(filelist2.size() - 1);
                          long lastMod2 = f2.lastModified() + multiplier * timeAmount;
                          if (System.currentTimeMillis() - lastMod2 > 0L || f2.isDirectory()) {
                            if ((p.getProperty("folder_not_match", "").equals("") || !Common.do_search(p.getProperty("folder_not_match", ""), f2.getCanonicalPath(), false, 0)) && (p.getProperty("folder_not_match_name", "").equals("") || !Common.do_search(p.getProperty("folder_not_match_name", ""), f2.getName(), false, 0))) {
                              Log.log("SERVER", 0, "FolderMonitor:" + action + " item " + f2.getAbsolutePath());
                              if (p.getProperty("folderMonitorAction", "Archive or Delete").equals("Archive or Delete")) {
                                f2.delete();
                                continue;
                              } 
                              foundItems.addElement(f2);
                              continue;
                            } 
                            Log.log("SERVER", 0, "FolderMonitor:Skipping item " + f2.getAbsolutePath() + " because of 'not match'.");
                            continue;
                          } 
                          Log.log("SERVER", 0, "FolderMonitor:Skipping item " + f2.getAbsolutePath() + " because of date being too new on this subitem.");
                        } 
                      } 
                    } 
                  } 
                } else if (currFilePointer.isFile() && p.getProperty("monitor_files", "true").equals("true")) {
                  String srcFold = currFilePointer.getCanonicalPath();
                  String destFold = String.valueOf(p.getProperty("zippath")) + currFilePointer.getCanonicalPath().substring(rFolder.getCanonicalPath().length());
                  int count = 0;
                  while ((new File(destFold)).exists() && count++ < 99)
                    destFold = String.valueOf(destFold) + count; 
                  if (count >= 99)
                    destFold = String.valueOf(destFold) + Common.makeBoundary(4); 
                  if (p.getProperty("folderMonitorAction", "Archive or Delete").equals("Archive or Delete")) {
                    Log.log("SERVER", 0, "FolderMonitor:Moving file " + srcFold + " to " + destFold);
                    (new File(destFold)).getCanonicalFile().getParentFile().mkdirs();
                    boolean moved = (new File(srcFold)).renameTo(new File(destFold));
                    if (!moved) {
                      Common.recurseCopy(srcFold, destFold, true);
                      Common.updateOSXInfo(destFold, "-R");
                      currFilePointer.delete();
                    } 
                  } else {
                    foundItems.addElement(currFilePointer);
                  } 
                } else if (currFilePointer.isDirectory() && (p.getProperty("monitor_empty_folders", "false").equals("true") || p.getProperty("monitor_non_empty_folders", "false").equals("true"))) {
                  Log.log("SERVER", 2, "FolderMonitor:Checking to see if folder is OK to move: " + currFilePointer.getAbsolutePath());
                  Vector emptyFolder = new Vector();
                  Common.getAllFileListing(emptyFolder, String.valueOf(currFilePointer.getCanonicalPath()) + "/", 99, true);
                  boolean empty = true;
                  for (int xx = 0; xx < emptyFolder.size(); xx++) {
                    File ef = emptyFolder.elementAt(xx);
                    if (!ef.getName().startsWith(".") && ef.isFile()) {
                      empty = false;
                      break;
                    } 
                  } 
                  Log.log("SERVER", 2, "FolderMonitor:Checking to see if folder is OK to move: " + currFilePointer.getAbsolutePath() + " : empty=" + empty + " items=" + emptyFolder.size());
                  if (empty || p.getProperty("monitor_non_empty_folders", "false").equals("true")) {
                    String srcFold = currFilePointer.getAbsolutePath();
                    String destFold = String.valueOf(p.getProperty("zippath")) + currFilePointer.getCanonicalPath().substring(rFolder.getCanonicalPath().length()) + "/";
                    int count = 0;
                    while ((new File(destFold)).exists() && count++ < 99)
                      destFold = String.valueOf(destFold) + count; 
                    if (count >= 99)
                      destFold = String.valueOf(destFold) + Common.makeBoundary(4); 
                    if (p.getProperty("folderMonitorAction", "Archive or Delete").equals("Archive or Delete")) {
                      Log.log("SERVER", 0, "FolderMonitor:empty=" + empty + ":Moving folder " + srcFold + " to " + destFold);
                      (new File(destFold)).getCanonicalFile().getParentFile().mkdirs();
                      boolean moved = (new File(srcFold)).renameTo(new File(destFold));
                      if (!moved) {
                        Common.recurseCopy(srcFold, destFold, true);
                        Common.updateOSXInfo(destFold, "-R");
                        Common.recurseDelete(String.valueOf(currFilePointer.getCanonicalPath()) + "/", false);
                      } 
                    } else {
                      foundItems.addElement(currFilePointer);
                    } 
                  } 
                } 
              } 
            } 
          } 
        } 
        if (foundItems.size() > 0) {
          Vector items = new Vector();
          for (int xx = 0; xx < foundItems.size(); xx++) {
            File f = foundItems.elementAt(xx);
            Properties item = new Properties();
            item.put("url", f.toURI().toURL().toExternalForm());
            item.put("the_file_name", f.getName());
            item.put("the_file_path", Common.all_but_last(f.getCanonicalPath()).substring(rFolder.getCanonicalPath().length()).replace('\\', '/'));
            item.put("the_file_size", (new StringBuffer(String.valueOf(f.length()))).toString());
            item.put("type", f.isDirectory() ? "DIR" : "FILE");
            items.addElement(item);
          } 
          Properties event = new Properties();
          event.put("event_plugin_list", p.getProperty("folderMonitorAction", "Archive or Delete"));
          event.put("name", "FolderMonitorEvent:" + p.getProperty("folder"));
          this.events6.doEventPlugin(null, event, null, items);
        } 
        continue;
      } 
    } else {
      siPUT("total_server_bytes_transfered", this.total_server_bytes_sent + this.total_server_bytes_received);
      siPUT("total_server_bytes_sent", this.total_server_bytes_sent);
      siPUT("total_server_bytes_received", this.total_server_bytes_received);
      siPUT("thread_pool_available", (new StringBuffer(String.valueOf(Worker.availableWorkers.size()))).toString());
      siPUT("thread_pool_busy", (new StringBuffer(String.valueOf(Worker.busyWorkers.size()))).toString());
      siPUT("thread_pool_busy_max", (new StringBuffer(String.valueOf(Worker.max_busy_workers))).toString());
      siPUT("ram_max", (new StringBuffer(String.valueOf(Runtime.getRuntime().maxMemory()))).toString());
      siPUT("ram_free", (new StringBuffer(String.valueOf(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()))).toString());
      siPUT("ram_used", (new StringBuffer(String.valueOf(siLG("ram_max") - siLG("ram_free")))).toString());
      if (Common.log != null && (this.loggingProvider1 != null || this.loggingProvider2 != null))
        while (Common.log.size() > 0) {
          Object o = Common.log.remove(0);
          String s = "";
          String tag = "";
          if (o instanceof String) {
            tag = "PROXY";
            s = o.toString();
          } else {
            Properties properties = (Properties)o;
            s = properties.getProperty("data");
            tag = properties.getProperty("tag");
            if (IG("log_debug_level") < Integer.parseInt(properties.getProperty("level", "0")))
              continue; 
          } 
          if (this.loggingProvider1 != null)
            this.loggingProvider1.append_log(s, tag, true); 
          if (this.loggingProvider2 != null)
            this.loggingProvider2.append_log(s, tag, true); 
        }  
      calc_server_speeds(null, null);
      Vector server_list_vec = null;
      try {
        server_list_vec = (Vector)server_settings.get("server_list");
      } catch (Exception e) {
        Properties the_item = (Properties)server_settings.get("server_list");
        server_list_vec = new Vector();
        server_list_vec.addElement(the_item);
      } 
      int x;
      for (x = 0; x < this.main_servers.size(); x++) {
        GenericServer the_server = this.main_servers.elementAt(x);
        the_server.updateStatus();
      } 
      while (siVG("recent_user_list").size() > IG("recent_user_count"))
        siVG("recent_user_list").removeElementAt(0); 
      siPUT("total_logins", (new StringBuffer(String.valueOf(siIG("failed_logins") + siIG("successful_logins")))).toString());
      siPUT("users_connected", (new StringBuffer(String.valueOf(getTotalConnectedUsers()))).toString());
      siPUT("current_datetime_millis", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      siPUT("current_datetime_ddmmyyhhmmss", this.mmddyyhhmmss.format(new Date()));
      for (x = 0; x < this.previewWorkers.size(); x++) {
        PreviewWorker preview = this.previewWorkers.elementAt(x);
        preview.run(null);
      } 
      if (BG("s3crush_replicated")) {
        if (!System.getProperties().containsKey("crushftp.s3_replicated"))
          System.getProperties().put("crushftp.s3_replicated", new Vector()); 
        Vector v = (Vector)System.getProperties().get("crushftp.s3_replicated");
        while (v.size() > 0) {
          Properties pp = v.remove(0);
          pp.put("need_response", "false");
          SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.s3CrushClient.writeFs", "info", pp);
        } 
      } 
      update_history("logged_in_users");
      update_history("current_download_speed");
      update_history("current_upload_speed");
      update_history("ram_max");
      update_history("ram_free");
      update_history("incoming_transfers");
      update_history("outgoing_transfers");
      Properties p = new Properties();
      p.put("server_settings", server_settings);
      p.put("server_info", this.server_info);
      runPlugins(p);
    } 
  }
  
  public void setupGlobalPrefs() {
    System.getProperties().put("java.net.preferIPv4Stack", (new StringBuffer(String.valueOf(BG("force_ipv4")))).toString());
    System.getProperties().put("crushftp.debug", (new StringBuffer(String.valueOf(IG("log_debug_level")))).toString());
    System.getProperties().put("crushftp.lsla", (new StringBuffer(String.valueOf(BG("lsla")))).toString());
    System.getProperties().put("crushftp.socketpooltimeout", (new StringBuffer(String.valueOf(IG("socketpool_timeout") * 1000))).toString());
    System.getProperties().put("crushftp.ls.year", (new StringBuffer(String.valueOf(BG("lsla_year")))).toString());
    System.getProperties().put("crushftp.max_threads", (new StringBuffer(String.valueOf(IG("max_threads")))).toString());
    System.getProperties().put("crushftp.http_buffer", (new StringBuffer(String.valueOf(IG("http_buffer")))).toString());
    System.getProperties().put("crushftp.file_client_not_found_error", (new StringBuffer(String.valueOf(BG("file_client_not_found_error")))).toString());
    System.getProperties().put("crushftp.memcache", System.getProperty("crushftp.memcache", (new StringBuffer(String.valueOf(BG("memcache")))).toString()));
    System.getProperties().put("crushftp.proxy.list.max", (new StringBuffer(String.valueOf(IG("proxy_list_max")))).toString());
    System.getProperties().put("crushftp.multi_journal", (new StringBuffer(String.valueOf(BG("multi_journal")))).toString());
    System.getProperties().put("crushftp.hash_algorithm", (new StringBuffer(String.valueOf(SG("hash_algorithm")))).toString());
    System.getProperties().put("crushftp.crushtask.store_job_items", (new StringBuffer(String.valueOf(BG("store_job_items")))).toString());
    System.getProperties().put("crushftp.ssl_renegotiation_blocked", (new StringBuffer(String.valueOf(BG("ssl_renegotiation_blocked")))).toString());
    System.getProperties().put("crushftp.tls_version_client", (new StringBuffer(String.valueOf(SG("tls_version_client")))).toString());
    System.getProperties().put("crushftp.s3_sha256", (new StringBuffer(String.valueOf(BG("s3_sha256")))).toString());
    System.getProperties().put("crushftp.replicated_vfs_url", (new StringBuffer(String.valueOf(SG("replicated_vfs_url")))).toString());
    System.getProperties().put("crushftp.replicated_vfs_root_url", (new StringBuffer(String.valueOf(SG("replicated_vfs_root_url")))).toString());
    System.getProperties().put("crushftp.replicated_vfs_user", (new StringBuffer(String.valueOf(SG("replicated_vfs_user")))).toString());
    System.getProperties().put("crushftp.replicated_vfs_pass", (new StringBuffer(String.valueOf(SG("replicated_vfs_pass")))).toString());
    System.getProperties().put("crushftp.replicated_vfs_ping_interval", (new StringBuffer(String.valueOf(SG("replicated_vfs_ping_interval")))).toString());
    System.getProperties().put("crushftp.replicated_auto_play_journal", (new StringBuffer(String.valueOf(SG("replicated_auto_play_journal")))).toString());
    System.getProperties().put("crushftp.s3_partial", (new StringBuffer(String.valueOf(!BG("s3_ignore_partial")))).toString());
    System.getProperties().put("crushftp.line_separator_crlf", (new StringBuffer(String.valueOf(BG("line_separator_crlf")))).toString());
    System.getProperties().put("crushftp.audit_job_logs", (new StringBuffer(String.valueOf(BG("audit_job_logs")))).toString());
    System.getProperties().put("crushftp.disable_mdtm_modifications", (new StringBuffer(String.valueOf(BG("disable_mdtm_modifications")))).toString());
    System.getProperties().put("crushftp.log_date_format", (new StringBuffer(String.valueOf(SG("log_date_format")))).toString());
    System.getProperties().put("crushftp.terrabytes_label_short", (new StringBuffer(String.valueOf(SG("terrabytes_label_short")))).toString());
    System.getProperties().put("crushftp.gigabytes_label_short", (new StringBuffer(String.valueOf(SG("gigabytes_label_short")))).toString());
    System.getProperties().put("crushftp.megabytes_label_short", (new StringBuffer(String.valueOf(SG("megabytes_label_short")))).toString());
    System.getProperties().put("crushftp.kilobytes_label_short", (new StringBuffer(String.valueOf(SG("kilobytes_label_short")))).toString());
    System.getProperties().put("crushftp.bytes_label_short", (new StringBuffer(String.valueOf(SG("bytes_label_short")))).toString());
    System.getProperties().put("crushftp.terrabytes_label", (new StringBuffer(String.valueOf(SG("terrabytes_label")))).toString());
    System.getProperties().put("crushftp.gigabytes_label", (new StringBuffer(String.valueOf(SG("gigabytes_label")))).toString());
    System.getProperties().put("crushftp.megabytes_label", (new StringBuffer(String.valueOf(SG("megabytes_label")))).toString());
    System.getProperties().put("crushftp.kilobytes_label", (new StringBuffer(String.valueOf(SG("kilobytes_label")))).toString());
    System.getProperties().put("crushftp.bytes_label", (new StringBuffer(String.valueOf(SG("bytes_label")))).toString());
    System.getProperties().put("crushftp.jobs_location", (new StringBuffer(String.valueOf(SG("jobs_location")))).toString());
    System.getProperties().put("crushftp.log_location", change_vars_to_values_static(SG("log_location"), null, null, null));
    System.getProperties().put("crushftp.log_debug_level", (new StringBuffer(String.valueOf(SG("log_debug_level")))).toString());
    System.getProperties().put("crushftp.smtp_helo_ip", (new StringBuffer(String.valueOf(SG("smtp_helo_ip")))).toString());
    System.getProperties().put("crushftp.smtp_subject_utf8", (new StringBuffer(String.valueOf(SG("smtp_subject_utf8")))).toString());
    System.getProperties().put("crushftp.smtp_subject_encoded", (new StringBuffer(String.valueOf(SG("smtp_subject_encoded")))).toString());
    System.getProperties().put("crushftp.debug_socks_log", (new StringBuffer(String.valueOf(BG("debug_socks_log")))).toString());
    System.getProperties().put("crushftp.smtp.sasl", (new StringBuffer(String.valueOf(BG("crushftp_smtp_sasl")))).toString());
    Common.System2.put("enterprise_level", (new StringBuffer(String.valueOf(siIG("enterprise_level")))).toString());
  }
  
  public void update_history(String key) {
    Vector v = (Vector)this.server_info.get(String.valueOf(key) + "_history");
    Object v2 = this.server_info.get(key);
    if (v == null) {
      v = new Vector();
      this.server_info.put(String.valueOf(key) + "_history", v);
    } 
    if (v2 instanceof Vector)
      this.server_info.put(String.valueOf(key) + "_count", (new StringBuffer(String.valueOf(((Vector)v2).size()))).toString()); 
    while (v.size() > 299)
      v.remove(0); 
    Object o = siOG(key);
    if (o instanceof String) {
      v.addElement(o);
    } else {
      v.addElement(Common.CLONE(o));
    } 
  }
  
  public void runPlugins(Properties info) {
    runPlugins(info, false);
  }
  
  public void runPlugins(Properties info, boolean debug) {
    Vector plugins = (Vector)server_settings.get("plugins");
    if (plugins != null)
      for (int x = 0; x < plugins.size(); x++) {
        Vector pluginPrefs = null;
        if (plugins.elementAt(x) instanceof Vector) {
          pluginPrefs = plugins.elementAt(x);
        } else {
          pluginPrefs = new Vector();
          pluginPrefs.addElement(plugins.elementAt(x));
        } 
        for (int xx = 0; xx < pluginPrefs.size(); xx++) {
          if (!(pluginPrefs.elementAt(xx) instanceof String)) {
            Properties pluginPref = pluginPrefs.elementAt(xx);
            if (debug)
              Log.log("PLUGIN", 2, String.valueOf(pluginPref.getProperty("pluginName")) + " : " + pluginPref.getProperty("subItem", "")); 
            try {
              Common.runPlugin(pluginPref.getProperty("pluginName"), info, pluginPref.getProperty("subItem", ""));
            } catch (Exception e) {
              Log.log("PLUGIN", 1, e);
            } 
          } 
        } 
      }  
  }
  
  public void update_ip() {
    String new_ip = this.common_code.discover_ip();
    if (new_ip.equals("0.0.0.0")) {
      new_ip = SG("discovered_ip");
      if (new_ip.equals("0.0.0.0"))
        new_ip = Common.getLocalIP(); 
      append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Auto IP lookup failure (could not reach server)!") + "---", "ERROR");
    } 
    if (!new_ip.equals("0.0.0.0") && Common.count_str(new_ip, ".") == 3) {
      server_settings.put("discovered_ip", new_ip);
    } else {
      append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Auto IP lookup failure (could not reach server, and could not detect local IP)!") + "---", "ERROR");
    } 
    for (int x = 0; x < this.main_servers.size(); x++) {
      GenericServer the_server = this.main_servers.elementAt(x);
      the_server.updateStatus();
    } 
  }
  
  public static int calc_server_up_speeds(String username, String ip) throws Exception {
    int speed = 0;
    for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        SessionCrush theSession = (SessionCrush)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (theSession.uiBG("receiving_file"))
          if ((username == null || username.equalsIgnoreCase(theSession.uiSG("user_name"))) && (ip == null || ip.equalsIgnoreCase(theSession.uiSG("user_ip"))))
            speed = (int)(speed + theSession.uiLG("current_transfer_speed"));  
      } catch (Exception e) {
        if (e.indexOf("Interrupted") >= 0)
          throw e; 
        Log.log("SERVER", 1, e);
      } 
    } 
    return speed;
  }
  
  public static int count_users_up() throws Exception {
    int num_items = 0;
    for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        SessionCrush theSession = (SessionCrush)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (theSession.stor_files_pool_used.size() > 0 && !theSession.uiBG("pause_now"))
          num_items++; 
      } catch (Exception e) {
        if (e.indexOf("Interrupted") >= 0)
          throw e; 
        Log.log("SERVER", 2, e);
      } 
    } 
    return num_items;
  }
  
  public static Vector get_transfer_times() throws Exception {
    Vector timer = new Vector();
    int x;
    for (x = siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        SessionCrush theSession = (SessionCrush)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (theSession.uiLG("seconds_remaining") > 0L)
          timer.addElement((new StringBuffer(String.valueOf(theSession.uiLG("seconds_remaining")))).toString()); 
      } catch (Exception e) {
        if (e.indexOf("Interrupted") >= 0)
          throw e; 
      } 
    } 
    for (x = 0; x < timer.size(); x++) {
      for (int xx = x + 1; xx < timer.size(); xx++) {
        int num1 = Integer.parseInt(timer.elementAt(x).toString());
        int num2 = Integer.parseInt(timer.elementAt(xx).toString());
        if (num2 < num1) {
          timer.setElementAt((new StringBuffer(String.valueOf(num2))).toString(), x);
          timer.setElementAt((new StringBuffer(String.valueOf(num1))).toString(), xx);
        } 
      } 
    } 
    return timer;
  }
  
  public static int count_users_down() throws Exception {
    int num_items = 0;
    for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        SessionCrush theSession = (SessionCrush)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (theSession.retr_files_pool_used.size() > 0 && !theSession.uiBG("pause_now"))
          num_items++; 
      } catch (Exception e) {
        if (e.indexOf("Interrupted") >= 0)
          throw e; 
        Log.log("SERVER", 3, e);
      } 
    } 
    return num_items;
  }
  
  public static int calc_server_down_speeds(String username, String ip) throws Exception {
    int speed = 0;
    for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        SessionCrush theSession = (SessionCrush)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (theSession.uiBG("sending_file"))
          if ((username == null || username.equalsIgnoreCase(theSession.uiSG("user_name"))) && (ip == null || ip.equalsIgnoreCase(theSession.uiSG("user_ip"))))
            speed = (int)(speed + theSession.uiLG("current_transfer_speed"));  
      } catch (Exception e) {
        if (e.indexOf("Interrupted") >= 0)
          throw e; 
        Log.log("SERVER", 3, e);
      } 
    } 
    return speed;
  }
  
  public static int calc_server_speeds(String username, String ip) {
    int speed = 0;
    try {
      int downSpeed = calc_server_down_speeds(username, ip);
      speed += downSpeed;
      int upSpeed = calc_server_up_speeds(username, ip);
      speed += upSpeed;
      siPUT("current_download_speed", downSpeed);
      siPUT("current_upload_speed", upSpeed);
    } catch (Exception e) {
      Log.log("SERVER", 3, e);
    } 
    return speed;
  }
  
  public void quit_server() {
    append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|********" + LOC.G("CrushFTP Quit") + "******** " + siSG("version_info_str") + siSG("sub_version_info_str"), "QUIT_SERVER");
    try {
      this.loggingProvider2.shutdown();
    } catch (Exception exception) {}
    try {
      this.loggingProvider1.shutdown();
    } catch (Exception exception) {}
    try {
      Thread.sleep(2000L);
    } catch (InterruptedException interruptedException) {}
    System.exit(0);
  }
  
  public void append_log(String log_data, String tag) {
    Properties p = new Properties();
    p.put("tag", tag);
    p.put("level", "0");
    p.put("data", log_data);
    Common.log.addElement(p);
  }
  
  public void save_server_settings(boolean autoSave) {
    this.prefsProvider.check_code();
    if (this.starting)
      return; 
    if (autoSave && !BG("allow_auto_save"))
      return; 
    this.prefsProvider.savePrefs(server_settings, null);
  }
  
  public static void put_in(String key, Object data) {
    try {
      thisObj.server_info.put(key, data);
    } catch (Exception exception) {}
  }
  
  public void do_auto_update_early(boolean webOnly) throws Exception {
    try {
      (new Thread(new Runnable(this, webOnly) {
            final ServerStatus this$0;
            
            private final boolean val$webOnly;
            
            public void run() {
              try {
                if (this.this$0.updateHandler.doSilentUpdate(true, ServerStatus.version_info_str, this.val$webOnly))
                  if (!this.val$webOnly)
                    this.this$0.restart_crushftp();  
              } catch (Exception e) {
                Log.log("SERVER", 0, e);
              } 
            }
          })).start();
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
      throw e;
    } 
  }
  
  public void doCheckForUpdate(boolean checkBuild) {
    Properties p = new Properties();
    p.put("version", version_info_str);
    p.put("check_build", (new StringBuffer(String.valueOf(checkBuild))).toString());
    Common.checkForUpdate(p);
    if (!p.getProperty("version", "").equals(version_info_str)) {
      if (checkBuild && p.getProperty("version", "").equals(String.valueOf(version_info_str) + sub_version_info_str))
        return; 
      this.server_info.put("update_available", "true");
      this.server_info.put("update_available_version", p.getProperty("version"));
      this.server_info.put("update_available_html", p.getProperty("html"));
      runAlerts("update", null);
    } 
  }
  
  public void restart_crushftp() {
    save_server_settings(false);
    this.starting = true;
    stop_all_servers();
    this.shutdown.run();
    try {
      Thread.sleep(1000L);
      if (Common.machine_is_windows()) {
        Runtime.getRuntime().exec("net start CrushFTPRestart");
      } else if (Common.machine_is_x()) {
        Runtime.getRuntime().exec(new String[] { "launchctl", "start", "com.crushftp.CrushFTPUpdate" });
      } else if (!SG("restart_script").trim().equals("")) {
        Runtime.getRuntime().exec(SG("restart_script").split(";"));
      } else {
        Runtime.getRuntime().exec(new String[] { "nohup", "java", "-cp", "plugins/lib/CrushFTPRestart.jar", "CrushFTPRestart" });
      } 
    } catch (Exception ee) {
      ee.printStackTrace();
    } 
    quit_server();
  }
  
  public boolean kick(String the_user, boolean logit) {
    for (int x = 0; x < siVG("user_list").size(); x++) {
      Properties p = siVG("user_list").elementAt(x);
      if (p.getProperty("id").equalsIgnoreCase(the_user))
        return kick(p, logit); 
    } 
    return false;
  }
  
  public boolean kick(String the_user) {
    return kick(the_user, true);
  }
  
  public boolean kick(Properties user_info) {
    return kick(user_info, true);
  }
  
  public boolean kick(Properties user_info, boolean logit) {
    try {
      SessionCrush theSession = (SessionCrush)user_info.get("session");
      if (logit)
        try {
          append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("User Kicked") + "---:" + theSession.uiSG("user_number") + "-" + theSession.uiSG("user_name"), "KICK");
        } catch (Exception exception) {} 
      try {
        remove_user(user_info);
      } catch (Exception exception) {}
      if (theSession != null && !theSession.uiBG("dieing")) {
        if (logit)
          theSession.uiPUT("termination_message", "KICKED"); 
        theSession.uiPUT("friendly_quit", "true");
        theSession.not_done = false;
        theSession.killSession();
        try {
          while (theSession.session_socks.size() > 0) {
            Socket sock = theSession.session_socks.remove(0);
            try {
              Worker.startWorker(new Runnable(this, sock) {
                    final ServerStatus this$0;
                    
                    private final Socket val$sock;
                    
                    public void run() {
                      try {
                        this.val$sock.setSoTimeout(2000);
                        this.val$sock.setSoLinger(true, 2);
                        this.val$sock.close();
                      } catch (Exception exception) {}
                    }
                  });
            } catch (IOException iOException) {}
          } 
        } catch (Exception exception) {
          try {
            while (theSession.old_data_socks.size() > 0) {
              Socket tempSock = theSession.old_data_socks.remove(0);
              tempSock.close();
            } 
          } catch (Exception exception1) {}
        } 
      } else {
        return true;
      } 
      while (theSession.old_data_socks.size() <= 0) {
        Socket socket = theSession.old_data_socks.remove(0);
        socket.close();
      } 
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
      return false;
    } 
  }
  
  public boolean passive_kick(Properties user_info) {
    boolean success = true;
    try {
      SessionCrush theSession = (SessionCrush)user_info.get("session");
      try {
        append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("User Kicked") + "---:" + theSession.uiSG("user_number") + "-" + theSession.uiSG("user_name"), "KICK");
      } catch (Exception exception) {}
      try {
        append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Kicking session: passive_kick.") + "---", "KICK");
      } catch (Exception exception) {}
      if (!theSession.uiBG("dieing")) {
        theSession.uiPUT("termination_message", "KICKED");
        theSession.uiPUT("friendly_quit", "true");
        theSession.not_done = false;
        theSession.do_kill(null);
      } 
    } catch (Exception e) {
      success = false;
      Log.log("SERVER", 1, e);
    } 
    return success;
  }
  
  public boolean ban(String the_user, String reason) {
    return ban(the_user, 0, reason);
  }
  
  public boolean ban(String the_user, int timeout, String reason) {
    int x;
    for (x = 0; x < siVG("user_list").size(); x++) {
      Properties p = siVG("user_list").elementAt(x);
      if (p.getProperty("id").equalsIgnoreCase(the_user))
        return ban(p, timeout, false, reason); 
    } 
    for (x = 0; x < siVG("recent_user_list").size(); x++) {
      Properties p = siVG("recent_user_list").elementAt(x);
      if (p.getProperty("id").equalsIgnoreCase(the_user))
        return ban(p, timeout, false, reason); 
    } 
    return false;
  }
  
  public boolean ban(Properties user_info, int timeout, String reason) {
    return ban(user_info, timeout, false, reason);
  }
  
  public boolean ban(Properties user_info, int timeout, boolean onlyRealBan, String reason) {
    try {
      String new_ip_text = user_info.getProperty("user_ip");
      new_ip_text = new_ip_text.substring(new_ip_text.indexOf("/") + 1, new_ip_text.length());
      if (ban_ip(new_ip_text, timeout, onlyRealBan, reason)) {
        try {
          append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("User Banned") + "---:" + user_info.getProperty("user_number") + "-" + user_info.getProperty("user_name") + "  " + new_ip_text, "BAN");
        } catch (Exception exception) {}
        return true;
      } 
    } catch (Exception exception) {}
    return false;
  }
  
  public boolean ban_ip(String ip, int timeout, String reason) throws Exception {
    return ban_ip(ip, timeout, false, reason);
  }
  
  public boolean ban_ip(String ip, int timeout, boolean onlyRealBan, String reason) throws Exception {
    if (SG("never_ban").equals("disabled"))
      return false; 
    if (SG("never_ban").trim().equals("*"))
      return false; 
    if (!ip.endsWith(".1") && ip.indexOf(".") > 0) {
      Properties ip_data = new Properties();
      Vector ip_list = (Vector)server_settings.get("ip_restrictions");
      boolean ipOK = true;
      for (int x = 0; x < ip_list.size(); x++) {
        Properties p = ip_list.elementAt(x);
        if (p.getProperty("start_ip").equals(ip))
          return !onlyRealBan; 
      } 
      String[] never_ban = SG("never_ban").split(",");
      for (int i = 0; i < never_ban.length; i++) {
        if (!never_ban[i].trim().equals("") && 
          Common.do_search(never_ban[i].trim(), ip, false, 0))
          return false; 
      } 
      long timeout2 = (60000 * timeout);
      if (System.currentTimeMillis() - this.server_start_time.getTime() < 60000L)
        timeout2 = 5000L; 
      if (ipOK) {
        ip_data.put("type", (timeout2 > 0L) ? "T" : "D");
        ip_data.put("start_ip", ip);
        ip_data.put("stop_ip", ip);
        ip_data.put("reason", String.valueOf(thisObj.logDateFormat.format(new Date())) + ":" + reason);
        if (timeout2 > 0L)
          ip_data.put("timeout", (new StringBuffer(String.valueOf(timeout2 + System.currentTimeMillis()))).toString()); 
        ip_list.insertElementAt(ip_data, 0);
        server_settings.put("ip_restrictions", ip_list);
        Properties pp = new Properties();
        pp.put("id", String.valueOf((new Date()).getTime()) + ":" + Common.makeBoundary());
        pp.put("complete", "false");
        pp.put("data", server_settings);
        thisObj.setSettings(pp);
        try {
          Properties info = new Properties();
          info.put("alert_type", "ban");
          info.put("alert_sub_type", "ip");
          info.put("alert_timeout", (new StringBuffer(String.valueOf(timeout2))).toString());
          info.put("alert_max", "0");
          info.put("alert_msg", ip);
          runAlerts("security_alert", info, null, null);
        } catch (Exception e) {
          Log.log("BAN", 1, e);
        } 
        return true;
      } 
    } 
    return false;
  }
  
  private static String uSG(Properties user_info, String key) {
    return user_info.getProperty(key, "");
  }
  
  private static int uIG(Properties user_info, String data) {
    try {
      return Integer.parseInt(uSG(user_info, data));
    } catch (Exception exception) {
      return 0;
    } 
  }
  
  private static long uLG(Properties user_info, String data) {
    try {
      return Long.parseLong(uSG(user_info, data));
    } catch (Exception exception) {
      return 0L;
    } 
  }
  
  private static boolean uBG(Properties user_info, String data) {
    return uSG(user_info, data).toLowerCase().equals("true");
  }
  
  public String change_vars_to_values(String in_str, SessionCrush the_session) {
    if (the_session != null)
      return change_vars_to_values(in_str, the_session.user, the_session.user_info, the_session); 
    return change_vars_to_values(in_str, new Properties(), new Properties(), the_session);
  }
  
  public String change_vars_to_values(String in_str, Properties user, Properties user_info, SessionCrush the_session) {
    return change_vars_to_values_static(in_str, user, user_info, the_session);
  }
  
  public static String change_vars_to_values_static(String in_str, Properties user, Properties user_info, SessionCrush the_session) {
    try {
      String r1 = "%";
      String r2 = "%";
      for (int r = 0; r < 2; r++) {
        if (in_str.indexOf(r1) >= 0)
          in_str = parse_server_messages(in_str); 
        if (in_str.indexOf(String.valueOf(r1) + "ldap_") >= 0)
          while (in_str.indexOf(String.valueOf(r1) + "ldap_") >= 0) {
            String key = in_str.substring(in_str.indexOf(String.valueOf(r1) + "ldap_"), in_str.indexOf(r2, in_str.indexOf(String.valueOf(r1) + "ldap_") + 1) + 1);
            in_str = Common.replace_str(in_str, key, uSG(user, key.substring(1, key.length() - 1)));
          }  
        if (in_str.indexOf(String.valueOf(r1) + "user_") >= 0) {
          int loc = in_str.indexOf(String.valueOf(r1) + "user_");
          while (loc >= 0) {
            String key = in_str.substring(loc, in_str.indexOf(r2, loc + 1) + 1);
            String user_key = key.substring((String.valueOf(r1) + "user_").length(), key.length() - 1);
            String user_key2 = "user_" + user_key;
            if (user_key.equals("user_sfv")) {
              user_key = "user_md5";
              in_str = in_str.replaceAll("CRC32", "MD5");
            } 
            if (user_key2.equals("user_sfv")) {
              user_key2 = "user_md5";
              in_str = in_str.replaceAll("CRC32", "MD5");
            } 
            if (!user_key2.equals("user_password"))
              if (user != null && user.containsKey(user_key)) {
                in_str = Common.replace_str(in_str, key, uSG(user, user_key));
              } else if (user_info.containsKey(user_key)) {
                in_str = Common.replace_str(in_str, key, uSG(user_info, user_key));
              } else if (user != null && user.containsKey(user_key2)) {
                in_str = Common.replace_str(in_str, key, uSG(user, user_key2));
              } else if (user_info.containsKey(user_key2)) {
                in_str = Common.replace_str(in_str, key, uSG(user_info, user_key2));
              } else if (user_key2.equalsIgnoreCase("user_dir")) {
                String cd = user_info.getProperty("current_dir", "/");
                if (cd != null && user != null && cd.toUpperCase().startsWith(user.getProperty("root_dir", "").toUpperCase()))
                  cd = cd.substring(user.getProperty("root_dir").length() - 1); 
                in_str = Common.replace_str(in_str, key, cd);
              }  
            loc++;
            loc = in_str.indexOf(String.valueOf(r1) + "user_", loc);
          } 
        } 
        if (in_str.indexOf(String.valueOf(r1) + "beep" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "beep" + r2, ""); 
        if (in_str.indexOf(String.valueOf(r1) + "hostname" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "hostname" + r2, hostname); 
        if (in_str.indexOf(String.valueOf(r1) + "server_time_date" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "server_time_date" + r2, (new Date()).toString()); 
        if (in_str.indexOf(String.valueOf(r1) + "login_number" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "login_number" + r2, uSG(user_info, "user_number")); 
        if (in_str.indexOf(String.valueOf(r1) + "users_connected" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "users_connected" + r2, thisObj.getTotalConnectedUsers()); 
        if (in_str.indexOf(String.valueOf(r1) + "user_password" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_password" + r2, uSG(user_info, "current_password")); 
        if (in_str.indexOf(String.valueOf(r1) + "user_name" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_name" + r2, uSG(user, "username")); 
        if (in_str.indexOf(String.valueOf(r1) + "user_anonymous_password" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_anonymous_password" + r2, uSG(user_info, "user_name").equalsIgnoreCase("anonymous") ? uSG(user_info, "current_password") : ""); 
        if (in_str.indexOf(String.valueOf(r1) + "user_current_dir" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_current_dir" + r2, the_session.get_PWD()); 
        if (in_str.indexOf(String.valueOf(r1) + "user_sessionid" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_sessionid" + r2, uSG(user_info, "CrushAuth")); 
        if (in_str.indexOf(String.valueOf(r1) + "user_site_commands_text" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_site_commands_text" + r2, uSG(user, "site")); 
        try {
          if (in_str.indexOf(String.valueOf(r1) + "user_time_remaining" + r2) >= 0) {
            String time_str = String.valueOf(uLG(user_info, "seconds_remaining")) + " secs";
            if (uLG(user_info, "seconds_remaining") == 0L)
              time_str = "<None Active>"; 
            user_info.put("last_time_remaining", time_str);
            if (uLG(user_info, "seconds_remaining") > 60L)
              time_str = String.valueOf(uLG(user_info, "seconds_remaining") / 60L) + "min, " + (uLG(user_info, "seconds_remaining") - uLG(user_info, "seconds_remaining") / 60L * 60L) + " secs"; 
            in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_time_remaining" + r2, time_str);
            user_info.put("last_time_remaining", time_str);
          } 
        } catch (Exception e) {
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_time_remaining" + r2, uSG(user_info, "last_time_remaining"));
        } 
        if (in_str.indexOf(String.valueOf(r1) + "user_paused" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_paused" + r2, uBG(user_info, "pause_now") ? "!PAUSED!" : ""); 
        if (in_str.indexOf(String.valueOf(r1) + "user_bytes_remaining" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_bytes_remaining" + r2, uLG(user_info, "file_length") - uLG(user_info, "bytes_sent") - uLG(user_info, "start_transfer_byte_amount")); 
        if (in_str.indexOf(String.valueOf(r1) + "user_pasv_port" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_pasv_port" + r2, uIG(user_info, "PASV_port")); 
        if (in_str.indexOf(String.valueOf(r1) + "user_ratio" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_ratio" + r2, uSG(user, "ratio") + " to 1"); 
        if (in_str.indexOf(String.valueOf(r1) + "user_perm_ratio" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_perm_ratio" + r2, uBG(user, "perm_ratio") ? "Yes" : "No"); 
        if (in_str.indexOf(String.valueOf(r1) + "user_reverse_ip" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "user_reverse_ip" + r2, InetAddress.getByName(uSG(user, "user_ip")).getHostName()); 
        if (in_str.indexOf(String.valueOf(r1) + "tunnels" + r2) >= 0) {
          String userTunnels = String.valueOf(user.getProperty("tunnels", "")) + ",";
          Vector tunnels = VG("tunnels");
          ByteArrayOutputStream baot = new ByteArrayOutputStream();
          for (int x = 0; x < tunnels.size(); x++) {
            ByteArrayOutputStream baot2 = new ByteArrayOutputStream();
            Properties p = tunnels.elementAt(x);
            if (userTunnels.indexOf(String.valueOf(p.getProperty("id")) + ",") >= 0 && !p.getProperty("tunnelType", "HTTP").equals("SSH")) {
              p.store(baot2, "");
              String s = new String(baot2.toByteArray(), "UTF8");
              s = Common.url_encode(s);
              baot.write(s.getBytes("UTF8"));
              baot.write(";;;".getBytes());
            } 
          } 
          String tunnelsStr = (new String(baot.toByteArray(), "UTF8")).replace('%', '~');
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "tunnels" + r2, tunnelsStr);
        } 
        if (in_str.indexOf(String.valueOf(r1) + "last_login_date_time" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "last_login_date_time" + r2, siSG("last_login_date_time")); 
        if (in_str.indexOf(String.valueOf(r1) + "last_login_ip" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "last_login_ip" + r2, siSG("last_login_ip")); 
        if (in_str.indexOf(String.valueOf(r1) + "last_login_user" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "last_login_user" + r2, siSG("last_login_user")); 
        if (in_str.indexOf(String.valueOf(r1) + "failed_logins" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "failed_logins" + r2, siIG("failed_logins")); 
        if (in_str.indexOf(String.valueOf(r1) + "successful_logins" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "successful_logins" + r2, siIG("successful_logins")); 
        if (in_str.indexOf(String.valueOf(r1) + "total_logins" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "total_logins" + r2, siIG("failed_logins") + siIG("successful_logins")); 
        if (in_str.indexOf(String.valueOf(r1) + "downloaded_files" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "downloaded_files" + r2, siIG("downloaded_files")); 
        if (in_str.indexOf(String.valueOf(r1) + "uploaded_files" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "uploaded_files" + r2, siIG("uploaded_files")); 
        if (in_str.indexOf(String.valueOf(r1) + "bytes_received_f" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "bytes_received_f" + r2, siSG("total_server_bytes_received")); 
        if (in_str.indexOf(String.valueOf(r1) + "bytes_sent_f" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "bytes_sent_f" + r2, siSG("total_server_bytes_sent")); 
        if (in_str.indexOf(String.valueOf(r1) + "total_bytes_f" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "total_bytes_f" + r2, siSG("total_server_bytes_transfered")); 
        if (in_str.indexOf(String.valueOf(r1) + "max_server_download_speed" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "max_server_download_speed" + r2, SG("max_server_download_speed")); 
        if (in_str.indexOf(String.valueOf(r1) + "max_server_upload_speed" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "max_server_upload_speed" + r2, SG("max_server_upload_speed")); 
        if (in_str.indexOf(String.valueOf(r1) + "bytes_received" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "bytes_received" + r2, siSG("total_server_bytes_received")); 
        if (in_str.indexOf(String.valueOf(r1) + "bytes_sent" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "bytes_sent" + r2, siSG("total_server_bytes_sent")); 
        if (in_str.indexOf(String.valueOf(r1) + "total_bytes" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "total_bytes" + r2, siSG("total_server_bytes_transfered")); 
        if (in_str.indexOf(String.valueOf(r1) + "current_server_downloading_count" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "current_server_downloading_count" + r2, count_users_down()); 
        if (in_str.indexOf(String.valueOf(r1) + "current_server_uploading_count" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "current_server_uploading_count" + r2, count_users_up()); 
        if (in_str.indexOf(String.valueOf(r1) + "current_download_speed" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "current_download_speed" + r2, siSG("current_download_speed")); 
        if (in_str.indexOf(String.valueOf(r1) + "current_upload_speed" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "current_upload_speed" + r2, siSG("current_upload_speed")); 
        if (in_str.indexOf(String.valueOf(r1) + "max_users" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "max_users" + r2, SG("max_users")); 
        if (in_str.indexOf(String.valueOf(r1) + "ip" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "ip" + r2, siSG("discovered_ip")); 
        if (in_str.indexOf(String.valueOf(r1) + "beep_connect" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "beep_connect" + r2, SG("beep_connect")); 
        if (in_str.indexOf(String.valueOf(r1) + "deny_reserved_ports" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "deny_reserved_ports" + r2, SG("deny_reserved_ports")); 
        if (in_str.indexOf(String.valueOf(r1) + "deny_fxp" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "deny_fxp" + r2, SG("deny_fxp")); 
        if (in_str.indexOf(String.valueOf(r1) + "about_info_str" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "about_info" + r2, siSG("about_info_str")); 
        if (in_str.indexOf(String.valueOf(r1) + "version_info" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "version_info" + r2, siSG("version_info_str")); 
        if (in_str.indexOf(String.valueOf(r1) + "start_time" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "start_time" + r2, siSG("server_start_time")); 
        if (in_str.indexOf(String.valueOf(r1) + "thread_count" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "thread_count" + r2, Thread.activeCount()); 
        if (in_str.indexOf(String.valueOf(r1) + "free_memory" + r2) >= 0)
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "free_memory" + r2, Runtime.getRuntime().freeMemory() / 1024L); 
        if (in_str.indexOf(String.valueOf(r1) + "global_") >= 0) {
          if (Common.System2.get("global_variables") == null)
            Common.System2.put("global_variables", new Properties()); 
          Properties global_variables = (Properties)Common.System2.get("global_variables");
          Enumeration keys = global_variables.keys();
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if (in_str.indexOf(String.valueOf(r1) + key + r2) >= 0)
              in_str = Common.replace_str(in_str, String.valueOf(r1) + key + r2, global_variables.getProperty(key, "")); 
          } 
        } 
        while (in_str.indexOf(String.valueOf(r1) + "customData_") >= 0) {
          String custom = in_str.substring(in_str.indexOf(String.valueOf(r1) + "customData_") + (String.valueOf(r1) + "customData_").length());
          custom = custom.substring(0, custom.indexOf(r2));
          Properties customData = (Properties)server_settings.get("customData");
          String val = customData.getProperty(custom, "");
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "customData_" + custom + r2, val);
        } 
        if (in_str.indexOf(String.valueOf(r1) + "ban" + r2) >= 0) {
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "ban" + r2, "");
          thisObj.ban(user_info, 0, "msg variable");
        } 
        if (in_str.indexOf(String.valueOf(r1) + "kick" + r2) >= 0) {
          in_str = Common.replace_str(in_str, String.valueOf(r1) + "kick" + r2, "");
          thisObj.passive_kick(user_info);
        } 
        if (in_str.indexOf("<SPACE>") >= 0)
          in_str = Common.space_encode(in_str); 
        if (in_str.indexOf("<FREESPACE>") >= 0)
          in_str = Common.free_space(in_str); 
        if (in_str.indexOf("<URL>") >= 0)
          in_str = Common.url_encoder(in_str); 
        if (in_str.indexOf("<REVERSE_IP>") >= 0)
          in_str = Common.reverse_ip(in_str); 
        if (in_str.indexOf("<SOUND>") >= 0)
          in_str = thisObj.common_code.play_sound(in_str); 
        if (in_str.indexOf("<LIST>") >= 0)
          in_str = thisObj.get_dir_list(in_str, the_session); 
        if (in_str.indexOf("<INCLUDE>") >= 0)
          in_str = thisObj.do_include_file_command(in_str); 
        r1 = "{";
        r2 = "}";
      } 
    } catch (Exception e) {
      Log.log("SERVER", 2, e);
    } 
    return in_str;
  }
  
  public String strip_variables(String in_str, SessionCrush the_session) {
    in_str = Common.replace_str(in_str, "<SPACE>", "");
    in_str = Common.replace_str(in_str, "<URL>", "");
    in_str = Common.replace_str(in_str, "<SPEAK>", "");
    in_str = Common.replace_str(in_str, "<SOUND>", "");
    in_str = Common.replace_str(in_str, "<LIST>", "");
    in_str = Common.replace_str(in_str, "<INCLUDE>", "");
    return in_str;
  }
  
  public String do_include_file_command(String in_str) {
    try {
      String file_name = in_str.substring(in_str.indexOf("<INCLUDE>") + 9, in_str.indexOf("</INCLUDE>"));
      RandomAccessFile includer = new RandomAccessFile(file_name, "r");
      byte[] temp_array = new byte[(int)includer.length()];
      includer.read(temp_array);
      includer.close();
      String include_data = String.valueOf(new String(temp_array)) + this.CRLF;
      return Common.replace_str(in_str, "<INCLUDE>" + file_name + "</INCLUDE>", include_data);
    } catch (Exception exception) {
      return in_str;
    } 
  }
  
  public String get_dir_list(String in_str, SessionCrush the_session) throws Exception {
    String command = in_str.substring(in_str.indexOf("<LIST>") + 6, in_str.indexOf("</LIST>"));
    String path = command.trim();
    Vector list = new Vector();
    if (!path.startsWith(the_session.user.getProperty("root_dir")))
      path = String.valueOf(the_session.user.getProperty("root_dir")) + path.substring(1); 
    the_session.uVFS.getListing(list, path);
    StringBuffer add_str = new StringBuffer();
    for (int x = 0; x < list.size(); x++) {
      Properties item = list.elementAt(x);
      LIST_handler.generateLineEntry(item, add_str, false, path, false, the_session, false);
    } 
    in_str = Common.replace_str(in_str, "<LIST>" + command + "</LIST>", add_str.toString());
    return in_str;
  }
  
  public static String parse_server_messages(String in_str) {
    Enumeration the_list = LOC.localization.keys();
    while (the_list.hasMoreElements()) {
      String cur = the_list.nextElement().toString();
      if (cur.startsWith("%") && in_str.indexOf(cur) >= 0)
        in_str = Common.replace_str(in_str, cur, SG(cur)); 
    } 
    return in_str;
  }
  
  public boolean check_hammer_ip(String ip) {
    try {
      if (Common.count_str(siSG("hammer_history"), ip) >= IG("hammer_attempts"))
        if (ban_ip(ip, IG("ban_timeout"), "hammering")) {
          try {
            Properties info = new Properties();
            info.put("alert_type", "hammering");
            info.put("alert_sub_type", "ip");
            info.put("alert_timeout", (new StringBuffer(String.valueOf(IG("ban_timeout")))).toString());
            info.put("alert_max", (new StringBuffer(String.valueOf(IG("hammer_attempts")))).toString());
            info.put("user_ip", ip);
            info.put("alert_msg", "");
            runAlerts("security_alert", info, null, null);
          } catch (Exception e) {
            Log.log("BAN", 1, e);
          } 
          siPUT("hammer_history", Common.replace_str(siSG("hammer_history"), ip, ""));
          try {
            append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---IP Banned---:" + ip, "BAN");
          } catch (Exception exception) {}
          return false;
        }  
    } catch (Exception exception) {}
    return true;
  }
  
  public boolean check_hammer_ip_http(String ip) {
    try {
      if (Common.count_str(siSG("hammer_history_http"), ip) >= IG("hammer_attempts_http"))
        if (ban_ip(ip, IG("ban_timeout_http"), "hammering http")) {
          try {
            Properties info = new Properties();
            info.put("alert_type", "hammering");
            info.put("alert_sub_type", "ip");
            info.put("alert_timeout", (new StringBuffer(String.valueOf(IG("ban_timeout")))).toString());
            info.put("alert_max", (new StringBuffer(String.valueOf(IG("hammer_attempts")))).toString());
            info.put("user_ip", ip);
            info.put("alert_msg", "");
            runAlerts("security_alert", info, null, null);
          } catch (Exception e) {
            Log.log("BAN", 1, e);
          } 
          siPUT("hammer_history_http", Common.replace_str(siSG("hammer_history_http"), ip, ""));
          try {
            append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---IP Banned---:" + ip, "BAN");
          } catch (Exception exception) {}
          return false;
        }  
    } catch (Exception exception) {}
    return true;
  }
  
  public void runAlerts(String action, SessionCrush the_user) {
    runAlerts(action, null, null, the_user, null, siBG("dmz_mode"));
  }
  
  public void runAlerts(String alert_action, Properties info, Properties user_info, SessionCrush the_user) {
    runAlerts(alert_action, info, user_info, the_user, null, siBG("dmz_mode"));
  }
  
  public void runAlerts(String alert_action, Properties info, Properties user_info, SessionCrush the_user, Properties the_alert, boolean dmz_mode) {
    Vector alerts = VG("alerts");
    if (user_info == null && the_user != null)
      user_info = the_user.user_info; 
    boolean ok = false;
    for (int x = 0; x < alerts.size() && the_alert == null; x++) {
      Properties properties = alerts.elementAt(x);
      if (properties.getProperty("type").equalsIgnoreCase("Disk Space Below Threshold") && alert_action.equals("disk")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("Variable Watcher") && alert_action.equals("variables")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("CrushFTP Update Available") && alert_action.equals("update")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("CrushFTP Started") && alert_action.equals("started")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User reached quota percentage") && alert_action.equals("user_upload_session")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Exceeded Upload Transfer Amount Per Session") && alert_action.equals("user_upload_session")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Exceeded Upload Transfer Amount Per Day") && alert_action.equals("user_upload_day")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Exceeded Upload Transfer Amount Per Month") && alert_action.equals("user_upload_month")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Exceeded Download Transfer Amount Per Session") && alert_action.equals("user_download_session")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Download Speed Below Minimum") && alert_action.equals("user_download_speed")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Upload Speed Below Minimum") && alert_action.equals("user_upload_speed")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Exceeded Download Transfer Amount Per Day") && alert_action.equals("user_download_day")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Exceeded Download Transfer Amount Per Month") && alert_action.equals("user_download_month")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("Proxy Blacklisted Site Attempted") && alert_action.equals("proxy_blacklist")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("IP Banned for Failed Logins") && alert_action.equals("ip_banned_logins")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Changed Password") && alert_action.equals("password_change")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("Server Port Failed") && alert_action.equals("server_port_error")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("Invalid Email Attempted") && alert_action.equals("invalid_email")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("User Hammering") && alert_action.equals("user_hammering")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("Plugin Message") && alert_action.startsWith("pluginMessage_")) {
        ok = true;
      } else if (properties.getProperty("type").equalsIgnoreCase("Security Alert") && alert_action.equals("security_alert")) {
        ok = true;
      } 
      if (ok) {
        the_alert = properties;
        break;
      } 
    } 
    if (the_alert != null)
      Log.log("SERVER", 0, "ALERT:Found matching alert..processing:" + the_alert.getProperty("type")); 
    Properties recent_drives = (Properties)this.server_info.get("recent_drives");
    if (recent_drives == null)
      recent_drives = new Properties(); 
    if (alert_action.equals("disk"))
      recent_drives.clear(); 
    if (the_alert == null)
      return; 
    Log.log("SERVER", 0, "ALERT:Processing alert criteria:" + the_alert.getProperty("type"));
    Properties p = the_alert;
    ok = true;
    String subject = p.getProperty("subject", "");
    String body = p.getProperty("body", "");
    String to = p.getProperty("to", "");
    String cc = p.getProperty("cc", "");
    String bcc = p.getProperty("bcc", "");
    String from = p.getProperty("from", "");
    if (p.getProperty("type").equalsIgnoreCase("Disk Space Below Threshold") && alert_action.equals("disk")) {
      String drive = p.getProperty("drive", "/");
      long mb = Long.parseLong(p.getProperty("threshold_mb", "0")) * 1024L * 1024L;
      long free_bytes = Common.get_free_disk_space(drive);
      this.server_info.put("recent_drives", recent_drives);
      recent_drives.put(drive, String.valueOf(Common.format_bytes_short(free_bytes)) + " free");
      subject = Common.replace_str(subject, "%free_bytes%", Common.format_bytes_short(free_bytes));
      body = Common.replace_str(body, "%free_bytes%", Common.format_bytes_short(free_bytes));
      if (free_bytes > mb) {
        ok = false;
        p.remove("no_email");
      } else if (p.getProperty("no_email", "").equals("true")) {
        ok = false;
      } else {
        p.put("no_email", "true");
      } 
    } else if (p.getProperty("type").equalsIgnoreCase("Variable Watcher") && alert_action.equals("variables")) {
      ok = false;
      String cond = p.getProperty("variableCondition", "equals");
      String var1 = change_vars_to_values_static(p.getProperty("variable1", ""), null, new Properties(), null);
      String var2 = change_vars_to_values_static(p.getProperty("variable2", ""), null, new Properties(), null);
      Enumeration keys = thisObj.server_info.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        String val = thisObj.server_info.getProperty(key);
        if (var1.indexOf("%server_" + key + "%") >= 0)
          var1 = Common.replace_str(var1, "%server_" + key + "%", (new StringBuffer(String.valueOf(val))).toString()); 
        if (var1.indexOf("%" + key + "%") >= 0)
          var1 = Common.replace_str(var1, "%" + key + "%", (new StringBuffer(String.valueOf(val))).toString()); 
        if (var2.indexOf("%server_" + key + "%") >= 0)
          var2 = Common.replace_str(var2, "%server_" + key + "%", (new StringBuffer(String.valueOf(val))).toString()); 
        if (var2.indexOf("%" + key + "%") >= 0)
          var2 = Common.replace_str(var2, "%" + key + "%", (new StringBuffer(String.valueOf(val))).toString()); 
      } 
      if (cond.equals("equals")) {
        if (var1.equals(var2))
          ok = true; 
      } else if (cond.equals("contains")) {
        if (var1.indexOf(var2) >= 0)
          ok = true; 
      } else if (cond.equals("matches pattern")) {
        if (Common.do_search(var2, var1, false, 0))
          ok = true; 
      } else if (cond.equals("doesn't equal") || cond.equals("!equal")) {
        if (!var1.equals(var2))
          ok = true; 
      } else if (cond.equals("doesn't contain") || cond.equals("!contain")) {
        if (var1.indexOf(var2) < 0)
          ok = true; 
      } else if (cond.equals("doesn't match pattern") || cond.equals("!match pattern")) {
        if (!Common.do_search(var2, var1, false, 0))
          ok = true; 
      } else if (cond.equals("greater than")) {
        try {
          if (Float.parseFloat(var1.trim()) > Float.parseFloat(var2.trim()))
            ok = true; 
        } catch (Exception exception) {}
      } else if (cond.equals("less than")) {
        try {
          if (Float.parseFloat(var1.trim()) < Float.parseFloat(var2.trim()))
            ok = true; 
        } catch (Exception exception) {}
      } else if (cond.equals("greater than or equal")) {
        try {
          if (Float.parseFloat(var1.trim()) >= Float.parseFloat(var2.trim()))
            ok = true; 
        } catch (Exception exception) {}
      } else if (cond.equals("less than or equal")) {
        try {
          if (Float.parseFloat(var1.trim()) <= Float.parseFloat(var2.trim()))
            ok = true; 
        } catch (Exception exception) {}
      } 
      subject = Common.replace_str(subject, "%var1%", var1);
      subject = Common.replace_str(subject, "%var2%", var2);
      subject = Common.replace_str(subject, "%condition%", cond);
      body = Common.replace_str(body, "%var1%", var1);
      body = Common.replace_str(body, "%var2%", var2);
      body = Common.replace_str(body, "%condition%", cond);
    } else if (!p.getProperty("type").equalsIgnoreCase("CrushFTP Update Available") || !alert_action.equals("update")) {
      if (!p.getProperty("type").equalsIgnoreCase("CrushFTP Started") || !alert_action.equals("started"))
        if (p.getProperty("type").equalsIgnoreCase("User reached quota percentage") && alert_action.equals("user_upload_session")) {
          long used, total, perc;
          String path = user_info.getProperty("current_dir");
          try {
            used = the_user.get_quota_used(path);
          } catch (Exception e) {
            ok = false;
            p.remove("no_email");
            used = -12345L;
          } 
          try {
            total = the_user.get_total_quota(path);
          } catch (Exception e) {
            ok = false;
            p.remove("no_email");
            total = -12345L;
          } 
          if (total != -12345L && used >= 0L) {
            perc = used * 100L;
            perc /= total;
          } else {
            perc = -1L;
          } 
          if (perc >= Integer.parseInt(p.getProperty("quota_perc", ""))) {
            body = String.valueOf(body) + "\nPercentage of quota has been reached for" + path;
          } else {
            ok = false;
          } 
        } else if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Upload Transfer Amount Per Session") || !alert_action.equals("user_upload_session")) {
          if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Upload Transfer Amount Per Day") || !alert_action.equals("user_upload_day"))
            if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Upload Transfer Amount Per Month") || !alert_action.equals("user_upload_month"))
              if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Download Transfer Amount Per Session") || !alert_action.equals("user_download_session"))
                if (!p.getProperty("type").equalsIgnoreCase("User Download Speed Below Minimum") || !alert_action.equals("user_download_speed"))
                  if (!p.getProperty("type").equalsIgnoreCase("User Upload Speed Below Minimum") || !alert_action.equals("user_upload_speed"))
                    if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Download Transfer Amount Per Day") || !alert_action.equals("user_download_day"))
                      if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Download Transfer Amount Per Month") || !alert_action.equals("user_download_month"))
                        if (!p.getProperty("type").equalsIgnoreCase("Proxy Blacklisted Site Attempted") || !alert_action.equals("proxy_blacklist"))
                          if (!p.getProperty("type").equalsIgnoreCase("IP Banned for Failed Logins") || !alert_action.equals("ip_banned_logins"))
                            if (!p.getProperty("type").equalsIgnoreCase("User Changed Password") || !alert_action.equals("password_change"))
                              if (!p.getProperty("type").equalsIgnoreCase("Server Port Failed") || !alert_action.equals("server_port_error"))
                                if (p.getProperty("type").equalsIgnoreCase("Invalid Email Attempted") && alert_action.equals("invalid_email")) {
                                  subject = Common.replace_str(subject, "%result%", info.getProperty("result", ""));
                                  subject = Common.replace_str(subject, "%subject%", info.getProperty("subject", ""));
                                  subject = Common.replace_str(subject, "%body%", info.getProperty("body", ""));
                                  subject = Common.replace_str(subject, "%to%", info.getProperty("to", ""));
                                  subject = Common.replace_str(subject, "%cc%", info.getProperty("cc", ""));
                                  subject = Common.replace_str(subject, "%bcc%", info.getProperty("bcc", ""));
                                  subject = Common.replace_str(subject, "%from%", info.getProperty("from", ""));
                                  body = Common.replace_str(body, "%result%", info.getProperty("result", ""));
                                  body = Common.replace_str(body, "%subject%", info.getProperty("subject", ""));
                                  body = Common.replace_str(body, "%body%", info.getProperty("body", ""));
                                  body = Common.replace_str(body, "%to%", info.getProperty("to", ""));
                                  body = Common.replace_str(body, "%cc%", info.getProperty("cc", ""));
                                  body = Common.replace_str(body, "%bcc%", info.getProperty("bcc", ""));
                                  body = Common.replace_str(body, "%from%", info.getProperty("from", ""));
                                  to = Common.replace_str(to, "%to%", info.getProperty("to", ""));
                                  to = Common.replace_str(to, "%cc%", info.getProperty("cc", ""));
                                  to = Common.replace_str(to, "%bcc%", info.getProperty("bcc", ""));
                                  to = Common.replace_str(to, "%from%", info.getProperty("from", ""));
                                  cc = Common.replace_str(cc, "%to%", info.getProperty("to", ""));
                                  cc = Common.replace_str(cc, "%cc%", info.getProperty("cc", ""));
                                  cc = Common.replace_str(cc, "%bcc%", info.getProperty("bcc", ""));
                                  cc = Common.replace_str(cc, "%from%", info.getProperty("from", ""));
                                  bcc = Common.replace_str(bcc, "%to%", info.getProperty("to", ""));
                                  bcc = Common.replace_str(bcc, "%cc%", info.getProperty("cc", ""));
                                  bcc = Common.replace_str(bcc, "%bcc%", info.getProperty("bcc", ""));
                                  bcc = Common.replace_str(bcc, "%from%", info.getProperty("from", ""));
                                  from = Common.replace_str(from, "%to%", info.getProperty("to", ""));
                                  from = Common.replace_str(from, "%cc%", info.getProperty("cc", ""));
                                  from = Common.replace_str(from, "%bcc%", info.getProperty("bcc", ""));
                                  from = Common.replace_str(from, "%from%", info.getProperty("from", ""));
                                } else if (p.getProperty("type").equalsIgnoreCase("User Hammering") && alert_action.equals("user_hammering")) {
                                  Properties loginsCounter = new Properties();
                                  Properties loginsUserInfos = new Properties();
                                  long now = System.currentTimeMillis();
                                  Properties sessionIds = new Properties();
                                  int xx;
                                  for (xx = 0; xx < siVG("user_list").size(); xx++) {
                                    try {
                                      Properties ui = siVG("user_list").elementAt(xx);
                                      long stamp = Long.parseLong(ui.getProperty("login_date_stamp_unique", "0"));
                                      String user_name = ui.getProperty("user_name", "");
                                      if (!user_name.equals("") && !user_name.equalsIgnoreCase("anonymous"))
                                        if (now - stamp < (Integer.parseInt(p.getProperty("login_interval", "60")) * 1000)) {
                                          String id = ui.getProperty("CrushAuth", "");
                                          if (id.equals(""))
                                            id = ui.getProperty("id"); 
                                          if (!sessionIds.containsKey(id))
                                            loginsCounter.put(user_name, (new StringBuffer(String.valueOf(Integer.parseInt(loginsCounter.getProperty(user_name, "0")) + 1))).toString()); 
                                          sessionIds.put(id, "found");
                                          loginsUserInfos.put(user_name, ui);
                                        }  
                                    } catch (Exception e) {
                                      Log.log("ALERT", 2, e);
                                    } 
                                  } 
                                  for (xx = 0; xx < siVG("recent_user_list").size(); xx++) {
                                    try {
                                      Properties ui = siVG("recent_user_list").elementAt(xx);
                                      long stamp = Long.parseLong(ui.getProperty("login_date_stamp_unique", "0"));
                                      String user_name = ui.getProperty("user_name", "");
                                      if (!user_name.equals("") && !user_name.equalsIgnoreCase("anonymous"))
                                        if (now - stamp < (Integer.parseInt(p.getProperty("login_interval", "60")) * 1000)) {
                                          String id = ui.getProperty("CrushAuth", "");
                                          if (id.equals(""))
                                            id = ui.getProperty("id"); 
                                          if (!sessionIds.containsKey(id))
                                            loginsCounter.put(user_name, (new StringBuffer(String.valueOf(Integer.parseInt(loginsCounter.getProperty(user_name, "0")) + 1))).toString()); 
                                          sessionIds.put(id, "found");
                                          loginsUserInfos.put(user_name, ui);
                                        }  
                                    } catch (Exception e) {
                                      Log.log("ALERT", 2, e);
                                    } 
                                  } 
                                  Enumeration keys = loginsCounter.keys();
                                  boolean found = false;
                                  String line = "";
                                  boolean hasLines = false;
                                  if (body.indexOf("<LINE>") >= 0) {
                                    line = body.substring(body.indexOf("<LINE>") + "<LINE>".length(), body.lastIndexOf("</LINE>"));
                                    hasLines = true;
                                  } 
                                  Properties recent_hammering = (Properties)this.server_info.get("recent_hammering");
                                  if (recent_hammering == null)
                                    recent_hammering = new Properties(); 
                                  this.server_info.put("recent_hammering", recent_hammering);
                                  recent_hammering.clear();
                                  String newLines = "";
                                  while (keys.hasMoreElements()) {
                                    String key = keys.nextElement().toString();
                                    int count = Integer.parseInt(loginsCounter.getProperty(key, "0"));
                                    if (count > Integer.parseInt(p.getProperty("login_count", "100"))) {
                                      recent_hammering.put(key, String.valueOf(count) + " logins");
                                      newLines = thisObj.change_vars_to_values(line, null, (Properties)loginsUserInfos.get(key), null);
                                      if (hasLines) {
                                        newLines = String.valueOf(newLines) + "\r\n" + key + ":" + count + "\r\n";
                                      } else {
                                        body = String.valueOf(body) + "\r\n" + key + ":" + count + "\r\n";
                                      } 
                                      found = true;
                                      try {
                                        Properties info2 = new Properties();
                                        info2.put("alert_type", "hammering");
                                        info2.put("alert_sub_type", "user");
                                        info2.put("alert_timeout", "0");
                                        info2.put("alert_max", (new StringBuffer(String.valueOf(p.getProperty("login_count", "100")))).toString());
                                        info2.put("alert_count", (new StringBuffer(String.valueOf(count))).toString());
                                        info2.put("alert_msg", hasLines ? newLines.trim() : body.trim());
                                        runAlerts("security_alert", info2, (Properties)loginsUserInfos.get(key), null, null, dmz_mode);
                                      } catch (Exception e) {
                                        Log.log("BAN", 1, e);
                                      } 
                                    } 
                                  } 
                                  body = Common.replace_str(body, "<LINE>" + line + "</LINE>", newLines).trim();
                                  if (!found)
                                    ok = false; 
                                } else if (p.getProperty("type").equalsIgnoreCase("Plugin Message") && alert_action.startsWith("pluginMessage_")) {
                                  subject = Common.replace_str(subject, "%message%", alert_action.substring(alert_action.indexOf("_") + 1));
                                  body = Common.replace_str(body, "%message%", alert_action.substring(alert_action.indexOf("_") + 1));
                                  if (p.getProperty("to").toUpperCase().startsWith("PLUGIN:")) {
                                    String plugin = p.getProperty("to").substring("plugin:".length()).trim();
                                    Vector items = new Vector();
                                    File f = new File(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/temp/");
                                    Properties item = new Properties();
                                    try {
                                      item.put("url", f.toURI().toURL().toExternalForm());
                                    } catch (Exception exception) {}
                                    item.put("the_command", alert_action);
                                    item.put("subject", subject);
                                    item.put("body", body);
                                    items.addElement(item);
                                    append_log(String.valueOf(LOC.G("ALERT:")) + p.getProperty("type") + ":" + plugin, "ERROR");
                                    Properties event = new Properties();
                                    event.put("event_plugin_list", plugin);
                                    event.put("name", "PluginMessage_Alert:" + plugin);
                                    this.events6.doEventPlugin(null, event, the_user, items);
                                    ok = false;
                                  } 
                                } else if (p.getProperty("type").equalsIgnoreCase("Security Alert") && alert_action.equals("security_alert")) {
                                  subject = Common.replace_str(subject, "{alert_type}", info.getProperty("alert_type", ""));
                                  subject = Common.replace_str(subject, "{alert_sub_type}", info.getProperty("alert_sub_type", ""));
                                  subject = Common.replace_str(subject, "{alert_timeout}", info.getProperty("alert_timeout", ""));
                                  subject = Common.replace_str(subject, "{alert_max}", info.getProperty("alert_max", ""));
                                  subject = Common.replace_str(subject, "{alert_msg}", info.getProperty("alert_msg", ""));
                                  body = Common.replace_str(body, "{alert_type}", info.getProperty("alert_type", ""));
                                  body = Common.replace_str(body, "{alert_sub_type}", info.getProperty("alert_sub_type", ""));
                                  body = Common.replace_str(body, "{alert_timeout}", info.getProperty("alert_timeout", ""));
                                  body = Common.replace_str(body, "{alert_max}", info.getProperty("alert_max", ""));
                                  body = Common.replace_str(body, "{alert_msg}", info.getProperty("alert_msg", ""));
                                  subject = Common.replace_str(subject, "%alert_type%", info.getProperty("alert_type", ""));
                                  subject = Common.replace_str(subject, "%alert_sub_type%", info.getProperty("alert_sub_type", ""));
                                  subject = Common.replace_str(subject, "%alert_timeout%", info.getProperty("alert_timeout", ""));
                                  subject = Common.replace_str(subject, "%alert_max%", info.getProperty("alert_max", ""));
                                  subject = Common.replace_str(subject, "%alert_msg%", info.getProperty("alert_msg", ""));
                                  body = Common.replace_str(body, "%alert_type%", info.getProperty("alert_type", ""));
                                  body = Common.replace_str(body, "%alert_sub_type%", info.getProperty("alert_sub_type", ""));
                                  body = Common.replace_str(body, "%alert_timeout%", info.getProperty("alert_timeout", ""));
                                  body = Common.replace_str(body, "%alert_max%", info.getProperty("alert_max", ""));
                                  body = Common.replace_str(body, "%alert_msg%", info.getProperty("alert_msg", ""));
                                } else {
                                  ok = false;
                                }            
        }  
    } 
    if (ok) {
      if (siBG("dmz_mode") && the_alert != null) {
        Vector queue = (Vector)Common.System2.get("crushftp.dmz.queue");
        Properties action = new Properties();
        action.put("type", "GET:SINGLETON");
        action.put("id", Common.makeBoundary());
        action.put("need_response", "true");
        queue.addElement(action);
        action = UserTools.waitResponse(action, 30);
        Object object = "";
        if (action != null)
          object = action.get("singleton_id"); 
        action = new Properties();
        action.put("type", "PUT:ALERT");
        action.put("id", Common.makeBoundary());
        action.put("alert_action", alert_action);
        if (info != null)
          action.put("info", info); 
        if (user_info != null)
          action.put("user_info", user_info); 
        action.put("singleton_id", object);
        action.put("alert", the_alert);
        queue.addElement(action);
        Log.log("SERVER", 0, "ALERT:DMZ sent alert to internal for processing:" + the_alert.getProperty("type"));
      } 
      if (!BG("run_alerts_dmz") && siBG("dmz_mode"))
        return; 
      Log.log("SERVER", 0, "ALERT:Processing alert:" + p.getProperty("type"));
      Log.log("SERVER", 2, "ALERT:Processing alert:" + p);
      Properties user = null;
      if (the_user != null)
        user = the_user.user; 
      to = change_vars_to_values(to, user, user_info, the_user);
      cc = change_vars_to_values(cc, user, user_info, the_user);
      bcc = change_vars_to_values(bcc, user, user_info, the_user);
      from = change_vars_to_values(from, user, user_info, the_user);
      subject = change_vars_to_values(subject, user, user_info, the_user);
      body = change_vars_to_values(body, user, user_info, the_user);
      Enumeration keys = thisObj.server_info.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        String val = thisObj.server_info.getProperty(key);
        if (body.indexOf("%server_" + key + "%") >= 0)
          body = Common.replace_str(body, "%server_" + key + "%", (new StringBuffer(String.valueOf(val))).toString()); 
        if (body.indexOf("%" + key + "%") >= 0)
          body = Common.replace_str(body, "%" + key + "%", (new StringBuffer(String.valueOf(val))).toString()); 
        if (subject.indexOf("%server_" + key + "%") >= 0)
          subject = Common.replace_str(subject, "%server_" + key + "%", (new StringBuffer(String.valueOf(val))).toString()); 
        if (subject.indexOf("%" + key + "%") >= 0)
          subject = Common.replace_str(subject, "%" + key + "%", (new StringBuffer(String.valueOf(val))).toString()); 
      } 
      if (!p.getProperty("alert_plugin", "").equals("")) {
        Properties item = (Properties)p.clone();
        if (the_user != null)
          item.putAll(the_user.user); 
        if (user_info != null)
          item.putAll(user_info); 
        item.putAll(p);
        item.put("url", "file://" + p.getProperty("type") + "/" + p.getProperty("name"));
        item.put("the_file_name", subject);
        item.put("the_file_path", body);
        item.put("to", to);
        item.put("cc", cc);
        item.put("bcc", bcc);
        item.put("from", from);
        item.put("subject", subject);
        item.put("dmz_mode", (new StringBuffer(String.valueOf(dmz_mode))).toString());
        item.put("body", body);
        if (info != null)
          item.putAll(info); 
        Vector items = new Vector();
        items.addElement(item);
        Properties event = new Properties();
        event.put("event_plugin_list", p.getProperty("alert_plugin", ""));
        event.put("name", "AlertPlugin:" + p.getProperty("type"));
        this.events6.doEventPlugin(null, event, the_user, items);
      } 
      if (p.get("hours") == null)
        p.put("hours", new Properties()); 
      Properties hours = (Properties)p.get("hours");
      SimpleDateFormat hh = new SimpleDateFormat("HH", Locale.US);
      String HH = hh.format(new Date());
      int curHH = Integer.parseInt(hours.getProperty(HH, "0"));
      if (curHH >= Integer.parseInt(p.getProperty("max_alert_emails", "60")))
        ok = false; 
      if (ok && !to.equals("")) {
        int xx;
        for (xx = 0; xx < 10; xx++)
          hours.put("0" + xx, "0"); 
        for (xx = 10; xx < 25; xx++)
          hours.put(xx, "0"); 
        hours.put(HH, (new StringBuffer(String.valueOf(curHH + 1))).toString());
        String emailResult = Common.send_mail(SG("discovered_ip"), to, cc, bcc, from, subject, body, SG("smtp_server"), SG("smtp_user"), SG("smtp_pass"), BG("smtp_ssl"), BG("smtp_html"), (File_B[])null);
        if (emailResult.toUpperCase().indexOf("SUCCESS") < 0) {
          Log.log("SMTP", 0, String.valueOf(LOC.G("FAILURE:")) + " " + emailResult + "\r\n");
          Log.log("SMTP", 0, String.valueOf(LOC.G("FROM:")) + " " + change_vars_to_values(p.getProperty("from"), user, user_info, the_user) + "\r\n");
          Log.log("SMTP", 0, String.valueOf(LOC.G("TO:")) + " " + change_vars_to_values(p.getProperty("to"), user, user_info, the_user) + "\r\n");
          Log.log("SMTP", 0, String.valueOf(LOC.G("CC:")) + " " + change_vars_to_values(p.getProperty("cc"), user, user_info, the_user) + "\r\n");
          Log.log("SMTP", 0, String.valueOf(LOC.G("BCC:")) + " " + change_vars_to_values(p.getProperty("bcc"), user, user_info, the_user) + "\r\n");
          Log.log("SMTP", 0, String.valueOf(LOC.G("SUBJECT:")) + " " + change_vars_to_values(p.getProperty("subject"), user, user_info, the_user) + "\r\n");
          Log.log("SMTP", 0, String.valueOf(LOC.G("BODY:")) + " " + change_vars_to_values(p.getProperty("body"), user, user_info, the_user) + "\r\n");
        } 
      } 
    } 
  }
  
  public void sendEmail(Properties p) {
    String results = Common.send_mail(server_settings.getProperty("discovered_ip"), p.getProperty("to", ""), p.getProperty("cc", ""), p.getProperty("bcc", ""), p.getProperty("from", ""), p.getProperty("reply_to", ""), p.getProperty("subject", ""), p.getProperty("body", ""), p.getProperty("server", ""), p.getProperty("user", ""), p.getProperty("pass", ""), p.getProperty("ssl", "").equals("true"), p.getProperty("html", "").equals("true"), null);
    p.put("results", results);
    if (siVG("server_queue") != null)
      siVG("server_queue").addElement(p); 
    if (results.toUpperCase().indexOf("SUCCESS") < 0) {
      Properties m = new Properties();
      m.put("result", results);
      m.put("body", p.getProperty("body", ""));
      m.put("subject", p.getProperty("subject", ""));
      m.put("to", p.getProperty("to", ""));
      m.put("from", p.getProperty("from", ""));
      m.put("reply_to", p.getProperty("reply_to", ""));
      m.put("cc", p.getProperty("cc", ""));
      m.put("bcc", p.getProperty("bcc", ""));
      thisObj.runAlerts("invalid_email", m, null, null);
    } 
  }
  
  public static void checkServerGroups() {
    Vector sgs = (Vector)server_settings.get("server_groups");
    boolean addItems = (sgs.size() == 0);
    String lastServerGroupName = "MainUsers";
    File[] f = (new File(System.getProperty("crushftp.users"))).listFiles();
    if (f != null)
      for (int j = 0; j < f.length; j++) {
        if (!f[j].getName().equals("extra_vfs") && 
          f[j].isDirectory() && (f[j].listFiles()).length > 0) {
          if (addItems)
            sgs.addElement(f[j].getName()); 
          lastServerGroupName = f[j].getName();
        } 
      }  
    if (sgs.size() == 0)
      sgs.addElement("MainUsers"); 
    for (int x = sgs.size() - 1; x >= 0; x--) {
      if (sgs.elementAt(x).toString().equals("extra_vfs"))
        sgs.remove(x); 
    } 
    Vector pref_server_items = (Vector)server_settings.get("server_list");
    for (int i = 0; i < pref_server_items.size(); i++) {
      Properties p = pref_server_items.elementAt(i);
      if (p.getProperty("linkedServer", "").equals(""))
        p.put("linkedServer", lastServerGroupName); 
    } 
  }
  
  public static int IG(String data) {
    if (data != null && (data.equals("max_server_upload_speed") || data.equals("max_server_download_speed"))) {
      String[] intervals = SG(data).split(";");
      SimpleDateFormat HHmm = new SimpleDateFormat("HHmm");
      int now = Integer.parseInt(HHmm.format(new Date()));
      String last_interval = null;
      for (int x = 0; x < intervals.length; x++) {
        if (intervals[x].indexOf(":") < 0 && intervals[x].length() > 0) {
          last_interval = "0:" + intervals[x];
        } else if (intervals[x].indexOf(":") >= 0) {
          String interval = intervals[x].split(":")[0];
          if (last_interval == null || Integer.parseInt(interval) <= now)
            last_interval = intervals[x]; 
        } 
      } 
      try {
        return Integer.parseInt(last_interval.split(":")[1]);
      } catch (Exception exception) {}
    } 
    try {
      return Integer.parseInt(SG(data));
    } catch (Exception exception) {
      return 0;
    } 
  }
  
  public static long LG(String data) {
    try {
      return Long.parseLong(SG(data));
    } catch (Exception exception) {
      return 0L;
    } 
  }
  
  public static boolean BG(String data) {
    if (thisObj == null)
      return false; 
    return SG(data).toLowerCase().equals("true");
  }
  
  public static Vector VG(String data) {
    if (thisObj == null)
      return null; 
    return (Vector)server_settings.get(data);
  }
  
  public static String SG(String data) {
    if (thisObj == null)
      return null; 
    if (server_settings.containsKey(data))
      return server_settings.getProperty(data); 
    if (LOC.localization.containsKey(data))
      return LOC.localization.getProperty(data); 
    if (!thisObj.default_settings.containsKey(data))
      return data; 
    server_settings.put(data, thisObj.default_settings.getProperty(data));
    return thisObj.default_settings.getProperty(data);
  }
  
  public static int siIG(String data) {
    try {
      return Integer.parseInt(siSG(data));
    } catch (Exception exception) {
      return 0;
    } 
  }
  
  public static long siLG(String data) {
    try {
      return Long.parseLong(siSG(data));
    } catch (Exception exception) {
      return 0L;
    } 
  }
  
  public static boolean siBG(String data) {
    return siSG(data).toLowerCase().equals("true");
  }
  
  public static void siPUT(String key, Object val) {
    thisObj.server_info.put(key, val);
  }
  
  public static void siPUT2(String key, Object val) {
    thisObj.server_info.put(key, val);
    server_settings.put(key, val);
  }
  
  public static String siSG(String data) {
    return thisObj.server_info.getProperty(data, "");
  }
  
  public static Vector siVG(String data) {
    return (Vector)thisObj.server_info.get(data);
  }
  
  public static Properties siPG(String data) {
    return (Properties)thisObj.server_info.get(data);
  }
  
  public static Object siOG(String data) {
    return thisObj.server_info.get(data);
  }
}
