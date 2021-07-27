package com.hierynomus.smbj.event;

import net.engio.mbassy.bus.SyncMessageBus;
import net.engio.mbassy.bus.common.PubSubSupport;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;
import net.engio.mbassy.bus.error.PublicationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SMBEventBus {
  private static final Logger log = LoggerFactory.getLogger(SMBEventBus.class);
  
  private PubSubSupport<SMBEvent> wrappedBus;
  
  public SMBEventBus() {
    this(new SyncMessageBus<SMBEvent>(new IPublicationErrorHandler() {
            public void handleError(PublicationError error) {
              if (error.getCause() != null) {
                SMBEventBus.log.error(error.toString(), error.getCause());
              } else {
                SMBEventBus.log.error(error.toString());
              } 
            }
          }));
  }
  
  public SMBEventBus(PubSubSupport<SMBEvent> wrappedBus) {
    this.wrappedBus = wrappedBus;
  }
  
  public void subscribe(Object listener) {
    this.wrappedBus.subscribe(listener);
  }
  
  public boolean unsubscribe(Object listener) {
    return this.wrappedBus.unsubscribe(listener);
  }
  
  public void publish(SMBEvent message) {
    this.wrappedBus.publish(message);
  }
}
