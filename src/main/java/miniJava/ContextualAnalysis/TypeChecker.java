package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Token;

public class TypeChecker implements Visitor<Object, TypeDenoter> {
    private ErrorReporter reporter;

    public TypeChecker(AST ast, ErrorReporter reporter) {
        this.reporter = reporter;
        ast.visit(this, null);
    }

    private void reportTypeError(String msg) {
        this.reporter.reportError("Type Error: " + msg);
    }

    private TypeDenoter verifyBinaryExpr(TypeDenoter type1, TypeDenoter type2, String operator) {
        switch (operator) {
            case "||":
            case "&&":
                if (type1.typeKind == TypeKind.BOOLEAN && type2.typeKind == TypeKind.BOOLEAN)
                    return new BaseType(TypeKind.BOOLEAN, null);
            case "+":
            case "-":
            case "/":
            case "*":
                if (type1.typeKind == TypeKind.INT && type2.typeKind == TypeKind.INT)
                    return new BaseType(TypeKind.INT, null);
            case ">":
            case ">=":
            case "<":
            case "<=":
                // int op int -> bool
                if (type1.typeKind == TypeKind.INT && type2.typeKind == TypeKind.INT) {
                    return new BaseType(TypeKind.BOOLEAN, null);
                }
            case "==":
            case "!=":
                // a op a -> bool
                if (matchType(type1, type2))
                    return new BaseType(TypeKind.BOOLEAN, null);
            default:
                if (type1.typeKind == TypeKind.UNSUPPORTED || type2.typeKind == TypeKind.UNSUPPORTED)
                    return new BaseType(TypeKind.UNSUPPORTED, null);
                reportTypeError("Invalid binary Expression: " + type1.typeKind + " " + operator
                + " " + type2.typeKind + "-> UNSUPPORTED");
                return new BaseType(TypeKind.UNSUPPORTED, null);
        }
    }

    private boolean matchType(TypeDenoter type1, TypeDenoter type2) {
        if (type1.typeKind == TypeKind.CLASS || type2.typeKind == TypeKind.CLASS) {
            if (type1.typeKind != TypeKind.CLASS || type2.typeKind != TypeKind.CLASS) {
                reportTypeError("Invalid class comparison");
                return false;
            }
            ClassType classType1 = (ClassType)type1;
            ClassType classType2 = (ClassType)type2;
//            System.out.println(classType1.className + " " + classType2.className);
            if (classType1.className.spelling.equals(classType2.className.spelling))
                return true;
            else
                reportTypeError("Cannot compare instance of class " + classType1.className.spelling + " and "
                + classType2.className.spelling);
        }
        return type1.typeKind == type2.typeKind;
    }

    @Override
    public TypeDenoter visitPackage(Package prog, Object arg) {
        prog.classDeclList.forEach(cd -> cd.visit(this, null));
        return null;
    }

    @Override
    public TypeDenoter visitClassDecl(ClassDecl cd, Object arg) {
        cd.fieldDeclList.forEach(fd -> fd.visit(this, null));
        cd.methodDeclList.forEach(md -> md.visit(this, null));
        return cd.type;
    }

    @Override
    public TypeDenoter visitFieldDecl(FieldDecl fd, Object arg) {
        return fd.type;
    }

    @Override
    public TypeDenoter visitMethodDecl(MethodDecl md, Object arg) {
        // verify return type and all returned objects' types align
        md.parameterDeclList.forEach(param -> param.visit(this, null));
        md.statementList.forEach(sl -> sl.visit(this, md));
        return md.type;
    }

