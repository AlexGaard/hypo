package com.github.alexgaard.hypo.exception;

import com.github.alexgaard.hypo.Dependencies;
import com.github.alexgaard.hypo.DependencyId;
import com.github.alexgaard.hypo.Provider;

import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class MissingDependencyProviderException extends RuntimeException {

    public MissingDependencyProviderException(DependencyId missingProviderId, Map<DependencyId, Provider<?>> providers) {
        super(message(missingProviderId, providers));
    }

    private static String message(DependencyId dependencyId, Map<DependencyId, Provider<?>> providers) {
        String registeredProviders = providers.keySet().stream().map(DependencyId::id).collect(Collectors.joining(", "));
        String str = "Unable to find a registered dependency provider for %s.%nRegistered providers: %s";
        return format(str, dependencyId, registeredProviders);
    }

}
