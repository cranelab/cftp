package com.hierynomus.asn1.types.primitive;

import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Primitive;
import com.hierynomus.asn1.types.ASN1Tag;
import java.util.Arrays;

public abstract class ASN1PrimitiveValue<T> extends ASN1Object<T> implements ASN1Primitive {
  protected byte[] valueBytes;
  
  public ASN1PrimitiveValue(ASN1Tag tag) {
    super(tag);
  }
  
  public ASN1PrimitiveValue(ASN1Tag tag, byte[] valueBytes) {
    super(tag);
    this.valueBytes = valueBytes;
  }
  
  protected int valueHash() {
    return Arrays.hashCode(this.valueBytes);
  }
}
