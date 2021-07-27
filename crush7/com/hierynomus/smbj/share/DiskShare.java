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
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2CreateResponse;
import com.hierynomus.mssmb2.messages.SMB2QueryInfoRequest;
import com.hierynomus.mssmb2.messages.SMB2SetInfoRequest;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import com.hierynomus.protocol.transport.TransportException;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smbj.common.SMBRuntimeException;
import com.hierynomus.smbj.common.SmbPath;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class DiskShare extends Share {
  public DiskShare(SmbPath smbPath, TreeConnect treeConnect) {
    super(smbPath, treeConnect);
  }
  
  public DiskEntry open(String path, Set<AccessMask> accessMask, Set<FileAttributes> attributes, Set<SMB2ShareAccess> shareAccesses, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    SMB2CreateResponse response = createFile(path, null, accessMask, attributes, shareAccesses, createDisposition, createOptions);
    return getDiskEntry(path, response);
  }
  
  protected DiskEntry getDiskEntry(String path, SMB2CreateResponse response) {
    if (response.getFileAttributes().contains(FileAttributes.FILE_ATTRIBUTE_DIRECTORY))
      return new Directory(response.getFileId(), this, path); 
    return new File(response.getFileId(), this, path);
  }
  
  public Directory openDirectory(String path, Set<AccessMask> accessMask, Set<FileAttributes> attributes, Set<SMB2ShareAccess> shareAccesses, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    EnumSet<SMB2CreateOptions> actualCreateOptions = (createOptions != null) ? EnumSet.<SMB2CreateOptions>copyOf(createOptions) : EnumSet.<SMB2CreateOptions>noneOf(SMB2CreateOptions.class);
    actualCreateOptions.add(SMB2CreateOptions.FILE_DIRECTORY_FILE);
    actualCreateOptions.remove(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE);
    EnumSet<FileAttributes> actualAttributes = (attributes != null) ? EnumSet.<FileAttributes>copyOf(attributes) : EnumSet.<FileAttributes>noneOf(FileAttributes.class);
    actualAttributes.add(FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
    return (Directory)open(
        path, 
        accessMask, 
        actualAttributes, 
        shareAccesses, 
        createDisposition, 
        actualCreateOptions);
  }
  
  public File openFile(String path, Set<AccessMask> accessMask, Set<FileAttributes> attributes, Set<SMB2ShareAccess> shareAccesses, SMB2CreateDisposition createDisposition, Set<SMB2CreateOptions> createOptions) {
    EnumSet<SMB2CreateOptions> actualCreateOptions = (createOptions != null) ? EnumSet.<SMB2CreateOptions>copyOf(createOptions) : EnumSet.<SMB2CreateOptions>noneOf(SMB2CreateOptions.class);
    actualCreateOptions.add(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE);
    actualCreateOptions.remove(SMB2CreateOptions.FILE_DIRECTORY_FILE);
    EnumSet<FileAttributes> actualAttributes = (attributes != null) ? EnumSet.<FileAttributes>copyOf(attributes) : EnumSet.<FileAttributes>noneOf(FileAttributes.class);
    actualAttributes.remove(FileAttributes.FILE_ATTRIBUTE_DIRECTORY);
    return (File)open(
        path, 
        accessMask, 
        actualAttributes, 
        shareAccesses, 
        createDisposition, 
        actualCreateOptions);
  }
  
  public boolean fileExists(String path) throws SMBApiException {
    return exists(path, EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE), EnumSet.of(NtStatus.STATUS_OBJECT_NAME_NOT_FOUND, NtStatus.STATUS_OBJECT_PATH_NOT_FOUND, NtStatus.STATUS_FILE_IS_A_DIRECTORY));
  }
  
  public boolean folderExists(String path) throws SMBApiException {
    return exists(path, EnumSet.of(SMB2CreateOptions.FILE_DIRECTORY_FILE), EnumSet.of(NtStatus.STATUS_OBJECT_NAME_NOT_FOUND, NtStatus.STATUS_OBJECT_PATH_NOT_FOUND, NtStatus.STATUS_NOT_A_DIRECTORY));
  }
  
  private boolean exists(String path, EnumSet<SMB2CreateOptions> createOptions, Set<NtStatus> acceptedStatuses) throws SMBApiException {
    try {
      DiskEntry ignored = open(path, EnumSet.of(AccessMask.FILE_READ_ATTRIBUTES), EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, createOptions);
      return true;
    } catch (SMBApiException sae) {
      if (acceptedStatuses.contains(sae.getStatus()))
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
    Directory d = openDirectory(path, EnumSet.of(AccessMask.GENERIC_READ), (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null);
    return d.list(informationClass, searchPattern);
  }
  
  public void mkdir(String path) throws SMBApiException {
    Directory fileHandle = openDirectory(
        path, 
        EnumSet.of(AccessMask.FILE_LIST_DIRECTORY, AccessMask.FILE_ADD_SUBDIRECTORY), 
        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_DIRECTORY), 
        SMB2ShareAccess.ALL, 
        SMB2CreateDisposition.FILE_CREATE, 
        EnumSet.of(SMB2CreateOptions.FILE_DIRECTORY_FILE));
    fileHandle.close();
  }
  
  public FileAllInformation getFileInformation(String path) throws SMBApiException {
    return getFileInformation(path, FileAllInformation.class);
  }
  
  public <F extends com.hierynomus.msfscc.fileinformation.FileQueryableInformation> F getFileInformation(String path, Class<F> informationClass) throws SMBApiException {
    DiskEntry e = open(path, EnumSet.of(AccessMask.GENERIC_READ), (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null);
    return e.getFileInformation(informationClass);
  }
  
  public FileAllInformation getFileInformation(SMB2FileId fileId) throws SMBApiException, TransportException {
    return getFileInformation(fileId, FileAllInformation.class);
  }
  
  public <F extends com.hierynomus.msfscc.fileinformation.FileQueryableInformation> F getFileInformation(SMB2FileId fileId, Class<F> informationClass) throws SMBApiException {
    FileInformation.Decoder<F> decoder = (FileInformation.Decoder)FileInformationFactory.getDecoder((Class)informationClass);
    byte[] outputBuffer = queryInfo(
        fileId, 
        SMB2QueryInfoRequest.SMB2QueryInfoType.SMB2_0_INFO_FILE, 
        null, 
        decoder.getInformationClass(), 
        null)
      .getOutputBuffer();
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
    setInfo(
        fileId, 
        SMB2SetInfoRequest.SMB2InfoType.SMB2_0_INFO_FILE, 
        null, 
        encoder.getInformationClass(), 
        buffer.getCompactData());
  }
  
  public <F extends com.hierynomus.msfscc.fileinformation.FileSettableInformation> void setFileInformation(String path, F information) throws SMBApiException {
    DiskEntry e = open(path, EnumSet.of(AccessMask.GENERIC_WRITE), (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null);
    e.setFileInformation(information);
  }
  
  public ShareInfo getShareInformation() throws SMBApiException {
    Directory directory = openDirectory("", EnumSet.of(AccessMask.FILE_READ_ATTRIBUTES), (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null);
    byte[] outputBuffer = queryInfo(
        directory.getFileId(), 
        SMB2QueryInfoRequest.SMB2QueryInfoType.SMB2_0_INFO_FILESYSTEM, 
        null, 
        null, 
        FileSystemInformationClass.FileFsFullSizeInformation)
      .getOutputBuffer();
    try {
      return ShareInfo.parseFsFullSizeInformation(new Buffer.PlainBuffer(outputBuffer, Endian.LE));
    } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
      throw new SMBRuntimeException(e);
    } 
  }
  
  public void rmdir(String path, boolean recursive) throws SMBApiException {
    if (recursive) {
      List<FileIdBothDirectoryInformation> list = list(path);
      for (FileIdBothDirectoryInformation fi : list) {
        if (fi.getFileName().equals(".") || fi.getFileName().equals(".."))
          continue; 
        String childPath = String.valueOf(path) + "\\" + fi.getFileName();
        if (!EnumWithValue.EnumUtils.isSet(fi.getFileAttributes(), FileAttributes.FILE_ATTRIBUTE_DIRECTORY)) {
          rm(childPath);
          continue;
        } 
        rmdir(childPath, true);
      } 
      rmdir(path, false);
    } else {
      DiskEntry e = open(
          path, 
          EnumSet.of(AccessMask.DELETE), 
          EnumSet.of(FileAttributes.FILE_ATTRIBUTE_DIRECTORY), 
          EnumSet.of(SMB2ShareAccess.FILE_SHARE_DELETE, SMB2ShareAccess.FILE_SHARE_WRITE, SMB2ShareAccess.FILE_SHARE_READ), 
          SMB2CreateDisposition.FILE_OPEN, 
          EnumSet.of(SMB2CreateOptions.FILE_DIRECTORY_FILE));
      e.deleteOnClose();
    } 
  }
  
  public void rm(String path) throws SMBApiException {
    DiskEntry e = open(
        path, 
        EnumSet.of(AccessMask.DELETE), 
        EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), 
        EnumSet.of(SMB2ShareAccess.FILE_SHARE_DELETE, SMB2ShareAccess.FILE_SHARE_WRITE, SMB2ShareAccess.FILE_SHARE_READ), 
        SMB2CreateDisposition.FILE_OPEN, 
        EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE));
    e.deleteOnClose();
  }
  
  public void deleteOnClose(SMB2FileId fileId) {
    setFileInformation(fileId, new FileDispositionInformation(true));
  }
  
  public SecurityDescriptor getSecurityInfo(String path, Set<SecurityInformation> securityInfo) throws SMBApiException {
    EnumSet<AccessMask> accessMask = EnumSet.of(AccessMask.GENERIC_READ);
    if (securityInfo.contains(SecurityInformation.SACL_SECURITY_INFORMATION))
      accessMask.add(AccessMask.ACCESS_SYSTEM_SECURITY); 
    DiskEntry e = open(path, accessMask, (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null);
    return e.getSecurityInformation(securityInfo);
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
    Set<AccessMask> accessMask = EnumSet.of(AccessMask.GENERIC_WRITE);
    if (securityInfo.contains(SecurityInformation.SACL_SECURITY_INFORMATION))
      accessMask.add(AccessMask.ACCESS_SYSTEM_SECURITY); 
    if (securityInfo.contains(SecurityInformation.OWNER_SECURITY_INFORMATION))
      accessMask.add(AccessMask.WRITE_OWNER); 
    if (securityInfo.contains(SecurityInformation.DACL_SECURITY_INFORMATION))
      accessMask.add(AccessMask.WRITE_DAC); 
    DiskEntry e = open(path, accessMask, (Set<FileAttributes>)null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, (Set<SMB2CreateOptions>)null);
    e.setSecurityInformation(securityDescriptor, securityInfo);
  }
  
  public void setSecurityInfo(SMB2FileId fileId, Set<SecurityInformation> securityInfo, SecurityDescriptor securityDescriptor) throws SMBApiException {
    SMBBuffer buffer = new SMBBuffer();
    securityDescriptor.write(buffer);
    setInfo(
        fileId, 
        SMB2SetInfoRequest.SMB2InfoType.SMB2_0_INFO_SECURITY, 
        securityInfo, 
        null, 
        buffer.getCompactData());
  }
  
  public String toString() {
    return String.valueOf(getClass().getSimpleName()) + "[" + getSmbPath() + "]";
  }
}
