package jline.console.internal;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import jline.console.ConsoleReader;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.Completer;
import jline.console.history.FileHistory;
import jline.console.history.PersistentHistory;
import jline.internal.Configuration;

public class ConsoleRunner {
  public static final String property = "jline.history";
  
  public static void main(String[] args) throws Exception {
    List<String> argList = new ArrayList<String>(Arrays.asList(args));
    if (argList.size() == 0) {
      usage();
      return;
    } 
    String historyFileName = System.getProperty("jline.history", null);
    String mainClass = argList.remove(0);
    ConsoleReader reader = new ConsoleReader();
    if (historyFileName != null) {
      reader.setHistory(new FileHistory(new File(Configuration.getUserHome(), 
              String.format(".jline-%s.%s.history", new Object[] { mainClass, historyFileName }))));
    } else {
      reader.setHistory(new FileHistory(new File(Configuration.getUserHome(), 
              String.format(".jline-%s.history", new Object[] { mainClass }))));
    } 
    String completors = System.getProperty(ConsoleRunner.class.getName() + ".completers", "");
    List<Completer> completorList = new ArrayList<Completer>();
    for (StringTokenizer tok = new StringTokenizer(completors, ","); tok.hasMoreTokens(); ) {
      Object obj = Class.forName(tok.nextToken()).newInstance();
      completorList.add((Completer)obj);
    } 
    if (completorList.size() > 0)
      reader.addCompleter(new ArgumentCompleter(completorList)); 
    ConsoleReaderInputStream.setIn(reader);
    try {
      Class<?> type = Class.forName(mainClass);
      Method method = type.getMethod("main", new Class[] { String[].class });
      String[] mainArgs = argList.<String>toArray(new String[argList.size()]);
      method.invoke(null, new Object[] { mainArgs });
    } finally {
      ConsoleReaderInputStream.restoreIn();
      if (reader.getHistory() instanceof PersistentHistory)
        ((PersistentHistory)reader.getHistory()).flush(); 
    } 
  }
  
  private static void usage() {
    System.out.println("Usage: \n   java [-Djline.history='name'] " + ConsoleRunner.class
        .getName() + " <target class name> [args]" + "\n\nThe -Djline.history option will avoid history" + "\nmangling when running ConsoleRunner on the same application." + "\n\nargs will be passed directly to the target class name.");
  }
}
