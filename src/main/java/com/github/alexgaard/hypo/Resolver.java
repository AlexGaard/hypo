package com.github.alexgaard.hypo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

public class Resolver {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Class<?>, Provider<?>> providers;

    private final Map<Class<?>, OnPostInit> onPostInitListeners;

    public Resolver() {
        this.providers = new HashMap<>();
        this.onPostInitListeners = new HashMap<>();
    }

    public <T> Resolver register(Class<T> clazz, Provider<T> provider) {
       return register(clazz, provider, null);
    }

    public <T> Resolver register(Class<T> clazz, Supplier<T> provider) {
        return register(clazz, (ignored) -> provider.get());
    }

    public <T> Resolver register(
            Class<T> clazz,
            Provider<T> provider,
            OnPostInit<T> onPostInit
    ) {
        providers.put(clazz, provider);

        if (onPostInit != null) {
            onPostInitListeners.put(clazz, onPostInit);
        }

        return this;
    }

    public Dependencies resolve() {
        Dependencies dependencies = new Dependencies(Map.copyOf(providers));

        dependencies.initialize();

        log.debug("Finished initialization of dependencies");

        onPostInitListeners.forEach(
                (clazz, listener) -> listener.onPostInit(dependencies, dependencies.get(clazz))
        );

        return dependencies;
    }

    public interface OnPostInit<T> {

        void onPostInit(Dependencies dependencies, T dependency);

    }
}
