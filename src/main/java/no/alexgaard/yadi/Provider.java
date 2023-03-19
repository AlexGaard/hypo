package no.alexgaard.yadi;

public interface Provider<T> {
    T provide(Registry registry);

}
