package crushftp.reports8;

import com.crushftp.client.Common;
import com.crushftp.client.VRL;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.UserTools;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class UserUsage {
  Properties server_info = null;
  
  Properties server_settings = null;
  
  public void init(Properties server_settings, Properties server_info) {
    this.server_settings = server_settings;
    this.server_info = server_info;
  }
  
  class sorter implements Comparator {
    Properties allItems;
    
    String sort;
    
    final UserUsage this$0;
    
    sorter(UserUsage this$0) {
      this.this$0 = this$0;
      this.allItems = null;
      this.sort = null;
    }
    
    public void setObj(Properties allItems, String sort) {
      this.allItems = allItems;
      this.sort = sort;
    }
    
    public int compare(Object p1, Object p2) {
      String val1 = ((Properties)p1).getProperty(this.sort, "0").toUpperCase();
      String val2 = ((Properties)p2).getProperty(this.sort, "0").toUpperCase();
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
      try {
        status.put("status", "100%");
      } catch (Exception exception) {}
      Vector sgs = (Vector)this.server_settings.get("server_groups");
      for (int i = 0; i < sgs.size(); i++) {
        String server = sgs.elementAt(i).toString();
        Vector current_user_group_listing = new Vector();
        Properties groups = UserTools.getGroups(server);
        UserTools.refreshUserList(server, current_user_group_listing);
        SimpleDateFormat MMddyyyyHHmmss = new SimpleDateFormat("MMddyyyyHHmmss");
        SimpleDateFormat MMddyyyyHHmmss2 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        for (int x = 0; x < current_user_group_listing.size(); x++) {
          String username = current_user_group_listing.elementAt(x).toString();
          boolean user_ok = false;
          for (int xx = 0; xx < usernames.size(); xx++) {
            if (Common.do_search(usernames.elementAt(xx).toString().toUpperCase(), username.toUpperCase(), false, 0))
              user_ok = true; 
          } 
          if (user_ok || usernames.size() <= 0) {
            Properties tempUser = UserTools.ut.getUser(server, username, true);
            if (tempUser == null)
              tempUser = new Properties(); 
            String enabled = UserTools.ut.getEndUserProperty(server, username, "max_logins", "0");
            VFS uVFS = UserTools.ut.getVFS(server, username);
            Vector linked_vfs = (Vector)tempUser.get("linked_vfs");
            if (linked_vfs == null)
              linked_vfs = new Vector(); 
            for (int j = 0; j < linked_vfs.size(); j++) {
              if (!linked_vfs.elementAt(j).toString().trim().equals(""))
                try {
                  VFS tempVFS = UserTools.ut.getVFS(server, linked_vfs.elementAt(j).toString());
                  uVFS.addLinkedVFS(tempVFS);
                } catch (Exception e) {
                  Log.log("REPORT", 1, e);
                }  
            } 
            Vector v = new Vector();
            try {
              Properties perms = uVFS.getCombinedPermissions();
              Enumeration enumeration = perms.keys();
              while (enumeration.hasMoreElements()) {
                String key = enumeration.nextElement().toString();
                if (key.equals("/"))
                  continue; 
                Properties p = uVFS.get_item(key);
                if (p != null) {
                  p = VRL.safe(p);
                  v.addElement(p);
                } 
              } 
              sorter cd2 = new sorter(this);
              cd2.setObj(new Properties(), "url");
              Object[] tmp = v.toArray();
              Arrays.sort(tmp, cd2);
              v.removeAllElements();
              for (int k = 0; k < tmp.length; k++)
                v.addElement(tmp[k]); 
            } catch (Exception e) {
              Log.log("REPORT", 1, e);
            } 
            Properties user = new Properties();
            user.put("username", username);
            user.put("password", ServerStatus.thisObj.common_code.decode_pass(tempUser.getProperty("password", "")));
            user.put("site_privs", tempUser.getProperty("site", ""));
            user.put("notes", tempUser.getProperty("notes", ""));
            user.put("max_logins", tempUser.getProperty("max_logins", ""));
            user.put("linked_vfs", tempUser.getProperty("linked_vfs", ""));
            user.put("last_login", tempUser.getProperty("last_logins", "").split(",")[0]);
            user.put("expire_password", tempUser.getProperty("expire_password", ""));
            user.put("expire_password_when", tempUser.getProperty("expire_password_when", ""));
            user.put("expire_password_days", tempUser.getProperty("expire_password_days", ""));
            user.put("account_expire", tempUser.getProperty("account_expire", ""));
            if (!tempUser.getProperty("created_time", "").equals(""))
              user.put("created_time", MMddyyyyHHmmss2.format(MMddyyyyHHmmss.parse(tempUser.getProperty("created_time", "")))); 
            user.put("enabled", (new StringBuffer(String.valueOf(!enabled.equals("-1")))).toString());
            String group_list = "";
            Enumeration keys = groups.keys();
            while (keys.hasMoreElements()) {
              String key = keys.nextElement().toString();
              Vector vv = (Vector)groups.get(key);
              for (int k = 0; k < vv.size(); k++) {
                if (vv.elementAt(k).toString().equalsIgnoreCase(username))
                  group_list = String.valueOf(group_list) + key + ","; 
              } 
            } 
            if (group_list.length() > 0)
              group_list = group_list.substring(0, group_list.length() - 1); 
            user.put("groups", group_list);
            user.put("listing", v);
            userDetails.put(username, user);
            uVFS.disconnect();
            uVFS.free();
          } 
        } 
      } 
      Vector users = doSort(userDetails, cd1);
      Properties results = new Properties();
      results.put("users", users);
      results.put("export", params.getProperty("export", ""));
      results.put("params", Common.removeNonStrings(params).toString());
      results.put("paramsObj", Common.removeNonStrings(params));
      if (users.size() > 0)
        status.put("report_empty", "false"); 
      sb.append(Common.getXMLString(results, "results", "WebInterface/Reports/UserUsage.xsl"));
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
