package com.crushftp.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

public class FileClient extends GenericClient {
  public static Properties dirCachePerm = new Properties();
  
  public static boolean memCache = System.getProperty("crushftp.memcache", "false").equals("true");
  
  int lsBytesRead = 0;
  
  Properties dirCache = new Properties();
  
  public FileClient(String url, String header, Vector log) {
    super(header, log);
    this.url = (new VRL(url)).getPath();
  }
  
  public void logout() throws Exception {
    close();
    this.dirCache = new Properties();
    this.logQueue = new Vector();
  }
  
  public void freeCache() {
    this.dirCache = new Properties();
    this.logQueue = new Vector();
  }
  
  private String getAbsolutePath(File_U f) {
    return Common.machine_is_windows() ? ("/" + f.getAbsolutePath().replace('\\', '/')) : f.getAbsolutePath();
  }
  
  public Properties stat(String path) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    File_U test = new File_U(path);
    if (getAbsolutePath(test).toLowerCase().indexOf(".zip/") >= 0 && System.getProperty("crushftp.zipstat", "true").equals("true")) {
      int pos = path.toLowerCase().indexOf(".zip/");
      ZipClient zc = new ZipClient(path.substring(0, pos + 4), this.logHeader, this.logQueue);
      Properties zi = zc.stat("!" + getAbsolutePath(test).substring(pos + 5));
      test = new File_U(path.substring(0, pos + 4));
      if (zi != null)
        return zi; 
    } 
    if (!test.exists() && System.getProperty("crushftp.isTestCall", "false").equals("true"))
      throw new Exception("Item not found..."); 
    if (!test.exists())
      return null; 
    Properties allitems = new Properties();
    if (test.getParentFile() != null)
      getOSXListing(getAbsolutePath((File_U)test.getParentFile()), allitems, true); 
    Properties dir_item = new Properties();
    String name = test.getName();
    if (name.equals("") && (Common.machine_is_x() || Common.machine_is_unix() || Common.machine_is_linux())) {
      name = "localhost";
    } else if (name.equals("") && Common.machine_is_windows()) {
      name = test.getPath().substring(0, 2);
    } 
    dir_item.put("name", name.replaceAll("\r", "%0A").replaceAll("\n", "%0D"));
    dir_item.put("size", "0");
    dir_item.put("type", "FILE");
    if (test.isDirectory()) {
      dir_item.put("type", "DIR");
      dir_item.put("permissions", "drwxrwxrwx");
      dir_item.put("size", "1");
      if (this.config.getProperty("count_dir_items", "false").equals("true")) {
        int i = 0;
        File_U[] list = (File_U[])test.listFiles();
        for (int x = 0; list != null && x < list.length; x++) {
          if (!list[x].getName().startsWith("."))
            i++; 
        } 
        dir_item.put("size", (new StringBuffer(String.valueOf(i))).toString());
      } 
      if (this.config.getProperty("count_dir_size", "false").equals("true")) {
        long i = 0L;
        File_U[] list = (File_U[])test.listFiles();
        for (int x = 0; list != null && x < list.length; x++) {
          if (list[x].isFile())
            i += list[x].length(); 
        } 
        dir_item.put("size", (new StringBuffer(String.valueOf(i))).toString());
      } 
      if (!path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
    } else if (test.isFile()) {
      dir_item.put("permissions", "-rwxrwxrwx");
      dir_item.put("size", (new StringBuffer(String.valueOf(getSize(test)))).toString());
    } 
    dir_item.put("url", test.toURI().toURL().toExternalForm());
    dir_item.put("link", "false");
    if (Common.isSymbolicLink(getAbsolutePath(test)))
      dir_item.put("link", "true"); 
    dir_item.put("num_items", "1");
    dir_item.put("owner", "user");
    dir_item.put("group", "group");
    dir_item.put("protocol", "file");
    dir_item.put("root_dir", Common.all_but_last(path));
    setFileDateInfo(test, dir_item);
    if (allitems.get(test.getName()) != null)
      setOSXInfo((Properties)allitems.get(test.getName()), dir_item); 
    if (memCache)
      try {
        dirCachePerm.put(test.getCanonicalPath().replace('\\', '/'), dir_item);
      } catch (Exception exception) {} 
    return dir_item;
  }
  
