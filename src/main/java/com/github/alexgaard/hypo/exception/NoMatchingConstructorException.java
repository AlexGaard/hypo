package com.github.alexgaard.hypo.exception;


import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class NoMatchingConstructorException extends RuntimeException {

    public NoMatchingConstructorException(Class<?> classWithBadConstructor) {
        super(exceptionString(classWithBadConstructor));
    }

    private static String exceptionString(Class<?> classWithBadConstructor) {
        String constructorsStr = Arrays.stream(classWithBadConstructor.getConstructors())
                .map(Constructor::toString)
                .collect(Collectors.joining("\n", "\t\t", ""));

        return format(
                "Unable to find a constructor on %s with parameters that matches registered dependencies. \n Found following constructors:\n%s",
                classWithBadConstructor.getCanonicalName(),
                constructorsStr
        );
    }

}