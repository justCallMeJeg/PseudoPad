package pseudopad.core;

public class Errors {
    public static class ReturnSignal extends RuntimeException {
        public final Object value;

        public ReturnSignal(Object value) {
            this.value = value;
        }
    }

    // ===== LEXER ERROR =====
    public static class LexerError extends RuntimeException {
        public LexerError(String message) {
            super(message);
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

    // ===== TYPE ERROR (optional, but good to have separately) =====
    public static class TypeError extends RuntimeException {
        public TypeError(String message) {
            super(message);
        }
    }

    static class BreakSignal extends RuntimeException {
    }

    static class SkipSignal extends RuntimeException {
    }

    // ===== COMPILATION ERROR =====
    public static class CompilationError {
        public final String message;
        public final int line;
        public final int column;
        public final int length;

        public CompilationError(String message, int line, int column, int length) {
            this.message = message;
            this.line = line;
            this.column = column;
            this.length = length;
        }

        @Override
        public String toString() {
            return "[Line " + line + ":" + column + "] " + message;
        }
    }

    // ===== COMPILATION RESULT =====
    public static class CompilationResult {
        public final AST.ProgramNode ast;
        public final java.util.List<CompilationError> errors;

        public CompilationResult(AST.ProgramNode ast, java.util.List<CompilationError> errors) {
            this.ast = ast;
            this.errors = errors;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

}
