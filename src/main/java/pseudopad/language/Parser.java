package pseudopad.language;

import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private int index = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token currentToken() {
        return tokens.get(index);
    }

    private void advance() {
        index++;
    }

    private boolean match(TokenType type) {
        return currentToken().type == type;
    }

    private Token consume(TokenType expectedType, String errorMessage) {
        if (match(expectedType)) {
            Token token = currentToken();
            advance();
            return token;
        }

        throw new Errors.ParserError(errorMessage, currentToken());
    }

    public AST.ProgramNode parse() {
        List<AST.Node> statements = new ArrayList<>();

        while (currentToken().type != TokenType.EOF) {
            statements.add(parseStatement());
        }

        return new AST.ProgramNode(statements);
    }

    private AST.Statement parseStatement() {
        if (match(TokenType.SET)) {
            return (AST.Statement) parseVariableDeclaration();
        }

        if (match(TokenType.IDENTIFIER) || match(TokenType.THIS)) {
            return parseExpressionStatement();
        }

        if (match(TokenType.PRINT)) {
            return (AST.Statement) parsePrintStatement();
        }

        if (match(TokenType.IF)) {
            return parseIfStatement();
        }

        if (match(TokenType.WHILE)) {
            return parseWhileStatement();
        }

        if (match(TokenType.FOR)) {
            return parseForStatement();
        }

        if (match(TokenType.BREAK)) {
            advance();
            consume(TokenType.SEMICOLON, "Expected ';' after 'break'.");
            return new AST.BreakNode();
        }

        if (match(TokenType.SKIP)) {
            advance();
            consume(TokenType.SEMICOLON, "Expected ';' after 'skip' or 'continue'.");
            return new AST.SkipNode();
        }

        if (match(TokenType.FUNC)) {
            return parseFunctionDeclaration();
        }

        if (match(TokenType.RETURN)) {
            return parseReturnStatement();
        }

        if (match(TokenType.CLASS)) {
            return parseClassDeclaration();
        }

        throw new RuntimeException("Unexpected token: " + currentToken());
    }

    private AST.Node parseVariableDeclaration() {
        consume(TokenType.SET, "Expected 'set' keyword.");

        boolean isConst = false;

        if (match(TokenType.CONST)) {
            isConst = true;
            advance();
        }

        String type;
        if (match(TokenType.TYPE)) {
            type = consume(TokenType.TYPE, "Expected type.").value;
        } else {
            type = consume(TokenType.IDENTIFIER, "Expected data type or class name.").value;
        }

        String identifier = consume(TokenType.IDENTIFIER, "Expected variable name.").value;

        AST.Expression value = null;
        if (match(TokenType.EQUALS)) {
            advance();
            value = parseExpression();
        }

        consume(TokenType.SEMICOLON, "Unexpected ';' after declaration.");
        return new AST.VariableDeclarationNode(isConst, type, identifier, value);
    }

    private AST.Statement parseExpressionStatement() {
        // 1. Parse the full expression (handles "x", "x.method()", "x[0]", etc.)
        AST.Expression expr = parseExpression();

        // 2. Check if it's actually an assignment
        if (match(TokenType.EQUALS)) {
            advance(); // Consume '='
            AST.Expression value = parseExpression();
            consume(TokenType.SEMICOLON, "Expected ';' after assignment.");

            // Check valid assignment targets (Identifier, Index, or Dot/Get)
            if (expr instanceof AST.IdentifierNode ||
                    expr instanceof AST.IndexExpressionNode ||
                    expr instanceof AST.GetExpressionNode) {
                return new AST.AssignmentNode(expr, value);
            } else {
                throw new Errors.ParserError("Invalid assignment target.", currentToken());
            }
        }

        // 3. If not '=', it must be a standalone expression (like a function call)
        consume(TokenType.SEMICOLON, "Expected ';' after statement.");
        return new AST.ExpressionStatement(expr);
    }

    private AST.Node parsePrintStatement() {
        consume(TokenType.PRINT, "Expected 'print' keyword.");
        consume(TokenType.LPAREN, "Expected '(' after 'print'.");

        AST.Expression value = parseExpression();

        consume(TokenType.RPAREN, "Expected ')' after 'print'.");
        consume(TokenType.SEMICOLON, "Expected ';' after statement.");

        return new AST.PrintNode(value);
    }

    private AST.Expression parseExpression() {
        return parseOr();
    }

    private AST.Expression parseListLiteral() {
        consume(TokenType.LBRACKET, "Expected '[' to start list literal.");

        List<AST.Expression> elements = new ArrayList<>();
        if (match(TokenType.RBRACKET)) { advance(); return new AST.ListLiteralNode(elements); }

        elements.add(parseExpression());

        while (match(TokenType.COMMA)) {
            consume(TokenType.COMMA, "Expected ',' after list literal.");
            elements.add(parseExpression());
        }

        consume(TokenType.RBRACKET, "Expected ']' after list elements.");

        return new AST.ListLiteralNode(elements);
    }

    private AST.Expression parseDictLiteral() {
        consume(TokenType.LBRACE, "Expected '{' to start dict literal.");

        Map<AST.Expression, AST.Expression> entries = new LinkedHashMap<>();

        if (match(TokenType.RBRACE)) { advance(); return new AST.DictLiteralNode(entries); }

        AST.Expression key = parseExpression();
        consume(TokenType.COLON, "Expected ':' after dict key.");
        AST.Expression value = parseExpression();
        entries.put(key, value);

        while (match(TokenType.COMMA)) {
            consume(TokenType.COMMA, "Expected ',' after dict key.");
            key = parseExpression();
            consume(TokenType.COLON, "Expected ':' after dict key.");
            value = parseExpression();
            entries.put(key, value);
        }

        consume(TokenType.RBRACE, "Expected '}' after dict literal.");

        return new AST.DictLiteralNode(entries);
    }

    private AST.Expression finishIndexing(AST.Expression base) {
        while (match(TokenType.LBRACKET)) {
            consume(TokenType.LBRACKET, "Expected '[' to start indexing literal.");

            AST.Expression expressionIndex = parseExpression();
            consume(TokenType.RBRACKET, "Expected ']' after indexing literal.");
            base = new AST.IndexExpressionNode(base, expressionIndex);
        }

        return base;
    }

    private AST.Expression parseOr() {
        AST.Expression expression = parseAnd();

        while (match(TokenType.OR)) {
            Token operator = currentToken();
            advance();
            AST.Expression right = parseAnd();
            expression = new AST.BinaryExpressionNode(expression, operator, right);
        }

        return expression;
    }

    private AST.Expression parseAnd() {
        AST.Expression expression = parseComparison();

        while (match(TokenType.AND)) {
            Token operator = currentToken();
            advance();
            AST.Expression right = parseComparison();
            expression = new AST.BinaryExpressionNode(expression, operator, right);
        }

        return expression;
    }

    private AST.Expression parseComparison() {
        AST.Expression expression = parseTerm();

        while (match(TokenType.EQUAL_EQUAL) || match(TokenType.BANG_EQUAL) || match(TokenType.LESS) || match(TokenType.LESS_EQUAL) || match(TokenType.GREATER) || match(TokenType.GREATER_EQUAL)) {
            Token operator = currentToken();
            advance();
            AST.Expression right = parseTerm();
            expression = new AST.BinaryExpressionNode(expression, operator, right);
        }

        return expression;
    }

    private AST.Expression parseTerm() {
        AST.Expression expression = parseFactor();

        while (match(TokenType.PLUS) || match(TokenType.MINUS)) {
            Token operator = currentToken();
            advance();
            AST.Expression right = parseFactor();
            expression = new AST.BinaryExpressionNode(expression, operator, right);
        }

        return expression;
    }

    private AST.Expression parseFactor() {
        AST.Expression expression = parsePower();

        while (match(TokenType.MULT) || match(TokenType.DIV) || match(TokenType.MOD)) {
            Token operator = currentToken();
            advance();
            AST.Expression right = parsePower();
            expression = new AST.BinaryExpressionNode(expression, operator, right);
        }

        return expression;
    }

    private AST.Expression parsePower() {
        AST.Expression expression = parseUnary();

        while (match(TokenType.CARET)) {
            Token operator = currentToken();
            advance();
            AST.Expression right = parseUnary();
            expression = new AST.BinaryExpressionNode(expression, operator, right);
        }

        return expression;
    }

    private AST.Expression parseUnary() {
        if (match(TokenType.MINUS) || match(TokenType.NOT)) {
            Token operator = currentToken();
            advance();
            AST.Expression right = parseUnary();
            return new AST.UnaryExpressionNode(operator, right);
        }

        return parseCall();
    }

    private AST.Expression parseCall() {
        AST.Expression expression = parsePrimary();

        while (true) {
            if (match(TokenType.LPAREN)) {
                advance(); // Consume '('
                expression = finishCall(expression);
            } else if (match(TokenType.DOT)) { // NEW: Handle dot operator
                advance(); // Consume '.'
                Token name = consume(TokenType.IDENTIFIER, "Expected property name after '.'.");
                expression = new AST.GetExpressionNode(expression, name);
            } else if (match(TokenType.LBRACKET)) {
                // NEW: Handle Indexing in the chain (e.g. obj.list[0])
                advance(); // Consume '['
                AST.Expression index = parseExpression();
                consume(TokenType.RBRACKET, "Expected ']' after index.");
                expression = new AST.IndexExpressionNode(expression, index);
            } else {
                break;
            }
        }
        return expression;
    }

    private AST.Expression finishCall(AST.Expression callee) {
        List<AST.Expression> arguments = new ArrayList<>();

        // Check if the call has arguments (i.e., next token is NOT ')')
        if (!match(TokenType.RPAREN)) {
            arguments.add(parseExpression()); // Parse the first argument

            // Parse subsequent arguments if commas are found
            while (match(TokenType.COMMA)) {
                advance(); // <--- THIS WAS MISSING: Consume the comma!
                arguments.add(parseExpression());
            }
        }

        Token paren = consume(TokenType.RPAREN, "Expected ')' after arguments.");

        return new AST.CallExpressionNode(callee, paren, arguments);
    }

    private AST.Expression parsePrimary() {
        Token token = currentToken();
        AST.Expression expression;

        switch (token.type) {
            case NUMBER:
                advance();
                expression = new AST.LiteralNode(Double.valueOf(token.value), "NUMBER");
                break;
            case STRING:
                advance();
                expression = new AST.LiteralNode(token.value, "STRING");
                break;
            case BOOLEAN:
                advance();
                expression = new AST.LiteralNode(Boolean.parseBoolean(token.value), "BOOLEAN");
                break;
            case IDENTIFIER:
                advance();
                expression = new AST.IdentifierNode(token.value);
                break;
            case LPAREN:
                advance();
                expression = parseExpression();
                consume(TokenType.RPAREN, "Expected ')'");
                break;
            case LBRACKET:
                expression = parseListLiteral();
                break;
            case LBRACE:
                expression = parseDictLiteral();
                break;
            case THIS:
                advance();
                expression = new AST.ThisExpressionNode(token); // token is the captured 'this'
                break;
            default:
                throw new Errors.ParserError("Unexpected token: " + token);
        }

        return finishIndexing(expression);
    }


    private AST.Statement parseIfStatement() {
        consume(TokenType.IF, "Expected 'if'.");
        consume(TokenType.LPAREN, "Expected '(' after 'if'.");
        AST.Expression condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after condition.");
        consume(TokenType.THEN, "Expected 'then'.");

        List<AST.Statement> thenBranch = new ArrayList<>();
        while (!match(TokenType.ELIF) && !match(TokenType.ELSE) && !match(TokenType.ENDIF)) {
            thenBranch.add((AST.Statement) parseStatement());
        }

        List<AST.ElifNode> elifBranches = new ArrayList<>();
        while (match(TokenType.ELIF)) {
            advance(); // consume 'elif'
            consume(TokenType.LPAREN, "Expected '(' after 'elif'");
            AST.Expression elifCond = parseExpression();
            consume(TokenType.RPAREN, "Expected ')' after elif condition");
            consume(TokenType.THEN, "Expected 'then' after elif condition");

            List<AST.Statement> elifBody = new ArrayList<>();
            while (!match(TokenType.ELIF) && !match(TokenType.ELSE) && !match(TokenType.ENDIF)) {
                elifBody.add((AST.Statement) parseStatement());
            }
            elifBranches.add(new AST.ElifNode(elifCond, elifBody));
        }

        List<AST.Statement> elseBranch = null;
        if (match(TokenType.ELSE)) {
            advance(); // consume 'else'
            elseBranch = new ArrayList<>();
            while (!match(TokenType.ENDIF)) {
                elseBranch.add((AST.Statement) parseStatement());
            }
        }

        consume(TokenType.ENDIF, "Expected 'endif' to close if statement.");

        return new AST.IfNode(condition, thenBranch, elifBranches, elseBranch);
    }

    private AST.Statement parseWhileStatement() {
        consume(TokenType.WHILE, "Expected 'while'.");
        consume(TokenType.LPAREN, "Expected '(' after 'while'.");
        AST.Expression condition = parseExpression();
        consume(TokenType.RPAREN, "Expected ')' after condition.");
        consume(TokenType.DO, "Expected 'do'.");

        List<AST.Statement> body = new ArrayList<>();

        while (!(match(TokenType.ENDWHILE))) {
            body.add((AST.Statement) parseStatement());
        }

        consume(TokenType.ENDWHILE, "Expected 'endwhile' to close while statement.");
        return new AST.WhileNode(condition, body);
    }

    private AST.Statement parseForStatement() {
        consume(TokenType.FOR, "Expected 'for'.");
        consume(TokenType.LPAREN, "Expected '(' after 'for'.");

        AST.Statement initializer = null;
        if (!match(TokenType.SEMICOLON)) {
            initializer = parseInitializer();
        }

        AST.Expression condition = null;
        if (!match(TokenType.SEMICOLON)) {
            condition = parseExpression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after for condition");

        AST.Statement increment = null;
        if (!match(TokenType.RPAREN)) {
            increment = (AST.Statement) parseIncrement();
        }
        consume(TokenType.RPAREN, "Expected ')' after for clauses");

        consume(TokenType.DO, "Expected 'do'");

        List<AST.Statement> body = new ArrayList<>();
        while (!match(TokenType.ENDFOR)) {
            body.add((AST.Statement) parseStatement());
        }

        consume(TokenType.ENDFOR, "Expected 'endfor'");

        return new AST.ForNode(initializer, condition, increment, body);
    }

    private AST.Statement parseInitializer() {
        if (match(TokenType.SEMICOLON)) return null;
        if (match(TokenType.SET)) return (AST.Statement) parseVariableDeclaration();
        return (AST.Statement) parseExpressionStatement();
    }

    private AST.Node parseIncrement() {
        AST.Expression expr = parseExpression();

        if (match(TokenType.EQUALS)) {
            advance();
            AST.Expression value = parseExpression();
            return new AST.AssignmentNode(expr, value);
        }

        return expr;
    }

    private AST.ReturnNode parseReturnStatement() {
        Token keyword = consume(TokenType.RETURN, "Expected 'return'.");
        AST.Expression value = null;
        if (!match(TokenType.SEMICOLON)) {
            value = parseExpression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after return value.");
        return new AST.ReturnNode(keyword, value);
    }

    private AST.FunctionNode parseFunctionDeclaration() {
        consume(TokenType.FUNC, "Expected 'func'.");

        // 1. Return Type
        String returnType = consume(TokenType.TYPE, "Expected return type (or void).").value;

        // 2. Function Name
        String name = consume(TokenType.IDENTIFIER, "Expected function name.").value;

        // 3. Parameters
        consume(TokenType.LPAREN, "Expected '(' after function name.");
        List<AST.FunctionNode.Parameter> parameters = new ArrayList<>();

        // Check if there are parameters (if the next token is NOT ')')
        if (!match(TokenType.RPAREN)) {
            // Parse the first parameter
            String type = consume(TokenType.TYPE, "Expected parameter type.").value;
            String paramName = consume(TokenType.IDENTIFIER, "Expected parameter name.").value;
            parameters.add(new AST.FunctionNode.Parameter(paramName, type));

            // Parse subsequent parameters while we see a comma
            while (match(TokenType.COMMA)) {
                advance(); // Consume ','
                type = consume(TokenType.TYPE, "Expected parameter type.").value;
                paramName = consume(TokenType.IDENTIFIER, "Expected parameter name.").value;
                parameters.add(new AST.FunctionNode.Parameter(paramName, type));
            }
        }

        consume(TokenType.RPAREN, "Expected ')' after parameters.");

        // 4. Body
        consume(TokenType.DO, "Expected 'do' before function body.");
        List<AST.Statement> body = new ArrayList<>();
        while (!match(TokenType.ENDFUNC) && !match(TokenType.EOF)) {
            body.add((AST.Statement) parseStatement());
        }
        consume(TokenType.ENDFUNC, "Expected 'endfunc'.");

        return new AST.FunctionNode(name, parameters, returnType, body);
    }

    private AST.ClassNode parseClassDeclaration() {
        consume(TokenType.CLASS, "Expected 'class'.");
        String name = consume(TokenType.IDENTIFIER, "Expected class name.").value;
        consume(TokenType.DO, "Expected 'do' before class body.");

        List<AST.VariableDeclarationNode> fields = new ArrayList<>();
        List<AST.FunctionNode> methods = new ArrayList<>();

        while (!match(TokenType.ENDCLASS) && !match(TokenType.EOF)) {
            if (match(TokenType.SET)) {
                // Reuse existing variable declaration logic
                fields.add((AST.VariableDeclarationNode) parseVariableDeclaration());
            } else if (match(TokenType.FUNC)) {
                // Reuse existing function declaration logic
                methods.add(parseFunctionDeclaration());
            } else {
                throw new Errors.ParserError("Classes can only contain fields ('set') or methods ('func').", currentToken());
            }
        }

        consume(TokenType.ENDCLASS, "Expected 'endclass'.");
        return new AST.ClassNode(name, fields, methods);
    }

}
