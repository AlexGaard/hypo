package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.annotation.InjectInto;
import com.github.alexgaard.hypo.exception.*;

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
                        constructorToInvoke = findAvailableConstructor(constructorClass);
                    }
                }

                Object[] constructorArgs = Arrays.stream(constructorToInvoke.getParameterTypes())
                        .map(param -> {
                            try {
                                return dependencies.get(param);
                            } catch (MissingDependencyProviderException e) {
                                throw new InvalidConstructorException(constructorToInvoke, param);
                            }
                        })
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

    private static Constructor<?> findAvailableConstructor(Class<?> constructorClass) {
        Constructor<?>[] constructors = constructorClass.getConstructors();

        Constructor<?> constructorToInvoke;

        if (constructors.length == 0) {
            throw new NoPublicConstructorException(constructorClass);
        } else if (constructors.length == 1) {
            constructorToInvoke = constructors[0];
        } else {
            List<Constructor<?>> annotatedConstructors = Arrays.stream(constructors)
                    .filter(constructor -> constructor.getAnnotation(InjectInto.class) != null)
                    .collect(Collectors.toList());

            if (annotatedConstructors.isEmpty()) {
                throw new MultipleConstructorsException(constructorClass);
            } else if (annotatedConstructors.size() == 1) {
                constructorToInvoke = annotatedConstructors.get(0);
            } else {
                throw new MultipleAnnotatedConstructorsException(constructorClass);
            }
        }

        return constructorToInvoke;
    }

}
