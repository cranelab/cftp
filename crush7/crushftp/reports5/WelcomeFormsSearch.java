package crushftp.reports5;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class WelcomeFormsSearch {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class counts implements Comparator {
    Properties allItems;
    
    String sort;
    
    final WelcomeFormsSearch this$0;
    
    counts(WelcomeFormsSearch this$0) {
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
      String sql = "select s.user_name as \"username\", s.rid as \"rid\", s.start_time as \"date\" \r FROM SESSIONS s\r where s.start_time >= ? and s.end_time <= ?\r /*START_USERNAMES*/and user_name in (%usernames%)/*END_USERNAMES*/\r";
      String tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WelcomeFormsSearch.sql");
      if (tmp != null) {
        sql = tmp;
      } else {
        Common.setFileText(sql, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WelcomeFormsSearch.sql");
      } 
      sql = ReportTools.fixSqlUsernames(sql, usernames);
      Vector summary = ServerStatus.thisObj.statTools.executeSqlQuery(sql, new Object[] { mmddyyyy.parse(params.getProperty("startDate", "1/1/2000")), mmddyyyy.parse(params.getProperty("endDate", "1/1/2100")) }false);
      String sql_meta = "SELECT item_key as \"item_key\", item_value as \"item_value\" \r FROM META_INFO \r WHERE SESSION_RID = ? and TRANSFER_RID = 0 \r";
      tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WelcomeFormsSearch_meta.sql");
      if (tmp != null) {
        sql_meta = tmp;
      } else {
        Common.setFileText(sql_meta, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WelcomeFormsSearch_meta.sql");
      } 
      String sql_transfers = "SELECT DIRECTION as \"direction\", cast(speed as bigint) as \"speed\", path as \"path\", file_name as \"name\", url as \"url\", cast(transfer_size as bigint) as \"size\"\r FROM TRANSFERS t\r where t.SESSION_RID = ? \r";
      tmp = Common.getFileText(String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WelcomeFormsSearch_transfers.sql");
      if (tmp != null) {
        sql_transfers = tmp;
      } else {
        Common.setFileText(sql_transfers, String.valueOf(System.getProperty("crushftp.web")) + "WebInterface/Reports/WelcomeFormsSearch_transfers.sql");
      } 
      Vector metas = new Vector();
      for (int x = 0; x < summary.size(); x++) {
        Properties p = summary.elementAt(x);
        Vector transferMetas = ServerStatus.thisObj.statTools.executeSqlQuery(sql_meta, new Object[] { p.getProperty("rid") }, false);
        Properties metaInfo = new Properties();
        for (int xxx = 0; xxx < transferMetas.size(); xxx++) {
          Properties ppp = transferMetas.elementAt(xxx);
          metaInfo.put(ppp.getProperty("item_key"), ppp.getProperty("item_value"));
        } 
        p.put("metaInfo", metaInfo);
        if (transferMetas.size() > 0) {
          Vector transfers = ServerStatus.thisObj.statTools.executeSqlQuery(sql_transfers, new Object[] { p.getProperty("rid") }, false);
          if (transfers != null)
            p.put("transfers", transfers); 
          metas.addElement(p);
        } 
      } 
      Properties results = new Properties();
      results.put("metas", metas);
      Common common_code = new Common();
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (metas.size() > 0)
        status.put("report_empty", "false"); 
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/WelcomeFormsSearch.xsl"));
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
}
