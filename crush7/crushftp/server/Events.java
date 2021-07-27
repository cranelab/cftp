package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.File_B;
import com.crushftp.client.VRL;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.JobScheduler;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class Events {
  public static int runningPluginCount = 0;
  
  public static Object pluginCountLock = new Object();
  
  Vector eventRunQueue = new Vector();
  
  Properties fileTracker = new Properties();
  
  public synchronized Properties process(String event_type, Properties fileItem1, Properties fileItem2, Vector the_events, SessionCrush theSession) {
    Properties info = null;
    if (the_events == null || the_events.size() == 0 || Common.System2.get("crushftp.dmz.queue") != null)
      return info; 
    String id = theSession.getId();
    if (id.endsWith(String.valueOf(theSession.uiSG("user_port")) + theSession.uiSG("user_number")))
      id = id.substring(0, id.lastIndexOf(String.valueOf(theSession.uiSG("user_port")) + theSession.uiSG("user_number"))); 
    if (event_type.equals("LOGOUT_ALL") || event_type.equals("BATCH_COMPLETE")) {
      for (int xx = this.eventRunQueue.size() - 1; xx >= 0; xx--) {
        Properties eventRun = this.eventRunQueue.elementAt(xx);
        if (eventRun.getProperty("id", "-1").equals(id)) {
          eventRun.put("wait", "disconnect_now");
          Log.log("EVENT", 2, "Setting events for id:" + id + " to run now.");
        } 
      } 
      return info;
    } 
    for (int x = 0; x < the_events.size(); x++) {
      Properties event = the_events.elementAt(x);
      if (!event.containsKey("id"))
        event.put("id", Common.makeBoundary()); 
      if ((event.getProperty("event_user_action_list", "").indexOf("(connect)") >= 0 && event_type.equals("LOGIN")) || (
        event.getProperty("event_user_action_list", "").indexOf("(upload)") >= 0 && event_type.equals("UPLOAD")) || (
        event.getProperty("event_user_action_list", "").indexOf("(upload)") >= 0 && event.getProperty("event_user_action_list", "").indexOf("(rename)") >= 0 && event_type.equals("RENAME")) || (
        event.getProperty("event_user_action_list", "").indexOf("(upload)") >= 0 && event_type.equals("RENAME") && fileItem2 != null && fileItem2.getProperty("url", "").endsWith(".filepart")) || (
        event.getProperty("event_user_action_list", "").indexOf("(download)") >= 0 && event_type.equals("DOWNLOAD")) || (
        event.getProperty("event_user_action_list", "").indexOf("(delete)") >= 0 && event_type.equals("DELETE")) || (
        event.getProperty("event_user_action_list", "").indexOf("(site)") >= 0 && event_type.equals("SITE")) || (
        event.getProperty("event_user_action_list", "").indexOf("(share)") >= 0 && event_type.equals("SHARE")) || (
        event.getProperty("event_user_action_list", "").indexOf("(custom)") >= 0 && event_type.equals("CUSTOM")) || (
        event.getProperty("event_user_action_list", "").indexOf("(rename)") >= 0 && event_type.equals("RENAME")) || (
        event.getProperty("event_user_action_list", "").indexOf("(error)") >= 0 && event_type.equals("ERROR")) || (
        event.getProperty("event_user_action_list", "").indexOf("(welcome)") >= 0 && event_type.equals("WELCOME"))) {
        boolean criteria_met = false;
        if (event.getProperty("event_always_cb", "").equals("true")) {
          criteria_met = true;
        } else if (event.getProperty("event_if_list", "").equals("(upload)") && event_type.equals("UPLOAD")) {
          criteria_met = true;
        } else if (event.getProperty("event_if_list", "").equals("(download)") && event_type.equals("DOWNLOAD")) {
          criteria_met = true;
        } else if (event.getProperty("event_if_list", "").equals("(real_url)") && (event_type.equals("UPLOAD") || event_type.equals("DOWNLOAD") || event_type.equals("DELETE") || event_type.equals("RENAME"))) {
          String url = ServerStatus.thisObj.change_vars_to_values(event.getProperty("event_dir_data", ""), theSession);
          String url2 = fileItem1.getProperty("url", "");
          String url3 = url;
          if (url2.toUpperCase().startsWith("FILE:/") && !url2.toUpperCase().startsWith("FILE://"))
            url2 = "file://" + url2.substring("file:/".length()); 
          if (url3.toUpperCase().startsWith("FILE:/") && !url3.toUpperCase().startsWith("FILE://"))
            url3 = "file://" + url3.substring("file:/".length()); 
          if (url.indexOf("*") < 0 && url.indexOf("?") < 0) {
            if (url2.toUpperCase().startsWith(url3.toUpperCase()))
              criteria_met = true; 
          } else if (Common.do_search(url3.toUpperCase(), url2.toUpperCase(), false, 0)) {
            criteria_met = true;
          } 
          if (ServerStatus.siIG("enterprise_level") <= 0 && criteria_met) {
            Log.log("EVENT", 0, "Enterprise license is required for URL matching on events.  Event has been ignored.");
            criteria_met = false;
          } 
          if (criteria_met)
            Log.log("EVENT", 0, "Matched event url:" + fileItem1.getProperty("url", "") + "   starts with:" + url); 
        } else if (event.getProperty("event_if_list", "").equals("(upload_dir)") && event_type.equals("UPLOAD")) {
          String path = event.getProperty("event_dir_data", "");
          path = ServerStatus.thisObj.change_vars_to_values(path, theSession);
          String path2 = fileItem1.getProperty("the_command_data");
          if (path.indexOf("*") < 0 && path.indexOf("?") < 0) {
            String path3 = path2;
            if (theSession != null && path3.startsWith(theSession.SG("root_dir")))
              path3 = path3.substring(theSession.SG("root_dir").length() - 1); 
            if (path2.toUpperCase().startsWith(path.toUpperCase()) || path3.toUpperCase().startsWith(path.toUpperCase())) {
              Log.log("EVENT", 0, "Matched event dir:" + path2 + "   starts with:" + path);
              criteria_met = true;
            } 
          } else if (Common.do_search(path.toUpperCase(), path2.toUpperCase(), false, 0)) {
            criteria_met = true;
          } 
        } else if ((event.getProperty("event_if_list", "").equals("(upload_dir)") || event.getProperty("event_if_list", "").equals("(download_dir)")) && (event_type.equals("RENAME") || event_type.equals("DELETE"))) {
          String path = event.getProperty("event_dir_data", "");
          path = ServerStatus.thisObj.change_vars_to_values(path, theSession);
          String path2 = fileItem1.getProperty("the_command_data");
          if (path.indexOf("*") < 0 && path.indexOf("?") < 0) {
            String path3 = path2;
            if (theSession != null && path3.startsWith(theSession.SG("root_dir")))
              path3 = path3.substring(theSession.SG("root_dir").length() - 1); 
            if (path2.toUpperCase().startsWith(path.toUpperCase()) || path3.toUpperCase().startsWith(path.toUpperCase())) {
              Log.log("EVENT", 0, "Matched event dir:" + path2 + "   starts with:" + path);
              criteria_met = true;
            } 
          } else if (Common.do_search(path.toUpperCase(), path2.toUpperCase(), false, 0)) {
            criteria_met = true;
          } 
        } else if (event.getProperty("event_if_list", "").equals("(download_dir)") && event_type.equals("DOWNLOAD")) {
          String path = event.getProperty("event_dir_data", "");
          path = ServerStatus.thisObj.change_vars_to_values(path, theSession);
          String path2 = fileItem1.getProperty("the_command_data");
          if (path.indexOf("*") < 0 && path.indexOf("?") < 0) {
            String path3 = path2;
            if (theSession != null && path3.startsWith(theSession.SG("root_dir")))
              path3 = path3.substring(theSession.SG("root_dir").length() - 1); 
            if (path2.toUpperCase().startsWith(path.toUpperCase()) || path3.toUpperCase().startsWith(path.toUpperCase())) {
              Log.log("EVENT", 0, "Matched event dir:" + path2 + "   starts with:" + path);
              criteria_met = true;
            } 
          } else if (Common.do_search(path.toUpperCase(), path2.toUpperCase(), false, 0)) {
            criteria_met = true;
          } 
        } 
        if (criteria_met) {
          Log.log("EVENT", 2, new Exception("Event trigger stack"));
          if (event.getProperty("event_now_cb", "").equals("true")) {
            Log.log("EVENT", 2, "Event is set to run immediately, running it in 2 seconds...");
            Properties properties = new Properties();
            properties.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
            properties.put("timeout", (new StringBuffer(String.valueOf(System.currentTimeMillis() + 2000L))).toString());
            properties.put("wait", "timeout");
            properties.put("session", theSession);
            properties.put("event", event);
            properties.put("id", id);
            if (fileItem1 != null) {
              properties.put("fileItem", fileItem1);
              if (event_type.equals("SITE"))
                if (!fileItem1.getProperty("event_name").equals(event.getProperty("name", "")))
                  continue;  
            } 
            Log.log("EVENT", 2, "Adding 2 second delayed event to the queue...");
            this.eventRunQueue.addElement(properties);
            if (event_type.equals("LOGIN") || event_type.equals("SITE") || event_type.equals("WELCOME") || event_type.equals("ERROR") || event.getProperty("name", "").toUpperCase().endsWith("_NO_DELAY")) {
              Log.log("EVENT", 2, "Skipping 2 second delay.");
              properties.put("timeout", "0");
              info = checkEventsNow();
            } 
            continue;
          } 
          Properties eventRun = new Properties();
          eventRun.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
          eventRun.put("session", theSession);
          eventRun.put("event", event);
          eventRun.put("id", id);
          eventRun.put("wait", "disconnect_all");
          if (event.getProperty("event_after_list", "").indexOf("(disconnect") >= 0) {
            String disconnectMsg = event.getProperty("event_after_list", "").substring(event.getProperty("event_after_list", "").indexOf("(disconnect") + "(disconnect".length());
            disconnectMsg = disconnectMsg.substring(0, disconnectMsg.indexOf(")"));
            eventRun.put("wait", "disconnect " + disconnectMsg);
            Log.log("EVENT", 2, "Adding event to queue:" + id + ":" + event.getProperty("name", "") + "...will check to see if its already in the queue...");
          } else if (event.getProperty("event_after_list", "").indexOf("(idle_") >= 0) {
            String secs = event.getProperty("event_after_list", "").substring(event.getProperty("event_after_list", "").indexOf("(idle_") + "(idle_".length());
            secs = secs.substring(0, secs.indexOf(")"));
            eventRun.put("wait", "idle " + secs);
            Log.log("EVENT", 2, "Adding event to queue:" + id + ":" + event.getProperty("name", "") + "...will check to see if its already in the queue...");
          } 
          boolean found = false;
          for (int xx = this.eventRunQueue.size() - 1; xx >= 0; xx--) {
            Properties eventRun2 = this.eventRunQueue.elementAt(xx);
            Properties event2 = (Properties)eventRun2.get("event");
            if (eventRun2.getProperty("id", "-1").equals(id) && event2.getProperty("id").equals(event.getProperty("id"))) {
              Log.log("EVENT", 2, "Event is in queue..." + event2.getProperty("id", "-1") + ":" + id);
              found = true;
            } 
          } 
          updateTracker(id, event_type, event, fileItem1, fileItem2, theSession);
          if (!found) {
            Log.log("EVENT", 2, "Adding event to queue as " + event_type + ".");
            this.eventRunQueue.addElement(eventRun);
          } 
        } 
      } 
      continue;
    } 
    return info;
  }
  
  private void updateTracker(String id, String event_type, Properties event, Properties fileItem1, Properties fileItem2, SessionCrush theSession) {
    if (Common.System2.get("crushftp.dmz.queue") != null)
      return; 
    Properties tracker = (Properties)this.fileTracker.get(String.valueOf(id) + "_" + event.getProperty("id"));
    if (tracker == null) {
      tracker = new Properties();
      tracker.put("uploads", new Vector());
      tracker.put("downloads", new Vector());
      tracker.put("deletes", new Vector());
      tracker.put("renames", new Vector());
      tracker.put("shares", new Vector());
      tracker.put("customs", new Vector());
      this.fileTracker.put(String.valueOf(id) + "_" + event.getProperty("id"), tracker);
    } 
    Vector uploads = (Vector)tracker.get("uploads");
    Vector downloads = (Vector)tracker.get("downloads");
    Vector deletes = (Vector)tracker.get("deletes");
    Vector renames = (Vector)tracker.get("renames");
    Vector shares = (Vector)tracker.get("shares");
    Vector customs = (Vector)tracker.get("customs");
    if (fileItem1 != null) {
      if (event_type.equals("UPLOAD")) {
        boolean found = false;
        for (int x = uploads.size() - 1; x >= 0 && !found; x--) {
          Properties p = uploads.elementAt(x);
          if (fileItem1.getProperty("mark_error", "").equals("true")) {
            Log.log("EVENT", 2, "Mark error event item:" + VRL.fileFix((new VRL(fileItem1.getProperty("url", ""))).safe()) + "  versus  " + VRL.fileFix((new VRL(p.getProperty("url", ""))).safe()));
            if (VRL.fileFix(p.getProperty("url", "")).equalsIgnoreCase(VRL.fileFix(fileItem1.getProperty("url", "")))) {
              p.put("the_file_error", fileItem1.getProperty("the_file_error"));
              p.put("the_file_status", fileItem1.getProperty("the_file_status"));
              found = true;
            } 
          } else if (VRL.fileFix(p.getProperty("url", "")).equalsIgnoreCase(VRL.fileFix(fileItem1.getProperty("url", "")))) {
            Log.log("EVENT", 2, "Found existing event item:" + VRL.fileFix((new VRL(p.getProperty("url", ""))).safe()) + " versus:" + VRL.fileFix((new VRL(fileItem1.getProperty("url", ""))).safe()));
            uploads.setElementAt(fileItem1, x);
            found = true;
          } 
        } 
        if (!found && !fileItem1.getProperty("mark_error", "").equals("true")) {
          Log.log("EVENT", 2, "New event item:" + VRL.fileFix((new VRL(fileItem1.getProperty("url", ""))).safe()));
          uploads.addElement(fileItem1);
        } else {
          return;
        } 
      } else if (event_type.equals("DOWNLOAD")) {
        downloads.addElement(fileItem1);
      } else if (event_type.equals("SHARE")) {
        shares.addElement(fileItem1);
      } else if (event_type.equals("CUSTOM")) {
        customs.addElement(fileItem1);
      } else if (event_type.equals("DELETE")) {
        deletes.addElement(fileItem1);
        for (int xx = uploads.size() - 1; xx >= 0; xx--) {
          Properties p = uploads.elementAt(xx);
          if (VRL.fileFix(p.getProperty("url", "")).equalsIgnoreCase(VRL.fileFix(fileItem1.getProperty("url", ""))))
            uploads.removeElementAt(xx); 
        } 
      } else if (event_type.equals("RENAME")) {
        if (fileItem2 == null)
          fileItem2 = fileItem1; 
        if (!fileItem2.getProperty("url", "").endsWith(".filepart"))
          renames.addElement(fileItem1); 
        int xx;
        for (xx = 0; xx < downloads.size(); xx++) {
          Properties p = downloads.elementAt(xx);
          if (VRL.fileFix(p.getProperty("url", "")).equalsIgnoreCase(VRL.fileFix(fileItem1.getProperty("url", "")))) {
            p.put("url", fileItem1.getProperty("url", ""));
            p.put("url_2", fileItem2.getProperty("url", ""));
            p.put("the_file_name", fileItem1.getProperty("the_file_name", ""));
            p.put("the_file_name_2", fileItem2.getProperty("the_file_name", ""));
            p.put("the_file_path", fileItem1.getProperty("the_file_path", ""));
            p.put("the_file_path_2", fileItem2.getProperty("the_file_path", ""));
          } 
        } 
        for (xx = 0; xx < uploads.size(); xx++) {
          Properties p = uploads.elementAt(xx);
          if (VRL.fileFix(p.getProperty("url", "")).equalsIgnoreCase(VRL.fileFix(fileItem2.getProperty("url", "")))) {
            p.put("url", fileItem1.getProperty("url", ""));
            p.put("url_2", fileItem2.getProperty("url", ""));
            p.put("the_file_name", fileItem1.getProperty("the_file_name", ""));
            p.put("the_file_name_2", fileItem2.getProperty("the_file_name", ""));
            p.put("the_file_path", fileItem1.getProperty("the_file_path", ""));
            p.put("the_file_path_2", fileItem2.getProperty("the_file_path", ""));
          } 
        } 
      } 
      theSession.add_log("[" + theSession.uiSG("user_number") + ":" + theSession.uiSG("user_name") + ":" + theSession.uiSG("user_ip") + "] WROTE: *Tracking event items (..." + id.substring(id.length() - 4) + ") uploads:" + uploads.size() + ",downloads:" + downloads.size() + ",deletes:" + deletes.size() + ",renames:" + renames.size() + ",lastType:" + event_type + "*", "STOR");
    } 
  }
  
  public synchronized Properties checkEventsNow() {
    Properties info = null;
    String lastName = "";
    int x;
    for (x = 0; x < this.eventRunQueue.size(); x++) {
      try {
        Properties eventRun = this.eventRunQueue.elementAt(x);
        SessionCrush theSession = (SessionCrush)eventRun.get("session");
        String id = eventRun.getProperty("id");
        Properties event = (Properties)eventRun.get("event");
        if (!event.containsKey("id"))
          event.put("id", Common.makeBoundary()); 
        lastName = event.getProperty("name", "");
        if (theSession.user == null) {
          Log.log("EVENT", 0, "Invalid user session found in event.");
          this.eventRunQueue.remove(eventRun);
          break;
        } 
        theSession.user_info.put("root_dir", theSession.user.getProperty("root_dir"));
        long lastActivity = Long.parseLong(theSession.getProperty("last_activity", "0"));
        boolean notConnected = (countConnectedUsers(theSession) == 0);
        boolean http = theSession.uiSG("user_protocol").startsWith("HTTP");
        String doEventType = null;
        if (eventRun.getProperty("wait").equals("timeout") || (eventRun.getProperty("wait").startsWith("disconnect") && event.getProperty("event_now_cb", "").equals("true"))) {
          Log.log("EVENT", 2, "Checking time on timeout event...");
          if (Long.parseLong(eventRun.getProperty("timeout", "0")) < System.currentTimeMillis()) {
            Log.log("EVENT", 2, "Event has reached timeout, running...");
            eventRun.put("delete", "true");
            Properties p = (Properties)eventRun.get("fileItem");
            if (p == null || !p.getProperty("usedByNow_" + event.getProperty("id"), "false").equals("true")) {
              if (p != null)
                p.put("usedByNow_" + event.getProperty("id"), "true"); 
              Vector items = new Vector();
              if (p != null)
                items.addElement(p); 
              cleanupItems(items);
              if (items.size() > 0 || event.getProperty("event_user_action_list", "").indexOf("(connect)") >= 0 || event.getProperty("event_user_action_list", "").indexOf("(site)") >= 0 || event.getProperty("event_user_action_list", "").indexOf("(welcome)") >= 0 || event.getProperty("event_user_action_list", "").indexOf("(error)") >= 0)
                info = runEvent(event, theSession, items); 
            } 
          } 
        } else if (eventRun.getProperty("wait").startsWith("idle")) {
          Log.log("EVENT", 2, "Checking time on idle event...");
          long secs = Long.parseLong(eventRun.getProperty("wait").split(" ")[1]);
          if (System.currentTimeMillis() - lastActivity > secs * 1000L || (System.currentTimeMillis() - lastActivity > ((http ? 300 : 10) * 1000) && notConnected)) {
            Log.log("EVENT", 2, "Event has reached idle timeout, running...");
            doEventType = "Idle";
          } 
        } else if (eventRun.getProperty("wait").startsWith("disconnect")) {
          Log.log("EVENT", 2, "Checking disconnect event, lastActivity:" + lastActivity + " " + new Date(lastActivity) + "  notConnected:" + notConnected);
          if (lastActivity == 0L || eventRun.getProperty("wait").equals("disconnect_now") || (System.currentTimeMillis() - lastActivity > ((http ? 300 : 10) * 1000) && notConnected))
            doEventType = "Disconnect"; 
        } 
        if (doEventType != null) {
          Log.log("EVENT", 2, "Running event type:" + doEventType + " id=" + id.substring(id.length() - 4) + " eventid:" + event.getProperty("id"));
          eventRun.put("delete", "true");
          Vector items = new Vector();
          Properties tracker = (Properties)this.fileTracker.get(String.valueOf(id) + "_" + event.getProperty("id"));
          if (tracker == null)
            Log.log("SERVER", 0, "Event lost!:" + event + ":" + eventRun); 
          Vector uploads = (Vector)tracker.get("uploads");
          Vector downloads = (Vector)tracker.get("downloads");
          Vector deletes = (Vector)tracker.get("deletes");
          Vector renames = (Vector)tracker.get("renames");
          Vector shares = (Vector)tracker.get("shares");
          Vector customs = (Vector)tracker.get("customs");
          if (event.getProperty("event_user_action_list", "").indexOf("(upload)") >= 0) {
            items.addAll(uploads);
          } else if (event.getProperty("event_user_action_list", "").indexOf("(download)") >= 0) {
            items.addAll(downloads);
          } else if (event.getProperty("event_user_action_list", "").indexOf("(delete)") >= 0) {
            items.addAll(deletes);
          } else if (event.getProperty("event_user_action_list", "").indexOf("(rename)") >= 0) {
            items.addAll(renames);
          } else if (event.getProperty("event_user_action_list", "").indexOf("(share)") >= 0) {
            items.addAll(shares);
          } else if (event.getProperty("event_user_action_list", "").indexOf("(custom)") >= 0) {
            items.addAll(customs);
          } 
          if (event.getProperty("event_user_action_list", "").indexOf("(") != event.getProperty("event_user_action_list", "").lastIndexOf("("))
            Common.do_sort(items, "modified", "modified"); 
          Log.log("EVENT", 2, "Checking event:" + event);
          Log.log("EVENT", 2, "Checking event items:" + items.size());
          for (int xx = items.size() - 1; xx >= 0; xx--) {
            Properties p = items.elementAt(xx);
            Properties p2 = (Properties)p.clone();
            p2.put("url", (new VRL(p2.getProperty("url"))).safe());
            Log.log("EVENT", 2, "Checking event item:" + p2);
            if (p.getProperty("usedBy" + doEventType + "_" + event.getProperty("id"), "false").equals("true")) {
              Log.log("EVENT", 0, "Event has already processed this item (removing...):" + p);
              items.removeElementAt(xx);
            } 
            p.put("usedBy" + doEventType + "_" + event.getProperty("id"), "true");
          } 
          cleanupItems(items);
          theSession.uiPUT("session_upload_count", (new StringBuffer(String.valueOf(items.size()))).toString());
          theSession.uiPUT("session_download_count", (new StringBuffer(String.valueOf(items.size()))).toString());
          if (items.size() > 0 || event.getProperty("event_user_action_list", "").indexOf("(connect)") >= 0 || event.getProperty("event_user_action_list", "").indexOf("(site)") >= 0 || event.getProperty("event_user_action_list", "").indexOf("(welcome)") >= 0 || event.getProperty("event_user_action_list", "").indexOf("(error)") >= 0)
            info = runEvent(event, theSession, items); 
        } 
      } catch (Exception e) {
        Log.log("EVENT", 0, "Event " + lastName + " failed due to an error");
        Log.log("EVENT", 0, e);
      } 
    } 
    for (x = this.eventRunQueue.size() - 1; x >= 0; x--) {
      Properties eventRun = this.eventRunQueue.elementAt(x);
      String id = eventRun.getProperty("id");
      Properties event = (Properties)eventRun.get("event");
      if (eventRun.getProperty("delete", "false").equals("true")) {
        Log.log("EVENT", 2, "Removing event tracker for id=" + id.substring(id.length() - 4) + " eventid:" + event.getProperty("id"));
        this.eventRunQueue.remove(x);
        this.fileTracker.remove(String.valueOf(id) + "_" + event.getProperty("id"));
      } 
    } 
    return info;
  }
  
  public int countConnectedUsers(SessionCrush this_user) {
    int num_users = 0;
    for (int x = ServerStatus.siVG("user_list").size() - 1; x >= 0; x--) {
      try {
        Properties p = ServerStatus.siVG("user_list").elementAt(x);
        if (((SessionCrush)p.get("session")).getId().equalsIgnoreCase(this_user.getId())) {
          num_users++;
          Log.log("EVENT", 2, "Found similar user:" + num_users + ":" + ((SessionCrush)p.get("session")).getId());
        } 
      } catch (Exception exception) {}
    } 
    return num_users;
  }
  
  public void cleanupItems(Vector itemsAll) {
    for (int xx = itemsAll.size() - 1; xx >= 0; xx--) {
      Properties p = itemsAll.elementAt(xx);
      if ((!ServerStatus.BG("event_empty_files") && p.getProperty("the_file_size", "0").equals("0")) || p.getProperty("the_file_name", "").indexOf(".DS_Store") >= 0 || p.getProperty("the_file_name", "").startsWith("._") || p.getProperty("the_file_name", "").equals(""))
        itemsAll.remove(p); 
    } 
  }
  
  public void cleanupItemsEmail(Vector itemsAll) {
    for (int xx = itemsAll.size() - 1; xx >= 0; xx--) {
      Properties p = itemsAll.elementAt(xx);
      if (!ServerStatus.BG("event_reuse") && p.getProperty("usedByEvent", "").equals("true"))
        itemsAll.remove(p); 
      p.put("usedByEvent", "true");
    } 
  }
  
  public Properties runEvent(Properties event, SessionCrush the_user, Vector items) throws Exception {
    Log.log("EVENT", 2, "runEvent::" + event.getProperty("name"));
    Log.log("EVENT", 2, "runEvent:items size:" + items.size());
    Log.log("EVENT", 2, "runEvent:items:" + items);
    Properties info = new Properties();
    Thread t = new Thread(new Runnable(this, items, event, the_user, info) {
          final Events this$0;
          
          private final Vector val$items;
          
          private final Properties val$event;
          
          private final SessionCrush val$the_user;
          
          private final Properties val$info;
          
          public void run() {
            try {
              Properties groupings = new Properties();
              groupings.put("default", new Vector());
              for (int x = 0; x < this.val$items.size(); x++) {
                String id = "default";
                Properties item = this.val$items.elementAt(x);
                if (ServerStatus.BG("event_batching"))
                  if (item.containsKey("metaInfo")) {
                    Properties metaInfo = (Properties)item.get("metaInfo");
                    id = metaInfo.getProperty("unique_upload_id", id);
                  }  
                Vector v = (Vector)groupings.get(id);
                if (v == null)
                  v = new Vector(); 
                groupings.put(id, v);
                v.addElement(item);
              } 
              if (groupings.size() > 1)
                groupings.remove("default"); 
              Enumeration keys = groupings.keys();
              while (keys.hasMoreElements()) {
                String groupId = keys.nextElement().toString();
                Log.log("EVENT", 0, "Grouping event on id:" + groupId);
                Vector groupedItems = (Vector)groupings.get(groupId);
                if (this.val$event.getProperty("event_action_list", "").indexOf("(send_email)") >= 0) {
                  this.this$0.cleanupItemsEmail(groupedItems);
                  if (groupedItems.size() > 0 || this.val$event.getProperty("event_user_action_list", "").indexOf("(connect)") >= 0 || this.val$event.getProperty("event_user_action_list", "").indexOf("(site)") >= 0 || this.val$event.getProperty("event_user_action_list", "").indexOf("(welcome)") >= 0 || this.val$event.getProperty("event_user_action_list", "").indexOf("(error)") >= 0)
                    this.this$0.doEventEmail(this.val$event, this.val$the_user.user, this.val$the_user.user_info, groupedItems, this.val$the_user.uiVG("lastUploadStats")); 
                } 
                if (this.val$event.getProperty("event_action_list", "").indexOf("(run_plugin)") >= 0)
                  this.this$0.doEventPlugin(this.val$info, this.val$event, this.val$the_user, groupedItems); 
              } 
            } finally {
              synchronized (Events.pluginCountLock) {
                Events.runningPluginCount--;
              } 
            } 
          }
        });
    while (true) {
      synchronized (pluginCountLock) {
        if (runningPluginCount <= ServerStatus.IG("max_event_threads")) {
          runningPluginCount++;
          break;
        } 
      } 
      try {
        Thread.sleep(100L);
      } catch (InterruptedException interruptedException) {}
    } 
    t.setName(String.valueOf(Thread.currentThread().getName()) + ":Running event:" + event.getProperty("name"));
    t.start();
    boolean async = (event.getProperty("async", "").equalsIgnoreCase("") || event.getProperty("async", "").equalsIgnoreCase("auto")) ? ServerStatus.BG("event_asynch") : event.getProperty("async", "").equalsIgnoreCase("yes");
    if (!async) {
      t.join((1000 * ServerStatus.IG("event_thread_timeout")));
      if (t.isAlive())
        Log.log("EVENT", 0, "Event didn't complete in 60 seconds, leaving thread running...items=" + items.size() + ":Event=" + event); 
    } 
    return info;
  }
  
  public String doEventEmail(Properties event, Properties user, Properties user_info, Vector items, Vector lastUploadStats) {
    Log.log("EVENT", 0, "Event:EMAIL " + event.getProperty("name") + ":" + event.getProperty("to") + "\r\n");
    Log.log("EVENT", 2, "doEventEmail:items size:" + items.size());
    Log.log("EVENT", 2, "doEventEmail:items:" + items);
    String to = "";
    String cc = "";
    String bcc = "";
    String from = "";
    String body = "";
    String subject = "";
    try {
      body = Common.replace_str(Common.replace_str(event.getProperty("body"), "&lt;LINE&gt;", "<LINE>"), "&lt;/LINE&gt;", "</LINE>");
      String the_body_line = "";
      int loops = 0;
      while (body.toUpperCase().indexOf("<LINE>") >= 0 && body.toUpperCase().indexOf("</LINE>") >= 0) {
        loops++;
        if (loops > 20)
          break; 
        try {
          the_body_line = body.substring(body.toUpperCase().indexOf("<LINE>") + "<LINE>".length(), body.toUpperCase().indexOf("</LINE>"));
        } catch (Exception exception) {}
        String lineData = "";
        for (int i = 0; i < items.size(); i++) {
          Properties p = items.elementAt(i);
          String the_line = the_body_line;
          the_line = replace_path_url_segments(p, the_line);
          the_line = Common.replace_str(the_line, "%user_time%", p.getProperty("user_time", ""));
          the_line = Common.replace_str(the_line, "%the_file_path%", p.getProperty("the_file_path", ""));
          the_line = Common.replace_str(the_line, "%the_file_name%", p.getProperty("the_file_name", ""));
          the_line = Common.replace_str(the_line, "%the_file_name_2%", p.getProperty("the_file_name_2", ""));
          the_line = Common.replace_str(the_line, "%the_file_size%", p.getProperty("the_file_size", ""));
          the_line = Common.replace_str(the_line, "%the_file_size_formatted%", Common.format_bytes_short2(Long.parseLong(p.getProperty("the_file_size", "0"))));
          the_line = Common.replace_str(the_line, "%the_file_speed%", p.getProperty("the_file_speed", ""));
          the_line = Common.replace_str(the_line, "%the_file_error%", p.getProperty("the_file_error", ""));
          the_line = Common.replace_str(the_line, "%the_file_start%", p.getProperty("the_file_start", ""));
          the_line = Common.replace_str(the_line, "%the_file_end%", p.getProperty("the_file_end", ""));
          the_line = Common.replace_str(the_line, "%the_file_md5%", p.getProperty("the_file_md5", ""));
          the_line = Common.replace_str(the_line, "%url%", p.getProperty("url", ""));
          the_line = Common.replace_str(the_line, "%url_2%", p.getProperty("url_2", ""));
          the_line = Common.replace_str(the_line, "%display%", p.getProperty("display", ""));
          the_line = Common.replace_str(the_line, "%all%", p.toString());
          the_line = ServerStatus.thisObj.change_vars_to_values(the_line, user, user_info, null);
          if (!the_line.trim().equals(""))
            if (event.getProperty("event_now_cb", "").equals("true") && (i == items.size() - 1 || event.getProperty("event_user_action_list").indexOf("(disconnect)") >= 0)) {
              lineData = String.valueOf(lineData) + the_line + "\r\n";
            } else if (!event.getProperty("event_now_cb", "").equals("true")) {
              lineData = String.valueOf(lineData) + the_line + "\r\n";
            }  
        } 
        Log.log("EVENT", 2, "BODY:<LINE>" + lineData + "</LINE>");
        try {
          body = Common.replace_str(body, body.substring(body.toUpperCase().indexOf("<LINE>"), body.toUpperCase().indexOf("</LINE>") + "</LINE>".length()), lineData);
        } catch (Exception e) {
          Log.log("EVENT", 1, e);
        } 
      } 
      body = ServerStatus.thisObj.change_vars_to_values(body, user, user_info, null);
      Properties form_email = Common.buildFormEmail(ServerStatus.server_settings, lastUploadStats);
      String web_upload_form_all = "";
      Vector names = new Vector();
      Vector forms = new Vector();
      for (int xx = 0; xx < items.size(); xx++) {
        String web_upload_form = "";
        Properties item = items.elementAt(xx);
        Properties uploadStat = (Properties)item.get("uploadStats");
        Properties metaInfo = null;
        if (uploadStat == null) {
          metaInfo = (Properties)item.get("metaInfo");
        } else {
          metaInfo = (Properties)uploadStat.get("metaInfo");
        } 
        if (metaInfo != null)
          if (metaInfo != null) {
            String id = metaInfo.getProperty("UploadFormId", "");
            Properties customForm = null;
            Vector customForms = (Vector)ServerStatus.server_settings.get("CustomForms");
            if (customForms != null) {
              for (int i = 0; i < customForms.size(); i++) {
                Properties p = customForms.elementAt(i);
                if (p.getProperty("id", "").equals(id)) {
                  customForm = p;
                  break;
                } 
              } 
              if (customForm != null) {
                if (!customForm.containsKey("entries"))
                  customForm.put("entries", new Vector()); 
                Vector entries = (Vector)customForm.get("entries");
                for (int j = 0; j < entries.size(); j++) {
                  Properties p = entries.elementAt(j);
                  if (!p.getProperty("type").trim().equals("label")) {
                    web_upload_form = String.valueOf(web_upload_form) + p.getProperty("name", "").trim() + ":" + metaInfo.getProperty(p.getProperty("name", "").trim()) + "\r\n\r\n";
                  } else {
                    web_upload_form = String.valueOf(web_upload_form) + p.getProperty("label", "").trim() + " " + p.getProperty("value", "") + "\r\n";
                  } 
                } 
              } 
              if (forms.indexOf(web_upload_form) < 0) {
                names.addElement(item.getProperty("the_file_name"));
                forms.addElement(web_upload_form);
              } else {
                names.setElementAt(String.valueOf(names.elementAt(forms.indexOf(web_upload_form)).toString()) + "," + ((uploadStat == null) ? item.getProperty("name", "") : uploadStat.getProperty("name", "")), forms.indexOf(web_upload_form));
              } 
            } 
          }  
      } 
      for (int x = 0; x < names.size(); x++)
        web_upload_form_all = String.valueOf(web_upload_form_all) + "File Name(s): " + names.elementAt(x).toString() + "\r\n\r\n" + forms.elementAt(x).toString() + "\r\n"; 
      body = Common.replace_str(body, "%web_upload_form%", web_upload_form_all);
      subject = event.getProperty("subject");
      subject = replace_line_variables(items, subject, user, user_info);
      subject = ServerStatus.thisObj.change_vars_to_values(subject, user, user_info, null);
      subject = Common.replaceFormVariables(form_email, subject);
      to = replace_line_variables(items, event.getProperty("to"), user, user_info).trim();
      to = ServerStatus.thisObj.change_vars_to_values(to, user, user_info, null);
      to = Common.replaceFormVariables(form_email, to);
      cc = replace_line_variables(items, event.getProperty("cc"), user, user_info).trim();
      cc = ServerStatus.thisObj.change_vars_to_values(cc, user, user_info, null);
      cc = Common.replaceFormVariables(form_email, cc);
      bcc = replace_line_variables(items, event.getProperty("bcc"), user, user_info).trim();
      bcc = ServerStatus.thisObj.change_vars_to_values(bcc, user, user_info, null);
      bcc = Common.replaceFormVariables(form_email, bcc);
      from = replace_line_variables(items, event.getProperty("from"), user, user_info).trim();
      from = ServerStatus.thisObj.change_vars_to_values(from, user, user_info, null);
      from = Common.replaceFormVariables(form_email, from);
      body = Common.replaceFormVariables(form_email, body);
      if (ServerStatus.SG("smtp_server").equals("")) {
        ServerStatus.server_settings.put("smtp_server", event.getProperty("smtp_server", ""));
        ServerStatus.server_settings.put("smtp_user", event.getProperty("smtp_user", ""));
        ServerStatus.server_settings.put("smtp_pass", event.getProperty("smtp_pass", ""));
        ServerStatus.thisObj.save_server_settings(true);
      } 
      String emailResult = Common.send_mail(ServerStatus.SG("discovered_ip"), to, cc, bcc, from, subject, body, ServerStatus.SG("smtp_server"), ServerStatus.SG("smtp_user"), ServerStatus.SG("smtp_pass"), ServerStatus.BG("smtp_ssl"), ServerStatus.BG("smtp_html"), (File_B[])null);
      if (emailResult.toUpperCase().indexOf("SUCCESS") < 0) {
        Log.log("EVENT", 0, String.valueOf(LOC.G("FAILURE:")) + " " + emailResult + "\r\n");
        Log.log("EVENT", 0, String.valueOf(LOC.G("FROM:")) + " " + from + "\r\n");
        Log.log("EVENT", 0, String.valueOf(LOC.G("TO:")) + " " + to + "\r\n");
        Log.log("EVENT", 0, String.valueOf(LOC.G("CC:")) + " " + cc + "\r\n");
        Log.log("EVENT", 0, String.valueOf(LOC.G("BCC:")) + " " + bcc + "\r\n");
        Log.log("EVENT", 0, String.valueOf(LOC.G("SUBJECT:")) + " " + subject + "\r\n");
        Log.log("EVENT", 0, String.valueOf(LOC.G("BODY:")) + " " + body + "\r\n");
        Properties m = new Properties();
        m.put("result", emailResult);
        m.put("body", body);
        m.put("subject", subject);
        m.put("to", to);
        m.put("from", from);
        m.put("cc", cc);
        m.put("bcc", bcc);
        ServerStatus.thisObj.runAlerts("invalid_email", m, null, null);
        return "ERROR:" + emailResult;
      } 
      Log.log("EVENT", 0, "Event:EMAIL SUCCESS " + event.getProperty("name") + ":" + to + "\r\n");
      return "SUCCESS";
    } catch (Exception e) {
      Log.log("EVENT", 0, String.valueOf(LOC.G("Event:EMAIL")) + " " + ServerStatus.thisObj.change_vars_to_values(event.getProperty("name"), user, user_info, null) + ":" + to + ":" + e + "\r\n");
      Log.log("EVENT", 1, e);
      Properties m = new Properties();
      m.put("result", e.toString());
      m.put("body", body);
      m.put("subject", subject);
      m.put("to", to);
      m.put("from", from);
      m.put("cc", cc);
      m.put("bcc", bcc);
      ServerStatus.thisObj.runAlerts("invalid_email", m, null, null);
      return "ERROR:" + e;
    } 
  }
  
  public String replace_line_variables(Vector items, String s, Properties user, Properties user_info) {
    String s_line = "";
    try {
      s_line = s.substring(s.toUpperCase().indexOf("<LINE>") + "<LINE>".length(), s.toUpperCase().indexOf("</LINE>"));
    } catch (Exception exception) {}
    String lineData = "";
    for (int x = 0; x < items.size(); x++) {
      Properties p = items.elementAt(x);
      String the_line = s_line;
      the_line = replace_path_url_segments(p, the_line);
      the_line = Common.replace_str(the_line, "%user_time%", p.getProperty("user_time", ""));
      the_line = Common.replace_str(the_line, "%the_file_path%", p.getProperty("the_file_path", ""));
      the_line = Common.replace_str(the_line, "%the_file_name%", p.getProperty("the_file_name", ""));
      the_line = Common.replace_str(the_line, "%the_file_name_2%", p.getProperty("the_file_name_2", ""));
      the_line = Common.replace_str(the_line, "%the_file_size%", p.getProperty("the_file_size", ""));
      the_line = Common.replace_str(the_line, "%the_file_size_formatted%", Common.format_bytes_short2(Long.parseLong(p.getProperty("the_file_size", "0"))));
      the_line = Common.replace_str(the_line, "%the_file_speed%", p.getProperty("the_file_speed", ""));
      the_line = Common.replace_str(the_line, "%the_file_error%", p.getProperty("the_file_error", ""));
      the_line = Common.replace_str(the_line, "%the_file_start%", p.getProperty("the_file_start", ""));
      the_line = Common.replace_str(the_line, "%the_file_end%", p.getProperty("the_file_end", ""));
      the_line = Common.replace_str(the_line, "%the_file_md5%", p.getProperty("the_file_md5", ""));
      the_line = Common.replace_str(the_line, "%url%", p.getProperty("url", ""));
      the_line = Common.replace_str(the_line, "%url_2%", p.getProperty("url_2", ""));
      the_line = Common.replace_str(the_line, "%display%", p.getProperty("display", ""));
      the_line = Common.replace_str(the_line, "%all%", p.toString());
      Enumeration keys = p.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        String val = p.get(key).toString();
        the_line = Common.replace_str(the_line, "%" + key + "%", val);
      } 
      the_line = ServerStatus.thisObj.change_vars_to_values(the_line, user, user_info, null);
      if (!the_line.trim().equals(""))
        lineData = String.valueOf(lineData) + the_line + "\r\n"; 
    } 
    Log.log("EVENT", 2, String.valueOf(s) + ":<LINE>" + lineData + "</LINE>");
    try {
      if (s.toUpperCase().indexOf("<LINE>") >= 0)
        s = Common.replace_str(s, s.substring(s.toUpperCase().indexOf("<LINE>"), s.toUpperCase().indexOf("</LINE>") + "</LINE>".length()), lineData); 
    } catch (Exception e) {
      Log.log("EVENT", 1, e);
    } 
    return s;
  }
  
  public String replace_path_url_segments(Properties p, String the_line) {
    String[] pathTokens = p.getProperty("the_file_path", "").split("/");
    int xx;
    for (xx = 1; xx < 20; xx++) {
      String token = "";
      if (pathTokens != null && xx - 1 < pathTokens.length)
        token = pathTokens[xx - 1]; 
      the_line = Common.replace_str(the_line, "%the_file_path" + xx + "%", token);
    } 
    pathTokens = p.getProperty("url", "").split("/");
    for (xx = 1; xx < 20; xx++) {
      String token = "";
      if (pathTokens != null && xx - 1 < pathTokens.length)
        token = pathTokens[xx - 1]; 
      the_line = Common.replace_str(the_line, "%url" + xx + "%", token);
    } 
    return the_line;
  }
  
  public static Properties eventPluginCache = new Properties();
  
  public Properties doEventPlugin(Properties info, Properties event, SessionCrush the_user, Vector items) {
    try {
      Thread.currentThread().setName(String.valueOf(LOC.G("Event:PLUGIN")) + " " + event.getProperty("name"));
      StackTraceElement[] ste = Thread.currentThread().getStackTrace();
      String horizontal_thread = "";
      for (int x = 0; x < ste.length && x < 4; x++)
        horizontal_thread = String.valueOf(horizontal_thread) + "|" + ste[x].getFileName() + ":" + ste[x].getMethodName() + ":" + ste[x].getLineNumber(); 
      Log.log("EVENT", 0, String.valueOf(LOC.G("Event:PLUGIN")) + " " + event.getProperty("name") + ":" + horizontal_thread);
      if (info == null)
        info = new Properties(); 
      info.put("action", "event");
      info.put("server_settings", ServerStatus.server_settings);
      info.put("event", event);
      if (the_user != null)
        info.put("ServerSession", the_user); 
      if (the_user != null)
        info.put("ServerSessionObject", the_user); 
      if (the_user != null)
        info.put("user", the_user.user); 
      if (the_user != null)
        info.put("user_info", the_user.user_info); 
      info.put("items", items);
      String pluginName = event.getProperty("event_plugin_list", "");
      String subItem = "";
      if (pluginName.indexOf(":") >= 0) {
        subItem = pluginName.substring(pluginName.indexOf(":") + 1);
        pluginName = pluginName.substring(0, pluginName.indexOf(":"));
      } 
      Properties cachedPlugin = null;
      Vector cache = null;
      if (pluginName.equalsIgnoreCase("Job")) {
        event = (Properties)event.clone();
        Vector jobs = JobScheduler.getJobList();
        File job = null;
        for (int i = 0; job == null && i < jobs.size(); i++) {
          File f = jobs.elementAt(i);
          if (f.getName().equalsIgnoreCase(subItem))
            job = f; 
        } 
        Properties params = (Properties)Common.readXMLObject(String.valueOf(job.getPath()) + "/job.XML");
        params.put("new_job_id", Common.makeBoundary(20));
        try {
          event.putAll(params);
          event.put("event_plugin_list", params.getProperty("plugin", params.getProperty("event_plugin_list")));
          event.put("name", "ScheduledPluginEvent:" + params.getProperty("scheduleName"));
          boolean override = false;
          if (event.getProperty("async", "").equals("no")) {
            int loops = 0;
            while (loops++ < 600) {
              synchronized (AdminControls.runningSchedules) {
                if (AdminControls.runningSchedules.indexOf(params.getProperty("scheduleName")) < 0) {
                  override = true;
                  AdminControls.runningSchedules.addElement(params.getProperty("scheduleName"));
                  break;
                } 
              } 
              Thread.sleep(1000L);
            } 
          } 
          if (!event.getProperty("async", "").equalsIgnoreCase("no"))
            override = true; 
          if (AdminControls.runningSchedules.indexOf(params.getProperty("scheduleName")) < 0 || override)
            try {
              if (!override)
                AdminControls.runningSchedules.addElement(params.getProperty("scheduleName")); 
              return ServerStatus.thisObj.events6.doEventPlugin(info, event, null, items);
            } finally {
              AdminControls.runningSchedules.remove(params.getProperty("scheduleName"));
            }  
        } catch (Exception e) {
          Log.log("HTTP_SERVER", 1, e);
        } 
      } else if (pluginName.endsWith(" (User Defined)")) {
        pluginName = pluginName.substring(0, pluginName.indexOf(" (User Defined)"));
        Object thePlugin = null;
        cache = (Vector)eventPluginCache.get(pluginName);
        synchronized (eventPluginCache) {
          if (cache == null)
            cache = new Vector(); 
          eventPluginCache.put(pluginName, cache);
          if (cache.size() > 0) {
            cachedPlugin = cache.remove(0);
            thePlugin = cachedPlugin.get("plugin");
            subItem = cachedPlugin.getProperty("subItem");
            Properties defaultPrefs = (Properties)cachedPlugin.get("defaultPrefs");
            defaultPrefs = (Properties)Common.CLONE(defaultPrefs);
            defaultPrefs.putAll(event);
            ServerStatus.thisObj.common_code.setPluginSettings(thePlugin, defaultPrefs);
          } 
        } 
        if (thePlugin == null) {
          subItem = Common.makeBoundary(10);
          thePlugin = Common.getPlugin(pluginName, (new File(String.valueOf(System.getProperty("crushftp.plugins")) + "plugins/")).toURI().toURL().toExternalForm(), subItem);
          Properties defaultPrefs = ServerStatus.thisObj.common_code.getPluginDefaultPrefs(pluginName, subItem);
          cachedPlugin = new Properties();
          cachedPlugin.put("plugin", thePlugin);
          cachedPlugin.put("defaultPrefs", Common.CLONE(defaultPrefs));
          cachedPlugin.put("subItem", subItem);
          defaultPrefs.putAll(event);
          ServerStatus.thisObj.common_code.setPluginSettings(thePlugin, defaultPrefs);
        } 
      } 
      Log.log("EVENT", 0, String.valueOf(LOC.G("Event:PLUGIN")) + " " + event.getProperty("name") + ":" + pluginName + ":" + subItem + ":");
      Common.runPlugin(pluginName, info, subItem);
      if (cachedPlugin != null && cache != null)
        cache.addElement(cachedPlugin); 
      return info;
    } catch (Exception e) {
      Log.log("EVENT", 1, e);
      Log.log("EVENT", 0, String.valueOf(LOC.G("FAILURE:")) + event.getProperty("name") + ":" + e + "\r\n");
      return info;
    } 
  }
}
