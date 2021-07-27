package com.hierynomus.msfscc.fileinformation;

import com.hierynomus.msdfsc.messages.StandardCharsets;
import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msdtyp.MsDataTypes;
import com.hierynomus.msfscc.FileInformationClass;
import com.hierynomus.protocol.commons.buffer.Buffer;
import com.hierynomus.protocol.commons.buffer.Endian;
import com.hierynomus.smbj.common.SMBRuntimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class FileInformationFactory {
  private static final Map<Class, FileInformation.Encoder> encoders = (Map)new HashMap<Class<?>, FileInformation.Encoder>();
  
  private static final Map<Class, FileInformation.Decoder> decoders = (Map)new HashMap<Class<?>, FileInformation.Decoder>();
  
  static {
    decoders.put(FileAccessInformation.class, new FileInformation.Decoder<FileAccessInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileAccessInformation;
          }
          
          public FileAccessInformation read(Buffer inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileAccessInformation(inputBuffer);
          }
        });
    decoders.put(FileAlignmentInformation.class, new FileInformation.Decoder<FileAlignmentInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileAlignmentInformation;
          }
          
          public FileAlignmentInformation read(Buffer inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileAlignmentInformation(inputBuffer);
          }
        });
    decoders.put(FileAllInformation.class, new FileInformation.Decoder<FileAllInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileAllInformation;
          }
          
          public FileAllInformation read(Buffer<?> inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileAllInformation(inputBuffer);
          }
        });
    FileInformation.Codec<FileAllocationInformation> allocationCodec = new FileInformation.Codec<FileAllocationInformation>() {
        public FileInformationClass getInformationClass() {
          return FileInformationClass.FileAllocationInformation;
        }
        
        public FileAllocationInformation read(Buffer inputBuffer) throws Buffer.BufferException {
          return FileInformationFactory.parseFileAllocationInformation(inputBuffer);
        }
        
        public void write(FileAllocationInformation info, Buffer outputBuffer) {
          FileInformationFactory.writeFileAllocationInformation(info, outputBuffer);
        }
      };
    decoders.put(FileAllocationInformation.class, allocationCodec);
    encoders.put(FileAllocationInformation.class, allocationCodec);
    FileInformation.Codec<FileBasicInformation> basicCodec = new FileInformation.Codec<FileBasicInformation>() {
        public FileInformationClass getInformationClass() {
          return FileInformationClass.FileBasicInformation;
        }
        
        public FileBasicInformation read(Buffer inputBuffer) throws Buffer.BufferException {
          return FileInformationFactory.parseFileBasicInformation(inputBuffer);
        }
        
        public void write(FileBasicInformation info, Buffer outputBuffer) {
          FileInformationFactory.writeFileBasicInformation(info, outputBuffer);
        }
      };
    decoders.put(FileBasicInformation.class, basicCodec);
    encoders.put(FileBasicInformation.class, basicCodec);
    FileInformation.Encoder<FileDispositionInformation> dispositionCodec = new FileInformation.Encoder<FileDispositionInformation>() {
        public FileInformationClass getInformationClass() {
          return FileInformationClass.FileDispositionInformation;
        }
        
        public void write(FileDispositionInformation info, Buffer outputBuffer) {
          FileInformationFactory.writeFileDispositionInformation(info, outputBuffer);
        }
      };
    encoders.put(FileDispositionInformation.class, dispositionCodec);
    decoders.put(FileEaInformation.class, new FileInformation.Decoder<FileEaInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileEaInformation;
          }
          
          public FileEaInformation read(Buffer inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileEaInformation(inputBuffer);
          }
        });
    FileInformation.Encoder<FileEndOfFileInformation> endOfFileCodec = new FileInformation.Encoder<FileEndOfFileInformation>() {
        public FileInformationClass getInformationClass() {
          return FileInformationClass.FileEndOfFileInformation;
        }
        
        public void write(FileEndOfFileInformation info, Buffer outputBuffer) {
          FileInformationFactory.writeFileEndOfFileInformation(info, outputBuffer);
        }
      };
    encoders.put(FileEndOfFileInformation.class, endOfFileCodec);
    decoders.put(FileInternalInformation.class, new FileInformation.Decoder<FileInternalInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileInternalInformation;
          }
          
          public FileInternalInformation read(Buffer inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileInternalInformation(inputBuffer);
          }
        });
    FileInformation.Codec<FileModeInformation> modeCodec = new FileInformation.Codec<FileModeInformation>() {
        public FileInformationClass getInformationClass() {
          return FileInformationClass.FileModeInformation;
        }
        
        public FileModeInformation read(Buffer inputBuffer) throws Buffer.BufferException {
          return FileInformationFactory.parseFileModeInformation(inputBuffer);
        }
        
        public void write(FileModeInformation info, Buffer outputBuffer) {
          FileInformationFactory.writeFileModeInformation(info, outputBuffer);
        }
      };
    decoders.put(FileModeInformation.class, modeCodec);
    encoders.put(FileModeInformation.class, modeCodec);
    decoders.put(FilePositionInformation.class, new FileInformation.Decoder<FilePositionInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FilePositionInformation;
          }
          
          public FilePositionInformation read(Buffer inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFilePositionInformation(inputBuffer);
          }
        });
    decoders.put(FileStandardInformation.class, new FileInformation.Decoder<FileStandardInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileStandardInformation;
          }
          
          public FileStandardInformation read(Buffer inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileStandardInformation(inputBuffer);
          }
        });
    decoders.put(FileBothDirectoryInformation.class, new FileInformation.Decoder<FileBothDirectoryInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileBothDirectoryInformation;
          }
          
          public FileBothDirectoryInformation read(Buffer<?> inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileBothDirectoryInformation(inputBuffer);
          }
        });
    decoders.put(FileDirectoryInformation.class, new FileInformation.Decoder<FileDirectoryInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileDirectoryInformation;
          }
          
          public FileDirectoryInformation read(Buffer<?> inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileDirectoryInformation(inputBuffer);
          }
        });
    decoders.put(FileFullDirectoryInformation.class, new FileInformation.Decoder<FileFullDirectoryInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileFullDirectoryInformation;
          }
          
          public FileFullDirectoryInformation read(Buffer<?> inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileFullDirectoryInformation(inputBuffer);
          }
        });
    decoders.put(FileIdBothDirectoryInformation.class, new FileInformation.Decoder<FileIdBothDirectoryInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileIdBothDirectoryInformation;
          }
          
          public FileIdBothDirectoryInformation read(Buffer<?> inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileIdBothDirectoryInformation(inputBuffer);
          }
        });
    decoders.put(FileIdFullDirectoryInformation.class, new FileInformation.Decoder<FileIdFullDirectoryInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileIdFullDirectoryInformation;
          }
          
          public FileIdFullDirectoryInformation read(Buffer<?> inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileIdFullDirectoryInformation(inputBuffer);
          }
        });
    decoders.put(FileNamesInformation.class, new FileInformation.Decoder<FileNamesInformation>() {
          public FileInformationClass getInformationClass() {
            return FileInformationClass.FileNamesInformation;
          }
          
          public FileNamesInformation read(Buffer<?> inputBuffer) throws Buffer.BufferException {
            return FileInformationFactory.parseFileNamesInformation(inputBuffer);
          }
        });
    FileInformation.Encoder<FileRenameInformation> renameCodec = new FileInformation.Encoder<FileRenameInformation>() {
        public FileInformationClass getInformationClass() {
          return FileInformationClass.FileRenameInformation;
        }
        
        public void write(FileRenameInformation info, Buffer<?> outputBuffer) {
          FileInformationFactory.writeFileRenameInformation(info, outputBuffer);
        }
      };
    encoders.put(FileRenameInformation.class, renameCodec);
  }
  
  public static <F extends FileInformation> FileInformation.Encoder<F> getEncoder(F fileInformation) {
    return getEncoder((Class)fileInformation.getClass());
  }
  
  public static <F extends FileInformation> FileInformation.Encoder<F> getEncoder(Class<F> fileInformationClass) {
    FileInformation.Encoder<F> encoder = encoders.get(fileInformationClass);
    if (encoder == null)
      throw new IllegalArgumentException("FileInformationClass not supported - " + fileInformationClass); 
    return encoder;
  }
  
  public static <F extends FileInformation> FileInformation.Decoder<F> getDecoder(Class<F> fileInformationClass) {
    FileInformation.Decoder<F> decoder = decoders.get(fileInformationClass);
    if (decoder == null)
      throw new IllegalArgumentException("FileInformationClass not supported - " + fileInformationClass); 
    return decoder;
  }
  
  public static <F extends FileDirectoryQueryableInformation> List<F> parseFileInformationList(byte[] data, FileInformation.Decoder<F> decoder) throws Buffer.BufferException {
    List<F> _fileInfoList = new ArrayList<F>();
    Iterator<F> iterator = createFileInformationIterator(data, decoder);
    while (iterator.hasNext())
      _fileInfoList.add(iterator.next()); 
    return _fileInfoList;
  }
  
  public static <F extends FileDirectoryQueryableInformation> Iterator<F> createFileInformationIterator(byte[] data, FileInformation.Decoder<F> decoder) {
    return new FileInfoIterator<F>(data, decoder, 0);
  }
  
  private static class FileInfoIterator<F extends FileDirectoryQueryableInformation> implements Iterator<F> {
    private final Buffer.PlainBuffer buffer;
    
    private final FileInformation.Decoder<F> decoder;
    
    private int offsetStart;
    
    private F next;
    
    FileInfoIterator(byte[] data, FileInformation.Decoder<F> decoder, int offsetStart) {
      this.buffer = new Buffer.PlainBuffer(data, Endian.LE);
      this.decoder = decoder;
      this.offsetStart = offsetStart;
      this.next = prepareNext();
    }
    
    public boolean hasNext() {
      return (this.next != null);
    }
    
    public F next() {
      if (this.next == null)
        throw new NoSuchElementException(); 
      F fileInfo = this.next;
      this.next = prepareNext();
      return fileInfo;
    }
    
    private F prepareNext() {
      try {
        FileDirectoryQueryableInformation fileDirectoryQueryableInformation;
        F next = null;
        while (next == null && this.offsetStart != -1) {
          this.buffer.rpos(this.offsetStart);
          FileDirectoryQueryableInformation fileDirectoryQueryableInformation1 = (FileDirectoryQueryableInformation)this.decoder.read(this.buffer);
          int nextOffset = (int)fileDirectoryQueryableInformation1.getNextOffset();
          if (nextOffset == 0) {
            this.offsetStart = -1;
          } else {
            this.offsetStart += nextOffset;
          } 
          fileDirectoryQueryableInformation = fileDirectoryQueryableInformation1;
        } 
        return (F)fileDirectoryQueryableInformation;
      } catch (com.hierynomus.protocol.commons.buffer.Buffer.BufferException e) {
        throw new SMBRuntimeException(e);
      } 
    }
    
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
  
  public static FileAllInformation parseFileAllInformation(Buffer<?> buffer) throws Buffer.BufferException {
    FileBasicInformation basicInformation = parseFileBasicInformation(buffer);
    FileStandardInformation standardInformation = parseFileStandardInformation(buffer);
    FileInternalInformation internalInformation = parseFileInternalInformation(buffer);
    FileEaInformation eaInformation = parseFileEaInformation(buffer);
    FileAccessInformation accessInformation = parseFileAccessInformation(buffer);
    FilePositionInformation positionInformation = parseFilePositionInformation(buffer);
    FileModeInformation modeInformation = parseFileModeInformation(buffer);
    FileAlignmentInformation alignmentInformation = parseFileAlignmentInformation(buffer);
    String nameInformation = parseFileNameInformation(buffer);
    FileAllInformation fi = new FileAllInformation(
        basicInformation, 
        standardInformation, 
        internalInformation, 
        eaInformation, 
        accessInformation, 
        positionInformation, 
        modeInformation, 
        alignmentInformation, 
        nameInformation);
    return fi;
  }
  
  private static String parseFileNameInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long fileNameLen = buffer.readUInt32();
    String fileName = buffer.readString(StandardCharsets.UTF_16LE, (int)fileNameLen / 2);
    return fileName;
  }
  
  private static FileBasicInformation parseFileBasicInformation(Buffer<?> buffer) throws Buffer.BufferException {
    FileTime creationTime = MsDataTypes.readFileTime(buffer);
    FileTime lastAccessTime = MsDataTypes.readFileTime(buffer);
    FileTime lastWriteTime = MsDataTypes.readFileTime(buffer);
    FileTime changeTime = MsDataTypes.readFileTime(buffer);
    long fileAttributes = buffer.readUInt32();
    buffer.skip(4);
    return new FileBasicInformation(creationTime, lastAccessTime, lastWriteTime, changeTime, fileAttributes);
  }
  
  private static void writeFileBasicInformation(FileBasicInformation information, Buffer<?> buffer) {
    MsDataTypes.putFileTime(information.getCreationTime(), buffer);
    MsDataTypes.putFileTime(information.getLastAccessTime(), buffer);
    MsDataTypes.putFileTime(information.getLastWriteTime(), buffer);
    MsDataTypes.putFileTime(information.getChangeTime(), buffer);
    buffer.putUInt32(information.getFileAttributes());
    buffer.putUInt32(0L);
  }
  
  private static void writeFileDispositionInformation(FileDispositionInformation information, Buffer<?> buffer) {
    buffer.putBoolean(information.isDeleteOnClose());
  }
  
  private static FileStandardInformation parseFileStandardInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long allocationSize = buffer.readLong();
    long endOfFile = buffer.readUInt64();
    long numberOfLinks = buffer.readUInt32();
    boolean deletePending = buffer.readBoolean();
    boolean directory = buffer.readBoolean();
    buffer.skip(2);
    return new FileStandardInformation(allocationSize, endOfFile, numberOfLinks, deletePending, directory);
  }
  
  private static FileInternalInformation parseFileInternalInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long indexNumber = buffer.readLong();
    return new FileInternalInformation(indexNumber);
  }
  
  private static FileEaInformation parseFileEaInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long eaSize = buffer.readUInt32();
    return new FileEaInformation(eaSize);
  }
  
  private static void writeFileEndOfFileInformation(FileEndOfFileInformation information, Buffer<?> buffer) {
    buffer.putLong(information.getEndOfFile());
  }
  
  private static FileAccessInformation parseFileAccessInformation(Buffer<?> buffer) throws Buffer.BufferException {
    int accessFlags = (int)buffer.readUInt32();
    return new FileAccessInformation(accessFlags);
  }
  
  private static FileAllocationInformation parseFileAllocationInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long allocationSize = buffer.readLong();
    return new FileAllocationInformation(allocationSize);
  }
  
  private static void writeFileAllocationInformation(FileAllocationInformation info, Buffer outputBuffer) {
    outputBuffer.putLong(info.getAllocationSize());
  }
  
  private static FilePositionInformation parseFilePositionInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long currentByteOffset = buffer.readLong();
    return new FilePositionInformation(currentByteOffset);
  }
  
  private static FileModeInformation parseFileModeInformation(Buffer<?> buffer) throws Buffer.BufferException {
    int mode = (int)buffer.readUInt32();
    return new FileModeInformation(mode);
  }
  
  private static void writeFileModeInformation(FileModeInformation info, Buffer outputBuffer) {
    outputBuffer.putUInt32(info.getMode() & 0xFFFFFFFFL);
  }
  
  private static FileAlignmentInformation parseFileAlignmentInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long alignmentReq = buffer.readUInt32();
    return new FileAlignmentInformation(alignmentReq);
  }
  
  public static FileBothDirectoryInformation parseFileBothDirectoryInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long nextOffset = buffer.readUInt32();
    long fileIndex = buffer.readUInt32();
    FileTime creationTime = MsDataTypes.readFileTime(buffer);
    FileTime lastAccessTime = MsDataTypes.readFileTime(buffer);
    FileTime lastWriteTime = MsDataTypes.readFileTime(buffer);
    FileTime changeTime = MsDataTypes.readFileTime(buffer);
    long endOfFile = buffer.readUInt64();
    long allocationSize = buffer.readUInt64();
    long fileAttributes = buffer.readUInt32();
    long fileNameLen = buffer.readUInt32();
    long eaSize = buffer.readUInt32();
    byte shortNameLen = buffer.readByte();
    buffer.readByte();
    byte[] shortNameBytes = buffer.readRawBytes(24);
    String shortName = new String(shortNameBytes, 0, shortNameLen, StandardCharsets.UTF_16LE);
    buffer.readUInt16();
    String fileName = buffer.readString(StandardCharsets.UTF_16LE, (int)fileNameLen / 2);
    FileBothDirectoryInformation fi = new FileBothDirectoryInformation(
        nextOffset, fileIndex, fileName, 
        creationTime, lastAccessTime, lastWriteTime, changeTime, 
        endOfFile, allocationSize, 
        fileAttributes, 
        eaSize, 
        shortName);
    return fi;
  }
  
  public static FileDirectoryInformation parseFileDirectoryInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long nextOffset = buffer.readUInt32();
    long fileIndex = buffer.readUInt32();
    FileTime creationTime = MsDataTypes.readFileTime(buffer);
    FileTime lastAccessTime = MsDataTypes.readFileTime(buffer);
    FileTime lastWriteTime = MsDataTypes.readFileTime(buffer);
    FileTime changeTime = MsDataTypes.readFileTime(buffer);
    long endOfFile = buffer.readUInt64();
    long allocationSize = buffer.readUInt64();
    long fileAttributes = buffer.readUInt32();
    long fileNameLen = buffer.readUInt32();
    String fileName = buffer.readString(StandardCharsets.UTF_16LE, (int)fileNameLen / 2);
    FileDirectoryInformation fi = new FileDirectoryInformation(
        nextOffset, fileIndex, fileName, 
        creationTime, lastAccessTime, lastWriteTime, changeTime, 
        endOfFile, allocationSize, 
        fileAttributes);
    return fi;
  }
  
  public static FileFullDirectoryInformation parseFileFullDirectoryInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long nextOffset = buffer.readUInt32();
    long fileIndex = buffer.readUInt32();
    FileTime creationTime = MsDataTypes.readFileTime(buffer);
    FileTime lastAccessTime = MsDataTypes.readFileTime(buffer);
    FileTime lastWriteTime = MsDataTypes.readFileTime(buffer);
    FileTime changeTime = MsDataTypes.readFileTime(buffer);
    long endOfFile = buffer.readUInt64();
    long allocationSize = buffer.readUInt64();
    long fileAttributes = buffer.readUInt32();
    long fileNameLen = buffer.readUInt32();
    long eaSize = buffer.readUInt32();
    String fileName = buffer.readString(StandardCharsets.UTF_16LE, (int)fileNameLen / 2);
    FileFullDirectoryInformation fi = new FileFullDirectoryInformation(
        nextOffset, fileIndex, fileName, 
        creationTime, lastAccessTime, lastWriteTime, changeTime, 
        endOfFile, allocationSize, 
        fileAttributes, 
        eaSize);
    return fi;
  }
  
  public static FileIdBothDirectoryInformation parseFileIdBothDirectoryInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long nextOffset = buffer.readUInt32();
    long fileIndex = buffer.readUInt32();
    FileTime creationTime = MsDataTypes.readFileTime(buffer);
    FileTime lastAccessTime = MsDataTypes.readFileTime(buffer);
    FileTime lastWriteTime = MsDataTypes.readFileTime(buffer);
    FileTime changeTime = MsDataTypes.readFileTime(buffer);
    long endOfFile = buffer.readUInt64();
    long allocationSize = buffer.readUInt64();
    long fileAttributes = buffer.readUInt32();
    long fileNameLen = buffer.readUInt32();
    long eaSize = buffer.readUInt32();
    byte shortNameLen = buffer.readByte();
    buffer.readByte();
    byte[] shortNameBytes = buffer.readRawBytes(24);
    String shortName = new String(shortNameBytes, 0, shortNameLen, StandardCharsets.UTF_16LE);
    buffer.readUInt16();
    byte[] fileId = buffer.readRawBytes(8);
    String fileName = buffer.readString(StandardCharsets.UTF_16LE, (int)fileNameLen / 2);
    FileIdBothDirectoryInformation fi = new FileIdBothDirectoryInformation(
        nextOffset, fileIndex, fileName, 
        creationTime, lastAccessTime, lastWriteTime, changeTime, 
        endOfFile, allocationSize, 
        fileAttributes, 
        eaSize, 
        shortName, 
        fileId);
    return fi;
  }
  
  public static FileIdFullDirectoryInformation parseFileIdFullDirectoryInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long nextOffset = buffer.readUInt32();
    long fileIndex = buffer.readUInt32();
    FileTime creationTime = MsDataTypes.readFileTime(buffer);
    FileTime lastAccessTime = MsDataTypes.readFileTime(buffer);
    FileTime lastWriteTime = MsDataTypes.readFileTime(buffer);
    FileTime changeTime = MsDataTypes.readFileTime(buffer);
    long endOfFile = buffer.readUInt64();
    long allocationSize = buffer.readUInt64();
    long fileAttributes = buffer.readUInt32();
    long fileNameLen = buffer.readUInt32();
    long eaSize = buffer.readUInt32();
    buffer.skip(4);
    byte[] fileId = buffer.readRawBytes(8);
    String fileName = buffer.readString(StandardCharsets.UTF_16LE, (int)fileNameLen / 2);
    FileIdFullDirectoryInformation fi = new FileIdFullDirectoryInformation(
        nextOffset, fileIndex, fileName, 
        creationTime, lastAccessTime, lastWriteTime, changeTime, 
        endOfFile, allocationSize, 
        fileAttributes, 
        eaSize, 
        fileId);
    return fi;
  }
  
  public static FileNamesInformation parseFileNamesInformation(Buffer<?> buffer) throws Buffer.BufferException {
    long nextOffset = buffer.readUInt32();
    long fileIndex = buffer.readUInt32();
    long fileNameLen = buffer.readUInt32();
    String fileName = buffer.readString(StandardCharsets.UTF_16LE, (int)fileNameLen / 2);
    FileNamesInformation fi = new FileNamesInformation(
        nextOffset, fileIndex, fileName);
    return fi;
  }
  
  public static void writeFileRenameInformation(FileRenameInformation information, Buffer<?> buffer) {
    buffer.putByte((byte)(information.isReplaceIfExists() ? 1 : 0));
    buffer.putRawBytes(new byte[7]);
    buffer.putUInt64(information.getRootDirectory());
    buffer.putUInt32((information.getFileNameLength() * 2));
    buffer.putRawBytes(information.getFileName().getBytes(StandardCharsets.UTF_16LE));
  }
}
