package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.CircularDependencyException;
import com.github.alexgaard.hypo.exception.MissingDependencyProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

import static com.github.alexgaard.hypo.DependencyId.id;


/**
 * Represents an immutable set of registered dependencies.
 * This class should be instantiated by {@link Resolver } and not created directly.
 */
public class Dependencies {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Deque<String> initializationStack;

    private final Map<String, Object> cache;

    private final Map<String, Provider<?>> providers;

    Dependencies(Map<String, Provider<?>> providers) {
        initializationStack = new ArrayDeque<>();
        cache = new HashMap<>();

        this.providers = providers;
    }

    /**
     * Retrieves a singleton dependency from the cache.
     * If the dependency does not exist, then it will be created once.
     * @param clazz class of dependency
     * @return the requested dependency
     * @param <T> type of dependency
     */
    public <T> T get(Class<T> clazz) {
        return get(clazz, null);
    }

    /**
     * Retrieves a singleton dependency from the cache.
     * The name is used to distinguish dependencies of the same class from each other.
     * If the dependency does not exist, then it will be created once.
     * @param clazz class of dependency
     * @param name name of the dependency
     * @return the requested dependency
     * @param <T> type of dependency
     */
    public <T> T get(Class<T> clazz, String name) {
        String dependencyId = id(clazz, name);
        T dependency = (T) cache.get(dependencyId);

        if (dependency == null) {
            dependency = create(clazz, name);
            cache.put(dependencyId, dependency);
        }

        return dependency;
    }

    /**
     * Create a new dependency of the requested class with a registered provider.
     * @param clazz class of dependency
     * @return the requested dependency
     * @param <T> type of dependency
     */
    public <T> T create(Class<T> clazz) {
        return create(clazz, null);
    }

    /**
     * Create a new dependency of the requested class with a registered provider.
     * The name is used to distinguish dependencies of the same class from each other.
     * @param clazz class of dependency
     * @param name name of the dependency
     * @return the requested dependency
     * @param <T> type of dependency
     */
    public <T> T create(Class<T> clazz, String name) {
        String dependencyId = id(clazz, name);

        if (initializationStack.contains(dependencyId)) {
            List<String> dependencyCycle = new ArrayList<>(initializationStack);
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
     * @param clazz class of dependency
     * @return a supplier of the requested dependency
     * @param <T> type of dependency
     */
    public <T> Supplier<T> lazyGet(Class<T> clazz) {
        return () -> get(clazz);
    }

    /**
     * Retrieves a singleton dependency from the cache lazily with a supplier.
     * The name is used to distinguish dependencies of the same class from each other.
     * If the dependency does not exist, then it will be created once.
     * @param clazz class of dependency
     * @param name name of the dependency
     * @return a supplier of the requested dependency
     * @param <T> type of dependency
     */
    public <T> Supplier<T> lazyGet(Class<T> clazz, String name) {
        return () -> get(clazz, name);
    }

    /**
     * Retrieves a new dependency from the cache lazily with a supplier.
     * @param clazz class of dependency
     * @return a supplier of the requested dependency
     * @param <T> type of dependency
     */
    public <T> Supplier<T> lazyCreate(Class<T> clazz) {
        return () -> create(clazz);
    }

    /**
     * Retrieves a new dependency from the cache lazily with a supplier.
     * The name is used to distinguish dependencies of the same class from each other.
     * @param clazz class of dependency
     * @param name name of the dependency
     * @return a supplier of the requested dependency
     * @param <T> type of dependency
     */
    public <T> Supplier<T> lazyCreate(Class<T> clazz, String name) {
        return () -> create(clazz, name);
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

    void initialize() {
        log.debug("Initializing dependencies with registered providers {}", providers);

        providers.forEach((clazz, provider) -> {
            if (!cache.containsKey(clazz)) {
                Object dependency = provider.provide(this);
                cache.put(clazz, dependency);
            }
        });

        log.debug("Created dependencies after initialization {}", cache);
    }

    private Provider<?> getProvider(String id) {
        Provider<?> provider = providers.get(id);

        if (provider == null) {
            throw new MissingDependencyProviderException(id, providers);
        }

        return provider;
    }

}
