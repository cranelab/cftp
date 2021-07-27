package org.apache.http.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;

public final class EntityUtils {
  public static void consumeQuietly(HttpEntity entity) {
    try {
      consume(entity);
    } catch (IOException ignore) {}
  }
  
  public static void consume(HttpEntity entity) throws IOException {
    if (entity == null)
      return; 
    if (entity.isStreaming()) {
      InputStream instream = entity.getContent();
      if (instream != null)
        instream.close(); 
    } 
  }
  
  public static void updateEntity(HttpResponse response, HttpEntity entity) throws IOException {
    Args.notNull(response, "Response");
    consume(response.getEntity());
    response.setEntity(entity);
  }
  
  public static byte[] toByteArray(HttpEntity entity) throws IOException {
    Args.notNull(entity, "Entity");
    InputStream instream = entity.getContent();
    if (instream == null)
      return null; 
    try {
      Args.check((entity.getContentLength() <= 2147483647L), "HTTP entity too large to be buffered in memory");
      int i = (int)entity.getContentLength();
      if (i < 0)
        i = 4096; 
      ByteArrayBuffer buffer = new ByteArrayBuffer(i);
      byte[] tmp = new byte[4096];
      int l;
      while ((l = instream.read(tmp)) != -1)
        buffer.append(tmp, 0, l); 
      return buffer.toByteArray();
    } finally {
      instream.close();
    } 
  }
  
  @Deprecated
  public static String getContentCharSet(HttpEntity entity) throws ParseException {
    Args.notNull(entity, "Entity");
    String charset = null;
    if (entity.getContentType() != null) {
      HeaderElement[] values = entity.getContentType().getElements();
      if (values.length > 0) {
        NameValuePair param = values[0].getParameterByName("charset");
        if (param != null)
          charset = param.getValue(); 
      } 
    } 
    return charset;
  }
  
  @Deprecated
  public static String getContentMimeType(HttpEntity entity) throws ParseException {
    Args.notNull(entity, "Entity");
    String mimeType = null;
    if (entity.getContentType() != null) {
      HeaderElement[] values = entity.getContentType().getElements();
      if (values.length > 0)
        mimeType = values[0].getName(); 
    } 
    return mimeType;
  }
  
  private static String toString(HttpEntity entity, ContentType contentType) throws IOException {
    InputStream instream = entity.getContent();
    if (instream == null)
      return null; 
    try {
      Args.check((entity.getContentLength() <= 2147483647L), "HTTP entity too large to be buffered in memory");
      int i = (int)entity.getContentLength();
      if (i < 0)
        i = 4096; 
      Charset charset = null;
      if (contentType != null) {
        charset = contentType.getCharset();
        if (charset == null) {
          ContentType defaultContentType = ContentType.getByMimeType(contentType.getMimeType());
          charset = (defaultContentType != null) ? defaultContentType.getCharset() : null;
        } 
      } 
      if (charset == null)
        charset = HTTP.DEF_CONTENT_CHARSET; 
      Reader reader = new InputStreamReader(instream, charset);
      CharArrayBuffer buffer = new CharArrayBuffer(i);
      char[] tmp = new char[1024];
      int l;
      while ((l = reader.read(tmp)) != -1)
        buffer.append(tmp, 0, l); 
      return buffer.toString();
    } finally {
      instream.close();
    } 
  }
  
  public static String toString(HttpEntity entity, Charset defaultCharset) throws IOException, ParseException {
    Args.notNull(entity, "Entity");
    ContentType contentType = null;
    try {
      contentType = ContentType.get(entity);
    } catch (UnsupportedCharsetException ex) {
      if (defaultCharset == null)
        throw new UnsupportedEncodingException(ex.getMessage()); 
    } 
    if (contentType != null) {
      if (contentType.getCharset() == null)
        contentType = contentType.withCharset(defaultCharset); 
    } else {
      contentType = ContentType.DEFAULT_TEXT.withCharset(defaultCharset);
    } 
    return toString(entity, contentType);
  }
  
  public static String toString(HttpEntity entity, String defaultCharset) throws IOException, ParseException {
    return toString(entity, (defaultCharset != null) ? Charset.forName(defaultCharset) : null);
  }
  
  public static String toString(HttpEntity entity) throws IOException, ParseException {
    Args.notNull(entity, "Entity");
    return toString(entity, ContentType.get(entity));
  }
}
