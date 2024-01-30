package comp520.syntacticanalyzer;

import comp520.ErrorReporter;

public class Parser implements ParserInterface {
    private Lexer _lexer;
    private Token _currToken;
    private ErrorReporter _reporter;
    private boolean eot = false;

    public Parser(Lexer lexer, ErrorReporter reporter) {
        this._lexer = lexer;
        this._reporter = reporter;
    }
    @Override
    public void parse() {
        _currToken = _lexer.scan();
        parseClassDecl();
        while (this._currToken.getType() == TokenType.CLASS) {
            parseClassDecl();
        }
    }

    private void parseClassDecl() {
        accept(TokenType.CLASS);
    }

    private void accept(TokenType expect) {
        if (_currToken.getType() == expect) {
           _currToken= _lexer.scan();
        }
    }

    private void rejectIt() {
    }
}