import crushftp.server.ServerStatus;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Vector;

public class CrushFTP {
  static Class class$0;
  
  public static void main(String[] args) {
    Vector all = new Vector();
    URL[] urls = new URL[0];
    File[] files = (new File("plugins/lib/")).listFiles();
    try {
      if (!(new File("plugins/lib/")).exists()) {
        System.out.println("plugins/lib folder not found, CrushFTP may not be able to start...");
      } else {
        all.addElement((new File("CrushFTP.jar")).toURI().toURL());
        int x;
        for (x = 0; x < files.length; x++) {
          if ((!files[x].isFile() || !files[x].getName().equalsIgnoreCase("CRUSHFTPRESTART.JAR")) && 
            files[x].isFile() && files[x].getName().toUpperCase().endsWith(".JAR"))
            all.addElement(files[x].toURI().toURL()); 
        } 
        urls = new URL[all.size()];
        for (x = 0; x < all.size(); x++) {
          try {
            urls[x] = all.elementAt(x);
          } catch (Exception e) {
            e.printStackTrace();
          } 
        } 
      } 
    } catch (Exception e) {
      e.printStackTrace();
    } 
    try {
      ClassLoader loader = URLClassLoader.newInstance(urls);
      ServerStatus.clasLoader = loader;
      Class c = Class.forName("CrushFTPLauncher", true, ServerStatus.clasLoader);
      if (class$0 == null)
        try {
        
        } catch (ClassNotFoundException classNotFoundException) {
          throw new NoClassDefFoundError(null.getMessage());
        }  
      false[class$0] = class$0 = Class.forName("java.lang.Object");
      Constructor cons = (new Class[1]).getConstructor(new Class[1]);
      cons.newInstance(new Object[] { args });
    } catch (Exception e) {
      e.printStackTrace();
    } 
  }
}