    @Override
    public TypeDenoter visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.type.visit(this, null);
        return pd.type;
    }

    @Override
    public TypeDenoter visitVarDecl(VarDecl decl, Object arg) {
        // retrieve declaration type
        return decl.type;
    }

    @Override
    public TypeDenoter visitBaseType(BaseType type, Object arg) {
        return type;
    }

    @Override
    public TypeDenoter visitClassType(ClassType type, Object arg) {
        return type;
    }

    @Override
    public TypeDenoter visitArrayType(ArrayType type, Object arg) {
        return type;
    }

    @Override
    public TypeDenoter visitBlockStmt(BlockStmt stmt, Object arg) {
        stmt.sl.forEach(st -> st.visit(this, null));
        return null;
    }

    @Override
    public TypeDenoter visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        TypeDenoter varDeclType = stmt.varDecl.visit(this, null);
        if (stmt.initExp != null) {
            TypeDenoter initExpType = stmt.initExp.visit(this, null);
//            System.out.println(initExpType);
            if (!matchType(varDeclType, initExpType)){
                reportTypeError("Variable declaration " + stmt.varDecl.name + " of type " +
                        varDeclType.typeKind + " assigned value of type " + initExpType.typeKind);
            }
        }
        return null;
    }

    @Override
    public TypeDenoter visitAssignStmt(AssignStmt stmt, Object arg) {
        TypeDenoter assignRefType = stmt.ref.visit(this, null);
        TypeDenoter assignValType = stmt.val.visit(this, null);
        if (!matchType(assignRefType, assignValType)) {
            reportTypeError("attempt to assign value of type "
                    + assignValType.typeKind.name() + " to reference of type " + assignRefType.typeKind.name());
            // in error, return unsupported type?
            return new BaseType(TypeKind.UNSUPPORTED, null);
        }
        return assignRefType;
    }

    @Override
    public TypeDenoter visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        TypeDenoter ixType = stmt.ix.visit(this, null);
        TypeDenoter arrayType = stmt.ref.visit(this, null);
        if (arrayType.typeKind != TypeKind.ARRAY) {
            reportTypeError("Attempt to index a non-array object: " + stmt.ref.decl.name);
        }
        if (ixType.typeKind != TypeKind.INT) {
            reportTypeError(ixType.typeKind + " used for array index");
        }
        TypeDenoter valType = stmt.exp.visit(this, null);
        if (valType.typeKind != ((ArrayType)arrayType).eltType.typeKind) {
            reportTypeError("Attempt to assign value of type " + valType.typeKind +
                    " to array of type " +((ArrayType)arrayType).eltType.typeKind);
        }
        return valType;
    }

    @Override
    public TypeDenoter visitCallStmt(CallStmt stmt, Object arg) {
        // Reference(ArgumentList?);
        // verify param count and types are valid with function declaration
        MethodDecl originalMethodDecl = (MethodDecl) stmt.methodRef.decl;
        if (originalMethodDecl.parameterDeclList.size() != stmt.argList.size()) {
            this.reporter.reportError("Attempt to call method " + originalMethodDecl.name +
                    " with incorrect number of arguments");
            return null;
        }
        for (int i = 0; i < stmt.argList.size(); i++) {
            TypeDenoter argType = stmt.argList.get(i).visit(this, null);
            TypeDenoter methodDeclParamType = originalMethodDecl.parameterDeclList.get(i).type;
            if (!matchType(argType, methodDeclParamType)) {
                reportTypeError("Attempt to call method " + originalMethodDecl.name +
                        " with incorrect type for argument " + originalMethodDecl.parameterDeclList.get(i));
                return null;
            }
        }
        return null;
    }

    @Override
    public TypeDenoter visitReturnStmt(ReturnStmt stmt, Object arg) {
        // ensure calling method return type matches return expr type
        MethodDecl sourceMethod = (MethodDecl)arg;
        TypeDenoter returnType = new BaseType(TypeKind.VOID, null);
        if (stmt.returnExpr != null) {
            returnType = stmt.returnExpr.visit(this, null);
        }
        if (!matchType(returnType, sourceMethod.type)) {
            reportTypeError("return type " + returnType.typeKind + " does not match method return type of "
                    + sourceMethod.type.typeKind);
        }
        return returnType;
    }

    @Override
    public TypeDenoter visitIfStmt(IfStmt stmt, Object arg) {
        // verify condition is boolean
        TypeDenoter conditionType = stmt.cond.visit(this, null);
        if (conditionType.typeKind != TypeKind.BOOLEAN) {
            reportTypeError("If conditional expression is of type " + conditionType.typeKind);
        }
        return null;
    }

    @Override
    public TypeDenoter visitWhileStmt(WhileStmt stmt, Object arg) {
        TypeDenoter conditionType = stmt.cond.visit(this, null);
        if (conditionType.typeKind != TypeKind.BOOLEAN) {
            reportTypeError("If conditional expression is of type " + conditionType.typeKind);
        }
        return null;
    }

    @Override
    public TypeDenoter visitUnaryExpr(UnaryExpr expr, Object arg) {
        // -^* int || !^* boolean
        String op = expr.operator.spelling;
        TypeDenoter exprType = expr.expr.visit(this, null);
        if (op.equals("-")) {
            if (exprType.typeKind != TypeKind.INT) {
                reportTypeError("attempt to use unary operator (" + op + ") with type " + exprType.typeKind);
                return new BaseType(TypeKind.UNSUPPORTED, null);
            }
            return new BaseType(TypeKind.INT, null);
        } else {
            if (exprType.typeKind != TypeKind.BOOLEAN) {
                reportTypeError("attempt to use unary operator (" + op + ") with type " + exprType.typeKind);
                return new BaseType(TypeKind.UNSUPPORTED, null);
            }
            return new BaseType(TypeKind.BOOLEAN, null);
        }
    }

    @Override
    public TypeDenoter visitBinaryExpr(BinaryExpr expr, Object arg) {
        TypeDenoter leftType = expr.left.visit(this, null);
        String operator = expr.operator.spelling;
        TypeDenoter rightType = expr.right.visit(this, null);
        TypeDenoter binaryExprType = verifyBinaryExpr(leftType, rightType, operator);
        return binaryExprType;
    }

    @Override
    public TypeDenoter visitRefExpr(RefExpr expr, Object arg) {
        return expr.ref.visit(this, null);
    }

    @Override
    public TypeDenoter visitIxExpr(IxExpr expr, Object arg) {
        // verify index is int
        // return element type of array
        TypeDenoter arrRefType = expr.ref.visit(this, null);
        if (arrRefType.typeKind != TypeKind.ARRAY) {
            reportTypeError("Attempt to index reference " + expr.ref.decl.name + " of type " + arrRefType.typeKind);
            return new BaseType(TypeKind.UNSUPPORTED, null);
        }
        // verify reference is an array
        TypeDenoter ixType = expr.ixExpr.visit(this, null);
        if (ixType.typeKind != TypeKind.INT) {
            reportTypeError("Index expression is of type " + ixType.typeKind);
        }
        return ((ArrayType)arrRefType).eltType;
    }

    @Override
    public TypeDenoter visitCallExpr(CallExpr expr, Object arg) {
        // verify that number and types of args match
        // return the return type
        MethodDecl originalMethodDecl = (MethodDecl) expr.functionRef.decl;
        if (originalMethodDecl.parameterDeclList.size() != expr.argList.size()) {
            this.reporter.reportError("Attempt to call method " + originalMethodDecl.name +
                    " with incorrect number of arguments");
        }
        for (int i = 0; i < expr.argList.size(); i++) {
            TypeDenoter argType = expr.argList.get(i).visit(this, null);
            TypeDenoter methodDeclParamType = originalMethodDecl.parameterDeclList.get(i).type;
            if (!matchType(argType, methodDeclParamType)) {
                reportTypeError("Attempt to call method " + originalMethodDecl.name +
                        " with incorrect type for argument " + originalMethodDecl.parameterDeclList.get(i));
            }
        }
        return originalMethodDecl.type;
    }

    @Override
    public TypeDenoter visitLiteralExpr(LiteralExpr expr, Object arg) {
        return expr.lit.visit(this, null);
    }

    @Override
    public TypeDenoter visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        // new id();
        return expr.classtype;
    }

    @Override
    public TypeDenoter visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        // new x[5];
        TypeDenoter sizeExprType = expr.sizeExpr.visit(this, null);
        if (sizeExprType.typeKind != TypeKind.INT) {
            reportTypeError("Index expression is of type " + sizeExprType.typeKind);
        }
        TypeDenoter eltType = expr.eltType;
        return new ArrayType(eltType, null);
    }

    @Override
    public TypeDenoter visitThisRef(ThisRef ref, Object arg) {
        // return ClassType of "this" instance
        return ref.decl.type;
    }

    @Override
    public TypeDenoter visitIdRef(IdRef ref, Object arg) {
        // return type of identifier
        TypeDenoter idType = ref.id.visit(this, null);
        return idType;
    }

    @Override
    public TypeDenoter visitQRef(QualRef ref, Object arg) {
        return ref.id.decl.type;
    }

    @Override
    public TypeDenoter visitIdentifier(Identifier id, Object arg) {
        // identification traversal should have tagged all identifiers with declarations
        // thus, we return the TypeDenoter for the identifier's declaration
        return id.decl.visit(this, null);
    }

    @Override
    public TypeDenoter visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public TypeDenoter visitIntLiteral(IntLiteral num, Object arg) {
        return new BaseType(TypeKind.INT, null);
    }

    @Override
    public TypeDenoter visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return new BaseType(TypeKind.BOOLEAN, null);
    }

    @Override
    public TypeDenoter visitNullLiteral(NullLiteral nil, Object arg) {
        // return ClassType
        Identifier nullIdentifier = new Identifier(new Token(nil.kind, "null", null), null);
        ClassType nullType = new ClassType(nullIdentifier, null);
        nullType.classDecl = new ClassDecl("Null", new FieldDeclList(), new MethodDeclList(), null);
        return nullType;
    }
}