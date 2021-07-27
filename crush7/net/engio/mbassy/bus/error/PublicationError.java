package net.engio.mbassy.bus.error;

import java.lang.reflect.Method;
import net.engio.mbassy.bus.IMessagePublication;
import net.engio.mbassy.subscription.SubscriptionContext;

public class PublicationError {
  private Throwable cause;
  
  private String errorMsg;
  
  private Method handler;
  
  private Object listener;
  
  private IMessagePublication publication;
  
  private Object message;
  
  public PublicationError(Throwable cause, String errorMsg, Method handler, Object listener, IMessagePublication publication) {
    this.cause = cause;
    this.errorMsg = errorMsg;
    this.handler = handler;
    this.listener = listener;
    this.publication = publication;
    this.message = (publication != null) ? publication.getMessage() : null;
  }
  
  public PublicationError(Throwable cause, String errorMsg, IMessagePublication publication) {
    this.cause = cause;
    this.errorMsg = errorMsg;
  }
  
  public PublicationError(Throwable cause, String errorMsg, SubscriptionContext context) {
    this.cause = cause;
    this.errorMsg = errorMsg;
    this.handler = context.getHandler().getMethod();
  }
  
  public PublicationError(Throwable cause, String errorMsg) {
    this.cause = cause;
    this.errorMsg = errorMsg;
  }
  
  public PublicationError() {}
  
  public Throwable getCause() {
    return this.cause;
  }
  
  public PublicationError setCause(Throwable cause) {
    this.cause = cause;
    return this;
  }
  
  public String getMessage() {
    return this.errorMsg;
  }
  
  public PublicationError setMessage(String message) {
    this.errorMsg = message;
    return this;
  }
  
  public PublicationError setPublishedMessage(Object message) {
    this.message = message;
    return this;
  }
  
  public Method getHandler() {
    return this.handler;
  }
  
  public PublicationError setHandler(Method handler) {
    this.handler = handler;
    return this;
  }
  
  public Object getListener() {
    return this.listener;
  }
  
  public PublicationError setListener(Object listener) {
    this.listener = listener;
    return this;
  }
  
  public Object getPublishedMessage() {
    return this.message;
  }
  
  public PublicationError setPublication(IMessagePublication publication) {
    this.publication = publication;
    return this;
  }
  
  public String toString() {
    String newLine = System.getProperty("line.separator");
    return "PublicationError{" + newLine + "\tcause=" + this.cause + newLine + "\tmessage='" + this.errorMsg + '\'' + newLine + "\thandler=" + this.handler + newLine + "\tlistener=" + this.listener + newLine + "\tpublishedMessage=" + 








      
      getPublishedMessage() + '}';
  }
}
