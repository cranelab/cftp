package org.json.simple.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

class Yylex {
  public static final int YYEOF = -1;
  
  private static final int ZZ_BUFFERSIZE = 16384;
  
  public static final int YYINITIAL = 0;
  
  public static final int STRING_BEGIN = 2;
  
  private static final int[] ZZ_LEXSTATE = new int[] { 0, 0, 1, 1 };
  
  private static final String ZZ_CMAP_PACKED = "\t\000\001\007\001\007\002\000\001\007\022\000\001\007\001\000\001\t\b\000\001\006\001\031\001\002\001\004\001\n\n\003\001\032\006\000\004\001\001\005\001\001\024\000\001\027\001\b\001\030\003\000\001\022\001\013\002\001\001\021\001\f\005\000\001\023\001\000\001\r\003\000\001\016\001\024\001\017\001\020\005\000\001\025\001\000\001\026ﾂ\000";
  
  private static final char[] ZZ_CMAP = zzUnpackCMap("\t\000\001\007\001\007\002\000\001\007\022\000\001\007\001\000\001\t\b\000\001\006\001\031\001\002\001\004\001\n\n\003\001\032\006\000\004\001\001\005\001\001\024\000\001\027\001\b\001\030\003\000\001\022\001\013\002\001\001\021\001\f\005\000\001\023\001\000\001\r\003\000\001\016\001\024\001\017\001\020\005\000\001\025\001\000\001\026ﾂ\000");
  
  private static final int[] ZZ_ACTION = zzUnpackAction();
  
  private static final String ZZ_ACTION_PACKED_0 = "\002\000\002\001\001\002\001\003\001\004\003\001\001\005\001\006\001\007\001\b\001\t\001\n\001\013\001\f\001\r\005\000\001\f\001\016\001\017\001\020\001\021\001\022\001\023\001\024\001\000\001\025\001\000\001\025\004\000\001\026\001\027\002\000\001\030";
  
  private static int[] zzUnpackAction() {
    int[] result = new int[45];
    int offset = 0;
    offset = zzUnpackAction("\002\000\002\001\001\002\001\003\001\004\003\001\001\005\001\006\001\007\001\b\001\t\001\n\001\013\001\f\001\r\005\000\001\f\001\016\001\017\001\020\001\021\001\022\001\023\001\024\001\000\001\025\001\000\001\025\004\000\001\026\001\027\002\000\001\030", offset, result);
    return result;
  }
  
