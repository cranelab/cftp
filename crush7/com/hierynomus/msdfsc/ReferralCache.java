package com.hierynomus.msdfsc;

import com.hierynomus.msdfsc.messages.DFSReferral;
import com.hierynomus.msdfsc.messages.SMB2GetDFSReferralResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReferralCache {
  private ReferralCacheNode cacheRoot = new ReferralCacheNode("<root>");
  
  public static class TargetSetEntry {
    String targetPath;
    
    boolean targetSetBoundary;
    
    public String getTargetPath() {
      return this.targetPath;
    }
  }
  
  public ReferralCacheEntry lookup(DFSPath dfsPath) {
    List<String> pathComponents = dfsPath.getPathComponents();
    ReferralCacheEntry referralEntry = this.cacheRoot.getReferralEntry(pathComponents.iterator());
    return referralEntry;
  }
  
  public void put(ReferralCacheEntry referralCacheEntry) {
    List<String> pathComponents = (new DFSPath(referralCacheEntry.dfsPathPrefix)).getPathComponents();
    this.cacheRoot.addReferralEntry(pathComponents.iterator(), referralCacheEntry);
  }
  
  public void clear() {
    this.cacheRoot.clear();
  }
  
  public static class ReferralCacheEntry {
    String dfsPathPrefix;
    
    DFSReferral.ServerType rootOrLink;
    
    boolean interlink;
    
    int ttl;
    
    long expires;
    
    boolean targetFailback;
    
    ReferralCache.TargetSetEntry targetHint;
    
    List<ReferralCache.TargetSetEntry> targetList;
    
    public ReferralCacheEntry(SMB2GetDFSReferralResponse response, DomainCache domainCache) {
      List<DFSReferral> referralEntries = response.getReferralEntries();
      for (int i = 0; i < referralEntries.size(); i++) {
        if (((DFSReferral)referralEntries.get(i)).getPath() == null)
          throw new IllegalStateException("Path cannot be null for a ReferralCacheEntry?"); 
      } 
      DFSReferral firstReferral = referralEntries.get(0);
      this.dfsPathPrefix = firstReferral.getDfsPath();
      this.rootOrLink = firstReferral.getServerType();
      this.interlink = (response.getReferralHeaderFlags().contains(SMB2GetDFSReferralResponse.ReferralHeaderFlags.ReferralServers) && 
        !response.getReferralHeaderFlags().contains(SMB2GetDFSReferralResponse.ReferralHeaderFlags.StorageServers));
      if (!this.interlink && referralEntries.size() == 1) {
        List<String> pathEntries = (new DFSPath(firstReferral.getPath())).getPathComponents();
        this.interlink = (domainCache.lookup(pathEntries.get(0)) != null);
      } 
      this.ttl = firstReferral.getTtl();
      this.expires = System.currentTimeMillis() + this.ttl * 1000L;
      this.targetFailback = response.getReferralHeaderFlags().contains(SMB2GetDFSReferralResponse.ReferralHeaderFlags.TargetFailback);
      this.targetList = new ArrayList<ReferralCache.TargetSetEntry>(referralEntries.size());
      for (DFSReferral r : referralEntries) {
        ReferralCache.TargetSetEntry e = new ReferralCache.TargetSetEntry();
        e.targetPath = r.getPath();
        this.targetList.add(e);
      } 
      this.targetHint = this.targetList.get(0);
    }
    
    public boolean isExpired() {
      long now = System.currentTimeMillis();
      return (now > this.expires);
    }
    
    public boolean isLink() {
      return (this.rootOrLink == DFSReferral.ServerType.LINK);
    }
    
    public boolean isRoot() {
      return (this.rootOrLink == DFSReferral.ServerType.ROOT);
    }
    
    public boolean isInterlink() {
      return (isLink() && this.interlink);
    }
    
    public String getDfsPathPrefix() {
      return this.dfsPathPrefix;
    }
    
    public ReferralCache.TargetSetEntry getTargetHint() {
      return this.targetHint;
    }
    
    public List<ReferralCache.TargetSetEntry> getTargetList() {
      return this.targetList;
    }
    
    public String toString() {
      return String.valueOf(this.dfsPathPrefix) + "->" + this.targetHint.targetPath + ", " + this.targetList;
    }
  }
  
  static class ReferralCacheNode {
    String pathComponent;
    
    Map<String, ReferralCacheNode> childNodes = new ConcurrentHashMap<String, ReferralCacheNode>();
    
    ReferralCache.ReferralCacheEntry entry;
    
    ReferralCacheNode(String pathComponent) {
      this.pathComponent = pathComponent;
    }
    
    void addReferralEntry(Iterator<String> pathComponents, ReferralCache.ReferralCacheEntry entry) {
      if (pathComponents.hasNext()) {
        String component = ((String)pathComponents.next()).toLowerCase();
        ReferralCacheNode referralCacheNode = this.childNodes.get(component);
        if (referralCacheNode == null)
          this.childNodes.put(component, referralCacheNode = new ReferralCacheNode(component)); 
        referralCacheNode.addReferralEntry(pathComponents, entry);
      } else {
        this.entry = entry;
      } 
    }
    
    ReferralCache.ReferralCacheEntry getReferralEntry(Iterator<String> pathComponents) {
      if (pathComponents.hasNext()) {
        String component = ((String)pathComponents.next()).toLowerCase();
        ReferralCacheNode referralCacheNode = this.childNodes.get(component);
        if (referralCacheNode != null)
          return referralCacheNode.getReferralEntry(pathComponents); 
      } 
      return this.entry;
    }
    
    void clear() {
      this.childNodes = new HashMap<String, ReferralCacheNode>();
      this.entry = null;
    }
  }
}
