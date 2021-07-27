package com.hierynomus.ntlm.messages;

import com.hierynomus.msdfsc.messages.StandardCharsets;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import java.util.EnumSet;

public class NtlmNegotiate extends NtlmPacket {
  public static final long DEFAULT_FLAGS = EnumWithValue.EnumUtils.toLong(EnumSet.of(
        NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_56, (Enum[])new NtlmNegotiateFlag[] { NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_128, 
          NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_TARGET_INFO, 
          NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY, 
          NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_SIGN, 
          NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_ALWAYS_SIGN, 
          NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_KEY_EXCH, 
          NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_NTLM, 
          NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_NTLM, 
          NtlmNegotiateFlag.NTLMSSP_REQUEST_TARGET, 
          NtlmNegotiateFlag.NTLMSSP_NEGOTIATE_UNICODE }));
  
  private long flags = DEFAULT_FLAGS;
  
  public void write(Buffer.PlainBuffer buffer) {
    buffer.putString("NTLMSSP\000", StandardCharsets.UTF_8);
    buffer.putUInt32(1L);
    buffer.putUInt32(this.flags);
    buffer.putUInt16(0);
    buffer.putUInt16(0);
    buffer.putUInt32(0L);
    buffer.putUInt16(0);
    buffer.putUInt16(0);
    buffer.putUInt32(0L);
  }
}
