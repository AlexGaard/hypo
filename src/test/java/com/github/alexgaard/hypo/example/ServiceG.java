package com.github.alexgaard.hypo.example;

public class ServiceG {

    public final ServiceD serviceD;

    public final Config config;

    public ServiceG(Config config, ServiceD serviceD) {
        this.serviceD = serviceD;
        this.config = config;
    }

    public ServiceG(ServiceD serviceD, Config config) {
        this.serviceD = serviceD;
        this.config = config;
    }

}
