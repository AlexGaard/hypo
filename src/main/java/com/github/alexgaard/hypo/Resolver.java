package com.github.alexgaard.hypo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.github.alexgaard.hypo.util.DependencyId.id;

/**
 * Resolves a set of registered dependency providers into an immutable instance of {@link Dependencies}
 */
public class Resolver {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, Provider<?>> providers;

    private final Map<DependencyId, OnPostInit> onPostInitListeners;

    public Resolver() {
        this.providers = new HashMap<>();
        this.onPostInitListeners = new HashMap<>();
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param clazz class of dependency
     * @param provider provider for the dependency
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> clazz, Provider<T> provider) {
        return register(clazz, null, provider, null);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param clazz class of dependency
     * @param provider provider for the dependency
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> clazz, Supplier<T> provider) {
        return register(clazz, ignored -> provider.get());
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param clazz class of dependency
     * @param provider provider for the dependency
     * @param onPostInit callback which is triggered after the initialization of dependencies
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> clazz, Provider<T> provider, OnPostInit<T> onPostInit) {
        return register(clazz, null, provider, onPostInit);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param clazz class of dependency
     * @param name name of the dependency
     * @param provider provider for the dependency
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> clazz, String name, Provider<T> provider) {
        return register(clazz, name, provider, null);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param clazz class of dependency
     * @param name name of the dependency
     * @param provider provider for the dependency
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> clazz, String name, Supplier<T> provider) {
        return register(clazz, name, ignored -> provider.get(), null);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param clazz class of dependency
     * @param name name of the dependency
     * @param provider provider for the dependency
     * @param onPostInit callback which is triggered after the initialization of dependencies
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> clazz, String name, Supplier<T> provider, OnPostInit<T> onPostInit) {
        return register(clazz, name, ignored -> provider.get(), onPostInit);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param clazz class of dependency
     * @param name name of the dependency
     * @param provider provider for the dependency
     * @param onPostInit callback which is triggered after the initialization of dependencies
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> clazz, String name, Provider<T> provider, OnPostInit<T> onPostInit) {
        String id = id(clazz, name);

        if (providers.containsKey(id)) {
            log.warn("Overwriting the previously registered provider for {}", id);
        }

        providers.put(id, provider);

        if (onPostInit != null) {
            DependencyId dependencyId = new DependencyId(clazz, name);
            onPostInitListeners.put(dependencyId, onPostInit);
        }

        return this;
    }

    /**
     * Uses the registered providers to resolve a new set of dependencies.
     * The dependencies are resolved immediately and will throw a {@link com.github.alexgaard.hypo.exception.CircularDependencyException}
     * if a circular dependency is present in the registered providers.
     * This method can be called multiple times, and will return a new set of dependencies each time.
     * @return a new set of dependencies
     */
    public Dependencies resolve() {
        Dependencies dependencies = new Dependencies(Map.copyOf(providers));

        dependencies.initialize();

        log.debug("Finished initialization of dependencies");

        onPostInitListeners.forEach(
                (id, listener) -> listener.onPostInit(dependencies, dependencies.get(id.clazz, id.name))
        );

        return dependencies;
    }

    public interface OnPostInit<T> {

        void onPostInit(Dependencies dependencies, T dependency);

    }

    public static class DependencyId {

        public final Class<?> clazz;

        public final String name;

        public DependencyId(Class<?> clazz, String name) {
            this.clazz = clazz;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DependencyId that = (DependencyId) o;
            return clazz.equals(that.clazz) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, name);
        }

    }

}
