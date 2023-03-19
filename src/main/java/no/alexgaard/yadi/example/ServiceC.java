package no.alexgaard.yadi.example;

public class ServiceC {
    private ServiceA serviceA;

    private final IServiceD serviceD;

    private final Config config;

    public ServiceC(ServiceA serviceA, IServiceD serviceD, Config config) {
        this.serviceA = serviceA;
        this.serviceD = serviceD;
        this.config = config;
    }

    public ServiceA getServiceA() {
        return serviceA;
    }

    public void setServiceA(ServiceA serviceA) {
        this.serviceA = serviceA;
    }


}
