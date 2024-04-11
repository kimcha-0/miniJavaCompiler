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
                fd.type.visit(this, arg);
                this.tables.enter(fd);
            }
            for (MethodDecl md : classDecl.methodDeclList) {
                md.classContext = classDecl;
                md.type.visit(this, arg);
                this.tables.enter(md);
            }
        }

        prog.classDeclList.forEach(cd -> cd.visit(this, arg));
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
            return null;
        }
        return null;
    }

    @Override
    public Declaration visitFieldDecl(FieldDecl fd, Object arg) {
        if (fd.initExpression != null) {
            fd.initExpression.visit(this, arg);
        }
        fd.inClass = (ClassDecl)arg;
        return null;
    }

    @Override
    public Declaration visitMethodDecl(MethodDecl md, Object arg) {
        md.inClass = (ClassDecl)arg;
        this.tables.openScope();
        md.parameterDeclList.forEach(param -> param.visit(this, arg));
        md.statementList.forEach(stmt -> stmt.visit(this, md));
        this.tables.closeScope();
        return null;
    }

    @Override
    public Declaration visitParameterDecl(ParameterDecl pd, Object arg) {
        pd.type.visit(this, arg);
        // enter ParameterDecl
        this.tables.enter(pd);
        return null;
    }

    @Override
    public Declaration visitVarDecl(VarDecl decl, Object arg) {
        decl.type.visit(this, arg);
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
        type.classDecl = tables.retrieve(type.className, "class");
        type.className.decl = type.classDecl;

        return null;
    }

    @Override
    public Declaration visitArrayType(ArrayType type, Object arg) {
        type.eltType.visit(this, arg);
        return null;
    }

    @Override
    public Declaration visitBlockStmt(BlockStmt stmt, Object arg) {
        this.tables.openScope();
        stmt.sl.forEach(s -> s.visit(this, (MethodDecl)arg));
        this.tables.closeScope();
        return null;
    }

    @Override
    public Declaration visitVardeclStmt(VarDeclStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.varDecl.visit(this, context);
        stmt.initExp.visit(this, context);
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
        MethodDecl context = (MethodDecl)arg;
        stmt.methodRef.visit(this, context);
        stmt.argList.forEach(args -> args.visit(this, context));
        return null;
    }

    @Override
    public Declaration visitReturnStmt(ReturnStmt stmt, Object arg) {
        if (stmt.returnExpr != null) {
            stmt.returnExpr.visit(this, (MethodDecl)arg);
        }
        return null;
    }

    @Override
    public Declaration visitIfStmt(IfStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.cond.visit(this, context);
        if (stmt.thenStmt instanceof VarDeclStmt)
            idError("Variable cannot be declared in its own scope in if statement.");
        stmt.thenStmt.visit(this, context);
        if (stmt.elseStmt != null) {
            if (stmt.elseStmt instanceof VarDeclStmt)
                idError("Variable cannot be declared in its own scope in else statement.");
            stmt.elseStmt.visit(this, context);
        }
        return null;
    }

    private void idError(String msg) {
        this.reporter.reportError("Id Error: " + msg);
        throw new IdentificationError();
    }

    @Override
    public Declaration visitWhileStmt(WhileStmt stmt, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        stmt.cond.visit(this, context);
        if (stmt.body instanceof VarDeclStmt) {
            idError("Variable cannot be declared in its own scope in while statement");
        }
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
        MethodDecl context = (MethodDecl)arg;
        expr.ref.visit(this, context);
        if (expr.ref.decl instanceof MethodDecl) {
            idError("Attempt to reference method");
            throw new IdentificationError();
        }
        return null;
    }

    @Override
    public Declaration visitIxExpr(IxExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.ref.visit(this, context);
        expr.ixExpr.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitCallExpr(CallExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.functionRef.visit(this, context);
        expr.argList.forEach(args -> args.visit(this, context));
        return null;
    }

    @Override
    public Declaration visitLiteralExpr(LiteralExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        return null;
    }

    @Override
    public Declaration visitNewObjectExpr(NewObjectExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.classtype.visit(this, null);
        return null;
    }

    @Override
    public Declaration visitNewArrayExpr(NewArrayExpr expr, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        expr.eltType.visit(this, null);
        expr.sizeExpr.visit(this, context);
        return null;
    }

    @Override
    public Declaration visitThisRef(ThisRef ref, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        if (context.isStatic) {
            reporter.reportError("Attempt to reference this in a static context");
        }
        ref.decl = context.inClass;
        return ref.decl;
    }

    @Override
    public Declaration visitIdRef(IdRef ref, Object arg) {
        MethodDecl context = (MethodDecl)arg;
        ref.decl = (Declaration) ref.id.visit(this, arg);
        return ref.decl;
    }

    @Override
    public Declaration visitQRef(QualRef ref, Object arg) {
        MethodDecl currMethodContext = (MethodDecl)arg;
        // A a = new A(); a.x = 2;
        // QualRef(IdRef("a"), "x");
//        System.out.println("qual ref visit");
        ref.ref.visit(this, arg);
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
//                if (!md.isStatic) {
//                    this.reporter.reportError("Attempt to access non-static member in class context " + classDeclContext.name);
//                    throw new IdentificationError();
//                }
                if (md.isPrivate && currMethodContext.inClass != md.classContext) {
                    this.reporter.reportError("Attempt to access private member " + md.name + " in context "
                            + currMethodContext.inClass.name);
                    throw new IdentificationError();
                }
                ref.id.decl = idDecl;
                ref.decl = ref.id.decl;
                return null;
            }
            throw new IdentificationError();
        } else if (context instanceof LocalDecl) {
//            System.out.println("Referencing local");
            LocalDecl ld = (LocalDecl)context;
            switch (ld.type.typeKind) {
                case CLASS:
                    // local instance of a class; search corresponding class for correct member
//                    System.out.println("referencing class member");
                    Declaration member = (Declaration) ((ClassType)ld.type).classDecl.visit(this, ref.id);
                    if (member == null) {
                        this.reporter.reportError("Attempt to reference " + ref.id.spelling +
                                " but not found in context " + ((ClassType)ld.type).classDecl.name);
                        throw new IdentificationError();
                    }
                    if (checkMember(ref, currMethodContext, member)) break;
                    throw new IdentificationError();
                case ARRAY:
                    if (ref.id.spelling.equals("length")) {
                        ref.id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, null),
                                "length", null);
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
                        this.reporter.reportError("Attempt to reference " + ref.id.spelling +
                                " but not found in context " + md.classContext.name);
                        throw new IdentificationError();
                    }
                    if (checkMember(ref, currMethodContext, member)) break;
                    throw new IdentificationError();
                case ARRAY:
                    // int[] x = new int[5]; x.length();
//                    System.out.println("referencing length method");
                    if (ref.id.spelling.equals("length")) {
                        ref.id.decl = new FieldDecl(false, false, new BaseType(TypeKind.INT, null),
                                "length", null);
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

    private boolean checkMember(QualRef ref, MethodDecl currMethodContext, Declaration member) {
        if (member instanceof MemberDecl) {
            MemberDecl memberDecl = (MemberDecl)member;
            if (memberDecl.isPrivate && currMethodContext.inClass != memberDecl.classContext) {
                this.reporter.reportError("Attempt to access private member " + memberDecl.name +
                        " in context " + currMethodContext.inClass.name);
                throw new IdentificationError();
            }
            ref.id.decl = member;
            ref.decl = ref.id.decl;
            return true;
        }
        return false;
    }

    @Override
    public Declaration visitIdentifier(Identifier id, Object arg) {
        MethodDecl md = (MethodDecl)arg;
        Declaration ret;
        ret = this.tables.retrieve(id, md);
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