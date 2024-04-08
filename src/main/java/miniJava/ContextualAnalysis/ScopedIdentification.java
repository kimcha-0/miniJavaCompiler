package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Package;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;
import miniJava.IdentificationError;

public class ScopedIdentification implements Visitor<Object, Declaration> {

    private ErrorReporter reporter;
    private IdentificationTable tables;

    public ScopedIdentification(ErrorReporter reporter, AST ast) {
        this.reporter = reporter;
        this.tables = new IdentificationTable(reporter);
        ast.visit(this, null);
    }

    @Override
    public Declaration visitPackage(Package prog, Object arg) {
        // enter all classDecl to handle out of order references
        prog.classDeclList.forEach(cd -> tables.enter(cd));
        this.tables.openScope();
        for (ClassDecl classDecl : prog.classDeclList) {
            for (FieldDecl fd : classDecl.fieldDeclList) {
                fd.classContext = classDecl;
                fd.type.visit(this, null);
                this.tables.enter(fd);
            }
            for (MethodDecl md : classDecl.methodDeclList) {
                md.classContext = classDecl;
                md.type.visit(this, null);
                this.tables.enter(md);
            }
        }

        prog.classDeclList.forEach(cd -> cd.visit(this, null));
        this.tables.closeScope();
        this.tables.closeScope();
        return null;
    }

