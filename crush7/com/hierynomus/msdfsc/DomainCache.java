package com.hierynomus.msdfsc;

import com.hierynomus.msdfsc.messages.DFSReferral;
import com.hierynomus.msdfsc.messages.SMB2GetDFSReferralResponse;
import com.hierynomus.protocol.commons.EnumWithValue;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DomainCache {
  private Map<String, DomainCacheEntry> cache = new ConcurrentHashMap<String, DomainCacheEntry>();
  
  public static class DomainCacheEntry {
    String domainName;
    
    String DCHint;
    
    List<String> DCList;
    
    public DomainCacheEntry(SMB2GetDFSReferralResponse response) {
      if (response.getReferralEntries().size() != 1)
        throw new IllegalStateException("Expecting exactly 1 referral for a domain referral, found: " + response.getReferralEntries().size()); 
      DFSReferral dfsReferral = response.getReferralEntries().get(0);
      if (!EnumWithValue.EnumUtils.isSet(dfsReferral.getReferralEntryFlags(), DFSReferral.ReferralEntryFlags.NameListReferral))
        throw new IllegalStateException("Referral Entry for '" + dfsReferral.getSpecialName() + "' does not have NameListReferral bit set."); 
      this.domainName = dfsReferral.getSpecialName();
      this.DCHint = dfsReferral.getExpandedNames().get(0);
      this.DCList = dfsReferral.getExpandedNames();
    }
    
    public String getDomainName() {
      return this.domainName;
    }
    
    public String getDCHint() {
      return this.DCHint;
    }
    
    public List<String> getDCList() {
      return this.DCList;
    }
    
    public String toString() {
      return String.valueOf(this.domainName) + "->" + this.DCHint + ", " + this.DCList;
    }
  }
  
  public DomainCacheEntry lookup(String domainName) {
    return this.cache.get(domainName);
  }
  
  public void put(DomainCacheEntry domainCacheEntry) {
    this.cache.put(domainCacheEntry.domainName, domainCacheEntry);
  }
}
