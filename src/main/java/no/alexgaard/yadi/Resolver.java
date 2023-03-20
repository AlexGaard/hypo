package no.alexgaard.yadi;

import java.util.*;
import java.util.function.Supplier;

public class Resolver {

    private final Map<Class<?>, Provider<?>> providers;
    private final Map<Class<?>, OnResolved> onResolvedListeners;

    public Resolver() {
        this.providers = new HashMap<>();
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
        providers.put(clazz, provider);

        if (onResolvedListener != null) {
            onResolvedListeners.put(clazz, onResolvedListener);
        }

        return this;
    }

    public Registry resolve() {
        Registry registry = new Registry();
        providers.forEach((clazz, provider) -> registry.registerProvider((Class) clazz, (Provider) provider));

        registry.getProviders().forEach((dependencyClass, provider) -> {
            Object dependency = provider.provide(registry);
            registry.addDependency((Class) dependencyClass, dependency);
        });

        onResolvedListeners.forEach(
                (clazz, listener) -> listener.onResolved(registry, registry.getDependencies().get(clazz))
        );

        return registry;
    }

    public interface OnResolved<T> {

        void onResolved(Registry registry, T dependency);

    }
}
