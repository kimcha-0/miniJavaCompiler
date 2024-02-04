package syntacticanalyzer;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.Lexer;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

import static org.junit.jupiter.api.Assertions.*;
import static miniJava.SyntacticAnalyzer.TokenType.*;
import java.io.FileInputStream;
import java.io.IOException;

class PA1Test {

    @org.junit.jupiter.api.Test
    void scan() {
        try (FileInputStream in = new FileInputStream("pa1-tests-partial/pass144.java")) {
            ErrorReporter reporter = new ErrorReporter();
            Lexer lexer = new Lexer(in, reporter);
            assertToken(lexer, CLASS,"class");
            assertToken(lexer, IDENTIFIER, "id");
            assertToken(lexer, LCURLY, "{");
            assertToken(lexer, INT, "int");
            assertToken(lexer, IDENTIFIER, "x");
            assertToken(lexer, SEMICOLON, ";");
            assertToken(lexer, RCURLY, "}");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void assertToken(Lexer lexer, TokenType expectType, String expectStr) {
        Token token = lexer.scan();
        assertEquals(expectType, token.getType());
        assertEquals(expectStr, token.getText());
        System.out.println(token.getText());
    }

    @org.junit.jupiter.api.Test
    void parse() {
        try(FileInputStream in = new FileInputStream("pa1-tests-partial/fail101.java")) {
            ErrorReporter reporter = new ErrorReporter();
            Lexer lexer = new Lexer(in, reporter);
            Parser parser = new Parser(lexer, reporter);
            parser.parse();
            assert(reporter.hasErrors());
            if (reporter.hasErrors()) reporter.outputErrors();
            else System.out.println("Success");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
