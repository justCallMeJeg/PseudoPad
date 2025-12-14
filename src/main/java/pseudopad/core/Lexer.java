package pseudopad.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Joseph Mikhaeli Jalandoni
 */
public class Lexer {
    private final String text;
    private int index = 0;
    private int line = 1;
    private int column = 1;

    public Lexer(String text) {
        this.text = text;
    }

    private char currentChar() {
        if (index >= text.length())
            return '\0';
        return text.charAt(index);
    }

    private void advance() {
        if (currentChar() == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        index++;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (true) {
            char c = currentChar();

            if (c == '\0') {
                tokens.add(new Token(TokenType.EOF, null, line, column, index, 0));
                break;
            }

            if (Character.isWhitespace(c)) {
                advance();
                continue;
            }

            if (Character.isDigit(c)) {
                tokens.add(numberToken());
                continue;
            }

            if (c == '"') {
                tokens.add(stringToken());
                continue;
            }

            if (Character.isLetter(c)) {
                tokens.add(identifierOrKeyword());
                continue;
            }

            switch (c) {
                case '=' -> {
                    int start = index; // Start of '='
                    advance();
                    switch (currentChar()) {
                        case '=' -> {
                            // "==" (Length 2)
                            tokens.add(new Token(TokenType.EQUAL_EQUAL, null, line, column, start, 2));
                            advance(); // Consume 2nd '='
                            continue;
                        }
                        case ' ', '(', '"', '\0' -> {
                            // "=" (Length 1)
                            tokens.add(new Token(TokenType.EQUALS, null, line, column, start, 1));
                            continue;
                        }
                    }

                    throw new Errors.LexerError(
                            "Unexpected character: =" + currentChar(), line, column);
                }
                case '!' -> {
                    int start = index;
                    advance();
                    if (currentChar() == '=') {
                        tokens.add(new Token(TokenType.BANG_EQUAL, null, line, column, start, 2));
                        advance();
                        continue;
                    } else {
                        throw new Errors.LexerError(
                                "Unexpected character: !" + currentChar(), line, column);
                    }
                }
                case '<' -> {
                    int start = index;
                    advance();
                    switch (currentChar()) {
                        case '=' -> {
                            tokens.add(new Token(TokenType.LESS_EQUAL, null, line, column, start, 2));
                            advance();
                            continue;
                        }
                        case ' ', '(', '"', '\0' -> {
                            tokens.add(new Token(TokenType.LESS, null, line, column, start, 1));
                            continue;
                        }
                    }

                    throw new Errors.LexerError(
                            "Unexpected character: <" + currentChar(), line, column);
                }
                case '>' -> {
                    int start = index;
                    advance();
                    switch (currentChar()) {
                        case '=' -> {
                            tokens.add(new Token(TokenType.GREATER_EQUAL, null, line, column, start, 2));
                            advance();
                            continue;
                        }
                        case ' ', '(', '"', '\0' -> {
                            tokens.add(new Token(TokenType.GREATER, null, line, column, start, 1));
                            continue;
                        }
                    }

                    throw new Errors.LexerError(
                            "Unexpected character: >" + currentChar(), line, column);
                }
                case ';' ->

                {
                    tokens.add(simple(TokenType.SEMICOLON));
                    continue;
                }
                case '(' -> {
                    tokens.add(simple(TokenType.LPAREN));
                    continue;
                }
                case ')' -> {
                    tokens.add(simple(TokenType.RPAREN));
                    continue;
                }
                case '+' -> {
                    tokens.add(simple(TokenType.PLUS));
                    continue;
                }
                case '-' -> {
                    tokens.add(simple(TokenType.MINUS));
                    continue;
                }
                case '*' -> {
                    tokens.add(simple(TokenType.MULT));
                    continue;
                }
                case '/' -> {
                    tokens.add(simple(TokenType.DIV));
                    continue;
                }
                case '%' -> {
                    tokens.add(simple(TokenType.MOD));
                    continue;
                }
                case '^' -> {
                    tokens.add(simple(TokenType.CARET));
                    continue;
                }
                case '[' -> {
                    tokens.add(simple(TokenType.LBRACKET));
                    continue;
                }
                case ']' -> {
                    tokens.add(simple(TokenType.RBRACKET));
                    continue;
                }
                case '{' -> {
                    tokens.add(simple(TokenType.LBRACE));
                    continue;
                }
                case '}' -> {
                    tokens.add(simple(TokenType.RBRACE));
                    continue;
                }
                case ',' -> {
                    tokens.add(simple(TokenType.COMMA));
                    continue;
                }
                case ':' -> {
                    tokens.add(simple(TokenType.COLON));
                    continue;
                }
                case '.' -> {
                    tokens.add(simple(TokenType.DOT));
                    continue;
                }
                case '#' -> {
                    int start = index;
                    advance(); // Consume the opening '#'

                    // Consume characters until we hit the closing '#' or End Of File
                    while (currentChar() != '#' && currentChar() != '\0') {
                        advance();
                    }

                    // Check for unterminated comment (EOF before closing #)
                    if (currentChar() == '\0') {
                        throw new Errors.LexerError("Unterminated comment", line, column);
                    }

                    advance(); // Consume the closing '#'

                    // Add comments as tokens for Syntax Highlighting
                    int length = index - start;
                    tokens.add(new Token(TokenType.COMMENT, null, line, column, start, length));
                    continue;
                }
            }

            throw new Errors.LexerError("Unexpected character: " + c, line, column);
        }

        return tokens;
    }

    private Token numberToken() {
        int startColumn = column;
        StringBuilder builder = new StringBuilder();

        while (Character.isDigit(currentChar())) {
            builder.append(currentChar());
            advance();
        }

        if (currentChar() == '.') {
            builder.append(".");
            advance();
            while (Character.isDigit(currentChar())) {
                builder.append(currentChar());
                advance();
            }
        }

        return new Token(TokenType.NUMBER, builder.toString(), line, startColumn, index - builder.length(),
                builder.length());
    }

    private Token stringToken() {
        int startColumn = column;
        advance();

        StringBuilder builder = new StringBuilder();

        while (currentChar() != '"' && currentChar() != '\0') {
            builder.append(currentChar());
            advance();
        }

        if (currentChar() != '"') {
            throw new Errors.LexerError("Unterminated string", line, column);
        }

        advance();

        return new Token(TokenType.STRING, builder.toString(), line, startColumn, index - (builder.length() + 2),
                builder.length() + 2);
    }

    private static final java.util.Map<String, TokenType> keywords;

    static {
        keywords = new java.util.HashMap<>();
        keywords.put("SET", TokenType.SET);
        keywords.put("CONST", TokenType.CONST);
        keywords.put("PRINT", TokenType.PRINT);
        keywords.put("TRUE", TokenType.BOOLEAN);
        keywords.put("FALSE", TokenType.BOOLEAN);
        keywords.put("NUMBER", TokenType.TYPE);
        keywords.put("STRING", TokenType.TYPE);
        keywords.put("BOOLEAN", TokenType.TYPE);
        keywords.put("LIST", TokenType.TYPE);
        keywords.put("DICT", TokenType.TYPE);
        keywords.put("VOID", TokenType.TYPE);
        keywords.put("AND", TokenType.AND);
        keywords.put("OR", TokenType.OR);
        keywords.put("NOT", TokenType.NOT);
        keywords.put("IF", TokenType.IF);
        keywords.put("THEN", TokenType.THEN);
        keywords.put("ELIF", TokenType.ELIF);
        keywords.put("ELSE", TokenType.ELSE);
        keywords.put("ENDIF", TokenType.ENDIF);
        keywords.put("WHILE", TokenType.WHILE);
        keywords.put("DO", TokenType.DO);
        keywords.put("ENDWHILE", TokenType.ENDWHILE);
        keywords.put("FOR", TokenType.FOR);
        keywords.put("ENDFOR", TokenType.ENDFOR);
        keywords.put("SKIP", TokenType.SKIP);
        keywords.put("CONTINUE", TokenType.SKIP);
        keywords.put("BREAK", TokenType.BREAK);
        keywords.put("FUNC", TokenType.FUNC);
        keywords.put("ENDFUNC", TokenType.ENDFUNC);
        keywords.put("RETURN", TokenType.RETURN);
        keywords.put("CLASS", TokenType.CLASS);
        keywords.put("ENDCLASS", TokenType.ENDCLASS);
        keywords.put("THIS", TokenType.THIS);
    }

    private Token identifierOrKeyword() {
        int startColumn = column;
        StringBuilder builder = new StringBuilder();

        while (currentChar() != '\0' && Character.isJavaIdentifierPart(currentChar())) {
            builder.append(currentChar());
            advance();
        }

        String word = builder.toString();
        TokenType type = keywords.get(word.toUpperCase());

        if (type == null) {
            type = TokenType.IDENTIFIER;
        }

        // Special handling for boolean literals to keep 'word' value, others might not
        // need value
        if (type == TokenType.BOOLEAN) {
            return new Token(type, word, line, startColumn, index - word.length(), word.length());
        }

        // Special handling for Types to keep 'word' value
        if (type == TokenType.TYPE) {
            return new Token(type, word, line, startColumn, index - word.length(), word.length());
        }

        return new Token(type, (type == TokenType.IDENTIFIER) ? word : null, line, startColumn, index - word.length(),
                word.length());
    }

    private Token simple(TokenType type) {
        int startIndex = index;
        Token token = new Token(type, null, line, column, startIndex, 1);
        advance();
        return token;
    }
}