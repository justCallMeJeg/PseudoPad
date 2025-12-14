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
                    SimpleAttributeSet attrs = new SimpleAttributeSet();
                    StyleConstants.setForeground(attrs, getForeground());

                    // 2. Append new text
                    doc.insertString(doc.getLength(), text, attrs);

                    // 3. Infinite Scroll Trap (Truncation)
                    // Efficient Rendering: Remove in chunks (hysteresis) to avoid constant
                    // resizing.
                    // If content exceeds limit, remove old content down to 80% of usage.
                    int MAX_CHARS = 10000;
                    int length = doc.getLength();
                    if (length > MAX_CHARS) {
                        int targetLength = (int) (MAX_CHARS * 0.8);
                        int charsToRemove = length - targetLength;

                        // Don't cut in the middle of a prompt if possible, but for now simple cut.
                        doc.remove(0, charsToRemove);

                        // Adjust prompt position as it shifts with deletion
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
