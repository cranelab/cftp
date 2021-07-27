package crushftp.server;

import com.crushftp.client.Common;
import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import com.maverick.sshd.SessionChannel;
import com.maverick.sshd.SftpFile;
import com.maverick.sshd.SftpFileAttributes;
import com.maverick.sshd.SshContext;
import com.maverick.sshd.platform.InvalidHandleException;
import com.maverick.sshd.platform.NativeFileSystemProvider;
import com.maverick.sshd.platform.PermissionDeniedException;
import com.maverick.sshd.platform.UnsupportedFileOperationException;
import com.maverick.util.UnsignedInteger32;
import com.maverick.util.UnsignedInteger64;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.IdlerKiller;
import crushftp.handlers.Log;
import crushftp.handlers.SessionCrush;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

public class ServerSessionSSH implements NativeFileSystemProvider {
  private Map openFiles = new HashMap();
  
  private Map dirList = new HashMap();
  
  private Map quickLookupDirItem = new HashMap();
  
  SessionCrush thisSession = null;
  
  Common common_code = new Common();
  
  public static Properties sessionLookup = new Properties();
  
  public static Properties connectionLookup = new Properties();
  
  public Object statLock = new Object();
  
  public IdlerKiller thread_killer_item = null;
  
  public void init(byte[] sessionid, SessionChannel session, SshContext context, String protocolInUse) throws IOException {
    this.thisSession = (SessionCrush)sessionLookup.get(sessionid);
    int minutes = this.thisSession.IG("max_idle_time");
    if (minutes == 0)
      minutes = 9999; 
    this.thread_killer_item = new IdlerKiller(this.thisSession, (new Date()).getTime(), minutes, null);
    Worker.startWorker(this.thread_killer_item);
    this.thisSession.add_log_formatted("CONNECT " + this.thisSession.uiSG("user_name"), "USER");
  }
  
  public void init(SessionChannel arg0, SshContext arg1) throws IOException {}
  
  public boolean makeDirectory(String path) throws PermissionDeniedException, FileNotFoundException, IOException {
    try {
      stop_idle_timer();
      start_idle_timer();
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } 
    delay();
    path = fixPath(path);
    this.thisSession.add_log_formatted("makeDirectory " + path, "MKD");
    Log.log("SSH_SERVER", 2, "SFTP:MakeDirectory:" + path);
    this.thisSession.uiPUT("the_command", "MKD");
    this.thisSession.uiPUT("the_command_data", path);
    this.thisSession.runPlugin("beforeCommand", null);
    String result = "error";
    try {
      result = this.thisSession.do_MKD(false, path);
      Properties p = new Properties();
      p.put("command_code", "0");
      p.put("command_data", result);
      this.thisSession.runPlugin("afterCommand", p);
      Log.log("SSH_SERVER", 2, "SFTP:MakeDirectory:" + result);
      if (result.equals("%MKD-exists%"))
        throw new IOException("%MKD-exists%"); 
      if (result.equals("%MKD-bad%"))
        throw new PermissionDeniedException("%MKD-bad%"); 
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    } 
    return true;
  }
  
  public SftpFileAttributes getFileAttributes(byte[] handle) throws IOException, InvalidHandleException {
    String path_handle = new String(handle, "UTF8");
    String path = path_handle.substring(0, path_handle.lastIndexOf(":"));
    path = fixPath(path);
    this.thisSession.add_log_formatted("getFileAttributes " + path, "MDTM");
    Log.log("SSH_SERVER", 2, "SFTP:getFileAttributes1:" + path);
    this.thisSession.uiPUT("the_command_data", path);
    this.thisSession.uiPUT("the_command", "FILE_ATTRIBUTE");
    this.thisSession.runPlugin("beforeCommand", null);
    SftpFileAttributes attrs = null;
    try {
      attrs = getFileAttributes(path_handle.substring(0, path_handle.lastIndexOf(":")));
      Properties p = new Properties();
      p.put("command_code", "0");
      p.put("command_data", attrs);
      this.thisSession.runPlugin("afterCommand", p);
    } catch (FileNotFoundException e) {
      throw new IOException(e.getMessage());
    } 
    return attrs;
  }
  
  public SftpFileAttributes getFileAttributes(String path) throws IOException, FileNotFoundException {
    path = fixPath(path);
    this.thisSession.add_log_formatted("getFileAttributes " + path, "MDTM");
    Log.log("SSH_SERVER", 2, "SFTP:getFileAttributes2:" + path);
    this.thisSession.uiPUT("the_command_data", path);
    this.thisSession.uiPUT("the_command", "FILE_ATTRIBUTE");
    this.thisSession.runPlugin("beforeCommand", null);
    Properties item = null;
    item = (Properties)this.quickLookupDirItem.get(path);
    try {
      synchronized (this.statLock) {
        if (item == null)
          item = this.thisSession.uVFS.get_item(path); 
      } 
    } catch (Exception e) {
      e.printStackTrace();
    } 
    if (ServerStatus.IG("log_debug_level") >= 2)
      Log.log("SSH_SERVER", 2, "SFTP:getFileAttributes2:" + VRL.safe(item)); 
    if (path.indexOf(":filetree") >= 0 && ServerStatus.BG("allow_filetree")) {
      SftpFileAttributes attrs = new SftpFileAttributes();
      return attrs;
    } 
    if (item == null)
      throw new FileNotFoundException(LOC.G("File not found3:" + path)); 
    SftpFileAttributes sfa = getFileAttributes(item);
    Properties p = new Properties();
    p.put("command_code", "0");
    p.put("command_data", sfa);
    this.thisSession.runPlugin("afterCommand", p);
    return sfa;
  }
  
  public SftpFileAttributes getFileAttributes(Properties item) {
    SftpFileAttributes attrs = new SftpFileAttributes();
    attrs.setPermissions(item.getProperty("permissions", "-----------").substring(1, 10));
    attrs.setPermissions(new UnsignedInteger32(attrs.getPermissions().longValue() | (item.getProperty("type").equals("DIR") ? 16384L : 32768L)));
    if (Long.parseLong(item.getProperty("size", "0")) < 0L)
      item.put("size", "0"); 
    attrs.setSize(new UnsignedInteger64(item.getProperty("size", "0")));
    long lastMod = Long.parseLong(item.getProperty("modified", (new StringBuffer(String.valueOf(System.currentTimeMillis()))).toString()));
    if (lastMod < 0L)
      lastMod = (new Date()).getTime(); 
    attrs.setTimes(new UnsignedInteger32(lastMod / 1000L), new UnsignedInteger32(lastMod / 1000L));
    return attrs;
  }
  
