package jline.console;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.ResourceBundle;
import java.util.Stack;
import jline.DefaultTerminal2;
import jline.Terminal;
import jline.Terminal2;
import jline.TerminalFactory;
import jline.UnixTerminal;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.completer.Completer;
import jline.console.completer.CompletionHandler;
import jline.console.history.History;
import jline.console.history.MemoryHistory;
import jline.internal.Ansi;
import jline.internal.Configuration;
import jline.internal.Curses;
import jline.internal.InputStreamReader;
import jline.internal.Log;
import jline.internal.NonBlockingInputStream;
import jline.internal.Nullable;
import jline.internal.Preconditions;
import jline.internal.Urls;

public class ConsoleReader implements Closeable {
  public static final String JLINE_NOBELL = "jline.nobell";
  
  public static final String JLINE_ESC_TIMEOUT = "jline.esc.timeout";
  
  public static final String JLINE_INPUTRC = "jline.inputrc";
  
  public static final String INPUT_RC = ".inputrc";
  
  public static final String DEFAULT_INPUT_RC = "/etc/inputrc";
  
  public static final String JLINE_EXPAND_EVENTS = "jline.expandevents";
  
  public static final char BACKSPACE = '\b';
  
  public static final char RESET_LINE = '\r';
  
  public static final char KEYBOARD_BELL = '\007';
  
  public static final char NULL_MASK = '\000';
  
  public static final int TAB_WIDTH = 8;
  
