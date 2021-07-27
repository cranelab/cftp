package javax.mail.event;

import java.util.EventListener;

public interface FolderListener extends EventListener {
  void folderCreated(FolderEvent paramFolderEvent);
  
  void folderDeleted(FolderEvent paramFolderEvent);
  
  void folderRenamed(FolderEvent paramFolderEvent);
}
