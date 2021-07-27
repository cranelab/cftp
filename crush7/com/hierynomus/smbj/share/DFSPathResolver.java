package com.hierynomus.smbj.share;

import com.hierynomus.msdfsc.DFSException;
import com.hierynomus.msdfsc.DFSPath;
import com.hierynomus.msdfsc.DomainCache;
import com.hierynomus.msdfsc.ReferralCache;
import com.hierynomus.msdfsc.messages.SMB2GetDFSReferralRequest;
import com.hierynomus.msdfsc.messages.SMB2GetDFSReferralResponse;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb2.messages.SMB2IoctlResponse;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.concurrent.Futures;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.io.BufferByteChunkProvider;
import com.hierynomus.smbj.session.Session;
import java.io.IOException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DFSPathResolver {
  private static final Logger logger = LoggerFactory.getLogger(DFSPathResolver.class);
  
  private static final long FSCTL_DFS_GET_REFERRALS = 393620L;
  
  private static final long FSCTL_DFS_GET_REFERRALS_EX = 393648L;
  
  private enum DfsRequestType {
    DOMAIN, DC, SYSVOL, ROOT, LINK;
  }
  
  private ReferralCache referralCache = new ReferralCache();
  
  private DomainCache domainCache = new DomainCache();
  
  public String resolve(Session session, String uncPath) throws PathResolveException {
    logger.info("Starting DFS resolution for {}", uncPath);
    DFSPath dfsPath = new DFSPath(uncPath);
    ResolveState state = new ResolveState(dfsPath);
    DFSPath resolved = step1(session, state);
    return resolved.toPath();
  }
  
  private DFSPath step1(Session session, ResolveState state) throws DFSException {
    logger.trace("DFS[1]: {}", state);
    if (state.path.hasOnlyOnePathComponent() || state.path.isIpc())
      return step12(state); 
    return step2(session, state);
  }
  
  private DFSPath step2(Session session, ResolveState state) throws DFSException {
    logger.trace("DFS[2]: {}", state);
    ReferralCache.ReferralCacheEntry lookup = this.referralCache.lookup(state.path);
    if (lookup == null || (lookup.isExpired() && lookup.isRoot()))
      return step5(session, state); 
    if (lookup.isExpired())
      return step9(session, state, lookup); 
    if (lookup.isLink())
      return step4(session, state, lookup); 
    return step3(session, state, lookup);
  }
  
  private DFSPath step3(Session session, ResolveState state, ReferralCache.ReferralCacheEntry lookup) {
    logger.trace("DFS[3]: {}", state);
    state.path = state.path.replacePrefix(lookup.getDfsPathPrefix(), lookup.getTargetHint().getTargetPath());
    state.isDFSPath = true;
    return step8(session, state, lookup);
  }
  
  private DFSPath step4(Session session, ResolveState state, ReferralCache.ReferralCacheEntry lookup) throws DFSException {
    logger.trace("DFS[4]: {}", state);
    if (state.path.isSysVolOrNetLogon())
      return step3(session, state, lookup); 
    if (lookup.isInterlink())
      return step11(session, state, lookup); 
    return step3(session, state, lookup);
  }
  
  private DFSPath step5(Session session, ResolveState state) throws DFSException {
    logger.trace("DFS[5]: {}", state);
    String potentialDomain = state.path.getPathComponents().get(0);
    DomainCache.DomainCacheEntry domainCacheEntry = this.domainCache.lookup(potentialDomain);
    if (domainCacheEntry == null) {
      state.hostName = potentialDomain;
      state.resolvedDomainEntry = false;
      return step6(session, state);
    } 
    if (domainCacheEntry.getDCHint() == null || domainCacheEntry.getDCHint().isEmpty()) {
      String bootstrapDC = session.getAuthenticationContext().getDomain();
      ReferralResult result = sendDfsReferralRequest(DfsRequestType.DC, bootstrapDC, session, state.path);
      if (!result.status.isSuccess())
        return step13(session, state, result); 
      domainCacheEntry = result.domainCacheEntry;
    } 
    if (state.path.isSysVolOrNetLogon())
      return step10(session, state, domainCacheEntry); 
    state.hostName = domainCacheEntry.getDCHint();
    state.resolvedDomainEntry = true;
    return step6(session, state);
  }
  
  private DFSPath step6(Session session, ResolveState state) throws DFSException {
    logger.trace("DFS[6]: {}", state);
    ReferralResult result = sendDfsReferralRequest(DfsRequestType.ROOT, state.path.getPathComponents().get(0), session, state.path);
    if (result.status.isSuccess())
      return step7(session, state, result.referralCacheEntry); 
    if (state.resolvedDomainEntry)
      return step13(session, state, result); 
    if (state.isDFSPath)
      return step14(session, state, result); 
    return step12(state);
  }
  
  private DFSPath step7(Session session, ResolveState state, ReferralCache.ReferralCacheEntry lookup) throws DFSException {
    logger.trace("DFS[7]: {}", state);
    if (lookup.isRoot())
      return step3(session, state, lookup); 
    return step4(session, state, lookup);
  }
  
  private DFSPath step8(Session session, ResolveState state, ReferralCache.ReferralCacheEntry lookup) {
    logger.trace("DFS[8]: {}", state);
    return state.path;
  }
  
  private DFSPath step9(Session session, ResolveState state, ReferralCache.ReferralCacheEntry lookup) throws DFSException {
    logger.trace("DFS[9]: {}", state);
    DFSPath rootPath = new DFSPath(state.path.getPathComponents().subList(0, 2));
    ReferralCache.ReferralCacheEntry rootReferralCacheEntry = this.referralCache.lookup(rootPath);
    if (rootReferralCacheEntry == null)
      throw new IllegalStateException("Could not find referral cache entry for " + rootPath); 
    ReferralResult result = sendDfsReferralRequest(DfsRequestType.LINK, rootReferralCacheEntry.getTargetHint().getTargetPath(), session, state.path);
    if (!result.status.isSuccess())
      return step14(session, state, result); 
    if (result.referralCacheEntry.isRoot())
      return step3(session, state, result.referralCacheEntry); 
    return step4(session, state, result.referralCacheEntry);
  }
  
  private DFSPath step10(Session session, ResolveState state, DomainCache.DomainCacheEntry domainCacheEntry) throws DFSException {
    logger.trace("DFS[10]: {}", state);
    ReferralResult r = sendDfsReferralRequest(DfsRequestType.SYSVOL, domainCacheEntry.getDCHint(), session, state.path);
    if (r.status.isSuccess())
      return step3(session, state, r.referralCacheEntry); 
    return step13(session, state, r);
  }
  
  private DFSPath step11(Session session, ResolveState state, ReferralCache.ReferralCacheEntry lookup) throws DFSException {
    logger.trace("DFS[11]: {}", state);
    state.path = state.path.replacePrefix(lookup.getDfsPathPrefix(), lookup.getTargetHint().getTargetPath());
    state.isDFSPath = true;
    return step2(session, state);
  }
  
  private DFSPath step12(ResolveState state) {
    logger.trace("DFS[12]: {}", state);
    return state.path;
  }
  
  private DFSPath step13(Session session, ResolveState state, ReferralResult result) throws DFSException {
    logger.trace("DFS[13]: {}", state);
    throw new DFSException(result.status, "Cannot get DC for domain '" + (String)state.path.getPathComponents().get(0) + "'");
  }
  
  private DFSPath step14(Session session, ResolveState state, ReferralResult result) throws DFSException {
    logger.trace("DFS[14]: {}", state);
    throw new DFSException(result.status, "DFS request failed for path " + state.path);
  }
  
  private ReferralResult sendDfsReferralRequest(DfsRequestType type, String hostName, Session session, DFSPath path) throws DFSException {
    Session dfsSession = session;
    if (!hostName.equals(session.getConnection().getRemoteHostname())) {
      Connection connection;
      AuthenticationContext auth = session.getAuthenticationContext();
      Connection oldConnection = session.getConnection();
      try {
        connection = oldConnection.getClient().connect(hostName);
      } catch (IOException e) {
        throw new DFSException(e);
      } 
      dfsSession = connection.authenticate(auth);
    } 
    try {
      Share dfsShare = dfsSession.connectShare("IPC$");
      return getReferral(type, dfsShare, path);
    } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
      throw new DFSException(e);
    } catch (IOException e) {
      throw new DFSException(e);
    } 
  }
  
  private ReferralResult getReferral(DfsRequestType type, Share share, DFSPath path) throws TransportException, Buffer.BufferException {
    SMB2GetDFSReferralRequest req = new SMB2GetDFSReferralRequest(path.toPath());
    SMBBuffer buffer = new SMBBuffer();
    req.writeTo(buffer);
    Future<SMB2IoctlResponse> ioctl = share.ioctlAsync(393620L, true, new BufferByteChunkProvider(buffer));
    SMB2IoctlResponse response = Futures.<SMB2IoctlResponse, TransportException>get(ioctl, TransportException.Wrapper);
    return handleReferralResponse(type, response, path);
  }
  
  private ReferralResult handleReferralResponse(DfsRequestType type, SMB2IoctlResponse response, DFSPath originalPath) throws Buffer.BufferException {
    ReferralResult result = new ReferralResult(response.getHeader().getStatus(), null);
    if (result.status == NtStatus.STATUS_SUCCESS) {
      SMB2GetDFSReferralResponse resp = new SMB2GetDFSReferralResponse(originalPath.toPath());
      resp.read(new SMBBuffer(response.getOutputBuffer()));
      switch (type) {
        case null:
          handleDCReferralResponse(result, resp);
          return result;
        case DOMAIN:
          throw new UnsupportedOperationException(DfsRequestType.DOMAIN + " not used yet.");
        case SYSVOL:
        case ROOT:
        case LINK:
          handleRootOrLinkReferralResponse(result, resp);
          return result;
      } 
      throw new IllegalStateException("Encountered unhandled DFS RequestType: " + type);
    } 
    return result;
  }
  
  private void handleRootOrLinkReferralResponse(ReferralResult result, SMB2GetDFSReferralResponse response) {
    if (response.getReferralEntries().isEmpty())
      result.status = NtStatus.STATUS_OBJECT_PATH_NOT_FOUND; 
    ReferralCache.ReferralCacheEntry referralCacheEntry = new ReferralCache.ReferralCacheEntry(response, this.domainCache);
    this.referralCache.put(referralCacheEntry);
    result.referralCacheEntry = referralCacheEntry;
  }
  
  private void handleDCReferralResponse(ReferralResult result, SMB2GetDFSReferralResponse response) {
    if (response.getVersionNumber() < 3)
      return; 
    DomainCache.DomainCacheEntry domainCacheEntry = new DomainCache.DomainCacheEntry(response);
    this.domainCache.put(domainCacheEntry);
    result.domainCacheEntry = domainCacheEntry;
  }
  
  private static class ResolveState {
    DFSPath path;
    
    boolean resolvedDomainEntry = false;
    
    boolean isDFSPath = false;
    
    String hostName = null;
    
    ResolveState(DFSPath path) {
      this.path = path;
    }
    
    public String toString() {
      return "ResolveState{path=" + 
        this.path + 
        ", resolvedDomainEntry=" + this.resolvedDomainEntry + 
        ", isDFSPath=" + this.isDFSPath + 
        ", hostName='" + this.hostName + '\'' + 
        '}';
    }
  }
  
  private static class ReferralResult {
    NtStatus status;
    
    ReferralCache.ReferralCacheEntry referralCacheEntry;
    
    DomainCache.DomainCacheEntry domainCacheEntry;
    
    private ReferralResult(NtStatus status) {
      this.status = status;
    }
    
    private ReferralResult(ReferralCache.ReferralCacheEntry referralCacheEntry) {
      this.referralCacheEntry = referralCacheEntry;
    }
    
    private ReferralResult(DomainCache.DomainCacheEntry domainCacheEntry) {
      this.domainCacheEntry = domainCacheEntry;
    }
  }
}
