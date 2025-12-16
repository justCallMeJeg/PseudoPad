package pseudopad.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Interpreter class.
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
class InterpreterTest {

    private List<String> output;

    private Interpreter createInterpreter() {
        Interpreter.InputProvider inputProvider = new Interpreter.InputProvider() {
            @Override
            public String read(String prompt) {
                return "";
            }

            @Override
            public String readPopup(String prompt) {
                return "";
            }
        };
        return new Interpreter(inputProvider, text -> {
            String trimmed = text.trim();
            if (!trimmed.isEmpty()) {
                output.add(trimmed);
            }
        });
    }

    @BeforeEach
    void setUp() {
        output = new ArrayList<>();
    }

    private void run(String code) {
        output.clear();
        Interpreter interpreter = createInterpreter();
        List<Token> tokens = new Lexer(code).tokenize();
        Parser parser = new Parser(tokens);
        AST.ProgramNode program = parser.parse();
        interpreter.run(program);
    }

    private String evaluate(String expression) {
        run("PRINT(" + expression + ");");
        return output.isEmpty() ? null : output.get(0);
    }

    // ==================================================================================
    // ARITHMETIC OPERATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperations {
        @Test
        void addNumbers() {
            assertEquals("5.0", evaluate("2 + 3"));
        }

        @Test
        void subtractNumbers() {
            assertEquals("7.0", evaluate("10 - 3"));
        }

        @Test
        void multiplyNumbers() {
            assertEquals("12.0", evaluate("4 * 3"));
        }

        @Test
        void divideNumbers() {
            assertEquals("5.0", evaluate("10 / 2"));
        }

        @Test
        void calculatePower() {
            assertEquals("8.0", evaluate("2 ^ 3"));
        }

        @Test
        void calculateModulo() {
            assertEquals("1.0", evaluate("10 % 3"));
        }

        @Test
        void respectPrecedence() {
            assertEquals("7.0", evaluate("1 + 2 * 3"));
        }

        @Test
        void handleParentheses() {
            assertEquals("9.0", evaluate("(1 + 2) * 3"));
        }

