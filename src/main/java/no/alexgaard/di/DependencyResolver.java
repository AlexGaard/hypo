package no.alexgaard.di;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class DependencyResolver {

    private final Registry registry;
    private final AtomicBoolean isResolving;

    public DependencyResolver() {
        this.registry = new Registry();
        this.isResolving = new AtomicBoolean(false);
    }

    public <T> DependencyResolver register(Class<T> clazz, DependencyProvider<T> provider) {
        registry.registerProvider(clazz, provider);
        return this;
    }

    public <T> DependencyResolver register(Class<T> clazz, Supplier<T> provider) {
        registry.registerProvider(clazz, (ignored) -> provider.get());
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

        isResolving.set(false);

        return new Dependencies(registry.getDependencies());
    }

}
