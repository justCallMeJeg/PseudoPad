package pseudopad.core;

import java.util.List;

/**
 * 
 * @author Joseph Mikhaeli Jalandoni
 */
public interface Callable {
    int arity(); // Number of arguments expected

    Object call(Interpreter interpreter, List<Object> arguments);
}
