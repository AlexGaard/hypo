package com.github.alexgaard.hypo;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.RetentionPolicy.SOURCE;

@Target({ TYPE })
@Retention(SOURCE)
public @interface Dependency {
    String name() default "";

}
