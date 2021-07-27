package com.sun.mail.imap;

import com.sun.mail.imap.protocol.BODYSTRUCTURE;
import java.util.ArrayList;
import java.util.List;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.MultipartDataSource;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimePartDataSource;

public class IMAPMultipartDataSource extends MimePartDataSource implements MultipartDataSource {
  private List<IMAPBodyPart> parts;
  
  protected IMAPMultipartDataSource(MimePart part, BODYSTRUCTURE[] bs, String sectionId, IMAPMessage msg) {
    super(part);
    this.parts = new ArrayList<IMAPBodyPart>(bs.length);
    for (int i = 0; i < bs.length; i++)
      this.parts.add(new IMAPBodyPart(bs[i], (sectionId == null) ? 

            
            Integer.toString(i + 1) : (sectionId + "." + 
            Integer.toString(i + 1)), msg)); 
  }
  
  public int getCount() {
    return this.parts.size();
  }
  
  public BodyPart getBodyPart(int index) throws MessagingException {
    return this.parts.get(index);
  }
}
