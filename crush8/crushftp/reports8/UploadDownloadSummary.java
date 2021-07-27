package crushftp.reports8;

import com.crushftp.client.VRL;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class UploadDownloadSummary {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  SimpleDateFormat sdf;
  
  SimpleDateFormat sdfyy;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final UploadDownloadSummary this$0;
    
    sorter(UploadDownloadSummary this$0) {
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
  
  public UploadDownloadSummary() {
    this.sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
    this.sdfyy = new SimpleDateFormat("MM/dd/yy hh:mm:ss aa", Locale.US);
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
      String tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadDownloadSummary_summary.sql");
      if (tmp != null) {
        sql = tmp;
      } else {
        Common.setFileText(sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadDownloadSummary_summary.sql");
      } 
      sql = ReportTools.fixSqlUsernames(sql, usernames);
      Vector summary = ServerStatus.thisObj.statTools.executeSqlQuery(sql, new Object[] { mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/21 00 00:00:00")) }false, params);
      String sql_transfers = "SELECT user_name as \"username\", s.ip as \"ip\", t.start_time as \"date\", path as \"path\", file_name as \"name\", url as \"url\", cast(transfer_size as bigint) as \"size\", cast(speed as bigint)*1024 as \"speed\"\r FROM TRANSFERS t, SESSIONS s\r where s.rid = t.session_rid\r and DIRECTION = ?\r and t.start_time >= ? and t.start_time <= ?\r order by t.start_time\r";
      tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadDownloadSummary_transfers.sql");
      if (tmp != null) {
        sql_transfers = tmp;
      } else {
        Common.setFileText(sql_transfers, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadDownloadSummary_transfers.sql");
      } 
      Vector allDownloads = new Vector();
      Vector allUploads = new Vector();
      if (params.getProperty("showDownloads").equals("true"))
        allDownloads = ServerStatus.thisObj.statTools.executeSqlQuery(sql_transfers, new Object[] { "DOWNLOAD", mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100 00:00:00")) }false, params); 
      if (params.getProperty("showUploads").equals("true"))
        allUploads = ServerStatus.thisObj.statTools.executeSqlQuery(sql_transfers, new Object[] { "UPLOAD", mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100 00:00:00")) }false, params); 
      Properties results = new Properties();
      for (int x = 0; x < summary.size(); x++) {
        Properties p = summary.elementAt(x);
        results.put("downloadCount", (new StringBuffer(String.valueOf(Long.parseLong(results.getProperty("downloadCount", "0")) + Long.parseLong(p.getProperty("downloadCount", "0"))))).toString());
        results.put("uploadCount", (new StringBuffer(String.valueOf(Long.parseLong(results.getProperty("uploadCount", "0")) + Long.parseLong(p.getProperty("uploadCount", "0"))))).toString());
        results.put("downloadBytes", (new StringBuffer(String.valueOf((long)(Float.parseFloat(results.getProperty("downloadBytes", "0")) + (float)(long)Float.parseFloat(p.getProperty("downloadBytes", "0")))))).toString());
        results.put("uploadBytes", (new StringBuffer(String.valueOf((long)(Float.parseFloat(results.getProperty("uploadBytes", "0")) + (float)(long)Float.parseFloat(p.getProperty("uploadBytes", "0")))))).toString());
        Vector downloads = new Vector();
        for (int xx = 0; xx < allDownloads.size(); xx++) {
          Properties pp = allDownloads.elementAt(xx);
          if (pp.getProperty("username", "").equals(p.getProperty("username", "")))
            downloads.addElement(VRL.safe(pp)); 
        } 
        Vector uploads = new Vector();
        for (int i = 0; i < allUploads.size(); i++) {
          Properties pp = allUploads.elementAt(i);
          if (pp.getProperty("username", "").equals(p.getProperty("username", "")))
            uploads.addElement(VRL.safe(pp)); 
        } 
        p.put("downloads", downloads);
        p.put("uploads", uploads);
      } 
      results.put("userCount", (new StringBuffer(String.valueOf(summary.size()))).toString());
      results.put("showUploads", params.getProperty("showUploads"));
      results.put("showDownloads", params.getProperty("showDownloads"));
      results.put("showFiles", params.getProperty("showFiles"));
      results.put("showPaths", params.getProperty("showPaths"));
      results.put("showIPs", params.getProperty("showIPs"));
      results.put("showDates", params.getProperty("showDates"));
      results.put("showSizes", params.getProperty("showSizes"));
      results.put("summary", summary);
      Common common_code = new Common();
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (summary.size() > 0)
        status.put("report_empty", "false"); 
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/UploadDownloadSummary.xsl"));
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
}
