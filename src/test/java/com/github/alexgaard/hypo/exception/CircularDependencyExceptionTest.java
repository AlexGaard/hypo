package com.github.alexgaard.hypo.exception;

import com.github.alexgaard.hypo.example.ServiceA;
import com.github.alexgaard.hypo.example.ServiceB;
import com.github.alexgaard.hypo.example.ServiceC;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CircularDependencyExceptionTest {

    @Test
    public void shouldThrowCircularDependencyException() {
        List<Class<?>> circularDependencies = Arrays.asList(
                ServiceB.class, ServiceC.class, ServiceA.class, ServiceB.class
        );

        CircularDependencyException exception = new CircularDependencyException(circularDependencies);

        String expectedMsg = "Circular dependency detected while initializing com.github.alexgaard.hypo.example.ServiceB.\n"
                + "Dependency chain: com.github.alexgaard.hypo.example.ServiceB -> com.github.alexgaard.hypo.example.ServiceC -> com.github.alexgaard.hypo.example.ServiceA -> com.github.alexgaard.hypo.example.ServiceB";

        assertEquals(exception.getMessage(), expectedMsg);
    }

}
