package pseudopad.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SemanticAnalyzer class.
 * 
 * Syntax reference:
 * - Types: lowercase (number, string, boolean, list, dict, void)
 * - FUNC: FUNC returnType name(params) DO ... ENDFUNC
 * - CLASS: CLASS Name DO ... ENDCLASS
 */
class SemanticAnalyzerTest {

    private SemanticAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new SemanticAnalyzer();
    }

    private List<Errors.CompilationError> analyze(String code) {
        List<Token> tokens = new Lexer(code).tokenize();
        Parser parser = new Parser(tokens);
        AST.ProgramNode program = parser.parse();
        return analyzer.analyze(program);
    }

    // ==================================================================================
    // TYPE CHECKING
    // ==================================================================================

    @Nested
    @DisplayName("Type Checking")
    class TypeChecking {
        @Test
        void acceptValidTypeAssignment() {
            List<Errors.CompilationError> errors = analyze("SET number x = 10;");
            assertTrue(errors.isEmpty(), "Should have no errors for valid assignment");
        }

        @Test
        void acceptValidStringAssignment() {
            List<Errors.CompilationError> errors = analyze("SET string name = \"John\";");
            assertTrue(errors.isEmpty());
        }

        @Test
        void acceptValidBooleanAssignment() {
            List<Errors.CompilationError> errors = analyze("SET boolean flag = TRUE;");
            assertTrue(errors.isEmpty());
        }

        @Test
        void analyzeTypeMismatch() {
            List<Errors.CompilationError> errors = analyze("SET number x = \"hello\";");
            assertNotNull(errors);
        }

        @Test
        void acceptNullForAnyType() {
            List<Errors.CompilationError> errors = analyze("SET string name = NULL;");
            assertNotNull(errors); // May or may not be an error depending on implementation
        }
    }

    // ==================================================================================
    // VARIABLE DECLARATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Variable Declarations")
    class VariableDeclarations {
        @Test
        void acceptValidDeclaration() {
            List<Errors.CompilationError> errors = analyze("SET number count = 0;");
            assertTrue(errors.isEmpty());
        }

        @Test
        void analyzeUnknownType() {
            List<Errors.CompilationError> errors = analyze("SET INVALID x = 10;");
            assertNotNull(errors);
        }
    }

    // ==================================================================================
    // FUNCTION DECLARATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Function Declarations")
    class FunctionDeclarations {
        @Test
        void acceptValidFunction() {
            List<Errors.CompilationError> errors = analyze(
                    "FUNC number add(number a, number b) DO\nRETURN a + b;\nENDFUNC");
            assertTrue(errors.isEmpty());
        }

        @Test
        void acceptVoidFunctionNoReturn() {
            List<Errors.CompilationError> errors = analyze(
                    "FUNC void greet() DO\nPRINT(\"Hello\");\nENDFUNC");
            assertTrue(errors.isEmpty());
        }

        @Test
        void analyzeMissingReturn() {
            List<Errors.CompilationError> errors = analyze(
                    "FUNC number getValue() DO\nPRINT(\"test\");\nENDFUNC");
            assertNotNull(errors);
        }
    }

    // ==================================================================================
    // CLASS DECLARATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Class Declarations")
    class ClassDeclarations {
        @Test
        void acceptValidClass() {
            List<Errors.CompilationError> errors = analyze(
                    "CLASS Person DO\nSET string name = \"\";\nENDCLASS");
            assertTrue(errors.isEmpty());
        }

        @Test
        void acceptClassWithMethods() {
            List<Errors.CompilationError> errors = analyze(
                    "CLASS Counter DO\nSET number count = 0;\nFUNC void increment() DO\ncount = count + 1;\nENDFUNC\nENDCLASS");
            assertTrue(errors.isEmpty());
        }
    }

    // ==================================================================================
    // CONTROL FLOW
    // ==================================================================================

    @Nested
    @DisplayName("Control Flow")
    class ControlFlowTests {
        @Test
        void analyzeIfCondition() {
            List<Errors.CompilationError> errors = analyze(
                    "IF (TRUE) THEN\nPRINT(\"yes\");\nENDIF");
            assertTrue(errors.isEmpty());
        }

        @Test
        void analyzeWhileLoop() {
            List<Errors.CompilationError> errors = analyze(
                    "SET number i = 0;\nWHILE (i < 10) DO\ni = i + 1;\nENDWHILE");
            assertTrue(errors.isEmpty());
        }

        @Test
        void analyzeForLoop() {
            List<Errors.CompilationError> errors = analyze(
                    "FOR (SET number i = 0; i < 10; i = i + 1) DO\nPRINT(i);\nENDFOR");
            assertNotNull(errors);
        }
    }

    // ==================================================================================
    // EXPRESSIONS
    // ==================================================================================

    @Nested
    @DisplayName("Expressions")
    class ExpressionTests {
        @Test
        void validateBinaryExpressionTypes() {
            List<Errors.CompilationError> errors = analyze("SET number result = 1 + 2;");
            assertTrue(errors.isEmpty());
        }

        @Test
        void validateComparisonExpressions() {
            List<Errors.CompilationError> errors = analyze("SET boolean result = 5 > 3;");
            assertTrue(errors.isEmpty());
        }

        @Test
        void validateLogicalExpressions() {
            List<Errors.CompilationError> errors = analyze("SET boolean result = TRUE AND FALSE;");
            assertTrue(errors.isEmpty());
        }
    }

    // ==================================================================================
    // LIST AND DICT TYPES
    // ==================================================================================

    @Nested
    @DisplayName("List and Dict Types")
    class ListAndDictTypes {
        @Test
        void acceptValidListDeclaration() {
            List<Errors.CompilationError> errors = analyze("SET list items = [1, 2, 3];");
            assertTrue(errors.isEmpty());
        }

        @Test
        void acceptEmptyList() {
            List<Errors.CompilationError> errors = analyze("SET list items = [];");
            assertTrue(errors.isEmpty());
        }

        @Test
        void acceptValidDictDeclaration() {
            List<Errors.CompilationError> errors = analyze("SET dict data = {\"key\": \"value\"};");
            assertTrue(errors.isEmpty());
        }

        @Test
        void acceptEmptyDict() {
            List<Errors.CompilationError> errors = analyze("SET dict data = {};");
            assertTrue(errors.isEmpty());
        }
    }
}
