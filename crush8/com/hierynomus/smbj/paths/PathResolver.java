package com.hierynomus.smbj.paths;

import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.StatusHandler;

public interface PathResolver {
  public static final PathResolver LOCAL = new PathResolver() {
      public SmbPath resolve(Session session, SMB2Packet responsePacket, SmbPath smbPath) {
        return smbPath;
      }
      
      public SmbPath resolve(Session session, SmbPath smbPath) {
        return smbPath;
      }
      
      public StatusHandler statusHandler() {
        return StatusHandler.SUCCESS;
      }
    };
  
  SmbPath resolve(Session paramSession, SMB2Packet paramSMB2Packet, SmbPath paramSmbPath) throws PathResolveException;
  
  SmbPath resolve(Session paramSession, SmbPath paramSmbPath) throws PathResolveException;
  
  StatusHandler statusHandler();
}
