package crushftp.handlers;

import crushftp.gui.LOC;
import crushftp.server.ServerStatus;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class JobScheduler {
  public static void runSchedules(Properties pp) {
    boolean ranASchedule = false;
    SimpleDateFormat time = new SimpleDateFormat("hh:mm aa");
    if (ServerStatus.server_settings.get("schedules") == null)
      return; 
    Vector schedules = (Vector)ServerStatus.server_settings.get("schedules");
    for (int x = 0; x < schedules.size(); x++) {
      boolean runSchedule = false;
      Properties p = null;
      try {
        p = schedules.elementAt(x);
        long nextRun = Long.parseLong(p.getProperty("nextRun", "0"));
        long newNextRun = (new Date()).getTime();
        if (p.getProperty("scheduleType", "").equals("minutely") && (new Date()).getTime() > nextRun) {
          runSchedule = true;
          Calendar c = new GregorianCalendar();
          c.setTime(new Date());
          c.add(12, Integer.parseInt(p.getProperty("minutelyAmount")));
          newNextRun = c.getTime().getTime();
        } else if (!p.getProperty("scheduleType", "").equals("minutely")) {
          String[] schedule_time = p.getProperty("scheduleTime").split(",");
          for (int xx = 0; xx < schedule_time.length && !runSchedule; xx++) {
            if (time.format(new Date()).equals(time.format(time.parse(schedule_time[xx].trim()))) && (new Date()).getTime() > nextRun)
              if (p.getProperty("scheduleType", "").equals("daily")) {
                while (time.format(new Date(newNextRun)).equals(time.format(new Date())))
                  newNextRun += 30000L; 
                runSchedule = true;
              } else if (p.getProperty("scheduleType", "").equals("weekly")) {
                String day = "";
                String today_date = (new SimpleDateFormat("EEE", Locale.US)).format(new Date()).toUpperCase();
                if (today_date.equals("SUN"))
                  day = "(1)"; 
                if (today_date.equals("MON"))
                  day = "(2)"; 
                if (today_date.equals("TUE"))
                  day = "(3)"; 
                if (today_date.equals("WED"))
                  day = "(4)"; 
                if (today_date.equals("THU"))
                  day = "(5)"; 
                if (today_date.equals("FRI"))
                  day = "(6)"; 
                if (today_date.equals("SAT"))
                  day = "(7)"; 
                if (p.getProperty("weekDays", "").indexOf(day) >= 0) {
                  if (p.getProperty("weekDays", "").indexOf(day) == p.getProperty("weekDays", "").length() - day.length()) {
                    Calendar c = new GregorianCalendar();
                    c.setTime(new Date());
                    while (c.get(7) != 1)
                      c.add(5, -1); 
                    c.add(5, 7 * Integer.parseInt(p.getProperty("weeklyAmount")));
                    c.add(10, -1);
                    newNextRun = c.getTime().getTime();
                  } 
                  runSchedule = true;
                } 
              } else if (p.getProperty("scheduleType", "").equals("monthly")) {
                SimpleDateFormat dd = new SimpleDateFormat("dd");
                SimpleDateFormat d = new SimpleDateFormat("d");
                String day1 = "(" + d.format(new Date()) + ")";
                String day2 = "(" + dd.format(new Date()) + ")";
                if (p.getProperty("monthDays", "").indexOf(day1) >= 0 || p.getProperty("monthDays", "").indexOf(day2) >= 0) {
                  if (p.getProperty("monthDays", "").indexOf(day1) == p.getProperty("monthDays", "").length() - day1.length() || p.getProperty("monthDays", "").indexOf(day2) == p.getProperty("monthDays", "").length() - day2.length()) {
                    Calendar c = new GregorianCalendar();
                    c.setTime(new Date());
                    while (c.get(5) != 1)
                      c.add(5, -1); 
                    c.add(2, Integer.parseInt(p.getProperty("monthlyAmount")));
                    c.add(10, -1);
                    newNextRun = c.getTime().getTime();
                  } 
                  runSchedule = true;
                } 
              }  
          } 
        } 
        if (runSchedule || nextRun == -1L) {
          p.put("nextRun", (new StringBuffer(String.valueOf(newNextRun))).toString());
          ranASchedule = true;
          if (ServerStatus.siIG("enterprise_level") <= 0)
            throw new Exception("Job Scheduler feature is only for Enterprise licenses."); 
          runSchedule(p);
        } 
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        Log.log("SERVER", 0, (String)p);
      } 
    } 
    if (ranASchedule)
      ServerStatus.thisObj.save_server_settings(false); 
  }
  
  public static void runSchedule(Properties p) {
    boolean ok = true;
    if (p.getProperty("scheduleName", "").toUpperCase().endsWith("_SINGLE")) {
      Vector jobs = ServerStatus.siVG("running_tasks");
      for (int x = 0; x < jobs.size() && ok; x++) {
        Properties job = jobs.elementAt(x);
        Properties settings = (Properties)job.get("settings");
        if (settings == null)
          settings = new Properties(); 
        if (settings.getProperty("scheduleName", "").equals(p.getProperty("scheduleName", "")) && job.getProperty("status", "").equalsIgnoreCase("RUNNING"))
          ok = false; 
      } 
    } 
    if (ok) {
      Thread runIt = new Thread(new JobScheduler$1$runner(p));
      runIt.setPriority(1);
      runIt.setName("schedule:" + p.getProperty("scheduleName") + ":" + new Date());
      runIt.start();
      Log.log("SERVER", 0, LOC.G("Ran Schedule") + ":" + p.getProperty("scheduleName") + ":" + new Date());
      Log.log("SERVER", 1, "Schedule Config:" + p.toString());
    } else {
      Log.log("SERVER", 0, "Skipping scheduled job since its still running:" + p.getProperty("scheduleName"));
    } 
  }
}