  private long getSize(File_U test) {
    if (this.config.getProperty("checkEncryptedHeader", "false").equals("true"))
      return Common.getFileSize(test.getPath()); 
    return test.length();
  }
  
  public Vector list(String path, Vector list) throws Exception {
    if (!path.replace('\\', '/').endsWith("/"))
      path = String.valueOf(path) + "/"; 
    String originalPath = path;
    path = String.valueOf(this.url) + path.substring(1);
    File_U item = new File_U(path);
    if (!getAbsolutePath(item).toLowerCase().endsWith(".zip") && getAbsolutePath(item).toLowerCase().indexOf(".zip/") < 0 && !item.exists() && System.getProperty("crushftp.file_client_not_found_error", "true").equals("true"))
      throw new FileNotFoundException("No such item:" + item); 
    File_U[] items = (File_U[])item.listFiles();
    if (path.equals("/") && Common.machine_is_windows())
      items = (File_U[])File_U.listRoots(); 
    Properties allitems = new Properties();
    getOSXInfo(allitems, getAbsolutePath(item), true);
    if ((getAbsolutePath(item).toLowerCase().endsWith(".zip") || getAbsolutePath(item).toLowerCase().indexOf(".zip/") >= 0) && path.endsWith("/")) {
      int pos = getAbsolutePath(item).toLowerCase().indexOf(".zip");
      ZipClient zc = new ZipClient(getAbsolutePath(item).substring(0, pos + 4), this.logHeader, this.logQueue);
      String addOn = "";
      if (getAbsolutePath(item).toLowerCase().indexOf(".zip/") >= 0) {
        pos++;
        addOn = "/";
      } 
      zc.list("!" + getAbsolutePath(item).substring(pos + 4) + addOn, list);
      items = (File_U[])null;
    } else if (item.isFile()) {
      items = new File_U[1];
      items[0] = item;
    } 
    if (items == null)
      items = new File_U[0]; 
    this.dirCache.clear();
    Vector permCacheList = new Vector();
    for (int x = 0; x < items.length; x++) {
      File_U test = items[x];
      String tempName = test.getName();
      if (Common.machine_is_windows() && path.equals("/"))
        tempName = items[x].getPath().substring(0, 2); 
      String tempPath = String.valueOf(originalPath) + tempName;
      Properties dir_item = stat(tempPath);
      if (dir_item == null)
        Common.log("FILE_CLIENT", 1, "Linux?  Bad LANG setting in crushftp_init.sh?  Couldn't find item:" + tempPath); 
      String itemName = Common.normalize2(tempName);
      if (allitems.get(itemName) != null)
        setOSXInfo((Properties)allitems.get(itemName), dir_item); 
      long size = 0L;
      try {
        if (dir_item != null) {
          size = Long.parseLong(dir_item.getProperty("size", "0"));
          if (dir_item.getProperty("type").equalsIgnoreCase("FILE"))
            size = getSize(test); 
        } 
      } catch (Exception e) {
        Common.log("FILE_CLIENT", 1, "Invalid file, or dead alias:" + test);
        Common.log("FILE_CLIENT", 1, e);
      } 
      if (dir_item != null) {
        dir_item.put("size", (new StringBuffer(String.valueOf(size))).toString());
        list.add(dir_item);
        if (memCache)
          try {
            permCacheList.addElement(test.getCanonicalPath().replace('\\', '/'));
          } catch (Exception exception) {} 
      } 
    } 
    if (memCache)
      try {
        dirCachePerm.put(String.valueOf(item.getCanonicalPath().replace('\\', '/')) + "/", permCacheList);
      } catch (Exception exception) {} 
    this.dirCache.clear();
    return list;
  }
  
