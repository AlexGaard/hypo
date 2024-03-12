package com.github.alexgaard.hypo.exception;

import com.github.alexgaard.hypo.annotation.InjectInto;

import static java.lang.String.format;

public class MultipleConstructorsException extends RuntimeException {
    public MultipleConstructorsException(Class<?> classWithMultipleConstructors) {
        super(exceptionString(classWithMultipleConstructors));
    }

    private static String exceptionString(Class<?> classWithMultipleConstructors) {
        return format(
                "Found multiple available constructors on %s. Annotate the constructor to use for injection with %s",
                classWithMultipleConstructors.getCanonicalName(),
                InjectInto.class.getSimpleName()
        );
    }
}
