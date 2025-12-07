package pseudopad.core;

import java.util.List;
import java.util.Map;

public class AST {
    public static class ProgramNode {
        public final List<Node> statements;

        public ProgramNode(List<Node> statements) {
            this.statements = statements;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ProgramNode[\n");
            for (Node statement : statements) {
                builder.append(" ").append(statement).append("\n");
            }
            builder.append("]");
            return builder.toString();
        }
    }

    public static abstract class Node {
    }

    public abstract static class Expression extends Node {
    }

    public abstract static class Statement extends Node {
    }

    public static class LiteralNode extends Expression {
        public final Object value;
        public final String typeName;

        public LiteralNode(Object value, String typeName) {
            this.value = value;
            this.typeName = typeName;
        }

        public String toString() {
            return "LiteralNode(" + value + ": " + typeName + ")";
        }
    }

    public static class IdentifierNode extends Expression {
        public final Token token;
        public final String name;

        public IdentifierNode(Token token) {
            this.token = token;
            this.name = token.value;
        }

        public String toString() {
            return "IdentifierNode(" + name + ")";
        }
    }

    public static class PrintNode extends Statement {
        public final Expression expression;

        public PrintNode(Expression expression) {
            this.expression = expression;
        }

        public String toString() {
            return "PrintNode(" + expression + ")";
        }
    }

    public static class CallExpressionNode extends Expression {
        public final Expression callee;
        public final Token parenthesis; // for error location tracking
        public final List<Expression> arguments;

        public CallExpressionNode(Expression callee, Token parenthesis, List<Expression> arguments) {
            this.callee = callee;
            this.parenthesis = parenthesis;
            this.arguments = arguments;
        }

        @Override
        public String toString() {
            return "Call(" + callee + ", args=" + arguments + ")";
        }
    }

    public static class BinaryExpressionNode extends Expression {
        public final Expression left;
        public final Token operator;
        public final Expression right;

        public BinaryExpressionNode(Expression left, Token operator, Expression right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        public String toString() {
            return "BinaryExpressionNode(" + left + ", " + operator + ", " + right + ")";
        }
    }

    public static class UnaryExpressionNode extends Expression {
        public final Token operator;
        public final Expression expression;

        public UnaryExpressionNode(Token operator, Expression expression) {
            this.operator = operator;
            this.expression = expression;
        }

        public String toString() {
            return "UnaryExpressionNode(" + operator + ", " + expression + ")";
        }
    }

    public static class ListLiteralNode extends Expression {
        public final List<AST.Expression> elements;

        public ListLiteralNode(List<AST.Expression> elements) {
            this.elements = elements;
        }

        public String toString() {
            return "ListLiteralNode(Elements: " + elements + ")";
        }
    }

    public static class DictLiteralNode extends Expression {
        public final Map<AST.Expression, AST.Expression> entries;

        public DictLiteralNode(Map<Expression, Expression> entries) {
            this.entries = entries;
        }

        public String toString() {
            return "DictLiteralNode(Entries: " + entries + ")";
        }
    }

    public static class IndexExpressionNode extends Expression {
        public final AST.Expression target;
        public final AST.Expression index;

        public IndexExpressionNode(AST.Expression target, AST.Expression index) {
            this.target = target;
            this.index = index;
        }

        public String toString() {
            return "IndexExpressionNode(Target: " + target + ", Index: " + index + ")";
        }
    }

    public static class GetExpressionNode extends Expression {
        public final Expression object;
        public final Token name;

        public GetExpressionNode(Expression object, Token name) {
            this.object = object;
            this.name = name;
        }

        @Override
        public String toString() {
            return "Get(" + object + "." + name.value + ")";
        }
    }

    public static class ClassNode extends Statement {
        public final String name;
        public final Token nameToken;
        public final List<VariableDeclarationNode> fields;
        public final List<FunctionNode> methods;

        public ClassNode(Token nameToken, List<VariableDeclarationNode> fields, List<FunctionNode> methods) {
            this.nameToken = nameToken;
            this.name = nameToken.value;
            this.fields = fields;
            this.methods = methods;
        }

        @Override
        public String toString() {
            return "Class(" + name + ")";
        }
    }

    public static class ThisExpressionNode extends Expression {
        public final Token keyword;

        public ThisExpressionNode(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        public String toString() {
            return "This";
        }
    }

