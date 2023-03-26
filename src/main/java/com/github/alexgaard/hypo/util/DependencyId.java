package com.github.alexgaard.hypo.util;

public class DependencyId {

    private DependencyId() {}

    public static String id(Class<?> clazz) {
        return clazz.getCanonicalName();
    }

    public static String id(Class<?> clazz, String name) {
        if (name == null) return id(clazz);

        return clazz.getCanonicalName() + "@" + name;
    }

}
