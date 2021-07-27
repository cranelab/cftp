package com.sun.mail.imap;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.event.MessageCountEvent;

public class MessageVanishedEvent extends MessageCountEvent {
  private long[] uids;
  
  private static final Message[] noMessages = new Message[0];
  
  private static final long serialVersionUID = 2142028010250024922L;
  
  public MessageVanishedEvent(Folder folder, long[] uids) {
    super(folder, 2, true, noMessages);
    this.uids = uids;
  }
  
  public long[] getUIDs() {
    return this.uids;
  }
}
