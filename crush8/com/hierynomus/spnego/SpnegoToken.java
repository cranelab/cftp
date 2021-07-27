package com.hierynomus.spnego;

import com.hierynomus.asn1.ASN1OutputStream;
import com.hierynomus.asn1.encodingrules.der.DEREncoder;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import com.hierynomus.asn1.types.constructed.ASN1Sequence;
import com.hierynomus.asn1.types.constructed.ASN1TaggedObject;
import com.hierynomus.protocol.commons.buffer.Buffer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

abstract class SpnegoToken {
  private int tokenTagNo;
  
  private String tokenName;
  
  public SpnegoToken(int tokenTagNo, String tokenName) {
    this.tokenTagNo = tokenTagNo;
    this.tokenName = tokenName;
  }
  
  protected void writeGss(Buffer<?> buffer, ASN1Object negToken) throws IOException {
    ASN1TaggedObject negotiationToken = new ASN1TaggedObject(ASN1Tag.contextSpecific(this.tokenTagNo).constructed(), negToken);
    List<ASN1Object> implicitSeqGssApi = new ArrayList<>();
    implicitSeqGssApi.add(ObjectIdentifiers.SPNEGO);
    implicitSeqGssApi.add(negotiationToken);
    ASN1TaggedObject token = new ASN1TaggedObject(ASN1Tag.application(0), new ASN1Sequence(implicitSeqGssApi), false);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ASN1OutputStream out = new ASN1OutputStream(new DEREncoder(), baos)) {
      out.writeObject(token);
    } 
    buffer.putRawBytes(baos.toByteArray());
  }
  
  protected void parseSpnegoToken(ASN1Object spnegoToken) throws SpnegoException {
    if (!(spnegoToken instanceof ASN1TaggedObject) || ((ASN1TaggedObject)spnegoToken).getTagNo() != this.tokenTagNo)
      throw new SpnegoException("Expected to find the " + this.tokenName + " (CHOICE [" + this.tokenTagNo + "]) header, not: " + spnegoToken); 
    ASN1Object negToken = ((ASN1TaggedObject)spnegoToken).getObject();
    if (!(negToken instanceof ASN1Sequence))
      throw new SpnegoException("Expected a " + this.tokenName + " (SEQUENCE), not: " + negToken); 
    for (ASN1Object asn1Object : negToken) {
      if (!(asn1Object instanceof ASN1TaggedObject))
        throw new SpnegoException("Expected an ASN.1 TaggedObject as " + this.tokenName + " contents, not: " + asn1Object); 
      ASN1TaggedObject asn1TaggedObject = (ASN1TaggedObject)asn1Object;
      parseTagged(asn1TaggedObject);
    } 
  }
  
  protected abstract void parseTagged(ASN1TaggedObject paramASN1TaggedObject) throws SpnegoException;
}
