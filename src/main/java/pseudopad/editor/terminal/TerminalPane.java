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

    public void appendOutput(String rawText) {
        SwingUtilities.invokeLater(() -> {
            String text = rawText;
            try {
                if (text.contains("\f")) {
                    setText("");
                    lastPromptPos = 0;
                    // Append everything after the last \f
                    text = text.substring(text.lastIndexOf("\f") + 1);
                }

                if (!text.isEmpty()) {
                    StyledDocument doc = getStyledDocument();
                    SimpleAttributeSet attrs = new SimpleAttributeSet();
                    StyleConstants.setForeground(attrs, getForeground());

                    doc.insertString(doc.getLength(), text, attrs);
                    setCaretPosition(doc.getLength());
                    lastPromptPos = doc.getLength(); // Update prompt position
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
