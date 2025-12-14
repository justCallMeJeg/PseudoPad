package pseudopad.editor.completion;

/**
 * Represents a single completion suggestion.
 */
public class CompletionItem implements Comparable<CompletionItem> {

    public enum Category {
        TYPE, // Data types: number, string, boolean, list, dict
        KEYWORD, // Keywords: set, const, print, return, break, skip
        SNIPPET, // Multi-line snippets: if, while, for, func, class
        IDENTIFIER, // User-defined identifiers
        MEMBER // Class members (fields/methods)
    }

    private final String label;
    private final String insertText;
    private final int cursorOffset;
    private final Category category;

    // Priority boost for context-aware sorting (higher = more relevant)
    private int priorityBoost = 0;

    public CompletionItem(String label, String insertText) {
        this(label, insertText, insertText.length(), Category.IDENTIFIER);
    }

    public CompletionItem(String label, String insertText, int cursorOffset) {
        this(label, insertText, cursorOffset, Category.IDENTIFIER);
    }

    public CompletionItem(String label, String insertText, int cursorOffset, Category category) {
        this.label = label;
        this.insertText = insertText;
        this.cursorOffset = cursorOffset;
        this.category = category;
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

    public Category getCategory() {
        return category;
    }

    public int getPriorityBoost() {
        return priorityBoost;
    }

    public void setPriorityBoost(int boost) {
        this.priorityBoost = boost;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public int compareTo(CompletionItem o) {
        // First compare by priority boost (higher boost = comes first)
        int boostCompare = Integer.compare(o.priorityBoost, this.priorityBoost);
        if (boostCompare != 0)
            return boostCompare;

        // Then alphabetically
        return this.label.compareToIgnoreCase(o.label);
    }
}
