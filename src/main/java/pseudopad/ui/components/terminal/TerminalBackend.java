package pseudopad.ui.components.terminal;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Interface for terminal backends.
 * Allows switching between system shell, custom interpreters, etc.
 * 
 * @author Geger John Paul Gabayeron
 */
public interface TerminalBackend {
    /**
     * Sends input string to the backend.
     * 
     * @param input The command or text entered by the user.
     */
    void sendInput(String input);

    /**
     * Sets the listener for output from the backend.
     * 
     * @param listener A consumer that handles output strings.
     */
    void setOutputListener(Consumer<String> listener);

    /**
     * Starts the backend (e.g., starts the process or initializes the interpreter).
     */
    void start();

    /**
     * Stops the backend (e.g., kills the process).
     */
    void stop();

    /**
     * Sets the project name to be displayed in the prompt.
     * 
     * @param projectName The name of the current project.
     */
    void setProjectName(String projectName);

    /**
     * Sets the provider for retrieving the current code content.
     * 
     * @param provider A supplier that returns the current code as a String.
     */
    void setCodeProvider(Supplier<String> provider);
}
