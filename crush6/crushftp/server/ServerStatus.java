package crushftp.server;

import com.crushftp.client.CommandBufferFlusher;
import com.crushftp.client.Common;
import com.crushftp.tunnel2.Tunnel2;
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
import crushftp.handlers.SharedSession;
import crushftp.handlers.ShutdownHandler;
import crushftp.handlers.SyncTools;
import crushftp.handlers.UpdateHandler;
import crushftp.handlers.UpdateTimer;
import crushftp.handlers.UserTools;
import crushftp.handlers.log.LoggingProviderDisk;
import crushftp.license.Maverick;
import crushftp.reports5.ReportTools;
import crushftp.server.daemon.GenericServer;
import crushftp.server.ssh.SSHDaemon;
import crushftp.user.SQLUsers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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

public class ServerStatus {
  public static String sub_version_info_str = "_32";
  
  public static String version_info_str = "Version 6.5.0";
  
  public static ClassLoader clasLoader = null;
  
  public static ServerStatus thisObj = null;
  
  public static Properties server_settings = new Properties();
  
  public Properties server_info = new Properties();
  
  public Date server_start_time = new Date();
  
  public Vector commandBuffer = new Vector();
  
  public StatTools statTools = new StatTools();
  
  public SearchTools searchTools = new SearchTools();
  
  public Events6 events6 = new Events6();
  
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
  
  public Thread hammer_timer_thread = null;
  
  public Thread ban_timer_thread = null;
  
  public Thread phammer_timer_thread = null;
  
  public Thread cban_timer_thread = null;
  
  public Thread discover_ip_timer_thread = null;
  
  public Thread log_rolling_thread = null;
  
  public Thread events_thread = null;
  
  public Thread monitor_folders_thread = null;
  
  public Thread monitor_folders_thread_instant = null;
  
  public Thread http_cleaner_thread = null;
  
  public Thread update_2_timer_thread = null;
  
  public Thread command_timer_thread = null;
  
  public Thread expireThread = null;
  
  public Vector main_servers = new Vector();
  
  public String CRLF = "\r\n";
  
  public Vector server_download_queue = new Vector();
  
  public Vector server_upload_queue = new Vector();
  
  public Properties dayofweek = new Properties();
  
  public Common common_code = new Common();
  
  public Properties default_settings = new Properties();
  
  public Vector previewWorkers = new Vector();
  
  public SimpleDateFormat logDateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
  
  SimpleDateFormat weekday = new SimpleDateFormat("EEE", Locale.US);
  
  SimpleDateFormat MM = new SimpleDateFormat("MM");
  
  public Object eventLock = new Object();
  
  public Vector availableWorkers = new Vector();
  
  public Vector busyWorkers = new Vector();
  
  public boolean starting = true;
  
  ShutdownHandler shutdown = new ShutdownHandler();
  
