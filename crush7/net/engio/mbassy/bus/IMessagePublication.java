package net.engio.mbassy.bus;

import net.engio.mbassy.bus.error.PublicationError;

public interface IMessagePublication {
  void execute();
  
  boolean isFinished();
  
  boolean isRunning();
  
  boolean isScheduled();
  
  boolean hasError();
  
  PublicationError getError();
  
  boolean isDeadMessage();
  
  boolean isFilteredMessage();
  
  Object getMessage();
}
