package com.github.alexgaard.hypo.example;

import com.github.alexgaard.hypo.annotation.InjectInto;

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

    @InjectInto
    public ServiceF(ServiceD serviceD, Config config) {
        this.serviceD = serviceD;
        this.config = config;
    }

}
