package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.CircularDependencyException;
import com.github.alexgaard.hypo.exception.MissingDependencyProviderException;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Supplier;


/**
 * Represents an immutable set of registered dependencies.
 * This class should be instantiated by {@link Resolver } and not created directly.
 */
public class Dependencies {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Deque<DependencyId> initializationStack;

    private final Map<DependencyId, Object> cache;

    private final Map<DependencyId, Provider<?>> providers;

    Dependencies(Map<DependencyId, Provider<?>> providers) {
        initializationStack = new ArrayDeque<>();
        cache = new HashMap<>();

        this.providers = providers;
    }

    /**
     * Retrieves a singleton dependency from the cache.
     * If the dependency does not exist, then it will be created once.
     *
     * @param dependencyClass class of dependency
     * @param <T>   type of dependency
     * @return the requested dependency
     */
    public <T> T get(Class<T> dependencyClass) {
        return get(dependencyClass, null);
    }

    /**
     * Get all dependencies that is equal to, implements or extends the specified class.
     *
     * @param dependencyClass class of dependency
     * @param <T>   type of dependency
     * @return all matching dependencies
     */
    public <T> List<T> getAll(Class<T> dependencyClass) {
        Set<DependencyId> providerIds = providers
                .keySet();

        List<T> dependencies = new ArrayList<>();

        providerIds.forEach(id -> {
            if (dependencyClass.isAssignableFrom(id.clazz)) {
                dependencies.add((T) get(id.clazz, id.name));
            }
        });

        return dependencies;
    }

    /**
     * Retrieves a singleton dependency from the cache.
     * The name is used to distinguish dependencies of the same class from each other.
     * If the dependency does not exist, then it will be created once.
     *
     * @param dependencyClass class of dependency
     * @param name  name of the dependency
     * @param <T>   type of dependency
     * @return the requested dependency
     */
    public <T> T get(Class<T> dependencyClass, String name) {
        DependencyId dependencyId = DependencyId.of(dependencyClass, name);
        T dependency = (T) cache.get(dependencyId);

        if (dependency == null) {
            dependency = create(dependencyClass, name);
            cache.put(dependencyId, dependency);
        }

        return dependency;
    }

    /**
     * Retrieves a dependency from the cache.
     * @param dependencyClass class of dependency
     * @return The dependency or null if the dependency does not exist
     * @param <T> type of dependency
     */
    public <T> @Nullable T find(Class<T> dependencyClass) {
        return (T) cache.get(DependencyId.of(dependencyClass));
    }

    /**
     * Retrieves a dependency from the cache.
     * @param dependencyClass class of dependency
     * @param name  name of the dependency
     * @return The dependency or null if the dependency does not exist
     * @param <T> type of dependency
     */
    public <T> @Nullable T find(Class<T> dependencyClass, String name) {
        return (T) cache.get(DependencyId.of(dependencyClass, name));
    }

    /**
     * Create a new dependency of the requested class with a registered provider.
     *
     * @param dependencyClass class of dependency
     * @param <T>   type of dependency
     * @return the requested dependency
     */
    public <T> T create(Class<T> dependencyClass) {
        return create(dependencyClass, null);
    }

    /**
     * Create a new dependency of the requested class with a registered provider.
     * The name is used to distinguish dependencies of the same class from each other.
     *
     * @param dependencyClass class of dependency
     * @param name  name of the dependency
     * @param <T>   type of dependency
     * @return the requested dependency
     */
    public <T> T create(Class<T> dependencyClass, String name) {
        DependencyId dependencyId = DependencyId.of(dependencyClass, name);

        if (initializationStack.contains(dependencyId)) {
            List<DependencyId> dependencyCycle = new ArrayList<>(initializationStack);
            dependencyCycle.add(dependencyId);

            throw new CircularDependencyException(dependencyCycle);
        }

        log.debug("Creating dependency {}. Dependency stack {}.", dependencyId, initializationStack);

        initializationStack.add(dependencyId);

        T dependency = (T) getProvider(dependencyId).provide(this);

        initializationStack.pop();

        return dependency;
    }

    /**
     * Retrieves a singleton dependency from the cache lazily with a supplier.
     * If the dependency does not exist, then it will be created once.
     *
     * @param dependencyClass class of dependency
     * @param <T>   type of dependency
     * @return a supplier of the requested dependency
     */
    public <T> Supplier<T> lazyGet(Class<T> dependencyClass) {
        return () -> get(dependencyClass);
    }

    /**
     * Retrieves a singleton dependency from the cache lazily with a supplier.
     * The name is used to distinguish dependencies of the same class from each other.
     * If the dependency does not exist, then it will be created once.
     *
     * @param dependencyClass class of dependency
     * @param name  name of the dependency
     * @param <T>   type of dependency
     * @return a supplier of the requested dependency
     */
    public <T> Supplier<T> lazyGet(Class<T> dependencyClass, String name) {
        return () -> get(dependencyClass, name);
    }

    /**
     * Retrieves a new dependency from the cache lazily with a supplier.
     *
     * @param dependencyClass class of dependency
     * @param <T>   type of dependency
     * @return a supplier of the requested dependency
     */
    public <T> Supplier<T> lazyCreate(Class<T> dependencyClass) {
        return () -> create(dependencyClass);
    }

    /**
     * Retrieves a new dependency from the cache lazily with a supplier.
     * The name is used to distinguish dependencies of the same class from each other.
     *
     * @param dependencyClass class of dependency
     * @param name  name of the dependency
     * @param <T>   type of dependency
     * @return a supplier of the requested dependency
     */
    public <T> Supplier<T> lazyCreate(Class<T> dependencyClass, String name) {
        return () -> create(dependencyClass, name);
    }

    void initialize() {
        log.debug("Initializing dependencies with registered providers {}", providers);

        providers.forEach((dependencyClass, provider) -> {
            if (!cache.containsKey(dependencyClass)) {
                Object dependency = provider.provide(this);
                cache.put(dependencyClass, dependency);
            }
        });

        log.debug("Created dependencies after initialization {}", cache);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependencies that = (Dependencies) o;
        return cache.equals(that.cache) && providers.equals(that.providers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cache, providers);
    }

    private Provider<?> getProvider(DependencyId id) {
        Provider<?> provider = providers.get(id);

        if (provider == null) {
            throw new MissingDependencyProviderException(id, providers);
        }

        return provider;
    }

}
