package com.hierynomus.asn1.types.string;

import com.hierynomus.asn1.ASN1OutputStream;
import com.hierynomus.asn1.ASN1ParseException;
import com.hierynomus.asn1.ASN1Parser;
import com.hierynomus.asn1.ASN1Serializer;
import com.hierynomus.asn1.encodingrules.ASN1Decoder;
import com.hierynomus.asn1.encodingrules.ASN1Encoder;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import java.io.IOException;
import java.util.Arrays;

public class ASN1OctetString extends ASN1String<byte[]> {
  public ASN1OctetString(byte[] bytes) {
    super(ASN1Tag.OCTET_STRING, bytes);
  }
  
  public ASN1OctetString(ASN1Tag<?> tag, byte[] bytes) {
    super(tag, bytes);
  }
  
  public byte[] getValue() {
    return Arrays.copyOf(this.valueBytes, this.valueBytes.length);
  }
  
  public int length() {
    return this.valueBytes.length;
  }
  
  public static class Parser extends ASN1Parser<ASN1OctetString> {
    public Parser(ASN1Decoder decoder) {
      super(decoder);
    }
    
    public ASN1OctetString parse(ASN1Tag<ASN1OctetString> asn1Tag, byte[] value) throws ASN1ParseException {
      return new ASN1OctetString(asn1Tag, value);
    }
  }
  
  public static class Serializer extends ASN1Serializer<ASN1OctetString> {
    public Serializer(ASN1Encoder encoder) {
      super(encoder);
    }
    
    public int serializedLength(ASN1OctetString asn1Object) throws IOException {
      return asn1Object.valueBytes.length;
    }
    
    public void serialize(ASN1OctetString asn1Object, ASN1OutputStream stream) throws IOException {
      stream.write(asn1Object.valueBytes);
    }
  }
}
