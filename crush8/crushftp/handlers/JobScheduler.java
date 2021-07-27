package crushftp.handlers;

import com.crushftp.client.File_S;
import com.crushftp.client.Worker;
import crushftp.gui.LOC;
import crushftp.server.AdminControls;
import crushftp.server.ServerStatus;
import crushftp.server.daemon.ServerBeat;
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
  
  public static Vector getJobList(boolean include_events) {
    Vector jobs = new Vector();
    String[] list = (new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/")).list();
    addJobs(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/", list, jobs, include_events);
    return jobs;
  }
  
  public static void addJobs(String path, String[] list, Vector jobs, boolean include_events) {
    if (list != null)
      Arrays.sort((Object[])list); 
    for (int x = 0; list != null && x < list.length; x++) {
      File_S f = new File_S(String.valueOf(path) + list[x]);
      if (!f.isFile() && (
        include_events || !f.getName().startsWith("__"))) {
        if (!(new File_S(String.valueOf(f.getPath()) + "/job.XML")).exists()) {
          String[] sub_list = f.list();
          addJobs(String.valueOf(path) + f.getName() + "/", sub_list, jobs, include_events);
        } 
        if ((new File_S(String.valueOf(f.getPath()) + "/job.XML")).exists() || (new File_S(String.valueOf(f.getPath()) + "/inprogress")).exists())
          jobs.addElement(f); 
      } 
    } 
  }
  
  public static String safeName(String scheduleName) {
    scheduleName = Common.replace_str(scheduleName, ":", "_");
    scheduleName = Common.replace_str(scheduleName, "#", "_");
    scheduleName = Common.replace_str(scheduleName, "@", "_");
    scheduleName = Common.replace_str(scheduleName, "!", "_");
    scheduleName = Common.replace_str(scheduleName, "&", "_");
    scheduleName = Common.replace_str(scheduleName, "\\", "_");
    scheduleName = Common.replace_str(scheduleName, ";", "_");
    scheduleName = Common.replace_str(scheduleName, "<", "_");
    scheduleName = Common.replace_str(scheduleName, ">", "_");
    return scheduleName;
  }
  
  public static void runSchedules(Properties pp) {
    if (!ServerStatus.BG("job_scheduler_enabled"))
      return; 
    if (System.getProperty("crushftp.singleuser", "false").equals("true"))
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
    Vector jobs = getJobList(false);
    if (!ServerBeat.current_master)
      if (ServerStatus.BG("single_job_scheduler_serverbeat"))
        return;  
    SimpleDateFormat yyMMddHHmm = new SimpleDateFormat("yyMMddHHmm", Locale.US);
    SimpleDateFormat yyMMdd = new SimpleDateFormat("yyMMdd", Locale.US);
    for (int x = 0; x < jobs.size(); x++) {
      boolean runSchedule = false;
      Properties p = null;
      File_S job = jobs.elementAt(x);
      if (!job.getName().startsWith("_"))
        try {
          Calendar c = new GregorianCalendar();
          p = (Properties)Common.readXMLObject(String.valueOf(job.getPath()) + "/job.XML");
          if (p != null) {
            Log.log("JOB_SCHEDULER", 0, "Checking time on job:" + job.getName());
            p.put("scheduleName", AdminControls.jobName(job));
            if (p.getProperty("enabled", "").equalsIgnoreCase("true")) {
              long nextRun = Long.parseLong(p.getProperty("nextRun", "0"));
              if (nextRun > System.currentTimeMillis())
                Log.log("JOB_SCHEDULER", 0, "Next run of the job: " + job.getName() + ": " + new Date(nextRun)); 
              c.setTimeInMillis(yyMMddHHmm.parse(yyMMddHHmm.format(new Date())).getTime());
              if (p.getProperty("scheduleTime", "").trim().startsWith(","))
                p.put("scheduleTime", p.getProperty("scheduleTime", "").trim().substring(1)); 
              if (p.getProperty("scheduleTime", "").trim().endsWith(","))
                p.put("scheduleTime", p.getProperty("scheduleTime", "").trim().substring(p.getProperty("scheduleTime", "").trim().length() - 1)); 
              if (p.getProperty("scheduleType", "").equals("minutely") && (new Date()).getTime() > nextRun) {
                Log.log("JOB_SCHEDULER", 0, "Calculating next run of job: " + job.getName());
                Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Minutely: " + "Scheduled time minutely amount: " + p.getProperty("minutelyAmount", ""));
                Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Minutely: Before the calculation: " + ":" + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                int minutes = Integer.parseInt(p.getProperty("minutelyAmount"));
                if (minutes == 0)
                  minutes = 1; 
                if (minutes > 0) {
                  c.add(12, minutes);
                } else {
                  c.add(13, minutes * -1);
                } 
                runSchedule = true;
                Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Minutely: Next run of the job: " + ":" + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
              } else if (!p.getProperty("scheduleType", "").equals("minutely")) {
                String[] schedule_time = p.getProperty("scheduleTime", "").split(",");
                if (p.getProperty("scheduleTime", "").indexOf(",0:") >= 0) {
                  String schedule_time2 = "";
                  for (int i = 0; i < schedule_time.length; i++) {
                    String next_time = schedule_time[i].trim();
                    if (next_time.startsWith("0:"))
                      next_time = "12:" + next_time.substring(2); 
                    schedule_time2 = String.valueOf(schedule_time2) + ((i == 0) ? "" : ",") + next_time;
                  } 
                  p.put("scheduleTime", schedule_time2);
                  schedule_time = p.getProperty("scheduleTime", "").split(",");
                } 
                for (int xx = 0; xx < schedule_time.length && (!runSchedule || nextRun == -1L); xx++) {
                  boolean last_time = (xx == schedule_time.length - 1);
                  if (!schedule_time[xx].trim().equals("")) {
                    if ((nextRun == -1L || time.format(new Date()).equals(time.format(time.parse(schedule_time[xx].trim())))) && System.currentTimeMillis() > nextRun) {
                      if (nextRun == -1L) {
                        Log.log("JOB_SCHEDULER", 0, "Calculating next run of job: " + job.getName());
                      } else {
                        Log.log("JOB_SCHEDULER", 0, "Scheduled to run:" + job.getName());
                      } 
                      Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Scheduled type: " + p.getProperty("scheduleType", ""));
                      Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Scheduled time: " + p.getProperty("scheduleTime", ""));
                      if (p.getProperty("scheduleType", "").equals("daily")) {
                        Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Daily: Before the calculation: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                        Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Daily: Checking last_time on job: " + last_time);
                        String next_time = null;
                        if (last_time) {
                          if (nextRun != -1L) {
                            c.add(12, -1);
                            c.add(5, Integer.parseInt(p.getProperty("dailyAmount")));
                          } 
                          c.setTimeInMillis(yyMMdd.parse(yyMMdd.format(c.getTime())).getTime());
                          Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Daily: Midnight of the day specified: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                          next_time = schedule_time[0];
                        } else {
                          if (nextRun >= 0L) {
                            c.add(12, 1);
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Daily: Advance one minute: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                          } 
                          if (nextRun < 0L) {
                            next_time = schedule_time[xx];
                          } else {
                            next_time = schedule_time[xx + 1];
                          } 
                        } 
                        if (nextRun == -1L)
                          for (int xxx = 0; xxx < schedule_time.length; xxx++) {
                            String daily_time = schedule_time[xxx];
                            if (time.parse(daily_time.trim()).getTime() > time.parse(time.format(c.getTime())).getTime()) {
                              next_time = daily_time;
                              break;
                            } 
                          }  
                        Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Daily: Checking next time on job: " + next_time + ":" + time.format(time.parse(next_time.trim())));
                        while (!time.format(c.getTime()).equals(time.format(time.parse(next_time.trim()))))
                          c.add(12, 1); 
                        runSchedule = true;
                        if (nextRun == -1L && System.currentTimeMillis() > c.getTimeInMillis())
                          c.add(5, Integer.parseInt(p.getProperty("dailyAmount"))); 
                        Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Daily: Checking calendar on job: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                      } else if (p.getProperty("scheduleType", "").equals("weekly")) {
                        String day = day_lookup.getProperty((new SimpleDateFormat("EEE", Locale.US)).format(new Date()).toUpperCase());
                        if (p.getProperty("weekDays", "").trim().equals("")) {
                          Log.log("SERVER", 0, "Cannot schedule " + job.getName() + " because of an invalid schedule configuration.  No week days chosen.");
                        } else if (nextRun == -1L || p.getProperty("weekDays", "").indexOf(day) >= 0) {
                          Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly:" + " Week days:" + p.getProperty("weekDays", ""));
                          c.setTime(new Date());
                          Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: Before the calculation: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                          String[] weekDays = p.getProperty("weekDays", "").split("\\)");
                          if (day.indexOf(weekDays[weekDays.length - 1]) >= 0 && last_time) {
                            while (c.get(12) != 0)
                              c.add(12, -1); 
                            while (c.get(7) != 1)
                              c.add(10, -1); 
                            while (c.get(7) == 1)
                              c.add(10, -1); 
                            c.add(10, 1);
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: " + " Scheduled time weekly amount: " + p.getProperty("weeklyAmount", ""));
                            for (int loops = 0; loops < Integer.parseInt(p.getProperty("weeklyAmount")); loops++) {
                              while (c.get(7) == 1)
                                c.add(10, 1); 
                              while (c.get(7) != 1)
                                c.add(10, 1); 
                            } 
                            c.setTimeInMillis(yyMMdd.parse(yyMMdd.format(c.getTime())).getTime());
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: " + " Day from week days: " + day + ":" + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                          } else if (last_time && nextRun != -1L) {
                            c.add(12, -1);
                            c.add(5, 1);
                            c.setTimeInMillis(yyMMdd.parse(yyMMdd.format(c.getTime())).getTime());
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: added one day : " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                          } 
                          String next_time = null;
                          if (last_time || nextRun == -1L) {
                            next_time = schedule_time[0];
                          } else {
                            c.add(12, 1);
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: Advance one minute: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                            next_time = schedule_time[xx + 1];
                          } 
                          Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: Next time : " + next_time);
                          if (nextRun == -1L) {
                            boolean time_matched = false;
                            String next_time2 = next_time;
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: Next time for calculation: " + next_time);
                            while (!time.format(c.getTime()).equals(time.format(time.parse(next_time.trim()))) && !time_matched) {
                              for (int xxx = 0; xxx < schedule_time.length && !time_matched; xxx++) {
                                next_time2 = schedule_time[xxx];
                                if (time.format(c.getTime()).equals(time.format(time.parse(next_time2.trim()))))
                                  time_matched = true; 
                              } 
                              if (!time_matched)
                                c.add(12, 1); 
                            } 
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: Calculated next scheduled time: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                          } else {
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly Days: Next time: " + next_time);
                            while (!time.format(c.getTime()).equals(time.format(time.parse(next_time.trim()))))
                              c.add(12, 1); 
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: Next scheduled time: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                          } 
                          if (nextRun == -1L || last_time) {
                            day = day_lookup.getProperty((new SimpleDateFormat("EEE", Locale.US)).format(c.getTime()).toUpperCase());
                            while (p.getProperty("weekDays", "").indexOf(day) < 0) {
                              c.add(5, 1);
                              day = day_lookup.getProperty((new SimpleDateFormat("EEE", Locale.US)).format(c.getTime()).toUpperCase());
                            } 
                          } 
                          runSchedule = true;
                          Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Weekly: Checking calendar on job: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                        } 
                      } else if (p.getProperty("scheduleType", "").equals("monthly")) {
                        SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
                        SimpleDateFormat d = new SimpleDateFormat("d", Locale.US);
                        String day1 = "(" + d.format(new Date()) + ")";
                        String day2 = "(" + dd.format(new Date()) + ")";
                        if (p.getProperty("monthDays", "").trim().equals("")) {
                          Log.log("SERVER", 0, "Cannot schedule " + job.getName() + " because of an invalid schedule configuration.  No month days chosen.");
                        } else if (nextRun == -1L || p.getProperty("monthDays", "").indexOf(day1) >= 0 || p.getProperty("monthDays", "").indexOf(day2) >= 0) {
                          Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: Before the calculation: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                          Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: Month days: " + p.getProperty("monthDays", ""));
                          String[] monthDays = p.getProperty("monthDays", "").split("\\)");
                          if (last_time && (day1.indexOf(monthDays[monthDays.length - 1]) >= 0 || day2.indexOf(monthDays[monthDays.length - 1]) >= 0)) {
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: day1: " + day1);
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: day2: " + day2);
                            c.setTime(new Date());
                            while (c.get(5) != 1)
                              c.add(5, -1); 
                            if (nextRun != -1L)
                              c.add(2, Integer.parseInt(p.getProperty("monthlyAmount"))); 
                            c.setTimeInMillis(yyMMdd.parse(yyMMdd.format(c.getTime())).getTime());
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: After processed days: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                          } 
                          String next_time = null;
                          if (last_time || nextRun == -1L) {
                            next_time = schedule_time[0];
                          } else {
                            c.add(12, 1);
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: One minute in advance: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                            next_time = schedule_time[xx + 1];
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: Next time: " + next_time);
                          } 
                          if (nextRun == -1L) {
                            for (int xxx = 0; xxx < schedule_time.length; xxx++) {
                              String daily_time = schedule_time[xxx];
                              if (time.parse(daily_time.trim()).getTime() > time.parse(time.format(c.getTime())).getTime()) {
                                next_time = daily_time;
                                break;
                              } 
                            } 
                            Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: Nearest next time: " + next_time);
                          } 
                          while (!time.format(c.getTime()).equals(time.format(time.parse(next_time.trim()))))
                            c.add(12, 1); 
                          Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: Advance it up to the first time interval: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
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
                            if (System.currentTimeMillis() > c.getTimeInMillis())
                              c.add(2, Integer.parseInt(p.getProperty("monthlyAmount"))); 
                          } 
                          runSchedule = true;
                          Log.log("JOB_SCHEDULER", 1, String.valueOf(job.getName()) + ":" + " Monthly: Checking calendar on job: " + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
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
                Log.log("JOB_SCHEDULER", 1, "Calculated next run of the job:" + job.getName() + ":" + c.getTimeInMillis() + ":" + new Date(c.getTimeInMillis()));
                String uid = Common.makeBoundary(6);
                Common.writeXMLObject(String.valueOf(job.getPath()) + "/job." + uid + "_new.XML", p, "job");
                if ((new File_S(String.valueOf(job.getPath()) + "/job." + uid + "_new.XML")).length() > 0L) {
                  (new File_S(String.valueOf(job.getPath()) + "/job." + uid + "_old.XML")).delete();
                  if ((new File_S(String.valueOf(job.getPath()) + "/job.XML")).renameTo(new File_S(String.valueOf(job.getPath()) + "/job." + uid + "_old.XML")))
                    if ((new File_S(String.valueOf(job.getPath()) + "/job." + uid + "_new.XML")).renameTo(new File_S(String.valueOf(job.getPath()) + "/job.XML")))
                      (new File_S(String.valueOf(job.getPath()) + "/job." + uid + "_old.XML")).delete();  
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
          } 
        } catch (Exception e) {
          Log.log("SERVER", 0, e);
          Log.log("SERVER", 0, (String)p);
        }  
    } 
    boolean purge_old = (System.currentTimeMillis() - lastOldCheck > 3600000L);
    if (purge_old) {
      lastOldCheck = System.currentTimeMillis();
      try {
        Worker.startWorker(new Runnable() {
              public void run() {
                Vector jobs = JobScheduler.getJobList(true);
                JobScheduler.lastOldCheck = System.currentTimeMillis();
                for (int x = 0; x < jobs.size(); x++) {
                  Properties p = null;
                  File_S job2 = jobs.elementAt(x);
                  JobScheduler.lastOldCheck = System.currentTimeMillis();
                  String name = job2.getName();
                  if (name.startsWith("__"))
                    try {
                      JobScheduler.lastOldCheck = System.currentTimeMillis();
                      Long.parseLong(name.substring(name.length() - 13));
                      if ((new File_S(String.valueOf(job2.getPath()) + "/job.XML")).exists()) {
                        if (System.currentTimeMillis() - (new File_S(String.valueOf(job2.getPath()) + "/job.XML")).lastModified() >= ServerStatus.LG("recent_temp_job_days") * 86400000L)
                          Common.recurseDelete(String.valueOf(job2.getPath()) + "/", false); 
                      } else if ((new File_S(String.valueOf(job2.getPath()) + "/inprogress/")).exists()) {
                        if (System.currentTimeMillis() - (new File_S(String.valueOf(job2.getPath()) + "/inprogress/")).lastModified() >= ServerStatus.LG("recent_temp_job_days") * 86400000L)
                          Common.recurseDelete(String.valueOf(job2.getPath()) + "/", false); 
                      } 
                    } catch (Exception exception) {} 
                  JobScheduler.lastOldCheck = System.currentTimeMillis();
                  File_S[] olds = (File_S[])job2.listFiles();
                  JobScheduler.lastOldCheck = System.currentTimeMillis();
                  for (int xx = 0; olds != null && xx < olds.length; xx++) {
                    JobScheduler.lastOldCheck = System.currentTimeMillis();
                    File_S old = olds[xx];
                    name = old.getName();
                    if (name.toUpperCase().endsWith(".XML") && !name.equalsIgnoreCase("job.XML") && !name.equalsIgnoreCase("inprogress.XML") && !name.equalsIgnoreCase("inprogress"))
                      if (System.currentTimeMillis() - old.lastModified() > ServerStatus.LG("recent_job_days") * 86400000L)
                        if (System.currentTimeMillis() - old.lastModified() > 300000L)
                          old.delete();   
                  } 
                  JobScheduler.lastOldCheck = System.currentTimeMillis();
                } 
              }
            }"PurgeOldJobsChecker");
      } catch (IOException e) {
        Log.log("JOB_SCHEDULER", 0, e);
      } 
    } 
  }
  
  public static boolean jobRunning(String scheduleName) {
    boolean ok = true;
    File_S[] inprogress = (File_S[])(new File_S(String.valueOf(ServerStatus.SG("jobs_location")) + "jobs/" + scheduleName + "/inprogress/")).listFiles();
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
      Log.log("JOB_SCHEDULER", 1, "Schedule Config:" + p.toString());
    } else {
      Log.log("SERVER", 0, "Skipping scheduled job since its still running:" + p.getProperty("scheduleName"));
    } 
  }
}
