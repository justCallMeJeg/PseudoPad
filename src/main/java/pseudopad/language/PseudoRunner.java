package pseudopad.language;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

public class PseudoRunner {

    public static String run(String sourceCode, Interpreter.InputProvider inputProvider) {
        // Capture standard output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream customOut = new PrintStream(outputStream);
        PrintStream originalOut = System.out; // Backup

        try {
            System.setOut(customOut); // Redirect System.out to our buffer

            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            AST.ProgramNode program = parser.parse();

            Interpreter interpreter = new Interpreter(inputProvider);
            interpreter.run(program);

        } catch (Exception e) {
            // Print errors to our captured output so the user sees them
            System.out.println("\nruntime error: " + e.getMessage());
        } finally {
            System.setOut(originalOut); // Restore stdout!
        }

        return outputStream.toString();
    }
}