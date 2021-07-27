package crushftp.reports8;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class TopDownloadsUploads {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  public void generate(Properties stats, Properties params, StringBuffer sb, Properties status) {
    try {
      if (ServerStatus.BG("auto_fix_stats_sessions"))
        ServerStatus.thisObj.statTools.executeSql("update SESSIONS set end_time = start_time where end_time < ? and start_time < ?", new Object[] { (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse("2000-01-01 00:00:00"), new Date(System.currentTimeMillis() - 86400000L) }); 
      Common.setupReportDates(params, params.getProperty("show", ""), params.getProperty("startDate"), params.getProperty("endDate"));
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
      Vector usernames = (Vector)params.get("usernames");
      String download_sql = "SELECT count(t.rid) as \"count\", cast(avg(speed) as bigint) as \"averageSpeed\", path as \"path\", file_name as \"name\", url as \"url\", cast(transfer_size as bigint) as \"size\"\r FROM TRANSFERS t, SESSIONS s\r where s.rid = t.session_rid\r and DIRECTION = ?\r and t.start_time >= ? and t.start_time <= ?\r /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r group by path,file_name,url,transfer_size\r order by 1 desc\r limit ?\r";
      String upload_sql = "SELECT t.session_rid as \"session_rid\", t.rid as \"transfer_rid\", cast(avg(speed) as bigint) as \"averageSpeed\", path as \"path\", file_name as \"name\", url as \"url\", cast(transfer_size as bigint) as \"size\"\r FROM TRANSFERS t, SESSIONS s\r where s.rid = t.session_rid\r and DIRECTION = ?\r and t.start_time >= ? and t.start_time <= ?\r /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r group by t.session_rid, t.rid, path,file_name,url,transfer_size\r order by 1 desc\r limit ?\r";
      String tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/TopDownloadsUploads_download.sql");
      if (tmp != null) {
        download_sql = tmp;
      } else {
        Common.setFileText(download_sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/TopDownloadsUploads_download.sql");
      } 
      tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/TopDownloadsUploads_upload.sql");
      if (tmp != null) {
        upload_sql = tmp;
      } else {
        Common.setFileText(upload_sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/TopDownloadsUploads_upload.sql");
      } 
      download_sql = ReportTools.fixSqlUsernames(download_sql, usernames);
      upload_sql = ReportTools.fixSqlUsernames(upload_sql, usernames);
      if (ServerStatus.thisObj.statTools.mssql) {
        download_sql = "SELECT top " + Integer.parseInt(params.getProperty("downloadCount")) + download_sql.substring("SELECT".length(), download_sql.indexOf("limit ?"));
        upload_sql = "SELECT top " + Integer.parseInt(params.getProperty("uploadCount")) + upload_sql.substring("SELECT".length(), upload_sql.indexOf("limit ?"));
      } 
      Vector downloads = null;
      Vector uploads = null;
      if (ServerStatus.thisObj.statTools.mssql) {
        downloads = ServerStatus.thisObj.statTools.executeSqlQuery(download_sql, new Object[] { "DOWNLOAD", mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100 00:00:00")) }false, params);
        uploads = ServerStatus.thisObj.statTools.executeSqlQuery(upload_sql, new Object[] { "UPLOAD", mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100 00:00:00")) }false, params);
      } else {
        downloads = ServerStatus.thisObj.statTools.executeSqlQuery(download_sql, new Object[] { "DOWNLOAD", mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100 00:00:00")), Integer.valueOf(params.getProperty("downloadCount")) }false, params);
        uploads = ServerStatus.thisObj.statTools.executeSqlQuery(upload_sql, new Object[] { "UPLOAD", mmddyyyy.parse(params.getProperty("startDate", "1/1/2000 00:00:00")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100 00:00:00")), Integer.valueOf(params.getProperty("uploadCount")) }false, params);
      } 
      try {
        for (int i = 0; i < downloads.size(); i++) {
          Properties p = downloads.elementAt(i);
          p.put("path", Common.all_but_last(p.getProperty("path")));
        } 
      } catch (Exception e) {
        Log.log("REPORT", 0, e);
      } 
      try {
        for (int i = 0; i < uploads.size(); i++) {
          Properties p = uploads.elementAt(i);
          p.put("path", Common.all_but_last(p.getProperty("path")));
        } 
      } catch (Exception e) {
        Log.log("REPORT", 0, e);
      } 
      String meta_sql = "SELECT session_rid as \"session_rid\", transfer_rid as \"transfer_rid\", item_key as \"item_key\", item_value as \"item_value\"\r FROM META_INFO where session_rid in (%session_rids%)\r order by SESSION_RID, TRANSFER_RID, RID";
      tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/TopDownloadsUploads_meta.sql");
      if (tmp != null) {
        meta_sql = tmp;
      } else {
        Common.setFileText(meta_sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/TopDownloadsUploads_meta.sql");
      } 
      String session_rids = "";
      for (int x = 0; x < uploads.size(); x++) {
        Properties p = uploads.elementAt(x);
        session_rids = String.valueOf(session_rids) + p.getProperty("session_rid") + ",";
      } 
      if (session_rids.length() > 0)
        session_rids = session_rids.substring(0, session_rids.length() - 1); 
      meta_sql = Common.replace_str(meta_sql, "%session_rids%", session_rids);
      if (!session_rids.trim().equals("")) {
        Vector metas = ServerStatus.thisObj.statTools.executeSqlQuery(meta_sql, new Object[0], false, params);
        for (int i = 0; i < uploads.size(); i++) {
          Properties p = uploads.elementAt(i);
          for (int xx = 0; xx < metas.size(); xx++) {
            Properties pp = metas.elementAt(xx);
            if (pp.getProperty("session_rid").equals(p.getProperty("session_rid")) && pp.getProperty("transfer_rid").equals(p.getProperty("transfer_rid"))) {
              Properties metaInfo = (Properties)p.get("metaInfo");
              if (metaInfo == null) {
                metaInfo = new Properties();
                p.put("metaInfo", metaInfo);
              } 
              metaInfo.put(pp.getProperty("item_key"), pp.getProperty("item_value"));
            } 
          } 
        } 
      } 
      Properties results = new Properties();
      results.put("downloads", downloads);
      results.put("uploads", uploads);
      results.put("showPaths", params.getProperty("showPaths"));
      results.put("showURLs", params.getProperty("showURLs"));
      results.put("showFormInfo", params.getProperty("showFormInfo"));
      Common common_code = new Common();
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (downloads.size() > 0 || uploads.size() > 0)
        status.put("report_empty", "false"); 
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/TopDownloadsUploads.xsl"));
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
}
