package com.github.alexgaard.hypo.exception;

import com.github.alexgaard.hypo.Dependency;

import static com.github.alexgaard.hypo.AnnotatedDependencyProcessor.DEPENDENCY_MODULE_FULL_NAME;
import static java.lang.String.format;

public class AnnotationProcessorException extends RuntimeException {
    public AnnotationProcessorException() {
        super(message());
    }

    private static String message() {
        return format("Unable to find the auto-generated class %s. The annotation processor is not setup correctly or no classes have been annotated with %s.\n", DEPENDENCY_MODULE_FULL_NAME, Dependency.class.getCanonicalName()) +
                "To setup the annotation processor for Gradle: add 'annotationProcessor \"com.github.alexgaard:hypo:<VERSION>\"' to your build.gradle or\n" +
                "'annotationProcessor(\"com.github.alexgaard:hypo:<VERSION>\")' to your build.gradle.kts\n" +
                "To setup the annotation processor for Maven: configure 'org.apache.maven.plugins:maven-compiler-plugin'\n";
    }

}
