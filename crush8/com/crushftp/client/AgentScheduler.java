package com.crushftp.client;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class AgentScheduler {
  static long lastOldCheck = 0L;
  
  static final long day = 86400000L;
  
  public static void runSchedules(AgentUI aui) {
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
    if (purge_old)
      lastOldCheck = System.currentTimeMillis(); 
    SimpleDateFormat yyMMddHHmm = new SimpleDateFormat("yyMMddHHmm");
    SimpleDateFormat yyMMdd = new SimpleDateFormat("yyMMdd");
    Vector schedules = (Vector)aui.prefs.get("schedules");
    for (int x = 0; x < schedules.size(); x++) {
      boolean runSchedule = false;
      Properties p = null;
      try {
        p = schedules.elementAt(x);
        if (p != null)
          if (p.getProperty("enabled", "").equalsIgnoreCase("true")) {
            long nextRun = Long.parseLong(p.getProperty("nextRun", "0"));
            long newNextRun = (new Date()).getTime();
            if (p.getProperty("scheduleType", "").equals("minutely") && (new Date()).getTime() > nextRun) {
              newNextRun = yyMMddHHmm.parse(yyMMddHHmm.format(new Date(newNextRun))).getTime();
              Calendar c = new GregorianCalendar();
              c.setTime(new Date(newNextRun));
              int minutes = Integer.parseInt(p.getProperty("minutelyAmount"));
              if (minutes == 0)
                minutes = 1; 
              if (minutes > 0) {
                c.add(12, minutes);
              } else {
                c.add(13, minutes * -1);
              } 
              newNextRun = c.getTime().getTime();
              runSchedule = true;
            } else if (!p.getProperty("scheduleType", "").equals("minutely")) {
              String[] schedule_time = p.getProperty("scheduleTime", "").split(",");
              for (int xx = 0; xx < schedule_time.length && (!runSchedule || nextRun == -1L); xx++) {
                boolean last_time = (xx == schedule_time.length - 1);
                if (!schedule_time[xx].trim().equals("")) {
                  if ((nextRun == -1L || time.format(new Date()).equals(time.format(time.parse(schedule_time[xx].trim())))) && (new Date()).getTime() > nextRun)
                    if (p.getProperty("scheduleType", "").equals("daily")) {
                      runSchedule = true;
                      String next_time = null;
                      if (last_time) {
                        Calendar c = new GregorianCalendar();
                        c.setTime(new Date(newNextRun - 60000L));
                        c.add(5, Integer.parseInt(p.getProperty("dailyAmount")));
                        newNextRun = yyMMdd.parse(yyMMdd.format(c.getTime())).getTime();
                        next_time = schedule_time[0];
                      } else {
                        newNextRun = yyMMddHHmm.parse(yyMMddHHmm.format(new Date(newNextRun))).getTime();
                        if (nextRun >= 0L)
                          newNextRun += 60000L; 
                        next_time = schedule_time[xx + 1];
                      } 
                      while (!time.format(new Date(newNextRun)).equals(time.format(time.parse(next_time.trim()))))
                        newNextRun += 60000L; 
                      newNextRun = yyMMddHHmm.parse(yyMMddHHmm.format(new Date(newNextRun))).getTime();
                    } else if (p.getProperty("scheduleType", "").equals("weekly")) {
                      String day = day_lookup.getProperty((new SimpleDateFormat("EEE", Locale.US)).format(new Date()).toUpperCase());
                      if (nextRun == -1L || p.getProperty("weekDays", "").indexOf(day) >= 0) {
                        if (p.getProperty("weekDays", "").indexOf(day) + 1 == (p.getProperty("weekDays", "").split("\\)")).length && last_time) {
                          Calendar c = new GregorianCalendar();
                          c.setTime(new Date());
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
                          newNextRun = c.getTime().getTime();
                          newNextRun = yyMMdd.parse(yyMMdd.format(c.getTime())).getTime();
                        } 
                        newNextRun = yyMMddHHmm.parse(yyMMddHHmm.format(new Date(newNextRun))).getTime();
                        String next_time = null;
                        if (last_time || nextRun == -1L) {
                          next_time = schedule_time[0];
                        } else {
                          newNextRun += 60000L;
                          next_time = schedule_time[xx + 1];
                        } 
                        while (!time.format(new Date(newNextRun)).equals(time.format(time.parse(next_time.trim()))))
                          newNextRun += 60000L; 
                        if (nextRun == -1L || last_time) {
                          day = day_lookup.getProperty((new SimpleDateFormat("EEE", Locale.US)).format(new Date(newNextRun)).toUpperCase());
                          while (p.getProperty("weekDays", "").indexOf(day) < 0) {
                            newNextRun += 86400000L;
                            day = day_lookup.getProperty((new SimpleDateFormat("EEE", Locale.US)).format(new Date(newNextRun)).toUpperCase());
                          } 
                        } 
                        runSchedule = true;
                      } 
                    } else if (p.getProperty("scheduleType", "").equals("monthly")) {
                      SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
                      SimpleDateFormat d = new SimpleDateFormat("d", Locale.US);
                      String day1 = "(" + d.format(new Date()) + ")";
                      String day2 = "(" + dd.format(new Date()) + ")";
                      if (nextRun == -1L || p.getProperty("monthDays", "").indexOf(day1) >= 0 || p.getProperty("monthDays", "").indexOf(day2) >= 0) {
                        if (last_time && (p.getProperty("monthDays", "").indexOf(day1) == p.getProperty("monthDays", "").length() - day1.length() || p.getProperty("monthDays", "").indexOf(day2) == p.getProperty("monthDays", "").length() - day2.length())) {
                          Calendar c = new GregorianCalendar();
                          c.setTime(new Date());
                          while (c.get(5) != 1)
                            c.add(5, -1); 
                          c.add(2, Integer.parseInt(p.getProperty("monthlyAmount")));
                          newNextRun = c.getTime().getTime();
                          newNextRun = yyMMdd.parse(yyMMdd.format(c.getTime())).getTime();
                        } 
                        newNextRun = yyMMddHHmm.parse(yyMMddHHmm.format(new Date(newNextRun))).getTime();
                        String next_time = null;
                        if (last_time || nextRun == -1L) {
                          next_time = schedule_time[0];
                        } else {
                          newNextRun += 60000L;
                          next_time = schedule_time[xx + 1];
                        } 
                        while (!time.format(new Date(newNextRun)).equals(time.format(time.parse(next_time.trim()))))
                          newNextRun += 60000L; 
                        if (nextRun == -1L) {
                          day1 = "(" + d.format(new Date(newNextRun)) + ")";
                          day2 = "(" + dd.format(new Date(newNextRun)) + ")";
                          while (p.getProperty("monthDays", "").indexOf(day1) < 0 && p.getProperty("monthDays", "").indexOf(day2) < 0) {
                            newNextRun += 60000L;
                            day1 = "(" + d.format(new Date(newNextRun)) + ")";
                            day2 = "(" + dd.format(new Date(newNextRun)) + ")";
                          } 
                        } 
                        runSchedule = true;
                      } 
                    }  
                  if (nextRun == -1L)
                    break; 
                } 
              } 
            } 
            if (runSchedule || nextRun == -1L) {
              p.put("nextRun", (new StringBuffer(String.valueOf(newNextRun))).toString());
              aui.savePrefs();
              if (nextRun >= 0L)
                runSchedule(aui, p); 
            } 
          }  
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println((String)p);
      } 
    } 
  }
  
  public static boolean jobRunning(String scheduleName) {
    boolean ok = true;
    return !ok;
  }
  
  public static void runSchedule(AgentUI aui, Properties p) {
    boolean ok = true;
    if (p.getProperty("scheduleName", "").toUpperCase().endsWith("_SINGLE") || p.getProperty("single", "").equals("true"))
      ok = !jobRunning(p.getProperty("scheduleName", "")); 
    if (ok) {
      Client client = aui.getNewClient();
      aui.clients.put(client.uid, client);
      client.prefs = p;
      aui.runSchedule(client, p);
      System.out.println("Ran Schedule :" + p.getProperty("scheduleName") + ":" + new Date());
    } else {
      System.out.println("Skipping scheduled job since its still running:" + p.getProperty("scheduleName"));
    } 
  }
}
