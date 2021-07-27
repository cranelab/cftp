package org.fusesource.jansi;

import java.util.Locale;

public class AnsiRenderer {
  public static final String BEGIN_TOKEN = "@|";
  
  private static final int BEGIN_TOKEN_LEN = 2;
  
  public static final String END_TOKEN = "|@";
  
  private static final int END_TOKEN_LEN = 2;
  
  public static final String CODE_TEXT_SEPARATOR = " ";
  
  public static final String CODE_LIST_SEPARATOR = ",";
  
  public static String render(String input) throws IllegalArgumentException {
    StringBuffer buff = new StringBuffer();
    int i = 0;
    while (true) {
      int j = input.indexOf("@|", i);
      if (j == -1) {
        if (i == 0)
          return input; 
        buff.append(input.substring(i, input.length()));
        return buff.toString();
      } 
      buff.append(input.substring(i, j));
      int k = input.indexOf("|@", j);
      if (k == -1)
        return input; 
      j += 2;
      String spec = input.substring(j, k);
      String[] items = spec.split(" ", 2);
      if (items.length == 1)
        return input; 
      String replacement = render(items[1], items[0].split(","));
      buff.append(replacement);
      i = k + 2;
    } 
  }
  
  private static String render(String text, String... codes) {
    Ansi ansi = Ansi.ansi();
    for (String name : codes) {
      Code code = Code.valueOf(name.toUpperCase(Locale.ENGLISH));
      if (code.isColor()) {
        if (code.isBackground()) {
          ansi = ansi.bg(code.getColor());
        } else {
          ansi = ansi.fg(code.getColor());
        } 
      } else if (code.isAttribute()) {
        ansi = ansi.a(code.getAttribute());
      } 
    } 
    return ansi.a(text).reset().toString();
  }
  
  public static boolean test(String text) {
    return (text != null && text.contains("@|"));
  }
  
  public enum Code {
    BLACK((String)Ansi.Color.BLACK),
    RED((String)Ansi.Color.RED),
    GREEN((String)Ansi.Color.GREEN),
    YELLOW((String)Ansi.Color.YELLOW),
    BLUE((String)Ansi.Color.BLUE),
    MAGENTA((String)Ansi.Color.MAGENTA),
    CYAN((String)Ansi.Color.CYAN),
    WHITE((String)Ansi.Color.WHITE),
    FG_BLACK((String)Ansi.Color.BLACK, false),
    FG_RED((String)Ansi.Color.RED, false),
    FG_GREEN((String)Ansi.Color.GREEN, false),
    FG_YELLOW((String)Ansi.Color.YELLOW, false),
    FG_BLUE((String)Ansi.Color.BLUE, false),
    FG_MAGENTA((String)Ansi.Color.MAGENTA, false),
    FG_CYAN((String)Ansi.Color.CYAN, false),
    FG_WHITE((String)Ansi.Color.WHITE, false),
    BG_BLACK((String)Ansi.Color.BLACK, true),
    BG_RED((String)Ansi.Color.RED, true),
    BG_GREEN((String)Ansi.Color.GREEN, true),
    BG_YELLOW((String)Ansi.Color.YELLOW, true),
    BG_BLUE((String)Ansi.Color.BLUE, true),
    BG_MAGENTA((String)Ansi.Color.MAGENTA, true),
    BG_CYAN((String)Ansi.Color.CYAN, true),
    BG_WHITE((String)Ansi.Color.WHITE, true),
    RESET((String)Ansi.Attribute.RESET),
    INTENSITY_BOLD((String)Ansi.Attribute.INTENSITY_BOLD),
    INTENSITY_FAINT((String)Ansi.Attribute.INTENSITY_FAINT),
    ITALIC((String)Ansi.Attribute.ITALIC),
    UNDERLINE((String)Ansi.Attribute.UNDERLINE),
    BLINK_SLOW((String)Ansi.Attribute.BLINK_SLOW),
    BLINK_FAST((String)Ansi.Attribute.BLINK_FAST),
    BLINK_OFF((String)Ansi.Attribute.BLINK_OFF),
    NEGATIVE_ON((String)Ansi.Attribute.NEGATIVE_ON),
    NEGATIVE_OFF((String)Ansi.Attribute.NEGATIVE_OFF),
    CONCEAL_ON((String)Ansi.Attribute.CONCEAL_ON),
    CONCEAL_OFF((String)Ansi.Attribute.CONCEAL_OFF),
    UNDERLINE_DOUBLE((String)Ansi.Attribute.UNDERLINE_DOUBLE),
    UNDERLINE_OFF((String)Ansi.Attribute.UNDERLINE_OFF),
    BOLD((String)Ansi.Attribute.INTENSITY_BOLD),
    FAINT((String)Ansi.Attribute.INTENSITY_FAINT);
    
    private final Enum n;
    
    private final boolean background;
    
    Code(Enum n, boolean background) {
      this.n = n;
      this.background = background;
    }
    
    public boolean isColor() {
      return this.n instanceof Ansi.Color;
    }
    
    public Ansi.Color getColor() {
      return (Ansi.Color)this.n;
    }
    
    public boolean isAttribute() {
      return this.n instanceof Ansi.Attribute;
    }
    
    public Ansi.Attribute getAttribute() {
      return (Ansi.Attribute)this.n;
    }
    
    public boolean isBackground() {
      return this.background;
    }
  }
}
