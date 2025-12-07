package pseudopad.core;

import java.util.*;

public class SemanticAnalyzer {
    private final List<Errors.CompilationError> errors = new ArrayList<>();
    private final Stack<Set<String>> scopes = new Stack<>();
    private int loopDepth = 0;
    private int functionDepth = 0;

    public List<Errors.CompilationError> analyze(AST.ProgramNode program) {
        errors.clear();
        scopes.clear();
        loopDepth = 0;
        functionDepth = 0;

        beginScope(); // Global scope
        declare("input"); // Built-in function

        for (AST.Node stmt : program.statements) {
            analyze(stmt);
        }

        endScope();
        return errors;
    }

    private void beginScope() {
        scopes.push(new HashSet<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(String name) {
        if (scopes.isEmpty())
            return;
        scopes.peek().add(name);
    }

    private void checkDefined(Token token) {
        String name = token.value;
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).contains(name)) {
                return;
            }
        }

        // Also check if it's a class method/field access via 'this'?
        // Simple scope check might fail for class members if not careful.
        // For now, simple variable check.
        error(token, "Undefined variable '" + name + "'.");
    }

    private void error(Token token, String message) {
        errors.add(new Errors.CompilationError(message, token.line, token.column, token.length));
    }

    private void analyze(AST.Node node) {
        if (node instanceof AST.VariableDeclarationNode decl) {
            if (decl.value != null)
                analyze(decl.value);
            // Declare AFTER value validation (no "var x = x + 1" in same scope if x
            // undefined)
            // Wait, Java allows int x = x + 1 if x was param? strict usage before declare.
            declare(decl.identifier);
        } else if (node instanceof AST.IdentifierNode id) {
            checkDefined(id.token);
        } else if (node instanceof AST.AssignmentNode assign) {
            analyze(assign.target);
            analyze(assign.value);
        } else if (node instanceof AST.PrintNode print) {
            analyze(print.expression);
        } else if (node instanceof AST.ExpressionStatement exprStmt) {
            analyze(exprStmt.expression);
        } else if (node instanceof AST.IfNode ifNode) {
            analyze(ifNode.condition);
            analyzeBlock(ifNode.thenBranch);
            for (AST.ElifNode elif : ifNode.elifBranches) {
                analyze(elif.condition);
                analyzeBlock(elif.body);
            }
            if (ifNode.elseBranch != null)
                analyzeBlock(ifNode.elseBranch);
        } else if (node instanceof AST.WhileNode whileNode) {
            analyze(whileNode.condition);
            loopDepth++;
            analyzeBlock(whileNode.body);
            loopDepth--;
        } else if (node instanceof AST.ForNode forNode) {
            beginScope(); // For loop initializer scope
            if (forNode.initializer != null)
                analyze(forNode.initializer);
            if (forNode.condition != null)
                analyze(forNode.condition);
            if (forNode.increment != null)
                analyze(forNode.increment);

            loopDepth++;
            analyzeBlock(forNode.body); // Body is inside loop scope?
            loopDepth--;
            endScope();
        } else if (node instanceof AST.BreakNode || node instanceof AST.SkipNode) {
            if (loopDepth == 0) {
                // We don't have token in BreakNode/SkipNode currently?
                // AST definition doesn't store token.
                // I should assume it's an error but I can't report line number easily without
                // token.
                // Assuming "0,0" for now or fix AST later.
                errors.add(new Errors.CompilationError("Loop control statement outside of loop.", 0, 0, 0));
            }
        } else if (node instanceof AST.FunctionNode func) {
            declare(func.name);
            functionDepth++;
            beginScope();
            for (AST.FunctionNode.Parameter param : func.parameters) {
                declare(param.name());
            }
            // Analyze body without new scope? Or body IS the scope?
            // Usually params are in function scope.
            for (AST.Statement stmt : func.body) {
                analyze(stmt);
            }
            endScope();
            functionDepth--;
        } else if (node instanceof AST.ReturnNode ret) {
            if (functionDepth == 0) {
                error(ret.keyword, "Return statement outside of function.");
            }
            if (ret.value != null)
                analyze(ret.value);
        } else if (node instanceof AST.CallExpressionNode call) {
            analyze(call.callee);
            for (AST.Expression arg : call.arguments) {
                analyze(arg);
            }
        } else if (node instanceof AST.BinaryExpressionNode bin) {
            analyze(bin.left);
            analyze(bin.right);
        } else if (node instanceof AST.UnaryExpressionNode unary) {
            analyze(unary.expression);
        } else if (node instanceof AST.ListLiteralNode list) {
            for (AST.Expression e : list.elements)
                analyze(e);
        } else if (node instanceof AST.DictLiteralNode dict) {
            for (Map.Entry<AST.Expression, AST.Expression> e : dict.entries.entrySet()) {
                analyze(e.getKey());
                analyze(e.getValue());
            }
        } else if (node instanceof AST.GetExpressionNode get) {
            analyze(get.object);
            // Property access is dynamic in this language (duck typing / runtime lookup)
            // So we don't check if `name` is defined in scope, it's a property of object.
        } else if (node instanceof AST.IndexExpressionNode index) {
            analyze(index.target);
            analyze(index.index);
        } else if (node instanceof AST.ClassNode klass) {
            declare(klass.name);
            // Class methods analysis...
            // inside methods 'this' is valid.
        }
        // ... Check other nodes ...
    }

    private void analyzeBlock(List<? extends AST.Node> block) {
        beginScope();
        for (AST.Node node : block) {
            analyze(node);
        }
        endScope();
    }
}
