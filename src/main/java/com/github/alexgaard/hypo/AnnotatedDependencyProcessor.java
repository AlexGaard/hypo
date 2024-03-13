package com.github.alexgaard.hypo;

import com.github.alexgaard.hypo.annotation.Dependency;
import com.github.alexgaard.hypo.annotation.InjectInto;
import com.github.alexgaard.hypo.annotation.Named;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.alexgaard.hypo.exception.ExceptionUtil.softenException;
import static java.lang.String.format;
import static javax.tools.StandardLocation.SOURCE_OUTPUT;

@AutoService(Processor.class)
public class AnnotatedDependencyProcessor extends AbstractProcessor {

    public static final String GENERATED_MODULE_PACKAGE = "com.github.alexgaard.hypo.generated";

    public static final String DEPENDENCY_MODULE_CLASS_NAME = "DependencyModule";

    public static final String DEPENDENCY_MODULE_REGISTER_FUNCTION_NAME = "registerModule";

    public static final String DEPENDENCY_MODULE_FULL_NAME = GENERATED_MODULE_PACKAGE + "." + DEPENDENCY_MODULE_CLASS_NAME;

    private static final String LOGGER_NAME = AnnotatedDependencyProcessor.class.getSimpleName();


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Dependency.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            if (annotatedElements.isEmpty()) {
                continue;
            }

            try {
                long start = System.currentTimeMillis();

                System.out.printf("[%s] Generating %s%n", LOGGER_NAME, DEPENDENCY_MODULE_FULL_NAME);

                String currentSource = getGeneratedModuleClassSource();

                // TODO: Use resource file to store data properly

                List<String> registerLines = annotatedElements
                        .stream()
                        .map(AnnotatedDependencyProcessor::createRegisterDependencyCode)
                        .collect(Collectors.toList());

                if (currentSource != null) {
                    List<String> currentLines = extractRegisterLinesFromSource(currentSource);

                    currentLines.forEach(line -> {
                        String lineStart = line.substring(0, line.indexOf(","));

                        if (registerLines.stream().noneMatch(regLine -> regLine.startsWith(lineStart))) {
                            registerLines.add(line);
                        }
                    });
                }

                Collections.sort(registerLines);

                String dependencyModuleClassSource = createDependencyModuleClassSource(registerLines);

                writeDependencyModuleSourceCode(dependencyModuleClassSource);

                System.out.printf("[%s] Time taken: %s%n", LOGGER_NAME, Duration.ofMillis(System.currentTimeMillis() - start));
            } catch (Exception e) {
                e.printStackTrace();
                throw softenException(e);
            }
        }

        return true;
    }

    private List<String> extractRegisterLinesFromSource(String dependencyModuleSourceCode) {
        return dependencyModuleSourceCode
                .lines()
                .filter(l -> l.contains("resolver.register("))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private void writeDependencyModuleSourceCode(String sourceCode) throws IOException {
        JavaFileObject builderFile = processingEnv.getFiler()
                .createSourceFile(DEPENDENCY_MODULE_FULL_NAME);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
            out.write(sourceCode);
            out.flush();
        }
    }

    private String getGeneratedModuleClassSource() {
        try {
            FileObject file = processingEnv.getFiler()
                    .getResource(SOURCE_OUTPUT, GENERATED_MODULE_PACKAGE, DEPENDENCY_MODULE_CLASS_NAME + ".java");

            return file.getCharContent(true).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String createDependencyModuleClassSource(Collection<String> resolveMethodLines) {
        List<String> lines = new ArrayList<>();
        lines.add(format("package %s;", GENERATED_MODULE_PACKAGE));
        lines.add("");
        lines.add(format("public class %s {", DEPENDENCY_MODULE_CLASS_NAME));
        lines.add("");
        lines.add(format("    private %s() {}", DEPENDENCY_MODULE_CLASS_NAME));
        lines.add("");
        lines.add(format("    public static void %s(%s resolver) {", DEPENDENCY_MODULE_REGISTER_FUNCTION_NAME, Resolver.class.getCanonicalName()));
        lines.add("        // Generated at: " + OffsetDateTime.now());
        lines.add("");
        resolveMethodLines.forEach(line -> {
            lines.add("        " + line);
        });
        lines.add("    }");
        lines.add("");
        lines.add("}");

        return String.join(System.lineSeparator(), lines.toArray(new String[0]));
    }

    private static String createRegisterDependencyCode(Element element) {
        Dependency dependencyAnnotation = element.getAnnotation(Dependency.class);
        String name = dependencyAnnotation.name();

        String classNameFull = element.toString();

        ExecutableElement availableConstructor = findAvailableConstructor(element);

        String parameters = availableConstructor.getParameters()
                .stream()
                .map(p -> {
                    Named named = p.getAnnotation(Named.class);
                    if (named != null && !named.value().isBlank()) {
                        return format("d.get(%s.class, \"%s\")", p.asType().toString(), named.value());
                    } else {
                        return format("d.get(%s.class)", p.asType().toString());
                    }
                })
                .collect(Collectors.joining(", "));

        if (name.isEmpty()) {
            return format("resolver.register(%s.class, (d) -> new %s(%s));", classNameFull, classNameFull, parameters);
        } else {
            return format("resolver.register(%s.class, \"%s\", (d) -> new %s(%s));", classNameFull, name, classNameFull, parameters);
        }
    }

    private static ExecutableElement findAvailableConstructor(Element classElement) {
        List<ExecutableElement> availableConstructors = classElement.getEnclosedElements()
                .stream()
                .filter(enclosedElement -> {
                    if (!(enclosedElement instanceof ExecutableElement)) {
                        return false;
                    }

                    ExecutableElement execElement = ((ExecutableElement) enclosedElement);

                    if (!execElement.getModifiers().contains(Modifier.PUBLIC)) {
                        return false;
                    }

                    return execElement.getSimpleName().toString().equals("<init>");
                })
                .map(e -> (ExecutableElement) e)
                .collect(Collectors.toList());

        if (availableConstructors.isEmpty()) {
            throw new RuntimeException("No available constructors");
        } else if (availableConstructors.size() == 1) {
            return availableConstructors.get(0);
        } else {
            List<ExecutableElement> constructorsWithAnnotation = availableConstructors
                    .stream()
                    .filter(e -> e.getAnnotation(InjectInto.class) != null)
                    .collect(Collectors.toList());

            if (constructorsWithAnnotation.isEmpty()) {
                throw new RuntimeException("Multiple constructors no annotation");
            } else if (constructorsWithAnnotation.size() == 1) {
                return constructorsWithAnnotation.get(0);
            } else {
                throw new RuntimeException("Multiple constructors with annotation");
            }
        }
    }

}
