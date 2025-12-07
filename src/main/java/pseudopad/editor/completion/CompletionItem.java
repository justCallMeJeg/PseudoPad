package pseudopad.editor.completion;

/**
 * Represents a single completion suggestion.
 */
public class CompletionItem implements Comparable<CompletionItem> {
    private final String label;
    private final String insertText;
    private final int cursorOffset; // Offset relative to the end of inserted text. Usually 0.
                                    // For snippets like "IF ... ENDIF", we might want cursor inside.
                                    // Let's define it as "Position relative to the START of the inserted text".

    public CompletionItem(String label, String insertText) {
        this(label, insertText, insertText.length());
    }

    public CompletionItem(String label, String insertText, int cursorOffset) {
        this.label = label;
        this.insertText = insertText;
        this.cursorOffset = cursorOffset;
    }

    public String getLabel() {
        return label;
    }

    public String getInsertText() {
        return insertText;
    }

    public int getCursorOffset() {
        return cursorOffset;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public int compareTo(CompletionItem o) {
        return this.label.compareToIgnoreCase(o.label);
    }
}
