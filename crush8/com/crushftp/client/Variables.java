package com.crushftp.client;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;

public class Variables {
  static long UID_GLOBAL = System.currentTimeMillis() / 1000L;
  
  public static Vector recent_guids = new Vector();
  
  SimpleDateFormat MM = new SimpleDateFormat("MM", Locale.US);
  
  SimpleDateFormat MMM = new SimpleDateFormat("MMM", Locale.US);
  
  SimpleDateFormat MMMM = new SimpleDateFormat("MMMM", Locale.US);
  
  SimpleDateFormat dd = new SimpleDateFormat("dd", Locale.US);
  
  SimpleDateFormat DD = new SimpleDateFormat("DD", Locale.US);
  
  SimpleDateFormat D = new SimpleDateFormat("D", Locale.US);
  
  SimpleDateFormat F = new SimpleDateFormat("F", Locale.US);
  
  SimpleDateFormat w = new SimpleDateFormat("w", Locale.US);
  
  SimpleDateFormat W = new SimpleDateFormat("W", Locale.US);
  
  SimpleDateFormat d_ = new SimpleDateFormat("d", Locale.US);
  
  SimpleDateFormat yy = new SimpleDateFormat("yy", Locale.US);
  
  SimpleDateFormat yyyy = new SimpleDateFormat("yyyy", Locale.US);
  
  SimpleDateFormat mm = new SimpleDateFormat("mm", Locale.US);
  
  SimpleDateFormat hh = new SimpleDateFormat("hh", Locale.US);
  
  SimpleDateFormat HH = new SimpleDateFormat("HH", Locale.US);
  
  SimpleDateFormat k = new SimpleDateFormat("k", Locale.US);
  
  SimpleDateFormat K = new SimpleDateFormat("K", Locale.US);
  
  SimpleDateFormat ss = new SimpleDateFormat("ss", Locale.US);
  
  SimpleDateFormat aa = new SimpleDateFormat("aa", Locale.US);
  
  SimpleDateFormat S = new SimpleDateFormat("S", Locale.US);
  
  SimpleDateFormat SSS = new SimpleDateFormat("SSS", Locale.US);
  
  SimpleDateFormat EEE = new SimpleDateFormat("EEE", Locale.US);
  
  SimpleDateFormat EEEE = new SimpleDateFormat("EEEE", Locale.US);
  
  SimpleDateFormat Z = new SimpleDateFormat("Z", Locale.US);
  
  SimpleDateFormat z = new SimpleDateFormat("z", Locale.US);
  
  Date d = new Date();
  
  public boolean use_safe_url = false;
  
  public static Object uidg_lock = new Object();
  
  public void setDate(Date d) {
    if (d == null)
      d = new Date(); 
    this.d = d;
  }
  
