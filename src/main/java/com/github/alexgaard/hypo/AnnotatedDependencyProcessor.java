package com.github.alexgaard.hypo;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;

@AutoService(Processor.class)
public class AnnotatedDependencyProcessor extends AbstractProcessor {

    public final static String GENERATED_MODULE_PACKAGE = "com.github.alexgaard.hypo.generated";

    public final static String DEPENDENCY_MODULE_CLASS_NAME = "DependencyModule";

    public final static String DEPENDENCY_MODULE_REGISTER_FUNCTION_NAME = "registerModule";

    public final static String DEPENDENCY_MODULE_FULL_NAME = GENERATED_MODULE_PACKAGE + "." + DEPENDENCY_MODULE_CLASS_NAME;


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Dependency.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_11;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);

            try {
                System.out.printf("[%s] Generating dependency module %s%n", AnnotatedDependencyProcessor.class.getSimpleName(), DEPENDENCY_MODULE_FULL_NAME);

                JavaFileObject builderFile = processingEnv.getFiler()
                        .createSourceFile(DEPENDENCY_MODULE_FULL_NAME);

                try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                    out.write(createDependencyModuleClassSource(annotatedElements));
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private static String createDependencyModuleClassSource(Set<? extends Element> elements) {
        List<String> lines = new ArrayList<>();
        lines.add(format("package %s;", GENERATED_MODULE_PACKAGE));
        lines.add("");
        lines.add(format("public class %s {", DEPENDENCY_MODULE_CLASS_NAME));
        lines.add("");
        lines.add(format("    private %s() {}", DEPENDENCY_MODULE_CLASS_NAME));
        lines.add("");
        lines.add(format("    public static void %s(%s resolver) {", DEPENDENCY_MODULE_REGISTER_FUNCTION_NAME, Resolver.class.getCanonicalName()));
        elements.forEach(e -> {
            Dependency dependencyAnnotation = e.getAnnotation(Dependency.class);
            String name = dependencyAnnotation.name();

            System.out.printf("[%s] Registering annotated class %s%n", AnnotatedDependencyProcessor.class.getSimpleName(), e);

            if (name.isEmpty()) {
                lines.add(format("        resolver.register(%s.class);", e));
            } else {
                lines.add(format("        resolver.register(%s.class, \"%s\");", e, name));
            }
        });
        lines.add(   "    }");
        lines.add("");
        lines.add("}");

        return String.join(System.lineSeparator(), lines.toArray(new String[0]));
    }

}
