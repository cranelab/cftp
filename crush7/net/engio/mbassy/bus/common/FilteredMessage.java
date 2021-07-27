package net.engio.mbassy.bus.common;

public final class FilteredMessage extends PublicationEvent {
  public FilteredMessage(Object event) {
    super(event);
  }
}
