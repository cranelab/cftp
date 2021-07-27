package com.hierynomus.smbj.share;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.SecurityDescriptor;
import com.hierynomus.msdtyp.SecurityInformation;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.FileSystemInformationClass;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileDispositionInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.msfscc.fileinformation.FileInformation;
import com.hierynomus.msfscc.fileinformation.FileInformationFactory;
import com.hierynomus.msfscc.fileinformation.ShareInfo;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMB2ImpersonationLevel;
import com.hierynomus.mssmb2.SMB2MessageCommandCode;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2CreateResponse;
import com.hierynomus.mssmb2.messages.SMB2QueryInfoRequest;
import com.hierynomus.mssmb2.messages.SMB2SetInfoRequest;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.common.SmbPath;
import com.hierynomus.smbj.paths.PathResolveException;
import com.hierynomus.smbj.paths.PathResolver;
import com.hierynomus.smbj.session.Session;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class DiskShare extends Share {
  private final PathResolver resolver;
  
  public DiskShare(SmbPath smbPath, TreeConnect treeConnect, PathResolver pathResolver) {
    super(smbPath, treeConnect);
    this.resolver = pathResolver;
  }
  
  public DiskEntry open(String path, Set<AccessMask> accessMask, Set<FileAttributes> attributes, Set<SMB2ShareAccess> shareAccesses, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    SmbPath pathAndFile = new SmbPath(this.smbPath, path);
    SMB2CreateResponseContext response = resolveAndCreateFile(pathAndFile, (SMB2ImpersonationLevel)null, accessMask, attributes, shareAccesses, createDisposition, createOptions);
    return getDiskEntry(path, response);
  }
  
  protected StatusHandler getCreateStatusHandler() {
    return this.resolver.statusHandler();
  }
  
  private SMB2CreateResponseContext resolveAndCreateFile(SmbPath path, SMB2ImpersonationLevel impersonationLevel, Set<AccessMask> accessMask, Set<FileAttributes> fileAttributes, Set<SMB2ShareAccess> shareAccess, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    try {
      SmbPath target = this.resolver.resolve(this.session, path);
      DiskShare resolvedShare = rerouteIfNeeded(path, target);
      return resolvedShare.createFileAndResolve(target, impersonationLevel, accessMask, fileAttributes, shareAccess, createDisposition, createOptions);
    } catch (PathResolveException pre) {
      throw new SMBApiException(pre.getStatus().getValue(), SMB2MessageCommandCode.SMB2_CREATE, "Cannot resolve path " + path, pre);
    } 
  }
  
  private DiskShare rerouteIfNeeded(SmbPath path, SmbPath target) {
    Session connectedSession = this.session;
    if (!path.isOnSameHost(target))
      connectedSession = connectedSession.buildNestedSession(target); 
    if (!path.isOnSameShare(target))
      return (DiskShare)connectedSession.connectShare(target.getShareName()); 
    return this;
  }
  
  private SMB2CreateResponseContext createFileAndResolve(SmbPath path, SMB2ImpersonationLevel impersonationLevel, Set<AccessMask> accessMask, Set<FileAttributes> fileAttributes, Set<SMB2ShareAccess> shareAccess, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    SMB2CreateResponse resp = createFile(path, impersonationLevel, accessMask, fileAttributes, shareAccess, createDisposition, createOptions);
    try {
      SmbPath target = this.resolver.resolve(this.session, resp, path);
      DiskShare resolveShare = rerouteIfNeeded(path, target);
      if (!path.equals(target))
        return resolveShare.createFileAndResolve(target, impersonationLevel, accessMask, fileAttributes, shareAccess, createDisposition, createOptions); 
    } catch (PathResolveException e) {
      throw new SMBApiException(e.getStatusCode(), SMB2MessageCommandCode.SMB2_CREATE, "Cannot resolve path " + path, e);
    } 
    return new SMB2CreateResponseContext(resp, path, this);
  }
  
  protected DiskEntry getDiskEntry(String path, SMB2CreateResponseContext responseContext) {
    SMB2CreateResponse response = responseContext.resp;
    if (response.getFileAttributes().contains(FileAttributes.FILE_ATTRIBUTE_DIRECTORY))
      return new Directory(response.getFileId(), responseContext.share, responseContext.target.toUncPath()); 
    return new File(response.getFileId(), responseContext.share, responseContext.target.toUncPath());
  }
  
  public Directory openDirectory(String path, Set<AccessMask> accessMask, Set<FileAttributes> attributes, Set<SMB2ShareAccess> shareAccesses, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    EnumSet<SMB2CreateOptions> actualCreateOptions = (createOptions != null) ? EnumSet.<SMB2CreateOptions>copyOf(createOptions) : EnumSet.<SMB2CreateOptions>noneOf(SMB2CreateOptions.class);
    actualCreateOptions.add(SMB2CreateOptions.FILE_DIRECTORY_FILE);
    actualCreateOptions.remove(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE);
    EnumSet<FileAttributes> actualAttributes = (attributes != null) ? EnumSet.<FileAttributes>copyOf(attributes) : EnumSet.<FileAttributes>noneOf(FileAttributes.class);
    actualAttributes.add(FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
    return (Directory)open(path, accessMask, actualAttributes, shareAccesses, createDisposition, actualCreateOptions);
  }
  
  public File openFile(String path, Set<AccessMask> accessMask, Set<FileAttributes> attributes, Set<SMB2ShareAccess> shareAccesses, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    EnumSet<SMB2CreateOptions> actualCreateOptions = (createOptions != null) ? EnumSet.<SMB2CreateOptions>copyOf(createOptions) : EnumSet.<SMB2CreateOptions>noneOf(SMB2CreateOptions.class);
    actualCreateOptions.add(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE);
    actualCreateOptions.remove(SMB2CreateOptions.FILE_DIRECTORY_FILE);
    EnumSet<FileAttributes> actualAttributes = (attributes != null) ? EnumSet.<FileAttributes>copyOf(attributes) : EnumSet.<FileAttributes>noneOf(FileAttributes.class);
    actualAttributes.remove(FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
    return (File)open(path, accessMask, actualAttributes, shareAccesses, createDisposition, actualCreateOptions);
  }
  
  private static StatusHandler FILE_EXISTS_STATUS_HANDLER = new StatusHandler() {
      public boolean isSuccess(long statusCode) {
        return (statusCode == NtStatus.STATUS_OBJECT_NAME_NOT_FOUND.getValue() || statusCode == NtStatus.STATUS_OBJECT_PATH_NOT_FOUND.getValue() || statusCode == NtStatus.STATUS_FILE_IS_A_DIRECTORY.getValue());
      }
    };
  
  public boolean fileExists(String path) throws SMBApiException {
    return exists(path, EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE), FILE_EXISTS_STATUS_HANDLER);
  }
  
  private static StatusHandler FOLDER_EXISTS_STATUS_HANDLER = new StatusHandler() {
      public boolean isSuccess(long statusCode) {
        return (statusCode == NtStatus.STATUS_OBJECT_NAME_NOT_FOUND.getValue() || statusCode == NtStatus.STATUS_OBJECT_PATH_NOT_FOUND.getValue() || statusCode == NtStatus.STATUS_NOT_A_DIRECTORY.getValue());
      }
    };
  
  public boolean folderExists(String path) throws SMBApiException {
    return exists(path, EnumSet.of(SMB2CreateOptions.FILE_DIRECTORY_FILE), FOLDER_EXISTS_STATUS_HANDLER);
  }
  
  private boolean exists(String path, EnumSet<SMB2CreateOptions> createOptions, StatusHandler statusHandler) throws SMBApiException {
    try (DiskEntry ignored = open(path, EnumSet.of(AccessMask.FILE_READ_ATTRIBUTES), EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, createOptions)) {
      return true;
    } catch (SMBApiException sae) {
      if (statusHandler.isSuccess(sae.getStatusCode()))
        return false; 
      throw sae;
    } 
  }
  
  public List<FileIdBothDirectoryInformation> list(String path) throws SMBApiException {
    return list(path, FileIdBothDirectoryInformation.class, (String)null);
  }
  
  public List<FileIdBothDirectoryInformation> list(String path, String searchPattern) throws SMBApiException {
    return list(path, FileIdBothDirectoryInformation.class, searchPattern);
  }
  
  public <I extends com.hierynomus.msfscc.fileinformation.FileDirectoryQueryableInformation> List<I> list(String path, Class<I> informationClass) {
    return list(path, informationClass, (String)null);
  }
  
  public <I extends com.hierynomus.msfscc.fileinformation.FileDirectoryQueryableInformation> List<I> list(String path, Class<I> informationClass, String searchPattern) {
    try (Directory d = openDirectory(path, EnumSet.of(AccessMask.FILE_LIST_DIRECTORY, AccessMask.FILE_READ_ATTRIBUTES, AccessMask.FILE_READ_EA), (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null)) {
      return d.list(informationClass, searchPattern);
    } 
  }
  
  public void mkdir(String path) throws SMBApiException {
    Directory fileHandle = openDirectory(path, 
        
        EnumSet.of(AccessMask.FILE_LIST_DIRECTORY, AccessMask.FILE_ADD_SUBDIRECTORY), 
        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_DIRECTORY), SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_CREATE, 

        
        EnumSet.of(SMB2CreateOptions.FILE_DIRECTORY_FILE));
    fileHandle.close();
  }
  
  public FileAllInformation getFileInformation(String path) throws SMBApiException {
    return getFileInformation(path, FileAllInformation.class);
  }
  
  public <F extends com.hierynomus.msfscc.fileinformation.FileQueryableInformation> F getFileInformation(String path, Class<F> informationClass) throws SMBApiException {
    try (DiskEntry e = open(path, EnumSet.of(AccessMask.FILE_READ_ATTRIBUTES, AccessMask.FILE_READ_EA), (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null)) {
      return (F)e.getFileInformation((Class)informationClass);
    } 
  }
  
  public FileAllInformation getFileInformation(SMB2FileId fileId) throws SMBApiException {
    return getFileInformation(fileId, FileAllInformation.class);
  }
  
  public <F extends com.hierynomus.msfscc.fileinformation.FileQueryableInformation> F getFileInformation(SMB2FileId fileId, Class<F> informationClass) throws SMBApiException {
    FileInformation.Decoder<F> decoder = (FileInformation.Decoder)FileInformationFactory.getDecoder((Class)informationClass);
    byte[] outputBuffer = queryInfo(fileId, SMB2QueryInfoRequest.SMB2QueryInfoType.SMB2_0_INFO_FILE, null, decoder.getInformationClass(), null).getOutputBuffer();
    try {
      return decoder.read(new Buffer.PlainBuffer(outputBuffer, Endian.LE));
    } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  public <F extends com.hierynomus.msfscc.fileinformation.FileSettableInformation> void setFileInformation(SMB2FileId fileId, F information) {
    SMBBuffer buffer = new SMBBuffer();
    FileInformation.Encoder<F> encoder = (FileInformation.Encoder)FileInformationFactory.getEncoder((FileInformation)information);
    encoder.write(information, buffer);
    setInfo(fileId, SMB2SetInfoRequest.SMB2InfoType.SMB2_0_INFO_FILE, null, encoder


        
        .getInformationClass(), buffer
        .getCompactData());
  }
  
  public <F extends com.hierynomus.msfscc.fileinformation.FileSettableInformation> void setFileInformation(String path, F information) throws SMBApiException {
    try (DiskEntry e = open(path, EnumSet.of(AccessMask.FILE_WRITE_ATTRIBUTES, AccessMask.FILE_WRITE_EA), (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null)) {
      e.setFileInformation(information);
    } 
  }
  
  public ShareInfo getShareInformation() throws SMBApiException {
    try (Directory directory = openDirectory("", EnumSet.of(AccessMask.FILE_READ_ATTRIBUTES), (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null)) {
      byte[] outputBuffer = queryInfo(directory.getFileId(), SMB2QueryInfoRequest.SMB2QueryInfoType.SMB2_0_INFO_FILESYSTEM, null, null, FileSystemInformationClass.FileFsFullSizeInformation).getOutputBuffer();
    } 
  }
  
  public void rmdir(String path, boolean recursive) throws SMBApiException {
    if (recursive) {
      List<FileIdBothDirectoryInformation> list = list(path);
      for (FileIdBothDirectoryInformation fi : list) {
        if (fi.getFileName().equals(".") || fi.getFileName().equals(".."))
          continue; 
        String childPath = path + "\\" + fi.getFileName();
        if (!EnumWithValue.EnumUtils.isSet(fi.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
          rm(childPath);
          continue;
        } 
        rmdir(childPath, true);
      } 
      rmdir(path, false);
    } else {
      try (DiskEntry e = open(path, 
            
            EnumSet.of(AccessMask.DELETE), 
            EnumSet.of(FileAttributes.FILE_ATTRIBUTE_DIRECTORY), 
            EnumSet.of(SMB2ShareAccess.FILE_SHARE_DELETE, SMB2ShareAccess.FILE_SHARE_WRITE, SMB2ShareAccess.FILE_SHARE_READ), SMB2CreateDisposition.FILE_OPEN, 
            
            EnumSet.of(SMB2CreateOptions.FILE_DIRECTORY_FILE))) {
        e.deleteOnClose();
      } 
    } 
  }
  
  public void rm(String path) throws SMBApiException {
    try (DiskEntry e = open(path, 
          
          EnumSet.of(AccessMask.DELETE), 
          EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), 
          EnumSet.of(SMB2ShareAccess.FILE_SHARE_DELETE, SMB2ShareAccess.FILE_SHARE_WRITE, SMB2ShareAccess.FILE_SHARE_READ), SMB2CreateDisposition.FILE_OPEN, 
          
          EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE))) {
      e.deleteOnClose();
    } 
  }
  
  public void deleteOnClose(SMB2FileId fileId) {
    setFileInformation(fileId, new FileDispositionInformation(true));
  }
  
  public SecurityDescriptor getSecurityInfo(String path, Set<SecurityInformation> securityInfo) throws SMBApiException {
    EnumSet<AccessMask> accessMask = EnumSet.of(AccessMask.READ_CONTROL);
    if (securityInfo.contains(SecurityInformation.SACL_SECURITY_INFORMATION))
      accessMask.add(AccessMask.ACCESS_SYSTEM_SECURITY); 
    try (DiskEntry e = open(path, accessMask, (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null)) {
      return e.getSecurityInformation(securityInfo);
    } 
  }
  
  public SecurityDescriptor getSecurityInfo(SMB2FileId fileId, Set<SecurityInformation> securityInfo) throws SMBApiException {
    byte[] outputBuffer = queryInfo(fileId, SMB2QueryInfoRequest.SMB2QueryInfoType.SMB2_0_INFO_SECURITY, securityInfo, null, null).getOutputBuffer();
    try {
      return SecurityDescriptor.read(new SMBBuffer(outputBuffer));
    } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  public void setSecurityInfo(String path, Set<SecurityInformation> securityInfo, SecurityDescriptor securityDescriptor) throws SMBApiException {
    Set<AccessMask> accessMask = EnumSet.noneOf(AccessMask.class);
    if (securityInfo.contains(SecurityInformation.SACL_SECURITY_INFORMATION))
      accessMask.add(AccessMask.ACCESS_SYSTEM_SECURITY); 
    if (securityInfo.contains(SecurityInformation.OWNER_SECURITY_INFORMATION) || securityInfo.contains(SecurityInformation.GROUP_SECURITY_INFORMATION))
      accessMask.add(AccessMask.WRITE_OWNER); 
    if (securityInfo.contains(SecurityInformation.DACL_SECURITY_INFORMATION))
      accessMask.add(AccessMask.WRITE_DAC); 
    try (DiskEntry e = open(path, accessMask, (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null)) {
      e.setSecurityInformation(securityDescriptor, securityInfo);
    } 
  }
  
  public void setSecurityInfo(SMB2FileId fileId, Set<SecurityInformation> securityInfo, SecurityDescriptor securityDescriptor) throws SMBApiException {
    SMBBuffer buffer = new SMBBuffer();
    securityDescriptor.write(buffer);
    setInfo(fileId, SMB2SetInfoRequest.SMB2InfoType.SMB2_0_INFO_SECURITY, securityInfo, null, buffer



        
        .getCompactData());
  }
  
  public String toString() {
    return getClass().getSimpleName() + "[" + getSmbPath() + "]";
  }
  
  static class SMB2CreateResponseContext {
    final SMB2CreateResponse resp;
    
    final DiskShare share;
    
    final SmbPath target;
    
    public SMB2CreateResponseContext(SMB2CreateResponse resp, SmbPath target, DiskShare share) {
      this.resp = resp;
      this.target = target;
      this.share = share;
    }
  }
}