  public byte[] openDirectory(String path) throws PermissionDeniedException, FileNotFoundException, IOException {
    Properties p;
    this.quickLookupDirItem.clear();
    delay();
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    path = fixPath(path);
    this.thisSession.add_log_formatted("CWD " + path, "CWD");
    Log.log("SSH_SERVER", 2, "SFTP:openDirectory:" + path);
    if (!path.startsWith("/"))
      path = "/" + path; 
    this.thisSession.uiPUT("the_command_data", path);
    this.thisSession.uiPUT("the_command", "CWD");
    this.thisSession.runPlugin("beforeCommand", null);
    Properties item = null;
    try {
      item = this.thisSession.uVFS.get_fake_item(path, "DIR");
    } catch (Exception e) {
      Log.log("SSH_SERVER", 2, e);
    } 
    if (ServerStatus.IG("log_debug_level") >= 2)
      Log.log("SSH_SERVER", 2, "SFTP:openDirectory:" + VRL.safe(item)); 
    if (item == null)
      throw new FileNotFoundException(LOC.G("File not found1:" + path)); 
    if (item.getProperty("type").equals("FILE")) {
      this.thisSession.add_log_formatted("550 CWD " + path + ":" + LOC.G("Item is a file!"), "CWD");
      throw new IOException(LOC.G("Item is a file!"));
    } 
    String result = "";
    try {
      try {
        result = this.thisSession.do_CWD();
      } catch (Exception e) {
        this.thisSession.add_log_formatted("550 CWD " + path + ":" + e.getMessage(), "CWD");
        throw new IOException(e.getMessage());
      } 
      Log.log("SSH_SERVER", 2, "SFTP:openDirectory:" + result);
      if (result.equals("%CWD-bad%")) {
        this.thisSession.add_log_formatted("550 CWD " + path + ":%CWD-bad%", "CWD");
        throw new PermissionDeniedException("%CWD-bad%");
      } 
      if (result.equals("%CWD-not found%")) {
        this.thisSession.add_log_formatted("550 CWD " + path + ":%CWD-not found%", "CWD");
        throw new FileNotFoundException("%CWD-not found%");
      } 
      this.dirList.put(String.valueOf(path) + "_status", "READY");
      this.dirList.remove(String.valueOf(path) + "_list");
      this.dirList.remove(String.valueOf(path) + "_hash");
    } finally {
      Properties properties = new Properties();
      properties.put("command_code", "0");
      properties.put("command_data", (new StringBuffer(String.valueOf(result))).toString());
      this.thisSession.runPlugin("afterCommand", properties);
    } 
    return path.getBytes("UTF8");
  }
  
  public SftpFile[] readDirectory(byte[] handle) throws InvalidHandleException, EOFException {
    try {
      stop_idle_timer();
      start_idle_timer();
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } 
    String path = new String(handle);
    try {
      path = new String(handle, "UTF8");
    } catch (Exception exception) {}
    path = fixPath(path);
    Log.log("SSH_SERVER", 2, "SFTP:readDirectory:" + path);
    Vector files = new Vector();
    if (this.dirList.get(String.valueOf(path) + "_status").toString().equals("READY")) {
      Vector list = new Vector();
      this.dirList.put(String.valueOf(path) + "_status", "LISTING");
      this.dirList.put(String.valueOf(path) + "_list", list);
      this.dirList.put(String.valueOf(path) + "_hash", new Properties());
      this.thisSession.add_log_formatted("150 LIST " + path, "LIST");
      this.thisSession.uiPUT("the_command_data", path);
      this.thisSession.uiPUT("the_command", "LIST");
      this.thisSession.runPlugin("beforeCommand", null);
      if (ServerStatus.BG("send_dot_dot_list_sftp")) {
        Properties dir_item = new Properties();
        dir_item.put("name", ".");
        dir_item.put("type", "DIR");
        dir_item.put("permissions", "drwxrwxrwx");
        dir_item.put("size", "0");
        dir_item.put("url", ".");
        dir_item.put("root_dir", ".");
        dir_item.put("sftp_path", ".");
        dir_item.put("link", "false");
        dir_item.put("num_items", "1");
        dir_item.put("owner", "user");
        dir_item.put("group", "group");
        dir_item.put("protocol", "file");
        list.addElement(dir_item);
        dir_item = new Properties();
        dir_item.put("name", "..");
        dir_item.put("type", "DIR");
        dir_item.put("permissions", "drwxrwxrwx");
        dir_item.put("size", "0");
        dir_item.put("url", ".");
        dir_item.put("root_dir", ".");
        dir_item.put("sftp_path", "..");
        dir_item.put("link", "false");
        dir_item.put("num_items", "1");
        dir_item.put("owner", "user");
        dir_item.put("group", "group");
        dir_item.put("protocol", "file");
        list.addElement(dir_item);
      } 
      Properties folder_item = new Properties();
      try {
        folder_item = this.thisSession.uVFS.get_item(path);
        if (folder_item.getProperty("privs", "").toUpperCase().indexOf("(VIEW)") >= 0) {
          String path2 = path;
          Runnable r = new Runnable(this, list, path2) {
              final ServerSessionSSH this$0;
              
              private final Vector val$list;
              
              private final String val$path2;
              
              public void run() {
                try {
                  this.this$0.thisSession.uVFS.getListing(this.val$list, this.val$path2);
                  this.this$0.dirList.put(String.valueOf(this.val$path2) + "_status", "LISTING_DONE");
                } catch (Exception e) {
                  Log.log("LIST", 0, e);
                  this.this$0.dirList.put(String.valueOf(this.val$path2) + "_status", "ERROR:" + e.getMessage());
                } 
              }
            };
          if (ServerStatus.BG("listing_multithreaded")) {
            Worker.startWorker(r, String.valueOf(Thread.currentThread().getName()) + ":Listing..." + path2);
          } else {
            r.run();
            Properties ppp = new Properties();
            ppp.put("listing", list);
            this.thisSession.runPlugin("list", ppp);
          } 
        } else {
          this.dirList.put(String.valueOf(path) + "_status", "LISTING_DONE");
        } 
      } catch (Exception e) {
        Log.log("SSH_SERVER", 1, e);
      } 
      if (Log.log("SSH_SERVER", 3, ""))
        Log.log("SSH_SERVER", 3, "SFTP:readDirectory:" + list); 
      files = getListFiles(list, path, (Properties)this.dirList.get(String.valueOf(path) + "_hash"));
    } else if (this.dirList.get(String.valueOf(path) + "_status").toString().equals("LISTING") || this.dirList.get(String.valueOf(path) + "_status").toString().equals("LISTING_DONE")) {
      files = getListFiles((Vector)this.dirList.get(String.valueOf(path) + "_list"), path, (Properties)this.dirList.get(String.valueOf(path) + "_hash"));
    } else {
      if (this.dirList.get(String.valueOf(path) + "_status").toString().equals("DONE")) {
        this.thisSession.add_log_formatted("226 LIST " + path, "LIST");
        throw new EOFException(LOC.G("There are no more files"));
      } 
      if (this.dirList.get(String.valueOf(path) + "_status").toString().startsWith("ERROR:")) {
        this.dirList.remove(String.valueOf(path) + "_list");
        this.thisSession.add_log_formatted("550 ERROR " + path + ":" + this.dirList.get(String.valueOf(path) + "_status").toString(), "LIST");
        throw new InvalidHandleException("ERROR:" + this.dirList.get(String.valueOf(path) + "_status").toString());
      } 
    } 
    SftpFile[] sftpfiles = new SftpFile[files.size()];
    for (int i = 0; i < files.size(); i++) {
      Properties properties = files.elementAt(i);
      sftpfiles[i] = new SftpFile(properties.getProperty("name"), getFileAttributes(properties));
    } 
    Properties p = new Properties();
    p.put("command_code", "0");
    p.put("command_data", (new StringBuffer(String.valueOf(path))).toString());
    this.thisSession.runPlugin("afterCommand", p);
    return sftpfiles;
  }
  
