package com.hierynomus.asn1.types.primitive;

import com.hierynomus.asn1.ASN1OutputStream;
import com.hierynomus.asn1.ASN1ParseException;
import com.hierynomus.asn1.ASN1Parser;
import com.hierynomus.asn1.ASN1Serializer;
import com.hierynomus.asn1.encodingrules.ASN1Decoder;
import com.hierynomus.asn1.encodingrules.ASN1Encoder;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import java.io.IOException;
import java.math.BigInteger;

public class ASN1Integer extends ASN1PrimitiveValue<BigInteger> {
  private BigInteger value;
  
  public ASN1Integer(long value) {
    this(BigInteger.valueOf(value));
  }
  
  public ASN1Integer(BigInteger value) {
    super(ASN1Tag.INTEGER);
    this.value = value;
    this.valueBytes = value.toByteArray();
  }
  
  private ASN1Integer(byte[] valueBytes, BigInteger value) {
    super(ASN1Tag.INTEGER, valueBytes);
    this.value = value;
  }
  
  public BigInteger getValue() {
    return this.value;
  }
  
  protected int valueHash() {
    return this.value.hashCode();
  }
  
  public static class Parser extends ASN1Parser<ASN1Integer> {
    public Parser(ASN1Decoder decoder) {
      super(decoder);
    }
    
    public ASN1Integer parse(ASN1Tag<ASN1Integer> asn1Tag, byte[] value) {
      return new ASN1Integer(value, new BigInteger(value), null);
    }
  }
  
  public static class Serializer extends ASN1Serializer<ASN1Integer> {
    public Serializer(ASN1Encoder encoder) {
      super(encoder);
    }
    
    public int serializedLength(ASN1Integer asn1Object) {
      return asn1Object.valueBytes.length;
    }
    
    public void serialize(ASN1Integer asn1Object, ASN1OutputStream stream) throws IOException {
      stream.write(asn1Object.valueBytes);
    }
  }
}
