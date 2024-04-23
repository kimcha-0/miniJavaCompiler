package syntacticanalyzer;

import miniJava.AbstractSyntaxTrees.AST;
import miniJava.AbstractSyntaxTrees.ASTDisplay;
import miniJava.CodeGeneration.CodeGenerator;
import miniJava.ContextualAnalysis.ScopedIdentification;
import miniJava.ContextualAnalysis.TypeChecker;
import miniJava.ErrorReporter;
import miniJava.IdentificationError;
import miniJava.SyntacticAnalyzer.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static miniJava.SyntacticAnalyzer.TokenType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PA1Test {

    @org.junit.jupiter.api.Test
    void scan() {
        try (FileInputStream in = new FileInputStream("pa1-tests-partial/fail322.java")) {
            ErrorReporter reporter = new ErrorReporter();
            LexerImpl lexer = new LexerImpl(in, reporter);
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

    void assertToken(LexerImpl lexer, TokenType expectType, String expectStr) {
        Token token = lexer.scan();
        assertEquals(expectType, token.getTokenType());
        assertEquals(expectStr, token.getTokenText());
    }

    @org.junit.jupiter.api.Test
    void parse() {
        try (FileInputStream in = new FileInputStream("/Users/davidkim/spring-2024/comp520/miniJavaCompiler/pa4_tests_partial/pass401.java")) {
            ErrorReporter reporter = new ErrorReporter();
            LexerImpl lexer = new LexerImpl(in, reporter);
            ParserImpl parser = new ParserImpl(lexer, reporter);
            ASTDisplay display = new ASTDisplay();
            AST ast = null;
            try {
                ast = parser.parse();
            } catch (SyntaxError e) {
                e.printStackTrace();
            } finally {
                if (!reporter.hasErrors()) {
                    try {
                        ScopedIdentification sI = new ScopedIdentification(reporter, ast);
                    } catch (IdentificationError e) {
//                        e.printStackTrace();
                    } finally {
                        if (reporter.hasErrors()) {
                            System.out.println("Error");
                            reporter.outputErrors();
                        } else {
                            display.showTree(ast);
                            try {
                                TypeChecker typeChecker = new TypeChecker(ast, reporter);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (!reporter.hasErrors()) {
                                System.out.println("Success");
                                CodeGenerator generator = new CodeGenerator(reporter, ast);
                            } else {
                                System.out.println("Error");
                                reporter.outputErrors();
                            }
                        }
                    }
                } else {
                    System.out.println("Error");
                    reporter.outputErrors();
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void compile() {
        File folder = new File("/Users/davidkim/spring-2024/comp520/miniJavaCompiler/pa3_tests");
        File[] files = folder.listFiles();
        ErrorReporter reporter = new ErrorReporter();
        System.out.println("testing...");
        AST syntaxTree = null;
        for (File file : files) {
            System.out.println("File: " + file.getName());
            try (FileInputStream in = new FileInputStream(file.getPath())) {
                reporter.clearErrors();
                LexerImpl lexer = new LexerImpl(in, reporter);
                ParserImpl parser = new ParserImpl(lexer, reporter);
                try {
                    syntaxTree = parser.parse();
                } catch (LexerError e) {

                } catch (SyntaxError e) {

                }
            } catch (IOException e) {

            } finally {
                if (!reporter.hasErrors()) {
                    ASTDisplay astDisplay = new ASTDisplay();
                    astDisplay.showTree(syntaxTree);
                    try {
                        ScopedIdentification scopedIdentification = new ScopedIdentification(reporter, syntaxTree);
                    } catch (IdentificationError e) {

                    } finally {
                        if (reporter.hasErrors()) {
                            System.out.println("Error");
                            reporter.outputErrors();
                        } else {
                            TypeChecker typeChecker = new TypeChecker(syntaxTree, reporter);
                            if (reporter.hasErrors()) {
                                System.out.println("Error");
                                reporter.outputErrors();
                            } else {
                                System.out.println("Success");
                            }
                        }
                    }
                }
                else {
                    System.out.println("Error");
                    reporter.outputErrors();
                }
                if (file.getName().indexOf('f') == 0) {
                    assert (reporter.hasErrors());
                } else {
                    assert (!reporter.hasErrors());
                }
                System.out.println('\n');
            }
        }
    }
}
