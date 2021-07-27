package com.sun.mail.imap.protocol;

import com.sun.mail.iap.ParsingException;
import com.sun.mail.iap.Response;
import com.sun.mail.util.PropUtil;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MailDateFormat;

public class ENVELOPE implements Item {
  static final char[] name = new char[] { 'E', 'N', 'V', 'E', 'L', 'O', 'P', 'E' };
  
  public int msgno;
  
  public Date date = null;
  
  public String subject;
  
  public InternetAddress[] from;
  
  public InternetAddress[] sender;
  
  public InternetAddress[] replyTo;
  
  public InternetAddress[] to;
  
  public InternetAddress[] cc;
  
  public InternetAddress[] bcc;
  
  public String inReplyTo;
  
  public String messageId;
  
  private static final MailDateFormat mailDateFormat = new MailDateFormat();
  
  private static final boolean parseDebug = PropUtil.getBooleanSystemProperty("mail.imap.parse.debug", false);
  
  public ENVELOPE(FetchResponse r) throws ParsingException {
    if (parseDebug)
      System.out.println("parse ENVELOPE"); 
    this.msgno = r.getNumber();
    r.skipSpaces();
    if (r.readByte() != 40)
      throw new ParsingException("ENVELOPE parse error"); 
    String s = r.readString();
    if (s != null)
      try {
        synchronized (mailDateFormat) {
          this.date = mailDateFormat.parse(s);
        } 
      } catch (ParseException parseException) {} 
    if (parseDebug)
      System.out.println("  Date: " + this.date); 
    this.subject = r.readString();
    if (parseDebug)
      System.out.println("  Subject: " + this.subject); 
    if (parseDebug)
      System.out.println("  From addresses:"); 
    this.from = parseAddressList(r);
    if (parseDebug)
      System.out.println("  Sender addresses:"); 
    this.sender = parseAddressList(r);
    if (parseDebug)
      System.out.println("  Reply-To addresses:"); 
    this.replyTo = parseAddressList(r);
    if (parseDebug)
      System.out.println("  To addresses:"); 
    this.to = parseAddressList(r);
    if (parseDebug)
      System.out.println("  Cc addresses:"); 
    this.cc = parseAddressList(r);
    if (parseDebug)
      System.out.println("  Bcc addresses:"); 
    this.bcc = parseAddressList(r);
    this.inReplyTo = r.readString();
    if (parseDebug)
      System.out.println("  In-Reply-To: " + this.inReplyTo); 
    this.messageId = r.readString();
    if (parseDebug)
      System.out.println("  Message-ID: " + this.messageId); 
    if (r.readByte() != 41)
      throw new ParsingException("ENVELOPE parse error"); 
  }
  
  private InternetAddress[] parseAddressList(Response r) throws ParsingException {
    r.skipSpaces();
    byte b = r.readByte();
    if (b == 40) {
      if (r.peekByte() == 41) {
        r.skip(1);
        return null;
      } 
      List<InternetAddress> v = new ArrayList<InternetAddress>();
      do {
        IMAPAddress a = new IMAPAddress(r);
        if (parseDebug)
          System.out.println("    Address: " + a); 
        if (a.isEndOfGroup())
          continue; 
        v.add(a);
      } while (r.peekByte() != 41);
      r.skip(1);
      return v.<InternetAddress>toArray(new InternetAddress[v.size()]);
    } 
    if (b == 78 || b == 110) {
      r.skip(2);
      return null;
    } 
    throw new ParsingException("ADDRESS parse error");
  }
}
