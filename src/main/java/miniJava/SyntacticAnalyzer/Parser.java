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
        SourcePosition posn = null;
        Statement statement = null;
        Expression expression = null;
        Expression insideExpr = null;
        VarDecl varDecl = null;
//        System.out.println("parsing stmt");
        switch (currToken.getTokenType()) {
            case LCURLY:
                posn = currToken.getTokenPosition();
                acceptIt();
                StatementList blockStmtList = new StatementList();
                while (!matchType(TokenType.RCURLY)) {
                    Statement stmt = parseStatement();
                    blockStmtList.add(stmt);
                }
                acceptIt();
                return new BlockStmt(blockStmtList, posn);
            case RETURN:
                posn = currToken.getTokenPosition();
                acceptIt();
                // Expression?
                if (!matchType(SEMICOLON)) {
                    expression = parseExpr();
                }
                accept(SEMICOLON);
                return new ReturnStmt(expression, posn);
            case IF:
                posn = currToken.getTokenPosition();
                acceptIt();
                accept(TokenType.LPAREN);
                Expression b = parseExpr();
                accept(TokenType.RPAREN);
                Statement t = parseStatement();
                if (matchType(TokenType.ELSE)) {
                    acceptIt();
                    parseStatement();
                }
                return new IfStmt(b, t, posn);
            case WHILE:
                posn = currToken.getTokenPosition();
                acceptIt();
                accept(TokenType.LPAREN);
                expression = parseExpr();
                accept(TokenType.RPAREN);
                statement = parseStatement();
                return new WhileStmt(expression, statement, posn);
            case BOOLEAN:
            case INT:
//                System.out.println("hi");
                // Type id = Expression;
                posn = currToken.getTokenPosition();
                TypeDenoter type = parseType();

                Identifier id = new Identifier(currToken);
                accept(TokenType.IDENTIFIER);
                Reference assnRef = new IdRef(id, posn);
                accept(EQUALS);
                expression = parseExpr();
                accept(SEMICOLON);
                varDecl = new VarDecl(type, id.spelling, posn);
                return new VarDeclStmt(varDecl, expression, posn);
            case THIS:
                posn = currToken.getTokenPosition();
                Reference ref = parseRef();
                ExprList exprList = new ExprList();
                if (currToken.getTokenType() == LSQUARE) {
                    // Ref [ Expression ] = Expression;
                    acceptIt();
                    insideExpr = parseExpr();
                    accept(RSQUARE);
                    accept(EQUALS);
                    expression = parseExpr();
                    accept(SEMICOLON);
                    return new IxAssignStmt(ref, insideExpr, expression, posn);
                } else if (currToken.getTokenType() == LPAREN) {
                    // Ref ( ArgList?);
                    acceptIt();
                    ExprList eL = null;
                    if (currToken.getTokenType() != RPAREN) {
                        eL = parseArgList();
                    }
                    accept(RPAREN);
                    accept(SEMICOLON);
                    return new CallStmt(ref, eL, posn);
                } else {
                    accept(EQUALS);
                    expression = parseExpr();
                    accept(SEMICOLON);
                    return new AssignStmt(ref, expression, posn); }
            case IDENTIFIER:
            default:
                id = new Identifier(currToken);
                posn = currToken.getTokenPosition();
                accept(IDENTIFIER);
                switch (currToken.getTokenType()) {
                    case IDENTIFIER:
                        // Type id = Expression;
                        Identifier varId = new Identifier(currToken);
                        accept(IDENTIFIER);
                        accept(EQUALS);
                        expression = parseExpr();
                        accept(SEMICOLON);
                        varDecl = new VarDecl(new ClassType(id, posn), varId.spelling, posn);
                        return new VarDeclStmt(varDecl, expression, posn);
                    case LSQUARE:
                        acceptIt();
                        if (currToken.getTokenType() != RSQUARE) {
                            // Ref[Expression] = Expression;
                            insideExpr = parseExpr();
                            accept(RSQUARE);
                            accept(EQUALS);
                            expression = parseExpr();
                            accept(SEMICOLON);
                            return new IxAssignStmt(new IdRef(id, posn), insideExpr, expression, posn);
                        } else {
                            // id[] id = Expression;
                            accept(RSQUARE);
                            String name = currToken.getTokenText();
                            accept(IDENTIFIER);
                            accept(EQUALS);
                            expression = parseExpr();
                            accept(SEMICOLON);
                            varDecl = new VarDecl(new ArrayType(new ClassType(id, posn), posn), name, posn);
                            return new VarDeclStmt(varDecl, expression, posn);
                        }
                    case PERIOD:
                        acceptIt();
                        if (matchType(THIS)) {
                            parseError("Reference error");
                            throw new SyntaxError();
                        }
                        Reference qualRef = parseRef();
                        ExprList exprList1 = new ExprList();
                        if (currToken.getTokenType() == LSQUARE) {
                            // id.id[5] = Expression;
                            acceptIt();
                            accept(INTLITERAL);
                            accept(RSQUARE);
                            accept(EQUALS);
                            expression = parseExpr();
                            accept(SEMICOLON);
                            return new AssignStmt(qualRef, expression, posn);
                        } else if (currToken.getTokenType() == EQUALS) {
                            // id.id = Expression;
                            acceptIt();
                            expression = parseExpr();
                            accept(SEMICOLON);
                            return new AssignStmt(qualRef, expression, posn);
                        } else {
                            // id.id();
                            accept(LPAREN);
                            if (currToken.getTokenType() != RPAREN) {
                                exprList1 = parseArgList();
                            }
                            acceptIt();
                            accept(SEMICOLON);
                            return new CallStmt(qualRef, exprList1, null);
                        }
                    case EQUALS:
                        // id = Expression;
                        acceptIt();
                        expression = parseExpr();
                        accept(SEMICOLON);
                        return new AssignStmt(new IdRef(id, posn), expression, posn);
                    case LPAREN:
                        // id();
                        acceptIt();
                        ExprList exprList2 = new ExprList();
                        if (currToken.getTokenType() != RPAREN) {
                            exprList2 = parseArgList();
                        }
                        acceptIt();
                        accept(SEMICOLON);
                        return new CallStmt(new IdRef(id, posn), exprList2, posn);
                }
        }
        return null;
    }

    private Expression parseExpr() {
        return parseOrExpr();
    }

    private Expression parseOrExpr() {
//        System.out.println("or");
        Expression expr1 = parseAndExpr();
        while (currToken.getTokenText().equals("||")) {
            Operator op = new Operator(currToken);
            acceptIt();
            Expression expr2 = parseAndExpr();
            expr1 = new BinaryExpr(op, expr1, expr2, null);
        }
        return expr1;
    }

    private Expression parseAndExpr() {
//        System.out.println("and");
        Expression expr1 = parseEqualityExpr();
        while (currToken.getTokenText().equals("&&")) {
            Operator op = new Operator(currToken);
            acceptIt();
            Expression expr2 = parseEqualityExpr();
            expr1 = new BinaryExpr(op, expr1, expr2, null);
        }
        return expr1;
    }


    private Expression parseEqualityExpr() {
//        System.out.println("eq==");
        Expression expr1 = parseRelExpr();
        while (currToken.getTokenText().equals("==") || currToken.getTokenText().equals("!=")) {
            Operator op = new Operator(currToken);
            acceptIt();
            Expression expr2 = parseRelExpr();
            expr1 = new BinaryExpr(op, expr1, expr2, null);
        }
        return expr1;
    }

    private Expression parseRelExpr() {
//        System.out.println("<>=");
        Expression expr1 = parseAddExpr();
        while (currToken.getTokenText().equals(">") || currToken.getTokenText().equals("<") || currToken.getTokenText().equals(">=")
                || currToken.getTokenText().equals("<=")) {
            Operator op = new Operator(currToken);
            acceptIt();
//            System.out.println("Token: " + currToken.getText());
            Expression expr2 = parseAddExpr();
            expr1 = new BinaryExpr(op, expr1, expr2, null);
        }
        return expr1;
    }

    private Expression parseAddExpr() {
//        System.out.println("+");
        Expression expr1 = parseMulExpr();
        while (currToken.getTokenText().equals("+") || currToken.getTokenText().equals("-")) {
            Operator op = new Operator(currToken);
            acceptIt();
            Expression expr2 = parseMulExpr();
            expr1 = new BinaryExpr(op, expr1, expr2, null);
        }
        return expr1;
    }

    private Expression parseMulExpr() {
//        System.out.println("*");
        Expression expr1 = parseUnaryOpExpr();
//        System.out.println("currToken" + currToken.getText());
        while (currToken.getTokenText().equals("*") || currToken.getTokenText().equals("/")) {
//            System.out.println("MultExpr: " + currToken.getText() + currToken.getType());
            Operator op = new Operator(currToken);
            acceptIt();
            Expression expr2 = parseUnaryOpExpr();
            expr1 = new BinaryExpr(op, expr1, expr2, null);
        }
        return expr1;
    }

    private Expression parseUnaryOpExpr() {
//        System.out.println("unary");
        if (currToken.getTokenText().equals("-") || currToken.getTokenText().equals("!")) {
            Operator op = new Operator(currToken);
            acceptIt();
            Expression expr = parseUnaryOpExpr();
            return new UnaryExpr(op, expr, null);
        } else return parseDefaultExpr();
    }

    private Expression parseDefaultExpr() {
        SourcePosition posn;
//        System.out.println("default expr");
        switch (currToken.getTokenType()) {
            case LPAREN:
                acceptIt();
                Expression expression = parseExpr();
                accept(TokenType.RPAREN);
                return expression;
            case INTLITERAL:
                IntLiteral intLiteral = new IntLiteral(currToken);
                acceptIt();
                return new LiteralExpr(intLiteral, currToken.getTokenPosition());

            case TRUE:
            case FALSE:
                posn = currToken.getTokenPosition();
                BooleanLiteral bool = new BooleanLiteral(currToken);
                acceptIt();
                return new LiteralExpr(bool, posn);
            case NEW:
//                System.out.println("new!");
                posn = currToken.getTokenPosition();
                acceptIt();
                Expression newExpr = handleNew();
                return null;
            case IDENTIFIER:
            case THIS:
            default:
                // Reference ( [ Expression ] | ( Expression ) )?
//                System.out.println(currToken.getTokenType());
//                System.out.println(currToken.getTokenText());
                Reference refr = parseRef();
                if (matchType(TokenType.LSQUARE)) {
                    acceptIt();
                    Expression e = parseExpr();
                    accept(RSQUARE);
                    return new IxExpr(refr, e, null);
                } else if (matchType(TokenType.LPAREN)) {
                    acceptIt();
                    ExprList el = new ExprList();
                    if (currToken.getTokenType() != RPAREN) el = parseArgList();
                    accept(TokenType.RPAREN);
                    return new CallExpr(refr, el, null);
                }
                return null;
        }
    }

    private Expression handleNew() {
        SourcePosition posn;
        Expression expr;
        switch (currToken.getTokenType()) {
            case IDENTIFIER:
                Identifier id = new Identifier(currToken);
                posn = currToken.getTokenPosition();
                acceptIt();
                if (currToken.getTokenType() == LPAREN) {
                    // new A()
                    acceptIt();
                    accept(TokenType.RPAREN);
                    return new NewObjectExpr(new ClassType(id, posn), posn);
                } else if (currToken.getTokenType() == LSQUARE) {
                    // new A[]
                    acceptIt();
                    expr = parseExpr();
                    accept(RSQUARE);
                    return new NewArrayExpr(new ClassType(id, posn), expr, posn);
                } else {
                    parseError("Expression syntax error");
                }
                return null;
            case INT:
                // new int[]
                posn = currToken.getTokenPosition();
                acceptIt();
                accept(TokenType.LSQUARE);
                expr = parseExpr();
                accept(RSQUARE);
                return new NewArrayExpr(new BaseType(TypeKind.INT, posn), expr, posn);
        }
        return null;
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
