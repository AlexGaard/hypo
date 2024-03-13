package com.github.alexgaard.hypo.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to inject a dependency with a specified name.
 */
@Target({ PARAMETER })
@Retention(RUNTIME)
public @interface Named {
    String value() default "";

}
