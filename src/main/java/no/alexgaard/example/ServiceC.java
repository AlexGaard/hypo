package no.alexgaard.example;

public class ServiceC {

    private final ServiceA serviceA;

    private final IServiceD serviceD;

    private final Config config;

    public ServiceC(ServiceA serviceA, IServiceD serviceD, Config config) {
        this.serviceA = serviceA;
        this.serviceD = serviceD;
        this.config = config;
    }
}
