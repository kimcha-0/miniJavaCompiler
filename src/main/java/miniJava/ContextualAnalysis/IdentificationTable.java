package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.ErrorReporter;
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
        // class _PrintStream { public void println( int n ) { } }
        // class String { }

        MethodDeclList printlnMethodList = new MethodDeclList();
        ParameterDeclList printlnParams = new ParameterDeclList();
        // int n
        printlnParams.add(new ParameterDecl(new BaseType(TypeKind.INT, null), "n", null));
        // public void println( int n ) { }
        MethodDecl println = new MethodDecl(new FieldDecl(false, false, new BaseType(TypeKind.VOID, null), "println", null),
                printlnParams, new StatementList(), null);
        printlnMethodList.add(println);
        ClassDecl printStreamDecl = new ClassDecl("_PrintStream", new FieldDeclList(), printlnMethodList, null);

        ClassDecl stringDecl = new ClassDecl("String", new FieldDeclList(), new MethodDeclList(), null);
        FieldDeclList systemFieldDeclList = new FieldDeclList();
        systemFieldDeclList.add(new FieldDecl(false, true, new ClassType(
                new Identifier(new Token(TokenType.IDENTIFIER, "_PrintStream", null),
                        printStreamDecl), null), "out", null));
        ClassDecl systemDecl = new ClassDecl("System", systemFieldDeclList, new MethodDeclList(), null);

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
        Map<String, Declaration> idTable = this.tables.peek();
        if (idTable.containsKey(decl.name)) {
            if (decl instanceof MemberDecl) {
                if (((MemberDecl)idTable.get(decl.name)).classContext != ((MemberDecl) decl).classContext) {
                    idTable.put(decl.name, decl);
                    return;
                }
            }
            this.reporter.reportError(decl.name + " has already been declared as a " + this.tables.peek().get(decl.name));
        } else {
            this.tables.peek().put(decl.name, decl);
//            System.out.println("entering: " + tables);
        }
    }

    public Declaration retrieve(Identifier iden, Declaration context) {
        Declaration ret = null;

        for (int i = this.tables.size() - 1; i > -1; i--) {
            if (this.tables.get(i).containsKey(iden.spelling)) {
                ret = this.tables.get(i).get(iden.spelling);
                System.out.println("Declaration " + this.tables.get(i).get(iden.spelling) + " found for identifier " + iden.spelling);
                break;
            }
        }
        if (ret == null) {
            this.reporter.reportError("No declaration found for identifier " + iden.spelling);
            return null;
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
