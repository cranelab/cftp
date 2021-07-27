package javax.mail.internet;

import com.sun.mail.util.ASCIIUtility;
import com.sun.mail.util.FolderClosedIOException;
import com.sun.mail.util.LineOutputStream;
import com.sun.mail.util.MessageRemovedIOException;
import com.sun.mail.util.MimeUtil;
import com.sun.mail.util.PropUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.EncodingAware;
import javax.mail.FolderClosedException;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;

public class MimeBodyPart extends BodyPart implements MimePart {
  private static final boolean setDefaultTextCharset = PropUtil.getBooleanSystemProperty("mail.mime.setdefaulttextcharset", true);
  
  private static final boolean setContentTypeFileName = PropUtil.getBooleanSystemProperty("mail.mime.setcontenttypefilename", true);
  
  private static final boolean encodeFileName = PropUtil.getBooleanSystemProperty("mail.mime.encodefilename", false);
  
  private static final boolean decodeFileName = PropUtil.getBooleanSystemProperty("mail.mime.decodefilename", false);
  
  private static final boolean ignoreMultipartEncoding = PropUtil.getBooleanSystemProperty("mail.mime.ignoremultipartencoding", true);
  
  static final boolean cacheMultipart = PropUtil.getBooleanSystemProperty("mail.mime.cachemultipart", true);
  
  protected DataHandler dh;
  
  protected byte[] content;
  
  protected InputStream contentStream;
  
  protected InternetHeaders headers;
  
  protected Object cachedContent;
  
  public MimeBodyPart() {
    this.headers = new InternetHeaders();
  }
  
  public MimeBodyPart(InputStream is) throws MessagingException {
    if (!(is instanceof ByteArrayInputStream) && !(is instanceof BufferedInputStream) && !(is instanceof SharedInputStream))
      is = new BufferedInputStream(is); 
    this.headers = new InternetHeaders(is);
    if (is instanceof SharedInputStream) {
      SharedInputStream sis = (SharedInputStream)is;
      this.contentStream = sis.newStream(sis.getPosition(), -1L);
    } else {
      try {
        this.content = ASCIIUtility.getBytes(is);
      } catch (IOException ioex) {
        throw new MessagingException("Error reading input stream", ioex);
      } 
    } 
  }
  
  public MimeBodyPart(InternetHeaders headers, byte[] content) throws MessagingException {
    this.headers = headers;
    this.content = content;
  }
  
  public int getSize() throws MessagingException {
    if (this.content != null)
      return this.content.length; 
    if (this.contentStream != null)
      try {
        int size = this.contentStream.available();
        if (size > 0)
          return size; 
      } catch (IOException iOException) {} 
    return -1;
  }
  
  public int getLineCount() throws MessagingException {
    return -1;
  }
  
  public String getContentType() throws MessagingException {
    String s = getHeader("Content-Type", null);
    s = MimeUtil.cleanContentType(this, s);
    if (s == null)
      s = "text/plain"; 
    return s;
  }
  
  public boolean isMimeType(String mimeType) throws MessagingException {
    return isMimeType(this, mimeType);
  }
  
  public String getDisposition() throws MessagingException {
    return getDisposition(this);
  }
  
  public void setDisposition(String disposition) throws MessagingException {
    setDisposition(this, disposition);
  }
  
  public String getEncoding() throws MessagingException {
    return getEncoding(this);
  }
  
  public String getContentID() throws MessagingException {
    return getHeader("Content-Id", null);
  }
  
  public void setContentID(String cid) throws MessagingException {
    if (cid == null) {
      removeHeader("Content-ID");
    } else {
      setHeader("Content-ID", cid);
    } 
  }
  
  public String getContentMD5() throws MessagingException {
    return getHeader("Content-MD5", null);
  }
  
  public void setContentMD5(String md5) throws MessagingException {
    setHeader("Content-MD5", md5);
  }
  
  public String[] getContentLanguage() throws MessagingException {
    return getContentLanguage(this);
  }
  
