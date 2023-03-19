package no.alexgaard.yadi;

import java.util.*;
import java.util.function.Supplier;

public class Resolver {

    private final Registry registry;
    private final Map<Class<?>, OnResolved> onResolvedListeners;

    public Resolver() {
        this.registry = new Registry();
        this.onResolvedListeners = new HashMap<>();
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
            OnResolved<T> onResolvedListener
    ) {
        registry.registerProvider(clazz, provider);

        if (onResolvedListener != null) {
            onResolvedListeners.put(clazz, onResolvedListener);
        }

        return this;
    }

    public synchronized Dependencies resolve() {
        registry.getProviders().forEach((dependencyClass, provider) -> {
            Object dependency = provider.provide(registry);
            registry.addDependency((Class<? super Object>) dependencyClass, dependency);
        });

        onResolvedListeners.forEach(
                (clazz, listener) -> listener.onResolved(registry, registry.getDependencies().get(clazz))
        );

        return new Dependencies(registry.getDependencies());
    }

    public interface OnResolved<T> {

        void onResolved(Registry registry, T dependency);

    }
}
