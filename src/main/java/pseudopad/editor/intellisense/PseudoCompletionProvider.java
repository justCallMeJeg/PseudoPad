package pseudopad.editor.intellisense;

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

    public PseudoCompletionProvider() {
    }

    static {
        // Statement Keywords
        addKeyword("set", "set ", CompletionItem.Category.KEYWORD);
        addKeyword("const", "const ", CompletionItem.Category.KEYWORD);
        addKeyword("print", "print();", CompletionItem.Category.KEYWORD);
        addKeyword("return", "return ", CompletionItem.Category.KEYWORD);
        addKeyword("break", "break", CompletionItem.Category.KEYWORD);
        addKeyword("skip", "skip", CompletionItem.Category.KEYWORD);

        // Data Types
        addKeyword("void", "void", CompletionItem.Category.TYPE);
        addKeyword("number", "number", CompletionItem.Category.TYPE);
        addKeyword("string", "string", CompletionItem.Category.TYPE);
        addKeyword("boolean", "boolean", CompletionItem.Category.TYPE);
        addKeyword("list", "list", CompletionItem.Category.TYPE);
        addKeyword("dict", "dict", CompletionItem.Category.TYPE);

        // Control Flow Snippets
        addSnippet("if", "if () then\n    \nendif", 4, CompletionItem.Category.SNIPPET);
        addSnippet("if-else", "if () then\n    \nelse\n    \nendif", 4, CompletionItem.Category.SNIPPET);
        addSnippet("while", "while () do\n    \nendwhile", 7, CompletionItem.Category.SNIPPET);
        addSnippet("for", "for () do\n    \nendfor", 5, CompletionItem.Category.SNIPPET);
        addSnippet("func", "func void name() do\n    \nendfunc", 5, CompletionItem.Category.SNIPPET);
        addSnippet("class", "class name do\n    \nendclass", 6, CompletionItem.Category.SNIPPET);
    }

    private static void addKeyword(String label, String code, CompletionItem.Category category) {
        KEYWORDS.add(new CompletionItem(label, code, code.length(), category));
    }

    private static void addSnippet(String label, String code, int cursorOffset, CompletionItem.Category category) {
        KEYWORDS.add(new CompletionItem(label, code, cursorOffset, category));
    }

    @Override
    public List<CompletionItem> getCompletions(JTextComponent comp) {
        List<CompletionItem> suggestions = new ArrayList<>();

        // Clone KEYWORDS list (so we don't modify the original)
        for (CompletionItem kw : KEYWORDS) {
            // Create new item to avoid mutating static list
            CompletionItem clone = new CompletionItem(kw.getLabel(), kw.getInsertText(),
                    kw.getCursorOffset(), kw.getCategory());
            suggestions.add(clone);
        }

        System.out.println("[DEBUG Provider] KEYWORDS.size=" + KEYWORDS.size() + ", suggestions after clone="
                + suggestions.size());

        Set<String> seen = new HashSet<>();
        for (CompletionItem item : suggestions) {
            seen.add(item.getLabel());
        }

        String text = comp.getText();
        Lexer lexer = new Lexer(text);
        List<Token> tokens = new ArrayList<>();

        try {
            tokens = lexer.tokenize();

            // Try to parse AST to get variable types and create function/class completions
            java.util.Map<String, String> varTypes = new java.util.HashMap<>();
            pseudopad.core.AST.ProgramNode ast = null;
            try {
                pseudopad.core.Parser parser = new pseudopad.core.Parser(tokens);
                ast = parser.parse();
                if (ast != null) {
                    collectVariableTypes(ast.statements, varTypes);

                    // Add function completions with parameter placeholders
                    for (pseudopad.core.AST.Node node : ast.statements) {
                        if (node instanceof pseudopad.core.AST.FunctionNode func) {
                            if (!seen.contains(func.name)) {
                                String insertText = buildFunctionCallSnippet(func);
                                String typeInfo = "func → " + func.returnType;
                                suggestions.add(new CompletionItem(func.name, insertText,
                                        func.name.length() + 1, CompletionItem.Category.IDENTIFIER, typeInfo));
                                seen.add(func.name);
                            }
                        } else if (node instanceof pseudopad.core.AST.ClassNode classNode) {
                            if (!seen.contains(classNode.name)) {
                                String insertText = buildConstructorSnippet(classNode);
                                suggestions.add(new CompletionItem(classNode.name, insertText,
                                        classNode.name.length() + 1, CompletionItem.Category.IDENTIFIER, "class"));
                                seen.add(classNode.name);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // Ignore parse errors for completion purposes
            }

            for (Token token : tokens) {
                if (token.type == TokenType.IDENTIFIER) {
                    String id = token.value;
                    if (id != null && !seen.contains(id)) {
                        String typeInfo = varTypes.get(id);
                        suggestions.add(
                                new CompletionItem(id, id, id.length(), CompletionItem.Category.IDENTIFIER, typeInfo));
                        seen.add(id);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore for robust fallback
        }

        // Detect context and boost priority accordingly
        CompletionItem.Category priorityCategory = detectContext(text, comp.getCaretPosition());
        if (priorityCategory != null) {
            for (CompletionItem item : suggestions) {
                if (item.getCategory() == priorityCategory) {
                    item.setPriorityBoost(100); // High boost for matching context
                }
            }
        }

        // Check for Dot Completion
        try {
            int caret = comp.getCaretPosition();
            int i = caret - 1;

            // Scan backwards through identifier characters
            while (i >= 0 && Character.isJavaIdentifierPart(text.charAt(i))) {
                i--;
            }

            // Check if we hit a dot
            if (i >= 0 && text.charAt(i) == '.') {
                // Verify there's no newline between dot and caret (dot must be on same line)
                // Check for both Unix (\n) and Windows (\r\n) line endings
                String segment = text.substring(i, caret);
                System.out.println("[DEBUG Provider] Dot at i=" + i + ", caret=" + caret + ", segment='"
                        + segment.replace("\n", "\\n").replace("\r", "\\r") + "'");
                boolean hasNewline = segment.contains("\n") || segment.contains("\r");
                if (!hasNewline) {
                    System.out.println("[DEBUG Provider] Dot completion triggered at i=" + i);
                    // Determine the caret position effectively "at the dot" (i + 1)
                    // Pass the tokens we just lexed to avoid re-work
                    return resolveDotCompletion(tokens, text, i + 1);
                } else {
                    System.out.println("[DEBUG Provider] Dot at i=" + i + " rejected (cross-line)");
                }
            }
        } catch (Exception e) {
            System.out.println("[DEBUG Provider] Exception in dot check: " + e.getMessage());
        }

        System.out.println("[DEBUG Provider] Reached sort, suggestions count=" + suggestions.size());

        // Sort suggestions based on priority boost and alphabetically
        java.util.Collections.sort(suggestions);

        System.out.println("[DEBUG Provider] Returning " + suggestions.size() + " suggestions");
        return suggestions;
    }

    /**
     * Detect the context based on the text before the caret.
     * Returns the category that should be prioritized, or null for no specific
     * context.
     */
    private CompletionItem.Category detectContext(String text, int caret) {
        // Find the previous non-whitespace token
        int i = caret - 1;

        // Skip current partial word
        while (i >= 0 && Character.isJavaIdentifierPart(text.charAt(i))) {
            i--;
        }
        // Skip whitespace
        while (i >= 0 && Character.isWhitespace(text.charAt(i))) {
            i--;
        }

        if (i < 0)
            return null;

        // Find the start of the previous word
        int end = i + 1;
        while (i >= 0 && Character.isJavaIdentifierPart(text.charAt(i))) {
            i--;
        }

        String prevWord = text.substring(i + 1, end).toLowerCase();

        // Context: after 'set' or 'set const' or 'func' => expect TYPE
        switch (prevWord) {
            case "set":
            case "const":
            case "func":
                return CompletionItem.Category.TYPE;
            default:
                return null;
        }
    }

    private List<CompletionItem> resolveDotCompletion(List<Token> tokens, String text, int caret) {
        List<CompletionItem> suggestions = new ArrayList<>();

        // Parse fresh AST to ensure we have latest structure (variable defs, etc.)
        try {
            pseudopad.core.Parser parser = new pseudopad.core.Parser(tokens);
            // Parser now synchronizes and returns partial AST
            pseudopad.core.AST.ProgramNode program = parser.parse();

            if (program == null) {
                suggestions.add(new CompletionItem("Debug: Program Null", ""));
                return suggestions;
            }

            // 1. Find variable name before dot
            String varName = getVariableNameBeforeDot(text, caret - 1);
            if (varName == null) {
                suggestions.add(new CompletionItem("Debug: No Var Name", ""));
                return suggestions;
            }
            // suggestions.add(new CompletionItem("Debug: Var " + varName, ""));

            // 2. Find variable type (class name) in AST
            String className = findVariableType(program, varName, caret);
            if (className == null) {
                suggestions.add(new CompletionItem("Debug: No Type for " + varName, ""));
                return suggestions;
            }
            // suggestions.add(new CompletionItem("Debug: Type " + className, ""));

            // 3. Find Class definition and populate suggestions
            int beforeCount = suggestions.size();
            populateClassMembers(program, className, suggestions);

            if (suggestions.size() == beforeCount) {
                suggestions.add(new CompletionItem("Debug: Class Not Found " + className, ""));
            }

        } catch (Exception e) {
            suggestions.add(new CompletionItem("Debug: Exception " + e.getMessage(), ""));
        }

        return suggestions;
    }

    private String getVariableNameBeforeDot(String text, int dotIndex) {
        // Scan backwards from dot
        int i = dotIndex - 1;
        while (i >= 0 && Character.isWhitespace(text.charAt(i)))
            i--;

        if (i < 0)
            return null;

        int end = i + 1;
        while (i >= 0 && Character.isJavaIdentifierPart(text.charAt(i)))
            i--;

        return text.substring(i + 1, end);
    }

    private String findVariableType(pseudopad.core.AST.ProgramNode program, String varName, int caret) {
        // Simple search: finding the LAST variable declaration of that name
        // before the caret.
        // NOTE: This does not support complex scoping (nested functions/ifs) perfectly
        // but works for top-level and simple class methods.

        // This walker is stateless and just scans everything.
        // A better approach would be to track scope ranges.
        // For Hack, we just look for variable declarations in the tree.

        return findVarTypeRecursive(program.statements, varName);
    }

    // Changed signature to accept List<? extends Node> to handle List<Statement>
    private String findVarTypeRecursive(List<? extends pseudopad.core.AST.Node> nodes, String varName) {
        for (pseudopad.core.AST.Node node : nodes) {
            if (node instanceof pseudopad.core.AST.VariableDeclarationNode varDecl) {
                if (varDecl.identifier.equals(varName)) {
                    return varDecl.typeName;
                }
            } else if (node instanceof pseudopad.core.AST.FunctionNode func) {
                // Check params
                for (pseudopad.core.AST.FunctionNode.Parameter param : func.parameters) {
                    if (param.name().equals(varName))
                        return param.type();
                }
                String found = findVarTypeRecursive(func.body, varName); // func.body is List<Statement>
                if (found != null)
                    return found;
            } else if (node instanceof pseudopad.core.AST.ClassNode cls) {
                for (pseudopad.core.AST.VariableDeclarationNode field : cls.fields) {
                    if (field.identifier.equals(varName))
                        return field.typeName;
                }
                for (pseudopad.core.AST.FunctionNode method : cls.methods) {
                    String found = findVarTypeRecursive(method.body, varName);
                    if (found != null)
                        return found;
                }
            }
            // Add If/While/For children if needed for recursion
            else if (node instanceof pseudopad.core.AST.IfNode ifNode) {
                String found = findVarTypeRecursive(ifNode.thenBranch, varName);
                if (found != null)
                    return found;
                if (ifNode.elseBranch != null) {
                    found = findVarTypeRecursive(ifNode.elseBranch, varName);
                    if (found != null)
                        return found;
                }
            } else if (node instanceof pseudopad.core.AST.WhileNode whileNode) {
                String found = findVarTypeRecursive(whileNode.body, varName);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Collect variable and function types from AST for completion type hints.
     */
    private void collectVariableTypes(java.util.List<? extends pseudopad.core.AST.Node> nodes,
            java.util.Map<String, String> varTypes) {
        for (pseudopad.core.AST.Node node : nodes) {
            if (node instanceof pseudopad.core.AST.VariableDeclarationNode varDecl) {
                varTypes.put(varDecl.identifier, varDecl.typeName);
            } else if (node instanceof pseudopad.core.AST.FunctionNode func) {
                varTypes.put(func.name, "func → " + func.returnType);
                // Also collect variables from function body
                collectVariableTypes(func.body, varTypes);
            } else if (node instanceof pseudopad.core.AST.ClassNode classNode) {
                varTypes.put(classNode.name, "class");
                // Collect fields
                for (pseudopad.core.AST.VariableDeclarationNode field : classNode.fields) {
                    varTypes.put(field.identifier, field.typeName);
                }
            } else if (node instanceof pseudopad.core.AST.IfNode ifNode) {
                collectVariableTypes(ifNode.thenBranch, varTypes);
                if (ifNode.elseBranch != null) {
                    collectVariableTypes(ifNode.elseBranch, varTypes);
                }
            } else if (node instanceof pseudopad.core.AST.WhileNode whileNode) {
                collectVariableTypes(whileNode.body, varTypes);
            } else if (node instanceof pseudopad.core.AST.ForNode forNode) {
                collectVariableTypes(forNode.body, varTypes);
            }
        }
    }

    private void populateClassMembers(pseudopad.core.AST.ProgramNode program, String className,
            List<CompletionItem> suggestions) {
        for (pseudopad.core.AST.Node node : program.statements) {
            if (node instanceof pseudopad.core.AST.ClassNode classNode) {
                if (classNode.name.equals(className)) {
                    // Fields
                    for (pseudopad.core.AST.VariableDeclarationNode field : classNode.fields) {
                        suggestions.add(new CompletionItem(field.identifier, field.identifier,
                                field.identifier.length(), CompletionItem.Category.MEMBER, field.typeName));
                    }
                    // Methods with parameter snippets
                    for (pseudopad.core.AST.FunctionNode method : classNode.methods) {
                        if (!method.name.equals("init")) {
                            String insertText = buildFunctionCallSnippet(method);
                            suggestions.add(new CompletionItem(method.name, insertText,
                                    method.name.length() + 1, CompletionItem.Category.MEMBER,
                                    "→ " + method.returnType));
                        }
                    }
                    return;
                }
            }
        }
    }

    /**
     * Build a function call snippet with parameter placeholders.
     * Example: "myFunc(param1, param2)"
     */
    private String buildFunctionCallSnippet(pseudopad.core.AST.FunctionNode func) {
        StringBuilder sb = new StringBuilder(func.name);
        sb.append("(");
        for (int i = 0; i < func.parameters.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(func.parameters.get(i).name());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Build a class constructor call snippet.
     * Example: "new ClassName(param1, param2)"
     */
    private String buildConstructorSnippet(pseudopad.core.AST.ClassNode classNode) {
        // Find init method if it exists
        for (pseudopad.core.AST.FunctionNode method : classNode.methods) {
            if (method.name.equals("init")) {
                StringBuilder sb = new StringBuilder("new ");
                sb.append(classNode.name);
                sb.append("(");
                for (int i = 0; i < method.parameters.size(); i++) {
                    if (i > 0)
                        sb.append(", ");
                    sb.append(method.parameters.get(i).name());
                }
                sb.append(")");
                return sb.toString();
            }
        }
        // No init method, just use empty parentheses
        return "new " + classNode.name + "()";
    }
}
