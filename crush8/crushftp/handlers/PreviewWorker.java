package crushftp.handlers;

import com.crushftp.client.Common;
import com.crushftp.client.File_S;
import com.crushftp.client.File_U;
import com.crushftp.client.GenericClient;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import crushftp.db.SearchHandler;
import crushftp.server.ServerStatus;
import crushftp.server.daemon.ServerBeat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.bind.DatatypeConverter;

public class PreviewWorker implements Serializable {
  private static final long serialVersionUID = 1L;
  
  Object convertSync = new Object();
  
  Object convertSync2 = new Object();
  
  Thread dirScannerThread = null;
  
  public Vector convertItems = new Vector();
  
  int conversionThreadsRunning = 0;
  
  Vector messages = null;
  
  public Properties badFiles = new Properties();
  
  public Properties prefs = null;
  
  public boolean abort = false;
  
  public static Properties byte_validation = new Properties();
  
  public static Properties exif_cache = new Properties();
  
  public PreviewWorker(Properties prefs) {
    this.prefs = prefs;
    try {
      Common.updateOSXInfo_U(String.valueOf((new File(String.valueOf(ServerStatus.SG("previews_path")) + "Preview")).getCanonicalPath()) + File.pathSeparator, "-R");
    } catch (IOException iOException) {}
    byte_validation.put("jpg0", new byte[] { -1, -40, -1, -37 });
    byte_validation.put("jpg1", new byte[] { -1, -40, -1, -32 });
    byte_validation.put("jpg2", new byte[] { -1, -40, -1, -31 });
    byte_validation.put("jpg3", new byte[] { -1, -40, -1, -18 });
    byte_validation.put("jpg4", new byte[] { -1, -40, -1, -30 });
    byte_validation.put("jpg5", new byte[] { -1, -40, -1, -19 });
    byte_validation.put("jpeg0", new byte[] { -1, -40, -1, -37 });
    byte_validation.put("jpeg1", new byte[] { -1, -40, -1, -32 });
    byte_validation.put("jpeg2", new byte[] { -1, -40, -1, -31 });
    byte_validation.put("jpeg3", new byte[] { -1, -40, -1, -18 });
    byte_validation.put("jpeg4", new byte[] { -1, -40, -1, -30 });
    byte_validation.put("jpeg5", new byte[] { -1, -40, -1, -19 });
    byte_validation.put("gif0", new byte[] { 71, 73, 70, 56, 55, 97 });
    byte_validation.put("gif1", new byte[] { 71, 73, 70, 56, 57, 97 });
    byte_validation.put("png0", new byte[] { -119, 80, 78, 71, 13, 10, 26, 10 });
    byte_validation.put("bmp0", new byte[] { 66, 77 });
    byte_validation.put("pdf0", new byte[] { 37, 80, 68, 70 });
    byte_validation.put("psd0", new byte[] { 56, 66, 80, 83 });
    byte_validation.put("tif0", new byte[] { 73, 73, 42 });
    byte_validation.put("tif1", new byte[] { 77, 77, 42 });
    byte_validation.put("tiff0", new byte[] { 73, 73, 42 });
    byte_validation.put("tiff1", new byte[] { 77, 77, 42 });
    byte_validation.put("cr20", new byte[] { 73, 73, 42, 16, 67, 82 });
    byte_validation.put("ai0", new byte[] { 37, 33, 80, 83 });
    byte_validation.put("ai1", new byte[] { 37, 80, 68, 70 });
    byte_validation.put("eps0", new byte[] { 37, 33, 80, 83 });
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
    if (!ServerBeat.current_master)
      if (ServerStatus.BG("single_preview_serverbeat"))
        return;  
    if (!this.prefs.getProperty("preview_enabled", "false").equalsIgnoreCase("true"))
      return; 
    if (this.abort)
      return; 
    if (this.dirScannerThread == null) {
      if (this.prefs.getProperty("preview_file_extensions").equals(".jpg, .jpeg, .gif, .png, .bmp, .ai, .pdf, .psd, .tif, .tiff, .cr2, .dng, .crw, .dcr, .mrw, .nef, .orf, .pef, .srf, .eps"))
        this.prefs.put("preview_file_extensions", ".jpg, .jpeg, .gif, .png, .bmp, .ai, .pdf, .psd, .tif, .tiff, .cr2, .eps"); 
      if (Common.machine_is_x_10_5_plus()) {
        try {
          Common.check_exec();
          Common.exec(new String[] { "chmod", "+x", (new File_S(String.valueOf(System.getProperty("crushftp.home")) + "qlmanage_wrapper.sh")).getCanonicalPath() });
        } catch (Exception e) {
          Log.log("PREVIEW", 1, e);
        } 
        try {
          Common.check_exec();
          Common.exec(new String[] { "chmod", "+x", (new File_S(String.valueOf(System.getProperty("crushftp.home")) + "pcastaction_wrapper.sh")).getCanonicalPath() });
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
  
  public boolean checkExtension(String name, Properties stat) {
    String ext = name.substring(name.lastIndexOf(".")).toUpperCase();
    String[] exts = this.prefs.getProperty("preview_file_extensions", "").toUpperCase().split(",");
    boolean ok_ext = false;
    boolean ok_size = true;
    for (int x = 0; x < exts.length; x++) {
      if (exts[x].indexOf(ext) >= 0) {
        ok_ext = true;
        if (exts[x].indexOf(":") >= 0) {
          long size = Long.parseLong(exts[x].substring(exts[x].indexOf(":") + 1)) * 1024L * 1024L;
          if (Long.parseLong(stat.getProperty("size", "0")) > size)
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
      msg("Image too large, skipping:" + (new VRL(stat.getProperty("url"))).getPath());
      return false;
    } 
    return ok_ext;
  }
  
  public boolean doConvert(GenericClient c, Properties stat_src, Properties stat_dst, boolean multiThread, Properties info, boolean override) {
    if ((new File_S(String.valueOf(System.getProperty("crushftp.home")) + "reset_preview_bad_files")).exists()) {
      (new File_S(String.valueOf(System.getProperty("crushftp.home")) + "reset_preview_bad_files")).delete();
      this.badFiles.clear();
    } 
    String name = stat_src.getProperty("name");
    if (name.indexOf(".") >= 0 && !name.startsWith(".")) {
      if (!checkExtension(name, stat_src) && !override)
        return false; 
      if (stat_dst == null)
        stat_dst = stat_src; 
      String dest = getDestPath2(stat_dst.getProperty("url"));
      VRL vrl = new VRL(stat_dst.getProperty("url"));
      dest = String.valueOf(dest) + vrl.getName() + "/";
      if (!(new File_U(dest)).exists())
        (new File_U(dest)).mkdirs(); 
      Common.updateOSXInfo_U(dest, "-R");
      if (!(new File_U(String.valueOf(dest) + "/p1/1.jpg")).exists() || Long.parseLong(stat_src.getProperty("modified")) != (new File_U(dest)).lastModified() || override)
        try {
          if (this.badFiles.containsKey(stat_src.getProperty("url")) && this.badFiles.getProperty(stat_src.getProperty("url")).equals(stat_src.getProperty("modified"))) {
            msg("Skipping file that can't be converted:" + vrl.getPath());
            return false;
          } 
          msg("Converting: " + vrl.getPath() + "      to: " + dest + "  exists:" + (new File_U(String.valueOf(dest) + "/p1/1.jpg")).exists() + "  modified:" + stat_src.getProperty("modified") + " vs " + (new File_U(dest)).lastModified());
          convert(stat_src, dest, multiThread, info);
          return true;
        } catch (Exception exception) {} 
    } 
    return false;
  }
  
  public void msg(String s) {
    if (this.messages != null) {
      this.messages.addElement("Preview:" + s);
    } else if (this.prefs.getProperty("preview_debug").equals("true")) {
      Log.log("PREVIEW", 0, s);
    } 
  }
  
  public void msg(Exception e) {
    if (this.messages != null) {
      this.messages.addElement(e);
    } else if (this.prefs.getProperty("preview_debug").equals("true")) {
      Log.log("PREVIEW", 0, e);
    } 
  }
  
  public static String getDestPath2(String url) {
    VRL vrl = new VRL(url);
    String destPath = vrl.getPath();
    if (vrl.getProtocol().equalsIgnoreCase("file")) {
      try {
        if (Common.machine_is_windows() && url.toLowerCase().startsWith("file:////")) {
          destPath = String.valueOf((new File_U(Common.url_decode(url.substring(6)))).getParentFile().getPath()) + "/";
          destPath = String.valueOf((new File_U(Common.url_decode(url.substring(6)))).getCanonicalFile().getParentFile().getPath()) + "/";
        } else {
          destPath = String.valueOf((new File_U(vrl.getPath())).getParentFile().getPath()) + "/";
          destPath = String.valueOf((new File_U(vrl.getPath())).getCanonicalFile().getParentFile().getPath()) + "/";
        } 
      } catch (Exception exception) {}
      if (destPath.indexOf(":") >= 0 && destPath.toUpperCase().startsWith("C:")) {
        destPath = destPath.substring(destPath.indexOf(":") + 1);
      } else if (destPath.indexOf(":") >= 0) {
        destPath = "/_-_" + destPath.substring(0, destPath.indexOf(":")) + "_-_" + destPath.substring(destPath.indexOf(":") + 1);
      } else if (Common.machine_is_windows() && destPath.replace('\\', '/').startsWith("//")) {
        destPath = "/_UNC_" + destPath.substring(2);
      } 
      destPath = destPath.replace('\\', '/');
    } else {
      try {
        destPath = Common.all_but_last(Common.getCanonicalPath(url));
      } catch (Exception exception) {}
    } 
    return String.valueOf(ServerStatus.SG("previews_path")) + "Preview" + destPath;
  }
  
  public String get(String key) {
    return this.prefs.getProperty(key);
  }
  
  public boolean convert(Properties stat, String destFile, boolean multiThread, Properties info) {
    try {
      VRL vrl = new VRL(stat.getProperty("url"));
      msg("Creating thumbnail for:" + vrl.getPath());
      synchronized (this.convertSync) {
        while (this.conversionThreadsRunning >= Integer.parseInt(get("preview_conversion_threads")))
          Thread.sleep(500L); 
      } 
      synchronized (this.convertSync2) {
        this.conversionThreadsRunning++;
      } 
      if (!multiThread) {
        (new null.multiConvert(this, stat, destFile, info)).run();
      } else {
        Worker.startWorker(new null.multiConvert(this, stat, destFile, info), "Preview:converting " + vrl.getPath());
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
        this.this$0.msg(e);
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
    ZipInputStream zin = new ZipInputStream(new FileInputStream(new File_U(srcFile)));
    ZipEntry entry = null;
    int loops = 0;
    while ((entry = zin.getNextEntry()) != null) {
      String path = Common.dots(entry.getName().replace('\\', '/'));
      if (path.indexOf("__MACOSX/") >= 0 || 
        path.indexOf(".DS_Store") >= 0)
        continue; 
      entries.append(String.valueOf(path) + "\r\n");
      loops++;
      if (loops > 100)
        break; 
    } 
    zin.close();
    return entries.toString();
  }
  
  public String convertCommandLine(Properties stat, String destFile) throws Exception {
    if (Common.machine_is_windows() && destFile.length() > 245) {
      this.badFiles.put(stat.getProperty("url"), stat.getProperty("modified"));
      return null;
    } 
    VRL vrl = new VRL(stat.getProperty("url"));
    Vector sizes = (Vector)this.prefs.get("preview_sizes");
    try {
      (new File_U(destFile)).mkdirs();
      Common.updateOSXInfo_U(String.valueOf(destFile) + "/", "-R");
      boolean exists = (new File_U(String.valueOf(destFile) + "/index.txt")).exists();
      RandomAccessFile index = new RandomAccessFile(new File_U(String.valueOf(destFile) + "/index.txt"), "rw");
      byte[] b = new byte[(int)index.length()];
      index.readFully(b);
      index.seek(index.length());
      if ((new String(b)).toUpperCase().indexOf(stat.getProperty("name").toUpperCase()) < 0)
        if (vrl.getProtocol().equalsIgnoreCase("file") && !exists) {
          index.write(getZipEntries(vrl.getPath()).getBytes());
          if ((new File_S(String.valueOf(System.getProperty("crushftp.search")) + "tika-app.jar")).exists() && this.prefs.getProperty("preview_file_extensions", "").indexOf("tika") >= 0) {
            byte[] b2 = SearchHandler.getContents(new File_U(vrl.getPath()), ServerStatus.IG("search_max_content_kb"));
            index.write("\r\n".getBytes());
            index.write(b2);
          } 
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
    String srcFile2 = "";
    boolean temp = false;
    if (vrl.getProtocol().equalsIgnoreCase("file")) {
      srcFile2 = vrl.getPath();
      if (Common.machine_is_windows() && srcFile2.startsWith("/")) {
        if (stat.getProperty("url").toLowerCase().startsWith("file:////"))
          srcFile2 = Common.url_decode(stat.getProperty("url").substring(6)); 
        srcFile2 = srcFile2.substring(1).replace('/', '\\');
      } 
    } else {
      GenericClient c = Common.getClient(Common.getBaseUrl(stat.getProperty("url")), "PREVIEW", new Vector());
      c.login(vrl.getUsername(), vrl.getPassword(), null);
      srcFile2 = String.valueOf(ServerStatus.SG("previews_path")) + "Preview/tmp/" + Common.makeBoundary(3) + "_" + vrl.getName();
      (new File_U(String.valueOf(ServerStatus.SG("previews_path")) + "Preview/tmp/")).mkdirs();
      temp = true;
      Common.streamCopier(null, null, c.download(vrl.getPath(), 0L, -1L, true), new FileOutputStream(new File_U(srcFile2)), false, true, true);
      srcFile2 = (new File_U(srcFile2)).getCanonicalPath();
      msg("Copying file down to temp folder:" + vrl.getPath() + "-->" + srcFile2 + ":Done");
    } 
    if (loops > 1.0F) {
      String s = this.prefs.getProperty("preview_movie_info_command_line", "");
      if (Common.machine_is_windows() && !s.toUpperCase().startsWith("CMD /"))
        s = "CMD /C " + s.trim(); 
      String[] args = s.split(" ");
      msg("Getting duration of movie:" + change_vars(s, srcFile2, destFile, 0.0F, 0));
      for (int l = 0; l < args.length; l++)
        args[l] = change_vars(args[l], srcFile2, destFile, 0.0F, 0); 
      File_S f = new File_S(get("preview_working_dir"));
      if (get("preview_working_dir").equals(""))
        f = new File_S(System.getProperty("crushftp.home")); 
      try {
        if (s.toLowerCase().indexOf("convert") >= 0 || s.toLowerCase().indexOf("magic") >= 0)
          validateBytes(new File_U(srcFile2)); 
        Common.check_exec();
        Process proc = Runtime.getRuntime().exec(args, envp, f);
        BufferedReader br1 = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader br2 = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        duration = getDuration(br1);
        if (duration < 0.0F)
          duration = getDuration(br2); 
        if (duration < 0.0F)
          duration = 0.0F; 
        Worker.startWorker(new discarder(this, proc.getInputStream()), "Discard input stream:" + vrl.getPath());
        Worker.startWorker(new discarder(this, proc.getErrorStream()), "Discard error stream:" + vrl.getPath());
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
        int total_pages = 0;
        if (xx <= sizes.size() - 2) {
          int i = 2;
          while ((new File_U(String.valueOf(destFile) + "/p" + i++ + "/3.jpg")).exists())
            total_pages++; 
        } 
        for (int page_loop = 0; page_loop <= total_pages || loop > 1; page_loop++) {
          if (loop == 1)
            page = "p" + (page_loop + 1); 
          try {
            String command_line = get("preview_command_line");
            if (Common.machine_is_windows() && !command_line.toUpperCase().startsWith("CMD /"))
              command_line = "CMD /C " + command_line.trim(); 
            command_line = Common.replace_str(command_line, "\\ ", "þ");
            String[] convert = command_line.split(" ");
            String[] convertCmd = (String[])null;
            convertCmd = new String[convert.length];
            String command_line_result = "";
            String altSrcFile = srcFile2;
            for (int x = 0; x < convert.length; x++) {
              String str = convert[x];
              str = str.replace('þ', ' ');
              str = Common.replace_str(str, "%width%", width);
              str = Common.replace_str(str, "%height%", height);
              if (xx < sizes.size() - 1)
                altSrcFile = String.valueOf(destFile) + "/" + page + "/" + sizes.size() + ".jpg"; 
              str = change_vars(str, altSrcFile, String.valueOf(destFile) + "/" + page + "/" + (xx + 1) + ".jpg", loc, 0);
              str = Common.url_decode(str);
              convertCmd[x] = str;
              command_line_result = String.valueOf(command_line_result) + str + " ";
            } 
            (new File_U(String.valueOf(destFile) + "/" + page)).mkdirs();
            Common.updateOSXInfo_U(String.valueOf(destFile) + "/" + page + "/", "-R");
            msg(command_line_result);
            if (loops > 1.0F && xx < sizes.size() - 1) {
              Common.copy_U(altSrcFile, String.valueOf(destFile) + "/" + page + "/" + (xx + 1) + ".jpg", true);
              (new File_U(String.valueOf(destFile) + "/" + page + "/" + (xx + 1) + ".jpg")).setLastModified(Long.parseLong(stat.getProperty("modified")));
              break;
            } 
            File_S f = new File_S(get("preview_working_dir"));
            if (get("preview_working_dir").equals(""))
              f = new File_S(System.getProperty("crushftp.home")); 
            String s = command_line;
            if (s.toLowerCase().indexOf("convert") >= 0 || s.toLowerCase().indexOf("magic") >= 0)
              validateBytes(new File_U(srcFile2)); 
            Common.check_exec();
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
                  if (tt != null)
                    tt.interrupt(); 
                  break;
                } 
              } 
            } 
            if (vrl.getPath().toUpperCase().endsWith(".PSD") || vrl.getPath().toUpperCase().endsWith(".PDF"))
              if (!(new File_U(String.valueOf(destFile) + "/p1/3.jpg")).exists())
                for (int i = 0; i < 500; ) {
                  if ((new File_U(String.valueOf(destFile) + "/p1/3-" + i + ".jpg")).exists()) {
                    (new File_U(String.valueOf(destFile) + "/p" + (i + 1) + "/")).mkdirs();
                    (new File_U(String.valueOf(destFile) + "/p1/3-" + i + ".jpg")).renameTo(new File_U(String.valueOf(destFile) + "/p" + (i + 1) + "/3.jpg"));
                    i++;
                  } 
                  break;
                }   
            if (!exitInfo.getProperty("exitCode", "").equals("0") || !(new File_U(String.valueOf(destFile) + "/" + page + "/" + (xx + 1) + ".jpg")).exists()) {
              this.badFiles.put(stat.getProperty("url"), stat.getProperty("modified"));
              msg("Preview cannot be generated, adding to list of bad files:" + vrl.getPath());
              break;
            } 
            (new File_U(String.valueOf(destFile) + "/" + page + "/" + (xx + 1) + ".jpg")).setLastModified(Long.parseLong(stat.getProperty("modified")));
          } catch (Exception e) {
            msg(e);
          } 
          if (loop > 1)
            break; 
        } 
      } 
    } 
    (new File_U(destFile)).setLastModified(Long.parseLong(stat.getProperty("modified")));
    if (temp)
      return (new File_U(srcFile2)).getCanonicalPath(); 
    return null;
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
      s = Common.replace_str(s, "%previews%", String.valueOf((new File_U(String.valueOf(ServerStatus.SG("previews_path")) + "/Preview")).getCanonicalPath()) + "/");
    } catch (IOException e) {
      msg(e);
    } 
    if (intervals < 3)
      s = change_vars(s, src, dest, time, intervals); 
    return s;
  }
  
  public void recurseConvert(GenericClient c, VRL vrl, int depth, int max_depth) throws Exception {
    if (depth > max_depth)
      return; 
    c.setConfig("no_stat", "true");
    Properties stat = c.stat(vrl.getPath());
    if (stat == null)
      return; 
    if (stat.getProperty("type").equalsIgnoreCase("dir")) {
      Vector list = new Vector();
      c.list(vrl.getPath(), list);
      for (int x = 0; x < list.size(); x++) {
        Properties p = list.elementAt(x);
        if (p.getProperty("type").equalsIgnoreCase("dir")) {
          recurseConvert(c, new VRL(p.getProperty("url")), depth + 1, max_depth);
        } else {
          doConvert(c, p, null, true, new Properties(), false);
        } 
        if (!this.prefs.getProperty("preview_enabled").equalsIgnoreCase("true") || this.abort)
          return; 
      } 
    } else {
      if (!this.prefs.getProperty("preview_enabled").equalsIgnoreCase("true") || this.abort)
        return; 
      doConvert(c, stat, null, true, new Properties(), false);
    } 
  }
  
  public void reverseRecurseConvert(String real_path, int depth, int max_depth) {
    if (depth > max_depth)
      return; 
    File_U f = new File_U(real_path);
    try {
      real_path = String.valueOf(f.getCanonicalPath()) + "/";
      f = new File_U(real_path);
    } catch (Exception exception) {}
    if (f.isDirectory()) {
      String[] files = f.list();
      for (int x = 0; files != null && x < files.length; x++) {
        if (!this.prefs.getProperty("preview_enabled").equalsIgnoreCase("true") || this.abort)
          return; 
        File_U f2 = new File_U(String.valueOf(real_path) + files[x]);
        if (!Common.isSymbolicLink_U(f2.getAbsolutePath()))
          if (f2.isDirectory())
            if ((new File_U(String.valueOf(real_path) + files[x] + "/index.txt")).exists() && (new File_U(String.valueOf(real_path) + files[x] + "/p1")).exists()) {
              File_U home = new File_U(String.valueOf(ServerStatus.SG("previews_path")) + "/Preview");
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
                      msg("Checking if file exists:" + checkPath);
                    } 
                  } 
                  if (!(new File_U(checkPath)).exists())
                    ok = false; 
                } 
              } catch (Exception exception) {}
              if (!ok) {
                if (!this.prefs.getProperty("preview_enabled").equalsIgnoreCase("true") || this.abort)
                  return; 
                msg("Deleting old thubmnail for deleted file:" + real_path + files[x] + "/");
                Common.recurseDelete_U(String.valueOf(real_path) + files[x] + "/", false);
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
      if (Common.machine_is_windows())
        srcFile = (new File_U(srcFile)).getCanonicalPath(); 
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
        String command_str = "";
        for (int x = 0; x < command.length; x++) {
          if (command[x].equalsIgnoreCase("%SRC%"))
            command[x] = srcFile; 
          command_str = String.valueOf(command_str) + command[x] + " ";
        } 
        if (envp.length == 0 || (envp.length == 1 && envp[0].trim().length() == 0))
          envp = (String[])null; 
        msg("Exif:" + command_str);
        File_S f = null;
        if (get("preview_working_dir").equals("")) {
          f = new File_S(System.getProperty("crushftp.home"));
        } else {
          f = new File_S(get("preview_working_dir"));
        } 
        Common.check_exec();
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
                msg(data);
              }  
          } 
        } 
        br.close();
        proc.waitFor();
      } catch (Exception e) {
        msg(e);
      } 
    } 
    if (metaInfo.size() > 0) {
      if ((new File_U(String.valueOf(destFile) + "info.xml")).exists()) {
        Properties metaInfo_old = (Properties)Common.readXMLObject_U(new File_U(String.valueOf(destFile) + "info.xml"));
        Enumeration keys = metaInfo_old.keys();
        while (keys.hasMoreElements()) {
          String key = (String)keys.nextElement();
          if (key.startsWith("crushftp_") || key.equalsIgnoreCase("keywords"))
            metaInfo.put(key, metaInfo_old.getProperty(key)); 
        } 
      } 
      long destFile_mdtm = (new File_U(destFile)).lastModified();
      Common.writeXMLObject_U(String.valueOf(destFile) + "info.xml", metaInfo, "EXIF");
      (new File_U(destFile)).setLastModified(destFile_mdtm);
    } 
    return metaInfo;
  }
  
  public void validateBytes(File_U f) throws Exception {
    String ext = f.getName().substring(f.getName().lastIndexOf(".") + 1).toLowerCase().trim();
    if ("mvg,msl,svg,".indexOf(String.valueOf(ext) + ",") >= 0)
      throw new Exception(f + " failed byte validation security check due to unsafe file type!"); 
    int found = 0;
    int invalid = 0;
    String failures = "";
    for (int x = 0; x < 10; x++) {
      if (byte_validation.containsKey(String.valueOf(ext) + x)) {
        found++;
        byte[] b1 = (byte[])byte_validation.get(String.valueOf(ext) + x);
        byte[] b2 = new byte[b1.length];
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        try {
          raf.readFully(b2);
        } catch (Exception exception) {}
        raf.close();
        for (int xx = 0; xx < b1.length; xx++) {
          if (b1[xx] != b2[xx]) {
            failures = String.valueOf(failures) + f + " failed byte validation security check before Preview generation! " + DatatypeConverter.printHexBinary(b1) + " vs. " + DatatypeConverter.printHexBinary(b2) + "\r\n";
            invalid++;
            break;
          } 
        } 
      } 
    } 
    if (found == invalid && found > 0) {
      msg(failures.trim());
      throw new Exception(f + " failed byte validation security check before Preview generation!");
    } 
  }
  
  public static Properties getMetaInfo(String destFile) throws Exception {
    Properties metaInfo = new Properties();
    if ((new File_U(String.valueOf(destFile) + "info.xml")).exists())
      metaInfo = (Properties)Common.readXMLObject_U(new File_U(String.valueOf(destFile) + "info.xml")); 
    if (metaInfo == null)
      metaInfo = new Properties(); 
    return metaInfo;
  }
  
  public static Properties setMetaInfo(String destFile, Properties metaInfo) throws Exception {
    (new File_U(destFile)).mkdirs();
    Common.writeXMLObject(String.valueOf(destFile) + "info.xml", metaInfo, "EXIF");
    return metaInfo;
  }
  
  public Properties setExifInfo(String srcFile, String destFile, String exif_key, String exif_val) {
    Properties metaInfo = null;
    if (get("preview_exif_set_command_line") != null && !get("preview_exif_set_command_line").trim().equals("") && !exif_key.startsWith("crushftp_")) {
      try {
        metaInfo = getMetaInfo(destFile);
        if (Common.machine_is_windows())
          srcFile = (new File_U(srcFile)).getCanonicalPath(); 
        if (Common.machine_is_windows())
          destFile = (new File_U(destFile)).getCanonicalPath(); 
      } catch (Exception e1) {
        msg(e1);
      } 
      msg("Updating " + srcFile + " with exif key change.");
      BufferedReader br = null;
      Process proc = null;
      String keywords = null;
      for (int xx = 0; xx < (exif_val.split(",")).length; xx++) {
        try {
          srcFile = (new File_U(srcFile)).getCanonicalPath();
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
          File_S f = null;
          if (get("preview_working_dir").equals("")) {
            f = new File_S(System.getProperty("crushftp.home"));
          } else {
            f = new File_S(get("preview_working_dir"));
          } 
          Common.check_exec();
          proc = Runtime.getRuntime().exec(command, envp, f);
          br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          Worker.startWorker(new discarder(this, proc.getErrorStream()));
          String data = "";
          while ((data = br.readLine()) != null)
            msg(data); 
          br.close();
          proc.waitFor();
          if (exif_key.equalsIgnoreCase("keywords"))
            keywords = exif_val; 
        } catch (Exception e) {
          msg(e);
        } 
      } 
      try {
        metaInfo = getExifInfo(srcFile, destFile);
        if (keywords != null)
          metaInfo.put("keywords", keywords); 
        setMetaInfo(destFile, metaInfo);
      } catch (Exception e) {
        msg(e);
      } 
    } 
    return metaInfo;
  }
}
