package com.hierynomus.asn1;

import com.hierynomus.asn1.encodingrules.ASN1Encoder;
import com.hierynomus.asn1.types.ASN1Object;
import java.io.IOException;

public abstract class ASN1Serializer<T extends ASN1Object> {
  protected final ASN1Encoder encoder;
  
  public ASN1Serializer(ASN1Encoder encoder) {
    this.encoder = encoder;
  }
  
  public abstract int serializedLength(T paramT) throws IOException;
  
  public abstract void serialize(T paramT, ASN1OutputStream paramASN1OutputStream) throws IOException;
}
