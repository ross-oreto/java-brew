package io.oreto.brew.data.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Constraint
public @interface False {
    String name() default "";
    String property() default "";
    String group() default "";
    String message() default "";
}
