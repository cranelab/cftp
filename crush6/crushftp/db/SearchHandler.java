package crushftp.db;

import com.crushftp.client.VRL;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.PreviewWorker;
import crushftp.server.ServerSession;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
import crushftp.server.Worker;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class SearchHandler implements Runnable {
  Properties status = new Properties();
  
  Vector listing = null;
  
  ServerSession thisSession = null;
  
  public SearchHandler(ServerSession thisSession, Vector listing) {
    this.thisSession = thisSession;
    this.listing = listing;
    this.status.put("done", "false");
  }
  
  public void run() {
    try {
      if (ServerStatus.SG("search_index_usernames").equals("")) {
        this.thisSession.uVFS.getListing(this.listing, this.thisSession.uiSG("current_dir"), 20, 1000, true);
      } else {
        Properties lookupItem = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
        try {
          Vector v = ServerStatus.thisObj.searchTools.executeSqlQuery(ServerStatus.SG("search_db_query"), new Object[] { String.valueOf((new VRL(lookupItem.getProperty("url"))).getPath()) + "%" }, false, false);
          for (int x = 0; x < v.size(); x++) {
            Properties pp = v.elementAt(x);
            if (pp.containsKey("ITEM_MODIFIED")) {
              pp.put("root_dir", pp.remove("ITEM_PATH"));
              String item_name = Common.last(pp.getProperty("root_dir"));
              if (item_name.endsWith("/"))
                item_name = item_name.substring(0, item_name.length() - 1); 
              pp.put("name", item_name);
              pp.put("type", pp.remove("ITEM_TYPE"));
              pp.put("size", pp.remove("ITEM_SIZE"));
              pp.put("modified", pp.remove("ITEM_MODIFIED"));
              pp.put("keywords", pp.remove("ITEM_KEYWORDS"));
              pp.put("db", "true");
              this.listing.addElement(pp);
            } 
          } 
          Log.log("SEARCH", 0, "Listing results size for search:" + this.listing.size());
          if (v.size() == 0) {
            Vector listing2 = new Vector();
            Vector status = new Vector();
            Runnable r = new Runnable(this, listing2, status) {
                final SearchHandler this$0;
                
                private final Vector val$listing2;
                
                private final Vector val$status;
                
                public void run() {
                  try {
                    this.this$0.thisSession.uVFS.getListing(this.val$listing2, this.this$0.thisSession.uiSG("current_dir"), 20, 100, true);
                    this.val$status.addElement("done");
                  } catch (Exception e) {
                    Log.log("SEARCH", 0, e);
                    this.val$status.addElement(e);
                  } 
                }
              };
            Worker.startWorker(r);
            while (listing2.size() > 0 || status.size() == 0) {
              if (listing2.size() > 0) {
                Properties pp = listing2.remove(0);
                this.listing.addElement(pp);
                buildEntry(pp, this.thisSession.uVFS, false, false);
                continue;
              } 
              Thread.sleep(100L);
            } 
            if (status.size() > 0 && !status.elementAt(0).toString().equals("done"))
              throw (Exception)status.elementAt(0); 
          } 
        } catch (Throwable t) {
          Log.log("SEARCH", 0, t);
        } 
      } 
    } catch (Exception e) {
      Log.log("SEARCH", 0, e);
    } 
    this.status.put("done", "true");
  }
  
  public static void buildEntry(Properties pp, VFS uVFS, boolean deleteOnly, boolean deleteItems) {
    if (ServerStatus.SG("search_index_usernames").equals(""))
      return; 
    try {
      if (deleteItems)
        ServerStatus.thisObj.searchTools.executeSql(ServerStatus.SG("search_db_delete"), new Object[] { String.valueOf((new VRL(pp.getProperty("url"))).getPath()) + "%" }); 
    } catch (Throwable t) {
      Log.log("SEARCH", 0, t);
    } 
    if (deleteOnly)
      return; 
    if (!pp.getProperty("protocol").equalsIgnoreCase("file") && !pp.getProperty("protocol").equalsIgnoreCase("virtual"))
      return; 
    Vector listing = new Vector();
    StringBuffer status = new StringBuffer();
    if (pp.getProperty("type", "").equalsIgnoreCase("DIR")) {
      try {
        Worker.startWorker(new Thread(new Runnable(uVFS, listing, pp, status) {
                private final VFS val$uVFS;
                
                private final Vector val$listing;
                
                private final Properties val$pp;
                
                private final StringBuffer val$status;
                
                public void run() {
                  try {
                    this.val$uVFS.getListing(this.val$listing, this.val$pp.getProperty("root_dir"), 20, 50000, true);
                  } catch (Exception e) {
                    this.val$listing.addElement(this.val$pp);
                  } 
                  this.val$status.append("done");
                }
              }));
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        status.append("done");
      } 
    } else {
      listing.addElement(pp);
      status.append("done");
    } 
    while (listing.size() > 0 || status.length() == 0) {
      if (listing.size() > 0) {
        Properties ppp = listing.remove(0);
        try {
          Object[] values = new Object[4];
          values[0] = ppp.getProperty("size");
          values[1] = ppp.getProperty("modified");
          values[2] = getKeywords(ppp);
          values[3] = (new VRL(ppp.getProperty("url"))).getPath();
          if (ServerStatus.thisObj.searchTools.executeSql(ServerStatus.SG("search_db_update"), values) <= 0) {
            values = new Object[5];
            values[0] = (new VRL(ppp.getProperty("url"))).getPath();
            values[1] = ppp.getProperty("type");
            values[2] = ppp.getProperty("size");
            values[3] = ppp.getProperty("modified");
            String keywords = getKeywords(ppp);
            if (keywords.length() > 1600)
              keywords = keywords.substring(0, 1600); 
            values[4] = keywords;
            ServerStatus.thisObj.searchTools.executeSql(ServerStatus.SG("search_db_insert"), values);
          } 
        } catch (Throwable t) {
          Log.log("SEARCH", 0, t);
        } 
        continue;
      } 
      try {
        Thread.sleep(100L);
      } catch (InterruptedException interruptedException) {}
    } 
  }
  
  public boolean isActive() {
    return this.status.getProperty("done", "false").equalsIgnoreCase("false");
  }
  
  public static Properties findItem(Properties pp, VFS uVFS, Vector items, String root_dir) throws Exception {
    String pp_canonical = (new File(pp.getProperty("root_dir"))).getCanonicalPath();
    for (int x = 0; x < uVFS.homes.size(); x++) {
      Properties virtual = uVFS.homes.elementAt(x);
      Enumeration keys = virtual.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        if (!key.equals("vfs_permissions_object")) {
          Properties home = (Properties)virtual.get(key);
          if (home.containsKey("vItems")) {
            Vector vItems = (Vector)home.get("vItems");
            for (int xx = 0; xx < vItems.size(); xx++) {
              Properties vitem = vItems.elementAt(xx);
              VRL vrl = new VRL(vitem.getProperty("url"));
              String home_canonical = (new File(vrl.getPath())).getCanonicalPath();
              if (vrl.getProtocol().equalsIgnoreCase("FILE") && pp_canonical.startsWith(home_canonical)) {
                Properties ppp = uVFS.get_item(String.valueOf(key) + pp_canonical.substring(home_canonical.length()));
                return ppp;
              } 
            } 
          } 
        } 
      } 
    } 
    return null;
  }
  
  public static String getKeywords(Properties pp) throws Exception {
    String the_dir = getPreviewPath(pp, "1", 1);
    String index = String.valueOf(ServerStatus.SG("previews_path")) + the_dir.substring(1);
    StringBuffer resultData = new StringBuffer();
    if ((new File(String.valueOf(Common.all_but_last(index)) + "../index.txt")).exists()) {
      RandomAccessFile out = new RandomAccessFile(String.valueOf(Common.all_but_last(index)) + "../index.txt", "r");
      byte[] b = new byte[(int)out.length()];
      out.readFully(b);
      out.close();
      resultData.append(new String(b, "UTF8"));
    } 
    if ((new File(String.valueOf(Common.all_but_last(index)) + "../info.xml")).exists()) {
      Properties xml = (Properties)Common.readXMLObject(String.valueOf(Common.all_but_last(index)) + "../info.xml");
      Enumeration keys = xml.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        resultData.append(" ").append(xml.getProperty(key));
      } 
    } 
    return resultData.toString();
  }
  
  public static String getPreviewPath(Properties item, String size, int frame) throws Exception {
    if (frame < 1)
      frame = 1; 
    String path = null;
    if (item != null) {
      VRL otherFile = new VRL(item.getProperty("url"));
      if (otherFile.getFile() == null)
        return "/"; 
      if (otherFile.getProtocol().equalsIgnoreCase("FTP"))
        return null; 
      path = otherFile.getCanonicalPath();
      if (path.equalsIgnoreCase("/"))
        return null; 
      try {
        path = String.valueOf(PreviewWorker.getDestPath(path)) + otherFile.getName() + "/";
      } catch (Exception e) {
        path = "./";
      } 
      while (!(new File(String.valueOf(path) + "p" + frame)).exists() && frame > 1)
        frame--; 
      size = Common.dots(size);
      if (size.indexOf(".") >= 0) {
        path = String.valueOf(path) + "p" + frame + "/" + size;
      } else {
        path = String.valueOf(path) + "p" + frame + "/" + size + ".jpg";
      } 
      if (ServerStatus.SG("previews_path").length() - 1 < 0) {
        path = null;
      } else {
        path = path.substring(ServerStatus.SG("previews_path").length() - 1);
      } 
    } 
    return path;
  }
}
