package net.engio.mbassy.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.engio.mbassy.dispatch.HandlerInvocation;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface Handler {
  Filter[] filters() default {};
  
  String condition() default "";
  
  Invoke delivery() default Invoke.Synchronously;
  
  int priority() default 0;
  
  boolean rejectSubtypes() default false;
  
  boolean enabled() default true;
  
  Class<? extends HandlerInvocation> invocation() default net.engio.mbassy.dispatch.ReflectiveHandlerInvocation.class;
}
