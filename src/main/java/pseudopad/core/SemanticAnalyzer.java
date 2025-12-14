package pseudopad.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates the AST for semantic correctness, e.g., type checking.
 */
public class SemanticAnalyzer {

    private final Set<String> validTypes = new HashSet<>();
    private final List<Errors.CompilationError> errors = new ArrayList<>();

    public SemanticAnalyzer() {
        // Built-in types
        validTypes.add("NUMBER");
        validTypes.add("STRING");
        validTypes.add("BOOLEAN");
        validTypes.add("LIST");
        validTypes.add("DICT");
        validTypes.add("VOID");
        // validTypes.add("ANY"); // If needed
    }

    public List<Errors.CompilationError> analyze(AST.ProgramNode program) {
        errors.clear();

        // Pass 1: Collect Class Declarations
        collectTypes(program);

        // Pass 2: Verify Statements
        verifyStatements(program.statements);

        return errors;
    }

    private void collectTypes(AST.ProgramNode program) {
        for (AST.Node node : program.statements) {
            if (node instanceof AST.ClassNode classNode) {
                validTypes.add(classNode.name);
            }
        }
    }

    private void verifyStatements(List<? extends AST.Node> statements) {
        for (AST.Node node : statements) {
            verifyNode(node);
        }
    }

    private void verifyNode(AST.Node node) {
        if (node instanceof AST.VariableDeclarationNode varDecl) {
            verifyType(varDecl.typeToken);
        } else if (node instanceof AST.FunctionNode funcNode) {
            verifyType(funcNode.returnTypeToken);
            for (AST.FunctionNode.Parameter param : funcNode.parameters) {
                verifyType(param.typeToken());
            }
            verifyStatements(funcNode.body);
        } else if (node instanceof AST.ClassNode classNode) {
            // Verify fields
            for (AST.VariableDeclarationNode field : classNode.fields) {
                verifyType(field.typeToken);
            }
            // Verify methods
            for (AST.FunctionNode method : classNode.methods) {
                verifyType(method.returnTypeToken);
                for (AST.FunctionNode.Parameter param : method.parameters) {
                    verifyType(param.typeToken());
                }
                verifyStatements(method.body);
            }
        } else if (node instanceof AST.IfNode ifNode) {
            verifyStatements(ifNode.thenBranch);
            if (ifNode.elifBranches != null) {
                for (AST.ElifNode elif : ifNode.elifBranches) {
                    verifyStatements(elif.body);
                }
            }
            if (ifNode.elseBranch != null) {
                verifyStatements(ifNode.elseBranch);
            }
        } else if (node instanceof AST.WhileNode whileNode) {
            verifyStatements(whileNode.body);
        } else if (node instanceof AST.ForNode forNode) {
            verifyStatements(forNode.body);
        }
        // TODO: add more checks if needed (e.g. valid assignment targets etc)
    }

    private void verifyType(Token typeToken) {
        if (typeToken == null)
            return;

        String typeName = typeToken.value;
        // Native types are case-insensitive (e.g. string vs STRING)
        if (typeToken.type == TokenType.TYPE) {
            typeName = typeName.toUpperCase();
        }

        if (!validTypes.contains(typeName)) {
            errors.add(new Errors.CompilationError(
                    "Unknown type: '" + typeName + "'",
                    typeToken.line,
                    typeToken.column,
                    typeToken.length));
        }
    }
}
