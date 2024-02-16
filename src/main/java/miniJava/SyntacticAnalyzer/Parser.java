package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
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
    public Package parse() {
        currToken = lexer.scan();
        ClassDeclList classDeclList = new ClassDeclList();
        Package aPackage = new Package(classDeclList, currToken.getTokenPosition());
        ClassDecl classDecl;
//        System.out.println("parsing...");
        while (currToken.getTokenType() != TokenType.EOT) {
            classDecl = parseClassDecl();
            aPackage.classDeclList.add(classDecl);
        }
        accept(TokenType.EOT);
        return aPackage;
    }

    private ClassDecl parseClassDecl() {
        if (matchType(TokenType.EOT)) return null;

        MethodDeclList methodDeclList = new MethodDeclList();
        FieldDeclList fieldDeclList = new FieldDeclList();
        MemberDecl memberDecl;

        accept(TokenType.CLASS);
        ClassDecl classDecl = new ClassDecl(currToken.getTokenText(), fieldDeclList, methodDeclList, currToken.getTokenPosition());
        accept(TokenType.IDENTIFIER);
        accept(TokenType.LCURLY);

        while (currToken.getTokenType() != TokenType.RCURLY) {
            FieldDecl fieldDecl;
            boolean isPublic = parseVis();
            System.out.println(isPublic);
            boolean isStatic = parseAccess();
            if (matchType(TokenType.VOID)) {
                acceptIt();
                String name = currToken.getTokenText();
                accept(TokenType.IDENTIFIER);
                fieldDecl = new FieldDecl(!isPublic, isStatic, new BaseType(TypeKind.VOID, null), name,  null);
                MethodDecl methodDecl = parseMethodDecl(fieldDecl);
                methodDeclList.add(methodDecl);
            } else {
                TypeDenoter type = parseType();
                String name = currToken.getTokenText();
                fieldDecl = new FieldDecl(!isPublic, isStatic, type, name, null);
                accept(TokenType.IDENTIFIER);
                if (matchType(TokenType.LPAREN)) {
                    MethodDecl methodDecl = parseMethodDecl(fieldDecl);
                    methodDeclList.add(methodDecl);
                }
                else {
                    fieldDeclList.add(fieldDecl);
                    accept(SEMICOLON);
                }
            }
        }
        accept(TokenType.RCURLY);
        return classDecl;
    }

    private MethodDecl parseMethodDecl(MemberDecl md) {
        accept(TokenType.LPAREN);
        // parameterList?
        ParameterDeclList paramDeclList = new ParameterDeclList();
        StatementList statementList = new StatementList();

        if (!matchType(TokenType.RPAREN)) {
            paramDeclList = parseParamList();
        }
        accept(TokenType.RPAREN);

        accept(TokenType.LCURLY);
        // Statement*
        while (!matchType(TokenType.RCURLY)) {
            Statement statement = parseStatement();
            statementList.add(statement);
        }
        accept(TokenType.RCURLY);
        return new MethodDecl(md, paramDeclList, statementList, md.posn);
    }

    private ParameterDeclList parseParamList() {
        ParameterDeclList paramList = new ParameterDeclList();
        TypeDenoter type = parseType();
        String name = currToken.getTokenText();
        accept(TokenType.IDENTIFIER);
        ParameterDecl paramDecl = new ParameterDecl(type, name, null);
        paramList.add(paramDecl);
        // (, Type id)*
        while (!matchType(TokenType.RPAREN)) {
            accept(TokenType.COMMA);
            TypeDenoter type1 = parseType();
            String name1 = currToken.getTokenText();
            paramDecl = new ParameterDecl(type1, name1, null);
            paramList.add(paramDecl);
            accept(TokenType.IDENTIFIER);
        }
        return paramList;
    }

    private ExprList parseArgList() {
        ExprList exprList = new ExprList();
        Expression expression = parseExpr();
        exprList.add(expression);
        //System.out.println(currToken.getTokenText());
        while (!matchType(TokenType.RPAREN)) {
            accept(TokenType.COMMA);
            expression = parseExpr();
            exprList.add(expression);
        }
        return exprList;
    }
    private TypeDenoter parseType() {
        // int | boolean | (int | id)[]
        TypeDenoter type;
        if (matchType(TokenType.INT)) {
            type = new BaseType(TypeKind.INT, null);
            acceptIt();
            if (matchType(TokenType.LSQUARE)) {
                acceptIt();
                accept(RSQUARE);
                return new ArrayType(type, null);
            }
            return type;
        } else if (matchType(TokenType.IDENTIFIER)) {
            Identifier id = new Identifier(currToken);
            SourcePosition posn = currToken.getTokenPosition();
            acceptIt();
            if (matchType(TokenType.LSQUARE)) {
                acceptIt();
                accept(RSQUARE);
                type = new ClassType(id, posn);
                return new ArrayType(type, posn);
            }
            return new ClassType(id, posn);
        } else if (matchType(TokenType.BOOLEAN)) {
            acceptIt();
            return new BaseType(TypeKind.BOOLEAN, null);
        } else {
            parseError("unkown type: " + currToken.getTokenType());
            return new BaseType(TypeKind.ERROR, null);
        }
    }

    private boolean parseAccess() {
        // static ?
        if (matchType(TokenType.STATIC)) {
            acceptIt();
            return true;
        }
        return false;
    }

    private boolean parseVis() {
        // public | private ?
//        System.out.println("vis");
        if (matchType(TokenType.PUBLIC)) {
            acceptIt();
            return true;
        } else if (matchType(TokenType.PRIVATE)) {
            acceptIt();
            return false;
        }
        return true;
    }

    private Reference parseRef() {
        // id | this | Reference.id
        // (id | this)(Reference.id)*
        SourcePosition posn = currToken.getTokenPosition();
        ThisRef thisRef = null;
        IdRef idRef = null;
        Identifier identifier = null;
        if (matchType(THIS)) {
            acceptIt();
            // ThisRef
            thisRef = new ThisRef(posn);
            if (!matchType(PERIOD)) return thisRef;
        } else {
            identifier = new Identifier(currToken);
            accept(IDENTIFIER);
            // IdRef
            idRef = new IdRef(identifier, posn);
            if (!matchType(PERIOD)) return idRef;
        }
        if (matchType(PERIOD)) {
            while (matchType(TokenType.PERIOD)) {
                acceptIt();
                identifier = new Identifier(currToken);
                accept(IDENTIFIER);
                // qualRef
            }
            return new QualRef(thisRef, identifier, posn);
        }
        return null;
    }

    private Statement parseStatement() {
//        System.out.println("parsing stmt");
        switch (currToken.getTokenType()) {
            case LCURLY:
                acceptIt();
                StatementList blockStmtList = new StatementList();
                while (!matchType(TokenType.RCURLY)) {
                    Statement stmt = parseStatement();
                }
                acceptIt();
                return new BlockStmt(blockStmtList, null);
            case RETURN:
                acceptIt();
                // Expression?
                Expression expression = null;
                if (!matchType(SEMICOLON)) {
                    expression = parseExpr();
                }
                accept(SEMICOLON);
                return new ReturnStmt(expression, null);
            case IF:
                acceptIt();
                accept(TokenType.LPAREN);
                Expression b = parseExpr();
                accept(TokenType.RPAREN);
                Statement t = parseStatement();
                if (matchType(TokenType.ELSE)) {
                    acceptIt();
                    parseStatement();
                }
                return new IfStmt(b, t, null);
            case WHILE:
                acceptIt();
                accept(TokenType.LPAREN);
                parseExpr();
                accept(TokenType.RPAREN);
                parseStatement();
                return new WhileStmt(null, null, null);
            case BOOLEAN:
            case INT:
//                System.out.println("hi");
                // Type id = Expression;
                parseType();
                accept(TokenType.IDENTIFIER);
                accept(EQUALS);
                parseExpr();
                accept(SEMICOLON);
                return new IxAssignStmt(null, null, null, null);
            case THIS:
                Reference ref = parseRef();
                Expression thisExpr;
                ExprList exprList = new ExprList();
                if (currToken.getTokenType() == LSQUARE) {
                    acceptIt();
                    thisExpr = parseExpr();
                    accept(RSQUARE);
                    accept(EQUALS);
                    thisExpr = parseExpr();
                    accept(SEMICOLON);
                    return null;
                } else if (currToken.getTokenType() == LPAREN) {
                    acceptIt();
                    if (currToken.getTokenType() != RPAREN) {
                        parseArgList();
                    }
                    accept(RPAREN);
                    accept(SEMICOLON);
                } else {
                    accept(EQUALS);
                    parseExpr();
                    accept(SEMICOLON);
                }
                return null;
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
                        return new IxAssignStmt(null, null, null, null);
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
                        return new AssignStmt(null, null, null);
                    case PERIOD:
                        acceptIt();
                        Reference qualRef = parseRef();
                        Expression expr = null;
                        ExprList exprList1 = new ExprList();
                        if (currToken.getTokenType() == LSQUARE) {
                            // id.id[5] = Expression;
                            acceptIt();
                            accept(INTLITERAL);
                            accept(RSQUARE);
                            accept(EQUALS);
                            expr = parseExpr();
                            accept(SEMICOLON);
                        } else if (currToken.getTokenType() == EQUALS) {
                            // id.id = Expression;
                            acceptIt();
                            expr = parseExpr();
                            accept(SEMICOLON);
                        } else {
                            // id.id() = Expression;
                            accept(LPAREN);
                            if (currToken.getTokenType() != RPAREN) {
                                exprList1 = parseArgList();
                            }
                            acceptIt();
                            accept(SEMICOLON);
                            return new CallStmt(qualRef, exprList1, null);
                        }
                        return new AssignStmt(qualRef, expr, null);
                    case EQUALS:
                        // id = Expression;o
                        acceptIt();
                        parseExpr();
                        accept(SEMICOLON);
                        return new AssignStmt(null, null, null);
                    case LPAREN:
                        // id();
                        acceptIt();
                        if (currToken.getTokenType() != RPAREN) {
                            parseArgList();
                        }
                        acceptIt();
                        accept(SEMICOLON);
                        return new CallStmt(null, null, null);
                }
        }
        return null;
    }

    private Expression parseExpr() {
        Expression expr = parseOrExpr();
        return null;
    }

    private Expression parseOrExpr() {
        Operator op = null;
//        System.out.println("or");
        Expression expr1 = parseAndExpr();
        Expression expr2 = null;
        while (currToken.getTokenText().equals("||")) {
            op = new Operator(currToken);
            acceptIt();
            expr2 = parseAndExpr();
        }
        return new BinaryExpr(op, expr1, expr2, null);
    }

    private Expression parseAndExpr() {
//        System.out.println("and");
        Expression expr1 = parseEqualityExpr();
        Expression expr2 = null;
        while (currToken.getTokenText().equals("&&")) {
            acceptIt();
            expr2 = parseEqualityExpr();
        }
        return null;
    }


    private Expression parseEqualityExpr() {
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

    private Expression parseDefaultExpr() {
//        System.out.println("default expr");
        switch (currToken.getTokenType()) {
            case LPAREN:
                acceptIt();
                parseExpr();
                accept(TokenType.RPAREN);
                return new CallExpr();
            case INTLITERAL:
                Token temp = currToken;
                acceptIt();
                return new IntLiteral(currToken);

            case TRUE:
            case FALSE:
                acceptIt();
            case NEW:
//                System.out.println("new!");
                acceptIt();
                handleNew();
                return;
            case IDENTIFIER:
            case THIS:
            default:
                // Reference ( [ Expression ] | ( Expression ) )?
//                System.out.println(currToken.getTokenType());
//                System.out.println(currToken.getTokenText());
                parseRef();
                if (matchType(TokenType.LSQUARE)) {
                    acceptIt();
                    parseExpr();
                    accept(RSQUARE);
                } else if (matchType(TokenType.LPAREN)) {
                    acceptIt();
                    if (currToken.getTokenType() != RPAREN) parseArgList();
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