  public String replace_vars_line_url(String the_line, Properties item, String r1, String r2) {
    if (item == null)
      return the_line; 
    if (the_line.indexOf(r1) < 0)
      return the_line; 
    String addon = "";
    try {
      if (!item.getProperty("url", "").trim().equals("")) {
        addon = "";
        for (int addOnLoop = 0; addOnLoop < 2; addOnLoop++) {
          if (addOnLoop > 0)
            addon = "_2"; 
          if (item.containsKey("url" + addon)) {
            VRL vrl = new VRL(Common.url_decode(item.getProperty("url" + addon)));
            String filename = "";
            if (vrl.getFile() != null)
              filename = Common.last(vrl.getFile()); 
            the_line = Common.replace_str(the_line, String.valueOf(r1) + "path" + addon + r2, item.getProperty("the_file_path" + addon, ""));
            the_line = Common.replace_str(the_line, String.valueOf(r1) + "name" + addon + r2, item.getProperty("the_file_name" + addon, ""));
            the_line = Common.replace_str(the_line, String.valueOf(r1) + "name2" + addon + r2, item.getProperty("the_file_name2" + addon, ""));
            the_line = Common.replace_str(the_line, String.valueOf(r1) + "parent_path" + addon + r2, Common.all_but_last(item.getProperty("the_file_path" + addon, "")));
            if (filename.indexOf(".") >= 0) {
              the_line = Common.replace_str(the_line, String.valueOf(r1) + "stem" + addon + r2, filename.substring(0, filename.lastIndexOf(".")));
              the_line = Common.replace_str(the_line, String.valueOf(r1) + "stem_alt" + addon + r2, filename.substring(0, filename.indexOf(".")));
              the_line = Common.replace_str(the_line, String.valueOf(r1) + "ext" + addon + r2, filename.substring(filename.lastIndexOf(".")));
              the_line = Common.replace_str(the_line, String.valueOf(r1) + "ext_alt" + addon + r2, filename.substring(filename.indexOf(".")));
              the_line = Common.replace_str(the_line, String.valueOf(r1) + "ext2" + addon + r2, filename.substring(filename.lastIndexOf(".") + 1));
              String[] dots_a = filename.split("\\.");
              for (int dots = 0; dots < dots_a.length; dots++)
                the_line = Common.replace_str(the_line, String.valueOf(r1) + "dot" + dots + addon + r2, dots_a[dots]); 
              int loop = 0;
              for (int i = dots_a.length - 1; i >= 0; i--) {
                the_line = Common.replace_str(the_line, String.valueOf(r1) + loop + "dot" + addon + r2, dots_a[i]);
                loop++;
              } 
            } else {
              the_line = Common.replace_str(the_line, String.valueOf(r1) + "stem" + addon + r2, filename);
              the_line = Common.replace_str(the_line, String.valueOf(r1) + "ext" + addon + r2, "");
              the_line = Common.replace_str(the_line, String.valueOf(r1) + "ext2" + addon + r2, "");
            } 
            if (item.containsKey("the_file_path" + addon)) {
              String[] paths = item.getProperty("the_file_path" + addon).split("/");
              for (int xx = 0; xx < 100; xx++) {
                if (xx < paths.length) {
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + "path" + addon + xx + r2, "/" + paths[paths.length - 1 - xx]);
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + xx + "path" + addon + r2, String.valueOf(paths[xx]) + "/");
                } else {
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + "path" + addon + xx + r2, "");
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + xx + "path" + addon + r2, "");
                } 
              } 
            } 
            if (item.containsKey("url" + addon)) {
              String[] paths = Common.url_decode(this.use_safe_url ? (new VRL(item.getProperty("url" + addon))).safe() : item.getProperty("url" + addon)).split("/");
              for (int xx = 0; xx < 100; xx++) {
                if (xx < paths.length) {
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + "url" + addon + xx + r2, "/" + paths[paths.length - 1 - xx]);
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + xx + "url" + addon + r2, String.valueOf(paths[xx]) + "/");
                } else {
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + "url" + addon + xx + r2, "");
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + xx + "url" + addon + r2, "");
                } 
                if (xx < paths.length) {
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + "url" + addon + "/" + xx + r2, paths[paths.length - 1 - xx]);
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + xx + "url" + addon + "/" + r2, paths[xx]);
                } else {
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + "url" + addon + "/" + xx + r2, "");
                  the_line = Common.replace_str(the_line, String.valueOf(r1) + xx + "url" + addon + "/" + r2, "");
                } 
              } 
            } 
          } 
        } 
      } 
    } catch (Exception e) {
      Common.log("SERVER", 0, e);
    } 
    return the_line;
  }
  
  public synchronized String replace_vars_line_date(String the_line, Properties item, String r1, String r2) {
    if (the_line.indexOf(r1) < 0)
      return the_line; 
    if (the_line.indexOf(String.valueOf(r1) + "uid" + r2) >= 0)
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "uid" + r2, (new StringBuffer(String.valueOf(uid()))).toString()); 
    if (the_line.indexOf(String.valueOf(r1) + "uidg" + r2) >= 0)
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "uidg" + r2, (new StringBuffer(String.valueOf(uidg()))).toString()); 
    for (int loop = 0; loop < 20; loop++) {
      if (the_line.indexOf(String.valueOf(r1) + "random" + loop + r2) >= 0)
        the_line = Common.replace_str(the_line, String.valueOf(r1) + "random" + loop + r2, (new StringBuffer(String.valueOf(Common.makeBoundary(loop)))).toString()); 
    } 
    int dateadd = 0;
    if (the_line.indexOf(String.valueOf(r1) + "dateadd") >= 0) {
      String inner = the_line.substring(the_line.indexOf(String.valueOf(r1) + "dateadd") + (String.valueOf(r1) + "dateadd").length(), the_line.indexOf(r2, the_line.indexOf(String.valueOf(r1) + "dateadd")));
      dateadd = Integer.parseInt(inner.split(":")[0].trim());
      the_line = String.valueOf(the_line.substring(0, the_line.indexOf(String.valueOf(r1) + "dateadd"))) + the_line.substring(the_line.indexOf(r2, the_line.indexOf(String.valueOf(r1) + "dateadd")) + 1);
    } 
    Date now = this.d;
    GregorianCalendar yesterday = new GregorianCalendar();
    yesterday.setTime(this.d);
    yesterday.add(5, -1);
    GregorianCalendar c = new GregorianCalendar();
    c.setTime(this.d);
    if (dateadd != 0)
      now = getDateAdd(now, dateadd); 
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "millis" + r2, (new StringBuffer(String.valueOf(now.getTime()))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "yesterday" + r2, (new StringBuffer(String.valueOf(yesterday.getTime().getTime()))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "time" + r2, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "now" + r2, (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "MM" + r2, this.MM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "MMM" + r2, this.MMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "MMMM" + r2, this.MMMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "dd" + r2, this.dd.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "d" + r2, this.d_.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "yy" + r2, this.yy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "yyyy" + r2, this.yyyy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "mm" + r2, this.mm.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "hh" + r2, this.hh.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "HH" + r2, this.HH.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "k" + r2, this.k.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "K" + r2, this.K.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "ss" + r2, this.ss.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "aa" + r2, this.aa.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "S" + r2, this.S.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "SSS" + r2, this.SSS.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "EEE" + r2, this.EEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "EEEE" + r2, this.EEEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "u" + r2, (new StringBuffer(String.valueOf(c.get(7)))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "Z" + r2, this.Z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "z" + r2, this.z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "DD" + r2, this.DD.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "D" + r2, this.D.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "F" + r2, this.F.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "w" + r2, this.w.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "W" + r2, this.W.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "J" + r2, (new StringBuffer(String.valueOf(date_to_julian(now)))).toString());
    now = yesterday.getTime();
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!millis" + r2, (new StringBuffer(String.valueOf(now.getTime()))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!MM" + r2, this.MM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!MMM" + r2, this.MMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!MMMM" + r2, this.MMMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!dd" + r2, this.dd.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!d" + r2, this.d_.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!yy" + r2, this.yy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!yyyy" + r2, this.yyyy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!mm" + r2, this.mm.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!hh" + r2, this.hh.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!HH" + r2, this.HH.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!k" + r2, this.k.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!K" + r2, this.K.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!ss" + r2, this.ss.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!aa" + r2, this.aa.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!S" + r2, this.S.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!SSS" + r2, this.SSS.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!EEE" + r2, this.EEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!EEEE" + r2, this.EEEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!u" + r2, (new StringBuffer(String.valueOf(yesterday.get(7)))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!Z" + r2, this.Z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!z" + r2, this.z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!DD" + r2, this.DD.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!D" + r2, this.D.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!F" + r2, this.F.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!w" + r2, this.w.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!W" + r2, this.W.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!J" + r2, (new StringBuffer(String.valueOf(date_to_julian(now)))).toString());
    GregorianCalendar lastweek = new GregorianCalendar();
    lastweek.setTime(this.d);
    lastweek.add(5, -7);
    now = lastweek.getTime();
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!millis" + r2, (new StringBuffer(String.valueOf(now.getTime()))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!MM" + r2, this.MM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!MMM" + r2, this.MMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!MMMM" + r2, this.MMMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!dd" + r2, this.dd.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!d" + r2, this.d_.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!yy" + r2, this.yy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!yyyy" + r2, this.yyyy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!mm" + r2, this.mm.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!hh" + r2, this.hh.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!HH" + r2, this.HH.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!k" + r2, this.k.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!K" + r2, this.K.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!ss" + r2, this.ss.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!aa" + r2, this.aa.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!S" + r2, this.S.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!SSS" + r2, this.SSS.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!EEE" + r2, this.EEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!EEEE" + r2, this.EEEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!u" + r2, (new StringBuffer(String.valueOf(lastweek.get(7)))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!Z" + r2, this.Z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!z" + r2, this.z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!DD" + r2, this.DD.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!D" + r2, this.D.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!F" + r2, this.F.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!w" + r2, this.w.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!W" + r2, this.W.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!J" + r2, (new StringBuffer(String.valueOf(date_to_julian(now)))).toString());
    GregorianCalendar lastmonth = new GregorianCalendar();
    lastmonth.setTime(this.d);
    lastmonth.add(2, -1);
    now = lastmonth.getTime();
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!millis" + r2, (new StringBuffer(String.valueOf(now.getTime()))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!MM" + r2, this.MM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!MMM" + r2, this.MMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!MMMM" + r2, this.MMMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!dd" + r2, this.dd.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!d" + r2, this.d_.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!yy" + r2, this.yy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!yyyy" + r2, this.yyyy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!mm" + r2, this.mm.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!hh" + r2, this.hh.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!HH" + r2, this.HH.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!k" + r2, this.k.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!K" + r2, this.K.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!ss" + r2, this.ss.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!aa" + r2, this.aa.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!S" + r2, this.S.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!SSS" + r2, this.SSS.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!EEE" + r2, this.EEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!EEEE" + r2, this.EEEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!u" + r2, (new StringBuffer(String.valueOf(lastmonth.get(7)))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!Z" + r2, this.Z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!z" + r2, this.z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!DD" + r2, this.DD.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!D" + r2, this.D.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!F" + r2, this.F.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!w" + r2, this.w.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!W" + r2, this.W.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "!!!J" + r2, (new StringBuffer(String.valueOf(date_to_julian(now)))).toString());
    now = new Date();
    c.setTime(now);
    if (dateadd != 0)
      now = getDateAdd(now, dateadd); 
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".MM" + r2, this.MM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".MMM" + r2, this.MMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".MMMM" + r2, this.MMMM.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".dd" + r2, this.dd.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".d" + r2, this.d_.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".yy" + r2, this.yy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".yyyy" + r2, this.yyyy.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".mm" + r2, this.mm.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".hh" + r2, this.hh.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".HH" + r2, this.HH.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".k" + r2, this.k.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".K" + r2, this.K.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".ss" + r2, this.ss.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".aa" + r2, this.aa.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".S" + r2, this.S.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".SSS" + r2, this.SSS.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".EEE" + r2, this.EEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".EEEE" + r2, this.EEEE.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".u" + r2, (new StringBuffer(String.valueOf(c.get(7)))).toString());
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".Z" + r2, this.Z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".z" + r2, this.z.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".DD" + r2, this.DD.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".D" + r2, this.D.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".F" + r2, this.F.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".w" + r2, this.w.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".W" + r2, this.W.format(now));
    the_line = Common.replace_str(the_line, String.valueOf(r1) + ".J" + r2, (new StringBuffer(String.valueOf(date_to_julian(now)))).toString());
    try {
      now = new Date(Long.parseLong(item.getProperty("the_file_start")));
      if (dateadd != 0)
        now = getDateAdd(now, dateadd); 
      c.setTime(now);
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "MM_" + r2, this.MM.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "MMM_" + r2, this.MMM.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "MMMM_" + r2, this.MMMM.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "dd_" + r2, this.dd.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "d_" + r2, this.d_.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "yy_" + r2, this.yy.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "yyyy_" + r2, this.yyyy.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "mm_" + r2, this.mm.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "hh_" + r2, this.hh.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "HH_" + r2, this.HH.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "k_" + r2, this.k.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "K_" + r2, this.K.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "ss_" + r2, this.ss.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "aa_" + r2, this.aa.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "S_" + r2, this.S.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "SSS_" + r2, this.SSS.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "EEE_" + r2, this.EEE.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "EEEE_" + r2, this.EEEE.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "u_" + r2, (new StringBuffer(String.valueOf(c.get(7)))).toString());
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "Z_" + r2, this.Z.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "z_" + r2, this.z.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "DD_" + r2, this.DD.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "D_" + r2, this.D.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "F_" + r2, this.F.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "w_" + r2, this.w.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "W_" + r2, this.W.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "J_" + r2, (new StringBuffer(String.valueOf(date_to_julian(now)))).toString());
    } catch (Exception exception) {}
    try {
      now = new Date(Long.parseLong(item.getProperty("the_file_end")));
      if (dateadd != 0)
        now = getDateAdd(now, dateadd); 
      c.setTime(now);
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_MM" + r2, this.MM.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_MMM" + r2, this.MMM.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_MMMM" + r2, this.MMMM.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_dd" + r2, this.dd.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_d" + r2, this.d_.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_yy" + r2, this.yy.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_yyyy" + r2, this.yyyy.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_mm" + r2, this.mm.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_hh" + r2, this.hh.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_HH" + r2, this.HH.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_k" + r2, this.k.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_K" + r2, this.K.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_ss" + r2, this.ss.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_aa" + r2, this.aa.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_S" + r2, this.S.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_SSS" + r2, this.SSS.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_EEE" + r2, this.EEE.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_EEEE" + r2, this.EEEE.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_u" + r2, (new StringBuffer(String.valueOf(c.get(7)))).toString());
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_Z" + r2, this.Z.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_z" + r2, this.z.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_DD" + r2, this.DD.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_D" + r2, this.D.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_F" + r2, this.F.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_w" + r2, this.w.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_W" + r2, this.W.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "_J" + r2, (new StringBuffer(String.valueOf(date_to_julian(now)))).toString());
    } catch (Exception exception) {}
    try {
      now = new Date(Long.parseLong(item.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString())));
      if (dateadd != 0)
        now = getDateAdd(now, dateadd); 
      c.setTime(now);
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-MM" + r2, this.MM.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-MMM" + r2, this.MMM.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-MMMM" + r2, this.MMMM.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-dd" + r2, this.dd.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-d" + r2, this.d_.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-yy" + r2, this.yy.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-yyyy" + r2, this.yyyy.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-mm" + r2, this.mm.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-hh" + r2, this.hh.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-HH" + r2, this.HH.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-k" + r2, this.k.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-K" + r2, this.K.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-ss" + r2, this.ss.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-aa" + r2, this.aa.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-S" + r2, this.S.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-SSS" + r2, this.SSS.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-EEE" + r2, this.EEE.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-EEEE" + r2, this.EEEE.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-u" + r2, (new StringBuffer(String.valueOf(c.get(7)))).toString());
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-Z" + r2, this.Z.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-z" + r2, this.z.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-DD" + r2, this.DD.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-D" + r2, this.D.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-F" + r2, this.F.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-w" + r2, this.w.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-W" + r2, this.W.format(now));
      the_line = Common.replace_str(the_line, String.valueOf(r1) + "-J" + r2, (new StringBuffer(String.valueOf(date_to_julian(now)))).toString());
    } catch (Exception exception) {}
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "r" + r2, "\r");
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "n" + r2, "\n");
    the_line = Common.replace_str(the_line, String.valueOf(r1) + "t" + r2, "\t");
    return the_line;
  }
  
  public static Date getDateAdd(Date d_tmp, int days) {
    GregorianCalendar gc = new GregorianCalendar();
    gc.setTime(d_tmp);
    gc.add(5, days);
    return gc.getTime();
  }
  
  public static String uid() {
    String chars = "1234567890";
    String rand = "";
    for (int i = 0; i < 8; i++)
      rand = String.valueOf(rand) + chars.charAt((int)(Math.random() * (chars.length() - 1))); 
    return rand;
  }
  
  public static String uidg() {
    synchronized (uidg_lock) {
      while (recent_guids.indexOf((new StringBuffer(String.valueOf(System.currentTimeMillis() / 1000L))).toString()) >= 0) {
        try {
          Thread.sleep(100L);
        } catch (Exception exception) {}
      } 
      UID_GLOBAL = System.currentTimeMillis() / 1000L;
      recent_guids.addElement((new StringBuffer(String.valueOf(UID_GLOBAL))).toString());
      while (recent_guids.size() > 0 && System.currentTimeMillis() - Long.parseLong(recent_guids.elementAt(0)) > 10800000L)
        recent_guids.removeElementAt(0); 
      return (new StringBuffer(String.valueOf(UID_GLOBAL))).toString();
    } 
  }
  
  public static double eval_math(String str) {
    return (new Variables$1$Parser(str)).parse();
  }
  
  public static double date_to_julian(Date d) {
    GregorianCalendar date = new GregorianCalendar();
    date.setTime(d);
    double extra = 100.0D * date.get(1) + (date.get(2) + 1) - 190002.5D;
    return 367.0D * date.get(1) - Math.floor(7.0D * (date.get(1) + Math.floor(((date.get(2) + 1) + 9.0D) / 12.0D)) / 4.0D) + Math.floor(275.0D * (date.get(2) + 1) / 9.0D) + date.get(5) + (date.get(11) + (date.get(12) + date.get(13) / 60.0D) / 60.0D) / 24.0D + 1721013.5D - 0.5D * extra / Math.abs(extra) + 0.5D;
  }
}
