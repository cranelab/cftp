package net.engio.mbassy.bus.publication;

import net.engio.mbassy.bus.IMessagePublication;

public interface IPublicationCommand {
  IMessagePublication now();
}