  private void getOSXInfo(Properties allitems, String realPath, boolean useOSXInfo) {
    File_U item = new File_U(realPath);
    getOSXListing(getAbsolutePath(item), allitems, useOSXInfo);
    if (Common.machine_is_x() || Common.machine_is_linux() || Common.machine_is_unix())
      if (item.isFile()) {
        if (allitems.get(getAbsolutePath(item)) != null)
          allitems.put(item.getName(), allitems.get(getAbsolutePath(item))); 
        allitems.remove(getAbsolutePath(item));
      }  
  }
  
  private void setOSXInfo(Properties p, Properties dir_item) {
    try {
      dir_item.put("owner", p.getProperty("owner"));
      dir_item.put("group", p.getProperty("group"));
      if (dir_item.getProperty("type", "").equals("DIR")) {
        dir_item.put("permissions", "d" + p.getProperty("permissions").substring(1));
      } else if (dir_item.getProperty("type", "").equals("FILE")) {
        dir_item.put("permissions", "-" + p.getProperty("permissions").substring(1));
      } 
      dir_item.put("month", p.getProperty("month"));
      dir_item.put("day", p.getProperty("day"));
      dir_item.put("time_or_year", p.getProperty("time_or_year"));
      dir_item.put("num_items", p.getProperty("num_items"));
      dir_item.put("link", p.getProperty("link", "false"));
      dir_item.put("linkedFile", p.getProperty("linkedFile", ""));
      String perm = String.valueOf(dir_item.getProperty("permissions", "-------------------")) + "-----------------------";
      String userP = changePermissions(perm.substring(1, 4));
      String userG = changePermissions(perm.substring(4, 7));
      String userW = changePermissions(perm.substring(7, 10));
      dir_item.put("permissionsNum", String.valueOf(userP) + userG + userW);
    } catch (Exception e) {
      Common.log("FILE_CLIENT", 1, e);
    } 
  }
  
  private String changePermissions(String s) {
    if (s.equalsIgnoreCase("--x"))
      return "1"; 
    if (s.equalsIgnoreCase("-w-"))
      return "2"; 
    if (s.equalsIgnoreCase("-wx"))
      return "3"; 
    if (s.equalsIgnoreCase("r--"))
      return "4"; 
    if (s.equalsIgnoreCase("r-x"))
      return "5"; 
    if (s.equalsIgnoreCase("rw-"))
      return "6"; 
    if (s.equalsIgnoreCase("rwx"))
      return "7"; 
    return "0";
  }
  
  private void setFileDateInfo(File_U test, Properties dir_item) {
    Date itemDate = new Date(test.lastModified());
    dir_item.put("modified", (new StringBuffer(String.valueOf(itemDate.getTime()))).toString());
    dir_item.put("month", months[Integer.parseInt(this.mm.format(itemDate))]);
    dir_item.put("day", this.dd.format(itemDate));
    String time_or_year = this.hhmm.format(itemDate);
    if (!this.yyyy.format(itemDate).equals(this.yyyy.format(new Date())) || System.getProperty("crushftp.ls.year", "false").equals("true"))
      time_or_year = this.yyyy.format(itemDate); 
    dir_item.put("time_or_year", time_or_year);
  }
  
