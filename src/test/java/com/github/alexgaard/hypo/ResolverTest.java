package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.example.*;
import com.github.alexgaard.hypo.exception.MissingDependencyProviderException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ResolverTest {

    @Test
    void shouldCallPostInitAfterInitlizingDependencies() {
        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceA.class, (d) -> new ServiceA(d.get(ServiceB.class)))
                .register(ServiceB.class, (d) -> new ServiceB(d.get(ServiceC.class)))
                .register(ServiceC.class,
                        (d) -> new ServiceC(null, d.get(ServiceD.class), d.get(Config.class)),
                        (d, serviceC) -> serviceC.setServiceA(d.get(ServiceA.class))
                )
                .register(ServiceD.class, (d) -> new ServiceDImpl());

        Dependencies dependencies = resolver.resolve();

        ServiceC serviceC = dependencies.get(ServiceC.class);

        assertNotNull(serviceC.getServiceA());
    }

    @Test
    void shouldOverwritePreviouslyRegisteredProvider() {
        AtomicBoolean called = new AtomicBoolean(false);

        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)))
                .register(ServiceE.class, (d) -> {
                    called.set(true);
                    return new ServiceE(d.get(Config.class));
                });

        resolver.resolve();

        assertTrue(called.get());
    }


    @Test
    void shouldOverwritePreviouslyRegisteredProviderWithPostInit() {
        AtomicBoolean called = new AtomicBoolean(false);

        Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceA.class, (d) -> new ServiceA(d.get(ServiceB.class)))
                .register(ServiceB.class, (d) -> new ServiceB(d.get(ServiceC.class)))
                .register(ServiceC.class,
                        (d) -> new ServiceC(null, d.get(ServiceD.class), d.get(Config.class)),
                        (d, serviceC) -> serviceC.setServiceA(d.get(ServiceA.class))
                )
                .register(ServiceC.class,
                        (d) -> new ServiceC(null, d.get(ServiceD.class), d.get(Config.class)),
                        (d, serviceC) -> {
                            called.set(true);
                            serviceC.setServiceA(d.get(ServiceA.class));
                        }
                )
                .register(ServiceD.class, (d) -> new ServiceDImpl());

        resolver.resolve();

        assertTrue(called.get());
    }

    @Test
    void shouldRegisterProviderWithName() {
        Resolver resolver = new Resolver()
                .register(Config.class, "config", Config::new);

        Dependencies dependencies = resolver.resolve();

        assertThrowsExactly(MissingDependencyProviderException.class, () -> dependencies.get(Config.class));
        assertNotNull(dependencies.get(Config.class, "config"));
    }

    @Test
    void shouldRegisterProviderWithNameAndPostInit() {
        AtomicBoolean called = new AtomicBoolean(false);

        Resolver resolver = new Resolver()
                .register(Config.class, "config", Config::new)
                .register(ServiceE.class, "e",
                        (d) -> new ServiceE(d.get(Config.class, "config")),
                        (dependencies, serviceE) -> called.set(true));

        Dependencies dependencies = resolver.resolve();

        assertNotNull(dependencies.get(ServiceE.class, "e"));
        assertTrue(called.get());
    }

    @Test
    void shouldRegisterSupplierWithNameAndPostInit() {
        AtomicBoolean called = new AtomicBoolean(false);

        Resolver resolver = new Resolver()
                .register(Config.class, "config", Config::new, (dependencies, serviceE) -> called.set(true));

        Dependencies dependencies = resolver.resolve();

        assertNotNull(dependencies.get(Config.class, "config"));
        assertTrue(called.get());
    }

}
