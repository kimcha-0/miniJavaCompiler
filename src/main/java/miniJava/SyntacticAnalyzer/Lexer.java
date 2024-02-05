package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static miniJava.SyntacticAnalyzer.TokenType.OPERATOR;

public class Lexer implements LexerInterface {
    private static Map<String, TokenType> keywordMap;
    private char charBuf;
    private boolean eot = false;
    private StringBuilder stringBuf;
    private InputStream in;
    private ErrorReporter reporter;

    private static final char eolUnix = '\n';
    private static final char eolWindows = '\r';

    public Lexer(FileInputStream in, ErrorReporter reporter) {
        this.stringBuf = new StringBuilder();
        this.in = in;
        this.reporter = reporter;
        nextChar();
    }

    @Override
    public Token scan() {
        stringBuf.setLength(0);
        while (!eot && (charBuf == ' ' || charBuf == '\t'
        || charBuf == eolUnix || charBuf == eolWindows)) {
            // System.out.println("skipping whitespace");
            this.skipIt();
        }
        if (charBuf == eolWindows) {
            skipIt();
            skipIt();
        }
        TokenType tokType = scanToken();
        while (tokType == null) {
            while (!eot && (charBuf == ' ' || charBuf == '\t'
                    || charBuf == eolUnix || charBuf == eolWindows)) {
                // System.out.println("skipping whitespace");
                this.skipIt();
            }
            tokType = scanToken();
        }
        String text = stringBuf.toString();
//        System.out.println(text);
        return new Token(tokType, text);
    }

    private TokenType scanToken() throws LexerError {
//        System.out.println("scanning...");
        if (eot) return TokenType.EOT;
        // TODO: refactor token handling code to decrease indent levels once I get a working lexer
        switch (charBuf) {
            // simple single character cases
            case '(':
                takeIt();
                return TokenType.LPAREN;
            case ')':
                takeIt();
                return TokenType.RPAREN;
            case '[':
                takeIt();
                return TokenType.LSQUARE;
            case ']':
                takeIt();
                return TokenType.RSQUARE;
            case '{':
                takeIt();
                return TokenType.LCURLY;
            case '}':
                takeIt();
                return TokenType.RCURLY;
            case '+', '-', '*':
                takeIt();
                return OPERATOR;
            case '.':
                takeIt();
                return TokenType.PERIOD;
            case ',':
                takeIt();
                return TokenType.COMMA;
            case ';':
                takeIt();
                return TokenType.SEMICOLON;
            // one or two character lexemes
            case '=':
                takeIt();
                if (charBuf == '=') {
                    takeIt();
                    return OPERATOR;
                }
                return TokenType.EQUALS;
            case '!', '>', '<':
                takeIt();
                if (charBuf == '=') {
                    takeIt();
//                    System.out.println("operator: " + stringBuf.toString());
                }
//                System.out.println("operator: " + stringBuf.toString());
                return OPERATOR;
                // two character lexemes
            case '&':
                takeIt();
                if (charBuf == '&') {
                    takeIt();
                }
                return OPERATOR;
            case '|':
                takeIt();
                if (charBuf == '|') {
                    takeIt();
                }
                return OPERATOR;
                // indefinite lexemes
            case '/':
                takeIt();
                if (charBuf == '/') {
                    stringBuf.setLength(0);
                    skipIt();
                    while (charBuf != '\n' && !eot) {
                        skipIt();
                    }
                    return null;
                } else if (charBuf == '*') {
                    skipIt();
                    while (!eot) {
                        if (charBuf == '*') {
                            skipIt();
                            if (charBuf == '/') {
                                skipIt();
                                return null;
                            } else throw new LexerError();
                        }
                        skipIt();
                    }
                } else return OPERATOR;
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9': 
                while (isDigit(charBuf)) {
                    takeIt();
                }
                return TokenType.INTLITERAL;
            default:
                if (isAlpha(charBuf)) {
                    return handleIdentifier();
                }
                lexError("Unrecognized character '" + charBuf + "' in input");
                return TokenType.ERROR;
        }
    }

    static {
        keywordMap = new HashMap<>();
        keywordMap.put("or", TokenType.OR);
        keywordMap.put("while", TokenType.WHILE);
        keywordMap.put("if", TokenType.IF);
        keywordMap.put("else", TokenType.ELSE);
        keywordMap.put("new", TokenType.NEW);
        keywordMap.put("class", TokenType.CLASS);
        keywordMap.put("static", TokenType.STATIC);
        keywordMap.put("boolean", TokenType.BOOLEAN);
        keywordMap.put("public", TokenType.PUBLIC);
        keywordMap.put("private", TokenType.PRIVATE);
        keywordMap.put("return", TokenType.RETURN);
        keywordMap.put("this", TokenType.THIS);
        keywordMap.put("void", TokenType.VOID);
        keywordMap.put("true", TokenType.TRUE);
        keywordMap.put("false", TokenType.FALSE);
        keywordMap.put("int", TokenType.INT);
    }

    private TokenType handleIdentifier() {
        while (isAlphaNumeric(charBuf)) {
            takeIt();
        }
        TokenType type = keywordMap.get(stringBuf.toString());
        if (stringBuf.charAt(0) == '_') {
            lexError("cannot start identifier with underscore" + stringBuf.toString());
            return TokenType.ERROR;
        }
        return type != null ? type : TokenType.IDENTIFIER;
    }

    private boolean peek(char expected) {
        nextChar();
        if (charBuf == expected) {
            return true;
        }
        return false;
    }


    private void nextChar() {
        if (!eot) {
            readChar();
        }
    }

    private void readChar() {
        try {
            int c = in.read();
            charBuf = (char) c;
            if (c == -1) {
                eot = true;
            }
//            System.out.println("read: " + (char)c);
        } catch (IOException e) {
            lexError("I/O Exception");
            eot = true;
        }
    }

    private void takeIt() {
        stringBuf.append(charBuf);
//        System.out.println("StringBuf: " + stringBuf);
        nextChar();
    }

    private void skipIt() {
        nextChar();
    }

    private boolean isAlpha(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void lexError(String errorMsg) {
        reporter.reportError("Lexer Error: " + errorMsg);
    }
}
