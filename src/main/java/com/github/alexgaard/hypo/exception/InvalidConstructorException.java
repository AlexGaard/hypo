package com.github.alexgaard.hypo.exception;

import java.lang.reflect.Constructor;

import static java.lang.String.format;

public class InvalidConstructorException extends RuntimeException {

    public InvalidConstructorException(Constructor<?> constructor, Class<?> missingParameter) {
        super(exceptionString(constructor, missingParameter));
    }

    private static String exceptionString(Constructor<?> constructor, Class<?> missingParameter) {
        return format(
                "The parameter %s in constructor %s has not been registered as an dependency",
                missingParameter.getCanonicalName(),
                constructor.getDeclaringClass().getCanonicalName()
        );
    }

}