    public static class VariableDeclarationNode extends Statement {
        public final boolean isConst;
        public final String typeName; // number | string | boolean
        public final String identifier;
        public final Token nameToken;
        public Expression value; // can be null

        public VariableDeclarationNode(boolean isConst, String typeName,
                Token nameToken, Expression value) {
            this.isConst = isConst;
            this.typeName = typeName;
            this.nameToken = nameToken;
            this.identifier = nameToken.value;
            this.value = value;
        }

        public String toString() {
            return "VarDecl(isConst=" + isConst + ", type=" + typeName + ", id=" + identifier +
                    ", value=" + value + ")";
        }
    }

    public static class VariableAssignmentNode extends Statement {
        public final String identifier;
        public final Expression value;

        public VariableAssignmentNode(String identifier, Expression value) {
            this.identifier = identifier;
            this.value = value;
        }

        public String toString() {
            return "Assign(id=" + identifier + ", value=" + value + ")";
        }
    }

    public static class AssignmentNode extends Statement {
        public final AST.Expression target;
        public final AST.Expression value;

        public AssignmentNode(AST.Expression target, AST.Expression value) {
            this.target = target;
            this.value = value;
        }
    }

    public static class IfNode extends Statement {
        public final Expression condition;
        public final List<Statement> thenBranch;
        public final List<ElifNode> elifBranches;
        public final List<Statement> elseBranch;

        public IfNode(Expression condition, List<Statement> thenBranch, List<ElifNode> elifBranches,
                List<Statement> elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elifBranches = elifBranches;
            this.elseBranch = elseBranch;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("If(")
                    .append(condition).append(") {\n");

            sb.append("  then ").append(thenBranch).append("\n");

            for (ElifNode e : elifBranches) {
                sb.append("  ").append(e).append("\n");
            }

            if (elseBranch != null) {
                sb.append("  else ").append(elseBranch).append("\n");
            }

            sb.append("}");
            return sb.toString();
        }
    }

    public static class ElifNode {
        public final Expression condition;
        public final List<Statement> body;

        public ElifNode(Expression condition, List<Statement> body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public String toString() {
            return "Elif(" + condition + ") " + body;
        }
    }

    public static class WhileNode extends Statement {
        public final Expression condition;
        public final List<AST.Statement> body;

        public WhileNode(Expression condition, List<AST.Statement> body) {
            this.condition = condition;
            this.body = body;
        }

        @Override
        public String toString() {
            return "While(\n\tCondition: " + condition + "\n\tBody: " + body + "\n)";
        }
    }

    public static class ForNode extends Statement {
        public final Node initializer;
        public final Expression condition;
        public final Node increment;
        public final List<Statement> body;

        public ForNode(Statement initializer, Expression condition, Statement increment, List<Statement> body) {
            this.initializer = initializer;
            this.condition = condition;
            this.increment = increment;
            this.body = body;
        }

        @Override
        public String toString() {
            return "While(\n\tCondition: " + condition + "\n\tBody: " + body + "\n\tInitializer: " + initializer
                    + "\n\tIncrement: " + increment + "\n)";
        }
    }

    public static class BreakNode extends Statement {
        @Override
        public String toString() {
            return "Break";
        }
    }

    public static class SkipNode extends Statement {
        @Override
        public String toString() {
            return "Skip";
        }
    }

    public static class ReturnNode extends Statement {
        public final Token keyword;
        public final Expression value; // Can be null for 'return;'

        public ReturnNode(Token keyword, Expression value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Return(" + value + ")";
        }
    }

    public static class FunctionNode extends Statement {
        public record Parameter(String name, String type) {
        }

        public final String name;
        public final Token nameToken;
        public final List<Parameter> parameters;
        public final String returnType;
        public final List<Statement> body;

        public FunctionNode(Token nameToken, List<Parameter> parameters, String returnType, List<Statement> body) {
            this.nameToken = nameToken;
            this.name = nameToken.value;
            this.parameters = parameters;
            this.returnType = returnType;
            this.body = body;
        }

        @Override
        public String toString() {
            return "Function(" + name + ")";
        }
    }

    public static class ExpressionStatement extends Statement {
        public final Expression expression;

        public ExpressionStatement(Expression expression) {
            this.expression = expression;
        }

        @Override
        public String toString() {
            return expression.toString() + ";";
        }
    }

}