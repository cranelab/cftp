package com.hierynomus.asn1;

import com.hierynomus.asn1.encodingrules.ASN1Decoder;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;

public abstract class ASN1Parser<T extends ASN1Object> {
  protected ASN1Decoder decoder;
  
  public ASN1Parser(ASN1Decoder decoder) {
    this.decoder = decoder;
  }
  
  public abstract T parse(ASN1Tag<T> paramASN1Tag, byte[] paramArrayOfbyte) throws ASN1ParseException;
}
