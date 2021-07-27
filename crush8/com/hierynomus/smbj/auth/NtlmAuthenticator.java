package com.hierynomus.smbj.auth;

import com.hierynomus.asn1.types.primitive.ASN1ObjectIdentifier;
import com.hierynomus.ntlm.functions.NtlmFunctions;
import com.hierynomus.ntlm.messages.AvId;
import com.hierynomus.ntlm.messages.NtlmAuthenticate;
import com.hierynomus.ntlm.messages.NtlmChallenge;
import com.hierynomus.ntlm.messages.NtlmNegotiate;
import com.hierynomus.ntlm.messages.NtlmNegotiateFlag;
import com.hierynomus.protocol.commons.ByteArrayUtils;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import com.hierynomus.security.SecurityProvider;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.spnego.NegTokenInit;
import com.hierynomus.spnego.NegTokenTarg;
import com.hierynomus.spnego.SpnegoException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NtlmAuthenticator implements Authenticator {
  private static final Logger logger = LoggerFactory.getLogger(NtlmAuthenticator.class);
  
  private static final ASN1ObjectIdentifier NTLMSSP = new ASN1ObjectIdentifier("1.3.6.1.4.1.311.2.2.10");
  
  private SecurityProvider securityProvider;
  
  private Random random;
  
  public static class Factory implements com.hierynomus.protocol.commons.Factory.Named<Authenticator> {
    public String getName() {
      return NtlmAuthenticator.NTLMSSP.getValue();
    }
    
    public NtlmAuthenticator create() {
      return new NtlmAuthenticator();
    }
  }
  
  private boolean initialized = false;
  
  private boolean completed = false;
  
  public AuthenticateResponse authenticate(AuthenticationContext context, byte[] gssToken, Session session) throws IOException {
    try {
      byte[] sessionkey;
      AuthenticateResponse response = new AuthenticateResponse();
      if (this.completed)
        return null; 
      if (!this.initialized) {
        logger.debug("Initialized Authentication of {} using NTLM", context.getUsername());
        NtlmNegotiate ntlmNegotiate = new NtlmNegotiate();
        this.initialized = true;
        response.setNegToken(negTokenInit(ntlmNegotiate));
        return response;
      } 
      logger.debug("Received token: {}", ByteArrayUtils.printHex(gssToken));
      NtlmFunctions ntlmFunctions = new NtlmFunctions(this.random, this.securityProvider);
      NegTokenTarg negTokenTarg = (new NegTokenTarg()).read(gssToken);
      BigInteger negotiationResult = negTokenTarg.getNegotiationResult();
      NtlmChallenge challenge = new NtlmChallenge();
      try {
        challenge.read(new Buffer.PlainBuffer(negTokenTarg.getResponseToken(), Endian.LE));
      } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
        throw new IOException(e);
      } 
      logger.debug("Received NTLM challenge from: {}", challenge.getTargetName());
      response.setWindowsVersion(challenge.getVersion());
      response.setNetBiosName(challenge.getAvPairString(AvId.MsvAvNbComputerName));
      byte[] serverChallenge = challenge.getServerChallenge();
      byte[] responseKeyNT = ntlmFunctions.NTOWFv2(String.valueOf(context.getPassword()), context.getUsername(), context.getDomain());
      byte[] ntlmv2ClientChallenge = ntlmFunctions.getNTLMv2ClientChallenge(challenge.getTargetInfo());
      byte[] ntlmv2Response = ntlmFunctions.getNTLMv2Response(responseKeyNT, serverChallenge, ntlmv2ClientChallenge);
      byte[] userSessionKey = ntlmFunctions.hmac_md5(responseKeyNT, new byte[][] { Arrays.copyOfRange(ntlmv2Response, 0, 16) });
      EnumSet<NtlmNegotiateFlag> negotiateFlags = challenge.getNegotiateFlags();
      if (negotiateFlags.contains(NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_KEY_EXCH) && (negotiateFlags
        .contains(NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_SIGN) || negotiateFlags
        .contains(NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_SEAL) || negotiateFlags
        .contains(NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_ALWAYS_SIGN))) {
        byte[] masterKey = new byte[16];
        this.random.nextBytes(masterKey);
        sessionkey = ntlmFunctions.encryptRc4(userSessionKey, masterKey);
        response.setSigningKey(masterKey);
      } else {
        sessionkey = userSessionKey;
        response.setSigningKey(sessionkey);
      } 
      this.completed = true;
      Object msvAvFlags = challenge.getAvPairObject(AvId.MsvAvFlags);
      if (msvAvFlags instanceof Long && (((Long)msvAvFlags).longValue() & 0x2L) > 0L) {
        NtlmAuthenticate ntlmAuthenticate = new NtlmAuthenticate(new byte[0], ntlmv2Response, context.getUsername(), context.getDomain(), null, sessionkey, EnumWithValue.EnumUtils.toLong(negotiateFlags), true);
        Buffer.PlainBuffer concatenatedBuffer = new Buffer.PlainBuffer(Endian.LE);
        concatenatedBuffer.putRawBytes(negTokenTarg.getResponseToken());
        concatenatedBuffer.putRawBytes(challenge.getServerChallenge());
        ntlmAuthenticate.writeAutentificateMessage(concatenatedBuffer);
        byte[] mic = ntlmFunctions.hmac_md5(userSessionKey, new byte[][] { concatenatedBuffer.getCompactData() });
        ntlmAuthenticate.setMic(mic);
        response.setNegToken(negTokenTarg(ntlmAuthenticate, negTokenTarg.getResponseToken()));
        return response;
      } 
      NtlmAuthenticate resp = new NtlmAuthenticate(new byte[0], ntlmv2Response, context.getUsername(), context.getDomain(), null, sessionkey, EnumWithValue.EnumUtils.toLong(negotiateFlags), false);
      response.setNegToken(negTokenTarg(resp, negTokenTarg.getResponseToken()));
      return response;
    } catch (SpnegoException spne) {
      throw new SMBRuntimeException(spne);
    } 
  }
  
  private byte[] negTokenInit(NtlmNegotiate ntlmNegotiate) throws SpnegoException {
    NegTokenInit negTokenInit = new NegTokenInit();
    negTokenInit.addSupportedMech(NTLMSSP);
    Buffer.PlainBuffer ntlmBuffer = new Buffer.PlainBuffer(Endian.LE);
    ntlmNegotiate.write(ntlmBuffer);
    negTokenInit.setMechToken(ntlmBuffer.getCompactData());
    Buffer.PlainBuffer negTokenBuffer = new Buffer.PlainBuffer(Endian.LE);
    negTokenInit.write(negTokenBuffer);
    return negTokenBuffer.getCompactData();
  }
  
  private byte[] negTokenTarg(NtlmAuthenticate resp, byte[] responseToken) throws SpnegoException {
    NegTokenTarg targ = new NegTokenTarg();
    targ.setResponseToken(responseToken);
    Buffer.PlainBuffer ntlmBuffer = new Buffer.PlainBuffer(Endian.LE);
    resp.write(ntlmBuffer);
    targ.setResponseToken(ntlmBuffer.getCompactData());
    Buffer.PlainBuffer negTokenBuffer = new Buffer.PlainBuffer(Endian.LE);
    targ.write(negTokenBuffer);
    return negTokenBuffer.getCompactData();
  }
  
  public void init(SmbConfig config) {
    this.securityProvider = config.getSecurityProvider();
    this.random = config.getRandomProvider();
  }
  
  public boolean supports(AuthenticationContext context) {
    return context.getClass().equals(AuthenticationContext.class);
  }
}
