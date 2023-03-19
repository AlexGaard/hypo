package no.alexgaard.yadi;

import java.util.HashMap;
import java.util.Map;

public class Registry {

    private final Map<Class, Object> dependencies;
    private final Map<Class, DependencyProvider> providers;

    private final Map<Class, Boolean> initializing;

    public Registry() {
        this.dependencies = new HashMap<>();
        this.providers = new HashMap<>();
        initializing = new HashMap<>();
    }

    protected void resolve() {
        providers.forEach((depClass, depProvider) -> {
            Object dep = depProvider.provide(this);
            dependencies.put(depClass, dep);
        });
    }

    protected Map<Class, Object> getDependencies() {
        return dependencies;
    }

    protected Map<Class, DependencyProvider> getProviders() {
        return providers;
    }

    protected <T> void registerProvider(Class<T> clazz, DependencyProvider<T> provider) {
        // TODO: Warn if already there
        providers.put(clazz, provider);
    }

    protected <T> void addDependency(Class<T> clazz, T dependency) {
        // TODO: Warn if already there
        dependencies.put(clazz, dependency);
    }

    private <T> DependencyProvider<T> getProvider(Class<T> clazz) {
        final var provider = providers.get(clazz);

        if (provider == null) {
            throw new IllegalStateException(
                    "Unable to find provider for dependency %s. Has this dependency been registered?"
                            .formatted(clazz.getCanonicalName())
            );
        }

        return provider;
    }

    public <T> T get(Class<T> clazz) {
        var dependency = dependencies.get(clazz);

        if (dependency == null) {
            if (initializing.getOrDefault(clazz, false)) {
                throw new IllegalStateException(
                        "Already initializing %s this is a circular dependency".formatted(clazz.getCanonicalName())
                );
            }

            initializing.put(clazz, true);
            dependency = getProvider(clazz).provide(this);
            dependencies.put(clazz, dependency);
            initializing.put(clazz, false); // Not necessary
        }

        return (T) dependency;
    }

}
