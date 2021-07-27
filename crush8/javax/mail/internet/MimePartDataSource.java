package javax.mail.internet;

import com.sun.mail.util.FolderClosedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownServiceException;
import javax.activation.DataSource;
import javax.mail.FolderClosedException;
import javax.mail.MessageAware;
import javax.mail.MessageContext;
import javax.mail.MessagingException;

public class MimePartDataSource implements DataSource, MessageAware {
  protected MimePart part;
  
  private MessageContext context;
  
  public MimePartDataSource(MimePart part) {
    this.part = part;
  }
  
  public InputStream getInputStream() throws IOException {
    try {
      InputStream is;
      if (this.part instanceof MimeBodyPart) {
        is = ((MimeBodyPart)this.part).getContentStream();
      } else if (this.part instanceof MimeMessage) {
        is = ((MimeMessage)this.part).getContentStream();
      } else {
        throw new MessagingException("Unknown part");
      } 
      String encoding = MimeBodyPart.restrictEncoding(this.part, this.part.getEncoding());
      if (encoding != null)
        return MimeUtility.decode(is, encoding); 
      return is;
    } catch (FolderClosedException fex) {
      throw new FolderClosedIOException(fex.getFolder(), fex
          .getMessage());
    } catch (MessagingException mex) {
      IOException ioex = new IOException(mex.getMessage());
      ioex.initCause(mex);
      throw ioex;
    } 
  }
  
  public OutputStream getOutputStream() throws IOException {
    throw new UnknownServiceException("Writing not supported");
  }
  
  public String getContentType() {
    try {
      return this.part.getContentType();
    } catch (MessagingException mex) {
      return "application/octet-stream";
    } 
  }
  
  public String getName() {
    try {
      if (this.part instanceof MimeBodyPart)
        return ((MimeBodyPart)this.part).getFileName(); 
    } catch (MessagingException messagingException) {}
    return "";
  }
  
  public synchronized MessageContext getMessageContext() {
    if (this.context == null)
      this.context = new MessageContext(this.part); 
    return this.context;
  }
}
