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

    // ===== TYPE ERROR (optional, but good to have separately) =====
    public static class TypeError extends RuntimeException {
        public TypeError(String message) {
            super(message);
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

    // ===== COMPILATION RESULT & ERROR =====
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
            return message + " at line " + line + ":" + column;
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
