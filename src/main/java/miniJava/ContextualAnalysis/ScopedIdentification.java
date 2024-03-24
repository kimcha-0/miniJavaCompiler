package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;

public class ScopedIdentification implements Visitor<Object, Object> {
    public IdentificationTable idTables;

    public ScopedIdentification(AST ast) {
        idTables = new IdentificationTable(new ErrorReporter());
        ast.visit(this, null);

    }
    @Override
    public Object visitPackage(Package prog, Object arg) {
        prog.classDeclList.forEach(cd -> idTables.enter(cd));
        idTables.openScope();
        prog.classDeclList.forEach(cd -> cd.visit(this, null));
        idTables.closeScope();
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        idTables.openScope();
        cd.fieldDeclList.forEach(fd -> fd.visit(this, null));
        cd.methodDeclList.forEach(md -> md.visit(this, null));
        idTables.closeScope();
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        idTables.enter(fd);
        fd.type.visit(this, null);
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        idTables.enter(md);
        md.type.visit(this, null);
        idTables.openScope();
        md.parameterDeclList.forEach(pd -> pd.visit(this, md));
        md.statementList.forEach(stmt -> stmt.visit(this, md));
        idTables.closeScope();
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        pd.type.visit(this, arg);
        idTables.enter(pd);
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        decl.type.visit(this, null);
        idTables.enter(decl);
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        Declaration ret = idTables.retrieve(type.className, null);
        if (!(ret instanceof ClassDecl)) idTables.idError("Attempt to reference class " + type.className.spelling + " but not found");
        else type.classDecl = (ClassDecl)ret;
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        type.eltType.visit(this, md);
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        idTables.openScope();
        stmt.sl.forEach(statement -> statement.visit(this, md));
        idTables.closeScope();
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        stmt.varDecl.visit(this, null);
        stmt.initExp.visit(this, null);
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.val.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        stmt.ref.visit(this, null);
        stmt.ix.visit(this, null);
        stmt.exp.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        stmt.methodRef.visit(this, null);
        stmt.argList.forEach(argu -> argu.visit(this, null));
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, null);
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        stmt.thenStmt.visit(this, null);
        if (stmt.elseStmt != null) stmt.elseStmt.visit(this, null);
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        stmt.cond.visit(this, null);
        stmt.body.visit(this, null);
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        expr.operator.visit(this, null);
        expr.expr.visit(this, null);
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        expr.left.visit(this, null);
        expr.operator.visit(this, null);
        expr.right.visit(this, null);
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, null);
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        expr.ref.visit(this, null);
        expr.ixExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        expr.functionRef.visit(this, null);
        expr.argList.forEach(argu -> argu.visit(this, null));
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        expr.lit.visit(this, null);
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
        expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, null);
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        if (md.isStatic) {
            idTables.idError("Tried to reference non-static this!");
        }
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        ref.decl = (Declaration)ref.id.visit(this, null);
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        ref.ref.visit(this, md);
        Declaration context = ref.ref.decl;
        return null;
    }

    @Override
    public Object visitNullRef(NullRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        Declaration decl = idTables.retrieve(id, md);
        id.setDecl(decl);
        return decl;

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
