package pseudopad.core;

/**
 * Interface to abstract error reporting mechanisms.
 * Allows decoupling error detection from error display (console log, red
 * squiggly lines, popups).
 */
public interface ErrorReporter {
    void error(int line, int column, String message);

    void runtimeError(Errors.RuntimeError error);
}
