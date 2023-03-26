package com.github.alexgaard.hypo.exception;

import com.github.alexgaard.hypo.Provider;

import java.util.Map;

import static java.lang.String.format;

public class MissingDependencyProviderException extends RuntimeException {

    public MissingDependencyProviderException(String missingProviderId, Map<String, Provider<?>> providers) {
        super(message(missingProviderId, providers));
    }

    private static String message(String dependencyId, Map<String, Provider<?>> providers) {
        String registeredProviders = String.join(", ", providers.keySet());
        String str = "Unable to find a registered dependency provider for %s\nRegistered providers: %s";
        return format(str, dependencyId, registeredProviders);
    }

}
