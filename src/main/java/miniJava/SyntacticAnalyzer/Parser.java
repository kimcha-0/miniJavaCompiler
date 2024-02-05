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
            if (matchType(TokenType.VOID)) {
                acceptIt();
                accept(IDENTIFIER);
                parseMethodDecl();
            } else {
                parseType();
                accept(IDENTIFIER);
                if (matchType(LPAREN)) parseMethodDecl();
                else parseFieldDecl();
            }
        }
        accept(TokenType.RCURLY);
    }

    private void parseMethodDecl() {
        accept(LPAREN);
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
        while (!matchType(RPAREN)) {
            accept(COMMA);
            parseExpr();
        }
    }

    private void parseFieldDecl() {
        accept(TokenType.SEMICOLON);
    }

    private void parseType() {
        // int | boolean | (int | id)[]
        if (matchType(TokenType.INT) || matchType(IDENTIFIER)) {
            acceptIt();
            if (matchType(LSQUARE)) {
                acceptIt();
                accept(RSQUARE);
            }
        } else if (matchType(TokenType.BOOLEAN)) {
            acceptIt();
        } else {
            parseError("unkown type: " + currToken.getType());
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
            parseError("reference expected but received: " + currToken.getType());
        }
    }

    private void parseStatement() {
//        System.out.println("parsing stmt");
        switch (currToken.getType()) {
            case LCURLY:
                acceptIt();
                while (!matchType(TokenType.RCURLY))  {
                    parseStatement();
                }
                acceptIt();
                return;
            case RETURN:
                acceptIt();
                // Expression?
                if (!matchType(TokenType.SEMICOLON)) {
                    parseExpr();
                }
                accept(TokenType.SEMICOLON);
                return;
            case IF:
                acceptIt();
                accept(LPAREN);
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
                accept(LPAREN);
                parseExpr();
                accept(TokenType.RPAREN);
                parseStatement();
                return;
            case IDENTIFIER, THIS:
                acceptIt();
//                System.out.println("parse stmt identifier");
                if (matchType(IDENTIFIER) || matchType(THIS)) {
                    acceptIt();
                    parseAssignment();
                } else if (matchType(LSQUARE)) {
                    acceptIt();
                    if (matchType(RSQUARE)) {
                        // Type[] id = expr;
                        acceptIt();
                        accept(IDENTIFIER);
                        parseAssignment();
                    } else {
                        // Type [expression] = expression;
                        parseExpr();
                        accept(RSQUARE);
                        parseAssignment();
                    }
                } else {
                    // reference cases
//                    System.out.println("reference" + currToken.getText());
                    if (matchType(PERIOD)) {
                        acceptIt();
                        parseRef();
                    }
                    if (matchType(EQUALS)) {
                        acceptIt();
                        parseExpr();
                        accept(SEMICOLON);
                    } else if (matchType(LSQUARE)) {
                        acceptIt();
                        parseExpr();
                        accept(RSQUARE);
                        accept(EQUALS);
                        parseExpr();
                        accept(SEMICOLON);
                    } else if (matchType(LPAREN)) {
                        acceptIt();
                        if (!matchType(RPAREN)) {
                            parseArgList();
                        }
                        accept(RPAREN);
                        accept(SEMICOLON);
                    }
                    return;
                }
                return;
            case BOOLEAN, INT:
//                System.out.println("hi");
                // Type id = Expression;
                parseType();
                accept(TokenType.IDENTIFIER);
                accept(TokenType.EQUALS);
                parseExpr();
                accept(TokenType.SEMICOLON);
                return;
            default:
                parseError("Syntax error while parsing statement");
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
        while (currToken.getText().equals("||")) {
            acceptIt();
            parseAndExpr();
        }
    }

    private void parseAndExpr() {
//        System.out.println("and");
        parseEqualityExpr();
        while (currToken.getText().equals("&&")) {
            acceptIt();
            parseEqualityExpr();
        }
    }

    private void parseEqualityExpr() {
//        System.out.println("eq==");
        parseRelExpr();
        while (currToken.getText().equals("==") || currToken.getText().equals("!=")) {
            acceptIt();
            parseRelExpr();
        }
    }

    private void parseRelExpr() {
//        System.out.println("<>=");
        parseAddExpr();
        while (currToken.getText().equals(">") || currToken.getText().equals("<") || currToken.getText().equals(">=")
                || currToken.getText().equals("<=")) {
            acceptIt();
//            System.out.println("Token: " + currToken.getText());
            parseAddExpr();
        }
    }

    private void parseAddExpr() {
//        System.out.println("+");
        parseMulExpr();
        while (currToken.getText().equals("+") || currToken.getText().equals("-")) {
            acceptIt();
            parseMulExpr();
        }
    }

    private void parseMulExpr() {
//        System.out.println("*");
        parseUnaryOpExpr();
//        System.out.println("currToken" + currToken.getText());
        while (currToken.getText().equals("*") || currToken.getText().equals("/")) {
//            System.out.println("MultExpr: " + currToken.getText() + currToken.getType());
            acceptIt();
            parseUnaryOpExpr();
        }
    }

    private void parseUnaryOpExpr() {
//        System.out.println("unary");
        if (currToken.getText().equals("-") || currToken.getText().equals("!")) {
            acceptIt();
            parseUnaryOpExpr();
        } else parseDefaultExpr();
    }

    private void parseDefaultExpr() {
//        System.out.println("default expr");
        switch (currToken.getType()) {
            case LPAREN:
                acceptIt();
                parseExpr();
                accept(TokenType.RPAREN);
                return;
            case INTLITERAL, TRUE, FALSE:
                acceptIt();
                return;
            case NEW:
//                System.out.println("new!");
                acceptIt();
                handleNew();
                return;
            case IDENTIFIER, THIS:
            default:
                // Reference ( [ Expression ] | ( Expression ) )?
                acceptIt();
                if (matchType(PERIOD)) {
                    acceptIt();
                    parseRef();
                }
                if (matchType(LSQUARE)) {
                    acceptIt();
                    parseExpr();
                    accept(RSQUARE);
                } else if (matchType(LPAREN)) {
                    acceptIt();
                    parseExpr();
                    accept(RPAREN);
                }
        }
    }

    private void handleNew() {
        switch (currToken.getType()) {
            case IDENTIFIER:
                acceptIt();
                if (currToken.getType() == LPAREN) {
                    acceptIt();
                    accept(TokenType.RPAREN);
                } else if (currToken.getType() == LSQUARE) {
                    acceptIt();
                    parseExpr();
                    accept(TokenType.RSQUARE);
                } else {
                    parseError("Expression syntax error");
                }
                return;
            case INT:
                acceptIt();
                accept(LSQUARE);
                parseExpr();
                accept(TokenType.RSQUARE);
                return;
        }
    }

    private void acceptIt() {
        accept(currToken.getType());
    }

    private void accept(TokenType expect) throws SyntaxError {
//        System.out.println("expected token '" + expect
//                + "' but received '" + currToken.getType() + "'");
        if (expect == currToken.getType()) {
            currToken = lexer.scan();
        } else {
            parseError("expected token '" + expect + "' but received '" + currToken.getType() + "'");
            throw new SyntaxError();
        }
    }

    private void parseError(String errorMsg) throws SyntaxError {
        reporter.reportError("Parse error: " + errorMsg);
        throw new SyntaxError();
    }

    private boolean matchType(TokenType expected) {
        return expected == currToken.getType();
    }
}
