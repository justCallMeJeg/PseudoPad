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

    public AutoCompletion(JTextComponent textComponent, CompletionProvider provider) {
        this.textComponent = textComponent;
        this.provider = provider;
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

            // Check previous character to ensure we are typing a word
            char prevChar = textComponent.getText(caret - 1, 1).charAt(0);
            if (!Character.isLetterOrDigit(prevChar)) {
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

        if (prefix.isEmpty()) {
            hidePopup();
            return;
        }

        List<CompletionItem> filtered = new ArrayList<>();

        for (CompletionItem item : allCompletions) {
            // Case-insensitive prefix match
            if (item.getLabel().toLowerCase().startsWith(prefix.toLowerCase())) {
                filtered.add(item);
            }
        }

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
            int start = Utilities.getWordStart(textComponent, caret);
            // Safety check: word start might be after caret if we are at start of line?
            // Utilities.getWordStart usually returns start of word containing pos.
            if (start > caret)
                start = caret;

            return textComponent.getText(start, caret - start);
        } catch (BadLocationException e) {
            return "";
        }
    }

    // Returns the start offset of the word at caret
    private int getWordStartOffset() {
        try {
            int caret = textComponent.getCaretPosition();
            // Basic logic: backtrack until whitespace or separator
            // Swing Utilities.getWordStart is smart about this
            return Utilities.getWordStart(textComponent, caret);
        } catch (BadLocationException e) {
            return textComponent.getCaretPosition();
        }
    }

    private void showPopup(List<CompletionItem> items) {
        list.setListData(items.toArray(new CompletionItem[0]));
        list.setSelectedIndex(0);

        try {
            int caret = textComponent.getCaretPosition();
            Rectangle rect = textComponent.modelToView(caret);
            Point location = rect.getLocation();
            SwingUtilities.convertPointToScreen(location, textComponent);

            // Offset slightly down
            location.y += rect.height;

            popup.setLocation(location);
            popup.setSize(200, Math.min(items.size() * 20 + 5, 200)); // Dynamic height
            popup.setVisible(true);
            isShowing = true;

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

            // Insert replacement
            textComponent.getDocument().insertString(start, item.getInsertText(), null);

            // Move caret to specified offset
            textComponent.setCaretPosition(start + item.getCursorOffset());

        } catch (BadLocationException e) {
            e.printStackTrace();
        } finally {
            textComponent.getDocument().addDocumentListener(documentListener);
            isInserting = false;
        }
    }
}
