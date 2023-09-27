package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.ConstructorInjectionFailedException;
import com.github.alexgaard.hypo.exception.MultipleMatchingConstructorException;
import com.github.alexgaard.hypo.exception.NoMatchingConstructorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.alexgaard.hypo.DependencyId.id;

/**
 * Resolves a set of registered dependency providers into an immutable instance of {@link Dependencies}
 */
public class Resolver {

    private static final Logger log = LoggerFactory.getLogger(Resolver.class);

    private final Map<String, Provider<?>> providers = new HashMap<>();

    private final Map<DependencyId, OnPostInit> onPostInitListeners = new HashMap<>();

    private final Set<Class<?>> constructorClasses = new HashSet<>();

    /**
     * Register a dependency, with an automatically created provider that invokes the constructor of the class.
     * The provider tries to find the constructor with the most matching parameters first.
     * @param clazz class of dependency
     * @return the resolver instance
     */
    public Resolver register(Class<?> clazz) {
        constructorClasses.add(clazz);
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

        constructorClasses.addAll(resolver.constructorClasses);

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
        constructorClasses.forEach(constructorClass -> {
            register(constructorClass, (Provider) createProviderFromConstructor(constructorClass, constructorClasses, providers));
        });

        Dependencies dependencies = new Dependencies(Map.copyOf(providers));

        dependencies.initialize();

        log.debug("Finished initialization of dependencies");

        onPostInitListeners.forEach(
                (id, listener) -> listener.onPostInit(dependencies, dependencies.get(id.clazz, id.name))
        );

        return dependencies;
    }

    private static Provider<?> createProviderFromConstructor(
            Class<?> constructorClass,
            Set<Class<?>> availableConstructors,
            Map<String, Provider<?>> availableProviders
    ) {
        List<Constructor<?>> constructors = Arrays.stream(constructorClass.getConstructors())
                .sorted((c1, c2) -> Integer.compare(c1.getParameterCount(), c2.getParameterCount()) * -1)
                .filter(constructor -> {
                    return Arrays.stream(constructor.getParameterTypes())
                            .allMatch(paramType -> availableConstructors.contains(paramType) || availableProviders.containsKey(id(paramType)));
                }).collect(Collectors.toList());

        if (constructors.isEmpty()) {
            throw new NoMatchingConstructorException(constructorClass);
        }

        Constructor<?> constructorToInvoke = constructors.get(0);

        constructors.forEach(constructor -> {
            boolean hasOtherConstructorWithMatchingParams = constructor != constructorToInvoke
                    && constructor.getParameterCount() == constructorToInvoke.getParameterCount();

            if (hasOtherConstructorWithMatchingParams) {
                throw new MultipleMatchingConstructorException(constructorClass, constructorToInvoke, constructor);
            }
        });

        return dependencies -> {
            Object[] constructorArgs = Arrays.stream(constructorToInvoke.getParameterTypes())
                    .map(dependencies::get)
                    .toArray();

            try {
                return constructorToInvoke.newInstance(constructorArgs);
            } catch (Throwable throwable) {
                throw new ConstructorInjectionFailedException(constructorClass, constructorToInvoke, throwable);
            }
        };
    }

    public interface OnPostInit<T> {

        void onPostInit(Dependencies dependencies, T dependency);

    }

}
