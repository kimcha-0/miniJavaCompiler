import java.io.FileInputStream;
import java.io.IOException;

import syntacticanalyzer.*;

public class Compiler {
    public static void main(String[] args) {
        ErrorReporter reporter = new ErrorReporter();

        if (args.length < 1) {
            System.out.println("File not provided");
            return;
        }

        try(FileInputStream in = new FileInputStream(args[0])) {
            Scanner scanner = new Scanner(in);
            Parser parser = new Parser();
            parser.parse();
        } catch(Exception e) {
            reporter.reportError("error occurred during scanner or parser instantiation");
        }

        if (reporter.hasErrors()) {

        }
    }
}
