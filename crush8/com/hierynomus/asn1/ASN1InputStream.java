package com.hierynomus.asn1;

import com.hierynomus.asn1.encodingrules.ASN1Decoder;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ASN1InputStream extends FilterInputStream implements Iterable<ASN1Object> {
  private static final Logger logger = LoggerFactory.getLogger(ASN1InputStream.class);
  
  private final ASN1Decoder decoder;
  
  public ASN1InputStream(ASN1Decoder decoder, InputStream wrapped) {
    super(wrapped);
    this.decoder = decoder;
  }
  
  public ASN1InputStream(ASN1Decoder decoder, byte[] value) {
    super(new ByteArrayInputStream(value));
    this.decoder = decoder;
  }
  
  public <T extends ASN1Object> T readObject() throws ASN1ParseException {
    try {
      ASN1Tag<?> tag = this.decoder.readTag(this);
      logger.trace("Read ASN.1 tag {}", tag);
      int length = this.decoder.readLength(this);
      logger.trace("Read ASN.1 object length: {}", Integer.valueOf(length));
      byte[] value = this.decoder.readValue(length, this);
      ASN1Object parse = (ASN1Object)tag.newParser(this.decoder).parse(tag, value);
      logger.debug("Read ASN.1 object: {}", parse);
      return (T)parse;
    } catch (ASN1ParseException pe) {
      throw pe;
    } catch (Exception e) {
      throw new ASN1ParseException(e, "Cannot parse ASN.1 object from stream", new Object[0]);
    } 
  }
  
  public byte[] readValue(int length) throws IOException {
    return this.decoder.readValue(length, this);
  }
  
  public Iterator<ASN1Object> iterator() {
    return new Iterator<ASN1Object>() {
        public boolean hasNext() {
          try {
            return (ASN1InputStream.this.available() > 0);
          } catch (IOException e) {
            return false;
          } 
        }
        
        public ASN1Object next() {
          return ASN1InputStream.this.readObject();
        }
        
        public void remove() {
          throw new UnsupportedOperationException("Remove not supported on ASN.1 InputStream iterator");
        }
      };
  }
  
  public ASN1Tag readTag() throws IOException {
    return this.decoder.readTag(this);
  }
  
  public int readLength() throws IOException {
    return this.decoder.readLength(this);
  }
}
