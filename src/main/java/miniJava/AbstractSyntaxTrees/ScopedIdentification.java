package miniJava.AbstractSyntaxTrees;

import miniJava.IdentificationError;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
public class ScopedIdentification implements Visitor {
    private Stack<Map<String, Declaration>> siTable;

    public ScopedIdentification() {
        this.siTable = new Stack<>();
        // push Level 0 and 1 IDTables
        for (int i = 0; i < 2; i++) {
            this.siTable.push(new HashMap<>());
        }
    }

    public void openScope() {
        Map<String, Declaration> idTable = new HashMap<>();
        this.siTable.push(idTable);
    }

    public Map<String, Declaration> closeScope() {
        return this.siTable.pop();
    }

    public void addDeclaration(String spelling, Declaration decl) {
        if (this.siTable.peek().containsKey(spelling)) throw new IdentificationError();
        else this.siTable.peek().put(spelling, decl);
    }

    public Declaration findDeclaration(Identifier id) {
        for (Map<String, Declaration> idTable : this.siTable) {
            if (idTable.containsKey(id.spelling)) {
                return idTable.get(id.spelling);
            }
        }
        return null;
    }
    @Override
    public Object visitPackage(Package prog, Object arg) {
        return null;
    }

    @Override
    public Object visitClassDecl(ClassDecl cd, Object arg) {
        return null;
    }

    @Override
    public Object visitFieldDecl(FieldDecl fd, Object arg) {
        return null;
    }

    @Override
    public Object visitMethodDecl(MethodDecl md, Object arg) {
        return null;
    }

    @Override
    public Object visitParameterDecl(ParameterDecl pd, Object arg) {
        return null;
    }

    @Override
    public Object visitVarDecl(VarDecl decl, Object arg) {
        return null;
    }

    @Override
    public Object visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Object visitClassType(ClassType type, Object arg) {
        return null;
    }

    @Override
    public Object visitArrayType(ArrayType type, Object arg) {
        return null;
    }

    @Override
    public Object visitBlockStmt(BlockStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitAssignStmt(AssignStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitCallStmt(CallStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt stmt, Object arg) {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitRefExpr(RefExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitIxExpr(IxExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        return null;
    }

    @Override
    public Object visitThisRef(ThisRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdRef(IdRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitQRef(QualRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitNullRef(NullRef ref, Object arg) {
        return null;
    }

    @Override
    public Object visitIdentifier(Identifier id, Object arg) {
        return null;
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