  public Vector getListFiles(Vector list, String path, Properties name_hash) {
    Vector files = new Vector();
    while (list.size() > 0 || this.dirList.get(String.valueOf(path) + "_status").toString().equals("LISTING")) {
      if (list.size() == 0) {
        try {
          Thread.sleep(10L);
        } catch (InterruptedException interruptedException) {}
        continue;
      } 
      Properties p = list.remove(0);
      if (name_hash.containsKey(p.getProperty("name")))
        continue; 
      name_hash.put(p.getProperty("name"), "");
      if (!p.getProperty("name").equals(".") && !p.getProperty("name").equals(".."))
        if (p.getProperty("privs").toLowerCase().indexOf("(invisible)") < 0) {
          if (!Common.filter_check("L", p.getProperty("name"), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSession.SG("file_filter")))
            continue; 
        } else {
          continue;
        }  
      String sftp_path = String.valueOf(p.getProperty("root_dir")) + p.getProperty("name");
      if (sftp_path.startsWith(this.thisSession.SG("root_dir")))
        sftp_path = sftp_path.substring(this.thisSession.SG("root_dir").length() - 1); 
      p.put("sftp_path", sftp_path);
      if (this.quickLookupDirItem.size() < 10000 && !p.getProperty("url").toLowerCase().startsWith("file:/"))
        this.quickLookupDirItem.put(fixPath(sftp_path), p); 
      files.addElement(p);
      this.thisSession.add_log_formatted(sftp_path, "DIR_LIST");
      if (files.size() >= 100)
        break; 
    } 
    if (list.size() == 0 && this.dirList.get(String.valueOf(path) + "_status").toString().equals("LISTING_DONE")) {
      this.dirList.put(String.valueOf(path) + "_status", "DONE");
      this.dirList.remove(String.valueOf(path) + "_list");
      this.dirList.remove(String.valueOf(path) + "_hash");
    } 
    return files;
  }
  
  public byte[] openFile(String path, UnsignedInteger32 flags, SftpFileAttributes attrs) throws PermissionDeniedException, FileNotFoundException, IOException {
    synchronized (this.statLock) {
      delay();
      path = fixPath(path);
      Log.log("SSH_SERVER", 2, "SFTP:openFile:" + path);
      Properties item = null;
      try {
        Log.log("SSH_SERVER", 2, "openFile:" + flags.intValue());
        if ((flags.intValue() & 0x8) == 8 || (flags.intValue() & 0x2) == 2 || (flags.intValue() & 0x10) == 16) {
          this.thisSession.uiPUT("the_command_data", path);
          this.thisSession.uiPUT("the_command", "STOR_INIT");
          this.thisSession.add_log_formatted("STOR START " + path, "STOR");
          Properties properties = new Properties();
          properties.put("actual_path", path);
          properties.put("message_string", "");
          this.thisSession.runPlugin("beforeCommand", properties);
          if (!properties.getProperty("message_string", "").equals(""))
            throw new IOException(properties.getProperty("message_string")); 
          path = properties.getProperty("actual_path");
          this.thisSession.uiPUT("the_command", "STOR");
          try {
            item = this.thisSession.uVFS.get_item(path);
          } catch (Exception exception) {}
          if (ServerStatus.IG("log_debug_level") >= 2)
            Log.log("SSH_SERVER", 2, "SFTP:openFile1:" + VRL.safe(item)); 
          if (!this.thisSession.check_access_privs(Common.all_but_last(path), "STOR") || !Common.filter_check("U", Common.last(path), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSession.SG("file_filter")))
            throw new PermissionDeniedException(properties.getProperty("message_string", LOC.G("STOR Denied for " + path + "!"))); 
          boolean fileExists = true;
          if ((flags.intValue() & 0x8) == 8 || (flags.intValue() & 0x10) == 16) {
            if (item != null) {
              GenericClient c = this.thisSession.uVFS.getClient(item);
              try {
                VRL vrl = new VRL(item.getProperty("url"));
                Properties stat = c.stat(vrl.getPath());
                if (item.getProperty("privs").indexOf("(view)") < 0 && stat != null)
                  try {
                    int fileNameInt = 1;
                    String itemName = item.getProperty("url");
                    String itemExt = "";
                    if (itemName.lastIndexOf(".") > 0 && (itemName.lastIndexOf(".") == itemName.length() - 4 || itemName.lastIndexOf(".") == itemName.length() - 5)) {
                      itemExt = itemName.substring(itemName.lastIndexOf("."));
                      itemName = itemName.substring(0, itemName.lastIndexOf("."));
                    } 
                    while (c.stat(String.valueOf(Common.all_but_last(vrl.getPath())) + itemName + fileNameInt + itemExt) != null)
                      fileNameInt++; 
                    c.rename(vrl.getPath(), String.valueOf(Common.all_but_last(vrl.getPath())) + itemName + fileNameInt + itemExt);
                    vrl = new VRL(String.valueOf(Common.all_but_last(vrl.toString())) + itemName + fileNameInt + itemExt);
                  } catch (Exception e) {
                    if (e.indexOf("Interrupted") >= 0)
                      throw e; 
                  }  
                if (c.stat(vrl.getPath()) != null && !this.thisSession.check_access_privs(path, "DELE"))
                  throw new PermissionDeniedException(properties.getProperty("message_string", LOC.G("STOR Denied for " + path + "!"))); 
              } finally {
                c = this.thisSession.uVFS.releaseClient(c);
              } 
              if (ServerStatus.IG("log_debug_level") >= 2)
                Log.log("SSH_SERVER", 2, "SFTP:openFile:setZeroLength:" + VRL.safe(item)); 
            } else {
              fileExists = false;
              try {
                item = this.thisSession.uVFS.get_item_parent(path);
                item.put("type", "FILE");
              } catch (Exception exception) {}
            } 
            if (ServerStatus.BG("allow_ssh_0_byte_file")) {
              if (ServerStatus.IG("log_debug_level") >= 2)
                Log.log("SSH_SERVER", 2, "SFTP:openFile:zero:" + VRL.safe(item)); 
              if (item != null && item.getProperty("privs", "").indexOf("(sync") < 0 && !path.endsWith("zipstream"))
                if (fileExists || (!fileExists && (flags.intValue() & 0x8) == 8))
                  item.put("zero_wanted", "true");  
            } 
          } 
          if ((flags.intValue() & 0x8) == 8 && ServerStatus.BG("allow_ssh_0_byte_file")) {
            try {
              String former_type = "FILE";
              if (fileExists)
                former_type = item.getProperty("type"); 
              item = this.thisSession.uVFS.get_item_parent(path);
              if (item != null)
                item.put("type", former_type); 
            } catch (Exception exception) {}
            if (item != null && item.getProperty("privs", "").indexOf("(sync") < 0 && !path.endsWith("zipstream")) {
              item.put("zero_wanted", "true");
              item.put("size", "0");
              if (!fileExists)
                item.put("type", "FILE"); 
              this.quickLookupDirItem.put(path, item);
            } 
          } 
          if (item == null) {
            item = this.thisSession.uVFS.get_item_parent(path);
            item.put("type", "FILE");
          } 
          item.put("allow_write", "true");
        } else {
          String temp_path = path;
          if (path.indexOf(":filetree") >= 0 && ServerStatus.BG("allow_filetree"))
            temp_path = path.substring(0, path.indexOf(":filetree")); 
          this.thisSession.uiPUT("the_command_data", path);
          this.thisSession.uiPUT("the_command", "RETR_INIT");
          Properties properties = new Properties();
          properties.put("actual_path", temp_path);
          this.thisSession.runPlugin("beforeCommand", properties);
          temp_path = properties.getProperty("actual_path");
          try {
            item = this.thisSession.uVFS.get_item(temp_path);
          } catch (Exception exception) {}
          if (ServerStatus.IG("log_debug_level") >= 2)
            Log.log("SSH_SERVER", 2, "SFTP:openFile2:" + VRL.safe(item)); 
          if (item == null)
            throw new FileNotFoundException("File not found2:" + path); 
          this.thisSession.uiPUT("the_command", "LIST");
          if (!this.thisSession.check_access_privs(path, "RETR", item) || !Common.filter_check("D", Common.last(path), String.valueOf(ServerStatus.SG("filename_filters_str")) + "\r\n" + this.thisSession.SG("file_filter")))
            throw new PermissionDeniedException(properties.getProperty("message_string", "Denied!")); 
          this.thisSession.add_log_formatted("RETR START " + path, "RETR");
        } 
        path = String.valueOf(path) + ":" + Common.makeBoundary(5);
        this.openFiles.put(path, item);
        if (ServerStatus.IG("log_debug_level") >= 2)
          Log.log("SSH_SERVER", 2, "SFTP:openedFile:" + path + ":" + VRL.safe(item)); 
      } catch (Exception e) {
        Log.log("SSH_SERVER", 1, e);
        this.thisSession.add_log_formatted("550 openFile error:" + e.getMessage(), "RETR");
        Properties properties = new Properties();
        properties.put("command_code", "0");
        properties.put("command_data", (new StringBuffer(String.valueOf(e.getMessage()))).toString());
        this.thisSession.runPlugin("afterCommand", properties);
        throw new IOException(e.getMessage());
      } 
      Log.log("SSH_SERVER", 2, "SFTP:openFile:reply:" + path);
      Properties p = new Properties();
      p.put("command_code", "0");
      p.put("command_data", (new StringBuffer(String.valueOf(path))).toString());
      this.thisSession.runPlugin("afterCommand", p);
      return path.getBytes("UTF8");
    } 
  }
  
