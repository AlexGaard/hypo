package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.example.*;
import com.github.alexgaard.hypo.exception.ConstructorInjectionFailedException;
import com.github.alexgaard.hypo.exception.MissingDependencyProviderException;
import com.github.alexgaard.hypo.exception.MultipleMatchingConstructorException;
import com.github.alexgaard.hypo.exception.NoMatchingConstructorException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ResolverTest {

    @Test
    void shouldResolveWithConstructorInjection() {
        Resolver resolver = new Resolver()
                .register(ServiceF.class)
                .register(Config.class, Config::new)
                .register(ServiceD.class, ServiceDImpl::new);

        Dependencies dependencies = resolver.resolve();

        assertNotNull(dependencies.get(ServiceD.class));
        assertNotNull(dependencies.get(Config.class));
        assertNotNull(dependencies.get(ServiceF.class));
    }

    @Test
    void shouldResolveWithConstructorInjectionLessParameters() {
        Resolver resolver = new Resolver()
                .register(ServiceD.class, ServiceDImpl::new)
                .register(ServiceF.class);

        Dependencies dependencies = resolver.resolve();

        assertNotNull(dependencies.get(ServiceD.class));
        assertThrows(MissingDependencyProviderException.class, () -> dependencies.get(Config.class));
        assertNotNull(dependencies.get(ServiceF.class));
    }

    @Test
    void shouldResolveWithConstructorInjectionNoParameters() {
        Resolver resolver = new Resolver()
                .register(ServiceF.class);

        Dependencies dependencies = resolver.resolve();

        assertThrows(MissingDependencyProviderException.class, () -> dependencies.get(ServiceD.class));
        assertThrows(MissingDependencyProviderException.class, () -> dependencies.get(Config.class));
        assertNotNull(dependencies.get(ServiceF.class));
    }

    @Test
    void shouldThrowWhenNoConstructorIsAvailable() {
        Resolver resolver = new Resolver()
                .register(ServiceA.class);

        assertThrows(NoMatchingConstructorException.class, resolver::resolve);
    }

    @Test
    void shouldThrowWhenMultipleMatchingConstructorsAreAvailable() {
        Resolver resolver = new Resolver()
                .register(ServiceG.class)
                .register(Config.class, Config::new)
                .register(ServiceD.class, ServiceDImpl::new);

        assertThrows(MultipleMatchingConstructorException.class, resolver::resolve);
    }

    @Test
    void shouldThrowWhenTryingToConstructorInjectAnInterface() {
        Resolver resolver = new Resolver()
                .register(ServiceD.class);

        assertThrows(NoMatchingConstructorException.class, resolver::resolve);
    }

    @Test
    void shouldResolveWithMultipleConstructorInjections() {
        Resolver resolver = new Resolver()
                .register(ServiceH.class)
                .register(ServiceF.class)
                .register(ServiceD.class, ServiceDImpl::new)
                .register(Config.class);

        assertDoesNotThrow(resolver::resolve);
    }

    @Test
    void shouldThrowCorrectExceptionWhenFailDuringConstructorInjection() {
        Resolver resolver = new Resolver()
                .register(ServiceI.class);

        assertThrows(ConstructorInjectionFailedException.class, resolver::resolve);
    }

    @Test
    void shouldCallPostInitAfterInitializingDependencies() {
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

    @Test
    void shouldRegisterProvidersFromModule() {
        Resolver resolver = new Resolver()
                .registerModule(this::configs);

        Dependencies dependencies = resolver.resolve();

        assertNotNull(dependencies.get(Config.class));
        assertNotNull(dependencies.get(ServiceE.class));
    }

    @Test
    void shouldCopyFromResolverSupplier() {
        Resolver resolver = new Resolver()
                .register(Config.class, Config::new)
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Resolver resolver2 = new Resolver()
                .copyFrom(() -> resolver);

        Dependencies dependencies = resolver2.resolve();

        assertNotNull(dependencies.get(Config.class));
        assertNotNull(dependencies.get(ServiceE.class));
    }

    @Test
    void shouldCopyFromResolver() {
        Resolver resolver = new Resolver()
                .register(Config.class, Config::new)
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));

        Resolver resolver2 = new Resolver()
                .copyFrom(resolver);

        Dependencies dependencies = resolver2.resolve();

        assertNotNull(dependencies.get(Config.class));
        assertNotNull(dependencies.get(ServiceE.class));
    }

    @Test
    void shouldScanForClassesAnnotatedWithDependency() {
        Dependencies dependencies = new Resolver()
                .scan("com.github.alexgaard.hypo")
                .register(Config.class, Config::new)
                .resolve();

        assertNotNull(dependencies.get(Config.class));
        assertNotNull(dependencies.get(AnnotatedService.class));
    }

    private void configs(Resolver resolver) {
        resolver.register(Config.class, Config::new)
                .register(ServiceE.class, (d) -> new ServiceE(d.get(Config.class)));
    }

}
