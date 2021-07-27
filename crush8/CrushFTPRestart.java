import com.crushftp.client.File_S;
import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.io.StringReader;

public class CrushFTPRestart implements Runnable {
  String home = "./";
  
  public static void main(String[] args) {
    String home = "./";
    if (args != null && args.length > 0)
      home = args[0]; 
    (new Thread(new CrushFTPRestart(home))).start();
  }
  
  public CrushFTPRestart(String home) {
    this.home = home;
  }
  
  public void run() {
    try {
      if (System.getProperties().getProperty("os.name", "").toUpperCase().indexOf("NDOWS") >= 0) {
        String fileList = "";
        System.out.println("Loading list of files to update...");
        try {
          RandomAccessFile in = new RandomAccessFile(new File_S("update_list.txt"), "r");
          byte[] b = new byte[(int)in.length()];
          in.readFully(b);
          in.close();
          fileList = new String(b);
        } catch (Exception e) {
          e.printStackTrace();
          Thread.sleep(100L);
        } 
        System.out.println("-----------------------------------------------------------------------");
        System.out.println(fileList);
        System.out.println("-----------------------------------------------------------------------");
        int loops = 0;
        while (loops++ < 5) {
          System.out.println("Stopping server...");
          Runtime.getRuntime().exec(new String[] { "net", "stop", "CrushFTP Server" });
          try {
            RandomAccessFile test = new RandomAccessFile(new File_S("CrushFTP.jar"), "rw");
            test.close();
            break;
          } catch (Exception exception) {
            Thread.sleep(2000L);
          } 
        } 
        System.out.println("Server stopped.");
        Thread.sleep(1000L);
        if (fileList.equals("")) {
          Process proc = Runtime.getRuntime().exec("cmd.exe /C update.bat".split(" "), (String[])null, new File_S(this.home));
          proc.waitFor();
        } else {
          BufferedReader br = new BufferedReader(new StringReader(fileList));
          String data = "";
          while ((data = br.readLine()) != null) {
            String f1 = data.substring(data.indexOf("UpdateTemp") + "UpdateTemp/".length()).trim();
            (new File_S("./" + f1)).delete();
            (new File_S("./UpdateTemp/" + f1)).renameTo(new File_S("./" + f1));
          } 
          (new File_S("update.bat")).delete();
        } 
        System.out.println("Finished updating files.");
        loops = 0;
        while (loops++ < 5) {
          try {
            RandomAccessFile test = new RandomAccessFile(new File_S("CrushFTP.jar"), "rw");
            test.close();
            System.out.println("Starting server...");
            Runtime.getRuntime().exec(new String[] { "net", "start", "CrushFTP Server" });
          } catch (Exception e) {
            break;
          } 
          Thread.sleep(2000L);
        } 
        System.out.println("Server started.");
        Runtime.getRuntime().exec("net stop CrushFTPRestart");
      } else if (System.getProperties().getProperty("os.name", "").toUpperCase().equals("MAC OS X")) {
        System.out.println("Trying to restart the daemon...");
        Process proc = Runtime.getRuntime().exec("launchctl stop com.crushftp.CrushFTP".split(" "), (String[])null, new File_S(this.home));
        proc.waitFor();
        System.out.println("Daemon stopped.");
        Thread.sleep(1000L);
        proc = Runtime.getRuntime().exec("launchctl start com.crushftp.CrushFTP".split(" "), (String[])null, new File_S(this.home));
        proc.waitFor();
        System.out.println("Finished restarting daemon.");
      } else {
        System.out.println("Trying to restart the daemon...");
        this.home = String.valueOf((new File_S(this.home)).getCanonicalPath()) + "/";
        Process proc = Runtime.getRuntime().exec(String.valueOf(this.home) + "crushftp_init.sh stop");
        proc.waitFor();
        System.out.println("Daemon stopped.");
        Thread.sleep(1000L);
        proc = Runtime.getRuntime().exec(String.valueOf(this.home) + "crushftp_init.sh start");
        proc.waitFor();
        System.out.println("Finished restarting daemon.");
      } 
    } catch (Exception e) {
      e.printStackTrace();
      try {
        Thread.sleep(100L);
      } catch (Exception exception) {}
    } 
    System.out.println("Exiting");
    System.exit(0);
  }
}
