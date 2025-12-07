package pseudopad.editor.terminal;

import javax.swing.*;

import pseudopad.core.PseudoRunner;

import java.util.function.Consumer;
import java.util.function.Supplier;

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
    private Supplier<String> codeProvider;

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

    // private void runProgram(StringBuilder response) {
    // response.append("Running program...\n");
    //
    // String code = null;
    // if (codeProvider != null) {
    // code = codeProvider.get();
    // }
    //
    // if (code == null || code.trim().isEmpty()) {
    // response.append("No code to execute.\n");
    // } else {
    // // Placeholder for custom interpreter execution
    // // MIKOOOOOOOOOOOOOOOOOOOOOOOOOO
    // // e.g., myInterpreter.execute(code);
    // }
    //
    // response.append("[Program finished]\n");
    // }

    private void runProgram(StringBuilder response) {
        // 1. Get the code
        String code = null;
        if (codeProvider != null) {
            code = codeProvider.get();
        }

        if (code == null || code.trim().isEmpty()) {
            response.append("No code to execute.\n");
            return;
        }

        response.append("Running...\n------------------------\n");

        // 2. Run in a separate thread so the UI doesn't freeze
        // We need a final copy of the code for the thread
        final String sourceCode = code;

        new Thread(() -> {
            // Buffer for batching output to prevent flooding the Event Dispatch Thread
            StringBuilder outputBuffer = new StringBuilder();
            long[] lastFlushTime = { System.currentTimeMillis() };

            // 3. Define how 'input()' works (Popup Dialog)
            pseudopad.core.Errors.CompilationResult result = PseudoRunner.run(sourceCode, (prompt) -> {
                return JOptionPane.showInputDialog(null, prompt, "Input", JOptionPane.QUESTION_MESSAGE);
            }, (text) -> {
                // 4. Send output back to the terminal with batching
                outputBuffer.append(text);

                long now = System.currentTimeMillis();
                // Flush if > 50ms passed or buffer is getting large (> 1KB)
                if (now - lastFlushTime[0] > 50 || outputBuffer.length() > 1024) {
                    if (outputListener != null) {
                        outputListener.accept(outputBuffer.toString());
                    }
                    outputBuffer.setLength(0);
                    lastFlushTime[0] = now;
                }
            });

            // 5. Update Problems View
            SwingUtilities.invokeLater(() -> {
                if (pseudopad.app.MainFrame.getInstance() != null) {
                    pseudopad.ui.MainLayout layout = (pseudopad.ui.MainLayout) pseudopad.app.MainFrame.getInstance()
                            .getContentPane();
                    if (layout.getProblemsPanel() != null) {
                        // Try to get the file object if possible
                        java.io.File activeFile = null;
                        String activePath = pseudopad.app.MainFrame.getInstance().getEditorTabbedPane().getActiveFile();
                        if (activePath != null) {
                            activeFile = new java.io.File(activePath);
                        }
                        layout.getProblemsPanel().updateErrors(activeFile, result.errors);
                    }
                }
            });

            // Flush any remaining output
            if (outputBuffer.length() > 0 && outputListener != null) {
                outputListener.accept(outputBuffer.toString());
            }

            // Send prompt after execution finishes
            if (outputListener != null) {
                outputListener.accept("\n" + getPrompt());
            }
        }).start();

        // Clear the immediate response so we don't print a prompt twice
        // (The thread prints the prompt when finished)
        response.setLength(0);
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
