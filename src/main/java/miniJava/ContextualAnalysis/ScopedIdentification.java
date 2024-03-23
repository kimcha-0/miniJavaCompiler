package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;

public class ScopedIdentification implements Visitor<Object, Object> {
    public IdentificationTable idTables;

    public ScopedIdentification(AST ast) {
        // init idTable stack
        this.idTables = new IdentificationTable(new ErrorReporter());
        ast.visit(this, null);
    }

    @Override
    public Object visitPackage(Package prog, Object arg) {
        idTables.openScope();
        prog.classDeclList.forEach(cd -> idTables.addDeclaration(cd));
        prog.classDeclList.forEach(cd -> cd.visit(this, prog));
        idTables.closeScope();
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        idTables.openScope();
        cd.fieldDeclList.forEach(fd -> fd.visit(this, cd));
        cd.methodDeclList.forEach(md -> md.visit(this, cd));
        idTables.closeScope();
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        fd.type.visit(this, null);
        ClassDecl context = (ClassDecl) arg;
        fd.classContext = context;
        this.idTables.addDeclaration(fd);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        ClassDecl context = (ClassDecl)arg;
        md.classContext = context;
        this.idTables.addDeclaration(md);
        md.type.visit(this, null);

        this.idTables.openScope();
        md.parameterDeclList.forEach(pd -> pd.visit(this, md));

        this.idTables.openScope();
        md.statementList.forEach(stmt -> stmt.visit(this, md));

        this.idTables.closeScope();
        this.idTables.closeScope();

        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.type.visit(this, null);
        this.idTables.addDeclaration(pd);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl vd, Object arg) {
        vd.type.visit(this, null);
        this.idTables.addDeclaration(vd);
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        this.idTables.findDeclaration(type.className, null);
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, null);
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        this.idTables.openScope();
        stmt.sl.forEach(s -> s.visit(this, md));
        this.idTables.closeScope();
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        stmt.varDecl.visit(this, null);
        stmt.initExp.visit(this, md);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        stmt.ref.visit(this, md);
        stmt.val.visit(this, md);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        stmt.ref.visit(this, md);
        stmt.ix.visit(this, md);
        stmt.exp.visit(this, md);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        stmt.methodRef.visit(this, md);
        stmt.argList.forEach(argu -> argu.visit(this, md));
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        if (stmt.returnExpr != null) stmt.returnExpr.visit(this, md);
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        stmt.cond.visit(this, md);
        stmt.thenStmt.visit(this, md);
        if (stmt.elseStmt != null) stmt.elseStmt.visit(this, md);
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        stmt.cond.visit(this, md);
        stmt.body.visit(this, md);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        expr.operator.visit(this, null);
        expr.expr.visit(this, md);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        expr.operator.visit(this, null);
        expr.left.visit(this, md);
        expr.right.visit(this, md);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        expr.ref.visit(this, md);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        expr.ref.visit(this, md);
        expr.ixExpr.visit(this, md);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        expr.functionRef.visit(this, md);
        expr.argList.forEach(argu -> argu.visit(this, md));
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        expr.classtype.visit(this, md);
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        expr.eltType.visit(this, md);
        expr.sizeExpr.visit(this, md);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        if (md.isStatic) {
            idTables.idError("attempts to reference non-static this");
        }
        ref.classContext = md.classContext;
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        ref.id.visit(this, md);
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        // get overall context
        MethodDecl md = (MethodDecl)arg;
        ref.ref.visit(this, md);
        Declaration context = ref.ref.decl;
        if (context == null) {
            this.idTables.idError("no context found for reference " + ref.id.spelling);
            return null;
        }
        return null;
    }

    @Override
    public Object visitNullRef(NullRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return idTables.findDeclaration(id, (MethodDecl)arg);
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
