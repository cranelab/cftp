package crushftp.reports5;

import com.crushftp.client.Common;
import com.crushftp.client.File_B;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import crushftp.server.ServerStatus;
import crushftp.server.daemon.ServerBeat;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class ReportTools {
  public String writeReport(String filename, String dir, Properties status, Properties config, Object reportItem, Properties server_settings, Properties server_info, boolean export, String reportName, Properties params) {
    String s = "";
    try {
      StringBuffer sb = new StringBuffer();
      Method generate = reportItem.getClass().getMethod("generate", new Class[] { (new Properties()).getClass(), (new Properties()).getClass(), (new StringBuffer()).getClass(), (new Properties()).getClass() });
      generate.invoke(reportItem, new Object[] { server_info.get("stats"), config, sb, status });
      s = new String(sb.toString());
      if (status.getProperty("report_empty", "true").equals("true")) {
        status.put("report_text", s);
        s = "";
      } 
      if (status.getProperty("report_empty", "true").equals("true") && (filename == null || filename.toUpperCase().indexOf("EMAIL") >= 0))
        return ""; 
      String ext = ".html";
      if (export) {
        s = s.substring("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".length() + 2);
        s = s.replaceAll("<html>", "");
        s = s.replaceAll("</html>", "");
        s = s.replaceAll("\\r", "");
        s = s.replaceAll("\\n", "");
        s = s.replaceAll("\\t", "");
        s = s.replaceAll("<br></br>", "\r");
      } 
      if (export)
        ext = ".csv"; 
      (new File(dir)).mkdirs();
      if (filename == null)
        filename = String.valueOf(reportName) + ext; 
      if (!filename.toUpperCase().endsWith(".CSV") && !filename.toUpperCase().endsWith(".HTML"))
        filename = String.valueOf(filename) + ext; 
      RandomAccessFile out = new RandomAccessFile(String.valueOf(dir) + filename, "rw");
      out.setLength(0L);
      out.write(s.getBytes("UTF8"));
      out.close();
      Common.updateOSXInfo(String.valueOf(dir) + filename);
      if (params.getProperty("emailReport", "").equals("true")) {
        File_B[] attachments = new File_B[1];
        attachments[0] = new File_B(String.valueOf(dir) + filename);
        String emailResult = Common.send_mail(server_settings.getProperty("discovered_ip", "0.0.0.0"), replaceVars(params.getProperty("to", ""), params, config), replaceVars(params.getProperty("cc", ""), params, config), replaceVars(params.getProperty("bcc", ""), params, config), replaceVars(params.getProperty("from", ""), params, config), replaceVars(params.getProperty("subject", ""), params, config), replaceVars(params.getProperty("body", ""), params, config), server_settings.getProperty("smtp_server", ""), server_settings.getProperty("smtp_user", ""), server_settings.getProperty("smtp_pass", ""), server_settings.getProperty("smtp_ssl", "").equals("true"), server_settings.getProperty("smtp_report_html", "").equals("true"), attachments);
        if (emailResult.toUpperCase().indexOf("SUCCESS") < 0) {
          Log.log("REPORT", 0, String.valueOf(LOC.G("FAILURE:")) + " " + emailResult + "\r\n");
          Properties m = new Properties();
          m.put("result", emailResult);
          m.put("body", replaceVars(params.getProperty("body", ""), params, config));
          m.put("subject", replaceVars(params.getProperty("subject", ""), params, config));
          m.put("to", replaceVars(params.getProperty("to", ""), params, config));
          m.put("from", replaceVars(params.getProperty("from", ""), params, config));
          m.put("cc", replaceVars(params.getProperty("cc", ""), params, config));
          m.put("bcc", replaceVars(params.getProperty("bcc", ""), params, config));
          ServerStatus.thisObj.runAlerts("invalid_email", m, null, null);
        } 
      } 
    } catch (Exception ee) {
      Log.log("REPORT", 2, ee);
    } 
    return s;
  }
  
  public static void skipEmail(String reportName, Properties params) {
    if (reportName.equalsIgnoreCase("NewFiles")) {
      params.put("from", "REPORT_TEST_" + params.getProperty("from"));
      params.put("to", "REPORT_TEST_" + params.getProperty("to"));
    } 
  }
  
  public static void unSkipEmail(String reportName, Properties params) {
    if (reportName.equalsIgnoreCase("NewFiles"))
      if (params.getProperty("from").startsWith("REPORT_TEST_")) {
        params.put("from", params.getProperty("from").substring("REPORT_TEST_".length()));
        params.put("to", params.getProperty("to").substring("REPORT_TEST_".length()));
      }  
  }
  
  public Object getReportItem(String reportName, Properties server_settings, Properties server_info) {
    try {
      Class c = ServerStatus.clasLoader.loadClass("crushftp.reports5." + reportName);
      Constructor cons = c.getConstructor(null);
      Object o = cons.newInstance(null);
      Method generate = o.getClass().getMethod("init", new Class[] { (new Properties()).getClass(), (new Properties()).getClass() });
      generate.invoke(o, new Object[] { server_settings, server_info });
      return o;
    } catch (Exception ee) {
      Log.log("REPORT", 2, ee);
      return null;
    } 
  }
  
  public void runScheduledReports(Properties server_settings, Properties server_info) {
    if (!ServerBeat.current_master)
      if (ServerStatus.BG("single_report_scheduler_serverbeat"))
        return;  
    boolean ranAReport = false;
    SimpleDateFormat time = new SimpleDateFormat("hh:mm aa", Locale.US);
    if (server_settings.get("reportSchedules") == null)
      return; 
    Vector reportSchedules = (Vector)server_settings.get("reportSchedules");
    for (int x = 0; x < reportSchedules.size(); x++) {
      boolean runReport = false;
      try {
        Properties p = reportSchedules.elementAt(x);
        if (p.getProperty("nextRun", "123").equals(""))
          p.put("nextRun", "0"); 
        long nextRun = Long.parseLong(p.getProperty("nextRun", "0"));
        long newNextRun = (new Date()).getTime();
        if (p.getProperty("reportType", "").equals("minutely") && (new Date()).getTime() > nextRun) {
          runReport = true;
          Calendar c = new GregorianCalendar();
          c.setTime(new Date());
          c.add(12, Integer.parseInt(p.getProperty("minutelyAmount")));
          newNextRun = c.getTime().getTime();
        } else if (time.format(new Date()).equals(time.format(time.parse(p.getProperty("reportTime")))) && (new Date()).getTime() > nextRun) {
          if (p.getProperty("reportType", "").equals("daily")) {
            while (time.format(new Date(newNextRun)).equals(time.format(new Date())))
              newNextRun += 1000L; 
            runReport = true;
          } else if (p.getProperty("reportType", "").equals("weekly")) {
            String day = "";
            String today_date = (new SimpleDateFormat("EEE", Locale.US)).format(new Date()).toUpperCase();
            if (today_date.equals("SUN"))
              day = "(1)"; 
            if (today_date.equals("MON"))
              day = "(2)"; 
            if (today_date.equals("TUE"))
              day = "(3)"; 
            if (today_date.equals("WED"))
              day = "(4)"; 
            if (today_date.equals("THU"))
              day = "(5)"; 
            if (today_date.equals("FRI"))
              day = "(6)"; 
            if (today_date.equals("SAT"))
              day = "(7)"; 
            if (p.getProperty("weekDays", "").indexOf(day) >= 0) {
              if (p.getProperty("weekDays", "").indexOf(day) == p.getProperty("weekDays", "").length() - day.length()) {
                Calendar c = new GregorianCalendar();
                c.setTime(new Date());
                while (c.get(7) != 1)
                  c.add(5, -1); 
                c.add(5, 7 * Integer.parseInt(p.getProperty("weeklyAmount")));
                c.add(10, -1);
                newNextRun = c.getTime().getTime();
              } 
              runReport = true;
            } 
          } else if (p.getProperty("reportType", "").equals("monthly")) {
            SimpleDateFormat d = new SimpleDateFormat("d", Locale.US);
            SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
            String day1 = "(" + d.format(new Date()) + ")";
            String day2 = "(" + dd.format(new Date()) + ")";
            if (p.getProperty("monthDays", "").indexOf(day1) >= 0 || p.getProperty("monthDays", "").indexOf(day2) >= 0) {
              if (p.getProperty("monthDays", "").indexOf(day1) == p.getProperty("monthDays", "").length() - day1.length() || p.getProperty("monthDays", "").indexOf(day2) == p.getProperty("monthDays", "").length() - day2.length()) {
                Calendar c = new GregorianCalendar();
                c.setTime(new Date());
                while (c.get(5) != 1)
                  c.add(5, -1); 
                c.add(2, Integer.parseInt(p.getProperty("monthlyAmount")));
                c.add(10, -1);
                newNextRun = c.getTime().getTime();
              } 
              runReport = true;
            } 
          } 
        } 
        if (runReport || nextRun == -1L) {
          p.put("nextRun", (new StringBuffer(String.valueOf(newNextRun))).toString());
          ranAReport = true;
          Enumeration keys = p.keys();
          Properties config = (Properties)p.get("config");
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if (p.get(key) instanceof String && !key.startsWith("schedule_"))
              config.put("schedule_" + key, p.get(key)); 
          } 
          Thread runIt = new Thread(new ReportTools$1$runner(this, server_settings, server_info, p));
          runIt.setPriority(1);
          runIt.setName("report:" + p.getProperty("scheduleName") + ":" + new Date());
          runIt.start();
          Log.log("REPORT", 0, LOC.G("Ran Scheduled Report") + ":" + p.getProperty("scheduleName") + ":" + new Date());
          Log.log("REPORT", 1, "Report Config:" + p.toString());
          try {
            throw new Exception("Who called?");
          } catch (Exception e) {
            Log.log("REPORT", 2, e);
          } 
        } 
      } catch (Exception e) {
        Log.log("REPORT", 1, e);
      } 
    } 
    if (ranAReport)
      ServerStatus.thisObj.save_server_settings(false); 
  }
  
  public static String fixSqlUsernames(String sql, Vector usernames) {
    String userNamesStr = "";
    Properties groupsAll = new Properties();
    if (usernames.toString().indexOf("...") >= 0) {
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
    for (int xx = 0; xx < usernames.size(); xx++) {
      String username = usernames.elementAt(xx).toString().toUpperCase().trim();
      if (!username.equals("*"))
        if (username.startsWith("...")) {
          Vector usernames2 = (Vector)groupsAll.get(username);
          for (int xxx = 0; xxx < usernames2.size(); xxx++) {
            String username2 = usernames2.elementAt(xxx).toString().toUpperCase().trim();
            userNamesStr = String.valueOf(userNamesStr) + "'" + username2.replace('\'', '_') + "',";
          } 
        } else {
          userNamesStr = String.valueOf(userNamesStr) + "'" + username.replace('\'', '_') + "',";
        }  
    } 
    if (userNamesStr.length() > 0 && sql.indexOf("%usernames%") >= 0) {
      userNamesStr = userNamesStr.substring(0, userNamesStr.length() - 1);
      sql = Common.replace_str(sql, "%usernames%", userNamesStr);
      sql = Common.replace_str(sql, "/*START_USERNAMES*/", "");
      sql = Common.replace_str(sql, "/*END_USERNAMES*/", "");
      sql = Common.replace_str(sql, "and user_name in ", "and upper(user_name) in ");
    } else if (sql.indexOf("/*START_USERNAMES*/") >= 0) {
      sql = String.valueOf(sql.substring(0, sql.indexOf("/*START_USERNAMES*/")).trim()) + " " + sql.substring(sql.indexOf("/*END_USERNAMES*/") + "/*END_USERNAMES*/".length()).trim();
    } 
    if (ServerStatus.thisObj.statTools.mysql)
      sql = Common.replace_str(sql, "CASEWHEN", "IF"); 
    return sql;
  }
  
  public static void fixListUsernames(Vector usernamesReal) {
    Vector usernames = (Vector)usernamesReal.clone();
    usernamesReal.removeAllElements();
    Properties groupsAll = new Properties();
    if (usernames.toString().indexOf("...") >= 0) {
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
    for (int xx = 0; xx < usernames.size(); xx++) {
      String username = usernames.elementAt(xx).toString().toUpperCase().trim();
      if (username.startsWith("...")) {
        Vector usernames2 = (Vector)groupsAll.get(username);
        for (int xxx = 0; xxx < usernames2.size(); xxx++)
          usernamesReal.addElement(usernames2.elementAt(xxx).toString().toUpperCase().trim()); 
      } else {
        usernamesReal.addElement(username);
      } 
    } 
  }
  
  public String replaceVars(String filename, Properties params, Properties config) {
    SimpleDateFormat mm = new SimpleDateFormat("mm", Locale.US);
    SimpleDateFormat ss = new SimpleDateFormat("ss", Locale.US);
    SimpleDateFormat HH = new SimpleDateFormat("HH", Locale.US);
    SimpleDateFormat hh = new SimpleDateFormat("hh", Locale.US);
    SimpleDateFormat aa = new SimpleDateFormat("aa", Locale.US);
    SimpleDateFormat MM = new SimpleDateFormat("MM", Locale.US);
    SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
    SimpleDateFormat yy = new SimpleDateFormat("yy", Locale.US);
    SimpleDateFormat yyyy = new SimpleDateFormat("yyyy", Locale.US);
    filename = Common.replace_str(filename, "%name%", config.getProperty("reportName"));
    filename = Common.replace_str(filename, "%schedule%", params.getProperty("scheduleName"));
    filename = Common.replace_str(filename, "%mm%", MM.format(new Date()));
    filename = Common.replace_str(filename, "%dd%", dd.format(new Date()));
    filename = Common.replace_str(filename, "%yy%", yy.format(new Date()));
    filename = Common.replace_str(filename, "%yyyy%", yyyy.format(new Date()));
    filename = Common.replace_str(filename, "{name}", config.getProperty("reportName"));
    filename = Common.replace_str(filename, "{schedule}", params.getProperty("scheduleName"));
    filename = Common.replace_str(filename, "{mm}", mm.format(new Date()));
    filename = Common.replace_str(filename, "{ss}", ss.format(new Date()));
    filename = Common.replace_str(filename, "{HH}", HH.format(new Date()));
    filename = Common.replace_str(filename, "{hh}", hh.format(new Date()));
    filename = Common.replace_str(filename, "{aa}", aa.format(new Date()));
    filename = Common.replace_str(filename, "{MM}", MM.format(new Date()));
    filename = Common.replace_str(filename, "{dd}", dd.format(new Date()));
    filename = Common.replace_str(filename, "{yy}", yy.format(new Date()));
    filename = Common.replace_str(filename, "{yyyy}", yyyy.format(new Date()));
    return filename;
  }
  
  public static String fixMsSql(String sql) {
    if (ServerStatus.thisObj.statTools.mssql) {
      sql = Common.replace_str(sql, "CASEWHEN(direction = 'UPLOAD',1,0)", "CASE WHEN(direction = 'UPLOAD') then 1 else 0 end");
      sql = Common.replace_str(sql, "CASEWHEN(direction = 'DOWNLOAD',1,0)", "CASE WHEN(direction = 'DOWNLOAD') then 1 else 0 end");
      sql = Common.replace_str(sql, "CASEWHEN(direction = 'UPLOAD',transfer_size,0)", "CASE WHEN(direction = 'UPLOAD') then transfer_size else 0 end");
      sql = Common.replace_str(sql, "CASEWHEN(direction = 'UPLOAD', transfer_size,0)", "CASE WHEN(direction = 'UPLOAD') then transfer_size else 0 end");
      sql = Common.replace_str(sql, "CASEWHEN(direction = 'DOWNLOAD', transfer_size,0)", "CASE WHEN(direction = 'DOWNLOAD') then transfer_size else 0 end");
      sql = Common.replace_str(sql, "CASEWHEN(direction = 'DOWNLOAD',transfer_size,0)", "CASE WHEN(direction = 'DOWNLOAD') then transfer_size else 0 end");
    } 
    return sql;
  }
}
