package org.boris.winrun4j;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class DDE {
  private static Set fileAssociationListeners = new LinkedHashSet();
  
  private static Set activationListeners = new LinkedHashSet();
  
  public static void ready() {
    NativeHelper.call(0L, "DDE_Ready", new long[0]);
  }
  
  public static void addFileAssocationListener(FileAssociationListener listener) {
    fileAssociationListeners.add(listener);
  }
  
  public static void execute(String command) {
    Iterator<FileAssociationListener> i = fileAssociationListeners.iterator();
    while (i.hasNext()) {
      FileAssociationListener listener = i.next();
      listener.execute(command);
    } 
  }
  
  public static void addActivationListener(ActivationListener listener) {
    activationListeners.add(listener);
  }
  
  public static void activate(String cmdLine) {
    Iterator<ActivationListener> i = activationListeners.iterator();
    while (i.hasNext()) {
      ActivationListener listener = i.next();
      listener.activate(cmdLine);
    } 
  }
}
