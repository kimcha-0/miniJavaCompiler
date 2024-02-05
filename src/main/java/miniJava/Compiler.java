package miniJava;

import miniJava.SyntacticAnalyzer.Lexer;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.SyntaxError;

import java.io.FileInputStream;
import java.io.IOException;

public class Compiler {
    public static void main(String[] args) {
        ErrorReporter reporter = new ErrorReporter();

        if (args.length < 1) {
            System.out.println("File not provided");
            return;
        } else if (args.length > 2) {
            return;
        }


        try(FileInputStream in = new FileInputStream(args[0])) {
            Lexer lexer = new Lexer(in, reporter);
            Parser parser = new Parser(lexer, reporter);
            try {
                parser.parse();
            } catch (SyntaxError e) {
                e.printStackTrace();
            } finally {
                if (reporter.hasErrors()) {
                    System.out.println("Error");
                    reporter.outputErrors();
                } else System.out.println("Success");
            }
        } catch(IOException e) {
            reporter.reportError(e.getMessage());
        }
    }
}
