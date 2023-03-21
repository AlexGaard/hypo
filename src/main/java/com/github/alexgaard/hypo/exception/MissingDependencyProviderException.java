package com.github.alexgaard.hypo.exception;

import static java.lang.String.format;

public class MissingDependencyProviderException extends RuntimeException {

    public MissingDependencyProviderException(String dependencyId) {
        super(message(dependencyId));
    }

    private static String message(String dependencyId) {
        String str = "Unable to find a dependency provider for %s. Has a provider for this dependency been registered?";
        return format(str, dependencyId);
    }

}
