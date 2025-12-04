package pseudopad.language;

import java.util.*;

public class Interpreter {
    // --- 1. NEW INTERFACE ---
    // This allows the GUI to tell us how to get input (e.g., via JOptionPane)
    public interface InputProvider {
        String read(String prompt);
    }

    // --- 2. UPDATED FIELDS ---
    public final Environment globals = new Environment();
    private Environment environment = globals;
    private final InputProvider inputProvider; // New field to store the input strategy

    // --- 3. NEW CONSTRUCTOR (For GUI) ---
    // The frontend will call this one, passing in the popup logic
    public Interpreter(InputProvider inputProvider) {
        this.inputProvider = inputProvider;
        initGlobals();
    }

    // --- 4. DEFAULT CONSTRUCTOR (For Testing/Console) ---
    // Keeps your old Scanner logic as a fallback so tests don't break
    public Interpreter() {
        this.inputProvider = (prompt) -> {
            System.out.print(prompt);
            return new Scanner(System.in).nextLine();
        };
        initGlobals();
    }

    // --- 5. MOVED GLOBAL DEFINITIONS ---
    private void initGlobals() {
        // Define native "input" function using the provider
        globals.define("input", new Callable() {
            @Override
            public int arity() { return 1; } // input("Prompt") -> 1 arg

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                String prompt = arguments.get(0).toString();
                // CRITICAL CHANGE: Use the provider instead of hardcoded Scanner
                return inputProvider.read(prompt);
            }
        }, "function", true);
    }

    public static class Variable {
        public Object value;
        public String declaredType;
        public boolean isConst;

        public Variable(Object value, String declaredType, boolean isConst) {
            this.value = value;
            this.declaredType = declaredType;
            this.isConst = isConst;
        }
    }

    private class UserDefinedFunction implements Callable {
        private final AST.FunctionNode declaration;
        private final Environment closure;

        public UserDefinedFunction(AST.FunctionNode declaration, Environment closure) {
            this.declaration = declaration;
            this.closure = closure;
        }

        UserDefinedFunction bind(PseudoInstance instance) {
            Environment environment = new Environment(closure);
            environment.define("this", instance, "object", true);
            return new UserDefinedFunction(declaration, environment);
        }

        @Override
        public int arity() {
            return declaration.parameters.size();
        }

        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
            // 1. Create a new environment for the function scope
            Environment environment = new Environment(closure);

            // 2. Bind arguments to parameters
            for (int i = 0; i < declaration.parameters.size(); i++) {
                AST.FunctionNode.Parameter param = declaration.parameters.get(i);
                Object value = arguments.get(i);

                // Strict Type Checking for Arguments
                if (!interpreter.isTypeCompatible(param.type(), value)) {
                    throw new Errors.TypeError("Expected argument " + (i+1) + " to be " +
                            param.type() + " but got " + value.getClass().getSimpleName());
                }

                // Define param in local scope
                environment.define(param.name(), value, param.type(), false);
            }

            // 3. Execute body
            try {
                // Swap interpreter environment to the local function scope
                Environment previous = interpreter.environment;
                interpreter.environment = environment;
                try {
                    interpreter.executeBlock(declaration.body);
                } finally {
                    interpreter.environment = previous; // Restore scope
                }
            } catch (Errors.ReturnSignal returnValue) {
                // 4. Handle Return Value & Type Check
                Object result = returnValue.value;

                if (declaration.returnType.equals("void")) {
                    if (result != null) throw new Errors.TypeError("Function '" + declaration.name + "' is void and cannot return a value.");
                    return null;
                }

                if (result == null) throw new Errors.TypeError("Function '" + declaration.name + "' must return a " + declaration.returnType);

                if (!interpreter.isTypeCompatible(declaration.returnType, result)) {
                    throw new Errors.TypeError("Function '" + declaration.name + "' expected to return " +
                            declaration.returnType + " but returned " + result.getClass().getSimpleName());
                }

                return result;
            }

            // 5. Handle void function finishing without return
            if (declaration.returnType.equals("void")) return null;

            throw new Errors.RuntimeError("Function '" + declaration.name + "' finished without returning a value.");
        }
    }

    // Represents a Class definition (e.g., "Person")
    private class PseudoClass implements Callable {
        final String name;
        final Map<String, AST.FunctionNode> methods;
        final List<AST.VariableDeclarationNode> fields;

        PseudoClass(String name, Map<String, AST.FunctionNode> methods, List<AST.VariableDeclarationNode> fields) {
            this.name = name;
            this.methods = methods;
            this.fields = fields;
        }

        // Finds a method in the class definition
        UserDefinedFunction findMethod(String name) {
            if (methods.containsKey(name)) {
                return new UserDefinedFunction(methods.get(name), globals); // Use globals as closure root
            }
            return null;
        }

        @Override
        public int arity() {
            UserDefinedFunction initializer = findMethod("init");
            if (initializer == null) return 0;
            return initializer.arity();
        }

        @Override
        public Object call(Interpreter interpreter, List<Object> arguments) {
            PseudoInstance instance = new PseudoInstance(this);

            // Call constructor (init) if it exists
            UserDefinedFunction initializer = findMethod("init");
            if (initializer != null) {
                initializer.bind(instance).call(interpreter, arguments);
            }

            return instance;
        }
    }

    // Represents an Object instance (e.g., a specific "Person")
    private class PseudoInstance {
        private final PseudoClass klass;
        private final Map<String, Environment.Variable> fields = new HashMap<>();

        PseudoInstance(PseudoClass klass) {
            this.klass = klass;
            // Initialize fields (set to null initially)
            for (AST.VariableDeclarationNode field : klass.fields) {
                // Note: We ignore the default value expression in declaration for now,
                // as fields are usually initialized in 'init'.
                // Strict typing: We store the declared type to enforce it later.
                fields.put(field.identifier, new Environment.Variable(null, field.typeName, field.isConst));
            }
        }

        Object get(Token name) {
            if (fields.containsKey(name.value)) {
                return fields.get(name.value).value;
            }

            UserDefinedFunction method = klass.findMethod(name.value);
            if (method != null) {
                return method.bind(this); // Bind 'this' to the method!
            }

            throw new Errors.RuntimeError("Undefined property '" + name.value + "'.", name);
        }

        void set(Token name, Object value) {
            if (fields.containsKey(name.value)) {
                Environment.Variable field = fields.get(name.value);
                // Type Check
                if (!isTypeCompatible(field.declaredType, value)) {
                    throw new Errors.TypeError("Field '" + name.value + "' expects " + field.declaredType + " but got " + value.getClass().getSimpleName());
                }
                field.value = value;
                return;
            }

            throw new Errors.RuntimeError("Undefined property '" + name.value + "'.", name);
        }
    }

    public void run(AST.ProgramNode program) {
        for (AST.Node node : program.statements) {
            execute(node);
        }
    }

    private void execute(AST.Node node) {
        switch (node) {
            case AST.VariableDeclarationNode variableDeclarationNode ->
                    executeVariableDeclaration(variableDeclarationNode);
            case AST.VariableAssignmentNode variableAssignmentNode -> executeVariableAssignment(variableAssignmentNode);
            case AST.AssignmentNode assignmentNode -> executeAssignment(assignmentNode);
            case AST.PrintNode printNode -> executePrint(printNode);
            case AST.IfNode ifNode -> executeIf(ifNode);
            case AST.WhileNode whileNode -> executeWhile(whileNode);
            case AST.ForNode forNode -> executeFor(forNode);
            case AST.BreakNode breakNode -> throw new Errors.BreakSignal();
            case AST.SkipNode skipNode -> throw new Errors.SkipSignal();
            case AST.FunctionNode funcNode -> executeFunctionDeclaration(funcNode);
            case AST.ReturnNode returnNode -> executeReturn(returnNode);
            case AST.ClassNode classNode -> executeClass(classNode);
            case AST.ThisExpressionNode thisNode -> evaluateThis(thisNode);
            case AST.ExpressionStatement statement -> evaluate(statement.expression);
            case AST.Expression expression -> evaluate(expression);
            default -> throw new Errors.RuntimeError("Unknown statement type: " + node.getClass());
        }
    }

    private void executeVariableDeclaration(AST.VariableDeclarationNode node) {
        Object value = null;

        if (node.value != null) {
            value = evaluate(node.value);

            String declaredType = node.typeName.toLowerCase();
            if (!isTypeCompatible(node.typeName, value)) {
                throw new Errors.TypeError("Type mismatch: expected " + declaredType + " but got value of type " +
                        (value instanceof List ? "list" : value instanceof Map ? "dict" : value.getClass().getSimpleName())
                );
            }
        }

        if (node.isConst && node.value == null) {
            throw new Errors.TypeError("Constant declaration cannot be null.");
        }

        environment.define(node.identifier, value, node.typeName, node.isConst);
    }

    private void executeVariableAssignment(AST.VariableAssignmentNode node) {
        Environment.Variable variable = environment.getVariable(node.identifier);

        if (variable == null) throw new Errors.TypeError("Variable '" + node.identifier + "' not declared.");
        if (variable.isConst) throw new Errors.TypeError("Cannot assign to constant variable '" + node.identifier + "'");

        Object value = evaluate(node.value);

        if (!isTypeCompatible(variable.declaredType, value)) {
            throw new Errors.TypeError("Type mismatch: variable '" + node.identifier +
                    "' is of type " + variable.declaredType +
                    " but got " + value.getClass().getSimpleName());
        }

        variable.value = value;
    }

    private void executePrint(AST.PrintNode node) {
        Object value = evaluate(node.expression);
        System.out.println(value);
    }

    private void executeIf(AST.IfNode node) {
        Object condition = evaluate(node.condition);

        if (!(condition instanceof Boolean)) throw new Errors.TypeError("If condition must evaluate to boolean.");
        if ((Boolean) condition) {executeBlock(node.thenBranch); return;}

        for (AST.ElifNode branch : node.elifBranches) {
            Object elifCondition = evaluate(branch.condition);

            if (!(elifCondition instanceof Boolean)) throw new Errors.TypeError("Elif condition must evaluate to boolean.");
            if ((Boolean) elifCondition) {executeBlock(branch.body); return;}
        }

        if (node.elseBranch != null) executeBlock(node.elseBranch);
    }

    private void executeBlock(List<? extends AST.Node> block) {
        for (AST.Node statement : block) {
            execute(statement);
        }
    }

    private void executeWhile(AST.WhileNode node) {
        while (getTruthValue(evaluate(node.condition))) {
            try { executeBlock(node.body); }
            catch (Errors.SkipSignal skipSignal){ continue; }
            catch (Errors.BreakSignal breakSignal){ break; }
        }
    }

    private void executeFor(AST.ForNode node) {
        if (node.initializer != null) execute(node.initializer);

        while (node.condition == null || getTruthValue(evaluate(node.condition))) {
            try { executeBlock(node.body); }
            catch (Errors.SkipSignal skipSignal){ continue; }
            catch (Errors.BreakSignal breakSignal){ break; }

            if (node.increment != null) execute(node.increment);
        }
    }

    private void executeFunctionDeclaration(AST.FunctionNode node) {
        // Capture the current environment (closure)
        UserDefinedFunction function = new UserDefinedFunction(node, this.environment);
        this.environment.define(node.name, function, "function", false);
    }

    private void executeReturn(AST.ReturnNode node) {
        Object value = null;
        if (node.value != null) {
            value = evaluate(node.value);
        }
        throw new Errors.ReturnSignal(value);
    }

    private void executeClass(AST.ClassNode node) {
        Map<String, AST.FunctionNode> methods = new HashMap<>();
        for (AST.FunctionNode method : node.methods) {
            methods.put(method.name, method);
        }
        PseudoClass klass = new PseudoClass(node.name, methods, node.fields);
        environment.define(node.name, klass, "class", true);
    }

    private Object evaluate(AST.Expression expression) {
        return switch (expression) {
            case AST.LiteralNode literalNode -> {
                if (literalNode.value instanceof Number) yield ((Double) literalNode.value);
                yield literalNode.value;
            }
            case AST.IdentifierNode identifierNode -> evaluateIdentifier(identifierNode);
            case AST.ListLiteralNode listNode -> evaluateList(listNode);
            case AST.DictLiteralNode dictNode -> evaluateDict(dictNode);
            case AST.IndexExpressionNode indexNode -> evaluateIndex(indexNode);
            case AST.BinaryExpressionNode binaryExpressionNode -> evaluateBinary(binaryExpressionNode);
            case AST.UnaryExpressionNode unaryExpressionNode -> evaluateUnary(unaryExpressionNode);
            case AST.CallExpressionNode callNode -> evaluateCall(callNode);
            case AST.GetExpressionNode getNode -> evaluateGet(getNode);
            case AST.ThisExpressionNode thisNode -> evaluateThis(thisNode);
            default -> throw new RuntimeException("Unknown expression type: " + expression.getClass());
        };
    }

    private Object evaluateIdentifier(AST.IdentifierNode node) {
        Environment.Variable variable = environment.getVariable(node.name);
        if (variable == null) throw new Errors.TypeError("Variable '" + node.name + "' not declared.");
        return variable.value;
    }

    private Object evaluateList(AST.ListLiteralNode node) {
        List<Object> list = new ArrayList<>();
        for (AST.Expression element : node.elements) {
            list.add(evaluate(element));
        }
        return list;
    }

    private Object evaluateDict(AST.DictLiteralNode node) {
        Map<Object, Object> map = new HashMap<>();
        for (Map.Entry<AST.Expression, AST.Expression> entry : node.entries.entrySet()) {
            Object key  = evaluate(entry.getKey());
            Object value = evaluate(entry.getValue());
            map.put(key, value);
        }
        return map;
    }

    private Object evaluateIndex(AST.IndexExpressionNode node) {
        Object target = evaluate(node.target);
        Object index = evaluate(node.index);

        if (target instanceof List<?> list) {
            if (!(index instanceof Number)) throw new Errors.RuntimeError("List index must be a number.");
            int i = ((Number) index).intValue();
            if (i < 0 || i >= list.size()) throw new Errors.RuntimeError("List index out of bounds.");
            return list.get(i);
        }

        if (target instanceof Map<?, ?> map) {
            if (!(index instanceof String)) throw new Errors.RuntimeError("Dictionary key must be a string.");
            if (!map.containsKey(index)) throw new Errors.RuntimeError("Key not found in dictionary.");
            return map.get(index);
        }

        throw new Errors.RuntimeError("Cannot index type: " + target.getClass());
    }

    private void executeAssignment(AST.AssignmentNode node) {
        if (node.target instanceof AST.IdentifierNode id) {
            executeVariableAssignment(new AST.VariableAssignmentNode(id.name, node.value));
        } else if (node.target instanceof AST.IndexExpressionNode indexNode) {
            Object target = evaluate(indexNode.target);
            Object key = evaluate(indexNode.index);
            Object value = evaluate(node.value);

            if (target instanceof List<?> list) {
                int i = ((Number) key).intValue();
                ((List<Object>) list).set(i, value);
            } else if (target instanceof Map<?, ?> map) {
                ((Map<Object, Object>) map).put(key, value);
            } else {
                throw new Errors.RuntimeError("Cannot assign to type: " + target.getClass());
            }
        } else if (node.target instanceof AST.GetExpressionNode getProp) {
            Object object = evaluate(getProp.object);
            if (!(object instanceof PseudoInstance)) {
                throw new Errors.RuntimeError("Only instances have fields.", getProp.name);
            }
            Object value = evaluate(node.value);
            ((PseudoInstance) object).set(getProp.name, value);
        } else {
            throw new Errors.RuntimeError("Invalid assignment target.");
        }
    }


    private Object evaluateUnary(AST.UnaryExpressionNode node) {
        Object right = evaluate(node.expression);

        if (node.operator.type == TokenType.MINUS) {
            if (isNumber(right)) {
                return -((Double) right);
            }
        }

        switch (node.operator.type) {
            case TokenType.MINUS:
                if (isNumber(right)) return -((Double) right);
                throw new Errors.RuntimeError("Expected a number type value after '-' but found: " + node.operator.type);
            case TokenType.NOT:
                if (isBoolean(right)) return !((Boolean) right);
                throw new Errors.RuntimeError("Expected a boolean type value after 'not' but found: " + node.operator.type);
            default: throw new Errors.RuntimeError("Unknown unary operator: " + node.operator.type);
        }
    }

    private Object evaluateCall(AST.CallExpressionNode node) {
        Object callee = evaluate(node.callee);

        List<Object> arguments = new ArrayList<>();
        for (AST.Expression arg : node.arguments) {
            arguments.add(evaluate(arg));
        }

        if (!(callee instanceof Callable)) {
            throw new Errors.RuntimeError("Can only call functions and classes.", node.parenthesis);
        }

        Callable function = (Callable) callee;
        if (arguments.size() != function.arity()) {
            throw new Errors.RuntimeError("Expected " + function.arity() + " arguments but got " + arguments.size() + ".", node.parenthesis);
        }

        return function.call(this, arguments);
    }

    private Object evaluateGet(AST.GetExpressionNode node) {
        Object object = evaluate(node.object);
        String name = node.name.value;

        if (object instanceof PseudoInstance) {
            return ((PseudoInstance) object).get(node.name);
        }

        // 1. BUILT-INS FOR LISTS
        if (object instanceof List) {
            List<Object> list = (List<Object>) object;
            switch (name) {
                case "append":
                    return new Callable() {
                        @Override public int arity() { return 1; }
                        @Override public Object call(Interpreter interpreter, List<Object> args) {
                            list.add(args.get(0));
                            return null; // void
                        }
                    };
                case "pop":
                    return new Callable() {
                        @Override public int arity() { return 0; }
                        @Override public Object call(Interpreter interpreter, List<Object> args) {
                            if (list.isEmpty()) throw new Errors.RuntimeError("Cannot pop from empty list.", node.name);
                            return list.remove(list.size() - 1);
                        }
                    };
                case "length":
                    // Note: 'length' is often a property, but here implemented as a method call .length()
                    // If you want .length property (no parens), return Double directly.
                    // Assuming method style .length() based on OOP request:
                    return new Callable() {
                        @Override public int arity() { return 0; }
                        @Override public Object call(Interpreter interpreter, List<Object> args) {
                            return (double) list.size();
                        }
                    };
            }
        }

        // 2. BUILT-INS FOR STRINGS
        if (object instanceof String) {
            String string = (String) object;
            switch (name) {
                case "toNumber":
                    return new Callable() {
                        @Override public int arity() { return 0; }
                        @Override public Object call(Interpreter interpreter, List<Object> args) {
                            try {
                                return Double.parseDouble(string);
                            } catch (NumberFormatException e) {
                                throw new Errors.RuntimeError("Cannot convert string '" + string + "' to number.", node.name);
                            }
                        }
                    };
                case "toBoolean":
                    return new Callable() {
                        @Override public int arity() { return 0; }
                        @Override public Object call(Interpreter interpreter, List<Object> args) {
                            return Boolean.parseBoolean(string);
                        }
                    };
                case "length":
                    return new Callable() {
                        @Override public int arity() { return 0; }
                        @Override public Object call(Interpreter interpreter, List<Object> args) {
                            return (double) string.length();
                        }
                    };
            }
        }

        // 3. BUILT-INS FOR DICTIONARIES (MAPS)
        if (object instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) object;
            switch (name) {
                case "keys":
                    return new Callable() {
                        @Override public int arity() { return 0; }
                        @Override public Object call(Interpreter interpreter, List<Object> args) {
                            return new ArrayList<>(map.keySet());
                        }
                    };
                case "remove":
                    return new Callable() {
                        @Override public int arity() { return 1; }
                        @Override public Object call(Interpreter interpreter, List<Object> args) {
                            return map.remove(args.get(0));
                        }
                    };
            }
        }

        throw new Errors.RuntimeError("Property '" + name + "' does not exist on type " + object.getClass().getSimpleName(), node.name);
    }

    private Object evaluateThis(AST.ThisExpressionNode node) {
        Environment.Variable var = environment.getVariable("this");

        if (var != null) {
            return var.value;
        }

        throw new Errors.RuntimeError("Use of 'this' outside of a class method.", node.keyword);
    }

    private Object evaluateBinary(AST.BinaryExpressionNode node) {
        Object left = evaluate(node.left);

        switch (node.operator.type) {
            case AND:
                if (!isBoolean(left)) throw new Errors.TypeError("Operator: " + node.operator.value + " must be boolean.");
                if (!(Boolean) left) return false;
                Object rightAnd = evaluate(node.right);
                if (!isBoolean(rightAnd))  throw new Errors.TypeError("Operator: " + node.operator.value + " must be boolean.");
                return (Boolean) rightAnd;
            case OR:
                if (!isBoolean(left)) throw new Errors.TypeError("Operator: " + node.operator.value + " must be boolean.");
                if ((Boolean) left) return true;
                Object rightOr = evaluate(node.right);
                if (!isBoolean(rightOr))  throw new Errors.TypeError("Operator: " + node.operator.value + " must be boolean.");
                return (Boolean) rightOr;
            default:
                Object right = evaluate(node.right);

                switch (node.operator.type) {
                    case PLUS:
                        if (areBothNumbers(left, right)) return (Double) left + (Double) right;
                        if (areEitherString(left, right)) return left + String.valueOf(right);
                        throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers or strings.");
                    case MINUS:
                        if (!areBothNumbers(left, right)) throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers.");
                        return (Double) left - (Double) right;
                    case MULT:
                        if (!areBothNumbers(left, right)) throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers.");
                        return (Double) left * (Double) right;
                    case DIV:
                        if (!areBothNumbers(left, right)) throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers.");
                        if ((Double) right == 0) throw new Errors.RuntimeError("Division by zero");
                        return (Double) left / (Double) right;
                    case MOD:
                        if (!areBothNumbers(left, right)) throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers.");
                        if ((Double) right == 0) throw new Errors.RuntimeError("Modulo by zero");
                        return (Double) left % (Double) right;
                    case CARET:
                        if (!areBothNumbers(left, right)) throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers.");
                        return Math.pow((Double) left, (Double) right);
                    case EQUAL_EQUAL:
                        return isEqual(left, right);
                    case BANG_EQUAL:
                        return !isEqual(left, right);
                    case LESS:
                        if (!areBothNumbers(left, right)) throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers.");
                        return toDouble(left) < toDouble(right);
                    case LESS_EQUAL:
                        if (!areBothNumbers(left, right)) throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers.");
                        return toDouble(left) <= toDouble(right);
                    case GREATER:
                        if (!areBothNumbers(left, right)) throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers.");
                        return toDouble(left) > toDouble(right);
                    case GREATER_EQUAL:
                        if (!areBothNumbers(left, right)) throw new Errors.TypeError("Operator: " + node.operator.value + " requires numbers.");
                        return toDouble(left) >= toDouble(right);
                    default:
                        throw new Errors.RuntimeError("Unknown operator: " + node.operator.value);
                }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isTypeCompatible(String type, Object value) {
        // 1. Handle Primitive Types
        switch (type) {
            case "number": return value instanceof Double;
            case "string": return value instanceof String;
            case "boolean": return value instanceof Boolean;
            case "list": return value instanceof List<?>;
            case "dict": return value instanceof Map<?, ?>;
            case "object": return true; // (Optional) Generic object type
        }

        // 2. Handle User-Defined Classes
        if (value instanceof PseudoInstance instance) {
            // Check if the instance's class name matches the required type (e.g., "Person" == "Person")
            return instance.klass.name.equals(type);
        }

        return false;
    }

    private boolean areBothNumbers(Object left, Object right) {
        return isNumber(left) && isNumber(right);
    }

    private boolean isNumber(Object value) {
        return value instanceof Double;
    }

    private boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }

    private boolean areEitherString(Object left, Object right) {
        return (left instanceof String || right instanceof String);
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private double toDouble(Object value) {
        if (!(value instanceof Double)) {
            throw new Errors.RuntimeError("Operand must be a number.");
        }
        return (Double) value;
    }

    private boolean getTruthValue(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        return false;
    }
}




