package crushftp.reports8;

import com.crushftp.client.Common;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class ExpiringAccounts {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final ExpiringAccounts this$0;
    
    sorter(ExpiringAccounts this$0) {
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
            String max_logins = UserTools.ut.getEndUserProperty(server, username, "max_logins", "0");
            if (!max_logins.equals("-1"))
              try {
                Date expire_date1 = new Date(System.currentTimeMillis() + 51840000000L);
                Date expire_date2 = new Date(System.currentTimeMillis() + 51840000000L);
                Properties user_tmp = UserTools.ut.getUser(server, username, false);
                if (user_tmp.getProperty("expire_password", "").equals("true") && !user_tmp.getProperty("expire_password_when", "").equals("") && params.getProperty("expire_password", "true").equals("true")) {
                  SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
                  try {
                    expire_date1 = sdf.parse(user_tmp.getProperty("expire_password_when", ""));
                  } catch (ParseException e) {
                    sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
                    expire_date1 = sdf.parse(user_tmp.getProperty("expire_password_when", ""));
                  } 
                } 
                if (!user_tmp.getProperty("account_expire", "").equals("") && params.getProperty("expire_account", "true").equals("true")) {
                  SimpleDateFormat sdf = null;
                  if (user_tmp.getProperty("account_expire", "") != null && user_tmp.getProperty("account_expire", "").indexOf("/") >= 0 && user_tmp.getProperty("account_expire", "").indexOf(":") >= 0) {
                    sdf = new SimpleDateFormat("MM/dd/yy hh:mm aa", Locale.US);
                  } else if (user_tmp.getProperty("account_expire", "") != null && user_tmp.getProperty("account_expire", "").indexOf("/") >= 0) {
                    sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                  } else {
                    sdf = new SimpleDateFormat("MMddyyyyHHmm", Locale.US);
                  } 
                  expire_date2 = sdf.parse(user_tmp.getProperty("account_expire", ""));
                } 
                Calendar c = new GregorianCalendar();
                c.setTime(new Date());
                c.add(5, Integer.parseInt(params.getProperty("days", "")));
                Log.log("REPORT", 1, String.valueOf(username) + ":" + expire_date1 + "  less than  " + c.getTime());
                Log.log("REPORT", 1, String.valueOf(username) + ":" + expire_date2 + "  less than  " + c.getTime());
                if (expire_date1.getTime() < c.getTime().getTime() || expire_date2.getTime() < c.getTime().getTime()) {
                  Properties user = new Properties();
                  user.put("username", username);
                  if (params.getProperty("expire_account", "true").equals("true"))
                    user.put("account_expire", user_tmp.getProperty("account_expire", "")); 
                  if (params.getProperty("expire_password", "true").equals("true"))
                    user.put("password_expire", user_tmp.getProperty("expire_password_when", "")); 
                  userDetails.put(username, user);
                } 
              } catch (Exception e) {
                Log.log("REPORT", 2, e);
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
      if (userDetails.size() > 0)
        status.put("report_empty", "false"); 
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/ExpiringAccounts.xsl"));
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
