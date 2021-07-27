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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.StringTokenizer;

public class ASN1ObjectIdentifier extends ASN1PrimitiveValue<String> {
  private String oid;
  
  public ASN1ObjectIdentifier(String oid) {
    super(ASN1Tag.OBJECT_IDENTIFIER);
    this.oid = oid;
  }
  
  private ASN1ObjectIdentifier(byte[] valueBytes, String oid) {
    super(ASN1Tag.OBJECT_IDENTIFIER, valueBytes);
    this.oid = oid;
  }
  
  public String getValue() {
    return this.oid;
  }
  
  protected int valueHash() {
    return this.oid.hashCode();
  }
  
  public static class Parser extends ASN1Parser<ASN1ObjectIdentifier> {
    public Parser(ASN1Decoder decoder) {
      super(decoder);
    }
    
    public ASN1ObjectIdentifier parse(ASN1Tag<ASN1ObjectIdentifier> asn1Tag, byte[] value) {
      Checks.checkArgument((value.length > 0), "An ASN.1 OBJECT IDENTIFIER should have at least a one byte value", new Object[0]);
      ByteArrayInputStream is = new ByteArrayInputStream(value);
      StringBuilder b = new StringBuilder();
      int firstTwo = is.read();
      b.append(firstTwo / 40);
      b.append('.').append(firstTwo % 40);
      while (is.available() > 0) {
        int x = is.read();
        if (x < 127) {
          b.append('.').append(x);
          continue;
        } 
        BigInteger v = BigInteger.valueOf((x & 0x7F));
        do {
          x = is.read();
          v = v.shiftLeft(7).add(BigInteger.valueOf((x & 0x7F)));
        } while (x > 127);
        b.append('.').append(v);
      } 
      return new ASN1ObjectIdentifier(value, b.toString(), null);
    }
  }
  
  public static class Serializer extends ASN1Serializer<ASN1ObjectIdentifier> {
    public Serializer(ASN1Encoder encoder) {
      super(encoder);
    }
    
    public int serializedLength(ASN1ObjectIdentifier asn1Object) {
      if (asn1Object.valueBytes == null)
        calculateBytes(asn1Object); 
      return asn1Object.valueBytes.length;
    }
    
    private void calculateBytes(ASN1ObjectIdentifier asn1Object) {
      String oid = asn1Object.oid;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      StringTokenizer tokenizer = new StringTokenizer(oid, ".");
      int first = Integer.parseInt(tokenizer.nextToken());
      int second = Integer.parseInt(tokenizer.nextToken());
      baos.write(first * 40 + second);
      while (tokenizer.hasMoreTokens()) {
        BigInteger token = new BigInteger(tokenizer.nextToken());
        if (token.intValue() > 0 && token.intValue() < 127) {
          baos.write(token.intValue());
          continue;
        } 
        int neededBytes = token.bitLength() / 7 + ((token.bitLength() % 7 > 0) ? 1 : 0);
        for (int i = neededBytes - 1; i >= 0; i--) {
          byte b = token.shiftRight(i * 7).byteValue();
          b = (byte)(b & Byte.MAX_VALUE);
          if (i > 0)
            b = (byte)(b | 0x80); 
          baos.write(b);
        } 
      } 
      asn1Object.valueBytes = baos.toByteArray();
    }
    
    public void serialize(ASN1ObjectIdentifier asn1Object, ASN1OutputStream stream) throws IOException {
      if (asn1Object.valueBytes == null)
        calculateBytes(asn1Object); 
      stream.write(asn1Object.valueBytes);
    }
  }
}
