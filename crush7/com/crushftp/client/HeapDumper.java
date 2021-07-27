package com.crushftp.client;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;

public class HeapDumper {
  static HotSpotDiagnosticMXBean hotspotMBean = null;
  
  static Class class$0;
  
  public String dump() {
    String filename = String.valueOf(System.getProperty("crushftp.home", "./")) + "crushftp_mem_dump" + Common.makeBoundary(3) + ".hprof";
    try {
      if (hotspotMBean == null) {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        if (class$0 == null)
          try {
          
          } catch (ClassNotFoundException classNotFoundException) {
            throw new NoClassDefFoundError(null.getMessage());
          }  
        hotspotMBean = ManagementFactory.<HotSpotDiagnosticMXBean>newPlatformMXBeanProxy((MBeanServerConnection)"com.sun.management:type=HotSpotDiagnostic", (String)class$0, class$0 = Class.forName("com.sun.management.HotSpotDiagnosticMXBean"));
      } 
      hotspotMBean.dumpHeap(filename, true);
    } catch (Exception e) {
      e.printStackTrace();
      Common.log("SERVER", 0, e);
    } 
    return "Memory dumped to: " + filename;
  }
}
