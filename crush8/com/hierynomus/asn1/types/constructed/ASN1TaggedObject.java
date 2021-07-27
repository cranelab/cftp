package com.hierynomus.asn1.types.constructed;

import com.hierynomus.asn1.ASN1InputStream;
import com.hierynomus.asn1.ASN1OutputStream;
import com.hierynomus.asn1.ASN1ParseException;
import com.hierynomus.asn1.ASN1Parser;
import com.hierynomus.asn1.ASN1Serializer;
import com.hierynomus.asn1.encodingrules.ASN1Decoder;
import com.hierynomus.asn1.encodingrules.ASN1Encoder;
import com.hierynomus.asn1.types.ASN1Constructed;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

public class ASN1TaggedObject extends ASN1Object<ASN1Object> implements ASN1Constructed {
  private final ASN1Object object;
  
  private byte[] bytes;
  
  private ASN1Decoder decoder;
  
  private boolean explicit = true;
  
  public ASN1TaggedObject(ASN1Tag tag, ASN1Object object, boolean explicit) {
    super(explicit ? tag.constructed() : tag.asEncoded(object.getTag().getAsn1Encoding()));
    this.object = object;
    this.explicit = explicit;
    this.bytes = null;
  }
  
  public ASN1TaggedObject(ASN1Tag tag, ASN1Object object) {
    this(tag, object, true);
  }
  
  private ASN1TaggedObject(ASN1Tag tag, byte[] bytes, ASN1Decoder decoder) {
    super(tag);
    this.bytes = bytes;
    this.decoder = decoder;
    this.object = null;
  }
  
  public boolean isExplicit() {
    return this.explicit;
  }
  
  public ASN1Object getValue() {
    return getObject();
  }
  
  public int getTagNo() {
    return this.tag.getTag();
  }
  
  public Iterator<ASN1Object> iterator() {
    return ((ASN1Sequence)getObject(ASN1Tag.SEQUENCE)).iterator();
  }
  
  public static class Parser extends ASN1Parser<ASN1TaggedObject> {
    public Parser(ASN1Decoder decoder) {
      super(decoder);
    }
    
    public ASN1TaggedObject parse(ASN1Tag<ASN1TaggedObject> asn1Tag, byte[] value) {
      return new ASN1TaggedObject(asn1Tag, value, this.decoder, null);
    }
  }
  
  public static class Serializer extends ASN1Serializer<ASN1TaggedObject> {
    public Serializer(ASN1Encoder encoder) {
      super(encoder);
    }
    
    public int serializedLength(ASN1TaggedObject asn1Object) throws IOException {
      if (asn1Object.bytes == null)
        calculateBytes(asn1Object); 
      return asn1Object.bytes.length;
    }
    
    private void calculateBytes(ASN1TaggedObject asn1Object) throws IOException {
      ASN1Object object = asn1Object.object;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ASN1OutputStream asn1OutputStream = new ASN1OutputStream(this.encoder, baos);
      if (asn1Object.explicit) {
        asn1OutputStream.writeObject(object);
      } else {
        object.getTag().newSerializer(this.encoder).serialize(object, asn1OutputStream);
      } 
      asn1Object.bytes = baos.toByteArray();
    }
    
    public void serialize(ASN1TaggedObject asn1Object, ASN1OutputStream stream) throws IOException {
      if (asn1Object.bytes == null)
        calculateBytes(asn1Object); 
      stream.write(asn1Object.bytes);
    }
  }
  
  public ASN1Object getObject() {
    if (this.object != null)
      return this.object; 
    try {
      return (new ASN1InputStream(this.decoder, this.bytes)).readObject();
    } catch (ASN1ParseException e) {
      throw new ASN1ParseException(e, "Unable to parse the explicit Tagged Object with %s, it might be implicit", new Object[] { this.tag });
    } 
  }
  
  public <T extends ASN1Object> T getObject(ASN1Tag<T> tag) {
    if (this.object != null && this.object.getTag().equals(tag))
      return (T)this.object; 
    if (this.object == null && this.bytes != null)
      return tag.newParser(this.decoder).parse(tag, this.bytes); 
    throw new ASN1ParseException("Unable to parse the implicit Tagged Object with %s, it is explicit", new Object[] { tag });
  }
  
  public String toString() {
    StringBuilder b = new StringBuilder();
    b.append(getClass().getSimpleName());
    b.append("[").append(this.tag);
    if (this.object != null) {
      b.append(",").append(this.object);
    } else {
      b.append(",<unknown>");
    } 
    b.append("]");
    return b.toString();
  }
}
