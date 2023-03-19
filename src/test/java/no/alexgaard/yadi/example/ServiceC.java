package no.alexgaard.yadi.example;

public class ServiceC {
    private ServiceA serviceA;

    private final ServiceD serviceD;

    private final Config config;

    public ServiceC(ServiceA serviceA, ServiceD serviceD, Config config) {
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
