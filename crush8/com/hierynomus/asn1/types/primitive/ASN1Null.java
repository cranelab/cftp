package com.hierynomus.asn1.types.primitive;

import com.hierynomus.asn1.ASN1OutputStream;
import com.hierynomus.asn1.ASN1ParseException;
import com.hierynomus.asn1.ASN1Parser;
import com.hierynomus.asn1.ASN1Serializer;
import com.hierynomus.asn1.encodingrules.ASN1Decoder;
import com.hierynomus.asn1.encodingrules.ASN1Encoder;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import com.hierynomus.asn1.util.Checks;
import java.io.IOException;

public class ASN1Null extends ASN1PrimitiveValue<Void> {
  private static final byte[] NULL_BYTES = new byte[0];
  
  public ASN1Null() {
    super(ASN1Tag.NULL, NULL_BYTES);
  }
  
  public Void getValue() {
    return null;
  }
  
  protected int valueHash() {
    return 0;
  }
  
  public static class Parser extends ASN1Parser<ASN1Null> {
    public Parser(ASN1Decoder decoder) {
      super(decoder);
    }
    
    public ASN1Null parse(ASN1Tag<ASN1Null> asn1Tag, byte[] value) {
      Checks.checkState((value.length == 0), "ASN.1 NULL can not have a value", new Object[0]);
      return new ASN1Null();
    }
  }
  
  public static class Serializer extends ASN1Serializer<ASN1Null> {
    public Serializer(ASN1Encoder encoder) {
      super(encoder);
    }
    
    public int serializedLength(ASN1Null asn1Object) {
      return 0;
    }
    
    public void serialize(ASN1Null asn1Object, ASN1OutputStream stream) throws IOException {}
  }
}
