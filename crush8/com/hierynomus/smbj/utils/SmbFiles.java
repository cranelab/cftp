package com.hierynomus.smbj.utils;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.io.InputStreamByteChunkProvider;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

public class SmbFiles {
  public static int copy(File source, DiskShare share, String destPath, boolean overwrite) throws IOException {
    int r = 0;
    if (source != null && source.exists() && source.canRead() && source.isFile())
      try (InputStream is = new FileInputStream(source)) {
        if (destPath != null && is != null)
          try (File f = share.openFile(destPath, 
                
                EnumSet.of(AccessMask.GENERIC_WRITE), 
                EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL), 
                EnumSet.of(SMB2ShareAccess.FILE_SHARE_WRITE), overwrite ? SMB2CreateDisposition.FILE_OVERWRITE_IF : SMB2CreateDisposition.FILE_CREATE, 
                
                EnumSet.noneOf(SMB2CreateOptions.class))) {
            r = f.write(new InputStreamByteChunkProvider(is));
          }  
      }  
    return r;
  }
}
