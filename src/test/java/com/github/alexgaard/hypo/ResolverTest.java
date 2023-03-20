package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.example.*;
import com.github.alexgaard.hypo.exception.CircularDependencyException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ResolverTest {

    @Test
    public void shouldResolveDependencies() {
        var resolver = new Resolver()
                .register(Config.class, Config::new)
                .register(ServiceE.class, (r) -> new ServiceE(r.get(Config.class)));

        var dependencies = resolver.resolve();

        assertNotNull(dependencies.get(Config.class));
        assertNotNull(dependencies.get(ServiceE.class));
    }

    @Test
    public void shouldResolveWithSameClassInstance() {
        AtomicReference<Config> configRef = new AtomicReference<>();
        AtomicReference<ServiceE> serviceERef = new AtomicReference<>();

        var resolver = new Resolver()
                .register(Config.class, (r) -> {
                    var config = new Config();
                    configRef.set(config);
                    return config;
                })
                .register(ServiceE.class, (r) -> {
                    var serviceE = new ServiceE(r.get(Config.class));
                    serviceERef.set(serviceE);
                    return serviceE;
                });

        var dependencies = resolver.resolve();

        assertEquals(configRef.get(), dependencies.get(Config.class));
        assertEquals(serviceERef.get(), dependencies.get(ServiceE.class));
    }

    @Test
    public void shouldThrowCircularDependencyException() {
        var resolver = new Resolver()
                .register(Config.class, (r) -> new Config())
                .register(ServiceA.class, (r) -> new ServiceA(r.get(ServiceB.class)))
                .register(ServiceB.class, (r) -> new ServiceB(r.get(ServiceC.class)))
                .register(ServiceC.class, (r) -> new ServiceC(r.get(ServiceA.class), r.get(ServiceD.class), r.get(Config.class)));

        assertThrowsExactly(CircularDependencyException.class, resolver::resolve);
    }

}
