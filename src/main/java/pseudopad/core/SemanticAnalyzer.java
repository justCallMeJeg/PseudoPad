package pseudopad.core;

import java.util.*;

import pseudopad.core.AST.*;
import pseudopad.core.AST.FunctionNode.*;
import pseudopad.core.Errors.*;

/**
 * Validates the AST for semantic correctness, e.g., type checking.
 */
public class SemanticAnalyzer {

    private final Set<String> validTypes = new HashSet<>();
    private final Map<String, FunctionSignature> functionSignatures = new HashMap<>();
    private final Map<String, List<String>> classConstructors = new HashMap<>();
    private final Map<String, String> variableTypes = new HashMap<>(); // Simple scope tracking
    private final List<Errors.CompilationError> errors = new ArrayList<>();

    // Track the expected return type of the current function being analyzed
    private String currentFunctionReturnType = null;

    private record FunctionSignature(List<String> paramTypes, String returnType) {
    }

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

    public List<CompilationError> analyze(ProgramNode program) {
        errors.clear();

        // Pass 1: Collect Class Declarations
        collectTypes(program);

        // Pass 2: Verify Statements
        verifyStatements(program.statements);

        return errors;
    }

    private void collectTypes(ProgramNode program) {
        for (Node node : program.statements) {
            if (node instanceof ClassNode classNode) {
                validTypes.add(classNode.name);

                // Check if class has init method for constructor verification
                List<String> initParams = new ArrayList<>();
                for (FunctionNode method : classNode.methods) {
                    if (method.name.equals("init")) {
                        for (FunctionNode.Parameter param : method.parameters) {
                            initParams.add(param.type());
                        }
                        break;
                    }
                }
                classConstructors.put(classNode.name, initParams);

            } else if (node instanceof FunctionNode funcNode) {
                List<String> paramTypes = new ArrayList<>();
                for (Parameter param : funcNode.parameters) {
                    paramTypes.add(param.type());
                }
                functionSignatures.put(funcNode.name, new FunctionSignature(paramTypes, funcNode.returnType));
            }
        }
    }

    private void verifyStatements(List<? extends Node> statements) {
        for (Node node : statements) {
            verifyNode(node);
        }
    }

