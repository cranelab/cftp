package com.crushftp.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

public class GenericClientMulti extends GenericClient {
  public Vector clients = null;
  
  public Vector vItems = null;
  
  public Properties originalvItem = null;
  
  public static Object journal_lock = new Object();
  
  public GenericClientMulti(String header, Vector log, Properties originalvItem, Vector vItems, Vector clients) {
    super(header, log);
    this.clients = clients;
    this.originalvItem = originalvItem;
    this.vItems = vItems;
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
      result = c.login(username, password, clientid);
    } 
    return result;
  }
  
  public void logout() throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      c.logout();
    } 
  }
  
  public void close() throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      c.close();
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
      Vector list2 = new Vector();
      try {
        list2 = c.list(fixPath(path, this.vItems.get(x)), new Vector());
      } catch (FileNotFoundException e) {
        Common.log("SERVER", 2, e);
      } catch (Exception e) {
        if (x == 0)
          throw e; 
      } 
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
      GenericClient c = this.clients.elementAt(x);
      String item_id = getItemId(c);
      try {
        if (x > 0 && journaling(item_id))
          throw new IOException("Journaling..."); 
        outs.addElement(c.upload(fixPath(path, this.vItems.get(x)), startPos, truncate, binary));
      } catch (Exception e) {
        outs.addElement(null);
        Common.log("SERVER", 1, e);
      } 
      item_ids.addElement(item_id);
    } 
    this.out = new GenericClientMulti$1$OutputMulti(this, outs, item_ids, path, startPos, truncate, binary);
    return this.out;
  }
  
  public boolean upload_0_byte(String path) throws Exception {
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
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
    return ok;
  }
  
  public boolean delete(String path) throws Exception {
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      String item_id = getItemId(c);
      try {
        if (x > 0 && journaling(item_id))
          throw new IOException("Journaling..."); 
        boolean ok2 = c.delete(fixPath(path, this.vItems.get(x)));
        if (!ok2 && journaling(item_id)) {
          ok = false;
          throw new IOException("Journaling...");
        } 
      } catch (Exception e) {
        if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
          writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, path, path, path, 0L, true, true, "delete", item_id);
        } else {
          throw e;
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
        if (!ok2 && journaling(item_id)) {
          ok = false;
          if (stat(path) == null)
            throw new IOException("Journaling..."); 
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
        if (!ok2 && journaling(item_id)) {
          ok = false;
          if (stat(path) == null)
            throw new IOException("Journaling..."); 
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
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      String item_id = getItemId(c);
      try {
        if (x > 0 && journaling(item_id))
          throw new IOException("Journaling..."); 
        boolean ok2 = c.rename(fixPath(rnfr, this.vItems.get(x)), fixPath(rnto, this.vItems.get(x)));
        if (!ok2 && journaling(item_id)) {
          ok = false;
          throw new IOException("Journaling...");
        } 
      } catch (Exception e) {
        if (x > 0 && System.getProperty("crushftp.multi_journal", "false").equals("true")) {
          writeJournal((byte[])null, 0, 0, this.clients.elementAt(x), Common.uidg(), x, rnfr, rnto, "", 0L, true, true, "rename", item_id);
        } else {
          throw e;
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
        Common.log("SERVER", 1, e);
      } 
    } 
    return null;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    boolean ok = true;
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      String item_id = getItemId(c);
      try {
        if (x > 0 && journaling(item_id))
          throw new IOException("Journaling..."); 
        boolean ok2 = c.mdtm(fixPath(path, this.vItems.get(x)), modified);
        if (!ok2 && journaling(item_id)) {
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
    return ok;
  }
  
  public void setMod(String path, String val, String param) throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
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
  
  public void setOwner(String path, String val, String param) throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
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
  
  public void setGroup(String path, String val, String param) throws Exception {
    for (int x = 0; x < this.clients.size(); x++) {
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
  
  public void writeJournal(byte[] b, int off, int len, GenericClient c, long start_time, int x, String path1, String path2, String path3, long startPos, boolean truncate, boolean binary, String action, String item_id) throws IOException {
    synchronized (journal_lock) {
      String journal_path = "./multi_journal/" + item_id + "/" + start_time + "/" + c.toString() + "/";
      if (!(new File(journal_path)).exists()) {
        (new File(journal_path)).mkdirs();
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
        Common.writeXMLObject(String.valueOf(journal_path) + "config.XML", config_wrapper, "config");
      } 
      if (action.equals("close")) {
        Common.recurseDelete(journal_path, false);
        String journal_path_parent = "./multi_journal/" + item_id + "/";
        (new File(String.valueOf(Common.all_but_last(journal_path)) + ".DS_Store")).delete();
        (new File(Common.all_but_last(journal_path))).delete();
        (new File(String.valueOf(journal_path_parent) + ".DS_Store")).delete();
        (new File(journal_path_parent)).delete();
      } else if (b != null) {
        RandomAccessFile raf = new RandomAccessFile(String.valueOf(journal_path) + action, "rw");
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
    String journal_path_parent = "./multi_journal/" + item_id + "/";
    File f = new File(journal_path_parent);
    if (!f.exists())
      return false; 
    File[] list = f.listFiles();
    if (list == null || list.length == 0)
      return false; 
    int count = 0;
    for (int x = 0; x < list.length; x++) {
      if (list[x].isDirectory()) {
        (new File(String.valueOf(list[x].getPath()) + "/.DS_Store")).delete();
        list[x].delete();
        if (list[x].exists())
          count++; 
      } 
    } 
    return (count > 0);
  }
  
  public void playJournal() {
    for (int x = 0; x < this.clients.size(); x++) {
      GenericClient c = this.clients.elementAt(x);
      String item_id = getItemId(c);
      if (journaling(item_id)) {
        String journal_path_parent = "./multi_journal/" + item_id + "/";
        File f = new File(journal_path_parent);
        File[] list = f.listFiles();
        Vector start_times = new Vector();
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
          while (start_times.size() > 0) {
            long start_time = Long.parseLong(start_times.remove(0).toString());
            String journal_path = String.valueOf(journal_path_parent) + start_time + "/";
            File[] folders = (new File(journal_path)).listFiles();
            for (int j = 0; j < folders.length; j++) {
              if (folders[j].isDirectory()) {
                Properties config_wrapper = (Properties)Common.readXMLObject(String.valueOf(folders[j].getPath()) + "/config.XML");
                if (config_wrapper.getProperty("x").equals((new StringBuffer(String.valueOf(x))).toString())) {
                  boolean delete_journal = false;
                  if (config_wrapper.getProperty("action").equals("upload")) {
                    OutputStream out = null;
                    try {
                      out = c.upload(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), Long.parseLong(config_wrapper.getProperty("startPos")), config_wrapper.getProperty("truncate").equals("true"), config_wrapper.getProperty("binary").equals("true"));
                      InputStream inp = new FileInputStream(String.valueOf(folders[j].getPath()) + "/upload");
                      try {
                        byte[] b = new byte[65535];
                        int bytesRead = 0;
                        while (bytesRead >= 0) {
                          bytesRead = inp.read(b);
                          if (bytesRead >= 0)
                            out.write(b, 0, bytesRead); 
                        } 
                        delete_journal = true;
                      } catch (Exception e) {
                        if (!e.getMessage().equalsIgnoreCase("Socket closed") && !e.getMessage().equalsIgnoreCase("Connection reset"))
                          Common.log("SERVER", 2, e); 
                      } finally {
                        inp.close();
                        out.close();
                      } 
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                      try {
                        out.close();
                      } catch (Exception exception) {}
                    } 
                  } else if (config_wrapper.getProperty("action").equals("upload_0_byte")) {
                    try {
                      c.upload_0_byte(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)));
                      delete_journal = true;
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                    } 
                  } else if (config_wrapper.getProperty("action").equals("delete")) {
                    try {
                      delete_journal = c.delete(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)));
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                    } 
                  } else if (config_wrapper.getProperty("action").equals("makedir")) {
                    try {
                      delete_journal = c.makedir(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)));
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                    } 
                  } else if (config_wrapper.getProperty("action").equals("makedirs")) {
                    try {
                      delete_journal = c.makedirs(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)));
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                    } 
                  } else if (config_wrapper.getProperty("action").equals("rename")) {
                    try {
                      delete_journal = c.rename(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), fixPath(config_wrapper.getProperty("path2"), this.vItems.get(x)));
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                    } 
                  } else if (config_wrapper.getProperty("action").equals("mdtm")) {
                    try {
                      delete_journal = c.mdtm(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), Long.parseLong(config_wrapper.getProperty("path2")));
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                    } 
                  } else if (config_wrapper.getProperty("action").equals("setMod")) {
                    try {
                      c.setMod(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), config_wrapper.getProperty("path2"), config_wrapper.getProperty("path3"));
                      delete_journal = true;
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                    } 
                  } else if (config_wrapper.getProperty("action").equals("setOwner")) {
                    try {
                      c.setOwner(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), config_wrapper.getProperty("path2"), config_wrapper.getProperty("path3"));
                      delete_journal = true;
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                    } 
                  } else if (config_wrapper.getProperty("action").equals("setGroup")) {
                    try {
                      c.setGroup(fixPath(config_wrapper.getProperty("path1"), this.vItems.get(x)), config_wrapper.getProperty("path2"), config_wrapper.getProperty("path3"));
                      delete_journal = true;
                    } catch (Exception e) {
                      Common.log("SERVER", 1, e);
                    } 
                  } 
                  if (delete_journal) {
                    Common.recurseDelete(folders[j].getPath(), false);
                    (new File(String.valueOf(journal_path) + ".DS_Store")).delete();
                    (new File(journal_path)).delete();
                    (new File(String.valueOf(journal_path_parent) + ".DS_Store")).delete();
                    (new File(journal_path_parent)).delete();
                  } 
                } 
              } 
            } 
          } 
        } catch (Exception e) {
          Common.log("SERVER", 1, e);
        } 
      } 
    } 
  }
}