  public void setContentLanguage(String[] languages) throws MessagingException {
    setContentLanguage(this, languages);
  }
  
  public String getDescription() throws MessagingException {
    return getDescription(this);
  }
  
  public void setDescription(String description) throws MessagingException {
    setDescription(description, null);
  }
  
  public void setDescription(String description, String charset) throws MessagingException {
    setDescription(this, description, charset);
  }
  
  public String getFileName() throws MessagingException {
    return getFileName(this);
  }
  
  public void setFileName(String filename) throws MessagingException {
    setFileName(this, filename);
  }
  
  public InputStream getInputStream() throws IOException, MessagingException {
    return getDataHandler().getInputStream();
  }
  
  protected InputStream getContentStream() throws MessagingException {
    if (this.contentStream != null)
      return ((SharedInputStream)this.contentStream).newStream(0L, -1L); 
    if (this.content != null)
      return new ByteArrayInputStream(this.content); 
    throw new MessagingException("No MimeBodyPart content");
  }
  
  public InputStream getRawInputStream() throws MessagingException {
    return getContentStream();
  }
  
  public DataHandler getDataHandler() throws MessagingException {
    if (this.dh == null)
      this.dh = new MimePartDataHandler(this); 
    return this.dh;
  }
  
  public Object getContent() throws IOException, MessagingException {
    Object c;
    if (this.cachedContent != null)
      return this.cachedContent; 
    try {
      c = getDataHandler().getContent();
    } catch (FolderClosedIOException fex) {
      throw new FolderClosedException(fex.getFolder(), fex.getMessage());
    } catch (MessageRemovedIOException mex) {
      throw new MessageRemovedException(mex.getMessage());
    } 
    if (cacheMultipart && (c instanceof Multipart || c instanceof javax.mail.Message) && (this.content != null || this.contentStream != null)) {
      this.cachedContent = c;
      if (c instanceof MimeMultipart)
        ((MimeMultipart)c).parse(); 
    } 
    return c;
  }
  
  public void setDataHandler(DataHandler dh) throws MessagingException {
    this.dh = dh;
    this.cachedContent = null;
    invalidateContentHeaders(this);
  }
  
  public void setContent(Object o, String type) throws MessagingException {
    if (o instanceof Multipart) {
      setContent((Multipart)o);
    } else {
      setDataHandler(new DataHandler(o, type));
    } 
  }
  
  public void setText(String text) throws MessagingException {
    setText(text, null);
  }
  
  public void setText(String text, String charset) throws MessagingException {
    setText(this, text, charset, "plain");
  }
  
  public void setText(String text, String charset, String subtype) throws MessagingException {
    setText(this, text, charset, subtype);
  }
  
  public void setContent(Multipart mp) throws MessagingException {
    setDataHandler(new DataHandler(mp, mp.getContentType()));
    mp.setParent(this);
  }
  
  public void attachFile(File file) throws IOException, MessagingException {
    FileDataSource fds = new FileDataSource(file);
    setDataHandler(new DataHandler((DataSource)fds));
    setFileName(fds.getName());
    setDisposition("attachment");
  }
  
  public void attachFile(String file) throws IOException, MessagingException {
    File f = new File(file);
    attachFile(f);
  }
  
  public void attachFile(File file, String contentType, String encoding) throws IOException, MessagingException {
    EncodedFileDataSource encodedFileDataSource = new EncodedFileDataSource(file, contentType, encoding);
    setDataHandler(new DataHandler((DataSource)encodedFileDataSource));
    setFileName(encodedFileDataSource.getName());
    setDisposition("attachment");
  }
  
  public void attachFile(String file, String contentType, String encoding) throws IOException, MessagingException {
    attachFile(new File(file), contentType, encoding);
  }
  
  private static class EncodedFileDataSource extends FileDataSource implements EncodingAware {
    private String contentType;
    
    private String encoding;
    
