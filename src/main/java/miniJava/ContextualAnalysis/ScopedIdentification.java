package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;

public class ScopedIdentification implements Visitor<Declaration, Declaration> {

    private ErrorReporter reporter;
    private IdentificationTable tables;

    public ScopedIdentification(ErrorReporter reporter, AST ast) {
        this.reporter = reporter;
        this.tables = new IdentificationTable(reporter);
        ast.visit(this, null);
    }

    @Override
    public Declaration visitPackage(Package prog, Declaration arg) {
        // enter all classDecl to handle out of order references
        prog.classDeclList.forEach(cd -> tables.enter(cd));
        this.tables.openScope();
        prog.classDeclList.forEach(cd -> cd.fieldDeclList.forEach(fd -> {
            fd.classContext = cd;
            this.tables.enter(fd);
        }));
        prog.classDeclList.forEach(cd -> cd.methodDeclList.forEach(md -> {
            md.classContext = cd;
            this.tables.enter(md);
        }));
        prog.classDeclList.forEach(cd -> cd.visit(this, null));
        this.tables.closeScope();
        return null;
    }

    @Override
    public Declaration visitClassDecl(ClassDecl cd, Declaration arg) {
        // member Declarations already entered
        cd.fieldDeclList.forEach(fd -> fd.visit(this, cd));
        cd.methodDeclList.forEach(md -> md.visit(this, cd));
        return null;
    }

    @Override
    public Declaration visitFieldDecl(FieldDecl fd, Declaration arg) {
        fd.type.visit(this, null);
        if (fd.initExpression != null) {
            fd.initExpression.visit(this, null);
        }
        return null;
    }

    @Override
    public Declaration visitMethodDecl(MethodDecl md, Declaration arg) {
        md.type.visit(this, md);
        md.parameterDeclList.forEach(param -> param.visit(this, md));
        md.statementList.forEach(stmt -> stmt.visit(this, md));
        return null;
    }

    @Override
    public Declaration visitParameterDecl(ParameterDecl pd, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        pd.type.visit(this, context);
        // enter ParameterDecl
        this.tables.enter(pd);
        return null;
    }

    @Override
    public Declaration visitVarDecl(VarDecl decl, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        decl.type.visit(this, context);
        // enter VarDecl
        this.tables.enter(decl);
        return null;
    }

    @Override
    public Declaration visitBaseType(BaseType type, Declaration arg) {
        return null;
    }

    @Override
    public Declaration visitClassType(ClassType type, Declaration arg) {
        type.classDecl = tables.retrieve(type.className);
        return null;
    }

    @Override
    public Declaration visitArrayType(ArrayType type, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        type.eltType.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitBlockStmt(BlockStmt stmt, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        this.tables.openScope();
        stmt.sl.forEach(s -> s.visit(this, context));
        this.tables.closeScope();
        return null;
    }

    @Override
    public Declaration visitVardeclStmt(VarDeclStmt stmt, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.varDecl.visit(this, context);
        stmt.initExp.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitAssignStmt(AssignStmt stmt, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.val.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitIxAssignStmt(IxAssignStmt stmt, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.ref.visit(this, context);
        stmt.ix.visit(this, context);
        stmt.exp.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitCallStmt(CallStmt stmt, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.methodRef.visit(this, context);
        stmt.argList.forEach(args -> args.visit(this, context));
        return null;
    }

    @Override
    public Declaration visitReturnStmt(ReturnStmt stmt, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, context);
        }
        return null;
    }

    @Override
    public Declaration visitIfStmt(IfStmt stmt, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.cond.visit(this, context);
        stmt.thenStmt.visit(this, context);
        if (stmt.elseStmt != null) stmt.elseStmt.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitWhileStmt(WhileStmt stmt, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.cond.visit(this, context);
        stmt.body.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitUnaryExpr(UnaryExpr expr, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.expr.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitBinaryExpr(BinaryExpr expr, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.left.visit(this, context);
        expr.right.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitRefExpr(RefExpr expr, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.ref.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitIxExpr(IxExpr expr, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.ref.visit(this, context);
        expr.ixExpr.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitCallExpr(CallExpr expr, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.functionRef.visit(this, context);
        expr.argList.forEach(args -> args.visit(this, context));
        return null;
    }

    @Override
    public Declaration visitLiteralExpr(LiteralExpr expr, Declaration arg) {
        return null;
    }

    @Override
    public Declaration visitNewObjectExpr(NewObjectExpr expr, Declaration arg) {
        expr.classtype.visit(this, null);
        return null;
    }

    @Override
    public Declaration visitNewArrayExpr(NewArrayExpr expr, Declaration arg) {
        expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, null);
        return null;
    }

    @Override
    public Declaration visitThisRef(ThisRef ref, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        if (!context.isStatic) {
            reporter.reportError("Attempt to reference this in a non-static context");
        }
        return null;
    }

    @Override
    public Declaration visitIdRef(IdRef ref, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        ref.decl = ref.id.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitQRef(QualRef ref, Declaration arg) {
        MethodDecl context = (MethodDecl)arg;
        ref.ref.visit(this, context);
        Declaration refContext = ref.ref.decl;
        ClassDecl classContext = (ClassDecl)ref.ref.decl;
        // example a.b -> IdRef(a).decl = VarDecl
        // search level 1 idTable to resolve id with ref's class context

        return null;
    }

    @Override
    public Declaration visitIdentifier(Identifier id, Declaration arg) {
        Declaration ret = this.tables.retrieve(id);
        id.decl = ret;
        return ret;
    }

    @Override
    public Declaration visitOperator(Operator op, Declaration arg) {
        return null;
    }

    @Override
    public Declaration visitIntLiteral(IntLiteral num, Declaration arg) {
        return null;
    }

    @Override
    public Declaration visitBooleanLiteral(BooleanLiteral bool, Declaration arg) {
        return null;
    }

    @Override
    public Declaration visitNullLiteral(NullLiteral nil, Declaration arg) {
        return null;
    }
}