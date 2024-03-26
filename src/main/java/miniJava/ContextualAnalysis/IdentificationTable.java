package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.Identifier;
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
        // init String, System
        openScope();
    }

    public void openScope() {
        System.out.println("Opening Scope: " + this.tables);
        this.tables.push(new HashMap<>());
    }

    public void closeScope() {
        System.out.println("Closing Scope: " + this.tables);
        this.tables.pop();
    }

    public void enter(Declaration decl) {
        if (this.tables.peek().containsKey(decl.name)) {
            this.reporter.reportError(decl.name + " has already been declared as a " + this.tables.peek().get(decl.name));
        } else {
            this.tables.peek().put(decl.name, decl);
        }
    }

    public Declaration retrieve(Identifier iden) {
        Declaration ret = null;
        for (int i = this.tables.size() - 1; i > -1; i--) {
            if (this.tables.get(i).containsKey(iden.spelling)) {
                ret = this.tables.get(i).get(iden.spelling);
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
