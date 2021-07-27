package com.crushftp.client;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class GenericClientMulti extends GenericClient {
  public Vector clients = null;
  
  public Vector vItems = null;
  
  public Properties originalvItem = null;
  
  public static Object journal_lock = new Object();
  
  public static Object replication_lock = new Object();
  
  public static boolean replicating = false;
  
  public static Properties replication_status = new Properties();
  
  public GenericClientMulti(String header, Vector log, Properties originalvItem, Vector vItems, Vector clients, boolean play) {
    super(header, log);
    this.clients = clients;
    this.originalvItem = originalvItem;
    this.vItems = vItems;
    this.config.put("timeout", System.getProperty("crushftp.multi_journal_timeout", "30000"));
    if (play)
      playJournal(); 
  }
  
  public void setCache(Properties statCache) {
    this.statCache = statCache;
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      c.setCache(statCache);
    } 
  }
  
  public void setConfigObj(Properties config) {
    config.putAll(this.config);
    this.config = config;
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      c.setConfigObj(config);
    } 
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    String result = "";
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      if (c.getConfig("replicated_login_user") != null)
        username = c.getConfig("replicated_login_user").toString(); 
      if (c.getConfig("replicated_login_pass") != null)
        password = c.getConfig("replicated_login_pass").toString(); 
      try {
        result = c.login(username, password, clientid);
      } catch (Exception e) {
        if (x == 0)
          throw e; 
        Common.log("REPLICATION", 1, e);
      } 
    } 
    return result;
  }
  
  public void logout() throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      try {
        c.logout();
      } catch (Exception e) {
        if (x == 0)
          throw e; 
        Common.log("REPLICATION", 1, e);
      } 
    } 
  }
  
  public void close() throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      try {
        c.close();
      } catch (Exception e) {
        if (x == 0)
          throw e; 
        Common.log("REPLICATION", 1, e);
      } 
    } 
  }
  
  public String fixPath(String path, Properties vItem) {
    VRL vrl = new VRL(this.originalvItem.getProperty("url"));
    VRL vrl2 = new VRL(vItem.getProperty("url"));
    return String.valueOf(vrl2.getPath()) + path.substring(vrl.getPath().length());
  }
  
  public Vector list(String path, Vector list) throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      String item_id = getItemId(c);
      if (x == 0 || !journaling(item_id) || System.getProperty("crushftp.multi_journal", "false").equals("false")) {
        Vector list2 = new Vector();
        try {
          list2 = c.list(fixPath(path, this.vItems.get(x)), new Vector());
        } catch (FileNotFoundException e) {
          Common.log("SERVER", 2, e);
        } catch (Exception e) {
          c.setConfig("error", e);
          if (x > 0 && System.getProperty("crushftp.replicated_vfs", "false").equals("true"))
            writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, path, path, 0L, true, true, "list", item_id); 
          if (x == 0)
            throw e; 
        } 
        if (System.getProperty("crushftp.replicated_vfs", "false").equals("true") && x == this.clients.size() - 1)
          break; 
        int listSize = list.size();
        for (int x2 = 0; x2 < list2.size(); x2++) {
          Properties p2 = list2.elementAt(x2);
          boolean found = false;
          for (int x1 = 0; !found && x1 < listSize; x1++) {
            Properties p1 = list.elementAt(x1);
            if (p1.getProperty("name", "").equals(p2.getProperty("name", "")))
              found = true; 
          } 
          if (!found)
            list.addElement(p2); 
        } 
      } 
    } 
    return list;
  }
  
  public InputStream download(String path, long startPos, long endPos, boolean binary) throws Exception {
    Exception lastException = null;
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      try {
        InputStream inTemp = c.download(fixPath(path, this.vItems.get(x)), startPos, endPos, binary);
        if (inTemp != null)
          return inTemp; 
      } catch (Exception e) {
        lastException = e;
      } 
    } 
    if (lastException != null)
      throw lastException; 
    return null;
  }
  
  public OutputStream upload(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    Vector outs = new Vector();
    Vector item_ids = new Vector();
    for (int x = 0; x < this.clients.size(); x++) {
      if (x <= 0 || !this.config.getProperty("replicate_content", "true").equals("false")) {
        GenericClient c = this.clients.elementAt(x);
        String item_id = getItemId(c);
        try {
          if (x > 0 && journaling(item_id))
            throw new IOException("Journaling..."); 
          outs.addElement(c.upload(fixPath(path, this.vItems.get(x)), startPos, truncate, binary));
        } catch (Exception e) {
          outs.addElement(null);
          Common.log("REPLICATION", 1, e);
        } 
        item_ids.addElement(item_id);
      } 
    } 
    this.out = new GenericClientMulti$1$OutputMulti(this, outs, item_ids, path, startPos, truncate, binary);
    return this.out;
  }
  
  public boolean upload_0_byte(String path) throws Exception {
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
      if (x <= 0 || !this.config.getProperty("replicate_content", "true").equals("false")) {
        GenericClient c = this.clients.elementAt(x);
        String item_id = getItemId(c);
        try {
          if (x > 0 && journaling(item_id))
            throw new IOException("Journaling..."); 
          boolean ok2 = c.upload_0_byte(fixPath(path, this.vItems.get(x)));
          if (!ok2)
            ok = false; 
        } catch (Exception e) {
          if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, path, path, 0L, true, true, "upload_0_byte", item_id);
          } else {
            throw e;
          } 
        } 
      } 
    } 
    return ok;
  }
  
  public boolean delete(String path) throws Exception {
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
      if ((x != 0 || !this.config.getProperty("skip_first_client", "false").equals("true")) && (
        x <= 0 || !this.config.getProperty("replicate_content", "true").equals("false"))) {
        GenericClient c = this.clients.elementAt(x);
        String item_id = getItemId(c);
        try {
          if (x > 0 && journaling(item_id))
            throw new IOException("Journaling..."); 
          boolean ok2 = c.delete(fixPath(path, this.vItems.get(x)));
          if (!ok2 && (journaling(item_id) || x > 0) && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            ok = false;
            throw new IOException("Journaling...");
          } 
          if (!ok2 && x == 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            ok = false;
            break;
          } 
        } catch (Exception e) {
          if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, path, path, 0L, true, true, "delete", item_id);
          } else {
            throw e;
          } 
        } 
      } 
    } 
    return ok;
  }
  
  public boolean makedir(String path) throws Exception {
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      String item_id = getItemId(c);
      try {
        if (x > 0 && journaling(item_id))
          throw new IOException("Journaling..."); 
        boolean ok2 = c.makedir(fixPath(path, this.vItems.get(x)));
        if (!ok2 && (journaling(item_id) || x > 0)) {
          ok = false;
          if (stat(path) == null)
            throw new IOException("Journaling..."); 
        } else if (!ok2 && x == 0) {
          ok = false;
          break;
        } 
      } catch (Exception e) {
        if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
          writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, path, path, 0L, true, true, "makedir", item_id);
        } else {
          throw e;
        } 
      } 
    } 
    return ok;
  }
  
  public boolean makedirs(String path) throws Exception {
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      String item_id = getItemId(c);
      try {
        if (x > 0 && journaling(item_id))
          throw new IOException("Journaling..."); 
        boolean ok2 = c.makedirs(fixPath(path, this.vItems.get(x)));
        if (!ok2 && (journaling(item_id) || x > 0)) {
          ok = false;
          if (stat(path) == null)
            throw new IOException("Journaling..."); 
        } else if (!ok2 && x == 0) {
          ok = false;
          break;
        } 
      } catch (Exception e) {
        if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
          writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, path, path, 0L, true, true, "makedirs", item_id);
        } else {
          throw e;
        } 
      } 
    } 
    return ok;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    boolean is_file = true;
    if (this.config.getProperty("replicate_content", "true").equals("true")) {
      Properties p = stat(rnfr);
      if (p != null && p.getProperty("type", "FILE").equalsIgnoreCase("DIR"))
        is_file = false; 
    } 
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
      if (x <= 0 || !this.config.getProperty("replicate_content", "true").equals("false") || !is_file) {
        GenericClient c = this.clients.elementAt(x);
        String item_id = getItemId(c);
        try {
          if (x > 0 && journaling(item_id))
            throw new IOException("Journaling..."); 
          boolean ok2 = c.rename(fixPath(rnfr, this.vItems.get(x)), fixPath(rnto, this.vItems.get(x)));
          if (!ok2 && (journaling(item_id) || x > 0) && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            ok = false;
            throw new IOException("Journaling...");
          } 
          if (!ok2 && x == 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            ok = false;
            break;
          } 
        } catch (Exception e) {
          if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, rnfr, rnto, "", 0L, true, true, "rename", item_id);
          } else {
            throw e;
          } 
        } 
      } 
    } 
    return ok;
  }
  
  public Properties stat(String path) throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      try {
        Properties p = c.stat(fixPath(path, this.vItems.get(x)));
        if (p != null)
          return p; 
      } catch (Exception e) {
        if (x == 0)
          throw e; 
        Common.log("REPLICATION", 1, e);
      } 
    } 
    return null;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
      if (x <= 0 || !this.config.getProperty("replicate_content", "true").equals("false")) {
        GenericClient c = this.clients.elementAt(x);
        String item_id = getItemId(c);
        try {
          if (x > 0 && journaling(item_id))
            throw new IOException("Journaling..."); 
          boolean ok2 = c.mdtm(fixPath(path, this.vItems.get(x)), modified);
          if (!ok2 && (journaling(item_id) || x > 0) && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            ok = false;
            throw new IOException("Journaling...");
          } 
        } catch (Exception e) {
          if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, (new StringBuffer(String.valueOf(modified))).toString(), path, 0L, true, true, "mdtm", item_id);
          } else {
            throw e;
          } 
        } 
      } 
    } 
    return ok;
  }
  
  public void setMod(String path, String val, String param) throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
      if (x <= 0 || !this.config.getProperty("replicate_content", "true").equals("false")) {
        GenericClient c = this.clients.elementAt(x);
        String item_id = getItemId(c);
        try {
          if (x > 0 && journaling(item_id))
            throw new IOException("Journaling..."); 
          c.setMod(path, val, param);
        } catch (Exception e) {
          if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, val, param, 0L, true, true, "setMod", item_id);
          } else {
            throw e;
          } 
        } 
      } 
    } 
  }
  
  public void setOwner(String path, String val, String param) throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
      if (x <= 0 || !this.config.getProperty("replicate_content", "true").equals("false")) {
        GenericClient c = this.clients.elementAt(x);
        String item_id = getItemId(c);
        try {
          if (x > 0 && journaling(item_id))
            throw new IOException("Journaling..."); 
          c.setOwner(path, val, param);
        } catch (Exception e) {
          if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, val, param, 0L, true, true, "setOwner", item_id);
          } else {
            throw e;
          } 
        } 
      } 
    } 
  }
  
  public void setGroup(String path, String val, String param) throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
      if (x <= 0 || !this.config.getProperty("replicate_content", "true").equals("false")) {
        GenericClient c = this.clients.elementAt(x);
        String item_id = getItemId(c);
        try {
          if (x > 0 && journaling(item_id))
            throw new IOException("Journaling..."); 
          c.setGroup(path, val, param);
        } catch (Exception e) {
          if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
            writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, val, param, 0L, true, true, "setGroup", item_id);
          } else {
            throw e;
          } 
        } 
      } 
    } 
  }
  
  public void writeJournal(byte[] b, int off, int len, GenericClient c, long start_time, int x, String path1, String path2, String path3, long startPos, boolean truncate, boolean binary, String action, String item_id) throws IOException {
    synchronized (journal_lock) {
      String journal_path = "./multi_journal/" + item_id + "/" + start_time + "/" + c.toString() + "/";
      if (!(new File_S(journal_path)).exists()) {
        (new File_S(journal_path)).mkdirs();
        Properties config_wrapper = new Properties();
        config_wrapper.put("config", c.config);
        config_wrapper.put("x", (new StringBuffer(String.valueOf(x))).toString());
        config_wrapper.put("path1", path1);
        config_wrapper.put("path2", path2);
        config_wrapper.put("path3", path3);
        config_wrapper.put("startPos", (new StringBuffer(String.valueOf(startPos))).toString());
        config_wrapper.put("truncate", (new StringBuffer(String.valueOf(truncate))).toString());
        config_wrapper.put("binary", (new StringBuffer(String.valueOf(binary))).toString());
        config_wrapper.put("action", (new StringBuffer(String.valueOf(action))).toString());
        if (action.equals("upload")) {
          Common.writeXMLObject(String.valueOf(journal_path) + "config.XML.locked", config_wrapper, "config");
        } else {
          Common.writeXMLObject(String.valueOf(journal_path) + "config.XML", config_wrapper, "config");
        } 
      } 
      if (action.equals("unlock")) {
        (new File_S(String.valueOf(journal_path) + "config.XML.locked")).renameTo(new File_S(String.valueOf(journal_path) + "config.XML"));
      } else if (action.equals("close")) {
        Common.recurseDelete(journal_path, false);
        String journal_path_parent = "./multi_journal/" + item_id + "/";
        (new File_S(String.valueOf(Common.all_but_last(journal_path)) + ".DS_Store")).delete();
        (new File_S(Common.all_but_last(journal_path))).delete();
        (new File_S(String.valueOf(journal_path_parent) + ".DS_Store")).delete();
        (new File_S(journal_path_parent)).delete();
        if ((new File_S(String.valueOf(journal_path) + "upload")).exists()) {
          Common.log("REPLICATION", 0, "Journaling close error!!!  path1:" + path1 + " path2:" + path2 + " startPos:" + startPos + " truncate:" + truncate + " binary:" + binary + " action:" + action + " item_id:" + item_id + " off:" + off + " len:" + len);
          Common.log("REPLICATION", 0, new Exception("Journaling issue."));
        } 
      } else if (b != null) {
        RandomAccessFile raf = new RandomAccessFile(new File_S(String.valueOf(journal_path) + action), "rw");
        raf.seek(raf.length());
        raf.write(b, off, len);
        raf.close();
      } 
    } 
  }
  
  private String getItemId(GenericClient c) {
    String item_id = null;
    try {
      item_id = Common.getMD5(new ByteArrayInputStream(c.url.getBytes("UTF8")));
    } catch (Exception exception) {}
    item_id = item_id.substring(item_id.length() - 6);
    return item_id;
  }
  
  private boolean journaling(String item_id) {
    synchronized (journal_lock) {
      String journal_path_parent = "./multi_journal/" + item_id + "/";
      File_S f = new File_S(journal_path_parent);
      if (!f.exists())
        return false; 
      File_S[] time_stamp_list = (File_S[])f.listFiles();
      if (time_stamp_list == null || time_stamp_list.length == 0)
        return false; 
      for (int x = 0; x < time_stamp_list.length; x++) {
        if (time_stamp_list[x].isDirectory()) {
          File_S time_stamp_item = new File_S(time_stamp_list[x]);
          (new File_S(String.valueOf(time_stamp_item.getPath()) + "/.DS_Store")).delete();
          if (time_stamp_item.exists()) {
            File_S[] folders = (File_S[])time_stamp_item.listFiles();
            if (folders == null || folders.length == 0) {
              time_stamp_item.delete();
              return false;
            } 
            for (int xx = 0; xx < folders.length; xx++) {
              if (folders[xx].isDirectory())
                try {
                  Properties config_wrapper = null;
                  if ((new File_S(String.valueOf(folders[xx].getPath()) + "/config.XML")).exists()) {
                    config_wrapper = (Properties)Common.readXMLObject(String.valueOf(folders[xx].getPath()) + "/config.XML");
                  } else {
                    config_wrapper = (Properties)Common.readXMLObject(String.valueOf(folders[xx].getPath()) + "/config.XML.locked");
                  } 
                  if (config_wrapper.getProperty("async", "false").equals("false"))
                    return true; 
                } catch (Exception e) {
                  Common.log("SERVER", 2, e);
                }  
            } 
          } 
        } 
      } 
      return false;
    } 
  }
  
  public void playJournal() {
    synchronized (replication_lock) {
      if (replicating)
        return; 
      replicating = true;
      replication_status.put("status1", "replicating");
      replication_status.put("status2", "starting");
      replication_status.put("status3", "starting");
      replication_status.put("error", "");
    } 
    try {
      for (int x = 0; x < this.clients.size(); x++) {
        GenericClient c = this.clients.elementAt(x);
        String item_id = getItemId(c);
        replication_status.put("status2", String.valueOf(x) + ":" + item_id + ":checking");
        if (journaling(item_id)) {
          replication_status.put("status2", String.valueOf(x) + ":" + item_id + ":journaling");
          String journal_path_parent = "./multi_journal/" + item_id + "/";
          File_S f = new File_S(journal_path_parent);
          File_S[] list = (File_S[])f.listFiles();
          Vector start_times = new Vector();
          replication_status.put("status2", String.valueOf(x) + ":" + item_id + ":journaling:sorting:" + list.length);
          for (int xx = 0; xx < list.length; xx++) {
            try {
              start_times.addElement((new StringBuffer(String.valueOf(Long.parseLong(list[xx].getName())))).toString());
            } catch (Exception exception) {}
          } 
          long[] times = new long[start_times.size()];
          int i;
          for (i = 0; i < start_times.size(); i++)
            times[i] = Long.parseLong(start_times.elementAt(i).toString()); 
          Arrays.sort(times);
          for (i = 0; i < start_times.size(); i++)
            start_times.setElementAt((new StringBuffer(String.valueOf(times[i]))).toString(), i); 
          try {
            long start_count = start_times.size();
            System.getProperties().put("crushftp.replciation.vfs.count", (new StringBuffer(String.valueOf(start_count))).toString());
            replication_status.put("status2", String.valueOf(x) + ":" + item_id + ":journaling:processing:" + start_count);
            while (start_times.size() > 0) {
              long start_time = Long.parseLong(start_times.remove(0).toString());
              replication_status.put("status2", String.valueOf(x) + ":" + item_id + ":journaling:processing:" + start_count + ":current:" + new Date(start_time));
              String journal_path = String.valueOf(journal_path_parent) + start_time + "/";
              File_S[] folders = (File_S[])(new File_S(journal_path)).listFiles();
              replication_status.put("last_item_client_id", item_id);
              replication_status.put("last_item_id", (new StringBuffer(String.valueOf(start_time))).toString());
              replication_status.put("last_item_path", journal_path);
              replication_status.put("last_item_date", new Date(start_time));
              replication_status.put("status3", "remaining:" + start_times.size() + ":path:" + journal_path);
              for (int j = 0; j < folders.length; j++) {
                if (folders[j].isDirectory()) {
                  if (!(new File_S(String.valueOf(folders[j].getPath()) + "/config.XML")).exists()) {
                    Common.log("REPLICATION", 0, "Server replication journal playback skipped due to upload in progress...:" + folders[j].getPath() + ".  Will try later.");
                    replication_status.put("status3", "remaining:" + start_times.size() + ":path:" + folders[j].getPath() + ":skipping due to upload in progress");
                    return;
                  } 
                  Properties config_wrapper = (Properties)Common.readXMLObject(String.valueOf(folders[j].getPath()) + "/config.XML");
                  if (config_wrapper.getProperty("x").equals((new StringBuffer(String.valueOf(x))).toString())) {
                    boolean delete_journal = false;
                    replication_status.put("last_item_action", (new StringBuffer(String.valueOf(config_wrapper.getProperty("action")))).toString());
                    replication_status.put("last_item_path1", (new StringBuffer(String.valueOf(config_wrapper.getProperty("path1")))).toString());
                    replication_status.put("last_item_path2", (new StringBuffer(String.valueOf(config_wrapper.getProperty("path2")))).toString());
                    replication_status.put("last_item_path3", (new StringBuffer(String.valueOf(config_wrapper.getProperty("path3")))).toString());
                    if (Thread.currentThread().getName().indexOf(":") < 0)
                      Thread.currentThread().setName(String.valueOf(Thread.currentThread().getName()) + "~"); 
                    String thread_name = Thread.currentThread().getName().substring(0, Thread.currentThread().getName().indexOf("~") + 1);
                    Thread.currentThread().setName(String.valueOf(thread_name) + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1"));
                    replication_status.put("status3", "remaining:" + start_times.size() + ":path:" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1"));
                    Common.log("REPLICATION", 0, Thread.currentThread().getName());
                    c.setConfig("pgpDecryptUpload", null);
                    c.setConfig("pgpPrivateKeyUploadPath", null);
                    c.setConfig("pgpPrivateKeyUploadPassword", null);
                    c.setConfig("pgpEncryptUpload", null);
                    c.setConfig("pgpAsciiUpload", null);
                    c.setConfig("pgpPublicKeyUploadPath", null);
                    c.setConfig("pgpDecryptDownload", null);
                    c.setConfig("pgpPrivateKeyDownloadPath", null);
                    c.setConfig("pgpPrivateKeyDownloadPassword", null);
                    c.setConfig("pgpEncryptDownload", null);
                    c.setConfig("pgpAsciiDownload", null);
                    c.setConfig("pgpPublicKeyDownloadPath", null);
                    c.setConfig("syncName", null);
                    c.setConfig("syncRevisionsPath", null);
                    c.setConfig("syncUploadOnly", null);
                    if (config_wrapper.getProperty("action").equals("upload")) {
                      OutputStream out = null;
                      try {
                        Properties config_tmp = (Properties)config_wrapper.get("config");
                        if (config_tmp != null) {
                          Properties item_tmp = (Properties)config_tmp.get("item");
                          if (item_tmp != null) {
                            String privs = item_tmp.getProperty("privs", "");
                            if (privs.indexOf("(pgp") >= 0)
                              for (int xxx = 0; xxx < (privs.split("\\(")).length; xxx++) {
                                String priv = privs.split("\\(")[xxx];
                                if (!priv.equals("")) {
                                  priv = priv.substring(0, priv.length() - 1).trim();
                                  if (priv.indexOf("=") >= 0)
                                    c.setConfig(priv.split("=")[0], priv.substring(priv.indexOf("=") + 1)); 
                                } 
                              }  
                          } 
                        } 
                        out = c.upload(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), Long.parseLong(config_wrapper.getProperty("startPos")), config_wrapper.getProperty("truncate").equals("true"), config_wrapper.getProperty("binary").equals("true"));
                        InputStream inp = new FileInputStream(new File_S(String.valueOf(folders[j].getPath()) + "/upload"));
                        long file_size = (new File_S(String.valueOf(folders[j].getPath()) + "/upload")).length();
                        try {
                          byte[] b = new byte[65535];
                          int bytesRead = 0;
                          long total_bytes = 0L;
                          long loop_time = 0L;
                          while (bytesRead >= 0) {
                            bytesRead = inp.read(b);
                            if (bytesRead >= 0) {
                              out.write(b, 0, bytesRead);
                              total_bytes += bytesRead;
                            } 
                            if (System.currentTimeMillis() - loop_time > 10000L) {
                              Thread.currentThread().setName(String.valueOf(thread_name) + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + Common.format_bytes_short(total_bytes) + " of " + Common.format_bytes_short(file_size));
                              Common.log("REPLICATION", 0, Thread.currentThread().getName());
                              loop_time = System.currentTimeMillis();
                            } 
                          } 
                        } catch (Exception e) {
                          if (!e.getMessage().equalsIgnoreCase("Socket closed") && !e.getMessage().equalsIgnoreCase("Connection reset"))
                            Common.log("REPLICATION", 2, e); 
                          replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        } finally {
                          inp.close();
                          out.close();
                          delete_journal = true;
                        } 
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                        try {
                          out.close();
                        } catch (Exception exception) {}
                      } 
                    } else if (config_wrapper.getProperty("action").equals("upload_0_byte")) {
                      try {
                        c.upload_0_byte(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)));
                        delete_journal = true;
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                      } 
                    } else if (config_wrapper.getProperty("action").equals("delete")) {
                      try {
                        delete_journal = c.delete(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)));
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                      } 
                    } else if (config_wrapper.getProperty("action").equals("makedir")) {
                      try {
                        delete_journal = c.makedir(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)));
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                      } 
                    } else if (config_wrapper.getProperty("action").equals("makedirs")) {
                      try {
                        delete_journal = c.makedirs(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)));
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                      } 
                    } else if (config_wrapper.getProperty("action").equals("rename")) {
                      try {
                        delete_journal = c.rename(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), fixPath(config_wrapper.getProperty("path2"), this.vItems.get(x)));
                        if (!delete_journal && System.getProperty("crushftp.journal.ignore_rename", "false").equals("true"))
                          delete_journal = true; 
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        if (!delete_journal && System.getProperty("crushftp.journal.ignore_rename_error", "false").equals("true"))
                          delete_journal = true; 
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                      } 
                    } else if (config_wrapper.getProperty("action").equals("list")) {
                      try {
                        c.list(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), new Vector());
                        delete_journal = true;
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                      } 
                    } else if (config_wrapper.getProperty("action").equals("mdtm")) {
                      try {
                        delete_journal = c.mdtm(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), Long.parseLong(config_wrapper.getProperty("path2")));
                        if (!delete_journal && System.getProperty("crushftp.journal.ignore_mdtm", "false").equals("true"))
                          delete_journal = true; 
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        if (!delete_journal && System.getProperty("crushftp.journal.ignore_mdtm_error", "false").equals("true")) {
                          delete_journal = true;
                        } else {
                          start_times.clear();
                        } 
                      } 
                    } else if (config_wrapper.getProperty("action").equals("setMod")) {
                      try {
                        c.setMod(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), config_wrapper.getProperty("path2"), config_wrapper.getProperty("path3"));
                        delete_journal = true;
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                      } 
                    } else if (config_wrapper.getProperty("action").equals("setOwner")) {
                      try {
                        c.setOwner(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), config_wrapper.getProperty("path2"), config_wrapper.getProperty("path3"));
                        delete_journal = true;
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                      } 
                    } else if (config_wrapper.getProperty("action").equals("setGroup")) {
                      try {
                        c.setGroup(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), config_wrapper.getProperty("path2"), config_wrapper.getProperty("path3"));
                        delete_journal = true;
                      } catch (Exception e) {
                        c.setConfig("error", e);
                        replication_status.put("error", e + ":" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":" + config_wrapper.getProperty("path2") + ":" + config_wrapper.getProperty("path3"));
                        Common.log("REPLICATION", 1, e);
                        start_times.clear();
                      } 
                    } else {
                      delete_journal = true;
                    } 
                    replication_status.put("status3", "remaining:" + start_times.size() + ":path:" + folders[j].getPath() + config_wrapper.getProperty("action") + ":" + config_wrapper.getProperty("path1") + ":delete_journal:" + delete_journal);
                    if (delete_journal) {
                      Common.recurseDelete(folders[j].getPath(), false);
                      (new File_S(String.valueOf(journal_path) + ".DS_Store")).delete();
                      (new File_S(journal_path)).delete();
                      (new File_S(String.valueOf(journal_path_parent) + ".DS_Store")).delete();
                      (new File_S(journal_path_parent)).delete();
                    } 
                    Thread.currentThread().setName(thread_name);
                  } 
                } 
              } 
            } 
          } catch (Exception e) {
            Common.log("REPLICATION", 1, e);
            replication_status.put("error", e + ":general");
          } 
        } 
      } 
    } finally {
      synchronized (replication_lock) {
        replicating = false;
        replication_status.put("status1", "idle");
        replication_status.put("status2", String.valueOf(replication_status.getProperty("status2", "")) + ":COMPLETE");
        replication_status.put("status3", String.valueOf(replication_status.getProperty("status3", "")) + ":COMPLETE");
      } 
    } 
  }
}
