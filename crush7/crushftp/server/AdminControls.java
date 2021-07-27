package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.File_B;
import com.crushftp.client.File_S;
import com.crushftp.client.GenericClientMulti;
import com.crushftp.client.HeapDumper;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
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
import crushftp.reports5.ReportTools;
import crushftp.server.daemon.DMZServer;
import crushftp.server.daemon.GenericServer;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import javax.crypto.Cipher;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class AdminControls {
  public static Properties jobs_summary_cache = new Properties();
  
  public static long last_job_cache_clean = System.currentTimeMillis();
  
  public static Object runInstanceAction(Properties request, String site) throws Exception {
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
      return adminAction(request, site); 
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
    if (request.getProperty("command").equals("testJobSchedule"))
      return testJobSchedule(request, site); 
    if (request.getProperty("command").equals("testSMTP"))
      return testSMTP(request, site); 
    if (request.getProperty("command").equals("sendEventEmail"))
      return sendEventEmail(request, site); 
    if (request.getProperty("command").equals("migrateUsersVFS"))
      return migrateUsersVFS(request, site); 
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
      return sendPassEmail(request, site); 
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
    return "";
  }
  
  public static Object getServerItem(String admin_group_name, Properties request, String site) {
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
              ((Properties)o).put("random_password_length", ServerStatus.SG("random_password_length"));
              ((Properties)o).put("min_password_length", ServerStatus.SG("min_password_length"));
              ((Properties)o).put("min_password_numbers", ServerStatus.SG("min_password_numbers"));
              ((Properties)o).put("min_password_lowers", ServerStatus.SG("min_password_lowers"));
              ((Properties)o).put("min_password_uppers", ServerStatus.SG("min_password_uppers"));
              ((Properties)o).put("min_password_specials", ServerStatus.SG("min_password_specials"));
              ((Properties)o).put("password_history_count", ServerStatus.SG("password_history_count"));
              ((Properties)o).put("unsafe_password_chars", ServerStatus.SG("unsafe_password_chars"));
              ((Properties)o).put("blank_passwords", ServerStatus.SG("blank_passwords"));
              ((Properties)o).put("user_default_folder_privs", ServerStatus.SG("user_default_folder_privs"));
              ((Properties)o).put("v8_beta", ServerStatus.SG("v8_beta"));
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
  
  public static Object getDashboardItems(String admin_group_name, Properties request, String site) {
    try {
      Properties o = null;
      Properties o2 = new Properties();
      try {
        if (request.getProperty("instance", "").equals("")) {
          o = (Properties)ServerStatus.thisObj.server_info.clone();
        } else {
          o = null;
          String id = Common.makeBoundary();
          DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
          Properties p = DMZServer.getResponse(id, 20);
          o = (Properties)p.get("data");
        } 
        String[] keys = "jce_installed,machine_is_linux,machine_is_solaris,machine_is_unix,machine_is_windows,machine_is_x,machine_is_x_10_5_plus,sub_version_info_str,version_info_str,registration_name,rid,enterprise_level,max_max_users,update_available,update_available_version,update_available_html,about_info_str,server_start_time,registration_email,server_list,recent_drives,recent_hammering,last_logins,ram_max,ram_free,thread_pool_available,thread_pool_busy,downloaded_files,uploaded_files,total_server_bytes_sent,total_server_bytes_received,successful_logins,failed_logins,current_download_speed,current_upload_speed,concurrent_users,replicated_servers,replicated_servers_count,replicated_servers_pending_user_sync,replicated_servers_pendingResponses,replicated_servers_lastActive,replicated_servers_sent_1,replicated_servers_sent_2,replicated_servers_sent_3,replicated_servers_sent_4,replicated_servers_sent_5,replicated_servers_queue_1,replicated_servers_queue_2,replicated_servers_queue_3,replicated_servers_queue_4,replicated_servers_queue_5,java_info,memcache_objects".split(",");
        for (int x = 0; x < keys.length; x++) {
          Object tmp = o.get(keys[x]);
          if (tmp == null)
            tmp = ""; 
          o2.put(keys[x], tmp);
        } 
        o2.put("replication_status", GenericClientMulti.replication_status);
      } catch (Exception e) {
        return "FAILURE:" + e.toString();
      } 
      return o2;
    } catch (Exception e) {
      return e.toString();
    } 
  }
  
  public static Object getJob(Properties request, String site) {
    Vector jobs = JobScheduler.getJobList();
    if (request.getProperty("name", "").equals("")) {
      Vector all = new Vector();
      for (int x = 0; x < jobs.size(); x++) {
        File f = jobs.elementAt(x);
        if (!f.getName().startsWith("_"))
          if (request.getProperty("schedule_info", "").equals("true")) {
            Properties p = (Properties)Common.readXMLObject(String.valueOf(((File)jobs.elementAt(x)).getPath()) + "/job.XML");
            if (p != null) {
              Properties p2 = new Properties();
              p2.put("name", f.getName());
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
            all.addElement(f.getName());
          }  
      } 
      return all;
    } 
    if (site.indexOf("(CONNECT)") >= 0 || site.indexOf("(JOB_EDIT)") >= 0 || site.indexOf("(JOB_VIEW)") >= 0)
      for (int x = 0; x < jobs.size(); x++) {
        String job_name = ((File)jobs.elementAt(x)).getName();
        if (request.getProperty("name", "").equalsIgnoreCase(job_name)) {
          Properties p = (Properties)Common.readXMLObject(String.valueOf(((File)jobs.elementAt(x)).getPath()) + "/job.XML");
          p.put("scheduleName", job_name);
          return p;
        } 
      }  
    return "FAILURE:Job not found.";
  }
  
  public static Object renameJob(Properties request, String site, boolean replicate) {
    if (ServerStatus.BG("replicate_jobs") && replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.renameJob", "info", pp);
    } 
    Vector jobs = JobScheduler.getJobList();
    for (int x = 0; x < jobs.size(); x++) {
      if (request.getProperty("priorName", "").equalsIgnoreCase(((File)jobs.elementAt(x)).getName())) {
        if (((File)jobs.elementAt(x)).renameTo(new File(String.valueOf(((File)jobs.elementAt(x)).getParent()) + "/" + JobScheduler.safeName(request.getProperty("name"))))) {
          Properties job = (Properties)Common.readXMLObject(new File(String.valueOf(((File)jobs.elementAt(x)).getParent()) + "/" + JobScheduler.safeName(request.getProperty("name")) + "/job.XML"));
          job.put("scheduleName", JobScheduler.safeName(request.getProperty("name")));
          try {
            Common.writeXMLObject(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + request.getProperty("name") + "/job.XML", job, "job");
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
  
  public static Object removeJob(Properties request, String site, boolean replicate) {
    if (ServerStatus.BG("replicate_jobs") && replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.removeJob", "info", pp);
    } 
    Vector jobs = JobScheduler.getJobList();
    for (int x = 0; x < jobs.size(); x++) {
      if (request.getProperty("name", "").equalsIgnoreCase(((File)jobs.elementAt(x)).getName())) {
        Common.recurseDelete(((File)jobs.elementAt(x)).getPath(), false);
        return "SUCCESS:" + JobScheduler.safeName(request.getProperty("name"));
      } 
    } 
    return "FAILURE:Job not found.";
  }
  
  public static Object addJob(Properties request, String site, boolean replicate) {
    if (ServerStatus.BG("replicate_jobs") && replicate) {
      Properties pp = new Properties();
      pp.put("request", request);
      pp.put("site", site);
      SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.AdminControls.addJob", "info", pp);
    } 
    Properties job = null;
    try {
      job = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("data").replace('+', ' ')).getBytes("UTF8")));
      if (job == null)
        throw new Exception("Invalid xml for job"); 
      Common.urlDecodePost(request);
      String jobName = JobScheduler.safeName(Common.dots(request.getProperty("name")));
      (new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName)).mkdirs();
      (new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job2.XML")).delete();
      Common.writeXMLObject(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job2.XML", job, "job");
      if ((new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job.XML")).exists()) {
        (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + jobName + "_19.XML")).delete();
        for (int x = 18; x >= 0; x--) {
          try {
            (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + jobName + "_" + x + ".XML")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + jobName + "_" + (x + 1) + ".XML"));
          } catch (Exception exception) {}
        } 
        (new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job.XML")).renameTo(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + jobName + "_0.XML"));
        (new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job.XML")).delete();
      } 
      if ((new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job2.XML")).length() > 0L) {
        (new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job2.XML")).renameTo(new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + jobName + "/job.XML"));
      } else {
        throw new Exception("Failed to save job...0 byte save, aborting.");
      } 
      return "SUCCESS:" + jobName;
    } catch (Exception e) {
      return "FAILURE:" + e.toString();
    } 
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
  
  public static Vector getJobsSummary(Properties request, String site) throws Exception {
    if (ServerStatus.siIG("enterprise_level") <= 0)
      return new Vector(); 
    Properties si = null;
    if (request.getProperty("instance", "").equals("")) {
      si = ServerStatus.thisObj.server_info;
    } else {
      String id = Common.makeBoundary();
      DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
      Properties p = DMZServer.getResponse(id, 20);
      si = (Properties)p.get("data");
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
      if (site.indexOf("(JOB_MONITOR)") < 0 || settings.getProperty("allowed_usernames", "").indexOf(request.getProperty("calling_user", "~NONE~")) >= 0)
        vv.insertElementAt(summaryJob, 0); 
    } 
    long start_time = Long.parseLong(request.getProperty("start_time", "0"));
    long end_time = Long.parseLong(request.getProperty("end_time", "0"));
    jobs = JobScheduler.getJobList();
    SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss", Locale.US);
    Vector vv2 = new Vector();
    String filter = request.getProperty("filter", "").toUpperCase();
    if (site.indexOf("(JOB_MONITOR)") < 0)
      synchronized (jobs_summary_cache) {
        for (int i = 0; i < jobs.size(); i++) {
          File f = (File)jobs.elementAt(i);
          File[] f2 = f.listFiles();
          for (int xx = 0; f2 != null && xx < f2.length; xx++) {
            String job_name = f2[xx].getName();
            if (!job_name.equalsIgnoreCase("job.XML") && 
              !job_name.equalsIgnoreCase("inprogress.XML") && 
              !job_name.equalsIgnoreCase("inprogress") && 
              job_name.toUpperCase().endsWith(".XML")) {
              job_name = job_name.substring(0, job_name.lastIndexOf(".XML"));
              if (job_name.indexOf("_") >= 0) {
                String job_id = job_name.split("_")[0];
                try {
                  if (!job_name.split("_")[1].equals("new") && sdf.parse(job_name.split("_")[1]).getTime() >= start_time && sdf.parse(job_name.split("_")[1]).getTime() <= end_time) {
                    Properties summaryJob = null;
                    if (jobs_summary_cache.containsKey(f2[xx].getPath())) {
                      Properties sj = (Properties)jobs_summary_cache.get(f2[xx].getPath());
                      if (sj.getProperty("modified", "0").equals((new StringBuffer(String.valueOf(f2[xx].lastModified()))).toString()))
                        summaryJob = sj; 
                    } 
                    boolean ok = false;
                    if (summaryJob == null) {
                      Properties tracker = (Properties)Common.readXMLObject(f2[xx].getPath());
                      summaryJob = new Properties();
                      summaryJob.put("name", "");
                      summaryJob.put("start", tracker.getProperty("start", ""));
                      summaryJob.put("end", tracker.getProperty("end", "0"));
                      summaryJob.put("id", job_id);
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
            } 
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
          File f = new File(key);
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
      DMZServer.sendCommand(request.getProperty("instance", ""), request, "GET:SERVER_INFO", id);
      Properties p = DMZServer.getResponse(id, 20);
      si = (Properties)p.get("data");
    } 
    Vector jobs = (Vector)si.get("running_tasks");
    Vector vv = new Vector();
    int x;
    for (x = 0; vv.size() == 0 && x < jobs.size(); x++) {
      Properties job = jobs.elementAt(x);
      if (job.getProperty("id").equals(request.getProperty("job_id"))) {
        Properties settings = (Properties)job.get("settings");
        if (settings == null || site.indexOf("(JOB_MONITOR)") < 0 || settings.getProperty("allowed_usernames", "").indexOf(request.getProperty("calling_user", "~NONE~")) >= 0)
          vv.addElement(job.clone()); 
      } 
    } 
    if (vv.size() == 0 && site.indexOf("(JOB_MONITOR)") < 0)
      if (!request.getProperty("scheduleName", "").equals("")) {
        File f = new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + Common.dots(request.getProperty("scheduleName", "")));
        checkJobFolder(request, vv, f);
      } else {
        jobs = JobScheduler.getJobList();
        for (x = 0; vv.size() == 0 && x < jobs.size(); x++)
          checkJobFolder(request, vv, (File)jobs.elementAt(x)); 
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
  
  public static void checkJobFolder(Properties request, Vector vv, File f) {
    File[] f2 = f.listFiles();
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
          vv.addElement(tracker.clone());
        } 
      } 
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
    if (Common.machine_is_x() && !(new File(request.getProperty("path"))).exists())
      request.put("path", "/Volumes" + request.getProperty("path")); 
    if (!(new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).mkdir())
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
    if (Common.machine_is_x() && !(new File(request.getProperty("path"))).exists())
      request.put("path", "/Volumes" + request.getProperty("path")); 
    if (!(new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).renameTo(new File(String.valueOf(request.getProperty("path")) + request.getProperty("newName"))))
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
    if (Common.machine_is_x() && !(new File(request.getProperty("path"))).exists())
      request.put("path", "/Volumes" + request.getProperty("path")); 
    Vector list = new Vector();
    try {
      Common.getAllFileListing(list, (new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).getCanonicalPath(), 5, true);
      if (list.size() > 100)
        return "Too many items to allow duplicate! " + list.size(); 
      Common.recurseCopy((new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).getCanonicalPath(), String.valueOf((new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).getCanonicalPath()) + "_tmp_" + Common.makeBoundary(), false);
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
    if (Common.machine_is_x() && !(new File(request.getProperty("path"))).exists())
      request.put("path", "/Volumes" + request.getProperty("path")); 
    if (!(new File(String.valueOf(request.getProperty("path")) + request.getProperty("name"))).delete())
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
      String[] keys = request.getProperty("key").split("/");
      Object o = null;
      boolean restartServers = false;
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
            Common.updateObject(new_o, o);
          } else if (new_o instanceof Properties || new_o instanceof Vector) {
            Common.updateObject(new_o, ((Properties)o).get(lastKey));
          } else {
            ((Properties)o).put(lastKey, new_o.toString());
          } 
        } else if (o instanceof Vector) {
          Vector v = (Vector)o;
          if (request.getProperty("data_action", "").equals("reset")) {
            if (request.getProperty("key").indexOf("/plugins/") >= 0) {
              if (new_o == null)
                new_o = new Vector(); 
              Common.updateObject(new_o, v);
            } else {
              v.removeAllElements();
              if (new_o == null)
                new_o = new Vector(); 
              v.addAll((Vector)new_o);
            } 
          } else {
            int i = Integer.parseInt(lastKey);
            if (request.getProperty("data_action", "").equals("remove")) {
              v.remove(i);
            } else if (request.getProperty("data_action", "").equals("move_left")) {
              Object o2 = v.elementAt(i);
              if (i > 0) {
                Object o1 = v.elementAt(i - 1);
                v.setElementAt(o2, i - 1);
                v.setElementAt(o1, i);
                if (i - 1 == 0)
                  ((Properties)o1).put("subItem", ""); 
              } 
            } else if (request.getProperty("data_action", "").equals("move_right")) {
              Object o2 = v.elementAt(i);
              if (i <= v.size() - 2) {
                Object o1 = v.elementAt(i + 1);
                v.setElementAt(o2, i + 1);
                v.setElementAt(o1, i);
              } 
            } else if (i > v.size() - 1) {
              v.addElement(new_o);
              if (v == (Vector)ServerStatus.server_settings.get("server_list"))
                ServerStatus.thisObj.start_this_server(i); 
            } else if (new_o instanceof Properties || new_o instanceof Vector) {
              Common.updateObject(new_o, v.elementAt(i));
            } else {
              v.setElementAt(new_o.toString(), i);
            } 
          } 
          if (request.getProperty("key").indexOf("/plugins/") >= 0)
            ServerStatus.thisObj.common_code.loadPluginsSync(ServerStatus.server_settings, ServerStatus.thisObj.server_info); 
          if (secondLastKey.equals("server_list"))
            restartServers = true; 
          if (request.getProperty("key").indexOf("server_settings") >= 0)
            ServerStatus.thisObj.reset_preview_workers(); 
        } 
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
        status = "FAILURE:" + e.toString();
      } 
      if (restartServers) {
        ServerStatus.thisObj.stop_all_servers();
        ServerStatus.thisObj.start_all_servers();
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
        throw new Exception("User not found:" + username + ":" + new_user); 
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
      UserTools.getExtraVFS(request.getProperty("serverGroup"), username, null, new_user);
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
      if (!pass.startsWith("SHA:") && !pass.startsWith("SHA512:") && !pass.startsWith("SHA3:") && !pass.startsWith("MD5:") && !pass.startsWith("CRYPT3:") && !pass.startsWith("BCRYPT:") && !pass.startsWith("MD5CRYPT:") && !pass.startsWith("SHA512CRYPT:")) {
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
      return user_items;
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
      status = e.toString();
      return status;
    } 
  }
  
  public static Object getPublicKeys(Properties request) {
    return "";
  }
  
  public static String setUserItem(Properties request, SessionCrush thisSession, String site) {
    String status = "OK";
    try {
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
          } else if (request.getProperty("data_action", "").equals("delete")) {
            groups = UserTools.getGroups(request.getProperty("serverGroup"));
            Vector group = (Vector)groups.get(request.getProperty("group_name"));
            if (group == null)
              group = new Vector(); 
            groups.put(request.getProperty("group_name"), group);
            if (!request.containsKey("usernames"))
              request.put("usernames", request.getProperty("username", "")); 
            String[] usernames = Common.url_decode(request.getProperty("usernames").replace('+', ' ')).split(";");
            if (usernames.length == 0) {
              groups.remove(request.getProperty("group_name"));
            } else {
              for (int x = 0; x < usernames.length; x++)
                group.remove(usernames[x].trim()); 
            } 
          } else {
            groups = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(request.getProperty("groups").replace('+', ' ')).getBytes("UTF8")));
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
          } else if (request.getProperty("data_action", "").equals("delete")) {
            inheritances = UserTools.getInheritance(request.getProperty("serverGroup"));
            if (!request.containsKey("usernames"))
              request.put("usernames", request.getProperty("username", "")); 
            String[] usernames = Common.url_decode(request.getProperty("usernames").replace('+', ' ')).split(";");
            for (int x = 0; x < usernames.length; x++) {
              Vector inherit = (Vector)inheritances.get(usernames[x]);
              if (inherit == null)
                inherit = new Vector(); 
              inherit.remove(request.getProperty("inheritance_name"));
              inheritances.put(usernames[x], inherit);
            } 
          } else {
            inheritances = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(request.getProperty("inheritance").replace('+', ' ')).getBytes("UTF8")));
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
                UserTools.deleteUser(request.getProperty("serverGroup"), username);
                Vector user_list = new Vector();
                UserTools.refreshUserList(request.getProperty("serverGroup"), user_list);
                for (int xx = 0; xx < user_list.size(); xx++) {
                  String newUser = Common.dots(user_list.elementAt(xx).toString());
                  if (newUser.toUpperCase().endsWith(".SHARED")) {
                    File f = new File(String.valueOf(System.getProperty("crushftp.users")) + "/" + request.getProperty("serverGroup") + "/" + newUser + "/VFS/Shares/" + username);
                    if (f.exists()) {
                      Common.recurseDelete(f.getCanonicalPath(), false);
                      f = new File(String.valueOf(System.getProperty("crushftp.users")) + "/" + request.getProperty("serverGroup") + "/" + newUser + "/VFS/Shares/");
                      if (f.listFiles() == null || (f.listFiles()).length == 0)
                        UserTools.deleteUser(request.getProperty("serverGroup"), newUser); 
                    } 
                  } 
                } 
                File[] accounts = (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/")).listFiles();
                for (int i = 0; accounts != null && i < accounts.length; i++) {
                  try {
                    if (accounts[i].getName().indexOf(",,") >= 0 && accounts[i].isDirectory()) {
                      String[] tokens = accounts[i].getName().split(",,");
                      Properties pp = new Properties();
                      for (int loop = 0; loop < tokens.length; loop++)
                        pp.put(tokens[loop].substring(0, tokens[loop].indexOf("=")).toUpperCase(), tokens[loop].substring(tokens[loop].indexOf("=") + 1)); 
                      if (username.equalsIgnoreCase(pp.getProperty("M"))) {
                        Common.recurseDelete(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + pp.getProperty("U") + pp.getProperty("P"), false);
                        Common.recurseDelete(accounts[i].getCanonicalPath(), false);
                      } 
                    } 
                  } catch (Exception e) {
                    Log.log("HTTP_SERVER", 1, e);
                  } 
                } 
              } else {
                Properties new_user = new Properties();
                if (!request.getProperty("user", "").equals(""))
                  new_user = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("user").replace('+', ' ')).getBytes("UTF8"))); 
                new_user.put("userVersion", "6");
                if (new_user.containsKey("password")) {
                  String pass = new_user.getProperty("password", "");
                  if (!pass.startsWith("SHA:") && !pass.startsWith("SHA512:") && !pass.startsWith("SHA3:") && !pass.startsWith("MD5:") && !pass.startsWith("CRYPT3:") && !pass.startsWith("BCRYPT:") && !pass.startsWith("MD5CRYPT:") && !pass.startsWith("SHA512CRYPT:")) {
                    pass = ServerStatus.thisObj.common_code.encode_pass(pass, ServerStatus.SG("password_encryption"), new_user.getProperty("salt", ""));
                    new_user.put("password", pass);
                  } else {
                    new_user.put("password", pass);
                  } 
                } 
                if (request.getProperty("data_action").equals("update")) {
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  Common.updateObject(new_user, user);
                  new_user = user;
                } 
                if (request.getProperty("data_action").equals("update_remove")) {
                  Properties user = UserTools.ut.getUser(request.getProperty("serverGroup"), username, false);
                  String[] keys = request.getProperty("update_remove_key", "").split(";");
                  for (int xx = 0; xx < keys.length; xx++)
                    user.remove(keys[xx]); 
                  new_user = user;
                } 
                UserTools.writeUser(request.getProperty("serverGroup"), username, new_user);
                UserTools.ut.getUser(request.getProperty("serverGroup"), username, true);
                if (request.containsKey("vfs_items") || request.containsKey("permissions"))
                  UserTools.writeVFS(request.getProperty("serverGroup"), username, processVFSSubmission(request, username, site, thisSession)); 
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
    if (site.indexOf("(CONNECT)") < 0 && site.indexOf("(USER_VIEW)") < 0 && site.indexOf("(USER_EDIT)") < 0 && site.indexOf("(REPORT_EDIT)") < 0) {
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
    VFS tempVFS = processVFSSubmission(request, username, site, thisSession);
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
    if (!(new File(path)).exists() && Common.machine_is_x())
      path = "/Volumes" + path; 
    path = Common.dots(path);
    File[] items = (File[])null;
    if (path.equals("/") && Common.machine_is_x()) {
      try {
        File[] other_volumes = (new File("/Volumes/")).listFiles();
        if (other_volumes == null) {
          other_volumes = new File[1];
          other_volumes[0] = new File("/");
        } 
        items = new File[other_volumes.length];
        for (int i = 0; i < other_volumes.length; i++)
          items[i] = other_volumes[i]; 
      } catch (Exception exception) {}
    } else if (path.equals("/") && Common.machine_is_windows()) {
      items = File.listRoots();
    } else {
      items = (new File(path)).listFiles();
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
    if (command.equalsIgnoreCase("getJobsSummary") && (site.indexOf("(JOB_EDIT)") >= 0 || site.indexOf("(JOB_VIEW)") >= 0 || site.indexOf("(JOB_LIST_HISTORY)") >= 0))
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
    if (command.equalsIgnoreCase("setServerItem") && site.indexOf("(PREF_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("getUser") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(USER_VIEW)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("setUserItem") && site.indexOf("(USER_EDIT)") >= 0)
      return true; 
    if (command.equalsIgnoreCase("refreshUser") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0))
      return true; 
    if (command.equalsIgnoreCase("getUserXMLListing") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(USER_VIEW)") >= 0 || site.indexOf("(JOB_EDIT)") >= 0))
      return true; 
    if ((command.equalsIgnoreCase("getUserList") && (site.indexOf("(USER_ADMIN)") >= 0 || site.indexOf("(USER_EDIT)") >= 0 || site.indexOf("(USER_VIEW)") >= 0)) || site.indexOf("(REPORT_EDIT)") >= 0)
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
    return false;
  }
  
  public static Object adminAction(Properties request, String site) {
    String status = "";
    try {
      if (!request.getProperty("instance", "").equals("") && checkRole("getServerItem", site)) {
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
      if (request.getProperty("action", "").equals("kick") && checkRole(request.getProperty("action", ""), site)) {
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
      } else if ((request.getProperty("action", "").equals("ban") || request.getProperty("action", "").equals("temporaryBan")) && checkRole("ban", site)) {
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
      } else if (request.getProperty("action", "").equals("getUserInfo") && checkRole(request.getProperty("action", ""), site)) {
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
      } else if (request.getProperty("action", "").equals("startAllServers") && checkRole(request.getProperty("action", ""), site)) {
        ServerStatus.thisObj.start_all_servers();
        status = "OK";
      } else if (request.getProperty("action", "").equals("stopAllServers") && checkRole(request.getProperty("action", ""), site)) {
        ServerStatus.thisObj.stop_all_servers();
        Thread.sleep(1000L);
        ServerStatus.thisObj.stop_all_servers();
        status = "OK";
      } else if (request.getProperty("action", "").equals("startServer") && checkRole(request.getProperty("action", ""), site)) {
        ServerStatus.thisObj.start_this_server(Integer.parseInt(request.getProperty("index")));
        status = "OK";
      } else if (request.getProperty("action", "").equals("stopServer") && checkRole(request.getProperty("action", ""), site)) {
        ServerStatus.thisObj.stop_this_server(Integer.parseInt(request.getProperty("index")));
        status = "OK";
      } else if (request.getProperty("action", "").equals("restartServer") && checkRole(request.getProperty("action", ""), site)) {
        int connected = ((GenericServer)ServerStatus.thisObj.main_servers.elementAt(Integer.parseInt(request.getProperty("index")))).connected_users;
        ServerStatus.thisObj.stop_this_server(Integer.parseInt(request.getProperty("index")));
        Thread.sleep(1000L);
        ServerStatus.thisObj.start_this_server(Integer.parseInt(request.getProperty("index")));
        ((GenericServer)ServerStatus.thisObj.main_servers.elementAt(Integer.parseInt(request.getProperty("index")))).connected_users = connected;
        status = "OK";
      } else if (request.getProperty("action", "").equals("allStats") && checkRole(request.getProperty("action", ""), site)) {
        ServerStatus.thisObj.reset_server_login_counts();
        ServerStatus.thisObj.reset_server_bytes_in_out();
        ServerStatus.thisObj.reset_upload_download_counter();
        status = "OK";
      } else if (request.getProperty("action", "").equals("loginStats") && checkRole(request.getProperty("action", ""), site)) {
        ServerStatus.thisObj.reset_server_login_counts();
        status = "OK";
      } else if (request.getProperty("action", "").equals("uploadDownloadStats") && checkRole(request.getProperty("action", ""), site)) {
        ServerStatus.thisObj.reset_upload_download_counter();
        status = "OK";
      } else if (request.getProperty("action", "").equals("transferStats") && checkRole(request.getProperty("action", ""), site)) {
        ServerStatus.thisObj.reset_server_bytes_in_out();
        status = "OK";
      } else if (request.getProperty("action", "").equals("serverStats") && checkRole(request.getProperty("action", ""), site)) {
        status = "OK";
      } else if (request.getProperty("action", "").equals("clearMaxTransferAmounts") && checkRole(request.getProperty("action", ""), site)) {
        ServerStatus.thisObj.statTools.clearMaxTransferAmounts(request);
        status = "OK";
      } else if (request.getProperty("action", "").equals("newFolder") && checkRole(request.getProperty("action", ""), site)) {
        status = newFolder(request, site, true);
        if (!status.equalsIgnoreCase("OK"))
          throw new Exception(status); 
      } else if (request.getProperty("action", "").equals("renameItem") && checkRole(request.getProperty("action", ""), site)) {
        status = renameItem(request, site, true);
        if (!status.equalsIgnoreCase("OK"))
          throw new Exception(status); 
      } else if (request.getProperty("action", "").equals("duplicateItem") && checkRole(request.getProperty("action", ""), site)) {
        status = duplicateItem(request, site, true);
        if (!status.equalsIgnoreCase("OK"))
          throw new Exception(status); 
      } else if (request.getProperty("action", "").equals("deleteItem") && checkRole(request.getProperty("action", ""), site)) {
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
      Common.generateKeyPair(Common.url_decode(request.getProperty("pgpPivateKeyPathGenerate").replace('+', ' ')), Integer.parseInt(request.getProperty("pgpKeySizeGenerate")), Integer.parseInt(request.getProperty("pgpKeyDaysGenerate")), Common.url_decode(request.getProperty("pgpPrivateKeyPasswordGenerate")), Common.url_decode(request.getProperty("pgpCommonNameGenerate").replace('+', ' ')));
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
      Object reportItem = ServerStatus.thisObj.rt.getReportItem(request.getProperty("reportName"), ServerStatus.server_settings, ServerStatus.thisObj.server_info);
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
      params.put("server_info", ServerStatus.thisObj.server_info);
      params.put("server_settings", ServerStatus.server_settings);
      Properties status = new Properties();
      xml = ServerStatus.thisObj.rt.writeReport("", "", status, params, reportItem, ServerStatus.server_settings, ServerStatus.thisObj.server_info, request.getProperty("export", "false").equals("true"), request.getProperty("reportName"), request);
      if (status.getProperty("report_empty", "true").equals("true")) {
        xml = status.remove("report_text").toString();
        xml = String.valueOf(xml) + "<hr/><center><h1>No data to report.</h1></center><hr/>";
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
      config.put("server_info", ServerStatus.thisObj.server_info);
      Properties status = new Properties();
      String dir = params.getProperty("reportFolder");
      if (!dir.endsWith("/"))
        dir = String.valueOf(dir) + "/"; 
      String filename = params.getProperty("reportFilename");
      filename = ServerStatus.thisObj.rt.replaceVars(filename, params, config);
      if (params.getProperty("reportOverwrite").equals("false") && (new File(String.valueOf(dir) + filename)).exists()) {
        response = String.valueOf(response) + "The report file already exists.";
      } else {
        if (config.get("usernames") == null)
          config.put("usernames", new Vector()); 
        ReportTools.skipEmail(config.getProperty("reportName"), config);
        ServerStatus.thisObj.rt.writeReport(filename, dir, status, config, ServerStatus.thisObj.rt.getReportItem(config.getProperty("reportName"), ServerStatus.server_settings, ServerStatus.thisObj.server_info), ServerStatus.server_settings, ServerStatus.thisObj.server_info, config.getProperty("export", "").equals("true"), config.getProperty("reportName"), params);
        ReportTools.unSkipEmail(config.getProperty("reportName"), config);
        response = String.valueOf(response) + LOC.G("Report written to") + ":" + dir + filename;
      } 
      response = String.valueOf(response) + "</response></commandResult>";
    } catch (Exception e) {
      Log.log("HTTP_SERVER", 1, e);
    } 
    return response;
  }
  
  public static Vector runningSchedules = new Vector();
  
  public static Object changeJobStatus(Properties request, String site) {
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
      Vector jobs = JobScheduler.getJobList();
      File job = null;
      for (int x = 0; job == null && x < jobs.size(); x++) {
        File f = jobs.elementAt(x);
        if (f.getName().equalsIgnoreCase(request.getProperty("scheduleName")))
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
  
  static String startJob(File job, boolean restore, StringBuffer jobid, Properties request) {
    String response = "";
    Properties params = (Properties)Common.readXMLObject(String.valueOf(job.getPath()) + "/job.XML");
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
        Worker.startWorker(new Runnable(restore, params, request) {
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
            });
      } catch (Exception e) {
        Log.log("HTTP_SERVER", 1, e);
      } 
    } 
    return response;
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
  
  public static String sendEventEmail(Properties request, String site) {
    String response = "<commandResult><response>";
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
          urls[x] = (new File(db_drv_files[x])).toURI().toURL(); 
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
          urls[x] = (new File(db_drv_files[x])).toURI().toURL(); 
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
          BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(table)));
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
          RandomAccessFile raf = new RandomAccessFile(table, "rw");
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
        if (v != null && (v.equals("4") || v.equals("5") || v.equals("6"))) {
          response = String.valueOf(response) + "CrushFTP " + v + " needs an upgrade license for CrushFTP 7.  http://www.crushftp.com/pricing.html";
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
          BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(the_dir)));
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
  
  public static String sendPassEmail(Properties request, String site) {
    Vector email_templates = ServerStatus.VG("email_templates");
    String subject = ServerStatus.SG("emailReminderSubjectText");
    String body = ServerStatus.SG("emailReminderBodyText");
    String from = ServerStatus.SG("smtp_from");
    String reply_to = "";
    String cc = "";
    String bcc = "";
    String templateName = Common.url_decode(request.getProperty("email_template", "").replace('+', ' ')).trim();
    Common.debug(2, "Looking up template info for admin pass email:" + templateName);
    for (int x = 0; x < email_templates.size(); x++) {
      Properties email_template = email_templates.elementAt(x);
      if (email_template.getProperty("name", "").replace('+', ' ').equals(templateName)) {
        Common.debug(2, "Found template:" + email_template);
        subject = email_template.getProperty("emailSubject", "");
        body = email_template.getProperty("emailBody", "");
        from = email_template.getProperty("emailFrom", from);
        cc = email_template.getProperty("emailCC", "");
        reply_to = email_template.getProperty("emailReplyTo", "");
        bcc = email_template.getProperty("emailBCC", "");
      } 
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
        body = Common.replace_str(body, "{user_name}", request.getProperty("user_name"));
        body = Common.replace_str(body, "{username}", request.getProperty("user_name"));
        body = Common.replace_str(body, "{user_pass}", request.getProperty("user_pass"));
        body = Common.replace_str(body, "{user_password}", request.getProperty("user_pass"));
        body = Common.replace_str(body, "{user_email}", request.getProperty("user_email"));
        body = Common.replace_str(body, "{user_first_name}", request.getProperty("user_first_name"));
        body = Common.replace_str(body, "{user_last_name}", request.getProperty("user_last_name"));
        body = ServerStatus.change_vars_to_values_static(body, userTemp, new Properties(), null);
        subject = Common.replace_str(subject, "%user_name%", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "%username%", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "%user_pass%", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "%user_password%", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "%user_email%", request.getProperty("user_email"));
        subject = Common.replace_str(subject, "%user_first_name%", request.getProperty("user_first_name"));
        subject = Common.replace_str(subject, "%user_last_name%", request.getProperty("user_last_name"));
        subject = Common.replace_str(subject, "{user_name}", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "{username}", request.getProperty("user_name"));
        subject = Common.replace_str(subject, "{user_pass}", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "{user_password}", request.getProperty("user_pass"));
        subject = Common.replace_str(subject, "{user_email}", request.getProperty("user_email"));
        subject = Common.replace_str(subject, "{user_first_name}", request.getProperty("user_first_name"));
        subject = Common.replace_str(subject, "{user_last_name}", request.getProperty("user_last_name"));
        subject = ServerStatus.change_vars_to_values_static(subject, userTemp, new Properties(), null);
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
        RandomAccessFile testIn = new RandomAccessFile(request.getProperty("keystorePath"), "r");
        if (testIn.length() == 0L)
          throw new IOException("Keystore file not found:" + request.getProperty("keystorePath")); 
        testIn.close();
      } 
      SSLServerSocket ss = (SSLServerSocket)ServerStatus.thisObj.common_code.getServerSocket(0, "127.0.0.1", request.getProperty("keystorePath"), request.getProperty("keystorePass"), request.getProperty("keyPass"), "", false, 10, false, true);
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
  
  public static VFS processVFSSubmission(Properties request, String username, String site, SessionCrush thisSession) throws Exception {
    Properties virtual = UserTools.generateEmptyVirtual();
    Properties permission0 = null;
    if (request.containsKey("permissions")) {
      permission0 = (Properties)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(Common.replace_str(request.getProperty("permissions").replace('+', ' '), "%26", "&amp;")).getBytes("UTF8")));
    } else {
      VFS tempVFS = UserTools.ut.getVFS(request.getProperty("serverGroup"), username);
      permission0 = tempVFS.getPermission0();
    } 
    Vector permissions = new Vector();
    permissions.addElement(permission0);
    virtual.put("vfs_permissions_object", permissions);
    if (request.containsKey("vfs_items")) {
      Vector vfs_items = (Vector)Common.readXMLObjectError(new ByteArrayInputStream(Common.url_decode(Common.replace_str(request.getProperty("vfs_items").replace('+', ' '), "%26", "&amp;")).getBytes("UTF8")));
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
              virtual.put("/" + path, properties);
            } else if (UserTools.parentPathOK(request.getProperty("serverGroup_backup", request.getProperty("serverGroup")), groupName, ((Properties)v.elementAt(0)).getProperty("url", ""))) {
              properties.put("type", "FILE");
              properties.put("vItems", v);
              virtual.put("/" + path, properties);
            } else {
              Log.log("HTTP_SERVER", 0, new Date() + ":User " + thisSession.uiSG("user_name") + " Violated Security Constraint for a USER_ADMIN:" + groupName + ".  URL of VFS item not allowed:" + ((Properties)v.elementAt(0)).getProperty("url", ""));
            } 
          } else {
            Properties properties = new Properties();
            properties.put("virtualPath", "/" + path);
            virtual.put("/" + path, properties);
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
    try {
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Vector items = new Vector();
      File[] accounts = (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/")).listFiles();
      SimpleDateFormat ex_sdf = new SimpleDateFormat("MMddyyyyHHmm", Locale.US);
      SimpleDateFormat expire_sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm aa", Locale.US);
      if (accounts != null)
        for (int x = 0; x < accounts.length; x++) {
          try {
            File f = accounts[x];
            if (f.getName().indexOf(",,") >= 0 && f.isDirectory()) {
              String[] tokens = f.getName().split(",,");
              Properties pp = new Properties();
              for (int xx = 0; xx < tokens.length; xx++) {
                String key = tokens[xx].substring(0, tokens[xx].indexOf("="));
                String val = tokens[xx].substring(tokens[xx].indexOf("=") + 1);
                pp.put(key.toUpperCase(), val);
              } 
              Properties info = (Properties)Common.readXMLObject(String.valueOf(f.getPath()) + "/INFO.XML");
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
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS/")).mkdirs();
      (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass"))).mkdirs();
      Object permissions = Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("permissions").replace('+', ' ')).getBytes("UTF8")));
      Properties info = (Properties)Common.readXMLObject(new ByteArrayInputStream(Common.url_decode(request.getProperty("info").replace('+', ' ')).getBytes("UTF8")));
      Common.writeXMLObject(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS.XML", permissions, "VFS");
      Common.writeXMLObject(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/INFO.XML", info, "INFO");
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
      (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder"))).renameTo(new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + fname));
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
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Common.recurseDelete(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder"), false);
      Common.recurseDelete(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass"), false);
      response = String.valueOf(response) + "Success.";
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
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Properties p = new Properties();
      Vector containedFiles = new Vector();
      Vector refFiles = new Vector();
      p.put("containedFiles", containedFiles);
      p.put("refFiles", refFiles);
      File[] files = (new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS/")).listFiles();
      if (files != null)
        for (int x = 0; x < files.length; x++) {
          if ((new File(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass") + "/" + files[x].getName())).exists()) {
            containedFiles.addElement(files[x].getName());
          } else {
            refFiles.addElement(files[x].getName());
          } 
        }  
      return Common.getXMLString(p, "temp_accounts_files", null);
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
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      Common.recurseDelete(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/VFS/" + request.getProperty("tempaccount_file"), false);
      Common.recurseDelete(String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass") + "/" + request.getProperty("tempaccount_file"), false);
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
      String s = handleInstance(request, site);
      if (s != null)
        return s; 
      File fileItem = new File(request.getProperty("tempaccount_file"));
      if (!fileItem.exists()) {
        response = String.valueOf(response) + "ERROR:File does not exist.";
      } else {
        String userHome = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "accounts/" + request.getProperty("tempaccount_folder") + "/";
        String userStorage = String.valueOf(ServerStatus.SG("temp_accounts_path")) + "storage/" + request.getProperty("tempaccount_user") + request.getProperty("tempaccount_pass") + "/";
        if (request.getProperty("tempaccount_reference", "false").equals("false"))
          Common.recurseCopyThreaded(fileItem.getPath(), String.valueOf(userStorage) + fileItem.getName() + (fileItem.isDirectory() ? "/" : ""), true, false); 
        Properties vItem = new Properties();
        if (request.getProperty("tempaccount_reference", "false").equals("false")) {
          vItem.put("url", (new File(String.valueOf(userStorage) + fileItem.getName())).toURI().toURL().toExternalForm());
        } else {
          vItem.put("url", fileItem.toURI().toURL().toExternalForm());
        } 
        vItem.put("type", fileItem.isDirectory() ? "dir" : "file");
        Vector v = new Vector();
        v.addElement(vItem);
        Common.writeXMLObject(String.valueOf(userHome) + "VFS/" + fileItem.getName(), v, "VFS");
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
}
