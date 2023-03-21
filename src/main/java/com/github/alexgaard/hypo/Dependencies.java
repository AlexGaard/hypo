package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.CircularDependencyException;
import com.github.alexgaard.hypo.exception.MissingDependencyProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

import static com.github.alexgaard.hypo.util.DependencyId.id;

public class Dependencies {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Deque<String> initializationStack;

    private final Map<String, Object> cache;

    private final Map<String, Provider<?>> providers;

    public Dependencies(Map<String, Provider<?>> providers) {
        initializationStack = new ArrayDeque<>();
        cache = new HashMap<>();

        this.providers = providers;
    }

    public <T> T get(Class<T> clazz) {
        return get(clazz, null);
    }

    public <T> T get(Class<T> clazz, String name) {
        T dependency = (T) cache.get(id(clazz, name));

        if (dependency == null) {
            return create(clazz);
        }

        return dependency;
    }

    public <T> T create(Class<T> clazz) {
       return create(clazz, null);
    }

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
        cache.put(dependencyId, dependency);

        initializationStack.pop();

        return dependency;
    }

    public <T> Supplier<T> lazyGet(Class<T> clazz) {
        return () -> get(clazz);
    }

    public <T> Supplier<T> lazyCreate(Class<T> clazz) {
        return () -> create(clazz);
    }

    protected void initialize() {
        log.debug("Initializing dependencies with registered providers {}", providers);

        providers.forEach((clazz, provider) -> {
            Object dependency = provider.provide(this);
            cache.put(clazz, dependency);
        });

        log.debug("Created dependencies after initialization {}", cache);
    }

    private Provider<?> getProvider(String id) {
        Provider<?> provider = providers.get(id);

        if (provider == null) {
            throw new MissingDependencyProviderException(id);
        }

        return provider;
    }

}
