package pseudopad.editor.terminal;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import pseudopad.settings.SettingsManager;

/**
 * A TextPane that acts as a terminal emulator.
 * It interfaces with a TerminalBackend to send input and receive output.
 * 
 * @author Geger John Paul Gabayeron
 */
public class TerminalPane extends JTextPane {

    private final TerminalBackend backend;
    private int lastPromptPos = 0;
    private boolean clearingTerminal = false; // Flag to allow clear operation to bypass filter

    // Command History
    private final java.util.List<String> commandHistory = new java.util.ArrayList<>();
    private int historyIndex = -1;
    private String tempInputBuffer = ""; // Stores current input when browsing history

    public TerminalPane(TerminalBackend backend) {
        this.backend = backend;

        // Visuals
        setBackground(new Color(30, 30, 30)); // Dark background
        setForeground(new Color(200, 200, 200)); // Light text
        setCaretColor(Color.WHITE);

        // Apply font from settings
        String fontFamily = SettingsManager.getInstance().get(SettingsManager.TERMINAL_FONT_FAMILY);
        int fontSize = SettingsManager.getInstance().get(SettingsManager.TERMINAL_FONT_SIZE);
        setFont(new Font(fontFamily, Font.PLAIN, fontSize));

        // Listen for terminal font settings changes
        SettingsManager.getInstance().addListener(SettingsManager.TERMINAL_FONT_FAMILY,
                (key, oldVal, newVal) -> SwingUtilities.invokeLater(() -> {
                    Font currentFont = getFont();
                    setFont(new Font(newVal, currentFont.getStyle(), currentFont.getSize()));
                }));
        SettingsManager.getInstance().addListener(SettingsManager.TERMINAL_FONT_SIZE,
                (key, oldVal, newVal) -> SwingUtilities.invokeLater(() -> {
                    Font currentFont = getFont();
                    setFont(new Font(currentFont.getFamily(), currentFont.getStyle(), newVal));
                }));

        // Backend Setup
        backend.setOutputListener(this::appendOutput);
        backend.start();

        // Setup context menu for copy/paste
        setupContextMenu();

        // Setup document filter to protect prompt/history from deletion
        setupDocumentFilter();

        // Input Handling
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume(); // Prevent default newline insertion
                    handleEnter();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    // Prevent backspace if at or before prompt, or if selection includes protected
                    // area
                    if (getCaretPosition() <= lastPromptPos || getSelectionStart() < lastPromptPos) {
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    // Prevent delete if selection includes protected area
                    if (getSelectionStart() < lastPromptPos) {
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_A && e.isControlDown()) {
                    // Ctrl+A: Select only the editable input area, not the whole terminal
                    e.consume();
                    setSelectionStart(lastPromptPos);
                    setSelectionEnd(getDocument().getLength());
                } else if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown() && e.isShiftDown()) {
                    // Ctrl+Shift+C = Copy selected text
                    e.consume();
                    copySelectedText();
                } else if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) {
                    // Ctrl+C = Cancel execution
                    e.consume();
                    backend.cancel();
                } else if (e.getKeyCode() == KeyEvent.VK_V && e.isControlDown()) {
                    // Ctrl+V = Paste
                    e.consume();
                    pasteText();
                } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    if (getCaretPosition() <= lastPromptPos) {
                        e.consume();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    e.consume();
                    navigateHistory(-1);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    e.consume();
                    navigateHistory(1);
                } else if (e.getKeyCode() == KeyEvent.VK_HOME) {
                    // Home key goes to start of editable area, not start of document
                    e.consume();
                    setCaretPosition(lastPromptPos);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (getCaretPosition() < lastPromptPos) {
                    setCaretPosition(getDocument().getLength()); // Force caret to end
                }
            }
        });

        // Mouse click handler for clickable links
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    handleLinkClick(e);
                }
            }
        });

        // Mouse motion handler to show hand cursor on links
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursorForLink(e);
            }
        });
    }

    /**
     * Sets up right-click context menu with Copy and Paste options.
     */
    private void setupContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem copyItem = new JMenuItem("Copy (Ctrl+Shift+C)");
        copyItem.addActionListener(e -> copySelectedText());
        popupMenu.add(copyItem);

        JMenuItem pasteItem = new JMenuItem("Paste (Ctrl+V)");
        pasteItem.addActionListener(e -> pasteText());
        popupMenu.add(pasteItem);

        setComponentPopupMenu(popupMenu);
    }

    /**
     * Sets up a document filter to protect prompt and history from modification.
     * This catches any attempt to delete or replace content before lastPromptPos.
     */
    private void setupDocumentFilter() {
        ((javax.swing.text.AbstractDocument) getDocument()).setDocumentFilter(
                new javax.swing.text.DocumentFilter() {
                    @Override
                    public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                        // Allow all removals during clear operation
                        if (clearingTerminal) {
                            super.remove(fb, offset, length);
                            return;
                        }
                        // Only allow removal in the editable area (after lastPromptPos)
                        if (offset >= lastPromptPos) {
                            super.remove(fb, offset, length);
                        } else if (offset + length > lastPromptPos) {
                            // Partial overlap: only remove the part after lastPromptPos
                            int newOffset = lastPromptPos;
                            int newLength = offset + length - lastPromptPos;
                            super.remove(fb, newOffset, newLength);
                        }
                        // If entirely in protected area, do nothing
                    }

                    @Override
                    public void replace(FilterBypass fb, int offset, int length, String text,
                            javax.swing.text.AttributeSet attrs) throws BadLocationException {
                        // Allow all replacements during clear operation
                        if (clearingTerminal) {
                            super.replace(fb, offset, length, text, attrs);
                            return;
                        }
                        // For replace, handle similar to remove for the deletion part
                        if (offset >= lastPromptPos) {
                            super.replace(fb, offset, length, text, attrs);
                        } else if (offset + length > lastPromptPos) {
                            // Partial overlap: adjust to only replace in editable area
                            int newOffset = lastPromptPos;
                            int newLength = offset + length - lastPromptPos;
                            super.replace(fb, newOffset, newLength, text, attrs);
                        } else if (text != null && !text.isEmpty()) {
                            // Entirely in protected area but inserting text: insert at lastPromptPos
                            super.insertString(fb, lastPromptPos, text, attrs);
                        }
                    }

                    @Override
                    public void insertString(FilterBypass fb, int offset, String string,
                            javax.swing.text.AttributeSet attr) throws BadLocationException {
                        // Allow insertion anywhere (the caret is already constrained)
                        // But if trying to insert before prompt, redirect to end
                        if (offset < lastPromptPos) {
                            offset = fb.getDocument().getLength();
                        }
                        super.insertString(fb, offset, string, attr);
                    }
                });
    }

    /**
     * Copies selected text to clipboard.
     */
    private void copySelectedText() {
        String selected = getSelectedText();
        if (selected != null && !selected.isEmpty()) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(selected), null);
        }
    }

    /**
     * Pastes text from clipboard at current input position.
     */
    private void pasteText() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String text = (String) clipboard.getData(DataFlavor.stringFlavor);
            if (text != null) {
                // Insert at caret position (but only in input area)
                int pos = Math.max(getCaretPosition(), lastPromptPos);
                getDocument().insertString(pos, text, null);
            }
        } catch (Exception ex) {
            // Ignore clipboard errors
        }
    }

    /**
     * Pattern to match file:line:col references (e.g., script.pseudo:2:12)
     */
    private static final Pattern LINK_PATTERN = Pattern.compile("([\\w.]+\\.pseudo):(\\d+)(?::(\\d+))?");

    /**
     * Handles click on a file:line link to navigate to that location.
     */
    private void handleLinkClick(MouseEvent e) {
        try {
            int offset = viewToModel2D(e.getPoint());
            String text = getDocument().getText(0, getDocument().getLength());

            // Find link at click position
            Matcher matcher = LINK_PATTERN.matcher(text);
            while (matcher.find()) {
                if (offset >= matcher.start() && offset <= matcher.end()) {
                    String fileName = matcher.group(1);
                    int line = Integer.parseInt(matcher.group(2));
                    int col = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 1;

                    // Navigate to file location
                    navigateToFile(fileName, line, col);
                    return;
                }
            }
        } catch (BadLocationException ex) {
            // Ignore
        }
    }

    /**
     * Updates cursor to hand cursor when hovering over a link.
     */
    private void updateCursorForLink(MouseEvent e) {
        try {
            int offset = viewToModel2D(e.getPoint());
            String text = getDocument().getText(0, getDocument().getLength());

            Matcher matcher = LINK_PATTERN.matcher(text);
            while (matcher.find()) {
                if (offset >= matcher.start() && offset <= matcher.end()) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
            }
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        } catch (BadLocationException ex) {
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        }
    }

    /**
     * Navigates to a specific line and column in the editor.
     * Communicates with MainFrame to open/focus the file and position the caret.
     */
    private void navigateToFile(String fileName, int line, int col) {
        pseudopad.app.MainFrame mainFrame = pseudopad.app.MainFrame.getInstance();
        if (mainFrame != null) {
            mainFrame.navigateToLine(line, col);
        }
    }

    private void handleEnter() {
        try {
            int len = getDocument().getLength();
            String input = getText(lastPromptPos, len - lastPromptPos);

            // Add newline after the typed command
            getDocument().insertString(len, "\n", null);

            // Update lastPromptPos to after the newline - command output will appear here
            lastPromptPos = getDocument().getLength();

            // Add to history if not empty
            if (!input.trim().isEmpty()) {
                commandHistory.add(input);
                historyIndex = -1; // Reset history index
                tempInputBuffer = "";
            }

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
                    clearingTerminal = true;
                    try {
                        setText("");
                        lastPromptPos = 0;
                    } finally {
                        clearingTerminal = false;
                    }
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

                    // 4. Update caret position
                    setCaretPosition(doc.getLength());

                    // 5. Update lastPromptPos to the end of the document
                    // This ensures that any output from the program (prompts, logs) becomes
                    // "read-only"
                    // and the user input starts fresh after it.
                    lastPromptPos = doc.getLength();
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Parses ANSI escape codes and appends text with appropriate colors.
     * Also detects clickable links (file:line:col) and underlines them.
     * Supports: \u001B[31m (red), \u001B[32m (green), \u001B[33m (yellow),
     * \u001B[36m (cyan), \u001B[0m (reset)
     */
    private void appendWithAnsiColors(StyledDocument doc, String text) throws BadLocationException {
        Color currentColor = getForeground();
        Color RED = new Color(255, 100, 100); // Bright red for errors
        Color GREEN = new Color(100, 255, 100); // Green for success
        Color YELLOW = new Color(255, 255, 100); // Yellow for warnings
        Color CYAN = new Color(100, 200, 255); // Cyan for info
        Color LINK_COLOR = new Color(100, 150, 255); // Blue for links
        Color DEFAULT = getForeground();

        // First, strip ANSI codes and build plain text with color info
        Pattern ansiPattern = Pattern.compile("\u001B\\[(\\d+)m");
        Matcher ansiMatcher = ansiPattern.matcher(text);

        StringBuilder plainText = new StringBuilder();
        java.util.List<int[]> colorRanges = new java.util.ArrayList<>(); // [start, end, colorCode]

        int lastEnd = 0;
        int currentColorCode = 0; // 0 = default

        while (ansiMatcher.find()) {
            if (ansiMatcher.start() > lastEnd) {
                int start = plainText.length();
                plainText.append(text.substring(lastEnd, ansiMatcher.start()));
                int end = plainText.length();
                colorRanges.add(new int[] { start, end, currentColorCode });
            }

            currentColorCode = Integer.parseInt(ansiMatcher.group(1));
            lastEnd = ansiMatcher.end();
        }

        // Append remaining text
        if (lastEnd < text.length()) {
            int start = plainText.length();
            plainText.append(text.substring(lastEnd));
            int end = plainText.length();
            colorRanges.add(new int[] { start, end, currentColorCode });
        }

        String cleanText = plainText.toString();

        // Find all link positions in clean text
        java.util.Set<int[]> linkRanges = new java.util.HashSet<>();
        Matcher linkMatcher = LINK_PATTERN.matcher(cleanText);
        while (linkMatcher.find()) {
            linkRanges.add(new int[] { linkMatcher.start(), linkMatcher.end() });
        }

        // Now insert text with colors and underlines
        for (int[] range : colorRanges) {
            int start = range[0];
            int end = range[1];
            int colorCode = range[2];

            String segment = cleanText.substring(start, end);

            // Break segment into parts - link and non-link portions
            int segmentPos = 0;
            for (int i = 0; i < segment.length();) {
                int globalPos = start + i;

                // Check if we're inside a link
                int[] linkRange = null;
                for (int[] lr : linkRanges) {
                    if (globalPos >= lr[0] && globalPos < lr[1]) {
                        linkRange = lr;
                        break;
                    }
                }

                if (linkRange != null) {
                    // Insert non-link part before this link
                    if (i > segmentPos) {
                        String nonLink = segment.substring(segmentPos, i);
                        SimpleAttributeSet attrs = new SimpleAttributeSet();
                        StyleConstants.setForeground(attrs,
                                getColorForCode(colorCode, DEFAULT, RED, GREEN, YELLOW, CYAN));
                        doc.insertString(doc.getLength(), nonLink, attrs);
                    }

                    // Insert link part with underline
                    int linkStart = Math.max(linkRange[0], start) - start;
                    int linkEnd = Math.min(linkRange[1], end) - start;
                    String linkText = segment.substring(linkStart, linkEnd);

                    SimpleAttributeSet linkAttrs = new SimpleAttributeSet();
                    StyleConstants.setForeground(linkAttrs, LINK_COLOR);
                    StyleConstants.setUnderline(linkAttrs, true);
                    doc.insertString(doc.getLength(), linkText, linkAttrs);

                    i = linkEnd;
                    segmentPos = linkEnd;
                } else {
                    i++;
                }
            }

            // Insert remaining non-link part
            if (segmentPos < segment.length()) {
                String remaining = segment.substring(segmentPos);
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, getColorForCode(colorCode, DEFAULT, RED, GREEN, YELLOW, CYAN));
                doc.insertString(doc.getLength(), remaining, attrs);
            }
        }
    }

    /**
     * Returns the color for a given ANSI color code.
     */
    private Color getColorForCode(int code, Color def, Color red, Color green, Color yellow, Color cyan) {
        return switch (code) {
            case 31 -> red;
            case 32 -> green;
            case 33 -> yellow;
            case 36 -> cyan;
            default -> def;
        };
    }

    private void navigateHistory(int direction) {
        // 0. Save current input if we are starting navigation
        if (historyIndex == -1) {
            try {
                int len = getDocument().getLength();
                if (len > lastPromptPos) {
                    tempInputBuffer = getText(lastPromptPos, len - lastPromptPos);
                } else {
                    tempInputBuffer = "";
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        // 1. Update Index
        int newIndex = historyIndex + direction;

        // Clamp logic
        // If going UP (direction -1)
        // - If index < 0, stay at 0 (oldest command)
        // - But if history is empty, do nothing
        // If going DOWN (direction 1)
        // - If index >= size, go to -1 (empty/temp buffer)

        if (commandHistory.isEmpty())
            return;

        if (direction < 0) { // UP
            if (historyIndex == -1) {
                newIndex = commandHistory.size() - 1;
            } else {
                newIndex = Math.max(0, newIndex);
            }
        } else { // DOWN
            if (newIndex >= commandHistory.size()) {
                newIndex = -1;
            }
        }

        if (newIndex == historyIndex)
            return; // No change

        historyIndex = newIndex;

        // 2. Update Input Area
        try {
            // Remove current input
            int len = getDocument().getLength();
            if (len > lastPromptPos) {
                getDocument().remove(lastPromptPos, len - lastPromptPos);
            }

            // Insert new text
            String textToInsert;
            if (historyIndex == -1) {
                textToInsert = tempInputBuffer;
            } else {
                textToInsert = commandHistory.get(historyIndex);
            }

            getDocument().insertString(lastPromptPos, textToInsert, null);

        } catch (BadLocationException e) {
            e.printStackTrace();
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