  private static int zzUnpackAction(String packed, int offset, int[] result) {
    int i = 0;
    int j = offset;
    int l = packed.length();
    label10: while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      while (true) {
        result[j++] = value;
        if (--count <= 0)
          continue label10; 
      } 
    } 
    return j;
  }
  
  private static final int[] ZZ_ROWMAP = zzUnpackRowMap();
  
  private static final String ZZ_ROWMAP_PACKED_0 = "\000\000\000\033\0006\000Q\000l\000\0006\000¢\000½\000Ø\0006\0006\0006\0006\0006\0006\000ó\000Ď\0006\000ĩ\000ń\000ş\000ź\000ƕ\0006\0006\0006\0006\0006\0006\0006\0006\000ư\000ǋ\000Ǧ\000Ǧ\000ȁ\000Ȝ\000ȷ\000ɒ\0006\0006\000ɭ\000ʈ\0006";
  
  private static int[] zzUnpackRowMap() {
    int[] result = new int[45];
    int offset = 0;
    offset = zzUnpackRowMap("\000\000\000\033\0006\000Q\000l\000\0006\000¢\000½\000Ø\0006\0006\0006\0006\0006\0006\000ó\000Ď\0006\000ĩ\000ń\000ş\000ź\000ƕ\0006\0006\0006\0006\0006\0006\0006\0006\000ư\000ǋ\000Ǧ\000Ǧ\000ȁ\000Ȝ\000ȷ\000ɒ\0006\0006\000ɭ\000ʈ\0006", offset, result);
    return result;
  }
  
  private static int zzUnpackRowMap(String packed, int offset, int[] result) {
    int i = 0;
    int j = offset;
    int l = packed.length();
    while (i < l) {
      int high = packed.charAt(i++) << 16;
      result[j++] = high | packed.charAt(i++);
    } 
    return j;
  }
  
  private static final int[] ZZ_TRANS = new int[] { 
      2, 2, 3, 4, 2, 2, 2, 5, 2, 6, 
      2, 2, 7, 8, 2, 9, 2, 2, 2, 2, 
      2, 10, 11, 12, 13, 14, 15, 16, 16, 16, 
      16, 16, 16, 16, 16, 17, 18, 16, 16, 16, 
      16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 
      16, 16, 16, 16, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, 4, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, 4, 19, 20, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, 20, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, 5, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      21, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, 22, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      23, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, 16, 16, 16, 16, 16, 16, 16, 
      16, -1, -1, 16, 16, 16, 16, 16, 16, 16, 
      16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 
      -1, -1, -1, -1, -1, -1, -1, -1, 24, 25, 
      26, 27, 28, 29, 30, 31, 32, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      33, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, 34, 35, -1, -1, 
      34, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      36, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, 37, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, 38, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, 39, -1, 39, -1, 39, -1, -1, 
      -1, -1, -1, 39, 39, -1, -1, -1, -1, 39, 
      39, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, 33, -1, 20, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, 20, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, 35, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, 38, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, 40, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, 41, -1, -1, -1, -1, -1, 
      -1, -1, -1, -1, -1, 42, -1, 42, -1, 42, 
      -1, -1, -1, -1, -1, 42, 42, -1, -1, -1, 
      -1, 42, 42, -1, -1, -1, -1, -1, -1, -1, 
      -1, -1, 43, -1, 43, -1, 43, -1, -1, -1, 
      -1, -1, 43, 43, -1, -1, -1, -1, 43, 43, 
      -1, -1, -1, -1, -1, -1, -1, -1, -1, 44, 
      -1, 44, -1, 44, -1, -1, -1, -1, -1, 44, 
      44, -1, -1, -1, -1, 44, 44, -1, -1, -1, 
      -1, -1, -1, -1, -1 };
  
  private static final int ZZ_UNKNOWN_ERROR = 0;
  
  private static final int ZZ_NO_MATCH = 1;
  
  private static final int ZZ_PUSHBACK_2BIG = 2;
  
  private static final String[] ZZ_ERROR_MSG = new String[] { "Unkown internal scanner error", "Error: could not match input", "Error: pushback value was too large" };
  
  private static final int[] ZZ_ATTRIBUTE = zzUnpackAttribute();
  
  private static final String ZZ_ATTRIBUTE_PACKED_0 = "\002\000\001\t\003\001\001\t\003\001\006\t\002\001\001\t\005\000\b\t\001\000\001\001\001\000\001\001\004\000\002\t\002\000\001\t";
  
  private Reader zzReader;
  
  private int zzState;
  
  private static int[] zzUnpackAttribute() {
    int[] result = new int[45];
    int offset = 0;
    offset = zzUnpackAttribute("\002\000\001\t\003\001\001\t\003\001\006\t\002\001\001\t\005\000\b\t\001\000\001\001\001\000\001\001\004\000\002\t\002\000\001\t", offset, result);
    return result;
  }
  
  private static int zzUnpackAttribute(String packed, int offset, int[] result) {
    int i = 0;
    int j = offset;
    int l = packed.length();
    label10: while (i < l) {
      int count = packed.charAt(i++);
      int value = packed.charAt(i++);
      while (true) {
        result[j++] = value;
        if (--count <= 0)
          continue label10; 
      } 
    } 
    return j;
  }
  
  private int zzLexicalState = 0;
  
  private char[] zzBuffer = new char[16384];
  
  private int zzMarkedPos;
  
  private int zzCurrentPos;
  
  private int zzStartRead;
  
  private int zzEndRead;
  
  private int yyline;
  
  private int yychar;
  
  private int yycolumn;
  
  private boolean zzAtBOL = true;
  
  private boolean zzAtEOF;
  
  private StringBuffer sb = new StringBuffer();
  
  int getPosition() {
    return this.yychar;
  }
  
  Yylex(Reader in) {
    this.zzReader = in;
  }
  
  Yylex(InputStream in) {
    this(new InputStreamReader(in));
  }
  
  private static char[] zzUnpackCMap(String packed) {
    char[] map = new char[65536];
    int i = 0;
    int j = 0;
    label10: while (i < 90) {
      int count = packed.charAt(i++);
      char value = packed.charAt(i++);
      while (true) {
        map[j++] = value;
        if (--count <= 0)
          continue label10; 
      } 
    } 
    return map;
  }
  
  private boolean zzRefill() throws IOException {
    if (this.zzStartRead > 0) {
      System.arraycopy(this.zzBuffer, this.zzStartRead, this.zzBuffer, 0, this.zzEndRead - this.zzStartRead);
      this.zzEndRead -= this.zzStartRead;
      this.zzCurrentPos -= this.zzStartRead;
      this.zzMarkedPos -= this.zzStartRead;
      this.zzStartRead = 0;
    } 
    if (this.zzCurrentPos >= this.zzBuffer.length) {
      char[] newBuffer = new char[this.zzCurrentPos * 2];
      System.arraycopy(this.zzBuffer, 0, newBuffer, 0, this.zzBuffer.length);
      this.zzBuffer = newBuffer;
    } 
    int numRead = this.zzReader.read(this.zzBuffer, this.zzEndRead, this.zzBuffer.length - this.zzEndRead);
    if (numRead > 0) {
      this.zzEndRead += numRead;
      return false;
    } 
    if (numRead == 0) {
      int c = this.zzReader.read();
      if (c == -1)
        return true; 
      this.zzBuffer[this.zzEndRead++] = (char)c;
      return false;
    } 
    return true;
  }
  
  public final void yyclose() throws IOException {
    this.zzAtEOF = true;
    this.zzEndRead = this.zzStartRead;
    if (this.zzReader != null)
      this.zzReader.close(); 
  }
  
  public final void yyreset(Reader reader) {
    this.zzReader = reader;
    this.zzAtBOL = true;
    this.zzAtEOF = false;
    this.zzEndRead = this.zzStartRead = 0;
    this.zzCurrentPos = this.zzMarkedPos = 0;
    this.yyline = this.yychar = this.yycolumn = 0;
    this.zzLexicalState = 0;
  }
  
  public final int yystate() {
    return this.zzLexicalState;
  }
  
  public final void yybegin(int newState) {
    this.zzLexicalState = newState;
  }
  
  public final String yytext() {
    return new String(this.zzBuffer, this.zzStartRead, this.zzMarkedPos - this.zzStartRead);
  }
  
  public final char yycharat(int pos) {
    return this.zzBuffer[this.zzStartRead + pos];
  }
  
  public final int yylength() {
    return this.zzMarkedPos - this.zzStartRead;
  }
  
  private void zzScanError(int errorCode) {
    String str;
    try {
      str = ZZ_ERROR_MSG[errorCode];
    } catch (ArrayIndexOutOfBoundsException e) {
      str = ZZ_ERROR_MSG[0];
    } 
    throw new Error(str);
  }
  
  public void yypushback(int number) {
    if (number > yylength())
      zzScanError(2); 
    this.zzMarkedPos -= number;
  }
  
  public Yytoken yylex() throws IOException, ParseException {
    int zzEndReadL = this.zzEndRead;
    char[] zzBufferL = this.zzBuffer;
    char[] zzCMapL = ZZ_CMAP;
    int[] zzTransL = ZZ_TRANS;
    int[] zzRowMapL = ZZ_ROWMAP;
    int[] zzAttrL = ZZ_ATTRIBUTE;
    while (true) {
      int zzInput;
      Boolean bool;
      Double double_;
      Long val;
      int zzMarkedPosL = this.zzMarkedPos;
      this.yychar += zzMarkedPosL - this.zzStartRead;
      int zzAction = -1;
      int zzCurrentPosL = this.zzCurrentPos = this.zzStartRead = zzMarkedPosL;
      this.zzState = ZZ_LEXSTATE[this.zzLexicalState];
      while (true) {
        if (zzCurrentPosL < zzEndReadL) {
          zzInput = zzBufferL[zzCurrentPosL++];
        } else {
          if (this.zzAtEOF) {
            int i = -1;
            break;
          } 
          this.zzCurrentPos = zzCurrentPosL;
          this.zzMarkedPos = zzMarkedPosL;
          boolean eof = zzRefill();
          zzCurrentPosL = this.zzCurrentPos;
          zzMarkedPosL = this.zzMarkedPos;
          zzBufferL = this.zzBuffer;
          zzEndReadL = this.zzEndRead;
          if (eof) {
            int i = -1;
            break;
          } 
          zzInput = zzBufferL[zzCurrentPosL++];
        } 
        int zzNext = zzTransL[zzRowMapL[this.zzState] + zzCMapL[zzInput]];
        if (zzNext == -1)
          break; 
        this.zzState = zzNext;
        int zzAttributes = zzAttrL[this.zzState];
        if ((zzAttributes & 0x1) == 1) {
          zzAction = this.zzState;
          zzMarkedPosL = zzCurrentPosL;
          if ((zzAttributes & 0x8) == 8)
            break; 
        } 
      } 
      this.zzMarkedPos = zzMarkedPosL;
      switch ((zzAction < 0) ? zzAction : ZZ_ACTION[zzAction]) {
        case 11:
          this.sb.append(yytext());
          continue;
        case 25:
          continue;
        case 4:
          this.sb.delete(0, this.sb.length());
          yybegin(2);
          continue;
        case 26:
          continue;
        case 16:
          this.sb.append('\b');
          continue;
        case 27:
          continue;
        case 6:
          return new Yytoken(2, null);
        case 28:
          continue;
        case 23:
          bool = Boolean.valueOf(yytext());
          return new Yytoken(0, bool);
        case 29:
          continue;
        case 22:
          return new Yytoken(0, null);
        case 30:
          continue;
        case 13:
          yybegin(0);
          return new Yytoken(0, this.sb.toString());
        case 31:
          continue;
        case 12:
          this.sb.append('\\');
          continue;
        case 32:
          continue;
        case 21:
          double_ = Double.valueOf(yytext());
          return new Yytoken(0, double_);
        case 33:
          continue;
        case 1:
          throw new ParseException(this.yychar, 0, new Character(yycharat(0)));
        case 34:
          continue;
        case 8:
          return new Yytoken(4, null);
        case 35:
          continue;
        case 19:
          this.sb.append('\r');
          continue;
        case 36:
          continue;
        case 15:
          this.sb.append('/');
          continue;
        case 37:
          continue;
        case 10:
          return new Yytoken(6, null);
        case 38:
          continue;
        case 14:
          this.sb.append('"');
          continue;
        case 39:
          continue;
        case 5:
          return new Yytoken(1, null);
        case 40:
          continue;
        case 17:
          this.sb.append('\f');
          continue;
        case 41:
          continue;
        case 24:
          try {
            int ch = Integer.parseInt(yytext().substring(2), 16);
            this.sb.append((char)ch);
          } catch (Exception e) {
            throw new ParseException(this.yychar, 2, e);
          } 
          continue;
        case 42:
          continue;
        case 20:
          this.sb.append('\t');
          continue;
        case 43:
          continue;
        case 7:
          return new Yytoken(3, null);
        case 44:
          continue;
        case 2:
          val = Long.valueOf(yytext());
          return new Yytoken(0, val);
        case 45:
          continue;
        case 18:
          this.sb.append('\n');
          continue;
        case 46:
          continue;
        case 9:
          return new Yytoken(5, null);
        case 47:
        case 3:
        case 48:
          continue;
      } 
      if (zzInput == -1 && this.zzStartRead == this.zzCurrentPos) {
        this.zzAtEOF = true;
        return null;
      } 
      zzScanError(1);
    } 
  }
}
