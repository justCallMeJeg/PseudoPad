package pseudopad.editor.completion;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;

/**
 * Handles the display and logic of the auto-completion popup.
 */
public class AutoCompletion {

    private final JTextComponent textComponent;
    private final CompletionProvider provider;
    private JWindow popup;
    private JList<CompletionItem> list;
    private boolean isShowing = false;

    private boolean isInserting = false;

    private DocumentListener documentListener;

    // Static reference for checking popup visibility from outside
    private static AutoCompletion activeInstance = null;

    // Flag to track when completion handled an Enter key
    private static boolean lastEnterHandledByCompletion = false;

    /**
     * Check if the completion popup is currently visible.
     */
    public static boolean isPopupVisible() {
        return activeInstance != null && activeInstance.isShowing;
    }

    /**
     * Check if completion recently handled an Enter key (resets flag after check).
     */
    public static boolean wasEnterHandledByCompletion() {
        boolean result = lastEnterHandledByCompletion;
        lastEnterHandledByCompletion = false;
        return result;
    }

    public AutoCompletion(JTextComponent textComponent, CompletionProvider provider) {
        this.textComponent = textComponent;
        this.provider = provider;
        activeInstance = this;
        initPopup();
        initListeners();
    }

    private void initPopup() {
        popup = new JWindow(SwingUtilities.getWindowAncestor(textComponent));
        popup.setType(JWindow.Type.POPUP);
        popup.setFocusableWindowState(false); // CRITICAL: Don't steal focus

        list = new JList<>();
        list.setFocusable(false); // List itself shouldn't take focus either
        list.setBackground(new Color(40, 44, 52)); // Dark theme default, should make configurable
        list.setForeground(Color.WHITE);
        list.setSelectionBackground(new Color(75, 110, 175));
        list.setSelectionForeground(Color.WHITE);
        list.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Handle mouse click on list
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertSelection();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        popup.add(scroll, BorderLayout.CENTER);
    }

