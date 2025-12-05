package pseudopad.language;

import java.util.ArrayList;
import java.util.List;

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
                    int start = index;
                    int col = column;
                    advance();
                    if (currentChar() == '=') {
                        advance();
                        tokens.add(new Token(TokenType.EQUAL_EQUAL, null, line, col, start, 2));
                    } else {
                        tokens.add(new Token(TokenType.EQUALS, null, line, col, start, 1));
                    }
                    continue;
                }
                case '!' -> {
                    int start = index;
                    int col = column;
                    advance();
                    if (currentChar() == '=') {
                        advance();
                        tokens.add(new Token(TokenType.BANG_EQUAL, null, line, col, start, 2));
                    } else {
                        throw new Errors.LexerError(
                                "Unexpected character: !" + currentChar() + " at " + line + ":" + column);
                    }
                    continue;
                }
                case '<' -> {
                    int start = index;
                    int col = column;
                    advance();
                    if (currentChar() == '=') {
                        advance();
                        tokens.add(new Token(TokenType.LESS_EQUAL, null, line, col, start, 2));
                    } else {
                        tokens.add(new Token(TokenType.LESS, null, line, col, start, 1));
                    }
                    continue;
                }
                case '>' -> {
                    int start = index;
                    int col = column;
                    advance();
                    if (currentChar() == '=') {
                        advance();
                        tokens.add(new Token(TokenType.GREATER_EQUAL, null, line, col, start, 2));
                    } else {
                        tokens.add(new Token(TokenType.GREATER, null, line, col, start, 1));
                    }
                    continue;
                }
                case ';' -> {
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
                    int col = column;
                    advance(); // Consume the opening '#'

                    // Consume characters until we hit the closing '#' or End Of File
                    while (currentChar() != '#' && currentChar() != '\0') {
                        advance();
                    }

                    // Check for unterminated comment (EOF before closing #)
                    if (currentChar() == '\0') {
                        throw new Errors.LexerError("Unterminated comment at line " + line);
                    }

                    advance(); // Consume the closing '#'
                    tokens.add(new Token(TokenType.COMMENT, null, line, col, start, index - start));
                    continue;
                }
            }

            throw new RuntimeException("Unexpected character: " + c + " at " + line + ":" + column);
        }

        return tokens;
    }

    private Token numberToken() {
        int start = index;
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

        return new Token(TokenType.NUMBER, builder.toString(), line, startColumn, start, index - start);
    }

    private Token stringToken() {
        int start = index;
        int startColumn = column;
        advance();

        StringBuilder builder = new StringBuilder();

        while (currentChar() != '"' && currentChar() != '\0') {
            builder.append(currentChar());
            advance();
        }

        if (currentChar() != '"') {
            throw new RuntimeException("Unterminated string at line " + line);
        }

        advance();

        return new Token(TokenType.STRING, builder.toString(), line, startColumn, start, index - start);
    }

    private Token identifierOrKeyword() {
        int start = index;
        int startColumn = column;
        StringBuilder builder = new StringBuilder();

        while (Character.isJavaIdentifierPart(currentChar()) && currentChar() != '\0') {
            builder.append(currentChar());
            advance();
        }

        String word = builder.toString();
        int length = index - start;
        switch (word.toUpperCase()) {
            case "SET" -> {
                return new Token(TokenType.SET, null, line, startColumn, start, length);
            }
            case "CONST" -> {
                return new Token(TokenType.CONST, null, line, startColumn, start, length);
            }
            case "PRINT" -> {
                return new Token(TokenType.PRINT, null, line, startColumn, start, length);
            }
            case "TRUE", "FALSE" -> {
                return new Token(TokenType.BOOLEAN, word, line, startColumn, start, length);
            }
            case "NUMBER", "STRING", "BOOLEAN", "LIST", "DICT", "VOID" -> {
                return new Token(TokenType.TYPE, word, line, startColumn, start, length);
            }
            case "AND" -> {
                return new Token(TokenType.AND, null, line, startColumn, start, length);
            }
            case "OR" -> {
                return new Token(TokenType.OR, null, line, startColumn, start, length);
            }
            case "NOT" -> {
                return new Token(TokenType.NOT, null, line, startColumn, start, length);
            }
            case "IF" -> {
                return new Token(TokenType.IF, null, line, startColumn, start, length);
            }
            case "THEN" -> {
                return new Token(TokenType.THEN, null, line, startColumn, start, length);
            }
            case "ELIF" -> {
                return new Token(TokenType.ELIF, null, line, startColumn, start, length);
            }
            case "ELSE" -> {
                return new Token(TokenType.ELSE, null, line, startColumn, start, length);
            }
            case "ENDIF" -> {
                return new Token(TokenType.ENDIF, null, line, startColumn, start, length);
            }
            case "WHILE" -> {
                return new Token(TokenType.WHILE, null, line, startColumn, start, length);
            }
            case "DO" -> {
                return new Token(TokenType.DO, null, line, startColumn, start, length);
            }
            case "ENDWHILE" -> {
                return new Token(TokenType.ENDWHILE, null, line, startColumn, start, length);
            }
            case "FOR" -> {
                return new Token(TokenType.FOR, null, line, startColumn, start, length);
            }
            case "ENDFOR" -> {
                return new Token(TokenType.ENDFOR, null, line, startColumn, start, length);
            }
            case "SKIP", "CONTINUE" -> {
                return new Token(TokenType.SKIP, null, line, startColumn, start, length);
            }
            case "BREAK" -> {
                return new Token(TokenType.BREAK, null, line, startColumn, start, length);
            }
            case "FUNC" -> {
                return new Token(TokenType.FUNC, null, line, startColumn, start, length);
            }
            case "ENDFUNC" -> {
                return new Token(TokenType.ENDFUNC, null, line, startColumn, start, length);
            }
            case "RETURN" -> {
                return new Token(TokenType.RETURN, null, line, startColumn, start, length);
            }
            case "CLASS" -> {
                return new Token(TokenType.CLASS, null, line, startColumn, start, length);
            }
            case "ENDCLASS" -> {
                return new Token(TokenType.ENDCLASS, null, line, startColumn, start, length);
            }
            case "THIS" -> {
                return new Token(TokenType.THIS, null, line, startColumn, start, length);
            }
        }

        return new Token(TokenType.IDENTIFIER, word, line, startColumn, start, length);
    }

    private Token simple(TokenType type) {
        Token token = new Token(type, null, line, column, index, 1);
        advance();
        return token;
    }
}