  LoggingProvider loggingProvider = null;
  
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
    this.server_info.put("recent_user_list", new Vector());
    this.server_info.put("invalid_usernames", new Properties());
    this.server_info.put("running_tasks", new Vector());
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
  }
  
  public static boolean killUpdateFiles() {
    killJar("log4j-1.2.6.jar", "log4j.jar");
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
        if (v != null && (v.equals("4") || v.equals("5"))) {
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
    siPUT("server_start_time", this.server_start_time);
    siPUT("current_download_speed", "0");
    siPUT("current_upload_speed", "0");
    siPUT("last_day_of_week", this.weekday.format(new Date()));
    siPUT("last_month", this.MM.format(new Date()));
    siPUT("hammer_history", "");
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
    boolean needSave = false;
    try {
      if (this.loggingProvider != null)
        this.loggingProvider.shutdown(); 
      this.loggingProvider = (LoggingProvider)Class.forName(SG("logging_provider")).newInstance();
    } catch (Exception e) {
      this.loggingProvider = new LoggingProviderDisk();
      e.printStackTrace();
      Log.log("SERVER", 0, e);
    } 
    if (VG("plugins") != null)
      for (int i = VG("plugins").size() - 1; i >= 0; i--) {
        try {
          Vector subitems = VG("plugins").elementAt(i);
          Properties p = subitems.elementAt(0);
          if (p.getProperty("pluginName").equalsIgnoreCase("mm.mysql-2.0.14-bin") || p.getProperty("pluginName").equalsIgnoreCase("mysql-connector-java-5.0.4-bin")) {
            VG("plugins").removeElementAt(i);
            needSave = true;
          } else if (!(new File(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/" + p.getProperty("pluginName") + ".jar")).exists()) {
            VG("plugins").removeElementAt(i);
            needSave = true;
          } 
        } catch (Exception exception) {}
      }  
    if (!SG("prefs_version").startsWith("6")) {
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
        server_settings.put("smtp_pass", this.common_code.encode_pass(SG("smtp_pass"), "DES")); 
      if (!SG("search_db_pass").equals(""))
        server_settings.put("search_db_pass", this.common_code.encode_pass(SG("search_db_pass"), "DES")); 
      if (!SG("db_pass").equals(""))
        server_settings.put("db_pass", this.common_code.encode_pass(SG("db_pass"), "DES")); 
      if (!SG("syncs_db_pass").equals(""))
        server_settings.put("syncs_db_pass", this.common_code.encode_pass(SG("syncs_db_pass"), "DES")); 
      if (!SG("stats_db_pass").equals(""))
        server_settings.put("stats_db_pass", this.common_code.encode_pass(SG("stats_db_pass"), "DES")); 
      if (!SG("filter1").equals("") && !SG("filter1").equals("filter1"))
        server_settings.put("globalKeystoreCertPass", server_settings.remove("filter1")); 
      if (!SG("filter2").equals("") && !SG("filter2").equals("filter2"))
        server_settings.put("globalKeystorePass", server_settings.remove("filter2")); 
      if (SG("log_allow_str").equals("(ERROR)(START)(STOP)(QUIT_SERVER)(RUN_SERVER)(KICK)(BAN)(DENIAL)(ACCEPT)(DISCONNECT)(USER)(PASS)(SYST)(NOOP)(SIZE)(MDTM)(RNFR)(RNTO)(PWD)(CWD)(TYPE)(REST)(DELE)(MKD)(RMD)(MACB)(ABOR)(RETR)(STOR)(APPE)(LIST)(NLST)(CDUP)(PASV)(PORT)(AUTH)(PBSZ)(PROT)(SITE)(QUIT)(GET)(PUT)(DELETE)(MOVE)(STAT)(HELP)(PAUSE_RESUME)(PROXY)"))
        server_settings.put("log_allow_str", this.default_settings.getProperty("log_allow_str")); 
      for (int i = VG("plugins").size() - 1; i >= 0; i--) {
        try {
          Vector subitems = VG("plugins").elementAt(i);
          for (int xx = 0; xx < subitems.size(); xx++) {
            Properties p = subitems.elementAt(xx);
            if (p.getProperty("pluginName").equalsIgnoreCase("CrushNOIP")) {
              if (!p.getProperty("pass", "").equals(""))
                p.put("pass", this.common_code.encode_pass(p.getProperty("pass"), "DES")); 
            } else if (p.getProperty("pluginName").equalsIgnoreCase("CrushSQL")) {
              if (!p.getProperty("db_pass", "").equals(""))
                p.put("db_pass", this.common_code.encode_pass(p.getProperty("db_pass"), "DES")); 
            } else if (p.getProperty("pluginName").equalsIgnoreCase("CrushTask")) {
              Vector tasks = (Vector)p.get("tasks");
              for (int xxx = 0; tasks != null && xxx < tasks.size(); xxx++) {
                Properties t = tasks.elementAt(xxx);
                if (t.getProperty("type", "").equalsIgnoreCase("HTTP")) {
                  if (!t.getProperty("password", "").equals(""))
                    t.put("password", this.common_code.encode_pass(t.getProperty("password"), "DES")); 
                } else if (t.getProperty("type", "").equalsIgnoreCase("FIND") || t.getProperty("type", "").equalsIgnoreCase("COPY")) {
                  if (!t.getProperty("ssh_private_key_pass", "").equals(""))
                    t.put("ssh_private_key_pass", this.common_code.encode_pass(t.getProperty("ssh_private_key_pass"), "DES")); 
                } else if (t.getProperty("type", "").equalsIgnoreCase("PGP")) {
                  if (!t.getProperty("key_password", "").equals(""))
                    t.put("key_password", this.common_code.encode_pass(t.getProperty("key_password"), "DES")); 
                } else if (t.getProperty("type", "").equalsIgnoreCase("PopImap")) {
                  if (!t.getProperty("mail_pass", "").equals(""))
                    t.put("mail_pass", this.common_code.encode_pass(t.getProperty("mail_pass"), "DES")); 
                } 
              } 
            } else if (p.getProperty("pluginName").equalsIgnoreCase("PostBack")) {
              if (!p.getProperty("password", "").equals(""))
                p.put("password", this.common_code.encode_pass(p.getProperty("password"), "DES")); 
            } 
          } 
        } catch (Exception e) {
          Log.log("PLUGIN", 0, e);
        } 
      } 
      Vector pref_server_items = (Vector)server_settings.get("server_list");
      for (int j = 0; j < pref_server_items.size(); j++) {
        Properties p = pref_server_items.elementAt(j);
        if (p.getProperty("serverType", "FTP").equalsIgnoreCase("SFTP"))
          SSHDaemon.setupDaemon(p); 
      } 
    } 
    if (!SG("prefs_version").equals("6.3") && !siBG("dmz_mode"))
      if (Common.OSXApp()) {
        try {
          RandomAccessFile raf = new RandomAccessFile("../../Info.plist", "rw");
          byte[] b = new byte[(int)raf.length()];
          raf.read(b);
          String s = new String(b, "UTF8");
          s = Common.replace_str(s, "$JAVAROOT/CrushFTP.jar", "$JAVAROOT/plugins/lib/CrushFTPJarProxy.jar");
          raf.setLength(0L);
          raf.write(s.getBytes("UTF8"));
          raf.close();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
        } 
        needSave = true;
        server_settings.put("prefs_version", "6.3");
      } else if (Common.machine_is_windows() && (new File("./service/wrapper.conf")).exists()) {
        try {
          RandomAccessFile raf = new RandomAccessFile("./service/wrapper.conf", "rw");
          byte[] b = new byte[(int)raf.length()];
          raf.read(b);
          String s = new String(b, "UTF8");
          if (s.indexOf("=CrushFTP.jar") >= 0) {
            s = Common.replace_str(s, "=CrushFTP.jar", "=plugins/lib/CrushFTPJarProxy.jar");
            raf.setLength(0L);
            raf.write(s.getBytes("UTF8"));
          } 
          raf.close();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
        } 
        needSave = true;
        server_settings.put("prefs_version", "6.3");
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
    append_log(String.valueOf(thisObj.logDateFormat.format(new Date())) + "|********" + LOC.G("CrushFTP Run") + "******** " + siSG("version_info_str") + siSG("sub_version_info_str") + "  Java:" + System.getProperty("java.version") + " from:" + System.getProperty("java.home") + " " + System.getProperty("sun.arch.data.model") + " bit  OS:" + System.getProperties().getProperty("os.name"), "RUN_SERVER");
    try {
      String ipList = "";
      Vector allow_list = (Vector)server_settings.get("ip_restrictions");
      for (int i = allow_list.size() - 1; i >= 0; i--) {
        Properties ip_data = allow_list.elementAt(i);
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
    checkServerGroups();
    this.total_server_bytes_sent = LG("total_server_bytes_sent");
    this.total_server_bytes_received = LG("total_server_bytes_received");
    if (!System.getProperty("crushftp.previews", "").equals(""))
      server_settings.put("previews_path", System.getProperty("crushftp.previews", "")); 
    reset_threads(start_threads);
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
      setup_update_2_timer();
      setup_stats_saver();
      setup_report_scheduler();
      setup_scheduler();
      setup_alerts();
      setup_new_version();
      this.common_code.loadPlugins(server_settings, this.server_info);
    } 
  }
  
  public void reset_server_login_counts() {
    this.server_info.put("successful_logins", "0");
    this.server_info.put("failed_logins", "0");
    server_settings.put("successful_logins", "0");
    server_settings.put("failed_logins", "0");
    thisObj.save_server_settings(false);
  }
  
  public void reset_server_bytes_in_out() {
    this.total_server_bytes_sent = 0L;
    this.total_server_bytes_received = 0L;
    this.server_last_bytes_sent = 0L;
    this.server_last_bytes_received = 0L;
    server_settings.put("total_server_bytes_sent", "0");
    server_settings.put("total_server_bytes_received", "0");
    thisObj.save_server_settings(false);
  }
  
  public void reset_upload_download_counter() {
    this.server_info.put("downloaded_files", "0");
    this.server_info.put("uploaded_files", "0");
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
    UpdateTimer the_thread = new UpdateTimer(this, 60000, "ServerStatus", "log_rolling");
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
    this.hammer_timer_thread = null;
    UpdateTimer the_thread = new UpdateTimer(this, IG("hammer_banning") * 1000, "ServerStatus", "hammer_timer");
    this.hammer_timer_thread = new Thread(the_thread);
    this.hammer_timer_thread.setName("ServerStatus:hammer_timer");
    this.hammer_timer_thread.setPriority(1);
    this.hammer_timer_thread.start();
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
    UpdateTimer the_thread = new UpdateTimer(this, 40000, "ServerStatus", "schedules");
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
    siVG("user_list").addElement(user_info);
    siPUT("logged_in_users", siVG("user_list").size());
    updateConcurrentUsers();
  }
  
  public void updateConcurrentUsers() {
    Properties uniqueSessions = new Properties();
    Properties concurrentUsers = new Properties();
    synchronized (siVG("user_list")) {
      for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
        Properties p = siVG("user_list").elementAt(x);
        ServerSession theSession = (ServerSession)p.get("session");
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
  }
  
  public int count_users(ServerSession this_user) {
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
  
  public static int count_users_ip(ServerSession this_user, String protocol) {
    int num_users = 0;
    for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        Properties p = siVG("user_list").elementAt(x);
        if (p.getProperty("user_name").equalsIgnoreCase(this_user.uiSG("user_name")) && p.getProperty("user_ip").equalsIgnoreCase(this_user.uiSG("user_ip")))
          if (protocol == null || (protocol != null && p.getProperty("user_protocol").equals(protocol)))
            num_users++;  
      } catch (Exception exception) {}
    } 
    return num_users;
  }
  
  public boolean kill_first_same_name_same_ip(Properties user_info) {
    for (int x = 0; x < siVG("user_list").size(); x++) {
      try {
        Properties p = siVG("user_list").elementAt(x);
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
    for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        Properties p = siVG("user_list").elementAt(x);
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
  
  public synchronized void remove_user(Properties user_info, boolean decrementServerCount) {
    ServerSession session = null;
    boolean removed = false;
    try {
      for (int x = 0; x < this.main_servers.size(); x++) {
        GenericServer s = this.main_servers.elementAt(x);
        if (s.server_item.getProperty("ip").equals(((ServerSession)user_info.get("session")).server_item.getProperty("ip")) && s.server_item.getProperty("port").equals(((ServerSession)user_info.get("session")).server_item.getProperty("port")))
          synchronized (s) {
            if (s.connected_users > 0 && decrementServerCount)
              s.connected_users--; 
            s.updateStatus();
          }  
      } 
    } catch (Exception exception) {}
    try {
      session = (ServerSession)user_info.get("session");
      user_info.remove("session");
      removed = siVG("user_list").remove(user_info);
      user_info.put("root_dir", session.user.getProperty("root_dir"));
    } catch (Exception exception) {}
    siPUT("logged_in_users", siVG("user_list").size());
    updateConcurrentUsers();
    try {
      if (removed) {
        siVG("recent_user_list").addElement(user_info);
        if (BG("user_log_disk")) {
          Vector user_log = (Vector)user_info.get("user_log");
          StringBuffer sb = new StringBuffer();
          (new File(String.valueOf((new File(change_vars_to_values_static(SG("log_location"), null, null, null))).getCanonicalFile().getParentFile().getPath()) + "/session_logs/")).mkdirs();
          for (int x = 0; x < user_log.size(); x++)
            sb.append(user_log.elementAt(x).toString()).append("\r\n"); 
          Common.copyStreams(new ByteArrayInputStream(sb.toString().getBytes("UTF8")), new FileOutputStream(new File(String.valueOf((new File(change_vars_to_values_static(SG("log_location"), null, null, null))).getCanonicalFile().getParentFile().getPath()) + "/session_logs/session_" + user_info.getProperty("user_name") + "_" + user_info.getProperty("user_number") + ".log")), true, true);
        } 
        user_info.put("root_dir", "/");
        try {
          user_info.put("root_dir", session.user.getProperty("root_dir"));
        } catch (NullPointerException nullPointerException) {}
      } 
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
    } 
  }
  
  public int getTotalConnectedUsers() {
    int total = 0;
    try {
      total = siIG("concurrent_users");
    } catch (Exception exception) {}
    return total;
  }
  
  public void stop_all_servers() {
    for (int x = this.main_servers.size() - 1; x >= 0; x--)
      stop_this_server(x); 
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
        put_in("discovered_ip", InetAddress.getLocalHost().getHostAddress());
      } catch (Exception exception) {} 
    try {
      append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Server Started") + "--- LAN IP=" + ip + " WAN IP=" + SG("discovered_ip") + " PORT=" + the_port, "START");
    } catch (Exception exception) {}
  }
  
  public void update_now(String arg) throws Exception {
    if (arg.equals("hammer_timer")) {
      siPUT("hammer_history", "");
    } else if (arg.equals("phammer_timer")) {
      Properties ips = new Properties();
      int x;
      for (x = 0; x < siVG("user_list").size(); x++) {
        try {
          Properties p = siVG("user_list").elementAt(x);
          Vector password_attempts = (Vector)p.get("password_attempts");
          if (ips.get(p.getProperty("user_ip")) == null) {
            ips.put(p.getProperty("user_ip"), new Vector());
            ((Vector)ips.get(p.getProperty("user_ip"))).add(p);
          } 
          ((Vector)ips.get(p.getProperty("user_ip"))).addAll(password_attempts);
        } catch (Exception exception) {}
      } 
      for (x = 0; x < siVG("recent_user_list").size(); x++) {
        try {
          Properties p = siVG("recent_user_list").elementAt(x);
          Vector password_attempts = (Vector)p.get("password_attempts");
          if (ips.get(p.getProperty("user_ip")) == null) {
            ips.put(p.getProperty("user_ip"), new Vector());
            ((Vector)ips.get(p.getProperty("user_ip"))).add(p);
          } 
          ((Vector)ips.get(p.getProperty("user_ip"))).addAll(password_attempts);
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
          if (ban((Properties)password_attempts2.elementAt(0), IG("pban_timeout"), true)) {
            try {
              append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Kicking session because of password hammer trigger.") + "---", "KICK");
            } catch (Exception exception) {}
            Properties user_info = (Properties)password_attempts2.elementAt(0);
            ServerSession thisSession = null;
            try {
              thisSession = (ServerSession)user_info.get("session");
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
      JobScheduler.runSchedules(new Properties());
    } else if (arg.equals("alerts")) {
      runAlerts("disk", null);
    } else if (arg.equals("new_version")) {
      if (SG("newversion") == null || BG("newversion")) {
        try {
          Thread.sleep(1000L);
        } catch (Exception exception) {}
        Properties p = new Properties();
        p.put("version", version_info_str);
        Common.checkForUpdate(p);
        if (!p.getProperty("version", "").equals(version_info_str)) {
          this.server_info.put("update_available", "true");
          this.server_info.put("update_available_version", p.getProperty("version"));
          this.server_info.put("update_available_html", p.getProperty("html"));
          runAlerts("update", null);
        } 
        try {
          Thread.sleep(604800000L);
        } catch (Exception exception) {}
      } 
    } else if (arg.equals("stats_saver")) {
      Thread.sleep(10000L);
      Log.log("SERVER", 0, "Server Memory Stats: Max=" + Common.format_bytes_short(siLG("ram_max")) + ", Free=" + Common.format_bytes_short(siLG("ram_free")));
      Calendar c = new GregorianCalendar();
      c.setTime(new Date());
      c.add(5, IG("stats_transfer_days") * -1);
      Vector transfer_rids = this.statTools.executeSqlQuery(SG("stats_get_transfers_time"), new Object[] { c.getTime() }, false);
      Log.log("STAT", 2, "Stats Transfer Cleanup: Deleting " + transfer_rids.size() + " sessions.");
      String transferRidsStr = "";
      for (int x = 0; x < transfer_rids.size(); x++) {
        Properties p = transfer_rids.elementAt(x);
        transferRidsStr = String.valueOf(transferRidsStr) + p.getProperty("RID") + ",";
      } 
      if (transferRidsStr.length() > 0) {
        transferRidsStr = transferRidsStr.substring(0, transferRidsStr.length() - 1);
        String deleteMetaInfoSql = "DELETE FROM META_INFO WHERE TRANSFER_RID IN (" + transferRidsStr + ")";
        this.statTools.executeSql(deleteMetaInfoSql, new Object[0]);
      } 
      this.statTools.executeSql(SG("stats_delete_transfers_time"), new Object[] { c.getTime() });
      c = new GregorianCalendar();
      c.setTime(new Date());
      c.add(5, IG("stats_session_days") * -1);
      Vector session_rids = this.statTools.executeSqlQuery(SG("stats_get_sessions_time"), new Object[] { c.getTime() }, false);
      Log.log("STAT", 2, "Stats Session Cleanup: Deleting " + session_rids.size() + " sessions.");
      String sessionRidsStr = "";
      int i;
      for (i = 0; i < session_rids.size(); i++) {
        Properties p = session_rids.elementAt(i);
        sessionRidsStr = String.valueOf(sessionRidsStr) + p.getProperty("RID") + ",";
      } 
      transfer_rids = new Vector();
      if (sessionRidsStr.length() > 0) {
        sessionRidsStr = sessionRidsStr.substring(0, sessionRidsStr.length() - 1);
        transfer_rids = this.statTools.executeSqlQuery(Common.replace_str(SG("stats_get_transfers_sessions"), "%sessions%", sessionRidsStr), new Object[0], false);
      } 
      transferRidsStr = "";
      for (i = 0; i < transfer_rids.size(); i++) {
        Properties p = transfer_rids.elementAt(i);
        transferRidsStr = String.valueOf(transferRidsStr) + p.getProperty("RID") + ",";
      } 
      if (transferRidsStr.length() > 0) {
        transferRidsStr = transferRidsStr.substring(0, transferRidsStr.length() - 1);
        this.statTools.executeSql(Common.replace_str(SG("stats_delete_meta_transfers"), "%transfers%", transferRidsStr), new Object[0]);
      } 
      this.statTools.executeSql(SG("stats_delete_sessions_time"), new Object[] { c.getTime() });
      checkCrushExpiration();
      SharedSession.flush();
    } else if (arg.equals("ban_timer")) {
      Vector ip_vec = (Vector)server_settings.get("ip_restrictions");
      this.common_code.remove_expired_bans(ip_vec);
      server_settings.put("ip_restrictions", ip_vec);
    } else if (arg.equals("cban_timer")) {
      Vector kick_list = new Vector();
      int x = 0;
      while (x < siVG("user_list").size()) {
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
              if (ban_ip(ip, IG("cban_timeout"), false)) {
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
      } 
      for (int xxx = 0; xxx < kick_list.size(); xxx++) {
        try {
          append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Kicking sessions because of too many failed commands.") + "---", "KICK");
        } catch (Exception exception) {}
        kick(kick_list.elementAt(xxx));
      } 
      Thread.sleep(5000L);
    } else if (arg.equals("discover_ip_timer")) {
      if (BG("auto_ip_discovery"))
        update_ip(); 
    } else if (arg.equals("update_2_timer")) {
      while (siVG("running_tasks").size() > IG("running_task_max"))
        siVG("running_tasks").remove(0); 
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
                the_server.put("customKeystorePass", this.common_code.encode_pass(the_server.getProperty("customKeystorePass"), "DES"));
                the_server.put("customKeystoreCertPass", this.common_code.encode_pass(the_server.getProperty("customKeystoreCertPass"), "DES"));
              } 
            } 
            setup_hammer_banning();
            setup_ban_timer();
            setup_discover_ip_refresh();
            setup_log_rolling();
            setup_http_cleaner();
            setup_stats_saver();
            setup_report_scheduler();
            if (doServerBounce)
              start_all_servers(); 
            this.server_info.put("currentFileDate", (new StringBuffer(String.valueOf(this.prefsProvider.getPrefsTime(null)))).toString());
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
          if (siVG("user_list").indexOf(siOG("waiting_quit_user_name")) < 0) {
            save_server_settings(false);
            quit_server();
          } 
        } catch (Exception exception) {} 
      if (siOG("waiting_restart_user_name") != null)
        try {
          if (siVG("user_list").indexOf(siOG("waiting_restart_user_name")) < 0)
            restart_crushftp(); 
        } catch (Exception exception) {} 
      System.getProperties().put("java.net.preferIPv4Stack", (new StringBuffer(String.valueOf(BG("force_ipv4")))).toString());
      System.getProperties().put("crushftp.debug", (new StringBuffer(String.valueOf(IG("log_debug_level")))).toString());
      System.getProperties().put("crushftp.lsla", (new StringBuffer(String.valueOf(BG("lsla")))).toString());
      System.getProperties().put("crushftp.socketpooltimeout", (new StringBuffer(String.valueOf(IG("socketpool_timeout") * 1000))).toString());
      System.getProperties().put("crushftp.ls.year", (new StringBuffer(String.valueOf(BG("lsla_year")))).toString());
      if (this.loggingProvider != null)
        this.loggingProvider.checkLogPath(); 
      if (!this.server_info.containsKey("last_expired_accounts_check"))
        this.server_info.put("last_expired_accounts_check", (new StringBuffer(String.valueOf(System.currentTimeMillis() - 60000L))).toString()); 
      if (Long.parseLong(this.server_info.getProperty("last_expired_accounts_check")) < System.currentTimeMillis() - 3600000L) {
        this.server_info.put("last_expired_accounts_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        Log.log("USER", 2, "Checking for expired accounts...");
        String username = "";
        try {
          Vector sgs = (Vector)server_settings.get("server_groups");
          for (int x = 0; x < sgs.size(); x++) {
            String serverGroup = sgs.elementAt(x).toString();
            Log.log("USER", 2, "Checking for expired accounts:" + serverGroup);
            Vector v = new Vector();
            UserTools.refreshUserList(serverGroup, v);
            for (int xx = 0; xx < v.size(); xx++) {
              username = v.elementAt(xx).toString();
              try {
                Properties user = UserTools.ut.getUser(serverGroup, username, false);
                if (user != null && user.getProperty("account_expire_delete", "").equals("true") && this.common_code.check_date_expired_roll(user.getProperty("account_expire"))) {
                  Log.log("USER", 0, "Deleting expired account:" + serverGroup + "/" + username);
                  UserTools.expireUserVFSTask(user, serverGroup, username);
                  Log.log("USER", 0, "Removing account:" + serverGroup + "/" + username);
                  UserTools.deleteUser(serverGroup, username);
                } 
                Thread.sleep(10L);
              } catch (Exception e) {
                Log.log("USER", 1, "Checking " + username + " for expiration...error:" + e.toString());
                Log.log("USER", 1, e);
              } 
            } 
          } 
        } catch (Exception e) {
          Log.log("USER", 1, "Checking " + username + " for expiration...error:" + e.toString());
          Log.log("USER", 1, e);
        } 
        Log.log("USER", 2, "Checking for expired accounts...done.");
        this.server_info.put("last_expired_accounts_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      } 
      if (!this.server_info.containsKey("last_search_index_interval"))
        this.server_info.put("last_search_index_interval", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()); 
      if (IG("search_index_interval") > 0 && Long.parseLong(this.server_info.getProperty("last_search_index_interval")) < System.currentTimeMillis() - (60000 * IG("search_index_interval"))) {
        this.server_info.put("last_search_index_interval", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        try {
          String[] usernames = SG("search_index_usernames").split(",");
          for (int x = 0; x < usernames.length; x++) {
            if (!usernames[x].trim().equals("")) {
              Vector server_groups = (Vector)server_settings.get("server_groups");
              for (int xx = 0; xx < server_groups.size(); xx++) {
                VFS uVFS = UserTools.ut.getVFS(server_groups.elementAt(xx).toString(), usernames[x].trim());
                Properties pp = uVFS.get_item("/");
                SearchHandler.buildEntry(pp, uVFS, false, false);
                uVFS.disconnect();
                uVFS.free();
              } 
            } 
          } 
        } catch (Exception e) {
          Log.log("SEARCH", 0, e);
        } 
      } 
      if (!this.server_info.containsKey("last_expired_sync_check"))
        this.server_info.put("last_expired_sync_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()); 
      if (Long.parseLong(this.server_info.getProperty("last_expired_sync_check")) < System.currentTimeMillis() - 3600000L) {
        this.server_info.put("last_expired_sync_check", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
        if (IG("sync_history_days") > 0)
          try {
            Calendar c = new GregorianCalendar();
            c.setTime(new Date());
            c.add(5, IG("sync_history_days") * -1);
            SyncTools.purgeExpired(c.getTime().getTime());
          } catch (Exception e) {
            Log.log("SYNC", 0, e);
          }  
      } 
    } else if (arg.equals("http_cleaner")) {
      long sessions = 0L;
      long http_keys = 0L;
      long http_keys_expired1 = 0L;
      long http_keys_expired2 = 0L;
      long http_activity_keys = 0L;
      long http_activity_keys_expired = 0L;
      try {
        Enumeration keys = SharedSession.find("crushftp.usernames.activity").keys();
        while (keys.hasMoreElements()) {
          String id = keys.nextElement().toString();
          http_activity_keys++;
          long time = Long.parseLong(SharedSession.find("crushftp.usernames.activity").getProperty(id, "0"));
          if ((new Date()).getTime() - time > (60000 * IG("http_session_timeout"))) {
            http_activity_keys_expired++;
            Enumeration e = SharedSession.find("crushftp.usernames").keys();
            while (e.hasMoreElements()) {
              String key2 = e.nextElement().toString();
              if (key2.indexOf("_" + id + "_") >= 0) {
                http_keys_expired1++;
                if (id.endsWith("_vfs")) {
                  VFS uVFS = (VFS)SharedSession.find("crushftp.usernames").get(key2);
                  uVFS.disconnect();
                } 
                SharedSession.find("crushftp.usernames").remove(key2);
                Tunnel2.stopTunnel(key2);
              } 
            } 
            SharedSession.find("crushftp.usernames.activity").remove(id);
          } 
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
          if (!SharedSession.find("crushftp.usernames.activity").containsKey(id)) {
            http_keys_expired2++;
            if (key2.endsWith("_vfs")) {
              VFS uVFS = (VFS)SharedSession.find("crushftp.usernames").get(key2);
              uVFS.disconnect();
            } 
            SharedSession.find("crushftp.usernames").remove(key2);
          } 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      try {
        Enumeration e = SharedSession.find("crushftp.sessions").keys();
        while (e.hasMoreElements()) {
          String id = e.nextElement().toString();
          sessions++;
          if (!SharedSession.find("crushftp.usernames.activity").containsKey(id)) {
            Properties session = (Properties)SharedSession.find("crushftp.sessions").get(id);
            VFS uVFS = (VFS)session.remove("sharedVFS");
            if (uVFS != null)
              uVFS.disconnect(); 
            SharedSession.find("crushftp.sessions").remove(id);
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
          if (System.currentTimeMillis() > generated + 600000L)
            resetTokens.remove(key2); 
        } 
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
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
      if (BG("roll_log") && this.loggingProvider != null)
        this.loggingProvider.checkForLogRoll(); 
      Thread.sleep(5000L);
      Vector running_tasks = siVG("running_tasks");
      String job_log_path = String.valueOf((new File(change_vars_to_values_static(SG("log_location"), null, null, null))).getCanonicalFile().getParentFile().getPath()) + "/logs/jobs/";
      File logFiles = new File(job_log_path);
      logFiles.mkdirs();
      File[] log_files = logFiles.listFiles();
      for (int x = 0; log_files != null && x < log_files.length; x++) {
        boolean found = false;
        for (int xx = 0; !found && xx < running_tasks.size(); xx++) {
          Properties tracker = running_tasks.elementAt(xx);
          if (Common.last(tracker.getProperty("log_file", "")).equalsIgnoreCase(log_files[x].getName()))
            found = true; 
        } 
        if (!found) {
          Log.log("SERVER", 0, "Cleaning up old job log file:" + log_files[x].getName());
          log_files[x].delete();
        } 
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
          long lastMod = currFilePointer.lastModified() + multiplier * timeAmount;
          if (!currFilePointer.getCanonicalPath().equals(rFolder.getCanonicalPath()) && (new Date()).compareTo(new Date(lastMod)) > 0) {
            if (Common.machine_is_windows()) {
              if (p.getProperty("folder_match", "*").indexOf("\\") < 0 && p.getProperty("folder_match", "*").indexOf("/") >= 0)
                p.put("folder_match", p.getProperty("folder_match", "*").replace('/', '\\')); 
              if (p.getProperty("folder_not_match", "*").indexOf("\\") < 0 && p.getProperty("folder_not_match", "*").indexOf("/") >= 0)
                p.put("folder_not_match", p.getProperty("folder_not_match", "*").replace('/', '\\')); 
              if (p.getProperty("folder_not_match_name", "*").indexOf("\\") < 0 && p.getProperty("folder_not_match_name", "*").indexOf("/") >= 0)
                p.put("folder_not_match_name", p.getProperty("folder_not_match_name", "*").replace('/', '\\')); 
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
          if (p.getProperty("folderMonitorAction", "Archive or Delete").toUpperCase().startsWith("JOB:")) {
            Properties request = new Properties();
            Vector schedules = VG("schedules");
            int index = -1;
            for (int j = 0; j < schedules.size(); j++) {
              Properties schedule = schedules.elementAt(j);
              if (("Job:" + schedule.getProperty("scheduleName", "")).equalsIgnoreCase(p.getProperty("folderMonitorAction", "Archive or Delete")))
                index = j; 
            } 
            if (index >= 0) {
              request.put("scheduleIndex", (new StringBuffer(String.valueOf(index))).toString());
              AdminControls.testJobSchedule(request);
            } else {
              Log.log("SERVER", 0, "Folder monitor failed to find schedule:" + p);
            } 
          } else {
            Properties event = new Properties();
            event.put("event_plugin_list", p.getProperty("folderMonitorAction", "Archive or Delete"));
            event.put("name", "FolderMonitorEvent:" + p.getProperty("folder"));
            this.events6.doEventPlugin(event, null, items);
          } 
        } 
        continue;
      } 
    } else {
      siPUT("total_server_bytes_transfered", this.total_server_bytes_sent + this.total_server_bytes_received);
      siPUT("total_server_bytes_sent", this.total_server_bytes_sent);
      siPUT("total_server_bytes_received", this.total_server_bytes_received);
      siPUT("thread_pool_available", (new StringBuffer(String.valueOf(thisObj.availableWorkers.size()))).toString());
      siPUT("thread_pool_busy", (new StringBuffer(String.valueOf(thisObj.busyWorkers.size()))).toString());
      siPUT("ram_max", (new StringBuffer(String.valueOf(Runtime.getRuntime().maxMemory()))).toString());
      siPUT("ram_free", (new StringBuffer(String.valueOf(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()))).toString());
      if (Common.log != null && this.loggingProvider != null)
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
          this.loggingProvider.append_log(s, tag, true);
        }  
      if (count_users_down() < IG("server_download_queue_size") && this.server_download_queue.size() > 0) {
        ServerSession the_thread = this.server_download_queue.elementAt(0);
        this.server_download_queue.removeElementAt(0);
        try {
          the_thread.uiPUT("pause_now", "false");
        } catch (Exception exception) {}
      } 
      if (count_users_up() < IG("server_upload_queue_size") && this.server_upload_queue.size() > 0) {
        ServerSession the_thread = this.server_upload_queue.elementAt(0);
        this.server_upload_queue.removeElementAt(0);
        try {
          the_thread.uiPUT("pause_now", "false");
        } catch (Exception exception) {}
      } 
      int x;
      for (x = this.server_upload_queue.size() - 1; x >= 0; x--) {
        ServerSession the_thread = this.server_upload_queue.elementAt(x);
        if (the_thread == null)
          this.server_upload_queue.removeElementAt(x); 
      } 
      for (x = this.server_download_queue.size() - 1; x >= 0; x--) {
        ServerSession the_thread = this.server_download_queue.elementAt(x);
        if (the_thread == null)
          this.server_download_queue.removeElementAt(x); 
      } 
      calc_server_speeds(null);
      Vector server_list_vec = null;
      try {
        server_list_vec = (Vector)server_settings.get("server_list");
      } catch (Exception e) {
        Properties the_item = (Properties)server_settings.get("server_list");
        server_list_vec = new Vector();
        server_list_vec.addElement(the_item);
      } 
      int i;
      for (i = 0; i < this.main_servers.size(); i++) {
        GenericServer the_server = this.main_servers.elementAt(i);
        the_server.updateStatus();
      } 
      while (siVG("recent_user_list").size() > IG("recent_user_count"))
        siVG("recent_user_list").removeElementAt(0); 
      siPUT("total_logins", (new StringBuffer(String.valueOf(siIG("failed_logins") + siIG("successful_logins")))).toString());
      siPUT("users_connected", (new StringBuffer(String.valueOf(getTotalConnectedUsers()))).toString());
      for (i = 0; i < this.previewWorkers.size(); i++) {
        PreviewWorker preview = this.previewWorkers.elementAt(i);
        preview.run(null);
      } 
      update_history("logged_in_users");
      update_history("current_download_speed");
      update_history("current_upload_speed");
      Properties p = new Properties();
      p.put("server_settings", server_settings);
      p.put("server_info", this.server_info);
      runPlugins(p);
    } 
  }
  
  public void update_history(String key) {
    Vector v = (Vector)this.server_info.get(String.valueOf(key) + "_history");
    if (v == null) {
      v = new Vector();
      this.server_info.put(String.valueOf(key) + "_history", v);
    } 
    while (v.size() > 299)
      v.remove(0); 
    v.addElement(siSG(key));
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
      put_in("discovered_ip", new_ip);
    } else {
      append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Auto IP lookup failure (could not reach server, and could not detect local IP)!") + "---", "ERROR");
    } 
    for (int x = 0; x < this.main_servers.size(); x++) {
      GenericServer the_server = this.main_servers.elementAt(x);
      the_server.updateStatus();
    } 
  }
  
  public static int calc_server_up_speeds(String username) throws Exception {
    int speed = 0;
    for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        ServerSession the_thread = (ServerSession)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (the_thread.uiBG("receiving_file"))
          if (username == null || username.equalsIgnoreCase(the_thread.uiSG("user_name")))
            speed = (int)(speed + the_thread.uiLG("current_transfer_speed"));  
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
        ServerSession the_thread = (ServerSession)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (the_thread.stor_files != null && the_thread.stor_files.active && !the_thread.uiBG("pause_now"))
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
        ServerSession the_thread = (ServerSession)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (the_thread.uiLG("seconds_remaining") > 0L)
          timer.addElement((new StringBuffer(String.valueOf(the_thread.uiLG("seconds_remaining")))).toString()); 
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
        ServerSession the_thread = (ServerSession)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (the_thread.retr_files != null && the_thread.retr_files.active && !the_thread.uiBG("pause_now"))
          num_items++; 
      } catch (Exception e) {
        if (e.indexOf("Interrupted") >= 0)
          throw e; 
        Log.log("SERVER", 2, e);
      } 
    } 
    return num_items;
  }
  
  public static int calc_server_down_speeds(String username) throws Exception {
    int speed = 0;
    for (int x = siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        ServerSession the_thread = (ServerSession)((Properties)siVG("user_list").elementAt(x)).get("session");
        if (the_thread.uiBG("sending_file"))
          if (username == null || username.equalsIgnoreCase(the_thread.uiSG("user_name")))
            speed = (int)(speed + the_thread.uiLG("current_transfer_speed"));  
      } catch (Exception e) {
        if (e.indexOf("Interrupted") >= 0)
          throw e; 
        Log.log("SERVER", 1, e);
      } 
    } 
    return speed;
  }
  
  public static int calc_server_speeds(String username) {
    int speed = 0;
    try {
      int downSpeed = calc_server_down_speeds(username);
      speed += downSpeed;
      int upSpeed = calc_server_up_speeds(username);
      speed += upSpeed;
      siPUT("current_download_speed", downSpeed);
      siPUT("current_upload_speed", upSpeed);
    } catch (Exception e) {
      Log.log("SERVER", 2, e);
    } 
    return speed;
  }
  
  public void quit_server() {
    append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|********" + LOC.G("CrushFTP Quit") + "******** " + siSG("version_info_str") + siSG("sub_version_info_str"), "QUIT_SERVER");
    try {
      this.loggingProvider.shutdown();
    } catch (Exception exception) {}
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
      server_settings.put(key, data);
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
      Thread.sleep(500L);
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
      ServerSession theSession = (ServerSession)user_info.get("session");
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
          theSession.this_thread.interrupt();
        } catch (Exception exception) {}
        try {
          theSession.os.close();
        } catch (Exception exception) {}
        try {
          theSession.sock.close();
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
      ServerSession the_thread = (ServerSession)user_info.get("session");
      try {
        append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("User Kicked") + "---:" + the_thread.uiSG("user_number") + "-" + the_thread.uiSG("user_name"), "KICK");
      } catch (Exception exception) {}
      try {
        append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("Kicking session: passive_kick.") + "---", "KICK");
      } catch (Exception exception) {}
      if (!the_thread.uiBG("dieing")) {
        the_thread.uiPUT("termination_message", "KICKED");
        the_thread.uiPUT("friendly_quit", "true");
        the_thread.not_done = false;
        the_thread.do_kill();
      } 
    } catch (Exception e) {
      success = false;
      Log.log("SERVER", 1, e);
    } 
    return success;
  }
  
  public boolean ban(String the_user) {
    return ban(the_user, 0);
  }
  
  public boolean ban(String the_user, int timeout) {
    int x;
    for (x = 0; x < siVG("user_list").size(); x++) {
      Properties p = siVG("user_list").elementAt(x);
      if (p.getProperty("id").equalsIgnoreCase(the_user))
        return ban(p, timeout, false); 
    } 
    for (x = 0; x < siVG("recent_user_list").size(); x++) {
      Properties p = siVG("recent_user_list").elementAt(x);
      if (p.getProperty("id").equalsIgnoreCase(the_user))
        return ban(p, timeout, false); 
    } 
    return false;
  }
  
  public boolean ban(Properties user_info, int timeout) {
    return ban(user_info, timeout, false);
  }
  
  public boolean ban(Properties user_info, int timeout, boolean onlyRealBan) {
    try {
      String new_ip_text = user_info.getProperty("user_ip");
      new_ip_text = new_ip_text.substring(new_ip_text.indexOf("/") + 1, new_ip_text.length());
      if (ban_ip(new_ip_text, timeout, onlyRealBan)) {
        try {
          append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---" + LOC.G("User Banned") + "---:" + user_info.getProperty("user_number") + "-" + user_info.getProperty("user_name") + "  " + new_ip_text, "BAN");
        } catch (Exception exception) {}
        return true;
      } 
    } catch (Exception exception) {}
    return false;
  }
  
  public boolean ban_ip(String ip, int timeout) throws Exception {
    return ban_ip(ip, timeout, false);
  }
  
  public boolean ban_ip(String ip, int timeout, boolean onlyRealBan) throws Exception {
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
      if (ipOK) {
        ip_data.put("type", (timeout > 0) ? "T" : "D");
        ip_data.put("start_ip", ip);
        ip_data.put("stop_ip", ip);
        if (timeout > 0)
          ip_data.put("timeout", (new StringBuffer(String.valueOf((timeout * 60000) + (new Date()).getTime()))).toString()); 
        ip_list.insertElementAt(ip_data, 0);
        server_settings.put("ip_restrictions", ip_list);
        Properties pp = new Properties();
        pp.put("id", String.valueOf((new Date()).getTime()) + ":" + Common.makeBoundary());
        pp.put("complete", "false");
        pp.put("data", server_settings);
        thisObj.setSettings(pp);
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
  
  public String change_vars_to_values(String in_str, ServerSession the_session) {
    if (the_session != null)
      return change_vars_to_values(in_str, the_session.user, the_session.user_info, the_session); 
    return change_vars_to_values(in_str, new Properties(), new Properties(), the_session);
  }
  
  public String change_vars_to_values(String in_str, Properties user, Properties user_info, ServerSession the_session) {
    return change_vars_to_values_static(in_str, user, user_info, the_session);
  }
  
  public static String change_vars_to_values_static(String in_str, Properties user, Properties user_info, ServerSession the_session) {
    try {
      if (in_str.indexOf("%") >= 0)
        in_str = parse_server_messages(in_str); 
      if (in_str.indexOf("%ldap_") >= 0)
        while (in_str.indexOf("%ldap_") >= 0) {
          String key = in_str.substring(in_str.indexOf("%ldap_"), in_str.indexOf("%", in_str.indexOf("%ldap_") + 1) + 1);
          in_str = Common.replace_str(in_str, key, uSG(user, key.substring(1, key.length() - 1)));
        }  
      if (in_str.indexOf("%user_") >= 0) {
        int loc = in_str.indexOf("%user_");
        while (loc >= 0) {
          String key = in_str.substring(loc, in_str.indexOf("%", loc + 1) + 1);
          String user_key = key.substring("%user_".length(), key.length() - 1);
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
              if (cd.toUpperCase().startsWith(user.getProperty("root_dir").toUpperCase()))
                cd = cd.substring(user.getProperty("root_dir").length() - 1); 
              in_str = Common.replace_str(in_str, key, cd);
            }  
          loc++;
          loc = in_str.indexOf("%user_", loc);
        } 
      } 
      if (in_str.indexOf("%beep%") >= 0)
        in_str = Common.replace_str(in_str, "%beep%", ""); 
      if (in_str.indexOf("%hostname%") >= 0)
        in_str = Common.replace_str(in_str, "%hostname%", hostname); 
      if (in_str.indexOf("{hostname}") >= 0)
        in_str = Common.replace_str(in_str, "{hostname}", hostname); 
      if (in_str.indexOf("%server_time_date%") >= 0)
        in_str = Common.replace_str(in_str, "%server_time_date%", (new Date()).toString()); 
      if (in_str.indexOf("%login_number%") >= 0)
        in_str = Common.replace_str(in_str, "%login_number%", uSG(user_info, "user_number")); 
      if (in_str.indexOf("%users_connected%") >= 0)
        in_str = Common.replace_str(in_str, "%users_connected%", thisObj.getTotalConnectedUsers()); 
      if (in_str.indexOf("%user_password%") >= 0)
        in_str = Common.replace_str(in_str, "%user_password%", uSG(user_info, "current_password")); 
      if (in_str.indexOf("%user_name%") >= 0)
        in_str = Common.replace_str(in_str, "%user_name%", uSG(user, "username")); 
      if (in_str.indexOf("%user_anonymous_password%") >= 0)
        in_str = Common.replace_str(in_str, "%user_anonymous_password%", uSG(user_info, "user_name").equalsIgnoreCase("anonymous") ? uSG(user_info, "current_password") : ""); 
      if (in_str.indexOf("%user_current_dir%") >= 0)
        in_str = Common.replace_str(in_str, "%user_current_dir%", the_session.get_PWD()); 
      if (in_str.indexOf("%user_sessionid%") >= 0)
        in_str = Common.replace_str(in_str, "%user_sessionid%", uSG(user_info, "CrushAuth")); 
      if (in_str.indexOf("%user_site_commands_text%") >= 0)
        in_str = Common.replace_str(in_str, "%user_site_commands_text%", uSG(user, "site")); 
      try {
        if (in_str.indexOf("%user_time_remaining%") >= 0) {
          String time_str = String.valueOf(uLG(user_info, "seconds_remaining")) + " secs";
          if (uLG(user_info, "seconds_remaining") == 0L)
            time_str = "<None Active>"; 
          user_info.put("last_time_remaining", time_str);
          if (uLG(user_info, "seconds_remaining") > 60L)
            time_str = String.valueOf(the_session.uiLG("seconds_remaining") / 60L) + "min, " + (the_session.uiLG("seconds_remaining") - the_session.uiLG("seconds_remaining") / 60L * 60L) + " secs"; 
          in_str = Common.replace_str(in_str, "%user_time_remaining%", time_str);
          user_info.put("last_time_remaining", time_str);
        } 
      } catch (Exception e) {
        in_str = Common.replace_str(in_str, "%user_time_remaining%", uSG(user_info, "last_time_remaining"));
      } 
      if (in_str.indexOf("%user_paused%") >= 0)
        in_str = Common.replace_str(in_str, "%user_paused%", uBG(user_info, "pause_now") ? "!PAUSED!" : ""); 
      if (in_str.indexOf("%user_bytes_remaining%") >= 0)
        in_str = Common.replace_str(in_str, "%user_bytes_remaining%", uLG(user_info, "file_length") - uLG(user_info, "bytes_sent") - uLG(user_info, "start_transfer_byte_amount")); 
      if (in_str.indexOf("%user_pasv_port%") >= 0)
        in_str = Common.replace_str(in_str, "%user_pasv_port%", uIG(user_info, "PASV_port")); 
      if (in_str.indexOf("%user_ratio%") >= 0)
        in_str = Common.replace_str(in_str, "%user_ratio%", uSG(user, "ratio") + " to 1"); 
      if (in_str.indexOf("%user_perm_ratio%") >= 0)
        in_str = Common.replace_str(in_str, "%user_perm_ratio%", uBG(user, "perm_ratio") ? "Yes" : "No"); 
      if (in_str.indexOf("%user_reverse_ip%") >= 0)
        in_str = Common.replace_str(in_str, "%user_reverse_ip%", InetAddress.getByName(uSG(user, "user_ip")).getHostName()); 
      if (in_str.indexOf("%tunnels%") >= 0) {
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
        in_str = Common.replace_str(in_str, "%tunnels%", tunnelsStr);
      } 
      if (in_str.indexOf("%last_login_date_time%") >= 0)
        in_str = Common.replace_str(in_str, "%last_login_date_time%", siSG("last_login_date_time")); 
      if (in_str.indexOf("%last_login_ip%") >= 0)
        in_str = Common.replace_str(in_str, "%last_login_ip%", siSG("last_login_ip")); 
      if (in_str.indexOf("%last_login_user%") >= 0)
        in_str = Common.replace_str(in_str, "%last_login_user%", siSG("last_login_user")); 
      if (in_str.indexOf("%failed_logins%") >= 0)
        in_str = Common.replace_str(in_str, "%failed_logins%", siIG("failed_logins")); 
      if (in_str.indexOf("%successful_logins%") >= 0)
        in_str = Common.replace_str(in_str, "%successful_logins%", siIG("successful_logins")); 
      if (in_str.indexOf("%total_logins%") >= 0)
        in_str = Common.replace_str(in_str, "%total_logins%", siIG("failed_logins") + siIG("successful_logins")); 
      if (in_str.indexOf("%downloaded_files%") >= 0)
        in_str = Common.replace_str(in_str, "%downloaded_files%", siIG("downloaded_files")); 
      if (in_str.indexOf("%uploaded_files%") >= 0)
        in_str = Common.replace_str(in_str, "%uploaded_files%", siIG("uploaded_files")); 
      if (in_str.indexOf("%bytes_received_f%") >= 0)
        in_str = Common.replace_str(in_str, "%bytes_received_f%", siSG("total_server_bytes_received")); 
      if (in_str.indexOf("%bytes_sent_f%") >= 0)
        in_str = Common.replace_str(in_str, "%bytes_sent_f%", siSG("total_server_bytes_sent")); 
      if (in_str.indexOf("%total_bytes_f%") >= 0)
        in_str = Common.replace_str(in_str, "%total_bytes_f%", siSG("total_server_bytes_transfered")); 
      if (in_str.indexOf("%max_server_download_speed%") >= 0)
        in_str = Common.replace_str(in_str, "%max_server_download_speed%", SG("max_server_download_speed")); 
      if (in_str.indexOf("%max_server_upload_speed%") >= 0)
        in_str = Common.replace_str(in_str, "%max_server_upload_speed%", SG("max_server_upload_speed")); 
      if (in_str.indexOf("%bytes_received%") >= 0)
        in_str = Common.replace_str(in_str, "%bytes_received%", siSG("total_server_bytes_received")); 
      if (in_str.indexOf("%bytes_sent%") >= 0)
        in_str = Common.replace_str(in_str, "%bytes_sent%", siSG("total_server_bytes_sent")); 
      if (in_str.indexOf("%total_bytes%") >= 0)
        in_str = Common.replace_str(in_str, "%total_bytes%", siSG("total_server_bytes_transfered")); 
      if (in_str.indexOf("%current_server_downloading_count%") >= 0)
        in_str = Common.replace_str(in_str, "%current_server_downloading_count%", count_users_down()); 
      if (in_str.indexOf("%current_server_uploading_count%") >= 0)
        in_str = Common.replace_str(in_str, "%current_server_uploading_count%", count_users_up()); 
      if (in_str.indexOf("%current_download_speed%") >= 0)
        in_str = Common.replace_str(in_str, "%current_download_speed%", siSG("current_download_speed")); 
      if (in_str.indexOf("%current_upload_speed%") >= 0)
        in_str = Common.replace_str(in_str, "%current_upload_speed%", siSG("current_upload_speed")); 
      if (in_str.indexOf("%max_users%") >= 0)
        in_str = Common.replace_str(in_str, "%max_users%", SG("max_users")); 
      if (in_str.indexOf("%ip%") >= 0)
        in_str = Common.replace_str(in_str, "%ip%", siSG("discovered_ip")); 
      if (in_str.indexOf("%beep_connect%") >= 0)
        in_str = Common.replace_str(in_str, "%beep_connect%", SG("beep_connect")); 
      if (in_str.indexOf("%deny_reserved_ports%") >= 0)
        in_str = Common.replace_str(in_str, "%deny_reserved_ports%", SG("deny_reserved_ports")); 
      if (in_str.indexOf("%deny_fxp%") >= 0)
        in_str = Common.replace_str(in_str, "%deny_fxp%", SG("deny_fxp")); 
      if (in_str.indexOf("%about_info_str%") >= 0)
        in_str = Common.replace_str(in_str, "%about_info%", siSG("about_info_str")); 
      if (in_str.indexOf("%version_info%") >= 0)
        in_str = Common.replace_str(in_str, "%version_info%", siSG("version_info_str")); 
      if (in_str.indexOf("%start_time%") >= 0)
        in_str = Common.replace_str(in_str, "%start_time%", siSG("server_start_time")); 
      if (in_str.indexOf("%thread_count%") >= 0)
        in_str = Common.replace_str(in_str, "%thread_count%", Thread.activeCount()); 
      if (in_str.indexOf("%free_memory%") >= 0)
        in_str = Common.replace_str(in_str, "%free_memory%", Runtime.getRuntime().freeMemory() / 1024L); 
      if (in_str.indexOf("%ban%") >= 0) {
        in_str = Common.replace_str(in_str, "%ban%", "");
        thisObj.ban(user_info, 0);
      } 
      if (in_str.indexOf("%kick%") >= 0) {
        in_str = Common.replace_str(in_str, "%kick%", "");
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
    } catch (Exception e) {
      Log.log("SERVER", 2, e);
    } 
    return in_str;
  }
  
  public String strip_variables(String in_str, ServerSession the_session) {
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
  
  public String get_dir_list(String in_str, ServerSession the_session) throws Exception {
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
        if (ban_ip(ip, IG("ban_timeout"))) {
          siPUT("hammer_history", Common.replace_str(siSG("hammer_history"), ip, ""));
          try {
            append_log(String.valueOf(this.logDateFormat.format(new Date())) + "|---IP Banned---:" + ip, "BAN");
          } catch (Exception exception) {}
          return false;
        }  
    } catch (Exception exception) {}
    return true;
  }
  
  public void runAlerts(String action, ServerSession the_user) {
    runAlerts(action, null, null, the_user);
  }
  
  public void runAlerts(String action, Properties info, Properties user_info, ServerSession the_user) {
    if (user_info == null && the_user != null)
      user_info = the_user.user_info; 
    Vector alerts = VG("alerts");
    for (int x = 0; x < alerts.size(); x++) {
      Properties p = alerts.elementAt(x);
      boolean ok = true;
      String subject = p.getProperty("subject", "");
      String body = p.getProperty("body", "");
      String to = p.getProperty("to", "");
      String cc = p.getProperty("cc", "");
      String bcc = p.getProperty("bcc", "");
      String from = p.getProperty("from", "");
      if (p.getProperty("type").equalsIgnoreCase("Disk Space Below Threshold") && action.equals("disk")) {
        String drive = p.getProperty("drive", "/");
        long mb = Long.parseLong(p.getProperty("threshold_mb", "0")) * 1024L * 1024L;
        long free_bytes = Common.get_free_disk_space(drive);
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
      } else if (!p.getProperty("type").equalsIgnoreCase("CrushFTP Update Available") || !action.equals("update")) {
        if (p.getProperty("type").equalsIgnoreCase("User reached quota percentage") && action.equals("user_upload_session")) {
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
        } else if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Upload Transfer Amount Per Session") || !action.equals("user_upload_session")) {
          if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Upload Transfer Amount Per Day") || !action.equals("user_upload_day"))
            if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Download Transfer Amount Per Session") || !action.equals("user_download_session"))
              if (!p.getProperty("type").equalsIgnoreCase("User Exceeded Download Transfer Amount Per Day") || !action.equals("user_download_day"))
                if (!p.getProperty("type").equalsIgnoreCase("Proxy Blacklisted Site Attempted") || !action.equals("proxy_blacklist"))
                  if (!p.getProperty("type").equalsIgnoreCase("IP Banned for Failed Logins") || !action.equals("ip_banned_logins"))
                    if (!p.getProperty("type").equalsIgnoreCase("User Changed Password") || !action.equals("password_change"))
                      if (p.getProperty("type").equalsIgnoreCase("Invalid Email Attempted") && action.equals("invalid_email")) {
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
                      } else if (p.getProperty("type").equalsIgnoreCase("User Hammering") && action.equals("user_hammering")) {
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
                        String newLines = "";
                        while (keys.hasMoreElements()) {
                          String key = keys.nextElement().toString();
                          int count = Integer.parseInt(loginsCounter.getProperty(key, "0"));
                          if (count > Integer.parseInt(p.getProperty("login_count", "100"))) {
                            newLines = thisObj.change_vars_to_values(line, null, (Properties)loginsUserInfos.get(key), null);
                            if (hasLines) {
                              newLines = String.valueOf(newLines) + "\r\n" + key + ":" + count + "\r\n";
                            } else {
                              body = String.valueOf(body) + "\r\n" + key + ":" + count + "\r\n";
                            } 
                            found = true;
                          } 
                        } 
                        body = Common.replace_str(body, "<LINE>" + line + "</LINE>", newLines).trim();
                        if (!found)
                          ok = false; 
                      } else if (p.getProperty("type").equalsIgnoreCase("Plugin Message") && action.startsWith("pluginMessage_")) {
                        subject = Common.replace_str(subject, "%message%", action.substring(action.indexOf("_") + 1));
                        body = Common.replace_str(body, "%message%", action.substring(action.indexOf("_") + 1));
                        if (p.getProperty("to").toUpperCase().startsWith("PLUGIN:")) {
                          String plugin = p.getProperty("to").substring("plugin:".length()).trim();
                          Vector items = new Vector();
                          File f = new File(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/temp/");
                          Properties item = new Properties();
                          try {
                            item.put("url", f.toURI().toURL().toExternalForm());
                          } catch (Exception exception) {}
                          item.put("the_command", action);
                          item.put("subject", subject);
                          item.put("body", body);
                          items.addElement(item);
                          append_log(String.valueOf(LOC.G("ALERT:")) + p.getProperty("type") + ":" + plugin, "ERROR");
                          Properties event = new Properties();
                          event.put("event_plugin_list", plugin);
                          event.put("name", "PluginMessage_Alert:" + plugin);
                          this.events6.doEventPlugin(event, the_user, items);
                          ok = false;
                        } 
                      } else {
                        ok = false;
                      }       
        } 
      } 
      if (ok) {
        append_log(String.valueOf(LOC.G("ALERT:")) + p.getProperty("type"), "ERROR");
        Properties user = null;
        if (the_user != null)
          user = the_user.user; 
        to = change_vars_to_values(to, user, user_info, the_user);
        cc = change_vars_to_values(cc, user, user_info, the_user);
        bcc = change_vars_to_values(bcc, user, user_info, the_user);
        from = change_vars_to_values(from, user, user_info, the_user);
        subject = change_vars_to_values(subject, user, user_info, the_user);
        body = change_vars_to_values(body, user, user_info, the_user);
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
          item.put("body", body);
          Vector items = new Vector();
          items.addElement(item);
          Properties event = new Properties();
          event.put("event_plugin_list", p.getProperty("alert_plugin", ""));
          event.put("name", "AlertPlugin:" + p.getProperty("type"));
          this.events6.doEventPlugin(event, the_user, items);
        } 
        if (p.get("hours") == null)
          p.put("hours", new Properties()); 
        Properties hours = (Properties)p.get("hours");
        SimpleDateFormat hh = new SimpleDateFormat("HH");
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
          String emailResult = Common.send_mail(SG("discovered_ip"), to, cc, bcc, from, subject, body, SG("smtp_server"), SG("smtp_user"), SG("smtp_pass"), BG("smtp_ssl"), BG("smtp_html"), null);
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
  }
  
  public void sendEmail(Properties p) {
    String results = Common.send_mail(server_settings.getProperty("discovered_ip"), p.getProperty("to", ""), p.getProperty("cc", ""), p.getProperty("bcc", ""), p.getProperty("from", ""), p.getProperty("subject", ""), p.getProperty("body", ""), p.getProperty("server", ""), p.getProperty("user", ""), p.getProperty("pass", ""), p.getProperty("ssl", "").equals("true"), p.getProperty("html", "").equals("true"), null);
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
  
  public void signCrushUploader() {
    if (!SG("cert_path").equals("builtin"));
  }
  
  public static int IG(String data) {
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
    put_in(data, thisObj.default_settings.getProperty(data));
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