  private void getOSXListing(String realPath, Properties all_dir_items, boolean useOSXInfo) {
    if (!useOSXInfo)
      return; 
    if (!System.getProperty("crushftp.lsla", "").equalsIgnoreCase("true"))
      return; 
    if (this.dirCache.containsKey(realPath)) {
      Properties oldCache = (Properties)this.dirCache.get(realPath);
      all_dir_items.putAll(oldCache);
      return;
    } 
    if (Common.machine_is_x() || Common.machine_is_linux() || Common.machine_is_unix()) {
      BufferedReader ls_in = null;
      Thread monitorThread = null;
      try {
        String data = "";
        this.lsBytesRead = 0;
        Common.log("FILE_CLIENT", 3, "ls -la " + realPath);
        Process ls_proc = Runtime.getRuntime().exec(new String[] { System.getProperty("crushftp.ls", "ls"), System.getProperty("crushftp.la", "-la"), realPath }new String[] { System.getProperty("crushftp.lang", "LANG=C") });
        monitorThread = new Thread(new FileClient$1$monitor(this, ls_proc));
        monitorThread.setName("ls -la " + realPath);
        monitorThread.start();
        ls_in = new BufferedReader(new InputStreamReader(ls_proc.getInputStream(), "UTF8"));
        while ((data = ls_in.readLine()) != null) {
          Common.log("FILE_CLIENT", 5, data);
          this.lsBytesRead += data.length();
          try {
            Properties dir_item = new Properties();
            if (!data.toUpperCase().startsWith("TOTAL ")) {
              if (data.toUpperCase().startsWith("D") || data.toUpperCase().startsWith("L")) {
                dir_item.put("type", "DIR");
              } else {
                dir_item.put("type", "FILE");
              } 
              StringTokenizer get_em = new StringTokenizer(data, " ");
              String permissions = String.valueOf(get_em.nextToken().trim()) + "-----------------";
              permissions = permissions.substring(0, 10);
              dir_item.put("permissions", permissions);
              dir_item.put("num_items", get_em.nextToken().trim());
              if (dir_item.getProperty("num_items", "0").length() > 3)
                dir_item.put("num_items", "999"); 
              dir_item.put("owner", (String.valueOf(get_em.nextToken().trim()) + "         ").substring(0, 8));
              dir_item.put("group", (String.valueOf(get_em.nextToken().trim()) + "         ").substring(0, 8));
              dir_item.put("size", get_em.nextToken().trim());
              dir_item.put("month", get_em.nextToken().trim());
              if (dir_item.getProperty("month").charAt(0) != (new StringBuffer(String.valueOf(dir_item.getProperty("month").charAt(0)))).toString().toUpperCase().charAt(0))
                dir_item.put("month", String.valueOf((new StringBuffer(String.valueOf(dir_item.getProperty("month").charAt(0)))).toString().toUpperCase()) + dir_item.getProperty("month").substring(1)); 
              if (dir_item.getProperty("month").indexOf("-") > 0) {
                dir_item.put("time_or_year", get_em.nextToken().trim());
                Date itemDate = this.yyyymmddHHmm.parse(String.valueOf(dir_item.getProperty("month")) + " " + dir_item.getProperty("time_or_year"));
                dir_item.put("month", months[Integer.parseInt(this.mm.format(itemDate))]);
                dir_item.put("day", this.dd.format(itemDate));
                String time_or_year = this.hhmm.format(itemDate);
                if (!this.yyyy.format(itemDate).equals(this.yyyy.format(new Date())))
                  time_or_year = this.yyyy.format(itemDate); 
                dir_item.put("time_or_year", time_or_year);
              } else if (dir_item.getProperty("month").trim().length() < 3) {
                String realDay = dir_item.getProperty("month", "");
                dir_item.put("month", get_em.nextToken().trim());
                dir_item.put("day", realDay);
                dir_item.put("time_or_year", get_em.nextToken().trim());
              } else {
                dir_item.put("day", get_em.nextToken().trim());
                dir_item.put("time_or_year", get_em.nextToken().trim());
              } 
              String name_data = get_em.nextToken();
              String searchName = String.valueOf(dir_item.getProperty("time_or_year")) + " " + name_data;
              name_data = data.substring(data.indexOf(name_data, data.indexOf(searchName) + dir_item.getProperty("time_or_year").length() + 1));
              if (name_data.equals(""))
                continue; 
              name_data = name_data.replaceAll("\r", "%0A").replaceAll("\n", "%0D");
              dir_item.put("name", name_data);
              if (data.toUpperCase().startsWith("L")) {
                dir_item.put("name", name_data.substring(0, name_data.indexOf(" ->")));
                dir_item.put("linkedFile", name_data.substring(name_data.indexOf(" ->") + 3).trim());
                if (!dir_item.getProperty("linkedFile").endsWith("/"))
                  dir_item.put("type", "FILE"); 
              } 
              dir_item.put("local", "true");
              if (!name_data.startsWith("Icon"))
                all_dir_items.put(Common.normalize2(name_data), dir_item); 
            } 
          } catch (Exception e) {
            if (ls_proc != null)
              ls_proc.destroy(); 
            Common.log("FILE_CLIENT", 1, e);
          } 
        } 
        try {
          if (ls_proc != null)
            ls_proc.destroy(); 
        } catch (Exception e) {
          Common.log("FILE_CLIENT", 5, e);
        } 
      } catch (Exception e) {
        Common.log("FILE_CLIENT", 1, e);
      } 
      try {
        if (ls_in != null)
          ls_in.close(); 
      } catch (Exception e) {
        Common.log("FILE_CLIENT", 5, e);
      } 
      try {
        if (monitorThread != null)
          monitorThread.interrupt(); 
      } catch (Exception e) {
        Common.log("FILE_CLIENT", 5, e);
      } 
      this.lsBytesRead = -1;
    } 
    this.dirCache.put(realPath, all_dir_items.clone());
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    InputStream fin = new FileInputStream(new File_U(path));
    try {
      if (startPos > 0L)
        fin.skip(startPos); 
    } catch (Exception e) {
      fin.close();
      throw e;
    } 
    if (endPos > 0L)
      fin = getLimitedInputStream(fin, startPos, endPos); 
    this.in = fin;
    return this.in;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    return (new File_U(path)).setLastModified(modified);
  }
  
