package javax.mail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MailSessionDefinition {
  String description() default "";
  
  String name();
  
  String storeProtocol() default "";
  
  String transportProtocol() default "";
  
  String host() default "";
  
  String user() default "";
  
  String password() default "";
  
  String from() default "";
  
  String[] properties() default {};
}
