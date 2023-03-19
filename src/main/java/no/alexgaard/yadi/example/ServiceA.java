package no.alexgaard.yadi.example;

public class ServiceA {

    private final ServiceB serviceB;

    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}
