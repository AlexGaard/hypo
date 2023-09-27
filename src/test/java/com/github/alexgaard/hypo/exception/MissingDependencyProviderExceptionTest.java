package com.github.alexgaard.hypo.exception;

import com.github.alexgaard.hypo.Provider;
import com.github.alexgaard.hypo.example.Config;
import com.github.alexgaard.hypo.example.ServiceA;
import com.github.alexgaard.hypo.example.ServiceD;
import com.github.alexgaard.hypo.example.ServiceDImpl;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.github.alexgaard.hypo.DependencyId.id;
import static org.junit.jupiter.api.Assertions.*;

class MissingDependencyProviderExceptionTest {

    @Test
    void shouldReturnCorrectMessage() {
        Map<String, Provider<?>> providers = new HashMap<>();
        providers.put(id(Config.class), (d) -> new Config());
        providers.put(id(ServiceD.class, "test"), (d) -> new ServiceDImpl());

        MissingDependencyProviderException e = new MissingDependencyProviderException(id(ServiceA.class), providers);

        String expectedStr = "Unable to find a registered dependency provider for com.github.alexgaard.hypo.example.ServiceA\n" +
                "Registered providers: com.github.alexgaard.hypo.example.ServiceD@test, com.github.alexgaard.hypo.example.Config";

        assertEquals(expectedStr, e.getMessage());
    }

}