package pl.chrono.usermanager.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogActivity {
  String action() default "UNKNOWN_ACTION";
  String details() default "UNKNOWN_DETAILS";
}
