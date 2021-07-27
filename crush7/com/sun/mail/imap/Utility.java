package com.sun.mail.imap;

import com.sun.mail.imap.protocol.MessageSet;
import com.sun.mail.imap.protocol.UIDSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.mail.Message;

public final class Utility {
  public static MessageSet[] toMessageSet(Message[] msgs, Condition cond) {
    List<MessageSet> v = new ArrayList<MessageSet>(1);
    for (int i = 0; i < msgs.length; i++) {
      IMAPMessage msg = (IMAPMessage)msgs[i];
      if (!msg.isExpunged()) {
        int current = msg.getSequenceNumber();
        if (cond == null || cond.test(msg)) {
          MessageSet set = new MessageSet();
          set.start = current;
          for (; ++i < msgs.length; i++) {
            msg = (IMAPMessage)msgs[i];
            if (!msg.isExpunged()) {
              int next = msg.getSequenceNumber();
              if (cond == null || cond.test(msg))
                if (next == current + 1) {
                  current = next;
                } else {
                  i--;
                  break;
                }  
            } 
          } 
          set.end = current;
          v.add(set);
        } 
      } 
    } 
    if (v.isEmpty())
      return null; 
    return v.<MessageSet>toArray(new MessageSet[v.size()]);
  }
  
  public static MessageSet[] toMessageSetSorted(Message[] msgs, Condition cond) {
    msgs = (Message[])msgs.clone();
    Arrays.sort(msgs, new Comparator<Message>() {
          public int compare(Message msg1, Message msg2) {
            return msg1.getMessageNumber() - msg2.getMessageNumber();
          }
        });
    return toMessageSet(msgs, cond);
  }
  
  public static interface Condition {
    boolean test(IMAPMessage param1IMAPMessage);
  }
  
  public static UIDSet[] toUIDSet(Message[] msgs) {
    List<UIDSet> v = new ArrayList<UIDSet>(1);
    for (int i = 0; i < msgs.length; i++) {
      IMAPMessage msg = (IMAPMessage)msgs[i];
      if (!msg.isExpunged()) {
        long current = msg.getUID();
        UIDSet set = new UIDSet();
        set.start = current;
        for (; ++i < msgs.length; i++) {
          msg = (IMAPMessage)msgs[i];
          if (!msg.isExpunged()) {
            long next = msg.getUID();
            if (next == current + 1L) {
              current = next;
            } else {
              i--;
              break;
            } 
          } 
        } 
        set.end = current;
        v.add(set);
      } 
    } 
    if (v.isEmpty())
      return null; 
    return v.<UIDSet>toArray(new UIDSet[v.size()]);
  }
  
  public static UIDSet[] getResyncUIDSet(ResyncData rd) {
    return rd.getUIDSet();
  }
}
