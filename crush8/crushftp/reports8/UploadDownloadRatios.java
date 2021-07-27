package crushftp.reports8;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class UploadDownloadRatios {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final UploadDownloadRatios this$0;
    
    sorter(UploadDownloadRatios this$0) {
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
      if (ServerStatus.BG("auto_fix_stats_sessions"))
        ServerStatus.thisObj.statTools.executeSql("update SESSIONS set end_time = start_time where end_time < ? and start_time < ?", new Object[] { (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse("2000-01-01 00:00:00"), new Date(System.currentTimeMillis() - 86400000L) }); 
      Common.setupReportDates(params, params.getProperty("show", ""), params.getProperty("startDate"), params.getProperty("endDate"));
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
      Vector usernames = (Vector)params.get("usernames");
      String sql = "select user_name as \"username\", sum(CASEWHEN(direction = 'UPLOAD',1,0)) as \"uploadCount\", sum(CASEWHEN(direction = 'DOWNLOAD',1,0)) as \"downloadCount\", cast(sum(CASEWHEN(direction = 'UPLOAD',transfer_size,0)) as bigint) as \"uploadBytes\", cast(sum(CASEWHEN(direction = 'DOWNLOAD', transfer_size,0)) as bigint) as \"downloadBytes\"\r FROM TRANSFERS t, SESSIONS s\r where s.rid = t.session_rid\r and s.start_time >= ? and s.end_time <= ?\r /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r group by user_name\r order by user_name\r";
      sql = ReportTools.fixMsSql(sql);
      String tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadDownloadRatios.sql");
      if (tmp != null) {
        sql = tmp;
      } else {
        Common.setFileText(sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadDownloadRatios.sql");
      } 
      sql = ReportTools.fixSqlUsernames(sql, usernames);
      Vector ratios = ServerStatus.thisObj.statTools.executeSqlQuery(sql, new Object[] { mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100 00:00:00")) }false, params);
      Properties results = new Properties();
      results.put("ratios", ratios);
      results.put("showBytes", params.getProperty("showBytes"));
      results.put("showCounts", params.getProperty("showCounts"));
      Common common_code = new Common();
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (ratios.size() > 0)
        status.put("report_empty", "false"); 
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/UploadDownloadRatios.xsl"));
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
}
