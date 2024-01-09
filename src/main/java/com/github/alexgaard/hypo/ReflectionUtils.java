package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.ConstructorInjectionFailedException;
import com.github.alexgaard.hypo.exception.MultipleMatchingConstructorException;
import com.github.alexgaard.hypo.exception.NoMatchingConstructorException;
import io.github.classgraph.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.alexgaard.hypo.exception.ExceptionUtil.softenException;

public class ReflectionUtils {

    private static final String DEPENDENCY_ANNOTATION_NAME = Dependency.class.getCanonicalName();

    private ReflectionUtils() {
    }

    public static List<DependencyId> scanForClassesWithDependencyAnnotation(String... packagePaths) {
        ClassGraph graph = new ClassGraph()
                .disableRuntimeInvisibleAnnotations()
                .enableAnnotationInfo()
                .acceptPackages(packagePaths);

        List<DependencyId> dependencies = new ArrayList<>();

        try (ScanResult scanResult = graph.scan()) {
            for (ClassInfo dependencyClassInfo : scanResult.getClassesWithAnnotation(DEPENDENCY_ANNOTATION_NAME)) {
                AnnotationInfo annotationInfo = dependencyClassInfo.getAnnotationInfo(DEPENDENCY_ANNOTATION_NAME);

                if (!dependencyClassInfo.isStandardClass()) {
                    continue;
                }

                List<AnnotationParameterValue> parameters = annotationInfo.getParameterValues();

                Class<?> clazz = Class.forName(dependencyClassInfo.getName());
                String name = (String) parameters.get(0).getValue();

                dependencies.add(DependencyId.of(clazz, name));
            }
        } catch (ClassNotFoundException e) {
            throw softenException(e);
        }

        return dependencies;
    }

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
