package crushftp.handlers;

import crushftp.server.ServerStatus;
import crushftp.server.Worker;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URL;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PreviewWorker implements Serializable {
  private static final long serialVersionUID = 1L;
  
  Object convertSync = new Object();
  
  Thread dirScannerThread = null;
  
  public Vector convertItems = new Vector();
  
  int conversionThreadsRunning = 0;
  
  Vector messages = null;
  
  public Properties badFiles = new Properties();
  
  public Properties prefs = null;
  
  public boolean abort = false;
  
  public PreviewWorker(Properties prefs) {
    this.prefs = prefs;
    try {
      Common.updateOSXInfo(String.valueOf((new File(String.valueOf(ServerStatus.SG("previews_path")) + "Preview")).getCanonicalPath()) + File.pathSeparator, "-R");
    } catch (IOException iOException) {}
  }
  
  public static void getDefaults(Properties prefs) {
    Vector preview_config = new Vector();
    Properties p = new Properties();
    p.put("preview_enabled", "false");
    p.put("preview_debug", "true");
    p.put("preview_scan_interval", "10");
    p.put("preview_conversion_threads", "1");
    p.put("preview_file_extensions", ".jpg, .jpeg, .gif, .png, .bmp, .pdf, .psd, .tif, .tiff, .zip");
    if (Common.machine_is_x_10_5_plus())
      p.put("preview_file_extensions", "*"); 
    Vector sizes = new Vector();
    sizes.addElement("80x80");
    sizes.addElement("160x160");
    sizes.addElement("800x800");
    p.put("preview_sizes", sizes);
    p.put("preview_subdirectories", "true");
    p.put("preview_reverseSubdirectories", "true");
    p.put("preview_frames", "1");
    p.put("preview_movie_info_command_line", "");
    p.put("preview_exif", "false");
    p.put("preview_exif_get_command_line", "exiftool -S %src%");
    p.put("preview_exif_set_command_line", "exiftool -overwrite_original_in_place -%key%=%val% %src%");
    p.put("preview_wait_timeout", "600");
    if (Common.machine_is_windows()) {
      p.put("preview_working_dir", "C:\\Program Files\\ImageMagick-6.5.0-Q16\\");
    } else {
      p.put("preview_working_dir", "");
    } 
    p.put("preview_environment", "");
    if (Common.machine_is_windows()) {
      p.put("preview_command_line", "convert.exe -colorspace RGB -strip -alpha off -geometry %width%x%width% -quality 75 %src%[0] %dst%");
      p.put("preview_environment", "MAGICK_HOME_OFF=./;DYLD_LIBRARY_PATH_OFF=./lib");
    } else if (Common.machine_is_x_10_5_plus()) {
      p.put("preview_command_line", "../../MacOS/qlmanage_wrapper.sh %width% %previews%temp/%random%/ %src% %dst%");
      p.put("preview_file_extensions", ".jpg, .jpeg, .gif, .png, .bmp, .pdf, .psd, .tif, .tiff, .zip, *.txt, *.rtf, *.doc, *.docx, *.xls, *.xlsx, *.pdf");
    } else if (Common.machine_is_x()) {
      p.put("preview_command_line", "sips -Z %width% -s format jpeg %src% -m /System/Library/ColorSync/Profiles/Generic\\ RGB\\ Profile.icc --out %dst%");
    } else {
      p.put("preview_command_line", "convert -colorspace RGB -strip -alpha off -geometry %width%x%width% -quality 75 %src%[0] %dst%");
      p.put("preview_environment", "MAGICK_HOME=/Applications/ImageMagick-6.3.6/;DYLD_LIBRARY_PATH=/Applications/ImageMagick-6.3.6/lib;PATH=/opt/local/bin:/opt/local/sbin:/bin");
    } 
    p.put("preview_folder_list", "");
    preview_config.addElement(p);
    prefs.put("preview_configs", preview_config);
  }
  
  public void run(Properties info) {
    if (!this.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true"))
      return; 
    if (this.abort)
      return; 
    if (this.dirScannerThread == null) {
      if (Common.machine_is_x_10_5_plus()) {
        try {
          Common.exec(new String[] { "chmod", "+x", (new File(String.valueOf(System.getProperty("crushftp.home")) + "qlmanage_wrapper.sh")).getCanonicalPath() });
        } catch (Exception e) {
          Log.log("PREVIEW", 1, e);
        } 
        try {
          Common.exec(new String[] { "chmod", "+x", (new File(String.valueOf(System.getProperty("crushftp.home")) + "pcastaction_wrapper.sh")).getCanonicalPath() });
        } catch (Exception e) {
          Log.log("PREVIEW", 1, e);
        } 
      } 
      msg("Started directory scanner.");
      null.Runner runner = new null.Runner(this);
      if (this.dirScannerThread == null) {
        this.dirScannerThread = new Thread(runner);
        this.dirScannerThread.setName("Preview:dirScanner");
        this.dirScannerThread.setPriority(1);
        this.dirScannerThread.start();
      } 
    } 
    if (info != null && info.getProperty("action", "").equals("event")) {
      Vector items = (Vector)info.get("items");
      msg("item list size:" + items.size());
      msg("items:" + items);
      for (int x = 0; x < items.size(); x++) {
        Properties p = items.elementAt(x);
        this.convertItems.addElement(p.clone());
      } 
    } 
  }
  
  public boolean checkExtension(String name, File sourceFile) {
    String ext = name.substring(name.lastIndexOf(".")).toUpperCase();
    String[] exts = this.prefs.getProperty("preview_file_extensions", "").toUpperCase().split(",");
    boolean ok_ext = false;
    boolean ok_size = true;
    for (int x = 0; x < exts.length; x++) {
      if (exts[x].indexOf(ext) >= 0) {
        ok_ext = true;
        if (exts[x].indexOf(":") >= 0) {
          long size = Long.parseLong(exts[x].substring(exts[x].indexOf(":") + 1)) * 1024L * 1024L;
          if (sourceFile.length() > size)
            ok_size = false; 
        } 
        break;
      } 
    } 
    if (this.prefs.getProperty("preview_file_extensions", "").equals("*"))
      ok_ext = true; 
    if (!ok_ext)
      return false; 
    if (!ok_size) {
      msg("Image too large, skipping:" + sourceFile.getPath());
      return false;
    } 
    return ok_ext;
  }
  
  public synchronized boolean doConvert(File sourceFile, File destFile, boolean multiThread, Properties info) {
    if ((new File(String.valueOf(System.getProperty("crushftp.home")) + "reset_preview_bad_files")).exists()) {
      (new File(String.valueOf(System.getProperty("crushftp.home")) + "reset_preview_bad_files")).delete();
      this.badFiles.clear();
    } 
    String name = sourceFile.getName();
    if (name.indexOf(".") >= 0 && !name.startsWith(".")) {
      if (!checkExtension(name, sourceFile))
        return false; 
      if (destFile == null)
        destFile = sourceFile; 
      String sourcePath = sourceFile.getPath();
      String destPath = null;
      try {
        destPath = destFile.getCanonicalPath();
      } catch (Exception exception) {}
      destPath = getDestPath(destPath);
      destPath = String.valueOf(destPath) + destFile.getName() + File.separator;
      (new File(destPath)).mkdirs();
      Common.updateOSXInfo(destPath, "-R");
      if (!(new File(String.valueOf(destPath) + "/p1/1.jpg")).exists() || (new File(sourcePath)).lastModified() != (new File(destPath)).lastModified())
        try {
          if (this.badFiles.containsKey((new File(sourcePath)).getCanonicalPath()) && this.badFiles.getProperty((new File(sourcePath)).getCanonicalPath()).equals((new StringBuffer(String.valueOf((new File((new File(sourcePath)).getCanonicalPath())).lastModified()))).toString())) {
            msg("Skipping file that can't be converted:" + sourcePath);
            return false;
          } 
          msg("Converting: " + sourcePath + "      to: " + destPath + "  exists:" + (new File(String.valueOf(destPath) + "/p1/1.jpg")).exists() + "  modified:" + (new File(sourcePath)).lastModified() + " vs " + (new File(destPath)).lastModified());
          convert(sourcePath, destPath, multiThread, info);
          return true;
        } catch (Exception exception) {} 
    } 
    return false;
  }
  
  public void msg(String s) {
    if (this.messages != null) {
      this.messages.addElement("Preview:" + s);
    } else if (this.prefs.getProperty("preview_debug").equals("true")) {
      Log.log("PREVIEW", 0, "Preview:" + s);
    } 
  }
  
  public void msg(Exception e) {
    if (this.messages != null) {
      this.messages.addElement(e);
    } else if (this.prefs.getProperty("preview_debug").equals("true")) {
      Log.log("PREVIEW", 0, e);
    } 
  }
  
  public static String getDestPath(String path) {
    String destPath = String.valueOf((new File(path)).getParentFile().getPath()) + "/";
    try {
      destPath = String.valueOf((new File(path)).getCanonicalFile().getParentFile().getPath()) + "/";
    } catch (Exception exception) {}
    if (destPath.indexOf(":") >= 0 && destPath.toUpperCase().startsWith("C:")) {
      destPath = destPath.substring(destPath.indexOf(":") + 1);
    } else if (destPath.indexOf(":") >= 0) {
      destPath = "/_-_" + destPath.substring(0, destPath.indexOf(":")) + "_-_" + destPath.substring(destPath.indexOf(":") + 1);
    } else if (Common.machine_is_windows() && destPath.replace('\\', '/').startsWith("//")) {
      destPath = "/_UNC_" + destPath.substring(2);
    } 
    destPath = destPath.replace('\\', '/');
    return String.valueOf(ServerStatus.SG("previews_path")) + "Preview" + destPath;
  }
  
  public String get(String key) {
    return this.prefs.getProperty(key);
  }
  
  public boolean convert(String srcFile, String destFile, boolean multiThread, Properties info) {
    try {
      msg("Creating thumbnail for:" + srcFile);
      synchronized (this.convertSync) {
        while (this.conversionThreadsRunning >= Integer.parseInt(get("preview_conversion_threads")))
          Thread.sleep(500L); 
        this.conversionThreadsRunning++;
      } 
      if (!multiThread) {
        (new null.multiConvert(this, srcFile, destFile, info)).run();
      } else {
        Worker.startWorker(new null.multiConvert(this, srcFile, destFile, info), "Preview:converting " + srcFile);
      } 
    } catch (Exception e) {
      msg(e);
    } 
    return true;
  }
  
  class discarder implements Runnable {
    InputStream in;
    
    final PreviewWorker this$0;
    
    public discarder(PreviewWorker this$0, InputStream in) {
      this.this$0 = this$0;
      this.in = null;
      this.in = in;
    }
    
    public void run() {
      BufferedReader br = null;
      try {
        br = new BufferedReader(new InputStreamReader(this.in));
        String data = "";
        while ((data = br.readLine()) != null)
          this.this$0.msg(data); 
      } catch (Exception e) {
        Log.log("PREVIEW", 2, e);
      } 
      try {
        br.close();
      } catch (Exception exception) {}
    }
  }
  
  public String getZipEntries(String srcFile) throws Exception {
    if (!srcFile.toUpperCase().endsWith(".ZIP"))
      return ""; 
    StringBuffer entries = new StringBuffer();
    ZipInputStream zin = new ZipInputStream(new FileInputStream(new File(srcFile)));
    ZipEntry entry = null;
    int loops = 0;
    while ((entry = zin.getNextEntry()) != null) {
      String path = Common.dots(entry.getName().replace('\\', '/'));
      entries.append(String.valueOf(path) + "\r\n");
      loops++;
      if (loops > 100)
        break; 
    } 
    zin.close();
    return entries.toString();
  }
  
  public void convertCommandLine(String srcFile, String destFile) {
    if (Common.machine_is_windows() && destFile.length() > 245) {
      this.badFiles.put(srcFile, (new StringBuffer(String.valueOf((new File(srcFile)).lastModified()))).toString());
      return;
    } 
    Vector sizes = (Vector)this.prefs.get("preview_sizes");
    try {
      (new File(String.valueOf(destFile) + File.separator)).mkdirs();
      Common.updateOSXInfo(String.valueOf(destFile) + File.separator, "-R");
      RandomAccessFile index = new RandomAccessFile(String.valueOf(destFile) + File.separator + "index.txt", "rw");
      byte[] b = new byte[(int)index.length()];
      index.readFully(b);
      index.seek(index.length());
      if ((new String(b)).toUpperCase().indexOf((new File(srcFile)).getName().toUpperCase()) < 0) {
        index.write((String.valueOf((new File(srcFile)).getName()) + "\r\n").getBytes());
        index.write(getZipEntries(srcFile).getBytes());
      } 
      index.close();
    } catch (Exception e) {
      msg(e);
    } 
    String[] envp = get("preview_environment").split(";");
    if (envp.length == 0 || (envp.length == 1 && envp[0].trim().length() == 0))
      envp = (String[])null; 
    float duration = 0.0F;
    float loops = Integer.parseInt(this.prefs.getProperty("preview_frames", "1"));
    if (loops == 0.0F)
      loops = 1.0F; 
    if (loops > 1.0F) {
      String s = this.prefs.getProperty("preview_movie_info_command_line", "");
      if (Common.machine_is_windows() && !s.toUpperCase().startsWith("CMD /"))
        s = "CMD /C " + s.trim(); 
      String[] args = s.split(" ");
      msg("Getting duration of movie:" + change_vars(s, srcFile, destFile, 0.0F, 0));
      for (int l = 0; l < args.length; l++)
        args[l] = change_vars(args[l], srcFile, destFile, 0.0F, 0); 
      File f = new File(get("preview_working_dir"));
      if (get("preview_working_dir").equals(""))
        f = new File(System.getProperty("crushftp.home")); 
      try {
        Process proc = Runtime.getRuntime().exec(args, envp, f);
        BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        duration = getDuration(br1);
        if (duration < 0.0F)
          duration = getDuration(br2); 
        if (duration < 0.0F)
          duration = 0.0F; 
        Worker.startWorker(new discarder(this, proc.getInputStream()), "Discard input stream:" + srcFile);
        Worker.startWorker(new discarder(this, proc.getErrorStream()), "Discard error stream:" + srcFile);
        Thread.sleep(1000L);
        proc.destroy();
      } catch (Exception e) {
        msg(e);
      } 
      msg("Duration:" + duration);
    } 
    float step = duration / loops;
    float loc = 0.0F;
    for (int loop = 1; loop <= loops; loop++) {
      loc += step;
      for (int xx = sizes.size() - 1; xx >= 0; xx--) {
        String size = sizes.elementAt(xx).toString();
        String width = size.substring(0, size.indexOf("x"));
        String height = size.substring(size.indexOf("x") + 1);
        String page = "p" + loop;
        try {
          String command_line = get("preview_command_line");
          if (Common.machine_is_windows() && !command_line.toUpperCase().startsWith("CMD /"))
            command_line = "CMD /C " + command_line.trim(); 
          command_line = Common.replace_str(command_line, "\\ ", "þ");
          String[] convert = command_line.split(" ");
          String[] convertCmd = (String[])null;
          convertCmd = new String[convert.length];
          String command_line_result = "";
          String altSrcFile = srcFile;
          for (int x = 0; x < convert.length; x++) {
            String s = convert[x];
            s = s.replace('þ', ' ');
            s = Common.replace_str(s, "%width%", width);
            s = Common.replace_str(s, "%height%", height);
            if (xx < sizes.size() - 1)
              altSrcFile = String.valueOf(destFile) + File.separator + page + File.separator + sizes.size() + ".jpg"; 
            s = change_vars(s, altSrcFile, String.valueOf(destFile) + File.separator + page + File.separator + (xx + 1) + ".jpg", loc, 0);
            s = Common.url_decode(s);
            convertCmd[x] = s;
            command_line_result = String.valueOf(command_line_result) + s + " ";
          } 
          (new File(String.valueOf(destFile) + File.separator + page + File.separator)).mkdirs();
          Common.updateOSXInfo(String.valueOf(destFile) + File.separator + page + File.separator, "-R");
          msg(command_line_result);
          if (loops > 1.0F && xx < sizes.size() - 1) {
            Common.copy(altSrcFile, String.valueOf(destFile) + File.separator + page + File.separator + (xx + 1) + ".jpg", true);
            (new File(String.valueOf(destFile) + File.separator + page + File.separator + (xx + 1) + ".jpg")).setLastModified((new File(srcFile)).lastModified());
          } else {
            File f = new File(get("preview_working_dir"));
            if (get("preview_working_dir").equals(""))
              f = null; 
            Process proc = Runtime.getRuntime().exec(convertCmd, envp, f);
            Thread.sleep(1000L);
            Worker.startWorker(new discarder(this, proc.getErrorStream()), "Preview:ErrorStream:" + command_line_result);
            Worker.startWorker(new discarder(this, proc.getInputStream()), "Preview:InputStream:" + command_line_result);
            Properties exitInfo = new Properties();
            Worker.startWorker(new Runnable(this, exitInfo, proc) {
                  final PreviewWorker this$0;
                  
                  private final Properties val$exitInfo;
                  
                  private final Process val$proc;
                  
                  public void run() {
                    this.val$exitInfo.put("thread", Thread.currentThread());
                    try {
                      this.val$exitInfo.put("exitCode", (new StringBuffer(String.valueOf(this.val$proc.waitFor()))).toString());
                    } catch (Exception e) {
                      this.val$exitInfo.put("exitCode", "50");
                      this.val$proc.destroy();
                    } 
                    try {
                      synchronized (this.val$exitInfo) {
                        this.val$exitInfo.remove("thread");
                      } 
                      Thread.sleep(1000L);
                    } catch (Exception exception) {}
                    try {
                      this.val$proc.getErrorStream().close();
                      this.val$proc.getInputStream().close();
                    } catch (Exception exception) {}
                  }
                });
            long start = System.currentTimeMillis();
            if (this.prefs.getProperty("preview_wait_timeout", "600").equals(""))
              this.prefs.put("preview_wait_timeout", "121"); 
            int timeout = Integer.parseInt(this.prefs.getProperty("preview_wait_timeout", "600"));
            while (exitInfo.getProperty("exitCode", "").equals("")) {
              Thread.sleep(100L);
              synchronized (exitInfo) {
                if (System.currentTimeMillis() - start >= (timeout * 1000) || !this.prefs.getProperty("preview_enabled").equalsIgnoreCase("true") || this.abort) {
                  Thread tt = (Thread)exitInfo.get("thread");
                  tt.interrupt();
                  break;
                } 
              } 
            } 
            if (!exitInfo.getProperty("exitCode", "").equals("0") || !(new File(String.valueOf(destFile) + File.separator + page + File.separator + (xx + 1) + ".jpg")).exists()) {
              this.badFiles.put(srcFile, (new StringBuffer(String.valueOf((new File(srcFile)).lastModified()))).toString());
              msg("Preview cannot be generated, adding to list of bad files:" + srcFile);
              break;
            } 
            if (srcFile.toUpperCase().endsWith(".PSD") || srcFile.toUpperCase().endsWith(".PDF"))
              if (!(new File(destFile)).exists()) {
                for (int i = 1; i < 500; ) {
                  if ((new File(String.valueOf(destFile.substring(0, destFile.lastIndexOf("."))) + "-" + i + destFile.substring(destFile.lastIndexOf(".")))).exists()) {
                    (new File(String.valueOf(destFile.substring(0, destFile.lastIndexOf("."))) + "-" + i + destFile.substring(destFile.lastIndexOf(".")))).delete();
                    i++;
                  } 
                  break;
                } 
                (new File(String.valueOf(destFile.substring(0, destFile.lastIndexOf("."))) + "-0" + destFile.substring(destFile.lastIndexOf(".")))).renameTo(new File(destFile));
              }  
            (new File(String.valueOf(destFile) + File.separator + page + File.separator + (xx + 1) + ".jpg")).setLastModified((new File(srcFile)).lastModified());
          } 
        } catch (Exception e) {
          msg(e);
        } 
      } 
    } 
    (new File(destFile)).setLastModified((new File(srcFile)).lastModified());
  }
  
  public float getDuration(BufferedReader br) throws IOException {
    Properties status = new Properties();
    Runnable r = new Runnable(this, br, status) {
        final PreviewWorker this$0;
        
        private final BufferedReader val$br;
        
        private final Properties val$status;
        
        public void run() {
          try {
            float duration = -1.0F;
            String data = "";
            int lines = 0;
            while ((data = this.val$br.readLine()) != null) {
              lines++;
              if (lines <= 3)
                try {
                  duration = Float.parseFloat(data.trim());
                  this.val$status.put("duration", (new StringBuffer(String.valueOf(duration))).toString());
                  break;
                } catch (NumberFormatException numberFormatException) {} 
              data = data.toUpperCase();
              this.this$0.msg(data);
              if (data.indexOf("DURATION") >= 0) {
                data = data.substring(data.indexOf(":") + 1);
                String num = "";
                boolean inNum = false;
                for (int c = 0; c < data.length(); c++) {
                  if ((data.charAt(c) >= '0' && data.charAt(c) <= '9') || data.charAt(c) == '.' || data.charAt(c) == ':') {
                    num = String.valueOf(num) + data.charAt(c);
                    inNum = true;
                  } else if (inNum) {
                    break;
                  } 
                } 
                String[] date = num.split(":");
                duration = Float.parseFloat(date[0]) * 3600.0F + Float.parseFloat(date[1]) * 60.0F + Float.parseFloat(date[2]) * 1.0F;
                this.val$status.put("duration", (new StringBuffer(String.valueOf(duration))).toString());
              } 
            } 
          } catch (Exception e) {
            this.this$0.msg(e);
          } 
        }
      };
    Thread t = new Thread(r);
    t.setName(String.valueOf(Thread.currentThread().getName()) + ":Getting movie duration.");
    t.start();
    try {
      t.join(30000L);
    } catch (Exception e) {
      msg(e);
    } 
    t.interrupt();
    return Float.parseFloat(status.getProperty("duration", "-1.0"));
  }
  
  public String change_vars(String s, String src, String dest, float time, int intervals) {
    intervals++;
    s = Common.replace_str(s, "%src%", src);
    s = Common.replace_str(s, "%dest%", dest);
    s = Common.replace_str(s, "%dst%", dest);
    s = Common.replace_str(s, "%time%", (new StringBuffer(String.valueOf((int)time))).toString());
    s = Common.replace_str(s, "%random%", Common.makeBoundary(4));
    try {
      s = Common.replace_str(s, "%previews%", String.valueOf((new File(String.valueOf(ServerStatus.SG("previews_path")) + "/Preview")).getCanonicalPath()) + "/");
    } catch (IOException e) {
      Log.log("PREVIEW", 2, e);
    } 
    if (intervals < 3)
      s = change_vars(s, src, dest, time, intervals); 
    return s;
  }
  
  public void recurseConvert(String real_path, int depth, int max_depth) {
    if (depth > max_depth)
      return; 
    File f = new File(real_path);
    try {
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File(real_path);
    } catch (Exception exception) {}
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; files != null && x < files.length; x++) {
        File f2 = new File(String.valueOf(real_path) + files[x]);
        if (!Common.isSymbolicLink(f2.getAbsolutePath())) {
          if (f2.isDirectory()) {
            if (f2.getName().indexOf("/") >= 0 || f2.getName().indexOf("\\") >= 0) {
              msg("Skipping invalid folder name:" + f2.getName());
            } else {
              recurseConvert(String.valueOf(real_path) + files[x] + "/", depth + 1, max_depth);
            } 
          } else {
            doConvert(f2, null, true, new Properties());
          } 
          if (!this.prefs.getProperty("preview_enabled").equalsIgnoreCase("true") || this.abort)
            return; 
        } 
      } 
    } else {
      if (!this.prefs.getProperty("preview_enabled").equalsIgnoreCase("true") || this.abort)
        return; 
      doConvert(f, null, true, new Properties());
    } 
  }
  
  public void reverseRecurseConvert(String real_path, int depth, int max_depth) {
    if (depth > max_depth)
      return; 
    File f = new File(real_path);
    try {
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File(real_path);
    } catch (Exception exception) {}
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; files != null && x < files.length; x++) {
        if (!this.prefs.getProperty("preview_enabled").equalsIgnoreCase("true") || this.abort)
          return; 
        File f2 = new File(String.valueOf(real_path) + files[x]);
        if (!Common.isSymbolicLink(f2.getAbsolutePath()))
          if (f2.isDirectory())
            if ((new File(String.valueOf(real_path) + files[x] + "/index.txt")).exists() && (new File(String.valueOf(real_path) + files[x] + "/p1")).exists()) {
              File home = new File(String.valueOf(ServerStatus.SG("previews_path")) + "/Preview");
              boolean ok = true;
              try {
                if (f2.getCanonicalPath().startsWith(home.getCanonicalPath())) {
                  String checkPath = f2.getCanonicalPath().substring(home.getCanonicalPath().length()).replace('\\', '/');
                  if (Common.machine_is_windows()) {
                    String driveLetter = checkPath.split("/")[1];
                    if (driveLetter.startsWith("_-_") && driveLetter.endsWith("_-_")) {
                      driveLetter = (new StringBuffer(String.valueOf(driveLetter.charAt(3)))).toString();
                      checkPath = String.valueOf(driveLetter) + ":" + checkPath.substring(checkPath.indexOf("/", 6));
                    } else if (driveLetter.startsWith("_UNC_")) {
                      driveLetter = "//";
                      checkPath = String.valueOf(driveLetter) + checkPath.substring(6);
                      Log.log("PREVIEW", 3, "Checking if file exists:" + checkPath);
                    } 
                  } 
                  if (!(new File(checkPath)).exists())
                    ok = false; 
                } 
              } catch (Exception exception) {}
              if (!ok) {
                if (!this.prefs.getProperty("preview_enabled").equalsIgnoreCase("true") || this.abort)
                  return; 
                msg("Deleting old thubmnail for deleted file:" + real_path + files[x] + "/");
                Common.recurseDelete(String.valueOf(real_path) + files[x] + "/", false);
              } 
            } else {
              reverseRecurseConvert(String.valueOf(real_path) + files[x] + "/", depth + 1, max_depth);
            }   
      } 
    } 
  }
  
  public Properties getExifInfo(String srcFile, String destFile) throws Exception {
    Properties metaInfo = new Properties();
    if (get("preview_exif_get_command_line") != null && !get("preview_exif_get_command_line").trim().equals("")) {
      msg("Making " + srcFile + " info.xml exif file...");
      BufferedReader br = null;
      Process proc = null;
      try {
        String[] command = (String[])null;
        String[] envp = get("preview_environment").split(";");
        if (Common.machine_is_windows()) {
          command = ("CMD /C " + get("preview_exif_get_command_line")).split(" ");
        } else {
          command = get("preview_exif_get_command_line").split(" ");
        } 
        for (int x = 0; x < command.length; x++) {
          if (command[x].equalsIgnoreCase("%SRC%"))
            command[x] = srcFile; 
        } 
        if (envp.length == 0 || (envp.length == 1 && envp[0].trim().length() == 0))
          envp = (String[])null; 
        File f = null;
        if (get("preview_working_dir").equals("")) {
          f = null;
        } else {
          f = new File(get("preview_working_dir"));
        } 
        proc = Runtime.getRuntime().exec(command, envp, f);
        br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        Worker.startWorker(new discarder(this, proc.getErrorStream()));
        String data = "";
        int lines = 0;
        while ((data = br.readLine()) != null) {
          lines++;
          if (data.indexOf(": ") >= 0) {
            String key = data.substring(0, data.indexOf(":")).trim().toLowerCase();
            key = key.replaceAll(" ", "_");
            if (!key.startsWith("0x") && key.indexOf("[") < 0 && key.indexOf("]") < 0)
              try {
                Integer.parseInt(key);
              } catch (Exception e) {
                metaInfo.put(key, data.substring(data.indexOf(":") + 1).trim());
                Log.log("PREVIEW", 2, data);
              }  
          } 
        } 
        br.close();
        proc.waitFor();
      } catch (Exception e) {
        msg(e);
      } 
    } 
    if (metaInfo.size() > 0)
      ServerStatus.thisObj.common_code.writeXMLObject(String.valueOf(destFile) + "info.xml", metaInfo, "EXIF"); 
    return metaInfo;
  }
  
  public Properties setExifInfo(String srcFile, String destFile, String exif_key, String exif_val) {
    Properties metaInfo = new Properties();
    if (get("preview_exif_set_command_line") != null && !get("preview_exif_set_command_line").trim().equals("")) {
      msg("Updating " + srcFile + " with exif key change.");
      BufferedReader br = null;
      Process proc = null;
      for (int xx = 0; xx < (exif_val.split(",")).length; xx++) {
        try {
          srcFile = (new File(srcFile)).getCanonicalPath();
          String[] command = (String[])null;
          String[] envp = get("preview_environment").split(";");
          if (Common.machine_is_windows()) {
            command = ("CMD /C " + get("preview_exif_set_command_line")).split(" ");
          } else {
            command = get("preview_exif_set_command_line").split(" ");
          } 
          for (int x = 0; x < command.length; x++) {
            if (command[x].equalsIgnoreCase("%SRC%"))
              command[x] = srcFile; 
            if (xx == 0 && command[x].toUpperCase().indexOf("%KEY%") >= 0)
              command[x] = String.valueOf(command[x].substring(0, command[x].toUpperCase().indexOf("%KEY%"))) + exif_key + command[x].substring(command[x].toUpperCase().indexOf("%KEY%") + "%KEY%".length()); 
            if (xx > 0 && command[x].toUpperCase().indexOf("%KEY%") >= 0)
              command[x] = String.valueOf(command[x].substring(0, command[x].toUpperCase().indexOf("%KEY%"))) + exif_key + "+" + command[x].substring(command[x].toUpperCase().indexOf("%KEY%") + "%KEY%".length()); 
            if (command[x].toUpperCase().indexOf("%VAL%") >= 0)
              command[x] = String.valueOf(command[x].substring(0, command[x].toUpperCase().indexOf("%VAL%"))) + exif_val.split(",")[xx].trim() + command[x].substring(command[x].toUpperCase().indexOf("%VAL%") + "%VAL%".length()); 
          } 
          if (envp.length == 0 || (envp.length == 1 && envp[0].trim().length() == 0))
            envp = (String[])null; 
          File f = null;
          if (get("preview_working_dir").equals("")) {
            f = null;
          } else {
            f = new File(get("preview_working_dir"));
          } 
          proc = Runtime.getRuntime().exec(command, envp, f);
          br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          Worker.startWorker(new discarder(this, proc.getErrorStream()));
          String data = "";
          while ((data = br.readLine()) != null)
            msg(data); 
          br.close();
          proc.waitFor();
          metaInfo = getExifInfo(srcFile, destFile);
        } catch (Exception e) {
          msg(e);
        } 
      } 
    } 
    return metaInfo;
  }
}
