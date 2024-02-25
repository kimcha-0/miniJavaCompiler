package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class NullLiteral extends Terminal {

    @Override
    public String toString() {
        return super.toString();
    }

    public NullLiteral(Token t) {
        super(t);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return null;
    }
}
