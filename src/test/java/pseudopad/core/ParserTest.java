package pseudopad.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Parser class.
 * 
 * Syntax reference:
 * - Types: lowercase (number, string, boolean, list, dict, void)
 * - PRINT: PRINT(expression);
 * - SET: SET type name = value;
 * - IF: IF (condition) THEN ... ENDIF
 * - WHILE: WHILE (condition) DO ... ENDWHILE
 * - FOR: FOR (init; cond; incr) DO ... ENDFOR
 * - FUNC: FUNC returnType name(params) DO ... ENDFUNC
 * - CLASS: CLASS Name DO ... ENDCLASS
 */
class ParserTest {

    private AST.ProgramNode parse(String input) {
        List<Token> tokens = new Lexer(input).tokenize();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    private AST.Node firstStatement(String input) {
        AST.ProgramNode program = parse(input);
        assertFalse(program.statements.isEmpty(),
                "Parser should produce at least one statement for: " + input);
        return program.statements.get(0);
    }

    // ==================================================================================
    // VARIABLE DECLARATIONS - lowercase types
    // ==================================================================================

    @Nested
    @DisplayName("Variable Declarations")
    class VariableDeclarations {
        @Test
        void parseSetDeclaration() {
            AST.Node stmt = firstStatement("SET number x = 10;");
            assertInstanceOf(AST.VariableDeclarationNode.class, stmt);
            AST.VariableDeclarationNode decl = (AST.VariableDeclarationNode) stmt;
            assertEquals("x", decl.identifier);
            assertFalse(decl.isConst);
        }

        @Test
        void parseConstDeclaration() {
            AST.Node stmt = firstStatement("SET CONST number PI = 3.14;");
            assertInstanceOf(AST.VariableDeclarationNode.class, stmt);
            AST.VariableDeclarationNode decl = (AST.VariableDeclarationNode) stmt;
            assertEquals("PI", decl.identifier);
            assertTrue(decl.isConst);
        }

        @Test
        void parseStringVariable() {
            AST.Node stmt = firstStatement("SET string name = \"John\";");
            assertInstanceOf(AST.VariableDeclarationNode.class, stmt);
        }

        @Test
        void parseBooleanVariable() {
            AST.Node stmt = firstStatement("SET boolean flag = TRUE;");
            assertInstanceOf(AST.VariableDeclarationNode.class, stmt);
        }
    }

    // ==================================================================================
    // EXPRESSIONS
    // ==================================================================================

    @Nested
    @DisplayName("Expressions")
    class Expressions {
        @Test
        void parseBinaryExpression() {
            AST.Node stmt = firstStatement("PRINT(1 + 2);");
            assertInstanceOf(AST.PrintNode.class, stmt);
            AST.PrintNode print = (AST.PrintNode) stmt;
            assertInstanceOf(AST.BinaryExpressionNode.class, print.expression);
        }

        @Test
        void parseNestedBinaryExpression() {
            AST.Node stmt = firstStatement("PRINT(1 + 2 * 3);");
            assertInstanceOf(AST.PrintNode.class, stmt);
        }

        @Test
        void parseUnaryExpression() {
            AST.Node stmt = firstStatement("PRINT(-5);");
            assertInstanceOf(AST.PrintNode.class, stmt);
            AST.PrintNode print = (AST.PrintNode) stmt;
            assertInstanceOf(AST.UnaryExpressionNode.class, print.expression);
        }

        @Test
        void parseNotExpression() {
            AST.Node stmt = firstStatement("PRINT(NOT TRUE);");
            assertInstanceOf(AST.PrintNode.class, stmt);
        }

        @Test
        void parseParenthesizedExpression() {
            AST.Node stmt = firstStatement("PRINT((1 + 2) * 3);");
            assertInstanceOf(AST.PrintNode.class, stmt);
        }
    }

    // ==================================================================================
    // LIST AND DICT LITERALS
    // ==================================================================================

    @Nested
    @DisplayName("List and Dict Literals")
    class ListAndDictLiterals {
        @Test
        void parseEmptyList() {
            AST.Node stmt = firstStatement("SET list items = [];");
            assertInstanceOf(AST.VariableDeclarationNode.class, stmt);
        }

        @Test
        void parseListWithElements() {
            AST.Node stmt = firstStatement("SET list nums = [1, 2, 3];");
            assertInstanceOf(AST.VariableDeclarationNode.class, stmt);
            AST.VariableDeclarationNode decl = (AST.VariableDeclarationNode) stmt;
            assertInstanceOf(AST.ListLiteralNode.class, decl.value);
        }

        @Test
        void parseEmptyDict() {
            AST.Node stmt = firstStatement("SET dict data = {};");
            assertInstanceOf(AST.VariableDeclarationNode.class, stmt);
        }

        @Test
        void parseDictWithEntries() {
            AST.Node stmt = firstStatement("SET dict person = {\"name\": \"John\"};");
            assertInstanceOf(AST.VariableDeclarationNode.class, stmt);
            AST.VariableDeclarationNode decl = (AST.VariableDeclarationNode) stmt;
            assertInstanceOf(AST.DictLiteralNode.class, decl.value);
        }
    }

    // ==================================================================================
    // CONTROL FLOW
    // ==================================================================================

    @Nested
    @DisplayName("Control Flow")
    class ControlFlow {
        @Test
        void parseIfStatement() {
            AST.Node stmt = firstStatement("IF (TRUE) THEN\nPRINT(\"yes\");\nENDIF");
            assertInstanceOf(AST.IfNode.class, stmt);
            AST.IfNode ifStmt = (AST.IfNode) stmt;
            assertNotNull(ifStmt.condition);
        }

        @Test
        void parseIfElseStatement() {
            AST.Node stmt = firstStatement("IF (FALSE) THEN\nPRINT(\"yes\");\nELSE\nPRINT(\"no\");\nENDIF");
            assertInstanceOf(AST.IfNode.class, stmt);
        }

        @Test
        void parseWhileStatement() {
            AST.Node stmt = firstStatement("WHILE (TRUE) DO\nPRINT(\"loop\");\nENDWHILE");
            assertInstanceOf(AST.WhileNode.class, stmt);
        }

        @Test
        void parseForStatement() {
            AST.Node stmt = firstStatement("FOR (SET number i = 0; i < 10; i = i + 1) DO\nPRINT(i);\nENDFOR");
            assertInstanceOf(AST.ForNode.class, stmt);
        }
    }

    // ==================================================================================
    // FUNCTION DECLARATIONS - need DO keyword
    // ==================================================================================

    @Nested
    @DisplayName("Function Declarations")
    class FunctionDeclarations {
        @Test
        void parseFunctionNoParams() {
            AST.Node stmt = firstStatement("FUNC void greet() DO\nPRINT(\"Hello\");\nENDFUNC");
            assertInstanceOf(AST.FunctionNode.class, stmt);
            AST.FunctionNode func = (AST.FunctionNode) stmt;
            assertEquals("greet", func.name);
            assertTrue(func.parameters.isEmpty());
        }

        @Test
        void parseFunctionWithParams() {
            AST.Node stmt = firstStatement("FUNC number add(number a, number b) DO\nRETURN a + b;\nENDFUNC");
            assertInstanceOf(AST.FunctionNode.class, stmt);
            AST.FunctionNode func = (AST.FunctionNode) stmt;
            assertEquals("add", func.name);
            assertEquals(2, func.parameters.size());
        }

        @Test
        void parseFunctionReturnType() {
            AST.Node stmt = firstStatement("FUNC string getName() DO\nRETURN \"test\";\nENDFUNC");
            assertInstanceOf(AST.FunctionNode.class, stmt);
        }
    }

    // ==================================================================================
    // CLASS DECLARATIONS - need DO keyword
    // ==================================================================================

    @Nested
    @DisplayName("Class Declarations")
    class ClassDeclarations {
        @Test
        void parseClassWithFields() {
            String code = "CLASS Person DO\nSET string name = \"\";\nENDCLASS";
            AST.Node stmt = firstStatement(code);
            assertInstanceOf(AST.ClassNode.class, stmt);
            AST.ClassNode cls = (AST.ClassNode) stmt;
            assertEquals("Person", cls.name);
        }

        @Test
        void parseClassWithMethods() {
            String code = "CLASS Calculator DO\nFUNC number add(number a, number b) DO\nRETURN a + b;\nENDFUNC\nENDCLASS";
            AST.Node stmt = firstStatement(code);
            assertInstanceOf(AST.ClassNode.class, stmt);
            AST.ClassNode cls = (AST.ClassNode) stmt;
            assertEquals(1, cls.methods.size());
        }
    }

    // ==================================================================================
    // PRINT STATEMENTS
    // ==================================================================================

    @Nested
    @DisplayName("Print Statements")
    class PrintStatements {
        @Test
        void parsePrintString() {
            AST.Node stmt = firstStatement("PRINT(\"Hello World\");");
            assertInstanceOf(AST.PrintNode.class, stmt);
            AST.PrintNode print = (AST.PrintNode) stmt;
            assertInstanceOf(AST.LiteralNode.class, print.expression);
        }

        @Test
        void parsePrintExpression() {
            AST.Node stmt = firstStatement("PRINT(1 + 2);");
            assertInstanceOf(AST.PrintNode.class, stmt);
        }

        @Test
        void parsePrintIdentifier() {
            AST.ProgramNode program = parse("SET number myVar = 10;\nPRINT(myVar);");
            assertEquals(2, program.statements.size());
        }
    }

    // ==================================================================================
    // ERROR HANDLING
    // ==================================================================================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {
        @Test
        void collectSyntaxErrors() {
            List<Token> tokens = new Lexer("SET number = 10").tokenize();
            Parser parser = new Parser(tokens);
            parser.parse();
            assertFalse(parser.errors.isEmpty());
        }

        @Test
        void recoverFromErrors() {
            List<Token> tokens = new Lexer("SET number x = ;").tokenize();
            Parser parser = new Parser(tokens);
            AST.ProgramNode program = parser.parse();
            assertNotNull(program);
        }
    }

    // ==================================================================================
    // CALL EXPRESSIONS
    // ==================================================================================

    @Nested
    @DisplayName("Call Expressions")
    class CallExpressions {
        @Test
        void parseFunctionCallInPrint() {
            AST.ProgramNode program = parse("FUNC void greet() DO\nPRINT(\"Hi\");\nENDFUNC\ngreet();");
            assertEquals(2, program.statements.size());
        }

        @Test
        void parseFunctionCallWithArgs() {
            AST.ProgramNode program = parse(
                    "FUNC number add(number a, number b) DO\nRETURN a + b;\nENDFUNC\nPRINT(add(1, 2));");
            assertEquals(2, program.statements.size());
        }
    }
}
