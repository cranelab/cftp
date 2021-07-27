package com.hierynomus.ntlm.messages;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum AvId implements EnumWithValue<AvId> {
  MsvAvEOL(0L),
  MsvAvNbComputerName(1L),
  MsvAvNdDomainName(2L),
  MsvAvDnsComputerName(3L),
  MsvAvDnsDomainName(4L),
  MsvAvDnsTreeName(5L),
  MsvAvFlags(6L),
  MsvAvTimestamp(7L),
  MsvAvSingleHost(8L),
  MsvAvTargetName(9L),
  MsvChannelBindings(10L);
  
  private final long value;
  
  AvId(long i) {
    this.value = i;
  }
  
  public long getValue() {
    return this.value;
  }
}
