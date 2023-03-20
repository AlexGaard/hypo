package com.github.alexgaard.hypo.exception;

import com.github.alexgaard.hypo.example.ServiceA;
import com.github.alexgaard.hypo.example.ServiceB;
import com.github.alexgaard.hypo.example.ServiceC;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CircularDependencyExceptionTest {

    @Test
    public void shouldThrowCircularDependencyException() {
        var circularDependencies = Arrays.asList(
                ServiceB.class, ServiceC.class, ServiceA.class, ServiceB.class
        );

        CircularDependencyException exception = new CircularDependencyException(circularDependencies);

        var expectedMsg = """
                Circular dependency detected while initializing com.github.alexgaard.hypo.example.ServiceB.
                Dependency chain: com.github.alexgaard.hypo.example.ServiceB -> com.github.alexgaard.hypo.example.ServiceC -> com.github.alexgaard.hypo.example.ServiceA -> com.github.alexgaard.hypo.example.ServiceB
                """.trim();

        assertEquals(exception.getMessage(), expectedMsg);
    }

}