  public void writeFile(byte[] handle, UnsignedInteger64 offset, byte[] data, int off, int len) throws InvalidHandleException, IOException {
    synchronized (this.statLock) {
      try {
        stop_idle_timer();
        start_idle_timer();
      } catch (Exception e) {
        Log.log("SSH_SERVER", 1, e);
      } 
      String path_handle = new String(handle, "UTF8");
      String path = path_handle.substring(0, path_handle.lastIndexOf(":"));
      path = fixPath(path);
      if (ServerStatus.IG("log_debug_level") >= 2)
        Log.log("SSH_SERVER", 2, "SFTP:writeFile:" + path_handle); 
      Properties item = (Properties)this.openFiles.get(path_handle);
      OutputStream outStream = (OutputStream)item.get("outputstream");
      STOR_handler stor_files = (STOR_handler)item.get("stor_files");
      if (ServerStatus.IG("log_debug_level") >= 2)
        Log.log("SSH_SERVER", 2, "SFTP:writeFile:" + VRL.safe(item)); 
      boolean buffered = false;
      try {
        if (item.getProperty("allow_write", "false").equals("true"))
          if (outStream == null) {
            delay();
            if (item.getProperty("type", "").equalsIgnoreCase("DIR") && !item.getProperty("name").toUpperCase().endsWith(".ZIPSTREAM"))
              throw new IOException("File cannot replace a directory."); 
            if (item.getProperty("socket_closed", "false").equals("true"))
              throw new IOException("Socket closed already."); 
            this.thisSession.uiPUT("the_command_data", path);
            this.thisSession.uiPUT("the_command", "beforeUpload_SSH");
            this.thisSession.add_log_formatted("STOR START " + path_handle, "STOR");
            Properties p = new Properties();
            p.put("actual_path", path);
            p.put("message_string", "");
            this.thisSession.runPlugin("beforeCommand", p);
            if (!p.getProperty("message_string", "").equals(""))
              throw new IOException(p.getProperty("message_string")); 
            path = p.getProperty("actual_path");
            this.thisSession.uiPUT("the_command", "STOR");
            this.thisSession.add_log_formatted("STOR " + path_handle, "STOR");
            getOutputStream(offset.longValue(), path, item);
            item.put("buffer", new Properties());
            outStream = (OutputStream)item.get("outputstream");
            item.put("size", (new StringBuffer(String.valueOf(offset.longValue()))).toString());
            p = new Properties();
            p.put("command_code", "0");
            p.put("command_data", (new StringBuffer(String.valueOf(path))).toString());
            this.thisSession.runPlugin("afterCommand", p);
          } else if (Long.parseLong(item.getProperty("size", "0")) != offset.longValue()) {
            if (ServerStatus.BG("sftp_buffered_write")) {
              if (item.getProperty("allow_write", "false").equals("true")) {
                Properties buffer = (Properties)item.get("buffer");
                Log.log("SSH_SERVER", 2, "Offset out of sequence during STOR of file:" + path_handle + " : " + item.getProperty("size", "0") + " vs." + (offset.longValue() - off) + ":buffer count=" + buffer.size());
                if (buffer.size() > 200)
                  throw new IOException("Streaming out of sequence buffer full:" + buffer.size()); 
                byte[] b = new byte[len];
                System.arraycopy(data, off, b, 0, len);
                buffer.put((new StringBuffer(String.valueOf(offset.longValue()))).toString(), b);
                buffered = true;
                item.put("zero_wanted", "false");
              } 
            } else {
              Log.log("SSH_SERVER", 2, "Offset jump during STOR of file:" + path_handle + " : " + item.getProperty("size", "0") + " vs." + (offset.longValue() - off));
              item.remove("stor_files");
              item.remove("outputstream");
              outStream.close();
              while (stor_files.active2.getProperty("active", "").equals("true"))
                Thread.sleep(1L); 
              try {
                stor_files.c.close();
              } catch (Exception exception) {}
              if (stor_files.quota_exceeded)
                throw new IOException("Quota Exceeded."); 
              if (item.getProperty("socket_closed", "false").equals("true"))
                throw new IOException("Socket closed already."); 
              getOutputStream(offset.longValue(), path, item);
              outStream = (OutputStream)item.get("outputstream");
              item.put("size", (new StringBuffer(String.valueOf(offset.longValue()))).toString());
            } 
          }  
      } catch (Exception e) {
        Log.log("SSH_SERVER", 1, e);
        this.thisSession.add_log_formatted("550 STOR error:" + e.getMessage(), "STOR");
        throw new IOException(e.getMessage());
      } 
      try {
        if (item.getProperty("allow_write", "false").equals("true") && !buffered)
          try {
            outStream.write(data, off, len);
            outStream.flush();
            item.put("zero_wanted", "false");
            if (offset.longValue() + len > Long.parseLong(item.getProperty("size")))
              item.put("size", (new StringBuffer(String.valueOf(offset.longValue() + len))).toString()); 
            if (ServerStatus.BG("sftp_buffered_write")) {
              long pos = Long.parseLong(item.getProperty("size"));
              Properties buffer = (Properties)item.get("buffer");
              while (buffer.containsKey((new StringBuffer(String.valueOf(pos))).toString())) {
                byte[] b = (byte[])buffer.remove((new StringBuffer(String.valueOf(pos))).toString());
                outStream.write(b);
                outStream.flush();
                pos += b.length;
                item.put("size", (new StringBuffer(String.valueOf(pos))).toString());
              } 
            } 
          } catch (SocketException e) {
            if (item.getProperty("socket_closed", "false").equals("true"))
              throw e; 
            if (stor_files != null && (stor_files.data_sock == null || stor_files.data_sock.isClosed())) {
              Log.log("SSH_SERVER", 0, "Tried to write extra bytes after socket closed:" + len + new String(data, off, len));
              item.put("socket_closed", "true");
            } else {
              throw e;
            } 
          }  
      } catch (IOException e) {
        Log.log("SSH_SERVER", 0, "Exception on write1:" + Thread.currentThread().getName() + ":" + len);
        Log.log("SSH_SERVER", 0, e);
        this.thisSession.add_log_formatted("550 STOR error:" + e.getMessage(), "STOR");
        throw e;
      } 
    } 
  }
  
