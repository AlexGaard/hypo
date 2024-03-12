package com.github.alexgaard.hypo.exception;


import static java.lang.String.format;

public class NoPublicConstructorException extends RuntimeException {

    public NoPublicConstructorException(Class<?> classWithNoPublicConstructor) {
        super(exceptionString(classWithNoPublicConstructor));
    }

    private static String exceptionString(Class<?> classWithNoPublicConstructor) {
        return format("The class %s has no public constructors available", classWithNoPublicConstructor.getCanonicalName());
    }

}