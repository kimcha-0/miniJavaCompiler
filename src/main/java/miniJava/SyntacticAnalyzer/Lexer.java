package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.sun.nio.sctp.HandlerResult;

public class Lexer implements LexerInterface {
    private char charBuf;
    private boolean eot = false;
    private StringBuilder stringBuf;
    private InputStream in;
    private ErrorReporter reporter;

    public Lexer(FileInputStream in, ErrorReporter reporter) {
        this.stringBuf = new StringBuilder();
        this.in = in;
        this.reporter = reporter;
    }

    @Override
    public Token scan() {
        while (charBuf == '\n' || charBuf == '\r' || charBuf == ' ') {
            this.skipIt();
        }
        stringBuf.setLength(0);
        TokenType tokType = scanToken();
        String text = stringBuf.toString();
        return new Token(tokType, text);
    }

    private TokenType scanToken() {
        // TODO: refactor token handling code to decrease indent levels once I get a working lexer
        stringBuf.setLength(0);
        switch (charBuf) {
            case '/':
                return handleSlash();
            case '&':
                if (peek('&')) {
                    takeIt();
                    return TokenType.OPERATOR;
                } else {
                    lexError("Unrecognized character'" + charBuf + "' in input");
                }
            case '|':
                if (peek('|')) {
                    takeIt();
                    return TokenType.OPERATOR;
                } else {
                    reporter.reportError("token not recognized, character after '|' incorrect");
                }
            case '+', '-', '!', '*':
                takeIt();
                return TokenType.OPERATOR;
            case '>':
                if (peek('=')) {
                    takeIt();
                    return TokenType.OPERATOR;
                } else {
                    lexError("Unrecognized character '" + charBuf + "' in input");
                }
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

    private TokenType handleIdentifier() {
        return null;
    }

    private TokenType handleSlash() {
        if (peek('/')) {
            this.skipIt();
            while (charBuf != '\n' && !eot) {
                skipIt();
            }
        } else if (peek('*')) {
            skipIt();
            while (charBuf != '*' && !eot) {
                skipIt();
            }
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

    private static final char eolUnix = '\n';
    private static final char eolWindows = '\r';

    private void nextChar() {
        if (!eot) {
            readChar();
        }
    }

    private void readChar() {
        try {
            int c = in.read();
            charBuf = (char) c;
            if (c == -1 || charBuf == eolUnix || charBuf == eolWindows) {
                eot = true;
            }
        } catch (IOException e) {
            lexError("I/O Exception");
            eot = true;
        }
    }

    private void takeIt() {
        stringBuf.append(charBuf);
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
