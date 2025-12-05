package pseudopad.language;

public class Token {
    public final TokenType type;
    public final String value;
    public final int line;
    public final int column;

    public Token(TokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        if (value == null) {
            return type.toString();
        }

        return type + "(" + value + ")";
    }
}
