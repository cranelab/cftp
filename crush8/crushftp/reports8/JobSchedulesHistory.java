package crushftp.reports8;

import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.AdminControls;
import crushftp.server.daemon.ServerBeat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class JobSchedulesHistory {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final JobSchedulesHistory this$0;
    
    sorter(JobSchedulesHistory this$0) {
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
      sorter cd1 = new sorter(this);
      cd1.setObj(new Properties(), "scheduleName");
      Vector jobs2 = new Vector();
      if (!ServerBeat.current_master)
        return; 
      Properties request = (Properties)params.clone();
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
      request.put("start_time", (new StringBuffer(String.valueOf(sdf.parse(request.getProperty("startDate")).getTime()))).toString());
      request.put("end_time", (new StringBuffer(String.valueOf(sdf.parse(request.getProperty("endDate")).getTime()))).toString());
      jobs2 = AdminControls.getJobsSummary(request, "");
      jobs2 = doSort(jobs2, cd1);
      Properties results = new Properties();
      results.put("jobs", jobs2);
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      status.put("report_empty", "false");
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/JobSchedulesHistory.xsl"));
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
  
  public Vector doSort(Vector v, sorter c) {
    Object[] objs = v.toArray();
    Arrays.sort(objs, c);
    v.removeAllElements();
    for (int x = 0; x < objs.length; x++)
      v.addElement(objs[x]); 
    return v;
  }
}
