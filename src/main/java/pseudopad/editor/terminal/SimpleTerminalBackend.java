package pseudopad.editor.terminal;

import javax.swing.*;

import pseudopad.core.PseudoRunner;
import pseudopad.core.Interpreter;

import java.util.function.Consumer;
import java.util.function.Supplier;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
    private Thread executionThread; // Track the running program thread

    // Blocking Queue for Inline Input
    private final BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>();
    private volatile boolean isWaitingForInput = false;

    @Override
    public void sendInput(String input) {
        if (!isRunning)
            return;

        // If the running program is waiting for input, feed it into the queue
        if (isWaitingForInput && executionThread != null && executionThread.isAlive()) {
            inputQueue.offer(input);
            return;
        }

        processCommand(input);
    }

    private void processCommand(String input) {
        if (outputListener == null)
            return;

        String command = input.trim();

        // Empty command - just show new prompt
        if (command.isEmpty()) {
            outputListener.accept(getPrompt());
            return;
        }

        switch (command) {
            case "help":
                outputListener.accept("Available commands:\n"
                        + "  help    - Show this help\n"
                        + "  version - Show version\n"
                        + "  clear   - Clear screen\n"
                        + "  run     - Run program\n"
                        + getPrompt());
                return;
            case "version":
                outputListener.accept("PseudoPad Terminal v1.0\n" + getPrompt());
                return;
            case "clear":
                // Clear screen with form feed, then show prompt
                outputListener.accept("\f" + getPrompt());
                return;
            case "run":
                runProgram();
                return;
            case "stop":
                cancel();
                return;
            default:
                outputListener.accept("Unknown command: " + command + "\n" + getPrompt());
                return;
        }
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

    private void runProgram() {
        // 1. Get the code
        String code = null;
        if (codeProvider != null) {
            code = codeProvider.get();
        }

        if (code == null || code.trim().isEmpty()) {
            outputListener.accept("\nNo code to execute.\n" + getPrompt());
            return;
        }

        if (executionThread != null && executionThread.isAlive()) {
            outputListener.accept("\nA program is already running. Type 'stop' to terminate it.\n" + getPrompt());
            return;
        }

        // 2. Run in a separate thread so the UI doesn't freeze
        final String sourceCode = code;

        executionThread = new Thread(() -> {
            try {
                // 3. Define Input Provider
                Interpreter.InputProvider inputProvider = new Interpreter.InputProvider() {
                    @Override
                    public String read(String prompt) {
                        try {
                            // Print the prompt to the terminal
                            if (outputListener != null) {
                                outputListener.accept(prompt);
                            }

                            // Wait for input
                            isWaitingForInput = true;
                            // Clear any previous stray input
                            inputQueue.clear();

                            String input = inputQueue.take(); // Blocks until input available
                            return input;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Input interrupted");
                        } finally {
                            isWaitingForInput = false;
                        }
                    }

                    @Override
                    public String readPopup(String prompt) {
                        return JOptionPane.showInputDialog(null, prompt, "Input", JOptionPane.QUESTION_MESSAGE);
                    }
                };

                // 4. Run directly - output goes straight to terminal, no extra prompt
                PseudoRunner.run(sourceCode, inputProvider, (msg) -> {
                    if (outputListener != null)
                        outputListener.accept(msg);
                });
            } finally {
                // 5. Finished - add prompt on new line
                executionThread = null; // Clear reference
                isWaitingForInput = false;
                if (outputListener != null) {
                    outputListener.accept(getPrompt());
                }
            }
        });
        executionThread.start();
    }

    @Override
    public void stop() {
        isRunning = false;
        cancel(); // Also cancel any running command
    }

    @Override
    public void cancel() {
        if (executionThread != null && executionThread.isAlive()) {
            executionThread.interrupt();
            if (outputListener != null) {
                outputListener.accept("\n>> Stopping...\n" + getPrompt());
            }
        }
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