  public int readFile(byte[] handle, UnsignedInteger64 offset, byte[] buf, int off, int numBytesToRead) throws InvalidHandleException, EOFException, IOException {
    try {
      stop_idle_timer();
      start_idle_timer();
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } 
    String path_handle = new String(handle, "UTF8");
    String path = path_handle.substring(0, path_handle.lastIndexOf(":"));
    path = fixPath(path);
    if (ServerStatus.IG("log_debug_level") >= 3)
      Log.log("SSH_SERVER", 3, "SFTP:readFile:" + path_handle); 
    Properties item = (Properties)this.openFiles.get(path_handle);
    InputStream inStream = (InputStream)item.get("inputstream");
    RETR_handler retr_files = (RETR_handler)item.get("retr_files");
    if (ServerStatus.IG("log_debug_level") >= 3)
      Log.log("SSH_SERVER", 3, "SFTP:readFile:" + VRL.safe(item)); 
    boolean bufferedRead = false;
    try {
      if (inStream == null) {
        delay();
        this.thisSession.add_log_formatted("RETR " + path_handle, "RETR");
        Log.log("SSH_SERVER", 1, "SFTP:readFile3:" + path_handle + ":offset=" + offset.longValue() + ":off=" + off + ":numbytes=" + numBytesToRead + ":" + VRL.safe(item));
        getInputStream(offset.longValue(), path, item);
        inStream = (InputStream)item.get("inputstream");
        item.put("loc", (new StringBuffer(String.valueOf(offset.longValue()))).toString());
      } else {
        long loc = Long.parseLong(item.getProperty("loc", "0"));
        Properties alreadyRead = (Properties)item.get("alreadyRead");
        while (alreadyRead.containsKey((new StringBuffer(String.valueOf(loc))).toString())) {
          loc += Long.parseLong(alreadyRead.getProperty((new StringBuffer(String.valueOf(loc))).toString()));
          item.put("loc", (new StringBuffer(String.valueOf(loc))).toString());
        } 
        boolean restartRETR = false;
        if (loc < offset.longValue()) {
          long bytesToSkip = offset.longValue() - loc;
          if (bytesToSkip > 5242880L) {
            restartRETR = true;
          } else {
            bufferedRead = true;
            inStream.mark(5242880);
            while (bytesToSkip > 0L) {
              long bytes = inStream.skip(bytesToSkip);
              if (bytes <= 0L) {
                bytesToSkip = 0L;
                continue;
              } 
              if (bytes >= 0L)
                bytesToSkip -= bytes; 
            } 
          } 
        } 
        if (loc > offset.longValue() || restartRETR) {
          Log.log("SSH_SERVER", 1, "SFTP:readFile4:" + path_handle + ": changing location in stream...offset:" + offset.longValue() + "  vs   lastPos:" + loc + ": openFiles size:" + this.openFiles.size());
          if (offset.longValue() < Long.parseLong(item.getProperty("size", "0"))) {
            item.remove("retr_files");
            item.remove("inputstream");
            inStream.close();
            while (retr_files.active2.getProperty("active", "").equals("true"))
              Thread.sleep(1L); 
            try {
              retr_files.c.close();
            } catch (Exception exception) {}
            getInputStream(offset.longValue(), path, item);
            inStream = (InputStream)item.get("inputstream");
            item.put("loc", (new StringBuffer(String.valueOf(offset.longValue()))).toString());
          } 
        } 
      } 
    } catch (EOFException e) {
      throw e;
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
      if (e.toString().indexOf("EOF") >= 0)
        throw new EOFException(e.toString()); 
      this.thisSession.add_log_formatted("550 RETR error:" + e.getMessage(), "RETR");
      throw new IOException(e.getMessage());
    } 
    int totalBytesRead = 0;
    int read = 0;
    while (read >= 0 && totalBytesRead < numBytesToRead) {
      read = inStream.read(buf, off, numBytesToRead - totalBytesRead);
      if (read >= 0) {
        totalBytesRead += read;
        off += read;
      } 
    } 
    if (totalBytesRead >= 0 && !bufferedRead)
      item.put("loc", (new StringBuffer(String.valueOf(offset.longValue() + totalBytesRead))).toString()); 
    if (bufferedRead) {
      Properties alreadyRead = (Properties)item.get("alreadyRead");
      alreadyRead.put((new StringBuffer(String.valueOf(offset.longValue()))).toString(), (new StringBuffer(String.valueOf(totalBytesRead))).toString());
      inStream.reset();
    } 
    if (totalBytesRead == 0 && read == -1)
      throw new EOFException(LOC.G("The file is EOF")); 
    return totalBytesRead;
  }
  
