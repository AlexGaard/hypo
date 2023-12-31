package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.exception.ConstructorInjectionFailedException;
import com.github.alexgaard.hypo.exception.MultipleMatchingConstructorException;
import com.github.alexgaard.hypo.exception.NoMatchingConstructorException;
import io.github.classgraph.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class ReflectionUtils {

    private final static String DEPENDENCY_ANNOTATION_NAME = Dependency.class.getCanonicalName();

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
            throw new RuntimeException(e);
        }

        return dependencies;
    }

    public static Provider<?> createProviderFromConstructor(
            Class<?> constructorClass,
            Set<DependencyId> availableDependencies,
            Map<DependencyId, Provider<?>> availableProviders
    ) {
        Set<Class<?>> availableClasses = availableDependencies.stream().map(d -> d.clazz).collect(Collectors.toSet());
        availableClasses.addAll(availableProviders.keySet().stream().map(d -> d.clazz).collect(Collectors.toSet()));

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

        return dependencies -> {
            Object[] constructorArgs = Arrays.stream(constructorToInvoke.getParameterTypes())
                    .map(dependencies::get)
                    .toArray();

            try {
                return constructorToInvoke.newInstance(constructorArgs);
            } catch (InvocationTargetException ite) {
                throw new ConstructorInjectionFailedException(constructorClass, constructorToInvoke, ite.getCause());
            }catch (Exception exception) {
                throw new ConstructorInjectionFailedException(constructorClass, constructorToInvoke, exception);
            }
        };
    }

}
