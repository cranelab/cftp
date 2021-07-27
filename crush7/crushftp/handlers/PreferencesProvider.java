package crushftp.handlers;

import crushftp.server.ServerStatus;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Properties;
import java.util.Vector;

public class PreferencesProvider {
  public long getPrefsTime(String instance) {
    if (ServerStatus.siBG("dmz_mode"))
      return 0L; 
    if (instance == null || instance.equals("")) {
      instance = "";
    } else if (!instance.startsWith("_")) {
      instance = "_" + instance;
    } 
    return (new File(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML")).lastModified();
  }
  
  public Properties loadPrefs(String instance) {
    if (ServerStatus.siBG("dmz_mode"))
      return ServerStatus.server_settings; 
    if (instance == null || instance.equals("")) {
      instance = "";
    } else if (!instance.startsWith("_")) {
      instance = "_" + instance;
    } 
    return (Properties)Common.readXMLObject(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML");
  }
  
  public Properties getBackupPrefs(String instance) {
    if (ServerStatus.siBG("dmz_mode"))
      return (Properties)ServerStatus.thisObj.default_settings.clone(); 
    if (instance == null || instance.equals("")) {
      instance = "";
    } else if (!instance.startsWith("_")) {
      instance = "_" + instance;
    } 
    Properties newPrefs = null;
    int index = 0;
    for (index = 0; index < 100; index++) {
      if (!(new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/prefs" + index + ".XML")).exists() || (new File(String.valueOf(System.getProperty("crushftp.backup")) + "backup/prefs" + index + ".XML")).length() == 0L)
        continue; 
      try {
        Log.log("SERVER", 0, "Trying prefs.xml backup file:prefs" + index + ".XML");
        newPrefs = (Properties)Common.readXMLObject(String.valueOf(System.getProperty("crushftp.backup")) + "backup/prefs" + index + ".XML");
        if (newPrefs == null)
          continue; 
        break;
      } catch (Exception exception) {
        continue;
      } 
    } 
    if (index >= 98) {
      Log.log("SERVER", 0, "prefs.XML backup files were missing or corrupt.  Using defaults instead...");
      newPrefs = (Properties)ServerStatus.thisObj.default_settings.clone();
    } 
    return newPrefs;
  }
  
  public void savePrefs(Properties server_settings, String instance) {
    if (ServerStatus.siBG("dmz_mode"))
      return; 
    if (instance == null || instance.equals("")) {
      instance = "";
    } else if (!instance.startsWith("_")) {
      instance = "_" + instance;
    } 
    synchronized (server_settings) {
      try {
        Vector add_vec = new Vector();
        add_vec.addElement(server_settings);
        try {
          if (!(new File(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML")).exists()) {
            RandomAccessFile makeIt = new RandomAccessFile(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML", "rw");
            makeIt.close();
            ServerStatus.thisObj.server_info.put("currentFileDate" + instance, (new StringBuffer(String.valueOf((new File(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML")).lastModified()))).toString());
          } 
          if ((new File(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML")).lastModified() == ServerStatus.siLG("currentFileDate" + instance)) {
            Common.write_server_settings(server_settings, instance);
            ServerStatus.thisObj.server_info.put("currentFileDate" + instance, (new StringBuffer(String.valueOf((new File(String.valueOf(System.getProperty("crushftp.prefs")) + "prefs" + instance + ".XML")).lastModified()))).toString());
          } 
        } catch (Exception e) {
          Log.log("SERVER", 0, "Prefs.XML failed to be written1...");
          Log.log("SERVER", 0, e);
        } 
      } catch (Exception e) {
        Log.log("SERVER", 0, "Prefs.XML failed to be written2...");
        Log.log("SERVER", 0, e);
      } 
    } 
  }
  
  public boolean check_code() {
    String name = ServerStatus.SG("registration_name");
    String email = ServerStatus.SG("registration_email");
    String code = ServerStatus.SG("registration_code");
    boolean ok = ServerStatus.thisObj.common_code.register(name, email, code);
    if (ok) {
      String v = ServerStatus.thisObj.common_code.getRegistrationAccess("V", ServerStatus.SG("registration_code"));
      if (v != null && (v.equals("4") || v.equals("5") || v.equals("6"))) {
        String msg = "CrushFTP " + ServerStatus.version_info_str + " will not work with a CrushFTP " + v + " license.";
        Log.log("SERVER", 0, msg);
        ServerStatus.put_in("max_max_users", "5");
        ServerStatus.put_in("max_users", "5");
        return false;
      } 
      ServerStatus.put_in("max_max_users", ServerStatus.thisObj.common_code.getRegistrationAccess("MAX", code));
      String e_level = ServerStatus.thisObj.common_code.getRegistrationAccess("E", ServerStatus.SG("registration_code"));
      if (e_level == null)
        e_level = "0"; 
      ServerStatus.thisObj.server_info.put("enterprise_level", e_level);
      ServerStatus.thisObj.server_info.put("registration_name", Common.url_decode(name));
      ServerStatus.thisObj.server_info.put("registration_email", email);
      return true;
    } 
    ServerStatus.put_in("max_max_users", "5");
    ServerStatus.put_in("max_users", "5");
    return false;
  }
}
