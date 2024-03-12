package com.github.alexgaard.hypo.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to mark which constructor that should be injected into if there are multiple available constructors.
 */
@Target({ CONSTRUCTOR })
@Retention(RUNTIME)
public @interface InjectInto {}
