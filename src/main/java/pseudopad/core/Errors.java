package pseudopad.core;

/**
 * 
 * @author Joseph Mikhaeli Jalandoni
 */
public class Errors {
    public static class ReturnSignal extends RuntimeException {
        public final Object value;

        public ReturnSignal(Object value) {
            this.value = value;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    // ===== LEXER ERROR =====
    public static class LexerError extends RuntimeException {
        public final int line;
        public final int column;

        public LexerError(String message, int line, int column) {
            super(message);
            this.line = line;
            this.column = column;
        }

        public LexerError(String message) {
            super(message);
            this.line = -1;
            this.column = -1;
        }
    }

    // ===== PARSER ERROR =====
    public static class ParserError extends RuntimeException {
        public final Token token;

        public ParserError(String message, Token token) {
            super(message + " at token " + token);
            this.token = token;
        }

        public ParserError(String message) {
            super(message);
            this.token = null;
        }
    }

    // ===== RUNTIME ERROR =====
    public static class RuntimeError extends RuntimeException {
        public final Token token;

        public RuntimeError(String message, Token token) {
            super(message + " at token " + token);
            this.token = token;
        }

        public RuntimeError(String message) {
            super(message);
            this.token = null;
        }
    }

    // ===== TYPE ERROR =====
    public static class TypeError extends RuntimeException {
        public final Token token;

        public TypeError(String message, Token token) {
            super(message);
            this.token = token;
        }

        public TypeError(String message) {
            super(message);
            this.token = null;
        }
    }

    // ===== EXECUTION STOPPED ERROR =====
    public static class ExecutionStoppedError extends RuntimeException {
        public ExecutionStoppedError() {
            super("Execution stopped by user.");
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    static class BreakSignal extends RuntimeException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    static class SkipSignal extends RuntimeException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    // ===== ERROR CATEGORY =====
    public enum ErrorCategory {
        LEXER("Lexical Error"),
        PARSER("Syntax Error"),
        SEMANTIC("Semantic Error"),
        RUNTIME("Runtime Error"),
        INTERNAL("Internal Error");

        private final String displayName;

        ErrorCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ===== COMPILATION ERROR =====
    public static class CompilationError {
        public final String message;
        public final int line;
        public final int column;
        public final int length;
        public final ErrorCategory category;

        public CompilationError(String message, int line, int column, int length) {
            this(message, line, column, length, ErrorCategory.INTERNAL);
        }

        public CompilationError(String message, int line, int column, int length, ErrorCategory category) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.length = length;
            this.category = category;
        }

        @Override
        public String toString() {
            return "[" + line + ":" + column + "] " + message;
        }

        /**
         * Returns a formatted string for tree display.
         * Example: "├── [Syntax Error] Line 5:10 - Expected semicolon"
         */
        public String toTreeFormat(boolean isLast) {
            String prefix = isLast ? "└── " : "├── ";
            return prefix + "[" + category.getDisplayName() + "] Line " + line + ":" + column + " - " + message;
        }

        /**
         * Returns red-colored error string for terminal output using ANSI codes.
         */
        public String toRedTerminal() {
            return "\u001B[31m[" + category.getDisplayName() + "] [" + line + ":" + column + "] " + message
                    + "\u001B[0m";
        }

        /**
         * Returns a formatted tree view for a list of errors.
         */
        public static String formatErrorTree(java.util.List<CompilationError> errors, String fileName) {
            if (errors == null || errors.isEmpty()) {
                return "\u001B[32m✓ No errors found\u001B[0m";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\u001B[31m✗ ").append(errors.size()).append(" error(s) in ").append(fileName)
                    .append("\u001B[0m\n");

            // Group errors by category
            java.util.Map<ErrorCategory, java.util.List<CompilationError>> grouped = new java.util.LinkedHashMap<>();
            for (CompilationError err : errors) {
                grouped.computeIfAbsent(err.category, k -> new java.util.ArrayList<>()).add(err);
            }

            int categoryIdx = 0;
            int categoryCount = grouped.size();
            for (var entry : grouped.entrySet()) {
                categoryIdx++;
                boolean isLastCategory = (categoryIdx == categoryCount);
                String categoryPrefix = isLastCategory ? "└── " : "├── ";
                String childPrefix = isLastCategory ? "    " : "│   ";

                sb.append(categoryPrefix).append("\u001B[31m").append(entry.getKey().getDisplayName())
                        .append(" (").append(entry.getValue().size()).append(")\u001B[0m\n");

                java.util.List<CompilationError> categoryErrors = entry.getValue();
                for (int i = 0; i < categoryErrors.size(); i++) {
                    CompilationError err = categoryErrors.get(i);
                    boolean isLastError = (i == categoryErrors.size() - 1);
                    String errorPrefix = isLastError ? "└── " : "├── ";
                    sb.append(childPrefix).append(errorPrefix)
                            .append("\u001B[31m[").append(err.line).append(":").append(err.column).append("] ")
                            .append(err.message).append("\u001B[0m\n");
                }
            }

            return sb.toString();
        }
    }

    public static class CompilationResult {
        public final AST.ProgramNode ast;
        public final java.util.List<CompilationError> errors;

        public CompilationResult(AST.ProgramNode ast, java.util.List<CompilationError> errors) {
            this.ast = ast;
            this.errors = errors;
        }
    }

}
