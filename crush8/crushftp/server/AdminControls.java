package crushftp.server;

import com.crushftp.client.Base64;
import com.crushftp.client.Common;
import com.crushftp.client.File_B;
import com.crushftp.client.File_S;
import com.crushftp.client.File_U;
import com.crushftp.client.GenericClient;
import com.crushftp.client.GenericClientMulti;
import com.crushftp.client.HeapDumper;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import com.didisoft.pgp.KeyStore;
import com.didisoft.pgp.PGPLib;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.JobScheduler;
import crushftp.handlers.Log;
import crushftp.handlers.SSLKeyManager;
import crushftp.handlers.SessionCrush;
import crushftp.handlers.SharedSessionReplicated;
import crushftp.handlers.SyncTools;
import crushftp.handlers.UserTools;
import crushftp.handlers.log.LoggingProviderDisk;
import crushftp.reports8.ReportTools;
import crushftp.server.daemon.DMZServer;
import crushftp.server.daemon.GenericServer;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.crypto.Cipher;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class AdminControls {
  public static Properties jobs_summary_cache = new Properties();
  
  public static long last_job_cache_clean = System.currentTimeMillis();
  
  public static Object runInstanceAction(Properties request, String site) throws Exception {
    return runInstanceAction(request, site, "127.0.0.1");
  }
  
  public static Object runInstanceAction(Properties request, String site, String user_ip) throws Exception {
    request.remove("instance");
    if (request.getProperty("command").equals("setServerItem"))
      return setServerItem(request, site); 
    if (request.getProperty("command").equals("getUser"))
      return getUser(request, site, null); 
    if (request.getProperty("command").equals("getPublicKeys"))
      return getPublicKeys(request); 
    if (request.getProperty("command").equals("setUserItem"))
      return setUserItem(request, null, site); 
    if (request.getProperty("command").equals("getUserList"))
      return getUserList(request, site, null); 
    if (request.getProperty("command").equals("getUserXML"))
      return getUserXML(request, site, null); 
    if (request.getProperty("command").equals("getUserXMLListing"))
      return getUserXMLListing(request, site, null); 
    if (request.getProperty("command").equals("getAdminXMLListing"))
      return getAdminXMLListing(request, null, site); 
    if (request.getProperty("command").equals("getLog"))
      return getLog(request, site); 
    if (request.getProperty("command").equals("adminAction"))
      return adminAction(request, site, user_ip); 
    if (request.getProperty("command").equals("updateNow"))
      return updateNow(request, site); 
    if (request.getProperty("command").equals("updateWebNow"))
      return updateWebNow(request, site); 
    if (request.getProperty("command").equals("updateNowProgress"))
      return updateNowProgress(request, site); 
    if (request.getProperty("command").equals("cancelUpdateProgress"))
      return cancelUpdateProgress(request, site); 
    if (request.getProperty("command").equals("dumpStack"))
      return dumpStack(request, site); 
    if (request.getProperty("command").equals("dumpHeap"))
      return dumpHeap(request, site); 
    if (request.getProperty("command").equals("pgpGenerateKeyPair"))
      return pgpGenerateKeyPair(request, site); 
    if (request.getProperty("command").equals("runReport"))
      return runReport(request, site); 
    if (request.getProperty("command").equals("testReportSchedule"))
      return testReportSchedule(request, site); 
    if (request.getProperty("command").equals("testPGP"))
      return testPGP(request, site); 
    if (request.getProperty("command").equals("testJobSchedule"))
      return testJobSchedule(request, site); 
    if (request.getProperty("command").equals("testSMTP"))
      return testSMTP(request, site); 
    if (request.getProperty("command").equals("testOTP"))
      return testOTP(request, site); 
    if (request.getProperty("command").equals("sendEventEmail"))
      return sendEventEmail(request, site); 
    if (request.getProperty("command").equals("migrateUsersVFS"))
      return migrateUsersVFS(request, site); 
    if (request.getProperty("command").equals("getJob"))
      return getJob(request, site); 
    if (request.getProperty("command").equals("addJob"))
      return addJob(request, site); 
    if (request.getProperty("command").equals("renameJob"))
      return renameJob(request, site); 
    if (request.getProperty("command").equals("getJobsSummary"))
      return getJobsSummary(request, site); 
    if (request.getProperty("command").equals("getJobInfo"))
      return getJobInfo(request, site); 
    if (request.getProperty("command").equals("removeJob"))
      return removeJob(request, site); 
    if (request.getProperty("command").equals("getServerRoots"))
      return getServerRoots(request, site); 
    if (request.getProperty("command").equals("convertUsers"))
      return convertUsers(request, site); 
    if (request.getProperty("command").equals("generateSSL"))
      return generateSSL(request, site); 
    if (request.getProperty("command").equals("generateCSR"))
      return generateCSR(request, site); 
    if (request.getProperty("command").equals("importReply"))
      return importReply(request, site); 
    if (request.getProperty("command").equals("listSSL"))
      return listSSL(request, site); 
    if (request.getProperty("command").equals("deleteSSL"))
      return deleteSSL(request, site); 
    if (request.getProperty("command").equals("renameSSL"))
      return renameSSL(request, site); 
    if (request.getProperty("command").equals("exportSSL"))
      return exportSSL(request, site); 
    if (request.getProperty("command").equals("addPrivateSSL"))
      return addPrivateSSL(request, site); 
    if (request.getProperty("command").equals("addPublicSSL"))
      return addPublicSSL(request, site); 
    if (request.getProperty("command").equals("restorePrefs"))
      return restorePrefs(request, site); 
    if (request.getProperty("command").equals("telnetSocket"))
      return telnetSocket(request, site); 
    if (request.getProperty("command").equals("testKeystore"))
      return testKeystore(request, site); 
    if (request.getProperty("command").equals("testDB"))
      return testDB(request, site); 
    if (request.getProperty("command").equals("testQuery"))
      return testQuery(request, site); 
    if (request.getProperty("command").equals("pluginMethodCall"))
      return pluginMethodCall(request, site); 
    if (request.getProperty("command").equals("convertXMLSQLUsers"))
      return convertXMLSQLUsers(request, site); 
    if (request.getProperty("command").equals("registerCrushFTP"))
      return registerCrushFTP(request, site); 
    if (request.getProperty("command").equals("importUsers"))
      return importUsers(request, site); 
    if (request.getProperty("command").equals("sendPassEmail"))
      return sendPassEmail(request, null, site); 
    if (request.getProperty("command").equals("getTempAccounts"))
      return getTempAccounts(request, site); 
    if (request.getProperty("command").equals("addTempAccount"))
      return addTempAccount(request, site); 
    if (request.getProperty("command").equals("removeTempAccount"))
      return removeTempAccount(request, site); 
    if (request.getProperty("command").equals("getTempAccountFiles"))
      return getTempAccountFiles(request, site); 
    if (request.getProperty("command").equals("removeTempAccountFile"))
      return removeTempAccountFile(request, site); 
    if (request.getProperty("command").equals("addTempAccountFile"))
      return addTempAccountFile(request, site); 
    if (request.getProperty("command").equals("deleteReplication"))
      return deleteReplication(request, site); 
    if (request.getProperty("command").equals("setReportSchedules"))
      return setReportSchedules(request, site); 
    if (request.getProperty("command").equals("deleteReportSchedules"))
      return deleteReportSchedules(request, site); 
    if (request.getProperty("command").equals("setMaxServerMemory"))
      return setMaxServerMemory(request, site); 
    if (request.getProperty("command").equals("restartProcess"))
      return restartProcess(request, site); 
    if (request.getProperty("command").equals("getDashboardItems"))
      return getDashboardItems(request, site); 
    if (request.getProperty("command").equals("getServerInfoItems"))
      return getServerInfoItems(request, site); 
    if (request.getProperty("command").equals("getServerSettingItems"))
      return getServerSettingItems(request, site); 
    return "";
  }
  
  public static Object getServerItem(String admin_group_name, Properties request, String site, Properties user) {
    try {
      String[] keys = request.getProperty("key").split("/");
      String last_key = "";
      Object o = null;
      try {
        for (int x = 0; x < keys.length; x++) {
          String key = keys[x];
          last_key = key;
          if (key.equals("server_settings")) {
            if (request.getProperty("instance", "").equals("")) {
              o = ServerStatus.server_settings;
            } else {
              String id = Common.makeBoundary();
              DMZServer.sendCommand(request.getProperty("instance", ""), new Properties(), "GET:SERVER_SETTINGS", id);
              Properties p = DMZServer.getResponse(id, 20);
              o = p.get("data");
            } 
            if (request.getProperty("defaults", "false").equals("true"))
              o = ServerStatus.thisObj.default_settings; 
            if (site.indexOf("(CONNECT)") < 0 && site.indexOf("(PREF_VIEW)") < 0 && site.indexOf("(PREF_EDIT)") < 0 && site.indexOf("(SERVER_VIEW)") < 0 && site.indexOf("(SERVER_EDIT)") < 0 && site.indexOf("(UPDATE_RUN)") < 0) {
              o = ServerStatus.thisObj.default_settings.clone();
              ((Properties)o).put("CustomForms", stripUnrelatedAdminItems("CustomForms", admin_group_name));
              ((Properties)o).put("tunnels", stripUnrelatedAdminItems("tunnels", admin_group_name));
              ((Properties)o).put("email_templates", stripUnrelatedAdminItems("email_templates", admin_group_name));
              Properties password_rules = SessionCrush.build_password_rules(user);
              ((Properties)o).putAll(password_rules);
              ((Properties)o).put("blank_passwords", ServerStatus.SG("blank_passwords"));
              ((Properties)o).put("user_default_folder_privs", ServerStatus.SG("user_default_folder_privs"));
              if (site.indexOf("(REPORT_VIEW)") >= 0 && site.indexOf("(REPORT_EDIT)") >= 0)
                ((Properties)o).put("reportSchedules", ServerStatus.VG("reportSchedules")); 
            } 
          } else if (key.equals("server_info")) {
            if (request.getProperty("instance", "").equals("")) {
              o = ServerStatus.thisObj.server_info.clone();
            } else {
              o = null;
              String id = Common.makeBoundary();
              DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
              Properties p = DMZServer.getResponse(id, 20);
              o = p.get("data");
            } 
            if (site.indexOf("(CONNECT)") < 0 && site.indexOf("(SERVER_VIEW)") < 0 && site.indexOf("(SERVER_EDIT)") < 0) {
              Properties o2 = new Properties();
              if (site.indexOf("(UPDATE_RUN)") >= 0) {
                o2.put("version_info_str", ((Properties)o).getProperty("version_info_str"));
                o2.put("sub_version_info_str", ((Properties)o).getProperty("sub_version_info_str"));
                o2.put("about_info_str", ((Properties)o).getProperty("about_info_str"));
                o2.put("about_info_str", ((Properties)o).getProperty("about_info_str"));
              } 
              o2.put("current_datetime_ddmmyyhhmmss", ((Properties)o).getProperty("current_datetime_ddmmyyhhmmss"));
              o = new Properties();
              ((Properties)o).putAll(o2);
            } 
          } else if (o instanceof Properties) {
            o = ((Properties)o).get(key);
          } else if (o instanceof Vector) {
            o = ((Vector)o).elementAt(Integer.parseInt(key));
          } 
        } 
      } catch (Exception e) {
        return "FAILURE:" + e.toString();
      } 
      if (last_key.equals("user_list") || last_key.equals("recent_user_list"))
        o = stripUserList(o); 
      if (last_key.equals("server_info")) {
        ((Properties)o).remove("plugins");
        ((Properties)o).remove("running_tasks");
        ((Properties)o).remove("user_list");
        ((Properties)o).remove("recent_user_list");
      } 
      if (o instanceof Properties)
        o = stripUser(o); 
      return o;
    } catch (Exception e) {
      return e.toString();
    } 
  }
  
  public static Object getDashboardItems(Properties request, String site) {
    Properties o2 = new Properties();
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 5);
        o2 = (Properties)p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return e.toString();
      } 
      return o2;
    } 
    try {
      Properties o = null;
      try {
        o = (Properties)ServerStatus.thisObj.server_info.clone();
        String[] keys = "jce_installed,low_memory,machine_is_linux,machine_is_solaris,machine_is_unix,machine_is_windows,machine_is_x,machine_is_x_10_5_plus,sub_version_info_str,version_info_str,registration_name,rid,enterprise_level,max_max_users,update_available,update_available_version,update_available_html,about_info_str,server_start_time,registration_email,server_list,recent_drives,recent_hammering,last_logins,ram_max,ram_free,thread_pool_available,thread_pool_busy,downloaded_files,uploaded_files,total_server_bytes_sent,total_server_bytes_received,successful_logins,failed_logins,current_download_speed,current_upload_speed,concurrent_users,replicated_servers,replicated_servers_count,replicated_servers_pending_user_sync,replicated_servers_pendingResponses,replicated_servers_lastActive,replicated_servers_sent_1,replicated_servers_sent_2,replicated_servers_sent_3,replicated_servers_sent_4,replicated_servers_sent_5,replicated_servers_queue_1,replicated_servers_queue_2,replicated_servers_queue_3,replicated_servers_queue_4,replicated_servers_queue_5,java_info,memcache_objects,keywords_cache_size,exif_item_count,replicated_received_message_count,replicated_write_prefs_count,replicated_user_changes_count,replicated_job_changes_count".split(",");
        for (int x = 0; x < keys.length; x++) {
          Object tmp = o.get(keys[x]);
          if (tmp == null)
            tmp = ""; 
          o2.put(keys[x], tmp);
        } 
        o2.put("replication_status", GenericClientMulti.replication_status);
        o2.put("max_threads", ServerStatus.SG("max_threads"));
        o2.put("hostname", ServerStatus.hostname);
      } catch (Exception e) {
        return "FAILURE:" + e.toString();
      } 
      return o2;
    } catch (Exception e) {
      return e.toString();
    } 
  }
  
  public static Object getServerInfoItems(Properties request, String site) {
    Properties o2 = new Properties();
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 5);
        o2 = (Properties)p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return e.toString();
      } 
      return o2;
    } 
    try {
      String allowed_list = "";
      if (site.indexOf("(USER_ADMIN)") >= 0)
        allowed_list = "machine_is_linux,machine_is_solaris,machine_is_unix,machine_is_windows,machine_is_x,machine_is_x_10_5_plus,sub_version_info_str,version_info_str"; 
      Properties o = null;
      try {
        if (request.getProperty("instance", "").equals(""))
          o = (Properties)ServerStatus.thisObj.server_info.clone(); 
        String[] keys = request.getProperty("keys").split(",");
        for (int x = 0; x < keys.length; x++) {
          Object tmp = o.get(keys[x]);
          if (tmp == null)
            tmp = ""; 
          if (allowed_list.indexOf(keys[x]) >= 0 || allowed_list.equals(""))
            o2.put(keys[x], tmp); 
        } 
      } catch (Exception e) {
        return "FAILURE:" + e.toString();
      } 
      return o2;
    } catch (Exception e) {
      return e.toString();
    } 
  }
  
  public static Object getServerSettingItems(Properties request, String site) {
    Properties o2 = new Properties();
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 5);
        o2 = (Properties)p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return e.toString();
      } 
      return o2;
    } 
    try {
      Properties o = null;
      String admin_group_name = request.getProperty("admin_group_name", "");
      try {
        if (request.getProperty("instance", "").equals("")) {
          o = (Properties)ServerStatus.server_settings.clone();
        } else {
          o = null;
          String id = Common.makeBoundary();
          DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_SETTINGS", id);
          Properties p = DMZServer.getResponse(id, 20);
          o = (Properties)p.get("data");
        } 
        if (site.indexOf("(CONNECT)") < 0 && site.indexOf("(PREF_VIEW)") < 0 && site.indexOf("(PREF_EDIT)") < 0 && site.indexOf("(SERVER_VIEW)") < 0 && site.indexOf("(SERVER_EDIT)") < 0 && site.indexOf("(UPDATE_RUN)") < 0) {
          o = (Properties)ServerStatus.thisObj.default_settings.clone();
          o.put("CustomForms", stripUnrelatedAdminItems("CustomForms", admin_group_name));
          o.put("tunnels", stripUnrelatedAdminItems("tunnels", admin_group_name));
          o.put("email_templates", stripUnrelatedAdminItems("email_templates", admin_group_name));
          Properties password_rules = SessionCrush.build_password_rules(null);
          o.putAll(password_rules);
          o.put("blank_passwords", ServerStatus.SG("blank_passwords"));
          o.put("user_default_folder_privs", ServerStatus.SG("user_default_folder_privs"));
          if (site.indexOf("(REPORT_VIEW)") >= 0 && site.indexOf("(REPORT_EDIT)") >= 0)
            o.put("reportSchedules", ServerStatus.VG("reportSchedules")); 
        } 
        String[] keys = request.getProperty("keys").split(",");
        for (int x = 0; x < keys.length; x++) {
          Object tmp = o.get(keys[x]);
          if (tmp == null)
            tmp = ""; 
          o2.put(keys[x], tmp);
        } 
      } catch (Exception e) {
        return "FAILURE:" + e.toString();
      } 
      return o2;
    } catch (Exception e) {
      return e.toString();
    } 
  }
  
  public static Object getJob(Properties request, String site) {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
        return p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return "FAILURE:Job not found.";
      } 
    } 
    Vector jobs = JobScheduler.getJobList(false);
    if (request.getProperty("name", "").equals("")) {
      Vector all = new Vector();
      for (int x = 0; x < jobs.size(); x++) {
        File_S f = jobs.elementAt(x);
        if (!f.getName().startsWith("_"))
          if (request.getProperty("schedule_info", "").equals("true")) {
            Properties p = (Properties)Common.readXMLObject(String.valueOf(((File_S)jobs.elementAt(x)).getPath()) + "/job.XML");
            if (p != null) {
              Properties p2 = new Properties();
              p2.put("name", jobName(f));
              p2.put("scheduleType", p.getProperty("scheduleType", "manual"));
              p2.put("minutelyAmount", p.getProperty("minutelyAmount", "1"));
              p2.put("weekDays", p.getProperty("weekDays", ""));
              p2.put("weeklyAmount", p.getProperty("weeklyAmount", "1"));
              p2.put("dailyAmount", p.getProperty("dailyAmount", "1"));
              p2.put("monthlyAmount", p.getProperty("monthlyAmount", "1"));
              p2.put("scheduleTime", p.getProperty("scheduleTime", ""));
              p2.put("monthDays", p.getProperty("monthDays", "1"));
              p2.put("enabled", p.getProperty("enabled", "false"));
              p2.put("nextRun", p.getProperty("nextRun", ""));
              all.addElement(p2);
            } 
          } else {
            all.addElement(jobName(f));
          }  
      } 
      return all;
    } 
    if (site.indexOf("(CONNECT)") >= 0 || site.indexOf("(JOB_EDIT)") >= 0 || site.indexOf("(JOB_VIEW)") >= 0)
      for (int x = 0; x < jobs.size(); x++) {
        String job_name = jobName(jobs.elementAt(x));
        if (request.getProperty("name", "").equalsIgnoreCase(job_name)) {
          Properties p = (Properties)Common.readXMLObject(String.valueOf(((File_S)jobs.elementAt(x)).getPath()) + "/job.XML");
          p.put("scheduleName", job_name);
          return p;
        } 
      }  
    return "FAILURE:Job not found.";
  }
  
  public static String jobName(File_S f) {
    return f.getPath().replace('\\', '/').substring((String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/").length());
  }
  
  public static Object saveReport(Properties request, String site, boolean replicate) {
    (new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "SavedReports/")).mkdirs();
    if (ServerStatus.BG("replicate_reports") && replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", "");
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.saveReport", "info", pp);
    } 
    try {
      RandomAccessFile out = new RandomAccessFile(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "SavedReports/" + request.getProperty("report_token")), "rw");
      out.setLength(0L);
      out.write(request.getProperty("s").getBytes("UTF8"));
      out.close();
    } catch (Exception e) {
      Log.log("SERVER", 1, e);
    } 
    return null;
  }
  
  public static Object renameJob(Properties request, String site) {
    return renameJob(request, site, true);
  }
  
  public static Object renameJob(Properties request, String site, boolean replicate) {
    if (ServerStatus.BG("replicate_jobs") && replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.renameJob", "info", pp);
    } 
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
        return p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return "FAILURE:Job not found.";
      } 
    } 
    if (JobScheduler.jobRunning(request.getProperty("priorName", "")))
      return "FAILURE:Rename is not allowed on running job. Frist stop the job  : " + request.getProperty("priorName", ""); 
    Vector jobs = JobScheduler.getJobList(false);
    for (int x = 0; x < jobs.size(); x++) {
      if (request.getProperty("priorName", "").equalsIgnoreCase(jobName(jobs.elementAt(x)))) {
        File newJob = new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + JobScheduler.safeName(request.getProperty("name")));
        if (((File_S)jobs.elementAt(x)).renameTo(newJob)) {
          Properties job = (Properties)Common.readXMLObject(new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + JobScheduler.safeName(request.getProperty("name")) + "/job.XML"));
          job.put("scheduleName", JobScheduler.safeName(request.getProperty("name")));
          try {
            Common.writeXMLObject(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + request.getProperty("name") + "/job.XML", job, "job");
            File_S[] f = (File_S[])newJob.listFiles();
            for (int xx = 0; f != null && xx < f.length; xx++) {
              if (f[xx].getName().indexOf("_") > 0 && f[xx].getName().lastIndexOf(".XML") > 0 && f[xx].isFile())
                try {
                  Properties tracker = (Properties)Common.readXMLObject(f[xx].getPath());
                  if (tracker.containsKey("settings")) {
                    Properties settings = (Properties)tracker.get("settings");
                    settings.put("scheduleName", JobScheduler.safeName(request.getProperty("name")));
                    Common.writeXMLObject(f[xx].getPath(), tracker, "tracker");
                  } 
                } catch (Exception e) {
                  Log.log("SERVER", 1, e);
                }  
            } 
            return "SUCCESS:" + JobScheduler.safeName(request.getProperty("name"));
          } catch (Exception e) {
            return "FAILURE:" + e.toString();
          } 
        } 
        return "FAILURE:Job could not be renamed to:" + JobScheduler.safeName(request.getProperty("name"));
      } 
    } 
    return "FAILURE:Job not found.";
  }
  
  public static Object removeJob(Properties request, String site) {
    return removeJob(request, site, true);
  }
  
  public static Object removeJob(Properties request, String site, boolean replicate) {
    if (ServerStatus.BG("replicate_jobs") && replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.removeJob", "info", pp);
    } 
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
        return p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return "FAILURE:Job not found.";
      } 
    } 
    Vector jobs = JobScheduler.getJobList(false);
    for (int x = 0; x < jobs.size(); x++) {
      if (request.getProperty("name", "").equalsIgnoreCase(jobName(jobs.elementAt(x)))) {
        Common.recurseDelete(((File_S)jobs.elementAt(x)).getPath(), false);
        return "SUCCESS:" + JobScheduler.safeName(request.getProperty("name"));
      } 
    } 
    return "FAILURE:Job not found.";
  }
  
  public static Object addJob(Properties request, String site) {
    return addJob(request, site, true);
  }
  
  public static Object addJob(Properties request, String site, boolean replicate) {
    if (ServerStatus.BG("replicate_jobs") && replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.addJob", "info", pp);
    } 
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
        return p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return "FAILURE:Job create failed.";
      } 
    } 
    Properties job = null;
    try {
      job = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("data").replace('+', ' ')).getBytes("UTF8")));
      if (job == null)
        throw new Exception("Invalid xml for job"); 
      Common.urlDecodePost(request);
      String jobName = JobScheduler.safeName(Common.dots(request.getProperty("name")));
      (new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName)).mkdirs();
      (new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job2.XML")).delete();
      Common.writeXMLObject(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job2.XML", job, "job");
      if ((new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job.XML")).exists()) {
        (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + jobName + "_19.XML")).delete();
        for (int x = 18; x >= 0; x--) {
          try {
            (new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + jobName + "_" + x + ".XML")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + jobName + "_" + (x + 1) + ".XML"));
          } catch (Exception exception) {}
        } 
        (new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job.XML")).renameTo(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + jobName + "_0.XML"));
        (new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job.XML")).delete();
      } 
      if ((new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job2.XML")).length() > 0L) {
        (new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job2.XML")).renameTo(new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job.XML"));
      } else {
        throw new Exception("Failed to save job...0 byte save, aborting.");
      } 
      return "SUCCESS:" + jobName;
    } catch (Exception e) {
      return "FAILURE:" + e.toString();
    } 
  }
  
  public static Object getServerRoots(Properties request, String site) {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties properties = DMZServer.getResponse(id, 20);
        return properties.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return "FAILURE:getServerRoots failed.";
      } 
    } 
    Properties p = new Properties();
    p.put("server.root", System.getProperty("crushftp.server.root", ""));
    p.put("user.root", System.getProperty("crushftp.user.root", ""));
    return p;
  }
  
  public static Vector stripUnrelatedAdminItems(String key, String admin_group_name) {
    Vector v = (Vector)ServerStatus.VG(key).clone();
    for (int xx = v.size() - 1; xx >= 0; xx--) {
      Properties pp = v.elementAt(xx);
      if (!pp.getProperty("name", "").toUpperCase().startsWith(admin_group_name.toUpperCase()))
        v.removeElementAt(xx); 
    } 
    if (v.size() == 0)
      v = ServerStatus.VG(key); 
    return v;
  }
  
  public static String getStatHistory(Properties request) throws Exception {
    StringBuffer xml = new StringBuffer();
    String[] params = request.getProperty("params").split(",");
    for (int x = 0; x < params.length; x++) {
      String param = params[x].trim();
      Properties si = null;
      if (request.getProperty("instance", "").equals("")) {
        si = ServerStatus.thisObj.server_info;
      } else {
        String id = Common.makeBoundary();
        DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
        Properties p = DMZServer.getResponse(id, 20);
        si = (Properties)p.get("data");
      } 
      Vector v = (Vector)si.get(String.valueOf(param) + "_history");
      if (v == null) {
        v = new Vector();
        v.addElement(si.getProperty(param));
      } 
      int loc = v.size() - 1;
      int intervals = Integer.parseInt(request.getProperty("priorIntervals", "1"));
      xml.append("<" + param + ">");
      while (intervals > 0 && loc >= 0) {
        xml.append(v.elementAt(loc).toString()).append((intervals > 1 || loc > 0) ? "," : "");
        intervals--;
        loc--;
      } 
      xml.append("</" + param + ">");
    } 
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response_data>" + xml + "</response_data></result>";
    return response;
  }
  
  static Properties expandUsernames_cache = new Properties();
  
  static String expandUsernames(String users) {
    if (expandUsernames_cache.containsKey(users) && System.currentTimeMillis() - Long.parseLong(expandUsernames_cache.getProperty(String.valueOf(users) + "time", "0")) < 30000L)
      return expandUsernames_cache.getProperty(users); 
    String userNamesStr = "";
    Properties groupsAll = new Properties();
    if (users.indexOf("...") >= 0) {
      Vector the_list = (Vector)((Vector)ServerStatus.server_settings.get("server_list")).clone();
      for (int i = 0; i < the_list.size(); i++) {
        Properties server_item = the_list.elementAt(i);
        Properties groups = UserTools.getGroups(server_item.getProperty("linkedServer"));
        Enumeration keys = groups.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          groupsAll.put("..." + key.toUpperCase(), groups.get(key));
        } 
      } 
    } 
    String[] usernames = users.split(",");
    for (int xx = 0; xx < usernames.length; xx++) {
      String username = usernames[xx].toUpperCase().trim();
      if (username.startsWith("...")) {
        Vector usernames2 = (Vector)groupsAll.get(username);
        for (int xxx = 0; xxx < usernames2.size(); xxx++)
          userNamesStr = String.valueOf(userNamesStr) + usernames2.elementAt(xxx).toString().toUpperCase().trim() + ","; 
      } else {
        userNamesStr = String.valueOf(userNamesStr) + username + ",";
      } 
    } 
    expandUsernames_cache.put(String.valueOf(users) + "_time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    expandUsernames_cache.put(users, userNamesStr);
    return userNamesStr;
  }
  
  public static Vector getJobsSummary(Properties request, String site) throws Exception {
    if (ServerStatus.siIG("enterprise_level") <= 0)
      return new Vector(); 
    Properties si = null;
    if (request.getProperty("instance", "").equals("")) {
      si = ServerStatus.thisObj.server_info;
    } else {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
        return (Vector)p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return null;
      } 
    } 
    Vector jobs = (Vector)si.get("running_tasks");
    Vector vv = new Vector();
    for (int x = 0; x < jobs.size(); x++) {
      Properties tracker = jobs.elementAt(x);
      Properties summaryJob = new Properties();
      summaryJob.put("name", "");
      summaryJob.put("start", tracker.getProperty("start", ""));
      summaryJob.put("id", tracker.getProperty("id", ""));
      summaryJob.put("log_file", tracker.getProperty("log_file", ""));
      summaryJob.put("stop", tracker.getProperty("stop", ""));
      summaryJob.put("status", tracker.getProperty("status", ""));
      Properties settings = (Properties)tracker.get("settings");
      if (settings == null)
        settings = new Properties(); 
      Properties set2 = new Properties();
      set2.put("pluginName", settings.getProperty("pluginName", ""));
      set2.put("subItem", settings.getProperty("subItem", ""));
      set2.put("scheduleName", settings.getProperty("scheduleName", ""));
      set2.put("name", settings.getProperty("name", ""));
      set2.put("id", settings.getProperty("id", ""));
      summaryJob.put("settings", set2);
      if (site.indexOf("(JOB_MONITOR)") < 0 || expandUsernames(settings.getProperty("allowed_usernames", "")).indexOf(request.getProperty("calling_user", "~NONE~").toUpperCase()) >= 0)
        vv.insertElementAt(summaryJob, 0); 
    } 
    long start_time = Long.parseLong(request.getProperty("start_time", "0"));
    long end_time = Long.parseLong(request.getProperty("end_time", "0"));
    jobs = JobScheduler.getJobList(request.getProperty("hideUserActiveSchedules", "false").equals("false"));
    SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss", Locale.US);
    Vector vv2 = new Vector();
    String filter = request.getProperty("filter", "").toUpperCase();
    synchronized (jobs_summary_cache) {
      for (int i = 0; i < jobs.size(); i++) {
        File_S f = (File_S)jobs.elementAt(i);
        File_S[] f2 = (File_S[])f.listFiles();
        for (int xx = 0; f2 != null && xx < f2.length; xx++) {
          String job_name = f2[xx].getName();
          if (!job_name.equalsIgnoreCase("job.XML") && 
            !job_name.equalsIgnoreCase("inprogress.XML") && 
            !job_name.equalsIgnoreCase("inprogress") && 
            job_name.toUpperCase().endsWith(".XML")) {
            job_name = job_name.substring(0, job_name.lastIndexOf(".XML"));
            if (job_name.indexOf("_") < 0)
              continue; 
            String job_id = job_name.split("_")[0];
            try {
              if (!job_name.split("_")[1].equals("new") && sdf.parse(job_name.split("_")[1]).getTime() >= start_time && sdf.parse(job_name.split("_")[1]).getTime() <= end_time) {
                Properties summaryJob = null;
                if (jobs_summary_cache.containsKey(f2[xx].getPath())) {
                  Properties sj = (Properties)jobs_summary_cache.get(f2[xx].getPath());
                  if (site.indexOf("(JOB_MONITOR)") >= 0) {
                    Properties settings = (Properties)sj.get("settings");
                    if (expandUsernames(settings.getProperty("allowed_usernames", "")).indexOf(request.getProperty("calling_user", "~NONE~").toUpperCase()) < 0)
                      continue; 
                  } 
                  if (sj.getProperty("modified", "0").equals((new StringBuffer(String.valueOf(f2[xx].lastModified()))).toString()))
                    summaryJob = sj; 
                } 
                boolean ok = false;
                if (summaryJob == null) {
                  Properties tracker = (Properties)Common.readXMLObject(f2[xx].getPath());
                  Properties settings = (Properties)tracker.get("settings");
                  if (site.indexOf("(JOB_MONITOR)") >= 0 && expandUsernames(settings.getProperty("allowed_usernames", "")).indexOf(request.getProperty("calling_user", "~NONE~").toUpperCase()) < 0)
                    continue; 
                  summaryJob = new Properties();
                  summaryJob.put("name", "");
                  summaryJob.put("start", tracker.getProperty("start", ""));
                  summaryJob.put("end", tracker.getProperty("end", "0"));
                  summaryJob.put("id", job_id);
                  summaryJob.put("log_file", tracker.getProperty("log_file", ""));
                  summaryJob.put("stop", tracker.getProperty("stop", ""));
                  summaryJob.put("status", tracker.getProperty("status", ""));
                  if (settings == null)
                    settings = new Properties(); 
                  Properties set2 = new Properties();
                  set2.put("pluginName", settings.getProperty("pluginName", ""));
                  set2.put("subItem", settings.getProperty("subItem", ""));
                  set2.put("scheduleName", settings.getProperty("scheduleName", ""));
                  set2.put("name", settings.getProperty("name", ""));
                  set2.put("id", settings.getProperty("id", ""));
                  if (site.indexOf("(JOB_MONITOR)") >= 0)
                    set2.put("allowed_usernames", settings.getProperty("allowed_usernames", "")); 
                  summaryJob.put("settings", set2);
                  summaryJob.put("modified", (new StringBuffer(String.valueOf(f2[xx].lastModified()))).toString());
                  jobs_summary_cache.put(f2[xx].getPath(), summaryJob);
                } 
                if (!filter.equals("")) {
                  Properties settings = (Properties)summaryJob.get("settings");
                  if (settings.getProperty("scheduleName", "").toUpperCase().indexOf(filter) >= 0) {
                    ok = true;
                  } else if (summaryJob.getProperty("status", "").toUpperCase().indexOf(filter) >= 0) {
                    ok = true;
                  } else {
                    Date start_d = new Date(Long.parseLong(summaryJob.getProperty("start", "0")));
                    Date end_d = new Date(Long.parseLong(summaryJob.getProperty("end", "0")));
                    Date duration_d = new Date(Long.parseLong(summaryJob.getProperty("end", "0")) - Long.parseLong(summaryJob.getProperty("start", "0")));
                    SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss aa");
                    SimpleDateFormat sdf3 = new SimpleDateFormat("hh:mm:ss aa");
                    String s1 = sdf2.format(start_d).toUpperCase();
                    String s2 = sdf2.format(end_d).toUpperCase();
                    String s3 = sdf3.format(duration_d).toUpperCase();
                    if (s1.indexOf(filter) >= 0) {
                      ok = true;
                    } else if (s2.indexOf(filter) >= 0) {
                      ok = true;
                    } else if (s3.indexOf(filter) >= 0) {
                      ok = true;
                    } 
                  } 
                } else {
                  ok = true;
                } 
                if (ok)
                  vv2.insertElementAt(summaryJob, 0); 
              } 
            } catch (Exception e) {
              Log.log("SERVER", 1, e);
            } 
          } 
          continue;
        } 
      } 
    } 
    Common.do_sort(vv, "", "start");
    Common.do_sort(vv2, "", "start");
    while (vv2.size() > ServerStatus.IG("max_job_summary_scan"))
      vv2.removeElementAt(vv2.size() - 1); 
    vv2.addAll(vv);
    vv = vv2;
    synchronized (jobs_summary_cache) {
      if (System.currentTimeMillis() - last_job_cache_clean > 60000L) {
        Enumeration keys = jobs_summary_cache.keys();
        while (keys.hasMoreElements()) {
          String key = keys.nextElement().toString();
          Properties sj = (Properties)jobs_summary_cache.get(key);
          File_S f = new File_S(key);
          if (!f.exists() || !sj.getProperty("modified", "0").equals((new StringBuffer(String.valueOf(f.lastModified()))).toString()))
            jobs_summary_cache.remove(key); 
        } 
        last_job_cache_clean = System.currentTimeMillis();
      } 
    } 
    return vv;
  }
  
  public static Vector getJobInfo(Properties request, String site) throws Exception {
    if (ServerStatus.siIG("enterprise_level") <= 0)
      return new Vector(); 
    Properties si = null;
    if (request.getProperty("instance", "").equals("")) {
      si = ServerStatus.thisObj.server_info;
    } else {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
        return (Vector)p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return null;
      } 
    } 
    Vector jobs = (Vector)si.get("running_tasks");
    Vector vv = new Vector();
    int x;
    for (x = 0; vv.size() == 0 && x < jobs.size(); x++) {
      Properties job = jobs.elementAt(x);
      if (job.getProperty("id").equals(request.getProperty("job_id"))) {
        Properties settings = (Properties)job.get("settings");
        if (settings == null || site.indexOf("(JOB_MONITOR)") < 0 || expandUsernames(settings.getProperty("allowed_usernames", "")).indexOf(request.getProperty("calling_user", "~NONE~").toUpperCase()) >= 0)
          vv.addElement(job.clone()); 
      } 
    } 
    if (vv.size() == 0)
      if (!request.getProperty("scheduleName", "").equals("")) {
        File_S f = new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + Common.dots(request.getProperty("scheduleName", "")));
        checkJobFolder(site, request, vv, f);
      } else {
        jobs = JobScheduler.getJobList(false);
        for (x = 0; vv.size() == 0 && x < jobs.size(); x++)
          checkJobFolder(site, request, vv, (File_S)jobs.elementAt(x)); 
      }  
    for (x = 0; x < vv.size(); x++) {
      Properties tracker = (Properties)vv.elementAt(x);
      if (request.getProperty("extra_keys", "settings").indexOf("settings") < 0)
        tracker.remove("settings"); 
      Vector active_items = (Vector)tracker.get("active_items");
      if (active_items != null) {
        active_items = (Vector)active_items.clone();
        tracker.put("active_items", active_items);
        for (int xx = 0; xx < active_items.size(); xx++) {
          Properties active_item = active_items.elementAt(xx);
          active_item = (Properties)active_item.clone();
          active_items.setElementAt(active_item, xx);
          Vector newItems = (Vector)active_item.get("newItems");
          Vector items = (Vector)active_item.get("items");
          active_item.put("newItems_count", "0");
          active_item.put("items_count", "0");
          if (newItems != null)
            active_item.put("newItems_count", (new StringBuffer(String.valueOf(newItems.size()))).toString()); 
          if (items != null)
            active_item.put("items_count", (new StringBuffer(String.valueOf(items.size()))).toString()); 
          if (items != null && items.size() == 1) {
            Properties p = items.elementAt(0);
            if (p.containsKey("incoming_count"))
              active_item.put("items_count", p.getProperty("incoming_count")); 
          } 
          if (newItems != null && newItems.size() == 1) {
            Properties p = newItems.elementAt(0);
            if (p.containsKey("incoming_count"))
              active_item.put("newItems_count", p.getProperty("outgoing_count")); 
          } 
          if (request.getProperty("extra_keys", "active_prefs").indexOf("active_prefs") < 0 || site.indexOf("(JOB_MONITOR)") >= 0)
            active_item.remove("prefs"); 
          if (request.getProperty("extra_keys", "active_items").indexOf("active_items") < 0 || site.indexOf("(JOB_MONITOR)") >= 0)
            active_item.remove("items"); 
          if (request.getProperty("extra_keys", "active_newItems").indexOf("active_newItems") < 0 || site.indexOf("(JOB_MONITOR)") >= 0)
            active_item.remove("newItems"); 
          if (active_item.containsKey("items")) {
            Vector items2 = (Vector)Common.CLONE(active_item.get("items"));
            active_item.put("items", items2);
            for (int xxx = 0; xxx < items2.size(); xxx++) {
              Properties item = items2.elementAt(xxx);
              if (!item.getProperty("url", "FILE:").toUpperCase().startsWith("FILE:"))
                item.put("url", (new VRL(item.getProperty("url"))).safe()); 
            } 
          } 
          if (active_item.containsKey("newItems")) {
            Vector items2 = (Vector)Common.CLONE(active_item.get("newItems"));
            active_item.put("newItems", items2);
            for (int xxx = 0; xxx < items2.size(); xxx++) {
              Properties item = items2.elementAt(xxx);
              if (!item.getProperty("url", "FILE:").toUpperCase().startsWith("FILE:"))
                item.put("url", (new VRL(item.getProperty("url"))).safe()); 
            } 
          } 
        } 
      } 
    } 
    return vv;
  }
  
  public static void checkJobFolder(String site, Properties request, Vector vv, File_S f) {
    File_S[] f2 = (File_S[])f.listFiles();
    for (int xx = 0; vv.size() == 0 && f2 != null && xx < f2.length; xx++) {
      String job_name = f2[xx].getName();
      if (!job_name.equalsIgnoreCase("job.XML") && 
        !job_name.equalsIgnoreCase("inprogress.XML") && 
        !job_name.equalsIgnoreCase("inprogress") && 
        job_name.toUpperCase().endsWith(".XML")) {
        job_name = job_name.substring(0, job_name.lastIndexOf(".XML"));
        String job_id = job_name.split("_")[0];
        if (job_id.equals(request.getProperty("job_id"))) {
          Properties tracker = (Properties)Common.readXMLObject(f2[xx].getPath());
          tracker.put("job_history_obj_path", f2[xx].getPath());
          if (site.indexOf("(JOB_MONITOR)") >= 0) {
            Properties settings = (Properties)tracker.get("settings");
            if (expandUsernames(settings.getProperty("allowed_usernames", "")).indexOf(request.getProperty("calling_user", "~NONE~").toUpperCase()) < 0)
              continue; 
          } 
          vv.addElement(tracker.clone());
        } 
      } 
      continue;
    } 
  }
  
  public static Vector getSessionList(Properties request) throws Exception {
    Properties si = null;
    if (request.getProperty("instance", "").equals("")) {
      si = ServerStatus.thisObj.server_info;
    } else {
      String id = Common.makeBoundary();
      DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
      Properties p = DMZServer.getResponse(id, 20);
      si = (Properties)p.get("data");
    } 
    Vector v = (Vector)((Vector)si.get(request.getProperty("session_list"))).clone();
    Vector vv = new Vector();
    for (int x = 0; x < v.size(); x++) {
      Properties user_info = v.elementAt(x);
      Properties p = new Properties();
      p.put("user_name", user_info.getProperty("user_name"));
      p.put("user_number", user_info.getProperty("user_number"));
      p.put("user_ip", user_info.getProperty("user_ip"));
      p.put("user_protocol", user_info.getProperty("user_protocol"));
      p.put("current_dir", user_info.getProperty("current_dir"));
      p.put("last_activity", user_info.getProperty("last_activity", "0"));
      vv.addElement(p);
    } 
    return vv;
  }
  
  public static String newFolder(Properties request, String site, boolean replicate) {
    if (replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.newFolder", "info", pp);
    } 
    if (Common.machine_is_x() && !(new File_U(request.getProperty("path"))).exists())
      request.put("path", "/Volumes" + request.getProperty("path")); 
    if (!(new File_U(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).mkdirs())
      return "New Folder Failed!"; 
    Common.updateOSXInfo(String.valueOf(request.getProperty("path")) + request.getProperty("name"));
    return "OK";
  }
  
  public static String renameItem(Properties request, String site, boolean replicate) {
    if (replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.renameItem", "info", pp);
    } 
    if (Common.machine_is_x() && !(new File_U(request.getProperty("path"))).exists())
      request.put("path", "/Volumes" + request.getProperty("path")); 
    if (!(new File_U(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).renameTo(new File_U(String.valueOf(request.getProperty("path")) + request.getProperty("newName"))))
      return "Rename Failed!"; 
    return "OK";
  }
  
  public static String duplicateItem(Properties request, String site, boolean replicate) {
    if (replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.duplicateItem", "info", pp);
    } 
    if (Common.machine_is_x() && !(new File_U(request.getProperty("path"))).exists())
      request.put("path", "/Volumes" + request.getProperty("path")); 
    Vector list = new Vector();
    try {
      Common.getAllFileListing(list, (new File_U(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).getCanonicalPath(), 5, true);
      if (list.size() > 100)
        return "Too many items to allow duplicate! " + list.size(); 
      Common.recurseCopy_U((new File_U(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).getCanonicalPath(), String.valueOf((new File_U(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).getCanonicalPath()) + "_tmp_" + Common.makeBoundary(), false);
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
      return (String)e;
    } 
    return "OK";
  }
  
  public static String deleteItem(Properties request, String site, boolean replicate) {
    if (replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.deleteItem", "info", pp);
    } 
    if (Common.machine_is_x() && !(new File_U(request.getProperty("path"))).exists())
      request.put("path", "/Volumes" + request.getProperty("path")); 
    if (!(new File_U(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).exists())
      return "Item not found."; 
    if (!(new File_U(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).delete())
      return "Delete Failed. (Folders must be empty to be deleted.)"; 
    return "OK";
  }
  
  public static String setServerItem(Properties request, String site) {
    return setServerItem(request, site, true);
  }
  
  public static String setServerItem(Properties request, String site, boolean replicate) {
    String status = "OK";
    try {
      if (!request.getProperty("instance", "").equals(""))
        synchronized (DMZServer.stop_send_prefs) {
          String id = Common.makeBoundary();
          String instance = request.remove("instance").toString();
          DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
          Properties p = DMZServer.getResponse(id, 20);
          if (request.getProperty("key").indexOf("server_settings/") >= 0) {
            String id2 = Common.makeBoundary();
            DMZServer.sendCommand(instance, new Properties(), "GET:SERVER_SETTINGS", id2);
            Properties pp = DMZServer.getResponse(id2, 20);
            SharedSessionReplicated.send("", "WRITE_PREFS", instance, pp.get("data"));
            Thread.sleep(200L);
            Common.write_server_settings((Properties)pp.get("data"), instance);
          } 
          return p.get("data").toString();
        }  
      if (replicate && request.getProperty("key").indexOf("/server_list/") < 0 && request.getProperty("data", "").indexOf("<replicate_session_host_port>") < 0) {
        Properties pp = new Properties();
        pp.put("request", request);
        pp.put("site", site);
        SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.setServerItem", "info", pp);
      } 
      String original_disabled_ciphers = ServerStatus.SG("disabled_ciphers");
      String[] keys = request.getProperty("key").split("/");
      Object o = null;
      StringBuffer log_summary = new StringBuffer();
      try {
        for (int x = 0; x < keys.length - 1; x++) {
          String key = keys[x];
          if (key.equals("server_settings")) {
            o = ServerStatus.server_settings;
          } else if (key.equals("server_info")) {
            o = ServerStatus.thisObj.server_info;
          } else if (o instanceof Properties) {
            o = ((Properties)o).get(key);
          } else if (o instanceof Vector) {
            o = ((Vector)o).elementAt(Integer.parseInt(key));
          } 
        } 
        String lastKey = keys[keys.length - 1];
        String secondLastKey = "";
        if (keys.length >= 2)
          secondLastKey = keys[keys.length - 2]; 
        if (request.getProperty("key").equals("server_settings")) {
          o = ServerStatus.server_settings;
          lastKey = "server_prefs";
        } 
        Vector preview_config = ServerStatus.VG("preview_configs");
        Vector locked_preview = new Vector();
        for (int i = 0; i < preview_config.size(); i++) {
          Properties p = preview_config.elementAt(i);
          Properties locked_p = new Properties();
          locked_p.put("preview_command_line", p.getProperty("preview_command_line", ""));
          locked_p.put("preview_working_dir", p.getProperty("preview_working_dir", ""));
          locked_p.put("preview_environment", p.getProperty("preview_environment", ""));
          locked_p.put("preview_frames", p.getProperty("preview_frames", "1"));
          locked_p.put("preview_movie_info_command_line", p.getProperty("preview_movie_info_command_line", ""));
          locked_p.put("preview_exif_get_command_line", p.getProperty("preview_exif_get_command_line", ""));
          locked_p.put("preview_exif_set_command_line", p.getProperty("preview_exif_set_command_line", ""));
          locked_preview.addElement(locked_p);
        } 
        Object new_o = null;
        if (!request.getProperty("data_type").equals("text")) {
          if (request.getProperty("data").equals("") && request.getProperty("data_type").equals("vector")) {
            new_o = new Vector();
          } else {
            new_o = Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("data").replace('+', ' ')).getBytes("UTF8")));
          } 
        } else {
          new_o = Common.url_decode(request.getProperty("data", "").replace('+', ' '));
        } 
        if (o instanceof Properties) {
          if (lastKey.equals("server_prefs") && (new_o instanceof Properties || new_o instanceof Vector)) {
            Common.updateObjectLog(new_o, o, request.getProperty("key"), true, log_summary);
          } else if (new_o instanceof Properties || new_o instanceof Vector) {
            Common.updateObjectLog(new_o, ((Properties)o).get(lastKey), request.getProperty("key"), true, log_summary);
          } else {
            ((Properties)o).put(lastKey, new_o.toString());
          } 
          if (!System.getProperty("crushftp.user.root", "").equals("") || !System.getProperty("crushftp.server.root", "").equals("")) {
            preview_config = ServerStatus.VG("preview_configs");
            for (int j = 0; j < locked_preview.size(); j++) {
              Properties locked_p = locked_preview.elementAt(j);
              Properties p = preview_config.elementAt(j);
              p.putAll(locked_p);
            } 
          } 
        } else if (o instanceof Vector) {
          Vector v = (Vector)o;
          if (request.getProperty("data_action", "").equals("reset")) {
            if (request.getProperty("key").indexOf("/plugins/") >= 0) {
              if (new_o == null)
                new_o = new Vector(); 
              Common.updateObjectLog(new_o, v, request.getProperty("key"), true, log_summary);
            } else {
              if (new_o == null)
                new_o = new Vector(); 
              Common.updateObjectLog(new_o, v, request.getProperty("key"), true, log_summary);
            } 
          } else {
            int k = Integer.parseInt(lastKey);
            if (request.getProperty("data_action", "").equals("remove")) {
              Object delO = v.remove(k);
              Common.updateObjectLogOnly(delO, String.valueOf(request.getProperty("key")) + ":remove ", log_summary);
            } else if (request.getProperty("data_action", "").equals("move_left")) {
              Object o2 = v.elementAt(k);
              if (k > 0) {
                Object o1 = v.elementAt(k - 1);
                v.setElementAt(o2, k - 1);
                v.setElementAt(o1, k);
                if (k - 1 == 0)
                  ((Properties)o1).put("subItem", ""); 
              } 
              Common.updateObjectLogOnly(o2, String.valueOf(request.getProperty("key")) + ":move_left " + k, log_summary);
            } else if (request.getProperty("data_action", "").equals("move_right")) {
              Object o2 = v.elementAt(k);
              if (k <= v.size() - 2) {
                Object o1 = v.elementAt(k + 1);
                v.setElementAt(o2, k + 1);
                v.setElementAt(o1, k);
              } 
              Common.updateObjectLogOnly(o2, String.valueOf(request.getProperty("key")) + ":move_right " + k, log_summary);
            } else if (k > v.size() - 1) {
              v.addElement(new_o);
              Common.updateObjectLogOnly(new_o, String.valueOf(request.getProperty("key")) + ":add " + v.size(), log_summary);
              if (v == (Vector)ServerStatus.server_settings.get("server_list"))
                ServerStatus.thisObj.start_this_server(k); 
            } else if (new_o instanceof Properties || new_o instanceof Vector) {
              Common.updateObjectLog(new_o, v.elementAt(k), request.getProperty("key"), true, log_summary);
            } else {
              v.setElementAt(new_o.toString(), k);
              Common.updateObjectLogOnly(new_o, String.valueOf(request.getProperty("key")) + "/" + k + " " + k + "=", log_summary);
            } 
          } 
          if (!System.getProperty("crushftp.user.root", "").equals("") || !System.getProperty("crushftp.server.root", "").equals("")) {
            preview_config = ServerStatus.VG("preview_configs");
            for (int k = 0; k < locked_preview.size(); k++) {
              Properties locked_p = locked_preview.elementAt(k);
              Properties p = preview_config.elementAt(k);
              p.putAll(locked_p);
            } 
          } 
          if (request.getProperty("key").indexOf("/plugins/") >= 0)
            ServerStatus.thisObj.common_code.loadPluginsSync(ServerStatus.server_settings, ServerStatus.thisObj.server_info); 
          if (request.getProperty("key").indexOf("server_settings") >= 0)
            ServerStatus.thisObj.reset_preview_workers(); 
          int j;
          for (j = 0; j < ServerStatus.VG("server_list").size(); j++) {
            Properties new_server_item = ServerStatus.VG("server_list").elementAt(j);
            for (int xx = 0; xx < ServerStatus.thisObj.main_servers.size(); xx++) {
              GenericServer gs = ServerStatus.thisObj.main_servers.elementAt(xx);
              if (gs.server_item.getProperty("serverType", "").equals(new_server_item.getProperty("serverType", "")) && gs.server_item.getProperty("ip", "").equals(new_server_item.getProperty("ip", "")) && gs.server_item.getProperty("port", "").equals(new_server_item.getProperty("port", ""))) {
                gs.server_item = new_server_item;
                gs.updateStatus();
                break;
              } 
            } 
          } 
          if (!original_disabled_ciphers.equals(ServerStatus.SG("disabled_ciphers")))
            for (j = 0; j < ServerStatus.thisObj.main_servers.size(); j++) {
              GenericServer gs = ServerStatus.thisObj.main_servers.elementAt(j);
              if (gs.server_item.getProperty("serverType", "").equals("HTTPS") || gs.server_item.getProperty("serverType", "").equals("FTPS")) {
                ServerStatus.thisObj.stop_this_server(j);
                ServerStatus.thisObj.start_this_server(j);
              } 
            }  
          QuickConnect.ip_cache.clear();
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
        status = "FAILURE:" + e.toString();
      } 
      Properties template = Common.get_email_template("Change Setting Email");
      if (template != null) {
        String body = template.getProperty("emailBody");
        body = Common.replace_str(body, "{keys}", request.getProperty("key"));
        body = Common.replace_str(body, "{summary}", log_summary.toString());
        String subject = template.getProperty("emailSubject");
        subject = Common.replace_str(subject, "{keys}", request.getProperty("key"));
        subject = Common.replace_str(subject, "{summary}", log_summary.toString());
        Properties email_info = new Properties();
        email_info.put("server", ServerStatus.SG("smtp_server"));
        email_info.put("user", ServerStatus.SG("smtp_user"));
        email_info.put("pass", ServerStatus.SG("smtp_pass"));
        email_info.put("ssl", ServerStatus.SG("smtp_ssl"));
        email_info.put("html", ServerStatus.SG("smtp_html"));
        email_info.put("from", template.getProperty("emailFrom"));
        email_info.put("reply_to", template.getProperty("emailReplyTo"));
        email_info.put("to", template.getProperty("emailCC"));
        email_info.put("subject", subject);
        email_info.put("body", body);
        ServerStatus.thisObj.sendEmail(email_info);
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      Log.log("HTTP_SERVER", 1, request.getProperty("key"));
      status = e.toString();
    } 
    ServerStatus.thisObj.save_server_settings(false);
    return status;
  }
  
  public static Object getUser(Properties request, String site, SessionCrush thisSession) {
    if (site.indexOf("(CONNECT)") < 0)
      request.put("serverGroup", thisSession.server_item.getProperty("linkedServer")); 
    if (request.getProperty("serverGroup_original", "").equals("extra_vfs"))
      request.put("serverGroup", "extra_vfs"); 
    String status = "OK";
    String username = Common.url_decode(request.getProperty("username").replace('+', ' '));
    Vector extra_vfs = new Vector();
    if (request.getProperty("serverGroup").endsWith("_restored_backup")) {
      String source_path = String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + request.getProperty("user_zip_file");
      String dest_path = String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + request.getProperty("username");
      status = unzip_backup_folder(status, new File_S(source_path), dest_path);
      if (!request.getProperty("user_zip_file").contains("~")) {
        String file_end = request.getProperty("user_zip_file").substring(request.getProperty("user_zip_file").indexOf("-"), request.getProperty("user_zip_file").length());
        File_S[] folders = (File_S[])(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).listFiles();
        extra_vfs = new Vector();
        for (int x = 0; x < folders.length; x++) {
          File_S f = folders[x];
          if (f.getName().startsWith(String.valueOf(username) + "~") && f.getName().endsWith(file_end)) {
            Properties p = new Properties();
            p.put(f.getName().subSequence(f.getName().indexOf("~") + 1, f.getName().indexOf("-")), f.getName());
            extra_vfs.add(p);
          } 
        } 
      } 
    } 
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
        Properties p = DMZServer.getResponse(id, 20);
        return p.get("data");
      } 
      VFS uVFS = UserTools.ut.getVFS(request.getProperty("serverGroup"), username);
      Properties new_user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
      if (new_user == null || !new_user.getProperty("username", "not found").equalsIgnoreCase(username))
        throw new Exception("User not found:" + username); 
      if (site.indexOf("(CONNECT)") < 0 && site.indexOf("(USER_VIEW)") < 0 && site.indexOf("(USER_EDIT)") < 0) {
        Vector list = new Vector();
        Properties info = (Properties)thisSession.get("user_admin_info");
        list = (Vector)info.get("list");
        if (!username.equals(thisSession.SG("admin_group_name")))
          if (request.getProperty("serverGroup_original", "").equals("extra_vfs")) {
            if (list.indexOf(username.substring(0, username.lastIndexOf("~"))) < 0)
              throw new Exception("Username " + username + " not found."); 
          } else {
            if (new_user != null && !username.equals("default") && list.indexOf(username) < 0)
              return new Properties(); 
            if (list.indexOf(username) < 0 && !username.equals(thisSession.SG("admin_group_name")))
              throw new Exception("Username " + username + " not found."); 
          }  
      } 
      if (request.getProperty("serverGroup").endsWith("_restored_backup") && !request.getProperty("user_zip_file", "").contains("~")) {
        if (!extra_vfs.isEmpty())
          new_user.put("extra_vfs", extra_vfs); 
      } else {
        UserTools.getExtraVFS(request.getProperty("serverGroup"), username, null, new_user);
      } 
      Vector vfs_items = new Vector();
      Properties virtual = uVFS.homes.elementAt(0);
      Enumeration keys = virtual.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (key.equals("vfs_permissions_object"))
          continue; 
        Properties p = (Properties)virtual.get(key);
        String virtualPath = p.getProperty("virtualPath");
        if (virtualPath.equals(""))
          continue; 
        Properties dir_item = new Properties();
        if (p.getProperty("type").equalsIgnoreCase("DIR")) {
          if (!virtualPath.endsWith("/"))
            virtualPath = String.valueOf(virtualPath) + "/"; 
          dir_item.put("url", "");
          dir_item.put("type", "DIR");
        } else {
          Vector vItems = (Vector)p.get("vItems");
          dir_item = (Properties)((Properties)vItems.elementAt(0)).clone();
        } 
        dir_item.put("path", Common.all_but_last(virtualPath));
        dir_item.put("name", p.getProperty("name"));
        Vector wrapper = new Vector();
        wrapper.addElement(dir_item);
        vfs_items.addElement(wrapper);
      } 
      String pass = new_user.getProperty("password", "");
      if (!pass.startsWith("SHA:") && !pass.startsWith("SHA512:") && !pass.startsWith("SHA256:") && !pass.startsWith("SHA3:") && !pass.startsWith("MD5:") && !pass.startsWith("CRYPT3:") && !pass.startsWith("BCRYPT:") && !pass.startsWith("MD5CRYPT:") && !pass.startsWith("PBKDF2SHA256:") && !pass.startsWith("SHA512CRYPT:")) {
        pass = ServerStatus.thisObj.common_code.decode_pass(pass);
        new_user.put("password", pass);
      } 
      if (!new_user.getProperty("userVersion", "").equals("6") && !new_user.getProperty("as2EncryptKeystorePassword", "").equals("")) {
        new_user.put("as2EncryptKeystorePassword", ServerStatus.thisObj.common_code.encode_pass(new_user.getProperty("as2EncryptKeystorePassword", ""), "DES", ""));
        new_user.put("as2EncryptKeyPassword", ServerStatus.thisObj.common_code.encode_pass(new_user.getProperty("as2EncryptKeyPassword", ""), "DES", ""));
      } 
      Properties user_items = new Properties();
      user_items.put("user", new_user);
      if (uVFS.permissions == null)
        uVFS.permissions = new Vector(); 
      if (uVFS.permissions.size() == 0)
        uVFS.permissions.addElement(new Properties()); 
      user_items.put("permissions", uVFS.permissions.elementAt(0));
      user_items.put("vfs_items", vfs_items);
      if (new_user.containsKey("web_buttons")) {
        Vector buttons = (Vector)new_user.get("web_buttons");
        ServerSessionAJAX.fixButtons(buttons);
      } 
      if (request.getProperty("serverGroup").endsWith("_restored_backup"))
        Common.recurseDelete(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + request.getProperty("username"), false); 
      return user_items;
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = e.toString();
      return status;
    } 
  }
  
  private static String unzip_backup_folder(String status, File_S source_file, String dest_path) {
    (new File_S(dest_path)).mkdir();
    try {
      ZipInputStream zin = new ZipInputStream(new FileInputStream(source_file));
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        String path = entry.getName();
        if (entry.isDirectory()) {
          (new File_S(String.valueOf(dest_path) + path)).mkdirs();
          continue;
        } 
        File_S file_entry = new File_S(String.valueOf(dest_path) + path);
        if (!(new File_S(Common.all_but_last(String.valueOf(dest_path) + path))).exists())
          (new File_S(Common.all_but_last(String.valueOf(dest_path) + path))).mkdirs(); 
        RandomAccessFile out = new RandomAccessFile(file_entry, "rw");
        byte[] b = new byte[32768];
        int bytes_read = 0;
        while (bytes_read >= 0) {
          bytes_read = zin.read(b);
          if (bytes_read > 0 && out != null)
            out.write(b, 0, bytes_read); 
        } 
        out.close();
      } 
      zin.close();
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = e.toString();
    } 
    return status;
  }
  
  public static Object getPublicKeys(Properties request) {
    return "";
  }
  
  public static String setUserItem(Properties request, SessionCrush thisSession, String site) {
    String status = "OK";
    try {
      StringBuffer log_summary = new StringBuffer();
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      try {
        if (request.getProperty("xmlItem", "").equals("groups")) {
          Properties groups = null;
          if (request.getProperty("data_action", "").equals("add")) {
            groups = UserTools.getGroups(request.getProperty("serverGroup"));
            Vector group = (Vector)groups.get(request.getProperty("group_name"));
            if (group == null)
              group = new Vector(); 
            groups.put(request.getProperty("group_name"), group);
            if (!request.containsKey("usernames"))
              request.put("usernames", request.getProperty("username", "")); 
            String[] usernames = Common.url_decode(request.getProperty("usernames").replace('+', ' ')).split(";");
            for (int x = 0; x < usernames.length; x++)
              group.addElement(usernames[x].trim()); 
            Common.updateObjectLogOnly("add " + request.getProperty("usernames"), "users/" + request.getProperty("serverGroup") + "/inheritance/" + request.getProperty("group_name"), log_summary);
          } else if (request.getProperty("data_action", "").equals("delete")) {
            groups = UserTools.getGroups(request.getProperty("serverGroup"));
            Vector group = (Vector)groups.get(request.getProperty("group_name"));
            if (group == null)
              group = new Vector(); 
            groups.put(request.getProperty("group_name"), group);
            if (!request.containsKey("usernames"))
              request.put("usernames", request.getProperty("username", "")); 
            String[] usernames = Common.url_decode(request.getProperty("usernames").replace('+', ' ')).split(";");
            if (usernames.length == 0 || request.getProperty("usernames").equals("")) {
              groups.remove(request.getProperty("group_name"));
            } else {
              for (int x = 0; x < usernames.length; x++)
                group.remove(usernames[x].trim()); 
            } 
            Common.updateObjectLogOnly("delete " + request.getProperty("usernames"), "users/" + request.getProperty("serverGroup") + "/groups/" + request.getProperty("group_name"), log_summary);
          } else {
            groups = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(request.getProperty("groups").replace('+', ' ')).getBytes("UTF8")));
            Properties groups_original = UserTools.getGroups(request.getProperty("serverGroup"));
            Common.updateObjectLog(groups, groups_original, "users/" + request.getProperty("serverGroup") + "/groups", false, log_summary);
          } 
          if (groups == null)
            groups = new Properties(); 
          UserTools.writeGroups(request.getProperty("serverGroup"), groups);
        } else if (request.getProperty("xmlItem", "").equals("inheritance")) {
          Properties inheritances = null;
          if (request.getProperty("data_action", "").equals("add")) {
            inheritances = UserTools.getInheritance(request.getProperty("serverGroup"));
            if (!request.containsKey("usernames"))
              request.put("usernames", request.getProperty("username", "")); 
            String[] usernames = Common.url_decode(request.getProperty("usernames").replace('+', ' ')).split(";");
            for (int x = 0; x < usernames.length; x++) {
              Vector inherit = (Vector)inheritances.get(usernames[x]);
              if (inherit == null)
                inherit = new Vector(); 
              inherit.addElement(request.getProperty("inheritance_name"));
              inheritances.put(usernames[x], inherit);
            } 
            Common.updateObjectLogOnly("add " + request.getProperty("usernames"), "users/" + request.getProperty("serverGroup") + "/inheritance/" + request.getProperty("inheritance_name"), log_summary);
          } else if (request.getProperty("data_action", "").equals("delete")) {
            inheritances = UserTools.getInheritance(request.getProperty("serverGroup"));
            if (!request.containsKey("usernames"))
              request.put("usernames", request.getProperty("username", "")); 
            String[] usernames = Common.url_decode(request.getProperty("usernames").replace('+', ' ')).split(";");
            if (usernames.length == 0 || request.getProperty("usernames").equals("")) {
              Enumeration keys = inheritances.keys();
              while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                Vector inherit = (Vector)inheritances.get(key);
                for (int x = inherit.size() - 1; x >= 0; x--) {
                  if (inherit.elementAt(x).toString().equalsIgnoreCase(request.getProperty("inheritance_name")))
                    inherit.removeElementAt(x); 
                } 
                if (inherit.size() == 0)
                  inheritances.remove(key); 
              } 
            } else {
              for (int x = 0; x < usernames.length; x++) {
                Vector inherit = (Vector)inheritances.get(usernames[x]);
                if (inherit == null)
                  inherit = new Vector(); 
                inherit.remove(request.getProperty("inheritance_name"));
                inheritances.put(usernames[x], inherit);
              } 
            } 
            Common.updateObjectLogOnly("delete " + request.getProperty("usernames"), "users/" + request.getProperty("serverGroup") + "/inheritance/" + request.getProperty("inheritance_name"), log_summary);
          } else {
            inheritances = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(request.getProperty("inheritance").replace('+', ' ')).getBytes("UTF8")));
            Properties inheritances_original = UserTools.getInheritance(request.getProperty("serverGroup"));
            Common.updateObjectLog(inheritances, inheritances_original, "users/" + request.getProperty("serverGroup") + "/inheritance", false, log_summary);
          } 
          if (inheritances == null)
            inheritances = new Properties(); 
          UserTools.writeInheritance(request.getProperty("serverGroup"), inheritances);
        } else if (request.getProperty("xmlItem", "").equals("user")) {
          if (!request.containsKey("usernames"))
            request.put("usernames", request.getProperty("username", "")); 
          String[] usernames = Common.url_decode(request.getProperty("usernames").replace('+', ' ')).split(";");
          for (int x = 0; x < usernames.length; x++) {
            String username = usernames[x].trim();
            if (!username.equals("")) {
              if (request.getProperty("data_action").equals("delete")) {
                if (request.getProperty("expire_user", "false").equals("true")) {
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, true);
                  if (user != null)
                    UserTools.expireUserVFSTask(user, request.getProperty("serverGroup"), username); 
                } 
                Common.updateObjectLogOnly("delete ", "users/" + request.getProperty("serverGroup") + "/" + username, log_summary);
                UserTools.deleteUser(request.getProperty("serverGroup"), username);
                Vector user_list = new Vector();
                UserTools.refreshUserList(request.getProperty("serverGroup"), user_list);
                for (int xx = 0; xx < user_list.size(); xx++) {
                  String newUser = Common.dots(user_list.elementAt(xx).toString());
                  if (newUser.toUpperCase().endsWith(".SHARED")) {
                    File_S f = new File_S(String.valueOf(System.getProperty("crushftp.users")) + "/" + request.getProperty("serverGroup") + "/" + newUser + "/VFS/Shares/" + username);
                    if (f.exists()) {
                      Common.recurseDelete(f.getCanonicalPath(), false);
                      f = new File_S(String.valueOf(System.getProperty("crushftp.users")) + "/" + request.getProperty("serverGroup") + "/" + newUser + "/VFS/Shares/");
                      if (f.listFiles() == null || (f.listFiles()).length == 0)
                        UserTools.deleteUser(request.getProperty("serverGroup"), newUser); 
                    } 
                  } 
                } 
                File_U[] accounts = (File_U[])(new File_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/")).listFiles();
                for (int i = 0; accounts != null && i < accounts.length; i++) {
                  try {
                    if (accounts[i].getName().indexOf(",,") >= 0 && accounts[i].isDirectory()) {
                      String[] tokens = accounts[i].getName().split(",,");
                      Properties pp = new Properties();
                      for (int loop = 0; loop < tokens.length; loop++)
                        pp.put(tokens[loop].substring(0, tokens[loop].indexOf("=")).toUpperCase(), tokens[loop].substring(tokens[loop].indexOf("=") + 1)); 
                      if (username.equalsIgnoreCase(pp.getProperty("M"))) {
                        Common.recurseDelete_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + pp.getProperty("U") + pp.getProperty("P"), false);
                        Common.recurseDelete_U(accounts[i].getCanonicalPath(), false);
                      } 
                    } 
                  } catch (Exception e) {
                    Log.log("HTTP_SERVER", 1, e);
                  } 
                } 
              } else {
                Properties default_user = UserTools.ut.getUser(request.getProperty("serverGroup"), "default", false);
                Properties new_user = new Properties();
                if (!request.getProperty("user", "").equals(""))
                  new_user = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("user").replace('+', ' ')).getBytes("UTF8"))); 
                new_user.put("userVersion", "6");
                if (new_user.containsKey("password")) {
                  if (thisSession != null)
                    Log.log("SERVER", 0, String.valueOf(username) + " password changed by admin (" + thisSession.uiSG("user_name") + ")."); 
                  String pass = new_user.getProperty("password", "");
                  if (new_user.getProperty("salt", "").equals("") && default_user.getProperty("salt", "").equalsIgnoreCase("random"))
                    new_user.put("salt", Common.makeBoundary(8)); 
                  if (!pass.startsWith("SHA:") && !pass.startsWith("SHA512:") && !pass.startsWith("SHA256:") && !pass.startsWith("SHA3:") && !pass.startsWith("MD5:") && !pass.startsWith("CRYPT3:") && !pass.startsWith("BCRYPT:") && !pass.startsWith("MD5CRYPT:") && !pass.startsWith("PBKDF2SHA256:") && !pass.startsWith("SHA512CRYPT:")) {
                    pass = ServerStatus.thisObj.common_code.encode_pass(pass, ServerStatus.SG("password_encryption"), new_user.getProperty("salt", ""));
                    new_user.put("password", pass);
                  } else {
                    new_user.put("password", pass);
                  } 
                } 
                if (request.getProperty("data_action").equals("update") || request.getProperty("data_action").equals("update_vfs") || request.getProperty("data_action").equals("update_vfs_remove")) {
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  Common.updateObjectLog(new_user, user, "users/" + request.getProperty("serverGroup") + "/" + username, true, log_summary);
                  new_user = user;
                } 
                if (request.getProperty("data_action").equals("replace")) {
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  if (user != null)
                    Common.updateObjectLog(new_user, user, "users/" + request.getProperty("serverGroup") + "/" + username, false, log_summary); 
                } 
                if (request.getProperty("data_action").equals("new")) {
                  Common.updateObjectLogOnly("new ", "users/" + request.getProperty("serverGroup") + "/" + username, log_summary);
                  Common.updateObjectLog(new_user, new Properties(), "users/" + request.getProperty("serverGroup") + "/" + username, false, new StringBuffer());
                } 
                if (request.getProperty("data_action").equals("update_remove")) {
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  Common.updateObjectLogOnly(request.getProperty("update_remove_key", ""), "users/" + request.getProperty("serverGroup") + "/" + username, log_summary);
                  String[] keys = request.getProperty("update_remove_key", "").split(";");
                  for (int xx = 0; xx < keys.length; xx++)
                    user.remove(keys[xx]); 
                  new_user = user;
                } 
                UserTools.writeUser(request.getProperty("serverGroup"), username, new_user);
                UserTools.ut.getUser(request.getProperty("serverGroup"), username, true);
                if (request.containsKey("vfs_items") || request.containsKey("permissions"))
                  UserTools.writeVFS(request.getProperty("serverGroup"), username, processVFSSubmission(request, username, site, thisSession, true, log_summary)); 
                Properties template = Common.get_email_template("Change Email");
                if (template != null) {
                  Properties old_user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  if (old_user != null && new_user != null && !old_user.getProperty("email", "").equals(new_user.getProperty("email", ""))) {
                    String body = template.getProperty("emailBody");
                    body = Common.replace_str(body, "{old_email}", old_user.getProperty("email"));
                    body = Common.replace_str(body, "{new_email}", new_user.getProperty("email"));
                    body = Common.replace_str(body, "{summary}", log_summary.toString());
                    String subject = template.getProperty("emailSubject");
                    subject = Common.replace_str(subject, "{old_email}", old_user.getProperty("email"));
                    subject = Common.replace_str(subject, "{new_email}", new_user.getProperty("email"));
                    subject = Common.replace_str(subject, "{summary}", log_summary.toString());
                    new_user.put("username", username);
                    new_user.put("user_name", username);
                    body = ServerStatus.change_vars_to_values_static(body, new_user, new_user, null);
                    subject = ServerStatus.change_vars_to_values_static(subject, new_user, new_user, null);
                    String cc = ServerStatus.change_vars_to_values_static(template.getProperty("emailCC"), new_user, new_user, null);
                    String bcc = ServerStatus.change_vars_to_values_static(template.getProperty("emailBCC"), new_user, new_user, null);
                    Properties email_info = new Properties();
                    email_info.put("server", ServerStatus.SG("smtp_server"));
                    email_info.put("user", ServerStatus.SG("smtp_user"));
                    email_info.put("pass", ServerStatus.SG("smtp_pass"));
                    email_info.put("ssl", ServerStatus.SG("smtp_ssl"));
                    email_info.put("html", ServerStatus.SG("smtp_html"));
                    email_info.put("from", template.getProperty("emailFrom"));
                    email_info.put("reply_to", template.getProperty("emailReplyTo"));
                    email_info.put("to", String.valueOf(new_user.getProperty("email")) + "," + old_user.getProperty("email"));
                    email_info.put("cc", cc);
                    email_info.put("bcc", bcc);
                    email_info.put("subject", subject);
                    email_info.put("body", body);
                    ServerStatus.thisObj.sendEmail(email_info);
                  } 
                } 
                template = Common.get_email_template("Change User Email");
                if (template != null) {
                  String body = template.getProperty("emailBody");
                  body = Common.replace_str(body, "{new_email}", new_user.getProperty("email"));
                  body = Common.replace_str(body, "{summary}", log_summary.toString());
                  String subject = template.getProperty("emailSubject");
                  subject = Common.replace_str(subject, "{new_email}", new_user.getProperty("email"));
                  subject = Common.replace_str(subject, "{summary}", log_summary.toString());
                  new_user.put("username", username);
                  new_user.put("user_name", username);
                  body = ServerStatus.change_vars_to_values_static(body, new_user, new_user, null);
                  subject = ServerStatus.change_vars_to_values_static(subject, new_user, new_user, null);
                  String cc = ServerStatus.change_vars_to_values_static(template.getProperty("emailCC"), new_user, new_user, null);
                  String bcc = ServerStatus.change_vars_to_values_static(template.getProperty("emailBCC"), new_user, new_user, null);
                  Properties email_info = new Properties();
                  email_info.put("server", ServerStatus.SG("smtp_server"));
                  email_info.put("user", ServerStatus.SG("smtp_user"));
                  email_info.put("pass", ServerStatus.SG("smtp_pass"));
                  email_info.put("ssl", ServerStatus.SG("smtp_ssl"));
                  email_info.put("html", ServerStatus.SG("smtp_html"));
                  email_info.put("from", template.getProperty("emailFrom"));
                  email_info.put("reply_to", template.getProperty("emailReplyTo"));
                  email_info.put("to", cc);
                  email_info.put("cc", cc);
                  email_info.put("bcc", bcc);
                  email_info.put("subject", subject);
                  email_info.put("body", body);
                  ServerStatus.thisObj.sendEmail(email_info);
                } 
              } 
              UserTools.ut.forceMemoryReload(username);
            } 
          } 
        } else {
          status = "Unknown xmlItem:" + request.getProperty("xmlitem");
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
        status = "FAILURE:" + e.toString();
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = e.toString();
    } 
    return status;
  }
  
  public static Properties getUserList(Properties request, String site, SessionCrush thisSession) {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
        return (Properties)p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return null;
      } 
    } 
    if (site.indexOf("(CONNECT)") < 0)
      request.put("serverGroup", thisSession.server_item.getProperty("linkedServer")); 
    Vector list = new Vector();
    UserTools.refreshUserList(request.getProperty("serverGroup"), list);
    if (site.indexOf("(CONNECT)") < 0 && site.indexOf("(USER_VIEW)") < 0 && site.indexOf("(USER_EDIT)") < 0) {
      String groupName = thisSession.SG("admin_group_name");
      if (groupName.equals("") || groupName.equals("admin_group_name"))
        groupName = thisSession.uiSG("user_name"); 
      Properties info = UserTools.getAllowedUsers(groupName, request.getProperty("serverGroup"), list);
      Properties info2 = UserTools.getAllowedUsers("pendingSelfRegistration", request.getProperty("serverGroup"), list);
      list = (Vector)info.get("list");
      Vector list2 = (Vector)info2.get("list");
      for (int x = 0; x < list2.size(); x++) {
        String tempUsername = list2.elementAt(x).toString();
        if (list.indexOf(tempUsername) < 0)
          list.addElement(tempUsername); 
      } 
      thisSession.put("user_admin_info", info);
    } 
    Properties user_list = new Properties();
    user_list.put("user_list", list);
    return user_list;
  }
  
  static Object getUserXML(Properties request, String site, SessionCrush session) {
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
        Properties p = DMZServer.getResponse(id, 20);
        return p.get("data");
      } 
      Properties obj = null;
      if (request.getProperty("xmlItem", "").equals("group")) {
        if (site.indexOf("(USER_ADMIN)") >= 0) {
          String groupName = session.getProperty("admin_group_name", "");
          Properties obj2 = UserTools.getGroups(request.getProperty("serverGroup"));
          obj = new Properties();
          Enumeration keys = obj2.keys();
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if (key.toUpperCase().startsWith(String.valueOf(groupName.toUpperCase()) + "_"))
              obj.put(key.substring((String.valueOf(groupName.toUpperCase()) + "_").length()), obj2.remove(key)); 
          } 
        } else if (site.indexOf("(CONNECT)") >= 0 || site.indexOf("(REPORT_EDIT)") >= 0) {
          obj = UserTools.getGroups(request.getProperty("serverGroup"));
        } else if (site.indexOf("(USER_VIEW)") >= 0 || site.indexOf("(USER_EDIT)") >= 0) {
          obj = UserTools.getGroups(request.getProperty("serverGroup"));
        } 
      } else if (request.getProperty("xmlItem", "").equals("inheritance")) {
        if (site.indexOf("(CONNECT)") >= 0) {
          obj = UserTools.getInheritance(request.getProperty("serverGroup"));
        } else if (site.indexOf("(USER_ADMIN)") >= 0) {
          Properties info = (Properties)session.get("user_admin_info");
          obj = (Properties)info.get("inheritance");
        } else if (site.indexOf("(USER_VIEW)") >= 0 || site.indexOf("(USER_EDIT)") >= 0) {
          obj = UserTools.getInheritance(request.getProperty("serverGroup"));
        } 
      } 
      Properties result = new Properties();
      result.put("result_item", obj);
      return result;
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      return e.toString();
    } 
  }
  
  public static Properties getUserXMLListing(Properties request, String site, SessionCrush thisSession) throws Exception {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      Properties p = DMZServer.getResponse(id, 20);
      return (Properties)p.get("data");
    } 
    if (site.indexOf("(CONNECT)") < 0)
      request.put("serverGroup", thisSession.server_item.getProperty("linkedServer")); 
    String username = Common.url_decode(request.getProperty("username", "").replace('+', ' '));
    String parentUser = null;
    if (site.indexOf("(CONNECT)") < 0 && site.indexOf("(USER_VIEW)") < 0 && site.indexOf("(USER_EDIT)") < 0 && site.indexOf("(JOB_EDIT)") < 0) {
      Properties info = (Properties)thisSession.get("user_admin_info");
      Vector list = (Vector)info.get("list");
      if (request.getProperty("serverGroup_original", "").equals("extra_vfs")) {
        if (list.indexOf(username.substring(0, username.lastIndexOf("~"))) < 0)
          throw new Exception("Username " + username + " not found."); 
      } else if (list.indexOf(username) < 0) {
        throw new Exception("Username " + username + " not found.");
      } 
      String groupName = thisSession.SG("admin_group_name");
      if (groupName.equals("") || groupName.equals("admin_group_name"))
        groupName = thisSession.uiSG("user_name"); 
      parentUser = groupName;
    } 
    String path = Common.url_decode(request.getProperty("path", "").replace('+', ' '));
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    path = Common.dots(path);
    VFS tempVFS = processVFSSubmission(request, username, site, thisSession, false, new StringBuffer());
    Vector listing = UserTools.ut.get_virtual_list_fake(tempVFS, path, request.getProperty("serverGroup"), parentUser);
    if (listing.size() > 0 && listing.elementAt(0) instanceof String) {
      Properties p = new Properties();
      p.put("error", listing.elementAt(0));
      return p;
    } 
    return getListingInfo(listing, path);
  }
  
  public static Properties getAdminXMLListing(Properties request, SessionCrush thisSession, String site) throws Exception {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      Properties p = DMZServer.getResponse(id, 20);
      return (Properties)p.get("data");
    } 
    String path = Common.url_decode(request.getProperty("path", "").replace('+', ' '));
    if (path.startsWith("///") && !path.startsWith("////"))
      path = "/" + path; 
    if (path.startsWith("~"))
      path = Common.replace_str(path, "~", System.getProperty("user.home")); 
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    try {
      File_B[] items = new File_B[0];
      if (request.getProperty("file_mode", "").equals("server")) {
        if (!(new File_S(path)).exists() && Common.machine_is_x())
          path = "/Volumes" + path; 
        path = Common.dots(path);
        items = getFileItems(path);
      } else {
        if (!(new File_U(path)).exists() && Common.machine_is_x())
          path = "/Volumes" + path; 
        path = Common.dots(path);
        items = getFileItems_U(path);
      } 
      Vector listing = new Vector();
      for (int x = 0; x < items.length; x++) {
        Properties p = new Properties();
        p.put("name", items[x].getName());
        p.put("path", Common.all_but_last(items[x].getPath()));
        p.put("type", items[x].isDirectory() ? "DIR" : "FILE");
        p.put("size", (new StringBuffer(String.valueOf(items[x].length()))).toString());
        if (Common.machine_is_windows() && path.equals("/")) {
          p.put("name", items[x].getPath().substring(0, 2));
          p.put("path", "/");
        } 
        p.put("boot", (new StringBuffer(String.valueOf((path.equals("/") && Common.machine_is_x() && !items[x].getCanonicalPath().startsWith("/Volumes/"))))).toString());
        p.put("privs", "(read)(view)");
        p.put("owner", "user");
        p.put("group", "group");
        p.put("permissionsNum", "777");
        p.put("keywords", "");
        p.put("num_items", "1");
        p.put("preview", "");
        p.put("owner", "");
        p.put("owner", "");
        p.put("root_dir", p.getProperty("path"));
        p.put("url", (new StringBuffer(String.valueOf(items[x].toURI().toURL().toExternalForm()))).toString());
        listing.addElement(p);
      } 
      return getListingInfo(listing, path);
    } catch (Exception e) {
      Log.log("SERVER", 0, e);
      return new Properties();
    } 
  }
  
  private static File_B[] getFileItems(String path) {
    File_B[] items = (File_B[])null;
    if (path.equals("/") && Common.machine_is_x()) {
      try {
        File_B[] other_volumes = Common.convert_files_to_files_both((new File_S("/Volumes/")).listFiles());
        if (other_volumes == null) {
          other_volumes = new File_B[1];
          other_volumes[0] = new File_B(new File_S("/"));
        } 
        items = new File_B[other_volumes.length];
        for (int x = 0; x < other_volumes.length; x++)
          items[x] = other_volumes[x]; 
      } catch (Exception exception) {}
    } else if (path.equals("/") && Common.machine_is_windows()) {
      items = Common.convert_files_to_files_both(File_S.listRoots());
    } else {
      items = Common.convert_files_to_files_both((new File_S(path)).listFiles());
    } 
    return items;
  }
  
  private static File_B[] getFileItems_U(String path) {
    File_B[] items = (File_B[])null;
    if (path.equals("/") && Common.machine_is_x()) {
      try {
        File_B[] other_volumes = Common.convert_files_to_files_both((new File_U("/Volumes/")).listFiles());
        if (other_volumes == null) {
          other_volumes = new File_B[1];
          other_volumes[0] = new File_B(new File_U("/"));
        } 
        items = new File_B[other_volumes.length];
        for (int x = 0; x < other_volumes.length; x++)
          items[x] = other_volumes[x]; 
      } catch (Exception exception) {}
    } else if (path.equals("/") && Common.machine_is_windows()) {
      items = Common.convert_files_to_files_both(File_U.listRoots());
    } else {
      items = Common.convert_files_to_files_both((new File_U(path)).listFiles());
    } 
    return items;
  }
  
  public static Properties getLog(Properties request, String site) throws IOException {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
        return (Properties)p.get("data");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return null;
      } 
    } 
    if (site.indexOf("(CONNECT)") < 0 && !request.getProperty("log_file", "").toUpperCase().endsWith(".LOG") && !request.getProperty("log_file", "").equals(""))
      return null; 
    return LoggingProviderDisk.getLogSegmentStatic(Long.parseLong(request.getProperty("segment_start", "0")), Long.parseLong(request.getProperty("segment_len", "32768")), request.getProperty("log_file", ""));
  }
  
  static String buildXML(Object o, String key, String status) {
    String xml = "";
    if (o instanceof String) {
      status = o.toString();
      o = null;
    } 
    try {
      if (o != null) {
        xml = Common.getXMLString(o, key, null);
        if (xml.startsWith("<?"))
          xml = xml.substring(xml.indexOf("?>") + 2).trim(); 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    String response_type = "";
    if (o == null || o instanceof String)
      response_type = "text"; 
    if (o instanceof Properties) {
      response_type = "properties";
    } else if (o instanceof Vector) {
      response_type = "vector";
    } 
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response_status>" + status + "</response_status><response_type>" + response_type + "</response_type><response_data>" + xml + "</response_data></result>";
    return response;
  }
  
  public static boolean checkRole(String command, String site) {
    return checkRole(command, site, "127.0.0.1");
  }
  
  public static boolean checkRole(String command, String site, String user_ip) {
    boolean allowed = false;
    String[] admin_ips = ("127.0.0.1," + ServerStatus.SG("admin_ips")).split(",");
    for (int x = 0; x < admin_ips.length && !allowed; x++) {
      if (!admin_ips[x].trim().equals("") && 
        Common.do_search(admin_ips[x].trim(), user_ip, false, 0))
        allowed = true; 
    } 
    if (!allowed)
      return false; 
    if (site.indexOf("(CONNECT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("getServerItem") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_VIEW)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(JOB_VIEW)") >= 0 || site.indexOf("(JOB_EDIT)") >= 0 || site.indexOf("(JOB_RUN)") >= 0 || site.indexOf("(SHARE_VIEW)") >= 0 || site.indexOf("(PREF_VIEW)") >= 0 || site.indexOf("(SERVER_VIEW)") >= 0 || site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(UPDATE_RUN)") >= 0 || site.indexOf("(REPORT_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getJob") && (site.indexOf("(JOB_EDIT)") >= 0 || site.indexOf("(JOB_VIEW)") >= 0 || site.indexOf("(JOB_RUN)") >= 0 || site.indexOf("(JOB_LIST)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("renameJob") && site.indexOf("(JOB_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("removeJob") && site.indexOf("(JOB_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("changeJobStatus") && (site.indexOf("(JOB_EDIT)") >= 0 || site.indexOf("(JOB_RUN)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("addJob") && site.indexOf("(JOB_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("getStatHistory") && site.indexOf("(SERVER_VIEW)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("getJobsSummary") && (site.indexOf("(JOB_EDIT)") >= 0 || site.indexOf("(JOB_VIEW)") >= 0 || site.indexOf("(JOB_LIST_HISTORY)") >= 0 || site.indexOf("(JOB_MONITOR)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getJobInfo") && (site.indexOf("(JOB_EDIT)") >= 0 || site.indexOf("(JOB_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getJobInfo") && site.indexOf("(JOB_MONITOR)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("getSessionList") && site.indexOf("(SERVER_VIEW)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("getLog") && site.indexOf("(LOG_ACCESS)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("getDashboardItems") && (site.indexOf("(SERVER_VIEW)") >= 0 || site.indexOf("(SERVER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getServerInfoItems") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(SERVER_VIEW)") >= 0 || site.indexOf("(SERVER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getServerSettingItems") && (site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(SERVER_VIEW)") >= 0 || site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(USER_ADMIN)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("setServerItem") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("getUser") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(USER_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getUserVersions") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(USER_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getDeletedUsers") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(USER_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("setUserItem") && site.indexOf("(USER_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("refreshUser") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getUserXMLListing") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(USER_VIEW)") >= 0 || site.indexOf("(JOB_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getUserList") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(USER_VIEW)") >= 0 || site.indexOf("(REPORT_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getUserXML") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(USER_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("kick") && site.indexOf("(SERVER_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("ban") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getUserInfo") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(SERVER_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("startAllServers") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("stopAllServers") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("startServer") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("stopServer") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("restartServer") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("allStats") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("loginStats") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("uploadDownloadStats") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("transferStats") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("serverStats") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("dumpStack") && (site.indexOf("(SERVER_VIEW)") >= 0 || site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("system.gc") && (site.indexOf("(SERVER_VIEW)") >= 0 || site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("newFolder") && (site.indexOf("(PREF_EDIT)") >= 0 || site.indexOf("(USER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("renameItem") && (site.indexOf("(PREF_EDIT)") >= 0 || site.indexOf("(USER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("duplicateItem") && (site.indexOf("(PREF_EDIT)") >= 0 || site.indexOf("(USER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("deleteItem") && (site.indexOf("(PREF_EDIT)") >= 0 || site.indexOf("(USER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("updateNow") && site.indexOf("(UPDATE_RUN)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("checkForUpdate") && site.indexOf("(UPDATE_RUN)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("updateWebNow") && site.indexOf("(UPDATE_RUN)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("updateNowProgress") && site.indexOf("(UPDATE_RUN)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("cancelUpdateProgress") && site.indexOf("(UPDATE_RUN)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("pgpGenerateKeyPair") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("runReport") && (site.indexOf("(REPORT_RUN)") >= 0 || site.indexOf("(REPORT_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("testReportSchedule") && (site.indexOf("(REPORT_RUN)") >= 0 || site.indexOf("(REPORT_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("testJobSchedule") && (site.indexOf("(JOB_RUN)") >= 0 || site.indexOf("(JOB_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("testSMTP") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("testOTP") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("importUsers") && site.indexOf("(USER_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("sendPassEmail") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("sendEventEmail") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getTempAccounts") && (site.indexOf("(SHARE_EDIT)") >= 0 || site.indexOf("(SHARE_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("addTempAccount") && site.indexOf("(SHARE_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("removeTempAccount") && site.indexOf("(SHARE_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("getTempAccountFiles") && (site.indexOf("(SHARE_EDIT)") >= 0 || site.indexOf("(SHARE_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("removeTempAccountFile") && site.indexOf("(SHARE_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("addTempAccountFile") && site.indexOf("(SHARE_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("migrateUsersVFS") && site.indexOf("(USER_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("convertUsers") && site.indexOf("(USER_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("generateSSL") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("importReply") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("testKeystore") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("generateFileKey") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("listSSL") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("deleteSSL") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("renameSSL") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("addPrivateSSL") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("addPublicSSL") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("testDB") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("pluginMethodCall") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("convertXMLSQLUsers") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("registerCrushFTP") && (site.indexOf("(SERVER_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("setReportSchedules") && (site.indexOf("(REPORT_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("deleteReportSchedules") && (site.indexOf("(REPORT_EDIT)") >= 0 || site.indexOf("(PREF_EDIT)") >= 0))
      return true; 
    return false;
  }
  
  public static Object adminAction(Properties request, String site, String user_ip) {
    String status = "";
    try {
      if (!request.getProperty("instance", "").equals("") && checkRole("getServerItem", site, user_ip)) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
        Properties p = DMZServer.getResponse(id, 20);
        return p.get("data");
      } 
      Vector indexes = new Vector();
      String[] indexesStr = request.getProperty("index", "").split(",");
      for (int x = 0; x < indexesStr.length; x++)
        indexes.addElement(indexesStr[x].trim()); 
      Vector v = new Vector();
      v.addAll(ServerStatus.siVG("user_list"));
      if (request.getProperty("action", "").equals("kick") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        for (int i = v.size() - 1; i >= 0; i--) {
          Properties user_info = v.elementAt(i);
          for (int xx = 0; xx < indexes.size(); xx++) {
            if (user_info.getProperty("user_number").equals(indexes.elementAt(xx).toString())) {
              status = String.valueOf(status) + ServerStatus.thisObj.kick(user_info, true) + "\r\n";
              indexes.remove(xx);
              try {
                Properties info = new Properties();
                info.put("alert_type", "kick");
                info.put("alert_sub_type", "admin");
                info.put("alert_timeout", "0");
                info.put("alert_max", "0");
                info.put("alert_msg", user_info.getProperty("user_name"));
                ServerStatus.thisObj.runAlerts("security_alert", info, user_info, null);
              } catch (Exception e) {
                Log.log("BAN", 1, e);
              } 
              break;
            } 
          } 
        } 
      } else if ((request.getProperty("action", "").equals("ban") || request.getProperty("action", "").equals("temporaryBan")) && checkRole("ban", site, user_ip)) {
        v.addAll(ServerStatus.siVG("recent_user_list"));
        for (int i = v.size() - 1; i >= 0; i--) {
          Properties user_info = v.elementAt(i);
          for (int xx = 0; xx < indexes.size(); xx++) {
            if (user_info.getProperty("user_number").equals(indexes.elementAt(xx).toString())) {
              if (request.getProperty("action", "").equals("ban")) {
                status = String.valueOf(status) + ServerStatus.thisObj.ban(user_info, 0, "admin banned") + ",";
                try {
                  Properties info = new Properties();
                  info.put("alert_type", "ban");
                  info.put("alert_sub_type", "admin");
                  info.put("alert_timeout", "0");
                  info.put("alert_max", "0");
                  info.put("alert_msg", "permanent");
                  ServerStatus.thisObj.runAlerts("security_alert", info, user_info, null);
                } catch (Exception e) {
                  Log.log("BAN", 1, e);
                } 
              } else if (request.getProperty("action", "").equals("temporaryBan")) {
                status = String.valueOf(status) + ServerStatus.thisObj.ban(user_info, Integer.parseInt(request.getProperty("banTimeout")), "admin banned") + ",";
                try {
                  Properties info = new Properties();
                  info.put("alert_type", "ban");
                  info.put("alert_sub_type", "admin");
                  info.put("alert_timeout", (new StringBuffer(String.valueOf(request.getProperty("banTimeout")))).toString());
                  info.put("alert_max", "0");
                  info.put("alert_msg", "temporary");
                  ServerStatus.thisObj.runAlerts("security_alert", info, user_info, null);
                } catch (Exception e) {
                  Log.log("BAN", 1, e);
                } 
              } 
              status = String.valueOf(status) + ServerStatus.thisObj.kick(user_info, true) + ",";
              indexes.remove(xx);
              break;
            } 
          } 
        } 
      } else if (request.getProperty("action", "").equals("getUserInfo") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        v.addAll(ServerStatus.siVG("recent_user_list"));
        for (int i = v.size() - 1; i >= 0; i--) {
          Properties user_info = v.elementAt(i);
          for (int xx = 0; xx < indexes.size(); xx++) {
            if (user_info.getProperty("user_number").equals(indexes.elementAt(xx).toString())) {
              Properties user_info2 = (Properties)user_info.clone();
              return stripUser(user_info2);
            } 
          } 
        } 
      } else if (request.getProperty("action", "").equals("startAllServers") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        ServerStatus.thisObj.start_all_servers();
        status = "OK";
      } else if (request.getProperty("action", "").equals("stopAllServers") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        ServerStatus.thisObj.stop_all_servers();
        Thread.sleep(1000L);
        ServerStatus.thisObj.stop_all_servers();
        status = "OK";
      } else if (request.getProperty("action", "").equals("startServer") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        ServerStatus.thisObj.start_this_server(Integer.parseInt(request.getProperty("index")));
        status = "OK";
      } else if (request.getProperty("action", "").equals("stopServer") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        ServerStatus.thisObj.stop_this_server(Integer.parseInt(request.getProperty("index")));
        status = "OK";
      } else if (request.getProperty("action", "").equals("restartServer") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        int connected = ((GenericServer)ServerStatus.thisObj.main_servers.elementAt(Integer.parseInt(request.getProperty("index")))).connected_users;
        ServerStatus.thisObj.stop_this_server(Integer.parseInt(request.getProperty("index")));
        Thread.sleep(1000L);
        ServerStatus.thisObj.start_this_server(Integer.parseInt(request.getProperty("index")));
        ((GenericServer)ServerStatus.thisObj.main_servers.elementAt(Integer.parseInt(request.getProperty("index")))).connected_users = connected;
        status = "OK";
      } else if (request.getProperty("action", "").equals("allStats") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        ServerStatus.thisObj.reset_server_login_counts();
        ServerStatus.thisObj.reset_server_bytes_in_out();
        ServerStatus.thisObj.reset_upload_download_counter();
        status = "OK";
      } else if (request.getProperty("action", "").equals("loginStats") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        ServerStatus.thisObj.reset_server_login_counts();
        status = "OK";
      } else if (request.getProperty("action", "").equals("uploadDownloadStats") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        ServerStatus.thisObj.reset_upload_download_counter();
        status = "OK";
      } else if (request.getProperty("action", "").equals("transferStats") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        ServerStatus.thisObj.reset_server_bytes_in_out();
        status = "OK";
      } else if (request.getProperty("action", "").equals("serverStats") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        status = "OK";
      } else if (request.getProperty("action", "").equals("clearMaxTransferAmounts") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        ServerStatus.thisObj.statTools.clearMaxTransferAmounts(request);
        status = "OK";
      } else if (request.getProperty("action", "").equals("newFolder") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        status = newFolder(request, site, true);
        if (!status.equalsIgnoreCase("OK"))
          throw new Exception(status); 
      } else if (request.getProperty("action", "").equals("renameItem") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        status = renameItem(request, site, true);
        if (!status.equalsIgnoreCase("OK"))
          throw new Exception(status); 
      } else if (request.getProperty("action", "").equals("duplicateItem") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        status = duplicateItem(request, site, true);
        if (!status.equalsIgnoreCase("OK"))
          throw new Exception(status); 
      } else if (request.getProperty("action", "").equals("deleteItem") && checkRole(request.getProperty("action", ""), site, user_ip)) {
        status = deleteItem(request, site, true);
        if (!status.equalsIgnoreCase("OK"))
          throw new Exception(status); 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = e.toString();
    } 
    return status;
  }
  
  public static String updateNow(Properties request, String site) {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    String xml = "";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      ServerStatus.thisObj.do_auto_update_early(false);
      xml = "Success";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      xml = e.toString();
    } 
    response = String.valueOf(response) + "<result><response>" + xml + "</response></result>";
    return response;
  }
  
  public static String checkForUpdate(Properties request, String site) {
    ServerStatus.thisObj.doCheckForUpdate(true);
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    String xml = "";
    try {
      xml = (new StringBuffer(String.valueOf(ServerStatus.siBG("update_available")))).toString();
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      xml = e.toString();
    } 
    response = String.valueOf(response) + "<result><response>" + xml + "</response></result>";
    return response;
  }
  
  public static String updateWebNow(Properties request, String site) {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    String xml = "";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      ServerStatus.thisObj.do_auto_update_early(true);
      xml = "Success";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      xml = e.toString();
    } 
    response = String.valueOf(response) + "<result><response>" + xml + "</response></result>";
    return response;
  }
  
  public static String updateNowProgress(Properties request, String site) {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      response = String.valueOf(response) + "<currentStatus>" + ServerStatus.thisObj.updateHandler.updateCurrentStatus + "</currentStatus>";
      response = String.valueOf(response) + "<currentLoc>" + ServerStatus.thisObj.updateHandler.updateCurrentLoc + "</currentLoc>";
      response = String.valueOf(response) + "<maximumLoc>" + ServerStatus.thisObj.updateHandler.updateMaxSize + "</maximumLoc>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      response = String.valueOf(response) + e.toString();
    } 
    response = String.valueOf(response) + "</response></result>";
    return response;
  }
  
  public static String cancelUpdateProgress(Properties request, String site) {
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      ServerStatus.thisObj.updateHandler.cancel();
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      response = String.valueOf(response) + e.toString();
    } 
    response = String.valueOf(response) + "</response></result>";
    return response;
  }
  
  public static String dumpStack(Properties request, String site) {
    String response = "";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      response = String.valueOf(response) + e.toString();
    } 
    response = String.valueOf(response) + Common.dumpStack(String.valueOf(ServerStatus.version_info_str) + ServerStatus.sub_version_info_str);
    return response;
  }
  
  public static String dumpHeap(Properties request, String site) {
    String response = "";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      response = String.valueOf(response) + e.toString();
    } 
    response = String.valueOf(response) + (new HeapDumper()).dump();
    return response;
  }
  
  public static String pgpGenerateKeyPair(Properties request, String site) {
    String xml = "";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      String pass = VRL.vrlDecode(Common.url_decode(request.getProperty("pgpPrivateKeyPasswordGenerate")));
      Common.generateKeyPair(Common.url_decode(request.getProperty("pgpPivateKeyPathGenerate").replace('+', ' ')), Integer.parseInt(request.getProperty("pgpKeySizeGenerate")), Integer.parseInt(request.getProperty("pgpKeyDaysGenerate")), pass, Common.url_decode(request.getProperty("pgpCommonNameGenerate").replace('+', ' ')));
      xml = "Success";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      xml = e.toString();
    } 
    String response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \r\n";
    response = String.valueOf(response) + "<result><response>" + xml + "</response></result>";
    return response;
  }
  
  public static String runReport(Properties request, String site) {
    String xml = "";
    try {
      if (request.getProperty("reportName").equalsIgnoreCase("ExportUserPass") && site.toUpperCase().indexOf("(CONNECT)") < 0)
        throw new Exception("Access denied"); 
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      if (request.containsKey("report_token")) {
        String report_token = Common.dots(request.getProperty("report_token")).trim();
        if ((new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "SavedReports/" + report_token)).exists()) {
          RandomAccessFile in = new RandomAccessFile(new File_S(String.valueOf(System.getProperty("crushftp.prefs")) + "SavedReports/" + report_token), "r");
          byte[] b = new byte[0];
          try {
            b = new byte[(int)in.length()];
            in.readFully(b);
          } finally {
            in.close();
          } 
          xml = new String(b, "UTF8");
        } else {
          Thread.sleep(1000L);
          xml = "<html><body><h1>ERROR:No such report</h1></body></html>";
        } 
      } else {
        Object reportItem = ServerStatus.thisObj.rt.getReportItem(request.getProperty("reportName"), ServerStatus.server_settings);
        Properties params = request;
        Vector v = new Vector();
        if (request.containsKey("usernames")) {
          String[] usernames = request.getProperty("usernames").split(",");
          for (int x = 0; x < usernames.length; x++) {
            s = usernames[x].trim();
            if (!s.equals(""))
              v.addElement(s); 
          } 
          request.put("usernames", v);
        } 
        request.put("usernames", v);
        params.put("server_settings", ServerStatus.server_settings);
        Properties status = new Properties();
        xml = ServerStatus.thisObj.rt.writeReport("", "", status, params, reportItem, ServerStatus.server_settings, request.getProperty("export", "false").equals("true"), request.getProperty("reportName"), request);
        if (status.getProperty("report_empty", "true").equals("true")) {
          xml = status.remove("report_text").toString();
          xml = String.valueOf(xml) + "<hr/><center><h1>No data to report.</h1></center><hr/>";
        } 
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      xml = "<html><body><h1>" + e.toString() + "</h1></body></html>";
    } 
    return xml;
  }
  
  public static String handleInstance(Properties request, String site) throws Exception {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      Properties p = DMZServer.getResponse(id, 20);
      return p.get("data").toString();
    } 
    return null;
  }
  
  public static String testReportSchedule(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Vector schedules = ServerStatus.VG("reportSchedules");
      Properties params = schedules.elementAt(Integer.parseInt(request.getProperty("scheduleIndex")));
      Properties config = (Properties)params.get("config");
      config = (Properties)config.clone();
      config.put("server_settings", ServerStatus.server_settings);
      Properties status = new Properties();
      String dir = params.getProperty("reportFolder");
      if (!dir.endsWith("/"))
        dir = String.valueOf(dir) + "/"; 
      String filename = params.getProperty("reportFilename");
      filename = ServerStatus.thisObj.rt.replaceVars(filename, params, config);
      if (params.getProperty("reportOverwrite").equals("false") && (new File_S(String.valueOf(dir) + filename)).exists()) {
        response = String.valueOf(response) + "The report file already exists.";
      } else {
        if (config.get("usernames") == null)
          config.put("usernames", new Vector()); 
        config.put("export", params.getProperty("export", ""));
        ReportTools.skipEmail(config.getProperty("reportName"), config);
        ServerStatus.thisObj.rt.writeReport(filename, dir, status, config, ServerStatus.thisObj.rt.getReportItem(config.getProperty("reportName"), ServerStatus.server_settings), ServerStatus.server_settings, params.getProperty("export", "").equals("true"), config.getProperty("reportName"), params);
        ReportTools.unSkipEmail(config.getProperty("reportName"), config);
        response = String.valueOf(response) + LOC.G("Report written to") + ":" + dir + filename;
      } 
      response = String.valueOf(response) + "</response></commandResult>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static String testPGP(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      try {
        PGPLib pgp = new PGPLib();
        pgp.setUseExpiredKeys(true);
        ByteArrayOutputStream baos_key = new ByteArrayOutputStream();
        boolean pbe = false;
        String keyLocation = request.getProperty("publicKey");
        if (keyLocation.toLowerCase().startsWith("password:")) {
          pbe = true;
        } else {
          if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
            keyLocation = "FILE://" + keyLocation; 
          VRL key_vrl = new VRL(keyLocation);
          GenericClient c_key = Common.getClient(Common.getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
          Common.streamCopier(c_key.download(key_vrl.getPath(), 0L, -1L, true), baos_key, false, true, true);
          c_key.logout();
        } 
        ByteArrayInputStream bytesInKey = new ByteArrayInputStream(baos_key.toByteArray());
        pgp.setCompression("UNCOMPRESSED");
        String source_data = "This is a test.";
        ByteArrayOutputStream baos_encrypted = new ByteArrayOutputStream();
        ByteArrayInputStream bais_source = new ByteArrayInputStream(source_data.getBytes());
        if (pbe) {
          pgp.encryptStreamPBE(bais_source, "test_data", Common.encryptDecrypt(keyLocation.substring(keyLocation.indexOf(":") + 1), false), baos_encrypted, false, false);
        } else {
          pgp.encryptStream(bais_source, "test_data", bytesInKey, baos_encrypted, false, false);
        } 
        bytesInKey.close();
        String encrypted_data = Base64.encodeBytes(baos_encrypted.toByteArray());
        baos_key = new ByteArrayOutputStream();
        pbe = false;
        keyLocation = request.getProperty("privateKey");
        bais_source = new ByteArrayInputStream(baos_encrypted.toByteArray());
        ByteArrayOutputStream baos_decrypted = new ByteArrayOutputStream();
        if (keyLocation.toLowerCase().startsWith("password:")) {
          pbe = true;
        } else {
          if (keyLocation.indexOf(":") < 0 || keyLocation.indexOf(":") < 3)
            keyLocation = "FILE://" + keyLocation; 
          VRL key_vrl = new VRL(keyLocation);
          GenericClient c_key = Common.getClient(Common.getBaseUrl(key_vrl.toString()), "CrushFTP", new Vector());
          Common.streamCopier(c_key.download(key_vrl.getPath(), 0L, -1L, true), baos_key, false, true, true);
          c_key.logout();
        } 
        ByteArrayInputStream bytesIn1 = new ByteArrayInputStream(baos_key.toByteArray());
        ByteArrayInputStream bytesIn2 = new ByteArrayInputStream(baos_key.toByteArray());
        pgp.setCompression("UNCOMPRESSED");
        if (pbe) {
          pgp.decryptStreamPBE(bais_source, Common.encryptDecrypt(keyLocation.substring(keyLocation.indexOf(":") + 1), false), baos_decrypted);
        } else if ((new KeyStore()).importPrivateKey(bytesIn1)[0].checkPassword(request.getProperty("privateKeyPass"))) {
          pgp.decryptStream(bais_source, bytesIn2, request.getProperty("privateKeyPass"), baos_decrypted);
        } else {
          pgp.decryptStream(bais_source, bytesIn2, Common.encryptDecrypt(request.getProperty("privateKeyPass"), false), baos_decrypted);
        } 
        bytesIn1.close();
        bytesIn2.close();
        String decrypted_data = new String(baos_decrypted.toByteArray());
        if (!decrypted_data.trim().equals(source_data.trim()))
          throw new Exception("Source and decrypted data are not equal!!!  " + source_data + " vs. " + decrypted_data); 
        response = String.valueOf(response) + "SUCCESS:\r\n<br/>";
        response = String.valueOf(response) + "Source data:" + source_data + "\r\n<br/>";
        response = String.valueOf(response) + "Encrypted data:<br/>";
        response = String.valueOf(response) + encrypted_data + "<br/>";
        response = String.valueOf(response) + "\r\n<br/>";
        response = String.valueOf(response) + "Decrypted value:\r\n<br/>";
        response = String.valueOf(response) + decrypted_data;
      } catch (Exception e) {
        response = String.valueOf(response) + "ERROR:\r\n<br/>" + e;
      } 
      response = String.valueOf(response) + "</response></commandResult>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static Vector runningSchedules = new Vector();
  
  public static Object changeJobStatus(Properties request, String site) {
    if (request.getProperty("status", "").equals("restart")) {
      Vector vv = new Vector();
      File_S f1 = new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + Common.dots(request.getProperty("scheduleName", "")));
      checkJobFolder(site, request, vv, f1);
      if (vv.size() > 1)
        return "FAILURE:Job ID not found! " + vv.size(); 
      Properties tracker = vv.remove(0);
      if (!tracker.getProperty("status", "").equalsIgnoreCase("completed-errors") && !tracker.getProperty("status", "").equalsIgnoreCase("cancelled"))
        return "FAILURE:Only jobs in a cancelled or completed-errors status can be restarted. (" + tracker.getProperty("status") + ")"; 
      tracker.put("status", "running");
      Vector active_items = (Vector)tracker.get("active_items");
      if (active_items != null) {
        Properties task_item = active_items.elementAt(active_items.size() - 1);
        task_item.put("status", "active");
        task_item.remove("error");
      } 
      try {
        Common.writeXMLObject(tracker.getProperty("job_history_obj_path"), tracker, "tracker");
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      f1 = new File_S(tracker.getProperty("job_history_obj_path"));
      File_S f2 = new File_S(String.valueOf(f1.getParent()) + "/inprogress/" + tracker.getProperty("id") + ".XML");
      f1.renameTo(f2);
      startJob(new File_S(f1.getParent()), true, new StringBuffer(tracker.getProperty("id")), null);
      return "SUCCESS:" + request.getProperty("scheduleName", "") + " restarted.";
    } 
    Vector jobs = ServerStatus.siVG("running_tasks");
    for (int x = jobs.size() - 1; x >= 0; x--) {
      Properties tracker = jobs.elementAt(x);
      if (request.getProperty("job_id", "").equalsIgnoreCase(tracker.getProperty("id"))) {
        if (tracker.getProperty("status").equalsIgnoreCase("running") || tracker.getProperty("status").toLowerCase().indexOf("paused") >= 0) {
          tracker.put("status", request.getProperty("status"));
          if (tracker.getProperty("restore_job", "false").equals("true") && request.getProperty("status").equalsIgnoreCase("running")) {
            request.put("restore_job", "true");
            testJobSchedule(request, site);
          } 
          tracker.remove("restore_job");
        } 
        return "SUCCESS:" + tracker.getProperty("status");
      } 
    } 
    return "FAILURE:Job not found.";
  }
  
  public static String testJobSchedule(Properties request, String site) {
    if (ServerStatus.siIG("enterprise_level") <= 0)
      return "ERROR:Enterprise License only feature."; 
    String response = "";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Vector jobs = JobScheduler.getJobList(false);
      File_S job = null;
      for (int x = 0; job == null && x < jobs.size(); x++) {
        File_S f = jobs.elementAt(x);
        if (jobName(f).equalsIgnoreCase(request.getProperty("scheduleName")))
          job = f; 
      } 
      StringBuffer jobid = new StringBuffer();
      response = startJob(job, request.getProperty("restore_job", "").equals("true"), jobid, request);
      response = "<commandResult><response>" + response + "</response><jobid>" + jobid.toString() + "</jobid></commandResult>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      response = "ERROR:" + e;
    } 
    return response;
  }
  
  static String startJob(File_S job, boolean restore, StringBuffer jobid, Properties request) {
    String response = "";
    Properties params = (Properties)Common.readXMLObject(String.valueOf(job.getPath()) + "/job.XML");
    params.put("scheduleName", jobName(job));
    boolean ok = true;
    if (params.getProperty("scheduleName", "").toUpperCase().endsWith("_SINGLE") || params.getProperty("single", "").equalsIgnoreCase("true"))
      ok = !(!restore && JobScheduler.jobRunning(params.getProperty("scheduleName", ""))); 
    if (ok) {
      response = String.valueOf(response) + "Job Started";
    } else {
      response = String.valueOf(response) + "Job is already running, stop existing job first.";
    } 
    Log.log("SERVER", 1, "Job Schedule:" + params.getProperty("scheduleName") + ":" + response);
    if (ok) {
      if (jobid.length() == 0)
        jobid.append(Common.makeBoundary(20)); 
      params.put("new_job_id_run", jobid.toString());
      try {
        Runnable r = new Runnable(restore, params, request) {
            private final boolean val$restore;
            
            private final Properties val$params;
            
            private final Properties val$request;
            
            public void run() {
              Properties event = new Properties();
              if (this.val$restore)
                event.put("restore_job", "true"); 
              event.putAll(this.val$params);
              event.put("enabled", "true");
              event.put("event_plugin_list", this.val$params.getProperty("plugin", this.val$params.getProperty("event_plugin_list")));
              event.put("name", "ScheduledPluginEvent:" + this.val$params.getProperty("scheduleName"));
              try {
                AdminControls.runningSchedules.addElement(this.val$params.getProperty("scheduleName"));
                Properties info = (this.val$request == null) ? new Properties() : this.val$request;
                info = ServerStatus.thisObj.events6.doEventPlugin(info, event, null, new Vector());
              } finally {
                AdminControls.runningSchedules.remove(this.val$params.getProperty("scheduleName"));
              } 
            }
          };
        if (request == null || request.getProperty("async", "true").equals("true")) {
          Worker.startWorker(r);
        } else {
          request.put("return_tracker", "true");
          r.run();
          Properties tracker = (Properties)((Properties)request.get("tracker")).clone();
          tracker.remove("settings");
          tracker.remove("active_items");
          tracker.remove("connections");
          if (request.getProperty("response_type", "simple").equals("simple")) {
            response = tracker.getProperty("status");
          } else if (request.getProperty("response_type", "simple").equals("log")) {
            response = getJobLog(tracker);
          } else {
            if (request.getProperty("response_type", "simple").equals("all"))
              tracker.put("full_log", getJobLog(tracker)); 
            response = Common.getXMLString(tracker, "job", "");
            response = response.substring(response.indexOf("<job")).trim();
          } 
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
    } 
    return response;
  }
  
  public static String getJobLog(Properties tracker) {
    String full_log = "Error reading task log...";
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      long len = (new File_U(tracker.getProperty("log_file"), System.getProperty("crushftp.server.root", ""))).length();
      FileInputStream in = new FileInputStream(new File_U(tracker.getProperty("log_file"), System.getProperty("crushftp.server.root", "")));
      long skip = 0L;
      if (len > 1048576L)
        skip = len - 1048576L; 
      in.skip(skip);
      Common.copyStreams(in, baos, true, true);
      full_log = new String(baos.toByteArray(), "UTF8");
    } catch (IOException e) {
      full_log = String.valueOf(full_log) + "\r\n" + e + "\r\n" + tracker.getProperty("log_file");
      Log.log("SERVER", 0, e);
    } 
    return full_log;
  }
  
  public static String testSMTP(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Properties p = request;
      String results = Common.send_mail(ServerStatus.SG("discovered_ip"), p.getProperty("to", ""), p.getProperty("cc", ""), p.getProperty("bcc", ""), p.getProperty("from", ""), p.getProperty("subject", ""), p.getProperty("body", ""), p.getProperty("server", ""), p.getProperty("user", ""), p.getProperty("pass", ""), p.getProperty("ssl", "").equals("true"), p.getProperty("html", "").equals("true"), (File_B[])null);
      response = String.valueOf(response) + Common.url_encode(results);
      response = String.valueOf(response) + "</response></commandResult>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static String testOTP(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Common.send_otp_for_auth_sms(request.getProperty("otp_to", ""), "Test sms!");
      response = String.valueOf(response) + "Success!";
    } catch (Exception e) {
      response = String.valueOf(response) + "ERROR: " + e.getMessage();
      Log.log("HTTP_SERVER", 1, e);
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String sendEventEmail(Properties request, String site) {
    String response = "<commandResult><response>";
    Common.urlDecodePost(request);
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Properties fake_event = new Properties();
      fake_event.put("name", "fake_event");
      fake_event.put("to", Common.replace_str(Common.replace_str(request.getProperty("email_to"), "&gt;", ">"), "&lt;", "<"));
      fake_event.put("from", Common.replace_str(Common.replace_str(request.getProperty("email_from"), "&gt;", ">"), "&lt;", "<"));
      fake_event.put("cc", Common.replace_str(Common.replace_str(request.getProperty("email_cc"), "&gt;", ">"), "&lt;", "<"));
      fake_event.put("bcc", Common.replace_str(Common.replace_str(request.getProperty("email_bcc"), "&gt;", ">"), "&lt;", "<"));
      fake_event.put("body", request.getProperty("email_body"));
      fake_event.put("subject", request.getProperty("email_subject"));
      fake_event.put("event_user_action_list", "(disconnect)");
      fake_event.put("event_now_cb", "true");
      fake_event.put("event_now_cb", "true");
      Properties fake_user = UserTools.ut.getUser(request.getProperty("serverGroup"), request.getProperty("username"), true);
      Properties fake_user_info = new Properties();
      fake_user_info.put("user_name", request.getProperty("username"));
      fake_user_info.put("username", request.getProperty("username"));
      Vector fake_items = new Vector();
      Properties p = new Properties();
      p.put("name", "test item name");
      p.put("the_file_name", "test item name");
      p.put("url", "file://test/path/to/folder/test item name");
      p.put("type", "FILE");
      p.put("size", "500");
      p.put("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      p.put("sizeFormatted", Common.format_bytes2(p.getProperty("size")));
      p.put("privs", "(read)(write)(delete)(view)(resume)");
      p.put("path", "/fake_uploads/");
      p.put("the_file_path", "/fake_uploads/");
      p.put("num_items", "1");
      p.put("owner", "user");
      p.put("group", "group");
      p.put("month", "1");
      p.put("day", "1");
      p.put("time_or_year", "1970");
      p.put("permissions", "drwxrwxrwx");
      p.put("root_dir", "/");
      p.put("protocol", "file");
      p.put("link", "false");
      p.put("the_file_size", "500");
      p.put("the_command", "STOR");
      p.put("the_file_speed", "500K/sec");
      p.put("the_file_status", "");
      p.put("the_file_start", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      p.put("the_file_end", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      p.put("the_file_resume_loc", "0");
      p.put("the_file_md5", Common.getMD5(new ByteArrayInputStream("a".getBytes())));
      p.put("the_file_resume_loc", "0");
      p.put("the_file_resume_loc", "0");
      fake_items.addElement(p);
      response = String.valueOf(response) + ServerStatus.thisObj.events6.doEventEmail(fake_event, fake_user, fake_user_info, fake_items, new Vector());
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String migrateUsersVFS(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      response = String.valueOf(response) + ServerStatus.thisObj.common_code.migrateUsersVFS(request.getProperty("serverGroup"), request.getProperty("findPath"), request.getProperty("replacePath"));
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String convertUsers(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Vector users = new Vector();
      String[] usersStr = request.getProperty("users").split(";");
      for (int x = 0; x < usersStr.length; x++) {
        if (!usersStr[x].trim().equals(""))
          users.addElement(usersStr[x].trim()); 
      } 
      response = String.valueOf(response) + UserTools.convertUsers(request.getProperty("allUsers").equalsIgnoreCase("true"), users, request.getProperty("serverGroup"), request.getProperty("username", ""));
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String testDB(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Connection conn = null;
      if (!request.getProperty("db_driver").equalsIgnoreCase("org.apache.derby.jdbc.EmbeddedDriver")) {
        String[] db_drv_files = request.getProperty("db_driver_file").split(";");
        URL[] urls = new URL[db_drv_files.length];
        for (int x = 0; x < db_drv_files.length; x++)
          urls[x] = (new File_S(db_drv_files[x])).toURI().toURL(); 
        ClassLoader cl = new URLClassLoader(urls);
        Class drvCls = Class.forName(request.getProperty("db_driver"), true, cl);
        Driver driver = (Driver)drvCls.newInstance();
        Properties props = new Properties();
        props.setProperty("user", request.getProperty("db_user"));
        props.setProperty("password", ServerStatus.thisObj.common_code.decode_pass(request.getProperty("db_pass")));
        conn = driver.connect(request.getProperty("db_url"), props);
      } else {
        try {
          conn = ServerStatus.thisObj.statTools.getConnection();
        } catch (Throwable e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
      } 
      conn.close();
      response = String.valueOf(response) + "Success";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String testQuery(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Connection conn = null;
      if (!request.getProperty("db_driver").equalsIgnoreCase("org.apache.derby.jdbc.EmbeddedDriver")) {
        String[] db_drv_files = request.getProperty("db_driver_file").split(";");
        URL[] urls = new URL[db_drv_files.length];
        for (int x = 0; x < db_drv_files.length; x++)
          urls[x] = (new File_S(db_drv_files[x])).toURI().toURL(); 
        ClassLoader cl = new URLClassLoader(urls);
        Class drvCls = Class.forName(request.getProperty("db_driver"), true, cl);
        Driver driver = (Driver)drvCls.newInstance();
        Properties props = new Properties();
        props.setProperty("user", request.getProperty("db_user"));
        props.setProperty("password", ServerStatus.thisObj.common_code.decode_pass(request.getProperty("db_pass")));
        conn = driver.connect(request.getProperty("db_url"), props);
      } else if (request.getProperty("db_url", "").indexOf("statsDB") >= 0) {
        try {
          conn = ServerStatus.thisObj.statTools.getConnection();
        } catch (Throwable e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
      } else if (request.getProperty("db_url", "").indexOf("syncDB") >= 0) {
        try {
          conn = SyncTools.dbt.getConnection();
        } catch (Throwable e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
      } else if (request.getProperty("db_url", "").indexOf("searchDB") >= 0) {
        try {
          conn = ServerStatus.thisObj.searchTools.getConnection();
        } catch (Throwable e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
      } 
      Statement st = conn.createStatement();
      try {
        String sql = request.getProperty("sql");
        if (sql.startsWith("IMPORTCSV:")) {
          String table = Common.last(sql.substring(sql.indexOf(":") + 1).trim());
          BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File_S(table))));
          int count = 0;
          try {
            String line = "";
            while ((line = br.readLine()) != null) {
              String sql2 = "";
              try {
                String[] cols = line.split(";");
                sql2 = "insert into " + table.substring(0, table.indexOf(".")) + " values (";
                for (int x = 0; x < cols.length; x++) {
                  if (x > 0)
                    sql2 = String.valueOf(sql2) + ","; 
                  sql2 = String.valueOf(sql2) + cols[x];
                } 
                sql2 = String.valueOf(sql2) + ");";
                st.executeUpdate(sql2);
                count++;
              } catch (Exception e) {
                Log.log("SERVER", 0, e);
                Log.log("SERVER", 0, (new StringBuffer(String.valueOf(count))).toString());
                Log.log("SERVER", 0, sql2);
              } 
            } 
          } finally {
            br.close();
          } 
          response = String.valueOf(response) + count + " rows inserted.";
        } else if (sql.startsWith("EXPORTCSV:")) {
          String table = Common.last(sql.substring(sql.indexOf(":") + 1).trim());
          if (!table.toUpperCase().endsWith(".CSV"))
            throw new Exception("Must be a CSV file."); 
          SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
          RandomAccessFile raf = new RandomAccessFile(new File_S(table), "rw");
          raf.setLength(0L);
          int count = 0;
          try {
            ResultSet rs = st.executeQuery("select * from " + table.substring(0, table.indexOf(".")));
            while (rs.next()) {
              int columnCount = rs.getMetaData().getColumnCount();
              String line = "";
              for (int x = 1; x <= columnCount; x++) {
                String val = rs.getString(x);
                if (val == null)
                  val = ""; 
                if (x > 1)
                  line = String.valueOf(line) + ","; 
                if (rs.getMetaData().getColumnTypeName(x).equalsIgnoreCase("TIMESTAMP")) {
                  try {
                    line = String.valueOf(line) + "\"" + sdf.format(new Date(rs.getTimestamp(x).getTime())) + "\"";
                  } catch (Exception exception) {}
                } else if (rs.getMetaData().getColumnTypeName(x).equalsIgnoreCase("DOUBLE")) {
                  try {
                    line = String.valueOf(line) + "\"" + rs.getLong(x) + "\"";
                  } catch (Exception exception) {}
                } else {
                  try {
                    line = String.valueOf(line) + "\"" + rs.getString(x) + "\"";
                  } catch (Exception exception) {}
                } 
              } 
              line = String.valueOf(line) + "\r\n";
              raf.write(line.getBytes("UTF8"));
              line = "";
              count++;
            } 
          } finally {
            raf.close();
          } 
          response = String.valueOf(response) + count + " rows exported.";
        } else {
          boolean update = false;
          if (sql.toUpperCase().indexOf("USE ") >= 0)
            throw new Exception("Invalid SQL statement."); 
          if (!sql.toUpperCase().startsWith("SELECT"))
            update = true; 
          if (update) {
            response = String.valueOf(response) + st.executeUpdate(sql) + " rows updated.";
          } else {
            ResultSet rs = st.executeQuery(sql);
            Vector v = loadTable(rs, Integer.parseInt(request.getProperty("sql_limit")));
            s = Common.getXMLString(v, "SQL", null).trim();
            response = String.valueOf(response) + s.substring(s.indexOf("<SQL"));
          } 
        } 
      } finally {
        st.close();
        conn.close();
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  static Vector loadTable(ResultSet rs, int limit) {
    Vector v = new Vector();
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
      while (rs.next()) {
        Properties saver = new Properties();
        try {
          int columnCount = rs.getMetaData().getColumnCount();
          for (int x = 1; x <= columnCount; x++) {
            String key = rs.getMetaData().getColumnName(x);
            if (key.toUpperCase().startsWith("ORACLE_"))
              key = key.substring("ORACLE_".length()); 
            if (key.toUpperCase().startsWith("SQL_FIELD_"))
              key = key.substring("SQL_FIELD_".length()); 
            String val = rs.getString(x);
            if (val == null)
              val = ""; 
            if (rs.getMetaData().getColumnTypeName(x).equalsIgnoreCase("TIMESTAMP")) {
              try {
                saver.put(key, sdf.format(new Date(rs.getTimestamp(x).getTime())));
              } catch (Exception exception) {}
            } else if (rs.getMetaData().getColumnTypeName(x).equalsIgnoreCase("DOUBLE")) {
              try {
                saver.put(key, (new StringBuffer(String.valueOf(rs.getLong(x)))).toString());
              } catch (Exception exception) {}
            } else {
              try {
                saver.put(key, (new StringBuffer(String.valueOf(rs.getString(x)))).toString());
              } catch (Exception exception) {}
            } 
          } 
        } catch (Throwable ee) {
          Log.log("SERVER", 0, ee);
        } 
        v.addElement(saver);
        if (v.size() >= limit)
          break; 
      } 
      rs.close();
    } catch (Throwable e) {
      Log.log("SERVER", 0, e);
    } finally {
      try {
        rs.close();
      } catch (SQLException sQLException) {}
    } 
    return v;
  }
  
  public static String pluginMethodCall(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Object parent = Common.getPlugin(request.getProperty("pluginName"), null, request.getProperty("pluginSubItem", ""));
      if (parent == null && request.getProperty("pluginSubItem", "").equals(""))
        parent = Common.getPlugin(request.getProperty("pluginName"), null, "false"); 
      Method method = parent.getClass().getMethod(request.getProperty("method", "testSettings"), new Class[] { (new Properties()).getClass() });
      response = String.valueOf(response) + method.invoke(parent, new Object[] { request }).toString();
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String convertXMLSQLUsers(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      UserTools.convertUsersSQLXML(request.getProperty("fromMode"), request.getProperty("toMode"), request.getProperty("serverGroup"));
      response = String.valueOf(response) + "Success.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String registerCrushFTP(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String instance = request.getProperty("instance", "");
      String s = handleInstance(request, site);
      if (s != null) {
        String id2 = Common.makeBoundary();
        DMZServer.sendCommand(instance, new Properties(), "GET:SERVER_SETTINGS", id2);
        Properties p = DMZServer.getResponse(id2, 10);
        Common.write_server_settings((Properties)p.get("data"), instance);
        return s;
      } 
      String registration_name = Common.url_encode_all(request.getProperty("registration_name").toUpperCase().trim());
      String registration_email = request.getProperty("registration_email").toUpperCase().trim();
      String registration_code = request.getProperty("registration_code").trim();
      if (ServerStatus.thisObj.common_code.register(registration_name, registration_email, registration_code)) {
        String v = ServerStatus.thisObj.common_code.getRegistrationAccess("V", registration_code);
        if (v != null && (v.equals("4") || v.equals("5") || v.equals("6") || v.equals("7"))) {
          response = String.valueOf(response) + "CrushFTP " + v + " needs an upgrade license for CrushFTP 8.  http://www.crushftp.com/pricing.html";
        } else {
          ServerStatus.server_settings.put("registration_name", registration_name);
          ServerStatus.server_settings.put("registration_email", registration_email);
          ServerStatus.server_settings.put("registration_code", registration_code);
          ServerStatus.put_in("registration_name", registration_name);
          ServerStatus.put_in("registration_email", registration_email);
          ServerStatus.put_in("registration_code", registration_code);
          ServerStatus.server_settings.put("max_max_users", ServerStatus.thisObj.common_code.getRegistrationAccess("MAX", registration_code));
          ServerStatus.server_settings.put("max_users", ServerStatus.thisObj.common_code.getRegistrationAccess("MAX", registration_code));
          response = String.valueOf(response) + "Registration Information Accepted";
        } 
        ServerStatus.thisObj.prefsProvider.check_code();
        ServerStatus.thisObj.save_server_settings(false);
      } else {
        response = String.valueOf(response) + LOC.G("Invalid Name, Email or Code!");
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString());
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String restorePrefs(Properties request, String site) throws Exception {
    String backup_id = request.getProperty("backup_id", "");
    if (backup_id == null || backup_id.equals("")) {
      Vector v = new Vector();
      for (int index = 0; index < 100; index++) {
        File f = new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/prefs" + index + ".XML");
        if (f.exists() || f.length() > 0L)
          v.addElement(String.valueOf(index) + ":" + f.lastModified()); 
      } 
      return Common.getXMLString(v, "prefs", null);
    } 
    ServerStatus.thisObj.prefsProvider.savePrefs(ServerStatus.thisObj.prefsProvider.getBackupPrefs(backup_id), null);
    ServerStatus.thisObj.server_info.put("currentFileDate", "0");
    Thread.sleep(3000L);
    return "<commandResult><response>SUCCESS</response></commandResult>";
  }
  
  public static String importUsers(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      String the_dir = request.getProperty("the_dir");
      if (request.getProperty("user_type").equals("Import CrushFTP3 Users...")) {
        ServerStatus.thisObj.common_code.ConvertCrushFTP3Users(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/", ServerStatus.SG("password_encryption"), "");
      } else if (request.getProperty("user_type").equals("Import Folders As Users...")) {
        ServerStatus.thisObj.common_code.ConvertFolderUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import Mac OS X Users...")) {
        ServerStatus.thisObj.common_code.ConvertOSXUsers(String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import Serv-U Users...")) {
        ServerStatus.thisObj.common_code.ConvertServUUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import BulletProof Users...")) {
        ServerStatus.thisObj.common_code.ConvertBPFTPsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import Rumpus Users...")) {
        ServerStatus.thisObj.common_code.ConvertRumpusUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import ProFTPD Users...")) {
        ServerStatus.thisObj.common_code.ConvertPasswdUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/", "CRYPT3:");
      } else if (request.getProperty("user_type").equals("Import VSFTPD MD5Crypt Users...")) {
        ServerStatus.thisObj.common_code.ConvertPasswdUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/", "MD5CRYPT:");
      } else if (request.getProperty("user_type").equals("Import VSFTPD SHA512Crypt Users...")) {
        ServerStatus.thisObj.common_code.ConvertPasswdUsers(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/", "SHA512CRYPT:");
      } else if (request.getProperty("user_type").equals("Import ProFTPD Groups...")) {
        ServerStatus.thisObj.common_code.ConvertProFTPDGroups(request.getProperty("serverGroup"), the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import Tab Delimited Text...")) {
        ServerStatus.thisObj.common_code.convertTabDelimited(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import FileZilla Users...")) {
        ServerStatus.thisObj.common_code.convertFilezilla(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import WingFTP Users...")) {
        ServerStatus.thisObj.common_code.convertWingFTP(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/", "MD5:");
      } else if (request.getProperty("user_type").equals("Import Gene6 Users...")) {
        ServerStatus.thisObj.common_code.ConvertGene6Users(the_dir, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else if (request.getProperty("user_type").equals("Import CSV...")) {
        if (request.getProperty("preview", "false").equals("true")) {
          String snippet = "";
          BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File_S(the_dir))));
          String data = "";
          int lines = 0;
          while ((data = br.readLine()) != null && lines++ < 5)
            snippet = String.valueOf(snippet) + data + "\r\n"; 
          br.close();
          response = String.valueOf(response) + snippet;
          response = String.valueOf(response) + "</response></commandResult>";
          return response;
        } 
        response = String.valueOf(response) + Common.importCSV(request, String.valueOf(request.getProperty("serverGroup")) + "/");
      } else {
        throw new Exception("User import type not supported:" + request.getProperty("user_type"));
      } 
      response = String.valueOf(response) + "SUCCESS: Users imported.";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 0, e);
      response = String.valueOf(response) + e.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String sendPassEmail(Properties request, SessionCrush session, String site) {
    String subject = ServerStatus.SG("emailReminderSubjectText");
    String body = ServerStatus.SG("emailReminderBodyText");
    String from = ServerStatus.SG("smtp_from");
    String reply_to = "";
    String cc = "";
    String bcc = "";
    String templateName = Common.url_decode(request.getProperty("email_template", "").replace('+', ' ')).trim();
    Common.debug(2, "Looking up template info for admin pass email:" + templateName);
    Properties template = Common.get_email_template(templateName);
    if (template != null) {
      Common.debug(2, "Found template:" + template);
      subject = template.getProperty("emailSubject", "");
      body = template.getProperty("emailBody", "");
      from = template.getProperty("emailFrom", from);
      cc = template.getProperty("emailCC", "");
      reply_to = template.getProperty("emailReplyTo", "");
      bcc = template.getProperty("emailBCC", "");
    } 
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      String to = request.getProperty("user_email");
      if (from.equals(""))
        from = to; 
      from = Common.replace_str(Common.replace_str(from, "&gt;", ">"), "&lt;", "<");
      to = Common.replace_str(Common.replace_str(to, "&gt;", ">"), "&lt;", "<");
      cc = Common.replace_str(Common.replace_str(cc, "&gt;", ">"), "&lt;", "<");
      bcc = Common.replace_str(Common.replace_str(bcc, "&gt;", ">"), "&lt;", "<");
      if (ServerStatus.SG("smtp_server").equals("")) {
        response = String.valueOf(response) + LOC.G("This server is not configured to send email password reminders.") + "\r\n";
      } else if (!to.equals("")) {
        Properties userTemp = UserTools.ut.getUser(request.getProperty("serverGroup"), request.getProperty("user_name"), false);
        body = Common.replace_str(body, "%user_name%", request.getProperty("user_name"));
        body = Common.replace_str(body, "%username%", request.getProperty("user_name"));
        body = Common.replace_str(body, "%user_pass%", request.getProperty("user_pass"));
        body = Common.replace_str(body, "%user_password%", request.getProperty("user_pass"));
        body = Common.replace_str(body, "%user_email%", request.getProperty("user_email"));
        body = Common.replace_str(body, "%user_first_name%", request.getProperty("user_first_name"));
        body = Common.replace_str(body, "%user_last_name%", request.getProperty("user_last_name"));
        if (session != null)
          body = Common.replace_str(body, "%admin_user_name%", session.user.getProperty("username")); 
        if (session != null)
          body = Common.replace_str(body, "%admin_username%", session.user.getProperty("username")); 
        body = Common.replace_str(body, "{user_name}", request.getProperty("user_name"));
        body = Common.replace_str(body, "{username}", request.getProperty("user_name"));
        body = Common.replace_str(body, "{user_pass}", request.getProperty("user_pass"));
        body = Common.replace_str(body, "{user_password}", request.getProperty("user_pass"));
        body = Common.replace_str(body, "{user_email}", request.getProperty("user_email"));
        body = Common.replace_str(body, "{user_first_name}", request.getProperty("user_first_name"));
        body = Common.replace_str(body, "{user_last_name}", request.getProperty("user_last_name"));
        if (session != null)
          body = Common.replace_str(body, "{admin_user_name}", session.user.getProperty("username")); 
        if (session != null)
          body = Common.replace_str(body, "{admin_username}", session.user.getProperty("username")); 
        body = ServerStatus.change_vars_to_values_static(body, userTemp, new Properties(), session);
        subject = Common.replace_str(subject, "%user_name%", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "%username%", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "%user_pass%", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "%user_password%", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "%user_email%", request.getProperty("user_email"));
        subject = Common.replace_str(subject, "%user_first_name%", request.getProperty("user_first_name"));
        subject = Common.replace_str(subject, "%user_last_name%", request.getProperty("user_last_name"));
        if (session != null)
          subject = Common.replace_str(subject, "%admin_user_name%", session.user.getProperty("username")); 
        if (session != null)
          subject = Common.replace_str(subject, "%admin_username%", session.user.getProperty("username")); 
        subject = Common.replace_str(subject, "{user_name}", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "{username}", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "{user_pass}", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "{user_password}", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "{user_email}", request.getProperty("user_email"));
        subject = Common.replace_str(subject, "{user_first_name}", request.getProperty("user_first_name"));
        subject = Common.replace_str(subject, "{user_last_name}", request.getProperty("user_last_name"));
        if (session != null)
          subject = Common.replace_str(subject, "{admin_user_name}", session.user.getProperty("username")); 
        if (session != null)
          subject = Common.replace_str(subject, "{admin_username}", session.user.getProperty("username")); 
        subject = ServerStatus.change_vars_to_values_static(subject, userTemp, new Properties(), session);
        Properties email_info = new Properties();
        email_info.put("server", ServerStatus.SG("smtp_server"));
        email_info.put("user", ServerStatus.SG("smtp_user"));
        email_info.put("pass", ServerStatus.SG("smtp_pass"));
        email_info.put("ssl", ServerStatus.SG("smtp_ssl"));
        email_info.put("html", ServerStatus.SG("smtp_html"));
        email_info.put("from", from);
        email_info.put("reply_to", reply_to);
        email_info.put("to", to);
        email_info.put("cc", cc);
        email_info.put("bcc", bcc);
        email_info.put("subject", subject);
        email_info.put("body", body);
        ServerStatus.thisObj.sendEmail(email_info);
        response = String.valueOf(response) + email_info.getProperty("results", "") + "\r\n";
        if (response.toUpperCase().indexOf("ERROR") < 0)
          response = "<commandResult><response>" + LOC.G("An email was just sent to the email address associated with this user.") + "\r\n"; 
        Log.log("HTTP_SERVER", 0, String.valueOf(LOC.G("Password Emailed to user:")) + request.getProperty("user_name") + "  " + to + "   " + LOC.G("Email Result:") + response);
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String testKeystore(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      if (request.getProperty("dump_pass", "false").equals("true"))
        throw new Exception("\r\nkeystore:" + ServerStatus.thisObj.common_code.decode_pass(request.getProperty("keystorePass")) + "\r\n" + "key:" + ServerStatus.thisObj.common_code.decode_pass(request.getProperty("keyPass"))); 
      if (!request.getProperty("keystorePath").equals("builtin")) {
        RandomAccessFile testIn = new RandomAccessFile(new File_S(request.getProperty("keystorePath")), "r");
        if (testIn.length() == 0L)
          throw new IOException("Keystore file not found:" + request.getProperty("keystorePath")); 
        testIn.close();
      } 
      SSLServerSocket ss = (SSLServerSocket)ServerStatus.thisObj.common_code.getServerSocket(0, "127.0.0.1", request.getProperty("keystorePath"), request.getProperty("keystorePass"), request.getProperty("keyPass"), "", false, 10, false, true, null);
      SSLSocketFactory factory = (new Common()).getSSLContext("builtin", "builtin", "", "", "TLS", false, true).getSocketFactory();
      SSLSocket s1 = (SSLSocket)factory.createSocket(new Socket("127.0.0.1", ss.getLocalPort()), "127.0.0.1", ss.getLocalPort(), true);
      Common.configureSSLTLSSocket(s1, "TLSv1,TLSv1.1,TLSv1,TLSv1.2");
      SSLSocket s2 = (SSLSocket)ss.accept();
      ss.close();
      s1.setSoTimeout(1000);
      s2.setSoTimeout(1000);
      s2.setUseClientMode(false);
      s1.setUseClientMode(true);
      (new Thread(new Runnable(s1) {
            private final SSLSocket val$s1;
            
            public void run() {
              try {
                this.val$s1.startHandshake();
              } catch (Exception exception) {}
            }
          })).start();
      (new Thread(new Runnable(s2) {
            private final SSLSocket val$s2;
            
            public void run() {
              try {
                this.val$s2.startHandshake();
              } catch (Exception exception) {}
            }
          })).start();
      s1.getOutputStream().write("1".getBytes());
      s2.getOutputStream().write("1".getBytes());
      s1.close();
      s2.close();
      response = String.valueOf(response) + "Cert test successful.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "Cert test failed:";
      try {
        if (Cipher.getMaxAllowedKeyLength("AES") < Integer.MAX_VALUE)
          response = getCipherFix(response); 
      } catch (Exception exception) {}
      response = String.valueOf(response) + "\r\n\r\n" + Common.url_encode(ee.toString());
      Log.log("SERVER", 0, response);
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String getCipherFix(String response) {
    String help_url = "";
    if (System.getProperty("java.version").startsWith("1.6")) {
      help_url = "http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html";
    } else if (System.getProperty("java.version").startsWith("1.7")) {
      help_url = "http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html";
    } else if (System.getProperty("java.version").startsWith("1.8")) {
      help_url = "http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html";
    } 
    response = String.valueOf(response) + "Your java install needs to have the policy files installed allowing for strong cryptography.  \r\n\r\nPlease download them from here:\r\n" + help_url;
    try {
      response = String.valueOf(response) + "\r\n\r\nCopy them here:\r\n" + (new File(String.valueOf(System.getProperty("java.home")) + "/lib/security/")).getCanonicalPath();
      Desktop.getDesktop().open(new File(String.valueOf(System.getProperty("java.home")) + "/lib/security/"));
    } catch (IOException iOException) {}
    return response;
  }
  
  public static String generateSSL(Properties request, String site) {
    String response = "<commandResult><response>";
    String csr = "";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      csr = SSLKeyManager.buildNew(request.getProperty("key_alg"), Integer.parseInt(request.getProperty("key_size")), request.getProperty("sig_alg"), Integer.parseInt(request.getProperty("days")), request.getProperty("cn"), request.getProperty("ou"), request.getProperty("o"), request.getProperty("l"), request.getProperty("st"), request.getProperty("c"), request.getProperty("e"), request.getProperty("keystore_path"), Common.encryptDecrypt(request.getProperty("keystore_pass", request.getProperty("key_pass")), false), Common.encryptDecrypt(request.getProperty("keystore_pass", request.getProperty("key_pass")), false));
      response = String.valueOf(response) + "\r\n" + csr + "\r\n";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + request.getProperty("keystore_path") + " failed to be generated.") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String generateCSR(Properties request, String site) {
    String response = "<commandResult><response>";
    String csr = "";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      csr = SSLKeyManager.makeCSR(request.getProperty("keystore_path"), ServerStatus.thisObj.common_code.decode_pass(request.getProperty("keystore_pass", request.getProperty("key_pass"))), ServerStatus.thisObj.common_code.decode_pass(request.getProperty("keystore_pass", request.getProperty("key_pass"))));
      response = String.valueOf(response) + "\r\n" + csr + "\r\n";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + request.getProperty("keystore_path") + " failed to be generated.") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String importReply(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      String result = SSLKeyManager.importReply(request.getProperty("keystore_path"), Common.encryptDecrypt(request.getProperty("keystore_pass", request.getProperty("key_pass")), false), Common.encryptDecrypt(request.getProperty("keystore_pass", request.getProperty("key_pass")), false), request.getProperty("import_path"), request.getProperty("trusted_paths"));
      response = String.valueOf(response) + "\r\n" + result + "\r\n";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + request.getProperty("import_path") + " failed to be imported into " + request.getProperty("keystore_path") + ".") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String listSSL(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Vector v = SSLKeyManager.list(request.getProperty("keystore_path"), Common.encryptDecrypt(request.getProperty("keystore_pass"), false));
      s = Common.getXMLString(v, "SSL", null).trim();
      response = String.valueOf(response) + s.substring(s.indexOf("<SSL"));
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + request.getProperty("keystore_path") + " failed to be listed.") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String deleteSSL(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      boolean ok = SSLKeyManager.delete(request.getProperty("keystore_path"), Common.encryptDecrypt(request.getProperty("keystore_pass"), false), request.getProperty("alias"));
      response = String.valueOf(response) + "\r\n" + ok + "\r\n";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + request.getProperty("alias") + " failed to be deleted.") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String renameSSL(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      boolean ok = SSLKeyManager.rename(request.getProperty("keystore_path"), Common.encryptDecrypt(request.getProperty("keystore_pass"), false), request.getProperty("alias1"), request.getProperty("alias2"));
      response = String.valueOf(response) + "\r\n" + ok + "\r\n";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + request.getProperty("alias1") + " failed to be generated.") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String exportSSL(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      s = SSLKeyManager.export(request.getProperty("keystore_path"), Common.encryptDecrypt(request.getProperty("keystore_pass"), false), request.getProperty("alias"));
      response = String.valueOf(response) + "\r\n" + s + "\r\n";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + request.getProperty("alias") + " failed to be exported.") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String addPrivateSSL(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      SSLKeyManager.addPrivate(request.getProperty("keystore_path"), Common.encryptDecrypt(request.getProperty("keystore_pass"), false), request.getProperty("alias"), request.getProperty("key_path"), Common.encryptDecrypt(request.getProperty("key_pass"), false));
      response = String.valueOf(response) + "\r\nSUCCESS\r\n";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + request.getProperty("alias") + " failed to be generated.") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String addPublicSSL(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      SSLKeyManager.addPublic(request.getProperty("keystore_path"), Common.encryptDecrypt(request.getProperty("keystore_pass"), false), request.getProperty("alias"), request.getProperty("key_path"));
      response = String.valueOf(response) + "\r\nSUCCESS\r\n";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
      response = String.valueOf(response) + "\r\n" + Common.url_encode("ERROR:" + request.getProperty("alias") + " failed to be generated.") + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  static Properties tmp_telnet_sockets = new Properties();
  
  public static String telnetSocket(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Socket tmp = (Socket)tmp_telnet_sockets.get(request.getProperty("id", ""));
      try {
        if (request.getProperty("sub_command", "").equals("connect")) {
          tmp = new Socket();
          tmp.setSoTimeout(5000);
          tmp.connect(new InetSocketAddress(request.getProperty("host").trim(), Integer.parseInt(request.getProperty("port", "").trim())));
          String id = Common.makeBoundary(3);
          tmp.setSoTimeout(500);
          tmp_telnet_sockets.put(id, tmp);
          response = String.valueOf(response) + "<id>" + id + "</id>";
          response = String.valueOf(response) + "<data>Connected.\r\n</data>";
        } else if (tmp == null) {
          response = String.valueOf(response) + "<error>Not connected.\r\n</error>";
        } else if (request.getProperty("sub_command", "").equals("read")) {
          String result = "";
          byte[] b = new byte[16384];
          try {
            int bytesRead = tmp.getInputStream().read(b);
            if (bytesRead > 0) {
              result = new String(b, 0, bytesRead);
            } else if (bytesRead < 0) {
              tmp.close();
              tmp_telnet_sockets.remove(request.getProperty("id"));
              result = "Socket Closed.";
            } 
          } catch (SocketTimeoutException socketTimeoutException) {}
          response = String.valueOf(response) + "<data>" + Common.url_encode(result) + "</data>";
        } else if (request.getProperty("sub_command", "").equals("write")) {
          tmp.getOutputStream().write((String.valueOf(request.getProperty("data")) + "\r\n").getBytes());
          response = String.valueOf(response) + "<data></data>";
        } else if (request.getProperty("sub_command", "").equals("close")) {
          tmp.close();
          tmp_telnet_sockets.remove(request.getProperty("id"));
          response = String.valueOf(response) + "<data>Closed.\r\n</data>";
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 0, e);
        response = String.valueOf(response) + "<error>ERROR:" + Common.url_encode((String)e) + "</error>";
        if (tmp != null)
          tmp.close(); 
        tmp_telnet_sockets.remove(request.getProperty("id"));
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 0, ee);
      response = String.valueOf(response) + "Error:" + Common.url_encode(ee.toString()) + "\r\n";
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static Object stripUserList(Object o) {
    Vector v = (Vector)((Vector)o).clone();
    Vector vv = new Vector();
    for (int x = 0; x < v.size(); x++) {
      Object o2 = stripUser(v.elementAt(x));
      ((Properties)o2).remove("user_log");
      vv.addElement(o2);
    } 
    return vv;
  }
  
  public static Object stripUser(Object o) {
    Properties p2 = (Properties)((Properties)o).clone();
    p2.remove("stat");
    p2.remove("session");
    p2.remove("session_uploads");
    p2.remove("session_downloads");
    p2.remove("failed_commands");
    p2.remove("lastUploadStats");
    p2.remove("current_password");
    p2.remove("post_parameters");
    return p2;
  }
  
  public static VFS processVFSSubmission(Properties request, String username, String site, SessionCrush thisSession, boolean real_update, StringBuffer log_summary) throws Exception {
    Properties virtual = null;
    Properties virtual_orig = null;
    Properties permission0 = null;
    if (request.containsKey("permissions")) {
      permission0 = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(Common.replace_str(request.getProperty("permissions").replace('+', ' '), "%26", "&amp;")).getBytes("UTF8")));
    } else {
      VFS tempVFS = UserTools.ut.getVFS(request.getProperty("serverGroup"), username);
      permission0 = tempVFS.getPermission0();
    } 
    boolean remove = false;
    if (request.getProperty("data_action", "").startsWith("update_vfs")) {
      remove = request.getProperty("data_action").equals("update_vfs_remove");
      VFS tempVFS = UserTools.ut.getVFS(request.getProperty("serverGroup"), username);
      Properties permission_current = tempVFS.getPermission0();
      Enumeration keys = permission0.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (remove && !key.equals("/")) {
          permission_current.remove(key);
          continue;
        } 
        if (!key.equals("/"))
          permission_current.put(key, permission0.getProperty(key)); 
      } 
      permission0 = permission_current;
      virtual = tempVFS.homes.elementAt(0);
    } else {
      virtual = UserTools.generateEmptyVirtual();
    } 
    try {
      VFS tempVFS = UserTools.ut.getVFS(request.getProperty("serverGroup"), username);
      virtual_orig = tempVFS.homes.elementAt(0);
    } catch (Exception e) {
      virtual_orig = UserTools.generateEmptyVirtual();
    } 
    Vector permissions = new Vector();
    permissions.addElement(permission0);
    virtual.put("vfs_permissions_object", permissions);
    if (request.containsKey("vfs_items")) {
      Object o = Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(Common.replace_str(request.getProperty("vfs_items").replace('+', ' '), "%26", "&amp;")).getBytes("UTF8")));
      if (o instanceof Properties)
        o = null; 
      Vector vfs_items = (Vector)o;
      for (int x = 0; vfs_items != null && x < vfs_items.size(); x++) {
        Properties p = vfs_items.elementAt(x);
        Vector v = (Vector)p.get("vfs_item");
        if (!p.getProperty("name").equals("VFS") || !p.getProperty("path").equals("")) {
          Log.log("HTTP_SERVER", 2, (String)p);
          String path = Common.dots(String.valueOf(p.getProperty("path").substring(1)) + p.getProperty("name"));
          if (site.indexOf("(CONNECT)") < 0 && site.indexOf("(USER_VIEW)") < 0 && site.indexOf("(USER_EDIT)") < 0 && site.indexOf("(JOB_EDIT)") < 0) {
            String groupName = thisSession.SG("admin_group_name");
            if (groupName.equals("") || groupName.equals("admin_group_name"))
              groupName = thisSession.uiSG("user_name"); 
            Properties properties = new Properties();
            properties.put("virtualPath", "/" + path);
            properties.put("name", p.getProperty("name"));
            if (v.size() == 0 || (v.size() == 1 && ((Properties)v.elementAt(0)).getProperty("url", "").equals(""))) {
              properties.put("type", "DIR");
              if (remove) {
                virtual.remove("/" + path);
              } else {
                virtual.put("/" + path, properties);
              } 
            } else if (UserTools.parentPathOK(request.getProperty("serverGroup_backup", request.getProperty("serverGroup")), groupName, ((Properties)v.elementAt(0)).getProperty("url", ""))) {
              properties.put("type", "FILE");
              properties.put("vItems", v);
              if (remove) {
                virtual.remove("/" + path);
              } else {
                virtual.put("/" + path, properties);
              } 
            } else {
              Log.log("SERVER", 0, new Date() + ":User " + thisSession.uiSG("user_name") + " Violated Security Constraint for a USER_ADMIN:" + groupName + ".  URL of VFS item not allowed:" + ((Properties)v.elementAt(0)).getProperty("url", ""));
            } 
          } else {
            Properties properties = new Properties();
            properties.put("virtualPath", "/" + path);
            if (remove) {
              virtual.remove("/" + path);
            } else {
              virtual.put("/" + path, properties);
            } 
            properties.put("name", p.getProperty("name"));
            if (v.size() == 0 || (v.size() == 1 && ((Properties)v.elementAt(0)).getProperty("url", "").equals(""))) {
              properties.put("type", "DIR");
            } else {
              properties.put("type", "FILE");
              properties.put("vItems", v);
            } 
          } 
        } 
      } 
      Properties pp = new Properties();
      pp.put("type", "DIR");
      pp.put("virtualPath", "");
      pp.put("name", "VFS");
      virtual.put("/", pp);
    } else {
      VFS tempVFS = UserTools.ut.getVFS(request.getProperty("serverGroup"), username);
      Properties virtual2 = tempVFS.homes.elementAt(0);
      virtual2.remove("vfs_permissions_object");
      virtual.putAll(virtual2);
    } 
    if (real_update)
      Common.updateObjectLog(virtual, virtual_orig, "users/" + request.getProperty("serverGroup") + "/" + username + "/vfs/", false, log_summary); 
    return VFS.getVFS(virtual);
  }
  
  public static Properties getListingInfo(Vector listing, String the_dir) {
    Vector items = new Vector();
    try {
      for (int i = 0; i < listing.size(); i++) {
        Properties list_item = listing.elementAt(i);
        Log.log("HTTP_SERVER", 3, "Adding:" + list_item.getProperty("name"));
        list_item.put("preview", "0");
        list_item.put("sizeFormatted", Common.format_bytes2(list_item.getProperty("size")));
        list_item.put("modified", list_item.getProperty("modified", "0"));
        list_item.remove("url");
        list_item.put("itemType", list_item.getProperty("type"));
        list_item.put("root_dir", list_item.getProperty("root_dir", "/"));
        items.addElement(list_item);
      } 
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    Common.do_sort(items, "name");
    for (int x = 0; x < items.size(); x++) {
      Properties lp = items.elementAt(x);
      if (lp.getProperty("dir", "").indexOf("\"") >= 0)
        lp.put("dir", lp.getProperty("dir", "").replaceAll("\\\"", "%22")); 
      if (lp.getProperty("name", "").indexOf("\"") >= 0)
        lp.put("name", lp.getProperty("name", "").replaceAll("\\\"", "%22")); 
      if (lp.getProperty("name", "").endsWith(" ") || lp.getProperty("name", "").startsWith(" "))
        lp.put("name", lp.getProperty("name", "").replaceAll(" ", "%20")); 
      if (lp.getProperty("path", "").indexOf("\"") >= 0)
        lp.put("path", lp.getProperty("path", "").replaceAll("\\\"", "%22")); 
      if (lp.getProperty("root_dir", "").indexOf("\"") >= 0)
        lp.put("root_dir", lp.getProperty("root_dir", "").replaceAll("\\\"", "%22")); 
      String itemName = lp.getProperty("name");
      String itemPath = String.valueOf(the_dir) + lp.getProperty("name");
      String root_dir = lp.getProperty("root_dir");
      String href_path = String.valueOf(lp.getProperty("root_dir")) + lp.getProperty("name");
      if (href_path.startsWith("//") && !href_path.startsWith("////"))
        href_path = "//" + href_path; 
      lp.put("source", "/WebInterface/function/?command=getPreview&size=3&path=" + itemPath);
      lp.put("href_path", href_path);
      lp.put("root_dir", root_dir);
      lp.put("name", itemName);
    } 
    Properties listingProp = new Properties();
    listingProp.put("privs", "(read)(view)");
    listingProp.put("path", the_dir);
    listingProp.put("listing", items);
    return listingProp;
  }
  
  public static String getTempAccounts(Properties request, String site) {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
        return p.get("data").toString();
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        return "FAILURE:Job not found.";
      } 
    } 
    try {
      Vector items = new Vector();
      File_U[] accounts = (File_U[])(new File_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/")).listFiles();
      SimpleDateFormat ex_sdf = new SimpleDateFormat("MMddyyyyHHmm", Locale.US);
      SimpleDateFormat expire_sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm aa", Locale.US);
      if (accounts != null)
        for (int x = 0; x < accounts.length; x++) {
          try {
            File_U f = accounts[x];
            if (f.getName().indexOf(",,") >= 0 && f.isDirectory()) {
              String[] tokens = f.getName().split(",,");
              Properties pp = new Properties();
              for (int xx = 0; xx < tokens.length; xx++) {
                String key = tokens[xx].substring(0, tokens[xx].indexOf("="));
                String val = tokens[xx].substring(tokens[xx].indexOf("=") + 1);
                pp.put(key.toUpperCase(), val);
              } 
              Properties info = (Properties)Common.readXMLObject_U(String.valueOf(f.getPath()) + "/INFO.XML");
              pp.put("expire", expire_sdf.format(ex_sdf.parseObject(pp.getProperty("EX"))));
              info.putAll(pp);
              Enumeration keys = info.keys();
              while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                if (key.startsWith("ldap_"))
                  info.remove(key); 
              } 
              info.remove("web_customizations");
              info.remove("web_buttons");
              Properties ppp = new Properties();
              ppp.put("info", info);
              ppp.put("tempaccount_user", info.get("U"));
              ppp.put("tempaccount_pass", info.get("P"));
              ppp.put("tempaccount_folder", f.getName());
              items.addElement(ppp);
            } 
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
          } 
        }  
      return Common.getXMLString(items, "temp_accounts", null);
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      return null;
    } 
  }
  
  public static String addTempAccount(Properties request, String site) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
        try {
          Properties p = DMZServer.getResponse(id, 20);
          return p.get("data").toString();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
          return "FAILURE:Job not found.";
        } 
      } 
      (new File_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS/")).mkdirs();
      (new File_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass"))).mkdirs();
      Object permissions = Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("permissions").replace('+', ' ')).getBytes("UTF8")));
      Properties info = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("info").replace('+', ' ')).getBytes("UTF8")));
      Common.writeXMLObject_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS.XML", permissions, "VFS");
      Common.writeXMLObject_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/INFO.XML", info, "INFO");
      String[] part_names = request.getProperty("tempaccount_folder").split(",,");
      Date td = (new SimpleDateFormat("MM/dd/yyyy hh:mm aa", Locale.US)).parse(info.getProperty("expire"));
      String fname = "";
      for (int x = 0; x < part_names.length; x++) {
        if (!part_names[x].startsWith("ex=")) {
          fname = String.valueOf(fname) + part_names[x];
        } else {
          fname = String.valueOf(fname) + "ex=" + (new SimpleDateFormat("MMddyyyyHHmm", Locale.US)).format(td);
        } 
        if (x < part_names.length)
          fname = String.valueOf(fname) + ",,"; 
      } 
      (new File_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder"))).renameTo(new File_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + fname));
      response = String.valueOf(response) + "Success.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String removeTempAccount(Properties request, String site) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
        try {
          Properties p = DMZServer.getResponse(id, 20);
          return p.get("data").toString();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
          return "FAILURE:Job not found.";
        } 
      } 
      Properties account_files = (Properties)Common.getElements(Common.getSaxBuilder().build(new ByteArrayInputStream(getTempAccountFiles(request, site).getBytes("UTF8"))).getRootElement());
      Common.recurseDelete_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder"), false);
      Common.recurseDelete_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass"), false);
      response = String.valueOf(response) + "Success.";
      Vector files = (Vector)account_files.get("refFiles");
      if (files != null && files.size() > 0)
        for (int x = 0; x < files.size(); x++)
          Log.log("SERVER", 0, "Removed shared file or folder: " + files.get(x));  
      Log.log("SERVER", 0, "Removed temporary account: " + request.getProperty("tempaccount_user"));
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String getTempAccountFiles(Properties request, String site) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
        try {
          Properties properties = DMZServer.getResponse(id, 20);
          return properties.get("data").toString();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
          return "FAILURE:Job not found.";
        } 
      } 
      Properties p = new Properties();
      Vector containedFiles = new Vector();
      Vector refFiles = new Vector();
      p.put("containedFiles", containedFiles);
      p.put("refFiles", refFiles);
      File_U[] files = (File_U[])(new File_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS/")).listFiles();
      if (files != null)
        for (int x = 0; x < files.length; x++) {
          if ((new File_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass") + "/" + files[x].getName())).exists()) {
            containedFiles.addElement(files[x].getName());
          } else {
            refFiles.addElement(files[x].getName());
          } 
        }  
      return Common.getXMLString(p, "temp_accounts_files", null, true);
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
      response = String.valueOf(response) + "</response></commandResult>";
      return response;
    } 
  }
  
  public static String removeTempAccountFile(Properties request, String site) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_file", Common.dots(request.getProperty("tempaccount_file")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
        try {
          Properties p = DMZServer.getResponse(id, 20);
          return p.get("data").toString();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
          return "FAILURE:Job not found.";
        } 
      } 
      Common.recurseDelete_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS/" + request.getProperty("tempaccount_file"), false);
      Common.recurseDelete_U(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass") + "/" + request.getProperty("tempaccount_file"), false);
      response = String.valueOf(response) + "Success.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String addTempAccountFile(Properties request, String site) {
    request.put("tempaccount_user", Common.dots(request.getProperty("tempaccount_user")));
    request.put("tempaccount_pass", Common.dots(request.getProperty("tempaccount_pass")));
    request.put("tempaccount_file", Common.dots(request.getProperty("tempaccount_file")));
    request.put("tempaccount_folder", Common.dots(request.getProperty("tempaccount_folder")));
    String response = "<commandResult><response>";
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
        try {
          Properties p = DMZServer.getResponse(id, 20);
          return p.get("data").toString();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
          return "FAILURE:Job not found.";
        } 
      } 
      File_U fileItem = new File_U(request.getProperty("tempaccount_file"));
      if (!fileItem.exists()) {
        response = String.valueOf(response) + "ERROR:File does not exist.";
      } else {
        String userHome = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/";
        String userStorage = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass") + "/";
        if (request.getProperty("tempaccount_reference", "false").equals("false"))
          Common.recurseCopyThreaded_U(fileItem.getPath(), String.valueOf(userStorage) + fileItem.getName() + (fileItem.isDirectory() ? "/" : ""), true, false); 
        Properties vItem = new Properties();
        if (request.getProperty("tempaccount_reference", "false").equals("false")) {
          vItem.put("url", (new File_U(String.valueOf(userStorage) + fileItem.getName())).toURI().toURL().toExternalForm());
        } else {
          vItem.put("url", fileItem.toURI().toURL().toExternalForm());
        } 
        vItem.put("type", fileItem.isDirectory() ? "dir" : "file");
        Vector v = new Vector();
        v.addElement(vItem);
        Common.writeXMLObject_U(String.valueOf(userHome) + "VFS/" + fileItem.getName(), v, "VFS");
        if (request.getProperty("tempaccount_reference", "false").equals("false"))
          Thread.sleep(500L); 
        response = String.valueOf(response) + "Success.";
      } 
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static String deleteReplication(Properties request, String site) {
    String response = "<commandResult><response>";
    try {
      if (!request.getProperty("instance", "").equals("")) {
        String id = Common.makeBoundary();
        String instance = request.remove("instance").toString();
        DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
        try {
          Properties p = DMZServer.getResponse(id, 20);
          return p.get("data").toString();
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
          return "FAILURE:Replication not found.";
        } 
      } 
      String client_id = Common.dots(request.getProperty("client_id"));
      String item_id = Common.dots(request.getProperty("item_id"));
      if (client_id.length() < 3 || item_id.length() < 5)
        throw new Exception("Invalid client or item id!"); 
      if ((new File_S("./multi_journal/" + client_id + "/" + item_id)).exists())
        Common.recurseDelete("./multi_journal/" + client_id + "/" + item_id, false); 
      ServerStatus.thisObj.server_info.put("replicated_vfs_ping_interval", "0");
      response = String.valueOf(response) + client_id + "/" + item_id + " deleted.";
    } catch (Exception ee) {
      Log.log("HTTP_SERVER", 1, ee);
      response = String.valueOf(response) + "ERROR:" + ee.toString();
    } 
    response = String.valueOf(response) + "</response></commandResult>";
    return response;
  }
  
  public static void purgeSync(Properties request, VFS uVFS, String root_dir) throws Exception {}
  
  public static Object getUserVersions(Properties request) throws ParseException {
    String username = Common.url_decode(request.getProperty("username").replace('+', ' '));
    File_S[] folders = (File_S[])(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).listFiles();
    Vector userVersions = new Vector();
    for (int x = 0; x < folders.length; x++) {
      File_S f = folders[x];
      if (f.getName().startsWith(username) && !f.getName().startsWith(String.valueOf(username) + "~") && f.getName().endsWith(".zip")) {
        Properties user_version = new Properties();
        String version_date = f.getName().substring(f.getName().indexOf("-") + 1, f.getName().indexOf("."));
        SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy_HHmmss", Locale.US);
        Date date = sdf.parse(version_date);
        SimpleDateFormat sdf_readable = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
        user_version.put(f.getName(), sdf_readable.format(date));
        userVersions.addElement(user_version);
      } 
    } 
    Properties response = new Properties();
    response.put("user_versions", userVersions);
    return response;
  }
  
  public static Object getDeletedUsers(Properties request) throws ParseException {
    String server_group = Common.url_decode(request.getProperty("serverGroup").replace('+', ' '));
    File_S[] folders = (File_S[])(new File_S(String.valueOf(System.getProperty("crushftp.backup")) + "backup/")).listFiles();
    Vector user_list = new Vector();
    UserTools.refreshUserList(server_group, user_list);
    Properties deleted_users_files = new Properties();
    for (int x = 0; x < folders.length; x++) {
      File_S f = folders[x];
      if (!f.getName().contains("~") && f.getName().endsWith(".zip")) {
        String username = f.getName().substring(0, f.getName().indexOf("-"));
        if (!user_list.contains(username))
          if (!deleted_users_files.containsKey(username)) {
            Vector files = new Vector();
            files.add(f);
            deleted_users_files.put(username, files);
          } else {
            Vector files = (Vector)deleted_users_files.get(username);
            files.add(f);
          }  
      } 
    } 
    Vector deleted_users = new Vector();
    Enumeration e = deleted_users_files.propertyNames();
    while (e.hasMoreElements()) {
      String key = (String)e.nextElement();
      Object[] files = ((Vector)deleted_users_files.get(key)).toArray();
      Arrays.sort(files, Common.get_file_last_modified_Comparator());
      Properties p = new Properties();
      p.put(key, ((File_S)files[files.length - 1]).getName());
      deleted_users.add(p);
    } 
    Properties response = new Properties();
    response.put("deleted_users", deleted_users);
    return response;
  }
  
  public static String setReportSchedules(Properties request, String site) {
    String status = "OK";
    try {
      if (!request.getProperty("instance", "").equals(""))
        synchronized (DMZServer.stop_send_prefs) {
          String id = Common.makeBoundary();
          String instance = request.remove("instance").toString();
          DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
          Properties p = DMZServer.getResponse(id, 20);
          if (request.getProperty("key").indexOf("server_settings/") >= 0) {
            String id2 = Common.makeBoundary();
            DMZServer.sendCommand(instance, new Properties(), "GET:SERVER_SETTINGS", id2);
            Properties pp = DMZServer.getResponse(id2, 20);
            SharedSessionReplicated.send("", "WRITE_PREFS", instance, pp.get("data"));
            Thread.sleep(200L);
            Common.write_server_settings((Properties)pp.get("data"), instance);
          } 
          return p.get("data").toString();
        }  
      Properties new_schedule = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("data").replace('+', ' ')).getBytes("UTF8")));
      Vector repoortSchdedules = (Vector)ServerStatus.server_settings.get("reportSchedules");
      String[] keys = request.getProperty("key").split("/");
      int index = Integer.parseInt(keys[2]);
      if (index < repoortSchdedules.size()) {
        Common.updateObjectLog(new_schedule, repoortSchdedules.elementAt(index), request.getProperty("key"), true, new StringBuffer());
      } else {
        repoortSchdedules.add(new_schedule);
      } 
      ServerStatus.thisObj.save_server_settings(false);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = "FAILURE:" + e.toString();
    } 
    return status;
  }
  
  public static String deleteReportSchedules(Properties request, String site) {
    String status = "OK";
    try {
      if (!request.getProperty("instance", "").equals(""))
        synchronized (DMZServer.stop_send_prefs) {
          String id = Common.makeBoundary();
          String instance = request.remove("instance").toString();
          DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
          Properties p = DMZServer.getResponse(id, 20);
          if (request.getProperty("key").indexOf("server_settings/") >= 0) {
            String id2 = Common.makeBoundary();
            DMZServer.sendCommand(instance, new Properties(), "GET:SERVER_SETTINGS", id2);
            Properties pp = DMZServer.getResponse(id2, 20);
            SharedSessionReplicated.send("", "WRITE_PREFS", instance, pp.get("data"));
            Thread.sleep(200L);
            Common.write_server_settings((Properties)pp.get("data"), instance);
          } 
          return p.get("data").toString();
        }  
      String[] keys = request.getProperty("key").split("/");
      int index = Integer.parseInt(keys[2]);
      ((Vector)ServerStatus.server_settings.get("reportSchedules")).remove(index);
      ServerStatus.thisObj.save_server_settings(false);
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = "FAILURE:" + e.toString();
    } 
    return status;
  }
  
  public static String setMaxServerMemory(Properties request, String site) {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 20);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else {
      Common.update_service_memory(Integer.parseInt(request.getProperty("memory", "512")));
    } 
    return "";
  }
  
  public static String restartProcess(Properties request, String site) {
    if (!request.getProperty("instance", "").equals("")) {
      String id = Common.makeBoundary();
      String instance = request.remove("instance").toString();
      DMZServer.sendCommand(instance, request, site, "RUN:INSTANCE_ACTION", id);
      try {
        Properties p = DMZServer.getResponse(id, 5);
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
      } 
    } else {
      ServerStatus.thisObj.restart_crushftp();
    } 
    return "";
  }
}
