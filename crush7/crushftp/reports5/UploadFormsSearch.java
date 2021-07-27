package crushftp.reports5;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class UploadFormsSearch {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class counts implements Comparator {
    Properties allItems;
    
    String sort;
    
    final UploadFormsSearch this$0;
    
    counts(UploadFormsSearch this$0) {
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
        if (Integer.parseInt(val1) > Integer.parseInt(val2))
          return -1; 
      } catch (Exception exception) {}
      try {
        if (Integer.parseInt(val1) < Integer.parseInt(val2))
          return 1; 
      } catch (Exception exception) {}
      return val1.compareTo(val2) * -1;
    }
  }
  
  public void generate(Properties stats, Properties params, StringBuffer sb, Properties status) {
    try {
      Common.setupReportDates(params, params.getProperty("show", ""), params.getProperty("startDate"), params.getProperty("endDate"));
      SimpleDateFormat mmddyyyy = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
      Vector usernames = (Vector)params.get("usernames");
      Properties metaSearch = (Properties)params.clone();
      Enumeration keys = metaSearch.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (!key.toUpperCase().startsWith("META_"))
          metaSearch.remove(key); 
      } 
      String sql = "select s.rid as \"rid\" \r FROM TRANSFERS t, SESSIONS s\r where s.rid = t.session_rid\r and s.start_time >= ? and s.end_time <= ?\r /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r";
      String tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadFormsSearch.sql");
      if (tmp != null) {
        sql = tmp;
      } else {
        Common.setFileText(sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadFormsSearch.sql");
      } 
      sql = ReportTools.fixSqlUsernames(sql, usernames);
      Vector summary = ServerStatus.thisObj.statTools.executeSqlQuery(sql, new Object[] { mmddyyyy.parse(params.getProperty("startDate", "1/1/2000")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100")) }false);
      String sql_transfers = "SELECT t.rid as \"rid\", session_rid as \"session_rid\", cast(speed as bigint) as \"speed\", path as \"path\", file_name as \"name\", url as \"url\", cast(transfer_size as bigint) as \"size\"\r FROM TRANSFERS t, SESSIONS s\r where s.rid = t.session_rid\r and DIRECTION = ?\r and t.start_time >= ? and t.start_time <= ?\r order by t.start_time\r";
      tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadFormsSearch_transfers.sql");
      if (tmp != null) {
        sql_transfers = tmp;
      } else {
        Common.setFileText(sql_transfers, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadFormsSearch_transfers.sql");
      } 
      Vector allUploads = ServerStatus.thisObj.statTools.executeSqlQuery(sql_transfers, new Object[] { "UPLOAD", mmddyyyy.parse(params.getProperty("startDate", "1/1/2000")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100")) }false);
      String sql_meta = "SELECT item_key as \"item_key\", item_value as \"item_value\" \r FROM META_INFO \r WHERE TRANSFER_RID = ? \r";
      tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadFormsSearch_meta.sql");
      if (tmp != null) {
        sql_meta = tmp;
      } else {
        Common.setFileText(sql_meta, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/UploadFormsSearch_meta.sql");
      } 
      Vector uploads = new Vector();
      for (int x = 0; x < summary.size(); x++) {
        Properties sessions = summary.elementAt(x);
        for (int xx = 0; xx < allUploads.size(); xx++) {
          Properties transfer = allUploads.elementAt(xx);
          if (transfer.getProperty("session_rid", "").equals(sessions.getProperty("rid", ""))) {
            Vector transferMetas = ServerStatus.thisObj.statTools.executeSqlQuery(sql_meta, new Object[] { transfer.getProperty("rid") }, false);
            Properties metaInfo = new Properties();
            for (int xxx = 0; transferMetas != null && xxx < transferMetas.size(); xxx++) {
              Properties ppp = transferMetas.elementAt(xxx);
              metaInfo.put(ppp.getProperty("item_key"), ppp.getProperty("item_value"));
            } 
            Enumeration e_metas = metaSearch.keys();
            boolean ok = true;
            while (e_metas.hasMoreElements()) {
              String meta_key = e_metas.nextElement().toString();
              String meta_val = metaSearch.getProperty(meta_key, "");
              if (meta_key.endsWith("___type") || meta_val.trim().equals(""))
                continue; 
              String type = metaSearch.getProperty(String.valueOf(meta_key) + "___type", "");
              if (type.equals("text") || type.equals("textarea")) {
                if (metaInfo.getProperty(meta_key.substring("meta_".length()), "").toUpperCase().indexOf(meta_val.toUpperCase()) < 0)
                  ok = false; 
                continue;
              } 
              if (!metaInfo.getProperty(meta_key.substring("meta_".length()), "").toUpperCase().equals(meta_val.toUpperCase()))
                ok = false; 
            } 
            transfer.put("metaInfo", metaInfo);
            if (ok)
              uploads.addElement(transfer); 
          } 
        } 
      } 
      Properties results = new Properties();
      results.put("uploads", uploads);
      results.put("showPaths", params.getProperty("showPaths"));
      results.put("showURLs", params.getProperty("showURLs"));
      results.put("showFormInfo", params.getProperty("showFormInfo"));
      int maxUp = Integer.parseInt(params.getProperty("uploadCount"));
      while (uploads.size() > maxUp)
        uploads.removeElementAt(uploads.size() - 1); 
      Common common_code = new Common();
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (uploads.size() > 0)
        status.put("report_empty", "false"); 
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/UploadFormsSearch.xsl"));
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
  
  public Vector doSort(Properties item, counts c) {
    Enumeration e = item.keys();
    Vector v = new Vector();
    while (e.hasMoreElements()) {
      String key = e.nextElement().toString();
      v.addElement((Properties)item.get(key));
    } 
    Object[] objs = v.toArray();
    Arrays.sort(objs, c);
    v.removeAllElements();
    for (int x = 0; x < objs.length; x++)
      v.addElement(objs[x]); 
    return v;
  }
  
  public void updateItems(Properties p, Properties details, String username, SimpleDateFormat sdf, long start, long end) {
    long date = Long.parseLong(p.getProperty("date"));
    if (date < start || date > end)
      return; 
    if (details.get(p.getProperty("url")) == null) {
      Properties pp = new Properties();
      pp.putAll(p);
      pp.remove("speed");
      pp.remove("ip");
      pp.remove("date");
      pp.put("dates", new Vector());
      pp.put("speeds", new Vector());
      pp.put("users", new Vector());
      pp.put("ips", new Vector());
      details.put(p.getProperty("url"), pp);
    } 
    Properties itemDetails = (Properties)details.get(p.getProperty("url"));
    int i = Integer.parseInt(itemDetails.getProperty("count", "0"));
    i++;
    itemDetails.put("count", (new StringBuffer(String.valueOf(i))).toString());
    long speed = Long.parseLong(p.getProperty("speed"));
    long sumSpeed = Long.parseLong(itemDetails.getProperty("sumSpeed", "0")) + speed;
    itemDetails.put("sumSpeed", (new StringBuffer(String.valueOf(sumSpeed))).toString());
    if (((Vector)itemDetails.get("speeds")).size() == 0) {
      itemDetails.put("averageSpeed", "0");
    } else {
      itemDetails.put("averageSpeed", (new StringBuffer(String.valueOf(sumSpeed / ((Vector)itemDetails.get("speeds")).size()))).toString());
    } 
    ((Vector)itemDetails.get("speeds")).addElement(p.getProperty("speed"));
    if (((Vector)itemDetails.get("ips")).indexOf(p.getProperty("ip")) < 0)
      ((Vector)itemDetails.get("ips")).addElement(p.getProperty("ip")); 
    if (((Vector)itemDetails.get("users")).indexOf(username) < 0)
      ((Vector)itemDetails.get("users")).addElement(username); 
    String dateStr = sdf.format(new Date(date));
    if (((Vector)itemDetails.get("dates")).indexOf(dateStr) < 0)
      ((Vector)itemDetails.get("dates")).addElement(dateStr); 
  }
}
