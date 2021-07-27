package com.hierynomus.smbj.share;

import com.hierynomus.msdtyp.SecurityDescriptor;
import com.hierynomus.msdtyp.SecurityInformation;
import com.hierynomus.msfscc.fileinformation.FileAllInformation;
import com.hierynomus.msfscc.fileinformation.FileLinkInformation;
import com.hierynomus.msfscc.fileinformation.FileRenameInformation;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMBApiException;
import java.io.Closeable;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DiskEntry implements Closeable {
  protected final Logger logger = LoggerFactory.getLogger(getClass());
  
  protected DiskShare share;
  
  protected SMB2FileId fileId;
  
  protected String fileName;
  
  DiskEntry(SMB2FileId fileId, DiskShare share, String fileName) {
    this.share = share;
    this.fileId = fileId;
    this.fileName = fileName;
  }
  
  public void close() {
    this.share.closeFileId(this.fileId);
  }
  
  public SMB2FileId getFileId() {
    return this.fileId;
  }
  
  public String getFileName() {
    return this.fileName;
  }
  
  public DiskShare getDiskShare() {
    return this.share;
  }
  
  public FileAllInformation getFileInformation() throws SMBApiException {
    return getFileInformation(FileAllInformation.class);
  }
  
  public <F extends com.hierynomus.msfscc.fileinformation.FileQueryableInformation> F getFileInformation(Class<F> informationClass) throws SMBApiException {
    return this.share.getFileInformation(this.fileId, informationClass);
  }
  
  public <F extends com.hierynomus.msfscc.fileinformation.FileSettableInformation> void setFileInformation(F information) {
    this.share.setFileInformation(this.fileId, information);
  }
  
  public SecurityDescriptor getSecurityInformation(Set<SecurityInformation> securityInfo) throws SMBApiException {
    return this.share.getSecurityInfo(this.fileId, securityInfo);
  }
  
  public void setSecurityInformation(SecurityDescriptor securityDescriptor) throws SMBApiException {
    EnumSet<SecurityInformation> securityInfo = EnumSet.noneOf(SecurityInformation.class);
    if (securityDescriptor.getOwnerSid() != null)
      securityInfo.add(SecurityInformation.OWNER_SECURITY_INFORMATION); 
    if (securityDescriptor.getGroupSid() != null)
      securityInfo.add(SecurityInformation.GROUP_SECURITY_INFORMATION); 
    if (securityDescriptor.getControl().contains(SecurityDescriptor.Control.DP))
      securityInfo.add(SecurityInformation.DACL_SECURITY_INFORMATION); 
    if (securityDescriptor.getControl().contains(SecurityDescriptor.Control.SP))
      securityInfo.add(SecurityInformation.SACL_SECURITY_INFORMATION); 
    this.share.setSecurityInfo(this.fileId, securityInfo, securityDescriptor);
  }
  
  public void setSecurityInformation(SecurityDescriptor securityDescriptor, Set<SecurityInformation> securityInfo) throws SMBApiException {
    this.share.setSecurityInfo(this.fileId, securityInfo, securityDescriptor);
  }
  
  public void rename(String newName) throws SMBApiException {
    rename(newName, false);
  }
  
  public void rename(String newName, boolean replaceIfExist) throws SMBApiException {
    rename(newName, replaceIfExist, 0L);
  }
  
  public void rename(String newName, boolean replaceIfExist, long rootDirectory) throws SMBApiException {
    FileRenameInformation renameInfo = new FileRenameInformation(replaceIfExist, rootDirectory, newName);
    setFileInformation(renameInfo);
  }
  
  public void createHardlink(String linkname) throws SMBApiException {
    createHardlink(linkname, false);
  }
  
  public void createHardlink(String linkname, boolean replaceIfExist) throws SMBApiException {
    FileLinkInformation linkInfo = new FileLinkInformation(replaceIfExist, linkname);
    setFileInformation(linkInfo);
  }
  
  public byte[] ioctl(int ctlCode, boolean isFsCtl, byte[] inData, int inOffset, int inLength) {
    return this.share.ioctl(this.fileId, ctlCode, isFsCtl, inData, inOffset, inLength);
  }
  
  public int ioctl(int ctlCode, boolean isFsCtl, byte[] inData, int inOffset, int inLength, byte[] outData, int outOffset, int outLength) {
    return this.share.ioctl(this.fileId, ctlCode, isFsCtl, inData, inOffset, inLength, outData, outOffset, outLength);
  }
  
  public void flush() {
    this.share.flush(this.fileId);
  }
  
  public void deleteOnClose() {
    this.share.deleteOnClose(this.fileId);
  }
  
  public void closeSilently() {
    try {
      close();
    } catch (Exception e) {
      this.logger.warn("File close failed for {},{},{}", new Object[] { this.fileName, this.share, this.fileId, e });
    } 
  }
}