        @Test
        void handleNegativeNumbers() {
            assertEquals("-5.0", evaluate("-5"));
        }
    }

    // ==================================================================================
    // STRING OPERATIONS
    // ==================================================================================

    @Nested
    @DisplayName("String Operations")
    class StringOperations {
        @Test
        void concatenateStrings() {
            assertEquals("HelloWorld", evaluate("\"Hello\" + \"World\""));
        }

        @Test
        void concatenateStringAndNumber() {
            assertEquals("Value: 42.0", evaluate("\"Value: \" + 42"));
        }
    }

    // ==================================================================================
    // COMPARISON OPERATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Comparison Operations")
    class ComparisonOperations {
        @Test
        void compareEqual() {
            assertEquals("true", evaluate("5 == 5"));
        }

        @Test
        void compareNotEqual() {
            assertEquals("true", evaluate("5 != 3"));
        }

        @Test
        void compareLessThan() {
            assertEquals("true", evaluate("3 < 5"));
        }

        @Test
        void compareGreaterThan() {
            assertEquals("true", evaluate("5 > 3"));
        }

        @Test
        void compareLessOrEqual() {
            assertEquals("true", evaluate("5 <= 5"));
        }

        @Test
        void compareGreaterOrEqual() {
            assertEquals("true", evaluate("5 >= 5"));
        }
    }

    // ==================================================================================
    // LOGICAL OPERATIONS
    // ==================================================================================

    @Nested
    @DisplayName("Logical Operations")
    class LogicalOperations {
        @Test
        void evaluateAnd() {
            assertEquals("true", evaluate("TRUE AND TRUE"));
        }

        @Test
        void evaluateOr() {
            assertEquals("true", evaluate("TRUE OR FALSE"));
        }

        @Test
        void evaluateNot() {
            assertEquals("true", evaluate("NOT FALSE"));
        }

        @Test
        void shortCircuitAnd() {
            assertEquals("false", evaluate("FALSE AND TRUE"));
        }

        @Test
        void shortCircuitOr() {
            assertEquals("true", evaluate("TRUE OR FALSE"));
        }
    }

    // ==================================================================================
    // VARIABLES - using lowercase types
    // ==================================================================================

    @Nested
    @DisplayName("Variables")
    class Variables {
        @Test
        void declareAndUseVariable() {
            run("SET number x = 10;\nPRINT(x);");
            assertEquals("10.0", output.get(0));
        }

        @Test
        void assignNewValue() {
            run("SET number x = 10;\nx = 20;\nPRINT(x);");
            assertEquals("20.0", output.get(0));
        }

        @Test
        void throwOnConstReassignment() {
            assertThrows(Errors.TypeError.class, () -> run("SET CONST number PI = 3.14;\nPI = 3.15;"));
        }

        @Test
        void throwOnUndefinedVariable() {
            assertThrows(Errors.TypeError.class, () -> run("PRINT(undefinedVar);"));
        }
    }

    // ==================================================================================
    // CONTROL FLOW
    // ==================================================================================

    @Nested
    @DisplayName("Control Flow")
    class ControlFlow {
        @Test
        void executeIfTrue() {
            run("IF (TRUE) THEN\nPRINT(\"yes\");\nENDIF");
            assertEquals("yes", output.get(0));
        }

        @Test
        void skipIfFalse() {
            run("IF (FALSE) THEN\nPRINT(\"yes\");\nENDIF");
            assertTrue(output.isEmpty());
        }

        @Test
        void executeElse() {
            run("IF (FALSE) THEN\nPRINT(\"yes\");\nELSE\nPRINT(\"no\");\nENDIF");
            assertEquals("no", output.get(0));
        }

        @Test
        void executeWhileLoop() {
            run("SET number i = 0;\nWHILE (i < 3) DO\nPRINT(i);\ni = i + 1;\nENDWHILE");
            assertEquals(3, output.size());
        }

        @Test
        void executeForLoop() {
            run("FOR (SET number i = 0; i < 3; i = i + 1) DO\nPRINT(i);\nENDFOR");
            assertEquals(3, output.size());
        }

        @Test
        void handleBreak() {
            run("SET number i = 0;\nWHILE (TRUE) DO\nIF (i == 2) THEN\nBREAK;\nENDIF\nPRINT(i);\ni = i + 1;\nENDWHILE");
            assertEquals(2, output.size());
        }
    }

    // ==================================================================================
    // FUNCTIONS - need DO keyword
    // ==================================================================================

    @Nested
    @DisplayName("Functions")
    class Functions {
        @Test
        void callFunctionNoParams() {
            run("FUNC void greet() DO\nPRINT(\"Hello\");\nENDFUNC\ngreet();");
            assertEquals("Hello", output.get(0));
        }

        @Test
        void callFunctionWithParams() {
            run("FUNC void greet(string name) DO\nPRINT(\"Hello \" + name);\nENDFUNC\ngreet(\"World\");");
            assertEquals("Hello World", output.get(0));
        }

        @Test
        void returnValueFromFunction() {
            run("FUNC number add(number a, number b) DO\nRETURN a + b;\nENDFUNC\nPRINT(add(2, 3));");
            assertEquals("5.0", output.get(0));
        }

        @Test
        void handleRecursiveFunction() {
            run("FUNC number factorial(number n) DO\nIF (n <= 1) THEN\nRETURN 1;\nENDIF\nRETURN n * factorial(n - 1);\nENDFUNC\nPRINT(factorial(5));");
            assertEquals("120.0", output.get(0));
        }
    }

    // ==================================================================================
    // LISTS - using lowercase type
    // ==================================================================================

    @Nested
    @DisplayName("Lists")
    class Lists {
        @Test
        void createAndAccessList() {
            run("SET list nums = [1, 2, 3];\nPRINT(nums[0]);");
            assertEquals("1.0", output.get(0));
        }

        @Test
        void modifyListElement() {
            run("SET list nums = [1, 2, 3];\nnums[0] = 10;\nPRINT(nums[0]);");
            assertEquals("10.0", output.get(0));
        }
    }

    // ==================================================================================
    // DICTIONARIES - using lowercase type
    // ==================================================================================

    @Nested
    @DisplayName("Dictionaries")
    class Dictionaries {
        @Test
        void createAndAccessDict() {
            run("SET dict person = {\"name\": \"John\"};\nPRINT(person[\"name\"]);");
            assertEquals("John", output.get(0));
        }

        @Test
        void modifyDictValue() {
            run("SET dict person = {\"age\": 25};\nperson[\"age\"] = 30;\nPRINT(person[\"age\"]);");
            assertEquals("30.0", output.get(0));
        }
    }

    // ==================================================================================
    // PRINT STATEMENT
    // ==================================================================================

    @Nested
    @DisplayName("Print Statement")
    class PrintStatement {
        @Test
        void printStringLiteral() {
            run("PRINT(\"Hello World\");");
            assertEquals("Hello World", output.get(0));
        }

        @Test
        void printNumber() {
            run("PRINT(42);");
            assertEquals("42.0", output.get(0));
        }

        @Test
        void printBoolean() {
            run("PRINT(TRUE);");
            assertEquals("true", output.get(0));
        }

        @Test
        void printNull() {
            run("PRINT(NULL);");
            assertEquals("null", output.get(0));
        }
    }
}
