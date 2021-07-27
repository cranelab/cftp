package javax.mail.internet;

import com.sun.mail.util.MailLogger;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;

public class MailDateFormat extends SimpleDateFormat {
  private static final long serialVersionUID = -8148227605210628779L;
  
  private static final String PATTERN = "EEE, d MMM yyyy HH:mm:ss Z (z)";
  
  private static final MailLogger LOGGER = new MailLogger(MailDateFormat.class, "DEBUG", false, System.out);
  
  private static final int UNKNOWN_DAY_NAME = -1;
  
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
  
  private static final int LEAP_SECOND = 60;
  
  public MailDateFormat() {
    super("EEE, d MMM yyyy HH:mm:ss Z (z)", Locale.US);
  }
  
  private Object writeReplace() throws ObjectStreamException {
    MailDateFormat fmt = new MailDateFormat();
    fmt.superApplyPattern("EEE, d MMM yyyy HH:mm:ss 'XXXXX' (z)");
    fmt.setTimeZone(getTimeZone());
    return fmt;
  }
  
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    applyPattern("EEE, d MMM yyyy HH:mm:ss Z (z)");
  }
  
  public StringBuffer format(Date date, StringBuffer dateStrBuf, FieldPosition fieldPosition) {
    return super.format(date, dateStrBuf, fieldPosition);
  }
  
  public Date parse(String text, ParsePosition pos) {
    if (text == null || pos == null)
      throw new NullPointerException(); 
    if (0 > pos.getIndex() || pos.getIndex() >= text.length())
      return null; 
    return isLenient() ? (new Rfc2822LenientParser(text, pos))
      .parse() : (new Rfc2822StrictParser(text, pos))
      .parse();
  }
  
  public void setCalendar(Calendar newCalendar) {
    throw new UnsupportedOperationException("Method setCalendar() shouldn't be called");
  }
  
  public void setNumberFormat(NumberFormat newNumberFormat) {
    throw new UnsupportedOperationException("Method setNumberFormat() shouldn't be called");
  }
  
  private void superApplyPattern(String pattern) {
    applyPattern(pattern);
  }
  
  private Date toDate(int dayName, int day, int month, int year, int hour, int minute, int second, int zone) {
    if (second == 60)
      second = 59; 
    TimeZone tz = this.calendar.getTimeZone();
    try {
      this.calendar.setTimeZone(UTC);
      this.calendar.clear();
      this.calendar.set(year, month, day, hour, minute, second);
      if (dayName == -1 || dayName == this.calendar
        .get(7)) {
        this.calendar.add(12, zone);
        return this.calendar.getTime();
      } 
      throw new IllegalArgumentException("Inconsistent day-name");
    } finally {
      this.calendar.setTimeZone(tz);
    } 
  }
  
  private static abstract class AbstractDateParser {
    static final int INVALID_CHAR = -1;
    
    static final int MAX_YEAR_DIGITS = 8;
    
    final String text;
    
    final ParsePosition pos;
    
    AbstractDateParser(String text, ParsePosition pos) {
      this.text = text;
      this.pos = pos;
    }
    
    final Date parse() {
      int startPosition = this.pos.getIndex();
      try {
        return tryParse();
      } catch (Exception e) {
        if (MailDateFormat.LOGGER.isLoggable(Level.FINE))
          MailDateFormat.LOGGER.log(Level.FINE, "Bad date: '" + this.text + "'", e); 
        this.pos.setErrorIndex(this.pos.getIndex());
        this.pos.setIndex(startPosition + 1);
        return null;
      } 
    }
    
    abstract Date tryParse() throws ParseException;
    
    final int parseDayName() throws ParseException {
      switch (getChar()) {
        case 83:
          if (skipPair('u', 'n'))
            return 1; 
          if (skipPair('a', 't'))
            return 7; 
          break;
        case 84:
          if (skipPair('u', 'e'))
            return 3; 
          if (skipPair('h', 'u'))
            return 5; 
          break;
        case 77:
          if (skipPair('o', 'n'))
            return 2; 
          break;
        case 87:
          if (skipPair('e', 'd'))
            return 4; 
          break;
        case 70:
          if (skipPair('r', 'i'))
            return 6; 
          break;
        case -1:
          throw new ParseException("Invalid day-name", this.pos
              .getIndex());
      } 
      this.pos.setIndex(this.pos.getIndex() - 1);
      throw new ParseException("Invalid day-name", this.pos.getIndex());
    }
    
    final int parseMonthName(boolean caseSensitive) throws ParseException {
      switch (getChar()) {
        case 106:
          if (caseSensitive)
            break; 
        case 74:
          if (skipChar('u') || (!caseSensitive && skipChar('U'))) {
            if (skipChar('l') || (!caseSensitive && 
              skipChar('L')))
              return 6; 
            if (skipChar('n') || (!caseSensitive && 
              skipChar('N')))
              return 5; 
            this.pos.setIndex(this.pos.getIndex() - 1);
            break;
          } 
          if (skipPair('a', 'n') || (!caseSensitive && 
            skipAlternativePair('a', 'A', 'n', 'N')))
            return 0; 
          break;
        case 109:
          if (caseSensitive)
            break; 
        case 77:
          if (skipChar('a') || (!caseSensitive && skipChar('A'))) {
            if (skipChar('r') || (!caseSensitive && 
              skipChar('R')))
              return 2; 
            if (skipChar('y') || (!caseSensitive && 
              skipChar('Y')))
              return 4; 
            this.pos.setIndex(this.pos.getIndex() - 1);
          } 
          break;
        case 97:
          if (caseSensitive)
            break; 
        case 65:
          if (skipPair('u', 'g') || (!caseSensitive && 
            skipAlternativePair('u', 'U', 'g', 'G')))
            return 7; 
          if (skipPair('p', 'r') || (!caseSensitive && 
            skipAlternativePair('p', 'P', 'r', 'R')))
            return 3; 
          break;
        case 100:
          if (caseSensitive)
            break; 
        case 68:
          if (skipPair('e', 'c') || (!caseSensitive && 
            skipAlternativePair('e', 'E', 'c', 'C')))
            return 11; 
          break;
        case 111:
          if (caseSensitive)
            break; 
        case 79:
          if (skipPair('c', 't') || (!caseSensitive && 
            skipAlternativePair('c', 'C', 't', 'T')))
            return 9; 
          break;
        case 115:
          if (caseSensitive)
            break; 
        case 83:
          if (skipPair('e', 'p') || (!caseSensitive && 
            skipAlternativePair('e', 'E', 'p', 'P')))
            return 8; 
          break;
        case 110:
          if (caseSensitive)
            break; 
        case 78:
          if (skipPair('o', 'v') || (!caseSensitive && 
            skipAlternativePair('o', 'O', 'v', 'V')))
            return 10; 
          break;
        case 102:
          if (caseSensitive)
            break; 
        case 70:
          if (skipPair('e', 'b') || (!caseSensitive && 
            skipAlternativePair('e', 'E', 'b', 'B')))
            return 1; 
          break;
        case -1:
          throw new ParseException("Invalid month", this.pos.getIndex());
      } 
      this.pos.setIndex(this.pos.getIndex() - 1);
      throw new ParseException("Invalid month", this.pos.getIndex());
    }
    
    final int parseZoneOffset() throws ParseException {
      int sign = getChar();
      if (sign == 43 || sign == 45) {
        int offset = parseAsciiDigits(4, 4, true);
        if (!isValidZoneOffset(offset)) {
          this.pos.setIndex(this.pos.getIndex() - 5);
          throw new ParseException("Invalid zone", this.pos.getIndex());
        } 
        return ((sign == 43) ? -1 : 1) * (offset / 100 * 60 + offset % 100);
      } 
      if (sign != -1)
        this.pos.setIndex(this.pos.getIndex() - 1); 
      throw new ParseException("Invalid zone", this.pos.getIndex());
    }
    
    boolean isValidZoneOffset(int offset) {
      return (offset % 100 < 60);
    }
    
    final int parseAsciiDigits(int count) throws ParseException {
      return parseAsciiDigits(count, count);
    }
    
    final int parseAsciiDigits(int min, int max) throws ParseException {
      return parseAsciiDigits(min, max, false);
    }
    
    final int parseAsciiDigits(int min, int max, boolean isEOF) throws ParseException {
      int result = 0;
      int nbDigitsParsed = 0;
      while (nbDigitsParsed < max && peekAsciiDigit()) {
        result = result * 10 + getAsciiDigit();
        nbDigitsParsed++;
      } 
      if (nbDigitsParsed < min || (nbDigitsParsed == max && !isEOF && 
        peekAsciiDigit())) {
        this.pos.setIndex(this.pos.getIndex() - nbDigitsParsed);
      } else {
        return result;
      } 
      String range = (min == max) ? Integer.toString(min) : ("between " + min + " and " + max);
      throw new ParseException("Invalid input: expected " + range + " ASCII digits", this.pos
          .getIndex());
    }
    
    final void parseFoldingWhiteSpace() throws ParseException {
      if (!skipFoldingWhiteSpace())
        throw new ParseException("Invalid input: expected FWS", this.pos
            .getIndex()); 
    }
    
    final void parseChar(char ch) throws ParseException {
      if (!skipChar(ch))
        throw new ParseException("Invalid input: expected '" + ch + "'", this.pos
            .getIndex()); 
    }
    
    final int getAsciiDigit() {
      int ch = getChar();
      if (48 <= ch && ch <= 57)
        return Character.digit((char)ch, 10); 
      if (ch != -1)
        this.pos.setIndex(this.pos.getIndex() - 1); 
      return -1;
    }
    
    final int getChar() {
      if (this.pos.getIndex() < this.text.length()) {
        char ch = this.text.charAt(this.pos.getIndex());
        this.pos.setIndex(this.pos.getIndex() + 1);
        return ch;
      } 
      return -1;
    }
    
    boolean skipFoldingWhiteSpace() {
      if (skipChar(' ')) {
        if (!peekFoldingWhiteSpace())
          return true; 
        this.pos.setIndex(this.pos.getIndex() - 1);
      } else if (!peekFoldingWhiteSpace()) {
        return false;
      } 
      int startIndex = this.pos.getIndex();
      if (skipWhiteSpace()) {
        while (skipNewline()) {
          if (!skipWhiteSpace()) {
            this.pos.setIndex(startIndex);
            return false;
          } 
        } 
        return true;
      } 
      if (skipNewline() && skipWhiteSpace())
        return true; 
      this.pos.setIndex(startIndex);
      return false;
    }
    
    final boolean skipWhiteSpace() {
      int startIndex = this.pos.getIndex();
      while (skipAlternative(' ', '\t'));
      return (this.pos.getIndex() > startIndex);
    }
    
    final boolean skipNewline() {
      return skipPair('\r', '\n');
    }
    
    final boolean skipAlternativeTriple(char firstStandard, char firstAlternative, char secondStandard, char secondAlternative, char thirdStandard, char thirdAlternative) {
      if (skipAlternativePair(firstStandard, firstAlternative, secondStandard, secondAlternative)) {
        if (skipAlternative(thirdStandard, thirdAlternative))
          return true; 
        this.pos.setIndex(this.pos.getIndex() - 2);
      } 
      return false;
    }
    
    final boolean skipAlternativePair(char firstStandard, char firstAlternative, char secondStandard, char secondAlternative) {
      if (skipAlternative(firstStandard, firstAlternative)) {
        if (skipAlternative(secondStandard, secondAlternative))
          return true; 
        this.pos.setIndex(this.pos.getIndex() - 1);
      } 
      return false;
    }
    
    final boolean skipAlternative(char standard, char alternative) {
      return (skipChar(standard) || skipChar(alternative));
    }
    
    final boolean skipPair(char first, char second) {
      if (skipChar(first)) {
        if (skipChar(second))
          return true; 
        this.pos.setIndex(this.pos.getIndex() - 1);
      } 
      return false;
    }
    
    final boolean skipChar(char ch) {
      if (this.pos.getIndex() < this.text.length() && this.text
        .charAt(this.pos.getIndex()) == ch) {
        this.pos.setIndex(this.pos.getIndex() + 1);
        return true;
      } 
      return false;
    }
    
    final boolean peekAsciiDigit() {
      return (this.pos.getIndex() < this.text.length() && '0' <= this.text
        .charAt(this.pos.getIndex()) && this.text
        .charAt(this.pos.getIndex()) <= '9');
    }
    
    boolean peekFoldingWhiteSpace() {
      return (this.pos.getIndex() < this.text.length() && (this.text
        .charAt(this.pos.getIndex()) == ' ' || this.text
        .charAt(this.pos.getIndex()) == '\t' || this.text
        .charAt(this.pos.getIndex()) == '\r'));
    }
    
    final boolean peekChar(char ch) {
      return (this.pos.getIndex() < this.text.length() && this.text
        .charAt(this.pos.getIndex()) == ch);
    }
  }
  
  private class Rfc2822StrictParser extends AbstractDateParser {
    Rfc2822StrictParser(String text, ParsePosition pos) {
      super(text, pos);
    }
    
    Date tryParse() throws ParseException {
      int dayName = parseOptionalBegin();
      int day = parseDay();
      int month = parseMonth();
      int year = parseYear();
      parseFoldingWhiteSpace();
      int hour = parseHour();
      parseChar(':');
      int minute = parseMinute();
      int second = skipChar(':') ? parseSecond() : 0;
      parseFwsBetweenTimeOfDayAndZone();
      int zone = parseZone();
      try {
        return MailDateFormat.this.toDate(dayName, day, month, year, hour, minute, second, zone);
      } catch (IllegalArgumentException e) {
        throw new ParseException("Invalid input: some of the calendar fields have invalid values, or day-name is inconsistent with date", this.pos
            
            .getIndex());
      } 
    }
    
    int parseOptionalBegin() throws ParseException {
      int dayName;
      if (!peekAsciiDigit()) {
        skipFoldingWhiteSpace();
        dayName = parseDayName();
        parseChar(',');
      } else {
        dayName = -1;
      } 
      return dayName;
    }
    
    int parseDay() throws ParseException {
      skipFoldingWhiteSpace();
      return parseAsciiDigits(1, 2);
    }
    
    int parseMonth() throws ParseException {
      parseFwsInMonth();
      int month = parseMonthName(isMonthNameCaseSensitive());
      parseFwsInMonth();
      return month;
    }
    
    void parseFwsInMonth() throws ParseException {
      parseFoldingWhiteSpace();
    }
    
    boolean isMonthNameCaseSensitive() {
      return true;
    }
    
    int parseYear() throws ParseException {
      int year = parseAsciiDigits(4, 8);
      if (year >= 1900)
        return year; 
      this.pos.setIndex(this.pos.getIndex() - 4);
      while (this.text.charAt(this.pos.getIndex() - 1) == '0')
        this.pos.setIndex(this.pos.getIndex() - 1); 
      throw new ParseException("Invalid year", this.pos.getIndex());
    }
    
    int parseHour() throws ParseException {
      return parseAsciiDigits(2);
    }
    
    int parseMinute() throws ParseException {
      return parseAsciiDigits(2);
    }
    
    int parseSecond() throws ParseException {
      return parseAsciiDigits(2);
    }
    
    void parseFwsBetweenTimeOfDayAndZone() throws ParseException {
      parseFoldingWhiteSpace();
    }
    
    int parseZone() throws ParseException {
      return parseZoneOffset();
    }
  }
  
  private class Rfc2822LenientParser extends Rfc2822StrictParser {
    private Boolean hasDefaultFws;
    
    Rfc2822LenientParser(String text, ParsePosition pos) {
      super(text, pos);
    }
    
    int parseOptionalBegin() {
      while (this.pos.getIndex() < this.text.length() && !peekAsciiDigit())
        this.pos.setIndex(this.pos.getIndex() + 1); 
      return -1;
    }
    
    int parseDay() throws ParseException {
      skipFoldingWhiteSpace();
      return parseAsciiDigits(1, 3);
    }
    
    void parseFwsInMonth() throws ParseException {
      if (this.hasDefaultFws == null) {
        this.hasDefaultFws = Boolean.valueOf(!skipChar('-'));
        skipFoldingWhiteSpace();
      } else if (this.hasDefaultFws.booleanValue()) {
        skipFoldingWhiteSpace();
      } else {
        parseChar('-');
      } 
    }
    
    boolean isMonthNameCaseSensitive() {
      return false;
    }
    
    int parseYear() throws ParseException {
      int year = parseAsciiDigits(1, 8);
      if (year >= 1000)
        return year; 
      if (year >= 50)
        return year + 1900; 
      return year + 2000;
    }
    
    int parseHour() throws ParseException {
      return parseAsciiDigits(1, 2);
    }
    
    int parseMinute() throws ParseException {
      return parseAsciiDigits(1, 2);
    }
    
    int parseSecond() throws ParseException {
      return parseAsciiDigits(1, 2);
    }
    
    void parseFwsBetweenTimeOfDayAndZone() throws ParseException {
      skipFoldingWhiteSpace();
    }
    
    int parseZone() throws ParseException {
      try {
        int hoursOffset;
        if (this.pos.getIndex() >= this.text.length())
          throw new ParseException("Missing zone", this.pos.getIndex()); 
        if (peekChar('+') || peekChar('-'))
          return parseZoneOffset(); 
        if (skipAlternativePair('U', 'u', 'T', 't'))
          return 0; 
        if (skipAlternativeTriple('G', 'g', 'M', 'm', 'T', 't'))
          return 0; 
        if (skipAlternative('E', 'e')) {
          hoursOffset = 4;
        } else if (skipAlternative('C', 'c')) {
          hoursOffset = 5;
        } else if (skipAlternative('M', 'm')) {
          hoursOffset = 6;
        } else if (skipAlternative('P', 'p')) {
          hoursOffset = 7;
        } else {
          throw new ParseException("Invalid zone", this.pos
              .getIndex());
        } 
        if (skipAlternativePair('S', 's', 'T', 't')) {
          hoursOffset++;
        } else if (!skipAlternativePair('D', 'd', 'T', 't')) {
          this.pos.setIndex(this.pos.getIndex() - 1);
          throw new ParseException("Invalid zone", this.pos
              .getIndex());
        } 
        return hoursOffset * 60;
      } catch (ParseException e) {
        ParseException parseException1;
        if (MailDateFormat.LOGGER.isLoggable(Level.FINE))
          MailDateFormat.LOGGER.log(Level.FINE, "No timezone? : '" + this.text + "'", parseException1); 
        return 0;
      } 
    }
    
    boolean isValidZoneOffset(int offset) {
      return true;
    }
    
    boolean skipFoldingWhiteSpace() {
      boolean result = peekFoldingWhiteSpace();
      while (this.pos.getIndex() < this.text.length()) {
        switch (this.text.charAt(this.pos.getIndex())) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            this.pos.setIndex(this.pos.getIndex() + 1);
        } 
      } 
      return result;
    }
    
    boolean peekFoldingWhiteSpace() {
      return (super.peekFoldingWhiteSpace() || (this.pos
        .getIndex() < this.text.length() && this.text
        .charAt(this.pos.getIndex()) == '\n'));
    }
  }
}
