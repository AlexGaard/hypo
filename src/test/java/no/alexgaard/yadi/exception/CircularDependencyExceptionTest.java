package no.alexgaard.yadi.exception;

import no.alexgaard.yadi.Resolver;
import no.alexgaard.yadi.example.*;
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
                Circular dependency detected while initializing no.alexgaard.yadi.example.ServiceB.
                Dependency chain: no.alexgaard.yadi.example.ServiceB -> no.alexgaard.yadi.example.ServiceC -> no.alexgaard.yadi.example.ServiceA -> no.alexgaard.yadi.example.ServiceB
                """.trim();

        assertEquals(exception.getMessage(), expectedMsg);
    }

}
