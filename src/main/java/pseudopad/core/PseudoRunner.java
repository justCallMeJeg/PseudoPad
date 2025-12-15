package pseudopad.core;

import java.util.List;

import pseudopad.core.AST.ProgramNode;
import pseudopad.core.Interpreter.InputProvider;

/**
 * 
 * @author Joseph Mikhaeli Jalandoni
 */
public class PseudoRunner {

    public static void run(String sourceCode, InputProvider inputProvider, OutputProvider outputProvider) {
        try {
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            ProgramNode program = parser.parse();

            Interpreter interpreter = new Interpreter(inputProvider, outputProvider);
            interpreter.run(program);

        } catch (Errors.ExecutionStoppedError e) {
            outputProvider.print("\n>> " + e.getMessage());
        } catch (Errors.LexerError e) {
            outputProvider.print(formatStackTrace("Lexical Error", e.getMessage(), e.line, e.column, sourceCode));
        } catch (Errors.ParserError e) {
            int line = e.token != null ? e.token.line : 0;
            int col = e.token != null ? e.token.column : 0;
            outputProvider.print(formatStackTrace("Syntax Error", e.getMessage(), line, col, sourceCode));
        } catch (Errors.RuntimeError e) {
            int line = e.token != null ? e.token.line : 0;
            int col = e.token != null ? e.token.column : 0;
            outputProvider.print(formatStackTrace("Runtime Error", e.getMessage(), line, col, sourceCode));
        } catch (Errors.TypeError e) {
            int line = e.token != null ? e.token.line : 0;
            int col = e.token != null ? e.token.column : 0;
            outputProvider.print(formatStackTrace("Type Error", e.getMessage(), line, col, sourceCode));
        } catch (Exception e) {
            outputProvider.print(formatStackTrace("Internal Error", e.getMessage(), 0, 0, sourceCode));
        }
    }

    /**
     * Formats an error as a stack trace like Node.js/JavaScript.
     * Format:
     * filename:line
     * source code line
     * ^^^^^^
     * ErrorType: message
     * at <context> (file:line:col)
     */
    private static String formatStackTrace(String errorType, String message, int line, int col, String sourceCode) {
        StringBuilder sb = new StringBuilder();
        String fileName = "script.pseudo"; // Default filename
        String RED = "\u001B[31m";
        String RESET = "\u001B[0m";

        sb.append("\n");

        // Line 1: file:line
        if (line > 0) {
            sb.append(RED).append(fileName).append(":").append(line).append(RESET).append("\n");

            // Line 2: source code (indented)
            String[] lines = sourceCode.split("\n");
            if (line > 0 && line <= lines.length) {
                String sourceLine = lines[line - 1];
                sb.append(RED).append("    ").append(sourceLine).append(RESET).append("\n");

                // Line 3: pointer ^^^^^^ under the error
                sb.append(RED).append("    ");
                for (int i = 1; i < col; i++)
                    sb.append(" ");
                // Show multiple ^ to indicate error span
                sb.append("^^^^^^").append(RESET).append("\n");
            }
            sb.append("\n");
        }

        // Error type and message
        sb.append(RED).append(errorType).append(": ").append(message).append(RESET).append("\n");

        // Call stack (simulated for Pseudo language)
        if (line > 0) {
            sb.append(RED).append("    at <main> (").append(fileName).append(":").append(line).append(":").append(col)
                    .append(")").append(RESET).append("\n");
        }
        sb.append(RED).append("    at PseudoRunner.run (internal)").append(RESET).append("\n");
        sb.append(RED).append("    at Interpreter.execute (internal)").append(RESET).append("\n");

        return sb.toString();
    }

    public static String run(String sourceCode, InputProvider inputProvider) {
        StringBuilder outputAccumulator = new StringBuilder();
        OutputProvider provider = (msg) -> outputAccumulator.append(msg).append(System.lineSeparator());

        run(sourceCode, inputProvider, provider);

        return outputAccumulator.toString();
    }

    public static Errors.CompilationResult compile(String sourceCode) {
        java.util.List<Errors.CompilationError> errorList = new java.util.ArrayList<>();
        ProgramNode program = null;
        try {
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            program = parser.parse();
            errorList.addAll(parser.errors);

            // Run Semantic Analysis if AST is available
            if (program != null) {
                SemanticAnalyzer analyzer = new SemanticAnalyzer();
                errorList.addAll(analyzer.analyze(program));
            }

        } catch (Errors.LexerError e) {
            errorList.add(new Errors.CompilationError(e.getMessage(), e.line, e.column, 1, Errors.ErrorCategory.LEXER));
        } catch (Errors.ParserError e) {
            // Extract position info from token if available
            int line = 0, col = 0, len = 1;
            if (e.token != null) {
                line = e.token.line;
                col = e.token.column;
                len = e.token.length > 0 ? e.token.length : 1;
            }
            errorList.add(new Errors.CompilationError(e.getMessage(), line, col, len, Errors.ErrorCategory.PARSER));
        } catch (Exception e) {
            errorList.add(new Errors.CompilationError("Internal Error: " + e.getMessage(), 0, 0, 0,
                    Errors.ErrorCategory.INTERNAL));
        }

        return new Errors.CompilationResult(program, errorList);
    }
}
