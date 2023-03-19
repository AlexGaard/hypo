package no.alexgaard.yadi;

public interface DependencyProvider<T> {
    T provide(Registry registry);

}
