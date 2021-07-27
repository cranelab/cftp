package jline.console;

import jline.internal.Preconditions;

public class CursorBuffer {
  private boolean overTyping = false;
  
  public int cursor = 0;
  
  public final StringBuilder buffer = new StringBuilder();
  
  public CursorBuffer copy() {
    CursorBuffer that = new CursorBuffer();
    that.overTyping = this.overTyping;
    that.cursor = this.cursor;
    that.buffer.append(toString());
    return that;
  }
  
  public boolean isOverTyping() {
    return this.overTyping;
  }
  
  public void setOverTyping(boolean b) {
    this.overTyping = b;
  }
  
  public int length() {
    return this.buffer.length();
  }
  
  public char nextChar() {
    if (this.cursor == this.buffer.length())
      return Character.MIN_VALUE; 
    return this.buffer.charAt(this.cursor);
  }
  
  public char current() {
    if (this.cursor <= 0)
      return Character.MIN_VALUE; 
    return this.buffer.charAt(this.cursor - 1);
  }
  
  public void write(char c) {
    this.buffer.insert(this.cursor++, c);
    if (isOverTyping() && this.cursor < this.buffer.length())
      this.buffer.deleteCharAt(this.cursor); 
  }
  
  public void write(CharSequence str) {
    Preconditions.checkNotNull(str);
    if (this.buffer.length() == 0) {
      this.buffer.append(str);
    } else {
      this.buffer.insert(this.cursor, str);
    } 
    this.cursor += str.length();
    if (isOverTyping() && this.cursor < this.buffer.length())
      this.buffer.delete(this.cursor, this.cursor + str.length()); 
  }
  
  public boolean clear() {
    if (this.buffer.length() == 0)
      return false; 
    this.buffer.delete(0, this.buffer.length());
    this.cursor = 0;
    return true;
  }
  
  public String upToCursor() {
    if (this.cursor <= 0)
      return ""; 
    return this.buffer.substring(0, this.cursor);
  }
  
  public String toString() {
    return this.buffer.toString();
  }
}
