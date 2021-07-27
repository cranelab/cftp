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
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class SMB3Client extends GenericClient {
  int lsBytesRead = 0;
  
  Properties dirCache = new Properties();
  
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
    SmbConfig cfg = SmbConfig.builder().build();
    this.client = new SMBClient(cfg);
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
    try {
      Connection connection = this.client.connect((new VRL(this.url)).getHost());
      AuthenticationContext ac = new AuthenticationContext(username, password.toCharArray(), domain);
      if (this.session != null)
        this.session.logoff(); 
      this.session = connection.authenticate(ac);
      return "";
    } catch (Exception e) {
      throw new Exception("login failed:" + e);
    } 
  }
  
  public void logout() {
    try {
      this.session.logoff();
    } catch (TransportException transportException) {}
  }
  
  public Properties stat(String path) throws Exception {
    VRL vrl = new VRL(String.valueOf(this.url) + path.substring(1));
    DiskShare share = (DiskShare)this.session.connectShare(Common.replace_str(Common.first(vrl.getPath()), "/", ""));
    try {
      String share_part = Common.first(path);
      int path_offset = share_part.length() + 1;
      if (share.folderExists(path.substring(path_offset).replace('/', '\\'))) {
        FileAllInformation test = share.getFileInformation(path.substring(path_offset).replace('/', '\\'));
        return stat(test, path, share);
      } 
      if (share.fileExists(path.substring(path_offset).replace('/', '\\'))) {
        FileAllInformation test = share.getFileInformation(path.substring(path_offset).replace('/', '\\'));
        return stat(test, path, share);
      } 
      if (System.getProperty("crushftp.isTestCall", "false").equals("true"))
        throw new Exception("Item not found..."); 
      return null;
    } finally {
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
    Common.log("SMB_CLIENT", 2, "Getting list for:" + path);
    VRL vrl = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl.getPath());
    Common.log("SMB_CLIENT", 2, "Getting list for share_part:" + share_part);
    Common.log("SMB_CLIENT", 2, "Getting list for VRL:" + vrl.safe());
    DiskShare share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
    try {
      this.dirCache.clear();
      int path_offset = share_part.length() + 1;
      String path2 = path.substring(path_offset);
      if (path2.endsWith("/"))
        path2 = path2.substring(0, path2.length() - 1); 
      List list2 = share.list(path2.replace('/', '\\'));
      Common.log("SMB_CLIENT", 2, "Getting list for path2:" + path2);
      for (int x = 0; x < list2.size(); x++) {
        FileIdBothDirectoryInformation item = list2.get(x);
        try {
          String tempName = item.getFileName();
          Common.log("SMB_CLIENT", 2, "Got list item:" + x + ":" + tempName);
          if (!tempName.equals(".") && !tempName.equals("..")) {
            FileAllInformation fai = share.getFileInformation(String.valueOf(path.substring(path_offset).replace('/', '\\')) + tempName);
            if (fai.getStandardInformation().isDirectory() && tempName.endsWith("/"))
              tempName = tempName.substring(0, tempName.length() - 1); 
            String tempPath = String.valueOf(path) + tempName;
            Common.log("SMB_CLIENT", 2, "Got list tempPath:" + x + ":" + tempPath);
            list.add(stat(fai, tempPath, share));
          } 
        } catch (Exception e) {
          Common.log("SMB_CLIENT", 1, String.valueOf(x) + " of " + list2.size() + ":Invalid file, or dead alias:" + item);
          Common.log("SMB_CLIENT", 1, e);
        } 
      } 
      this.dirCache.clear();
      return list;
    } finally {
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
    Common.log("SMB_CLIENT", 2, "Downloading :" + path);
    VRL vrl = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
    File f = share.openFile(path.substring(path_offset).replace('/', '\\'), new HashSet(Arrays.asList(new Object[] { AccessMask.GENERIC_READ }, )), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
    InputStream fin = f.getInputStream();
    try {
      if (startPos > 0L)
        fin.skip(startPos); 
    } catch (Exception e) {
      fin.close();
      throw e;
    } 
    this.in = new SMB3Client$1$InputWrapper(this, fin, startPos, endPos, f, share);
    return this.in;
  }
  
  public boolean mdtm(String path, long modified) throws Exception {
    Common.log("SMB_CLIENT", 2, "mdtm:" + path + ":" + modified);
    VRL vrl1 = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
    try {
      FileAllInformation current = share.getFileInformation(path.substring(path_offset).replace('/', '\\'));
      FileBasicInformation update = new FileBasicInformation(new FileTime(modified), new FileTime(modified), new FileTime(modified), new FileTime(modified), current.getBasicInformation().getFileAttributes());
      share.setFileInformation(path.substring(path_offset).replace('/', '\\'), update);
    } catch (Exception e) {
      log(e);
      return false;
    } finally {
      try {
        share.close();
      } catch (IOException iOException) {}
    } 
    return true;
  }
  
  public boolean rename(String rnfr, String rnto) {
    VRL vrl1 = new VRL(String.valueOf(this.url) + rnfr.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
    try {
      if (!share.fileExists(rnto.substring(path_offset).replace('/', '\\'))) {
        File f = share.openFile(rnfr.substring(path_offset).replace('/', '\\'), new HashSet(Arrays.asList(new Object[] { AccessMask.DELETE, AccessMask.GENERIC_WRITE }, )), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null);
        f.rename(rnto.substring(path_offset).replace('/', '\\'), false);
        f.close();
        return true;
      } 
    } catch (Exception e) {
      log(e);
    } finally {
      try {
        share.close();
      } catch (IOException iOException) {}
    } 
    return false;
  }
  
  protected OutputStream upload3(String path, long startPos, boolean truncate, boolean binary) throws Exception {
    Common.log("SMB_CLIENT", 2, "Uploading:" + path);
    VRL vrl = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
    try {
      File f = share.openFile(path.substring(path_offset).replace('/', '\\'), new HashSet(Arrays.asList(new Object[] { AccessMask.GENERIC_ALL }, )), new HashSet(Arrays.asList(new Object[] { FileAttributes.FILE_ATTRIBUTE_NORMAL }, )), SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE, null);
      this.out = new SMB3Client$1$OutputWrapper(this, f.getOutputStream(), f, share);
    } catch (Exception e) {
      log(e);
      try {
        share.close();
      } catch (IOException iOException) {}
    } 
    return this.out;
  }
  
  public boolean delete(String path) {
    Common.log("SMB_CLIENT", 2, "Delete:" + path);
    VRL vrl1 = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
    try {
      share.rm(path.substring(path_offset).replace('/', '\\'));
    } catch (Exception e) {
      log(e);
      return false;
    } finally {
      try {
        share.close();
      } catch (IOException iOException) {}
    } 
    return true;
  }
  
  public boolean makedir(String path) {
    Common.log("SMB_CLIENT", 2, "makedir:" + path);
    VRL vrl1 = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
    try {
      share.mkdir(path.substring(path_offset).replace('/', '\\'));
    } catch (Exception e) {
      log(e);
      return false;
    } finally {
      try {
        share.close();
      } catch (IOException iOException) {}
    } 
    return true;
  }
  
  public boolean makedirs(String path) throws Exception {
    Common.log("SMB_CLIENT", 2, "makedirs:" + path);
    VRL vrl1 = new VRL(String.valueOf(this.url) + path.substring(1));
    String share_part = Common.first(vrl1.getPath());
    int path_offset = share_part.length() + 1;
    DiskShare share = (DiskShare)this.session.connectShare(Common.replace_str(share_part, "/", ""));
    try {
      share.mkdir(path.substring(path_offset).replace('/', '\\'));
    } catch (Exception e) {
      log(e);
      return false;
    } finally {
      try {
        share.close();
      } catch (IOException iOException) {}
    } 
    return true;
  }
  
  public String doCommand(String command) throws Exception {
    return "";
  }
}
