package org.fusesource.jansi;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class AnsiOutputStream extends FilterOutputStream {
  public static final byte[] REST_CODE = resetCode();
  
  private static final int MAX_ESCAPE_SEQUENCE_LENGTH = 100;
  
  private byte[] buffer;
  
  private int pos;
  
  private int startOfValue;
  
  private final ArrayList<Object> options;
  
  private static final int LOOKING_FOR_FIRST_ESC_CHAR = 0;
  
  private static final int LOOKING_FOR_SECOND_ESC_CHAR = 1;
  
  private static final int LOOKING_FOR_NEXT_ARG = 2;
  
  private static final int LOOKING_FOR_STR_ARG_END = 3;
  
  private static final int LOOKING_FOR_INT_ARG_END = 4;
  
  private static final int LOOKING_FOR_OSC_COMMAND = 5;
  
  private static final int LOOKING_FOR_OSC_COMMAND_END = 6;
  
  private static final int LOOKING_FOR_OSC_PARAM = 7;
  
  private static final int LOOKING_FOR_ST = 8;
  
  int state;
  
  private static final int FIRST_ESC_CHAR = 27;
  
  private static final int SECOND_ESC_CHAR = 91;
  
  private static final int SECOND_OSC_CHAR = 93;
  
  private static final int BEL = 7;
  
  private static final int SECOND_ST_CHAR = 92;
  
  protected static final int ERASE_SCREEN_TO_END = 0;
  
  protected static final int ERASE_SCREEN_TO_BEGINING = 1;
  
  protected static final int ERASE_SCREEN = 2;
  
  protected static final int ERASE_LINE_TO_END = 0;
  
  protected static final int ERASE_LINE_TO_BEGINING = 1;
  
  protected static final int ERASE_LINE = 2;
  
  protected static final int ATTRIBUTE_INTENSITY_BOLD = 1;
  
  protected static final int ATTRIBUTE_INTENSITY_FAINT = 2;
  
  protected static final int ATTRIBUTE_ITALIC = 3;
  
  protected static final int ATTRIBUTE_UNDERLINE = 4;
  
  protected static final int ATTRIBUTE_BLINK_SLOW = 5;
  
  protected static final int ATTRIBUTE_BLINK_FAST = 6;
  
  protected static final int ATTRIBUTE_NEGATIVE_ON = 7;
  
  protected static final int ATTRIBUTE_CONCEAL_ON = 8;
  
  protected static final int ATTRIBUTE_UNDERLINE_DOUBLE = 21;
  
  protected static final int ATTRIBUTE_INTENSITY_NORMAL = 22;
  
  protected static final int ATTRIBUTE_UNDERLINE_OFF = 24;
  
  protected static final int ATTRIBUTE_BLINK_OFF = 25;
  
  protected static final int ATTRIBUTE_NEGATIVE_Off = 27;
  
  protected static final int ATTRIBUTE_CONCEAL_OFF = 28;
  
  protected static final int BLACK = 0;
  
  protected static final int RED = 1;
  
  protected static final int GREEN = 2;
  
  protected static final int YELLOW = 3;
  
  protected static final int BLUE = 4;
  
  protected static final int MAGENTA = 5;
  
  protected static final int CYAN = 6;
  
  protected static final int WHITE = 7;
  
  public AnsiOutputStream(OutputStream os) {
    super(os);
    this.buffer = new byte[100];
    this.pos = 0;
    this.options = new ArrayList();
    this.state = 0;
  }
  
  public void write(int data) throws IOException {
    switch (this.state) {
      case 0:
        if (data == 27) {
          this.buffer[this.pos++] = (byte)data;
          this.state = 1;
          break;
        } 
        this.out.write(data);
        break;
      case 1:
        this.buffer[this.pos++] = (byte)data;
        if (data == 91) {
          this.state = 2;
          break;
        } 
        if (data == 93) {
          this.state = 5;
          break;
        } 
        reset(false);
        break;
      case 2:
        this.buffer[this.pos++] = (byte)data;
        if (34 == data) {
          this.startOfValue = this.pos - 1;
          this.state = 3;
          break;
        } 
        if (48 <= data && data <= 57) {
          this.startOfValue = this.pos - 1;
          this.state = 4;
          break;
        } 
        if (59 == data) {
          this.options.add(null);
          break;
        } 
        if (63 == data) {
          this.options.add(new Character('?'));
          break;
        } 
        if (61 == data) {
          this.options.add(new Character('='));
          break;
        } 
        reset(processEscapeCommand(this.options, data));
        break;
      case 4:
        this.buffer[this.pos++] = (byte)data;
        if (48 > data || data > 57) {
          String strValue = new String(this.buffer, this.startOfValue, this.pos - 1 - this.startOfValue, "UTF-8");
          Integer value = new Integer(strValue);
          this.options.add(value);
          if (data == 59) {
            this.state = 2;
            break;
          } 
          reset(processEscapeCommand(this.options, data));
        } 
        break;
      case 3:
        this.buffer[this.pos++] = (byte)data;
        if (34 != data) {
          String value = new String(this.buffer, this.startOfValue, this.pos - 1 - this.startOfValue, "UTF-8");
          this.options.add(value);
          if (data == 59) {
            this.state = 2;
            break;
          } 
          reset(processEscapeCommand(this.options, data));
        } 
        break;
      case 5:
        this.buffer[this.pos++] = (byte)data;
        if (48 <= data && data <= 57) {
          this.startOfValue = this.pos - 1;
          this.state = 6;
          break;
        } 
        reset(false);
        break;
      case 6:
        this.buffer[this.pos++] = (byte)data;
        if (59 == data) {
          String strValue = new String(this.buffer, this.startOfValue, this.pos - 1 - this.startOfValue, "UTF-8");
          Integer value = new Integer(strValue);
          this.options.add(value);
          this.startOfValue = this.pos;
          this.state = 7;
          break;
        } 
        if (48 <= data && data <= 57)
          break; 
        reset(false);
        break;
      case 7:
        this.buffer[this.pos++] = (byte)data;
        if (7 == data) {
          String value = new String(this.buffer, this.startOfValue, this.pos - 1 - this.startOfValue, "UTF-8");
          this.options.add(value);
          reset(processOperatingSystemCommand(this.options));
          break;
        } 
        if (27 == data)
          this.state = 8; 
        break;
      case 8:
        this.buffer[this.pos++] = (byte)data;
        if (92 == data) {
          String value = new String(this.buffer, this.startOfValue, this.pos - 2 - this.startOfValue, "UTF-8");
          this.options.add(value);
          reset(processOperatingSystemCommand(this.options));
          break;
        } 
        this.state = 7;
        break;
    } 
    if (this.pos >= this.buffer.length)
      reset(false); 
  }
  
  private void reset(boolean skipBuffer) throws IOException {
    if (!skipBuffer)
      this.out.write(this.buffer, 0, this.pos); 
    this.pos = 0;
    this.startOfValue = 0;
    this.options.clear();
    this.state = 0;
  }
  
  private boolean processEscapeCommand(ArrayList<Object> options, int command) throws IOException {
    try {
      int count;
      switch (command) {
        case 65:
          processCursorUp(optionInt(options, 0, 1));
          return true;
        case 66:
          processCursorDown(optionInt(options, 0, 1));
          return true;
        case 67:
          processCursorRight(optionInt(options, 0, 1));
          return true;
        case 68:
          processCursorLeft(optionInt(options, 0, 1));
          return true;
        case 69:
          processCursorDownLine(optionInt(options, 0, 1));
          return true;
        case 70:
          processCursorUpLine(optionInt(options, 0, 1));
          return true;
        case 71:
          processCursorToColumn(optionInt(options, 0));
          return true;
        case 72:
        case 102:
          processCursorTo(optionInt(options, 0, 1), optionInt(options, 1, 1));
          return true;
        case 74:
          processEraseScreen(optionInt(options, 0, 0));
          return true;
        case 75:
          processEraseLine(optionInt(options, 0, 0));
          return true;
        case 83:
          processScrollUp(optionInt(options, 0, 1));
          return true;
        case 84:
          processScrollDown(optionInt(options, 0, 1));
          return true;
        case 109:
          for (Object next : options) {
            if (next != null && next.getClass() != Integer.class)
              throw new IllegalArgumentException(); 
          } 
          count = 0;
          for (Object next : options) {
            if (next != null) {
              count++;
              int value = ((Integer)next).intValue();
              if (30 <= value && value <= 37) {
                processSetForegroundColor(value - 30);
                continue;
              } 
              if (40 <= value && value <= 47) {
                processSetBackgroundColor(value - 40);
                continue;
              } 
              if (90 <= value && value <= 97) {
                processSetForegroundColor(value - 90, true);
                continue;
              } 
              if (100 <= value && value <= 107) {
                processSetBackgroundColor(value - 100, true);
                continue;
              } 
              switch (value) {
                case 39:
                  processDefaultTextColor();
                  continue;
                case 49:
                  processDefaultBackgroundColor();
                  continue;
                case 0:
                  processAttributeRest();
                  continue;
              } 
              processSetAttribute(value);
            } 
          } 
          if (count == 0)
            processAttributeRest(); 
          return true;
        case 115:
          processSaveCursorPosition();
          return true;
        case 117:
          processRestoreCursorPosition();
          return true;
      } 
      if (97 <= command && 122 <= command) {
        processUnknownExtension(options, command);
        return true;
      } 
      if (65 <= command && 90 <= command) {
        processUnknownExtension(options, command);
        return true;
      } 
      return false;
    } catch (IllegalArgumentException ignore) {
      return false;
    } 
  }
  
  private boolean processOperatingSystemCommand(ArrayList<Object> options) throws IOException {
    int command = optionInt(options, 0);
    String label = (String)options.get(1);
    try {
      switch (command) {
        case 0:
          processChangeIconNameAndWindowTitle(label);
          return true;
        case 1:
          processChangeIconName(label);
          return true;
        case 2:
          processChangeWindowTitle(label);
          return true;
      } 
      processUnknownOperatingSystemCommand(command, label);
      return true;
    } catch (IllegalArgumentException ignore) {
      return false;
    } 
  }
  
  protected void processRestoreCursorPosition() throws IOException {}
  
  protected void processSaveCursorPosition() throws IOException {}
  
  protected void processScrollDown(int optionInt) throws IOException {}
  
  protected void processScrollUp(int optionInt) throws IOException {}
  
  protected void processEraseScreen(int eraseOption) throws IOException {}
  
  protected void processEraseLine(int eraseOption) throws IOException {}
  
  protected void processSetAttribute(int attribute) throws IOException {}
  
  protected void processSetForegroundColor(int color) throws IOException {
    processSetForegroundColor(color, false);
  }
  
  protected void processSetForegroundColor(int color, boolean bright) throws IOException {}
  
  protected void processSetBackgroundColor(int color) throws IOException {
    processSetBackgroundColor(color, false);
  }
  
  protected void processSetBackgroundColor(int color, boolean bright) throws IOException {}
  
  protected void processDefaultTextColor() throws IOException {}
  
  protected void processDefaultBackgroundColor() throws IOException {}
  
  protected void processAttributeRest() throws IOException {}
  
  protected void processCursorTo(int row, int col) throws IOException {}
  
  protected void processCursorToColumn(int x) throws IOException {}
  
  protected void processCursorUpLine(int count) throws IOException {}
  
  protected void processCursorDownLine(int count) throws IOException {
    for (int i = 0; i < count; i++)
      this.out.write(10); 
  }
  
  protected void processCursorLeft(int count) throws IOException {}
  
  protected void processCursorRight(int count) throws IOException {
    for (int i = 0; i < count; i++)
      this.out.write(32); 
  }
  
  protected void processCursorDown(int count) throws IOException {}
  
  protected void processCursorUp(int count) throws IOException {}
  
  protected void processUnknownExtension(ArrayList<Object> options, int command) {}
  
  protected void processChangeIconNameAndWindowTitle(String label) {
    processChangeIconName(label);
    processChangeWindowTitle(label);
  }
  
  protected void processChangeIconName(String label) {}
  
  protected void processChangeWindowTitle(String label) {}
  
  protected void processUnknownOperatingSystemCommand(int command, String param) {}
  
  private int optionInt(ArrayList<Object> options, int index) {
    if (options.size() <= index)
      throw new IllegalArgumentException(); 
    Object value = options.get(index);
    if (value == null)
      throw new IllegalArgumentException(); 
    if (!value.getClass().equals(Integer.class))
      throw new IllegalArgumentException(); 
    return ((Integer)value).intValue();
  }
  
  private int optionInt(ArrayList<Object> options, int index, int defaultValue) {
    if (options.size() > index) {
      Object value = options.get(index);
      if (value == null)
        return defaultValue; 
      return ((Integer)value).intValue();
    } 
    return defaultValue;
  }
  
  public void close() throws IOException {
    write(REST_CODE);
    flush();
    super.close();
  }
  
  private static byte[] resetCode() {
    try {
      return (new Ansi()).reset().toString().getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } 
  }
}
