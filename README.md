# Hypo

[![](https://jitpack.io/v/AlexGaard/hypo.svg)](https://jitpack.io/#AlexGaard/hypo) [![Tests](https://github.com/AlexGaard/hypo/actions/workflows/test.yml/badge.svg?branch=main)](https://github.com/AlexGaard/hypo/actions/workflows/test.yml)

[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=hypo&metric=coverage)](https://sonarcloud.io/summary/new_code?id=hypo) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=hypo&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=hypo) [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=hypo&metric=bugs)](https://sonarcloud.io/summary/new_code?id=hypo) [![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=hypo&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=hypo) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=hypo&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=hypo)

Hypo is a minimalistic and lightweight dependency injection library that gives you full control over how and when your dependencies are loaded.
Hypo has no side effects while resolving dependencies, which makes it very easy to test and verify that the dependency graph is resolved as expected.

To be able to resolve the dependency graph, Hypo uses providers (function that instantiates a dependency).
The providers can recursively invoke other providers, which together makes up the dependency graph.


## Usage

### Basic example

```java
class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(ServiceB serviceB) { this.serviceB = serviceB; }
}

class ServiceB {
    private final ServiceC serviceC;
    public ServiceB(ServiceC serviceC) { this.serviceC = serviceC; }
}

class ServiceC {}
    

Dependencies dependencies = new Resolver()
                .register(ServiceA.class)
                .register(ServiceB.class)
                .register(ServiceC.class)
                .resolve();

ServiceA serviceA = dependencies.get(ServiceA.class); // Returns a singleton of ServiceA

serviceA.doSomething();

ServiceC serviceC = dependencies.create(ServiceC.class); // Creates a new instance of ServiceC

serviceC.doSomethingElse();
```

In the example above 3 dependencies (ServiceA, ServiceB, ServiceC) are first registered with a provider which instantiates the dependency.
When `resolve()` is called, the providers will be invoked in order to build up the dependency graph which in this example looks like this: `ServiceA -> ServiceB -> ServiceC`.

The dependencies returned from `resolve()` can be used as a service locator to get the instantiated dependencies.

### Class scanning
Use the `Dependency` annotation to mark which classes should be registered by Hypo. 

```java
@Dependency
class ServiceA {
    private final ServiceB serviceB;
    public ServiceA(ServiceB serviceB) { this.serviceB = serviceB; }
}

@Dependency
class ServiceB {
    private final ServiceC serviceC;
    public ServiceB(ServiceC serviceC) { this.serviceC = serviceC; }
}

@Dependency
class ServiceC {}
    

Dependencies dependencies = new Resolver()
                .scan("com.example", "xyz.acme") // Register all annotated classes under these paths
                .resolve();
```

### Circular dependencies

If there is a circular dependency present when `resolve()` is called, an exception will be thrown displaying the circular dependency graph.

```java
 Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceA.class, (d) -> new ServiceA(d.get(ServiceB.class)))
                .register(ServiceB.class, (d) -> new ServiceB(d.get(ServiceC.class)))
                .register(ServiceC.class, (d) -> new ServiceC(d.get(ServiceA.class), d.get(ServiceD.class), d.get(Config.class)));

/*
throws CircularDependencyException with the following message:
    Circular dependency detected while initializing ServiceA.
    Dependency chain: ServiceA -> ServiceB -> ServiceC -> ServiceA
*/
resolver.resolve();
```

If you have a circular dependency, then often the best option is to restructure your dependencies, but sometimes a circular dependency is necessary.
There are several ways of resolving these problems, for example:

Initialize with null and use the `onPostInit` callback to set the dependency after initialization.

```java
Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceA.class, (d) -> new ServiceA(d.get(ServiceB.class)))
                .register(ServiceB.class, (d) -> new ServiceB(d.get(ServiceC.class)))
                .register(ServiceC.class,
                        (d) -> new ServiceC(null, d.get(ServiceD.class), d.get(Config.class)),
                        (d, serviceC) -> serviceC.setServiceA(d.get(ServiceA.class))
                );
```

Use `lazyGet` or `lazyCreate` to pass a ```Supplier<ServiceA>``` to *ServiceC*. This supplier must be invoked after `resolve()` has finished,
or else the circular dependency will be re-introduced.

```java
Resolver resolver = new Resolver()
                .register(Config.class, (d) -> new Config())
                .register(ServiceA.class, (d) -> new ServiceA(d.get(ServiceB.class)))
                .register(ServiceB.class, (d) -> new ServiceB(d.get(ServiceC.class)))
                .register(ServiceC.class,(d) -> new ServiceC(d.lazyGet(ServiceA.class), d.get(ServiceD.class), d.get(Config.class)));
```

## Installation

### Add Jitpack repository

Gradle:
```kotlin
repositories {
	maven { setUrl("https://jitpack.io") }
}
```

Maven:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Add dependency
Gradle:
```kotlin
dependencies {
	implementation("com.github.AlexGaard:hypo:LATEST_RELEASE")
}
```

Maven:
```xml
<dependency>
    <groupId>com.github.AlexGaard</groupId>
    <artifactId>hypo</artifactId>
    <version>LATEST_RELEASE</version>
</dependency>
```

The latest release can be found at https://github.com/AlexGaard/hypo/releases.

## Requirements

* JDK 11 or above


## Project name

Hypo, abbreviation of "hypodermic needle", is a medical tool used together with a syringe for injecting or extracting fluids.
