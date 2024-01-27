package comp520.syntacticanalyzer;

import comp520.ErrorReporter;

public class Parser implements ParserInterface {
    private Lexer _lexer;
    private Token _currToken;
    private ErrorReporter _reporter;

    public Parser(Lexer lexer, ErrorReporter reporter) {
        this._lexer = lexer;
        this._reporter = reporter;
    }
    @Override
    public void parse() {

    }

    @Override
    public void acceptIt() {

    }
}