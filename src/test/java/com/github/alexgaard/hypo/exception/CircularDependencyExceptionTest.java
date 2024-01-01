package com.github.alexgaard.hypo.exception;

import com.github.alexgaard.hypo.example.ServiceA;
import com.github.alexgaard.hypo.example.ServiceB;
import com.github.alexgaard.hypo.example.ServiceC;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class CircularDependencyExceptionTest {

    @Test
    void shouldThrowIfDependencyChainIsEmpty() {
        List<String> circularDependencies = Collections.emptyList();

        assertThrowsExactly(IllegalArgumentException.class, () -> {
            new CircularDependencyException(circularDependencies);
        });
    }

    @Test
    void shouldCreateExpectedMessage() {
        List<String> circularDependencies = Stream.of(
                ServiceB.class, ServiceC.class, ServiceA.class, ServiceB.class
        ).map(Class::getCanonicalName).collect(Collectors.toList());

        CircularDependencyException exception = new CircularDependencyException(circularDependencies);

        String expectedMsg = "Circular dependency detected while initializing com.github.alexgaard.hypo.example.ServiceB.\n"
                + "Dependency chain: com.github.alexgaard.hypo.example.ServiceB -> com.github.alexgaard.hypo.example.ServiceC -> com.github.alexgaard.hypo.example.ServiceA -> com.github.alexgaard.hypo.example.ServiceB";

        assertEquals(expectedMsg, exception.getMessage().replaceAll("\r\n", "\n"));
    }

}
