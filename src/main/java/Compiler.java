import java.io.FileInputStream;

import syntacticanalyzer.*;

public class Compiler {
    public static void main(String[] args) {
        ErrorReporter reporter = new ErrorReporter();

        if (args.length < 1) {
            System.out.println("File not provided");
            return;
        }

        try(FileInputStream in = new FileInputStream(args[0])) {
            Lexer lexer = new Lexer(in);
            Parser parser = new Parser(lexer);
            parser.parse();
        } catch(Exception e) {
            reporter.reportError("error occurred during scanner or parser instantiation");
        }

        if (reporter.hasErrors()) {
            System.out.println("Errors");
        } else System.out.println("Success");
    }
}