  public boolean rename(String rnfr0, String rnto0) throws Exception {
    String rnfr = String.valueOf(this.url) + rnfr0.substring(1);
    String rnto = String.valueOf(this.url) + rnto0.substring(1);
    String cpath1 = null;
    if (memCache)
      cpath1 = (new File_U(rnfr)).getCanonicalPath().replace('\\', '/'); 
    boolean ok = !(new File_U(rnto)).exists();
    if (ok)
      ok = (new File_U(rnfr)).renameTo(new File_U(rnto)); 
    if (Common.machine_is_windows() && (new File_U(rnfr)).isDirectory() && (new File_U(rnfr)).exists() && (new File_U(rnto)).exists() && !(new File_U(rnfr)).equals(new File_U(rnto))) {
      String[] f_list = (new File_U(rnfr)).list();
      if (f_list == null || f_list.length == 0)
        if (!(new File_U(rnfr)).delete()) {
          Common.log("FILE_CLIENT", 1, "File " + rnfr + " is used by another process");
        } else {
          ok = true;
        }  
    } 
    if (memCache && ok) {
      String cpath2 = (new File_U(rnto)).getCanonicalPath().replace('\\', '/');
      dirCachePerm.remove(cpath1);
      Vector permCacheList1 = (Vector)dirCachePerm.get(Common.all_but_last(cpath1));
      if (permCacheList1 != null)
        permCacheList1.remove(cpath1); 
      stat(rnto0);
      Vector permCacheList2 = (Vector)dirCachePerm.get(Common.all_but_last(cpath2));
      if (permCacheList2 != null)
        permCacheList2.addElement(cpath2); 
    } 
    return ok;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    RandomOutputStream fout = new RandomOutputStream(new File_U(path), false);
    if (truncate && startPos > 0L) {
      fout.setLength(startPos);
    } else if (truncate && startPos <= 0L) {
      fout.setLength(0L);
    } 
    if (startPos > 0L)
      fout.seek(startPos); 
    this.out = fout;
    return fout;
  }
  
  public boolean delete(String path) {
    path = String.valueOf(this.url) + path.substring(1);
    if (memCache)
      try {
        String cpath = (new File_U(path)).getCanonicalPath().replace('\\', '/');
        dirCachePerm.remove(cpath);
        Vector permCacheList = (Vector)dirCachePerm.get(Common.all_but_last(cpath));
        if (permCacheList != null)
          permCacheList.remove(cpath); 
      } catch (Exception exception) {} 
    File_U f = new File_U(path);
    if (this.config.getProperty("file_recurse_delete", "false").equals("true") && f.isDirectory())
      return Common.recurseDelete_U(f.getPath(), false); 
    return f.delete();
  }
  
