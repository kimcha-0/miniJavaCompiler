package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.ErrorReporter;
import miniJava.IdentificationError;

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

    public void addDeclaration(Declaration decl) {
        Map<String, Declaration> idTable = this.tables.peek();
        if (idTable.containsKey(decl.name)) {
            throw new IdentificationError();
        }
        tables.peek().put(decl.name, decl);
    }

    public void openScope() {
        this.tables.push(new HashMap<>());
    }

    public void closeScope() {
        this.tables.pop();
    }


    public Declaration findDeclaration(Declaration decl) {
        return this.tables.peek().containsKey(decl.name) ? decl : null;
    }

    public void idError(String error) {
        this.reporter.reportError("Identification error: " + error);
    }
}
