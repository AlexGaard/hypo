package com.github.alexgaard.hypo.exception;


import java.lang.reflect.Constructor;

import static java.lang.String.format;

public class ConstructorInjectionFailedException extends RuntimeException {

    public ConstructorInjectionFailedException(Class<?> classWithBadConstructor, Constructor<?> constructor, Throwable throwable) {
        super(format("Failed to inject into constructor %s from class %s", constructor.toGenericString(), classWithBadConstructor.getCanonicalName()), throwable);
    }

}