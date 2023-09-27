package com.github.alexgaard.hypo.example;

public class ServiceF {

    public final ServiceD serviceD;

    public final Config config;

    public ServiceF() {
        this.serviceD = null;
        this.config = null;
    }

    public ServiceF(ServiceD serviceD) {
        this.serviceD = serviceD;
        this.config = null;
    }

    public ServiceF(ServiceD serviceD, Config config) {
        this.serviceD = serviceD;
        this.config = config;
    }

}
