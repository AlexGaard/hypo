package no.alexgaard.yadi;

import no.alexgaard.yadi.exception.CircularDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Registry {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Class<?>, Object> cache;

    private final Map<Class<?>, Provider<?>> providers;

    private final Deque<Class<?>> initializationStack;

    public Registry() {
        cache = new HashMap<>();
        providers = new HashMap<>();
        initializationStack = new ArrayDeque<>();
    }

    public <T> T get(Class<T> clazz) {
        var dependency = cache.get(clazz);

        if (dependency == null) {
            if (initializationStack.contains(clazz)) {
                List<Class<?>> dependencyCycle = new ArrayList(initializationStack.stream().toList());
                dependencyCycle.add(clazz);

                throw new CircularDependencyException(dependencyCycle);
            }

            initializationStack.add(clazz);

            dependency = create(clazz);
            cache.put(clazz, dependency);

            initializationStack.pop();
        }

        return (T) dependency;
    }

    public <T> T create(Class<T> clazz) {
        return getProvider(clazz).provide(this);
    }

    protected Map<Class<?>, Object> getDependencies() {
        return cache;
    }

    protected Map<Class<?>, Provider<?>> getProviders() {
        return providers;
    }

    protected <T> void registerProvider(Class<T> clazz, Provider<T> provider) {
        if (providers.containsKey(clazz)) {
            log.warn("A provider for {} has already been registered. Overwriting with new provider.", clazz.getCanonicalName());
        }

        providers.put(clazz, provider);
    }

    protected <T> void addDependency(Class<T> clazz, T dependency) {
        if (cache.containsKey(clazz)) {
            log.warn("The dependency {} has already been added. Overwriting with new dependency.", clazz.getCanonicalName());
        }

        cache.put(clazz, dependency);
    }

    private <T> Provider<T> getProvider(Class<T> clazz) {
        Provider<T> provider = (Provider<T>) providers.get(clazz);

        if (provider == null) {
            throw new IllegalStateException(
                    "Unable to find provider for dependency %s. Has this dependency been registered?"
                            .formatted(clazz.getCanonicalName())
            );
        }

        return provider;
    }

}
