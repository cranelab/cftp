package com.crushftp.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class HTTPBufferedClient extends HTTPClient {
  public static Properties mem = new Properties();
  
  public static Vector unpublished_changes = new Vector();
  
  public static long last_action = 0L;
  
  static boolean http_buffer_cleaner_running = false;
  
  public RandomAccessFile raf2;
  
  public HTTPBufferedClient(String url, String header, Vector log) {
    super(url, header, log);
    this.raf2 = null;
    if (!url.endsWith("/"))
      url = String.valueOf(url) + "/"; 
    this.url = url;
    this.config.put("protocol", "HTTP");
    if (!http_buffer_cleaner_running) {
      http_buffer_cleaner_running = true;
      try {
        Worker.startWorker(new Runnable(this) {
              final HTTPBufferedClient this$0;
              
              public void run() {
                while (true) {
                  try {
                    Thread.sleep(1000L);
                  } catch (InterruptedException interruptedException) {}
                  while (HTTPBufferedClient.unpublished_changes.size() > 0) {
                    if (System.currentTimeMillis() - HTTPBufferedClient.last_action < 10000L)
                      break; 
                    try {
                      String command = HTTPBufferedClient.unpublished_changes.elementAt(0).toString().split(";")[0];
                      String path = HTTPBufferedClient.unpublished_changes.elementAt(0).toString().split(";")[1];
                      if (command.equals("upload")) {
                        if ((new File(HTTPBufferedClient.getUid(path))).exists()) {
                          this.this$0.log("BUFFERED:" + command + ":" + path);
                          Common.copyStreams(new FileInputStream(HTTPBufferedClient.getUid(path)), this.this$0.upload4(path, 0L, true, true), true, true);
                          (new File(HTTPBufferedClient.getUid(path))).delete();
                          HTTPBufferedClient.mem.remove("stat:" + HTTPBufferedClient.noSlash(path));
                        } 
                      } else if (command.equals("mdtm")) {
                        this.this$0.log("BUFFERED:" + command + ":" + path);
                        this.this$0.doAction2("mdtm", path, (new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date(Long.parseLong(HTTPBufferedClient.unpublished_changes.elementAt(0).toString().split(";")[2])))).equals("");
                        HTTPBufferedClient.mem.remove("stat:" + HTTPBufferedClient.noSlash(path));
                      } else if (command.equals("rename")) {
                        String path2 = HTTPBufferedClient.unpublished_changes.elementAt(0).toString().split(";")[2];
                        this.this$0.log("BUFFERED:" + command + ":" + path + ":" + path2);
                        this.this$0.doAction2("rename", path, path2);
                        HTTPBufferedClient.mem.remove("stat:" + HTTPBufferedClient.noSlash(path));
                        HTTPBufferedClient.mem.remove("stat:" + HTTPBufferedClient.noSlash(path2));
                        HTTPBufferedClient.mem.remove("list:" + HTTPBufferedClient.noSlash(Common.all_but_last(path)));
                        HTTPBufferedClient.mem.remove("list:" + HTTPBufferedClient.noSlash(Common.all_but_last(path2)));
                      } else if (command.equals("delete")) {
                        this.this$0.log("BUFFERED:" + command + ":" + path);
                        this.this$0.doAction2("delete", path, "");
                        HTTPBufferedClient.mem.remove("stat:" + HTTPBufferedClient.noSlash(path));
                        HTTPBufferedClient.mem.remove("list:" + HTTPBufferedClient.noSlash(Common.all_but_last(path)));
                      } 
                      HTTPBufferedClient.unpublished_changes.removeElementAt(0);
                    } catch (Exception exception) {}
                  } 
                } 
              }
            });
      } catch (IOException iOException) {}
    } 
  }
  
  public static boolean isBusy() {
    return (unpublished_changes.size() > 0);
  }
  
  public void close() throws Exception {
    if (this.in != null)
      this.in.close(); 
    if (this.out != null)
      this.out.close(); 
    this.in = null;
    this.out = null;
  }
  
  public Properties stat(String path) throws Exception {
    if (mem.containsKey("stat:" + noSlash(path))) {
      Properties properties = (Properties)mem.get("stat:" + noSlash(path));
      if (System.currentTimeMillis() - Long.parseLong(properties.getProperty("time")) < 60000L)
        return (Properties)properties.get("obj"); 
      mem.remove("stat:" + noSlash(path));
    } 
    Properties stat = super.stat(path);
    Properties p = new Properties();
    p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    if (stat != null)
      p.put("obj", stat); 
    if ((new File(getUid(path))).exists() && stat != null)
      stat.put("size", (new StringBuffer(String.valueOf((new File(getUid(path))).length()))).toString()); 
    mem.put("stat:" + noSlash(path), p);
    return stat;
  }
  
  public Properties list2(String path, Vector list) throws Exception {
    log("list2:" + path);
    if (mem.containsKey("list:" + noSlash(path))) {
      Properties properties = (Properties)mem.get("list:" + noSlash(path));
      log("stat_cache_lookup_list:" + noSlash(path) + ":" + (System.currentTimeMillis() - Long.parseLong(properties.getProperty("time"))) + "ms");
      if (System.currentTimeMillis() - Long.parseLong(properties.getProperty("time")) < 60000L) {
        Properties properties1 = (Properties)properties.get("obj");
        Vector vector = (Vector)properties1.get("listing");
        log("list2-end (cache):" + path + ":" + vector.size());
        list.addAll(vector);
        return (Properties)properties1.clone();
      } 
      mem.remove("list:" + noSlash(path));
    } 
    Properties listingProp = super.list2(path, list);
    Vector list2 = (Vector)listingProp.get("listing");
    Properties p = new Properties();
    p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    p.put("obj", listingProp.clone());
    mem.put("list:" + noSlash(path), p);
    for (int x = 0; x < list2.size(); x++) {
      Properties stat = list2.elementAt(x);
      Properties p2 = new Properties();
      p2.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      p2.put("obj", stat);
      mem.put("stat:" + noSlash(path) + "/" + stat.getProperty("name"), p2);
      log("stat_cached:" + noSlash(path) + "/" + stat.getProperty("name"));
      System.out.println("stat_raw:" + stat);
    } 
    log("list2-end:" + path + ":" + list2.size());
    return listingProp;
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary, String paths, int rev) throws Exception {
    last_action = System.currentTimeMillis();
    if (!(new File(getUid(path))).exists()) {
      InputStream in_tmp = super.download3(path, startPos, endPos, binary, paths, rev);
      Thread t = new Thread(new Runnable(this, path, in_tmp) {
            final HTTPBufferedClient this$0;
            
            private final String val$path;
            
            private final InputStream val$in_tmp;
            
            public void run() {
              try {
                (new File(HTTPBufferedClient.getUid(this.val$path))).delete();
                Common.copyStreams(this.val$in_tmp, new FileOutputStream(HTTPBufferedClient.getUid(this.val$path), false), true, true);
                (new File(HTTPBufferedClient.getUid(this.val$path))).deleteOnExit();
              } catch (Exception exception) {}
            }
          });
      t.start();
      t.join(10000L);
      if (t.isAlive()) {
        log("download taking too long for " + path + " spawing background thread for double download while we archive a copy.");
        return super.download3(path, startPos, endPos, binary, paths, rev);
      } 
    } 
    this.in = new null.InputWrapper(this, path, startPos, endPos, startPos, binary, path, endPos);
    return this.in;
  }
  
  protected OutputStream upload4(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    return super.upload3(path, startPos, truncate, binary);
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    last_action = System.currentTimeMillis();
    mem.remove("stat:" + noSlash(path));
    unpublished_changes.addElement("upload;" + noSlash(path));
    this.out = new null.OutputWrapper(this, path, startPos, truncate, binary, path, startPos, truncate);
    return this.out;
  }
  
  public boolean upload_0_byte(String path) throws Exception {
    last_action = System.currentTimeMillis();
    mem.remove("stat:" + noSlash(path));
    RandomAccessFile raf = new RandomAccessFile(getUid(path), "rw");
    raf.setLength(0L);
    raf.close();
    (new File(getUid(path))).deleteOnExit();
    return doAction("upload_0_byte", path, "").trim().equalsIgnoreCase("OK");
  }
  
  public boolean delete(String path) throws Exception {
    last_action = System.currentTimeMillis();
    Properties new_p = remove_from_list(noSlash(Common.all_but_last(path)), path);
    mem.remove("stat:" + noSlash(path));
    (new File(getUid(path))).delete();
    unpublished_changes.addElement("delete;" + noSlash(path));
    mem.put("list:" + noSlash(Common.all_but_last(path)), new_p);
    return doAction("delete", path, "").equals("");
  }
  
  public boolean makedir(String path) throws Exception {
    last_action = System.currentTimeMillis();
    mem.remove("stat:" + noSlash(path));
    mem.remove("list:" + noSlash(Common.all_but_last(path)));
    return doAction("makedir", path, "").equals("");
  }
  
  public boolean makedirs(String path) throws Exception {
    return makedir(path);
  }
  
  public Properties remove_from_list(String parent_path, String path) throws Exception {
    Vector list_tmp = new Vector();
    list(parent_path, list_tmp);
    Properties listingProp = new Properties();
    listingProp.put("listing", list_tmp);
    Properties p = new Properties();
    p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
    p.put("obj", listingProp.clone());
    int found = -1;
    for (int x = 0; x < list_tmp.size(); x++) {
      Properties stat = list_tmp.elementAt(x);
      if (stat.getProperty("name").equals(noSlash(Common.last(path))))
        found = x; 
    } 
    if (found >= 0)
      list_tmp.removeElementAt(found); 
    return p;
  }
  
  public boolean rename(String rnfr, String rnto) throws Exception {
    last_action = System.currentTimeMillis();
    Properties rnfr_stat_rnto = stat(rnfr);
    Properties new_p = remove_from_list(Common.all_but_last(rnfr), rnfr);
    mem.remove("stat:" + noSlash(rnfr));
    mem.remove("stat:" + noSlash(rnto));
    mem.put("list:" + noSlash(Common.all_but_last(rnfr)), new_p);
    mem.remove("list:" + noSlash(Common.all_but_last(rnfr)));
    if ((new File(getUid(rnfr))).exists()) {
      rnfr_stat_rnto.put("name", noSlash(Common.last(rnto)));
      String url_tmp = rnfr_stat_rnto.getProperty("url");
      rnfr_stat_rnto.put("url", String.valueOf(url_tmp.substring(0, url_tmp.lastIndexOf(rnfr))) + rnto);
      Vector list_tmp = new Vector();
      list(Common.all_but_last(rnto), list_tmp);
      list_tmp.addElement(rnfr_stat_rnto);
      Properties listingProp = new Properties();
      listingProp.put("listing", list_tmp);
      Properties p = new Properties();
      p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      p.put("obj", listingProp.clone());
      mem.remove("list:" + noSlash(Common.all_but_last(rnto)));
      p = new Properties();
      p.put("time", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString());
      p.put("obj", rnfr_stat_rnto.clone());
      mem.put("stat:" + noSlash(rnto), p);
      (new File(getUid(rnto))).delete();
      (new File(getUid(rnfr))).renameTo(new File(getUid(rnto)));
      unpublished_changes.addElement("rename;" + noSlash(rnfr) + ";" + noSlash(rnto));
    } else if (rnfr_stat_rnto != null) {
      doAction2("rename", rnfr, rnto);
    } 
    (new File(getUid(rnfr))).deleteOnExit();
    (new File(getUid(rnto))).deleteOnExit();
    return true;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    last_action = System.currentTimeMillis();
    mem.remove("stat:" + noSlash(path));
    unpublished_changes.addElement("mdtm;" + noSlash(path) + ";" + modified);
    return true;
  }
  
  public String doAction2(String command, String param1, String param2) throws Exception {
    return doAction(command, param1, param2);
  }
  
  public static String getUid(String path) {
    String uid = String.valueOf(System.getProperty("crushftpdrive.tmp", System.getProperty("java.io.tmpdir"))) + noSlash(path).replace('/', '_');
    return uid;
  }
}
