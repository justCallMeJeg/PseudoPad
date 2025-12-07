package pseudopad.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pseudopad.core.AST.ProgramNode;
import pseudopad.core.Errors.*;
import pseudopad.core.Interpreter.*;

public class PseudoRunner {

    public static CompilationResult compile(String sourceCode) {
        try {
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();

            if (!lexer.getErrors().isEmpty()) {
                return new CompilationResult(null, lexer.getErrors());
            }

            Parser parser = new Parser(tokens);
            ProgramNode program = parser.parse();

            if (!parser.getErrors().isEmpty()) {
                return new CompilationResult(program, parser.getErrors());
            }

            // Semantic Analysis
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            List<CompilationError> semanticErrors = analyzer.analyze(program);
            if (!semanticErrors.isEmpty()) {
                return new CompilationResult(program, semanticErrors);
            }

            return new CompilationResult(program, new ArrayList<>());

        } catch (Exception e) {
            e.printStackTrace();
            return new CompilationResult(null, Collections.singletonList(
                    new CompilationError(e.getMessage(), 0, 0, 0)));
        }
    }

    public static CompilationResult run(String sourceCode, InputProvider inputProvider,
            OutputProvider outputProvider) {

        CompilationResult result = compile(sourceCode);

        if (result.hasErrors()) {
            outputProvider.print("\nErrors Found:\n");
            for (CompilationError error : result.errors) {
                outputProvider.print(error.toString() + "\n");
            }
            return result;
        }

        try {
            if (result.ast != null) {
                Interpreter interpreter = new Interpreter(inputProvider, outputProvider);
                interpreter.run(result.ast);
            }
            return result;

        } catch (Exception e) {
            // Print errors to our captured output so the user sees them
            e.printStackTrace(); // For debugging
            outputProvider.print("\nruntime error: " + e.getMessage() + "\n");

            // Return failure
            return new CompilationResult(null, Collections.singletonList(
                    new CompilationError(e.getMessage(), 0, 0, 0)));
        }
    }
}
