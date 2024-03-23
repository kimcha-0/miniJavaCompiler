package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.ErrorReporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class IdentificationTable {
    private Stack<Map<String, Declaration>> tables;
    private ErrorReporter reporter;

    public IdentificationTable(ErrorReporter reporter) {
        this.reporter = reporter;
        this.tables = new Stack<>();
    }

    public ErrorReporter getReporter() {
        return this.reporter;
    }

    public void addDeclaration(Declaration decl) {
        for (int i = tables.size() - 1; i >= 0; i--) {
            if (tables.get(i).containsKey(decl.name)) {
                idError(decl.name + " already declared!");
                System.out.println(decl.name);
                return;
            }
        }
        tables.peek().put(decl.name, decl);
    }

    public void openScope() {
        this.tables.push(new HashMap<>());
    }

    public void closeScope() {
        this.tables.pop();
    }


    public Declaration findDeclaration(Identifier iden, MethodDecl methodDecl) {
        Declaration ret = null;
        for (int i = tables.size() - 1; i >= 0; i--) {
            Declaration candidate = tables.get(i).get(iden.spelling);
            if (candidate != null) {
                // System.out.println(candidate.name);
                ret = candidate;
            }
        }
        if (ret == null) {
            this.reporter.reportError("Attempts to reference: " + iden.spelling + " which was not found!");
            return null;
        }
        return ret;
    }

    public void idError(String error) {
        this.reporter.reportError("Identification error: " + error);
    }
}
