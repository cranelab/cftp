package org.apache.http.cookie;

import org.apache.http.annotation.Obsolete;

public interface SetCookie2 extends SetCookie {
  @Obsolete
  void setCommentURL(String paramString);
  
  @Obsolete
  void setPorts(int[] paramArrayOfint);
  
  @Obsolete
  void setDiscard(boolean paramBoolean);
}
