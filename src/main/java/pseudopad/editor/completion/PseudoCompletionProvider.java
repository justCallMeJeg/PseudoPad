package pseudopad.editor.completion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.text.JTextComponent;

import pseudopad.core.Lexer;
import pseudopad.core.Token;
import pseudopad.core.TokenType;

/**
 * CompletionProvider that suggests keywords and identifiers for Pseudo.
 */
public class PseudoCompletionProvider implements CompletionProvider {

    // Core keywords from TokenType
    private static final List<CompletionItem> KEYWORDS = new ArrayList<>();

    static {
        // Simple keywords
        addKeyword("set", "set ");
        addKeyword("const", "const ");
        addKeyword("print", "print();");
        addKeyword("return", "return ");
        addKeyword("break", "break");
        addKeyword("skip", "skip");

        // Types
        addKeyword("number", "number");
        addKeyword("string", "string");
        addKeyword("boolean", "boolean");
        addKeyword("list", "list");
        addKeyword("dict", "dict");

        // Control Flow Snippets
        addSnippet("if", "if () then\n    \nendif", 4); // Cursor inside
        addSnippet("if-else", "if () then\n    \nelse\n    \nendif", 4);
        addSnippet("while", "while () do\n    \nendwhile", 7);
        addSnippet("for", "for () do\n    \nendfor", 5);
        addSnippet("func", "func void name() do\n    \nendfunc", 5);
        addSnippet("class", "class name do\n    \nendclass", 6);
    }

    private static void addKeyword(String label, String code) {
        KEYWORDS.add(new CompletionItem(label, code, code.length()));
    }

    private static void addSnippet(String label, String code, int cursorOffset) {
        KEYWORDS.add(new CompletionItem(label, code, cursorOffset));
    }

    @Override
    public List<CompletionItem> getCompletions(JTextComponent comp) {
        List<CompletionItem> suggestions = new ArrayList<>(KEYWORDS); // Start with keywords
        Set<String> seen = new HashSet<>();

        // Add keywords to 'seen' to prevent duplicates if a user types a keyword as an
        // identifier (though Lexer should handle this)
        for (CompletionItem item : KEYWORDS) {
            seen.add(item.getLabel());
        }

        String text = comp.getText();
        Lexer lexer = new Lexer(text);

        try {
            List<Token> tokens = lexer.tokenize();
            for (Token token : tokens) {
                if (token.type == TokenType.IDENTIFIER) {
                    String id = token.value;
                    if (id != null && !seen.contains(id)) {
                        suggestions.add(new CompletionItem(id, id, id.length()));
                        seen.add(id);
                    }
                }
            }
        } catch (Exception e) {
            // Lexer might fail on incomplete code while typing, strictly ignore errors here
            // We still want to return whatever keywords/identifiers we found so far or just
            // keywords
        }

        return suggestions;
    }
}
