package com.github.alexgaard.hypo.exception;

import java.lang.reflect.Constructor;

import static java.lang.String.format;

public class MultipleMatchingConstructorException extends RuntimeException {

    public MultipleMatchingConstructorException(Class<?> classWithMultipleConstructors, Constructor<?> constructor1, Constructor<?> constructor2) {
        super(exceptionString(classWithMultipleConstructors, constructor1, constructor2));
    }

    private static String exceptionString(Class<?> classWithMultipleConstructors, Constructor<?> constructor1, Constructor<?> constructor2) {

        return format(
                "Found multiple constructors on %s with parameters that matches registered dependencies and same parameter count." +
                        "%n\t\tConstructor 1: %s%n\t\tConstructor 2: %s",
                classWithMultipleConstructors.getCanonicalName(),
                constructor1.toGenericString(),
                constructor2.toGenericString()
        );
    }

}
