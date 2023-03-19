package no.alexgaard.di;

public interface DependencyProvider<T> {
    T provide(Registry registry);

}
