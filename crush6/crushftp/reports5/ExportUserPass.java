package crushftp.reports5;

import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import crushftp.server.ServerStatus;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class ExportUserPass {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final ExportUserPass this$0;
    
    sorter(ExportUserPass this$0) {
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
      Vector usernames = (Vector)params.get("usernames");
      ReportTools.fixListUsernames(usernames);
      sorter cd1 = new sorter(this);
      Properties userDetails = new Properties();
      cd1.setObj(userDetails, "username");
      Vector sgs = (Vector)this.server_settings.get("server_groups");
      for (int i = 0; i < sgs.size(); i++) {
        String server = sgs.elementAt(i).toString();
        Vector current_user_group_listing = new Vector();
        UserTools.refreshUserList(server, current_user_group_listing);
        int pos = 0;
        for (int x = 0; x < current_user_group_listing.size(); x++) {
          pos++;
          try {
            status.put("status", String.valueOf((int)(pos / current_user_group_listing.size() * 100.0F)) + "%");
          } catch (Exception exception) {}
          String username = current_user_group_listing.elementAt(x).toString();
          boolean user_ok = false;
          for (int xx = 0; xx < usernames.size(); xx++) {
            if (Common.do_search(usernames.elementAt(xx).toString().toUpperCase(), username.toUpperCase(), false, 0))
              user_ok = true; 
          } 
          if (user_ok || usernames.size() <= 0) {
            Properties user = UserTools.ut.getUser(server, username, false);
            if (user == null) {
              Log.log("SERVER", 0, "Username not loadable, but folder existed in users:" + username);
            } else {
              String password = (new Common()).decode_pass(user.getProperty("password", ""));
              user.put("username", username);
              user.put("password", password);
              userDetails.put(username, user);
            } 
          } 
        } 
      } 
      Vector users = doSort(userDetails, cd1);
      Properties results = new Properties();
      results.put("users", users);
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      sb.append(ServerStatus.thisObj.common_code.getXMLString(results, "results", "WebInterface/Reports/ExportUserPass.xsl"));
    } catch (Exception e) {
      Log.log("REPORT", 1, e);
    } 
  }
  
  public Vector doSort(Properties item, sorter c) {
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
