package pseudopad.ui.components.terminal;

import java.util.function.Consumer;

/**
 * A lightweight terminal backend that runs purely in Java.
 * Useful for testing or as a base for custom interpreters.
 * 
 * @author Geger John Paul Gabayeron
 */
public class SimpleTerminalBackend implements TerminalBackend {

    private Consumer<String> outputListener;
    private boolean isRunning = false;
    private String projectName = "PseudoPad";
    private java.util.function.Supplier<String> codeProvider;

    @Override
    public void sendInput(String input) {
        if (!isRunning)
            return;

        // Echo the input (optional, depending on how TerminalPane handles it,
        // but usually backends might echo or the UI handles it.
        // Let's assume UI handles local echo for now, or we send it back as output.)
        // For a "terminal" feel, usually the backend output includes the result.

        processCommand(input);
    }

    private void processCommand(String input) {
        if (outputListener == null)
            return;

        String command = input.trim();

        // Simulate processing
        if (command.isEmpty()) {
            outputListener.accept("\n" + getPrompt());
            return;
        }

        StringBuilder response = new StringBuilder();
        response.append("\n"); // Newline after the user's input line

        switch (command) {
            case "help":
                response.append("Available commands:\n");
                response.append("  help    - Show this help\n");
                response.append("  version - Show version\n");
                response.append("  clear   - Clear screen\n");
                response.append("  run     - Run program (placeholder)\n");
                break;
            case "version":
                response.append("PseudoPad Terminal v1.0\n");
                break;
            case "clear":
                outputListener.accept("\f");
                response.setLength(0); // Clear buffer so we don't append extra newlines
                break;
            case "run":
                runProgram(response);
                break;
            default:
                response.append("Unknown command: " + command + "\n");
                break;
        }

        response.append(getPrompt()); // Prompt
        outputListener.accept(response.toString());
    }

    @Override
    public void setOutputListener(Consumer<String> listener) {
        this.outputListener = listener;
    }

    @Override
    public void start() {
        isRunning = true;
        if (outputListener != null) {
            outputListener.accept("PseudoPad Internal Terminal\nType 'help' for commands.\n\n" + getPrompt());
        }
    }

    private void runProgram(StringBuilder response) {
        response.append("Running program...\n");

        String code = null;
        if (codeProvider != null) {
            code = codeProvider.get();
        }

        if (code == null || code.trim().isEmpty()) {
            response.append("No code to execute.\n");
        } else {
            // Placeholder for custom interpreter execution
            // MIKOOOOOOOOOOOOOOOOOOOOOOOOOO
            // e.g., myInterpreter.execute(code);
        }

        response.append("[Program finished]\n");
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;
        // Optional: announce change or just update future prompts
    }

    @Override
    public void setCodeProvider(java.util.function.Supplier<String> provider) {
        this.codeProvider = provider;
    }

    private String getPrompt() {
        return (projectName != null ? projectName : "") + "> ";
    }
}
