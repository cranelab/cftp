package com.hierynomus.asn1.types.string;

import com.hierynomus.asn1.ASN1InputStream;
import com.hierynomus.asn1.ASN1OutputStream;
import com.hierynomus.asn1.ASN1ParseException;
import com.hierynomus.asn1.ASN1Parser;
import com.hierynomus.asn1.ASN1Serializer;
import com.hierynomus.asn1.encodingrules.ASN1Decoder;
import com.hierynomus.asn1.encodingrules.ASN1Encoder;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import com.hierynomus.asn1.util.Checks;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

public class ASN1BitString extends ASN1String<BitSet> {
  private int unusedBits;
  
  private ASN1BitString(ASN1Tag<ASN1BitString> tag, byte[] bytes, int unusedBits) {
    super(tag, bytes);
    this.unusedBits = unusedBits;
  }
  
  public ASN1BitString(BitSet bitSet) {
    super(ASN1Tag.BIT_STRING, bitSet.toByteArray());
  }
  
  public BitSet getValue() {
    return BitSet.valueOf(this.valueBytes);
  }
  
  public boolean isSet(int x) {
    int toCheck = x / 8;
    byte theByte = this.valueBytes[toCheck];
    int index = x % 8;
    int mask = 1 << 7 - index;
    int masked = theByte & mask;
    return (masked != 0);
  }
  
  public int length() {
    return this.valueBytes.length * 8 - this.unusedBits;
  }
  
  public static class Parser extends ASN1Parser<ASN1BitString> {
    public Parser(ASN1Decoder decoder) {
      super(decoder);
    }
    
    public ASN1BitString parse(ASN1Tag<ASN1BitString> asn1Tag, byte[] value) {
      if (asn1Tag.isConstructed()) {
        ASN1InputStream stream = new ASN1InputStream(this.decoder, value);
        try {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          int i = 0;
          while (stream.available() > 0) {
            ASN1Tag subTag = stream.readTag();
            Checks.checkState((subTag.getTag() == asn1Tag.getTag()), "Expected an ASN.1 BIT STRING as Constructed object, got: %s", new Object[] { subTag });
            int j = stream.readLength();
            byte[] subValue = stream.readValue(j);
            baos.write(subValue, 1, subValue.length - 1);
            if (stream.available() <= 0)
              i = subValue[0]; 
          } 
          return new ASN1BitString(asn1Tag, baos.toByteArray(), i, null);
        } catch (IOException e) {
          throw new ASN1ParseException(e, "Unable to parse Constructed ASN.1 BIT STRING", new Object[0]);
        } 
      } 
      byte unusedBits = value[0];
      byte[] bits = Arrays.copyOfRange(value, 1, value.length);
      return new ASN1BitString(asn1Tag, bits, unusedBits, null);
    }
  }
  
  public static class Serializer extends ASN1Serializer<ASN1BitString> {
    public Serializer(ASN1Encoder encoder) {
      super(encoder);
    }
    
    public int serializedLength(ASN1BitString asn1Object) {
      return 0;
    }
    
    public void serialize(ASN1BitString asn1Object, ASN1OutputStream stream) {}
  }
}