  public void closeFile(byte[] handle) throws InvalidHandleException, IOException {
    String path_handle = new String(handle, "UTF8");
    String path = path_handle;
    if (path_handle.indexOf(":") > 0)
      path = path_handle.substring(0, path_handle.lastIndexOf(":")); 
    delay();
    path = fixPath(path);
    synchronized (this.statLock) {
      Log.log("SSH_SERVER", 2, "SFTP:closeFile:" + path_handle);
      Properties item = (Properties)this.openFiles.get(path_handle);
      if (item != null) {
        if (item.get("inputstream") == null && item.get("outputstream") == null && ServerStatus.BG("event_empty_files") && item.getProperty("allow_write", "false").equals("true")) {
          String sshRand = this.thisSession.uiSG("randomaccess");
          this.thisSession.uiPUT("randomaccess", "true");
          writeFile(handle, new UnsignedInteger64(String.valueOf(0)), new byte[0], 0, 0);
          this.thisSession.uiPUT("randomaccess", sshRand);
          try {
            Thread.sleep(200L);
          } catch (Exception exception) {}
        } 
        item.put("socket_closed", "true");
        this.openFiles.remove(path_handle);
        STOR_handler stor_files = null;
        RETR_handler retr_files = null;
        if (item.get("outputstream") != null) {
          Properties buffer = (Properties)item.remove("buffer");
          if (buffer.size() > 0) {
            String missing_parts = "";
            Enumeration keys = buffer.keys();
            while (keys.hasMoreElements()) {
              String part = (String)keys.nextElement();
              missing_parts = String.valueOf(missing_parts) + part + ",";
              buffer.remove(part);
              stor_files = (STOR_handler)item.get("stor_files");
              stor_files.inError = true;
              stor_files.stop_message = "Missing file segements in upload:" + missing_parts;
              this.thisSession.add_log_formatted("STOR END   " + path_handle + ":ABORTED!", "STOR");
              stor_files.thisThread.interrupt();
            } 
          } 
          try {
            ((OutputStream)item.get("outputstream")).close();
            this.thisSession.add_log_formatted("STOR END   " + path_handle, "STOR");
          } catch (Exception exception) {}
          stor_files = (STOR_handler)item.remove("stor_files");
          try {
            Socket sock_tmp = stor_files.data_sock;
            int i = 1;
            long start_time = System.currentTimeMillis();
            while (sock_tmp != null && System.currentTimeMillis() - start_time < 300000L && (!sock_tmp.isClosed() || !stor_files.active2.getProperty("streamOpenStatus", "").equals("CLOSED")))
              Thread.sleep(i++); 
          } catch (Exception exception) {}
          if (stor_files != null && stor_files.stop_message != null && !stor_files.stop_message.equals("") && stor_files.inError) {
            String msg = stor_files.stop_message;
            stor_files.stop_message = "";
            stor_files.freeStor();
            if (ServerStatus.BG("ssh_runtime_exception"))
              throw new RuntimeException(msg); 
            this.thisSession.add_log_formatted("550 STOR close error:" + msg, "STOR");
            throw new IOException(msg);
          } 
          stor_files.freeStor();
        } 
        if (item.get("inputstream") != null) {
          try {
            ((InputStream)item.get("inputstream")).close();
            this.thisSession.add_log_formatted("RETR END   " + path_handle, "RETR");
          } catch (Exception exception) {}
          retr_files = (RETR_handler)item.remove("retr_files");
          try {
            int i = 1;
            long start_time = System.currentTimeMillis();
            while (retr_files.active2.getProperty("active", "").equals("true") && System.currentTimeMillis() - start_time < 300000L)
              Thread.sleep(i++); 
          } catch (Exception exception) {}
          if (retr_files != null && retr_files.stop_message != null && !retr_files.stop_message.equals("") && retr_files.inError) {
            String msg = retr_files.stop_message;
            retr_files.stop_message = "";
            if (ServerStatus.BG("ssh_runtime_exception"))
              throw new RuntimeException(msg); 
            this.thisSession.add_log_formatted("550 RETR close error:" + msg, "RETR");
            throw new IOException(msg);
          } 
        } 
        if (item.getProperty("zero_wanted", "false").equals("true")) {
          GenericClient c = null;
          try {
            c = this.thisSession.uVFS.getClient(item);
            c.upload_0_byte((new VRL(item.getProperty("url"))).getPath());
          } catch (Exception e) {
            this.thisSession.add_log_formatted("550 RETR close error:" + e.getMessage(), "RETR");
            Log.log("SSH_SERVER", 1, e);
          } finally {
            try {
              c = this.thisSession.uVFS.releaseClient(c);
            } catch (Exception e) {
              Log.log("SSH_SERVER", 1, e);
            } 
          } 
        } 
      } else {
        this.dirList.remove(String.valueOf(path) + "_status");
        this.dirList.remove(String.valueOf(path) + "_list");
        this.dirList.remove(String.valueOf(path) + "_hash");
      } 
    } 
  }
  
  public void removeFile(String path) throws PermissionDeniedException, IOException, FileNotFoundException {
    Properties p;
    try {
      stop_idle_timer();
      start_idle_timer();
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } 
    delay();
    path = fixPath(path);
    Log.log("SSH_SERVER", 2, "SFTP:removeFile:" + path);
    this.thisSession.uiPUT("the_command_data", path);
    this.thisSession.uiPUT("the_command", "DELE");
    this.thisSession.runPlugin("beforeCommand", null);
    String result = "";
    try {
      try {
        result = this.thisSession.do_DELE(false, path);
        Log.log("SSH_SERVER", 2, "SFTP:removeFile:" + result);
      } catch (Exception e) {
        this.thisSession.add_log_formatted("Delete failed" + e.getMessage(), "DELE");
        throw new IOException(e.getMessage());
      } 
      if (result.equals("%DELE-error%")) {
        this.thisSession.add_log_formatted("550 DELE error:%DELE-error%", "DELE");
        throw new IOException("%DELE-error%");
      } 
      if (result.equals("%DELE-not found%")) {
        this.thisSession.add_log_formatted("550 DELE error:%DELE-not found%", "DELE");
        throw new FileNotFoundException("%DELE-not found%");
      } 
      if (result.equals("%DELE-bad%")) {
        this.thisSession.add_log_formatted("550 DELE error:%DELE-bad%", "DELE");
        throw new PermissionDeniedException("%DELE-bad%");
      } 
    } finally {
      Properties properties = new Properties();
      properties.put("command_code", "0");
      properties.put("command_data", (new StringBuffer(String.valueOf(result))).toString());
      this.thisSession.runPlugin("afterCommand", properties);
    } 
    this.quickLookupDirItem.clear();
  }
  
  public void renameFile(String oldpath, String newpath) throws PermissionDeniedException, FileNotFoundException, IOException {
    Properties p;
    try {
      stop_idle_timer();
      start_idle_timer();
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } 
    delay();
    oldpath = fixPath(oldpath);
    newpath = fixPath(newpath);
    this.thisSession.add_log_formatted("RNFR " + oldpath, "RNFR");
    this.thisSession.add_log_formatted("RNTO " + newpath, "RNTO");
    Log.log("SSH_SERVER", 2, "SFTP:renameFile:" + oldpath);
    Log.log("SSH_SERVER", 2, "SFTP:renameFile:" + newpath);
    this.thisSession.uiPUT("the_command_data", oldpath);
    this.thisSession.uiPUT("the_command", "RNFR");
    this.thisSession.runPlugin("beforeCommand", null);
    String result = "";
    try {
      result = this.thisSession.do_RNFR();
      Log.log("SSH_SERVER", 2, "SFTP:renameFile:" + result);
    } catch (Exception e) {
      this.thisSession.add_log_formatted("550 RNFR error:" + e.getMessage(), "RNFR");
      throw new IOException(e.getMessage());
    } 
    if (result.indexOf("%RNFR-not found%") >= 0) {
      this.thisSession.add_log_formatted("550 RNFR error:%RNFR-not found%", "RNFR");
      throw new FileNotFoundException("%RNFR-not found%");
    } 
    if (result.indexOf("%RNFR-bad%") >= 0) {
      this.thisSession.add_log_formatted("550 RNFR error:%RNFR-bad%", "RNFR");
      throw new PermissionDeniedException("%RNFR-bad%");
    } 
    this.thisSession.uiPUT("the_command_data", newpath);
    this.thisSession.uiPUT("the_command", "RNTO");
    this.thisSession.runPlugin("beforeCommand", null);
    result = "";
    try {
      try {
        result = this.thisSession.do_RNTO(ServerStatus.BG("ssh_rename_overwrite"));
      } catch (Exception e) {
        this.thisSession.add_log_formatted("550 RNFR error:" + e.getMessage(), "RNFR");
        throw new IOException(e.getMessage());
      } 
      if (result.indexOf("%RNTO-error%") >= 0) {
        this.thisSession.add_log_formatted("550 RNTO error:%RNTO-error%", "RNTO");
        throw new IOException("%RNTO-error%");
      } 
      if (result.indexOf("%RNTO-bad_ext%") >= 0) {
        this.thisSession.add_log_formatted("550 RNTO error:%RNTO-bad_ext%", "RNTO");
        throw new PermissionDeniedException("%RNTO-bad_ext%");
      } 
      if (result.indexOf("%RNTO-bad%") >= 0) {
        this.thisSession.add_log_formatted("550 RNTO error:%RNTO-bad%", "RNTO");
        throw new PermissionDeniedException("%RNTO-bad%");
      } 
    } finally {
      Properties properties = new Properties();
      properties.put("command_code", "0");
      properties.put("command_data", (new StringBuffer(String.valueOf(result))).toString());
      this.thisSession.runPlugin("afterCommand", properties);
    } 
    this.quickLookupDirItem.clear();
  }
  
