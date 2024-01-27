package syntacticanalyzer;

public class Parser implements ParserInterface {
    private Lexer _lexer;
    private Token _currToken;

    public Parser(Lexer lexer) {
        this._lexer = lexer;
    }
    @Override
    public void parse() {
    }

    @Override
    public void acceptIt() {
    }
}