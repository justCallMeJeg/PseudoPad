package pseudopad.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Lexer class.
 * Tests tokenization of pseudocode language constructs.
 */
class LexerTest {

    // ==================================================================================
    // HELPER METHODS
    // ==================================================================================

    private List<Token> tokenize(String input) {
        return new Lexer(input).tokenize();
    }

    private Token firstToken(String input) {
        return tokenize(input).get(0);
    }

    // ==================================================================================
    // LITERALS
    // ==================================================================================

    @Nested
    @DisplayName("Number Literals")
    class NumberLiterals {
        @Test
        @DisplayName("Should tokenize integer")
        void tokenizeInteger() {
            Token token = firstToken("42");
            assertEquals(TokenType.NUMBER, token.type);
            assertEquals("42", token.value);
        }

        @Test
        @DisplayName("Should tokenize decimal number")
        void tokenizeDecimal() {
            Token token = firstToken("3.14");
            assertEquals(TokenType.NUMBER, token.type);
            assertEquals("3.14", token.value);
        }

        @Test
        @DisplayName("Should tokenize zero")
        void tokenizeZero() {
            Token token = firstToken("0");
            assertEquals(TokenType.NUMBER, token.type);
            assertEquals("0", token.value);
        }
    }

    @Nested
    @DisplayName("String Literals")
    class StringLiterals {
        @Test
        @DisplayName("Should tokenize simple string")
        void tokenizeSimpleString() {
            Token token = firstToken("\"hello\"");
            assertEquals(TokenType.STRING, token.type);
            assertEquals("hello", token.value);
        }

        @Test
        @DisplayName("Should tokenize empty string")
        void tokenizeEmptyString() {
            Token token = firstToken("\"\"");
            assertEquals(TokenType.STRING, token.type);
            assertEquals("", token.value);
        }

        @Test
        @DisplayName("Should tokenize string with spaces")
        void tokenizeStringWithSpaces() {
            Token token = firstToken("\"hello world\"");
            assertEquals(TokenType.STRING, token.type);
            assertEquals("hello world", token.value);
        }

        @Test
        @DisplayName("Should throw on unterminated string")
        void throwOnUnterminatedString() {
            assertThrows(Errors.LexerError.class, () -> tokenize("\"unterminated"));
        }
    }

    @Nested
    @DisplayName("Boolean Literals")
    class BooleanLiterals {
        @Test
        @DisplayName("Should tokenize TRUE")
        void tokenizeTrue() {
            Token token = firstToken("TRUE");
            assertEquals(TokenType.BOOLEAN, token.type);
            assertEquals("TRUE", token.value);
        }

        @Test
        @DisplayName("Should tokenize FALSE")
        void tokenizeFalse() {
            Token token = firstToken("FALSE");
            assertEquals(TokenType.BOOLEAN, token.type);
            assertEquals("FALSE", token.value);
        }
    }

    @Nested
    @DisplayName("Null Literal")
    class NullLiteral {
        @Test
        @DisplayName("Should tokenize NULL")
        void tokenizeNull() {
            Token token = firstToken("NULL");
            assertEquals(TokenType.NULL, token.type);
        }
    }

    // ==================================================================================
    // KEYWORDS
    // ==================================================================================

    @Nested
    @DisplayName("Keywords")
    class Keywords {
        @Test
        @DisplayName("Should tokenize SET keyword")
        void tokenizeSet() {
            assertEquals(TokenType.SET, firstToken("SET").type);
        }

        @Test
        @DisplayName("Should tokenize CONST keyword")
        void tokenizeConst() {
            assertEquals(TokenType.CONST, firstToken("CONST").type);
        }

        @Test
        @DisplayName("Should tokenize PRINT keyword")
        void tokenizePrint() {
            assertEquals(TokenType.PRINT, firstToken("PRINT").type);
        }

        @Test
        @DisplayName("Should tokenize IF keyword")
        void tokenizeIf() {
            assertEquals(TokenType.IF, firstToken("IF").type);
        }

        @Test
        @DisplayName("Should tokenize WHILE keyword")
        void tokenizeWhile() {
            assertEquals(TokenType.WHILE, firstToken("WHILE").type);
        }

        @Test
        @DisplayName("Should tokenize FOR keyword")
        void tokenizeFor() {
            assertEquals(TokenType.FOR, firstToken("FOR").type);
        }

        @Test
        @DisplayName("Should tokenize FUNC keyword")
        void tokenizeFunc() {
            assertEquals(TokenType.FUNC, firstToken("FUNC").type);
        }

