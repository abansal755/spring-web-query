package io.github.abansal755.springwebquery.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface FieldMapping {

    String name();
    String field();
    boolean allowOriginalFieldName() default false;
}
