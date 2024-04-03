package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;

public class ScopedIdentification implements Visitor<Object, Object> {

    private ErrorReporter reporter;
    private IdentificationTable tables;

    public ScopedIdentification(ErrorReporter reporter, AST ast) {
        this.reporter = reporter;
        this.tables = new IdentificationTable(reporter);
        ast.visit(this, null);
    }

    @Override
    public Object visitPackage(Package prog, Object arg) {
        // enter all classDecl to handle out of order references
        prog.classDeclList.forEach(cd -> tables.enter(cd));
        this.tables.openScope();
        for (ClassDecl classDecl : prog.classDeclList) {
            for (FieldDecl fd : classDecl.fieldDeclList) {
                fd.classContext = classDecl;
                this.tables.enter(fd);
            }
        }
        prog.classDeclList.forEach(cd -> cd.methodDeclList.forEach(md -> {
            md.classContext = cd;
            this.tables.enter(md);
        }));
        prog.classDeclList.forEach(cd -> cd.visit(this, null));
        this.tables.closeScope();
        this.tables.closeScope();
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        // member Declarations already entered
        if (arg == null) {
            cd.fieldDeclList.forEach(fd -> fd.visit(this, cd));
            cd.methodDeclList.forEach(md -> md.visit(this, cd));
        } else {
            Identifier id = (Identifier)arg;
            for (FieldDecl fd : cd.fieldDeclList) {
                if (fd.name.equals(id.spelling)) {
                    return fd;
                }
            }
            for (MethodDecl md : cd.methodDeclList) {
                if (md.name.equals(id.spelling)) {
                    return md;
                }
            }
        }
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        fd.type.visit(this, null);
        if (fd.initExpression != null) {
            fd.initExpression.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        this.tables.openScope();
        md.type.visit(this, md);
        md.parameterDeclList.forEach(param -> param.visit(this, md));
        md.statementList.forEach(stmt -> stmt.visit(this, md));
        this.tables.closeScope();
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        pd.type.visit(this, context);
        // enter ParameterDecl
        this.tables.enter(pd);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        decl.type.visit(this, context);
        // enter VarDecl
        this.tables.enter(decl);
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        type.classDecl = tables.retrieve(type.className, null);
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        type.eltType.visit(this, context);
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        this.tables.openScope();
        stmt.sl.forEach(s -> s.visit(this, context));
        this.tables.closeScope();
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.varDecl.visit(this, context);
        stmt.initExp.visit(this, context);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.val.visit(this, context);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.ref.visit(this, context);
        stmt.ix.visit(this, context);
        stmt.exp.visit(this, context);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.methodRef.visit(this, context);
        stmt.argList.forEach(args -> args.visit(this, context));
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, context);
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.cond.visit(this, context);
        stmt.thenStmt.visit(this, context);
        if (stmt.elseStmt != null) stmt.elseStmt.visit(this, context);
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.cond.visit(this, context);
        stmt.body.visit(this, context);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.expr.visit(this, context);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.left.visit(this, context);
        expr.right.visit(this, context);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.ref.visit(this, context);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.ref.visit(this, context);
        expr.ixExpr.visit(this, context);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.functionRef.visit(this, context);
        expr.argList.forEach(args -> args.visit(this, context));
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.classtype.visit(this, null);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        if (!context.isStatic) {
            reporter.reportError("Attempt to reference this in a non-static context");
        }
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        ref.decl = (Declaration) ref.id.visit(this, context);
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        Declaration ret = null;
        ret = this.tables.retrieve(id, null);
        id.decl = ret;
        return ret;
    }

    @Override
    public Object visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Object visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public Object visitNullLiteral(NullLiteral nil, Object arg) {
        return null;
    }
}