    private void verifyNode(Node node) {
        if (node instanceof VariableDeclarationNode varDecl) {
            variableTypes.put(varDecl.identifier, varDecl.typeName); // Add to scope
            verifyType(varDecl.typeToken);
            verifyExpression(varDecl.value);

            // Check if initial value type matches declared type
            if (varDecl.value != null) {
                String actualType = inferType(varDecl.value);
                if (actualType != null && !typesMatch(varDecl.typeName, actualType)) {
                    errors.add(new Errors.CompilationError(
                            "Type mismatch: cannot assign '" + actualType.toLowerCase()
                                    + "' to variable of type '" + varDecl.typeName.toLowerCase() + "'",
                            varDecl.identifierToken.line,
                            varDecl.identifierToken.column,
                            varDecl.identifierToken.length));
                }
            }
        } else if (node instanceof FunctionNode funcNode) {
            verifyType(funcNode.returnTypeToken);

            // New scope for function
            // Note: Since we don't have nested function support or complex closures yet,
            // clearing the map or using a fresh one for the body is sufficient.
            // But to be safe and simple: strict local scope.
            Map<String, String> previousScope = new HashMap<>(variableTypes);
            String previousReturnType = currentFunctionReturnType;
            currentFunctionReturnType = funcNode.returnType;

            for (Parameter param : funcNode.parameters) {
                verifyType(param.typeToken());
                variableTypes.put(param.name(), param.type());
            }
            verifyStatements(funcNode.body);

            // Check if non-void function has a return statement
            if (!funcNode.returnType.equalsIgnoreCase("void")) {
                if (!hasReturnStatement(funcNode.body)) {
                    errors.add(new Errors.CompilationError(
                            "Function '" + funcNode.name + "' must return a value of type '" + funcNode.returnType
                                    + "'",
                            funcNode.nameToken.line,
                            funcNode.nameToken.column,
                            funcNode.nameToken.length));
                }
            }

            // Restore scope and return type context
            currentFunctionReturnType = previousReturnType;
            variableTypes.clear();
            variableTypes.putAll(previousScope);

        } else if (node instanceof ClassNode classNode) {
            // Verify fields
            for (VariableDeclarationNode field : classNode.fields) {
                verifyType(field.typeToken);
                verifyExpression(field.value);
            }
            // Verify methods
            for (FunctionNode method : classNode.methods) {
                verifyType(method.returnTypeToken);

                Map<String, String> previousScope = new HashMap<>(variableTypes);

                for (Parameter param : method.parameters) {
                    verifyType(param.typeToken());
                    variableTypes.put(param.name(), param.type());
                }
                verifyStatements(method.body);

                variableTypes.clear();
                variableTypes.putAll(previousScope);
            }
        } else if (node instanceof IfNode ifNode) {
            verifyExpression(ifNode.condition);
            verifyStatements(ifNode.thenBranch);
            if (ifNode.elifBranches != null) {
                for (ElifNode elif : ifNode.elifBranches) {
                    verifyExpression(elif.condition);
                    verifyStatements(elif.body);
                }
            }
            if (ifNode.elseBranch != null) {
                verifyStatements(ifNode.elseBranch);
            }
        } else if (node instanceof WhileNode whileNode) {
            verifyExpression(whileNode.condition);
            verifyStatements(whileNode.body);
        } else if (node instanceof ForNode forNode) {
            verifyExpression(forNode.condition);
            verifyStatements(forNode.body);
        } else if (node instanceof AST.PrintNode printNode) {
            verifyExpression(printNode.expression);
        } else if (node instanceof AST.ReturnNode returnNode) {
            verifyExpression(returnNode.value);

            // Check if void function is trying to return a value
            if (currentFunctionReturnType != null &&
                    currentFunctionReturnType.equalsIgnoreCase("void") &&
                    returnNode.value != null) {
                errors.add(new Errors.CompilationError(
                        "Cannot return a value from a void function",
                        returnNode.keyword.line,
                        returnNode.keyword.column,
                        returnNode.keyword.length));
            }
            // Check if return value type matches expected function return type
            else if (currentFunctionReturnType != null && returnNode.value != null) {
                String actualType = inferType(returnNode.value);
                if (actualType != null && !typesMatch(currentFunctionReturnType, actualType)) {
                    errors.add(new Errors.CompilationError(
                            "Type mismatch: cannot return '" + actualType.toLowerCase() + "' from function expecting '"
                                    + currentFunctionReturnType.toLowerCase() + "'",
                            returnNode.keyword.line,
                            returnNode.keyword.column,
                            returnNode.keyword.length));
                }
            }
        } else if (node instanceof AST.ExpressionStatement exprStmt) {
            verifyExpression(exprStmt.expression);
        }
    }

