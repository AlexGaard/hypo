package com.github.alexgaard.hypo;

import java.util.Objects;

public class DependencyId {

    public final Class<?> clazz;

    public final String name;

    public DependencyId(Class<?> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public static String id(Class<?> clazz) {
        return clazz.getCanonicalName();
    }

    public static String id(Class<?> clazz, String name) {
        if (name == null) return id(clazz);

        return clazz.getCanonicalName() + "@" + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DependencyId that = (DependencyId) o;
        return clazz.equals(that.clazz) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, name);
    }

}