  public void removeDirectory(String path) throws PermissionDeniedException, FileNotFoundException, IOException {
    Properties p;
    try {
      stop_idle_timer();
      start_idle_timer();
    } catch (Exception e) {
      Log.log("SSH_SERVER", 1, e);
    } 
    delay();
    path = fixPath(path);
    Log.log("SSH_SERVER", 2, "SFTP:removeDirectory:" + path);
    this.thisSession.uiPUT("the_command_data", path);
    this.thisSession.uiPUT("the_command", "RMD");
    this.thisSession.runPlugin("beforeCommand", null);
    String result = "";
    try {
      try {
        result = this.thisSession.do_RMD(path);
        Log.log("SSH_SERVER", 2, "SFTP:removeDirectory:" + result);
        if (result.equals("%RMD-not_empty%") && ServerStatus.BG("sftp_recurse_delete")) {
          result = this.thisSession.do_DELE(true, path);
          result = Common.replace_str(result, "DELE", "RMD");
          this.thisSession.add_log_formatted("RMD (deleted) " + path + "    " + result, "RMD");
          this.thisSession.uVFS.reset();
        } 
      } catch (Exception e) {
        Log.log("SSH_SERVER", 1, e);
        throw new IOException(e.getMessage());
      } 
      if (result.equals("%RMD-not_empty%")) {
        this.thisSession.add_log_formatted("550 RMD error:%RMD-not_empty%", "RMD");
        throw new IOException("%RMD-not_empty%");
      } 
      if (result.equals("%RMD-not_found%")) {
        this.thisSession.add_log_formatted("550 RMD error:%RMD-not_found%", "RMD");
        throw new FileNotFoundException("%RMD-not_found%");
      } 
      if (result.equals("%RMD-bad%")) {
        this.thisSession.add_log_formatted("550 RMD error:%RMD-bad%", "RMD");
        throw new PermissionDeniedException("%RMD-bad%");
      } 
    } finally {
      Properties properties = new Properties();
      properties.put("command_code", "0");
      properties.put("command_data", (new StringBuffer(String.valueOf(result))).toString());
      this.thisSession.runPlugin("afterCommand", properties);
    } 
    this.quickLookupDirItem.clear();
  }
  
  public void setFileAttributes(String path, SftpFileAttributes attrs) throws PermissionDeniedException, IOException, FileNotFoundException {
    try {
      Log.log("SSH_SERVER", 2, "SFTP:setFileAttributes1:" + path);
      setFileAttributes(path.getBytes("UTF8"), attrs);
      Log.log("SSH_SERVER", 2, "SFTP:setFileAttributes2:" + path);
    } catch (Exception e) {
      this.thisSession.add_log_formatted("550 setFileAttributes error:" + e.getMessage(), "CHMOD");
      throw new IOException(e.getMessage());
    } 
  }
  
  public void setFileAttributes(byte[] handle, SftpFileAttributes attrs) throws PermissionDeniedException, IOException, InvalidHandleException {
    String path_handle = new String(handle, "UTF8");
    String path = path_handle;
    if (path_handle.indexOf(":") > 0)
      path = path_handle.substring(0, path_handle.lastIndexOf(":")); 
    delay();
    path = fixPath(path);
    this.thisSession.add_log_formatted("MDTM " + path + "  " + (attrs.getModifiedTime().longValue() * 1000L), "MDTM");
    Log.log("SSH_SERVER", 2, "SFTP:setFileAttributes3:" + path);
    Properties item = null;
    try {
      for (int x = 0; x < 100; x++) {
        item = this.thisSession.uVFS.get_item(path);
        if (item != null)
          break; 
        Thread.sleep(100L);
      } 
    } catch (Exception exception) {}
    if (ServerStatus.IG("log_debug_level") >= 2)
      Log.log("SSH_SERVER", 2, "SFTP:setFileAttributes3:" + VRL.safe(item)); 
    try {
      GenericClient c = this.thisSession.uVFS.getClient(item);
      try {
        VRL vrl = new VRL(item.getProperty("url"));
        this.thisSession.uiPUT("the_command", "STOR");
        this.thisSession.add_log_formatted("STOR START " + path, "STOR");
        if (!this.thisSession.check_access_privs(path, "STOR"))
          throw new PermissionDeniedException("MDTM " + LOC.G("Denied!")); 
        if (attrs.getModifiedTime().longValue() > 0L && !ServerStatus.BG("disable_mdtm_modifications"))
          c.mdtm(vrl.getPath(), attrs.getModifiedTime().longValue() * 1000L); 
        if (this.thisSession.SG("site").toUpperCase().indexOf("(SITE_CHMOD)") < 0)
          return; 
        if (attrs.getPermissions() != null && attrs.getPermissions().intValue() > 0) {
          int i = attrs.getPermissions().intValue();
          StringBuffer buf = new StringBuffer();
          buf.append('0');
          buf.append(toOct(i, 6));
          buf.append(toOct(i, 3));
          buf.append(toOct(i, 0));
          c.setMod(vrl.getPath(), buf.toString(), "");
        } 
        Log.log("SSH_SERVER", 2, "SFTP:setFileAttributesModified:" + (attrs.getModifiedTime().longValue() * 1000L));
      } finally {
        c = this.thisSession.uVFS.releaseClient(c);
      } 
    } catch (Exception e) {
      this.thisSession.add_log_formatted("550 setFileAttributes error:" + e.getMessage(), "CHMOD");
      throw new IOException(e.getMessage());
    } 
  }
  
  private int toOct(int v, int r) {
    v >>>= r;
    return (((v & 0x4) != 0) ? 4 : 0) + (((v & 0x2) != 0) ? 2 : 0) + (((v & 0x1) != 0) ? 1 : 0);
  }
  
  public SftpFile readSymbolicLink(String path) throws UnsupportedFileOperationException, FileNotFoundException, IOException, PermissionDeniedException {
    throw new UnsupportedFileOperationException(LOC.G("Symbolic links are not supported by the Virtual File System"));
  }
  
  public void createSymbolicLink(String link, String target) throws UnsupportedFileOperationException, FileNotFoundException, IOException, PermissionDeniedException {
    throw new UnsupportedFileOperationException(LOC.G("Symbolic links are not supported by the Virtual File System"));
  }
  
