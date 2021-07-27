package jline.console.completer;

import java.io.IOException;
import java.util.List;
import jline.console.ConsoleReader;

public interface CompletionHandler {
  boolean complete(ConsoleReader paramConsoleReader, List<CharSequence> paramList, int paramInt) throws IOException;
}
