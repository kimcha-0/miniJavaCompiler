package miniJava;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.ContextualAnalysis.ScopedIdentification;
import miniJava.SyntacticAnalyzer.Lexer;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.SyntaxError;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Compiler {
    public static void main(String[] args) {
        AST syntaxTree = null;
        ErrorReporter reporter = new ErrorReporter();
        if (args.length < 1) {
            throw new IllegalArgumentException("File must be provided for compilation");
        }
        try {
            FileInputStream in = new FileInputStream(args[0]);
            Lexer lexer = new Lexer(in, reporter);
            Parser parser = new Parser(lexer, reporter);
            try {
            syntaxTree = parser.parse();
            } catch (SyntaxError e) {
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (reporter.hasErrors()) {
                System.out.println("Error");
                reporter.outputErrors();
            } else {
                ASTDisplay astDisplay = new ASTDisplay();
                astDisplay.showTree(syntaxTree);
                ScopedIdentification sI = new ScopedIdentification(syntaxTree);
                if (sI.idTables.reporter.hasErrors()) sI.idTables.reporter.outputErrors();

            }
        }

    }
}
