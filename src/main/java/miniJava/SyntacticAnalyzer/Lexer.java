package comp520.syntacticanalyzer;

import comp520.ErrorReporter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Lexer implements LexerInterface {
    private char _charBuf;
    private boolean _eot = false;
    private StringBuilder _stringBuf;
    private InputStream _in;
    private ErrorReporter _reporter;

    public Lexer(FileInputStream in, ErrorReporter reporter) {
        this._stringBuf = new StringBuilder();
        this._in = in;
        this._reporter = reporter;
    }

    @Override
    public TokenType scanToken() {
        Token token = null;
        _stringBuf.setLength(0);
        switch (_charBuf) {
            case '/':
                if (peek('/')) {
                    skipIt();
                    while (_charBuf != '\n' && !_eot) {
                        skipIt();
                    }
                } else if (peek('*')) {
                    skipIt();
                    while (_charBuf != '*' && !_eot) {
                        skipIt();
                    }
                } else {
                    takeIt();
                    return TokenType.OPERATOR;
                }
                break;
            case '&':
                if (peek('&')) {
                    takeIt();
                } else {
                    _reporter.reportError("token not recognized, character after '&' incorrect");
                }
                break;
            case '|':
                if (peek('|')) {
                    takeIt();
                } else {
                    _reporter.reportError("token not recognized, character after '|' incorrect");
                }
                break;
            case '+', '-', '!', '*':
                takeIt();
                return TokenType.OPERATOR;
            default:
                return TokenType.IDENTIFIER;
        }
        return TokenType.ERROR;
    }

    @Override
    public Token scan() {
        _stringBuf.setLength(0);
        TokenType tokType = scanToken();
        String text = _stringBuf.toString();
        return new Token(tokType, text);
    }

    private boolean peek(char expected) {
        nextChar();
        if (_charBuf == expected) {
            return true;
        }
        return false;
    }

    private static final char eolUnix = '\n';
    private static final char eolWindows = '\r';

    private void nextChar() {
        if (!_eot) {
            readChar();
        }
    }

    private void readChar() {
        try {
            int c = _in.read();
            _charBuf = (char) c;
            if (c == -1 || _charBuf == eolUnix || _charBuf == eolWindows) {
                _eot = true;
            }
        } catch (IOException e) {
            lexError("I/O Exception");
            _eot = true;
        }
    }

    private void takeIt() {
        _stringBuf.append(_charBuf);
    }

    private void skipIt() {
        nextChar();
    }

    private void lexError(String errorMsg) {
        _reporter.reportError("Lexer Error: " + errorMsg);
    }
}