    @Override
    public Declaration visitClassDecl(ClassDecl cd, Object arg) {
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
    public Declaration visitFieldDecl(FieldDecl fd, Object arg) {
        if (fd.initExpression != null) {
            fd.initExpression.visit(this, null);
        }
        return null;
    }

    @Override
    public Declaration visitMethodDecl(MethodDecl md, Object arg) {
        this.tables.openScope();
        md.parameterDeclList.forEach(param -> param.visit(this, md));
        md.statementList.forEach(stmt -> stmt.visit(this, md));
        this.tables.closeScope();
        return null;
    }

    @Override
    public Declaration visitParameterDecl(ParameterDecl pd, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        pd.type.visit(this, context);
        // enter ParameterDecl
        this.tables.enter(pd);
        return null;
    }

    @Override
    public Declaration visitVarDecl(VarDecl decl, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        decl.type.visit(this, context);
        // enter VarDecl
        this.tables.enter(decl);
        return null;
    }

    @Override
    public Declaration visitBaseType(BaseType type, Object arg) {
        return null;
    }

    @Override
    public Declaration visitClassType(ClassType type, Object arg) {
        type.classDecl = tables.retrieve(type.className, null);
        return null;
    }

    @Override
    public Declaration visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, null);
        return null;
    }

    @Override
    public Declaration visitBlockStmt(BlockStmt stmt, Object arg) {
        this.tables.openScope();
        stmt.sl.forEach(s -> s.visit(this, null));
        this.tables.closeScope();
        return null;
    }

    @Override
    public Declaration visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        if (arg != null) {
            // cannot declare variable in its own scope.
            reporter.reportError("Cannot declare variable in its own scope");
            throw new IdentificationError();
        }
        stmt.varDecl.visit(this, null);
        stmt.initExp.visit(this, stmt.varDecl);
        return null;
    }

    @Override
    public Declaration visitAssignStmt(AssignStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.ref.visit(this, context);
        stmt.val.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitIxAssignStmt(IxAssignStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.ref.visit(this, context);
        stmt.ix.visit(this, context);
        stmt.exp.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitCallStmt(CallStmt stmt, Object arg) {
        stmt.methodRef.visit(this, "call");
        stmt.argList.forEach(args -> args.visit(this, null));
        return null;
    }

    @Override
    public Declaration visitReturnStmt(ReturnStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, context);
        }
        return null;
    }

    @Override
    public Declaration visitIfStmt(IfStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.cond.visit(this, context);
        stmt.thenStmt.visit(this, "then");
        if (stmt.elseStmt != null) stmt.elseStmt.visit(this, "else");
        return null;
    }

    @Override
    public Declaration visitWhileStmt(WhileStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.cond.visit(this, context);
        stmt.body.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitUnaryExpr(UnaryExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.expr.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitBinaryExpr(BinaryExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.left.visit(this, context);
        expr.right.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitRefExpr(RefExpr expr, Object arg) {
        expr.ref.visit(this, null);
        return null;
    }

    @Override
    public Declaration visitIxExpr(IxExpr expr, Object arg) {
        expr.ref.visit(this, null);
        expr.ixExpr.visit(this, null);
        return null;
    }

    @Override
    public Declaration visitCallExpr(CallExpr expr, Object arg) {
        expr.functionRef.visit(this, null);
        expr.argList.forEach(args -> args.visit(this, null));
        return null;
    }

    @Override
    public Declaration visitLiteralExpr(LiteralExpr expr, Object arg) {
        return null;
    }

    @Override
    public Declaration visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        expr.classtype.visit(this, null);
        return null;
    }

    @Override
    public Declaration visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, null);
        return null;
    }

    @Override
    public Declaration visitThisRef(ThisRef ref, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        if (!context.isStatic) {
            reporter.reportError("Attempt to reference this in a non-static context");
        }
        return null;
    }

    @Override
    public Declaration visitIdRef(IdRef ref, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        ref.decl = (Declaration) ref.id.visit(this, context);
        return ref.decl;
    }

    @Override
    public Declaration visitQRef(QualRef ref, Object arg) {
        // A a = new A(); a.x = 2;
        // QualRef(IdRef("a"), "x");
        System.out.println("qual ref visit");
        ref.ref.visit(this, null);
        Declaration context = ref.ref.decl;
        if (context == null) {
            this.reporter.reportError("no context found for reference " + ref.id.spelling);
            throw new IdentificationError();
        } else if (context instanceof ClassDecl) {
            // can only access static members
            ClassDecl classDeclContext = (ClassDecl)context;
            Declaration idDecl = (Declaration)context.visit(this, ref.id);
            if (idDecl instanceof MemberDecl) {
                MemberDecl md = (MemberDecl)idDecl;
                if (!md.isStatic) {
                    this.reporter.reportError("Attempt to access non-static member in class context " + classDeclContext.name);
                }
                ref.id.decl = idDecl;
                ref.decl = ref.id.decl;
                return null;
            }
        } else if (context instanceof LocalDecl) {
//            System.out.println("Referencing local");
            LocalDecl ld = (LocalDecl)context;
            switch (ld.type.typeKind) {
                case CLASS:
                    // local instance of a class; search corresponding class for correct member
//                    System.out.println("referencing class member");
                    Declaration member = (Declaration) ((ClassType)ld.type).classDecl.visit(this, ref.id);
                    if (member == null) {
                        this.reporter.reportError("Attempt to reference " + ref.id.spelling + " but not found in context " + ((ClassType)ld.type).classDecl.name);
                        throw new IdentificationError();
                    }
                    System.out.println("Declaration " + member + " found for identifier " + ref.id);
                    ref.id.decl = member;
                    ref.decl = ref.id.decl;
                    break;
                case ARRAY:
                    if (ref.id.spelling.equals("length")) {
                        ref.id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, null), "length", null);
                        ref.decl = ref.id.decl;
                        break;
                    }
                default:
                    this.reporter.reportError("attempt to reference " + ref.id.spelling + " for type " + ld.type.typeKind);
                    throw new IdentificationError();
            }
        } else if (context instanceof MemberDecl) {
//            System.out.println("Referencing a member");
            MemberDecl md = (MemberDecl)context;
            switch (md.type.typeKind) {
                case CLASS:
//                    System.out.println("referencing class member");
                    Declaration member = (Declaration)((ClassType)md.type).classDecl.visit(this, ref.id);
                    if (member == null) {
                        this.reporter.reportError("Attempt to reference " + ref.id.spelling + " but not found in context " + md.classContext.name);
                        throw new IdentificationError();
                    } else if (arg == null) {
                        if (member instanceof MethodDecl) {
                            this.reporter.reportError("Attempt to reference " + member + " in incorrect context");
                            throw new IdentificationError();
                        }
                    }
                    System.out.println("Declaration " + member + " found for identifier " + ref.id);
                    ref.id.decl = member;
                    ref.decl = ref.id.decl;
                    break;
                    // class member reference
                case ARRAY:
                    // int[] x = new int[5]; x.length();
//                    System.out.println("referencing length method");
                    if (ref.id.spelling.equals("length")) {
                        ref.id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, null), "length", null);
                        ref.decl = ref.id.decl;
                        break;
                    }
                default:
                    this.reporter.reportError("attempt to reference " + ref.id.spelling + " for type " + md.type.typeKind);
                    throw new IdentificationError();
            }
        }
        return null;
    }

    @Override
    public Declaration visitIdentifier(Identifier id, Object arg) {
        Declaration ret = this.tables.retrieve(id, null);
        id.decl = ret;
        return ret;
    }

    @Override
    public Declaration visitOperator(Operator op, Object arg) {
        return null;
    }

    @Override
    public Declaration visitIntLiteral(IntLiteral num, Object arg) {
        return null;
    }

    @Override
    public Declaration visitBooleanLiteral(BooleanLiteral bool, Object arg) {
        return null;
    }

    @Override
    public Declaration visitNullLiteral(NullLiteral nil, Object arg) {
        return null;
    }
}