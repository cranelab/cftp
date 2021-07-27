package net.engio.mbassy.subscription;

import net.engio.mbassy.bus.BusRuntime;
import net.engio.mbassy.listener.MetadataReader;

public class SubscriptionManagerProvider implements ISubscriptionManagerProvider {
  public SubscriptionManager createManager(MetadataReader reader, SubscriptionFactory factory, BusRuntime runtime) {
    return new SubscriptionManager(reader, factory, runtime);
  }
}
