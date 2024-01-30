package miniJava.SyntacticAnalyzer;

public class Token {
    private TokenType _type;
    private String _text;
    public Token(TokenType type, String text) {
        this._type = type;
        this._text = text;
    }

    public TokenType getType() {
        return _type;
    }

    public String getText() {
        return _text;
    }
}
