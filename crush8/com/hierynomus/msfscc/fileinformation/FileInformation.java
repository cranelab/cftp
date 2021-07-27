package com.hierynomus.msfscc.fileinformation;

import com.hierynomus.msfscc.FileInformationClass;
import com.hierynomus.protocol.commons.buffer.Buffer;

public interface FileInformation {
  public static interface Codec<F extends FileInformation> extends Encoder<F>, Decoder<F> {}
  
  public static interface Decoder<F extends FileInformation> {
    FileInformationClass getInformationClass();
    
    F read(Buffer param1Buffer) throws Buffer.BufferException;
  }
  
  public static interface Encoder<F extends FileInformation> {
    FileInformationClass getInformationClass();
    
    void write(F param1F, Buffer param1Buffer);
  }
}
