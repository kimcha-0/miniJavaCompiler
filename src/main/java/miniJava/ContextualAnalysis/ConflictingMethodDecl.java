package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.MethodDecl;
import miniJava.AbstractSyntaxTrees.Visitor;

import java.util.ArrayList;
import java.util.List;

public class ConflictingMethodDecl extends Declaration {
    public List<MethodDecl> conflictingDeclarations;
    public ConflictingMethodDecl(String name) {
        super(name, null, null);
        this.conflictingDeclarations = new ArrayList<>();
    }

    public void addConflictingDecl(MethodDecl decl) {
        this.conflictingDeclarations.add(decl);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return null;
    }
}
