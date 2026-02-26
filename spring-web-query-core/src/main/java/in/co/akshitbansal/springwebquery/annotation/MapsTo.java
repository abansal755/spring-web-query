package in.co.akshitbansal.springwebquery.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapsTo {

    String field();
    boolean absolute() default false;
}
