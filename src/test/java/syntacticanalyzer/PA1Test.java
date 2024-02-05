package syntacticanalyzer;

import miniJava.ErrorReporter;
import miniJava.SyntacticAnalyzer.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static miniJava.SyntacticAnalyzer.TokenType.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class PA1Test {

    @org.junit.jupiter.api.Test
    void scan() {
        try (FileInputStream in = new FileInputStream("pa1-tests-partial/fail151.java")) {
            ErrorReporter reporter = new ErrorReporter();
            Lexer lexer = new Lexer(in, reporter);
            assertToken(lexer, CLASS, "class");
            assertToken(lexer, IDENTIFIER, "LValueFail");
            assertToken(lexer, LCURLY, "{");
            assertToken(lexer, VOID, "void");
            assertToken(lexer, IDENTIFIER, "foo");
            assertToken(lexer, LPAREN, "(");
            assertToken(lexer, RPAREN, ")");
            assertToken(lexer, LCURLY, "{");
            assertToken(lexer, TRUE, "true");
            assertToken(lexer, EQUALS, "=");
            assertToken(lexer, FALSE, "false");
            assertToken(lexer, SEMICOLON, ";");
            assertToken(lexer, RCURLY, "}");
            assertToken(lexer, RCURLY, "}");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void assertToken(Lexer lexer, TokenType expectType, String expectStr) {
        Token token = lexer.scan();
        assertEquals(expectType, token.getType());
        assertEquals(expectStr, token.getText());
    }

    @org.junit.jupiter.api.Test
    void parse() {
        try (FileInputStream in = new FileInputStream("pa1-tests-partial/fail157.java")) {
            ErrorReporter reporter = new ErrorReporter();
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

                assert (reporter.hasErrors());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void compile() {
        File folder = new File("/Users/davidkim/spring-2024/comp520/miniJavaCompiler/pa1-tests-partial");
        File[] files = folder.listFiles();
        ErrorReporter reporter = new ErrorReporter();
        System.out.println("testing...");
        for (File file : files) {
            System.out.println("File: " + file.getName());
            try (FileInputStream in = new FileInputStream(file.getPath())) {
                reporter.clearErrors();
                Lexer lexer = new Lexer(in, reporter);
                Parser parser = new Parser(lexer, reporter);
                try {
                    parser.parse();
                } catch(LexerError e) {

                } catch(SyntaxError e) {

                }
            } catch (IOException e) {

            } finally {
                if (!reporter.hasErrors()) System.out.println("Success");
                else {
                    System.out.println("Error");
                    reporter.outputErrors();
                }
                if (file.getName().indexOf('f') == 0) {
                    assert(reporter.hasErrors());
                } else {
                    assert(!reporter.hasErrors());
                }
            }
        }
    }
}
