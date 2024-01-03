package com.github.alexgaard.hypo;

import java.util.Objects;

import static com.github.alexgaard.hypo.exception.ExceptionUtil.softenException;
import static java.lang.String.format;

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

    public static DependencyId of(String id) {
        String[] values = id.split("@");

        if (values.length != 1 && values.length != 2) {
            throw new IllegalArgumentException(format("id '%s' is not correctly formatted", id));
        }

        try {
            Class<?> clazz = Class.forName(values[0]);

            if (values.length == 1) {
                return DependencyId.of(clazz);
            } else {
                return DependencyId.of(clazz, values[1]);
            }
        } catch (ClassNotFoundException e) {
            throw softenException(e);
        }
    }

    public static String id(Class<?> clazz) {
        return clazz.getName();
    }

    public static String id(Class<?> clazz, String name) {
        if (name == null) return id(clazz);

        return clazz.getName() + "@" + name;
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