  private static final ResourceBundle resources = ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName());
  
  private static final int ESCAPE = 27;
  
  private static final int READ_EXPIRED = -2;
  
  private final Terminal2 terminal;
  
  private final Writer out;
  
  private final CursorBuffer buf = new CursorBuffer();
  
  private boolean cursorOk;
  
  private String prompt;
  
  private int promptLen;
  
  private boolean expandEvents = Configuration.getBoolean("jline.expandevents", true);
  
  private boolean bellEnabled = !Configuration.getBoolean("jline.nobell", true);
  
  private boolean handleUserInterrupt = false;
  
  private boolean handleLitteralNext = true;
  
  private Character mask;
  
  private Character echoCharacter;
  
  private CursorBuffer originalBuffer = null;
  
  private StringBuffer searchTerm = null;
  
  private String previousSearchTerm = "";
  
  private int searchIndex = -1;
  
  private int parenBlinkTimeout = 500;
  
  private final StringBuilder opBuffer = new StringBuilder();
  
  private final Stack<Character> pushBackChar = new Stack<Character>();
  
  private NonBlockingInputStream in;
  
  private long escapeTimeout;
  
  private Reader reader;
  
  private char charSearchChar = Character.MIN_VALUE;
  
  private char charSearchLastInvokeChar = Character.MIN_VALUE;
  
  private char charSearchFirstInvokeChar = Character.MIN_VALUE;
  
  private String yankBuffer = "";
  
  private KillRing killRing = new KillRing();
  
  private String encoding;
  
  private boolean quotedInsert;
  
  private boolean recording;
  
  private String macro = "";
  
  private String appName;
  
  private URL inputrcUrl;
  
  private ConsoleKeys consoleKeys;
  
  private String commentBegin = null;
  
  private boolean skipLF = false;
  
  private boolean copyPasteDetection = false;
  
  private State state = State.NORMAL;
  
  public static final String JLINE_COMPLETION_THRESHOLD = "jline.completion.threshold";
  
  private final List<Completer> completers;
  
  private CompletionHandler completionHandler;
  
  private int autoprintThreshold;
  
  private boolean paginationEnabled;
  
  private History history;
  
  private boolean historyEnabled;
  
  private enum State {
    NORMAL, SEARCH, FORWARD_SEARCH, VI_YANK_TO, VI_DELETE_TO, VI_CHANGE_TO;
  }
  
  public ConsoleReader() throws IOException {
    this(null, new FileInputStream(FileDescriptor.in), System.out, null);
  }
  
  public ConsoleReader(InputStream in, OutputStream out) throws IOException {
    this(null, in, out, null);
  }
  
  public ConsoleReader(InputStream in, OutputStream out, Terminal term) throws IOException {
    this(null, in, out, term);
  }
  
  public ConsoleReader(@Nullable String appName, InputStream in, OutputStream out, @Nullable Terminal term) throws IOException {
    this(appName, in, out, term, null);
  }
  
  private void setupSigCont() {
    try {
      Class<?> signalClass = Class.forName("sun.misc.Signal");
      Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
      Object signalHandler = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { signalHandlerClass }, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
              ConsoleReader.this.terminal.init();
              try {
                ConsoleReader.this.drawLine();
                ConsoleReader.this.flush();
              } catch (IOException e) {
                e.printStackTrace();
              } 
              return null;
            }
          });
      signalClass.getMethod("handle", new Class[] { signalClass, signalHandlerClass }).invoke(null, new Object[] { signalClass.getConstructor(new Class[] { String.class }).newInstance(new Object[] { "CONT" }), signalHandler });
    } catch (ClassNotFoundException classNotFoundException) {
    
    } catch (Exception exception) {}
  }
  
  private static URL getInputRc() throws IOException {
    String path = Configuration.getString("jline.inputrc");
    if (path == null) {
      File f = new File(Configuration.getUserHome(), ".inputrc");
      if (!f.exists())
        f = new File("/etc/inputrc"); 
      return f.toURI().toURL();
    } 
    return Urls.create(path);
  }
  
  public KeyMap getKeys() {
    return this.consoleKeys.getKeys();
  }
  
  void setInput(InputStream in) throws IOException {
    this.escapeTimeout = Configuration.getLong("jline.esc.timeout", 100L);
    boolean nonBlockingEnabled = (this.escapeTimeout > 0L && this.terminal.isSupported() && in != null);
    if (this.in != null)
      this.in.shutdown(); 
    InputStream wrapped = this.terminal.wrapInIfNeeded(in);
    this.in = new NonBlockingInputStream(wrapped, nonBlockingEnabled);
    this.reader = new InputStreamReader(this.in, this.encoding);
  }
  
  public void close() {
    if (this.in != null)
      this.in.shutdown(); 
  }
  
  @Deprecated
  public void shutdown() {
    close();
  }
  
  protected void finalize() throws Throwable {
    try {
      close();
    } finally {
      super.finalize();
    } 
  }
  
  public InputStream getInput() {
    return this.in;
  }
  
  public Writer getOutput() {
    return this.out;
  }
  
  public Terminal getTerminal() {
    return this.terminal;
  }
  
  public CursorBuffer getCursorBuffer() {
    return this.buf;
  }
  
  public void setExpandEvents(boolean expand) {
    this.expandEvents = expand;
  }
  
  public boolean getExpandEvents() {
    return this.expandEvents;
  }
  
  public void setCopyPasteDetection(boolean onoff) {
    this.copyPasteDetection = onoff;
  }
  
  public boolean isCopyPasteDetectionEnabled() {
    return this.copyPasteDetection;
  }
  
  public void setBellEnabled(boolean enabled) {
    this.bellEnabled = enabled;
  }
  
  public boolean getBellEnabled() {
    return this.bellEnabled;
  }
  
  public void setHandleUserInterrupt(boolean enabled) {
    this.handleUserInterrupt = enabled;
  }
  
  public boolean getHandleUserInterrupt() {
    return this.handleUserInterrupt;
  }
  
  public void setHandleLitteralNext(boolean handleLitteralNext) {
    this.handleLitteralNext = handleLitteralNext;
  }
  
  public boolean getHandleLitteralNext() {
    return this.handleLitteralNext;
  }
  
  public void setCommentBegin(String commentBegin) {
    this.commentBegin = commentBegin;
  }
  
  public String getCommentBegin() {
    String str = this.commentBegin;
    if (str == null) {
      str = this.consoleKeys.getVariable("comment-begin");
      if (str == null)
        str = "#"; 
    } 
    return str;
  }
  
  public void setPrompt(String prompt) {
    this.prompt = prompt;
    this.promptLen = (prompt == null) ? 0 : wcwidth(Ansi.stripAnsi(lastLine(prompt)), 0);
  }
  
  public String getPrompt() {
    return this.prompt;
  }
  
  public void setEchoCharacter(Character c) {
    this.echoCharacter = c;
  }
  
  public Character getEchoCharacter() {
    return this.echoCharacter;
  }
  
  protected final boolean resetLine() throws IOException {
    if (this.buf.cursor == 0)
      return false; 
    StringBuilder killed = new StringBuilder();
    while (this.buf.cursor > 0) {
      char c = this.buf.current();
      if (c == '\000')
        break; 
      killed.append(c);
      backspace();
    } 
    String copy = killed.reverse().toString();
    this.killRing.addBackwards(copy);
    return true;
  }
  
  int wcwidth(CharSequence str, int pos) {
    return wcwidth(str, 0, str.length(), pos);
  }
  
  int wcwidth(CharSequence str, int start, int end, int pos) {
    int cur = pos;
    for (int i = start; i < end; ) {
      int ucs;
      char c1 = str.charAt(i++);
      if (!Character.isHighSurrogate(c1) || i >= end) {
        ucs = c1;
      } else {
        char c2 = str.charAt(i);
        if (Character.isLowSurrogate(c2)) {
          i++;
          ucs = Character.toCodePoint(c1, c2);
        } else {
          ucs = c1;
        } 
      } 
      cur += wcwidth(ucs, cur);
    } 
    return cur - pos;
  }
  
  int wcwidth(int ucs, int pos) {
    if (ucs == 9)
      return nextTabStop(pos); 
    if (ucs < 32)
      return 2; 
    int w = WCWidth.wcwidth(ucs);
    return (w > 0) ? w : 0;
  }
  
  int nextTabStop(int pos) {
    int tabWidth = 8;
    int width = getTerminal().getWidth();
    int mod = (pos + tabWidth - 1) % tabWidth;
    int npos = pos + tabWidth - mod;
    return (npos < width) ? (npos - pos) : (width - pos);
  }
  
  int getCursorPosition() {
    return this.promptLen + wcwidth(this.buf.buffer, 0, this.buf.cursor, this.promptLen);
  }
  
  private static String lastLine(String str) {
    if (str == null)
      return ""; 
    int last = str.lastIndexOf("\n");
    if (last >= 0)
      return str.substring(last + 1, str.length()); 
    return str;
  }
  
  public boolean setCursorPosition(int position) throws IOException {
    if (position == this.buf.cursor)
      return true; 
    return (moveCursor(position - this.buf.cursor) != 0);
  }
  
  private void setBuffer(String buffer) throws IOException {
    if (buffer.equals(this.buf.buffer.toString()))
      return; 
    int sameIndex = 0;
    int i = 0, l1 = buffer.length(), l2 = this.buf.buffer.length();
    for (; i < l1 && i < l2 && 
      buffer.charAt(i) == this.buf.buffer.charAt(i); i++)
      sameIndex++; 
    int diff = this.buf.cursor - sameIndex;
    if (diff < 0) {
      moveToEnd();
      diff = this.buf.buffer.length() - sameIndex;
    } 
    backspace(diff);
    killLine();
    this.buf.buffer.setLength(sameIndex);
    putString(buffer.substring(sameIndex));
  }
  
  private void setBuffer(CharSequence buffer) throws IOException {
    setBuffer(String.valueOf(buffer));
  }
  
  private void setBufferKeepPos(String buffer) throws IOException {
    int pos = this.buf.cursor;
    setBuffer(buffer);
    setCursorPosition(pos);
  }
  
  private void setBufferKeepPos(CharSequence buffer) throws IOException {
    setBufferKeepPos(String.valueOf(buffer));
  }
  
  public void drawLine() throws IOException {
    String prompt = getPrompt();
    if (prompt != null)
      rawPrint(prompt); 
    fmtPrint(this.buf.buffer, 0, this.buf.cursor, this.promptLen);
    drawBuffer();
  }
  
  public void redrawLine() throws IOException {
    tputs("carriage_return", new Object[0]);
    drawLine();
  }
  
  final String finishBuffer() throws IOException {
    String str = this.buf.buffer.toString();
    String historyLine = str;
    if (this.expandEvents)
      try {
        str = expandEvents(str);
        historyLine = str.replace("!", "\\!");
        historyLine = historyLine.replaceAll("^\\^", "\\\\^");
      } catch (IllegalArgumentException e) {
        Log.error(new Object[] { "Could not expand event", e });
        beep();
        this.buf.clear();
        str = "";
      }  
    if (str.length() > 0)
      if (this.mask == null && isHistoryEnabled()) {
        this.history.add(historyLine);
      } else {
        this.mask = null;
      }  
    this.history.moveToEnd();
    this.buf.buffer.setLength(0);
    this.buf.cursor = 0;
    return str;
  }
  
  protected String expandEvents(String str) throws IOException {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      switch (c) {
        case '\\':
          if (i + 1 < str.length()) {
            char nextChar = str.charAt(i + 1);
            if (nextChar == '!' || (nextChar == '^' && i == 0)) {
              c = nextChar;
              i++;
            } 
          } 
          sb.append(c);
          break;
        case '!':
          if (i + 1 < str.length()) {
            int i1, idx;
            String sc, previous;
            int lastSpace;
            String ss;
            c = str.charAt(++i);
            boolean neg = false;
            String rep = null;
            switch (c) {
              case '!':
                if (this.history.size() == 0)
                  throw new IllegalArgumentException("!!: event not found"); 
                rep = this.history.get(this.history.index() - 1).toString();
                break;
              case '#':
                sb.append(sb.toString());
                break;
              case '?':
                i1 = str.indexOf('?', i + 1);
                if (i1 < 0)
                  i1 = str.length(); 
                sc = str.substring(i + 1, i1);
                i = i1;
                idx = searchBackwards(sc);
                if (idx < 0)
                  throw new IllegalArgumentException("!?" + sc + ": event not found"); 
                rep = this.history.get(idx).toString();
                break;
              case '$':
                if (this.history.size() == 0)
                  throw new IllegalArgumentException("!$: event not found"); 
                previous = this.history.get(this.history.index() - 1).toString().trim();
                lastSpace = previous.lastIndexOf(' ');
                if (lastSpace != -1) {
                  rep = previous.substring(lastSpace + 1);
                  break;
                } 
                rep = previous;
                break;
              case '\t':
              case ' ':
                sb.append('!');
                sb.append(c);
                break;
              case '-':
                neg = true;
                i++;
              case '0':
              case '1':
              case '2':
              case '3':
              case '4':
              case '5':
              case '6':
              case '7':
              case '8':
              case '9':
                i1 = i;
                for (; i < str.length(); i++) {
                  c = str.charAt(i);
                  if (c < '0' || c > '9')
                    break; 
                } 
                idx = 0;
                try {
                  idx = Integer.parseInt(str.substring(i1, i));
                } catch (NumberFormatException e) {
                  throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                } 
                if (neg) {
                  if (idx > 0 && idx <= this.history.size()) {
                    rep = this.history.get(this.history.index() - idx).toString();
                    break;
                  } 
                  throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                } 
                if (idx > this.history.index() - this.history.size() && idx <= this.history.index()) {
                  rep = this.history.get(idx - 1).toString();
                  break;
                } 
                throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
              default:
                ss = str.substring(i);
                i = str.length();
                idx = searchBackwards(ss, this.history.index(), true);
                if (idx < 0)
                  throw new IllegalArgumentException("!" + ss + ": event not found"); 
                rep = this.history.get(idx).toString();
                break;
            } 
            if (rep != null)
              sb.append(rep); 
            break;
          } 
          sb.append(c);
          break;
        case '^':
          if (i == 0) {
            int i1 = str.indexOf('^', i + 1);
            int i2 = str.indexOf('^', i1 + 1);
            if (i2 < 0)
              i2 = str.length(); 
            if (i1 > 0 && i2 > 0) {
              String s1 = str.substring(i + 1, i1);
              String s2 = str.substring(i1 + 1, i2);
              String s = this.history.get(this.history.index() - 1).toString().replace(s1, s2);
              sb.append(s);
              i = i2 + 1;
              break;
            } 
          } 
          sb.append(c);
          break;
        default:
          sb.append(c);
          break;
      } 
    } 
    String result = sb.toString();
    if (!str.equals(result)) {
      fmtPrint(result, getCursorPosition());
      println();
      flush();
    } 
    return result;
  }
  
  public void putString(CharSequence str) throws IOException {
    int pos = getCursorPosition();
    this.buf.write(str);
    if (this.mask == null) {
      fmtPrint(str, pos);
    } else if (this.mask.charValue() != '\000') {
      rawPrint(this.mask.charValue(), str.length());
    } 
    drawBuffer();
  }
  
  private void drawBuffer(int clear) throws IOException {
    int nbChars = this.buf.length() - this.buf.cursor;
    if (this.buf.cursor != this.buf.length() || clear != 0)
      if (this.mask != null) {
        if (this.mask.charValue() != '\000') {
          rawPrint(this.mask.charValue(), nbChars);
        } else {
          nbChars = 0;
        } 
      } else {
        fmtPrint(this.buf.buffer, this.buf.cursor, this.buf.length());
      }  
    int cursorPos = this.promptLen + wcwidth(this.buf.buffer, 0, this.buf.length(), this.promptLen);
    if (this.terminal.hasWeirdWrap() && !this.cursorOk) {
      int width = this.terminal.getWidth();
      if (cursorPos > 0 && cursorPos % width == 0) {
        rawPrint(32);
        tputs("carriage_return", new Object[0]);
      } 
      this.cursorOk = true;
    } 
    clearAhead(clear, cursorPos);
    back(nbChars);
  }
  
  private void drawBuffer() throws IOException {
    drawBuffer(0);
  }
  
  private void clearAhead(int num, int pos) throws IOException {
    if (num == 0)
      return; 
    int width = this.terminal.getWidth();
    if (this.terminal.getStringCapability("clr_eol") != null) {
      int cur = pos;
      int c0 = cur % width;
      int nb = Math.min(num, width - c0);
      tputs("clr_eol", new Object[0]);
      num -= nb;
      while (num > 0) {
        int prev = cur;
        cur = cur - cur % width + width;
        moveCursorFromTo(prev, cur);
        nb = Math.min(num, width);
        tputs("clr_eol", new Object[0]);
        num -= nb;
      } 
      moveCursorFromTo(cur, pos);
    } else if (!this.terminal.getBooleanCapability("auto_right_margin")) {
      int cur = pos;
      int c0 = cur % width;
      int nb = Math.min(num, width - c0);
      rawPrint(' ', nb);
      num -= nb;
      cur += nb;
      while (num > 0) {
        moveCursorFromTo(cur++, cur);
        nb = Math.min(num, width);
        rawPrint(' ', nb);
        num -= nb;
        cur += nb;
      } 
      moveCursorFromTo(cur, pos);
    } else {
      rawPrint(' ', num);
      moveCursorFromTo(pos + num, pos);
    } 
  }
  
  protected void back(int num) throws IOException {
    if (num == 0)
      return; 
    int i0 = this.promptLen + wcwidth(this.buf.buffer, 0, this.buf.cursor, this.promptLen);
    int i1 = i0 + ((this.mask != null) ? num : wcwidth(this.buf.buffer, this.buf.cursor, this.buf.cursor + num, i0));
    moveCursorFromTo(i1, i0);
  }
  
  public void flush() throws IOException {
    this.out.flush();
  }
  
  private int backspaceAll() throws IOException {
    return backspace(2147483647);
  }
  
  private int backspace(int num) throws IOException {
    if (this.buf.cursor == 0)
      return 0; 
    int count = -moveCursor(-num);
    int clear = wcwidth(this.buf.buffer, this.buf.cursor, this.buf.cursor + count, getCursorPosition());
    this.buf.buffer.delete(this.buf.cursor, this.buf.cursor + count);
    drawBuffer(clear);
    return count;
  }
  
  public boolean backspace() throws IOException {
    return (backspace(1) == 1);
  }
  
  protected boolean moveToEnd() throws IOException {
    if (this.buf.cursor == this.buf.length())
      return true; 
    return (moveCursor(this.buf.length() - this.buf.cursor) > 0);
  }
  
  private boolean deleteCurrentCharacter() throws IOException {
    if (this.buf.length() == 0 || this.buf.cursor == this.buf.length())
      return false; 
    this.buf.buffer.deleteCharAt(this.buf.cursor);
    drawBuffer(1);
    return true;
  }
  
  private Operation viDeleteChangeYankToRemap(Operation op) {
    switch (op) {
      case VI_EOF_MAYBE:
      case ABORT:
      case BACKWARD_CHAR:
      case FORWARD_CHAR:
      case END_OF_LINE:
      case VI_MATCH:
      case VI_BEGINNING_OF_LINE_OR_ARG_DIGIT:
      case VI_ARG_DIGIT:
      case VI_PREV_WORD:
      case VI_END_WORD:
      case VI_CHAR_SEARCH:
      case VI_NEXT_WORD:
      case VI_FIRST_PRINT:
      case VI_GOTO_MARK:
      case VI_COLUMN:
      case VI_DELETE_TO:
      case VI_YANK_TO:
      case VI_CHANGE_TO:
        return op;
    } 
    return Operation.VI_MOVEMENT_MODE;
  }
  
  private boolean viRubout(int count) throws IOException {
    boolean ok = true;
    for (int i = 0; ok && i < count; i++)
      ok = backspace(); 
    return ok;
  }
  
  private boolean viDelete(int count) throws IOException {
    boolean ok = true;
    for (int i = 0; ok && i < count; i++)
      ok = deleteCurrentCharacter(); 
    return ok;
  }
  
  private boolean viChangeCase(int count) throws IOException {
    boolean ok = true;
    for (int i = 0; ok && i < count; i++) {
      ok = (this.buf.cursor < this.buf.buffer.length());
      if (ok) {
        char ch = this.buf.buffer.charAt(this.buf.cursor);
        if (Character.isUpperCase(ch)) {
          ch = Character.toLowerCase(ch);
        } else if (Character.isLowerCase(ch)) {
          ch = Character.toUpperCase(ch);
        } 
        this.buf.buffer.setCharAt(this.buf.cursor, ch);
        drawBuffer(1);
        moveCursor(1);
      } 
    } 
    return ok;
  }
  
  private boolean viChangeChar(int count, int c) throws IOException {
    if (c < 0 || c == 27 || c == 3)
      return true; 
    boolean ok = true;
    for (int i = 0; ok && i < count; i++) {
      ok = (this.buf.cursor < this.buf.buffer.length());
      if (ok) {
        this.buf.buffer.setCharAt(this.buf.cursor, (char)c);
        drawBuffer(1);
        if (i < count - 1)
          moveCursor(1); 
      } 
    } 
    return ok;
  }
  
  private boolean viPreviousWord(int count) throws IOException {
    boolean ok = true;
    if (this.buf.cursor == 0)
      return false; 
    int pos = this.buf.cursor - 1;
    for (int i = 0; pos > 0 && i < count; i++) {
      while (pos > 0 && isWhitespace(this.buf.buffer.charAt(pos)))
        pos--; 
      while (pos > 0 && !isDelimiter(this.buf.buffer.charAt(pos - 1)))
        pos--; 
      if (pos > 0 && i < count - 1)
        pos--; 
    } 
    setCursorPosition(pos);
    return ok;
  }
  
  private boolean viDeleteTo(int startPos, int endPos, boolean isChange) throws IOException {
    if (startPos == endPos)
      return true; 
    if (endPos < startPos) {
      int tmp = endPos;
      endPos = startPos;
      startPos = tmp;
    } 
    setCursorPosition(startPos);
    this.buf.cursor = startPos;
    this.buf.buffer.delete(startPos, endPos);
    drawBuffer(endPos - startPos);
    if (!isChange && startPos > 0 && startPos == this.buf.length())
      moveCursor(-1); 
    return true;
  }
  
  private boolean viYankTo(int startPos, int endPos) throws IOException {
    int cursorPos = startPos;
    if (endPos < startPos) {
      int tmp = endPos;
      endPos = startPos;
      startPos = tmp;
    } 
    if (startPos == endPos) {
      this.yankBuffer = "";
      return true;
    } 
    this.yankBuffer = this.buf.buffer.substring(startPos, endPos);
    setCursorPosition(cursorPos);
    return true;
  }
  
  private boolean viPut(int count) throws IOException {
    if (this.yankBuffer.length() == 0)
      return true; 
    if (this.buf.cursor < this.buf.buffer.length())
      moveCursor(1); 
    for (int i = 0; i < count; i++)
      putString(this.yankBuffer); 
    moveCursor(-1);
    return true;
  }
  
  private boolean viCharSearch(int count, int invokeChar, int ch) throws IOException {
    if (ch < 0 || invokeChar < 0)
      return false; 
    char searchChar = (char)ch;
    if (invokeChar == 59 || invokeChar == 44) {
      if (this.charSearchChar == '\000')
        return false; 
      if (this.charSearchLastInvokeChar == ';' || this.charSearchLastInvokeChar == ',') {
        if (this.charSearchLastInvokeChar != invokeChar)
          this.charSearchFirstInvokeChar = switchCase(this.charSearchFirstInvokeChar); 
      } else if (invokeChar == 44) {
        this.charSearchFirstInvokeChar = switchCase(this.charSearchFirstInvokeChar);
      } 
      searchChar = this.charSearchChar;
    } else {
      this.charSearchChar = searchChar;
      this.charSearchFirstInvokeChar = (char)invokeChar;
    } 
    this.charSearchLastInvokeChar = (char)invokeChar;
    boolean isForward = Character.isLowerCase(this.charSearchFirstInvokeChar);
    boolean stopBefore = (Character.toLowerCase(this.charSearchFirstInvokeChar) == 't');
    boolean ok = false;
    if (isForward) {
      while (count-- > 0) {
        int pos = this.buf.cursor + 1;
        while (pos < this.buf.buffer.length()) {
          if (this.buf.buffer.charAt(pos) == searchChar) {
            setCursorPosition(pos);
            ok = true;
            break;
          } 
          pos++;
        } 
      } 
      if (ok) {
        if (stopBefore)
          moveCursor(-1); 
        if (isInViMoveOperationState())
          moveCursor(1); 
      } 
    } else {
      while (count-- > 0) {
        int pos = this.buf.cursor - 1;
        while (pos >= 0) {
          if (this.buf.buffer.charAt(pos) == searchChar) {
            setCursorPosition(pos);
            ok = true;
            break;
          } 
          pos--;
        } 
      } 
      if (ok && stopBefore)
        moveCursor(1); 
    } 
    return ok;
  }
  
  private static char switchCase(char ch) {
    if (Character.isUpperCase(ch))
      return Character.toLowerCase(ch); 
    return Character.toUpperCase(ch);
  }
  
  private final boolean isInViMoveOperationState() {
    return (this.state == State.VI_CHANGE_TO || this.state == State.VI_DELETE_TO || this.state == State.VI_YANK_TO);
  }
  
  private boolean viNextWord(int count) throws IOException {
    int pos = this.buf.cursor;
    int end = this.buf.buffer.length();
    for (int i = 0; pos < end && i < count; i++) {
      while (pos < end && !isDelimiter(this.buf.buffer.charAt(pos)))
        pos++; 
      if (i < count - 1 || this.state != State.VI_CHANGE_TO)
        while (pos < end && isDelimiter(this.buf.buffer.charAt(pos)))
          pos++;  
    } 
    setCursorPosition(pos);
    return true;
  }
  
  private boolean viEndWord(int count) throws IOException {
    int pos = this.buf.cursor;
    int end = this.buf.buffer.length();
    for (int i = 0; pos < end && i < count; i++) {
      if (pos < end - 1 && 
        !isDelimiter(this.buf.buffer.charAt(pos)) && 
        isDelimiter(this.buf.buffer.charAt(pos + 1)))
        pos++; 
      while (pos < end && isDelimiter(this.buf.buffer.charAt(pos)))
        pos++; 
      while (pos < end - 1 && !isDelimiter(this.buf.buffer.charAt(pos + 1)))
        pos++; 
    } 
    setCursorPosition(pos);
    return true;
  }
  
  private boolean previousWord() throws IOException {
    while (isDelimiter(this.buf.current()) && moveCursor(-1) != 0);
    while (!isDelimiter(this.buf.current()) && moveCursor(-1) != 0);
    return true;
  }
  
  private boolean nextWord() throws IOException {
    while (isDelimiter(this.buf.nextChar()) && moveCursor(1) != 0);
    while (!isDelimiter(this.buf.nextChar()) && moveCursor(1) != 0);
    return true;
  }
  
  private boolean unixWordRubout(int count) throws IOException {
    boolean success = true;
    StringBuilder killed = new StringBuilder();
    for (; count > 0; count--) {
      if (this.buf.cursor == 0) {
        success = false;
        break;
      } 
      while (isWhitespace(this.buf.current())) {
        char c = this.buf.current();
        if (c == '\000')
          break; 
        killed.append(c);
        backspace();
      } 
      while (!isWhitespace(this.buf.current())) {
        char c = this.buf.current();
        if (c == '\000')
          break; 
        killed.append(c);
        backspace();
      } 
    } 
    String copy = killed.reverse().toString();
    this.killRing.addBackwards(copy);
    return success;
  }
  
  private String insertComment(boolean isViMode) throws IOException {
    String comment = getCommentBegin();
    setCursorPosition(0);
    putString(comment);
    if (isViMode)
      this.consoleKeys.setKeyMap("vi-insert"); 
    return accept();
  }
  
  private int viSearch(char searchChar) throws IOException {
    boolean isForward = (searchChar == '/');
    CursorBuffer origBuffer = this.buf.copy();
    setCursorPosition(0);
    killLine();
    putString(Character.toString(searchChar));
    flush();
    boolean isAborted = false;
    boolean isComplete = false;
    int ch = -1;
    while (!isAborted && !isComplete && (ch = readCharacter()) != -1) {
      switch (ch) {
        case 27:
          isAborted = true;
          break;
        case 8:
        case 127:
          backspace();
          if (this.buf.cursor == 0)
            isAborted = true; 
          break;
        case 10:
        case 13:
          isComplete = true;
          break;
        default:
          putString(Character.toString((char)ch));
          break;
      } 
      flush();
    } 
    if (ch == -1 || isAborted) {
      setCursorPosition(0);
      killLine();
      putString(origBuffer.buffer);
      setCursorPosition(origBuffer.cursor);
      return -1;
    } 
    String searchTerm = this.buf.buffer.substring(1);
    int idx = -1;
    int end = this.history.index();
    int start = (end <= this.history.size()) ? 0 : (end - this.history.size());
    if (isForward) {
      for (int i = start; i < end; i++) {
        if (this.history.get(i).toString().contains(searchTerm)) {
          idx = i;
          break;
        } 
      } 
    } else {
      for (int i = end - 1; i >= start; i--) {
        if (this.history.get(i).toString().contains(searchTerm)) {
          idx = i;
          break;
        } 
      } 
    } 
    if (idx == -1) {
      setCursorPosition(0);
      killLine();
      putString(origBuffer.buffer);
      setCursorPosition(0);
      return -1;
    } 
    setCursorPosition(0);
    killLine();
    putString(this.history.get(idx));
    setCursorPosition(0);
    flush();
    isComplete = false;
    while (!isComplete && (ch = readCharacter()) != -1) {
      boolean isMatch, forward = isForward;
      switch (ch) {
        case 80:
        case 112:
          forward = !isForward;
        case 78:
        case 110:
          isMatch = false;
          if (forward) {
            for (int i = idx + 1; !isMatch && i < end; i++) {
              if (this.history.get(i).toString().contains(searchTerm)) {
                idx = i;
                isMatch = true;
              } 
            } 
          } else {
            for (int i = idx - 1; !isMatch && i >= start; i--) {
              if (this.history.get(i).toString().contains(searchTerm)) {
                idx = i;
                isMatch = true;
              } 
            } 
          } 
          if (isMatch) {
            setCursorPosition(0);
            killLine();
            putString(this.history.get(idx));
            setCursorPosition(0);
          } 
          break;
        default:
          isComplete = true;
          break;
      } 
      flush();
    } 
    return ch;
  }
  
  public void setParenBlinkTimeout(int timeout) {
    this.parenBlinkTimeout = timeout;
  }
  
  private void insertClose(String s) throws IOException {
    putString(s);
    int closePosition = this.buf.cursor;
    moveCursor(-1);
    viMatch();
    if (this.in.isNonBlockingEnabled())
      this.in.peek(this.parenBlinkTimeout); 
    setCursorPosition(closePosition);
    flush();
  }
  
  private boolean viMatch() throws IOException {
    int pos = this.buf.cursor;
    if (pos == this.buf.length())
      return false; 
    int type = getBracketType(this.buf.buffer.charAt(pos));
    int move = (type < 0) ? -1 : 1;
    int count = 1;
    if (type == 0)
      return false; 
    while (count > 0) {
      pos += move;
      if (pos < 0 || pos >= this.buf.buffer.length())
        return false; 
      int curType = getBracketType(this.buf.buffer.charAt(pos));
      if (curType == type) {
        count++;
        continue;
      } 
      if (curType == -type)
        count--; 
    } 
    if (move > 0 && isInViMoveOperationState())
      pos++; 
    setCursorPosition(pos);
    flush();
    return true;
  }
  
  private static int getBracketType(char ch) {
    switch (ch) {
      case '[':
        return 1;
      case ']':
        return -1;
      case '{':
        return 2;
      case '}':
        return -2;
      case '(':
        return 3;
      case ')':
        return -3;
    } 
    return 0;
  }
  
  private boolean deletePreviousWord() throws IOException {
    StringBuilder killed = new StringBuilder();
    char c;
    while (isDelimiter(c = this.buf.current()) && 
      c != '\000') {
      killed.append(c);
      backspace();
    } 
    while (!isDelimiter(c = this.buf.current()) && 
      c != '\000') {
      killed.append(c);
      backspace();
    } 
    String copy = killed.reverse().toString();
    this.killRing.addBackwards(copy);
    return true;
  }
  
  private boolean deleteNextWord() throws IOException {
    StringBuilder killed = new StringBuilder();
    char c;
    while (isDelimiter(c = this.buf.nextChar()) && 
      c != '\000') {
      killed.append(c);
      delete();
    } 
    while (!isDelimiter(c = this.buf.nextChar()) && 
      c != '\000') {
      killed.append(c);
      delete();
    } 
    String copy = killed.toString();
    this.killRing.add(copy);
    return true;
  }
  
  private boolean capitalizeWord() throws IOException {
    boolean first = true;
    int i = 1;
    char c;
    while (this.buf.cursor + i - 1 < this.buf.length() && !isDelimiter(c = this.buf.buffer.charAt(this.buf.cursor + i - 1))) {
      this.buf.buffer.setCharAt(this.buf.cursor + i - 1, first ? Character.toUpperCase(c) : Character.toLowerCase(c));
      first = false;
      i++;
    } 
    drawBuffer();
    moveCursor(i - 1);
    return true;
  }
  
  private boolean upCaseWord() throws IOException {
    int i = 1;
    char c;
    while (this.buf.cursor + i - 1 < this.buf.length() && !isDelimiter(c = this.buf.buffer.charAt(this.buf.cursor + i - 1))) {
      this.buf.buffer.setCharAt(this.buf.cursor + i - 1, Character.toUpperCase(c));
      i++;
    } 
    drawBuffer();
    moveCursor(i - 1);
    return true;
  }
  
  private boolean downCaseWord() throws IOException {
    int i = 1;
    char c;
    while (this.buf.cursor + i - 1 < this.buf.length() && !isDelimiter(c = this.buf.buffer.charAt(this.buf.cursor + i - 1))) {
      this.buf.buffer.setCharAt(this.buf.cursor + i - 1, Character.toLowerCase(c));
      i++;
    } 
    drawBuffer();
    moveCursor(i - 1);
    return true;
  }
  
  private boolean transposeChars(int count) throws IOException {
    for (; count > 0; count--) {
      if (this.buf.cursor == 0 || this.buf.cursor == this.buf.buffer.length())
        return false; 
      int first = this.buf.cursor - 1;
      int second = this.buf.cursor;
      char tmp = this.buf.buffer.charAt(first);
      this.buf.buffer.setCharAt(first, this.buf.buffer.charAt(second));
      this.buf.buffer.setCharAt(second, tmp);
      moveInternal(-1);
      drawBuffer();
      moveInternal(2);
    } 
    return true;
  }
  
  public boolean isKeyMap(String name) {
    KeyMap map = this.consoleKeys.getKeys();
    KeyMap mapByName = this.consoleKeys.getKeyMaps().get(name);
    if (mapByName == null)
      return false; 
    return (map == mapByName);
  }
  
  public String accept() throws IOException {
    moveToEnd();
    println();
    flush();
    return finishBuffer();
  }
  
  private void abort() throws IOException {
    beep();
    this.buf.clear();
    println();
    redrawLine();
  }
  
  public int moveCursor(int num) throws IOException {
    int where = num;
    if (this.buf.cursor == 0 && where <= 0)
      return 0; 
    if (this.buf.cursor == this.buf.buffer.length() && where >= 0)
      return 0; 
    if (this.buf.cursor + where < 0) {
      where = -this.buf.cursor;
    } else if (this.buf.cursor + where > this.buf.buffer.length()) {
      where = this.buf.buffer.length() - this.buf.cursor;
    } 
    moveInternal(where);
    return where;
  }
  
  private void moveInternal(int where) throws IOException {
    int i0, i1;
    this.buf.cursor += where;
    if (this.mask == null) {
      if (where < 0) {
        i1 = this.promptLen + wcwidth(this.buf.buffer, 0, this.buf.cursor, this.promptLen);
        i0 = i1 + wcwidth(this.buf.buffer, this.buf.cursor, this.buf.cursor - where, i1);
      } else {
        i0 = this.promptLen + wcwidth(this.buf.buffer, 0, this.buf.cursor - where, this.promptLen);
        i1 = i0 + wcwidth(this.buf.buffer, this.buf.cursor - where, this.buf.cursor, i0);
      } 
    } else if (this.mask.charValue() != '\000') {
      i1 = this.promptLen + this.buf.cursor;
      i0 = i1 - where;
    } else {
      return;
    } 
    moveCursorFromTo(i0, i1);
  }
  
  private void moveCursorFromTo(int i0, int i1) throws IOException {
    if (i0 == i1)
      return; 
    int width = getTerminal().getWidth();
    int l0 = i0 / width;
    int c0 = i0 % width;
    int l1 = i1 / width;
    int c1 = i1 % width;
    if (l0 == l1 + 1) {
      if (!tputs("cursor_up", new Object[0]))
        tputs("parm_up_cursor", new Object[] { Integer.valueOf(1) }); 
    } else if (l0 > l1) {
      if (!tputs("parm_up_cursor", new Object[] { Integer.valueOf(l0 - l1) }))
        for (int i = l1; i < l0; i++)
          tputs("cursor_up", new Object[0]);  
    } else if (l0 < l1) {
      tputs("carriage_return", new Object[0]);
      rawPrint('\n', l1 - l0);
      c0 = 0;
    } 
    if (c0 == c1 - 1) {
      tputs("cursor_right", new Object[0]);
    } else if (c0 == c1 + 1) {
      tputs("cursor_left", new Object[0]);
    } else if (c0 < c1) {
      if (!tputs("parm_right_cursor", new Object[] { Integer.valueOf(c1 - c0) }))
        for (int i = c0; i < c1; i++)
          tputs("cursor_right", new Object[0]);  
    } else if (c0 > c1 && 
      !tputs("parm_left_cursor", new Object[] { Integer.valueOf(c0 - c1) })) {
      for (int i = c1; i < c0; i++)
        tputs("cursor_left", new Object[0]); 
    } 
    this.cursorOk = true;
  }
  
  public int readCharacter() throws IOException {
    return readCharacter(false);
  }
  
  public int readCharacter(boolean checkForAltKeyCombo) throws IOException {
    int c = this.reader.read();
    if (c >= 0) {
      Log.trace(new Object[] { "Keystroke: ", Integer.valueOf(c) });
      if (this.terminal.isSupported())
        clearEcho(c); 
      if (c == 27 && checkForAltKeyCombo && this.in.peek(this.escapeTimeout) >= 32) {
        int next = this.reader.read();
        next += 1000;
        return next;
      } 
    } 
    return c;
  }
  
  private int clearEcho(int c) throws IOException {
    if (!this.terminal.isEchoEnabled())
      return 0; 
    int pos = getCursorPosition();
    int num = wcwidth(c, pos);
    moveCursorFromTo(pos + num, pos);
    drawBuffer(num);
    return num;
  }
  
  public int readCharacter(char... allowed) throws IOException {
    return readCharacter(false, allowed);
  }
  
  public int readCharacter(boolean checkForAltKeyCombo, char... allowed) throws IOException {
    Arrays.sort(allowed);
    char c;
    while (Arrays.binarySearch(allowed, c = (char)readCharacter(checkForAltKeyCombo)) < 0);
    return c;
  }
  
  public Object readBinding(KeyMap keys) throws IOException {
    Object o;
    this.opBuffer.setLength(0);
    do {
      int c = this.pushBackChar.isEmpty() ? readCharacter() : ((Character)this.pushBackChar.pop()).charValue();
      if (c == -1)
        return null; 
      this.opBuffer.appendCodePoint(c);
      if (this.recording)
        this.macro += new String(Character.toChars(c)); 
      if (this.quotedInsert) {
        o = Operation.SELF_INSERT;
        this.quotedInsert = false;
      } else {
        o = keys.getBound(this.opBuffer);
      } 
      if (!this.recording && !(o instanceof KeyMap)) {
        if (o != Operation.YANK_POP && o != Operation.YANK)
          this.killRing.resetLastYank(); 
        if (o != Operation.KILL_LINE && o != Operation.KILL_WHOLE_LINE && o != Operation.BACKWARD_KILL_WORD && o != Operation.KILL_WORD && o != Operation.UNIX_LINE_DISCARD && o != Operation.UNIX_WORD_RUBOUT)
          this.killRing.resetLastKill(); 
      } 
      if (o == Operation.DO_LOWERCASE_VERSION) {
        this.opBuffer.setLength(this.opBuffer.length() - 1);
        this.opBuffer.append(Character.toLowerCase((char)c));
        o = keys.getBound(this.opBuffer);
      } 
      if (o instanceof KeyMap)
        if (c == 27 && this.pushBackChar
          .isEmpty() && this.in
          .isNonBlockingEnabled() && this.in
          .peek(this.escapeTimeout) == -2) {
          o = ((KeyMap)o).getAnotherKey();
          if (o == null || o instanceof KeyMap)
            continue; 
          this.opBuffer.setLength(0);
        } else {
          continue;
        }  
      while (o == null && this.opBuffer.length() > 0) {
        c = this.opBuffer.charAt(this.opBuffer.length() - 1);
        this.opBuffer.setLength(this.opBuffer.length() - 1);
        Object o2 = keys.getBound(this.opBuffer);
        if (o2 instanceof KeyMap) {
          o = ((KeyMap)o2).getAnotherKey();
          if (o == null)
            continue; 
          this.pushBackChar.push(Character.valueOf((char)c));
        } 
      } 
    } while (o == null || o instanceof KeyMap);
    return o;
  }
  
  public String getLastBinding() {
    return this.opBuffer.toString();
  }
  
  public String readLine() throws IOException {
    return readLine((String)null);
  }
  
  public String readLine(Character mask) throws IOException {
    return readLine(null, mask);
  }
  
  public String readLine(String prompt) throws IOException {
    return readLine(prompt, null);
  }
  
  public String readLine(String prompt, Character mask) throws IOException {
    return readLine(prompt, mask, null);
  }
  
  public boolean setKeyMap(String name) {
    return this.consoleKeys.setKeyMap(name);
  }
  
  public String getKeyMap() {
    return this.consoleKeys.getKeys().getName();
  }
  
  public String readLine(String prompt, Character mask, String buffer) throws IOException {
    int repeatCount = 0;
    this.mask = (mask != null) ? mask : this.echoCharacter;
    if (prompt != null) {
      setPrompt(prompt);
    } else {
      prompt = getPrompt();
    } 
    try {
      if (buffer != null)
        this.buf.write(buffer); 
      if (!this.terminal.isSupported())
        beforeReadLine(prompt, mask); 
      if ((buffer != null && buffer.length() > 0) || (prompt != null && prompt
        .length() > 0)) {
        drawLine();
        this.out.flush();
      } 
      if (!this.terminal.isSupported())
        return readLineSimple(); 
      if (this.handleUserInterrupt)
        this.terminal.disableInterruptCharacter(); 
      if (this.handleLitteralNext && this.terminal instanceof UnixTerminal)
        ((UnixTerminal)this.terminal).disableLitteralNextCharacter(); 
      String originalPrompt = this.prompt;
      this.state = State.NORMAL;
      boolean success = true;
      this.pushBackChar.clear();
      while (true) {
        Object o = readBinding(getKeys());
        if (o == null)
          return null; 
        int c = 0;
        if (this.opBuffer.length() > 0)
          c = this.opBuffer.codePointBefore(this.opBuffer.length()); 
        Log.trace(new Object[] { "Binding: ", o });
        if (o instanceof String) {
          String macro = (String)o;
          for (int i = 0; i < macro.length(); i++)
            this.pushBackChar.push(Character.valueOf(macro.charAt(macro.length() - 1 - i))); 
          this.opBuffer.setLength(0);
          continue;
        } 
        if (o instanceof ActionListener) {
          ((ActionListener)o).actionPerformed(null);
          this.opBuffer.setLength(0);
          continue;
        } 
        CursorBuffer oldBuf = new CursorBuffer();
        oldBuf.buffer.append(this.buf.buffer);
        oldBuf.cursor = this.buf.cursor;
        if (this.state == State.SEARCH || this.state == State.FORWARD_SEARCH) {
          int cursorDest = -1;
          switch ((Operation)o) {
            case ABORT:
              this.state = State.NORMAL;
              this.buf.clear();
              this.buf.write(this.originalBuffer.buffer);
              this.buf.cursor = this.originalBuffer.cursor;
              break;
            case REVERSE_SEARCH_HISTORY:
              this.state = State.SEARCH;
              if (this.searchTerm.length() == 0)
                this.searchTerm.append(this.previousSearchTerm); 
              if (this.searchIndex > 0)
                this.searchIndex = searchBackwards(this.searchTerm.toString(), this.searchIndex); 
              break;
            case FORWARD_SEARCH_HISTORY:
              this.state = State.FORWARD_SEARCH;
              if (this.searchTerm.length() == 0)
                this.searchTerm.append(this.previousSearchTerm); 
              if (this.searchIndex > -1 && this.searchIndex < this.history.size() - 1)
                this.searchIndex = searchForwards(this.searchTerm.toString(), this.searchIndex); 
              break;
            case BACKWARD_DELETE_CHAR:
              if (this.searchTerm.length() > 0) {
                this.searchTerm.deleteCharAt(this.searchTerm.length() - 1);
                if (this.state == State.SEARCH) {
                  this.searchIndex = searchBackwards(this.searchTerm.toString());
                  break;
                } 
                this.searchIndex = searchForwards(this.searchTerm.toString());
              } 
              break;
            case SELF_INSERT:
              this.searchTerm.appendCodePoint(c);
              if (this.state == State.SEARCH) {
                this.searchIndex = searchBackwards(this.searchTerm.toString());
                break;
              } 
              this.searchIndex = searchForwards(this.searchTerm.toString());
              break;
            default:
              if (this.searchIndex != -1) {
                this.history.moveTo(this.searchIndex);
                cursorDest = this.history.current().toString().indexOf(this.searchTerm.toString());
              } 
              if (o != Operation.ACCEPT_LINE)
                o = null; 
              this.state = State.NORMAL;
              break;
          } 
          if (this.state == State.SEARCH || this.state == State.FORWARD_SEARCH) {
            if (this.searchTerm.length() == 0) {
              if (this.state == State.SEARCH) {
                printSearchStatus("", "");
              } else {
                printForwardSearchStatus("", "");
              } 
              this.searchIndex = -1;
            } else if (this.searchIndex == -1) {
              beep();
              printSearchStatus(this.searchTerm.toString(), "");
            } else if (this.state == State.SEARCH) {
              printSearchStatus(this.searchTerm.toString(), this.history.get(this.searchIndex).toString());
            } else {
              printForwardSearchStatus(this.searchTerm.toString(), this.history.get(this.searchIndex).toString());
            } 
          } else {
            restoreLine(originalPrompt, cursorDest);
          } 
        } 
        if (this.state != State.SEARCH && this.state != State.FORWARD_SEARCH) {
          boolean isArgDigit = false;
          int count = (repeatCount == 0) ? 1 : repeatCount;
          success = true;
          if (o instanceof Operation) {
            boolean isTabLiteral;
            String str1;
            int index, i;
            String str2;
            int lastChar, searchChar;
            Operation op = (Operation)o;
            int cursorStart = this.buf.cursor;
            State origState = this.state;
            if (this.state == State.VI_CHANGE_TO || this.state == State.VI_YANK_TO || this.state == State.VI_DELETE_TO)
              op = viDeleteChangeYankToRemap(op); 
            switch (op) {
              case COMPLETE:
                isTabLiteral = false;
                if (this.copyPasteDetection && c == 9 && (
                  
                  !this.pushBackChar.isEmpty() || (this.in
                  .isNonBlockingEnabled() && this.in.peek(this.escapeTimeout) != -2)))
                  isTabLiteral = true; 
                if (!isTabLiteral) {
                  success = complete();
                  break;
                } 
                putString(this.opBuffer);
                break;
              case POSSIBLE_COMPLETIONS:
                printCompletionCandidates();
                break;
              case BEGINNING_OF_LINE:
                success = setCursorPosition(0);
                break;
              case YANK:
                success = yank();
                break;
              case YANK_POP:
                success = yankPop();
                break;
              case KILL_LINE:
                success = killLine();
                break;
              case KILL_WHOLE_LINE:
                success = (setCursorPosition(0) && killLine());
                break;
              case CLEAR_SCREEN:
                success = clearScreen();
                redrawLine();
                break;
              case OVERWRITE_MODE:
                this.buf.setOverTyping(!this.buf.isOverTyping());
                break;
              case SELF_INSERT:
                putString(this.opBuffer);
                break;
              case ACCEPT_LINE:
                str1 = accept();
                return str1;
              case ABORT:
                if (this.searchTerm == null)
                  abort(); 
                break;
              case INTERRUPT:
                if (this.handleUserInterrupt) {
                  println();
                  flush();
                  String partialLine = this.buf.buffer.toString();
                  this.buf.clear();
                  this.history.moveToEnd();
                  throw new UserInterruptException(partialLine);
                } 
                break;
              case VI_MOVE_ACCEPT_LINE:
                this.consoleKeys.setKeyMap("vi-insert");
                str1 = accept();
                return str1;
              case BACKWARD_WORD:
                success = previousWord();
                break;
              case FORWARD_WORD:
                success = nextWord();
                break;
              case PREVIOUS_HISTORY:
                success = moveHistory(false);
                break;
              case VI_PREVIOUS_HISTORY:
                success = (moveHistory(false, count) && setCursorPosition(0));
                break;
              case NEXT_HISTORY:
                success = moveHistory(true);
                break;
              case VI_NEXT_HISTORY:
                success = (moveHistory(true, count) && setCursorPosition(0));
                break;
              case BACKWARD_DELETE_CHAR:
                success = backspace();
                break;
              case EXIT_OR_DELETE_CHAR:
                if (this.buf.buffer.length() == 0) {
                  str1 = null;
                  return str1;
                } 
                success = deleteCurrentCharacter();
                break;
              case DELETE_CHAR:
                success = deleteCurrentCharacter();
                break;
              case BACKWARD_CHAR:
                success = (moveCursor(-count) != 0);
                break;
              case FORWARD_CHAR:
                success = (moveCursor(count) != 0);
                break;
              case UNIX_LINE_DISCARD:
                success = resetLine();
                break;
              case UNIX_WORD_RUBOUT:
                success = unixWordRubout(count);
                break;
              case BACKWARD_KILL_WORD:
                success = deletePreviousWord();
                break;
              case KILL_WORD:
                success = deleteNextWord();
                break;
              case BEGINNING_OF_HISTORY:
                success = this.history.moveToFirst();
                if (success)
                  setBuffer(this.history.current()); 
                break;
              case END_OF_HISTORY:
                success = this.history.moveToLast();
                if (success)
                  setBuffer(this.history.current()); 
                break;
              case HISTORY_SEARCH_BACKWARD:
                this.searchTerm = new StringBuffer(this.buf.upToCursor());
                this.searchIndex = searchBackwards(this.searchTerm.toString(), this.history.index(), true);
                if (this.searchIndex == -1) {
                  beep();
                  break;
                } 
                success = this.history.moveTo(this.searchIndex);
                if (success)
                  setBufferKeepPos(this.history.current()); 
                break;
              case HISTORY_SEARCH_FORWARD:
                this.searchTerm = new StringBuffer(this.buf.upToCursor());
                index = this.history.index() + 1;
                if (index == this.history.size()) {
                  this.history.moveToEnd();
                  setBufferKeepPos(this.searchTerm.toString());
                  break;
                } 
                if (index < this.history.size()) {
                  this.searchIndex = searchForwards(this.searchTerm.toString(), index, true);
                  if (this.searchIndex == -1) {
                    beep();
                    break;
                  } 
                  success = this.history.moveTo(this.searchIndex);
                  if (success)
                    setBufferKeepPos(this.history.current()); 
                } 
                break;
              case REVERSE_SEARCH_HISTORY:
                this.originalBuffer = new CursorBuffer();
                this.originalBuffer.write(this.buf.buffer);
                this.originalBuffer.cursor = this.buf.cursor;
                if (this.searchTerm != null)
                  this.previousSearchTerm = this.searchTerm.toString(); 
                this.searchTerm = new StringBuffer(this.buf.buffer);
                this.state = State.SEARCH;
                if (this.searchTerm.length() > 0) {
                  this.searchIndex = searchBackwards(this.searchTerm.toString());
                  if (this.searchIndex == -1)
                    beep(); 
                  printSearchStatus(this.searchTerm.toString(), (this.searchIndex > -1) ? this.history.get(this.searchIndex).toString() : "");
                  break;
                } 
                this.searchIndex = -1;
                printSearchStatus("", "");
                break;
              case FORWARD_SEARCH_HISTORY:
                this.originalBuffer = new CursorBuffer();
                this.originalBuffer.write(this.buf.buffer);
                this.originalBuffer.cursor = this.buf.cursor;
                if (this.searchTerm != null)
                  this.previousSearchTerm = this.searchTerm.toString(); 
                this.searchTerm = new StringBuffer(this.buf.buffer);
                this.state = State.FORWARD_SEARCH;
                if (this.searchTerm.length() > 0) {
                  this.searchIndex = searchForwards(this.searchTerm.toString());
                  if (this.searchIndex == -1)
                    beep(); 
                  printForwardSearchStatus(this.searchTerm.toString(), (this.searchIndex > -1) ? this.history.get(this.searchIndex).toString() : "");
                  break;
                } 
                this.searchIndex = -1;
                printForwardSearchStatus("", "");
                break;
              case CAPITALIZE_WORD:
                success = capitalizeWord();
                break;
              case UPCASE_WORD:
                success = upCaseWord();
                break;
              case DOWNCASE_WORD:
                success = downCaseWord();
                break;
              case END_OF_LINE:
                success = moveToEnd();
                break;
              case TAB_INSERT:
                putString("\t");
                break;
              case RE_READ_INIT_FILE:
                this.consoleKeys.loadKeys(this.appName, this.inputrcUrl);
                break;
              case START_KBD_MACRO:
                this.recording = true;
                break;
              case END_KBD_MACRO:
                this.recording = false;
                this.macro = this.macro.substring(0, this.macro.length() - this.opBuffer.length());
                break;
              case CALL_LAST_KBD_MACRO:
                for (i = 0; i < this.macro.length(); i++)
                  this.pushBackChar.push(Character.valueOf(this.macro.charAt(this.macro.length() - 1 - i))); 
                this.opBuffer.setLength(0);
                break;
              case VI_EDITING_MODE:
                this.consoleKeys.setKeyMap("vi-insert");
                break;
              case VI_MOVEMENT_MODE:
                if (this.state == State.NORMAL)
                  moveCursor(-1); 
                this.consoleKeys.setKeyMap("vi-move");
                break;
              case VI_INSERTION_MODE:
                this.consoleKeys.setKeyMap("vi-insert");
                break;
              case VI_APPEND_MODE:
                moveCursor(1);
                this.consoleKeys.setKeyMap("vi-insert");
                break;
              case VI_APPEND_EOL:
                success = moveToEnd();
                this.consoleKeys.setKeyMap("vi-insert");
                break;
              case VI_EOF_MAYBE:
                if (this.buf.buffer.length() == 0)
                  return null; 
                str2 = accept();
                return str2;
              case TRANSPOSE_CHARS:
                success = transposeChars(count);
                break;
              case INSERT_COMMENT:
                str2 = insertComment(false);
                return str2;
              case INSERT_CLOSE_CURLY:
                insertClose("}");
                break;
              case INSERT_CLOSE_PAREN:
                insertClose(")");
                break;
              case INSERT_CLOSE_SQUARE:
                insertClose("]");
                break;
              case VI_INSERT_COMMENT:
                str2 = insertComment(true);
                return str2;
              case VI_MATCH:
                success = viMatch();
                break;
              case VI_SEARCH:
                lastChar = viSearch(this.opBuffer.charAt(0));
                if (lastChar != -1)
                  this.pushBackChar.push(Character.valueOf((char)lastChar)); 
                break;
              case VI_ARG_DIGIT:
                repeatCount = repeatCount * 10 + this.opBuffer.charAt(0) - 48;
                isArgDigit = true;
                break;
              case VI_BEGINNING_OF_LINE_OR_ARG_DIGIT:
                if (repeatCount > 0) {
                  repeatCount = repeatCount * 10 + this.opBuffer.charAt(0) - 48;
                  isArgDigit = true;
                  break;
                } 
                success = setCursorPosition(0);
                break;
              case VI_FIRST_PRINT:
                success = (setCursorPosition(0) && viNextWord(1));
                break;
              case VI_PREV_WORD:
                success = viPreviousWord(count);
                break;
              case VI_NEXT_WORD:
                success = viNextWord(count);
                break;
              case VI_END_WORD:
                success = viEndWord(count);
                break;
              case VI_INSERT_BEG:
                success = setCursorPosition(0);
                this.consoleKeys.setKeyMap("vi-insert");
                break;
              case VI_RUBOUT:
                success = viRubout(count);
                break;
              case VI_DELETE:
                success = viDelete(count);
                break;
              case VI_DELETE_TO:
                if (this.state == State.VI_DELETE_TO) {
                  success = (setCursorPosition(0) && killLine());
                  this.state = origState = State.NORMAL;
                  break;
                } 
                this.state = State.VI_DELETE_TO;
                break;
              case VI_YANK_TO:
                if (this.state == State.VI_YANK_TO) {
                  this.yankBuffer = this.buf.buffer.toString();
                  this.state = origState = State.NORMAL;
                  break;
                } 
                this.state = State.VI_YANK_TO;
                break;
              case VI_CHANGE_TO:
                if (this.state == State.VI_CHANGE_TO) {
                  success = (setCursorPosition(0) && killLine());
                  this.state = origState = State.NORMAL;
                  this.consoleKeys.setKeyMap("vi-insert");
                  break;
                } 
                this.state = State.VI_CHANGE_TO;
                break;
              case VI_KILL_WHOLE_LINE:
                success = (setCursorPosition(0) && killLine());
                this.consoleKeys.setKeyMap("vi-insert");
                break;
              case VI_PUT:
                success = viPut(count);
                break;
              case VI_CHAR_SEARCH:
                searchChar = (c != 59 && c != 44) ? (this.pushBackChar.isEmpty() ? readCharacter() : ((Character)this.pushBackChar.pop()).charValue()) : 0;
                success = viCharSearch(count, c, searchChar);
                break;
              case VI_CHANGE_CASE:
                success = viChangeCase(count);
                break;
              case VI_CHANGE_CHAR:
                success = viChangeChar(count, this.pushBackChar.isEmpty() ? readCharacter() : ((Character)this.pushBackChar.pop()).charValue());
                break;
              case VI_DELETE_TO_EOL:
                success = viDeleteTo(this.buf.cursor, this.buf.buffer.length(), false);
                break;
              case VI_CHANGE_TO_EOL:
                success = viDeleteTo(this.buf.cursor, this.buf.buffer.length(), true);
                this.consoleKeys.setKeyMap("vi-insert");
                break;
              case EMACS_EDITING_MODE:
                this.consoleKeys.setKeyMap("emacs");
                break;
              case QUIT:
                getCursorBuffer().clear();
                return accept();
              case QUOTED_INSERT:
                this.quotedInsert = true;
                break;
              case PASTE_FROM_CLIPBOARD:
                paste();
                break;
            } 
            if (origState != State.NORMAL) {
              if (origState == State.VI_DELETE_TO) {
                success = viDeleteTo(cursorStart, this.buf.cursor, false);
              } else if (origState == State.VI_CHANGE_TO) {
                success = viDeleteTo(cursorStart, this.buf.cursor, true);
                this.consoleKeys.setKeyMap("vi-insert");
              } else if (origState == State.VI_YANK_TO) {
                success = viYankTo(cursorStart, this.buf.cursor);
              } 
              this.state = State.NORMAL;
            } 
            if (this.state == State.NORMAL && !isArgDigit)
              repeatCount = 0; 
            if (this.state != State.SEARCH && this.state != State.FORWARD_SEARCH) {
              this.originalBuffer = null;
              this.previousSearchTerm = "";
              this.searchTerm = null;
              this.searchIndex = -1;
            } 
          } 
        } 
        if (!success)
          beep(); 
        this.opBuffer.setLength(0);
        flush();
      } 
    } finally {
      if (!this.terminal.isSupported())
        afterReadLine(); 
      if (this.handleUserInterrupt)
        this.terminal.enableInterruptCharacter(); 
    } 
  }
  
  private String readLineSimple() throws IOException {
    StringBuilder buff = new StringBuilder();
    if (this.skipLF) {
      this.skipLF = false;
      int i = readCharacter();
      if (i == -1 || i == 13)
        return buff.toString(); 
      if (i != 10)
        buff.append((char)i); 
    } 
    while (true) {
      int i = readCharacter();
      if (i == -1 && buff.length() == 0)
        return null; 
      if (i == -1 || i == 10)
        return buff.toString(); 
      if (i == 13) {
        this.skipLF = true;
        return buff.toString();
      } 
      buff.append((char)i);
    } 
  }
  
  public ConsoleReader(@Nullable String appName, InputStream in, OutputStream out, @Nullable Terminal term, @Nullable String encoding) throws IOException {
    this.completers = new LinkedList<Completer>();
    this.completionHandler = new CandidateListCompletionHandler();
    this.autoprintThreshold = Configuration.getInteger("jline.completion.threshold", 100);
    this.history = new MemoryHistory();
    this.historyEnabled = true;
    this.appName = (appName != null) ? appName : "JLine";
    this.encoding = (encoding != null) ? encoding : Configuration.getEncoding();
    Terminal terminal = (term != null) ? term : TerminalFactory.get();
    this.terminal = (terminal instanceof Terminal2) ? (Terminal2)terminal : new DefaultTerminal2(terminal);
    String outEncoding = (terminal.getOutputEncoding() != null) ? terminal.getOutputEncoding() : this.encoding;
    this.out = new OutputStreamWriter(terminal.wrapOutIfNeeded(out), outEncoding);
    setInput(in);
    this.inputrcUrl = getInputRc();
    this.consoleKeys = new ConsoleKeys(this.appName, this.inputrcUrl);
    if (terminal instanceof UnixTerminal && "/dev/tty".equals(((UnixTerminal)terminal).getSettings().getTtyDevice()) && Configuration.getBoolean("jline.sigcont", false))
      setupSigCont(); 
  }
  
  public boolean addCompleter(Completer completer) {
    return this.completers.add(completer);
  }
  
  public boolean removeCompleter(Completer completer) {
    return this.completers.remove(completer);
  }
  
  public Collection<Completer> getCompleters() {
    return Collections.unmodifiableList(this.completers);
  }
  
  public void setCompletionHandler(CompletionHandler handler) {
    this.completionHandler = Preconditions.<CompletionHandler>checkNotNull(handler);
  }
  
  public CompletionHandler getCompletionHandler() {
    return this.completionHandler;
  }
  
  protected boolean complete() throws IOException {
    Completer comp;
    if (this.completers.size() == 0)
      return false; 
    List<CharSequence> candidates = new LinkedList<CharSequence>();
    String bufstr = this.buf.buffer.toString();
    int cursor = this.buf.cursor;
    int position = -1;
    Iterator<Completer> iterator = this.completers.iterator();
    do {
      comp = iterator.next();
    } while (iterator.hasNext() && (position = comp.complete(bufstr, cursor, candidates)) == -1);
    return (candidates.size() != 0 && getCompletionHandler().complete(this, candidates, position));
  }
  
  protected void printCompletionCandidates() throws IOException {
    if (this.completers.size() == 0)
      return; 
    List<CharSequence> candidates = new LinkedList<CharSequence>();
    String bufstr = this.buf.buffer.toString();
    int cursor = this.buf.cursor;
    for (Completer comp : this.completers) {
      if (comp.complete(bufstr, cursor, candidates) != -1)
        break; 
    } 
    CandidateListCompletionHandler.printCandidates(this, candidates);
    drawLine();
  }
  
  public void setAutoprintThreshold(int threshold) {
    this.autoprintThreshold = threshold;
  }
  
  public int getAutoprintThreshold() {
    return this.autoprintThreshold;
  }
  
  public void setPaginationEnabled(boolean enabled) {
    this.paginationEnabled = enabled;
  }
  
  public boolean isPaginationEnabled() {
    return this.paginationEnabled;
  }
  
  public void setHistory(History history) {
    this.history = history;
  }
  
  public History getHistory() {
    return this.history;
  }
  
  public void setHistoryEnabled(boolean enabled) {
    this.historyEnabled = enabled;
  }
  
  public boolean isHistoryEnabled() {
    return this.historyEnabled;
  }
  
  private boolean moveHistory(boolean next, int count) throws IOException {
    boolean ok = true;
    for (int i = 0; i < count && (ok = moveHistory(next)); i++);
    return ok;
  }
  
  private boolean moveHistory(boolean next) throws IOException {
    if (next && !this.history.next())
      return false; 
    if (!next && !this.history.previous())
      return false; 
    setBuffer(this.history.current());
    return true;
  }
  
  private int fmtPrint(CharSequence buff, int cursorPos) throws IOException {
    return fmtPrint(buff, 0, buff.length(), cursorPos);
  }
  
  private int fmtPrint(CharSequence buff, int start, int end) throws IOException {
    return fmtPrint(buff, start, end, getCursorPosition());
  }
  
  private int fmtPrint(CharSequence buff, int start, int end, int cursorPos) throws IOException {
    Preconditions.checkNotNull(buff);
    for (int i = start; i < end; i++) {
      char c = buff.charAt(i);
      if (c == '\t') {
        int nb = nextTabStop(cursorPos);
        cursorPos += nb;
        while (nb-- > 0)
          this.out.write(32); 
      } else if (c < ' ') {
        this.out.write(94);
        this.out.write((char)(c + 64));
        cursorPos += 2;
      } else {
        int w = WCWidth.wcwidth(c);
        if (w > 0) {
          this.out.write(c);
          cursorPos += w;
        } 
      } 
    } 
    this.cursorOk = false;
    return cursorPos;
  }
  
  public void print(CharSequence s) throws IOException {
    rawPrint(s.toString());
  }
  
  public void println(CharSequence s) throws IOException {
    print(s);
    println();
  }
  
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");
  
  private Thread maskThread;
  
  public void println() throws IOException {
    rawPrint(LINE_SEPARATOR);
  }
  
  final void rawPrint(int c) throws IOException {
    this.out.write(c);
    this.cursorOk = false;
  }
  
  final void rawPrint(String str) throws IOException {
    this.out.write(str);
    this.cursorOk = false;
  }
  
  private void rawPrint(char c, int num) throws IOException {
    for (int i = 0; i < num; i++)
      rawPrint(c); 
  }
  
  private void rawPrintln(String s) throws IOException {
    rawPrint(s);
    println();
  }
  
  public boolean delete() throws IOException {
    if (this.buf.cursor == this.buf.buffer.length())
      return false; 
    this.buf.buffer.delete(this.buf.cursor, this.buf.cursor + 1);
    drawBuffer(1);
    return true;
  }
  
  public boolean killLine() throws IOException {
    int cp = this.buf.cursor;
    int len = this.buf.buffer.length();
    if (cp >= len)
      return false; 
    int num = len - cp;
    int pos = getCursorPosition();
    int width = wcwidth(this.buf.buffer, cp, len, pos);
    clearAhead(width, pos);
    char[] killed = new char[num];
    this.buf.buffer.getChars(cp, cp + num, killed, 0);
    this.buf.buffer.delete(cp, cp + num);
    String copy = new String(killed);
    this.killRing.add(copy);
    return true;
  }
  
  public boolean yank() throws IOException {
    String yanked = this.killRing.yank();
    if (yanked == null)
      return false; 
    putString(yanked);
    return true;
  }
  
  public boolean yankPop() throws IOException {
    if (!this.killRing.lastYank())
      return false; 
    String current = this.killRing.yank();
    if (current == null)
      return false; 
    backspace(current.length());
    String yanked = this.killRing.yankPop();
    if (yanked == null)
      return false; 
    putString(yanked);
    return true;
  }
  
  public boolean clearScreen() throws IOException {
    if (!tputs("clear_screen", new Object[0]))
      println(); 
    return true;
  }
  
  public void beep() throws IOException {
    if (this.bellEnabled && 
      tputs("bell", new Object[0]))
      flush(); 
  }
  
  public boolean paste() throws IOException {
    Clipboard clipboard;
    try {
      clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    } catch (Exception e) {
      return false;
    } 
    if (clipboard == null)
      return false; 
    Transferable transferable = clipboard.getContents(null);
    if (transferable == null)
      return false; 
    try {
      String value;
      Object content = transferable.getTransferData(DataFlavor.plainTextFlavor);
      if (content == null)
        try {
          content = (new DataFlavor()).getReaderForText(transferable);
        } catch (Exception exception) {} 
      if (content == null)
        return false; 
      if (content instanceof Reader) {
        value = "";
        BufferedReader read = new BufferedReader((Reader)content);
        String line;
        while ((line = read.readLine()) != null) {
          if (value.length() > 0)
            value = value + "\n"; 
          value = value + line;
        } 
      } else {
        value = content.toString();
      } 
      if (value == null)
        return true; 
      putString(value);
      return true;
    } catch (UnsupportedFlavorException e) {
      Log.error(new Object[] { "Paste failed: ", e });
      return false;
    } 
  }
  
  public void addTriggeredAction(char c, ActionListener listener) {
    getKeys().bind(Character.toString(c), listener);
  }
  
  public void printColumns(Collection<? extends CharSequence> items) throws IOException {
    int showLines;
    if (items == null || items.isEmpty())
      return; 
    int width = getTerminal().getWidth();
    int height = getTerminal().getHeight();
    int maxWidth = 0;
    for (CharSequence item : items) {
      int len = wcwidth(Ansi.stripAnsi(item.toString()), 0);
      maxWidth = Math.max(maxWidth, len);
    } 
    maxWidth += 3;
    Log.debug(new Object[] { "Max width: ", Integer.valueOf(maxWidth) });
    if (isPaginationEnabled()) {
      showLines = height - 1;
    } else {
      showLines = Integer.MAX_VALUE;
    } 
    StringBuilder buff = new StringBuilder();
    int realLength = 0;
    for (CharSequence item : items) {
      if (realLength + maxWidth > width) {
        rawPrintln(buff.toString());
        buff.setLength(0);
        realLength = 0;
        if (--showLines == 0) {
          print(resources.getString("DISPLAY_MORE"));
          flush();
          int c = readCharacter();
          if (c == 13 || c == 10) {
            showLines = 1;
          } else if (c != 113) {
            showLines = height - 1;
          } 
          tputs("carriage_return", new Object[0]);
          if (c == 113)
            break; 
        } 
      } 
      buff.append(item.toString());
      int strippedItemLength = wcwidth(Ansi.stripAnsi(item.toString()), 0);
      for (int i = 0; i < maxWidth - strippedItemLength; i++)
        buff.append(' '); 
      realLength += maxWidth;
    } 
    if (buff.length() > 0)
      rawPrintln(buff.toString()); 
  }
  
  private void beforeReadLine(String prompt, Character mask) {
    if (mask != null && this.maskThread == null) {
      final String fullPrompt = "\r" + prompt + "                 " + "                 " + "                 " + "\r" + prompt;
      this.maskThread = new Thread() {
          public void run() {
            while (!interrupted()) {
              try {
                Writer out = ConsoleReader.this.getOutput();
                out.write(fullPrompt);
                out.flush();
                sleep(3L);
              } catch (IOException e) {
                return;
              } catch (InterruptedException e) {
                return;
              } 
            } 
          }
        };
      this.maskThread.setPriority(10);
      this.maskThread.setDaemon(true);
      this.maskThread.start();
    } 
  }
  
  private void afterReadLine() {
    if (this.maskThread != null && this.maskThread.isAlive())
      this.maskThread.interrupt(); 
    this.maskThread = null;
  }
  
  public void resetPromptLine(String prompt, String buffer, int cursorDest) throws IOException {
    moveToEnd();
    this.buf.buffer.append(this.prompt);
    int promptLength = 0;
    if (this.prompt != null)
      promptLength = this.prompt.length(); 
    this.buf.cursor += promptLength;
    setPrompt("");
    backspaceAll();
    setPrompt(prompt);
    redrawLine();
    setBuffer(buffer);
    if (cursorDest < 0)
      cursorDest = buffer.length(); 
    setCursorPosition(cursorDest);
    flush();
  }
  
  public void printSearchStatus(String searchTerm, String match) throws IOException {
    printSearchStatus(searchTerm, match, "(reverse-i-search)`");
  }
  
  public void printForwardSearchStatus(String searchTerm, String match) throws IOException {
    printSearchStatus(searchTerm, match, "(i-search)`");
  }
  
  private void printSearchStatus(String searchTerm, String match, String searchLabel) throws IOException {
    String prompt = searchLabel + searchTerm + "': ";
    int cursorDest = match.indexOf(searchTerm);
    resetPromptLine(prompt, match, cursorDest);
  }
  
  public void restoreLine(String originalPrompt, int cursorDest) throws IOException {
    String prompt = lastLine(originalPrompt);
    String buffer = this.buf.buffer.toString();
    resetPromptLine(prompt, buffer, cursorDest);
  }
  
  public int searchBackwards(String searchTerm, int startIndex) {
    return searchBackwards(searchTerm, startIndex, false);
  }
  
  public int searchBackwards(String searchTerm) {
    return searchBackwards(searchTerm, this.history.index());
  }
  
  public int searchBackwards(String searchTerm, int startIndex, boolean startsWith) {
    ListIterator<History.Entry> it = this.history.entries(startIndex);
    while (it.hasPrevious()) {
      History.Entry e = it.previous();
      if (startsWith) {
        if (e.value().toString().startsWith(searchTerm))
          return e.index(); 
        continue;
      } 
      if (e.value().toString().contains(searchTerm))
        return e.index(); 
    } 
    return -1;
  }
  
  public int searchForwards(String searchTerm, int startIndex) {
    return searchForwards(searchTerm, startIndex, false);
  }
  
  public int searchForwards(String searchTerm) {
    return searchForwards(searchTerm, this.history.index());
  }
  
  public int searchForwards(String searchTerm, int startIndex, boolean startsWith) {
    if (startIndex >= this.history.size())
      startIndex = this.history.size() - 1; 
    ListIterator<History.Entry> it = this.history.entries(startIndex);
    if (this.searchIndex != -1 && it.hasNext())
      it.next(); 
    while (it.hasNext()) {
      History.Entry e = it.next();
      if (startsWith) {
        if (e.value().toString().startsWith(searchTerm))
          return e.index(); 
        continue;
      } 
      if (e.value().toString().contains(searchTerm))
        return e.index(); 
    } 
    return -1;
  }
  
  private static boolean isDelimiter(char c) {
    return !Character.isLetterOrDigit(c);
  }
  
  private static boolean isWhitespace(char c) {
    return Character.isWhitespace(c);
  }
  
  private boolean tputs(String cap, Object... params) throws IOException {
    String str = this.terminal.getStringCapability(cap);
    if (str == null)
      return false; 
    Curses.tputs(this.out, str, params);
    return true;
  }
}
