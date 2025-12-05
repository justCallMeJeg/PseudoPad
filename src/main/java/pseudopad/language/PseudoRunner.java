package pseudopad.language;

import java.util.List;

public class PseudoRunner {

    public static void run(String sourceCode, Interpreter.InputProvider inputProvider,
            Interpreter.OutputProvider outputProvider) {
        try {
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            AST.ProgramNode program = parser.parse();

            Interpreter interpreter = new Interpreter(inputProvider, outputProvider);
            interpreter.run(program);

        } catch (Exception e) {
            // Print errors to our captured output so the user sees them
            outputProvider.print("\nruntime error: " + e.getMessage() + "\n");
        }
    }
}