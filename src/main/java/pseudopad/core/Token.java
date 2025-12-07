package pseudopad.core;

public class Token {
    public final TokenType type;
    public final String value;
    public final int line;
    public final int column;
    public final int startIndex;
    public final int length;

    public Token(TokenType type, String value, int line, int column, int startIndex, int length) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
        this.startIndex = startIndex;
        this.length = length;
    }

    @Override
    public String toString() {
        if (value == null) {
            return type.toString();
        }

        return type + "(" + value + ")";
    }
}
