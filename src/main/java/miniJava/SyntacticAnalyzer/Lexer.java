package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
    }

    @Override
    public Token scan() {
        if (charBuf == eolWindows) {
            skipIt();
            skipIt();
        }

        while (!eot && (charBuf == ' ' || charBuf == eolUnix || charBuf == eolWindows)) { 
            this.skipIt();
        }
        stringBuf.setLength(0);
        TokenType tokType = scanToken();
        String text = stringBuf.toString();
        return new Token(tokType, text);
    }

    private TokenType scanToken() {
        if (eot) return TokenType.EOT;
        // TODO: refactor token handling code to decrease indent levels once I get a working lexer
        switch (charBuf) {
            case '/':
                TokenType slashResult = handleSlash();
                return slashResult != null ? slashResult : TokenType.ERROR;
            case '&':
                takeIt();
                if (peek('&')) {
                    takeIt();
                }
                return TokenType.OPERATOR;
            case '|':
                takeIt();
                if (peek('|')) {
                    takeIt();
                }
                return TokenType.OPERATOR;
            case '!':
                takeIt();
                if (peek('=')) {
                    takeIt();
                }
                return TokenType.OPERATOR;
            case '+', '-', '*':
                takeIt();
                return TokenType.OPERATOR;
            case '>':
                takeIt();
                if (peek('=')) {
                    takeIt();
                } 
                return TokenType.OPERATOR;
            case '<':
                if (peek('=')) {
                    takeIt();
                    return TokenType.OPERATOR;
                } else {
                    lexError("");
                }
            case '.':
                takeIt();
            case ';':
                return TokenType.SEMICOLON;
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
        keywordMap.put("new", TokenType.NEW);
        keywordMap.put("class", TokenType.CLASS);
        keywordMap.put("static", TokenType.STATIC);
        keywordMap.put("public", TokenType.VISIBILITY);
        keywordMap.put("private", TokenType.VISIBILITY);
        keywordMap.put("return", TokenType.RETURN);
        keywordMap.put("this", TokenType.THIS);
        keywordMap.put("void", TokenType.VOID);
    }

    private TokenType handleIdentifier() {

        while (charBuf != ' ' && charBuf != eolUnix && charBuf != eolWindows) {
            takeIt();
        }
        if (keywordMap.containsKey(stringBuf.toString())) {
            return keywordMap.get(stringBuf.toString());
        }
        return TokenType.IDENTIFIER;
    }

    private TokenType handleSlash() {
        if (peek('/')) {
            this.skipIt();
            while (charBuf != '\n' && !eot) {
                skipIt();
            }
            return null;
        } else if (peek('*')) {
            skipIt();
            while (charBuf != '*' && !eot) {
                skipIt();
            }
            return null;
        } else {
            takeIt();
            return TokenType.OPERATOR;
        }
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
        } catch (IOException e) {
            lexError("I/O Exception");
            eot = true;
        }
    }

    private void takeIt() {
        stringBuf.append(charBuf);
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

    private void lexError(String errorMsg) {
        reporter.reportError("Lexer Error: " + errorMsg);
    }
}
