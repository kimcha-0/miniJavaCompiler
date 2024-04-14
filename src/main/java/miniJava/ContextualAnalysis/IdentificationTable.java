package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;
import miniJava.IdentificationError;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class IdentificationTable {
    private Stack<Map<String, Declaration>> tables;
    private ErrorReporter reporter;

    public IdentificationTable(ErrorReporter reporter) {
        this.reporter = reporter;
        this.tables = new Stack<>();
        openScope();
        // class System { public static _PrintStream out; }
        // fielddecl is static, public,
        // class _PrintStream { public void println( int n ) { } }
        // class String { }

        MethodDeclList printlnMethodList = new MethodDeclList();
        ParameterDeclList printlnParams = new ParameterDeclList();
        // int n
        printlnParams.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
        // public void println( int n ) { }
        MethodDecl println = new MethodDecl(new FieldDecl(false, false, new BaseType(TypeKind.VOID,
                null), "println", null),
                printlnParams, new StatementList(), null);

        printlnMethodList.add(println);

        ClassDecl printStreamDecl = new ClassDecl("_PrintStream", new FieldDeclList(),
                printlnMethodList, null);

        println.classContext = printStreamDecl;


        ClassDecl stringDecl = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);

        FieldDeclList systemFieldDeclList = new FieldDeclList();
        ClassType printStreamType = new ClassType(new Identifier(new Token(TokenType.IDENTIFIER,
                "_PrintStream", null)), null);
        printStreamType.classDecl = printStreamDecl;
        FieldDecl out = new FieldDecl(false, true, printStreamType, "out", null);
        ClassDecl systemDecl = new ClassDecl("System", systemFieldDeclList, new MethodDeclList(), null);
        out.classContext = systemDecl;
        systemFieldDeclList.add(out);
        this.enter(stringDecl);
        this.enter(printStreamDecl);
        this.enter(systemDecl);
    }

    public void openScope() {
//        System.out.println("Opening Scope: " + this.tables);
        this.tables.push(new HashMap<>());
    }

    public void closeScope() {
//        System.out.println("Closing Scope: " + this.tables);
        this.tables.pop();
    }

    public void enter(Declaration decl) {
        // search up to level 2 table
        // TODO: fix this method!
        if (decl instanceof ClassDecl) {
            if (this.tables.peek().containsKey(decl.name)) {
                this.reporter.reportError("Id Error: Class Decl already exists");
                throw new IdentificationError();
            }
            this.tables.peek().put(decl.name, decl);
        } else if (decl instanceof MemberDecl) {
            if (this.tables.peek().containsKey(decl.name)) {
                checkMemberDuplicate(decl, this.tables.peek());
                return;
            }
            this.tables.peek().put(decl.name, decl);
        } else {
            // search up to level 2 table
            for (int i = this.tables.size() - 1; i > 1; i--) {
                if (this.tables.get(i).containsKey(decl.name)) {
                    this.reporter.reportError("Id Error: " + decl.name + " already exists");
                    throw new IdentificationError();
                }
            }
            this.tables.peek().put(decl.name, decl);
        }
    }


    private void checkMemberDuplicate(Declaration decl, Map<String, Declaration> idTable) {
        if (((MemberDecl) idTable.get(decl.name)).classContext != ((MemberDecl) decl).classContext) {
            idTable.put(decl.name, decl);
        } else {
            this.reporter.reportError(decl.name + " has already been declared as a "
                    + this.tables.peek().get(decl.name));
            throw new IdentificationError();
        }
    }

    public Declaration retrieve(Identifier iden, Object context) {
        Declaration ret = null;
        if (context == "class") {
            if (this.tables.get(0).containsKey(iden.spelling)) {
                ret = this.tables.get(0).get(iden.spelling);
                return ret;
            } else {
                this.reporter.reportError("No declaration found for identifier " + iden.spelling);
                return null;
            }
        }
        for (int i = this.tables.size() - 1; i > -1; i--) {
            if (this.tables.get(i).containsKey(iden.spelling)) {
                ret = this.tables.get(i).get(iden.spelling);
//                System.out.println("Declaration " + this.tables.get(i).get(iden.spelling) 
//                + " found for identifier " + iden.spelling);
                break;
            }
        }
        if (ret == null) {
            this.reporter.reportError("No declaration found for identifier " + iden.spelling);
            return null;
        } else if (ret instanceof VarDecl && !(((VarDecl) ret).init)){
            this.reporter.reportError("Attempt to reference variable that has not yet been initialized");
            return null;
        }
        if (context instanceof MethodDecl && ret instanceof MemberDecl) {
            if (((MethodDecl) context).isStatic && !((MemberDecl) ret).isStatic) {
                this.reporter.reportError("Attempt to reference non-static member " + ret.name
                        + " in static context");
                return null;
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            ret.append("Level ").append(i).append(": ").append(tables.get(i)).append("\n");
        }
        return ret.toString();
    }
}
