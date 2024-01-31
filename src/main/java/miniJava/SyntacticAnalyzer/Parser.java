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
        while (currToken.getType() != TokenType.EOT) {
            parseClassDecl();
        }
        accept(TokenType.EOT);
    }

    private void parseClassDecl() {
        accept(TokenType.CLASS);
        parseIdentifier();
        accept(TokenType.LCURLY);
        while (currToken.getType() != TokenType.RCURLY) {
            parseFieldDecl();
        }
        accept(TokenType.RCURLY);
    }

    private void methodDecl() {
        parseVis();
        parseAccess();
        if (currToken.getType() == TokenType.VOID) {
            accept(TokenType.VOID);
        } else parseType();
        parseIdentifier();
        accept(TokenType.LPAREN);
        // parameterList?
        parseParamList();
        accept(TokenType.RPAREN);
        accept(TokenType.LCURLY);
        parseStatement();
        // Statement*
        while (true) {
            parseStatement();
            break;
        }
        accept(TokenType.RCURLY);
    }

    private void parseParamList() {
        parseType();
        parseIdentifier();
        // parseIdentifier
    }

    private void parseArgList() {
        parseExpr();
    }

    private void parseFieldDecl() {
        parseVis();
        parseAccess();
        parseType();
        parseIdentifier();
        accept(TokenType.SEMICOLON);
    }

    private void parseType() {
        // int | boolean | (int | id)[]
        if (matchType(TokenType.INT)) {
            acceptIt();
        } else if (matchType(TokenType.BOOLEAN)) {
            acceptIt();
        } else if(matchType(TokenType.INT) || matchType(TokenType.IDENTIFIER){
            accept(TokenType.LSQUARE);
            accept(TokenType.RSQUARE);
        }
        parseError("unkown type: " + currToken.getType());
    }

    private void parseAccess() {
        // static ?
        if (matchType(TokenType.STATIC)) {
            acceptIt();
        }
    }

    private void parseVis() {
        // public | private ?
        if (matchType(TokenType.PUBLIC) || matchType(TokenType.PRIVATE)) {
            acceptIt();
        }
    }

    private void parseIdentifier() {
        accept(TokenType.IDENTIFIER);
    }

    private void parseRef() {
        // id | this | Reference.id
        // (id | this)(Reference.id)*
        if (matchType(TokenType.IDENTIFIER) || matchType(TokenType.THIS)) {
            acceptIt();
            while (matchType(TokenType.PERIOD)) {
                accept(TokenType.PERIOD);
                parseRef();
                accept(TokenType.IDENTIFIER);
            }
        }
        parseError("reference expected but received: " + currToken.getType());
    }

    private void parseStatement() {
    }
    private void parseExpr() {
    }

    private void acceptIt() {
        accept(currToken.getType());
    }

    private void accept(TokenType expect) {
        if (matchType(expect)) {
           currToken= lexer.scan();
        } else  parseError("expected token '" + expect 
                + "' but received '" + currToken.getType() + "'");
    }

    private void parseError(String errorMsg) {
        reporter.reportError("Parse error: " + errorMsg);
    }

    private boolean matchType(TokenType expected) {
        return expected == currToken.getType();
    }
}