    private void initListeners() {
        // Document Listener to update suggestions as user types
        documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!isInserting)
                    SwingUtilities.invokeLater(() -> checkCompletions());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!isInserting)
                    SwingUtilities.invokeLater(() -> checkCompletions());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                /* Ignore attributes */ }
        };
        textComponent.getDocument().addDocumentListener(documentListener);

        // Caret Listener to close popup if moved away
        textComponent.addCaretListener(e -> {
            if (!isInserting && isShowing) {
                SwingUtilities.invokeLater(() -> checkCompletions());
            }
        });

        // Focus Listener to hide popup when switching tabs/files
        textComponent.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                hidePopup();
            }
        });

        // Key Listener to navigate list
        textComponent.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isShowing)
                    return;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        moveSelection(1);
                        e.consume();
                        break;
                    case KeyEvent.VK_UP:
                        moveSelection(-1);
                        e.consume();
                        break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_TAB:
                        lastEnterHandledByCompletion = true; // Track that completion handled Enter
                        insertSelection();
                        e.consume();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        hidePopup();
                        e.consume();
                        break;
                }
            }
        });
    }

    private void checkCompletions() {
        try {
            int caret = textComponent.getCaretPosition();
            if (caret == 0) {
                hidePopup();
                return;
            }

            // Check previous character to ensure we are typing a word or dot
            char prevChar = textComponent.getText(caret - 1, 1).charAt(0);
            if (!Character.isLetterOrDigit(prevChar) && prevChar != '.') {
                hidePopup();
                return;
            }
        } catch (BadLocationException e) {
            hidePopup();
            return;
        }

        List<CompletionItem> allCompletions = provider.getCompletions(textComponent);
        if (allCompletions.isEmpty()) {
            hidePopup();
            return;
        }

        // Filter based on currently typed word
        String prefix = getWordAtCaret();

        // Check if we just typed a '.' for dot-completion
        boolean isDotCompletion = false;
        try {
            int caret = textComponent.getCaretPosition();
            if (caret > 0) {
                char prevChar = textComponent.getText(caret - 1, 1).charAt(0);
                isDotCompletion = (prevChar == '.');
            }
        } catch (BadLocationException e) {
            // Ignore
        }

        List<CompletionItem> filtered = new ArrayList<>();

        // For dot-completion, show all items; otherwise filter by prefix
        for (CompletionItem item : allCompletions) {
            if (isDotCompletion || prefix.isEmpty() ||
                    item.getLabel().toLowerCase().startsWith(prefix.toLowerCase())) {
                filtered.add(item);
            }
        }

        System.out.println("[DEBUG] prefix='" + prefix + "', allCompletions=" + allCompletions.size() + ", filtered="
                + filtered.size());

        if (filtered.isEmpty()) {
            hidePopup();
        } else {
            // If the only suggestion is exactly what we already typed, don't show it
            if (filtered.size() == 1 && filtered.get(0).getLabel().equalsIgnoreCase(prefix)) {
                hidePopup();
            } else {
                showPopup(filtered);
            }
        }
    }

    private String getWordAtCaret() {
        try {
            int caret = textComponent.getCaretPosition();
            if (caret < 0)
                return "";

            String text = textComponent.getText(0, caret);
            int start = caret - 1;
            while (start >= 0 && Character.isJavaIdentifierPart(text.charAt(start))) {
                start--;
            }
            start++; // First char of word

            return text.substring(start);
        } catch (BadLocationException e) {
            return "";
        }
    }

    // Returns the start offset of the word at caret
    private int getWordStartOffset() {
        try {
            int caret = textComponent.getCaretPosition();
            if (caret < 0)
                return 0;

            String text = textComponent.getText(0, caret);
            int start = caret - 1;
            while (start >= 0 && Character.isJavaIdentifierPart(text.charAt(start))) {
                start--;
            }
            start++; // First char of word

            return start;
        } catch (BadLocationException e) {
            return textComponent.getCaretPosition();
        }
    }

    private void showPopup(List<CompletionItem> items) {
        System.out.println("[DEBUG] showPopup called with " + items.size() + " items");
        list.setListData(items.toArray(new CompletionItem[0]));
        list.setSelectedIndex(0);

        try {
            int caret = textComponent.getCaretPosition();
            Rectangle rect = textComponent.modelToView(caret);

            if (rect == null) {
                System.out.println("[DEBUG] modelToView returned null");
                return;
            }

            Point location = rect.getLocation();
            SwingUtilities.convertPointToScreen(location, textComponent);

            // Offset slightly down
            location.y += rect.height;

            popup.setLocation(location);
            popup.setSize(200, Math.min(items.size() * 20 + 5, 200)); // Dynamic height
            popup.setVisible(true);
            isShowing = true;
            System.out.println("[DEBUG] popup shown at " + location);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void hidePopup() {
        if (isShowing) {
            popup.setVisible(false);
            isShowing = false;
        }
    }

    private void moveSelection(int delta) {
        int idx = list.getSelectedIndex();
        int size = list.getModel().getSize();
        int newIdx = (idx + delta + size) % size;
        list.setSelectedIndex(newIdx);
        list.ensureIndexIsVisible(newIdx);
    }

    private void insertSelection() {
        CompletionItem item = list.getSelectedValue();
        if (item == null)
            return;

        hidePopup();
        isInserting = true;

        // Prevent auto-triggering during insertion
        textComponent.getDocument().removeDocumentListener(documentListener);

        try {
            int caret = textComponent.getCaretPosition();
            int start = getWordStartOffset();
            // Handle edge case where getWordStart gives weird results at line boundaries
            if (start > caret)
                start = caret;

            // Remove the partial word
            textComponent.getDocument().remove(start, caret - start);

            // Calculate indentation of the current line
            javax.swing.text.Element root = textComponent.getDocument().getDefaultRootElement();
            int lineIdx = root.getElementIndex(start);
            javax.swing.text.Element line = root.getElement(lineIdx);
            int lineStart = line.getStartOffset();

            // Get text from line start up to insertion point to find indentation
            String linePrefix = textComponent.getText(lineStart, start - lineStart);
            StringBuilder indent = new StringBuilder();
            for (char c : linePrefix.toCharArray()) {
                if (Character.isWhitespace(c)) {
                    indent.append(c);
                } else {
                    break;
                }
            }
            String indentStr = indent.toString();

            // Insert replacement with adjusted indentation for multi-line snippets
            String textToInsert = item.getInsertText().replace("\n", "\n" + indentStr);

            textComponent.getDocument().insertString(start, textToInsert, null);

            // Move caret to specified offset
            // Note: Since we only indented *subsequent* lines, and currently all snippets
            // place cursor on the first line, we don't need to adjust cursorOffset.
            textComponent.setCaretPosition(start + item.getCursorOffset());

        } catch (BadLocationException e) {
            e.printStackTrace();
        } finally {
            textComponent.getDocument().addDocumentListener(documentListener);
            isInserting = false;
        }
    }
}