  public void getOutputStream(long offset, String path, Properties item) throws Exception {
    this.thisSession.uiPUT("the_command_data", path);
    this.thisSession.uiPUT("the_command", "STOR");
    this.thisSession.runPlugin("beforeCommand", null);
    if (item.getProperty("url").length() > ServerStatus.IG("max_url_length"))
      throw new IOException("File url length too long:" + item.getProperty("url").length() + " vs. " + ServerStatus.IG("max_url_length")); 
    if (!this.thisSession.uiBG("gotFirstSSHOutputStream")) {
      this.thisSession.uiPUT("gotFirstSSHOutputStream", "true");
      this.thisSession.uVFS.get_item(Common.all_but_last(path));
    } 
    delay();
    this.thisSession.uiPUT("start_resume_loc", (new StringBuffer(String.valueOf(offset))).toString());
    String tempCurrentDir = this.thisSession.uiSG("current_dir");
    this.thisSession.uiPUT("current_dir", path);
    STOR_handler stor_files = (STOR_handler)item.get("stor_files");
    if (stor_files == null)
      synchronized (this.thisSession.stor_files_pool_free) {
        if (this.thisSession.stor_files_pool_free.size() > 0) {
          stor_files = this.thisSession.stor_files_pool_free.remove(0);
        } else {
          stor_files = new STOR_handler();
        } 
      }  
    if (path.endsWith(".filepart"))
      stor_files.allowTempExtensions = false; 
    stor_files.wait_for_parent_free = true;
    this.thisSession.stor_files_pool_used.addElement(stor_files);
    item.put("stor_files", stor_files);
    stor_files.setThreadName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (stor)");
    Socket local_s = Common.getSTORSocket(this.thisSession, stor_files, "", false, path, ServerStatus.BG("ssh_randomaccess"), offset, null);
    local_s.setSoTimeout(((this.thisSession.IG("max_idle_time") <= 0) ? 60 : this.thisSession.IG("max_idle_time")) * 1000 * 60);
    this.thisSession.uiPUT("current_dir", tempCurrentDir);
    int loops = 0;
    while (loops++ < 10000 && (stor_files.active2.getProperty("streamOpenStatus", "").equals("STOPPED") || stor_files.active2.getProperty("streamOpenStatus", "").equals("PENDING")))
      Thread.sleep(1L); 
    item.put("outputstream", local_s.getOutputStream());
    Properties p = new Properties();
    p.put("command_code", "0");
    p.put("command_data", (new StringBuffer(String.valueOf(path))).toString());
    this.thisSession.runPlugin("afterCommand", p);
  }
  
  public void getInputStream(long offset, String path, Properties item) throws Exception {
    this.thisSession.uiPUT("the_command_data", path);
    this.thisSession.uiPUT("the_command", "RETR");
    this.thisSession.runPlugin("beforeCommand", null);
    delay();
    this.thisSession.uiPUT("start_resume_loc", (new StringBuffer(String.valueOf(offset))).toString());
    String tempCurrentDir = this.thisSession.uiSG("current_dir");
    this.thisSession.uiPUT("current_dir", path);
    RETR_handler retr_files = (RETR_handler)item.get("retr_files");
    if (retr_files == null)
      synchronized (this.thisSession.retr_files_pool_free) {
        if (this.thisSession.retr_files_pool_free.size() > 0) {
          retr_files = this.thisSession.retr_files_pool_free.remove(0);
        } else {
          retr_files = new RETR_handler();
        } 
      }  
    this.thisSession.retr_files_pool_used.addElement(retr_files);
    item.put("retr_files", retr_files);
    retr_files.setThreadName(String.valueOf(this.thisSession.uiSG("user_name")) + ":(" + this.thisSession.uiSG("user_number") + ")-" + this.thisSession.uiSG("user_ip") + " (retr)");
    Socket local_s = Common.getRETRSocket(this.thisSession, retr_files, "", false);
    local_s.setSoTimeout(((this.thisSession.IG("max_idle_time") <= 0) ? 60 : this.thisSession.IG("max_idle_time")) * 1000 * 60);
    this.thisSession.uiPUT("current_dir", tempCurrentDir);
    int loops = 0;
    while (loops++ < 10000 && (retr_files.active2.getProperty("streamOpenStatus", "").equals("STOPPED") || retr_files.active2.getProperty("streamOpenStatus", "").equals("PENDING")))
      Thread.sleep(1L); 
    item.put("inputstream", new BufferedInputStream(local_s.getInputStream()));
    item.put("alreadyRead", new Properties());
    Properties p = new Properties();
    p.put("command_code", "0");
    p.put("command_data", (new StringBuffer(String.valueOf(path))).toString());
    this.thisSession.runPlugin("afterCommand", p);
  }
  
  public void closeFilesystem() {}
  
  public boolean fileExists(String path) throws IOException {
    path = fixPath(path);
    Properties item = null;
    item = (Properties)this.quickLookupDirItem.get(path);
    try {
      synchronized (this.statLock) {
        if (item == null)
          item = this.thisSession.uVFS.get_item(path); 
      } 
    } catch (Exception exception) {}
    this.thisSession.add_log_formatted("fileExists " + path + ":" + ((item != null) ? 1 : 0), "MDTM");
    return (item != null);
  }
  
  public String fixPath(String path) {
    try {
      if (path.startsWith("\"") && path.endsWith("\""))
        path = path.substring(1, path.length() - 1); 
      path = Common.dots(path);
    } catch (Exception exception) {}
    while (path.startsWith("."))
      path = path.substring(1); 
    if (path.equals("/"))
      path = this.thisSession.SG("root_dir"); 
    if (path.toUpperCase().startsWith("/") && !path.toUpperCase().startsWith(this.thisSession.SG("root_dir").toUpperCase()))
      path = String.valueOf(this.thisSession.SG("root_dir")) + path.substring(1); 
    if (!path.startsWith("/"))
      path = "/" + path; 
    if (!path.startsWith(this.thisSession.SG("root_dir")))
      path = String.valueOf(this.thisSession.SG("root_dir")) + path.substring(1); 
    if (path.endsWith("*"))
      path = path.substring(0, path.length() - 1); 
    while (path.indexOf("//") >= 0)
      path = Common.replace_str(path, "//", "/"); 
    return path;
  }
  
  public String getDefaultPath() throws FileNotFoundException {
    String path = "/";
    try {
      if (!ServerStatus.BG("jailproxy"))
        path = this.thisSession.uVFS.user_info.getProperty("default_current_dir", "/"); 
    } catch (Exception exception) {}
    return path;
  }
  
  public String getRealPath(String path) throws IOException, FileNotFoundException {
    if (path.equals("."))
      path = getDefaultPath(); 
    path = fixPath(path);
    if (path.startsWith(this.thisSession.SG("root_dir")))
      path = path.substring(this.thisSession.SG("root_dir").length() - 1); 
    return path;
  }
  
  private void delay() throws IOException {
    try {
      Thread.sleep(Integer.parseInt(this.thisSession.server_item.getProperty("commandDelayInterval", "0")));
    } catch (InterruptedException e) {
      throw new IOException();
    } 
  }
  
  public void start_idle_timer() throws Exception {
    if (this.thisSession.uiBG("user_logged_in")) {
      stop_idle_timer();
      start_idle_timer(this.thisSession.IG("max_idle_time"));
    } 
  }
  
  public void start_idle_timer(int timeout) throws Exception {
    try {
      if (this.thisSession.uiBG("user_logged_in")) {
        this.thread_killer_item.timeout = timeout;
        this.thread_killer_item.last_activity = (new Date()).getTime();
        this.thread_killer_item.enabled = (timeout != 0);
      } 
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
  }
  
  public void stop_idle_timer() throws Exception {
    try {
      this.thread_killer_item.enabled = false;
    } catch (Exception e) {
      if (e.indexOf("Interrupted") >= 0)
        throw e; 
    } 
  }
}
