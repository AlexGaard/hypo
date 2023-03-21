package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.example.*;
import com.github.alexgaard.hypo.exception.CircularDependencyException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ResolverTest {

    @Test
    public void shouldResolveDependencies() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .register(ServiceE.class, (r) -> new ServiceE(r.get(Config.class)))
                .resolve();

        assertNotNull(dependencies.get(Config.class));
        assertNotNull(dependencies.get(ServiceE.class));
    }

    @Test
    public void get_shouldReturnTheSameDependency() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .resolve();

        assertEquals(dependencies.get(Config.class), dependencies.get(Config.class));
    }

    @Test
    public void create_shouldReturnTheDifferentDependencies() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .resolve();

        assertNotEquals(dependencies.create(Config.class), dependencies.create(Config.class));
    }

    @Test
    public void shouldResolveSameClassWithDifferentNames() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .register(ServiceE.class, (r) -> new ServiceE(r.get(Config.class)))
                .register(ServiceE.class, "s1", (r) -> new ServiceE(r.get(Config.class)))
                .register(ServiceE.class, "s2", (r) -> new ServiceE(r.get(Config.class)))
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
    public void shouldGetSameDependencyIfNameIsNull() {
        Dependencies dependencies = new Resolver()
                .register(Config.class, Config::new)
                .register(ServiceE.class, (r) -> new ServiceE(r.get(Config.class)))
                .resolve();

        assertEquals(dependencies.get(ServiceE.class), dependencies.get(ServiceE.class, null));
    }

    @Test
    public void shouldResolveWithSameClassInstance() {
        AtomicReference<Config> configRef = new AtomicReference<>();
        AtomicReference<ServiceE> serviceERef = new AtomicReference<>();

        Dependencies dependencies = new Resolver()
                .register(Config.class, (r) -> {
                    var config = new Config();
                    configRef.set(config);
                    return config;
                })
                .register(ServiceE.class, (r) -> {
                    var serviceE = new ServiceE(r.get(Config.class));
                    serviceERef.set(serviceE);
                    return serviceE;
                })
                .resolve();

        assertEquals(configRef.get(), dependencies.get(Config.class));
        assertEquals(serviceERef.get(), dependencies.get(ServiceE.class));
    }

    @Test
    public void shouldThrowCircularDependencyException() {
        Resolver resolver = new Resolver()
                .register(Config.class, (r) -> new Config())
                .register(ServiceA.class, (r) -> new ServiceA(r.get(ServiceB.class)))
                .register(ServiceB.class, (r) -> new ServiceB(r.get(ServiceC.class)))
                .register(ServiceC.class, (r) -> new ServiceC(r.get(ServiceA.class), r.get(ServiceD.class), r.get(Config.class)));

        assertThrowsExactly(CircularDependencyException.class, resolver::resolve);
    }

}
