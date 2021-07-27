package com.hierynomus.spnego;

import com.hierynomus.asn1.ASN1InputStream;
import com.hierynomus.asn1.ASN1OutputStream;
import com.hierynomus.asn1.encodingrules.der.DERDecoder;
import com.hierynomus.asn1.encodingrules.der.DEREncoder;
import com.hierynomus.asn1.types.ASN1Object;
import com.hierynomus.asn1.types.ASN1Tag;
import com.hierynomus.asn1.types.constructed.ASN1Sequence;
import com.hierynomus.asn1.types.constructed.ASN1TaggedObject;
import com.hierynomus.asn1.types.primitive.ASN1Enumerated;
import com.hierynomus.asn1.types.primitive.ASN1ObjectIdentifier;
import com.hierynomus.asn1.types.string.ASN1OctetString;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class NegTokenTarg extends SpnegoToken {
  private BigInteger negotiationResult;
  
  private ASN1ObjectIdentifier supportedMech;
  
  private byte[] responseToken;
  
  private byte[] mechListMic;
  
  public NegTokenTarg() {
    super(1, "NegTokenTarg");
  }
  
  protected void writeGss(Buffer<?> buffer, ASN1Object negToken) throws IOException {
    ASN1TaggedObject negotiationToken = new ASN1TaggedObject(ASN1Tag.contextSpecific(1).constructed(), negToken, true);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ASN1OutputStream out = new ASN1OutputStream(new DEREncoder(), baos)) {
      out.writeObject(negotiationToken);
    } 
    buffer.putRawBytes(baos.toByteArray());
  }
  
  public void write(Buffer<?> buffer) throws SpnegoException {
    List<ASN1Object> negTokenTarg = new ArrayList<>();
    try {
      if (this.negotiationResult != null)
        negTokenTarg.add(new ASN1TaggedObject(ASN1Tag.contextSpecific(0).constructed(), new ASN1Enumerated(this.negotiationResult))); 
      if (this.supportedMech != null)
        negTokenTarg.add(new ASN1TaggedObject(ASN1Tag.contextSpecific(1).constructed(), this.supportedMech)); 
      if (this.responseToken != null && this.responseToken.length > 0)
        negTokenTarg.add(new ASN1TaggedObject(ASN1Tag.contextSpecific(2).constructed(), new ASN1OctetString(this.responseToken))); 
      if (this.mechListMic != null && this.mechListMic.length > 0)
        negTokenTarg.add(new ASN1TaggedObject(ASN1Tag.contextSpecific(3).constructed(), new ASN1OctetString(this.mechListMic))); 
      writeGss(buffer, new ASN1Sequence(negTokenTarg));
    } catch (IOException e) {
      throw new SpnegoException("Could not write NegTokenTarg to buffer", e);
    } 
  }
  
  public NegTokenTarg read(byte[] bytes) throws SpnegoException {
    return read(new Buffer.PlainBuffer(bytes, Endian.LE));
  }
  
  private NegTokenTarg read(Buffer<?> buffer) throws SpnegoException {
    try (ASN1InputStream is = new ASN1InputStream(new DERDecoder(), buffer.asInputStream())) {
      ASN1Object instance = is.readObject();
      parseSpnegoToken(instance);
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
  
  private void readResponseToken(ASN1Object responseToken) throws SpnegoException {
    if (!(responseToken instanceof ASN1OctetString))
      throw new SpnegoException("Expected the responseToken (OCTET_STRING) contents, not: " + responseToken); 
    this.responseToken = ((ASN1OctetString)responseToken).getValue();
  }
  
  private void readMechListMIC(ASN1Object mic) throws SpnegoException {
    if (!(mic instanceof ASN1OctetString))
      throw new SpnegoException("Expected the responseToken (OCTET_STRING) contents, not: " + mic); 
    this.mechListMic = ((ASN1OctetString)mic).getValue();
  }
  
  private void readSupportedMech(ASN1Object supportedMech) throws SpnegoException {
    if (!(supportedMech instanceof ASN1ObjectIdentifier))
      throw new SpnegoException("Expected the supportedMech (OBJECT IDENTIFIER) contents, not: " + supportedMech); 
    this.supportedMech = (ASN1ObjectIdentifier)supportedMech;
  }
  
  private void readNegResult(ASN1Object object) throws SpnegoException {
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
