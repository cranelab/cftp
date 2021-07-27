package crushftp.reports5;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Properties;
import java.util.Vector;

public class AccountActivitySummary {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  static Properties lookupCache = new Properties();
  
  SimpleDateFormat sdf;
  
  SimpleDateFormat sdfyy;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final AccountActivitySummary this$0;
    
    sorter(AccountActivitySummary this$0) {
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
  
  public AccountActivitySummary() {
    this.sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
    this.sdfyy = new SimpleDateFormat("MM/dd/yy hh:mm:ss aa");
  }
  
  public void generate(Properties stats, Properties params, StringBuffer sb, Properties status) {
    try {
      Common.setupReportDates(params, params.getProperty("show", ""), params.getProperty("startDate"), params.getProperty("endDate"));
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy");
      Vector usernames = (Vector)params.get("usernames");
      String datediff = "datediff('ss',s.start_time,s.end_time) * 1000";
      String datediff2 = datediff;
      if (ServerStatus.thisObj.statTools.mysql) {
        datediff = "TIME_TO_SEC(timediff(s.end_time,s.start_time))*1000";
      } else if (ServerStatus.thisObj.statTools.derby) {
        datediff = "{fn TIMESTAMPDIFF(SQL_TSI_SECOND,s.end_time,s.start_time)} * 1000";
      } 
      String sql = "select user_name as \"username\", ip as \"ip\", s.start_time as \"start\",s.end_time as \"end\", " + datediff + " as \"durationRaw\", sum(CASEWHEN(direction = 'UPLOAD',1,0)) as \"uploadCount\", sum(CASEWHEN(direction = 'DOWNLOAD',1,0)) as \"downloadCount\", cast(sum(CASEWHEN(direction = 'UPLOAD',transfer_size,0)) as bigint) as \"uploadBytes\", cast(sum(CASEWHEN(direction = 'DOWNLOAD', transfer_size,0)) as bigint) as \"downloadBytes\"\r" + " FROM TRANSFERS t, SESSIONS s\r" + " where s.rid = t.session_rid\r" + " and s.start_time >= ? and s.end_time <= ?\r" + " /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r" + " group by user_name, ip, s.start_time, s.end_time, s.rid\r" + " order by user_name\r";
      String tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/AccountActivitySummary.sql");
      if (tmp != null) {
        sql = tmp;
      } else {
        Common.setFileText(sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/AccountActivitySummary.sql");
      } 
      sql = ReportTools.fixSqlUsernames(sql, usernames);
      sql = Common.replace_str(sql, datediff2, datediff);
      Vector summary = ServerStatus.thisObj.statTools.executeSqlQuery(sql, new Object[] { mmddyyyy.parse(params.getProperty("startDate", "1/1/2000")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100")) }false);
      Properties results = new Properties();
      for (int x = 0; x < summary.size(); x++) {
        Properties p = summary.elementAt(x);
        long secs = (long)Float.parseFloat(p.getProperty("durationRaw", "0")) / 1000L;
        if (secs < 0L) {
          secs = 0L;
          p.put("end", p.getProperty("start"));
        } 
        results.put("downloadCount", (new StringBuffer(String.valueOf(Long.parseLong(results.getProperty("downloadCount", "0")) + Long.parseLong(p.getProperty("downloadCount", "0"))))).toString());
        results.put("uploadCount", (new StringBuffer(String.valueOf(Long.parseLong(results.getProperty("uploadCount", "0")) + Long.parseLong(p.getProperty("uploadCount", "0"))))).toString());
        results.put("downloadBytes", (new StringBuffer(String.valueOf((long)(Float.parseFloat(results.getProperty("downloadBytes", "0")) + (float)(long)Float.parseFloat(p.getProperty("downloadBytes", "0")))))).toString());
        results.put("uploadBytes", (new StringBuffer(String.valueOf((long)(Float.parseFloat(results.getProperty("uploadBytes", "0")) + (float)(long)Float.parseFloat(p.getProperty("uploadBytes", "0")))))).toString());
        results.put("durationRaw", (new StringBuffer(String.valueOf((long)(Float.parseFloat(results.getProperty("durationRaw", "0")) + (float)(long)Float.parseFloat(p.getProperty("durationRaw", "0")))))).toString());
        p.put("duration", (new StringBuffer(String.valueOf(Common.format_time_pretty(secs)))).toString());
        if (params.getProperty("reverseDNS", "").equals("true"))
          try {
            String key = p.getProperty("ip");
            if (lookupCache.getProperty(p.getProperty("ip"), "").equals("")) {
              InetAddress addr = InetAddress.getByName(key);
              lookupCache.put(key, addr.getHostName());
            } 
            key = String.valueOf(key) + " (" + lookupCache.getProperty(key) + ")";
            p.put("ip", key);
          } catch (Exception exception) {} 
      } 
      results.put("duration", (new StringBuffer(String.valueOf(Common.format_time_pretty(Long.parseLong(results.getProperty("durationRaw", "0")) / 1000L)))).toString());
      results.put("userCount", (new StringBuffer(String.valueOf(summary.size()))).toString());
      results.put("summary", summary);
      results.put("reverseDNS", params.getProperty("reverseDNS"));
      Common common_code = new Common();
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (summary.size() > 0)
        sb.append(common_code.getXMLString(results, "results", "WebInterface/Reports/AccountActivitySummary.xsl")); 
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
}
