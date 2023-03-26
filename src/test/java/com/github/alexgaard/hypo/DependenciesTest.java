package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.example.*;
import com.github.alexgaard.hypo.exception.CircularDependencyException;
import com.github.alexgaard.hypo.exception.MissingDependencyProviderException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DependenciesTest {

    @Test
    void shouldResolveDependencies() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)))
                .resolve();

        assertNotNull(dependencies.get(Config.class));
        assertNotNull(dependencies.get(ServiceE.class));
    }

    @Test
    void get_shouldReturnTheSameDependency() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .resolve();

        assertEquals(dependencies.get(Config.class), dependencies.get(Config.class));
    }

    @Test
    void create_shouldReturnTheDifferentDependencies() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .resolve();

        assertNotEquals(dependencies.create(Config.class), dependencies.create(Config.class));
    }

    @Test
    void shouldResolveSameClassWithDifferentNames() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)))
                .register(ServiceE.class, "s1", (d) -> new ServiceE(d.get(Config.class)))
                .register(ServiceE.class, "s2", (d) -> new ServiceE(d.get(Config.class)))
                .resolve();

        ServiceE s = dependencies.get(ServiceE.class);
        ServiceE s1 = dependencies.get(ServiceE.class, "s1");
        ServiceE s2 = dependencies.get(ServiceE.class, "s2");

        assertNotNull(s);
        assertNotNull(s1);
        assertNotNull(s2);

        assertNotEquals(s, s1);
        assertNotEquals(s, s2);
        assertNotEquals(s1, s2);
    }

    @Test
    void shouldGetSameDependencyIfNameIsNull() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)))
                .resolve();

        assertEquals(dependencies.get(ServiceE.class), dependencies.get(ServiceE.class, null));
    }

    @Test
    void shouldResolveWithSameClassInstance() {
        AtomicReference<Config> configRef = new AtomicReference<>();
        AtomicReference<ServiceE> serviceERef = new AtomicReference<>();

        Dependencies dependencies = new Resolver()
                .register(Config.class, (d) -> {
                    var config = new Config();
                    configRef.set(config);
                    return config;
                })
                .register(ServiceE.class, (d) -> {
                    var serviceE = new ServiceE(d.get(Config.class));
                    serviceERef.set(serviceE);
                    return serviceE;
                })
                .resolve();

        assertEquals(configRef.get(), dependencies.get(Config.class));
        assertEquals(serviceERef.get(), dependencies.get(ServiceE.class));
    }

    @Test
    void shouldThrowCircularDependencyExceptionWhenACircularDependencyIsPresent() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceA.class, (d) -> new ServiceA(d.get(ServiceB.class)))
                .register(ServiceB.class, (d) -> new ServiceB(d.get(ServiceC.class)))
                .register(ServiceC.class, (d) -> new ServiceC(d.get(ServiceA.class), d.get(ServiceD.class), d.get(Config.class)));

        assertThrowsExactly(CircularDependencyException.class, resolver::resolve);
    }

    @Test
    void get_shouldThrowMissingDependencyProviderExceptionIfProviderNotRegistered() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d1 = resolver.resolve();

        assertThrowsExactly(MissingDependencyProviderException.class, () -> d1.get(ServiceB.class));
    }

    @Test
    void create_shouldThrowMissingDependencyProviderExceptionIfProviderNotRegistered() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d1 = resolver.resolve();

        assertThrowsExactly(MissingDependencyProviderException.class, () -> d1.create(ServiceB.class));
    }

    @Test
    void shouldReturnDifferentDependenciesWhenResolvingTwice() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d1 = resolver.resolve();
        Dependencies d2 = resolver.resolve();

        assertNotEquals(d1, d2);
    }

    @Test
    void get_shouldReturnDependencyFromCache() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d = resolver.resolve();

        assertEquals(d.get(ServiceE.class), d.get(ServiceE.class));
    }

    @Test
    void lazyGet_shouldReturnDependencyFromCache() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d = resolver.resolve();

        assertEquals(d.lazyGet(ServiceE.class).get(), d.lazyGet(ServiceE.class).get());
    }

    @Test
    void lazyGet_shouldReturnDependencyWithNameFromCache() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, "test", (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d = resolver.resolve();

        assertNotNull(d.lazyGet(ServiceE.class, "test").get());
    }

    @Test
    void create_shouldReturnNewDependency() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d = resolver.resolve();

        assertNotEquals(d.create(ServiceE.class), d.create(ServiceE.class));
    }

    @Test
    void create_shouldNotCacheDependency() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d = resolver.resolve();

        assertNotEquals(d.create(ServiceE.class), d.get(ServiceE.class));
    }

    @Test
    void lazyCreate_shouldReturnNewDependency() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d = resolver.resolve();

        assertNotEquals(d.lazyCreate(ServiceE.class).get(), d.lazyCreate(ServiceE.class).get());
    }

    @Test
    void lazyCreate_shouldReturnNewDependencyWithName() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, "test", (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d = resolver.resolve();

        assertNotNull(d.lazyCreate(ServiceE.class, "test").get());
    }

    @Test
    void shouldImplementHashCodeCorrectly() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Dependencies d1 = resolver.resolve();
        Dependencies d2 = resolver.resolve();

        assertNotEquals(d1.hashCode(), d2.hashCode());
    }

}
