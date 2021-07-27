package com.sun.mail.imap.protocol;

import com.sun.mail.iap.ParsingException;
import javax.mail.Flags;

public class FLAGS extends Flags implements Item {
  static final char[] name = new char[] { 'F', 'L', 'A', 'G', 'S' };
  
  public int msgno;
  
  private static final long serialVersionUID = 439049847053756670L;
  
  public FLAGS(IMAPResponse r) throws ParsingException {
    this.msgno = r.getNumber();
    r.skipSpaces();
    String[] flags = r.readSimpleList();
    if (flags != null)
      for (int i = 0; i < flags.length; i++) {
        String s = flags[i];
        if (s.length() >= 2 && s.charAt(0) == '\\') {
          switch (Character.toUpperCase(s.charAt(1))) {
            case 'S':
              add(Flags.Flag.SEEN);
              break;
            case 'R':
              add(Flags.Flag.RECENT);
              break;
            case 'D':
              if (s.length() >= 3) {
                char c = s.charAt(2);
                if (c == 'e' || c == 'E') {
                  add(Flags.Flag.DELETED);
                  break;
                } 
                if (c == 'r' || c == 'R')
                  add(Flags.Flag.DRAFT); 
                break;
              } 
              add(s);
              break;
            case 'A':
              add(Flags.Flag.ANSWERED);
              break;
            case 'F':
              add(Flags.Flag.FLAGGED);
              break;
            case '*':
              add(Flags.Flag.USER);
              break;
            default:
              add(s);
              break;
          } 
        } else {
          add(s);
        } 
      }  
  }
}
