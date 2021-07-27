package crushftp.db;

import com.crushftp.client.Common;
import com.crushftp.client.FileClient;
import com.crushftp.client.File_S;
import com.crushftp.client.File_U;
import com.crushftp.client.GenericClient;
import com.crushftp.client.GenericClientMulti;
import com.crushftp.client.VRL;
import com.crushftp.client.Worker;
import crushftp.handlers.Common;
import crushftp.handlers.Log;
import crushftp.handlers.PreviewWorker;
import crushftp.handlers.SessionCrush;
import crushftp.server.LIST_handler;
import crushftp.server.ServerStatus;
import crushftp.server.VFS;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class SearchHandler implements Runnable {
  Properties status = new Properties();
  
  Vector listing = null;
  
  SessionCrush thisSession = null;
  
  String the_dir = null;
  
  Properties added_hash = new Properties();
  
  int depth = 20;
  
  public static Properties keywords_cache = new Properties();
  
  public SearchHandler(SessionCrush thisSession, Vector listing, String the_dir, int depth) {
    this.thisSession = thisSession;
    this.listing = listing;
    this.the_dir = the_dir;
    this.depth = depth;
    this.status.put("done", "false");
  }
  
  public void recurseAddCache(String cpath, int depth_level) {
    if (depth_level-- == 0)
      return; 
    Vector dirCache = (Vector)FileClient.dirCachePerm.get(String.valueOf(cpath) + "/");
    if (Log.log("SEARCH", 2, ""))
      Log.log("SEARCH", 2, "Search " + cpath + " size=" + ((dirCache == null) ? "null" : (new StringBuffer(String.valueOf(dirCache.size()))).toString())); 
    if (dirCache == null) {
      Properties dir_item = (Properties)FileClient.dirCachePerm.get(cpath);
      if (dir_item != null) {
        if (Log.log("SEARCH", 2, ""))
          Log.log("SEARCH", 2, "Search " + cpath + " size=" + dir_item.size()); 
        dir_item = (Properties)dir_item.clone();
        dir_item.put("db", "true");
        if (!this.added_hash.containsKey(dir_item.getProperty("url"))) {
          this.listing.addElement(dir_item);
          getKeywords(dir_item.getProperty("url"));
          this.added_hash.put(dir_item.getProperty("url"), "");
        } 
      } 
    } else {
      Properties dir_item = (Properties)FileClient.dirCachePerm.get(cpath);
      if (dir_item != null) {
        dir_item = (Properties)dir_item.clone();
        dir_item.put("db", "true");
        if (!this.added_hash.containsKey(dir_item.getProperty("url"))) {
          this.listing.addElement(dir_item);
          getKeywords(dir_item.getProperty("url"));
          this.added_hash.put(dir_item.getProperty("url"), "");
        } 
      } 
      for (int x = 0; x < dirCache.size(); x++)
        recurseAddCache(dirCache.elementAt(x).toString(), depth_level); 
    } 
  }
  
  public void run() {
    try {
      if (FileClient.memCache) {
        Properties lookupItem = this.thisSession.uVFS.get_item(this.the_dir);
        VRL vrl = new VRL(lookupItem.getProperty("url"));
        Vector root_items = new Vector();
        if (vrl.getProtocol().equalsIgnoreCase("virtual")) {
          this.thisSession.uVFS.getListing(root_items, this.the_dir);
        } else {
          GenericClient c = this.thisSession.uVFS.getClient(lookupItem);
          c.list((new VRL(lookupItem.getProperty("url"))).getPath(), root_items);
          c.close();
          this.thisSession.uVFS.releaseClient(c);
        } 
        for (int x = 0; x < root_items.size(); x++) {
          lookupItem = root_items.elementAt(x);
          vrl = new VRL(lookupItem.getProperty("url"));
          if (vrl.getProtocol().equalsIgnoreCase("virtual")) {
            lookupItem = this.thisSession.uVFS.get_item(vrl.getPath());
            if ((new VRL(lookupItem.getProperty("url"))).getProtocol().equalsIgnoreCase("file")) {
              GenericClient c = this.thisSession.uVFS.getClient(lookupItem);
              Vector root_items2 = new Vector();
              if (c instanceof GenericClientMulti) {
                GenericClientMulti gcm = (GenericClientMulti)c;
                for (int xx = 0; xx < gcm.clients.size(); xx++) {
                  GenericClient c2 = gcm.clients.elementAt(xx);
                  Vector v = new Vector();
                  c2.list((new VRL((String)c2.getConfig("url"))).getPath(), v);
                  root_items2.addAll(v);
                } 
              } else {
                c.list((new VRL(lookupItem.getProperty("url"))).getPath(), root_items2);
              } 
              c.close();
              this.thisSession.uVFS.releaseClient(c);
              root_items.addAll(root_items2);
            } 
          } 
          if (vrl.getProtocol().equalsIgnoreCase("file")) {
            String cpath = (new File_U(vrl.getPath())).getCanonicalPath().replace('\\', '/');
            getKeywords(vrl.toString());
            recurseAddCache(cpath, this.depth);
          } 
        } 
        Log.log("SEARCH", 0, "Listing results size for search:" + this.listing.size());
      } else if (ServerStatus.SG("search_index_usernames").equals("")) {
        this.thisSession.uVFS.getListing(this.listing, this.the_dir, this.depth, 1000, true);
      } else {
        Properties lookupItem = this.thisSession.uVFS.get_item(this.thisSession.uiSG("current_dir"));
        try {
          Vector v = ServerStatus.thisObj.searchTools.executeSqlQuery(ServerStatus.SG("search_db_query"), new Object[] { String.valueOf((new VRL(lookupItem.getProperty("url"))).getPath()) + "%" }, false, false);
          for (int x = 0; x < v.size(); x++) {
            Properties pp = v.elementAt(x);
            if (pp.containsKey("ITEM_MODIFIED")) {
              pp.put("root_dir", pp.remove("ITEM_PATH"));
              pp.put("url", (new VRL(pp.getProperty("root_dir", ""))).toString());
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
        } catch (Throwable t) {
          Log.log("SEARCH", 0, t);
        } 
      } 
    } catch (Exception e) {
      Log.log("SEARCH", 0, e);
    } 
    this.added_hash.clear();
    this.status.put("done", "true");
  }
  
  public static void buildEntry(Properties pp, VFS uVFS, boolean deleteOnly, boolean deleteItems) {
    if (!FileClient.memCache && ServerStatus.SG("search_index_usernames").equals(""))
      return; 
    try {
      if (!FileClient.memCache && deleteItems)
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
              }), String.valueOf(Thread.currentThread().getName()) + ":lister:");
      } catch (Exception e) {
        Log.log("SERVER", 0, e);
        status.append("done");
      } 
    } else {
      listing.addElement(pp);
      status.append("done");
    } 
    boolean tika = (new File_S(String.valueOf(System.getProperty("crushftp.search")) + "tika-app.jar")).exists();
    while (listing.size() > 0 || status.length() == 0) {
      if (listing.size() > 0) {
        Properties ppp = listing.remove(0);
        ServerStatus.thisObj.server_info.put("memcache_objects", (new StringBuffer(String.valueOf(FileClient.dirCachePerm.size()))).toString());
        if (FileClient.memCache) {
          getKeywords(ppp.getProperty("url"));
          continue;
        } 
        try {
          if (uVFS.thisSession != null && !LIST_handler.checkName(ppp, uVFS.thisSession, false, false))
            continue; 
        } catch (Throwable e) {
          Log.log("SEARCH", 1, e);
        } 
        try {
          String contents = "";
          File_U f = new File_U((new VRL(ppp.getProperty("url"))).getPath());
          if (f.isFile() && tika) {
            Vector v = ServerStatus.thisObj.searchTools.executeSqlQuery(ServerStatus.SG("search_db_query"), new Object[] { (new VRL(ppp.getProperty("url"))).getPath() }, false, false);
            if (v.size() == 1) {
              Properties tmp = v.elementAt(0);
              if (tmp.getProperty("ITEM_MODIFIED", "0").equals(ppp.getProperty("modified", "-1")))
                continue; 
            } 
            if (tika)
              contents = new String(getContents(f, ServerStatus.IG("search_max_content_kb")), "UTF8"); 
          } 
          Object[] values = new Object[4];
          values[0] = ppp.getProperty("size");
          values[1] = ppp.getProperty("modified");
          String keywords = getKeywords(ppp.getProperty("url"));
          if (keywords.length() > 1600)
            keywords = keywords.substring(0, 1600); 
          values[2] = String.valueOf(keywords) + "\r\n" + contents;
          values[3] = (new VRL(ppp.getProperty("url"))).getPath();
          if (ServerStatus.thisObj.searchTools.executeSql(ServerStatus.SG("search_db_update"), values) <= 0) {
            values = new Object[5];
            values[0] = (new VRL(ppp.getProperty("url"))).getPath();
            values[1] = ppp.getProperty("type");
            values[2] = ppp.getProperty("size");
            values[3] = ppp.getProperty("modified");
            values[4] = String.valueOf(keywords) + "\r\n" + contents;
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
  
  public static byte[] getContents(File_U f, int max_kb) throws Exception {
    if (f.isDirectory())
      return new byte[0]; 
    Common.check_exec();
    Process proc = Runtime.getRuntime().exec((String.valueOf(System.getProperty("java.home")) + File_S.separator + "bin" + File_S.separator + "java;-jar;tika-app.jar;-t;" + f.getCanonicalPath()).split(";"), (String[])null, new File_S(System.getProperty("crushftp.search")));
    InputStream in = proc.getInputStream();
    byte[] b = new byte[1024];
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    int bytes_read = 0;
    while (bytes_read >= 0) {
      bytes_read = in.read(b);
      if (baos.size() > max_kb * 1024) {
        proc.destroy();
        break;
      } 
      if (bytes_read >= 0)
        baos.write(b, 0, bytes_read); 
    } 
    in.close();
    try {
      proc.destroy();
    } catch (Exception exception) {}
    if (Common.machine_is_windows()) {
      String s = new String(baos.toByteArray(), ServerStatus.SG("windows_character_encoding_process"));
      baos.reset();
      baos.write(s.getBytes("UTF8"));
    } 
    return baos.toByteArray();
  }
  
  public boolean isActive() {
    return this.status.getProperty("done", "false").equalsIgnoreCase("false");
  }
  
  public static Properties findItem(Properties pp, VFS uVFS, Vector items, String root_dir) throws Exception {
    String pp_canonical = null;
    if (FileClient.memCache) {
      pp_canonical = (new File_U((new VRL(pp.getProperty("url"))).getPath())).getCanonicalPath();
    } else {
      pp_canonical = (new File_U(pp.getProperty("root_dir"))).getCanonicalPath();
    } 
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
              String home_canonical = (new File_U(vrl.getPath())).getCanonicalPath();
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
  
  public static String getKeywords(String url) {
    if (!ServerStatus.BG("search_keywords_also"))
      return ""; 
    String the_dir = getPreviewPath(url, "1", 1);
    if (the_dir == null)
      return ""; 
    String index = String.valueOf(ServerStatus.SG("previews_path")) + the_dir.substring(1);
    StringBuffer resultData = new StringBuffer();
    if (FileClient.memCache)
      if (keywords_cache.containsKey(index))
        return keywords_cache.getProperty(index);  
    if ((new File_U(String.valueOf(Common.all_but_last(index)) + "../index.txt")).exists())
      try {
        RandomAccessFile out = new RandomAccessFile(new File_U(String.valueOf(Common.all_but_last(index)) + "../index.txt"), "r");
        byte[] b = new byte[(int)out.length()];
        out.readFully(b);
        out.close();
        resultData.append(new String(b, "UTF8"));
      } catch (Exception e) {
        Log.log("PREVIEW", 1, e);
      }  
    if ((new File_U(String.valueOf(Common.all_but_last(index)) + "../info.xml")).exists()) {
      Properties xml = (Properties)Common.readXMLObject_U(String.valueOf(Common.all_but_last(index)) + "../info.xml");
      Enumeration keys = xml.keys();
      while (keys.hasMoreElements()) {
        String key = keys.nextElement().toString();
        resultData.append(" ").append(xml.getProperty(key));
      } 
    } 
    if (FileClient.memCache && ServerStatus.BG("search_keywords_also"))
      synchronized (keywords_cache) {
        String s = resultData.toString();
        long total_size = ServerStatus.siLG("keywords_cache_size") + s.length() + index.length() + 10L;
        keywords_cache.put(index, s);
        ServerStatus.siPUT("keywords_cache_size", (new StringBuffer(String.valueOf(total_size))).toString());
      }  
    return resultData.toString();
  }
  
  public static String getPreviewPath(Properties item, String size, int frame) {
    return getPreviewPath(item.getProperty("url"), size, frame);
  }
  
  public static String getPreviewPath(String url, String size, int frame) {
    if (frame < 1)
      frame = 1; 
    String path = null;
    if (url != null) {
      VRL otherFile = new VRL(url);
      if (otherFile.getFile() == null)
        return "/"; 
      try {
        path = otherFile.getCanonicalPath();
      } catch (Exception e) {
        Log.log("PREVIEW", 1, e);
      } 
      if (path.equalsIgnoreCase("/"))
        return null; 
      try {
        path = String.valueOf(PreviewWorker.getDestPath2(url)) + otherFile.getName() + "/";
      } catch (Exception e) {
        path = "./";
      } 
      while (!(new File_U(String.valueOf(path) + "p" + frame)).exists() && frame > 1)
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
