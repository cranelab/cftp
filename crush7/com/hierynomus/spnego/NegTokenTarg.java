package com.hierynomus.spnego;

import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

public class NegTokenTarg extends SpnegoToken {
  private BigInteger negotiationResult;
  
  private ASN1ObjectIdentifier supportedMech;
  
  private byte[] responseToken;
  
  private byte[] mechListMic;
  
  public NegTokenTarg() {
    super(1, "NegTokenTarg");
  }
  
  protected void writeGss(Buffer<?> buffer, ASN1EncodableVector negToken) throws IOException {
    DERTaggedObject negotiationToken = new DERTaggedObject(true, 1, (ASN1Encodable)new DERSequence(negToken));
    buffer.putRawBytes(negotiationToken.getEncoded());
  }
  
  public void write(Buffer<?> buffer) throws SpnegoException {
    try {
      ASN1EncodableVector negTokenTarg = new ASN1EncodableVector();
      if (this.negotiationResult != null)
        negTokenTarg.add((ASN1Encodable)new DERTaggedObject(0, (ASN1Encodable)new ASN1Enumerated(this.negotiationResult))); 
      if (this.supportedMech != null)
        negTokenTarg.add((ASN1Encodable)new DERTaggedObject(1, (ASN1Encodable)this.supportedMech)); 
      if (this.responseToken != null && this.responseToken.length > 0)
        negTokenTarg.add((ASN1Encodable)new DERTaggedObject(2, (ASN1Encodable)new DEROctetString(this.responseToken))); 
      if (this.mechListMic != null && this.mechListMic.length > 0)
        negTokenTarg.add((ASN1Encodable)new DERTaggedObject(3, (ASN1Encodable)new DEROctetString(this.mechListMic))); 
      writeGss(buffer, negTokenTarg);
    } catch (IOException e) {
      throw new SpnegoException("Could not write NegTokenTarg to buffer", e);
    } 
  }
  
  public NegTokenTarg read(byte[] bytes) throws SpnegoException {
    return read(new Buffer.PlainBuffer(bytes, Endian.LE));
  }
  
  private NegTokenTarg read(Buffer<?> buffer) throws SpnegoException {
    try {
      ASN1InputStream is = new ASN1InputStream(buffer.asInputStream());
      ASN1Primitive instance = is.readObject();
      parseSpnegoToken((ASN1Encodable)instance);
    } catch (IOException e) {
      throw new SpnegoException("Could not read NegTokenTarg from buffer", e);
    } 
    return this;
  }
  
  protected void parseTagged(ASN1TaggedObject asn1TaggedObject) throws SpnegoException {
    switch (asn1TaggedObject.getTagNo()) {
      case 0:
        readNegResult(asn1TaggedObject.getObject());
        return;
      case 1:
        readSupportedMech(asn1TaggedObject.getObject());
        return;
      case 2:
        readResponseToken(asn1TaggedObject.getObject());
        return;
      case 3:
        readMechListMIC(asn1TaggedObject.getObject());
        return;
    } 
    throw new SpnegoException("Unknown Object Tag " + asn1TaggedObject.getTagNo() + " encountered.");
  }
  
  private void readResponseToken(ASN1Primitive responseToken) throws SpnegoException {
    if (!(responseToken instanceof ASN1OctetString))
      throw new SpnegoException("Expected the responseToken (OCTET_STRING) contents, not: " + responseToken); 
    this.responseToken = ((ASN1OctetString)responseToken).getOctets();
  }
  
  private void readMechListMIC(ASN1Primitive mic) throws SpnegoException {
    if (!(mic instanceof ASN1OctetString))
      throw new SpnegoException("Expected the responseToken (OCTET_STRING) contents, not: " + mic); 
    this.mechListMic = ((ASN1OctetString)mic).getOctets();
  }
  
  private void readSupportedMech(ASN1Primitive supportedMech) throws SpnegoException {
    if (!(supportedMech instanceof ASN1ObjectIdentifier))
      throw new SpnegoException("Expected the supportedMech (OBJECT IDENTIFIER) contents, not: " + supportedMech); 
    this.supportedMech = (ASN1ObjectIdentifier)supportedMech;
  }
  
  private void readNegResult(ASN1Primitive object) throws SpnegoException {
    if (!(object instanceof ASN1Enumerated))
      throw new SpnegoException("Expected the negResult (ENUMERATED) contents, not: " + this.supportedMech); 
    this.negotiationResult = ((ASN1Enumerated)object).getValue();
  }
  
  public BigInteger getNegotiationResult() {
    return this.negotiationResult;
  }
  
  public void setNegotiationResult(BigInteger negotiationResult) {
    this.negotiationResult = negotiationResult;
  }
  
  public ASN1ObjectIdentifier getSupportedMech() {
    return this.supportedMech;
  }
  
  public void setSupportedMech(ASN1ObjectIdentifier supportedMech) {
    this.supportedMech = supportedMech;
  }
  
  public byte[] getResponseToken() {
    return this.responseToken;
  }
  
  public void setResponseToken(byte[] responseToken) {
    this.responseToken = responseToken;
  }
  
  public byte[] getMechListMic() {
    return this.mechListMic;
  }
  
  public void setMechListMic(byte[] mechListMic) {
    this.mechListMic = mechListMic;
  }
}
