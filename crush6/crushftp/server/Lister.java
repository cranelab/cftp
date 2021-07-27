package crushftp.server;

import com.crushftp.client.VRL;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

public class Lister implements Runnable {
  ServerSession theSession = null;
  
  String the_dir = null;
  
  Vector listing = null;
  
  int scanDepth = 0;
  
  Properties status = null;
  
  public Lister(ServerSession theSession, String the_dir, Vector listing, int scanDepth, Properties status) {
    this.theSession = theSession;
    this.the_dir = the_dir;
    this.listing = listing;
    this.scanDepth = scanDepth;
    this.status = status;
  }
  
  public void run() {
    Thread.currentThread().setName(String.valueOf(this.theSession.uiSG("user_name")) + ":(" + this.theSession.uiSG("user_number") + ")-" + this.theSession.uiSG("user_ip") + " (FULL PATH LIST)");
    this.theSession.uiPUT("list_filetree_status", "Building list...");
    String data = "";
    try {
      Properties item = this.theSession.uVFS.get_item(this.the_dir, -1);
      VRL v = new VRL(item.getProperty("url"));
      if (Common.machine_is_windows() && System.getProperty("crushftp.fast_list", "false").equals("true") && v.getProtocol().equalsIgnoreCase("file") && this.scanDepth == 999) {
        Process proc = Runtime.getRuntime().exec(new String[] { "plugins/lib/listfiles.exe", "/r", v.getPath() });
        BufferedReader winIn = new BufferedReader(new InputStreamReader(proc.getInputStream(), "UTF16"));
        SimpleDateFormat day = new SimpleDateFormat("dd");
        SimpleDateFormat time_or_year = new SimpleDateFormat("yyyy");
        int chop = v.getPath().length();
        Properties lastP = null;
        while (true) {
          data = winIn.readLine();
          if (data == null || data.startsWith("ERROR:"))
            break; 
          String[] parts = data.split(";");
          boolean isDir = parts[0].equalsIgnoreCase("D");
          long size = Long.parseLong(parts[1]);
          Date modified = new Date(Long.parseLong(parts[2]));
          String name = Common.last(parts[3]);
          String path = (String.valueOf(parts[3]) + (isDir ? "\\" : "")).replace('\\', '/');
          Properties p = new Properties();
          p.put("permissions", String.valueOf(isDir ? "d" : "-") + "rwxrwxrwx");
          p.put("owner", "user");
          p.put("group", "group");
          p.put("num_items", "1");
          p.put("name", name);
          p.put("type", isDir ? "DIR" : "FILE");
          p.put("size", (new StringBuffer(String.valueOf(size))).toString());
          p.put("path", "/" + path);
          p.put("url", "file://" + path.replaceAll("%", "%25").replaceAll(" ", "%20"));
          p.put("modified", (new StringBuffer(String.valueOf(modified.getTime()))).toString());
          p.put("day", day.format(modified));
          p.put("time_or_year", time_or_year.format(modified));
          p.put("privs", item.getProperty("privs"));
          p.put("root_dir", "/" + this.the_dir + Common.all_but_last(path.substring(chop)));
          this.listing.addElement(p);
          lastP = p;
          while (this.listing.size() > 10000)
            Thread.sleep(100L); 
        } 
        if (lastP != null) {
          lastP = (Properties)lastP.clone();
          lastP.put("name", Common.makeBoundary());
          this.listing.addElement(lastP);
        } 
        winIn.close();
        if (proc.exitValue() != 0)
          throw new Exception("Listing error:" + data); 
        proc.destroy();
      } else {
        this.theSession.uVFS.getListing(this.listing, this.the_dir, this.scanDepth, 10000, true, null);
      } 
    } catch (Exception e) {
      Log.log("LIST", 0, data);
      Log.log("LIST", 0, e);
      this.status.put("stop_message", "FAILED:" + e.getMessage() + ":" + data);
    } 
    this.status.put("done", "true");
  }
}
