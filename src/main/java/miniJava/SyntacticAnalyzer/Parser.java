package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

import static miniJava.SyntacticAnalyzer.TokenType.*;

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
        while (currToken.getTokenType() != TokenType.EOT) {
            parseClassDecl();
        }
        accept(TokenType.EOT);
    }

    private void parseClassDecl() {
        if (matchType(TokenType.EOT)) return;
        accept(TokenType.CLASS);
        accept(TokenType.IDENTIFIER);
        accept(TokenType.LCURLY);
        while (currToken.getTokenType() != TokenType.RCURLY) {
            parseVis();
            parseAccess();
            if (matchType(TokenType.VOID)) {
                acceptIt();
                accept(TokenType.IDENTIFIER);
                parseMethodDecl();
            } else {
                parseType();
                accept(TokenType.IDENTIFIER);
                if (matchType(TokenType.LPAREN)) parseMethodDecl();
                else parseFieldDecl();
            }
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
        while (!matchType(TokenType.RPAREN)) {
            accept(TokenType.COMMA);
            parseExpr();
        }
    }

    private void parseFieldDecl() {
        accept(SEMICOLON);
    }

    private void parseType() {
        // int | boolean | (int | id)[]
        if (matchType(TokenType.INT) || matchType(TokenType.IDENTIFIER)) {
            acceptIt();
            if (matchType(TokenType.LSQUARE)) {
                acceptIt();
                accept(RSQUARE);
            }
        } else if (matchType(TokenType.BOOLEAN)) {
            acceptIt();
        } else {
            parseError("unkown type: " + currToken.getTokenType());
        }
    }

    private void parseAccess() {
        // static ?
        if (matchType(TokenType.STATIC)) {
            acceptIt();
        }
    }

    private void parseVis() {
        // public | private ?
//        System.out.println("vis");
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
                acceptIt();
                parseRef();
                accept(TokenType.IDENTIFIER);
            }
        } else {
            parseError("reference expected but received: " + currToken.getTokenType());
        }
    }

    private void parseStatement() {
//        System.out.println("parsing stmt");
        switch (currToken.getTokenType()) {
            case LCURLY:
                acceptIt();
                while (!matchType(TokenType.RCURLY)) {
                    parseStatement();
                }
                acceptIt();
                return;
            case RETURN:
                acceptIt();
                // Expression?
                if (!matchType(SEMICOLON)) {
                    parseExpr();
                }
                accept(SEMICOLON);
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
            case BOOLEAN:
            case INT:
//                System.out.println("hi");
                // Type id = Expression;
                parseType();
                accept(TokenType.IDENTIFIER);
                accept(EQUALS);
                parseExpr();
                accept(SEMICOLON);
                return;
            case THIS:
                parseRef();
                return;
            case IDENTIFIER:
            default:
                accept(IDENTIFIER);
                switch (currToken.getTokenType()) {
                    case IDENTIFIER:
                        // Type id = Expression;
                        accept(IDENTIFIER);
                        accept(EQUALS);
                        parseExpr();
                        accept(SEMICOLON);
                        return;
                    case LSQUARE:
                        acceptIt();
                        if (currToken.getTokenType() != RSQUARE) {
                            // Ref[Expression] = Expression;
                            parseExpr();
                            accept(RSQUARE);
                            accept(EQUALS);
                            parseExpr();
                            accept(SEMICOLON);
                        } else {
                            // id[] id = Expression;
                            accept(RSQUARE);
                            accept(IDENTIFIER);
                            accept(EQUALS);
                            parseExpr();
                            accept(SEMICOLON);
                        }
                        return;
                    case PERIOD:
                        acceptIt();
                        accept(IDENTIFIER);
                        while (currToken.getTokenType() == PERIOD) {
                            acceptIt();
                            accept(IDENTIFIER);
                        }
                        if (currToken.getTokenType() == LSQUARE) {
                            // id.id[5] = Expression;
                            acceptIt();
                            accept(INTLITERAL);
                            accept(RSQUARE);
                            accept(EQUALS);
                            parseExpr();
                            accept(SEMICOLON);
                        } else if (currToken.getTokenType() == EQUALS) {
                            // id.id = Expression;
                            acceptIt();
                            parseExpr();
                            accept(SEMICOLON);
                        } else {
                            // id.id() = Expression;
                            accept(LPAREN);
                            if (currToken.getTokenType() != RPAREN) {
                                parseArgList();
                            }
                            acceptIt();
                            accept(SEMICOLON);
                        }
                        return;
                    case EQUALS:
                        // id = Expression;
                        acceptIt();
                        parseExpr();
                        accept(SEMICOLON);
                    case LPAREN:
                        // id();
                        acceptIt();
                        if (currToken.getTokenType() != RPAREN) {
                            parseArgList();
                        }
                        acceptIt();
                        accept(SEMICOLON);
                }
        }
    }

    private void parseAssignment() {
        accept(EQUALS);
        parseExpr();
        accept(SEMICOLON);
    }

    private void parseExpr() {
        parseOrExpr();
    }

    private void parseOrExpr() {
//        System.out.println("or");
        parseAndExpr();
        while (currToken.getTokenText().equals("||")) {
            acceptIt();
            parseAndExpr();
        }
    }

    private void parseAndExpr() {
//        System.out.println("and");
        parseEqualityExpr();
        while (currToken.getTokenText().equals("&&")) {
            acceptIt();
            parseEqualityExpr();
        }
    }

    private void parseEqualityExpr() {
//        System.out.println("eq==");
        parseRelExpr();
        while (currToken.getTokenText().equals("==") || currToken.getTokenText().equals("!=")) {
            acceptIt();
            parseRelExpr();
        }
    }

    private void parseRelExpr() {
//        System.out.println("<>=");
        parseAddExpr();
        while (currToken.getTokenText().equals(">") || currToken.getTokenText().equals("<") || currToken.getTokenText().equals(">=")
                || currToken.getTokenText().equals("<=")) {
            acceptIt();
//            System.out.println("Token: " + currToken.getText());
            parseAddExpr();
        }
    }

    private void parseAddExpr() {
//        System.out.println("+");
        parseMulExpr();
        while (currToken.getTokenText().equals("+") || currToken.getTokenText().equals("-")) {
            acceptIt();
            parseMulExpr();
        }
    }

    private void parseMulExpr() {
//        System.out.println("*");
        parseUnaryOpExpr();
//        System.out.println("currToken" + currToken.getText());
        while (currToken.getTokenText().equals("*") || currToken.getTokenText().equals("/")) {
//            System.out.println("MultExpr: " + currToken.getText() + currToken.getType());
            acceptIt();
            parseUnaryOpExpr();
        }
    }

    private void parseUnaryOpExpr() {
//        System.out.println("unary");
        if (currToken.getTokenText().equals("-") || currToken.getTokenText().equals("!")) {
            acceptIt();
            parseUnaryOpExpr();
        } else parseDefaultExpr();
    }

    private void parseDefaultExpr() {
//        System.out.println("default expr");
        switch (currToken.getTokenType()) {
            case LPAREN:
                acceptIt();
                parseExpr();
                accept(TokenType.RPAREN);
                return;
            case INTLITERAL:
            case TRUE:
            case FALSE:
                acceptIt();
                return;
            case NEW:
//                System.out.println("new!");
                acceptIt();
                handleNew();
                return;
            case IDENTIFIER:
            case THIS:
            default:
                // Reference ( [ Expression ] | ( Expression ) )?
                acceptIt();
                if (matchType(TokenType.PERIOD)) {
                    acceptIt();
                    parseRef();
                }
                if (matchType(TokenType.LSQUARE)) {
                    acceptIt();
                    parseExpr();
                    accept(RSQUARE);
                } else if (matchType(TokenType.LPAREN)) {
                    acceptIt();
                    parseExpr();
                    accept(TokenType.RPAREN);
                }
        }
    }

    private void handleNew() {
        switch (currToken.getTokenType()) {
            case IDENTIFIER:
                acceptIt();
                if (currToken.getTokenType() == LPAREN) {
                    acceptIt();
                    accept(TokenType.RPAREN);
                } else if (currToken.getTokenType() == LSQUARE) {
                    acceptIt();
                    parseExpr();
                    accept(RSQUARE);
                } else {
                    parseError("Expression syntax error");
                }
                return;
            case INT:
                acceptIt();
                accept(TokenType.LSQUARE);
                parseExpr();
                accept(RSQUARE);
                return;
        }
    }

    private void acceptIt() {
        accept(currToken.getTokenType());
    }

    private void accept(TokenType expect) throws SyntaxError {
//        System.out.println("expected token '" + expect
//                + "' but received '" + currToken.getType() + "'");
        if (expect == currToken.getTokenType()) {
            currToken = lexer.scan();
        } else {
            parseError("expected token '" + expect + "' but received '" + currToken.getTokenType() + "'");
            throw new SyntaxError();
        }
    }

    private void parseError(String errorMsg) throws SyntaxError {
        reporter.reportError("Parse error: " + errorMsg);
        throw new SyntaxError();
    }

    private boolean matchType(TokenType expected) {
        return expected == currToken.getTokenType();
    }
}
