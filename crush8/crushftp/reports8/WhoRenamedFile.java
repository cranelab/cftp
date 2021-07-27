package crushftp.reports8;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class WhoRenamedFile {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final WhoRenamedFile this$0;
    
    sorter(WhoRenamedFile this$0) {
      this.this$0 = this$0;
      this.allItems = null;
      this.sort = null;
    }
    
    public void setObj(Properties allItems, String sort) {
      this.allItems = allItems;
      this.sort = sort;
    }
    
    public int compare(Object p1, Object p2) {
      String val1 = ((Properties)p1).getProperty(this.sort, "0");
      String val2 = ((Properties)p2).getProperty(this.sort, "0");
      try {
        if (Float.parseFloat(val1) > Float.parseFloat(val2))
          return -1; 
      } catch (Exception exception) {}
      try {
        if (Float.parseFloat(val1) < Float.parseFloat(val2))
          return 1; 
      } catch (Exception exception) {}
      return val1.compareTo(val2) * 1;
    }
  }
  
  public void generate(Properties stats, Properties params, StringBuffer sb, Properties status) {
    try {
      Common.setupReportDates(params, params.getProperty("show", ""), params.getProperty("startDate"), params.getProperty("endDate"));
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
      Vector usernames = (Vector)params.get("usernames");
      String sql = "SELECT user_name as \"username\"\r FROM SESSIONS\r where start_time >= ? and end_time <= ?\r /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r group by user_name\r order by user_name\r";
      String tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WhoRenamedFile.sql");
      if (tmp != null) {
        sql = tmp;
      } else {
        Common.setFileText(sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WhoRenamedFile.sql");
      } 
      sql = ReportTools.fixSqlUsernames(sql, usernames);
      Vector users = ServerStatus.thisObj.statTools.executeSqlQuery(sql, new Object[] { mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100 00:00:00")) }false, params);
      String sql_files = "SELECT user_name as \"username\", url as \"url\", path as \"path\", file_name as \"name\", t.start_time as \"date\"\r FROM TRANSFERS t, SESSIONS s\r where t.session_rid = s.rid\r and s.start_time >= ? and s.end_time <= ?\r and t.direction = 'RENAME'\r and upper(path) like ?\r and upper(file_name) like ?\r /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r group by user_name, url, path, file_name, t.start_time\r order by user_name, url, t.start_time\r";
      tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WhoRenamedFile_files.sql");
      if (tmp != null) {
        sql_files = tmp;
      } else {
        Common.setFileText(sql_files, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WhoRenamedFile_files.sql");
      } 
      sql_files = ReportTools.fixSqlUsernames(sql_files, usernames);
      String path = params.getProperty("path").toUpperCase();
      String filename = params.getProperty("filename").toUpperCase();
      String method = params.getProperty("searchFilename");
      if (method.equals("contains"))
        filename = "%" + filename + "%"; 
      if (method.equals("starts with"))
        filename = filename + "%"; 
      if (method.equals("ends with"))
        filename = "%" + filename; 
      if (method.equals("equals"))
        filename = filename; 
      method = params.getProperty("searchPath");
      if (method.equals("contains"))
        path = "%" + path + "%"; 
      if (method.equals("starts with"))
        path = path + "%"; 
      if (method.equals("ends with"))
        path = "%" + path; 
      if (method.equals("equals"))
        path = path; 
      Vector allFiles = ServerStatus.thisObj.statTools.executeSqlQuery(sql_files, new Object[] { mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100 00:00:00")), path, filename }false, params);
      Vector users_with_activity = new Vector();
      Properties users_with_activity_map = new Properties();
      Properties file_tracked = new Properties();
      for (int x = 0; x < users.size(); x++) {
        Properties p = users.elementAt(x);
        for (int xx = 0; xx < allFiles.size(); xx++) {
          Properties pp = allFiles.elementAt(xx);
          if (pp.getProperty("username", "").equals(p.getProperty("username", ""))) {
            if (!users_with_activity_map.containsKey(p.getProperty("username", ""))) {
              users_with_activity_map.put(p.getProperty("username", ""), "");
              users_with_activity.addElement(p);
            } 
            Vector filesDetail = (Vector)p.get("filesDetail");
            if (filesDetail == null)
              filesDetail = new Vector(); 
            p.put("filesDetail", filesDetail);
            Vector dates = new Vector();
            dates.addElement(pp.getProperty("date"));
            pp.put("dates", dates);
            int xxx = xx;
            while (++xxx < allFiles.size()) {
              Properties temp = allFiles.elementAt(xxx);
              if (!temp.getProperty("url").equals(pp.getProperty("url")))
                break; 
              dates.addElement(temp.getProperty("date"));
            } 
            if (!file_tracked.containsKey(String.valueOf(pp.getProperty("username")) + ":" + pp.getProperty("url"))) {
              file_tracked.put(String.valueOf(pp.getProperty("username")) + ":" + pp.getProperty("url"), "");
              filesDetail.addElement(pp);
            } 
          } 
        } 
      } 
      Properties results = new Properties();
      results.put("users", users_with_activity);
      Common common_code = new Common();
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (users.size() > 0)
        status.put("report_empty", "false"); 
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/WhoRenamedFile.xsl"));
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
}
