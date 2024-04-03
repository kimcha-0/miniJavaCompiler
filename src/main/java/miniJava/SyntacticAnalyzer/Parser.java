package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.Package;

public interface Parser {
    /** entry point into recursive parsing */
   Package parse();
}
