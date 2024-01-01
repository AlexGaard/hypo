package com.github.alexgaard.hypo.exception;

public class ExceptionUtil {

    private ExceptionUtil() {}

    /**
     * Uses template type erasure to trick the compiler into removing checking of exception. The compiler
     * treats E as RuntimeException, meaning that softenException doesn't need to declare it,
     * but the runtime treats E as Exception (because of type erasure), which avoids
     * {@link ClassCastException}.
     */
    public static <T extends Throwable> T softenException(Throwable t) throws T {
        if (t == null) throw new RuntimeException();
        //noinspection unchecked
        throw (T) t;
    }

}
