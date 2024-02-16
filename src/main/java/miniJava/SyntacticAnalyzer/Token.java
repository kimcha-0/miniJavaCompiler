package miniJava.SyntacticAnalyzer;

public class Token {
    private TokenType _type;
    private String _text;
    private SourcePosition pos;
    public Token(TokenType type, String text, SourcePosition pos) {
        this._type = type;
        this.pos = pos;
        this._text = text;
    }

    public TokenType getTokenType() {
        return _type;
    }

    public SourcePosition getTokenPosition() {
        return pos;
    }

    public String getTokenText() {
        return _text;
    }
}
