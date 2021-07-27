package org.fusesource.jansi;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class Ansi {
  private static final char FIRST_ESC_CHAR = '\033';
  
  private static final char SECOND_ESC_CHAR = '[';
  
  public enum Color {
    BLACK(0, "BLACK"),
    RED(1, "RED"),
    GREEN(2, "GREEN"),
    YELLOW(3, "YELLOW"),
    BLUE(4, "BLUE"),
    MAGENTA(5, "MAGENTA"),
    CYAN(6, "CYAN"),
    WHITE(7, "WHITE"),
    DEFAULT(9, "DEFAULT");
    
    private final int value;
    
    private final String name;
    
    Color(int index, String name) {
      this.value = index;
      this.name = name;
    }
    
    public String toString() {
      return this.name;
    }
    
    public int value() {
      return this.value;
    }
    
    public int fg() {
      return this.value + 30;
    }
    
    public int bg() {
      return this.value + 40;
    }
    
    public int fgBright() {
      return this.value + 90;
    }
    
    public int bgBright() {
      return this.value + 100;
    }
  }
  
  public enum Attribute {
    RESET(0, "RESET"),
    INTENSITY_BOLD(1, "INTENSITY_BOLD"),
    INTENSITY_FAINT(2, "INTENSITY_FAINT"),
    ITALIC(3, "ITALIC_ON"),
    UNDERLINE(4, "UNDERLINE_ON"),
    BLINK_SLOW(5, "BLINK_SLOW"),
    BLINK_FAST(6, "BLINK_FAST"),
    NEGATIVE_ON(7, "NEGATIVE_ON"),
    CONCEAL_ON(8, "CONCEAL_ON"),
    STRIKETHROUGH_ON(9, "STRIKETHROUGH_ON"),
    UNDERLINE_DOUBLE(21, "UNDERLINE_DOUBLE"),
    INTENSITY_BOLD_OFF(22, "INTENSITY_BOLD_OFF"),
    ITALIC_OFF(23, "ITALIC_OFF"),
    UNDERLINE_OFF(24, "UNDERLINE_OFF"),
    BLINK_OFF(25, "BLINK_OFF"),
    NEGATIVE_OFF(27, "NEGATIVE_OFF"),
    CONCEAL_OFF(28, "CONCEAL_OFF"),
    STRIKETHROUGH_OFF(29, "STRIKETHROUGH_OFF");
    
    private final int value;
    
    private final String name;
    
    Attribute(int index, String name) {
      this.value = index;
      this.name = name;
    }
    
    public String toString() {
      return this.name;
    }
    
    public int value() {
      return this.value;
    }
  }
  
  public enum Erase {
    FORWARD(0, "FORWARD"),
    BACKWARD(1, "BACKWARD"),
    ALL(2, "ALL");
    
    private final int value;
    
    private final String name;
    
    Erase(int index, String name) {
      this.value = index;
      this.name = name;
    }
    
    public String toString() {
      return this.name;
    }
    
    public int value() {
      return this.value;
    }
  }
  
  public static final String DISABLE = Ansi.class.getName() + ".disable";
  
  private static Callable<Boolean> detector = new Callable<Boolean>() {
      public Boolean call() throws Exception {
        return Boolean.valueOf(!Boolean.getBoolean(Ansi.DISABLE));
      }
    };
  
  public static void setDetector(Callable<Boolean> detector) {
    if (detector == null)
      throw new IllegalArgumentException(); 
    Ansi.detector = detector;
  }
  
  public static boolean isDetected() {
    try {
      return ((Boolean)detector.call()).booleanValue();
    } catch (Exception e) {
      return true;
    } 
  }
  
  private static final InheritableThreadLocal<Boolean> holder = new InheritableThreadLocal<Boolean>() {
      protected Boolean initialValue() {
        return Boolean.valueOf(Ansi.isDetected());
      }
    };
  
  private final StringBuilder builder;
  
  public static void setEnabled(boolean flag) {
    holder.set(Boolean.valueOf(flag));
  }
  
  public static boolean isEnabled() {
    return ((Boolean)holder.get()).booleanValue();
  }
  
  public static Ansi ansi() {
    if (isEnabled())
      return new Ansi(); 
    return new NoAnsi();
  }
  
  private static class NoAnsi extends Ansi {
    private NoAnsi() {}
    
    public Ansi fg(Ansi.Color color) {
      return this;
    }
    
    public Ansi bg(Ansi.Color color) {
      return this;
    }
    
    public Ansi fgBright(Ansi.Color color) {
      return this;
    }
    
    public Ansi bgBright(Ansi.Color color) {
      return this;
    }
    
    public Ansi a(Ansi.Attribute attribute) {
      return this;
    }
    
    public Ansi cursor(int x, int y) {
      return this;
    }
    
    public Ansi cursorToColumn(int x) {
      return this;
    }
    
    public Ansi cursorUp(int y) {
      return this;
    }
    
    public Ansi cursorRight(int x) {
      return this;
    }
    
    public Ansi cursorDown(int y) {
      return this;
    }
    
    public Ansi cursorLeft(int x) {
      return this;
    }
    
    public Ansi cursorDownLine() {
      return this;
    }
    
    public Ansi cursorDownLine(int n) {
      return this;
    }
    
    public Ansi cursorUpLine() {
      return this;
    }
    
    public Ansi cursorUpLine(int n) {
      return this;
    }
    
    public Ansi eraseScreen() {
      return this;
    }
    
    public Ansi eraseScreen(Ansi.Erase kind) {
      return this;
    }
    
    public Ansi eraseLine() {
      return this;
    }
    
    public Ansi eraseLine(Ansi.Erase kind) {
      return this;
    }
    
    public Ansi scrollUp(int rows) {
      return this;
    }
    
    public Ansi scrollDown(int rows) {
      return this;
    }
    
    public Ansi saveCursorPosition() {
      return this;
    }
    
    @Deprecated
    public Ansi restorCursorPosition() {
      return this;
    }
    
    public Ansi restoreCursorPosition() {
      return this;
    }
    
    public Ansi reset() {
      return this;
    }
  }
  
  private final ArrayList<Integer> attributeOptions = new ArrayList<Integer>(5);
  
  public Ansi() {
    this(new StringBuilder());
  }
  
  public Ansi(Ansi parent) {
    this(new StringBuilder(parent.builder));
    this.attributeOptions.addAll(parent.attributeOptions);
  }
  
  public Ansi(int size) {
    this(new StringBuilder(size));
  }
  
  public Ansi(StringBuilder builder) {
    this.builder = builder;
  }
  
  public static Ansi ansi(StringBuilder builder) {
    return new Ansi(builder);
  }
  
  public static Ansi ansi(int size) {
    return new Ansi(size);
  }
  
  public Ansi fg(Color color) {
    this.attributeOptions.add(Integer.valueOf(color.fg()));
    return this;
  }
  
  public Ansi fgBlack() {
    return fg(Color.BLACK);
  }
  
  public Ansi fgBlue() {
    return fg(Color.BLUE);
  }
  
  public Ansi fgCyan() {
    return fg(Color.CYAN);
  }
  
  public Ansi fgDefault() {
    return fg(Color.DEFAULT);
  }
  
  public Ansi fgGreen() {
    return fg(Color.GREEN);
  }
  
  public Ansi fgMagenta() {
    return fg(Color.MAGENTA);
  }
  
  public Ansi fgRed() {
    return fg(Color.RED);
  }
  
  public Ansi fgYellow() {
    return fg(Color.YELLOW);
  }
  
  public Ansi bg(Color color) {
    this.attributeOptions.add(Integer.valueOf(color.bg()));
    return this;
  }
  
  public Ansi bgCyan() {
    return fg(Color.CYAN);
  }
  
  public Ansi bgDefault() {
    return bg(Color.DEFAULT);
  }
  
  public Ansi bgGreen() {
    return bg(Color.GREEN);
  }
  
  public Ansi bgMagenta() {
    return bg(Color.MAGENTA);
  }
  
  public Ansi bgRed() {
    return bg(Color.RED);
  }
  
  public Ansi bgYellow() {
    return bg(Color.YELLOW);
  }
  
  public Ansi fgBright(Color color) {
    this.attributeOptions.add(Integer.valueOf(color.fgBright()));
    return this;
  }
  
  public Ansi fgBrightBlack() {
    return fgBright(Color.BLACK);
  }
  
  public Ansi fgBrightBlue() {
    return fgBright(Color.BLUE);
  }
  
  public Ansi fgBrightCyan() {
    return fgBright(Color.CYAN);
  }
  
  public Ansi fgBrightDefault() {
    return fgBright(Color.DEFAULT);
  }
  
  public Ansi fgBrightGreen() {
    return fgBright(Color.GREEN);
  }
  
  public Ansi fgBrightMagenta() {
    return fgBright(Color.MAGENTA);
  }
  
  public Ansi fgBrightRed() {
    return fgBright(Color.RED);
  }
  
  public Ansi fgBrightYellow() {
    return fgBright(Color.YELLOW);
  }
  
  public Ansi bgBright(Color color) {
    this.attributeOptions.add(Integer.valueOf(color.bgBright()));
    return this;
  }
  
  public Ansi bgBrightCyan() {
    return fgBright(Color.CYAN);
  }
  
  public Ansi bgBrightDefault() {
    return bgBright(Color.DEFAULT);
  }
  
  public Ansi bgBrightGreen() {
    return bgBright(Color.GREEN);
  }
  
  public Ansi bgBrightMagenta() {
    return bg(Color.MAGENTA);
  }
  
  public Ansi bgBrightRed() {
    return bgBright(Color.RED);
  }
  
  public Ansi bgBrightYellow() {
    return bgBright(Color.YELLOW);
  }
  
  public Ansi a(Attribute attribute) {
    this.attributeOptions.add(Integer.valueOf(attribute.value()));
    return this;
  }
  
  public Ansi cursor(int x, int y) {
    return appendEscapeSequence('H', new Object[] { Integer.valueOf(x), Integer.valueOf(y) });
  }
  
  public Ansi cursorToColumn(int x) {
    return appendEscapeSequence('G', x);
  }
  
  public Ansi cursorUp(int y) {
    return appendEscapeSequence('A', y);
  }
  
  public Ansi cursorDown(int y) {
    return appendEscapeSequence('B', y);
  }
  
  public Ansi cursorRight(int x) {
    return appendEscapeSequence('C', x);
  }
  
  public Ansi cursorLeft(int x) {
    return appendEscapeSequence('D', x);
  }
  
  public Ansi cursorDownLine() {
    return appendEscapeSequence('E');
  }
  
  public Ansi cursorDownLine(int n) {
    return appendEscapeSequence('E', n);
  }
  
  public Ansi cursorUpLine() {
    return appendEscapeSequence('F');
  }
  
  public Ansi cursorUpLine(int n) {
    return appendEscapeSequence('F', n);
  }
  
  public Ansi eraseScreen() {
    return appendEscapeSequence('J', Erase.ALL.value());
  }
  
  public Ansi eraseScreen(Erase kind) {
    return appendEscapeSequence('J', kind.value());
  }
  
  public Ansi eraseLine() {
    return appendEscapeSequence('K');
  }
  
  public Ansi eraseLine(Erase kind) {
    return appendEscapeSequence('K', kind.value());
  }
  
  public Ansi scrollUp(int rows) {
    return appendEscapeSequence('S', rows);
  }
  
  public Ansi scrollDown(int rows) {
    return appendEscapeSequence('T', rows);
  }
  
  public Ansi saveCursorPosition() {
    return appendEscapeSequence('s');
  }
  
  @Deprecated
  public Ansi restorCursorPosition() {
    return appendEscapeSequence('u');
  }
  
  public Ansi restoreCursorPosition() {
    return appendEscapeSequence('u');
  }
  
  public Ansi reset() {
    return a(Attribute.RESET);
  }
  
  public Ansi bold() {
    return a(Attribute.INTENSITY_BOLD);
  }
  
  public Ansi boldOff() {
    return a(Attribute.INTENSITY_BOLD_OFF);
  }
  
  public Ansi a(String value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(boolean value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(char value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(char[] value, int offset, int len) {
    flushAttributes();
    this.builder.append(value, offset, len);
    return this;
  }
  
  public Ansi a(char[] value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(CharSequence value, int start, int end) {
    flushAttributes();
    this.builder.append(value, start, end);
    return this;
  }
  
  public Ansi a(CharSequence value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(double value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(float value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(int value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(long value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(Object value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi a(StringBuffer value) {
    flushAttributes();
    this.builder.append(value);
    return this;
  }
  
  public Ansi newline() {
    flushAttributes();
    this.builder.append(System.getProperty("line.separator"));
    return this;
  }
  
  public Ansi format(String pattern, Object... args) {
    flushAttributes();
    this.builder.append(String.format(pattern, args));
    return this;
  }
  
  public Ansi render(String text) {
    a(AnsiRenderer.render(text));
    return this;
  }
  
  public Ansi render(String text, Object... args) {
    a(String.format(AnsiRenderer.render(text), args));
    return this;
  }
  
  public String toString() {
    flushAttributes();
    return this.builder.toString();
  }
  
  private Ansi appendEscapeSequence(char command) {
    flushAttributes();
    this.builder.append('\033');
    this.builder.append('[');
    this.builder.append(command);
    return this;
  }
  
  private Ansi appendEscapeSequence(char command, int option) {
    flushAttributes();
    this.builder.append('\033');
    this.builder.append('[');
    this.builder.append(option);
    this.builder.append(command);
    return this;
  }
  
  private Ansi appendEscapeSequence(char command, Object... options) {
    flushAttributes();
    return _appendEscapeSequence(command, options);
  }
  
  private void flushAttributes() {
    if (this.attributeOptions.isEmpty())
      return; 
    if (this.attributeOptions.size() == 1 && ((Integer)this.attributeOptions.get(0)).intValue() == 0) {
      this.builder.append('\033');
      this.builder.append('[');
      this.builder.append('m');
    } else {
      _appendEscapeSequence('m', this.attributeOptions.toArray());
    } 
    this.attributeOptions.clear();
  }
  
  private Ansi _appendEscapeSequence(char command, Object... options) {
    this.builder.append('\033');
    this.builder.append('[');
    int size = options.length;
    for (int i = 0; i < size; i++) {
      if (i != 0)
        this.builder.append(';'); 
      if (options[i] != null)
        this.builder.append(options[i]); 
    } 
    this.builder.append(command);
    return this;
  }
}
