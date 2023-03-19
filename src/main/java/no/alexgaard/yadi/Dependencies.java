package no.alexgaard.yadi;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Dependencies {

    private final Map<Class, Object> dependencies;

    public Dependencies(Map<Class, Object> dependencies) {
        this.dependencies = dependencies;
    }

    public <T> T get(Class<T> clazz) {
        Object dependency = dependencies.get(clazz);

        if (dependency == null) {
            throw new IllegalStateException(
                    "Unable to get dependency %s because it has not been registered."
                            .formatted(clazz.getCanonicalName())
            );
        }

//        if (!dependency.getClass().equals(clazz)) {
//            throw new IllegalStateException(
//                    "Unable to get dependency %s because it has not been registered."
//                            .formatted(clazz.getCanonicalName())
//            );
//        }

        return (T) dependency;
    }

    public <T> Optional<T> getOptional(Class<T> clazz) {
        return (Optional<T>) Optional.ofNullable(dependencies.get(clazz));
    }

    @Override
    public String toString() {
        return "Dependencies{" +
                "dependencies=" + dependencies +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependencies that = (Dependencies) o;
        return dependencies.equals(that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencies);
    }

}
