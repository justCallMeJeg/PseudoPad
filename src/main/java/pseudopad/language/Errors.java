package pseudopad.language;

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

    static class BreakSignal extends RuntimeException {}
    static class SkipSignal extends RuntimeException {}

}