        @Test
        @DisplayName("Should tokenize CLASS keyword")
        void tokenizeClass() {
            assertEquals(TokenType.CLASS, firstToken("CLASS").type);
        }

        @Test
        @DisplayName("Should tokenize RETURN keyword")
        void tokenizeReturn() {
            assertEquals(TokenType.RETURN, firstToken("RETURN").type);
        }
    }

    // ==================================================================================
    // OPERATORS
    // ==================================================================================

    @Nested
    @DisplayName("Arithmetic Operators")
    class ArithmeticOperators {
        @Test
        @DisplayName("Should tokenize plus")
        void tokenizePlus() {
            assertEquals(TokenType.PLUS, firstToken("+").type);
        }

        @Test
        @DisplayName("Should tokenize minus")
        void tokenizeMinus() {
            assertEquals(TokenType.MINUS, firstToken("-").type);
        }

        @Test
        @DisplayName("Should tokenize multiply")
        void tokenizeMultiply() {
            assertEquals(TokenType.MULT, firstToken("*").type);
        }

        @Test
        @DisplayName("Should tokenize divide")
        void tokenizeDivide() {
            assertEquals(TokenType.DIV, firstToken("/").type);
        }

        @Test
        @DisplayName("Should tokenize modulo")
        void tokenizeModulo() {
            assertEquals(TokenType.MOD, firstToken("%").type);
        }

        @Test
        @DisplayName("Should tokenize power")
        void tokenizePower() {
            assertEquals(TokenType.CARET, firstToken("^").type);
        }
    }

    @Nested
    @DisplayName("Comparison Operators")
    class ComparisonOperators {
        @Test
        @DisplayName("Should tokenize equal equal")
        void tokenizeEqualEqual() {
            assertEquals(TokenType.EQUAL_EQUAL, firstToken("== ").type);
        }

        @Test
        @DisplayName("Should tokenize not equal")
        void tokenizeNotEqual() {
            assertEquals(TokenType.BANG_EQUAL, firstToken("!=").type);
        }

        @Test
        @DisplayName("Should tokenize less than")
        void tokenizeLessThan() {
            assertEquals(TokenType.LESS, firstToken("< ").type);
        }

        @Test
        @DisplayName("Should tokenize less or equal")
        void tokenizeLessOrEqual() {
            assertEquals(TokenType.LESS_EQUAL, firstToken("<=").type);
        }

        @Test
        @DisplayName("Should tokenize greater than")
        void tokenizeGreaterThan() {
            assertEquals(TokenType.GREATER, firstToken("> ").type);
        }

        @Test
        @DisplayName("Should tokenize greater or equal")
        void tokenizeGreaterOrEqual() {
            assertEquals(TokenType.GREATER_EQUAL, firstToken(">=").type);
        }
    }

    @Nested
    @DisplayName("Logical Operators")
    class LogicalOperators {
        @Test
        @DisplayName("Should tokenize AND")
        void tokenizeAnd() {
            assertEquals(TokenType.AND, firstToken("AND").type);
        }

        @Test
        @DisplayName("Should tokenize OR")
        void tokenizeOr() {
            assertEquals(TokenType.OR, firstToken("OR").type);
        }

        @Test
        @DisplayName("Should tokenize NOT")
        void tokenizeNot() {
            assertEquals(TokenType.NOT, firstToken("NOT").type);
        }
    }

    // ==================================================================================
    // IDENTIFIERS
    // ==================================================================================

    @Nested
    @DisplayName("Identifiers")
    class Identifiers {
        @Test
        @DisplayName("Should tokenize simple identifier")
        void tokenizeSimpleIdentifier() {
            Token token = firstToken("myVar");
            assertEquals(TokenType.IDENTIFIER, token.type);
            assertEquals("myVar", token.value);
        }

        @Test
        @DisplayName("Should tokenize identifier with underscore")
        void tokenizeIdentifierWithUnderscore() {
            Token token = firstToken("my_var");
            assertEquals(TokenType.IDENTIFIER, token.type);
            assertEquals("my_var", token.value);
        }

        @Test
        @DisplayName("Should tokenize identifier with numbers")
        void tokenizeIdentifierWithNumbers() {
            Token token = firstToken("var123");
            assertEquals(TokenType.IDENTIFIER, token.type);
            assertEquals("var123", token.value);
        }
    }

    // ==================================================================================
    // COMMENTS
    // ==================================================================================