    public EncodedFileDataSource(File file, String contentType, String encoding) {
      super(file);
      this.contentType = contentType;
      this.encoding = encoding;
    }
    
    public String getContentType() {
      return (this.contentType != null) ? this.contentType : super.getContentType();
    }
    
    public String getEncoding() {
      return this.encoding;
    }
  }
  
  public void saveFile(File file) throws IOException, MessagingException {
    OutputStream out = null;
    InputStream in = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(file));
      in = getInputStream();
      byte[] buf = new byte[8192];
      int len;
      while ((len = in.read(buf)) > 0)
        out.write(buf, 0, len); 
    } finally {
      try {
        if (in != null)
          in.close(); 
      } catch (IOException iOException) {}
      try {
        if (out != null)
          out.close(); 
      } catch (IOException iOException) {}
    } 
  }
  
  public void saveFile(String file) throws IOException, MessagingException {
    File f = new File(file);
    saveFile(f);
  }
  
  public void writeTo(OutputStream os) throws IOException, MessagingException {
    writeTo(this, os, null);
  }
  
  public String[] getHeader(String name) throws MessagingException {
    return this.headers.getHeader(name);
  }
  
  public String getHeader(String name, String delimiter) throws MessagingException {
    return this.headers.getHeader(name, delimiter);
  }
  
  public void setHeader(String name, String value) throws MessagingException {
    this.headers.setHeader(name, value);
  }
  
  public void addHeader(String name, String value) throws MessagingException {
    this.headers.addHeader(name, value);
  }
  
  public void removeHeader(String name) throws MessagingException {
    this.headers.removeHeader(name);
  }
  
  public Enumeration getAllHeaders() throws MessagingException {
    return this.headers.getAllHeaders();
  }
  
  public Enumeration getMatchingHeaders(String[] names) throws MessagingException {
    return this.headers.getMatchingHeaders(names);
  }
  
  public Enumeration getNonMatchingHeaders(String[] names) throws MessagingException {
    return this.headers.getNonMatchingHeaders(names);
  }
  
  public void addHeaderLine(String line) throws MessagingException {
    this.headers.addHeaderLine(line);
  }
  
  public Enumeration getAllHeaderLines() throws MessagingException {
    return this.headers.getAllHeaderLines();
  }
  
  public Enumeration getMatchingHeaderLines(String[] names) throws MessagingException {
    return this.headers.getMatchingHeaderLines(names);
  }
  
  public Enumeration getNonMatchingHeaderLines(String[] names) throws MessagingException {
    return this.headers.getNonMatchingHeaderLines(names);
  }
  
  protected void updateHeaders() throws MessagingException {
    updateHeaders(this);
    if (this.cachedContent != null) {
      this.dh = new DataHandler(this.cachedContent, getContentType());
      this.cachedContent = null;
      this.content = null;
      if (this.contentStream != null)
        try {
          this.contentStream.close();
        } catch (IOException iOException) {} 
      this.contentStream = null;
    } 
  }
  
  static boolean isMimeType(MimePart part, String mimeType) throws MessagingException {
    String type = part.getContentType();
    try {
      return (new ContentType(type)).match(mimeType);
    } catch (ParseException ex) {
      try {
        int i = type.indexOf(';');
        if (i > 0)
          return (new ContentType(type.substring(0, i))).match(mimeType); 
      } catch (ParseException parseException) {}
      return type.equalsIgnoreCase(mimeType);
    } 
  }
  
  static void setText(MimePart part, String text, String charset, String subtype) throws MessagingException {
    if (charset == null)
      if (MimeUtility.checkAscii(text) != 1) {
        charset = MimeUtility.getDefaultMIMECharset();
      } else {
        charset = "us-ascii";
      }  
    part.setContent(text, "text/" + subtype + "; charset=" + 
        MimeUtility.quote(charset, "()<>@,;:\\\"\t []/?="));
  }
  
  static String getDisposition(MimePart part) throws MessagingException {
    String s = part.getHeader("Content-Disposition", (String)null);
    if (s == null)
      return null; 
    ContentDisposition cd = new ContentDisposition(s);
    return cd.getDisposition();
  }
  
  static void setDisposition(MimePart part, String disposition) throws MessagingException {
    if (disposition == null) {
      part.removeHeader("Content-Disposition");
    } else {
      String s = part.getHeader("Content-Disposition", (String)null);
      if (s != null) {
        ContentDisposition cd = new ContentDisposition(s);
        cd.setDisposition(disposition);
        disposition = cd.toString();
      } 
      part.setHeader("Content-Disposition", disposition);
    } 
  }
  
  static String getDescription(MimePart part) throws MessagingException {
    String rawvalue = part.getHeader("Content-Description", (String)null);
    if (rawvalue == null)
      return null; 
    try {
      return MimeUtility.decodeText(MimeUtility.unfold(rawvalue));
    } catch (UnsupportedEncodingException ex) {
      return rawvalue;
    } 
  }
  
  static void setDescription(MimePart part, String description, String charset) throws MessagingException {
    if (description == null) {
      part.removeHeader("Content-Description");
      return;
    } 
    try {
      part.setHeader("Content-Description", MimeUtility.fold(21, 
            MimeUtility.encodeText(description, charset, null)));
    } catch (UnsupportedEncodingException uex) {
      throw new MessagingException("Encoding error", uex);
    } 
  }
  
  static String getFileName(MimePart part) throws MessagingException {
    String filename = null;
    String s = part.getHeader("Content-Disposition", (String)null);
    if (s != null) {
      ContentDisposition cd = new ContentDisposition(s);
      filename = cd.getParameter("filename");
    } 
    if (filename == null) {
      s = part.getHeader("Content-Type", (String)null);
      s = MimeUtil.cleanContentType(part, s);
      if (s != null)
        try {
          ContentType ct = new ContentType(s);
          filename = ct.getParameter("name");
        } catch (ParseException parseException) {} 
    } 
    if (decodeFileName && filename != null)
      try {
        filename = MimeUtility.decodeText(filename);
      } catch (UnsupportedEncodingException ex) {
        throw new MessagingException("Can't decode filename", ex);
      }  
    return filename;
  }
  
  static void setFileName(MimePart part, String name) throws MessagingException {
    if (encodeFileName && name != null)
      try {
        name = MimeUtility.encodeText(name);
      } catch (UnsupportedEncodingException ex) {
        throw new MessagingException("Can't encode filename", ex);
      }  
    String s = part.getHeader("Content-Disposition", (String)null);
    ContentDisposition cd = new ContentDisposition((s == null) ? "attachment" : s);
    String charset = MimeUtility.getDefaultMIMECharset();
    ParameterList p = cd.getParameterList();
    if (p == null) {
      p = new ParameterList();
      cd.setParameterList(p);
    } 
    if (encodeFileName) {
      p.setLiteral("filename", name);
    } else {
      p.set("filename", name, charset);
    } 
    part.setHeader("Content-Disposition", cd.toString());
    if (setContentTypeFileName) {
      s = part.getHeader("Content-Type", (String)null);
      s = MimeUtil.cleanContentType(part, s);
      if (s != null)
        try {
          ContentType cType = new ContentType(s);
          p = cType.getParameterList();
          if (p == null) {
            p = new ParameterList();
            cType.setParameterList(p);
          } 
          if (encodeFileName) {
            p.setLiteral("name", name);
          } else {
            p.set("name", name, charset);
          } 
          part.setHeader("Content-Type", cType.toString());
        } catch (ParseException parseException) {} 
    } 
  }
  
  static String[] getContentLanguage(MimePart part) throws MessagingException {
    String s = part.getHeader("Content-Language", (String)null);
    if (s == null)
      return null; 
    HeaderTokenizer h = new HeaderTokenizer(s, "()<>@,;:\\\"\t []/?=");
    List<String> v = new ArrayList<String>();
    while (true) {
      HeaderTokenizer.Token tk = h.next();
      int tkType = tk.getType();
      if (tkType == -4)
        break; 
      if (tkType == -1)
        v.add(tk.getValue()); 
    } 
    if (v.isEmpty())
      return null; 
    String[] language = new String[v.size()];
    v.toArray(language);
    return language;
  }
  
  static void setContentLanguage(MimePart part, String[] languages) throws MessagingException {
    StringBuffer sb = new StringBuffer(languages[0]);
    int len = "Content-Language".length() + 2 + languages[0].length();
    for (int i = 1; i < languages.length; i++) {
      sb.append(',');
      len++;
      if (len > 76) {
        sb.append("\r\n\t");
        len = 8;
      } 
      sb.append(languages[i]);
      len += languages[i].length();
    } 
    part.setHeader("Content-Language", sb.toString());
  }
  
  static String getEncoding(MimePart part) throws MessagingException {
    String s = part.getHeader("Content-Transfer-Encoding", (String)null);
    if (s == null)
      return null; 
    s = s.trim();
    if (s.equalsIgnoreCase("7bit") || s.equalsIgnoreCase("8bit") || s
      .equalsIgnoreCase("quoted-printable") || s
      .equalsIgnoreCase("binary") || s
      .equalsIgnoreCase("base64"))
      return s; 
    HeaderTokenizer h = new HeaderTokenizer(s, "()<>@,;:\\\"\t []/?=");
    while (true) {
      HeaderTokenizer.Token tk = h.next();
      int tkType = tk.getType();
      if (tkType == -4)
        break; 
      if (tkType == -1)
        return tk.getValue(); 
    } 
    return s;
  }
  
  static void setEncoding(MimePart part, String encoding) throws MessagingException {
    part.setHeader("Content-Transfer-Encoding", encoding);
  }
  
  static String restrictEncoding(MimePart part, String encoding) throws MessagingException {
    if (!ignoreMultipartEncoding || encoding == null)
      return encoding; 
    if (encoding.equalsIgnoreCase("7bit") || encoding
      .equalsIgnoreCase("8bit") || encoding
      .equalsIgnoreCase("binary"))
      return encoding; 
    String type = part.getContentType();
    if (type == null)
      return encoding; 
    try {
      ContentType cType = new ContentType(type);
      if (cType.match("multipart/*"))
        return null; 
      if (cType.match("message/*") && 
        !PropUtil.getBooleanSystemProperty("mail.mime.allowencodedmessages", false))
        return null; 
    } catch (ParseException parseException) {}
    return encoding;
  }
  
  static void updateHeaders(MimePart part) throws MessagingException {
    DataHandler dh = part.getDataHandler();
    if (dh == null)
      return; 
    try {
      String type = dh.getContentType();
      boolean composite = false;
      boolean needCTHeader = (part.getHeader("Content-Type") == null);
      ContentType cType = new ContentType(type);
      if (cType.match("multipart/*")) {
        Object o;
        composite = true;
        if (part instanceof MimeBodyPart) {
          MimeBodyPart mbp = (MimeBodyPart)part;
          o = (mbp.cachedContent != null) ? mbp.cachedContent : dh.getContent();
        } else if (part instanceof MimeMessage) {
          MimeMessage msg = (MimeMessage)part;
          o = (msg.cachedContent != null) ? msg.cachedContent : dh.getContent();
        } else {
          o = dh.getContent();
        } 
        if (o instanceof MimeMultipart) {
          ((MimeMultipart)o).updateHeaders();
        } else {
          throw new MessagingException("MIME part of type \"" + type + "\" contains object of type " + o
              
              .getClass().getName() + " instead of MimeMultipart");
        } 
      } else if (cType.match("message/rfc822")) {
        composite = true;
      } 
      if (dh instanceof MimePartDataHandler) {
        MimePartDataHandler mdh = (MimePartDataHandler)dh;
        MimePart mpart = mdh.getPart();
        if (mpart != part) {
          if (needCTHeader)
            part.setHeader("Content-Type", mpart.getContentType()); 
          String enc = mpart.getEncoding();
          if (enc != null) {
            setEncoding(part, enc);
            return;
          } 
        } else {
          return;
        } 
      } 
      if (!composite) {
        if (part.getHeader("Content-Transfer-Encoding") == null)
          setEncoding(part, MimeUtility.getEncoding(dh)); 
        if (needCTHeader && setDefaultTextCharset && cType
          .match("text/*") && cType
          .getParameter("charset") == null) {
          String charset, enc = part.getEncoding();
          if (enc != null && enc.equalsIgnoreCase("7bit")) {
            charset = "us-ascii";
          } else {
            charset = MimeUtility.getDefaultMIMECharset();
          } 
          cType.setParameter("charset", charset);
          type = cType.toString();
        } 
      } 
      if (needCTHeader) {
        if (setContentTypeFileName) {
          String s = part.getHeader("Content-Disposition", (String)null);
          if (s != null) {
            ContentDisposition cd = new ContentDisposition(s);
            String filename = cd.getParameter("filename");
            if (filename != null) {
              ParameterList p = cType.getParameterList();
              if (p == null) {
                p = new ParameterList();
                cType.setParameterList(p);
              } 
              if (encodeFileName) {
                p.setLiteral("name", 
                    MimeUtility.encodeText(filename));
              } else {
                p.set("name", filename, 
                    MimeUtility.getDefaultMIMECharset());
              } 
              type = cType.toString();
            } 
          } 
        } 
        part.setHeader("Content-Type", type);
      } 
    } catch (IOException ex) {
      throw new MessagingException("IOException updating headers", ex);
    } 
  }
  
  static void invalidateContentHeaders(MimePart part) throws MessagingException {
    part.removeHeader("Content-Type");
    part.removeHeader("Content-Transfer-Encoding");
  }
  
  static void writeTo(MimePart part, OutputStream os, String[] ignoreList) throws IOException, MessagingException {
    LineOutputStream los = null;
    if (os instanceof LineOutputStream) {
      los = (LineOutputStream)os;
    } else {
      los = new LineOutputStream(os);
    } 
    Enumeration<String> hdrLines = part.getNonMatchingHeaderLines(ignoreList);
    while (hdrLines.hasMoreElements())
      los.writeln(hdrLines.nextElement()); 
    los.writeln();
    InputStream is = null;
    byte[] buf = null;
    try {
      DataHandler dh = part.getDataHandler();
      if (dh instanceof MimePartDataHandler) {
        MimePartDataHandler mpdh = (MimePartDataHandler)dh;
        MimePart mpart = mpdh.getPart();
        if (mpart.getEncoding() != null)
          is = mpdh.getContentStream(); 
      } 
      if (is != null) {
        buf = new byte[8192];
        int len;
        while ((len = is.read(buf)) > 0)
          os.write(buf, 0, len); 
      } else {
        os = MimeUtility.encode(os, 
            restrictEncoding(part, part.getEncoding()));
        part.getDataHandler().writeTo(os);
      } 
    } finally {
      if (is != null)
        is.close(); 
      buf = null;
    } 
    os.flush();
  }
  
  static class MimePartDataHandler extends DataHandler {
    MimePart part;
    
    public MimePartDataHandler(MimePart part) {
      super(new MimePartDataSource(part));
      this.part = part;
    }
    
    InputStream getContentStream() throws MessagingException {
      InputStream is = null;
      if (this.part instanceof MimeBodyPart) {
        MimeBodyPart mbp = (MimeBodyPart)this.part;
        is = mbp.getContentStream();
      } else if (this.part instanceof MimeMessage) {
        MimeMessage msg = (MimeMessage)this.part;
        is = msg.getContentStream();
      } 
      return is;
    }
    
    MimePart getPart() {
      return this.part;
    }
  }
}
