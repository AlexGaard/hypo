package no.alexgaard.yadi;

public interface OnResolved<T> {

    void onResolved(Registry registry, T dependency);

}
