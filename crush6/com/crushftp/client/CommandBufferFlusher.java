package com.crushftp.client;

import java.util.Properties;
import java.util.Vector;

public class CommandBufferFlusher implements Runnable {
  int interval = 10;
  
  public static final Vector commandBuffer = new Vector();
  
  public CommandBufferFlusher(int interval) {
    this.interval = interval;
  }
  
  public void setInterval(int interval) {
    this.interval = interval;
  }
  
  public void run() {
    while (true) {
      try {
        flushBuffer();
      } catch (Exception e) {
        Common.log("SERVER", 1, e);
      } 
      if (this.interval == 0) {
        try {
          Thread.sleep(100L);
        } catch (Exception exception) {}
        continue;
      } 
      try {
        Thread.sleep((this.interval * 1000));
      } catch (Exception exception) {}
    } 
  }
  
  public static void flushBuffer() {
    synchronized (commandBuffer) {
      while (commandBuffer.size() > 0) {
        Vector paths = new Vector();
        String app = "";
        String val = "";
        String param = "";
        for (int x = commandBuffer.size() - 1; x >= 0; x--) {
          Properties p = commandBuffer.elementAt(x);
          Common.log("SERVER", 2, "OS COMMAND:" + p);
          if (val.equals("")) {
            app = p.getProperty("app");
            param = p.getProperty("param", "");
            val = p.getProperty("val");
            paths.addElement(app);
            if (!param.equals(""))
              paths.addElement(param); 
            paths.addElement(val);
          } 
          if (app.equals(p.getProperty("app")) && val.equals(p.getProperty("val")) && param.equals(p.getProperty("param", ""))) {
            commandBuffer.remove(p);
            paths.addElement(p.getProperty("path"));
          } 
          if (paths.toString().length() > 800)
            break; 
        } 
        try {
          Common.log("SERVER", 2, paths.toString());
          String[] c = new String[paths.size()];
          for (int i = 0; i < paths.size(); i++)
            c[i] = paths.elementAt(i).toString(); 
          Process proc = Runtime.getRuntime().exec(c);
          proc.waitFor();
          proc.destroy();
        } catch (Exception e) {
          Common.log("SERVER", 2, e);
          Common.log("SERVER", 2, paths.toString());
        } 
      } 
    } 
  }
}
