package com.hierynomus.smbj.share;

import com.hierynomus.mserref.NtStatus;
import com.hierynomus.msfscc.FileInformationClass;
import com.hierynomus.msfscc.fileinformation.FileDirectoryQueryableInformation;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.msfscc.fileinformation.FileInformation;
import com.hierynomus.msfscc.fileinformation.FileInformationFactory;
import com.hierynomus.mssmb2.SMB2FileId;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.mssmb2.messages.SMB2QueryDirectoryRequest;
import com.hierynomus.mssmb2.messages.SMB2QueryDirectoryResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Directory extends DiskEntry implements Iterable<FileIdBothDirectoryInformation> {
  Directory(SMB2FileId fileId, DiskShare diskShare, String fileName) {
    super(fileId, diskShare, fileName);
  }
  
  public List<FileIdBothDirectoryInformation> list() throws SMBApiException {
    return list(FileIdBothDirectoryInformation.class);
  }
  
  public <F extends FileDirectoryQueryableInformation> List<F> list(Class<F> informationClass) throws SMBApiException {
    return list(informationClass, (String)null);
  }
  
  public <F extends FileDirectoryQueryableInformation> List<F> list(Class<F> informationClass, String searchPattern) {
    List<F> fileList = new ArrayList<>();
    Iterator<F> iterator = iterator(informationClass, searchPattern);
    while (iterator.hasNext())
      fileList.add(iterator.next()); 
    return fileList;
  }
  
  public Iterator<FileIdBothDirectoryInformation> iterator() {
    return iterator(FileIdBothDirectoryInformation.class);
  }
  
  public <F extends FileDirectoryQueryableInformation> Iterator<F> iterator(Class<F> informationClass) {
    return iterator(informationClass, (String)null);
  }
  
  public <F extends FileDirectoryQueryableInformation> Iterator<F> iterator(Class<F> informationClass, String searchPattern) {
    return new DirectoryIterator<>(informationClass, searchPattern);
  }
  
  public SMB2FileId getFileId() {
    return this.fileId;
  }
  
  public String toString() {
    return String.format("Directory{fileId=%s, fileName='%s'}", new Object[] { this.fileId, this.fileName });
  }
  
  private class DirectoryIterator<F extends FileDirectoryQueryableInformation> implements Iterator<F> {
    private final FileInformation.Decoder<F> decoder;
    
    private Iterator<F> currentIterator;
    
    private byte[] currentBuffer;
    
    private F next;
    
    private String searchPattern;
    
    DirectoryIterator(Class<F> informationClass, String searchPattern) {
      this.decoder = FileInformationFactory.getDecoder((Class)informationClass);
      this.searchPattern = searchPattern;
      queryDirectory(true);
      this.next = prepareNext();
    }
    
    public boolean hasNext() {
      return (this.next != null);
    }
    
    public F next() {
      if (!hasNext())
        throw new NoSuchElementException(); 
      F fileInfo = this.next;
      this.next = prepareNext();
      return fileInfo;
    }
    
    private F prepareNext() {
      while (this.currentIterator != null) {
        if (this.currentIterator.hasNext())
          return this.currentIterator.next(); 
        queryDirectory(false);
      } 
      return null;
    }
    
    private void queryDirectory(boolean firstQuery) {
      EnumSet<SMB2QueryDirectoryRequest.SMB2QueryDirectoryFlags> flags;
      DiskShare share = Directory.this.share;
      if (firstQuery) {
        flags = EnumSet.of(SMB2QueryDirectoryRequest.SMB2QueryDirectoryFlags.SMB2_RESTART_SCANS);
      } else {
        flags = EnumSet.noneOf(SMB2QueryDirectoryRequest.SMB2QueryDirectoryFlags.class);
      } 
      FileInformationClass informationClass = this.decoder.getInformationClass();
      SMB2QueryDirectoryResponse qdResp = share.queryDirectory(Directory.this.fileId, flags, informationClass, this.searchPattern);
      long status = qdResp.getHeader().getStatusCode();
      byte[] buffer = qdResp.getOutputBuffer();
      if (status == NtStatus.STATUS_NO_MORE_FILES.getValue() || status == NtStatus.STATUS_NO_SUCH_FILE.getValue() || (this.currentBuffer != null && Arrays.equals(this.currentBuffer, buffer))) {
        this.currentIterator = null;
        this.currentBuffer = null;
      } else {
        this.currentBuffer = buffer;
        this.currentIterator = FileInformationFactory.createFileInformationIterator(this.currentBuffer, this.decoder);
      } 
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
