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
        } catch (Exception e) {
            outputProvider.print("\nruntime error: " + e.getMessage());
        }
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
            errorList.add(new Errors.CompilationError(e.getMessage(), e.line, e.column, 1));
        } catch (Exception e) {
            errorList.add(new Errors.CompilationError("Internal Error: " + e.getMessage(), 0, 0, 0));
        }

        return new Errors.CompilationResult(program, errorList);
    }
}
