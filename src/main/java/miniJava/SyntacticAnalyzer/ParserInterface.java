package miniJava.SyntacticAnalyzer;

import miniJava.AbstractSyntaxTrees.Package;

public interface ParserInterface {
    /** entry point into recursive parsing */
   Package parse();
}