    @Nested
    @DisplayName("Comments")
    class Comments {
        @Test
        @DisplayName("Should tokenize comment")
        void tokenizeComment() {
            List<Token> tokens = tokenize("#this is a comment#");
            assertEquals(TokenType.COMMENT, tokens.get(0).type);
        }

        @Test
        @DisplayName("Should throw on unterminated comment")
        void throwOnUnterminatedComment() {
            assertThrows(Errors.LexerError.class, () -> tokenize("#unterminated comment"));
        }
    }

    // ==================================================================================
    // DELIMITERS
    // ==================================================================================

    @Nested
    @DisplayName("Delimiters")
    class Delimiters {
        @Test
        @DisplayName("Should tokenize parentheses")
        void tokenizeParentheses() {
            List<Token> tokens = tokenize("()");
            assertEquals(TokenType.LPAREN, tokens.get(0).type);
            assertEquals(TokenType.RPAREN, tokens.get(1).type);
        }

        @Test
        @DisplayName("Should tokenize brackets")
        void tokenizeBrackets() {
            List<Token> tokens = tokenize("[]");
            assertEquals(TokenType.LBRACKET, tokens.get(0).type);
            assertEquals(TokenType.RBRACKET, tokens.get(1).type);
        }

        @Test
        @DisplayName("Should tokenize braces")
        void tokenizeBraces() {
            List<Token> tokens = tokenize("{}");
            assertEquals(TokenType.LBRACE, tokens.get(0).type);
            assertEquals(TokenType.RBRACE, tokens.get(1).type);
        }

        @Test
        @DisplayName("Should tokenize comma")
        void tokenizeComma() {
            assertEquals(TokenType.COMMA, firstToken(",").type);
        }

        @Test
        @DisplayName("Should tokenize colon")
        void tokenizeColon() {
            assertEquals(TokenType.COLON, firstToken(":").type);
        }

        @Test
        @DisplayName("Should tokenize dot")
        void tokenizeDot() {
            assertEquals(TokenType.DOT, firstToken(".").type);
        }

        @Test
        @DisplayName("Should tokenize semicolon")
        void tokenizeSemicolon() {
            assertEquals(TokenType.SEMICOLON, firstToken(";").type);
        }
    }

    // ==================================================================================
    // COMPLETE STATEMENTS
    // ==================================================================================

    @Nested
    @DisplayName("Complete Statements")
    class CompleteStatements {
        @Test
        @DisplayName("Should tokenize variable declaration")
        void tokenizeVariableDeclaration() {
            List<Token> tokens = tokenize("SET NUMBER x = 10");
            assertEquals(TokenType.SET, tokens.get(0).type);
            assertEquals(TokenType.TYPE, tokens.get(1).type);
            assertEquals(TokenType.IDENTIFIER, tokens.get(2).type);
            assertEquals(TokenType.EQUALS, tokens.get(3).type);
            assertEquals(TokenType.NUMBER, tokens.get(4).type);
            assertEquals(TokenType.EOF, tokens.get(5).type);
        }

        @Test
        @DisplayName("Should tokenize print statement")
        void tokenizePrintStatement() {
            List<Token> tokens = tokenize("PRINT \"Hello\"");
            assertEquals(TokenType.PRINT, tokens.get(0).type);
            assertEquals(TokenType.STRING, tokens.get(1).type);
            assertEquals(TokenType.EOF, tokens.get(2).type);
        }

        @Test
        @DisplayName("Should tokenize arithmetic expression")
        void tokenizeArithmeticExpression() {
            List<Token> tokens = tokenize("1 + 2 * 3");
            assertEquals(TokenType.NUMBER, tokens.get(0).type);
            assertEquals(TokenType.PLUS, tokens.get(1).type);
            assertEquals(TokenType.NUMBER, tokens.get(2).type);
            assertEquals(TokenType.MULT, tokens.get(3).type);
            assertEquals(TokenType.NUMBER, tokens.get(4).type);
        }
    }

    // ==================================================================================
    // EOF
    // ==================================================================================

    @Nested
    @DisplayName("End of File")
    class EndOfFile {
        @Test
        @DisplayName("Should add EOF token at end")
        void addEofToken() {
            List<Token> tokens = tokenize("");
            assertEquals(1, tokens.size());
            assertEquals(TokenType.EOF, tokens.get(0).type);
        }

        @Test
        @DisplayName("Should have EOF as last token")
        void eofAsLastToken() {
            List<Token> tokens = tokenize("SET");
            assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).type);
        }
    }
}
