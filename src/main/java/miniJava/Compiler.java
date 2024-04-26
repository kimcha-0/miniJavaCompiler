package miniJava;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.CodeGeneration.CodeGenerator;
import miniJava.ContextualAnalysis.ScopedIdentification;
import miniJava.ContextualAnalysis.TypeChecker;
import miniJava.SyntacticAnalyzer.LexerImpl;
import miniJava.SyntacticAnalyzer.ParserImpl;
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
            LexerImpl lexer = new LexerImpl(in, reporter);
            ParserImpl parser = new ParserImpl(lexer, reporter);
            try {
            syntaxTree = parser.parse();
            } catch (SyntaxError e) {
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (reporter.hasErrors()) {
//                System.out.println("Error");
                reporter.outputErrors();
            } else {
                ASTDisplay astDisplay = new ASTDisplay();
                try {
                    ScopedIdentification sI = new ScopedIdentification(reporter, syntaxTree);
                } catch (IdentificationError e) {
                } finally {
                    if (reporter.hasErrors()) {
//                        System.out.println("Error");
                        reporter.outputErrors();
                    } else {
                        TypeChecker typeChecker = new TypeChecker(syntaxTree, reporter);
                        if (reporter.hasErrors()) {
//                            System.out.println("Error");
                            reporter.outputErrors();
                        } else {
                                CodeGenerator codeGen = new CodeGenerator(reporter, syntaxTree);
                                if (reporter.hasErrors()) {
                                    reporter.outputErrors();
                                }
                        }
                }
                // astDisplay.showTree(syntaxTree);
                }

            }
        }

    }
}
