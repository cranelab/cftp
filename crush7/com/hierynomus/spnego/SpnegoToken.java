package com.hierynomus.spnego;

import com.hierynomus.protocol.commons.buffer.Buffer;
import java.io.IOException;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERApplicationSpecific;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

abstract class SpnegoToken {
  private int tokenTagNo;
  
  private String tokenName;
  
  public SpnegoToken(int tokenTagNo, String tokenName) {
    this.tokenTagNo = tokenTagNo;
    this.tokenName = tokenName;
  }
  
  protected void writeGss(Buffer<?> buffer, ASN1EncodableVector negToken) throws IOException {
    DERTaggedObject negotiationToken = new DERTaggedObject(true, this.tokenTagNo, (ASN1Encodable)new DERSequence(negToken));
    ASN1EncodableVector implicitSeqGssApi = new ASN1EncodableVector();
    implicitSeqGssApi.add((ASN1Encodable)ObjectIdentifiers.SPNEGO);
    implicitSeqGssApi.add((ASN1Encodable)negotiationToken);
    DERApplicationSpecific gssApiHeader = new DERApplicationSpecific(0, implicitSeqGssApi);
    buffer.putRawBytes(gssApiHeader.getEncoded());
  }
  
  protected void parseSpnegoToken(ASN1Encodable spnegoToken) throws IOException, SpnegoException {
    if (!(spnegoToken instanceof ASN1TaggedObject) || ((ASN1TaggedObject)spnegoToken).getTagNo() != this.tokenTagNo)
      throw new SpnegoException("Expected to find the " + this.tokenName + " (CHOICE [" + this.tokenTagNo + "]) header, not: " + spnegoToken); 
    ASN1Primitive negToken = ((ASN1TaggedObject)spnegoToken).getObject();
    if (!(negToken instanceof ASN1Sequence))
      throw new SpnegoException("Expected a " + this.tokenName + " (SEQUENCE), not: " + negToken); 
    Enumeration<ASN1Encodable> tokenObjects = ((ASN1Sequence)negToken).getObjects();
    while (tokenObjects.hasMoreElements()) {
      ASN1Encodable asn1Encodable = tokenObjects.nextElement();
      if (!(asn1Encodable instanceof ASN1TaggedObject))
        throw new SpnegoException("Expected an ASN.1 TaggedObject as " + this.tokenName + " contents, not: " + asn1Encodable); 
      ASN1TaggedObject asn1TaggedObject = (ASN1TaggedObject)asn1Encodable;
      parseTagged(asn1TaggedObject);
    } 
  }
  
  protected abstract void parseTagged(ASN1TaggedObject paramASN1TaggedObject) throws SpnegoException;
}
