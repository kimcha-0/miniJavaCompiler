package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NullRef extends Reference {
    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return null;
    }

    public NullRef(SourcePosition posn) {
        super(posn);
    }
}
