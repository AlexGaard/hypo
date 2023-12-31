package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.ConstructorInjectionFailedException;
import com.github.alexgaard.hypo.exception.MultipleMatchingConstructorException;
import com.github.alexgaard.hypo.exception.NoMatchingConstructorException;
import io.github.classgraph.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.alexgaard.hypo.DependencyId.id;
import static com.github.alexgaard.hypo.ReflectionUtils.createProviderFromConstructor;

/**
 * Resolves a set of registered dependency providers into an immutable instance of {@link Dependencies}
 */
public class Resolver {

    private static final Logger log = LoggerFactory.getLogger(Resolver.class);

    private final Map<DependencyId, Provider<?>> providers = new HashMap<>();

    private final Map<DependencyId, OnPostInit> onPostInitListeners = new HashMap<>();

    private final Set<DependencyId> constructorInjectionDependencies = new HashSet<>();

    /**
     * Register a dependency, with an automatically created provider that invokes the constructor of the class.
     * The provider tries to find the constructor with the most matching parameters first.
     * If no matching constructor can be found, a {@link NoMatchingConstructorException} will be thrown when resolving the dependencies.
     * If multiple matching constructors are found, a {@link MultipleMatchingConstructorException} will be thrown when resolving the dependencies.
     * @param clazz class of dependency
     * @return the resolver instance
     */
    public Resolver register(Class<?> clazz) {
        constructorInjectionDependencies.add(DependencyId.of(clazz));
        return this;
    }

    /**
     * Register a named dependency, with an automatically created provider that invokes the constructor of the class.
     * The provider tries to find the constructor with the most matching parameters first.
     * If no matching constructor can be found, a {@link NoMatchingConstructorException} will be thrown when resolving the dependencies.
     * If multiple matching constructors are found, a {@link MultipleMatchingConstructorException} will be thrown when resolving the dependencies.
     * @param clazz class of dependency
     * @param name name of the dependency
     * @return the resolver instance
     */
    public Resolver register(Class<?> clazz, String name) {
        constructorInjectionDependencies.add(DependencyId.of(clazz, name));
        return this;
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
        DependencyId dependencyId = DependencyId.of(clazz, name);

        if (providers.containsKey(dependencyId)) {
            log.warn("Overwriting the previously registered provider for {}", dependencyId);
        }

        providers.put(dependencyId, provider);

        if (onPostInit != null) {
            onPostInitListeners.put(dependencyId, onPostInit);
        }

        return this;
    }

    /**
     * Registers the providers from a module. A module is a grouping of dependency providers that are registered together.
     * Ex:
     * <pre>{@code
     * Dependencies dependencies = new Resolver()
     *                .registerModule(this::configs)
     *                .resolve();
     *
     *private void configs(Resolver resolver) {
     *    resolver.register(ConfigA.class, ConfigA::new)
     *        .register(ConfigB.class, ConfigB::new);
     *}
     * }</pre>
     * @param module a consumer which is invoked with the resolver instance
     * @return the resolver instance
     */
    public Resolver registerModule(Consumer<Resolver> module) {
        module.accept(this);
        return this;
    }

    /**
     * Copies the registered providers and listeners from another resolver.
     * If the providers or listeners already exists, then they will be overwritten.
     * @param resolverSupplier supplies a resolver to copy from
     * @return the resolver instance
     */
    public Resolver copyFrom(Supplier<Resolver> resolverSupplier) {
        return copyFrom(resolverSupplier.get());
    }

    /**
     * Copies the registered providers and listeners from another resolver.
     * If the providers or listeners already exists, then they will be overwritten.
     * @param resolver the resolver to copy from
     * @return the resolver instance
     */
    public Resolver copyFrom(Resolver resolver) {
        providers.putAll(resolver.providers);

        onPostInitListeners.putAll(resolver.onPostInitListeners);

        constructorInjectionDependencies.addAll(resolver.constructorInjectionDependencies);

        return this;
    }

    /**
     * Scans the class path for classes annotated with {@link Dependency}
     * and registers them with an automatically created provider that invokes the constructor of the class. See {@link #register(Class, String)}
     * @param packagePaths the package paths that will be scanned. Ex: "com.github.alexgaard.hypo"
     * @return the resolver instance
     */
    public Resolver scan(String... packagePaths) {
        ReflectionUtils.scanForClassesWithDependencyAnnotation(packagePaths)
                .forEach(dependency -> register(dependency.clazz, dependency.name));

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
        // Register providers for constructors
        constructorInjectionDependencies.forEach(dependencyId -> {
            Provider provider = createProviderFromConstructor(dependencyId.clazz, constructorInjectionDependencies, providers);
            register(dependencyId.clazz, dependencyId.name, provider);
        });

        Map<String, Provider<?>> providerCopy = new HashMap<>(providers.size());
        providers.forEach((k, v) -> providerCopy.put(k.id(), v));

        Dependencies dependencies = new Dependencies(providerCopy);

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

}
