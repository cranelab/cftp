package com.crushftp.client;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileBasicInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.Directory;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class SMB3Client extends GenericClient {
  int lsBytesRead = 0;
  
  Properties dirCache = new Properties();
  
  StringBuffer die_now = new StringBuffer();
  
  SMBClient client = null;
  
  Session session = null;
  
  public SMB3Client(String url, String header, Vector log) {
    super(header, log);
    if (url.indexOf("@\\") >= 0)
      url = url.replace('\\', '/'); 
    if (url.indexOf("@//") >= 0)
      url = Common.replace_str(url, "@//", "@"); 
    if (url.toUpperCase().startsWith("SMB3:/") && !url.toUpperCase().startsWith("SMB3://"))
      url = "SMB3://" + url.substring(6); 
    this.url = url;
    this.config.put("protocol", "SMB3");
  }
  
  public String simpleUrl(String s) {
    if (s.indexOf("@") >= 0)
      s = "SMB3://" + s.substring(s.indexOf("@") + 1); 
    return s;
  }
  
  public String login2(String username, String password, String clientid) throws Exception {
    String domain = null;
    username = VRL.vrlDecode(username);
    password = VRL.vrlDecode(password);
    if (username.indexOf("\\") >= 0) {
      domain = username.substring(0, username.indexOf("\\"));
      username = username.substring(username.indexOf("\\") + 1);
    } 
    this.config.put("username", username);
    this.config.put("password", password);
    if (domain != null)
      this.config.put("domain", domain); 
    if (clientid != null)
      this.config.put("clientid", clientid); 
    if (domain == null && this.config.containsKey("domain"))
      domain = this.config.getProperty("domain"); 
    for (int loops = 0; loops < 5; ) {
      try {
        SmbConfig.Builder builder = SmbConfig.builder();
        builder = builder.withDfsEnabled(this.config.getProperty("dfs_enabled", "false").equals("true"));
        if (!this.config.getProperty("read_buffer_size", "").equals(""))
          builder = builder.withReadBufferSize(Integer.parseInt(this.config.getProperty("read_buffer_size", ""))); 
        if (!this.config.getProperty("write_buffer_size", "").equals(""))
          builder = builder.withWriteBufferSize(Integer.parseInt(this.config.getProperty("write_buffer_size", ""))); 
        if (!this.config.getProperty("timeout", "").equals("")) {
          builder = builder.withSoTimeout(Long.parseLong(this.config.getProperty("timeout")), TimeUnit.MILLISECONDS);
          builder = builder.withTimeout(Long.parseLong(this.config.getProperty("timeout")), TimeUnit.MILLISECONDS);
        } 
        if (!this.config.getProperty("write_timeout", "").equals(""))
          builder = builder.withWriteTimeout(Long.parseLong(this.config.getProperty("write_timeout")), TimeUnit.MILLISECONDS); 
        if (!this.config.getProperty("read_timeout", "").equals(""))
          builder = builder.withReadTimeout(Long.parseLong(this.config.getProperty("read_timeout")), TimeUnit.MILLISECONDS); 
        if (!this.config.getProperty("smb3_required_signing", "false").equals("true"))
          builder = builder.withSigningRequired(true); 
        this.client = new SMBClient(builder.withMultiProtocolNegotiate(true).build());
        reconnect();
      } catch (Exception e) {
        if (loops >= 4)
          throw new Exception("login failed:" + e); 
        if (e instanceof TransportException) {
          loops++;
          continue;
        } 
        throw new Exception("login failed:" + e);
      } 
      break;
    } 
    return "";
  }
  
  private void reconnect() throws Exception {
    try {
      if (this.session != null)
        this.session.logoff(); 
    } catch (Exception exception) {}
    try {
      this.session = null;
      this.die_now.append("die");
      this.die_now = new StringBuffer();
      connect(this.config.getProperty("username", ""), this.config.getProperty("password", ""), this.config.getProperty("domain", ""));
    } catch (Exception e) {
      throw new Exception("login failed:" + e);
    } 
  }
  
  private void connect(String username, String password, String domain) throws Exception {
    Connection connection = null;
    VRL u = new VRL(this.url);
    if (!this.config.getProperty("use_dmz", "false").equals("false") && !this.config.getProperty("use_dmz", "no").equals("no") && !this.config.getProperty("use_dmz", "no").equals("null") && !this.config.getProperty("use_dmz", "").equals("")) {
      log("SMB_CLIENT:Connecting to:" + u.getHost() + ":" + u.getPort());
      ServerSocket ss = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
      Worker.startWorker(new Runnable(this, ss, u) {
            final SMB3Client this$0;
            
            private final ServerSocket val$ss;
            
            private final VRL val$u;
            
            public void run() {
              try {
                StringBuffer die_now2 = this.this$0.die_now;
                this.val$ss.setSoTimeout(5000);
                while (die_now2.length() == 0) {
                  try {
                    Socket sock2 = this.val$ss.accept();
                    Socket sock1 = Common.getSocket("SMB3", this.val$u, this.this$0.config.getProperty("use_dmz", "false"), "", Integer.parseInt(this.this$0.config.getProperty("timeout", "30000")));
                    sock1.setSoTimeout(600000);
                    if (Integer.parseInt(this.this$0.config.getProperty("timeout", "0")) > 0)
                      sock1.setSoTimeout(Integer.parseInt(this.this$0.config.getProperty("timeout", "0"))); 
                    this.this$0.log("SMB_CLIENT:Socket connected to:" + this.val$u.getHost() + ":" + this.val$u.getPort());
                    Common.streamCopier(sock1.getInputStream(), sock2.getOutputStream(), true, true, true);
                    Common.streamCopier(sock2.getInputStream(), sock1.getOutputStream(), true, true, true);
                  } catch (SocketTimeoutException socketTimeoutException) {}
                } 
                this.val$ss.close();
              } catch (Exception e) {
                Common.log("SMB_CLIENT", 1, e);
                this.this$0.log(e);
              } 
            }
          }"SMBv3 proxy thread for " + u.getHost() + ":" + u.getPort());
      connection = this.client.connect("127.0.0.1", ss.getLocalPort());
    } else {
      connection = this.client.connect(u.getHost(), 445);
    } 
    AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), domain);
    if (this.session != null) {
      this.session.logoff();
      this.die_now.append("die");
      this.die_now = new StringBuffer();
    } 
    this.session = connection.authenticate(ac);
    log("SMB_CLIENT:Authenticating to:" + username + "@" + u.getHost() + ":" + u.getPort());
  }
  
  public void logout() {
    try {
      if (this.session != null)
        this.session.logoff(); 
    } catch (TransportException transportException) {}
    this.die_now.append("die");
    this.die_now = new StringBuffer();
  }
  
  public Properties stat(String path) throws Exception {
    VRL vrl = new VRL(String.valueOf(this.url) + path.substring(1));
    if (!this.session.getConnection().isConnected())
      reconnect(); 
    DiskShare share = null;
    for (int x = 0; x < 5; x++) {
      try {
        share = (DiskShare)this.session.connectShare(Common.replace_str(Common.first(vrl.getPath()), "/", ""));
        break;
      } catch (Exception e) {
        Common.log("SMB_CLIENT", 1, e);
        try {
          Thread.sleep(3000L);
        } catch (Exception exception) {}
        reconnect();
      } 
    } 
    try {
      String share_part = Common.first(path);
      int path_offset = share_part.length() + 1;
      String path2 = path;
      if (path.endsWith("/"))
        path2 = path2.substring(0, path2.length() - 1); 
      if (path_offset == path.length()) {
        String name = share.getFileInformation("").getNameInformation();
        Properties dir_item = new Properties();
        dir_item.put("type", "DIR");
        dir_item.put("permissions", "drwxrwxrwx");
        dir_item.put("size", "1");
        Common.log("SMB_CLIENT", 2, "Got stat name:path:" + name + ":" + path + ":Dir=true");
        if (this.config.getProperty("count_dir_items", "false").equals("true")) {
          int i = 0;
          List list2 = share.list("");
          for (int j = 0; list2 != null && j < list2.size(); j++) {
            FileIdBothDirectoryInformation fibdi = list2.get(j);
            if (!fibdi.getFileName().startsWith("."))
              i++; 
          } 
          dir_item.put("size", (new StringBuffer(String.valueOf(i))).toString());
        } 
        dir_item.put("url", String.valueOf(this.url) + path.substring(1));
        dir_item.put("link", "false");
        dir_item.put("num_items", "1");
        dir_item.put("owner", "user");
        dir_item.put("group", "group");
        dir_item.put("protocol", "file");
        dir_item.put("root_dir", Common.all_but_last((new VRL(dir_item.getProperty("url"))).getPath()));
        setFileDateInfo(share.getFileInformation(""), dir_item);
        return dir_item;
      } 
      if (share.folderExists(path2.substring(path_offset).replace('/', '\\'))) {
        FileAllInformation test = share.getFileInformation(path2.substring(path_offset).replace('/', '\\'));
        return stat(test, path2, share);
      } 
      if (share.fileExists(path2.substring(path_offset).replace('/', '\\'))) {
        FileAllInformation test = share.getFileInformation(path2.substring(path_offset).replace('/', '\\'));
        return stat(test, path2, share);
      } 
      if (System.getProperty("crushftp.isTestCall" + Thread.currentThread().getId(), "false").equals("true"))
        throw new Exception("Item not found..."); 
      return null;
    } finally {
      if (share != null)
        share.close(); 
    } 
  }
  
  public Properties stat(FileAllInformation test, String path, DiskShare share) throws Exception {
    String share_part = Common.first(path);
    int path_offset = share_part.length() + 1;
    Properties dir_item = new Properties();
    String name = Common.last(test.getNameInformation());
    if (name.equals("")) {
      name = path;
      if (name.endsWith("/"))
        name = name.substring(0, name.length() - 1); 
      name = Common.last(name);
    } 
    dir_item.put("name", name.replaceAll("\r", "%0A").replaceAll("\n", "%0D"));
    dir_item.put("size", "0");
    if (test.getStandardInformation().isDirectory()) {
      if (!path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
      dir_item.put("type", "DIR");
      dir_item.put("permissions", "drwxrwxrwx");
      dir_item.put("size", "1");
      Common.log("SMB_CLIENT", 2, "Got stat name:path:" + name + ":" + path + ":Dir=true");
      if (this.config.getProperty("count_dir_items", "false").equals("true")) {
        int i = 0;
        List list2 = share.list(path.substring(path_offset, path.length() - 1).replace('/', '\\'));
        for (int x = 0; list2 != null && x < list2.size(); x++) {
          FileIdBothDirectoryInformation fibdi = list2.get(x);
          if (!fibdi.getFileName().startsWith("."))
            i++; 
        } 
        dir_item.put("size", (new StringBuffer(String.valueOf(i))).toString());
      } 
      if (!path.endsWith("/"))
        path = String.valueOf(path) + "/"; 
    } else {
      Common.log("SMB_CLIENT", 2, "Got stat name:path:" + name + ":" + path + ":Dir=false");
      dir_item.put("type", "FILE");
      dir_item.put("permissions", "-rwxrwxrwx");
      dir_item.put("size", (new StringBuffer(String.valueOf(getSize(test)))).toString());
    } 
    dir_item.put("url", String.valueOf(this.url) + path.substring(1));
    dir_item.put("link", "false");
    dir_item.put("num_items", "1");
    dir_item.put("owner", "user");
    dir_item.put("group", "group");
    dir_item.put("protocol", "file");
    dir_item.put("root_dir", Common.all_but_last((new VRL(dir_item.getProperty("url"))).getPath()));
    setFileDateInfo(test, dir_item);
    return dir_item;
  }
  
  private long getSize(FileAllInformation test) {
    return test.getStandardInformation().getEndOfFile();
  }
  
  public Vector list(String path, Vector list) throws Exception {
    if (!path.replace('\\', '/').endsWith("/"))
      path = String.valueOf(path) + "/"; 
    if (!path.endsWith("/"))
      path = String.valueOf(path) + "/"; 
    log("SMB_CLIENT:Getting list for:" + path);
    VRL vrl = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl.getPath());
    Common.log("SMB_CLIENT", 2, "Getting list for share_part:" + share_part);
    Common.log("SMB_CLIENT", 2, "Getting list for VRL:" + vrl.safe());
    if (!this.session.getConnection().isConnected())
      reconnect(); 
    DiskShare share = null;
    for (int x = 0; x < 5; x++) {
      try {
        share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
        break;
      } catch (Exception e) {
        Common.log("SMB_CLIENT", 1, e);
        log(e);
        log("Retrying in 3 seconds due to prior error...");
        try {
          Thread.sleep(3000L);
        } catch (Exception exception) {}
        reconnect();
      } 
    } 
    try {
      this.dirCache.clear();
      int path_offset = share_part.length() + 1;
      String path2 = path.substring(path_offset);
      if (path2.endsWith("/"))
        path2 = path2.substring(0, path2.length() - 1); 
      List list2 = share.list(path2.replace('/', '\\'));
      Common.log("SMB_CLIENT", 2, "Getting list for path2:" + path2);
      for (int i = 0; i < list2.size(); i++) {
        FileIdBothDirectoryInformation item = list2.get(i);
        try {
          String tempName = item.getFileName();
          Common.log("SMB_CLIENT", 2, "Got list item:" + i + ":" + tempName);
          if (!tempName.equals(".") && !tempName.equals("..")) {
            String tempPath = String.valueOf(path.substring(path_offset).replace('/', '\\')) + tempName;
            Common.log("SMB_CLIENT", 2, "Getting info for path item:" + i + ":" + tempPath);
            FileAllInformation fai = share.getFileInformation(tempPath);
            if (fai.getStandardInformation().isDirectory() && tempName.endsWith("/"))
              tempName = tempName.substring(0, tempName.length() - 1); 
            tempPath = String.valueOf(path) + tempName;
            Common.log("SMB_CLIENT", 2, "Got list tempPath:" + i + ":" + tempPath);
            list.add(stat(fai, tempPath, share));
          } 
        } catch (Exception e) {
          Common.log("SMB_CLIENT", 1, String.valueOf(i + 1) + " of " + list2.size() + ":Invalid file, or dead alias:" + item.getFileName() + ":" + e);
          Common.log("SMB_CLIENT", 1, e);
          log(String.valueOf(i + 1) + " of " + list2.size() + ":Invalid file, or dead alias:" + item.getFileName() + ":" + e);
          log(e);
        } 
      } 
      this.dirCache.clear();
      return list;
    } finally {
      if (share != null)
        share.close(); 
    } 
  }
  
  private void setFileDateInfo(FileAllInformation test, Properties dir_item) {
    Date itemDate = new Date(test.getBasicInformation().getLastWriteTime().toEpochMillis());
    dir_item.put("modified", (new StringBuffer(String.valueOf(itemDate.getTime()))).toString());
    dir_item.put("month", months[Integer.parseInt(this.mm.format(itemDate))]);
    dir_item.put("day", this.dd.format(itemDate));
    String time_or_year = this.hhmm.format(itemDate);
    if (!this.yyyy.format(itemDate).equals(this.yyyy.format(new Date())) || System.getProperty("crushftp.ls.year", "false").equals("true"))
      time_or_year = this.yyyy.format(itemDate); 
    dir_item.put("time_or_year", time_or_year);
  }
  
  protected InputStream download3(String path, long startPos, long endPos, boolean binary) throws Exception {
    log("SMB_CLIENT:Downloading :" + path);
    VRL vrl = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl.getPath());
    int path_offset = share_part.length() + 1;
    if (!this.session.getConnection().isConnected())
      reconnect(); 
    DiskShare share2 = null;
    for (int x = 0; x < 5; x++) {
      try {
        share2 = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
        break;
      } catch (Exception e) {
        Common.log("SMB_CLIENT", 1, e);
        try {
          Thread.sleep(3000L);
        } catch (Exception exception) {}
        reconnect();
      } 
    } 
    DiskShare share = share2;
    File f = share.openFile(path.substring(path_offset).replace('/', '\\'), new HashSet(Arrays.asList(new Object[] { AccessMask.GENERIC_READ }, )), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
    InputStream fin = f.getInputStream();
    try {
      if (startPos > 0L)
        fin.skip(startPos); 
    } catch (Exception e) {
      fin.close();
      throw e;
    } 
    this.in = new null.InputWrapper(this, fin, startPos, endPos, f, share);
    return this.in;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    log("SMB_CLIENT:mdtm:" + path + ":" + modified);
    VRL vrl1 = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    if (!this.session.getConnection().isConnected())
      reconnect(); 
    DiskShare share = null;
    for (int x = 0; x < 5; x++) {
      try {
        share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
        break;
      } catch (Exception e) {
        Common.log("SMB_CLIENT", 1, e);
        try {
          Thread.sleep(3000L);
        } catch (Exception exception) {}
        reconnect();
      } 
    } 
    try {
      FileAllInformation current = share.getFileInformation(path.substring(path_offset).replace('/', '\\'));
      FileBasicInformation update = new FileBasicInformation(FileTime.fromDate(new Date()), FileTime.fromDate(new Date()), FileTime.fromDate(new Date(modified)), FileTime.fromDate(new Date(modified)), current.getBasicInformation().getFileAttributes());
      share.setFileInformation(path.substring(path_offset).replace('/', '\\'), update);
    } catch (Exception e) {
      log(e);
      return false;
    } finally {
      try {
        if (share != null)
          share.close(); 
      } catch (IOException iOException) {}
    } 
    return true;
  }
  
  public boolean rename(String rnfr, String rnto) {
    Properties p = null;
    try {
      p = stat(rnfr);
    } catch (Exception e) {
      log(e);
    } 
    VRL vrl1 = new VRL(String.valueOf(this.url) + rnfr.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = null;
    try {
      if (!this.session.getConnection().isConnected())
        reconnect(); 
      for (int x = 0; x < 5; x++) {
        try {
          share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
          break;
        } catch (Exception e) {
          Common.log("SMB_CLIENT", 1, e);
          try {
            Thread.sleep(3000L);
          } catch (Exception exception) {}
          reconnect();
        } 
      } 
      if (!share.fileExists(rnto.substring(path_offset).replace('/', '\\'))) {
        if (p.getProperty("type").equalsIgnoreCase("DIR")) {
          Directory f = share.openDirectory(rnfr.substring(path_offset).replace('/', '\\'), new HashSet(Arrays.asList(new Object[] { AccessMask.DELETE, AccessMask.GENERIC_WRITE }, )), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
          f.rename(rnto.substring(path_offset).replace('/', '\\'), false);
          f.close();
        } else {
          File f = share.openFile(rnfr.substring(path_offset).replace('/', '\\'), new HashSet(Arrays.asList(new Object[] { AccessMask.DELETE, AccessMask.GENERIC_WRITE }, )), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
          f.rename(rnto.substring(path_offset).replace('/', '\\'), false);
          f.close();
        } 
        return true;
      } 
    } catch (Exception e) {
      log(e);
    } finally {
      try {
        if (share != null)
          share.close(); 
      } catch (IOException iOException) {}
    } 
    return false;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    log("SMB_CLIENT:Uploading:" + path);
    VRL vrl = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl.getPath());
    int path_offset = share_part.length() + 1;
    if (!this.session.getConnection().isConnected())
      reconnect(); 
    DiskShare share2 = null;
    for (int x = 0; x < 5; x++) {
      try {
        share2 = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
        break;
      } catch (Exception e) {
        Common.log("SMB_CLIENT", 1, e);
        try {
          Thread.sleep(3000L);
        } catch (Exception exception) {}
        reconnect();
      } 
    } 
    DiskShare share = share2;
    File f = null;
    if (startPos > 0L) {
      f = share.openFile(path.substring(path_offset).replace('/', '\\'), new HashSet(Arrays.asList(new Object[] { AccessMask.FILE_READ_ATTRIBUTES, AccessMask.GENERIC_WRITE, AccessMask.FILE_APPEND_DATA }, )), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
    } else {
      f = share.openFile(path.substring(path_offset).replace('/', '\\'), new HashSet(Arrays.asList(new Object[] { AccessMask.GENERIC_WRITE }, )), new HashSet(Arrays.asList(new Object[] { FileAttributes.FILE_ATTRIBUTE_NORMAL }, )), SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null);
    } 
    try {
      this.out = new null.OutputWrapper(this, f, startPos, share);
    } catch (Exception e) {
      log(e);
      try {
        share.close();
      } catch (IOException iOException) {}
    } 
    return this.out;
  }
  
  public boolean delete(String path) {
    Properties p = null;
    try {
      p = stat(path);
    } catch (Exception e) {
      log(e);
    } 
    if (p == null)
      return true; 
    log("SMB_CLIENT:Delete:" + path);
    VRL vrl1 = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = null;
    try {
      if (!this.session.getConnection().isConnected())
        reconnect(); 
      for (int x = 0; x < 5; x++) {
        try {
          share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
          break;
        } catch (Exception e) {
          Common.log("SMB_CLIENT", 1, e);
          try {
            Thread.sleep(3000L);
          } catch (Exception exception) {}
          reconnect();
        } 
      } 
      if (p.getProperty("type").equalsIgnoreCase("DIR")) {
        share.rmdir(path.substring(path_offset).replace('/', '\\'), false);
      } else {
        share.rm(path.substring(path_offset).replace('/', '\\'));
      } 
    } catch (Exception e) {
      log(e);
      return false;
    } finally {
      try {
        if (share != null)
          share.close(); 
      } catch (IOException iOException) {}
    } 
    return true;
  }
  
  public boolean makedir(String path) {
    log("SMB_CLIENT:makedir:" + path);
    VRL vrl1 = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = null;
    try {
      if (!this.session.getConnection().isConnected())
        reconnect(); 
      for (int x = 0; x < 5; x++) {
        try {
          share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
          break;
        } catch (Exception e) {
          Common.log("SMB_CLIENT", 1, e);
          try {
            Thread.sleep(3000L);
          } catch (Exception exception) {}
          reconnect();
        } 
      } 
      share.mkdir(path.substring(path_offset).replace('/', '\\'));
    } catch (Exception e) {
      log(e);
      return false;
    } finally {
      try {
        if (share != null)
          share.close(); 
      } catch (IOException iOException) {}
    } 
    return true;
  }
  
  public boolean makedirs(String path) throws Exception {
    log("SMB_CLIENT:makedirs:" + path);
    VRL vrl1 = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    boolean ok = true;
    String[] parts = path.substring(path_offset).split("/");
    String path2 = "";
    for (int x = 0; x < parts.length && ok; x++) {
      path2 = String.valueOf(path2) + parts[x] + "/";
      Properties stat2 = null;
      try {
        stat2 = stat(String.valueOf(share_part) + "/" + path2);
      } catch (Exception e) {
        log(e);
      } 
      if (stat2 == null)
        ok = makedir(String.valueOf(share_part) + "/" + path2); 
    } 
    return ok;
  }
  
  public String doCommand(String command) throws Exception {
    return "";
  }
}
