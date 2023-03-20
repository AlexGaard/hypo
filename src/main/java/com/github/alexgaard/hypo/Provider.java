package com.github.alexgaard.hypo;

public interface Provider<T> {
    T provide(Registry registry);

}
