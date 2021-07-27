package crushftp.handlers;

import com.crushftp.client.Worker;
import crushftp.gui.LOC;
import crushftp.server.ServerStatus;
import crushftp.server.daemon.ServerBeat;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class JobScheduler {
  static long lastOldCheck = 0L;
  
  static final long day = 86400000L;
  
  public static Vector getJobList() {
    Vector jobs = new Vector();
    String[] list = (new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/")).list();
    if (list != null)
      Arrays.sort((Object[])list); 
    for (int x = 0; list != null && x < list.length; x++) {
      File f = new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + list[x]);
      if (!f.isFile() && ((
        new File(String.valueOf(f.getPath()) + "/job.XML")).exists() || (new File(String.valueOf(f.getPath()) + "/inprogress")).exists()))
        jobs.addElement(f); 
    } 
    return jobs;
  }
  
  public static String safeName(String scheduleName) {
    scheduleName = Common.replace_str(scheduleName, ":", "_");
    scheduleName = Common.replace_str(scheduleName, "#", "_");
    scheduleName = Common.replace_str(scheduleName, "@", "_");
    scheduleName = Common.replace_str(scheduleName, "!", "_");
    scheduleName = Common.replace_str(scheduleName, "&", "_");
    scheduleName = Common.replace_str(scheduleName, "/", "_");
    scheduleName = Common.replace_str(scheduleName, "\\", "_");
    scheduleName = Common.replace_str(scheduleName, ";", "_");
    scheduleName = Common.replace_str(scheduleName, "<", "_");
    scheduleName = Common.replace_str(scheduleName, ">", "_");
    return scheduleName;
  }
  
  public static void runSchedules(Properties pp) {
    if (!ServerStatus.BG("job_scheduler_enabled"))
      return; 
    SimpleDateFormat time = new SimpleDateFormat("hh:mm aa", Locale.US);
    Properties day_lookup = new Properties();
    day_lookup.put("SUN", "(1)");
    day_lookup.put("MON", "(2)");
    day_lookup.put("TUE", "(3)");
    day_lookup.put("WED", "(4)");
    day_lookup.put("THU", "(5)");
    day_lookup.put("FRI", "(6)");
    day_lookup.put("SAT", "(7)");
    boolean purge_old = (System.currentTimeMillis() - lastOldCheck > 3600000L);
    purge_old = true;
    if (purge_old)
      lastOldCheck = System.currentTimeMillis(); 
    Vector jobs = getJobList();
    if (!ServerBeat.current_master)
      if (ServerStatus.BG("single_job_scheduler_serverbeat"))
        return;  
    SimpleDateFormat yyMMddHHmm = new SimpleDateFormat("yyMMddHHmm");
    SimpleDateFormat yyMMdd = new SimpleDateFormat("yyMMdd");
    for (int x = 0; x < jobs.size(); x++) {
      boolean runSchedule = false;
      Properties p = null;
      File job = jobs.elementAt(x);
      if (!job.getName().startsWith("_"))
        try {
          Calendar c = new GregorianCalendar();
          p = (Properties)Common.readXMLObject(String.valueOf(job.getPath()) + "/job.XML");
          if (p != null) {
            Log.log("SERVER", 1, "Checking time on job:" + job.getName());
            p.put("scheduleName", job.getName());
            if (!p.getProperty("enabled", "").equalsIgnoreCase("true"))
              continue; 
            long nextRun = Long.parseLong(p.getProperty("nextRun", "0"));
            c.setTimeInMillis(yyMMddHHmm.parse(yyMMddHHmm.format(new Date())).getTime());
            if (p.getProperty("scheduleTime", "").trim().startsWith(","))
              p.put("scheduleTime", p.getProperty("scheduleTime", "").trim().substring(1)); 
            if (p.getProperty("scheduleTime", "").trim().endsWith(","))
              p.put("scheduleTime", p.getProperty("scheduleTime", "").trim().substring(p.getProperty("scheduleTime", "").trim().length() - 1)); 
            if (p.getProperty("scheduleType", "").equals("minutely") && (new Date()).getTime() > nextRun) {
              int minutes = Integer.parseInt(p.getProperty("minutelyAmount"));
              if (minutes == 0)
                minutes = 1; 
              if (minutes > 0) {
                c.add(12, minutes);
              } else {
                c.add(13, minutes * -1);
              } 
              runSchedule = true;
            } else if (!p.getProperty("scheduleType", "").equals("minutely")) {
              String[] schedule_time = p.getProperty("scheduleTime", "").split(",");
              for (int xx = 0; xx < schedule_time.length && (!runSchedule || nextRun == -1L); xx++) {
                boolean last_time = (xx == schedule_time.length - 1);
                if (!schedule_time[xx].trim().equals("")) {
                  if ((nextRun == -1L || time.format(new Date()).equals(time.format(time.parse(schedule_time[xx].trim())))) && System.currentTimeMillis() > nextRun) {
                    Log.log("SERVER", 1, "Scheduled to run:" + job.getName());
                    if (p.getProperty("scheduleType", "").equals("daily")) {
                      String next_time = null;
                      if (last_time) {
                        c.add(12, -1);
                        c.add(5, Integer.parseInt(p.getProperty("dailyAmount")));
                        c.setTimeInMillis(yyMMdd.parse(yyMMdd.format(c.getTime())).getTime());
                        next_time = schedule_time[0];
                      } else {
                        if (nextRun >= 0L)
                          c.add(12, 1); 
                        if (nextRun < 0L) {
                          next_time = schedule_time[xx];
                        } else {
                          next_time = schedule_time[xx + 1];
                        } 
                      } 
                      while (!time.format(c.getTime()).equals(time.format(time.parse(next_time.trim()))))
                        c.add(12, 1); 
                      runSchedule = true;
                    } else if (p.getProperty("scheduleType", "").equals("weekly")) {
                      String day = day_lookup.getProperty((new SimpleDateFormat("EEE", Locale.US)).format(new Date()).toUpperCase());
                      if (p.getProperty("weekDays", "").trim().equals("")) {
                        Log.log("SERVER", 0, "Cannot schedule " + job.getName() + " because of an invalid schedule configuration.  No week days chosen.");
                      } else if (nextRun == -1L || p.getProperty("weekDays", "").indexOf(day) >= 0) {
                        c.setTime(new Date());
                        if (p.getProperty("weekDays", "").indexOf(day) + 1 == (p.getProperty("weekDays", "").split("\\)")).length && last_time) {
                          while (c.get(12) != 0)
                            c.add(12, -1); 
                          while (c.get(7) != 1)
                            c.add(10, -1); 
                          while (c.get(7) == 1)
                            c.add(10, -1); 
                          c.add(10, 1);
                          for (int loops = 0; loops < Integer.parseInt(p.getProperty("weeklyAmount")); loops++) {
                            while (c.get(7) == 1)
                              c.add(10, 1); 
                            while (c.get(7) != 1)
                              c.add(10, 1); 
                          } 
                          c.setTimeInMillis(yyMMdd.parse(yyMMdd.format(c.getTime())).getTime());
                        } else if (last_time && nextRun != -1L) {
                          c.add(12, -1);
                          c.add(5, 1);
                          c.setTimeInMillis(yyMMdd.parse(yyMMdd.format(c.getTime())).getTime());
                        } 
                        String next_time = null;
                        if (last_time || nextRun == -1L) {
                          next_time = schedule_time[0];
                        } else {
                          c.add(12, 1);
                          next_time = schedule_time[xx + 1];
                        } 
                        while (!time.format(c.getTime()).equals(time.format(time.parse(next_time.trim()))))
                          c.add(12, 1); 
                        if (nextRun == -1L || last_time) {
                          day = day_lookup.getProperty((new SimpleDateFormat("EEE", Locale.US)).format(c.getTime()).toUpperCase());
                          while (p.getProperty("weekDays", "").indexOf(day) < 0) {
                            c.add(5, 1);
                            day = day_lookup.getProperty((new SimpleDateFormat("EEE", Locale.US)).format(c.getTime()).toUpperCase());
                          } 
                        } 
                        runSchedule = true;
                      } 
                    } else if (p.getProperty("scheduleType", "").equals("monthly")) {
                      SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
                      SimpleDateFormat d = new SimpleDateFormat("d", Locale.US);
                      String day1 = "(" + d.format(new Date()) + ")";
                      String day2 = "(" + dd.format(new Date()) + ")";
                      if (p.getProperty("monthDays", "").trim().equals("")) {
                        Log.log("SERVER", 0, "Cannot schedule " + job.getName() + " because of an invalid schedule configuration.  No month days chosen.");
                      } else if (nextRun == -1L || p.getProperty("monthDays", "").indexOf(day1) >= 0 || p.getProperty("monthDays", "").indexOf(day2) >= 0) {
                        if (last_time && (p.getProperty("monthDays", "").indexOf(day1) == p.getProperty("monthDays", "").length() - day1.length() || p.getProperty("monthDays", "").indexOf(day2) == p.getProperty("monthDays", "").length() - day2.length())) {
                          c.setTime(new Date());
                          while (c.get(5) != 1)
                            c.add(5, -1); 
                          c.add(2, Integer.parseInt(p.getProperty("monthlyAmount")));
                          c.setTimeInMillis(yyMMdd.parse(yyMMdd.format(c.getTime())).getTime());
                        } 
                        String next_time = null;
                        if (last_time || nextRun == -1L) {
                          next_time = schedule_time[0];
                        } else {
                          c.add(12, 1);
                          next_time = schedule_time[xx + 1];
                        } 
                        while (!time.format(c.getTime()).equals(time.format(time.parse(next_time.trim()))))
                          c.add(12, 1); 
                        if (nextRun == -1L) {
                          day1 = "(" + d.format(c.getTime()) + ")";
                          day2 = "(" + dd.format(c.getTime()) + ")";
                          while (p.getProperty("monthDays", "").indexOf(day1) < 0 && p.getProperty("monthDays", "").indexOf(day2) < 0) {
                            c.add(12, 1);
                            day1 = "(" + d.format(c.getTime()) + ")";
                            day2 = "(" + dd.format(c.getTime()) + ")";
                          } 
                          while (!time.format(c.getTime()).equals(time.format(time.parse(next_time.trim()))))
                            c.add(12, 1); 
                        } 
                        runSchedule = true;
                      } 
                    } 
                  } 
                  if (nextRun == -1L)
                    break; 
                } 
              } 
            } 
            if (runSchedule || nextRun == -1L) {
              c.setTimeInMillis(yyMMddHHmm.parse(yyMMddHHmm.format(c.getTime())).getTime());
              p.put("nextRun", (new StringBuffer(String.valueOf(c.getTimeInMillis()))).toString());
              String uid = Common.makeBoundary(6);
              Common.writeXMLObject(String.valueOf(job.getPath()) + "/job." + uid + "_new.XML", p, "job");
              if ((new File(String.valueOf(job.getPath()) + "/job." + uid + "_new.XML")).length() > 0L) {
                (new File(String.valueOf(job.getPath()) + "/job." + uid + "_old.XML")).delete();
                if ((new File(String.valueOf(job.getPath()) + "/job.XML")).renameTo(new File(String.valueOf(job.getPath()) + "/job." + uid + "_old.XML")))
                  if ((new File(String.valueOf(job.getPath()) + "/job." + uid + "_new.XML")).renameTo(new File(String.valueOf(job.getPath()) + "/job.XML")))
                    (new File(String.valueOf(job.getPath()) + "/job." + uid + "_old.XML")).delete();  
                if (ServerStatus.siIG("enterprise_level") <= 0)
                  throw new Exception("Job Scheduler feature is only for Enterprise licenses."); 
                if (nextRun >= 0L) {
                  Properties p2 = p;
                  Worker.startWorker(new Runnable(p2) {
                        private final Properties val$p2;
                        
                        public void run() {
                          JobScheduler.runSchedule(this.val$p2);
                        }
                      });
                } 
              } else {
                throw new Exception("Job Scheduler failed to update next run time for job, cancelling scheduled time:" + job);
              } 
            } 
          } 
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
          Log.log("SERVER", 0, (String)p);
        }  
      if (purge_old) {
        lastOldCheck = System.currentTimeMillis();
        File job2 = job;
        try {
          Worker.startWorker(new Runnable(job2) {
                private final File val$job2;
                
                public void run() {
                  String name = this.val$job2.getName();
                  if (name.startsWith("__"))
                    try {
                      Long.parseLong(name.substring(name.length() - 13));
                      if ((new File(String.valueOf(this.val$job2.getPath()) + "/job.XML")).exists()) {
                        if (System.currentTimeMillis() - (new File(String.valueOf(this.val$job2.getPath()) + "/job.XML")).lastModified() >= ServerStatus.LG("recent_temp_job_days") * 86400000L)
                          Common.recurseDelete(String.valueOf(this.val$job2.getPath()) + "/", false); 
                      } else if ((new File(String.valueOf(this.val$job2.getPath()) + "/inprogress/")).exists()) {
                        if (System.currentTimeMillis() - (new File(String.valueOf(this.val$job2.getPath()) + "/inprogress/")).lastModified() >= ServerStatus.LG("recent_temp_job_days") * 86400000L)
                          Common.recurseDelete(String.valueOf(this.val$job2.getPath()) + "/", false); 
                      } 
                    } catch (Exception exception) {} 
                  JobScheduler.lastOldCheck = System.currentTimeMillis();
                  File[] olds = this.val$job2.listFiles();
                  JobScheduler.lastOldCheck = System.currentTimeMillis();
                  for (int xx = 0; olds != null && xx < olds.length; xx++) {
                    JobScheduler.lastOldCheck = System.currentTimeMillis();
                    File old = olds[xx];
                    name = old.getName();
                    if (name.toUpperCase().endsWith(".XML") && !name.equalsIgnoreCase("job.XML") && !name.equalsIgnoreCase("inprogress.XML") && !name.equalsIgnoreCase("inprogress"))
                      if (System.currentTimeMillis() - old.lastModified() > ServerStatus.LG("recent_job_days") * 86400000L)
                        if (System.currentTimeMillis() - old.lastModified() > 300000L)
                          old.delete();   
                  } 
                }
              });
        } catch (IOException e) {
          Log.log("SERVER", 1, e);
        } 
      } 
      continue;
    } 
  }
  
  public static boolean jobRunning(String scheduleName) {
    boolean ok = true;
    File[] inprogress = (new File(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + scheduleName + "/inprogress/")).listFiles();
    if (inprogress != null)
      for (int i = 0; ok && i < inprogress.length; i++) {
        if (inprogress[i].getName().toUpperCase().endsWith(".XML") || !inprogress[i].getName().startsWith("."))
          ok = false; 
      }  
    Vector jobs = (Vector)ServerStatus.thisObj.server_info.get("running_tasks");
    for (int x = 0; ok && x < jobs.size(); x++) {
      Properties tracker = jobs.elementAt(x);
      Properties settings = (Properties)tracker.get("settings");
      if (settings == null)
        settings = new Properties(); 
      if (scheduleName.equalsIgnoreCase(settings.getProperty("scheduleName", "")))
        ok = false; 
    } 
    return !ok;
  }
  
  public static void runSchedule(Properties p) {
    boolean ok = true;
    if (p.getProperty("scheduleName", "").toUpperCase().endsWith("_SINGLE") || p.getProperty("single", "").equals("true"))
      ok = !jobRunning(p.getProperty("scheduleName", "")); 
    if (p.getProperty("singleServer", "").equals("true") && ok) {
      ok = !jobRunning(p.getProperty("scheduleName", ""));
      if (ok) {
        Properties pp = new Properties();
        pp.put("scheduleName", p.getProperty("scheduleName"));
        pp.put("need_response", "true");
        SharedSessionReplicated.send(Common.makeBoundary(), "crushftp.JobScheduler.jobRunning", "info", pp);
        long start = System.currentTimeMillis();
        while (pp.getProperty("response_num", "0").equals("0") && System.currentTimeMillis() - start < 5000L) {
          try {
            Thread.sleep(100L);
          } catch (Exception exception) {}
        } 
        Properties val = (Properties)pp.get("val");
        if (val != null) {
          Enumeration keys = val.keys();
          while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            if (key.startsWith("running_") && val.getProperty(key).equalsIgnoreCase("true"))
              ok = false; 
          } 
        } 
      } 
    } 
    if (ok) {
      Thread runIt = new Thread(new null.runner(p));
      runIt.setPriority(1);
      runIt.setName("schedule:" + p.getProperty("scheduleName") + ":" + new Date());
      runIt.start();
      Log.log("SERVER", 0, LOC.G("Ran Schedule") + ":" + p.getProperty("scheduleName") + ":" + new Date());
      Log.log("SERVER", 2, "Schedule Config:" + p.toString());
    } else {
      Log.log("SERVER", 0, "Skipping scheduled job since its still running:" + p.getProperty("scheduleName"));
    } 
  }
}
