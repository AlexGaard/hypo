package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.AnnotationProcessorException;
import com.github.alexgaard.hypo.exception.MultipleMatchingConstructorException;
import com.github.alexgaard.hypo.exception.NoMatchingConstructorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.github.alexgaard.hypo.AnnotatedDependencyProcessor.DEPENDENCY_MODULE_FULL_NAME;
import static com.github.alexgaard.hypo.AnnotatedDependencyProcessor.DEPENDENCY_MODULE_REGISTER_FUNCTION_NAME;
import static com.github.alexgaard.hypo.ReflectionUtils.createProviderFromConstructor;

/**
 * Resolves a set of registered dependency providers into an immutable instance of {@link Dependencies}
 */
public class Resolver {

    private static final Logger log = LoggerFactory.getLogger(Resolver.class);

    private final Map<DependencyId, Provider<?>> providers = new HashMap<>();

    private final Map<DependencyId, OnPostInit> onPostInitListeners = new HashMap<>();

    /**
     * Register a dependency with an automatically created provider that invokes the constructor of the class.
     * The provider tries to find the constructor with the most matching parameters first.
     * If no matching constructor can be found, a {@link NoMatchingConstructorException} will be thrown when resolving the dependencies.
     * If multiple matching constructors are found, a {@link MultipleMatchingConstructorException} will be thrown when resolving the dependencies.
     * @param dependencyClass class of dependency
     * @param <T> type of dependency
     * @return the resolver instance
     */
    public <T> Resolver register(Class<T> dependencyClass) {
        register(dependencyClass, createProviderFromConstructor(dependencyClass));
        return this;
    }

    /**
     * Register a named dependency with an automatically created provider that invokes the constructor of the class.
     * The provider tries to find the constructor with the most matching parameters first.
     * If no matching constructor can be found, a {@link NoMatchingConstructorException} will be thrown when resolving the dependencies.
     * If multiple matching constructors are found, a {@link MultipleMatchingConstructorException} will be thrown when resolving the dependencies.
     * @param dependencyClass class of dependency
     * @param name name of the dependency
     * @param <T> type of dependency
     * @return the resolver instance
     */
    public <T> Resolver register(Class<T> dependencyClass, String name) {
        register(dependencyClass, name, createProviderFromConstructor(dependencyClass));
        return this;
    }

    /**
     * Register a dependency with a class that it extends or implements from.
     * The dependency is created from an automatically created provider that invokes the constructor of the class.
     * The provider tries to find the constructor with the most matching parameters first.
     * If no matching constructor can be found, a {@link NoMatchingConstructorException} will be thrown when resolving the dependencies.
     * If multiple matching constructors are found, a {@link MultipleMatchingConstructorException} will be thrown when resolving the dependencies.
     * @param superOrInterfaceClass class which dependency extends or implements
     * @param dependencyClass class of dependency
     * @param <S> super class that dependency extends or interface that dependency implements
     * @param <T> type of dependency
     * @return the resolver instance
     */
    public <S, T extends S> Resolver register(Class<S> superOrInterfaceClass, Class<T> dependencyClass) {
        register(superOrInterfaceClass, (Provider<S>) createProviderFromConstructor(dependencyClass));
        return this;
    }

    /**
     * Register a named dependency with a class that it extends or implements from.
     * The dependency is created from an automatically created provider that invokes the constructor of the class.
     * The provider tries to find the constructor with the most matching parameters first.
     * If no matching constructor can be found, a {@link NoMatchingConstructorException} will be thrown when resolving the dependencies.
     * If multiple matching constructors are found, a {@link MultipleMatchingConstructorException} will be thrown when resolving the dependencies.
     * @param superOrInterfaceClass class which dependency extends or implements
     * @param dependencyClass class of dependency
     * @param name name of the dependency
     * @param <S> super class that dependency extends or interface that dependency implements
     * @param <T> type of dependency
     * @return the resolver instance
     */
    public <S, T extends S> Resolver register(Class<S> superOrInterfaceClass, Class<T> dependencyClass, String name) {
        register(superOrInterfaceClass, name, (Provider<S>) createProviderFromConstructor(dependencyClass));
        return this;
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param dependencyClass class of dependency
     * @param provider provider for the dependency
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> dependencyClass, Provider<T> provider) {
        return register(dependencyClass, null, provider, null);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param dependencyClass class of dependency
     * @param provider provider for the dependency
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> dependencyClass, Supplier<T> provider) {
        return register(dependencyClass, ignored -> provider.get());
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param dependencyClass class of dependency
     * @param provider provider for the dependency
     * @param onPostInit callback which is triggered after the initialization of dependencies
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> dependencyClass, Provider<T> provider, OnPostInit<T> onPostInit) {
        return register(dependencyClass, null, provider, onPostInit);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param dependencyClass class of dependency
     * @param name name of the dependency
     * @param provider provider for the dependency
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> dependencyClass, String name, Provider<T> provider) {
        return register(dependencyClass, name, provider, null);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param dependencyClass class of dependency
     * @param name name of the dependency
     * @param provider provider for the dependency
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> dependencyClass, String name, Supplier<T> provider) {
        return register(dependencyClass, name, ignored -> provider.get(), null);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param dependencyClass class of dependency
     * @param name name of the dependency
     * @param provider provider for the dependency
     * @param onPostInit callback which is triggered after the initialization of dependencies
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> dependencyClass, String name, Supplier<T> provider, OnPostInit<T> onPostInit) {
        return register(dependencyClass, name, ignored -> provider.get(), onPostInit);
    }

    /**
     * Register a dependency provider for the specified dependency.
     * @param dependencyClass class of dependency
     * @param name name of the dependency
     * @param provider provider for the dependency
     * @param onPostInit callback which is triggered after the initialization of dependencies
     * @return the resolver instance
     * @param <T> type of dependency
     */
    public <T> Resolver register(Class<T> dependencyClass, String name, Provider<T> provider, OnPostInit<T> onPostInit) {
        DependencyId dependencyId = DependencyId.of(dependencyClass, name);

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

        return this;
    }

    /**
     * Register all dependencies annotated with {@link Dependency}.
     * This function relies on the annotation processor {@link AnnotatedDependencyProcessor} to work correctly.
     * This is usually configured in your pom.xml or build.gradle/build.gradle.kts file.
     * The dependencies are automatically created with a provider that invokes the constructor.
     * @return the resolver instance
     */
    public Resolver registerAnnotatedDependencies() {
        try {
            Class<?> clazz = Class.forName(DEPENDENCY_MODULE_FULL_NAME);
            Method method = clazz.getDeclaredMethod(DEPENDENCY_MODULE_REGISTER_FUNCTION_NAME, Resolver.class);
            method.invoke(null, this);
        } catch (ClassNotFoundException e) {
            throw new AnnotationProcessorException();
        } catch (Exception e) {
            throw new RuntimeException("Failed to register generated dependency module", e);
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
        Map<DependencyId, Provider<?>> providerCopy = new HashMap<>(providers.size());
        providerCopy.putAll(providers);

        Dependencies dependencies = new Dependencies(providerCopy);

        dependencies.initialize();

        log.debug("Finished initialization of dependencies");

        onPostInitListeners.forEach(
                (id, listener) -> listener.onPostInit(dependencies, dependencies.get(id.clazz, id.name))
        );

        return dependencies;
    }

}
