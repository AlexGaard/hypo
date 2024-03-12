package com.github.alexgaard.hypo.exception;

import com.github.alexgaard.hypo.annotation.InjectInto;

import static java.lang.String.format;

public class MultipleAnnotatedConstructorsException extends RuntimeException {
    public MultipleAnnotatedConstructorsException(Class<?> classWithMultipleConstructors) {
        super(exceptionString(classWithMultipleConstructors));
    }

    private static String exceptionString(Class<?> classWithMultipleConstructors) {
        return format(
                "Found multiple constructors on %s annotated with %s",
                classWithMultipleConstructors.getCanonicalName(),
                InjectInto.class.getSimpleName()
        );
    }

}
