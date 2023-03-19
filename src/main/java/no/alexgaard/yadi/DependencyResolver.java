package no.alexgaard.yadi;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class DependencyResolver {

    private final Registry registry;
    private final AtomicBoolean isResolving;

    private final Map<Class, OnResolved> onResolvedListeners;

    public DependencyResolver() {
        this.registry = new Registry();
        this.isResolving = new AtomicBoolean(false);
        this.onResolvedListeners = new HashMap<>();
    }

    public <T> DependencyResolver register(Class<T> clazz, DependencyProvider<T> provider) {
       return register(clazz, provider);
    }

    public <T> DependencyResolver register(Class<T> clazz, Supplier<T> provider) {
        return register(clazz, (ignored) -> provider.get());
    }

    public <T> DependencyResolver register(
            Class<T> clazz,
            DependencyProvider<T> provider,
            OnResolved<T> onResolvedListener
    ) {
        registry.registerProvider(clazz, provider);

        if (onResolvedListener != null) {
            onResolvedListeners.put(clazz, onResolvedListener);
        }

        return this;
    }

    public Dependencies resolve() {
        if (isResolving.get()) {
            throw new IllegalStateException("Already resolving dependencies");
        }

        isResolving.set(true);

        registry.getProviders().forEach((depClass, depProvider) -> {
            Object dep = depProvider.provide(registry);
            registry.addDependency(depClass, dep);
        });

        onResolvedListeners.forEach(
                (clazz, listener) -> listener.onResolved(registry, registry.getDependencies().get(clazz))
        );

        isResolving.set(false);

        return new Dependencies(registry.getDependencies());
    }

}
