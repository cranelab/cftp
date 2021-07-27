package jline.console.completer;

import java.util.List;

public interface Completer {
  int complete(String paramString, int paramInt, List<CharSequence> paramList);
}
