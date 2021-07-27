package crushftp.handlers;

import com.crushftp.client.Base64;
import com.crushftp.client.Common;
import com.crushftp.client.URLConnection;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateHandler2 {
  public String CRLF = "\r\n";
  
  public InputStream di = null;
  
  static final long serialVersionUID = 0L;
  
  public long updateMaxSize = 51200L;
  
  public long updateCurrentLoc = 0L;
  
  public String updateCurrentStatus = "";
  
  boolean stopNow = false;
  
  SimpleDateFormat MMddyyHHmmss = new SimpleDateFormat("MMddyyHHmmss", Locale.US);
  
  public boolean doSilentUpdate(boolean earlyAccess, String thisVersion, boolean webOnly) throws Exception {
    this.stopNow = false;
    this.updateCurrentLoc = 0L;
    this.updateCurrentStatus = "Building list of files...";
    int minMd5Size = 262144;
    String fname = "CrushFTP" + Common.V() + "_" + ((Common.machine_is_x() && Common.OSXApp()) ? "OSX" : "PC");
    this.updateCurrentStatus = "Building list of files..." + fname;
    String url = "https://www.crushftp.com/";
    String username = "early" + Common.V();
    String password = "early" + Common.V();
    String home = System.getProperty("crushftp.home");
    if (Common.machine_is_x() && Common.OSXApp())
      home = String.valueOf(System.getProperty("crushftp.home")) + "../../../../"; 
    String homeF = (new File(home)).getCanonicalPath();
    String backup1 = String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + this.MMddyyHHmmss.format(new Date()) + "_" + thisVersion + " Files_tmp/";
    String backup2 = String.valueOf(System.getProperty("crushftp.backup")) + "backup/" + this.MMddyyHHmmss.format(new Date()) + "_" + thisVersion + " Files/";
    (new File(backup1)).mkdirs();
    (new File(backup2)).mkdirs();
    if (!(new File(String.valueOf(home) + fname + "_new.zip")).exists()) {
      Vector files2 = new Vector();
      int loops = 0;
      while (files2.size() == 0 && loops++ < 5) {
        files2 = getServerFileInfo(fname, username, password, url);
        if (files2.size() == 0)
          Thread.sleep(3000L); 
      } 
      Vector files = files2;
      files.removeElementAt(files.size() - 1);
      for (int x = files.size() - 1; x >= 0; x--) {
        this.updateCurrentStatus = "Building list of files...:" + x;
        Properties p = files.elementAt(x);
        if (p.getProperty("name").equals("crushftp_init.sh")) {
          files.remove(x);
        } else if (p.getProperty("name").equals("Info.plist")) {
          files.remove(x);
        } else if (p.getProperty("name").equals("CrushFTP.command")) {
          files.remove(x);
        } else if (p.getProperty("name").equals("CrushFTP7_OSX")) {
          files.remove(x);
        } else if (p.getProperty("name").equals("CrushFTP7_PC")) {
          files.remove(x);
        } else if (p.getProperty("name").equals("CrushFTP7.app")) {
          files.remove(x);
        } else if (p.getProperty("name").equals("CrushFTP7.app.zip")) {
          files.remove(x);
        } else if (p.getProperty("name").equalsIgnoreCase(".DS_Store")) {
          files.remove(x);
        } else if (p.getProperty("name").equalsIgnoreCase("thumbs.db")) {
          files.remove(x);
        } else if (p.getProperty("name").equalsIgnoreCase("logo.png")) {
          files.remove(x);
        } else if (p.getProperty("name").equalsIgnoreCase("mime_types.txt")) {
          files.remove(x);
        } else if (p.getProperty("name").equalsIgnoreCase("AttachmentRedirectorSettings.xml")) {
          files.remove(x);
        } else if (p.getProperty("name").equalsIgnoreCase("favicon.ico")) {
          files.remove(x);
        } else if (p.getProperty("name").equalsIgnoreCase("win_service.jar") && Common.machine_is_x()) {
          files.remove(x);
        } else if (p.getProperty("path").toUpperCase().indexOf("/USERS/") >= 0) {
          files.remove(x);
        } else if (webOnly && p.getProperty("path").indexOf("/WebInterface/") < 0) {
          files.remove(x);
        } 
        if (this.stopNow)
          return false; 
      } 
      StringBuffer localStatus = new StringBuffer();
      (new Thread(new Runnable(this, files, homeF, localStatus) {
            final UpdateHandler2 this$0;
            
            private final Vector val$files;
            
            private final String val$homeF;
            
            private final StringBuffer val$localStatus;
            
            public void run() {
              for (int x = 0; x < this.val$files.size(); x++) {
                this.this$0.updateCurrentStatus = "Building list of file differences...:" + (x + 1) + " of " + (this.val$files.size() + 1);
                if (this.this$0.stopNow)
                  break; 
                Properties p = this.val$files.elementAt(x);
                String tmpPath = p.getProperty("path");
                tmpPath = tmpPath.substring(tmpPath.indexOf("/", 1));
                if ((new File(String.valueOf(this.val$homeF) + tmpPath)).exists()) {
                  p.put("localSize", (new StringBuffer(String.valueOf((new File(String.valueOf(this.val$homeF) + tmpPath)).length()))).toString());
                  p.put("localModified", (new StringBuffer(String.valueOf((new File(String.valueOf(this.val$homeF) + tmpPath)).lastModified()))).toString());
                  if (p.getProperty("type").equals("FILE"))
                    try {
                      p.put("localMd5", Common.getMD5(new FileInputStream(new File(String.valueOf(this.val$homeF) + tmpPath))).trim().toUpperCase().substring(24));
                    } catch (Exception e) {
                      Log.log("UPDATE", 0, e);
                    }  
                } 
              } 
              this.val$localStatus.append("done");
            }
          })).start();
      for (int i = 0; i < files.size(); i++) {
        this.updateCurrentStatus = "Building list of server differences...:" + (i + 1) + " of " + (files.size() + 1);
        Properties p = files.elementAt(i);
        if (p.getProperty("type").equals("FILE") && !p.containsKey("md5")) {
          Log.log("UPDATE", 0, "Looking up file md5:" + p);
          Properties config = new Properties();
          config.put("user_dmz", "(current_server)");
          URLConnection urlc = URLConnection.openConnection(new VRL(String.valueOf(url) + "?command=getMd5s&chunked=false&path=" + Base64.encodeBytes(p.getProperty("path").getBytes("UTF8"))), config);
          String auth = "Basic " + Base64.encodeBytes((String.valueOf(username) + ":" + password).getBytes());
          urlc.setRequestProperty("Authorization", auth);
          urlc.setRequestMethod("GET");
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          Common.copyStreams(urlc.getInputStream(), baos, true, true);
          p.put("md5", "");
          try {
            p.put("md5", (new String(baos.toByteArray())).trim().toUpperCase().substring(24));
          } catch (Exception e) {
            Log.log("SERVER", 1, e);
          } 
        } 
        if (this.stopNow)
          return false; 
      } 
      while (localStatus.length() == 0)
        Thread.sleep(100L); 
      StringBuffer paths = new StringBuffer();
      int largeFiles = 0;
      int smallFiles = 0;
      long totalBytes = 0L;
      for (int j = 0; j < files.size(); j++) {
        Properties p = files.elementAt(j);
        if (p.getProperty("type").equals("DIR")) {
          String tmpPath = p.getProperty("path");
          tmpPath = tmpPath.substring(tmpPath.indexOf("/", 1));
          if (!(new File(String.valueOf(homeF) + tmpPath)).exists())
            (new File(String.valueOf(homeF) + tmpPath)).mkdirs(); 
        } else if (!p.getProperty("md5", "1").trim().equalsIgnoreCase(p.getProperty("localMd5", "2").trim())) {
          paths.append(p.getProperty("path"));
          if (j < files.size() - 1)
            paths.append(":"); 
          totalBytes += Long.parseLong(p.getProperty("size"));
          if (p.getProperty("name").endsWith(".jar") || Long.parseLong(p.getProperty("size")) > 262144L) {
            Log.log("UPDATE", 0, "Update needed on larger file:" + p.getProperty("path"));
            largeFiles++;
          } else {
            smallFiles++;
          } 
        } 
        if (this.stopNow)
          return false; 
      } 
      this.updateMaxSize = totalBytes;
      Log.log("UPDATE", 0, "Updating " + smallFiles + " small files, and " + largeFiles + " large files:" + Common.format_bytes_short(this.updateMaxSize));
      if (this.stopNow)
        return false; 
      Log.log("UPDATE", 0, "Updating files:" + paths);
      try {
        this.updateCurrentStatus = "Downloading " + (smallFiles + largeFiles) + " files.";
        doHTTPDownloads(home, fname, earlyAccess, paths.toString());
        this.updateCurrentStatus = "Unzipping " + (smallFiles + largeFiles) + " files.";
      } finally {
        this.updateCurrentLoc = 0L;
        this.updateMaxSize = 51200L;
        this.stopNow = false;
      } 
      Common.recurseCopy(String.valueOf(home) + fname + "_new.zip", String.valueOf(backup2) + fname + "_new.zip", true);
    } else {
      this.updateCurrentStatus = "Using local offline file...: " + fname + "_new.zip";
      Thread.sleep(1000L);
    } 
    try {
      Common.recurseCopy("./CrushFTP.jar", String.valueOf(backup1) + "CrushFTP.jar", true);
      Common.recurseCopy("./plugins/", String.valueOf(backup1) + "plugins/", true);
      Common.recurseCopy(String.valueOf(home) + "WebInterface/", String.valueOf(backup1) + "WebInterface/", true);
      Vector list = new Vector();
      Common.appendListing(backup1, list, "", 99, true);
      Vector list2 = new Vector();
      Properties history_hash = new Properties();
      for (int x = 0; x < list.size(); x++) {
        Properties p = new Properties();
        URL uRL = ((File)list.elementAt(x)).toURI().toURL();
        p.put("url", uRL);
        if (!history_hash.containsKey(uRL))
          list2.addElement(p); 
        history_hash.put(uRL, "");
      } 
      Common.zip(backup1, list2, String.valueOf(backup2) + "full_app.zip");
      Common.recurseDelete(backup1, false);
      doUpdate(home, backup2, String.valueOf(fname) + "_new.zip");
      (new File(String.valueOf(home) + fname + "_new.zip")).delete();
      if (this.stopNow)
        return false; 
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      this.updateCurrentLoc = 0L;
      this.updateMaxSize = 51200L;
      this.stopNow = false;
    } 
    this.updateCurrentStatus = "COMPLETE";
    Thread.sleep(2000L);
    if (webOnly)
      (new File(String.valueOf(home) + fname + "_new.zip")).delete(); 
    Worker.startWorker(new Runnable(this) {
          final UpdateHandler2 this$0;
          
          public void run() {
            try {
              Thread.sleep(10000L);
            } catch (Exception exception) {}
            this.this$0.updateCurrentStatus = "";
          }
        });
    return true;
  }
  
  public void cancel() {
    this.stopNow = true;
  }
  
  public Vector getServerFileInfo(String f, String username, String password, String url) {
    Vector serverFileList = new Vector();
    URLConnection urlc = null;
    Properties config = new Properties();
    config.put("user_dmz", "(current_server)");
    int loops = 0;
    while (loops++ < 5) {
      try {
        VRL u = new VRL(String.valueOf(url) + f + "/:filetree");
        Log.log("UPDATE", 0, "Getting folder contents information " + u);
        urlc = URLConnection.openConnection(u, config);
        String auth = "Basic " + Base64.encodeBytes((String.valueOf(username) + ":" + password).getBytes());
        urlc.setRequestProperty("Authorization", auth);
        urlc.setReadTimeout(70000);
        urlc.setRequestMethod("GET");
        urlc.setUseCaches(false);
        urlc.setDoInput(true);
        InputStream in = urlc.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF8"));
        String data = "";
        try {
          while ((data = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(data);
            Properties p = new Properties();
            p.put("permissions", st.nextToken());
            if (p.getProperty("permissions").startsWith("d")) {
              p.put("type", "DIR");
            } else {
              p.put("type", "FILE");
            } 
            st.nextToken();
            String owner = st.nextToken();
            String group = st.nextToken();
            if (owner.equalsIgnoreCase("MD5"))
              p.put("md5", group); 
            long tempfileSize = Long.parseLong(st.nextToken());
            p.put("modified", st.nextToken());
            st.nextToken();
            String year = st.nextToken();
            String rootdir = data.substring(data.indexOf(String.valueOf(year) + " /") + (String.valueOf(year) + " ").length()).trim();
            p.put("path", rootdir);
            p.put("name", Common.last(rootdir));
            p.put("size", (new StringBuffer(String.valueOf(tempfileSize))).toString());
            serverFileList.addElement(p);
          } 
        } finally {
          urlc.getResponseCode();
        } 
        break;
      } catch (Exception e) {
        Log.log("UPDATE", 0, e);
      } finally {
        try {
          if (urlc != null)
            urlc.disconnect(); 
        } catch (IOException iOException) {}
      } 
    } 
    return serverFileList;
  }
  
  public static void doUpdate(String home, String backup, String zip) throws Exception {
    Vector errors = new Vector();
    if (unzip(String.valueOf(home) + zip, errors, backup)) {
      String batch = "";
      String manualFiles = "";
      if (Common.machine_is_windows())
        batch = String.valueOf(batch) + "net stop \"CrushFTP Server\"\r\nping 127.0.0.1 -n 5\r\n"; 
      for (int x = 0; x < errors.size(); x++) {
        Properties p = errors.elementAt(x);
        String source = p.getProperty("source");
        String dest = p.getProperty("dest");
        String back = p.getProperty("backup");
        if (Common.machine_is_windows()) {
          manualFiles = String.valueOf(manualFiles) + source + "\r\n";
          source = source.replace('/', '\\');
          dest = dest.replace('/', '\\');
          back = back.replace('/', '\\');
          batch = String.valueOf(batch) + "move \"" + dest + "\" \"" + back + "\"\r\n";
          batch = String.valueOf(batch) + "move \"" + source + "\" \"" + dest + "\"\r\n";
          batch = Common.replace_str(batch, "\\\\", "\\");
        } 
      } 
      manualFiles = String.valueOf(manualFiles) + "UpdateTemp/nothing_to_do.txt\r\n";
      if (Common.machine_is_windows())
        batch = String.valueOf(batch) + "net start \"CrushFTP Server\"\r\nping 127.0.0.1 -n 10\r\n"; 
      if (batch.length() > 0) {
        String fname = "update.sh";
        if (Common.machine_is_windows()) {
          fname = "update.bat";
          batch = String.valueOf(batch) + "del \"" + (new File(String.valueOf(home) + fname)).getCanonicalPath() + "\"\r\n";
        } 
        RandomAccessFile out = new RandomAccessFile(String.valueOf(home) + fname, "rw");
        out.setLength(0L);
        out.write(batch.getBytes());
        out.close();
        out = new RandomAccessFile(String.valueOf(home) + "update_list.txt", "rw");
        out.setLength(0L);
        out.write(manualFiles.getBytes());
        out.close();
      } 
    } 
  }
  
  public void doHTTPDownloads(String home, String fname, boolean earlyAccess, String paths) throws Exception {
    String url = "https://www.crushftp.com/";
    String username = "update" + Common.V();
    String password = "update" + Common.V();
    if (earlyAccess) {
      username = "early" + Common.V();
      password = "early" + Common.V();
    } 
    getFileHTTP(home, fname, "zip", url, paths, username, password);
  }
  
  public static boolean unzip(String sourcePath, Vector errors, String destPath) throws Exception {
    ZipInputStream zin = new ZipInputStream(new FileInputStream(sourcePath));
    Log.log("UPDATE", 3, "Unzipping:" + sourcePath);
    sourcePath = Common.all_but_last(sourcePath);
    try {
      ZipEntry entry;
      while ((entry = zin.getNextEntry()) != null) {
        String path2 = entry.getName();
        path2 = path2.replace('\\', '/');
        path2 = path2.replace('\\', '/');
        path2 = Common.replace_str(path2, "..", "");
        path2 = path2.substring(path2.indexOf("/", 1));
        if (entry.isDirectory()) {
          (new File(String.valueOf(sourcePath) + path2)).mkdirs();
          if (path2.indexOf("WebInterface") < 0)
            Common.updateOSXInfo(String.valueOf(sourcePath) + path2); 
          Log.log("UPDATE", 0, "Updating directory:" + sourcePath + path2);
          continue;
        } 
        if ((new File(Common.all_but_last(String.valueOf(sourcePath) + path2))).mkdirs() && path2.indexOf("WebInterface") < 0)
          Common.updateOSXInfo(Common.all_but_last(String.valueOf(sourcePath) + path2)); 
        byte[] b = new byte[32768];
        int bytes_read = 0;
        String ext = "_tmp";
        if ((String.valueOf(sourcePath) + path2).indexOf("/WebInterface/") >= 0)
          ext = ""; 
        (new File(String.valueOf(sourcePath) + path2 + "_tmp")).delete();
        RandomAccessFile out = new RandomAccessFile(String.valueOf(sourcePath) + path2 + ext, "rw");
        if ((String.valueOf(sourcePath) + path2).indexOf("/WebInterface/localizations/") >= 0 && out.length() > 0L) {
          out.close();
          out = null;
        } 
        boolean skip_update = false;
        if (path2.endsWith("crushftp_init.sh")) {
          skip_update = true;
        } else if (path2.endsWith("Info.plist")) {
          skip_update = true;
        } else if (path2.endsWith("CrushFTP.command")) {
          skip_update = true;
        } else if (path2.endsWith("CrushFTP7_OSX")) {
          skip_update = true;
        } else if (path2.endsWith("CrushFTP7_PC")) {
          skip_update = true;
        } else if (path2.endsWith("CrushFTP7.app")) {
          skip_update = true;
        } else if (path2.endsWith("CrushFTP7.app.zip")) {
          skip_update = true;
        } else if (path2.endsWith(".DS_Store")) {
          skip_update = true;
        } else if (path2.endsWith("thumbs.db")) {
          skip_update = true;
        } else if (path2.endsWith("logo.png")) {
          skip_update = true;
        } else if (path2.endsWith("mime_types.txt")) {
          skip_update = true;
        } else if (path2.endsWith("AttachmentRedirectorSettings.xml")) {
          skip_update = true;
        } else if (path2.endsWith("favicon.ico")) {
          skip_update = true;
        } else if (path2.endsWith("win_service.jar") && Common.machine_is_x()) {
          skip_update = true;
        } else if (path2.toUpperCase().indexOf("/USERS/") >= 0) {
          skip_update = true;
        } 
        if (skip_update) {
          out.close();
          out = null;
          File f = new File(String.valueOf(sourcePath) + path2 + ext);
          if (f.getName().endsWith("_tmp"))
            f.delete(); 
        } 
        if (out != null) {
          out.setLength(0L);
          if (path2.indexOf("WebInterface") < 0)
            Common.updateOSXInfo(String.valueOf(sourcePath) + path2 + ext); 
        } 
        while (bytes_read >= 0) {
          bytes_read = zin.read(b);
          if (bytes_read > 0 && out != null)
            out.write(b, 0, bytes_read); 
        } 
        boolean ok1 = false;
        boolean ok2 = false;
        if (out != null) {
          out.close();
          (new File(String.valueOf(sourcePath) + path2 + ext)).setLastModified(entry.getTime());
          Log.log("UPDATE", 0, "Updating:" + sourcePath + path2 + "    to:" + Common.all_but_last(String.valueOf(destPath) + path2));
          if ((new File(Common.all_but_last(String.valueOf(destPath) + path2))).mkdirs() && path2.indexOf("WebInterface") < 0)
            Common.updateOSXInfo(Common.all_but_last(String.valueOf(destPath) + path2)); 
          if (!ext.equals("")) {
            ok1 = (new File(String.valueOf(sourcePath) + path2)).renameTo(new File(String.valueOf(destPath) + path2));
            ok2 = !(new File(String.valueOf(sourcePath) + path2)).exists();
          } 
        } 
        if (ok1 || ok2 || ext.equals("") || out == null) {
          if (out != null) {
            if (!ext.equals(""))
              (new File(String.valueOf(sourcePath) + path2 + ext)).renameTo(new File(String.valueOf(sourcePath) + path2)); 
            if (path2.indexOf("WebInterface") < 0)
              Common.updateOSXInfo(String.valueOf(sourcePath) + path2); 
            Log.log("UPDATE", 3, "OK");
            (new File(String.valueOf(sourcePath) + path2)).setLastModified(entry.getTime());
          } 
          continue;
        } 
        Log.log("UPDATE", 0, "DEFERRED:" + sourcePath + path2 + "    to:" + Common.all_but_last(String.valueOf(destPath) + path2));
        if ((new File(Common.all_but_last(String.valueOf(sourcePath) + "UpdateTemp/" + path2))).mkdirs() && path2.indexOf("WebInterface") < 0)
          Common.updateOSXInfo(Common.all_but_last(String.valueOf(sourcePath) + "UpdateTemp/" + path2)); 
        (new File(String.valueOf(sourcePath) + path2 + "_tmp")).renameTo(new File(String.valueOf(sourcePath) + "UpdateTemp/" + path2));
        if (path2.indexOf("WebInterface") < 0)
          Common.updateOSXInfo(String.valueOf(sourcePath) + "UpdateTemp/" + path2); 
        Properties p = new Properties();
        p.put("source", String.valueOf(sourcePath) + "UpdateTemp/" + path2);
        p.put("dest", String.valueOf(sourcePath) + path2);
        p.put("backup", (new File(String.valueOf(destPath) + path2)).getCanonicalPath());
        errors.addElement(p);
        Log.log("UPDATE", 3, "Updating for later:" + sourcePath + "UpdateTemp/" + path2);
      } 
      Common.updateOSXInfo(String.valueOf(sourcePath) + "WebInterface/", "-R");
    } catch (Exception e) {
      ZipEntry entry;
      Log.log("UPDATE", 1, (Exception)entry);
      zin.close();
      return false;
    } 
    zin.close();
    return true;
  }
  
  public String getFileHTTP(String localPath, String name, String ext, String url, String paths, String username, String password) throws Exception {
    Properties config = new Properties();
    config.put("user_dmz", "(current_server)");
    URLConnection urlc = URLConnection.openConnection(new VRL("https://www.crushftp.com/"), config);
    String auth = "Basic " + Base64.encodeBytes((String.valueOf(username) + ":" + password).getBytes());
    urlc.setRequestProperty("Authorization", auth);
    urlc.setRequestMethod("GET");
    urlc.setDoOutput(true);
    urlc.getOutputStream().write(("command=downloadAsZip&path_shortening=false&paths=" + paths).getBytes("UTF8"));
    this.di = urlc.getInputStream();
    this.updateCurrentLoc = 0L;
    int bytes_read = 0;
    byte[] temp_array = new byte[32768];
    RandomAccessFile of_stream = null;
    (new File(String.valueOf(localPath) + name + "_new." + ext)).delete();
    String out_file = String.valueOf(name) + "_new." + ext;
    of_stream = new RandomAccessFile(String.valueOf(localPath) + out_file, "rw");
    of_stream.setLength(0L);
    bytes_read = this.di.read(temp_array);
    this.updateCurrentLoc += bytes_read;
    while (bytes_read > 0 && !this.stopNow) {
      of_stream.write(temp_array, 0, bytes_read);
      bytes_read = this.di.read(temp_array);
      if (bytes_read > 0)
        this.updateCurrentLoc += bytes_read; 
    } 
    this.di.close();
    if (this.stopNow)
      return "cancelled"; 
    this.updateCurrentLoc = this.updateMaxSize;
    return "";
  }
}
