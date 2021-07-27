package com.hierynomus.msdfsc;

import com.hierynomus.msdfsc.messages.DFSReferral;
import com.hierynomus.msdfsc.messages.SMB2GetDFSReferralResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class ReferralCache {
  private ReferralCacheNode cacheRoot = new ReferralCacheNode("<root>");
  
  public static class TargetSetEntry {
    final String targetPath;
    
    final boolean targetSetBoundary;
    
    public TargetSetEntry(String targetPath, boolean targetSetBoundary) {
      this.targetPath = targetPath;
      this.targetSetBoundary = targetSetBoundary;
    }
    
    public String getTargetPath() {
      return this.targetPath;
    }
    
    public String toString() {
      return "TargetSetEntry[" + this.targetPath + ",targetSetBoundary=" + this.targetSetBoundary + "]";
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
    private final String dfsPathPrefix;
    
    private final DFSReferral.ServerType rootOrLink;
    
    private final boolean interlink;
    
    private final int ttl;
    
    private final long expires;
    
    private final boolean targetFailback;
    
    private final ReferralCache.TargetSetEntry targetHint;
    
    private final List<ReferralCache.TargetSetEntry> targetList;
    
    public ReferralCacheEntry(SMB2GetDFSReferralResponse response, DomainCache domainCache) {
      List<DFSReferral> referralEntries = response.getReferralEntries();
      for (DFSReferral referralEntry : referralEntries) {
        if (referralEntry.getPath() == null)
          throw new IllegalStateException("Path cannot be null for a ReferralCacheEntry?"); 
      } 
      DFSReferral firstReferral = referralEntries.get(0);
      this.dfsPathPrefix = firstReferral.getDfsPath();
      this.rootOrLink = firstReferral.getServerType();
      boolean interlink = (response.getReferralHeaderFlags().contains(SMB2GetDFSReferralResponse.ReferralHeaderFlags.ReferralServers) && !response.getReferralHeaderFlags().contains(SMB2GetDFSReferralResponse.ReferralHeaderFlags.StorageServers));
      if (!interlink && referralEntries.size() == 1) {
        List<String> pathEntries = (new DFSPath(firstReferral.getPath())).getPathComponents();
        interlink = (domainCache.lookup(pathEntries.get(0)) != null);
      } 
      this.interlink = interlink;
      this.ttl = firstReferral.getTtl();
      this.expires = System.currentTimeMillis() + this.ttl * 1000L;
      this.targetFailback = response.getReferralHeaderFlags().contains(SMB2GetDFSReferralResponse.ReferralHeaderFlags.TargetFailback);
      List<ReferralCache.TargetSetEntry> targetList = new ArrayList<>(referralEntries.size());
      for (DFSReferral r : referralEntries) {
        ReferralCache.TargetSetEntry e = new ReferralCache.TargetSetEntry(r.getPath(), false);
        targetList.add(e);
      } 
      this.targetHint = targetList.get(0);
      this.targetList = Collections.unmodifiableList(targetList);
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
      return this.dfsPathPrefix + "->" + this.targetHint.targetPath + "(" + this.rootOrLink + "), " + this.targetList;
    }
  }
  
  private static class ReferralCacheNode {
    static final AtomicReferenceFieldUpdater<ReferralCacheNode, ReferralCache.ReferralCacheEntry> ENTRY_UPDATER = AtomicReferenceFieldUpdater.newUpdater(ReferralCacheNode.class, ReferralCache.ReferralCacheEntry.class, "entry");
    
    private final String pathComponent;
    
    private final Map<String, ReferralCacheNode> childNodes = new ConcurrentHashMap<>();
    
    private volatile ReferralCache.ReferralCacheEntry entry;
    
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
        ENTRY_UPDATER.set(this, entry);
      } 
    }
    
    ReferralCache.ReferralCacheEntry getReferralEntry(Iterator<String> pathComponents) {
      if (pathComponents.hasNext()) {
        String component = ((String)pathComponents.next()).toLowerCase();
        ReferralCacheNode referralCacheNode = this.childNodes.get(component);
        if (referralCacheNode != null)
          return referralCacheNode.getReferralEntry(pathComponents); 
      } 
      return ENTRY_UPDATER.get(this);
    }
    
    void clear() {
      this.childNodes.clear();
      ENTRY_UPDATER.set(this, null);
    }
  }
}
