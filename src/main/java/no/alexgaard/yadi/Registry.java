package no.alexgaard.yadi;

import no.alexgaard.yadi.exception.CircularDependencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Registry {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<Class<?>, Object> dependencies;
    private final Map<Class<?>, Provider<?>> providers;

    private final Deque<Class<?>> dependencyStack;

    public Registry() {
        dependencies = new HashMap<>();
        providers = new HashMap<>();
        dependencyStack = new ArrayDeque<>();
    }

    protected Map<Class<?>, Object> getDependencies() {
        return dependencies;
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
        if (dependencies.containsKey(clazz)) {
            log.warn("The dependency {} has already been registered. Overwriting with new dependency.", clazz.getCanonicalName());
        }

        dependencies.put(clazz, dependency);
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

    public <T> T get(Class<T> clazz) {
        var dependency = dependencies.get(clazz);

        if (dependency == null) {
            if (dependencyStack.contains(clazz)) {
                List<Class<?>> dependencyCycle = new ArrayList(dependencyStack.stream().toList());
                dependencyCycle.add(clazz);

                throw new CircularDependencyException(dependencyCycle);
            }

            dependencyStack.add(clazz);

            dependency = getProvider(clazz).provide(this);
            dependencies.put(clazz, dependency);

            dependencyStack.pop();
        }

        return (T) dependency;
    }

}
