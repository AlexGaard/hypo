package no.alexgaard;

import no.alexgaard.di.DependencyResolver;
import no.alexgaard.example.*;

public class Main {

    public static void main(String[] args) {
        var resolver = new DependencyResolver();

        resolver.register(Config.class, Config::new);
        resolver.register(ServiceA.class, (reg) -> new ServiceA(reg.get(ServiceB.class)));
        resolver.register(ServiceB.class, (reg) -> new ServiceB(reg.get(ServiceC.class)));
        resolver.register(ServiceC.class, (reg) -> new ServiceC(
                reg.get(ServiceA.class), reg.get(IServiceD.class), reg.get(Config.class))
        );
        resolver.register(IServiceD.class, ServiceD::new);

        var dependencies = resolver.resolve();

        System.out.println(dependencies.get(ServiceA.class));

        System.out.println(dependencies);
    }

}