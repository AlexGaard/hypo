package com.github.alexgaard.hypo;

import java.util.*;
import java.util.function.Supplier;

public class Resolver {

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

    public Registry resolve() {
        Registry registry = new Registry(Map.copyOf(providers));

        registry.initialize();

        onPostInitListeners.forEach(
                (clazz, listener) -> listener.onPostInit(registry, registry.get(clazz))
        );

        return registry;
    }

    public interface OnPostInit<T> {

        void onPostInit(Registry registry, T dependency);

    }
}
