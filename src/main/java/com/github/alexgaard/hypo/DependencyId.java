package com.github.alexgaard.hypo;

import java.util.Objects;

public class DependencyId {

    public final Class<?> clazz;

    public final String name;

    private DependencyId(Class<?> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public String id() {
        return DependencyId.id(clazz, name);
    }

    public static DependencyId of(Class<?> clazz, String name) {
        if (name != null && name.isEmpty()) {
            return new DependencyId(clazz, null);
        }

        return new DependencyId(clazz, name);
    }

    public static DependencyId of(Class<?> clazz) {
        return new DependencyId(clazz, null);
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

    @Override
    public String toString() {
        return "DependencyId{class=" + clazz + ", name='" + name + "'}";
    }
}
