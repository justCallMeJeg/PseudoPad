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
}
