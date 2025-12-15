package pseudopad.editor.terminal;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * A TextPane that acts as a terminal emulator.
 * It interfaces with a TerminalBackend to send input and receive output.
 * 
 * @author Geger John Paul Gabayeron
 */
public class TerminalPane extends JTextPane {

    private final TerminalBackend backend;
    private int lastPromptPos = 0;

    public TerminalPane(TerminalBackend backend) {
        this.backend = backend;

        // Visuals
        setBackground(new Color(30, 30, 30)); // Dark background
        setForeground(new Color(200, 200, 200)); // Light text
        setCaretColor(Color.WHITE);
        setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Backend Setup
        backend.setOutputListener(this::appendOutput);
        backend.start();

        // Input Handling
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume(); // Prevent default newline insertion
                    handleEnter();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    if (getCaretPosition() <= lastPromptPos) {
                        e.consume(); // Prevent deleting prompt/history
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) {
                    e.consume();
                    backend.cancel();
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    if (getCaretPosition() <= lastPromptPos) {
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    e.consume(); // Disable history navigation for now
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (getCaretPosition() < lastPromptPos) {
                    setCaretPosition(getDocument().getLength()); // Force caret to end
                }
            }
        });
    }

    private void handleEnter() {
        try {
            int len = getDocument().getLength();
            String input = getText(lastPromptPos, len - lastPromptPos);

            // Append newline locally for visual feedback if backend doesn't echo
            // immediately
            // But usually backend handles the response.
            // For SimpleBackend, we just send it.

            backend.sendInput(input);

        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private final StringBuilder outputBuffer = new StringBuilder();
    private javax.swing.Timer outputTimer;

    public void appendOutput(String rawText) {
        synchronized (outputBuffer) {
            outputBuffer.append(rawText);
        }

        if (outputTimer == null) {
            outputTimer = new javax.swing.Timer(50, e -> flushOutput());
            outputTimer.setRepeats(false);
            outputTimer.start();
        } else if (!outputTimer.isRunning()) {
            outputTimer.restart();
        }
    }

    private void flushOutput() {
        String textToAppend;
        synchronized (outputBuffer) {
            if (outputBuffer.length() == 0)
                return;
            textToAppend = outputBuffer.toString();
            outputBuffer.setLength(0);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                String text = textToAppend;
                StyledDocument doc = getStyledDocument();

                // 1. Handle Clear Screen Protocol (\f)
                if (text.contains("\f")) {
                    setText("");
                    lastPromptPos = 0;
                    text = text.substring(text.lastIndexOf("\f") + 1);
                }

                if (!text.isEmpty()) {
                    // 2. Parse and render text with ANSI color codes
                    appendWithAnsiColors(doc, text);

                    // 3. Infinite Scroll Trap (Truncation)
                    int MAX_CHARS = 10000;
                    int length = doc.getLength();
                    if (length > MAX_CHARS) {
                        int targetLength = (int) (MAX_CHARS * 0.8);
                        int charsToRemove = length - targetLength;
                        doc.remove(0, charsToRemove);
                        lastPromptPos = Math.max(0, lastPromptPos - charsToRemove);
                    }

                    // 4. Update caret and prompt position
                    setCaretPosition(doc.getLength());
                    lastPromptPos = doc.getLength();
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Parses ANSI escape codes and appends text with appropriate colors.
     * Supports: \u001B[31m (red), \u001B[32m (green), \u001B[33m (yellow),
     * \u001B[36m (cyan), \u001B[0m (reset)
     */
    private void appendWithAnsiColors(StyledDocument doc, String text) throws BadLocationException {
        Color currentColor = getForeground();
        Color RED = new Color(255, 100, 100); // Bright red for errors
        Color GREEN = new Color(100, 255, 100); // Green for success
        Color YELLOW = new Color(255, 255, 100); // Yellow for warnings
        Color CYAN = new Color(100, 200, 255); // Cyan for info
        Color DEFAULT = getForeground();

        // Regex to match ANSI escape codes: \u001B[XXm
        java.util.regex.Pattern ansiPattern = java.util.regex.Pattern.compile("\u001B\\[(\\d+)m");
        java.util.regex.Matcher matcher = ansiPattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            // Append text before the escape code with current color
            if (matcher.start() > lastEnd) {
                String segment = text.substring(lastEnd, matcher.start());
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, currentColor);
                doc.insertString(doc.getLength(), segment, attrs);
            }

            // Parse color code and update current color
            int code = Integer.parseInt(matcher.group(1));
            switch (code) {
                case 0 -> currentColor = DEFAULT; // Reset
                case 31 -> currentColor = RED; // Red
                case 32 -> currentColor = GREEN; // Green
                case 33 -> currentColor = YELLOW; // Yellow
                case 36 -> currentColor = CYAN; // Cyan
            }

            lastEnd = matcher.end();
        }

        // Append remaining text after last escape code
        if (lastEnd < text.length()) {
            String segment = text.substring(lastEnd);
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setForeground(attrs, currentColor);
            doc.insertString(doc.getLength(), segment, attrs);
        }
    }

    public void runCommand(String command) {
        if (backend != null) {
            backend.sendInput(command);
        }
    }

    public void setProjectName(String projectName) {
        if (backend != null) {
            backend.setProjectName(projectName);
        }
    }

    public void shutdown() {
        if (backend != null) {
            backend.stop();
        }
    }
}
