package org.fusesource.jansi;

import java.io.IOException;
import java.io.OutputStream;
import org.fusesource.jansi.internal.Kernel32;
import org.fusesource.jansi.internal.WindowsSupport;

public final class WindowsAnsiOutputStream extends AnsiOutputStream {
  private static final long console = Kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
  
  private static final short FOREGROUND_BLACK = 0;
  
  private static final short FOREGROUND_YELLOW = (short)(Kernel32.FOREGROUND_RED | Kernel32.FOREGROUND_GREEN);
  
  private static final short FOREGROUND_MAGENTA = (short)(Kernel32.FOREGROUND_BLUE | Kernel32.FOREGROUND_RED);
  
  private static final short FOREGROUND_CYAN = (short)(Kernel32.FOREGROUND_BLUE | Kernel32.FOREGROUND_GREEN);
  
  private static final short FOREGROUND_WHITE = (short)(Kernel32.FOREGROUND_RED | Kernel32.FOREGROUND_GREEN | Kernel32.FOREGROUND_BLUE);
  
  private static final short BACKGROUND_BLACK = 0;
  
  private static final short BACKGROUND_YELLOW = (short)(Kernel32.BACKGROUND_RED | Kernel32.BACKGROUND_GREEN);
  
  private static final short BACKGROUND_MAGENTA = (short)(Kernel32.BACKGROUND_BLUE | Kernel32.BACKGROUND_RED);
  
  private static final short BACKGROUND_CYAN = (short)(Kernel32.BACKGROUND_BLUE | Kernel32.BACKGROUND_GREEN);
  
  private static final short BACKGROUND_WHITE = (short)(Kernel32.BACKGROUND_RED | Kernel32.BACKGROUND_GREEN | Kernel32.BACKGROUND_BLUE);
  
  private static final short[] ANSI_FOREGROUND_COLOR_MAP = new short[] { 0, Kernel32.FOREGROUND_RED, Kernel32.FOREGROUND_GREEN, FOREGROUND_YELLOW, Kernel32.FOREGROUND_BLUE, FOREGROUND_MAGENTA, FOREGROUND_CYAN, FOREGROUND_WHITE };
  
  private static final short[] ANSI_BACKGROUND_COLOR_MAP = new short[] { 0, Kernel32.BACKGROUND_RED, Kernel32.BACKGROUND_GREEN, BACKGROUND_YELLOW, Kernel32.BACKGROUND_BLUE, BACKGROUND_MAGENTA, BACKGROUND_CYAN, BACKGROUND_WHITE };
  
  private final Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
  
  private final short originalColors;
  
  private boolean negative;
  
  private short savedX = -1;
  
  private short savedY = -1;
  
  public WindowsAnsiOutputStream(OutputStream os) throws IOException {
    super(os);
    getConsoleInfo();
    this.originalColors = this.info.attributes;
  }
  
  private void getConsoleInfo() throws IOException {
    this.out.flush();
    if (Kernel32.GetConsoleScreenBufferInfo(console, this.info) == 0)
      throw new IOException("Could not get the screen info: " + WindowsSupport.getLastErrorMessage()); 
    if (this.negative)
      this.info.attributes = invertAttributeColors(this.info.attributes); 
  }
  
  private void applyAttribute() throws IOException {
    this.out.flush();
    short attributes = this.info.attributes;
    if (this.negative)
      attributes = invertAttributeColors(attributes); 
    if (Kernel32.SetConsoleTextAttribute(console, attributes) == 0)
      throw new IOException(WindowsSupport.getLastErrorMessage()); 
  }
  
  private short invertAttributeColors(short attributes) {
    int fg = 0xF & attributes;
    fg <<= 8;
    int bg = 240 * attributes;
    bg >>= 8;
    attributes = (short)(attributes & 0xFF00 | fg | bg);
    return attributes;
  }
  
  private void applyCursorPosition() throws IOException {
    if (Kernel32.SetConsoleCursorPosition(console, this.info.cursorPosition.copy()) == 0)
      throw new IOException(WindowsSupport.getLastErrorMessage()); 
  }
  
  protected void processEraseScreen(int eraseOption) throws IOException {
    Kernel32.COORD topLeft;
    int screenLength;
    Kernel32.COORD topLeft2;
    int lengthToCursor, lengthToEnd;
    getConsoleInfo();
    int[] written = new int[1];
    switch (eraseOption) {
      case 2:
        topLeft = new Kernel32.COORD();
        topLeft.x = 0;
        topLeft.y = this.info.window.top;
        screenLength = this.info.window.height() * this.info.size.x;
        Kernel32.FillConsoleOutputAttribute(console, this.originalColors, screenLength, topLeft, written);
        Kernel32.FillConsoleOutputCharacterW(console, ' ', screenLength, topLeft, written);
        break;
      case 1:
        topLeft2 = new Kernel32.COORD();
        topLeft2.x = 0;
        topLeft2.y = this.info.window.top;
        lengthToCursor = (this.info.cursorPosition.y - this.info.window.top) * this.info.size.x + this.info.cursorPosition.x;
        Kernel32.FillConsoleOutputAttribute(console, this.originalColors, lengthToCursor, topLeft2, written);
        Kernel32.FillConsoleOutputCharacterW(console, ' ', lengthToCursor, topLeft2, written);
        break;
      case 0:
        lengthToEnd = (this.info.window.bottom - this.info.cursorPosition.y) * this.info.size.x + this.info.size.x - this.info.cursorPosition.x;
        Kernel32.FillConsoleOutputAttribute(console, this.originalColors, lengthToEnd, this.info.cursorPosition.copy(), written);
        Kernel32.FillConsoleOutputCharacterW(console, ' ', lengthToEnd, this.info.cursorPosition.copy(), written);
        break;
    } 
  }
  