  public boolean makedir(String path0) {
    String path = String.valueOf(this.url) + path0.substring(1);
    boolean ok = !((!(new File_U(path)).exists() || !(new File_U(path)).isDirectory()) && !(new File_U(path)).mkdir());
    if (memCache && ok)
      try {
        String cpath = (new File_U(path)).getCanonicalPath().replace('\\', '/');
        stat(path0);
        Vector permCacheList = (Vector)dirCachePerm.get(Common.all_but_last(cpath));
        if (permCacheList == null) {
          permCacheList = new Vector();
          dirCachePerm.put(Common.all_but_last(cpath), permCacheList);
        } 
        if (permCacheList.indexOf(cpath) < 0)
          permCacheList.addElement(cpath); 
      } catch (Exception exception) {} 
    return ok;
  }
  
  public boolean makedirs(String path) throws Exception {
    path = String.valueOf(this.url) + path.substring(1);
    boolean ok = !(!(new File_U(path)).exists() && !(new File_U(path)).mkdirs());
    if (memCache && ok)
      makedir(path); 
    return ok;
  }
  
  public void setMod(String path, String val, String param) {
    path = path.replace(';', '_').replace('&', '_');
    val = val.replace(';', '_').replace('&', '_');
    param = param.replace(';', '_').replace('&', '_');
    doOSCommand(System.getProperty("crushftp.chmod", "chmod"), param, val, path);
  }
  
  public void setOwner(String path, String val, String param) {
    if (val == null || val.equals(""))
      return; 
    path = path.replace(';', '_').replace('&', '_');
    val = val.replace(';', '_').replace('&', '_');
    param = param.replace(';', '_').replace('&', '_');
    if (Common.machine_is_windows()) {
      doOSCommand("icacls.exe", path.replace('/', '\\'), "/setowner", val);
    } else {
      doOSCommand(System.getProperty("crushftp.chown", "chown"), param, val, path);
    } 
  }
  
  public void setGroup(String path, String val, String param) {
    path = path.replace(';', '_').replace('&', '_');
    val = val.replace(';', '_').replace('&', '_');
    param = param.replace(';', '_').replace('&', '_');
    doOSCommand(System.getProperty("crushftp.chgrp", "chgrp"), param, val, path);
  }
  
  public void doOSCommand(String app, String param, String val, String path) {
    Properties p = new Properties();
    p.put("app", app);
    p.put("param", param);
    p.put("val", val);
    p.put("path", path);
    CommandBufferFlusher.commandBuffer.addElement(p);
    CommandBufferFlusher.flushBuffer();
  }
  
  public String doCommand(String command) throws Exception {
    if (command.startsWith("SITE PGP_HEADER_SIZE")) {
      command = command.substring(command.indexOf(" ") + 1);
      command = command.substring(command.indexOf(" ") + 1);
      long size = Long.parseLong(command.substring(0, command.indexOf(" ")).trim());
      command = command.substring(command.indexOf(" ") + 1);
      if (command.startsWith("/"))
        command = command.substring(1); 
      String path = String.valueOf(this.url) + command.trim();
      RandomOutputStream fout = new RandomOutputStream(new File_U(path), false);
      int offset = "-----BEGIN PGP MESSAGE-----\r\nCRUSHFTP#".length() + 10;
      fout.seek(offset);
      fout.write((new StringBuffer(String.valueOf(size))).toString().getBytes("UTF8"));
      fout.close();
      return "214 OK";
    } 
    return "";
  }
  
  public String getLastMd5() {
    if (this.out != null && this.out instanceof RandomOutputStream)
      return ((RandomOutputStream)this.out).getLastMd5(); 
    return super.getLastMd5();
  }
}
