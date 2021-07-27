package jline.console.completer;

import jline.internal.Preconditions;

public class EnumCompleter extends StringsCompleter {
  public EnumCompleter(Class<? extends Enum<?>> source) {
    this(source, true);
  }
  
  public EnumCompleter(Class<? extends Enum<?>> source, boolean toLowerCase) {
    Preconditions.checkNotNull(source);
    for (Enum<?> n : (Enum[])source.getEnumConstants())
      getStrings().add(toLowerCase ? n.name().toLowerCase() : n.name()); 
  }
}
