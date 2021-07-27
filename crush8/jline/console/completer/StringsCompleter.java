package jline.console.completer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import jline.internal.Preconditions;

public class StringsCompleter implements Completer {
  private final SortedSet<String> strings = new TreeSet<String>();
  
  public StringsCompleter() {}
  
  public StringsCompleter(Collection<String> strings) {
    Preconditions.checkNotNull(strings);
    getStrings().addAll(strings);
  }
  
  public StringsCompleter(String... strings) {
    this(Arrays.asList(strings));
  }
  
  public Collection<String> getStrings() {
    return this.strings;
  }
  
  public int complete(String buffer, int cursor, List<CharSequence> candidates) {
    Preconditions.checkNotNull(candidates);
    if (buffer == null) {
      candidates.addAll((Collection)this.strings);
    } else {
      for (String match : this.strings.tailSet(buffer)) {
        if (!match.startsWith(buffer))
          break; 
        candidates.add(match);
      } 
    } 
    return candidates.isEmpty() ? -1 : 0;
  }
}
