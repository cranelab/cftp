package crushftp.reports5;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class UserIPs {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  static Properties lookupCache = new Properties();
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final UserIPs this$0;
    
    sorter(UserIPs this$0) {
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
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
      Vector usernames = (Vector)params.get("usernames");
      String sql = "SELECT user_name as \"username\", count(distinct ip) as \"count\"\r FROM SESSIONS\r where start_time >= ? and end_time <= ?\r /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r group by user_name\r order by user_name\r";
      String tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UserIPs.sql");
      if (tmp != null) {
        sql = tmp;
      } else {
        Common.setFileText(sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UserIPs.sql");
      } 
      sql = ReportTools.fixSqlUsernames(sql, usernames);
      Vector ips = ServerStatus.thisObj.statTools.executeSqlQuery(sql, new Object[] { mmddyyyy.parse(params.getProperty("startDate", "1/1/2000")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100")) }false);
      String sql_ips = "SELECT user_name as \"username\", ip as \"ip\", count(ip) as \"count\"\r FROM SESSIONS\r where start_time >= ? and end_time <= ?\r /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r group by user_name, ip\r order by user_name\r";
      tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UserIPs_all.sql");
      if (tmp != null) {
        sql_ips = tmp;
      } else {
        Common.setFileText(sql_ips, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UserIPs_all.sql");
      } 
      sql_ips = ReportTools.fixSqlUsernames(sql_ips, usernames);
      Vector allIps = ServerStatus.thisObj.statTools.executeSqlQuery(sql_ips, new Object[] { mmddyyyy.parse(params.getProperty("startDate", "1/1/2000")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100")) }false);
      Properties uniques = new Properties();
      for (int x = 0; x < ips.size(); x++) {
        Properties p = ips.elementAt(x);
        for (int xx = 0; xx < allIps.size(); xx++) {
          Properties pp = allIps.elementAt(xx);
          if (params.getProperty("reverseDNS", "").equals("true"))
            try {
              String key = pp.getProperty("ip");
              if (lookupCache.getProperty(pp.getProperty("ip"), "").equals("")) {
                InetAddress addr = InetAddress.getByName(key);
                lookupCache.put(key, addr.getHostName());
              } 
              key = String.valueOf(key) + " (" + lookupCache.getProperty(key) + ")";
              p.put("ip", key);
            } catch (Exception exception) {} 
          if (pp.getProperty("username", "").equals(p.getProperty("username", "")))
            p.put(p.getProperty("ip"), pp.getProperty("count")); 
          uniques.put(pp.getProperty("ip"), "");
        } 
      } 
      Properties results = new Properties();
      results.put("unique_ip_count", (new StringBuffer(String.valueOf(uniques.size()))).toString());
      results.put("ips", ips);
      results.put("showIPs", params.getProperty("showIPs"));
      results.put("reverseDNS", params.getProperty("reverseDNS"));
      results.put("showCounts", params.getProperty("showCounts"));
      Common common_code = new Common();
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (ips.size() > 0)
        status.put("report_empty", "false"); 
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/UserIPs.xsl"));
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
}
