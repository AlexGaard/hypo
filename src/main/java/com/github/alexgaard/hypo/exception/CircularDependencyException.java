package com.github.alexgaard.hypo.exception;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class CircularDependencyException extends RuntimeException {

    public CircularDependencyException(Collection<Class<?>> dependencyCycle) {
        super(cyclicDependencyMessage(dependencyCycle));
    }

    private static String cyclicDependencyMessage(Collection<Class<?>> dependencyCycle) {
        return format(
                "Circular dependency detected while initializing %s.\nDependency chain: %s",
                getLast(dependencyCycle).getCanonicalName(), dependencyChainStr(dependencyCycle)
        );
    }

    private static <T> T getLast(Collection<T> collection) {
        if (collection.isEmpty()) {
            throw new IllegalArgumentException("Cannot get last element from an empty collection");
        }

        return new ArrayList<>(collection).get(collection.size() - 1);
    }

    private static String dependencyChainStr(Collection<Class<?>> dependencyCycle) {
        return dependencyCycle
                .stream()
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(" -> "));
    }

}