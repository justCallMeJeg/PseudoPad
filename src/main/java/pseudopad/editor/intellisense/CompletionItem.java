package pseudopad.editor.intellisense;

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
    private final String typeInfo; // Type information (e.g., "number", "string", "void")

    // Priority boost for context-aware sorting (higher = more relevant)
    private int priorityBoost = 0;

    public CompletionItem(String label, String insertText) {
        this(label, insertText, insertText.length(), Category.IDENTIFIER, null);
    }

    public CompletionItem(String label, String insertText, int cursorOffset) {
        this(label, insertText, cursorOffset, Category.IDENTIFIER, null);
    }

    public CompletionItem(String label, String insertText, int cursorOffset, Category category) {
        this(label, insertText, cursorOffset, category, null);
    }

    public CompletionItem(String label, String insertText, int cursorOffset, Category category, String typeInfo) {
        this.label = label;
        this.insertText = insertText;
        this.cursorOffset = cursorOffset;
        this.category = category;
        this.typeInfo = typeInfo;
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

    public String getTypeInfo() {
        return typeInfo;
    }

    public int getPriorityBoost() {
        return priorityBoost;
    }

    public void setPriorityBoost(int boost) {
        this.priorityBoost = boost;
    }

    @Override
    public String toString() {
        if (typeInfo != null && !typeInfo.isEmpty()) {
            return label + " : " + typeInfo;
        }
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
