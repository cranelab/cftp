package javax.mail.internet;

public class HeaderTokenizer {
  private String string;
  
  private boolean skipComments;
  
  private String delimiters;
  
  private int currentPos;
  
  private int maxPos;
  
  private int nextPos;
  
  private int peekPos;
  
  public static final String RFC822 = "()<>@,;:\\\"\t .[]";
  
  public static final String MIME = "()<>@,;:\\\"\t []/?=";
  
  public static class Token {
    private int type;
    
    private String value;
    
    public static final int ATOM = -1;
    
    public static final int QUOTEDSTRING = -2;
    
    public static final int COMMENT = -3;
    
    public static final int EOF = -4;
    
    public Token(int type, String value) {
      this.type = type;
      this.value = value;
    }
    
    public int getType() {
      return this.type;
    }
    
    public String getValue() {
      return this.value;
    }
  }
  
  private static final Token EOFToken = new Token(-4, null);
  
  public HeaderTokenizer(String header, String delimiters, boolean skipComments) {
    this.string = (header == null) ? "" : header;
    this.skipComments = skipComments;
    this.delimiters = delimiters;
    this.currentPos = this.nextPos = this.peekPos = 0;
    this.maxPos = this.string.length();
  }
  
  public HeaderTokenizer(String header, String delimiters) {
    this(header, delimiters, true);
  }
  
  public HeaderTokenizer(String header) {
    this(header, "()<>@,;:\\\"\t .[]");
  }
  
  public Token next() throws ParseException {
    return next(false, false);
  }
  
  public Token next(char endOfAtom) throws ParseException {
    return next(endOfAtom, false);
  }
  
  public Token next(char endOfAtom, boolean keepEscapes) throws ParseException {
    this.currentPos = this.nextPos;
    Token tk = getNext(endOfAtom, keepEscapes);
    this.nextPos = this.peekPos = this.currentPos;
    return tk;
  }
  
  public Token peek() throws ParseException {
    this.currentPos = this.peekPos;
    Token tk = getNext(false, false);
    this.peekPos = this.currentPos;
    return tk;
  }
  
  public String getRemainder() {
    if (this.nextPos >= this.string.length())
      return null; 
    return this.string.substring(this.nextPos);
  }
  
  private Token getNext(char endOfAtom, boolean keepEscapes) throws ParseException {
    if (this.currentPos >= this.maxPos)
      return EOFToken; 
    if (skipWhiteSpace() == -4)
      return EOFToken; 
    boolean filter = false;
    char c = this.string.charAt(this.currentPos);
    while (c == '(') {
      int i = ++this.currentPos, nesting = 1;
      for (; nesting > 0 && this.currentPos < this.maxPos; 
        this.currentPos++) {
        c = this.string.charAt(this.currentPos);
        if (c == '\\') {
          this.currentPos++;
          filter = true;
        } else if (c == '\r') {
          filter = true;
        } else if (c == '(') {
          nesting++;
        } else if (c == ')') {
          nesting--;
        } 
      } 
      if (nesting != 0)
        throw new ParseException("Unbalanced comments"); 
      if (!this.skipComments) {
        String s;
        if (filter) {
          s = filterToken(this.string, i, this.currentPos - 1, keepEscapes);
        } else {
          s = this.string.substring(i, this.currentPos - 1);
        } 
        return new Token(-3, s);
      } 
      if (skipWhiteSpace() == -4)
        return EOFToken; 
      c = this.string.charAt(this.currentPos);
    } 
    if (c == '"') {
      this.currentPos++;
      return collectString('"', keepEscapes);
    } 
    if (c < ' ' || c >= '' || this.delimiters.indexOf(c) >= 0) {
      if (endOfAtom > '\000' && c != endOfAtom)
        return collectString(endOfAtom, keepEscapes); 
      this.currentPos++;
      char[] ch = new char[1];
      ch[0] = c;
      return new Token(c, new String(ch));
    } 
    int start;
    for (start = this.currentPos; this.currentPos < this.maxPos; this.currentPos++) {
      c = this.string.charAt(this.currentPos);
      if (c < ' ' || c >= '' || c == '(' || c == ' ' || c == '"' || this.delimiters
        .indexOf(c) >= 0) {
        if (endOfAtom > '\000' && c != endOfAtom) {
          this.currentPos = start;
          return collectString(endOfAtom, keepEscapes);
        } 
        break;
      } 
    } 
    return new Token(-1, this.string.substring(start, this.currentPos));
  }
  
  private Token collectString(char eos, boolean keepEscapes) throws ParseException {
    boolean filter = false;
    int start;
    for (start = this.currentPos; this.currentPos < this.maxPos; this.currentPos++) {
      char c = this.string.charAt(this.currentPos);
      if (c == '\\') {
        this.currentPos++;
        filter = true;
      } else if (c == '\r') {
        filter = true;
      } else if (c == eos) {
        String str;
        this.currentPos++;
        if (filter) {
          str = filterToken(this.string, start, this.currentPos - 1, keepEscapes);
        } else {
          str = this.string.substring(start, this.currentPos - 1);
        } 
        if (c != '"') {
          str = trimWhiteSpace(str);
          this.currentPos--;
        } 
        return new Token(-2, str);
      } 
    } 
    if (eos == '"')
      throw new ParseException("Unbalanced quoted string"); 
    if (filter) {
      s = filterToken(this.string, start, this.currentPos, keepEscapes);
    } else {
      s = this.string.substring(start, this.currentPos);
    } 
    String s = trimWhiteSpace(s);
    return new Token(-2, s);
  }
  
  private int skipWhiteSpace() {
    for (; this.currentPos < this.maxPos; this.currentPos++) {
      char c;
      if ((c = this.string.charAt(this.currentPos)) != ' ' && c != '\t' && c != '\r' && c != '\n')
        return this.currentPos; 
    } 
    return -4;
  }
  
  private static String trimWhiteSpace(String s) {
    char c;
    int i;
    for (i = s.length() - 1; i >= 0 && ((
      c = s.charAt(i)) == ' ' || c == '\t' || c == '\r' || c == '\n'); i--);
    if (i <= 0)
      return ""; 
    return s.substring(0, i + 1);
  }
  
  private static String filterToken(String s, int start, int end, boolean keepEscapes) {
    StringBuffer sb = new StringBuffer();
    boolean gotEscape = false;
    boolean gotCR = false;
    for (int i = start; i < end; i++) {
      char c = s.charAt(i);
      if (c == '\n' && gotCR) {
        gotCR = false;
      } else {
        gotCR = false;
        if (!gotEscape) {
          if (c == '\\') {
            gotEscape = true;
          } else if (c == '\r') {
            gotCR = true;
          } else {
            sb.append(c);
          } 
        } else {
          if (keepEscapes)
            sb.append('\\'); 
          sb.append(c);
          gotEscape = false;
        } 
      } 
    } 
    return sb.toString();
  }
}
