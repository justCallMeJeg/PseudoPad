package pseudopad.language;

import java.util.List;

public interface Callable {
    int arity(); // Number of arguments expected
    Object call(Interpreter interpreter, List<Object> arguments);
}