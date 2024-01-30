package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

public class Parser implements ParserInterface {
    private Lexer lexer;
    private Token currToken;
    private ErrorReporter reporter;
    private boolean eot = false;

    public Parser(Lexer lexer, ErrorReporter reporter) {
        this.lexer = lexer;
        this.reporter = reporter;
    }
    @Override
    public void parse() {
        currToken = lexer.scan();
        parseClassDecl();
        while (currToken.getType() == TokenType.CLASS) {
            accept(TokenType.CLASS);
            parseClassDecl();
        }
    }

    private void parseClassDecl() {
        if (currToken.getType() != TokenType.CLASS) {
            rejectIt();
            return;
        }
        parseIdentifier();
        accept(TokenType.LCURLY);
        parseFieldDecl();
        accept(TokenType.RCURLY);
    }

    private void methodDecl() {
        parseVis();
        parseAccess();
        if (currToken.getType() == TokenType.VOID) {

        } else parseType();
        parseIdentifier();
        accept(TokenType.LPAREN);
        // parameterList?
        parseParamList();
    }

    private void parseParamList() {
        parseType();
        parseIdentifier();
        // (, Expression)*
    }

    private void parseArgList() {
        parseExpr();
    }

    private void parseFieldDecl() {
        parseVis();
        parseAccess();
        parseType();
        parseIdentifier();
    }

    private void parseType() {
        // int | boolean | (int | id)[]
    }

    private void parseAccess() {
        // static ?
    }

    private void parseVis() {
        // public | private ?
    }

    private void parseIdentifier() {
        if (currToken.getType() != TokenType.IDENTIFIER) {
            rejectIt();
            return;
        }
    }

    private void parseRef() {
        // id | this | Reference.id
    }

    private void parseStatement() {

    }
    private void parseExpr() {

    }

    private void accept(TokenType expect) {
        if (currToken.getType() == expect) {
           currToken= lexer.scan();
        }
    }

    private void rejectIt() {
        parseError();
    }

    private void parseError() {
        reporter.reportError("Parse error: ");
    }
}