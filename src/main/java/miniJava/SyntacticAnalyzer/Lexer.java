package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static miniJava.SyntacticAnalyzer.TokenType.*;

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
//        System.out.println("scanning...");
        stringBuf.setLength(0);
        while (!eot && (charBuf == ' ' || charBuf == '\t' || charBuf == eolUnix || charBuf == eolWindows)) {
            // System.out.println("skipping whitespace");
            this.skipIt();
        }
        // handle windows carry return

        if (eot) return new Token(EOT, null);
        // TODO: refactor token handling code to decrease indent levels once I get a working lexer
        switch (charBuf) {
            // simple single character cases
            case '(':
                takeIt();
                return new Token(LPAREN, "(");
            case ')':
                takeIt();
                return new Token(RPAREN, ")");
            case '[':
                takeIt();
                return new Token(LSQUARE, "[");
            case ']':
                takeIt();
                return new Token(RSQUARE, "]");
            case '{':
                takeIt();
                return new Token(LCURLY, "{");
            case '}':
                takeIt();
                return new Token(RCURLY, "}");
            case '+':
            case '-':
            case '*':
                takeIt();
                return new Token(OPERATOR, stringBuf.toString());
            case '.':
                takeIt();
                return new Token(PERIOD, ".");
            case ',':
                takeIt();
                return new Token(COMMA, ",");
            case ';':
                takeIt();
                return new Token(SEMICOLON, ";");
            // one or two character lexemes
            case '=':
                takeIt();
                if (charBuf == '=') {
                    takeIt();
                    return new Token(OPERATOR, "==");
                }
                return new Token(EQUALS, "=");
            case '!':
            case '>':
            case '<':
                takeIt();
                if (charBuf == '=') {
                    takeIt();
//                    System.out.println("operator: " + stringBuf.toString());
                }
//                System.out.println("operator: " + stringBuf.toString());
                return new Token(OPERATOR, stringBuf.toString());
            // two character lexemes
            case '&':
                takeIt();
                if (charBuf == '&') {
                    takeIt();
                    return new Token(OPERATOR, "&&");
                }
                lexError("& followed by: '" + charBuf + "'");
                return new Token(ERROR, stringBuf.toString());
            case '|':
                takeIt();
                if (charBuf == '|') {
                    takeIt();
                    return new Token(OPERATOR, "||");
                }
                lexError("| followed by: '" + charBuf + "'");
                return new Token(ERROR, stringBuf.toString());
            // indefinite lexemes
            case '/':
                takeIt();
                stringBuf.setLength(0);
                if (charBuf == '/') {
                    skipIt();
                    while (charBuf != '\n' && !eot) {
                        skipIt();
                    }
                    stringBuf.setLength(0);
                    return scan();
                } else if (charBuf == '*') {
                    boolean endComment = false;
                    skipIt();
                    while (!endComment) {
                        System.out.println(charBuf);
                        if (eot) throw new SyntaxError();
                        if (charBuf == '*') {
                            skipIt();
                            endComment = charBuf == '/';
                        } else {
                            skipIt();
                        }
                    }
                    skipIt();
                    return scan();
                } else return new Token(OPERATOR, "/");
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                while (isDigit(charBuf)) {
                    takeIt();
                }
                return createToken(INTLITERAL, stringBuf.toString());
            default:
                if (isAlpha(charBuf)) {
                    return handleIdentifier();
                }
                lexError("Unrecognized character '" + charBuf + "' in input");
                return new Token(ERROR, stringBuf.toString());
        }
    }

    private Token createToken(TokenType type, String text) {
//        System.out.println("new Token Type: " + type + ", text: '" + text + "'");
        return new Token(type, text);
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

    private Token handleIdentifier() {
        while (isAlphaNumeric(charBuf)) {
            takeIt();
        }
        TokenType type = keywordMap.get(stringBuf.toString());
        if (stringBuf.charAt(0) == '_') {
            lexError("cannot start identifier with underscore" + stringBuf.toString());
            return new Token(ERROR, stringBuf.toString());
        }
        return type != null ? createToken(type, stringBuf.toString()) : createToken(IDENTIFIER, stringBuf.toString());
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
