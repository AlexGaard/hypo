package com.github.alexgaard.hypo;

/**
 * Provides a dependency when invoked.
 * A provider should always create a new instance of a dependency.
 * @param <T> type of dependency which is provided
 */
public interface Provider<T> {
    T provide(Dependencies dependencies);

}
