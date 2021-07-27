package crushftp.server.daemon;

import crushftp.handlers.Log;
import crushftp.server.ServerStatus;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Properties;

public class CustomServer extends GenericServer {
  Object customClass = null;
  
  public CustomServer(Properties server_item) {
    super(server_item);
  }
  
  public void run() {
    this.thread = Thread.currentThread();
    this.listen_port = Integer.parseInt(this.server_item.getProperty("port"));
    this.listen_ip = this.server_item.getProperty("ip");
    serverPorts.addElement((new StringBuffer(String.valueOf(this.listen_port))).toString());
    this.startingPropertiesHash = getPropertiesHash((Properties)this.server_item.clone());
    this.started = true;
    try {
      getSocket();
      if (this.socket_created && this.die_now.length() == 0) {
        Class c = ServerStatus.clasLoader.loadClass(this.server_item.getProperty("server_class"));
        Constructor cons = c.getConstructor(new Class[] { (new Properties()).getClass() });
        this.customClass = cons.newInstance(new Object[] { this.server_item });
        Method startClass = this.customClass.getClass().getMethod("startClass", null);
        startClass.invoke(this.customClass, null);
        Method updateClass = this.customClass.getClass().getMethod("updateClass", new Class[] { (new Properties()).getClass() });
        while (this.socket_created && this.die_now.length() == 0) {
          Thread.sleep(1000L);
          updateClass.invoke(this.customClass, new Object[] { this.server_item });
        } 
      } 
    } catch (Exception e) {
      if (e.getMessage().indexOf("socket closed") < 0) {
        Log.log("SERVER", 1, e);
      } else {
        Log.log("SERVER", 3, e);
      } 
    } 
    if (this.customClass != null) {
      try {
        Method stopClass = this.customClass.getClass().getMethod("stopClass", null);
        stopClass.invoke(this.customClass, null);
      } catch (Exception e) {
        Log.log("SERVER", 1, e);
      } 
      this.customClass = null;
    } 
    this.socket_created = false;
    updateStatus();
    if (this.restart) {
      this.restart = false;
      this.die_now = new StringBuffer();
      (new Thread(this)).start();
    } 
  }
  
  public void updateStatus() {
    synchronized (updateServerStatuses) {
      if (!this.started)
        return; 
      updateStatusInit();
    } 
  }
}
