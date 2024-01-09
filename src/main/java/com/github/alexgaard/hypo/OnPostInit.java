package com.github.alexgaard.hypo;

public interface OnPostInit<T> {

    void onPostInit(Dependencies dependencies, T dependency);

}
