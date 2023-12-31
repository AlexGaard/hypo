package com.github.alexgaard.hypo.example;

import com.github.alexgaard.hypo.Dependency;

@Dependency
public class AnnotatedService {

    private final Config config;

    public AnnotatedService(Config config) {
        this.config = config;
    }

}
