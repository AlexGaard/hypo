package com.github.alexgaard.hypo.exception;


import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class NoMatchingConstructorException extends RuntimeException {

    public NoMatchingConstructorException(Class<?> classWithBadConstructor, Predicate<Class<?>> isParamMatching) {
        super(exceptionString(classWithBadConstructor, isParamMatching));
    }

    private static String exceptionString(Class<?> classWithBadConstructor, Predicate<Class<?>> isParamMatching) {
        Constructor<?>[] constructors = classWithBadConstructor.getConstructors();

        if (constructors.length == 0) {
            return format("%s has no public constructors that can be used for injection", classWithBadConstructor.getCanonicalName());
        }

        String constructorsStr = Arrays.stream(constructors)
                .map(c -> {
                    String missingParams = Arrays.stream(c.getParameterTypes())
                            .filter(isParamMatching.negate())
                            .map(Class::getCanonicalName)
                            .collect(Collectors.joining(", "));

                    return "\t* " + c + " is missing the parameters " + missingParams;
                })
                .collect(Collectors.joining("\n"));

        return format(
                "Unable to find a constructor in %s with parameters that matches registered dependencies.\n" +
                        "Make sure that all the parameters for at least one of the constructors listed below have been registered.\n" +
                        "Available constructors:\n%s",
                classWithBadConstructor.getCanonicalName(),
                constructorsStr
        );
    }

}