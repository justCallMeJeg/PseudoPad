package pseudopad.core;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    public static class Variable {
        public Object value;
        public final String declaredType;
        public final boolean isConst;

        public Variable(Object value, String declaredType, boolean isConst) {
            this.value = value;
            this.declaredType = declaredType;
            this.isConst = isConst;
        }
    }

    // "enclosing" will be null for the global scope, but used later for functions
    final Environment enclosing;
    private final Map<String, Variable> values = new HashMap<>();

    public Environment() {
        this.enclosing = null;
    }

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void define(String name, Object value, String type, boolean isConst) {
        if (values.containsKey(name) && values.get(name).isConst) {
            throw new Errors.TypeError("Constant declaration already exists: " + name);
        }
        values.put(name, new Variable(value, type, isConst));
    }

    public Variable getVariable(String name) {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        if (enclosing != null)
            return enclosing.getVariable(name);
        return null;
    }

    public Object get(Token name) {
        Variable var = getVariable(name.value);
        if (var != null)
            return var.value;

        throw new Errors.RuntimeError("Undefined variable '" + name.value + "'.", name);
    }

    public void assign(Token name, Object value) {
        Variable var = getVariable(name.value);
        if (var != null) {
            if (var.isConst) {
                throw new Errors.TypeError("Cannot assign to constant variable '" + name.value + "'");
            }
            // Type checking remains in Interpreter, we just update value here
            var.value = value;
            return;
        }

        throw new Errors.RuntimeError("Undefined variable '" + name.value + "'.", name);
    }
}