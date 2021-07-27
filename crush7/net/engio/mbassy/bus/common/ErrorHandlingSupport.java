package net.engio.mbassy.bus.common;

import java.util.Collection;
import net.engio.mbassy.bus.error.IPublicationErrorHandler;

public interface ErrorHandlingSupport {
  Collection<IPublicationErrorHandler> getRegisteredErrorHandlers();
}
