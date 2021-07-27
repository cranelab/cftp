package net.engio.mbassy.bus.common;

public final class DeadMessage extends PublicationEvent {
  public DeadMessage(Object message) {
    super(message);
  }
}
