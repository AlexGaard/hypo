package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.ConstructorInjectionFailedException;
import com.github.alexgaard.hypo.exception.MultipleMatchingConstructorException;
import com.github.alexgaard.hypo.exception.NoMatchingConstructorException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectionUtils {

    private ReflectionUtils() {}

    public static <T> Provider<T> createProviderFromConstructor(Class<T> constructorClass) {
        return new Provider<>() {
            private Constructor<?> constructorToInvoke;

            @Override
            public T provide(Dependencies dependencies) {
                synchronized (this) {
                    if (constructorToInvoke == null) {
                        constructorToInvoke = findAvailableConstructor(dependencies, constructorClass);
                    }
                }

                Object[] constructorArgs = Arrays.stream(constructorToInvoke.getParameterTypes())
                        .map(dependencies::get)
                        .toArray();

                try {
                    return (T) constructorToInvoke.newInstance(constructorArgs);
                } catch (InvocationTargetException ite) {
                    throw new ConstructorInjectionFailedException(constructorClass, constructorToInvoke, ite.getCause());
                } catch (Exception exception) {
                    throw new ConstructorInjectionFailedException(constructorClass, constructorToInvoke, exception);
                }
            }
        };
    }

    private static Constructor<?> findAvailableConstructor(Dependencies dependencies, Class<?> constructorClass) {
        List<Class<?>> availableClasses = dependencies.getProviders()
                .keySet()
                .stream()
                .map(p -> p.clazz).collect(Collectors.toList());

        List<Constructor<?>> constructors = Arrays.stream(constructorClass.getConstructors())
                .sorted((c1, c2) -> Integer.compare(c1.getParameterCount(), c2.getParameterCount()) * -1)
                .filter(constructor -> Arrays.stream(constructor.getParameterTypes()).allMatch(availableClasses::contains))
                .collect(Collectors.toList());

        if (constructors.isEmpty()) {
            throw new NoMatchingConstructorException(constructorClass, availableClasses::contains);
        }

        Constructor<?> constructorToInvoke = constructors.get(0);

        constructors.forEach(constructor -> {
            boolean hasOtherConstructorWithMatchingParams = constructor != constructorToInvoke
                    && constructor.getParameterCount() == constructorToInvoke.getParameterCount();

            if (hasOtherConstructorWithMatchingParams) {
                throw new MultipleMatchingConstructorException(constructorClass, constructorToInvoke, constructor);
            }
        });

        return constructorToInvoke;
    }

}
