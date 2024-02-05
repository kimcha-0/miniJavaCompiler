package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

public class Parser implements ParserInterface {
    private Lexer lexer;
    private Token currToken;
    private ErrorReporter reporter;

    public Parser(Lexer lexer, ErrorReporter reporter) {
        this.lexer = lexer;
        this.reporter = reporter;
    }

    @Override
    public void parse() {
        currToken = lexer.scan();
//        System.out.println("parsing...");
        while (currToken.getType() != TokenType.EOT) {
            parseClassDecl();
        }
        accept(TokenType.EOT);
    }

    private void parseClassDecl() {
        if (matchType(TokenType.EOT)) return;
        accept(TokenType.CLASS);
        accept(TokenType.IDENTIFIER);
        accept(TokenType.LCURLY);
        while (currToken.getType() != TokenType.RCURLY) {
            parseVis();
            parseAccess();
            if (matchType(TokenType.IDENTIFIER)) {
                accept(TokenType.VOID);
            } else parseType();
            accept(TokenType.IDENTIFIER);
            if (matchType(TokenType.LPAREN)) parseMethodDecl();
        }
        accept(TokenType.RCURLY);
    }

    private void parseMethodDecl() {
        accept(TokenType.LPAREN);
        // parameterList?
        if (!matchType(TokenType.RPAREN)) parseParamList();
        accept(TokenType.RPAREN);

        accept(TokenType.LCURLY);
        // Statement*
        while (!matchType(TokenType.RCURLY)) {
            parseStatement();
        }
        accept(TokenType.RCURLY);
    }

    private void parseParamList() {
        parseType();
        accept(TokenType.IDENTIFIER);
        // (, Type id)*
        while (!matchType(TokenType.RPAREN)) {
            accept(TokenType.COMMA);
            parseType();
            accept(TokenType.IDENTIFIER);
        }
    }

    private void parseArgList() {
        parseExpr();
    }

    private void parseFieldDecl() {
        parseVis();
        parseAccess();
        parseType();
        accept(TokenType.IDENTIFIER);
        accept(TokenType.SEMICOLON);
    }

    private void parseType() {
        // int | boolean | (int | id)[]
        if (matchType(TokenType.INT)) {
            acceptIt();
        } else if (matchType(TokenType.BOOLEAN)) {
            acceptIt();
        } else if (matchType(TokenType.INT) || matchType(TokenType.IDENTIFIER)) {
            acceptIt();
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
        switch (currToken.getType()) {
            case LCURLY:
                acceptIt();
                while (!matchType(TokenType.RCURLY))  {
                    parseStatement();
                }
            case RETURN:
                acceptIt();
                // Statement?
                if (!matchType(TokenType.EOT)) {
                    parseExpr();
                }
                accept(TokenType.SEMICOLON);
                return;
            case IF:
                acceptIt();
                accept(TokenType.LPAREN);
                parseExpr();
                accept(TokenType.RPAREN);
                parseStatement();
                if (matchType(TokenType.ELSE)) {
                    acceptIt();
                    parseStatement();
                }
                return;
            case WHILE:
                acceptIt();
                accept(TokenType.LPAREN);
                parseExpr();
                accept(TokenType.RPAREN);
                parseStatement();
                return;
            case BOOLEAN, INT, IDENTIFIER:
                // Type id = Expression;
                parseType();
                accept(TokenType.IDENTIFIER);
                accept(TokenType.EQUALS);
                parseExpr();
                accept(TokenType.SEMICOLON);
                return;
            default:
                // Reference = Expression;
                parseRef();
                switch (currToken.getType()) {
                    case EQUALS:
                        acceptIt();
                        parseExpr();
                        accept(TokenType.SEMICOLON);
                        return;
                // Reference [ Expression ] = Expression;
                    case LSQUARE:
                        acceptIt();
                        parseExpr();
                        accept(TokenType.RSQUARE);
                        accept(TokenType.EQUALS);
                        parseExpr();
                        accept(TokenType.SEMICOLON);
                        return;
                // Reference ( ArgumnetList? )?
                    case LPAREN:
                        acceptIt();
                        if (!matchType(TokenType.RPAREN)) parseArgList();
                        accept(TokenType.RPAREN);
                }
                parseError("Syntax error while parsing statement");
        }
    }

    private void parseExpr() {
        switch (currToken.getType()) {
            case OPERATOR:
                // unop Expression
                if (currToken.getText().equals("!") || currToken.getText().equals("-")) {
                    acceptIt();
                    parseExpr();
                }
                return;
            case INTLITERAL, TRUE, FALSE:
                acceptIt();
                return;
            case NEW:
                acceptIt();
                handleNew();
                break;
            case LPAREN:
                acceptIt();
                parseExpr();
                accept(TokenType.RPAREN);
                break;
            default:
                parseRef();
                switch (currToken.getType()) {
                    case LSQUARE:
                        acceptIt();
                        parseExpr();
                        accept(TokenType.RSQUARE);
                        return;
                    case LPAREN:
                        acceptIt();
                        if (!matchType(TokenType.RPAREN)) parseArgList();
                        accept(TokenType.RPAREN);
                        return;
                }
        }
    }

    private void handleNew() {
        switch (currToken.getType()) {
            case IDENTIFIER:
                acceptIt();
                if (currToken.getType() == TokenType.LPAREN) {
                    accept(TokenType.LPAREN);
                    accept(TokenType.RPAREN);
                } else {
                    accept(TokenType.LSQUARE);
                    parseExpr();
                    accept(TokenType.RSQUARE);
                }
                break;
            case INT:
                acceptIt();
                accept(TokenType.LSQUARE);
                parseExpr();
                accept(TokenType.RSQUARE);
                break;
        }
    }

    private void acceptIt() {
        accept(currToken.getType());
    }

    private void accept(TokenType expect) throws SyntaxError {
//        System.out.println("expected token '" + expect
//                + "' but received '" + currToken.getType() + "'");
        if (matchType(expect)) {
            currToken = lexer.scan();
        } else {
            parseError("expected token '" + expect + "' but received '" + currToken.getType() + "'");
            throw new SyntaxError();
        }
    }

    private void parseError(String errorMsg) {
        reporter.reportError("Parse error: " + errorMsg);
    }

    private boolean matchType(TokenType expected) {
        return expected == currToken.getType();
    }
}
