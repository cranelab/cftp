import com.crushftp.client.Common;
import crushftp.gui.LOC;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

public class CrushFTPLauncher {
  static {
    System.setProperty("Syslog.classloader.warning", "off");
  }
  
  public CrushFTPLauncher(Object o) {
    Common.log = new Vector();
    if (System.getProperties().containsKey("crushftp.log.append"))
      Common.log = (Vector)System.getProperties().remove("crushftp.log.append"); 
    String[] args = (String[])o;
    Common.initSystemProperties(true);
    System.getProperties().put("crushftp.version", "6");
    String defaultUserFolder = "MainUsers";
    if (args == null || args.length == 0) {
      args = new String[1];
      args[0] = "-g";
    } 
    if (args[0].toUpperCase().startsWith("-DMZI")) {
      Common common_code = new Common();
      if (Common.machine_is_x()) {
        common_code.install_osx_service();
      } else if (Common.machine_is_windows()) {
        common_code.install_windows_service();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          Common.copyStreams(new FileInputStream("service/wrapper.conf"), baos, true, true);
          String s = new String(baos.toByteArray(), "UTF8");
          s = Common.replace_str(s, "wrapper.app.parameter.1=-d", "wrapper.app.parameter.1=-dmz\r\nwrapper.app.parameter.2=" + args[1]);
          s = Common.replace_str(s, "plugins/lib/CrushFTPJarProxy.jar", "CrushFTP.jar");
          RandomAccessFile raf = new RandomAccessFile("service/wrapper.conf", "rw");
          raf.setLength(0L);
          raf.write(s.getBytes("UTF8"));
          raf.close();
          Process proc = Runtime.getRuntime().exec("net stop \"CrushFTP Server\"");
          BufferedReader proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          String data = "";
          while ((data = proc_in.readLine()) != null)
            Log.log("SERVER", 0, data); 
          proc_in.close();
          proc = Runtime.getRuntime().exec("net start \"CrushFTP Server\"");
          proc_in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          data = "";
          while ((data = proc_in.readLine()) != null)
            Log.log("SERVER", 0, data); 
          proc_in.close();
        } catch (IOException e) {
          e.printStackTrace();
        } 
      } 
    } else if (args[0].toUpperCase().startsWith("-DMZ")) {
      System.setProperty("java.awt.headless", "true");
    } else if (args[0].toUpperCase().startsWith("-V")) {
      System.out.println("CrushFTP " + ServerStatus.version_info_str + ServerStatus.sub_version_info_str);
      System.out.println("CrushTunnel 3.1.16");
    } else if (args[0].toUpperCase().startsWith("-D")) {
      System.setProperty("java.awt.headless", "true");
    } else if (args[0].toUpperCase().equals("-P")) {
      if (args[2].startsWith("RANDOM_")) {
        args[2] = Common.makeBoundary(Integer.parseInt(args[2].split("_")[1].trim()));
        System.out.println("Using random password:" + args[2]);
      } 
      System.out.println((new Common()).encode_pass(args[2], args[1], (args.length >= 4) ? args[3] : ""));
    } else if (args[0].toUpperCase().equals("-R")) {
      SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.US);
      System.getProperties().put("crushftp.immunityToDieString", sdf.format(new Date()));
      if (Common.machine_is_x()) {
        Common.remove_osx_service();
      } else if (Common.machine_is_windows()) {
        Common.remove_windows_service();
      } 
    } else if (args[0].toUpperCase().equals("-A")) {
      String userDir = defaultUserFolder;
      (new Common()).writeAdminUser(args[1], args[2], userDir, false);
      System.out.println(LOC.G("Admin user written to") + ":" + System.getProperties().getProperty("crushftp.users") + userDir + "/");
    } else if (args[0].toUpperCase().equals("-U")) {
      String permissions = "(read)(view)(resume)";
      String templateUser = "";
      String notes = null;
      String email = null;
      if (args.length >= 5)
        if (args[4].toUpperCase().indexOf("FULL") >= 0) {
          permissions = "(read)(write)(view)(delete)(resume)(rename)(makedir)(deletedir)";
        } else if (args[4].toUpperCase().indexOf("READ_ONLY") >= 0) {
          permissions = "(read)(view)(resume)";
        } else {
          permissions = args[4];
        }  
      if (args.length >= 6)
        templateUser = args[5]; 
      if (args.length >= 7)
        notes = args[6]; 
      if (args.length >= 8)
        email = args[7]; 
      (new Common()).writeNewUser(args[1], args[2], args[3], permissions, templateUser, notes, email, defaultUserFolder);
      System.out.println(LOC.G("User written to") + ":" + System.getProperties().getProperty("crushftp.users") + defaultUserFolder + "/");
    } else if (args[0].toUpperCase().equals("-?") || args[0].toUpperCase().equals("/?") || args[0].toUpperCase().equals("-HELP") || args[0].toUpperCase().equals("/HELP")) {
      System.out.println("   : (GUI LOADED) no parameters loads it normally with a GUI.");
      System.out.println("-d :      runs as a daemon without ever initializing any GUI objects at all.");
      System.out.println("                  Very fast, low memory, useful when being run as a service.");
      System.out.println("-h : (GUI LOADED) runs it normal with a GUI, but after loading it hides the main GUI");
      System.out.println("                  window so its not visible wasting CPU cycles.");
      System.out.println("-a :      takes 2 additional quoted parameters of [username] [password] and makes a directory");
      System.out.println("                  named [username] with a user.XML file in it that has FULL remote admin privs.");
      System.out.println("-u :      takes 7 additional quoted parameters of [username] [password] [start dir path in slash notation ex:\"/my hd/\"]");
      System.out.println("                  [permissions (either 'FULL' or 'READ_ONLY' or the actual items] [templateUser:(all settings from user are copied except VFS.  Use \"\" to skip)] [notes] [email]");
      System.out.println("                  and makes a directory named [username] with a user.XML file in it.");
      System.out.println("-r :      removes service/daemon (Windows and OS X only)");
      System.out.println("-p :      takes two parameters to encrypt a password. The first is the format (DES or SHA), the second is the password.");
      System.out.println("");
      System.out.println("-v : CrushFTP version info.");
      System.out.println("-? : this help screen.");
      System.out.println("/? : this help screen.");
      System.out.println("-help : this help screen.");
      System.out.println("/help : this help screen.");
    } else {
    
    } 
  }
}
