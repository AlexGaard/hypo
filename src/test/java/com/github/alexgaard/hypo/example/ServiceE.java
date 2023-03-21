package com.github.alexgaard.hypo.example;

public class ServiceE {
    public final Config config;

    public ServiceE(Config config) {
        this.config = config;
    }

    public static class ServiceEChild {
        public final String text;

        public ServiceEChild(String text) {
            this.text = text;
        }
    }

}
