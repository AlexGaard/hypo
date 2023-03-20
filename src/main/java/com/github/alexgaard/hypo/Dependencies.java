package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.CircularDependencyException;
import com.github.alexgaard.hypo.exception.MissingDependencyProviderException;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Dependencies {

    private final Deque<Class<?>> initializationStack;

    private final Map<Class<?>, Object> cache;

    private final Map<Class<?>, Provider<?>> providers;

    public Dependencies(Map<Class<?>, Provider<?>> providers) {
        initializationStack = new ArrayDeque<>();
        cache = new HashMap<>();

        this.providers = providers;
    }

    public <T> T get(Class<T> clazz) {
        T dependency = (T) cache.get(clazz);

        if (dependency == null) {
            return create(clazz);
        }

        return dependency;
    }

    public <T> T create(Class<T> clazz) {
        if (initializationStack.contains(clazz)) {
            List<Class<?>> dependencyCycle = new ArrayList<>(initializationStack);
            dependencyCycle.add(clazz);

            throw new CircularDependencyException(dependencyCycle);
        }

        initializationStack.add(clazz);

        T dependency = getProvider(clazz).provide(this);
        cache.put(clazz, dependency);

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
        providers.forEach((clazz, provider) -> {
            Object dependency = provider.provide(this);
            cache.put(clazz, dependency);
        });
    }

    private <T> Provider<T> getProvider(Class<T> clazz) {
        Provider<T> provider = (Provider<T>) providers.get(clazz);

        if (provider == null) {
            throw new MissingDependencyProviderException(clazz);
        }

        return provider;
    }

}