  protected void processEraseLine(int eraseOption) throws IOException {
    Kernel32.COORD leftColCurrRow, leftColCurrRow2;
    int lengthToLastCol;
    getConsoleInfo();
    int[] written = new int[1];
    switch (eraseOption) {
      case 2:
        leftColCurrRow = this.info.cursorPosition.copy();
        leftColCurrRow.x = 0;
        Kernel32.FillConsoleOutputAttribute(console, this.originalColors, this.info.size.x, leftColCurrRow, written);
        Kernel32.FillConsoleOutputCharacterW(console, ' ', this.info.size.x, leftColCurrRow, written);
        break;
      case 1:
        leftColCurrRow2 = this.info.cursorPosition.copy();
        leftColCurrRow2.x = 0;
        Kernel32.FillConsoleOutputAttribute(console, this.originalColors, this.info.cursorPosition.x, leftColCurrRow2, written);
        Kernel32.FillConsoleOutputCharacterW(console, ' ', this.info.cursorPosition.x, leftColCurrRow2, written);
        break;
      case 0:
        lengthToLastCol = this.info.size.x - this.info.cursorPosition.x;
        Kernel32.FillConsoleOutputAttribute(console, this.originalColors, lengthToLastCol, this.info.cursorPosition.copy(), written);
        Kernel32.FillConsoleOutputCharacterW(console, ' ', lengthToLastCol, this.info.cursorPosition.copy(), written);
        break;
    } 
  }
  
  protected void processCursorLeft(int count) throws IOException {
    getConsoleInfo();
    this.info.cursorPosition.x = (short)Math.max(0, this.info.cursorPosition.x - count);
    applyCursorPosition();
  }
  
  protected void processCursorRight(int count) throws IOException {
    getConsoleInfo();
    this.info.cursorPosition.x = (short)Math.min(this.info.window.width(), this.info.cursorPosition.x + count);
    applyCursorPosition();
  }
  
  protected void processCursorDown(int count) throws IOException {
    getConsoleInfo();
    this.info.cursorPosition.y = (short)Math.min(this.info.size.y, this.info.cursorPosition.y + count);
    applyCursorPosition();
  }
  
  protected void processCursorUp(int count) throws IOException {
    getConsoleInfo();
    this.info.cursorPosition.y = (short)Math.max(this.info.window.top, this.info.cursorPosition.y - count);
    applyCursorPosition();
  }
  
  protected void processCursorTo(int row, int col) throws IOException {
    getConsoleInfo();
    this.info.cursorPosition.y = (short)Math.max(this.info.window.top, Math.min(this.info.size.y, this.info.window.top + row - 1));
    this.info.cursorPosition.x = (short)Math.max(0, Math.min(this.info.window.width(), col - 1));
    applyCursorPosition();
  }
  
  protected void processCursorToColumn(int x) throws IOException {
    getConsoleInfo();
    this.info.cursorPosition.x = (short)Math.max(0, Math.min(this.info.window.width(), x - 1));
    applyCursorPosition();
  }
  
  protected void processSetForegroundColor(int color, boolean bright) throws IOException {
    this.info.attributes = (short)(this.info.attributes & 0xFFFFFFF8 | ANSI_FOREGROUND_COLOR_MAP[color]);
    applyAttribute();
  }
  
  protected void processSetBackgroundColor(int color, boolean bright) throws IOException {
    this.info.attributes = (short)(this.info.attributes & 0xFFFFFF8F | ANSI_BACKGROUND_COLOR_MAP[color]);
    applyAttribute();
  }
  
  protected void processDefaultTextColor() throws IOException {
    this.info.attributes = (short)(this.info.attributes & 0xFFFFFFF0 | this.originalColors & 0xF);
    applyAttribute();
  }
  
  protected void processDefaultBackgroundColor() throws IOException {
    this.info.attributes = (short)(this.info.attributes & 0xFFFFFF0F | this.originalColors & 0xF0);
    applyAttribute();
  }
  
  protected void processAttributeRest() throws IOException {
    this.info.attributes = (short)(this.info.attributes & 0xFFFFFF00 | this.originalColors);
    this.negative = false;
    applyAttribute();
  }
  
  protected void processSetAttribute(int attribute) throws IOException {
    switch (attribute) {
      case 1:
        this.info.attributes = (short)(this.info.attributes | Kernel32.FOREGROUND_INTENSITY);
        applyAttribute();
        break;
      case 22:
        this.info.attributes = (short)(this.info.attributes & (Kernel32.FOREGROUND_INTENSITY ^ 0xFFFFFFFF));
        applyAttribute();
        break;
      case 4:
        this.info.attributes = (short)(this.info.attributes | Kernel32.BACKGROUND_INTENSITY);
        applyAttribute();
        break;
      case 24:
        this.info.attributes = (short)(this.info.attributes & (Kernel32.BACKGROUND_INTENSITY ^ 0xFFFFFFFF));
        applyAttribute();
        break;
      case 7:
        this.negative = true;
        applyAttribute();
        break;
      case 27:
        this.negative = false;
        applyAttribute();
        break;
    } 
  }
  
  protected void processSaveCursorPosition() throws IOException {
    getConsoleInfo();
    this.savedX = this.info.cursorPosition.x;
    this.savedY = this.info.cursorPosition.y;
  }
  
  protected void processRestoreCursorPosition() throws IOException {
    if (this.savedX != -1 && this.savedY != -1) {
      this.out.flush();
      this.info.cursorPosition.x = this.savedX;
      this.info.cursorPosition.y = this.savedY;
      applyCursorPosition();
    } 
  }
  
  protected void processChangeWindowTitle(String label) {
    Kernel32.SetConsoleTitle(label);
  }
}
