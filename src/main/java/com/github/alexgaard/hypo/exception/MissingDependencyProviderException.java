package com.github.alexgaard.hypo.exception;

import static java.lang.String.format;

public class MissingDependencyProviderException extends RuntimeException {

    public MissingDependencyProviderException(Class<?> missingClass) {
        super(message(missingClass));
    }

    private static String message(Class<?> missingClass) {
        String str = "Unable to find a dependency provider for %s. Has a provider for this dependency been registered?";
        return format(str, missingClass.getCanonicalName());
    }

}
