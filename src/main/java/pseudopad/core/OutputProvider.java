package pseudopad.core;

/**
 * Interface to abstract output mechanisms.
 * Allows the interpreter to send output to various destinations (console, GUI,
 * testing buffers)
 * without being coupled to System.out.
 */
public interface OutputProvider {
    void print(String message);
}
