package miniJava.SyntacticAnalyzer;

public class SourcePosition {
    int line;
    int col;

    public SourcePosition(int line, int col)  {
        this.line = line;
        this.col = col;
    }

    @Override
    public String toString() {
        return "line: " + line + " col: " + col;
    }
}