    private void verifyExpression(AST.Expression expr) {
        if (expr == null)
            return;

        if (expr instanceof AST.CallExpressionNode call) {
            if (call.callee instanceof AST.IdentifierNode id) {
                String name = id.name;
                List<AST.Expression> args = call.arguments;
                int argCount = args.size();

                // Check Global Functions
                if (functionSignatures.containsKey(name)) {
                    FunctionSignature sig = functionSignatures.get(name);
                    int expected = sig.paramTypes.size();
                    if (argCount != expected) {
                        errors.add(new Errors.CompilationError(
                                "Function '" + name + "' expects " + expected + " arguments, but got " + argCount + ".",
                                call.parenthesis.line,
                                call.parenthesis.column,
                                1));
                    } else {
                        // Check argument types
                        for (int i = 0; i < argCount; i++) {
                            String expectedType = sig.paramTypes.get(i);
                            String actualType = inferType(args.get(i));
                            if (actualType != null && !typesMatch(expectedType, actualType)) {
                                errors.add(new Errors.CompilationError(
                                        "Function '" + name + "' expects argument " + (i + 1) + " to be " + expectedType
                                                + ", but got " + actualType + ".",
                                        call.parenthesis.line,
                                        call.parenthesis.column,
                                        1));
                            }
                        }
                    }
                }
                // Check Class Constructors
                else if (classConstructors.containsKey(name)) {
                    List<String> expectedTypes = classConstructors.get(name);
                    int expected = expectedTypes.size();
                    if (argCount != expected) {
                        errors.add(new Errors.CompilationError(
                                "Class constructor '" + name + "' expects " + expected + " arguments, but got "
                                        + argCount + ".",
                                call.parenthesis.line,
                                call.parenthesis.column,
                                1));
                    } else {
                        // Check argument types
                        for (int i = 0; i < argCount; i++) {
                            String expectedType = expectedTypes.get(i);
                            String actualType = inferType(args.get(i));
                            if (actualType != null && !typesMatch(expectedType, actualType)) {
                                errors.add(new Errors.CompilationError(
                                        "Constructor '" + name + "' expects argument " + (i + 1) + " to be "
                                                + expectedType + ", but got " + actualType + ".",
                                        call.parenthesis.line,
                                        call.parenthesis.column,
                                        1));
                            }
                        }
                    }
                }
            }
            // Verify arguments recursively
            for (AST.Expression arg : call.arguments) {
                verifyExpression(arg);
            }
        } else if (expr instanceof AST.BinaryExpressionNode bin) {
            verifyExpression(bin.left);
            verifyExpression(bin.right);
        } else if (expr instanceof AST.UnaryExpressionNode unary) {
            verifyExpression(unary.expression);
        } else if (expr instanceof AST.ListLiteralNode list) {
            for (AST.Expression e : list.elements)
                verifyExpression(e);
        } else if (expr instanceof AST.IdentifierNode id) {
            // Check if variable is defined
            if (!variableTypes.containsKey(id.name)
                    && !functionSignatures.containsKey(id.name)
                    && !classConstructors.containsKey(id.name)) {
                int line = id.token != null ? id.token.line : 0;
                int col = id.token != null ? id.token.column : 0;
                int len = id.token != null ? id.token.length : 0;
                errors.add(new Errors.CompilationError(
                        "Undefined variable: '" + id.name + "'",
                        line, col, len));
            }
        }
    }

    private String inferType(AST.Expression expr) {
        if (expr instanceof AST.LiteralNode lit) {
            return lit.typeName.toUpperCase();
        } else if (expr instanceof AST.IdentifierNode id) {
            return variableTypes.get(id.name);
        } else if (expr instanceof AST.ListLiteralNode) {
            return "LIST";
        } else if (expr instanceof AST.DictLiteralNode) {
            return "DICT";
        } else if (expr instanceof AST.CallExpressionNode call) {
            if (call.callee instanceof AST.IdentifierNode id && functionSignatures.containsKey(id.name)) {
                return functionSignatures.get(id.name).returnType.toUpperCase();
            }
            // Constructor calls return the class name
            if (call.callee instanceof AST.IdentifierNode id && classConstructors.containsKey(id.name)) {
                return id.name;
            }
        }
        return null; // Unknown
    }

    private boolean typesMatch(String expected, String actual) {
        if (expected.equalsIgnoreCase("ANY") || actual.equalsIgnoreCase("ANY"))
            return true;
        return expected.equalsIgnoreCase(actual);
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

    /**
     * Recursively checks if a list of statements contains a return statement.
     */
    private boolean hasReturnStatement(List<? extends Node> statements) {
        for (Node stmt : statements) {
            if (stmt instanceof AST.ReturnNode) {
                return true;
            }
            // Check inside if/else branches
            if (stmt instanceof IfNode ifNode) {
                if (hasReturnStatement(ifNode.thenBranch))
                    return true;
                if (ifNode.elseBranch != null && hasReturnStatement(ifNode.elseBranch))
                    return true;
                if (ifNode.elifBranches != null) {
                    for (ElifNode elif : ifNode.elifBranches) {
                        if (hasReturnStatement(elif.body))
                            return true;
                    }
                }
            }
            // Check inside loops
            if (stmt instanceof WhileNode whileNode) {
                if (hasReturnStatement(whileNode.body))
                    return true;
            }
            if (stmt instanceof ForNode forNode) {
                if (hasReturnStatement(forNode.body))
                    return true;
            }
        }
        return false;
    }
}
