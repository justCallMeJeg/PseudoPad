package pseudopad.editor;

/**
 * Code formatter for Pseudo language.
 * Handles indentation for block structures like func/endfunc, if/endif, etc.
 */
public class PseudoFormatter {

    private static final int INDENT_SIZE = 4;
    private static final String INDENT = "    "; // 4 spaces

    /**
     * Format the given Pseudo code.
     */
    public static String format(String code) {
        if (code == null || code.isEmpty()) {
            return code;
        }

        StringBuilder result = new StringBuilder();
        String[] lines = code.split("\n", -1); // -1 to keep trailing empty strings
        int indentLevel = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Skip empty lines but preserve them (unless it's the last line)
            if (line.isEmpty()) {
                if (i < lines.length - 1) {
                    result.append("\n");
                }
                continue;
            }

            String upperLine = line.toUpperCase();

            // Check if this line should decrease indent BEFORE printing
            boolean decreaseFirst = shouldDecreaseIndentFirst(upperLine);
            if (decreaseFirst && indentLevel > 0) {
                indentLevel--;
            }

            // Add proper indentation
            result.append(getIndent(indentLevel));
            result.append(line);

            // Add newline if not last line
            if (i < lines.length - 1) {
                result.append("\n");
            }

            // Check if this line should increase indent for next line
            if (shouldIncreaseIndent(upperLine)) {
                indentLevel++;
            }
            // Check if this line (like ELSE, ELIF) should decrease then increase
            else if (isMiddleKeyword(upperLine) && !decreaseFirst) {
                // Already handled by decreaseFirst
            }
        }

        return result.toString();
    }

    /**
     * Check if the line starts a block that increases indentation.
     */
    private static boolean shouldIncreaseIndent(String upperLine) {
        // Lines ending with DO or THEN start a block
        if (upperLine.endsWith(" DO") || upperLine.equals("DO")) {
            return true;
        }
        if (upperLine.endsWith(" THEN") || upperLine.equals("THEN")) {
            return true;
        }
        // ELSE and ELIF start new blocks
        if (upperLine.equals("ELSE") || upperLine.startsWith("ELSE ")) {
            return true;
        }
        if (upperLine.startsWith("ELIF ") || upperLine.startsWith("ELIF(")) {
            return true;
        }
        // CLASS declaration
        if (upperLine.startsWith("CLASS ")) {
            return true;
        }
        return false;
    }

    /**
     * Check if this line should decrease indent before being printed.
     * These are closing keywords like ENDFUNC, ENDIF, etc.
     */
    private static boolean shouldDecreaseIndentFirst(String upperLine) {
        return upperLine.equals("ENDFUNC") ||
                upperLine.equals("ENDIF") ||
                upperLine.equals("ENDWHILE") ||
                upperLine.equals("ENDFOR") ||
                upperLine.equals("ENDCLASS") ||
                upperLine.equals("ELSE") ||
                upperLine.startsWith("ELSE ") ||
                upperLine.startsWith("ELIF ") ||
                upperLine.startsWith("ELIF(");
    }

    /**
     * Check if this is a middle keyword that should be at same level as opening.
     */
    private static boolean isMiddleKeyword(String upperLine) {
        return upperLine.equals("ELSE") ||
                upperLine.startsWith("ELSE ") ||
                upperLine.startsWith("ELIF ");
    }

    /**
     * Get the indentation string for the given level.
     */
    private static String getIndent(int level) {
        if (level <= 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
        return sb.toString();
    }
}
