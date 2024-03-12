package com.github.alexgaard.hypo.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Used to mark dependencies to register with {@link com.github.alexgaard.hypo.Resolver#registerAnnotatedDependencies()}
 */
@Target({ TYPE })
@Retention(SOURCE)
public @interface Dependency {
    String name() default "";

}
